package com.bovinemagnet.pgconsole.model;

/**
 * Represents comprehensive per-database performance metrics and statistics collected from PostgreSQL's
 * {@code pg_stat_database} system view and related metadata sources.
 * <p>
 * This class encapsulates all key metrics for monitoring database health, performance, and activity,
 * including transaction statistics, block I/O, tuple operations, session information, and timing data.
 * It provides both raw metric values and calculated ratios (cache hit ratio, commit ratio) to facilitate
 * performance analysis and comparison across databases.
 * <p>
 * Metrics are cumulative since the last statistics reset ({@link #statsReset}) and include:
 * <ul>
 *   <li><strong>Transaction metrics:</strong> commits, rollbacks, commit ratio</li>
 *   <li><strong>I/O metrics:</strong> blocks read from disk vs. cache, cache hit ratio, read/write times</li>
 *   <li><strong>Tuple metrics:</strong> rows returned, fetched, inserted, updated, deleted</li>
 *   <li><strong>Session metrics:</strong> active connections, session time, abandoned/fatal/killed sessions</li>
 *   <li><strong>Resource metrics:</strong> temp files/bytes, deadlocks, conflicts</li>
 *   <li><strong>Metadata:</strong> database size, pg_stat_statements availability, access permissions</li>
 * </ul>
 * <p>
 * This class is used throughout the dashboard to display database-level metrics in the overview,
 * databases list, and detailed database views. It supports both multi-database comparison and
 * drill-down analysis for individual databases.
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see com.bovinemagnet.pgconsole.service.PostgresService#getDatabaseMetrics(String)
 * @see com.bovinemagnet.pgconsole.service.PostgresService#getAllDatabaseMetrics()
 */
public class DatabaseMetrics {
    /**
     * Database OID (object identifier) from PostgreSQL's internal catalog.
     * Uniquely identifies the database within the PostgreSQL cluster.
     */
    private long datid;

    /**
     * Database name as it appears in the PostgreSQL cluster.
     * This is the name used in connection strings and SQL commands.
     */
    private String datname;

    /**
     * Number of backends (connections) currently connected to this database.
     * Includes active queries, idle connections, and idle-in-transaction sessions.
     */
    private int numBackends;

    /**
     * Number of transactions that have been committed in this database since statistics reset.
     * This is a cumulative counter that increases with each successful commit.
     */
    private long xactCommit;

    /**
     * Number of transactions that have been rolled back in this database since statistics reset.
     * High rollback counts may indicate application errors or constraint violations.
     */
    private long xactRollback;

    /**
     * Number of disk blocks read from storage (cache misses) since statistics reset.
     * High values relative to {@link #blksHit} indicate poor cache utilisation.
     */
    private long blksRead;

    /**
     * Number of disk blocks found in shared buffer cache (cache hits) since statistics reset.
     * High values relative to {@link #blksRead} indicate good cache utilisation.
     */
    private long blksHit;

    /**
     * Number of rows scanned and returned by queries since statistics reset.
     * This includes rows from sequential scans and index scans.
     */
    private long tupReturned;

    /**
     * Number of rows fetched by queries since statistics reset.
     * Typically lower than {@link #tupReturned} as it counts rows actually returned to clients.
     */
    private long tupFetched;

    /**
     * Number of rows inserted by INSERT operations since statistics reset.
     */
    private long tupInserted;

    /**
     * Number of rows updated by UPDATE operations since statistics reset.
     */
    private long tupUpdated;

    /**
     * Number of rows deleted by DELETE operations since statistics reset.
     */
    private long tupDeleted;

    /**
     * Number of queries cancelled due to conflicts with recovery in this database.
     * Only relevant for standby/replica databases during streaming replication.
     */
    private long conflicts;

    /**
     * Number of temporary files created by queries in this database.
     * Temporary files are created when work_mem is insufficient for sort or hash operations.
     */
    private long tempFiles;

    /**
     * Total amount of data written to temporary files by queries, in bytes.
     * Large values indicate queries requiring more work_mem allocation.
     */
    private long tempBytes;

