package com.bovinemagnet.pgconsole.model;

/**
 * Represents a query comparison between two time periods for baseline analysis.
 * <p>
 * This model is used to track and compare query performance metrics from PostgreSQL's
 * {@code pg_stat_statements} extension across different time periods. It captures both
 * previous and current period statistics, calculates deltas and percentage changes,
 * and classifies queries into movement types (new, removed, increased, decreased, or stable).
 * <p>
 * The primary use case is identifying "top movers" - queries that have shown significant
 * changes in execution frequency or performance between two sampling periods. This helps
 * database administrators quickly identify queries that may require attention due to
 * performance degradation or unexpected usage patterns.
 * <p>
 * Each instance includes:
 * <ul>
 * <li>Query identification and text</li>
 * <li>Previous period metrics (calls, total time, mean time)</li>
 * <li>Current period metrics (calls, total time, mean time)</li>
 * <li>Calculated deltas and percentage changes</li>
 * <li>Movement classification and impact score for ranking</li>
 * <li>Helper methods for formatted output in dashboards</li>
 * </ul>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see com.bovinemagnet.pgconsole.service.PostgresService
 * @since 0.0.0
 */
public class QueryBaseline {

    /**
     * Classifies the type of change a query has undergone between two time periods.
     * <p>
     * Movement types are determined by comparing previous and current period statistics
     * and applying configurable thresholds to classify the magnitude and direction of change.
     */
    public enum MovementType {
        /** Query appeared in the current period but was not present in the previous period. */
        NEW_QUERY,

        /** Query was present in the previous period but has disappeared from the current period. */
        REMOVED,

        /** Query execution frequency or total time has increased beyond the threshold. */
        INCREASED,

        /** Query execution frequency or total time has decreased beyond the threshold. */
        DECREASED,

        /** Query metrics remain within the stable threshold range. */
        STABLE
    }

    /**
     * Unique identifier for the query from {@code pg_stat_statements}.
     * Typically the query hash or identifier used by PostgreSQL.
     */
    private String queryId;

    /**
     * The full SQL query text.
     * May contain parameter placeholders depending on {@code pg_stat_statements} configuration.
     */
    private String queryText;

    /**
     * Number of times the query was executed in the previous time period.
     */
    private long previousCalls;

    /**
     * Total execution time in milliseconds for all calls in the previous period.
     */
    private double previousTotalTime;

    /**
     * Average execution time in milliseconds per call in the previous period.
     */
    private double previousMeanTime;

    /**
     * Number of times the query was executed in the current time period.
     */
    private long currentCalls;

    /**
     * Total execution time in milliseconds for all calls in the current period.
     */
    private double currentTotalTime;

    /**
     * Average execution time in milliseconds per call in the current period.
     */
    private double currentMeanTime;

    /**
     * Absolute change in execution count (current calls minus previous calls).
     */
    private long callsDelta;

    /**
     * Absolute change in total execution time in milliseconds.
     */
    private double totalTimeDelta;

    /**
     * Absolute change in mean execution time in milliseconds.
     */
    private double meanTimeDelta;

    /**
     * Percentage change in execution count.
     * Calculated as {@code ((currentCalls - previousCalls) / previousCalls) * 100}.
     */
    private double callsChangePercent;

    /**
     * Percentage change in total execution time.
     * Calculated as {@code ((currentTotalTime - previousTotalTime) / previousTotalTime) * 100}.
     */
    private double totalTimeChangePercent;

    /**
     * Percentage change in mean execution time.
     * Calculated as {@code ((currentMeanTime - previousMeanTime) / previousMeanTime) * 100}.
     */
    private double meanTimeChangePercent;

    /**
     * Classification of the query's movement pattern.
     * @see MovementType
     */
    private MovementType movementType;

    /**
     * Combined impact score used for ranking queries by significance.
     * Higher scores indicate queries with greater impact on database performance.
     * Typically calculated from a weighted combination of call frequency and execution time changes.
     */
    private double impactScore;

    /**
     * Constructs a new QueryBaseline instance with default values.
     * All numeric fields are initialised to zero, and all object fields to null.
     */
    public QueryBaseline() {
    }

    /**
     * Retrieves the unique query identifier.
     *
     * @return the query identifier from {@code pg_stat_statements}, or null if not set
     */
    public String getQueryId() {
        return queryId;
    }

