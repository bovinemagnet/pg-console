package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.config.DashboardConfig;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FeatureToggleService}.
 * <p>
 * Tests the feature toggle logic for dashboard sections and pages,
 * including hierarchical enable/disable behaviour.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ExtendWith(MockitoExtension.class)
class FeatureToggleServiceTest {

    @Mock
    DashboardConfig dashboardConfig;

    @Mock
    DashboardConfig.MonitoringConfig monitoringConfig;

    @Mock
    DashboardConfig.AnalysisConfig analysisConfig;

    @Mock
    DashboardConfig.InfrastructureConfig infrastructureConfig;

    @Mock
    DashboardConfig.DataControlConfig dataControlConfig;

    @Mock
    DashboardConfig.EnterpriseConfig enterpriseConfig;

    @Mock
    DashboardConfig.SecurityDashboardConfig securityConfig;

    @Mock
    DashboardConfig.InsightsConfig insightsConfig;

    @Mock
    DashboardConfig.DiagnosticsConfig diagnosticsConfig;

    @InjectMocks
    FeatureToggleService featureToggleService;

    @BeforeEach
    void setUp() {
        lenient().when(dashboardConfig.monitoring()).thenReturn(monitoringConfig);
        lenient().when(dashboardConfig.analysis()).thenReturn(analysisConfig);
        lenient().when(dashboardConfig.infrastructure()).thenReturn(infrastructureConfig);
        lenient().when(dashboardConfig.dataControl()).thenReturn(dataControlConfig);
        lenient().when(dashboardConfig.enterprise()).thenReturn(enterpriseConfig);
        lenient().when(dashboardConfig.security()).thenReturn(securityConfig);
        lenient().when(dashboardConfig.insights()).thenReturn(insightsConfig);
        lenient().when(dashboardConfig.diagnostics()).thenReturn(diagnosticsConfig);
    }

    @Nested
    @DisplayName("Section-level toggles")
    class SectionLevelTests {

        @Test
        @DisplayName("Monitoring section enabled when config is true")
        void monitoringSectionEnabled() {
            when(monitoringConfig.enabled()).thenReturn(true);

            assertThat(featureToggleService.isMonitoringSectionEnabled()).isTrue();
        }

        @Test
        @DisplayName("Monitoring section disabled when config is false")
        void monitoringSectionDisabled() {
            when(monitoringConfig.enabled()).thenReturn(false);

            assertThat(featureToggleService.isMonitoringSectionEnabled()).isFalse();
        }

        @Test
        @DisplayName("Analysis section reflects config state")
        void analysisSectionEnabled() {
            when(analysisConfig.enabled()).thenReturn(true);

            assertThat(featureToggleService.isAnalysisSectionEnabled()).isTrue();
        }

        @Test
        @DisplayName("Security section reflects config state")
        void securitySectionEnabled() {
            when(securityConfig.enabled()).thenReturn(true);

            assertThat(featureToggleService.isSecuritySectionEnabled()).isTrue();
        }

