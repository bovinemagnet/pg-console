package com.bovinemagnet.pgconsole.resource;

import com.bovinemagnet.pgconsole.model.Activity;
import com.bovinemagnet.pgconsole.model.BlockingTree;
import com.bovinemagnet.pgconsole.model.DatabaseInfo;
import com.bovinemagnet.pgconsole.model.DatabaseMetrics;
import com.bovinemagnet.pgconsole.model.LockInfo;
import com.bovinemagnet.pgconsole.model.OverviewStats;
import com.bovinemagnet.pgconsole.model.SlowQuery;
import com.bovinemagnet.pgconsole.model.TableStats;
import com.bovinemagnet.pgconsole.service.PostgresService;
import com.bovinemagnet.pgconsole.service.SparklineService;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Arrays;
import java.util.List;

@Path("/")
public class DashboardResource {

    @ConfigProperty(name = "quarkus.application.name", defaultValue = "pg-console")
    String appName;

    @ConfigProperty(name = "quarkus.application.version", defaultValue = "1.0.0")
    String appVersion;

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

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index() {
        OverviewStats stats = postgresService.getOverviewStats();

        // Generate sparklines from history (last 1 hour)
        String connectionsSparkline = sparklineService.getConnectionsSparkline(1, 120, 30);
        String activeQueriesSparkline = sparklineService.getActiveQueriesSparkline(1, 120, 30);
        String blockedQueriesSparkline = sparklineService.getBlockedQueriesSparkline(1, 120, 30);
        String cacheHitSparkline = sparklineService.getCacheHitRatioSparkline(1, 120, 30);

        return index.data("stats", stats)
                    .data("connectionsSparkline", connectionsSparkline)
                    .data("activeQueriesSparkline", activeQueriesSparkline)
                    .data("blockedQueriesSparkline", blockedQueriesSparkline)
                    .data("cacheHitSparkline", cacheHitSparkline);
    }

    @GET
    @Path("/slow-queries")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance slowQueries(
            @QueryParam("sortBy") String sortBy,
            @QueryParam("order") String order) {
        
        List<SlowQuery> queries = postgresService.getSlowQueries(
            sortBy != null ? sortBy : "totalTime",
            order != null ? order : "desc"
        );
        
        return slowQueries.data("queries", queries)
                         .data("sortBy", sortBy != null ? sortBy : "totalTime")
                         .data("order", order != null ? order : "desc");
    }

    @GET
    @Path("/activity")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance activity() {
        List<Activity> activities = postgresService.getCurrentActivity();
        return activity.data("activities", activities);
    }

    @GET
    @Path("/tables")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance tables() {
        List<TableStats> stats = postgresService.getTableStats();
        return tables.data("tables", stats);
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
    public TemplateInstance about() {
        DatabaseInfo dbInfo = postgresService.getDatabaseInfo();
        return about.data("dbInfo", dbInfo)
                    .data("appName", appName)
                    .data("appVersion", appVersion);
    }

    @GET
    @Path("/locks")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance locks() {
        List<BlockingTree> blockingTree = postgresService.getBlockingTree();
        List<LockInfo> lockInfos = postgresService.getLockInfo();
        return locks.data("blockingTree", blockingTree)
                    .data("locks", lockInfos);
    }

    @GET
    @Path("/slow-queries/{queryId}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance queryDetail(@PathParam("queryId") String queryId) {
        SlowQuery query = postgresService.getSlowQueryById(queryId);

        // Generate sparklines from query history (last 24 hours)
        String meanTimeSparkline = sparklineService.getQueryMeanTimeSparkline(queryId, 24, 200, 40);
        String callsSparkline = sparklineService.getQueryCallsSparkline(queryId, 24, 200, 40);

        return queryDetail.data("query", query)
                          .data("meanTimeSparkline", meanTimeSparkline)
                          .data("callsSparkline", callsSparkline);
    }

    @GET
    @Path("/databases")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance databases() {
        List<DatabaseMetrics> dbMetrics = postgresService.getAllDatabaseMetrics();
        return databases.data("databases", dbMetrics);
    }

    @GET
    @Path("/databases/{dbName}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance databaseDetail(@PathParam("dbName") String dbName) {
        DatabaseMetrics db = postgresService.getDatabaseMetrics(dbName);
        return databaseDetail.data("db", db);
    }
}
