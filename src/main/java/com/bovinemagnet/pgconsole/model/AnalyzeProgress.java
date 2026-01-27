package com.bovinemagnet.pgconsole.model;

/**
 * Represents ANALYZE progress from the PostgreSQL system view
 * {@code pg_stat_progress_analyze}.
 * <p>
 * This class captures detailed information about ongoing ANALYZE operations,
 * including the current phase and progress metrics.
 * Available in PostgreSQL 13+.
 * <p>
 * The operation proceeds through several phases:
 * <ul>
 *   <li>Initializing</li>
 *   <li>Acquiring sample rows</li>
 *   <li>Acquiring inherited sample rows</li>
 *   <li>Computing statistics</li>
 *   <li>Computing extended statistics</li>
 *   <li>Finalizing</li>
 * </ul>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see <a href="https://www.postgresql.org/docs/current/progress-reporting.html#ANALYZE-PROGRESS-REPORTING">ANALYZE Progress Reporting</a>
 */
public class AnalyzeProgress {

    /** Process ID of the backend performing the operation */
    private int pid;

    /** Name of the database */
    private String database;

    /** Schema name of the table */
    private String schemaName;

    /** Name of the table being analysed */
    private String tableName;

    /** Current phase of the operation */
    private String phase;

    /** Total number of sample blocks to read */
    private long sampleBlksTotal;

    /** Number of sample blocks scanned */
    private long sampleBlksScanned;

    /** Total number of extended statistics to compute */
    private long extStatsTotal;

    /** Number of extended statistics computed */
    private long extStatsComputed;

    /** Total number of child tables */
    private long childTablesTotal;

    /** Number of child tables scanned */
    private long childTablesDone;

    /** Current child table OID being processed */
    private long currentChildTableRelid;

    /** Computed progress percentage */
    private double progressPercent;

    /**
     * Constructs a new AnalyzeProgress instance.
     */
    public AnalyzeProgress() {
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

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public long getSampleBlksTotal() {
        return sampleBlksTotal;
    }

    public void setSampleBlksTotal(long sampleBlksTotal) {
        this.sampleBlksTotal = sampleBlksTotal;
    }

    public long getSampleBlksScanned() {
        return sampleBlksScanned;
    }

    public void setSampleBlksScanned(long sampleBlksScanned) {
        this.sampleBlksScanned = sampleBlksScanned;
    }

    public long getExtStatsTotal() {
        return extStatsTotal;
    }

    public void setExtStatsTotal(long extStatsTotal) {
        this.extStatsTotal = extStatsTotal;
    }

    public long getExtStatsComputed() {
        return extStatsComputed;
    }

    public void setExtStatsComputed(long extStatsComputed) {
        this.extStatsComputed = extStatsComputed;
    }

    public long getChildTablesTotal() {
        return childTablesTotal;
    }

    public void setChildTablesTotal(long childTablesTotal) {
        this.childTablesTotal = childTablesTotal;
    }

    public long getChildTablesDone() {
        return childTablesDone;
    }

    public void setChildTablesDone(long childTablesDone) {
        this.childTablesDone = childTablesDone;
    }

    public long getCurrentChildTableRelid() {
        return currentChildTableRelid;
    }

    public void setCurrentChildTableRelid(long currentChildTableRelid) {
        this.currentChildTableRelid = currentChildTableRelid;
    }

    public double getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(double progressPercent) {
        this.progressPercent = progressPercent;
    }

    /**
     * Calculates progress based on sample blocks or extended stats.
     */
    public void calculateProgress() {
        if (sampleBlksTotal > 0) {
            this.progressPercent = (sampleBlksScanned * 100.0) / sampleBlksTotal;
        } else if (extStatsTotal > 0) {
            this.progressPercent = (extStatsComputed * 100.0) / extStatsTotal;
        } else if (childTablesTotal > 0) {
            this.progressPercent = (childTablesDone * 100.0) / childTablesTotal;
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
        if (phase.toLowerCase().contains("acquiring")) return "bg-info";
        if (phase.toLowerCase().contains("computing")) return "bg-primary";
        if (phase.toLowerCase().contains("finalizing")) return "bg-success";
        return "bg-secondary";
    }
}
