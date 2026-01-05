package com.bovinemagnet.pgconsole.model;

import java.util.List;

/**
 * Aggregated overview statistics for the PostgreSQL dashboard.
 * <p>
 * This model encapsulates the key performance metrics and resource utilisation
 * displayed on the main dashboard. It provides a comprehensive snapshot of the
 * database server's current state, including connection pool status, query activity,
 * cache performance, storage consumption, and the largest database objects.
 * <p>
 * The statistics are gathered from various PostgreSQL system views including
 * {@code pg_stat_activity}, {@code pg_stat_database}, and size-related functions.
 * <p>
 * This class is primarily used by {@link com.bovinemagnet.pgconsole.resource.DashboardResource}
 * to populate the overview dashboard template.
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see com.bovinemagnet.pgconsole.service.PostgresService#getOverviewStats()
 */
public class OverviewStats {

    /** PostgreSQL server version string (e.g., "PostgreSQL 14.5 on x86_64-pc-linux-gnu") */
    private String version;

    /** Current number of active database connections */
    private int connectionsUsed;

    /** Maximum allowed database connections (from {@code max_connections} setting) */
    private int connectionsMax;

    /** Number of queries currently in active execution state */
    private int activeQueries;

    /** Number of queries currently blocked waiting on locks */
    private int blockedQueries;

    /** Duration of the longest running query in human-readable format (e.g., "00:05:23") */
    private String longestQueryDuration;

    /** Cache hit ratio as a percentage (0.0 to 100.0) indicating buffer cache efficiency */
    private double cacheHitRatio;

    /** Total size of the current database in human-readable format (e.g., "1.2 GB") */
    private String databaseSize;

    /** Top tables ordered by size, typically limited to the largest 5-10 tables */
    private List<TableSize> topTablesBySize;

    /** Top indices ordered by size, typically limited to the largest 5-10 indices */
    private List<IndexSize> topIndexesBySize;

    /**
     * Returns the PostgreSQL server version string.
     *
     * @return the version string (e.g., "PostgreSQL 14.5 on x86_64-pc-linux-gnu"),
     *         or null if not yet populated
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the PostgreSQL server version string.
     *
     * @param version the version string from {@code version()} function
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Returns the number of currently active database connections.
     * <p>
     * This is an alias for {@link #getConnectionsUsed()} provided for
     * backwards API compatibility and template convenience.
     *
     * @return the current connection count
     * @see #getConnectionsUsed()
     */
    public int getActiveConnections() {
        return connectionsUsed;
    }

    /**
     * Returns the maximum allowed database connections.
     * <p>
     * This is an alias for {@link #getConnectionsMax()} provided for
     * backwards API compatibility and template convenience.
     *
     * @return the maximum connection limit from {@code max_connections}
     * @see #getConnectionsMax()
     */
    public int getMaxConnections() {
        return connectionsMax;
    }

    /**
     * Returns the current number of database connections in use.
     * <p>
     * This count includes all connections regardless of state (active, idle,
     * idle in transaction).
     *
     * @return the number of current connections
     */
    public int getConnectionsUsed() {
        return connectionsUsed;
    }

    /**
     * Sets the current number of database connections in use.
     *
     * @param connectionsUsed the connection count, must be non-negative
     */
    public void setConnectionsUsed(int connectionsUsed) {
        this.connectionsUsed = connectionsUsed;
    }

    /**
     * Returns the maximum allowed database connections.
     * <p>
     * This value corresponds to the PostgreSQL {@code max_connections}
     * configuration parameter.
     *
     * @return the maximum connection limit
     */
    public int getConnectionsMax() {
        return connectionsMax;
    }

    /**
     * Sets the maximum allowed database connections.
     *
     * @param connectionsMax the maximum connection limit, must be positive
     */
    public void setConnectionsMax(int connectionsMax) {
        this.connectionsMax = connectionsMax;
    }

    /**
     * Returns the number of queries currently in active execution.
     * <p>
     * This excludes idle connections and queries waiting on locks.
     *
     * @return the count of actively running queries
     */
    public int getActiveQueries() {
        return activeQueries;
    }

    /**
     * Sets the number of actively executing queries.
     *
     * @param activeQueries the active query count, must be non-negative
     */
    public void setActiveQueries(int activeQueries) {
        this.activeQueries = activeQueries;
    }

    /**
     * Returns the number of queries currently blocked waiting on locks.
     * <p>
     * A non-zero value indicates lock contention which may impact performance.
     * Use the locks dashboard for detailed blocking tree information.
     *
     * @return the count of blocked queries
     * @see com.bovinemagnet.pgconsole.model.BlockingTree
     */
    public int getBlockedQueries() {
        return blockedQueries;
    }

