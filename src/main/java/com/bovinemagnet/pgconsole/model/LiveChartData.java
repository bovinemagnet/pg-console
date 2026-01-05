package com.bovinemagnet.pgconsole.model;

import java.time.Instant;
import java.util.List;
import java.util.ArrayList;

/**
 * Represents live chart data for real-time monitoring dashboards.
 * <p>
 * This model supports multiple data series for interactive charts
 * with configurable refresh intervals and pause/resume functionality.
 * It is designed for use with PostgreSQL Console's live monitoring
 * dashboards to visualise database metrics over time.
 * </p>
 * <p>
 * The class provides factory methods for common chart types including
 * database connections, transaction rates, tuple operations, and cache
 * hit ratios. Each chart can contain multiple {@link DataSeries}, and
 * each series contains a list of {@link DataPoint} instances representing
 * time-series data.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * LiveChartData chart = LiveChartData.createConnectionsChart();
 * DataSeries activeSeries = chart.getSeriesByName("Active");
 * activeSeries.addPoint(Instant.now(), 42.0);
 * chart.setLastUpdated(Instant.now());
 * chart.trimToMaxPoints();
 * }</pre>
 * </p>
 *
 * @author Paul Snow
 * @since 0.0.0
 * @see DataSeries
 * @see DataPoint
 */
public class LiveChartData {

    /**
     * Represents a single data point in a time series.
     * <p>
     * Each data point consists of a timestamp, a numeric value, and an
     * optional label for display purposes. Data points are typically
     * aggregated within a {@link DataSeries} to form a complete time series.
     * </p>
     *
     * @author Paul Snow
     * @since 0.0.0
     */
    public static class DataPoint {
        private Instant timestamp;
        private double value;
        private String label;

        /**
         * Constructs an empty data point.
         * <p>
         * All fields are initialised to their default values (null for objects,
         * 0.0 for primitives).
         * </p>
         */
        public DataPoint() {}

        /**
         * Constructs a data point with a timestamp and value.
         *
         * @param timestamp the timestamp when this data point was recorded
         * @param value the numeric value of this data point
         */
        public DataPoint(Instant timestamp, double value) {
            this.timestamp = timestamp;
            this.value = value;
        }

        /**
         * Constructs a data point with a timestamp, value, and label.
         *
         * @param timestamp the timestamp when this data point was recorded
         * @param value the numeric value of this data point
         * @param label an optional descriptive label for display purposes
         */
        public DataPoint(Instant timestamp, double value, String label) {
            this.timestamp = timestamp;
            this.value = value;
            this.label = label;
        }

        /**
         * Returns the timestamp when this data point was recorded.
         *
         * @return the timestamp, or null if not set
         */
        public Instant getTimestamp() { return timestamp; }

        /**
         * Sets the timestamp when this data point was recorded.
         *
         * @param timestamp the timestamp to set
         */
        public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

        /**
         * Returns the numeric value of this data point.
         *
         * @return the value
         */
        public double getValue() { return value; }

        /**
         * Sets the numeric value of this data point.
         *
         * @param value the value to set
         */
        public void setValue(double value) { this.value = value; }

        /**
         * Returns the optional descriptive label for this data point.
         *
         * @return the label, or null if not set
         */
        public String getLabel() { return label; }

        /**
         * Sets the optional descriptive label for this data point.
         *
         * @param label the label to set
         */
        public void setLabel(String label) { this.label = label; }

        /**
         * Returns the timestamp as milliseconds since the Unix epoch.
         * <p>
         * This is useful for JSON serialisation and JavaScript chart libraries
         * that expect timestamps in milliseconds.
         * </p>
         *
         * @return the timestamp in milliseconds, or 0 if timestamp is null
         */
        public long getTimestampMillis() {
            return timestamp != null ? timestamp.toEpochMilli() : 0;
        }
    }

