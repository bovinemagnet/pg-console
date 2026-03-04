package com.bovinemagnet.pgconsole.model;

/**
 * Represents a per-query performance comparison between two time windows.
 * <p>
 * Each instance captures the execution statistics for a single query in both
 * the baseline (window A) and current (window B) periods, enabling identification
 * of regressions, improvements, and new or disappeared queries.
 * <p>
 * The {@link #status} field categorises the query as:
 * <ul>
 *   <li>{@code "existing"} - present in both windows</li>
 *   <li>{@code "new"} - only present in window B (appeared)</li>
 *   <li>{@code "gone"} - only present in window A (disappeared)</li>
 * </ul>
 * <p>
 * Computed properties provide percentage and absolute deltas, CSS styling classes
 * for template rendering, and regression detection based on a 10% threshold.
 * <p>
 * <strong>Usage example:</strong>
 * <pre>{@code
 * QueryComparison qc = new QueryComparison();
 * qc.setQueryId("abc123");
 * qc.setQueryText("SELECT * FROM users WHERE id = $1");
 * qc.setMeanTimeA(2.5);
 * qc.setMeanTimeB(3.8);
 * qc.setCallCountA(1000);
 * qc.setCallCountB(1200);
 * qc.setStatus("existing");
 * // qc.getMeanTimeChange() => 52.0 (percent increase)
 * // qc.isRegression()      => true
 * // qc.getCssClass()       => "text-danger"
 * }</pre>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see WindowComparison
 * @see ComparisonWindow
 */
public class QueryComparison {

    /** Query identifier from {@code pg_stat_statements.queryid}. */
    private String queryId;

    /** Normalised SQL query text with parameter placeholders. */
    private String queryText;

    /** Mean execution time in milliseconds during window A (baseline). */
    private double meanTimeA;

    /** Mean execution time in milliseconds during window B (current). */
    private double meanTimeB;

    /** Total call count during window A (baseline). */
    private long callCountA;

    /** Total call count during window B (current). */
    private long callCountB;

    /**
     * Status of the query across both windows.
     * <p>
     * One of: "existing" (present in both), "new" (only in B), "gone" (only in A).
     */
    private String status;

    /**
     * Constructs an empty query comparison instance.
     */
    public QueryComparison() {
    }

    // ========================================
    // Getters and Setters
    // ========================================

    /**
     * Returns the query identifier.
     *
     * @return the query ID from pg_stat_statements
     */
    public String getQueryId() {
        return queryId;
    }

    /**
     * Sets the query identifier.
     *
     * @param queryId the query ID
     */
    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }

    /**
     * Returns the normalised SQL query text.
     *
     * @return the query text with parameter placeholders
     */
    public String getQueryText() {
        return queryText;
    }

    /**
     * Sets the normalised SQL query text.
     *
     * @param queryText the query text
     */
    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }

    /**
     * Returns the mean execution time during window A (baseline).
     *
     * @return mean time in milliseconds for window A
     */
    public double getMeanTimeA() {
        return meanTimeA;
    }

    /**
     * Sets the mean execution time during window A (baseline).
     *
     * @param meanTimeA mean time in milliseconds
     */
    public void setMeanTimeA(double meanTimeA) {
        this.meanTimeA = meanTimeA;
    }

    /**
     * Returns the mean execution time during window B (current).
     *
     * @return mean time in milliseconds for window B
     */
    public double getMeanTimeB() {
        return meanTimeB;
    }

    /**
     * Sets the mean execution time during window B (current).
     *
     * @param meanTimeB mean time in milliseconds
     */
    public void setMeanTimeB(double meanTimeB) {
        this.meanTimeB = meanTimeB;
    }

    /**
     * Returns the total call count during window A (baseline).
     *
     * @return the call count for window A
     */
    public long getCallCountA() {
        return callCountA;
    }

    /**
     * Sets the total call count during window A (baseline).
     *
     * @param callCountA the call count
     */
    public void setCallCountA(long callCountA) {
        this.callCountA = callCountA;
    }

    /**
     * Returns the total call count during window B (current).
     *
     * @return the call count for window B
     */
    public long getCallCountB() {
        return callCountB;
    }

    /**
     * Sets the total call count during window B (current).
     *
     * @param callCountB the call count
     */
    public void setCallCountB(long callCountB) {
        this.callCountB = callCountB;
    }

    /**
     * Returns the status of this query across both windows.
     *
     * @return "existing", "new", or "gone"
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the status of this query across both windows.
     *
     * @param status "existing", "new", or "gone"
     */
    public void setStatus(String status) {
        this.status = status;
    }

    // ========================================
    // Computed Methods
    // ========================================

    /**
     * Calculates the percentage change in mean execution time from window A to window B.
     * <p>
     * A positive result indicates the query got slower; a negative result indicates
     * it got faster. Returns 0 if the baseline mean time is zero to avoid division
     * by zero.
     *
     * @return the percentage change in mean time
     */
    public double getMeanTimeChange() {
        if (meanTimeA == 0) {
            return 0;
        }
        return ((meanTimeB - meanTimeA) / meanTimeA) * 100.0;
    }

    /**
     * Calculates the absolute delta in mean execution time between the two windows.
     * <p>
     * A positive value indicates the query is slower in window B; a negative value
     * indicates it is faster.
     *
     * @return the absolute difference (meanTimeB - meanTimeA) in milliseconds
     */
    public double getMeanTimeDelta() {
        return meanTimeB - meanTimeA;
    }

    /**
     * Calculates the absolute delta in call count between the two windows.
     * <p>
     * A positive value indicates more calls in window B; a negative value
     * indicates fewer calls.
     *
     * @return the difference (callCountB - callCountA)
     */
    public long getCallCountDelta() {
        return callCountB - callCountA;
    }

    /**
     * Returns the Bootstrap CSS class for colour-coding the mean time change.
     * <p>
     * The class is determined by the percentage change in mean execution time:
     * <ul>
     *   <li>{@code "text-danger"} - mean time increased by more than 10% (regression)</li>
     *   <li>{@code "text-success"} - mean time decreased by more than 10% (improvement)</li>
     *   <li>{@code ""} (empty) - change is within the 10% threshold (neutral)</li>
     * </ul>
     *
     * @return the CSS class name for template rendering
     */
    public String getCssClass() {
        double change = getMeanTimeChange();
        if (change > 10.0) {
            return "text-danger";
        } else if (change < -10.0) {
            return "text-success";
        }
        return "";
    }

    /**
     * Returns whether this query represents a performance regression.
     * <p>
     * A query is considered a regression if its mean execution time increased
     * by more than 10% from window A to window B.
     *
     * @return true if mean time increased by more than 10%
     */
    public boolean isRegression() {
        return getMeanTimeChange() > 10.0;
    }
}
