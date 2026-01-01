package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.config.InstanceConfig;
import com.bovinemagnet.pgconsole.model.SystemMetricsHistory;
import com.bovinemagnet.pgconsole.repository.HistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SparklineService} schema-enabled fallback logic.
 * <p>
 * Tests the routing between persistent storage (HistoryRepository) and
 * in-memory storage (InMemoryMetricsStore) based on schema configuration.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ExtendWith(MockitoExtension.class)
class SparklineServiceSchemaTest {

    @Mock
    HistoryRepository historyRepository;

    @Mock
    InMemoryMetricsStore inMemoryMetricsStore;

    @Mock
    InstanceConfig config;

    @Mock
    InstanceConfig.SchemaConfig schemaConfig;

    @InjectMocks
    SparklineService sparklineService;

    @BeforeEach
    void setUp() {
        lenient().when(config.schema()).thenReturn(schemaConfig);
    }

    @Nested
    @DisplayName("Schema-enabled data source routing")
    class SchemaEnabledRoutingTests {

        @Test
        @DisplayName("should use HistoryRepository when schema is enabled")
        void getConnectionsSparkline_whenSchemaEnabled_usesHistoryRepository() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(true);
            List<SystemMetricsHistory> mockHistory = createMockSystemMetricsHistory(5);
            when(historyRepository.getSystemMetricsHistory(eq("test-instance"), anyInt()))
                    .thenReturn(mockHistory);

            // Act
            String sparkline = sparklineService.getConnectionsSparkline("test-instance", 1, 120, 24);

            // Assert
            verify(historyRepository).getSystemMetricsHistory("test-instance", 1);
            verify(inMemoryMetricsStore, never()).getSystemMetricsHistory(anyString(), anyInt());
            assertThat(sparkline).isNotNull();
            assertThat(sparkline).contains("<svg");
        }

        @Test
        @DisplayName("should use InMemoryMetricsStore when schema is disabled")
        void getConnectionsSparkline_whenSchemaDisabled_usesInMemoryStore() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(false);
            List<SystemMetricsHistory> mockHistory = createMockSystemMetricsHistory(5);
            when(inMemoryMetricsStore.getSystemMetricsHistory(eq("test-instance"), anyInt()))
                    .thenReturn(mockHistory);

            // Act
            String sparkline = sparklineService.getConnectionsSparkline("test-instance", 1, 120, 24);

