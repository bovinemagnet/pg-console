package com.bovinemagnet.pgconsole.model;

/**
 * Represents a configurable widget within a custom dashboard that displays
 * PostgreSQL metrics, statistics, or visualisations.
 * <p>
 * Widgets are the fundamental building blocks of custom dashboards, providing
 * a flexible way to display various PostgreSQL monitoring data. Each widget
 * has a type (defined by {@link WidgetType}), a position in the dashboard grid,
 * and optional configuration stored as JSON.
 * <p>
 * The widget system uses a Bootstrap 12-column grid layout, where each widget
 * can span 1-12 columns (controlled by {@link #width}) and one or more rows
 * (controlled by {@link #height}). Widgets are ordered by their {@link #position}
 * value and flow from left to right, top to bottom within the grid.
 * <p>
 * Example usage:
 * <pre>{@code
 * CustomWidget widget = CustomWidget.builder()
 *     .dashboardId(1L)
 *     .widgetType(WidgetType.CACHE_RATIO)
 *     .title("Cache Performance")
 *     .width(6)
 *     .height(1)
 *     .position(0)
 *     .config("{\"threshold\": 90}")
 *     .build();
 * }</pre>
 *
 * @author Paul Snow
 * @since 0.0.0
 * @see CustomDashboard
 * @see WidgetType
 */
public class CustomWidget {

    /**
     * Enumeration of available widget types for custom dashboards.
     * <p>
     * Each widget type corresponds to a specific PostgreSQL metric or visualisation,
     * including real-time gauges, historical trends (sparklines), top-N lists,
     * and custom SQL queries. Widget types are identified by a unique code string
     * stored in the database.
     * <p>
     * Widget types include both simple metrics (e.g., connection count) and
     * complex visualisations (e.g., sparkline trends over time). The {@code CUSTOM_SQL}
     * type allows users to define read-only queries for bespoke monitoring needs.
     *
     * @since 0.0.0
     */
    public enum WidgetType {
        /** Displays the current total connection count across all databases. */
        CONNECTIONS("connections", "Connections", "bi-plug"),

        /** Displays the cache hit ratio as a percentage, indicating buffer pool efficiency. */
        CACHE_RATIO("cache-ratio", "Cache Hit Ratio", "bi-speedometer"),

        /** Shows the count of currently executing queries (state = 'active'). */
        ACTIVE_QUERIES("active-queries", "Active Queries", "bi-play-circle"),

        /** Shows the count of queries blocked by locks from other sessions. */
        BLOCKED_QUERIES("blocked-queries", "Blocked Queries", "bi-exclamation-triangle"),

        /** Displays the total database size in human-readable format. */
        DB_SIZE("db-size", "Database Size", "bi-hdd"),

        /** Lists the top N tables by size, with configurable limit. */
        TOP_TABLES("top-tables", "Top Tables", "bi-table"),

        /** Lists the top N indexes by size, with configurable limit. */
        TOP_INDEXES("top-indexes", "Top Indexes", "bi-lightning"),

        /** Renders an SVG sparkline showing connection count trends over time. */
        SPARKLINE_CONNECTIONS("sparkline-connections", "Connection Trend", "bi-graph-up"),

        /** Renders an SVG sparkline showing query execution trends over time. */
        SPARKLINE_QUERIES("sparkline-queries", "Query Trend", "bi-graph-up-arrow"),

        /** Displays information about the currently longest-running query. */
        LONGEST_QUERY("longest-query", "Longest Query", "bi-clock-history"),

        /** Shows the transaction commit and rollback rate per second. */
        TRANSACTION_RATE("transaction-rate", "Transaction Rate", "bi-arrow-repeat"),

        /** Displays tuple-level statistics including inserts, updates, deletes. */
        TUPLE_STATS("tuple-stats", "Tuple Statistics", "bi-list-ol"),

        /** Executes a user-defined read-only SQL query and displays results. */
        CUSTOM_SQL("custom-sql", "Custom Query", "bi-code-square");

        /** Database code identifier for this widget type. */
        private final String code;

