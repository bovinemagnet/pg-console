package com.bovinemagnet.pgconsole.model;

/**
 * Represents HOT (Heap-Only Tuple) update efficiency metrics for PostgreSQL tables.
 * <p>
 * HOT (Heap-Only Tuple) updates are a PostgreSQL optimisation that allows UPDATE operations
 * to avoid creating new index entries when the updated columns are not indexed. This
 * significantly reduces table bloat, improves performance, and reduces I/O overhead.
 * </p>
 * <p>
 * This class tracks key metrics from {@code pg_stat_user_tables} and table metadata to
 * measure how efficiently tables are utilising HOT updates. Low HOT efficiency may indicate:
 * </p>
 * <ul>
 *   <li>Insufficient free space in table pages (fill factor too high)</li>
 *   <li>Excessive number of indexes preventing HOT updates</li>
 *   <li>Frequent updates to indexed columns</li>
 *   <li>Need for table reorganisation or VACUUM</li>
 * </ul>
 * <p>
 * The class provides analytical methods to calculate efficiency percentages, bloat ratios,
 * and recommendations for improving HOT update performance through fill factor tuning.
 * </p>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @since 0.0.0
 * @see <a href="https://www.postgresql.org/docs/current/storage-hot.html">PostgreSQL HOT Updates</a>
 */
public class HotUpdateEfficiency {

    /**
     * The schema name containing the table.
     */
    private String schemaName;

    /**
     * The table name within the schema.
     */
    private String tableName;

    /**
     * Total number of rows updated since statistics were last reset.
     * Corresponds to {@code n_tup_upd} from {@code pg_stat_user_tables}.
     */
    private long nTupUpd;

    /**
     * Number of rows updated using HOT (Heap-Only Tuple) updates.
     * Corresponds to {@code n_tup_hot_upd} from {@code pg_stat_user_tables}.
     * HOT updates avoid creating new index entries, improving performance.
     */
    private long nTupHotUpd;

    /**
     * Estimated number of live (non-deleted) rows in the table.
     * Corresponds to {@code n_live_tup} from {@code pg_stat_user_tables}.
     */
    private long nLiveTup;

    /**
     * Estimated number of dead (deleted) rows awaiting VACUUM.
     * Corresponds to {@code n_dead_tup} from {@code pg_stat_user_tables}.
     * High values indicate bloat and need for vacuum.
     */
    private long nDeadTup;

    /**
     * The table's fill factor percentage (0-100).
     * Lower values reserve more free space per page for HOT updates.
     * Default is 100 (no reserved space). Typical tuned values are 70-90.
     */
    private int fillfactor;

    /**
     * Number of indexes on the table.
     * More indexes reduce the likelihood of HOT updates, as any indexed
     * column update prevents HOT optimisation.
     */
    private int indexCount;

    /**
     * Total size of the table in bytes, including TOAST and indexes.
     */
    private long tableSizeBytes;

    /**
     * Constructs a new HotUpdateEfficiency instance with default values.
     */
    public HotUpdateEfficiency() {}

    /**
     * Returns the schema name containing the table.
     *
     * @return the schema name, or null if not set
     */
    public String getSchemaName() { return schemaName; }

    /**
     * Sets the schema name containing the table.
     *
     * @param schemaName the schema name to set
     */
    public void setSchemaName(String schemaName) { this.schemaName = schemaName; }

    /**
     * Returns the table name within the schema.
     *
     * @return the table name, or null if not set
     */
    public String getTableName() { return tableName; }

    /**
     * Sets the table name within the schema.
     *
     * @param tableName the table name to set
     */
    public void setTableName(String tableName) { this.tableName = tableName; }

    /**
     * Returns the total number of rows updated since statistics were last reset.
     *
     * @return the total update count from {@code pg_stat_user_tables.n_tup_upd}
     */
    public long getnTupUpd() { return nTupUpd; }

    /**
     * Sets the total number of rows updated.
     *
     * @param nTupUpd the total update count to set
     */
    public void setnTupUpd(long nTupUpd) { this.nTupUpd = nTupUpd; }

