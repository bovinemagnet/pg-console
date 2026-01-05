package com.bovinemagnet.pgconsole.model;

import java.time.Instant;
import java.util.List;

/**
 * Summary of all intelligent insights for dashboard display.
 * <p>
 * Aggregates anomalies, recommendations, forecasts, and health metrics
 * into a single view for the insights dashboard. This class provides a
 * comprehensive overview of database health by consolidating various
 * monitoring signals into actionable metrics and severity-based counts.
 * <p>
 * The summary includes:
 * <ul>
 *   <li>Anomaly counts by severity (critical, high, medium, low)</li>
 *   <li>Recommendation counts by severity</li>
 *   <li>Forecast alerts for storage and connection capacity</li>
 *   <li>Overall health score (0-100 scale)</li>
 *   <li>Top concerns requiring immediate attention</li>
 * </ul>
 * <p>
 * Health scores are calculated based on the severity and quantity of issues,
 * with critical issues having the highest impact on the score. A score of 90+
 * indicates excellent health, whilst a score below 30 indicates critical issues
 * requiring immediate attention.
 *
 * @author Paul Snow
 * @since 0.0.0
 * @see DetectedAnomaly
 * @see Concern
 */
public class InsightSummary {

    /**
     * A concern or issue requiring attention.
     * <p>
     * Represents a single actionable item derived from anomalies, recommendations,
     * or forecasts. Each concern includes a severity level, descriptive information,
     * and an optional URL for taking corrective action.
     * <p>
     * Concerns are used to populate the "top concerns" list on the insights dashboard,
     * providing operators with a quick view of the most important issues to address.
     *
     * @see DetectedAnomaly.Severity
     */
    public static class Concern {
        /** Brief title of the concern. */
        private String title;

        /** Detailed description of the issue and its potential impact. */
        private String description;

        /** Severity level indicating urgency of the concern. */
        private DetectedAnomaly.Severity severity;

        /** Source type: 'anomaly', 'recommendation', or 'forecast'. */
        private String sourceType;

        /** Optional URL for detailed information or corrective action. */
        private String actionUrl;

        /**
         * Default constructor for serialisation frameworks.
         */
        public Concern() {
        }

        /**
         * Constructs a fully populated concern.
         *
         * @param title brief title of the concern
         * @param description detailed description of the issue
         * @param severity severity level indicating urgency
         * @param sourceType source type ('anomaly', 'recommendation', or 'forecast')
         * @param actionUrl optional URL for detailed information or action
         */
        public Concern(String title, String description, DetectedAnomaly.Severity severity,
                       String sourceType, String actionUrl) {
            this.title = title;
            this.description = description;
            this.severity = severity;
            this.sourceType = sourceType;
            this.actionUrl = actionUrl;
        }

        /**
         * Returns the brief title of this concern.
         *
         * @return the concern title
         */
        public String getTitle() {
            return title;
        }

        /**
         * Sets the brief title of this concern.
         *
         * @param title the concern title
         */
        public void setTitle(String title) {
            this.title = title;
        }

        /**
         * Returns the detailed description of this concern.
         *
         * @return the concern description
         */
        public String getDescription() {
            return description;
        }

        /**
         * Sets the detailed description of this concern.
         *
         * @param description the concern description
         */
        public void setDescription(String description) {
            this.description = description;
        }

        /**
         * Returns the severity level of this concern.
         *
         * @return the severity level
         */
        public DetectedAnomaly.Severity getSeverity() {
            return severity;
        }

        /**
         * Sets the severity level of this concern.
         *
         * @param severity the severity level
         */
        public void setSeverity(DetectedAnomaly.Severity severity) {
            this.severity = severity;
        }

        /**
         * Returns the source type that generated this concern.
         *
         * @return the source type ('anomaly', 'recommendation', or 'forecast')
         */
        public String getSourceType() {
            return sourceType;
        }

        /**
         * Sets the source type that generated this concern.
         *
         * @param sourceType the source type ('anomaly', 'recommendation', or 'forecast')
         */
        public void setSourceType(String sourceType) {
            this.sourceType = sourceType;
        }

        /**
         * Returns the optional action URL for this concern.
         *
         * @return the action URL, or null if not applicable
         */
        public String getActionUrl() {
            return actionUrl;
        }

        /**
         * Sets the optional action URL for this concern.
         *
         * @param actionUrl the action URL, or null if not applicable
         */
        public void setActionUrl(String actionUrl) {
            this.actionUrl = actionUrl;
        }
    }

    /** Unique identifier for this summary record. */
    private Long id;

    /** PostgreSQL instance identifier this summary belongs to. */
    private String instanceId;

    /** Timestamp when this summary was calculated. */
    private Instant calculatedAt;

