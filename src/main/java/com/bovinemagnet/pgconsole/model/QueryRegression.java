package com.bovinemagnet.pgconsole.model;

/**
 * Represents a query that has shown performance regression between two time windows.
 * <p>
 * This model captures query performance metrics from both a previous (baseline) period and
 * a current period, allowing identification of queries that have degraded in performance.
 * Performance regression is measured by comparing mean execution times, total execution times,
 * and call frequencies across the two periods.
 * </p>
 * <p>
 * The class provides computed delta values and percentage changes to quantify the regression,
 * along with a severity classification (CRITICAL, HIGH, MEDIUM, LOW) based on the magnitude
 * of the performance change. Additionally, it includes formatting methods suitable for
 * presentation in dashboards and reports.
 * </p>
 * Example usage:
 * <pre>{@code
 * QueryRegression regression = new QueryRegression();
 * regression.setPreviousMeanTime(10.5);
 * regression.setCurrentMeanTime(25.3);
 * regression.setMeanTimeChangePercent(140.95);
 * regression.setSeverity(QueryRegression.Severity.CRITICAL);
 *
 * if (regression.isRegression()) {
 *     System.out.println("Query has degraded: " + regression.getMeanTimeChangeFormatted());
 * }
 * }</pre>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see com.bovinemagnet.pgconsole.model.SlowQuery
 */
public class QueryRegression {

    /**
     * Severity classification for query performance regressions.
     * <p>
     * Severity is determined by the percentage change in mean execution time
     * between the previous and current periods. Higher percentages indicate
     * more severe performance degradation.
     * </p>
     *
     * @see #calculateSeverity(double)
     */
    public enum Severity {
        /**
         * Critical severity: query is more than 100% slower (execution time has more than doubled).
         */
        CRITICAL,

        /**
         * High severity: query is 50-100% slower.
         */
        HIGH,

        /**
         * Medium severity: query is 25-50% slower.
         */
        MEDIUM,

        /**
         * Low severity: query has exceeded the regression threshold but is less than 25% slower.
         */
        LOW
    }

    /**
     * Unique identifier for the query, typically a hash of the normalised query text.
     * This ID is consistent across executions of the same query structure.
     */
    private String queryId;

    /**
     * The full text of the SQL query with parameter placeholders normalised.
     * May be truncated for display purposes using {@link #getQueryPreview()}.
     */
    private String queryText;

    /**
     * Mean (average) execution time in milliseconds for the previous (baseline) period.
     * This represents the typical performance before the regression occurred.
     */
    private double previousMeanTime;

    /**
     * Total number of times the query was executed during the previous period.
     */
    private long previousCalls;

    /**
     * Total cumulative execution time in milliseconds for all calls during the previous period.
     */
    private double previousTotalTime;

    /**
     * Mean (average) execution time in milliseconds for the current period.
     * This represents the current performance after the regression occurred.
     */
    private double currentMeanTime;

    /**
     * Total number of times the query was executed during the current period.
     */
    private long currentCalls;

    /**
     * Total cumulative execution time in milliseconds for all calls during the current period.
     */
    private double currentTotalTime;

    /**
     * Absolute change in mean execution time (current - previous) in milliseconds.
     * Positive values indicate slower performance; negative values indicate improvement.
     */
    private double meanTimeDelta;

    /**
     * Percentage change in mean execution time relative to the previous period.
     * Calculated as ((current - previous) / previous) * 100.
     * Positive values indicate regression (slower); negative values indicate improvement (faster).
     */
    private double meanTimeChangePercent;

    /**
     * Percentage change in total execution time relative to the previous period.
     * This accounts for both changes in execution time and call frequency.
     */
    private double totalTimeChangePercent;

    /**
     * Change in the number of query calls (current - previous).
     * Positive values indicate increased query frequency; negative values indicate decreased frequency.
     */
    private long callsDelta;

    /**
     * Severity classification of this regression based on the magnitude of performance change.
     *
     * @see Severity
     */
    private Severity severity;

    /**
     * Constructs a new QueryRegression instance with all fields initialised to default values.
     * Field values should be set using the appropriate setter methods.
     */
    public QueryRegression() {
    }

    /**
     * Returns the unique identifier for the query.
     *
     * @return the query ID, or null if not set
     */
    public String getQueryId() {
        return queryId;
    }

