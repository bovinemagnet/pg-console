package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.Activity;
import com.bovinemagnet.pgconsole.model.BlockingTree;
import com.bovinemagnet.pgconsole.model.DatabaseInfo;
import com.bovinemagnet.pgconsole.model.DatabaseMetrics;
import com.bovinemagnet.pgconsole.model.ExplainPlan;
import com.bovinemagnet.pgconsole.model.InstanceInfo;
import com.bovinemagnet.pgconsole.model.LockInfo;
import com.bovinemagnet.pgconsole.model.OverviewStats;
import com.bovinemagnet.pgconsole.model.SlowQuery;
import com.bovinemagnet.pgconsole.model.TableStats;
import com.bovinemagnet.pgconsole.model.WaitEventSummary;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for querying PostgreSQL statistics and metrics.
 * Supports multiple database instances via DataSourceManager.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class PostgresService {

    private static final Logger LOG = Logger.getLogger(PostgresService.class);

    @Inject
    DataSourceManager dataSourceManager;

    @ConfigProperty(name = "pg-console.databases")
    Optional<String> databaseFilter;

    /**
     * Retrieves the datasource for the specified instance.
     *
     * @param instanceName the name of the PostgreSQL instance
     * @return the datasource for the instance
     */
    private DataSource getDataSource(String instanceName) {
        return dataSourceManager.getDataSource(instanceName);
    }

    /**
     * Retrieves the list of configured PostgreSQL instances with their connection status and metadata.
     *
     * @return list of instance information objects containing name, display name, connection status, and version details
     * @see DataSourceManager#getInstanceInfoList()
     */
    public List<InstanceInfo> getInstanceList() {
        return dataSourceManager.getInstanceInfoList();
    }

    /**
     * Retrieves the list of available PostgreSQL instance names.
     *
     * @return list of configured instance names
     * @see DataSourceManager#getAvailableInstances()
     */
    public List<String> getAvailableInstances() {
        return dataSourceManager.getAvailableInstances();
    }

    /**
     * Retrieves the set of database names to monitor based on configured filters.
     * <p>
     * If the filter configuration is empty or blank, returns an empty set,
     * which indicates that all non-template databases should be shown.
     *
     * @return set of database names to include in monitoring, or empty set to include all databases
     */
    private Set<String> getDatabaseFilterSet() {
        if (databaseFilter.isEmpty() || databaseFilter.get().isBlank()) {
            return Set.of();
        }
        return Arrays.stream(databaseFilter.get().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Determines whether the specified database should be included in monitoring based on configured filters.
     * <p>
     * If no filter is configured (empty set), all databases are included.
     *
     * @param dbName the name of the database to check
     * @return true if the database should be included, false otherwise
     */
    private boolean shouldIncludeDatabase(String dbName) {
        Set<String> filter = getDatabaseFilterSet();
        return filter.isEmpty() || filter.contains(dbName);
    }

    // ========== Slow Queries ==========

    /**
     * Retrieves slow queries from pg_stat_statements for the specified instance.
     * <p>
     * Queries pg_stat_statements to identify queries with high execution times or call counts.
     * Note that pg_stat_statements extension must be installed and enabled for this method to return results.
     *
     * @param instanceName the name of the PostgreSQL instance to query
     * @param sortBy the column to sort by (e.g., "totalTime", "calls", "meanTime", "maxTime", "rows")
     * @param order the sort order ("asc" for ascending, "desc" for descending)
     * @return list of slow query records, limited to 100 results; empty list if pg_stat_statements is unavailable
     * @see #getSlowQueryById(String, String)
     */
    public List<SlowQuery> getSlowQueries(String instanceName, String sortBy, String order) {
        List<SlowQuery> queries = new ArrayList<>();
        String orderClause = getOrderClause(sortBy, order);

        String sql = """
            SELECT
                md5(query) as queryid,
                query,
                calls as total_calls,
                total_exec_time as total_time,
                mean_exec_time as mean_time,
                min_exec_time as min_time,
                max_exec_time as max_time,
                rows,
                'unknown' as user,
                'current' as database
            FROM pg_stat_statements
            WHERE query NOT LIKE '%pg_stat_statements%'
            ORDER BY """ + " " + orderClause + " LIMIT 100";

        try (Connection conn = getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                SlowQuery query = new SlowQuery();
                query.setQueryId(rs.getString("queryid"));
                query.setQuery(rs.getString("query"));
                query.setTotalCalls(rs.getLong("total_calls"));
                query.setTotalTime(rs.getDouble("total_time"));
                query.setMeanTime(rs.getDouble("mean_time"));
                query.setMinTime(rs.getDouble("min_time"));
                query.setMaxTime(rs.getDouble("max_time"));
                query.setRows(rs.getLong("rows"));
                query.setUser(rs.getString("user"));
                query.setDatabase(rs.getString("database"));
                queries.add(query);
            }
        } catch (SQLException e) {
            LOG.warnf("Could not query pg_stat_statements on %s: %s", instanceName, e.getMessage());
        }

        return queries;
    }

    /**
     * Retrieves slow queries from pg_stat_statements for the default instance.
     * <p>
     * This is a convenience method that delegates to {@link #getSlowQueries(String, String, String)}
     * using "default" as the instance name.
     *
     * @param sortBy the column to sort by (e.g., "totalTime", "calls", "meanTime", "maxTime", "rows")
     * @param order the sort order ("asc" for ascending, "desc" for descending)
     * @return list of slow query records, limited to 100 results
     * @see #getSlowQueries(String, String, String)
     */
    public List<SlowQuery> getSlowQueries(String sortBy, String order) {
        return getSlowQueries("default", sortBy, order);
    }

    /**
     * Constructs an ORDER BY clause for slow query sorting.
     *
     * @param sortBy the field to sort by; defaults to "totalTime" if null
     * @param order the sort direction ("asc" or "desc"); defaults to "DESC" if not "asc"
     * @return SQL ORDER BY clause fragment (e.g., "total_exec_time DESC")
     */
    private String getOrderClause(String sortBy, String order) {
        String column = switch (sortBy != null ? sortBy : "totalTime") {
            case "calls" -> "calls";
            case "meanTime" -> "mean_exec_time";
            case "totalTime" -> "total_exec_time";
            case "maxTime" -> "max_exec_time";
            case "rows" -> "rows";
            default -> "total_exec_time";
        };
        String direction = "asc".equalsIgnoreCase(order) ? "ASC" : "DESC";
        return column + " " + direction;
    }

    /**
     * Retrieves detailed information about a specific slow query by its query ID.
     * <p>
     * Returns comprehensive statistics including execution time details, buffer I/O metrics,
     * and temporary file usage from pg_stat_statements.
     *
     * @param instanceName the name of the PostgreSQL instance to query
     * @param queryId the MD5 hash of the query text used as identifier
     * @return the slow query details, or null if not found
     * @see #getSlowQueries(String, String, String)
     */
    public SlowQuery getSlowQueryById(String instanceName, String queryId) {
        String sql = """
            SELECT
                md5(query) as queryid,
                query,
                calls as total_calls,
                total_exec_time as total_time,
                mean_exec_time as mean_time,
                min_exec_time as min_time,
                max_exec_time as max_time,
                stddev_exec_time as stddev_time,
                rows,
                shared_blks_hit,
                shared_blks_read,
                shared_blks_written,
                temp_blks_read,
                temp_blks_written,
                'unknown' as user,
                'current' as database
            FROM pg_stat_statements
            WHERE md5(query) = ?
            LIMIT 1
            """;

        try (Connection conn = getDataSource(instanceName).getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, queryId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    SlowQuery query = new SlowQuery();
                    query.setQueryId(rs.getString("queryid"));
                    query.setQuery(rs.getString("query"));
                    query.setTotalCalls(rs.getLong("total_calls"));
                    query.setTotalTime(rs.getDouble("total_time"));
                    query.setMeanTime(rs.getDouble("mean_time"));
                    query.setMinTime(rs.getDouble("min_time"));
                    query.setMaxTime(rs.getDouble("max_time"));
                    query.setStddevTime(rs.getDouble("stddev_time"));
                    query.setRows(rs.getLong("rows"));
                    query.setSharedBlksHit(rs.getLong("shared_blks_hit"));
                    query.setSharedBlksRead(rs.getLong("shared_blks_read"));
                    query.setSharedBlksWritten(rs.getLong("shared_blks_written"));
                    query.setTempBlksRead(rs.getLong("temp_blks_read"));
                    query.setTempBlksWritten(rs.getLong("temp_blks_written"));
                    query.setUser(rs.getString("user"));
                    query.setDatabase(rs.getString("database"));
                    return query;
                }
            }
        } catch (SQLException e) {
            LOG.warnf("Could not query pg_stat_statements on %s: %s", instanceName, e.getMessage());
        }

        return null;
    }

    /**
     * Retrieves detailed information about a specific slow query by its query ID for the default instance.
     *
     * @param queryId the MD5 hash of the query text used as identifier
     * @return the slow query details, or null if not found
     * @see #getSlowQueryById(String, String)
     */
    public SlowQuery getSlowQueryById(String queryId) {
        return getSlowQueryById("default", queryId);
    }

    // ========== Activity ==========

    /**
     * Retrieves current activity from pg_stat_activity for the specified instance.
     * <p>
     * Returns information about all non-idle backend processes, excluding the current backend.
     * Includes query text, connection information, wait events, and blocking relationships.
     *
     * @param instanceName the name of the PostgreSQL instance to query
     * @return list of active backend processes, limited to 50 most recent by query start time
     * @throws RuntimeException if the query fails
     * @see #getBlockingTree(String)
     */
    public List<Activity> getCurrentActivity(String instanceName) {
        List<Activity> activities = new ArrayList<>();

        String sql = """
            SELECT
                pid,
                usename as user,
                datname as database,
                application_name,
                client_addr::text as client_addr,
                backend_start,
                xact_start,
                query_start,
                state_change,
                state,
                wait_event_type,
                wait_event,
                query,
                (SELECT pid FROM pg_stat_activity WHERE pid = ANY(pg_blocking_pids(pg_stat_activity.pid)) LIMIT 1) as blocking_pid
            FROM pg_stat_activity
            WHERE pid != pg_backend_pid()
              AND state != 'idle'
            ORDER BY query_start DESC NULLS LAST
            LIMIT 50
            """;

        try (Connection conn = getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Activity activity = new Activity();
                activity.setPid(rs.getInt("pid"));
                activity.setUser(rs.getString("user"));
                activity.setDatabase(rs.getString("database"));
                activity.setApplicationName(rs.getString("application_name"));
                activity.setClientAddr(rs.getString("client_addr"));
                activity.setBackendStart(rs.getTimestamp("backend_start") != null ?
                    rs.getTimestamp("backend_start").toLocalDateTime() : null);
                activity.setXactStart(rs.getTimestamp("xact_start") != null ?
                    rs.getTimestamp("xact_start").toLocalDateTime() : null);
                activity.setQueryStart(rs.getTimestamp("query_start") != null ?
                    rs.getTimestamp("query_start").toLocalDateTime() : null);
                activity.setStateChange(rs.getTimestamp("state_change") != null ?
                    rs.getTimestamp("state_change").toLocalDateTime() : null);
                activity.setState(rs.getString("state"));
                activity.setWaitEventType(rs.getString("wait_event_type"));
                activity.setWaitEvent(rs.getString("wait_event"));
                activity.setQuery(rs.getString("query"));
                int blockingPid = rs.getInt("blocking_pid");
                activity.setBlockingPid(rs.wasNull() ? null : blockingPid);
                activities.add(activity);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query current activity on " + instanceName, e);
        }

        return activities;
    }

    /**
     * Retrieves current activity from pg_stat_activity for the default instance.
     *
     * @return list of active backend processes, limited to 50 most recent
     * @throws RuntimeException if the query fails
     * @see #getCurrentActivity(String)
     */
    public List<Activity> getCurrentActivity() {
        return getCurrentActivity("default");
    }

    // ========== Cancel/Terminate Queries ==========

    /**
     * Cancels the current query running on a PostgreSQL backend process.
     * <p>
     * Uses {@code pg_cancel_backend()} which sends a SIGINT signal to cancel the query.
     * This is less aggressive than {@link #terminateQuery(String, int)} as it only cancels
     * the current query whilst leaving the connection open.
     *
     * @param instanceName the name of the PostgreSQL instance
     * @param pid the backend process ID to cancel
     * @return true if the signal was sent successfully, false otherwise
     * @see #terminateQuery(String, int)
     */
    public boolean cancelQuery(String instanceName, int pid) {
        String sql = "SELECT pg_cancel_backend(?)";
        try (Connection conn = getDataSource(instanceName).getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, pid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    boolean result = rs.getBoolean(1);
                    LOG.infof("Cancel query on %s pid %d: %s", instanceName, pid, result ? "success" : "failed");
                    return result;
                }
            }
        } catch (SQLException e) {
            LOG.errorf("Failed to cancel backend %d on %s: %s", pid, instanceName, e.getMessage());
        }
        return false;
    }

    /**
     * Terminates a PostgreSQL backend process, forcefully disconnecting the client.
     * <p>
     * Uses {@code pg_terminate_backend()} which sends a SIGTERM signal to terminate the entire
     * backend process. This is more aggressive than {@link #cancelQuery(String, int)} as it
     * closes the client connection. Use with caution.
     *
     * @param instanceName the name of the PostgreSQL instance
     * @param pid the backend process ID to terminate
     * @return true if the signal was sent successfully, false otherwise
     * @see #cancelQuery(String, int)
     */
    public boolean terminateQuery(String instanceName, int pid) {
        String sql = "SELECT pg_terminate_backend(?)";
        try (Connection conn = getDataSource(instanceName).getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, pid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    boolean result = rs.getBoolean(1);
                    LOG.infof("Terminate backend on %s pid %d: %s", instanceName, pid, result ? "success" : "failed");
                    return result;
                }
            }
        } catch (SQLException e) {
            LOG.errorf("Failed to terminate backend %d on %s: %s", pid, instanceName, e.getMessage());
        }
        return false;
    }

    // ========== Table Stats ==========

    /**
     * Retrieves table statistics from pg_stat_user_tables for the specified instance.
     * <p>
     * Returns statistics about user-defined tables, including scan counts, tuple operations,
     * and live/dead tuple counts. Excludes system catalogues.
     *
     * @param instanceName the name of the PostgreSQL instance to query
     * @return list of table statistics, ordered by total tuples (live + dead) descending, limited to 50
     * @throws RuntimeException if the query fails
     */
    public List<TableStats> getTableStats(String instanceName) {
        List<TableStats> stats = new ArrayList<>();

        String sql = """
            SELECT
                schemaname,
                relname as tablename,
                seq_scan,
                seq_tup_read,
                COALESCE(idx_scan, 0) as idx_scan,
                COALESCE(idx_tup_fetch, 0) as idx_tup_fetch,
                n_tup_ins,
                n_tup_upd,
                n_tup_del,
                n_live_tup,
                n_dead_tup
            FROM pg_stat_user_tables
            WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
            ORDER BY (n_live_tup + n_dead_tup) DESC
            LIMIT 50
            """;

        try (Connection conn = getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                TableStats stat = new TableStats();
                stat.setSchemaName(rs.getString("schemaname"));
                stat.setTableName(rs.getString("tablename"));
                stat.setSeqScan(rs.getLong("seq_scan"));
                stat.setSeqTupRead(rs.getLong("seq_tup_read"));
                stat.setIdxScan(rs.getLong("idx_scan"));
                stat.setIdxTupFetch(rs.getLong("idx_tup_fetch"));
                stat.setnTupIns(rs.getLong("n_tup_ins"));
                stat.setnTupUpd(rs.getLong("n_tup_upd"));
                stat.setnTupDel(rs.getLong("n_tup_del"));
                stat.setnLiveTup(rs.getLong("n_live_tup"));
                stat.setnDeadTup(rs.getLong("n_dead_tup"));
                stats.add(stat);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query table stats on " + instanceName, e);
        }

        return stats;
    }

    /**
     * Retrieves table statistics from pg_stat_user_tables for the default instance.
     *
     * @return list of table statistics, ordered by total tuples descending, limited to 50
     * @throws RuntimeException if the query fails
     * @see #getTableStats(String)
     */
    public List<TableStats> getTableStats() {
        return getTableStats("default");
    }

    // ========== Database Info ==========

    /**
     * Retrieves general database information and configuration for the specified instance.
     * <p>
     * Returns PostgreSQL version, current database and user, server encoding, start time,
     * and checks for the presence of the pg_stat_statements extension.
     *
     * @param instanceName the name of the PostgreSQL instance to query
     * @return database information object with metadata and extension status
     * @throws RuntimeException if the basic info query fails
     */
    public DatabaseInfo getDatabaseInfo(String instanceName) {
        DatabaseInfo info = new DatabaseInfo();

        String sql = """
            SELECT
                version() as postgres_version,
                current_database() as current_database,
                current_user as current_user,
                (SELECT setting FROM pg_settings WHERE name = 'server_encoding') as server_encoding,
                (SELECT pg_postmaster_start_time()::text) as server_start_time
            """;

        try (Connection conn = getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                info.setPostgresVersion(rs.getString("postgres_version"));
                info.setCurrentDatabase(rs.getString("current_database"));
                info.setCurrentUser(rs.getString("current_user"));
                info.setServerEncoding(rs.getString("server_encoding"));
                info.setServerStartTime(rs.getString("server_start_time"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query database info on " + instanceName, e);
        }

        // Check for pg_stat_statements extension
        String extSql = """
            SELECT extversion FROM pg_extension WHERE extname = 'pg_stat_statements'
            """;

        try (Connection conn = getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(extSql)) {

            if (rs.next()) {
                info.setPgStatStatementsEnabled(true);
                info.setPgStatStatementsVersion(rs.getString("extversion"));
            } else {
                info.setPgStatStatementsEnabled(false);
            }
        } catch (SQLException e) {
            info.setPgStatStatementsEnabled(false);
        }

        return info;
    }

    /**
     * Retrieves general database information and configuration for the default instance.
     *
     * @return database information object with metadata and extension status
     * @throws RuntimeException if the basic info query fails
     * @see #getDatabaseInfo(String)
     */
    public DatabaseInfo getDatabaseInfo() {
        return getDatabaseInfo("default");
    }

    // ========== Overview Stats ==========

    /**
     * Retrieves comprehensive overview statistics for the specified instance.
     * <p>
     * Returns a consolidated view including connection usage, active/blocked query counts,
     * PostgreSQL version, longest running query duration, cache hit ratio, database size,
     * and the top 10 largest tables and indexes.
     *
     * @param instanceName the name of the PostgreSQL instance to query
     * @return overview statistics aggregating multiple metrics
     * @throws RuntimeException if any critical query fails
     */
    public OverviewStats getOverviewStats(String instanceName) {
        OverviewStats stats = new OverviewStats();

        // Get connection counts and max connections
        String connectionsSql = """
            SELECT
                (SELECT count(*) FROM pg_stat_activity) as connections_used,
                (SELECT setting::int FROM pg_settings WHERE name = 'max_connections') as max_connections,
                (SELECT count(*) FROM pg_stat_activity WHERE state = 'active' AND pid != pg_backend_pid()) as active_queries,
                (SELECT count(*) FROM pg_stat_activity WHERE cardinality(pg_blocking_pids(pid)) > 0) as blocked_queries
            """;

        try (Connection conn = getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(connectionsSql)) {

            if (rs.next()) {
                stats.setConnectionsUsed(rs.getInt("connections_used"));
                stats.setConnectionsMax(rs.getInt("max_connections"));
                stats.setActiveQueries(rs.getInt("active_queries"));
                stats.setBlockedQueries(rs.getInt("blocked_queries"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query connection stats on " + instanceName, e);
        }

        // Get PostgreSQL version
        String versionSql = "SELECT version()";
        try (Connection conn = getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(versionSql)) {

            if (rs.next()) {
                String fullVersion = rs.getString(1);
                // Extract just the major version (e.g., "PostgreSQL 16.2")
                if (fullVersion != null && fullVersion.startsWith("PostgreSQL")) {
                    int endIdx = fullVersion.indexOf(" on ");
                    if (endIdx > 0) {
                        stats.setVersion(fullVersion.substring(0, endIdx));
                    } else {
                        stats.setVersion(fullVersion.split(",")[0]);
                    }
                } else {
                    stats.setVersion(fullVersion);
                }
            }
        } catch (SQLException e) {
            stats.setVersion("Unknown");
        }

        // Get longest running query duration
        String longestQuerySql = """
            SELECT
                COALESCE(
                    (SELECT age(now(), query_start)::text
                     FROM pg_stat_activity
                     WHERE state = 'active' AND pid != pg_backend_pid()
                     ORDER BY query_start ASC NULLS LAST
                     LIMIT 1),
                    'None'
                ) as longest_duration
            """;

        try (Connection conn = getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(longestQuerySql)) {

            if (rs.next()) {
                stats.setLongestQueryDuration(rs.getString("longest_duration"));
            }
        } catch (SQLException e) {
            stats.setLongestQueryDuration("Unknown");
        }

        // Get cache hit ratio
        String cacheSql = """
            SELECT
                CASE WHEN (blks_hit + blks_read) = 0 THEN 0
                     ELSE (blks_hit::float / (blks_hit + blks_read)) * 100
                END as cache_hit_ratio
            FROM pg_stat_database
            WHERE datname = current_database()
            """;

        try (Connection conn = getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(cacheSql)) {

            if (rs.next()) {
                stats.setCacheHitRatio(rs.getDouble("cache_hit_ratio"));
            }
        } catch (SQLException e) {
            stats.setCacheHitRatio(0);
        }

        // Get database size
        String sizeSql = """
            SELECT pg_size_pretty(pg_database_size(current_database())) as db_size
            """;

        try (Connection conn = getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sizeSql)) {

            if (rs.next()) {
                stats.setDatabaseSize(rs.getString("db_size"));
            }
        } catch (SQLException e) {
            stats.setDatabaseSize("Unknown");
        }

        // Get top 10 tables by size
        String tablesSql = """
            SELECT
                schemaname,
                relname as tablename,
                pg_size_pretty(pg_total_relation_size(schemaname || '.' || relname)) as size,
                pg_total_relation_size(schemaname || '.' || relname) as size_bytes
            FROM pg_stat_user_tables
            ORDER BY pg_total_relation_size(schemaname || '.' || relname) DESC
            LIMIT 10
            """;

        List<OverviewStats.TableSize> tables = new ArrayList<>();
        try (Connection conn = getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(tablesSql)) {

            while (rs.next()) {
                OverviewStats.TableSize table = new OverviewStats.TableSize();
                table.setSchemaName(rs.getString("schemaname"));
                table.setTableName(rs.getString("tablename"));
                table.setSize(rs.getString("size"));
                table.setSizeBytes(rs.getLong("size_bytes"));
                tables.add(table);
            }
        } catch (SQLException e) {
            // Ignore - empty list is fine
        }
        stats.setTopTablesBySize(tables);

        // Get top 10 indexes by size
        String indexesSql = """
            SELECT
                schemaname,
                indexrelname as indexname,
                relname as tablename,
                pg_size_pretty(pg_relation_size(indexrelid)) as size,
                pg_relation_size(indexrelid) as size_bytes
            FROM pg_stat_user_indexes
            ORDER BY pg_relation_size(indexrelid) DESC
            LIMIT 10
            """;

        List<OverviewStats.IndexSize> indexes = new ArrayList<>();
        try (Connection conn = getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(indexesSql)) {

            while (rs.next()) {
                OverviewStats.IndexSize index = new OverviewStats.IndexSize();
                index.setSchemaName(rs.getString("schemaname"));
                index.setIndexName(rs.getString("indexname"));
                index.setTableName(rs.getString("tablename"));
                index.setSize(rs.getString("size"));
                index.setSizeBytes(rs.getLong("size_bytes"));
                indexes.add(index);
            }
        } catch (SQLException e) {
            // Ignore - empty list is fine
        }
        stats.setTopIndexesBySize(indexes);

        return stats;
    }

    /**
     * Retrieves comprehensive overview statistics for the default instance.
     *
     * @return overview statistics aggregating multiple metrics
     * @throws RuntimeException if any critical query fails
     * @see #getOverviewStats(String)
     */
    public OverviewStats getOverviewStats() {
        return getOverviewStats("default");
    }

    // ========== Blocking Tree ==========

    /**
     * Retrieves the lock blocking hierarchy for the specified instance.
     * <p>
     * Identifies blocked queries and their blocking processes by analysing pg_locks and pg_stat_activity.
     * Returns pairs of blocked and blocker processes with query details and lock modes.
     *
     * @param instanceName the name of the PostgreSQL instance to query
     * @return list of blocking relationships showing which queries are blocking others; empty list if query fails
     * @see #getLockInfo(String)
     */
    public List<BlockingTree> getBlockingTree(String instanceName) {
        List<BlockingTree> tree = new ArrayList<>();

        String sql = """
            SELECT
                blocked.pid as blocked_pid,
                blocked.usename as blocked_user,
                blocked.query as blocked_query,
                blocked.state as blocked_state,
                age(now(), blocked.query_start)::text as blocked_duration,
                blocker.pid as blocker_pid,
                blocker.usename as blocker_user,
                blocker.query as blocker_query,
                blocker.state as blocker_state,
                blocked_locks.mode as lock_mode
            FROM pg_stat_activity blocked
            JOIN pg_locks blocked_locks ON blocked.pid = blocked_locks.pid AND NOT blocked_locks.granted
            JOIN pg_locks blocker_locks ON blocker_locks.granted
                AND blocked_locks.locktype = blocker_locks.locktype
                AND blocked_locks.database IS NOT DISTINCT FROM blocker_locks.database
                AND blocked_locks.relation IS NOT DISTINCT FROM blocker_locks.relation
                AND blocked_locks.page IS NOT DISTINCT FROM blocker_locks.page
                AND blocked_locks.tuple IS NOT DISTINCT FROM blocker_locks.tuple
                AND blocked_locks.virtualxid IS NOT DISTINCT FROM blocker_locks.virtualxid
                AND blocked_locks.transactionid IS NOT DISTINCT FROM blocker_locks.transactionid
                AND blocked_locks.classid IS NOT DISTINCT FROM blocker_locks.classid
                AND blocked_locks.objid IS NOT DISTINCT FROM blocker_locks.objid
                AND blocked_locks.objsubid IS NOT DISTINCT FROM blocker_locks.objsubid
                AND blocked_locks.pid != blocker_locks.pid
            JOIN pg_stat_activity blocker ON blocker.pid = blocker_locks.pid
            ORDER BY blocked.query_start
            """;

        try (Connection conn = getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                BlockingTree item = new BlockingTree();
                item.setBlockedPid(rs.getInt("blocked_pid"));
                item.setBlockedUser(rs.getString("blocked_user"));
                item.setBlockedQuery(rs.getString("blocked_query"));
                item.setBlockedState(rs.getString("blocked_state"));
                item.setBlockedDuration(rs.getString("blocked_duration"));
                item.setBlockerPid(rs.getInt("blocker_pid"));
                item.setBlockerUser(rs.getString("blocker_user"));
                item.setBlockerQuery(rs.getString("blocker_query"));
                item.setBlockerState(rs.getString("blocker_state"));
                item.setLockMode(rs.getString("lock_mode"));
                tree.add(item);
            }
        } catch (SQLException e) {
            LOG.warnf("Could not query blocking tree on %s: %s", instanceName, e.getMessage());
        }

        return tree;
    }

    /**
     * Retrieves the lock blocking hierarchy for the default instance.
     *
     * @return list of blocking relationships; empty list if query fails
     * @see #getBlockingTree(String)
     */
    public List<BlockingTree> getBlockingTree() {
        return getBlockingTree("default");
    }

    // ========== Lock Info ==========

    /**
     * Retrieves current lock information from pg_locks for the specified instance.
     * <p>
     * Returns details about all locks held or awaited by backend processes, joined with
     * activity information. Excludes the current backend.
     *
     * @param instanceName the name of the PostgreSQL instance to query
     * @return list of lock information including granted/waiting status, lock type, and relation details; limited to 100 records
     * @throws RuntimeException if the query fails
     * @see #getBlockingTree(String)
     */
    public List<LockInfo> getLockInfo(String instanceName) {
        List<LockInfo> locks = new ArrayList<>();

        String sql = """
            SELECT
                l.pid,
                l.locktype as lock_type,
                d.datname as database,
                COALESCE(c.relname, l.locktype || ':' || COALESCE(l.transactionid::text, l.virtualxid::text, '')) as relation,
                l.mode,
                l.granted,
                a.query,
                a.state,
                a.wait_event_type,
                a.wait_event,
                a.usename as user
            FROM pg_locks l
            LEFT JOIN pg_database d ON l.database = d.oid
            LEFT JOIN pg_class c ON l.relation = c.oid
            JOIN pg_stat_activity a ON l.pid = a.pid
            WHERE l.pid != pg_backend_pid()
            ORDER BY l.granted, l.pid
            LIMIT 100
            """;

        try (Connection conn = getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                LockInfo lock = new LockInfo();
                lock.setPid(rs.getInt("pid"));
                lock.setLockType(rs.getString("lock_type"));
                lock.setDatabase(rs.getString("database"));
                lock.setRelation(rs.getString("relation"));
                lock.setMode(rs.getString("mode"));
                lock.setGranted(rs.getBoolean("granted"));
                lock.setQuery(rs.getString("query"));
                lock.setState(rs.getString("state"));
                lock.setWaitEventType(rs.getString("wait_event_type"));
                lock.setWaitEvent(rs.getString("wait_event"));
                lock.setUser(rs.getString("user"));
                locks.add(lock);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query lock info on " + instanceName, e);
        }

        return locks;
    }

    /**
     * Retrieves current lock information from pg_locks for the default instance.
     *
     * @return list of lock information; limited to 100 records
     * @throws RuntimeException if the query fails
     * @see #getLockInfo(String)
     */
    public List<LockInfo> getLockInfo() {
        return getLockInfo("default");
    }

    // ========== Database List ==========

    /**
     * Retrieves the list of non-template databases for the specified instance.
     * <p>
     * Returns database names from pg_database, filtered by the configured database filter
     * and excluding template databases.
     *
     * @param instanceName the name of the PostgreSQL instance to query
     * @return list of database names, filtered according to configured filters
     * @throws RuntimeException if the query fails
     * @see #shouldIncludeDatabase(String)
     */
    public List<String> getDatabaseList(String instanceName) {
        List<String> databases = new ArrayList<>();

        String sql = """
            SELECT datname
            FROM pg_database
            WHERE datistemplate = false
            ORDER BY datname
            """;

        try (Connection conn = getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String dbName = rs.getString("datname");
                if (shouldIncludeDatabase(dbName)) {
                    databases.add(dbName);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query database list on " + instanceName, e);
        }

        return databases;
    }

    /**
     * Retrieves the list of non-template databases for the default instance.
     *
     * @return list of database names, filtered according to configured filters
     * @throws RuntimeException if the query fails
     * @see #getDatabaseList(String)
     */
    public List<String> getDatabaseList() {
        return getDatabaseList("default");
    }

    // ========== Database Metrics ==========

    /**
     * Retrieves comprehensive metrics for all databases on the specified instance.
     * <p>
     * Returns statistics from pg_stat_database including transaction counts, block I/O,
     * tuple operations, conflicts, deadlocks, session statistics, and database sizes.
     * Excludes template databases and applies configured database filters.
     *
     * @param instanceName the name of the PostgreSQL instance to query
     * @return list of database metrics for all monitored databases
     * @throws RuntimeException if the query fails
     * @see #getDatabaseMetrics(String, String)
     */
    public List<DatabaseMetrics> getAllDatabaseMetrics(String instanceName) {
        List<DatabaseMetrics> metrics = new ArrayList<>();

        String sql = """
            SELECT
                d.datid,
                d.datname,
                d.numbackends,
                d.xact_commit,
                d.xact_rollback,
                d.blks_read,
                d.blks_hit,
                d.tup_returned,
                d.tup_fetched,
                d.tup_inserted,
                d.tup_updated,
                d.tup_deleted,
                d.conflicts,
                d.temp_files,
                d.temp_bytes,
                d.deadlocks,
                COALESCE(d.blk_read_time, 0) as blk_read_time,
                COALESCE(d.blk_write_time, 0) as blk_write_time,
                COALESCE(d.session_time, 0) as session_time,
                COALESCE(d.active_time, 0) as active_time,
                COALESCE(d.idle_in_transaction_time, 0) as idle_in_transaction_time,
                COALESCE(d.sessions, 0) as sessions,
                COALESCE(d.sessions_abandoned, 0) as sessions_abandoned,
                COALESCE(d.sessions_fatal, 0) as sessions_fatal,
                COALESCE(d.sessions_killed, 0) as sessions_killed,
                d.stats_reset::text as stats_reset,
                pg_size_pretty(pg_database_size(d.datname)) as database_size,
                pg_database_size(d.datname) as database_size_bytes,
                EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_stat_statements') as pg_stat_statements_enabled
            FROM pg_stat_database d
            JOIN pg_database db ON d.datid = db.oid
            WHERE db.datistemplate = false
            ORDER BY d.datname
            """;

        try (Connection conn = getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                DatabaseMetrics m = mapDatabaseMetrics(rs);
                if (shouldIncludeDatabase(m.getDatname())) {
                    metrics.add(m);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query database metrics on " + instanceName, e);
        }

        return metrics;
    }

    /**
     * Retrieves comprehensive metrics for all databases on the default instance.
     *
     * @return list of database metrics for all monitored databases
     * @throws RuntimeException if the query fails
     * @see #getAllDatabaseMetrics(String)
     */
    public List<DatabaseMetrics> getAllDatabaseMetrics() {
        return getAllDatabaseMetrics("default");
    }

    /**
     * Retrieves comprehensive metrics for a specific database on the specified instance.
     * <p>
     * Returns statistics from pg_stat_database for a single database, including transaction counts,
     * block I/O, tuple operations, conflicts, deadlocks, session statistics, and database size.
     *
     * @param instanceName the name of the PostgreSQL instance to query
     * @param databaseName the name of the database to retrieve metrics for
     * @return database metrics, or null if the database is not found
     * @throws RuntimeException if the query fails
     * @see #getAllDatabaseMetrics(String)
     */
    public DatabaseMetrics getDatabaseMetrics(String instanceName, String databaseName) {
        String sql = """
            SELECT
                d.datid,
                d.datname,
                d.numbackends,
                d.xact_commit,
                d.xact_rollback,
                d.blks_read,
                d.blks_hit,
                d.tup_returned,
                d.tup_fetched,
                d.tup_inserted,
                d.tup_updated,
                d.tup_deleted,
                d.conflicts,
                d.temp_files,
                d.temp_bytes,
                d.deadlocks,
                COALESCE(d.blk_read_time, 0) as blk_read_time,
                COALESCE(d.blk_write_time, 0) as blk_write_time,
                COALESCE(d.session_time, 0) as session_time,
                COALESCE(d.active_time, 0) as active_time,
                COALESCE(d.idle_in_transaction_time, 0) as idle_in_transaction_time,
                COALESCE(d.sessions, 0) as sessions,
                COALESCE(d.sessions_abandoned, 0) as sessions_abandoned,
                COALESCE(d.sessions_fatal, 0) as sessions_fatal,
                COALESCE(d.sessions_killed, 0) as sessions_killed,
                d.stats_reset::text as stats_reset,
                pg_size_pretty(pg_database_size(d.datname)) as database_size,
                pg_database_size(d.datname) as database_size_bytes,
                EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_stat_statements') as pg_stat_statements_enabled
            FROM pg_stat_database d
            WHERE d.datname = ?
            """;

        try (Connection conn = getDataSource(instanceName).getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, databaseName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapDatabaseMetrics(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query database metrics for " + databaseName + " on " + instanceName, e);
        }

        return null;
    }

    /**
     * Retrieves comprehensive metrics for a specific database on the default instance.
     *
     * @param databaseName the name of the database to retrieve metrics for
     * @return database metrics, or null if the database is not found
     * @throws RuntimeException if the query fails
     * @see #getDatabaseMetrics(String, String)
     */
    public DatabaseMetrics getDatabaseMetrics(String databaseName) {
        return getDatabaseMetrics("default", databaseName);
    }

    /**
     * Maps a ResultSet row to a DatabaseMetrics object.
     * <p>
     * This is a utility method used internally to convert database query results
     * into strongly-typed model objects.
     *
     * @param rs the ResultSet positioned at the row to map
     * @return populated DatabaseMetrics object
     * @throws SQLException if there is an error reading from the ResultSet
     */
    private DatabaseMetrics mapDatabaseMetrics(ResultSet rs) throws SQLException {
        DatabaseMetrics m = new DatabaseMetrics();
        m.setDatid(rs.getLong("datid"));
        m.setDatname(rs.getString("datname"));
        m.setNumBackends(rs.getInt("numbackends"));
        m.setXactCommit(rs.getLong("xact_commit"));
        m.setXactRollback(rs.getLong("xact_rollback"));
        m.setBlksRead(rs.getLong("blks_read"));
        m.setBlksHit(rs.getLong("blks_hit"));
        m.setTupReturned(rs.getLong("tup_returned"));
        m.setTupFetched(rs.getLong("tup_fetched"));
        m.setTupInserted(rs.getLong("tup_inserted"));
        m.setTupUpdated(rs.getLong("tup_updated"));
        m.setTupDeleted(rs.getLong("tup_deleted"));
        m.setConflicts(rs.getLong("conflicts"));
        m.setTempFiles(rs.getLong("temp_files"));
        m.setTempBytes(rs.getLong("temp_bytes"));
        m.setDeadlocks(rs.getLong("deadlocks"));
        m.setBlkReadTime(rs.getDouble("blk_read_time"));
        m.setBlkWriteTime(rs.getDouble("blk_write_time"));
        m.setSessionTime(rs.getDouble("session_time"));
        m.setActiveTime(rs.getDouble("active_time"));
        m.setIdleInTransactionTime(rs.getDouble("idle_in_transaction_time"));
        m.setSessions(rs.getLong("sessions"));
        m.setSessionsAbandoned(rs.getLong("sessions_abandoned"));
        m.setSessionsFatal(rs.getLong("sessions_fatal"));
        m.setSessionsKilled(rs.getLong("sessions_killed"));
        m.setStatsReset(rs.getString("stats_reset"));
        m.setDatabaseSize(rs.getString("database_size"));
        m.setDatabaseSizeBytes(rs.getLong("database_size_bytes"));
        m.setPgStatStatementsEnabled(rs.getBoolean("pg_stat_statements_enabled"));
        return m;
    }

    // ========== Wait Events ==========

    /**
     * Gets a summary of current wait events from pg_stat_activity.
     *
     * @param instanceName the instance to query
     * @return list of wait event summaries
     */
    public List<WaitEventSummary> getWaitEventSummary(String instanceName) {
        List<WaitEventSummary> summaries = new ArrayList<>();

        String sql = """
            SELECT
                wait_event_type,
                wait_event,
                COUNT(*) as session_count
            FROM pg_stat_activity
            WHERE pid != pg_backend_pid()
            GROUP BY wait_event_type, wait_event
            ORDER BY
                CASE WHEN wait_event_type IS NULL THEN 0 ELSE 1 END,
                wait_event_type,
                session_count DESC
            """;

        try (Connection conn = getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                WaitEventSummary summary = new WaitEventSummary();
                summary.setWaitEventType(rs.getString("wait_event_type"));
                summary.setWaitEvent(rs.getString("wait_event"));
                summary.setSessionCount(rs.getInt("session_count"));
                summaries.add(summary);
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to get wait event summary for %s: %s", instanceName, e.getMessage());
        }

        return summaries;
    }

    /**
     * Retrieves a summary of current wait events from pg_stat_activity for the default instance.
     *
     * @return list of wait event summaries with session counts
     * @see #getWaitEventSummary(String)
     */
    public List<WaitEventSummary> getWaitEventSummary() {
        return getWaitEventSummary("default");
    }

    /**
     * Retrieves wait event totals grouped by wait_event_type for the specified instance.
     * <p>
     * Aggregates sessions from pg_stat_activity by wait_event_type to provide a high-level
     * view of what types of events backends are waiting on.
     *
     * @param instanceName the name of the PostgreSQL instance to query
     * @return list of wait event type summaries with session counts, sorted by session count descending
     */
    public List<WaitEventSummary> getWaitEventTypeSummary(String instanceName) {
        List<WaitEventSummary> summaries = new ArrayList<>();

        String sql = """
            SELECT
                wait_event_type,
                COUNT(*) as session_count
            FROM pg_stat_activity
            WHERE pid != pg_backend_pid()
            GROUP BY wait_event_type
            ORDER BY
                CASE WHEN wait_event_type IS NULL THEN 0 ELSE 1 END,
                session_count DESC
            """;

        try (Connection conn = getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                WaitEventSummary summary = new WaitEventSummary();
                summary.setWaitEventType(rs.getString("wait_event_type"));
                summary.setSessionCount(rs.getInt("session_count"));
                summaries.add(summary);
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to get wait event type summary for %s: %s", instanceName, e.getMessage());
        }

        return summaries;
    }

    /**
     * Retrieves wait event totals grouped by wait_event_type for the default instance.
     *
     * @return list of wait event type summaries with session counts
     * @see #getWaitEventTypeSummary(String)
     */
    public List<WaitEventSummary> getWaitEventTypeSummary() {
        return getWaitEventTypeSummary("default");
    }

    // =============================================
    // EXPLAIN Plan Methods
    // =============================================

    /**
     * Generates an EXPLAIN plan for a query on the specified instance.
     * <p>
     * Only SELECT, WITH (CTE), and VALUES queries are allowed for safety reasons.
     * The query is NOT executed unless {@code analyse} is true, in which case EXPLAIN ANALYZE
     * runs the query and provides actual execution statistics.
     * <p>
     * Note: Using EXPLAIN ANALYZE will execute the query, which may modify data if DDL/DML
     * is embedded in functions or triggers. This method blocks such statements at the top level.
     *
     * @param instanceName the name of the PostgreSQL instance to query
     * @param query the SQL query to explain (must be SELECT, WITH, or VALUES)
     * @param analyse if true, uses EXPLAIN ANALYZE which actually executes the query
     * @param buffers if true, includes buffer usage information in the plan output
     * @return the explain plan result containing plan text or error message
     * @see #isExplainSafe(String)
     */
    public ExplainPlan explainQuery(String instanceName, String query, boolean analyse, boolean buffers) {
        ExplainPlan plan = new ExplainPlan();
        plan.setQuery(query);
        plan.setAnalyse(analyse);
        plan.setBuffers(buffers);

        // Validate the query is safe to explain
        String trimmedQuery = query.trim().toUpperCase();
        if (!isExplainSafe(trimmedQuery)) {
            plan.setError("Only SELECT, WITH, and VALUES queries can be explained for safety reasons.");
            return plan;
        }

        // Build the EXPLAIN command
        StringBuilder explainCmd = new StringBuilder("EXPLAIN ");
        if (analyse || buffers) {
            explainCmd.append("(");
            if (analyse) {
                explainCmd.append("ANALYZE");
            }
            if (buffers) {
                if (analyse) explainCmd.append(", ");
                explainCmd.append("BUFFERS");
            }
            explainCmd.append(") ");
        }
        explainCmd.append(query);

        StringBuilder planText = new StringBuilder();

        try (Connection conn = getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(explainCmd.toString())) {

            while (rs.next()) {
                String line = rs.getString(1);
                plan.addPlanLine(line);
                if (planText.length() > 0) {
                    planText.append("\n");
                }
                planText.append(line);
            }
            plan.setPlanText(planText.toString());

        } catch (SQLException e) {
            LOG.warnf("Failed to explain query for %s: %s", instanceName, e.getMessage());
            plan.setError("Failed to generate explain plan: " + e.getMessage());
        }

        return plan;
    }

    /**
     * Validates whether a query is safe to execute with EXPLAIN.
     * <p>
     * Only allows SELECT, WITH (CTE), and VALUES statements to prevent accidental
     * execution of DDL or DML operations.
     *
     * @param upperQuery the SQL query in uppercase with leading whitespace removed
     * @return true if the query starts with SELECT, WITH, or VALUES; false otherwise
     */
    private boolean isExplainSafe(String upperQuery) {
        // Remove leading whitespace and check first keyword
        String cleaned = upperQuery.replaceAll("^\\s+", "");

        // Allow SELECT, WITH, and VALUES
        if (cleaned.startsWith("SELECT") ||
            cleaned.startsWith("WITH") ||
            cleaned.startsWith("VALUES")) {
            return true;
        }

        // Check for common DML that we should block
        if (cleaned.startsWith("INSERT") ||
            cleaned.startsWith("UPDATE") ||
            cleaned.startsWith("DELETE") ||
            cleaned.startsWith("DROP") ||
            cleaned.startsWith("CREATE") ||
            cleaned.startsWith("ALTER") ||
            cleaned.startsWith("TRUNCATE")) {
            return false;
        }

        // Default to false for safety
        return false;
    }

    /**
     * Generates an EXPLAIN plan for a query on the default instance.
     *
     * @param query the SQL query to explain (must be SELECT, WITH, or VALUES)
     * @param analyse if true, uses EXPLAIN ANALYZE which actually executes the query
     * @param buffers if true, includes buffer usage information in the plan output
     * @return the explain plan result containing plan text or error message
     * @see #explainQuery(String, String, boolean, boolean)
     */
    public ExplainPlan explainQuery(String query, boolean analyse, boolean buffers) {
        return explainQuery("default", query, analyse, buffers);
    }
}
