package com.bovinemagnet.pgconsole.model;

/**
 * Represents query performance statistics retrieved from PostgreSQL's {@code pg_stat_statements} extension.
 * <p>
 * This Data Transfer Object (DTO) encapsulates execution metrics for SQL queries including timing information,
 * execution counts, buffer I/O statistics, and cache hit ratios. It is primarily used to identify and analyse
 * slow or resource-intensive queries for performance optimisation.
 * <p>
 * The {@code pg_stat_statements} extension must be enabled in PostgreSQL for this data to be available.
 * Query text may be normalised (parameters replaced with placeholders) depending on PostgreSQL configuration.
 * <p>
 * Time values are measured in milliseconds and represent cumulative statistics since the last
 * {@code pg_stat_statements_reset()} call or server restart.
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see <a href="https://www.postgresql.org/docs/current/pgstatstatements.html">PostgreSQL pg_stat_statements Documentation</a>
 * @since 0.0.0
 */
public class SlowQuery {
    /** Unique identifier for the normalised query text. */
    private String queryId;

    /** The SQL query text, potentially normalised with parameter placeholders. */
    private String query;

    /** Total number of times this query has been executed. */
    private long totalCalls;

    /** Cumulative execution time across all calls, in milliseconds. */
    private double totalTime;

    /** Average execution time per call, in milliseconds. */
    private double meanTime;

    /** Minimum execution time observed, in milliseconds. */
    private double minTime;

    /** Maximum execution time observed, in milliseconds. */
    private double maxTime;

    /** Standard deviation of execution times, in milliseconds. Indicates execution time variability. */
    private double stddevTime;

    /** Total number of rows returned or affected by all executions. */
    private long rows;

    /** Number of shared buffer blocks satisfied from cache (buffer pool hits). */
    private long sharedBlksHit;

    /** Number of shared buffer blocks read from disk (buffer pool misses). */
    private long sharedBlksRead;

    /** Number of shared buffer blocks written to disk. */
    private long sharedBlksWritten;

    /** Number of temporary file blocks read. Non-zero indicates work_mem exhaustion. */
    private long tempBlksRead;

    /** Number of temporary file blocks written. Non-zero indicates work_mem exhaustion. */
    private long tempBlksWritten;

    /** PostgreSQL user who executed the query. */
    private String user;

    /** Database in which the query was executed. */
    private String database;

    /**
     * Constructs an empty SlowQuery instance.
     * <p>
     * All numeric fields default to zero, and string fields default to null.
     */
    public SlowQuery() {
    }

    /**
     * Constructs a SlowQuery with core timing and execution statistics.
     * <p>
     * This constructor initialises the most commonly used fields for query performance analysis.
     * Additional statistics (standard deviation, buffer I/O) must be set via setter methods.
     *
     * @param queryId unique identifier for the normalised query text
     * @param query the SQL query text
     * @param totalCalls total number of executions
     * @param totalTime cumulative execution time in milliseconds
     * @param meanTime average execution time in milliseconds
     * @param minTime minimum execution time in milliseconds
     * @param maxTime maximum execution time in milliseconds
     * @param rows total number of rows returned or affected
     * @param user PostgreSQL user who executed the query
     * @param database database in which the query was executed
     */
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

    /**
     * Returns the unique identifier for the normalised query text.
     *
     * @return the query identifier, or null if not set
     */
    public String getQueryId() { return queryId; }

    /**
     * Sets the unique identifier for the normalised query text.
     *
     * @param queryId the query identifier
     */
    public void setQueryId(String queryId) { this.queryId = queryId; }

    /**
     * Returns the SQL query text.
     * <p>
     * The text may be normalised with parameter placeholders (e.g., {@code $1}, {@code $2})
     * depending on PostgreSQL's {@code pg_stat_statements.track} configuration.
     *
     * @return the query text, or null if not set
     */
    public String getQuery() { return query; }

    /**
     * Sets the SQL query text.
     *
     * @param query the query text
     */
    public void setQuery(String query) { this.query = query; }

    /**
     * Returns the total number of times this query has been executed.
     *
     * @return the execution count
     */
    public long getTotalCalls() { return totalCalls; }

    /**
     * Sets the total number of times this query has been executed.
     *
     * @param totalCalls the execution count
     */
    public void setTotalCalls(long totalCalls) { this.totalCalls = totalCalls; }

    /**
     * Returns the cumulative execution time across all calls.
     *
     * @return the total execution time in milliseconds
     */
    public double getTotalTime() { return totalTime; }

    /**
     * Sets the cumulative execution time across all calls.
     *
     * @param totalTime the total execution time in milliseconds
     */
    public void setTotalTime(double totalTime) { this.totalTime = totalTime; }

    /**
     * Returns the average execution time per call.
     *
     * @return the mean execution time in milliseconds
     */
    public double getMeanTime() { return meanTime; }

