package com.bovinemagnet.pgconsole.model;

import java.time.Duration;
import java.time.Instant;

/**
 * Represents deadlock statistics for a PostgreSQL database, including current counts,
 * calculated rates, and historical trend data.
 * <p>
 * This class encapsulates deadlock metrics collected from PostgreSQL's {@code pg_stat_database}
 * system view, combined with calculated values such as deadlocks per hour derived from
 * historical sampling data.
 * <p>
 * Deadlocks occur when two or more transactions block each other indefinitely, requiring
 * PostgreSQL to automatically abort one transaction to resolve the cycle. Monitoring deadlock
 * frequency helps identify application-level concurrency issues.
 * <p>
 * The statistics are cumulative since the last statistics reset ({@link #statsReset}).
 * Rate calculations use historical samples from the {@code pgconsole.database_metrics_history}
 * table when available.
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see com.bovinemagnet.pgconsole.service.PostgresService#getDeadlockStats(String)
 */
public class DeadlockStats {

    /**
     * Database name as it appears in the PostgreSQL cluster.
     */
    private String databaseName;

    /**
     * Cumulative count of deadlocks detected since statistics reset.
     * Retrieved from {@code pg_stat_database.deadlocks}.
     */
    private long deadlockCount;

    /**
     * Calculated rate of deadlocks per hour based on historical samples.
     * Returns -1 if historical data is unavailable or insufficient.
     */
    private double deadlocksPerHour;

    /**
     * SVG representation of the deadlock count trend over time.
     * Empty string if history is not available.
     */
    private String sparklineSvg;

    /**
     * Timestamp when statistics were last reset for this database.
     * Null if never reset or not available.
     */
    private Instant statsReset;

    /**
     * Number of hours since statistics were last reset.
     * Used for calculating average rates when historical data is unavailable.
     */
    private double hoursSinceReset;

    /**
     * Default constructor.
     */
    public DeadlockStats() {
    }

    /**
     * Constructs a DeadlockStats with all required values.
     *
     * @param databaseName     the database name
     * @param deadlockCount    cumulative deadlock count
     * @param deadlocksPerHour calculated rate per hour
     * @param sparklineSvg     SVG sparkline or empty string
     * @param statsReset       when stats were last reset
     */
    public DeadlockStats(String databaseName, long deadlockCount, double deadlocksPerHour,
                         String sparklineSvg, Instant statsReset) {
        this.databaseName = databaseName;
        this.deadlockCount = deadlockCount;
        this.deadlocksPerHour = deadlocksPerHour;
        this.sparklineSvg = sparklineSvg;
        this.statsReset = statsReset;
        if (statsReset != null) {
            this.hoursSinceReset = Duration.between(statsReset, Instant.now()).toHours();
        }
    }

    /**
     * Returns the database name.
     *
     * @return the database name
     */
    public String getDatabaseName() {
        return databaseName;
    }

    /**
     * Sets the database name.
     *
     * @param databaseName the database name
     */
    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    /**
     * Returns the cumulative deadlock count since statistics reset.
     *
     * @return the deadlock count
     */
    public long getDeadlockCount() {
        return deadlockCount;
    }

    /**
     * Sets the cumulative deadlock count.
     *
     * @param deadlockCount the deadlock count
     */
    public void setDeadlockCount(long deadlockCount) {
        this.deadlockCount = deadlockCount;
    }

    /**
     * Returns the calculated deadlocks per hour rate.
     *
     * @return deadlocks per hour, or -1 if unavailable
     */
    public double getDeadlocksPerHour() {
        return deadlocksPerHour;
    }

    /**
     * Sets the deadlocks per hour rate.
     *
     * @param deadlocksPerHour the rate per hour
     */
    public void setDeadlocksPerHour(double deadlocksPerHour) {
        this.deadlocksPerHour = deadlocksPerHour;
    }

