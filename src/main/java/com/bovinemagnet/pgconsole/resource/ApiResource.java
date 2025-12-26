package com.bovinemagnet.pgconsole.resource;

import com.bovinemagnet.pgconsole.config.InstanceConfig;
import com.bovinemagnet.pgconsole.model.Activity;
import com.bovinemagnet.pgconsole.model.BlockingTree;
import com.bovinemagnet.pgconsole.model.DatabaseMetrics;
import com.bovinemagnet.pgconsole.model.LockInfo;
import com.bovinemagnet.pgconsole.model.OverviewStats;
import com.bovinemagnet.pgconsole.model.SlowQuery;
import com.bovinemagnet.pgconsole.model.TableStats;
import com.bovinemagnet.pgconsole.model.WaitEventSummary;
import com.bovinemagnet.pgconsole.service.DataSourceManager;
import com.bovinemagnet.pgconsole.service.IndexAdvisorService;
import com.bovinemagnet.pgconsole.service.InfrastructureService;
import com.bovinemagnet.pgconsole.service.PostgresService;
import com.bovinemagnet.pgconsole.service.QueryRegressionService;
import com.bovinemagnet.pgconsole.service.ReplicationService;
import com.bovinemagnet.pgconsole.service.TableMaintenanceService;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API resource for programmatic access to PostgreSQL metrics.
 * Returns JSON responses for integration with external tools.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
public class ApiResource {

    @Inject
    InstanceConfig config;

    @Inject
    PostgresService postgresService;

    @Inject
    DataSourceManager dataSourceManager;

    @Inject
    IndexAdvisorService indexAdvisorService;

    @Inject
    QueryRegressionService queryRegressionService;

    @Inject
    TableMaintenanceService tableMaintenanceService;

    @Inject
    ReplicationService replicationService;

    @Inject
    InfrastructureService infrastructureService;

    /**
     * Returns overview statistics for a PostgreSQL instance.
     */
    @GET
    @Path("/overview")
    public Map<String, Object> getOverview(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        OverviewStats stats = postgresService.getOverviewStats(instance);
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("instance", instance);
        response.put("stats", stats);
        return response;
    }

    /**
     * Returns current database activity.
     */
    @GET
    @Path("/activity")
    public Map<String, Object> getActivity(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        List<Activity> activities = postgresService.getCurrentActivity(instance);
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("instance", instance);
        response.put("count", activities.size());
        response.put("activities", activities);
        return response;
    }

