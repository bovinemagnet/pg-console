package com.bovinemagnet.pgconsole.model;

import java.time.Instant;
import java.util.Map;

/**
 * Represents a natural language query and its parsed intent for the PostgreSQL Console dashboard.
 * <p>
 * This class encapsulates the user's natural language query text, the detected intent,
 * extracted parameters, and metadata about the query resolution process. It enables users
 * to interact with the PostgreSQL monitoring dashboard using conversational queries like
 * "show me slow queries from yesterday" instead of navigating through menus.
 * <p>
 * The query resolution process involves:
 * <ul>
 *   <li>Parsing the user's natural language input</li>
 *   <li>Matching against known intent patterns</li>
 *   <li>Extracting relevant parameters (time ranges, database names, etc.)</li>
 *   <li>Building the appropriate dashboard URL with query parameters</li>
 *   <li>Tracking user feedback for continuous improvement</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>{@code
 * NaturalLanguageQuery query = new NaturalLanguageQuery("show me slow queries from yesterday");
 * query.setMatchedIntent(Intent.SLOW_QUERIES_YESTERDAY);
 * query.setExtractedParameters(Map.of("timeRange", "yesterday"));
 * String url = query.buildUrl("production-db");
 * // Returns: /slow-queries?instance=production-db&timeRange=yesterday
 * }</pre>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @since 0.0.0
 * @see Intent
 * @see Feedback
 */
public class NaturalLanguageQuery {

    /**
     * Represents the detected intent from a natural language query.
     * <p>
     * Each intent corresponds to a specific dashboard page or feature in the PostgreSQL
     * Console. The intent determines which endpoint to navigate to and what information
     * to display to the user.
     * <p>
     * Intents are matched against user queries using pattern matching and keyword detection.
     * When a query cannot be confidently matched to any intent, the {@link #UNKNOWN} intent
     * is used as a fallback.
     *
     * @since 0.0.0
     */
    public enum Intent {
        /** Intent for general slow query analysis. */
        SLOW_QUERIES("Slow Queries", "/slow-queries", "View query performance"),

        /** Intent for viewing slow queries specifically from yesterday. */
        SLOW_QUERIES_YESTERDAY("Slow Queries (Yesterday)", "/slow-queries", "Yesterday's slow queries"),

        /** Intent for viewing slow queries from a specific number of hours. */
        SLOW_QUERIES_HOURS("Slow Queries (Hours)", "/slow-queries", "Recent slow queries"),

        /** Intent for analysing table growth over time. */
        TABLE_GROWTH("Table Growth", "/storage-insights", "Storage growth analysis"),

        /** Intent for viewing current table sizes and statistics. */
        TABLE_SIZE("Table Size", "/tables", "Table statistics"),

        /** Intent for diagnosing general database slowness. */
        SLOW_DIAGNOSIS("Slow Diagnosis", "/insights", "Why is database slow"),

        /** Intent for viewing current lock contention. */
        LOCKS("Locks", "/locks", "Current lock contention"),

        /** Intent for viewing blocked queries and blocking relationships. */
        BLOCKING("Blocking", "/locks", "Blocked queries"),

        /** Intent for viewing current database connections and active queries. */
        ACTIVITY("Activity", "/activity", "Current connections and queries"),

        /** Intent for viewing replication lag and status. */
        REPLICATION("Replication", "/replication", "Replication status"),

        /** Intent for viewing index recommendations and suggestions. */
        INDEX_ADVISOR("Index Advisor", "/index-advisor", "Index recommendations"),

        /** Intent for viewing vacuum progress and recommendations. */
        VACUUM("Vacuum", "/vacuum-progress", "Vacuum status and recommendations"),

        /** Intent for general storage insights and analysis. */
        STORAGE("Storage", "/storage-insights", "Storage insights"),

        /** Intent for viewing detected anomalies in database metrics. */
        ANOMALIES("Anomalies", "/anomalies", "Detected anomalies"),

        /** Intent for viewing metric forecasts and predictions. */
        FORECASTS("Forecasts", "/forecasts", "Metric forecasts"),

        /** Intent for viewing all system recommendations. */
        RECOMMENDATIONS("Recommendations", "/recommendations", "All recommendations"),

        /** Fallback intent when the query cannot be matched to any specific intent. */
        UNKNOWN("Unknown", "/", "Could not determine intent");

        private final String displayName;
        private final String targetEndpoint;
        private final String description;

        /**
         * Constructs an Intent with the specified display name, target endpoint, and description.
         *
         * @param displayName the human-readable name for this intent
         * @param targetEndpoint the dashboard endpoint path this intent maps to
         * @param description a brief description of what this intent provides
         */
        Intent(String displayName, String targetEndpoint, String description) {
            this.displayName = displayName;
            this.targetEndpoint = targetEndpoint;
            this.description = description;
        }

