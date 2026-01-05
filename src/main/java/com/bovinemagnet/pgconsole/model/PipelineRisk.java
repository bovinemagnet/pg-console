package com.bovinemagnet.pgconsole.model;

import java.time.Duration;
import java.time.Instant;

/**
 * Represents pipeline/queue table risk metrics for monitoring event backlogs.
 * <p>
 * This model tracks queue tables (identified by naming patterns like *_queue, *_event)
 * and monitors for processing backlogs by analysing the oldest row age. Pipeline tables
 * typically contain events or work items that are processed asynchronously, and a growing
 * backlog or stale oldest row indicates potential processing issues.
 * </p>
 * <p>
 * Risk levels are calculated based on the age of the oldest unprocessed row:
 * <ul>
 *   <li><strong>OK</strong>: Age is below the stale threshold</li>
 *   <li><strong>WARNING</strong>: Age exceeds the stale threshold</li>
 *   <li><strong>CRITICAL</strong>: Age exceeds twice the stale threshold</li>
 *   <li><strong>UNKNOWN</strong>: No timestamp data available</li>
 * </ul>
 * </p>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see com.bovinemagnet.pgconsole.service.PostgresService
 */
public class PipelineRisk {
    /**
     * The PostgreSQL schema containing the pipeline table.
     */
    private String schemaName;

    /**
     * The name of the pipeline/queue table.
     */
    private String tableName;

    /**
     * The current number of rows in the pipeline table.
     */
    private long rowCount;

    /**
     * The total size of the table in bytes.
     */
    private long tableSizeBytes;

    /**
     * The timestamp of the oldest row in the pipeline table, indicating potential backlog age.
     */
    private Instant oldestRowTimestamp;

    /**
     * The name of the timestamp column used to determine row age (e.g., "created_at", "queued_at").
     */
    private String timestampColumn;

    /**
     * The naming pattern that identified this table as a pipeline/queue table (e.g., "*_queue", "*_event").
     */
    private String patternMatched;

    /**
     * Constructs a new PipelineRisk instance with default values.
     */
    public PipelineRisk() {}

    /**
     * Returns the PostgreSQL schema name containing the pipeline table.
     *
     * @return the schema name
     */
    public String getSchemaName() { return schemaName; }

    /**
     * Sets the PostgreSQL schema name containing the pipeline table.
     *
     * @param schemaName the schema name to set
     */
    public void setSchemaName(String schemaName) { this.schemaName = schemaName; }

    /**
     * Returns the pipeline/queue table name.
     *
     * @return the table name
     */
    public String getTableName() { return tableName; }

    /**
     * Sets the pipeline/queue table name.
     *
     * @param tableName the table name to set
     */
    public void setTableName(String tableName) { this.tableName = tableName; }

    /**
     * Returns the current number of rows in the pipeline table.
     * <p>
     * A growing row count may indicate backlog accumulation.
     * </p>
     *
     * @return the row count
     */
    public long getRowCount() { return rowCount; }

    /**
     * Sets the current number of rows in the pipeline table.
     *
     * @param rowCount the row count to set
     */
    public void setRowCount(long rowCount) { this.rowCount = rowCount; }

    /**
     * Returns the total size of the table in bytes.
     *
     * @return the table size in bytes
     */
    public long getTableSizeBytes() { return tableSizeBytes; }

    /**
     * Sets the total size of the table in bytes.
     *
     * @param tableSizeBytes the table size in bytes to set
     */
    public void setTableSizeBytes(long tableSizeBytes) { this.tableSizeBytes = tableSizeBytes; }

    /**
     * Returns the timestamp of the oldest row in the pipeline table.
     * <p>
     * A timestamp far in the past indicates stale unprocessed events.
     * May be null if no timestamp column was found or the table is empty.
     * </p>
     *
     * @return the oldest row timestamp, or null if not available
     */
    public Instant getOldestRowTimestamp() { return oldestRowTimestamp; }

    /**
     * Sets the timestamp of the oldest row in the pipeline table.
     *
     * @param oldestRowTimestamp the oldest row timestamp to set, or null if not available
     */
    public void setOldestRowTimestamp(Instant oldestRowTimestamp) { this.oldestRowTimestamp = oldestRowTimestamp; }

    /**
     * Returns the name of the timestamp column used to determine row age.
     * <p>
     * Common column names include "created_at", "queued_at", "inserted_at", or "timestamp".
     * </p>
     *
     * @return the timestamp column name, or null if no suitable column was found
     */
    public String getTimestampColumn() { return timestampColumn; }

