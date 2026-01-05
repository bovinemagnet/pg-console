package com.bovinemagnet.pgconsole.model;

import java.time.Duration;
import java.time.Instant;

/**
 * Represents table statistics freshness metrics from PostgreSQL's pg_stat_user_tables view.
 * <p>
 * Tracks when table statistics were last updated and the percentage of rows
 * modified since the last ANALYZE, helping identify tables with stale statistics
 * that may cause poor query planning. PostgreSQL's query planner relies on accurate
 * statistics to choose optimal execution plans; stale statistics can lead to
 * inefficient query execution.
 * </p>
 * <p>
 * This model provides utility methods to:
 * <ul>
 * <li>Determine how long since the last ANALYZE operation</li>
 * <li>Calculate the percentage of rows modified since last ANALYZE</li>
 * <li>Predict when autovacuum will trigger an ANALYZE</li>
 * <li>Assess the urgency of running a manual ANALYZE</li>
 * </ul>
 * </p>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see PostgresService
 */
public class StatisticalFreshness {
    /**
     * The schema name containing the table.
     */
    private String schemaName;

    /**
     * The table name (without schema qualification).
     */
    private String tableName;

    /**
     * Timestamp of the last manual ANALYZE operation.
     * Null if never manually analysed.
     */
    private Instant lastAnalyze;

    /**
     * Timestamp of the last automatic ANALYZE operation by autovacuum.
     * Null if never auto-analysed.
     */
    private Instant lastAutoanalyze;

    /**
     * Estimated number of live (current) rows in the table.
     * This value comes from pg_stat_user_tables.n_live_tup.
     */
    private long nLiveTup;

    /**
     * Estimated number of dead (obsolete) rows in the table.
     * This value comes from pg_stat_user_tables.n_dead_tup.
     */
    private long nDeadTup;

    /**
     * Number of rows modified (inserted, updated, deleted) since the last ANALYZE.
     * This value comes from pg_stat_user_tables.n_mod_since_analyze.
     */
    private long nModSinceAnalyze;

    /**
     * Number of rows inserted since the last VACUUM.
     * This value comes from pg_stat_user_tables.n_ins_since_vacuum.
     */
    private long nInsSinceVacuum;

    /**
     * Total number of times this table has been automatically analysed by autovacuum.
     */
    private long autoanalyzeCount;

    /**
     * Total number of times this table has been manually analysed.
     */
    private long analyzeCount;

    /**
     * Constructs a new StatisticalFreshness instance with default values.
     */
    public StatisticalFreshness() {}

    /**
     * Returns the schema name containing the table.
     *
     * @return the schema name
     */
    public String getSchemaName() { return schemaName; }

    /**
     * Sets the schema name containing the table.
     *
     * @param schemaName the schema name to set
     */
    public void setSchemaName(String schemaName) { this.schemaName = schemaName; }

    /**
     * Returns the table name (without schema qualification).
     *
     * @return the table name
     */
    public String getTableName() { return tableName; }

    /**
     * Sets the table name (without schema qualification).
     *
     * @param tableName the table name to set
     */
    public void setTableName(String tableName) { this.tableName = tableName; }

    /**
     * Returns the timestamp of the last manual ANALYZE operation.
     *
     * @return the last manual ANALYZE timestamp, or null if never manually analysed
     */
    public Instant getLastAnalyze() { return lastAnalyze; }

    /**
     * Sets the timestamp of the last manual ANALYZE operation.
     *
     * @param lastAnalyze the last manual ANALYZE timestamp to set, or null if never analysed
     */
    public void setLastAnalyze(Instant lastAnalyze) { this.lastAnalyze = lastAnalyze; }

    /**
     * Returns the timestamp of the last automatic ANALYZE operation by autovacuum.
     *
     * @return the last automatic ANALYZE timestamp, or null if never auto-analysed
     */
    public Instant getLastAutoanalyze() { return lastAutoanalyze; }

    /**
     * Sets the timestamp of the last automatic ANALYZE operation by autovacuum.
     *
     * @param lastAutoanalyze the last automatic ANALYZE timestamp to set, or null if never auto-analysed
     */
    public void setLastAutoanalyze(Instant lastAutoanalyze) { this.lastAutoanalyze = lastAutoanalyze; }

    /**
     * Returns the estimated number of live (current) rows in the table.
     *
     * @return the number of live rows
     */
    public long getnLiveTup() { return nLiveTup; }

    /**
     * Sets the estimated number of live (current) rows in the table.
     *
     * @param nLiveTup the number of live rows to set
     */
    public void setnLiveTup(long nLiveTup) { this.nLiveTup = nLiveTup; }

    /**
     * Returns the estimated number of dead (obsolete) rows in the table.
     *
     * @return the number of dead rows
     */
    public long getnDeadTup() { return nDeadTup; }

    /**
     * Sets the estimated number of dead (obsolete) rows in the table.
     *
     * @param nDeadTup the number of dead rows to set
     */
    public void setnDeadTup(long nDeadTup) { this.nDeadTup = nDeadTup; }

