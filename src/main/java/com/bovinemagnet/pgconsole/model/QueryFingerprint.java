package com.bovinemagnet.pgconsole.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a group of similar queries identified by their fingerprint.
 * Queries with the same fingerprint have the same structure but may have different literal values.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class QueryFingerprint {

    private String fingerprint;
    private String normalisedQuery;
    private List<SlowQuery> instances = new ArrayList<>();

    // Aggregated statistics
    private long totalCalls;
    private double totalTime;
    private double avgMeanTime;
    private long totalRows;

    public QueryFingerprint() {
    }

    public QueryFingerprint(String fingerprint, String normalisedQuery) {
        this.fingerprint = fingerprint;
        this.normalisedQuery = normalisedQuery;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public String getNormalisedQuery() {
        return normalisedQuery;
    }

    public void setNormalisedQuery(String normalisedQuery) {
        this.normalisedQuery = normalisedQuery;
    }

    public List<SlowQuery> getInstances() {
        return instances;
    }

    public void setInstances(List<SlowQuery> instances) {
        this.instances = instances;
    }

    public void addInstance(SlowQuery query) {
        this.instances.add(query);
    }

    public long getTotalCalls() {
        return totalCalls;
    }

    public void setTotalCalls(long totalCalls) {
        this.totalCalls = totalCalls;
    }

    public double getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(double totalTime) {
        this.totalTime = totalTime;
    }

    public double getAvgMeanTime() {
        return avgMeanTime;
    }

    public void setAvgMeanTime(double avgMeanTime) {
        this.avgMeanTime = avgMeanTime;
    }

    public long getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(long totalRows) {
        this.totalRows = totalRows;
    }

    /**
     * Returns the number of query instances in this fingerprint group.
     */
    public int getInstanceCount() {
        return instances.size();
    }

    /**
     * Returns a short version of the normalised query for display.
     */
    public String getShortNormalisedQuery() {
        if (normalisedQuery == null) {
            return "";
        }
        String cleaned = normalisedQuery.replaceAll("\\s+", " ").trim();
        if (cleaned.length() <= 100) {
            return cleaned;
        }
        return cleaned.substring(0, 97) + "...";
    }

    /**
     * Formats the total time for display.
     */
    public String getTotalTimeFormatted() {
        if (totalTime < 1000) {
            return String.format("%.2f ms", totalTime);
        } else if (totalTime < 60000) {
            return String.format("%.2f s", totalTime / 1000);
        } else if (totalTime < 3600000) {
            return String.format("%.1f min", totalTime / 60000);
        } else {
            return String.format("%.1f h", totalTime / 3600000);
        }
    }

    /**
     * Formats the average mean time for display.
     */
    public String getAvgMeanTimeFormatted() {
        if (avgMeanTime < 1000) {
            return String.format("%.2f ms", avgMeanTime);
        } else if (avgMeanTime < 60000) {
            return String.format("%.2f s", avgMeanTime / 1000);
        } else {
            return String.format("%.1f min", avgMeanTime / 60000);
        }
    }

    /**
     * Recalculates aggregated statistics from instances.
     */
    public void recalculateStats() {
        this.totalCalls = 0;
        this.totalTime = 0;
        this.totalRows = 0;
        double sumMeanTime = 0;

        for (SlowQuery q : instances) {
            this.totalCalls += q.getTotalCalls();
            this.totalTime += q.getTotalTime();
            this.totalRows += q.getRows();
            sumMeanTime += q.getMeanTime();
        }

        if (!instances.isEmpty()) {
            this.avgMeanTime = sumMeanTime / instances.size();
        }
    }
}
