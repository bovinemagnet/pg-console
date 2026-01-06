package com.bovinemagnet.pgconsole.e2e.page;

import com.microsoft.playwright.Page;

/**
 * Page Object Model for the Slow Queries page of PG Console.
 * <p>
 * This class encapsulates all interactions with the slow queries page located at {@code /slow-queries}.
 * The page displays query performance data sourced from PostgreSQL's {@code pg_stat_statements} extension,
 * showing metrics such as execution count, mean execution time, total time, and rows returned.
 * <p>
 * The Page Object Model pattern is used to abstract the HTML table structure and provide a maintainable
 * API for E2E tests. This allows tests to interact with query data and sorting controls through meaningful
 * methods rather than raw CSS selectors, improving test readability and resilience to UI changes.
 * <p>
 * Key features tested through this page object:
 * <ul>
 *     <li>Tabular display of slow queries from {@code pg_stat_statements}</li>
 *     <li>Sortable columns (calls, mean time, total time, rows returned)</li>
 *     <li>Query detail drill-down navigation (clicking a query row)</li>
 *     <li>Handling of empty state when no query data is available</li>
 *     <li>Extraction of individual cell values for assertion in tests</li>
 * </ul>
 * <p>
 * The page uses server-side sorting with URL parameters (validated against an allowlist to prevent
 * SQL injection). Clicking a column header reloads the page with the appropriate sort parameter.
 * <p>
 * Usage example:
 * <pre>{@code
 * SlowQueriesPage slowQueries = new SlowQueriesPage(page, baseUrl);
 * slowQueries.navigate();
 * assertTrue(slowQueries.hasQueriesTable());
 * slowQueries.sortByTotalTime();
 * String topQuery = slowQueries.getQueryText(0);
 * slowQueries.clickFirstQuery();
 * }</pre>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see BasePage
 * @see com.bovinemagnet.pgconsole.resource.DashboardResource
 * @since 0.0.0
 */
public class SlowQueriesPage extends BasePage {

    /**
     * CSS selector for the main queries table element.
     * Uses Bootstrap's {@code .table} class for styling.
     */
    private static final String QUERIES_TABLE = "table.table";

    /**
     * CSS selector for sortable column headers.
     * Selects all anchor elements within table headers, which trigger server-side sorting.
     */
    private static final String SORT_HEADER = "th a";

    /**
     * CSS selector for table body rows.
     * Each row represents a single query from {@code pg_stat_statements}.
     */
    private static final String QUERY_ROW = "table.table tbody tr";

    /**
     * CSS selector for the query text cell (second column).
     * Uses CSS nth-child pseudo-selector to target the specific column position.
     * This cell typically displays a truncated version of the SQL query.
     */
    private static final String QUERY_TEXT_CELL = "td:nth-child(2)";

    /**
     * CSS selector for the calls count cell (third column).
     * Displays the number of times the query has been executed.
     */
    private static final String CALLS_CELL = "td:nth-child(3)";

    /**
     * CSS selector for the mean execution time cell (fourth column).
     * Shows the average execution time per call, typically in milliseconds.
     */
    private static final String MEAN_TIME_CELL = "td:nth-child(4)";

    /**
     * CSS selector for the total execution time cell (fifth column).
     * Displays the cumulative execution time across all calls.
     */
    private static final String TOTAL_TIME_CELL = "td:nth-child(5)";

    /**
     * CSS selector for query detail links within table cells.
     * Clicking these links navigates to the detailed query statistics page.
     */
    private static final String QUERY_LINK = "table.table tbody tr td a";

    /**
     * Playwright text selector for the no data message.
     * Displayed when {@code pg_stat_statements} has no data or the extension is not enabled.
     */
    private static final String NO_DATA_MESSAGE = "text=No slow queries found";

    /**
     * Constructs a new SlowQueriesPage instance for E2E testing.
     * <p>
     * This constructor delegates to {@link BasePage} to initialise the Playwright page
     * instance and base URL. The page object is then ready for navigation and interaction
     * with the slow queries page.
     *
     * @param page the Playwright {@link Page} instance used for browser automation
     * @param baseUrl the base URL for the application (e.g., {@code http://localhost:8080})
     * @see BasePage#BasePage(Page, String)
     */
    public SlowQueriesPage(Page page, String baseUrl) {
        super(page, baseUrl);
    }

    /**
     * Navigates to the slow queries page.
     * <p>
     * Loads the {@code /slow-queries} path with the default database instance parameter.
     * This method waits for the page to fully load before returning.
     *
     * @see #navigateToWithInstance(String)
     */
    public void navigate() {
        navigateToWithInstance("/slow-queries");
    }

