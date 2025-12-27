package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.config.DashboardConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for checking feature toggle states.
 * <p>
 * Determines whether dashboard sections and pages are enabled based on configuration.
 * Uses a hierarchical model where pages are only enabled if their parent section is also enabled.
 * <p>
 * Usage:
 * <pre>
 * if (!featureToggleService.isPageEnabled("slow-queries")) {
 *     throw new NotFoundException();
 * }
 * </pre>
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class FeatureToggleService {

    @Inject
    DashboardConfig dashboardConfig;

    // ========================================
    // Section-level checks
    // ========================================

    /**
     * Checks if the Monitoring section is enabled.
     *
     * @return true if section is enabled
     */
    public boolean isMonitoringSectionEnabled() {
        return dashboardConfig.monitoring().enabled();
    }

    /**
     * Checks if the Analysis section is enabled.
     *
     * @return true if section is enabled
     */
    public boolean isAnalysisSectionEnabled() {
        return dashboardConfig.analysis().enabled();
    }

    /**
     * Checks if the Infrastructure section is enabled.
     *
     * @return true if section is enabled
     */
    public boolean isInfrastructureSectionEnabled() {
        return dashboardConfig.infrastructure().enabled();
    }

    /**
     * Checks if the Data Control section is enabled.
     *
     * @return true if section is enabled
     */
    public boolean isDataControlSectionEnabled() {
        return dashboardConfig.dataControl().enabled();
    }

    /**
     * Checks if the Enterprise section is enabled.
     *
     * @return true if section is enabled
     */
    public boolean isEnterpriseSectionEnabled() {
        return dashboardConfig.enterprise().enabled();
    }

    /**
     * Checks if the Security section is enabled.
     *
     * @return true if section is enabled
     */
    public boolean isSecuritySectionEnabled() {
        return dashboardConfig.security().enabled();
    }

    // ========================================
    // Monitoring Section Pages
    // ========================================

    /**
     * Checks if the Dashboard page is enabled.
     *
     * @return true if page is enabled
     */
    public boolean isDashboardEnabled() {
        return isMonitoringSectionEnabled() && dashboardConfig.monitoring().dashboardEnabled();
    }

    /**
     * Checks if the Activity page is enabled.
     *
     * @return true if page is enabled
     */
    public boolean isActivityEnabled() {
        return isMonitoringSectionEnabled() && dashboardConfig.monitoring().activityEnabled();
    }

    /**
     * Checks if the Slow Queries page is enabled.
     *
     * @return true if page is enabled
     */
    public boolean isSlowQueriesEnabled() {
        return isMonitoringSectionEnabled() && dashboardConfig.monitoring().slowQueriesEnabled();
    }

    /**
     * Checks if the Locks page is enabled.
     *
     * @return true if page is enabled
     */
    public boolean isLocksEnabled() {
        return isMonitoringSectionEnabled() && dashboardConfig.monitoring().locksEnabled();
    }

    /**
     * Checks if the Wait Events page is enabled.
     *
     * @return true if page is enabled
     */
    public boolean isWaitEventsEnabled() {
        return isMonitoringSectionEnabled() && dashboardConfig.monitoring().waitEventsEnabled();
    }

    /**
     * Checks if the Tables page is enabled.
     *
     * @return true if page is enabled
     */
    public boolean isTablesEnabled() {
        return isMonitoringSectionEnabled() && dashboardConfig.monitoring().tablesEnabled();
    }

    /**
     * Checks if the Databases page is enabled.
     *
     * @return true if page is enabled
     */
    public boolean isDatabasesEnabled() {
        return isMonitoringSectionEnabled() && dashboardConfig.monitoring().databasesEnabled();
    }

    // ========================================
    // Analysis Section Pages
    // ========================================

    /**
     * Checks if the Index Advisor page is enabled.
     *
     * @return true if page is enabled
     */
    public boolean isIndexAdvisorEnabled() {
        return isAnalysisSectionEnabled() && dashboardConfig.analysis().indexAdvisorEnabled();
    }

    /**
     * Checks if the Query Regressions page is enabled.
     *
     * @return true if page is enabled
     */
    public boolean isQueryRegressionsEnabled() {
        return isAnalysisSectionEnabled() && dashboardConfig.analysis().queryRegressionsEnabled();
    }

    /**
     * Checks if the Table Maintenance page is enabled.
     *
     * @return true if page is enabled
     */
    public boolean isTableMaintenanceEnabled() {
        return isAnalysisSectionEnabled() && dashboardConfig.analysis().tableMaintenanceEnabled();
    }

    /**
     * Checks if the Baselines page is enabled.
     *
     * @return true if page is enabled
     */
    public boolean isBaselinesEnabled() {
        return isAnalysisSectionEnabled() && dashboardConfig.analysis().baselinesEnabled();
    }

    // ========================================
    // Infrastructure Section Pages
    // ========================================

    /**
     * Checks if the Replication page is enabled.
     *
     * @return true if page is enabled
     */
    public boolean isReplicationEnabled() {
        return isInfrastructureSectionEnabled() && dashboardConfig.infrastructure().replicationEnabled();
    }

    /**
     * Checks if the Infrastructure page is enabled.
     *
     * @return true if page is enabled
     */
    public boolean isInfrastructureEnabled() {
        return isInfrastructureSectionEnabled() && dashboardConfig.infrastructure().infrastructureEnabled();
    }

    // ========================================
    // Data Control Section Pages
    // ========================================

    /**
     * Checks if the Logical Replication page is enabled.
     *
     * @return true if page is enabled
     */
    public boolean isLogicalReplicationEnabled() {
        return isDataControlSectionEnabled() && dashboardConfig.dataControl().logicalReplicationEnabled();
    }

    /**
     * Checks if the CDC page is enabled.
     *
     * @return true if page is enabled
     */
    public boolean isCdcEnabled() {
        return isDataControlSectionEnabled() && dashboardConfig.dataControl().cdcEnabled();
    }

    /**
     * Checks if the Data Lineage page is enabled.
     *
     * @return true if page is enabled
     */
    public boolean isDataLineageEnabled() {
        return isDataControlSectionEnabled() && dashboardConfig.dataControl().dataLineageEnabled();
    }

    /**
     * Checks if the Partitions page is enabled.
     *
     * @return true if page is enabled
     */
    public boolean isPartitionsEnabled() {
        return isDataControlSectionEnabled() && dashboardConfig.dataControl().partitionsEnabled();
    }

    // ========================================
    // Enterprise Section Pages
    // ========================================

    /**
     * Checks if the Instance Comparison page is enabled.
     *
     * @return true if page is enabled
     */
    public boolean isComparisonEnabled() {
        return isEnterpriseSectionEnabled() && dashboardConfig.enterprise().comparisonEnabled();
    }

    /**
     * Checks if the Schema Comparison page is enabled.
     *
     * @return true if page is enabled
     */
    public boolean isSchemaComparisonEnabled() {
        return isEnterpriseSectionEnabled() && dashboardConfig.enterprise().schemaComparisonEnabled();
    }

    /**
     * Checks if the Bookmarks page is enabled.
     *
     * @return true if page is enabled
     */
    public boolean isBookmarksEnabled() {
        return isEnterpriseSectionEnabled() && dashboardConfig.enterprise().bookmarksEnabled();
    }

    /**
     * Checks if the Audit Log page is enabled.
     *
     * @return true if page is enabled
     */
    public boolean isAuditLogEnabled() {
        return isEnterpriseSectionEnabled() && dashboardConfig.enterprise().auditLogEnabled();
    }

    // ========================================
    // Security Section Pages
    // ========================================

    /**
     * Checks if the Security Overview page is enabled.
     *
     * @return true if page is enabled
     */
    public boolean isSecurityOverviewEnabled() {
        return isSecuritySectionEnabled() && dashboardConfig.security().overviewEnabled();
    }

    /**
     * Checks if the Roles page is enabled.
     *
     * @return true if page is enabled
     */
    public boolean isRolesEnabled() {
        return isSecuritySectionEnabled() && dashboardConfig.security().rolesEnabled();
    }

    /**
     * Checks if the Security Connections page is enabled.
     *
     * @return true if page is enabled
     */
    public boolean isSecurityConnectionsEnabled() {
        return isSecuritySectionEnabled() && dashboardConfig.security().connectionsEnabled();
    }

    /**
     * Checks if the Access page is enabled.
     *
     * @return true if page is enabled
     */
    public boolean isAccessEnabled() {
        return isSecuritySectionEnabled() && dashboardConfig.security().accessEnabled();
    }

    /**
     * Checks if the Compliance page is enabled.
     *
     * @return true if page is enabled
     */
    public boolean isComplianceEnabled() {
        return isSecuritySectionEnabled() && dashboardConfig.security().complianceEnabled();
    }

    /**
     * Checks if the Security Recommendations page is enabled.
     *
     * @return true if page is enabled
     */
    public boolean isSecurityRecommendationsEnabled() {
        return isSecuritySectionEnabled() && dashboardConfig.security().recommendationsEnabled();
    }

    // ========================================
    // Guard methods
    // ========================================

    /**
     * Throws NotFoundException if the specified page is disabled.
     * Used as a route guard in resource methods.
     *
     * @param pageId the page identifier (e.g., "dashboard", "slow-queries")
     * @throws NotFoundException if the page is disabled
     */
    public void requirePageEnabled(String pageId) {
        if (!isPageEnabled(pageId)) {
            throw new NotFoundException("Page not found");
        }
    }

    /**
     * Checks if a page is enabled by its identifier.
     *
     * @param pageId the page identifier
     * @return true if page is enabled
     */
    public boolean isPageEnabled(String pageId) {
        return switch (pageId) {
            // Monitoring
            case "dashboard" -> isDashboardEnabled();
            case "activity" -> isActivityEnabled();
            case "slow-queries" -> isSlowQueriesEnabled();
            case "locks" -> isLocksEnabled();
            case "wait-events" -> isWaitEventsEnabled();
            case "tables" -> isTablesEnabled();
            case "databases" -> isDatabasesEnabled();
            // Analysis
            case "index-advisor" -> isIndexAdvisorEnabled();
            case "query-regressions" -> isQueryRegressionsEnabled();
            case "table-maintenance" -> isTableMaintenanceEnabled();
            case "baselines", "statements-management" -> isBaselinesEnabled();
            // Infrastructure
            case "replication" -> isReplicationEnabled();
            case "infrastructure" -> isInfrastructureEnabled();
            // Data Control
            case "logical-replication" -> isLogicalReplicationEnabled();
            case "cdc" -> isCdcEnabled();
            case "data-lineage" -> isDataLineageEnabled();
            case "partitions" -> isPartitionsEnabled();
            // Enterprise
            case "comparison" -> isComparisonEnabled();
            case "schema-comparison" -> isSchemaComparisonEnabled();
            case "bookmarks" -> isBookmarksEnabled();
            case "audit-log" -> isAuditLogEnabled();
            // Security
            case "security" -> isSecurityOverviewEnabled();
            case "security-roles" -> isRolesEnabled();
            case "security-connections" -> isSecurityConnectionsEnabled();
            case "security-access" -> isAccessEnabled();
            case "security-compliance" -> isComplianceEnabled();
            case "security-recommendations" -> isSecurityRecommendationsEnabled();
            // System (always enabled)
            case "about" -> true;
            default -> true;
        };
    }

    /**
     * Returns all feature toggle states as a map.
     * Useful for exposing toggles to templates.
     *
     * @return map of feature names to enabled state
     */
    public Map<String, Boolean> getAllToggles() {
        Map<String, Boolean> toggles = new HashMap<>();

        // Sections
        toggles.put("monitoringSection", isMonitoringSectionEnabled());
        toggles.put("analysisSection", isAnalysisSectionEnabled());
        toggles.put("infrastructureSection", isInfrastructureSectionEnabled());
        toggles.put("dataControlSection", isDataControlSectionEnabled());
        toggles.put("enterpriseSection", isEnterpriseSectionEnabled());
        toggles.put("securitySection", isSecuritySectionEnabled());

        // Monitoring pages
        toggles.put("dashboard", isDashboardEnabled());
        toggles.put("activity", isActivityEnabled());
        toggles.put("slowQueries", isSlowQueriesEnabled());
        toggles.put("locks", isLocksEnabled());
        toggles.put("waitEvents", isWaitEventsEnabled());
        toggles.put("tables", isTablesEnabled());
        toggles.put("databases", isDatabasesEnabled());

        // Analysis pages
        toggles.put("indexAdvisor", isIndexAdvisorEnabled());
        toggles.put("queryRegressions", isQueryRegressionsEnabled());
        toggles.put("tableMaintenance", isTableMaintenanceEnabled());
        toggles.put("baselines", isBaselinesEnabled());

        // Infrastructure pages
        toggles.put("replication", isReplicationEnabled());
        toggles.put("infrastructure", isInfrastructureEnabled());

        // Data Control pages
        toggles.put("logicalReplication", isLogicalReplicationEnabled());
        toggles.put("cdc", isCdcEnabled());
        toggles.put("dataLineage", isDataLineageEnabled());
        toggles.put("partitions", isPartitionsEnabled());

        // Enterprise pages
        toggles.put("comparison", isComparisonEnabled());
        toggles.put("schemaComparison", isSchemaComparisonEnabled());
        toggles.put("bookmarks", isBookmarksEnabled());
        toggles.put("auditLog", isAuditLogEnabled());

        // Security pages
        toggles.put("securityOverview", isSecurityOverviewEnabled());
        toggles.put("roles", isRolesEnabled());
        toggles.put("securityConnections", isSecurityConnectionsEnabled());
        toggles.put("access", isAccessEnabled());
        toggles.put("compliance", isComplianceEnabled());
        toggles.put("securityRecommendations", isSecurityRecommendationsEnabled());

        return toggles;
    }
}
