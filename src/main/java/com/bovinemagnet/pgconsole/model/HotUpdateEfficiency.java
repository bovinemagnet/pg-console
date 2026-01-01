package com.bovinemagnet.pgconsole.model;

/**
 * Represents HOT (Heap-Only Tuple) update efficiency metrics.
 * <p>
 * HOT updates avoid creating new index entries when updating rows,
 * significantly reducing bloat. Low HOT efficiency indicates potential
 * for fill factor tuning or index review.
 * </p>
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class HotUpdateEfficiency {
    private String schemaName;
    private String tableName;
    private long nTupUpd;
    private long nTupHotUpd;
    private long nLiveTup;
    private long nDeadTup;
    private int fillfactor;
    private int indexCount;
    private long tableSizeBytes;

    public HotUpdateEfficiency() {}

    public String getSchemaName() { return schemaName; }
    public void setSchemaName(String schemaName) { this.schemaName = schemaName; }

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }

    public long getnTupUpd() { return nTupUpd; }
    public void setnTupUpd(long nTupUpd) { this.nTupUpd = nTupUpd; }

    public long getnTupHotUpd() { return nTupHotUpd; }
    public void setnTupHotUpd(long nTupHotUpd) { this.nTupHotUpd = nTupHotUpd; }

    public long getnLiveTup() { return nLiveTup; }
    public void setnLiveTup(long nLiveTup) { this.nLiveTup = nLiveTup; }

    public long getnDeadTup() { return nDeadTup; }
    public void setnDeadTup(long nDeadTup) { this.nDeadTup = nDeadTup; }

    public int getFillfactor() { return fillfactor; }
    public void setFillfactor(int fillfactor) { this.fillfactor = fillfactor; }

    public int getIndexCount() { return indexCount; }
    public void setIndexCount(int indexCount) { this.indexCount = indexCount; }

    public long getTableSizeBytes() { return tableSizeBytes; }
    public void setTableSizeBytes(long tableSizeBytes) { this.tableSizeBytes = tableSizeBytes; }

    /**
     * Returns the fully qualified table name.
     *
     * @return schema.table format
     */
    public String getFullTableName() {
        return schemaName + "." + tableName;
    }

    /**
     * Returns the HOT update efficiency as a percentage.
     * <p>
     * Higher percentage indicates better update efficiency.
     * 100% means all updates were HOT updates.
     * </p>
     *
     * @return HOT update percentage (0-100)
     */
    public double getHotUpdatePercent() {
        if (nTupUpd == 0) return 100.0; // No updates = perfect efficiency
        return (double) nTupHotUpd / nTupUpd * 100.0;
    }

    /**
     * Returns the non-HOT update count (updates that created index entries).
     *
     * @return non-HOT update count
     */
    public long getNonHotUpdates() {
        return nTupUpd - nTupHotUpd;
    }

    /**
     * Returns the bloat ratio as a percentage.
     *
     * @return bloat percentage
     */
    public double getBloatRatio() {
        if (nLiveTup == 0) return 0.0;
        return (double) nDeadTup / (nLiveTup + nDeadTup) * 100.0;
    }

    /**
     * Returns a recommended fill factor based on update patterns.
     * <p>
     * Tables with many updates benefit from lower fill factors
     * to allow more HOT updates.
     * </p>
     *
     * @return recommended fill factor (70-100)
     */
    public int getRecommendedFillfactor() {
        double hotPercent = getHotUpdatePercent();
        if (hotPercent >= 90) return 100; // Already efficient
        if (hotPercent >= 70) return 90;
        if (hotPercent >= 50) return 85;
        if (hotPercent >= 30) return 80;
        return 70; // Low HOT efficiency, needs more space
    }

    /**
     * Returns whether fill factor tuning is recommended.
     *
     * @return true if fill factor change would help
     */
    public boolean isFillfactorTuningRecommended() {
        if (nTupUpd < 1000) return false; // Not enough updates to matter
        int recommended = getRecommendedFillfactor();
        return fillfactor > recommended + 5; // Current is too high
    }

    /**
     * Returns the severity level based on HOT efficiency.
     *
     * @param warnThreshold percentage threshold for warning
     * @return "critical", "warning", or "ok"
     */
    public String getEfficiencyLevel(double warnThreshold) {
        if (nTupUpd < 100) return "ok"; // Not enough data
        double hotPercent = getHotUpdatePercent();
        if (hotPercent < warnThreshold / 2) return "critical";
        if (hotPercent < warnThreshold) return "warning";
        return "ok";
    }

    /**
     * Returns the CSS class for styling based on efficiency.
     *
     * @param warnThreshold percentage threshold for warning
     * @return CSS class name
     */
    public String getEfficiencyClass(double warnThreshold) {
        String level = getEfficiencyLevel(warnThreshold);
        return switch (level) {
            case "critical" -> "text-danger";
            case "warning" -> "text-warning";
            default -> "text-success";
        };
    }

    /**
     * Returns a recommendation message for improving HOT efficiency.
     *
     * @return recommendation string
     */
    public String getRecommendation() {
        if (nTupUpd < 100) {
            return "Insufficient update data for analysis";
        }

        double hotPercent = getHotUpdatePercent();
        if (hotPercent >= 90) {
            return "HOT update efficiency is excellent";
        }

        StringBuilder sb = new StringBuilder();
        if (isFillfactorTuningRecommended()) {
            sb.append(String.format("Consider reducing fill factor from %d to %d. ",
                    fillfactor, getRecommendedFillfactor()));
        }
        if (indexCount > 5) {
            sb.append("Many indexes may be preventing HOT updates. Review index necessity. ");
        }
        if (hotPercent < 50) {
            sb.append("Check if indexed columns are frequently updated.");
        }

        return sb.length() > 0 ? sb.toString().trim() : "Consider reviewing update patterns";
    }

    /**
     * Returns table size in human-readable format.
     *
     * @return formatted size string
     */
    public String getTableSizeDisplay() {
        if (tableSizeBytes < 1024) return tableSizeBytes + " B";
        if (tableSizeBytes < 1024 * 1024) return String.format("%.1f KB", tableSizeBytes / 1024.0);
        if (tableSizeBytes < 1024 * 1024 * 1024) return String.format("%.1f MB", tableSizeBytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", tableSizeBytes / (1024.0 * 1024.0 * 1024.0));
    }
}
