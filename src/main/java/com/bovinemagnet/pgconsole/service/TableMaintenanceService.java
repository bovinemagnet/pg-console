package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.TableMaintenanceRecommendation;
import com.bovinemagnet.pgconsole.model.TableMaintenanceRecommendation.MaintenanceType;
import com.bovinemagnet.pgconsole.model.TableMaintenanceRecommendation.Severity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for analysing table health and providing maintenance recommendations.
 * <p>
 * Monitors PostgreSQL table statistics to identify maintenance requirements including:
 * <ul>
 *   <li>Tables with excessive dead tuples requiring VACUUM</li>
 *   <li>Tables with outdated statistics requiring ANALYSE</li>
 *   <li>Bloated tables that may benefit from VACUUM FULL</li>
 * </ul>
 * <p>
 * Recommendations are prioritised by severity (CRITICAL, HIGH, MEDIUM, LOW) based
 * on configurable thresholds for dead tuple ratios, table sizes, and staleness of
 * statistics. Only tables exceeding minimum size thresholds are flagged to avoid
 * noise from trivial maintenance needs.
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see TableMaintenanceRecommendation
 * @see MaintenanceSummary
 */
@ApplicationScoped
public class TableMaintenanceService {

    private static final Logger LOG = Logger.getLogger(TableMaintenanceService.class);

    // Thresholds for recommendations
    private static final double CRITICAL_DEAD_TUPLE_RATIO = 0.3;  // 30% dead tuples
    private static final double HIGH_DEAD_TUPLE_RATIO = 0.2;      // 20% dead tuples
    private static final double MEDIUM_DEAD_TUPLE_RATIO = 0.1;    // 10% dead tuples
    private static final long CRITICAL_DEAD_TUPLES = 1_000_000;   // 1M dead tuples
    private static final int OVERDUE_VACUUM_DAYS = 7;             // 7 days without vacuum
    private static final int OVERDUE_ANALYSE_DAYS = 7;            // 7 days without analyse
    private static final double HIGH_BLOAT_THRESHOLD = 50.0;      // 50% bloat
    private static final double MEDIUM_BLOAT_THRESHOLD = 25.0;    // 25% bloat
    private static final long MIN_TABLE_SIZE = 1_000_000;         // Only flag tables > 1MB

    @Inject
    DataSourceManager dataSourceManager;

    /**
     * Retrieves all maintenance recommendations for a database instance.
     * <p>
     * Aggregates recommendations from vacuum, analyse, and bloat analysis to provide
     * a comprehensive view of table maintenance needs. Results are sorted by severity
     * (CRITICAL first) and then by dead tuple ratio to prioritise the most urgent
     * maintenance tasks.
     *
     * @param instanceName the database instance identifier
     * @return list of maintenance recommendations sorted by priority. Returns an empty
     *         list if no maintenance is needed or if an error occurs.
     * @see #findTablesNeedingVacuum(String)
     * @see #findTablesNeedingAnalyse(String)
     * @see #findBloatedTables(String)
     */
    public List<TableMaintenanceRecommendation> getRecommendations(String instanceName) {
        List<TableMaintenanceRecommendation> recommendations = new ArrayList<>();

        recommendations.addAll(findTablesNeedingVacuum(instanceName));
        recommendations.addAll(findTablesNeedingAnalyse(instanceName));
        recommendations.addAll(findBloatedTables(instanceName));

        // Sort by severity (CRITICAL first), then by dead tuple ratio
        recommendations.sort((a, b) -> {
            int severityCompare = a.getSeverity().compareTo(b.getSeverity());
            if (severityCompare != 0) return severityCompare;
            return Double.compare(b.getDeadTupleRatio(), a.getDeadTupleRatio());
        });

        return recommendations;
    }