    /**
     * Number of deadlocks detected in this database since statistics reset.
     * Deadlocks occur when two or more transactions block each other indefinitely.
     */
    private long deadlocks;

    /**
     * Time spent reading data file blocks by backends in this database, in milliseconds.
     * Only tracked when {@code track_io_timing} is enabled in PostgreSQL configuration.
     */
    private double blkReadTime;

    /**
     * Time spent writing data file blocks by backends in this database, in milliseconds.
     * Only tracked when {@code track_io_timing} is enabled in PostgreSQL configuration.
     */
    private double blkWriteTime;

    /**
     * Total time spent by database sessions (connections) in this database, in milliseconds.
     * Includes both active query time and idle time within sessions.
     */
    private double sessionTime;

    /**
     * Time spent executing active queries in this database, in milliseconds.
     * Excludes idle time, idle-in-transaction time, and connection overhead.
     */
    private double activeTime;

    /**
     * Time spent by sessions in idle-in-transaction state, in milliseconds.
     * High values indicate transactions being held open without activity, blocking vacuum and causing bloat.
     */
    private double idleInTransactionTime;

    /**
     * Total number of sessions (connections) established to this database since statistics reset.
     * Includes both current and terminated sessions.
     */
    private long sessions;

    /**
     * Number of database sessions that were terminated because the client disconnected unexpectedly.
     */
    private long sessionsAbandoned;

    /**
     * Number of database sessions that were terminated due to fatal errors.
     * Fatal errors typically result from out-of-memory conditions or internal errors.
     */
    private long sessionsFatal;

    /**
     * Number of database sessions that were terminated by administrative action (pg_terminate_backend).
     */
    private long sessionsKilled;

    /**
     * Timestamp when statistics for this database were last reset via pg_stat_reset().
     * Statistics are cumulative from this point forward. May be null if never reset.
     */
    private String statsReset;

    /**
     * Human-readable representation of the database size (e.g., "42.3 MB", "1.2 GB").
     * Includes all tables, indexes, and TOAST data for this database.
     */
    private String databaseSize;

    /**
     * Database size in bytes, used for sorting and programmatic comparisons.
     * Calculated using {@code pg_database_size()} function.
     */
    private long databaseSizeBytes;

    /**
     * Indicates whether the {@code pg_stat_statements} extension is installed and enabled for this database.
     * When true, detailed query statistics are available for slow query analysis.
     */
    private boolean pgStatStatementsEnabled;

    /**
     * Indicates whether the current user has access permissions to query this database's statistics.
     * Set to false when permission errors occur during metric collection.
     */
    private boolean hasAccess = true;

    // Getters and Setters

    /**
     * Returns the database OID (object identifier).
     *
     * @return the database OID from PostgreSQL's internal catalog
     */
    public long getDatid() {
        return datid;
    }

    /**
     * Sets the database OID (object identifier).
     *
     * @param datid the database OID to set
     */
    public void setDatid(long datid) {
        this.datid = datid;
    }

    /**
     * Returns the database name.
     *
     * @return the database name as it appears in the PostgreSQL cluster
     */
    public String getDatname() {
        return datname;
    }

    /**
     * Sets the database name.
     *
     * @param datname the database name to set
     */
    public void setDatname(String datname) {
        this.datname = datname;
    }

    /**
     * Returns the number of backends (connections) currently connected to this database.
     *
     * @return the current number of database connections
     */
    public int getNumBackends() {
        return numBackends;
    }

    /**
     * Sets the number of backends (connections) currently connected to this database.
     *
     * @param numBackends the number of backends to set
     */
    public void setNumBackends(int numBackends) {
        this.numBackends = numBackends;
    }

    /**
     * Returns the number of committed transactions since statistics reset.
     *
     * @return the cumulative count of transaction commits
     */
    public long getXactCommit() {
        return xactCommit;
    }

    /**
     * Sets the number of committed transactions.
     *
     * @param xactCommit the transaction commit count to set
     */
    public void setXactCommit(long xactCommit) {
        this.xactCommit = xactCommit;
    }

