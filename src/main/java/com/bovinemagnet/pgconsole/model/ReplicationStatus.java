package com.bovinemagnet.pgconsole.model;

import java.time.Instant;

/**
 * Represents the streaming replication status of a PostgreSQL standby server.
 * <p>
 * This model captures data from the {@code pg_stat_replication} system view, which provides
 * information about active replication connections from this database cluster to standby servers.
 * It includes connection details, replication state, WAL (Write-Ahead Log) positions, and lag metrics.
 * <p>
 * The class includes computed fields to simplify lag analysis and provides formatting methods
 * for display purposes in dashboard templates.
 * <p>
 * Key metrics tracked:
 * <ul>
 *   <li>LSN (Log Sequence Number) positions for sent, written, flushed, and replayed WAL</li>
 *   <li>Lag measurements in both time (milliseconds) and bytes</li>
 *   <li>Synchronous vs asynchronous replication state</li>
 *   <li>Client connection information and replication state</li>
 * </ul>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see <a href="https://www.postgresql.org/docs/current/monitoring-stats.html#PG-STAT-REPLICATION-VIEW">pg_stat_replication Documentation</a>
 */
public class ReplicationStatus {

    /**
     * Represents the current state of a replication connection.
     * <p>
     * These states correspond to the {@code state} column in {@code pg_stat_replication}
     * and indicate the progress of the replication process.
     */
    public enum ReplicationState {
        /** Initial connection state, standby is starting up */
        STARTUP,

        /** Standby is catching up with the primary by streaming WAL */
        CATCHUP,

        /** Normal operating state, standby is streaming WAL in real-time */
        STREAMING,

        /** Connection is being used for a base backup operation */
        BACKUP,

        /** Replication is being stopped */
        STOPPING,

        /** State could not be determined or is unrecognised */
        UNKNOWN
    }

    /** Process ID of the WAL sender process on the primary server */
    private int pid;

    /** Name of the user used for the replication connection */
    private String useName;

    /** Name of the application connected to this WAL sender (set by application_name parameter) */
    private String applicationName;

    /** IP address of the client connected to this WAL sender, may be null for Unix socket connections */
    private String clientAddr;

    /** Hostname of the connected client as reported by reverse DNS lookup, may be null */
    private String clientHostname;

    /** TCP port number that the client is using for communication, -1 for Unix sockets */
    private int clientPort;

    /** Time when the WAL sender process was started */
    private Instant backendStart;

    /** Earliest transaction ID that this standby needs, preventing vacuum from removing old rows */
    private Instant backendXmin;

    /** Current replication state of this connection */
    private ReplicationState state;

    /** Last Write-Ahead Log location sent to this standby server */
    private String sentLsn;

    /** Last WAL location written to disk by this standby server */
    private String writeLsn;

    /** Last WAL location flushed to disk by this standby server */
    private String flushLsn;

    /** Last WAL location replayed (applied) by this standby server */
    private String replayLsn;

    /** Time delay in milliseconds between writing WAL on primary and writing it on standby */
    private long writeLag;

    /** Time delay in milliseconds between writing WAL on primary and flushing it on standby */
    private long flushLag;

    /** Time delay in milliseconds between writing WAL on primary and replaying it on standby */
    private long replayLag;

    /** Priority of this standby server for being chosen as synchronous standby (0 for async) */
    private String syncPriority;

    /** Synchronous state: 'sync' for synchronous, 'async' for asynchronous, 'potential' for potential sync */
    private String syncState;

    /** Last time a status packet was received from the standby */
    private Instant replyTime;

    /**
     * Computed field: number of bytes the standby is behind the primary.
     * Calculated from the difference between sent and replayed LSN positions.
     */
    private long lagBytes;

    /**
     * Computed field: whether this is a synchronous standby.
     * Set to true when syncState is 'sync'.
     */
    private boolean isSync;

    /**
     * Computed field: whether this standby has measurable lag.
     * Set to true when lagBytes is greater than zero.
     */
    private boolean hasLag;

    /**
     * Constructs a new ReplicationStatus instance with default values.
     * All fields are initialised to their default values and should be populated
     * using the setter methods.
     */
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

    /**
     * Sets the replication state.
     *
     * @param state the replication state to set
     */
    public void setState(ReplicationState state) {
        this.state = state;
    }

    /**
     * Parses and sets the replication state from a string value.
     * <p>
     * This method converts the string representation of the state (as returned by PostgreSQL)
     * into the corresponding {@link ReplicationState} enum value. The conversion is case-insensitive.
     * If the string is null or cannot be parsed, the state is set to {@link ReplicationState#UNKNOWN}.
     *
     * @param stateStr the string representation of the replication state (e.g., "streaming", "catchup")
     */
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

    /**
     * Sets the synchronous replication state and updates the computed {@code isSync} field.
     * <p>
     * This method automatically sets the {@code isSync} flag to true if the state is "sync"
     * (case-insensitive), false otherwise. Common values include "sync", "async", and "potential".
     *
     * @param syncState the synchronous state ('sync', 'async', or 'potential')
     */
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

    /**
     * Sets the replication lag in bytes and updates the computed {@code hasLag} field.
     * <p>
     * This method automatically sets the {@code hasLag} flag to true if the lag is greater
     * than zero, false otherwise. The lag value represents how many bytes behind the primary
     * server this standby is, calculated from LSN positions.
     *
     * @param lagBytes the replication lag in bytes
     */
    public void setLagBytes(long lagBytes) {
        this.lagBytes = lagBytes;
        this.hasLag = lagBytes > 0;
    }

