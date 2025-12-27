package com.bovinemagnet.pgconsole.repository;

import com.bovinemagnet.pgconsole.model.NotificationChannel;
import com.bovinemagnet.pgconsole.model.NotificationResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing notification history.
 * <p>
 * Provides operations for logging and querying notification history.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class NotificationHistoryRepository {

    @Inject
    DataSource dataSource;

    /**
     * Saves a notification result to history.
     *
     * @param result notification result to save
     * @return saved result with ID
     */
    public NotificationResult save(NotificationResult result) {
        String sql = """
            INSERT INTO pgconsole.notification_history
                (channel_id, channel_name, channel_type, alert_id, alert_type, alert_severity,
                 alert_message, instance_name, sent_at, success, response_code, response_body,
                 error_message, escalation_tier, dedup_key)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, result.getChannelId());
            stmt.setString(2, result.getChannelName());
            stmt.setString(3, result.getChannelType() != null ? result.getChannelType().name() : null);
            stmt.setString(4, result.getAlertId());
            stmt.setString(5, result.getAlertType());
            stmt.setString(6, result.getAlertSeverity());
            stmt.setString(7, result.getAlertMessage());
            stmt.setString(8, result.getInstanceName());
            stmt.setTimestamp(9, Timestamp.from(result.getSentAt()));
            stmt.setBoolean(10, result.isSuccess());
            stmt.setInt(11, result.getResponseCode());
            stmt.setString(12, result.getResponseBody());
            stmt.setString(13, result.getErrorMessage());
            stmt.setObject(14, result.getEscalationTier());
            stmt.setString(15, result.getDedupKey());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    result.setId(rs.getLong("id"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save notification history", e);
        }
        return result;
    }

    /**
     * Finds recent notification history.
     *
     * @param limit maximum number of results
     * @return list of recent notifications
     */
    public List<NotificationResult> findRecent(int limit) {
        String sql = """
            SELECT id, channel_id, channel_name, channel_type, alert_id, alert_type, alert_severity,
                   alert_message, instance_name, sent_at, success, response_code, response_body,
                   error_message, escalation_tier, dedup_key
            FROM pgconsole.notification_history
            ORDER BY sent_at DESC
            LIMIT ?
            """;

        List<NotificationResult> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch notification history", e);
        }
        return results;
    }

    /**
     * Finds notification history for a specific channel.
     *
     * @param channelId channel ID
     * @param limit maximum number of results
     * @return list of notifications for the channel
     */
    public List<NotificationResult> findByChannelId(Long channelId, int limit) {
        String sql = """
            SELECT id, channel_id, channel_name, channel_type, alert_id, alert_type, alert_severity,
                   alert_message, instance_name, sent_at, success, response_code, response_body,
                   error_message, escalation_tier, dedup_key
            FROM pgconsole.notification_history
            WHERE channel_id = ?
            ORDER BY sent_at DESC
            LIMIT ?
            """;

        List<NotificationResult> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, channelId);
            stmt.setInt(2, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch notification history by channel", e);
        }
        return results;
    }

    /**
     * Finds notification history for a specific alert.
     *
     * @param alertId alert ID
     * @return list of notifications for the alert
     */
    public List<NotificationResult> findByAlertId(String alertId) {
        String sql = """
            SELECT id, channel_id, channel_name, channel_type, alert_id, alert_type, alert_severity,
                   alert_message, instance_name, sent_at, success, response_code, response_body,
                   error_message, escalation_tier, dedup_key
            FROM pgconsole.notification_history
            WHERE alert_id = ?
            ORDER BY sent_at DESC
            """;

        List<NotificationResult> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, alertId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch notification history by alert", e);
        }
        return results;
    }

    /**
     * Gets notification counts for the last hour.
     *
     * @param channelId channel ID
     * @return count of notifications sent
     */
    public int getCountLastHour(Long channelId) {
        String sql = """
            SELECT COUNT(*) FROM pgconsole.notification_history
            WHERE channel_id = ? AND sent_at > NOW() - INTERVAL '1 hour'
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, channelId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get notification count", e);
        }
        return 0;
    }

    /**
     * Finds failed notifications for retry.
     *
     * @param limit maximum number of results
     * @return list of failed notifications
     */
    public List<NotificationResult> findFailedForRetry(int limit) {
        String sql = """
            SELECT id, channel_id, channel_name, channel_type, alert_id, alert_type, alert_severity,
                   alert_message, instance_name, sent_at, success, response_code, response_body,
                   error_message, escalation_tier, dedup_key
            FROM pgconsole.notification_history
            WHERE success = FALSE AND sent_at > NOW() - INTERVAL '1 hour'
            ORDER BY sent_at
            LIMIT ?
            """;

        List<NotificationResult> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch failed notifications", e);
        }
        return results;
    }

    /**
     * Gets statistics for a time period.
     *
     * @param hours number of hours to look back
     * @return statistics map
     */
    public NotificationStats getStats(int hours) {
        String sql = """
            SELECT
                COUNT(*) as total,
                COUNT(*) FILTER (WHERE success = TRUE) as success_count,
                COUNT(*) FILTER (WHERE success = FALSE) as failure_count,
                COUNT(DISTINCT channel_id) as channels_used,
                COUNT(DISTINCT alert_id) as alerts_notified
            FROM pgconsole.notification_history
            WHERE sent_at > NOW() - INTERVAL '1 hour' * ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, hours);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new NotificationStats(
                        rs.getInt("total"),
                        rs.getInt("success_count"),
                        rs.getInt("failure_count"),
                        rs.getInt("channels_used"),
                        rs.getInt("alerts_notified")
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get notification stats", e);
        }
        return new NotificationStats(0, 0, 0, 0, 0);
    }

    /**
     * Deletes old notification history.
     *
     * @param days number of days to retain
     * @return number of deleted records
     */
    public int deleteOlderThan(int days) {
        String sql = """
            DELETE FROM pgconsole.notification_history
            WHERE sent_at < NOW() - INTERVAL '1 day' * ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, days);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete old notification history", e);
        }
    }

    private NotificationResult mapRow(ResultSet rs) throws SQLException {
        NotificationResult result = new NotificationResult();
        result.setId(rs.getLong("id"));
        result.setChannelId(rs.getObject("channel_id", Long.class));
        result.setChannelName(rs.getString("channel_name"));

        String channelType = rs.getString("channel_type");
        if (channelType != null) {
            result.setChannelType(NotificationChannel.ChannelType.valueOf(channelType));
        }

        result.setAlertId(rs.getString("alert_id"));
        result.setAlertType(rs.getString("alert_type"));
        result.setAlertSeverity(rs.getString("alert_severity"));
        result.setAlertMessage(rs.getString("alert_message"));
        result.setInstanceName(rs.getString("instance_name"));

        Timestamp sentAt = rs.getTimestamp("sent_at");
        if (sentAt != null) {
            result.setSentAt(sentAt.toInstant());
        }

        result.setSuccess(rs.getBoolean("success"));
        result.setResponseCode(rs.getInt("response_code"));
        result.setResponseBody(rs.getString("response_body"));
        result.setErrorMessage(rs.getString("error_message"));
        result.setEscalationTier(rs.getObject("escalation_tier", Integer.class));
        result.setDedupKey(rs.getString("dedup_key"));

        return result;
    }

    /**
     * Statistics for notifications.
     */
    public record NotificationStats(
        int total,
        int successCount,
        int failureCount,
        int channelsUsed,
        int alertsNotified
    ) {
        public double getSuccessRate() {
            return total > 0 ? (double) successCount / total * 100 : 0;
        }
    }
}
