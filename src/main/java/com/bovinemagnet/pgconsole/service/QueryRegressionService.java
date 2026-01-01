package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.config.InstanceConfig;
import com.bovinemagnet.pgconsole.model.QueryMetricsHistory;
import com.bovinemagnet.pgconsole.model.QueryRegression;
import com.bovinemagnet.pgconsole.model.QueryRegression.Severity;
import com.bovinemagnet.pgconsole.repository.HistoryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for detecting query performance regressions.
 * Compares query performance between time windows to identify slowdowns.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class QueryRegressionService {

    private static final Logger LOG = Logger.getLogger(QueryRegressionService.class);

    // Default thresholds
    private static final int DEFAULT_REGRESSION_THRESHOLD = 50;  // 50% slower
    private static final int DEFAULT_WINDOW_HOURS = 24;          // Compare 24h periods

    @Inject
    HistoryRepository historyRepository;

    @Inject
    InstanceConfig config;

    /**
     * Detects query performance regressions using default thresholds.
     * <p>
     * Compares query performance between the current 24-hour period and the previous
     * 24-hour period, flagging queries with mean execution time increases of 50% or more.
     *
     * @param instanceName the database instance identifier
     * @return list of queries showing performance regression, sorted by severity (CRITICAL first)
     *         then by percentage change descending
     * @see #detectRegressions(String, int, int)
     */
    public List<QueryRegression> detectRegressions(String instanceName) {
        return detectRegressions(instanceName, DEFAULT_WINDOW_HOURS, DEFAULT_REGRESSION_THRESHOLD);
    }

    /**
     * Detects query performance regressions with configurable time windows and thresholds.
     * <p>
     * Compares aggregated query metrics between two time periods:
     * <ul>
     * <li>Current period: last {@code windowHours} hours</li>
     * <li>Previous period: from {@code windowHours} to {@code 2 * windowHours} hours ago</li>
     * </ul>
     * Queries are matched by query ID (normalised fingerprint). Only queries present in both
     * periods are compared. Queries with very low mean time (less than 1ms) are excluded to
     * avoid noise from fast queries where timing variations are insignificant.
     * <p>
     * Severity levels are assigned based on percentage increase:
     * CRITICAL (200%+), HIGH (100-200%), MEDIUM (50-100%), LOW (below 50%).
     *
     * @param instanceName the database instance identifier
     * @param windowHours the size of each comparison window in hours
     * @param thresholdPercent minimum percentage increase to flag as regression (e.g., 50 for 50%)
     * @return list of queries showing performance regression, sorted by severity then by change percentage
     */
    public List<QueryRegression> detectRegressions(String instanceName, int windowHours, int thresholdPercent) {
        List<QueryRegression> regressions = new ArrayList<>();

        try {
            // Get metrics for current period (last windowHours)
            List<QueryMetricsHistory> currentPeriod = historyRepository.getAggregatedQueryMetrics(
                instanceName, windowHours, 0);

            // Get metrics for previous period (windowHours to 2*windowHours ago)
            List<QueryMetricsHistory> previousPeriod = historyRepository.getAggregatedQueryMetrics(
                instanceName, windowHours * 2, windowHours);

            // Build map of previous period by query_id
            Map<String, QueryMetricsHistory> previousMap = new HashMap<>();
            for (QueryMetricsHistory m : previousPeriod) {
                previousMap.put(m.getQueryId(), m);
            }

            // Compare and find regressions
            for (QueryMetricsHistory current : currentPeriod) {
                QueryMetricsHistory previous = previousMap.get(current.getQueryId());

                if (previous == null) {
                    // Query is new, no baseline to compare
                    continue;
                }

                // Skip queries with very low mean time (< 1ms) as changes are noise
                if (previous.getMeanTimeMs() < 1.0) {
                    continue;
                }

                double changePercent = calculatePercentChange(previous.getMeanTimeMs(), current.getMeanTimeMs());

                // Only report if above threshold
                if (changePercent >= thresholdPercent) {
                    QueryRegression regression = new QueryRegression();
                    regression.setQueryId(current.getQueryId());
                    regression.setQueryText(current.getQueryText());

                    regression.setPreviousMeanTime(previous.getMeanTimeMs());
                    regression.setPreviousCalls(previous.getTotalCalls());
                    regression.setPreviousTotalTime(previous.getTotalTimeMs());

                    regression.setCurrentMeanTime(current.getMeanTimeMs());
                    regression.setCurrentCalls(current.getTotalCalls());
                    regression.setCurrentTotalTime(current.getTotalTimeMs());

                    regression.setMeanTimeDelta(current.getMeanTimeMs() - previous.getMeanTimeMs());
                    regression.setMeanTimeChangePercent(changePercent);
                    regression.setCallsDelta(current.getTotalCalls() - previous.getTotalCalls());

                    double totalTimeChange = calculatePercentChange(previous.getTotalTimeMs(), current.getTotalTimeMs());
                    regression.setTotalTimeChangePercent(totalTimeChange);

                    regression.setSeverity(QueryRegression.calculateSeverity(changePercent));

                    regressions.add(regression);
                }
            }

            // Sort by severity (CRITICAL first), then by change percent descending
            regressions.sort((a, b) -> {
                int severityCompare = a.getSeverity().compareTo(b.getSeverity());
                if (severityCompare != 0) return severityCompare;
                return Double.compare(b.getMeanTimeChangePercent(), a.getMeanTimeChangePercent());
            });

        } catch (Exception e) {
            LOG.warnf("Failed to detect regressions for %s: %s", instanceName, e.getMessage());
        }

        return regressions;
    }

    /**
     * Detects query performance improvements (queries that became faster).
     * <p>
     * Identifies queries where mean execution time decreased by at least the specified threshold
     * percentage. Uses the same time window comparison logic as regression detection, but looks
     * for negative performance changes (faster execution).
     * <p>
     * All improvements are assigned LOW severity as they represent positive changes.
     *
     * @param instanceName the database instance identifier
     * @param windowHours the size of each comparison window in hours
     * @param thresholdPercent minimum percentage decrease to flag as improvement (positive value, e.g., 50 for 50% faster)
     * @return list of queries showing performance improvement, sorted by improvement magnitude (most improved first)
     */
    public List<QueryRegression> detectImprovements(String instanceName, int windowHours, int thresholdPercent) {
        List<QueryRegression> improvements = new ArrayList<>();

        try {
            List<QueryMetricsHistory> currentPeriod = historyRepository.getAggregatedQueryMetrics(
                instanceName, windowHours, 0);

            List<QueryMetricsHistory> previousPeriod = historyRepository.getAggregatedQueryMetrics(
                instanceName, windowHours * 2, windowHours);

            Map<String, QueryMetricsHistory> previousMap = new HashMap<>();
            for (QueryMetricsHistory m : previousPeriod) {
                previousMap.put(m.getQueryId(), m);
            }

            for (QueryMetricsHistory current : currentPeriod) {
                QueryMetricsHistory previous = previousMap.get(current.getQueryId());

                if (previous == null || previous.getMeanTimeMs() < 1.0) {
                    continue;
                }

                double changePercent = calculatePercentChange(previous.getMeanTimeMs(), current.getMeanTimeMs());

                // Improvement is a negative change
                if (changePercent <= -thresholdPercent) {
                    QueryRegression improvement = new QueryRegression();
                    improvement.setQueryId(current.getQueryId());
                    improvement.setQueryText(current.getQueryText());

                    improvement.setPreviousMeanTime(previous.getMeanTimeMs());
                    improvement.setPreviousCalls(previous.getTotalCalls());
                    improvement.setPreviousTotalTime(previous.getTotalTimeMs());

                    improvement.setCurrentMeanTime(current.getMeanTimeMs());
                    improvement.setCurrentCalls(current.getTotalCalls());
                    improvement.setCurrentTotalTime(current.getTotalTimeMs());

                    improvement.setMeanTimeDelta(current.getMeanTimeMs() - previous.getMeanTimeMs());
                    improvement.setMeanTimeChangePercent(changePercent);
                    improvement.setCallsDelta(current.getTotalCalls() - previous.getTotalCalls());

                    double totalTimeChange = calculatePercentChange(previous.getTotalTimeMs(), current.getTotalTimeMs());
                    improvement.setTotalTimeChangePercent(totalTimeChange);

                    // Use LOW severity for improvements (they're positive)
                    improvement.setSeverity(Severity.LOW);

                    improvements.add(improvement);
                }
            }

            // Sort by improvement magnitude (most improved first)
            improvements.sort((a, b) -> Double.compare(a.getMeanTimeChangePercent(), b.getMeanTimeChangePercent()));

        } catch (Exception e) {
            LOG.warnf("Failed to detect improvements for %s: %s", instanceName, e.getMessage());
        }

        return improvements;
    }

    /**
     * Retrieves summary statistics for query regression detection.
     * <p>
     * Computes aggregate counts of regressions and improvements, breaking down
     * regressions by severity level (CRITICAL, HIGH, MEDIUM).
     *
     * @param instanceName the database instance identifier
     * @param windowHours the size of each comparison window in hours
     * @param thresholdPercent minimum percentage change threshold
     * @return summary statistics including total counts and severity breakdown
     */
    public RegressionSummary getSummary(String instanceName, int windowHours, int thresholdPercent) {
        List<QueryRegression> regressions = detectRegressions(instanceName, windowHours, thresholdPercent);
        List<QueryRegression> improvements = detectImprovements(instanceName, windowHours, thresholdPercent);

        RegressionSummary summary = new RegressionSummary();
        summary.setTotalRegressions(regressions.size());
        summary.setTotalImprovements(improvements.size());

        long critical = regressions.stream().filter(r -> r.getSeverity() == Severity.CRITICAL).count();
        long high = regressions.stream().filter(r -> r.getSeverity() == Severity.HIGH).count();
        long medium = regressions.stream().filter(r -> r.getSeverity() == Severity.MEDIUM).count();

        summary.setCriticalCount((int) critical);
        summary.setHighCount((int) high);
        summary.setMediumCount((int) medium);

        return summary;
    }

    /**
     * Calculates the percentage change between two values.
     * <p>
     * Returns 0 if the old value is 0 to avoid division by zero.
     *
     * @param oldValue the baseline value
     * @param newValue the new value to compare
     * @return percentage change from old to new (positive for increase, negative for decrease)
     */
    private double calculatePercentChange(double oldValue, double newValue) {
        if (oldValue == 0) {
            return 0;
        }
        return ((newValue - oldValue) / oldValue) * 100;
    }

    /**
     * Encapsulates summary statistics for query regression detection.
     * <p>
     * Provides aggregate counts of regressions and improvements with breakdown
     * by severity level for use in dashboard displays.
     */
    public static class RegressionSummary {
        private int totalRegressions;
        private int totalImprovements;
        private int criticalCount;
        private int highCount;
        private int mediumCount;

        public int getTotalRegressions() {
            return totalRegressions;
        }

        public void setTotalRegressions(int totalRegressions) {
            this.totalRegressions = totalRegressions;
        }

        public int getTotalImprovements() {
            return totalImprovements;
        }

        public void setTotalImprovements(int totalImprovements) {
            this.totalImprovements = totalImprovements;
        }

        public int getCriticalCount() {
            return criticalCount;
        }

        public void setCriticalCount(int criticalCount) {
            this.criticalCount = criticalCount;
        }

        public int getHighCount() {
            return highCount;
        }

        public void setHighCount(int highCount) {
            this.highCount = highCount;
        }

        public int getMediumCount() {
            return mediumCount;
        }

        public void setMediumCount(int mediumCount) {
            this.mediumCount = mediumCount;
        }

        public boolean hasRegressions() {
            return totalRegressions > 0;
        }
    }
}
