package com.bovinemagnet.pgconsole.model;

import java.time.Duration;
import java.time.Instant;

/**
 * Represents WAL archiving statistics from PostgreSQL.
 * <p>
 * This model captures data from the {@code pg_stat_archiver} system view, which provides
 * information about the WAL archiver process. WAL archiving is essential for point-in-time
 * recovery (PITR) and creating base backups.
 * <p>
 * Key metrics tracked:
 * <ul>
 *   <li>Count of successfully archived WAL files</li>
 *   <li>Count of failed archive attempts</li>
 *   <li>Last archived WAL file name and timestamp</li>
 *   <li>Last failed archive attempt details</li>
 * </ul>
 * <p>
 * Archive failures are critical and can lead to data loss if not addressed promptly,
 * as PostgreSQL may run out of WAL storage space.
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see <a href="https://www.postgresql.org/docs/current/monitoring-stats.html#PG-STAT-ARCHIVER-VIEW">pg_stat_archiver Documentation</a>
 */
public class ArchiverStats {

    /** Number of WAL files that have been successfully archived */
    private long archivedCount;

    /** Name of the last successfully archived WAL file */
    private String lastArchivedWal;

    /** Time when the last WAL file was successfully archived */
    private Instant lastArchivedTime;

    /** Number of failed attempts to archive WAL files */
    private long failedCount;

    /** Name of the WAL file that failed to be archived, or null if no failures */
    private String lastFailedWal;

    /** Time of the last archive failure, or null if no failures */
    private Instant lastFailedTime;

    /** Time at which these statistics were last reset */
    private Instant statsReset;

    /** Whether archiving is enabled on this server */
    private boolean archivingEnabled;

    /** Current archive mode setting */
    private String archiveMode;

    /** Archive command configured */
    private String archiveCommand;

    /**
     * Constructs a new ArchiverStats instance with default values.
     */
    public ArchiverStats() {
    }

    public long getArchivedCount() {
        return archivedCount;
    }

    public void setArchivedCount(long archivedCount) {
        this.archivedCount = archivedCount;
    }

    public String getLastArchivedWal() {
        return lastArchivedWal;
    }

    public void setLastArchivedWal(String lastArchivedWal) {
        this.lastArchivedWal = lastArchivedWal;
    }

    public Instant getLastArchivedTime() {
        return lastArchivedTime;
    }

    public void setLastArchivedTime(Instant lastArchivedTime) {
        this.lastArchivedTime = lastArchivedTime;
    }

    public long getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(long failedCount) {
        this.failedCount = failedCount;
    }

    public String getLastFailedWal() {
        return lastFailedWal;
    }

    public void setLastFailedWal(String lastFailedWal) {
        this.lastFailedWal = lastFailedWal;
    }

    public Instant getLastFailedTime() {
        return lastFailedTime;
    }

    public void setLastFailedTime(Instant lastFailedTime) {
        this.lastFailedTime = lastFailedTime;
    }

    public Instant getStatsReset() {
        return statsReset;
    }

    public void setStatsReset(Instant statsReset) {
        this.statsReset = statsReset;
    }

    public boolean isArchivingEnabled() {
        return archivingEnabled;
    }

    public void setArchivingEnabled(boolean archivingEnabled) {
        this.archivingEnabled = archivingEnabled;
    }

    public String getArchiveMode() {
        return archiveMode;
    }

    public void setArchiveMode(String archiveMode) {
        this.archiveMode = archiveMode;
        this.archivingEnabled = "on".equalsIgnoreCase(archiveMode) || "always".equalsIgnoreCase(archiveMode);
    }

    public String getArchiveCommand() {
        return archiveCommand;
    }

    public void setArchiveCommand(String archiveCommand) {
        this.archiveCommand = archiveCommand;
    }

    // ========== Computed Metrics ==========

    /**
     * Checks if there have been any archive failures.
     *
     * @return true if failed_count > 0
     */
    public boolean hasFailures() {
        return failedCount > 0;
    }

