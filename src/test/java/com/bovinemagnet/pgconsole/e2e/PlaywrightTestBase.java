package com.bovinemagnet.pgconsole.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

import java.nio.file.Paths;

/**
 * Base class for all Playwright end-to-end tests providing browser automation infrastructure.
 * <p>
 * This abstract base class manages the full lifecycle of Playwright browser automation tests,
 * following the Page Object Model pattern to promote maintainable and reusable test code.
 * It handles browser initialisation, context isolation, and provides helper methods for
 * common test operations.
 *
 * <h2>Browser Lifecycle</h2>
 * <ul>
 *   <li>Playwright and Browser instances are created once per test class ({@code @BeforeAll})</li>
 *   <li>Each test method receives a fresh BrowserContext and Page ({@code @BeforeEach})</li>
 *   <li>This isolation prevents state leakage between tests</li>
 *   <li>All resources are properly cleaned up after tests complete</li>
 * </ul>
 *
 * <h2>Configuration</h2>
 * Test behaviour can be configured via system properties:
 * <ul>
 *   <li>{@code e2e.baseUrl} - Application base URL (default: http://localhost:8080)</li>
 *   <li>{@code e2e.browser} - Browser type: chromium, firefox, webkit (default: chromium)</li>
 *   <li>{@code e2e.headless} - Run headless (default: true)</li>
 *   <li>{@code e2e.slowMo} - Slow motion delay in milliseconds (default: 0)</li>
 *   <li>{@code e2e.timeout} - Default timeout in milliseconds (default: 30000)</li>
 *   <li>{@code e2e.screenshotDir} - Screenshot directory (default: build/e2e-screenshots)</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * public class MyDashboardTest extends PlaywrightTestBase {
 *     @Test
 *     void testDashboardLoads() {
 *         navigateTo("/");
 *         assertTrue(pageLoadedWithoutErrors());
 *         assertEquals("PostgreSQL Console", getPageTitle());
 *     }
 * }
 * }</pre>
 *
 * <h2>Test Tagging</h2>
 * All subclasses are automatically tagged with {@code @Tag("e2e")} to enable
 * selective test execution (e.g., {@code ./gradlew test --tests "*E2E*"}).
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see BasePage
 * @see DashboardE2ETest
 * @see NavigationE2ETest
 * @see ThemeE2ETest
 * @since 0.0.0
 */
@Tag("e2e")
public abstract class PlaywrightTestBase {

    /**
     * Playwright instance shared across all test methods in a test class.
     * <p>
     * Created once per test class in {@link #setUpPlaywright()} and closed in {@link #tearDownPlaywright()}.
     * This singleton approach improves test performance by avoiding repeated Playwright initialisation.
     */
    protected static Playwright playwright;

    /**
     * Browser instance shared across all test methods in a test class.
     * <p>
     * The browser type (Chromium, Firefox, or WebKit) is determined by the {@code e2e.browser}
     * system property. Created once per test class and closed after all tests complete.
     *
     * @see #BROWSER_TYPE
     */
    protected static Browser browser;

    /**
     * Browser context created fresh for each test method.
     * <p>
     * Provides test isolation by ensuring each test has its own cookies, local storage,
     * and session data. Configured with a 1920x1080 viewport and en-GB locale.
     */
    protected BrowserContext context;

    /**
     * Page instance created fresh for each test method.
     * <p>
     * Represents a single browser tab/page within the test's isolated context.
     * Automatically inherits the default timeout from the context.
     */
    protected Page page;

    /**
     * Base URL for the application under test.
     * <p>
     * Configurable via the {@code e2e.baseUrl} system property.
     * Defaults to {@code http://localhost:8080} for local development testing.
     *
     * @see #navigateTo(String)
     * @see #navigateToWithInstance(String)
     */
    protected static final String BASE_URL = System.getProperty("e2e.baseUrl", "http://localhost:8080");

