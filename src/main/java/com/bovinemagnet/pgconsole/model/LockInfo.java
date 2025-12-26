package com.bovinemagnet.pgconsole.model;

/**
 * Lock information for the Locks page.
 *
 * @author Paul Snow
 * @version 0.0.0
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

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public String getLockType() {
        return lockType;
    }

    public void setLockType(String lockType) {
        this.lockType = lockType;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public boolean isGranted() {
        return granted;
    }

    public void setGranted(boolean granted) {
        this.granted = granted;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getShortQuery() {
        if (query == null) return "";
        return query.length() > 80 ? query.substring(0, 80) + "..." : query;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getWaitEventType() {
        return waitEventType;
    }

    public void setWaitEventType(String waitEventType) {
        this.waitEventType = waitEventType;
    }

    public String getWaitEvent() {
        return waitEvent;
    }

    public void setWaitEvent(String waitEvent) {
        this.waitEvent = waitEvent;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public boolean isIdleInTransaction() {
        return "idle in transaction".equals(state);
    }
}
