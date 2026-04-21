package com.bovinemagnet.pgconsole.resource;

import com.bovinemagnet.pgconsole.config.InstanceConfig;
import com.bovinemagnet.pgconsole.model.ColumnCorrelation;
import com.bovinemagnet.pgconsole.model.DatabaseMetricsHistory;
import com.bovinemagnet.pgconsole.model.HotUpdateEfficiency;
import com.bovinemagnet.pgconsole.model.IndexRedundancy;
import com.bovinemagnet.pgconsole.model.InfrastructureMetricsHistory;
import com.bovinemagnet.pgconsole.model.LiveChartHistoryPoint;
import com.bovinemagnet.pgconsole.model.PipelineRisk;
import com.bovinemagnet.pgconsole.model.QueryMetricsHistory;
import com.bovinemagnet.pgconsole.model.StatisticalFreshness;
import com.bovinemagnet.pgconsole.model.ToastBloat;
import com.bovinemagnet.pgconsole.model.WriteReadRatio;
import com.bovinemagnet.pgconsole.model.XidWraparound;
import com.bovinemagnet.pgconsole.repository.HistoryRepository;
import com.bovinemagnet.pgconsole.service.FeatureToggleService;
import com.bovinemagnet.pgconsole.service.LiveChartHistoryStore;
import com.bovinemagnet.pgconsole.service.MetricsHistoryBridgeService;
import com.bovinemagnet.pgconsole.service.PostgresService;
import com.bovinemagnet.pgconsole.util.Filenames;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API resource for Phase 21 Enhanced Database Diagnostics.
 * <p>
 * Provides JSON endpoints for:
 * <ul>
 *   <li>Live chart data (connections, transactions, tuples, cache)</li>
 *   <li>All diagnostics metrics as JSON</li>
 * </ul>
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@Path("/api/diagnostics")
@Produces(MediaType.APPLICATION_JSON)
public class DiagnosticsApiResource {

    @ConfigProperty(name = "pg-console.diagnostics.queue-stale-hours", defaultValue = "24")
    int queueStaleHours;

    @Inject
    InstanceConfig config;

    @Inject
    PostgresService postgresService;

    @Inject
    FeatureToggleService featureToggleService;

    @Inject
    LiveChartHistoryStore liveChartHistoryStore;

    @Inject
    MetricsHistoryBridgeService metricsHistoryBridgeService;

    @Inject
    HistoryRepository historyRepository;

    /**
     * Returns the default instance name from configuration.
     * <p>
     * Parses the instances configuration and returns the first one.
     *
     * @return the default instance name
     */
    private String getDefaultInstance() {
        String instances = config.instances();
        if (instances == null || instances.isBlank()) {
            return "default";
        }
        return instances.split(",")[0].trim();
    }

    private String resolveInstance(String instance) {
        return "default".equals(instance) ? getDefaultInstance() : instance;
    }

    // ========================================
    // Live Chart Endpoints
    // ========================================

    /**
     * Returns current connection counts by state.
     *
     * @param instance the PostgreSQL instance name
     * @return map with active, idle, idleInTransaction counts
     */
    @GET
    @Path("/live-charts/connections")
    public Map<String, Object> getConnectionsData(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("live-charts");

        String instanceName = resolveInstance(instance);
        Map<String, Object> result = new HashMap<>();

        var chartData = postgresService.getConnectionsChartData(instanceName);
        var activeSeries = chartData.getSeriesByName("Active");
        var idleSeries = chartData.getSeriesByName("Idle");
        var idleTxnSeries = chartData.getSeriesByName("Idle in Transaction");

        result.put("active", activeSeries != null ? activeSeries.getLatestValue() : 0);
        result.put("idle", idleSeries != null ? idleSeries.getLatestValue() : 0);
        result.put("idleInTransaction", idleTxnSeries != null ? idleTxnSeries.getLatestValue() : 0);
        result.put("timestamp", System.currentTimeMillis());

        return result;
    }