    /**
     * Browser type to use for tests.
     * <p>
     * Configurable via the {@code e2e.browser} system property.
     * Supported values: {@code chromium}, {@code firefox}, {@code webkit}.
     * Defaults to {@code chromium}.
     */
    protected static final String BROWSER_TYPE = System.getProperty("e2e.browser", "chromium");

    /**
     * Whether to run the browser in headless mode.
     * <p>
     * Configurable via the {@code e2e.headless} system property.
     * Headless mode runs tests without a visible browser window, suitable for CI/CD environments.
     * Defaults to {@code true}.
     */
    protected static final boolean HEADLESS = Boolean.parseBoolean(
            System.getProperty("e2e.headless", "true"));

    /**
     * Slow motion delay in milliseconds applied between browser actions.
     * <p>
     * Configurable via the {@code e2e.slowMo} system property.
     * Useful for debugging tests by making actions visible in headed mode.
     * Defaults to {@code 0} (no delay).
     */
    protected static final int SLOW_MO = Integer.parseInt(
            System.getProperty("e2e.slowMo", "0"));

    /**
     * Default timeout in milliseconds for Playwright operations.
     * <p>
     * Configurable via the {@code e2e.timeout} system property.
     * Applied to page navigation, element selection, and other asynchronous operations.
     * Defaults to {@code 30000} (30 seconds).
     */
    protected static final int DEFAULT_TIMEOUT = Integer.parseInt(
            System.getProperty("e2e.timeout", "30000"));

    /**
     * Directory path for storing screenshots captured during tests.
     * <p>
     * Configurable via the {@code e2e.screenshotDir} system property.
     * Typically used for capturing evidence of test failures.
     * Defaults to {@code build/e2e-screenshots}.
     *
     * @see #takeScreenshot(String)
     */
    protected static final String SCREENSHOT_DIR = System.getProperty(
            "e2e.screenshotDir", "build/e2e-screenshots");

    /**
     * Initialises Playwright and the browser before all tests in the class.
     * <p>
     * This method is executed once per test class before any test methods run.
     * It creates the Playwright instance and launches the browser based on the
     * configured browser type, headless mode, and slow motion settings.
     * <p>
     * The browser instance is reused across all test methods to improve performance.
     *
     * @see #BROWSER_TYPE
     * @see #HEADLESS
     * @see #SLOW_MO
     * @see #tearDownPlaywright()
     */
    @BeforeAll
    static void setUpPlaywright() {
        playwright = Playwright.create();

        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                .setHeadless(HEADLESS)
                .setSlowMo(SLOW_MO);

        browser = switch (BROWSER_TYPE.toLowerCase()) {
            case "firefox" -> playwright.firefox().launch(options);
            case "webkit" -> playwright.webkit().launch(options);
            default -> playwright.chromium().launch(options);
        };
    }

    /**
     * Closes the browser and Playwright after all tests in the class.
     * <p>
     * This method is executed once per test class after all test methods have completed.
     * It properly releases browser and Playwright resources to prevent resource leaks.
     * Null checks ensure safe cleanup even if initialisation failed.
     *
     * @see #setUpPlaywright()
     */
    @AfterAll
    static void tearDownPlaywright() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    /**
     * Creates a new browser context and page before each test method.
     * <p>
     * This method is executed before every test method to ensure test isolation.
     * Each test receives a fresh context with its own cookies, local storage, and session data,
     * preventing state leakage between tests.
     * <p>
     * The context is configured with:
     * <ul>
     *   <li>Viewport size: 1920x1080 (Full HD resolution)</li>
     *   <li>Locale: en-GB (British English)</li>
     *   <li>Default timeout: {@link #DEFAULT_TIMEOUT}</li>
     * </ul>
     *
     * @see #tearDownPage()
     * @see #DEFAULT_TIMEOUT
     */
    @BeforeEach
    void setUpPage() {
        Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                .setViewportSize(1920, 1080)
                .setLocale("en-GB");

        context = browser.newContext(contextOptions);
        context.setDefaultTimeout(DEFAULT_TIMEOUT);

        page = context.newPage();
    }

