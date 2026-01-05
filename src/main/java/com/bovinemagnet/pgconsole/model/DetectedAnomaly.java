package com.bovinemagnet.pgconsole.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Represents a detected anomaly in a metric value from PostgreSQL monitoring data.
 * <p>
 * An anomaly is identified when a metric value deviates significantly from its statistical
 * baseline, calculated using historical data. The detection uses standard deviation (sigma)
 * to determine how far the current value diverges from the expected mean.
 * <p>
 * Each anomaly includes:
 * <ul>
 *   <li>Statistical context (baseline mean, standard deviation, sigma deviation)</li>
 *   <li>Severity classification based on deviation magnitude</li>
 *   <li>Type classification (spike, drop, trend, pattern break)</li>
 *   <li>Root cause suggestions based on correlated metrics</li>
 *   <li>State tracking (acknowledged, resolved)</li>
 * </ul>
 * <p>
 * Anomalies can be linked to alerts and tracked through their lifecycle from detection
 * to acknowledgement to resolution. Correlated metrics help identify cascading issues
 * or common root causes.
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see com.bovinemagnet.pgconsole.service.AnomalyDetectionService
 */
public class DetectedAnomaly {

    /**
     * Severity classification for anomalies based on sigma deviation from baseline.
     * <p>
     * Severity levels use standard statistical significance thresholds:
     * <ul>
     *   <li>CRITICAL: ≥4σ (99.994% confidence) - immediate attention required</li>
     *   <li>HIGH: ≥3σ (99.7% confidence) - urgent investigation recommended</li>
     *   <li>MEDIUM: ≥2.5σ (98.76% confidence) - monitor closely</li>
     *   <li>LOW: ≥2σ (95.45% confidence) - informational</li>
     * </ul>
     * Each severity includes a Bootstrap CSS class for visual presentation in dashboards.
     */
    public enum Severity {
        /** Critical severity requiring immediate attention (≥4 sigma deviation). */
        CRITICAL("Critical", "bg-danger", "> 4 sigma deviation"),

        /** High severity requiring urgent investigation (≥3 sigma deviation). */
        HIGH("High", "bg-warning text-dark", "> 3 sigma deviation"),

        /** Medium severity requiring close monitoring (≥2.5 sigma deviation). */
        MEDIUM("Medium", "bg-info", "> 2.5 sigma deviation"),

        /** Low severity for informational purposes (≥2 sigma deviation). */
        LOW("Low", "bg-secondary", "> 2 sigma deviation");

        private final String displayName;
        private final String cssClass;
        private final String description;

        /**
         * Constructs a Severity enum constant with display attributes.
         *
         * @param displayName human-readable name for UI display
         * @param cssClass Bootstrap CSS classes for styling
         * @param description threshold description
         */
        Severity(String displayName, String cssClass, String description) {
            this.displayName = displayName;
            this.cssClass = cssClass;
            this.description = description;
        }

        /**
         * Returns the human-readable display name.
         *
         * @return display name suitable for UI presentation
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Returns the Bootstrap CSS classes for styling this severity level.
         *
         * @return CSS class string (e.g., "bg-danger", "bg-warning text-dark")
         */
        public String getCssClass() {
            return cssClass;
        }

        /**
         * Returns the threshold description for this severity level.
         *
         * @return description of sigma deviation threshold
         */
        public String getDescription() {
            return description;
        }

        /**
         * Determines the appropriate severity level based on sigma deviation.
         * <p>
         * Uses absolute value of sigma to classify both positive and negative
         * deviations. Higher absolute deviations result in higher severity.
         *
         * @param sigma the standard deviation from baseline (positive or negative)
         * @return the severity level corresponding to the deviation magnitude
         */
        public static Severity fromSigma(double sigma) {
            double absSigma = Math.abs(sigma);
            if (absSigma >= 4.0) {
                return CRITICAL;
            } else if (absSigma >= 3.0) {
                return HIGH;
            } else if (absSigma >= 2.5) {
                return MEDIUM;
            } else {
                return LOW;
            }
        }
    }

