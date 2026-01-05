package com.bovinemagnet.pgconsole.model;

/**
 * Represents column correlation statistics for CLUSTER recommendations.
 * <p>
 * Column correlation measures how well the physical order of rows matches
 * the logical order defined by an index. High correlation improves range
 * query performance. Low correlation suggests CLUSTER might help.
 * </p>
 * <p>
 * This model is populated from PostgreSQL's {@code pg_stats} system view
 * and table statistics views. Correlation values range from -1 to 1, where
 * values close to 1 or -1 indicate good correlation, and values near 0
 * indicate poor correlation between physical and logical ordering.
 * </p>
 * <p>
 * The class provides utility methods to determine if running {@code CLUSTER}
 * would be beneficial, estimate performance improvements, and generate the
 * appropriate SQL command.
 * </p>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see <a href="https://www.postgresql.org/docs/current/view-pg-stats.html">PostgreSQL pg_stats documentation</a>
 */
public class ColumnCorrelation {
    /**
     * The schema name containing the table.
     * Typically "public" in most databases.
     */
    private String schemaName;

    /**
     * The table name being analysed for correlation.
     */
    private String tableName;

    /**
     * The column name whose correlation is being measured.
     * Correlation is specific to individual columns.
     */
    private String columnName;

    /**
     * The index name that could be used for CLUSTER operation.
     * May be null if no suitable index exists.
     */
    private String indexName;

    /**
     * The correlation coefficient from pg_stats.
     * <p>
     * Values range from -1 to 1:
     * <ul>
     *   <li>1.0 = perfectly ordered (ascending)</li>
     *   <li>-1.0 = perfectly reverse ordered (descending)</li>
     *   <li>0.0 = no correlation (random order)</li>
     * </ul>
     * </p>
     */
    private double correlation;

    /**
     * The number of distinct values in the column (from pg_stats.n_distinct).
     * Negative values indicate the fraction of distinct values relative to total rows.
     */
    private long nDistinct;

    /**
     * The fraction of null values in the column (from pg_stats.null_frac).
     * Values range from 0.0 (no nulls) to 1.0 (all nulls).
     */
    private double nullFrac;

    /**
     * The total table size in bytes.
     * Used to determine if CLUSTER is worth the effort for large tables.
     */
    private long tableSizeBytes;

    /**
     * The number of sequential scans performed on this table.
     * High sequential scans may indicate missing indices.
     */
    private long seqScan;

    /**
     * The number of index scans performed on this table.
     * High index scans combined with low correlation suggest CLUSTER would help.
     */
    private long idxScan;

    /**
     * Constructs a new ColumnCorrelation with default values.
     */
    public ColumnCorrelation() {}

    /**
     * Returns the schema name containing the table.
     *
     * @return the schema name, never null
     */
    public String getSchemaName() { return schemaName; }

    /**
     * Sets the schema name containing the table.
     *
     * @param schemaName the schema name to set
     */
    public void setSchemaName(String schemaName) { this.schemaName = schemaName; }

    /**
     * Returns the table name being analysed.
     *
     * @return the table name, never null
     */
    public String getTableName() { return tableName; }

    /**
     * Sets the table name being analysed.
     *
     * @param tableName the table name to set
     */
    public void setTableName(String tableName) { this.tableName = tableName; }

    /**
     * Returns the column name whose correlation is being measured.
     *
     * @return the column name, never null
     */
    public String getColumnName() { return columnName; }

    /**
     * Sets the column name whose correlation is being measured.
     *
     * @param columnName the column name to set
     */
    public void setColumnName(String columnName) { this.columnName = columnName; }

    /**
     * Returns the index name that could be used for CLUSTER operation.
     *
     * @return the index name, or null if no suitable index exists
     */
    public String getIndexName() { return indexName; }

    /**
     * Sets the index name that could be used for CLUSTER operation.
     *
     * @param indexName the index name to set, may be null
     */
    public void setIndexName(String indexName) { this.indexName = indexName; }

    /**
     * Returns the correlation coefficient from pg_stats.
     * <p>
     * Values range from -1.0 (perfectly reverse ordered) through 0.0 (random)
     * to 1.0 (perfectly ordered).
     * </p>
     *
     * @return the correlation coefficient (-1.0 to 1.0)
     */
    public double getCorrelation() { return correlation; }

