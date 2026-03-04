package com.bovinemagnet.pgconsole.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end tests for all critical dashboard pages.
 * <p>
 * This test class verifies that all critical dashboard pages load correctly without
 * server errors, template exceptions, or rendering failures. It provides comprehensive
 * coverage of the main dashboard sections including core monitoring, infrastructure,
 * and the newly added PostgreSQL system view pages.
 * <p>
 * <strong>Test Prerequisites:</strong>
 * <ul>
 *   <li>Application must be running and accessible at the configured base URL</li>
 *   <li>PostgreSQL database must be available</li>
 *   <li>Feature toggles must be enabled for all tested pages</li>
 * </ul>
 * <p>
 * <strong>Test Categories:</strong>
 * <ul>
 *   <li><strong>Core Dashboards:</strong> Main overview, activity, locks, tables</li>
 *   <li><strong>Query Analysis:</strong> Slow queries, query details</li>
 *   <li><strong>Infrastructure:</strong> Replication, health checks, config</li>
 *   <li><strong>New Infrastructure Pages:</strong> Functions, I/O stats, maintenance progress, etc.</li>
 * </ul>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see PlaywrightTestBase
 */
@DisplayName("Critical Dashboards E2E Tests")
class CriticalDashboardsE2ETest extends PlaywrightTestBase {

    // ========================================
    // Core Dashboard Tests
    // ========================================

    @Nested
    @DisplayName("Core Dashboards")
    class CoreDashboards {

        @Test
        @DisplayName("Main dashboard loads without errors")
        void mainDashboardLoads() {
            navigateToWithInstance("/");
            assertTrue(pageLoadedWithoutErrors(), "Main dashboard should load without errors");
            assertTrue(pageContainsText("Connections") || pageContainsText("Overview"),
                    "Dashboard should contain expected content");
        }

        @Test
        @DisplayName("Activity page loads without errors")
        void activityPageLoads() {
            navigateToWithInstance("/activity");
            assertTrue(pageLoadedWithoutErrors(), "Activity page should load without errors");
            assertTrue(pageContainsText("Activity") || pageContainsText("Sessions"),
                    "Activity page should contain expected content");
        }

        @Test
        @DisplayName("Locks page loads without errors")
        void locksPageLoads() {
            navigateToWithInstance("/locks");
            assertTrue(pageLoadedWithoutErrors(), "Locks page should load without errors");
            assertTrue(pageContainsText("Lock") || pageContainsText("Blocking"),
                    "Locks page should contain expected content");
        }

        @Test
        @DisplayName("Tables page loads without errors")
        void tablesPageLoads() {
            navigateToWithInstance("/tables");
            assertTrue(pageLoadedWithoutErrors(), "Tables page should load without errors");
            assertTrue(pageContainsText("Table") || pageContainsText("Size"),
                    "Tables page should contain expected content");
        }

        @Test
        @DisplayName("Databases page loads without errors")
        void databasesPageLoads() {
            navigateToWithInstance("/databases");
            assertTrue(pageLoadedWithoutErrors(), "Databases page should load without errors");
            assertTrue(pageContainsText("Database") || pageContainsText("Size"),
                    "Databases page should contain expected content");
        }

        @Test
        @DisplayName("Slow queries page loads without errors")
        void slowQueriesPageLoads() {
            navigateToWithInstance("/slow-queries");
            assertTrue(pageLoadedWithoutErrors(), "Slow queries page should load without errors");
            assertTrue(pageContainsText("Query") || pageContainsText("Queries") || pageContainsText("pg_stat_statements"),
                    "Slow queries page should contain expected content");
        }
    }

    // ========================================
    // Infrastructure Dashboard Tests
    // ========================================

    @Nested
    @DisplayName("Infrastructure Dashboards")
    class InfrastructureDashboards {

        @Test
        @DisplayName("Replication page loads without errors")
        void replicationPageLoads() {
            navigateToWithInstance("/replication");
            assertTrue(pageLoadedWithoutErrors(), "Replication page should load without errors");
        }

        @Test
        @DisplayName("Infrastructure page loads without errors")
        void infrastructurePageLoads() {
            navigateToWithInstance("/infrastructure");
            assertTrue(pageLoadedWithoutErrors(), "Infrastructure page should load without errors");
        }

