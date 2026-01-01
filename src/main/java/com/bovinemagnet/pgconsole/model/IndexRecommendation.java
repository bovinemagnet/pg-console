package com.bovinemagnet.pgconsole.model;

/**
 * Represents an index recommendation or index issue.
 * Used by the Index Advisor to suggest improvements.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class IndexRecommendation {

    public enum RecommendationType {
        MISSING_INDEX,      // Table needs an index based on scan patterns
        UNUSED_INDEX,       // Index exists but hasn't been used
        DUPLICATE_INDEX,    // Index is redundant (covered by another)
        LOW_CARDINALITY,    // Index on low-cardinality column (may not help)
        BLOATED_INDEX       // Index is bloated and needs rebuilding
    }

    public enum Severity {
        HIGH,    // Should be addressed soon
        MEDIUM,  // Worth investigating
        LOW      // Informational
    }

    private RecommendationType type;
    private Severity severity;
    private String schemaName;
    private String tableName;
    private String indexName;
    private String recommendation;
    private String rationale;
    private String suggestedAction;

    // Statistics to support the recommendation
    private long seqScans;
    private long idxScans;
    private double seqScanRatio;
    private long tableRows;
    private String tableSize;
    private String indexSize;
    private long deadTuples;

    public IndexRecommendation() {
    }

    public IndexRecommendation(RecommendationType type, Severity severity) {
        this.type = type;
        this.severity = severity;
    }

    // Getters and setters

    public RecommendationType getType() {
        return type;
    }

    public void setType(RecommendationType type) {
        this.type = type;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
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

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public String getSuggestedAction() {
        return suggestedAction;
    }

    public void setSuggestedAction(String suggestedAction) {
        this.suggestedAction = suggestedAction;
    }

    public long getSeqScans() {
        return seqScans;
    }

    public void setSeqScans(long seqScans) {
        this.seqScans = seqScans;
    }

    public long getIdxScans() {
        return idxScans;
    }

    public void setIdxScans(long idxScans) {
        this.idxScans = idxScans;
    }

    public double getSeqScanRatio() {
        return seqScanRatio;
    }

    public void setSeqScanRatio(double seqScanRatio) {
        this.seqScanRatio = seqScanRatio;
    }

    public long getTableRows() {
        return tableRows;
    }

    public void setTableRows(long tableRows) {
        this.tableRows = tableRows;
    }

    public String getTableSize() {
        return tableSize;
    }

    public void setTableSize(String tableSize) {
        this.tableSize = tableSize;
    }

    public String getIndexSize() {
        return indexSize;
    }

    public void setIndexSize(String indexSize) {
        this.indexSize = indexSize;
    }

    public long getDeadTuples() {
        return deadTuples;
    }

    public void setDeadTuples(long deadTuples) {
        this.deadTuples = deadTuples;
    }

    /**
     * Returns the full table name (schema.table).
     */
    public String getFullTableName() {
        if (schemaName != null && !schemaName.isEmpty()) {
            return schemaName + "." + tableName;
        }
        return tableName;
    }

    /**
     * Returns the type as a display-friendly string.
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
     * Returns the severity CSS class for Bootstrap badges.
     */
    public String getSeverityCssClass() {
        return switch (severity) {
            case HIGH -> "bg-danger";
            case MEDIUM -> "bg-warning text-dark";
            case LOW -> "bg-info";
        };
    }

    /**
     * Returns the type CSS class for Bootstrap badges.
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
     * Formats the sequential scan ratio as a percentage.
     */
    public String getSeqScanRatioFormatted() {
        return String.format("%.1f%%", seqScanRatio);
    }
}