    /**
     * Sets the correlation coefficient from pg_stats.
     *
     * @param correlation the correlation coefficient to set (-1.0 to 1.0)
     */
    public void setCorrelation(double correlation) { this.correlation = correlation; }

    /**
     * Returns the number of distinct values in the column.
     * <p>
     * Negative values indicate the fraction of distinct values relative to
     * total rows (e.g., -0.5 means approximately half the rows are distinct).
     * </p>
     *
     * @return the number of distinct values, or negative for fraction
     */
    public long getnDistinct() { return nDistinct; }

    /**
     * Sets the number of distinct values in the column.
     *
     * @param nDistinct the number of distinct values, or negative for fraction
     */
    public void setnDistinct(long nDistinct) { this.nDistinct = nDistinct; }

    /**
     * Returns the fraction of null values in the column.
     *
     * @return the null fraction (0.0 to 1.0)
     */
    public double getNullFrac() { return nullFrac; }

    /**
     * Sets the fraction of null values in the column.
     *
     * @param nullFrac the null fraction to set (0.0 to 1.0)
     */
    public void setNullFrac(double nullFrac) { this.nullFrac = nullFrac; }

    /**
     * Returns the total table size in bytes.
     *
     * @return the table size in bytes
     */
    public long getTableSizeBytes() { return tableSizeBytes; }

    /**
     * Sets the total table size in bytes.
     *
     * @param tableSizeBytes the table size to set
     */
    public void setTableSizeBytes(long tableSizeBytes) { this.tableSizeBytes = tableSizeBytes; }

    /**
     * Returns the number of sequential scans performed on this table.
     *
     * @return the sequential scan count
     */
    public long getSeqScan() { return seqScan; }

    /**
     * Sets the number of sequential scans performed on this table.
     *
     * @param seqScan the sequential scan count to set
     */
    public void setSeqScan(long seqScan) { this.seqScan = seqScan; }

    /**
     * Returns the number of index scans performed on this table.
     *
     * @return the index scan count
     */
    public long getIdxScan() { return idxScan; }

    /**
     * Sets the number of index scans performed on this table.
     *
     * @param idxScan the index scan count to set
     */
    public void setIdxScan(long idxScan) { this.idxScan = idxScan; }

    /**
     * Returns the fully qualified table name in schema.table format.
     * <p>
     * This is useful for display purposes and for constructing SQL statements
     * that require explicit schema qualification.
     * </p>
     *
     * @return the fully qualified table name (e.g., "public.users")
     */
    public String getFullTableName() {
        return schemaName + "." + tableName;
    }

    /**
     * Returns the absolute correlation value.
     * <p>
     * Correlation ranges from -1 to 1. This method returns the absolute value
     * because both perfectly ordered (1.0) and perfectly reverse-ordered (-1.0)
     * indicate good correlation with the index. Only values near 0 indicate
     * poor correlation that would benefit from CLUSTER.
     * </p>
     *
     * @return the absolute correlation value (0.0 to 1.0)
     */
    public double getAbsCorrelation() {
        return Math.abs(correlation);
    }

    /**
     * Returns the absolute correlation value as a percentage.
     * <p>
     * This is a convenience method for display purposes, converting
     * the 0.0-1.0 absolute correlation to a 0-100 percentage scale.
     * </p>
     *
     * @return the correlation percentage (0.0 to 100.0)
     */
    public double getCorrelationPercent() {
        return getAbsCorrelation() * 100.0;
    }

    /**
     * Determines whether running CLUSTER would likely benefit this table.
     * <p>
     * CLUSTER is recommended when all of the following conditions are met:
     * <ul>
     *   <li>Absolute correlation is low (less than 0.5)</li>
     *   <li>Table has significant size (greater than 10 MB)</li>
     *   <li>Table is actively used for index scans (more than 100)</li>
     * </ul>
     * Small tables or tables not using indices would not benefit enough
     * to justify the cost of clustering.
     * </p>
     *
     * @return {@code true} if CLUSTER is recommended, {@code false} otherwise
     */
    public boolean isClusterRecommended() {
        return getAbsCorrelation() < 0.5
                && tableSizeBytes > 10 * 1024 * 1024
                && idxScan > 100;
    }