    /**
     * Checks whether this standby is configured as a synchronous replica.
     * <p>
     * This is a computed field that is set to true when the {@code syncState} is "sync".
     * Synchronous replicas provide stronger data durability guarantees as the primary
     * waits for acknowledgement before committing transactions.
     *
     * @return true if this is a synchronous standby, false otherwise
     */
    public boolean isSync() {
        return isSync;
    }

    /**
     * Checks whether this standby has measurable replication lag.
     * <p>
     * This is a computed field that is set to true when {@code lagBytes} is greater than zero.
     *
     * @return true if the standby has lag, false if it is fully caught up
     */
    public boolean isHasLag() {
        return hasLag;
    }

    /**
     * Returns a Bootstrap CSS class appropriate for the current replication state.
     * <p>
     * This method is used by Qute templates to apply colour-coded badges indicating
     * the health and status of the replication connection.
     *
     * @return a Bootstrap background class (e.g., "bg-success", "bg-warning", "bg-danger")
     */
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

    /**
     * Returns a Bootstrap CSS class appropriate for the synchronous replication state.
     * <p>
     * Synchronous replicas receive "bg-success" (green), whilst asynchronous replicas
     * receive "bg-info" (blue) to visually distinguish the replication mode.
     *
     * @return "bg-success" if synchronous, "bg-info" otherwise
     */
    public String getSyncStateCssClass() {
        if (isSync) {
            return "bg-success";
        }
        return "bg-info";
    }

    /**
     * Returns the replay lag formatted as a human-readable string.
     * <p>
     * The lag is formatted with appropriate units (ms, s, min, hours) depending
     * on the magnitude of the delay.
     *
     * @return formatted replay lag (e.g., "150 ms", "2.5 s", "3.2 min")
     * @see #formatLag(long)
     */
    public String getReplayLagFormatted() {
        return formatLag(replayLag);
    }

    /**
     * Returns the write lag formatted as a human-readable string.
     * <p>
     * The lag is formatted with appropriate units (ms, s, min, hours) depending
     * on the magnitude of the delay.
     *
     * @return formatted write lag (e.g., "50 ms", "1.2 s")
     * @see #formatLag(long)
     */
    public String getWriteLagFormatted() {
        return formatLag(writeLag);
    }

    /**
     * Returns the flush lag formatted as a human-readable string.
     * <p>
     * The lag is formatted with appropriate units (ms, s, min, hours) depending
     * on the magnitude of the delay.
     *
     * @return formatted flush lag (e.g., "100 ms", "1.8 s")
     * @see #formatLag(long)
     */
    public String getFlushLagFormatted() {
        return formatLag(flushLag);
    }

    /**
     * Returns the lag in bytes formatted as a human-readable string.
     * <p>
     * The byte count is formatted with appropriate units (B, KB, MB, GB) depending
     * on the magnitude.
     *
     * @return formatted byte lag (e.g., "1.5 MB", "230.0 KB")
     * @see #formatBytes(long)
     */
    public String getLagBytesFormatted() {
        return formatBytes(lagBytes);
    }

    /**
     * Formats a lag value in milliseconds into a human-readable string with appropriate units.
     * <p>
     * The formatting automatically selects the most appropriate unit based on magnitude:
     * <ul>
     *   <li>Less than 1 second: milliseconds (e.g., "150 ms")</li>
     *   <li>1 second to 1 minute: seconds with one decimal place (e.g., "2.5 s")</li>
     *   <li>1 minute to 1 hour: minutes with one decimal place (e.g., "3.2 min")</li>
     *   <li>1 hour or more: hours with one decimal place (e.g., "1.5 hours")</li>
     * </ul>
     *
     * @param lagMs the lag time in milliseconds
     * @return formatted lag string, or "N/A" if the value is negative
     */
    private String formatLag(long lagMs) {
        if (lagMs < 0) return "N/A";
        if (lagMs == 0) return "0 ms";
        if (lagMs < 1000) return lagMs + " ms";
        if (lagMs < 60000) return String.format("%.1f s", lagMs / 1000.0);
        if (lagMs < 3600000) return String.format("%.1f min", lagMs / 60000.0);
        return String.format("%.1f hours", lagMs / 3600000.0);
    }

    /**
     * Formats a byte count into a human-readable string with appropriate units.
     * <p>
     * The formatting automatically selects the most appropriate unit based on magnitude:
     * <ul>
     *   <li>Less than 1 KB: bytes (e.g., "512 B")</li>
     *   <li>1 KB to 1 MB: kilobytes with one decimal place (e.g., "230.5 KB")</li>
     *   <li>1 MB to 1 GB: megabytes with one decimal place (e.g., "45.2 MB")</li>
     *   <li>1 GB or more: gigabytes with one decimal place (e.g., "2.8 GB")</li>
     * </ul>
     *
     * @param bytes the byte count
     * @return formatted byte string, or "N/A" if the value is negative
     */
    private String formatBytes(long bytes) {
        if (bytes < 0) return "N/A";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * Returns a display-friendly representation of the client connection.
     * <p>
     * This method prioritises the hostname (from reverse DNS lookup) if available,
     * falling back to the IP address, and finally to "Unknown" if neither is set.
     * This is useful for dashboard displays where a recognisable name is preferred
     * over a numeric IP address.
     *
     * @return the hostname if available, otherwise the IP address, or "Unknown" if neither is set
     */
    public String getClientDisplay() {
        if (clientHostname != null && !clientHostname.isEmpty()) {
            return clientHostname;
        }
        return clientAddr != null ? clientAddr : "Unknown";
    }
}
