package com.bovinemagnet.pgconsole.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Unified recommendation from all recommendation sources within PG Console.
 * <p>
 * Aggregates recommendations from Index Adviser, Table Maintenance,
 * Query Regression, Configuration Tuning, and Anomaly Detection into a
 * single prioritised list for actionable database optimisation insights.
 * <p>
 * This model supports the full lifecycle of a recommendation from creation
 * through application or dismissal, including effectiveness tracking to
 * measure the actual impact of applied recommendations.
 * <p>
 * Each recommendation includes:
 * <ul>
 *   <li>Priority scoring based on severity, impact, and effort</li>
 *   <li>Suggested SQL or configuration changes</li>
 *   <li>Rollback capability for safety</li>
 *   <li>Status tracking (open, in progress, applied, dismissed, deferred)</li>
 *   <li>Pre/post metrics for effectiveness measurement</li>
 * </ul>
 * <p>
 * Usage example:
 * <pre>{@code
 * UnifiedRecommendation rec = new UnifiedRecommendation();
 * rec.setSource(Source.INDEX_ADVISOR);
 * rec.setSeverity(Severity.HIGH);
 * rec.setEstimatedImpact(Impact.HIGH);
 * rec.setEstimatedEffort(Effort.MINIMAL);
 * rec.setPriorityScore(
 *     UnifiedRecommendation.calculatePriorityScore(
 *         rec.getSeverity(),
 *         rec.getEstimatedImpact(),
 *         rec.getEstimatedEffort()
 *     )
 * );
 * rec.setSuggestedSql("CREATE INDEX idx_users_email ON users(email)");
 * }</pre>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see com.bovinemagnet.pgconsole.service.PostgresService
 */
public class UnifiedRecommendation {

    /**
     * Source system that generated the recommendation.
     * <p>
     * Each source represents a different analysis engine within PG Console,
     * with its own display name, Bootstrap icon, and description for UI
     * rendering purposes.
     */
    public enum Source {
        /** Recommendations for missing or unused indices based on query patterns. */
        INDEX_ADVISOR("Index Advisor", "bi-list-ul", "Index-related recommendations"),

        /** Recommendations for VACUUM, ANALYSE, and table bloat remediation. */
        TABLE_MAINTENANCE("Table Maintenance", "bi-tools", "Vacuum and analyse recommendations"),

        /** Alerts for queries that have degraded in performance over time. */
        QUERY_REGRESSION("Query Regression", "bi-graph-down", "Performance degradation alerts"),

        /** Suggestions for PostgreSQL configuration parameter tuning. */
        CONFIG_TUNING("Configuration", "bi-sliders", "PostgreSQL configuration suggestions"),

        /** Anomalies detected in database metrics (connections, cache hits, etc.). */
        ANOMALY("Anomaly", "bi-exclamation-triangle", "Detected metric anomalies");

        private final String displayName;
        private final String icon;
        private final String description;

        Source(String displayName, String icon, String description) {
            this.displayName = displayName;
            this.icon = icon;
            this.description = description;
        }

        /**
         * Returns the human-readable display name for this source.
         *
         * @return display name (e.g., "Index Advisor")
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Returns the Bootstrap Icons class name for this source.
         *
         * @return icon class (e.g., "bi-list-ul")
         */
        public String getIcon() {
            return icon;
        }

        /**
         * Returns a brief description of this recommendation source.
         *
         * @return description text
         */
        public String getDescription() {
            return description;
        }
    }

    /**
     * Severity level indicating the urgency of addressing the recommendation.
     * <p>
     * Severity contributes to priority scoring, with higher severities
     * receiving more weight in the calculation. Each severity level includes
     * a numeric weight (1-4) and Bootstrap CSS classes for UI styling.
     */
    public enum Severity {
        /** Critical severity: immediate action required (weight: 4). */
        CRITICAL("Critical", "bg-danger", 4),

        /** High severity: should be addressed soon (weight: 3). */
        HIGH("High", "bg-warning text-dark", 3),

        /** Medium severity: address when convenient (weight: 2). */
        MEDIUM("Medium", "bg-info", 2),

        /** Low severity: minor issue or optimisation (weight: 1). */
        LOW("Low", "bg-secondary", 1);

        private final String displayName;
        private final String cssClass;
        private final int weight;

        Severity(String displayName, String cssClass, int weight) {
            this.displayName = displayName;
            this.cssClass = cssClass;
            this.weight = weight;
        }