    /**
     * Sets the average execution time per call.
     *
     * @param meanTime the mean execution time in milliseconds
     */
    public void setMeanTime(double meanTime) { this.meanTime = meanTime; }

    /**
     * Returns the minimum execution time observed.
     *
     * @return the minimum execution time in milliseconds
     */
    public double getMinTime() { return minTime; }

    /**
     * Sets the minimum execution time observed.
     *
     * @param minTime the minimum execution time in milliseconds
     */
    public void setMinTime(double minTime) { this.minTime = minTime; }

    /**
     * Returns the maximum execution time observed.
     *
     * @return the maximum execution time in milliseconds
     */
    public double getMaxTime() { return maxTime; }

    /**
     * Sets the maximum execution time observed.
     *
     * @param maxTime the maximum execution time in milliseconds
     */
    public void setMaxTime(double maxTime) { this.maxTime = maxTime; }

    /**
     * Returns the total number of rows returned or affected by all executions.
     *
     * @return the row count
     */
    public long getRows() { return rows; }

    /**
     * Sets the total number of rows returned or affected by all executions.
     *
     * @param rows the row count
     */
    public void setRows(long rows) { this.rows = rows; }

    /**
     * Returns the PostgreSQL user who executed the query.
     *
     * @return the username, or null if not set
     */
    public String getUser() { return user; }

    /**
     * Sets the PostgreSQL user who executed the query.
     *
     * @param user the username
     */
    public void setUser(String user) { this.user = user; }

    /**
     * Returns the database in which the query was executed.
     *
     * @return the database name, or null if not set
     */
    public String getDatabase() { return database; }

    /**
     * Sets the database in which the query was executed.
     *
     * @param database the database name
     */
    public void setDatabase(String database) { this.database = database; }

    /**
     * Returns a truncated version of the query text suitable for display in lists or tables.
     * <p>
     * If the query text exceeds 100 characters, it is truncated to 97 characters with
     * an ellipsis ({@code ...}) appended. This is useful for UI rendering where space is limited.
     *
     * @return truncated query text, or empty string if query is null
     */
    public String getShortQuery() {
        if (query == null) return "";
        return query.length() > 100 ? query.substring(0, 97) + "..." : query;
    }

    /**
     * Returns the standard deviation of execution times.
     * <p>
     * High standard deviation indicates inconsistent query performance, which may suggest
     * parameter-sensitive execution plans, caching effects, or resource contention.
     *
     * @return the standard deviation in milliseconds
     */
    public double getStddevTime() {
        return stddevTime;
    }

    /**
     * Sets the standard deviation of execution times.
     *
     * @param stddevTime the standard deviation in milliseconds
     */
    public void setStddevTime(double stddevTime) {
        this.stddevTime = stddevTime;
    }

    /**
     * Returns the number of shared buffer blocks satisfied from cache (buffer pool hits).
     * <p>
     * Higher values relative to {@link #getSharedBlksRead()} indicate better cache utilisation.
     *
     * @return the cache hit count in blocks
     * @see #getCacheHitRatio()
     */
    public long getSharedBlksHit() {
        return sharedBlksHit;
    }

    /**
     * Sets the number of shared buffer blocks satisfied from cache.
     *
     * @param sharedBlksHit the cache hit count in blocks
     */
    public void setSharedBlksHit(long sharedBlksHit) {
        this.sharedBlksHit = sharedBlksHit;
    }

    /**
     * Returns the number of shared buffer blocks read from disk (buffer pool misses).
     * <p>
     * Higher values indicate poor cache utilisation and increased disk I/O.
     *
     * @return the cache miss count in blocks
     * @see #getCacheHitRatio()
     */
    public long getSharedBlksRead() {
        return sharedBlksRead;
    }

    /**
     * Sets the number of shared buffer blocks read from disk.
     *
     * @param sharedBlksRead the cache miss count in blocks
     */
    public void setSharedBlksRead(long sharedBlksRead) {
        this.sharedBlksRead = sharedBlksRead;
    }

    /**
     * Returns the number of shared buffer blocks written to disk.
     * <p>
     * Non-zero values typically indicate checkpoint activity or dirty buffer eviction.
     *
     * @return the block write count
     */
    public long getSharedBlksWritten() {
        return sharedBlksWritten;
    }

    /**
     * Sets the number of shared buffer blocks written to disk.
     *
     * @param sharedBlksWritten the block write count
     */
    public void setSharedBlksWritten(long sharedBlksWritten) {
        this.sharedBlksWritten = sharedBlksWritten;
    }

    /**
     * Returns the number of temporary file blocks read.
     * <p>
     * Non-zero values indicate that the query exhausted {@code work_mem} and spilled to disk,
     * which significantly degrades performance. Consider increasing {@code work_mem} or
     * optimising the query to reduce memory requirements.
     *
     * @return the temporary block read count
     */
    public long getTempBlksRead() {
        return tempBlksRead;
    }