    /**
     * Returns the number of rolled back transactions since statistics reset.
     *
     * @return the cumulative count of transaction rollbacks
     */
    public long getXactRollback() {
        return xactRollback;
    }

    /**
     * Sets the number of rolled back transactions.
     *
     * @param xactRollback the transaction rollback count to set
     */
    public void setXactRollback(long xactRollback) {
        this.xactRollback = xactRollback;
    }

    /**
     * Returns the number of disk blocks read from storage (cache misses).
     *
     * @return the cumulative count of blocks read from disk
     */
    public long getBlksRead() {
        return blksRead;
    }

    /**
     * Sets the number of disk blocks read from storage.
     *
     * @param blksRead the blocks read count to set
     */
    public void setBlksRead(long blksRead) {
        this.blksRead = blksRead;
    }

    /**
     * Returns the number of disk blocks found in shared buffer cache (cache hits).
     *
     * @return the cumulative count of blocks found in cache
     */
    public long getBlksHit() {
        return blksHit;
    }

    /**
     * Sets the number of disk blocks found in shared buffer cache.
     *
     * @param blksHit the blocks hit count to set
     */
    public void setBlksHit(long blksHit) {
        this.blksHit = blksHit;
    }

    /**
     * Returns the number of rows scanned and returned by queries.
     *
     * @return the cumulative count of tuples returned
     */
    public long getTupReturned() {
        return tupReturned;
    }

    /**
     * Sets the number of rows scanned and returned by queries.
     *
     * @param tupReturned the tuples returned count to set
     */
    public void setTupReturned(long tupReturned) {
        this.tupReturned = tupReturned;
    }

    /**
     * Returns the number of rows fetched by queries.
     *
     * @return the cumulative count of tuples fetched
     */
    public long getTupFetched() {
        return tupFetched;
    }

    /**
     * Sets the number of rows fetched by queries.
     *
     * @param tupFetched the tuples fetched count to set
     */
    public void setTupFetched(long tupFetched) {
        this.tupFetched = tupFetched;
    }

    /**
     * Returns the number of rows inserted.
     *
     * @return the cumulative count of tuples inserted
     */
    public long getTupInserted() {
        return tupInserted;
    }

    /**
     * Sets the number of rows inserted.
     *
     * @param tupInserted the tuples inserted count to set
     */
    public void setTupInserted(long tupInserted) {
        this.tupInserted = tupInserted;
    }

    /**
     * Returns the number of rows updated.
     *
     * @return the cumulative count of tuples updated
     */
    public long getTupUpdated() {
        return tupUpdated;
    }

    /**
     * Sets the number of rows updated.
     *
     * @param tupUpdated the tuples updated count to set
     */
    public void setTupUpdated(long tupUpdated) {
        this.tupUpdated = tupUpdated;
    }

    /**
     * Returns the number of rows deleted.
     *
     * @return the cumulative count of tuples deleted
     */
    public long getTupDeleted() {
        return tupDeleted;
    }

    /**
     * Sets the number of rows deleted.
     *
     * @param tupDeleted the tuples deleted count to set
     */
    public void setTupDeleted(long tupDeleted) {
        this.tupDeleted = tupDeleted;
    }

    /**
     * Returns the number of queries cancelled due to recovery conflicts.
     *
     * @return the cumulative count of conflicts
     */
    public long getConflicts() {
        return conflicts;
    }

    /**
     * Sets the number of queries cancelled due to recovery conflicts.
     *
     * @param conflicts the conflicts count to set
     */
    public void setConflicts(long conflicts) {
        this.conflicts = conflicts;
    }

    /**
     * Returns the number of temporary files created by queries.
     *
     * @return the cumulative count of temporary files
     */
    public long getTempFiles() {
        return tempFiles;
    }

    /**
     * Sets the number of temporary files created by queries.
     *
     * @param tempFiles the temporary files count to set
     */
    public void setTempFiles(long tempFiles) {
        this.tempFiles = tempFiles;
    }

    /**
     * Returns the total bytes written to temporary files.
     *
     * @return the cumulative bytes written to temporary files
     */
    public long getTempBytes() {
        return tempBytes;
    }

