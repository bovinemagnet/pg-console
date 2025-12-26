package com.bovinemagnet.pgconsole.resource;

import com.bovinemagnet.pgconsole.config.InstanceConfig;
import com.bovinemagnet.pgconsole.model.Activity;
import com.bovinemagnet.pgconsole.model.BlockingTree;
import com.bovinemagnet.pgconsole.model.DatabaseInfo;
import com.bovinemagnet.pgconsole.model.DatabaseMetrics;
import com.bovinemagnet.pgconsole.model.LockInfo;
import com.bovinemagnet.pgconsole.model.OverviewStats;
import com.bovinemagnet.pgconsole.model.SlowQuery;
import com.bovinemagnet.pgconsole.model.TableStats;
import com.bovinemagnet.pgconsole.service.DataSourceManager;
import com.bovinemagnet.pgconsole.service.PostgresService;
import com.bovinemagnet.pgconsole.service.SparklineService;
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
    PostgresService postgresService;

    @Inject
    SparklineService sparklineService;

    @Inject
    DataSourceManager dataSourceManager;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index(
            @QueryParam("instance") @DefaultValue("default") String instance) {
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
                    .data("securityEnabled", config.security().enabled());
    }

    @GET
    @Path("/slow-queries")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance slowQueries(
            @QueryParam("instance") @DefaultValue("default") String instance,
            @QueryParam("sortBy") String sortBy,
            @QueryParam("order") String order) {

        List<SlowQuery> queries = postgresService.getSlowQueries(
            instance,
            sortBy != null ? sortBy : "totalTime",
            order != null ? order : "desc"
        );

        return slowQueries.data("queries", queries)
                         .data("sortBy", sortBy != null ? sortBy : "totalTime")
                         .data("order", order != null ? order : "desc")
                         .data("instances", dataSourceManager.getInstanceInfoList())
                         .data("currentInstance", instance)
                         .data("securityEnabled", config.security().enabled());
    }

    @GET
    @Path("/activity")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance activity(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        List<Activity> activities = postgresService.getCurrentActivity(instance);
        return activity.data("activities", activities)
                      .data("instances", dataSourceManager.getInstanceInfoList())
                      .data("currentInstance", instance)
                      .data("securityEnabled", config.security().enabled());
    }

    @GET
    @Path("/tables")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance tables(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        List<TableStats> stats = postgresService.getTableStats(instance);
        return tables.data("tables", stats)
                    .data("instances", dataSourceManager.getInstanceInfoList())
                    .data("currentInstance", instance)
                    .data("securityEnabled", config.security().enabled());
    }

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

    @GET
    @Path("/about")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance about(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        DatabaseInfo dbInfo = postgresService.getDatabaseInfo(instance);
        return about.data("dbInfo", dbInfo)
                    .data("appName", appName)
                    .data("appVersion", appVersion)
                    .data("instances", dataSourceManager.getInstanceInfoList())
                    .data("currentInstance", instance)
                    .data("securityEnabled", config.security().enabled());
    }

    @GET
    @Path("/locks")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance locks(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        List<BlockingTree> blockingTree = postgresService.getBlockingTree(instance);
        List<LockInfo> lockInfos = postgresService.getLockInfo(instance);
        return locks.data("blockingTree", blockingTree)
                    .data("locks", lockInfos)
                    .data("instances", dataSourceManager.getInstanceInfoList())
                    .data("currentInstance", instance)
                    .data("securityEnabled", config.security().enabled());
    }

    @GET
    @Path("/slow-queries/{queryId}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance queryDetail(
            @PathParam("queryId") String queryId,
            @QueryParam("instance") @DefaultValue("default") String instance) {
        SlowQuery query = postgresService.getSlowQueryById(instance, queryId);

        // Generate sparklines from query history (last 24 hours)
        String meanTimeSparkline = sparklineService.getQueryMeanTimeSparkline(instance, queryId, 24, 200, 40);
        String callsSparkline = sparklineService.getQueryCallsSparkline(instance, queryId, 24, 200, 40);

        return queryDetail.data("query", query)
                          .data("meanTimeSparkline", meanTimeSparkline)
                          .data("callsSparkline", callsSparkline)
                          .data("instances", dataSourceManager.getInstanceInfoList())
                          .data("currentInstance", instance)
                          .data("securityEnabled", config.security().enabled());
    }

    @GET
    @Path("/databases")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance databases(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        List<DatabaseMetrics> dbMetrics = postgresService.getAllDatabaseMetrics(instance);
        return databases.data("databases", dbMetrics)
                       .data("instances", dataSourceManager.getInstanceInfoList())
                       .data("currentInstance", instance)
                       .data("securityEnabled", config.security().enabled());
    }

    @GET
    @Path("/databases/{dbName}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance databaseDetail(
            @PathParam("dbName") String dbName,
            @QueryParam("instance") @DefaultValue("default") String instance) {
        DatabaseMetrics db = postgresService.getDatabaseMetrics(instance, dbName);
        return databaseDetail.data("db", db)
                            .data("instances", dataSourceManager.getInstanceInfoList())
                            .data("currentInstance", instance)
                            .data("securityEnabled", config.security().enabled());
    }

    // --- Admin Actions: Cancel/Terminate Queries ---

    /**
     * Cancels a running query by PID.
     * Requires admin role when security is enabled (enforced via HTTP permissions).
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
     * Terminates a backend connection by PID.
     * Requires admin role when security is enabled (enforced via HTTP permissions).
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

    // --- CSV Export ---

    /**
     * Exports slow queries as a CSV file.
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

    /**
     * Escapes a string for CSV format.
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