    /**
     * Returns current transaction counts.
     *
     * @param instance the PostgreSQL instance name
     * @return map with commits and rollbacks counts
     */
    @GET
    @Path("/live-charts/transactions")
    public Map<String, Object> getTransactionsData(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("live-charts");

        String instanceName = resolveInstance(instance);
        Map<String, Object> result = new HashMap<>();

        var chartData = postgresService.getTransactionsChartData(instanceName);
        var commitsSeries = chartData.getSeriesByName("Commits");
        var rollbacksSeries = chartData.getSeriesByName("Rollbacks");

        result.put("commits", commitsSeries != null ? commitsSeries.getLatestValue() : 0);
        result.put("rollbacks", rollbacksSeries != null ? rollbacksSeries.getLatestValue() : 0);
        result.put("timestamp", System.currentTimeMillis());

        return result;
    }

    /**
     * Returns current tuple operation counts.
     *
     * @param instance the PostgreSQL instance name
     * @return map with inserted, updated, deleted counts
     */
    @GET
    @Path("/live-charts/tuples")
    public Map<String, Object> getTuplesData(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("live-charts");

        String instanceName = resolveInstance(instance);
        Map<String, Object> result = new HashMap<>();

        var chartData = postgresService.getTuplesChartData(instanceName);
        var insertSeries = chartData.getSeriesByName("Inserted");
        var updateSeries = chartData.getSeriesByName("Updated");
        var deleteSeries = chartData.getSeriesByName("Deleted");

        result.put("inserted", insertSeries != null ? insertSeries.getLatestValue() : 0);
        result.put("updated", updateSeries != null ? updateSeries.getLatestValue() : 0);
        result.put("deleted", deleteSeries != null ? deleteSeries.getLatestValue() : 0);
        result.put("timestamp", System.currentTimeMillis());

        return result;
    }

    /**
     * Returns current cache hit ratios.
     *
     * @param instance the PostgreSQL instance name
     * @return map with bufferHitRatio and indexHitRatio
     */
    @GET
    @Path("/live-charts/cache")
    public Map<String, Object> getCacheData(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("live-charts");

        String instanceName = resolveInstance(instance);
        Map<String, Object> result = new HashMap<>();

        var chartData = postgresService.getCacheChartData(instanceName);
        var bufferSeries = chartData.getSeriesByName("Buffer Cache");
        var indexSeries = chartData.getSeriesByName("Index Cache");

        result.put("bufferHitRatio", bufferSeries != null ? bufferSeries.getLatestValue() : 100.0);
        result.put("indexHitRatio", indexSeries != null ? indexSeries.getLatestValue() : 100.0);
        result.put("timestamp", System.currentTimeMillis());

        return result;
    }

    // ========================================
    // Diagnostics Data Endpoints
    // ========================================

    /**
     * Returns pipeline risk metrics as JSON.
     *
     * @param instance the PostgreSQL instance name
     * @return list of pipeline risk metrics
     */
    @GET
    @Path("/pipeline-risk")
    public List<PipelineRisk> getPipelineRisk(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("pipeline-risk");
        return postgresService.getPipelineRisk(resolveInstance(instance), null, queueStaleHours);
    }

    /**
     * Returns TOAST bloat metrics as JSON.
     *
     * @param instance the PostgreSQL instance name
     * @return list of TOAST bloat metrics
     */
    @GET
    @Path("/toast-bloat")
    public List<ToastBloat> getToastBloat(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("toast-bloat");
        return postgresService.getToastBloat(resolveInstance(instance));
    }

    /**
     * Returns index redundancy findings as JSON.
     *
     * @param instance the PostgreSQL instance name
     * @return list of index redundancy findings
     */
    @GET
    @Path("/index-redundancy")
    public List<IndexRedundancy> getIndexRedundancy(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("index-redundancy");
        return postgresService.getIndexRedundancy(resolveInstance(instance));
    }

    /**
     * Returns statistical freshness metrics as JSON.
     *
     * @param instance the PostgreSQL instance name
     * @return list of statistical freshness metrics
     */
    @GET
    @Path("/statistical-freshness")
    public List<StatisticalFreshness> getStatisticalFreshness(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("statistical-freshness");
        return postgresService.getStatisticalFreshness(resolveInstance(instance));
    }

    /**
     * Returns write/read ratio metrics as JSON.
     *
     * @param instance the PostgreSQL instance name
     * @return list of write/read ratio metrics
     */
    @GET
    @Path("/write-read-ratio")
    public List<WriteReadRatio> getWriteReadRatio(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("write-read-ratio");
        return postgresService.getWriteReadRatio(resolveInstance(instance));
    }

