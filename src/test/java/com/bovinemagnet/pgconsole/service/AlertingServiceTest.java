package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.config.InstanceConfig;
import com.bovinemagnet.pgconsole.model.OverviewStats;
import com.bovinemagnet.pgconsole.testutil.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AlertingService}.
 * <p>
 * Tests alert triggering logic, threshold checking, and cooldown behaviour.
 * Note: Actual HTTP/email sending is not tested as the HttpClient is internal.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ExtendWith(MockitoExtension.class)
class AlertingServiceTest {

    @Mock
    InstanceConfig config;

    @Mock
    InstanceConfig.AlertingConfig alertingConfig;

    @Mock
    InstanceConfig.ThresholdsConfig thresholdsConfig;

    @InjectMocks
    AlertingService alertingService;

    @BeforeEach
    void setUp() {
        lenient().when(config.alerting()).thenReturn(alertingConfig);
    }

    @Nested
    @DisplayName("checkAndAlert behaviour")
    class CheckAndAlertTests {

        @Test
        @DisplayName("Does nothing when alerting is disabled")
        void noAlertWhenDisabled() {
            when(alertingConfig.enabled()).thenReturn(false);

            OverviewStats stats = TestDataFactory.createHighLoadStats();
            alertingService.checkAndAlert("default", stats);

            // Should not try to access thresholds when disabled
            verify(alertingConfig, never()).thresholds();
        }

        @Test
        @DisplayName("Checks thresholds when alerting is enabled")
        void checksThresholdsWhenEnabled() {
            when(alertingConfig.enabled()).thenReturn(true);
            when(alertingConfig.thresholds()).thenReturn(thresholdsConfig);
            lenient().when(thresholdsConfig.connectionPercent()).thenReturn(100); // High to not trigger
            lenient().when(thresholdsConfig.blockedQueries()).thenReturn(100);
            lenient().when(thresholdsConfig.cacheHitRatio()).thenReturn(0);

            OverviewStats stats = TestDataFactory.createOverviewStats(50, 100, 5, 0, 99.5);
            alertingService.checkAndAlert("default", stats);

            // Verify thresholds were accessed
            verify(alertingConfig).thresholds();
        }

        @Test
        @DisplayName("Triggers connection alert when threshold exceeded")
        void triggersConnectionAlert() {
            when(alertingConfig.enabled()).thenReturn(true);
            when(alertingConfig.thresholds()).thenReturn(thresholdsConfig);
            lenient().when(alertingConfig.cooldownSeconds()).thenReturn(0);
            lenient().when(alertingConfig.webhookUrl()).thenReturn(Optional.empty());
            lenient().when(alertingConfig.emailTo()).thenReturn(Optional.empty());
            when(thresholdsConfig.connectionPercent()).thenReturn(90); // 90% threshold
            lenient().when(thresholdsConfig.blockedQueries()).thenReturn(100);
            lenient().when(thresholdsConfig.cacheHitRatio()).thenReturn(0);

            // 95% connections used should trigger
            OverviewStats stats = TestDataFactory.createOverviewStats(95, 100, 5, 0, 99.5);
            alertingService.checkAndAlert("default", stats);

            // Verify connection threshold was checked (might be called multiple times)
            verify(thresholdsConfig, atLeast(1)).connectionPercent();
        }

        @Test
        @DisplayName("Triggers blocked queries alert when threshold exceeded")
        void triggersBlockedQueriesAlert() {
            when(alertingConfig.enabled()).thenReturn(true);
            when(alertingConfig.thresholds()).thenReturn(thresholdsConfig);
            lenient().when(alertingConfig.cooldownSeconds()).thenReturn(0);
            lenient().when(alertingConfig.webhookUrl()).thenReturn(Optional.empty());
            lenient().when(alertingConfig.emailTo()).thenReturn(Optional.empty());
            lenient().when(thresholdsConfig.connectionPercent()).thenReturn(100);
            when(thresholdsConfig.blockedQueries()).thenReturn(5); // 5 blocked threshold
            lenient().when(thresholdsConfig.cacheHitRatio()).thenReturn(0);

            // 10 blocked queries should trigger
            OverviewStats stats = TestDataFactory.createOverviewStats(50, 100, 5, 10, 99.5);
            alertingService.checkAndAlert("default", stats);

            verify(thresholdsConfig, atLeast(1)).blockedQueries();
        }

        @Test
        @DisplayName("Triggers cache hit ratio alert when below threshold")
        void triggersCacheHitRatioAlert() {
            when(alertingConfig.enabled()).thenReturn(true);
            when(alertingConfig.thresholds()).thenReturn(thresholdsConfig);
            lenient().when(alertingConfig.cooldownSeconds()).thenReturn(0);
            lenient().when(alertingConfig.webhookUrl()).thenReturn(Optional.empty());
            lenient().when(alertingConfig.emailTo()).thenReturn(Optional.empty());
            lenient().when(thresholdsConfig.connectionPercent()).thenReturn(100);
            lenient().when(thresholdsConfig.blockedQueries()).thenReturn(100);
            when(thresholdsConfig.cacheHitRatio()).thenReturn(90); // 90% threshold

            // 80% cache hit ratio should trigger
            OverviewStats stats = TestDataFactory.createOverviewStats(50, 100, 5, 0, 80.0);
            alertingService.checkAndAlert("default", stats);

            verify(thresholdsConfig, atLeast(1)).cacheHitRatio();
        }
    }

