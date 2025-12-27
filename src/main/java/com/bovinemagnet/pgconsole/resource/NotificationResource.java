package com.bovinemagnet.pgconsole.resource;

import com.bovinemagnet.pgconsole.config.InstanceConfig;
import com.bovinemagnet.pgconsole.model.ActiveAlert;
import com.bovinemagnet.pgconsole.model.AlertSilence;
import com.bovinemagnet.pgconsole.model.EscalationPolicy;
import com.bovinemagnet.pgconsole.model.MaintenanceWindow;
import com.bovinemagnet.pgconsole.model.NotificationChannel;
import com.bovinemagnet.pgconsole.model.NotificationResult;
import com.bovinemagnet.pgconsole.repository.NotificationChannelRepository;
import com.bovinemagnet.pgconsole.repository.NotificationHistoryRepository;
import com.bovinemagnet.pgconsole.service.DataSourceManager;
import com.bovinemagnet.pgconsole.service.FeatureToggleService;
import com.bovinemagnet.pgconsole.service.notification.AlertManagementService;
import com.bovinemagnet.pgconsole.service.notification.EscalationService;
import com.bovinemagnet.pgconsole.service.notification.NotificationDispatcher;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Resource for managing notifications, alerts, silences, and maintenance windows.
 * <p>
 * Provides both web UI and REST API endpoints for the notification system.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@Path("/notifications")
public class NotificationResource {

    private static final Logger LOG = Logger.getLogger(NotificationResource.class);

    @Inject
    DataSourceManager dataSourceManager;

    @Inject
    NotificationChannelRepository channelRepository;

    @Inject
    NotificationHistoryRepository historyRepository;

    @Inject
    NotificationDispatcher dispatcher;

    @Inject
    AlertManagementService alertService;

    @Inject
    EscalationService escalationService;

    @Inject
    FeatureToggleService featureToggleService;

    @Inject
    InstanceConfig config;

    @Inject
    Template notifications;

    @Inject
    Template notificationChannels;

    @Inject
    Template notificationHistory;

    @Inject
    Template alerts;

    @Inject
    Template silences;

    @Inject
    Template maintenanceWindows;

    @Inject
    Template escalationPolicies;

    // ===== Web UI Endpoints =====

