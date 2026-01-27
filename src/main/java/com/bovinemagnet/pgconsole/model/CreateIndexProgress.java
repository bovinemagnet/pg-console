package com.bovinemagnet.pgconsole.model;

/**
 * Represents index creation progress from the PostgreSQL system view
 * {@code pg_stat_progress_create_index}.
 * <p>
 * This class captures detailed information about ongoing CREATE INDEX and
 * REINDEX operations, including the current phase and progress metrics.
 * Available in PostgreSQL 12+.
 * <p>
 * The index creation process proceeds through several phases:
 * <ul>
 *   <li>Initializing</li>
 *   <li>Waiting for writers before build</li>
 *   <li>Building index</li>
 *   <li>Waiting for writers before validation</li>
 *   <li>Index validation: scanning index</li>
 *   <li>Index validation: sorting tuples</li>
 *   <li>Index validation: scanning table</li>
 *   <li>Waiting for old snapshots</li>
 *   <li>Waiting for readers before marking dead</li>
 *   <li>Waiting for readers before dropping</li>
 * </ul>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see <a href="https://www.postgresql.org/docs/current/progress-reporting.html#CREATE-INDEX-PROGRESS-REPORTING">CREATE INDEX Progress Reporting</a>
 */
public class CreateIndexProgress {

    /** Process ID of the backend creating the index */
    private int pid;

    /** Name of the database */
    private String database;

    /** Schema name of the table */
    private String schemaName;

    /** Name of the table being indexed */
    private String tableName;

    /** Name of the index being created */
    private String indexName;

    /** Current phase of the operation */
    private String phase;

    /** Number of lockers waited for */
    private long lockersTotal;

    /** Number of lockers already waited for */
    private long lockersDone;

    /** Current locker's PID being waited for */
    private int currentLockerPid;

    /** Total number of blocks to be processed */
    private long blocksTotal;

    /** Number of blocks already processed */
    private long blocksDone;

    /** Total number of tuples to be processed */
    private long tuplesTotal;

    /** Number of tuples already processed */
    private long tuplesDone;

    /** Number of partitions in a partitioned index */
    private long partitionsTotal;

    /** Number of partitions already processed */
    private long partitionsDone;

    /** Computed progress percentage */
    private double progressPercent;

    /**
     * Constructs a new CreateIndexProgress instance.
     */
    public CreateIndexProgress() {
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

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

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public long getLockersTotal() {
        return lockersTotal;
    }

    public void setLockersTotal(long lockersTotal) {
        this.lockersTotal = lockersTotal;
    }

    public long getLockersDone() {
        return lockersDone;
    }

    public void setLockersDone(long lockersDone) {
        this.lockersDone = lockersDone;
    }

    public int getCurrentLockerPid() {
        return currentLockerPid;
    }

    public void setCurrentLockerPid(int currentLockerPid) {
        this.currentLockerPid = currentLockerPid;
    }

    public long getBlocksTotal() {
        return blocksTotal;
    }

    public void setBlocksTotal(long blocksTotal) {
        this.blocksTotal = blocksTotal;
    }

    public long getBlocksDone() {
        return blocksDone;
    }

    public void setBlocksDone(long blocksDone) {
        this.blocksDone = blocksDone;
    }

    public long getTuplesTotal() {
        return tuplesTotal;
    }

    public void setTuplesTotal(long tuplesTotal) {
        this.tuplesTotal = tuplesTotal;
    }

    public long getTuplesDone() {
        return tuplesDone;
    }

    public void setTuplesDone(long tuplesDone) {
        this.tuplesDone = tuplesDone;
    }

    public long getPartitionsTotal() {
        return partitionsTotal;
    }

    public void setPartitionsTotal(long partitionsTotal) {
        this.partitionsTotal = partitionsTotal;
    }

    public long getPartitionsDone() {
        return partitionsDone;
    }

    public void setPartitionsDone(long partitionsDone) {
        this.partitionsDone = partitionsDone;
    }

    public double getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(double progressPercent) {
        this.progressPercent = progressPercent;
    }

    /**
     * Calculates progress based on blocks or tuples processed.
     */
    public void calculateProgress() {
        if (blocksTotal > 0) {
            this.progressPercent = (blocksDone * 100.0) / blocksTotal;
        } else if (tuplesTotal > 0) {
            this.progressPercent = (tuplesDone * 100.0) / tuplesTotal;
        } else {
            this.progressPercent = 0;
        }
    }

    /**
     * Returns the fully qualified table name.
     *
     * @return schema.table format
     */
    public String getFullTableName() {
        if (schemaName != null && !schemaName.isEmpty()) {
            return schemaName + "." + tableName;
        }
        return tableName;
    }

    /**
     * Returns formatted progress percentage.
     *
     * @return progress with one decimal place
     */
    public String getProgressFormatted() {
        return String.format("%.1f%%", progressPercent);
    }

    /**
     * Returns Bootstrap CSS class for the progress bar.
     *
     * @return Bootstrap background class
     */
    public String getProgressBarCssClass() {
        if (progressPercent >= 90) return "bg-success";
        if (progressPercent >= 50) return "bg-info";
        return "bg-primary";
    }

    /**
     * Returns Bootstrap CSS class for the phase badge.
     *
     * @return Bootstrap background class
     */
    public String getPhaseCssClass() {
        if (phase == null) return "bg-secondary";
        if (phase.contains("building")) return "bg-primary";
        if (phase.contains("waiting")) return "bg-warning text-dark";
        if (phase.contains("validation")) return "bg-info";
        return "bg-secondary";
    }
}