    /**
     * Represents a data series (line) in the chart.
     * <p>
     * A data series contains a collection of {@link DataPoint} instances
     * that form a line on the chart. Each series has a name, colour, and
     * visibility state. The class provides utility methods for calculating
     * aggregates like minimum, maximum, and average values across all points.
     * </p>
     * <p>
     * Series are typically created with a name and colour, then populated
     * with data points over time as metrics are sampled. The visibility
     * flag allows series to be toggled on and off in the chart display.
     * </p>
     *
     * @author Paul Snow
     * @since 0.0.0
     */
    public static class DataSeries {
        private String name;
        private String colour;
        private List<DataPoint> points;
        private boolean visible;

        /**
         * Constructs an empty data series.
         * <p>
         * Initialises the points list as an empty {@link ArrayList} and
         * sets visibility to true by default.
         * </p>
         */
        public DataSeries() {
            this.points = new ArrayList<>();
            this.visible = true;
        }

        /**
         * Constructs a data series with a name and colour.
         *
         * @param name the series name (e.g., "Active Connections")
         * @param colour the series colour in CSS format (e.g., "#28a745" or "rgb(40, 167, 69)")
         */
        public DataSeries(String name, String colour) {
            this();
            this.name = name;
            this.colour = colour;
        }

        /**
         * Returns the name of this data series.
         *
         * @return the series name, or null if not set
         */
        public String getName() { return name; }

        /**
         * Sets the name of this data series.
         *
         * @param name the series name to set
         */
        public void setName(String name) { this.name = name; }

        /**
         * Returns the colour of this data series.
         *
         * @return the series colour in CSS format, or null if not set
         */
        public String getColour() { return colour; }

        /**
         * Sets the colour of this data series.
         *
         * @param colour the colour in CSS format (e.g., "#28a745" or "rgb(40, 167, 69)")
         */
        public void setColour(String colour) { this.colour = colour; }

        /**
         * Returns the list of data points in this series.
         *
         * @return the list of data points, never null
         */
        public List<DataPoint> getPoints() { return points; }

        /**
         * Sets the list of data points for this series.
         *
         * @param points the list of data points to set
         */
        public void setPoints(List<DataPoint> points) { this.points = points; }

        /**
         * Returns whether this series is currently visible in the chart.
         *
         * @return true if the series is visible, false otherwise
         */
        public boolean isVisible() { return visible; }

        /**
         * Sets whether this series is currently visible in the chart.
         *
         * @param visible true to show the series, false to hide it
         */
        public void setVisible(boolean visible) { this.visible = visible; }

        /**
         * Adds a data point to this series.
         * <p>
         * If the points list is null, it will be initialised as an empty
         * {@link ArrayList} before adding the point.
         * </p>
         *
         * @param point the data point to add
         */
        public void addPoint(DataPoint point) {
            if (points == null) points = new ArrayList<>();
            points.add(point);
        }

        /**
         * Adds a data point to this series with the specified timestamp and value.
         * <p>
         * This is a convenience method that creates a new {@link DataPoint}
         * instance and adds it to the series.
         * </p>
         *
         * @param timestamp the timestamp when the data was recorded
         * @param value the numeric value of the data point
         */
        public void addPoint(Instant timestamp, double value) {
            addPoint(new DataPoint(timestamp, value));
        }

        /**
         * Returns the most recent value in this series.
         * <p>
         * This retrieves the value of the last data point in the series,
         * which typically represents the current or latest measurement.
         * </p>
         *
         * @return the latest value, or 0.0 if the series is empty
         */
        public double getLatestValue() {
            if (points == null || points.isEmpty()) return 0.0;
            return points.get(points.size() - 1).getValue();
        }

        /**
         * Returns the minimum value across all data points in this series.
         *
         * @return the minimum value, or 0.0 if the series is empty
         */
        public double getMinValue() {
            if (points == null || points.isEmpty()) return 0.0;
            return points.stream().mapToDouble(DataPoint::getValue).min().orElse(0.0);
        }

