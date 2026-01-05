package com.bovinemagnet.pgconsole.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Row-Level Security (RLS) policy from the PostgreSQL pg_policy system catalogue.
 * <p>
 * Row-Level Security policies control which rows users can view or modify in a table. Each policy
 * defines conditions that determine row visibility (USING expression) and modification permissions
 * (WITH CHECK expression). Policies can be either permissive (granting access) or restrictive
 * (denying access), and apply to specific SQL commands (SELECT, INSERT, UPDATE, DELETE, or ALL).
 * <p>
 * A table can have multiple policies, which are combined as follows:
 * <ul>
 *   <li>Permissive policies are OR'ed together</li>
 *   <li>Restrictive policies are AND'ed together</li>
 *   <li>The final result is: (permissive1 OR permissive2 OR ...) AND (restrictive1 AND restrictive2 AND ...)</li>
 * </ul>
 * <p>
 * This model maps data from the pg_policy and pg_policies views, providing both the raw policy
 * definition and convenience methods for dashboard display (CSS classes, truncated expressions, etc.).
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see <a href="https://www.postgresql.org/docs/current/sql-createpolicy.html">PostgreSQL CREATE POLICY Documentation</a>
 */
public class RlsPolicy {

    /**
     * Enumerates the SQL commands that a Row-Level Security policy can apply to.
     * <p>
     * PostgreSQL stores these as single-character codes in pg_policy.polcmd:
     * <ul>
     *   <li>'*' or null - ALL commands</li>
     *   <li>'r' - SELECT (read)</li>
     *   <li>'a' - INSERT (add)</li>
     *   <li>'w' - UPDATE (write)</li>
     *   <li>'d' - DELETE</li>
     * </ul>
     * This enum provides a type-safe representation with human-readable names and descriptions.
     */
    public enum Command {
        /** Applies to all SQL commands (SELECT, INSERT, UPDATE, DELETE). */
        ALL("ALL", "Applies to all commands"),

        /** Applies to SELECT queries that retrieve rows. */
        SELECT("SELECT", "Applies to SELECT queries"),

        /** Applies to INSERT statements that add new rows. */
        INSERT("INSERT", "Applies to INSERT statements"),

        /** Applies to UPDATE statements that modify existing rows. */
        UPDATE("UPDATE", "Applies to UPDATE statements"),

        /** Applies to DELETE statements that remove rows. */
        DELETE("DELETE", "Applies to DELETE statements");

        private final String displayName;
        private final String description;

        /**
         * Constructs a Command enum value.
         *
         * @param displayName the human-readable command name (e.g., "SELECT", "ALL")
         * @param description a brief description of what the command applies to
         */
        Command(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        /**
         * Returns the human-readable display name for this command.
         *
         * @return the command name (e.g., "SELECT", "INSERT", "ALL")
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Returns a brief description of what this command applies to.
         *
         * @return the command description
         */
        public String getDescription() {
            return description;
        }

        /**
         * Parses a command from the PostgreSQL pg_policy.polcmd character code.
         * <p>
         * Converts PostgreSQL's single-character command codes to their corresponding
         * Command enum values. The mapping is:
         * <ul>
         *   <li>null or '*' → ALL</li>
         *   <li>'r' or 'R' → SELECT</li>
         *   <li>'a' or 'A' → INSERT</li>
         *   <li>'w' or 'W' → UPDATE</li>
         *   <li>'d' or 'D' → DELETE</li>
         *   <li>Any other value → ALL (fallback)</li>
         * </ul>
         *
         * @param cmd the command character from pg_policy.polcmd (may be null)
         * @return the corresponding Command enum value, never null
         */
        public static Command fromString(String cmd) {
            if (cmd == null || cmd.equals("*")) {
                return ALL;
            }
            return switch (cmd.toUpperCase()) {
                case "R" -> SELECT;
                case "A" -> INSERT;
                case "W" -> UPDATE;
                case "D" -> DELETE;
                default -> ALL;
            };
        }
    }

    /** The schema name containing the table to which this policy applies. */
    private String schemaName;

    /** The table name to which this policy applies. */
    private String tableName;

    /** The name of this policy (unique within the table). */
    private String policyName;

    /** The SQL command(s) to which this policy applies. */
    private Command command;

    /** Whether this is a permissive policy (true) or restrictive policy (false). */
    private boolean permissive;

    /** The USING expression that determines which rows are visible for SELECT/UPDATE/DELETE. */
    private String usingExpression;

    /** The WITH CHECK expression that determines which rows can be added/modified via INSERT/UPDATE. */
    private String withCheckExpression;

    /** The list of database roles to which this policy applies (empty means PUBLIC). */
    private List<String> roles = new ArrayList<>();

