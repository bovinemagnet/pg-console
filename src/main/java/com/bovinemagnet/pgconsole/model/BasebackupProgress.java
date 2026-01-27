package com.bovinemagnet.pgconsole.model;

/**
 * Represents base backup progress from the PostgreSQL system view
 * {@code pg_stat_progress_basebackup}.
 * <p>
 * This class captures detailed information about ongoing pg_basebackup operations,
 * including the current phase and progress metrics.
 * Available in PostgreSQL 13+.
 * <p>
 * The operation proceeds through several phases:
 * <ul>
 *   <li>Initializing</li>
 *   <li>Waiting for checkpoint to finish</li>
 *   <li>Estimating backup size</li>
 *   <li>Streaming database files</li>
 *   <li>Waiting for WAL archiving to finish</li>
 *   <li>Transferring WAL</li>
 * </ul>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see <a href="https://www.postgresql.org/docs/current/progress-reporting.html#BASEBACKUP-PROGRESS-REPORTING">BASE BACKUP Progress Reporting</a>
 */
public class BasebackupProgress {

    /** Process ID of the WAL sender process */
    private int pid;

    /** Current phase of the operation */
    private String phase;

    /** Total number of tablespace directories to back up */
    private long backupTotal;

    /** Number of tablespaces backed up */
    private long backupStreamed;

    /** Total estimated size of all tablespaces */
    private long tablespaceTotal;

    /** Amount of data streamed so far */
    private long tablespaceStreamed;

    /** Computed progress percentage */
    private double progressPercent;

    /**
     * Constructs a new BasebackupProgress instance.
     */
    public BasebackupProgress() {
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public long getBackupTotal() {
        return backupTotal;
    }

    public void setBackupTotal(long backupTotal) {
        this.backupTotal = backupTotal;
    }

    public long getBackupStreamed() {
        return backupStreamed;
    }

    public void setBackupStreamed(long backupStreamed) {
        this.backupStreamed = backupStreamed;
    }

    public long getTablespaceTotal() {
        return tablespaceTotal;
    }

    public void setTablespaceTotal(long tablespaceTotal) {
        this.tablespaceTotal = tablespaceTotal;
    }

    public long getTablespaceStreamed() {
        return tablespaceStreamed;
    }

    public void setTablespaceStreamed(long tablespaceStreamed) {
        this.tablespaceStreamed = tablespaceStreamed;
    }

    public double getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(double progressPercent) {
        this.progressPercent = progressPercent;
    }

    /**
     * Calculates progress based on tablespace size streamed.
     */
    public void calculateProgress() {
        if (tablespaceTotal > 0) {
            this.progressPercent = (tablespaceStreamed * 100.0) / tablespaceTotal;
        } else if (backupTotal > 0) {
            this.progressPercent = (backupStreamed * 100.0) / backupTotal;
        } else {
            this.progressPercent = 0;
        }
    }

    /**
     * Returns formatted progress percentage.
     *
     * @return progress with one decimal place
     */
    public String getProgressFormatted() {
        return String.format("%.1f%%", progressPercent);
    }

    /**
     * Returns Bootstrap CSS class for the progress bar.
     *
     * @return Bootstrap background class
     */
    public String getProgressBarCssClass() {
        if (progressPercent >= 90) return "bg-success";
        if (progressPercent >= 50) return "bg-info";
        return "bg-primary";
    }

    /**
     * Returns Bootstrap CSS class for the phase badge.
     *
     * @return Bootstrap background class
     */
    public String getPhaseCssClass() {
        if (phase == null) return "bg-secondary";
        if (phase.toLowerCase().contains("streaming")) return "bg-primary";
        if (phase.toLowerCase().contains("waiting")) return "bg-warning text-dark";
        if (phase.toLowerCase().contains("estimating")) return "bg-info";
        if (phase.toLowerCase().contains("transferring")) return "bg-success";
        return "bg-secondary";
    }

    /**
     * Returns the tablespace streamed formatted as a human-readable size.
     *
     * @return formatted size string
     */
    public String getTablespaceStreamedFormatted() {
        return formatBytes(tablespaceStreamed);
    }

    /**
     * Returns the total tablespace size formatted as a human-readable size.
     *
     * @return formatted size string
     */
    public String getTablespaceTotalFormatted() {
        return formatBytes(tablespaceTotal);
    }

    /**
     * Formats bytes as a human-readable size string.
     *
     * @param bytes the byte count
     * @return formatted size string
     */
    private String formatBytes(long bytes) {
        if (bytes < 0) return "N/A";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