    /** Count of critical severity anomalies detected. */
    private int anomalyCountCritical;

    /** Count of high severity anomalies detected. */
    private int anomalyCountHigh;

    /** Count of medium severity anomalies detected. */
    private int anomalyCountMedium;

    /** Count of low severity anomalies detected. */
    private int anomalyCountLow;

    /** Count of critical severity recommendations. */
    private int recommendationCountCritical;

    /** Count of high severity recommendations. */
    private int recommendationCountHigh;

    /** Count of medium severity recommendations. */
    private int recommendationCountMedium;

    /** Count of low severity recommendations. */
    private int recommendationCountLow;

    /** Forecasted days until storage reaches warning threshold, or null if not at risk. */
    private Integer storageDaysUntilWarning;

    /** Forecasted days until storage reaches critical threshold, or null if not at risk. */
    private Integer storageDaysUntilCritical;

    /** Forecasted days until connections reach warning threshold, or null if not at risk. */
    private Integer connectionsDaysUntilWarning;

    /** Overall health score on a scale of 0-100, where 100 is perfect health. */
    private int overallHealthScore;

    /** List of top concerns requiring immediate attention, ordered by severity. */
    private List<Concern> topConcerns;

    /**
     * Default constructor initialising a new insight summary.
     * <p>
     * Sets the calculation timestamp to the current time and the health score
     * to 100 (perfect health). All issue counts default to zero.
     */
    public InsightSummary() {
        this.calculatedAt = Instant.now();
        this.overallHealthScore = 100;
    }

    /**
     * Constructs a new insight summary for a specific PostgreSQL instance.
     * <p>
     * Initialises with current timestamp, perfect health score (100), and
     * associates the summary with the specified instance identifier.
     *
     * @param instanceId the PostgreSQL instance identifier
     */
    public InsightSummary(String instanceId) {
        this();
        this.instanceId = instanceId;
    }

    /**
     * Returns the total count of open anomalies across all severity levels.
     * <p>
     * This aggregates critical, high, medium, and low severity anomalies
     * into a single count for dashboard display.
     *
     * @return the sum of all anomaly counts
     */
    public int getTotalAnomalyCount() {
        return anomalyCountCritical + anomalyCountHigh + anomalyCountMedium + anomalyCountLow;
    }

    /**
     * Returns the total count of open recommendations across all severity levels.
     * <p>
     * This aggregates critical, high, medium, and low severity recommendations
     * into a single count for dashboard display.
     *
     * @return the sum of all recommendation counts
     */
    public int getTotalRecommendationCount() {
        return recommendationCountCritical + recommendationCountHigh
                + recommendationCountMedium + recommendationCountLow;
    }

    /**
     * Returns the Bootstrap CSS class appropriate for the current health score.
     * <p>
     * Maps health score ranges to Bootstrap background colour classes:
     * <ul>
     *   <li>90-100: bg-success (green)</li>
     *   <li>70-89: bg-info (blue)</li>
     *   <li>50-69: bg-warning text-dark (yellow with dark text)</li>
     *   <li>0-49: bg-danger (red)</li>
     * </ul>
     *
     * @return Bootstrap CSS class string for badge or background styling
     */
    public String getHealthScoreCssClass() {
        if (overallHealthScore >= 90) {
            return "bg-success";
        } else if (overallHealthScore >= 70) {
            return "bg-info";
        } else if (overallHealthScore >= 50) {
            return "bg-warning text-dark";
        } else {
            return "bg-danger";
        }
    }

    /**
     * Returns a human-readable health status description.
     * <p>
     * Translates the numeric health score into a descriptive status:
     * <ul>
     *   <li>90-100: "Excellent"</li>
     *   <li>70-89: "Good"</li>
     *   <li>50-69: "Fair"</li>
     *   <li>30-49: "Poor"</li>
     *   <li>0-29: "Critical"</li>
     * </ul>
     *
     * @return descriptive health status string
     */
    public String getHealthStatus() {
        if (overallHealthScore >= 90) {
            return "Excellent";
        } else if (overallHealthScore >= 70) {
            return "Good";
        } else if (overallHealthScore >= 50) {
            return "Fair";
        } else if (overallHealthScore >= 30) {
            return "Poor";
        } else {
            return "Critical";
        }
    }

    /**
     * Checks whether any critical severity issues exist.
     * <p>
     * Critical issues are those that require immediate attention and
     * may indicate service degradation or potential outages.
     *
     * @return true if there are critical anomalies or recommendations, false otherwise
     */
    public boolean hasCriticalIssues() {
        return anomalyCountCritical > 0 || recommendationCountCritical > 0;
    }

