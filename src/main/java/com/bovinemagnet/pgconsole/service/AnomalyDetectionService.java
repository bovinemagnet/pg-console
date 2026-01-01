package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.DetectedAnomaly;
import com.bovinemagnet.pgconsole.model.MetricBaseline;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Service for detecting anomalies in database metrics.
 * <p>
 * Uses statistical baseline learning to detect significant deviations
 * from normal behaviour. Supports seasonal patterns (hourly/daily cycles)
 * and provides root cause suggestions.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class AnomalyDetectionService {

    private static final Logger LOG = Logger.getLogger(AnomalyDetectionService.class);

    // Default sigma thresholds for anomaly severity
    private static final double CRITICAL_SIGMA = 4.0;
    private static final double HIGH_SIGMA = 3.0;
    private static final double MEDIUM_SIGMA = 2.5;
    private static final double LOW_SIGMA = 2.0;

    // Metrics to monitor
    private static final List<MetricDefinition> MONITORED_METRICS = List.of(
            new MetricDefinition("total_connections", "system", "Total database connections"),
            new MetricDefinition("active_queries", "system", "Active running queries"),
            new MetricDefinition("blocked_queries", "system", "Blocked queries"),
            new MetricDefinition("cache_hit_ratio", "system", "Buffer cache hit ratio"),
            new MetricDefinition("longest_query_seconds", "system", "Longest running query duration"),
            new MetricDefinition("total_database_size_bytes", "system", "Total database size")
    );

    @Inject
    DataSourceManager dataSourceManager;

    @Inject
    AlertingService alertingService;

    /**
     * Calculate and store baselines for all monitored metrics.
     *
     * @param instanceName the PostgreSQL instance name
     * @param trainingDays number of days of historical data to use
     */
    public void calculateBaselines(String instanceName, int trainingDays) {
        LOG.infof("Calculating baselines for instance %s using %d days of data", instanceName, trainingDays);

        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            for (MetricDefinition metric : MONITORED_METRICS) {
                // Calculate overall baseline
                MetricBaseline overall = calculateBaselineForMetric(ds, instanceName, metric, trainingDays, null, null);
                if (overall != null) {
                    saveBaseline(ds, overall);
                }

                // Calculate hourly baselines for seasonal patterns
                for (int hour = 0; hour < 24; hour++) {
                    MetricBaseline hourly = calculateBaselineForMetric(ds, instanceName, metric, trainingDays, hour, null);
                    if (hourly != null) {
                        saveBaseline(ds, hourly);
                    }
                }

                // Calculate daily baselines
                for (int day = 0; day < 7; day++) {
                    MetricBaseline daily = calculateBaselineForMetric(ds, instanceName, metric, trainingDays, null, day);
                    if (daily != null) {
                        saveBaseline(ds, daily);
                    }
                }
            }

            LOG.infof("Baseline calculation completed for instance %s", instanceName);

        } catch (Exception e) {
            LOG.errorf(e, "Error calculating baselines for instance %s", instanceName);
        }
    }

    /**
     * Detect anomalies in current metric values.
     *
     * @param instanceName the PostgreSQL instance name
     * @return list of detected anomalies
     */
    public List<DetectedAnomaly> detectAnomalies(String instanceName) {
        List<DetectedAnomaly> anomalies = new ArrayList<>();

        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            // Get current metric values
            Map<String, Double> currentValues = getCurrentMetricValues(ds, instanceName);

            // Get current time context for seasonal baselines
            Calendar cal = Calendar.getInstance();
            int currentHour = cal.get(Calendar.HOUR_OF_DAY);
            int currentDay = cal.get(Calendar.DAY_OF_WEEK) - 1;  // Convert to 0-6

            for (MetricDefinition metric : MONITORED_METRICS) {
                Double currentValue = currentValues.get(metric.name);
                if (currentValue == null) {
                    continue;
                }

                // Try to find the best matching baseline (seasonal first, then overall)
                MetricBaseline baseline = findBestBaseline(ds, instanceName, metric.name, currentHour, currentDay);
                if (baseline == null || baseline.getStddev() == 0) {
                    continue;
                }

                // Calculate deviation
                double sigma = baseline.calculateSigma(currentValue);

                // Check if anomalous
                if (Math.abs(sigma) >= LOW_SIGMA) {
                    DetectedAnomaly anomaly = createAnomaly(instanceName, metric, currentValue, baseline, sigma);

                    // Find correlated metrics
                    anomaly.setCorrelatedMetrics(findCorrelatedAnomalies(currentValues, ds, instanceName, metric.name, currentHour, currentDay));

                    // Generate root cause suggestion
                    anomaly.setRootCauseSuggestion(generateRootCauseSuggestion(metric.name, sigma, anomaly.getDirection()));

                    anomalies.add(anomaly);

                    // Save to database
                    saveAnomaly(ds, anomaly);

                    // Fire alert for critical/high anomalies
                    if (anomaly.getSeverity() == DetectedAnomaly.Severity.CRITICAL
                            || anomaly.getSeverity() == DetectedAnomaly.Severity.HIGH) {
                        fireAnomalyAlert(instanceName, anomaly);
                    }
                }
            }

        } catch (Exception e) {
            LOG.errorf(e, "Error detecting anomalies for instance %s", instanceName);
        }

        return anomalies;
    }

    /**
     * Get open (unresolved) anomalies for an instance.
     *
     * @param instanceName the PostgreSQL instance name
     * @return list of open anomalies
     */
    public List<DetectedAnomaly> getOpenAnomalies(String instanceName) {
        List<DetectedAnomaly> anomalies = new ArrayList<>();

        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            String sql = """
                SELECT id, metric_name, metric_category, detected_at, anomaly_value,
                       baseline_mean, baseline_stddev, deviation_sigma, severity,
                       anomaly_type, direction, root_cause_suggestion,
                       acknowledged_at, acknowledged_by
                FROM pgconsole.detected_anomaly
                WHERE instance_id = ? AND resolved_at IS NULL
                ORDER BY
                    CASE severity
                        WHEN 'CRITICAL' THEN 1
                        WHEN 'HIGH' THEN 2
                        WHEN 'MEDIUM' THEN 3
                        ELSE 4
                    END,
                    detected_at DESC
                """;

            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, instanceName);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        DetectedAnomaly anomaly = mapAnomaly(rs, instanceName);
                        anomalies.add(anomaly);
                    }
                }
            }

        } catch (Exception e) {
            LOG.errorf(e, "Error getting open anomalies for instance %s", instanceName);
        }

        return anomalies;
    }

    /**
     * Get anomaly history for an instance.
     *
     * @param instanceName the PostgreSQL instance name
     * @param hours number of hours to look back
     * @return list of anomalies
     */
    public List<DetectedAnomaly> getAnomalyHistory(String instanceName, int hours) {
        List<DetectedAnomaly> anomalies = new ArrayList<>();

        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            String sql = """
                SELECT id, metric_name, metric_category, detected_at, anomaly_value,
                       baseline_mean, baseline_stddev, deviation_sigma, severity,
                       anomaly_type, direction, root_cause_suggestion,
                       acknowledged_at, acknowledged_by, resolved_at, resolution_notes
                FROM pgconsole.detected_anomaly
                WHERE instance_id = ? AND detected_at > NOW() - INTERVAL '%d hours'
                ORDER BY detected_at DESC
                LIMIT 100
                """.formatted(hours);

            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, instanceName);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        anomalies.add(mapAnomaly(rs, instanceName));
                    }
                }
            }

        } catch (Exception e) {
            LOG.errorf(e, "Error getting anomaly history for instance %s", instanceName);
        }

        return anomalies;
    }

    /**
     * Acknowledge an anomaly.
     *
     * @param instanceName the PostgreSQL instance name
     * @param anomalyId the anomaly ID
     * @param username the acknowledging user
     */
    public void acknowledgeAnomaly(String instanceName, long anomalyId, String username) {
        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            String sql = """
                UPDATE pgconsole.detected_anomaly
                SET acknowledged_at = NOW(), acknowledged_by = ?
                WHERE id = ? AND instance_id = ?
                """;

            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, username);
                stmt.setLong(2, anomalyId);
                stmt.setString(3, instanceName);
                stmt.executeUpdate();
            }

        } catch (Exception e) {
            LOG.errorf(e, "Error acknowledging anomaly %d", anomalyId);
        }
    }

    /**
     * Resolve an anomaly.
     *
     * @param instanceName the PostgreSQL instance name
     * @param anomalyId the anomaly ID
     * @param notes resolution notes
     */
    public void resolveAnomaly(String instanceName, long anomalyId, String notes) {
        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            String sql = """
                UPDATE pgconsole.detected_anomaly
                SET resolved_at = NOW(), resolution_notes = ?
                WHERE id = ? AND instance_id = ?
                """;

            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, notes);
                stmt.setLong(2, anomalyId);
                stmt.setString(3, instanceName);
                stmt.executeUpdate();
            }

        } catch (Exception e) {
            LOG.errorf(e, "Error resolving anomaly %d", anomalyId);
        }
    }

    /**
     * Get anomaly summary counts.
     */
    public Map<DetectedAnomaly.Severity, Integer> getAnomalySummary(String instanceName) {
        Map<DetectedAnomaly.Severity, Integer> summary = new EnumMap<>(DetectedAnomaly.Severity.class);
        for (DetectedAnomaly.Severity s : DetectedAnomaly.Severity.values()) {
            summary.put(s, 0);
        }

        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            String sql = """
                SELECT severity, COUNT(*) as cnt
                FROM pgconsole.detected_anomaly
                WHERE instance_id = ? AND resolved_at IS NULL
                GROUP BY severity
                """;

            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, instanceName);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String severityStr = rs.getString("severity");
                        int count = rs.getInt("cnt");
                        try {
                            DetectedAnomaly.Severity severity = DetectedAnomaly.Severity.valueOf(severityStr);
                            summary.put(severity, count);
                        } catch (IllegalArgumentException e) {
                            // Unknown severity, skip
                        }
                    }
                }
            }

        } catch (Exception e) {
            LOG.errorf(e, "Error getting anomaly summary for instance %s", instanceName);
        }

        return summary;
    }

    // Private helper methods

    private MetricBaseline calculateBaselineForMetric(DataSource ds, String instanceName,
                                                       MetricDefinition metric, int days,
                                                       Integer hour, Integer dayOfWeek) {
        String sql = """
            SELECT
                AVG(%s) as mean,
                STDDEV_SAMP(%s) as stddev,
                MIN(%s) as min_val,
                MAX(%s) as max_val,
                PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY %s) as median,
                PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY %s) as p95,
                PERCENTILE_CONT(0.99) WITHIN GROUP (ORDER BY %s) as p99,
                COUNT(*) as sample_count,
                MIN(sampled_at) as period_start,
                MAX(sampled_at) as period_end
            FROM pgconsole.system_metrics_history
            WHERE instance_id = ?
              AND sampled_at > NOW() - INTERVAL '%d days'
            """.formatted(metric.name, metric.name, metric.name, metric.name,
                metric.name, metric.name, metric.name, days);

        // Add time filters for seasonal patterns
        if (hour != null) {
            sql += " AND EXTRACT(HOUR FROM sampled_at) = " + hour;
        }
        if (dayOfWeek != null) {
            sql += " AND EXTRACT(DOW FROM sampled_at) = " + dayOfWeek;
        }

        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, instanceName);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && rs.getInt("sample_count") >= 10) {
                    MetricBaseline baseline = new MetricBaseline(instanceName, metric.name,
                            MetricBaseline.Category.valueOf(metric.category.toUpperCase()));

                    baseline.setMean(rs.getDouble("mean"));
                    baseline.setStddev(rs.getDouble("stddev"));
                    baseline.setMin(rs.getDouble("min_val"));
                    baseline.setMax(rs.getDouble("max_val"));
                    baseline.setMedian(rs.getDouble("median"));
                    baseline.setP95(rs.getDouble("p95"));
                    baseline.setP99(rs.getDouble("p99"));
                    baseline.setSampleCount(rs.getInt("sample_count"));
                    baseline.setHourOfDay(hour);
                    baseline.setDayOfWeek(dayOfWeek);
                    baseline.setCalculatedAt(Instant.now());

                    Timestamp periodStart = rs.getTimestamp("period_start");
                    Timestamp periodEnd = rs.getTimestamp("period_end");
                    if (periodStart != null) {
                        baseline.setPeriodStart(periodStart.toInstant());
                    }
                    if (periodEnd != null) {
                        baseline.setPeriodEnd(periodEnd.toInstant());
                    }

                    return baseline;
                }
            }

        } catch (Exception e) {
            LOG.debugf(e, "Error calculating baseline for metric %s", metric.name);
        }

        return null;
    }

    private void saveBaseline(DataSource ds, MetricBaseline baseline) {
        String sql = """
            INSERT INTO pgconsole.metric_baseline
                (instance_id, metric_name, metric_category, baseline_mean, baseline_stddev,
                 baseline_min, baseline_max, baseline_median, baseline_p95, baseline_p99,
                 sample_count, hour_of_day, day_of_week, calculated_at, period_start, period_end)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), ?, ?)
            ON CONFLICT (instance_id, metric_name, metric_category, hour_of_day, day_of_week)
            DO UPDATE SET
                baseline_mean = EXCLUDED.baseline_mean,
                baseline_stddev = EXCLUDED.baseline_stddev,
                baseline_min = EXCLUDED.baseline_min,
                baseline_max = EXCLUDED.baseline_max,
                baseline_median = EXCLUDED.baseline_median,
                baseline_p95 = EXCLUDED.baseline_p95,
                baseline_p99 = EXCLUDED.baseline_p99,
                sample_count = EXCLUDED.sample_count,
                calculated_at = NOW(),
                period_start = EXCLUDED.period_start,
                period_end = EXCLUDED.period_end
            """;

        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, baseline.getInstanceId());
            stmt.setString(2, baseline.getMetricName());
            stmt.setString(3, baseline.getCategory().name().toLowerCase());
            stmt.setDouble(4, baseline.getMean());
            stmt.setDouble(5, baseline.getStddev());
            stmt.setObject(6, baseline.getMin(), Types.DOUBLE);
            stmt.setObject(7, baseline.getMax(), Types.DOUBLE);
            stmt.setObject(8, baseline.getMedian(), Types.DOUBLE);
            stmt.setObject(9, baseline.getP95(), Types.DOUBLE);
            stmt.setObject(10, baseline.getP99(), Types.DOUBLE);
            stmt.setInt(11, baseline.getSampleCount());
            stmt.setObject(12, baseline.getHourOfDay(), Types.INTEGER);
            stmt.setObject(13, baseline.getDayOfWeek(), Types.INTEGER);
            stmt.setTimestamp(14, baseline.getPeriodStart() != null ?
                    Timestamp.from(baseline.getPeriodStart()) : null);
            stmt.setTimestamp(15, baseline.getPeriodEnd() != null ?
                    Timestamp.from(baseline.getPeriodEnd()) : null);

            stmt.executeUpdate();

        } catch (Exception e) {
            LOG.debugf(e, "Error saving baseline for metric %s", baseline.getMetricName());
        }
    }

    private Map<String, Double> getCurrentMetricValues(DataSource ds, String instanceName) {
        Map<String, Double> values = new HashMap<>();

        String sql = """
            SELECT total_connections, active_queries, blocked_queries,
                   cache_hit_ratio, longest_query_seconds, total_database_size_bytes
            FROM pgconsole.system_metrics_history
            WHERE instance_id = ?
            ORDER BY sampled_at DESC
            LIMIT 1
            """;

        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, instanceName);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    values.put("total_connections", rs.getDouble("total_connections"));
                    values.put("active_queries", rs.getDouble("active_queries"));
                    values.put("blocked_queries", rs.getDouble("blocked_queries"));
                    values.put("cache_hit_ratio", rs.getDouble("cache_hit_ratio"));
                    values.put("longest_query_seconds", rs.getDouble("longest_query_seconds"));
                    values.put("total_database_size_bytes", rs.getDouble("total_database_size_bytes"));
                }
            }

        } catch (Exception e) {
            LOG.debugf(e, "Error getting current metric values");
        }

        return values;
    }

    private MetricBaseline findBestBaseline(DataSource ds, String instanceName,
                                             String metricName, int hour, int day) {
        // Try hourly baseline first
        MetricBaseline baseline = getBaseline(ds, instanceName, metricName, hour, null);
        if (baseline != null && baseline.getSampleCount() >= 10) {
            return baseline;
        }

        // Try daily baseline
        baseline = getBaseline(ds, instanceName, metricName, null, day);
        if (baseline != null && baseline.getSampleCount() >= 10) {
            return baseline;
        }

        // Fall back to overall baseline
        return getBaseline(ds, instanceName, metricName, null, null);
    }

    private MetricBaseline getBaseline(DataSource ds, String instanceName,
                                        String metricName, Integer hour, Integer day) {
        String sql = """
            SELECT baseline_mean, baseline_stddev, baseline_min, baseline_max,
                   baseline_median, baseline_p95, baseline_p99, sample_count
            FROM pgconsole.metric_baseline
            WHERE instance_id = ? AND metric_name = ?
              AND (hour_of_day IS NOT DISTINCT FROM ?)
              AND (day_of_week IS NOT DISTINCT FROM ?)
            """;

        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, instanceName);
            stmt.setString(2, metricName);
            stmt.setObject(3, hour, Types.INTEGER);
            stmt.setObject(4, day, Types.INTEGER);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    MetricBaseline baseline = new MetricBaseline(instanceName, metricName,
                            MetricBaseline.Category.SYSTEM);
                    baseline.setMean(rs.getDouble("baseline_mean"));
                    baseline.setStddev(rs.getDouble("baseline_stddev"));
                    baseline.setMin(rs.getDouble("baseline_min"));
                    baseline.setMax(rs.getDouble("baseline_max"));
                    baseline.setMedian(rs.getDouble("baseline_median"));
                    baseline.setP95(rs.getDouble("baseline_p95"));
                    baseline.setP99(rs.getDouble("baseline_p99"));
                    baseline.setSampleCount(rs.getInt("sample_count"));
                    baseline.setHourOfDay(hour);
                    baseline.setDayOfWeek(day);
                    return baseline;
                }
            }

        } catch (Exception e) {
            LOG.debugf(e, "Error getting baseline for metric %s", metricName);
        }

        return null;
    }

    private DetectedAnomaly createAnomaly(String instanceName, MetricDefinition metric,
                                           double value, MetricBaseline baseline, double sigma) {
        DetectedAnomaly anomaly = new DetectedAnomaly();
        anomaly.setInstanceId(instanceName);
        anomaly.setMetricName(metric.name);
        anomaly.setMetricCategory(metric.category);
        anomaly.setDetectedAt(Instant.now());
        anomaly.setAnomalyValue(value);
        anomaly.setBaselineMean(baseline.getMean());
        anomaly.setBaselineStddev(baseline.getStddev());
        anomaly.setDeviationSigma(sigma);
        anomaly.setSeverity(DetectedAnomaly.Severity.fromSigma(sigma));
        anomaly.setDirection(sigma > 0 ? DetectedAnomaly.Direction.ABOVE : DetectedAnomaly.Direction.BELOW);
        anomaly.setAnomalyType(DetectedAnomaly.AnomalyType.SPIKE);  // Default to spike

        return anomaly;
    }

    private List<DetectedAnomaly.CorrelatedMetric> findCorrelatedAnomalies(
            Map<String, Double> currentValues, DataSource ds, String instanceName,
            String excludeMetric, int hour, int day) {

        List<DetectedAnomaly.CorrelatedMetric> correlated = new ArrayList<>();

        for (MetricDefinition metric : MONITORED_METRICS) {
            if (metric.name.equals(excludeMetric)) {
                continue;
            }

            Double value = currentValues.get(metric.name);
            if (value == null) {
                continue;
            }

            MetricBaseline baseline = findBestBaseline(ds, instanceName, metric.name, hour, day);
            if (baseline == null || baseline.getStddev() == 0) {
                continue;
            }

            double sigma = baseline.calculateSigma(value);
            if (Math.abs(sigma) >= LOW_SIGMA) {
                double changePercent = ((value - baseline.getMean()) / baseline.getMean()) * 100;
                DetectedAnomaly.Direction dir = sigma > 0 ?
                        DetectedAnomaly.Direction.ABOVE : DetectedAnomaly.Direction.BELOW;
                correlated.add(new DetectedAnomaly.CorrelatedMetric(metric.name, changePercent, dir));
            }
        }

        return correlated;
    }

    private String generateRootCauseSuggestion(String metricName, double sigma,
                                                DetectedAnomaly.Direction direction) {
        boolean isHigh = direction == DetectedAnomaly.Direction.ABOVE;

        return switch (metricName) {
            case "total_connections" -> isHigh ?
                    "High connection count. Check for connection leaks, long-running transactions, or consider connection pooling." :
                    "Low connection count may indicate application issues or network problems.";
            case "active_queries" -> isHigh ?
                    "Unusually high number of active queries. Check for slow queries, blocking, or increased load." :
                    "Very few active queries may indicate application downtime or connection issues.";
            case "blocked_queries" -> isHigh ?
                    "Blocked queries detected. Check for lock contention, long-running transactions, or deadlocks." :
                    "Blocked queries have decreased, which is normal.";
            case "cache_hit_ratio" -> isHigh ?
                    "Cache hit ratio is unusually high (this is typically good)." :
                    "Low cache hit ratio. Consider increasing shared_buffers or investigating access patterns.";
            case "longest_query_seconds" -> isHigh ?
                    "Very long-running query detected. Consider query optimisation or timeout settings." :
                    "Query times are shorter than expected (this is typically good).";
            case "total_database_size_bytes" -> isHigh ?
                    "Rapid database growth. Check for large data imports, excessive logging, or bloat." :
                    "Database size decreased, possibly from vacuum or data deletion.";
            default -> "Anomaly detected in " + metricName + ". Investigate recent changes.";
        };
    }

    private void saveAnomaly(DataSource ds, DetectedAnomaly anomaly) {
        String sql = """
            INSERT INTO pgconsole.detected_anomaly
                (instance_id, metric_name, metric_category, detected_at, anomaly_value,
                 baseline_mean, baseline_stddev, deviation_sigma, severity, anomaly_type,
                 direction, root_cause_suggestion)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;

        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, anomaly.getInstanceId());
            stmt.setString(2, anomaly.getMetricName());
            stmt.setString(3, anomaly.getMetricCategory());
            stmt.setTimestamp(4, Timestamp.from(anomaly.getDetectedAt()));
            stmt.setDouble(5, anomaly.getAnomalyValue());
            stmt.setDouble(6, anomaly.getBaselineMean());
            stmt.setDouble(7, anomaly.getBaselineStddev());
            stmt.setDouble(8, anomaly.getDeviationSigma());
            stmt.setString(9, anomaly.getSeverity().name());
            stmt.setString(10, anomaly.getAnomalyType().name());
            stmt.setString(11, anomaly.getDirection().name());
            stmt.setString(12, anomaly.getRootCauseSuggestion());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    anomaly.setId(rs.getLong(1));
                }
            }

        } catch (Exception e) {
            LOG.debugf(e, "Error saving anomaly");
        }
    }

    private DetectedAnomaly mapAnomaly(ResultSet rs, String instanceName) throws SQLException {
        DetectedAnomaly anomaly = new DetectedAnomaly();
        anomaly.setId(rs.getLong("id"));
        anomaly.setInstanceId(instanceName);
        anomaly.setMetricName(rs.getString("metric_name"));
        anomaly.setMetricCategory(rs.getString("metric_category"));

        Timestamp detectedAt = rs.getTimestamp("detected_at");
        if (detectedAt != null) {
            anomaly.setDetectedAt(detectedAt.toInstant());
        }

        anomaly.setAnomalyValue(rs.getDouble("anomaly_value"));
        anomaly.setBaselineMean(rs.getDouble("baseline_mean"));
        anomaly.setBaselineStddev(rs.getDouble("baseline_stddev"));
        anomaly.setDeviationSigma(rs.getDouble("deviation_sigma"));

        String severityStr = rs.getString("severity");
        if (severityStr != null) {
            try {
                anomaly.setSeverity(DetectedAnomaly.Severity.valueOf(severityStr));
            } catch (IllegalArgumentException e) {
                anomaly.setSeverity(DetectedAnomaly.Severity.LOW);
            }
        }

        String typeStr = rs.getString("anomaly_type");
        if (typeStr != null) {
            try {
                anomaly.setAnomalyType(DetectedAnomaly.AnomalyType.valueOf(typeStr));
            } catch (IllegalArgumentException e) {
                anomaly.setAnomalyType(DetectedAnomaly.AnomalyType.SPIKE);
            }
        }

        String dirStr = rs.getString("direction");
        if (dirStr != null) {
            try {
                anomaly.setDirection(DetectedAnomaly.Direction.valueOf(dirStr));
            } catch (IllegalArgumentException e) {
                anomaly.setDirection(DetectedAnomaly.Direction.ABOVE);
            }
        }

        anomaly.setRootCauseSuggestion(rs.getString("root_cause_suggestion"));

        Timestamp ackAt = rs.getTimestamp("acknowledged_at");
        if (ackAt != null) {
            anomaly.setAcknowledgedAt(ackAt.toInstant());
        }
        anomaly.setAcknowledgedBy(rs.getString("acknowledged_by"));

        // Check for resolved columns
        try {
            Timestamp resolvedAt = rs.getTimestamp("resolved_at");
            if (resolvedAt != null) {
                anomaly.setResolvedAt(resolvedAt.toInstant());
            }
            anomaly.setResolutionNotes(rs.getString("resolution_notes"));
        } catch (SQLException e) {
            // Columns may not be in all queries
        }

        return anomaly;
    }

    private void fireAnomalyAlert(String instanceName, DetectedAnomaly anomaly) {
        String title = String.format("Anomaly Detected: %s", anomaly.getMetricName());
        String message = String.format(
                "Instance: %s\nMetric: %s\nValue: %.2f (baseline: %.2f ± %.2f)\nDeviation: %.1f sigma %s\nSeverity: %s\n\nSuggestion: %s",
                instanceName,
                anomaly.getMetricName(),
                anomaly.getAnomalyValue(),
                anomaly.getBaselineMean(),
                anomaly.getBaselineStddev(),
                Math.abs(anomaly.getDeviationSigma()),
                anomaly.getDirection().getDisplayName().toLowerCase(),
                anomaly.getSeverity().getDisplayName(),
                anomaly.getRootCauseSuggestion()
        );

        try {
            alertingService.sendAlert(anomaly.getInstanceId(), "ANOMALY_" + anomaly.getMetricName().toUpperCase(), title, message);
        } catch (Exception e) {
            LOG.debugf(e, "Error sending anomaly alert");
        }
    }

    /**
     * Definition of a metric to monitor.
     */
    private record MetricDefinition(String name, String category, String description) {
    }
}
