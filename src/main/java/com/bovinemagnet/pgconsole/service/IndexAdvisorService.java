package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.IndexRecommendation;
import com.bovinemagnet.pgconsole.model.IndexRecommendation.RecommendationType;
import com.bovinemagnet.pgconsole.model.IndexRecommendation.Severity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for analysing indexes and providing recommendations.
 * Identifies missing indexes, unused indexes, and duplicates.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class IndexAdvisorService {

    private static final Logger LOG = Logger.getLogger(IndexAdvisorService.class);

    // Thresholds for recommendations
    private static final double HIGH_SEQ_SCAN_RATIO = 0.9;   // 90% sequential scans
    private static final double MEDIUM_SEQ_SCAN_RATIO = 0.7; // 70% sequential scans
    private static final long MIN_TABLE_ROWS = 1000;         // Only consider tables with significant rows
    private static final long MIN_SEQ_SCANS = 100;           // Minimum scans to be significant

    @Inject
    DataSourceManager dataSourceManager;

    /**
     * Retrieves all index recommendations for the specified database instance.
     * <p>
     * Analyses the instance for missing indexes, unused indexes, and duplicate indexes,
     * then consolidates and sorts recommendations by severity (HIGH, MEDIUM, LOW) and type.
     *
     * @param instanceName the database instance identifier
     * @return list of all index recommendations sorted by severity (HIGH first) then by type
     */
    public List<IndexRecommendation> getRecommendations(String instanceName) {
        List<IndexRecommendation> recommendations = new ArrayList<>();

        recommendations.addAll(findTablesNeedingIndexes(instanceName));
        recommendations.addAll(findUnusedIndexes(instanceName));
        recommendations.addAll(findDuplicateIndexes(instanceName));

        // Sort by severity (HIGH first)
        recommendations.sort((a, b) -> {
            int severityCompare = a.getSeverity().compareTo(b.getSeverity());
            if (severityCompare != 0) return severityCompare;
            return a.getType().compareTo(b.getType());
        });

        return recommendations;
    }

    /**
     * Identifies tables with high sequential scan ratios that may benefit from indexes.
     * <p>
     * Queries {@code pg_stat_user_tables} to find tables where sequential scans dominate
     * over index scans. Only considers tables with significant row counts (over 1000 rows)
     * and scan activity (over 100 sequential scans) to avoid false positives on small or
     * infrequently accessed tables.
     * <p>
     * Severity is HIGH if sequential scans exceed 90% of total scans, MEDIUM if over 70%.
     * System schemas (pg_catalog, information_schema, pgconsole) are excluded.
     *
     * @param instanceName the database instance identifier
     * @return list of tables recommended for indexing, sorted by sequential scan ratio descending
     */
    public List<IndexRecommendation> findTablesNeedingIndexes(String instanceName) {
        List<IndexRecommendation> recommendations = new ArrayList<>();

        String sql = """
            SELECT
                schemaname,
                relname as tablename,
                seq_scan,
                idx_scan,
                CASE WHEN seq_scan + idx_scan > 0
                    THEN seq_scan::float / (seq_scan + idx_scan)
                    ELSE 0
                END as seq_scan_ratio,
                n_live_tup as row_count,
                n_dead_tup as dead_tuples,
                pg_size_pretty(pg_table_size(schemaname || '.' || relname)) as table_size
            FROM pg_stat_user_tables
            WHERE seq_scan > ?
              AND n_live_tup > ?
              AND schemaname NOT IN ('pg_catalog', 'information_schema', 'pgconsole')
            ORDER BY seq_scan_ratio DESC, seq_scan DESC
            LIMIT 50
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, MIN_SEQ_SCANS);
            stmt.setLong(2, MIN_TABLE_ROWS);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    double seqScanRatio = rs.getDouble("seq_scan_ratio");

                    // Only recommend if ratio is significant
                    if (seqScanRatio < MEDIUM_SEQ_SCAN_RATIO) {
                        continue;
                    }

                    Severity severity = seqScanRatio >= HIGH_SEQ_SCAN_RATIO ? Severity.HIGH : Severity.MEDIUM;

                    IndexRecommendation rec = new IndexRecommendation(RecommendationType.MISSING_INDEX, severity);
                    rec.setSchemaName(rs.getString("schemaname"));
                    rec.setTableName(rs.getString("tablename"));
                    rec.setSeqScans(rs.getLong("seq_scan"));
                    rec.setIdxScans(rs.getLong("idx_scan"));
                    rec.setSeqScanRatio(seqScanRatio * 100);
                    rec.setTableRows(rs.getLong("row_count"));
                    rec.setDeadTuples(rs.getLong("dead_tuples"));
                    rec.setTableSize(rs.getString("table_size"));

                    rec.setRecommendation("Consider adding an index to this table");
                    rec.setRationale(String.format(
                        "%.1f%% of scans are sequential (%d seq scans vs %d index scans). " +
                        "Table has %d rows.",
                        seqScanRatio * 100, rec.getSeqScans(), rec.getIdxScans(), rec.getTableRows()));
                    rec.setSuggestedAction(
                        "Analyse query patterns using pg_stat_statements to identify columns " +
                        "frequently used in WHERE clauses, then create appropriate indexes.");

                    recommendations.add(rec);
                }
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to find tables needing indexes for %s: %s", instanceName, e.getMessage());
        }

        return recommendations;
    }

    /**
     * Identifies indexes that have never been used since statistics were last reset.
     * <p>
     * Queries {@code pg_stat_user_indexes} to find indexes with zero index scans. Primary key
     * and unique constraint indexes are excluded as they serve data integrity purposes beyond
     * query optimisation. System schemas are also excluded.
     * <p>
     * Severity is MEDIUM for indexes larger than 10 MB, LOW for indexes between 1 MB and 10 MB,
     * and LOW for smaller indexes. Larger unused indexes represent more significant waste of
     * storage and write performance.
     *
     * @param instanceName the database instance identifier
     * @return list of unused indexes ordered by size descending
     */
    public List<IndexRecommendation> findUnusedIndexes(String instanceName) {
        List<IndexRecommendation> recommendations = new ArrayList<>();

        String sql = """
            SELECT
                s.schemaname,
                s.relname as tablename,
                s.indexrelname as indexname,
                s.idx_scan,
                pg_size_pretty(pg_relation_size(s.indexrelid)) as index_size,
                pg_relation_size(s.indexrelid) as index_size_bytes,
                i.indisunique,
                i.indisprimary
            FROM pg_stat_user_indexes s
            JOIN pg_index i ON s.indexrelid = i.indexrelid
            WHERE s.idx_scan = 0
              AND s.schemaname NOT IN ('pg_catalog', 'information_schema', 'pgconsole')
              AND NOT i.indisprimary
              AND NOT i.indisunique
            ORDER BY pg_relation_size(s.indexrelid) DESC
            LIMIT 50
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                long indexSizeBytes = rs.getLong("index_size_bytes");

                // Only flag indexes over 1MB as significant
                Severity severity = indexSizeBytes > 10_000_000 ? Severity.MEDIUM :
                                   indexSizeBytes > 1_000_000 ? Severity.LOW : Severity.LOW;

                IndexRecommendation rec = new IndexRecommendation(RecommendationType.UNUSED_INDEX, severity);
                rec.setSchemaName(rs.getString("schemaname"));
                rec.setTableName(rs.getString("tablename"));
                rec.setIndexName(rs.getString("indexname"));
                rec.setIdxScans(rs.getLong("idx_scan"));
                rec.setIndexSize(rs.getString("index_size"));

                rec.setRecommendation("Consider dropping this unused index");
                rec.setRationale(String.format(
                    "Index '%s' has never been used since statistics were reset. " +
                    "It consumes %s of storage.",
                    rec.getIndexName(), rec.getIndexSize()));
                rec.setSuggestedAction(
                    "Verify the index is not used by recent queries before dropping. " +
                    "Consider waiting for a full business cycle to confirm.");

                recommendations.add(rec);
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to find unused indexes for %s: %s", instanceName, e.getMessage());
        }

        return recommendations;
    }

    /**
     * Identifies potentially redundant indexes where one index is a prefix of another.
     * <p>
     * Detects indexes on the same table where one index's column list is a prefix of another
     * index's column list. For example, if index A covers columns (a, b) and index B covers
     * columns (a, b, c), then index A may be redundant as index B can serve the same queries.
     * <p>
     * Note that this analysis is conservative and may flag indexes that are intentionally
     * separate for query performance reasons. Verification is recommended before dropping.
     * All duplicate index recommendations are assigned LOW severity.
     *
     * @param instanceName the database instance identifier
     * @return list of potentially redundant indexes ordered by size descending
     */
    public List<IndexRecommendation> findDuplicateIndexes(String instanceName) {
        List<IndexRecommendation> recommendations = new ArrayList<>();

        // This query finds indexes where the column list is a prefix of another index
        String sql = """
            WITH index_cols AS (
                SELECT
                    n.nspname as schemaname,
                    t.relname as tablename,
                    i.relname as indexname,
                    pg_get_indexdef(i.oid) as indexdef,
                    array_to_string(array_agg(a.attname ORDER BY array_position(ix.indkey, a.attnum)), ',') as columns,
                    pg_relation_size(i.oid) as index_size,
                    pg_size_pretty(pg_relation_size(i.oid)) as index_size_pretty
                FROM pg_index ix
                JOIN pg_class i ON i.oid = ix.indexrelid
                JOIN pg_class t ON t.oid = ix.indrelid
                JOIN pg_namespace n ON n.oid = t.relnamespace
                JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = ANY(ix.indkey)
                WHERE n.nspname NOT IN ('pg_catalog', 'information_schema', 'pgconsole')
                GROUP BY n.nspname, t.relname, i.relname, i.oid, ix.indkey
            )
            SELECT
                a.schemaname,
                a.tablename,
                a.indexname as duplicate_index,
                a.columns as duplicate_columns,
                a.index_size_pretty as duplicate_size,
                b.indexname as covering_index,
                b.columns as covering_columns
            FROM index_cols a
            JOIN index_cols b ON a.schemaname = b.schemaname
                AND a.tablename = b.tablename
                AND a.indexname != b.indexname
                AND b.columns LIKE a.columns || '%'
            ORDER BY a.index_size DESC
            LIMIT 30
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                IndexRecommendation rec = new IndexRecommendation(RecommendationType.DUPLICATE_INDEX, Severity.LOW);
                rec.setSchemaName(rs.getString("schemaname"));
                rec.setTableName(rs.getString("tablename"));
                rec.setIndexName(rs.getString("duplicate_index"));
                rec.setIndexSize(rs.getString("duplicate_size"));

                String duplicateCols = rs.getString("duplicate_columns");
                String coveringIndex = rs.getString("covering_index");
                String coveringCols = rs.getString("covering_columns");

                rec.setRecommendation("This index may be redundant");
                rec.setRationale(String.format(
                    "Index '%s' on columns (%s) appears to be covered by index '%s' on columns (%s).",
                    rec.getIndexName(), duplicateCols, coveringIndex, coveringCols));
                rec.setSuggestedAction(
                    "Verify that the covering index can serve all queries using this index, " +
                    "then consider dropping the duplicate to save storage and write overhead.");

                recommendations.add(rec);
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to find duplicate indexes for %s: %s", instanceName, e.getMessage());
        }

        return recommendations;
    }

    /**
     * Retrieves summary statistics for the index advisor dashboard.
     * <p>
     * Computes aggregate counts of tables with high sequential scan ratios, unused indexes,
     * total indexes, and the total storage consumed by unused indexes. These metrics provide
     * a high-level overview of indexing opportunities and inefficiencies.
     *
     * @param instanceName the database instance identifier
     * @return summary statistics for index advisor analysis
     */
    public IndexAdvisorSummary getSummary(String instanceName) {
        IndexAdvisorSummary summary = new IndexAdvisorSummary();

        String sql = """
            SELECT
                (SELECT COUNT(*) FROM pg_stat_user_tables
                 WHERE seq_scan > 100 AND n_live_tup > 1000
                 AND seq_scan::float / NULLIF(seq_scan + idx_scan, 0) > 0.7) as high_seq_scan_tables,
                (SELECT COUNT(*) FROM pg_stat_user_indexes s
                 JOIN pg_index i ON s.indexrelid = i.indexrelid
                 WHERE s.idx_scan = 0 AND NOT i.indisprimary AND NOT i.indisunique) as unused_indexes,
                (SELECT COUNT(*) FROM pg_stat_user_indexes) as total_indexes,
                (SELECT pg_size_pretty(SUM(pg_relation_size(s.indexrelid)))
                 FROM pg_stat_user_indexes s
                 JOIN pg_index i ON s.indexrelid = i.indexrelid
                 WHERE s.idx_scan = 0 AND NOT i.indisprimary AND NOT i.indisunique) as unused_index_size
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                summary.setHighSeqScanTables(rs.getInt("high_seq_scan_tables"));
                summary.setUnusedIndexes(rs.getInt("unused_indexes"));
                summary.setTotalIndexes(rs.getInt("total_indexes"));
                summary.setUnusedIndexSize(rs.getString("unused_index_size"));
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to get index advisor summary for %s: %s", instanceName, e.getMessage());
        }

        return summary;
    }

    /**
     * Encapsulates summary statistics for the Index Advisor dashboard.
     * <p>
     * Provides aggregate metrics about indexing opportunities including tables needing
     * indexes, unused indexes, and storage waste from unused indexes.
     */
    public static class IndexAdvisorSummary {
        private int highSeqScanTables;
        private int unusedIndexes;
        private int totalIndexes;
        private String unusedIndexSize;

        public int getHighSeqScanTables() { return highSeqScanTables; }
        public void setHighSeqScanTables(int value) { this.highSeqScanTables = value; }

        public int getUnusedIndexes() { return unusedIndexes; }
        public void setUnusedIndexes(int value) { this.unusedIndexes = value; }

        public int getTotalIndexes() { return totalIndexes; }
        public void setTotalIndexes(int value) { this.totalIndexes = value; }

        public String getUnusedIndexSize() { return unusedIndexSize; }
        public void setUnusedIndexSize(String value) { this.unusedIndexSize = value; }

        public int getUnusedPercentage() {
            if (totalIndexes == 0) return 0;
            return (int) ((unusedIndexes * 100.0) / totalIndexes);
        }

        public int getUsedPercentage() {
            return 100 - getUnusedPercentage();
        }
    }
}