        /**
         * Returns the maximum value across all data points in this series.
         *
         * @return the maximum value, or 0.0 if the series is empty
         */
        public double getMaxValue() {
            if (points == null || points.isEmpty()) return 0.0;
            return points.stream().mapToDouble(DataPoint::getValue).max().orElse(0.0);
        }

        /**
         * Returns the average value across all data points in this series.
         *
         * @return the average value, or 0.0 if the series is empty
         */
        public double getAverageValue() {
            if (points == null || points.isEmpty()) return 0.0;
            return points.stream().mapToDouble(DataPoint::getValue).average().orElse(0.0);
        }
    }

    private String chartId;
    private String title;
    private String yAxisLabel;
    private String xAxisLabel;
    private List<DataSeries> series;
    private Instant lastUpdated;
    private int refreshIntervalMs;
    private boolean paused;
    private int maxDataPoints;

    /**
     * Constructs an empty live chart data instance with default settings.
     * <p>
     * Initialises the series list as an empty {@link ArrayList}, sets the
     * refresh interval to 5000 milliseconds (5 seconds), the maximum data
     * points to 60 (representing 5 minutes of data at 5-second intervals),
     * and sets the paused state to false.
     * </p>
     */
    public LiveChartData() {
        this.series = new ArrayList<>();
        this.refreshIntervalMs = 5000; // Default 5 seconds
        this.maxDataPoints = 60; // Default 60 points (5 minutes at 5s interval)
        this.paused = false;
    }

    /**
     * Constructs a live chart data instance with the specified chart ID and title.
     * <p>
     * This constructor calls the default constructor to initialise default
     * settings, then sets the chart ID and title.
     * </p>
     *
     * @param chartId the unique identifier for this chart (e.g., "connections")
     * @param title the display title for this chart (e.g., "Database Connections")
     */
    public LiveChartData(String chartId, String title) {
        this();
        this.chartId = chartId;
        this.title = title;
    }

    /**
     * Returns the unique identifier for this chart.
     *
     * @return the chart ID, or null if not set
     */
    public String getChartId() { return chartId; }

    /**
     * Sets the unique identifier for this chart.
     *
     * @param chartId the chart ID to set
     */
    public void setChartId(String chartId) { this.chartId = chartId; }

    /**
     * Returns the display title for this chart.
     *
     * @return the title, or null if not set
     */
    public String getTitle() { return title; }

    /**
     * Sets the display title for this chart.
     *
     * @param title the title to set
     */
    public void setTitle(String title) { this.title = title; }

    /**
     * Returns the Y-axis label for this chart.
     *
     * @return the Y-axis label, or null if not set
     */
    public String getyAxisLabel() { return yAxisLabel; }

    /**
     * Sets the Y-axis label for this chart.
     *
     * @param yAxisLabel the Y-axis label to set (e.g., "Connections", "Transactions/sec")
     */
    public void setyAxisLabel(String yAxisLabel) { this.yAxisLabel = yAxisLabel; }

    /**
     * Returns the X-axis label for this chart.
     *
     * @return the X-axis label, or null if not set
     */
    public String getxAxisLabel() { return xAxisLabel; }

    /**
     * Sets the X-axis label for this chart.
     *
     * @param xAxisLabel the X-axis label to set (typically "Time")
     */
    public void setxAxisLabel(String xAxisLabel) { this.xAxisLabel = xAxisLabel; }

    /**
     * Returns the list of data series in this chart.
     *
     * @return the list of data series, never null
     */
    public List<DataSeries> getSeries() { return series; }

    /**
     * Sets the list of data series for this chart.
     *
     * @param series the list of data series to set
     */
    public void setSeries(List<DataSeries> series) { this.series = series; }

    /**
     * Returns the timestamp when this chart was last updated.
     *
     * @return the last updated timestamp, or null if not set
     */
    public Instant getLastUpdated() { return lastUpdated; }