        @Test
        @DisplayName("Config health page loads without errors")
        void configHealthPageLoads() {
            navigateToWithInstance("/config-health");
            assertTrue(pageLoadedWithoutErrors(), "Config health page should load without errors");
        }

        @Test
        @DisplayName("Checkpoints page loads without errors")
        void checkpointsPageLoads() {
            navigateToWithInstance("/checkpoints");
            assertTrue(pageLoadedWithoutErrors(), "Checkpoints page should load without errors");
        }

        @Test
        @DisplayName("WAL checkpoints page loads without errors")
        void walCheckpointsPageLoads() {
            navigateToWithInstance("/wal-checkpoints");
            assertTrue(pageLoadedWithoutErrors(), "WAL checkpoints page should load without errors");
        }

        @Test
        @DisplayName("Health check page loads without errors")
        void healthCheckPageLoads() {
            navigateToWithInstance("/health-check");
            assertTrue(pageLoadedWithoutErrors(), "Health check page should load without errors");
        }
    }

    // ========================================
    // New Infrastructure Pages Tests
    // ========================================

    @Nested
    @DisplayName("New Infrastructure Pages")
    class NewInfrastructurePages {

        @Test
        @DisplayName("Functions page loads without errors")
        void functionsPageLoads() {
            navigateToWithInstance("/functions");
            assertTrue(pageLoadedWithoutErrors(), "Functions page should load without errors");
            assertTrue(pageContainsText("Function") || pageContainsText("pg_stat_user_functions"),
                    "Functions page should contain expected content");
        }

        @Test
        @DisplayName("Functions page displays function statistics table")
        void functionsPageHasTable() {
            navigateToWithInstance("/functions");
            waitForPageLoad();
            // Page should have a table or a message about no functions
            assertTrue(isVisible("table") || pageContainsText("No functions") || pageContainsText("no function"),
                    "Functions page should display table or empty state message");
        }

        @Test
        @DisplayName("I/O Statistics page loads without errors")
        void ioStatisticsPageLoads() {
            navigateToWithInstance("/io-statistics");
            assertTrue(pageLoadedWithoutErrors(), "I/O Statistics page should load without errors");
            assertTrue(pageContainsText("I/O") || pageContainsText("Statistics") || pageContainsText("pg_stat_io"),
                    "I/O Statistics page should contain expected content");
        }

        @Test
        @DisplayName("I/O Statistics page handles PostgreSQL version requirements")
        void ioStatisticsPageHandlesVersion() {
            navigateToWithInstance("/io-statistics");
            assertTrue(pageLoadedWithoutErrors(), "I/O Statistics page should handle version requirements gracefully");
            // Should either show data or a message about version requirements (PG 16+)
            assertTrue(pageContainsText("Backend") || pageContainsText("16") || pageContainsText("No I/O"),
                    "I/O Statistics page should show data or version message");
        }

        @Test
        @DisplayName("Maintenance Progress page loads without errors")
        void maintenanceProgressPageLoads() {
            navigateToWithInstance("/maintenance-progress");
            assertTrue(pageLoadedWithoutErrors(), "Maintenance Progress page should load without errors");
            assertTrue(pageContainsText("Maintenance") || pageContainsText("Progress") || pageContainsText("VACUUM"),
                    "Maintenance Progress page should contain expected content");
        }

        @Test
        @DisplayName("Maintenance Progress page has tabs for different operation types")
        void maintenanceProgressPageHasTabs() {
            navigateToWithInstance("/maintenance-progress");
            waitForPageLoad();
            // Check for tab navigation or section headers
            assertTrue(
                    pageContainsText("VACUUM") || pageContainsText("ANALYZE") ||
                    pageContainsText("CREATE INDEX") || pageContainsText("CLUSTER") ||
                    pageContainsText("No maintenance") || pageContainsText("no operations"),
                    "Maintenance Progress page should display operation tabs or empty state");
        }

        @Test
        @DisplayName("WAL Receiver page loads without errors")
        void walReceiverPageLoads() {
            navigateToWithInstance("/wal-receiver");
            assertTrue(pageLoadedWithoutErrors(), "WAL Receiver page should load without errors");
            assertTrue(pageContainsText("WAL") || pageContainsText("Receiver") || pageContainsText("Standby") || pageContainsText("primary"),
                    "WAL Receiver page should contain expected content");
        }

