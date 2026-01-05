package com.bovinemagnet.pgconsole.model;

/**
 * Represents a database column that may contain personally identifiable information (PII).
 * <p>
 * This class is used to track and report columns that potentially store sensitive personal data,
 * detected through heuristic pattern matching on column names, data types, and naming conventions.
 * Each indicator includes metadata about the column location, the type of PII detected, and a
 * confidence score representing the likelihood that the column actually contains PII.
 * <p>
 * The class supports multiple PII categories ranging from high-sensitivity data (passwords,
 * financial information) to medium-sensitivity data (email addresses, names) to low-sensitivity
 * data (IP addresses). Each category is associated with visual indicators (icons, CSS classes)
 * for UI presentation.
 * <p>
 * Example usage:
 * <pre>{@code
 * PiiColumnIndicator indicator = new PiiColumnIndicator(
 *     "public",
 *     "users",
 *     "email_address",
 *     "varchar",
 *     PiiType.EMAIL,
 *     "Column name contains 'email'"
 * );
 * indicator.setConfidenceScore(0.95);
 * }</pre>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see SensitiveTable
 */
public class PiiColumnIndicator {

    /**
     * Enumeration of personally identifiable information (PII) types that can be detected in database columns.
     * <p>
     * Each PII type is categorised by sensitivity level and associated with visual presentation
     * attributes for use in user interfaces. The sensitivity levels (HIGH, MEDIUM, LOW) help
     * prioritise security and compliance concerns.
     * <p>
     * High-sensitivity types include credentials, financial data, and health information that
     * require strict protection under regulations like GDPR, HIPAA, or PCI-DSS.
     * Medium-sensitivity types include contact information and personal identifiers.
     * Low-sensitivity types include technical identifiers that may indirectly identify individuals.
     */
    public enum PiiType {
        /** Email addresses (medium sensitivity). */
        EMAIL("Email", "bi-envelope", "Email addresses"),

        /** Phone numbers (medium sensitivity). */
        PHONE("Phone", "bi-phone", "Phone numbers"),

        /** Social Security Numbers or National Insurance numbers (high sensitivity). */
        SSN("SSN/NI", "bi-card-text", "Social Security or National Insurance numbers"),

        /** Passwords or authentication credentials (high sensitivity). */
        PASSWORD("Password", "bi-key", "Password or credential data"),

        /** Dates of birth or age information (medium sensitivity). */
        DOB("Date of Birth", "bi-calendar", "Birth dates or ages"),

        /** Physical mailing addresses (medium sensitivity). */
        ADDRESS("Address", "bi-house", "Physical addresses"),

        /** Personal names (first, last, full names) (medium sensitivity). */
        NAME("Name", "bi-person", "Personal names"),

        /** Financial data such as credit card or bank account numbers (high sensitivity). */
        FINANCIAL("Financial", "bi-credit-card", "Financial data (credit card, bank account)"),

        /** Government-issued identity documents like passports or driver's licences (high sensitivity). */
        ID_DOCUMENT("ID Document", "bi-card-heading", "Passport, driver's licence, etc."),

        /** IP addresses that may identify individuals or sessions (low sensitivity). */
        IP_ADDRESS("IP Address", "bi-globe", "IP addresses"),

        /** Health or medical data protected under HIPAA or similar regulations (high sensitivity). */
        HEALTH("Health", "bi-heart-pulse", "Health or medical data"),

        /** Other types of potentially sensitive personal data (low sensitivity). */
        OTHER("Other PII", "bi-shield-exclamation", "Other potentially sensitive data");

        /** Human-readable display name for UI presentation. */
        private final String displayName;

        /** Bootstrap icon class (e.g., "bi-envelope") for visual representation. */
        private final String iconClass;

        /** Brief description of the PII type and what it encompasses. */
        private final String description;

        /**
         * Constructs a PiiType with display metadata.
         *
         * @param displayName human-readable name for UI display
         * @param iconClass   Bootstrap icon class for visual representation
         * @param description brief explanation of what this PII type covers
         */
        PiiType(String displayName, String iconClass, String description) {
            this.displayName = displayName;
            this.iconClass = iconClass;
            this.description = description;
        }

        /**
         * Returns the human-readable display name for this PII type.
         * <p>
         * This value is suitable for presentation in user interfaces, reports, and logs.
         *
         * @return the display name, never null
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Returns the Bootstrap icon class associated with this PII type.
         * <p>
         * Icon classes follow Bootstrap Icons naming conventions (e.g., "bi-envelope", "bi-key").
         * These can be used to render visual indicators in web interfaces.
         *
         * @return the icon class name, never null
         * @see <a href="https://icons.getbootstrap.com/">Bootstrap Icons</a>
         */
        public String getIconClass() {
            return iconClass;
        }

