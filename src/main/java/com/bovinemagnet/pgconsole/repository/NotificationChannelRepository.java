package com.bovinemagnet.pgconsole.repository;

import com.bovinemagnet.pgconsole.model.NotificationChannel;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing notification channels.
 * <p>
 * Provides CRUD operations for notification channel configurations
 * stored in the pgconsole.notification_channel table.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class NotificationChannelRepository {

    @Inject
    DataSource dataSource;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Finds all notification channels.
     *
     * @return list of all channels
     */
    public List<NotificationChannel> findAll() {
        String sql = """
            SELECT id, name, channel_type, enabled, config, severity_filter,
                   alert_type_filter, instance_filter, rate_limit_per_hour,
                   created_at, updated_at, last_used_at, test_mode
            FROM pgconsole.notification_channel
            ORDER BY name
            """;

        List<NotificationChannel> channels = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                channels.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch notification channels", e);
        }
        return channels;
    }

    /**
     * Finds all enabled notification channels.
     *
     * @return list of enabled channels
     */
    public List<NotificationChannel> findEnabled() {
        String sql = """
            SELECT id, name, channel_type, enabled, config, severity_filter,
                   alert_type_filter, instance_filter, rate_limit_per_hour,
                   created_at, updated_at, last_used_at, test_mode
            FROM pgconsole.notification_channel
            WHERE enabled = TRUE
            ORDER BY name
            """;

        List<NotificationChannel> channels = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                channels.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch enabled notification channels", e);
        }
        return channels;
    }

    /**
     * Finds a notification channel by ID.
     *
     * @param id channel ID
     * @return optional containing channel if found
     */
    public Optional<NotificationChannel> findById(Long id) {
        String sql = """
            SELECT id, name, channel_type, enabled, config, severity_filter,
                   alert_type_filter, instance_filter, rate_limit_per_hour,
                   created_at, updated_at, last_used_at, test_mode
            FROM pgconsole.notification_channel
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
            throw new RuntimeException("Failed to fetch notification channel by ID", e);
        }
        return Optional.empty();
    }

    /**
     * Finds channels by type.
     *
     * @param channelType the channel type
     * @return list of matching channels
     */
    public List<NotificationChannel> findByType(NotificationChannel.ChannelType channelType) {
        String sql = """
            SELECT id, name, channel_type, enabled, config, severity_filter,
                   alert_type_filter, instance_filter, rate_limit_per_hour,
                   created_at, updated_at, last_used_at, test_mode
            FROM pgconsole.notification_channel
            WHERE channel_type = ? AND enabled = TRUE
            ORDER BY name
            """;

        List<NotificationChannel> channels = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, channelType.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    channels.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch channels by type", e);
        }
        return channels;
    }

    /**
     * Saves a new notification channel.
     *
     * @param channel channel to save
     * @return saved channel with ID
     */
    public NotificationChannel save(NotificationChannel channel) {
        String sql = """
            INSERT INTO pgconsole.notification_channel
                (name, channel_type, enabled, config, severity_filter,
                 alert_type_filter, instance_filter, rate_limit_per_hour, test_mode)
            VALUES (?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?)
            RETURNING id, created_at, updated_at
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, channel.getName());
            stmt.setString(2, channel.getChannelType().name());
            stmt.setBoolean(3, channel.isEnabled());
            stmt.setString(4, serializeConfig(channel));
            stmt.setArray(5, conn.createArrayOf("text",
                channel.getSeverityFilter() != null ? channel.getSeverityFilter().toArray() : new String[0]));
            stmt.setArray(6, conn.createArrayOf("text",
                channel.getAlertTypeFilter() != null ? channel.getAlertTypeFilter().toArray() : new String[0]));
            stmt.setArray(7, conn.createArrayOf("text",
                channel.getInstanceFilter() != null ? channel.getInstanceFilter().toArray() : new String[0]));
            stmt.setInt(8, channel.getRateLimitPerHour());
            stmt.setBoolean(9, channel.isTestMode());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    channel.setId(rs.getLong("id"));
                    channel.setCreatedAt(rs.getTimestamp("created_at").toInstant());
                    channel.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save notification channel", e);
        }
        return channel;
    }

    /**
     * Updates an existing notification channel.
     *
     * @param channel channel to update
     * @return updated channel
     */
    public NotificationChannel update(NotificationChannel channel) {
        String sql = """
            UPDATE pgconsole.notification_channel
            SET name = ?, channel_type = ?, enabled = ?, config = ?::jsonb,
                severity_filter = ?, alert_type_filter = ?, instance_filter = ?,
                rate_limit_per_hour = ?, test_mode = ?, updated_at = NOW()
            WHERE id = ?
            RETURNING updated_at
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, channel.getName());
            stmt.setString(2, channel.getChannelType().name());
            stmt.setBoolean(3, channel.isEnabled());
            stmt.setString(4, serializeConfig(channel));
            stmt.setArray(5, conn.createArrayOf("text",
                channel.getSeverityFilter() != null ? channel.getSeverityFilter().toArray() : new String[0]));
            stmt.setArray(6, conn.createArrayOf("text",
                channel.getAlertTypeFilter() != null ? channel.getAlertTypeFilter().toArray() : new String[0]));
            stmt.setArray(7, conn.createArrayOf("text",
                channel.getInstanceFilter() != null ? channel.getInstanceFilter().toArray() : new String[0]));
            stmt.setInt(8, channel.getRateLimitPerHour());
            stmt.setBoolean(9, channel.isTestMode());
            stmt.setLong(10, channel.getId());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    channel.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update notification channel", e);
        }
        return channel;
    }

    /**
     * Deletes a notification channel.
     *
     * @param id channel ID
     */
    public void delete(Long id) {
        String sql = "DELETE FROM pgconsole.notification_channel WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete notification channel", e);
        }
    }

    /**
     * Updates the last used timestamp for a channel.
     *
     * @param id channel ID
     */
    public void updateLastUsed(Long id) {
        String sql = "UPDATE pgconsole.notification_channel SET last_used_at = NOW() WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update last used timestamp", e);
        }
    }

    private NotificationChannel mapRow(ResultSet rs) throws SQLException {
        NotificationChannel channel = new NotificationChannel();
        channel.setId(rs.getLong("id"));
        channel.setName(rs.getString("name"));
        channel.setChannelType(NotificationChannel.ChannelType.valueOf(rs.getString("channel_type")));
        channel.setEnabled(rs.getBoolean("enabled"));

        // Parse config JSON
        String configJson = rs.getString("config");
        deserializeConfig(channel, configJson);

        // Parse array filters
        Array severityArray = rs.getArray("severity_filter");
        if (severityArray != null) {
            channel.setSeverityFilter(Arrays.asList((String[]) severityArray.getArray()));
        }

        Array alertTypeArray = rs.getArray("alert_type_filter");
        if (alertTypeArray != null) {
            channel.setAlertTypeFilter(Arrays.asList((String[]) alertTypeArray.getArray()));
        }

        Array instanceArray = rs.getArray("instance_filter");
        if (instanceArray != null) {
            channel.setInstanceFilter(Arrays.asList((String[]) instanceArray.getArray()));
        }

        channel.setRateLimitPerHour(rs.getInt("rate_limit_per_hour"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            channel.setCreatedAt(createdAt.toInstant());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            channel.setUpdatedAt(updatedAt.toInstant());
        }

        Timestamp lastUsedAt = rs.getTimestamp("last_used_at");
        if (lastUsedAt != null) {
            channel.setLastUsedAt(lastUsedAt.toInstant());
        }

        channel.setTestMode(rs.getBoolean("test_mode"));

        return channel;
    }

    private String serializeConfig(NotificationChannel channel) {
        try {
            return switch (channel.getChannelType()) {
                case SLACK -> objectMapper.writeValueAsString(channel.getSlackConfig());
                case TEAMS -> objectMapper.writeValueAsString(channel.getTeamsConfig());
                case PAGERDUTY -> objectMapper.writeValueAsString(channel.getPagerDutyConfig());
                case DISCORD -> objectMapper.writeValueAsString(channel.getDiscordConfig());
                case EMAIL -> objectMapper.writeValueAsString(channel.getEmailConfig());
            };
        } catch (Exception e) {
            return "{}";
        }
    }

    private void deserializeConfig(NotificationChannel channel, String configJson) {
        if (configJson == null || configJson.isBlank()) {
            return;
        }
        try {
            switch (channel.getChannelType()) {
                case SLACK -> channel.setSlackConfig(
                    objectMapper.readValue(configJson, NotificationChannel.SlackConfig.class));
                case TEAMS -> channel.setTeamsConfig(
                    objectMapper.readValue(configJson, NotificationChannel.TeamsConfig.class));
                case PAGERDUTY -> channel.setPagerDutyConfig(
                    objectMapper.readValue(configJson, NotificationChannel.PagerDutyConfig.class));
                case DISCORD -> channel.setDiscordConfig(
                    objectMapper.readValue(configJson, NotificationChannel.DiscordConfig.class));
                case EMAIL -> channel.setEmailConfig(
                    objectMapper.readValue(configJson, NotificationChannel.EmailConfig.class));
            }
        } catch (Exception e) {
            // Log and continue with default config
        }
    }
}
