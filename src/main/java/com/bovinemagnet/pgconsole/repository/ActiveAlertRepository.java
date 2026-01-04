package com.bovinemagnet.pgconsole.repository;

import com.bovinemagnet.pgconsole.config.MetadataDataSource;
import com.bovinemagnet.pgconsole.model.ActiveAlert;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing active alerts.
 * <p>
 * Provides CRUD operations for active alert tracking and escalation management.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class ActiveAlertRepository {

    @Inject
    @MetadataDataSource
    DataSource dataSource;

    /**
     * Finds all active (non-resolved) alerts.
     *
     * @return list of active alerts
     */
    public List<ActiveAlert> findActive() {
        String sql = """
            SELECT id, alert_id, alert_type, alert_severity, alert_message, instance_name,
                   fired_at, last_notification_at, current_escalation_tier, escalation_policy_id,
                   acknowledged, resolved, resolved_at, metadata
            FROM pgconsole.active_alert
            WHERE resolved = FALSE
            ORDER BY fired_at DESC
            """;

        List<ActiveAlert> alerts = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                alerts.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch active alerts", e);
        }
        return alerts;
    }

    /**
     * Finds all alerts (including resolved).
     *
     * @param limit maximum number of alerts to return
     * @return list of alerts
     */
    public List<ActiveAlert> findAll(int limit) {
        String sql = """
            SELECT id, alert_id, alert_type, alert_severity, alert_message, instance_name,
                   fired_at, last_notification_at, current_escalation_tier, escalation_policy_id,
                   acknowledged, resolved, resolved_at, metadata
            FROM pgconsole.active_alert
            ORDER BY fired_at DESC
            LIMIT ?
            """;

        List<ActiveAlert> alerts = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    alerts.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch alerts", e);
        }
        return alerts;
    }

    /**
     * Finds alerts due for escalation.
     *
     * @return list of alerts needing escalation
     */
    public List<ActiveAlert> findDueForEscalation() {
        String sql = """
            SELECT a.id, a.alert_id, a.alert_type, a.alert_severity, a.alert_message, a.instance_name,
                   a.fired_at, a.last_notification_at, a.current_escalation_tier, a.escalation_policy_id,
                   a.acknowledged, a.resolved, a.resolved_at, a.metadata
            FROM pgconsole.active_alert a
            JOIN pgconsole.escalation_policy p ON p.id = a.escalation_policy_id
            JOIN pgconsole.escalation_tier t ON t.policy_id = p.id
                 AND t.tier_order = a.current_escalation_tier + 1
            WHERE a.resolved = FALSE
              AND a.acknowledged = FALSE
              AND a.last_notification_at + (t.delay_minutes * INTERVAL '1 minute') <= NOW()
            ORDER BY a.last_notification_at
            """;

        List<ActiveAlert> alerts = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                alerts.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch alerts due for escalation", e);
        }
        return alerts;
    }

    /**
     * Finds an alert by ID.
     *
     * @param id alert ID
     * @return optional containing alert if found
     */
    public Optional<ActiveAlert> findById(Long id) {
        String sql = """
            SELECT id, alert_id, alert_type, alert_severity, alert_message, instance_name,
                   fired_at, last_notification_at, current_escalation_tier, escalation_policy_id,
                   acknowledged, resolved, resolved_at, metadata
            FROM pgconsole.active_alert
            WHERE id = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch alert by ID", e);
        }
        return Optional.empty();
    }

    /**
     * Finds an alert by alert ID.
     *
     * @param alertId alert ID string
     * @return optional containing alert if found
     */
    public Optional<ActiveAlert> findByAlertId(String alertId) {
        String sql = """
            SELECT id, alert_id, alert_type, alert_severity, alert_message, instance_name,
                   fired_at, last_notification_at, current_escalation_tier, escalation_policy_id,
                   acknowledged, resolved, resolved_at, metadata
            FROM pgconsole.active_alert
            WHERE alert_id = ? AND resolved = FALSE
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, alertId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch alert by alert ID", e);
        }
        return Optional.empty();
    }

    /**
     * Saves a new active alert.
     *
     * @param alert alert to save
     * @return saved alert with ID
     */
    public ActiveAlert save(ActiveAlert alert) {
        String sql = """
            INSERT INTO pgconsole.active_alert
                (alert_id, alert_type, alert_severity, alert_message, instance_name,
                 fired_at, last_notification_at, current_escalation_tier, escalation_policy_id,
                 acknowledged, resolved, metadata)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
            RETURNING id
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, alert.getAlertId());
            stmt.setString(2, alert.getAlertType());
            stmt.setString(3, alert.getAlertSeverity());
            stmt.setString(4, alert.getAlertMessage());
            stmt.setString(5, alert.getInstanceName());
            stmt.setTimestamp(6, alert.getFiredAt() != null ?
                Timestamp.from(alert.getFiredAt()) : Timestamp.from(Instant.now()));
            stmt.setTimestamp(7, alert.getLastNotificationAt() != null ?
                Timestamp.from(alert.getLastNotificationAt()) : null);
            stmt.setInt(8, alert.getCurrentEscalationTier());
            stmt.setObject(9, alert.getEscalationPolicyId());
            stmt.setBoolean(10, alert.isAcknowledged());
            stmt.setBoolean(11, alert.isResolved());
            stmt.setString(12, alert.getMetadata());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    alert.setId(rs.getLong("id"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save active alert", e);
        }
        return alert;
    }

    /**
     * Updates an existing alert.
     *
     * @param alert alert to update
     * @return updated alert
     */
    public ActiveAlert update(ActiveAlert alert) {
        String sql = """
            UPDATE pgconsole.active_alert
            SET last_notification_at = ?, current_escalation_tier = ?,
                acknowledged = ?, resolved = ?, resolved_at = ?, metadata = ?::jsonb
            WHERE id = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, alert.getLastNotificationAt() != null ?
                Timestamp.from(alert.getLastNotificationAt()) : null);
            stmt.setInt(2, alert.getCurrentEscalationTier());
            stmt.setBoolean(3, alert.isAcknowledged());
            stmt.setBoolean(4, alert.isResolved());
            stmt.setTimestamp(5, alert.getResolvedAt() != null ?
                Timestamp.from(alert.getResolvedAt()) : null);
            stmt.setString(6, alert.getMetadata());
            stmt.setLong(7, alert.getId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update active alert", e);
        }
        return alert;
    }

    /**
     * Acknowledges an alert.
     *
     * @param id alert ID
     */
    public void acknowledge(Long id) {
        String sql = "UPDATE pgconsole.active_alert SET acknowledged = TRUE WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to acknowledge alert", e);
        }
    }

    /**
     * Resolves an alert.
     *
     * @param id alert ID
     */
    public void resolve(Long id) {
        String sql = """
            UPDATE pgconsole.active_alert
            SET resolved = TRUE, resolved_at = NOW()
            WHERE id = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to resolve alert", e);
        }
    }

    /**
     * Updates escalation tier for an alert.
     *
     * @param id alert ID
     * @param newTier new escalation tier
     */
    public void updateEscalationTier(Long id, int newTier) {
        String sql = """
            UPDATE pgconsole.active_alert
            SET current_escalation_tier = ?, last_notification_at = NOW()
            WHERE id = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, newTier);
            stmt.setLong(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update escalation tier", e);
        }
    }

    /**
     * Deletes resolved alerts older than the specified number of days.
     *
     * @param days number of days to retain
     * @return number of deleted alerts
     */
    public int deleteOlderThan(int days) {
        String sql = """
            DELETE FROM pgconsole.active_alert
            WHERE resolved = TRUE AND resolved_at < NOW() - INTERVAL '1 day' * ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, days);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete old alerts", e);
        }
    }

    private ActiveAlert mapRow(ResultSet rs) throws SQLException {
        ActiveAlert alert = new ActiveAlert();
        alert.setId(rs.getLong("id"));
        alert.setAlertId(rs.getString("alert_id"));
        alert.setAlertType(rs.getString("alert_type"));
        alert.setAlertSeverity(rs.getString("alert_severity"));
        alert.setAlertMessage(rs.getString("alert_message"));
        alert.setInstanceName(rs.getString("instance_name"));

        Timestamp firedAt = rs.getTimestamp("fired_at");
        if (firedAt != null) {
            alert.setFiredAt(firedAt.toInstant());
        }

        Timestamp lastNotificationAt = rs.getTimestamp("last_notification_at");
        if (lastNotificationAt != null) {
            alert.setLastNotificationAt(lastNotificationAt.toInstant());
        }

        alert.setCurrentEscalationTier(rs.getInt("current_escalation_tier"));
        alert.setEscalationPolicyId(rs.getObject("escalation_policy_id", Long.class));
        alert.setAcknowledged(rs.getBoolean("acknowledged"));
        alert.setResolved(rs.getBoolean("resolved"));

        Timestamp resolvedAt = rs.getTimestamp("resolved_at");
        if (resolvedAt != null) {
            alert.setResolvedAt(resolvedAt.toInstant());
        }

        alert.setMetadata(rs.getString("metadata"));

        return alert;
    }
}
