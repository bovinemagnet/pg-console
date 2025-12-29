package com.bovinemagnet.pgconsole.model;

import java.util.List;

/**
 * Represents index redundancy and analysis metrics.
 * <p>
 * This model identifies duplicate/overlapping indexes, missing foreign key indexes,
 * and calculates index-to-table size ratios to detect over-indexing.
 * </p>
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class IndexRedundancy {

    /**
     * Type of redundancy issue detected.
     */
    public enum RedundancyType {
        DUPLICATE,          // Exact duplicate index
        OVERLAPPING,        // Index columns are a prefix of another
        MISSING_FK,         // Missing index on foreign key column
        UNUSED,             // Index exists but never used
        OVER_INDEXED        // Table has excessive index-to-data ratio
    }

    private String schemaName;
    private String tableName;
    private String indexName;
    private String relatedIndexName;
    private RedundancyType redundancyType;
    private List<String> indexColumns;
    private List<String> relatedIndexColumns;
    private long indexSizeBytes;
    private long tableSizeBytes;
    private long indexScans;
    private String fkConstraintName;
    private String fkReferencedTable;
    private String recommendation;

    public IndexRedundancy() {}

    public String getSchemaName() { return schemaName; }
    public void setSchemaName(String schemaName) { this.schemaName = schemaName; }

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }

    public String getIndexName() { return indexName; }
    public void setIndexName(String indexName) { this.indexName = indexName; }

    public String getRelatedIndexName() { return relatedIndexName; }
    public void setRelatedIndexName(String relatedIndexName) { this.relatedIndexName = relatedIndexName; }

    public RedundancyType getRedundancyType() { return redundancyType; }
    public void setRedundancyType(RedundancyType redundancyType) { this.redundancyType = redundancyType; }

    public List<String> getIndexColumns() { return indexColumns; }
    public void setIndexColumns(List<String> indexColumns) { this.indexColumns = indexColumns; }

    public List<String> getRelatedIndexColumns() { return relatedIndexColumns; }
    public void setRelatedIndexColumns(List<String> relatedIndexColumns) { this.relatedIndexColumns = relatedIndexColumns; }

    public long getIndexSizeBytes() { return indexSizeBytes; }
    public void setIndexSizeBytes(long indexSizeBytes) { this.indexSizeBytes = indexSizeBytes; }

    public long getTableSizeBytes() { return tableSizeBytes; }
    public void setTableSizeBytes(long tableSizeBytes) { this.tableSizeBytes = tableSizeBytes; }

    public long getIndexScans() { return indexScans; }
    public void setIndexScans(long indexScans) { this.indexScans = indexScans; }

    public String getFkConstraintName() { return fkConstraintName; }
    public void setFkConstraintName(String fkConstraintName) { this.fkConstraintName = fkConstraintName; }

    public String getFkReferencedTable() { return fkReferencedTable; }
    public void setFkReferencedTable(String fkReferencedTable) { this.fkReferencedTable = fkReferencedTable; }

    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }

    /**
     * Returns the fully qualified table name.
     *
     * @return schema.table format
     */
    public String getFullTableName() {
        return schemaName + "." + tableName;
    }

    /**
     * Returns the index columns as a comma-separated string.
     *
     * @return column list string
     */
    public String getIndexColumnsDisplay() {
        if (indexColumns == null || indexColumns.isEmpty()) return "";
        return String.join(", ", indexColumns);
    }

    /**
     * Returns the related index columns as a comma-separated string.
     *
     * @return column list string
     */
    public String getRelatedIndexColumnsDisplay() {
        if (relatedIndexColumns == null || relatedIndexColumns.isEmpty()) return "";
        return String.join(", ", relatedIndexColumns);
    }

    /**
     * Returns the index-to-table size ratio as a percentage.
     *
     * @return ratio percentage
     */
    public double getIndexRatio() {
        if (tableSizeBytes == 0) return 0.0;
        return (double) indexSizeBytes / (double) tableSizeBytes * 100.0;
    }

    /**
     * Returns the wasted space in human-readable format.
     *
     * @return formatted size string
     */
    public String getWastedSpaceDisplay() {
        return formatBytes(indexSizeBytes);
    }

    /**
     * Returns the index size in human-readable format.
     *
     * @return formatted size string
     */
    public String getIndexSizeDisplay() {
        return formatBytes(indexSizeBytes);
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    /**
     * Returns a human-readable description of the redundancy type.
     *
     * @return type description
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
     * Returns the CSS class for styling based on redundancy type.
     *
     * @return CSS class name
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
     * Returns the severity level.
     *
     * @return "high", "medium", or "low"
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
