package com.bovinemagnet.pgconsole.resource;

import com.bovinemagnet.pgconsole.config.InstanceConfig;
import com.bovinemagnet.pgconsole.model.WindowComparison;
import com.bovinemagnet.pgconsole.service.FeatureToggleService;
import com.bovinemagnet.pgconsole.service.WindowComparisonService;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * REST API resource for the comparison window feature.
 * <p>
 * Provides JSON endpoints for performing window comparisons and retrieving
 * preset configurations. These endpoints can be consumed by htmx interactions,
 * external integrations, or programmatic clients.
 * <p>
 * All endpoints require the "window-compare" feature toggle to be enabled and
 * accept an {@code instance} query parameter for multi-instance support.
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see WindowComparisonService
 * @see WindowComparison
 */
@Path("/api/window-compare")
@Produces(MediaType.APPLICATION_JSON)
public class WindowComparisonApiResource {

    @Inject
    InstanceConfig config;

    @Inject
    WindowComparisonService windowComparisonService;

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
     * Resolves the instance name, substituting the default if "default" is specified.
     *
     * @param instance the instance parameter from the request
     * @return the resolved instance name
     */
    private String resolveInstance(String instance) {
        return "default".equals(instance) ? getDefaultInstance() : instance;
    }

    // ========================================
    // Comparison Endpoint
    // ========================================

    /**
     * Performs a comparison between two time windows and returns the result as JSON.
     * <p>
     * Accepts ISO-8601 datetime strings for both window boundaries and returns
     * a {@link WindowComparison} containing aggregated metrics, deltas, and
     * per-query performance changes.
     *
     * @param startA   start of window A (baseline) as ISO-8601 string
     * @param endA     end of window A (baseline) as ISO-8601 string
     * @param startB   start of window B (current) as ISO-8601 string
     * @param endB     end of window B (current) as ISO-8601 string
     * @param instance the PostgreSQL instance name
     * @return the comparison result as JSON
     */
    @GET
    @Path("/compare")
    public WindowComparison compare(
            @QueryParam("startA") String startA,
            @QueryParam("endA") String endA,
            @QueryParam("startB") String startB,
            @QueryParam("endB") String endB,
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("window-compare");

        String instanceName = resolveInstance(instance);

        Instant instantStartA = Instant.parse(startA);
        Instant instantEndA = Instant.parse(endA);
        Instant instantStartB = Instant.parse(startB);
        Instant instantEndB = Instant.parse(endB);

        return windowComparisonService.compare(
                instanceName, instantStartA, instantEndA, instantStartB, instantEndB);
    }

    // ========================================
    // Presets Endpoint
    // ========================================

    /**
     * Returns the available preset comparison configurations as JSON.
     * <p>
     * Each preset includes the window boundaries and an availability flag
     * indicating whether sufficient history data exists for the comparison.
     *
     * @param instance the PostgreSQL instance name
     * @return list of preset configurations as JSON
     */
    @GET
    @Path("/presets")
    public List<Map<String, Object>> getPresets(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("window-compare");

        String instanceName = resolveInstance(instance);
        return windowComparisonService.getPresets(instanceName);
    }
}
