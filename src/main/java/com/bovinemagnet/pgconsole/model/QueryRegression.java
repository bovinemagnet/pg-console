package com.bovinemagnet.pgconsole.model;

/**
 * Represents a query that has shown performance regression.
 * Compares query performance between two time windows.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class QueryRegression {

    public enum Severity {
        CRITICAL,   // >100% slower
        HIGH,       // >50% slower
        MEDIUM,     // >25% slower
        LOW         // >threshold but <25%
    }

    private String queryId;
    private String queryText;

    // Previous period stats
    private double previousMeanTime;
    private long previousCalls;
    private double previousTotalTime;

    // Current period stats
    private double currentMeanTime;
    private long currentCalls;
    private double currentTotalTime;

    // Computed deltas
    private double meanTimeDelta;        // Absolute change in ms
    private double meanTimeChangePercent; // Percentage change
    private double totalTimeChangePercent;
    private long callsDelta;

    private Severity severity;

    public QueryRegression() {
    }

    public String getQueryId() {
        return queryId;
    }

    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }

    public String getQueryText() {
        return queryText;
    }

    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }

    public String getQueryPreview() {
        if (queryText == null) {
            return "";
        }
        String cleaned = queryText.replaceAll("\\s+", " ").trim();
        return cleaned.length() > 100 ? cleaned.substring(0, 100) + "..." : cleaned;
    }

    public double getPreviousMeanTime() {
        return previousMeanTime;
    }

    public void setPreviousMeanTime(double previousMeanTime) {
        this.previousMeanTime = previousMeanTime;
    }

    public long getPreviousCalls() {
        return previousCalls;
    }

    public void setPreviousCalls(long previousCalls) {
        this.previousCalls = previousCalls;
    }

    public double getPreviousTotalTime() {
        return previousTotalTime;
    }

    public void setPreviousTotalTime(double previousTotalTime) {
        this.previousTotalTime = previousTotalTime;
    }

    public double getCurrentMeanTime() {
        return currentMeanTime;
    }

    public void setCurrentMeanTime(double currentMeanTime) {
        this.currentMeanTime = currentMeanTime;
    }

    public long getCurrentCalls() {
        return currentCalls;
    }

    public void setCurrentCalls(long currentCalls) {
        this.currentCalls = currentCalls;
    }

    public double getCurrentTotalTime() {
        return currentTotalTime;
    }

    public void setCurrentTotalTime(double currentTotalTime) {
        this.currentTotalTime = currentTotalTime;
    }

    public double getMeanTimeDelta() {
        return meanTimeDelta;
    }

    public void setMeanTimeDelta(double meanTimeDelta) {
        this.meanTimeDelta = meanTimeDelta;
    }

    public double getMeanTimeChangePercent() {
        return meanTimeChangePercent;
    }

    public void setMeanTimeChangePercent(double meanTimeChangePercent) {
        this.meanTimeChangePercent = meanTimeChangePercent;
    }

    public double getTotalTimeChangePercent() {
        return totalTimeChangePercent;
    }

    public void setTotalTimeChangePercent(double totalTimeChangePercent) {
        this.totalTimeChangePercent = totalTimeChangePercent;
    }

    public long getCallsDelta() {
        return callsDelta;
    }

    public void setCallsDelta(long callsDelta) {
        this.callsDelta = callsDelta;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    /**
     * Returns the severity CSS class for Bootstrap badges.
     */
    public String getSeverityCssClass() {
        return switch (severity) {
            case CRITICAL -> "bg-danger";
            case HIGH -> "bg-warning text-dark";
            case MEDIUM -> "bg-info";
            case LOW -> "bg-secondary";
        };
    }

    /**
     * Formats the mean time change as a percentage string with direction indicator.
     */
    public String getMeanTimeChangeFormatted() {
        String sign = meanTimeChangePercent >= 0 ? "+" : "";
        return String.format("%s%.1f%%", sign, meanTimeChangePercent);
    }

    /**
     * Formats the previous mean time.
     */
    public String getPreviousMeanTimeFormatted() {
        return formatTime(previousMeanTime);
    }

    /**
     * Formats the current mean time.
     */
    public String getCurrentMeanTimeFormatted() {
        return formatTime(currentMeanTime);
    }

    /**
     * Formats the calls delta with direction indicator.
     */
    public String getCallsDeltaFormatted() {
        String sign = callsDelta >= 0 ? "+" : "";
        return String.format("%s%d", sign, callsDelta);
    }

    /**
     * Returns true if this is a regression (slower).
     */
    public boolean isRegression() {
        return meanTimeChangePercent > 0;
    }

    /**
     * Returns true if this is an improvement (faster).
     */
    public boolean isImprovement() {
        return meanTimeChangePercent < 0;
    }

    private String formatTime(double ms) {
        if (ms >= 1000) {
            return String.format("%.2f s", ms / 1000);
        } else {
            return String.format("%.2f ms", ms);
        }
    }

    /**
     * Calculates severity based on percentage change.
     */
    public static Severity calculateSeverity(double changePercent) {
        if (changePercent >= 100) {
            return Severity.CRITICAL;
        } else if (changePercent >= 50) {
            return Severity.HIGH;
        } else if (changePercent >= 25) {
            return Severity.MEDIUM;
        } else {
            return Severity.LOW;
        }
    }
}
