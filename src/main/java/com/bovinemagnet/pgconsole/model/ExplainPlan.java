package com.bovinemagnet.pgconsole.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an EXPLAIN ANALYZE output for a query.
 * Contains both text and structured plan information.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class ExplainPlan {

    private String queryId;
    private String query;
    private String planText;
    private List<String> planLines = new ArrayList<>();
    private LocalDateTime generatedAt;
    private boolean analyse;
    private boolean buffers;
    private String error;

    public ExplainPlan() {
        this.generatedAt = LocalDateTime.now();
    }

    public ExplainPlan(String queryId, String query) {
        this.queryId = queryId;
        this.query = query;
        this.generatedAt = LocalDateTime.now();
    }

    public String getQueryId() {
        return queryId;
    }

    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getPlanText() {
        return planText;
    }

    public void setPlanText(String planText) {
        this.planText = planText;
    }

    public List<String> getPlanLines() {
        return planLines;
    }

    public void setPlanLines(List<String> planLines) {
        this.planLines = planLines;
    }

    public void addPlanLine(String line) {
        this.planLines.add(line);
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public boolean isAnalyse() {
        return analyse;
    }

    public void setAnalyse(boolean analyse) {
        this.analyse = analyse;
    }

    public boolean isBuffers() {
        return buffers;
    }

    public void setBuffers(boolean buffers) {
        this.buffers = buffers;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public boolean hasError() {
        return error != null && !error.isEmpty();
    }

    /**
     * Returns a formatted timestamp string.
     */
    public String getGeneratedAtFormatted() {
        if (generatedAt == null) {
            return "";
        }
        return generatedAt.toString().replace("T", " ").substring(0, 19);
    }

    /**
     * Returns the explain options used as a descriptive string.
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