        /**
         * Returns the human-readable display name for this intent.
         * <p>
         * This name is typically shown in the UI to help users understand what the
         * query was interpreted as.
         *
         * @return the display name, never null
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Returns the dashboard endpoint path this intent targets.
         * <p>
         * The endpoint is used to construct the full URL for navigation, combined with
         * instance and other extracted parameters.
         *
         * @return the endpoint path (e.g., "/slow-queries", "/locks"), never null
         */
        public String getTargetEndpoint() {
            return targetEndpoint;
        }

        /**
         * Returns a brief description of what this intent provides.
         * <p>
         * The description explains the type of information or analysis this intent
         * will show to the user.
         *
         * @return the description text, never null
         */
        public String getDescription() {
            return description;
        }
    }

    /**
     * Represents user feedback on the helpfulness and accuracy of query resolution.
     * <p>
     * This feedback mechanism allows users to indicate whether the natural language
     * query was correctly interpreted and whether the resulting page was useful.
     * Feedback data can be used to improve intent matching algorithms and pattern
     * recognition over time.
     *
     * @since 0.0.0
     */
    public enum Feedback {
        /** Indicates the query result was useful and correctly interpreted. */
        HELPFUL("Helpful", "The result was useful"),

        /** Indicates the result was not useful, though the intent may have been correct. */
        NOT_HELPFUL("Not Helpful", "The result was not useful"),

        /** Indicates the intent was completely misunderstood. */
        WRONG("Wrong", "The intent was misunderstood");

        private final String displayName;
        private final String description;

        /**
         * Constructs a Feedback value with the specified display name and description.
         *
         * @param displayName the human-readable name for this feedback option
         * @param description a brief explanation of what this feedback indicates
         */
        Feedback(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        /**
         * Returns the human-readable display name for this feedback option.
         *
         * @return the display name, never null
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Returns a description explaining what this feedback indicates.
         *
         * @return the description text, never null
         */
        public String getDescription() {
            return description;
        }
    }

    /** Unique identifier for this query record. May be null if not persisted. */
    private Long id;

    /** The original natural language query text entered by the user. */
    private String queryText;

    /** The intent that was matched from the query text. */
    private Intent matchedIntent;

    /** The final resolved endpoint or description of where the query led. */
    private String resolvedTo;

    /**
     * Parameters extracted from the natural language query.
     * <p>
     * Common parameter keys include:
     * <ul>
     *   <li>{@code timeRange} - Time period specification (e.g., "yesterday", "24h")</li>
     *   <li>{@code database} - Specific database name</li>
     *   <li>{@code hours} - Number of hours for time-based queries</li>
     *   <li>{@code table} - Specific table name</li>
     * </ul>
     */
    private Map<String, String> extractedParameters;

    /** Indicates whether the query was successfully resolved to an intent. */
    private boolean successful;

    /** User feedback on the query result, if provided. */
    private Feedback userFeedback;

    /** Timestamp when the query was submitted. */
    private Instant queriedAt;

    /** Username or identifier of who submitted the query. May be null for anonymous queries. */
    private String queriedBy;

    /**
     * Confidence score for the intent match, ranging from 0.0 to 1.0.
     * <p>
     * Higher scores indicate stronger confidence in the intent matching.
     * Scores below a certain threshold may result in the {@link Intent#UNKNOWN} intent.
     */
    private double confidenceScore;

    /**
     * The pattern or rule that was matched to determine the intent.
     * <p>
     * This helps with debugging intent matching and understanding which rules
     * are being triggered by specific query phrasings.
     */
    private String matchedPattern;

    /**
     * Constructs a new NaturalLanguageQuery with default values.
     * <p>
     * Sets {@code queriedAt} to the current timestamp and {@code successful} to true.
     */
    public NaturalLanguageQuery() {
        this.queriedAt = Instant.now();
        this.successful = true;
    }

    /**
     * Constructs a new NaturalLanguageQuery with the specified query text.
     * <p>
     * Sets {@code queriedAt} to the current timestamp and {@code successful} to true.
     *
     * @param queryText the natural language query text, may be null
     */
    public NaturalLanguageQuery(String queryText) {
        this();
        this.queryText = queryText;
    }

    /**
     * Builds the full URL with the instance parameter and any extracted parameters.
     * <p>
     * Constructs a URL by combining the matched intent's target endpoint with the
     * instance identifier and any parameters extracted from the natural language query.
     * If no intent is matched, returns the root path "/".
     * <p>
     * Example output: {@code /slow-queries?instance=production-db&timeRange=yesterday}
     *
     * @param instance the database instance identifier to include in the URL
     * @return the complete URL path with query parameters, never null
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
     * Returns a human-readable explanation of what was understood from the query.
     * <p>
     * Generates a user-friendly message describing the matched intent and any
     * extracted parameters. If the query could not be understood, provides
     * helpful guidance on what types of queries are supported.
     * <p>
     * Example outputs:
     * <ul>
     *   <li>"Showing view query performance (timeRange: yesterday)"</li>
     *   <li>"Showing current lock contention"</li>
     *   <li>"I couldn't understand your query. Try asking about slow queries, locks, activity, or storage."</li>
     * </ul>
     *
     * @return a human-readable explanation of the query interpretation, never null
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
     * Checks if this query was successfully understood and matched to a valid intent.
     * <p>
     * Returns true if the query has a matched intent that is not {@link Intent#UNKNOWN}.
     * This is a convenience method to quickly determine if the query resolution succeeded.
     *
     * @return true if the query was understood, false otherwise
     */
    public boolean isUnderstood() {
        return matchedIntent != null && matchedIntent != Intent.UNKNOWN;
    }

