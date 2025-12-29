package com.bovinemagnet.pgconsole.resource;

import com.bovinemagnet.pgconsole.config.InstanceConfig;
import com.bovinemagnet.pgconsole.model.ColumnCorrelation;
import com.bovinemagnet.pgconsole.model.HotUpdateEfficiency;
import com.bovinemagnet.pgconsole.model.IndexRedundancy;
import com.bovinemagnet.pgconsole.model.PipelineRisk;
import com.bovinemagnet.pgconsole.model.StatisticalFreshness;
import com.bovinemagnet.pgconsole.model.ToastBloat;
import com.bovinemagnet.pgconsole.model.WriteReadRatio;
import com.bovinemagnet.pgconsole.model.XidWraparound;
import com.bovinemagnet.pgconsole.service.FeatureToggleService;
import com.bovinemagnet.pgconsole.service.PostgresService;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
}
