package com.bovinemagnet.pgconsole.model;

/**
 * Represents COPY progress from the PostgreSQL system view
 * {@code pg_stat_progress_copy}.
 * <p>
 * This class captures detailed information about ongoing COPY operations,
 * including the current command type, data source/destination, and progress metrics.
 * Available in PostgreSQL 14+.
 * <p>
 * Tracks both COPY FROM (import) and COPY TO (export) operations with metrics for:
 * <ul>
 *   <li>Number of tuples processed</li>
 *   <li>Number of tuples excluded by WHERE clause</li>
 *   <li>Bytes processed for file-based operations</li>
 * </ul>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see <a href="https://www.postgresql.org/docs/current/progress-reporting.html#COPY-PROGRESS-REPORTING">COPY Progress Reporting</a>
 */
public class CopyProgress {

    /** Process ID of the backend performing the operation */
    private int pid;

    /** Name of the database */
    private String database;

    /** Schema name of the table */
    private String schemaName;

    /** Name of the table being copied */
    private String tableName;

    /** Type of COPY command: FROM or TO */
    private String command;

    /** Type of data source or destination: FILE, PROGRAM, PIPE, or CALLBACK */
    private String type;

    /** Number of bytes already processed (file-based operations) */
    private long bytesProcessed;

    /** Total bytes in the file (if known) */
    private long bytesTotal;

    /** Number of tuples already processed */
    private long tuplesProcessed;

    /** Number of tuples excluded by WHERE clause (COPY FROM) */
    private long tuplesExcluded;

    /** Computed progress percentage */
    private double progressPercent;

    /**
     * Constructs a new CopyProgress instance.
     */
    public CopyProgress() {
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getBytesProcessed() {
        return bytesProcessed;
    }

    public void setBytesProcessed(long bytesProcessed) {
        this.bytesProcessed = bytesProcessed;
    }

    public long getBytesTotal() {
        return bytesTotal;
    }

    public void setBytesTotal(long bytesTotal) {
        this.bytesTotal = bytesTotal;
    }

    public long getTuplesProcessed() {
        return tuplesProcessed;
    }

    public void setTuplesProcessed(long tuplesProcessed) {
        this.tuplesProcessed = tuplesProcessed;
    }

    public long getTuplesExcluded() {
        return tuplesExcluded;
    }

    public void setTuplesExcluded(long tuplesExcluded) {
        this.tuplesExcluded = tuplesExcluded;
    }

    public double getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(double progressPercent) {
        this.progressPercent = progressPercent;
    }

    /**
     * Calculates progress based on bytes processed (if known).
     */
    public void calculateProgress() {
        if (bytesTotal > 0) {
            this.progressPercent = (bytesProcessed * 100.0) / bytesTotal;
        } else {
            // Cannot calculate progress without knowing total
            this.progressPercent = -1;
        }
    }

    /**
     * Returns the fully qualified table name.
     *
     * @return schema.table format
     */
    public String getFullTableName() {
        if (schemaName != null && !schemaName.isEmpty()) {
            return schemaName + "." + tableName;
        }
        return tableName;
    }

    /**
     * Returns formatted progress percentage.
     *
     * @return progress with one decimal place, or "Unknown" if not calculable
     */
    public String getProgressFormatted() {
        if (progressPercent < 0) return "Unknown";
        return String.format("%.1f%%", progressPercent);
    }

    /**
     * Returns Bootstrap CSS class for the progress bar.
     *
     * @return Bootstrap background class
     */
    public String getProgressBarCssClass() {
        if (progressPercent < 0) return "bg-secondary";
        if (progressPercent >= 90) return "bg-success";
        if (progressPercent >= 50) return "bg-info";
        return "bg-primary";
    }

    /**
     * Returns Bootstrap CSS class for the command badge.
     *
     * @return Bootstrap background class
     */
    public String getCommandCssClass() {
        if ("FROM".equalsIgnoreCase(command)) return "bg-primary";
        if ("TO".equalsIgnoreCase(command)) return "bg-info";
        return "bg-secondary";
    }

    /**
     * Returns the bytes processed formatted as a human-readable size.
     *
     * @return formatted size string
     */
    public String getBytesProcessedFormatted() {
        return formatBytes(bytesProcessed);
    }

    /**
     * Returns the total bytes formatted as a human-readable size.
     *
     * @return formatted size string
     */
    public String getBytesTotalFormatted() {
        return formatBytes(bytesTotal);
    }

    /**
     * Returns the tuples processed formatted with thousands separators.
     *
     * @return formatted tuple count
     */
    public String getTuplesProcessedFormatted() {
        return String.format("%,d", tuplesProcessed);
    }

    /**
     * Formats bytes as a human-readable size string.
     *
     * @param bytes the byte count
     * @return formatted size string
     */
    private String formatBytes(long bytes) {
        if (bytes < 0) return "N/A";
        if (bytes == 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
