package com.bovinemagnet.pgconsole.e2e.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

/**
 * Base Page Object providing common interactions and elements for all page objects.
 * <p>
 * This class implements the Page Object Model (POM) pattern, encapsulating page-specific
 * behaviour and element selectors to promote maintainable and reusable test code. It serves
 * as the foundation for all page-specific Page Objects, providing shared functionality for
 * navigation, element interaction, and access to common UI components.
 *
 * <h2>Page Object Model Pattern</h2>
 * The Page Object Model is a design pattern that:
 * <ul>
 *   <li>Encapsulates page structure and behaviour in dedicated classes</li>
 *   <li>Separates test logic from page interaction details</li>
 *   <li>Improves test maintainability by centralising element selectors</li>
 *   <li>Reduces code duplication across tests</li>
 *   <li>Makes tests more readable and resilient to UI changes</li>
 * </ul>
 *
 * <h2>Common Components</h2>
 * This base class provides access to UI components present on all or most pages:
 * <ul>
 *   <li><strong>Navigation:</strong> Sidebar links and section expansion</li>
 *   <li><strong>Theme:</strong> Light/dark mode toggle and state detection</li>
 *   <li><strong>Auto-refresh:</strong> Dropdown for configuring refresh intervals</li>
 *   <li><strong>Instance selector:</strong> Multi-instance database selection</li>
 * </ul>
 *
 * <h2>Element Interaction Helpers</h2>
 * Provides convenience methods for common Playwright operations:
 * <ul>
 *   <li>Navigation and page loading</li>
 *   <li>Element visibility checks</li>
 *   <li>Text content retrieval</li>
 *   <li>Clicking and waiting</li>
 *   <li>Table manipulation (row counting, sorting, cell access)</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * public class DashboardPage extends BasePage {
 *     private static final String CONNECTIONS_WIDGET = "#connectionsWidget";
 *
 *     public DashboardPage(Page page, String baseUrl) {
 *         super(page, baseUrl);
 *     }
 *
 *     public void open() {
 *         navigateTo("/");
 *     }
 *
 *     public int getConnectionCount() {
 *         String text = getText(CONNECTIONS_WIDGET + " .metric-value");
 *         return Integer.parseInt(text);
 *     }
 * }
 * }</pre>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see PlaywrightTestBase
 * @see DashboardPage
 * @see SlowQueriesPage
 * @since 0.0.0
 */
public class BasePage {

    /**
     * Playwright Page instance used for all browser interactions.
     * <p>
     * This is the primary interface for interacting with the browser page,
     * providing methods for navigation, element selection, and action execution.
     */
    protected final Page page;

    /**
     * Base URL for the application under test.
     * <p>
     * Used to construct full URLs for navigation. Typically configured to
     * point to {@code http://localhost:8080} for local testing or a test
     * environment URL for CI/CD pipelines.
     */
    protected final String baseUrl;

    // Common selectors

    /**
     * CSS selector for the navigation sidebar component.
     * <p>
     * The sidebar contains navigation links to all major dashboard sections
     * and is present on all pages.
     */
    protected static final String SIDEBAR = ".sidebar";

    /**
     * CSS selector for the top navigation bar component.
     * <p>
     * The topbar contains the application title, theme toggle, auto-refresh
     * dropdown, and instance selector.
     */
    protected static final String TOPBAR = ".topbar";

    /**
     * CSS selector for the theme toggle button.
     * <p>
     * Clicking this button switches between light and dark mode, with the
     * preference persisted in localStorage.
     */
    protected static final String THEME_TOGGLE = "#themeToggle";

    /**
     * CSS selector for the auto-refresh dropdown menu.
     * <p>
     * This dropdown allows users to configure automatic dashboard refresh
     * intervals (Off, 5s, 10s, 30s, 60s).
     */
    protected static final String AUTO_REFRESH_DROPDOWN = "#autoRefreshDropdown";

    /**
     * CSS selector for the database instance selection dropdown.
     * <p>
     * Used in multi-instance deployments to switch between different
     * PostgreSQL databases being monitored.
     */
    protected static final String INSTANCE_SELECT = "#instanceSelect";

    /**
     * CSS selector for the main content area of the page.
     * <p>
     * This area contains the primary dashboard content, excluding the
     * sidebar and topbar.
     */
    protected static final String MAIN_CONTENT = ".main-content";