        /**
         * Returns the human-readable display name for this severity level.
         *
         * @return display name (e.g., "Critical", "High")
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Returns the Bootstrap CSS class for styling this severity level.
         *
         * @return CSS class (e.g., "bg-danger", "bg-warning text-dark")
         */
        public String getCssClass() {
            return cssClass;
        }

        /**
         * Returns the numeric weight used in priority score calculation.
         * <p>
         * Higher weight values indicate greater urgency. Weight values
         * range from 1 (LOW) to 4 (CRITICAL).
         *
         * @return weight value (1-4)
         */
        public int getWeight() {
            return weight;
        }
    }

    /**
     * Estimated impact of applying the recommendation on database performance.
     * <p>
     * Impact represents the expected magnitude of improvement if the
     * recommendation is applied. This is used in priority scoring to
     * balance urgency (severity) with potential benefit (impact).
     */
    public enum Impact {
        /** Significant improvement expected (e.g., 50%+ performance gain). */
        HIGH("High", "Significant improvement expected"),

        /** Moderate improvement expected (e.g., 10-50% performance gain). */
        MEDIUM("Medium", "Moderate improvement expected"),

        /** Minor improvement expected (e.g., <10% performance gain). */
        LOW("Low", "Minor improvement expected");

        private final String displayName;
        private final String description;

        Impact(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        /**
         * Returns the human-readable display name for this impact level.
         *
         * @return display name (e.g., "High", "Medium")
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Returns a description of the expected improvement magnitude.
         *
         * @return description text
         */
        public String getDescription() {
            return description;
        }
    }

    /**
     * Estimated effort required to apply the recommendation.
     * <p>
     * Effort represents the time, risk, and complexity involved in implementing
     * the recommendation. Lower effort recommendations receive higher priority
     * scores, encouraging quick wins. Effort is inversely related to priority.
     */
    public enum Effort {
        /** Minimal effort: can be applied immediately with one-click (< 5 minutes, no risk). */
        MINIMAL("Minimal", "< 5 minutes, no risk"),

        /** Low effort: quick implementation with minimal planning (< 30 minutes, low risk). */
        LOW("Low", "< 30 minutes, low risk"),

        /** Medium effort: requires some planning and testing (< 2 hours, moderate planning needed). */
        MEDIUM("Medium", "< 2 hours, moderate planning needed"),

        /** High effort: significant planning, testing, and potentially downtime required. */
        HIGH("High", "Significant planning and testing needed");

        private final String displayName;
        private final String description;

        Effort(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        /**
         * Returns the human-readable display name for this effort level.
         *
         * @return display name (e.g., "Minimal", "Low")
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Returns a description of the time and risk associated with this effort level.
         *
         * @return description text including time estimate and risk level
         */
        public String getDescription() {
            return description;
        }
    }

    /**
     * Current status of the recommendation in its lifecycle.
     * <p>
     * Recommendations progress through various states from creation (OPEN)
     * to resolution (APPLIED or DISMISSED). Statuses include Bootstrap CSS
     * classes for consistent UI rendering.
     */
    public enum Status {
        /** Initial status: recommendation has been created but not yet acted upon. */
        OPEN("Open", "bg-primary", "Pending action"),

        /** Work has begun on implementing this recommendation. */
        IN_PROGRESS("In Progress", "bg-info", "Currently being worked on"),

        /** Recommendation has been successfully implemented. */
        APPLIED("Applied", "bg-success", "Successfully applied"),

        /** User has dismissed this recommendation as not applicable or desirable. */
        DISMISSED("Dismissed", "bg-secondary", "Dismissed by user"),

        /** Implementation has been postponed to a future date. */
        DEFERRED("Deferred", "bg-warning text-dark", "Scheduled for later");

        private final String displayName;
        private final String cssClass;
        private final String description;

        Status(String displayName, String cssClass, String description) {
            this.displayName = displayName;
            this.cssClass = cssClass;
            this.description = description;
        }

        /**
         * Returns the human-readable display name for this status.
         *
         * @return display name (e.g., "Open", "Applied")
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Returns the Bootstrap CSS class for styling this status.
         *
         * @return CSS class (e.g., "bg-primary", "bg-success")
         */
        public String getCssClass() {
            return cssClass;
        }

        /**
         * Returns a brief description of this status state.
         *
         * @return description text
         */
        public String getDescription() {
            return description;
        }
    }

    /**
     * Effectiveness rating recorded after a recommendation has been applied.
     * <p>
     * This enum supports post-implementation feedback to track which types
     * of recommendations deliver the most value. Effectiveness is determined
     * by comparing pre-application and post-application metric values.
     *
     * @see #getImprovementPercent()
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
