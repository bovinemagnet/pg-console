package com.bovinemagnet.pgconsole.e2e.page;

import com.microsoft.playwright.Page;

/**
 * Page Object Model for the main dashboard (overview) page of PG Console.
 * <p>
 * This class encapsulates all interactions with the dashboard page located at the root path ({@code /}).
 * The dashboard provides a comprehensive overview of PostgreSQL database health including real-time
 * metrics for connections, active queries, blocked queries, cache hit ratio, and database size.
 * It also displays sparkline visualisations for historical trends and tables showing top tables
 * and indices by size and activity.
 * <p>
 * The Page Object Model pattern is used to abstract the HTML structure and provide a maintainable
 * API for E2E tests. This allows tests to interact with the page through meaningful methods rather
 * than raw CSS selectors, improving test readability and resilience to UI changes.
 * <p>
 * Key features tested through this page object:
 * <ul>
 *     <li>Five core metric widgets (connections, active queries, blocked queries, cache hit ratio, database size)</li>
 *     <li>SVG sparkline charts showing historical metric trends</li>
 *     <li>Top tables and top indices cards with tabular data</li>
 *     <li>Widget drill-down navigation to detailed pages</li>
 * </ul>
 * <p>
 * Usage example:
 * <pre>{@code
 * DashboardPage dashboard = new DashboardPage(page, baseUrl);
 * dashboard.navigate();
 * assertTrue(dashboard.allCoreWidgetsVisible());
 * assertEquals("42", dashboard.getConnectionsValue());
 * dashboard.clickActiveQueriesWidget();
 * }</pre>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see BasePage
 * @see com.bovinemagnet.pgconsole.e2e.DashboardE2ETest
 * @since 0.0.0
 */
public class DashboardPage extends BasePage {

    /**
     * CSS selector for the connections metric widget card.
     * Uses Playwright's {@code :has-text()} pseudo-selector to locate the card by heading text.
     */
    private static final String CONNECTIONS_WIDGET = ".card:has-text('Connections')";

    /**
     * CSS selector for the active queries metric widget card.
     * Displays the current count of non-idle active queries.
     */
    private static final String ACTIVE_QUERIES_WIDGET = ".card:has-text('Active Queries')";

    /**
     * CSS selector for the blocked queries metric widget card.
     * Shows queries currently waiting on locks.
     */
    private static final String BLOCKED_QUERIES_WIDGET = ".card:has-text('Blocked')";

    /**
     * CSS selector for the cache hit ratio metric widget card.
     * Displays the percentage of queries served from cache vs. disk.
     */
    private static final String CACHE_HIT_WIDGET = ".card:has-text('Cache Hit')";

    /**
     * CSS selector for the database size metric widget card.
     * Shows the total size of the monitored database(s).
     */
    private static final String DATABASE_SIZE_WIDGET = ".card:has-text('Database Size')";

    /**
     * CSS selector for the top tables card.
     * Contains a table of the largest or most active tables.
     */
    private static final String TOP_TABLES_CARD = ".card:has-text('Top Tables')";

    /**
     * CSS selector for the top indices card.
     * Contains a table of the largest or most active indices.
     */
    private static final String TOP_INDEXES_CARD = ".card:has-text('Top Indexes')";

    /**
     * CSS selector for SVG sparkline elements.
     * Sparklines provide inline visualisations of metric trends over time.
     */
    private static final String SPARKLINE_SVG = "svg.sparkline";

    /**
     * Constructs a new DashboardPage instance for E2E testing.
     * <p>
     * This constructor delegates to {@link BasePage} to initialise the Playwright page
     * instance and base URL. The page object is then ready for navigation and interaction.
     *
     * @param page the Playwright {@link Page} instance used for browser automation
     * @param baseUrl the base URL for the application (e.g., {@code http://localhost:8080})
     * @see BasePage#BasePage(Page, String)
     */
    public DashboardPage(Page page, String baseUrl) {
        super(page, baseUrl);
    }

    /**
     * Navigates to the dashboard overview page.
     * <p>
     * Loads the root path ({@code /}) with the default database instance parameter.
     * This method waits for the page to fully load before returning.
     *
     * @see #navigateToWithInstance(String)
     */
    public void navigate() {
        navigateToWithInstance("/");
    }

    // ========== Widget Visibility ==========

    /**
     * Checks if the connections metric widget is visible on the dashboard.
     * <p>
     * The connections widget displays the current number of active database connections.
     * This is a core metric that should always be present on a healthy dashboard.
     *
     * @return {@code true} if the connections widget is visible, {@code false} otherwise
     * @see #getConnectionsValue()
     * @see #clickConnectionsWidget()
     */
    public boolean hasConnectionsWidget() {
        return isVisible(CONNECTIONS_WIDGET);
    }

