package com.bovinemagnet.pgconsole.model;

import java.time.Instant;

/**
 * System-level metrics snapshot for historical tracking.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class SystemMetricsHistory {
    private Long id;
    private Instant sampledAt;

    // Connection metrics
    private int totalConnections;
    private int maxConnections;
    private int activeQueries;
    private int idleConnections;
    private int idleInTransaction;

    // Blocking metrics
    private int blockedQueries;
    private Double longestQuerySeconds;
    private Double longestTransactionSeconds;

    // Cache metrics
    private Double cacheHitRatio;

    // Size
    private Long totalDatabaseSizeBytes;

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

    public int getTotalConnections() {
        return totalConnections;
    }

    public void setTotalConnections(int totalConnections) {
        this.totalConnections = totalConnections;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public int getActiveQueries() {
        return activeQueries;
    }

    public void setActiveQueries(int activeQueries) {
        this.activeQueries = activeQueries;
    }

    public int getIdleConnections() {
        return idleConnections;
    }

    public void setIdleConnections(int idleConnections) {
        this.idleConnections = idleConnections;
    }

    public int getIdleInTransaction() {
        return idleInTransaction;
    }

    public void setIdleInTransaction(int idleInTransaction) {
        this.idleInTransaction = idleInTransaction;
    }

    public int getBlockedQueries() {
        return blockedQueries;
    }

    public void setBlockedQueries(int blockedQueries) {
        this.blockedQueries = blockedQueries;
    }

    public Double getLongestQuerySeconds() {
        return longestQuerySeconds;
    }

    public void setLongestQuerySeconds(Double longestQuerySeconds) {
        this.longestQuerySeconds = longestQuerySeconds;
    }

    public Double getLongestTransactionSeconds() {
        return longestTransactionSeconds;
    }

    public void setLongestTransactionSeconds(Double longestTransactionSeconds) {
        this.longestTransactionSeconds = longestTransactionSeconds;
    }

    public Double getCacheHitRatio() {
        return cacheHitRatio;
    }

    public void setCacheHitRatio(Double cacheHitRatio) {
        this.cacheHitRatio = cacheHitRatio;
    }

    public Long getTotalDatabaseSizeBytes() {
        return totalDatabaseSizeBytes;
    }

    public void setTotalDatabaseSizeBytes(Long totalDatabaseSizeBytes) {
        this.totalDatabaseSizeBytes = totalDatabaseSizeBytes;
    }
}
