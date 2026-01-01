package com.bovinemagnet.pgconsole.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a point-in-time snapshot of database state for incident analysis.
 * Contains all relevant metrics and activity information captured at a specific moment.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class IncidentReport {

    private static final DateTimeFormatter FILENAME_FORMAT =
        DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter DISPLAY_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String reportId;
    private String instanceName;
    private LocalDateTime capturedAt;
    private String description;

    // Overview stats
    private OverviewStats overviewStats;

    // Current activity
    private List<Activity> activities = new ArrayList<>();
    private int activeQueryCount;
    private int blockedQueryCount;
    private int idleConnectionCount;

    // Wait events
    private List<WaitEventSummary> waitEvents = new ArrayList<>();

    // Lock information
    private List<BlockingTree> blockingTree = new ArrayList<>();
    private List<LockInfo> locks = new ArrayList<>();

    // Top slow queries
    private List<SlowQuery> topSlowQueries = new ArrayList<>();

    // Database info
    private DatabaseInfo databaseInfo;

    public IncidentReport() {
        this.capturedAt = LocalDateTime.now();
        this.reportId = "IR-" + FILENAME_FORMAT.format(capturedAt);
    }

    public IncidentReport(String instanceName) {
        this();
        this.instanceName = instanceName;
    }

    // Getters and setters

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public LocalDateTime getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(LocalDateTime capturedAt) {
        this.capturedAt = capturedAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public OverviewStats getOverviewStats() {
        return overviewStats;
    }

    public void setOverviewStats(OverviewStats overviewStats) {
        this.overviewStats = overviewStats;
    }

    public List<Activity> getActivities() {
        return activities;
    }

    public void setActivities(List<Activity> activities) {
        this.activities = activities;
        recalculateActivityCounts();
    }

    public int getActiveQueryCount() {
        return activeQueryCount;
    }

    public int getBlockedQueryCount() {
        return blockedQueryCount;
    }

    public int getIdleConnectionCount() {
        return idleConnectionCount;
    }

    public List<WaitEventSummary> getWaitEvents() {
        return waitEvents;
    }

    public void setWaitEvents(List<WaitEventSummary> waitEvents) {
        this.waitEvents = waitEvents;
    }

    public List<BlockingTree> getBlockingTree() {
        return blockingTree;
    }

    public void setBlockingTree(List<BlockingTree> blockingTree) {
        this.blockingTree = blockingTree;
    }

    public List<LockInfo> getLocks() {
        return locks;
    }

    public void setLocks(List<LockInfo> locks) {
        this.locks = locks;
    }

    public List<SlowQuery> getTopSlowQueries() {
        return topSlowQueries;
    }

    public void setTopSlowQueries(List<SlowQuery> topSlowQueries) {
        this.topSlowQueries = topSlowQueries;
    }

    public DatabaseInfo getDatabaseInfo() {
        return databaseInfo;
    }

    public void setDatabaseInfo(DatabaseInfo databaseInfo) {
        this.databaseInfo = databaseInfo;
    }

    /**
     * Recalculates activity counts from the activities list.
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
     * Returns the capture timestamp formatted for display.
     */
    public String getCapturedAtFormatted() {
        return capturedAt != null ? DISPLAY_FORMAT.format(capturedAt) : "";
    }

    /**
     * Returns a filename-safe timestamp string.
     */
    public String getFilenameTimestamp() {
        return capturedAt != null ? FILENAME_FORMAT.format(capturedAt) : "";
    }

    /**
     * Checks if there are blocking issues in this snapshot.
     */
    public boolean hasBlockingIssues() {
        return blockedQueryCount > 0 || !blockingTree.isEmpty();
    }

    /**
     * Returns the total number of connections captured.
     */
    public int getTotalConnections() {
        return activities.size();
    }

    /**
     * Returns the total number of wait events captured.
     */
    public int getTotalWaitEvents() {
        return waitEvents.stream()
                         .mapToInt(WaitEventSummary::getSessionCount)
                         .sum();
    }

    /**
     * Generates a summary string for the report.
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
