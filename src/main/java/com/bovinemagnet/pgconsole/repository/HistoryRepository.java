package com.bovinemagnet.pgconsole.repository;

import com.bovinemagnet.pgconsole.model.DatabaseMetricsHistory;
import com.bovinemagnet.pgconsole.model.QueryMetricsHistory;
import com.bovinemagnet.pgconsole.model.SystemMetricsHistory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for storing and retrieving historical metrics.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class HistoryRepository {

    @Inject
    DataSource dataSource;

    /**
     * Saves a system metrics snapshot.
     */
    public void saveSystemMetrics(SystemMetricsHistory metrics) {
        String sql = """
            INSERT INTO pgconsole.system_metrics_history (
                sampled_at, total_connections, max_connections, active_queries,
                idle_connections, idle_in_transaction, blocked_queries,
                longest_query_seconds, longest_transaction_seconds,
                cache_hit_ratio, total_database_size_bytes
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.from(Instant.now()));
            stmt.setInt(2, metrics.getTotalConnections());
            stmt.setInt(3, metrics.getMaxConnections());
            stmt.setInt(4, metrics.getActiveQueries());
            stmt.setInt(5, metrics.getIdleConnections());
            stmt.setInt(6, metrics.getIdleInTransaction());
            stmt.setInt(7, metrics.getBlockedQueries());
            stmt.setObject(8, metrics.getLongestQuerySeconds());
            stmt.setObject(9, metrics.getLongestTransactionSeconds());
            stmt.setObject(10, metrics.getCacheHitRatio());
            stmt.setObject(11, metrics.getTotalDatabaseSizeBytes());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save system metrics", e);
        }
    }

    /**
     * Saves a query metrics snapshot.
     */
    public void saveQueryMetrics(QueryMetricsHistory metrics) {
        String sql = """
            INSERT INTO pgconsole.query_metrics_history (
                sampled_at, query_id, query_text, total_calls, total_time_ms,
                total_rows, mean_time_ms, min_time_ms, max_time_ms, stddev_time_ms,
                shared_blks_hit, shared_blks_read, temp_blks_written
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.from(Instant.now()));
            stmt.setString(2, metrics.getQueryId());
            stmt.setString(3, metrics.getQueryText());
            stmt.setLong(4, metrics.getTotalCalls());
            stmt.setDouble(5, metrics.getTotalTimeMs());
            stmt.setLong(6, metrics.getTotalRows());
            stmt.setDouble(7, metrics.getMeanTimeMs());
            stmt.setObject(8, metrics.getMinTimeMs());
            stmt.setObject(9, metrics.getMaxTimeMs());
            stmt.setObject(10, metrics.getStddevTimeMs());
            stmt.setObject(11, metrics.getSharedBlksHit());
            stmt.setObject(12, metrics.getSharedBlksRead());
            stmt.setObject(13, metrics.getTempBlksWritten());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save query metrics", e);
        }
    }

    /**
     * Saves a database metrics snapshot.
     */
    public void saveDatabaseMetrics(DatabaseMetricsHistory metrics) {
        String sql = """
            INSERT INTO pgconsole.database_metrics_history (
                sampled_at, database_name, num_backends, xact_commit, xact_rollback,
                blks_hit, blks_read, cache_hit_ratio, tup_returned, tup_fetched,
                tup_inserted, tup_updated, tup_deleted, deadlocks, conflicts,
                temp_files, temp_bytes, database_size_bytes
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.from(Instant.now()));
            stmt.setString(2, metrics.getDatabaseName());
            stmt.setInt(3, metrics.getNumBackends());
            stmt.setLong(4, metrics.getXactCommit());
            stmt.setLong(5, metrics.getXactRollback());
            stmt.setLong(6, metrics.getBlksHit());
            stmt.setLong(7, metrics.getBlksRead());
            stmt.setObject(8, metrics.getCacheHitRatio());
            stmt.setObject(9, metrics.getTupReturned());
            stmt.setObject(10, metrics.getTupFetched());
            stmt.setObject(11, metrics.getTupInserted());
            stmt.setObject(12, metrics.getTupUpdated());
            stmt.setObject(13, metrics.getTupDeleted());
            stmt.setObject(14, metrics.getDeadlocks());
            stmt.setObject(15, metrics.getConflicts());
            stmt.setObject(16, metrics.getTempFiles());
            stmt.setObject(17, metrics.getTempBytes());
            stmt.setObject(18, metrics.getDatabaseSizeBytes());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save database metrics", e);
        }
    }

    /**
     * Gets system metrics history for the last N hours.
     */
    public List<SystemMetricsHistory> getSystemMetricsHistory(int hours) {
        List<SystemMetricsHistory> history = new ArrayList<>();
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);

        String sql = """
            SELECT id, sampled_at, total_connections, max_connections, active_queries,
                   idle_connections, idle_in_transaction, blocked_queries,
                   longest_query_seconds, longest_transaction_seconds,
                   cache_hit_ratio, total_database_size_bytes
            FROM pgconsole.system_metrics_history
            WHERE sampled_at >= ?
            ORDER BY sampled_at ASC
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.from(since));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    SystemMetricsHistory m = new SystemMetricsHistory();
                    m.setId(rs.getLong("id"));
                    m.setSampledAt(rs.getTimestamp("sampled_at").toInstant());
                    m.setTotalConnections(rs.getInt("total_connections"));
                    m.setMaxConnections(rs.getInt("max_connections"));
                    m.setActiveQueries(rs.getInt("active_queries"));
                    m.setIdleConnections(rs.getInt("idle_connections"));
                    m.setIdleInTransaction(rs.getInt("idle_in_transaction"));
                    m.setBlockedQueries(rs.getInt("blocked_queries"));
                    m.setLongestQuerySeconds(rs.getObject("longest_query_seconds", Double.class));
                    m.setLongestTransactionSeconds(rs.getObject("longest_transaction_seconds", Double.class));
                    m.setCacheHitRatio(rs.getObject("cache_hit_ratio", Double.class));
                    m.setTotalDatabaseSizeBytes(rs.getObject("total_database_size_bytes", Long.class));
                    history.add(m);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get system metrics history", e);
        }

        return history;
    }

    /**
     * Gets query metrics history for a specific query.
     */
    public List<QueryMetricsHistory> getQueryMetricsHistory(String queryId, int hours) {
        List<QueryMetricsHistory> history = new ArrayList<>();
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);

        String sql = """
            SELECT id, sampled_at, query_id, query_text, total_calls, total_time_ms,
                   total_rows, mean_time_ms, min_time_ms, max_time_ms, stddev_time_ms,
                   shared_blks_hit, shared_blks_read, temp_blks_written
            FROM pgconsole.query_metrics_history
            WHERE query_id = ? AND sampled_at >= ?
            ORDER BY sampled_at ASC
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, queryId);
            stmt.setTimestamp(2, Timestamp.from(since));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    QueryMetricsHistory m = new QueryMetricsHistory();
                    m.setId(rs.getLong("id"));
                    m.setSampledAt(rs.getTimestamp("sampled_at").toInstant());
                    m.setQueryId(rs.getString("query_id"));
                    m.setQueryText(rs.getString("query_text"));
                    m.setTotalCalls(rs.getLong("total_calls"));
                    m.setTotalTimeMs(rs.getDouble("total_time_ms"));
                    m.setTotalRows(rs.getLong("total_rows"));
                    m.setMeanTimeMs(rs.getDouble("mean_time_ms"));
                    m.setMinTimeMs(rs.getObject("min_time_ms", Double.class));
                    m.setMaxTimeMs(rs.getObject("max_time_ms", Double.class));
                    m.setStddevTimeMs(rs.getObject("stddev_time_ms", Double.class));
                    m.setSharedBlksHit(rs.getObject("shared_blks_hit", Long.class));
                    m.setSharedBlksRead(rs.getObject("shared_blks_read", Long.class));
                    m.setTempBlksWritten(rs.getObject("temp_blks_written", Long.class));
                    history.add(m);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get query metrics history", e);
        }

        return history;
    }

    /**
     * Gets database metrics history.
     */
    public List<DatabaseMetricsHistory> getDatabaseMetricsHistory(String databaseName, int hours) {
        List<DatabaseMetricsHistory> history = new ArrayList<>();
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);

        String sql = """
            SELECT id, sampled_at, database_name, num_backends, xact_commit, xact_rollback,
                   blks_hit, blks_read, cache_hit_ratio, tup_returned, tup_fetched,
                   tup_inserted, tup_updated, tup_deleted, deadlocks, conflicts,
                   temp_files, temp_bytes, database_size_bytes
            FROM pgconsole.database_metrics_history
            WHERE database_name = ? AND sampled_at >= ?
            ORDER BY sampled_at ASC
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, databaseName);
            stmt.setTimestamp(2, Timestamp.from(since));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    DatabaseMetricsHistory m = new DatabaseMetricsHistory();
                    m.setId(rs.getLong("id"));
                    m.setSampledAt(rs.getTimestamp("sampled_at").toInstant());
                    m.setDatabaseName(rs.getString("database_name"));
                    m.setNumBackends(rs.getInt("num_backends"));
                    m.setXactCommit(rs.getLong("xact_commit"));
                    m.setXactRollback(rs.getLong("xact_rollback"));
                    m.setBlksHit(rs.getLong("blks_hit"));
                    m.setBlksRead(rs.getLong("blks_read"));
                    m.setCacheHitRatio(rs.getObject("cache_hit_ratio", Double.class));
                    m.setTupReturned(rs.getObject("tup_returned", Long.class));
                    m.setTupFetched(rs.getObject("tup_fetched", Long.class));
                    m.setTupInserted(rs.getObject("tup_inserted", Long.class));
                    m.setTupUpdated(rs.getObject("tup_updated", Long.class));
                    m.setTupDeleted(rs.getObject("tup_deleted", Long.class));
                    m.setDeadlocks(rs.getObject("deadlocks", Long.class));
                    m.setConflicts(rs.getObject("conflicts", Long.class));
                    m.setTempFiles(rs.getObject("temp_files", Long.class));
                    m.setTempBytes(rs.getObject("temp_bytes", Long.class));
                    m.setDatabaseSizeBytes(rs.getObject("database_size_bytes", Long.class));
                    history.add(m);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get database metrics history", e);
        }

        return history;
    }

    /**
     * Deletes history data older than the specified number of days.
     */
    public int deleteOldData(int retentionDays) {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        int totalDeleted = 0;

        String[] tables = {
            "pgconsole.system_metrics_history",
            "pgconsole.query_metrics_history",
            "pgconsole.database_metrics_history"
        };

        try (Connection conn = dataSource.getConnection()) {
            for (String table : tables) {
                String sql = "DELETE FROM " + table + " WHERE sampled_at < ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setTimestamp(1, Timestamp.from(cutoff));
                    totalDeleted += stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete old history data", e);
        }

        return totalDeleted;
    }
}
