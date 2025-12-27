package com.bovinemagnet.pgconsole.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Unified recommendation from all recommendation sources.
 * <p>
 * Aggregates recommendations from Index Advisor, Table Maintenance,
 * Query Regression, Config Tuning, and Anomaly Detection into a
 * single prioritised list.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class UnifiedRecommendation {

    /**
     * Source of the recommendation.
     */
    public enum Source {
        INDEX_ADVISOR("Index Advisor", "bi-list-ul", "Index-related recommendations"),
        TABLE_MAINTENANCE("Table Maintenance", "bi-tools", "Vacuum and analyse recommendations"),
        QUERY_REGRESSION("Query Regression", "bi-graph-down", "Performance degradation alerts"),
        CONFIG_TUNING("Configuration", "bi-sliders", "PostgreSQL configuration suggestions"),
        ANOMALY("Anomaly", "bi-exclamation-triangle", "Detected metric anomalies");

        private final String displayName;
        private final String icon;
        private final String description;

        Source(String displayName, String icon, String description) {
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
     * Severity level.
     */
    public enum Severity {
        CRITICAL("Critical", "bg-danger", 4),
        HIGH("High", "bg-warning text-dark", 3),
        MEDIUM("Medium", "bg-info", 2),
        LOW("Low", "bg-secondary", 1);

        private final String displayName;
        private final String cssClass;
        private final int weight;

        Severity(String displayName, String cssClass, int weight) {
            this.displayName = displayName;
            this.cssClass = cssClass;
            this.weight = weight;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getCssClass() {
            return cssClass;
        }

        public int getWeight() {
            return weight;
        }
    }

    /**
     * Estimated impact of applying the recommendation.
     */
    public enum Impact {
        HIGH("High", "Significant improvement expected"),
        MEDIUM("Medium", "Moderate improvement expected"),
        LOW("Low", "Minor improvement expected");

        private final String displayName;
        private final String description;

        Impact(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Estimated effort to apply the recommendation.
     */
    public enum Effort {
        MINIMAL("Minimal", "< 5 minutes, no risk"),
        LOW("Low", "< 30 minutes, low risk"),
        MEDIUM("Medium", "< 2 hours, moderate planning needed"),
        HIGH("High", "Significant planning and testing needed");

        private final String displayName;
        private final String description;

        Effort(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Status of the recommendation.
     */
    public enum Status {
        OPEN("Open", "bg-primary", "Pending action"),
        IN_PROGRESS("In Progress", "bg-info", "Currently being worked on"),
        APPLIED("Applied", "bg-success", "Successfully applied"),
        DISMISSED("Dismissed", "bg-secondary", "Dismissed by user"),
        DEFERRED("Deferred", "bg-warning text-dark", "Scheduled for later");

        private final String displayName;
        private final String cssClass;
        private final String description;

        Status(String displayName, String cssClass, String description) {
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
    }

    /**
     * Effectiveness rating after application.
     */
    public enum Effectiveness {
        EXCELLENT("Excellent", "bg-success", "Exceeded expectations"),
        GOOD("Good", "bg-info", "Met expectations"),
        NEUTRAL("Neutral", "bg-secondary", "No measurable change"),
        POOR("Poor", "bg-danger", "Did not improve or worsened");

        private final String displayName;
        private final String cssClass;
        private final String description;

        Effectiveness(String displayName, String cssClass, String description) {
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
    }

    private Long id;
    private String instanceId;

    // Recommendation details
    private Source source;
    private String recommendationType;
    private String title;
    private String description;
    private String rationale;

    // Priority and impact
    private Severity severity;
    private int priorityScore;  // 1-100
    private Impact estimatedImpact;
    private Effort estimatedEffort;

    // Suggested action
    private String suggestedSql;
    private Map<String, String> suggestedConfig;
    private String rollbackSql;

    // Affected objects
    private List<String> affectedTables;
    private List<String> affectedIndexes;
    private List<String> affectedQueries;

    // State tracking
    private Status status;
    private Instant createdAt;
    private Instant appliedAt;
    private String appliedBy;
    private Instant dismissedAt;
    private String dismissedBy;
    private String dismissReason;
    private LocalDate deferredUntil;

    // Effectiveness tracking
    private Double preMetricValue;
    private Double postMetricValue;
    private Effectiveness effectivenessRating;

    public UnifiedRecommendation() {
        this.status = Status.OPEN;
        this.createdAt = Instant.now();
    }

    /**
     * Calculate a composite priority score based on severity, impact, and effort.
     */
    public static int calculatePriorityScore(Severity severity, Impact impact, Effort effort) {
        int score = 0;

        // Severity contributes most (up to 50 points)
        score += severity.getWeight() * 12;

        // Impact contributes significantly (up to 30 points)
        switch (impact) {
            case HIGH -> score += 30;
            case MEDIUM -> score += 20;
            case LOW -> score += 10;
        }

        // Lower effort = higher priority (up to 20 points)
        switch (effort) {
            case MINIMAL -> score += 20;
            case LOW -> score += 15;
            case MEDIUM -> score += 10;
            case HIGH -> score += 5;
        }

        return Math.min(100, score);
    }

    /**
     * Check if this recommendation can be applied with one click.
     */
    public boolean isOneClickApplicable() {
        return suggestedSql != null && !suggestedSql.isBlank()
                && estimatedEffort == Effort.MINIMAL;
    }

    /**
     * Get the improvement percentage after application.
     */
    public Double getImprovementPercent() {
        if (preMetricValue == null || postMetricValue == null || preMetricValue == 0) {
            return null;
        }
        return ((preMetricValue - postMetricValue) / preMetricValue) * 100;
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

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public String getRecommendationType() {
        return recommendationType;
    }

    public void setRecommendationType(String recommendationType) {
        this.recommendationType = recommendationType;
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

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public int getPriorityScore() {
        return priorityScore;
    }

    public void setPriorityScore(int priorityScore) {
        this.priorityScore = priorityScore;
    }

    public Impact getEstimatedImpact() {
        return estimatedImpact;
    }

    public void setEstimatedImpact(Impact estimatedImpact) {
        this.estimatedImpact = estimatedImpact;
    }

    public Effort getEstimatedEffort() {
        return estimatedEffort;
    }

    public void setEstimatedEffort(Effort estimatedEffort) {
        this.estimatedEffort = estimatedEffort;
    }

    public String getSuggestedSql() {
        return suggestedSql;
    }

    public void setSuggestedSql(String suggestedSql) {
        this.suggestedSql = suggestedSql;
    }

    public Map<String, String> getSuggestedConfig() {
        return suggestedConfig;
    }

    public void setSuggestedConfig(Map<String, String> suggestedConfig) {
        this.suggestedConfig = suggestedConfig;
    }

    public String getRollbackSql() {
        return rollbackSql;
    }

    public void setRollbackSql(String rollbackSql) {
        this.rollbackSql = rollbackSql;
    }

    public List<String> getAffectedTables() {
        return affectedTables;
    }

    public void setAffectedTables(List<String> affectedTables) {
        this.affectedTables = affectedTables;
    }

    public List<String> getAffectedIndexes() {
        return affectedIndexes;
    }

    public void setAffectedIndexes(List<String> affectedIndexes) {
        this.affectedIndexes = affectedIndexes;
    }

    public List<String> getAffectedQueries() {
        return affectedQueries;
    }

    public void setAffectedQueries(List<String> affectedQueries) {
        this.affectedQueries = affectedQueries;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(Instant appliedAt) {
        this.appliedAt = appliedAt;
    }

    public String getAppliedBy() {
        return appliedBy;
    }

    public void setAppliedBy(String appliedBy) {
        this.appliedBy = appliedBy;
    }

    public Instant getDismissedAt() {
        return dismissedAt;
    }

    public void setDismissedAt(Instant dismissedAt) {
        this.dismissedAt = dismissedAt;
    }

    public String getDismissedBy() {
        return dismissedBy;
    }

    public void setDismissedBy(String dismissedBy) {
        this.dismissedBy = dismissedBy;
    }

    public String getDismissReason() {
        return dismissReason;
    }

    public void setDismissReason(String dismissReason) {
        this.dismissReason = dismissReason;
    }

    public LocalDate getDeferredUntil() {
        return deferredUntil;
    }

    public void setDeferredUntil(LocalDate deferredUntil) {
        this.deferredUntil = deferredUntil;
    }

    public Double getPreMetricValue() {
        return preMetricValue;
    }

    public void setPreMetricValue(Double preMetricValue) {
        this.preMetricValue = preMetricValue;
    }

    public Double getPostMetricValue() {
        return postMetricValue;
    }

    public void setPostMetricValue(Double postMetricValue) {
        this.postMetricValue = postMetricValue;
    }

    public Effectiveness getEffectivenessRating() {
        return effectivenessRating;
    }

    public void setEffectivenessRating(Effectiveness effectivenessRating) {
        this.effectivenessRating = effectivenessRating;
    }
}