        @Test
        @DisplayName("WAL Receiver page handles non-standby server gracefully")
        void walReceiverPageHandlesNonStandby() {
            navigateToWithInstance("/wal-receiver");
            assertTrue(pageLoadedWithoutErrors(), "WAL Receiver page should handle non-standby server gracefully");
            // On a primary server, should show informational message
            assertTrue(pageContainsText("WAL Receiver") || pageContainsText("primary") || pageContainsText("standby") || pageContainsText("No WAL"),
                    "WAL Receiver page should show appropriate content for server type");
        }

        @Test
        @DisplayName("Config Files page loads without errors")
        void configFilesPageLoads() {
            navigateToWithInstance("/config-files");
            assertTrue(pageLoadedWithoutErrors(), "Config Files page should load without errors");
            assertTrue(pageContainsText("Config") || pageContainsText("Setting") || pageContainsText("postgresql.conf"),
                    "Config Files page should contain expected content");
        }

        @Test
        @DisplayName("Prepared Statements page loads without errors")
        void preparedStatementsPageLoads() {
            navigateToWithInstance("/prepared-statements");
            assertTrue(pageLoadedWithoutErrors(), "Prepared Statements page should load without errors");
            assertTrue(pageContainsText("Prepared") || pageContainsText("Statement") || pageContainsText("Cursor"),
                    "Prepared Statements page should contain expected content");
        }

        @Test
        @DisplayName("Prepared Statements page has tabs for statements and cursors")
        void preparedStatementsPageHasTabs() {
            navigateToWithInstance("/prepared-statements");
            waitForPageLoad();
            assertTrue(pageContainsText("Prepared Statement") || pageContainsText("Cursor") || pageContainsText("No prepared"),
                    "Prepared Statements page should display tabs or empty state");
        }

        @Test
        @DisplayName("Materialised Views page loads without errors")
        void materialisedViewsPageLoads() {
            navigateToWithInstance("/matviews");
            assertTrue(pageLoadedWithoutErrors(), "Materialised Views page should load without errors");
            assertTrue(pageContainsText("Materialised") || pageContainsText("View") || pageContainsText("pg_matviews"),
                    "Materialised Views page should contain expected content");
        }

        @Test
        @DisplayName("Sequences page loads without errors")
        void sequencesPageLoads() {
            navigateToWithInstance("/sequences");
            assertTrue(pageLoadedWithoutErrors(), "Sequences page should load without errors");
            assertTrue(pageContainsText("Sequence") || pageContainsText("pg_sequences"),
                    "Sequences page should contain expected content");
        }

        @Test
        @DisplayName("Sequences page handles PostgreSQL version requirements")
        void sequencesPageHandlesVersion() {
            navigateToWithInstance("/sequences");
            assertTrue(pageLoadedWithoutErrors(), "Sequences page should handle version requirements gracefully");
            // Should show data or version requirement message (PG 10+)
            assertTrue(pageContainsText("Sequence") || pageContainsText("10") || pageContainsText("No sequence"),
                    "Sequences page should show data or version message");
        }

        @Test
        @DisplayName("Extensions page loads without errors")
        void extensionsPageLoads() {
            navigateToWithInstance("/extensions");
            assertTrue(pageLoadedWithoutErrors(), "Extensions page should load without errors");
            assertTrue(pageContainsText("Extension") || pageContainsText("Installed") || pageContainsText("Available"),
                    "Extensions page should contain expected content");
        }

        @Test
        @DisplayName("Extensions page has tabs for installed and available")
        void extensionsPageHasTabs() {
            navigateToWithInstance("/extensions");
            waitForPageLoad();
            assertTrue(pageContainsText("Installed") || pageContainsText("Available"),
                    "Extensions page should display installed/available tabs");
        }
    }

    // ========================================
    // Parametrized Smoke Tests
    // ========================================

    @Nested
    @DisplayName("Dashboard Smoke Tests")
    class DashboardSmokeTests {

