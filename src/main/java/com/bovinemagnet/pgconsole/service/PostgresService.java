package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.Activity;
import com.bovinemagnet.pgconsole.model.BlockingTree;
import com.bovinemagnet.pgconsole.model.DatabaseInfo;
import com.bovinemagnet.pgconsole.model.DatabaseMetrics;
import com.bovinemagnet.pgconsole.model.LockInfo;
import com.bovinemagnet.pgconsole.model.OverviewStats;
import com.bovinemagnet.pgconsole.model.SlowQuery;
import com.bovinemagnet.pgconsole.model.TableStats;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class PostgresService {

    @Inject
    DataSource dataSource;

    @ConfigProperty(name = "pg-console.databases")
    Optional<String> databaseFilter;

    /**
     * Returns the set of database names to monitor.
     * If empty, all non-template databases are shown.
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
     * Returns true if the given database should be included based on the filter.
     */
    private boolean shouldIncludeDatabase(String dbName) {
        Set<String> filter = getDatabaseFilterSet();
        return filter.isEmpty() || filter.contains(dbName);
    }

    public List<SlowQuery> getSlowQueries(String sortBy, String order) {
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

        try (Connection conn = dataSource.getConnection();
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
            // If pg_stat_statements is not available, return empty list
            System.err.println("Warning: Could not query pg_stat_statements: " + e.getMessage());
        }
        
        return queries;
    }

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

    public List<Activity> getCurrentActivity() {
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

        try (Connection conn = dataSource.getConnection();
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
            throw new RuntimeException("Failed to query current activity", e);
        }
        
        return activities;
    }

    public List<TableStats> getTableStats() {
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

        try (Connection conn = dataSource.getConnection();
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
            throw new RuntimeException("Failed to query table stats", e);
        }

        return stats;
    }

    public DatabaseInfo getDatabaseInfo() {
        DatabaseInfo info = new DatabaseInfo();

        String sql = """
            SELECT
                version() as postgres_version,
                current_database() as current_database,
                current_user as current_user,
                (SELECT setting FROM pg_settings WHERE name = 'server_encoding') as server_encoding,
                (SELECT pg_postmaster_start_time()::text) as server_start_time
            """;

        try (Connection conn = dataSource.getConnection();
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
            throw new RuntimeException("Failed to query database info", e);
        }

        // Check for pg_stat_statements extension
        String extSql = """
            SELECT extversion FROM pg_extension WHERE extname = 'pg_stat_statements'
            """;

        try (Connection conn = dataSource.getConnection();
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

    public OverviewStats getOverviewStats() {
        OverviewStats stats = new OverviewStats();

        // Get connection counts and max connections
        String connectionsSql = """
            SELECT
                (SELECT count(*) FROM pg_stat_activity) as connections_used,
                (SELECT setting::int FROM pg_settings WHERE name = 'max_connections') as max_connections,
                (SELECT count(*) FROM pg_stat_activity WHERE state = 'active' AND pid != pg_backend_pid()) as active_queries,
                (SELECT count(*) FROM pg_stat_activity WHERE cardinality(pg_blocking_pids(pid)) > 0) as blocked_queries
            """;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(connectionsSql)) {

            if (rs.next()) {
                stats.setConnectionsUsed(rs.getInt("connections_used"));
                stats.setConnectionsMax(rs.getInt("max_connections"));
                stats.setActiveQueries(rs.getInt("active_queries"));
                stats.setBlockedQueries(rs.getInt("blocked_queries"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query connection stats", e);
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

        try (Connection conn = dataSource.getConnection();
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

        try (Connection conn = dataSource.getConnection();
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

        try (Connection conn = dataSource.getConnection();
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
        try (Connection conn = dataSource.getConnection();
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
        try (Connection conn = dataSource.getConnection();
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

    public List<BlockingTree> getBlockingTree() {
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

        try (Connection conn = dataSource.getConnection();
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
            // Return empty list on error
            System.err.println("Warning: Could not query blocking tree: " + e.getMessage());
        }

        return tree;
    }

    public List<LockInfo> getLockInfo() {
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

        try (Connection conn = dataSource.getConnection();
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
            throw new RuntimeException("Failed to query lock info", e);
        }

        return locks;
    }

    public SlowQuery getSlowQueryById(String queryId) {
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

        try (Connection conn = dataSource.getConnection();
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
            System.err.println("Warning: Could not query pg_stat_statements: " + e.getMessage());
        }

        return null;
    }

    public List<String> getDatabaseList() {
        List<String> databases = new ArrayList<>();

        String sql = """
            SELECT datname
            FROM pg_database
            WHERE datistemplate = false
            ORDER BY datname
            """;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String dbName = rs.getString("datname");
                if (shouldIncludeDatabase(dbName)) {
                    databases.add(dbName);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query database list", e);
        }

        return databases;
    }

    public List<DatabaseMetrics> getAllDatabaseMetrics() {
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

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                DatabaseMetrics m = mapDatabaseMetrics(rs);
                if (shouldIncludeDatabase(m.getDatname())) {
                    metrics.add(m);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query database metrics", e);
        }

        return metrics;
    }

    public DatabaseMetrics getDatabaseMetrics(String databaseName) {
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

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, databaseName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapDatabaseMetrics(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query database metrics for " + databaseName, e);
        }

        return null;
    }

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
}