    /**
     * Closes the page and context after each test method.
     * <p>
     * This method is executed after every test method to clean up browser resources.
     * The context is closed, which automatically closes all associated pages.
     * Null checks ensure safe cleanup even if setup failed.
     *
     * @see #setUpPage()
     */
    @AfterEach
    void tearDownPage() {
        if (context != null) {
            context.close();
        }
    }

    // ========== Helper Methods ==========

    /**
     * Navigates to a path relative to the base URL.
     * <p>
     * Constructs the full URL by prepending {@link #BASE_URL} to the provided path
     * and navigates the page to that location.
     *
     * @param path the relative path (e.g., "/slow-queries", "/activity")
     * @see #BASE_URL
     * @see #navigateToWithInstance(String)
     */
    protected void navigateTo(String path) {
        page.navigate(BASE_URL + path);
    }

    /**
     * Navigates to a path with the default instance parameter appended.
     * <p>
     * This is useful for multi-instance deployments where the default instance
     * needs to be explicitly selected via query parameter.
     *
     * @param path the relative path (e.g., "/slow-queries")
     * @see #navigateTo(String)
     */
    protected void navigateToWithInstance(String path) {
        navigateTo(path + "?instance=default");
    }

    /**
     * Takes a full-page screenshot and saves it to the screenshot directory.
     * <p>
     * The screenshot is saved as a PNG file with the specified name in the
     * directory configured by {@link #SCREENSHOT_DIR}. The full page is captured,
     * including content beyond the viewport.
     *
     * @param name the base name for the screenshot file (without extension)
     * @see #SCREENSHOT_DIR
     */
    protected void takeScreenshot(String name) {
        page.screenshot(new Page.ScreenshotOptions()
                .setPath(Paths.get(SCREENSHOT_DIR, name + ".png"))
                .setFullPage(true));
    }

    /**
     * Waits for the page to be fully loaded (network idle state).
     * <p>
     * This waits for the {@code load} event, which fires when the page and all
     * dependent resources have finished loading. Useful after navigation or
     * actions that trigger page reloads.
     */
    protected void waitForPageLoad() {
        page.waitForLoadState();
    }

    /**
     * Waits for an element matching the selector to be visible on the page.
     * <p>
     * Blocks until the element appears in the DOM and is visible, or until
     * the default timeout is reached.
     *
     * @param selector the CSS selector or text selector (e.g., "text=Submit")
     * @see #DEFAULT_TIMEOUT
     */
    protected void waitForElement(String selector) {
        page.waitForSelector(selector);
    }

    /**
     * Clicks an element and waits for navigation to complete.
     * <p>
     * This is a convenience method for clicking elements that trigger page
     * navigation (e.g., links, form submissions). It ensures the new page
     * is fully loaded before returning.
     *
     * @param selector the CSS selector or text selector
     * @see #waitForPageLoad()
     */
    protected void clickAndWait(String selector) {
        page.click(selector);
        waitForPageLoad();
    }

    /**
     * Retrieves the page title from the browser tab.
     * <p>
     * Returns the content of the {@code <title>} element in the HTML head.
     *
     * @return the current page title, never {@code null}
     */
    protected String getPageTitle() {
        return page.title();
    }

    /**
     * Checks if an element matching the selector is visible on the page.
     * <p>
     * An element is considered visible if it is in the DOM, has non-zero dimensions,
     * and is not hidden by CSS.
     *
     * @param selector the CSS selector or text selector
     * @return {@code true} if the element is visible, {@code false} otherwise
     */
    protected boolean isVisible(String selector) {
        return page.isVisible(selector);
    }

    /**
     * Retrieves the text content of an element.
     * <p>
     * Returns all text within the element, including text from child elements,
     * but excludes HTML tags. May include whitespace and hidden text.
     *
     * @param selector the CSS selector or text selector
     * @return the text content, or {@code null} if the element is not found
     * @see #getInnerText(String)
     */
    protected String getText(String selector) {
        return page.textContent(selector);
    }

