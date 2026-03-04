package com.bovinemagnet.pgconsole.model;

import java.util.List;

/**
 * Represents the full result of comparing two time windows, containing the
 * paired windows, metric deltas, and per-query performance changes.
 * <p>
 * A {@code WindowComparison} aggregates the results of a side-by-side analysis
 * between two {@link ComparisonWindow} instances (window A and window B). The
 * {@link #deltas} list provides metric-level before/after comparisons, whilst
 * the {@link #queryChanges} list identifies individual query regressions,
 * improvements, and new/gone queries.
 * <p>
 * The {@link #getOverallDirection()} method summarises the comparison as
 * "improved", "degraded", or "mixed" based on the majority of metric deltas,
 * providing a quick at-a-glance assessment for dashboard rendering.
 * <p>
 * <strong>Usage example:</strong>
 * <pre>{@code
 * WindowComparison comparison = new WindowComparison();
 * comparison.setWindowA(yesterdayWindow);
 * comparison.setWindowB(todayWindow);
 * comparison.setDeltas(metricDeltas);
 * comparison.setQueryChanges(queryComparisons);
 * // comparison.hasData()             => true (if both windows have data)
 * // comparison.getOverallDirection() => "improved"
 * }</pre>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see ComparisonWindow
 * @see MetricDelta
 * @see QueryComparison
 */
public class WindowComparison {

    /** The first (earlier/baseline) comparison window. */
    private ComparisonWindow windowA;

    /** The second (later/current) comparison window. */
    private ComparisonWindow windowB;

    /** Metric-level deltas comparing window A to window B. */
    private List<MetricDelta> deltas;

    /** Per-query performance changes between the two windows. */
    private List<QueryComparison> queryChanges;

    /**
     * Constructs an empty window comparison instance.
     */
    public WindowComparison() {
    }

    // ========================================
    // Getters and Setters
    // ========================================

    /**
     * Returns the first (baseline) comparison window.
     *
     * @return window A
     */
    public ComparisonWindow getWindowA() {
        return windowA;
    }

    /**
     * Sets the first (baseline) comparison window.
     *
     * @param windowA the baseline window
     */
    public void setWindowA(ComparisonWindow windowA) {
        this.windowA = windowA;
    }

    /**
     * Returns the second (current) comparison window.
     *
     * @return window B
     */
    public ComparisonWindow getWindowB() {
        return windowB;
    }

    /**
     * Sets the second (current) comparison window.
     *
     * @param windowB the current window
     */
    public void setWindowB(ComparisonWindow windowB) {
        this.windowB = windowB;
    }

    /**
     * Returns the list of metric deltas between the two windows.
     *
     * @return the metric deltas, or null if not yet computed
     */
    public List<MetricDelta> getDeltas() {
        return deltas;
    }

    /**
     * Sets the list of metric deltas between the two windows.
     *
     * @param deltas the metric deltas
     */
    public void setDeltas(List<MetricDelta> deltas) {
        this.deltas = deltas;
    }

    /**
     * Returns the list of per-query performance changes.
     *
     * @return the query comparisons, or null if not yet computed
     */
    public List<QueryComparison> getQueryChanges() {
        return queryChanges;
    }

    /**
     * Sets the list of per-query performance changes.
     *
     * @param queryChanges the query comparisons
     */
    public void setQueryChanges(List<QueryComparison> queryChanges) {
        this.queryChanges = queryChanges;
    }

    // ========================================
    // Computed Methods
    // ========================================

    /**
     * Returns whether both windows contain sufficient data for a meaningful comparison.
     * <p>
     * Both windows must have at least one history sample for the comparison
     * to be considered valid.
     *
     * @return true if both window A and window B have data
     */
    public boolean hasData() {
        return windowA != null && windowB != null
                && windowA.hasData() && windowB.hasData();
    }

    /**
     * Returns the overall direction of the comparison based on the majority of metric deltas.
     * <p>
     * Examines each {@link MetricDelta} in the {@link #deltas} list and counts
     * improvements versus degradations. The result is:
     * <ul>
     *   <li>{@code "improved"} - more metrics improved than degraded</li>
     *   <li>{@code "degraded"} - more metrics degraded than improved</li>
     *   <li>{@code "mixed"} - equal number of improvements and degradations, or no deltas</li>
     * </ul>
     *
     * @return "improved", "degraded", or "mixed"
     */
    public String getOverallDirection() {
        if (deltas == null || deltas.isEmpty()) {
            return "mixed";
        }

        int improved = 0;
        int degraded = 0;

        for (MetricDelta delta : deltas) {
            String direction = delta.getDirection();
            if ("improvement".equals(direction)) {
                improved++;
            } else if ("degradation".equals(direction)) {
                degraded++;
            }
        }

        if (improved > degraded) {
            return "improved";
        } else if (degraded > improved) {
            return "degraded";
        } else {
            return "mixed";
        }
    }
}