    /**
     * Default constructor.
     */
    public RlsPolicy() {
    }

    /**
     * Constructs an RlsPolicy with the specified attributes.
     * <p>
     * This constructor initialises the core policy attributes. The WITH CHECK expression
     * and roles list must be set separately via their respective setter methods.
     *
     * @param schemaName       the schema name containing the table
     * @param tableName        the table name to which this policy applies
     * @param policyName       the unique policy name
     * @param command          the SQL command this policy applies to (cannot be null)
     * @param permissive       true for permissive policy, false for restrictive policy
     * @param usingExpression  the USING clause expression (may be null)
     */
    public RlsPolicy(String schemaName, String tableName, String policyName,
                     Command command, boolean permissive, String usingExpression) {
        this.schemaName = schemaName;
        this.tableName = tableName;
        this.policyName = policyName;
        this.command = command;
        this.permissive = permissive;
        this.usingExpression = usingExpression;
    }

    // Getters and Setters

    /**
     * Returns the schema name containing the table.
     *
     * @return the schema name
     */
    public String getSchemaName() {
        return schemaName;
    }

    /**
     * Sets the schema name containing the table.
     *
     * @param schemaName the schema name
     */
    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    /**
     * Returns the table name to which this policy applies.
     *
     * @return the table name
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Sets the table name to which this policy applies.
     *
     * @param tableName the table name
     */
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    /**
     * Returns the policy name.
     *
     * @return the policy name (unique within the table)
     */
    public String getPolicyName() {
        return policyName;
    }

    /**
     * Sets the policy name.
     *
     * @param policyName the policy name (must be unique within the table)
     */
    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    /**
     * Returns the SQL command to which this policy applies.
     *
     * @return the command (SELECT, INSERT, UPDATE, DELETE, or ALL)
     */
    public Command getCommand() {
        return command;
    }

    /**
     * Sets the SQL command to which this policy applies.
     *
     * @param command the command type
     */
    public void setCommand(Command command) {
        this.command = command;
    }

    /**
     * Checks whether this is a permissive or restrictive policy.
     * <p>
     * Permissive policies grant access to rows that match their conditions (OR'ed together).
     * Restrictive policies deny access to rows that don't match their conditions (AND'ed together).
     *
     * @return true if this is a permissive policy, false if restrictive
     */
    public boolean isPermissive() {
        return permissive;
    }

    /**
     * Sets whether this is a permissive or restrictive policy.
     *
     * @param permissive true for permissive, false for restrictive
     */
    public void setPermissive(boolean permissive) {
        this.permissive = permissive;
    }

    /**
     * Returns the USING expression that determines row visibility.
     * <p>
     * This expression is evaluated for SELECT, UPDATE, and DELETE commands to determine
     * which existing rows are visible to the current user. Only rows for which the expression
     * evaluates to true are accessible.
     *
     * @return the USING clause SQL expression, or null if not defined
     */
    public String getUsingExpression() {
        return usingExpression;
    }

    /**
     * Sets the USING expression that determines row visibility.
     *
     * @param usingExpression the USING clause SQL expression
     */
    public void setUsingExpression(String usingExpression) {
        this.usingExpression = usingExpression;
    }

    /**
     * Returns the WITH CHECK expression that controls row modifications.
     * <p>
     * This expression is evaluated for INSERT and UPDATE commands to determine whether
     * new or modified rows can be added to the table. Only rows for which the expression
     * evaluates to true can be inserted or updated.
     * <p>
     * If not specified for a policy, PostgreSQL uses the USING expression as the WITH CHECK
     * expression by default.
     *
     * @return the WITH CHECK clause SQL expression, or null if not explicitly defined
     */
    public String getWithCheckExpression() {
        return withCheckExpression;
    }

    /**
     * Sets the WITH CHECK expression that controls row modifications.
     *
     * @param withCheckExpression the WITH CHECK clause SQL expression
     */
    public void setWithCheckExpression(String withCheckExpression) {
        this.withCheckExpression = withCheckExpression;
    }

    /**
     * Returns the list of database roles to which this policy applies.
     * <p>
     * If the list is empty, the policy applies to all roles (PUBLIC). Otherwise, the policy
     * only applies when one of the specified roles is active for the current database user.
     *
     * @return the list of role names, never null but may be empty for PUBLIC
     */
    public List<String> getRoles() {
        return roles;
    }

    /**
     * Sets the list of database roles to which this policy applies.
     *
     * @param roles the list of role names (empty list means PUBLIC)
     */
    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    // Helper Methods