        /**
         * Returns a brief description of what this PII type encompasses.
         * <p>
         * The description explains the kind of data classified under this type,
         * helpful for tooltips and documentation.
         *
         * @return the description text, never null
         */
        public String getDescription() {
            return description;
        }

        /**
         * Returns the Bootstrap CSS class for badge styling based on sensitivity level.
         * <p>
         * High-sensitivity types (SSN, PASSWORD, FINANCIAL, ID_DOCUMENT, HEALTH) use "bg-danger"
         * to indicate critical security concern. Medium-sensitivity types (EMAIL, PHONE, DOB,
         * ADDRESS, NAME) use "bg-warning text-dark" to indicate moderate concern. Low-sensitivity
         * types (IP_ADDRESS, OTHER) use "bg-info" to indicate informational concern.
         * <p>
         * These classes are compatible with Bootstrap 5 badge and alert components.
         *
         * @return Bootstrap CSS class string for badge colouring, never null
         */
        public String getCssClass() {
            return switch (this) {
                case SSN, PASSWORD, FINANCIAL, ID_DOCUMENT, HEALTH -> "bg-danger";
                case EMAIL, PHONE, DOB, ADDRESS, NAME -> "bg-warning text-dark";
                case IP_ADDRESS, OTHER -> "bg-info";
            };
        }

        /**
         * Returns the data sensitivity level for this PII type.
         * <p>
         * The sensitivity level determines the degree of protection required and the
         * severity of potential data breaches:
         * <ul>
         * <li>HIGH: Credentials, financial data, health records, government IDs - requires
         *     strongest protection, often regulated by law (GDPR, HIPAA, PCI-DSS)</li>
         * <li>MEDIUM: Contact information, personal identifiers - requires standard
         *     protection and consent management</li>
         * <li>LOW: Technical identifiers - requires basic protection, limited regulatory concern</li>
         * </ul>
         *
         * @return the sensitivity level classification, never null
         * @see SensitiveTable.SensitivityLevel
         */
        public SensitiveTable.SensitivityLevel getSensitivityLevel() {
            return switch (this) {
                case SSN, PASSWORD, FINANCIAL, ID_DOCUMENT, HEALTH -> SensitiveTable.SensitivityLevel.HIGH;
                case EMAIL, PHONE, DOB, ADDRESS, NAME -> SensitiveTable.SensitivityLevel.MEDIUM;
                case IP_ADDRESS, OTHER -> SensitiveTable.SensitivityLevel.LOW;
            };
        }
    }

    /** The database schema containing the table with the potentially sensitive column. */
    private String schemaName;

    /** The table name containing the potentially sensitive column. */
    private String tableName;

    /** The column name that may contain PII. */
    private String columnName;

    /** The PostgreSQL data type of the column (e.g., "varchar", "text", "integer"). */
    private String dataType;

    /** The type of PII detected in this column. */
    private PiiType piiType;

    /** Explanation of why this column was flagged as containing PII (e.g., "Column name contains 'email'"). */
    private String matchReason;

    /** Confidence score (0.0 to 1.0) indicating likelihood that this column actually contains PII. */
    private double confidenceScore;

    /**
     * Constructs an empty PiiColumnIndicator.
     * <p>
     * Use setter methods to populate fields after construction.
     */
    public PiiColumnIndicator() {
    }

    /**
     * Constructs a PiiColumnIndicator with the specified attributes.
     * <p>
     * The confidence score defaults to 0.0 and should be set separately using
     * {@link #setConfidenceScore(double)} based on the detection algorithm's certainty.
     *
     * @param schemaName  the database schema name containing the table
     * @param tableName   the table name containing the column
     * @param columnName  the column name potentially containing PII
     * @param dataType    the PostgreSQL data type of the column
     * @param piiType     the type of PII detected in this column
     * @param matchReason human-readable explanation for why this column was flagged
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

    /**
     * Returns the database schema name containing the table.
     *
     * @return the schema name, may be null
     */
    public String getSchemaName() {
        return schemaName;
    }

    /**
     * Sets the database schema name containing the table.
     *
     * @param schemaName the schema name to set
     */
    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    /**
     * Returns the table name containing the potentially sensitive column.
     *
     * @return the table name, may be null
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Sets the table name containing the potentially sensitive column.
     *
     * @param tableName the table name to set
     */
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    /**
     * Returns the column name potentially containing PII.
     *
     * @return the column name, may be null
     */
    public String getColumnName() {
        return columnName;
    }

    /**
     * Sets the column name potentially containing PII.
     *
     * @param columnName the column name to set
     */
    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    /**
     * Returns the PostgreSQL data type of the column.
     * <p>
     * Examples include "varchar", "text", "integer", "timestamp", etc.
     *
     * @return the data type, may be null
     */
    public String getDataType() {
        return dataType;
    }

