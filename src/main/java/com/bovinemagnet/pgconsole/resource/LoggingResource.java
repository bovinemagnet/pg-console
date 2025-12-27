package com.bovinemagnet.pgconsole.resource;

import com.bovinemagnet.pgconsole.config.LoggingConfig;
import com.bovinemagnet.pgconsole.logging.LogLevelManager;
import com.bovinemagnet.pgconsole.logging.StructuredLogger;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * REST resource for runtime log level management.
 * <p>
 * Provides endpoints for:
 * <ul>
 *   <li>Viewing current log configuration</li>
 *   <li>Adjusting log levels at runtime</li>
 *   <li>Enabling temporary debug mode</li>
 *   <li>Applying log level presets</li>
 * </ul>
 * <p>
 * All endpoints require admin role when security is enabled.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@Path("/api/v1/logging")
@Produces(MediaType.APPLICATION_JSON)
public class LoggingResource {

    @Inject
    LogLevelManager logLevelManager;

    @Inject
    LoggingConfig loggingConfig;

    @Inject
    StructuredLogger structuredLogger;

    /**
     * Gets the current logging configuration.
     *
     * @return logging configuration summary
     */
    @GET
    @Path("/config")
    public Response getLoggingConfig() {
        Map<String, Object> response = new HashMap<>();
        response.put("format", loggingConfig.format());
        response.put("levels", logLevelManager.getLogConfiguration());
        response.put("temporaryLevels", formatExpiryMap(logLevelManager.getTemporaryLevelExpiry()));
        response.put("sqlLoggingEnabled", loggingConfig.sqlEnabled());
        response.put("sqlSlowThresholdMs", loggingConfig.sqlSlowThresholdMs());
        response.put("redactionEnabled", loggingConfig.redactEnabled());
        response.put("asyncLoggingEnabled", loggingConfig.asyncEnabled());
        response.put("fileLoggingEnabled", loggingConfig.fileEnabled());

        if (loggingConfig.fileEnabled()) {
            response.put("logFilePath", loggingConfig.filePath());
        }

        structuredLogger.info("LOGGING", "Logging configuration requested");
        return Response.ok(response).build();
    }

    /**
     * Gets the current log level for a specific logger.
     *
     * @param loggerName logger name/category
     * @return current log level
     */
    @GET
    @Path("/level/{loggerName}")
    public Response getLogLevel(@PathParam("loggerName") String loggerName) {
        String level = logLevelManager.getLogLevel(loggerName);

        Map<String, Object> response = new HashMap<>();
        response.put("logger", loggerName);
        response.put("level", level);

        return Response.ok(response).build();
    }

