package com.bovinemagnet.pgconsole.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Row-Level Security (RLS) policy from pg_policy.
 * <p>
 * Contains the policy definition including the USING and WITH CHECK
 * expressions that control row visibility and modification.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class RlsPolicy {

    /**
     * SQL commands that a policy applies to.
     */
    public enum Command {
        ALL("ALL", "Applies to all commands"),
        SELECT("SELECT", "Applies to SELECT queries"),
        INSERT("INSERT", "Applies to INSERT statements"),
        UPDATE("UPDATE", "Applies to UPDATE statements"),
        DELETE("DELETE", "Applies to DELETE statements");

        private final String displayName;
        private final String description;

        Command(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        /**
         * Parses a command from the PostgreSQL policy command string.
         *
         * @param cmd the command string from pg_policy
         * @return the corresponding Command enum
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

    private String schemaName;
    private String tableName;
    private String policyName;
    private Command command;
    private boolean permissive;
    private String usingExpression;
    private String withCheckExpression;
    private List<String> roles = new ArrayList<>();

    /**
     * Default constructor.
     */
    public RlsPolicy() {
    }

    /**
     * Constructs an RlsPolicy with the specified attributes.
     *
     * @param schemaName       the schema containing the table
     * @param tableName        the table name
     * @param policyName       the policy name
     * @param command          the command this policy applies to
     * @param permissive       whether this is a permissive policy
     * @param usingExpression  the USING expression
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

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public Command getCommand() {
        return command;
    }

    public void setCommand(Command command) {
        this.command = command;
    }

    public boolean isPermissive() {
        return permissive;
    }

    public void setPermissive(boolean permissive) {
        this.permissive = permissive;
    }

    public String getUsingExpression() {
        return usingExpression;
    }

    public void setUsingExpression(String usingExpression) {
        this.usingExpression = usingExpression;
    }

    public String getWithCheckExpression() {
        return withCheckExpression;
    }

    public void setWithCheckExpression(String withCheckExpression) {
        this.withCheckExpression = withCheckExpression;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    // Helper Methods

    /**
     * Returns the fully qualified table name.
     *
     * @return schema.table format
     */
    public String getFullyQualifiedTableName() {
        return schemaName + "." + tableName;
    }

    /**
     * Returns the command display name.
     *
     * @return command for display
     */
    public String getCommandDisplay() {
        return command != null ? command.getDisplayName() : "ALL";
    }

    /**
     * Returns the CSS class for the command badge.
     *
     * @return CSS class name
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
     * Returns the policy type (permissive or restrictive).
     *
     * @return policy type for display
     */
    public String getPolicyTypeDisplay() {
        return permissive ? "PERMISSIVE" : "RESTRICTIVE";
    }

    /**
     * Returns the CSS class for the policy type badge.
     *
     * @return CSS class name
     */
    public String getPolicyTypeCssClass() {
        return permissive ? "bg-info" : "bg-warning text-dark";
    }

    /**
     * Returns the roles as a comma-separated string.
     *
     * @return roles for display
     */
    public String getRolesDisplay() {
        if (roles == null || roles.isEmpty()) {
            return "PUBLIC";
        }
        return String.join(", ", roles);
    }

    /**
     * Returns a truncated version of the USING expression for display.
     *
     * @param maxLength maximum length before truncation
     * @return truncated expression
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
     * Returns a truncated version of the WITH CHECK expression for display.
     *
     * @param maxLength maximum length before truncation
     * @return truncated expression
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
     * Checks if this policy has a WITH CHECK expression.
     *
     * @return true if WITH CHECK is defined
     */
    public boolean hasWithCheck() {
        return withCheckExpression != null && !withCheckExpression.isEmpty();
    }
}
