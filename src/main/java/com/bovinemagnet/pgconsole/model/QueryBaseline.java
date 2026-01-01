package com.bovinemagnet.pgconsole.model;

/**
 * Represents a query comparison between two time periods (baseline).
 * Used for the top movers report in pg_stat_statements management.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class QueryBaseline {

    public enum MovementType {
        NEW_QUERY,      // Query appeared in current period
        REMOVED,        // Query disappeared from current period
        INCREASED,      // Query increased in calls/time
        DECREASED,      // Query decreased in calls/time
        STABLE          // Query stable within threshold
    }

    private String queryId;
    private String queryText;

    // Previous period stats
    private long previousCalls;
    private double previousTotalTime;
    private double previousMeanTime;

    // Current period stats
    private long currentCalls;
    private double currentTotalTime;
    private double currentMeanTime;

    // Deltas
    private long callsDelta;
    private double totalTimeDelta;
    private double meanTimeDelta;
    private double callsChangePercent;
    private double totalTimeChangePercent;
    private double meanTimeChangePercent;

    private MovementType movementType;
    private double impactScore; // Combined score for ranking

    public QueryBaseline() {
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
        return cleaned.length() > 80 ? cleaned.substring(0, 80) + "..." : cleaned;
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

    public double getPreviousMeanTime() {
        return previousMeanTime;
    }

    public void setPreviousMeanTime(double previousMeanTime) {
        this.previousMeanTime = previousMeanTime;
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

    public double getCurrentMeanTime() {
        return currentMeanTime;
    }

    public void setCurrentMeanTime(double currentMeanTime) {
        this.currentMeanTime = currentMeanTime;
    }

    public long getCallsDelta() {
        return callsDelta;
    }

    public void setCallsDelta(long callsDelta) {
        this.callsDelta = callsDelta;
    }

    public double getTotalTimeDelta() {
        return totalTimeDelta;
    }

    public void setTotalTimeDelta(double totalTimeDelta) {
        this.totalTimeDelta = totalTimeDelta;
    }

    public double getMeanTimeDelta() {
        return meanTimeDelta;
    }

    public void setMeanTimeDelta(double meanTimeDelta) {
        this.meanTimeDelta = meanTimeDelta;
    }

    public double getCallsChangePercent() {
        return callsChangePercent;
    }

    public void setCallsChangePercent(double callsChangePercent) {
        this.callsChangePercent = callsChangePercent;
    }

    public double getTotalTimeChangePercent() {
        return totalTimeChangePercent;
    }

    public void setTotalTimeChangePercent(double totalTimeChangePercent) {
        this.totalTimeChangePercent = totalTimeChangePercent;
    }

    public double getMeanTimeChangePercent() {
        return meanTimeChangePercent;
    }

    public void setMeanTimeChangePercent(double meanTimeChangePercent) {
        this.meanTimeChangePercent = meanTimeChangePercent;
    }

    public MovementType getMovementType() {
        return movementType;
    }

    public void setMovementType(MovementType movementType) {
        this.movementType = movementType;
    }

    public double getImpactScore() {
        return impactScore;
    }

    public void setImpactScore(double impactScore) {
        this.impactScore = impactScore;
    }

    public String getMovementTypeCssClass() {
        return switch (movementType) {
            case NEW_QUERY -> "bg-success";
            case REMOVED -> "bg-secondary";
            case INCREASED -> "bg-danger";
            case DECREASED -> "bg-info";
            case STABLE -> "bg-light text-dark";
        };
    }

    public String getMovementTypeDisplay() {
        return switch (movementType) {
            case NEW_QUERY -> "New";
            case REMOVED -> "Removed";
            case INCREASED -> "Increased";
            case DECREASED -> "Decreased";
            case STABLE -> "Stable";
        };
    }

    public String getCallsChangeFormatted() {
        if (movementType == MovementType.NEW_QUERY) return "New";
        if (movementType == MovementType.REMOVED) return "Removed";
        String sign = callsChangePercent >= 0 ? "+" : "";
        return String.format("%s%.0f%%", sign, callsChangePercent);
    }

    public String getTotalTimeChangeFormatted() {
        if (movementType == MovementType.NEW_QUERY) return "New";
        if (movementType == MovementType.REMOVED) return "Removed";
        String sign = totalTimeChangePercent >= 0 ? "+" : "";
        return String.format("%s%.0f%%", sign, totalTimeChangePercent);
    }

    public String getMeanTimeChangeFormatted() {
        if (movementType == MovementType.NEW_QUERY) return "—";
        if (movementType == MovementType.REMOVED) return "—";
        String sign = meanTimeChangePercent >= 0 ? "+" : "";
        return String.format("%s%.0f%%", sign, meanTimeChangePercent);
    }

    public String formatTime(double ms) {
        if (ms >= 1000) {
            return String.format("%.2f s", ms / 1000);
        } else {
            return String.format("%.2f ms", ms);
        }
    }

    public String getPreviousTotalTimeFormatted() {
        return formatTime(previousTotalTime);
    }

    public String getCurrentTotalTimeFormatted() {
        return formatTime(currentTotalTime);
    }

    public String getTotalTimeDeltaFormatted() {
        String sign = totalTimeDelta >= 0 ? "+" : "";
        return sign + formatTime(totalTimeDelta);
    }

    public boolean isSignificantIncrease() {
        return movementType == MovementType.INCREASED && totalTimeChangePercent > 50;
    }

    public boolean isSignificantDecrease() {
        return movementType == MovementType.DECREASED && totalTimeChangePercent < -50;
    }
}