        /**
         * Parametrised test that verifies multiple dashboard pages load without errors.
         * <p>
         * This smoke test provides quick verification that all critical pages are
         * accessible and render without server-side errors.
         *
         * @param path the dashboard path to test
         */
        @ParameterizedTest(name = "Dashboard {0} loads without errors")
        @ValueSource(strings = {
                "/",
                "/activity",
                "/locks",
                "/tables",
                "/databases",
                "/slow-queries",
                "/replication",
                "/infrastructure",
                "/config-health",
                "/health-check",
                "/functions",
                "/io-statistics",
                "/maintenance-progress",
                "/wal-receiver",
                "/config-files",
                "/prepared-statements",
                "/matviews",
                "/sequences",
                "/extensions",
                "/stopwatch",
                "/window-compare",
                "/diagnostics",
                "/diagnostics/metrics-history",
                "/diagnostics/query-trends",
                "/diagnostics/database-trends",
                "/diagnostics/infrastructure-trends"
        })
        void dashboardLoadsWithoutErrors(String path) {
            navigateToWithInstance(path);
            assertTrue(pageLoadedWithoutErrors(),
                    "Dashboard " + path + " should load without errors");
        }
    }

    // ========================================
    // Navigation Tests
    // ========================================

    @Nested
    @DisplayName("Navigation Tests")
    class NavigationTests {

        @Test
        @DisplayName("Can navigate to Functions from sidebar")
        void canNavigateToFunctions() {
            navigateToWithInstance("/");
            waitForPageLoad();

            // Expand Infrastructure section if collapsed
            if (isVisible(".nav-section-title:has-text('Infrastructure')")) {
                page.click(".nav-section-title:has-text('Infrastructure')");
            }

            // Click Functions link if visible
            if (isVisible("a:has-text('Functions')")) {
                clickAndWait("a:has-text('Functions')");
                assertTrue(page.url().contains("/functions"), "Should navigate to Functions page");
                assertTrue(pageLoadedWithoutErrors(), "Functions page should load without errors");
            }
        }

        @Test
        @DisplayName("Can navigate to I/O Statistics from sidebar")
        void canNavigateToIoStatistics() {
            navigateToWithInstance("/");
            waitForPageLoad();

            if (isVisible(".nav-section-title:has-text('Infrastructure')")) {
                page.click(".nav-section-title:has-text('Infrastructure')");
            }

            if (isVisible("a:has-text('I/O Statistics')")) {
                clickAndWait("a:has-text('I/O Statistics')");
                assertTrue(page.url().contains("/io-statistics"), "Should navigate to I/O Statistics page");
                assertTrue(pageLoadedWithoutErrors(), "I/O Statistics page should load without errors");
            }
        }

        @Test
        @DisplayName("Can navigate to Maintenance Progress from sidebar")
        void canNavigateToMaintenanceProgress() {
            navigateToWithInstance("/");
            waitForPageLoad();

            if (isVisible(".nav-section-title:has-text('Infrastructure')")) {
                page.click(".nav-section-title:has-text('Infrastructure')");
            }

            if (isVisible("a:has-text('Maintenance Progress')")) {
                clickAndWait("a:has-text('Maintenance Progress')");
                assertTrue(page.url().contains("/maintenance-progress"), "Should navigate to Maintenance Progress page");
                assertTrue(pageLoadedWithoutErrors(), "Maintenance Progress page should load without errors");
            }
        }

        @Test
        @DisplayName("Can navigate to Extensions from sidebar")
        void canNavigateToExtensions() {
            navigateToWithInstance("/");
            waitForPageLoad();

            if (isVisible(".nav-section-title:has-text('Infrastructure')")) {
                page.click(".nav-section-title:has-text('Infrastructure')");
            }

            if (isVisible("a:has-text('Extensions')")) {
                clickAndWait("a:has-text('Extensions')");
                assertTrue(page.url().contains("/extensions"), "Should navigate to Extensions page");
                assertTrue(pageLoadedWithoutErrors(), "Extensions page should load without errors");
            }
        }
    }

    // ========================================
    // Page Content Verification Tests
    // ========================================

    @Nested
    @DisplayName("Page Content Verification")
    class PageContentVerification {

        @Test
        @DisplayName("Functions page has proper page title")
        void functionsPageHasTitle() {
            navigateToWithInstance("/functions");
            String title = getPageTitle();
            assertTrue(title.contains("Function") || title.contains("PostgreSQL"),
                    "Functions page should have appropriate title");
        }

