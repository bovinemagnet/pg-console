package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.CustomDashboard;
import com.bovinemagnet.pgconsole.model.CustomWidget;
import com.bovinemagnet.pgconsole.model.OverviewStats;
import com.bovinemagnet.pgconsole.repository.CustomDashboardRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Service for managing custom dashboards and rendering widget data.
 * Provides business logic for dashboard CRUD and widget data fetching.
 *
 * @author Paul Snow
 * @since 0.0.0
 */
@ApplicationScoped
public class CustomDashboardService {

    @Inject
    CustomDashboardRepository repository;

    @Inject
    PostgresService postgresService;

    @Inject
    SparklineService sparklineService;

    @Inject
    FeatureToggleService toggleService;

    // Pattern to validate custom SQL (only SELECT allowed)
    private static final Pattern SELECT_ONLY_PATTERN = Pattern.compile(
            "^\\s*SELECT\\s+.*",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    // Dangerous SQL keywords to block
    private static final Pattern DANGEROUS_PATTERN = Pattern.compile(
            "(INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|TRUNCATE|GRANT|REVOKE|EXECUTE|COPY|"
                    + "pg_read_file|pg_write_file|pg_terminate_backend|pg_cancel_backend)",
            Pattern.CASE_INSENSITIVE
    );

    // ========================================
    // Dashboard CRUD
    // ========================================

    /**
     * Get all dashboards for an instance.
     */
    public List<CustomDashboard> getDashboards(String instanceId) {
        if (!toggleService.isSchemaEnabled()) {
            return List.of(); // No persistence in schema-free mode
        }
        return repository.findByInstance(instanceId);
    }

    /**
     * Get a dashboard by ID with widgets.
     */
    public Optional<CustomDashboard> getDashboard(Long id) {
        if (!toggleService.isSchemaEnabled()) {
            return Optional.empty();
        }
        return repository.findById(id);
    }

    /**
     * Get a dashboard by ID, throwing NotFoundException if not found.
     */
    public CustomDashboard requireDashboard(Long id) {
        return getDashboard(id)
                .orElseThrow(() -> new NotFoundException("Dashboard not found: " + id));
    }

    /**
     * Create a new dashboard.
     */
    public CustomDashboard createDashboard(CustomDashboard dashboard) {
        if (!toggleService.isSchemaEnabled()) {
            throw new IllegalStateException("Custom dashboards require schema mode to be enabled");
        }
        return repository.create(dashboard);
    }

    /**
     * Update an existing dashboard.
     */
    public CustomDashboard updateDashboard(CustomDashboard dashboard) {
        if (!toggleService.isSchemaEnabled()) {
            throw new IllegalStateException("Custom dashboards require schema mode to be enabled");
        }
        return repository.update(dashboard);
    }

    /**
     * Delete a dashboard.
     */
    public void deleteDashboard(Long id) {
        if (!toggleService.isSchemaEnabled()) {
            throw new IllegalStateException("Custom dashboards require schema mode to be enabled");
        }
        repository.delete(id);
    }

    // ========================================
    // Widget Management
    // ========================================

    /**
     * Add a widget to a dashboard.
     */
    public CustomWidget addWidget(Long dashboardId, CustomWidget widget) {
        if (!toggleService.isSchemaEnabled()) {
            throw new IllegalStateException("Custom dashboards require schema mode to be enabled");
        }
        widget.setDashboardId(dashboardId);
        return repository.createWidget(widget);
    }

    /**
     * Update a widget.
     */
    public CustomWidget updateWidget(CustomWidget widget) {
        if (!toggleService.isSchemaEnabled()) {
            throw new IllegalStateException("Custom dashboards require schema mode to be enabled");
        }
        return repository.updateWidget(widget);
    }

    /**
     * Delete a widget.
     */
    public void deleteWidget(Long widgetId) {
        if (!toggleService.isSchemaEnabled()) {
            throw new IllegalStateException("Custom dashboards require schema mode to be enabled");
        }
        repository.deleteWidget(widgetId);
    }

    /**
     * Replace all widgets in a dashboard.
     */
    public void replaceWidgets(Long dashboardId, List<CustomWidget> widgets) {
        if (!toggleService.isSchemaEnabled()) {
            throw new IllegalStateException("Custom dashboards require schema mode to be enabled");
        }
        repository.replaceWidgets(dashboardId, widgets);
    }

    // ========================================
    // Widget Data Fetching
    // ========================================

    /**
     * Load data for all widgets in a dashboard.
     */
    public void loadWidgetData(CustomDashboard dashboard, String instanceId) {
        if (dashboard.getWidgets() == null) return;

        OverviewStats stats = postgresService.getOverviewStats(instanceId);

        for (CustomWidget widget : dashboard.getWidgets()) {
            try {
                Object data = fetchWidgetData(widget, instanceId, stats);
                widget.setData(data);
            } catch (Exception e) {
                // Log error but don't fail the whole dashboard
                widget.setData(Map.of("error", e.getMessage()));
            }
        }
    }

    /**
     * Fetch data for a single widget.
     */
    public Object fetchWidgetData(CustomWidget widget, String instanceId, OverviewStats stats) {
        if (stats == null) {
            stats = postgresService.getOverviewStats(instanceId);
        }

        final OverviewStats finalStats = stats;

        return switch (widget.getWidgetType()) {
            case "connections" -> Map.of(
                    "current", finalStats.getActiveConnections(),
                    "max", finalStats.getMaxConnections(),
                    "percentage", finalStats.getConnectionPercentage()
            );
            case "cache-ratio" -> Map.of(
                    "ratio", finalStats.getCacheHitRatio(),
                    "formatted", finalStats.getCacheHitRatioFormatted()
            );
            case "active-queries" -> Map.of(
                    "count", finalStats.getActiveQueries()
            );
            case "blocked-queries" -> Map.of(
                    "count", finalStats.getBlockedQueries()
            );
            case "db-size" -> Map.of(
                    "formatted", finalStats.getDatabaseSize()
            );
            case "longest-query" -> Map.of(
                    "formatted", finalStats.getLongestQueryDuration()
            );
            case "top-tables" -> finalStats.getTopTablesBySize() != null
                    ? finalStats.getTopTablesBySize().stream().limit(5).toList()
                    : List.of();
            case "top-indexes" -> finalStats.getTopIndexesBySize() != null
                    ? finalStats.getTopIndexesBySize().stream().limit(5).toList()
                    : List.of();
            case "sparkline-connections" -> sparklineService.getConnectionsSparkline(instanceId, 24, 120, 30);
            case "sparkline-queries" -> sparklineService.getActiveQueriesSparkline(instanceId, 24, 120, 30);
            case "transaction-rate" -> {
                // Get database metrics for the current database (postgres by default)
                var metrics = postgresService.getDatabaseMetrics(instanceId, "postgres");
                yield metrics != null
                        ? (metrics.getXactCommit() + metrics.getXactRollback())
                        : 0L;
            }
            case "tuple-stats" -> {
                // Get database metrics for the current database
                var metrics = postgresService.getDatabaseMetrics(instanceId, "postgres");
                yield metrics != null
                        ? Map.of(
                        "inserted", metrics.getTupInserted(),
                        "updated", metrics.getTupUpdated(),
                        "deleted", metrics.getTupDeleted(),
                        "fetched", metrics.getTupFetched()
                )
                        : Map.of();
            }
            case "custom-sql" -> executeCustomSql(widget, instanceId);
            default -> Map.of("error", "Unknown widget type: " + widget.getWidgetType());
        };
    }

    // ========================================
    // Custom SQL Execution
    // ========================================

    /**
     * Validate and execute a custom SQL query.
     * Only SELECT statements are allowed.
     */
    public Object executeCustomSql(CustomWidget widget, String instanceId) {
        String config = widget.getConfig();
        if (config == null || config.isBlank() || config.equals("{}")) {
            return Map.of("error", "No SQL query configured");
        }

        // Parse config to get SQL
        String sql = extractSqlFromConfig(config);
        if (sql == null || sql.isBlank()) {
            return Map.of("error", "No SQL query in configuration");
        }

        // Validate SQL is read-only
        if (!isValidSelectQuery(sql)) {
            return Map.of("error", "Only SELECT queries are allowed");
        }

        // Custom SQL widget is a placeholder for future implementation
        // For now, return validation success but note the feature requires additional setup
        return Map.of(
                "info", "Custom SQL widget validated successfully",
                "sql", sql,
                "note", "Query execution requires additional configuration"
        );
    }

    /**
     * Check if a SQL query is a valid SELECT statement.
     */
    public boolean isValidSelectQuery(String sql) {
        if (sql == null || sql.isBlank()) {
            return false;
        }

        // Must start with SELECT
        if (!SELECT_ONLY_PATTERN.matcher(sql).matches()) {
            return false;
        }

        // Must not contain dangerous keywords
        if (DANGEROUS_PATTERN.matcher(sql).find()) {
            return false;
        }

        return true;
    }

    private String extractSqlFromConfig(String config) {
        // Simple JSON parsing - extract "sql" field
        // In production, use proper JSON parsing
        if (config.contains("\"sql\"")) {
            int start = config.indexOf("\"sql\"");
            int colonIndex = config.indexOf(":", start);
            if (colonIndex > 0) {
                int valueStart = config.indexOf("\"", colonIndex);
                if (valueStart > 0) {
                    int valueEnd = config.indexOf("\"", valueStart + 1);
                    if (valueEnd > valueStart) {
                        return config.substring(valueStart + 1, valueEnd)
                                .replace("\\n", "\n")
                                .replace("\\\"", "\"");
                    }
                }
            }
        }
        return null;
    }

    // ========================================
    // Utility Methods
    // ========================================

    /**
     * Get available widget types.
     */
    public List<Map<String, String>> getAvailableWidgetTypes() {
        return List.of(
                Map.of("code", "connections", "label", "Connections", "icon", "bi-plug"),
                Map.of("code", "cache-ratio", "label", "Cache Hit Ratio", "icon", "bi-speedometer"),
                Map.of("code", "active-queries", "label", "Active Queries", "icon", "bi-play-circle"),
                Map.of("code", "blocked-queries", "label", "Blocked Queries", "icon", "bi-exclamation-triangle"),
                Map.of("code", "db-size", "label", "Database Size", "icon", "bi-hdd"),
                Map.of("code", "longest-query", "label", "Longest Query", "icon", "bi-clock-history"),
                Map.of("code", "top-tables", "label", "Top Tables", "icon", "bi-table"),
                Map.of("code", "top-indexes", "label", "Top Indexes", "icon", "bi-lightning"),
                Map.of("code", "sparkline-connections", "label", "Connection Trend", "icon", "bi-graph-up"),
                Map.of("code", "sparkline-queries", "label", "Query Trend", "icon", "bi-graph-up-arrow"),
                Map.of("code", "transaction-rate", "label", "Transaction Rate", "icon", "bi-arrow-repeat"),
                Map.of("code", "tuple-stats", "label", "Tuple Statistics", "icon", "bi-list-ol"),
                Map.of("code", "custom-sql", "label", "Custom Query", "icon", "bi-code-square")
        );
    }
}
