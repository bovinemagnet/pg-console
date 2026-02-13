package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.config.InstanceConfig;
import com.bovinemagnet.pgconsole.model.DatabaseMetricsHistory;
import com.bovinemagnet.pgconsole.model.InfrastructureMetricsHistory;
import com.bovinemagnet.pgconsole.model.SystemMetricsHistory;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Function;

/**
 * In-memory storage for metrics when schema is disabled.
 * <p>
 * This service stores recent metrics in memory for short-term trend display
 * when the pgconsole schema is not available. Metrics are automatically
 * evicted after the configured retention period.
 * <p>
 * Thread-safe for concurrent sampling and reading.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class InMemoryMetricsStore {

    private static final Logger LOG = Logger.getLogger("pgconsole.InMemoryMetricsStore");

    /**
     * System metrics storage keyed by instance ID.
     */
    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<SystemMetricsHistory>> systemMetrics =
            new ConcurrentHashMap<>();

    /**
     * Database metrics storage keyed by "instanceId::databaseName".
     */
    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<DatabaseMetricsHistory>> databaseMetrics =
            new ConcurrentHashMap<>();

    /**
     * Infrastructure metrics storage keyed by instance ID.
     */
    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<InfrastructureMetricsHistory>> infrastructureMetrics =
            new ConcurrentHashMap<>();

    @Inject
    InstanceConfig config;

    /**
     * Adds a system metrics snapshot to the in-memory store.
     *
     * @param instanceId the instance identifier
     * @param metrics    the metrics snapshot to store
     */
    public void addSystemMetrics(String instanceId, SystemMetricsHistory metrics) {
        if (instanceId == null || metrics == null) {
            return;
        }

        ConcurrentLinkedDeque<SystemMetricsHistory> deque = systemMetrics.computeIfAbsent(
                instanceId, k -> new ConcurrentLinkedDeque<>());

        // Ensure sampledAt is set
        if (metrics.getSampledAt() == null) {
            metrics.setSampledAt(Instant.now());
        }

        deque.addLast(metrics);
        LOG.debugf("Added system metrics for instance '%s', store size: %d", instanceId, deque.size());
    }

    /**
     * Retrieves system metrics history for a given instance within a time window.
     *
     * @param instanceId the instance identifier
     * @param hours      number of hours to look back
     * @return list of metrics snapshots ordered by time (oldest first)
     */
    public List<SystemMetricsHistory> getSystemMetricsHistory(String instanceId, int hours) {
        if (instanceId == null) {
            return new ArrayList<>();
        }

        ConcurrentLinkedDeque<SystemMetricsHistory> deque = systemMetrics.get(instanceId);
        if (deque == null || deque.isEmpty()) {
            return new ArrayList<>();
        }

        Instant cutoff = Instant.now().minus(hours, ChronoUnit.HOURS);
        List<SystemMetricsHistory> result = new ArrayList<>();

        for (SystemMetricsHistory m : deque) {
            if (m.getSampledAt() != null && m.getSampledAt().isAfter(cutoff)) {
                result.add(m);
            }
        }

        LOG.debugf("Retrieved %d system metrics for instance '%s' (last %d hours)",
                Integer.valueOf(result.size()), instanceId, Integer.valueOf(hours));
        return result;
    }

    /**
     * Gets the current number of stored metrics for an instance.
     *
     * @param instanceId the instance identifier
     * @return number of stored metrics
     */
    public int getMetricsCount(String instanceId) {
        ConcurrentLinkedDeque<SystemMetricsHistory> deque = systemMetrics.get(instanceId);
        return deque != null ? deque.size() : 0;
    }

    /**
     * Gets the configured retention period in minutes.
     *
     * @return retention period in minutes
     */
    public int getRetentionMinutes() {
        return config.schema().inMemoryMinutes();
    }

    // --- Database Metrics ---

    /**
     * Adds a database metrics snapshot to the in-memory store.
     *
     * @param instanceId   the instance identifier
     * @param databaseName the database name
     * @param metrics      the metrics snapshot to store
     */
    public void addDatabaseMetrics(String instanceId, String databaseName, DatabaseMetricsHistory metrics) {
        if (instanceId == null || databaseName == null || metrics == null) {
            return;
        }

        String key = instanceId + "::" + databaseName;
        ConcurrentLinkedDeque<DatabaseMetricsHistory> deque = databaseMetrics.computeIfAbsent(
                key, k -> new ConcurrentLinkedDeque<>());

        if (metrics.getSampledAt() == null) {
            metrics.setSampledAt(Instant.now());
        }

        deque.addLast(metrics);
        LOG.debugf("Added database metrics for '%s' on instance '%s', store size: %d",
                databaseName, instanceId, deque.size());
    }

    /**
     * Retrieves database metrics history for a given instance and database within a time window.
     *
     * @param instanceId   the instance identifier
     * @param databaseName the database name
     * @param hours        number of hours to look back
     * @return list of metrics snapshots ordered by time (oldest first)
     */
    public List<DatabaseMetricsHistory> getDatabaseMetricsHistory(String instanceId, String databaseName, int hours) {
        if (instanceId == null || databaseName == null) {
            return new ArrayList<>();
        }

        String key = instanceId + "::" + databaseName;
        ConcurrentLinkedDeque<DatabaseMetricsHistory> deque = databaseMetrics.get(key);
        if (deque == null || deque.isEmpty()) {
            return new ArrayList<>();
        }

        Instant cutoff = Instant.now().minus(hours, ChronoUnit.HOURS);
        List<DatabaseMetricsHistory> result = new ArrayList<>();

        for (DatabaseMetricsHistory m : deque) {
            if (m.getSampledAt() != null && m.getSampledAt().isAfter(cutoff)) {
                result.add(m);
            }
        }

        return result;
    }

    // --- Infrastructure Metrics ---

    /**
     * Adds an infrastructure metrics snapshot to the in-memory store.
     *
     * @param instanceId the instance identifier
     * @param metrics    the metrics snapshot to store
     */
    public void addInfrastructureMetrics(String instanceId, InfrastructureMetricsHistory metrics) {
        if (instanceId == null || metrics == null) {
            return;
        }

        ConcurrentLinkedDeque<InfrastructureMetricsHistory> deque = infrastructureMetrics.computeIfAbsent(
                instanceId, k -> new ConcurrentLinkedDeque<>());

        if (metrics.getSampledAt() == null) {
            metrics.setSampledAt(Instant.now());
        }

        deque.addLast(metrics);
        LOG.debugf("Added infrastructure metrics for instance '%s', store size: %d", instanceId, deque.size());
    }

    /**
     * Retrieves infrastructure metrics history for a given instance within a time window.
     *
     * @param instanceId the instance identifier
     * @param hours      number of hours to look back
     * @return list of metrics snapshots ordered by time (oldest first)
     */
    public List<InfrastructureMetricsHistory> getInfrastructureMetricsHistory(String instanceId, int hours) {
        if (instanceId == null) {
            return new ArrayList<>();
        }

        ConcurrentLinkedDeque<InfrastructureMetricsHistory> deque = infrastructureMetrics.get(instanceId);
        if (deque == null || deque.isEmpty()) {
            return new ArrayList<>();
        }

        Instant cutoff = Instant.now().minus(hours, ChronoUnit.HOURS);
        List<InfrastructureMetricsHistory> result = new ArrayList<>();

        for (InfrastructureMetricsHistory m : deque) {
            if (m.getSampledAt() != null && m.getSampledAt().isAfter(cutoff)) {
                result.add(m);
            }
        }

        return result;
    }

    /**
     * Clears all stored metrics (useful for testing).
     */
    public void clear() {
        systemMetrics.clear();
        databaseMetrics.clear();
        infrastructureMetrics.clear();
        LOG.debug("Cleared all in-memory metrics");
    }

    /**
     * Clears stored metrics for a specific instance.
     *
     * @param instanceId the instance identifier
     */
    public void clear(String instanceId) {
        systemMetrics.remove(instanceId);
        infrastructureMetrics.remove(instanceId);
        // Remove all database metrics for this instance
        databaseMetrics.keySet().removeIf(key -> key.startsWith(instanceId + "::"));
        LOG.debugf("Cleared in-memory metrics for instance '%s'", instanceId);
    }

    /**
     * Scheduled job to evict old metrics entries.
     * <p>
     * Runs every 60 seconds and removes entries older than the configured
     * retention period (in-memory-minutes).
     */
    @Scheduled(every = "60s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void evictOldEntries() {
        // Only run eviction when schema is disabled (in-memory mode active)
        if (config.schema().enabled()) {
            return;
        }

        int retentionMinutes = config.schema().inMemoryMinutes();
        Instant cutoff = Instant.now().minus(retentionMinutes, ChronoUnit.MINUTES);

        int totalEvicted = 0;

        // Evict system metrics
        for (var entry : systemMetrics.entrySet()) {
            totalEvicted += evictDeque(entry.getValue(), cutoff,
                    m -> m.getSampledAt(), entry.getKey());
        }

        // Evict database metrics
        for (var entry : databaseMetrics.entrySet()) {
            totalEvicted += evictDeque(entry.getValue(), cutoff,
                    m -> m.getSampledAt(), entry.getKey());
        }

        // Evict infrastructure metrics
        for (var entry : infrastructureMetrics.entrySet()) {
            totalEvicted += evictDeque(entry.getValue(), cutoff,
                    m -> m.getSampledAt(), entry.getKey());
        }

        if (totalEvicted > 0) {
            LOG.debugf("In-memory eviction complete: removed %d entries older than %d minutes",
                    totalEvicted, retentionMinutes);
        }
    }

    /**
     * Generic helper to evict old entries from any deque.
     *
     * @param deque         the deque to evict from
     * @param cutoff        the cutoff instant
     * @param timeExtractor function to extract the timestamp from an entry
     * @param label         label for debug logging
     * @param <T>           the type of entries in the deque
     * @return number of evicted entries
     */
    private <T> int evictDeque(ConcurrentLinkedDeque<T> deque, Instant cutoff,
                               Function<T, Instant> timeExtractor, String label) {
        int evicted = 0;
        while (!deque.isEmpty()) {
            T oldest = deque.peekFirst();
            if (oldest != null) {
                Instant ts = timeExtractor.apply(oldest);
                if (ts != null && ts.isBefore(cutoff)) {
                    deque.pollFirst();
                    evicted++;
                } else {
                    break;
                }
            } else {
                break;
            }
        }
        if (evicted > 0) {
            LOG.debugf("Evicted %d old entries from '%s'", evicted, label);
        }
        return evicted;
    }

    /**
     * Gets a summary of current storage state for logging.
     *
     * @return summary string
     */
    public String getStorageSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("In-memory metrics store: ");

        int totalEntries = 0;
        for (var entry : systemMetrics.entrySet()) {
            totalEntries += entry.getValue().size();
        }
        for (var entry : databaseMetrics.entrySet()) {
            totalEntries += entry.getValue().size();
        }
        for (var entry : infrastructureMetrics.entrySet()) {
            totalEntries += entry.getValue().size();
        }

        if (totalEntries == 0) {
            sb.append("empty");
        } else {
            sb.append("system=[");
            boolean first = true;
            for (var entry : systemMetrics.entrySet()) {
                if (!first) sb.append(", ");
                sb.append(entry.getKey()).append("=").append(entry.getValue().size());
                first = false;
            }
            sb.append("], database=").append(databaseMetrics.size()).append(" keys");
            sb.append(", infrastructure=[");
            first = true;
            for (var entry : infrastructureMetrics.entrySet()) {
                if (!first) sb.append(", ");
                sb.append(entry.getKey()).append("=").append(entry.getValue().size());
                first = false;
            }
            sb.append("]");
        }
        sb.append(" (retention: ").append(config.schema().inMemoryMinutes()).append(" min)");
        return sb.toString();
    }
}
