package com.bovinemagnet.pgconsole.model;

/**
 * Represents CLUSTER and VACUUM FULL progress from the PostgreSQL system view
 * {@code pg_stat_progress_cluster}.
 * <p>
 * This class captures detailed information about ongoing CLUSTER and VACUUM FULL
 * operations, including the current phase and progress metrics.
 * Available in PostgreSQL 12+.
 * <p>
 * The operation proceeds through several phases:
 * <ul>
 *   <li>Initializing</li>
 *   <li>Seq Scanning Heap</li>
 *   <li>Index Scanning Heap</li>
 *   <li>Sorting Tuples</li>
 *   <li>Writing New Heap</li>
 *   <li>Swapping Relation Files</li>
 *   <li>Rebuilding Index</li>
 *   <li>Performing Final Cleanup</li>
 * </ul>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see <a href="https://www.postgresql.org/docs/current/progress-reporting.html#CLUSTER-PROGRESS-REPORTING">CLUSTER Progress Reporting</a>
 */
public class ClusterProgress {

    /** Process ID of the backend performing the operation */
    private int pid;

    /** Name of the database */
    private String database;

    /** Schema name of the table */
    private String schemaName;

    /** Name of the table being clustered */
    private String tableName;

    /** Type of operation: CLUSTER or VACUUM FULL */
    private String command;

    /** Current phase of the operation */
    private String phase;

    /** Index used for clustering (null for VACUUM FULL) */
    private String clusterIndexName;

    /** Total number of heap blocks */
    private long heapBlksTotal;

    /** Number of heap blocks scanned */
    private long heapBlksScanned;

    /** Total number of heap tuples */
    private long heapTuplesTotal;

    /** Number of heap tuples scanned */
    private long heapTuplesScanned;

    /** Number of heap tuples written */
    private long heapTuplesWritten;

    /** Index being rebuilt (during rebuild phase) */
    private String indexRebuildName;

    /** Computed progress percentage */
    private double progressPercent;

    /**
     * Constructs a new ClusterProgress instance.
     */
    public ClusterProgress() {
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

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getClusterIndexName() {
        return clusterIndexName;
    }

    public void setClusterIndexName(String clusterIndexName) {
        this.clusterIndexName = clusterIndexName;
    }

    public long getHeapBlksTotal() {
        return heapBlksTotal;
    }

    public void setHeapBlksTotal(long heapBlksTotal) {
        this.heapBlksTotal = heapBlksTotal;
    }

    public long getHeapBlksScanned() {
        return heapBlksScanned;
    }

    public void setHeapBlksScanned(long heapBlksScanned) {
        this.heapBlksScanned = heapBlksScanned;
    }

    public long getHeapTuplesTotal() {
        return heapTuplesTotal;
    }

    public void setHeapTuplesTotal(long heapTuplesTotal) {
        this.heapTuplesTotal = heapTuplesTotal;
    }

    public long getHeapTuplesScanned() {
        return heapTuplesScanned;
    }

    public void setHeapTuplesScanned(long heapTuplesScanned) {
        this.heapTuplesScanned = heapTuplesScanned;
    }

    public long getHeapTuplesWritten() {
        return heapTuplesWritten;
    }

    public void setHeapTuplesWritten(long heapTuplesWritten) {
        this.heapTuplesWritten = heapTuplesWritten;
    }

    public String getIndexRebuildName() {
        return indexRebuildName;
    }

    public void setIndexRebuildName(String indexRebuildName) {
        this.indexRebuildName = indexRebuildName;
    }

    public double getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(double progressPercent) {
        this.progressPercent = progressPercent;
    }

    /**
     * Calculates progress based on heap blocks or tuples processed.
     */
    public void calculateProgress() {
        if (heapBlksTotal > 0) {
            this.progressPercent = (heapBlksScanned * 100.0) / heapBlksTotal;
        } else if (heapTuplesTotal > 0) {
            this.progressPercent = (heapTuplesScanned * 100.0) / heapTuplesTotal;
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
     * Returns Bootstrap CSS class for the command badge.
     *
     * @return Bootstrap background class
     */
    public String getCommandCssClass() {
        if ("CLUSTER".equalsIgnoreCase(command)) return "bg-primary";
        if ("VACUUM FULL".equalsIgnoreCase(command)) return "bg-warning text-dark";
        return "bg-secondary";
    }

    /**
     * Returns Bootstrap CSS class for the phase badge.
     *
     * @return Bootstrap background class
     */
    public String getPhaseCssClass() {
        if (phase == null) return "bg-secondary";
        if (phase.toLowerCase().contains("scanning")) return "bg-info";
        if (phase.toLowerCase().contains("sorting")) return "bg-warning text-dark";
        if (phase.toLowerCase().contains("writing")) return "bg-primary";
        if (phase.toLowerCase().contains("rebuilding")) return "bg-info";
        if (phase.toLowerCase().contains("cleanup")) return "bg-success";
        return "bg-secondary";
    }
}