    /**
     * Sets the number of temporary file blocks read.
     *
     * @param tempBlksRead the temporary block read count
     */
    public void setTempBlksRead(long tempBlksRead) {
        this.tempBlksRead = tempBlksRead;
    }

    /**
     * Returns the number of temporary file blocks written.
     * <p>
     * Non-zero values indicate that the query exhausted {@code work_mem} and spilled to disk.
     * Operations that commonly trigger this include large sorts, hash joins, and aggregations.
     *
     * @return the temporary block write count
     */
    public long getTempBlksWritten() {
        return tempBlksWritten;
    }

    /**
     * Sets the number of temporary file blocks written.
     *
     * @param tempBlksWritten the temporary block write count
     */
    public void setTempBlksWritten(long tempBlksWritten) {
        this.tempBlksWritten = tempBlksWritten;
    }

    /**
     * Returns the total execution time formatted as a human-readable string.
     * <p>
     * The format adapts to the time magnitude: milliseconds, seconds, or minutes.
     *
     * @return formatted total time (e.g., "1.234 ms", "5.67 s", "2.30 min")
     * @see #formatTime(double)
     */
    public String getTotalTimeFormatted() {
        return formatTime(totalTime);
    }

    /**
     * Returns the mean execution time formatted as a human-readable string.
     * <p>
     * The format adapts to the time magnitude: milliseconds, seconds, or minutes.
     *
     * @return formatted mean time (e.g., "1.234 ms", "5.67 s", "2.30 min")
     * @see #formatTime(double)
     */
    public String getMeanTimeFormatted() {
        return formatTime(meanTime);
    }

    /**
     * Returns the minimum execution time formatted as a human-readable string.
     * <p>
     * The format adapts to the time magnitude: milliseconds, seconds, or minutes.
     *
     * @return formatted minimum time (e.g., "0.123 ms", "1.23 s", "1.50 min")
     * @see #formatTime(double)
     */
    public String getMinTimeFormatted() {
        return formatTime(minTime);
    }

    /**
     * Returns the maximum execution time formatted as a human-readable string.
     * <p>
     * The format adapts to the time magnitude: milliseconds, seconds, or minutes.
     *
     * @return formatted maximum time (e.g., "123.45 ms", "45.67 s", "10.20 min")
     * @see #formatTime(double)
     */
    public String getMaxTimeFormatted() {
        return formatTime(maxTime);
    }

    /**
     * Returns the standard deviation of execution time formatted as a human-readable string.
     * <p>
     * The format adapts to the time magnitude: milliseconds, seconds, or minutes.
     *
     * @return formatted standard deviation (e.g., "12.34 ms", "2.34 s", "1.50 min")
     * @see #formatTime(double)
     */
    public String getStddevTimeFormatted() {
        return formatTime(stddevTime);
    }

    /**
     * Formats a time value in milliseconds to a human-readable string.
     * <p>
     * The format adapts based on magnitude:
     * <ul>
     *   <li>Less than 1 ms: three decimal places (e.g., "0.123 ms")</li>
     *   <li>1 ms to 1000 ms: two decimal places (e.g., "123.45 ms")</li>
     *   <li>1 second to 60 seconds: converted to seconds with two decimal places (e.g., "12.34 s")</li>
     *   <li>Above 60 seconds: converted to minutes with two decimal places (e.g., "5.67 min")</li>
     * </ul>
     *
     * @param ms the time value in milliseconds
     * @return formatted time string with appropriate unit
     */
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

    /**
     * Calculates the buffer cache hit ratio for this query.
     * <p>
     * The ratio represents the percentage of buffer block accesses that were satisfied from
     * PostgreSQL's shared buffer cache without requiring disk I/O. Higher ratios indicate
     * better cache utilisation and performance.
     * <p>
     * Formula: {@code (sharedBlksHit / (sharedBlksHit + sharedBlksRead)) * 100}
     * <p>
     * Returns 100.0 if there were no buffer accesses, avoiding division by zero.
     *
     * @return cache hit ratio as a percentage (0.0 to 100.0)
     * @see #getSharedBlksHit()
     * @see #getSharedBlksRead()
     */
    public double getCacheHitRatio() {
        long total = sharedBlksHit + sharedBlksRead;
        if (total == 0) return 100.0;
        return (sharedBlksHit * 100.0) / total;
    }

    /**
     * Returns the cache hit ratio formatted as a percentage string with one decimal place.
     *
     * @return formatted cache hit ratio (e.g., "98.5%", "100.0%")
     * @see #getCacheHitRatio()
     */
    public String getCacheHitRatioFormatted() {
        return String.format("%.1f%%", getCacheHitRatio());
    }
}
