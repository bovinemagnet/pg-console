package com.bovinemagnet.pgconsole.model;

/**
 * Represents TOAST (The Oversized-Attribute Storage Technique) table bloat metrics.
 * <p>
 * TOAST tables store large values (text, bytea, etc.) separately from the main table.
 * This model tracks TOAST-specific bloat which can be significant for tables with
 * large text or binary columns. PostgreSQL automatically creates TOAST tables when
 * a table contains columns that may exceed the page size (typically 8KB).
 * </p>
 * <p>
 * Bloat occurs when TOAST tables accumulate dead tuples that haven't been reclaimed
 * by VACUUM. High TOAST bloat can impact query performance and waste disk space.
 * </p>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see <a href="https://www.postgresql.org/docs/current/storage-toast.html">PostgreSQL TOAST Documentation</a>
 */
public class ToastBloat {
    /** The schema containing the main table. */
    private String schemaName;

    /** The name of the main table that owns the TOAST table. */
    private String tableName;

    /** The name of the TOAST table (typically pg_toast.pg_toast_xxxxx). */
    private String toastTableName;

    /** The size of the main table in bytes. */
    private long mainTableSizeBytes;

    /** The size of the TOAST table in bytes. */
    private long toastTableSizeBytes;

    /** The size of the TOAST table's index in bytes. */
    private long toastIndexSizeBytes;

    /** The estimated amount of reclaimable bloat in bytes. */
    private long estimatedBloatBytes;

    /** The bloat as a percentage of the total TOAST table size. */
    private double bloatPercent;

    /** The number of live tuples in the TOAST table. */
    private long nLiveTup;

    /** The number of dead tuples in the TOAST table. */
    private long nDeadTup;

    public ToastBloat() {}

    public String getSchemaName() { return schemaName; }
    public void setSchemaName(String schemaName) { this.schemaName = schemaName; }

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }

    public String getToastTableName() { return toastTableName; }
    public void setToastTableName(String toastTableName) { this.toastTableName = toastTableName; }

    public long getMainTableSizeBytes() { return mainTableSizeBytes; }
    public void setMainTableSizeBytes(long mainTableSizeBytes) { this.mainTableSizeBytes = mainTableSizeBytes; }

    public long getToastTableSizeBytes() { return toastTableSizeBytes; }
    public void setToastTableSizeBytes(long toastTableSizeBytes) { this.toastTableSizeBytes = toastTableSizeBytes; }

    public long getToastIndexSizeBytes() { return toastIndexSizeBytes; }
    public void setToastIndexSizeBytes(long toastIndexSizeBytes) { this.toastIndexSizeBytes = toastIndexSizeBytes; }

    public long getEstimatedBloatBytes() { return estimatedBloatBytes; }
    public void setEstimatedBloatBytes(long estimatedBloatBytes) { this.estimatedBloatBytes = estimatedBloatBytes; }

    public double getBloatPercent() { return bloatPercent; }
    public void setBloatPercent(double bloatPercent) { this.bloatPercent = bloatPercent; }

    public long getnLiveTup() { return nLiveTup; }
    public void setnLiveTup(long nLiveTup) { this.nLiveTup = nLiveTup; }

    public long getnDeadTup() { return nDeadTup; }
    public void setnDeadTup(long nDeadTup) { this.nDeadTup = nDeadTup; }

    /**
     * Returns the fully qualified table name in schema.table format.
     * <p>
     * This is useful for uniquely identifying tables across different schemas.
     * </p>
     *
     * @return the schema-qualified table name (e.g., "public.users")
     */
    public String getFullTableName() {
        return schemaName + "." + tableName;
    }

    /**
     * Calculates the TOAST to main table size ratio as a percentage.
     * <p>
     * This metric indicates how much TOAST storage is used relative to the main table.
     * A high ratio suggests the table contains many large values stored in TOAST.
     * </p>
     *
     * @return the ratio as a percentage (e.g., 150.0 means TOAST table is 1.5x the main table size),
     *         or 0.0 if the main table size is zero
     */
    public double getToastRatio() {
        if (mainTableSizeBytes == 0) return 0.0;
        return (double) toastTableSizeBytes / (double) mainTableSizeBytes * 100.0;
    }

    /**
     * Returns the total TOAST storage size (table plus index) in human-readable format.
     * <p>
     * Combines both the TOAST table and its index to show the complete TOAST storage footprint.
     * </p>
     *
     * @return formatted size string (e.g., "1.5 GB", "256.0 MB", "4.2 KB")
     */
    public String getTotalToastSizeDisplay() {
        long total = toastTableSizeBytes + toastIndexSizeBytes;
        return formatBytes(total);
    }

    /**
     * Returns the estimated reclaimable bloat space in human-readable format.
     * <p>
     * This represents the approximate amount of disk space that could be reclaimed
     * by running VACUUM FULL on the TOAST table.
     * </p>
     *
     * @return formatted size string (e.g., "128.5 MB", "2.3 GB")
     */
    public String getReclaimableDisplay() {
        return formatBytes(estimatedBloatBytes);
    }

    /**
     * Returns the main table size in human-readable format.
     *
     * @return formatted size string (e.g., "5.2 GB", "128.0 MB")
     */
    public String getMainTableSizeDisplay() {
        return formatBytes(mainTableSizeBytes);
    }

    /**
     * Returns the TOAST table size in human-readable format.
     *
     * @return formatted size string (e.g., "1.8 GB", "64.5 MB")
     */
    public String getToastTableSizeDisplay() {
        return formatBytes(toastTableSizeBytes);
    }

    /**
     * Formats a byte count into a human-readable size string using binary units.
     * <p>
     * Automatically selects the most appropriate unit (B, KB, MB, GB) based on size.
     * Uses 1024 as the divisor for binary units.
     * </p>
     *
     * @param bytes the number of bytes to format
     * @return formatted string with appropriate unit (e.g., "1.5 GB", "256.0 MB", "4 B")
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    /**
     * Determines the severity level based on the bloat percentage relative to a threshold.
     * <p>
     * The severity is calculated as follows:
     * <ul>
     *   <li><strong>critical</strong>: bloat percentage is at least twice the warning threshold</li>
     *   <li><strong>warning</strong>: bloat percentage meets or exceeds the warning threshold</li>
     *   <li><strong>ok</strong>: bloat percentage is below the warning threshold</li>
     * </ul>
     * </p>
     *
     * @param warnThreshold the warning threshold percentage (e.g., 20.0 for 20%)
     * @return "critical", "warning", or "ok" indicating the severity level
     */
    public String getSeverity(double warnThreshold) {
        if (bloatPercent >= warnThreshold * 2) return "critical";
        if (bloatPercent >= warnThreshold) return "warning";
        return "ok";
    }

    /**
     * Returns the Bootstrap CSS class for styling based on bloat severity.
     * <p>
     * Maps severity levels to Bootstrap text colour classes for visual indication:
     * <ul>
     *   <li><strong>critical</strong> → "text-danger" (red)</li>
     *   <li><strong>warning</strong> → "text-warning" (yellow/amber)</li>
     *   <li><strong>ok</strong> → "text-success" (green)</li>
     * </ul>
     * </p>
     *
     * @param warnThreshold the warning threshold percentage used to determine severity
     * @return Bootstrap CSS class name for styling the bloat indicator
     * @see #getSeverity(double)
     */
    public String getSeverityClass(double warnThreshold) {
        String severity = getSeverity(warnThreshold);
        return switch (severity) {
            case "critical" -> "text-danger";
            case "warning" -> "text-warning";
            default -> "text-success";
        };
    }
}
