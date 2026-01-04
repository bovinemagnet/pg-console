package com.bovinemagnet.pgconsole.repository;

import com.bovinemagnet.pgconsole.config.MetadataDataSource;
import com.bovinemagnet.pgconsole.model.MaintenanceWindow;
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
 * Repository for managing maintenance windows.
 * <p>
 * Provides CRUD operations for maintenance window configurations.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class MaintenanceWindowRepository {

    @Inject
    @MetadataDataSource
    DataSource dataSource;

    /**
     * Finds all maintenance windows.
     *
     * @return list of all maintenance windows
     */
    public List<MaintenanceWindow> findAll() {
        String sql = """
            SELECT id, name, description, start_time, end_time, recurring,
                   recurrence_pattern, instance_filter, alert_type_filter,
                   created_by, created_at, updated_at
            FROM pgconsole.maintenance_window
            ORDER BY start_time DESC
            """;

        List<MaintenanceWindow> windows = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                windows.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch maintenance windows", e);
        }
        return windows;
    }

    /**
     * Finds all currently active maintenance windows.
     *
     * @return list of active windows
     */
    public List<MaintenanceWindow> findActive() {
        String sql = """
            SELECT id, name, description, start_time, end_time, recurring,
                   recurrence_pattern, instance_filter, alert_type_filter,
                   created_by, created_at, updated_at
            FROM pgconsole.maintenance_window
            WHERE start_time <= NOW() AND end_time > NOW()
            ORDER BY end_time
            """;

        List<MaintenanceWindow> windows = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                windows.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch active maintenance windows", e);
        }
        return windows;
    }

    /**
     * Finds upcoming maintenance windows (scheduled but not yet started).
     *
     * @return list of upcoming windows
     */
    public List<MaintenanceWindow> findUpcoming() {
        String sql = """
            SELECT id, name, description, start_time, end_time, recurring,
                   recurrence_pattern, instance_filter, alert_type_filter,
                   created_by, created_at, updated_at
            FROM pgconsole.maintenance_window
            WHERE start_time > NOW()
            ORDER BY start_time
            """;

        List<MaintenanceWindow> windows = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                windows.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch upcoming maintenance windows", e);
        }
        return windows;
    }

    /**
     * Finds a maintenance window by ID.
     *
     * @param id window ID
     * @return optional containing window if found
     */
    public Optional<MaintenanceWindow> findById(Long id) {
        String sql = """
            SELECT id, name, description, start_time, end_time, recurring,
                   recurrence_pattern, instance_filter, alert_type_filter,
                   created_by, created_at, updated_at
            FROM pgconsole.maintenance_window
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
            throw new RuntimeException("Failed to fetch maintenance window by ID", e);
        }
        return Optional.empty();
    }

    /**
     * Saves a new maintenance window.
     *
     * @param window window to save
     * @return saved window with ID
     */
    public MaintenanceWindow save(MaintenanceWindow window) {
        String sql = """
            INSERT INTO pgconsole.maintenance_window
                (name, description, start_time, end_time, recurring, recurrence_pattern,
                 instance_filter, alert_type_filter, created_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id, created_at, updated_at
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, window.getName());
            stmt.setString(2, window.getDescription());
            stmt.setTimestamp(3, Timestamp.from(window.getStartTime()));
            stmt.setTimestamp(4, Timestamp.from(window.getEndTime()));
            stmt.setBoolean(5, window.isRecurring());
            stmt.setString(6, window.getRecurrencePattern());
            stmt.setArray(7, conn.createArrayOf("text",
                window.getInstanceFilter() != null ? window.getInstanceFilter().toArray() : new String[0]));
            stmt.setArray(8, conn.createArrayOf("text",
                window.getAlertTypeFilter() != null ? window.getAlertTypeFilter().toArray() : new String[0]));
            stmt.setString(9, window.getCreatedBy());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    window.setId(rs.getLong("id"));
                    window.setCreatedAt(rs.getTimestamp("created_at").toInstant());
                    window.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save maintenance window", e);
        }
        return window;
    }

    /**
     * Updates an existing maintenance window.
     *
     * @param window window to update
     * @return updated window
     */
    public MaintenanceWindow update(MaintenanceWindow window) {
        String sql = """
            UPDATE pgconsole.maintenance_window
            SET name = ?, description = ?, start_time = ?, end_time = ?, recurring = ?,
                recurrence_pattern = ?, instance_filter = ?, alert_type_filter = ?,
                updated_at = NOW()
            WHERE id = ?
            RETURNING updated_at
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, window.getName());
            stmt.setString(2, window.getDescription());
            stmt.setTimestamp(3, Timestamp.from(window.getStartTime()));
            stmt.setTimestamp(4, Timestamp.from(window.getEndTime()));
            stmt.setBoolean(5, window.isRecurring());
            stmt.setString(6, window.getRecurrencePattern());
            stmt.setArray(7, conn.createArrayOf("text",
                window.getInstanceFilter() != null ? window.getInstanceFilter().toArray() : new String[0]));
            stmt.setArray(8, conn.createArrayOf("text",
                window.getAlertTypeFilter() != null ? window.getAlertTypeFilter().toArray() : new String[0]));
            stmt.setLong(9, window.getId());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    window.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update maintenance window", e);
        }
        return window;
    }

    /**
     * Deletes a maintenance window.
     *
     * @param id window ID
     */
    public void delete(Long id) {
        String sql = "DELETE FROM pgconsole.maintenance_window WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete maintenance window", e);
        }
    }

    /**
     * Checks if any active maintenance window suppresses the given alert.
     *
     * @param instanceName instance name
     * @param alertType alert type
     * @return true if alert should be suppressed
     */
    public boolean shouldSuppress(String instanceName, String alertType) {
        List<MaintenanceWindow> activeWindows = findActive();
        for (MaintenanceWindow window : activeWindows) {
            if (window.suppressesAlert(instanceName, alertType)) {
                return true;
            }
        }
        return false;
    }

    private MaintenanceWindow mapRow(ResultSet rs) throws SQLException {
        MaintenanceWindow window = new MaintenanceWindow();
        window.setId(rs.getLong("id"));
        window.setName(rs.getString("name"));
        window.setDescription(rs.getString("description"));

        Timestamp startTime = rs.getTimestamp("start_time");
        if (startTime != null) {
            window.setStartTime(startTime.toInstant());
        }

        Timestamp endTime = rs.getTimestamp("end_time");
        if (endTime != null) {
            window.setEndTime(endTime.toInstant());
        }

        window.setRecurring(rs.getBoolean("recurring"));
        window.setRecurrencePattern(rs.getString("recurrence_pattern"));

        Array instanceArray = rs.getArray("instance_filter");
        if (instanceArray != null) {
            window.setInstanceFilter(Arrays.asList((String[]) instanceArray.getArray()));
        }

        Array alertTypeArray = rs.getArray("alert_type_filter");
        if (alertTypeArray != null) {
            window.setAlertTypeFilter(Arrays.asList((String[]) alertTypeArray.getArray()));
        }

        window.setCreatedBy(rs.getString("created_by"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            window.setCreatedAt(createdAt.toInstant());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            window.setUpdatedAt(updatedAt.toInstant());
        }

        return window;
    }
}
