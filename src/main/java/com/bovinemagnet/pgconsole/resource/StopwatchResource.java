package com.bovinemagnet.pgconsole.resource;

import com.bovinemagnet.pgconsole.config.InstanceConfig;
import com.bovinemagnet.pgconsole.model.StopwatchSession;
import com.bovinemagnet.pgconsole.service.DataSourceManager;
import com.bovinemagnet.pgconsole.service.FeatureToggleService;
import com.bovinemagnet.pgconsole.service.StopwatchService;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;
import java.util.Map;

/**
 * JAX-RS resource for the Stopwatch dashboard pages.
 * <p>
 * Provides server-side rendered HTML pages for the stopwatch feature, enabling
 * users to capture before/after metric snapshots for performance comparison.
 * The main page shows either an idle state with recent session history, or a
 * running state with the active session and elapsed timer.
 * <p>
 * Individual session results are displayed on a detail page with side-by-side
 * comparison of start and end metrics, delta calculations, and top query diffs.
 * <p>
 * All pages require the "stopwatch" feature toggle to be enabled.
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see StopwatchService
 * @see StopwatchSession
 */
@Path("/stopwatch")
public class StopwatchResource {

    @ConfigProperty(name = "quarkus.application.name", defaultValue = "pg-console")
    String appName;

    @ConfigProperty(name = "quarkus.application.version", defaultValue = "1.0.0")
    String appVersion;

    @Inject
    InstanceConfig config;

    @Inject
    Template stopwatch;

    @Inject
    Template stopwatchResult;

    @Inject
    StopwatchService stopwatchService;

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
    // Stopwatch Main Page
    // ========================================

    /**
     * Displays the main stopwatch page.
     * <p>
     * If a stopwatch session is currently running for the selected instance,
     * the page shows the running state with elapsed timer and a stop/cancel
     * button. Otherwise, the page shows the idle state with a start button
     * and a list of recent sessions.
     *
     * @param instance the PostgreSQL instance name
     * @return rendered template
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("stopwatch");

        String instanceName = "default".equals(instance) ? getDefaultInstance() : instance;

        StopwatchSession activeSession = stopwatchService.getActiveSession(instanceName);
        List<StopwatchSession> recentSessions = stopwatchService.getRecentSessions(instanceName);

        return stopwatch.data("appName", appName)
                .data("appVersion", appVersion)
                .data("currentPage", "stopwatch")
                .data("pageTitle", "Stopwatch")
                .data("instance", instanceName)
                .data("currentInstance", instanceName)
                .data("instances", dataSourceManager.getInstanceInfoList())
                .data("activeSession", activeSession)
                .data("recentSessions", recentSessions)
                .data("schemaEnabled", config.schema().enabled())
                .data("toggles", getToggles());
    }

    // ========================================
    // Stopwatch Result Page
    // ========================================

    /**
     * Displays the stopwatch result page for a completed session.
     * <p>
     * Shows a side-by-side comparison of the start and end metrics snapshots,
     * with delta calculations highlighting improvements and degradations.
     * Also displays top query comparisons between the two snapshots.
     *
     * @param id       the stopwatch session id
     * @param instance the PostgreSQL instance name
     * @return rendered template
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance result(
            @PathParam("id") long id,
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("stopwatch");

        String instanceName = "default".equals(instance) ? getDefaultInstance() : instance;

        StopwatchSession session = stopwatchService.getSession(id);

        return stopwatchResult.data("appName", appName)
                .data("appVersion", appVersion)
                .data("currentPage", "stopwatch")
                .data("pageTitle", "Stopwatch Result")
                .data("instance", instanceName)
                .data("currentInstance", instanceName)
                .data("instances", dataSourceManager.getInstanceInfoList())
                .data("session", session)
                .data("schemaEnabled", config.schema().enabled())
                .data("toggles", getToggles());
    }
}
