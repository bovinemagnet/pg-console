package com.bovinemagnet.pgconsole;

import com.bovinemagnet.pgconsole.config.InstanceConfig;
import io.quarkus.runtime.StartupEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SchemaStartupLogger}.
 * <p>
 * Tests the logging behaviour at application startup for different schema configurations.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ExtendWith(MockitoExtension.class)
class SchemaStartupLoggerTest {

    @Mock
    InstanceConfig config;

    @Mock
    InstanceConfig.SchemaConfig schemaConfig;

    @Mock
    InstanceConfig.HistoryConfig historyConfig;

    @Mock
    StartupEvent startupEvent;

    @InjectMocks
    SchemaStartupLogger schemaStartupLogger;

    @BeforeEach
    void setUp() {
        lenient().when(config.schema()).thenReturn(schemaConfig);
        lenient().when(config.history()).thenReturn(historyConfig);
    }

    @Nested
    @DisplayName("Schema enabled logging")
    class SchemaEnabledLoggingTests {

        @Test
        @DisplayName("should log schema enabled with history enabled")
        void onStart_withSchemaAndHistoryEnabled_logsCorrectly() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(true);
            when(historyConfig.enabled()).thenReturn(true);
            when(historyConfig.intervalSeconds()).thenReturn(60);
            when(historyConfig.retentionDays()).thenReturn(7);

            // Act
            schemaStartupLogger.onStart(startupEvent);

            // Assert - verify config was accessed at least once
            verify(schemaConfig, atLeastOnce()).enabled();
            verify(historyConfig, atLeastOnce()).enabled();
            verify(historyConfig, atLeastOnce()).intervalSeconds();
            verify(historyConfig, atLeastOnce()).retentionDays();
        }

        @Test
        @DisplayName("should log schema enabled with history disabled")
        void onStart_withSchemaEnabledHistoryDisabled_logsCorrectly() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(true);
            when(historyConfig.enabled()).thenReturn(false);

            // Act
            schemaStartupLogger.onStart(startupEvent);

