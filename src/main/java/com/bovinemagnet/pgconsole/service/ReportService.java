package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.OverviewStats;
import com.bovinemagnet.pgconsole.model.SlowQuery;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for generating and sending scheduled PostgreSQL monitoring reports.
 * <p>
 * Provides functionality to create daily and weekly summary reports containing
 * database overview statistics, slow query analysis, index recommendations, and
 * maintenance suggestions. Supports automated scheduling and email delivery via
 * the Quarkus Mailer.
 * <p>
 * Reports are HTML-formatted and can be sent to multiple recipients. The service
 * uses a scheduled task to process pending reports based on their configuration.
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see ScheduledReport
 */
@ApplicationScoped
public class ReportService {

    private static final Logger LOG = Logger.getLogger(ReportService.class);

    @Inject
    DataSource dataSource;

    @Inject
    PostgresService postgresService;

    @Inject
    IndexAdvisorService indexAdvisorService;

    @Inject
    TableMaintenanceService tableMaintenanceService;

    @Inject
    Mailer mailer;

    @ConfigProperty(name = "pg-console.reports.enabled", defaultValue = "false")
    boolean reportsEnabled;

    /**
     * Processes all scheduled reports that are due for execution.
     * <p>
     * This method runs every 60 seconds and checks for enabled reports where the
     * next run time has been reached. For each due report, it generates and sends
     * the report, then updates the next run timestamp.
     * <p>
     * Processing only occurs if reports are enabled via the {@code pg-console.reports.enabled}
     * configuration property.
     *
     * @see #getDueReports()
     * @see #sendReport(ScheduledReport)
     */
    @Scheduled(every = "60s", identity = "report-scheduler")
    public void processScheduledReports() {
        if (!reportsEnabled) {
            return;
        }

        List<ScheduledReport> dueReports = getDueReports();
        for (ScheduledReport report : dueReports) {
            try {
                sendReport(report);
                updateLastRun(report);
            } catch (Exception e) {
                LOG.errorf("Failed to send report %s: %s", report.getName(), e.getMessage());
            }
        }
    }

