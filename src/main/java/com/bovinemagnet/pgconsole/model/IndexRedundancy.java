package com.bovinemagnet.pgconsole.model;

import java.util.List;

/**
 * Represents index redundancy and analysis metrics for PostgreSQL database optimisation.
 * <p>
 * This model identifies various types of index inefficiencies including duplicate indexes,
 * overlapping indexes, missing foreign key indexes, and over-indexing scenarios. It provides
 * detailed metrics to help database administrators optimise index usage and reclaim wasted
 * storage space.
 * <p>
 * The class calculates index-to-table size ratios, tracks index scan statistics, and provides
 * actionable recommendations for index maintenance. This data is typically sourced from
 * PostgreSQL system catalogues including {@code pg_stat_user_indexes}, {@code pg_indexes},
 * and {@code pg_constraint}.
 * <p>
 * Example usage in detecting redundant indexes:
 * <pre>{@code
 * IndexRedundancy redundancy = new IndexRedundancy();
 * redundancy.setIndexName("idx_users_email");
 * redundancy.setRelatedIndexName("idx_users_email_name");
 * redundancy.setRedundancyType(RedundancyType.OVERLAPPING);
 * redundancy.setRecommendation("Consider dropping idx_users_email as it is covered by idx_users_email_name");
 * }</pre>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see com.bovinemagnet.pgconsole.service.PostgresService
 */
public class IndexRedundancy {

    /**
     * Enumeration of index redundancy and inefficiency types.
     * <p>
     * Each type represents a specific category of index problem that may warrant
     * database administrator attention, ranging from critical issues (duplicate indexes)
     * to optimisation opportunities (unused indexes).
     * </p>
     */
    public enum RedundancyType {
        /**
         * Exact duplicate index with identical columns in the same order.
         * <p>
         * This represents two or more indexes on the same table with exactly the same
         * column definitions. One of these indexes should typically be dropped.
         * </p>
         * Severity: High
         */
        DUPLICATE,

        /**
         * Overlapping index where one index's columns are a prefix of another.
         * <p>
         * For example, an index on (a) is redundant if an index on (a, b) exists,
         * since the multi-column index can satisfy queries on column 'a' alone.
         * However, the narrower index may still be useful for performance in some cases.
         * </p>
         * Severity: Medium
         */
        OVERLAPPING,

        /**
         * Missing index on a foreign key column.
         * <p>
         * Foreign key columns without indexes can cause performance problems during
         * DELETE and UPDATE operations on the referenced table, as PostgreSQL must
         * scan the referencing table to check for constraint violations.
         * </p>
         * Severity: Medium
         */
        MISSING_FK,

        /**
         * Index exists but has never been used according to statistics.
         * <p>
         * An index with zero index scans may be a candidate for removal, though
         * consider the age of statistics and whether the index serves a future need.
         * </p>
         * Severity: Low
         */
        UNUSED,

        /**
         * Table has an excessive index-to-data size ratio.
         * <p>
         * When total index size significantly exceeds table size, it may indicate
         * over-indexing which can slow down write operations and waste storage.
         * </p>
         * Severity: Low
         */
        OVER_INDEXED
    }

    /** The schema name containing the table and index. */
    private String schemaName;

    /** The table name that owns the index. */
    private String tableName;

    /** The name of the index being analysed for redundancy. */
    private String indexName;

    /**
     * The name of the related index causing redundancy.
     * <p>
     * For {@link RedundancyType#DUPLICATE} and {@link RedundancyType#OVERLAPPING} types,
     * this indicates the other index involved in the redundancy relationship.
     * May be null for {@link RedundancyType#MISSING_FK}, {@link RedundancyType#UNUSED},
     * and {@link RedundancyType#OVER_INDEXED} types.
     * </p>
     */
    private String relatedIndexName;

    /** The type of redundancy or inefficiency detected. */
    private RedundancyType redundancyType;

    /**
     * The ordered list of column names that comprise the index.
     * <p>
     * Column order is significant for multi-column indexes in PostgreSQL.
     * </p>
     */
    private List<String> indexColumns;

