package com.bovinemagnet.pgconsole.repository;

import com.bovinemagnet.pgconsole.config.MetadataDataSource;
import com.bovinemagnet.pgconsole.model.StopwatchSession;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for persisting and retrieving stopwatch sessions.
 * <p>
 * Provides CRUD operations against the {@code pgconsole.stopwatch_session} table,
 * storing before/after metric snapshots as JSONB columns for detailed comparison.
 * Supports multi-instance monitoring via the {@code instance_id} column.
 * <p>
 * All database access uses plain JDBC with {@link PreparedStatement} for SQL injection
 * prevention, following the project's convention of avoiding ORM frameworks.
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see StopwatchSession
 * @see com.bovinemagnet.pgconsole.service.StopwatchService
 */
@ApplicationScoped
public class StopwatchRepository {

    @Inject
    @MetadataDataSource
    DataSource dataSource;

    /**
     * Creates a new stopwatch session in the running state.
     * <p>
     * Inserts a new row with status "running", the current timestamp as
     * {@code started_at}, and the provided start snapshot and top queries
     * as JSONB columns.
     *
     * @param instanceId      the PostgreSQL instance identifier
     * @param startSnapshot   JSON-serialised metrics snapshot at session start
     * @param topQueriesStart JSON-serialised top queries at session start
     * @return the generated session id
     * @throws RuntimeException if the database insert fails
     */
    public long createSession(String instanceId, String startSnapshot, String topQueriesStart) {
        String sql = """
            INSERT INTO pgconsole.stopwatch_session (
                instance_id, status, started_at, start_snapshot,
                top_queries_start, created_at
            ) VALUES (?, 'running', NOW(), ?::jsonb, ?::jsonb, NOW())
            RETURNING id
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, instanceId);
            stmt.setString(2, startSnapshot);
            stmt.setString(3, topQueriesStart);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                throw new RuntimeException("Failed to retrieve generated id for stopwatch session");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create stopwatch session for " + instanceId, e);
        }
    }

    /**
     * Stops an active stopwatch session by recording the end snapshot.
     * <p>
     * Updates the session status to "stopped", sets {@code stopped_at} to the
     * current timestamp, and stores the end snapshot and top queries.
     *
     * @param id              the session id to stop
     * @param endSnapshot     JSON-serialised metrics snapshot at session stop
     * @param topQueriesEnd   JSON-serialised top queries at session stop
     * @throws RuntimeException if the database update fails
     */
    public void stopSession(long id, String endSnapshot, String topQueriesEnd) {
        String sql = """
            UPDATE pgconsole.stopwatch_session
            SET status = 'stopped',
                stopped_at = NOW(),
                end_snapshot = ?::jsonb,
                top_queries_end = ?::jsonb
            WHERE id = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, endSnapshot);
            stmt.setString(2, topQueriesEnd);
            stmt.setLong(3, id);

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to stop stopwatch session " + id, e);
        }
    }

    /**
     * Cancels an active stopwatch session without recording an end snapshot.
     * <p>
     * Updates the session status to "cancelled" and sets {@code stopped_at}
     * to the current timestamp.
     *
     * @param id the session id to cancel
     * @throws RuntimeException if the database update fails
     */
    public void cancelSession(long id) {
        String sql = """
            UPDATE pgconsole.stopwatch_session
            SET status = 'cancelled',
                stopped_at = NOW()
            WHERE id = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to cancel stopwatch session " + id, e);
        }
    }

    /**
     * Retrieves a stopwatch session by its unique identifier.
     *
     * @param id the session id
     * @return the session, or null if not found
     * @throws RuntimeException if the database query fails
     */
    public StopwatchSession getSession(long id) {
        String sql = """
            SELECT id, instance_id, status, started_at, stopped_at, notes,
                   start_snapshot::text, end_snapshot::text,
                   top_queries_start::text, top_queries_end::text, created_at
            FROM pgconsole.stopwatch_session
            WHERE id = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve stopwatch session " + id, e);
        }
    }

    /**
     * Retrieves the currently active (running) stopwatch session for an instance.
     * <p>
     * Only one session per instance may be in the "running" state at any time.
     *
     * @param instanceId the PostgreSQL instance identifier
     * @return the active session, or null if no session is running
     * @throws RuntimeException if the database query fails
     */
    public StopwatchSession getActiveSession(String instanceId) {
        String sql = """
            SELECT id, instance_id, status, started_at, stopped_at, notes,
                   start_snapshot::text, end_snapshot::text,
                   top_queries_start::text, top_queries_end::text, created_at
            FROM pgconsole.stopwatch_session
            WHERE status = 'running' AND instance_id = ?
            ORDER BY started_at DESC
            LIMIT 1
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, instanceId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve active stopwatch session for " + instanceId, e);
        }
    }

    /**
     * Retrieves recent stopwatch sessions for an instance, ordered by start time descending.
     *
     * @param instanceId the PostgreSQL instance identifier
     * @param limit      the maximum number of sessions to return
     * @return the list of recent sessions, may be empty
     * @throws RuntimeException if the database query fails
     */
    public List<StopwatchSession> getRecentSessions(String instanceId, int limit) {
        String sql = """
            SELECT id, instance_id, status, started_at, stopped_at, notes,
                   start_snapshot::text, end_snapshot::text,
                   top_queries_start::text, top_queries_end::text, created_at
            FROM pgconsole.stopwatch_session
            WHERE instance_id = ?
            ORDER BY started_at DESC
            LIMIT ?
            """;

        List<StopwatchSession> sessions = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, instanceId);
            stmt.setInt(2, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve recent stopwatch sessions for " + instanceId, e);
        }

        return sessions;
    }

    /**
     * Updates the notes field on an existing stopwatch session.
     *
     * @param id    the session id
     * @param notes the new notes text
     * @throws RuntimeException if the database update fails
     */
    public void updateNotes(long id, String notes) {
        String sql = """
            UPDATE pgconsole.stopwatch_session
            SET notes = ?
            WHERE id = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, notes);
            stmt.setLong(2, id);

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update notes for stopwatch session " + id, e);
        }
    }

    /**
     * Maps a ResultSet row to a StopwatchSession object.
     *
     * @param rs the result set positioned at the current row
     * @return the mapped session
     * @throws SQLException if a column read fails
     */
    private StopwatchSession mapRow(ResultSet rs) throws SQLException {
        StopwatchSession session = new StopwatchSession();
        session.setId(rs.getLong("id"));
        session.setInstanceId(rs.getString("instance_id"));
        session.setStatus(rs.getString("status"));

        Timestamp startedAt = rs.getTimestamp("started_at");
        session.setStartedAt(startedAt != null ? startedAt.toInstant() : null);

        Timestamp stoppedAt = rs.getTimestamp("stopped_at");
        session.setStoppedAt(stoppedAt != null ? stoppedAt.toInstant() : null);

        session.setNotes(rs.getString("notes"));
        session.setStartSnapshot(rs.getString("start_snapshot"));
        session.setEndSnapshot(rs.getString("end_snapshot"));
        session.setTopQueriesStart(rs.getString("top_queries_start"));
        session.setTopQueriesEnd(rs.getString("top_queries_end"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        session.setCreatedAt(createdAt != null ? createdAt.toInstant() : null);

        return session;
    }
}