    /**
     * Returns HOT efficiency metrics as JSON.
     *
     * @param instance the PostgreSQL instance name
     * @return list of HOT efficiency metrics
     */
    @GET
    @Path("/hot-efficiency")
    public List<HotUpdateEfficiency> getHotEfficiency(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("hot-efficiency");
        return postgresService.getHotEfficiency(resolveInstance(instance));
    }

    /**
     * Returns column correlation metrics as JSON.
     *
     * @param instance the PostgreSQL instance name
     * @return list of column correlation metrics
     */
    @GET
    @Path("/correlation")
    public List<ColumnCorrelation> getCorrelation(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("correlation");
        return postgresService.getColumnCorrelation(resolveInstance(instance));
    }

    /**
     * Returns XID wraparound metrics as JSON.
     *
     * @param instance the PostgreSQL instance name
     * @return list of XID wraparound metrics
     */
    @GET
    @Path("/xid-wraparound")
    public List<XidWraparound> getXidWraparound(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("xid-wraparound");
        return postgresService.getXidWraparound(resolveInstance(instance));
    }

    // ========================================
    // Metrics History Endpoints
    // ========================================

    /**
     * Returns historical connection counts as time-series arrays.
     * <p>
     * Routes to in-memory store for short windows (up to 60 min) or
     * persisted history tables for longer windows (up to 7 days).
     *
     * @param instance the PostgreSQL instance name
     * @param minutes  the time window in minutes
     * @return map with timestamps, active, idle, idleInTransaction arrays
     */
    @GET
    @Path("/metrics-history/connections")
    public Map<String, Object> getConnectionsHistory(
            @QueryParam("instance") @DefaultValue("default") String instance,
            @QueryParam("minutes") @DefaultValue("30") int minutes) {
        featureToggleService.requirePageEnabled("metrics-history");

        String instanceName = resolveInstance(instance);
        int clampedMinutes = clampMinutes(minutes);
        var ts = metricsHistoryBridgeService.getConnectionsHistory(instanceName, clampedMinutes);

        Map<String, Object> result = new HashMap<>();
        result.put("timestamps", ts.getTimestamps());
        result.put("active", ts.getSeries().get("active"));
        result.put("idle", ts.getSeries().get("idle"));
        result.put("idleInTransaction", ts.getSeries().get("idleInTransaction"));
        result.put("dataPoints", ts.getDataPoints());
        result.put("dataSource", ts.getDataSource());
        return result;
    }

    /**
     * Returns historical transaction rates as time-series arrays.
     * <p>
     * Rate calculation is done server-side: iterates adjacent points and
     * computes delta / seconds_elapsed. Routes to bridge service for
     * in-memory or persisted data.
     *
     * @param instance the PostgreSQL instance name
     * @param minutes  the time window in minutes
     * @return map with timestamps, commitsRate, rollbacksRate arrays
     */
    @GET
    @Path("/metrics-history/transactions")
    public Map<String, Object> getTransactionsHistory(
            @QueryParam("instance") @DefaultValue("default") String instance,
            @QueryParam("minutes") @DefaultValue("30") int minutes) {
        featureToggleService.requirePageEnabled("metrics-history");

        String instanceName = resolveInstance(instance);
        int clampedMinutes = clampMinutes(minutes);
        var ts = metricsHistoryBridgeService.getTransactionsHistory(instanceName, clampedMinutes);

        Map<String, Object> result = new HashMap<>();
        result.put("timestamps", ts.getTimestamps());
        result.put("commitsRate", ts.getSeries().get("commitsRate"));
        result.put("rollbacksRate", ts.getSeries().get("rollbacksRate"));
        result.put("dataPoints", ts.getDataPoints());
        result.put("dataSource", ts.getDataSource());
        return result;
    }

    /**
     * Returns historical tuple operation rates as time-series arrays.
     * <p>
     * Rate calculation is done server-side. Routes to bridge service for
     * in-memory or persisted data.
     *
     * @param instance the PostgreSQL instance name
     * @param minutes  the time window in minutes
     * @return map with timestamps, insertsRate, updatesRate, deletesRate arrays
     */
    @GET
    @Path("/metrics-history/tuples")
    public Map<String, Object> getTuplesHistory(
            @QueryParam("instance") @DefaultValue("default") String instance,
            @QueryParam("minutes") @DefaultValue("30") int minutes) {
        featureToggleService.requirePageEnabled("metrics-history");

        String instanceName = resolveInstance(instance);
        int clampedMinutes = clampMinutes(minutes);
        var ts = metricsHistoryBridgeService.getTuplesHistory(instanceName, clampedMinutes);

        Map<String, Object> result = new HashMap<>();
        result.put("timestamps", ts.getTimestamps());
        result.put("insertsRate", ts.getSeries().get("insertsRate"));
        result.put("updatesRate", ts.getSeries().get("updatesRate"));
        result.put("deletesRate", ts.getSeries().get("deletesRate"));
        result.put("dataPoints", ts.getDataPoints());
        result.put("dataSource", ts.getDataSource());
        return result;
    }

