package com.bovinemagnet.pgconsole.model;

import java.time.Instant;

/**
 * Immutable data point storing all live chart metric values at a single point in time.
 * <p>
 * Captures the same 4 metric groups as the live charts page:
 * <ul>
 *   <li><b>Connections</b>: active, idle, idleInTransaction (gauges)</li>
 *   <li><b>Transactions</b>: commits, rollbacks (cumulative counters)</li>
 *   <li><b>Tuples</b>: inserted, updated, deleted (cumulative counters)</li>
 *   <li><b>Cache</b>: bufferCacheHitRatio, indexCacheHitRatio (percentages)</li>
 * </ul>
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class LiveChartHistoryPoint {

    private final Instant sampledAt;

    // Connection gauges
    private final double active;
    private final double idle;
    private final double idleInTransaction;

    // Transaction cumulative counters
    private final double commits;
    private final double rollbacks;

    // Tuple cumulative counters
    private final double inserted;
    private final double updated;
    private final double deleted;

    // Cache hit ratios (percentages)
    private final double bufferCacheHitRatio;
    private final double indexCacheHitRatio;

    /**
     * Constructs a new history point with all metric values.
     *
     * @param sampledAt           the timestamp when this sample was taken
     * @param active              number of active connections
     * @param idle                number of idle connections
     * @param idleInTransaction   number of idle-in-transaction connections
     * @param commits             cumulative transaction commit count
     * @param rollbacks           cumulative transaction rollback count
     * @param inserted            cumulative tuples inserted count
     * @param updated             cumulative tuples updated count
     * @param deleted             cumulative tuples deleted count
     * @param bufferCacheHitRatio buffer cache hit ratio percentage
     * @param indexCacheHitRatio  index cache hit ratio percentage
     */
    public LiveChartHistoryPoint(Instant sampledAt,
                                  double active, double idle, double idleInTransaction,
                                  double commits, double rollbacks,
                                  double inserted, double updated, double deleted,
                                  double bufferCacheHitRatio, double indexCacheHitRatio) {
        this.sampledAt = sampledAt;
        this.active = active;
        this.idle = idle;
        this.idleInTransaction = idleInTransaction;
        this.commits = commits;
        this.rollbacks = rollbacks;
        this.inserted = inserted;
        this.updated = updated;
        this.deleted = deleted;
        this.bufferCacheHitRatio = bufferCacheHitRatio;
        this.indexCacheHitRatio = indexCacheHitRatio;
    }

    public Instant getSampledAt() {
        return sampledAt;
    }

    public double getActive() {
        return active;
    }

    public double getIdle() {
        return idle;
    }

    public double getIdleInTransaction() {
        return idleInTransaction;
    }

    public double getCommits() {
        return commits;
    }

    public double getRollbacks() {
        return rollbacks;
    }

    public double getInserted() {
        return inserted;
    }

    public double getUpdated() {
        return updated;
    }

    public double getDeleted() {
        return deleted;
    }

    public double getBufferCacheHitRatio() {
        return bufferCacheHitRatio;
    }

    public double getIndexCacheHitRatio() {
        return indexCacheHitRatio;
    }
}