    /**
     * CSS selector for the page title (H1 heading).
     * <p>
     * Each page typically has a prominent H1 element displaying the
     * page title (e.g., "Slow Queries", "Active Sessions").
     */
    protected static final String PAGE_TITLE = "h1";

    /**
     * Constructs a new BasePage instance.
     * <p>
     * This constructor initialises the page object with references to the
     * Playwright Page and the base URL for navigation. Subclasses should
     * call this constructor via {@code super(page, baseUrl)}.
     *
     * @param page the Playwright Page instance for browser interactions
     * @param baseUrl the base URL for the application under test
     */
    public BasePage(Page page, String baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    // ========== Navigation ==========

    /**
     * Navigates to a path relative to the base URL and waits for page load.
     * <p>
     * Constructs the full URL by appending the path to {@link #baseUrl},
     * navigates to that URL, and waits for the page to reach the load state
     * before returning.
     *
     * @param path the relative path (e.g., "/", "/slow-queries", "/activity")
     * @see #baseUrl
     * @see #waitForPageLoad()
     */
    public void navigateTo(String path) {
        page.navigate(baseUrl + path);
        waitForPageLoad();
    }

    /**
     * Navigates to a path with the default instance parameter appended.
     * <p>
     * This is useful for multi-instance deployments where the default instance
     * needs to be explicitly selected via the {@code instance=default} query parameter.
     *
     * @param path the relative path (e.g., "/slow-queries")
     * @see #navigateTo(String)
     */
    public void navigateToWithInstance(String path) {
        navigateTo(path + "?instance=default");
    }

    /**
     * Waits for the page to be fully loaded.
     * <p>
     * This waits for the {@code load} event, which fires when the page and all
     * dependent resources (stylesheets, scripts, images) have finished loading.
     */
    public void waitForPageLoad() {
        page.waitForLoadState();
    }

    /**
     * Retrieves the current page URL.
     * <p>
     * Returns the complete URL including protocol, host, path, and query parameters.
     *
     * @return the current page URL, never {@code null}
     */
    public String getCurrentUrl() {
        return page.url();
    }

    // ========== Common Interactions ==========

    /**
     * Retrieves the page title from the H1 heading element.
     * <p>
     * This returns the text of the first H1 element on the page, which typically
     * displays the main page title (e.g., "Slow Queries", "Active Sessions").
     *
     * @return the page title text, or {@code null} if no H1 element is found
     * @see #getBrowserTitle()
     */
    public String getPageTitle() {
        return page.locator(PAGE_TITLE).first().textContent();
    }

    /**
     * Retrieves the browser tab title.
     * <p>
     * Returns the content of the {@code <title>} element in the HTML head,
     * which appears in the browser tab.
     *
     * @return the browser tab title, never {@code null}
     * @see #getPageTitle()
     */
    public String getBrowserTitle() {
        return page.title();
    }

    /**
     * Verifies the page loaded without server, template, or application errors.
     * <p>
     * Scans the page content for common error indicators including:
     * <ul>
     *   <li>TemplateException (Qute template rendering errors)</li>
     *   <li>500 - Internal Server Error (HTTP 500 responses)</li>
     *   <li>Error id (generic application error pages)</li>
     *   <li>Exception (Java exception stack traces)</li>
     * </ul>
     * This is useful for smoke testing to ensure pages render correctly.
     *
     * @return {@code true} if no error indicators are present, {@code false} if errors were detected
     */
    public boolean pageLoadedWithoutErrors() {
        String content = page.content();
        return !content.contains("TemplateException")
                && !content.contains("500 - Internal Server Error")
                && !content.contains("Error id")
                && !content.contains("Exception");
    }

    // ========== Theme Toggle ==========

    /**
     * Toggles between light and dark mode.
     * <p>
     * Clicks the theme toggle button to switch themes. The new theme preference
     * is automatically persisted in the browser's localStorage.
     *
     * @see #isDarkMode()
     * @see #enableDarkMode()
     * @see #enableLightMode()
     */
    public void toggleTheme() {
        page.click(THEME_TOGGLE);
    }

    /**
     * Checks if dark mode is currently active.
     * <p>
     * Inspects the {@code data-bs-theme} attribute on the HTML element to
     * determine the current Bootstrap theme state.
     *
     * @return {@code true} if dark mode is active, {@code false} if light mode is active
     * @see #toggleTheme()
     */
    public boolean isDarkMode() {
        String theme = page.getAttribute("html", "data-bs-theme");
        return "dark".equals(theme);
    }

    /**
     * Ensures dark mode is enabled.
     * <p>
     * If dark mode is not currently active, this method toggles the theme to
     * enable it. If dark mode is already active, no action is taken.
     *
     * @see #isDarkMode()
     * @see #toggleTheme()
     */
    public void enableDarkMode() {
        if (!isDarkMode()) {
            toggleTheme();
        }
    }

    /**
     * Ensures light mode is enabled.
     * <p>
     * If light mode is not currently active (i.e., dark mode is active), this method
     * toggles the theme to enable light mode. If light mode is already active, no action is taken.
     *
     * @see #isDarkMode()
     * @see #toggleTheme()
     */
    public void enableLightMode() {
        if (isDarkMode()) {
            toggleTheme();
        }
    }

    // ========== Auto-Refresh ==========

    /**
     * Sets the auto-refresh interval via the dropdown menu.
     * <p>
     * Configures how frequently the dashboard automatically refreshes its data.
     * The setting is persisted in the browser's localStorage and applies to all
     * dashboard pages.
     *
     * @param seconds the refresh interval in seconds (0 to disable auto-refresh,
     *                supported values: 0, 5, 10, 30, 60)
     * @see #disableAutoRefresh()
     */
    public void setAutoRefresh(int seconds) {
        page.click(AUTO_REFRESH_DROPDOWN);
        String optionText = seconds == 0 ? "Off" : seconds + "s";
        page.click("text=" + optionText);
    }

    /**
     * Disables auto-refresh.
     * <p>
     * Convenience method that sets the auto-refresh interval to 0 (Off).
     *
     * @see #setAutoRefresh(int)
     */
    public void disableAutoRefresh() {
        setAutoRefresh(0);
    }

    // ========== Instance Selection ==========

    /**
     * Checks if the instance selector dropdown is visible on the page.
     * <p>
     * The instance selector is only visible in multi-instance deployments
     * where multiple PostgreSQL databases are being monitored.
     *
     * @return {@code true} if the instance selector is visible, {@code false} otherwise
     * @see #selectInstance(String)
     */
    public boolean hasInstanceSelector() {
        return page.isVisible(INSTANCE_SELECT);
    }

    /**
     * Selects a database instance from the instance selector dropdown.
     * <p>
     * This method only performs the selection if the instance selector is visible.
     * After selection, it waits for the page to reload with data from the selected
     * instance.
     *
     * @param instanceName the name of the database instance to select
     * @see #hasInstanceSelector()
     * @see #getSelectedInstance()
     * @see #waitForPageLoad()
     */
    public void selectInstance(String instanceName) {
        if (hasInstanceSelector()) {
            page.selectOption(INSTANCE_SELECT, instanceName);
            waitForPageLoad();
        }
    }

    /**
     * Retrieves the currently selected database instance name.
     * <p>
     * If the instance selector is not visible (single-instance deployment),
     * this method returns "default".
     *
     * @return the name of the currently selected instance, or "default" if no selector is present
     * @see #hasInstanceSelector()
     * @see #selectInstance(String)
     */
    public String getSelectedInstance() {
        if (hasInstanceSelector()) {
            return page.locator(INSTANCE_SELECT + " option:checked").textContent();
        }
        return "default";
    }

    // ========== Sidebar Navigation ==========

    /**
     * Checks if the navigation sidebar is visible on the page.
     * <p>
     * The sidebar contains links to all major dashboard sections and should
     * be present on all pages. This method is useful for layout verification tests.
     *
     * @return {@code true} if the sidebar is visible, {@code false} otherwise
     */
    public boolean isSidebarVisible() {
        return page.isVisible(SIDEBAR);
    }

    /**
     * Clicks a navigation link in the sidebar by its visible text and waits for navigation.
     * <p>
     * This method locates a link within the sidebar that contains the specified text,
     * clicks it, and waits for the resulting page to fully load.
     *
     * @param linkText the visible text of the navigation link (e.g., "Slow Queries", "Activity")
     * @see #waitForPageLoad()
     */
    public void clickNavLink(String linkText) {
        page.click(".sidebar a:has-text('" + linkText + "')");
        waitForPageLoad();
    }

    /**
     * Expands a collapsible navigation section by clicking its header.
     * <p>
     * Some sidebar sections may be collapsible. This method clicks the section
     * header to expand it, revealing nested navigation links. If the section is
     * not visible, no action is taken.
     *
     * @param sectionTitle the title of the navigation section to expand
     */
    public void expandNavSection(String sectionTitle) {
        Locator section = page.locator(".nav-section-title:has-text('" + sectionTitle + "')");
        if (section.isVisible()) {
            section.click();
        }
    }

    // ========== Element Helpers ==========

    /**
     * Checks if an element matching the selector is visible on the page.
     * <p>
     * An element is considered visible if it is in the DOM, has non-zero dimensions,
     * and is not hidden by CSS properties.
     *
     * @param selector the CSS selector (e.g., "#myButton", ".error-message")
     * @return {@code true} if the element is visible, {@code false} otherwise
     */
    public boolean isVisible(String selector) {
        return page.isVisible(selector);
    }

    /**
     * Retrieves the text content of an element.
     * <p>
     * Returns all text within the element, including text from child elements,
     * but excludes HTML tags. May include whitespace and text from hidden elements.
     *
     * @param selector the CSS selector
     * @return the text content, or {@code null} if the element is not found
     * @see #getInnerText(String)
     */
    public String getText(String selector) {
        return page.textContent(selector);
    }

    /**
     * Retrieves the inner text of an element (visible text only).
     * <p>
     * Unlike {@link #getText(String)}, this returns only the visible text,
     * respecting CSS styling and excluding hidden elements. Whitespace is normalised.
     *
     * @param selector the CSS selector
     * @return the inner text, or {@code null} if the element is not found
     * @see #getText(String)
     */
    public String getInnerText(String selector) {
        return page.innerText(selector);
    }

    /**
     * Clicks an element matching the selector.
     * <p>
     * Waits for the element to be actionable (visible, stable, enabled) before clicking.
     *
     * @param selector the CSS selector
     */
    public void click(String selector) {
        page.click(selector);
    }

    /**
     * Waits for an element to be visible on the page.
     * <p>
     * Blocks until the element appears in the DOM and is visible, or until
     * the page's default timeout is reached. This is useful when waiting for
     * dynamically loaded content.
     *
     * @param selector the CSS selector
     */
    public void waitForElement(String selector) {
        page.waitForSelector(selector, new Page.WaitForSelectorOptions()
                .setState(WaitForSelectorState.VISIBLE));
    }

    /**
     * Counts the number of elements matching the selector.
     * <p>
     * Returns the count of all matching elements, regardless of visibility.
     *
     * @param selector the CSS selector
     * @return the number of matching elements (0 if none found)
     */
    public int countElements(String selector) {
        return page.locator(selector).count();
    }

    // ========== Table Helpers ==========

    /**
     * Counts the number of rows in a table's body.
     * <p>
     * This counts only the {@code <tr>} elements within the {@code <tbody>},
     * excluding header and footer rows. Useful for verifying query results
     * and table content.
     *
     * @param tableSelector the CSS selector for the table element
     * @return the number of body rows in the table
     */
    public int getTableRowCount(String tableSelector) {
        return page.locator(tableSelector + " tbody tr").count();
    }

    /**
     * Sorts a table by clicking on a column header and waits for the page to reload.
     * <p>
     * Many dashboard tables support server-side sorting. This method triggers
     * sorting by clicking a column header that contains the specified text,
     * then waits for the page to reload with the sorted data.
     *
     * @param headerText the visible text of the column header to sort by
     * @see #waitForPageLoad()
     */
    public void sortTableBy(String headerText) {
        page.click("th:has-text('" + headerText + "')");
        waitForPageLoad();
    }

    /**
     * Retrieves the text content of a specific table cell.
     * <p>
     * Locates a cell by its row and column indices (both 0-based) and returns
     * its text content. Only considers cells within the {@code <tbody>}.
     *
     * @param tableSelector the CSS selector for the table element
     * @param row the row index (0-based, first data row is 0)
     * @param col the column index (0-based, first column is 0)
     * @return the text content of the cell, or {@code null} if the cell doesn't exist
     */
    public String getTableCell(String tableSelector, int row, int col) {
        return page.locator(tableSelector + " tbody tr").nth(row)
                .locator("td").nth(col).textContent();
    }
}
