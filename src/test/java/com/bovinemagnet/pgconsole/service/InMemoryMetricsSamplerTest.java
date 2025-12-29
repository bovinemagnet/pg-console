package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.config.InstanceConfig;
import com.bovinemagnet.pgconsole.model.OverviewStats;
import com.bovinemagnet.pgconsole.model.SystemMetricsHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link InMemoryMetricsSampler}.
 * <p>
 * Tests the in-memory metrics sampling functionality, including schema-aware
 * execution and integration with the metrics store.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ExtendWith(MockitoExtension.class)
class InMemoryMetricsSamplerTest {

    @Mock
    DataSourceManager dataSourceManager;

    @Mock
    InMemoryMetricsStore metricsStore;

    @Mock
    InstanceConfig config;

    @Mock
    InstanceConfig.SchemaConfig schemaConfig;

    @Mock
    InstanceConfig.AlertingConfig alertingConfig;

    @Mock
    PostgresService postgresService;

    @Mock
    AlertingService alertingService;

    @Mock
    DataSource dataSource;

    @Mock
    Connection connection;

    @Mock
    Statement statement;

    @Mock
    ResultSet resultSet;

    @InjectMocks
    InMemoryMetricsSampler metricsSampler;

    @BeforeEach
    void setUp() {
        lenient().when(config.schema()).thenReturn(schemaConfig);
        lenient().when(config.alerting()).thenReturn(alertingConfig);
    }

    @Nested
    @DisplayName("sampleInMemoryMetrics scheduling tests")
    class SampleInMemoryMetricsTests {

        @Test
        @DisplayName("should not sample when schema is enabled")
        void sampleInMemoryMetrics_whenSchemaEnabled_doesNotSample() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(true);

            // Act
            metricsSampler.sampleInMemoryMetrics();

            // Assert - should not try to get instances
            verify(dataSourceManager, never()).getAvailableInstances();
        }