            // Assert
            verify(schemaConfig, atLeastOnce()).enabled();
            verify(historyConfig, atLeastOnce()).enabled();
        }

        @Test
        @DisplayName("should log custom sampling interval")
        void onStart_withCustomSamplingInterval_logsInterval() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(true);
            when(historyConfig.enabled()).thenReturn(true);
            when(historyConfig.intervalSeconds()).thenReturn(120);
            when(historyConfig.retentionDays()).thenReturn(14);

            // Act
            schemaStartupLogger.onStart(startupEvent);

            // Assert
            verify(historyConfig).intervalSeconds();
            verify(historyConfig).retentionDays();
        }
    }

    @Nested
    @DisplayName("Schema disabled logging")
    class SchemaDisabledLoggingTests {

        @Test
        @DisplayName("should log schema disabled with in-memory retention")
        void onStart_withSchemaDisabled_logsInMemoryMode() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(false);
            when(schemaConfig.inMemoryMinutes()).thenReturn(30);

            // Act
            schemaStartupLogger.onStart(startupEvent);

            // Assert
            verify(schemaConfig).enabled();
            verify(schemaConfig).inMemoryMinutes();
        }

        @Test
        @DisplayName("should log custom in-memory retention period")
        void onStart_withCustomInMemoryRetention_logsRetention() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(false);
            when(schemaConfig.inMemoryMinutes()).thenReturn(60);

            // Act
            schemaStartupLogger.onStart(startupEvent);

            // Assert
            verify(schemaConfig).inMemoryMinutes();
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should handle exceptions gracefully")
        void onStart_withException_doesNotThrow() {
            // Arrange
            when(schemaConfig.enabled()).thenThrow(new RuntimeException("Config error"));

            // Act & Assert - should not throw
            schemaStartupLogger.onStart(startupEvent);

            // Verify it attempted to check schema enabled
            verify(schemaConfig).enabled();
        }

        @Test
        @DisplayName("should handle null config gracefully")
        void onStart_withNullConfig_doesNotThrow() {
            // Arrange
            when(config.schema()).thenReturn(null);

            // Act & Assert - should not throw (NPE handled internally)
            schemaStartupLogger.onStart(startupEvent);
        }
    }

    @Nested
    @DisplayName("Startup event handling")
    class StartupEventHandlingTests {

        @Test
        @DisplayName("should be called on application startup")
        void onStart_triggeredByStartupEvent_logsSchemaMode() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(true);
            when(historyConfig.enabled()).thenReturn(true);
            when(historyConfig.intervalSeconds()).thenReturn(60);
            when(historyConfig.retentionDays()).thenReturn(7);

            // Act
            schemaStartupLogger.onStart(startupEvent);

            // Assert - verify it processed the startup event
            verify(config).schema();
            verify(schemaConfig).enabled();
        }

        @Test
        @DisplayName("should work with different startup event instances")
        void onStart_withDifferentEvents_worksCorrectly() {
            // Arrange
            StartupEvent event1 = mock(StartupEvent.class);
            StartupEvent event2 = mock(StartupEvent.class);
            when(schemaConfig.enabled()).thenReturn(true);
            when(historyConfig.enabled()).thenReturn(false);

            // Act
            schemaStartupLogger.onStart(event1);
            schemaStartupLogger.onStart(event2);

            // Assert - should have been called twice
            verify(config, times(2)).schema();
        }
    }

    @Nested
    @DisplayName("Configuration variations")
    class ConfigurationVariationsTests {

        @Test
        @DisplayName("should handle minimum retention period")
        void onStart_withMinimumRetention_logsCorrectly() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(true);
            when(historyConfig.enabled()).thenReturn(true);
            when(historyConfig.intervalSeconds()).thenReturn(30);
            when(historyConfig.retentionDays()).thenReturn(1);

            // Act
            schemaStartupLogger.onStart(startupEvent);

            // Assert
            verify(historyConfig).retentionDays();
        }

        @Test
        @DisplayName("should handle maximum retention period")
        void onStart_withMaximumRetention_logsCorrectly() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(true);
            when(historyConfig.enabled()).thenReturn(true);
            when(historyConfig.intervalSeconds()).thenReturn(60);
            when(historyConfig.retentionDays()).thenReturn(365);

            // Act
            schemaStartupLogger.onStart(startupEvent);

            // Assert
            verify(historyConfig).retentionDays();
        }

        @Test
        @DisplayName("should handle minimum in-memory retention")
        void onStart_withMinimumInMemoryRetention_logsCorrectly() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(false);
            when(schemaConfig.inMemoryMinutes()).thenReturn(5);

            // Act
            schemaStartupLogger.onStart(startupEvent);

            // Assert
            verify(schemaConfig).inMemoryMinutes();
        }

        @Test
        @DisplayName("should handle maximum in-memory retention")
        void onStart_withMaximumInMemoryRetention_logsCorrectly() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(false);
            when(schemaConfig.inMemoryMinutes()).thenReturn(1440); // 24 hours

            // Act
            schemaStartupLogger.onStart(startupEvent);

            // Assert
            verify(schemaConfig).inMemoryMinutes();
        }
    }

    @Nested
    @DisplayName("Multiple invocations")
    class MultipleInvocationsTests {

        @Test
        @DisplayName("should handle being called multiple times")
        void onStart_calledMultipleTimes_logsEachTime() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(true);
            when(historyConfig.enabled()).thenReturn(true);
            when(historyConfig.intervalSeconds()).thenReturn(60);
            when(historyConfig.retentionDays()).thenReturn(7);

            // Act
            schemaStartupLogger.onStart(startupEvent);
            schemaStartupLogger.onStart(startupEvent);
            schemaStartupLogger.onStart(startupEvent);

            // Assert - should have accessed config each time
            verify(config, times(3)).schema();
            verify(schemaConfig, times(3)).enabled();
        }

        @Test
        @DisplayName("should handle config changes between invocations")
        void onStart_withConfigChanges_logsUpdatedValues() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(true).thenReturn(false);
            when(historyConfig.enabled()).thenReturn(true);
            when(historyConfig.intervalSeconds()).thenReturn(60);
            when(historyConfig.retentionDays()).thenReturn(7);
            when(schemaConfig.inMemoryMinutes()).thenReturn(30);

            // Act
            schemaStartupLogger.onStart(startupEvent); // First call - schema enabled
            schemaStartupLogger.onStart(startupEvent); // Second call - schema disabled

            // Assert - verify both invocations happened
            verify(config, atLeast(2)).schema();
            verify(schemaConfig, atLeast(2)).enabled();
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("should handle zero interval seconds")
        void onStart_withZeroIntervalSeconds_doesNotThrow() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(true);
            when(historyConfig.enabled()).thenReturn(true);
            when(historyConfig.intervalSeconds()).thenReturn(0);
            when(historyConfig.retentionDays()).thenReturn(7);

            // Act & Assert
            schemaStartupLogger.onStart(startupEvent);
        }

        @Test
        @DisplayName("should handle zero retention days")
        void onStart_withZeroRetentionDays_doesNotThrow() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(true);
            when(historyConfig.enabled()).thenReturn(true);
            when(historyConfig.intervalSeconds()).thenReturn(60);
            when(historyConfig.retentionDays()).thenReturn(0);

            // Act & Assert
            schemaStartupLogger.onStart(startupEvent);
        }

        @Test
        @DisplayName("should handle zero in-memory minutes")
        void onStart_withZeroInMemoryMinutes_doesNotThrow() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(false);
            when(schemaConfig.inMemoryMinutes()).thenReturn(0);

            // Act & Assert
            schemaStartupLogger.onStart(startupEvent);
        }
    }
}
