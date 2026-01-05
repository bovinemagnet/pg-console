package com.bovinemagnet.pgconsole.model;

/**
 * Represents transaction ID (XID) wraparound risk metrics for PostgreSQL databases.
 * <p>
 * PostgreSQL uses 32-bit transaction IDs that wrap around after approximately 4 billion
 * transactions. Aggressive vacuuming is required to prevent wraparound, which would cause
 * data loss and potential database shutdown. This model tracks the age of the oldest
 * unfrozen transaction ID per database and provides helper methods to assess risk levels
 * and generate recommendations.
 * </p>
 * <p>
 * XID wraparound occurs when the transaction ID counter reaches its maximum value and
 * cycles back to 0. To prevent this, PostgreSQL freezes old transaction IDs through the
 * VACUUM FREEZE process. This class monitors the progress toward wraparound and identifies
 * tables that require attention.
 * </p>
 * <p>
 * Example usage in templates:
 * <pre>{@code
 * {#for item in wraparoundData}
 *   <div class="{item.getSeverityClass(50, 75)}">
 *     {item.databaseName}: {item.xidAgeDisplay} ({item.calculatePercentToWraparound()}%)
 *     {item.recommendation}
 *   </div>
 * {/for}
 * }</pre>
 * </p>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see <a href="https://www.postgresql.org/docs/current/routine-vacuuming.html#VACUUM-FOR-WRAPAROUND">PostgreSQL VACUUM Documentation</a>
 */
public class XidWraparound {
    /**
     * The name of the database being monitored.
     */
    private String databaseName;

    /**
     * The database's frozen transaction ID from pg_database.datfrozenxid.
     * This represents the oldest unfrozen XID that might appear in this database.
     */
    private long datfrozenxid;

    /**
     * The current age of the oldest transaction ID in transactions.
     * Calculated as (current XID - datfrozenxid).
     */
    private long xidAge;

    /**
     * The percentage progress toward XID wraparound (0-100).
     * This field may be set explicitly or calculated via {@link #calculatePercentToWraparound()}.
     */
    private long percentToWraparound;

    /**
     * The name of the table with the oldest unfrozen XID.
     * Null if not yet determined or if no specific table is identified.
     */
    private String oldestXidTable;

    /**
     * The schema containing the table with the oldest unfrozen XID.
     * Null if not yet determined or if no specific schema is identified.
     */
    private String oldestXidSchema;

    /**
     * The frozen XID of the oldest relation (table/index).
     * Retrieved from pg_class.relfrozenxid for the oldest table.
     */
    private long oldestRelFrozenXid;

    /**
     * The autovacuum_freeze_max_age setting for this database.
     * When XID age exceeds this value, PostgreSQL triggers aggressive autovacuum.
     */
    private long autovacuumFreezeMaxAge;

    /**
     * Indicates whether a VACUUM operation is currently running on this database.
     */
    private boolean vacuumRunning;

    /**
     * Maximum XID age before wraparound occurs (2^31 - 1).
     * PostgreSQL uses signed 32-bit integers, limiting the maximum age.
     */
    private static final long MAX_XID_AGE = 2147483647L;

    /**
     * Default value for autovacuum_freeze_max_age (200 million transactions).
     * Used as fallback when the actual setting cannot be determined.
     */
    private static final long DEFAULT_FREEZE_MAX_AGE = 200000000L;

    /**
     * Constructs a new XidWraparound instance with default values.
     */
    public XidWraparound() {}

    /**
     * Returns the name of the database being monitored.
     *
     * @return the database name, or null if not set
     */
    public String getDatabaseName() { return databaseName; }

