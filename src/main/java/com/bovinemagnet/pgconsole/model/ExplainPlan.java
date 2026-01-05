package com.bovinemagnet.pgconsole.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the execution plan output from PostgreSQL's EXPLAIN command.
 * <p>
 * This class encapsulates the results of running EXPLAIN (optionally with ANALYZE and BUFFERS)
 * on a query. It stores both the raw plan text and a structured line-by-line representation,
 * making it suitable for display in web templates or further processing.
 * </p>
 * <p>
 * The explain plan can be generated with different options:
 * </p>
 * <ul>
 * <li><strong>EXPLAIN</strong> - Shows the query planner's estimated execution plan without running the query</li>
 * <li><strong>EXPLAIN ANALYZE</strong> - Executes the query and shows actual execution statistics</li>
 * <li><strong>EXPLAIN (ANALYZE, BUFFERS)</strong> - Includes buffer usage statistics (cache hits/misses)</li>
 * </ul>
 * <p>
 * This model is typically populated by the PostgresService when a user requests detailed
 * query analysis from the slow queries dashboard.
 * </p>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see com.bovinemagnet.pgconsole.service.PostgresService
 */
public class ExplainPlan {

    /**
     * The unique identifier of the query from pg_stat_statements.
     * Corresponds to the queryid column in PostgreSQL's statistics views.
     */
    private String queryId;

    /**
     * The SQL query text that was explained.
     * This is the actual query that was submitted to EXPLAIN.
     */
    private String query;

    /**
     * The complete EXPLAIN output as a single text block.
     * Contains the entire plan including all nodes, costs, and statistics.
     */
    private String planText;

    /**
     * The EXPLAIN output split into individual lines for structured display.
     * Each line represents a node or detail in the execution plan tree.
     */
    private List<String> planLines = new ArrayList<>();

    /**
     * The timestamp when this explain plan was generated.
     * Set to the current time when the object is constructed.
     */
    private LocalDateTime generatedAt;

    /**
     * Indicates whether ANALYZE was used (query was actually executed).
     * When true, the plan contains actual execution times and row counts.
     * When false, the plan contains only estimates from the query planner.
     */
    private boolean analyse;

    /**
     * Indicates whether BUFFERS option was used.
     * When true, the plan includes buffer usage statistics showing
     * shared block hits, reads, dirtied, and written counts.
     */
    private boolean buffers;

    /**
     * Error message if the EXPLAIN operation failed.
     * Null or empty if the operation was successful.
     * May contain PostgreSQL error messages or permission issues.
     */
    private String error;

    /**
     * Constructs a new ExplainPlan with the current timestamp.
     * The generatedAt field is automatically set to the current time.
     */
    public ExplainPlan() {
        this.generatedAt = LocalDateTime.now();
    }

    /**
     * Constructs a new ExplainPlan for a specific query.
     *
     * @param queryId the unique identifier from pg_stat_statements
     * @param query the SQL query text to be explained
     */
    public ExplainPlan(String queryId, String query) {
        this.queryId = queryId;
        this.query = query;
        this.generatedAt = LocalDateTime.now();
    }

    /**
     * Returns the unique query identifier from pg_stat_statements.
     *
     * @return the query ID, or null if not set
     */
    public String getQueryId() {
        return queryId;
    }

    /**
     * Sets the unique query identifier.
     *
     * @param queryId the query ID from pg_stat_statements
     */
    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }

    /**
     * Returns the SQL query text that was explained.
     *
     * @return the query text, or null if not set
     */
    public String getQuery() {
        return query;
    }

    /**
     * Sets the SQL query text.
     *
     * @param query the SQL query to be explained
     */
    public void setQuery(String query) {
        this.query = query;
    }

    /**
     * Returns the complete EXPLAIN output as a single text block.
     *
     * @return the full plan text, or null if not set
     */
    public String getPlanText() {
        return planText;
    }

    /**
     * Sets the complete EXPLAIN output.
     *
     * @param planText the full plan text from PostgreSQL
     */
    public void setPlanText(String planText) {
        this.planText = planText;
    }

    /**
     * Returns the EXPLAIN output split into individual lines.
     * The returned list is mutable and can be modified.
     *
     * @return the list of plan lines, never null but may be empty
     */
    public List<String> getPlanLines() {
        return planLines;
    }

    /**
     * Sets the list of plan lines.
     *
     * @param planLines the list of plan lines to set
     */
    public void setPlanLines(List<String> planLines) {
        this.planLines = planLines;
    }

    /**
     * Adds a single line to the plan lines list.
     * This is a convenience method for building the plan incrementally.
     *
     * @param line the plan line to add
     */
    public void addPlanLine(String line) {
        this.planLines.add(line);
    }

    /**
     * Returns the timestamp when this explain plan was generated.
     *
     * @return the generation timestamp, never null for properly constructed instances
     */
    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    /**
     * Sets the generation timestamp.
     *
     * @param generatedAt the timestamp when the plan was generated
     */
    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    /**
     * Returns whether ANALYZE was used when generating the plan.
     * When true, the query was actually executed and the plan contains
     * real execution times and row counts rather than estimates.
     *
     * @return true if ANALYZE was used, false otherwise
     */
    public boolean isAnalyse() {
        return analyse;
    }

    /**
     * Sets whether ANALYZE was used.
     *
     * @param analyse true if ANALYZE was used, false otherwise
     */
    public void setAnalyse(boolean analyse) {
        this.analyse = analyse;
    }

    /**
     * Returns whether BUFFERS option was used when generating the plan.
     * When true, the plan includes buffer usage statistics.
     *
     * @return true if BUFFERS was used, false otherwise
     */
    public boolean isBuffers() {
        return buffers;
    }

    /**
     * Sets whether BUFFERS option was used.
     *
     * @param buffers true if BUFFERS was used, false otherwise
     */
    public void setBuffers(boolean buffers) {
        this.buffers = buffers;
    }

    /**
     * Returns the error message if the EXPLAIN operation failed.
     *
     * @return the error message, or null if the operation was successful
     */
    public String getError() {
        return error;
    }

    /**
     * Sets the error message.
     *
     * @param error the error message to set, or null to clear the error
     */
    public void setError(String error) {
        this.error = error;
    }

    /**
     * Checks whether this explain plan has an error.
     *
     * @return true if an error message is present and non-empty, false otherwise
     */
    public boolean hasError() {
        return error != null && !error.isEmpty();
    }

    /**
     * Returns a formatted timestamp string suitable for display in web templates.
     * The format is "yyyy-MM-dd HH:mm:ss" (e.g., "2026-01-05 14:30:45").
     *
     * @return the formatted timestamp, or an empty string if generatedAt is null
     */
    public String getGeneratedAtFormatted() {
        if (generatedAt == null) {
            return "";
        }
        return generatedAt.toString().replace("T", " ").substring(0, 19);
    }

    /**
     * Returns a human-readable description of the EXPLAIN options that were used.
     * <p>
     * Examples:
     * </p>
     * <ul>
     * <li>"EXPLAIN" - basic explain without execution</li>
     * <li>"EXPLAIN ANALYZE" - explain with query execution</li>
     * <li>"EXPLAIN ANALYZE BUFFERS" - explain with execution and buffer statistics</li>
     * </ul>
     *
     * @return a string describing the EXPLAIN command and its options
     */
    public String getOptionsDescription() {
        StringBuilder sb = new StringBuilder("EXPLAIN");
        if (analyse) {
            sb.append(" ANALYZE");
        }
        if (buffers) {
            sb.append(" BUFFERS");
        }
        return sb.toString();
    }
}