    /**
     * The ordered list of column names for the related index.
     * <p>
     * Used for comparing index structures in duplicate and overlapping scenarios.
     * May be null if no related index exists.
     * </p>
     */
    private List<String> relatedIndexColumns;

    /** The disk space consumed by the index in bytes. */
    private long indexSizeBytes;

    /** The disk space consumed by the table (excluding indexes) in bytes. */
    private long tableSizeBytes;

    /**
     * The number of index scans performed using this index.
     * <p>
     * Sourced from {@code pg_stat_user_indexes.idx_scan}. A value of zero
     * indicates the index has never been used by the query planner.
     * </p>
     */
    private long indexScans;

    /**
     * The name of the foreign key constraint, if this redundancy relates to a missing FK index.
     * <p>
     * Only populated for {@link RedundancyType#MISSING_FK} types.
     * </p>
     */
    private String fkConstraintName;

    /**
     * The name of the table referenced by the foreign key constraint.
     * <p>
     * Only populated for {@link RedundancyType#MISSING_FK} types.
     * </p>
     */
    private String fkReferencedTable;

    /**
     * Human-readable recommendation for addressing the redundancy.
     * <p>
     * Provides actionable guidance such as "Consider dropping this index" or
     * "Create index on foreign key column to improve DELETE performance".
     * </p>
     */
    private String recommendation;

    /**
     * Constructs an empty IndexRedundancy instance.
     * <p>
     * All fields are initialised to their default values and must be set
     * using the appropriate setter methods.
     * </p>
     */
    public IndexRedundancy() {}

    /**
     * Returns the schema name containing the table and index.
     *
     * @return the schema name, or null if not set
     */
    public String getSchemaName() { return schemaName; }

    /**
     * Sets the schema name containing the table and index.
     *
     * @param schemaName the schema name
     */
    public void setSchemaName(String schemaName) { this.schemaName = schemaName; }

    /**
     * Returns the table name that owns the index.
     *
     * @return the table name, or null if not set
     */
    public String getTableName() { return tableName; }

    /**
     * Sets the table name that owns the index.
     *
     * @param tableName the table name
     */
    public void setTableName(String tableName) { this.tableName = tableName; }

    /**
     * Returns the name of the index being analysed for redundancy.
     *
     * @return the index name, or null if not set
     */
    public String getIndexName() { return indexName; }

    /**
     * Sets the name of the index being analysed for redundancy.
     *
     * @param indexName the index name
     */
    public void setIndexName(String indexName) { this.indexName = indexName; }

    /**
     * Returns the name of the related index causing redundancy.
     *
     * @return the related index name, or null if not applicable
     */
    public String getRelatedIndexName() { return relatedIndexName; }

    /**
     * Sets the name of the related index causing redundancy.
     *
     * @param relatedIndexName the related index name
     */
    public void setRelatedIndexName(String relatedIndexName) { this.relatedIndexName = relatedIndexName; }

    /**
     * Returns the type of redundancy or inefficiency detected.
     *
     * @return the redundancy type, or null if not set
     */
    public RedundancyType getRedundancyType() { return redundancyType; }

    /**
     * Sets the type of redundancy or inefficiency detected.
     *
     * @param redundancyType the redundancy type
     */
    public void setRedundancyType(RedundancyType redundancyType) { this.redundancyType = redundancyType; }

    /**
     * Returns the ordered list of column names that comprise the index.
     *
     * @return the list of index column names, or null if not set
     */
    public List<String> getIndexColumns() { return indexColumns; }

    /**
     * Sets the ordered list of column names that comprise the index.
     *
     * @param indexColumns the list of index column names
     */
    public void setIndexColumns(List<String> indexColumns) { this.indexColumns = indexColumns; }

    /**
     * Returns the ordered list of column names for the related index.
     *
     * @return the list of related index column names, or null if not applicable
     */
    public List<String> getRelatedIndexColumns() { return relatedIndexColumns; }

