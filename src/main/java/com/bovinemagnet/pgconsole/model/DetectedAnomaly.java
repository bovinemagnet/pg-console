package com.bovinemagnet.pgconsole.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Represents a detected anomaly in a metric value.
 * <p>
 * An anomaly is detected when a metric value deviates significantly
 * from its statistical baseline. Includes severity classification,
 * root cause suggestions, and correlation with other metrics.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class DetectedAnomaly {

    /**
     * Severity level of the anomaly.
     */
    public enum Severity {
        CRITICAL("Critical", "bg-danger", "> 4 sigma deviation"),
        HIGH("High", "bg-warning text-dark", "> 3 sigma deviation"),
        MEDIUM("Medium", "bg-info", "> 2.5 sigma deviation"),
        LOW("Low", "bg-secondary", "> 2 sigma deviation");

        private final String displayName;
        private final String cssClass;
        private final String description;

        Severity(String displayName, String cssClass, String description) {
            this.displayName = displayName;
            this.cssClass = cssClass;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getCssClass() {
            return cssClass;
        }

        public String getDescription() {
            return description;
        }

        /**
         * Determine severity based on sigma deviation.
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
     * Type of anomaly detected.
     */
    public enum AnomalyType {
        SPIKE("Spike", "bi-graph-up-arrow", "Sudden increase"),
        DROP("Drop", "bi-graph-down-arrow", "Sudden decrease"),
        TREND("Trend", "bi-arrow-up-right", "Gradual change over time"),
        PATTERN_BREAK("Pattern Break", "bi-exclamation-triangle", "Deviation from expected pattern");

        private final String displayName;
        private final String icon;
        private final String description;

        AnomalyType(String displayName, String icon, String description) {
            this.displayName = displayName;
            this.icon = icon;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getIcon() {
            return icon;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Direction of the anomaly.
     */
    public enum Direction {
        ABOVE("Above Baseline", "text-danger"),
        BELOW("Below Baseline", "text-info");

        private final String displayName;
        private final String cssClass;

        Direction(String displayName, String cssClass) {
            this.displayName = displayName;
            this.cssClass = cssClass;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getCssClass() {
            return cssClass;
        }
    }

    private Long id;
    private String instanceId;
    private String metricName;
    private String metricCategory;

    // Anomaly details
    private Instant detectedAt;
    private double anomalyValue;
    private double baselineMean;
    private double baselineStddev;
    private double deviationSigma;

    // Classification
    private Severity severity;
    private AnomalyType anomalyType;
    private Direction direction;

    // Root cause analysis
    private String rootCauseSuggestion;
    private List<CorrelatedMetric> correlatedMetrics;

    // State tracking
    private Instant acknowledgedAt;
    private String acknowledgedBy;
    private Instant resolvedAt;
    private String resolutionNotes;

    // Link to alert
    private Long alertId;

    /**
     * A metric that changed at the same time as this anomaly.
     */
    public static class CorrelatedMetric {
        private String metricName;
        private double changePercent;
        private Direction direction;

        public CorrelatedMetric() {
        }

        public CorrelatedMetric(String metricName, double changePercent, Direction direction) {
            this.metricName = metricName;
            this.changePercent = changePercent;
            this.direction = direction;
        }

        public String getMetricName() {
            return metricName;
        }

        public void setMetricName(String metricName) {
            this.metricName = metricName;
        }

        public double getChangePercent() {
            return changePercent;
        }

        public void setChangePercent(double changePercent) {
            this.changePercent = changePercent;
        }

        public Direction getDirection() {
            return direction;
        }

        public void setDirection(Direction direction) {
            this.direction = direction;
        }
    }

    public DetectedAnomaly() {
        this.detectedAt = Instant.now();
    }

    /**
     * Check if this anomaly is still open (not resolved).
     */
    public boolean isOpen() {
        return resolvedAt == null;
    }

    /**
     * Check if this anomaly has been acknowledged.
     */
    public boolean isAcknowledged() {
        return acknowledgedAt != null;
    }

    /**
     * Get a formatted deviation display string.
     */
    public String getDeviationDisplay() {
        return String.format("%.1f\u03c3 %s", Math.abs(deviationSigma),
                direction == Direction.ABOVE ? "above" : "below");
    }

    /**
     * Get a formatted value comparison display.
     */
    public String getValueComparisonDisplay() {
        return String.format("%.2f (baseline: %.2f ± %.2f)",
                anomalyValue, baselineMean, baselineStddev);
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public String getMetricCategory() {
        return metricCategory;
    }

    public void setMetricCategory(String metricCategory) {
        this.metricCategory = metricCategory;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(Instant detectedAt) {
        this.detectedAt = detectedAt;
    }

    public double getAnomalyValue() {
        return anomalyValue;
    }

    public void setAnomalyValue(double anomalyValue) {
        this.anomalyValue = anomalyValue;
    }

    public double getBaselineMean() {
        return baselineMean;
    }

    public void setBaselineMean(double baselineMean) {
        this.baselineMean = baselineMean;
    }

    public double getBaselineStddev() {
        return baselineStddev;
    }

    public void setBaselineStddev(double baselineStddev) {
        this.baselineStddev = baselineStddev;
    }

    public double getDeviationSigma() {
        return deviationSigma;
    }

    public void setDeviationSigma(double deviationSigma) {
        this.deviationSigma = deviationSigma;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public AnomalyType getAnomalyType() {
        return anomalyType;
    }

    public void setAnomalyType(AnomalyType anomalyType) {
        this.anomalyType = anomalyType;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public String getRootCauseSuggestion() {
        return rootCauseSuggestion;
    }

    public void setRootCauseSuggestion(String rootCauseSuggestion) {
        this.rootCauseSuggestion = rootCauseSuggestion;
    }

    public List<CorrelatedMetric> getCorrelatedMetrics() {
        return correlatedMetrics;
    }

    public void setCorrelatedMetrics(List<CorrelatedMetric> correlatedMetrics) {
        this.correlatedMetrics = correlatedMetrics;
    }

    public Instant getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(Instant acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public String getAcknowledgedBy() {
        return acknowledgedBy;
    }

    public void setAcknowledgedBy(String acknowledgedBy) {
        this.acknowledgedBy = acknowledgedBy;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public Long getAlertId() {
        return alertId;
    }

    public void setAlertId(Long alertId) {
        this.alertId = alertId;
    }
}