    /**
     * Returns the SVG sparkline for deadlock trend.
     *
     * @return SVG string or empty string if unavailable
     */
    public String getSparklineSvg() {
        return sparklineSvg;
    }

    /**
     * Sets the SVG sparkline.
     *
     * @param sparklineSvg the SVG string
     */
    public void setSparklineSvg(String sparklineSvg) {
        this.sparklineSvg = sparklineSvg;
    }

    /**
     * Returns when statistics were last reset.
     *
     * @return the reset timestamp or null
     */
    public Instant getStatsReset() {
        return statsReset;
    }

    /**
     * Sets the statistics reset timestamp.
     *
     * @param statsReset the reset timestamp
     */
    public void setStatsReset(Instant statsReset) {
        this.statsReset = statsReset;
        if (statsReset != null) {
            this.hoursSinceReset = Duration.between(statsReset, Instant.now()).toHours();
        }
    }

    /**
     * Returns hours since statistics were reset.
     *
     * @return hours since reset, or 0 if unknown
     */
    public double getHoursSinceReset() {
        return hoursSinceReset;
    }

    /**
     * Returns true if any deadlocks have been detected.
     *
     * @return true if deadlock count is greater than zero
     */
    public boolean hasDeadlocks() {
        return deadlockCount > 0;
    }

    /**
     * Returns true if the deadlock rate indicates a problem.
     * A rate above 1 deadlock per hour is considered concerning.
     *
     * @return true if deadlocks per hour exceeds threshold
     */
    public boolean hasHighRate() {
        return deadlocksPerHour > 1.0;
    }

    /**
     * Returns the appropriate Bootstrap CSS class for severity styling.
     * <ul>
     *   <li>{@code text-danger fw-bold} - High rate (>1/hour) or high count (>10)</li>
     *   <li>{@code text-warning} - Any deadlocks detected</li>
     *   <li>{@code text-muted} - No deadlocks</li>
     * </ul>
     *
     * @return the CSS class string
     */
    public String getSeverityCssClass() {
        if (deadlocksPerHour > 1.0 || deadlockCount > 10) {
            return "text-danger fw-bold";
        } else if (deadlockCount > 0) {
            return "text-warning";
        }
        return "text-muted";
    }

    /**
     * Returns the badge CSS class for display.
     *
     * @return Bootstrap badge class
     */
    public String getBadgeCssClass() {
        if (deadlocksPerHour > 1.0 || deadlockCount > 10) {
            return "bg-danger";
        } else if (deadlockCount > 0) {
            return "bg-warning text-dark";
        }
        return "bg-success";
    }

    /**
     * Returns a formatted string showing time since statistics reset.
     *
     * @return human-readable duration (e.g., "2 days", "5 hours")
     */
    public String getTimeSinceReset() {
        if (statsReset == null) {
            return "Unknown";
        }
        Duration duration = Duration.between(statsReset, Instant.now());
        long days = duration.toDays();
        if (days > 0) {
            return days + " day" + (days != 1 ? "s" : "");
        }
        long hours = duration.toHours();
        if (hours > 0) {
            return hours + " hour" + (hours != 1 ? "s" : "");
        }
        long minutes = duration.toMinutes();
        return minutes + " minute" + (minutes != 1 ? "s" : "");
    }

    /**
     * Returns the deadlocks per hour formatted for display.
     *
     * @return formatted rate string or "N/A" if unavailable
     */
    public String getFormattedRate() {
        if (deadlocksPerHour < 0) {
            return "N/A";
        }
        if (deadlocksPerHour == 0) {
            return "0";
        }
        if (deadlocksPerHour < 0.01) {
            return "< 0.01";
        }
        return String.format("%.2f", deadlocksPerHour);
    }

    /**
     * Returns true if sparkline data is available.
     *
     * @return true if sparklineSvg is not null or empty
     */
    public boolean hasSparkline() {
        return sparklineSvg != null && !sparklineSvg.isEmpty();
    }
}
