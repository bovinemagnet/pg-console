package com.bovinemagnet.pgconsole.resource;

import com.bovinemagnet.pgconsole.config.InstanceConfig;
import com.bovinemagnet.pgconsole.model.ColumnCorrelation;
import com.bovinemagnet.pgconsole.model.HotUpdateEfficiency;
import com.bovinemagnet.pgconsole.model.IndexRedundancy;
import com.bovinemagnet.pgconsole.model.LiveChartData;
import com.bovinemagnet.pgconsole.model.PipelineRisk;
import com.bovinemagnet.pgconsole.model.StatisticalFreshness;
import com.bovinemagnet.pgconsole.model.ToastBloat;
import com.bovinemagnet.pgconsole.model.WriteReadRatio;
import com.bovinemagnet.pgconsole.model.XidWraparound;
import com.bovinemagnet.pgconsole.service.FeatureToggleService;
import com.bovinemagnet.pgconsole.service.PostgresService;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;
import java.util.Map;

/**
 * Resource for Phase 21 Enhanced Database Diagnostics dashboards.
 * <p>
 * Provides endpoints for:
 * <ul>
 *   <li>Pipeline/Queue Risk Monitoring</li>
 *   <li>TOAST Bloat Analysis</li>
 *   <li>Index Redundancy Detection</li>
 *   <li>Statistical Freshness Monitoring</li>
 *   <li>Write/Read Ratio Analysis</li>
 *   <li>HOT Update Efficiency</li>
 *   <li>Column Correlation Statistics</li>
 *   <li>Interactive Live Charts</li>
 *   <li>XID Wraparound Monitoring</li>
 * </ul>
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@Path("/diagnostics")
public class DiagnosticsResource {

    @ConfigProperty(name = "quarkus.application.name", defaultValue = "pg-console")
    String appName;

    @ConfigProperty(name = "quarkus.application.version", defaultValue = "1.0.0")
    String appVersion;

    @ConfigProperty(name = "pg-console.diagnostics.queue-stale-hours", defaultValue = "24")
    int queueStaleHours;

    @ConfigProperty(name = "pg-console.diagnostics.toast-bloat-warn-percent", defaultValue = "30")
    int toastBloatWarnPercent;

    @ConfigProperty(name = "pg-console.diagnostics.hot-efficiency-warn-percent", defaultValue = "50")
    int hotEfficiencyWarnPercent;

    @ConfigProperty(name = "pg-console.diagnostics.xid-warn-percent", defaultValue = "50")
    int xidWarnPercent;

    @ConfigProperty(name = "pg-console.diagnostics.xid-critical-percent", defaultValue = "75")
    int xidCriticalPercent;

    @Inject
    InstanceConfig config;

    @Inject
    Template pipelineRisk;

    @Inject
    Template toastBloat;

    @Inject
    Template indexRedundancy;

    @Inject
    Template statisticalFreshness;

    @Inject
    Template writeReadRatio;

    @Inject
    Template hotEfficiency;

    @Inject
    Template correlation;

    @Inject
    Template liveCharts;

    @Inject
    Template xidWraparound;

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

    /**
     * Returns the feature toggles map for template rendering.
     *
     * @return map of feature toggle states
     */
    private Map<String, Boolean> getToggles() {
        return featureToggleService.getAllToggles();
    }

    // ========================================
    // Pipeline Risk Monitoring
    // ========================================

    /**
     * Displays the Pipeline Risk monitoring page.
     *
     * @param instance the PostgreSQL instance name
     * @return rendered template
     */
    @GET
    @Path("/pipeline-risk")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance pipelineRisk(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("pipeline-risk");

        String instanceName = "default".equals(instance) ? getDefaultInstance() : instance;
        List<PipelineRisk> risks = postgresService.getPipelineRisk(instanceName, null, queueStaleHours);

        return pipelineRisk.data("appName", appName)
                .data("appVersion", appVersion)
                .data("currentPage", "pipeline-risk")
                .data("pageTitle", "Pipeline Risk")
                .data("instance", instanceName)
                .data("instances", postgresService.getInstanceList())
                .data("risks", risks)
                .data("staleThresholdHours", queueStaleHours)
                .data("schemaEnabled", config.schema().enabled())
                .data("toggles", getToggles());
    }

    // ========================================
    // TOAST Bloat Analysis
    // ========================================

    /**
     * Displays the TOAST Bloat analysis page.
     *
     * @param instance the PostgreSQL instance name
     * @return rendered template
     */
    @GET
    @Path("/toast-bloat")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance toastBloat(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("toast-bloat");

        String instanceName = "default".equals(instance) ? getDefaultInstance() : instance;
        List<ToastBloat> bloats = postgresService.getToastBloat(instanceName);

        return toastBloat.data("appName", appName)
                .data("appVersion", appVersion)
                .data("currentPage", "toast-bloat")
                .data("pageTitle", "TOAST Bloat")
                .data("instance", instanceName)
                .data("instances", postgresService.getInstanceList())
                .data("bloats", bloats)
                .data("warnPercent", toastBloatWarnPercent)
                .data("schemaEnabled", config.schema().enabled())
                .data("toggles", getToggles());
    }

    // ========================================
    // Index Redundancy Detection
    // ========================================

    /**
     * Displays the Index Redundancy detection page.
     *
     * @param instance the PostgreSQL instance name
     * @return rendered template
     */
    @GET
    @Path("/index-redundancy")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance indexRedundancy(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("index-redundancy");

        String instanceName = "default".equals(instance) ? getDefaultInstance() : instance;
        List<IndexRedundancy> redundancies = postgresService.getIndexRedundancy(instanceName);

        return indexRedundancy.data("appName", appName)
                .data("appVersion", appVersion)
                .data("currentPage", "index-redundancy")
                .data("pageTitle", "Index Redundancy")
                .data("instance", instanceName)
                .data("instances", postgresService.getInstanceList())
                .data("redundancies", redundancies)
                .data("schemaEnabled", config.schema().enabled())
                .data("toggles", getToggles());
    }

    // ========================================
    // Statistical Freshness Monitoring
    // ========================================

    /**
     * Displays the Statistical Freshness monitoring page.
     *
     * @param instance the PostgreSQL instance name
     * @return rendered template
     */
    @GET
    @Path("/statistical-freshness")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance statisticalFreshness(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("statistical-freshness");

        String instanceName = "default".equals(instance) ? getDefaultInstance() : instance;
        List<StatisticalFreshness> freshness = postgresService.getStatisticalFreshness(instanceName);

        return statisticalFreshness.data("appName", appName)
                .data("appVersion", appVersion)
                .data("currentPage", "statistical-freshness")
                .data("pageTitle", "Statistical Freshness")
                .data("instance", instanceName)
                .data("instances", postgresService.getInstanceList())
                .data("freshness", freshness)
                .data("schemaEnabled", config.schema().enabled())
                .data("toggles", getToggles());
    }

    // ========================================
    // Write/Read Ratio Analysis
    // ========================================

    /**
     * Displays the Write/Read Ratio analysis page.
     *
     * @param instance the PostgreSQL instance name
     * @return rendered template
     */
    @GET
    @Path("/write-read-ratio")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance writeReadRatio(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("write-read-ratio");

        String instanceName = "default".equals(instance) ? getDefaultInstance() : instance;
        List<WriteReadRatio> ratios = postgresService.getWriteReadRatio(instanceName);

        return writeReadRatio.data("appName", appName)
                .data("appVersion", appVersion)
                .data("currentPage", "write-read-ratio")
                .data("pageTitle", "Write/Read Ratio")
                .data("instance", instanceName)
                .data("instances", postgresService.getInstanceList())
                .data("ratios", ratios)
                .data("schemaEnabled", config.schema().enabled())
                .data("toggles", getToggles());
    }

    // ========================================
    // HOT Update Efficiency
    // ========================================

    /**
     * Displays the HOT Update Efficiency page.
     *
     * @param instance the PostgreSQL instance name
     * @return rendered template
     */
    @GET
    @Path("/hot-efficiency")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance hotEfficiency(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("hot-efficiency");

        String instanceName = "default".equals(instance) ? getDefaultInstance() : instance;
        List<HotUpdateEfficiency> efficiencies = postgresService.getHotEfficiency(instanceName);

        return hotEfficiency.data("appName", appName)
                .data("appVersion", appVersion)
                .data("currentPage", "hot-efficiency")
                .data("pageTitle", "HOT Update Efficiency")
                .data("instance", instanceName)
                .data("instances", postgresService.getInstanceList())
                .data("efficiencies", efficiencies)
                .data("warnPercent", hotEfficiencyWarnPercent)
                .data("schemaEnabled", config.schema().enabled())
                .data("toggles", getToggles());
    }

    // ========================================
    // Column Correlation Statistics
    // ========================================

    /**
     * Displays the Column Correlation statistics page.
     *
     * @param instance the PostgreSQL instance name
     * @return rendered template
     */
    @GET
    @Path("/correlation")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance correlation(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("correlation");

        String instanceName = "default".equals(instance) ? getDefaultInstance() : instance;
        List<ColumnCorrelation> correlations = postgresService.getColumnCorrelation(instanceName);

        return correlation.data("appName", appName)
                .data("appVersion", appVersion)
                .data("currentPage", "correlation")
                .data("pageTitle", "Column Correlation")
                .data("instance", instanceName)
                .data("instances", postgresService.getInstanceList())
                .data("correlations", correlations)
                .data("schemaEnabled", config.schema().enabled())
                .data("toggles", getToggles());
    }

    // ========================================
    // Interactive Live Charts
    // ========================================

    /**
     * Displays the Interactive Live Charts page.
     *
     * @param instance the PostgreSQL instance name
     * @return rendered template
     */
    @GET
    @Path("/live-charts")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance liveCharts(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("live-charts");

        String instanceName = "default".equals(instance) ? getDefaultInstance() : instance;

        // Get initial chart data
        LiveChartData connectionsChart = postgresService.getConnectionsChartData(instanceName);
        LiveChartData transactionsChart = postgresService.getTransactionsChartData(instanceName);
        LiveChartData tuplesChart = postgresService.getTuplesChartData(instanceName);
        LiveChartData cacheChart = postgresService.getCacheChartData(instanceName);

        return liveCharts.data("appName", appName)
                .data("appVersion", appVersion)
                .data("currentPage", "live-charts")
                .data("pageTitle", "Live Charts")
                .data("instance", instanceName)
                .data("instances", postgresService.getInstanceList())
                .data("connectionsChart", connectionsChart)
                .data("transactionsChart", transactionsChart)
                .data("tuplesChart", tuplesChart)
                .data("cacheChart", cacheChart)
                .data("schemaEnabled", config.schema().enabled())
                .data("toggles", getToggles());
    }

    // ========================================
    // XID Wraparound Monitoring
    // ========================================

    /**
     * Displays the XID Wraparound monitoring page.
     *
     * @param instance the PostgreSQL instance name
     * @return rendered template
     */
    @GET
    @Path("/xid-wraparound")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance xidWraparound(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("xid-wraparound");

        String instanceName = "default".equals(instance) ? getDefaultInstance() : instance;
        List<XidWraparound> xids = postgresService.getXidWraparound(instanceName);

        return xidWraparound.data("appName", appName)
                .data("appVersion", appVersion)
                .data("currentPage", "xid-wraparound")
                .data("pageTitle", "XID Wraparound")
                .data("instance", instanceName)
                .data("instances", postgresService.getInstanceList())
                .data("xids", xids)
                .data("warnPercent", xidWarnPercent)
                .data("criticalPercent", xidCriticalPercent)
                .data("schemaEnabled", config.schema().enabled())
                .data("toggles", getToggles());
    }
}
