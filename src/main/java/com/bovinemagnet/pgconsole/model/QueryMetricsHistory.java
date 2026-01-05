package com.bovinemagnet.pgconsole.model;

import java.time.Instant;

/**
 * Represents a point-in-time snapshot of query performance metrics from PostgreSQL's
 * {@code pg_stat_statements} extension.
 * <p>
 * This model captures both cumulative metrics (total calls, total time, total rows) and
 * statistical measures (mean, min, max, standard deviation) for individual SQL queries.
 * Snapshots are sampled at regular intervals by {@link com.bovinemagnet.pgconsole.service.MetricsSamplerService}
 * and persisted to the {@code pgconsole.query_metrics_history} table for trend analysis,
 * regression detection, and performance monitoring over time.
 * <p>
 * The cumulative counters allow calculation of deltas between consecutive samples to
 * determine query execution rates and resource consumption patterns. Block I/O metrics
 * track buffer cache efficiency and disk access patterns for each query.
 * <p>
 * <b>Usage Example:</b>
 * <pre>{@code
 * QueryMetricsHistory snapshot = new QueryMetricsHistory();
 * snapshot.setSampledAt(Instant.now());
 * snapshot.setQueryId("1234567890abcdef");
 * snapshot.setQueryText("SELECT * FROM users WHERE active = $1");
 * snapshot.setTotalCalls(1500);
 * snapshot.setTotalTimeMs(3250.5);
 * snapshot.setMeanTimeMs(2.17);
 * historyRepository.saveQueryMetrics("prod-instance", snapshot);
 * }</pre>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see com.bovinemagnet.pgconsole.service.MetricsSamplerService
 * @see com.bovinemagnet.pgconsole.repository.HistoryRepository
 * @see com.bovinemagnet.pgconsole.service.QueryRegressionService
 */
public class QueryMetricsHistory {
    /**
     * Database-generated unique identifier for this metrics snapshot.
     * <p>
     * This is the primary key from the {@code pgconsole.query_metrics_history} table.
     */
    private Long id;

    /**
     * The timestamp when this metrics snapshot was captured.
     * <p>
     * Represents the exact moment the sampling occurred, used for temporal analysis
     * and identifying trends over time. Never null for persisted records.
     */
    private Instant sampledAt;

    /**
     * PostgreSQL query identifier hash from {@code pg_stat_statements.queryid}.
     * <p>
     * This is a stable hash of the normalised query structure (with constants replaced
     * by parameter placeholders). Different literal values in the same query structure
     * will share the same query ID, enabling aggregation of similar queries.
     */
    private String queryId;

    /**
     * The normalised SQL query text with constants replaced by parameter placeholders.
     * <p>
     * For example: {@code SELECT * FROM users WHERE id = $1 AND status = $2}
     * <p>
     * May be null if the query text has been evicted from {@code pg_stat_statements}
     * or truncated due to length limits.
     */
    private String queryText;

    /**
     * Cumulative number of times this query has been executed since statistics reset.
     * <p>
     * This counter is monotonically increasing. To calculate executions between samples,
     * subtract the previous sample's value from the current value.
     */
    private long totalCalls;

    /**
     * Cumulative total execution time in milliseconds since statistics reset.
     * <p>
     * Includes planning and execution time. This counter is monotonically increasing.
     * Divide by {@link #totalCalls} to get the historical mean execution time.
     */
    private double totalTimeMs;

    /**
     * Cumulative total number of rows returned or affected since statistics reset.
     * <p>
     * This counter is monotonically increasing. Useful for identifying queries that
     * process large result sets or perform bulk operations.
     */
    private long totalRows;

    /**
     * Mean execution time in milliseconds at the moment of sampling.
     * <p>
     * Calculated by PostgreSQL as {@code total_time / calls}. Provides a snapshot
     * of average query performance at this point in time.
     */
    private double meanTimeMs;

    /**
     * Minimum execution time in milliseconds observed across all executions.
     * <p>
     * Represents the fastest execution of this query. May be null if PostgreSQL
     * version does not track this metric or if the query has never executed.
     */
    private Double minTimeMs;

    /**
     * Maximum execution time in milliseconds observed across all executions.
     * <p>
     * Represents the slowest execution of this query. Useful for identifying
     * performance outliers and worst-case scenarios. May be null if PostgreSQL
     * version does not track this metric.
     */
    private Double maxTimeMs;

    /**
     * Standard deviation of execution times in milliseconds.
     * <p>
     * Indicates execution time variability. High standard deviation suggests
     * inconsistent performance, possibly due to parameter-dependent execution
     * plans or resource contention. May be null if insufficient data or PostgreSQL
     * version does not track this metric.
     */
    private Double stddevTimeMs;

    /**
     * Cumulative number of shared buffer blocks satisfied from PostgreSQL's buffer cache.
     * <p>
     * Blocks read from memory rather than disk. Higher values relative to
     * {@link #sharedBlksRead} indicate better cache efficiency. May be null if
     * {@code pg_stat_statements.track_io_timing} is disabled.
     */
    private Long sharedBlksHit;

    /**
     * Cumulative number of shared buffer blocks read from disk.
     * <p>
     * Physical disk reads required when blocks were not in cache. Lower values
     * relative to {@link #sharedBlksHit} indicate better cache efficiency. May be
     * null if {@code pg_stat_statements.track_io_timing} is disabled.
     */
    private Long sharedBlksRead;

    /**
     * Cumulative number of temporary blocks written to disk.
     * <p>
     * Non-zero values indicate work_mem was insufficient for in-memory operations,
     * forcing PostgreSQL to spill to disk. This typically impacts query performance
     * significantly. May be null if {@code pg_stat_statements.track_io_timing} is disabled.
     */
    private Long tempBlksWritten;