    /**
     * Main notifications dashboard.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance dashboard(@QueryParam("instance") String instance) {
        AlertManagementService.AlertStats stats = alertService.getAlertStats();
        NotificationHistoryRepository.NotificationStats notifStats = dispatcher.getStats(24);

        return notifications.data("currentInstance", instance)
            .data("instances", dataSourceManager.getInstanceInfoList())
            .data("alertStats", stats)
            .data("notificationStats", notifStats)
            .data("activeAlerts", alertService.getActiveAlerts())
            .data("activeSilences", alertService.getActiveSilences())
            .data("activeMaintenanceWindows", alertService.getActiveMaintenanceWindows())
            .data("recentHistory", dispatcher.getRecentHistory(10))
            .data("title", "Notifications")
            .data("securityEnabled", config.security().enabled())
            .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Notification channels management page.
     */
    @GET
    @Path("/channels")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance channelsPage(@QueryParam("instance") String instance) {
        return notificationChannels.data("currentInstance", instance)
            .data("instances", dataSourceManager.getInstanceInfoList())
            .data("channels", channelRepository.findAll())
            .data("channelTypes", NotificationChannel.ChannelType.values())
            .data("title", "Notification Channels")
            .data("securityEnabled", config.security().enabled())
            .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Notification history page.
     */
    @GET
    @Path("/history")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance historyPage(
            @QueryParam("instance") String instance,
            @QueryParam("limit") @DefaultValue("100") int limit) {

        return notificationHistory.data("currentInstance", instance)
            .data("instances", dataSourceManager.getInstanceInfoList())
            .data("history", historyRepository.findRecent(limit))
            .data("stats", dispatcher.getStats(24))
            .data("title", "Notification History")
            .data("securityEnabled", config.security().enabled())
            .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Active alerts page.
     */
    @GET
    @Path("/alerts")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance alertsPage(@QueryParam("instance") String instance) {
        return alerts.data("currentInstance", instance)
            .data("instances", dataSourceManager.getInstanceInfoList())
            .data("activeAlerts", alertService.getActiveAlerts())
            .data("recentAlerts", alertService.getRecentAlerts(50))
            .data("stats", alertService.getAlertStats())
            .data("title", "Alerts")
            .data("securityEnabled", config.security().enabled())
            .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Silences management page.
     */
    @GET
    @Path("/silences")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance silencesPage(@QueryParam("instance") String instance) {
        return silences.data("currentInstance", instance)
            .data("instances", dataSourceManager.getInstanceInfoList())
            .data("silences", alertService.getAllSilences())
            .data("activeSilences", alertService.getActiveSilences())
            .data("title", "Alert Silences")
            .data("securityEnabled", config.security().enabled())
            .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Maintenance windows management page.
     */
    @GET
    @Path("/maintenance")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance maintenanceWindowsPage(@QueryParam("instance") String instance) {
        return maintenanceWindows.data("currentInstance", instance)
            .data("instances", dataSourceManager.getInstanceInfoList())
            .data("windows", alertService.getAllMaintenanceWindows())
            .data("activeWindows", alertService.getActiveMaintenanceWindows())
            .data("upcomingWindows", alertService.getUpcomingMaintenanceWindows())
            .data("title", "Maintenance Windows")
            .data("securityEnabled", config.security().enabled())
            .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Escalation policies management page.
     */
    @GET
    @Path("/escalation")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance escalationPoliciesPage(@QueryParam("instance") String instance) {
        return escalationPolicies.data("currentInstance", instance)
            .data("instances", dataSourceManager.getInstanceInfoList())
            .data("policies", escalationService.listPolicies())
            .data("channels", channelRepository.findEnabled())
            .data("title", "Escalation Policies")
            .data("securityEnabled", config.security().enabled())
            .data("toggles", featureToggleService.getAllToggles());
    }

    // ===== REST API Endpoints =====

    // --- Channels API ---

    @GET
    @Path("/api/channels")
    @Produces(MediaType.APPLICATION_JSON)
    public List<NotificationChannel> listChannels() {
        return channelRepository.findAll();
    }

    @GET
    @Path("/api/channels/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChannel(@PathParam("id") Long id) {
        return channelRepository.findById(id)
            .map(channel -> Response.ok(channel).build())
            .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    @Path("/api/channels")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createChannel(NotificationChannel channel) {
        if (!dispatcher.validateChannelConfig(channel)) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Invalid channel configuration")
                .build();
        }

        NotificationChannel saved = channelRepository.save(channel);
        return Response.status(Response.Status.CREATED).entity(saved).build();
    }

    @PUT
    @Path("/api/channels/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateChannel(@PathParam("id") Long id, NotificationChannel channel) {
        if (!channelRepository.findById(id).isPresent()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        channel.setId(id);
        NotificationChannel updated = channelRepository.update(channel);
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/api/channels/{id}")
    public Response deleteChannel(@PathParam("id") Long id) {
        channelRepository.delete(id);
        return Response.noContent().build();
    }

    @POST
    @Path("/api/channels/{id}/test")
    @Produces(MediaType.APPLICATION_JSON)
    public Response testChannel(@PathParam("id") Long id) {
        NotificationResult result = dispatcher.testChannel(id);
        if (result.isSuccess()) {
            return Response.ok(result).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity(result).build();
        }
    }

    // --- Alerts API ---

    @GET
    @Path("/api/alerts")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ActiveAlert> listAlerts(
            @QueryParam("active") @DefaultValue("true") boolean activeOnly) {
        return activeOnly ? alertService.getActiveAlerts() : alertService.getRecentAlerts(100);
    }

    @GET
    @Path("/api/alerts/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAlert(@PathParam("id") Long id) {
        return alertService.getAlert(id)
            .map(alert -> Response.ok(alert).build())
            .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    @Path("/api/alerts")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response fireAlert(FireAlertRequest request) {
        ActiveAlert alert = alertService.fireAlert(
            request.alertType(),
            request.severity(),
            request.message(),
            request.instanceName(),
            request.escalationPolicyId()
        );
        return Response.status(Response.Status.CREATED).entity(alert).build();
    }

    @POST
    @Path("/api/alerts/{id}/acknowledge")
    public Response acknowledgeAlert(@PathParam("id") Long id) {
        alertService.acknowledgeAlert(id, null);
        return Response.ok().build();
    }

    @POST
    @Path("/api/alerts/{id}/resolve")
    @Produces(MediaType.APPLICATION_JSON)
    public Response resolveAlert(
            @PathParam("id") Long id,
            @QueryParam("sendResolution") @DefaultValue("true") boolean sendResolution) {
        List<NotificationResult> results = alertService.resolveAlert(id, null, sendResolution);
        return Response.ok(results).build();
    }

    // --- Silences API ---

    @GET
    @Path("/api/silences")
    @Produces(MediaType.APPLICATION_JSON)
    public List<AlertSilence> listSilences(
            @QueryParam("active") @DefaultValue("false") boolean activeOnly) {
        return activeOnly ? alertService.getActiveSilences() : alertService.getAllSilences();
    }

    @POST
    @Path("/api/silences")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createSilence(AlertSilence silence) {
        AlertSilence saved = alertService.createSilence(silence);
        return Response.status(Response.Status.CREATED).entity(saved).build();
    }

    @POST
    @Path("/api/silences/quick")
    @Produces(MediaType.APPLICATION_JSON)
    public Response createQuickSilence(
            @QueryParam("alertType") String alertType,
            @QueryParam("instanceName") String instanceName,
            @QueryParam("durationMinutes") @DefaultValue("60") int durationMinutes) {
        AlertSilence silence = alertService.createQuickSilence(
            alertType, instanceName, durationMinutes, null);
        return Response.status(Response.Status.CREATED).entity(silence).build();
    }

    @POST
    @Path("/api/silences/{id}/expire")
    public Response expireSilence(@PathParam("id") Long id) {
        alertService.expireSilence(id);
        return Response.ok().build();
    }

    @DELETE
    @Path("/api/silences/{id}")
    public Response deleteSilence(@PathParam("id") Long id) {
        alertService.deleteSilence(id);
        return Response.noContent().build();
    }

    // --- Maintenance Windows API ---

    @GET
    @Path("/api/maintenance")
    @Produces(MediaType.APPLICATION_JSON)
    public List<MaintenanceWindow> listMaintenanceWindows(
            @QueryParam("active") @DefaultValue("false") boolean activeOnly) {
        return activeOnly ? alertService.getActiveMaintenanceWindows()
                          : alertService.getAllMaintenanceWindows();
    }

    @POST
    @Path("/api/maintenance")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createMaintenanceWindow(MaintenanceWindow window) {
        MaintenanceWindow saved = alertService.createMaintenanceWindow(window);
        return Response.status(Response.Status.CREATED).entity(saved).build();
    }

    @PUT
    @Path("/api/maintenance/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateMaintenanceWindow(@PathParam("id") Long id, MaintenanceWindow window) {
        if (!alertService.getMaintenanceWindow(id).isPresent()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        window.setId(id);
        MaintenanceWindow updated = alertService.updateMaintenanceWindow(window);
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/api/maintenance/{id}")
    public Response deleteMaintenanceWindow(@PathParam("id") Long id) {
        alertService.deleteMaintenanceWindow(id);
        return Response.noContent().build();
    }

    // --- Escalation Policies API ---

    @GET
    @Path("/api/escalation")
    @Produces(MediaType.APPLICATION_JSON)
    public List<EscalationPolicy> listEscalationPolicies() {
        return escalationService.listPolicies();
    }

    @GET
    @Path("/api/escalation/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEscalationPolicy(@PathParam("id") Long id) {
        EscalationPolicy policy = escalationService.getPolicy(id);
        if (policy == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(policy).build();
    }

    @POST
    @Path("/api/escalation")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createEscalationPolicy(EscalationPolicy policy) {
        EscalationPolicy saved = escalationService.createPolicy(policy);
        return Response.status(Response.Status.CREATED).entity(saved).build();
    }

    @PUT
    @Path("/api/escalation/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateEscalationPolicy(@PathParam("id") Long id, EscalationPolicy policy) {
        if (escalationService.getPolicy(id) == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        policy.setId(id);
        EscalationPolicy updated = escalationService.updatePolicy(policy);
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/api/escalation/{id}")
    public Response deleteEscalationPolicy(@PathParam("id") Long id) {
        escalationService.deletePolicy(id);
        return Response.noContent().build();
    }

    // --- Statistics API ---

    @GET
    @Path("/api/stats")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStats() {
        return Response.ok()
            .entity(new StatsResponse(
                alertService.getAlertStats(),
                dispatcher.getStats(24)
            ))
            .build();
    }

    // --- Request/Response Records ---

    public record FireAlertRequest(
        String alertType,
        String severity,
        String message,
        String instanceName,
        Long escalationPolicyId
    ) {}

    public record StatsResponse(
        AlertManagementService.AlertStats alertStats,
        NotificationHistoryRepository.NotificationStats notificationStats
    ) {}
}
