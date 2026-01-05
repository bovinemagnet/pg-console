package com.bovinemagnet.pgconsole.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a database table that may contain sensitive or personally identifiable information (PII).
 * <p>
 * This class aggregates sensitivity information about a table by analysing its columns for
 * potential PII indicators through heuristic pattern matching on column names, data types,
 * and naming conventions. It tracks the overall sensitivity level, individual PII columns,
 * Row-Level Security (RLS) protection status, and estimated table size.
 * <p>
 * The sensitivity assessment helps identify tables that may require additional security
 * controls, compliance review, or data governance policies. Tables are classified into
 * three sensitivity levels (HIGH, MEDIUM, LOW) based on the most sensitive PII type
 * detected amongst their columns.
 * <p>
 * This class also tracks whether PostgreSQL Row-Level Security policies are enabled on
 * the table, allowing administrators to identify sensitive tables that lack appropriate
 * access controls.
 * <p>
 * Example usage:
 * <pre>{@code
 * SensitiveTable table = new SensitiveTable("public", "users", SensitivityLevel.HIGH);
 * table.setHasRls(true);
 * table.setPolicyCount(2);
 * table.setEstimatedRowCount(150000);
 *
 * PiiColumnIndicator emailCol = new PiiColumnIndicator(
 *     "public", "users", "email", "varchar", PiiType.EMAIL, "Column name contains 'email'"
 * );
 * table.addPiiColumn(emailCol);
 *
 * if (table.needsRlsProtection()) {
 *     System.out.println("Warning: " + table.getFullyQualifiedName() +
 *                        " contains sensitive data but lacks RLS protection");
 * }
 * }</pre>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see PiiColumnIndicator
 * @see PiiColumnIndicator.PiiType
 */
public class SensitiveTable {

    /**
     * Enumeration of sensitivity levels for tables based on the PII content they contain.
     * <p>
     * The sensitivity level is typically determined by the most sensitive PII type found
     * amongst the table's columns. For example, a table containing both email addresses
     * (MEDIUM) and passwords (HIGH) would be classified as HIGH sensitivity.
     * <p>
     * Each level is associated with visual indicators (CSS classes) for presentation
     * in user interfaces and carries specific compliance and security implications:
     * <ul>
     * <li>HIGH: Tables containing credentials, financial data, health records, or government
     *     IDs that require the strongest protection under regulations like GDPR, HIPAA,
     *     or PCI-DSS. Should always have RLS or equivalent access controls.</li>
     * <li>MEDIUM: Tables containing contact information, personal identifiers, or names
     *     that require standard protection and proper consent management. Should have
     *     RLS for multi-tenant applications.</li>
     * <li>LOW: Tables containing technical identifiers or indirect PII that require
     *     basic protection with limited regulatory concerns.</li>
     * </ul>
     */
    public enum SensitivityLevel {
        /** High sensitivity level for tables containing highly sensitive PII such as passwords, financial data, or health records. */
        HIGH("High", "bg-danger", "Contains highly sensitive PII"),

        /** Medium sensitivity level for tables containing moderately sensitive data such as email addresses or personal names. */
        MEDIUM("Medium", "bg-warning text-dark", "Contains moderately sensitive data"),

        /** Low sensitivity level for tables containing potentially sensitive data with limited regulatory concern. */
        LOW("Low", "bg-info", "Contains potentially sensitive data");

        /** Human-readable display name for UI presentation. */
        private final String displayName;

        /** Bootstrap CSS class for badge styling and colour coding. */
        private final String cssClass;

        /** Brief description of what this sensitivity level represents. */
        private final String description;

        /**
         * Constructs a SensitivityLevel with display metadata.
         *
         * @param displayName human-readable name for UI display
         * @param cssClass    Bootstrap CSS class for badge styling
         * @param description brief explanation of this sensitivity level
         */
        SensitivityLevel(String displayName, String cssClass, String description) {
            this.displayName = displayName;
            this.cssClass = cssClass;
            this.description = description;
        }

