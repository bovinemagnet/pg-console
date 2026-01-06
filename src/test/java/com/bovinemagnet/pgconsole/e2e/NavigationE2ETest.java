package com.bovinemagnet.pgconsole.e2e;

import com.bovinemagnet.pgconsole.e2e.page.BasePage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end tests for navigation and page loading across all dashboard pages.
 * <p>
 * Verifies that all main application pages load successfully without errors and
 * contain the expected structural elements. This test suite validates the complete
 * navigation structure including main dashboards, analysis tools, infrastructure
 * monitoring pages, and security sections.
 * <p>
 * <strong>Test Prerequisites:</strong>
 * <ul>
 *   <li>Application must be running and accessible at the configured base URL</li>
 *   <li>PostgreSQL database must be available with required extensions enabled</li>
 *   <li>Database must have pg_stat_statements extension configured</li>
 *   <li>Test assumes default instance configuration is accessible</li>
 * </ul>
 * <p>
 * <strong>Test Assumptions:</strong>
 * <ul>
 *   <li>All pages use consistent error messaging patterns</li>
 *   <li>Sidebar navigation is present on all dashboard pages</li>
 *   <li>Pages append ?instance=default parameter for database selection</li>
 *   <li>Browser title elements indicate successful page rendering</li>
 * </ul>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see BasePage
 * @see PlaywrightTestBase
 */
@DisplayName("Navigation E2E Tests")
class NavigationE2ETest extends PlaywrightTestBase {

    private BasePage basePage;

    /**
     * Initialises the BasePage instance before each test.
     * <p>
     * Creates a new BasePage object configured with the current Playwright page
     * instance and the base URL. This setup ensures each test has a clean page
     * object for navigation and assertions.
     */
    @BeforeEach
    void setUpBasePage() {
        basePage = new BasePage(page, BASE_URL);
    }

    // ========== Main Dashboard Pages ==========

    /**
     * Verifies that the main dashboard (overview) page loads successfully.
     * <p>
     * Tests the root dashboard endpoint to ensure it renders without server errors,
     * displays the standard sidebar navigation, and includes a page title. This is
     * the primary landing page showing overview widgets for connections, active queries,
     * cache hit ratio, and database size.
     */
    @Test
    @DisplayName("Dashboard page loads without errors")
    void dashboardPageLoads() {
        basePage.navigateToWithInstance("/");

        assertTrue(basePage.pageLoadedWithoutErrors(), "Dashboard should load without errors");
        assertTrue(basePage.isSidebarVisible(), "Sidebar should be visible");
        assertNotNull(basePage.getPageTitle(), "Page should have a title");
    }

    /**
     * Verifies that the slow queries page loads successfully.
     * <p>
     * Tests the slow queries dashboard which displays query performance metrics
     * from pg_stat_statements, including execution times, call counts, and resource
     * usage. Validates both page rendering and correct browser title.
     */
    @Test
    @DisplayName("Slow queries page loads without errors")
    void slowQueriesPageLoads() {
        basePage.navigateToWithInstance("/slow-queries");

        assertTrue(basePage.pageLoadedWithoutErrors(), "Slow queries page should load without errors");
        assertTrue(basePage.getBrowserTitle().contains("Slow Queries"), "Title should contain 'Slow Queries'");
    }

    /**
     * Verifies that the activity page loads successfully.
     * <p>
     * Tests the database activity dashboard which displays current connections and
     * running queries from pg_stat_activity, showing real-time database workload.
     */
    @Test
    @DisplayName("Activity page loads without errors")
    void activityPageLoads() {
        basePage.navigateToWithInstance("/activity");

        assertTrue(basePage.pageLoadedWithoutErrors(), "Activity page should load without errors");
    }

    /**
     * Verifies that the locks page loads successfully.
     * <p>
     * Tests the locks dashboard which displays lock contention information from
     * pg_locks, including blocking trees and queries waiting for locks. Essential
     * for diagnosing concurrency issues.
     */
    @Test
    @DisplayName("Locks page loads without errors")
    void locksPageLoads() {
        basePage.navigateToWithInstance("/locks");

        assertTrue(basePage.pageLoadedWithoutErrors(), "Locks page should load without errors");
    }