    /**
     * Sets the number of queries blocked by locks.
     *
     * @param blockedQueries the blocked query count, must be non-negative
     */
    public void setBlockedQueries(int blockedQueries) {
        this.blockedQueries = blockedQueries;
    }

    /**
     * Returns the duration of the longest running query in human-readable format.
     * <p>
     * The format is typically "HH:MM:SS" (e.g., "00:05:23" for 5 minutes 23 seconds).
     * Returns null or empty string if no queries are running.
     *
     * @return the formatted duration, or null if no queries are running
     */
    public String getLongestQueryDuration() {
        return longestQueryDuration;
    }

    /**
     * Sets the longest query duration.
     *
     * @param longestQueryDuration the duration in human-readable format
     */
    public void setLongestQueryDuration(String longestQueryDuration) {
        this.longestQueryDuration = longestQueryDuration;
    }

    /**
     * Returns the database cache hit ratio as a percentage.
     * <p>
     * This metric indicates the percentage of data requests satisfied from
     * PostgreSQL's shared buffer cache rather than requiring disk I/O.
     * Values above 95% generally indicate good cache performance.
     * <p>
     * The ratio is calculated from {@code pg_stat_database} statistics
     * (blks_hit / (blks_hit + blks_read)).
     *
     * @return the cache hit ratio as a percentage (0.0 to 100.0)
     */
    public double getCacheHitRatio() {
        return cacheHitRatio;
    }

    /**
     * Sets the cache hit ratio percentage.
     *
     * @param cacheHitRatio the ratio as a percentage, typically 0.0 to 100.0
     */
    public void setCacheHitRatio(double cacheHitRatio) {
        this.cacheHitRatio = cacheHitRatio;
    }

    /**
     * Returns the total database size in human-readable format.
     * <p>
     * The size is formatted with appropriate units (KB, MB, GB, TB) using
     * PostgreSQL's {@code pg_size_pretty()} function.
     *
     * @return the formatted database size (e.g., "1.2 GB"), or null if not available
     */
    public String getDatabaseSize() {
        return databaseSize;
    }

    /**
     * Sets the formatted database size.
     *
     * @param databaseSize the size in human-readable format
     */
    public void setDatabaseSize(String databaseSize) {
        this.databaseSize = databaseSize;
    }

    /**
     * Returns the list of largest tables by size.
     * <p>
     * The list is typically limited to the top 5-10 tables to avoid cluttering
     * the dashboard. Each entry includes schema, table name, and size information.
     *
     * @return the list of top tables, or null if not yet populated
     * @see TableSize
     */
    public List<TableSize> getTopTablesBySize() {
        return topTablesBySize;
    }

    /**
     * Sets the list of largest tables.
     *
     * @param topTablesBySize the list of table size information
     */
    public void setTopTablesBySize(List<TableSize> topTablesBySize) {
        this.topTablesBySize = topTablesBySize;
    }

    /**
     * Returns the list of largest indices by size.
     * <p>
     * The list is typically limited to the top 5-10 indices to avoid cluttering
     * the dashboard. Large unused indices may indicate optimisation opportunities.
     *
     * @return the list of top indices, or null if not yet populated
     * @see IndexSize
     */
    public List<IndexSize> getTopIndexesBySize() {
        return topIndexesBySize;
    }

    /**
     * Sets the list of largest indices.
     *
     * @param topIndexesBySize the list of index size information
     */
    public void setTopIndexesBySize(List<IndexSize> topIndexesBySize) {
        this.topIndexesBySize = topIndexesBySize;
    }

    /**
     * Calculates the connection pool utilisation as a percentage.
     * <p>
     * This convenience method computes the ratio of used connections to maximum
     * connections. High values (above 80%) may indicate the need to increase
     * {@code max_connections} or investigate connection pooling.
     *
     * @return the connection utilisation percentage (0 to 100), or 0 if max is zero
     */
    public int getConnectionPercentage() {
        if (connectionsMax == 0) return 0;
        return (int) ((connectionsUsed * 100.0) / connectionsMax);
    }

    /**
     * Returns the cache hit ratio formatted as a percentage string.
     * <p>
     * The value is formatted to one decimal place with a percentage sign
     * (e.g., "98.5%") for display in templates.
     *
     * @return the formatted cache hit ratio
     */
    public String getCacheHitRatioFormatted() {
        return String.format("%.1f%%", cacheHitRatio);
    }

    /**
     * Represents size information for a database table.
     * <p>
     * This nested class encapsulates both human-readable and byte-level size
     * information for a single table, including its schema qualification.
     * The size includes the table data, TOAST tables, and free space map,
     * but excludes associated indices.
     *
     * @see OverviewStats#getTopTablesBySize()
     */
    public static class TableSize {

        /** Schema name containing the table */
        private String schemaName;

