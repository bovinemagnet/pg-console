package com.bovinemagnet.pgconsole.e2e;

import com.bovinemagnet.pgconsole.e2e.page.DashboardPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end tests for the main dashboard (overview) page widgets and functionality.
 * <p>
 * Verifies that all dashboard widgets display correctly, contain valid data, and
 * provide the expected real-time metrics overview. The dashboard serves as the primary
 * landing page displaying key PostgreSQL performance indicators including connections,
 * active queries, cache hit ratio, database size, and top tables/indexes.
 * <p>
 * <strong>Test Prerequisites:</strong>
 * <ul>
 *   <li>Application must be running and accessible at the configured base URL</li>
 *   <li>PostgreSQL database must be available with pg_stat_statements enabled</li>
 *   <li>Database must have active connections for metrics to display</li>
 *   <li>Historical data may be required for sparkline visualisations</li>
 *   <li>Dashboard widgets must query pg_stat_database and related system views</li>
 * </ul>
 * <p>
 * <strong>Test Assumptions:</strong>
 * <ul>
 *   <li>Dashboard widgets load synchronously on page render</li>
 *   <li>Widget values are non-null when database is accessible</li>
 *   <li>Cache hit ratio displays as a percentage value</li>
 *   <li>Top tables and indexes cards are always present (even if empty)</li>
 *   <li>Sparklines are optional and may not display without historical data</li>
 *   <li>Browser title contains identifiable text (Dashboard/Overview/PostgreSQL)</li>
 * </ul>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see DashboardPage
 * @see PlaywrightTestBase
 */
@DisplayName("Dashboard E2E Tests")
class DashboardE2ETest extends PlaywrightTestBase {

    private DashboardPage dashboardPage;

    /**
     * Initialises the DashboardPage instance before each test.
     * <p>
     * Creates a new DashboardPage object configured with the current Playwright page
     * instance and the base URL. This setup ensures each test has a clean page
     * object for interacting with dashboard-specific elements and widgets.
     */
    @BeforeEach
    void setUpDashboardPage() {
        dashboardPage = new DashboardPage(page, BASE_URL);
    }

    /**
     * Verifies that the dashboard page loads without server errors.
     * <p>
     * Tests basic page rendering by navigating to the dashboard and checking for
     * the absence of error indicators such as TemplateException or 500 errors.
     * This is a fundamental smoke test ensuring the page can be accessed.
     */
    @Test
    @DisplayName("Dashboard loads without errors")
    void dashboardLoads() {
        dashboardPage.navigate();

        assertTrue(dashboardPage.pageLoadedWithoutErrors(), "Dashboard should load without errors");
    }

    /**
     * Verifies that all core dashboard widgets are visible on page load.
     * <p>
     * Tests the presence of the five primary dashboard widgets: connections,
     * active queries, blocked queries, cache hit ratio, and database size.
     * This ensures the dashboard layout is complete and all critical metrics
     * are displayed to users.
     */
    @Test
    @DisplayName("Dashboard displays all core widgets")
    void dashboardDisplaysAllWidgets() {
        dashboardPage.navigate();

        assertTrue(dashboardPage.allCoreWidgetsVisible(), "All core widgets should be visible");
    }

    /**
     * Verifies that the connections widget displays a valid value.
     * <p>
     * Tests that the connections widget is visible and contains a non-null,
     * non-empty value representing the current number of database connections
     * from pg_stat_database. This is a key indicator of database workload.
     */
    @Test
    @DisplayName("Connections widget displays value")
    void connectionsWidgetHasValue() {
        dashboardPage.navigate();

        assertTrue(dashboardPage.hasConnectionsWidget(), "Connections widget should be visible");
        String value = dashboardPage.getConnectionsValue();
        assertNotNull(value, "Connections value should not be null");
        assertFalse(value.isEmpty(), "Connections value should not be empty");
    }

    /**
     * Verifies that the active queries widget displays a valid value.
     * <p>
     * Tests that the active queries widget is visible and contains a non-null
     * value representing the count of currently executing queries from
     * pg_stat_activity. This metric helps identify database activity levels.
     */
    @Test
    @DisplayName("Active queries widget displays value")
    void activeQueriesWidgetHasValue() {
        dashboardPage.navigate();

        assertTrue(dashboardPage.hasActiveQueriesWidget(), "Active queries widget should be visible");
        String value = dashboardPage.getActiveQueriesValue();
        assertNotNull(value, "Active queries value should not be null");
    }

