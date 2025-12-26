package com.bovinemagnet.pgconsole.model;

public class SlowQuery {
    private String queryId;
    private String query;
    private long totalCalls;
    private double totalTime;
    private double meanTime;
    private double minTime;
    private double maxTime;
    private long rows;
    private String user;
    private String database;
    
    public SlowQuery() {
    }

    public SlowQuery(String queryId, String query, long totalCalls, double totalTime, 
                     double meanTime, double minTime, double maxTime, long rows, 
                     String user, String database) {
        this.queryId = queryId;
        this.query = query;
        this.totalCalls = totalCalls;
        this.totalTime = totalTime;
        this.meanTime = meanTime;
        this.minTime = minTime;
        this.maxTime = maxTime;
        this.rows = rows;
        this.user = user;
        this.database = database;
    }

    public String getQueryId() { return queryId; }
    public void setQueryId(String queryId) { this.queryId = queryId; }
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public long getTotalCalls() { return totalCalls; }
    public void setTotalCalls(long totalCalls) { this.totalCalls = totalCalls; }
    public double getTotalTime() { return totalTime; }
    public void setTotalTime(double totalTime) { this.totalTime = totalTime; }
    public double getMeanTime() { return meanTime; }
    public void setMeanTime(double meanTime) { this.meanTime = meanTime; }
    public double getMinTime() { return minTime; }
    public void setMinTime(double minTime) { this.minTime = minTime; }
    public double getMaxTime() { return maxTime; }
    public void setMaxTime(double maxTime) { this.maxTime = maxTime; }
    public long getRows() { return rows; }
    public void setRows(long rows) { this.rows = rows; }
    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }
    public String getDatabase() { return database; }
    public void setDatabase(String database) { this.database = database; }
    
    public String getShortQuery() {
        if (query == null) return "";
        return query.length() > 100 ? query.substring(0, 97) + "..." : query;
    }
}