    /**
     * Sets the PostgreSQL data type of the column.
     *
     * @param dataType the data type to set (e.g., "varchar", "text")
     */
    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    /**
     * Returns the type of PII detected in this column.
     *
     * @return the PII type, may be null
     * @see PiiType
     */
    public PiiType getPiiType() {
        return piiType;
    }

    /**
     * Sets the type of PII detected in this column.
     *
     * @param piiType the PII type to set
     * @see PiiType
     */
    public void setPiiType(PiiType piiType) {
        this.piiType = piiType;
    }

    /**
     * Returns the human-readable explanation for why this column was flagged as PII.
     * <p>
     * Example reasons include "Column name contains 'email'", "Matches SSN pattern",
     * or "Column name ends with '_password'".
     *
     * @return the match reason explanation, may be null
     */
    public String getMatchReason() {
        return matchReason;
    }

    /**
     * Sets the explanation for why this column was flagged as PII.
     *
     * @param matchReason the match reason to set
     */
    public void setMatchReason(String matchReason) {
        this.matchReason = matchReason;
    }

    /**
     * Returns the confidence score indicating likelihood that this column contains PII.
     * <p>
     * The score ranges from 0.0 (very unlikely) to 1.0 (very likely). Higher scores
     * indicate stronger pattern matches or more definitive naming conventions.
     *
     * @return the confidence score between 0.0 and 1.0
     */
    public double getConfidenceScore() {
        return confidenceScore;
    }

    /**
     * Sets the confidence score indicating likelihood that this column contains PII.
     * <p>
     * The score should be between 0.0 and 1.0, where higher values indicate
     * greater certainty in the PII detection.
     *
     * @param confidenceScore the confidence score to set (0.0 to 1.0)
     */
    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    // Helper Methods

    /**
     * Returns the fully qualified column name in schema.table.column format.
     * <p>
     * This format is suitable for unambiguous column references in SQL queries,
     * logs, and reports. For example: "public.users.email_address".
     *
     * @return the fully qualified column name, or a partial name if components are null
     */
    public String getFullyQualifiedName() {
        return schemaName + "." + tableName + "." + columnName;
    }

    /**
     * Returns the human-readable display name for the detected PII type.
     * <p>
     * If no PII type is set, returns "Unknown". This method is safe to call
     * even when {@link #piiType} is null.
     *
     * @return the PII type display name, or "Unknown" if piiType is null
     */
    public String getPiiTypeDisplay() {
        return piiType != null ? piiType.getDisplayName() : "Unknown";
    }

    /**
     * Returns the Bootstrap CSS class for styling the PII type badge.
     * <p>
     * Returns sensitivity-appropriate classes from the {@link PiiType#getCssClass()} method,
     * or "bg-secondary" if no PII type is set. These classes are compatible with
     * Bootstrap 5 badge components.
     *
     * @return Bootstrap CSS class string for badge styling, never null
     */
    public String getPiiTypeCssClass() {
        return piiType != null ? piiType.getCssClass() : "bg-secondary";
    }

    /**
     * Returns the Bootstrap icon class for visual representation of the PII type.
     * <p>
     * Returns the icon class from {@link PiiType#getIconClass()}, or "bi-question-circle"
     * if no PII type is set. These classes are compatible with Bootstrap Icons.
     *
     * @return Bootstrap icon class name, never null
     * @see <a href="https://icons.getbootstrap.com/">Bootstrap Icons</a>
     */
    public String getPiiTypeIconClass() {
        return piiType != null ? piiType.getIconClass() : "bi-question-circle";
    }

    /**
     * Returns the confidence score formatted as a percentage string.
     * <p>
     * Formats the {@link #confidenceScore} value as a whole number percentage
     * (e.g., "95%" for a score of 0.95). Suitable for display in tables and reports.
     *
     * @return formatted percentage string with "%" suffix, never null
     */
    public String getConfidenceDisplay() {
        return String.format("%.0f%%", confidenceScore * 100);
    }

    /**
     * Returns a Bootstrap CSS class for colour-coding the confidence score.
     * <p>
     * The colour scheme indicates confidence strength:
     * <ul>
     * <li>90%+: "text-success" (green) - high confidence</li>
     * <li>70-89%: "text-info" (blue) - good confidence</li>
     * <li>50-69%: "text-warning" (yellow) - moderate confidence</li>
     * <li>Below 50%: "text-muted" (grey) - low confidence</li>
     * </ul>
     *
     * @return Bootstrap text colour class based on confidence level, never null
     */
    public String getConfidenceCssClass() {
        if (confidenceScore >= 0.9) return "text-success";
        if (confidenceScore >= 0.7) return "text-info";
        if (confidenceScore >= 0.5) return "text-warning";
        return "text-muted";
    }
}
