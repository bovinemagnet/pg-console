package com.bovinemagnet.pgconsole.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for Change Data Capture (CDC) monitoring.
 * Tracks table change activity, row velocities, and WAL generation.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class ChangeDataCaptureService {

    private static final Logger LOG = Logger.getLogger(ChangeDataCaptureService.class);

    @Inject
    DataSourceManager dataSourceManager;

    /**
     * Retrieves table change activity statistics for all user tables in the specified database instance.
     * <p>
     * Queries the {@code pg_stat_user_tables} system view to gather comprehensive statistics about
     * table modifications including insert, update, delete operations, HOT updates, tuple counts,
     * and maintenance activity.
     * <p>
     * Results are ordered by total change activity (inserts + updates + deletes) in descending order
     * and limited to the top 50 most active tables. System schemas (pg_catalog, information_schema, pgconsole)
     * are excluded from the results.
     *
     * @param instanceName the name of the PostgreSQL database instance to query
     * @return a list of {@link TableChangeActivity} objects containing change statistics for each table;
     *         returns an empty list if the query fails or no user tables exist
     * @see TableChangeActivity
     */
    public List<TableChangeActivity> getTableChangeActivity(String instanceName) {
        List<TableChangeActivity> activities = new ArrayList<>();

        String sql = """
            SELECT
                schemaname,
                relname as tablename,
                n_tup_ins as inserts,
                n_tup_upd as updates,
                n_tup_del as deletes,
                n_tup_hot_upd as hot_updates,
                n_live_tup as live_tuples,
                n_dead_tup as dead_tuples,
                n_mod_since_analyze as mods_since_analyze,
                last_vacuum,
                last_autovacuum,
                last_analyze,
                last_autoanalyze,
                pg_size_pretty(pg_total_relation_size(schemaname || '.' || relname)) as table_size,
                pg_total_relation_size(schemaname || '.' || relname) as table_size_bytes
            FROM pg_stat_user_tables
            WHERE schemaname NOT IN ('pg_catalog', 'information_schema', 'pgconsole')
            ORDER BY (n_tup_ins + n_tup_upd + n_tup_del) DESC
            LIMIT 50
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                TableChangeActivity activity = new TableChangeActivity();
                activity.setSchemaName(rs.getString("schemaname"));
                activity.setTableName(rs.getString("tablename"));
                activity.setInserts(rs.getLong("inserts"));
                activity.setUpdates(rs.getLong("updates"));
                activity.setDeletes(rs.getLong("deletes"));
                activity.setHotUpdates(rs.getLong("hot_updates"));
                activity.setLiveTuples(rs.getLong("live_tuples"));
                activity.setDeadTuples(rs.getLong("dead_tuples"));
                activity.setModsSinceAnalyze(rs.getLong("mods_since_analyze"));
                activity.setTableSize(rs.getString("table_size"));
                activity.setTableSizeBytes(rs.getLong("table_size_bytes"));

                Timestamp lastVacuum = rs.getTimestamp("last_vacuum");
                if (lastVacuum != null) activity.setLastVacuum(lastVacuum.toInstant());

                Timestamp lastAutoVacuum = rs.getTimestamp("last_autovacuum");
                if (lastAutoVacuum != null) activity.setLastAutoVacuum(lastAutoVacuum.toInstant());

                activities.add(activity);
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to get table change activity for %s: %s", instanceName, e.getMessage());
        }

        return activities;
    }

    /**
     * Retrieves tables with high modification rates (high churn) from the specified database instance.
     * <p>
     * Identifies tables experiencing significant data modification activity by calculating the total
     * number of changes (inserts + updates + deletes) and the churn ratio (total changes divided by
     * live tuples). This metric helps identify tables that may benefit from more aggressive vacuum
     * settings or partitioning strategies.
     * <p>
     * Only tables with more than 100 live tuples are included to filter out insignificant tables.
     * Results are ordered by total changes in descending order.
     *
     * @param instanceName the name of the PostgreSQL database instance to query
     * @param limit the maximum number of high-churn tables to return
     * @return a list of {@link HighChurnTable} objects ordered by total changes descending;
     *         returns an empty list if the query fails or no qualifying tables exist
     * @see HighChurnTable
     */
    public List<HighChurnTable> getHighChurnTables(String instanceName, int limit) {
        List<HighChurnTable> tables = new ArrayList<>();

        String sql = """
            SELECT
                schemaname,
                relname as tablename,
                n_tup_ins + n_tup_upd + n_tup_del as total_changes,
                n_tup_ins as inserts,
                n_tup_upd as updates,
                n_tup_del as deletes,
                n_live_tup as live_tuples,
                CASE WHEN n_live_tup > 0
                    THEN (n_tup_ins + n_tup_upd + n_tup_del)::float / n_live_tup
                    ELSE 0
                END as churn_ratio,
                pg_size_pretty(pg_total_relation_size(schemaname || '.' || relname)) as table_size
            FROM pg_stat_user_tables
            WHERE schemaname NOT IN ('pg_catalog', 'information_schema', 'pgconsole')
              AND n_live_tup > 100
            ORDER BY total_changes DESC
            LIMIT ?
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    HighChurnTable table = new HighChurnTable();
                    table.setSchemaName(rs.getString("schemaname"));
                    table.setTableName(rs.getString("tablename"));
                    table.setTotalChanges(rs.getLong("total_changes"));
                    table.setInserts(rs.getLong("inserts"));
                    table.setUpdates(rs.getLong("updates"));
                    table.setDeletes(rs.getLong("deletes"));
                    table.setLiveTuples(rs.getLong("live_tuples"));
                    table.setChurnRatio(rs.getDouble("churn_ratio"));
                    table.setTableSize(rs.getString("table_size"));
                    tables.add(table);
                }
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to get high churn tables for %s: %s", instanceName, e.getMessage());
        }

        return tables;
    }

    /**
     * Estimates Write-Ahead Log (WAL) generation by table based on change counts and row sizes.
     * <p>
     * Calculates an approximate WAL generation volume for each table by multiplying the total
     * number of tuple changes by the average row size, then applying a 1.2x overhead factor to
     * account for WAL metadata and alignment. This estimation helps identify tables that are
     * primary contributors to WAL volume and may impact replication lag or backup performance.
     * <p>
     * The average row size is calculated as total table size divided by live tuples. Only tables
     * with active modifications (total_changes &gt; 0) and live tuples are included in the results.
     * Results are limited to the top 20 tables by estimated WAL generation.
     * <p>
     * <strong>Note:</strong> This is an approximation and may not reflect actual WAL volume due to
     * factors such as index updates, TOAST data, and varying transaction patterns.
     *
     * @param instanceName the name of the PostgreSQL database instance to query
     * @return a list of {@link TableWalEstimate} objects ordered by estimated WAL bytes descending;
     *         returns an empty list if the query fails or no qualifying tables exist
     * @see TableWalEstimate
     */
    public List<TableWalEstimate> getWalGenerationByTable(String instanceName) {
        List<TableWalEstimate> estimates = new ArrayList<>();

        String sql = """
            WITH table_changes AS (
                SELECT
                    schemaname,
                    relname as tablename,
                    n_tup_ins + n_tup_upd + n_tup_del as total_changes,
                    n_tup_ins,
                    n_tup_upd,
                    n_tup_del,
                    pg_total_relation_size(schemaname || '.' || relname) as table_size,
                    n_live_tup
                FROM pg_stat_user_tables
                WHERE schemaname NOT IN ('pg_catalog', 'information_schema', 'pgconsole')
                  AND n_live_tup > 0
            )
            SELECT
                schemaname,
                tablename,
                total_changes,
                n_tup_ins,
                n_tup_upd,
                n_tup_del,
                table_size,
                n_live_tup,
                -- Estimate average row size
                CASE WHEN n_live_tup > 0 THEN table_size / n_live_tup ELSE 0 END as avg_row_size,
                -- Rough WAL estimate: changes * avg_row_size * 1.2 (overhead factor)
                CASE WHEN n_live_tup > 0
                    THEN (total_changes * (table_size / n_live_tup) * 1.2)::bigint
                    ELSE 0
                END as estimated_wal_bytes
            FROM table_changes
            WHERE total_changes > 0
            ORDER BY estimated_wal_bytes DESC
            LIMIT 20
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                TableWalEstimate estimate = new TableWalEstimate();
                estimate.setSchemaName(rs.getString("schemaname"));
                estimate.setTableName(rs.getString("tablename"));
                estimate.setTotalChanges(rs.getLong("total_changes"));
                estimate.setInserts(rs.getLong("n_tup_ins"));
                estimate.setUpdates(rs.getLong("n_tup_upd"));
                estimate.setDeletes(rs.getLong("n_tup_del"));
                estimate.setAvgRowSize(rs.getLong("avg_row_size"));
                estimate.setEstimatedWalBytes(rs.getLong("estimated_wal_bytes"));
                estimates.add(estimate);
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to get WAL estimates for %s: %s", instanceName, e.getMessage());
        }

        return estimates;
    }

    /**
     * Retrieves aggregate Change Data Capture summary statistics for the specified database instance.
     * <p>
     * Aggregates statistics from {@code pg_stat_user_tables} to provide a database-wide overview
     * of data modification activity. The summary includes total insert, update, delete, and HOT update
     * counts across all user tables, as well as live and dead tuple counts. Additionally, it counts
     * tables with high activity levels (more than 10,000 total changes).
     * <p>
     * This summary is useful for understanding overall database write activity and identifying
     * potential maintenance requirements such as vacuum or index rebuilds.
     *
     * @param instanceName the name of the PostgreSQL database instance to query
     * @return a {@link CdcSummary} object containing aggregate statistics; returns an object
     *         with default values (zeros) if the query fails
     * @see CdcSummary
     */
    public CdcSummary getSummary(String instanceName) {
        CdcSummary summary = new CdcSummary();

        String sql = """
            SELECT
                SUM(n_tup_ins) as total_inserts,
                SUM(n_tup_upd) as total_updates,
                SUM(n_tup_del) as total_deletes,
                SUM(n_tup_hot_upd) as total_hot_updates,
                SUM(n_live_tup) as total_live_tuples,
                SUM(n_dead_tup) as total_dead_tuples,
                COUNT(*) as table_count,
                COUNT(*) FILTER (WHERE n_tup_ins + n_tup_upd + n_tup_del > 10000) as high_activity_tables
            FROM pg_stat_user_tables
            WHERE schemaname NOT IN ('pg_catalog', 'information_schema', 'pgconsole')
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                summary.setTotalInserts(rs.getLong("total_inserts"));
                summary.setTotalUpdates(rs.getLong("total_updates"));
                summary.setTotalDeletes(rs.getLong("total_deletes"));
                summary.setTotalHotUpdates(rs.getLong("total_hot_updates"));
                summary.setTotalLiveTuples(rs.getLong("total_live_tuples"));
                summary.setTotalDeadTuples(rs.getLong("total_dead_tuples"));
                summary.setTableCount(rs.getInt("table_count"));
                summary.setHighActivityTables(rs.getInt("high_activity_tables"));
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to get CDC summary for %s: %s", instanceName, e.getMessage());
        }

        return summary;
    }

    // --- Model Classes ---

    /**
     * Data transfer object containing change activity statistics for a single database table.
     * <p>
     * Encapsulates metrics from PostgreSQL's {@code pg_stat_user_tables} view including
     * tuple modification counts, maintenance timestamps, and table size information.
     * Provides calculated fields for total changes, HOT update ratios, and activity level
     * classification.
     *
     * @see #getTableChangeActivity(String)
     */
    public static class TableChangeActivity {
        private String schemaName;
        private String tableName;
        private long inserts;
        private long updates;
        private long deletes;
        private long hotUpdates;
        private long liveTuples;
        private long deadTuples;
        private long modsSinceAnalyze;
        private String tableSize;
        private long tableSizeBytes;
        private Instant lastVacuum;
        private Instant lastAutoVacuum;

        public String getSchemaName() { return schemaName; }
        public void setSchemaName(String name) { this.schemaName = name; }

        public String getTableName() { return tableName; }
        public void setTableName(String name) { this.tableName = name; }

        public String getFullName() { return schemaName + "." + tableName; }

        public long getInserts() { return inserts; }
        public void setInserts(long inserts) { this.inserts = inserts; }

        public long getUpdates() { return updates; }
        public void setUpdates(long updates) { this.updates = updates; }

        public long getDeletes() { return deletes; }
        public void setDeletes(long deletes) { this.deletes = deletes; }

        public long getHotUpdates() { return hotUpdates; }
        public void setHotUpdates(long hotUpdates) { this.hotUpdates = hotUpdates; }

        public long getTotalChanges() { return inserts + updates + deletes; }

        public long getLiveTuples() { return liveTuples; }
        public void setLiveTuples(long tuples) { this.liveTuples = tuples; }

        public long getDeadTuples() { return deadTuples; }
        public void setDeadTuples(long tuples) { this.deadTuples = tuples; }

        public long getModsSinceAnalyze() { return modsSinceAnalyze; }
        public void setModsSinceAnalyze(long mods) { this.modsSinceAnalyze = mods; }

        public String getTableSize() { return tableSize; }
        public void setTableSize(String size) { this.tableSize = size; }

        public long getTableSizeBytes() { return tableSizeBytes; }
        public void setTableSizeBytes(long bytes) { this.tableSizeBytes = bytes; }

        public Instant getLastVacuum() { return lastVacuum; }
        public void setLastVacuum(Instant time) { this.lastVacuum = time; }

        public Instant getLastAutoVacuum() { return lastAutoVacuum; }
        public void setLastAutoVacuum(Instant time) { this.lastAutoVacuum = time; }

        public double getHotUpdateRatio() {
            if (updates == 0) return 0;
            return (double) hotUpdates / updates * 100;
        }

        public String getHotUpdateRatioFormatted() {
            return String.format("%.1f%%", getHotUpdateRatio());
        }

        public String getActivityLevel() {
            long total = getTotalChanges();
            if (total > 1_000_000) return "Very High";
            if (total > 100_000) return "High";
            if (total > 10_000) return "Medium";
            if (total > 1_000) return "Low";
            return "Minimal";
        }

        public String getActivityCssClass() {
            long total = getTotalChanges();
            if (total > 1_000_000) return "bg-danger";
            if (total > 100_000) return "bg-warning text-dark";
            if (total > 10_000) return "bg-info";
            return "bg-secondary";
        }
    }

    /**
     * Data transfer object representing a table with high data modification rates (high churn).
     * <p>
     * Contains statistics about tables experiencing significant write activity, including
     * the total number of changes, individual operation counts (inserts, updates, deletes),
     * and a churn ratio that indicates the rate of change relative to the number of live tuples.
     * A high churn ratio suggests the table's data is frequently modified relative to its size.
     *
     * @see #getHighChurnTables(String, int)
     */
    public static class HighChurnTable {
        private String schemaName;
        private String tableName;
        private long totalChanges;
        private long inserts;
        private long updates;
        private long deletes;
        private long liveTuples;
        private double churnRatio;
        private String tableSize;

        public String getSchemaName() { return schemaName; }
        public void setSchemaName(String name) { this.schemaName = name; }

        public String getTableName() { return tableName; }
        public void setTableName(String name) { this.tableName = name; }

        public String getFullName() { return schemaName + "." + tableName; }

        public long getTotalChanges() { return totalChanges; }
        public void setTotalChanges(long changes) { this.totalChanges = changes; }

        public long getInserts() { return inserts; }
        public void setInserts(long inserts) { this.inserts = inserts; }

        public long getUpdates() { return updates; }
        public void setUpdates(long updates) { this.updates = updates; }

        public long getDeletes() { return deletes; }
        public void setDeletes(long deletes) { this.deletes = deletes; }

        public long getLiveTuples() { return liveTuples; }
        public void setLiveTuples(long tuples) { this.liveTuples = tuples; }

        public double getChurnRatio() { return churnRatio; }
        public void setChurnRatio(double ratio) { this.churnRatio = ratio; }

        public String getChurnRatioFormatted() {
            return String.format("%.2fx", churnRatio);
        }

        public String getTableSize() { return tableSize; }
        public void setTableSize(String size) { this.tableSize = size; }

        public String getTotalChangesFormatted() {
            if (totalChanges >= 1_000_000_000) return String.format("%.1fB", totalChanges / 1_000_000_000.0);
            if (totalChanges >= 1_000_000) return String.format("%.1fM", totalChanges / 1_000_000.0);
            if (totalChanges >= 1_000) return String.format("%.1fK", totalChanges / 1_000.0);
            return String.valueOf(totalChanges);
        }
    }

    /**
     * Data transfer object containing estimated Write-Ahead Log (WAL) generation statistics for a table.
     * <p>
     * Provides an approximation of how much WAL data a table has generated based on its change
     * activity and average row size. The estimate is calculated by multiplying total changes by
     * average row size and applying an overhead factor. This helps identify tables that contribute
     * significantly to WAL volume and may impact replication performance or archival storage.
     *
     * @see #getWalGenerationByTable(String)
     */
    public static class TableWalEstimate {
        private String schemaName;
        private String tableName;
        private long totalChanges;
        private long inserts;
        private long updates;
        private long deletes;
        private long avgRowSize;
        private long estimatedWalBytes;

        public String getSchemaName() { return schemaName; }
        public void setSchemaName(String name) { this.schemaName = name; }

        public String getTableName() { return tableName; }
        public void setTableName(String name) { this.tableName = name; }

        public String getFullName() { return schemaName + "." + tableName; }

        public long getTotalChanges() { return totalChanges; }
        public void setTotalChanges(long changes) { this.totalChanges = changes; }

        public long getInserts() { return inserts; }
        public void setInserts(long inserts) { this.inserts = inserts; }

        public long getUpdates() { return updates; }
        public void setUpdates(long updates) { this.updates = updates; }

        public long getDeletes() { return deletes; }
        public void setDeletes(long deletes) { this.deletes = deletes; }

        public long getAvgRowSize() { return avgRowSize; }
        public void setAvgRowSize(long size) { this.avgRowSize = size; }

        public long getEstimatedWalBytes() { return estimatedWalBytes; }
        public void setEstimatedWalBytes(long bytes) { this.estimatedWalBytes = bytes; }

        public String getEstimatedWalFormatted() {
            if (estimatedWalBytes >= 1024L * 1024 * 1024) {
                return String.format("%.1f GB", estimatedWalBytes / (1024.0 * 1024 * 1024));
            }
            if (estimatedWalBytes >= 1024 * 1024) {
                return String.format("%.1f MB", estimatedWalBytes / (1024.0 * 1024));
            }
            if (estimatedWalBytes >= 1024) {
                return String.format("%.1f KB", estimatedWalBytes / 1024.0);
            }
            return estimatedWalBytes + " B";
        }
    }

    /**
     * Data transfer object containing aggregate Change Data Capture statistics for a database instance.
     * <p>
     * Provides database-wide summary statistics including total insert, update, delete, and HOT update
     * counts across all user tables. Also tracks tuple counts (live and dead) and identifies tables
     * with high activity levels. This summary is useful for monitoring overall database write workload
     * and identifying potential maintenance requirements.
     *
     * @see #getSummary(String)
     */
    public static class CdcSummary {
        private long totalInserts;
        private long totalUpdates;
        private long totalDeletes;
        private long totalHotUpdates;
        private long totalLiveTuples;
        private long totalDeadTuples;
        private int tableCount;
        private int highActivityTables;

        public long getTotalInserts() { return totalInserts; }
        public void setTotalInserts(long count) { this.totalInserts = count; }

        public long getTotalUpdates() { return totalUpdates; }
        public void setTotalUpdates(long count) { this.totalUpdates = count; }

        public long getTotalDeletes() { return totalDeletes; }
        public void setTotalDeletes(long count) { this.totalDeletes = count; }

        public long getTotalHotUpdates() { return totalHotUpdates; }
        public void setTotalHotUpdates(long count) { this.totalHotUpdates = count; }

        public long getTotalChanges() { return totalInserts + totalUpdates + totalDeletes; }

        public long getTotalLiveTuples() { return totalLiveTuples; }
        public void setTotalLiveTuples(long count) { this.totalLiveTuples = count; }

        public long getTotalDeadTuples() { return totalDeadTuples; }
        public void setTotalDeadTuples(long count) { this.totalDeadTuples = count; }

        public int getTableCount() { return tableCount; }
        public void setTableCount(int count) { this.tableCount = count; }

        public int getHighActivityTables() { return highActivityTables; }
        public void setHighActivityTables(int count) { this.highActivityTables = count; }

        public String formatCount(long count) {
            if (count >= 1_000_000_000) return String.format("%.1fB", count / 1_000_000_000.0);
            if (count >= 1_000_000) return String.format("%.1fM", count / 1_000_000.0);
            if (count >= 1_000) return String.format("%.1fK", count / 1_000.0);
            return String.valueOf(count);
        }
    }
}
