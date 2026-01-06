package com.bovinemagnet.pgconsole.model;

import java.time.Instant;

/**
 * Represents checkpoint statistics from PostgreSQL 17+.
 * <p>
 * This model captures data from the {@code pg_stat_checkpointer} system view, which
 * was introduced in PostgreSQL 17 to separate checkpoint statistics from the
 * background writer statistics. For earlier versions, this data is included in
 * {@link BgWriterStats}.
 * <p>
 * Checkpoints are critical for data durability - they ensure that all dirty buffers
 * are written to disk, creating a known-good starting point for crash recovery.
 * <p>
 * Key metrics tracked:
 * <ul>
 *   <li>Number of timed vs requested checkpoints</li>
 *   <li>Number of restartpoints (on standbys)</li>
 *   <li>Checkpoint write and sync timing</li>
 *   <li>Buffers written during checkpoints</li>
 * </ul>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see <a href="https://www.postgresql.org/docs/17/monitoring-stats.html#PG-STAT-CHECKPOINTER-VIEW">pg_stat_checkpointer Documentation</a>
 */
public class CheckpointStats {

    /** Number of scheduled checkpoints performed (from checkpoint_timeout) */
    private long numTimed;

    /** Number of requested checkpoints performed (from manual CHECKPOINT, pg_switch_wal, etc.) */
    private long numRequested;

    /** Number of restartpoints performed (on standby servers) */
    private long restartpointsTimed;

    /** Number of requested restartpoints performed */
    private long restartpointsReq;

    /** Number of restartpoints that have been performed due to WAL segment switching */
    private long restartpointsDone;

    /** Total time spent writing checkpoint data to disk in milliseconds */
    private double writeTime;

    /** Total time spent syncing checkpoint data to disk in milliseconds */
    private double syncTime;

    /** Number of buffers written during checkpoints */
    private long buffersWritten;

    /** Time at which these statistics were last reset */
    private Instant statsReset;

    /** Whether this view is available (PostgreSQL 17+) */
    private boolean available = false;

    /**
     * Constructs a new CheckpointStats instance with default values.
     */
    public CheckpointStats() {
    }

    public long getNumTimed() {
        return numTimed;
    }

    public void setNumTimed(long numTimed) {
        this.numTimed = numTimed;
    }

    public long getNumRequested() {
        return numRequested;
    }

    public void setNumRequested(long numRequested) {
        this.numRequested = numRequested;
    }

    public long getRestartpointsTimed() {
        return restartpointsTimed;
    }

    public void setRestartpointsTimed(long restartpointsTimed) {
        this.restartpointsTimed = restartpointsTimed;
    }

    public long getRestartpointsReq() {
        return restartpointsReq;
    }

    public void setRestartpointsReq(long restartpointsReq) {
        this.restartpointsReq = restartpointsReq;
    }

    public long getRestartpointsDone() {
        return restartpointsDone;
    }

    public void setRestartpointsDone(long restartpointsDone) {
        this.restartpointsDone = restartpointsDone;
    }

    public double getWriteTime() {
        return writeTime;
    }

    public void setWriteTime(double writeTime) {
        this.writeTime = writeTime;
    }

    public double getSyncTime() {
        return syncTime;
    }

    public void setSyncTime(double syncTime) {
        this.syncTime = syncTime;
    }

    public long getBuffersWritten() {
        return buffersWritten;
    }

    public void setBuffersWritten(long buffersWritten) {
        this.buffersWritten = buffersWritten;
    }

    public Instant getStatsReset() {
        return statsReset;
    }

    public void setStatsReset(Instant statsReset) {
        this.statsReset = statsReset;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    // ========== Computed Metrics ==========

    /**
     * Returns the total number of checkpoints performed.
     *
     * @return total checkpoints (timed + requested)
     */
    public long getTotalCheckpoints() {
        return numTimed + numRequested;
    }

    /**
     * Returns the total number of restartpoints performed.
     *
     * @return total restartpoints
     */
    public long getTotalRestartpoints() {
        return restartpointsTimed + restartpointsReq + restartpointsDone;
    }

    /**
     * Calculates the percentage of checkpoints that were timed (scheduled).
     * <p>
     * A healthy primary should have mostly timed checkpoints (above 90%).
     *
     * @return percentage of timed checkpoints (0-100), or 0 if no checkpoints
     */
    public double getTimedCheckpointPercent() {
        long total = getTotalCheckpoints();
        if (total == 0) return 0;
        return (numTimed * 100.0) / total;
    }

    /**
     * Calculates the average checkpoint write time in seconds.
     *
     * @return average write time per checkpoint in seconds
     */
    public double getAvgWriteTimeSec() {
        long total = getTotalCheckpoints();
        if (total == 0) return 0;
        return (writeTime / 1000.0) / total;
    }

    /**
     * Calculates the average checkpoint sync time in seconds.
     *
     * @return average sync time per checkpoint in seconds
     */
    public double getAvgSyncTimeSec() {
        long total = getTotalCheckpoints();
        if (total == 0) return 0;
        return (syncTime / 1000.0) / total;
    }

    /**
     * Calculates the average buffers written per checkpoint.
     *
     * @return average buffers per checkpoint, or 0 if no checkpoints
     */
    public double getAvgBuffersPerCheckpoint() {
        long total = getTotalCheckpoints();
        if (total == 0) return 0;
        return (double) buffersWritten / total;
    }

    /**
     * Checks if this is a standby server (has restartpoints).
     *
     * @return true if restartpoints are present
     */
    public boolean isStandby() {
        return getTotalRestartpoints() > 0;
    }

    /**
     * Returns a CSS class based on the timed checkpoint percentage.
     *
     * @return Bootstrap CSS class for severity indication
     */
    public String getTimedCheckpointCssClass() {
        double pct = getTimedCheckpointPercent();
        if (pct >= 90) return "text-success";
        if (pct >= 70) return "text-warning";
        return "text-danger";
    }

    /**
     * Returns a recommendation based on checkpoint statistics.
     *
     * @return recommendation text, or null if optimal
     */
    public String getRecommendation() {
        double timedPct = getTimedCheckpointPercent();
        if (timedPct < 70 && timedPct > 0) {
            return "Consider increasing max_wal_size to reduce the frequency of requested checkpoints";
        }
        double avgWriteSec = getAvgWriteTimeSec();
        if (avgWriteSec > 30) {
            return "Checkpoint writes are taking too long; consider increasing checkpoint_completion_target";
        }
        return null;
    }

    /**
     * Checks if there is a recommendation available.
     *
     * @return true if there is a recommendation
     */
    public boolean hasRecommendation() {
        return getRecommendation() != null;
    }
}
