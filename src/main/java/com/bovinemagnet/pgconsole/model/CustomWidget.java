package com.bovinemagnet.pgconsole.model;

/**
 * Represents a widget within a custom dashboard.
 * Widgets display specific metrics or data visualisations.
 *
 * @author Paul Snow
 * @since 0.0.0
 */
public class CustomWidget {

    /**
     * Available widget types for custom dashboards.
     */
    public enum WidgetType {
        /** Current connection count gauge */
        CONNECTIONS("connections", "Connections", "bi-plug"),
        /** Cache hit ratio percentage */
        CACHE_RATIO("cache-ratio", "Cache Hit Ratio", "bi-speedometer"),
        /** Active query count */
        ACTIVE_QUERIES("active-queries", "Active Queries", "bi-play-circle"),
        /** Blocked query count */
        BLOCKED_QUERIES("blocked-queries", "Blocked Queries", "bi-exclamation-triangle"),
        /** Database size */
        DB_SIZE("db-size", "Database Size", "bi-hdd"),
        /** Top N tables by size */
        TOP_TABLES("top-tables", "Top Tables", "bi-table"),
        /** Top N indexes by size */
        TOP_INDEXES("top-indexes", "Top Indexes", "bi-lightning"),
        /** Connection trend sparkline */
        SPARKLINE_CONNECTIONS("sparkline-connections", "Connection Trend", "bi-graph-up"),
        /** Query trend sparkline */
        SPARKLINE_QUERIES("sparkline-queries", "Query Trend", "bi-graph-up-arrow"),
        /** Longest running query */
        LONGEST_QUERY("longest-query", "Longest Query", "bi-clock-history"),
        /** Transaction rate */
        TRANSACTION_RATE("transaction-rate", "Transaction Rate", "bi-arrow-repeat"),
        /** Tuple statistics */
        TUPLE_STATS("tuple-stats", "Tuple Statistics", "bi-list-ol"),
        /** Custom SQL query (read-only) */
        CUSTOM_SQL("custom-sql", "Custom Query", "bi-code-square");

        private final String code;
        private final String label;
        private final String icon;

        WidgetType(String code, String label, String icon) {
            this.code = code;
            this.label = label;
            this.icon = icon;
        }

        public String getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }

        public String getIcon() {
            return icon;
        }

        public static WidgetType fromCode(String code) {
            for (WidgetType type : values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }
            return CONNECTIONS; // Default
        }
    }

    private Long id;
    private Long dashboardId;
    private String widgetType;
    private String title;
    private String config; // JSON string for widget-specific configuration
    private int position;
    private int width;
    private int height;

    // Transient field for rendered data
    private transient Object data;

    public CustomWidget() {
        this.widgetType = WidgetType.CONNECTIONS.getCode();
        this.config = "{}";
        this.position = 0;
        this.width = 6;
        this.height = 1;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final CustomWidget widget = new CustomWidget();

        public Builder id(Long id) {
            widget.id = id;
            return this;
        }

        public Builder dashboardId(Long dashboardId) {
            widget.dashboardId = dashboardId;
            return this;
        }

        public Builder widgetType(String widgetType) {
            widget.widgetType = widgetType;
            return this;
        }

        public Builder widgetType(WidgetType type) {
            widget.widgetType = type.getCode();
            return this;
        }

        public Builder title(String title) {
            widget.title = title;
            return this;
        }

        public Builder config(String config) {
            widget.config = config;
            return this;
        }

        public Builder position(int position) {
            widget.position = position;
            return this;
        }

        public Builder width(int width) {
            widget.width = Math.max(1, Math.min(12, width));
            return this;
        }

        public Builder height(int height) {
            widget.height = Math.max(1, height);
            return this;
        }

        public CustomWidget build() {
            return widget;
        }
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDashboardId() {
        return dashboardId;
    }

    public void setDashboardId(Long dashboardId) {
        this.dashboardId = dashboardId;
    }

    public String getWidgetType() {
        return widgetType;
    }

    public void setWidgetType(String widgetType) {
        this.widgetType = widgetType;
    }

    public WidgetType getWidgetTypeEnum() {
        return WidgetType.fromCode(widgetType);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Get the display title, using the widget type label as fallback.
     */
    public String getDisplayTitle() {
        if (title != null && !title.isBlank()) {
            return title;
        }
        return getWidgetTypeEnum().getLabel();
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = Math.max(1, Math.min(12, width));
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = Math.max(1, height);
    }

    /**
     * Get the Bootstrap column class for this widget's width.
     */
    public String getColClass() {
        return "col-md-" + width;
    }

    /**
     * Get the icon class for this widget type.
     */
    public String getIcon() {
        return getWidgetTypeEnum().getIcon();
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
