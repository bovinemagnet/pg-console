package com.bovinemagnet.pgconsole.repository;

import com.bovinemagnet.pgconsole.model.AlertSilence;
import com.bovinemagnet.pgconsole.model.AlertSilence.Matcher;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing alert silences.
 * <p>
 * Provides CRUD operations for alert silence rules.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class AlertSilenceRepository {

    @Inject
    DataSource dataSource;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Finds all alert silences.
     *
     * @return list of all silences
     */
    public List<AlertSilence> findAll() {
        String sql = """
            SELECT id, name, description, matchers, start_time, end_time,
                   created_by, created_at
            FROM pgconsole.alert_silence
            ORDER BY end_time DESC
            """;

        List<AlertSilence> silences = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                silences.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch alert silences", e);
        }
        return silences;
    }

    /**
     * Finds all currently active silences.
     *
     * @return list of active silences
     */
    public List<AlertSilence> findActive() {
        String sql = """
            SELECT id, name, description, matchers, start_time, end_time,
                   created_by, created_at
            FROM pgconsole.alert_silence
            WHERE start_time <= NOW() AND end_time > NOW()
            ORDER BY end_time
            """;

        List<AlertSilence> silences = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                silences.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch active alert silences", e);
        }
        return silences;
    }

    /**
     * Finds a silence by ID.
     *
     * @param id silence ID
     * @return optional containing silence if found
     */
    public Optional<AlertSilence> findById(Long id) {
        String sql = """
            SELECT id, name, description, matchers, start_time, end_time,
                   created_by, created_at
            FROM pgconsole.alert_silence
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
            throw new RuntimeException("Failed to fetch alert silence by ID", e);
        }
        return Optional.empty();
    }

    /**
     * Saves a new alert silence.
     *
     * @param silence silence to save
     * @return saved silence with ID
     */
    public AlertSilence save(AlertSilence silence) {
        String sql = """
            INSERT INTO pgconsole.alert_silence
                (name, description, matchers, start_time, end_time, created_by)
            VALUES (?, ?, ?::jsonb, ?, ?, ?)
            RETURNING id, created_at
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, silence.getName());
            stmt.setString(2, silence.getDescription());
            stmt.setString(3, serializeMatchers(silence.getMatchers()));
            stmt.setTimestamp(4, Timestamp.from(silence.getStartTime()));
            stmt.setTimestamp(5, Timestamp.from(silence.getEndTime()));
            stmt.setString(6, silence.getCreatedBy());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    silence.setId(rs.getLong("id"));
                    silence.setCreatedAt(rs.getTimestamp("created_at").toInstant());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save alert silence", e);
        }
        return silence;
    }

    /**
     * Updates an existing silence.
     *
     * @param silence silence to update
     * @return updated silence
     */
    public AlertSilence update(AlertSilence silence) {
        String sql = """
            UPDATE pgconsole.alert_silence
            SET name = ?, description = ?, matchers = ?::jsonb,
                start_time = ?, end_time = ?
            WHERE id = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, silence.getName());
            stmt.setString(2, silence.getDescription());
            stmt.setString(3, serializeMatchers(silence.getMatchers()));
            stmt.setTimestamp(4, Timestamp.from(silence.getStartTime()));
            stmt.setTimestamp(5, Timestamp.from(silence.getEndTime()));
            stmt.setLong(6, silence.getId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update alert silence", e);
        }
        return silence;
    }

    /**
     * Deletes a silence.
     *
     * @param id silence ID
     */
    public void delete(Long id) {
        String sql = "DELETE FROM pgconsole.alert_silence WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete alert silence", e);
        }
    }

    /**
     * Expires a silence immediately.
     *
     * @param id silence ID
     */
    public void expire(Long id) {
        String sql = "UPDATE pgconsole.alert_silence SET end_time = NOW() WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to expire alert silence", e);
        }
    }

    /**
     * Checks if an alert is currently silenced.
     *
     * @param alertType alert type
     * @param alertSeverity alert severity
     * @param instanceName instance name
     * @param alertMessage alert message
     * @return true if alert is silenced
     */
    public boolean isSilenced(String alertType, String alertSeverity,
                              String instanceName, String alertMessage) {
        List<AlertSilence> activeSilences = findActive();
        for (AlertSilence silence : activeSilences) {
            if (silence.matches(alertType, alertSeverity, instanceName, alertMessage)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Deletes expired silences older than the specified number of days.
     *
     * @param days number of days to retain
     * @return number of deleted silences
     */
    public int deleteExpiredOlderThan(int days) {
        String sql = """
            DELETE FROM pgconsole.alert_silence
            WHERE end_time < NOW() - INTERVAL '1 day' * ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, days);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete expired silences", e);
        }
    }

    private AlertSilence mapRow(ResultSet rs) throws SQLException {
        AlertSilence silence = new AlertSilence();
        silence.setId(rs.getLong("id"));
        silence.setName(rs.getString("name"));
        silence.setDescription(rs.getString("description"));

        String matchersJson = rs.getString("matchers");
        if (matchersJson != null && !matchersJson.isBlank()) {
            silence.setMatchers(deserializeMatchers(matchersJson));
        }

        Timestamp startTime = rs.getTimestamp("start_time");
        if (startTime != null) {
            silence.setStartTime(startTime.toInstant());
        }

        Timestamp endTime = rs.getTimestamp("end_time");
        if (endTime != null) {
            silence.setEndTime(endTime.toInstant());
        }

        silence.setCreatedBy(rs.getString("created_by"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            silence.setCreatedAt(createdAt.toInstant());
        }

        return silence;
    }

    private String serializeMatchers(List<Matcher> matchers) {
        try {
            return objectMapper.writeValueAsString(matchers);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<Matcher> deserializeMatchers(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Matcher>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