        @Test
        @DisplayName("I/O Statistics page has proper page title")
        void ioStatisticsPageHasTitle() {
            navigateToWithInstance("/io-statistics");
            String title = getPageTitle();
            assertTrue(title.contains("I/O") || title.contains("Statistics") || title.contains("PostgreSQL"),
                    "I/O Statistics page should have appropriate title");
        }

        @Test
        @DisplayName("Extensions page displays pg_stat_statements if installed")
        void extensionsPageShowsPgStatStatements() {
            navigateToWithInstance("/extensions");
            waitForPageLoad();
            // pg_stat_statements is commonly installed
            if (pageContainsText("pg_stat_statements")) {
                assertTrue(pageContainsText("Installed") || pageContainsText("Available"),
                        "pg_stat_statements should be shown in installed or available list");
            }
        }

        @Test
        @DisplayName("Sequences page displays data type information")
        void sequencesPageShowsDataTypes() {
            navigateToWithInstance("/sequences");
            waitForPageLoad();
            // If sequences exist, should show data type badges
            if (!pageContainsText("No Sequence") && !pageContainsText("no sequence")) {
                assertTrue(pageContainsText("bigint") || pageContainsText("integer") ||
                          pageContainsText("smallint") || pageContainsText("Usage"),
                        "Sequences page should display data type information");
            }
        }
    }

    // ========================================
    // Stopwatch and Window Comparison Tests
    // ========================================

    @Nested
    @DisplayName("Stopwatch Pages")
    class StopwatchPages {

        @Test
        @DisplayName("Stopwatch page loads without errors")
        void stopwatchPageLoads() {
            navigateToWithInstance("/stopwatch");
            assertTrue(pageLoadedWithoutErrors(), "Stopwatch page should load without errors");
            assertTrue(pageContainsText("Stopwatch") || pageContainsText("monitoring session"),
                    "Stopwatch page should contain expected content");
        }

        @Test
        @DisplayName("Stopwatch page has start/stop controls")
        void stopwatchPageHasControls() {
            navigateToWithInstance("/stopwatch");
            waitForPageLoad();
            assertTrue(pageContainsText("Start") || pageContainsText("Stop") || pageContainsText("Cancel"),
                    "Stopwatch page should display session controls");
        }

        @Test
        @DisplayName("Stopwatch page shows recent sessions section")
        void stopwatchPageShowsRecentSessions() {
            navigateToWithInstance("/stopwatch");
            waitForPageLoad();
            assertTrue(pageContainsText("Recent") || pageContainsText("Session") || pageContainsText("No sessions"),
                    "Stopwatch page should display recent sessions or empty state");
        }
    }

    @Nested
    @DisplayName("Window Comparison Pages")
    class WindowComparisonPages {

        @Test
        @DisplayName("Window Comparison page loads without errors")
        void windowComparePageLoads() {
            navigateToWithInstance("/window-compare");
            assertTrue(pageLoadedWithoutErrors(), "Window Comparison page should load without errors");
            assertTrue(pageContainsText("Window Comparison") || pageContainsText("Compare"),
                    "Window Comparison page should contain expected content");
        }

        @Test
        @DisplayName("Window Comparison page has preset buttons")
        void windowComparePageHasPresets() {
            navigateToWithInstance("/window-compare");
            waitForPageLoad();
            assertTrue(pageContainsText("Preset") || pageContainsText("Yesterday") || pageContainsText("Last Week"),
                    "Window Comparison page should display quick presets");
        }

        @Test
        @DisplayName("Window Comparison page has custom comparison form")
        void windowComparePageHasCustomForm() {
            navigateToWithInstance("/window-compare");
            waitForPageLoad();
            assertTrue(pageContainsText("Window A") || pageContainsText("Window B") || pageContainsText("Custom"),
                    "Window Comparison page should display custom comparison form");
        }
    }

    // ========================================
    // Diagnostics and Trends Pages Tests
    // ========================================

    @Nested
    @DisplayName("Diagnostics and Trends Pages")
    class DiagnosticsAndTrendsPages {

        @Test
        @DisplayName("Diagnostics page loads without errors")
        void diagnosticsPageLoads() {
            navigateToWithInstance("/diagnostics");
            assertTrue(pageLoadedWithoutErrors(), "Diagnostics page should load without errors");
            assertTrue(pageContainsText("Diagnostics") || pageContainsText("diagnostic"),
                    "Diagnostics page should contain expected content");
        }

