package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for aggregating and managing unified recommendations.
 * <p>
 * Collects recommendations from Index Advisor, Table Maintenance,
 * Query Regression, and Anomaly Detection into a single prioritised list.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class UnifiedRecommendationService {

    private static final Logger LOG = Logger.getLogger(UnifiedRecommendationService.class);

    @Inject
    DataSourceManager dataSourceManager;

    @Inject
    IndexAdvisorService indexAdvisorService;

    @Inject
    TableMaintenanceService tableMaintenanceService;

    @Inject
    QueryRegressionService queryRegressionService;

    @Inject
    AnomalyDetectionService anomalyDetectionService;

    /**
     * Get all unified recommendations for an instance, sorted by priority.
     *
     * @param instanceName the PostgreSQL instance name
     * @return list of unified recommendations
     */
    public List<UnifiedRecommendation> getRecommendations(String instanceName) {
        List<UnifiedRecommendation> recommendations = new ArrayList<>();

        // Collect from Index Advisor
        try {
            List<IndexRecommendation> indexRecs = indexAdvisorService.getRecommendations(instanceName);
            for (IndexRecommendation ir : indexRecs) {
                recommendations.add(convertIndexRecommendation(instanceName, ir));
            }
        } catch (Exception e) {
            LOG.debugf(e, "Error getting index recommendations");
        }

        // Collect from Table Maintenance
        try {
            List<TableMaintenanceRecommendation> maintRecs = tableMaintenanceService.getRecommendations(instanceName);
            for (TableMaintenanceRecommendation mr : maintRecs) {
                recommendations.add(convertMaintenanceRecommendation(instanceName, mr));
            }
        } catch (Exception e) {
            LOG.debugf(e, "Error getting maintenance recommendations");
        }

        // Collect from Query Regression
        try {
            List<QueryRegression> regressions = queryRegressionService.detectRegressions(instanceName, 24, 50);
            for (QueryRegression qr : regressions) {
                recommendations.add(convertQueryRegression(instanceName, qr));
            }
        } catch (Exception e) {
            LOG.debugf(e, "Error getting query regressions");
        }

        // Collect from Anomaly Detection
        try {
            List<DetectedAnomaly> anomalies = anomalyDetectionService.getOpenAnomalies(instanceName);
            for (DetectedAnomaly anomaly : anomalies) {
                recommendations.add(convertAnomaly(instanceName, anomaly));
            }
        } catch (Exception e) {
            LOG.debugf(e, "Error getting anomalies");
        }

        // Add config tuning suggestions
        recommendations.addAll(getConfigTuningSuggestions(instanceName));

        // Sort by priority score (descending)
        recommendations.sort((a, b) -> Integer.compare(b.getPriorityScore(), a.getPriorityScore()));

        return recommendations;
    }

    /**
     * Get recommendations filtered by source.
     *
     * @param instanceName the PostgreSQL instance name
     * @param source the source to filter by
     * @return filtered list of recommendations
     */
    public List<UnifiedRecommendation> getRecommendationsBySource(String instanceName,
                                                                    UnifiedRecommendation.Source source) {
        return getRecommendations(instanceName).stream()
                .filter(r -> r.getSource() == source)
                .collect(Collectors.toList());
    }

    /**
     * Get recommendations filtered by severity.
     *
     * @param instanceName the PostgreSQL instance name
     * @param severity the minimum severity
     * @return filtered list of recommendations
     */
    public List<UnifiedRecommendation> getRecommendationsBySeverity(String instanceName,
                                                                      UnifiedRecommendation.Severity severity) {
        return getRecommendations(instanceName).stream()
                .filter(r -> r.getSeverity().getWeight() >= severity.getWeight())
                .collect(Collectors.toList());
    }

    /**
     * Get top N recommendations by priority.
     *
     * @param instanceName the PostgreSQL instance name
     * @param limit maximum number of recommendations
     * @return top recommendations
     */
    public List<UnifiedRecommendation> getTopRecommendations(String instanceName, int limit) {
        return getRecommendations(instanceName).stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Get recommendation summary counts.
     *
     * @param instanceName the PostgreSQL instance name
     * @return map of severity to count
     */
    public Map<UnifiedRecommendation.Severity, Integer> getSummary(String instanceName) {
        Map<UnifiedRecommendation.Severity, Integer> summary = new EnumMap<>(UnifiedRecommendation.Severity.class);
        for (UnifiedRecommendation.Severity s : UnifiedRecommendation.Severity.values()) {
            summary.put(s, 0);
        }

        for (UnifiedRecommendation rec : getRecommendations(instanceName)) {
            summary.merge(rec.getSeverity(), 1, Integer::sum);
        }

        return summary;
    }

    /**
     * Mark a recommendation as applied.
     *
     * @param instanceName the PostgreSQL instance name
     * @param recommendationId the recommendation ID
     * @param username the user who applied it
     */
    public void markApplied(String instanceName, long recommendationId, String username) {
        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            String sql = """
                UPDATE pgconsole.unified_recommendation
                SET status = 'APPLIED', applied_at = NOW(), applied_by = ?
                WHERE id = ? AND instance_id = ?
                """;

            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, username);
                stmt.setLong(2, recommendationId);
                stmt.setString(3, instanceName);
                stmt.executeUpdate();
            }

        } catch (Exception e) {
            LOG.errorf(e, "Error marking recommendation %d as applied", recommendationId);
        }
    }

    /**
     * Dismiss a recommendation.
     *
     * @param instanceName the PostgreSQL instance name
     * @param recommendationId the recommendation ID
     * @param username the user who dismissed it
     * @param reason the reason for dismissal
     */
    public void dismiss(String instanceName, long recommendationId, String username, String reason) {
        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            String sql = """
                UPDATE pgconsole.unified_recommendation
                SET status = 'DISMISSED', dismissed_at = NOW(), dismissed_by = ?, dismiss_reason = ?
                WHERE id = ? AND instance_id = ?
                """;

            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, username);
                stmt.setString(2, reason);
                stmt.setLong(3, recommendationId);
                stmt.setString(4, instanceName);
                stmt.executeUpdate();
            }

        } catch (Exception e) {
            LOG.errorf(e, "Error dismissing recommendation %d", recommendationId);
        }
    }

    /**
     * Defer a recommendation to a later date.
     *
     * @param instanceName the PostgreSQL instance name
     * @param recommendationId the recommendation ID
     * @param deferUntil the date to defer until
     */
    public void defer(String instanceName, long recommendationId, LocalDate deferUntil) {
        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            String sql = """
                UPDATE pgconsole.unified_recommendation
                SET status = 'DEFERRED', deferred_until = ?
                WHERE id = ? AND instance_id = ?
                """;

            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setDate(1, java.sql.Date.valueOf(deferUntil));
                stmt.setLong(2, recommendationId);
                stmt.setString(3, instanceName);
                stmt.executeUpdate();
            }

        } catch (Exception e) {
            LOG.errorf(e, "Error deferring recommendation %d", recommendationId);
        }
    }

    // Private conversion methods

    private UnifiedRecommendation convertIndexRecommendation(String instanceName, IndexRecommendation ir) {
        UnifiedRecommendation ur = new UnifiedRecommendation();
        ur.setInstanceId(instanceName);
        ur.setSource(UnifiedRecommendation.Source.INDEX_ADVISOR);
        ur.setRecommendationType(ir.getType().name());
        ur.setTitle(ir.getTypeDisplay() + ": " + ir.getTableName());
        ur.setDescription(ir.getRecommendation());
        ur.setRationale(ir.getRationale());

        // Map severity
        ur.setSeverity(mapIndexSeverity(ir.getSeverity()));

        // Estimate impact and effort
        ur.setEstimatedImpact(estimateIndexImpact(ir));
        ur.setEstimatedEffort(estimateIndexEffort(ir));

        // Set suggested SQL
        ur.setSuggestedSql(ir.getSuggestedAction());

        // Set affected objects
        ur.setAffectedTables(List.of(ir.getTableName()));
        if (ir.getIndexName() != null) {
            ur.setAffectedIndexes(List.of(ir.getIndexName()));
        }

        // Calculate priority score
        ur.setPriorityScore(UnifiedRecommendation.calculatePriorityScore(
                ur.getSeverity(), ur.getEstimatedImpact(), ur.getEstimatedEffort()));

        return ur;
    }

    private UnifiedRecommendation convertMaintenanceRecommendation(String instanceName, TableMaintenanceRecommendation mr) {
        UnifiedRecommendation ur = new UnifiedRecommendation();
        ur.setInstanceId(instanceName);
        ur.setSource(UnifiedRecommendation.Source.TABLE_MAINTENANCE);
        ur.setRecommendationType(mr.getType().name());
        ur.setTitle(mr.getTypeDisplay() + ": " + mr.getTableName());
        ur.setDescription(mr.getRecommendation());
        ur.setRationale(mr.getRationale());

        // Map severity
        ur.setSeverity(mapMaintenanceSeverity(mr.getSeverity()));

        // Estimate impact and effort
        ur.setEstimatedImpact(estimateMaintenanceImpact(mr));
        ur.setEstimatedEffort(UnifiedRecommendation.Effort.MINIMAL);

        // Set suggested SQL
        ur.setSuggestedSql(generateMaintenanceSql(mr));

        // Set affected objects
        ur.setAffectedTables(List.of(mr.getTableName()));

        // Calculate priority score
        ur.setPriorityScore(UnifiedRecommendation.calculatePriorityScore(
                ur.getSeverity(), ur.getEstimatedImpact(), ur.getEstimatedEffort()));

        return ur;
    }

    private UnifiedRecommendation convertQueryRegression(String instanceName, QueryRegression qr) {
        UnifiedRecommendation ur = new UnifiedRecommendation();
        ur.setInstanceId(instanceName);
        ur.setSource(UnifiedRecommendation.Source.QUERY_REGRESSION);
        ur.setRecommendationType("REGRESSION");
        ur.setTitle("Query Regression: " + qr.getMeanTimeChangeFormatted());
        ur.setDescription("Query performance degraded by " + qr.getMeanTimeChangeFormatted());
        ur.setRationale(String.format("Mean time increased from %s to %s (%s calls)",
                qr.getPreviousMeanTimeFormatted(),
                formatDuration(qr.getCurrentMeanTime()),
                qr.getCurrentCalls()));

        // Map severity
        ur.setSeverity(mapRegressionSeverity(qr.getSeverity()));

        // Set impact and effort
        ur.setEstimatedImpact(UnifiedRecommendation.Impact.HIGH);
        ur.setEstimatedEffort(UnifiedRecommendation.Effort.MEDIUM);

        // Set affected queries
        String queryPreview = qr.getQueryText();
        if (queryPreview != null && queryPreview.length() > 100) {
            queryPreview = queryPreview.substring(0, 100) + "...";
        }
        ur.setAffectedQueries(queryPreview != null ? List.of(queryPreview) : List.of());

        // Calculate priority score
        ur.setPriorityScore(UnifiedRecommendation.calculatePriorityScore(
                ur.getSeverity(), ur.getEstimatedImpact(), ur.getEstimatedEffort()));

        return ur;
    }

    private UnifiedRecommendation convertAnomaly(String instanceName, DetectedAnomaly anomaly) {
        UnifiedRecommendation ur = new UnifiedRecommendation();
        ur.setInstanceId(instanceName);
        ur.setSource(UnifiedRecommendation.Source.ANOMALY);
        ur.setRecommendationType("ANOMALY");
        ur.setTitle("Anomaly: " + anomaly.getMetricName());
        ur.setDescription(anomaly.getDeviationDisplay() + " baseline");
        ur.setRationale(anomaly.getRootCauseSuggestion());

        // Map severity
        ur.setSeverity(mapAnomalySeverity(anomaly.getSeverity()));

        // Set impact and effort
        ur.setEstimatedImpact(UnifiedRecommendation.Impact.HIGH);
        ur.setEstimatedEffort(UnifiedRecommendation.Effort.MEDIUM);

        // Calculate priority score
        ur.setPriorityScore(UnifiedRecommendation.calculatePriorityScore(
                ur.getSeverity(), ur.getEstimatedImpact(), ur.getEstimatedEffort()));

        return ur;
    }

    private List<UnifiedRecommendation> getConfigTuningSuggestions(String instanceName) {
        List<UnifiedRecommendation> suggestions = new ArrayList<>();

        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            // Check shared_buffers vs RAM
            suggestions.addAll(checkSharedBuffers(ds, instanceName));

            // Check work_mem
            suggestions.addAll(checkWorkMem(ds, instanceName));

            // Check autovacuum settings
            suggestions.addAll(checkAutovacuum(ds, instanceName));

        } catch (Exception e) {
            LOG.debugf(e, "Error getting config tuning suggestions");
        }

        return suggestions;
    }

    private List<UnifiedRecommendation> checkSharedBuffers(DataSource ds, String instanceName) {
        List<UnifiedRecommendation> suggestions = new ArrayList<>();

        String sql = """
            SELECT current_setting('shared_buffers') as shared_buffers,
                   pg_size_bytes(current_setting('shared_buffers')) as shared_buffers_bytes
            """;

        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                long sharedBuffersBytes = rs.getLong("shared_buffers_bytes");
                String sharedBuffers = rs.getString("shared_buffers");

                // Very small shared_buffers (less than 128MB) on production
                if (sharedBuffersBytes < 128 * 1024 * 1024) {
                    UnifiedRecommendation ur = new UnifiedRecommendation();
                    ur.setInstanceId(instanceName);
                    ur.setSource(UnifiedRecommendation.Source.CONFIG_TUNING);
                    ur.setRecommendationType("SHARED_BUFFERS");
                    ur.setTitle("Increase shared_buffers");
                    ur.setDescription("shared_buffers is set to " + sharedBuffers +
                            ", which may be too low for production workloads");
                    ur.setRationale("Recommended to set shared_buffers to 25% of available RAM, " +
                            "typically at least 1GB for production databases");
                    ur.setSeverity(UnifiedRecommendation.Severity.MEDIUM);
                    ur.setEstimatedImpact(UnifiedRecommendation.Impact.HIGH);
                    ur.setEstimatedEffort(UnifiedRecommendation.Effort.LOW);
                    ur.setSuggestedConfig(Map.of("shared_buffers", "1GB"));
                    ur.setPriorityScore(UnifiedRecommendation.calculatePriorityScore(
                            ur.getSeverity(), ur.getEstimatedImpact(), ur.getEstimatedEffort()));

                    suggestions.add(ur);
                }
            }

        } catch (Exception e) {
            LOG.debugf(e, "Error checking shared_buffers");
        }

        return suggestions;
    }

    private List<UnifiedRecommendation> checkWorkMem(DataSource ds, String instanceName) {
        // Check if work_mem is too low based on temp file usage
        return new ArrayList<>();  // Simplified for now
    }

    private List<UnifiedRecommendation> checkAutovacuum(DataSource ds, String instanceName) {
        // Check autovacuum settings
        return new ArrayList<>();  // Simplified for now
    }

    // Severity mapping methods

    private UnifiedRecommendation.Severity mapIndexSeverity(IndexRecommendation.Severity severity) {
        return switch (severity) {
            case HIGH -> UnifiedRecommendation.Severity.HIGH;
            case MEDIUM -> UnifiedRecommendation.Severity.MEDIUM;
            case LOW -> UnifiedRecommendation.Severity.LOW;
        };
    }

    private UnifiedRecommendation.Severity mapMaintenanceSeverity(TableMaintenanceRecommendation.Severity severity) {
        return switch (severity) {
            case CRITICAL -> UnifiedRecommendation.Severity.CRITICAL;
            case HIGH -> UnifiedRecommendation.Severity.HIGH;
            case MEDIUM -> UnifiedRecommendation.Severity.MEDIUM;
            case LOW -> UnifiedRecommendation.Severity.LOW;
        };
    }

    private UnifiedRecommendation.Severity mapRegressionSeverity(QueryRegression.Severity severity) {
        return switch (severity) {
            case CRITICAL -> UnifiedRecommendation.Severity.CRITICAL;
            case HIGH -> UnifiedRecommendation.Severity.HIGH;
            case MEDIUM -> UnifiedRecommendation.Severity.MEDIUM;
            case LOW -> UnifiedRecommendation.Severity.LOW;
        };
    }

    private UnifiedRecommendation.Severity mapAnomalySeverity(DetectedAnomaly.Severity severity) {
        return switch (severity) {
            case CRITICAL -> UnifiedRecommendation.Severity.CRITICAL;
            case HIGH -> UnifiedRecommendation.Severity.HIGH;
            case MEDIUM -> UnifiedRecommendation.Severity.MEDIUM;
            case LOW -> UnifiedRecommendation.Severity.LOW;
        };
    }

    // Impact estimation methods

    private UnifiedRecommendation.Impact estimateIndexImpact(IndexRecommendation ir) {
        if (ir.getType() == IndexRecommendation.RecommendationType.MISSING_INDEX && ir.getSeqScanRatio() > 0.9) {
            return UnifiedRecommendation.Impact.HIGH;
        } else if (ir.getType() == IndexRecommendation.RecommendationType.UNUSED_INDEX) {
            return UnifiedRecommendation.Impact.MEDIUM;
        }
        return UnifiedRecommendation.Impact.LOW;
    }

    private UnifiedRecommendation.Effort estimateIndexEffort(IndexRecommendation ir) {
        if (ir.getType() == IndexRecommendation.RecommendationType.MISSING_INDEX) {
            return UnifiedRecommendation.Effort.LOW;  // Creating index is quick
        } else if (ir.getType() == IndexRecommendation.RecommendationType.UNUSED_INDEX) {
            return UnifiedRecommendation.Effort.MINIMAL;  // Dropping unused index is safe
        }
        return UnifiedRecommendation.Effort.MEDIUM;
    }

    private UnifiedRecommendation.Impact estimateMaintenanceImpact(TableMaintenanceRecommendation mr) {
        if (mr.getDeadTupleRatio() > 0.3) {
            return UnifiedRecommendation.Impact.HIGH;
        } else if (mr.getDeadTupleRatio() > 0.1) {
            return UnifiedRecommendation.Impact.MEDIUM;
        }
        return UnifiedRecommendation.Impact.LOW;
    }

    private String generateMaintenanceSql(TableMaintenanceRecommendation mr) {
        return switch (mr.getType()) {
            case VACUUM -> "VACUUM ANALYSE " + mr.getSchemaName() + "." + mr.getTableName();
            case ANALYSE -> "ANALYSE " + mr.getSchemaName() + "." + mr.getTableName();
            case VACUUM_FULL -> "VACUUM FULL " + mr.getSchemaName() + "." + mr.getTableName();
            case REINDEX -> "REINDEX TABLE " + mr.getSchemaName() + "." + mr.getTableName();
        };
    }

    private String formatDuration(double ms) {
        if (ms < 1) {
            return String.format("%.2f ms", ms);
        } else if (ms < 1000) {
            return String.format("%.1f ms", ms);
        } else if (ms < 60000) {
            return String.format("%.2f s", ms / 1000);
        } else {
            return String.format("%.1f min", ms / 60000);
        }
    }
}