    /**
     * Returns the number of rows modified since the last ANALYZE.
     *
     * @return the number of rows modified (inserted, updated, deleted) since last ANALYZE
     */
    public long getnModSinceAnalyze() { return nModSinceAnalyze; }

    /**
     * Sets the number of rows modified since the last ANALYZE.
     *
     * @param nModSinceAnalyze the number of modified rows to set
     */
    public void setnModSinceAnalyze(long nModSinceAnalyze) { this.nModSinceAnalyze = nModSinceAnalyze; }

    /**
     * Returns the number of rows inserted since the last VACUUM.
     *
     * @return the number of rows inserted since last VACUUM
     */
    public long getnInsSinceVacuum() { return nInsSinceVacuum; }

    /**
     * Sets the number of rows inserted since the last VACUUM.
     *
     * @param nInsSinceVacuum the number of inserted rows to set
     */
    public void setnInsSinceVacuum(long nInsSinceVacuum) { this.nInsSinceVacuum = nInsSinceVacuum; }

    /**
     * Returns the total number of times this table has been automatically analysed.
     *
     * @return the automatic ANALYZE count
     */
    public long getAutoanalyzeCount() { return autoanalyzeCount; }

    /**
     * Sets the total number of times this table has been automatically analysed.
     *
     * @param autoanalyzeCount the automatic ANALYZE count to set
     */
    public void setAutoanalyzeCount(long autoanalyzeCount) { this.autoanalyzeCount = autoanalyzeCount; }

    /**
     * Returns the total number of times this table has been manually analysed.
     *
     * @return the manual ANALYZE count
     */
    public long getAnalyzeCount() { return analyzeCount; }

    /**
     * Sets the total number of times this table has been manually analysed.
     *
     * @param analyzeCount the manual ANALYZE count to set
     */
    public void setAnalyzeCount(long analyzeCount) { this.analyzeCount = analyzeCount; }

    /**
     * Returns the fully qualified table name in schema.table format.
     * <p>
     * This is useful for displaying or referencing the table in SQL queries.
     * </p>
     *
     * @return the fully qualified table name (e.g., "public.users")
     */
    public String getFullTableName() {
        return schemaName + "." + tableName;
    }

    /**
     * Returns the most recent ANALYZE timestamp, whether manual or automatic.
     * <p>
     * This method examines both {@link #lastAnalyze} and {@link #lastAutoanalyze}
     * and returns whichever occurred most recently. If only one is present, that
     * value is returned.
     * </p>
     *
     * @return the most recent ANALYZE timestamp, or null if the table has never been analysed
     * @see #getLastAnalyze()
     * @see #getLastAutoanalyze()
     */
    public Instant getLastAnyAnalyze() {
        if (lastAnalyze == null) return lastAutoanalyze;
        if (lastAutoanalyze == null) return lastAnalyze;
        return lastAnalyze.isAfter(lastAutoanalyze) ? lastAnalyze : lastAutoanalyze;
    }

    /**
     * Calculates the percentage of rows modified since the last ANALYZE operation.
     * <p>
     * This metric indicates how stale the table statistics are. A high percentage
     * suggests the statistics may not accurately reflect the current table state,
     * which can lead to suboptimal query plans. Generally, statistics should be
     * updated when this percentage exceeds 10-20%.
     * </p>
     *
     * @return the modification percentage (0.0 to 100.0+), or 0.0 if the table has no live rows
     */
    public double getModifiedPercent() {
        if (nLiveTup == 0) return 0.0;
        return (double) nModSinceAnalyze / (double) nLiveTup * 100.0;
    }

    /**
     * Calculates the number of days since the last ANALYZE operation.
     * <p>
     * This considers both manual and automatic ANALYZE operations, using the
     * most recent timestamp from either source.
     * </p>
     *
     * @return the number of days since the last ANALYZE, or -1 if the table has never been analysed
     * @see #getLastAnyAnalyze()
     */
    public long getDaysSinceAnalyze() {
        Instant last = getLastAnyAnalyze();
        if (last == null) return -1;
        return Duration.between(last, Instant.now()).toDays();
    }

    /**
     * Returns a human-readable description of the time since the last ANALYZE operation.
     * <p>
     * The format automatically adjusts based on the duration:
     * <ul>
     * <li>Days if &gt;= 1 day (e.g., "5 days ago")</li>
     * <li>Hours if &lt; 1 day and &gt;= 1 hour (e.g., "3 hours ago")</li>
     * <li>Minutes if &lt; 1 hour (e.g., "45 minutes ago")</li>
     * <li>"Never" if the table has never been analysed</li>
     * </ul>
     * </p>
     *
     * @return a formatted string describing when the last ANALYZE occurred
     * @see #getLastAnyAnalyze()
     */
    public String getLastAnalyzeDisplay() {
        Instant last = getLastAnyAnalyze();
        if (last == null) return "Never";

        Duration age = Duration.between(last, Instant.now());
        long days = age.toDays();
        long hours = age.toHours() % 24;

        if (days > 0) {
            return String.format("%d days ago", days);
        } else if (hours > 0) {
            return String.format("%d hours ago", hours);
        } else {
            long minutes = age.toMinutes();
            return String.format("%d minutes ago", minutes);
        }
    }

