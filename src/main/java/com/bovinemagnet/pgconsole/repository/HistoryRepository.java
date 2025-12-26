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
 * Supports multi-instance via instance_id column.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class HistoryRepository {

    @Inject
    DataSource dataSource;

    /**
     * Saves a system metrics snapshot for an instance.
     */
    public void saveSystemMetrics(String instanceId, SystemMetricsHistory metrics) {
        String sql = """
            INSERT INTO pgconsole.system_metrics_history (
                instance_id, sampled_at, total_connections, max_connections, active_queries,
                idle_connections, idle_in_transaction, blocked_queries,
                longest_query_seconds, longest_transaction_seconds,
                cache_hit_ratio, total_database_size_bytes
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, instanceId);
            stmt.setTimestamp(2, Timestamp.from(Instant.now()));
            stmt.setInt(3, metrics.getTotalConnections());
            stmt.setInt(4, metrics.getMaxConnections());
            stmt.setInt(5, metrics.getActiveQueries());
            stmt.setInt(6, metrics.getIdleConnections());
            stmt.setInt(7, metrics.getIdleInTransaction());
            stmt.setInt(8, metrics.getBlockedQueries());
            stmt.setObject(9, metrics.getLongestQuerySeconds());
            stmt.setObject(10, metrics.getLongestTransactionSeconds());
            stmt.setObject(11, metrics.getCacheHitRatio());
            stmt.setObject(12, metrics.getTotalDatabaseSizeBytes());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save system metrics for " + instanceId, e);
        }
    }

    /** Backward-compatible overload for default instance. */
    public void saveSystemMetrics(SystemMetricsHistory metrics) {
        saveSystemMetrics("default", metrics);
    }

    /**
     * Saves a query metrics snapshot for an instance.
     */
    public void saveQueryMetrics(String instanceId, QueryMetricsHistory metrics) {
        String sql = """
            INSERT INTO pgconsole.query_metrics_history (
                instance_id, sampled_at, query_id, query_text, total_calls, total_time_ms,
                total_rows, mean_time_ms, min_time_ms, max_time_ms, stddev_time_ms,
                shared_blks_hit, shared_blks_read, temp_blks_written
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, instanceId);
            stmt.setTimestamp(2, Timestamp.from(Instant.now()));
            stmt.setString(3, metrics.getQueryId());
            stmt.setString(4, metrics.getQueryText());
            stmt.setLong(5, metrics.getTotalCalls());
            stmt.setDouble(6, metrics.getTotalTimeMs());
            stmt.setLong(7, metrics.getTotalRows());
            stmt.setDouble(8, metrics.getMeanTimeMs());
            stmt.setObject(9, metrics.getMinTimeMs());
            stmt.setObject(10, metrics.getMaxTimeMs());
            stmt.setObject(11, metrics.getStddevTimeMs());
            stmt.setObject(12, metrics.getSharedBlksHit());
            stmt.setObject(13, metrics.getSharedBlksRead());
            stmt.setObject(14, metrics.getTempBlksWritten());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save query metrics for " + instanceId, e);
        }
    }

    /** Backward-compatible overload for default instance. */
    public void saveQueryMetrics(QueryMetricsHistory metrics) {
        saveQueryMetrics("default", metrics);
    }

    /**
     * Saves a database metrics snapshot for an instance.
     */
    public void saveDatabaseMetrics(String instanceId, DatabaseMetricsHistory metrics) {
        String sql = """
            INSERT INTO pgconsole.database_metrics_history (
                instance_id, sampled_at, database_name, num_backends, xact_commit, xact_rollback,
                blks_hit, blks_read, cache_hit_ratio, tup_returned, tup_fetched,
                tup_inserted, tup_updated, tup_deleted, deadlocks, conflicts,
                temp_files, temp_bytes, database_size_bytes
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, instanceId);
            stmt.setTimestamp(2, Timestamp.from(Instant.now()));
            stmt.setString(3, metrics.getDatabaseName());
            stmt.setInt(4, metrics.getNumBackends());
            stmt.setLong(5, metrics.getXactCommit());
            stmt.setLong(6, metrics.getXactRollback());
            stmt.setLong(7, metrics.getBlksHit());
            stmt.setLong(8, metrics.getBlksRead());
            stmt.setObject(9, metrics.getCacheHitRatio());
            stmt.setObject(10, metrics.getTupReturned());
            stmt.setObject(11, metrics.getTupFetched());
            stmt.setObject(12, metrics.getTupInserted());
            stmt.setObject(13, metrics.getTupUpdated());
            stmt.setObject(14, metrics.getTupDeleted());
            stmt.setObject(15, metrics.getDeadlocks());
            stmt.setObject(16, metrics.getConflicts());
            stmt.setObject(17, metrics.getTempFiles());
            stmt.setObject(18, metrics.getTempBytes());
            stmt.setObject(19, metrics.getDatabaseSizeBytes());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save database metrics for " + instanceId, e);
        }
    }

    /** Backward-compatible overload for default instance. */
    public void saveDatabaseMetrics(DatabaseMetricsHistory metrics) {
        saveDatabaseMetrics("default", metrics);
    }

    /**
     * Gets system metrics history for an instance for the last N hours.
     */
    public List<SystemMetricsHistory> getSystemMetricsHistory(String instanceId, int hours) {
        List<SystemMetricsHistory> history = new ArrayList<>();
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);

        String sql = """
            SELECT id, sampled_at, total_connections, max_connections, active_queries,
                   idle_connections, idle_in_transaction, blocked_queries,
                   longest_query_seconds, longest_transaction_seconds,
                   cache_hit_ratio, total_database_size_bytes
            FROM pgconsole.system_metrics_history
            WHERE instance_id = ? AND sampled_at >= ?
            ORDER BY sampled_at ASC
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, instanceId);
            stmt.setTimestamp(2, Timestamp.from(since));

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
                    m.setLongestQuerySeconds(getDoubleOrNull(rs, "longest_query_seconds"));
                    m.setLongestTransactionSeconds(getDoubleOrNull(rs, "longest_transaction_seconds"));
                    m.setCacheHitRatio(getDoubleOrNull(rs, "cache_hit_ratio"));
                    m.setTotalDatabaseSizeBytes(getLongOrNull(rs, "total_database_size_bytes"));
                    history.add(m);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get system metrics history for " + instanceId, e);
        }

        return history;
    }

    /** Backward-compatible overload for default instance. */
    public List<SystemMetricsHistory> getSystemMetricsHistory(int hours) {
        return getSystemMetricsHistory("default", hours);
    }

    /**
     * Gets query metrics history for a specific query on an instance.
     */
    public List<QueryMetricsHistory> getQueryMetricsHistory(String instanceId, String queryId, int hours) {
        List<QueryMetricsHistory> history = new ArrayList<>();
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);

        String sql = """
            SELECT id, sampled_at, query_id, query_text, total_calls, total_time_ms,
                   total_rows, mean_time_ms, min_time_ms, max_time_ms, stddev_time_ms,
                   shared_blks_hit, shared_blks_read, temp_blks_written
            FROM pgconsole.query_metrics_history
            WHERE instance_id = ? AND query_id = ? AND sampled_at >= ?
            ORDER BY sampled_at ASC
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, instanceId);
            stmt.setString(2, queryId);
            stmt.setTimestamp(3, Timestamp.from(since));

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
                    m.setMinTimeMs(getDoubleOrNull(rs, "min_time_ms"));
                    m.setMaxTimeMs(getDoubleOrNull(rs, "max_time_ms"));
                    m.setStddevTimeMs(getDoubleOrNull(rs, "stddev_time_ms"));
                    m.setSharedBlksHit(getLongOrNull(rs, "shared_blks_hit"));
                    m.setSharedBlksRead(getLongOrNull(rs, "shared_blks_read"));
                    m.setTempBlksWritten(getLongOrNull(rs, "temp_blks_written"));
                    history.add(m);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get query metrics history for " + instanceId, e);
        }

        return history;
    }

    /** Backward-compatible overload for default instance. */
    public List<QueryMetricsHistory> getQueryMetricsHistory(String queryId, int hours) {
        return getQueryMetricsHistory("default", queryId, hours);
    }

    /**
     * Gets database metrics history for an instance.
     */
    public List<DatabaseMetricsHistory> getDatabaseMetricsHistory(String instanceId, String databaseName, int hours) {
        List<DatabaseMetricsHistory> history = new ArrayList<>();
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);

        String sql = """
            SELECT id, sampled_at, database_name, num_backends, xact_commit, xact_rollback,
                   blks_hit, blks_read, cache_hit_ratio, tup_returned, tup_fetched,
                   tup_inserted, tup_updated, tup_deleted, deadlocks, conflicts,
                   temp_files, temp_bytes, database_size_bytes
            FROM pgconsole.database_metrics_history
            WHERE instance_id = ? AND database_name = ? AND sampled_at >= ?
            ORDER BY sampled_at ASC
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, instanceId);
            stmt.setString(2, databaseName);
            stmt.setTimestamp(3, Timestamp.from(since));

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
                    m.setCacheHitRatio(getDoubleOrNull(rs, "cache_hit_ratio"));
                    m.setTupReturned(getLongOrNull(rs, "tup_returned"));
                    m.setTupFetched(getLongOrNull(rs, "tup_fetched"));
                    m.setTupInserted(getLongOrNull(rs, "tup_inserted"));
                    m.setTupUpdated(getLongOrNull(rs, "tup_updated"));
                    m.setTupDeleted(getLongOrNull(rs, "tup_deleted"));
                    m.setDeadlocks(getLongOrNull(rs, "deadlocks"));
                    m.setConflicts(getLongOrNull(rs, "conflicts"));
                    m.setTempFiles(getLongOrNull(rs, "temp_files"));
                    m.setTempBytes(getLongOrNull(rs, "temp_bytes"));
                    m.setDatabaseSizeBytes(getLongOrNull(rs, "database_size_bytes"));
                    history.add(m);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get database metrics history for " + instanceId, e);
        }

        return history;
    }

    /** Backward-compatible overload for default instance. */
    public List<DatabaseMetricsHistory> getDatabaseMetricsHistory(String databaseName, int hours) {
        return getDatabaseMetricsHistory("default", databaseName, hours);
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

    /**
     * Gets aggregated query metrics for a time period.
     * Returns the average mean_time, total calls, and latest query text for each query.
     *
     * @param instanceId the instance ID
     * @param startHoursAgo hours ago for the start of the period
     * @param endHoursAgo hours ago for the end of the period
     * @return list of aggregated query metrics
     */
    public List<QueryMetricsHistory> getAggregatedQueryMetrics(String instanceId, int startHoursAgo, int endHoursAgo) {
        List<QueryMetricsHistory> results = new ArrayList<>();
        Instant startTime = Instant.now().minus(startHoursAgo, ChronoUnit.HOURS);
        Instant endTime = Instant.now().minus(endHoursAgo, ChronoUnit.HOURS);

        String sql = """
            SELECT
                query_id,
                MAX(query_text) as query_text,
                AVG(mean_time_ms) as avg_mean_time,
                SUM(total_calls) as total_calls,
                SUM(total_time_ms) as total_time,
                COUNT(*) as sample_count
            FROM pgconsole.query_metrics_history
            WHERE instance_id = ?
              AND sampled_at >= ? AND sampled_at < ?
            GROUP BY query_id
            HAVING COUNT(*) >= 2
            ORDER BY AVG(mean_time_ms) DESC
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, instanceId);
            stmt.setTimestamp(2, Timestamp.from(startTime));
            stmt.setTimestamp(3, Timestamp.from(endTime));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    QueryMetricsHistory m = new QueryMetricsHistory();
                    m.setQueryId(rs.getString("query_id"));
                    m.setQueryText(rs.getString("query_text"));
                    m.setMeanTimeMs(rs.getDouble("avg_mean_time"));
                    m.setTotalCalls(rs.getLong("total_calls"));
                    m.setTotalTimeMs(rs.getDouble("total_time"));
                    results.add(m);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get aggregated query metrics for " + instanceId, e);
        }

        return results;
    }

    private Double getDoubleOrNull(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    private Long getLongOrNull(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