    /**
     * Checks whether storage capacity is at risk.
     * <p>
     * Storage is considered at risk if the forecast indicates a warning
     * threshold will be reached within 30 days.
     *
     * @return true if storage warning is forecasted within 30 days, false otherwise
     */
    public boolean hasStorageRisk() {
        return storageDaysUntilWarning != null && storageDaysUntilWarning <= 30;
    }

    /**
     * Checks whether connection capacity is at risk.
     * <p>
     * Connections are considered at risk if the forecast indicates a warning
     * threshold will be reached within 7 days.
     *
     * @return true if connection warning is forecasted within 7 days, false otherwise
     */
    public boolean hasConnectionRisk() {
        return connectionsDaysUntilWarning != null && connectionsDaysUntilWarning <= 7;
    }

    /**
     * Calculates a health score based on issue counts and storage forecast.
     * <p>
     * The score starts at 100 (perfect health) and deductions are applied
     * based on the severity and quantity of issues:
     * <ul>
     *   <li>Critical issues: -20 points each</li>
     *   <li>High severity issues: -10 points each</li>
     *   <li>Medium severity issues: -5 points each</li>
     *   <li>Low severity issues: -2 points each</li>
     *   <li>Storage warning ≤7 days: -20 points</li>
     *   <li>Storage warning ≤14 days: -10 points</li>
     *   <li>Storage warning ≤30 days: -5 points</li>
     * </ul>
     * <p>
     * The final score is clamped to the range [0, 100].
     *
     * @param criticalIssues count of critical severity issues
     * @param highIssues count of high severity issues
     * @param mediumIssues count of medium severity issues
     * @param lowIssues count of low severity issues
     * @param daysUntilStorageWarning forecasted days until storage warning, or null if not at risk
     * @return health score in the range 0-100
     */
    public static int calculateHealthScore(int criticalIssues, int highIssues,
                                            int mediumIssues, int lowIssues,
                                            Integer daysUntilStorageWarning) {
        int score = 100;

        // Deduct for issues (more severe = higher deduction)
        score -= criticalIssues * 20;
        score -= highIssues * 10;
        score -= mediumIssues * 5;
        score -= lowIssues * 2;

        // Deduct for storage risk
        if (daysUntilStorageWarning != null) {
            if (daysUntilStorageWarning <= 7) {
                score -= 20;
            } else if (daysUntilStorageWarning <= 14) {
                score -= 10;
            } else if (daysUntilStorageWarning <= 30) {
                score -= 5;
            }
        }

        return Math.max(0, Math.min(100, score));
    }

    /**
     * Returns the unique identifier for this summary record.
     *
     * @return the summary ID, or null if not yet persisted
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the unique identifier for this summary record.
     *
     * @param id the summary ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns the PostgreSQL instance identifier this summary belongs to.
     *
     * @return the instance identifier
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * Sets the PostgreSQL instance identifier this summary belongs to.
     *
     * @param instanceId the instance identifier
     */
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    /**
     * Returns the timestamp when this summary was calculated.
     *
     * @return the calculation timestamp
     */
    public Instant getCalculatedAt() {
        return calculatedAt;
    }