    /**
     * Classification of anomaly patterns based on temporal behaviour.
     * <p>
     * Different anomaly types indicate different underlying causes:
     * <ul>
     *   <li>SPIKE - sudden increase (e.g., connection surge, query storm)</li>
     *   <li>DROP - sudden decrease (e.g., connection loss, service degradation)</li>
     *   <li>TREND - gradual change (e.g., memory leak, growing data volume)</li>
     *   <li>PATTERN_BREAK - deviation from expected pattern (e.g., missing scheduled job)</li>
     * </ul>
     * Each type includes a Bootstrap icon for visual identification in dashboards.
     */
    public enum AnomalyType {
        /** Sudden increase in metric value. */
        SPIKE("Spike", "bi-graph-up-arrow", "Sudden increase"),

        /** Sudden decrease in metric value. */
        DROP("Drop", "bi-graph-down-arrow", "Sudden decrease"),

        /** Gradual change over time in metric value. */
        TREND("Trend", "bi-arrow-up-right", "Gradual change over time"),

        /** Deviation from expected temporal pattern. */
        PATTERN_BREAK("Pattern Break", "bi-exclamation-triangle", "Deviation from expected pattern");

        private final String displayName;
        private final String icon;
        private final String description;

        /**
         * Constructs an AnomalyType enum constant with display attributes.
         *
         * @param displayName human-readable name for UI display
         * @param icon Bootstrap icon class name
         * @param description brief explanation of the anomaly type
         */
        AnomalyType(String displayName, String icon, String description) {
            this.displayName = displayName;
            this.icon = icon;
            this.description = description;
        }

        /**
         * Returns the human-readable display name.
         *
         * @return display name suitable for UI presentation
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Returns the Bootstrap icon class for visual representation.
         *
         * @return icon class name (e.g., "bi-graph-up-arrow")
         */
        public String getIcon() {
            return icon;
        }

        /**
         * Returns the brief description of this anomaly type.
         *
         * @return explanation of what this anomaly type represents
         */
        public String getDescription() {
            return description;
        }
    }

    /**
     * Direction of the anomaly relative to the statistical baseline.
     * <p>
     * Indicates whether the anomalous value is above or below the expected mean:
     * <ul>
     *   <li>ABOVE - value exceeds baseline (often performance degradation)</li>
     *   <li>BELOW - value falls below baseline (often resource depletion)</li>
     * </ul>
     * Each direction includes a Bootstrap CSS class for colour-coded presentation.
     */
    public enum Direction {
        /** Metric value is above the baseline mean. */
        ABOVE("Above Baseline", "text-danger"),

        /** Metric value is below the baseline mean. */
        BELOW("Below Baseline", "text-info");

        private final String displayName;
        private final String cssClass;

        /**
         * Constructs a Direction enum constant with display attributes.
         *
         * @param displayName human-readable name for UI display
         * @param cssClass Bootstrap CSS classes for colour styling
         */
        Direction(String displayName, String cssClass) {
            this.displayName = displayName;
            this.cssClass = cssClass;
        }

        /**
         * Returns the human-readable display name.
         *
         * @return display name suitable for UI presentation
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Returns the Bootstrap CSS classes for colour-coding this direction.
         *
         * @return CSS class string (e.g., "text-danger", "text-info")
         */
        public String getCssClass() {
            return cssClass;
        }
    }

    /** Unique identifier for this anomaly record. */
    private Long id;

    /** PostgreSQL instance identifier where the anomaly was detected. */
    private String instanceId;

    /** Name of the metric that exhibited anomalous behaviour. */
    private String metricName;

    /** Category or group of the metric (e.g., "connections", "queries", "cache"). */
    private String metricCategory;

    // Anomaly details

    /** Timestamp when the anomaly was first detected. */
    private Instant detectedAt;

    /** The actual metric value that triggered the anomaly detection. */
    private double anomalyValue;

    /** The statistical mean of the metric calculated from historical baseline data. */
    private double baselineMean;

    /** The standard deviation of the metric calculated from historical baseline data. */
    private double baselineStddev;

    /** Number of standard deviations the anomaly value is from the baseline mean (can be negative). */
    private double deviationSigma;

    // Classification

    /** Severity level of the anomaly based on deviation magnitude. */
    private Severity severity;