    /**
     * Sets the unique identifier for the query.
     *
     * @param queryId the query ID to set
     */
    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }

    /**
     * Returns the full text of the SQL query.
     *
     * @return the query text, or null if not set
     */
    public String getQueryText() {
        return queryText;
    }

    /**
     * Sets the full text of the SQL query.
     *
     * @param queryText the query text to set
     */
    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }

    /**
     * Returns a truncated preview of the query text suitable for display in tables or lists.
     * <p>
     * This method normalises whitespace (collapsing multiple spaces/newlines into single spaces)
     * and truncates the query to 100 characters if it exceeds that length, appending "..." to
     * indicate truncation.
     * </p>
     *
     * @return a preview of the query text (maximum 100 characters), or empty string if query text is null
     */
    public String getQueryPreview() {
        if (queryText == null) {
            return "";
        }
        String cleaned = queryText.replaceAll("\\s+", " ").trim();
        return cleaned.length() > 100 ? cleaned.substring(0, 100) + "..." : cleaned;
    }

    /**
     * Returns the mean execution time for the previous period.
     *
     * @return the previous mean time in milliseconds
     */
    public double getPreviousMeanTime() {
        return previousMeanTime;
    }

    /**
     * Sets the mean execution time for the previous period.
     *
     * @param previousMeanTime the previous mean time in milliseconds
     */
    public void setPreviousMeanTime(double previousMeanTime) {
        this.previousMeanTime = previousMeanTime;
    }

    /**
     * Returns the number of calls during the previous period.
     *
     * @return the previous call count
     */
    public long getPreviousCalls() {
        return previousCalls;
    }

    /**
     * Sets the number of calls during the previous period.
     *
     * @param previousCalls the previous call count
     */
    public void setPreviousCalls(long previousCalls) {
        this.previousCalls = previousCalls;
    }

    /**
     * Returns the total execution time for the previous period.
     *
     * @return the previous total time in milliseconds
     */
    public double getPreviousTotalTime() {
        return previousTotalTime;
    }

    /**
     * Sets the total execution time for the previous period.
     *
     * @param previousTotalTime the previous total time in milliseconds
     */
    public void setPreviousTotalTime(double previousTotalTime) {
        this.previousTotalTime = previousTotalTime;
    }

    /**
     * Returns the mean execution time for the current period.
     *
     * @return the current mean time in milliseconds
     */
    public double getCurrentMeanTime() {
        return currentMeanTime;
    }

    /**
     * Sets the mean execution time for the current period.
     *
     * @param currentMeanTime the current mean time in milliseconds
     */
    public void setCurrentMeanTime(double currentMeanTime) {
        this.currentMeanTime = currentMeanTime;
    }

    /**
     * Returns the number of calls during the current period.
     *
     * @return the current call count
     */
    public long getCurrentCalls() {
        return currentCalls;
    }

    /**
     * Sets the number of calls during the current period.
     *
     * @param currentCalls the current call count
     */
    public void setCurrentCalls(long currentCalls) {
        this.currentCalls = currentCalls;
    }

    /**
     * Returns the total execution time for the current period.
     *
     * @return the current total time in milliseconds
     */
    public double getCurrentTotalTime() {
        return currentTotalTime;
    }

    /**
     * Sets the total execution time for the current period.
     *
     * @param currentTotalTime the current total time in milliseconds
     */
    public void setCurrentTotalTime(double currentTotalTime) {
        this.currentTotalTime = currentTotalTime;
    }

    /**
     * Returns the absolute change in mean execution time.
     *
     * @return the mean time delta in milliseconds (positive indicates regression, negative indicates improvement)
     */
    public double getMeanTimeDelta() {
        return meanTimeDelta;
    }

    /**
     * Sets the absolute change in mean execution time.
     *
     * @param meanTimeDelta the mean time delta in milliseconds
     */
    public void setMeanTimeDelta(double meanTimeDelta) {
        this.meanTimeDelta = meanTimeDelta;
    }

    /**
     * Returns the percentage change in mean execution time.
     *
     * @return the mean time change as a percentage (positive indicates regression, negative indicates improvement)
     */
    public double getMeanTimeChangePercent() {
        return meanTimeChangePercent;
    }

    /**
     * Sets the percentage change in mean execution time.
     *
     * @param meanTimeChangePercent the mean time change as a percentage
     */
    public void setMeanTimeChangePercent(double meanTimeChangePercent) {
        this.meanTimeChangePercent = meanTimeChangePercent;
    }

    /**
     * Returns the percentage change in total execution time.
     *
     * @return the total time change as a percentage
     */
    public double getTotalTimeChangePercent() {
        return totalTimeChangePercent;
    }

    /**
     * Sets the percentage change in total execution time.
     *
     * @param totalTimeChangePercent the total time change as a percentage
     */
    public void setTotalTimeChangePercent(double totalTimeChangePercent) {
        this.totalTimeChangePercent = totalTimeChangePercent;
    }

    /**
     * Returns the change in the number of calls.
     *
     * @return the calls delta (positive indicates increased frequency, negative indicates decreased frequency)
     */
    public long getCallsDelta() {
        return callsDelta;
    }

    /**
     * Sets the change in the number of calls.
     *
     * @param callsDelta the calls delta
     */
    public void setCallsDelta(long callsDelta) {
        this.callsDelta = callsDelta;
    }

    /**
     * Returns the severity classification of this regression.
     *
     * @return the severity level
     */
    public Severity getSeverity() {
        return severity;
    }

    /**
     * Sets the severity classification of this regression.
     *
     * @param severity the severity level to set
     */
    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    /**
     * Returns the Bootstrap CSS class appropriate for displaying this regression's severity as a badge.
     * This method maps severity levels to Bootstrap 5 badge colour classes:
     * <ul>
     * <li>CRITICAL: {@code bg-danger} (red background)</li>
     * <li>HIGH: {@code bg-warning text-dark} (yellow background with dark text)</li>
     * <li>MEDIUM: {@code bg-info} (blue background)</li>
     * <li>LOW: {@code bg-secondary} (grey background)</li>
     * </ul>
     *
     * @return the Bootstrap CSS class for the current severity level
     * @throws NullPointerException if severity is null
     */
    public String getSeverityCssClass() {
        return switch (severity) {
            case CRITICAL -> "bg-danger";
            case HIGH -> "bg-warning text-dark";
            case MEDIUM -> "bg-info";
            case LOW -> "bg-secondary";
        };
    }

    /**
     * Returns a formatted string representation of the mean time percentage change.
     * <p>
     * The returned string includes a direction indicator (+ or -) and is formatted
     * to one decimal place with a percentage symbol. For example: "+45.3%" or "-12.7%".
     * </p>
     *
     * @return the formatted percentage change (e.g., "+45.3%" for regression, "-12.7%" for improvement)
     */
    public String getMeanTimeChangeFormatted() {
        String sign = meanTimeChangePercent >= 0 ? "+" : "";
        return String.format("%s%.1f%%", sign, meanTimeChangePercent);
    }

    /**
     * Returns a formatted string representation of the previous period's mean execution time.
     * <p>
     * Times are formatted as seconds (e.g., "1.25 s") if 1000ms or greater,
     * otherwise as milliseconds (e.g., "123.45 ms").
     * </p>
     *
     * @return the formatted previous mean time
     */
    public String getPreviousMeanTimeFormatted() {
        return formatTime(previousMeanTime);
    }

    /**
     * Returns a formatted string representation of the current period's mean execution time.
     * <p>
     * Times are formatted as seconds (e.g., "1.25 s") if 1000ms or greater,
     * otherwise as milliseconds (e.g., "123.45 ms").
     * </p>
     *
     * @return the formatted current mean time
     */
    public String getCurrentMeanTimeFormatted() {
        return formatTime(currentMeanTime);
    }

    /**
     * Returns a formatted string representation of the calls delta with a direction indicator.
     * <p>
     * The returned string includes a + or - prefix to indicate the direction of change.
     * For example: "+150" for increased calls or "-42" for decreased calls.
     * </p>
     *
     * @return the formatted calls delta (e.g., "+150" or "-42")
     */
    public String getCallsDeltaFormatted() {
        String sign = callsDelta >= 0 ? "+" : "";
        return String.format("%s%d", sign, callsDelta);
    }

    /**
     * Indicates whether this represents a performance regression (slower execution).
     * <p>
     * A regression is defined as a positive mean time change percentage, meaning the
     * query is taking longer to execute in the current period compared to the previous period.
     * </p>
     *
     * @return true if the query has become slower (meanTimeChangePercent &gt; 0), false otherwise
     */
    public boolean isRegression() {
        return meanTimeChangePercent > 0;
    }

    /**
     * Indicates whether this represents a performance improvement (faster execution).
     * <p>
     * An improvement is defined as a negative mean time change percentage, meaning the
     * query is taking less time to execute in the current period compared to the previous period.
     * </p>
     *
     * @return true if the query has become faster (meanTimeChangePercent &lt; 0), false otherwise
     */
    public boolean isImprovement() {
        return meanTimeChangePercent < 0;
    }

    /**
     * Formats a time value in milliseconds as a human-readable string.
     * <p>
     * Times of 1000ms or greater are displayed in seconds (e.g., "1.25 s"),
     * whilst times less than 1000ms are displayed in milliseconds (e.g., "123.45 ms").
     * Both formats use two decimal places for precision.
     * </p>
     *
     * @param ms the time value in milliseconds
     * @return the formatted time string
     */
    private String formatTime(double ms) {
        if (ms >= 1000) {
            return String.format("%.2f s", ms / 1000);
        } else {
            return String.format("%.2f ms", ms);
        }
    }

    /**
     * Calculates the severity level for a given percentage change in execution time.
     * Severity levels are determined by the following thresholds:
     * <ul>
     * <li>CRITICAL: 100% or greater (execution time has at least doubled)</li>
     * <li>HIGH: 50% to &lt;100% slower</li>
     * <li>MEDIUM: 25% to &lt;50% slower</li>
     * <li>LOW: Less than 25% slower</li>
     * </ul>
     * <p>
     * Note: This method is typically used during regression analysis to classify
     * the severity based on computed percentage changes.
     * </p>
     *
     * @param changePercent the percentage change in execution time (positive values indicate regression)
     * @return the appropriate Severity level for the given percentage change
     */
    public static Severity calculateSeverity(double changePercent) {
        if (changePercent >= 100) {
            return Severity.CRITICAL;
        } else if (changePercent >= 50) {
            return Severity.HIGH;
        } else if (changePercent >= 25) {
            return Severity.MEDIUM;
        } else {
            return Severity.LOW;
        }
    }
}
