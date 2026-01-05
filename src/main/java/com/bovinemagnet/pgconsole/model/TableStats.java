package com.bovinemagnet.pgconsole.model;

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
}