        /**
         * Returns the human-readable display name for this sensitivity level.
         * <p>
         * This value is suitable for presentation in user interfaces, reports, and logs.
         *
         * @return the display name, never null
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Returns the Bootstrap CSS class for badge styling based on this sensitivity level.
         * <p>
         * The CSS classes provide visual colour coding:
         * <ul>
         * <li>HIGH: "bg-danger" (red) - critical security concern</li>
         * <li>MEDIUM: "bg-warning text-dark" (yellow) - moderate concern</li>
         * <li>LOW: "bg-info" (blue) - informational concern</li>
         * </ul>
         * These classes are compatible with Bootstrap 5 badge and alert components.
         *
         * @return Bootstrap CSS class string for badge colouring, never null
         */
        public String getCssClass() {
            return cssClass;
        }

        /**
         * Returns a brief description of what this sensitivity level represents.
         * <p>
         * The description explains the implications of this classification,
         * helpful for tooltips and documentation.
         *
         * @return the description text, never null
         */
        public String getDescription() {
            return description;
        }
    }

    /** The database schema containing this table. */
    private String schemaName;

    /** The table name within the schema. */
    private String tableName;

    /** The overall sensitivity level assessed for this table based on its PII content. */
    private SensitivityLevel sensitivityLevel;

    /** List of columns within this table that have been identified as potentially containing PII. */
    private List<PiiColumnIndicator> piiColumns = new ArrayList<>();

    /** Indicates whether PostgreSQL Row-Level Security (RLS) is enabled on this table. */
    private boolean hasRls;

    /** The number of RLS policies attached to this table (if RLS is enabled). */
    private int policyCount;

    /** Estimated number of rows in this table from PostgreSQL statistics. */
    private long estimatedRowCount;

    /**
     * Constructs an empty SensitiveTable.
     * <p>
     * Use setter methods to populate fields after construction.
     */
    public SensitiveTable() {
    }

    /**
     * Constructs a SensitiveTable with the specified core attributes.
     * <p>
     * Additional properties such as PII columns, RLS status, and row count
     * should be set using the appropriate setter methods.
     *
     * @param schemaName       the database schema name containing the table
     * @param tableName        the table name within the schema
     * @param sensitivityLevel the assessed sensitivity level based on PII content
     */
    public SensitiveTable(String schemaName, String tableName, SensitivityLevel sensitivityLevel) {
        this.schemaName = schemaName;
        this.tableName = tableName;
        this.sensitivityLevel = sensitivityLevel;
    }

    // Getters and Setters

    /**
     * Returns the database schema name containing this table.
     *
     * @return the schema name, may be null
     */
    public String getSchemaName() {
        return schemaName;
    }

    /**
     * Sets the database schema name containing this table.
     *
     * @param schemaName the schema name to set
     */
    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    /**
     * Returns the table name within the schema.
     *
     * @return the table name, may be null
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Sets the table name within the schema.
     *
     * @param tableName the table name to set
     */
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    /**
     * Returns the overall sensitivity level assessed for this table.
     * <p>
     * The sensitivity level is typically determined by the most sensitive PII type
     * found amongst the table's columns.
     *
     * @return the sensitivity level, may be null
     * @see SensitivityLevel
     */
    public SensitivityLevel getSensitivityLevel() {
        return sensitivityLevel;
    }

    /**
     * Sets the overall sensitivity level for this table.
     *
     * @param sensitivityLevel the sensitivity level to set
     * @see SensitivityLevel
     */
    public void setSensitivityLevel(SensitivityLevel sensitivityLevel) {
        this.sensitivityLevel = sensitivityLevel;
    }

    /**
     * Returns the list of columns identified as potentially containing PII.
     * <p>
     * The returned list is mutable and can be modified directly, or individual
     * columns can be added using {@link #addPiiColumn(PiiColumnIndicator)}.
     *
     * @return the list of PII column indicators, never null but may be empty
     * @see PiiColumnIndicator
     */
    public List<PiiColumnIndicator> getPiiColumns() {
        return piiColumns;
    }

