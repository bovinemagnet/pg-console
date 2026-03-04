package com.bovinemagnet.pgconsole.resource;

import com.bovinemagnet.pgconsole.config.InstanceConfig;
import com.bovinemagnet.pgconsole.model.StopwatchSession;
import com.bovinemagnet.pgconsole.service.FeatureToggleService;
import com.bovinemagnet.pgconsole.service.StopwatchService;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API resource for the stopwatch feature.
 * <p>
 * Provides JSON endpoints for starting, stopping, and cancelling stopwatch
 * sessions, as well as querying active and recent sessions. These endpoints
 * are consumed by htmx interactions on the stopwatch dashboard page.
 * <p>
 * All endpoints require the "stopwatch" feature toggle to be enabled and
 * accept an {@code instance} query parameter for multi-instance support.
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see StopwatchService
 * @see StopwatchSession
 */
@Path("/api/stopwatch")
@Produces(MediaType.APPLICATION_JSON)
public class StopwatchApiResource {

    @Inject
    InstanceConfig config;

    @Inject
    StopwatchService stopwatchService;

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
    // Session Lifecycle Endpoints
    // ========================================

    /**
     * Starts a new stopwatch session for the specified instance.
     * <p>
     * Captures the current metrics snapshot and creates a session in the
     * "running" state. Returns 409 Conflict if a session is already running.
     *
     * @param instance the PostgreSQL instance name
     * @return the created session as JSON, or 409 if already active
     */
    @POST
    @Path("/start")
    public Response startSession(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("stopwatch");

        String instanceName = resolveInstance(instance);

        try {
            StopwatchSession session = stopwatchService.startSession(instanceName);

            Map<String, Object> result = new HashMap<>();
            result.put("id", session.getId());
            result.put("status", session.getStatus());
            result.put("startedAt", session.getStartedAt() != null ? session.getStartedAt().toString() : null);
            result.put("instanceId", session.getInstanceId());

            return Response.ok(result).build();
        } catch (IllegalStateException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return Response.status(Response.Status.CONFLICT).entity(error).build();
        }
    }

    /**
     * Stops the active stopwatch session for the specified instance.
     * <p>
     * Captures the current metrics snapshot and updates the session to
     * "stopped". Returns 409 Conflict if no session is running.
     *
     * @param instance the PostgreSQL instance name
     * @return the stopped session as JSON, or 409 if no active session
     */
    @POST
    @Path("/stop")
    public Response stopSession(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("stopwatch");

        String instanceName = resolveInstance(instance);

        try {
            StopwatchSession session = stopwatchService.stopSession(instanceName);

            Map<String, Object> result = new HashMap<>();
            result.put("id", session.getId());
            result.put("status", session.getStatus());
            result.put("startedAt", session.getStartedAt() != null ? session.getStartedAt().toString() : null);
            result.put("stoppedAt", session.getStoppedAt() != null ? session.getStoppedAt().toString() : null);
            result.put("elapsedSeconds", session.getElapsedSeconds());
            result.put("elapsedDisplay", session.getElapsedDisplay());
            result.put("instanceId", session.getInstanceId());

            return Response.ok(result).build();
        } catch (IllegalStateException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return Response.status(Response.Status.CONFLICT).entity(error).build();
        }
    }

    /**
     * Cancels the active stopwatch session for the specified instance.
     * <p>
     * Sets the session status to "cancelled" without capturing an end snapshot.
     * Returns 409 Conflict if no session is running.
     *
     * @param instance the PostgreSQL instance name
     * @return the cancelled session as JSON, or 409 if no active session
     */
    @POST
    @Path("/cancel")
    public Response cancelSession(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("stopwatch");

        String instanceName = resolveInstance(instance);

        try {
            StopwatchSession session = stopwatchService.cancelSession(instanceName);

            Map<String, Object> result = new HashMap<>();
            result.put("id", session.getId());
            result.put("status", session.getStatus());
            result.put("instanceId", session.getInstanceId());

            return Response.ok(result).build();
        } catch (IllegalStateException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return Response.status(Response.Status.CONFLICT).entity(error).build();
        }
    }

    // ========================================
    // Session Query Endpoints
    // ========================================

    /**
     * Returns the active stopwatch session for the specified instance.
     * <p>
     * Returns 204 No Content if no session is currently running.
     *
     * @param instance the PostgreSQL instance name
     * @return the active session as JSON, or 204 if none
     */
    @GET
    @Path("/active")
    public Response getActiveSession(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("stopwatch");

        String instanceName = resolveInstance(instance);
        StopwatchSession session = stopwatchService.getActiveSession(instanceName);

        if (session == null) {
            return Response.noContent().build();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", session.getId());
        result.put("status", session.getStatus());
        result.put("startedAt", session.getStartedAt() != null ? session.getStartedAt().toString() : null);
        result.put("elapsedSeconds", session.getElapsedSeconds());
        result.put("elapsedDisplay", session.getElapsedDisplay());
        result.put("instanceId", session.getInstanceId());

        return Response.ok(result).build();
    }

    /**
     * Returns recent stopwatch sessions for the specified instance.
     *
     * @param instance the PostgreSQL instance name
     * @return the list of recent sessions as JSON
     */
    @GET
    @Path("/recent")
    public List<StopwatchSession> getRecentSessions(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("stopwatch");

        String instanceName = resolveInstance(instance);
        return stopwatchService.getRecentSessions(instanceName);
    }

    /**
     * Returns the current stopwatch status for the specified instance.
     * <p>
     * Provides a summary of the active session (if any) suitable for
     * polling by the dashboard timer.
     *
     * @param instance the PostgreSQL instance name
     * @return status map with active session details or null
     */
    @GET
    @Path("/status")
    public Map<String, Object> getStatus(
            @QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("stopwatch");

        String instanceName = resolveInstance(instance);
        StopwatchSession session = stopwatchService.getActiveSession(instanceName);

        Map<String, Object> result = new HashMap<>();
        if (session != null) {
            result.put("active", true);
            result.put("id", session.getId());
            result.put("startedAt", session.getStartedAt() != null ? session.getStartedAt().toString() : null);
            result.put("elapsedSeconds", session.getElapsedSeconds());
            result.put("elapsedDisplay", session.getElapsedDisplay());
        } else {
            result.put("active", false);
        }

        return result;
    }
}
