package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.config.MetadataDataSource;
import com.bovinemagnet.pgconsole.model.AuditLog;
import com.bovinemagnet.pgconsole.model.AuditLog.Action;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for audit logging of admin actions.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class AuditService {

    private static final Logger LOG = Logger.getLogger(AuditService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject
    @MetadataDataSource
    DataSource dataSource;

    /**
     * Records an administrative action to the audit log.
     * <p>
     * Writes a comprehensive audit record to the pgconsole.audit_log table including
     * user identity, action type, target information, optional details as JSON,
     * client IP address, and success/failure status.
     *
     * @param instanceId the database instance identifier where the action occurred
     * @param username the username who performed the action
     * @param action the type of action performed
     * @param targetType the type of target affected (e.g., "query", "connection", "bookmark")
     * @param targetId the identifier of the target (e.g., PID, query ID, bookmark ID)
     * @param details optional additional details as a map; will be serialised to JSON; may be null
     * @param clientIp the client IP address from which the action originated
     * @param success true if the action succeeded, false if it failed
     * @param errorMessage error message if the action failed; null if successful
     * @see Action
     */
    public void log(String instanceId, String username, Action action,
                    String targetType, String targetId, Map<String, Object> details,
                    String clientIp, boolean success, String errorMessage) {

        String sql = """
            INSERT INTO pgconsole.audit_log
            (timestamp, instance_id, username, action, target_type, target_id, details, client_ip, success, error_message)
            VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?)
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.from(Instant.now()));
            stmt.setString(2, instanceId);
            stmt.setString(3, username);
            stmt.setString(4, action.name());
            stmt.setString(5, targetType);
            stmt.setString(6, targetId);

            if (details != null) {
                stmt.setString(7, MAPPER.writeValueAsString(details));
            } else {
                stmt.setNull(7, Types.OTHER);
            }

            stmt.setString(8, clientIp);
            stmt.setBoolean(9, success);
            stmt.setString(10, errorMessage);

            stmt.executeUpdate();

            LOG.infof("Audit: %s by %s on %s/%s - %s",
                    action.name(), username, targetType, targetId, success ? "SUCCESS" : "FAILED");

        } catch (SQLException | JsonProcessingException e) {
            LOG.errorf("Failed to write audit log: %s", e.getMessage());
        }
    }

    /**
     * Records a successful administrative action to the audit log.
     * <p>
     * This is a convenience method that calls {@link #log} with success=true,
     * no details, and no error message.
     *
     * @param instanceId the database instance identifier
     * @param username the username who performed the action
     * @param action the type of action performed
     * @param targetType the type of target affected
     * @param targetId the identifier of the target
     * @param clientIp the client IP address
     * @see #log
     */
    public void logSuccess(String instanceId, String username, Action action,
                           String targetType, String targetId, String clientIp) {
        log(instanceId, username, action, targetType, targetId, null, clientIp, true, null);
    }

    /**
     * Records a successful administrative action with additional details to the audit log.
     * <p>
     * This is a convenience method that calls {@link #log} with success=true
     * and includes custom details as a JSON object.
     *
     * @param instanceId the database instance identifier
     * @param username the username who performed the action
     * @param action the type of action performed
     * @param targetType the type of target affected
     * @param targetId the identifier of the target
     * @param details additional context information to store as JSON
     * @param clientIp the client IP address
     * @see #log
     */
    public void logSuccess(String instanceId, String username, Action action,
                           String targetType, String targetId, Map<String, Object> details, String clientIp) {
        log(instanceId, username, action, targetType, targetId, details, clientIp, true, null);
    }

    /**
     * Records a failed administrative action to the audit log.
     * <p>
     * This is a convenience method that calls {@link #log} with success=false
     * and includes an error message.
     *
     * @param instanceId the database instance identifier
     * @param username the username who performed the action
     * @param action the type of action attempted
     * @param targetType the type of target affected
     * @param targetId the identifier of the target
     * @param clientIp the client IP address
     * @param errorMessage description of why the action failed
     * @see #log
     */
    public void logFailure(String instanceId, String username, Action action,
                           String targetType, String targetId, String clientIp, String errorMessage) {
        log(instanceId, username, action, targetType, targetId, null, clientIp, false, errorMessage);
    }

    /**
     * Retrieves the most recent audit log entries across all instances.
     * <p>
     * Returns audit records ordered by timestamp descending, limited to the specified count.
     *
     * @param limit maximum number of audit log entries to return
     * @return list of recent audit log entries
     * @see #getLogs(String, String, String, int)
     */
    public List<AuditLog> getRecentLogs(int limit) {
        return getLogs(null, null, null, limit);
    }

    /**
     * Retrieves audit log entries for a specific database instance.
     * <p>
     * Returns audit records for the specified instance, ordered by timestamp descending.
     *
     * @param instanceId the database instance identifier to filter by
     * @param limit maximum number of audit log entries to return
     * @return list of audit log entries for the specified instance
     * @see #getLogs(String, String, String, int)
     */
    public List<AuditLog> getLogsForInstance(String instanceId, int limit) {
        return getLogs(instanceId, null, null, limit);
    }

    /**
     * Retrieves audit log entries with optional filters.
     * <p>
     * Allows filtering by instance ID, action type, and username. Any filter parameter
     * may be null to skip that filter. Results are ordered by timestamp descending.
     *
     * @param instanceId the database instance identifier to filter by; null for all instances
     * @param action the action type name to filter by; null for all actions
     * @param username the username to filter by; null for all users
     * @param limit maximum number of audit log entries to return
     * @return list of filtered audit log entries
     */
    public List<AuditLog> getLogs(String instanceId, String action, String username, int limit) {
        List<AuditLog> logs = new ArrayList<>();

        StringBuilder sql = new StringBuilder("""
            SELECT id, timestamp, instance_id, username, action, target_type, target_id,
                   details, client_ip, success, error_message
            FROM pgconsole.audit_log
            WHERE 1=1
            """);

        List<Object> params = new ArrayList<>();

        if (instanceId != null && !instanceId.isEmpty()) {
            sql.append(" AND instance_id = ?");
            params.add(instanceId);
        }
        if (action != null && !action.isEmpty()) {
            sql.append(" AND action = ?");
            params.add(action);
        }
        if (username != null && !username.isEmpty()) {
            sql.append(" AND username = ?");
            params.add(username);
        }

        sql.append(" ORDER BY timestamp DESC LIMIT ?");
        params.add(limit);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AuditLog log = new AuditLog();
                    log.setId(rs.getLong("id"));
                    log.setTimestamp(rs.getTimestamp("timestamp").toInstant());
                    log.setInstanceId(rs.getString("instance_id"));
                    log.setUsername(rs.getString("username"));
                    log.setAction(rs.getString("action"));
                    log.setTargetType(rs.getString("target_type"));
                    log.setTargetId(rs.getString("target_id"));

                    String detailsJson = rs.getString("details");
                    if (detailsJson != null) {
                        try {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> details = MAPPER.readValue(detailsJson, HashMap.class);
                            log.setDetails(details);
                        } catch (JsonProcessingException e) {
                            LOG.debugf("Failed to parse audit log details: %s", e.getMessage());
                        }
                    }

                    log.setClientIp(rs.getString("client_ip"));
                    log.setSuccess(rs.getBoolean("success"));
                    log.setErrorMessage(rs.getString("error_message"));

                    logs.add(log);
                }
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to get audit logs: %s", e.getMessage());
        }

        return logs;
    }

    /**
     * Retrieves summary statistics for audit log activity.
     * <p>
     * Provides aggregated metrics including total actions in the last 24 hours and 7 days,
     * failure counts, and unique user counts. Useful for dashboard displays and monitoring.
     *
     * @return audit summary statistics with counts and metrics
     */
    public AuditSummary getSummary() {
        AuditSummary summary = new AuditSummary();

        String sql = """
            SELECT
                (SELECT COUNT(*) FROM pgconsole.audit_log WHERE timestamp > NOW() - INTERVAL '24 hours') as last_24h,
                (SELECT COUNT(*) FROM pgconsole.audit_log WHERE timestamp > NOW() - INTERVAL '7 days') as last_7d,
                (SELECT COUNT(*) FROM pgconsole.audit_log WHERE success = FALSE AND timestamp > NOW() - INTERVAL '24 hours') as failures_24h,
                (SELECT COUNT(DISTINCT username) FROM pgconsole.audit_log WHERE timestamp > NOW() - INTERVAL '24 hours') as unique_users_24h
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                summary.setActionsLast24h(rs.getInt("last_24h"));
                summary.setActionsLast7d(rs.getInt("last_7d"));
                summary.setFailuresLast24h(rs.getInt("failures_24h"));
                summary.setUniqueUsersLast24h(rs.getInt("unique_users_24h"));
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to get audit summary: %s", e.getMessage());
        }

        return summary;
    }

    /**
     * Container for audit log summary statistics.
     * <p>
     * Provides aggregated counts of audit log activity over different time periods,
     * including total actions, failures, and unique users.
     */
    public static class AuditSummary {
        private int actionsLast24h;
        private int actionsLast7d;
        private int failuresLast24h;
        private int uniqueUsersLast24h;

        public int getActionsLast24h() { return actionsLast24h; }
        public void setActionsLast24h(int value) { this.actionsLast24h = value; }

        public int getActionsLast7d() { return actionsLast7d; }
        public void setActionsLast7d(int value) { this.actionsLast7d = value; }

        public int getFailuresLast24h() { return failuresLast24h; }
        public void setFailuresLast24h(int value) { this.failuresLast24h = value; }

        public int getUniqueUsersLast24h() { return uniqueUsersLast24h; }
        public void setUniqueUsersLast24h(int value) { this.uniqueUsersLast24h = value; }
    }
}
