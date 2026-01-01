package com.bovinemagnet.pgconsole.resource;

import com.bovinemagnet.pgconsole.config.InstanceConfig;
import com.bovinemagnet.pgconsole.model.*;
import com.bovinemagnet.pgconsole.service.*;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;

/**
 * Resource for intelligent insights dashboards.
 * <p>
 * Provides endpoints for anomaly detection, forecasting, recommendations,
 * natural language queries, and runbook management.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@Path("/insights")
@Produces(MediaType.TEXT_HTML)
public class InsightsResource {

    private static final Logger LOG = Logger.getLogger(InsightsResource.class);

    @Inject
    InsightsService insightsService;

    @Inject
    AnomalyDetectionService anomalyDetectionService;

    @Inject
    ForecastingService forecastingService;

    @Inject
    UnifiedRecommendationService recommendationService;

    @Inject
    NaturalLanguageQueryService nlQueryService;

    @Inject
    RunbookService runbookService;

    @Inject
    FeatureToggleService featureToggleService;

    @Inject
    DataSourceManager dataSourceManager;

    @Inject
    InstanceConfig config;

    @Inject
    Template insights;

    @Inject
    Template anomalies;

    @Inject
    Template forecasts;

    @Inject
    Template recommendations;

    @Inject
    Template runbooks;

    @Inject
    Template runbookExecution;

    /**
     * Main insights dashboard.
     */
    @GET
    public TemplateInstance getInsightsDashboard(@QueryParam("instance") @DefaultValue("default") String instance) {
        if (!featureToggleService.isPageEnabled("insights")) {
            throw new NotFoundException("Insights page is disabled");
        }

        InsightSummary summary = insightsService.getSummary(instance);
        List<UnifiedRecommendation> topRecommendations = insightsService.getTopRecommendations(instance, 5);
        List<DetectedAnomaly> openAnomalies = insightsService.getOpenAnomalies(instance);
        List<Runbook> suggestedRunbooks = insightsService.getSuggestedRunbooks(instance);
        List<String> suggestedQueries = insightsService.getSuggestedQueries();

        return insights
                .data("instance", instance)
                .data("summary", summary)
                .data("topRecommendations", topRecommendations)
                .data("openAnomalies", openAnomalies)
                .data("suggestedRunbooks", suggestedRunbooks)
                .data("suggestedQueries", suggestedQueries)
                .data("instances", dataSourceManager.getInstanceInfoList())
                .data("currentInstance", instance)
                .data("securityEnabled", config.security().enabled())
                .data("schemaEnabled", config.schema().enabled())
                .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Anomalies page.
     */
    @GET
    @Path("/anomalies")
    public TemplateInstance getAnomaliesPage(
            @QueryParam("instance") @DefaultValue("default") String instance,
            @QueryParam("hours") @DefaultValue("24") int hours) {

        if (!featureToggleService.isPageEnabled("anomalies")) {
            throw new NotFoundException("Anomalies page is disabled");
        }

        List<DetectedAnomaly> allAnomalies = insightsService.getAnomalyHistory(instance, hours);
        List<DetectedAnomaly> openAnomalies = insightsService.getOpenAnomalies(instance);
        Map<DetectedAnomaly.Severity, Integer> summary = anomalyDetectionService.getAnomalySummary(instance);

        return anomalies
                .data("instance", instance)
                .data("hours", hours)
                .data("allAnomalies", allAnomalies)
                .data("openAnomalies", openAnomalies)
                .data("summary", summary)
                .data("instances", dataSourceManager.getInstanceInfoList())
                .data("currentInstance", instance)
                .data("securityEnabled", config.security().enabled())
                .data("schemaEnabled", config.schema().enabled())
                .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Forecasts page.
     */
    @GET
    @Path("/forecasts")
    public TemplateInstance getForecastsPage(@QueryParam("instance") @DefaultValue("default") String instance) {
        if (!featureToggleService.isPageEnabled("forecasts")) {
            throw new NotFoundException("Forecasts page is disabled");
        }

        List<MetricForecast> storageForecasts = insightsService.getStorageForecasts(instance);
        List<MetricForecast> connectionForecasts = insightsService.getConnectionForecasts(instance);
        Double storageGrowthRate = insightsService.getStorageGrowthRate(instance);

        // Pre-calculate formatted growth rates for template display
        String dailyGrowthMb = storageGrowthRate != null
                ? String.format("%.2f", storageGrowthRate / 1024.0 / 1024.0)
                : null;
        String weeklyGrowthGb = storageGrowthRate != null
                ? String.format("%.2f", storageGrowthRate * 7.0 / 1024.0 / 1024.0 / 1024.0)
                : null;
        String monthlyGrowthGb = storageGrowthRate != null
                ? String.format("%.2f", storageGrowthRate * 30.0 / 1024.0 / 1024.0 / 1024.0)
                : null;

        return forecasts
                .data("instance", instance)
                .data("storageForecasts", storageForecasts)
                .data("connectionForecasts", connectionForecasts)
                .data("storageGrowthRate", storageGrowthRate)
                .data("dailyGrowthMb", dailyGrowthMb)
                .data("weeklyGrowthGb", weeklyGrowthGb)
                .data("monthlyGrowthGb", monthlyGrowthGb)
                .data("instances", dataSourceManager.getInstanceInfoList())
                .data("currentInstance", instance)
                .data("securityEnabled", config.security().enabled())
                .data("schemaEnabled", config.schema().enabled())
                .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Unified recommendations page.
     */
    @GET
    @Path("/recommendations")
    public TemplateInstance getRecommendationsPage(
            @QueryParam("instance") @DefaultValue("default") String instance,
            @QueryParam("source") String source,
            @QueryParam("severity") String severity) {

        if (!featureToggleService.isPageEnabled("recommendations")) {
            throw new NotFoundException("Recommendations page is disabled");
        }

        List<UnifiedRecommendation> allRecommendations;

        if (source != null && !source.isBlank()) {
            try {
                UnifiedRecommendation.Source sourceEnum = UnifiedRecommendation.Source.valueOf(source.toUpperCase());
                allRecommendations = recommendationService.getRecommendationsBySource(instance, sourceEnum);
            } catch (IllegalArgumentException e) {
                allRecommendations = recommendationService.getRecommendations(instance);
            }
        } else if (severity != null && !severity.isBlank()) {
            try {
                UnifiedRecommendation.Severity severityEnum = UnifiedRecommendation.Severity.valueOf(severity.toUpperCase());
                allRecommendations = recommendationService.getRecommendationsBySeverity(instance, severityEnum);
            } catch (IllegalArgumentException e) {
                allRecommendations = recommendationService.getRecommendations(instance);
            }
        } else {
            allRecommendations = recommendationService.getRecommendations(instance);
        }

        Map<UnifiedRecommendation.Severity, Integer> summary = recommendationService.getSummary(instance);

        return recommendations
                .data("instance", instance)
                .data("recommendations", allRecommendations)
                .data("summary", summary)
                .data("sourceFilter", source)
                .data("severityFilter", severity)
                .data("sources", UnifiedRecommendation.Source.values())
                .data("severities", UnifiedRecommendation.Severity.values())
                .data("instances", dataSourceManager.getInstanceInfoList())
                .data("currentInstance", instance)
                .data("securityEnabled", config.security().enabled())
                .data("schemaEnabled", config.schema().enabled())
                .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Runbooks page.
     */
    @GET
    @Path("/runbooks")
    public TemplateInstance getRunbooksPage(@QueryParam("instance") @DefaultValue("default") String instance) {
        if (!featureToggleService.isPageEnabled("runbooks")) {
            throw new NotFoundException("Runbooks page is disabled");
        }

        List<Runbook> allRunbooks = runbookService.getRunbooks(instance);
        List<RunbookExecution> recentExecutions = runbookService.getRecentExecutions(instance, 10);
        List<RunbookExecution> inProgress = runbookService.getInProgressExecutions(instance);

        return runbooks
                .data("instance", instance)
                .data("runbooks", allRunbooks)
                .data("recentExecutions", recentExecutions)
                .data("inProgressExecutions", inProgress)
                .data("categories", Runbook.Category.values())
                .data("instances", dataSourceManager.getInstanceInfoList())
                .data("currentInstance", instance)
                .data("securityEnabled", config.security().enabled())
                .data("schemaEnabled", config.schema().enabled())
                .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Runbook execution page.
     */
    @GET
    @Path("/runbooks/execution/{executionId}")
    public TemplateInstance getRunbookExecutionPage(
            @PathParam("executionId") long executionId,
            @QueryParam("instance") @DefaultValue("default") String instance) {

        if (!featureToggleService.isPageEnabled("runbooks")) {
            throw new NotFoundException("Runbooks page is disabled");
        }

        RunbookExecution execution = runbookService.getExecution(instance, executionId);
        if (execution == null) {
            throw new NotFoundException("Execution not found: " + executionId);
        }

        return runbookExecution
                .data("instance", instance)
                .data("execution", execution)
                .data("instances", dataSourceManager.getInstanceInfoList())
                .data("currentInstance", instance)
                .data("securityEnabled", config.security().enabled())
                .data("schemaEnabled", config.schema().enabled())
                .data("toggles", featureToggleService.getAllToggles());
    }

    // ================================
    // API Endpoints (JSON)
    // ================================

    /**
     * Natural language query endpoint.
     */
    @POST
    @Path("/ask")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response askQuestion(
            @FormParam("query") String query,
            @QueryParam("instance") @DefaultValue("default") String instance) {

        NaturalLanguageQuery result = insightsService.parseNaturalLanguageQuery(query, instance);

        return Response.ok(Map.of(
                "query", result.getQueryText(),
                "understood", result.isUnderstood(),
                "intent", result.getMatchedIntent() != null ? result.getMatchedIntent().name() : "UNKNOWN",
                "explanation", result.getExplanation(),
                "redirectUrl", result.getResolvedTo() != null ? result.getResolvedTo() : "/insights",
                "confidence", result.getConfidenceScore()
        )).build();
    }

    /**
     * Explain a term endpoint.
     */
    @GET
    @Path("/explain/{term}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response explainTerm(@PathParam("term") String term) {
        String explanation = insightsService.explainTerm(term);
        return Response.ok(Map.of("term", term, "explanation", explanation)).build();
    }

    /**
     * Acknowledge an anomaly.
     */
    @POST
    @Path("/anomalies/{anomalyId}/acknowledge")
    @Produces(MediaType.APPLICATION_JSON)
    public Response acknowledgeAnomaly(
            @PathParam("anomalyId") long anomalyId,
            @QueryParam("instance") @DefaultValue("default") String instance,
            @QueryParam("username") @DefaultValue("anonymous") String username) {

        anomalyDetectionService.acknowledgeAnomaly(instance, anomalyId, username);
        return Response.ok(Map.of("status", "acknowledged", "anomalyId", anomalyId)).build();
    }

    /**
     * Resolve an anomaly.
     */
    @POST
    @Path("/anomalies/{anomalyId}/resolve")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response resolveAnomaly(
            @PathParam("anomalyId") long anomalyId,
            @FormParam("notes") String notes,
            @QueryParam("instance") @DefaultValue("default") String instance) {

        anomalyDetectionService.resolveAnomaly(instance, anomalyId, notes);
        return Response.ok(Map.of("status", "resolved", "anomalyId", anomalyId)).build();
    }

    /**
     * Start a runbook execution.
     */
    @POST
    @Path("/runbooks/{runbookId}/start")
    @Produces(MediaType.APPLICATION_JSON)
    public Response startRunbook(
            @PathParam("runbookId") long runbookId,
            @QueryParam("instance") @DefaultValue("default") String instance,
            @QueryParam("username") @DefaultValue("anonymous") String username) {

        RunbookExecution execution = runbookService.startExecution(
                instance, runbookId, RunbookExecution.TriggeredBy.MANUAL, username);

        return Response.ok(Map.of(
                "status", "started",
                "executionId", execution.getId(),
                "redirectUrl", "/insights/runbooks/execution/" + execution.getId() + "?instance=" + instance
        )).build();
    }

    /**
     * Advance runbook execution to next step.
     */
    @POST
    @Path("/runbooks/execution/{executionId}/advance")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response advanceStep(
            @PathParam("executionId") long executionId,
            @FormParam("output") String output,
            @QueryParam("instance") @DefaultValue("default") String instance) {

        RunbookExecution execution = runbookService.advanceStep(instance, executionId, output);

        return Response.ok(Map.of(
                "status", execution.getStatus().name(),
                "currentStep", execution.getCurrentStep(),
                "progressPercent", execution.getProgressPercent()
        )).build();
    }

    /**
     * Skip current runbook step.
     */
    @POST
    @Path("/runbooks/execution/{executionId}/skip")
    @Produces(MediaType.APPLICATION_JSON)
    public Response skipStep(
            @PathParam("executionId") long executionId,
            @QueryParam("instance") @DefaultValue("default") String instance) {

        RunbookExecution execution = runbookService.skipStep(instance, executionId);

        return Response.ok(Map.of(
                "status", execution.getStatus().name(),
                "currentStep", execution.getCurrentStep()
        )).build();
    }

    /**
     * Cancel runbook execution.
     */
    @POST
    @Path("/runbooks/execution/{executionId}/cancel")
    @Produces(MediaType.APPLICATION_JSON)
    public Response cancelExecution(
            @PathParam("executionId") long executionId,
            @QueryParam("instance") @DefaultValue("default") String instance) {

        RunbookExecution execution = runbookService.cancelExecution(instance, executionId);

        return Response.ok(Map.of("status", "cancelled", "executionId", executionId)).build();
    }

    /**
     * Refresh insights (recalculate baselines and forecasts).
     */
    @POST
    @Path("/refresh")
    @Produces(MediaType.APPLICATION_JSON)
    public Response refreshInsights(@QueryParam("instance") @DefaultValue("default") String instance) {
        insightsService.refreshInsights(instance);
        return Response.ok(Map.of("status", "refreshed", "instance", instance)).build();
    }

    /**
     * Dismiss a recommendation.
     */
    @POST
    @Path("/recommendations/{recommendationId}/dismiss")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response dismissRecommendation(
            @PathParam("recommendationId") long recommendationId,
            @FormParam("reason") String reason,
            @QueryParam("instance") @DefaultValue("default") String instance,
            @QueryParam("username") @DefaultValue("anonymous") String username) {

        recommendationService.dismiss(instance, recommendationId, username, reason);
        return Response.ok(Map.of("status", "dismissed", "recommendationId", recommendationId)).build();
    }

    /**
     * Mark recommendation as applied.
     */
    @POST
    @Path("/recommendations/{recommendationId}/apply")
    @Produces(MediaType.APPLICATION_JSON)
    public Response applyRecommendation(
            @PathParam("recommendationId") long recommendationId,
            @QueryParam("instance") @DefaultValue("default") String instance,
            @QueryParam("username") @DefaultValue("anonymous") String username) {

        recommendationService.markApplied(instance, recommendationId, username);
        return Response.ok(Map.of("status", "applied", "recommendationId", recommendationId)).build();
    }
}