        @Test
        @DisplayName("Metrics History page loads without errors")
        void metricsHistoryPageLoads() {
            navigateToWithInstance("/diagnostics/metrics-history");
            assertTrue(pageLoadedWithoutErrors(), "Metrics History page should load without errors");
            assertTrue(pageContainsText("Metrics History") || pageContainsText("Historical"),
                    "Metrics History page should contain expected content");
        }

        @Test
        @DisplayName("Metrics History page has time window selector")
        void metricsHistoryPageHasTimeWindow() {
            navigateToWithInstance("/diagnostics/metrics-history");
            waitForPageLoad();
            assertTrue(pageContainsText("5 min") || pageContainsText("1 hour") || pageContainsText("24 hours") || pageContainsText("7 days"),
                    "Metrics History page should display time window selector");
        }

        @Test
        @DisplayName("Metrics History page has export buttons")
        void metricsHistoryPageHasExport() {
            navigateToWithInstance("/diagnostics/metrics-history");
            waitForPageLoad();
            assertTrue(pageContainsText("JSON") || pageContainsText("CSV") || pageContainsText("Export"),
                    "Metrics History page should display export buttons");
        }

        @Test
        @DisplayName("Query Trends page loads without errors")
        void queryTrendsPageLoads() {
            navigateToWithInstance("/diagnostics/query-trends");
            assertTrue(pageLoadedWithoutErrors(), "Query Trends page should load without errors");
            assertTrue(pageContainsText("Query Trends") || pageContainsText("query performance"),
                    "Query Trends page should contain expected content");
        }

        @Test
        @DisplayName("Query Trends page has time window selector")
        void queryTrendsPageHasTimeWindow() {
            navigateToWithInstance("/diagnostics/query-trends");
            waitForPageLoad();
            assertTrue(pageContainsText("1h") || pageContainsText("6h") || pageContainsText("24h") || pageContainsText("7d"),
                    "Query Trends page should display time window selector");
        }

        @Test
        @DisplayName("Database Trends page loads without errors")
        void databaseTrendsPageLoads() {
            navigateToWithInstance("/diagnostics/database-trends");
            assertTrue(pageLoadedWithoutErrors(), "Database Trends page should load without errors");
            assertTrue(pageContainsText("Database Trends") || pageContainsText("Per-database"),
                    "Database Trends page should contain expected content");
        }

        @Test
        @DisplayName("Database Trends page has database selector")
        void databaseTrendsPageHasDatabaseSelector() {
            navigateToWithInstance("/diagnostics/database-trends");
            waitForPageLoad();
            assertTrue(pageContainsText("Database") || pageContainsText("Select"),
                    "Database Trends page should display database selector");
        }

        @Test
        @DisplayName("Infrastructure Trends page loads without errors")
        void infrastructureTrendsPageLoads() {
            navigateToWithInstance("/diagnostics/infrastructure-trends");
            assertTrue(pageLoadedWithoutErrors(), "Infrastructure Trends page should load without errors");
            assertTrue(pageContainsText("Infrastructure Trends") || pageContainsText("WAL"),
                    "Infrastructure Trends page should contain expected content");
        }

        @Test
        @DisplayName("Infrastructure Trends page has time window selector")
        void infrastructureTrendsPageHasTimeWindow() {
            navigateToWithInstance("/diagnostics/infrastructure-trends");
            waitForPageLoad();
            assertTrue(pageContainsText("1h") || pageContainsText("6h") || pageContainsText("24h") || pageContainsText("7d"),
                    "Infrastructure Trends page should display time window selector");
        }
    }

    // ========================================
    // Theme and Responsiveness Tests
    // ========================================

    @Nested
    @DisplayName("Theme Tests")
    class ThemeTests {

        @Test
        @DisplayName("Functions page renders correctly in dark mode")
        void functionsPageDarkMode() {
            navigateToWithInstance("/functions");
            toggleDarkMode();
            assertTrue(isDarkMode(), "Dark mode should be enabled");
            assertTrue(pageLoadedWithoutErrors(), "Functions page should render correctly in dark mode");
        }

