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
     * Gets all partitioned tables with their partitions.
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
     * Gets partition details for a specific table.
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
     * Gets orphan partitions (partitions without a parent).
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
     * Gets partitioning summary statistics.
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
     * Calculates partition size distribution metrics.
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