    /** Type of anomaly pattern detected (spike, drop, trend, or pattern break). */
    private AnomalyType anomalyType;

    /** Direction of the deviation relative to baseline (above or below). */
    private Direction direction;

    // Root cause analysis

    /** Suggested root cause or explanation for the anomaly based on analysis. */
    private String rootCauseSuggestion;

    /** List of other metrics that changed significantly at the same time, potentially related. */
    private List<CorrelatedMetric> correlatedMetrics;

    // State tracking

    /** Timestamp when the anomaly was acknowledged by a user or system. */
    private Instant acknowledgedAt;

    /** Username or system identifier of who acknowledged the anomaly. */
    private String acknowledgedBy;

    /** Timestamp when the anomaly was marked as resolved. */
    private Instant resolvedAt;

    /** Free-text notes about how the anomaly was resolved or what action was taken. */
    private String resolutionNotes;

    // Link to alert

    /** Foreign key to associated alert record, if this anomaly triggered an alert. */
    private Long alertId;

    /**
     * Represents a metric that exhibited correlated changes at the same time as the primary anomaly.
     * <p>
     * Correlated metrics help identify cascading failures or common root causes. For example,
     * if connection count spikes, you might see correlated changes in active queries, lock counts,
     * or memory usage. The change percentage and direction help quantify the correlation strength.
     */
    public static class CorrelatedMetric {
        /** Name of the correlated metric. */
        private String metricName;

        /** Percentage change from baseline at the time of correlation. */
        private double changePercent;

        /** Direction of the change (above or below baseline). */
        private Direction direction;

        /**
         * Constructs an empty CorrelatedMetric.
         * <p>
         * Required for bean instantiation and serialization frameworks.
         */
        public CorrelatedMetric() {
        }

        /**
         * Constructs a CorrelatedMetric with all properties.
         *
         * @param metricName the name of the correlated metric
         * @param changePercent the percentage change from baseline
         * @param direction the direction of the change (ABOVE or BELOW)
         */
        public CorrelatedMetric(String metricName, double changePercent, Direction direction) {
            this.metricName = metricName;
            this.changePercent = changePercent;
            this.direction = direction;
        }

        /**
         * Returns the name of the correlated metric.
         *
         * @return metric name
         */
        public String getMetricName() {
            return metricName;
        }

        /**
         * Sets the name of the correlated metric.
         *
         * @param metricName metric name
         */
        public void setMetricName(String metricName) {
            this.metricName = metricName;
        }

        /**
         * Returns the percentage change from baseline.
         * <p>
         * A positive value indicates increase, negative indicates decrease.
         *
         * @return change percentage (e.g., 25.5 means 25.5% increase)
         */
        public double getChangePercent() {
            return changePercent;
        }

        /**
         * Sets the percentage change from baseline.
         *
         * @param changePercent change percentage
         */
        public void setChangePercent(double changePercent) {
            this.changePercent = changePercent;
        }

        /**
         * Returns the direction of the change.
         *
         * @return ABOVE if above baseline, BELOW if below baseline
         */
        public Direction getDirection() {
            return direction;
        }

        /**
         * Sets the direction of the change.
         *
         * @param direction ABOVE or BELOW
         */
        public void setDirection(Direction direction) {
            this.direction = direction;
        }
    }

    /**
     * Constructs a new DetectedAnomaly with detection time set to current instant.
     * <p>
     * All other fields are initialised to their default values and should be
     * populated before persisting the anomaly.
     */
    public DetectedAnomaly() {
        this.detectedAt = Instant.now();
    }

    /**
     * Checks if this anomaly is still open (not resolved).
     * <p>
     * An anomaly remains open until it is explicitly marked as resolved by
     * setting the {@code resolvedAt} timestamp.
     *
     * @return {@code true} if the anomaly has not been resolved, {@code false} otherwise
     */
    public boolean isOpen() {
        return resolvedAt == null;
    }

    /**
     * Checks if this anomaly has been acknowledged.
     * <p>
     * Acknowledgement indicates that a user or system has been notified of the
     * anomaly and is aware of it, even if it hasn't been resolved yet.
     *
     * @return {@code true} if the anomaly has been acknowledged, {@code false} otherwise
     */
    public boolean isAcknowledged() {
        return acknowledgedAt != null;
    }

