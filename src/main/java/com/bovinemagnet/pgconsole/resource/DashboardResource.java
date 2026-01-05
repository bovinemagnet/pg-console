package com.bovinemagnet.pgconsole.resource;

import com.bovinemagnet.pgconsole.config.InstanceConfig;
import com.bovinemagnet.pgconsole.model.Activity;
import com.bovinemagnet.pgconsole.model.BlockingTree;
import com.bovinemagnet.pgconsole.model.DatabaseInfo;
import com.bovinemagnet.pgconsole.model.DatabaseMetrics;
import com.bovinemagnet.pgconsole.model.ExplainPlan;
import com.bovinemagnet.pgconsole.model.IncidentReport;
import com.bovinemagnet.pgconsole.model.LockInfo;
import com.bovinemagnet.pgconsole.model.OverviewStats;
import com.bovinemagnet.pgconsole.model.QueryFingerprint;
import com.bovinemagnet.pgconsole.model.SlowQuery;
import com.bovinemagnet.pgconsole.model.TableStats;
import com.bovinemagnet.pgconsole.model.WaitEventSummary;
import com.bovinemagnet.pgconsole.service.AuditService;
import com.bovinemagnet.pgconsole.service.BookmarkService;
import com.bovinemagnet.pgconsole.service.ChangeDataCaptureService;
import com.bovinemagnet.pgconsole.service.ComparisonService;
import com.bovinemagnet.pgconsole.service.ComplianceService;
import com.bovinemagnet.pgconsole.service.ConnectionSecurityService;
import com.bovinemagnet.pgconsole.service.DataAccessPatternService;
import com.bovinemagnet.pgconsole.service.DataSourceManager;
import com.bovinemagnet.pgconsole.service.LogicalReplicationService;
import com.bovinemagnet.pgconsole.service.PartitioningService;
import com.bovinemagnet.pgconsole.service.SchemaChangeService;
import com.bovinemagnet.pgconsole.service.IncidentReportService;
import com.bovinemagnet.pgconsole.service.IndexAdvisorService;
import com.bovinemagnet.pgconsole.service.InfrastructureService;
import com.bovinemagnet.pgconsole.service.PostgresService;
import com.bovinemagnet.pgconsole.service.QueryFingerprintService;
import com.bovinemagnet.pgconsole.service.QueryRegressionService;
import com.bovinemagnet.pgconsole.service.SecurityAuditService;
import com.bovinemagnet.pgconsole.service.FeatureToggleService;
import com.bovinemagnet.pgconsole.service.HealthCheckService;
import com.bovinemagnet.pgconsole.service.SecurityRecommendationService;
import com.bovinemagnet.pgconsole.service.SparklineService;
import com.bovinemagnet.pgconsole.service.ReplicationService;
import com.bovinemagnet.pgconsole.service.StatementsManagementService;
import com.bovinemagnet.pgconsole.service.TableMaintenanceService;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * Main dashboard resource handling all web UI endpoints.
 * Supports multi-instance PostgreSQL monitoring.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@Path("/")
public class DashboardResource {

    @ConfigProperty(name = "quarkus.application.name", defaultValue = "pg-console")
    String appName;

    @ConfigProperty(name = "quarkus.application.version", defaultValue = "1.0.0")
    String appVersion;

    @Inject
    InstanceConfig config;

    @Inject
    Template index;

    @Inject
    Template slowQueries;

    @Inject
    Template activity;

    @Inject
    Template tables;

    @Inject
    Template about;

    @Inject
    Template locks;

    @Inject
    Template queryDetail;

    @Inject
    Template databases;

    @Inject
    Template databaseDetail;

    @Inject
    Template waitEvents;

    @Inject
    Template indexAdvisor;

    @Inject
    Template queryRegressions;

    @Inject
    Template tableMaintenance;

    @Inject
    Template statementsManagement;

    @Inject
    Template replication;

    @Inject
    Template infrastructure;

    @Inject
    Template configHealth;

    @Inject
    Template checkpoints;

    @Inject
    Template healthCheck;

    @Inject
    Template auditLog;

    @Inject
    Template bookmarks;

    @Inject
    Template comparison;

    @Inject
    Template logicalReplication;

    @Inject
    Template cdc;

    @Inject
    Template dataLineage;

    @Inject
    Template partitions;

    @Inject
    Template security;

    @Inject
    Template securityRoles;

    @Inject
    Template securityConnections;

    @Inject
    Template securityAccess;

    @Inject
    Template securityCompliance;

    @Inject
    Template securityRecommendations;

    @Inject
    PostgresService postgresService;

    @Inject
    SparklineService sparklineService;

    @Inject
    DataSourceManager dataSourceManager;

    @Inject
    QueryFingerprintService fingerprintService;

    @Inject
    IncidentReportService incidentReportService;

    @Inject
    IndexAdvisorService indexAdvisorService;

    @Inject
    QueryRegressionService queryRegressionService;

    @Inject
    TableMaintenanceService tableMaintenanceService;

    @Inject
    StatementsManagementService statementsManagementService;

    @Inject
    ReplicationService replicationService;

    @Inject
    InfrastructureService infrastructureService;

    @Inject
    HealthCheckService healthCheckService;

    @Inject
    AuditService auditService;

    @Inject
    BookmarkService bookmarkService;

    @Inject
    ComparisonService comparisonService;

    @Inject
    LogicalReplicationService logicalReplicationService;

    @Inject
    ChangeDataCaptureService changeDataCaptureService;

    @Inject
    SchemaChangeService schemaChangeService;

    @Inject
    PartitioningService partitioningService;

    @Inject
    SecurityAuditService securityAuditService;

    @Inject
    ConnectionSecurityService connectionSecurityService;

    @Inject
    DataAccessPatternService dataAccessPatternService;

    @Inject
    ComplianceService complianceService;

    @Inject
    SecurityRecommendationService securityRecommendationService;

    @Inject
    FeatureToggleService featureToggleService;

    /**
     * Renders the main dashboard overview page with key PostgreSQL metrics and sparklines.
     * <p>
     * Displays connection counts, active queries, blocked queries, and cache hit ratios
     * with visual sparklines showing trends over the last hour.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return template instance containing overview statistics and sparkline data
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("dashboard");
        OverviewStats stats = postgresService.getOverviewStats(instance);

        // Generate sparklines from history (last 1 hour)
        String connectionsSparkline = sparklineService.getConnectionsSparkline(instance, 1, 120, 30);
        String activeQueriesSparkline = sparklineService.getActiveQueriesSparkline(instance, 1, 120, 30);
        String blockedQueriesSparkline = sparklineService.getBlockedQueriesSparkline(instance, 1, 120, 30);
        String cacheHitSparkline = sparklineService.getCacheHitRatioSparkline(instance, 1, 120, 30);

        return index.data("stats", stats)
                    .data("connectionsSparkline", connectionsSparkline)
                    .data("activeQueriesSparkline", activeQueriesSparkline)
                    .data("blockedQueriesSparkline", blockedQueriesSparkline)
                    .data("cacheHitSparkline", cacheHitSparkline)
                    .data("instances", dataSourceManager.getInstanceInfoList())
                    .data("currentInstance", instance)
                    .data("securityEnabled", config.security().enabled())
                    .data("schemaEnabled", config.schema().enabled())
                    .data("inMemoryMinutes", config.schema().inMemoryMinutes())
                    .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Renders the slow queries page displaying pg_stat_statements data.
     * <p>
     * Supports both individual query view and grouped fingerprint view.
     * Queries can be sorted by total time, calls, mean time, or other metrics.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @param sortBy the sort field (defaults to "totalTime")
     * @param order the sort order "asc" or "desc" (defaults to "desc")
     * @param view the view mode: "individual" or "grouped" (defaults to "individual")
     * @return template instance containing slow query data
     */
    @GET
    @Path("/slow-queries")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance slowQueries(
            @QueryParam("instance") @DefaultValue("default") String instance,
            @QueryParam("sortBy") String sortBy,
            @QueryParam("order") String order,
            @QueryParam("view") @DefaultValue("individual") String view) {
        featureToggleService.requirePageEnabled("slow-queries");

        List<SlowQuery> queries = postgresService.getSlowQueries(
            instance,
            sortBy != null ? sortBy : "totalTime",
            order != null ? order : "desc"
        );

        TemplateInstance template = slowQueries.data("queries", queries)
                         .data("sortBy", sortBy != null ? sortBy : "totalTime")
                         .data("order", order != null ? order : "desc")
                         .data("view", view)
                         .data("instances", dataSourceManager.getInstanceInfoList())
                         .data("currentInstance", instance)
                         .data("securityEnabled", config.security().enabled())
                         .data("schemaEnabled", config.schema().enabled())
                         .data("inMemoryMinutes", config.schema().inMemoryMinutes())
                         .data("toggles", featureToggleService.getAllToggles());

        // Add grouped data if viewing grouped
        if ("grouped".equals(view)) {
            List<QueryFingerprint> fingerprints = fingerprintService.groupQueriesSortedByTotalTime(queries);
            template = template.data("fingerprints", fingerprints);
        }

        return template;
    }

