package com.bovinemagnet.pgconsole.model;

/**
 * Represents a before/after delta for a single metric, used for rendering
 * comparison visualisations in stopwatch and comparison window templates.
 * <p>
 * Each instance captures the metric name, start and end values, unit of measurement,
 * and whether a higher value represents an improvement or degradation. Computed
 * properties provide the absolute delta, percentage change, CSS styling class,
 * and directional label for template rendering.
 * <p>
 * <strong>Usage example:</strong>
 * <pre>{@code
 * MetricDelta delta = new MetricDelta("Cache Hit Ratio", 95.2, 98.7, "%", true);
 * // delta.getDelta()       => 3.5
 * // delta.getDeltaPercent() => 3.68
 * // delta.getCssClass()     => "text-success"
 * // delta.getDirection()    => "improvement"
 * }</pre>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see com.bovinemagnet.pgconsole.model.StopwatchSession
 */
public class MetricDelta {

    /** Human-readable name of the metric (e.g., "Cache Hit Ratio", "Active Queries"). */
    private String name;

    /** The metric value at the start of the measurement period. */
    private double startValue;

    /** The metric value at the end of the measurement period. */
    private double endValue;

    /** The unit of measurement (e.g., "%", "ms", "connections", "bytes"). */
    private String unit;

    /**
     * Whether a higher value represents an improvement for this metric.
     * <p>
     * For example, cache hit ratio is better when higher (true), whilst
     * blocked queries are better when lower (false).
     */
    private boolean higherIsBetter;

    /**
     * Constructs a MetricDelta with all required fields.
     *
     * @param name           the human-readable metric name
     * @param startValue     the value at the start of measurement
     * @param endValue       the value at the end of measurement
     * @param unit           the unit of measurement
     * @param higherIsBetter true if a higher value is an improvement
     */
    public MetricDelta(String name, double startValue, double endValue, String unit, boolean higherIsBetter) {
        this.name = name;
        this.startValue = startValue;
        this.endValue = endValue;
        this.unit = unit;
        this.higherIsBetter = higherIsBetter;
    }

    /**
     * Default constructor for framework use.
     */
    public MetricDelta() {
    }

    /**
     * Returns the human-readable name of the metric.
     *
     * @return the metric name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the human-readable name of the metric.
     *
     * @param name the metric name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the metric value at the start of the measurement period.
     *
     * @return the start value
     */
    public double getStartValue() {
        return startValue;
    }

    /**
     * Sets the metric value at the start of the measurement period.
     *
     * @param startValue the start value
     */
    public void setStartValue(double startValue) {
        this.startValue = startValue;
    }

    /**
     * Returns the metric value at the end of the measurement period.
     *
     * @return the end value
     */
    public double getEndValue() {
        return endValue;
    }

    /**
     * Sets the metric value at the end of the measurement period.
     *
     * @param endValue the end value
     */
    public void setEndValue(double endValue) {
        this.endValue = endValue;
    }

    /**
     * Returns the unit of measurement for this metric.
     *
     * @return the unit (e.g., "%", "ms", "connections")
     */
    public String getUnit() {
        return unit;
    }

    /**
     * Sets the unit of measurement for this metric.
     *
     * @param unit the unit of measurement
     */
    public void setUnit(String unit) {
        this.unit = unit;
    }

    /**
     * Returns whether a higher value represents an improvement for this metric.
     *
     * @return true if higher is better
     */
    public boolean isHigherIsBetter() {
        return higherIsBetter;
    }

    /**
     * Sets whether a higher value represents an improvement for this metric.
     *
     * @param higherIsBetter true if higher is better
     */
    public void setHigherIsBetter(boolean higherIsBetter) {
        this.higherIsBetter = higherIsBetter;
    }

    // ========================================
    // Computed Methods
    // ========================================

    /**
     * Calculates the absolute delta between end and start values.
     *
     * @return the difference (endValue - startValue)
     */
    public double getDelta() {
        return endValue - startValue;
    }

    /**
     * Calculates the percentage change from start to end value.
     * <p>
     * If the start value is zero, returns 0 to avoid division by zero.
     * A positive result indicates an increase; a negative result indicates a decrease.
     *
     * @return the percentage change, or 0 if start value is zero
     */
    public double getDeltaPercent() {
        if (startValue == 0) {
            return 0;
        }
        return ((endValue - startValue) / Math.abs(startValue)) * 100.0;
    }

    /**
     * Returns the Bootstrap CSS class for colour-coding the delta direction.
     * <p>
     * The class is determined by whether the change represents an improvement
     * or degradation, based on the {@link #higherIsBetter} flag:
     * <ul>
     *   <li>{@code "text-success"} - the metric improved</li>
     *   <li>{@code "text-danger"} - the metric degraded</li>
     *   <li>{@code ""} (empty) - no change (neutral)</li>
     * </ul>
     *
     * @return the CSS class name for template rendering
     */
    public String getCssClass() {
        double delta = getDelta();
        if (delta == 0) {
            return "";
        }
        boolean improved = higherIsBetter ? (delta > 0) : (delta < 0);
        return improved ? "text-success" : "text-danger";
    }

    /**
     * Returns a human-readable directional label for the metric change.
     * <p>
     * The direction considers the {@link #higherIsBetter} flag to determine
     * whether the observed change is positive or negative:
     * <ul>
     *   <li>{@code "improvement"} - the metric moved in the desirable direction</li>
     *   <li>{@code "degradation"} - the metric moved in the undesirable direction</li>
     *   <li>{@code "neutral"} - no change observed</li>
     * </ul>
     *
     * @return "improvement", "degradation", or "neutral"
     */
    public String getDirection() {
        double delta = getDelta();
        if (delta == 0) {
            return "neutral";
        }
        boolean improved = higherIsBetter ? (delta > 0) : (delta < 0);
        return improved ? "improvement" : "degradation";
    }
}