    /**
     * Calculates the archive success rate as a percentage.
     *
     * @return success rate (0-100), or 100 if no attempts
     */
    public double getSuccessRate() {
        long total = archivedCount + failedCount;
        if (total == 0) return 100.0;
        return (archivedCount * 100.0) / total;
    }

    /**
     * Calculates the time since the last successful archive.
     *
     * @return duration since last archive, or null if never archived
     */
    public Duration getTimeSinceLastArchive() {
        if (lastArchivedTime == null) return null;
        return Duration.between(lastArchivedTime, Instant.now());
    }

    /**
     * Returns the time since last archive as a human-readable string.
     *
     * @return formatted duration (e.g., "5 minutes", "2 hours"), or "Never" if no archive
     */
    public String getTimeSinceLastArchiveFormatted() {
        Duration d = getTimeSinceLastArchive();
        if (d == null) return "Never";

        long hours = d.toHours();
        if (hours >= 24) {
            long days = hours / 24;
            return days + (days == 1 ? " day" : " days");
        }
        if (hours >= 1) {
            return hours + (hours == 1 ? " hour" : " hours");
        }
        long minutes = d.toMinutes();
        if (minutes >= 1) {
            return minutes + (minutes == 1 ? " minute" : " minutes");
        }
        return "< 1 minute";
    }

    /**
     * Checks if the archive lag is concerning (over 1 hour).
     *
     * @return true if archiving is enabled and lag exceeds 1 hour
     */
    public boolean hasArchiveLag() {
        if (!archivingEnabled) return false;
        Duration d = getTimeSinceLastArchive();
        return d != null && d.toHours() >= 1;
    }

    /**
     * Returns the overall health status of archiving.
     * <p>
     * Health is determined by:
     * <ul>
     *   <li>HEALTHY: No failures, recent archive activity</li>
     *   <li>WARNING: Some failures or archiving lag</li>
     *   <li>CRITICAL: Recent failures or significant lag</li>
     *   <li>DISABLED: Archiving not enabled</li>
     * </ul>
     *
     * @return health status string
     */
    public String getHealthStatus() {
        if (!archivingEnabled) return "DISABLED";

        // Recent failure is critical
        if (lastFailedTime != null) {
            Duration sinceFail = Duration.between(lastFailedTime, Instant.now());
            if (sinceFail.toHours() < 1) return "CRITICAL";
        }

        // Any failures is a warning
        if (failedCount > 0) return "WARNING";

        // Long archive lag is a warning
        if (hasArchiveLag()) return "WARNING";

        return "HEALTHY";
    }

    /**
     * Returns a CSS class based on the health status.
     *
     * @return Bootstrap CSS class for badge styling
     */
    public String getHealthCssClass() {
        return switch (getHealthStatus()) {
            case "HEALTHY" -> "bg-success";
            case "WARNING" -> "bg-warning text-dark";
            case "CRITICAL" -> "bg-danger";
            case "DISABLED" -> "bg-secondary";
            default -> "bg-secondary";
        };
    }

    /**
     * Returns a CSS class for the failure count display.
     *
     * @return Bootstrap CSS class based on failure severity
     */
    public String getFailedCountCssClass() {
        if (failedCount == 0) return "text-success";
        if (failedCount < 10) return "text-warning";
        return "text-danger fw-bold";
    }

    /**
     * Returns a recommendation based on archiver status.
     *
     * @return recommendation text, or null if everything is optimal
     */
    public String getRecommendation() {
        if (!archivingEnabled) {
            return "WAL archiving is disabled. Enable it for point-in-time recovery capability.";
        }
        if (lastFailedTime != null && Duration.between(lastFailedTime, Instant.now()).toHours() < 24) {
            return "Recent archive failure detected. Check archive_command configuration and destination availability.";
        }
        if (failedCount > 0) {
            return "Archive failures have occurred. Review archive logs and verify destination storage.";
        }
        if (hasArchiveLag()) {
            return "Archive lag detected. Check archive process and destination throughput.";
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