    /**
     * Checks if the active queries metric widget is visible on the dashboard.
     * <p>
     * The active queries widget shows the count of currently executing (non-idle) queries.
     * This helps identify database activity levels at a glance.
     *
     * @return {@code true} if the active queries widget is visible, {@code false} otherwise
     * @see #getActiveQueriesValue()
     * @see #clickActiveQueriesWidget()
     */
    public boolean hasActiveQueriesWidget() {
        return isVisible(ACTIVE_QUERIES_WIDGET);
    }

    /**
     * Checks if the blocked queries metric widget is visible on the dashboard.
     * <p>
     * The blocked queries widget displays the number of queries waiting on locks.
     * A non-zero value indicates potential lock contention issues.
     *
     * @return {@code true} if the blocked queries widget is visible, {@code false} otherwise
     * @see #getBlockedQueriesValue()
     * @see #clickBlockedQueriesWidget()
     */
    public boolean hasBlockedQueriesWidget() {
        return isVisible(BLOCKED_QUERIES_WIDGET);
    }

    /**
     * Checks if the cache hit ratio metric widget is visible on the dashboard.
     * <p>
     * The cache hit ratio widget shows the percentage of queries served from PostgreSQL's
     * buffer cache rather than disk. Higher percentages indicate better performance.
     *
     * @return {@code true} if the cache hit ratio widget is visible, {@code false} otherwise
     * @see #getCacheHitValue()
     */
    public boolean hasCacheHitWidget() {
        return isVisible(CACHE_HIT_WIDGET);
    }

    /**
     * Checks if the database size metric widget is visible on the dashboard.
     * <p>
     * The database size widget displays the total size of the monitored database(s).
     * This helps track database growth trends over time.
     *
     * @return {@code true} if the database size widget is visible, {@code false} otherwise
     * @see #getDatabaseSizeValue()
     */
    public boolean hasDatabaseSizeWidget() {
        return isVisible(DATABASE_SIZE_WIDGET);
    }

    /**
     * Checks if the top tables card is visible on the dashboard.
     * <p>
     * The top tables card displays a ranked list of the largest or most active tables,
     * helping identify tables that may need optimisation or maintenance.
     *
     * @return {@code true} if the top tables card is visible, {@code false} otherwise
     * @see #getTopTablesCount()
     */
    public boolean hasTopTablesCard() {
        return isVisible(TOP_TABLES_CARD);
    }

    /**
     * Checks if the top indices card is visible on the dashboard.
     * <p>
     * The top indices card displays a ranked list of the largest or most active indices,
     * useful for identifying index bloat or maintenance needs.
     *
     * @return {@code true} if the top indices card is visible, {@code false} otherwise
     * @see #getTopIndexesCount()
     */
    public boolean hasTopIndexesCard() {
        return isVisible(TOP_INDEXES_CARD);
    }

    /**
     * Checks if any sparkline SVG charts are present on the dashboard.
     * <p>
     * Sparklines provide inline visualisations of metric trends over time. They are
     * generated server-side and embedded as SVG elements within metric widgets.
     * The presence of sparklines indicates that historical data sampling is enabled
     * and functioning correctly.
     *
     * @return {@code true} if at least one sparkline SVG element exists, {@code false} otherwise
     * @see com.bovinemagnet.pgconsole.service.SparklineService
     */
    public boolean hasSparklines() {
        return countElements(SPARKLINE_SVG) > 0;
    }

    // ========== Widget Values ==========

    /**
     * Retrieves the connections count displayed in the connections widget.
     * <p>
     * The value is extracted from the {@code <h2>} heading within the connections widget card.
     * This typically displays an integer representing the current number of active database connections.
     *
     * @return the connections count as a string (e.g., {@code "42"})
     * @see #hasConnectionsWidget()
     */
    public String getConnectionsValue() {
        return getInnerText(CONNECTIONS_WIDGET + " h2");
    }

    /**
     * Retrieves the active queries count displayed in the active queries widget.
     * <p>
     * The value represents the number of currently executing (non-idle) queries.
     * This is extracted from the {@code <h2>} heading element within the widget.
     *
     * @return the active queries count as a string (e.g., {@code "5"})
     * @see #hasActiveQueriesWidget()
     */
    public String getActiveQueriesValue() {
        return getInnerText(ACTIVE_QUERIES_WIDGET + " h2");
    }

