package com.bovinemagnet.pgconsole.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a group of similar queries identified by their fingerprint.
 * <p>
 * A query fingerprint is a normalised representation of a SQL query where
 * literal values are replaced with placeholders. Queries with the same
 * fingerprint have identical structure but may differ in their literal
 * values (e.g., WHERE id = 1 and WHERE id = 2 share the same fingerprint).
 * <p>
 * This class aggregates statistics across all query instances that share
 * the same fingerprint, enabling analysis of query patterns rather than
 * individual query executions. This is particularly useful for identifying
 * problematic query structures in systems where queries are dynamically
 * generated with varying parameter values.
 * <p>
 * The aggregated statistics include total execution time, call counts,
 * and row counts across all instances, as well as the average mean
 * execution time per instance.
 *
 * @author Paul Snow
 * @version 0.0.0
 * @since 0.0.0
 * @see SlowQuery
 */
public class QueryFingerprint {

    /**
     * The unique fingerprint identifier for this query pattern.
     * Typically a hash or normalised representation of the query structure.
     */
    private String fingerprint;

    /**
     * The normalised query text with literal values replaced by placeholders.
     * This represents the canonical form of all queries in this fingerprint group.
     */
    private String normalisedQuery;

    /**
     * The list of individual query instances that match this fingerprint.
     * Each instance represents a specific execution or set of executions
     * from {@code pg_stat_statements}.
     */
    private List<SlowQuery> instances = new ArrayList<>();

    /**
     * The total number of calls across all query instances in this fingerprint group.
     * Sum of all {@link SlowQuery#getTotalCalls()} values.
     */
    private long totalCalls;

    /**
     * The total execution time in milliseconds across all query instances.
     * Sum of all {@link SlowQuery#getTotalTime()} values.
     */
    private double totalTime;

    /**
     * The average of mean execution times across all query instances in milliseconds.
     * This is the mean of {@link SlowQuery#getMeanTime()} values, not the overall mean.
     */
    private double avgMeanTime;

    /**
     * The total number of rows returned across all query instances.
     * Sum of all {@link SlowQuery#getRows()} values.
     */
    private long totalRows;

    /**
     * Constructs an empty QueryFingerprint with no instances.
     * All statistics are initialised to zero or empty collections.
     */
    public QueryFingerprint() {
    }

    /**
     * Constructs a QueryFingerprint with the specified fingerprint and normalised query.
     *
     * @param fingerprint the unique fingerprint identifier for this query pattern
     * @param normalisedQuery the normalised query text with placeholders
     */
    public QueryFingerprint(String fingerprint, String normalisedQuery) {
        this.fingerprint = fingerprint;
        this.normalisedQuery = normalisedQuery;
    }

    /**
     * Returns the unique fingerprint identifier for this query pattern.
     *
     * @return the fingerprint identifier, or null if not set
     */
    public String getFingerprint() {
        return fingerprint;
    }

    /**
     * Sets the unique fingerprint identifier for this query pattern.
     *
     * @param fingerprint the fingerprint identifier to set
     */
    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    /**
     * Returns the normalised query text with literal values replaced by placeholders.
     *
     * @return the normalised query text, or null if not set
     */
    public String getNormalisedQuery() {
        return normalisedQuery;
    }

    /**
     * Sets the normalised query text.
     *
     * @param normalisedQuery the normalised query text to set
     */
    public void setNormalisedQuery(String normalisedQuery) {
        this.normalisedQuery = normalisedQuery;
    }

    /**
     * Returns the list of query instances that match this fingerprint.
     * <p>
     * The returned list is mutable and can be modified directly, though
     * using {@link #addInstance(SlowQuery)} is preferred for adding instances.
     *
     * @return the list of query instances, never null
     */
    public List<SlowQuery> getInstances() {
        return instances;
    }

    /**
     * Sets the list of query instances for this fingerprint.
     * <p>
     * Note: This replaces the entire instances list. Consider using
     * {@link #addInstance(SlowQuery)} to add individual instances.
     *
     * @param instances the list of query instances to set
     */
    public void setInstances(List<SlowQuery> instances) {
        this.instances = instances;
    }

    /**
     * Adds a query instance to this fingerprint group.
     * <p>
     * Note: This method does not automatically recalculate aggregated statistics.
     * Call {@link #recalculateStats()} after adding instances to update
     * the total calls, total time, and other aggregated metrics.
     *
     * @param query the query instance to add
     * @see #recalculateStats()
     */
    public void addInstance(SlowQuery query) {
        this.instances.add(query);
    }

    /**
     * Returns the total number of calls across all query instances.
     *
     * @return the total number of calls, zero if no instances or not calculated
     */
    public long getTotalCalls() {
        return totalCalls;
    }

    /**
     * Sets the total number of calls across all query instances.
     * <p>
     * Typically set by {@link #recalculateStats()} rather than manually.
     *
     * @param totalCalls the total number of calls to set
     */
    public void setTotalCalls(long totalCalls) {
        this.totalCalls = totalCalls;
    }

    /**
     * Returns the total execution time in milliseconds across all query instances.
     *
     * @return the total execution time in milliseconds, zero if no instances or not calculated
     */
    public double getTotalTime() {
        return totalTime;
    }

    /**
     * Sets the total execution time in milliseconds.
     * <p>
     * Typically set by {@link #recalculateStats()} rather than manually.
     *
     * @param totalTime the total execution time in milliseconds to set
     */
    public void setTotalTime(double totalTime) {
        this.totalTime = totalTime;
    }