    // ========== Table Presence ==========

    /**
     * Checks if the queries table is visible on the page.
     * <p>
     * The table displays query performance data from PostgreSQL's {@code pg_stat_statements} extension.
     * If the extension is not enabled or contains no data, this table will not be present.
     *
     * @return {@code true} if the queries table is visible, {@code false} otherwise
     * @see #hasNoDataMessage()
     * @see #getQueryCount()
     */
    public boolean hasQueriesTable() {
        return isVisible(QUERIES_TABLE);
    }

    /**
     * Checks if the no data message is displayed on the page.
     * <p>
     * This message appears when {@code pg_stat_statements} has no recorded queries, which can occur
     * if the extension is not enabled, has been recently reset, or the database has had no query activity.
     * Tests should handle both table and no-data states as valid scenarios.
     *
     * @return {@code true} if the "No slow queries found" message is shown, {@code false} otherwise
     * @see #hasQueriesTable()
     * @see #isPageValid()
     */
    public boolean hasNoDataMessage() {
        return isVisible(NO_DATA_MESSAGE);
    }

    /**
     * Retrieves the number of query rows displayed in the table.
     * <p>
     * This count represents the number of distinct queries returned from {@code pg_stat_statements}.
     * The count excludes the table header row and only includes data rows in the table body.
     * The actual number may be limited by the application's query result limit.
     *
     * @return the number of query rows (e.g., {@code 50} if showing 50 queries)
     * @see #hasQueriesTable()
     */
    public int getQueryCount() {
        return getTableRowCount(QUERIES_TABLE);
    }

    // ========== Sorting ==========

    /**
     * Sorts the table by total execution time in descending order.
     * <p>
     * Clicking the "Total Time" column header triggers a server-side sort via URL parameter.
     * The page reloads with queries ordered by their cumulative execution time, with the
     * most expensive queries first. This is useful for identifying queries that consume
     * the most database resources overall.
     * <p>
     * The sort parameter is validated server-side against an allowlist to prevent SQL injection.
     *
     * @see #sortByMeanTime()
     */
    public void sortByTotalTime() {
        click("th:has-text('Total Time')");
        waitForPageLoad();
    }

    /**
     * Sorts the table by mean (average) execution time in descending order.
     * <p>
     * Clicking the "Mean" column header triggers a server-side sort via URL parameter.
     * The page reloads with queries ordered by their average execution time per call.
     * This is useful for identifying queries that are individually slow, even if they
     * are not called frequently.
     * <p>
     * The sort parameter is validated server-side against an allowlist to prevent SQL injection.
     *
     * @see #sortByTotalTime()
     */
    public void sortByMeanTime() {
        click("th:has-text('Mean')");
        waitForPageLoad();
    }

    /**
     * Sorts the table by number of calls (executions) in descending order.
     * <p>
     * Clicking the "Calls" column header triggers a server-side sort via URL parameter.
     * The page reloads with queries ordered by execution frequency. This is useful for
     * identifying the most frequently executed queries, which may be candidates for
     * optimisation even if their individual execution time is low.
     * <p>
     * The sort parameter is validated server-side against an allowlist to prevent SQL injection.
     *
     * @see #sortByTotalTime()
     */
    public void sortByCalls() {
        click("th:has-text('Calls')");
        waitForPageLoad();
    }

    /**
     * Sorts the table by number of rows returned in descending order.
     * <p>
     * Clicking the "Rows" column header triggers a server-side sort via URL parameter.
     * The page reloads with queries ordered by the total number of rows they have returned.
     * This is useful for identifying queries that may be fetching excessive data.
     * <p>
     * The sort parameter is validated server-side against an allowlist to prevent SQL injection.
     *
     * @see #sortByCalls()
     */
    public void sortByRows() {
        click("th:has-text('Rows')");
        waitForPageLoad();
    }

    // ========== Query Details ==========

    /**
     * Clicks the first query in the table to view its detailed statistics.
     * <p>
     * This action navigates to the query detail page ({@code /slow-queries/{queryId}}), which
     * displays comprehensive statistics including execution plans, timing breakdowns, and
     * I/O metrics. The method waits for the detail page to fully load before returning.
     *
     * @see #clickQuery(int)
     */
    public void clickFirstQuery() {
        click(QUERY_LINK);
        waitForPageLoad();
    }