    /**
     * Returns a formatted string describing the deviation magnitude and direction.
     * <p>
     * Example output: "3.2σ above" or "2.1σ below"
     * <p>
     * The sigma symbol (σ) is used to represent standard deviations from the baseline.
     *
     * @return formatted deviation string suitable for UI display
     */
    public String getDeviationDisplay() {
        return String.format("%.1f\u03c3 %s", Math.abs(deviationSigma),
                direction == Direction.ABOVE ? "above" : "below");
    }

    /**
     * Returns a formatted string comparing the anomaly value to the baseline.
     * <p>
     * Example output: "125.50 (baseline: 100.00 ± 10.00)"
     * <p>
     * Shows the anomaly value followed by the baseline mean ± one standard deviation,
     * providing context for how far the value deviated from expected.
     *
     * @return formatted comparison string suitable for UI display
     */
    public String getValueComparisonDisplay() {
        return String.format("%.2f (baseline: %.2f ± %.2f)",
                anomalyValue, baselineMean, baselineStddev);
    }

    // Getters and setters

    /**
     * Returns the unique identifier for this anomaly.
     *
     * @return anomaly ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the unique identifier for this anomaly.
     *
     * @param id anomaly ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns the PostgreSQL instance identifier where the anomaly was detected.
     *
     * @return instance ID
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * Sets the PostgreSQL instance identifier.
     *
     * @param instanceId instance ID
     */
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    /**
     * Returns the name of the metric that exhibited anomalous behaviour.
     *
     * @return metric name
     */
    public String getMetricName() {
        return metricName;
    }

    /**
     * Sets the name of the metric.
     *
     * @param metricName metric name
     */
    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    /**
     * Returns the category or group of the metric.
     *
     * @return metric category (e.g., "connections", "queries", "cache")
     */
    public String getMetricCategory() {
        return metricCategory;
    }

    /**
     * Sets the category or group of the metric.
     *
     * @param metricCategory metric category
     */
    public void setMetricCategory(String metricCategory) {
        this.metricCategory = metricCategory;
    }

    /**
     * Returns the timestamp when the anomaly was first detected.
     *
     * @return detection timestamp
     */
    public Instant getDetectedAt() {
        return detectedAt;
    }

    /**
     * Sets the timestamp when the anomaly was detected.
     *
     * @param detectedAt detection timestamp
     */
    public void setDetectedAt(Instant detectedAt) {
        this.detectedAt = detectedAt;
    }

    /**
     * Returns the actual metric value that triggered the anomaly detection.
     *
     * @return anomaly value
     */
    public double getAnomalyValue() {
        return anomalyValue;
    }

    /**
     * Sets the metric value that triggered the anomaly.
     *
     * @param anomalyValue anomaly value
     */
    public void setAnomalyValue(double anomalyValue) {
        this.anomalyValue = anomalyValue;
    }

    /**
     * Returns the statistical mean calculated from historical baseline data.
     *
     * @return baseline mean
     */
    public double getBaselineMean() {
        return baselineMean;
    }

    /**
     * Sets the statistical baseline mean.
     *
     * @param baselineMean baseline mean
     */
    public void setBaselineMean(double baselineMean) {
        this.baselineMean = baselineMean;
    }

    /**
     * Returns the standard deviation calculated from historical baseline data.
     *
     * @return baseline standard deviation
     */
    public double getBaselineStddev() {
        return baselineStddev;
    }

    /**
     * Sets the baseline standard deviation.
     *
     * @param baselineStddev baseline standard deviation
     */
    public void setBaselineStddev(double baselineStddev) {
        this.baselineStddev = baselineStddev;
    }

    /**
     * Returns the number of standard deviations from the baseline mean.
     * <p>
     * Positive values indicate deviation above baseline, negative values indicate
     * deviation below baseline.
     *
     * @return deviation in sigma (standard deviations)
     */
    public double getDeviationSigma() {
        return deviationSigma;
    }

