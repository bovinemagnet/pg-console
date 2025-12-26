package com.bovinemagnet.pgconsole.model;

/**
 * Represents blocking relationship information showing blocker and blocked sessions.
 * <p>
 * This model encapsulates the details of lock blocking scenarios, identifying
 * which process is blocking another and the associated queries and states.
 * It is essential for diagnosing deadlocks and lock contention issues.
 * </p>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see LockInfo
 * @see Activity
 */
public class BlockingTree {
    private int blockedPid;
    private String blockedUser;
    private String blockedQuery;
    private String blockedState;
    private String blockedDuration;
    private int blockerPid;
    private String blockerUser;
    private String blockerQuery;
    private String blockerState;
    private String lockMode;

    /**
     * Returns the process ID of the blocked session.
     *
     * @return the blocked process ID
     */
    public int getBlockedPid() {
        return blockedPid;
    }

    /**
     * Sets the process ID of the blocked session.
     *
     * @param blockedPid the blocked process ID
     */
    public void setBlockedPid(int blockedPid) {
        this.blockedPid = blockedPid;
    }

    /**
     * Returns the user of the blocked session.
     *
     * @return the blocked user name
     */
    public String getBlockedUser() {
        return blockedUser;
    }

    /**
     * Sets the user of the blocked session.
     *
     * @param blockedUser the blocked user name
     */
    public void setBlockedUser(String blockedUser) {
        this.blockedUser = blockedUser;
    }

    /**
     * Returns the query text of the blocked session.
     *
     * @return the blocked query text
     */
    public String getBlockedQuery() {
        return blockedQuery;
    }

    /**
     * Sets the query text of the blocked session.
     *
     * @param blockedQuery the blocked query text
     */
    public void setBlockedQuery(String blockedQuery) {
        this.blockedQuery = blockedQuery;
    }

    /**
     * Returns a truncated version of the blocked query for display.
     *
     * @return the shortened blocked query, or empty string if null
     */
    public String getShortBlockedQuery() {
        if (blockedQuery == null) return "";
        return blockedQuery.length() > 60 ? blockedQuery.substring(0, 60) + "..." : blockedQuery;
    }

    /**
     * Returns the state of the blocked session.
     *
     * @return the blocked session state
     */
    public String getBlockedState() {
        return blockedState;
    }

    /**
     * Sets the state of the blocked session.
     *
     * @param blockedState the blocked session state
     */
    public void setBlockedState(String blockedState) {
        this.blockedState = blockedState;
    }

    /**
     * Returns the duration for which the session has been blocked.
     *
     * @return the blocked duration as a formatted string
     */
    public String getBlockedDuration() {
        return blockedDuration;
    }

    /**
     * Sets the duration for which the session has been blocked.
     *
     * @param blockedDuration the blocked duration
     */
    public void setBlockedDuration(String blockedDuration) {
        this.blockedDuration = blockedDuration;
    }

    /**
     * Returns the process ID of the blocking session.
     *
     * @return the blocker process ID
     */
    public int getBlockerPid() {
        return blockerPid;
    }

    /**
     * Sets the process ID of the blocking session.
     *
     * @param blockerPid the blocker process ID
     */
    public void setBlockerPid(int blockerPid) {
        this.blockerPid = blockerPid;
    }

    /**
     * Returns the user of the blocking session.
     *
     * @return the blocker user name
     */
    public String getBlockerUser() {
        return blockerUser;
    }

    /**
     * Sets the user of the blocking session.
     *
     * @param blockerUser the blocker user name
     */
    public void setBlockerUser(String blockerUser) {
        this.blockerUser = blockerUser;
    }

    /**
     * Returns the query text of the blocking session.
     *
     * @return the blocker query text
     */
    public String getBlockerQuery() {
        return blockerQuery;
    }

    /**
     * Sets the query text of the blocking session.
     *
     * @param blockerQuery the blocker query text
     */
    public void setBlockerQuery(String blockerQuery) {
        this.blockerQuery = blockerQuery;
    }

    /**
     * Returns a truncated version of the blocker query for display.
     *
     * @return the shortened blocker query, or empty string if null
     */
    public String getShortBlockerQuery() {
        if (blockerQuery == null) return "";
        return blockerQuery.length() > 60 ? blockerQuery.substring(0, 60) + "..." : blockerQuery;
    }

    /**
     * Returns the state of the blocking session.
     *
     * @return the blocker session state
     */
    public String getBlockerState() {
        return blockerState;
    }

    /**
     * Sets the state of the blocking session.
     *
     * @param blockerState the blocker session state
     */
    public void setBlockerState(String blockerState) {
        this.blockerState = blockerState;
    }

    /**
     * Returns the lock mode causing the block.
     *
     * @return the lock mode
     */
    public String getLockMode() {
        return lockMode;
    }

    /**
     * Sets the lock mode causing the block.
     *
     * @param lockMode the lock mode
     */
    public void setLockMode(String lockMode) {
        this.lockMode = lockMode;
    }

    /**
     * Checks if the blocker is idle in a transaction.
     * <p>
     * A blocker idle in transaction is particularly problematic as it
     * may hold locks indefinitely without actively working.
     * </p>
     *
     * @return true if the blocker state is "idle in transaction", false otherwise
     */
    public boolean isBlockerIdleInTransaction() {
        return "idle in transaction".equals(blockerState);
    }
}