    /**
     * Sets the ordered list of column names for the related index.
     *
     * @param relatedIndexColumns the list of related index column names
     */
    public void setRelatedIndexColumns(List<String> relatedIndexColumns) { this.relatedIndexColumns = relatedIndexColumns; }

    /**
     * Returns the disk space consumed by the index in bytes.
     *
     * @return the index size in bytes
     */
    public long getIndexSizeBytes() { return indexSizeBytes; }

    /**
     * Sets the disk space consumed by the index in bytes.
     *
     * @param indexSizeBytes the index size in bytes
     */
    public void setIndexSizeBytes(long indexSizeBytes) { this.indexSizeBytes = indexSizeBytes; }

    /**
     * Returns the disk space consumed by the table (excluding indexes) in bytes.
     *
     * @return the table size in bytes
     */
    public long getTableSizeBytes() { return tableSizeBytes; }

    /**
     * Sets the disk space consumed by the table (excluding indexes) in bytes.
     *
     * @param tableSizeBytes the table size in bytes
     */
    public void setTableSizeBytes(long tableSizeBytes) { this.tableSizeBytes = tableSizeBytes; }

    /**
     * Returns the number of index scans performed using this index.
     *
     * @return the number of index scans
     */
    public long getIndexScans() { return indexScans; }

    /**
     * Sets the number of index scans performed using this index.
     *
     * @param indexScans the number of index scans
     */
    public void setIndexScans(long indexScans) { this.indexScans = indexScans; }

    /**
     * Returns the name of the foreign key constraint for missing FK index scenarios.
     *
     * @return the foreign key constraint name, or null if not applicable
     */
    public String getFkConstraintName() { return fkConstraintName; }

    /**
     * Sets the name of the foreign key constraint for missing FK index scenarios.
     *
     * @param fkConstraintName the foreign key constraint name
     */
    public void setFkConstraintName(String fkConstraintName) { this.fkConstraintName = fkConstraintName; }

    /**
     * Returns the name of the table referenced by the foreign key constraint.
     *
     * @return the referenced table name, or null if not applicable
     */
    public String getFkReferencedTable() { return fkReferencedTable; }

    /**
     * Sets the name of the table referenced by the foreign key constraint.
     *
     * @param fkReferencedTable the referenced table name
     */
    public void setFkReferencedTable(String fkReferencedTable) { this.fkReferencedTable = fkReferencedTable; }

    /**
     * Returns the human-readable recommendation for addressing the redundancy.
     *
     * @return the recommendation text, or null if not set
     */
    public String getRecommendation() { return recommendation; }