    /**
     * Sets the total bytes written to temporary files.
     *
     * @param tempBytes the temporary bytes to set
     */
    public void setTempBytes(long tempBytes) {
        this.tempBytes = tempBytes;
    }

    /**
     * Returns the number of deadlocks detected.
     *
     * @return the cumulative count of deadlocks
     */
    public long getDeadlocks() {
        return deadlocks;
    }

    /**
     * Sets the number of deadlocks detected.
     *
     * @param deadlocks the deadlocks count to set
     */
    public void setDeadlocks(long deadlocks) {
        this.deadlocks = deadlocks;
    }

    /**
     * Returns the time spent reading data file blocks, in milliseconds.
     * Only populated when {@code track_io_timing} is enabled.
     *
     * @return the cumulative block read time in milliseconds
     */
    public double getBlkReadTime() {
        return blkReadTime;
    }

    /**
     * Sets the time spent reading data file blocks.
     *
     * @param blkReadTime the block read time in milliseconds to set
     */
    public void setBlkReadTime(double blkReadTime) {
        this.blkReadTime = blkReadTime;
    }

    /**
     * Returns the time spent writing data file blocks, in milliseconds.
     * Only populated when {@code track_io_timing} is enabled.
     *
     * @return the cumulative block write time in milliseconds
     */
    public double getBlkWriteTime() {
        return blkWriteTime;
    }

    /**
     * Sets the time spent writing data file blocks.
     *
     * @param blkWriteTime the block write time in milliseconds to set
     */
    public void setBlkWriteTime(double blkWriteTime) {
        this.blkWriteTime = blkWriteTime;
    }

    /**
     * Returns the total time spent by database sessions, in milliseconds.
     *
     * @return the cumulative session time in milliseconds
     */
    public double getSessionTime() {
        return sessionTime;
    }

    /**
     * Sets the total time spent by database sessions.
     *
     * @param sessionTime the session time in milliseconds to set
     */
    public void setSessionTime(double sessionTime) {
        this.sessionTime = sessionTime;
    }

    /**
     * Returns the time spent executing active queries, in milliseconds.
     *
     * @return the cumulative active query time in milliseconds
     */
    public double getActiveTime() {
        return activeTime;
    }

    /**
     * Sets the time spent executing active queries.
     *
     * @param activeTime the active time in milliseconds to set
     */
    public void setActiveTime(double activeTime) {
        this.activeTime = activeTime;
    }

    /**
     * Returns the time spent in idle-in-transaction state, in milliseconds.
     *
     * @return the cumulative idle-in-transaction time in milliseconds
     */
    public double getIdleInTransactionTime() {
        return idleInTransactionTime;
    }

    /**
     * Sets the time spent in idle-in-transaction state.
     *
     * @param idleInTransactionTime the idle-in-transaction time in milliseconds to set
     */
    public void setIdleInTransactionTime(double idleInTransactionTime) {
        this.idleInTransactionTime = idleInTransactionTime;
    }

    /**
     * Returns the total number of sessions established to this database.
     *
     * @return the cumulative count of sessions
     */
    public long getSessions() {
        return sessions;
    }

    /**
     * Sets the total number of sessions established to this database.
     *
     * @param sessions the sessions count to set
     */
    public void setSessions(long sessions) {
        this.sessions = sessions;
    }

    /**
     * Returns the number of sessions terminated by client disconnect.
     *
     * @return the cumulative count of abandoned sessions
     */
    public long getSessionsAbandoned() {
        return sessionsAbandoned;
    }

    /**
     * Sets the number of sessions terminated by client disconnect.
     *
     * @param sessionsAbandoned the abandoned sessions count to set
     */
    public void setSessionsAbandoned(long sessionsAbandoned) {
        this.sessionsAbandoned = sessionsAbandoned;
    }

    /**
     * Returns the number of sessions terminated due to fatal errors.
     *
     * @return the cumulative count of fatal sessions
     */
    public long getSessionsFatal() {
        return sessionsFatal;
    }