            // Assert
            verify(inMemoryMetricsStore).getSystemMetricsHistory("test-instance", 1);
            verify(historyRepository, never()).getSystemMetricsHistory(anyString(), anyInt());
            assertThat(sparkline).isNotNull();
            assertThat(sparkline).contains("<svg");
        }
    }

    @Nested
    @DisplayName("Connections sparkline tests")
    class ConnectionsSparklineTests {

        @Test
        @DisplayName("should generate area sparkline with schema enabled")
        void getConnectionsSparkline_withSchemaEnabled_generatesAreaSparkline() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(true);
            List<SystemMetricsHistory> mockHistory = createMockSystemMetricsHistory(3);
            when(historyRepository.getSystemMetricsHistory(anyString(), anyInt())).thenReturn(mockHistory);

            // Act
            String sparkline = sparklineService.getConnectionsSparkline("test-instance", 1, 100, 20);

            // Assert
            assertThat(sparkline).contains("fill-opacity=\"0.2\"");
            assertThat(sparkline).contains(SparklineService.COLOUR_PRIMARY);
        }

        @Test
        @DisplayName("should generate area sparkline with schema disabled")
        void getConnectionsSparkline_withSchemaDisabled_generatesAreaSparkline() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(false);
            List<SystemMetricsHistory> mockHistory = createMockSystemMetricsHistory(3);
            when(inMemoryMetricsStore.getSystemMetricsHistory(anyString(), anyInt())).thenReturn(mockHistory);

            // Act
            String sparkline = sparklineService.getConnectionsSparkline("test-instance", 1, 100, 20);

            // Assert
            assertThat(sparkline).contains("fill-opacity=\"0.2\"");
            assertThat(sparkline).contains(SparklineService.COLOUR_PRIMARY);
        }

        @Test
        @DisplayName("should use default instance when not specified")
        void getConnectionsSparkline_withoutInstanceId_usesDefaultInstance() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(true);
            List<SystemMetricsHistory> mockHistory = createMockSystemMetricsHistory(3);
            when(historyRepository.getSystemMetricsHistory(eq("default"), anyInt())).thenReturn(mockHistory);

            // Act
            String sparkline = sparklineService.getConnectionsSparkline(1, 100, 20);

            // Assert
            verify(historyRepository).getSystemMetricsHistory("default", 1);
        }
    }

    @Nested
    @DisplayName("Active queries sparkline tests")
    class ActiveQueriesSparklineTests {

        @Test
        @DisplayName("should use HistoryRepository when schema is enabled")
        void getActiveQueriesSparkline_whenSchemaEnabled_usesHistoryRepository() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(true);
            List<SystemMetricsHistory> mockHistory = createMockSystemMetricsHistory(4);
            when(historyRepository.getSystemMetricsHistory(anyString(), anyInt())).thenReturn(mockHistory);

            // Act
            String sparkline = sparklineService.getActiveQueriesSparkline("test-instance", 1, 120, 24);

            // Assert
            verify(historyRepository).getSystemMetricsHistory("test-instance", 1);
            assertThat(sparkline).contains(SparklineService.COLOUR_SUCCESS);
        }

        @Test
        @DisplayName("should use InMemoryMetricsStore when schema is disabled")
        void getActiveQueriesSparkline_whenSchemaDisabled_usesInMemoryStore() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(false);
            List<SystemMetricsHistory> mockHistory = createMockSystemMetricsHistory(4);
            when(inMemoryMetricsStore.getSystemMetricsHistory(anyString(), anyInt())).thenReturn(mockHistory);

            // Act
            String sparkline = sparklineService.getActiveQueriesSparkline("test-instance", 1, 120, 24);

            // Assert
            verify(inMemoryMetricsStore).getSystemMetricsHistory("test-instance", 1);
            assertThat(sparkline).contains(SparklineService.COLOUR_SUCCESS);
        }

        @Test
        @DisplayName("should use default instance when not specified")
        void getActiveQueriesSparkline_withoutInstanceId_usesDefaultInstance() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(false);
            List<SystemMetricsHistory> mockHistory = createMockSystemMetricsHistory(3);
            when(inMemoryMetricsStore.getSystemMetricsHistory(eq("default"), anyInt())).thenReturn(mockHistory);

            // Act
            String sparkline = sparklineService.getActiveQueriesSparkline(1, 100, 20);

            // Assert
            verify(inMemoryMetricsStore).getSystemMetricsHistory("default", 1);
        }
    }

    @Nested
    @DisplayName("Blocked queries sparkline tests")
    class BlockedQueriesSparklineTests {

        @Test
        @DisplayName("should use HistoryRepository when schema is enabled")
        void getBlockedQueriesSparkline_whenSchemaEnabled_usesHistoryRepository() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(true);
            List<SystemMetricsHistory> mockHistory = createMockSystemMetricsHistory(3);
            when(historyRepository.getSystemMetricsHistory(anyString(), anyInt())).thenReturn(mockHistory);

            // Act
            String sparkline = sparklineService.getBlockedQueriesSparkline("test-instance", 1, 120, 24);

            // Assert
            verify(historyRepository).getSystemMetricsHistory("test-instance", 1);
            assertThat(sparkline).contains(SparklineService.COLOUR_DANGER);
        }

        @Test
        @DisplayName("should use InMemoryMetricsStore when schema is disabled")
        void getBlockedQueriesSparkline_whenSchemaDisabled_usesInMemoryStore() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(false);
            List<SystemMetricsHistory> mockHistory = createMockSystemMetricsHistory(3);
            when(inMemoryMetricsStore.getSystemMetricsHistory(anyString(), anyInt())).thenReturn(mockHistory);

            // Act
            String sparkline = sparklineService.getBlockedQueriesSparkline("test-instance", 1, 120, 24);

            // Assert
            verify(inMemoryMetricsStore).getSystemMetricsHistory("test-instance", 1);
            assertThat(sparkline).contains(SparklineService.COLOUR_DANGER);
        }

        @Test
        @DisplayName("should use default instance when not specified")
        void getBlockedQueriesSparkline_withoutInstanceId_usesDefaultInstance() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(true);
            List<SystemMetricsHistory> mockHistory = createMockSystemMetricsHistory(3);
            when(historyRepository.getSystemMetricsHistory(eq("default"), anyInt())).thenReturn(mockHistory);

            // Act
            String sparkline = sparklineService.getBlockedQueriesSparkline(1, 100, 20);

            // Assert
            verify(historyRepository).getSystemMetricsHistory("default", 1);
        }
    }

    @Nested
    @DisplayName("Cache hit ratio sparkline tests")
    class CacheHitRatioSparklineTests {

        @Test
        @DisplayName("should use HistoryRepository when schema is enabled")
        void getCacheHitRatioSparkline_whenSchemaEnabled_usesHistoryRepository() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(true);
            List<SystemMetricsHistory> mockHistory = createMockSystemMetricsHistory(5);
            when(historyRepository.getSystemMetricsHistory(anyString(), anyInt())).thenReturn(mockHistory);

            // Act
            String sparkline = sparklineService.getCacheHitRatioSparkline("test-instance", 1, 120, 24);

            // Assert
            verify(historyRepository).getSystemMetricsHistory("test-instance", 1);
            assertThat(sparkline).contains(SparklineService.COLOUR_INFO);
        }

        @Test
        @DisplayName("should use InMemoryMetricsStore when schema is disabled")
        void getCacheHitRatioSparkline_whenSchemaDisabled_usesInMemoryStore() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(false);
            List<SystemMetricsHistory> mockHistory = createMockSystemMetricsHistory(5);
            when(inMemoryMetricsStore.getSystemMetricsHistory(anyString(), anyInt())).thenReturn(mockHistory);

            // Act
            String sparkline = sparklineService.getCacheHitRatioSparkline("test-instance", 1, 120, 24);

            // Assert
            verify(inMemoryMetricsStore).getSystemMetricsHistory("test-instance", 1);
            assertThat(sparkline).contains(SparklineService.COLOUR_INFO);
        }

        @Test
        @DisplayName("should handle null cache hit ratio values")
        void getCacheHitRatioSparkline_withNullValues_defaultsTo100() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(true);
            List<SystemMetricsHistory> mockHistory = createMockSystemMetricsHistoryWithNullCacheRatio(3);
            when(historyRepository.getSystemMetricsHistory(anyString(), anyInt())).thenReturn(mockHistory);

            // Act
            String sparkline = sparklineService.getCacheHitRatioSparkline("test-instance", 1, 120, 24);

            // Assert
            assertThat(sparkline).isNotNull();
            assertThat(sparkline).contains("<svg");
        }

        @Test
        @DisplayName("should use default instance when not specified")
        void getCacheHitRatioSparkline_withoutInstanceId_usesDefaultInstance() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(false);
            List<SystemMetricsHistory> mockHistory = createMockSystemMetricsHistory(3);
            when(inMemoryMetricsStore.getSystemMetricsHistory(eq("default"), anyInt())).thenReturn(mockHistory);

            // Act
            String sparkline = sparklineService.getCacheHitRatioSparkline(1, 100, 20);

            // Assert
            verify(inMemoryMetricsStore).getSystemMetricsHistory("default", 1);
        }
    }

    @Nested
    @DisplayName("isSchemaEnabled tests")
    class IsSchemaEnabledTests {

        @Test
        @DisplayName("should return true when schema is enabled")
        void isSchemaEnabled_whenEnabled_returnsTrue() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(true);

            // Act
            boolean result = sparklineService.isSchemaEnabled();

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when schema is disabled")
        void isSchemaEnabled_whenDisabled_returnsFalse() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(false);

            // Act
            boolean result = sparklineService.isSchemaEnabled();

            // Assert
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Empty data handling")
    class EmptyDataHandlingTests {

        @Test
        @DisplayName("should generate empty sparkline when no data from repository")
        void getConnectionsSparkline_withNoDataFromRepository_generatesEmptySparkline() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(true);
            when(historyRepository.getSystemMetricsHistory(anyString(), anyInt()))
                    .thenReturn(new ArrayList<>());

            // Act
            String sparkline = sparklineService.getConnectionsSparkline("test-instance", 1, 120, 24);

            // Assert
            assertThat(sparkline).contains("sparkline-empty");
            assertThat(sparkline).contains("stroke-dasharray");
        }

        @Test
        @DisplayName("should generate empty sparkline when no data from in-memory store")
        void getConnectionsSparkline_withNoDataFromInMemory_generatesEmptySparkline() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(false);
            when(inMemoryMetricsStore.getSystemMetricsHistory(anyString(), anyInt()))
                    .thenReturn(new ArrayList<>());

            // Act
            String sparkline = sparklineService.getConnectionsSparkline("test-instance", 1, 120, 24);

            // Assert
            assertThat(sparkline).contains("sparkline-empty");
            assertThat(sparkline).contains("stroke-dasharray");
        }

        @Test
        @DisplayName("should generate empty sparkline when only one data point")
        void getConnectionsSparkline_withOneDataPoint_generatesEmptySparkline() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(true);
            List<SystemMetricsHistory> mockHistory = createMockSystemMetricsHistory(1);
            when(historyRepository.getSystemMetricsHistory(anyString(), anyInt())).thenReturn(mockHistory);

            // Act
            String sparkline = sparklineService.getConnectionsSparkline("test-instance", 1, 120, 24);

            // Assert
            assertThat(sparkline).contains("sparkline-empty");
        }
    }

    // Helper methods

    /**
     * Creates mock system metrics history with specified number of entries.
     */
    private List<SystemMetricsHistory> createMockSystemMetricsHistory(int count) {
        List<SystemMetricsHistory> history = new ArrayList<>();
        Instant now = Instant.now();

        for (int i = 0; i < count; i++) {
            SystemMetricsHistory metrics = new SystemMetricsHistory();
            metrics.setSampledAt(now.minusSeconds(i * 60L));
            metrics.setTotalConnections(50 + i * 5);
            metrics.setActiveQueries(10 + i * 2);
            metrics.setBlockedQueries(i);
            metrics.setCacheHitRatio(95.0 + i);
            history.add(metrics);
        }

        return history;
    }

    /**
     * Creates mock system metrics history with null cache hit ratio values.
     */
    private List<SystemMetricsHistory> createMockSystemMetricsHistoryWithNullCacheRatio(int count) {
        List<SystemMetricsHistory> history = new ArrayList<>();
        Instant now = Instant.now();

        for (int i = 0; i < count; i++) {
            SystemMetricsHistory metrics = new SystemMetricsHistory();
            metrics.setSampledAt(now.minusSeconds(i * 60L));
            metrics.setTotalConnections(50 + i * 5);
            metrics.setActiveQueries(10 + i * 2);
            metrics.setBlockedQueries(i);
            metrics.setCacheHitRatio(null); // Null value
            history.add(metrics);
        }

        return history;
    }
}
