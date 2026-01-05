package com.bovinemagnet.pgconsole.model;

import java.time.Instant;

/**
 * Per-database metrics snapshot for historical tracking and trend analysis.
 * <p>
 * This class represents a point-in-time snapshot of database-level statistics
 * captured from PostgreSQL's {@code pg_stat_database} system view. Snapshots are
 * periodically sampled by {@code MetricsSamplerService} and persisted to the
 * {@code pgconsole.database_metrics_history} table for historical trend analysis.
 * <p>
 * Unlike {@link DatabaseMetrics}, which represents current real-time metrics,
 * this class is optimised for storage and retrieval of historical data, containing
 * only the most critical metrics needed for trend visualisation and performance
 * analysis over time.
 * <p>
 * Key use cases include:
 * <ul>
 * <li>Generating sparklines for dashboard widgets</li>
 * <li>Tracking cache hit ratio trends over time</li>
 * <li>Monitoring transaction commit/rollback patterns</li>
 * <li>Identifying gradual performance degradation</li>
 * <li>Analysing database growth patterns</li>
 * </ul>
 * <p>
 * Historical data is automatically pruned based on the configured retention period
 * (default: 7 days) to prevent unbounded growth.
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see DatabaseMetrics
 * @see com.bovinemagnet.pgconsole.service.MetricsSamplerService
 * @see com.bovinemagnet.pgconsole.repository.HistoryRepository
 */
public class DatabaseMetricsHistory {
    /**
     * Primary key for the history record.
     */
    private Long id;

    /**
     * Timestamp when this metrics snapshot was captured.
     * <p>
     * This is set to the current time when the snapshot is sampled, not when
     * the underlying PostgreSQL statistics were last updated.
     */
    private Instant sampledAt;

    /**
     * Name of the database these metrics apply to.
     * <p>
     * Corresponds to {@code datname} in PostgreSQL's {@code pg_stat_database} view.
     */
    private String databaseName;

    /**
     * Number of active backend connections to this database at sample time.
     * <p>
     * Includes all connection types (active queries, idle, idle in transaction).
     * Corresponds to {@code numbackends} in {@code pg_stat_database}.
     */
    private int numBackends;

    /**
     * Cumulative count of transactions committed in this database since statistics reset.
     * <p>
     * This is a monotonically increasing counter. To calculate commit rate,
     * compute the difference between consecutive samples and divide by the time interval.
     * Corresponds to {@code xact_commit} in {@code pg_stat_database}.
     */
    private long xactCommit;

    /**
     * Cumulative count of transactions rolled back in this database since statistics reset.
     * <p>
     * High rollback counts relative to commits may indicate application logic issues,
     * constraint violations, or deadlock resolution. Corresponds to {@code xact_rollback}
     * in {@code pg_stat_database}.
     */
    private long xactRollback;

    /**
     * Cumulative count of disk blocks read from shared buffer cache (cache hits).
     * <p>
     * This represents data blocks that were found in PostgreSQL's shared buffers
     * and did not require physical disk I/O. Higher values indicate better cache
     * performance. Corresponds to {@code blks_hit} in {@code pg_stat_database}.
     *
     * @see #blksRead
     * @see #cacheHitRatio
     */
    private long blksHit;

    /**
     * Cumulative count of disk blocks physically read from disk (cache misses).
     * <p>
     * This represents data blocks that were not found in cache and required
     * physical disk I/O. Lower values indicate better cache performance.
     * Corresponds to {@code blks_read} in {@code pg_stat_database}.
     *
     * @see #blksHit
     * @see #cacheHitRatio
     */
    private long blksRead;

    /**
     * Calculated cache hit ratio as a percentage (0-100).
     * <p>
     * Computed as {@code (blks_hit * 100.0) / (blks_hit + blks_read)}. Values above
     * 95% are generally considered healthy for production databases. Null if
     * no blocks have been read yet (cold start).
     *
     * @see #blksHit
     * @see #blksRead
     */
    private Double cacheHitRatio;

    /**
     * Cumulative count of rows returned by queries in this database.
     * <p>
     * This includes all rows scanned, even those filtered out by WHERE clauses.
     * A high ratio of {@code tup_returned} to {@code tup_fetched} may indicate
     * inefficient queries doing excessive sequential scans. Corresponds to
     * {@code tup_returned} in {@code pg_stat_database}.
     *
     * @see #tupFetched
     */
    private Long tupReturned;

    /**
     * Cumulative count of rows fetched (retrieved) by queries in this database.
     * <p>
     * This represents rows actually returned to the client after filtering.
     * Corresponds to {@code tup_fetched} in {@code pg_stat_database}.
     *
     * @see #tupReturned
     */
    private Long tupFetched;

    /**
     * Cumulative count of rows inserted into tables in this database.
     * <p>
     * Useful for tracking write workload patterns and data growth over time.
     * Corresponds to {@code tup_inserted} in {@code pg_stat_database}.
     */
    private Long tupInserted;

