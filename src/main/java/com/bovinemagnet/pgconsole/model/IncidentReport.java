package com.bovinemagnet.pgconsole.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a point-in-time snapshot of database state for incident analysis.
 * <p>
 * This class captures a comprehensive view of PostgreSQL database health and performance
 * at a specific moment in time. It is designed to support post-incident analysis,
 * performance troubleshooting, and forensic investigation of database issues.
 * <p>
 * An incident report includes:
 * <ul>
 *   <li>Overview statistics (connections, cache hit ratio, database size)</li>
 *   <li>Current database activity and active queries</li>
 *   <li>Wait event summaries showing resource contention</li>
 *   <li>Lock information including blocking relationships</li>
 *   <li>Top slow queries from pg_stat_statements</li>
 *   <li>Database metadata and version information</li>
 * </ul>
 * <p>
 * Reports are identified by a unique report ID generated from the capture timestamp.
 * Activity counts (active, blocked, idle) are automatically calculated when activities
 * are set via {@link #setActivities(List)}.
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see Activity
 * @see OverviewStats
 * @see BlockingTree
 * @see SlowQuery
 * @since 0.0.0
 */
public class IncidentReport {

    /**
     * Date-time formatter for generating filename-safe timestamps.
     * Format: yyyyMMdd-HHmmss (e.g., "20260105-143022")
     */
    private static final DateTimeFormatter FILENAME_FORMAT =
        DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    /**
     * Date-time formatter for human-readable display timestamps.
     * Format: yyyy-MM-dd HH:mm:ss (e.g., "2026-01-05 14:30:22")
     */
    private static final DateTimeFormatter DISPLAY_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Unique identifier for this incident report.
     * Generated automatically in the format "IR-{timestamp}" where timestamp
     * follows the {@link #FILENAME_FORMAT} pattern.
     */
    private String reportId;

    /**
     * Name of the PostgreSQL instance being monitored.
     * This typically corresponds to the database server hostname or logical instance name.
     */
    private String instanceName;

    /**
     * Timestamp when this report snapshot was captured.
     * Set automatically to the current time when the report is created.
     */
    private LocalDateTime capturedAt;

    /**
     * Optional human-readable description of the incident or reason for capture.
     * May be null if no description is provided.
     */
    private String description;

    /**
     * High-level overview statistics for the database instance.
     * Includes connection counts, cache hit ratios, database size, and other summary metrics.
     *
     * @see OverviewStats
     */
    private OverviewStats overviewStats;

    /**
     * List of current database activities (connections and queries).
     * Each activity represents a connection from pg_stat_activity.
     * Setting this list automatically recalculates activity counts.
     *
     * @see Activity
     * @see #recalculateActivityCounts()
     */
    private List<Activity> activities = new ArrayList<>();

    /**
     * Count of active queries at the time of capture.
     * Automatically calculated from {@link #activities} based on query state.
     */
    private int activeQueryCount;

    /**
     * Count of blocked queries at the time of capture.
     * Automatically calculated from {@link #activities} based on blocking relationships.
     */
    private int blockedQueryCount;

    /**
     * Count of idle connections at the time of capture.
     * Automatically calculated from {@link #activities} for connections not active or blocked.
     */
    private int idleConnectionCount;

    /**
     * Summary of wait events grouped by wait event type.
     * Shows what resources sessions are waiting for (locks, I/O, CPU, etc.).
     *
     * @see WaitEventSummary
     */
    private List<WaitEventSummary> waitEvents = new ArrayList<>();

    /**
     * Hierarchical representation of blocking relationships between queries.
     * Shows which queries are blocking other queries and the chain of dependencies.
     *
     * @see BlockingTree
     */
    private List<BlockingTree> blockingTree = new ArrayList<>();

    /**
     * Detailed lock information from pg_locks.
     * Includes all lock types, modes, and granted status at the time of capture.
     *
     * @see LockInfo
     */
    private List<LockInfo> locks = new ArrayList<>();

    /**
     * List of the slowest queries captured from pg_stat_statements.
     * Typically ordered by total execution time or mean execution time.
     *
     * @see SlowQuery
     */
    private List<SlowQuery> topSlowQueries = new ArrayList<>();

    /**
     * Metadata about the PostgreSQL database instance.
     * Includes version, uptime, configuration parameters, and server information.
     *
     * @see DatabaseInfo
     */
    private DatabaseInfo databaseInfo;

    /**
     * Constructs a new incident report with the current timestamp.
     * <p>
     * The report ID is automatically generated in the format "IR-{timestamp}"
     * where the timestamp follows the pattern yyyyMMdd-HHmmss.
     * The {@link #capturedAt} field is set to the current system time.
     */
    public IncidentReport() {
        this.capturedAt = LocalDateTime.now();
        this.reportId = "IR-" + FILENAME_FORMAT.format(capturedAt);
    }

    /**
     * Constructs a new incident report for a specific PostgreSQL instance.
     * <p>
     * This constructor calls the default constructor to initialise the timestamp
     * and report ID, then sets the instance name.
     *
     * @param instanceName the name of the PostgreSQL instance being monitored
     */
    public IncidentReport(String instanceName) {
        this();
        this.instanceName = instanceName;
    }

    // Getters and setters

    /**
     * Returns the unique identifier for this incident report.
     * <p>
     * The report ID is generated automatically in the format "IR-{timestamp}".
     *
     * @return the report ID, never null
     */
    public String getReportId() {
        return reportId;
    }

    /**
     * Sets the unique identifier for this incident report.
     * <p>
     * Note: This is typically set automatically by the constructor and should
     * only be changed if loading a report from persistent storage.
     *
     * @param reportId the report ID to set
     */
    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    /**
     * Returns the name of the PostgreSQL instance being monitored.
     *
     * @return the instance name, may be null if not set
     */
    public String getInstanceName() {
        return instanceName;
    }

    /**
     * Sets the name of the PostgreSQL instance being monitored.
     *
     * @param instanceName the instance name to set
     */
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    /**
     * Returns the timestamp when this report snapshot was captured.
     *
     * @return the capture timestamp, never null for reports created via constructor
     */
    public LocalDateTime getCapturedAt() {
        return capturedAt;
    }

    /**
     * Sets the timestamp when this report snapshot was captured.
     *
     * @param capturedAt the capture timestamp to set
     */
    public void setCapturedAt(LocalDateTime capturedAt) {
        this.capturedAt = capturedAt;
    }

    /**
     * Returns the human-readable description of the incident.
     *
     * @return the incident description, may be null if not provided
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets a human-readable description of the incident or reason for capture.
     *
     * @param description the incident description to set, may be null
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the high-level overview statistics for the database instance.
     *
     * @return the overview statistics, may be null if not yet captured
     * @see OverviewStats
     */
    public OverviewStats getOverviewStats() {
        return overviewStats;
    }

    /**
     * Sets the high-level overview statistics for the database instance.
     *
     * @param overviewStats the overview statistics to set
     * @see OverviewStats
     */
    public void setOverviewStats(OverviewStats overviewStats) {
        this.overviewStats = overviewStats;
    }

    /**
     * Returns the list of current database activities.
     * <p>
     * The returned list is mutable and may be modified by the caller.
     *
     * @return the list of activities, never null but may be empty
     * @see Activity
     */
    public List<Activity> getActivities() {
        return activities;
    }

    /**
     * Sets the list of current database activities.
     * <p>
     * This method automatically recalculates activity counts (active, blocked, idle)
     * based on the provided activities.
     *
     * @param activities the list of activities to set, must not be null
     * @see #recalculateActivityCounts()
     */
    public void setActivities(List<Activity> activities) {
        this.activities = activities;
        recalculateActivityCounts();
    }

    /**
     * Returns the count of active queries at the time of capture.
     * <p>
     * This value is automatically calculated by {@link #setActivities(List)}
     * based on query state from pg_stat_activity.
     *
     * @return the number of active queries
     */
    public int getActiveQueryCount() {
        return activeQueryCount;
    }

    /**
     * Returns the count of blocked queries at the time of capture.
     * <p>
     * This value is automatically calculated by {@link #setActivities(List)}
     * based on blocking relationships in the activities list.
     *
     * @return the number of blocked queries
     */
    public int getBlockedQueryCount() {
        return blockedQueryCount;
    }

    /**
     * Returns the count of idle connections at the time of capture.
     * <p>
     * This value is automatically calculated by {@link #setActivities(List)}
     * for connections that are neither active nor blocked.
     *
     * @return the number of idle connections
     */
    public int getIdleConnectionCount() {
        return idleConnectionCount;
    }

    /**
     * Returns the summary of wait events grouped by wait event type.
     * <p>
     * The returned list is mutable and may be modified by the caller.
     *
     * @return the list of wait event summaries, never null but may be empty
     * @see WaitEventSummary
     */
    public List<WaitEventSummary> getWaitEvents() {
        return waitEvents;
    }

    /**
     * Sets the summary of wait events grouped by wait event type.
     *
     * @param waitEvents the list of wait event summaries to set
     * @see WaitEventSummary
     */
    public void setWaitEvents(List<WaitEventSummary> waitEvents) {
        this.waitEvents = waitEvents;
    }

    /**
     * Returns the hierarchical representation of blocking relationships.
     * <p>
     * The returned list is mutable and may be modified by the caller.
     *
     * @return the list of blocking tree nodes, never null but may be empty
     * @see BlockingTree
     */
    public List<BlockingTree> getBlockingTree() {
        return blockingTree;
    }

    /**
     * Sets the hierarchical representation of blocking relationships.
     *
     * @param blockingTree the list of blocking tree nodes to set
     * @see BlockingTree
     */
    public void setBlockingTree(List<BlockingTree> blockingTree) {
        this.blockingTree = blockingTree;
    }

    /**
     * Returns the detailed lock information from pg_locks.
     * <p>
     * The returned list is mutable and may be modified by the caller.
     *
     * @return the list of locks, never null but may be empty
     * @see LockInfo
     */
    public List<LockInfo> getLocks() {
        return locks;
    }

    /**
     * Sets the detailed lock information from pg_locks.
     *
     * @param locks the list of locks to set
     * @see LockInfo
     */
    public void setLocks(List<LockInfo> locks) {
        this.locks = locks;
    }

    /**
     * Returns the list of slowest queries from pg_stat_statements.
     * <p>
     * The returned list is mutable and may be modified by the caller.
     *
     * @return the list of slow queries, never null but may be empty
     * @see SlowQuery
     */
    public List<SlowQuery> getTopSlowQueries() {
        return topSlowQueries;
    }

    /**
     * Sets the list of slowest queries from pg_stat_statements.
     *
     * @param topSlowQueries the list of slow queries to set
     * @see SlowQuery
     */
    public void setTopSlowQueries(List<SlowQuery> topSlowQueries) {
        this.topSlowQueries = topSlowQueries;
    }

    /**
     * Returns the metadata about the PostgreSQL database instance.
     *
     * @return the database information, may be null if not yet captured
     * @see DatabaseInfo
     */
    public DatabaseInfo getDatabaseInfo() {
        return databaseInfo;
    }

    /**
     * Sets the metadata about the PostgreSQL database instance.
     *
     * @param databaseInfo the database information to set
     * @see DatabaseInfo
     */
    public void setDatabaseInfo(DatabaseInfo databaseInfo) {
        this.databaseInfo = databaseInfo;
    }

    /**
     * Recalculates activity counts from the activities list.
     * <p>
     * This method is called automatically by {@link #setActivities(List)} to update
     * the {@link #activeQueryCount}, {@link #blockedQueryCount}, and
     * {@link #idleConnectionCount} fields based on the current activities.
     * <p>
     * The categorisation logic:
     * <ul>
     *   <li>Blocked: Activities with a non-null blocking PID</li>
     *   <li>Active: Activities with state "active" and not blocked</li>
     *   <li>Idle: All other activities (idle, idle in transaction, etc.)</li>
     * </ul>
     *
     * @see #setActivities(List)
     */
    private void recalculateActivityCounts() {
        activeQueryCount = 0;
        blockedQueryCount = 0;
        idleConnectionCount = 0;

        for (Activity a : activities) {
            String state = a.getState();
            boolean isActive = "active".equalsIgnoreCase(state);
            boolean isBlocked = a.getBlockingPid() != null;

            if (isBlocked) {
                blockedQueryCount++;
            } else if (isActive) {
                activeQueryCount++;
            } else {
                idleConnectionCount++;
            }
        }
    }

    /**
     * Returns the capture timestamp formatted for human-readable display.
     * <p>
     * Uses the format "yyyy-MM-dd HH:mm:ss" (e.g., "2026-01-05 14:30:22").
     *
     * @return the formatted timestamp, or an empty string if capturedAt is null
     * @see #DISPLAY_FORMAT
     */
    public String getCapturedAtFormatted() {
        return capturedAt != null ? DISPLAY_FORMAT.format(capturedAt) : "";
    }

    /**
     * Returns a filename-safe timestamp string for file naming.
     * <p>
     * Uses the format "yyyyMMdd-HHmmss" (e.g., "20260105-143022").
     * This format is safe for use in filenames across all operating systems.
     *
     * @return the filename-safe timestamp, or an empty string if capturedAt is null
     * @see #FILENAME_FORMAT
     */
    public String getFilenameTimestamp() {
        return capturedAt != null ? FILENAME_FORMAT.format(capturedAt) : "";
    }

    /**
     * Checks if there are blocking issues present in this snapshot.
     * <p>
     * Blocking issues are indicated by either:
     * <ul>
     *   <li>One or more blocked queries ({@link #blockedQueryCount} &gt; 0)</li>
     *   <li>Non-empty blocking tree (hierarchical lock dependencies)</li>
     * </ul>
     *
     * @return {@code true} if blocking issues are detected, {@code false} otherwise
     */
    public boolean hasBlockingIssues() {
        return blockedQueryCount > 0 || !blockingTree.isEmpty();
    }

    /**
     * Returns the total number of database connections captured in this snapshot.
     * <p>
     * This is equivalent to the size of the {@link #activities} list.
     *
     * @return the total number of connections, zero if no activities were captured
     */
    public int getTotalConnections() {
        return activities.size();
    }

    /**
     * Returns the total number of sessions experiencing wait events.
     * <p>
     * This sums the session counts across all wait event types in the
     * {@link #waitEvents} list. Note that a single session may appear in
     * multiple wait event categories if it transitions between wait states.
     *
     * @return the total number of waiting sessions, zero if no wait events
     * @see WaitEventSummary#getSessionCount()
     */
    public int getTotalWaitEvents() {
        return waitEvents.stream()
                         .mapToInt(WaitEventSummary::getSessionCount)
                         .sum();
    }

    /**
     * Generates a concise summary string for this incident report.
     * <p>
     * The summary includes instance name, capture timestamp, and connection counts
     * in a pipe-delimited format suitable for logging or display in tabular views.
     * <p>
     * Example output:
     * <pre>
     * Instance: db-prod-01 | Captured: 2026-01-05 14:30:22 | Connections: 47 | Active: 12 | Blocked: 3
     * </pre>
     *
     * @return a single-line summary string, never null
     * @see #getCapturedAtFormatted()
     * @see #getTotalConnections()
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Instance: ").append(instanceName);
        sb.append(" | Captured: ").append(getCapturedAtFormatted());
        sb.append(" | Connections: ").append(getTotalConnections());
        sb.append(" | Active: ").append(activeQueryCount);
        sb.append(" | Blocked: ").append(blockedQueryCount);
        return sb.toString();
    }
}