    /**
     * Predicts whether autovacuum's automatic ANALYZE will trigger soon.
     * <p>
     * PostgreSQL's autovacuum triggers an automatic ANALYZE when the following
     * condition is met:
     * <pre>{@code
     * n_mod_since_analyze >= autovacuum_analyze_threshold + (autovacuum_analyze_scale_factor * reltuples)
     * }</pre>
     * With default settings, this is: {@code 50 + (0.1 * reltuples)}
     * </p>
     * <p>
     * This method returns true when modifications have reached 80% of the threshold,
     * indicating that an automatic ANALYZE is likely imminent.
     * </p>
     *
     * @return true if autoanalyze is likely to trigger soon (within 80% of threshold), false otherwise
     * @see #getAutoanalyzeProgress()
     */
    public boolean isAutoanalyzeImminent() {
        // Default PostgreSQL threshold: 50 + 10% of rows
        long threshold = 50 + (long)(nLiveTup * 0.1);
        return nModSinceAnalyze >= threshold * 0.8; // Within 80% of threshold
    }

    /**
     * Calculates the percentage progress toward the autoanalyze threshold.
     * <p>
     * This uses the default PostgreSQL autovacuum threshold formula:
     * {@code 50 + (0.1 * n_live_tup)}. A value approaching 100% indicates
     * that an automatic ANALYZE will trigger soon.
     * </p>
     *
     * @return the percentage progress toward autoanalyze (0.0 to 100.0), capped at 100.0
     * @see #isAutoanalyzeImminent()
     */
    public double getAutoanalyzeProgress() {
        long threshold = 50 + (long)(nLiveTup * 0.1);
        if (threshold == 0) return 0.0;
        return Math.min(100.0, (double) nModSinceAnalyze / threshold * 100.0);
    }

    /**
     * Determines the staleness severity level based on the modification percentage.
     * <p>
     * The severity is categorised as:
     * <ul>
     * <li>"critical" - modifications exceed 3x the warning threshold</li>
     * <li>"warning" - modifications exceed the warning threshold</li>
     * <li>"ok" - modifications are below the warning threshold</li>
     * </ul>
     * </p>
     *
     * @param warnThreshold the percentage threshold for considering statistics stale (e.g., 10.0 for 10%)
     * @return "critical", "warning", or "ok"
     * @see #getModifiedPercent()
     * @see #getStalenessClass(double)
     */
    public String getStaleness(double warnThreshold) {
        double modPercent = getModifiedPercent();
        if (modPercent >= warnThreshold * 3) return "critical";
        if (modPercent >= warnThreshold) return "warning";
        return "ok";
    }

    /**
     * Returns the Bootstrap CSS class for styling based on staleness severity.
     * <p>
     * Maps the staleness level to appropriate Bootstrap colour classes:
     * <ul>
     * <li>"text-danger" for critical staleness</li>
     * <li>"text-warning" for warning staleness</li>
     * <li>"text-success" for ok/acceptable staleness</li>
     * </ul>
     * </p>
     *
     * @param warnThreshold the percentage threshold for considering statistics stale (e.g., 10.0 for 10%)
     * @return the Bootstrap CSS class name for styling
     * @see #getStaleness(double)
     */
    public String getStalenessClass(double warnThreshold) {
        String staleness = getStaleness(warnThreshold);
        return switch (staleness) {
            case "critical" -> "text-danger";
            case "warning" -> "text-warning";
            default -> "text-success";
        };
    }

    /**
     * Calculates a priority score for ranking tables by their need for ANALYZE.
     * <p>
     * The score combines multiple factors:
     * <ul>
     * <li>Modification percentage - how much the table has changed</li>
     * <li>Age score (days since ANALYZE × 2) - how long since statistics were updated</li>
     * <li>Size score (log10 of live rows) - larger tables have more impact on performance</li>
     * </ul>
     * </p>
     * <p>
     * Higher scores indicate more urgent need for ANALYZE. This helps prioritise
     * maintenance efforts on tables where stale statistics are most likely to
     * impact query performance.
     * </p>
     *
     * @return the priority score (unbounded, higher values indicate greater urgency)
     * @see #getModifiedPercent()
     * @see #getDaysSinceAnalyze()
     */
    public double getPriorityScore() {
        double modScore = getModifiedPercent();
        double ageScore = getDaysSinceAnalyze() > 0 ? getDaysSinceAnalyze() * 2 : 0;
        double sizeScore = Math.log10(Math.max(1, nLiveTup));
        return modScore + ageScore + sizeScore;
    }
}
