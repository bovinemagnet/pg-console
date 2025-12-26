package com.bovinemagnet.pgconsole.model;

import java.time.Instant;

/**
 * Query metrics snapshot for historical tracking.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class QueryMetricsHistory {
    private Long id;
    private Instant sampledAt;

    // Query identification
    private String queryId;
    private String queryText;

    // Cumulative counters
    private long totalCalls;
    private double totalTimeMs;
    private long totalRows;

    // Point-in-time stats
    private double meanTimeMs;
    private Double minTimeMs;
    private Double maxTimeMs;
    private Double stddevTimeMs;

    // Block I/O
    private Long sharedBlksHit;
    private Long sharedBlksRead;
    private Long tempBlksWritten;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getSampledAt() {
        return sampledAt;
    }

    public void setSampledAt(Instant sampledAt) {
        this.sampledAt = sampledAt;
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

    public long getTotalCalls() {
        return totalCalls;
    }

    public void setTotalCalls(long totalCalls) {
        this.totalCalls = totalCalls;
    }

    public double getTotalTimeMs() {
        return totalTimeMs;
    }

    public void setTotalTimeMs(double totalTimeMs) {
        this.totalTimeMs = totalTimeMs;
    }

    public long getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(long totalRows) {
        this.totalRows = totalRows;
    }

    public double getMeanTimeMs() {
        return meanTimeMs;
    }

    public void setMeanTimeMs(double meanTimeMs) {
        this.meanTimeMs = meanTimeMs;
    }

    public Double getMinTimeMs() {
        return minTimeMs;
    }

    public void setMinTimeMs(Double minTimeMs) {
        this.minTimeMs = minTimeMs;
    }

    public Double getMaxTimeMs() {
        return maxTimeMs;
    }

    public void setMaxTimeMs(Double maxTimeMs) {
        this.maxTimeMs = maxTimeMs;
    }

    public Double getStddevTimeMs() {
        return stddevTimeMs;
    }

    public void setStddevTimeMs(Double stddevTimeMs) {
        this.stddevTimeMs = stddevTimeMs;
    }

    public Long getSharedBlksHit() {
        return sharedBlksHit;
    }

    public void setSharedBlksHit(Long sharedBlksHit) {
        this.sharedBlksHit = sharedBlksHit;
    }

    public Long getSharedBlksRead() {
        return sharedBlksRead;
    }

    public void setSharedBlksRead(Long sharedBlksRead) {
        this.sharedBlksRead = sharedBlksRead;
    }

    public Long getTempBlksWritten() {
        return tempBlksWritten;
    }

    public void setTempBlksWritten(Long tempBlksWritten) {
        this.tempBlksWritten = tempBlksWritten;
    }
}