    /**
     * Returns the number of rows updated using HOT (Heap-Only Tuple) updates.
     *
     * @return the HOT update count from {@code pg_stat_user_tables.n_tup_hot_upd}
     */
    public long getnTupHotUpd() { return nTupHotUpd; }

    /**
     * Sets the number of rows updated using HOT updates.
     *
     * @param nTupHotUpd the HOT update count to set
     */
    public void setnTupHotUpd(long nTupHotUpd) { this.nTupHotUpd = nTupHotUpd; }

    /**
     * Returns the estimated number of live (non-deleted) rows in the table.
     *
     * @return the live tuple count from {@code pg_stat_user_tables.n_live_tup}
     */
    public long getnLiveTup() { return nLiveTup; }

    /**
     * Sets the estimated number of live rows.
     *
     * @param nLiveTup the live tuple count to set
     */
    public void setnLiveTup(long nLiveTup) { this.nLiveTup = nLiveTup; }

    /**
     * Returns the estimated number of dead (deleted) rows awaiting VACUUM.
     *
     * @return the dead tuple count from {@code pg_stat_user_tables.n_dead_tup}
     */
    public long getnDeadTup() { return nDeadTup; }

    /**
     * Sets the estimated number of dead rows.
     *
     * @param nDeadTup the dead tuple count to set
     */
    public void setnDeadTup(long nDeadTup) { this.nDeadTup = nDeadTup; }

    /**
     * Returns the table's fill factor percentage.
     *
     * @return the fill factor (0-100), where 100 means no reserved space
     */
    public int getFillfactor() { return fillfactor; }

    /**
     * Sets the table's fill factor percentage.
     *
     * @param fillfactor the fill factor to set (0-100)
     */
    public void setFillfactor(int fillfactor) { this.fillfactor = fillfactor; }

    /**
     * Returns the number of indexes on the table.
     *
     * @return the count of indexes
     */
    public int getIndexCount() { return indexCount; }

    /**
     * Sets the number of indexes on the table.
     *
     * @param indexCount the index count to set
     */
    public void setIndexCount(int indexCount) { this.indexCount = indexCount; }

    /**
     * Returns the total size of the table in bytes.
     *
     * @return the table size in bytes, including TOAST and indexes
     */
    public long getTableSizeBytes() { return tableSizeBytes; }

    /**
     * Sets the total size of the table in bytes.
     *
     * @param tableSizeBytes the table size to set
     */
    public void setTableSizeBytes(long tableSizeBytes) { this.tableSizeBytes = tableSizeBytes; }

    /**
     * Returns the fully qualified table name in schema.table format.
     * <p>
     * Useful for display and logging purposes where the full table identifier is needed.
     * </p>
     *
     * @return the fully qualified name as "schema.table"
     */
    public String getFullTableName() {
        return schemaName + "." + tableName;
    }

    /**
     * Calculates and returns the HOT update efficiency as a percentage.
     * <p>
     * This metric indicates what proportion of UPDATE operations were able to use
     * the HOT optimisation. A higher percentage indicates better performance:
     * </p>
     * <ul>
     *   <li>100% - All updates were HOT updates (optimal)</li>
     *   <li>90-100% - Excellent efficiency</li>
     *   <li>70-90% - Good efficiency</li>
     *   <li>50-70% - Moderate efficiency, consider tuning</li>
     *   <li>&lt;50% - Poor efficiency, tuning recommended</li>
     * </ul>
     * <p>
     * If no updates have occurred ({@code n_tup_upd} is 0), returns 100.0 to indicate
     * there are no efficiency concerns.
     * </p>
     *
     * @return HOT update percentage (0.0-100.0)
     */
    public double getHotUpdatePercent() {
        if (nTupUpd == 0) return 100.0; // No updates = perfect efficiency
        return (double) nTupHotUpd / nTupUpd * 100.0;
    }

