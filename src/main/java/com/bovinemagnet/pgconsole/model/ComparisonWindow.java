package com.bovinemagnet.pgconsole.model;

import java.time.Duration;
import java.time.Instant;

/**
 * Represents one side of a comparison window, encapsulating the time range,
 * aggregated metrics, and sample metadata for a single measurement period.
 * <p>
 * A comparison window captures the averaged metrics over a defined time span,
 * such as "Yesterday 09:00-17:00" or "Today 09:00-17:00". Two windows are
 * paired in a {@link WindowComparison} to produce side-by-side analysis.
 * <p>
 * The {@link #hasData()} method indicates whether sufficient history samples
 * were available for the window, and {@link #getDurationDisplay()} provides
 * a human-readable duration string for template rendering.
 * <p>
 * <strong>Usage example:</strong>
 * <pre>{@code
 * ComparisonWindow window = new ComparisonWindow();
 * window.setLabel("Yesterday");
 * window.setWindowStart(Instant.parse("2026-03-03T09:00:00Z"));
 * window.setWindowEnd(Instant.parse("2026-03-03T17:00:00Z"));
 * window.setAggregatedMetrics(aggregated);
 * window.setSampleCount(480);
 * // window.getDurationDisplay() => "8h 0m"
 * // window.hasData()            => true
 * }</pre>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see WindowComparison
 * @see AggregatedMetrics
 */
public class ComparisonWindow {

    /** Display name for this window (e.g., "Yesterday", "Today", "Last Week"). */
    private String label;

    /** Start of the comparison window (inclusive). */
    private Instant windowStart;

    /** End of the comparison window (inclusive). */
    private Instant windowEnd;

    /** Aggregated metrics computed over this window's time range. */
    private AggregatedMetrics aggregatedMetrics;

    /** Number of history samples available within this window. */
    private int sampleCount;

    /**
     * Constructs an empty comparison window instance.
     */
    public ComparisonWindow() {
    }

    // ========================================
    // Getters and Setters
    // ========================================

    /**
     * Returns the display label for this window.
     *
     * @return the label (e.g., "Yesterday", "Today")
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the display label for this window.
     *
     * @param label the display label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Returns the start of the comparison window.
     *
     * @return the window start instant (inclusive)
     */
    public Instant getWindowStart() {
        return windowStart;
    }

    /**
     * Sets the start of the comparison window.
     *
     * @param windowStart the window start instant (inclusive)
     */
    public void setWindowStart(Instant windowStart) {
        this.windowStart = windowStart;
    }

    /**
     * Returns the end of the comparison window.
     *
     * @return the window end instant (inclusive)
     */
    public Instant getWindowEnd() {
        return windowEnd;
    }

    /**
     * Sets the end of the comparison window.
     *
     * @param windowEnd the window end instant (inclusive)
     */
    public void setWindowEnd(Instant windowEnd) {
        this.windowEnd = windowEnd;
    }

    /**
     * Returns the aggregated metrics computed for this window.
     *
     * @return the aggregated metrics, or null if not yet computed
     */
    public AggregatedMetrics getAggregatedMetrics() {
        return aggregatedMetrics;
    }

    /**
     * Sets the aggregated metrics for this window.
     *
     * @param aggregatedMetrics the aggregated metrics
     */
    public void setAggregatedMetrics(AggregatedMetrics aggregatedMetrics) {
        this.aggregatedMetrics = aggregatedMetrics;
    }

    /**
     * Returns the number of history samples available within this window.
     *
     * @return the sample count
     */
    public int getSampleCount() {
        return sampleCount;
    }

    /**
     * Sets the number of history samples available within this window.
     *
     * @param sampleCount the sample count
     */
    public void setSampleCount(int sampleCount) {
        this.sampleCount = sampleCount;
    }

    // ========================================
    // Computed Methods
    // ========================================

    /**
     * Returns a human-readable display of the window duration.
     * <p>
     * Formats the duration between {@link #windowStart} and {@link #windowEnd}
     * as "{@code Xh Ym}", for example "8h 0m" or "1h 30m".
     * Returns "0h 0m" if either boundary is null.
     *
     * @return the formatted duration string
     */
    public String getDurationDisplay() {
        if (windowStart == null || windowEnd == null) {
            return "0h 0m";
        }
        Duration duration = Duration.between(windowStart, windowEnd);
        long totalMinutes = duration.toMinutes();
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return hours + "h " + minutes + "m";
    }

    /**
     * Returns whether this window contains sufficient data for comparison.
     * <p>
     * A window is considered to have data if at least one history sample
     * was available within the time range.
     *
     * @return true if {@link #sampleCount} is greater than zero
     */
    public boolean hasData() {
        return sampleCount > 0;
    }
}
