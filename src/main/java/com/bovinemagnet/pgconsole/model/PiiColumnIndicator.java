package com.bovinemagnet.pgconsole.model;

/**
 * Represents a column that may contain personally identifiable information (PII).
 * <p>
 * PII detection is performed using heuristic pattern matching on column names.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class PiiColumnIndicator {

    /**
     * Types of PII that can be detected.
     */
    public enum PiiType {
        EMAIL("Email", "bi-envelope", "Email addresses"),
        PHONE("Phone", "bi-phone", "Phone numbers"),
        SSN("SSN/NI", "bi-card-text", "Social Security or National Insurance numbers"),
        PASSWORD("Password", "bi-key", "Password or credential data"),
        DOB("Date of Birth", "bi-calendar", "Birth dates or ages"),
        ADDRESS("Address", "bi-house", "Physical addresses"),
        NAME("Name", "bi-person", "Personal names"),
        FINANCIAL("Financial", "bi-credit-card", "Financial data (credit card, bank account)"),
        ID_DOCUMENT("ID Document", "bi-card-heading", "Passport, driver's licence, etc."),
        IP_ADDRESS("IP Address", "bi-globe", "IP addresses"),
        HEALTH("Health", "bi-heart-pulse", "Health or medical data"),
        OTHER("Other PII", "bi-shield-exclamation", "Other potentially sensitive data");

        private final String displayName;
        private final String iconClass;
        private final String description;

        PiiType(String displayName, String iconClass, String description) {
            this.displayName = displayName;
            this.iconClass = iconClass;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getIconClass() {
            return iconClass;
        }

        public String getDescription() {
            return description;
        }

        /**
         * Returns the CSS class for the PII type badge.
         *
         * @return CSS class name based on sensitivity
         */
        public String getCssClass() {
            return switch (this) {
                case SSN, PASSWORD, FINANCIAL, ID_DOCUMENT, HEALTH -> "bg-danger";
                case EMAIL, PHONE, DOB, ADDRESS, NAME -> "bg-warning text-dark";
                case IP_ADDRESS, OTHER -> "bg-info";
            };
        }

        /**
         * Returns the sensitivity level for this PII type.
         *
         * @return sensitivity level
         */
        public SensitiveTable.SensitivityLevel getSensitivityLevel() {
            return switch (this) {
                case SSN, PASSWORD, FINANCIAL, ID_DOCUMENT, HEALTH -> SensitiveTable.SensitivityLevel.HIGH;
                case EMAIL, PHONE, DOB, ADDRESS, NAME -> SensitiveTable.SensitivityLevel.MEDIUM;
                case IP_ADDRESS, OTHER -> SensitiveTable.SensitivityLevel.LOW;
            };
        }
    }

    private String schemaName;
    private String tableName;
    private String columnName;
    private String dataType;
    private PiiType piiType;
    private String matchReason;
    private double confidenceScore;

    /**
     * Default constructor.
     */
    public PiiColumnIndicator() {
    }

    /**
     * Constructs a PiiColumnIndicator with the specified attributes.
     *
     * @param schemaName  the schema containing the table
     * @param tableName   the table name
     * @param columnName  the column name
     * @param dataType    the column data type
     * @param piiType     the detected PII type
     * @param matchReason why this was flagged as PII
     */
    public PiiColumnIndicator(String schemaName, String tableName, String columnName,
                               String dataType, PiiType piiType, String matchReason) {
        this.schemaName = schemaName;
        this.tableName = tableName;
        this.columnName = columnName;
        this.dataType = dataType;
        this.piiType = piiType;
        this.matchReason = matchReason;
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

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public PiiType getPiiType() {
        return piiType;
    }

    public void setPiiType(PiiType piiType) {
        this.piiType = piiType;
    }

    public String getMatchReason() {
        return matchReason;
    }

    public void setMatchReason(String matchReason) {
        this.matchReason = matchReason;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    // Helper Methods

    /**
     * Returns the fully qualified column name.
     *
     * @return schema.table.column format
     */
    public String getFullyQualifiedName() {
        return schemaName + "." + tableName + "." + columnName;
    }

    /**
     * Returns the PII type display name.
     *
     * @return PII type for display
     */
    public String getPiiTypeDisplay() {
        return piiType != null ? piiType.getDisplayName() : "Unknown";
    }

    /**
     * Returns the CSS class for the PII type badge.
     *
     * @return CSS class name
     */
    public String getPiiTypeCssClass() {
        return piiType != null ? piiType.getCssClass() : "bg-secondary";
    }

    /**
     * Returns the icon class for the PII type.
     *
     * @return Bootstrap icon class
     */
    public String getPiiTypeIconClass() {
        return piiType != null ? piiType.getIconClass() : "bi-question-circle";
    }

    /**
     * Returns the confidence score as a percentage string.
     *
     * @return confidence percentage
     */
    public String getConfidenceDisplay() {
        return String.format("%.0f%%", confidenceScore * 100);
    }

    /**
     * Returns the CSS class based on confidence score.
     *
     * @return CSS class name
     */
    public String getConfidenceCssClass() {
        if (confidenceScore >= 0.9) return "text-success";
        if (confidenceScore >= 0.7) return "text-info";
        if (confidenceScore >= 0.5) return "text-warning";
        return "text-muted";
    }
}