    /**
     * Renders the current activity page showing all active database sessions.
     * <p>
     * Displays queries from pg_stat_activity including running queries, idle connections,
     * and idle-in-transaction sessions.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return template instance containing current activity data
     */
    @GET
    @Path("/activity")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance activity(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("activity");
        List<Activity> activities = postgresService.getCurrentActivity(instance);
        return activity.data("activities", activities)
                      .data("instances", dataSourceManager.getInstanceInfoList())
                      .data("currentInstance", instance)
                      .data("securityEnabled", config.security().enabled())
                      .data("schemaEnabled", config.schema().enabled())
                      .data("inMemoryMinutes", config.schema().inMemoryMinutes())
                      .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Renders the tables page showing statistics for all user tables.
     * <p>
     * Displays table sizes, row counts, index usage, sequential scans,
     * and vacuum statistics from pg_stat_user_tables.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return template instance containing table statistics
     */
    @GET
    @Path("/tables")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance tables(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("tables");
        List<TableStats> stats = postgresService.getTableStats(instance);
        return tables.data("tables", stats)
                    .data("instances", dataSourceManager.getInstanceInfoList())
                    .data("currentInstance", instance)
                    .data("securityEnabled", config.security().enabled())
                    .data("schemaEnabled", config.schema().enabled())
                    .data("inMemoryMinutes", config.schema().inMemoryMinutes())
                    .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Generates an SVG sparkline visualisation from a comma-separated list of values.
     * <p>
     * This endpoint is typically called via htmx to dynamically render sparklines.
     *
     * @param values comma-separated list of numeric values
     * @return SVG sparkline as HTML string, or empty string if values are null/empty
     */
    @GET
    @Path("/api/sparkline")
    @Produces(MediaType.TEXT_HTML)
    public String sparkline(@QueryParam("values") String values) {
        if (values == null || values.isEmpty()) {
            return "";
        }

        List<Double> valueList = Arrays.stream(values.split(","))
            .map(String::trim)
            .map(Double::parseDouble)
            .toList();

        return sparklineService.generateSparkline(valueList, 100, 30);
    }

    // --- Drill-Down Tooltip Fragments (Phase 21) ---

    /**
     * Returns a drill-down fragment showing top databases by active connections.
     * <p>
     * Displays a list of the top 5 databases ranked by current connection count,
     * with connection numbers and a brief explanation of the metric.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return HTML fragment for the connections drill-down tooltip
     */
    @GET
    @Path("/fragments/drilldown/connections")
    @Produces(MediaType.TEXT_HTML)
    public String drilldownConnections(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        var stats = postgresService.getOverviewStats(instance);
        var dbMetrics = postgresService.getAllDatabaseMetrics(instance);

        // Sort by numBackends (active connections) descending
        var topDatabases = dbMetrics.stream()
                .sorted((a, b) -> Long.compare(b.getNumBackends(), a.getNumBackends()))
                .limit(5)
                .toList();

        StringBuilder html = new StringBuilder();
        html.append("<div class=\"drilldown-header\"><i class=\"bi bi-plug\"></i> Connections by Database</div>");
        html.append("<div class=\"drilldown-body\">");

        if (topDatabases.isEmpty()) {
            html.append("<div class=\"drilldown-item\"><span class=\"text-muted\">No database connections</span></div>");
        } else {
            for (var db : topDatabases) {
                String valueClass = db.getNumBackends() > 10 ? "drilldown-value-warning" : "";
                html.append("<div class=\"drilldown-item\">");
                html.append("<span class=\"drilldown-item-label\"><code>").append(escapeHtml(db.getDatname())).append("</code></span>");
                html.append("<span class=\"drilldown-item-value ").append(valueClass).append("\">").append(db.getNumBackends()).append("</span>");
                html.append("</div>");
            }
        }

        html.append("</div>");
        html.append("<div class=\"drilldown-explanation\"><i class=\"bi bi-info-circle\"></i> ");
        html.append("Active backend connections per database. High counts may indicate connection pool saturation.");
        html.append("</div>");

        return html.toString();
    }

    /**
     * Returns a drill-down fragment showing longest running active queries.
     * <p>
     * Displays the top 5 currently running queries ranked by execution duration,
     * with truncated query text and duration values.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return HTML fragment for the active queries drill-down tooltip
     */
    @GET
    @Path("/fragments/drilldown/active-queries")
    @Produces(MediaType.TEXT_HTML)
    public String drilldownActiveQueries(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        var activities = postgresService.getCurrentActivity(instance);

        // Filter to active queries and sort by query start time (oldest first = longest running)
        var activeQueries = activities.stream()
                .filter(a -> "active".equals(a.getState()))
                .filter(a -> a.getQueryStart() != null)
                .sorted((a, b) -> a.getQueryStart().compareTo(b.getQueryStart()))
                .limit(5)
                .toList();

        StringBuilder html = new StringBuilder();
        html.append("<div class=\"drilldown-header\"><i class=\"bi bi-lightning\"></i> Longest Running Queries</div>");
        html.append("<div class=\"drilldown-body\">");

        if (activeQueries.isEmpty()) {
            html.append("<div class=\"drilldown-item\"><span class=\"text-muted\">No active queries</span></div>");
        } else {
            for (var query : activeQueries) {
                long durationSeconds = java.time.Duration.between(query.getQueryStart(), java.time.LocalDateTime.now()).getSeconds();
                String duration = formatDuration(durationSeconds);
                String queryText = query.getQuery() != null ?
                    (query.getQuery().length() > 40 ? query.getQuery().substring(0, 40) + "..." : query.getQuery()) : "N/A";
                String valueClass = durationSeconds > 60 ? "drilldown-value-danger" :
                                   (durationSeconds > 10 ? "drilldown-value-warning" : "");

                html.append("<div class=\"drilldown-item\">");
                html.append("<span class=\"drilldown-item-label\" title=\"").append(escapeHtml(query.getQuery())).append("\"><code>").append(escapeHtml(queryText)).append("</code></span>");
                html.append("<span class=\"drilldown-item-value ").append(valueClass).append("\">").append(duration).append("</span>");
                html.append("</div>");
            }
        }

        html.append("</div>");
        html.append("<div class=\"drilldown-explanation\"><i class=\"bi bi-info-circle\"></i> ");
        html.append("Currently executing queries sorted by runtime. Long-running queries may need optimisation.");
        html.append("</div>");

        return html.toString();
    }

    /**
     * Formats a duration in seconds to a human-readable string.
     *
     * @param seconds the duration in seconds
     * @return formatted duration string (e.g., "5s", "2m 30s", "1h 5m")
     */
    private String formatDuration(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            long mins = seconds / 60;
            long secs = seconds % 60;
            return secs > 0 ? mins + "m " + secs + "s" : mins + "m";
        } else {
            long hours = seconds / 3600;
            long mins = (seconds % 3600) / 60;
            return mins > 0 ? hours + "h " + mins + "m" : hours + "h";
        }
    }

    /**
     * Returns a drill-down fragment showing current blocking relationships.
     * <p>
     * Displays blocked queries with their blockers, showing the blocking tree
     * structure and lock wait durations.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return HTML fragment for the blocked queries drill-down tooltip
     */
    @GET
    @Path("/fragments/drilldown/blocked-queries")
    @Produces(MediaType.TEXT_HTML)
    public String drilldownBlockedQueries(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        var blockingTree = postgresService.getBlockingTree(instance);

        StringBuilder html = new StringBuilder();
        html.append("<div class=\"drilldown-header\"><i class=\"bi bi-lock\"></i> Blocking Relationships</div>");
        html.append("<div class=\"drilldown-body\">");

        if (blockingTree.isEmpty()) {
            html.append("<div class=\"drilldown-item\"><span class=\"text-success\"><i class=\"bi bi-check-circle\"></i> No blocking detected</span></div>");
        } else {
            for (var block : blockingTree.stream().limit(5).toList()) {
                html.append("<div class=\"drilldown-item\">");
                html.append("<span class=\"drilldown-item-label\">");
                html.append("<span class=\"text-danger\">PID ").append(block.getBlockedPid()).append("</span>");
                html.append(" <i class=\"bi bi-arrow-left\"></i> ");
                html.append("<span class=\"text-warning\">PID ").append(block.getBlockerPid()).append("</span>");
                html.append("</span>");
                html.append("<span class=\"drilldown-item-value drilldown-value-danger\">").append(block.getBlockedDuration() != null ? block.getBlockedDuration() : "N/A").append("</span>");
                html.append("</div>");
            }
        }

        html.append("</div>");
        html.append("<div class=\"drilldown-explanation\"><i class=\"bi bi-info-circle\"></i> ");
        html.append("Shows which processes are blocked waiting for locks held by other processes.");
        html.append("</div>");

        return html.toString();
    }

    /**
     * Returns a drill-down fragment explaining cache hit ratio with trend.
     * <p>
     * Shows the current cache hit ratio, a mini sparkline trend, and an
     * explanation of what the metric means and target values.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return HTML fragment for the cache hit ratio drill-down tooltip
     */
    @GET
    @Path("/fragments/drilldown/cache-hit")
    @Produces(MediaType.TEXT_HTML)
    public String drilldownCacheHit(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        var stats = postgresService.getOverviewStats(instance);
        String sparkline = sparklineService.getCacheHitRatioSparkline(instance, 1, 180, 30);

        StringBuilder html = new StringBuilder();
        html.append("<div class=\"drilldown-header\"><i class=\"bi bi-speedometer2\"></i> Cache Hit Ratio Details</div>");

        if (!sparkline.isEmpty()) {
            html.append("<div class=\"drilldown-sparkline\">").append(sparkline).append("</div>");
        }

        html.append("<div class=\"drilldown-body\">");

        // Current value
        String valueClass = stats.getCacheHitRatio() < 90 ? "drilldown-value-warning" :
                           (stats.getCacheHitRatio() >= 99 ? "drilldown-value-success" : "");
        html.append("<div class=\"drilldown-item\">");
        html.append("<span class=\"drilldown-item-label\">Current Ratio</span>");
        html.append("<span class=\"drilldown-item-value ").append(valueClass).append("\">").append(stats.getCacheHitRatioFormatted()).append("</span>");
        html.append("</div>");

        // Target
        html.append("<div class=\"drilldown-item\">");
        html.append("<span class=\"drilldown-item-label\">Target</span>");
        html.append("<span class=\"drilldown-item-value drilldown-value-success\">&gt;99%</span>");
        html.append("</div>");

        // Status
        String status = stats.getCacheHitRatio() >= 99 ? "Excellent" :
                       (stats.getCacheHitRatio() >= 95 ? "Good" :
                       (stats.getCacheHitRatio() >= 90 ? "Acceptable" : "Needs Attention"));
        String statusClass = stats.getCacheHitRatio() >= 95 ? "drilldown-value-success" :
                            (stats.getCacheHitRatio() >= 90 ? "drilldown-value-warning" : "drilldown-value-danger");
        html.append("<div class=\"drilldown-item\">");
        html.append("<span class=\"drilldown-item-label\">Status</span>");
        html.append("<span class=\"drilldown-item-value ").append(statusClass).append("\">").append(status).append("</span>");
        html.append("</div>");

        html.append("</div>");
        html.append("<div class=\"drilldown-explanation\"><i class=\"bi bi-info-circle\"></i> ");
        html.append("Percentage of data reads served from shared buffers vs disk. Low ratios indicate insufficient memory or buffer pool sizing.");
        html.append("</div>");

        return html.toString();
    }

    /**
     * Returns a drill-down fragment showing table operation breakdown.
     * <p>
     * Displays insert, update, delete, and scan statistics for a specific table,
     * helping identify workload patterns and optimisation opportunities.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @param tableName the fully-qualified table name (schema.table)
     * @return HTML fragment for the table operations drill-down tooltip
     */
    @GET
    @Path("/fragments/drilldown/table")
    @Produces(MediaType.TEXT_HTML)
    public String drilldownTable(
            @QueryParam("instance") @DefaultValue("default") String instance,
            @QueryParam("table") String tableName) {
        var tables = postgresService.getTableStats(instance);

        // Find the specific table by constructing full name from schema.table
        var tableOpt = tables.stream()
                .filter(t -> (t.getSchemaName() + "." + t.getTableName()).equals(tableName))
                .findFirst();

        StringBuilder html = new StringBuilder();
        html.append("<div class=\"drilldown-header\"><i class=\"bi bi-table\"></i> Table Operations</div>");
        html.append("<div class=\"drilldown-body\">");

        if (tableOpt.isEmpty()) {
            // Show basic info even if not found
            html.append("<div class=\"drilldown-item\">");
            html.append("<span class=\"drilldown-item-label\">Table</span>");
            html.append("<span class=\"drilldown-item-value\"><code>").append(escapeHtml(tableName)).append("</code></span>");
            html.append("</div>");
        } else {
            var table = tableOpt.get();

            // Live tuples
            html.append("<div class=\"drilldown-item\">");
            html.append("<span class=\"drilldown-item-label\">Live Rows</span>");
            html.append("<span class=\"drilldown-item-value\">").append(formatNumber(table.getnLiveTup())).append("</span>");
            html.append("</div>");

            // Dead tuples
            String deadClass = table.getnDeadTup() > 10000 ? "drilldown-value-warning" : "";
            html.append("<div class=\"drilldown-item\">");
            html.append("<span class=\"drilldown-item-label\">Dead Rows</span>");
            html.append("<span class=\"drilldown-item-value ").append(deadClass).append("\">").append(formatNumber(table.getnDeadTup())).append("</span>");
            html.append("</div>");

            // Bloat ratio
            String bloatClass = table.getBloatRatio() > 20 ? "drilldown-value-danger" : (table.getBloatRatio() > 10 ? "drilldown-value-warning" : "");
            html.append("<div class=\"drilldown-item\">");
            html.append("<span class=\"drilldown-item-label\">Bloat</span>");
            html.append("<span class=\"drilldown-item-value ").append(bloatClass).append("\">").append(String.format("%.1f%%", table.getBloatRatio())).append("</span>");
            html.append("</div>");

            // Seq scans vs Index scans
            long totalScans = table.getSeqScan() + table.getIdxScan();
            String scanRatio = totalScans > 0 ?
                String.format("%.0f%% idx", (table.getIdxScan() * 100.0 / totalScans)) : "N/A";
            String scanClass = totalScans > 0 && (table.getIdxScan() * 100.0 / totalScans) < 50 ? "drilldown-value-warning" : "drilldown-value-success";
            html.append("<div class=\"drilldown-item\">");
            html.append("<span class=\"drilldown-item-label\">Scan Ratio</span>");
            html.append("<span class=\"drilldown-item-value ").append(scanClass).append("\">").append(scanRatio).append("</span>");
            html.append("</div>");

            // Sequential scans count
            html.append("<div class=\"drilldown-item\">");
            html.append("<span class=\"drilldown-item-label\">Seq Scans</span>");
            html.append("<span class=\"drilldown-item-value\">").append(formatNumber(table.getSeqScan())).append("</span>");
            html.append("</div>");
        }

        html.append("</div>");
        html.append("<div class=\"drilldown-explanation\"><i class=\"bi bi-info-circle\"></i> ");
        html.append("Table statistics including row counts and scan patterns. High bloat suggests vacuum needed.");
        html.append("</div>");

        return html.toString();
    }

    /**
     * Returns a drill-down fragment showing index details.
     * <p>
     * Displays size and table information for a specific index.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @param indexName the index name
     * @param tableName the table name the index belongs to
     * @return HTML fragment for the index details drill-down tooltip
     */
    @GET
    @Path("/fragments/drilldown/index")
    @Produces(MediaType.TEXT_HTML)
    public String drilldownIndex(
            @QueryParam("instance") @DefaultValue("default") String instance,
            @QueryParam("index") String indexName,
            @QueryParam("table") String tableName) {
        var stats = postgresService.getOverviewStats(instance);

        // Find the specific index from top indexes
        var indexOpt = stats.getTopIndexesBySize().stream()
                .filter(i -> i.getIndexName().equals(indexName))
                .findFirst();

        StringBuilder html = new StringBuilder();
        html.append("<div class=\"drilldown-header\"><i class=\"bi bi-diagram-3\"></i> Index Details</div>");
        html.append("<div class=\"drilldown-body\">");

        if (indexOpt.isEmpty()) {
            html.append("<div class=\"drilldown-item\">");
            html.append("<span class=\"drilldown-item-label\">Index</span>");
            html.append("<span class=\"drilldown-item-value\"><code>").append(escapeHtml(indexName)).append("</code></span>");
            html.append("</div>");
            html.append("<div class=\"drilldown-item\">");
            html.append("<span class=\"drilldown-item-label\">Table</span>");
            html.append("<span class=\"drilldown-item-value\"><code>").append(escapeHtml(tableName)).append("</code></span>");
            html.append("</div>");
        } else {
            var idx = indexOpt.get();

            html.append("<div class=\"drilldown-item\">");
            html.append("<span class=\"drilldown-item-label\">Index</span>");
            html.append("<span class=\"drilldown-item-value\"><code>").append(escapeHtml(idx.getIndexName())).append("</code></span>");
            html.append("</div>");

            html.append("<div class=\"drilldown-item\">");
            html.append("<span class=\"drilldown-item-label\">Table</span>");
            html.append("<span class=\"drilldown-item-value\"><code>").append(escapeHtml(idx.getTableName())).append("</code></span>");
            html.append("</div>");

            html.append("<div class=\"drilldown-item\">");
            html.append("<span class=\"drilldown-item-label\">Size</span>");
            html.append("<span class=\"drilldown-item-value\">").append(idx.getSize()).append("</span>");
            html.append("</div>");

            // Size in bytes for more detail
            html.append("<div class=\"drilldown-item\">");
            html.append("<span class=\"drilldown-item-label\">Size (bytes)</span>");
            html.append("<span class=\"drilldown-item-value\">").append(formatNumber(idx.getSizeBytes())).append("</span>");
            html.append("</div>");
        }

        html.append("</div>");
        html.append("<div class=\"drilldown-explanation\"><i class=\"bi bi-info-circle\"></i> ");
        html.append("Index size information. Large indexes may benefit from REINDEX to reclaim space.");
        html.append("</div>");

        return html.toString();
    }

    /**
     * Returns a drill-down fragment showing the longest running query details.
     * <p>
     * Displays information about the single longest running query including
     * PID, user, duration, and truncated query text.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return HTML fragment for the longest query drill-down tooltip
     */
    @GET
    @Path("/fragments/drilldown/longest-query")
    @Produces(MediaType.TEXT_HTML)
    public String drilldownLongestQuery(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        var activities = postgresService.getCurrentActivity(instance);

        // Find the longest running active query (oldest query start = longest running)
        var longestOpt = activities.stream()
                .filter(a -> "active".equals(a.getState()))
                .filter(a -> a.getQueryStart() != null)
                .min((a, b) -> a.getQueryStart().compareTo(b.getQueryStart()));

        StringBuilder html = new StringBuilder();
        html.append("<div class=\"drilldown-header\"><i class=\"bi bi-hourglass-split\"></i> Longest Running Query</div>");
        html.append("<div class=\"drilldown-body\">");

        if (longestOpt.isEmpty()) {
            html.append("<div class=\"drilldown-item\"><span class=\"text-muted\">No active queries</span></div>");
        } else {
            var longest = longestOpt.get();

            html.append("<div class=\"drilldown-item\">");
            html.append("<span class=\"drilldown-item-label\">PID</span>");
            html.append("<span class=\"drilldown-item-value\">").append(longest.getPid()).append("</span>");
            html.append("</div>");

            html.append("<div class=\"drilldown-item\">");
            html.append("<span class=\"drilldown-item-label\">User</span>");
            html.append("<span class=\"drilldown-item-value\">").append(escapeHtml(longest.getUser())).append("</span>");
            html.append("</div>");

            html.append("<div class=\"drilldown-item\">");
            html.append("<span class=\"drilldown-item-label\">Application</span>");
            html.append("<span class=\"drilldown-item-value\">").append(escapeHtml(longest.getApplicationName() != null ? longest.getApplicationName() : "N/A")).append("</span>");
            html.append("</div>");

            long durationSeconds = longest.getQueryStart() != null ?
                java.time.Duration.between(longest.getQueryStart(), java.time.LocalDateTime.now()).getSeconds() : 0;
            String durationClass = durationSeconds > 60 ? "drilldown-value-danger" :
                                  (durationSeconds > 10 ? "drilldown-value-warning" : "");
            html.append("<div class=\"drilldown-item\">");
            html.append("<span class=\"drilldown-item-label\">Duration</span>");
            html.append("<span class=\"drilldown-item-value ").append(durationClass).append("\">").append(formatDuration(durationSeconds)).append("</span>");
            html.append("</div>");

            if (longest.getQuery() != null) {
                String truncatedQuery = longest.getQuery().length() > 100 ?
                    longest.getQuery().substring(0, 100) + "..." : longest.getQuery();
                html.append("<div class=\"drilldown-item\" style=\"flex-direction: column; align-items: flex-start;\">");
                html.append("<span class=\"drilldown-item-label\">Query</span>");
                html.append("<code style=\"font-size: 0.75rem; word-break: break-all;\">").append(escapeHtml(truncatedQuery)).append("</code>");
                html.append("</div>");
            }
        }

        html.append("</div>");
        html.append("<div class=\"drilldown-explanation\"><i class=\"bi bi-info-circle\"></i> ");
        html.append("The currently longest-running query. Long queries may indicate missing indexes or inefficient SQL.");
        html.append("</div>");

        return html.toString();
    }

    /**
     * Returns a drill-down fragment showing database size breakdown.
     * <p>
     * Displays the top 5 databases by size with their individual sizes
     * and percentage of total storage.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return HTML fragment for the database size drill-down tooltip
     */
    @GET
    @Path("/fragments/drilldown/database-size")
    @Produces(MediaType.TEXT_HTML)
    public String drilldownDatabaseSize(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        var dbMetrics = postgresService.getAllDatabaseMetrics(instance);

        // Sort by size descending and take top 5
        var topDatabases = dbMetrics.stream()
                .sorted((a, b) -> Long.compare(b.getDatabaseSizeBytes(), a.getDatabaseSizeBytes()))
                .limit(5)
                .toList();

        long totalSize = dbMetrics.stream().mapToLong(DatabaseMetrics::getDatabaseSizeBytes).sum();

        StringBuilder html = new StringBuilder();
        html.append("<div class=\"drilldown-header\"><i class=\"bi bi-hdd\"></i> Database Size Breakdown</div>");
        html.append("<div class=\"drilldown-body\">");

        if (topDatabases.isEmpty()) {
            html.append("<div class=\"drilldown-item\"><span class=\"text-muted\">No databases found</span></div>");
        } else {
            for (var db : topDatabases) {
                double percentage = totalSize > 0 ? (db.getDatabaseSizeBytes() * 100.0 / totalSize) : 0;
                html.append("<div class=\"drilldown-item\">");
                html.append("<span class=\"drilldown-item-label\"><code>").append(escapeHtml(db.getDatname())).append("</code></span>");
                html.append("<span class=\"drilldown-item-value\">").append(db.getDatabaseSize()).append(" <small class=\"text-muted\">(").append(String.format("%.1f%%", percentage)).append(")</small></span>");
                html.append("</div>");
            }
        }

        html.append("</div>");
        html.append("<div class=\"drilldown-explanation\"><i class=\"bi bi-info-circle\"></i> ");
        html.append("Storage usage by database. Monitor growth trends to plan capacity.");
        html.append("</div>");

        return html.toString();
    }

    /**
     * Formats a number with thousand separators for display.
     *
     * @param number the number to format
     * @return formatted string with thousand separators
     */
    private String formatNumber(long number) {
        return String.format("%,d", number);
    }

    /**
     * Renders the about page showing database version and application information.
     * <p>
     * Displays PostgreSQL version, uptime, and pg-console application details.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return template instance containing database and application information
     */
    @GET
    @Path("/about")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance about(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        // About page is always enabled - no feature toggle check
        DatabaseInfo dbInfo = postgresService.getDatabaseInfo(instance);
        return about.data("dbInfo", dbInfo)
                    .data("appName", appName)
                    .data("appVersion", appVersion)
                    .data("instances", dataSourceManager.getInstanceInfoList())
                    .data("currentInstance", instance)
                    .data("securityEnabled", config.security().enabled())
                    .data("schemaEnabled", config.schema().enabled())
                    .data("inMemoryMinutes", config.schema().inMemoryMinutes())
                    .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Renders the locks page showing lock information and blocking tree.
     * <p>
     * Displays current locks from pg_locks and a hierarchical blocking tree
     * showing which processes are blocking others.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return template instance containing lock and blocking tree data
     */
    @GET
    @Path("/locks")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance locks(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("locks");
        List<BlockingTree> blockingTree = postgresService.getBlockingTree(instance);
        List<LockInfo> lockInfos = postgresService.getLockInfo(instance);
        return locks.data("blockingTree", blockingTree)
                    .data("locks", lockInfos)
                    .data("instances", dataSourceManager.getInstanceInfoList())
                    .data("currentInstance", instance)
                    .data("securityEnabled", config.security().enabled())
                    .data("schemaEnabled", config.schema().enabled())
                    .data("inMemoryMinutes", config.schema().inMemoryMinutes())
                    .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Renders the wait events page showing what database processes are waiting on.
     * <p>
     * Displays wait event summaries grouped by type and individual wait events
     * from pg_stat_activity, helping identify performance bottlenecks.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return template instance containing wait event summaries
     */
    @GET
    @Path("/wait-events")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance waitEvents(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("wait-events");
        List<WaitEventSummary> typeSummaries = postgresService.getWaitEventTypeSummary(instance);
        List<WaitEventSummary> waitEventList = postgresService.getWaitEventSummary(instance);
        return waitEvents.data("typeSummaries", typeSummaries)
                        .data("waitEvents", waitEventList)
                        .data("instances", dataSourceManager.getInstanceInfoList())
                        .data("currentInstance", instance)
                        .data("securityEnabled", config.security().enabled())
                        .data("schemaEnabled", config.schema().enabled())
                        .data("inMemoryMinutes", config.schema().inMemoryMinutes())
                        .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Renders the index advisor page with index recommendations.
     * <p>
     * Analyses query patterns and table scans to suggest missing indexes
     * that could improve query performance.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return template instance containing index recommendations and summary
     */
    @GET
    @Path("/index-advisor")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance indexAdvisor(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("index-advisor");
        var recommendations = indexAdvisorService.getRecommendations(instance);
        var summary = indexAdvisorService.getSummary(instance);
        return indexAdvisor.data("recommendations", recommendations)
                          .data("summary", summary)
                          .data("instances", dataSourceManager.getInstanceInfoList())
                          .data("currentInstance", instance)
                          .data("securityEnabled", config.security().enabled())
                          .data("schemaEnabled", config.schema().enabled())
                          .data("inMemoryMinutes", config.schema().inMemoryMinutes())
                          .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Renders the query regressions page showing queries with performance changes.
     * <p>
     * Compares query performance over time to detect regressions (slower) and
     * improvements (faster). Uses historical data to identify significant changes.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @param windowHours time window in hours for comparison (defaults to 24)
     * @param thresholdPercent percentage change threshold for detection (defaults to 50)
     * @return template instance containing regression and improvement data
     */
    @GET
    @Path("/query-regressions")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance queryRegressions(
            @QueryParam("instance") @DefaultValue("default") String instance,
            @QueryParam("window") @DefaultValue("24") int windowHours,
            @QueryParam("threshold") @DefaultValue("50") int thresholdPercent) {
        featureToggleService.requirePageEnabled("query-regressions");
        var regressions = queryRegressionService.detectRegressions(instance, windowHours, thresholdPercent);
        var improvements = queryRegressionService.detectImprovements(instance, windowHours, thresholdPercent);
        var summary = queryRegressionService.getSummary(instance, windowHours, thresholdPercent);
        return queryRegressions.data("regressions", regressions)
                               .data("improvements", improvements)
                               .data("summary", summary)
                               .data("windowHours", windowHours)
                               .data("thresholdPercent", thresholdPercent)
                               .data("instances", dataSourceManager.getInstanceInfoList())
                               .data("currentInstance", instance)
                               .data("securityEnabled", config.security().enabled())
                               .data("schemaEnabled", config.schema().enabled())
                               .data("inMemoryMinutes", config.schema().inMemoryMinutes())
                               .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Renders the table maintenance page with vacuum and analyse recommendations.
     * <p>
     * Identifies tables that need vacuuming or analysing based on bloat,
     * dead tuple counts, and last vacuum/analyse timestamps.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return template instance containing maintenance recommendations and summary
     */
    @GET
    @Path("/table-maintenance")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance tableMaintenance(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("table-maintenance");
        var recommendations = tableMaintenanceService.getRecommendations(instance);
        var summary = tableMaintenanceService.getSummary(instance);
        return tableMaintenance.data("recommendations", recommendations)
                               .data("summary", summary)
                               .data("instances", dataSourceManager.getInstanceInfoList())
                               .data("currentInstance", instance)
                               .data("securityEnabled", config.security().enabled())
                               .data("schemaEnabled", config.schema().enabled())
                               .data("inMemoryMinutes", config.schema().inMemoryMinutes())
                               .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Renders the statements management page showing query performance changes.
     * <p>
     * Displays "top movers" - queries whose performance characteristics have
     * changed significantly, either improving or degrading.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @param windowHours time window in hours for comparison (defaults to 24)
     * @return template instance containing top movers and summary statistics
     */
    @GET
    @Path("/statements-management")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance statementsManagement(
            @QueryParam("instance") @DefaultValue("default") String instance,
            @QueryParam("window") @DefaultValue("24") int windowHours) {
        featureToggleService.requirePageEnabled("statements-management");
        var topMovers = statementsManagementService.getTopMovers(instance, windowHours);
        var summary = statementsManagementService.getSummary(instance);
        return statementsManagement.data("topMovers", topMovers)
                                   .data("summary", summary)
                                   .data("windowHours", windowHours)
                                   .data("instances", dataSourceManager.getInstanceInfoList())
                                   .data("currentInstance", instance)
                                   .data("securityEnabled", config.security().enabled())
                                   .data("schemaEnabled", config.schema().enabled())
                                   .data("inMemoryMinutes", config.schema().inMemoryMinutes())
                                   .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Renders the replication page showing streaming replication status.
     * <p>
     * Displays connected replicas, replication slots, WAL statistics,
     * and indicates whether this instance is itself a replica.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return template instance containing replication status and statistics
     */
    @GET
    @Path("/replication")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance replication(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("replication");
        var replicas = replicationService.getStreamingReplication(instance);
        var slots = replicationService.getReplicationSlots(instance);
        var walStats = replicationService.getWalStats(instance);
        var summary = replicationService.getSummary(instance);
        var isReplica = replicationService.isReplica(instance);
        return replication.data("replicas", replicas)
                          .data("slots", slots)
                          .data("walStats", walStats)
                          .data("summary", summary)
                          .data("isReplica", isReplica)
                          .data("instances", dataSourceManager.getInstanceInfoList())
                          .data("currentInstance", instance)
                          .data("securityEnabled", config.security().enabled())
                          .data("schemaEnabled", config.schema().enabled())
                          .data("inMemoryMinutes", config.schema().inMemoryMinutes())
                          .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Renders the infrastructure page showing background processes and storage.
     * <p>
     * Displays vacuum progress, background writer statistics, checkpointer stats,
     * and storage usage information.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return template instance containing infrastructure metrics
     */
    @GET
    @Path("/infrastructure")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance infrastructure(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("infrastructure");
        var vacuumProgress = infrastructureService.getVacuumProgress(instance);
        var bgProcessStats = infrastructureService.getBackgroundProcessStats(instance);
        var storageStats = infrastructureService.getStorageStats(instance);
        return infrastructure.data("vacuumProgress", vacuumProgress)
                             .data("bgProcessStats", bgProcessStats)
                             .data("storageStats", storageStats)
                             .data("instances", dataSourceManager.getInstanceInfoList())
                             .data("currentInstance", instance)
                             .data("securityEnabled", config.security().enabled())
                             .data("schemaEnabled", config.schema().enabled())
                             .data("inMemoryMinutes", config.schema().inMemoryMinutes())
                             .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Renders the configuration health dashboard showing PostgreSQL settings
     * with status assessments and recommendations.
     * <p>
     * Evaluates critical configuration parameters such as memory settings,
     * connection limits, autovacuum settings, and monitoring configuration.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return template instance containing configuration settings with health status
     */
    @GET
    @Path("/config-health")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance configHealthPage(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        var settings = postgresService.getConfigurationHealth(instance);

        // Group by category for display
        var groupedSettings = settings.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                com.bovinemagnet.pgconsole.model.ConfigSetting::getCategory));

        // Count issues by status
        long criticalCount = settings.stream()
            .filter(s -> s.getStatus() == com.bovinemagnet.pgconsole.model.ConfigSetting.Status.CRITICAL).count();
        long warningCount = settings.stream()
            .filter(s -> s.getStatus() == com.bovinemagnet.pgconsole.model.ConfigSetting.Status.WARNING).count();
        long infoCount = settings.stream()
            .filter(s -> s.getStatus() == com.bovinemagnet.pgconsole.model.ConfigSetting.Status.INFO).count();

        return configHealth.data("settings", settings)
                           .data("groupedSettings", groupedSettings)
                           .data("criticalCount", criticalCount)
                           .data("warningCount", warningCount)
                           .data("infoCount", infoCount)
                           .data("totalSettings", settings.size())
                           .data("instances", dataSourceManager.getInstanceInfoList())
                           .data("currentInstance", instance)
                           .data("securityEnabled", config.security().enabled())
                           .data("schemaEnabled", config.schema().enabled())
                           .data("inMemoryMinutes", config.schema().inMemoryMinutes())
                           .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Renders the checkpoints and WAL dashboard showing PostgreSQL checkpoint
     * and background writer metrics with interpretation and recommendations.
     * <p>
     * Provides insights into checkpoint frequency, forced checkpoint ratios,
     * buffer write distribution, and potential I/O bottlenecks.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return template instance containing checkpoint and bgwriter statistics
     */
    @GET
    @Path("/checkpoints")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance checkpointsPage(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        var stats = infrastructureService.getBackgroundProcessStats(instance);

        return checkpoints.data("stats", stats)
                          .data("instances", dataSourceManager.getInstanceInfoList())
                          .data("currentInstance", instance)
                          .data("securityEnabled", config.security().enabled())
                          .data("schemaEnabled", config.schema().enabled())
                          .data("inMemoryMinutes", config.schema().inMemoryMinutes())
                          .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Renders the consolidated health check dashboard showing overall
     * PostgreSQL health status with traffic light indicators.
     * <p>
     * Aggregates health status from connections, performance, maintenance,
     * replication, and configuration into a quick overview.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return template instance containing health check results grouped by category
     */
    @GET
    @Path("/health-check")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance healthCheckPage(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        var checks = healthCheckService.performHealthChecks(instance);
        var groupedChecks = healthCheckService.groupByCategory(checks);
        var overallStatus = healthCheckService.getOverallStatus(checks);
        var statusCounts = healthCheckService.countByStatus(checks);

        // Convert enum-keyed map to individual values for Qute template access
        var okCount = statusCounts.getOrDefault(com.bovinemagnet.pgconsole.model.HealthCheck.Status.OK, 0L);
        var warningCount = statusCounts.getOrDefault(com.bovinemagnet.pgconsole.model.HealthCheck.Status.WARNING, 0L);
        var criticalCount = statusCounts.getOrDefault(com.bovinemagnet.pgconsole.model.HealthCheck.Status.CRITICAL, 0L);

        return healthCheck.data("checks", checks)
                          .data("groupedChecks", groupedChecks)
                          .data("overallStatus", overallStatus)
                          .data("okCount", okCount)
                          .data("warningCount", warningCount)
                          .data("criticalCount", criticalCount)
                          .data("instances", dataSourceManager.getInstanceInfoList())
                          .data("currentInstance", instance)
                          .data("securityEnabled", config.security().enabled())
                          .data("schemaEnabled", config.schema().enabled())
                          .data("inMemoryMinutes", config.schema().inMemoryMinutes())
                          .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Renders the audit log page showing recent administrative actions.
     * <p>
     * Displays a log of user actions such as query cancellations, connection
     * terminations, and other administrative operations.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return template instance containing recent audit logs and summary
     */
    @GET
    @Path("/audit-log")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance auditLog(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("audit-log");
        var logs = auditService.getRecentLogs(100);
        var summary = auditService.getSummary();
        return auditLog.data("logs", logs)
                       .data("summary", summary)
                       .data("instances", dataSourceManager.getInstanceInfoList())
                       .data("currentInstance", instance)
                       .data("securityEnabled", config.security().enabled())
                       .data("schemaEnabled", config.schema().enabled())
                       .data("inMemoryMinutes", config.schema().inMemoryMinutes())
                       .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Renders the bookmarks page showing saved queries and notes.
     * <p>
     * Displays user-saved bookmarks with optional tag filtering.
     * Bookmarks can include queries, notes, and metadata.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @param tag optional tag filter to show only bookmarks with this tag
     * @return template instance containing bookmarks, summary, and available tags
     */
    @GET
    @Path("/bookmarks")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance bookmarks(
            @QueryParam("instance") @DefaultValue("default") String instance,
            @QueryParam("tag") String tag) {
        featureToggleService.requirePageEnabled("bookmarks");
        var allBookmarks = bookmarkService.getBookmarks(instance, null, tag);
        var summary = bookmarkService.getSummary(instance);
        var tags = bookmarkService.getAllTags(instance);
        return bookmarks.data("bookmarks", allBookmarks)
                        .data("summary", summary)
                        .data("tags", tags)
                        .data("instances", dataSourceManager.getInstanceInfoList())
                        .data("currentInstance", instance)
                        .data("securityEnabled", config.security().enabled())
                        .data("schemaEnabled", config.schema().enabled())
                        .data("inMemoryMinutes", config.schema().inMemoryMinutes())
                        .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Renders the comparison page showing metrics across multiple instances.
     * <p>
     * Provides a side-by-side comparison of key metrics across all configured
     * PostgreSQL instances, including common queries running on multiple instances.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return template instance containing comparison data and common queries
     */
    @GET
    @Path("/comparison")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance comparison(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("comparison");
        var comparisons = comparisonService.compareOverview();
        // Get all instance IDs for finding common queries
        var instanceIds = dataSourceManager.getInstanceInfoList().stream()
                .map(info -> info.getName())
                .toList();
        var commonQueries = comparisonService.findCommonQueries(instanceIds, 20);
        return comparison.data("comparisons", comparisons)
                         .data("commonQueries", commonQueries)
                         .data("instances", dataSourceManager.getInstanceInfoList())
                         .data("currentInstance", instance)
                         .data("securityEnabled", config.security().enabled())
                         .data("schemaEnabled", config.schema().enabled())
                         .data("inMemoryMinutes", config.schema().inMemoryMinutes())
                         .data("toggles", featureToggleService.getAllToggles());
    }

    // --- Phase 8: Change Data Control & Schema Management ---

    /**
     * Renders the logical replication page showing publications and subscriptions.
     * <p>
     * Displays logical replication configuration including publications,
     * subscriptions, replication origins, and subscription statistics.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return template instance containing logical replication data and summary
     */
    @GET
    @Path("/logical-replication")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance logicalReplication(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("logical-replication");
        var publications = logicalReplicationService.getPublications(instance);
        var subscriptions = logicalReplicationService.getSubscriptions(instance);
        var origins = logicalReplicationService.getReplicationOrigins(instance);
        var subscriptionStats = logicalReplicationService.getSubscriptionStats(instance);
        var summary = logicalReplicationService.getSummary(instance);
        return logicalReplication.data("publications", publications)
                                 .data("subscriptions", subscriptions)
                                 .data("origins", origins)
                                 .data("subscriptionStats", subscriptionStats)
                                 .data("summary", summary)
                                 .data("instances", dataSourceManager.getInstanceInfoList())
                                 .data("currentInstance", instance)
                                 .data("securityEnabled", config.security().enabled())
                                 .data("schemaEnabled", config.schema().enabled())
                                 .data("inMemoryMinutes", config.schema().inMemoryMinutes())
                                 .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Renders the change data capture page showing table modification activity.
     * <p>
     * Displays table change activity, high-churn tables, and WAL generation
     * estimates to help monitor data change patterns.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return template instance containing CDC activity and statistics
     */
    @GET
    @Path("/cdc")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance cdc(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("cdc");
        var activities = changeDataCaptureService.getTableChangeActivity(instance);
        var highChurnTables = changeDataCaptureService.getHighChurnTables(instance, 10);
        var walEstimates = changeDataCaptureService.getWalGenerationByTable(instance);
        var summary = changeDataCaptureService.getSummary(instance);
        return cdc.data("activities", activities)
                  .data("highChurnTables", highChurnTables)
                  .data("walEstimates", walEstimates)
                  .data("summary", summary)
                  .data("instances", dataSourceManager.getInstanceInfoList())
                  .data("currentInstance", instance)
                  .data("securityEnabled", config.security().enabled())
                  .data("schemaEnabled", config.schema().enabled())
                  .data("inMemoryMinutes", config.schema().inMemoryMinutes())
                  .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Renders the data lineage page showing schema dependencies.
     * <p>
     * Displays event triggers, foreign key relationships, view dependencies,
     * and function dependencies to help understand data flow and schema structure.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return template instance containing dependency information and summary
     */
    @GET
    @Path("/data-lineage")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance dataLineage(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("data-lineage");
        var eventTriggers = schemaChangeService.getEventTriggers(instance);
        var foreignKeys = schemaChangeService.getForeignKeyRelationships(instance);
        var viewDependencies = schemaChangeService.getViewDependencies(instance);
        var functionDependencies = schemaChangeService.getFunctionDependencies(instance);
        var summary = schemaChangeService.getSummary(instance);
        return dataLineage.data("eventTriggers", eventTriggers)
                          .data("foreignKeys", foreignKeys)
                          .data("viewDependencies", viewDependencies)
                          .data("functionDependencies", functionDependencies)
                          .data("summary", summary)
                          .data("instances", dataSourceManager.getInstanceInfoList())
                          .data("currentInstance", instance)
                          .data("securityEnabled", config.security().enabled())
                          .data("schemaEnabled", config.schema().enabled())
                          .data("inMemoryMinutes", config.schema().inMemoryMinutes())
                          .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Renders the partitions page showing table partitioning information.
     * <p>
     * Displays partitioned tables, their partitions, and orphan partitions
     * (partitions without a parent table).
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return template instance containing partition data and summary
     */
    @GET
    @Path("/partitions")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance partitions(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("partitions");
        var partitionedTables = partitioningService.getPartitionedTables(instance);
        var orphanPartitions = partitioningService.getOrphanPartitions(instance);
        var summary = partitioningService.getSummary(instance);
        return partitions.data("partitionedTables", partitionedTables)
                         .data("orphanPartitions", orphanPartitions)
                         .data("summary", summary)
                         .data("instances", dataSourceManager.getInstanceInfoList())
                         .data("currentInstance", instance)
                         .data("securityEnabled", config.security().enabled())
                         .data("schemaEnabled", config.schema().enabled())
                         .data("inMemoryMinutes", config.schema().inMemoryMinutes())
                         .data("toggles", featureToggleService.getAllToggles());
    }

    // --- Phase 10: Security & Compliance Monitoring ---

    /**
     * Renders the main security dashboard showing security overview.
     * <p>
     * Displays security score, critical warnings count, SSL coverage percentage,
     * privileged roles count, and quick links to detailed security pages.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return template instance containing security overview data
     */
    @GET
    @Path("/security")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance security(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("security");
        var auditSummary = securityAuditService.getSummary(instance);
        var connectionSummary = connectionSecurityService.getSummary(instance);
        var accessSummary = dataAccessPatternService.getSummary(instance);
        var complianceSummary = complianceService.getSummary(instance);
        var recommendationSummary = securityRecommendationService.getSummary(instance);
        var warnings = securityAuditService.getAllWarnings(instance);
        var complianceScores = complianceService.getAllComplianceScores(instance);
        return security.data("auditSummary", auditSummary)
                       .data("connectionSummary", connectionSummary)
                       .data("accessSummary", accessSummary)
                       .data("complianceSummary", complianceSummary)
                       .data("recommendationSummary", recommendationSummary)
                       .data("warnings", warnings)
                       .data("complianceScores", complianceScores)
                       .data("instances", dataSourceManager.getInstanceInfoList())
                       .data("currentInstance", instance)
                       .data("securityEnabled", config.security().enabled())
                       .data("schemaEnabled", config.schema().enabled())
                       .data("inMemoryMinutes", config.schema().inMemoryMinutes())
                       .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Renders the security roles page showing role and permission auditing.
     * <p>
     * Displays all database roles with their privileges, role hierarchy,
     * membership information, and security warnings about elevated privileges.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return template instance containing role audit data
     */
    @GET
    @Path("/security/roles")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance securityRoles(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("security-roles");
        var roles = securityAuditService.getAllRoles(instance);
        var memberships = securityAuditService.getRoleMemberships(instance);
        var warnings = securityAuditService.getAllWarnings(instance);
        var summary = securityAuditService.getSummary(instance);
        return securityRoles.data("roles", roles)
                            .data("memberships", memberships)
                            .data("warnings", warnings)
                            .data("summary", summary)
                            .data("instances", dataSourceManager.getInstanceInfoList())
                            .data("currentInstance", instance)
                            .data("securityEnabled", config.security().enabled())
                            .data("schemaEnabled", config.schema().enabled())
                            .data("inMemoryMinutes", config.schema().inMemoryMinutes())
                            .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Renders the connection security page showing SSL and authentication analysis.
     * <p>
     * Displays current SSL connections, pg_hba.conf rules, authentication methods,
     * and connection source analysis with security warnings.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return template instance containing connection security data
     */
    @GET
    @Path("/security/connections")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance securityConnections(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("security-connections");
        var sslConnections = connectionSecurityService.getSslConnections(instance);
        var hbaRules = connectionSecurityService.getHbaRules(instance);
        var warnings = connectionSecurityService.getWarnings(instance);
        var summary = connectionSecurityService.getSummary(instance);
        var sslEnabled = connectionSecurityService.isSslEnabled(instance);
        var passwordEncryption = connectionSecurityService.getPasswordEncryption(instance);
        var authMethods = connectionSecurityService.getAuthMethodBreakdown(instance);
        return securityConnections.data("sslConnections", sslConnections)
                                  .data("hbaRules", hbaRules)
                                  .data("warnings", warnings)
                                  .data("summary", summary)
                                  .data("sslEnabled", sslEnabled)
                                  .data("passwordEncryption", passwordEncryption)
                                  .data("authMethods", authMethods)
                                  .data("instances", dataSourceManager.getInstanceInfoList())
                                  .data("currentInstance", instance)
                                  .data("securityEnabled", config.security().enabled())
                                  .data("schemaEnabled", config.schema().enabled())
                                  .data("inMemoryMinutes", config.schema().inMemoryMinutes())
                                  .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Renders the data access patterns page showing PII detection and RLS policies.
     * <p>
     * Displays sensitive tables with potential PII, detected PII columns,
     * row-level security policies, and tables needing additional protection.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return template instance containing data access pattern data
     */
    @GET
    @Path("/security/access")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance securityAccess(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("security-access");
        var sensitiveTables = dataAccessPatternService.getSensitiveTables(instance);
        var piiColumns = dataAccessPatternService.getPiiColumns(instance);
        var rlsPolicies = dataAccessPatternService.getRlsPolicies(instance);
        var summary = dataAccessPatternService.getSummary(instance);
        return securityAccess.data("sensitiveTables", sensitiveTables)
                             .data("piiColumns", piiColumns)
                             .data("rlsPolicies", rlsPolicies)
                             .data("summary", summary)
                             .data("instances", dataSourceManager.getInstanceInfoList())
                             .data("currentInstance", instance)
                             .data("securityEnabled", config.security().enabled())
                             .data("schemaEnabled", config.schema().enabled())
                             .data("inMemoryMinutes", config.schema().inMemoryMinutes())
                             .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Renders the compliance dashboard showing security compliance scores.
     * <p>
     * Displays compliance scores across multiple areas including access control,
     * encryption, audit logging, data protection, and authentication.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return template instance containing compliance score data
     */
    @GET
    @Path("/security/compliance")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance securityCompliance(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("security-compliance");
        var scores = complianceService.getAllComplianceScores(instance);
        var summary = complianceService.getSummary(instance);
        return securityCompliance.data("scores", scores)
                                 .data("summary", summary)
                                 .data("instances", dataSourceManager.getInstanceInfoList())
                                 .data("currentInstance", instance)
                                 .data("securityEnabled", config.security().enabled())
                                 .data("schemaEnabled", config.schema().enabled())
                                 .data("inMemoryMinutes", config.schema().inMemoryMinutes())
                                 .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Renders the security recommendations page showing actionable improvements.
     * <p>
     * Displays security recommendations organised by priority (critical, high,
     * medium, low) with suggested actions and rationale for each recommendation.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return template instance containing security recommendations
     */
    @GET
    @Path("/security/recommendations")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance securityRecommendations(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("security-recommendations");
        var recommendations = securityRecommendationService.getAllRecommendations(instance);
        var summary = securityRecommendationService.getSummary(instance);

        // Pre-filter by priority for template display
        var criticalRecs = recommendations.stream()
                .filter(r -> r.getPriority() == com.bovinemagnet.pgconsole.model.SecurityRecommendation.Priority.CRITICAL)
                .toList();
        var highRecs = recommendations.stream()
                .filter(r -> r.getPriority() == com.bovinemagnet.pgconsole.model.SecurityRecommendation.Priority.HIGH)
                .toList();
        var mediumRecs = recommendations.stream()
                .filter(r -> r.getPriority() == com.bovinemagnet.pgconsole.model.SecurityRecommendation.Priority.MEDIUM)
                .toList();
        var lowRecs = recommendations.stream()
                .filter(r -> r.getPriority() == com.bovinemagnet.pgconsole.model.SecurityRecommendation.Priority.LOW
                        || r.getPriority() == com.bovinemagnet.pgconsole.model.SecurityRecommendation.Priority.INFORMATIONAL)
                .toList();

        return securityRecommendations.data("recommendations", recommendations)
                                      .data("criticalRecs", criticalRecs)
                                      .data("highRecs", highRecs)
                                      .data("mediumRecs", mediumRecs)
                                      .data("lowRecs", lowRecs)
                                      .data("summary", summary)
                                      .data("instances", dataSourceManager.getInstanceInfoList())
                                      .data("currentInstance", instance)
                                      .data("securityEnabled", config.security().enabled())
                                      .data("schemaEnabled", config.schema().enabled())
                                      .data("inMemoryMinutes", config.schema().inMemoryMinutes())
                                      .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Renders the query detail page for a specific query.
     * <p>
     * Displays detailed statistics and sparklines showing performance trends
     * over the last 24 hours for the specified query.
     *
     * @param queryId the query identifier from pg_stat_statements
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return template instance containing query details and sparkline data
     */
    @GET
    @Path("/slow-queries/{queryId}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance queryDetail(
            @PathParam("queryId") String queryId,
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("slow-queries");
        SlowQuery query = postgresService.getSlowQueryById(instance, queryId);

        // Generate sparklines from query history (last 24 hours)
        String meanTimeSparkline = sparklineService.getQueryMeanTimeSparkline(instance, queryId, 24, 200, 40);
        String callsSparkline = sparklineService.getQueryCallsSparkline(instance, queryId, 24, 200, 40);

        return queryDetail.data("query", query)
                          .data("meanTimeSparkline", meanTimeSparkline)
                          .data("callsSparkline", callsSparkline)
                          .data("instances", dataSourceManager.getInstanceInfoList())
                          .data("currentInstance", instance)
                          .data("securityEnabled", config.security().enabled())
                          .data("schemaEnabled", config.schema().enabled())
                          .data("inMemoryMinutes", config.schema().inMemoryMinutes())
                          .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Renders the databases page showing metrics for all databases.
     * <p>
     * Displays size, connection counts, transaction rates, and cache hit ratios
     * for all databases in the PostgreSQL instance.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return template instance containing database metrics
     */
    @GET
    @Path("/databases")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance databases(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("databases");
        List<DatabaseMetrics> dbMetrics = postgresService.getAllDatabaseMetrics(instance);
        return databases.data("databases", dbMetrics)
                       .data("instances", dataSourceManager.getInstanceInfoList())
                       .data("currentInstance", instance)
                       .data("securityEnabled", config.security().enabled())
                       .data("schemaEnabled", config.schema().enabled())
                       .data("inMemoryMinutes", config.schema().inMemoryMinutes())
                       .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Renders the database detail page for a specific database.
     * <p>
     * Displays detailed metrics and statistics for a single database
     * including size, connections, and activity metrics.
     *
     * @param dbName the name of the database
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return template instance containing database-specific metrics
     */
    @GET
    @Path("/databases/{dbName}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance databaseDetail(
            @PathParam("dbName") String dbName,
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("databases");
        DatabaseMetrics db = postgresService.getDatabaseMetrics(instance, dbName);
        return databaseDetail.data("db", db)
                            .data("instances", dataSourceManager.getInstanceInfoList())
                            .data("currentInstance", instance)
                            .data("securityEnabled", config.security().enabled())
                            .data("schemaEnabled", config.schema().enabled())
                            .data("inMemoryMinutes", config.schema().inMemoryMinutes())
                            .data("toggles", featureToggleService.getAllToggles());
    }

    // --- Admin Actions: Cancel/Terminate Queries ---

    /**
     * Cancels a running query by process ID (PID).
     * <p>
     * Calls pg_cancel_backend() to send SIGINT to the backend process.
     * Requires admin role when security is enabled (enforced via HTTP permissions).
     *
     * @param pid the process ID of the backend to cancel
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return HTTP response with success or failure badge HTML
     */
    @POST
    @Path("/api/activity/{pid}/cancel")
    @Produces(MediaType.TEXT_HTML)
    public Response cancelQuery(
            @PathParam("pid") int pid,
            @QueryParam("instance") @DefaultValue("default") String instance) {
        boolean success = postgresService.cancelQuery(instance, pid);
        if (success) {
            return Response.ok("<span class=\"badge bg-success\">Query cancelled</span>").build();
        } else {
            return Response.ok("<span class=\"badge bg-warning\">Could not cancel query</span>").build();
        }
    }

    /**
     * Terminates a backend connection by process ID (PID).
     * <p>
     * Calls pg_terminate_backend() to forcefully disconnect the backend.
     * This is more aggressive than cancellation and will close the connection.
     * Requires admin role when security is enabled (enforced via HTTP permissions).
     *
     * @param pid the process ID of the backend to terminate
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return HTTP response with success or failure badge HTML
     */
    @POST
    @Path("/api/activity/{pid}/terminate")
    @Produces(MediaType.TEXT_HTML)
    public Response terminateQuery(
            @PathParam("pid") int pid,
            @QueryParam("instance") @DefaultValue("default") String instance) {
        boolean success = postgresService.terminateQuery(instance, pid);
        if (success) {
            return Response.ok("<span class=\"badge bg-success\">Connection terminated</span>").build();
        } else {
            return Response.ok("<span class=\"badge bg-warning\">Could not terminate connection</span>").build();
        }
    }

    // --- Explain Plan ---

    /**
     * Generates an EXPLAIN plan for a query.
     * <p>
     * Executes EXPLAIN (optionally with ANALYSE and BUFFERS) on the provided query.
     * Returns an HTML fragment for htmx to insert into the page.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @param query the SQL query to explain
     * @param analyse whether to execute EXPLAIN ANALYSE (defaults to false)
     * @param buffers whether to include buffer usage in the plan (defaults to false)
     * @return HTTP response containing the EXPLAIN plan as HTML
     */
    @POST
    @Path("/api/explain")
    @Produces(MediaType.TEXT_HTML)
    public Response explainQuery(
            @QueryParam("instance") @DefaultValue("default") String instance,
            @QueryParam("query") String query,
            @QueryParam("analyse") @DefaultValue("false") boolean analyse,
            @QueryParam("buffers") @DefaultValue("false") boolean buffers) {

        if (query == null || query.trim().isEmpty()) {
            return Response.ok("<div class=\"alert alert-warning\">No query provided</div>").build();
        }

        ExplainPlan plan = postgresService.explainQuery(instance, query, analyse, buffers);

        StringBuilder html = new StringBuilder();
        html.append("<div class=\"card\">");
        html.append("<div class=\"card-header d-flex justify-content-between align-items-center\">");
        html.append("<h5 class=\"mb-0\">").append(plan.getOptionsDescription()).append(" Plan</h5>");
        html.append("<small class=\"text-muted\">Generated: ").append(plan.getGeneratedAtFormatted()).append("</small>");
        html.append("</div>");
        html.append("<div class=\"card-body\">");

        if (plan.hasError()) {
            html.append("<div class=\"alert alert-danger mb-0\">");
            html.append("<strong>Error:</strong> ").append(escapeHtml(plan.getError()));
            html.append("</div>");
        } else {
            html.append("<pre class=\"mb-0\" style=\"white-space: pre-wrap; font-size: 0.85em;\">");
            html.append(escapeHtml(plan.getPlanText()));
            html.append("</pre>");
        }

        html.append("</div></div>");

        return Response.ok(html.toString()).build();
    }

    /**
     * Escapes HTML special characters to prevent XSS and ensure correct rendering.
     * <p>
     * Replaces ampersand, less-than, greater-than, and quote characters
     * with their HTML entity equivalents.
     *
     * @param text the text to escape, may be null
     * @return escaped text, or empty string if input is null
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    // --- CSV Export ---

    /**
     * Exports slow queries as a CSV file.
     * <p>
     * Generates a CSV download containing query statistics from pg_stat_statements.
     * The filename includes the instance name and timestamp.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @param sortBy the sort field (defaults to "totalTime")
     * @param order the sort order "asc" or "desc" (defaults to "desc")
     * @return HTTP response with CSV content and download headers
     */
    @GET
    @Path("/slow-queries/export")
    @Produces("text/csv")
    public Response exportSlowQueries(
            @QueryParam("instance") @DefaultValue("default") String instance,
            @QueryParam("sortBy") String sortBy,
            @QueryParam("order") String order) {

        List<SlowQuery> queries = postgresService.getSlowQueries(
            instance,
            sortBy != null ? sortBy : "totalTime",
            order != null ? order : "desc"
        );

        StringBuilder csv = new StringBuilder();

        // Header row
        csv.append("Query ID,Query,Total Calls,Total Time (ms),Mean Time (ms),")
           .append("Min Time (ms),Max Time (ms),Rows,Shared Blks Hit,Shared Blks Read,")
           .append("Temp Blks Written\n");

        // Data rows
        for (SlowQuery q : queries) {
            csv.append(escapeCsv(q.getQueryId())).append(",")
               .append(escapeCsv(q.getQuery())).append(",")
               .append(q.getTotalCalls()).append(",")
               .append(String.format("%.2f", q.getTotalTime())).append(",")
               .append(String.format("%.2f", q.getMeanTime())).append(",")
               .append(String.format("%.2f", q.getMinTime())).append(",")
               .append(String.format("%.2f", q.getMaxTime())).append(",")
               .append(q.getRows()).append(",")
               .append(q.getSharedBlksHit()).append(",")
               .append(q.getSharedBlksRead()).append(",")
               .append(q.getTempBlksWritten())
               .append("\n");
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String filename = String.format("slow-queries-%s-%s.csv", instance, timestamp);

        return Response.ok(csv.toString())
                      .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                      .build();
    }

    // --- Incident Report ---

    /**
     * Captures and exports an incident report as a text file.
     * <p>
     * Creates a point-in-time snapshot of the database state including
     * active queries, locks, configuration, and key metrics.
     * Useful for post-incident analysis and troubleshooting.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @param description optional description of the incident
     * @return HTTP response with incident report as plain text download
     */
    @GET
    @Path("/incident-report/export")
    @Produces("text/plain")
    public Response exportIncidentReport(
            @QueryParam("instance") @DefaultValue("default") String instance,
            @QueryParam("description") String description) {

        IncidentReport report = incidentReportService.captureReport(instance, description);
        String reportText = incidentReportService.formatAsText(report);

        String filename = String.format("incident-report-%s-%s.txt",
            instance, report.getFilenameTimestamp());

        return Response.ok(reportText)
                      .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                      .build();
    }

    /**
     * Escapes a string for CSV format according to RFC 4180.
     * <p>
     * Wraps values containing commas, newlines, or quotes in double quotes.
     * Escapes existing quotes by doubling them.
     *
     * @param value the value to escape, may be null
     * @return escaped CSV value, or empty string if input is null
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        // If value contains comma, newline, or quote, wrap in quotes and escape existing quotes
        if (value.contains(",") || value.contains("\n") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
