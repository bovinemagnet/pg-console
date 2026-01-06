package com.bovinemagnet.pgconsole.model;

import java.time.Instant;

/**
 * Represents background writer and checkpoint statistics from PostgreSQL.
 * <p>
 * This model captures data from the {@code pg_stat_bgwriter} system view, which provides
 * server-wide statistics about the background writer process and checkpoint activity.
 * The background writer periodically flushes dirty buffers to reduce checkpoint I/O spikes.
 * <p>
 * Key metrics tracked:
 * <ul>
 *   <li>Checkpoint counts (timed vs requested)</li>
 *   <li>Buffer write statistics (by bgwriter, backends, and checkpoints)</li>
 *   <li>Write timing and efficiency metrics</li>
 *   <li>Checkpoint synchronisation timing</li>
 * </ul>
 * <p>
 * Note: In PostgreSQL 17+, some checkpoint-specific statistics have moved to
 * {@code pg_stat_checkpointer}. This model provides unified access regardless of version.
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see <a href="https://www.postgresql.org/docs/current/monitoring-stats.html#PG-STAT-BGWRITER-VIEW">pg_stat_bgwriter Documentation</a>
 */
public class BgWriterStats {

    /** Number of scheduled checkpoints that have been performed */
    private long checkpointsTimed;

    /** Number of requested checkpoints that have been performed */
    private long checkpointsReq;

    /** Total time spent writing checkpoint data in milliseconds */
    private double checkpointWriteTime;

    /** Total time spent syncing checkpoint data in milliseconds */
    private double checkpointSyncTime;

    /** Number of buffers written during checkpoints */
    private long buffersCheckpoint;

    /** Number of buffers written directly by a backend (not bgwriter or checkpoint) */
    private long buffersBackend;

    /** Number of buffers written by the background writer */
    private long buffersClean;

    /** Number of times background writer stopped a cleaning scan because it wrote too many buffers */
    private long maxwrittenClean;

    /** Number of buffers allocated */
    private long buffersAlloc;

    /** Number of times a backend had to execute fsync itself (bad for performance) */
    private long buffersFsyncBackend;

    /** Time at which these statistics were last reset */
    private Instant statsReset;

    /**
     * Constructs a new BgWriterStats instance with default values.
     */
    public BgWriterStats() {
    }

    public long getCheckpointsTimed() {
        return checkpointsTimed;
    }

    public void setCheckpointsTimed(long checkpointsTimed) {
        this.checkpointsTimed = checkpointsTimed;
    }

    public long getCheckpointsReq() {
        return checkpointsReq;
    }

    public void setCheckpointsReq(long checkpointsReq) {
        this.checkpointsReq = checkpointsReq;
    }

    public double getCheckpointWriteTime() {
        return checkpointWriteTime;
    }

    public void setCheckpointWriteTime(double checkpointWriteTime) {
        this.checkpointWriteTime = checkpointWriteTime;
    }

    public double getCheckpointSyncTime() {
        return checkpointSyncTime;
    }

    public void setCheckpointSyncTime(double checkpointSyncTime) {
        this.checkpointSyncTime = checkpointSyncTime;
    }

    public long getBuffersCheckpoint() {
        return buffersCheckpoint;
    }

    public void setBuffersCheckpoint(long buffersCheckpoint) {
        this.buffersCheckpoint = buffersCheckpoint;
    }

    public long getBuffersBackend() {
        return buffersBackend;
    }

    public void setBuffersBackend(long buffersBackend) {
        this.buffersBackend = buffersBackend;
    }

    public long getBuffersClean() {
        return buffersClean;
    }

    public void setBuffersClean(long buffersClean) {
        this.buffersClean = buffersClean;
    }

    public long getMaxwrittenClean() {
        return maxwrittenClean;
    }

    public void setMaxwrittenClean(long maxwrittenClean) {
        this.maxwrittenClean = maxwrittenClean;
    }

    public long getBuffersAlloc() {
        return buffersAlloc;
    }

    public void setBuffersAlloc(long buffersAlloc) {
        this.buffersAlloc = buffersAlloc;
    }

    public long getBuffersFsyncBackend() {
        return buffersFsyncBackend;
    }