    /**
     * Sets the name of the database being monitored.
     *
     * @param databaseName the database name
     */
    public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }

    /**
     * Returns the database's frozen transaction ID from pg_database.datfrozenxid.
     *
     * @return the frozen XID value
     */
    public long getDatfrozenxid() { return datfrozenxid; }

    /**
     * Sets the database's frozen transaction ID.
     *
     * @param datfrozenxid the frozen XID value from pg_database.datfrozenxid
     */
    public void setDatfrozenxid(long datfrozenxid) { this.datfrozenxid = datfrozenxid; }

    /**
     * Returns the current age of the oldest transaction ID in transactions.
     *
     * @return the XID age, calculated as (current XID - datfrozenxid)
     */
    public long getXidAge() { return xidAge; }

    /**
     * Sets the current age of the oldest transaction ID.
     *
     * @param xidAge the XID age in number of transactions
     */
    public void setXidAge(long xidAge) { this.xidAge = xidAge; }

    /**
     * Returns the percentage progress toward XID wraparound.
     * <p>
     * Note: This returns the stored field value. For calculated percentage,
     * use {@link #calculatePercentToWraparound()}.
     * </p>
     *
     * @return the percentage as a long value (0-100)
     * @see #calculatePercentToWraparound()
     */
    public long getPercentToWraparound() { return percentToWraparound; }

    /**
     * Sets the percentage progress toward XID wraparound.
     *
     * @param percentToWraparound the percentage value (0-100)
     */
    public void setPercentToWraparound(long percentToWraparound) { this.percentToWraparound = percentToWraparound; }

    /**
     * Returns the name of the table with the oldest unfrozen XID.
     *
     * @return the table name, or null if not determined
     * @see #getOldestXidFullTableName()
     */
    public String getOldestXidTable() { return oldestXidTable; }

    /**
     * Sets the name of the table with the oldest unfrozen XID.
     *
     * @param oldestXidTable the table name
     */
    public void setOldestXidTable(String oldestXidTable) { this.oldestXidTable = oldestXidTable; }

    /**
     * Returns the schema containing the table with the oldest unfrozen XID.
     *
     * @return the schema name, or null if not determined
     * @see #getOldestXidFullTableName()
     */
    public String getOldestXidSchema() { return oldestXidSchema; }

    /**
     * Sets the schema containing the table with the oldest unfrozen XID.
     *
     * @param oldestXidSchema the schema name
     */
    public void setOldestXidSchema(String oldestXidSchema) { this.oldestXidSchema = oldestXidSchema; }

    /**
     * Returns the frozen XID of the oldest relation (table or index).
     *
     * @return the relfrozenxid value from pg_class
     */
    public long getOldestRelFrozenXid() { return oldestRelFrozenXid; }

    /**
     * Sets the frozen XID of the oldest relation.
     *
     * @param oldestRelFrozenXid the relfrozenxid value
     */
    public void setOldestRelFrozenXid(long oldestRelFrozenXid) { this.oldestRelFrozenXid = oldestRelFrozenXid; }

    /**
     * Returns the autovacuum_freeze_max_age setting for this database.
     *
     * @return the freeze max age threshold in transactions
     */
    public long getAutovacuumFreezeMaxAge() { return autovacuumFreezeMaxAge; }

    /**
     * Sets the autovacuum_freeze_max_age setting.
     *
     * @param autovacuumFreezeMaxAge the freeze max age threshold
     */
    public void setAutovacuumFreezeMaxAge(long autovacuumFreezeMaxAge) { this.autovacuumFreezeMaxAge = autovacuumFreezeMaxAge; }

    /**
     * Returns whether a VACUUM operation is currently running on this database.
     *
     * @return true if vacuum is in progress, false otherwise
     */
    public boolean isVacuumRunning() { return vacuumRunning; }

    /**
     * Sets whether a VACUUM operation is currently running.
     *
     * @param vacuumRunning true if vacuum is in progress
     */
    public void setVacuumRunning(boolean vacuumRunning) { this.vacuumRunning = vacuumRunning; }

    /**
     * Calculates the percentage progress toward XID wraparound.
     * <p>
     * This method computes the percentage based on the current {@link #xidAge}
     * relative to {@link #MAX_XID_AGE}. The result is a precise double value
     * that can be used for detailed monitoring and alerting.
     * </p>
     *
     * @return percentage from 0.0 to 100.0, or 0.0 if xidAge is zero
     * @see #getPercentToWraparound()
     */
    public double calculatePercentToWraparound() {
        if (xidAge == 0) return 0.0;
        return (double) xidAge / MAX_XID_AGE * 100.0;
    }

    /**
     * Returns the number of transactions remaining before wraparound occurs.
     * <p>
     * This value indicates how many more transactions can occur before
     * reaching the critical wraparound threshold. A low number requires
     * immediate attention and manual VACUUM FREEZE operations.
     * </p>
     *
     * @return the remaining transaction count, never negative
     * @see #calculatePercentToWraparound()
     */
    public long getTransactionsRemaining() {
        return MAX_XID_AGE - xidAge;
    }

    /**
     * Returns the XID age formatted for human-readable display.
     * <p>
     * Formats the transaction age using appropriate units:
     * </p>
     * <ul>
     *   <li>Less than 1 million: displays with comma separators (e.g., "500,000")</li>
     *   <li>1 million to 999 million: displays in millions (e.g., "150.5M")</li>
     *   <li>1 billion or more: displays in billions (e.g., "1.25B")</li>
     * </ul>
     *
     * @return formatted age string suitable for UI display
     */
    public String getXidAgeDisplay() {
        if (xidAge < 1000000) return String.format("%,d", xidAge);
        if (xidAge < 1000000000) return String.format("%.1fM", xidAge / 1000000.0);
        return String.format("%.2fB", xidAge / 1000000000.0);
    }

    /**
     * Returns the severity level based on XID age percentage.
     * <p>
     * Evaluates the current wraparound risk using the provided thresholds.
     * This method is useful for implementing custom alerting logic with
     * configurable thresholds.
     * </p>
     *
     * @param warnPercent warning threshold percentage (typically 50-60)
     * @param criticalPercent critical threshold percentage (typically 75-80)
     * @return "critical" if at or above critical threshold,
     *         "warning" if at or above warning threshold but below critical,
     *         "ok" otherwise
     * @see #getSeverityClass(double, double)
     * @see #getProgressBarClass(double, double)
     */
    public String getSeverity(double warnPercent, double criticalPercent) {
        double percent = calculatePercentToWraparound();
        if (percent >= criticalPercent) return "critical";
        if (percent >= warnPercent) return "warning";
        return "ok";
    }

    /**
     * Returns the Bootstrap CSS text class for styling based on severity.
     * <p>
     * Maps severity levels to Bootstrap 5 text colour classes suitable
     * for displaying warnings and alerts in Qute templates.
     * </p>
     *
     * @param warnPercent warning threshold percentage (typically 50-60)
     * @param criticalPercent critical threshold percentage (typically 75-80)
     * @return "text-danger" for critical, "text-warning" for warning,
     *         "text-success" for ok
     * @see #getSeverity(double, double)
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
     * Returns the Bootstrap CSS progress bar class based on severity.
     * <p>
     * Maps severity levels to Bootstrap 5 background colour classes suitable
     * for progress bars showing wraparound risk progression.
     * </p>
     *
     * @param warnPercent warning threshold percentage (typically 50-60)
     * @param criticalPercent critical threshold percentage (typically 75-80)
     * @return "bg-danger" for critical, "bg-warning" for warning,
     *         "bg-success" for ok
     * @see #getSeverity(double, double)
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
     * Returns the fully qualified name of the table with the oldest unfrozen XID.
     * <p>
     * Combines {@link #oldestXidSchema} and {@link #oldestXidTable} into a
     * schema-qualified table name suitable for use in SQL statements.
     * </p>
     *
     * @return schema.table format, or empty string if schema or table is null
     * @see #getVacuumFreezeCommand()
     */
    public String getOldestXidFullTableName() {
        if (oldestXidSchema == null || oldestXidTable == null) return "";
        return oldestXidSchema + "." + oldestXidTable;
    }

    /**
     * Returns whether an emergency vacuum is required based on autovacuum settings.
     * <p>
     * PostgreSQL triggers aggressive autovacuum when XID age exceeds
     * autovacuum_freeze_max_age. This method checks if that threshold has been
     * reached, indicating that emergency intervention may be needed if autovacuum
     * cannot keep up with transaction load.
     * </p>
     *
     * @return true if XID age exceeds autovacuum_freeze_max_age threshold
     * @see #getRecommendation()
     */
    public boolean isEmergencyVacuumRequired() {
        // Emergency when past autovacuum_freeze_max_age
        long threshold = autovacuumFreezeMaxAge > 0 ? autovacuumFreezeMaxAge : DEFAULT_FREEZE_MAX_AGE;
        return xidAge > threshold;
    }

    /**
     * Returns the recommended action based on current XID wraparound state.
     * <p>
     * Analyses the XID age percentage, autovacuum thresholds, and vacuum status
     * to provide actionable recommendations. The recommendations are prioritised
     * from critical to informational.
     * </p>
     * <p>
     * Recommendation levels:
     * </p>
     * <ul>
     *   <li>75%+ to wraparound: Critical alert requiring immediate manual intervention</li>
     *   <li>50-75% to wraparound: Warning to schedule aggressive vacuuming</li>
     *   <li>Past autovacuum_freeze_max_age: Information that autovacuum should engage</li>
     *   <li>Vacuum running: Status update indicating monitoring needed</li>
     *   <li>Below thresholds: Confirmation that XID age is safe</li>
     * </ul>
     *
     * @return human-readable recommendation text suitable for display in dashboards
     * @see #isEmergencyVacuumRequired()
     * @see #getVacuumFreezeCommand()
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
     * Returns the SQL VACUUM FREEZE command for the oldest table or full database.
     * <p>
     * Generates an executable SQL command to freeze transaction IDs. If the oldest
     * table is known, targets that specific table for efficiency. Otherwise, returns
     * a full database VACUUM FREEZE command.
     * </p>
     * <p>
     * The generated command can be executed directly in psql or via JDBC to
     * immediately reduce XID age. Note that VACUUM FREEZE operations can be
     * resource-intensive and may take significant time on large tables.
     * </p>
     *
     * @return SQL command string, either "VACUUM FREEZE schema.table;" for a specific
     *         table or "VACUUM FREEZE;  -- Full database" if table is unknown
     * @see #getOldestXidFullTableName()
     */
    public String getVacuumFreezeCommand() {
        if (oldestXidSchema == null || oldestXidTable == null) {
            return "VACUUM FREEZE;  -- Full database";
        }
        return String.format("VACUUM FREEZE %s.%s;", oldestXidSchema, oldestXidTable);
    }
}