    /**
     * Calculates the number of non-HOT updates that required index maintenance.
     * <p>
     * Non-HOT updates are more expensive because they require:
     * </p>
     * <ul>
     *   <li>Creating new index entries for all affected indexes</li>
     *   <li>Updating index pointers to the new tuple version</li>
     *   <li>Additional I/O operations</li>
     *   <li>Potential for increased table bloat</li>
     * </ul>
     * <p>
     * A high count relative to total updates indicates tuning opportunities.
     * </p>
     *
     * @return the count of updates that were not HOT updates (n_tup_upd - n_tup_hot_upd)
     */
    public long getNonHotUpdates() {
        return nTupUpd - nTupHotUpd;
    }

    /**
     * Calculates the table bloat ratio as a percentage of total tuples.
     * <p>
     * Bloat represents the proportion of dead tuples (deleted or obsolete row versions)
     * that occupy space but serve no purpose. High bloat ratios indicate:
     * </p>
     * <ul>
     *   <li>Need for VACUUM to reclaim space</li>
     *   <li>Potential for performance degradation</li>
     *   <li>Wasted disk space and memory cache</li>
     *   <li>Inefficient sequential scans</li>
     * </ul>
     * <p>
     * Recommended thresholds:
     * </p>
     * <ul>
     *   <li>&lt;10% - Healthy</li>
     *   <li>10-20% - Monitor, consider VACUUM</li>
     *   <li>20-30% - VACUUM recommended</li>
     *   <li>&gt;30% - VACUUM urgently needed</li>
     * </ul>
     * <p>
     * Returns 0.0 if the table has no tuples.
     * </p>
     *
     * @return bloat percentage (0.0-100.0)
     */
    public double getBloatRatio() {
        if (nLiveTup == 0) return 0.0;
        return (double) nDeadTup / (nLiveTup + nDeadTup) * 100.0;
    }

    /**
     * Recommends an optimal fill factor based on observed HOT update efficiency.
     * <p>
     * The fill factor determines how much free space is reserved in each table page
     * for future updates. Lower fill factors reserve more space, enabling more HOT
     * updates by ensuring updated tuples can fit in the same page.
     * </p>
     * <p>
     * Recommendation logic:
     * </p>
     * <ul>
     *   <li>90%+ HOT efficiency → 100 fill factor (no change needed)</li>
     *   <li>70-90% HOT efficiency → 90 fill factor (slight reservation)</li>
     *   <li>50-70% HOT efficiency → 85 fill factor (moderate reservation)</li>
     *   <li>30-50% HOT efficiency → 80 fill factor (significant reservation)</li>
     *   <li>&lt;30% HOT efficiency → 70 fill factor (maximum reservation)</li>
     * </ul>
     * <p>
     * Note: Changing fill factor requires a table rewrite (VACUUM FULL or CLUSTER).
     * </p>
     *
     * @return recommended fill factor value (70-100)
     * @see #isFillfactorTuningRecommended()
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
     * Determines whether fill factor tuning is recommended for this table.
     * <p>
     * Tuning is recommended when all of the following conditions are met:
     * </p>
     * <ul>
     *   <li>Table has at least 1000 updates (sufficient data for analysis)</li>
     *   <li>Current fill factor is more than 5 points higher than recommended</li>
     *   <li>The difference is significant enough to justify the cost of rewriting the table</li>
     * </ul>
     * <p>
     * Tables with fewer than 1000 updates are excluded as the sample size is too
     * small to make reliable recommendations.
     * </p>
     *
     * @return true if adjusting the fill factor would likely improve HOT efficiency, false otherwise
     * @see #getRecommendedFillfactor()
     */
    public boolean isFillfactorTuningRecommended() {
        if (nTupUpd < 1000) return false; // Not enough updates to matter
        int recommended = getRecommendedFillfactor();
        return fillfactor > recommended + 5; // Current is too high
    }