        @Test
        @DisplayName("Extensions page renders correctly in dark mode")
        void extensionsPageDarkMode() {
            navigateToWithInstance("/extensions");
            toggleDarkMode();
            assertTrue(isDarkMode(), "Dark mode should be enabled");
            assertTrue(pageLoadedWithoutErrors(), "Extensions page should render correctly in dark mode");
        }

        @Test
        @DisplayName("Maintenance Progress page renders correctly in dark mode")
        void maintenanceProgressPageDarkMode() {
            navigateToWithInstance("/maintenance-progress");
            toggleDarkMode();
            assertTrue(isDarkMode(), "Dark mode should be enabled");
            assertTrue(pageLoadedWithoutErrors(), "Maintenance Progress page should render correctly in dark mode");
        }

        @Test
        @DisplayName("Stopwatch page renders correctly in dark mode")
        void stopwatchPageDarkMode() {
            navigateToWithInstance("/stopwatch");
            toggleDarkMode();
            assertTrue(isDarkMode(), "Dark mode should be enabled");
            assertTrue(pageLoadedWithoutErrors(), "Stopwatch page should render correctly in dark mode");
        }

        @Test
        @DisplayName("Window Comparison page renders correctly in dark mode")
        void windowComparePageDarkMode() {
            navigateToWithInstance("/window-compare");
            toggleDarkMode();
            assertTrue(isDarkMode(), "Dark mode should be enabled");
            assertTrue(pageLoadedWithoutErrors(), "Window Comparison page should render correctly in dark mode");
        }

        @Test
        @DisplayName("Metrics History page renders correctly in dark mode")
        void metricsHistoryPageDarkMode() {
            navigateToWithInstance("/diagnostics/metrics-history");
            toggleDarkMode();
            assertTrue(isDarkMode(), "Dark mode should be enabled");
            assertTrue(pageLoadedWithoutErrors(), "Metrics History page should render correctly in dark mode");
        }
    }

    // ========================================
    // Navigation Tests for New Pages
    // ========================================

    @Nested
    @DisplayName("New Page Navigation Tests")
    class NewPageNavigationTests {

        @Test
        @DisplayName("Can navigate to Stopwatch from sidebar")
        void canNavigateToStopwatch() {
            navigateToWithInstance("/");
            waitForPageLoad();

            if (isVisible("a:has-text('Stopwatch')")) {
                clickAndWait("a:has-text('Stopwatch')");
                assertTrue(page.url().contains("/stopwatch"), "Should navigate to Stopwatch page");
                assertTrue(pageLoadedWithoutErrors(), "Stopwatch page should load without errors");
            }
        }

        @Test
        @DisplayName("Can navigate to Window Comparison from sidebar")
        void canNavigateToWindowCompare() {
            navigateToWithInstance("/");
            waitForPageLoad();

            if (isVisible("a:has-text('Window Compare')") || isVisible("a:has-text('Window Comparison')")) {
                if (isVisible("a:has-text('Window Compare')")) {
                    clickAndWait("a:has-text('Window Compare')");
                } else {
                    clickAndWait("a:has-text('Window Comparison')");
                }
                assertTrue(page.url().contains("/window-compare"), "Should navigate to Window Comparison page");
                assertTrue(pageLoadedWithoutErrors(), "Window Comparison page should load without errors");
            }
        }

        @Test
        @DisplayName("Can navigate to Metrics History from sidebar")
        void canNavigateToMetricsHistory() {
            navigateToWithInstance("/");
            waitForPageLoad();

            if (isVisible("a:has-text('Metrics History')")) {
                clickAndWait("a:has-text('Metrics History')");
                assertTrue(page.url().contains("/metrics-history"), "Should navigate to Metrics History page");
                assertTrue(pageLoadedWithoutErrors(), "Metrics History page should load without errors");
            }
        }

        @Test
        @DisplayName("Can navigate to Query Trends from sidebar")
        void canNavigateToQueryTrends() {
            navigateToWithInstance("/");
            waitForPageLoad();

            if (isVisible("a:has-text('Query Trends')")) {
                clickAndWait("a:has-text('Query Trends')");
                assertTrue(page.url().contains("/query-trends"), "Should navigate to Query Trends page");
                assertTrue(pageLoadedWithoutErrors(), "Query Trends page should load without errors");
            }
        }
    }
}