    /**
     * Sets the sigma deviation from baseline.
     *
     * @param deviationSigma deviation in standard deviations
     */
    public void setDeviationSigma(double deviationSigma) {
        this.deviationSigma = deviationSigma;
    }

    /**
     * Returns the severity level of the anomaly.
     *
     * @return severity (CRITICAL, HIGH, MEDIUM, or LOW)
     */
    public Severity getSeverity() {
        return severity;
    }

    /**
     * Sets the severity level of the anomaly.
     *
     * @param severity severity level
     */
    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    /**
     * Returns the type of anomaly pattern detected.
     *
     * @return anomaly type (SPIKE, DROP, TREND, or PATTERN_BREAK)
     */
    public AnomalyType getAnomalyType() {
        return anomalyType;
    }

    /**
     * Sets the type of anomaly pattern.
     *
     * @param anomalyType anomaly type
     */
    public void setAnomalyType(AnomalyType anomalyType) {
        this.anomalyType = anomalyType;
    }

    /**
     * Returns the direction of the deviation.
     *
     * @return direction (ABOVE or BELOW baseline)
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * Sets the direction of the deviation.
     *
     * @param direction ABOVE or BELOW baseline
     */
    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    /**
     * Returns the suggested root cause or explanation for the anomaly.
     *
     * @return root cause suggestion, or {@code null} if not analysed
     */
    public String getRootCauseSuggestion() {
        return rootCauseSuggestion;
    }

    /**
     * Sets the root cause suggestion.
     *
     * @param rootCauseSuggestion suggested explanation
     */
    public void setRootCauseSuggestion(String rootCauseSuggestion) {
        this.rootCauseSuggestion = rootCauseSuggestion;
    }

    /**
     * Returns the list of metrics that changed at the same time as this anomaly.
     *
     * @return list of correlated metrics, or {@code null} if not analysed
     */
    public List<CorrelatedMetric> getCorrelatedMetrics() {
        return correlatedMetrics;
    }

    /**
     * Sets the list of correlated metrics.
     *
     * @param correlatedMetrics list of correlated metrics
     */
    public void setCorrelatedMetrics(List<CorrelatedMetric> correlatedMetrics) {
        this.correlatedMetrics = correlatedMetrics;
    }

    /**
     * Returns the timestamp when the anomaly was acknowledged.
     *
     * @return acknowledgement timestamp, or {@code null} if not acknowledged
     */
    public Instant getAcknowledgedAt() {
        return acknowledgedAt;
    }

    /**
     * Sets the acknowledgement timestamp.
     *
     * @param acknowledgedAt acknowledgement timestamp
     */
    public void setAcknowledgedAt(Instant acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    /**
     * Returns the username or identifier of who acknowledged the anomaly.
     *
     * @return acknowledger identifier, or {@code null} if not acknowledged
     */
    public String getAcknowledgedBy() {
        return acknowledgedBy;
    }

    /**
     * Sets the identifier of who acknowledged the anomaly.
     *
     * @param acknowledgedBy acknowledger username or system identifier
     */
    public void setAcknowledgedBy(String acknowledgedBy) {
        this.acknowledgedBy = acknowledgedBy;
    }

    /**
     * Returns the timestamp when the anomaly was resolved.
     *
     * @return resolution timestamp, or {@code null} if still open
     */
    public Instant getResolvedAt() {
        return resolvedAt;
    }

    /**
     * Sets the resolution timestamp.
     *
     * @param resolvedAt resolution timestamp
     */
    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    /**
     * Returns the free-text notes about the anomaly resolution.
     *
     * @return resolution notes, or {@code null} if not resolved
     */
    public String getResolutionNotes() {
        return resolutionNotes;
    }

    /**
     * Sets the resolution notes.
     *
     * @param resolutionNotes notes describing the resolution
     */
    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    /**
     * Returns the ID of the associated alert, if this anomaly triggered one.
     *
     * @return alert ID, or {@code null} if no alert was triggered
     */
    public Long getAlertId() {
        return alertId;
    }

    /**
     * Sets the associated alert ID.
     *
     * @param alertId alert ID
     */
    public void setAlertId(Long alertId) {
        this.alertId = alertId;
    }
}