    /**
     * Cumulative count of rows updated in tables in this database.
     * <p>
     * High update volumes may indicate increased table bloat and the need
     * for more frequent vacuuming. Corresponds to {@code tup_updated} in
     * {@code pg_stat_database}.
     */
    private Long tupUpdated;

    /**
     * Cumulative count of rows deleted from tables in this database.
     * <p>
     * High delete volumes may indicate increased table bloat and the need
     * for more frequent vacuuming. Corresponds to {@code tup_deleted} in
     * {@code pg_stat_database}.
     */
    private Long tupDeleted;

    /**
     * Cumulative count of deadlocks detected in this database.
     * <p>
     * Deadlocks occur when two or more transactions are waiting for each other
     * to release locks. PostgreSQL automatically detects and resolves deadlocks
     * by aborting one of the transactions. Non-zero values indicate potential
     * application logic issues with transaction ordering. Corresponds to
     * {@code deadlocks} in {@code pg_stat_database}.
     */
    private Long deadlocks;

    /**
     * Cumulative count of query conflicts in this database.
     * <p>
     * On primary servers, this is typically zero. On standby servers, this counts
     * queries cancelled due to conflicts with recovery (e.g., when recovery needs
     * to apply a WAL record that would invalidate an active query's snapshot).
     * Corresponds to {@code conflicts} in {@code pg_stat_database}.
     */
    private Long conflicts;

    /**
     * Cumulative count of temporary files created for query execution.
     * <p>
     * Temporary files are created when queries require more memory than
     * {@code work_mem} allows for operations like sorts, hashes, and aggregations.
     * High values indicate queries are spilling to disk, which significantly
     * impacts performance. Corresponds to {@code temp_files} in {@code pg_stat_database}.
     *
     * @see #tempBytes
     */
    private Long tempFiles;

    /**
     * Cumulative size in bytes of temporary files created for query execution.
     * <p>
     * Tracks the total volume of data spilled to disk when query operations exceed
     * available {@code work_mem}. Increasing {@code work_mem} may reduce this value
     * at the cost of higher memory usage. Corresponds to {@code temp_bytes} in
     * {@code pg_stat_database}.
     *
     * @see #tempFiles
     */
    private Long tempBytes;

    /**
     * Total size of the database in bytes at sample time.
     * <p>
     * This includes all tables, indexes, and TOAST data for the database.
     * Calculated using {@code pg_database_size(datname)}. Useful for tracking
     * database growth trends and capacity planning.
     */
    private Long databaseSizeBytes;

    // Getters and Setters

    /**
     * Returns the primary key for this history record.
     *
     * @return the record identifier, or null if not yet persisted
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the primary key for this history record.
     *
     * @param id the record identifier
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns the timestamp when this metrics snapshot was captured.
     *
     * @return the sample timestamp
     */
    public Instant getSampledAt() {
        return sampledAt;
    }

    /**
     * Sets the timestamp when this metrics snapshot was captured.
     *
     * @param sampledAt the sample timestamp
     */
    public void setSampledAt(Instant sampledAt) {
        this.sampledAt = sampledAt;
    }

    /**
     * Returns the name of the database these metrics apply to.
     *
     * @return the database name
     */
    public String getDatabaseName() {
        return databaseName;
    }

    /**
     * Sets the name of the database these metrics apply to.
     *
     * @param databaseName the database name
     */
    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    /**
     * Returns the number of active backend connections at sample time.
     *
     * @return the connection count
     */
    public int getNumBackends() {
        return numBackends;
    }

    /**
     * Sets the number of active backend connections at sample time.
     *
     * @param numBackends the connection count
     */
    public void setNumBackends(int numBackends) {
        this.numBackends = numBackends;
    }

    /**
     * Returns the cumulative count of committed transactions.
     *
     * @return the commit count since statistics reset
     */
    public long getXactCommit() {
        return xactCommit;
    }

    /**
     * Sets the cumulative count of committed transactions.
     *
     * @param xactCommit the commit count since statistics reset
     */
    public void setXactCommit(long xactCommit) {
        this.xactCommit = xactCommit;
    }

    /**
     * Returns the cumulative count of rolled back transactions.
     *
     * @return the rollback count since statistics reset
     */
    public long getXactRollback() {
        return xactRollback;
    }

    /**
     * Sets the cumulative count of rolled back transactions.
     *
     * @param xactRollback the rollback count since statistics reset
     */
    public void setXactRollback(long xactRollback) {
        this.xactRollback = xactRollback;
    }

    /**
     * Returns the cumulative count of cache hits (blocks read from buffer cache).
     *
     * @return the cache hit count
     */
    public long getBlksHit() {
        return blksHit;
    }

    /**
     * Sets the cumulative count of cache hits (blocks read from buffer cache).
     *
     * @param blksHit the cache hit count
     */
    public void setBlksHit(long blksHit) {
        this.blksHit = blksHit;
    }