    /**
     * Determines the potential benefit level from running CLUSTER.
     * <p>
     * Benefit levels are determined by absolute correlation:
     * <ul>
     *   <li>"high" - correlation less than 0.1 (nearly random ordering)</li>
     *   <li>"medium" - correlation between 0.1 and 0.3</li>
     *   <li>"low" - correlation between 0.3 and 0.5</li>
     *   <li>"none" - correlation 0.5 or higher, or CLUSTER not recommended</li>
     * </ul>
     * </p>
     *
     * @return the benefit level: "high", "medium", "low", or "none"
     * @see #isClusterRecommended()
     */
    public String getClusterBenefit() {
        if (!isClusterRecommended()) return "none";

        double absCorr = getAbsCorrelation();
        if (absCorr < 0.1) return "high";
        if (absCorr < 0.3) return "medium";
        return "low";
    }

    /**
     * Returns the Bootstrap CSS class for styling correlation values.
     * <p>
     * Maps correlation quality to visual indicators:
     * <ul>
     *   <li>"text-success" (green) - excellent correlation (0.9 or higher)</li>
     *   <li>"text-warning" (yellow) - fair correlation (0.5 to 0.9)</li>
     *   <li>"text-danger" (red) - poor correlation (below 0.5)</li>
     * </ul>
     * </p>
     *
     * @return the Bootstrap CSS class name for styling
     */
    public String getCorrelationClass() {
        double absCorr = getAbsCorrelation();
        if (absCorr >= 0.9) return "text-success";
        if (absCorr >= 0.5) return "text-warning";
        return "text-danger";
    }

    /**
     * Returns a human-readable description of the correlation status.
     * <p>
     * Status levels based on absolute correlation:
     * <ul>
     *   <li>"Excellent" - 0.95 or higher</li>
     *   <li>"Good" - 0.9 to 0.95</li>
     *   <li>"Fair" - 0.7 to 0.9</li>
     *   <li>"Poor" - 0.5 to 0.7</li>
     *   <li>"Very Poor" - below 0.5</li>
     * </ul>
     * </p>
     *
     * @return a descriptive status string
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
     * Generates the SQL CLUSTER command for this table and index.
     * <p>
     * Returns a ready-to-execute SQL statement to cluster the table
     * using the associated index. If no index is available, returns
     * a commented placeholder instead.
     * </p>
     * <p>
     * Example output: {@code CLUSTER public.users USING users_created_at_idx;}
     * </p>
     *
     * @return the SQL CLUSTER command, or a comment if no suitable index exists
     */
    public String getClusterCommand() {
        if (indexName == null || indexName.isEmpty()) {
            return String.format("-- No suitable index for CLUSTER on %s.%s", schemaName, tableName);
        }
        return String.format("CLUSTER %s.%s USING %s;", schemaName, tableName, indexName);
    }

    /**
     * Formats the table size in human-readable units.
     * <p>
     * Converts bytes to appropriate units (B, KB, MB, GB) with
     * sensible precision for display purposes.
     * </p>
     * <p>
     * Examples:
     * <ul>
     *   <li>512 bytes → "512 B"</li>
     *   <li>5120 bytes → "5.0 KB"</li>
     *   <li>5242880 bytes → "5.0 MB"</li>
     *   <li>5368709120 bytes → "5.00 GB"</li>
     * </ul>
     * </p>
     *
     * @return the formatted size string with appropriate units
     */
    public String getTableSizeDisplay() {
        if (tableSizeBytes < 1024) return tableSizeBytes + " B";
        if (tableSizeBytes < 1024 * 1024) return String.format("%.1f KB", tableSizeBytes / 1024.0);
        if (tableSizeBytes < 1024 * 1024 * 1024) return String.format("%.1f MB", tableSizeBytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", tableSizeBytes / (1024.0 * 1024.0 * 1024.0));
    }

    /**
     * Estimates the potential performance improvement from running CLUSTER.
     * <p>
     * Range queries on well-correlated data can be 2-10x faster because
     * PostgreSQL can read consecutive pages from disk rather than seeking
     * randomly. The improvement factor is based on the current correlation:
     * <ul>
     *   <li>1.0x - correlation 0.9 or higher (already well-ordered)</li>
     *   <li>1.5x - correlation 0.5 to 0.9 (moderate improvement)</li>
     *   <li>2.5x - correlation 0.3 to 0.5 (significant improvement)</li>
     *   <li>5.0x - correlation 0.1 to 0.3 (major improvement)</li>
     *   <li>10.0x - correlation below 0.1 (dramatic improvement possible)</li>
     * </ul>
     * These are estimates; actual improvement depends on workload patterns.
     * </p>
     *
     * @return the estimated improvement factor (1.0 = no improvement expected)
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
