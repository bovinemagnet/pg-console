package com.bovinemagnet.pgconsole.model;

import java.time.Instant;

/**
 * Per-database metrics snapshot for historical tracking.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class DatabaseMetricsHistory {
    private Long id;
    private Instant sampledAt;

    // Database identification
    private String databaseName;

    // Connection metrics
    private int numBackends;

    // Transaction metrics
    private long xactCommit;
    private long xactRollback;

    // Cache metrics
    private long blksHit;
    private long blksRead;
    private Double cacheHitRatio;

    // Tuple metrics
    private Long tupReturned;
    private Long tupFetched;
    private Long tupInserted;
    private Long tupUpdated;
    private Long tupDeleted;

    // Problem indicators
    private Long deadlocks;
    private Long conflicts;
    private Long tempFiles;
    private Long tempBytes;

    // Size
    private Long databaseSizeBytes;

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

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public int getNumBackends() {
        return numBackends;
    }

    public void setNumBackends(int numBackends) {
        this.numBackends = numBackends;
    }

    public long getXactCommit() {
        return xactCommit;
    }

    public void setXactCommit(long xactCommit) {
        this.xactCommit = xactCommit;
    }

    public long getXactRollback() {
        return xactRollback;
    }

    public void setXactRollback(long xactRollback) {
        this.xactRollback = xactRollback;
    }

    public long getBlksHit() {
        return blksHit;
    }

    public void setBlksHit(long blksHit) {
        this.blksHit = blksHit;
    }

    public long getBlksRead() {
        return blksRead;
    }

    public void setBlksRead(long blksRead) {
        this.blksRead = blksRead;
    }

    public Double getCacheHitRatio() {
        return cacheHitRatio;
    }

    public void setCacheHitRatio(Double cacheHitRatio) {
        this.cacheHitRatio = cacheHitRatio;
    }

    public Long getTupReturned() {
        return tupReturned;
    }

    public void setTupReturned(Long tupReturned) {
        this.tupReturned = tupReturned;
    }

    public Long getTupFetched() {
        return tupFetched;
    }

    public void setTupFetched(Long tupFetched) {
        this.tupFetched = tupFetched;
    }

    public Long getTupInserted() {
        return tupInserted;
    }

    public void setTupInserted(Long tupInserted) {
        this.tupInserted = tupInserted;
    }

    public Long getTupUpdated() {
        return tupUpdated;
    }

    public void setTupUpdated(Long tupUpdated) {
        this.tupUpdated = tupUpdated;
    }

    public Long getTupDeleted() {
        return tupDeleted;
    }

    public void setTupDeleted(Long tupDeleted) {
        this.tupDeleted = tupDeleted;
    }

    public Long getDeadlocks() {
        return deadlocks;
    }

    public void setDeadlocks(Long deadlocks) {
        this.deadlocks = deadlocks;
    }

    public Long getConflicts() {
        return conflicts;
    }

    public void setConflicts(Long conflicts) {
        this.conflicts = conflicts;
    }

    public Long getTempFiles() {
        return tempFiles;
    }

    public void setTempFiles(Long tempFiles) {
        this.tempFiles = tempFiles;
    }

    public Long getTempBytes() {
        return tempBytes;
    }

    public void setTempBytes(Long tempBytes) {
        this.tempBytes = tempBytes;
    }

    public Long getDatabaseSizeBytes() {
        return databaseSizeBytes;
    }

    public void setDatabaseSizeBytes(Long databaseSizeBytes) {
        this.databaseSizeBytes = databaseSizeBytes;
    }
}
