package com.bovinemagnet.pgconsole.service;

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
    DataSource dataSource;

    /**
     * Logs an admin action to the audit log.
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
     * Logs a successful action.
     */
    public void logSuccess(String instanceId, String username, Action action,
                           String targetType, String targetId, String clientIp) {
        log(instanceId, username, action, targetType, targetId, null, clientIp, true, null);
    }

    /**
     * Logs a successful action with details.
     */
    public void logSuccess(String instanceId, String username, Action action,
                           String targetType, String targetId, Map<String, Object> details, String clientIp) {
        log(instanceId, username, action, targetType, targetId, details, clientIp, true, null);
    }

    /**
     * Logs a failed action.
     */
    public void logFailure(String instanceId, String username, Action action,
                           String targetType, String targetId, String clientIp, String errorMessage) {
        log(instanceId, username, action, targetType, targetId, null, clientIp, false, errorMessage);
    }

    /**
     * Gets recent audit log entries.
     */
    public List<AuditLog> getRecentLogs(int limit) {
        return getLogs(null, null, null, limit);
    }

    /**
     * Gets audit log entries for a specific instance.
     */
    public List<AuditLog> getLogsForInstance(String instanceId, int limit) {
        return getLogs(instanceId, null, null, limit);
    }

    /**
     * Gets audit log entries with filters.
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
     * Gets summary statistics for audit logs.
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
     * Summary statistics for audit logs.
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
