package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.QueryBaseline;
import com.bovinemagnet.pgconsole.model.QueryBaseline.MovementType;
import com.bovinemagnet.pgconsole.model.QueryMetricsHistory;
import com.bovinemagnet.pgconsole.repository.HistoryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service for managing pg_stat_statements and providing baseline comparisons.
 * Supports reset with snapshot, baseline comparisons, and top movers report.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class StatementsManagementService {

    private static final Logger LOG = Logger.getLogger(StatementsManagementService.class);

    private static final double SIGNIFICANT_CHANGE_THRESHOLD = 10.0; // 10% change threshold
    private static final int TOP_MOVERS_LIMIT = 50;

    @Inject
    DataSourceManager dataSourceManager;

    @Inject
    HistoryRepository historyRepository;

    /**
     * Gets top movers - queries with biggest changes between periods.
     *
     * @param instanceName the database instance
     * @param windowHours the size of each comparison window
     * @return list of queries with biggest changes, sorted by impact
     */
    public List<QueryBaseline> getTopMovers(String instanceName, int windowHours) {
        List<QueryBaseline> movers = new ArrayList<>();

        try {
            // Get current period stats
            List<QueryMetricsHistory> currentPeriod = historyRepository.getAggregatedQueryMetrics(
                instanceName, windowHours, 0);

            // Get previous period stats
            List<QueryMetricsHistory> previousPeriod = historyRepository.getAggregatedQueryMetrics(
                instanceName, windowHours * 2, windowHours);

            // Build maps for lookup
            Map<String, QueryMetricsHistory> currentMap = new HashMap<>();
            for (QueryMetricsHistory m : currentPeriod) {
                currentMap.put(m.getQueryId(), m);
            }

            Map<String, QueryMetricsHistory> previousMap = new HashMap<>();
            for (QueryMetricsHistory m : previousPeriod) {
                previousMap.put(m.getQueryId(), m);
            }

            // Track all unique query IDs
            Set<String> allQueryIds = new HashSet<>();
            allQueryIds.addAll(currentMap.keySet());
            allQueryIds.addAll(previousMap.keySet());

            for (String queryId : allQueryIds) {
                QueryMetricsHistory current = currentMap.get(queryId);
                QueryMetricsHistory previous = previousMap.get(queryId);

                QueryBaseline baseline = new QueryBaseline();
                baseline.setQueryId(queryId);

                if (previous == null && current != null) {
                    // New query
                    baseline.setQueryText(current.getQueryText());
                    baseline.setCurrentCalls(current.getTotalCalls());
                    baseline.setCurrentTotalTime(current.getTotalTimeMs());
                    baseline.setCurrentMeanTime(current.getMeanTimeMs());
                    baseline.setMovementType(MovementType.NEW_QUERY);
                    baseline.setImpactScore(current.getTotalTimeMs()); // Impact = total time
                    movers.add(baseline);

                } else if (previous != null && current == null) {
                    // Removed query
                    baseline.setQueryText(previous.getQueryText());
                    baseline.setPreviousCalls(previous.getTotalCalls());
                    baseline.setPreviousTotalTime(previous.getTotalTimeMs());
                    baseline.setPreviousMeanTime(previous.getMeanTimeMs());
                    baseline.setMovementType(MovementType.REMOVED);
                    baseline.setImpactScore(previous.getTotalTimeMs()); // Impact = previous total time
                    movers.add(baseline);

                } else if (previous != null && current != null) {
                    // Existing query - compute deltas
                    baseline.setQueryText(current.getQueryText());

                    baseline.setPreviousCalls(previous.getTotalCalls());
                    baseline.setPreviousTotalTime(previous.getTotalTimeMs());
                    baseline.setPreviousMeanTime(previous.getMeanTimeMs());

                    baseline.setCurrentCalls(current.getTotalCalls());
                    baseline.setCurrentTotalTime(current.getTotalTimeMs());
                    baseline.setCurrentMeanTime(current.getMeanTimeMs());

                    // Calculate deltas
                    baseline.setCallsDelta(current.getTotalCalls() - previous.getTotalCalls());
                    baseline.setTotalTimeDelta(current.getTotalTimeMs() - previous.getTotalTimeMs());
                    baseline.setMeanTimeDelta(current.getMeanTimeMs() - previous.getMeanTimeMs());

                    // Calculate percentage changes
                    if (previous.getTotalCalls() > 0) {
                        baseline.setCallsChangePercent(
                            ((double) (current.getTotalCalls() - previous.getTotalCalls()) / previous.getTotalCalls()) * 100);
                    }
                    if (previous.getTotalTimeMs() > 0) {
                        baseline.setTotalTimeChangePercent(
                            ((current.getTotalTimeMs() - previous.getTotalTimeMs()) / previous.getTotalTimeMs()) * 100);
                    }
                    if (previous.getMeanTimeMs() > 0) {
                        baseline.setMeanTimeChangePercent(
                            ((current.getMeanTimeMs() - previous.getMeanTimeMs()) / previous.getMeanTimeMs()) * 100);
                    }

                    // Determine movement type
                    if (Math.abs(baseline.getTotalTimeChangePercent()) < SIGNIFICANT_CHANGE_THRESHOLD) {
                        baseline.setMovementType(MovementType.STABLE);
                    } else if (baseline.getTotalTimeDelta() > 0) {
                        baseline.setMovementType(MovementType.INCREASED);
                    } else {
                        baseline.setMovementType(MovementType.DECREASED);
                    }

                    // Impact score based on absolute change in total time
                    baseline.setImpactScore(Math.abs(baseline.getTotalTimeDelta()));

                    // Only add if significant change
                    if (baseline.getMovementType() != MovementType.STABLE) {
                        movers.add(baseline);
                    }
                }
            }

            // Sort by impact score descending
            movers.sort((a, b) -> Double.compare(b.getImpactScore(), a.getImpactScore()));

            // Limit results
            if (movers.size() > TOP_MOVERS_LIMIT) {
                movers = new ArrayList<>(movers.subList(0, TOP_MOVERS_LIMIT));
            }

        } catch (Exception e) {
            LOG.warnf("Failed to get top movers for %s: %s", instanceName, e.getMessage());
        }

        return movers;
    }

    /**
     * Gets summary statistics for pg_stat_statements.
     */
    public StatementsSummary getSummary(String instanceName) {
        StatementsSummary summary = new StatementsSummary();

        String sql = """
            SELECT
                COUNT(*) as total_queries,
                SUM(calls) as total_calls,
                SUM(total_exec_time) as total_exec_time,
                AVG(mean_exec_time) as avg_mean_time,
                MAX(mean_exec_time) as max_mean_time,
                pg_stat_statements_reset() IS NOT NULL as can_reset
            FROM pg_stat_statements
            WHERE dbid = (SELECT oid FROM pg_database WHERE datname = current_database())
            """;

        // First check if pg_stat_statements exists
        String checkSql = """
            SELECT EXISTS (
                SELECT 1 FROM pg_extension WHERE extname = 'pg_stat_statements'
            ) as extension_exists
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection()) {
            // Check extension exists
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(checkSql)) {
                if (rs.next() && !rs.getBoolean("extension_exists")) {
                    summary.setExtensionAvailable(false);
                    return summary;
                }
            }

            summary.setExtensionAvailable(true);

            // Get summary stats
            String statsSql = """
                SELECT
                    COUNT(*) as total_queries,
                    COALESCE(SUM(calls), 0) as total_calls,
                    COALESCE(SUM(total_exec_time), 0) as total_exec_time,
                    COALESCE(AVG(mean_exec_time), 0) as avg_mean_time,
                    COALESCE(MAX(mean_exec_time), 0) as max_mean_time
                FROM pg_stat_statements
                WHERE dbid = (SELECT oid FROM pg_database WHERE datname = current_database())
                """;

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(statsSql)) {
                if (rs.next()) {
                    summary.setTotalQueries(rs.getInt("total_queries"));
                    summary.setTotalCalls(rs.getLong("total_calls"));
                    summary.setTotalExecTimeMs(rs.getDouble("total_exec_time"));
                    summary.setAvgMeanTimeMs(rs.getDouble("avg_mean_time"));
                    summary.setMaxMeanTimeMs(rs.getDouble("max_mean_time"));
                }
            }

            // Get stats reset timestamp
            String resetTimeSql = """
                SELECT stats_reset FROM pg_stat_statements_info
                """;

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(resetTimeSql)) {
                if (rs.next()) {
                    var resetTime = rs.getTimestamp("stats_reset");
                    if (resetTime != null) {
                        summary.setLastReset(resetTime.toInstant());
                    }
                }
            } catch (SQLException e) {
                // pg_stat_statements_info may not exist in older versions
                LOG.debugf("Could not get stats_reset time: %s", e.getMessage());
            }

        } catch (SQLException e) {
            LOG.warnf("Failed to get statements summary for %s: %s", instanceName, e.getMessage());
        }

        return summary;
    }

    /**
     * Resets pg_stat_statements. Should be called after capturing a snapshot.
     *
     * @param instanceName the database instance
     * @return true if reset succeeded
     */
    public boolean resetStatements(String instanceName) {
        String sql = "SELECT pg_stat_statements_reset()";

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            LOG.infof("Reset pg_stat_statements for instance: %s", instanceName);
            return true;
        } catch (SQLException e) {
            LOG.warnf("Failed to reset pg_stat_statements for %s: %s", instanceName, e.getMessage());
            return false;
        }
    }

    /**
     * Summary statistics for pg_stat_statements.
     */
    public static class StatementsSummary {
        private boolean extensionAvailable;
        private int totalQueries;
        private long totalCalls;
        private double totalExecTimeMs;
        private double avgMeanTimeMs;
        private double maxMeanTimeMs;
        private Instant lastReset;

        public boolean isExtensionAvailable() {
            return extensionAvailable;
        }

        public void setExtensionAvailable(boolean extensionAvailable) {
            this.extensionAvailable = extensionAvailable;
        }

        public int getTotalQueries() {
            return totalQueries;
        }

        public void setTotalQueries(int totalQueries) {
            this.totalQueries = totalQueries;
        }

        public long getTotalCalls() {
            return totalCalls;
        }

        public void setTotalCalls(long totalCalls) {
            this.totalCalls = totalCalls;
        }

        public double getTotalExecTimeMs() {
            return totalExecTimeMs;
        }

        public void setTotalExecTimeMs(double totalExecTimeMs) {
            this.totalExecTimeMs = totalExecTimeMs;
        }

        public String getTotalExecTimeFormatted() {
            if (totalExecTimeMs >= 3600000) {
                return String.format("%.1f hours", totalExecTimeMs / 3600000);
            } else if (totalExecTimeMs >= 60000) {
                return String.format("%.1f minutes", totalExecTimeMs / 60000);
            } else if (totalExecTimeMs >= 1000) {
                return String.format("%.1f seconds", totalExecTimeMs / 1000);
            }
            return String.format("%.0f ms", totalExecTimeMs);
        }

        public double getAvgMeanTimeMs() {
            return avgMeanTimeMs;
        }

        public void setAvgMeanTimeMs(double avgMeanTimeMs) {
            this.avgMeanTimeMs = avgMeanTimeMs;
        }

        public String getAvgMeanTimeFormatted() {
            if (avgMeanTimeMs >= 1000) {
                return String.format("%.2f s", avgMeanTimeMs / 1000);
            }
            return String.format("%.2f ms", avgMeanTimeMs);
        }

        public double getMaxMeanTimeMs() {
            return maxMeanTimeMs;
        }

        public void setMaxMeanTimeMs(double maxMeanTimeMs) {
            this.maxMeanTimeMs = maxMeanTimeMs;
        }

        public String getMaxMeanTimeFormatted() {
            if (maxMeanTimeMs >= 1000) {
                return String.format("%.2f s", maxMeanTimeMs / 1000);
            }
            return String.format("%.2f ms", maxMeanTimeMs);
        }

        public Instant getLastReset() {
            return lastReset;
        }

        public void setLastReset(Instant lastReset) {
            this.lastReset = lastReset;
        }

        public String getLastResetFormatted() {
            if (lastReset == null) {
                return "Never / Unknown";
            }
            long hoursSince = java.time.Duration.between(lastReset, Instant.now()).toHours();
            if (hoursSince < 1) {
                return "Less than an hour ago";
            } else if (hoursSince < 24) {
                return hoursSince + " hours ago";
            } else {
                long daysSince = hoursSince / 24;
                return daysSince + " day" + (daysSince > 1 ? "s" : "") + " ago";
            }
        }

        public String getTotalCallsFormatted() {
            if (totalCalls >= 1_000_000_000) {
                return String.format("%.1fB", totalCalls / 1_000_000_000.0);
            } else if (totalCalls >= 1_000_000) {
                return String.format("%.1fM", totalCalls / 1_000_000.0);
            } else if (totalCalls >= 1_000) {
                return String.format("%.1fK", totalCalls / 1_000.0);
            }
            return String.valueOf(totalCalls);
        }
    }
}
