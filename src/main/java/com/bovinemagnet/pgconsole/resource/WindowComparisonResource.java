package com.bovinemagnet.pgconsole.resource;

import com.bovinemagnet.pgconsole.config.InstanceConfig;
import com.bovinemagnet.pgconsole.model.WindowComparison;
import com.bovinemagnet.pgconsole.service.DataSourceManager;
import com.bovinemagnet.pgconsole.service.FeatureToggleService;
import com.bovinemagnet.pgconsole.service.WindowComparisonService;
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

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * JAX-RS resource for the comparison window dashboard pages.
 * <p>
 * Provides server-side rendered HTML pages for the window comparison feature,
 * enabling users to compare metrics and query performance across two time
 * windows. The setup page offers preset comparisons and custom datetime inputs,
 * whilst the result page displays the side-by-side analysis.
 * <p>
 * All pages require the "window-compare" feature toggle to be enabled.
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see WindowComparisonService
 * @see WindowComparison
 */
@Path("/window-compare")
public class WindowComparisonResource {

    @ConfigProperty(name = "quarkus.application.name", defaultValue = "pg-console")
    String appName;

    @ConfigProperty(name = "quarkus.application.version", defaultValue = "1.0.0")
    String appVersion;

    @Inject
    InstanceConfig config;

    @Inject
    Template windowCompare;

    @Inject
    Template windowCompareResult;

    @Inject
    WindowComparisonService windowComparisonService;

    @Inject
    FeatureToggleService featureToggleService;

    @Inject
    DataSourceManager dataSourceManager;

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
    // Comparison Setup Page
    // ========================================

    /**
     * Displays the comparison window setup page.
     * <p>
     * Shows preset comparison options (e.g., "Yesterday vs Today",
     * "Last Week Same Time") with availability indicators, and provides
     * custom datetime input fields for bespoke window definitions.
     *
     * @param instance the PostgreSQL instance name
     * @return rendered template
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("window-compare");

        String instanceName = "default".equals(instance) ? getDefaultInstance() : instance;

        List<Map<String, Object>> presets = windowComparisonService.getPresets(instanceName);

        return windowCompare.data("appName", appName)
                .data("appVersion", appVersion)
                .data("currentPage", "window-compare")
                .data("pageTitle", "Window Comparison")
                .data("instance", instanceName)
                .data("currentInstance", instanceName)
                .data("instances", dataSourceManager.getInstanceInfoList())
                .data("presets", presets)
                .data("schemaEnabled", config.schema().enabled())
                .data("toggles", getToggles());
    }

    // ========================================
    // Comparison Result Page
    // ========================================

    /**
     * Displays the comparison result page for two time windows.
     * <p>
     * Parses the ISO-8601 datetime strings for both windows, performs
     * the comparison via {@link WindowComparisonService#compare}, and
     * renders the result template with side-by-side metrics, deltas,
     * and per-query performance changes.
     *
     * @param startA   start of window A as ISO-8601 string
     * @param endA     end of window A as ISO-8601 string
     * @param startB   start of window B as ISO-8601 string
     * @param endB     end of window B as ISO-8601 string
     * @param instance the PostgreSQL instance name
     * @return rendered template
     */
    @GET
    @Path("/result")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance result(
            @QueryParam("startA") String startA,
            @QueryParam("endA") String endA,
            @QueryParam("startB") String startB,
            @QueryParam("endB") String endB,
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("window-compare");

        String instanceName = "default".equals(instance) ? getDefaultInstance() : instance;

        Instant instantStartA = Instant.parse(startA);
        Instant instantEndA = Instant.parse(endA);
        Instant instantStartB = Instant.parse(startB);
        Instant instantEndB = Instant.parse(endB);

        WindowComparison comparison = windowComparisonService.compare(
                instanceName, instantStartA, instantEndA, instantStartB, instantEndB);

        return windowCompareResult.data("appName", appName)
                .data("appVersion", appVersion)
                .data("currentPage", "window-compare")
                .data("pageTitle", "Window Comparison Result")
                .data("instance", instanceName)
                .data("currentInstance", instanceName)
                .data("instances", dataSourceManager.getInstanceInfoList())
                .data("comparison", comparison)
                .data("schemaEnabled", config.schema().enabled())
                .data("toggles", getToggles());
    }
}
