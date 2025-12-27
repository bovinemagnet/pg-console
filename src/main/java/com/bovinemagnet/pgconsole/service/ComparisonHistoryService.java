package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.ComparisonHistory;
import com.bovinemagnet.pgconsole.model.SchemaComparisonResult;
import com.bovinemagnet.pgconsole.repository.ComparisonHistoryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing schema comparison history.
 * <p>
 * Tracks comparison executions for audit purposes and
 * enables drift detection over time.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class ComparisonHistoryService {

    private static final Logger LOG = Logger.getLogger(ComparisonHistoryService.class);

    @Inject
    ComparisonHistoryRepository repository;

    @ConfigProperty(name = "pgconsole.comparison.history.retention-days", defaultValue = "90")
    int retentionDays;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Records a comparison result in history.
     *
     * @param result comparison result
     * @param username user who performed the comparison
     * @param profileName optional profile name used
     */
    public void record(SchemaComparisonResult result, String username, String profileName) {
        LOG.infof("Recording comparison history: %s.%s -> %s.%s by %s",
                result.getSourceInstance(), result.getSourceSchema(),
                result.getDestinationInstance(), result.getDestinationSchema(),
                username);

        ComparisonHistory history = ComparisonHistory.fromResult(result, username);
        history.setProfileName(profileName);

        // Store result snapshot as JSON
        try {
            history.setResultSnapshotJson(objectMapper.writeValueAsString(result.getSummary()));
            if (result.getFilter() != null) {
                history.setFilterConfigJson(objectMapper.writeValueAsString(result.getFilter()));
            }
        } catch (JsonProcessingException e) {
            LOG.warnf("Failed to serialise comparison result: %s", e.getMessage());
        }

        repository.save(history);
    }

    /**
     * Gets recent comparison history.
     *
     * @param limit maximum records to return
     * @return list of history records
     */
    public List<ComparisonHistory> getHistory(int limit) {
        return repository.findRecent(limit);
    }

    /**
     * Gets history for a specific instance pair.
     *
     * @param sourceInstance source instance
     * @param destInstance destination instance
     * @param days number of days to look back
     * @return matching history records
     */
    public List<ComparisonHistory> getHistoryForPair(String sourceInstance, String destInstance, int days) {
        return repository.findByInstances(sourceInstance, destInstance, days);
    }

    /**
     * Gets the most recent comparison for an instance pair.
     *
     * @param sourceInstance source instance
     * @param destInstance destination instance
     * @return most recent history or empty
     */
    public Optional<ComparisonHistory> getMostRecent(String sourceInstance, String destInstance) {
        return repository.findMostRecent(sourceInstance, destInstance);
    }

    /**
     * Gets a history record by ID.
     *
     * @param id history ID
     * @return history record or empty
     */
    public Optional<ComparisonHistory> findById(long id) {
        return repository.findById(id);
    }

    /**
     * Gets total history count.
     *
     * @return total count
     */
    public long count() {
        return repository.count();
    }

    /**
     * Detects drift between current comparison and previous.
     *
     * @param current current comparison
     * @param sourceInstance source instance
     * @param destInstance destination instance
     * @return true if drift detected
     */
    public boolean detectDrift(SchemaComparisonResult current,
                                String sourceInstance, String destInstance) {
        Optional<ComparisonHistory> previous = repository.findMostRecent(sourceInstance, destInstance);
        if (previous.isEmpty()) {
            return false;
        }

        ComparisonHistory prev = previous.get();
        SchemaComparisonResult.ComparisonSummary summary = current.getSummary();

        return summary.getMissingObjects() != prev.getMissingCount()
                || summary.getExtraObjects() != prev.getExtraCount()
                || summary.getModifiedObjects() != prev.getModifiedCount();
    }

    /**
     * Gets drift summary between current and previous comparison.
     *
     * @param current current comparison result
     * @param sourceInstance source instance
     * @param destInstance destination instance
     * @return drift summary or null if no previous comparison
     */
    public DriftSummary getDriftSummary(SchemaComparisonResult current,
                                         String sourceInstance, String destInstance) {
        Optional<ComparisonHistory> previous = repository.findMostRecent(sourceInstance, destInstance);
        if (previous.isEmpty()) {
            return null;
        }

        ComparisonHistory prev = previous.get();
        SchemaComparisonResult.ComparisonSummary summary = current.getSummary();

        return new DriftSummary(
                summary.getMissingObjects() - prev.getMissingCount(),
                summary.getExtraObjects() - prev.getExtraCount(),
                summary.getModifiedObjects() - prev.getModifiedCount(),
                prev.getComparedAt()
        );
    }

    /**
     * Scheduled job to clean up old history records.
     */
    @Scheduled(cron = "0 0 3 * * ?") // Daily at 3 AM
    void cleanupOldHistory() {
        int deleted = repository.deleteOlderThan(retentionDays);
        if (deleted > 0) {
            LOG.infof("Cleaned up %d old comparison history records", deleted);
        }
    }

    /**
     * Manually triggers cleanup of old history.
     *
     * @param days age threshold in days
     * @return number of records deleted
     */
    public int deleteOlderThan(int days) {
        int deleted = repository.deleteOlderThan(days);
        LOG.infof("Deleted %d history records older than %d days", deleted, days);
        return deleted;
    }

    /**
     * Summary of drift between comparisons.
     */
    public record DriftSummary(
            int missingDelta,
            int extraDelta,
            int modifiedDelta,
            java.time.Instant previousComparedAt
    ) {
        /**
         * Checks if any drift occurred.
         *
         * @return true if any delta is non-zero
         */
        public boolean hasDrift() {
            return missingDelta != 0 || extraDelta != 0 || modifiedDelta != 0;
        }

        /**
         * Gets total absolute drift.
         *
         * @return sum of absolute deltas
         */
        public int totalDrift() {
            return Math.abs(missingDelta) + Math.abs(extraDelta) + Math.abs(modifiedDelta);
        }

        /**
         * Gets a summary description.
         *
         * @return human-readable description
         */
        public String getDescription() {
            if (!hasDrift()) {
                return "No drift detected";
            }

            StringBuilder sb = new StringBuilder();
            if (missingDelta != 0) {
                sb.append(missingDelta > 0 ? "+" : "").append(missingDelta).append(" missing, ");
            }
            if (extraDelta != 0) {
                sb.append(extraDelta > 0 ? "+" : "").append(extraDelta).append(" extra, ");
            }
            if (modifiedDelta != 0) {
                sb.append(modifiedDelta > 0 ? "+" : "").append(modifiedDelta).append(" modified");
            }

            String result = sb.toString();
            if (result.endsWith(", ")) {
                result = result.substring(0, result.length() - 2);
            }
            return result;
        }
    }
}
