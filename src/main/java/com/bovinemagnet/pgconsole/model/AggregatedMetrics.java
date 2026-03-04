package com.bovinemagnet.pgconsole.model;

import java.time.Instant;

/**
 * Aggregated metrics over a time window.
 * <p>
 * Contains averaged and summed metrics computed from persisted history data.
 * Used by the comparison window feature to summarise metrics for a period.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class AggregatedMetrics {

    /** Start of the aggregation window. */
    private Instant windowStart;

    /** End of the aggregation window. */
    private Instant windowEnd;

    /** Number of samples in the aggregation window. */
    private int sampleCount;

    /** Average total connections across the window. */
    private double avgTotalConnections;

    /** Average active queries across the window. */
    private double avgActiveQueries;

    /** Average blocked queries across the window. */
    private double avgBlockedQueries;

    /** Average idle-in-transaction connections across the window. */
    private double avgIdleInTransaction;

    /** Average cache hit ratio across the window. */
    private Double avgCacheHitRatio;

    /** Average transaction commit rate per second. */
    private double avgCommitRate;

    /** Average transaction rollback rate per second. */
    private double avgRollbackRate;

    /** Average tuple insert rate per second. */
    private double avgInsertRate;

    /** Average tuple update rate per second. */
    private double avgUpdateRate;

    /** Average tuple delete rate per second. */
    private double avgDeleteRate;

    /** Average longest query duration in seconds. */
    private Double avgLongestQuerySeconds;

    /** Average database size in bytes. */
    private Long avgDatabaseSizeBytes;

    /**
     * Constructs an empty aggregated metrics instance.
     */
    public AggregatedMetrics() {
    }

    /**
     * Gets the window start time.
     *
     * @return start of aggregation window
     */
    public Instant getWindowStart() {
        return windowStart;
    }

    /**
     * Sets the window start time.
     *
     * @param windowStart start of aggregation window
     */
    public void setWindowStart(Instant windowStart) {
        this.windowStart = windowStart;
    }

    /**
     * Gets the window end time.
     *
     * @return end of aggregation window
     */
    public Instant getWindowEnd() {
        return windowEnd;
    }

    /**
     * Sets the window end time.
     *
     * @param windowEnd end of aggregation window
     */
    public void setWindowEnd(Instant windowEnd) {
        this.windowEnd = windowEnd;
    }

    /**
     * Gets the number of samples.
     *
     * @return sample count
     */
    public int getSampleCount() {
        return sampleCount;
    }

    /**
     * Sets the number of samples.
     *
     * @param sampleCount sample count
     */
    public void setSampleCount(int sampleCount) {
        this.sampleCount = sampleCount;
    }

    /**
     * Gets the average total connections.
     *
     * @return average total connections
     */
    public double getAvgTotalConnections() {
        return avgTotalConnections;
    }

    /**
     * Sets the average total connections.
     *
     * @param avgTotalConnections average total connections
     */
    public void setAvgTotalConnections(double avgTotalConnections) {
        this.avgTotalConnections = avgTotalConnections;
    }

    /**
     * Gets the average active queries.
     *
     * @return average active queries
     */
    public double getAvgActiveQueries() {
        return avgActiveQueries;
    }

    /**
     * Sets the average active queries.
     *
     * @param avgActiveQueries average active queries
     */
    public void setAvgActiveQueries(double avgActiveQueries) {
        this.avgActiveQueries = avgActiveQueries;
    }

    /**
     * Gets the average blocked queries.
     *
     * @return average blocked queries
     */
    public double getAvgBlockedQueries() {
        return avgBlockedQueries;
    }

    /**
     * Sets the average blocked queries.
     *
     * @param avgBlockedQueries average blocked queries
     */
    public void setAvgBlockedQueries(double avgBlockedQueries) {
        this.avgBlockedQueries = avgBlockedQueries;
    }

    /**
     * Gets the average idle-in-transaction connections.
     *
     * @return average idle-in-transaction connections
     */
    public double getAvgIdleInTransaction() {
        return avgIdleInTransaction;
    }

    /**
     * Sets the average idle-in-transaction connections.
     *
     * @param avgIdleInTransaction average idle-in-transaction connections
     */
    public void setAvgIdleInTransaction(double avgIdleInTransaction) {
        this.avgIdleInTransaction = avgIdleInTransaction;
    }

    /**
     * Gets the average cache hit ratio.
     *
     * @return average cache hit ratio, or null if unavailable
     */
    public Double getAvgCacheHitRatio() {
        return avgCacheHitRatio;
    }

    /**
     * Sets the average cache hit ratio.
     *
     * @param avgCacheHitRatio average cache hit ratio
     */
    public void setAvgCacheHitRatio(Double avgCacheHitRatio) {
        this.avgCacheHitRatio = avgCacheHitRatio;
    }

    /**
     * Gets the average commit rate per second.
     *
     * @return average commit rate
     */
    public double getAvgCommitRate() {
        return avgCommitRate;
    }

    /**
     * Sets the average commit rate per second.
     *
     * @param avgCommitRate average commit rate
     */
    public void setAvgCommitRate(double avgCommitRate) {
        this.avgCommitRate = avgCommitRate;
    }

    /**
     * Gets the average rollback rate per second.
     *
     * @return average rollback rate
     */
    public double getAvgRollbackRate() {
        return avgRollbackRate;
    }

    /**
     * Sets the average rollback rate per second.
     *
     * @param avgRollbackRate average rollback rate
     */
    public void setAvgRollbackRate(double avgRollbackRate) {
        this.avgRollbackRate = avgRollbackRate;
    }

    /**
     * Gets the average insert rate per second.
     *
     * @return average insert rate
     */
    public double getAvgInsertRate() {
        return avgInsertRate;
    }

    /**
     * Sets the average insert rate per second.
     *
     * @param avgInsertRate average insert rate
     */
    public void setAvgInsertRate(double avgInsertRate) {
        this.avgInsertRate = avgInsertRate;
    }

    /**
     * Gets the average update rate per second.
     *
     * @return average update rate
     */
    public double getAvgUpdateRate() {
        return avgUpdateRate;
    }

    /**
     * Sets the average update rate per second.
     *
     * @param avgUpdateRate average update rate
     */
    public void setAvgUpdateRate(double avgUpdateRate) {
        this.avgUpdateRate = avgUpdateRate;
    }

    /**
     * Gets the average delete rate per second.
     *
     * @return average delete rate
     */
    public double getAvgDeleteRate() {
        return avgDeleteRate;
    }

    /**
     * Sets the average delete rate per second.
     *
     * @param avgDeleteRate average delete rate
     */
    public void setAvgDeleteRate(double avgDeleteRate) {
        this.avgDeleteRate = avgDeleteRate;
    }

    /**
     * Gets the average longest query duration.
     *
     * @return average longest query in seconds, or null if unavailable
     */
    public Double getAvgLongestQuerySeconds() {
        return avgLongestQuerySeconds;
    }

    /**
     * Sets the average longest query duration.
     *
     * @param avgLongestQuerySeconds average longest query in seconds
     */
    public void setAvgLongestQuerySeconds(Double avgLongestQuerySeconds) {
        this.avgLongestQuerySeconds = avgLongestQuerySeconds;
    }

    /**
     * Gets the average database size in bytes.
     *
     * @return average database size, or null if unavailable
     */
    public Long getAvgDatabaseSizeBytes() {
        return avgDatabaseSizeBytes;
    }

    /**
     * Sets the average database size in bytes.
     *
     * @param avgDatabaseSizeBytes average database size
     */
    public void setAvgDatabaseSizeBytes(Long avgDatabaseSizeBytes) {
        this.avgDatabaseSizeBytes = avgDatabaseSizeBytes;
    }
}