        @Test
        @DisplayName("Insights section reflects config state")
        void insightsSectionEnabled() {
            when(insightsConfig.enabled()).thenReturn(true);

            assertThat(featureToggleService.isInsightsSectionEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("Page-level toggles with hierarchy")
    class PageLevelTests {

        @Test
        @DisplayName("Dashboard enabled when section and page both enabled")
        void dashboardEnabledWhenBothEnabled() {
            when(monitoringConfig.enabled()).thenReturn(true);
            when(monitoringConfig.dashboardEnabled()).thenReturn(true);

            assertThat(featureToggleService.isDashboardEnabled()).isTrue();
        }

        @Test
        @DisplayName("Dashboard disabled when section disabled even if page enabled")
        void dashboardDisabledWhenSectionDisabled() {
            when(monitoringConfig.enabled()).thenReturn(false);
            // Note: dashboardEnabled() not stubbed as it won't be called due to short-circuit evaluation

            assertThat(featureToggleService.isDashboardEnabled()).isFalse();
        }

        @Test
        @DisplayName("Dashboard disabled when page disabled even if section enabled")
        void dashboardDisabledWhenPageDisabled() {
            when(monitoringConfig.enabled()).thenReturn(true);
            when(monitoringConfig.dashboardEnabled()).thenReturn(false);

            assertThat(featureToggleService.isDashboardEnabled()).isFalse();
        }

        @Test
        @DisplayName("Slow queries page respects hierarchical toggle")
        void slowQueriesHierarchicalToggle() {
            when(monitoringConfig.enabled()).thenReturn(true);
            when(monitoringConfig.slowQueriesEnabled()).thenReturn(true);

            assertThat(featureToggleService.isSlowQueriesEnabled()).isTrue();
        }

        @Test
        @DisplayName("Index advisor page respects analysis section toggle")
        void indexAdvisorHierarchicalToggle() {
            when(analysisConfig.enabled()).thenReturn(true);
            when(analysisConfig.indexAdvisorEnabled()).thenReturn(true);

            assertThat(featureToggleService.isIndexAdvisorEnabled()).isTrue();
        }

        @Test
        @DisplayName("Runbooks page respects insights section toggle")
        void runbooksHierarchicalToggle() {
            when(insightsConfig.enabled()).thenReturn(true);
            when(insightsConfig.runbooksEnabled()).thenReturn(true);

            assertThat(featureToggleService.isRunbooksEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("isPageEnabled by pageId")
    class IsPageEnabledTests {

        @Test
        @DisplayName("Returns true for enabled dashboard page")
        void dashboardPageEnabled() {
            when(monitoringConfig.enabled()).thenReturn(true);
            when(monitoringConfig.dashboardEnabled()).thenReturn(true);

            assertThat(featureToggleService.isPageEnabled("dashboard")).isTrue();
        }

        @Test
        @DisplayName("Returns false for disabled page")
        void disabledPage() {
            when(monitoringConfig.enabled()).thenReturn(false);

            assertThat(featureToggleService.isPageEnabled("activity")).isFalse();
        }

        @Test
        @DisplayName("Returns true for about page which is always enabled")
        void aboutPageAlwaysEnabled() {
            assertThat(featureToggleService.isPageEnabled("about")).isTrue();
        }

        @Test
        @DisplayName("Returns true for unknown pages by default")
        void unknownPageDefaultsToEnabled() {
            assertThat(featureToggleService.isPageEnabled("unknown-page")).isTrue();
        }

        @Test
        @DisplayName("Handles slow-queries page ID correctly")
        void slowQueriesPageId() {
            when(monitoringConfig.enabled()).thenReturn(true);
            when(monitoringConfig.slowQueriesEnabled()).thenReturn(true);

            assertThat(featureToggleService.isPageEnabled("slow-queries")).isTrue();
        }

        @Test
        @DisplayName("Handles security page ID correctly")
        void securityPageId() {
            when(securityConfig.enabled()).thenReturn(true);
            when(securityConfig.overviewEnabled()).thenReturn(true);

            assertThat(featureToggleService.isPageEnabled("security")).isTrue();
        }
    }

    @Nested
    @DisplayName("requirePageEnabled guard")
    class RequirePageEnabledTests {

        @Test
        @DisplayName("Does not throw when page is enabled")
        void noExceptionWhenEnabled() {
            when(monitoringConfig.enabled()).thenReturn(true);
            when(monitoringConfig.dashboardEnabled()).thenReturn(true);

            featureToggleService.requirePageEnabled("dashboard");
            // No exception expected
        }

        @Test
        @DisplayName("Throws NotFoundException when page is disabled")
        void throwsNotFoundWhenDisabled() {
            when(monitoringConfig.enabled()).thenReturn(false);

            assertThatThrownBy(() -> featureToggleService.requirePageEnabled("dashboard"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Page not found");
        }
    }

    @Nested
    @DisplayName("getAllToggles")
    class GetAllTogglesTests {

        @Test
        @DisplayName("Returns map with all toggle states")
        void returnsAllToggles() {
            // Set up some enabled and disabled toggles
            when(monitoringConfig.enabled()).thenReturn(true);
            when(monitoringConfig.dashboardEnabled()).thenReturn(true);
            when(monitoringConfig.activityEnabled()).thenReturn(true);
            when(monitoringConfig.slowQueriesEnabled()).thenReturn(true);
            when(monitoringConfig.locksEnabled()).thenReturn(true);
            when(monitoringConfig.waitEventsEnabled()).thenReturn(true);
            when(monitoringConfig.tablesEnabled()).thenReturn(true);
            when(monitoringConfig.databasesEnabled()).thenReturn(true);

            when(analysisConfig.enabled()).thenReturn(false);
            when(infrastructureConfig.enabled()).thenReturn(true);
            when(infrastructureConfig.replicationEnabled()).thenReturn(true);
            when(infrastructureConfig.infrastructureEnabled()).thenReturn(true);

            when(dataControlConfig.enabled()).thenReturn(true);
            when(dataControlConfig.logicalReplicationEnabled()).thenReturn(true);
            when(dataControlConfig.cdcEnabled()).thenReturn(true);
            when(dataControlConfig.dataLineageEnabled()).thenReturn(true);
            when(dataControlConfig.partitionsEnabled()).thenReturn(true);

            when(enterpriseConfig.enabled()).thenReturn(true);
            when(enterpriseConfig.comparisonEnabled()).thenReturn(true);
            when(enterpriseConfig.schemaComparisonEnabled()).thenReturn(true);
            when(enterpriseConfig.bookmarksEnabled()).thenReturn(true);
            when(enterpriseConfig.auditLogEnabled()).thenReturn(true);

            when(securityConfig.enabled()).thenReturn(true);
            when(securityConfig.overviewEnabled()).thenReturn(true);
            when(securityConfig.rolesEnabled()).thenReturn(true);
            when(securityConfig.connectionsEnabled()).thenReturn(true);
            when(securityConfig.accessEnabled()).thenReturn(true);
            when(securityConfig.complianceEnabled()).thenReturn(true);
            when(securityConfig.recommendationsEnabled()).thenReturn(true);

            when(insightsConfig.enabled()).thenReturn(true);
            when(insightsConfig.dashboardEnabled()).thenReturn(true);
            when(insightsConfig.anomaliesEnabled()).thenReturn(true);
            when(insightsConfig.forecastsEnabled()).thenReturn(true);
            when(insightsConfig.recommendationsEnabled()).thenReturn(true);
            when(insightsConfig.runbooksEnabled()).thenReturn(true);

            when(diagnosticsConfig.enabled()).thenReturn(true);
            when(diagnosticsConfig.pipelineRiskEnabled()).thenReturn(true);
            when(diagnosticsConfig.toastBloatEnabled()).thenReturn(true);
            when(diagnosticsConfig.indexRedundancyEnabled()).thenReturn(true);
            when(diagnosticsConfig.statisticalFreshnessEnabled()).thenReturn(true);
            when(diagnosticsConfig.writeReadRatioEnabled()).thenReturn(true);
            when(diagnosticsConfig.hotEfficiencyEnabled()).thenReturn(true);
            when(diagnosticsConfig.correlationEnabled()).thenReturn(true);
            when(diagnosticsConfig.liveChartsEnabled()).thenReturn(true);
            when(diagnosticsConfig.xidWraparoundEnabled()).thenReturn(true);

            Map<String, Boolean> toggles = featureToggleService.getAllToggles();

            assertThat(toggles)
                .isNotEmpty()
                .containsEntry("monitoringSection", true)
                .containsEntry("analysisSection", false)
                .containsEntry("dashboard", true)
                .containsEntry("insightsSection", true);
        }
    }
}
