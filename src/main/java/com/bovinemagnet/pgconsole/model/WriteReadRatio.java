package com.bovinemagnet.pgconsole.model;

/**
 * Represents write/read ratio analysis for table workload classification.
 * <p>
 * Analyses the balance between write operations (INSERT, UPDATE, DELETE)
 * and read operations (sequential scans, index scans) to identify
 * write-heavy vs read-heavy tables for optimisation purposes.
 * </p>
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class WriteReadRatio {

    /**
     * Workload pattern classification based on the ratio of write to read operations.
     * <p>
     * Patterns are determined by analysing the percentage of write operations:
     * <ul>
     * <li>WRITE_HEAVY: &gt;= 70% writes - tables with high INSERT/UPDATE/DELETE activity</li>
     * <li>READ_HEAVY: &lt;= 30% writes - tables with high SELECT activity</li>
     * <li>BALANCED: 30-70% writes - mixed read/write workload</li>
     * <li>IDLE: &lt; 100 total operations - low activity tables</li>
     * </ul>
     * </p>
     *
     * @see #getWorkloadPattern()
     */
    public enum WorkloadPattern {
        /** Predominantly read operations (&lt;= 30% writes). Optimise for query performance and indexing. */
        READ_HEAVY,

        /** Predominantly write operations (&gt;= 70% writes). Optimise for write throughput and vacuum tuning. */
        WRITE_HEAVY,

        /** Mixed workload (30-70% writes). Balance between read and write optimisations. */
        BALANCED,

        /** Very low activity (&lt; 100 total operations). May not require optimisation. */
        IDLE
    }

    /** Schema name of the table. */
    private String schemaName;

    /** Table name without schema qualification. */
    private String tableName;

    /** Number of sequential scans initiated on this table (from pg_stat_user_tables.seq_scan). */
    private long seqScan;

    /** Number of live tuples fetched by sequential scans (from pg_stat_user_tables.seq_tup_read). */
    private long seqTupRead;

    /** Number of index scans initiated on this table (from pg_stat_user_tables.idx_scan). */
    private long idxScan;

    /** Number of live tuples fetched by index scans (from pg_stat_user_tables.idx_tup_fetch). */
    private long idxTupFetch;

    /** Number of tuples inserted (from pg_stat_user_tables.n_tup_ins). */
    private long nTupIns;

    /** Number of tuples updated (from pg_stat_user_tables.n_tup_upd). */
    private long nTupUpd;

    /** Number of tuples deleted (from pg_stat_user_tables.n_tup_del). */
    private long nTupDel;

    /** Number of tuples HOT (Heap-Only Tuple) updated (from pg_stat_user_tables.n_tup_hot_upd). */
    private long nTupHotUpd;

    /** Estimated number of live tuples (from pg_stat_user_tables.n_live_tup). */
    private long nLiveTup;

    public WriteReadRatio() {}

    public String getSchemaName() { return schemaName; }
    public void setSchemaName(String schemaName) { this.schemaName = schemaName; }

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }

    public long getSeqScan() { return seqScan; }
    public void setSeqScan(long seqScan) { this.seqScan = seqScan; }

    public long getSeqTupRead() { return seqTupRead; }
    public void setSeqTupRead(long seqTupRead) { this.seqTupRead = seqTupRead; }

    public long getIdxScan() { return idxScan; }
    public void setIdxScan(long idxScan) { this.idxScan = idxScan; }

    public long getIdxTupFetch() { return idxTupFetch; }
    public void setIdxTupFetch(long idxTupFetch) { this.idxTupFetch = idxTupFetch; }

    public long getnTupIns() { return nTupIns; }
    public void setnTupIns(long nTupIns) { this.nTupIns = nTupIns; }

    public long getnTupUpd() { return nTupUpd; }
    public void setnTupUpd(long nTupUpd) { this.nTupUpd = nTupUpd; }

    public long getnTupDel() { return nTupDel; }
    public void setnTupDel(long nTupDel) { this.nTupDel = nTupDel; }

    public long getnTupHotUpd() { return nTupHotUpd; }
    public void setnTupHotUpd(long nTupHotUpd) { this.nTupHotUpd = nTupHotUpd; }

    public long getnLiveTup() { return nLiveTup; }
    public void setnLiveTup(long nLiveTup) { this.nLiveTup = nLiveTup; }

    /**
     * Returns the fully qualified table name.
     *
     * @return schema.table format
     */
    public String getFullTableName() {
        return schemaName + "." + tableName;
    }

    /**
     * Returns the total number of write operations.
     *
     * @return total writes (inserts + updates + deletes)
     */
    public long getTotalWrites() {
        return nTupIns + nTupUpd + nTupDel;
    }

    /**
     * Returns the total number of read operations (scans).
     *
     * @return total reads (sequential + index scans)
     */
    public long getTotalReads() {
        return seqScan + idxScan;
    }

    /**
     * Returns the total tuples read.
     *
     * @return total tuples read
     */
    public long getTotalTuplesRead() {
        return seqTupRead + idxTupFetch;
    }

    /**
     * Returns the write ratio as a percentage of total operations.
     *
     * @return write percentage (0-100)
     */
    public double getWritePercent() {
        long total = getTotalWrites() + getTotalReads();
        if (total == 0) return 0.0;
        return (double) getTotalWrites() / total * 100.0;
    }

    /**
     * Returns the read ratio as a percentage of total operations.
     *
     * @return read percentage (0-100)
     */
    public double getReadPercent() {
        return 100.0 - getWritePercent();
    }

    /**
     * Classifies the workload pattern based on write/read ratio.
     *
     * @return workload pattern classification
     */
    public WorkloadPattern getWorkloadPattern() {
        long total = getTotalWrites() + getTotalReads();
        if (total < 100) return WorkloadPattern.IDLE;

        double writePercent = getWritePercent();
        if (writePercent >= 70) return WorkloadPattern.WRITE_HEAVY;
        if (writePercent <= 30) return WorkloadPattern.READ_HEAVY;
        return WorkloadPattern.BALANCED;
    }

    /**
     * Returns a human-readable workload description.
     *
     * @return workload description
     */
    public String getWorkloadDisplay() {
        return switch (getWorkloadPattern()) {
            case READ_HEAVY -> "Read-Heavy";
            case WRITE_HEAVY -> "Write-Heavy";
            case BALANCED -> "Balanced";
            case IDLE -> "Low Activity";
        };
    }

    /**
     * Returns the CSS class for styling based on workload pattern.
     *
     * @return CSS class name
     */
    public String getWorkloadClass() {
        return switch (getWorkloadPattern()) {
            case READ_HEAVY -> "text-info";
            case WRITE_HEAVY -> "text-warning";
            case BALANCED -> "text-success";
            case IDLE -> "text-muted";
        };
    }

    /**
     * Returns the dominant operation type.
     *
     * @return "INSERT", "UPDATE", "DELETE", "READ", or "NONE"
     */
    public String getDominantOperation() {
        long maxWrite = Math.max(nTupIns, Math.max(nTupUpd, nTupDel));
        long maxRead = getTotalReads();

        if (maxWrite == 0 && maxRead == 0) return "NONE";

        if (maxRead > maxWrite) return "READ";
        if (nTupIns >= nTupUpd && nTupIns >= nTupDel) return "INSERT";
        if (nTupUpd >= nTupIns && nTupUpd >= nTupDel) return "UPDATE";
        return "DELETE";
    }

    /**
     * Returns formatted operation counts for display.
     *
     * @return formatted string like "I:1.2M U:500K D:10K"
     */
    public String getOperationBreakdown() {
        return String.format("I:%s U:%s D:%s",
                formatNumber(nTupIns),
                formatNumber(nTupUpd),
                formatNumber(nTupDel));
    }

    /**
     * Returns the index usage ratio (index scans / total scans).
     *
     * @return index usage percentage (0-100)
     */
    public double getIndexUsagePercent() {
        long totalScans = seqScan + idxScan;
        if (totalScans == 0) return 0.0;
        return (double) idxScan / totalScans * 100.0;
    }

    private String formatNumber(long num) {
        if (num < 1000) return String.valueOf(num);
        if (num < 1000000) return String.format("%.1fK", num / 1000.0);
        if (num < 1000000000) return String.format("%.1fM", num / 1000000.0);
        return String.format("%.1fB", num / 1000000000.0);
    }
}
