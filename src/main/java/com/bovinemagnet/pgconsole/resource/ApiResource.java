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
import com.bovinemagnet.pgconsole.model.ComparisonHistory;
import com.bovinemagnet.pgconsole.model.ComparisonProfile;
import com.bovinemagnet.pgconsole.model.SchemaComparisonResult;
import com.bovinemagnet.pgconsole.service.ComparisonHistoryService;
import com.bovinemagnet.pgconsole.service.ComparisonProfileService;
import com.bovinemagnet.pgconsole.service.ComplianceService;
import com.bovinemagnet.pgconsole.service.ConnectionSecurityService;
import com.bovinemagnet.pgconsole.service.DataAccessPatternService;
import com.bovinemagnet.pgconsole.service.DataSourceManager;
import com.bovinemagnet.pgconsole.service.FeatureToggleService;
import com.bovinemagnet.pgconsole.service.SchemaComparisonService;
import com.bovinemagnet.pgconsole.service.IndexAdvisorService;
import com.bovinemagnet.pgconsole.service.InfrastructureService;
import com.bovinemagnet.pgconsole.service.PostgresService;
import com.bovinemagnet.pgconsole.service.QueryRegressionService;
import com.bovinemagnet.pgconsole.service.ReplicationService;
import com.bovinemagnet.pgconsole.service.SecurityAuditService;
import com.bovinemagnet.pgconsole.service.SecurityRecommendationService;
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
    SchemaComparisonService schemaComparisonService;

    @Inject
    ComparisonProfileService comparisonProfileService;

    @Inject
    ComparisonHistoryService comparisonHistoryService;

    @Inject
    FeatureToggleService featureToggleService;

    /**
     * Returns overview statistics for a PostgreSQL instance as JSON.
     * <p>
     * Includes timestamp, instance identifier, and key metrics such as
     * connection counts, database size, and cache hit ratio.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return JSON map containing timestamp, instance name, and overview statistics
     */
    @GET
    @Path("/overview")
    public Map<String, Object> getOverview(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("dashboard");
        OverviewStats stats = postgresService.getOverviewStats(instance);
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("instance", instance);
        response.put("stats", stats);
        return response;
    }

    /**
     * Returns current database activity as JSON.
     * <p>
     * Retrieves active sessions from pg_stat_activity including running queries,
     * idle connections, and session metadata.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return JSON map containing timestamp, instance name, activity count, and activity list
     */
    @GET
    @Path("/activity")
    public Map<String, Object> getActivity(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("activity");
        List<Activity> activities = postgresService.getCurrentActivity(instance);
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("instance", instance);
        response.put("count", activities.size());
        response.put("activities", activities);
        return response;
    }

    /**
     * Returns slow queries from pg_stat_statements as JSON.
     * <p>
     * Retrieves query performance statistics with configurable sorting and limiting.
     * Results can be sorted by total time, calls, mean time, or other metrics.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @param sortBy the sort field (defaults to "totalTime")
     * @param order the sort order "asc" or "desc" (defaults to "desc")
     * @param limit maximum number of queries to return (defaults to 50)
     * @return JSON map containing timestamp, instance name, query count, and query list
     */
    @GET
    @Path("/slow-queries")
    public Map<String, Object> getSlowQueries(
            @QueryParam("instance") @DefaultValue("default") String instance,
            @QueryParam("sortBy") @DefaultValue("totalTime") String sortBy,
            @QueryParam("order") @DefaultValue("desc") String order,
            @QueryParam("limit") @DefaultValue("50") int limit) {
        featureToggleService.requirePageEnabled("slow-queries");
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
     * Returns lock information as JSON.
     * <p>
     * Retrieves current locks from pg_locks and a hierarchical blocking tree
     * showing which processes are blocking others.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return JSON map containing timestamp, instance name, blocking tree, and locks list
     */
    @GET
    @Path("/locks")
    public Map<String, Object> getLocks(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("locks");
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
     * Returns wait event summary as JSON.
     * <p>
     * Retrieves wait event statistics from pg_stat_activity, grouped by type
     * and individually, to help identify performance bottlenecks.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return JSON map containing timestamp, instance name, type summaries, and wait events
     */
    @GET
    @Path("/wait-events")
    public Map<String, Object> getWaitEvents(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("wait-events");
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
     * Returns table statistics as JSON.
     * <p>
     * Retrieves statistics for all user tables from pg_stat_user_tables
     * including sizes, row counts, index usage, and vacuum information.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return JSON map containing timestamp, instance name, table count, and tables list
     */
    @GET
    @Path("/tables")
    public Map<String, Object> getTables(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("tables");
        List<TableStats> tables = postgresService.getTableStats(instance);
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("instance", instance);
        response.put("count", tables.size());
        response.put("tables", tables);
        return response;
    }

    /**
     * Returns database metrics as JSON.
     * <p>
     * Retrieves metrics for all databases in the PostgreSQL instance
     * including size, connection counts, transaction rates, and cache hit ratios.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return JSON map containing timestamp, instance name, database count, and databases list
     */
    @GET
    @Path("/databases")
    public Map<String, Object> getDatabases(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("databases");
        List<DatabaseMetrics> databases = postgresService.getAllDatabaseMetrics(instance);
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("instance", instance);
        response.put("count", databases.size());
        response.put("databases", databases);
        return response;
    }

    /**
     * Returns index recommendations as JSON.
     * <p>
     * Analyses query patterns and table scans to suggest missing indexes
     * that could improve query performance.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return JSON map containing timestamp, instance name, summary, and recommendations list
     */
    @GET
    @Path("/index-advisor")
    public Map<String, Object> getIndexAdvisor(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("index-advisor");
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
     * Returns query regressions as JSON.
     * <p>
     * Compares query performance over time to detect regressions (slower) and
     * improvements (faster). Uses historical data to identify significant changes.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @param windowHours time window in hours for comparison (defaults to 24)
     * @param thresholdPercent percentage change threshold for detection (defaults to 50)
     * @return JSON map containing timestamp, instance name, window parameters, summary, regressions, and improvements
     */
    @GET
    @Path("/query-regressions")
    public Map<String, Object> getQueryRegressions(
            @QueryParam("instance") @DefaultValue("default") String instance,
            @QueryParam("window") @DefaultValue("24") int windowHours,
            @QueryParam("threshold") @DefaultValue("50") int thresholdPercent) {
        featureToggleService.requirePageEnabled("query-regressions");
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
     * Returns table maintenance recommendations as JSON.
     * <p>
     * Identifies tables that need vacuuming or analysing based on bloat,
     * dead tuple counts, and last vacuum/analyse timestamps.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return JSON map containing timestamp, instance name, summary, and recommendations list
     */
    @GET
    @Path("/table-maintenance")
    public Map<String, Object> getTableMaintenance(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("table-maintenance");
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
     * Returns replication status as JSON.
     * <p>
     * Retrieves streaming replication information including connected replicas,
     * replication slots, WAL statistics, and whether this instance is a replica.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return JSON map containing timestamp, instance name, replica status, summary, WAL stats, replicas, and slots
     */
    @GET
    @Path("/replication")
    public Map<String, Object> getReplication(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("replication");
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
     * Returns infrastructure status as JSON.
     * <p>
     * Retrieves vacuum progress, background writer statistics, checkpointer stats,
     * and storage usage information for monitoring PostgreSQL infrastructure health.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return JSON map containing timestamp, instance name, vacuum progress, background process stats, and storage stats
     */
    @GET
    @Path("/infrastructure")
    public Map<String, Object> getInfrastructure(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("infrastructure");
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
     * Returns list of configured PostgreSQL instances as JSON.
     * <p>
     * Retrieves all configured instances from the application configuration,
     * including their names and display names.
     *
     * @return JSON map containing timestamp, instance count, and instances list
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
     * Health check endpoint for monitoring and load balancers.
     * <p>
     * Returns health status, PostgreSQL version, and connection information.
     * Returns "healthy" status if database is accessible, "unhealthy" with error details otherwise.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return JSON map containing timestamp, instance name, status, and database information or error details
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

    // --- Phase 10: Security & Compliance Monitoring ---

    /**
     * Returns security summary as JSON.
     * <p>
     * Provides an overview of security status including role audit summary,
     * connection security summary, data access patterns, compliance scores,
     * and recommendation counts by priority.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return JSON map containing timestamp, instance name, and security summaries
     */
    @GET
    @Path("/security/summary")
    public Map<String, Object> getSecuritySummary(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("security");
        var auditSummary = securityAuditService.getSummary(instance);
        var connectionSummary = connectionSecurityService.getSummary(instance);
        var accessSummary = dataAccessPatternService.getSummary(instance);
        var complianceSummary = complianceService.getSummary(instance);
        var recommendationSummary = securityRecommendationService.getSummary(instance);
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("instance", instance);
        response.put("roles", auditSummary);
        response.put("connections", connectionSummary);
        response.put("dataAccess", accessSummary);
        response.put("compliance", complianceSummary);
        response.put("recommendations", recommendationSummary);
        return response;
    }

    /**
     * Returns role and permission audit data as JSON.
     * <p>
     * Retrieves all database roles with their privileges, role memberships,
     * and security warnings about elevated privileges or password issues.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return JSON map containing timestamp, instance name, roles, memberships, warnings, and summary
     */
    @GET
    @Path("/security/roles")
    public Map<String, Object> getSecurityRoles(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("security-roles");
        var roles = securityAuditService.getAllRoles(instance);
        var memberships = securityAuditService.getRoleMemberships(instance);
        var warnings = securityAuditService.getAllWarnings(instance);
        var summary = securityAuditService.getSummary(instance);
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("instance", instance);
        response.put("count", roles.size());
        response.put("roles", roles);
        response.put("memberships", memberships);
        response.put("warnings", warnings);
        response.put("summary", summary);
        return response;
    }

    /**
     * Returns connection security data as JSON.
     * <p>
     * Retrieves SSL connection status, pg_hba.conf rules, authentication methods,
     * and connection security warnings.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return JSON map containing timestamp, instance name, SSL connections, HBA rules, warnings, and summary
     */
    @GET
    @Path("/security/connections")
    public Map<String, Object> getSecurityConnections(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("security-connections");
        var sslConnections = connectionSecurityService.getSslConnections(instance);
        var hbaRules = connectionSecurityService.getHbaRules(instance);
        var warnings = connectionSecurityService.getWarnings(instance);
        var summary = connectionSecurityService.getSummary(instance);
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("instance", instance);
        response.put("sslConnections", sslConnections);
        response.put("hbaRules", hbaRules);
        response.put("warnings", warnings);
        response.put("summary", summary);
        return response;
    }

    /**
     * Returns data access pattern data as JSON.
     * <p>
     * Retrieves sensitive tables with PII detection, PII column indicators,
     * row-level security policies, and data access summary.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return JSON map containing timestamp, instance name, sensitive tables, PII columns, RLS policies, and summary
     */
    @GET
    @Path("/security/access")
    public Map<String, Object> getSecurityAccess(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("security-access");
        var sensitiveTables = dataAccessPatternService.getSensitiveTables(instance);
        var piiColumns = dataAccessPatternService.getPiiColumns(instance);
        var rlsPolicies = dataAccessPatternService.getRlsPolicies(instance);
        var summary = dataAccessPatternService.getSummary(instance);
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("instance", instance);
        response.put("sensitiveTables", sensitiveTables);
        response.put("piiColumns", piiColumns);
        response.put("rlsPolicies", rlsPolicies);
        response.put("summary", summary);
        return response;
    }

    /**
     * Returns compliance scores as JSON.
     * <p>
     * Retrieves compliance scores across multiple areas including access control,
     * encryption, audit logging, data protection, and authentication.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return JSON map containing timestamp, instance name, scores, and summary
     */
    @GET
    @Path("/security/compliance")
    public Map<String, Object> getSecurityCompliance(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("security-compliance");
        var scores = complianceService.getAllComplianceScores(instance);
        var summary = complianceService.getSummary(instance);
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("instance", instance);
        response.put("scores", scores);
        response.put("summary", summary);
        return response;
    }

    /**
     * Returns security recommendations as JSON.
     * <p>
     * Retrieves actionable security recommendations organised by priority
     * (critical, high, medium, low) with suggested actions and rationale.
     *
     * @param instance the PostgreSQL instance identifier (defaults to "default")
     * @return JSON map containing timestamp, instance name, recommendations, and summary
     */
    @GET
    @Path("/security/recommendations")
    public Map<String, Object> getSecurityRecommendations(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("security-recommendations");
        var recommendations = securityRecommendationService.getAllRecommendations(instance);
        var summary = securityRecommendationService.getSummary(instance);
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("instance", instance);
        response.put("count", recommendations.size());
        response.put("recommendations", recommendations);
        response.put("summary", summary);
        return response;
    }

    // --- Phase 12: Schema Comparison & Migration ---

    /**
     * Compares schemas between two PostgreSQL instances.
     * <p>
     * Returns detailed differences including missing, extra, and modified objects.
     * Supports filtering by object types.
     *
     * @param sourceInstance source instance name
     * @param destInstance destination instance name
     * @param sourceSchema source schema (default: public)
     * @param destSchema destination schema (default: public)
     * @return JSON containing comparison result with all differences
     */
    @GET
    @Path("/schema-comparison/compare")
    public Map<String, Object> compareSchemas(
            @QueryParam("sourceInstance") @DefaultValue("default") String sourceInstance,
            @QueryParam("destInstance") @DefaultValue("default") String destInstance,
            @QueryParam("sourceSchema") @DefaultValue("public") String sourceSchema,
            @QueryParam("destSchema") @DefaultValue("public") String destSchema) {
        featureToggleService.requirePageEnabled("schema-comparison");

        SchemaComparisonResult result = schemaComparisonService.compare(
                sourceInstance, destInstance, sourceSchema, destSchema, null);

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("sourceInstance", sourceInstance);
        response.put("destInstance", destInstance);
        response.put("sourceSchema", sourceSchema);
        response.put("destSchema", destSchema);
        response.put("success", result.isSuccess());
        response.put("summary", result.getSummary());
        response.put("differences", result.getDifferences());

        if (!result.isSuccess()) {
            response.put("error", result.getErrorMessage());
        }

        return response;
    }

    /**
     * Returns available schemas for an instance.
     *
     * @param instance instance name
     * @return JSON list of schema names
     */
    @GET
    @Path("/schema-comparison/schemas")
    public Map<String, Object> getSchemas(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("schema-comparison");

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("instance", instance);
        response.put("schemas", schemaComparisonService.getSchemas(instance));
        return response;
    }

    /**
     * Returns saved comparison profiles.
     *
     * @return JSON list of comparison profiles
     */
    @GET
    @Path("/schema-comparison/profiles")
    public Map<String, Object> getProfiles() {
        featureToggleService.requirePageEnabled("schema-comparison");
        List<ComparisonProfile> profiles = comparisonProfileService.findAll();
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("count", profiles.size());
        response.put("profiles", profiles);
        return response;
    }

    /**
     * Returns comparison history records.
     *
     * @param limit maximum records to return (default: 50)
     * @return JSON list of history records
     */
    @GET
    @Path("/schema-comparison/history")
    public Map<String, Object> getHistory(
            @QueryParam("limit") @DefaultValue("50") int limit) {
        featureToggleService.requirePageEnabled("schema-comparison");

        List<ComparisonHistory> history = comparisonHistoryService.getHistory(limit);
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("count", history.size());
        response.put("totalCount", comparisonHistoryService.count());
        response.put("history", history);
        return response;
    }

    /**
     * Returns schema summary statistics.
     *
     * @param instance instance name
     * @param schema schema name (default: public)
     * @return JSON map of object type to count
     */
    @GET
    @Path("/schema-comparison/summary")
    public Map<String, Object> getSchemaSummary(
            @QueryParam("instance") @DefaultValue("default") String instance,
            @QueryParam("schema") @DefaultValue("public") String schema) {
        featureToggleService.requirePageEnabled("schema-comparison");

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("instance", instance);
        response.put("schema", schema);
        response.put("summary", schemaComparisonService.getSchemaSummary(instance, schema));
        return response;
    }
}