    /**
     * Returns the cumulative count of cache misses (blocks read from disk).
     *
     * @return the cache miss count
     */
    public long getBlksRead() {
        return blksRead;
    }

    /**
     * Sets the cumulative count of cache misses (blocks read from disk).
     *
     * @param blksRead the cache miss count
     */
    public void setBlksRead(long blksRead) {
        this.blksRead = blksRead;
    }

    /**
     * Returns the calculated cache hit ratio as a percentage.
     *
     * @return the cache hit ratio (0-100), or null if no data available
     */
    public Double getCacheHitRatio() {
        return cacheHitRatio;
    }

    /**
     * Sets the calculated cache hit ratio as a percentage.
     *
     * @param cacheHitRatio the cache hit ratio (0-100)
     */
    public void setCacheHitRatio(Double cacheHitRatio) {
        this.cacheHitRatio = cacheHitRatio;
    }

    /**
     * Returns the cumulative count of rows returned by queries.
     *
     * @return the row return count, or null if not available
     */
    public Long getTupReturned() {
        return tupReturned;
    }

    /**
     * Sets the cumulative count of rows returned by queries.
     *
     * @param tupReturned the row return count
     */
    public void setTupReturned(Long tupReturned) {
        this.tupReturned = tupReturned;
    }

    /**
     * Returns the cumulative count of rows fetched by queries.
     *
     * @return the row fetch count, or null if not available
     */
    public Long getTupFetched() {
        return tupFetched;
    }

    /**
     * Sets the cumulative count of rows fetched by queries.
     *
     * @param tupFetched the row fetch count
     */
    public void setTupFetched(Long tupFetched) {
        this.tupFetched = tupFetched;
    }

    /**
     * Returns the cumulative count of rows inserted.
     *
     * @return the insert count, or null if not available
     */
    public Long getTupInserted() {
        return tupInserted;
    }

    /**
     * Sets the cumulative count of rows inserted.
     *
     * @param tupInserted the insert count
     */
    public void setTupInserted(Long tupInserted) {
        this.tupInserted = tupInserted;
    }

    /**
     * Returns the cumulative count of rows updated.
     *
     * @return the update count, or null if not available
     */
    public Long getTupUpdated() {
        return tupUpdated;
    }

    /**
     * Sets the cumulative count of rows updated.
     *
     * @param tupUpdated the update count
     */
    public void setTupUpdated(Long tupUpdated) {
        this.tupUpdated = tupUpdated;
    }

    /**
     * Returns the cumulative count of rows deleted.
     *
     * @return the delete count, or null if not available
     */
    public Long getTupDeleted() {
        return tupDeleted;
    }

    /**
     * Sets the cumulative count of rows deleted.
     *
     * @param tupDeleted the delete count
     */
    public void setTupDeleted(Long tupDeleted) {
        this.tupDeleted = tupDeleted;
    }

    /**
     * Returns the cumulative count of deadlocks detected.
     *
     * @return the deadlock count, or null if not available
     */
    public Long getDeadlocks() {
        return deadlocks;
    }

    /**
     * Sets the cumulative count of deadlocks detected.
     *
     * @param deadlocks the deadlock count
     */
    public void setDeadlocks(Long deadlocks) {
        this.deadlocks = deadlocks;
    }

    /**
     * Returns the cumulative count of query conflicts.
     *
     * @return the conflict count, or null if not available
     */
    public Long getConflicts() {
        return conflicts;
    }

    /**
     * Sets the cumulative count of query conflicts.
     *
     * @param conflicts the conflict count
     */
    public void setConflicts(Long conflicts) {
        this.conflicts = conflicts;
    }

    /**
     * Returns the cumulative count of temporary files created.
     *
     * @return the temporary file count, or null if not available
     */
    public Long getTempFiles() {
        return tempFiles;
    }

    /**
     * Sets the cumulative count of temporary files created.
     *
     * @param tempFiles the temporary file count
     */
    public void setTempFiles(Long tempFiles) {
        this.tempFiles = tempFiles;
    }

    /**
     * Returns the cumulative size in bytes of temporary files created.
     *
     * @return the temporary file size in bytes, or null if not available
     */
    public Long getTempBytes() {
        return tempBytes;
    }

    /**
     * Sets the cumulative size in bytes of temporary files created.
     *
     * @param tempBytes the temporary file size in bytes
     */
    public void setTempBytes(Long tempBytes) {
        this.tempBytes = tempBytes;
    }

    /**
     * Returns the total database size in bytes at sample time.
     *
     * @return the database size in bytes, or null if not available
     */
    public Long getDatabaseSizeBytes() {
        return databaseSizeBytes;
    }

    /**
     * Sets the total database size in bytes at sample time.
     *
     * @param databaseSizeBytes the database size in bytes
     */
    public void setDatabaseSizeBytes(Long databaseSizeBytes) {
        this.databaseSizeBytes = databaseSizeBytes;
    }
}
