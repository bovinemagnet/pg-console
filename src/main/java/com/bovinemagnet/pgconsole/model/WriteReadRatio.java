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
     * Workload pattern classification.
     */
    public enum WorkloadPattern {
        READ_HEAVY,     // Predominantly reads
        WRITE_HEAVY,    // Predominantly writes
        BALANCED,       // Mixed workload
        IDLE            // Very low activity
    }

    private String schemaName;
    private String tableName;
    private long seqScan;
    private long seqTupRead;
    private long idxScan;
    private long idxTupFetch;
    private long nTupIns;
    private long nTupUpd;
    private long nTupDel;
    private long nTupHotUpd;
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
