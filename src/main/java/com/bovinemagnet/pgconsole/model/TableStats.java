package com.bovinemagnet.pgconsole.model;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Represents table usage statistics from pg_stat_user_tables.
 * <p>
 * This model captures scan patterns, tuple operations, and bloat metrics
 * for a specific table. It is used to identify tables requiring maintenance
 * or indexing improvements. The statistics are gathered from PostgreSQL's
 * system catalogue view {@code pg_stat_user_tables}, which tracks cumulative
 * statistics for user-defined tables since the last statistics reset.
 * </p>
 * <p>
 * High sequential scan ratios may indicate missing indices, whilst high dead
 * tuple counts suggest the need for vacuuming. The bloat ratio can be
 * calculated via {@link #getBloatRatio()} to assess maintenance requirements.
 * </p>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see TableMaintenanceRecommendation
 * @see IndexRecommendation
 * @since 0.0.0
 */
public class TableStats {

    /** The schema name containing this table. */
    private String schemaName;

    /** The table name. */
    private String tableName;

    /** The number of sequential scans initiated on this table. */
    private long seqScan;

    /** The number of live rows fetched by sequential scans. */
    private long seqTupRead;

    /** The number of index scans initiated on this table. */
    private long idxScan;

    /** The number of live rows fetched by index scans. */
    private long idxTupFetch;

    /** The number of rows inserted into this table. */
    private long nTupIns;

    /** The number of rows updated in this table. */
    private long nTupUpd;

    /** The number of rows deleted from this table. */
    private long nTupDel;

    /** The estimated number of live rows in this table. */
    private long nLiveTup;

    /** The estimated number of dead rows in this table requiring vacuum. */
    private long nDeadTup;

    /** Timestamp of the last manual VACUUM on this table, or null if never vacuumed. */
    private LocalDateTime lastVacuum;

    /** Timestamp of the last autovacuum on this table, or null if never auto-vacuumed. */
    private LocalDateTime lastAutovacuum;

    /** Timestamp of the last manual ANALYZE on this table, or null if never analysed. */
    private LocalDateTime lastAnalyze;

    /** Timestamp of the last auto-analyze on this table, or null if never auto-analysed. */
    private LocalDateTime lastAutoanalyze;

    /**
     * Constructs a new TableStats instance with default values.
     * <p>
     * All numeric fields are initialised to 0, and string fields to null.
     * This constructor is primarily used by JDBC result set mapping.
     * </p>
     */
    public TableStats() {}

    /**
     * Returns the schema name containing this table.
     *
     * @return the schema name
     */
    public String getSchemaName() { return schemaName; }

    /**
     * Sets the schema name containing this table.
     *
     * @param schemaName the schema name
     */
    public void setSchemaName(String schemaName) { this.schemaName = schemaName; }

    /**
     * Returns the table name.
     *
     * @return the table name
     */
    public String getTableName() { return tableName; }

    /**
     * Sets the table name.
     *
     * @param tableName the table name
     */
    public void setTableName(String tableName) { this.tableName = tableName; }

    /**
     * Returns the number of sequential scans initiated on this table.
     *
     * @return the sequential scan count
     */
    public long getSeqScan() { return seqScan; }

    /**
     * Sets the number of sequential scans initiated on this table.
     *
     * @param seqScan the sequential scan count
     */
    public void setSeqScan(long seqScan) { this.seqScan = seqScan; }

    /**
     * Returns the number of live rows fetched by sequential scans.
     *
     * @return the sequential tuple read count
     */
    public long getSeqTupRead() { return seqTupRead; }

    /**
     * Sets the number of live rows fetched by sequential scans.
     *
     * @param seqTupRead the sequential tuple read count
     */
    public void setSeqTupRead(long seqTupRead) { this.seqTupRead = seqTupRead; }

    /**
     * Returns the number of index scans initiated on this table.
     *
     * @return the index scan count
     */
    public long getIdxScan() { return idxScan; }

    /**
     * Sets the number of index scans initiated on this table.
     *
     * @param idxScan the index scan count
     */
    public void setIdxScan(long idxScan) { this.idxScan = idxScan; }

    /**
     * Returns the number of live rows fetched by index scans.
     *
     * @return the index tuple fetch count
     */
    public long getIdxTupFetch() { return idxTupFetch; }

    /**
     * Sets the number of live rows fetched by index scans.
     *
     * @param idxTupFetch the index tuple fetch count
     */
    public void setIdxTupFetch(long idxTupFetch) { this.idxTupFetch = idxTupFetch; }

    /**
     * Returns the number of rows inserted into this table.
     *
     * @return the insert count
     */
    public long getnTupIns() { return nTupIns; }

    /**
     * Sets the number of rows inserted into this table.
     *
     * @param nTupIns the insert count
     */
    public void setnTupIns(long nTupIns) { this.nTupIns = nTupIns; }

    /**
     * Returns the number of rows updated in this table.
     *
     * @return the update count
     */
    public long getnTupUpd() { return nTupUpd; }

    /**
     * Sets the number of rows updated in this table.
     *
     * @param nTupUpd the update count
     */
    public void setnTupUpd(long nTupUpd) { this.nTupUpd = nTupUpd; }

    /**
     * Returns the number of rows deleted from this table.
     *
     * @return the delete count
     */
    public long getnTupDel() { return nTupDel; }

    /**
     * Sets the number of rows deleted from this table.
     *
     * @param nTupDel the delete count
     */
    public void setnTupDel(long nTupDel) { this.nTupDel = nTupDel; }

    /**
     * Returns the estimated number of live rows in this table.
     *
     * @return the live tuple count
     */
    public long getnLiveTup() { return nLiveTup; }

    /**
     * Sets the estimated number of live rows in this table.
     *
     * @param nLiveTup the live tuple count
     */
    public void setnLiveTup(long nLiveTup) { this.nLiveTup = nLiveTup; }

    /**
     * Returns the estimated number of dead rows in this table.
     *
     * @return the dead tuple count
     */
    public long getnDeadTup() { return nDeadTup; }

    /**
     * Sets the estimated number of dead rows in this table.
     *
     * @param nDeadTup the dead tuple count
     */
    public void setnDeadTup(long nDeadTup) { this.nDeadTup = nDeadTup; }

    /**
     * Calculates the table bloat ratio as a percentage.
     * <p>
     * Bloat indicates the proportion of dead tuples to total tuples
     * (live + dead). High bloat suggests the table needs vacuuming to
     * reclaim space and maintain query performance.
     * </p>
     * <p>
     * Typical threshold values:
     * </p>
     * <ul>
     *   <li>0-10%: Normal, no action required</li>
     *   <li>10-20%: Consider scheduling vacuum during maintenance window</li>
     *   <li>20%+: Immediate vacuum recommended to prevent performance degradation</li>
     * </ul>
     * <p>
     * The calculation uses the formula: {@code (nDeadTup / (nLiveTup + nDeadTup)) * 100}
     * </p>
     *
     * @return the bloat ratio as a percentage (0-100), or 0.0 if there are no live tuples
     */
    public double getBloatRatio() {
        if (nLiveTup == 0) return 0.0;
        return (double) nDeadTup / (double) (nLiveTup + nDeadTup) * 100.0;
    }

    /**
     * Returns the dead tuple ratio as a percentage of total tuples.
     *
     * @return the dead tuple percentage (0-100), or 0.0 if no tuples exist
     */
    public double getDeadTupleRatio() {
        long total = nLiveTup + nDeadTup;
        if (total == 0) return 0.0;
        return (double) nDeadTup / (double) total * 100.0;
    }

    /**
     * Returns the dead tuple ratio formatted to one decimal place.
     *
     * @return formatted string (e.g., "12.3")
     */
    public String getDeadTupleRatioFormatted() {
        return String.format("%.1f", getDeadTupleRatio());
    }

    public LocalDateTime getLastVacuum() { return lastVacuum; }
    public void setLastVacuum(LocalDateTime lastVacuum) { this.lastVacuum = lastVacuum; }

    public LocalDateTime getLastAutovacuum() { return lastAutovacuum; }
    public void setLastAutovacuum(LocalDateTime lastAutovacuum) { this.lastAutovacuum = lastAutovacuum; }

    public LocalDateTime getLastAnalyze() { return lastAnalyze; }
    public void setLastAnalyze(LocalDateTime lastAnalyze) { this.lastAnalyze = lastAnalyze; }

    public LocalDateTime getLastAutoanalyze() { return lastAutoanalyze; }
    public void setLastAutoanalyze(LocalDateTime lastAutoanalyze) { this.lastAutoanalyze = lastAutoanalyze; }

    /**
     * Returns the most recent vacuum timestamp (manual or auto).
     *
     * @return the latest vacuum timestamp, or null if never vacuumed
     */
    public LocalDateTime getLastVacuumAny() {
        if (lastVacuum == null) return lastAutovacuum;
        if (lastAutovacuum == null) return lastVacuum;
        return lastVacuum.isAfter(lastAutovacuum) ? lastVacuum : lastAutovacuum;
    }

    /**
     * Returns the most recent analyze timestamp (manual or auto).
     *
     * @return the latest analyze timestamp, or null if never analysed
     */
    public LocalDateTime getLastAnalyzeAny() {
        if (lastAnalyze == null) return lastAutoanalyze;
        if (lastAutoanalyze == null) return lastAnalyze;
        return lastAnalyze.isAfter(lastAutoanalyze) ? lastAnalyze : lastAutoanalyze;
    }

    /**
     * Returns a human-readable relative time string for the last vacuum.
     * <p>
     * Examples: "3h ago", "2d ago", "Never"
     *
     * @return relative time string
     */
    public String getLastVacuumAge() {
        return formatAge(getLastVacuumAny());
    }

    /**
     * Returns a human-readable relative time string for the last analyze.
     *
     * @return relative time string
     */
    public String getLastAnalyzeAge() {
        return formatAge(getLastAnalyzeAny());
    }

    /**
     * Returns a CSS class suffix for vacuum age colouring.
     * <p>
     * Green (&lt;1 day), yellow (1-7 days), red (&gt;7 days or never).
     *
     * @return "success", "warning", or "danger"
     */
    public String getVacuumAgeColour() {
        return ageColour(getLastVacuumAny());
    }

    /**
     * Returns a CSS class suffix for analyze age colouring.
     *
     * @return "success", "warning", or "danger"
     */
    public String getAnalyzeAgeColour() {
        return ageColour(getLastAnalyzeAny());
    }

    private String formatAge(LocalDateTime timestamp) {
        if (timestamp == null) return "Never";
        long hours = ChronoUnit.HOURS.between(timestamp, LocalDateTime.now());
        if (hours < 1) return "<1h ago";
        if (hours < 24) return hours + "h ago";
        long days = hours / 24;
        if (days < 30) return days + "d ago";
        return (days / 30) + "mo ago";
    }

    private String ageColour(LocalDateTime timestamp) {
        if (timestamp == null) return "danger";
        long hours = ChronoUnit.HOURS.between(timestamp, LocalDateTime.now());
        if (hours < 24) return "success";
        if (hours < 168) return "warning"; // 7 days
        return "danger";
    }
}