    /**
     * Sets the human-readable recommendation for addressing the redundancy.
     *
     * @param recommendation the recommendation text
     */
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }

    /**
     * Returns the fully qualified table name in schema.table format.
     * <p>
     * Combines the schema and table names with a period separator for
     * unambiguous table identification in PostgreSQL queries.
     * </p>
     *
     * @return the fully qualified table name, or "null.null" if schema or table name is not set
     */
    public String getFullTableName() {
        return schemaName + "." + tableName;
    }

    /**
     * Returns the index columns as a comma-separated string for display purposes.
     * <p>
     * Converts the list of column names into a readable format suitable for
     * presentation in UI tables or reports. Column order is preserved.
     * </p>
     *
     * @return comma-separated column names, or empty string if no columns are set
     */
    public String getIndexColumnsDisplay() {
        if (indexColumns == null || indexColumns.isEmpty()) return "";
        return String.join(", ", indexColumns);
    }

    /**
     * Returns the related index columns as a comma-separated string for display purposes.
     * <p>
     * Converts the list of related index column names into a readable format suitable
     * for comparing index structures in redundancy analysis.
     * </p>
     *
     * @return comma-separated column names, or empty string if no related columns are set
     */
    public String getRelatedIndexColumnsDisplay() {
        if (relatedIndexColumns == null || relatedIndexColumns.isEmpty()) return "";
        return String.join(", ", relatedIndexColumns);
    }

    /**
     * Calculates and returns the index-to-table size ratio as a percentage.
     * <p>
     * This metric helps identify over-indexing scenarios. A very high ratio
     * (e.g., greater than 100%) indicates that indexes consume more space than
     * the actual table data, which may impact write performance and storage costs.
     * </p>
     *
     * @return the percentage ratio of index size to table size, or 0.0 if table size is zero
     */
    public double getIndexRatio() {
        if (tableSizeBytes == 0) return 0.0;
        return (double) indexSizeBytes / (double) tableSizeBytes * 100.0;
    }

    /**
     * Returns the wasted space in human-readable format.
     * <p>
     * For redundant or unused indexes, this represents the disk space that could
     * be reclaimed by dropping the index. Format ranges from bytes to gigabytes
     * depending on size.
     * </p>
     *
     * @return formatted size string (e.g., "15.2 MB", "1.5 GB")
     */
    public String getWastedSpaceDisplay() {
        return formatBytes(indexSizeBytes);
    }

    /**
     * Returns the index size in human-readable format.
     * <p>
     * Converts the raw byte count into a readable format with appropriate
     * unit suffix (B, KB, MB, GB).
     * </p>
     *
     * @return formatted size string (e.g., "512 KB", "2.3 GB")
     */
    public String getIndexSizeDisplay() {
        return formatBytes(indexSizeBytes);
    }

    /**
     * Formats a byte count into a human-readable string with appropriate units.
     * <p>
     * Uses base-1024 conversion (binary prefix) and selects the most appropriate
     * unit to avoid excessive decimal places. Format precision varies by unit:
     * bytes (no decimals), KB/MB (1 decimal), GB (2 decimals).
     * </p>
     *
     * @param bytes the number of bytes to format
     * @return formatted string with unit suffix
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    /**
     * Returns a human-readable description of the redundancy type.
     * <p>
     * Converts the enum value into a user-friendly label suitable for
     * display in dashboards and reports.
     * </p>
     *
     * @return descriptive type label (e.g., "Duplicate Index", "Missing FK Index"),
     *         or "Unknown" if redundancy type is not set
     */
    public String getTypeDisplay() {
        if (redundancyType == null) return "Unknown";
        return switch (redundancyType) {
            case DUPLICATE -> "Duplicate Index";
            case OVERLAPPING -> "Overlapping Index";
            case MISSING_FK -> "Missing FK Index";
            case UNUSED -> "Unused Index";
            case OVER_INDEXED -> "Over-indexed Table";
        };
    }

    /**
     * Returns the Bootstrap CSS class for styling based on redundancy severity.
     * <p>
     * Provides colour coding for UI presentation:
     * </p>
     * <ul>
     * <li>DUPLICATE, OVERLAPPING: text-danger (red) - high priority issues</li>
     * <li>MISSING_FK, OVER_INDEXED: text-warning (amber) - medium priority</li>
     * <li>UNUSED: text-info (blue) - low priority optimisation opportunity</li>
     * <li>null: text-muted (grey) - unknown state</li>
     * </ul>
     *
     * @return Bootstrap 5 text colour class name
     */
    public String getTypeClass() {
        if (redundancyType == null) return "text-muted";
        return switch (redundancyType) {
            case DUPLICATE, OVERLAPPING -> "text-danger";
            case MISSING_FK -> "text-warning";
            case UNUSED -> "text-info";
            case OVER_INDEXED -> "text-warning";
        };
    }

    /**
     * Returns the severity level for prioritising remediation efforts.
     * <p>
     * Categorises redundancy types into three priority levels:
     * </p>
     * <ul>
     * <li>high: DUPLICATE - immediate attention recommended</li>
     * <li>medium: OVERLAPPING, MISSING_FK - should be addressed soon</li>
     * <li>low: UNUSED, OVER_INDEXED - optimisation opportunities</li>
     * </ul>
     *
     * @return severity level as "high", "medium", or "low"
     */
    public String getSeverity() {
        if (redundancyType == null) return "low";
        return switch (redundancyType) {
            case DUPLICATE -> "high";
            case OVERLAPPING, MISSING_FK -> "medium";
            case UNUSED, OVER_INDEXED -> "low";
        };
    }
}