    /**
     * Sets the unique query identifier.
     *
     * @param queryId the query identifier from {@code pg_stat_statements}
     */
    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }

    /**
     * Retrieves the full SQL query text.
     *
     * @return the complete query text, or null if not set
     */
    public String getQueryText() {
        return queryText;
    }

    /**
     * Sets the full SQL query text.
     *
     * @param queryText the complete query text
     */
    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }

    /**
     * Generates a truncated preview of the query text suitable for dashboard display.
     * <p>
     * The preview is created by normalising whitespace (collapsing multiple spaces into one)
     * and truncating to 80 characters with an ellipsis if the query exceeds that length.
     *
     * @return a formatted preview string, limited to 80 characters plus ellipsis if truncated;
     *         returns an empty string if query text is null
     */
    public String getQueryPreview() {
        if (queryText == null) {
            return "";
        }
        String cleaned = queryText.replaceAll("\\s+", " ").trim();
        return cleaned.length() > 80 ? cleaned.substring(0, 80) + "..." : cleaned;
    }

    /**
     * Retrieves the number of query executions in the previous time period.
     *
     * @return the execution count from the previous period
     */
    public long getPreviousCalls() {
        return previousCalls;
    }

    /**
     * Sets the number of query executions in the previous time period.
     *
     * @param previousCalls the execution count from the previous period
     */
    public void setPreviousCalls(long previousCalls) {
        this.previousCalls = previousCalls;
    }

    /**
     * Retrieves the total execution time for all calls in the previous period.
     *
     * @return the total execution time in milliseconds
     */
    public double getPreviousTotalTime() {
        return previousTotalTime;
    }

    /**
     * Sets the total execution time for all calls in the previous period.
     *
     * @param previousTotalTime the total execution time in milliseconds
     */
    public void setPreviousTotalTime(double previousTotalTime) {
        this.previousTotalTime = previousTotalTime;
    }

    /**
     * Retrieves the average execution time per call in the previous period.
     *
     * @return the mean execution time in milliseconds
     */
    public double getPreviousMeanTime() {
        return previousMeanTime;
    }

    /**
     * Sets the average execution time per call in the previous period.
     *
     * @param previousMeanTime the mean execution time in milliseconds
     */
    public void setPreviousMeanTime(double previousMeanTime) {
        this.previousMeanTime = previousMeanTime;
    }

    /**
     * Retrieves the number of query executions in the current time period.
     *
     * @return the execution count from the current period
     */
    public long getCurrentCalls() {
        return currentCalls;
    }

    /**
     * Sets the number of query executions in the current time period.
     *
     * @param currentCalls the execution count from the current period
     */
    public void setCurrentCalls(long currentCalls) {
        this.currentCalls = currentCalls;
    }

    /**
     * Retrieves the total execution time for all calls in the current period.
     *
     * @return the total execution time in milliseconds
     */
    public double getCurrentTotalTime() {
        return currentTotalTime;
    }

    /**
     * Sets the total execution time for all calls in the current period.
     *
     * @param currentTotalTime the total execution time in milliseconds
     */
    public void setCurrentTotalTime(double currentTotalTime) {
        this.currentTotalTime = currentTotalTime;
    }

    /**
     * Retrieves the average execution time per call in the current period.
     *
     * @return the mean execution time in milliseconds
     */
    public double getCurrentMeanTime() {
        return currentMeanTime;
    }

    /**
     * Sets the average execution time per call in the current period.
     *
     * @param currentMeanTime the mean execution time in milliseconds
     */
    public void setCurrentMeanTime(double currentMeanTime) {
        this.currentMeanTime = currentMeanTime;
    }

    /**
     * Retrieves the absolute change in execution count between periods.
     *
     * @return the difference between current and previous calls (current - previous)
     */
    public long getCallsDelta() {
        return callsDelta;
    }

    /**
     * Sets the absolute change in execution count between periods.
     *
     * @param callsDelta the difference between current and previous calls
     */
    public void setCallsDelta(long callsDelta) {
        this.callsDelta = callsDelta;
    }

    /**
     * Retrieves the absolute change in total execution time between periods.
     *
     * @return the difference in total time in milliseconds (current - previous)
     */
    public double getTotalTimeDelta() {
        return totalTimeDelta;
    }

    /**
     * Sets the absolute change in total execution time between periods.
     *
     * @param totalTimeDelta the difference in total time in milliseconds
     */
    public void setTotalTimeDelta(double totalTimeDelta) {
        this.totalTimeDelta = totalTimeDelta;
    }

    /**
     * Retrieves the absolute change in mean execution time between periods.
     *
     * @return the difference in mean time in milliseconds (current - previous)
     */
    public double getMeanTimeDelta() {
        return meanTimeDelta;
    }

    /**
     * Sets the absolute change in mean execution time between periods.
     *
     * @param meanTimeDelta the difference in mean time in milliseconds
     */
    public void setMeanTimeDelta(double meanTimeDelta) {
        this.meanTimeDelta = meanTimeDelta;
    }

    /**
     * Retrieves the percentage change in execution count.
     *
     * @return the percentage change, calculated as {@code ((current - previous) / previous) * 100}
     */
    public double getCallsChangePercent() {
        return callsChangePercent;
    }

    /**
     * Sets the percentage change in execution count.
     *
     * @param callsChangePercent the percentage change in call frequency
     */
    public void setCallsChangePercent(double callsChangePercent) {
        this.callsChangePercent = callsChangePercent;
    }

    /**
     * Retrieves the percentage change in total execution time.
     *
     * @return the percentage change, calculated as {@code ((current - previous) / previous) * 100}
     */
    public double getTotalTimeChangePercent() {
        return totalTimeChangePercent;
    }

    /**
     * Sets the percentage change in total execution time.
     *
     * @param totalTimeChangePercent the percentage change in total time
     */
    public void setTotalTimeChangePercent(double totalTimeChangePercent) {
        this.totalTimeChangePercent = totalTimeChangePercent;
    }

    /**
     * Retrieves the percentage change in mean execution time.
     *
     * @return the percentage change, calculated as {@code ((current - previous) / previous) * 100}
     */
    public double getMeanTimeChangePercent() {
        return meanTimeChangePercent;
    }

    /**
     * Sets the percentage change in mean execution time.
     *
     * @param meanTimeChangePercent the percentage change in mean time
     */
    public void setMeanTimeChangePercent(double meanTimeChangePercent) {
        this.meanTimeChangePercent = meanTimeChangePercent;
    }

    /**
     * Retrieves the movement classification for this query.
     *
     * @return the movement type indicating how this query has changed between periods
     * @see MovementType
     */
    public MovementType getMovementType() {
        return movementType;
    }

    /**
     * Sets the movement classification for this query.
     *
     * @param movementType the movement type classification
     * @see MovementType
     */
    public void setMovementType(MovementType movementType) {
        this.movementType = movementType;
    }

    /**
     * Retrieves the calculated impact score for ranking purposes.
     *
     * @return the impact score; higher values indicate greater significance
     */
    public double getImpactScore() {
        return impactScore;
    }

    /**
     * Sets the calculated impact score for ranking purposes.
     *
     * @param impactScore the impact score value
     */
    public void setImpactScore(double impactScore) {
        this.impactScore = impactScore;
    }

    /**
     * Retrieves the Bootstrap CSS class appropriate for the movement type.
     * <p>
     * This method maps movement types to Bootstrap background colour classes
     * for visual indication in the dashboard:
     * <ul>
     * <li>NEW_QUERY: "bg-success" (green)</li>
     * <li>REMOVED: "bg-secondary" (grey)</li>
     * <li>INCREASED: "bg-danger" (red)</li>
     * <li>DECREASED: "bg-info" (blue)</li>
     * <li>STABLE: "bg-light text-dark" (light grey)</li>
     * </ul>
     *
     * @return the Bootstrap CSS class name for styling based on movement type
     */
    public String getMovementTypeCssClass() {
        return switch (movementType) {
            case NEW_QUERY -> "bg-success";
            case REMOVED -> "bg-secondary";
            case INCREASED -> "bg-danger";
            case DECREASED -> "bg-info";
            case STABLE -> "bg-light text-dark";
        };
    }

    /**
     * Retrieves a human-readable display label for the movement type.
     *
     * @return the display name of the movement type ("New", "Removed", "Increased", "Decreased", or "Stable")
     */
    public String getMovementTypeDisplay() {
        return switch (movementType) {
            case NEW_QUERY -> "New";
            case REMOVED -> "Removed";
            case INCREASED -> "Increased";
            case DECREASED -> "Decreased";
            case STABLE -> "Stable";
        };
    }

    /**
     * Formats the calls change percentage for dashboard display.
     * <p>
     * Returns "New" for new queries, "Removed" for removed queries, or a formatted
     * percentage string with sign and no decimal places for other movement types.
     *
     * @return formatted percentage string (e.g., "+25%", "-10%", "New", "Removed")
     */
    public String getCallsChangeFormatted() {
        if (movementType == MovementType.NEW_QUERY) return "New";
        if (movementType == MovementType.REMOVED) return "Removed";
        String sign = callsChangePercent >= 0 ? "+" : "";
        return String.format("%s%.0f%%", sign, callsChangePercent);
    }

    /**
     * Formats the total time change percentage for dashboard display.
     * <p>
     * Returns "New" for new queries, "Removed" for removed queries, or a formatted
     * percentage string with sign and no decimal places for other movement types.
     *
     * @return formatted percentage string (e.g., "+150%", "-30%", "New", "Removed")
     */
    public String getTotalTimeChangeFormatted() {
        if (movementType == MovementType.NEW_QUERY) return "New";
        if (movementType == MovementType.REMOVED) return "Removed";
        String sign = totalTimeChangePercent >= 0 ? "+" : "";
        return String.format("%s%.0f%%", sign, totalTimeChangePercent);
    }

    /**
     * Formats the mean time change percentage for dashboard display.
     * <p>
     * Returns an em dash ("—") for new or removed queries (where mean time comparison
     * is not applicable), or a formatted percentage string with sign and no decimal places
     * for other movement types.
     *
     * @return formatted percentage string (e.g., "+75%", "-20%") or "—" for new/removed queries
     */
    public String getMeanTimeChangeFormatted() {
        if (movementType == MovementType.NEW_QUERY) return "—";
        if (movementType == MovementType.REMOVED) return "—";
        String sign = meanTimeChangePercent >= 0 ? "+" : "";
        return String.format("%s%.0f%%", sign, meanTimeChangePercent);
    }

    /**
     * Formats a time value in milliseconds for human-readable display.
     * <p>
     * Times of 1000ms or greater are converted to seconds with two decimal places.
     * Times less than 1000ms are displayed in milliseconds with two decimal places.
     *
     * @param ms the time value in milliseconds
     * @return formatted time string (e.g., "1.25 s", "345.67 ms")
     */
    public String formatTime(double ms) {
        if (ms >= 1000) {
            return String.format("%.2f s", ms / 1000);
        } else {
            return String.format("%.2f ms", ms);
        }
    }

    /**
     * Retrieves the previous period's total time as a formatted string.
     *
     * @return formatted time string in seconds or milliseconds
     * @see #formatTime(double)
     */
    public String getPreviousTotalTimeFormatted() {
        return formatTime(previousTotalTime);
    }

    /**
     * Retrieves the current period's total time as a formatted string.
     *
     * @return formatted time string in seconds or milliseconds
     * @see #formatTime(double)
     */
    public String getCurrentTotalTimeFormatted() {
        return formatTime(currentTotalTime);
    }

    /**
     * Retrieves the total time delta as a formatted string with sign.
     * <p>
     * Positive deltas are prefixed with "+", negative deltas retain their minus sign.
     *
     * @return formatted delta string with sign (e.g., "+1.25 s", "-345.67 ms")
     * @see #formatTime(double)
     */
    public String getTotalTimeDeltaFormatted() {
        String sign = totalTimeDelta >= 0 ? "+" : "";
        return sign + formatTime(totalTimeDelta);
    }

    /**
     * Determines if this query represents a significant performance degradation.
     * <p>
     * A query is considered significantly increased if it has an INCREASED movement type
     * and the total time has grown by more than 50%.
     *
     * @return true if total time increased by more than 50%, false otherwise
     */
    public boolean isSignificantIncrease() {
        return movementType == MovementType.INCREASED && totalTimeChangePercent > 50;
    }

    /**
     * Determines if this query represents a significant performance improvement.
     * <p>
     * A query is considered significantly decreased if it has a DECREASED movement type
     * and the total time has decreased by more than 50%.
     *
     * @return true if total time decreased by more than 50%, false otherwise
     */
    public boolean isSignificantDecrease() {
        return movementType == MovementType.DECREASED && totalTimeChangePercent < -50;
    }
}
