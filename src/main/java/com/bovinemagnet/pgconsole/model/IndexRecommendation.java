package com.bovinemagnet.pgconsole.model;

/**
 * Represents an index recommendation or identified index issue within a PostgreSQL database.
 * <p>
 * This model is used by the Index Adviser to analyse table and index statistics and provide
 * actionable recommendations for improving database performance. Recommendations can range from
 * creating missing indices to removing unused or duplicate indices, or rebuilding bloated indices.
 * <p>
 * Each recommendation includes:
 * <ul>
 *   <li>A {@link RecommendationType} indicating the category of issue</li>
 *   <li>A {@link Severity} level (HIGH, MEDIUM, LOW) to prioritise actions</li>
 *   <li>Contextual information (schema, table, index names)</li>
 *   <li>A human-readable recommendation, rationale, and suggested action</li>
 *   <li>Supporting statistics (scan counts, table size, dead tuples, etc.)</li>
 * </ul>
 * <p>
 * This class also provides convenience methods for formatting data for display in dashboard
 * templates, including Bootstrap CSS classes for severity and type badges.
 *
 * @author Paul Snow
 * @version 0.0.0
 * @since 0.0.0
 * @see com.bovinemagnet.pgconsole.service.PostgresService
 */
public class IndexRecommendation {

    /**
     * Categorises the type of index recommendation or issue.
     * <p>
     * Each type represents a distinct pattern identified through analysis of PostgreSQL
     * statistics views such as {@code pg_stat_user_tables}, {@code pg_stat_user_indexes},
     * and {@code pg_statio_user_indexes}.
     */
    public enum RecommendationType {
        /**
         * Indicates a table that would benefit from an index based on sequential scan patterns.
         * Typically identified when a table has high sequential scan counts relative to index scans.
         */
        MISSING_INDEX,

        /**
         * Indicates an existing index that has not been used (zero index scans).
         * Unused indices consume storage and impose maintenance overhead during writes without
         * providing query performance benefits.
         */
        UNUSED_INDEX,

        /**
         * Indicates an index that is redundant because it is covered by another index.
         * For example, an index on {@code (a)} is redundant if an index on {@code (a, b)} exists,
         * as the latter can serve the same queries.
         */
        DUPLICATE_INDEX,

        /**
         * Indicates an index on a column with low cardinality (few distinct values).
         * Low-cardinality indices may not provide sufficient selectivity to improve query performance
         * and can waste storage and maintenance resources.
         */
        LOW_CARDINALITY,

        /**
         * Indicates an index that has become bloated and would benefit from rebuilding.
         * Index bloat occurs when indices accumulate dead tuples or unused space, leading to
         * degraded performance and increased storage usage.
         */
        BLOATED_INDEX
    }

    /**
     * Categorises the urgency or priority of addressing the recommendation.
     * <p>
     * Severity levels help database administrators prioritise which recommendations to
     * implement first, focusing on issues with the greatest potential performance impact.
     */
    public enum Severity {
        /**
         * High-severity issues that should be addressed soon.
         * These typically have significant performance or storage implications.
         */
        HIGH,

        /**
         * Medium-severity issues worth investigating.
         * These may have moderate impact or require further analysis before action.
         */
        MEDIUM,

        /**
         * Low-severity issues that are informational.
         * These may be minor optimisations or observations with limited impact.
         */
        LOW
    }

    /**
     * The category of index recommendation or issue.
     * Never null for valid recommendations.
     */
    private RecommendationType type;

    /**
     * The severity level indicating the priority of addressing this recommendation.
     * Never null for valid recommendations.
     */
    private Severity severity;

    /**
     * The schema name where the table or index resides.
     * May be null if the recommendation is not schema-specific.
     */
    private String schemaName;

    /**
     * The table name associated with this recommendation.
     * Never null for valid recommendations.
     */
    private String tableName;

    /**
     * The index name, if the recommendation relates to an existing index.
     * Null for {@link RecommendationType#MISSING_INDEX} recommendations.
     */
    private String indexName;

    /**
     * A concise, human-readable summary of the recommendation.
     * For example: "Create an index on customer_id column".
     */
    private String recommendation;

    /**
     * A detailed explanation of why this recommendation is being made.
     * Includes relevant statistics and observations that support the recommendation.
     */
    private String rationale;

    /**
     * A specific SQL statement or action the administrator can take to implement the recommendation.
     * For example: "CREATE INDEX idx_customer_id ON orders(customer_id);".
     */
    private String suggestedAction;

    /**
     * The number of sequential scans performed on the table.
     * Retrieved from {@code pg_stat_user_tables.seq_scan}.
     */
    private long seqScans;

    /**
     * The number of index scans performed on the index (if applicable).
     * Retrieved from {@code pg_stat_user_indexes.idx_scan}.
     */
    private long idxScans;

    /**
     * The ratio of sequential scans to total scans (sequential + index), expressed as a percentage.
     * Values closer to 100% indicate heavy reliance on sequential scans, suggesting a missing index.
     */
    private double seqScanRatio;

