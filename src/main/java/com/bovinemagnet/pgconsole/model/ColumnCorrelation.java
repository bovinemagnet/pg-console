package com.bovinemagnet.pgconsole.model;

/**
 * Represents column correlation statistics for CLUSTER recommendations.
 * <p>
 * Column correlation measures how well the physical order of rows matches
 * the logical order defined by an index. High correlation improves range
 * query performance. Low correlation suggests CLUSTER might help.
 * </p>
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class ColumnCorrelation {
    private String schemaName;
    private String tableName;
    private String columnName;
    private String indexName;
    private double correlation;
    private long nDistinct;
    private double nullFrac;
    private long tableSizeBytes;
    private long seqScan;
    private long idxScan;

    public ColumnCorrelation() {}

    public String getSchemaName() { return schemaName; }
    public void setSchemaName(String schemaName) { this.schemaName = schemaName; }

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }

    public String getColumnName() { return columnName; }
    public void setColumnName(String columnName) { this.columnName = columnName; }

    public String getIndexName() { return indexName; }
    public void setIndexName(String indexName) { this.indexName = indexName; }

    public double getCorrelation() { return correlation; }
    public void setCorrelation(double correlation) { this.correlation = correlation; }

    public long getnDistinct() { return nDistinct; }
    public void setnDistinct(long nDistinct) { this.nDistinct = nDistinct; }

    public double getNullFrac() { return nullFrac; }
    public void setNullFrac(double nullFrac) { this.nullFrac = nullFrac; }

    public long getTableSizeBytes() { return tableSizeBytes; }
    public void setTableSizeBytes(long tableSizeBytes) { this.tableSizeBytes = tableSizeBytes; }

    public long getSeqScan() { return seqScan; }
    public void setSeqScan(long seqScan) { this.seqScan = seqScan; }

    public long getIdxScan() { return idxScan; }
    public void setIdxScan(long idxScan) { this.idxScan = idxScan; }

    /**
     * Returns the fully qualified table name.
     *
     * @return schema.table format
     */
    public String getFullTableName() {
        return schemaName + "." + tableName;
    }

    /**
     * Returns the absolute correlation value.
     * <p>
     * Correlation ranges from -1 to 1. We use absolute value because
     * both perfectly ordered and perfectly reverse-ordered are good.
     * </p>
     *
     * @return absolute correlation (0-1)
     */
    public double getAbsCorrelation() {
        return Math.abs(correlation);
    }

    /**
     * Returns the correlation as a percentage.
     *
     * @return correlation percentage (0-100)
     */
    public double getCorrelationPercent() {
        return getAbsCorrelation() * 100.0;
    }

    /**
     * Returns whether CLUSTER would likely benefit this table.
     * <p>
     * CLUSTER is beneficial when:
     * - Correlation is low (< 0.5)
     * - Table has significant size (> 10MB)
     * - Table is used for range queries (index scans > 0)
     * </p>
     *
     * @return true if CLUSTER is recommended
     */
    public boolean isClusterRecommended() {
        return getAbsCorrelation() < 0.5
                && tableSizeBytes > 10 * 1024 * 1024
                && idxScan > 100;
    }

    /**
     * Returns the potential benefit level from CLUSTER.
     *
     * @return "high", "medium", "low", or "none"
     */
    public String getClusterBenefit() {
        if (!isClusterRecommended()) return "none";

        double absCorr = getAbsCorrelation();
        if (absCorr < 0.1) return "high";
        if (absCorr < 0.3) return "medium";
        return "low";
    }

    /**
     * Returns the CSS class for styling based on correlation.
     *
     * @return CSS class name
     */
    public String getCorrelationClass() {
        double absCorr = getAbsCorrelation();
        if (absCorr >= 0.9) return "text-success";
        if (absCorr >= 0.5) return "text-warning";
        return "text-danger";
    }

    /**
     * Returns a human-readable description of the correlation status.
     *
     * @return status description
     */
    public String getCorrelationStatus() {
        double absCorr = getAbsCorrelation();
        if (absCorr >= 0.95) return "Excellent";
        if (absCorr >= 0.9) return "Good";
        if (absCorr >= 0.7) return "Fair";
        if (absCorr >= 0.5) return "Poor";
        return "Very Poor";
    }

    /**
     * Returns the CLUSTER command for this table and index.
     *
     * @return SQL CLUSTER command
     */
    public String getClusterCommand() {
        if (indexName == null || indexName.isEmpty()) {
            return String.format("-- No suitable index for CLUSTER on %s.%s", schemaName, tableName);
        }
        return String.format("CLUSTER %s.%s USING %s;", schemaName, tableName, indexName);
    }

    /**
     * Returns table size in human-readable format.
     *
     * @return formatted size string
     */
    public String getTableSizeDisplay() {
        if (tableSizeBytes < 1024) return tableSizeBytes + " B";
        if (tableSizeBytes < 1024 * 1024) return String.format("%.1f KB", tableSizeBytes / 1024.0);
        if (tableSizeBytes < 1024 * 1024 * 1024) return String.format("%.1f MB", tableSizeBytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", tableSizeBytes / (1024.0 * 1024.0 * 1024.0));
    }

    /**
     * Estimates the performance improvement from CLUSTER.
     * <p>
     * Range queries on well-correlated data can be 2-10x faster.
     * </p>
     *
     * @return estimated improvement factor
     */
    public double getEstimatedImprovement() {
        double absCorr = getAbsCorrelation();
        if (absCorr >= 0.9) return 1.0; // No improvement expected
        if (absCorr >= 0.5) return 1.5;
        if (absCorr >= 0.3) return 2.5;
        if (absCorr >= 0.1) return 5.0;
        return 10.0; // Very poorly correlated
    }
}
