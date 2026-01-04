package com.bovinemagnet.pgconsole.repository;

import com.bovinemagnet.pgconsole.config.MetadataDataSource;
import com.bovinemagnet.pgconsole.model.CustomDashboard;
import com.bovinemagnet.pgconsole.model.CustomWidget;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Repository for custom dashboard and widget persistence.
 * Provides CRUD operations for user-defined dashboards.
 *
 * @author Paul Snow
 * @since 0.0.0
 */
@ApplicationScoped
public class CustomDashboardRepository {

    @Inject
    @MetadataDataSource
    DataSource dataSource;

    // ========================================
    // Dashboard CRUD
    // ========================================

    /**
     * Get all dashboards for an instance.
     */
    public List<CustomDashboard> findByInstance(String instanceId) {
        String sql = """
            SELECT id, created_at, updated_at, instance_id, name, description,
                   layout, created_by, is_default, is_shared, tags
            FROM pgconsole.custom_dashboard
            WHERE instance_id = ?
            ORDER BY name
            """;

        List<CustomDashboard> dashboards = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, instanceId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    dashboards.add(mapDashboard(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch dashboards for instance: " + instanceId, e);
        }
        return dashboards;
    }

    /**
     * Get all shared dashboards visible to a user.
     */
    public List<CustomDashboard> findShared(String instanceId) {
        String sql = """
            SELECT id, created_at, updated_at, instance_id, name, description,
                   layout, created_by, is_default, is_shared, tags
            FROM pgconsole.custom_dashboard
            WHERE instance_id = ? AND is_shared = TRUE
            ORDER BY name
            """;

        List<CustomDashboard> dashboards = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, instanceId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    dashboards.add(mapDashboard(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch shared dashboards", e);
        }
        return dashboards;
    }

    /**
     * Get a dashboard by ID with its widgets.
     */
    public Optional<CustomDashboard> findById(Long id) {
        String sql = """
            SELECT id, created_at, updated_at, instance_id, name, description,
                   layout, created_by, is_default, is_shared, tags
            FROM pgconsole.custom_dashboard
            WHERE id = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    CustomDashboard dashboard = mapDashboard(rs);
                    dashboard.setWidgets(findWidgetsByDashboardId(id));
                    return Optional.of(dashboard);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch dashboard: " + id, e);
        }
        return Optional.empty();
    }

    /**
     * Create a new dashboard.
     */
    public CustomDashboard create(CustomDashboard dashboard) {
        String sql = """
            INSERT INTO pgconsole.custom_dashboard
            (instance_id, name, description, layout, created_by, is_default, is_shared, tags)
            VALUES (?, ?, ?, ?::jsonb, ?, ?, ?, ?)
            RETURNING id, created_at, updated_at
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, dashboard.getInstanceId());
            stmt.setString(2, dashboard.getName());
            stmt.setString(3, dashboard.getDescription());
            stmt.setString(4, dashboard.getLayout() != null ? dashboard.getLayout() : "{}");
            stmt.setString(5, dashboard.getCreatedBy());
            stmt.setBoolean(6, dashboard.isDefault());
            stmt.setBoolean(7, dashboard.isShared());
            stmt.setArray(8, conn.createArrayOf("TEXT", dashboard.getTags().toArray()));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    dashboard.setId(rs.getLong("id"));
                    dashboard.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
                    dashboard.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));
                }
            }

            // Create widgets if any
            if (dashboard.getWidgets() != null) {
                for (CustomWidget widget : dashboard.getWidgets()) {
                    widget.setDashboardId(dashboard.getId());
                    createWidget(widget, conn);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create dashboard: " + dashboard.getName(), e);
        }
        return dashboard;
    }

    /**
     * Update an existing dashboard.
     */
    public CustomDashboard update(CustomDashboard dashboard) {
        String sql = """
            UPDATE pgconsole.custom_dashboard
            SET name = ?, description = ?, layout = ?::jsonb, is_default = ?, is_shared = ?,
                tags = ?, updated_at = NOW()
            WHERE id = ?
            RETURNING updated_at
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, dashboard.getName());
            stmt.setString(2, dashboard.getDescription());
            stmt.setString(3, dashboard.getLayout());
            stmt.setBoolean(4, dashboard.isDefault());
            stmt.setBoolean(5, dashboard.isShared());
            stmt.setArray(6, conn.createArrayOf("TEXT", dashboard.getTags().toArray()));
            stmt.setLong(7, dashboard.getId());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    dashboard.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update dashboard: " + dashboard.getId(), e);
        }
        return dashboard;
    }

    /**
     * Delete a dashboard and its widgets.
     */
    public void delete(Long id) {
        String sql = "DELETE FROM pgconsole.custom_dashboard WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete dashboard: " + id, e);
        }
    }

    // ========================================
    // Widget CRUD
    // ========================================

    /**
     * Get all widgets for a dashboard.
     */
    public List<CustomWidget> findWidgetsByDashboardId(Long dashboardId) {
        String sql = """
            SELECT id, dashboard_id, widget_type, title, config, position, width, height
            FROM pgconsole.custom_widget
            WHERE dashboard_id = ?
            ORDER BY position
            """;

        List<CustomWidget> widgets = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, dashboardId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    widgets.add(mapWidget(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch widgets for dashboard: " + dashboardId, e);
        }
        return widgets;
    }

    /**
     * Create a widget.
     */
    public CustomWidget createWidget(CustomWidget widget) {
        try (Connection conn = dataSource.getConnection()) {
            return createWidget(widget, conn);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create widget", e);
        }
    }

    private CustomWidget createWidget(CustomWidget widget, Connection conn) throws SQLException {
        String sql = """
            INSERT INTO pgconsole.custom_widget
            (dashboard_id, widget_type, title, config, position, width, height)
            VALUES (?, ?, ?, ?::jsonb, ?, ?, ?)
            RETURNING id
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, widget.getDashboardId());
            stmt.setString(2, widget.getWidgetType());
            stmt.setString(3, widget.getTitle());
            stmt.setString(4, widget.getConfig() != null ? widget.getConfig() : "{}");
            stmt.setInt(5, widget.getPosition());
            stmt.setInt(6, widget.getWidth());
            stmt.setInt(7, widget.getHeight());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    widget.setId(rs.getLong("id"));
                }
            }
        }
        return widget;
    }

    /**
     * Update a widget.
     */
    public CustomWidget updateWidget(CustomWidget widget) {
        String sql = """
            UPDATE pgconsole.custom_widget
            SET widget_type = ?, title = ?, config = ?::jsonb, position = ?, width = ?, height = ?
            WHERE id = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, widget.getWidgetType());
            stmt.setString(2, widget.getTitle());
            stmt.setString(3, widget.getConfig());
            stmt.setInt(4, widget.getPosition());
            stmt.setInt(5, widget.getWidth());
            stmt.setInt(6, widget.getHeight());
            stmt.setLong(7, widget.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update widget: " + widget.getId(), e);
        }
        return widget;
    }

    /**
     * Delete a widget.
     */
    public void deleteWidget(Long widgetId) {
        String sql = "DELETE FROM pgconsole.custom_widget WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, widgetId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete widget: " + widgetId, e);
        }
    }

    /**
     * Replace all widgets for a dashboard.
     */
    public void replaceWidgets(Long dashboardId, List<CustomWidget> widgets) {
        String deleteSql = "DELETE FROM pgconsole.custom_widget WHERE dashboard_id = ?";

        try (Connection conn = dataSource.getConnection()) {
            // Delete existing widgets
            try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
                stmt.setLong(1, dashboardId);
                stmt.executeUpdate();
            }

            // Insert new widgets
            int position = 0;
            for (CustomWidget widget : widgets) {
                widget.setDashboardId(dashboardId);
                widget.setPosition(position++);
                createWidget(widget, conn);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to replace widgets for dashboard: " + dashboardId, e);
        }
    }

    // ========================================
    // Mapping
    // ========================================

    private CustomDashboard mapDashboard(ResultSet rs) throws SQLException {
        CustomDashboard dashboard = new CustomDashboard();
        dashboard.setId(rs.getLong("id"));
        dashboard.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        dashboard.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));
        dashboard.setInstanceId(rs.getString("instance_id"));
        dashboard.setName(rs.getString("name"));
        dashboard.setDescription(rs.getString("description"));
        dashboard.setLayout(rs.getString("layout"));
        dashboard.setCreatedBy(rs.getString("created_by"));
        dashboard.setDefault(rs.getBoolean("is_default"));
        dashboard.setShared(rs.getBoolean("is_shared"));

        Array tagsArray = rs.getArray("tags");
        if (tagsArray != null) {
            String[] tags = (String[]) tagsArray.getArray();
            dashboard.setTags(new ArrayList<>(Arrays.asList(tags)));
        }

        return dashboard;
    }

    private CustomWidget mapWidget(ResultSet rs) throws SQLException {
        CustomWidget widget = new CustomWidget();
        widget.setId(rs.getLong("id"));
        widget.setDashboardId(rs.getLong("dashboard_id"));
        widget.setWidgetType(rs.getString("widget_type"));
        widget.setTitle(rs.getString("title"));
        widget.setConfig(rs.getString("config"));
        widget.setPosition(rs.getInt("position"));
        widget.setWidth(rs.getInt("width"));
        widget.setHeight(rs.getInt("height"));
        return widget;
    }
}