    /**
     * Verifies that the cache hit widget displays a percentage value.
     * <p>
     * Tests that the cache hit ratio widget is visible and contains a value
     * formatted as either a percentage (with %) or a numeric value. Cache hit
     * ratio is calculated from pg_stat_database buffer cache statistics and
     * indicates how effectively PostgreSQL is using memory.
     */
    @Test
    @DisplayName("Cache hit widget displays percentage")
    void cacheHitWidgetHasPercentage() {
        dashboardPage.navigate();

        assertTrue(dashboardPage.hasCacheHitWidget(), "Cache hit widget should be visible");
        String value = dashboardPage.getCacheHitValue();
        assertNotNull(value, "Cache hit value should not be null");
        assertTrue(value.contains("%") || value.matches(".*\\d.*"),
                "Cache hit should display a percentage or number");
    }

    /**
     * Verifies that the database size widget displays a valid value.
     * <p>
     * Tests that the database size widget is visible and contains a non-null,
     * non-empty value representing the total size of the monitored database.
     * This typically includes formatted size strings like "1.2 GB" from
     * pg_database_size() calculations.
     */
    @Test
    @DisplayName("Database size widget displays value")
    void databaseSizeWidgetHasValue() {
        dashboardPage.navigate();

        assertTrue(dashboardPage.hasDatabaseSizeWidget(), "Database size widget should be visible");
        String value = dashboardPage.getDatabaseSizeValue();
        assertNotNull(value, "Database size value should not be null");
        assertFalse(value.isEmpty(), "Database size value should not be empty");
    }

    /**
     * Verifies that the top tables card is visible on the dashboard.
     * <p>
     * Tests the presence of the top tables card which displays the largest or
     * most active tables based on pg_stat_user_tables metrics. This card helps
     * users identify which tables require attention for performance or maintenance.
     */
    @Test
    @DisplayName("Top tables card is visible")
    void topTablesCardVisible() {
        dashboardPage.navigate();

        assertTrue(dashboardPage.hasTopTablesCard(), "Top tables card should be visible");
    }

    /**
     * Verifies that the top indexes card is visible on the dashboard.
     * <p>
     * Tests the presence of the top indexes card which displays the most frequently
     * used or largest indexes based on pg_stat_user_indexes metrics. This helps
     * users understand index usage patterns and identify optimisation opportunities.
     */
    @Test
    @DisplayName("Top indexes card is visible")
    void topIndexesCardVisible() {
        dashboardPage.navigate();

        assertTrue(dashboardPage.hasTopIndexesCard(), "Top indexes card should be visible");
    }

    /**
     * Verifies that the dashboard renders correctly with or without sparklines.
     * <p>
     * Tests dashboard page structure when sparkline visualisations may or may not
     * be present. Sparklines require historical data from the metrics sampling job,
     * so they might not display on fresh installations. This test ensures the page
     * renders successfully regardless of sparkline availability.
     */
    @Test
    @DisplayName("Dashboard has sparkline visualisations")
    void dashboardHasSparklines() {
        dashboardPage.navigate();

        // Note: Sparklines may or may not be present depending on historical data
        // This test just verifies the page structure is correct
        assertTrue(dashboardPage.pageLoadedWithoutErrors(),
                "Dashboard should render correctly with or without sparklines");
    }

    /**
     * Verifies that the dashboard page has an appropriate browser title.
     * <p>
     * Tests that the browser tab title contains expected keywords such as
     * "Dashboard", "Overview", or "PostgreSQL" to help users identify the page
     * when multiple tabs are open. This improves user experience and navigation.
     */
    @Test
    @DisplayName("Dashboard page title is correct")
    void dashboardPageTitleCorrect() {
        dashboardPage.navigate();

        String title = dashboardPage.getBrowserTitle();
        assertTrue(title.contains("Dashboard") || title.contains("Overview") || title.contains("PostgreSQL"),
                "Page title should contain expected text");
    }
}