        /** Human-readable label displayed in the UI. */
        private final String label;

        /** Bootstrap Icons class name for visual identification. */
        private final String icon;

        /**
         * Constructs a widget type with its identifying attributes.
         *
         * @param code the unique database code for this widget type
         * @param label the human-readable display label
         * @param icon the Bootstrap Icons class name (e.g., "bi-plug")
         */
        WidgetType(String code, String label, String icon) {
            this.code = code;
            this.label = label;
            this.icon = icon;
        }

        /**
         * Returns the unique database code for this widget type.
         * <p>
         * This code is stored in the database and used for widget type identification
         * and deserialisation.
         *
         * @return the widget type code, never null
         */
        public String getCode() {
            return code;
        }

        /**
         * Returns the human-readable label for this widget type.
         * <p>
         * This label is displayed in the UI and used as the default widget title
         * when no custom title is specified.
         *
         * @return the display label, never null
         */
        public String getLabel() {
            return label;
        }

        /**
         * Returns the Bootstrap Icons class name for this widget type.
         * <p>
         * The icon provides visual identification in the dashboard UI.
         *
         * @return the icon class name (e.g., "bi-plug"), never null
         */
        public String getIcon() {
            return icon;
        }

        /**
         * Converts a database code string to its corresponding WidgetType enum.
         * <p>
         * This method performs case-sensitive matching against all widget type codes.
         * If no matching type is found, it defaults to {@link #CONNECTIONS} to ensure
         * a valid widget type is always returned.
         *
         * @param code the widget type code to look up
         * @return the matching WidgetType, or {@link #CONNECTIONS} if not found
         */
        public static WidgetType fromCode(String code) {
            for (WidgetType type : values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }
            return CONNECTIONS; // Default
        }
    }

    /** Primary key identifier for database persistence. */
    private Long id;

    /** Foreign key reference to the parent dashboard. */
    private Long dashboardId;

    /** Widget type code matching a {@link WidgetType#getCode()}. */
    private String widgetType;

    /** Optional custom title that overrides the default widget type label. */
    private String title;

    /** JSON string containing widget-specific configuration (e.g., thresholds, limits). */
    private String config;

    /** Zero-based position index controlling widget order in the dashboard grid. */
    private int position;

    /** Width in Bootstrap grid columns (1-12), where 12 spans the full width. */
    private int width;

    /** Height in rows, allowing widgets to occupy vertical space (minimum 1). */
    private int height;

    /**
     * Transient field holding the rendered widget data.
     * <p>
     * This field is populated at runtime with the widget's data (e.g., metric values,
     * query results) and is not persisted to the database. The data type varies
     * depending on the widget type.
     */
    private transient Object data;

    /**
     * Constructs a new CustomWidget with default values.
     * <p>
     * Default values:
     * <ul>
     *   <li>widgetType: {@link WidgetType#CONNECTIONS}</li>
     *   <li>config: "{}" (empty JSON object)</li>
     *   <li>position: 0</li>
     *   <li>width: 6 (half-width in Bootstrap grid)</li>
     *   <li>height: 1</li>
     * </ul>
     */
    public CustomWidget() {
        this.widgetType = WidgetType.CONNECTIONS.getCode();
        this.config = "{}";
        this.position = 0;
        this.width = 6;
        this.height = 1;
    }

    /**
     * Creates a new Builder instance for constructing CustomWidget objects.
     * <p>
     * The Builder pattern provides a fluent API for widget construction with
     * validation and sensible defaults.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing CustomWidget instances with a fluent API.
     * <p>
     * The Builder enforces constraints such as grid width (1-12 columns) and
     * minimum height (1 row) during construction. All builder methods return
     * {@code this} to enable method chaining.
     * <p>
     * Example:
     * <pre>{@code
     * CustomWidget widget = CustomWidget.builder()
     *     .dashboardId(1L)
     *     .widgetType(WidgetType.CACHE_RATIO)
     *     .width(4)
     *     .position(2)
     *     .build();
     * }</pre>
     *
     * @since 0.0.0
     */
    public static class Builder {
        /** The widget instance being constructed. */
        private final CustomWidget widget = new CustomWidget();

