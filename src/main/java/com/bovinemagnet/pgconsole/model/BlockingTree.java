package com.bovinemagnet.pgconsole.model;

/**
 * Blocking relationship information showing blocker and blocked sessions.
 *
 * @author Paul Snow
 * @version 0.0.0
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

    public int getBlockedPid() {
        return blockedPid;
    }

    public void setBlockedPid(int blockedPid) {
        this.blockedPid = blockedPid;
    }

    public String getBlockedUser() {
        return blockedUser;
    }

    public void setBlockedUser(String blockedUser) {
        this.blockedUser = blockedUser;
    }

    public String getBlockedQuery() {
        return blockedQuery;
    }

    public void setBlockedQuery(String blockedQuery) {
        this.blockedQuery = blockedQuery;
    }

    public String getShortBlockedQuery() {
        if (blockedQuery == null) return "";
        return blockedQuery.length() > 60 ? blockedQuery.substring(0, 60) + "..." : blockedQuery;
    }

    public String getBlockedState() {
        return blockedState;
    }

    public void setBlockedState(String blockedState) {
        this.blockedState = blockedState;
    }

    public String getBlockedDuration() {
        return blockedDuration;
    }

    public void setBlockedDuration(String blockedDuration) {
        this.blockedDuration = blockedDuration;
    }

    public int getBlockerPid() {
        return blockerPid;
    }

    public void setBlockerPid(int blockerPid) {
        this.blockerPid = blockerPid;
    }

    public String getBlockerUser() {
        return blockerUser;
    }

    public void setBlockerUser(String blockerUser) {
        this.blockerUser = blockerUser;
    }

    public String getBlockerQuery() {
        return blockerQuery;
    }

    public void setBlockerQuery(String blockerQuery) {
        this.blockerQuery = blockerQuery;
    }

    public String getShortBlockerQuery() {
        if (blockerQuery == null) return "";
        return blockerQuery.length() > 60 ? blockerQuery.substring(0, 60) + "..." : blockerQuery;
    }

    public String getBlockerState() {
        return blockerState;
    }

    public void setBlockerState(String blockerState) {
        this.blockerState = blockerState;
    }

    public String getLockMode() {
        return lockMode;
    }

    public void setLockMode(String lockMode) {
        this.lockMode = lockMode;
    }

    public boolean isBlockerIdleInTransaction() {
        return "idle in transaction".equals(blockerState);
    }
}