    /**
     * Returns the fully qualified table name in schema.table format.
     * <p>
     * This is useful for display purposes and for constructing SQL queries that reference
     * the table across different schemas.
     *
     * @return the table name in "schema.table" format (e.g., "public.users")
     */
    public String getFullyQualifiedTableName() {
        return schemaName + "." + tableName;
    }

    /**
     * Returns the command display name for use in dashboard templates.
     * <p>
     * This provides a human-readable representation of the command type.
     *
     * @return the command display name (e.g., "SELECT", "ALL"), or "ALL" if command is null
     */
    public String getCommandDisplay() {
        return command != null ? command.getDisplayName() : "ALL";
    }

    /**
     * Returns the Bootstrap CSS class for styling the command badge in the dashboard.
     * <p>
     * The mapping is:
     * <ul>
     *   <li>ALL → bg-primary (blue)</li>
     *   <li>SELECT → bg-success (green)</li>
     *   <li>INSERT → bg-info (cyan)</li>
     *   <li>UPDATE → bg-warning text-dark (yellow with dark text)</li>
     *   <li>DELETE → bg-danger (red)</li>
     *   <li>null → bg-secondary (grey)</li>
     * </ul>
     *
     * @return the Bootstrap CSS class name for the command badge
     */
    public String getCommandCssClass() {
        if (command == null) return "bg-secondary";
        return switch (command) {
            case ALL -> "bg-primary";
            case SELECT -> "bg-success";
            case INSERT -> "bg-info";
            case UPDATE -> "bg-warning text-dark";
            case DELETE -> "bg-danger";
        };
    }

    /**
     * Returns the policy type as a display string for dashboard templates.
     *
     * @return "PERMISSIVE" if the policy is permissive, "RESTRICTIVE" otherwise
     */
    public String getPolicyTypeDisplay() {
        return permissive ? "PERMISSIVE" : "RESTRICTIVE";
    }

    /**
     * Returns the Bootstrap CSS class for styling the policy type badge.
     * <p>
     * Permissive policies use "bg-info" (cyan), while restrictive policies use
     * "bg-warning text-dark" (yellow with dark text).
     *
     * @return the Bootstrap CSS class name for the policy type badge
     */
    public String getPolicyTypeCssClass() {
        return permissive ? "bg-info" : "bg-warning text-dark";
    }

    /**
     * Returns the roles as a comma-separated string for dashboard display.
     * <p>
     * If no roles are specified (empty list), returns "PUBLIC" to indicate the policy
     * applies to all database users.
     *
     * @return comma-separated role names, or "PUBLIC" if the roles list is empty
     */
    public String getRolesDisplay() {
        if (roles == null || roles.isEmpty()) {
            return "PUBLIC";
        }
        return String.join(", ", roles);
    }

    /**
     * Returns a truncated version of the USING expression for compact dashboard display.
     * <p>
     * If the expression is null or empty, returns "-". If it exceeds the specified maximum
     * length, it is truncated with "..." appended. Otherwise, the full expression is returned.
     *
     * @param maxLength the maximum length before truncation (including the "..." suffix)
     * @return the truncated USING expression, "-" if not defined, or the full expression if short enough
     */
    public String getUsingExpressionShort(int maxLength) {
        if (usingExpression == null || usingExpression.isEmpty()) {
            return "-";
        }
        if (usingExpression.length() <= maxLength) {
            return usingExpression;
        }
        return usingExpression.substring(0, maxLength - 3) + "...";
    }

    /**
     * Returns a truncated version of the WITH CHECK expression for compact dashboard display.
     * <p>
     * If the expression is null or empty, returns "-". If it exceeds the specified maximum
     * length, it is truncated with "..." appended. Otherwise, the full expression is returned.
     *
     * @param maxLength the maximum length before truncation (including the "..." suffix)
     * @return the truncated WITH CHECK expression, "-" if not defined, or the full expression if short enough
     */
    public String getWithCheckExpressionShort(int maxLength) {
        if (withCheckExpression == null || withCheckExpression.isEmpty()) {
            return "-";
        }
        if (withCheckExpression.length() <= maxLength) {
            return withCheckExpression;
        }
        return withCheckExpression.substring(0, maxLength - 3) + "...";
    }

    /**
     * Checks whether this policy has an explicitly defined WITH CHECK expression.
     * <p>
     * If a policy does not have a WITH CHECK expression, PostgreSQL uses the USING
     * expression for WITH CHECK validation by default. This method only returns true
     * if a WITH CHECK expression is explicitly defined in the policy.
     *
     * @return true if the WITH CHECK expression is defined and non-empty, false otherwise
     */
    public boolean hasWithCheck() {
        return withCheckExpression != null && !withCheckExpression.isEmpty();
    }
}