    /**
     * Sets the list of columns identified as potentially containing PII.
     * <p>
     * Replaces any existing PII column indicators with the provided list.
     *
     * @param piiColumns the list of PII column indicators to set
     * @see PiiColumnIndicator
     */
    public void setPiiColumns(List<PiiColumnIndicator> piiColumns) {
        this.piiColumns = piiColumns;
    }

    /**
     * Returns whether PostgreSQL Row-Level Security (RLS) is enabled on this table.
     * <p>
     * RLS is a PostgreSQL feature that allows fine-grained access control at the
     * row level using security policies. Tables containing sensitive data should
     * typically have RLS enabled.
     *
     * @return true if RLS is enabled, false otherwise
     * @see <a href="https://www.postgresql.org/docs/current/ddl-rowsecurity.html">PostgreSQL RLS Documentation</a>
     */
    public boolean isHasRls() {
        return hasRls;
    }

    /**
     * Sets whether PostgreSQL Row-Level Security (RLS) is enabled on this table.
     *
     * @param hasRls true if RLS is enabled, false otherwise
     */
    public void setHasRls(boolean hasRls) {
        this.hasRls = hasRls;
    }

    /**
     * Returns the number of Row-Level Security policies attached to this table.
     * <p>
     * This value is only meaningful when {@link #hasRls} is true. A table may have
     * RLS enabled but have zero policies (effectively blocking all access), or it
     * may have multiple policies for different user roles or access patterns.
     *
     * @return the number of RLS policies, typically 0 when RLS is disabled
     */
    public int getPolicyCount() {
        return policyCount;
    }

    /**
     * Sets the number of Row-Level Security policies attached to this table.
     *
     * @param policyCount the number of RLS policies to set
     */
    public void setPolicyCount(int policyCount) {
        this.policyCount = policyCount;
    }

    /**
     * Returns the estimated number of rows in this table.
     * <p>
     * This value is typically derived from PostgreSQL's statistics views
     * (e.g., {@code pg_class.reltuples}) and represents an approximate count.
     * It helps assess the data volume and potential impact of a security breach.
     *
     * @return the estimated row count, may be 0 for empty or newly created tables
     */
    public long getEstimatedRowCount() {
        return estimatedRowCount;
    }

    /**
     * Sets the estimated number of rows in this table.
     *
     * @param estimatedRowCount the estimated row count to set
     */
    public void setEstimatedRowCount(long estimatedRowCount) {
        this.estimatedRowCount = estimatedRowCount;
    }

    // Helper Methods

    /**
     * Returns the fully qualified table name in schema.table format.
     * <p>
     * This format is suitable for unambiguous table references in SQL queries,
     * logs, and reports. For example: "public.users".
     *
     * @return the fully qualified table name, or a partial name if components are null
     */
    public String getFullyQualifiedName() {
        return schemaName + "." + tableName;
    }

    /**
     * Returns the Bootstrap CSS class for styling the sensitivity level badge.
     * <p>
     * Returns sensitivity-appropriate classes from {@link SensitivityLevel#getCssClass()},
     * or "bg-secondary" if no sensitivity level is set. These classes are compatible
     * with Bootstrap 5 badge components:
     * <ul>
     * <li>HIGH: "bg-danger" (red)</li>
     * <li>MEDIUM: "bg-warning text-dark" (yellow)</li>
     * <li>LOW: "bg-info" (blue)</li>
     * <li>Unknown: "bg-secondary" (grey)</li>
     * </ul>
     *
     * @return Bootstrap CSS class string for badge styling, never null
     */
    public String getSensitivityBadgeCssClass() {
        return sensitivityLevel != null ? sensitivityLevel.getCssClass() : "bg-secondary";
    }

    /**
     * Returns the human-readable display name for the sensitivity level.
     * <p>
     * Returns the display name from {@link SensitivityLevel#getDisplayName()},
     * or "Unknown" if no sensitivity level is set. This method is safe to call
     * even when {@link #sensitivityLevel} is null.
     *
     * @return the sensitivity level display name, or "Unknown" if not set, never null
     */
    public String getSensitivityDisplay() {
        return sensitivityLevel != null ? sensitivityLevel.getDisplayName() : "Unknown";
    }

