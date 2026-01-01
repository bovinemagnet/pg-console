package com.bovinemagnet.pgconsole.model;

import java.time.LocalDateTime;

/**
 * Represents a PostgreSQL backend process activity from pg_stat_activity.
 * <p>
 * This model captures the current state of a database connection, including
 * details about the running query, connection metadata, wait events, and
 * blocking relationships. It is primarily used for monitoring active sessions
 * and identifying performance issues.
 * </p>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see LockInfo
 * @see BlockingTree
 */
public class Activity {
    private int pid;
    private String user;
    private String database;
    private String applicationName;
    private String clientAddr;
    private LocalDateTime backendStart;
    private LocalDateTime xactStart;
    private LocalDateTime queryStart;
    private LocalDateTime stateChange;
    private String state;
    private String waitEventType;
    private String waitEvent;
    private String query;
    private Integer blockingPid;
    
    public Activity() {}

    /**
     * Returns the process ID of the backend.
     *
     * @return the backend process ID
     */
    public int getPid() { return pid; }

    /**
     * Sets the process ID of the backend.
     *
     * @param pid the backend process ID
     */
    public void setPid(int pid) { this.pid = pid; }

    /**
     * Returns the PostgreSQL user connected to this backend.
     *
     * @return the user name
     */
    public String getUser() { return user; }

    /**
     * Sets the PostgreSQL user connected to this backend.
     *
     * @param user the user name
     */
    public void setUser(String user) { this.user = user; }

    /**
     * Returns the database name this backend is connected to.
     *
     * @return the database name
     */
    public String getDatabase() { return database; }

    /**
     * Sets the database name this backend is connected to.
     *
     * @param database the database name
     */
    public void setDatabase(String database) { this.database = database; }

    /**
     * Returns the application name reported by the client.
     *
     * @return the application name, or null if not set
     */
    public String getApplicationName() { return applicationName; }

    /**
     * Sets the application name reported by the client.
     *
     * @param applicationName the application name
     */
    public void setApplicationName(String applicationName) { this.applicationName = applicationName; }

    /**
     * Returns the client IP address.
     *
     * @return the client IP address, or null if local connection
     */
    public String getClientAddr() { return clientAddr; }

    /**
     * Sets the client IP address.
     *
     * @param clientAddr the client IP address
     */
    public void setClientAddr(String clientAddr) { this.clientAddr = clientAddr; }

    /**
     * Returns the timestamp when the backend process was started.
     *
     * @return the backend start timestamp
     */
    public LocalDateTime getBackendStart() { return backendStart; }

    /**
     * Sets the timestamp when the backend process was started.
     *
     * @param backendStart the backend start timestamp
     */
    public void setBackendStart(LocalDateTime backendStart) { this.backendStart = backendStart; }

    /**
     * Returns the timestamp when the current transaction was started.
     *
     * @return the transaction start timestamp, or null if no active transaction
     */
    public LocalDateTime getXactStart() { return xactStart; }

    /**
     * Sets the timestamp when the current transaction was started.
     *
     * @param xactStart the transaction start timestamp
     */
    public void setXactStart(LocalDateTime xactStart) { this.xactStart = xactStart; }

    /**
     * Returns the timestamp when the current query was started.
     *
     * @return the query start timestamp, or null if no active query
     */
    public LocalDateTime getQueryStart() { return queryStart; }

    /**
     * Sets the timestamp when the current query was started.
     *
     * @param queryStart the query start timestamp
     */
    public void setQueryStart(LocalDateTime queryStart) { this.queryStart = queryStart; }

    /**
     * Returns the timestamp when the state was last changed.
     *
     * @return the state change timestamp
     */
    public LocalDateTime getStateChange() { return stateChange; }

    /**
     * Sets the timestamp when the state was last changed.
     *
     * @param stateChange the state change timestamp
     */
    public void setStateChange(LocalDateTime stateChange) { this.stateChange = stateChange; }

    /**
     * Returns the current state of the backend.
     * <p>
     * Common states include "active", "idle", "idle in transaction",
     * "idle in transaction (aborted)", etc.
     * </p>
     *
     * @return the backend state
     */
    public String getState() { return state; }

    /**
     * Sets the current state of the backend.
     *
     * @param state the backend state
     */
    public void setState(String state) { this.state = state; }

    /**
     * Returns the type of wait event if the backend is waiting.
     *
     * @return the wait event type, or null if not waiting
     */
    public String getWaitEventType() { return waitEventType; }

    /**
     * Sets the type of wait event if the backend is waiting.
     *
     * @param waitEventType the wait event type
     */
    public void setWaitEventType(String waitEventType) { this.waitEventType = waitEventType; }

    /**
     * Returns the specific wait event if the backend is waiting.
     *
     * @return the wait event name, or null if not waiting
     */
    public String getWaitEvent() { return waitEvent; }

    /**
     * Sets the specific wait event if the backend is waiting.
     *
     * @param waitEvent the wait event name
     */
    public void setWaitEvent(String waitEvent) { this.waitEvent = waitEvent; }

    /**
     * Returns the text of the current or most recent query.
     *
     * @return the query text, or null if no query
     */
    public String getQuery() { return query; }

    /**
     * Sets the text of the current or most recent query.
     *
     * @param query the query text
     */
    public void setQuery(String query) { this.query = query; }

    /**
     * Returns the PID of the backend blocking this process.
     *
     * @return the blocking process ID, or null if not blocked
     */
    public Integer getBlockingPid() { return blockingPid; }

    /**
     * Sets the PID of the backend blocking this process.
     *
     * @param blockingPid the blocking process ID
     */
    public void setBlockingPid(Integer blockingPid) { this.blockingPid = blockingPid; }

    /**
     * Returns a truncated version of the query text suitable for display.
     * <p>
     * The query is truncated to 80 characters with an ellipsis if longer.
     * </p>
     *
     * @return the shortened query text, or empty string if query is null
     */
    public String getShortQuery() {
        if (query == null) return "";
        return query.length() > 80 ? query.substring(0, 77) + "..." : query;
    }
}