    public void setBuffersFsyncBackend(long buffersFsyncBackend) {
        this.buffersFsyncBackend = buffersFsyncBackend;
    }

    public Instant getStatsReset() {
        return statsReset;
    }

    public void setStatsReset(Instant statsReset) {
        this.statsReset = statsReset;
    }

    // ========== Computed Metrics ==========

    /**
     * Returns the total number of checkpoints performed.
     *
     * @return total checkpoints (timed + requested)
     */
    public long getTotalCheckpoints() {
        return checkpointsTimed + checkpointsReq;
    }

    /**
     * Calculates the percentage of checkpoints that were timed (scheduled).
     * <p>
     * A healthy system should have mostly timed checkpoints (above 90%).
     * High requested checkpoint percentage indicates WAL filling up too quickly.
     *
     * @return percentage of timed checkpoints (0-100), or 0 if no checkpoints
     */
    public double getTimedCheckpointPercent() {
        long total = getTotalCheckpoints();
        if (total == 0) return 0;
        return (checkpointsTimed * 100.0) / total;
    }

    /**
     * Calculates the average checkpoint write time in seconds.
     *
     * @return average write time per checkpoint in seconds, or 0 if no checkpoints
     */
    public double getAvgCheckpointWriteTimeSec() {
        long total = getTotalCheckpoints();
        if (total == 0) return 0;
        return (checkpointWriteTime / 1000.0) / total;
    }

    /**
     * Calculates the average checkpoint sync time in seconds.
     *
     * @return average sync time per checkpoint in seconds, or 0 if no checkpoints
     */
    public double getAvgCheckpointSyncTimeSec() {
        long total = getTotalCheckpoints();
        if (total == 0) return 0;
        return (checkpointSyncTime / 1000.0) / total;
    }

    /**
     * Calculates the backend write ratio as a percentage.
     * <p>
     * Backend writes occur when the background writer cannot keep up with dirty buffer
     * generation. High values (above 10%) indicate the background writer needs tuning
     * (increase bgwriter_lru_maxpages or decrease bgwriter_delay).
     *
     * @return percentage of writes performed by backends, or 0 if no writes
     */
    public double getBackendWritePercent() {
        long totalWrites = buffersCheckpoint + buffersBackend + buffersClean;
        if (totalWrites == 0) return 0;
        return (buffersBackend * 100.0) / totalWrites;
    }

    /**
     * Checks if checkpoint performance is healthy.
     * <p>
     * Returns true if timed checkpoints dominate and backend writes are minimal.
     *
     * @return true if checkpoint behaviour is optimal
     */
    public boolean isHealthy() {
        return getTimedCheckpointPercent() >= 90 && getBackendWritePercent() <= 10;
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
     * Returns a CSS class based on the backend write percentage.
     * <p>
     * Backend writes should ideally be below 10% of total writes.
     *
     * @return Bootstrap CSS class for severity indication
     */
    public String getBackendWriteCssClass() {
        double pct = getBackendWritePercent();
        if (pct <= 5) return "text-success";
        if (pct <= 15) return "text-warning";
        return "text-danger";
    }

    /**
     * Returns a CSS class based on the maxwritten_clean count.
     * <p>
     * Non-zero values indicate bgwriter is hitting its buffer limit.
     *
     * @return Bootstrap CSS class for severity indication
     */
    public String getMaxwrittenCleanCssClass() {
        if (maxwrittenClean == 0) return "text-success";
        if (maxwrittenClean < 100) return "text-warning";
        return "text-danger";
    }

    /**
     * Returns a recommendation based on current statistics.
     *
     * @return recommendation text, or null if everything is optimal
     */
    public String getRecommendation() {
        if (getTimedCheckpointPercent() < 70) {
            return "Consider increasing checkpoint_timeout or max_wal_size to reduce requested checkpoints";
        }
        if (getBackendWritePercent() > 15) {
            return "Consider increasing bgwriter_lru_maxpages or decreasing bgwriter_delay";
        }
        if (maxwrittenClean > 0) {
            return "Background writer is hitting maxwritten_clean limit; consider increasing bgwriter_lru_maxpages";
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