    /**
     * Sets the number of sessions terminated due to fatal errors.
     *
     * @param sessionsFatal the fatal sessions count to set
     */
    public void setSessionsFatal(long sessionsFatal) {
        this.sessionsFatal = sessionsFatal;
    }

    /**
     * Returns the number of sessions terminated by administrative action.
     *
     * @return the cumulative count of killed sessions
     */
    public long getSessionsKilled() {
        return sessionsKilled;
    }

    /**
     * Sets the number of sessions terminated by administrative action.
     *
     * @param sessionsKilled the killed sessions count to set
     */
    public void setSessionsKilled(long sessionsKilled) {
        this.sessionsKilled = sessionsKilled;
    }

    /**
     * Returns the timestamp when statistics were last reset.
     *
     * @return the statistics reset timestamp, or null if never reset
     */
    public String getStatsReset() {
        return statsReset;
    }

    /**
     * Sets the timestamp when statistics were last reset.
     *
     * @param statsReset the statistics reset timestamp to set
     */
    public void setStatsReset(String statsReset) {
        this.statsReset = statsReset;
    }

    /**
     * Returns the human-readable database size.
     *
     * @return the formatted database size (e.g., "42.3 MB", "1.2 GB")
     */
    public String getDatabaseSize() {
        return databaseSize;
    }

    /**
     * Sets the human-readable database size.
     *
     * @param databaseSize the formatted database size to set
     */
    public void setDatabaseSize(String databaseSize) {
        this.databaseSize = databaseSize;
    }

    /**
     * Returns the database size in bytes.
     *
     * @return the database size in bytes
     */
    public long getDatabaseSizeBytes() {
        return databaseSizeBytes;
    }

    /**
     * Sets the database size in bytes.
     *
     * @param databaseSizeBytes the database size in bytes to set
     */
    public void setDatabaseSizeBytes(long databaseSizeBytes) {
        this.databaseSizeBytes = databaseSizeBytes;
    }

    /**
     * Indicates whether the pg_stat_statements extension is enabled.
     *
     * @return true if pg_stat_statements is installed and enabled, false otherwise
     */
    public boolean isPgStatStatementsEnabled() {
        return pgStatStatementsEnabled;
    }

    /**
     * Sets whether the pg_stat_statements extension is enabled.
     *
     * @param pgStatStatementsEnabled true if pg_stat_statements is enabled
     */
    public void setPgStatStatementsEnabled(boolean pgStatStatementsEnabled) {
        this.pgStatStatementsEnabled = pgStatStatementsEnabled;
    }

    /**
     * Indicates whether the current user has access to query this database's statistics.
     *
     * @return true if access is permitted, false if permission errors occurred
     */
    public boolean isHasAccess() {
        return hasAccess;
    }

    /**
     * Sets whether the current user has access to query this database's statistics.
     *
     * @param hasAccess true if access is permitted
     */
    public void setHasAccess(boolean hasAccess) {
        this.hasAccess = hasAccess;
    }

    // Calculated fields

    /**
     * Calculates the cache hit ratio as a percentage.
     * <p>
     * The cache hit ratio indicates how effectively PostgreSQL's shared buffer cache is being used.
     * A high ratio (above 95%) suggests that most data requests are satisfied from memory rather than
     * requiring disk I/O. A low ratio may indicate insufficient shared_buffers configuration or a
     * working set larger than available cache.
     *
     * @return the cache hit percentage (0-100), or 100.0 if no blocks have been accessed
     */
    public double getCacheHitRatio() {
        long total = blksHit + blksRead;
        if (total == 0) return 100.0;
        return (blksHit * 100.0) / total;
    }

    /**
     * Returns the cache hit ratio formatted as a percentage string.
     *
     * @return the cache hit ratio formatted to one decimal place with '%' symbol (e.g., "98.5%")
     * @see #getCacheHitRatio()
     */
    public String getCacheHitRatioFormatted() {
        return String.format("%.1f%%", getCacheHitRatio());
    }

