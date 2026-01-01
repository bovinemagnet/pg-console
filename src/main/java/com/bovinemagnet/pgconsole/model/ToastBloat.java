package com.bovinemagnet.pgconsole.model;

/**
 * Represents TOAST (The Oversized-Attribute Storage Technique) table bloat metrics.
 * <p>
 * TOAST tables store large values (text, bytea, etc.) separately from the main table.
 * This model tracks TOAST-specific bloat which can be significant for tables with
 * large text or binary columns.
 * </p>
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class ToastBloat {
    private String schemaName;
    private String tableName;
    private String toastTableName;
    private long mainTableSizeBytes;
    private long toastTableSizeBytes;
    private long toastIndexSizeBytes;
    private long estimatedBloatBytes;
    private double bloatPercent;
    private long nLiveTup;
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
     * Returns the fully qualified table name.
     *
     * @return schema.table format
     */
    public String getFullTableName() {
        return schemaName + "." + tableName;
    }

    /**
     * Returns the TOAST to main table size ratio as a percentage.
     *
     * @return ratio percentage
     */
    public double getToastRatio() {
        if (mainTableSizeBytes == 0) return 0.0;
        return (double) toastTableSizeBytes / (double) mainTableSizeBytes * 100.0;
    }

    /**
     * Returns total TOAST size (table + index) in human-readable format.
     *
     * @return formatted size string
     */
    public String getTotalToastSizeDisplay() {
        long total = toastTableSizeBytes + toastIndexSizeBytes;
        return formatBytes(total);
    }

    /**
     * Returns estimated reclaimable space in human-readable format.
     *
     * @return formatted size string
     */
    public String getReclaimableDisplay() {
        return formatBytes(estimatedBloatBytes);
    }

    /**
     * Returns the main table size in human-readable format.
     *
     * @return formatted size string
     */
    public String getMainTableSizeDisplay() {
        return formatBytes(mainTableSizeBytes);
    }

    /**
     * Returns the TOAST table size in human-readable format.
     *
     * @return formatted size string
     */
    public String getToastTableSizeDisplay() {
        return formatBytes(toastTableSizeBytes);
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    /**
     * Returns severity level based on bloat percentage.
     *
     * @param warnThreshold warning threshold percentage
     * @return "critical", "warning", or "ok"
     */
    public String getSeverity(double warnThreshold) {
        if (bloatPercent >= warnThreshold * 2) return "critical";
        if (bloatPercent >= warnThreshold) return "warning";
        return "ok";
    }

    /**
     * Returns the CSS class for styling based on severity.
     *
     * @param warnThreshold warning threshold percentage
     * @return CSS class name
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
