package com.bovinemagnet.pgconsole.model;

import java.time.Instant;

/**
 * Represents the WAL receiver status for a PostgreSQL standby server.
 * <p>
 * This model captures data from the {@code pg_stat_wal_receiver} system view,
 * which provides information about the WAL receiver process on a standby server.
 * This view only contains data when queried on a standby server that is receiving
 * streaming replication from a primary server.
 * <p>
 * Key information tracked:
 * <ul>
 *   <li>Connection status and sender details</li>
 *   <li>LSN positions for received, written, flushed WAL</li>
 *   <li>Message timing for monitoring replication health</li>
 *   <li>Replication slot information if configured</li>
 * </ul>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see <a href="https://www.postgresql.org/docs/current/monitoring-stats.html#PG-STAT-WAL-RECEIVER-VIEW">pg_stat_wal_receiver Documentation</a>
 */
public class WalReceiverStatus {

    /**
     * Represents the possible states of the WAL receiver.
     */
    public enum ReceiverStatus {
        /** WAL receiver is starting up */
        STARTING,
        /** WAL receiver is catching up with the sender */
        CATCHUP,
        /** WAL receiver is streaming in real-time */
        STREAMING,
        /** WAL receiver is waiting for more data */
        WAITING,
        /** WAL receiver is restarting after an error */
        RESTARTING,
        /** WAL receiver is stopping */
        STOPPING,
        /** WAL receiver state is unknown */
        UNKNOWN
    }

    /** Process ID of the WAL receiver process */
    private int pid;

    /** Current status of the WAL receiver */
    private ReceiverStatus status;

    /** First WAL location used at receiver start */
    private String receiveStartLsn;

    /** Timeline number of first WAL location received */
    private int receiveStartTli;

    /** Last WAL location written to disk */
    private String writtenLsn;

    /** Last WAL location flushed to disk */
    private String flushedLsn;

    /** Timeline number of last location received */
    private int receivedTli;

    /** Time of last message received from sender */
    private Instant lastMsgReceiptTime;

    /** Time of last message sent by sender */
    private Instant lastMsgSendTime;

    /** End LSN reported to sender */
    private String latestEndLsn;

    /** Time of latest end LSN report */
    private Instant latestEndTime;

    /** Replication slot name used by this receiver */
    private String slotName;

    /** Hostname of the sender server */
    private String senderHost;

    /** Port number of the sender server */
    private int senderPort;

    /** Connection info (with password masked) */
    private String conninfo;

    /** Whether this is an active standby server */
    private boolean isStandby;

    /**
     * Constructs a new WalReceiverStatus instance with default values.
     */
    public WalReceiverStatus() {
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public ReceiverStatus getStatus() {
        return status;
    }

    public void setStatus(ReceiverStatus status) {
        this.status = status;
    }

    /**
     * Parses and sets the receiver status from a string value.
     *
     * @param statusStr the string representation of the status
     */
    public void setStatusFromString(String statusStr) {
        if (statusStr == null) {
            this.status = ReceiverStatus.UNKNOWN;
            return;
        }
        try {
            this.status = ReceiverStatus.valueOf(statusStr.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            this.status = ReceiverStatus.UNKNOWN;
        }
    }

    public String getReceiveStartLsn() {
        return receiveStartLsn;
    }

    public void setReceiveStartLsn(String receiveStartLsn) {
        this.receiveStartLsn = receiveStartLsn;
    }

    public int getReceiveStartTli() {
        return receiveStartTli;
    }

    public void setReceiveStartTli(int receiveStartTli) {
        this.receiveStartTli = receiveStartTli;
    }

    public String getWrittenLsn() {
        return writtenLsn;
    }

    public void setWrittenLsn(String writtenLsn) {
        this.writtenLsn = writtenLsn;
    }

    public String getFlushedLsn() {
        return flushedLsn;
    }

    public void setFlushedLsn(String flushedLsn) {
        this.flushedLsn = flushedLsn;
    }

    public int getReceivedTli() {
        return receivedTli;
    }

    public void setReceivedTli(int receivedTli) {
        this.receivedTli = receivedTli;
    }

    public Instant getLastMsgReceiptTime() {
        return lastMsgReceiptTime;
    }

    public void setLastMsgReceiptTime(Instant lastMsgReceiptTime) {
        this.lastMsgReceiptTime = lastMsgReceiptTime;
    }

    public Instant getLastMsgSendTime() {
        return lastMsgSendTime;
    }

    public void setLastMsgSendTime(Instant lastMsgSendTime) {
        this.lastMsgSendTime = lastMsgSendTime;
    }

    public String getLatestEndLsn() {
        return latestEndLsn;
    }

    public void setLatestEndLsn(String latestEndLsn) {
        this.latestEndLsn = latestEndLsn;
    }

    public Instant getLatestEndTime() {
        return latestEndTime;
    }

    public void setLatestEndTime(Instant latestEndTime) {
        this.latestEndTime = latestEndTime;
    }

    public String getSlotName() {
        return slotName;
    }

    public void setSlotName(String slotName) {
        this.slotName = slotName;
    }

    public String getSenderHost() {
        return senderHost;
    }

    public void setSenderHost(String senderHost) {
        this.senderHost = senderHost;
    }

    public int getSenderPort() {
        return senderPort;
    }

    public void setSenderPort(int senderPort) {
        this.senderPort = senderPort;
    }

    public String getConninfo() {
        return conninfo;
    }

    public void setConninfo(String conninfo) {
        this.conninfo = conninfo;
    }

    public boolean isStandby() {
        return isStandby;
    }

    public void setStandby(boolean standby) {
        isStandby = standby;
    }

    /**
     * Returns a Bootstrap CSS class appropriate for the current status.
     *
     * @return Bootstrap background class for the status badge
     */
    public String getStatusCssClass() {
        if (status == null) return "bg-secondary";
        return switch (status) {
            case STREAMING -> "bg-success";
            case CATCHUP -> "bg-warning text-dark";
            case STARTING, WAITING -> "bg-info";
            case RESTARTING -> "bg-warning text-dark";
            case STOPPING -> "bg-danger";
            case UNKNOWN -> "bg-secondary";
        };
    }

    /**
     * Returns the sender address as a display string.
     *
     * @return formatted sender address (host:port)
     */
    public String getSenderDisplay() {
        if (senderHost == null || senderHost.isEmpty()) {
            return "Unknown";
        }
        if (senderPort > 0) {
            return senderHost + ":" + senderPort;
        }
        return senderHost;
    }

    /**
     * Returns the time since last message was received as a human-readable string.
     *
     * @return formatted time since last message
     */
    public String getTimeSinceLastMessage() {
        if (lastMsgReceiptTime == null) {
            return "N/A";
        }
        long seconds = java.time.Duration.between(lastMsgReceiptTime, Instant.now()).getSeconds();
        if (seconds < 60) return seconds + " seconds ago";
        if (seconds < 3600) return (seconds / 60) + " minutes ago";
        return (seconds / 3600) + " hours ago";
    }
}