    /**
     * Verifies that the deadlocks page loads successfully.
     * <p>
     * Tests the deadlocks monitoring dashboard which provides historical deadlock
     * information and analysis tools for investigating and resolving deadlock scenarios.
     */
    @Test
    @DisplayName("Deadlocks page loads without errors")
    void deadlocksPageLoads() {
        basePage.navigateToWithInstance("/deadlocks");

        assertTrue(basePage.pageLoadedWithoutErrors(), "Deadlocks page should load without errors");
    }

    /**
     * Verifies that the wait events page loads successfully.
     * <p>
     * Tests the wait events dashboard which displays what queries are waiting for,
     * helping identify performance bottlenecks related to I/O, locks, or other
     * resource contention.
     */
    @Test
    @DisplayName("Wait events page loads without errors")
    void waitEventsPageLoads() {
        basePage.navigateToWithInstance("/wait-events");

        assertTrue(basePage.pageLoadedWithoutErrors(), "Wait events page should load without errors");
    }

    /**
     * Verifies that the tables page loads successfully.
     * <p>
     * Tests the tables statistics dashboard which displays table sizes, bloat metrics,
     * and maintenance recommendations from pg_stat_user_tables and related views.
     */
    @Test
    @DisplayName("Tables page loads without errors")
    void tablesPageLoads() {
        basePage.navigateToWithInstance("/tables");

        assertTrue(basePage.pageLoadedWithoutErrors(), "Tables page should load without errors");
    }

    /**
     * Verifies that the databases page loads successfully.
     * <p>
     * Tests the databases comparison dashboard which displays per-database metrics
     * from pg_stat_database, allowing comparison of activity, cache hit ratios,
     * and transaction counts across multiple databases.
     */
    @Test
    @DisplayName("Databases page loads without errors")
    void databasesPageLoads() {
        basePage.navigateToWithInstance("/databases");

        assertTrue(basePage.pageLoadedWithoutErrors(), "Databases page should load without errors");
    }

    /**
     * Verifies that the about page loads successfully.
     * <p>
     * Tests the about/information page which displays application version details
     * and PostgreSQL server information including version, uptime, and configuration.
     */
    @Test
    @DisplayName("About page loads without errors")
    void aboutPageLoads() {
        basePage.navigateToWithInstance("/about");

        assertTrue(basePage.pageLoadedWithoutErrors(), "About page should load without errors");
    }

    // ========== Analysis Section ==========

    /**
     * Verifies that the index advisor page loads successfully.
     * <p>
     * Tests the index advisor analysis tool which examines query patterns and
     * suggests missing indexes that could improve query performance. Analyses
     * sequential scans and provides recommendations for index creation.
     */
    @Test
    @DisplayName("Index advisor page loads without errors")
    void indexAdvisorPageLoads() {
        basePage.navigateToWithInstance("/index-advisor");

        assertTrue(basePage.pageLoadedWithoutErrors(), "Index advisor page should load without errors");
    }

    /**
     * Verifies that the query regressions page loads successfully.
     * <p>
     * Tests the query regressions analysis tool which identifies queries whose
     * performance has degraded over time by comparing historical execution metrics.
     * Useful for detecting plan changes or data volume impacts.
     */
    @Test
    @DisplayName("Query regressions page loads without errors")
    void queryRegressionsPageLoads() {
        basePage.navigateToWithInstance("/query-regressions");

        assertTrue(basePage.pageLoadedWithoutErrors(), "Query regressions page should load without errors");
    }

    /**
     * Verifies that the table maintenance page loads successfully.
     * <p>
     * Tests the table maintenance recommendations page which identifies tables
     * requiring VACUUM or ANALYZE operations based on dead tuple counts and
     * statistics staleness.
     */
    @Test
    @DisplayName("Table maintenance page loads without errors")
    void tableMaintenancePageLoads() {
        basePage.navigateToWithInstance("/table-maintenance");

        assertTrue(basePage.pageLoadedWithoutErrors(), "Table maintenance page should load without errors");
    }

    // ========== Infrastructure Section ==========

    /**
     * Verifies that the replication page loads successfully.
     * <p>
     * Tests the replication monitoring dashboard which displays replication lag,
     * streaming replication status, and replica health from pg_stat_replication.
     * Essential for monitoring high-availability configurations.
     */
    @Test
    @DisplayName("Replication page loads without errors")
    void replicationPageLoads() {
        basePage.navigateToWithInstance("/replication");

        assertTrue(basePage.pageLoadedWithoutErrors(), "Replication page should load without errors");
    }

