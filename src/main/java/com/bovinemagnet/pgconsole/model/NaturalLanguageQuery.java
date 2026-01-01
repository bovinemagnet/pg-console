package com.bovinemagnet.pgconsole.model;

import java.time.Instant;
import java.util.Map;

/**
 * Represents a natural language query and its parsed intent.
 * <p>
 * Maps user queries like "show me slow queries from yesterday"
 * to specific dashboard endpoints and parameters.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class NaturalLanguageQuery {

    /**
     * The intent detected from the natural language query.
     */
    public enum Intent {
        SLOW_QUERIES("Slow Queries", "/slow-queries", "View query performance"),
        SLOW_QUERIES_YESTERDAY("Slow Queries (Yesterday)", "/slow-queries", "Yesterday's slow queries"),
        SLOW_QUERIES_HOURS("Slow Queries (Hours)", "/slow-queries", "Recent slow queries"),
        TABLE_GROWTH("Table Growth", "/storage-insights", "Storage growth analysis"),
        TABLE_SIZE("Table Size", "/tables", "Table statistics"),
        SLOW_DIAGNOSIS("Slow Diagnosis", "/insights", "Why is database slow"),
        LOCKS("Locks", "/locks", "Current lock contention"),
        BLOCKING("Blocking", "/locks", "Blocked queries"),
        ACTIVITY("Activity", "/activity", "Current connections and queries"),
        REPLICATION("Replication", "/replication", "Replication status"),
        INDEX_ADVISOR("Index Advisor", "/index-advisor", "Index recommendations"),
        VACUUM("Vacuum", "/vacuum-progress", "Vacuum status and recommendations"),
        STORAGE("Storage", "/storage-insights", "Storage insights"),
        ANOMALIES("Anomalies", "/anomalies", "Detected anomalies"),
        FORECASTS("Forecasts", "/forecasts", "Metric forecasts"),
        RECOMMENDATIONS("Recommendations", "/recommendations", "All recommendations"),
        UNKNOWN("Unknown", "/", "Could not determine intent");

        private final String displayName;
        private final String targetEndpoint;
        private final String description;

        Intent(String displayName, String targetEndpoint, String description) {
            this.displayName = displayName;
            this.targetEndpoint = targetEndpoint;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getTargetEndpoint() {
            return targetEndpoint;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * User feedback on query helpfulness.
     */
    public enum Feedback {
        HELPFUL("Helpful", "The result was useful"),
        NOT_HELPFUL("Not Helpful", "The result was not useful"),
        WRONG("Wrong", "The intent was misunderstood");

        private final String displayName;
        private final String description;

        Feedback(String displayName, String description) {
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

    private Long id;
    private String queryText;
    private Intent matchedIntent;
    private String resolvedTo;
    private Map<String, String> extractedParameters;
    private boolean successful;
    private Feedback userFeedback;
    private Instant queriedAt;
    private String queriedBy;

    // Match confidence
    private double confidenceScore;
    private String matchedPattern;

    public NaturalLanguageQuery() {
        this.queriedAt = Instant.now();
        this.successful = true;
    }

    public NaturalLanguageQuery(String queryText) {
        this();
        this.queryText = queryText;
    }

    /**
     * Build the full URL with parameters.
     */
    public String buildUrl(String instance) {
        if (matchedIntent == null) {
            return "/";
        }
        StringBuilder url = new StringBuilder(matchedIntent.getTargetEndpoint());
        url.append("?instance=").append(instance);

        if (extractedParameters != null) {
            for (Map.Entry<String, String> entry : extractedParameters.entrySet()) {
                url.append("&").append(entry.getKey()).append("=").append(entry.getValue());
            }
        }

        return url.toString();
    }

    /**
     * Get a human-readable explanation of what was understood.
     */
    public String getExplanation() {
        if (matchedIntent == null || matchedIntent == Intent.UNKNOWN) {
            return "I couldn't understand your query. Try asking about slow queries, locks, activity, or storage.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Showing ").append(matchedIntent.getDescription().toLowerCase());

        if (extractedParameters != null && !extractedParameters.isEmpty()) {
            sb.append(" (");
            boolean first = true;
            for (Map.Entry<String, String> entry : extractedParameters.entrySet()) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(entry.getKey()).append(": ").append(entry.getValue());
                first = false;
            }
            sb.append(")");
        }

        return sb.toString();
    }

    /**
     * Check if this query was successfully understood.
     */
    public boolean isUnderstood() {
        return matchedIntent != null && matchedIntent != Intent.UNKNOWN;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQueryText() {
        return queryText;
    }

    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }

    public Intent getMatchedIntent() {
        return matchedIntent;
    }

    public void setMatchedIntent(Intent matchedIntent) {
        this.matchedIntent = matchedIntent;
    }

    public String getResolvedTo() {
        return resolvedTo;
    }

    public void setResolvedTo(String resolvedTo) {
        this.resolvedTo = resolvedTo;
    }

    public Map<String, String> getExtractedParameters() {
        return extractedParameters;
    }

    public void setExtractedParameters(Map<String, String> extractedParameters) {
        this.extractedParameters = extractedParameters;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public Feedback getUserFeedback() {
        return userFeedback;
    }

    public void setUserFeedback(Feedback userFeedback) {
        this.userFeedback = userFeedback;
    }

    public Instant getQueriedAt() {
        return queriedAt;
    }

    public void setQueriedAt(Instant queriedAt) {
        this.queriedAt = queriedAt;
    }

    public String getQueriedBy() {
        return queriedBy;
    }

    public void setQueriedBy(String queriedBy) {
        this.queriedBy = queriedBy;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getMatchedPattern() {
        return matchedPattern;
    }

    public void setMatchedPattern(String matchedPattern) {
        this.matchedPattern = matchedPattern;
    }
}