    /**
     * Sets the timestamp when this chart was last updated.
     *
     * @param lastUpdated the timestamp to set
     */
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }

    /**
     * Returns the refresh interval in milliseconds.
     *
     * @return the refresh interval in milliseconds
     */
    public int getRefreshIntervalMs() { return refreshIntervalMs; }

    /**
     * Sets the refresh interval in milliseconds.
     * <p>
     * This determines how frequently the chart should poll for new data.
     * Common values include 5000 (5 seconds), 10000 (10 seconds), or 60000 (1 minute).
     * </p>
     *
     * @param refreshIntervalMs the refresh interval in milliseconds
     */
    public void setRefreshIntervalMs(int refreshIntervalMs) { this.refreshIntervalMs = refreshIntervalMs; }

    /**
     * Returns whether this chart is currently paused.
     *
     * @return true if the chart is paused, false if it is actively updating
     */
    public boolean isPaused() { return paused; }

    /**
     * Sets whether this chart is currently paused.
     * <p>
     * When paused, the chart should not poll for new data or update its display.
     * </p>
     *
     * @param paused true to pause the chart, false to resume updates
     */
    public void setPaused(boolean paused) { this.paused = paused; }

    /**
     * Returns the maximum number of data points to retain per series.
     *
     * @return the maximum number of data points
     */
    public int getMaxDataPoints() { return maxDataPoints; }

    /**
     * Sets the maximum number of data points to retain per series.
     * <p>
     * When the number of points exceeds this limit, older points should be
     * removed to maintain a rolling window of data. This prevents unbounded
     * memory growth for long-running charts.
     * </p>
     *
     * @param maxDataPoints the maximum number of data points to retain
     */
    public void setMaxDataPoints(int maxDataPoints) { this.maxDataPoints = maxDataPoints; }

    /**
     * Adds a data series to the chart.
     * <p>
     * If the series list is null, it will be initialised as an empty
     * {@link ArrayList} before adding the series.
     * </p>
     *
     * @param dataSeries the series to add
     */
    public void addSeries(DataSeries dataSeries) {
        if (series == null) series = new ArrayList<>();
        series.add(dataSeries);
    }

    /**
     * Retrieves a data series by its name.
     * <p>
     * Searches through all series in this chart and returns the first
     * series with a matching name. The comparison is case-sensitive.
     * </p>
     *
     * @param name the name of the series to find
     * @return the matching {@link DataSeries}, or null if not found or series list is null
     */
    public DataSeries getSeriesByName(String name) {
        if (series == null) return null;
        return series.stream()
                .filter(s -> name.equals(s.getName()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns the last updated time as a formatted string for display.
     * <p>
     * This provides a human-readable representation of when the chart
     * was last updated. If the chart has never been updated, returns "Never".
     * </p>
     *
     * @return the formatted timestamp string, or "Never" if lastUpdated is null
     */
    public String getLastUpdatedDisplay() {
        if (lastUpdated == null) return "Never";
        return lastUpdated.toString();
    }

    /**
     * Returns the refresh interval in seconds.
     * <p>
     * This is a convenience method that converts the millisecond-based
     * refresh interval to seconds for easier display and configuration.
     * </p>
     *
     * @return the refresh interval in seconds
     */
    public int getRefreshIntervalSeconds() {
        return refreshIntervalMs / 1000;
    }

    /**
     * Trims all data series to the maximum number of data points.
     * <p>
     * This method removes the oldest data points from each series when
     * the number of points exceeds {@link #maxDataPoints}. This creates
     * a rolling window effect, maintaining a fixed time range of historical
     * data whilst preventing unbounded memory growth.
     * </p>
     * <p>
     * The trimming is performed by removing excess points from the beginning
     * of each series' point list, preserving the most recent data.
     * </p>
     */
    public void trimToMaxPoints() {
        if (series == null) return;
        for (DataSeries s : series) {
            if (s.getPoints() != null && s.getPoints().size() > maxDataPoints) {
                int excess = s.getPoints().size() - maxDataPoints;
                s.setPoints(new ArrayList<>(s.getPoints().subList(excess, s.getPoints().size())));
            }
        }
    }

    /**
     * Creates a pre-configured chart for monitoring database connections.
     * <p>
     * This factory method creates a chart with three data series:
     * <ul>
     * <li>Active connections (green, #28a745)</li>
     * <li>Idle connections (grey, #6c757d)</li>
     * <li>Idle in transaction connections (warning yellow, #ffc107)</li>
     * </ul>
     * The chart is configured with appropriate axis labels and is ready
     * to receive connection count data points.
     * </p>
     *
     * @return a {@link LiveChartData} instance configured for database connection monitoring
     */
    public static LiveChartData createConnectionsChart() {
        LiveChartData chart = new LiveChartData("connections", "Database Connections");
        chart.setyAxisLabel("Connections");
        chart.setxAxisLabel("Time");
        chart.addSeries(new DataSeries("Active", "#28a745"));
        chart.addSeries(new DataSeries("Idle", "#6c757d"));
        chart.addSeries(new DataSeries("Idle in Transaction", "#ffc107"));
        return chart;
    }

    /**
     * Creates a pre-configured chart for monitoring transaction rates.
     * <p>
     * This factory method creates a chart with two data series:
     * <ul>
     * <li>Committed transactions (green, #28a745)</li>
     * <li>Rolled back transactions (danger red, #dc3545)</li>
     * </ul>
     * The Y-axis is labelled as "Transactions/sec" to indicate the rate
     * of transactions rather than cumulative counts.
     * </p>
     *
     * @return a {@link LiveChartData} instance configured for transaction rate monitoring
     */
    public static LiveChartData createTransactionsChart() {
        LiveChartData chart = new LiveChartData("transactions", "Transaction Rate");
        chart.setyAxisLabel("Transactions/sec");
        chart.setxAxisLabel("Time");
        chart.addSeries(new DataSeries("Commits", "#28a745"));
        chart.addSeries(new DataSeries("Rollbacks", "#dc3545"));
        return chart;
    }

    /**
     * Creates a pre-configured chart for monitoring tuple operations.
     * <p>
     * This factory method creates a chart with three data series representing
     * the rate of tuple (row) operations:
     * <ul>
     * <li>Inserted tuples (green, #28a745)</li>
     * <li>Updated tuples (blue, #007bff)</li>
     * <li>Deleted tuples (danger red, #dc3545)</li>
     * </ul>
     * The Y-axis is labelled as "Tuples/sec" to indicate the rate of
     * operations rather than cumulative counts.
     * </p>
     *
     * @return a {@link LiveChartData} instance configured for tuple operation monitoring
     */
    public static LiveChartData createTuplesChart() {
        LiveChartData chart = new LiveChartData("tuples", "Tuple Operations");
        chart.setyAxisLabel("Tuples/sec");
        chart.setxAxisLabel("Time");
        chart.addSeries(new DataSeries("Inserted", "#28a745"));
        chart.addSeries(new DataSeries("Updated", "#007bff"));
        chart.addSeries(new DataSeries("Deleted", "#dc3545"));
        return chart;
    }

    /**
     * Creates a pre-configured chart for monitoring cache hit ratios.
     * <p>
     * This factory method creates a chart with two data series representing
     * cache effectiveness:
     * <ul>
     * <li>Buffer cache hit ratio (green, #28a745)</li>
     * <li>Index cache hit ratio (blue, #007bff)</li>
     * </ul>
     * The Y-axis is labelled as "Hit Ratio %" to indicate percentages
     * (0-100). Higher values indicate better cache performance and fewer
     * disk reads.
     * </p>
     *
     * @return a {@link LiveChartData} instance configured for cache hit ratio monitoring
     */
    public static LiveChartData createCacheChart() {
        LiveChartData chart = new LiveChartData("cache", "Cache Hit Ratio");
        chart.setyAxisLabel("Hit Ratio %");
        chart.setxAxisLabel("Time");
        chart.addSeries(new DataSeries("Buffer Cache", "#28a745"));
        chart.addSeries(new DataSeries("Index Cache", "#007bff"));
        return chart;
    }
}
