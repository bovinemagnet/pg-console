package com.bovinemagnet.pgconsole.model;

import java.time.LocalDateTime;

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
    
    public int getPid() { return pid; }
    public void setPid(int pid) { this.pid = pid; }
    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }
    public String getDatabase() { return database; }
    public void setDatabase(String database) { this.database = database; }
    public String getApplicationName() { return applicationName; }
    public void setApplicationName(String applicationName) { this.applicationName = applicationName; }
    public String getClientAddr() { return clientAddr; }
    public void setClientAddr(String clientAddr) { this.clientAddr = clientAddr; }
    public LocalDateTime getBackendStart() { return backendStart; }
    public void setBackendStart(LocalDateTime backendStart) { this.backendStart = backendStart; }
    public LocalDateTime getXactStart() { return xactStart; }
    public void setXactStart(LocalDateTime xactStart) { this.xactStart = xactStart; }
    public LocalDateTime getQueryStart() { return queryStart; }
    public void setQueryStart(LocalDateTime queryStart) { this.queryStart = queryStart; }
    public LocalDateTime getStateChange() { return stateChange; }
    public void setStateChange(LocalDateTime stateChange) { this.stateChange = stateChange; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getWaitEventType() { return waitEventType; }
    public void setWaitEventType(String waitEventType) { this.waitEventType = waitEventType; }
    public String getWaitEvent() { return waitEvent; }
    public void setWaitEvent(String waitEvent) { this.waitEvent = waitEvent; }
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public Integer getBlockingPid() { return blockingPid; }
    public void setBlockingPid(Integer blockingPid) { this.blockingPid = blockingPid; }
    
    public String getShortQuery() {
        if (query == null) return "";
        return query.length() > 80 ? query.substring(0, 77) + "..." : query;
    }
}
