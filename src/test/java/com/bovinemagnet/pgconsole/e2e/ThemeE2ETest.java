package com.bovinemagnet.pgconsole.e2e;

import com.bovinemagnet.pgconsole.e2e.page.BasePage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end tests for theme toggle functionality and persistence.
 * <p>
 * Verifies that the dark mode/light mode theme toggle functions correctly and
 * that user preferences persist across page navigations via localStorage. The
 * theme system uses Bootstrap 5's {@code data-bs-theme} attribute to control
 * the colour scheme across the entire application.
 * <p>
 * <strong>Test Prerequisites:</strong>
 * <ul>
 *   <li>Application must be running with theme toggle enabled in the topbar</li>
 *   <li>Browser must support localStorage for theme persistence</li>
 *   <li>Theme toggle button must be accessible via #themeToggle selector</li>
 *   <li>HTML element must support data-bs-theme attribute</li>
 * </ul>
 * <p>
 * <strong>Test Assumptions:</strong>
 * <ul>
 *   <li>Theme state is stored in browser localStorage with consistent key</li>
 *   <li>Default theme is light mode when no preference is stored</li>
 *   <li>Theme toggle button is visible on all dashboard pages</li>
 *   <li>Theme changes apply immediately without page reload</li>
 *   <li>Each test starts with a clean browser context</li>
 * </ul>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see BasePage
 * @see PlaywrightTestBase
 */
@DisplayName("Theme Toggle E2E Tests")
class ThemeE2ETest extends PlaywrightTestBase {

    private BasePage basePage;

    /**
     * Initialises the BasePage instance before each test.
     * <p>
     * Creates a new BasePage object configured with the current Playwright page
     * instance and the base URL. This setup ensures each test has a clean page
     * object and isolated browser context for theme testing.
     */
    @BeforeEach
    void setUpBasePage() {
        basePage = new BasePage(page, BASE_URL);
    }

    /**
     * Verifies that the theme toggle button is visible on the dashboard.
     * <p>
     * Tests the presence of the theme toggle button in the topbar navigation,
     * ensuring users can access the theme switching functionality. The button
     * should be present and visible on initial page load.
     */
    @Test
    @DisplayName("Theme toggle button is visible")
    void themeToggleVisible() {
        basePage.navigateToWithInstance("/");

        assertTrue(basePage.isVisible("#themeToggle"), "Theme toggle should be visible");
    }

    /**
     * Verifies that toggling from light mode to dark mode works correctly.
     * <p>
     * Tests the theme toggle functionality by explicitly starting in light mode
     * and clicking the toggle button. Validates that the {@code data-bs-theme}
     * attribute changes to "dark" immediately after the toggle action.
     */
    @Test
    @DisplayName("Toggle to dark mode")
    void toggleToDarkMode() {
        basePage.navigateToWithInstance("/");
        basePage.enableLightMode(); // Ensure starting from light mode

        basePage.toggleTheme();

        assertTrue(basePage.isDarkMode(), "Should be in dark mode after toggle");
    }

    /**
     * Verifies that toggling from dark mode to light mode works correctly.
     * <p>
     * Tests the theme toggle functionality by explicitly starting in dark mode
     * and clicking the toggle button. Validates that the {@code data-bs-theme}
     * attribute removes the "dark" value immediately after the toggle action.
     */
    @Test
    @DisplayName("Toggle back to light mode")
    void toggleToLightMode() {
        basePage.navigateToWithInstance("/");
        basePage.enableDarkMode(); // Ensure starting from dark mode

        basePage.toggleTheme();

        assertFalse(basePage.isDarkMode(), "Should be in light mode after toggle");
    }

    /**
     * Verifies that dark mode preference persists across page navigation.
     * <p>
     * Tests the localStorage persistence mechanism by enabling dark mode on the
     * dashboard and navigating to a different page. Validates that the theme
     * preference is restored automatically on the new page, demonstrating that
     * the client-side theme storage works correctly.
     */
    @Test
    @DisplayName("Dark mode persists across page navigation")
    void darkModePersistsAcrossNavigation() {
        basePage.navigateToWithInstance("/");
        basePage.enableDarkMode();
        assertTrue(basePage.isDarkMode(), "Should be in dark mode");

        // Navigate to another page
        basePage.navigateToWithInstance("/slow-queries");

        assertTrue(basePage.isDarkMode(), "Dark mode should persist after navigation");
    }

    /**
     * Verifies that light mode preference persists across page navigation.
     * <p>
     * Tests the localStorage persistence mechanism by explicitly enabling light mode
     * on the dashboard and navigating to a different page. Validates that the light
     * theme preference is maintained on the new page, ensuring default/light mode
     * persistence works symmetrically with dark mode.
     */
    @Test
    @DisplayName("Light mode persists across page navigation")
    void lightModePersistsAcrossNavigation() {
        basePage.navigateToWithInstance("/");
        basePage.enableLightMode();
        assertFalse(basePage.isDarkMode(), "Should be in light mode");

        // Navigate to another page
        basePage.navigateToWithInstance("/activity");

        assertFalse(basePage.isDarkMode(), "Light mode should persist after navigation");
    }

    /**
     * Verifies that theme toggling and persistence work correctly across multiple pages.
     * <p>
     * Tests a comprehensive user journey involving multiple page navigations and theme
     * toggles to ensure the theme system works reliably in realistic usage scenarios.
     * This test validates:
     * <ul>
     *   <li>Theme can be toggled on any page, not just the dashboard</li>
     *   <li>Theme preference persists when navigating between different pages</li>
     *   <li>Multiple theme toggles work correctly without state corruption</li>
     *   <li>Theme state remains consistent throughout the navigation flow</li>
     * </ul>
     */
    @Test
    @DisplayName("Theme toggle works on multiple pages")
    void themeToggleWorksOnMultiplePages() {
        // Start on dashboard
        basePage.navigateToWithInstance("/");
        basePage.enableLightMode();

        // Toggle on slow queries page
        basePage.navigateToWithInstance("/slow-queries");
        basePage.toggleTheme();
        assertTrue(basePage.isDarkMode(), "Should be dark mode on slow queries");

        // Check persistence on locks page
        basePage.navigateToWithInstance("/locks");
        assertTrue(basePage.isDarkMode(), "Dark mode should persist to locks page");

        // Toggle back on locks page
        basePage.toggleTheme();
        assertFalse(basePage.isDarkMode(), "Should be light mode after second toggle");

        // Verify persistence on dashboard
        basePage.navigateToWithInstance("/");
        assertFalse(basePage.isDarkMode(), "Light mode should persist to dashboard");
    }
}
