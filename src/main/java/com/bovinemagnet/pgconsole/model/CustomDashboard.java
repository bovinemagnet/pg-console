package com.bovinemagnet.pgconsole.model;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a user-defined custom dashboard with personalised widgets for PostgreSQL monitoring.
 * <p>
 * Custom dashboards allow users to create tailored monitoring views by composing multiple widgets
 * that display specific PostgreSQL metrics, statistics, or visualisations. Dashboards can be
 * personalised per user, shared across users, and optionally set as the default view.
 * <p>
 * Each dashboard maintains a collection of {@link CustomWidget} instances that define what
 * metrics to display, their positioning, and configuration. The layout is stored as a JSON
 * string allowing flexible grid-based positioning of widgets.
 * <p>
 * Dashboards are scoped to a specific PostgreSQL instance via {@code instanceId}, enabling
 * multi-instance monitoring within a single application deployment.
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * CustomDashboard dashboard = CustomDashboard.builder()
 *     .name("Production Monitoring")
 *     .description("Key production metrics and alerts")
 *     .instanceId("prod-db-01")
 *     .createdBy("admin")
 *     .isDefault(true)
 *     .isShared(true)
 *     .tags(List.of("production", "critical"))
 *     .build();
 *
 * dashboard.addWidget(CustomWidget.builder()
 *     .widgetType(CustomWidget.WidgetType.CONNECTIONS)
 *     .title("Active Connections")
 *     .build());
 * }</pre>
 *
 * @author Paul Snow
 * @since 0.0.0
 * @see CustomWidget
 */
public class CustomDashboard {

    /** Unique identifier for this dashboard. */
    private Long id;

    /** Timestamp when this dashboard was created. */
    private OffsetDateTime createdAt;

    /** Timestamp when this dashboard was last updated. */
    private OffsetDateTime updatedAt;

    /** PostgreSQL instance identifier this dashboard is associated with. */
    private String instanceId;

    /** Display name of the dashboard. */
    private String name;

    /** Optional detailed description of the dashboard's purpose. */
    private String description;

    /** JSON string containing layout configuration (e.g., grid columns, responsive breakpoints). */
    private String layout;

    /** Username or identifier of the user who created this dashboard. */
    private String createdBy;

    /** Whether this dashboard is the default for the current user or instance. */
    private boolean isDefault;

    /** Whether this dashboard is visible to other users. */
    private boolean isShared;

    /** Categorisation tags for filtering and organisation (e.g., "production", "performance"). */
    private List<String> tags;

    /** Collection of widgets that compose this dashboard. */
    private List<CustomWidget> widgets;

    /**
     * Constructs a new CustomDashboard with default values.
     * <p>
     * Initialises empty lists for tags and widgets, and sets a default layout
     * configuration with 2 columns.
     */
    public CustomDashboard() {
        this.tags = new ArrayList<>();
        this.widgets = new ArrayList<>();
        this.layout = "{\"columns\": 2}";
    }

    /**
     * Creates a new builder for constructing CustomDashboard instances.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link CustomDashboard} instances with a fluent API.
     * <p>
     * The builder pattern provides a readable and flexible way to construct dashboards
     * with optional fields, avoiding telescoping constructors.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * CustomDashboard dashboard = CustomDashboard.builder()
     *     .name("System Overview")
     *     .instanceId("db-prod")
     *     .isDefault(true)
     *     .tags(List.of("overview", "system"))
     *     .build();
     * }</pre>
     */
    public static class Builder {
        private final CustomDashboard dashboard = new CustomDashboard();

        /**
         * Sets the unique identifier for the dashboard.
         *
         * @param id the dashboard ID
         * @return this builder
         */
        public Builder id(Long id) {
            dashboard.id = id;
            return this;
        }

        /**
         * Sets the creation timestamp.
         *
         * @param createdAt the timestamp when the dashboard was created
         * @return this builder
         */
        public Builder createdAt(OffsetDateTime createdAt) {
            dashboard.createdAt = createdAt;
            return this;
        }

        /**
         * Sets the last update timestamp.
         *
         * @param updatedAt the timestamp when the dashboard was last modified
         * @return this builder
         */
        public Builder updatedAt(OffsetDateTime updatedAt) {
            dashboard.updatedAt = updatedAt;
            return this;
        }

        /**
         * Sets the PostgreSQL instance identifier.
         *
         * @param instanceId the instance ID this dashboard monitors
         * @return this builder
         */
        public Builder instanceId(String instanceId) {
            dashboard.instanceId = instanceId;
            return this;
        }

        /**
         * Sets the display name of the dashboard.
         *
         * @param name the dashboard name
         * @return this builder
         */
        public Builder name(String name) {
            dashboard.name = name;
            return this;
        }

        /**
         * Sets the detailed description.
         *
         * @param description the dashboard description
         * @return this builder
         */
        public Builder description(String description) {
            dashboard.description = description;
            return this;
        }

        /**
         * Sets the layout configuration as a JSON string.
         * <p>
         * The layout defines how widgets are arranged, typically including grid
         * specifications, column counts, and responsive breakpoints.
         *
         * @param layout JSON string containing layout configuration
         * @return this builder
         */
        public Builder layout(String layout) {
            dashboard.layout = layout;
            return this;
        }

        /**
         * Sets the creator's username or identifier.
         *
         * @param createdBy the username of the dashboard creator
         * @return this builder
         */
        public Builder createdBy(String createdBy) {
            dashboard.createdBy = createdBy;
            return this;
        }

