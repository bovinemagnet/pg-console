package com.bovinemagnet.pgconsole.model;

/**
 * Aggregates WAL and checkpoint monitoring statistics for dashboard display.
 * <p>
 * This composite model brings together statistics from multiple PostgreSQL
 * system views to provide a comprehensive overview of WAL generation,
 * checkpointing behaviour, and archiving status.
 * <p>
 * Components:
 * <ul>
 *   <li>{@link WalStats} - WAL generation metrics (PG14+)</li>
 *   <li>{@link BgWriterStats} - Background writer and checkpoint stats</li>
 *   <li>{@link CheckpointStats} - Checkpoint-specific stats (PG17+)</li>
 *   <li>{@link ArchiverStats} - WAL archiving status</li>
 * </ul>
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class WalCheckpointOverview {

    /** WAL generation statistics (null if PG version < 14) */
    private WalStats walStats;

    /** Background writer statistics (always available) */
    private BgWriterStats bgWriterStats;

    /** Checkpointer statistics (null if PG version < 17) */
    private CheckpointStats checkpointStats;

    /** WAL archiver statistics (always available but may show archiving disabled) */
    private ArchiverStats archiverStats;

    /** PostgreSQL server version number (e.g., 160000 for PG16) */
    private int pgVersionNum;

    /** PostgreSQL server version string (e.g., "PostgreSQL 16.2") */
    private String pgVersion;

    /**
     * Constructs a new WalCheckpointOverview with default values.
     */
    public WalCheckpointOverview() {
    }

    public WalStats getWalStats() {
        return walStats;
    }

    public void setWalStats(WalStats walStats) {
        this.walStats = walStats;
    }

    public BgWriterStats getBgWriterStats() {
        return bgWriterStats;
    }

    public void setBgWriterStats(BgWriterStats bgWriterStats) {
        this.bgWriterStats = bgWriterStats;
    }

    public CheckpointStats getCheckpointStats() {
        return checkpointStats;
    }

    public void setCheckpointStats(CheckpointStats checkpointStats) {
        this.checkpointStats = checkpointStats;
    }

    public ArchiverStats getArchiverStats() {
        return archiverStats;
    }

    public void setArchiverStats(ArchiverStats archiverStats) {
        this.archiverStats = archiverStats;
    }

    public int getPgVersionNum() {
        return pgVersionNum;
    }

    public void setPgVersionNum(int pgVersionNum) {
        this.pgVersionNum = pgVersionNum;
    }

    public String getPgVersion() {
        return pgVersion;
    }

    public void setPgVersion(String pgVersion) {
        this.pgVersion = pgVersion;
    }

    // ========== Feature Availability ==========

    /**
     * Checks if pg_stat_wal is available (PostgreSQL 14+).
     *
     * @return true if WAL stats are available
     */
    public boolean hasWalStats() {
        return pgVersionNum >= 140000 && walStats != null;
    }

    /**
     * Checks if pg_stat_checkpointer is available (PostgreSQL 17+).
     *
     * @return true if separate checkpoint stats are available
     */
    public boolean hasCheckpointStats() {
        return pgVersionNum >= 170000 && checkpointStats != null;
    }

    /**
     * Checks if WAL archiving is configured and enabled.
     *
     * @return true if archiving is enabled
     */
    public boolean hasArchiving() {
        return archiverStats != null && archiverStats.isArchivingEnabled();
    }

    // ========== Health Summary ==========

    /**
     * Returns the overall health status of WAL/checkpoint operations.
     * <p>
     * Evaluates multiple factors:
     * <ul>
     *   <li>Checkpoint timing (timed vs requested ratio)</li>
     *   <li>Backend write ratio</li>
     *   <li>Archiver health</li>
     * </ul>
     *
     * @return health status: "HEALTHY", "WARNING", or "CRITICAL"
     */
    public String getOverallHealthStatus() {
        boolean hasWarning = false;
        boolean hasCritical = false;

        // Check archiver status
        if (archiverStats != null && archiverStats.isArchivingEnabled()) {
            String arcHealth = archiverStats.getHealthStatus();
            if ("CRITICAL".equals(arcHealth)) hasCritical = true;
            if ("WARNING".equals(arcHealth)) hasWarning = true;
        }

        // Check checkpoint efficiency
        if (bgWriterStats != null) {
            if (bgWriterStats.getTimedCheckpointPercent() < 50) hasCritical = true;
            else if (bgWriterStats.getTimedCheckpointPercent() < 70) hasWarning = true;

            if (bgWriterStats.getBackendWritePercent() > 25) hasCritical = true;
            else if (bgWriterStats.getBackendWritePercent() > 15) hasWarning = true;
        }

        if (hasCritical) return "CRITICAL";
        if (hasWarning) return "WARNING";
        return "HEALTHY";
    }

    /**
     * Returns a CSS class for the overall health status badge.
     *
     * @return Bootstrap CSS class
     */
    public String getOverallHealthCssClass() {
        return switch (getOverallHealthStatus()) {
            case "HEALTHY" -> "bg-success";
            case "WARNING" -> "bg-warning text-dark";
            case "CRITICAL" -> "bg-danger";
            default -> "bg-secondary";
        };
    }

    /**
     * Returns the number of issues detected across all components.
     *
     * @return count of issues
     */
    public int getIssueCount() {
        int count = 0;

        if (bgWriterStats != null) {
            if (bgWriterStats.hasRecommendation()) count++;
        }

        if (checkpointStats != null) {
            if (checkpointStats.hasRecommendation()) count++;
        }

        if (archiverStats != null) {
            if (archiverStats.hasRecommendation()) count++;
        }

        return count;
    }

    /**
     * Checks if there are any issues requiring attention.
     *
     * @return true if issues exist
     */
    public boolean hasIssues() {
        return getIssueCount() > 0;
    }

    /**
     * Returns a summary text for the status badge.
     *
     * @return status text (e.g., "Healthy", "2 issues")
     */
    public String getStatusSummary() {
        int issues = getIssueCount();
        if (issues == 0) return "Healthy";
        return issues + (issues == 1 ? " issue" : " issues");
    }
}