    /**
     * Retrieves the blocked queries count displayed in the blocked queries widget.
     * <p>
     * The value indicates the number of queries currently waiting on locks.
     * A value greater than {@code "0"} suggests lock contention that may need investigation.
     *
     * @return the blocked queries count as a string (e.g., {@code "0"} or {@code "3"})
     * @see #hasBlockedQueriesWidget()
     */
    public String getBlockedQueriesValue() {
        return getInnerText(BLOCKED_QUERIES_WIDGET + " h2");
    }

    /**
     * Retrieves the cache hit ratio value displayed in the cache hit widget.
     * <p>
     * The value represents the percentage of queries served from PostgreSQL's buffer cache
     * rather than requiring disk I/O. Higher percentages (e.g., 99%+) indicate better performance.
     * The returned string typically includes the percentage symbol.
     *
     * @return the cache hit ratio as a string (e.g., {@code "99.2%"})
     * @see #hasCacheHitWidget()
     */
    public String getCacheHitValue() {
        return getInnerText(CACHE_HIT_WIDGET + " h2");
    }

    /**
     * Retrieves the database size value displayed in the database size widget.
     * <p>
     * The value shows the total size of the monitored database(s), typically formatted
     * with appropriate units (e.g., MB, GB). This helps track database growth over time.
     *
     * @return the database size as a formatted string (e.g., {@code "1.2 GB"})
     * @see #hasDatabaseSizeWidget()
     */
    public String getDatabaseSizeValue() {
        return getInnerText(DATABASE_SIZE_WIDGET + " h2");
    }

    // ========== Widget Click Actions ==========

    /**
     * Clicks the connections widget to navigate to detailed connection information.
     * <p>
     * This action typically navigates to the activity page showing all current database
     * connections with details such as client address, query state, and duration.
     * The method waits for the page to fully load before returning.
     *
     * @see #hasConnectionsWidget()
     * @see #getConnectionsValue()
     */
    public void clickConnectionsWidget() {
        click(CONNECTIONS_WIDGET);
        waitForPageLoad();
    }

    /**
     * Clicks the active queries widget to navigate to detailed query activity information.
     * <p>
     * This action typically navigates to the activity page filtered or highlighted to show
     * currently executing queries. The method waits for the page to fully load before returning.
     *
     * @see #hasActiveQueriesWidget()
     * @see #getActiveQueriesValue()
     */
    public void clickActiveQueriesWidget() {
        click(ACTIVE_QUERIES_WIDGET);
        waitForPageLoad();
    }

    /**
     * Clicks the blocked queries widget to navigate to detailed lock information.
     * <p>
     * This action typically navigates to the locks page showing blocked queries, lock contention,
     * and the blocking tree visualisation. The method waits for the page to fully load before returning.
     *
     * @see #hasBlockedQueriesWidget()
     * @see #getBlockedQueriesValue()
     */
    public void clickBlockedQueriesWidget() {
        click(BLOCKED_QUERIES_WIDGET);
        waitForPageLoad();
    }

    // ========== Dashboard Health ==========

    /**
     * Verifies that all core dashboard widgets are present and visible.
     * <p>
     * This is a composite check that validates the dashboard loaded correctly by confirming
     * the presence of all five essential metric widgets: connections, active queries, blocked
     * queries, cache hit ratio, and database size. This method is useful for smoke testing
     * the dashboard's basic functionality.
     *
     * @return {@code true} if all five core widgets are visible, {@code false} if any are missing
     * @see #hasConnectionsWidget()
     * @see #hasActiveQueriesWidget()
     * @see #hasBlockedQueriesWidget()
     * @see #hasCacheHitWidget()
     * @see #hasDatabaseSizeWidget()
     */
    public boolean allCoreWidgetsVisible() {
        return hasConnectionsWidget()
                && hasActiveQueriesWidget()
                && hasBlockedQueriesWidget()
                && hasCacheHitWidget()
                && hasDatabaseSizeWidget();
    }

    /**
     * Retrieves the number of table rows displayed in the top tables card.
     * <p>
     * The top tables card shows the largest or most active database tables.
     * This count excludes the header row and represents only data rows in the table body.
     *
     * @return the number of table rows (e.g., {@code 10} if showing the top 10 tables)
     * @see #hasTopTablesCard()
     */
    public int getTopTablesCount() {
        return getTableRowCount(TOP_TABLES_CARD + " table");
    }

    /**
     * Retrieves the number of index rows displayed in the top indices card.
     * <p>
     * The top indices card shows the largest or most active database indices.
     * This count excludes the header row and represents only data rows in the table body.
     *
     * @return the number of index rows (e.g., {@code 10} if showing the top 10 indices)
     * @see #hasTopIndexesCard()
     */
    public int getTopIndexesCount() {
        return getTableRowCount(TOP_INDEXES_CARD + " table");
    }
}