        /**
         * Sets the widget's primary key identifier.
         *
         * @param id the database ID, or null for new widgets
         * @return this Builder instance
         */
        public Builder id(Long id) {
            widget.id = id;
            return this;
        }

        /**
         * Sets the parent dashboard's identifier.
         *
         * @param dashboardId the dashboard ID this widget belongs to
         * @return this Builder instance
         */
        public Builder dashboardId(Long dashboardId) {
            widget.dashboardId = dashboardId;
            return this;
        }

        /**
         * Sets the widget type using a code string.
         * <p>
         * The code should match a valid {@link WidgetType#getCode()} value.
         *
         * @param widgetType the widget type code
         * @return this Builder instance
         */
        public Builder widgetType(String widgetType) {
            widget.widgetType = widgetType;
            return this;
        }

        /**
         * Sets the widget type using a WidgetType enum value.
         * <p>
         * This is the type-safe alternative to {@link #widgetType(String)}.
         *
         * @param type the widget type enum
         * @return this Builder instance
         */
        public Builder widgetType(WidgetType type) {
            widget.widgetType = type.getCode();
            return this;
        }

        /**
         * Sets the custom title for this widget.
         * <p>
         * If null or blank, the widget will use the {@link WidgetType#getLabel()}
         * as its display title.
         *
         * @param title the custom title, or null to use the default
         * @return this Builder instance
         */
        public Builder title(String title) {
            widget.title = title;
            return this;
        }

        /**
         * Sets the widget configuration as a JSON string.
         * <p>
         * Configuration format varies by widget type. For example:
         * <ul>
         *   <li>TOP_TABLES: {@code {"limit": 10}}</li>
         *   <li>CACHE_RATIO: {@code {"threshold": 90}}</li>
         *   <li>CUSTOM_SQL: {@code {"query": "SELECT ..."}}</li>
         * </ul>
         *
         * @param config the JSON configuration string
         * @return this Builder instance
         */
        public Builder config(String config) {
            widget.config = config;
            return this;
        }

        /**
         * Sets the widget's position in the dashboard grid.
         * <p>
         * Widgets are sorted by position in ascending order. Position 0 appears
         * first (top-left), and higher positions flow left-to-right, top-to-bottom.
         *
         * @param position the zero-based position index
         * @return this Builder instance
         */
        public Builder position(int position) {
            widget.position = position;
            return this;
        }

        /**
         * Sets the widget width in Bootstrap grid columns (1-12).
         * <p>
         * Values outside this range are clamped: values less than 1 become 1,
         * and values greater than 12 become 12. A width of 12 spans the full
         * dashboard width.
         *
         * @param width the desired width in columns (will be clamped to 1-12)
         * @return this Builder instance
         */
        public Builder width(int width) {
            widget.width = Math.max(1, Math.min(12, width));
            return this;
        }

        /**
         * Sets the widget height in rows.
         * <p>
         * Values less than 1 are clamped to 1. There is no upper limit, allowing
         * widgets to occupy significant vertical space if needed.
         *
         * @param height the desired height in rows (minimum 1)
         * @return this Builder instance
         */
        public Builder height(int height) {
            widget.height = Math.max(1, height);
            return this;
        }

        /**
         * Constructs the CustomWidget instance with all configured values.
         *
         * @return the constructed CustomWidget
         */
        public CustomWidget build() {
            return widget;
        }
    }

    /**
     * Returns the widget's primary key identifier.
     *
     * @return the database ID, or null if not yet persisted
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the widget's primary key identifier.
     *
     * @param id the database ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns the parent dashboard's identifier.
     *
     * @return the dashboard ID this widget belongs to
     */
    public Long getDashboardId() {
        return dashboardId;
    }

    /**
     * Sets the parent dashboard's identifier.
     *
     * @param dashboardId the dashboard ID
     */
    public void setDashboardId(Long dashboardId) {
        this.dashboardId = dashboardId;
    }

