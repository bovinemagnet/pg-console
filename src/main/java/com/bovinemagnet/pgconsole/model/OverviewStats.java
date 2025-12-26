package com.bovinemagnet.pgconsole.model;

import java.util.List;

/**
 * Overview statistics for the dashboard.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class OverviewStats {
    private String version;
    private int connectionsUsed;
    private int connectionsMax;
    private int activeQueries;
    private int blockedQueries;
    private String longestQueryDuration;
    private double cacheHitRatio;
    private String databaseSize;
    private List<TableSize> topTablesBySize;
    private List<IndexSize> topIndexesBySize;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Alias for getConnectionsUsed() for API compatibility.
     */
    public int getActiveConnections() {
        return connectionsUsed;
    }

    /**
     * Alias for getConnectionsMax() for API compatibility.
     */
    public int getMaxConnections() {
        return connectionsMax;
    }

    public int getConnectionsUsed() {
        return connectionsUsed;
    }

    public void setConnectionsUsed(int connectionsUsed) {
        this.connectionsUsed = connectionsUsed;
    }

    public int getConnectionsMax() {
        return connectionsMax;
    }

    public void setConnectionsMax(int connectionsMax) {
        this.connectionsMax = connectionsMax;
    }

    public int getActiveQueries() {
        return activeQueries;
    }

    public void setActiveQueries(int activeQueries) {
        this.activeQueries = activeQueries;
    }

    public int getBlockedQueries() {
        return blockedQueries;
    }

    public void setBlockedQueries(int blockedQueries) {
        this.blockedQueries = blockedQueries;
    }

    public String getLongestQueryDuration() {
        return longestQueryDuration;
    }

    public void setLongestQueryDuration(String longestQueryDuration) {
        this.longestQueryDuration = longestQueryDuration;
    }

    public double getCacheHitRatio() {
        return cacheHitRatio;
    }

    public void setCacheHitRatio(double cacheHitRatio) {
        this.cacheHitRatio = cacheHitRatio;
    }

    public String getDatabaseSize() {
        return databaseSize;
    }

    public void setDatabaseSize(String databaseSize) {
        this.databaseSize = databaseSize;
    }

    public List<TableSize> getTopTablesBySize() {
        return topTablesBySize;
    }

    public void setTopTablesBySize(List<TableSize> topTablesBySize) {
        this.topTablesBySize = topTablesBySize;
    }

    public List<IndexSize> getTopIndexesBySize() {
        return topIndexesBySize;
    }

    public void setTopIndexesBySize(List<IndexSize> topIndexesBySize) {
        this.topIndexesBySize = topIndexesBySize;
    }

    public int getConnectionPercentage() {
        if (connectionsMax == 0) return 0;
        return (int) ((connectionsUsed * 100.0) / connectionsMax);
    }

    public String getCacheHitRatioFormatted() {
        return String.format("%.1f%%", cacheHitRatio);
    }

    /**
     * Table size information.
     */
    public static class TableSize {
        private String schemaName;
        private String tableName;
        private String size;
        private long sizeBytes;

        public String getSchemaName() {
            return schemaName;
        }

        public void setSchemaName(String schemaName) {
            this.schemaName = schemaName;
        }

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public String getSize() {
            return size;
        }

        public void setSize(String size) {
            this.size = size;
        }

        public long getSizeBytes() {
            return sizeBytes;
        }

        public void setSizeBytes(long sizeBytes) {
            this.sizeBytes = sizeBytes;
        }

        public String getFullName() {
            return schemaName + "." + tableName;
        }
    }

    /**
     * Index size information.
     */
    public static class IndexSize {
        private String schemaName;
        private String indexName;
        private String tableName;
        private String size;
        private long sizeBytes;

        public String getSchemaName() {
            return schemaName;
        }

        public void setSchemaName(String schemaName) {
            this.schemaName = schemaName;
        }

        public String getIndexName() {
            return indexName;
        }

        public void setIndexName(String indexName) {
            this.indexName = indexName;
        }

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public String getSize() {
            return size;
        }

        public void setSize(String size) {
            this.size = size;
        }

        public long getSizeBytes() {
            return sizeBytes;
        }

        public void setSizeBytes(long sizeBytes) {
            this.sizeBytes = sizeBytes;
        }
    }
}