    /**
     * Calculates the transaction commit ratio as a percentage.
     * <p>
     * The commit ratio indicates the proportion of transactions that complete successfully versus
     * those that roll back. A high ratio (above 95%) is normal for healthy applications. A low ratio
     * may indicate frequent application errors, constraint violations, or intentional rollbacks in
     * application logic.
     *
     * @return the commit percentage (0-100), or 100.0 if no transactions have occurred
     */
    public double getCommitRatio() {
        long total = xactCommit + xactRollback;
        if (total == 0) return 100.0;
        return (xactCommit * 100.0) / total;
    }

    /**
     * Returns the commit ratio formatted as a percentage string.
     *
     * @return the commit ratio formatted to one decimal place with '%' symbol (e.g., "99.2%")
     * @see #getCommitRatio()
     */
    public String getCommitRatioFormatted() {
        return String.format("%.1f%%", getCommitRatio());
    }

    /**
     * Calculates the total number of transactions (commits plus rollbacks).
     *
     * @return the sum of committed and rolled back transactions since statistics reset
     */
    public long getTotalTransactions() {
        return xactCommit + xactRollback;
    }

    /**
     * Returns the temporary bytes usage formatted as a human-readable string.
     *
     * @return the temp bytes formatted with appropriate unit (B, KB, MB, or GB)
     * @see #getTempBytes()
     */
    public String getTempBytesFormatted() {
        return formatBytes(tempBytes);
    }

    /**
     * Returns the block read time formatted as a human-readable string.
     *
     * @return the block read time formatted with appropriate unit (ms, s, min, or hrs)
     * @see #getBlkReadTime()
     */
    public String getBlkReadTimeFormatted() {
        return formatTime(blkReadTime);
    }

    /**
     * Returns the block write time formatted as a human-readable string.
     *
     * @return the block write time formatted with appropriate unit (ms, s, min, or hrs)
     * @see #getBlkWriteTime()
     */
    public String getBlkWriteTimeFormatted() {
        return formatTime(blkWriteTime);
    }

    /**
     * Returns the session time formatted as a human-readable string.
     *
     * @return the session time formatted with appropriate unit (ms, s, min, or hrs)
     * @see #getSessionTime()
     */
    public String getSessionTimeFormatted() {
        return formatTime(sessionTime);
    }

    /**
     * Returns the active time formatted as a human-readable string.
     *
     * @return the active time formatted with appropriate unit (ms, s, min, or hrs)
     * @see #getActiveTime()
     */
    public String getActiveTimeFormatted() {
        return formatTime(activeTime);
    }

    /**
     * Returns the idle-in-transaction time formatted as a human-readable string.
     *
     * @return the idle-in-transaction time formatted with appropriate unit (ms, s, min, or hrs)
     * @see #getIdleInTransactionTime()
     */
    public String getIdleInTransactionTimeFormatted() {
        return formatTime(idleInTransactionTime);
    }

    /**
     * Formats a byte count into a human-readable string with appropriate unit.
     * <p>
     * Automatically selects the most appropriate unit (B, KB, MB, or GB) based on the magnitude
     * of the byte count, formatting to one decimal place for units larger than bytes.
     *
     * @param bytes the number of bytes to format
     * @return the formatted string with unit (e.g., "42 B", "1.5 KB", "128.7 MB", "2.3 GB")
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * Formats a time duration in milliseconds into a human-readable string with appropriate unit.
     * <p>
     * Automatically selects the most appropriate unit (ms, s, min, or hrs) based on the magnitude
     * of the time value. Milliseconds are formatted as whole numbers, while larger units use one
     * decimal place.
     *
     * @param ms the time duration in milliseconds
     * @return the formatted string with unit (e.g., "250 ms", "5.2 s", "12.8 min", "3.5 hrs")
     */
    private String formatTime(double ms) {
        if (ms < 1000) return String.format("%.0f ms", ms);
        if (ms < 60000) return String.format("%.1f s", ms / 1000);
        if (ms < 3600000) return String.format("%.1f min", ms / 60000);
        return String.format("%.1f hrs", ms / 3600000);
    }
}
