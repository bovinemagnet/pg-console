package com.bovinemagnet.pgconsole.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents PostgreSQL configuration settings related to deadlock detection and logging,
 * along with recommended values and optimisation status.
 * <p>
 * This class encapsulates the key configuration parameters that affect deadlock behaviour:
 * <ul>
 *   <li>{@code deadlock_timeout} - How long to wait before checking for deadlocks</li>
 *   <li>{@code log_lock_waits} - Whether to log when queries wait for locks</li>
 * </ul>
 * <p>
 * The class provides comparison between current settings and recommended values,
 * along with explanations and actionable recommendations for improvement.
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see com.bovinemagnet.pgconsole.service.PostgresService#getDeadlockConfig(String)
 */
public class DeadlockConfig {

    /**
     * Current value of deadlock_timeout setting.
     * This is the time to wait on a lock before checking for deadlocks.
     */
    private String deadlockTimeout;

    /**
     * Whether log_lock_waits is enabled.
     * When enabled, logs a message when a query waits longer than deadlock_timeout.
     */
    private boolean logLockWaits;

    /**
     * Current value of lock_timeout setting.
     * Maximum time to wait for a lock before aborting the statement (0 = disabled).
     */
    private String lockTimeout;

    /**
     * Recommended value for deadlock_timeout.
     */
    private static final String RECOMMENDED_DEADLOCK_TIMEOUT = "1s";

    /**
     * Recommended value for log_lock_waits.
     */
    private static final boolean RECOMMENDED_LOG_LOCK_WAITS = true;

    /**
     * Default constructor.
     */
    public DeadlockConfig() {
    }

    /**
     * Constructs a DeadlockConfig with the specified values.
     *
     * @param deadlockTimeout current deadlock_timeout setting
     * @param logLockWaits    current log_lock_waits setting
     * @param lockTimeout     current lock_timeout setting
     */
    public DeadlockConfig(String deadlockTimeout, boolean logLockWaits, String lockTimeout) {
        this.deadlockTimeout = deadlockTimeout;
        this.logLockWaits = logLockWaits;
        this.lockTimeout = lockTimeout;
    }

    /**
     * Returns the current deadlock_timeout setting.
     *
     * @return the deadlock timeout value
     */
    public String getDeadlockTimeout() {
        return deadlockTimeout;
    }

    /**
     * Sets the deadlock_timeout value.
     *
     * @param deadlockTimeout the timeout value
     */
    public void setDeadlockTimeout(String deadlockTimeout) {
        this.deadlockTimeout = deadlockTimeout;
    }

    /**
     * Returns whether log_lock_waits is enabled.
     *
     * @return true if lock waits are logged
     */
    public boolean isLogLockWaits() {
        return logLockWaits;
    }

    /**
     * Sets whether log_lock_waits is enabled.
     *
     * @param logLockWaits true to enable lock wait logging
     */
    public void setLogLockWaits(boolean logLockWaits) {
        this.logLockWaits = logLockWaits;
    }

    /**
     * Returns the current lock_timeout setting.
     *
     * @return the lock timeout value
     */
    public String getLockTimeout() {
        return lockTimeout;
    }

    /**
     * Sets the lock_timeout value.
     *
     * @param lockTimeout the timeout value
     */
    public void setLockTimeout(String lockTimeout) {
        this.lockTimeout = lockTimeout;
    }

    /**
     * Returns the recommended deadlock_timeout value.
     *
     * @return the recommended timeout
     */
    public String getRecommendedDeadlockTimeout() {
        return RECOMMENDED_DEADLOCK_TIMEOUT;
    }

    /**
     * Returns the recommended log_lock_waits value.
     *
     * @return true (logging should be enabled)
     */
    public boolean getRecommendedLogLockWaits() {
        return RECOMMENDED_LOG_LOCK_WAITS;
    }

