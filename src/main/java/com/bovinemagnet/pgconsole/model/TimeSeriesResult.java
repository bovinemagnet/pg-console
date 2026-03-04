package com.bovinemagnet.pgconsole.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unified DTO for time-series data returned by the metrics history bridge.
 * <p>
 * Contains timestamps, named data series, data point count, and metadata
 * about the data source and resolution. Used by the metrics history API
 * to return data from either in-memory or persisted storage transparently.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class TimeSeriesResult {

    /** Epoch millisecond timestamps for each data point. */
    private List<Long> timestamps;

    /** Named data series mapping series name to values. */
    private Map<String, List<Double>> series;

    /** Number of data points in this result. */
    private int dataPoints;

    /** Source of the data: "in-memory" or "persisted". */
    private String dataSource;

    /** Approximate resolution in seconds between data points. */
    private int resolutionSeconds;

    /**
     * Constructs an empty time-series result.
     */
    public TimeSeriesResult() {
        this.timestamps = new ArrayList<>();
        this.series = new HashMap<>();
        this.dataPoints = 0;
        this.dataSource = "in-memory";
        this.resolutionSeconds = 5;
    }

    /**
     * Gets the timestamps.
     *
     * @return list of epoch millisecond timestamps
     */
    public List<Long> getTimestamps() {
        return timestamps;
    }

    /**
     * Sets the timestamps.
     *
     * @param timestamps list of epoch millisecond timestamps
     */
    public void setTimestamps(List<Long> timestamps) {
        this.timestamps = timestamps;
    }

    /**
     * Gets the named data series.
     *
     * @return map of series name to value lists
     */
    public Map<String, List<Double>> getSeries() {
        return series;
    }

    /**
     * Sets the named data series.
     *
     * @param series map of series name to value lists
     */
    public void setSeries(Map<String, List<Double>> series) {
        this.series = series;
    }

    /**
     * Adds a named series to the result.
     *
     * @param name   the series name
     * @param values the series values
     */
    public void addSeries(String name, List<Double> values) {
        this.series.put(name, values);
    }

    /**
     * Gets the number of data points.
     *
     * @return data point count
     */
    public int getDataPoints() {
        return dataPoints;
    }

    /**
     * Sets the number of data points.
     *
     * @param dataPoints data point count
     */
    public void setDataPoints(int dataPoints) {
        this.dataPoints = dataPoints;
    }

    /**
     * Gets the data source identifier.
     *
     * @return "in-memory" or "persisted"
     */
    public String getDataSource() {
        return dataSource;
    }

    /**
     * Sets the data source identifier.
     *
     * @param dataSource "in-memory" or "persisted"
     */
    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Gets the approximate resolution in seconds.
     *
     * @return seconds between data points
     */
    public int getResolutionSeconds() {
        return resolutionSeconds;
    }

    /**
     * Sets the approximate resolution in seconds.
     *
     * @param resolutionSeconds seconds between data points
     */
    public void setResolutionSeconds(int resolutionSeconds) {
        this.resolutionSeconds = resolutionSeconds;
    }
}