    /**
     * Verifies that the infrastructure page loads successfully.
     * <p>
     * Tests the infrastructure overview dashboard which provides system-level
     * metrics including server resources, connection limits, and overall
     * database cluster health indicators.
     */
    @Test
    @DisplayName("Infrastructure page loads without errors")
    void infrastructurePageLoads() {
        basePage.navigateToWithInstance("/infrastructure");

        assertTrue(basePage.pageLoadedWithoutErrors(), "Infrastructure page should load without errors");
    }

    /**
     * Verifies that the configuration health page loads successfully.
     * <p>
     * Tests the configuration health dashboard which analyses PostgreSQL
     * configuration parameters and identifies potential misconfigurations or
     * suboptimal settings based on best practices.
     */
    @Test
    @DisplayName("Config health page loads without errors")
    void configHealthPageLoads() {
        basePage.navigateToWithInstance("/config-health");

        assertTrue(basePage.pageLoadedWithoutErrors(), "Config health page should load without errors");
    }

    /**
     * Verifies that the checkpoints page loads successfully.
     * <p>
     * Tests the checkpoints monitoring dashboard which displays checkpoint
     * statistics including frequency, duration, and associated I/O metrics
     * from pg_stat_bgwriter.
     */
    @Test
    @DisplayName("Checkpoints page loads without errors")
    void checkpointsPageLoads() {
        basePage.navigateToWithInstance("/checkpoints");

        assertTrue(basePage.pageLoadedWithoutErrors(), "Checkpoints page should load without errors");
    }

    /**
     * Verifies that the WAL checkpoints page loads successfully.
     * <p>
     * Tests the Write-Ahead Log (WAL) checkpoints dashboard which provides
     * detailed information about WAL generation, archiving status, and checkpoint
     * tuning recommendations.
     */
    @Test
    @DisplayName("WAL checkpoints page loads without errors")
    void walCheckpointsPageLoads() {
        basePage.navigateToWithInstance("/wal-checkpoints");

        assertTrue(basePage.pageLoadedWithoutErrors(), "WAL checkpoints page should load without errors");
    }

    /**
     * Verifies that the health check page loads successfully.
     * <p>
     * Tests the overall health check dashboard which aggregates multiple health
     * indicators to provide a quick assessment of database cluster status and
     * identifies critical issues requiring attention.
     */
    @Test
    @DisplayName("Health check page loads without errors")
    void healthCheckPageLoads() {
        basePage.navigateToWithInstance("/health-check");

        assertTrue(basePage.pageLoadedWithoutErrors(), "Health check page should load without errors");
    }

    // ========== Security Section ==========

    /**
     * Verifies that the security overview page loads successfully.
     * <p>
     * Tests the security overview dashboard which displays authentication methods,
     * connection security settings, and user access patterns. Provides visibility
     * into database security posture.
     */
    @Test
    @DisplayName("Security overview page loads without errors")
    void securityPageLoads() {
        basePage.navigateToWithInstance("/security");

        assertTrue(basePage.pageLoadedWithoutErrors(), "Security page should load without errors");
    }

    /**
     * Verifies that the security roles page loads successfully.
     * <p>
     * Tests the security roles dashboard which displays database roles, their
     * privileges, and membership hierarchies from pg_roles and pg_auth_members.
     * Essential for auditing access control configuration.
     */
    @Test
    @DisplayName("Security roles page loads without errors")
    void securityRolesPageLoads() {
        basePage.navigateToWithInstance("/security/roles");

        assertTrue(basePage.pageLoadedWithoutErrors(), "Security roles page should load without errors");
    }

    // ========== Sidebar Navigation ==========

    /**
     * Verifies that sidebar navigation functions correctly.
     * <p>
     * Tests the interactive navigation by clicking a sidebar link and verifying
     * that the application navigates to the correct page and renders successfully.
     * This validates that the client-side navigation mechanism works across pages.
     */
    @Test
    @DisplayName("Sidebar navigation to slow queries works")
    void sidebarNavigationWorks() {
        basePage.navigateToWithInstance("/");
        assertTrue(basePage.isSidebarVisible(), "Sidebar should be visible");

        basePage.clickNavLink("Slow Queries");

        assertTrue(basePage.getCurrentUrl().contains("/slow-queries"), "Should navigate to slow queries");
        assertTrue(basePage.pageLoadedWithoutErrors(), "Page should load without errors");
    }
}