    /**
     * Returns the average of mean execution times across all query instances in milliseconds.
     * <p>
     * This is calculated as the arithmetic mean of each instance's mean execution time,
     * not as the overall mean of all individual query executions.
     *
     * @return the average mean execution time in milliseconds, zero if no instances or not calculated
     */
    public double getAvgMeanTime() {
        return avgMeanTime;
    }

    /**
     * Sets the average of mean execution times.
     * <p>
     * Typically set by {@link #recalculateStats()} rather than manually.
     *
     * @param avgMeanTime the average mean execution time in milliseconds to set
     */
    public void setAvgMeanTime(double avgMeanTime) {
        this.avgMeanTime = avgMeanTime;
    }

    /**
     * Returns the total number of rows returned across all query instances.
     *
     * @return the total number of rows, zero if no instances or not calculated
     */
    public long getTotalRows() {
        return totalRows;
    }

    /**
     * Sets the total number of rows returned across all query instances.
     * <p>
     * Typically set by {@link #recalculateStats()} rather than manually.
     *
     * @param totalRows the total number of rows to set
     */
    public void setTotalRows(long totalRows) {
        this.totalRows = totalRows;
    }

    /**
     * Returns the number of query instances in this fingerprint group.
     * <p>
     * This is a convenience method equivalent to {@code getInstances().size()}.
     *
     * @return the number of query instances, zero if no instances have been added
     */
    public int getInstanceCount() {
        return instances.size();
    }

    /**
     * Returns a shortened version of the normalised query suitable for display in tables or lists.
     * <p>
     * The query text is cleaned by:
     * <ul>
     *     <li>Collapsing all whitespace sequences to single spaces</li>
     *     <li>Trimming leading and trailing whitespace</li>
     *     <li>Truncating to 100 characters with ellipsis if longer</li>
     * </ul>
     * <p>
     * This method is designed for use in Qute templates where space is limited.
     *
     * @return the shortened normalised query, or an empty string if normalised query is null
     */
    public String getShortNormalisedQuery() {
        if (normalisedQuery == null) {
            return "";
        }
        String cleaned = normalisedQuery.replaceAll("\\s+", " ").trim();
        if (cleaned.length() <= 100) {
            return cleaned;
        }
        return cleaned.substring(0, 97) + "...";
    }

    /**
     * Formats the total execution time as a human-readable string with appropriate units.
     * <p>
     * The formatting automatically selects the most appropriate unit:
     * <ul>
     *     <li>Milliseconds (ms) for times under 1 second</li>
     *     <li>Seconds (s) for times under 1 minute</li>
     *     <li>Minutes (min) for times under 1 hour</li>
     *     <li>Hours (h) for times 1 hour or greater</li>
     * </ul>
     * <p>
     * This method is designed for use in Qute templates for consistent time display.
     *
     * @return the formatted total time string (e.g., "123.45 ms", "2.5 min", "1.2 h")
     */
    public String getTotalTimeFormatted() {
        if (totalTime < 1000) {
            return String.format("%.2f ms", totalTime);
        } else if (totalTime < 60000) {
            return String.format("%.2f s", totalTime / 1000);
        } else if (totalTime < 3600000) {
            return String.format("%.1f min", totalTime / 60000);
        } else {
            return String.format("%.1f h", totalTime / 3600000);
        }
    }

    /**
     * Formats the average mean execution time as a human-readable string with appropriate units.
     * <p>
     * The formatting automatically selects the most appropriate unit:
     * <ul>
     *     <li>Milliseconds (ms) for times under 1 second</li>
     *     <li>Seconds (s) for times under 1 minute</li>
     *     <li>Minutes (min) for times 1 minute or greater</li>
     * </ul>
     * <p>
     * Note: Hours are not used for average times as individual query mean times
     * are typically much shorter than total accumulated times.
     * <p>
     * This method is designed for use in Qute templates for consistent time display.
     *
     * @return the formatted average mean time string (e.g., "45.67 ms", "3.2 s")
     */
    public String getAvgMeanTimeFormatted() {
        if (avgMeanTime < 1000) {
            return String.format("%.2f ms", avgMeanTime);
        } else if (avgMeanTime < 60000) {
            return String.format("%.2f s", avgMeanTime / 1000);
        } else {
            return String.format("%.1f min", avgMeanTime / 60000);
        }
    }

    /**
     * Recalculates all aggregated statistics from the current list of query instances.
     * <p>
     * This method iterates through all instances and computes:
     * <ul>
     *     <li>{@link #totalCalls} - sum of all instance call counts</li>
     *     <li>{@link #totalTime} - sum of all instance total times</li>
     *     <li>{@link #totalRows} - sum of all instance row counts</li>
     *     <li>{@link #avgMeanTime} - arithmetic mean of all instance mean times</li>
     * </ul>
     * <p>
     * This method should be called after adding instances via {@link #addInstance(SlowQuery)}
     * or {@link #setInstances(List)} to ensure the aggregated statistics are accurate.
     * If the instances list is empty, all statistics are reset to zero.
     * <p>
     * <strong>Thread Safety:</strong> This method is not thread-safe. External synchronisation
     * is required if instances may be modified concurrently.
     *
     * @see #addInstance(SlowQuery)
     */
    public void recalculateStats() {
        this.totalCalls = 0;
        this.totalTime = 0;
        this.totalRows = 0;
        double sumMeanTime = 0;

        for (SlowQuery q : instances) {
            this.totalCalls += q.getTotalCalls();
            this.totalTime += q.getTotalTime();
            this.totalRows += q.getRows();
            sumMeanTime += q.getMeanTime();
        }

        if (!instances.isEmpty()) {
            this.avgMeanTime = sumMeanTime / instances.size();
        }
    }
}
