package com.bovinemagnet.pgconsole.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a database table that may contain sensitive or PII data.
 * <p>
 * Sensitivity is determined by heuristic analysis of column names
 * and table patterns.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class SensitiveTable {

    /**
     * Sensitivity levels for tables based on PII content.
     */
    public enum SensitivityLevel {
        HIGH("High", "bg-danger", "Contains highly sensitive PII"),
        MEDIUM("Medium", "bg-warning text-dark", "Contains moderately sensitive data"),
        LOW("Low", "bg-info", "Contains potentially sensitive data");

        private final String displayName;
        private final String cssClass;
        private final String description;

        SensitivityLevel(String displayName, String cssClass, String description) {
            this.displayName = displayName;
            this.cssClass = cssClass;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getCssClass() {
            return cssClass;
        }

        public String getDescription() {
            return description;
        }
    }

    private String schemaName;
    private String tableName;
    private SensitivityLevel sensitivityLevel;
    private List<PiiColumnIndicator> piiColumns = new ArrayList<>();
    private boolean hasRls;
    private int policyCount;
    private long estimatedRowCount;

    /**
     * Default constructor.
     */
    public SensitiveTable() {
    }

    /**
     * Constructs a SensitiveTable with the specified attributes.
     *
     * @param schemaName       the schema containing the table
     * @param tableName        the table name
     * @param sensitivityLevel the assessed sensitivity level
     */
    public SensitiveTable(String schemaName, String tableName, SensitivityLevel sensitivityLevel) {
        this.schemaName = schemaName;
        this.tableName = tableName;
        this.sensitivityLevel = sensitivityLevel;
    }

    // Getters and Setters

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

    public SensitivityLevel getSensitivityLevel() {
        return sensitivityLevel;
    }

    public void setSensitivityLevel(SensitivityLevel sensitivityLevel) {
        this.sensitivityLevel = sensitivityLevel;
    }

    public List<PiiColumnIndicator> getPiiColumns() {
        return piiColumns;
    }

    public void setPiiColumns(List<PiiColumnIndicator> piiColumns) {
        this.piiColumns = piiColumns;
    }

    public boolean isHasRls() {
        return hasRls;
    }

    public void setHasRls(boolean hasRls) {
        this.hasRls = hasRls;
    }

    public int getPolicyCount() {
        return policyCount;
    }

    public void setPolicyCount(int policyCount) {
        this.policyCount = policyCount;
    }

    public long getEstimatedRowCount() {
        return estimatedRowCount;
    }

    public void setEstimatedRowCount(long estimatedRowCount) {
        this.estimatedRowCount = estimatedRowCount;
    }

    // Helper Methods

    /**
     * Returns the fully qualified table name.
     *
     * @return schema.table format
     */
    public String getFullyQualifiedName() {
        return schemaName + "." + tableName;
    }

    /**
     * Returns the CSS class for the sensitivity badge.
     *
     * @return CSS class name
     */
    public String getSensitivityBadgeCssClass() {
        return sensitivityLevel != null ? sensitivityLevel.getCssClass() : "bg-secondary";
    }

    /**
     * Returns the display name for the sensitivity level.
     *
     * @return sensitivity level display name
     */
    public String getSensitivityDisplay() {
        return sensitivityLevel != null ? sensitivityLevel.getDisplayName() : "Unknown";
    }

    /**
     * Adds a PII column indicator to this table.
     *
     * @param indicator the PII column indicator
     */
    public void addPiiColumn(PiiColumnIndicator indicator) {
        this.piiColumns.add(indicator);
    }

    /**
     * Returns the number of PII columns detected.
     *
     * @return count of PII columns
     */
    public int getPiiColumnCount() {
        return piiColumns != null ? piiColumns.size() : 0;
    }

    /**
     * Returns a comma-separated list of PII column names.
     *
     * @return PII column names for display
     */
    public String getPiiColumnsDisplay() {
        if (piiColumns == null || piiColumns.isEmpty()) {
            return "-";
        }
        return piiColumns.stream()
                .map(PiiColumnIndicator::getColumnName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("-");
    }

    /**
     * Returns the RLS status badge text.
     *
     * @return RLS status for display
     */
    public String getRlsStatusBadge() {
        if (hasRls) {
            return "Enabled (" + policyCount + " " + (policyCount == 1 ? "policy" : "policies") + ")";
        }
        return "Disabled";
    }

    /**
     * Returns the CSS class for the RLS status badge.
     *
     * @return CSS class name
     */
    public String getRlsStatusCssClass() {
        return hasRls ? "bg-success" : "bg-warning text-dark";
    }

    /**
     * Checks if this table needs RLS protection based on sensitivity.
     *
     * @return true if RLS is recommended but not enabled
     */
    public boolean needsRlsProtection() {
        return !hasRls && sensitivityLevel != null &&
               (sensitivityLevel == SensitivityLevel.HIGH ||
                sensitivityLevel == SensitivityLevel.MEDIUM);
    }

    /**
     * Returns the formatted estimated row count.
     *
     * @return formatted row count
     */
    public String getEstimatedRowCountFormatted() {
        if (estimatedRowCount < 1000) {
            return String.valueOf(estimatedRowCount);
        }
        if (estimatedRowCount < 1000000) {
            return String.format("%.1fK", estimatedRowCount / 1000.0);
        }
        return String.format("%.1fM", estimatedRowCount / 1000000.0);
    }
}
