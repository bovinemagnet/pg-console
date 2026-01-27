package com.bovinemagnet.pgconsole.model;

import java.time.Instant;

/**
 * Represents a prepared statement from the PostgreSQL system view
 * {@code pg_prepared_statements}.
 * <p>
 * This class captures information about prepared statements that exist
 * in the current session, including the statement text, parameter types,
 * and creation time.
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see <a href="https://www.postgresql.org/docs/current/view-pg-prepared-statements.html">pg_prepared_statements Documentation</a>
 */
public class PreparedStatementInfo {

    /** Name of the prepared statement */
    private String name;

    /** The SQL statement text */
    private String statement;

    /** Time when the statement was prepared */
    private Instant prepareTime;

    /** Parameter types (as array of OIDs) */
    private String parameterTypes;

    /** Whether the statement was prepared from SQL (vs protocol) */
    private boolean fromSql;

    /** Number of parameters */
    private int parameterCount;

    /**
     * Constructs a new PreparedStatementInfo instance.
     */
    public PreparedStatementInfo() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatement() {
        return statement;
    }

    public void setStatement(String statement) {
        this.statement = statement;
    }

    public Instant getPrepareTime() {
        return prepareTime;
    }

    public void setPrepareTime(Instant prepareTime) {
        this.prepareTime = prepareTime;
    }

    public String getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(String parameterTypes) {
        this.parameterTypes = parameterTypes;
        // Count parameters from the types array
        if (parameterTypes != null && !parameterTypes.isEmpty() && !parameterTypes.equals("{}")) {
            this.parameterCount = parameterTypes.split(",").length;
        } else {
            this.parameterCount = 0;
        }
    }

    public boolean isFromSql() {
        return fromSql;
    }

    public void setFromSql(boolean fromSql) {
        this.fromSql = fromSql;
    }

    public int getParameterCount() {
        return parameterCount;
    }

    /**
     * Returns a truncated version of the statement for display.
     *
     * @param maxLength maximum length before truncation
     * @return truncated statement
     */
    public String getStatementTruncated(int maxLength) {
        if (statement == null) return "";
        // Normalise whitespace
        String normalised = statement.replaceAll("\\s+", " ").trim();
        if (normalised.length() <= maxLength) return normalised;
        return normalised.substring(0, maxLength - 3) + "...";
    }

    /**
     * Returns the statement type (SELECT, INSERT, UPDATE, DELETE, etc.).
     *
     * @return statement type or "Unknown"
     */
    public String getStatementType() {
        if (statement == null || statement.isEmpty()) return "Unknown";
        String upper = statement.trim().toUpperCase();
        if (upper.startsWith("SELECT")) return "SELECT";
        if (upper.startsWith("INSERT")) return "INSERT";
        if (upper.startsWith("UPDATE")) return "UPDATE";
        if (upper.startsWith("DELETE")) return "DELETE";
        if (upper.startsWith("WITH")) return "CTE";
        if (upper.startsWith("CALL")) return "CALL";
        return "Other";
    }

    /**
     * Returns Bootstrap CSS class for the statement type badge.
     *
     * @return Bootstrap background class
     */
    public String getStatementTypeCssClass() {
        return switch (getStatementType()) {
            case "SELECT" -> "bg-success";
            case "INSERT" -> "bg-primary";
            case "UPDATE" -> "bg-warning text-dark";
            case "DELETE" -> "bg-danger";
            case "CTE" -> "bg-info";
            case "CALL" -> "bg-secondary";
            default -> "bg-secondary";
        };
    }

    /**
     * Returns Bootstrap CSS class for the source badge.
     *
     * @return Bootstrap background class
     */
    public String getSourceCssClass() {
        return fromSql ? "bg-info" : "bg-secondary";
    }

    /**
     * Returns the source display text.
     *
     * @return source display text
     */
    public String getSourceDisplay() {
        return fromSql ? "SQL" : "Protocol";
    }

    /**
     * Returns the age of the prepared statement as a human-readable string.
     *
     * @return formatted age string
     */
    public String getAgeFormatted() {
        if (prepareTime == null) return "Unknown";
        long seconds = java.time.Duration.between(prepareTime, Instant.now()).getSeconds();
        if (seconds < 60) return seconds + " seconds";
        if (seconds < 3600) return (seconds / 60) + " minutes";
        if (seconds < 86400) return (seconds / 3600) + " hours";
        return (seconds / 86400) + " days";
    }
}
