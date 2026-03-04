package com.bovinemagnet.pgconsole.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Point-in-time snapshot of key database metrics.
 * <p>
 * Used by the stopwatch feature to capture before/after states
 * for performance comparison. Includes connection counts, query statistics,
 * cache ratios, transaction rates, tuple operations, lock information,
 * and a snapshot of the top queries at the time of capture.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class MetricsSnapshot {

    /** When this snapshot was captured. */
    private Instant capturedAt;

    /** Total number of database connections. */
    private int totalConnections;

    /** Number of active queries. */
    private int activeQueries;

    /** Number of blocked queries. */
    private int blockedQueries;

    /** Number of idle connections. */
    private int idleConnections;

    /** Number of idle-in-transaction connections. */
    private int idleInTransaction;

    /** Buffer cache hit ratio as a percentage (0-100). */
    private Double cacheHitRatio;

    /** Transaction commit count (cumulative). */
    private long transactionCommits;

    /** Transaction rollback count (cumulative). */
    private long transactionRollbacks;

    /** Tuples inserted (cumulative). */
    private long tuplesInserted;

    /** Tuples updated (cumulative). */
    private long tuplesUpdated;

    /** Tuples deleted (cumulative). */
    private long tuplesDeleted;

    /** Total database size in bytes. */
    private Long totalDatabaseSizeBytes;

    /** Duration of the longest running query in seconds. */
    private Double longestQuerySeconds;

    /** Top queries at the time of snapshot. */
    private List<SlowQuery> topQueries;

    /**
     * Constructs an empty metrics snapshot with the current time.
     */
    public MetricsSnapshot() {
        this.capturedAt = Instant.now();
        this.topQueries = new ArrayList<>();
    }

    /**
     * Gets the capture timestamp.
     *
     * @return when this snapshot was taken
     */
    public Instant getCapturedAt() {
        return capturedAt;
    }

    /**
     * Sets the capture timestamp.
     *
     * @param capturedAt when this snapshot was taken
     */
    public void setCapturedAt(Instant capturedAt) {
        this.capturedAt = capturedAt;
    }

    /**
     * Gets the total connection count.
     *
     * @return total connections
     */
    public int getTotalConnections() {
        return totalConnections;
    }

    /**
     * Sets the total connection count.
     *
     * @param totalConnections total connections
     */
    public void setTotalConnections(int totalConnections) {
        this.totalConnections = totalConnections;
    }

    /**
     * Gets the active query count.
     *
     * @return active queries
     */
    public int getActiveQueries() {
        return activeQueries;
    }

    /**
     * Sets the active query count.
     *
     * @param activeQueries active queries
     */
    public void setActiveQueries(int activeQueries) {
        this.activeQueries = activeQueries;
    }

    /**
     * Gets the blocked query count.
     *
     * @return blocked queries
     */
    public int getBlockedQueries() {
        return blockedQueries;
    }

    /**
     * Sets the blocked query count.
     *
     * @param blockedQueries blocked queries
     */
    public void setBlockedQueries(int blockedQueries) {
        this.blockedQueries = blockedQueries;
    }

    /**
     * Gets the idle connection count.
     *
     * @return idle connections
     */
    public int getIdleConnections() {
        return idleConnections;
    }

    /**
     * Sets the idle connection count.
     *
     * @param idleConnections idle connections
     */
    public void setIdleConnections(int idleConnections) {
        this.idleConnections = idleConnections;
    }

    /**
     * Gets the idle-in-transaction connection count.
     *
     * @return idle-in-transaction connections
     */
    public int getIdleInTransaction() {
        return idleInTransaction;
    }

    /**
     * Sets the idle-in-transaction connection count.
     *
     * @param idleInTransaction idle-in-transaction connections
     */
    public void setIdleInTransaction(int idleInTransaction) {
        this.idleInTransaction = idleInTransaction;
    }

    /**
     * Gets the cache hit ratio.
     *
     * @return cache hit ratio as a percentage, or null if unavailable
     */
    public Double getCacheHitRatio() {
        return cacheHitRatio;
    }

    /**
     * Sets the cache hit ratio.
     *
     * @param cacheHitRatio cache hit ratio as a percentage
     */
    public void setCacheHitRatio(Double cacheHitRatio) {
        this.cacheHitRatio = cacheHitRatio;
    }

    /**
     * Gets the cumulative transaction commit count.
     *
     * @return transaction commits
     */
    public long getTransactionCommits() {
        return transactionCommits;
    }

    /**
     * Sets the cumulative transaction commit count.
     *
     * @param transactionCommits transaction commits
     */
    public void setTransactionCommits(long transactionCommits) {
        this.transactionCommits = transactionCommits;
    }

    /**
     * Gets the cumulative transaction rollback count.
     *
     * @return transaction rollbacks
     */
    public long getTransactionRollbacks() {
        return transactionRollbacks;
    }

    /**
     * Sets the cumulative transaction rollback count.
     *
     * @param transactionRollbacks transaction rollbacks
     */
    public void setTransactionRollbacks(long transactionRollbacks) {
        this.transactionRollbacks = transactionRollbacks;
    }

    /**
     * Gets the cumulative tuples inserted count.
     *
     * @return tuples inserted
     */
    public long getTuplesInserted() {
        return tuplesInserted;
    }

    /**
     * Sets the cumulative tuples inserted count.
     *
     * @param tuplesInserted tuples inserted
     */
    public void setTuplesInserted(long tuplesInserted) {
        this.tuplesInserted = tuplesInserted;
    }

    /**
     * Gets the cumulative tuples updated count.
     *
     * @return tuples updated
     */
    public long getTuplesUpdated() {
        return tuplesUpdated;
    }

    /**
     * Sets the cumulative tuples updated count.
     *
     * @param tuplesUpdated tuples updated
     */
    public void setTuplesUpdated(long tuplesUpdated) {
        this.tuplesUpdated = tuplesUpdated;
    }

    /**
     * Gets the cumulative tuples deleted count.
     *
     * @return tuples deleted
     */
    public long getTuplesDeleted() {
        return tuplesDeleted;
    }

    /**
     * Sets the cumulative tuples deleted count.
     *
     * @param tuplesDeleted tuples deleted
     */
    public void setTuplesDeleted(long tuplesDeleted) {
        this.tuplesDeleted = tuplesDeleted;
    }

    /**
     * Gets the total database size in bytes.
     *
     * @return total size in bytes, or null if unavailable
     */
    public Long getTotalDatabaseSizeBytes() {
        return totalDatabaseSizeBytes;
    }

    /**
     * Sets the total database size in bytes.
     *
     * @param totalDatabaseSizeBytes total size in bytes
     */
    public void setTotalDatabaseSizeBytes(Long totalDatabaseSizeBytes) {
        this.totalDatabaseSizeBytes = totalDatabaseSizeBytes;
    }

    /**
     * Gets the duration of the longest running query.
     *
     * @return longest query duration in seconds, or null if unavailable
     */
    public Double getLongestQuerySeconds() {
        return longestQuerySeconds;
    }

    /**
     * Sets the duration of the longest running query.
     *
     * @param longestQuerySeconds longest query duration in seconds
     */
    public void setLongestQuerySeconds(Double longestQuerySeconds) {
        this.longestQuerySeconds = longestQuerySeconds;
    }

    /**
     * Gets the top queries at the time of snapshot.
     *
     * @return list of top queries
     */
    public List<SlowQuery> getTopQueries() {
        return topQueries;
    }

    /**
     * Sets the top queries at the time of snapshot.
     *
     * @param topQueries list of top queries
     */
    public void setTopQueries(List<SlowQuery> topQueries) {
        this.topQueries = topQueries;
    }
}