    /**
     * Sets the log level for a specific logger.
     * <p>
     * Requires admin role when security is enabled.
     *
     * @param loggerName logger name/category
     * @param request log level request
     * @return result of the operation
     */
    @PUT
    @Path("/level/{loggerName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("admin")
    public Response setLogLevel(@PathParam("loggerName") String loggerName,
                                LogLevelRequest request) {

        boolean success;
        if (request.durationMinutes != null && request.durationMinutes > 0) {
            // Set temporary level
            success = logLevelManager.setTemporaryLogLevel(
                loggerName,
                request.level,
                Duration.ofMinutes(request.durationMinutes)
            );
        } else {
            // Set permanent level
            success = logLevelManager.setLogLevel(loggerName, request.level);
        }

        structuredLogger.logAudit("SET_LOG_LEVEL", loggerName, "admin",
            success ? "success" : "failed");

        Map<String, Object> response = new HashMap<>();
        response.put("logger", loggerName);
        response.put("level", request.level);
        response.put("success", success);
        response.put("temporary", request.durationMinutes != null && request.durationMinutes > 0);

        if (request.durationMinutes != null && request.durationMinutes > 0) {
            response.put("expiresAt", Instant.now().plus(Duration.ofMinutes(request.durationMinutes)).toString());
        }

        return success ?
            Response.ok(response).build() :
            Response.status(Response.Status.BAD_REQUEST).entity(response).build();
    }

    /**
     * Reverts a logger to its original level.
     *
     * @param loggerName logger name/category
     * @return result of the operation
     */
    @DELETE
    @Path("/level/{loggerName}")
    @RolesAllowed("admin")
    public Response revertLogLevel(@PathParam("loggerName") String loggerName) {
        logLevelManager.revertLogLevel(loggerName);

        structuredLogger.logAudit("REVERT_LOG_LEVEL", loggerName, "admin", "success");

        Map<String, Object> response = new HashMap<>();
        response.put("logger", loggerName);
        response.put("level", logLevelManager.getLogLevel(loggerName));
        response.put("reverted", true);

        return Response.ok(response).build();
    }

    /**
     * Applies a log level preset.
     *
     * @param preset preset name (MINIMAL, STANDARD, VERBOSE, DEBUG)
     * @param loggerName optional logger name (applies to root if not specified)
     * @return result of the operation
     */
    @POST
    @Path("/preset/{preset}")
    @RolesAllowed("admin")
    public Response applyPreset(@PathParam("preset") String preset,
                                @QueryParam("logger") String loggerName) {
        try {
            LogLevelManager.LogPreset logPreset = LogLevelManager.LogPreset.valueOf(preset.toUpperCase());

            if (loggerName != null && !loggerName.isBlank()) {
                logLevelManager.applyPreset(loggerName, logPreset);
            } else {
                logLevelManager.applyPreset(logPreset);
            }

            structuredLogger.logAudit("APPLY_LOG_PRESET", preset, "admin", "success");

            Map<String, Object> response = new HashMap<>();
            response.put("preset", preset);
            response.put("logger", loggerName != null ? loggerName : "ROOT");
            response.put("level", logPreset.getLevel().getName());
            response.put("success", true);

            return Response.ok(response).build();
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Invalid preset: " + preset);
            response.put("validPresets", new String[]{"MINIMAL", "STANDARD", "VERBOSE", "DEBUG"});
            return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
        }
    }

    /**
     * Enables debug mode temporarily.
     *
     * @param durationMinutes duration in minutes (default 15)
     * @return result of the operation
     */
    @POST
    @Path("/debug")
    @RolesAllowed("admin")
    public Response enableDebugMode(@QueryParam("duration") @DefaultValue("15") int durationMinutes) {
        Duration duration = Duration.ofMinutes(durationMinutes);
        logLevelManager.enableDebugMode(duration);

        structuredLogger.logAudit("ENABLE_DEBUG_MODE", String.valueOf(durationMinutes) + "min", "admin", "success");

        Map<String, Object> response = new HashMap<>();
        response.put("debugMode", true);
        response.put("durationMinutes", durationMinutes);
        response.put("expiresAt", Instant.now().plus(duration).toString());

        return Response.ok(response).build();
    }

    /**
     * Disables debug mode.
     *
     * @return result of the operation
     */
    @DELETE
    @Path("/debug")
    @RolesAllowed("admin")
    public Response disableDebugMode() {
        logLevelManager.disableDebugMode();

        structuredLogger.logAudit("DISABLE_DEBUG_MODE", "", "admin", "success");

        Map<String, Object> response = new HashMap<>();
        response.put("debugMode", false);
        response.put("message", "Debug mode disabled");

        return Response.ok(response).build();
    }

    /**
     * Gets available log presets.
     *
     * @return list of available presets
     */
    @GET
    @Path("/presets")
    public Response getPresets() {
        Map<String, String> presets = new HashMap<>();
        for (LogLevelManager.LogPreset preset : LogLevelManager.LogPreset.values()) {
            presets.put(preset.name(), preset.getLevel().getName());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("presets", presets);
        response.put("descriptions", Map.of(
            "MINIMAL", "Errors and critical warnings only",
            "STANDARD", "Informational messages and above",
            "VERBOSE", "Includes debug information",
            "DEBUG", "All available detail"
        ));

        return Response.ok(response).build();
    }

    /**
     * Formats expiry map for JSON response.
     */
    private Map<String, String> formatExpiryMap(Map<String, Instant> expiryMap) {
        Map<String, String> formatted = new HashMap<>();
        for (Map.Entry<String, Instant> entry : expiryMap.entrySet()) {
            formatted.put(entry.getKey(), entry.getValue().toString());
        }
        return formatted;
    }

    /**
     * Request body for setting log levels.
     */
    public static class LogLevelRequest {
        public String level;
        public Integer durationMinutes;
    }
}