    /**
     * Returns the database-generated unique identifier for this snapshot.
     *
     * @return the primary key ID, or null if not yet persisted
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the database-generated unique identifier.
     *
     * @param id the primary key ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns the timestamp when this metrics snapshot was captured.
     *
     * @return the sampling timestamp, never null for persisted records
     */
    public Instant getSampledAt() {
        return sampledAt;
    }

    /**
     * Sets the timestamp when this metrics snapshot was captured.
     *
     * @param sampledAt the sampling timestamp
     */
    public void setSampledAt(Instant sampledAt) {
        this.sampledAt = sampledAt;
    }

    /**
     * Returns the PostgreSQL query identifier hash.
     *
     * @return the query ID from {@code pg_stat_statements.queryid}
     */
    public String getQueryId() {
        return queryId;
    }

    /**
     * Sets the PostgreSQL query identifier hash.
     *
     * @param queryId the query ID
     */
    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }

    /**
     * Returns the normalised SQL query text.
     *
     * @return the query text with parameter placeholders, or null if unavailable
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
     * Returns the cumulative number of times this query has been executed.
     *
     * @return the total execution count since statistics reset
     */
    public long getTotalCalls() {
        return totalCalls;
    }

    /**
     * Sets the cumulative number of times this query has been executed.
     *
     * @param totalCalls the total execution count
     */
    public void setTotalCalls(long totalCalls) {
        this.totalCalls = totalCalls;
    }

    /**
     * Returns the cumulative total execution time in milliseconds.
     *
     * @return the total execution time since statistics reset
     */
    public double getTotalTimeMs() {
        return totalTimeMs;
    }

    /**
     * Sets the cumulative total execution time in milliseconds.
     *
     * @param totalTimeMs the total execution time
     */
    public void setTotalTimeMs(double totalTimeMs) {
        this.totalTimeMs = totalTimeMs;
    }

    /**
     * Returns the cumulative total number of rows returned or affected.
     *
     * @return the total row count since statistics reset
     */
    public long getTotalRows() {
        return totalRows;
    }

    /**
     * Sets the cumulative total number of rows returned or affected.
     *
     * @param totalRows the total row count
     */
    public void setTotalRows(long totalRows) {
        this.totalRows = totalRows;
    }

    /**
     * Returns the mean execution time in milliseconds at sampling time.
     *
     * @return the average execution time
     */
    public double getMeanTimeMs() {
        return meanTimeMs;
    }

    /**
     * Sets the mean execution time in milliseconds.
     *
     * @param meanTimeMs the average execution time
     */
    public void setMeanTimeMs(double meanTimeMs) {
        this.meanTimeMs = meanTimeMs;
    }

    /**
     * Returns the minimum execution time in milliseconds.
     *
     * @return the fastest execution time, or null if not tracked
     */
    public Double getMinTimeMs() {
        return minTimeMs;
    }

    /**
     * Sets the minimum execution time in milliseconds.
     *
     * @param minTimeMs the fastest execution time
     */
    public void setMinTimeMs(Double minTimeMs) {
        this.minTimeMs = minTimeMs;
    }

    /**
     * Returns the maximum execution time in milliseconds.
     *
     * @return the slowest execution time, or null if not tracked
     */
    public Double getMaxTimeMs() {
        return maxTimeMs;
    }

    /**
     * Sets the maximum execution time in milliseconds.
     *
     * @param maxTimeMs the slowest execution time
     */
    public void setMaxTimeMs(Double maxTimeMs) {
        this.maxTimeMs = maxTimeMs;
    }

    /**
     * Returns the standard deviation of execution times in milliseconds.
     *
     * @return the execution time variability, or null if not tracked
     */
    public Double getStddevTimeMs() {
        return stddevTimeMs;
    }

    /**
     * Sets the standard deviation of execution times in milliseconds.
     *
     * @param stddevTimeMs the execution time variability
     */
    public void setStddevTimeMs(Double stddevTimeMs) {
        this.stddevTimeMs = stddevTimeMs;
    }

    /**
     * Returns the cumulative number of shared buffer blocks satisfied from cache.
     *
     * @return the buffer cache hits, or null if I/O timing not tracked
     */
    public Long getSharedBlksHit() {
        return sharedBlksHit;
    }

    /**
     * Sets the cumulative number of shared buffer blocks satisfied from cache.
     *
     * @param sharedBlksHit the buffer cache hits
     */
    public void setSharedBlksHit(Long sharedBlksHit) {
        this.sharedBlksHit = sharedBlksHit;
    }

    /**
     * Returns the cumulative number of shared buffer blocks read from disk.
     *
     * @return the physical disk reads, or null if I/O timing not tracked
     */
    public Long getSharedBlksRead() {
        return sharedBlksRead;
    }

    /**
     * Sets the cumulative number of shared buffer blocks read from disk.
     *
     * @param sharedBlksRead the physical disk reads
     */
    public void setSharedBlksRead(Long sharedBlksRead) {
        this.sharedBlksRead = sharedBlksRead;
    }

    /**
     * Returns the cumulative number of temporary blocks written to disk.
     *
     * @return the temporary disk writes, or null if I/O timing not tracked
     */
    public Long getTempBlksWritten() {
        return tempBlksWritten;
    }

    /**
     * Sets the cumulative number of temporary blocks written to disk.
     *
     * @param tempBlksWritten the temporary disk writes
     */
    public void setTempBlksWritten(Long tempBlksWritten) {
        this.tempBlksWritten = tempBlksWritten;
    }
}
