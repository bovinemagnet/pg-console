package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.MetricsSnapshot;
import com.bovinemagnet.pgconsole.model.StopwatchSession;
import com.bovinemagnet.pgconsole.repository.StopwatchRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Business logic service for the stopwatch feature.
 * <p>
 * Coordinates the lifecycle of stopwatch sessions, including capturing
 * point-in-time metrics snapshots via the {@link MetricsHistoryBridgeService},
 * serialising them to JSON, and persisting sessions via the
 * {@link StopwatchRepository}.
 * <p>
 * The service enforces the constraint that only one session per instance
 * may be in the "running" state at any given time. Attempting to start a
 * new session whilst one is already active will result in an
 * {@link IllegalStateException}.
 * <p>
 * JSON serialisation of metric snapshots and top queries uses Jackson's
 * {@link ObjectMapper}. Serialisation failures are logged but do not prevent
 * session creation, storing null for the affected JSON column.
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see StopwatchSession
 * @see StopwatchRepository
 * @see MetricsHistoryBridgeService
 */
@ApplicationScoped
public class StopwatchService {

    private static final Logger LOG = Logger.getLogger(StopwatchService.class);

    /** Maximum number of recent sessions returned by {@link #getRecentSessions(String)}. */
    private static final int RECENT_SESSION_LIMIT = 20;

    @Inject
    StopwatchRepository stopwatchRepository;

    @Inject
    MetricsHistoryBridgeService metricsHistoryBridgeService;

    @Inject
    ObjectMapper objectMapper;

    /**
     * Starts a new stopwatch session for the specified instance.
     * <p>
     * Captures the current metrics snapshot and top queries, serialises them
     * to JSON, and creates a new session in the "running" state.
     *
     * @param instanceName the PostgreSQL instance identifier
     * @return the newly created session
     * @throws IllegalStateException if an active session already exists for this instance
     */
    public StopwatchSession startSession(String instanceName) {
        StopwatchSession existing = stopwatchRepository.getActiveSession(instanceName);
        if (existing != null) {
            throw new IllegalStateException(
                    "An active stopwatch session already exists for instance '" + instanceName
                    + "' (id=" + existing.getId() + "). Stop or cancel it first.");
        }

        MetricsSnapshot snapshot = metricsHistoryBridgeService.captureSnapshot(instanceName);

        String snapshotJson = serialiseToJson(snapshot, "start snapshot");
        String topQueriesJson = serialiseToJson(snapshot.getTopQueries(), "start top queries");

        long id = stopwatchRepository.createSession(instanceName, snapshotJson, topQueriesJson);
        return stopwatchRepository.getSession(id);
    }

    /**
     * Stops the active stopwatch session for the specified instance.
     * <p>
     * Captures the current metrics snapshot and top queries, serialises them
     * to JSON, and updates the session to "stopped" with the end snapshot.
     *
     * @param instanceName the PostgreSQL instance identifier
     * @return the stopped session with end snapshot populated
     * @throws IllegalStateException if no active session exists for this instance
     */
    public StopwatchSession stopSession(String instanceName) {
        StopwatchSession active = stopwatchRepository.getActiveSession(instanceName);
        if (active == null) {
            throw new IllegalStateException(
                    "No active stopwatch session exists for instance '" + instanceName + "'.");
        }

        MetricsSnapshot snapshot = metricsHistoryBridgeService.captureSnapshot(instanceName);

        String snapshotJson = serialiseToJson(snapshot, "end snapshot");
        String topQueriesJson = serialiseToJson(snapshot.getTopQueries(), "end top queries");

        stopwatchRepository.stopSession(active.getId(), snapshotJson, topQueriesJson);
        return stopwatchRepository.getSession(active.getId());
    }

    /**
     * Cancels the active stopwatch session for the specified instance.
     * <p>
     * Sets the session status to "cancelled" without capturing an end snapshot.
     *
     * @param instanceName the PostgreSQL instance identifier
     * @return the cancelled session
     * @throws IllegalStateException if no active session exists for this instance
     */
    public StopwatchSession cancelSession(String instanceName) {
        StopwatchSession active = stopwatchRepository.getActiveSession(instanceName);
        if (active == null) {
            throw new IllegalStateException(
                    "No active stopwatch session exists for instance '" + instanceName + "'.");
        }

        stopwatchRepository.cancelSession(active.getId());
        return stopwatchRepository.getSession(active.getId());
    }

    /**
     * Retrieves the currently active stopwatch session for an instance.
     *
     * @param instanceName the PostgreSQL instance identifier
     * @return the active session, or null if none is running
     */
    public StopwatchSession getActiveSession(String instanceName) {
        return stopwatchRepository.getActiveSession(instanceName);
    }

    /**
     * Retrieves a stopwatch session by its unique identifier.
     *
     * @param id the session id
     * @return the session, or null if not found
     */
    public StopwatchSession getSession(long id) {
        return stopwatchRepository.getSession(id);
    }

    /**
     * Retrieves the most recent stopwatch sessions for an instance.
     * <p>
     * Returns up to {@value #RECENT_SESSION_LIMIT} sessions, ordered by
     * start time descending.
     *
     * @param instanceName the PostgreSQL instance identifier
     * @return the list of recent sessions, may be empty
     */
    public List<StopwatchSession> getRecentSessions(String instanceName) {
        return stopwatchRepository.getRecentSessions(instanceName, RECENT_SESSION_LIMIT);
    }

    /**
     * Updates the notes field on an existing stopwatch session.
     *
     * @param id    the session id
     * @param notes the new notes text
     */
    public void updateNotes(long id, String notes) {
        stopwatchRepository.updateNotes(id, notes);
    }

    /**
     * Serialises an object to JSON using the injected ObjectMapper.
     * <p>
     * On failure, logs a warning and returns null rather than propagating
     * the exception, allowing the session to be created/stopped even if
     * JSON serialisation fails for a particular component.
     *
     * @param value       the object to serialise
     * @param description a human-readable description for logging
     * @return the JSON string, or null if serialisation fails
     */
    private String serialiseToJson(Object value, String description) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            LOG.warn("Failed to serialise " + description + " to JSON", e);
            return null;
        }
    }
}
