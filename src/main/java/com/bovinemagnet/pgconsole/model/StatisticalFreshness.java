package com.bovinemagnet.pgconsole.model;

import java.time.Duration;
import java.time.Instant;

/**
 * Represents table statistics freshness metrics.
 * <p>
 * Tracks when table statistics were last updated and the percentage of rows
 * modified since the last ANALYZE, helping identify tables with stale statistics
 * that may cause poor query planning.
 * </p>
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class StatisticalFreshness {
    private String schemaName;
    private String tableName;
    private Instant lastAnalyze;
    private Instant lastAutoanalyze;
    private long nLiveTup;
    private long nDeadTup;
    private long nModSinceAnalyze;
    private long nInsSinceVacuum;
    private long autoanalyzeCount;
    private long analyzeCount;

    public StatisticalFreshness() {}

    public String getSchemaName() { return schemaName; }
    public void setSchemaName(String schemaName) { this.schemaName = schemaName; }

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }

    public Instant getLastAnalyze() { return lastAnalyze; }
    public void setLastAnalyze(Instant lastAnalyze) { this.lastAnalyze = lastAnalyze; }

    public Instant getLastAutoanalyze() { return lastAutoanalyze; }
    public void setLastAutoanalyze(Instant lastAutoanalyze) { this.lastAutoanalyze = lastAutoanalyze; }

    public long getnLiveTup() { return nLiveTup; }
    public void setnLiveTup(long nLiveTup) { this.nLiveTup = nLiveTup; }

    public long getnDeadTup() { return nDeadTup; }
    public void setnDeadTup(long nDeadTup) { this.nDeadTup = nDeadTup; }

    public long getnModSinceAnalyze() { return nModSinceAnalyze; }
    public void setnModSinceAnalyze(long nModSinceAnalyze) { this.nModSinceAnalyze = nModSinceAnalyze; }

    public long getnInsSinceVacuum() { return nInsSinceVacuum; }
    public void setnInsSinceVacuum(long nInsSinceVacuum) { this.nInsSinceVacuum = nInsSinceVacuum; }

    public long getAutoanalyzeCount() { return autoanalyzeCount; }
    public void setAutoanalyzeCount(long autoanalyzeCount) { this.autoanalyzeCount = autoanalyzeCount; }

    public long getAnalyzeCount() { return analyzeCount; }
    public void setAnalyzeCount(long analyzeCount) { this.analyzeCount = analyzeCount; }

    /**
     * Returns the fully qualified table name.
     *
     * @return schema.table format
     */
    public String getFullTableName() {
        return schemaName + "." + tableName;
    }

    /**
     * Returns the most recent analyze timestamp (manual or auto).
     *
     * @return most recent analyze time, or null if never analysed
     */
    public Instant getLastAnyAnalyze() {
        if (lastAnalyze == null) return lastAutoanalyze;
        if (lastAutoanalyze == null) return lastAnalyze;
        return lastAnalyze.isAfter(lastAutoanalyze) ? lastAnalyze : lastAutoanalyze;
    }

    /**
     * Returns the percentage of rows modified since last ANALYZE.
     *
     * @return modification percentage (0-100+)
     */
    public double getModifiedPercent() {
        if (nLiveTup == 0) return 0.0;
        return (double) nModSinceAnalyze / (double) nLiveTup * 100.0;
    }

    /**
     * Returns the days since last analyze.
     *
     * @return days since analyze, or -1 if never analysed
     */
    public long getDaysSinceAnalyze() {
        Instant last = getLastAnyAnalyze();
        if (last == null) return -1;
        return Duration.between(last, Instant.now()).toDays();
    }

    /**
     * Returns a human-readable description of time since last analyze.
     *
     * @return formatted time string
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
     * Predicts whether autoanalyze will trigger soon.
     * <p>
     * PostgreSQL triggers autoanalyze when:
     * n_mod_since_analyze >= autovacuum_analyze_threshold + autovacuum_analyze_scale_factor * reltuples
     * Default: 50 + 0.1 * reltuples
     * </p>
     *
     * @return true if autoanalyze is likely to trigger soon
     */
    public boolean isAutoanalyzeImminent() {
        // Default PostgreSQL threshold: 50 + 10% of rows
        long threshold = 50 + (long)(nLiveTup * 0.1);
        return nModSinceAnalyze >= threshold * 0.8; // Within 80% of threshold
    }

    /**
     * Returns percentage progress toward autoanalyze threshold.
     *
     * @return percentage (0-100+)
     */
    public double getAutoanalyzeProgress() {
        long threshold = 50 + (long)(nLiveTup * 0.1);
        if (threshold == 0) return 0.0;
        return Math.min(100.0, (double) nModSinceAnalyze / threshold * 100.0);
    }

    /**
     * Returns the staleness severity level.
     *
     * @param warnThreshold percentage threshold for warning
     * @return "critical", "warning", or "ok"
     */
    public String getStaleness(double warnThreshold) {
        double modPercent = getModifiedPercent();
        if (modPercent >= warnThreshold * 3) return "critical";
        if (modPercent >= warnThreshold) return "warning";
        return "ok";
    }

    /**
     * Returns the CSS class for styling based on staleness.
     *
     * @param warnThreshold percentage threshold for warning
     * @return CSS class name
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
     * Returns the priority score for ranking tables needing ANALYZE.
     * Higher score = more urgent need for ANALYZE.
     *
     * @return priority score
     */
    public double getPriorityScore() {
        double modScore = getModifiedPercent();
        double ageScore = getDaysSinceAnalyze() > 0 ? getDaysSinceAnalyze() * 2 : 0;
        double sizeScore = Math.log10(Math.max(1, nLiveTup));
        return modScore + ageScore + sizeScore;
    }
}
