package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.Activity;
import com.bovinemagnet.pgconsole.model.BlockingTree;
import com.bovinemagnet.pgconsole.model.IncidentReport;
import com.bovinemagnet.pgconsole.model.LockInfo;
import com.bovinemagnet.pgconsole.model.SlowQuery;
import com.bovinemagnet.pgconsole.model.WaitEventSummary;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Service for generating incident reports.
 * Captures a point-in-time snapshot of database state for analysis.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class IncidentReportService {

    private static final int TOP_QUERIES_LIMIT = 20;

    @Inject
    PostgresService postgresService;

    /**
     * Captures a comprehensive point-in-time incident report for the specified instance.
     * <p>
     * Collects a snapshot of the database state including overview statistics, current activity,
     * wait events, blocking chains, lock information, top slow queries, and database configuration.
     * This snapshot is useful for post-incident analysis and troubleshooting.
     * <p>
     * The report limits slow queries to the top 20 by total execution time to keep report size
     * manageable whilst capturing the most impactful queries.
     *
     * @param instanceName the database instance identifier to capture
     * @param description optional description of the incident for context; may be null
     * @return complete incident report with all captured metrics and state information
     */
    public IncidentReport captureReport(String instanceName, String description) {
        IncidentReport report = new IncidentReport(instanceName);
        report.setDescription(description);

        // Capture overview stats
        report.setOverviewStats(postgresService.getOverviewStats(instanceName));

        // Capture current activity
        report.setActivities(postgresService.getCurrentActivity(instanceName));

        // Capture wait events
        report.setWaitEvents(postgresService.getWaitEventSummary(instanceName));

        // Capture blocking tree and locks
        report.setBlockingTree(postgresService.getBlockingTree(instanceName));
        report.setLocks(postgresService.getLockInfo(instanceName));

        // Capture top slow queries (limited to reduce report size)
        List<SlowQuery> allQueries = postgresService.getSlowQueries(instanceName, "totalTime", "desc");
        if (allQueries.size() > TOP_QUERIES_LIMIT) {
            report.setTopSlowQueries(allQueries.subList(0, TOP_QUERIES_LIMIT));
        } else {
            report.setTopSlowQueries(allQueries);
        }

        // Capture database info
        report.setDatabaseInfo(postgresService.getDatabaseInfo(instanceName));

        return report;
    }

    /**
     * Generates a human-readable text-based report suitable for export and analysis.
     * <p>
     * Formats the incident report as structured plain text with sections for overview
     * statistics, active connections, wait events, blocking chains, locks, slow queries,
     * and database information. Active and blocked queries are highlighted, whilst idle
     * connections are excluded for clarity.
     * <p>
     * The lock section is truncated to the first 50 locks to prevent excessive output
     * for databases with many concurrent locks.
     *
     * @param report the incident report to format
     * @return formatted multi-line text report ready for export or logging
     */
    public String formatAsText(IncidentReport report) {
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("=" .repeat(80)).append("\n");
        sb.append("INCIDENT REPORT: ").append(report.getReportId()).append("\n");
        sb.append("=" .repeat(80)).append("\n\n");

        // Summary
        sb.append("Instance:    ").append(report.getInstanceName()).append("\n");
        sb.append("Captured:    ").append(report.getCapturedAtFormatted()).append("\n");
        if (report.getDescription() != null && !report.getDescription().isEmpty()) {
            sb.append("Description: ").append(report.getDescription()).append("\n");
        }
        sb.append("\n");

        // Overview Stats
        sb.append("-".repeat(80)).append("\n");
        sb.append("OVERVIEW STATS\n");
        sb.append("-".repeat(80)).append("\n");
        if (report.getOverviewStats() != null) {
            var stats = report.getOverviewStats();
            sb.append(String.format("Total Connections: %d / %d (max)%n",
                stats.getConnectionsUsed(), stats.getConnectionsMax()));
            sb.append(String.format("Active Queries:    %d%n", stats.getActiveQueries()));
            sb.append(String.format("Blocked Queries:   %d%n", stats.getBlockedQueries()));
            sb.append(String.format("Cache Hit Ratio:   %s%n", stats.getCacheHitRatioFormatted()));
            sb.append(String.format("Database Size:     %s%n", stats.getDatabaseSize()));
        }
        sb.append("\n");

        // Current Activity
        sb.append("-".repeat(80)).append("\n");
        sb.append("CURRENT ACTIVITY (").append(report.getTotalConnections()).append(" connections)\n");
        sb.append("-".repeat(80)).append("\n");
        sb.append(String.format("Active: %d | Blocked: %d | Idle: %d%n%n",
            report.getActiveQueryCount(), report.getBlockedQueryCount(), report.getIdleConnectionCount()));

        for (Activity a : report.getActivities()) {
            String state = a.getState();
            boolean isActive = "active".equalsIgnoreCase(state);
            boolean isBlocked = a.getBlockingPid() != null;
            if (isActive || isBlocked) {
                sb.append(String.format("PID: %d | User: %s | Database: %s%n",
                    a.getPid(), a.getUser(), a.getDatabase()));
                sb.append(String.format("State: %s | Wait: %s%n",
                    state, a.getWaitEvent() != null ? a.getWaitEvent() : "-"));
                if (a.getQuery() != null) {
                    sb.append("Query: ").append(truncate(a.getQuery(), 200)).append("\n");
                }
                sb.append("\n");
            }
        }

        // Wait Events
        sb.append("-".repeat(80)).append("\n");
        sb.append("WAIT EVENTS\n");
        sb.append("-".repeat(80)).append("\n");
        if (report.getWaitEvents().isEmpty()) {
            sb.append("No wait events recorded.\n");
        } else {
            for (WaitEventSummary we : report.getWaitEvents()) {
                sb.append(String.format("%-20s | %-30s | %d sessions%n",
                    we.getWaitEventType() != null ? we.getWaitEventType() : "(Active)",
                    we.getWaitEvent() != null ? we.getWaitEvent() : "-",
                    we.getSessionCount()));
            }
        }
        sb.append("\n");

        // Blocking Tree
        sb.append("-".repeat(80)).append("\n");
        sb.append("BLOCKING TREE\n");
        sb.append("-".repeat(80)).append("\n");
        if (report.getBlockingTree().isEmpty()) {
            sb.append("No blocking chains detected.\n");
        } else {
            for (BlockingTree bt : report.getBlockingTree()) {
                sb.append(String.format("Blocker PID %d (%s) -> Blocked PID %d (%s)%n",
                    bt.getBlockerPid(), bt.getBlockerUser(),
                    bt.getBlockedPid(), bt.getBlockedUser()));
                sb.append(String.format("  Lock Mode: %s | Blocked Duration: %s%n",
                    bt.getLockMode(), bt.getBlockedDuration()));
                if (bt.getBlockerQuery() != null) {
                    sb.append("  Blocker Query: ").append(truncate(bt.getBlockerQuery(), 100)).append("\n");
                }
                if (bt.getBlockedQuery() != null) {
                    sb.append("  Blocked Query: ").append(truncate(bt.getBlockedQuery(), 100)).append("\n");
                }
                sb.append("\n");
            }
        }

        // Locks
        sb.append("-".repeat(80)).append("\n");
        sb.append("ACTIVE LOCKS (").append(report.getLocks().size()).append(" locks)\n");
        sb.append("-".repeat(80)).append("\n");
        int lockCount = 0;
        for (LockInfo lock : report.getLocks()) {
            if (lockCount++ >= 50) {
                sb.append("... (truncated, ").append(report.getLocks().size() - 50).append(" more locks)\n");
                break;
            }
            sb.append(String.format("PID %d | %s | %s | Granted: %s%n",
                lock.getPid(), lock.getLockType(), lock.getMode(), lock.isGranted() ? "Yes" : "No"));
        }
        sb.append("\n");

        // Top Slow Queries
        sb.append("-".repeat(80)).append("\n");
        sb.append("TOP SLOW QUERIES (by total time)\n");
        sb.append("-".repeat(80)).append("\n");
        int queryNum = 1;
        for (SlowQuery q : report.getTopSlowQueries()) {
            sb.append(String.format("%d. Calls: %d | Total: %s | Mean: %s%n",
                queryNum++, q.getTotalCalls(), q.getTotalTimeFormatted(), q.getMeanTimeFormatted()));
            sb.append("   ").append(truncate(q.getQuery(), 150)).append("\n\n");
        }

        // Database Info
        sb.append("-".repeat(80)).append("\n");
        sb.append("DATABASE INFO\n");
        sb.append("-".repeat(80)).append("\n");
        if (report.getDatabaseInfo() != null) {
            var db = report.getDatabaseInfo();
            sb.append(String.format("PostgreSQL Version: %s%n", db.getPostgresVersion()));
            sb.append(String.format("Server Start Time:  %s%n", db.getServerStartTime()));
            sb.append(String.format("Current Database:   %s%n", db.getCurrentDatabase()));
            sb.append(String.format("Current User:       %s%n", db.getCurrentUser()));
        }

        sb.append("\n");
        sb.append("=".repeat(80)).append("\n");
        sb.append("END OF REPORT\n");
        sb.append("=".repeat(80)).append("\n");

        return sb.toString();
    }

    /**
     * Truncates and normalises text for single-line display in reports.
     * <p>
     * Replaces all whitespace (including newlines) with single spaces and truncates
     * to the specified maximum length with ellipsis if necessary.
     *
     * @param text the text to truncate; may be null
     * @param maxLength maximum length of returned string including ellipsis
     * @return truncated and normalised text, or empty string if input is null
     */
    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        // Replace newlines with spaces for single-line display
        String cleaned = text.replaceAll("\\s+", " ").trim();
        if (cleaned.length() <= maxLength) {
            return cleaned;
        }
        return cleaned.substring(0, maxLength - 3) + "...";
    }
}