    /**
     * Returns historical cache hit ratios as time-series arrays.
     * <p>
     * Routes to bridge service for in-memory or persisted data.
     *
     * @param instance the PostgreSQL instance name
     * @param minutes  the time window in minutes
     * @return map with timestamps, bufferHitRatio, indexHitRatio arrays
     */
    @GET
    @Path("/metrics-history/cache")
    public Map<String, Object> getCacheHistory(
            @QueryParam("instance") @DefaultValue("default") String instance,
            @QueryParam("minutes") @DefaultValue("30") int minutes) {
        featureToggleService.requirePageEnabled("metrics-history");

        String instanceName = resolveInstance(instance);
        int clampedMinutes = clampMinutes(minutes);
        var ts = metricsHistoryBridgeService.getCacheHistory(instanceName, clampedMinutes);

        Map<String, Object> result = new HashMap<>();
        result.put("timestamps", ts.getTimestamps());
        result.put("bufferHitRatio", ts.getSeries().get("bufferHitRatio"));
        result.put("indexHitRatio", ts.getSeries().get("indexHitRatio"));
        result.put("dataPoints", ts.getDataPoints());
        result.put("dataSource", ts.getDataSource());
        return result;
    }

    /**
     * Returns a list of tracked queries with aggregated metrics.
     *
     * @param instance the PostgreSQL instance name
     * @param hours    the time window in hours
     * @return list of query summaries
     */
    @GET
    @Path("/history/queries/list")
    public List<QueryMetricsHistory> getTrackedQueries(
            @QueryParam("instance") @DefaultValue("default") String instance,
            @QueryParam("hours") @DefaultValue("24") int hours) {
        featureToggleService.requirePageEnabled("query-trends");

        String instanceName = resolveInstance(instance);
        int clampedHours = Math.max(1, Math.min(hours, 168));
        return historyRepository.getDistinctQueryIds(instanceName, clampedHours);
    }

    /**
     * Returns time-series data for a specific query.
     *
     * @param instance the PostgreSQL instance name
     * @param queryId  the query identifier
     * @param hours    the time window in hours
     * @return list of query metrics history points
     */
    @GET
    @Path("/history/queries")
    public List<QueryMetricsHistory> getQueryHistory(
            @QueryParam("instance") @DefaultValue("default") String instance,
            @QueryParam("queryId") String queryId,
            @QueryParam("hours") @DefaultValue("24") int hours) {
        featureToggleService.requirePageEnabled("query-trends");

        String instanceName = resolveInstance(instance);
        int clampedHours = Math.max(1, Math.min(hours, 168));
        return historyRepository.getQueryMetricsHistory(instanceName, queryId, clampedHours);
    }

    /**
     * Returns time-series data for a specific database.
     *
     * @param instance the PostgreSQL instance name
     * @param database the database name
     * @param hours    the time window in hours
     * @return list of database metrics history points
     */
    @GET
    @Path("/history/database")
    public List<DatabaseMetricsHistory> getDatabaseHistory(
            @QueryParam("instance") @DefaultValue("default") String instance,
            @QueryParam("database") String database,
            @QueryParam("hours") @DefaultValue("24") int hours) {
        featureToggleService.requirePageEnabled("database-trends");

        String instanceName = resolveInstance(instance);
        int clampedHours = Math.max(1, Math.min(hours, 168));
        return historyRepository.getDatabaseMetricsHistory(instanceName, database, clampedHours);
    }

    /**
     * Returns time-series data for infrastructure metrics.
     *
     * @param instance the PostgreSQL instance name
     * @param hours    the time window in hours
     * @return list of infrastructure metrics history points
     */
    @GET
    @Path("/history/infrastructure")
    public List<InfrastructureMetricsHistory> getInfrastructureHistory(
            @QueryParam("instance") @DefaultValue("default") String instance,
            @QueryParam("hours") @DefaultValue("24") int hours) {
        featureToggleService.requirePageEnabled("infrastructure-trends");

        String instanceName = resolveInstance(instance);
        int clampedHours = Math.max(1, Math.min(hours, 168));
        return historyRepository.getInfrastructureMetricsHistory(instanceName, clampedHours);
    }

