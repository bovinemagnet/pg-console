package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.config.InstanceConfig;
import com.bovinemagnet.pgconsole.model.SystemMetricsHistory;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;

/**
 * In-memory metrics sampler for schema-free mode.
 * <p>
 * This service samples PostgreSQL system metrics and stores them in the
 * {@link InMemoryMetricsStore} when the pgconsole schema is disabled.
 * It runs at a shorter interval than the persistent sampler to provide
 * more responsive in-memory trends.
 * <p>
 * Only samples system-level metrics (connections, active queries, blocked
 * queries, cache hit ratio) as these are the metrics displayed in sparklines.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class InMemoryMetricsSampler {

    private static final Logger LOG = Logger.getLogger("pgconsole.InMemoryMetricsSampler");

    /**
     * SQL query for sampling system metrics.
     * Same query as MetricsSamplerService but used for in-memory storage.
     */
    private static final String SYSTEM_METRICS_SQL = """
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

    @Inject
    DataSourceManager dataSourceManager;

    @Inject
    InMemoryMetricsStore metricsStore;

    @Inject
    InstanceConfig config;

    @Inject
    PostgresService postgresService;

    @Inject
    AlertingService alertingService;

    /**
     * Samples system metrics every 30 seconds when schema is disabled.
     * <p>
     * This shorter interval (compared to the 60-second persistent sampler)
     * provides more responsive in-memory trends. The sampler only runs when
     * the schema is disabled (read-only mode).
     */
    @Scheduled(every = "30s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void sampleInMemoryMetrics() {
        // Only run when schema is disabled (in-memory mode)
        if (config.schema().enabled()) {
            return;
        }

        List<String> instances = dataSourceManager.getAvailableInstances();
        for (String instanceId : instances) {
            try {
                sampleSystemMetrics(instanceId);

                // Also check alerting thresholds even in schema-free mode
                if (config.alerting().enabled()) {
                    try {
                        var stats = postgresService.getOverviewStats(instanceId);
                        alertingService.checkAndAlert(instanceId, stats);
                    } catch (Exception e) {
                        LOG.debugf("Failed to check alerts for instance %s: %s", instanceId, e.getMessage());
                    }
                }

                LOG.debugf("In-memory metrics sampling completed for instance: %s", instanceId);
            } catch (Exception e) {
                LOG.warnf("Failed to sample in-memory metrics for instance %s: %s", instanceId, e.getMessage());
            }
        }
    }

    /**
     * Samples system-level metrics for a single instance and stores in memory.
     *
     * @param instanceId the database instance identifier
     */
    private void sampleSystemMetrics(String instanceId) {
        try (Connection conn = dataSourceManager.getDataSource(instanceId).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SYSTEM_METRICS_SQL)) {

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

                metricsStore.addSystemMetrics(instanceId, metrics);
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to sample system metrics for %s: %s", instanceId, e.getMessage());
        }
    }

    /**
     * Safely extracts a double value from a ResultSet.
     *
     * @param rs     the ResultSet to read from
     * @param column the column name to retrieve
     * @return the double value, or null if the database value is NULL
     * @throws SQLException if a database access error occurs
     */
    private Double getDoubleOrNull(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    /**
     * Safely extracts a long value from a ResultSet.
     *
     * @param rs     the ResultSet to read from
     * @param column the column name to retrieve
     * @return the long value, or null if the database value is NULL
     * @throws SQLException if a database access error occurs
     */
    private Long getLongOrNull(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
