package com.bovinemagnet.pgconsole.model;

import java.time.Instant;

/**
 * Represents Write-Ahead Log (WAL) statistics from PostgreSQL.
 * <p>
 * This model captures data from the {@code pg_stat_wal} system view (available in PostgreSQL 14+),
 * which provides server-wide statistics about WAL activity. WAL is PostgreSQL's mechanism for
 * ensuring data durability by recording changes before they are applied to data files.
 * <p>
 * Key metrics tracked:
 * <ul>
 *   <li>WAL record counts and sizes</li>
 *   <li>Full page image (FPI) statistics</li>
 *   <li>WAL buffer usage and sync operations</li>
 *   <li>Write and sync timing information</li>
 * </ul>
 * <p>
 * Note: This view requires PostgreSQL 14 or later. For earlier versions, WAL statistics
 * are limited and may not be available.
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see <a href="https://www.postgresql.org/docs/current/monitoring-stats.html#PG-STAT-WAL-VIEW">pg_stat_wal Documentation</a>
 */
public class WalStats {

    /** Total number of WAL records generated */
    private long walRecords;

    /** Total number of full page images (FPI) generated due to hint bit updates */
    private long walFpi;

    /** Total amount of WAL generated in bytes */
    private long walBytes;

    /** Number of times WAL buffers were written to disk via XLogWrite request */
    private long walBuffersFull;

    /** Number of times WAL data was written to disk */
    private long walWrite;

    /** Number of times WAL files were synced to disk */
    private long walSync;

    /** Total time spent writing WAL to disk in milliseconds */
    private double walWriteTime;

    /** Total time spent syncing WAL files to disk in milliseconds */
    private double walSyncTime;

    /** Time at which these statistics were last reset */
    private Instant statsReset;

    /** PostgreSQL version number (to check feature availability) */
    private int pgVersionNum;

    /**
     * Constructs a new WalStats instance with default values.
     */
    public WalStats() {
    }

    /**
     * Constructs a new WalStats instance with all values specified.
     *
     * @param walRecords total WAL records generated
     * @param walFpi total full page images generated
     * @param walBytes total WAL bytes generated
     * @param walBuffersFull times WAL buffers were full
     * @param walWrite times WAL was written
     * @param walSync times WAL was synced
     * @param walWriteTime total write time in ms
     * @param walSyncTime total sync time in ms
     * @param statsReset time of last stats reset
     */
    public WalStats(long walRecords, long walFpi, long walBytes, long walBuffersFull,
                    long walWrite, long walSync, double walWriteTime, double walSyncTime,
                    Instant statsReset) {
        this.walRecords = walRecords;
        this.walFpi = walFpi;
        this.walBytes = walBytes;
        this.walBuffersFull = walBuffersFull;
        this.walWrite = walWrite;
        this.walSync = walSync;
        this.walWriteTime = walWriteTime;
        this.walSyncTime = walSyncTime;
        this.statsReset = statsReset;
    }

    public long getWalRecords() {
        return walRecords;
    }

    public void setWalRecords(long walRecords) {
        this.walRecords = walRecords;
    }

    public long getWalFpi() {
        return walFpi;
    }

    public void setWalFpi(long walFpi) {
        this.walFpi = walFpi;
    }

    public long getWalBytes() {
        return walBytes;
    }

    public void setWalBytes(long walBytes) {
        this.walBytes = walBytes;
    }

    public long getWalBuffersFull() {
        return walBuffersFull;
    }

    public void setWalBuffersFull(long walBuffersFull) {
        this.walBuffersFull = walBuffersFull;
    }

    public long getWalWrite() {
        return walWrite;
    }

    public void setWalWrite(long walWrite) {
        this.walWrite = walWrite;
    }

    public long getWalSync() {
        return walSync;
    }

    public void setWalSync(long walSync) {
        this.walSync = walSync;
    }

    public double getWalWriteTime() {
        return walWriteTime;
    }

    public void setWalWriteTime(double walWriteTime) {
        this.walWriteTime = walWriteTime;
    }

    public double getWalSyncTime() {
        return walSyncTime;
    }

    public void setWalSyncTime(double walSyncTime) {
        this.walSyncTime = walSyncTime;
    }

    public Instant getStatsReset() {
        return statsReset;
    }

    public void setStatsReset(Instant statsReset) {
        this.statsReset = statsReset;
    }

    public int getPgVersionNum() {
        return pgVersionNum;
    }

    public void setPgVersionNum(int pgVersionNum) {
        this.pgVersionNum = pgVersionNum;
    }

    /**
     * Returns the WAL bytes formatted as a human-readable string.
     *
     * @return formatted size (e.g., "1.5 GB", "230.0 MB")
     */
    public String getWalBytesFormatted() {
        return formatBytes(walBytes);
    }

    /**
     * Calculates the average WAL write time per operation in milliseconds.
     *
     * @return average write time, or 0 if no writes occurred
     */
    public double getAvgWriteTimeMs() {
        if (walWrite == 0) return 0;
        return walWriteTime / walWrite;
    }

    /**
     * Calculates the average WAL sync time per operation in milliseconds.
     *
     * @return average sync time, or 0 if no syncs occurred
     */
    public double getAvgSyncTimeMs() {
        if (walSync == 0) return 0;
        return walSyncTime / walSync;
    }

    /**
     * Calculates the full page image ratio as a percentage.
     * <p>
     * A high FPI ratio may indicate frequent hint bit updates or
     * aggressive checkpoint settings.
     *
     * @return FPI ratio as percentage (0-100), or 0 if no records
     */
    public double getFpiRatio() {
        if (walRecords == 0) return 0;
        return (walFpi * 100.0) / walRecords;
    }

    /**
     * Returns the FPI ratio formatted as a percentage string.
     *
     * @return formatted FPI ratio (e.g., "12.5")
     */
    public String getFpiRatioFormatted() {
        return String.format("%.1f", getFpiRatio());
    }

    /**
     * Returns the average write time formatted as a string.
     *
     * @return formatted average write time (e.g., "0.12")
     */
    public String getAvgWriteTimeMsFormatted() {
        return String.format("%.2f", getAvgWriteTimeMs());
    }

    /**
     * Returns the average sync time formatted as a string.
     *
     * @return formatted average sync time (e.g., "0.45")
     */
    public String getAvgSyncTimeMsFormatted() {
        return String.format("%.2f", getAvgSyncTimeMs());
    }

    /**
     * Checks if pg_stat_wal is available (PostgreSQL 14+).
     *
     * @return true if WAL stats are available
     */
    public boolean isAvailable() {
        return pgVersionNum >= 140000;
    }

    /**
     * Returns a CSS class based on the FPI ratio.
     * <p>
     * High FPI ratios indicate inefficiency in WAL generation.
     *
     * @return Bootstrap CSS class for severity indication
     */
    public String getFpiRatioCssClass() {
        double ratio = getFpiRatio();
        if (ratio > 50) return "text-danger";
        if (ratio > 30) return "text-warning";
        return "text-success";
    }

    /**
     * Formats a byte count into a human-readable string.
     */
    private String formatBytes(long bytes) {
        if (bytes < 0) return "N/A";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        if (bytes < 1024L * 1024 * 1024 * 1024) return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        return String.format("%.1f TB", bytes / (1024.0 * 1024 * 1024 * 1024));
    }
}