    /**
     * Returns the widget type code string.
     *
     * @return the widget type code
     * @see WidgetType#getCode()
     */
    public String getWidgetType() {
        return widgetType;
    }

    /**
     * Sets the widget type code string.
     *
     * @param widgetType the widget type code
     */
    public void setWidgetType(String widgetType) {
        this.widgetType = widgetType;
    }

    /**
     * Returns the widget type as a WidgetType enum.
     * <p>
     * This method converts the stored code string to its corresponding enum value
     * using {@link WidgetType#fromCode(String)}, which defaults to
     * {@link WidgetType#CONNECTIONS} if the code is not recognised.
     *
     * @return the WidgetType enum, never null
     */
    public WidgetType getWidgetTypeEnum() {
        return WidgetType.fromCode(widgetType);
    }

    /**
     * Returns the custom title for this widget.
     *
     * @return the custom title, or null if using the default
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the custom title for this widget.
     *
     * @param title the custom title, or null to use the default
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the display title, using the widget type label as a fallback.
     * <p>
     * If a custom title is set and non-blank, it is returned. Otherwise, the
     * default label from the widget's {@link WidgetType} is used.
     *
     * @return the display title, never null
     */
    public String getDisplayTitle() {
        if (title != null && !title.isBlank()) {
            return title;
        }
        return getWidgetTypeEnum().getLabel();
    }

    /**
     * Returns the widget configuration as a JSON string.
     *
     * @return the JSON configuration string, never null
     */
    public String getConfig() {
        return config;
    }

    /**
     * Sets the widget configuration as a JSON string.
     *
     * @param config the JSON configuration string
     */
    public void setConfig(String config) {
        this.config = config;
    }

    /**
     * Returns the widget's position in the dashboard grid.
     *
     * @return the zero-based position index
     */
    public int getPosition() {
        return position;
    }

    /**
     * Sets the widget's position in the dashboard grid.
     *
     * @param position the zero-based position index
     */
    public void setPosition(int position) {
        this.position = position;
    }

    /**
     * Returns the widget width in Bootstrap grid columns.
     *
     * @return the width (1-12)
     */
    public int getWidth() {
        return width;
    }

    /**
     * Sets the widget width in Bootstrap grid columns.
     * <p>
     * Values are automatically clamped to the valid range of 1-12.
     *
     * @param width the desired width (will be clamped to 1-12)
     */
    public void setWidth(int width) {
        this.width = Math.max(1, Math.min(12, width));
    }

    /**
     * Returns the widget height in rows.
     *
     * @return the height (minimum 1)
     */
    public int getHeight() {
        return height;
    }

    /**
     * Sets the widget height in rows.
     * <p>
     * Values less than 1 are automatically clamped to 1.
     *
     * @param height the desired height (minimum 1)
     */
    public void setHeight(int height) {
        this.height = Math.max(1, height);
    }

    /**
     * Returns the Bootstrap column CSS class for this widget's width.
     * <p>
     * The returned class follows the pattern {@code col-md-N} where N is the
     * widget's width (1-12). This class should be applied to the widget's
     * container div for proper grid layout.
     *
     * @return the Bootstrap column class (e.g., "col-md-6")
     */
    public String getColClass() {
        return "col-md-" + width;
    }

    /**
     * Returns the Bootstrap Icons class name for this widget type.
     * <p>
     * This is a convenience method that delegates to the widget type's icon.
     *
     * @return the icon class name (e.g., "bi-plug")
     * @see WidgetType#getIcon()
     */
    public String getIcon() {
        return getWidgetTypeEnum().getIcon();
    }

    /**
     * Returns the transient data object for this widget.
     * <p>
     * The data object contains the rendered widget data (e.g., metric values,
     * query results) populated at runtime. The type of the object varies
     * depending on the widget type.
     *
     * @return the widget data, or null if not yet populated
     */
    public Object getData() {
        return data;
    }

    /**
     * Sets the transient data object for this widget.
     * <p>
     * This field is populated by the service layer before rendering and is
     * not persisted to the database.
     *
     * @param data the widget data object
     */
    public void setData(Object data) {
        this.data = data;
    }
}