        /** Unqualified table name */
        private String tableName;

        /** Human-readable size (e.g., "42 MB") from {@code pg_size_pretty()} */
        private String size;

        /** Exact size in bytes for sorting and calculations */
        private long sizeBytes;

        /**
         * Returns the schema name.
         *
         * @return the schema name (e.g., "public"), or null if not set
         */
        public String getSchemaName() {
            return schemaName;
        }

        /**
         * Sets the schema name.
         *
         * @param schemaName the schema name
         */
        public void setSchemaName(String schemaName) {
            this.schemaName = schemaName;
        }

        /**
         * Returns the unqualified table name.
         *
         * @return the table name, or null if not set
         */
        public String getTableName() {
            return tableName;
        }

        /**
         * Sets the table name.
         *
         * @param tableName the unqualified table name
         */
        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        /**
         * Returns the table size in human-readable format.
         * <p>
         * This is typically formatted by PostgreSQL's {@code pg_size_pretty()}
         * function with appropriate units (KB, MB, GB, TB).
         *
         * @return the formatted size (e.g., "42 MB"), or null if not set
         */
        public String getSize() {
            return size;
        }

        /**
         * Sets the human-readable size.
         *
         * @param size the formatted size string
         */
        public void setSize(String size) {
            this.size = size;
        }

        /**
         * Returns the exact table size in bytes.
         * <p>
         * This value is used for accurate sorting and size comparisons.
         *
         * @return the size in bytes
         */
        public long getSizeBytes() {
            return sizeBytes;
        }

        /**
         * Sets the exact size in bytes.
         *
         * @param sizeBytes the size in bytes, must be non-negative
         */
        public void setSizeBytes(long sizeBytes) {
            this.sizeBytes = sizeBytes;
        }

        /**
         * Returns the fully qualified table name.
         * <p>
         * Combines the schema and table name with a dot separator,
         * suitable for use in SQL queries and display.
         *
         * @return the qualified name (e.g., "public.users"), or just the table
         *         name if schema is null
         */
        public String getFullName() {
            return schemaName + "." + tableName;
        }
    }

    /**
     * Represents size information for a database index.
     * <p>
     * This nested class encapsulates both human-readable and byte-level size
     * information for a single index, including its schema qualification and
     * parent table association. Large indices may indicate opportunities for
     * optimisation, particularly if the index is unused or redundant.
     *
     * @see OverviewStats#getTopIndexesBySize()
     */
    public static class IndexSize {

        /** Schema name containing the index */
        private String schemaName;

        /** Index name */
        private String indexName;

        /** Name of the table this index belongs to */
        private String tableName;

        /** Human-readable size (e.g., "18 MB") from {@code pg_size_pretty()} */
        private String size;

        /** Exact size in bytes for sorting and calculations */
        private long sizeBytes;

        /**
         * Returns the schema name.
         *
         * @return the schema name (e.g., "public"), or null if not set
         */
        public String getSchemaName() {
            return schemaName;
        }

        /**
         * Sets the schema name.
         *
         * @param schemaName the schema name
         */
        public void setSchemaName(String schemaName) {
            this.schemaName = schemaName;
        }

        /**
         * Returns the index name.
         *
         * @return the index name, or null if not set
         */
        public String getIndexName() {
            return indexName;
        }

        /**
         * Sets the index name.
         *
         * @param indexName the index name
         */
        public void setIndexName(String indexName) {
            this.indexName = indexName;
        }

        /**
         * Returns the name of the table this index belongs to.
         * <p>
         * This information helps identify which table would be affected
         * if the index were to be dropped or rebuilt.
         *
         * @return the parent table name, or null if not set
         */
        public String getTableName() {
            return tableName;
        }

        /**
         * Sets the parent table name.
         *
         * @param tableName the table name
         */
        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        /**
         * Returns the index size in human-readable format.
         * <p>
         * This is typically formatted by PostgreSQL's {@code pg_size_pretty()}
         * function with appropriate units (KB, MB, GB, TB).
         *
         * @return the formatted size (e.g., "18 MB"), or null if not set
         */
        public String getSize() {
            return size;
        }

        /**
         * Sets the human-readable size.
         *
         * @param size the formatted size string
         */
        public void setSize(String size) {
            this.size = size;
        }

        /**
         * Returns the exact index size in bytes.
         * <p>
         * This value is used for accurate sorting and size comparisons.
         *
         * @return the size in bytes
         */
        public long getSizeBytes() {
            return sizeBytes;
        }

        /**
         * Sets the exact size in bytes.
         *
         * @param sizeBytes the size in bytes, must be non-negative
         */
        public void setSizeBytes(long sizeBytes) {
            this.sizeBytes = sizeBytes;
        }
    }
}