    /**
     * Returns true if deadlock_timeout is set to the recommended value.
     *
     * @return true if optimal
     */
    public boolean isDeadlockTimeoutOptimal() {
        return RECOMMENDED_DEADLOCK_TIMEOUT.equals(deadlockTimeout);
    }

    /**
     * Returns true if log_lock_waits is set to the recommended value.
     *
     * @return true if optimal
     */
    public boolean isLogLockWaitsOptimal() {
        return logLockWaits == RECOMMENDED_LOG_LOCK_WAITS;
    }

    /**
     * Returns true if all deadlock-related settings are optimal.
     *
     * @return true if all settings match recommendations
     */
    public boolean isOptimal() {
        return isDeadlockTimeoutOptimal() && isLogLockWaitsOptimal();
    }

    /**
     * Returns the number of settings that need adjustment.
     *
     * @return count of non-optimal settings
     */
    public int getIssueCount() {
        int count = 0;
        if (!isDeadlockTimeoutOptimal()) count++;
        if (!isLogLockWaitsOptimal()) count++;
        return count;
    }

    /**
     * Returns a list of actionable recommendations for improving deadlock configuration.
     *
     * @return list of recommendation strings
     */
    public List<String> getRecommendations() {
        List<String> recommendations = new ArrayList<>();

        if (!isLogLockWaitsOptimal()) {
            recommendations.add("Enable log_lock_waits to capture lock wait events in PostgreSQL logs. " +
                    "This helps identify queries that frequently wait for locks and may be deadlock candidates.");
        }

        if (!isDeadlockTimeoutOptimal()) {
            recommendations.add("Consider setting deadlock_timeout to 1s. The current value of '" +
                    deadlockTimeout + "' may affect how quickly deadlocks are detected.");
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Deadlock configuration is optimal. Monitor the deadlock count " +
                    "and investigate any occurrences using the Locks & Blocking dashboard.");
        }

        return recommendations;
    }

    /**
     * Returns the CSS class for the deadlock_timeout status badge.
     *
     * @return Bootstrap badge class
     */
    public String getDeadlockTimeoutBadgeClass() {
        return isDeadlockTimeoutOptimal() ? "bg-success" : "bg-warning text-dark";
    }

    /**
     * Returns the CSS class for the log_lock_waits status badge.
     *
     * @return Bootstrap badge class
     */
    public String getLogLockWaitsBadgeClass() {
        return isLogLockWaitsOptimal() ? "bg-success" : "bg-warning text-dark";
    }

    /**
     * Returns the overall configuration status badge class.
     *
     * @return Bootstrap badge class
     */
    public String getOverallBadgeClass() {
        return isOptimal() ? "bg-success" : "bg-warning text-dark";
    }

    /**
     * Returns a human-readable status summary.
     *
     * @return status string (e.g., "Optimal", "2 issues")
     */
    public String getStatusSummary() {
        if (isOptimal()) {
            return "Optimal";
        }
        int issues = getIssueCount();
        return issues + " issue" + (issues != 1 ? "s" : "");
    }

    /**
     * Returns an explanation of the deadlock_timeout setting.
     *
     * @return explanation text
     */
    public String getDeadlockTimeoutExplanation() {
        return "Time to wait on a lock before checking for deadlocks. " +
                "Lower values detect deadlocks faster but increase overhead.";
    }

    /**
     * Returns an explanation of the log_lock_waits setting.
     *
     * @return explanation text
     */
    public String getLogLockWaitsExplanation() {
        return "When enabled, logs a message whenever a query waits longer than deadlock_timeout " +
                "for a lock. Essential for identifying lock contention patterns.";
    }

    /**
     * Returns an explanation of the lock_timeout setting.
     *
     * @return explanation text
     */
    public String getLockTimeoutExplanation() {
        return "Maximum time a statement will wait for a lock before being aborted. " +
                "A value of 0 means wait forever (default). Setting a limit can prevent long waits.";
    }
}