    /**
     * Exports all metrics history as JSON or CSV.
     *
     * @param instance the PostgreSQL instance name
     * @param minutes  the time window in minutes
     * @param format   export format: "json" or "csv"
     * @return downloadable response
     */
    @GET
    @Path("/metrics-history/export")
    public Response exportHistory(
            @QueryParam("instance") @DefaultValue("default") String instance,
            @QueryParam("minutes") @DefaultValue("30") int minutes,
            @QueryParam("format") @DefaultValue("json") String format) {
        featureToggleService.requirePageEnabled("metrics-history");

        String instanceName = resolveInstance(instance);
        int clampedMinutes = clampMinutes(minutes);
        List<LiveChartHistoryPoint> points = liveChartHistoryStore.getHistory(instanceName, clampedMinutes);

        if ("csv".equalsIgnoreCase(format)) {
            return exportCsv(points, instanceName);
        }
        return exportJson(points, instanceName);
    }

    private Response exportJson(List<LiveChartHistoryPoint> points, String instanceName) {
        List<Map<String, Object>> rows = new ArrayList<>(points.size());
        for (LiveChartHistoryPoint p : points) {
            Map<String, Object> row = new HashMap<>();
            row.put("sampledAt", p.getSampledAt().toString());
            row.put("active", p.getActive());
            row.put("idle", p.getIdle());
            row.put("idleInTransaction", p.getIdleInTransaction());
            row.put("commits", p.getCommits());
            row.put("rollbacks", p.getRollbacks());
            row.put("inserted", p.getInserted());
            row.put("updated", p.getUpdated());
            row.put("deleted", p.getDeleted());
            row.put("bufferCacheHitRatio", p.getBufferCacheHitRatio());
            row.put("indexCacheHitRatio", p.getIndexCacheHitRatio());
            rows.add(row);
        }

        Map<String, Object> export = new HashMap<>();
        export.put("instance", instanceName);
        export.put("exportedAt", Instant.now().toString());
        export.put("dataPoints", points.size());
        export.put("data", rows);

        String filename = "metrics-history-" + instanceName + "-"
                + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                        .withZone(ZoneOffset.UTC).format(Instant.now())
                + ".json";

        return Response.ok(export, MediaType.APPLICATION_JSON)
                .header("Content-Disposition", "attachment; filename=\"" + Filenames.sanitize(filename) + "\"")
                .build();
    }

    private Response exportCsv(List<LiveChartHistoryPoint> points, String instanceName) {
        StringBuilder csv = new StringBuilder();
        csv.append("sampledAt,active,idle,idleInTransaction,commits,rollbacks,")
           .append("inserted,updated,deleted,bufferCacheHitRatio,indexCacheHitRatio\n");

        for (LiveChartHistoryPoint p : points) {
            csv.append(p.getSampledAt().toString()).append(',')
               .append(p.getActive()).append(',')
               .append(p.getIdle()).append(',')
               .append(p.getIdleInTransaction()).append(',')
               .append(p.getCommits()).append(',')
               .append(p.getRollbacks()).append(',')
               .append(p.getInserted()).append(',')
               .append(p.getUpdated()).append(',')
               .append(p.getDeleted()).append(',')
               .append(p.getBufferCacheHitRatio()).append(',')
               .append(p.getIndexCacheHitRatio()).append('\n');
        }

        String filename = "metrics-history-" + instanceName + "-"
                + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                        .withZone(ZoneOffset.UTC).format(Instant.now())
                + ".csv";

        return Response.ok(csv.toString(), "text/csv")
                .header("Content-Disposition", "attachment; filename=\"" + Filenames.sanitize(filename) + "\"")
                .build();
    }

    /**
     * Clamps the minutes parameter to allowed values.
     *
     * @param minutes the requested minutes
     * @return clamped value (minimum 1, maximum 10080 i.e. 7 days)
     */
    private int clampMinutes(int minutes) {
        if (minutes < 1) return 5;
        if (minutes > 10080) return 10080;
        return minutes;
    }
}
