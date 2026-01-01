package com.bovinemagnet.pgconsole.model;

import java.time.Duration;
import java.time.Instant;

/**
 * Represents pipeline/queue table risk metrics for monitoring event backlogs.
 * <p>
 * This model tracks queue tables (identified by naming patterns like *_queue, *_event)
 * and monitors for processing backlogs by analysing the oldest row age.
 * </p>
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class PipelineRisk {
    private String schemaName;
    private String tableName;
    private long rowCount;
    private long tableSizeBytes;
    private Instant oldestRowTimestamp;
    private String timestampColumn;
    private String patternMatched;

    public PipelineRisk() {}

    public String getSchemaName() { return schemaName; }
    public void setSchemaName(String schemaName) { this.schemaName = schemaName; }

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }

    public long getRowCount() { return rowCount; }
    public void setRowCount(long rowCount) { this.rowCount = rowCount; }

    public long getTableSizeBytes() { return tableSizeBytes; }
    public void setTableSizeBytes(long tableSizeBytes) { this.tableSizeBytes = tableSizeBytes; }

    public Instant getOldestRowTimestamp() { return oldestRowTimestamp; }
    public void setOldestRowTimestamp(Instant oldestRowTimestamp) { this.oldestRowTimestamp = oldestRowTimestamp; }

    public String getTimestampColumn() { return timestampColumn; }
    public void setTimestampColumn(String timestampColumn) { this.timestampColumn = timestampColumn; }

    public String getPatternMatched() { return patternMatched; }
    public void setPatternMatched(String patternMatched) { this.patternMatched = patternMatched; }

    /**
     * Returns the fully qualified table name.
     *
     * @return schema.table format
     */
    public String getFullTableName() {
        return schemaName + "." + tableName;
    }

    /**
     * Returns the age of the oldest row as a Duration.
     *
     * @return duration since oldest row, or null if no timestamp
     */
    public Duration getOldestRowAge() {
        if (oldestRowTimestamp == null) return null;
        return Duration.between(oldestRowTimestamp, Instant.now());
    }

    /**
     * Returns a human-readable display of the oldest row age.
     *
     * @return formatted age string like "5d 3h" or "2h 15m"
     */
    public String getOldestAgeDisplay() {
        Duration age = getOldestRowAge();
        if (age == null) return "N/A";

        long days = age.toDays();
        long hours = age.toHours() % 24;
        long minutes = age.toMinutes() % 60;

        if (days > 0) {
            return String.format("%dd %dh", days, hours);
        } else if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }

    /**
     * Returns the table size in human-readable format.
     *
     * @return formatted size like "1.5 GB" or "256 MB"
     */
    public String getTableSizeDisplay() {
        if (tableSizeBytes < 1024) return tableSizeBytes + " B";
        if (tableSizeBytes < 1024 * 1024) return String.format("%.1f KB", tableSizeBytes / 1024.0);
        if (tableSizeBytes < 1024 * 1024 * 1024) return String.format("%.1f MB", tableSizeBytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", tableSizeBytes / (1024.0 * 1024.0 * 1024.0));
    }

    /**
     * Returns the risk level based on oldest row age.
     *
     * @param staleHoursThreshold hours after which queue is considered stale
     * @return risk level: "critical", "warning", "ok", or "unknown"
     */
    public String getRiskLevel(int staleHoursThreshold) {
        Duration age = getOldestRowAge();
        if (age == null) return "unknown";

        long hours = age.toHours();
        if (hours >= staleHoursThreshold * 2) return "critical";
        if (hours >= staleHoursThreshold) return "warning";
        return "ok";
    }

    /**
     * Returns the CSS class for styling based on risk level.
     *
     * @param staleHoursThreshold hours threshold for staleness
     * @return CSS class name
     */
    public String getRiskClass(int staleHoursThreshold) {
        String level = getRiskLevel(staleHoursThreshold);
        return switch (level) {
            case "critical" -> "text-danger";
            case "warning" -> "text-warning";
            case "ok" -> "text-success";
            default -> "text-muted";
        };
    }
}