    /**
     * Sets the timestamp when this summary was calculated.
     *
     * @param calculatedAt the calculation timestamp
     */
    public void setCalculatedAt(Instant calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    /**
     * Returns the count of critical severity anomalies.
     *
     * @return the critical anomaly count
     */
    public int getAnomalyCountCritical() {
        return anomalyCountCritical;
    }

    /**
     * Sets the count of critical severity anomalies.
     *
     * @param anomalyCountCritical the critical anomaly count
     */
    public void setAnomalyCountCritical(int anomalyCountCritical) {
        this.anomalyCountCritical = anomalyCountCritical;
    }

    /**
     * Returns the count of high severity anomalies.
     *
     * @return the high severity anomaly count
     */
    public int getAnomalyCountHigh() {
        return anomalyCountHigh;
    }

    /**
     * Sets the count of high severity anomalies.
     *
     * @param anomalyCountHigh the high severity anomaly count
     */
    public void setAnomalyCountHigh(int anomalyCountHigh) {
        this.anomalyCountHigh = anomalyCountHigh;
    }

    /**
     * Returns the count of medium severity anomalies.
     *
     * @return the medium severity anomaly count
     */
    public int getAnomalyCountMedium() {
        return anomalyCountMedium;
    }

    /**
     * Sets the count of medium severity anomalies.
     *
     * @param anomalyCountMedium the medium severity anomaly count
     */
    public void setAnomalyCountMedium(int anomalyCountMedium) {
        this.anomalyCountMedium = anomalyCountMedium;
    }

    /**
     * Returns the count of low severity anomalies.
     *
     * @return the low severity anomaly count
     */
    public int getAnomalyCountLow() {
        return anomalyCountLow;
    }

    /**
     * Sets the count of low severity anomalies.
     *
     * @param anomalyCountLow the low severity anomaly count
     */
    public void setAnomalyCountLow(int anomalyCountLow) {
        this.anomalyCountLow = anomalyCountLow;
    }

    /**
     * Returns the count of critical severity recommendations.
     *
     * @return the critical recommendation count
     */
    public int getRecommendationCountCritical() {
        return recommendationCountCritical;
    }

    /**
     * Sets the count of critical severity recommendations.
     *
     * @param recommendationCountCritical the critical recommendation count
     */
    public void setRecommendationCountCritical(int recommendationCountCritical) {
        this.recommendationCountCritical = recommendationCountCritical;
    }

    /**
     * Returns the count of high severity recommendations.
     *
     * @return the high severity recommendation count
     */
    public int getRecommendationCountHigh() {
        return recommendationCountHigh;
    }

    /**
     * Sets the count of high severity recommendations.
     *
     * @param recommendationCountHigh the high severity recommendation count
     */
    public void setRecommendationCountHigh(int recommendationCountHigh) {
        this.recommendationCountHigh = recommendationCountHigh;
    }

    /**
     * Returns the count of medium severity recommendations.
     *
     * @return the medium severity recommendation count
     */
    public int getRecommendationCountMedium() {
        return recommendationCountMedium;
    }

    /**
     * Sets the count of medium severity recommendations.
     *
     * @param recommendationCountMedium the medium severity recommendation count
     */
    public void setRecommendationCountMedium(int recommendationCountMedium) {
        this.recommendationCountMedium = recommendationCountMedium;
    }

    /**
     * Returns the count of low severity recommendations.
     *
     * @return the low severity recommendation count
     */
    public int getRecommendationCountLow() {
        return recommendationCountLow;
    }

    /**
     * Sets the count of low severity recommendations.
     *
     * @param recommendationCountLow the low severity recommendation count
     */
    public void setRecommendationCountLow(int recommendationCountLow) {
        this.recommendationCountLow = recommendationCountLow;
    }

    /**
     * Returns the forecasted days until storage reaches warning threshold.
     *
     * @return days until warning threshold, or null if not at risk
     */
    public Integer getStorageDaysUntilWarning() {
        return storageDaysUntilWarning;
    }

    /**
     * Sets the forecasted days until storage reaches warning threshold.
     *
     * @param storageDaysUntilWarning days until warning threshold, or null if not at risk
     */
    public void setStorageDaysUntilWarning(Integer storageDaysUntilWarning) {
        this.storageDaysUntilWarning = storageDaysUntilWarning;
    }

    /**
     * Returns the forecasted days until storage reaches critical threshold.
     *
     * @return days until critical threshold, or null if not at risk
     */
    public Integer getStorageDaysUntilCritical() {
        return storageDaysUntilCritical;
    }

    /**
     * Sets the forecasted days until storage reaches critical threshold.
     *
     * @param storageDaysUntilCritical days until critical threshold, or null if not at risk
     */
    public void setStorageDaysUntilCritical(Integer storageDaysUntilCritical) {
        this.storageDaysUntilCritical = storageDaysUntilCritical;
    }

    /**
     * Returns the forecasted days until connections reach warning threshold.
     *
     * @return days until warning threshold, or null if not at risk
     */
    public Integer getConnectionsDaysUntilWarning() {
        return connectionsDaysUntilWarning;
    }

    /**
     * Sets the forecasted days until connections reach warning threshold.
     *
     * @param connectionsDaysUntilWarning days until warning threshold, or null if not at risk
     */
    public void setConnectionsDaysUntilWarning(Integer connectionsDaysUntilWarning) {
        this.connectionsDaysUntilWarning = connectionsDaysUntilWarning;
    }

    /**
     * Returns the overall health score on a scale of 0-100.
     *
     * @return the health score, where 100 is perfect health
     */
    public int getOverallHealthScore() {
        return overallHealthScore;
    }

    /**
     * Sets the overall health score on a scale of 0-100.
     *
     * @param overallHealthScore the health score, where 100 is perfect health
     */
    public void setOverallHealthScore(int overallHealthScore) {
        this.overallHealthScore = overallHealthScore;
    }

    /**
     * Returns the list of top concerns requiring immediate attention.
     * <p>
     * Concerns are typically ordered by severity, with critical issues first.
     *
     * @return the list of top concerns, or null if not populated
     */
    public List<Concern> getTopConcerns() {
        return topConcerns;
    }

    /**
     * Sets the list of top concerns requiring immediate attention.
     *
     * @param topConcerns the list of top concerns
     */
    public void setTopConcerns(List<Concern> topConcerns) {
        this.topConcerns = topConcerns;
    }
}