    /**
     * Generates an HTML-formatted daily summary report for a database instance.
     * <p>
     * The report includes:
     * <ul>
     *   <li>Database overview statistics (version, connections, cache hit ratio, size)</li>
     *   <li>Top 10 slowest queries by total execution time</li>
     *   <li>Index recommendations (up to 5)</li>
     *   <li>Table maintenance recommendations (up to 5)</li>
     * </ul>
     * <p>
     * The generated HTML includes inline CSS styling for consistent formatting
     * across email clients.
     *
     * @param instanceId the database instance identifier
     * @return HTML-formatted report content. If an error occurs during generation,
     *         the error message is included in the returned HTML.
     * @see PostgresService#getOverviewStats(String)
     * @see PostgresService#getSlowQueries(String, String, String)
     * @see IndexAdvisorService#getRecommendations(String)
     * @see TableMaintenanceService#getRecommendations(String)
     */
    public String generateDailySummary(String instanceId) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; }");
        html.append("h1 { color: #336699; }");
        html.append("h2 { color: #666; border-bottom: 1px solid #ccc; padding-bottom: 5px; }");
        html.append("table { border-collapse: collapse; width: 100%; margin-bottom: 20px; }");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
        html.append("th { background-color: #f5f5f5; }");
        html.append(".warning { background-color: #fff3cd; }");
        html.append(".danger { background-color: #f8d7da; }");
        html.append(".success { background-color: #d4edda; }");
        html.append("</style></head><body>");

        // Header
        html.append("<h1>PostgreSQL Daily Summary Report</h1>");
        html.append("<p><strong>Instance:</strong> ").append(instanceId).append("</p>");
        html.append("<p><strong>Generated:</strong> ")
            .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            .append("</p>");

        try {
            // Overview stats
            OverviewStats stats = postgresService.getOverviewStats(instanceId);
            html.append("<h2>Database Overview</h2>");
            html.append("<table>");
            html.append("<tr><th>Metric</th><th>Value</th></tr>");
            html.append("<tr><td>Version</td><td>").append(stats.getVersion()).append("</td></tr>");
            html.append("<tr><td>Connections</td><td>")
                .append(stats.getActiveConnections()).append(" / ").append(stats.getMaxConnections())
                .append("</td></tr>");
            html.append("<tr><td>Active Queries</td><td>").append(stats.getActiveQueries()).append("</td></tr>");
            html.append("<tr><td>Blocked Queries</td><td>").append(stats.getBlockedQueries()).append("</td></tr>");
            html.append("<tr><td>Cache Hit Ratio</td><td>").append(stats.getCacheHitRatioFormatted()).append("</td></tr>");
            html.append("<tr><td>Database Size</td><td>").append(stats.getDatabaseSize()).append("</td></tr>");
            html.append("</table>");

            // Top slow queries
            List<SlowQuery> slowQueries = postgresService.getSlowQueries(instanceId, "totalTime", "desc");
            html.append("<h2>Top 10 Slow Queries</h2>");
            html.append("<table>");
            html.append("<tr><th>Query</th><th>Calls</th><th>Total Time</th><th>Mean Time</th></tr>");
            int count = 0;
            for (SlowQuery query : slowQueries) {
                if (count++ >= 10) break;
                html.append("<tr>");
                String queryPreview = query.getQuery();
                if (queryPreview.length() > 100) {
                    queryPreview = queryPreview.substring(0, 100) + "...";
                }
                html.append("<td><code>").append(escapeHtml(queryPreview)).append("</code></td>");
                html.append("<td>").append(query.getTotalCalls()).append("</td>");
                html.append("<td>").append(query.getTotalTimeFormatted()).append("</td>");
                html.append("<td>").append(query.getMeanTimeFormatted()).append("</td>");
                html.append("</tr>");
            }
            html.append("</table>");

            // Index recommendations
            var indexRecs = indexAdvisorService.getRecommendations(instanceId);
            if (!indexRecs.isEmpty()) {
                html.append("<h2>Index Recommendations</h2>");
                html.append("<table>");
                html.append("<tr><th>Type</th><th>Table</th><th>Recommendation</th></tr>");
                for (var rec : indexRecs) {
                    if (count++ >= 5) break;
                    html.append("<tr>");
                    html.append("<td>").append(rec.getTypeDisplay()).append("</td>");
                    html.append("<td>").append(rec.getFullTableName()).append("</td>");
                    html.append("<td>").append(rec.getRecommendation()).append("</td>");
                    html.append("</tr>");
                }
                html.append("</table>");
            }

            // Maintenance recommendations
            var maintenanceRecs = tableMaintenanceService.getRecommendations(instanceId);
            if (!maintenanceRecs.isEmpty()) {
                html.append("<h2>Maintenance Recommendations</h2>");
                html.append("<table>");
                html.append("<tr><th>Table</th><th>Action</th><th>Urgency</th></tr>");
                count = 0;
                for (var rec : maintenanceRecs) {
                    if (count++ >= 5) break;
                    html.append("<tr>");
                    html.append("<td>").append(rec.getFullTableName()).append("</td>");
                    html.append("<td>").append(rec.getRecommendation()).append("</td>");
                    html.append("<td>").append(rec.getSeverity()).append("</td>");
                    html.append("</tr>");
                }
                html.append("</table>");
            }

        } catch (Exception e) {
            html.append("<p class=\"danger\">Error generating report: ")
                .append(escapeHtml(e.getMessage())).append("</p>");
        }

        html.append("<hr><p><em>Generated by PostgreSQL Console</em></p>");
        html.append("</body></html>");

        return html.toString();
    }

    /**
     * Generates and sends a report to all configured recipients.
     * <p>
     * The report content is generated based on the report type (daily_summary,
     * weekly_summary, etc.) and sent via email to each recipient. Failures to
     * individual recipients are logged but do not prevent delivery to others.
     *
     * @param report the scheduled report configuration containing type, instance,
     *               and recipient list
     * @see #generateDailySummary(String)
     */
    public void sendReport(ScheduledReport report) {
        String content = switch (report.getReportType()) {
            case "daily_summary" -> generateDailySummary(report.getInstanceId());
            case "weekly_summary" -> generateDailySummary(report.getInstanceId()); // Same format for now
            default -> generateDailySummary(report.getInstanceId());
        };

        for (String recipient : report.getRecipients()) {
            try {
                mailer.send(Mail.withHtml(recipient,
                        "PostgreSQL Report: " + report.getName(),
                        content));
                LOG.infof("Sent report '%s' to %s", report.getName(), recipient);
            } catch (Exception e) {
                LOG.errorf("Failed to send report to %s: %s", recipient, e.getMessage());
            }
        }
    }

    /**
     * Retrieves all enabled reports that are due for execution.
     * <p>
     * Queries the {@code pgconsole.scheduled_report} table for reports where:
     * <ul>
     *   <li>The report is enabled</li>
     *   <li>The next_run_at timestamp is null or in the past</li>
     * </ul>
     *
     * @return list of scheduled reports ready for execution, or an empty list
     *         if no reports are due or if an error occurs
     */
    public List<ScheduledReport> getDueReports() {
        List<ScheduledReport> reports = new ArrayList<>();

        String sql = """
            SELECT id, name, instance_id, report_type, schedule, recipients, config
            FROM pgconsole.scheduled_report
            WHERE enabled = TRUE
              AND (next_run_at IS NULL OR next_run_at <= NOW())
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                reports.add(mapReport(rs));
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to get due reports: %s", e.getMessage());
        }

        return reports;
    }

    /**
     * Retrieves all configured scheduled reports regardless of status.
     * <p>
     * Returns complete report configurations including enabled/disabled state,
     * last run time, and next scheduled run time for administrative purposes.
     *
     * @return list of all scheduled reports ordered by name, or an empty list
     *         if no reports exist or if an error occurs
     */
    public List<ScheduledReport> getAllReports() {
        List<ScheduledReport> reports = new ArrayList<>();

        String sql = """
            SELECT id, name, instance_id, report_type, schedule, recipients, enabled,
                   last_run_at, next_run_at, config
            FROM pgconsole.scheduled_report
            ORDER BY name
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                ScheduledReport report = mapReport(rs);
                report.setEnabled(rs.getBoolean("enabled"));
                Timestamp lastRun = rs.getTimestamp("last_run_at");
                if (lastRun != null) {
                    report.setLastRunAt(lastRun.toInstant());
                }
                Timestamp nextRun = rs.getTimestamp("next_run_at");
                if (nextRun != null) {
                    report.setNextRunAt(nextRun.toInstant());
                }
                reports.add(report);
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to get scheduled reports: %s", e.getMessage());
        }

        return reports;
    }

    /**
     * Creates a new scheduled report configuration in the database.
     * <p>
     * Persists the report configuration to the {@code pgconsole.scheduled_report}
     * table and calculates the initial next run timestamp based on the schedule.
     * The generated report ID is set on the provided report object.
     *
     * @param report the report configuration to create. Must have name, instanceId,
     *               reportType, schedule, and recipients populated.
     * @return the same report object with the generated ID populated, or the
     *         original object if creation fails
     */
    public ScheduledReport createReport(ScheduledReport report) {
        String sql = """
            INSERT INTO pgconsole.scheduled_report
            (name, instance_id, report_type, schedule, recipients, enabled, next_run_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, report.getName());
            stmt.setString(2, report.getInstanceId());
            stmt.setString(3, report.getReportType());
            stmt.setString(4, report.getSchedule());
            stmt.setArray(5, conn.createArrayOf("TEXT", report.getRecipients().toArray()));
            stmt.setBoolean(6, report.isEnabled());
            stmt.setTimestamp(7, Timestamp.from(calculateNextRun(report.getSchedule())));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    report.setId(rs.getLong("id"));
                }
            }

            LOG.infof("Created scheduled report: %s", report.getName());

        } catch (SQLException e) {
            LOG.errorf("Failed to create scheduled report: %s", e.getMessage());
        }

        return report;
    }

    /**
     * Updates the last run timestamp and calculates the next scheduled run time.
     * <p>
     * Called after successfully sending a report to record execution and schedule
     * the next occurrence based on the report's schedule configuration.
     *
     * @param report the report whose run timestamps should be updated
     */
    private void updateLastRun(ScheduledReport report) {
        String sql = """
            UPDATE pgconsole.scheduled_report
            SET last_run_at = NOW(), next_run_at = ?
            WHERE id = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.from(calculateNextRun(report.getSchedule())));
            stmt.setLong(2, report.getId());
            stmt.executeUpdate();

        } catch (SQLException e) {
            LOG.errorf("Failed to update report last run: %s", e.getMessage());
        }
    }

    private Instant calculateNextRun(String schedule) {
        // Simple schedule parsing: daily, weekly, or cron-like
        LocalDateTime now = LocalDateTime.now();
        return switch (schedule.toLowerCase()) {
            case "daily" -> now.plusDays(1).withHour(8).withMinute(0).atZone(ZoneId.systemDefault()).toInstant();
            case "weekly" -> now.plusWeeks(1).withHour(8).withMinute(0).atZone(ZoneId.systemDefault()).toInstant();
            default -> now.plusDays(1).atZone(ZoneId.systemDefault()).toInstant();
        };
    }

    private ScheduledReport mapReport(ResultSet rs) throws SQLException {
        ScheduledReport report = new ScheduledReport();
        report.setId(rs.getLong("id"));
        report.setName(rs.getString("name"));
        report.setInstanceId(rs.getString("instance_id"));
        report.setReportType(rs.getString("report_type"));
        report.setSchedule(rs.getString("schedule"));

        var recipientsArray = rs.getArray("recipients");
        if (recipientsArray != null) {
            String[] recipients = (String[]) recipientsArray.getArray();
            report.setRecipients(List.of(recipients));
        }

        return report;
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    /**
     * Configuration for a scheduled database monitoring report.
     * <p>
     * Encapsulates all settings for automated report generation including
     * schedule frequency, target instance, report type, recipient list, and
     * execution history. Supports standard schedules (daily, weekly) and
     * custom cron-like expressions.
     */
    public static class ScheduledReport {
        private long id;
        private String name;
        private String instanceId;
        private String reportType;
        private String schedule;
        private List<String> recipients;
        private boolean enabled = true;
        private Instant lastRunAt;
        private Instant nextRunAt;

        public long getId() { return id; }
        public void setId(long id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getInstanceId() { return instanceId; }
        public void setInstanceId(String id) { this.instanceId = id; }

        public String getReportType() { return reportType; }
        public void setReportType(String type) { this.reportType = type; }

        public String getSchedule() { return schedule; }
        public void setSchedule(String schedule) { this.schedule = schedule; }

        public List<String> getRecipients() { return recipients; }
        public void setRecipients(List<String> recipients) { this.recipients = recipients; }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public Instant getLastRunAt() { return lastRunAt; }
        public void setLastRunAt(Instant time) { this.lastRunAt = time; }

        public String getLastRunAtFormatted() {
            if (lastRunAt == null) return "Never";
            return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    .withZone(ZoneId.systemDefault())
                    .format(lastRunAt);
        }

        public Instant getNextRunAt() { return nextRunAt; }
        public void setNextRunAt(Instant time) { this.nextRunAt = time; }

        public String getNextRunAtFormatted() {
            if (nextRunAt == null) return "Not scheduled";
            return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    .withZone(ZoneId.systemDefault())
                    .format(nextRunAt);
        }
    }
}
