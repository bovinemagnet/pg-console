package com.bovinemagnet.pgconsole.model;

/**
 * Per-database metrics from pg_stat_database.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class DatabaseMetrics {
    private long datid;
    private String datname;
    private int numBackends;
    private long xactCommit;
    private long xactRollback;
    private long blksRead;
    private long blksHit;
    private long tupReturned;
    private long tupFetched;
    private long tupInserted;
    private long tupUpdated;
    private long tupDeleted;
    private long conflicts;
    private long tempFiles;
    private long tempBytes;
    private long deadlocks;
    private double blkReadTime;
    private double blkWriteTime;
    private double sessionTime;
    private double activeTime;
    private double idleInTransactionTime;
    private long sessions;
    private long sessionsAbandoned;
    private long sessionsFatal;
    private long sessionsKilled;
    private String statsReset;
    private String databaseSize;
    private long databaseSizeBytes;
    private boolean pgStatStatementsEnabled;
    private boolean hasAccess = true;

    // Getters and Setters
    public long getDatid() {
        return datid;
    }

    public void setDatid(long datid) {
        this.datid = datid;
    }

    public String getDatname() {
        return datname;
    }

    public void setDatname(String datname) {
        this.datname = datname;
    }

    public int getNumBackends() {
        return numBackends;
    }

    public void setNumBackends(int numBackends) {
        this.numBackends = numBackends;
    }

    public long getXactCommit() {
        return xactCommit;
    }

    public void setXactCommit(long xactCommit) {
        this.xactCommit = xactCommit;
    }

    public long getXactRollback() {
        return xactRollback;
    }

    public void setXactRollback(long xactRollback) {
        this.xactRollback = xactRollback;
    }

    public long getBlksRead() {
        return blksRead;
    }

    public void setBlksRead(long blksRead) {
        this.blksRead = blksRead;
    }

    public long getBlksHit() {
        return blksHit;
    }

    public void setBlksHit(long blksHit) {
        this.blksHit = blksHit;
    }

    public long getTupReturned() {
        return tupReturned;
    }

    public void setTupReturned(long tupReturned) {
        this.tupReturned = tupReturned;
    }

    public long getTupFetched() {
        return tupFetched;
    }

    public void setTupFetched(long tupFetched) {
        this.tupFetched = tupFetched;
    }

    public long getTupInserted() {
        return tupInserted;
    }

    public void setTupInserted(long tupInserted) {
        this.tupInserted = tupInserted;
    }

    public long getTupUpdated() {
        return tupUpdated;
    }

    public void setTupUpdated(long tupUpdated) {
        this.tupUpdated = tupUpdated;
    }

    public long getTupDeleted() {
        return tupDeleted;
    }

    public void setTupDeleted(long tupDeleted) {
        this.tupDeleted = tupDeleted;
    }

    public long getConflicts() {
        return conflicts;
    }

    public void setConflicts(long conflicts) {
        this.conflicts = conflicts;
    }

    public long getTempFiles() {
        return tempFiles;
    }

    public void setTempFiles(long tempFiles) {
        this.tempFiles = tempFiles;
    }

    public long getTempBytes() {
        return tempBytes;
    }

    public void setTempBytes(long tempBytes) {
        this.tempBytes = tempBytes;
    }

    public long getDeadlocks() {
        return deadlocks;
    }

    public void setDeadlocks(long deadlocks) {
        this.deadlocks = deadlocks;
    }

    public double getBlkReadTime() {
        return blkReadTime;
    }

    public void setBlkReadTime(double blkReadTime) {
        this.blkReadTime = blkReadTime;
    }

    public double getBlkWriteTime() {
        return blkWriteTime;
    }

    public void setBlkWriteTime(double blkWriteTime) {
        this.blkWriteTime = blkWriteTime;
    }

    public double getSessionTime() {
        return sessionTime;
    }

    public void setSessionTime(double sessionTime) {
        this.sessionTime = sessionTime;
    }

    public double getActiveTime() {
        return activeTime;
    }

    public void setActiveTime(double activeTime) {
        this.activeTime = activeTime;
    }

    public double getIdleInTransactionTime() {
        return idleInTransactionTime;
    }

    public void setIdleInTransactionTime(double idleInTransactionTime) {
        this.idleInTransactionTime = idleInTransactionTime;
    }

    public long getSessions() {
        return sessions;
    }

    public void setSessions(long sessions) {
        this.sessions = sessions;
    }

    public long getSessionsAbandoned() {
        return sessionsAbandoned;
    }

    public void setSessionsAbandoned(long sessionsAbandoned) {
        this.sessionsAbandoned = sessionsAbandoned;
    }

    public long getSessionsFatal() {
        return sessionsFatal;
    }

    public void setSessionsFatal(long sessionsFatal) {
        this.sessionsFatal = sessionsFatal;
    }

    public long getSessionsKilled() {
        return sessionsKilled;
    }

    public void setSessionsKilled(long sessionsKilled) {
        this.sessionsKilled = sessionsKilled;
    }

    public String getStatsReset() {
        return statsReset;
    }

    public void setStatsReset(String statsReset) {
        this.statsReset = statsReset;
    }

    public String getDatabaseSize() {
        return databaseSize;
    }

    public void setDatabaseSize(String databaseSize) {
        this.databaseSize = databaseSize;
    }

    public long getDatabaseSizeBytes() {
        return databaseSizeBytes;
    }

    public void setDatabaseSizeBytes(long databaseSizeBytes) {
        this.databaseSizeBytes = databaseSizeBytes;
    }

    public boolean isPgStatStatementsEnabled() {
        return pgStatStatementsEnabled;
    }

    public void setPgStatStatementsEnabled(boolean pgStatStatementsEnabled) {
        this.pgStatStatementsEnabled = pgStatStatementsEnabled;
    }

    public boolean isHasAccess() {
        return hasAccess;
    }

    public void setHasAccess(boolean hasAccess) {
        this.hasAccess = hasAccess;
    }

    // Calculated fields
    public double getCacheHitRatio() {
        long total = blksHit + blksRead;
        if (total == 0) return 100.0;
        return (blksHit * 100.0) / total;
    }

    public String getCacheHitRatioFormatted() {
        return String.format("%.1f%%", getCacheHitRatio());
    }

    public double getCommitRatio() {
        long total = xactCommit + xactRollback;
        if (total == 0) return 100.0;
        return (xactCommit * 100.0) / total;
    }

    public String getCommitRatioFormatted() {
        return String.format("%.1f%%", getCommitRatio());
    }

    public long getTotalTransactions() {
        return xactCommit + xactRollback;
    }

    public String getTempBytesFormatted() {
        return formatBytes(tempBytes);
    }

    public String getBlkReadTimeFormatted() {
        return formatTime(blkReadTime);
    }

    public String getBlkWriteTimeFormatted() {
        return formatTime(blkWriteTime);
    }

    public String getSessionTimeFormatted() {
        return formatTime(sessionTime);
    }

    public String getActiveTimeFormatted() {
        return formatTime(activeTime);
    }

    public String getIdleInTransactionTimeFormatted() {
        return formatTime(idleInTransactionTime);
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private String formatTime(double ms) {
        if (ms < 1000) return String.format("%.0f ms", ms);
        if (ms < 60000) return String.format("%.1f s", ms / 1000);
        if (ms < 3600000) return String.format("%.1f min", ms / 60000);
        return String.format("%.1f hrs", ms / 3600000);
    }
}
