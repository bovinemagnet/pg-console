package com.bovinemagnet.pgconsole.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for table partitioning insights and management.
 * Provides partition tree visualisation, size distribution, and recommendations.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class PartitioningService {

    private static final Logger LOG = Logger.getLogger(PartitioningService.class);

    @Inject
    DataSourceManager dataSourceManager;

    /**
     * Retrieves all partitioned tables and their partition details from the specified database instance.
     * <p>
     * Queries the {@code pg_partitioned_table} system catalogue to identify parent tables using
     * declarative partitioning, then retrieves all child partitions via the {@code pg_inherits}
     * system catalogue. For each partition, metadata including partition boundaries, size, and
     * tuple counts are collected.
     * <p>
     * After retrieving the data, this method calculates partition metrics including size distribution,
     * average partition size, and identifies imbalanced or empty partitions. Results are ordered by
     * total table size in descending order.
     * <p>
     * System schemas (pg_catalog, information_schema, pgconsole) are excluded from the results.
     *
     * @param instanceName the name of the PostgreSQL database instance to query
     * @return a list of {@link PartitionedTable} objects with populated partition details and metrics;
     *         returns an empty list if the query fails or no partitioned tables exist
     * @see PartitionedTable
     * @see Partition
     */
    public List<PartitionedTable> getPartitionedTables(String instanceName) {
        Map<String, PartitionedTable> tableMap = new HashMap<>();

        // Get parent tables
        String parentSql = """
            SELECT
                n.nspname as schema_name,
                c.relname as table_name,
                c.oid as table_oid,
                pg_get_partkeydef(c.oid) as partition_key,
                CASE p.partstrat
                    WHEN 'l' THEN 'LIST'
                    WHEN 'r' THEN 'RANGE'
                    WHEN 'h' THEN 'HASH'
                    ELSE 'UNKNOWN'
                END as partition_strategy,
                pg_size_pretty(pg_total_relation_size(c.oid)) as total_size,
                pg_total_relation_size(c.oid) as total_size_bytes
            FROM pg_class c
            JOIN pg_namespace n ON n.oid = c.relnamespace
            JOIN pg_partitioned_table p ON p.partrelid = c.oid
            WHERE n.nspname NOT IN ('pg_catalog', 'information_schema', 'pgconsole')
            ORDER BY pg_total_relation_size(c.oid) DESC
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(parentSql)) {

            while (rs.next()) {
                PartitionedTable table = new PartitionedTable();
                table.setSchemaName(rs.getString("schema_name"));
                table.setTableName(rs.getString("table_name"));
                table.setTableOid(rs.getLong("table_oid"));
                table.setPartitionKey(rs.getString("partition_key"));
                table.setPartitionStrategy(rs.getString("partition_strategy"));
                table.setTotalSize(rs.getString("total_size"));
                table.setTotalSizeBytes(rs.getLong("total_size_bytes"));
                table.setPartitions(new ArrayList<>());

                tableMap.put(table.getFullName(), table);
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to get partitioned tables for %s: %s", instanceName, e.getMessage());
            return new ArrayList<>();
        }

        // Get partitions for each parent
        String partitionSql = """
            SELECT
                parent_ns.nspname as parent_schema,
                parent.relname as parent_table,
                child_ns.nspname as partition_schema,
                child.relname as partition_name,
                pg_get_expr(child.relpartbound, child.oid) as partition_bound,
                pg_size_pretty(pg_total_relation_size(child.oid)) as partition_size,
                pg_total_relation_size(child.oid) as partition_size_bytes,
                (SELECT count(*) FROM pg_inherits WHERE inhparent = child.oid) as sub_partition_count
            FROM pg_inherits i
            JOIN pg_class parent ON parent.oid = i.inhparent
            JOIN pg_namespace parent_ns ON parent_ns.oid = parent.relnamespace
            JOIN pg_class child ON child.oid = i.inhrelid
            JOIN pg_namespace child_ns ON child_ns.oid = child.relnamespace
            WHERE parent_ns.nspname NOT IN ('pg_catalog', 'information_schema', 'pgconsole')
              AND child.relispartition = true
            ORDER BY parent_ns.nspname, parent.relname, child.relname
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(partitionSql)) {

            while (rs.next()) {
                String parentFullName = rs.getString("parent_schema") + "." + rs.getString("parent_table");
                PartitionedTable parent = tableMap.get(parentFullName);

                if (parent != null) {
                    Partition partition = new Partition();
                    partition.setSchemaName(rs.getString("partition_schema"));
                    partition.setPartitionName(rs.getString("partition_name"));
                    partition.setPartitionBound(rs.getString("partition_bound"));
                    partition.setSize(rs.getString("partition_size"));
                    partition.setSizeBytes(rs.getLong("partition_size_bytes"));
                    partition.setSubPartitionCount(rs.getInt("sub_partition_count"));

                    parent.getPartitions().add(partition);
                }
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to get partitions for %s: %s", instanceName, e.getMessage());
        }

        // Calculate size distribution and detect issues
        for (PartitionedTable table : tableMap.values()) {
            calculatePartitionMetrics(table);
        }

        return new ArrayList<>(tableMap.values());
    }

    /**
     * Retrieves detailed partition information for a specific partitioned table.
     * <p>
     * Queries the {@code pg_partitioned_table} and {@code pg_inherits} system catalogues to
     * retrieve comprehensive information about a single partitioned table and all its child
     * partitions. For each partition, this method retrieves the partition boundaries, size,
     * live and dead tuple counts, and maintenance timestamps (last vacuum, last analyse).
     * <p>
     * After retrieving the data, partition metrics are calculated including size distribution
     * percentages, imbalance detection, and identification of empty partitions. This detailed
     * view is useful for partition maintenance planning and troubleshooting partition skew issues.
     *
     * @param instanceName the name of the PostgreSQL database instance to query
     * @param schemaName the schema name of the partitioned table
     * @param tableName the name of the partitioned table
     * @return a {@link PartitionedTable} object with detailed partition information and metrics;
     *         returns {@code null} if the specified table is not found or is not partitioned
     * @see PartitionedTable
     * @see Partition
     */
    public PartitionedTable getPartitionedTableDetails(String instanceName, String schemaName, String tableName) {
        PartitionedTable table = null;

        String sql = """
            SELECT
                n.nspname as schema_name,
                c.relname as table_name,
                c.oid as table_oid,
                pg_get_partkeydef(c.oid) as partition_key,
                CASE p.partstrat
                    WHEN 'l' THEN 'LIST'
                    WHEN 'r' THEN 'RANGE'
                    WHEN 'h' THEN 'HASH'
                    ELSE 'UNKNOWN'
                END as partition_strategy,
                pg_size_pretty(pg_total_relation_size(c.oid)) as total_size,
                pg_total_relation_size(c.oid) as total_size_bytes
            FROM pg_class c
            JOIN pg_namespace n ON n.oid = c.relnamespace
            JOIN pg_partitioned_table p ON p.partrelid = c.oid
            WHERE n.nspname = ? AND c.relname = ?
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, schemaName);
            stmt.setString(2, tableName);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    table = new PartitionedTable();
                    table.setSchemaName(rs.getString("schema_name"));
                    table.setTableName(rs.getString("table_name"));
                    table.setTableOid(rs.getLong("table_oid"));
                    table.setPartitionKey(rs.getString("partition_key"));
                    table.setPartitionStrategy(rs.getString("partition_strategy"));
                    table.setTotalSize(rs.getString("total_size"));
                    table.setTotalSizeBytes(rs.getLong("total_size_bytes"));
                    table.setPartitions(new ArrayList<>());
                }
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to get partitioned table %s.%s for %s: %s",
                    schemaName, tableName, instanceName, e.getMessage());
            return null;
        }

        if (table == null) return null;

        // Get partitions with tuple counts
        String partitionSql = """
            SELECT
                child_ns.nspname as partition_schema,
                child.relname as partition_name,
                pg_get_expr(child.relpartbound, child.oid) as partition_bound,
                pg_size_pretty(pg_total_relation_size(child.oid)) as partition_size,
                pg_total_relation_size(child.oid) as partition_size_bytes,
                s.n_live_tup as live_tuples,
                s.n_dead_tup as dead_tuples,
                s.last_vacuum,
                s.last_analyze,
                (SELECT count(*) FROM pg_inherits WHERE inhparent = child.oid) as sub_partition_count
            FROM pg_inherits i
            JOIN pg_class parent ON parent.oid = i.inhparent
            JOIN pg_namespace parent_ns ON parent_ns.oid = parent.relnamespace
            JOIN pg_class child ON child.oid = i.inhrelid
            JOIN pg_namespace child_ns ON child_ns.oid = child.relnamespace
            LEFT JOIN pg_stat_user_tables s ON s.relid = child.oid
            WHERE parent_ns.nspname = ? AND parent.relname = ?
              AND child.relispartition = true
            ORDER BY child.relname
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             var stmt = conn.prepareStatement(partitionSql)) {

            stmt.setString(1, schemaName);
            stmt.setString(2, tableName);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Partition partition = new Partition();
                    partition.setSchemaName(rs.getString("partition_schema"));
                    partition.setPartitionName(rs.getString("partition_name"));
                    partition.setPartitionBound(rs.getString("partition_bound"));
                    partition.setSize(rs.getString("partition_size"));
                    partition.setSizeBytes(rs.getLong("partition_size_bytes"));
                    partition.setLiveTuples(rs.getLong("live_tuples"));
                    partition.setDeadTuples(rs.getLong("dead_tuples"));
                    partition.setSubPartitionCount(rs.getInt("sub_partition_count"));

                    table.getPartitions().add(partition);
                }
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to get partition details for %s.%s on %s: %s",
                    schemaName, tableName, instanceName, e.getMessage());
        }

        calculatePartitionMetrics(table);
        return table;
    }

    /**
     * Retrieves orphan partitions that exist without a valid parent table relationship.
     * <p>
     * Identifies partitions marked as {@code relispartition = true} in {@code pg_class} but
     * lacking corresponding entries in the {@code pg_inherits} system catalogue. Orphan partitions
     * typically result from incomplete DDL operations, failed partition detachment, or manual
     * catalogue manipulation.
     * <p>
     * Orphan partitions can cause issues with backup/restore operations and schema migration,
     * and should typically be either re-attached to a parent table or dropped. Results are
     * ordered by size in descending order to prioritise larger orphans.
     * <p>
     * System schemas (pg_catalog, information_schema, pgconsole) are excluded from the results.
     *
     * @param instanceName the name of the PostgreSQL database instance to query
     * @return a list of {@link OrphanPartition} objects ordered by size descending; returns
     *         an empty list if the query fails or no orphan partitions exist
     * @see OrphanPartition
     */
    public List<OrphanPartition> getOrphanPartitions(String instanceName) {
        List<OrphanPartition> orphans = new ArrayList<>();

        String sql = """
            SELECT
                n.nspname as schema_name,
                c.relname as table_name,
                pg_size_pretty(pg_total_relation_size(c.oid)) as size,
                pg_total_relation_size(c.oid) as size_bytes,
                s.n_live_tup as live_tuples
            FROM pg_class c
            JOIN pg_namespace n ON n.oid = c.relnamespace
            LEFT JOIN pg_stat_user_tables s ON s.relid = c.oid
            WHERE c.relispartition = true
              AND NOT EXISTS (
                  SELECT 1 FROM pg_inherits i WHERE i.inhrelid = c.oid
              )
              AND n.nspname NOT IN ('pg_catalog', 'information_schema', 'pgconsole')
            ORDER BY pg_total_relation_size(c.oid) DESC
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                OrphanPartition orphan = new OrphanPartition();
                orphan.setSchemaName(rs.getString("schema_name"));
                orphan.setTableName(rs.getString("table_name"));
                orphan.setSize(rs.getString("size"));
                orphan.setSizeBytes(rs.getLong("size_bytes"));
                orphan.setLiveTuples(rs.getLong("live_tuples"));
                orphans.add(orphan);
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to get orphan partitions for %s: %s", instanceName, e.getMessage());
        }

        return orphans;
    }

    /**
     * Retrieves aggregate partitioning statistics for the specified database instance.
     * <p>
     * Provides a database-wide summary of partitioning usage including counts of partitioned
     * tables, total partitions, orphan partitions, and the cumulative size of all partitioned
     * tables. Additionally identifies imbalanced tables (those with significant size skew across
     * partitions) to highlight potential maintenance requirements.
     * <p>
     * This summary is useful for understanding the overall partitioning landscape, identifying
     * potential issues (orphans, imbalanced partitions), and planning partition maintenance activities.
     *
     * @param instanceName the name of the PostgreSQL database instance to query
     * @return a {@link PartitioningSummary} object containing aggregate partitioning statistics;
     *         returns an object with default values (zeros) if the query fails
     * @see PartitioningSummary
     */
    public PartitioningSummary getSummary(String instanceName) {
        PartitioningSummary summary = new PartitioningSummary();

        String sql = """
            SELECT
                (SELECT count(*) FROM pg_partitioned_table) as partitioned_table_count,
                (SELECT count(*) FROM pg_class WHERE relispartition = true) as partition_count,
                (SELECT count(*) FROM pg_class c
                 WHERE c.relispartition = true
                 AND NOT EXISTS (SELECT 1 FROM pg_inherits i WHERE i.inhrelid = c.oid)) as orphan_count,
                (SELECT COALESCE(sum(pg_total_relation_size(c.oid)), 0)
                 FROM pg_class c
                 JOIN pg_partitioned_table p ON p.partrelid = c.oid) as total_size_bytes
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                summary.setPartitionedTableCount(rs.getInt("partitioned_table_count"));
                summary.setTotalPartitionCount(rs.getInt("partition_count"));
                summary.setOrphanPartitionCount(rs.getInt("orphan_count"));
                summary.setTotalSizeBytes(rs.getLong("total_size_bytes"));
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to get partitioning summary for %s: %s", instanceName, e.getMessage());
        }

        // Count imbalanced tables
        List<PartitionedTable> tables = getPartitionedTables(instanceName);
        int imbalanced = (int) tables.stream().filter(PartitionedTable::isImbalanced).count();
        summary.setImbalancedTableCount(imbalanced);

        return summary;
    }

    /**
     * Calculates partition size distribution metrics and identifies imbalanced or empty partitions.
     * <p>
     * Computes aggregate metrics for a partitioned table including partition count, average
     * partition size, and size imbalance ratio (max size / min size). For each individual partition,
     * calculates the size percentage relative to total table size and flags partitions that are
     * significantly larger than average (more than 3x) or completely empty.
     * <p>
     * The size imbalance ratio helps identify tables with uneven data distribution across partitions,
     * which may benefit from partition reorganisation or revised partitioning strategies. A ratio
     * greater than 10 indicates significant imbalance.
     * <p>
     * This is a private helper method called internally after partition data retrieval.
     *
     * @param table the partitioned table for which to calculate metrics; must have a populated
     *              partition list (may be empty)
     */
    private void calculatePartitionMetrics(PartitionedTable table) {
        List<Partition> partitions = table.getPartitions();
        if (partitions.isEmpty()) return;

        long totalSize = partitions.stream().mapToLong(Partition::getSizeBytes).sum();
        long avgSize = totalSize / partitions.size();
        long minSize = partitions.stream().mapToLong(Partition::getSizeBytes).min().orElse(0);
        long maxSize = partitions.stream().mapToLong(Partition::getSizeBytes).max().orElse(0);

        table.setPartitionCount(partitions.size());
        table.setAveragePartitionSize(avgSize);

        // Calculate size distribution percentage for each partition
        for (Partition partition : partitions) {
            if (totalSize > 0) {
                partition.setSizePercentage((double) partition.getSizeBytes() / totalSize * 100);
            }

            // Flag imbalanced partitions (more than 3x average)
            if (avgSize > 0 && partition.getSizeBytes() > avgSize * 3) {
                partition.setImbalanced(true);
            }

            // Flag empty partitions
            if (partition.getSizeBytes() == 0) {
                partition.setEmpty(true);
            }
        }

        // Detect overall imbalance
        if (maxSize > 0 && minSize > 0) {
            double sizeRatio = (double) maxSize / minSize;
            table.setSizeImbalanceRatio(sizeRatio);
            table.setHasImbalance(sizeRatio > 10);
        }
    }

    // --- Model Classes ---

    /**
     * Data transfer object representing a partitioned table and its associated partition metadata.
     * <p>
     * Encapsulates information about a PostgreSQL declarative partitioned table including the
     * partition strategy (RANGE, LIST, HASH), partition key definition, total table size, and
     * a collection of all child partitions. Additionally provides calculated metrics such as
     * partition count, average partition size, and size imbalance ratios.
     * <p>
     * The imbalance detection helps identify partition skew where some partitions are significantly
     * larger than others, which may indicate the need for partition maintenance or strategy revision.
     *
     * @see #getPartitionedTables(String)
     * @see #getPartitionedTableDetails(String, String, String)
     * @see Partition
     */
    public static class PartitionedTable {
        private String schemaName;
        private String tableName;
        private long tableOid;
        private String partitionKey;
        private String partitionStrategy;
        private String totalSize;
        private long totalSizeBytes;
        private List<Partition> partitions;
        private int partitionCount;
        private long averagePartitionSize;
        private double sizeImbalanceRatio;
        private boolean hasImbalance;

        public String getSchemaName() { return schemaName; }
        public void setSchemaName(String name) { this.schemaName = name; }

        public String getTableName() { return tableName; }
        public void setTableName(String name) { this.tableName = name; }

        public String getFullName() { return schemaName + "." + tableName; }

        public long getTableOid() { return tableOid; }
        public void setTableOid(long oid) { this.tableOid = oid; }

        public String getPartitionKey() { return partitionKey; }
        public void setPartitionKey(String key) { this.partitionKey = key; }

        public String getPartitionStrategy() { return partitionStrategy; }
        public void setPartitionStrategy(String strategy) { this.partitionStrategy = strategy; }

        public String getStrategyCssClass() {
            if (partitionStrategy == null) return "bg-secondary";
            return switch (partitionStrategy) {
                case "RANGE" -> "bg-primary";
                case "LIST" -> "bg-info";
                case "HASH" -> "bg-success";
                default -> "bg-secondary";
            };
        }

        public String getTotalSize() { return totalSize; }
        public void setTotalSize(String size) { this.totalSize = size; }

        public long getTotalSizeBytes() { return totalSizeBytes; }
        public void setTotalSizeBytes(long bytes) { this.totalSizeBytes = bytes; }

        public List<Partition> getPartitions() { return partitions; }
        public void setPartitions(List<Partition> partitions) { this.partitions = partitions; }

        public int getPartitionCount() { return partitionCount; }
        public void setPartitionCount(int count) { this.partitionCount = count; }

        public long getAveragePartitionSize() { return averagePartitionSize; }
        public void setAveragePartitionSize(long size) { this.averagePartitionSize = size; }

        public String getAveragePartitionSizeFormatted() {
            return formatBytes(averagePartitionSize);
        }

        public double getSizeImbalanceRatio() { return sizeImbalanceRatio; }
        public void setSizeImbalanceRatio(double ratio) { this.sizeImbalanceRatio = ratio; }

        public String getSizeImbalanceRatioFormatted() {
            return String.format("%.1fx", sizeImbalanceRatio);
        }

        public boolean isHasImbalance() { return hasImbalance; }
        public void setHasImbalance(boolean hasImbalance) { this.hasImbalance = hasImbalance; }

        // Alias for template usage
        public boolean isImbalanced() { return hasImbalance; }

        public String getStrategy() { return partitionStrategy; }

        public String getTotalSizeFormatted() { return totalSize; }

        public long getTotalRows() {
            if (partitions == null) return 0;
            return partitions.stream().mapToLong(Partition::getLiveTuples).sum();
        }

        public String getTotalRowsFormatted() {
            long total = getTotalRows();
            if (total >= 1_000_000_000) return String.format("%.1fB", total / 1_000_000_000.0);
            if (total >= 1_000_000) return String.format("%.1fM", total / 1_000_000.0);
            if (total >= 1_000) return String.format("%.1fK", total / 1_000.0);
            return String.valueOf(total);
        }

        public boolean hasEmptyPartitions() {
            if (partitions == null) return false;
            return partitions.stream().anyMatch(Partition::isEmpty);
        }

        private String formatBytes(long bytes) {
            if (bytes >= 1024L * 1024 * 1024) return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
            if (bytes >= 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
            if (bytes >= 1024) return String.format("%.1f KB", bytes / 1024.0);
            return bytes + " B";
        }
    }

    /**
     * Data transfer object representing a single partition within a partitioned table.
     * <p>
     * Contains detailed information about an individual partition including its name, partition
     * boundary definition, size, tuple counts (live and dead), and sub-partition count for
     * multi-level partitioning. Additionally includes calculated fields such as size percentage
     * relative to the parent table and flags for imbalanced or empty partitions.
     * <p>
     * Partitions flagged as imbalanced (more than 3x average size) or empty (zero size) may
     * require attention such as data redistribution, partition pruning, or maintenance operations.
     *
     * @see PartitionedTable
     */
    public static class Partition {
        private String schemaName;
        private String partitionName;
        private String partitionBound;
        private String size;
        private long sizeBytes;
        private long liveTuples;
        private long deadTuples;
        private int subPartitionCount;
        private double sizePercentage;
        private boolean imbalanced;
        private boolean empty;

        public String getSchemaName() { return schemaName; }
        public void setSchemaName(String name) { this.schemaName = name; }

        public String getPartitionName() { return partitionName; }
        public void setPartitionName(String name) { this.partitionName = name; }

        public String getFullName() { return schemaName + "." + partitionName; }

        public String getPartitionBound() { return partitionBound; }
        public void setPartitionBound(String bound) { this.partitionBound = bound; }

        public String getSize() { return size; }
        public void setSize(String size) { this.size = size; }

        public long getSizeBytes() { return sizeBytes; }
        public void setSizeBytes(long bytes) { this.sizeBytes = bytes; }

        public long getLiveTuples() { return liveTuples; }
        public void setLiveTuples(long tuples) { this.liveTuples = tuples; }

        public long getDeadTuples() { return deadTuples; }
        public void setDeadTuples(long tuples) { this.deadTuples = tuples; }

        public int getSubPartitionCount() { return subPartitionCount; }
        public void setSubPartitionCount(int count) { this.subPartitionCount = count; }

        public boolean hasSubPartitions() { return subPartitionCount > 0; }

        public double getSizePercentage() { return sizePercentage; }
        public void setSizePercentage(double pct) { this.sizePercentage = pct; }

        public String getSizePercentageFormatted() {
            return String.format("%.1f%%", sizePercentage);
        }

        public boolean isImbalanced() { return imbalanced; }
        public void setImbalanced(boolean imbalanced) { this.imbalanced = imbalanced; }

        // Alias for template - marks partitions significantly larger than average
        public boolean isOverweight() { return imbalanced; }

        public boolean isEmpty() { return empty; }
        public void setEmpty(boolean empty) { this.empty = empty; }

        public String getName() { return partitionName; }

        public String getBoundaryDisplay() { return partitionBound != null ? partitionBound : "DEFAULT"; }

        public String getSizeFormatted() { return size; }

        public String getRowCountFormatted() {
            if (liveTuples >= 1_000_000_000) return String.format("%.1fB", liveTuples / 1_000_000_000.0);
            if (liveTuples >= 1_000_000) return String.format("%.1fM", liveTuples / 1_000_000.0);
            if (liveTuples >= 1_000) return String.format("%.1fK", liveTuples / 1_000.0);
            return String.valueOf(liveTuples);
        }

        public String getRowCssClass() {
            if (imbalanced) return "table-warning";
            if (empty) return "table-secondary";
            return "";
        }
    }

    /**
     * Data transfer object representing an orphan partition without a valid parent table.
     * <p>
     * Orphan partitions are tables marked as partitions ({@code relispartition = true}) but
     * lacking entries in the inheritance hierarchy ({@code pg_inherits}). These typically result
     * from incomplete DDL operations, failed partition detachment, or manual system catalogue
     * modifications.
     * <p>
     * Orphan partitions should be investigated and either re-attached to an appropriate parent
     * table or dropped to prevent schema inconsistencies and potential backup/restore issues.
     *
     * @see #getOrphanPartitions(String)
     */
    public static class OrphanPartition {
        private String schemaName;
        private String tableName;
        private String size;
        private long sizeBytes;
        private long liveTuples;

        public String getSchemaName() { return schemaName; }
        public void setSchemaName(String name) { this.schemaName = name; }

        public String getTableName() { return tableName; }
        public void setTableName(String name) { this.tableName = name; }

        public String getFullName() { return schemaName + "." + tableName; }

        public String getSize() { return size; }
        public void setSize(String size) { this.size = size; }

        public long getSizeBytes() { return sizeBytes; }
        public void setSizeBytes(long bytes) { this.sizeBytes = bytes; }

        public long getLiveTuples() { return liveTuples; }
        public void setLiveTuples(long tuples) { this.liveTuples = tuples; }

        public String getPartitionName() { return tableName; }

        public String getExpectedParent() { return "Unknown (orphaned)"; }

        public String getSizeFormatted() { return size; }

        public String getIssue() { return "Partition without parent table - possible DDL failure"; }
    }

    /**
     * Data transfer object containing aggregate partitioning statistics for a database instance.
     * <p>
     * Provides summary counts of partitioned tables, total partitions, orphan partitions, and
     * imbalanced tables across the entire database. Also tracks the cumulative size of all
     * partitioned data. This summary helps assess the overall health and complexity of the
     * database's partitioning implementation.
     * <p>
     * Metrics such as orphan partition count and imbalanced table count serve as indicators
     * of potential maintenance requirements or partitioning strategy issues.
     *
     * @see #getSummary(String)
     */
    public static class PartitioningSummary {
        private int partitionedTableCount;
        private int totalPartitionCount;
        private int orphanPartitionCount;

        public int getPartitionedTableCount() { return partitionedTableCount; }
        public void setPartitionedTableCount(int count) { this.partitionedTableCount = count; }

        public int getTotalPartitionCount() { return totalPartitionCount; }
        public void setTotalPartitionCount(int count) { this.totalPartitionCount = count; }

        public int getOrphanPartitionCount() { return orphanPartitionCount; }
        public void setOrphanPartitionCount(int count) { this.orphanPartitionCount = count; }

        public boolean hasPartitioning() { return partitionedTableCount > 0; }

        public boolean hasIssues() { return orphanPartitionCount > 0; }

        // Additional getters for template
        private long totalSizeBytes;
        private int imbalancedTableCount;

        public void setTotalSizeBytes(long bytes) { this.totalSizeBytes = bytes; }
        public long getTotalSizeBytes() { return totalSizeBytes; }

        public String getTotalSizeFormatted() {
            if (totalSizeBytes >= 1024L * 1024 * 1024) return String.format("%.1f GB", totalSizeBytes / (1024.0 * 1024 * 1024));
            if (totalSizeBytes >= 1024 * 1024) return String.format("%.1f MB", totalSizeBytes / (1024.0 * 1024));
            if (totalSizeBytes >= 1024) return String.format("%.1f KB", totalSizeBytes / 1024.0);
            return totalSizeBytes + " B";
        }

        public void setImbalancedTableCount(int count) { this.imbalancedTableCount = count; }
        public int getImbalancedTableCount() { return imbalancedTableCount; }
    }
}