        /**
         * Sets whether this dashboard is the default.
         *
         * @param isDefault true if this should be the default dashboard, false otherwise
         * @return this builder
         */
        public Builder isDefault(boolean isDefault) {
            dashboard.isDefault = isDefault;
            return this;
        }

        /**
         * Sets whether this dashboard is shared with other users.
         *
         * @param isShared true if the dashboard is shared, false for private
         * @return this builder
         */
        public Builder isShared(boolean isShared) {
            dashboard.isShared = isShared;
            return this;
        }

        /**
         * Sets the categorisation tags.
         * <p>
         * Creates a defensive copy of the provided list. If null is provided,
         * initialises with an empty list.
         *
         * @param tags the list of tags, or null for no tags
         * @return this builder
         */
        public Builder tags(List<String> tags) {
            dashboard.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
            return this;
        }

        /**
         * Sets the collection of widgets for this dashboard.
         * <p>
         * Creates a defensive copy of the provided list. If null is provided,
         * initialises with an empty list.
         *
         * @param widgets the list of widgets, or null for no widgets
         * @return this builder
         */
        public Builder widgets(List<CustomWidget> widgets) {
            dashboard.widgets = widgets != null ? new ArrayList<>(widgets) : new ArrayList<>();
            return this;
        }

        /**
         * Constructs the CustomDashboard instance with the configured values.
         *
         * @return the constructed CustomDashboard
         */
        public CustomDashboard build() {
            return dashboard;
        }
    }

    // Getters and Setters

    /**
     * Retrieves the unique identifier for this dashboard.
     *
     * @return the dashboard ID, or null if not yet persisted
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the unique identifier for this dashboard.
     *
     * @param id the dashboard ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Retrieves the timestamp when this dashboard was created.
     *
     * @return the creation timestamp, or null if not set
     */
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the timestamp when this dashboard was created.
     *
     * @param createdAt the creation timestamp
     */
    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Retrieves the timestamp when this dashboard was last updated.
     *
     * @return the last update timestamp, or null if never updated
     */
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the timestamp when this dashboard was last updated.
     *
     * @param updatedAt the last update timestamp
     */
    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Retrieves the PostgreSQL instance identifier this dashboard monitors.
     *
     * @return the instance ID, or null if not scoped to a specific instance
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * Sets the PostgreSQL instance identifier this dashboard monitors.
     *
     * @param instanceId the instance ID
     */
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    /**
     * Retrieves the display name of this dashboard.
     *
     * @return the dashboard name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the display name of this dashboard.
     *
     * @param name the dashboard name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Retrieves the detailed description of this dashboard.
     *
     * @return the description, or null if not set
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the detailed description of this dashboard.
     *
     * @param description the description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Retrieves the layout configuration as a JSON string.
     * <p>
     * The layout typically defines grid specifications, column counts,
     * and responsive breakpoints for widget positioning.
     *
     * @return the layout JSON string
     */
    public String getLayout() {
        return layout;
    }

    /**
     * Sets the layout configuration as a JSON string.
     *
     * @param layout the layout JSON string
     */
    public void setLayout(String layout) {
        this.layout = layout;
    }

    /**
     * Retrieves the username or identifier of the user who created this dashboard.
     *
     * @return the creator's username, or null if not set
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Sets the username or identifier of the user who created this dashboard.
     *
     * @param createdBy the creator's username
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Checks whether this dashboard is the default for the current user or instance.
     *
     * @return true if this is the default dashboard, false otherwise
     */
    public boolean isDefault() {
        return isDefault;
    }

    /**
     * Sets whether this dashboard is the default for the current user or instance.
     * <p>
     * Only one dashboard should typically be marked as default per user/instance.
     *
     * @param aDefault true to mark as default, false otherwise
     */
    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    /**
     * Checks whether this dashboard is shared with other users.
     *
     * @return true if the dashboard is shared, false if private
     */
    public boolean isShared() {
        return isShared;
    }

    /**
     * Sets whether this dashboard is shared with other users.
     *
     * @param shared true to share the dashboard, false to keep private
     */
    public void setShared(boolean shared) {
        isShared = shared;
    }

    /**
     * Retrieves the categorisation tags for this dashboard.
     * <p>
     * Tags enable filtering and organisation of dashboards (e.g., "production", "performance").
     *
     * @return the list of tags, never null
     */
    public List<String> getTags() {
        return tags;
    }

    /**
     * Sets the categorisation tags for this dashboard.
     * <p>
     * If null is provided, initialises with an empty list to prevent null pointer exceptions.
     *
     * @param tags the list of tags, or null for no tags
     */
    public void setTags(List<String> tags) {
        this.tags = tags != null ? tags : new ArrayList<>();
    }

    /**
     * Retrieves the collection of widgets that compose this dashboard.
     *
     * @return the list of widgets, never null
     * @see CustomWidget
     */
    public List<CustomWidget> getWidgets() {
        return widgets;
    }

    /**
     * Sets the collection of widgets that compose this dashboard.
     * <p>
     * If null is provided, initialises with an empty list to prevent null pointer exceptions.
     *
     * @param widgets the list of widgets, or null for no widgets
     * @see CustomWidget
     */
    public void setWidgets(List<CustomWidget> widgets) {
        this.widgets = widgets != null ? widgets : new ArrayList<>();
    }

    /**
     * Adds a widget to this dashboard's collection.
     * <p>
     * If the widgets collection is null (which shouldn't occur under normal circumstances),
     * initialises it before adding the widget.
     *
     * @param widget the widget to add, must not be null
     * @see CustomWidget
     */
    public void addWidget(CustomWidget widget) {
        if (this.widgets == null) {
            this.widgets = new ArrayList<>();
        }
        this.widgets.add(widget);
    }
}
