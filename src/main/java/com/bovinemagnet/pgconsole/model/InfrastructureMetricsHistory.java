package com.bovinemagnet.pgconsole.model;

import java.time.Instant;

/**
 * Infrastructure-level metrics snapshot for historical tracking of WAL,
 * checkpoint, and buffer statistics.
 * <p>
 * This class represents a point-in-time snapshot of server-wide infrastructure
 * statistics captured from PostgreSQL's {@code pg_stat_wal},
 * {@code pg_stat_bgwriter}, and {@code pg_stat_checkpointer} system views.
 * <p>
 * WAL fields are nullable because {@code pg_stat_wal} only exists on
 * PostgreSQL 14+. Checkpoint fields are sourced from {@code pg_stat_bgwriter}
 * (PG12-16) or {@code pg_stat_checkpointer} (PG17+).
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see com.bovinemagnet.pgconsole.service.MetricsSamplerService
 * @see com.bovinemagnet.pgconsole.repository.HistoryRepository
 */
public class InfrastructureMetricsHistory {

    private Long id;
    private Instant sampledAt;

    // WAL stats (PG14+, nullable for older versions)
    private Long walRecords;
    private Long walFpi;
    private Long walBytes;
    private Long walBuffersFull;
    private Long walWrite;
    private Long walSync;
    private Double walWriteTime;
    private Double walSyncTime;

    // Checkpoint stats
    private Long checkpointsTimed;
    private Long checkpointsReq;
    private Double checkpointWriteTime;
    private Double checkpointSyncTime;

    // Buffer stats
    private Long buffersCheckpoint;
    private Long buffersClean;
    private Long buffersAlloc;
    private Long buffersBackend;

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

    public Long getWalRecords() {
        return walRecords;
    }

    public void setWalRecords(Long walRecords) {
        this.walRecords = walRecords;
    }

    public Long getWalFpi() {
        return walFpi;
    }

    public void setWalFpi(Long walFpi) {
        this.walFpi = walFpi;
    }

    public Long getWalBytes() {
        return walBytes;
    }

    public void setWalBytes(Long walBytes) {
        this.walBytes = walBytes;
    }

    public Long getWalBuffersFull() {
        return walBuffersFull;
    }

    public void setWalBuffersFull(Long walBuffersFull) {
        this.walBuffersFull = walBuffersFull;
    }

    public Long getWalWrite() {
        return walWrite;
    }

    public void setWalWrite(Long walWrite) {
        this.walWrite = walWrite;
    }

    public Long getWalSync() {
        return walSync;
    }

    public void setWalSync(Long walSync) {
        this.walSync = walSync;
    }

    public Double getWalWriteTime() {
        return walWriteTime;
    }

    public void setWalWriteTime(Double walWriteTime) {
        this.walWriteTime = walWriteTime;
    }

    public Double getWalSyncTime() {
        return walSyncTime;
    }

    public void setWalSyncTime(Double walSyncTime) {
        this.walSyncTime = walSyncTime;
    }

    public Long getCheckpointsTimed() {
        return checkpointsTimed;
    }

    public void setCheckpointsTimed(Long checkpointsTimed) {
        this.checkpointsTimed = checkpointsTimed;
    }

    public Long getCheckpointsReq() {
        return checkpointsReq;
    }

    public void setCheckpointsReq(Long checkpointsReq) {
        this.checkpointsReq = checkpointsReq;
    }

    public Double getCheckpointWriteTime() {
        return checkpointWriteTime;
    }

    public void setCheckpointWriteTime(Double checkpointWriteTime) {
        this.checkpointWriteTime = checkpointWriteTime;
    }

    public Double getCheckpointSyncTime() {
        return checkpointSyncTime;
    }

    public void setCheckpointSyncTime(Double checkpointSyncTime) {
        this.checkpointSyncTime = checkpointSyncTime;
    }

    public Long getBuffersCheckpoint() {
        return buffersCheckpoint;
    }

    public void setBuffersCheckpoint(Long buffersCheckpoint) {
        this.buffersCheckpoint = buffersCheckpoint;
    }

    public Long getBuffersClean() {
        return buffersClean;
    }

    public void setBuffersClean(Long buffersClean) {
        this.buffersClean = buffersClean;
    }

    public Long getBuffersAlloc() {
        return buffersAlloc;
    }

    public void setBuffersAlloc(Long buffersAlloc) {
        this.buffersAlloc = buffersAlloc;
    }

    public Long getBuffersBackend() {
        return buffersBackend;
    }

    public void setBuffersBackend(Long buffersBackend) {
        this.buffersBackend = buffersBackend;
    }
}