    /**
     * Returns slow queries from pg_stat_statements.
     */
    @GET
    @Path("/slow-queries")
    public Map<String, Object> getSlowQueries(
            @QueryParam("instance") @DefaultValue("default") String instance,
            @QueryParam("sortBy") @DefaultValue("totalTime") String sortBy,
            @QueryParam("order") @DefaultValue("desc") String order,
            @QueryParam("limit") @DefaultValue("50") int limit) {
        List<SlowQuery> queries = postgresService.getSlowQueries(instance, sortBy, order);
        if (queries.size() > limit) {
            queries = queries.subList(0, limit);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("instance", instance);
        response.put("count", queries.size());
        response.put("queries", queries);
        return response;
    }

    /**
     * Returns lock information.
     */
    @GET
    @Path("/locks")
    public Map<String, Object> getLocks(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        List<BlockingTree> blockingTree = postgresService.getBlockingTree(instance);
        List<LockInfo> locks = postgresService.getLockInfo(instance);
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("instance", instance);
        response.put("blockingTree", blockingTree);
        response.put("locks", locks);
        return response;
    }

    /**
     * Returns wait event summary.
     */
    @GET
    @Path("/wait-events")
    public Map<String, Object> getWaitEvents(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        List<WaitEventSummary> typeSummaries = postgresService.getWaitEventTypeSummary(instance);
        List<WaitEventSummary> waitEvents = postgresService.getWaitEventSummary(instance);
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("instance", instance);
        response.put("typeSummaries", typeSummaries);
        response.put("waitEvents", waitEvents);
        return response;
    }

    /**
     * Returns table statistics.
     */
    @GET
    @Path("/tables")
    public Map<String, Object> getTables(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        List<TableStats> tables = postgresService.getTableStats(instance);
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("instance", instance);
        response.put("count", tables.size());
        response.put("tables", tables);
        return response;
    }

    /**
     * Returns database metrics.
     */
    @GET
    @Path("/databases")
    public Map<String, Object> getDatabases(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        List<DatabaseMetrics> databases = postgresService.getAllDatabaseMetrics(instance);
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("instance", instance);
        response.put("count", databases.size());
        response.put("databases", databases);
        return response;
    }

    /**
     * Returns index recommendations.
     */
    @GET
    @Path("/index-advisor")
    public Map<String, Object> getIndexAdvisor(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        var recommendations = indexAdvisorService.getRecommendations(instance);
        var summary = indexAdvisorService.getSummary(instance);
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("instance", instance);
        response.put("summary", summary);
        response.put("recommendations", recommendations);
        return response;
    }

    /**
     * Returns query regressions.
     */
    @GET
    @Path("/query-regressions")
    public Map<String, Object> getQueryRegressions(
            @QueryParam("instance") @DefaultValue("default") String instance,
            @QueryParam("window") @DefaultValue("24") int windowHours,
            @QueryParam("threshold") @DefaultValue("50") int thresholdPercent) {
        var regressions = queryRegressionService.detectRegressions(instance, windowHours, thresholdPercent);
        var improvements = queryRegressionService.detectImprovements(instance, windowHours, thresholdPercent);
        var summary = queryRegressionService.getSummary(instance, windowHours, thresholdPercent);
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("instance", instance);
        response.put("windowHours", windowHours);
        response.put("thresholdPercent", thresholdPercent);
        response.put("summary", summary);
        response.put("regressions", regressions);
        response.put("improvements", improvements);
        return response;
    }

    /**
     * Returns table maintenance recommendations.
     */
    @GET
    @Path("/table-maintenance")
    public Map<String, Object> getTableMaintenance(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        var recommendations = tableMaintenanceService.getRecommendations(instance);
        var summary = tableMaintenanceService.getSummary(instance);
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("instance", instance);
        response.put("summary", summary);
        response.put("recommendations", recommendations);
        return response;
    }

    /**
     * Returns replication status.
     */
    @GET
    @Path("/replication")
    public Map<String, Object> getReplication(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        var replicas = replicationService.getStreamingReplication(instance);
        var slots = replicationService.getReplicationSlots(instance);
        var walStats = replicationService.getWalStats(instance);
        var summary = replicationService.getSummary(instance);
        var isReplica = replicationService.isReplica(instance);
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("instance", instance);
        response.put("isReplica", isReplica);
        response.put("summary", summary);
        response.put("walStats", walStats);
        response.put("replicas", replicas);
        response.put("slots", slots);
        return response;
    }

    /**
     * Returns infrastructure status (vacuum progress, background processes, storage).
     */
    @GET
    @Path("/infrastructure")
    public Map<String, Object> getInfrastructure(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        var vacuumProgress = infrastructureService.getVacuumProgress(instance);
        var bgProcessStats = infrastructureService.getBackgroundProcessStats(instance);
        var storageStats = infrastructureService.getStorageStats(instance);
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("instance", instance);
        response.put("vacuumProgress", vacuumProgress);
        response.put("backgroundProcessStats", bgProcessStats);
        response.put("storageStats", storageStats);
        return response;
    }

    /**
     * Returns list of configured instances.
     */
    @GET
    @Path("/instances")
    public Map<String, Object> getInstances() {
        var instances = dataSourceManager.getInstanceInfoList();
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("count", instances.size());
        response.put("instances", instances);
        return response;
    }

    /**
     * Health check endpoint.
     */
    @GET
    @Path("/health")
    public Map<String, Object> getHealth(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("instance", instance);
        try {
            OverviewStats stats = postgresService.getOverviewStats(instance);
            response.put("status", "healthy");
            response.put("version", stats.getVersion());
            response.put("connections", stats.getActiveConnections());
            response.put("maxConnections", stats.getMaxConnections());
        } catch (Exception e) {
            response.put("status", "unhealthy");
            response.put("error", e.getMessage());
        }
        return response;
    }
}