    /**
     * Retrieves the inner text of an element (visible text only).
     * <p>
     * Unlike {@link #getText(String)}, this returns only the visible text,
     * respecting CSS styling and excluding hidden elements. Whitespace is normalised.
     *
     * @param selector the CSS selector or text selector
     * @return the inner text, or {@code null} if the element is not found
     * @see #getText(String)
     */
    protected String getInnerText(String selector) {
        return page.innerText(selector);
    }

    /**
     * Checks if the page HTML content contains specific text.
     * <p>
     * Performs a simple substring search on the entire page source.
     * Note that this includes text that may not be visible to the user.
     *
     * @param text the text to search for
     * @return {@code true} if the text is present in the page source, {@code false} otherwise
     */
    protected boolean pageContainsText(String text) {
        return page.content().contains(text);
    }

    /**
     * Toggles dark mode by clicking the theme toggle button.
     * <p>
     * This method interacts with the Bootstrap theme toggle to switch between
     * light and dark modes. The state is persisted in the browser's localStorage.
     *
     * @see #isDarkMode()
     */
    protected void toggleDarkMode() {
        page.click("#themeToggle");
    }

    /**
     * Checks if dark mode is currently active.
     * <p>
     * Inspects the {@code data-bs-theme} attribute on the HTML element to
     * determine the current theme state.
     *
     * @return {@code true} if dark mode is active, {@code false} if light mode is active
     * @see #toggleDarkMode()
     */
    protected boolean isDarkMode() {
        String theme = page.getAttribute("html", "data-bs-theme");
        return "dark".equals(theme);
    }

    /**
     * Sets the auto-refresh interval via the dropdown menu.
     * <p>
     * Configures how frequently the dashboard automatically refreshes its data.
     * The setting is persisted in the browser's localStorage.
     *
     * @param seconds the refresh interval in seconds (0 to disable auto-refresh,
     *                supported values: 0, 5, 10, 30, 60)
     */
    protected void setAutoRefresh(int seconds) {
        page.click("#autoRefreshDropdown");
        page.click("text=" + (seconds == 0 ? "Off" : seconds + "s"));
    }

    /**
     * Selects a database instance from the instance selector dropdown.
     * <p>
     * This method only performs the selection if the instance selector is visible
     * on the page. After selection, it waits for the page to reload with data
     * from the selected instance.
     *
     * @param instanceName the name of the database instance to select
     * @see #waitForPageLoad()
     */
    protected void selectInstance(String instanceName) {
        if (isVisible("#instanceSelect")) {
            page.selectOption("#instanceSelect", instanceName);
            waitForPageLoad();
        }
    }

    /**
     * Counts the number of rows in a table's body.
     * <p>
     * This counts only the {@code <tr>} elements within the {@code <tbody>},
     * excluding header and footer rows.
     *
     * @param tableSelector the CSS selector for the table element
     * @return the number of body rows in the table
     */
    protected int getTableRowCount(String tableSelector) {
        return page.locator(tableSelector + " tbody tr").count();
    }

    /**
     * Sorts a table by clicking on a column header.
     * <p>
     * Triggers server-side sorting by clicking a sortable table header.
     * The method waits for the page to reload with the sorted data.
     *
     * @param headerText the visible text of the column header to sort by
     * @see #waitForPageLoad()
     */
    protected void sortTableBy(String headerText) {
        page.click("th:has-text('" + headerText + "')");
        waitForPageLoad();
    }

    /**
     * Verifies the page loaded without server or template errors.
     * <p>
     * Scans the page content for common error indicators such as template exceptions,
     * HTTP 500 errors, and generic error identifiers. This is useful for smoke testing
     * to ensure pages render correctly.
     *
     * @return {@code true} if no error indicators are present, {@code false} if errors were detected
     */
    protected boolean pageLoadedWithoutErrors() {
        String content = page.content();
        return !content.contains("TemplateException")
                && !content.contains("500 - Internal Server Error")
                && !content.contains("Error id");
    }
}
