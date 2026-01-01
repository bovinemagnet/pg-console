package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.config.InstanceConfig;
import com.bovinemagnet.pgconsole.model.DatabaseMetricsHistory;
import com.bovinemagnet.pgconsole.model.QueryMetricsHistory;
import com.bovinemagnet.pgconsole.model.SystemMetricsHistory;
import com.bovinemagnet.pgconsole.repository.HistoryRepository;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.sql.*;
import java.time.Instant;
import java.util.List;

/**
 * Scheduled service for sampling PostgreSQL metrics across all instances.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class MetricsSamplerService {

    private static final Logger LOG = Logger.getLogger(MetricsSamplerService.class);

    @Inject
    DataSourceManager dataSourceManager;

    @Inject
    HistoryRepository historyRepository;

    @Inject
    InstanceConfig config;

    @Inject
    PostgresService postgresService;

    @Inject
    AlertingService alertingService;

    /**
     * Samples system metrics every minute (configurable via cron).
     * Iterates over all configured instances and captures system, query, and database metrics.
     * Also performs alerting threshold checks if alerting is enabled.
     * <p>
     * This method is scheduled to run at regular intervals and will skip concurrent execution
     * to prevent overlapping runs. Failed sampling for individual instances is logged but
     * does not prevent sampling of other instances.
     *
     * @see #sampleSystemMetrics(String)
     * @see #sampleQueryMetrics(String)
     * @see #sampleDatabaseMetrics(String)
     */
    @Scheduled(every = "${pg-console.history.interval-seconds:60}s",
               concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void sampleMetrics() {
        // Skip if schema is disabled (read-only mode) or history is disabled
        if (!config.schema().enabled() || !config.history().enabled()) {
            return;
        }

        List<String> instances = dataSourceManager.getAvailableInstances();
        for (String instanceId : instances) {
            try {
                sampleSystemMetrics(instanceId);
                sampleQueryMetrics(instanceId);
                sampleDatabaseMetrics(instanceId);

                // Check alerting thresholds
                if (config.alerting().enabled()) {
                    try {
                        var stats = postgresService.getOverviewStats(instanceId);
                        alertingService.checkAndAlert(instanceId, stats);
                    } catch (Exception e) {
                        LOG.debugf("Failed to check alerts for instance %s: %s", instanceId, e.getMessage());
                    }
                }

                LOG.debugf("Metrics sampling completed for instance: %s", instanceId);
            } catch (Exception e) {
                LOG.errorf("Failed to sample metrics for instance %s: %s", instanceId, e.getMessage());
            }
        }
    }

    /**
     * Cleans up old history data daily at 3 AM.
     * Removes historical metrics older than the configured retention period to prevent
     * unbounded growth of the history tables.
     * <p>
     * The retention period is configured via {@code pg-console.history.retention-days}.
     * This operation is performed once daily and skips concurrent execution to prevent
     * overlapping cleanup runs.
     */
    @Scheduled(cron = "0 0 3 * * ?",
               concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void cleanupOldData() {
        // Skip if schema is disabled (read-only mode) or history is disabled
        if (!config.schema().enabled() || !config.history().enabled()) {
            return;
        }

        try {
            int retentionDays = config.history().retentionDays();
            int deleted = historyRepository.deleteOldData(retentionDays);
            LOG.infof("Cleaned up %d old history records (retention: %d days)", deleted, retentionDays);
        } catch (Exception e) {
            LOG.error("Failed to cleanup old history data", e);
        }
    }

    /**
     * Samples system-level metrics for a single instance.
     * Captures connection counts, active query counts, cache hit ratios, and database sizes.
     * <p>
     * The metrics are persisted to the history repository for trend analysis and alerting.
     *
     * @param instanceId the database instance identifier
     */
    private void sampleSystemMetrics(String instanceId) {
        String sql = """
            SELECT
                (SELECT count(*) FROM pg_stat_activity) as total_connections,
                (SELECT setting::int FROM pg_settings WHERE name = 'max_connections') as max_connections,
                (SELECT count(*) FROM pg_stat_activity WHERE state = 'active' AND query NOT LIKE '%pg_stat_activity%') as active_queries,
                (SELECT count(*) FROM pg_stat_activity WHERE state = 'idle') as idle_connections,
                (SELECT count(*) FROM pg_stat_activity WHERE state = 'idle in transaction') as idle_in_transaction,
                (SELECT count(*) FROM pg_stat_activity WHERE wait_event_type = 'Lock') as blocked_queries,
                (SELECT EXTRACT(EPOCH FROM max(now() - query_start)) FROM pg_stat_activity WHERE state = 'active' AND query NOT LIKE '%pg_stat_activity%') as longest_query_seconds,
                (SELECT EXTRACT(EPOCH FROM max(now() - xact_start)) FROM pg_stat_activity WHERE xact_start IS NOT NULL) as longest_transaction_seconds,
                (SELECT
                    CASE WHEN sum(blks_hit) + sum(blks_read) > 0
                    THEN (sum(blks_hit) * 100.0) / (sum(blks_hit) + sum(blks_read))
                    ELSE 100.0 END
                FROM pg_stat_database) as cache_hit_ratio,
                (SELECT sum(pg_database_size(datname)) FROM pg_database WHERE datistemplate = false) as total_database_size_bytes
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceId).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                SystemMetricsHistory metrics = new SystemMetricsHistory();
                metrics.setSampledAt(Instant.now());
                metrics.setTotalConnections(rs.getInt("total_connections"));
                metrics.setMaxConnections(rs.getInt("max_connections"));
                metrics.setActiveQueries(rs.getInt("active_queries"));
                metrics.setIdleConnections(rs.getInt("idle_connections"));
                metrics.setIdleInTransaction(rs.getInt("idle_in_transaction"));
                metrics.setBlockedQueries(rs.getInt("blocked_queries"));
                metrics.setLongestQuerySeconds(getDoubleOrNull(rs, "longest_query_seconds"));
                metrics.setLongestTransactionSeconds(getDoubleOrNull(rs, "longest_transaction_seconds"));
                metrics.setCacheHitRatio(getDoubleOrNull(rs, "cache_hit_ratio"));
                metrics.setTotalDatabaseSizeBytes(getLongOrNull(rs, "total_database_size_bytes"));

                historyRepository.saveSystemMetrics(instanceId, metrics);
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to sample system metrics for %s: %s", instanceId, e.getMessage());
        }
    }

    /**
     * Samples query-level metrics for a single instance.
     * Captures the top N slowest queries from pg_stat_statements including execution times,
     * call counts, and buffer statistics.
     * <p>
     * The number of top queries captured is configurable via {@code pg-console.history.top-queries}.
     * Queries containing 'pg_stat_statements' or 'pg_console' are excluded from capture.
     *
     * @param instanceId the database instance identifier
     */
    private void sampleQueryMetrics(String instanceId) {
        String sql = """
            SELECT
                md5(query) as query_id,
                query,
                calls as total_calls,
                total_exec_time as total_time_ms,
                rows as total_rows,
                mean_exec_time as mean_time_ms,
                min_exec_time as min_time_ms,
                max_exec_time as max_time_ms,
                stddev_exec_time as stddev_time_ms,
                shared_blks_hit,
                shared_blks_read,
                temp_blks_written
            FROM pg_stat_statements
            WHERE query NOT LIKE '%pg_stat_statements%'
              AND query NOT LIKE '%pg_console%'
            ORDER BY total_exec_time DESC
            LIMIT ?
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceId).getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, config.history().topQueries());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    QueryMetricsHistory metrics = new QueryMetricsHistory();
                    metrics.setSampledAt(Instant.now());
                    metrics.setQueryId(rs.getString("query_id"));
                    metrics.setQueryText(rs.getString("query"));
                    metrics.setTotalCalls(rs.getLong("total_calls"));
                    metrics.setTotalTimeMs(rs.getDouble("total_time_ms"));
                    metrics.setTotalRows(rs.getLong("total_rows"));
                    metrics.setMeanTimeMs(rs.getDouble("mean_time_ms"));
                    metrics.setMinTimeMs(getDoubleOrNull(rs, "min_time_ms"));
                    metrics.setMaxTimeMs(getDoubleOrNull(rs, "max_time_ms"));
                    metrics.setStddevTimeMs(getDoubleOrNull(rs, "stddev_time_ms"));
                    metrics.setSharedBlksHit(getLongOrNull(rs, "shared_blks_hit"));
                    metrics.setSharedBlksRead(getLongOrNull(rs, "shared_blks_read"));
                    metrics.setTempBlksWritten(getLongOrNull(rs, "temp_blks_written"));

                    historyRepository.saveQueryMetrics(instanceId, metrics);
                }
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to sample query metrics for %s: %s", instanceId, e.getMessage());
        }
    }

    /**
     * Samples database-level metrics for a single instance.
     * Captures statistics for each database including transaction counts, cache hit ratios,
     * tuple activity, deadlocks, conflicts, and temporary file usage.
     * <p>
     * Template databases are excluded from sampling. Metrics are captured from pg_stat_database
     * and enriched with size information from pg_database_size().
     *
     * @param instanceId the database instance identifier
     */
    private void sampleDatabaseMetrics(String instanceId) {
        String sql = """
            SELECT
                d.datname,
                d.numbackends,
                d.xact_commit,
                d.xact_rollback,
                d.blks_hit,
                d.blks_read,
                CASE WHEN d.blks_hit + d.blks_read > 0
                    THEN (d.blks_hit * 100.0) / (d.blks_hit + d.blks_read)
                    ELSE 100.0 END as cache_hit_ratio,
                d.tup_returned,
                d.tup_fetched,
                d.tup_inserted,
                d.tup_updated,
                d.tup_deleted,
                d.deadlocks,
                d.conflicts,
                d.temp_files,
                d.temp_bytes,
                pg_database_size(d.datname) as database_size_bytes
            FROM pg_stat_database d
            JOIN pg_database db ON d.datid = db.oid
            WHERE db.datistemplate = false
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceId).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                DatabaseMetricsHistory metrics = new DatabaseMetricsHistory();
                metrics.setSampledAt(Instant.now());
                metrics.setDatabaseName(rs.getString("datname"));
                metrics.setNumBackends(rs.getInt("numbackends"));
                metrics.setXactCommit(rs.getLong("xact_commit"));
                metrics.setXactRollback(rs.getLong("xact_rollback"));
                metrics.setBlksHit(rs.getLong("blks_hit"));
                metrics.setBlksRead(rs.getLong("blks_read"));
                metrics.setCacheHitRatio(getDoubleOrNull(rs, "cache_hit_ratio"));
                metrics.setTupReturned(getLongOrNull(rs, "tup_returned"));
                metrics.setTupFetched(getLongOrNull(rs, "tup_fetched"));
                metrics.setTupInserted(getLongOrNull(rs, "tup_inserted"));
                metrics.setTupUpdated(getLongOrNull(rs, "tup_updated"));
                metrics.setTupDeleted(getLongOrNull(rs, "tup_deleted"));
                metrics.setDeadlocks(getLongOrNull(rs, "deadlocks"));
                metrics.setConflicts(getLongOrNull(rs, "conflicts"));
                metrics.setTempFiles(getLongOrNull(rs, "temp_files"));
                metrics.setTempBytes(getLongOrNull(rs, "temp_bytes"));
                metrics.setDatabaseSizeBytes(getLongOrNull(rs, "database_size_bytes"));

                historyRepository.saveDatabaseMetrics(instanceId, metrics);
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to sample database metrics for %s: %s", instanceId, e.getMessage());
        }
    }

    /**
     * Safely extracts a double value from a ResultSet, returning null if the value is SQL NULL.
     *
     * @param rs the ResultSet to read from
     * @param column the column name to retrieve
     * @return the double value, or null if the database value is NULL
     * @throws SQLException if a database access error occurs
     */
    private Double getDoubleOrNull(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    /**
     * Safely extracts a long value from a ResultSet, returning null if the value is SQL NULL.
     *
     * @param rs the ResultSet to read from
     * @param column the column name to retrieve
     * @return the long value, or null if the database value is NULL
     * @throws SQLException if a database access error occurs
     */
    private Long getLongOrNull(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
