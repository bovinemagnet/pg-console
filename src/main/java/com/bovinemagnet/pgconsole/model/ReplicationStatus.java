package com.bovinemagnet.pgconsole.model;

import java.time.Instant;

/**
 * Represents streaming replication status from pg_stat_replication.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class ReplicationStatus {

    public enum ReplicationState {
        STARTUP,
        CATCHUP,
        STREAMING,
        BACKUP,
        STOPPING,
        UNKNOWN
    }

    private int pid;
    private String useName;
    private String applicationName;
    private String clientAddr;
    private String clientHostname;
    private int clientPort;
    private Instant backendStart;
    private Instant backendXmin;
    private ReplicationState state;
    private String sentLsn;
    private String writeLsn;
    private String flushLsn;
    private String replayLsn;
    private long writeLag;      // in milliseconds
    private long flushLag;      // in milliseconds
    private long replayLag;     // in milliseconds
    private String syncPriority;
    private String syncState;
    private Instant replyTime;

    // Computed fields
    private long lagBytes;      // bytes behind primary
    private boolean isSync;
    private boolean hasLag;

    public ReplicationStatus() {
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public String getUseName() {
        return useName;
    }

    public void setUseName(String useName) {
        this.useName = useName;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getClientAddr() {
        return clientAddr;
    }

    public void setClientAddr(String clientAddr) {
        this.clientAddr = clientAddr;
    }

    public String getClientHostname() {
        return clientHostname;
    }

    public void setClientHostname(String clientHostname) {
        this.clientHostname = clientHostname;
    }

    public int getClientPort() {
        return clientPort;
    }

    public void setClientPort(int clientPort) {
        this.clientPort = clientPort;
    }

    public Instant getBackendStart() {
        return backendStart;
    }

    public void setBackendStart(Instant backendStart) {
        this.backendStart = backendStart;
    }

    public Instant getBackendXmin() {
        return backendXmin;
    }

    public void setBackendXmin(Instant backendXmin) {
        this.backendXmin = backendXmin;
    }

    public ReplicationState getState() {
        return state;
    }

    public void setState(ReplicationState state) {
        this.state = state;
    }

    public void setStateFromString(String stateStr) {
        if (stateStr == null) {
            this.state = ReplicationState.UNKNOWN;
            return;
        }
        try {
            this.state = ReplicationState.valueOf(stateStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            this.state = ReplicationState.UNKNOWN;
        }
    }

    public String getSentLsn() {
        return sentLsn;
    }

    public void setSentLsn(String sentLsn) {
        this.sentLsn = sentLsn;
    }

    public String getWriteLsn() {
        return writeLsn;
    }

    public void setWriteLsn(String writeLsn) {
        this.writeLsn = writeLsn;
    }

    public String getFlushLsn() {
        return flushLsn;
    }

    public void setFlushLsn(String flushLsn) {
        this.flushLsn = flushLsn;
    }

    public String getReplayLsn() {
        return replayLsn;
    }

    public void setReplayLsn(String replayLsn) {
        this.replayLsn = replayLsn;
    }

    public long getWriteLag() {
        return writeLag;
    }

    public void setWriteLag(long writeLag) {
        this.writeLag = writeLag;
    }

    public long getFlushLag() {
        return flushLag;
    }

    public void setFlushLag(long flushLag) {
        this.flushLag = flushLag;
    }

    public long getReplayLag() {
        return replayLag;
    }

    public void setReplayLag(long replayLag) {
        this.replayLag = replayLag;
    }

    public String getSyncPriority() {
        return syncPriority;
    }

    public void setSyncPriority(String syncPriority) {
        this.syncPriority = syncPriority;
    }

    public String getSyncState() {
        return syncState;
    }

    public void setSyncState(String syncState) {
        this.syncState = syncState;
        this.isSync = "sync".equalsIgnoreCase(syncState);
    }

    public Instant getReplyTime() {
        return replyTime;
    }

    public void setReplyTime(Instant replyTime) {
        this.replyTime = replyTime;
    }

    public long getLagBytes() {
        return lagBytes;
    }

    public void setLagBytes(long lagBytes) {
        this.lagBytes = lagBytes;
        this.hasLag = lagBytes > 0;
    }

    public boolean isSync() {
        return isSync;
    }

    public boolean isHasLag() {
        return hasLag;
    }

    public String getStateCssClass() {
        return switch (state) {
            case STREAMING -> "bg-success";
            case CATCHUP -> "bg-warning text-dark";
            case STARTUP -> "bg-info";
            case BACKUP -> "bg-secondary";
            case STOPPING -> "bg-danger";
            case UNKNOWN -> "bg-secondary";
        };
    }

    public String getSyncStateCssClass() {
        if (isSync) {
            return "bg-success";
        }
        return "bg-info";
    }

    public String getReplayLagFormatted() {
        return formatLag(replayLag);
    }

    public String getWriteLagFormatted() {
        return formatLag(writeLag);
    }

    public String getFlushLagFormatted() {
        return formatLag(flushLag);
    }

    public String getLagBytesFormatted() {
        return formatBytes(lagBytes);
    }

    private String formatLag(long lagMs) {
        if (lagMs < 0) return "N/A";
        if (lagMs == 0) return "0 ms";
        if (lagMs < 1000) return lagMs + " ms";
        if (lagMs < 60000) return String.format("%.1f s", lagMs / 1000.0);
        if (lagMs < 3600000) return String.format("%.1f min", lagMs / 60000.0);
        return String.format("%.1f hours", lagMs / 3600000.0);
    }

    private String formatBytes(long bytes) {
        if (bytes < 0) return "N/A";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    public String getClientDisplay() {
        if (clientHostname != null && !clientHostname.isEmpty()) {
            return clientHostname;
        }
        return clientAddr != null ? clientAddr : "Unknown";
    }
}