        @Test
        @DisplayName("should sample when schema is disabled")
        void sampleInMemoryMetrics_whenSchemaDisabled_samplesMetrics() throws SQLException {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(false);
            when(alertingConfig.enabled()).thenReturn(false);
            when(dataSourceManager.getAvailableInstances()).thenReturn(List.of("test-instance"));
            when(dataSourceManager.getDataSource("test-instance")).thenReturn(dataSource);
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.createStatement()).thenReturn(statement);
            when(statement.executeQuery(anyString())).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);

            // Mock ResultSet values
            mockResultSetValues();

            // Act
            metricsSampler.sampleInMemoryMetrics();

            // Assert - should have called addSystemMetrics
            verify(metricsStore).addSystemMetrics(eq("test-instance"), any(SystemMetricsHistory.class));
        }

        @Test
        @DisplayName("should handle multiple instances")
        void sampleInMemoryMetrics_withMultipleInstances_samplesAll() throws SQLException {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(false);
            when(alertingConfig.enabled()).thenReturn(false);
            when(dataSourceManager.getAvailableInstances())
                    .thenReturn(List.of("instance1", "instance2", "instance3"));

            // Setup mocks for each instance
            when(dataSourceManager.getDataSource(anyString())).thenReturn(dataSource);
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.createStatement()).thenReturn(statement);
            when(statement.executeQuery(anyString())).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            mockResultSetValues();

            // Act
            metricsSampler.sampleInMemoryMetrics();

            // Assert - should have sampled all instances
            verify(metricsStore).addSystemMetrics(eq("instance1"), any(SystemMetricsHistory.class));
            verify(metricsStore).addSystemMetrics(eq("instance2"), any(SystemMetricsHistory.class));
            verify(metricsStore).addSystemMetrics(eq("instance3"), any(SystemMetricsHistory.class));
        }

        @Test
        @DisplayName("should handle no instances")
        void sampleInMemoryMetrics_withNoInstances_doesNotThrow() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(false);
            when(dataSourceManager.getAvailableInstances()).thenReturn(Collections.emptyList());

            // Act & Assert - should not throw
            metricsSampler.sampleInMemoryMetrics();

            // Assert - should not have tried to sample
            verify(metricsStore, never()).addSystemMetrics(anyString(), any());
        }

        @Test
        @DisplayName("should continue sampling other instances when one fails")
        void sampleInMemoryMetrics_withFailedInstance_continuesWithOthers() throws SQLException {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(false);
            when(alertingConfig.enabled()).thenReturn(false);
            when(dataSourceManager.getAvailableInstances())
                    .thenReturn(List.of("failing-instance", "working-instance"));

            // First instance throws exception
            when(dataSourceManager.getDataSource("failing-instance"))
                    .thenThrow(new RuntimeException("Database connection failed"));

            // Second instance works
            when(dataSourceManager.getDataSource("working-instance")).thenReturn(dataSource);
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.createStatement()).thenReturn(statement);
            when(statement.executeQuery(anyString())).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            mockResultSetValues();

            // Act
            metricsSampler.sampleInMemoryMetrics();

            // Assert - should have sampled the working instance
            verify(metricsStore).addSystemMetrics(eq("working-instance"), any(SystemMetricsHistory.class));
            verify(metricsStore, never()).addSystemMetrics(eq("failing-instance"), any());
        }
    }

    @Nested
    @DisplayName("System metrics sampling tests")
    class SystemMetricsSamplingTests {

        @Test
        @DisplayName("should store all system metrics correctly")
        void sampleSystemMetrics_storesAllMetricsCorrectly() throws SQLException {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(false);
            when(alertingConfig.enabled()).thenReturn(false);
            when(dataSourceManager.getAvailableInstances()).thenReturn(List.of("test-instance"));
            when(dataSourceManager.getDataSource("test-instance")).thenReturn(dataSource);
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.createStatement()).thenReturn(statement);
            when(statement.executeQuery(anyString())).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);

            // Mock specific values
            when(resultSet.getInt("total_connections")).thenReturn(75);
            when(resultSet.getInt("max_connections")).thenReturn(100);
            when(resultSet.getInt("active_queries")).thenReturn(15);
            when(resultSet.getInt("idle_connections")).thenReturn(50);
            when(resultSet.getInt("idle_in_transaction")).thenReturn(5);
            when(resultSet.getInt("blocked_queries")).thenReturn(3);
            when(resultSet.getDouble("longest_query_seconds")).thenReturn(25.5);
            when(resultSet.wasNull()).thenReturn(false);
            when(resultSet.getDouble("longest_transaction_seconds")).thenReturn(45.0);
            when(resultSet.getDouble("cache_hit_ratio")).thenReturn(97.5);
            when(resultSet.getLong("total_database_size_bytes")).thenReturn(5000000000L);

            ArgumentCaptor<SystemMetricsHistory> metricsCaptor = ArgumentCaptor.forClass(SystemMetricsHistory.class);

            // Act
            metricsSampler.sampleInMemoryMetrics();

            // Assert
            verify(metricsStore).addSystemMetrics(eq("test-instance"), metricsCaptor.capture());

            SystemMetricsHistory capturedMetrics = metricsCaptor.getValue();
            assertThat(capturedMetrics.getTotalConnections()).isEqualTo(75);
            assertThat(capturedMetrics.getMaxConnections()).isEqualTo(100);
            assertThat(capturedMetrics.getActiveQueries()).isEqualTo(15);
            assertThat(capturedMetrics.getIdleConnections()).isEqualTo(50);
            assertThat(capturedMetrics.getIdleInTransaction()).isEqualTo(5);
            assertThat(capturedMetrics.getBlockedQueries()).isEqualTo(3);
            assertThat(capturedMetrics.getLongestQuerySeconds()).isEqualTo(25.5);
            assertThat(capturedMetrics.getLongestTransactionSeconds()).isEqualTo(45.0);
            assertThat(capturedMetrics.getCacheHitRatio()).isEqualTo(97.5);
            assertThat(capturedMetrics.getTotalDatabaseSizeBytes()).isEqualTo(5000000000L);
            assertThat(capturedMetrics.getSampledAt()).isNotNull();
        }

        @Test
        @DisplayName("should handle null double values correctly")
        void sampleSystemMetrics_withNullDoubleValues_handlesCorrectly() throws SQLException {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(false);
            when(alertingConfig.enabled()).thenReturn(false);
            when(dataSourceManager.getAvailableInstances()).thenReturn(List.of("test-instance"));
            when(dataSourceManager.getDataSource("test-instance")).thenReturn(dataSource);
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.createStatement()).thenReturn(statement);
            when(statement.executeQuery(anyString())).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);

            // Mock int values
            when(resultSet.getInt(anyString())).thenReturn(10);

            // Mock null double values
            when(resultSet.getDouble(anyString())).thenReturn(0.0);
            when(resultSet.wasNull()).thenReturn(true);
            when(resultSet.getLong(anyString())).thenReturn(0L);

            ArgumentCaptor<SystemMetricsHistory> metricsCaptor = ArgumentCaptor.forClass(SystemMetricsHistory.class);

            // Act
            metricsSampler.sampleInMemoryMetrics();

            // Assert
            verify(metricsStore).addSystemMetrics(eq("test-instance"), metricsCaptor.capture());

            SystemMetricsHistory capturedMetrics = metricsCaptor.getValue();
            assertThat(capturedMetrics.getLongestQuerySeconds()).isNull();
            assertThat(capturedMetrics.getLongestTransactionSeconds()).isNull();
            assertThat(capturedMetrics.getCacheHitRatio()).isNull();
            assertThat(capturedMetrics.getTotalDatabaseSizeBytes()).isNull();
        }

        @Test
        @DisplayName("should handle SQL exceptions gracefully")
        void sampleSystemMetrics_withSQLException_logsAndContinues() throws SQLException {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(false);
            when(dataSourceManager.getAvailableInstances()).thenReturn(List.of("test-instance"));
            when(dataSourceManager.getDataSource("test-instance")).thenReturn(dataSource);
            when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

            // Act & Assert - should not throw
            metricsSampler.sampleInMemoryMetrics();

            // Should not have stored any metrics
            verify(metricsStore, never()).addSystemMetrics(anyString(), any());
        }

        @Test
        @DisplayName("should close resources properly")
        void sampleSystemMetrics_closesResourcesProperly() throws SQLException {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(false);
            when(alertingConfig.enabled()).thenReturn(false);
            when(dataSourceManager.getAvailableInstances()).thenReturn(List.of("test-instance"));
            when(dataSourceManager.getDataSource("test-instance")).thenReturn(dataSource);
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.createStatement()).thenReturn(statement);
            when(statement.executeQuery(anyString())).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            mockResultSetValues();

            // Act
            metricsSampler.sampleInMemoryMetrics();

            // Assert - resources should be closed
            verify(resultSet).close();
            verify(statement).close();
            verify(connection).close();
        }
    }

    @Nested
    @DisplayName("Alerting integration tests")
    class AlertingIntegrationTests {

        @Test
        @DisplayName("should check alerts when alerting is enabled")
        void sampleInMemoryMetrics_withAlertingEnabled_checksAlerts() throws SQLException {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(false);
            when(alertingConfig.enabled()).thenReturn(true);
            when(dataSourceManager.getAvailableInstances()).thenReturn(List.of("test-instance"));
            when(dataSourceManager.getDataSource("test-instance")).thenReturn(dataSource);
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.createStatement()).thenReturn(statement);
            when(statement.executeQuery(anyString())).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            mockResultSetValues();

            OverviewStats mockStats = new OverviewStats();
            when(postgresService.getOverviewStats("test-instance")).thenReturn(mockStats);

            // Act
            metricsSampler.sampleInMemoryMetrics();

            // Assert
            verify(postgresService).getOverviewStats("test-instance");
            verify(alertingService).checkAndAlert("test-instance", mockStats);
        }

        @Test
        @DisplayName("should not check alerts when alerting is disabled")
        void sampleInMemoryMetrics_withAlertingDisabled_doesNotCheckAlerts() throws SQLException {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(false);
            when(alertingConfig.enabled()).thenReturn(false);
            when(dataSourceManager.getAvailableInstances()).thenReturn(List.of("test-instance"));
            when(dataSourceManager.getDataSource("test-instance")).thenReturn(dataSource);
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.createStatement()).thenReturn(statement);
            when(statement.executeQuery(anyString())).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            mockResultSetValues();

            // Act
            metricsSampler.sampleInMemoryMetrics();

            // Assert
            verify(postgresService, never()).getOverviewStats(anyString());
            verify(alertingService, never()).checkAndAlert(anyString(), any());
        }

        @Test
        @DisplayName("should continue sampling even if alert check fails")
        void sampleInMemoryMetrics_whenAlertCheckFails_continuesSampling() throws SQLException {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(false);
            when(alertingConfig.enabled()).thenReturn(true);
            when(dataSourceManager.getAvailableInstances()).thenReturn(List.of("test-instance"));
            when(dataSourceManager.getDataSource("test-instance")).thenReturn(dataSource);
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.createStatement()).thenReturn(statement);
            when(statement.executeQuery(anyString())).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            mockResultSetValues();

            // Alert checking throws exception
            when(postgresService.getOverviewStats("test-instance"))
                    .thenThrow(new RuntimeException("Failed to get stats"));

            // Act & Assert - should not throw
            metricsSampler.sampleInMemoryMetrics();

            // Should still have stored the metrics
            verify(metricsStore).addSystemMetrics(eq("test-instance"), any(SystemMetricsHistory.class));
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("should handle empty result set")
        void sampleSystemMetrics_withEmptyResultSet_doesNotStore() throws SQLException {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(false);
            when(alertingConfig.enabled()).thenReturn(false);
            when(dataSourceManager.getAvailableInstances()).thenReturn(List.of("test-instance"));
            when(dataSourceManager.getDataSource("test-instance")).thenReturn(dataSource);
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.createStatement()).thenReturn(statement);
            when(statement.executeQuery(anyString())).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false); // No results

            // Act
            metricsSampler.sampleInMemoryMetrics();

            // Assert - should not store metrics
            verify(metricsStore, never()).addSystemMetrics(anyString(), any());
        }

        @Test
        @DisplayName("should handle zero values correctly")
        void sampleSystemMetrics_withZeroValues_storesCorrectly() throws SQLException {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(false);
            when(alertingConfig.enabled()).thenReturn(false);
            when(dataSourceManager.getAvailableInstances()).thenReturn(List.of("test-instance"));
            when(dataSourceManager.getDataSource("test-instance")).thenReturn(dataSource);
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.createStatement()).thenReturn(statement);
            when(statement.executeQuery(anyString())).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);

            // All zeros
            when(resultSet.getInt(anyString())).thenReturn(0);
            when(resultSet.getDouble(anyString())).thenReturn(0.0);
            when(resultSet.wasNull()).thenReturn(false);
            when(resultSet.getLong(anyString())).thenReturn(0L);

            ArgumentCaptor<SystemMetricsHistory> metricsCaptor = ArgumentCaptor.forClass(SystemMetricsHistory.class);

            // Act
            metricsSampler.sampleInMemoryMetrics();

            // Assert
            verify(metricsStore).addSystemMetrics(eq("test-instance"), metricsCaptor.capture());

            SystemMetricsHistory capturedMetrics = metricsCaptor.getValue();
            assertThat(capturedMetrics.getTotalConnections()).isZero();
            assertThat(capturedMetrics.getActiveQueries()).isZero();
            assertThat(capturedMetrics.getCacheHitRatio()).isEqualTo(0.0);
        }
    }

    // Helper method to mock ResultSet values
    private void mockResultSetValues() throws SQLException {
        when(resultSet.getInt("total_connections")).thenReturn(50);
        when(resultSet.getInt("max_connections")).thenReturn(100);
        when(resultSet.getInt("active_queries")).thenReturn(10);
        when(resultSet.getInt("idle_connections")).thenReturn(30);
        when(resultSet.getInt("idle_in_transaction")).thenReturn(5);
        when(resultSet.getInt("blocked_queries")).thenReturn(2);
        when(resultSet.getDouble("longest_query_seconds")).thenReturn(15.5);
        when(resultSet.getDouble("longest_transaction_seconds")).thenReturn(30.0);
        when(resultSet.getDouble("cache_hit_ratio")).thenReturn(98.5);
        when(resultSet.getLong("total_database_size_bytes")).thenReturn(1024000000L);
        when(resultSet.wasNull()).thenReturn(false);
    }
}