    /**
     * Clicks a specific query row to view its detailed statistics.
     * <p>
     * This action navigates to the query detail page ({@code /slow-queries/{queryId}}) for
     * the selected query. The index is zero-based, so {@code clickQuery(0)} clicks the first row.
     * The method waits for the detail page to fully load before returning.
     *
     * @param index the zero-based row index (0 for first row, 1 for second row, etc.)
     * @see #clickFirstQuery()
     * @see #getQueryText(int)
     */
    public void clickQuery(int index) {
        page.locator(QUERY_ROW).nth(index).locator("a").first().click();
        waitForPageLoad();
    }

    /**
     * Retrieves the query text from a specific table row.
     * <p>
     * The query text is typically truncated in the table view for readability. The full
     * query can be viewed on the query detail page. This method is useful for verifying
     * that expected queries appear in the table.
     *
     * @param index the zero-based row index (0 for first row, 1 for second row, etc.)
     * @return the truncated query text as displayed in the table (e.g., {@code "SELECT * FROM users WHERE..."})
     * @see #clickQuery(int)
     */
    public String getQueryText(int index) {
        return page.locator(QUERY_ROW).nth(index).locator(QUERY_TEXT_CELL).textContent();
    }

    /**
     * Retrieves the calls (execution) count from a specific table row.
     * <p>
     * The calls count represents the number of times the query has been executed since
     * {@code pg_stat_statements} was last reset. Higher values indicate frequently executed queries.
     *
     * @param index the zero-based row index (0 for first row, 1 for second row, etc.)
     * @return the calls count as a string (e.g., {@code "1,234"} or {@code "5"})
     * @see #getCalls(int)
     */
    public String getCalls(int index) {
        return page.locator(QUERY_ROW).nth(index).locator(CALLS_CELL).textContent();
    }

    /**
     * Retrieves the mean (average) execution time from a specific table row.
     * <p>
     * The mean time represents the average time taken per query execution, typically
     * displayed in milliseconds. This metric helps identify individually slow queries.
     *
     * @param index the zero-based row index (0 for first row, 1 for second row, etc.)
     * @return the mean execution time as a formatted string (e.g., {@code "125.4 ms"})
     * @see #getTotalTime(int)
     */
    public String getMeanTime(int index) {
        return page.locator(QUERY_ROW).nth(index).locator(MEAN_TIME_CELL).textContent();
    }

    /**
     * Retrieves the total execution time from a specific table row.
     * <p>
     * The total time represents the cumulative time spent executing this query across
     * all calls, typically displayed in milliseconds or seconds. This metric helps identify
     * queries that consume the most database resources overall.
     *
     * @param index the zero-based row index (0 for first row, 1 for second row, etc.)
     * @return the total execution time as a formatted string (e.g., {@code "45.2 s"} or {@code "1.5 h"})
     * @see #getMeanTime(int)
     */
    public String getTotalTime(int index) {
        return page.locator(QUERY_ROW).nth(index).locator(TOTAL_TIME_CELL).textContent();
    }

    // ========== Verification ==========

    /**
     * Verifies that the slow queries page loaded correctly and is in a valid state.
     * <p>
     * A valid page is one that loaded without template exceptions or server errors, and displays
     * either the queries table (when data is available) or the no data message (when
     * {@code pg_stat_statements} is empty). This method is useful for smoke testing page health.
     * <p>
     * Both the table state and the no-data state are considered valid, as the availability of
     * query data depends on database activity and extension configuration.
     *
     * @return {@code true} if the page loaded without errors and displays expected content,
     *         {@code false} if errors occurred or neither table nor message is present
     * @see #hasQueriesTable()
     * @see #hasNoDataMessage()
     * @see BasePage#pageLoadedWithoutErrors()
     */
    public boolean isPageValid() {
        return pageLoadedWithoutErrors()
                && (hasQueriesTable() || hasNoDataMessage());
    }

    /**
     * Checks if the table has sortable column headers.
     * <p>
     * Sortable headers are implemented as anchor ({@code <a>}) elements within table headers ({@code <th>}).
     * Clicking these headers triggers server-side sorting via URL parameters. The presence of sortable
     * headers indicates that the table rendered correctly with interactive sorting functionality.
     *
     * @return {@code true} if at least one sortable header link is present, {@code false} otherwise
     * @see #sortByTotalTime()
     * @see #sortByMeanTime()
     * @see #sortByCalls()
     * @see #sortByRows()
     */
    public boolean hasSortableHeaders() {
        return countElements(SORT_HEADER) > 0;
    }
}