    /**
     * Identifies tables with excessive dead tuples requiring VACUUM.
     * <p>
     * Analyses {@code pg_stat_user_tables} to find tables where dead tuple ratios
     * or absolute dead tuple counts exceed configured thresholds:
     * <ul>
     *   <li>CRITICAL: ≥{@value #CRITICAL_DEAD_TUPLE_RATIO} ratio or ≥{@value #CRITICAL_DEAD_TUPLES} dead tuples</li>
     *   <li>HIGH: ≥{@value #HIGH_DEAD_TUPLE_RATIO} ratio</li>
     *   <li>MEDIUM: ≥{@value #MEDIUM_DEAD_TUPLE_RATIO} ratio</li>
     * </ul>
     * <p>
     * Only tables larger than {@value #MIN_TABLE_SIZE} bytes are considered unless
     * they exceed the critical dead tuple count threshold.
     *
     * @param instanceName the database instance identifier
     * @return list of recommendations for tables needing VACUUM, limited to top 100
     *         candidates. Returns an empty list if an error occurs.
     */
    public List<TableMaintenanceRecommendation> findTablesNeedingVacuum(String instanceName) {
        List<TableMaintenanceRecommendation> recommendations = new ArrayList<>();

        String sql = """
            SELECT
                schemaname,
                relname as tablename,
                n_live_tup,
                n_dead_tup,
                CASE WHEN n_live_tup + n_dead_tup > 0
                    THEN n_dead_tup::float / (n_live_tup + n_dead_tup)
                    ELSE 0
                END as dead_tuple_ratio,
                pg_size_pretty(pg_table_size(schemaname || '.' || relname)) as table_size,
                pg_table_size(schemaname || '.' || relname) as table_size_bytes,
                last_vacuum,
                last_autovacuum,
                last_analyze,
                last_autoanalyze
            FROM pg_stat_user_tables
            WHERE schemaname NOT IN ('pg_catalog', 'information_schema', 'pgconsole')
              AND n_live_tup + n_dead_tup > 100
            ORDER BY dead_tuple_ratio DESC, n_dead_tup DESC
            LIMIT 100
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                double deadTupleRatio = rs.getDouble("dead_tuple_ratio");
                long deadTuples = rs.getLong("n_dead_tup");
                long tableSizeBytes = rs.getLong("table_size_bytes");

                // Skip if dead tuple ratio is below threshold
                if (deadTupleRatio < MEDIUM_DEAD_TUPLE_RATIO && deadTuples < CRITICAL_DEAD_TUPLES) {
                    continue;
                }

                // Skip small tables unless they have critical dead tuple counts
                if (tableSizeBytes < MIN_TABLE_SIZE && deadTuples < CRITICAL_DEAD_TUPLES) {
                    continue;
                }

                Severity severity;
                if (deadTupleRatio >= CRITICAL_DEAD_TUPLE_RATIO || deadTuples >= CRITICAL_DEAD_TUPLES) {
                    severity = Severity.CRITICAL;
                } else if (deadTupleRatio >= HIGH_DEAD_TUPLE_RATIO) {
                    severity = Severity.HIGH;
                } else {
                    severity = Severity.MEDIUM;
                }

                TableMaintenanceRecommendation rec = new TableMaintenanceRecommendation();
                rec.setSchemaName(rs.getString("schemaname"));
                rec.setTableName(rs.getString("tablename"));
                rec.setType(MaintenanceType.VACUUM);
                rec.setSeverity(severity);

                rec.setLiveTuples(rs.getLong("n_live_tup"));
                rec.setDeadTuples(deadTuples);
                rec.setDeadTupleRatio(deadTupleRatio);
                rec.setTableSize(rs.getString("table_size"));
                rec.setTableSizeBytes(tableSizeBytes);

                setVacuumAnalyseTimestamps(rs, rec);

                rec.setRecommendation("Run VACUUM on this table");
                rec.setRationale(String.format(
                    "%.1f%% dead tuples (%,d dead of %,d total). Table size: %s.",
                    deadTupleRatio * 100, deadTuples, rec.getLiveTuples() + deadTuples, rec.getTableSize()));

                recommendations.add(rec);
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to find tables needing vacuum for %s: %s", instanceName, e.getMessage());
        }

        return recommendations;
    }

    /**
     * Identifies tables with outdated statistics requiring ANALYSE.
     * <p>
     * Examines {@code pg_stat_user_tables} to find tables where statistics are stale
     * based on last analyse timestamp and modification counts. Severity is assigned as:
     * <ul>
     *   <li>HIGH: Never analysed, or &gt;{@value #OVERDUE_ANALYSE_DAYS}×3 days since last analyse</li>
     *   <li>MEDIUM: &gt;{@value #OVERDUE_ANALYSE_DAYS} days since last analyse</li>
     *   <li>LOW: Recently analysed but with significant modifications</li>
     * </ul>
     * <p>
     * Only tables larger than {@value #MIN_TABLE_SIZE} bytes with &gt;1000 live tuples
     * are considered to focus on impactful recommendations.
     *
     * @param instanceName the database instance identifier
     * @return list of recommendations for tables needing ANALYSE, limited to top 50
     *         candidates. Returns an empty list if an error occurs.
     */
    public List<TableMaintenanceRecommendation> findTablesNeedingAnalyse(String instanceName) {
        List<TableMaintenanceRecommendation> recommendations = new ArrayList<>();

        String sql = """
            SELECT
                schemaname,
                relname as tablename,
                n_live_tup,
                n_dead_tup,
                pg_size_pretty(pg_table_size(schemaname || '.' || relname)) as table_size,
                pg_table_size(schemaname || '.' || relname) as table_size_bytes,
                last_vacuum,
                last_autovacuum,
                last_analyze,
                last_autoanalyze,
                n_mod_since_analyze
            FROM pg_stat_user_tables
            WHERE schemaname NOT IN ('pg_catalog', 'information_schema', 'pgconsole')
              AND n_live_tup > 1000
              AND pg_table_size(schemaname || '.' || relname) >= ?
            ORDER BY
                CASE
                    WHEN last_analyze IS NULL AND last_autoanalyze IS NULL THEN 0
                    ELSE 1
                END,
                GREATEST(COALESCE(last_analyze, '1970-01-01'::timestamp),
                         COALESCE(last_autoanalyze, '1970-01-01'::timestamp)) ASC
            LIMIT 50
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, MIN_TABLE_SIZE);