    /**
     * Sets the name of the timestamp column used to determine row age.
     *
     * @param timestampColumn the timestamp column name to set
     */
    public void setTimestampColumn(String timestampColumn) { this.timestampColumn = timestampColumn; }

    /**
     * Returns the naming pattern that identified this table as a pipeline/queue table.
     * <p>
     * Examples include "*_queue", "*_event", "*_job", etc.
     * </p>
     *
     * @return the pattern that matched this table
     */
    public String getPatternMatched() { return patternMatched; }

    /**
     * Sets the naming pattern that identified this table as a pipeline/queue table.
     *
     * @param patternMatched the pattern to set
     */
    public void setPatternMatched(String patternMatched) { this.patternMatched = patternMatched; }

    /**
     * Returns the fully qualified table name in PostgreSQL format.
     * <p>
     * Combines the schema name and table name in the format "schema.table",
     * which is useful for display purposes and SQL query construction.
     * </p>
     *
     * @return the fully qualified table name in "schema.table" format
     */
    public String getFullTableName() {
        return schemaName + "." + tableName;
    }

    /**
     * Calculates the age of the oldest row as a Duration.
     * <p>
     * Computes the duration between the oldest row's timestamp and the current time.
     * This value indicates how long the oldest event has been waiting in the pipeline.
     * </p>
     *
     * @return the duration since the oldest row was created, or null if no timestamp is available
     */
    public Duration getOldestRowAge() {
        if (oldestRowTimestamp == null) return null;
        return Duration.between(oldestRowTimestamp, Instant.now());
    }

    /**
     * Returns a human-readable display of the oldest row age.
     * <p>
     * Formats the age in a compact, user-friendly format:
     * <ul>
     *   <li>If age includes days: "5d 3h"</li>
     *   <li>If age is hours/minutes: "2h 15m"</li>
     *   <li>If age is less than an hour: "42m"</li>
     *   <li>If no timestamp available: "N/A"</li>
     * </ul>
     * This method is useful for rendering age information in dashboard views.
     * </p>
     *
     * @return formatted age string like "5d 3h", "2h 15m", "42m", or "N/A"
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
     * <p>
     * Converts the byte count to the most appropriate unit (B, KB, MB, GB)
     * with appropriate decimal precision for easy readability.
     * </p>
     * <p>
     * Format examples:
     * <ul>
     *   <li>512 bytes: "512 B"</li>
     *   <li>2048 bytes: "2.0 KB"</li>
     *   <li>1048576 bytes: "1.0 MB"</li>
     *   <li>1073741824 bytes: "1.00 GB"</li>
     * </ul>
     * </p>
     *
     * @return formatted size string like "1.5 GB", "256 MB", "12.3 KB", or "512 B"
     */
    public String getTableSizeDisplay() {
        if (tableSizeBytes < 1024) return tableSizeBytes + " B";
        if (tableSizeBytes < 1024 * 1024) return String.format("%.1f KB", tableSizeBytes / 1024.0);
        if (tableSizeBytes < 1024 * 1024 * 1024) return String.format("%.1f MB", tableSizeBytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", tableSizeBytes / (1024.0 * 1024.0 * 1024.0));
    }

    /**
     * Calculates the risk level based on the oldest row's age relative to a staleness threshold.
     * <p>
     * The risk level helps identify pipeline backlogs that require attention:
     * <ul>
     *   <li><strong>ok</strong>: Age is below the threshold (pipeline processing normally)</li>
     *   <li><strong>warning</strong>: Age exceeds the threshold (pipeline may be slow)</li>
     *   <li><strong>critical</strong>: Age exceeds twice the threshold (pipeline significantly delayed)</li>
     *   <li><strong>unknown</strong>: No timestamp data available (unable to assess risk)</li>
     * </ul>
     * </p>
     *
     * @param staleHoursThreshold the number of hours after which a queue is considered stale
     * @return the risk level as a string: "critical", "warning", "ok", or "unknown"
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
     * Returns the Bootstrap CSS class for styling based on the calculated risk level.
     * <p>
     * This method maps risk levels to Bootstrap 5 text colour classes for visual indication:
     * <ul>
     *   <li><strong>critical</strong>: "text-danger" (red)</li>
     *   <li><strong>warning</strong>: "text-warning" (yellow/amber)</li>
     *   <li><strong>ok</strong>: "text-success" (green)</li>
     *   <li><strong>unknown</strong>: "text-muted" (grey)</li>
     * </ul>
     * These classes are applied in dashboard templates for at-a-glance risk assessment.
     * </p>
     *
     * @param staleHoursThreshold the number of hours threshold for staleness calculation
     * @return the Bootstrap CSS class name for text colouring
     * @see #getRiskLevel(int)
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
