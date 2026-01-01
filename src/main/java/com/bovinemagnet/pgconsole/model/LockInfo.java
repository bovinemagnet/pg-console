package com.bovinemagnet.pgconsole.model;

/**
 * Represents lock information from pg_locks joined with pg_stat_activity.
 * <p>
 * This model captures details about database locks, including the lock type,
 * mode, grant status, and associated query information. It is used to identify
 * lock contention and blocking scenarios.
 * </p>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see BlockingTree
 * @see Activity
 */
public class LockInfo {
    private int pid;
    private String lockType;
    private String database;
    private String relation;
    private String mode;
    private boolean granted;
    private String query;
    private String state;
    private String waitEventType;
    private String waitEvent;
    private String user;

    /**
     * Returns the process ID holding or waiting for the lock.
     *
     * @return the process ID
     */
    public int getPid() {
        return pid;
    }

    /**
     * Sets the process ID holding or waiting for the lock.
     *
     * @param pid the process ID
     */
    public void setPid(int pid) {
        this.pid = pid;
    }

    /**
     * Returns the type of lock (e.g., relation, transactionid, tuple).
     *
     * @return the lock type
     */
    public String getLockType() {
        return lockType;
    }

    /**
     * Sets the type of lock.
     *
     * @param lockType the lock type
     */
    public void setLockType(String lockType) {
        this.lockType = lockType;
    }

    /**
     * Returns the database name where the lock exists.
     *
     * @return the database name
     */
    public String getDatabase() {
        return database;
    }

    /**
     * Sets the database name where the lock exists.
     *
     * @param database the database name
     */
    public void setDatabase(String database) {
        this.database = database;
    }

    /**
     * Returns the name of the relation (table/index) being locked.
     *
     * @return the relation name, or null if not a relation lock
     */
    public String getRelation() {
        return relation;
    }

    /**
     * Sets the name of the relation being locked.
     *
     * @param relation the relation name
     */
    public void setRelation(String relation) {
        this.relation = relation;
    }

    /**
     * Returns the lock mode (e.g., AccessShareLock, RowExclusiveLock).
     *
     * @return the lock mode
     */
    public String getMode() {
        return mode;
    }

    /**
     * Sets the lock mode.
     *
     * @param mode the lock mode
     */
    public void setMode(String mode) {
        this.mode = mode;
    }

    /**
     * Checks whether the lock has been granted.
     *
     * @return true if the lock is granted, false if waiting
     */
    public boolean isGranted() {
        return granted;
    }

    /**
     * Sets whether the lock has been granted.
     *
     * @param granted true if granted, false if waiting
     */
    public void setGranted(boolean granted) {
        this.granted = granted;
    }

    /**
     * Returns the query associated with this lock.
     *
     * @return the query text
     */
    public String getQuery() {
        return query;
    }

    /**
     * Sets the query associated with this lock.
     *
     * @param query the query text
     */
    public void setQuery(String query) {
        this.query = query;
    }

    /**
     * Returns a truncated version of the query for display.
     * <p>
     * The query is truncated to 80 characters with an ellipsis if longer.
     * </p>
     *
     * @return the shortened query text, or empty string if query is null
     */
    public String getShortQuery() {
        if (query == null) return "";
        return query.length() > 80 ? query.substring(0, 80) + "..." : query;
    }

    /**
     * Returns the state of the backend holding this lock.
     *
     * @return the backend state
     */
    public String getState() {
        return state;
    }

    /**
     * Sets the state of the backend holding this lock.
     *
     * @param state the backend state
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * Returns the wait event type if the backend is waiting.
     *
     * @return the wait event type, or null if not waiting
     */
    public String getWaitEventType() {
        return waitEventType;
    }

    /**
     * Sets the wait event type.
     *
     * @param waitEventType the wait event type
     */
    public void setWaitEventType(String waitEventType) {
        this.waitEventType = waitEventType;
    }

    /**
     * Returns the specific wait event name.
     *
     * @return the wait event name, or null if not waiting
     */
    public String getWaitEvent() {
        return waitEvent;
    }

    /**
     * Sets the specific wait event name.
     *
     * @param waitEvent the wait event name
     */
    public void setWaitEvent(String waitEvent) {
        this.waitEvent = waitEvent;
    }

    /**
     * Returns the user holding or waiting for the lock.
     *
     * @return the user name
     */
    public String getUser() {
        return user;
    }

    /**
     * Sets the user holding or waiting for the lock.
     *
     * @param user the user name
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Checks if this backend is idle in a transaction.
     * <p>
     * Idle in transaction sessions can block other queries from
     * obtaining necessary locks, leading to performance issues.
     * </p>
     *
     * @return true if the state is "idle in transaction", false otherwise
     */
    public boolean isIdleInTransaction() {
        return "idle in transaction".equals(state);
    }
}
