package com.bovinemagnet.pgconsole.model;

/**
 * Slow query statistics from pg_stat_statements.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class SlowQuery {
    private String queryId;
    private String query;
    private long totalCalls;
    private double totalTime;
    private double meanTime;
    private double minTime;
    private double maxTime;
    private double stddevTime;
    private long rows;
    private long sharedBlksHit;
    private long sharedBlksRead;
    private long sharedBlksWritten;
    private long tempBlksRead;
    private long tempBlksWritten;
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

    public double getStddevTime() {
        return stddevTime;
    }

    public void setStddevTime(double stddevTime) {
        this.stddevTime = stddevTime;
    }

    public long getSharedBlksHit() {
        return sharedBlksHit;
    }

    public void setSharedBlksHit(long sharedBlksHit) {
        this.sharedBlksHit = sharedBlksHit;
    }

    public long getSharedBlksRead() {
        return sharedBlksRead;
    }

    public void setSharedBlksRead(long sharedBlksRead) {
        this.sharedBlksRead = sharedBlksRead;
    }

    public long getSharedBlksWritten() {
        return sharedBlksWritten;
    }

    public void setSharedBlksWritten(long sharedBlksWritten) {
        this.sharedBlksWritten = sharedBlksWritten;
    }

    public long getTempBlksRead() {
        return tempBlksRead;
    }

    public void setTempBlksRead(long tempBlksRead) {
        this.tempBlksRead = tempBlksRead;
    }

    public long getTempBlksWritten() {
        return tempBlksWritten;
    }

    public void setTempBlksWritten(long tempBlksWritten) {
        this.tempBlksWritten = tempBlksWritten;
    }

    public String getTotalTimeFormatted() {
        return formatTime(totalTime);
    }

    public String getMeanTimeFormatted() {
        return formatTime(meanTime);
    }

    public String getMinTimeFormatted() {
        return formatTime(minTime);
    }

    public String getMaxTimeFormatted() {
        return formatTime(maxTime);
    }

    public String getStddevTimeFormatted() {
        return formatTime(stddevTime);
    }

    private String formatTime(double ms) {
        if (ms < 1) {
            return String.format("%.3f ms", ms);
        } else if (ms < 1000) {
            return String.format("%.2f ms", ms);
        } else if (ms < 60000) {
            return String.format("%.2f s", ms / 1000);
        } else {
            return String.format("%.2f min", ms / 60000);
        }
    }

    public double getCacheHitRatio() {
        long total = sharedBlksHit + sharedBlksRead;
        if (total == 0) return 100.0;
        return (sharedBlksHit * 100.0) / total;
    }

    public String getCacheHitRatioFormatted() {
        return String.format("%.1f%%", getCacheHitRatio());
    }
}
