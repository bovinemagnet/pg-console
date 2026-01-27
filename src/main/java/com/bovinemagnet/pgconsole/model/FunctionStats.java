package com.bovinemagnet.pgconsole.model;

/**
 * Represents function/procedure performance statistics from the PostgreSQL
 * system view {@code pg_stat_user_functions}.
 * <p>
 * This class captures detailed execution statistics for user-defined functions
 * and procedures, including call counts and timing information.
 * Requires track_functions to be set to 'pl' or 'all' in postgresql.conf.
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see <a href="https://www.postgresql.org/docs/current/monitoring-stats.html#PG-STAT-USER-FUNCTIONS-VIEW">pg_stat_user_functions Documentation</a>
 */
public class FunctionStats {

    /** OID of the function */
    private long funcid;

    /** Schema name containing the function */
    private String schemaName;

    /** Name of the function */
    private String funcName;

    /** Number of times the function has been called */
    private long calls;

    /** Total time spent in the function (milliseconds), including nested calls */
    private double totalTime;

    /** Self time spent in the function (milliseconds), excluding nested calls */
    private double selfTime;

    /** Computed mean execution time per call */
    private double meanTime;

    /**
     * Constructs a new FunctionStats instance.
     */
    public FunctionStats() {
    }

    public long getFuncid() {
        return funcid;
    }

    public void setFuncid(long funcid) {
        this.funcid = funcid;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getFuncName() {
        return funcName;
    }

    public void setFuncName(String funcName) {
        this.funcName = funcName;
    }

    public long getCalls() {
        return calls;
    }

    public void setCalls(long calls) {
        this.calls = calls;
    }

    public double getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(double totalTime) {
        this.totalTime = totalTime;
    }

    public double getSelfTime() {
        return selfTime;
    }

    public void setSelfTime(double selfTime) {
        this.selfTime = selfTime;
    }

    public double getMeanTime() {
        return meanTime;
    }

    public void setMeanTime(double meanTime) {
        this.meanTime = meanTime;
    }

    /**
     * Calculates the mean time per call.
     */
    public void calculateMeanTime() {
        if (calls > 0) {
            this.meanTime = totalTime / calls;
        } else {
            this.meanTime = 0;
        }
    }

    /**
     * Returns the fully qualified function name.
     *
     * @return schema.function format
     */
    public String getFullName() {
        if (schemaName != null && !schemaName.isEmpty()) {
            return schemaName + "." + funcName;
        }
        return funcName;
    }

    /**
     * Returns the call count formatted with thousands separators.
     *
     * @return formatted call count
     */
    public String getCallsFormatted() {
        return String.format("%,d", calls);
    }

    /**
     * Returns the total time formatted as a human-readable string.
     *
     * @return formatted total time
     */
    public String getTotalTimeFormatted() {
        return formatTime(totalTime);
    }

    /**
     * Returns the self time formatted as a human-readable string.
     *
     * @return formatted self time
     */
    public String getSelfTimeFormatted() {
        return formatTime(selfTime);
    }

    /**
     * Returns the mean time formatted as a human-readable string.
     *
     * @return formatted mean time
     */
    public String getMeanTimeFormatted() {
        return formatTime(meanTime);
    }

    /**
     * Returns the percentage of self time vs total time.
     *
     * @return self time percentage
     */
    public double getSelfTimePercent() {
        if (totalTime == 0) return 0;
        return (selfTime / totalTime) * 100;
    }

    /**
     * Returns the self time percentage formatted.
     *
     * @return formatted self time percentage
     */
    public String getSelfTimePercentFormatted() {
        return String.format("%.1f%%", getSelfTimePercent());
    }

    /**
     * Formats time in milliseconds as a human-readable string.
     *
     * @param timeMs time in milliseconds
     * @return formatted time string
     */
    private String formatTime(double timeMs) {
        if (timeMs < 0) return "N/A";
        if (timeMs == 0) return "0 ms";
        if (timeMs < 1) return String.format("%.3f ms", timeMs);
        if (timeMs < 1000) return String.format("%.2f ms", timeMs);
        if (timeMs < 60000) return String.format("%.2f s", timeMs / 1000.0);
        if (timeMs < 3600000) return String.format("%.1f min", timeMs / 60000.0);
        return String.format("%.1f hours", timeMs / 3600000.0);
    }

    /**
     * Returns whether this function is a performance concern based on total time.
     *
     * @return true if total time exceeds 1 second
     */
    public boolean isHighTotalTime() {
        return totalTime > 1000;
    }

    /**
     * Returns whether this function has a high mean execution time.
     *
     * @return true if mean time exceeds 10 milliseconds
     */
    public boolean isHighMeanTime() {
        return meanTime > 10;
    }

    /**
     * Returns Bootstrap CSS class based on performance indicators.
     *
     * @return Bootstrap table row class
     */
    public String getRowCssClass() {
        if (isHighTotalTime() && isHighMeanTime()) return "table-danger";
        if (isHighTotalTime() || isHighMeanTime()) return "table-warning";
        return "";
    }
}
