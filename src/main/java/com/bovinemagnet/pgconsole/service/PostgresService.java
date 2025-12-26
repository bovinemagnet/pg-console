package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.Activity;
import com.bovinemagnet.pgconsole.model.SlowQuery;
import com.bovinemagnet.pgconsole.model.TableStats;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class PostgresService {

    @Inject
    DataSource dataSource;

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
            ORDER BY """ + orderClause + """
            LIMIT 100
            """;

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
}