    /**
     * Returns the unique identifier for this query record.
     *
     * @return the query ID, or null if not persisted
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the unique identifier for this query record.
     *
     * @param id the query ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns the original natural language query text entered by the user.
     *
     * @return the query text, may be null
     */
    public String getQueryText() {
        return queryText;
    }

    /**
     * Sets the natural language query text.
     *
     * @param queryText the query text
     */
    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }

    /**
     * Returns the intent that was matched from the query text.
     *
     * @return the matched intent, or null if not yet determined
     */
    public Intent getMatchedIntent() {
        return matchedIntent;
    }

    /**
     * Sets the matched intent for this query.
     *
     * @param matchedIntent the intent to set
     */
    public void setMatchedIntent(Intent matchedIntent) {
        this.matchedIntent = matchedIntent;
    }

    /**
     * Returns the final resolved endpoint or description of where the query led.
     *
     * @return the resolved destination, may be null
     */
    public String getResolvedTo() {
        return resolvedTo;
    }

    /**
     * Sets the final resolved endpoint or description.
     *
     * @param resolvedTo the resolved destination
     */
    public void setResolvedTo(String resolvedTo) {
        this.resolvedTo = resolvedTo;
    }

    /**
     * Returns the parameters extracted from the natural language query.
     * <p>
     * The map contains key-value pairs where keys are parameter names (e.g., "timeRange",
     * "database", "hours") and values are the extracted values.
     *
     * @return the extracted parameters map, may be null or empty
     */
    public Map<String, String> getExtractedParameters() {
        return extractedParameters;
    }

    /**
     * Sets the extracted parameters for this query.
     *
     * @param extractedParameters the parameters map
     */
    public void setExtractedParameters(Map<String, String> extractedParameters) {
        this.extractedParameters = extractedParameters;
    }

    /**
     * Returns whether the query was successfully resolved to an intent.
     *
     * @return true if successful, false otherwise
     */
    public boolean isSuccessful() {
        return successful;
    }

    /**
     * Sets the success status of the query resolution.
     *
     * @param successful true if successful, false otherwise
     */
    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    /**
     * Returns the user feedback on the query result, if provided.
     *
     * @return the user feedback, or null if not yet provided
     */
    public Feedback getUserFeedback() {
        return userFeedback;
    }

    /**
     * Sets the user feedback for this query.
     *
     * @param userFeedback the feedback value
     */
    public void setUserFeedback(Feedback userFeedback) {
        this.userFeedback = userFeedback;
    }

    /**
     * Returns the timestamp when the query was submitted.
     *
     * @return the query timestamp, never null for properly constructed instances
     */
    public Instant getQueriedAt() {
        return queriedAt;
    }

    /**
     * Sets the timestamp when the query was submitted.
     *
     * @param queriedAt the timestamp
     */
    public void setQueriedAt(Instant queriedAt) {
        this.queriedAt = queriedAt;
    }

    /**
     * Returns the username or identifier of who submitted the query.
     *
     * @return the user identifier, or null for anonymous queries
     */
    public String getQueriedBy() {
        return queriedBy;
    }

    /**
     * Sets the user identifier for who submitted the query.
     *
     * @param queriedBy the user identifier
     */
    public void setQueriedBy(String queriedBy) {
        this.queriedBy = queriedBy;
    }

    /**
     * Returns the confidence score for the intent match.
     * <p>
     * The score ranges from 0.0 (no confidence) to 1.0 (highest confidence).
     * Higher scores indicate the pattern matching algorithm is more confident
     * that the matched intent is correct.
     *
     * @return the confidence score, typically between 0.0 and 1.0
     */
    public double getConfidenceScore() {
        return confidenceScore;
    }

    /**
     * Sets the confidence score for the intent match.
     *
     * @param confidenceScore the confidence score, typically between 0.0 and 1.0
     */
    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    /**
     * Returns the pattern or rule that was matched to determine the intent.
     * <p>
     * This value is useful for debugging and understanding which specific
     * pattern or keyword triggered the intent match.
     *
     * @return the matched pattern identifier, may be null
     */
    public String getMatchedPattern() {
        return matchedPattern;
    }

    /**
     * Sets the pattern or rule that was matched.
     *
     * @param matchedPattern the pattern identifier
     */
    public void setMatchedPattern(String matchedPattern) {
        this.matchedPattern = matchedPattern;
    }
}
