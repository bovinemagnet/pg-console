package com.bovinemagnet.pgconsole.model;

import java.time.Instant;
import java.util.List;

/**
 * Summary of all intelligent insights for dashboard display.
 * <p>
 * Aggregates anomalies, recommendations, forecasts, and health metrics
 * into a single view for the insights dashboard.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class InsightSummary {

    /**
     * A concern or issue requiring attention.
     */
    public static class Concern {
        private String title;
        private String description;
        private DetectedAnomaly.Severity severity;
        private String sourceType;  // 'anomaly', 'recommendation', 'forecast'
        private String actionUrl;

        public Concern() {
        }

        public Concern(String title, String description, DetectedAnomaly.Severity severity,
                       String sourceType, String actionUrl) {
            this.title = title;
            this.description = description;
            this.severity = severity;
            this.sourceType = sourceType;
            this.actionUrl = actionUrl;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public DetectedAnomaly.Severity getSeverity() {
            return severity;
        }

        public void setSeverity(DetectedAnomaly.Severity severity) {
            this.severity = severity;
        }

        public String getSourceType() {
            return sourceType;
        }

        public void setSourceType(String sourceType) {
            this.sourceType = sourceType;
        }

        public String getActionUrl() {
            return actionUrl;
        }

        public void setActionUrl(String actionUrl) {
            this.actionUrl = actionUrl;
        }
    }

    private Long id;
    private String instanceId;
    private Instant calculatedAt;

    // Anomaly summary
    private int anomalyCountCritical;
    private int anomalyCountHigh;
    private int anomalyCountMedium;
    private int anomalyCountLow;

    // Recommendation summary
    private int recommendationCountCritical;
    private int recommendationCountHigh;
    private int recommendationCountMedium;
    private int recommendationCountLow;

    // Forecast alerts
    private Integer storageDaysUntilWarning;
    private Integer storageDaysUntilCritical;
    private Integer connectionsDaysUntilWarning;

    // Health score (0-100)
    private int overallHealthScore;

    // Top concerns for quick display
    private List<Concern> topConcerns;

    public InsightSummary() {
        this.calculatedAt = Instant.now();
        this.overallHealthScore = 100;
    }

    public InsightSummary(String instanceId) {
        this();
        this.instanceId = instanceId;
    }

    /**
     * Get the total count of open anomalies.
     */
    public int getTotalAnomalyCount() {
        return anomalyCountCritical + anomalyCountHigh + anomalyCountMedium + anomalyCountLow;
    }

    /**
     * Get the total count of open recommendations.
     */
    public int getTotalRecommendationCount() {
        return recommendationCountCritical + recommendationCountHigh
                + recommendationCountMedium + recommendationCountLow;
    }

    /**
     * Get the health score CSS class.
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
     * Get a health status description.
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
     * Check if there are any critical issues.
     */
    public boolean hasCriticalIssues() {
        return anomalyCountCritical > 0 || recommendationCountCritical > 0;
    }

    /**
     * Check if storage is at risk.
     */
    public boolean hasStorageRisk() {
        return storageDaysUntilWarning != null && storageDaysUntilWarning <= 30;
    }

    /**
     * Check if connections are at risk.
     */
    public boolean hasConnectionRisk() {
        return connectionsDaysUntilWarning != null && connectionsDaysUntilWarning <= 7;
    }

    /**
     * Calculate a simple health score based on current metrics.
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

    public Instant getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(Instant calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    public int getAnomalyCountCritical() {
        return anomalyCountCritical;
    }

    public void setAnomalyCountCritical(int anomalyCountCritical) {
        this.anomalyCountCritical = anomalyCountCritical;
    }

    public int getAnomalyCountHigh() {
        return anomalyCountHigh;
    }

    public void setAnomalyCountHigh(int anomalyCountHigh) {
        this.anomalyCountHigh = anomalyCountHigh;
    }

    public int getAnomalyCountMedium() {
        return anomalyCountMedium;
    }

    public void setAnomalyCountMedium(int anomalyCountMedium) {
        this.anomalyCountMedium = anomalyCountMedium;
    }

    public int getAnomalyCountLow() {
        return anomalyCountLow;
    }

    public void setAnomalyCountLow(int anomalyCountLow) {
        this.anomalyCountLow = anomalyCountLow;
    }

    public int getRecommendationCountCritical() {
        return recommendationCountCritical;
    }

    public void setRecommendationCountCritical(int recommendationCountCritical) {
        this.recommendationCountCritical = recommendationCountCritical;
    }

    public int getRecommendationCountHigh() {
        return recommendationCountHigh;
    }

    public void setRecommendationCountHigh(int recommendationCountHigh) {
        this.recommendationCountHigh = recommendationCountHigh;
    }

    public int getRecommendationCountMedium() {
        return recommendationCountMedium;
    }

    public void setRecommendationCountMedium(int recommendationCountMedium) {
        this.recommendationCountMedium = recommendationCountMedium;
    }

    public int getRecommendationCountLow() {
        return recommendationCountLow;
    }

    public void setRecommendationCountLow(int recommendationCountLow) {
        this.recommendationCountLow = recommendationCountLow;
    }

    public Integer getStorageDaysUntilWarning() {
        return storageDaysUntilWarning;
    }

    public void setStorageDaysUntilWarning(Integer storageDaysUntilWarning) {
        this.storageDaysUntilWarning = storageDaysUntilWarning;
    }

    public Integer getStorageDaysUntilCritical() {
        return storageDaysUntilCritical;
    }

    public void setStorageDaysUntilCritical(Integer storageDaysUntilCritical) {
        this.storageDaysUntilCritical = storageDaysUntilCritical;
    }

    public Integer getConnectionsDaysUntilWarning() {
        return connectionsDaysUntilWarning;
    }

    public void setConnectionsDaysUntilWarning(Integer connectionsDaysUntilWarning) {
        this.connectionsDaysUntilWarning = connectionsDaysUntilWarning;
    }

    public int getOverallHealthScore() {
        return overallHealthScore;
    }

    public void setOverallHealthScore(int overallHealthScore) {
        this.overallHealthScore = overallHealthScore;
    }

    public List<Concern> getTopConcerns() {
        return topConcerns;
    }

    public void setTopConcerns(List<Concern> topConcerns) {
        this.topConcerns = topConcerns;
    }
}
