package com.bovinemagnet.pgconsole;

import com.bovinemagnet.pgconsole.service.FeatureToggleService;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Displays dashboard links and application information on startup.
 * <p>
 * Shows all available dashboards organised by section, making it easy
 * to discover and navigate to different features.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class StartupBanner {

    private static final Logger LOG = Logger.getLogger(StartupBanner.class);

    @ConfigProperty(name = "quarkus.http.port", defaultValue = "8080")
    int httpPort;

    @ConfigProperty(name = "quarkus.http.host", defaultValue = "0.0.0.0")
    String httpHost;

    @ConfigProperty(name = "quarkus.application.name", defaultValue = "pg-console")
    String appName;

    @ConfigProperty(name = "quarkus.application.version", defaultValue = "1.0.0")
    String appVersion;

    @Inject
    FeatureToggleService featureToggleService;

    void onStart(@Observes StartupEvent event) {
        try {
            displayBanner();
        } catch (Exception e) {
            LOG.warn("Could not display startup banner: " + e.getMessage());
        }
    }

    private void displayBanner() {
        String baseUrl = getBaseUrl();

        StringBuilder banner = new StringBuilder();
        banner.append("\n");
        banner.append("╔══════════════════════════════════════════════════════════════════════════════╗\n");
        banner.append("║                                                                              ║\n");
        banner.append(String.format("║   %-72s   ║%n", "PostgreSQL Console v" + appVersion));
        banner.append("║                                                                              ║\n");
        banner.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        banner.append("║                                                                              ║\n");
        banner.append(String.format("║   %-72s   ║%n", "Application running at: " + baseUrl));
        banner.append("║                                                                              ║\n");
        banner.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");

        // Monitoring Section
        if (featureToggleService.isMonitoringSectionEnabled()) {
            banner.append("║                                                                              ║\n");
            banner.append("║   MONITORING                                                                 ║\n");
            banner.append("║   ──────────                                                                 ║\n");
            appendLink(banner, "Dashboard", "/", featureToggleService.isPageEnabled("dashboard"), baseUrl);
            appendLink(banner, "Activity", "/activity", featureToggleService.isPageEnabled("activity"), baseUrl);
            appendLink(banner, "Slow Queries", "/slow-queries", featureToggleService.isPageEnabled("slow-queries"), baseUrl);
            appendLink(banner, "Locks", "/locks", featureToggleService.isPageEnabled("locks"), baseUrl);
            appendLink(banner, "Wait Events", "/wait-events", featureToggleService.isPageEnabled("wait-events"), baseUrl);
            appendLink(banner, "Tables", "/tables", featureToggleService.isPageEnabled("tables"), baseUrl);
            appendLink(banner, "Databases", "/databases", featureToggleService.isPageEnabled("databases"), baseUrl);
        }

        // Analysis Section
        if (featureToggleService.isAnalysisSectionEnabled()) {
            banner.append("║                                                                              ║\n");
            banner.append("║   ANALYSIS                                                                   ║\n");
            banner.append("║   ────────                                                                   ║\n");
            appendLink(banner, "Index Advisor", "/index-advisor", featureToggleService.isPageEnabled("index-advisor"), baseUrl);
            appendLink(banner, "Query Regressions", "/query-regressions", featureToggleService.isPageEnabled("query-regressions"), baseUrl);
            appendLink(banner, "Table Maintenance", "/table-maintenance", featureToggleService.isPageEnabled("table-maintenance"), baseUrl);
            appendLink(banner, "Baselines", "/statements-management", featureToggleService.isPageEnabled("baselines"), baseUrl);
        }

        // Infrastructure Section
        if (featureToggleService.isInfrastructureSectionEnabled()) {
            banner.append("║                                                                              ║\n");
            banner.append("║   INFRASTRUCTURE                                                             ║\n");
            banner.append("║   ──────────────                                                             ║\n");
            appendLink(banner, "Replication", "/replication", featureToggleService.isPageEnabled("replication"), baseUrl);
            appendLink(banner, "Infrastructure", "/infrastructure", featureToggleService.isPageEnabled("infrastructure"), baseUrl);
        }

        // Data Control Section
        if (featureToggleService.isDataControlSectionEnabled()) {
            banner.append("║                                                                              ║\n");
            banner.append("║   DATA CONTROL                                                               ║\n");
            banner.append("║   ────────────                                                               ║\n");
            appendLink(banner, "Logical Replication", "/logical-replication", featureToggleService.isPageEnabled("logical-replication"), baseUrl);
            appendLink(banner, "CDC", "/cdc", featureToggleService.isPageEnabled("cdc"), baseUrl);
            appendLink(banner, "Data Lineage", "/data-lineage", featureToggleService.isPageEnabled("data-lineage"), baseUrl);
            appendLink(banner, "Partitions", "/partitions", featureToggleService.isPageEnabled("partitions"), baseUrl);
        }

        // Enterprise Section
        if (featureToggleService.isEnterpriseSectionEnabled()) {
            banner.append("║                                                                              ║\n");
            banner.append("║   ENTERPRISE                                                                 ║\n");
            banner.append("║   ──────────                                                                 ║\n");
            appendLink(banner, "Comparison", "/comparison", featureToggleService.isPageEnabled("comparison"), baseUrl);
            appendLink(banner, "Schema Diff", "/schema-comparison", featureToggleService.isPageEnabled("schema-comparison"), baseUrl);
            appendLink(banner, "Bookmarks", "/bookmarks", featureToggleService.isPageEnabled("bookmarks"), baseUrl);
            appendLink(banner, "Audit Log", "/audit-log", featureToggleService.isPageEnabled("audit-log"), baseUrl);
        }

        // Insights Section
        if (featureToggleService.isInsightsSectionEnabled()) {
            banner.append("║                                                                              ║\n");
            banner.append("║   INSIGHTS                                                                   ║\n");
            banner.append("║   ────────                                                                   ║\n");
            appendLink(banner, "Insights Dashboard", "/insights", featureToggleService.isPageEnabled("insights"), baseUrl);
            appendLink(banner, "Anomalies", "/insights/anomalies", featureToggleService.isPageEnabled("anomalies"), baseUrl);
            appendLink(banner, "Forecasts", "/insights/forecasts", featureToggleService.isPageEnabled("forecasts"), baseUrl);
            appendLink(banner, "Recommendations", "/insights/recommendations", featureToggleService.isPageEnabled("recommendations"), baseUrl);
            appendLink(banner, "Runbooks", "/insights/runbooks", featureToggleService.isPageEnabled("runbooks"), baseUrl);
        }

        // Security Section
        if (featureToggleService.isSecuritySectionEnabled()) {
            banner.append("║                                                                              ║\n");
            banner.append("║   SECURITY                                                                   ║\n");
            banner.append("║   ────────                                                                   ║\n");
            appendLink(banner, "Security Overview", "/security", featureToggleService.isPageEnabled("security-overview"), baseUrl);
            appendLink(banner, "Roles", "/security/roles", featureToggleService.isPageEnabled("security-roles"), baseUrl);
            appendLink(banner, "Connections", "/security/connections", featureToggleService.isPageEnabled("security-connections"), baseUrl);
            appendLink(banner, "Access", "/security/access", featureToggleService.isPageEnabled("security-access"), baseUrl);
        }

        // Other Pages
        banner.append("║                                                                              ║\n");
        banner.append("║   OTHER                                                                       ║\n");
        banner.append("║   ─────                                                                       ║\n");
        appendLink(banner, "About", "/about", true, baseUrl);
        appendLink(banner, "API Docs", "/api", true, baseUrl);

        banner.append("║                                                                              ║\n");
        banner.append("╚══════════════════════════════════════════════════════════════════════════════╝\n");

        System.out.println(banner);
    }

    private void appendLink(StringBuilder banner, String name, String path, boolean enabled, String baseUrl) {
        String status = enabled ? "" : " (disabled)";
        String url = baseUrl + path;
        String line = String.format("║     %-20s %s%-30s   ║", name + ":", url, status);
        // Ensure line is exactly 80 characters
        if (line.length() < 80) {
            line = line.substring(0, line.length() - 1) + " ".repeat(80 - line.length()) + "║";
        }
        banner.append(line).append("\n");
    }

    private String getBaseUrl() {
        String host = httpHost;
        if ("0.0.0.0".equals(host)) {
            host = "localhost";
        }
        return "http://" + host + ":" + httpPort;
    }
}
