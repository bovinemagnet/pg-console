package com.bovinemagnet.pgconsole.model;

import java.time.Instant;
import java.util.List;
import java.util.ArrayList;

/**
 * Represents live chart data for real-time monitoring dashboards.
 * <p>
 * This model supports multiple data series for interactive charts
 * with configurable refresh intervals and pause/resume functionality.
 * </p>
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class LiveChartData {

    /**
     * Represents a single data point in a time series.
     */
    public static class DataPoint {
        private Instant timestamp;
        private double value;
        private String label;

        public DataPoint() {}

        public DataPoint(Instant timestamp, double value) {
            this.timestamp = timestamp;
            this.value = value;
        }

        public DataPoint(Instant timestamp, double value, String label) {
            this.timestamp = timestamp;
            this.value = value;
            this.label = label;
        }

        public Instant getTimestamp() { return timestamp; }
        public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

        public double getValue() { return value; }
        public void setValue(double value) { this.value = value; }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }

        public long getTimestampMillis() {
            return timestamp != null ? timestamp.toEpochMilli() : 0;
        }
    }

    /**
     * Represents a data series (line) in the chart.
     */
    public static class DataSeries {
        private String name;
        private String colour;
        private List<DataPoint> points;
        private boolean visible;

        public DataSeries() {
            this.points = new ArrayList<>();
            this.visible = true;
        }

        public DataSeries(String name, String colour) {
            this();
            this.name = name;
            this.colour = colour;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getColour() { return colour; }
        public void setColour(String colour) { this.colour = colour; }

        public List<DataPoint> getPoints() { return points; }
        public void setPoints(List<DataPoint> points) { this.points = points; }

        public boolean isVisible() { return visible; }
        public void setVisible(boolean visible) { this.visible = visible; }

        public void addPoint(DataPoint point) {
            if (points == null) points = new ArrayList<>();
            points.add(point);
        }

        public void addPoint(Instant timestamp, double value) {
            addPoint(new DataPoint(timestamp, value));
        }

        public double getLatestValue() {
            if (points == null || points.isEmpty()) return 0.0;
            return points.get(points.size() - 1).getValue();
        }

        public double getMinValue() {
            if (points == null || points.isEmpty()) return 0.0;
            return points.stream().mapToDouble(DataPoint::getValue).min().orElse(0.0);
        }

        public double getMaxValue() {
            if (points == null || points.isEmpty()) return 0.0;
            return points.stream().mapToDouble(DataPoint::getValue).max().orElse(0.0);
        }

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

    public LiveChartData() {
        this.series = new ArrayList<>();
        this.refreshIntervalMs = 5000; // Default 5 seconds
        this.maxDataPoints = 60; // Default 60 points (5 minutes at 5s interval)
        this.paused = false;
    }

    public LiveChartData(String chartId, String title) {
        this();
        this.chartId = chartId;
        this.title = title;
    }

    public String getChartId() { return chartId; }
    public void setChartId(String chartId) { this.chartId = chartId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getyAxisLabel() { return yAxisLabel; }
    public void setyAxisLabel(String yAxisLabel) { this.yAxisLabel = yAxisLabel; }

    public String getxAxisLabel() { return xAxisLabel; }
    public void setxAxisLabel(String xAxisLabel) { this.xAxisLabel = xAxisLabel; }

    public List<DataSeries> getSeries() { return series; }
    public void setSeries(List<DataSeries> series) { this.series = series; }

    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }

    public int getRefreshIntervalMs() { return refreshIntervalMs; }
    public void setRefreshIntervalMs(int refreshIntervalMs) { this.refreshIntervalMs = refreshIntervalMs; }

    public boolean isPaused() { return paused; }
    public void setPaused(boolean paused) { this.paused = paused; }

    public int getMaxDataPoints() { return maxDataPoints; }
    public void setMaxDataPoints(int maxDataPoints) { this.maxDataPoints = maxDataPoints; }

    /**
     * Adds a data series to the chart.
     *
     * @param dataSeries the series to add
     */
    public void addSeries(DataSeries dataSeries) {
        if (series == null) series = new ArrayList<>();
        series.add(dataSeries);
    }

    /**
     * Gets a series by name.
     *
     * @param name the series name
     * @return the series or null if not found
     */
    public DataSeries getSeriesByName(String name) {
        if (series == null) return null;
        return series.stream()
                .filter(s -> name.equals(s.getName()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns the last updated time as a formatted string.
     *
     * @return formatted timestamp
     */
    public String getLastUpdatedDisplay() {
        if (lastUpdated == null) return "Never";
        return lastUpdated.toString();
    }

    /**
     * Returns the refresh interval in seconds.
     *
     * @return interval in seconds
     */
    public int getRefreshIntervalSeconds() {
        return refreshIntervalMs / 1000;
    }

    /**
     * Trims all series to the maximum data points.
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
     * Creates connection chart data structure.
     *
     * @return LiveChartData configured for connections
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
     * Creates commit/rollback chart data structure.
     *
     * @return LiveChartData configured for transactions
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
     * Creates tuple operations chart data structure.
     *
     * @return LiveChartData configured for tuples
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
     * Creates cache hit ratio chart data structure.
     *
     * @return LiveChartData configured for cache
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