    /**
     * The estimated number of live rows in the table.
     * Retrieved from {@code pg_stat_user_tables.n_live_tup}.
     */
    private long tableRows;

    /**
     * A human-readable representation of the table size.
     * For example: "42 MB", obtained via {@code pg_size_pretty(pg_total_relation_size())}.
     */
    private String tableSize;

    /**
     * A human-readable representation of the index size (if applicable).
     * For example: "8192 bytes", obtained via {@code pg_size_pretty(pg_relation_size())}.
     */
    private String indexSize;

    /**
     * The number of dead tuples in the table.
     * Retrieved from {@code pg_stat_user_tables.n_dead_tup}.
     * High values may indicate the need for vacuum or index rebuilding.
     */
    private long deadTuples;

    /**
     * Constructs an empty IndexRecommendation.
     * All fields will be null or zero until explicitly set.
     */
    public IndexRecommendation() {
    }

    /**
     * Constructs an IndexRecommendation with the specified type and severity.
     * <p>
     * This constructor is useful for creating recommendations where type and severity
     * are known upfront, with other fields to be populated subsequently.
     *
     * @param type the category of index recommendation or issue
     * @param severity the severity level of the recommendation
     */
    public IndexRecommendation(RecommendationType type, Severity severity) {
        this.type = type;
        this.severity = severity;
    }

    /**
     * Returns the category of index recommendation or issue.
     *
     * @return the recommendation type, or null if not set
     */
    public RecommendationType getType() {
        return type;
    }

    /**
     * Sets the category of index recommendation or issue.
     *
     * @param type the recommendation type to set
     */
    public void setType(RecommendationType type) {
        this.type = type;
    }

    /**
     * Returns the severity level of the recommendation.
     *
     * @return the severity, or null if not set
     */
    public Severity getSeverity() {
        return severity;
    }

    /**
     * Sets the severity level of the recommendation.
     *
     * @param severity the severity to set
     */
    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    /**
     * Returns the schema name where the table or index resides.
     *
     * @return the schema name, or null if not applicable
     */
    public String getSchemaName() {
        return schemaName;
    }

    /**
     * Sets the schema name where the table or index resides.
     *
     * @param schemaName the schema name to set
     */
    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    /**
     * Returns the table name associated with this recommendation.
     *
     * @return the table name, or null if not set
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Sets the table name associated with this recommendation.
     *
     * @param tableName the table name to set
     */
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    /**
     * Returns the index name, if the recommendation relates to an existing index.
     *
     * @return the index name, or null for missing index recommendations
     */
    public String getIndexName() {
        return indexName;
    }

    /**
     * Sets the index name for recommendations relating to existing indices.
     *
     * @param indexName the index name to set
     */
    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    /**
     * Returns a concise, human-readable summary of the recommendation.
     *
     * @return the recommendation summary, or null if not set
     */
    public String getRecommendation() {
        return recommendation;
    }

    /**
     * Sets the concise summary of the recommendation.
     *
     * @param recommendation the recommendation summary to set
     */
    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    /**
     * Returns a detailed explanation of why this recommendation is being made.
     *
     * @return the rationale, or null if not set
     */
    public String getRationale() {
        return rationale;
    }

    /**
     * Sets the detailed explanation supporting the recommendation.
     *
     * @param rationale the rationale to set
     */
    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    /**
     * Returns the specific action the administrator can take to implement the recommendation.
     *
     * @return the suggested action (often a SQL statement), or null if not set
     */
    public String getSuggestedAction() {
        return suggestedAction;
    }

    /**
     * Sets the specific action to implement the recommendation.
     *
     * @param suggestedAction the suggested action to set (e.g., CREATE INDEX statement)
     */
    public void setSuggestedAction(String suggestedAction) {
        this.suggestedAction = suggestedAction;
    }

    /**
     * Returns the number of sequential scans performed on the table.
     *
     * @return the sequential scan count
     */
    public long getSeqScans() {
        return seqScans;
    }

    /**
     * Sets the number of sequential scans performed on the table.
     *
     * @param seqScans the sequential scan count to set
     */
    public void setSeqScans(long seqScans) {
        this.seqScans = seqScans;
    }

    /**
     * Returns the number of index scans performed on the index.
     *
     * @return the index scan count
     */
    public long getIdxScans() {
        return idxScans;
    }

    /**
     * Sets the number of index scans performed on the index.
     *
     * @param idxScans the index scan count to set
     */
    public void setIdxScans(long idxScans) {
        this.idxScans = idxScans;
    }

    /**
     * Returns the ratio of sequential scans to total scans, as a percentage.
     * <p>
     * Values closer to 100% indicate heavy reliance on sequential scans,
     * suggesting potential benefit from adding an index.
     *
     * @return the sequential scan ratio as a percentage (0.0 to 100.0)
     */
    public double getSeqScanRatio() {
        return seqScanRatio;
    }

    /**
     * Sets the ratio of sequential scans to total scans.
     *
     * @param seqScanRatio the sequential scan ratio to set (0.0 to 100.0)
     */
    public void setSeqScanRatio(double seqScanRatio) {
        this.seqScanRatio = seqScanRatio;
    }