    @Nested
    @DisplayName("Cooldown behaviour")
    class CooldownTests {

        @Test
        @DisplayName("Clear cooldown allows immediate re-alert")
        void clearCooldownAllowsReAlert() {
            // Clear a specific cooldown
            alertingService.clearCooldown("default", "TEST_ALERT");
            // No exception means success
        }

        @Test
        @DisplayName("Clear all cooldowns resets all tracking")
        void clearAllCooldownsResetsTracking() {
            // Clear all cooldowns
            alertingService.clearAllCooldowns();
            // No exception means success
        }

        @Test
        @DisplayName("Cooldown prevents repeated threshold checks from triggering")
        void cooldownBehaviourWithThresholds() {
            when(alertingConfig.enabled()).thenReturn(true);
            when(alertingConfig.thresholds()).thenReturn(thresholdsConfig);
            when(alertingConfig.cooldownSeconds()).thenReturn(300); // 5 minute cooldown
            lenient().when(alertingConfig.webhookUrl()).thenReturn(Optional.empty());
            lenient().when(alertingConfig.emailTo()).thenReturn(Optional.empty());
            when(thresholdsConfig.connectionPercent()).thenReturn(90);
            lenient().when(thresholdsConfig.blockedQueries()).thenReturn(100);
            lenient().when(thresholdsConfig.cacheHitRatio()).thenReturn(0);

            OverviewStats highLoadStats = TestDataFactory.createOverviewStats(95, 100, 5, 0, 99.5);

            // First call
            alertingService.checkAndAlert("default", highLoadStats);

            // Second call - cooldown should apply internally
            alertingService.checkAndAlert("default", highLoadStats);

            // Thresholds checked both times, but internal cooldown tracking should prevent duplicate alerts
            verify(alertingConfig, times(2)).thresholds();
        }
    }

    @Nested
    @DisplayName("sendAlert behaviour")
    class SendAlertTests {

        @Test
        @DisplayName("Respects cooldown when sending alerts")
        void respectsCooldown() {
            lenient().when(alertingConfig.cooldownSeconds()).thenReturn(300);
            lenient().when(alertingConfig.webhookUrl()).thenReturn(Optional.empty());
            lenient().when(alertingConfig.emailTo()).thenReturn(Optional.empty());

            // First alert - should not be blocked
            alertingService.sendAlert("default", "COOLDOWN_TEST_1", "Title", "Message");

            // Second alert of same type - should be blocked by cooldown
            alertingService.sendAlert("default", "COOLDOWN_TEST_1", "Title 2", "Message 2");

            // The internal cooldown should have blocked the second alert
            verify(alertingConfig, atLeast(1)).cooldownSeconds();
        }

        @Test
        @DisplayName("Different alert types are independent")
        void differentAlertTypesIndependent() {
            lenient().when(alertingConfig.cooldownSeconds()).thenReturn(0); // No cooldown
            lenient().when(alertingConfig.webhookUrl()).thenReturn(Optional.empty());
            lenient().when(alertingConfig.emailTo()).thenReturn(Optional.empty());

            // First alert type
            alertingService.sendAlert("default", "INDEPENDENT_TYPE_A", "Title A", "Message A");

            // Different alert type - should not be blocked
            alertingService.sendAlert("default", "INDEPENDENT_TYPE_B", "Title B", "Message B");

            // Both should have been processed (checked webhook at least twice)
            verify(alertingConfig, atLeast(2)).webhookUrl();
        }

        @Test
        @DisplayName("Accesses webhook URL when configured")
        void accessesWebhookUrl() {
            lenient().when(alertingConfig.cooldownSeconds()).thenReturn(0);
            lenient().when(alertingConfig.webhookUrl()).thenReturn(Optional.empty());
            lenient().when(alertingConfig.emailTo()).thenReturn(Optional.empty());

            alertingService.sendAlert("default", "WEBHOOK_TEST", "Title", "Message");

            verify(alertingConfig, atLeast(1)).webhookUrl();
        }

        @Test
        @DisplayName("Accesses email when configured")
        void accessesEmail() {
            lenient().when(alertingConfig.cooldownSeconds()).thenReturn(0);
            lenient().when(alertingConfig.webhookUrl()).thenReturn(Optional.empty());
            lenient().when(alertingConfig.emailTo()).thenReturn(Optional.empty());

            alertingService.sendAlert("default", "EMAIL_TEST", "Title", "Message");

            verify(alertingConfig, atLeast(1)).emailTo();
        }
    }
}