            try (ResultSet rs = stmt.executeQuery()) {
                Instant now = Instant.now();

                while (rs.next()) {
                    Timestamp lastAnalyse = rs.getTimestamp("last_analyze");
                    Timestamp lastAutoAnalyse = rs.getTimestamp("last_autoanalyze");
                    long modsSinceAnalyse = rs.getLong("n_mod_since_analyze");

                    // Determine effective last analyse
                    Instant effectiveLastAnalyse = null;
                    if (lastAnalyse != null) {
                        effectiveLastAnalyse = lastAnalyse.toInstant();
                    }
                    if (lastAutoAnalyse != null) {
                        Instant autoAnalyseInstant = lastAutoAnalyse.toInstant();
                        if (effectiveLastAnalyse == null || autoAnalyseInstant.isAfter(effectiveLastAnalyse)) {
                            effectiveLastAnalyse = autoAnalyseInstant;
                        }
                    }

                    // Skip if recently analysed and low modifications
                    if (effectiveLastAnalyse != null) {
                        long daysSinceAnalyse = ChronoUnit.DAYS.between(effectiveLastAnalyse, now);
                        long liveTuples = rs.getLong("n_live_tup");
                        double modRatio = liveTuples > 0 ? (double) modsSinceAnalyse / liveTuples : 0;

                        if (daysSinceAnalyse < OVERDUE_ANALYSE_DAYS && modRatio < 0.1) {
                            continue;
                        }
                    }

                    Severity severity;
                    if (effectiveLastAnalyse == null) {
                        severity = Severity.HIGH;
                    } else {
                        long daysSinceAnalyse = ChronoUnit.DAYS.between(effectiveLastAnalyse, now);
                        if (daysSinceAnalyse > OVERDUE_ANALYSE_DAYS * 3) {
                            severity = Severity.HIGH;
                        } else if (daysSinceAnalyse > OVERDUE_ANALYSE_DAYS) {
                            severity = Severity.MEDIUM;
                        } else {
                            severity = Severity.LOW;
                        }
                    }

                    TableMaintenanceRecommendation rec = new TableMaintenanceRecommendation();
                    rec.setSchemaName(rs.getString("schemaname"));
                    rec.setTableName(rs.getString("tablename"));
                    rec.setType(MaintenanceType.ANALYSE);
                    rec.setSeverity(severity);

                    rec.setLiveTuples(rs.getLong("n_live_tup"));
                    rec.setDeadTuples(rs.getLong("n_dead_tup"));
                    rec.setTableSize(rs.getString("table_size"));
                    rec.setTableSizeBytes(rs.getLong("table_size_bytes"));

                    setVacuumAnalyseTimestamps(rs, rec);

                    rec.setRecommendation("Run ANALYSE on this table");

                    if (effectiveLastAnalyse == null) {
                        rec.setRationale("Table has never been analysed. Statistics may be outdated.");
                    } else {
                        long daysSince = ChronoUnit.DAYS.between(effectiveLastAnalyse, now);
                        rec.setRationale(String.format(
                            "Last analysed %d days ago. %,d modifications since then.",
                            daysSince, modsSinceAnalyse));
                    }

                    recommendations.add(rec);
                }
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to find tables needing analyse for %s: %s", instanceName, e.getMessage());
        }