    /**
     * Returns the estimated number of live rows in the table.
     *
     * @return the table row count
     */
    public long getTableRows() {
        return tableRows;
    }

    /**
     * Sets the estimated number of live rows in the table.
     *
     * @param tableRows the table row count to set
     */
    public void setTableRows(long tableRows) {
        this.tableRows = tableRows;
    }

    /**
     * Returns a human-readable representation of the table size.
     *
     * @return the table size (e.g., "42 MB"), or null if not set
     */
    public String getTableSize() {
        return tableSize;
    }

    /**
     * Sets a human-readable representation of the table size.
     *
     * @param tableSize the table size to set (e.g., "42 MB")
     */
    public void setTableSize(String tableSize) {
        this.tableSize = tableSize;
    }

    /**
     * Returns a human-readable representation of the index size.
     *
     * @return the index size (e.g., "8192 bytes"), or null if not applicable
     */
    public String getIndexSize() {
        return indexSize;
    }

    /**
     * Sets a human-readable representation of the index size.
     *
     * @param indexSize the index size to set (e.g., "8192 bytes")
     */
    public void setIndexSize(String indexSize) {
        this.indexSize = indexSize;
    }

    /**
     * Returns the number of dead tuples in the table.
     * <p>
     * High values may indicate the need for VACUUM or index rebuilding.
     *
     * @return the dead tuple count
     */
    public long getDeadTuples() {
        return deadTuples;
    }

    /**
     * Sets the number of dead tuples in the table.
     *
     * @param deadTuples the dead tuple count to set
     */
    public void setDeadTuples(long deadTuples) {
        this.deadTuples = deadTuples;
    }

    /**
     * Returns the fully qualified table name in the format {@code schema.table}.
     * <p>
     * If the schema name is null or empty, returns only the table name.
     * This method is useful for display purposes and for generating SQL statements
     * that reference the table.
     *
     * @return the fully qualified table name, or just the table name if schema is not set
     */
    public String getFullTableName() {
        if (schemaName != null && !schemaName.isEmpty()) {
            return schemaName + "." + tableName;
        }
        return tableName;
    }

    /**
     * Returns a display-friendly string representation of the recommendation type.
     * <p>
     * Converts the enum constant (e.g., {@code MISSING_INDEX}) to a human-readable
     * format (e.g., "Missing Index") suitable for display in dashboards and reports.
     *
     * @return a formatted type name for display purposes
     */
    public String getTypeDisplay() {
        return switch (type) {
            case MISSING_INDEX -> "Missing Index";
            case UNUSED_INDEX -> "Unused Index";
            case DUPLICATE_INDEX -> "Duplicate Index";
            case LOW_CARDINALITY -> "Low Cardinality";
            case BLOATED_INDEX -> "Bloated Index";
        };
    }

    /**
     * Returns the Bootstrap CSS class for rendering the severity level as a badge.
     * <p>
     * Maps severity levels to appropriate Bootstrap 5 background colour classes:
     * <ul>
     *   <li>HIGH: {@code bg-danger} (red)</li>
     *   <li>MEDIUM: {@code bg-warning text-dark} (yellow with dark text)</li>
     *   <li>LOW: {@code bg-info} (blue)</li>
     * </ul>
     *
     * @return the Bootstrap CSS class for the severity badge
     */
    public String getSeverityCssClass() {
        return switch (severity) {
            case HIGH -> "bg-danger";
            case MEDIUM -> "bg-warning text-dark";
            case LOW -> "bg-info";
        };
    }

    /**
     * Returns the Bootstrap CSS class for rendering the recommendation type as a badge.
     * <p>
     * Maps recommendation types to appropriate Bootstrap 5 background colour classes:
     * <ul>
     *   <li>MISSING_INDEX: {@code bg-danger} (red, high priority)</li>
     *   <li>UNUSED_INDEX: {@code bg-warning text-dark} (yellow)</li>
     *   <li>DUPLICATE_INDEX: {@code bg-secondary} (grey)</li>
     *   <li>LOW_CARDINALITY: {@code bg-info} (blue)</li>
     *   <li>BLOATED_INDEX: {@code bg-warning text-dark} (yellow)</li>
     * </ul>
     *
     * @return the Bootstrap CSS class for the type badge
     */
    public String getTypeCssClass() {
        return switch (type) {
            case MISSING_INDEX -> "bg-danger";
            case UNUSED_INDEX -> "bg-warning text-dark";
            case DUPLICATE_INDEX -> "bg-secondary";
            case LOW_CARDINALITY -> "bg-info";
            case BLOATED_INDEX -> "bg-warning text-dark";
        };
    }

    /**
     * Formats the sequential scan ratio as a percentage string for display.
     * <p>
     * The ratio is formatted to one decimal place with a percent sign (e.g., "85.3%").
     *
     * @return the formatted sequential scan ratio as a percentage string
     */
    public String getSeqScanRatioFormatted() {
        return String.format("%.1f%%", seqScanRatio);
    }
}
