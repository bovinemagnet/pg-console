package com.bovinemagnet.pgconsole.model;

import java.time.Instant;

/**
 * Represents I/O statistics from the PostgreSQL system view {@code pg_stat_io}.
 * <p>
 * This class captures detailed I/O statistics per backend type, object type, and context,
 * providing insights into read, write, extend, and fsync operations.
 * Available in PostgreSQL 16+.
 * <p>
 * Statistics are broken down by:
 * <ul>
 *   <li>Backend type (autovacuum worker, client backend, etc.)</li>
 *   <li>Object type (relation, temp relation)</li>
 *   <li>Context (normal, vacuum, bulkread, bulkwrite)</li>
 * </ul>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see <a href="https://www.postgresql.org/docs/current/monitoring-stats.html#PG-STAT-IO-VIEW">pg_stat_io Documentation</a>
 */
public class IoStatistics {

    /** Type of backend (autovacuum worker, background worker, client backend, etc.) */
    private String backendType;

    /** Object type (relation, temp relation) */
    private String object;

    /** Context (normal, vacuum, bulkread, bulkwrite) */
    private String context;

    /** Number of read operations */
    private long reads;

    /** Time spent in read operations (milliseconds) */
    private double readTime;

    /** Number of write operations */
    private long writes;

    /** Time spent in write operations (milliseconds) */
    private double writeTime;

    /** Number of writeback requests */
    private long writebacks;

    /** Time spent in writeback operations (milliseconds) */
    private double writebackTime;

    /** Number of extend operations */
    private long extends_;

    /** Time spent in extend operations (milliseconds) */
    private double extendTime;

    /** Number of hits (reads satisfied from shared buffers) */
    private long hits;

    /** Number of evictions from shared buffers */
    private long evictions;

    /** Number of buffer reuses */
    private long reuses;

    /** Number of fsync calls */
    private long fsyncs;

    /** Time spent in fsync operations (milliseconds) */
    private double fsyncTime;

    /** Time when statistics were last reset */
    private Instant statsReset;

    /**
     * Constructs a new IoStatistics instance.
     */
    public IoStatistics() {
    }

    public String getBackendType() {
        return backendType;
    }

    public void setBackendType(String backendType) {
        this.backendType = backendType;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public long getReads() {
        return reads;
    }

    public void setReads(long reads) {
        this.reads = reads;
    }

    public double getReadTime() {
        return readTime;
    }

    public void setReadTime(double readTime) {
        this.readTime = readTime;
    }

    public long getWrites() {
        return writes;
    }

    public void setWrites(long writes) {
        this.writes = writes;
    }

    public double getWriteTime() {
        return writeTime;
    }

    public void setWriteTime(double writeTime) {
        this.writeTime = writeTime;
    }

    public long getWritebacks() {
        return writebacks;
    }

    public void setWritebacks(long writebacks) {
        this.writebacks = writebacks;
    }

    public double getWritebackTime() {
        return writebackTime;
    }

    public void setWritebackTime(double writebackTime) {
        this.writebackTime = writebackTime;
    }

    public long getExtends() {
        return extends_;
    }

    public void setExtends(long extends_) {
        this.extends_ = extends_;
    }

    public double getExtendTime() {
        return extendTime;
    }

    public void setExtendTime(double extendTime) {
        this.extendTime = extendTime;
    }

    public long getHits() {
        return hits;
    }

    public void setHits(long hits) {
        this.hits = hits;
    }

    public long getEvictions() {
        return evictions;
    }

    public void setEvictions(long evictions) {
        this.evictions = evictions;
    }

    public long getReuses() {
        return reuses;
    }

    public void setReuses(long reuses) {
        this.reuses = reuses;
    }

    public long getFsyncs() {
        return fsyncs;
    }

    public void setFsyncs(long fsyncs) {
        this.fsyncs = fsyncs;
    }

    public double getFsyncTime() {
        return fsyncTime;
    }

    public void setFsyncTime(double fsyncTime) {
        this.fsyncTime = fsyncTime;
    }

    public Instant getStatsReset() {
        return statsReset;
    }

    public void setStatsReset(Instant statsReset) {
        this.statsReset = statsReset;
    }

    /**
     * Returns the total I/O operations (reads + writes + extends).
     *
     * @return total I/O operation count
     */
    public long getTotalOperations() {
        return reads + writes + extends_;
    }

    /**
     * Returns the cache hit ratio for this context.
     *
     * @return hit ratio as a percentage, or 0 if no reads
     */
    public double getHitRatio() {
        long total = reads + hits;
        if (total == 0) return 0;
        return (hits * 100.0) / total;
    }

    /**
     * Returns the cache hit ratio formatted as a percentage string.
     *
     * @return formatted hit ratio
     */
    public String getHitRatioFormatted() {
        return String.format("%.1f%%", getHitRatio());
    }

    /**
     * Returns the read time formatted as a human-readable string.
     *
     * @return formatted read time
     */
    public String getReadTimeFormatted() {
        return formatTime(readTime);
    }

    /**
     * Returns the write time formatted as a human-readable string.
     *
     * @return formatted write time
     */
    public String getWriteTimeFormatted() {
        return formatTime(writeTime);
    }

    /**
     * Returns the fsync time formatted as a human-readable string.
     *
     * @return formatted fsync time
     */
    public String getFsyncTimeFormatted() {
        return formatTime(fsyncTime);
    }

    /**
     * Returns operations formatted with thousands separators.
     *
     * @param count the operation count
     * @return formatted count
     */
    public String formatCount(long count) {
        return String.format("%,d", count);
    }

    /**
     * Formats time in milliseconds as a human-readable string.
     *
     * @param timeMs time in milliseconds
     * @return formatted time string
     */
    private String formatTime(double timeMs) {
        if (timeMs < 0) return "N/A";
        if (timeMs == 0) return "0 ms";
        if (timeMs < 1000) return String.format("%.1f ms", timeMs);
        if (timeMs < 60000) return String.format("%.2f s", timeMs / 1000.0);
        if (timeMs < 3600000) return String.format("%.1f min", timeMs / 60000.0);
        return String.format("%.1f hours", timeMs / 3600000.0);
    }

    /**
     * Returns Bootstrap CSS class for the backend type badge.
     *
     * @return Bootstrap background class
     */
    public String getBackendTypeCssClass() {
        if (backendType == null) return "bg-secondary";
        return switch (backendType.toLowerCase()) {
            case "client backend" -> "bg-primary";
            case "autovacuum worker" -> "bg-warning text-dark";
            case "background worker" -> "bg-info";
            case "checkpointer" -> "bg-success";
            case "startup" -> "bg-secondary";
            default -> "bg-secondary";
        };
    }

    /**
     * Returns Bootstrap CSS class for the context badge.
     *
     * @return Bootstrap background class
     */
    public String getContextCssClass() {
        if (context == null) return "bg-secondary";
        return switch (context.toLowerCase()) {
            case "normal" -> "bg-primary";
            case "vacuum" -> "bg-warning text-dark";
            case "bulkread" -> "bg-info";
            case "bulkwrite" -> "bg-success";
            default -> "bg-secondary";
        };
    }
}
