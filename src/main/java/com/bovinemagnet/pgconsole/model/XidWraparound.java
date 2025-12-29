package com.bovinemagnet.pgconsole.model;

/**
 * Represents transaction ID (XID) wraparound risk metrics.
 * <p>
 * PostgreSQL uses 32-bit transaction IDs that wrap around after ~4 billion
 * transactions. Aggressive vacuuming is required to prevent wraparound,
 * which would cause data loss. This model tracks the age of the oldest
 * unfrozen transaction ID per database.
 * </p>
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class XidWraparound {
    private String databaseName;
    private long datfrozenxid;
    private long xidAge;
    private long percentToWraparound;
    private String oldestXidTable;
    private String oldestXidSchema;
    private long oldestRelFrozenXid;
    private long autovacuumFreezeMaxAge;
    private boolean vacuumRunning;

    // Maximum XID age before wraparound (2^31 - 1)
    private static final long MAX_XID_AGE = 2147483647L;
    // Default autovacuum_freeze_max_age
    private static final long DEFAULT_FREEZE_MAX_AGE = 200000000L;

    public XidWraparound() {}

    public String getDatabaseName() { return databaseName; }
    public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }

    public long getDatfrozenxid() { return datfrozenxid; }
    public void setDatfrozenxid(long datfrozenxid) { this.datfrozenxid = datfrozenxid; }

    public long getXidAge() { return xidAge; }
    public void setXidAge(long xidAge) { this.xidAge = xidAge; }

    public long getPercentToWraparound() { return percentToWraparound; }
    public void setPercentToWraparound(long percentToWraparound) { this.percentToWraparound = percentToWraparound; }

    public String getOldestXidTable() { return oldestXidTable; }
    public void setOldestXidTable(String oldestXidTable) { this.oldestXidTable = oldestXidTable; }

    public String getOldestXidSchema() { return oldestXidSchema; }
    public void setOldestXidSchema(String oldestXidSchema) { this.oldestXidSchema = oldestXidSchema; }

    public long getOldestRelFrozenXid() { return oldestRelFrozenXid; }
    public void setOldestRelFrozenXid(long oldestRelFrozenXid) { this.oldestRelFrozenXid = oldestRelFrozenXid; }

    public long getAutovacuumFreezeMaxAge() { return autovacuumFreezeMaxAge; }
    public void setAutovacuumFreezeMaxAge(long autovacuumFreezeMaxAge) { this.autovacuumFreezeMaxAge = autovacuumFreezeMaxAge; }

    public boolean isVacuumRunning() { return vacuumRunning; }
    public void setVacuumRunning(boolean vacuumRunning) { this.vacuumRunning = vacuumRunning; }

    /**
     * Calculates the percentage progress toward XID wraparound.
     *
     * @return percentage (0-100)
     */
    public double calculatePercentToWraparound() {
        if (xidAge == 0) return 0.0;
        return (double) xidAge / MAX_XID_AGE * 100.0;
    }

    /**
     * Returns the number of transactions remaining before wraparound.
     *
     * @return remaining transactions
     */
    public long getTransactionsRemaining() {
        return MAX_XID_AGE - xidAge;
    }

    /**
     * Returns the formatted XID age in millions.
     *
     * @return formatted age like "150M" or "1.2B"
     */
    public String getXidAgeDisplay() {
        if (xidAge < 1000000) return String.format("%,d", xidAge);
        if (xidAge < 1000000000) return String.format("%.1fM", xidAge / 1000000.0);
        return String.format("%.2fB", xidAge / 1000000000.0);
    }

    /**
     * Returns the severity level based on XID age.
     *
     * @param warnPercent warning threshold percentage
     * @param criticalPercent critical threshold percentage
     * @return "critical", "warning", or "ok"
     */
    public String getSeverity(double warnPercent, double criticalPercent) {
        double percent = calculatePercentToWraparound();
        if (percent >= criticalPercent) return "critical";
        if (percent >= warnPercent) return "warning";
        return "ok";
    }

    /**
     * Returns the CSS class for styling based on severity.
     *
     * @param warnPercent warning threshold percentage
     * @param criticalPercent critical threshold percentage
     * @return CSS class name
     */
    public String getSeverityClass(double warnPercent, double criticalPercent) {
        String severity = getSeverity(warnPercent, criticalPercent);
        return switch (severity) {
            case "critical" -> "text-danger";
            case "warning" -> "text-warning";
            default -> "text-success";
        };
    }

    /**
     * Returns the progress bar class based on severity.
     *
     * @param warnPercent warning threshold percentage
     * @param criticalPercent critical threshold percentage
     * @return Bootstrap progress bar class
     */
    public String getProgressBarClass(double warnPercent, double criticalPercent) {
        String severity = getSeverity(warnPercent, criticalPercent);
        return switch (severity) {
            case "critical" -> "bg-danger";
            case "warning" -> "bg-warning";
            default -> "bg-success";
        };
    }

    /**
     * Returns the fully qualified name of the table with oldest XID.
     *
     * @return schema.table or empty string
     */
    public String getOldestXidFullTableName() {
        if (oldestXidSchema == null || oldestXidTable == null) return "";
        return oldestXidSchema + "." + oldestXidTable;
    }

    /**
     * Returns whether emergency vacuum is required.
     *
     * @return true if XID age is critically high
     */
    public boolean isEmergencyVacuumRequired() {
        // Emergency when past autovacuum_freeze_max_age
        long threshold = autovacuumFreezeMaxAge > 0 ? autovacuumFreezeMaxAge : DEFAULT_FREEZE_MAX_AGE;
        return xidAge > threshold;
    }

    /**
     * Returns the recommended action based on current state.
     *
     * @return action recommendation
     */
    public String getRecommendation() {
        double percent = calculatePercentToWraparound();

        if (percent >= 75) {
            return "CRITICAL: Immediate manual VACUUM FREEZE required! " +
                    "Database at risk of shutdown to prevent wraparound.";
        }
        if (percent >= 50) {
            return "WARNING: Schedule aggressive vacuuming. " +
                    "Consider running VACUUM FREEZE on oldest tables.";
        }
        if (isEmergencyVacuumRequired()) {
            return "Autovacuum should trigger aggressive freeze. " +
                    "Monitor vacuum progress.";
        }
        if (vacuumRunning) {
            return "Vacuum in progress. Monitoring recommended.";
        }

        return "XID age is within safe limits.";
    }

    /**
     * Returns the VACUUM FREEZE command for the oldest table.
     *
     * @return SQL command or empty string
     */
    public String getVacuumFreezeCommand() {
        if (oldestXidSchema == null || oldestXidTable == null) {
            return "VACUUM FREEZE;  -- Full database";
        }
        return String.format("VACUUM FREEZE %s.%s;", oldestXidSchema, oldestXidTable);
    }
}