    /**
     * Determines the severity level of HOT update inefficiency.
     * <p>
     * Classifies the efficiency into three levels based on the warning threshold:
     * </p>
     * <ul>
     *   <li><strong>critical</strong> - HOT efficiency below half the warning threshold</li>
     *   <li><strong>warning</strong> - HOT efficiency below the warning threshold</li>
     *   <li><strong>ok</strong> - HOT efficiency at or above the warning threshold</li>
     * </ul>
     * <p>
     * Tables with fewer than 100 updates are always classified as "ok" since there
     * is insufficient data for meaningful analysis.
     * </p>
     *
     * @param warnThreshold the percentage threshold for warning level (typically 70.0)
     * @return severity level: "critical", "warning", or "ok"
     * @see #getEfficiencyClass(double)
     */
    public String getEfficiencyLevel(double warnThreshold) {
        if (nTupUpd < 100) return "ok"; // Not enough data
        double hotPercent = getHotUpdatePercent();
        if (hotPercent < warnThreshold / 2) return "critical";
        if (hotPercent < warnThreshold) return "warning";
        return "ok";
    }

    /**
     * Returns the Bootstrap CSS class name for styling based on HOT efficiency.
     * <p>
     * Maps efficiency levels to Bootstrap text colour classes for visual indication:
     * </p>
     * <ul>
     *   <li><strong>text-danger</strong> - Critical efficiency issues (red)</li>
     *   <li><strong>text-warning</strong> - Warning level efficiency (amber/yellow)</li>
     *   <li><strong>text-success</strong> - Acceptable efficiency (green)</li>
     * </ul>
     * <p>
     * Useful for displaying efficiency metrics in HTML templates with appropriate
     * visual indicators.
     * </p>
     *
     * @param warnThreshold the percentage threshold for warning level (typically 70.0)
     * @return Bootstrap CSS class name for text colour
     * @see #getEfficiencyLevel(double)
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
     * Generates a human-readable recommendation message for improving HOT update efficiency.
     * <p>
     * Analyses the current metrics and provides actionable recommendations based on:
     * </p>
     * <ul>
     *   <li>Sample size (requires at least 100 updates for meaningful analysis)</li>
     *   <li>Current HOT efficiency percentage</li>
     *   <li>Fill factor tuning opportunities</li>
     *   <li>Index count impact</li>
     *   <li>Overall update patterns</li>
     * </ul>
     * <p>
     * Example recommendations:
     * </p>
     * <ul>
     *   <li>"HOT update efficiency is excellent" (90%+ efficiency)</li>
     *   <li>"Consider reducing fill factor from 100 to 85" (tuning recommended)</li>
     *   <li>"Many indexes may be preventing HOT updates. Review index necessity." (6+ indexes)</li>
     *   <li>"Check if indexed columns are frequently updated." (low efficiency)</li>
     *   <li>"Insufficient update data for analysis" (less than 100 updates)</li>
     * </ul>
     *
     * @return a recommendation message, never null
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
     * Formats the table size in human-readable units.
     * <p>
     * Automatically selects the most appropriate unit (B, KB, MB, GB) based on
     * the size value to ensure readability. The formatted string includes one
     * decimal place for KB and MB, and two decimal places for GB.
     * </p>
     * <p>
     * Examples:
     * </p>
     * <ul>
     *   <li>512 bytes → "512 B"</li>
     *   <li>1536 bytes → "1.5 KB"</li>
     *   <li>5242880 bytes → "5.0 MB"</li>
     *   <li>1073741824 bytes → "1.00 GB"</li>
     * </ul>
     *
     * @return formatted size string with appropriate unit suffix, never null
     */
    public String getTableSizeDisplay() {
        if (tableSizeBytes < 1024) return tableSizeBytes + " B";
        if (tableSizeBytes < 1024 * 1024) return String.format("%.1f KB", tableSizeBytes / 1024.0);
        if (tableSizeBytes < 1024 * 1024 * 1024) return String.format("%.1f MB", tableSizeBytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", tableSizeBytes / (1024.0 * 1024.0 * 1024.0));
    }
}