    /**
     * Adds a PII column indicator to this table's collection of sensitive columns.
     * <p>
     * This method appends the indicator to the existing list returned by
     * {@link #getPiiColumns()}.
     *
     * @param indicator the PII column indicator to add, should not be null
     * @see PiiColumnIndicator
     */
    public void addPiiColumn(PiiColumnIndicator indicator) {
        this.piiColumns.add(indicator);
    }

    /**
     * Returns the number of PII columns detected in this table.
     * <p>
     * This count reflects the size of the list returned by {@link #getPiiColumns()}.
     *
     * @return the count of PII columns, 0 if no columns have been identified
     */
    public int getPiiColumnCount() {
        return piiColumns != null ? piiColumns.size() : 0;
    }

    /**
     * Returns a comma-separated list of PII column names for display purposes.
     * <p>
     * This method extracts column names from all {@link PiiColumnIndicator} instances
     * in the {@link #piiColumns} list and joins them with commas. Suitable for
     * presentation in tables, reports, and summary views.
     *
     * @return comma-separated column names, or "-" if no PII columns are present, never null
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
     * Returns the Row-Level Security status badge text for display.
     * <p>
     * If RLS is enabled, returns "Enabled (N policy/policies)" where N is the
     * policy count. If RLS is disabled, returns "Disabled". Suitable for
     * presentation in UI badges or status columns.
     *
     * @return RLS status text with policy count, never null
     */
    public String getRlsStatusBadge() {
        if (hasRls) {
            return "Enabled (" + policyCount + " " + (policyCount == 1 ? "policy" : "policies") + ")";
        }
        return "Disabled";
    }

    /**
     * Returns the Bootstrap CSS class for styling the RLS status badge.
     * <p>
     * Returns colour-coded classes based on RLS status:
     * <ul>
     * <li>Enabled: "bg-success" (green) - indicates proper protection</li>
     * <li>Disabled: "bg-warning text-dark" (yellow) - indicates potential security concern</li>
     * </ul>
     * These classes are compatible with Bootstrap 5 badge components.
     *
     * @return Bootstrap CSS class string for badge styling, never null
     */
    public String getRlsStatusCssClass() {
        return hasRls ? "bg-success" : "bg-warning text-dark";
    }

    /**
     * Determines whether this table needs Row-Level Security protection based on its sensitivity level.
     * <p>
     * Returns true if all of the following conditions are met:
     * <ul>
     * <li>RLS is not currently enabled ({@link #hasRls} is false)</li>
     * <li>A sensitivity level is set ({@link #sensitivityLevel} is not null)</li>
     * <li>The sensitivity level is HIGH or MEDIUM</li>
     * </ul>
     * <p>
     * This method helps identify sensitive tables that lack appropriate access controls
     * and should be flagged for security review. LOW sensitivity tables are excluded
     * from this check as they may not require RLS in all deployment scenarios.
     *
     * @return true if RLS protection is recommended but not currently enabled, false otherwise
     */
    public boolean needsRlsProtection() {
        return !hasRls && sensitivityLevel != null &&
               (sensitivityLevel == SensitivityLevel.HIGH ||
                sensitivityLevel == SensitivityLevel.MEDIUM);
    }

    /**
     * Returns a human-readable formatted version of the estimated row count.
     * <p>
     * Formats large numbers using SI suffixes for better readability:
     * <ul>
     * <li>Less than 1,000: Returns the exact number (e.g., "523")</li>
     * <li>1,000 to 999,999: Returns thousands with one decimal place (e.g., "15.3K")</li>
     * <li>1,000,000 or more: Returns millions with one decimal place (e.g., "2.7M")</li>
     * </ul>
     * Suitable for display in table columns where space is limited.
     *
     * @return formatted row count string with SI suffix if applicable, never null
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