        return recommendations;
    }

    /**
     * Identifies tables with significant bloat that may benefit from VACUUM FULL.
     * <p>
     * Uses a simple bloat estimation based on the ratio of dead tuples to live tuples
     * as a proxy for wasted space that regular VACUUM cannot reclaim. Severity levels:
     * <ul>
     *   <li>HIGH: ≥{@value #HIGH_BLOAT_THRESHOLD}% estimated bloat</li>
     *   <li>MEDIUM: ≥{@value #MEDIUM_BLOAT_THRESHOLD}% estimated bloat</li>
     * </ul>
     * <p>
     * Only tables larger than {@value #MIN_TABLE_SIZE}×10 bytes (10MB) with &gt;10,000
     * live tuples are considered, as VACUUM FULL requires an exclusive lock and significant
     * I/O overhead.
     * <p>
     * <strong>Note:</strong> This is a simplified estimate. For production environments,
     * consider using {@code pgstattuple} or similar tools for accurate bloat measurement.
     *
     * @param instanceName the database instance identifier
     * @return list of recommendations for bloated tables, limited to top 20 candidates.
     *         Returns an empty list if an error occurs.
     */
    public List<TableMaintenanceRecommendation> findBloatedTables(String instanceName) {
        List<TableMaintenanceRecommendation> recommendations = new ArrayList<>();

        // Simple bloat estimate based on dead tuples vs expected size
        String sql = """
            SELECT
                schemaname,
                relname as tablename,
                n_live_tup,
                n_dead_tup,
                pg_size_pretty(pg_table_size(schemaname || '.' || relname)) as table_size,
                pg_table_size(schemaname || '.' || relname) as table_size_bytes,
                last_vacuum,
                last_autovacuum,
                last_analyze,
                last_autoanalyze,
                CASE WHEN n_live_tup > 0
                    THEN (n_dead_tup::float / n_live_tup) * 100
                    ELSE 0
                END as estimated_bloat_percent
            FROM pg_stat_user_tables
            WHERE schemaname NOT IN ('pg_catalog', 'information_schema', 'pgconsole')
              AND n_live_tup > 10000
              AND pg_table_size(schemaname || '.' || relname) >= ?
              AND (n_dead_tup::float / NULLIF(n_live_tup, 0)) > 0.25
            ORDER BY estimated_bloat_percent DESC
            LIMIT 20
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, MIN_TABLE_SIZE * 10); // Only flag tables > 10MB for VACUUM FULL

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    double bloatPercent = rs.getDouble("estimated_bloat_percent");

                    if (bloatPercent < MEDIUM_BLOAT_THRESHOLD) {
                        continue;
                    }

                    Severity severity;
                    if (bloatPercent >= HIGH_BLOAT_THRESHOLD) {
                        severity = Severity.HIGH;
                    } else {
                        severity = Severity.MEDIUM;
                    }

                    TableMaintenanceRecommendation rec = new TableMaintenanceRecommendation();
                    rec.setSchemaName(rs.getString("schemaname"));
                    rec.setTableName(rs.getString("tablename"));
                    rec.setType(MaintenanceType.VACUUM_FULL);
                    rec.setSeverity(severity);

                    rec.setLiveTuples(rs.getLong("n_live_tup"));
                    rec.setDeadTuples(rs.getLong("n_dead_tup"));
                    rec.setTableSize(rs.getString("table_size"));
                    rec.setTableSizeBytes(rs.getLong("table_size_bytes"));
                    rec.setEstimatedBloatPercent(bloatPercent);

                    // Estimate bloat size
                    long tableSizeBytes = rs.getLong("table_size_bytes");
                    long estimatedBloatBytes = (long) (tableSizeBytes * (bloatPercent / (100 + bloatPercent)));
                    rec.setEstimatedBloatSize(formatBytes(estimatedBloatBytes));

                    setVacuumAnalyseTimestamps(rs, rec);

                    rec.setRecommendation("Consider VACUUM FULL (requires exclusive lock)");
                    rec.setRationale(String.format(
                        "Estimated %.0f%% bloat (~%s). Regular VACUUM cannot reclaim this space.",
                        bloatPercent, rec.getEstimatedBloatSize()));

                    recommendations.add(rec);
                }
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to find bloated tables for %s: %s", instanceName, e.getMessage());
        }

        return recommendations;
    }

    /**
     * Generates summary statistics for the maintenance dashboard.
     * <p>
     * Provides aggregate counts of tables requiring various maintenance actions
     * including vacuum needs, analyse requirements, and overall table health metrics.
     *
     * @param instanceName the database instance identifier
     * @return summary object containing maintenance overview statistics
     * @see MaintenanceSummary
     */
    public MaintenanceSummary getSummary(String instanceName) {
        MaintenanceSummary summary = new MaintenanceSummary();

        String sql = """
            SELECT
                COUNT(*) FILTER (WHERE n_dead_tup::float / NULLIF(n_live_tup + n_dead_tup, 0) > 0.1) as tables_needing_vacuum,
                COUNT(*) FILTER (WHERE last_analyze IS NULL AND last_autoanalyze IS NULL) as never_analysed,
                COUNT(*) FILTER (WHERE
                    GREATEST(COALESCE(last_analyze, '1970-01-01'::timestamp),
                             COALESCE(last_autoanalyze, '1970-01-01'::timestamp))
                    < NOW() - INTERVAL '7 days'
                ) as analyse_overdue,
                COUNT(*) as total_tables,
                COALESCE(SUM(n_dead_tup), 0) as total_dead_tuples
            FROM pg_stat_user_tables
            WHERE schemaname NOT IN ('pg_catalog', 'information_schema', 'pgconsole')
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                summary.setTablesNeedingVacuum(rs.getInt("tables_needing_vacuum"));
                summary.setTablesNeverAnalysed(rs.getInt("never_analysed"));
                summary.setTablesAnalyseOverdue(rs.getInt("analyse_overdue"));
                summary.setTotalTables(rs.getInt("total_tables"));
                summary.setTotalDeadTuples(rs.getLong("total_dead_tuples"));
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to get maintenance summary for %s: %s", instanceName, e.getMessage());
        }

        return summary;
    }

    private void setVacuumAnalyseTimestamps(ResultSet rs, TableMaintenanceRecommendation rec) throws SQLException {
        Timestamp lastVacuum = rs.getTimestamp("last_vacuum");
        Timestamp lastAutoVacuum = rs.getTimestamp("last_autovacuum");
        Timestamp lastAnalyse = rs.getTimestamp("last_analyze");
        Timestamp lastAutoAnalyse = rs.getTimestamp("last_autoanalyze");

        if (lastVacuum != null) rec.setLastVacuum(lastVacuum.toInstant());
        if (lastAutoVacuum != null) rec.setLastAutoVacuum(lastAutoVacuum.toInstant());
        if (lastAnalyse != null) rec.setLastAnalyse(lastAnalyse.toInstant());
        if (lastAutoAnalyse != null) rec.setLastAutoAnalyse(lastAutoAnalyse.toInstant());
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " bytes";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * Aggregated summary statistics for table maintenance status.
     * <p>
     * Provides dashboard-level metrics including counts of tables requiring
     * vacuum, tables with outdated statistics, and total dead tuple accumulation
     * across all tables.
     */
    public static class MaintenanceSummary {
        private int tablesNeedingVacuum;
        private int tablesNeverAnalysed;
        private int tablesAnalyseOverdue;
        private int totalTables;
        private long totalDeadTuples;

        public int getTablesNeedingVacuum() {
            return tablesNeedingVacuum;
        }

        public void setTablesNeedingVacuum(int tablesNeedingVacuum) {
            this.tablesNeedingVacuum = tablesNeedingVacuum;
        }

        public int getTablesNeverAnalysed() {
            return tablesNeverAnalysed;
        }

        public void setTablesNeverAnalysed(int tablesNeverAnalysed) {
            this.tablesNeverAnalysed = tablesNeverAnalysed;
        }

        public int getTablesAnalyseOverdue() {
            return tablesAnalyseOverdue;
        }

        public void setTablesAnalyseOverdue(int tablesAnalyseOverdue) {
            this.tablesAnalyseOverdue = tablesAnalyseOverdue;
        }

        public int getTotalTables() {
            return totalTables;
        }

        public void setTotalTables(int totalTables) {
            this.totalTables = totalTables;
        }

        public long getTotalDeadTuples() {
            return totalDeadTuples;
        }

        public void setTotalDeadTuples(long totalDeadTuples) {
            this.totalDeadTuples = totalDeadTuples;
        }

        public String getTotalDeadTuplesFormatted() {
            if (totalDeadTuples >= 1_000_000_000) {
                return String.format("%.1fB", totalDeadTuples / 1_000_000_000.0);
            } else if (totalDeadTuples >= 1_000_000) {
                return String.format("%.1fM", totalDeadTuples / 1_000_000.0);
            } else if (totalDeadTuples >= 1_000) {
                return String.format("%.1fK", totalDeadTuples / 1_000.0);
            }
            return String.valueOf(totalDeadTuples);
        }

        public boolean hasIssues() {
            return tablesNeedingVacuum > 0 || tablesNeverAnalysed > 0;
        }
    }
}
