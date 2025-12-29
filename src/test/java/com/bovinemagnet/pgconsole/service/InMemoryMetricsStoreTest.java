package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.config.InstanceConfig;
import com.bovinemagnet.pgconsole.model.SystemMetricsHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link InMemoryMetricsStore}.
 * <p>
 * Tests the in-memory storage functionality for metrics when schema is disabled,
 * including adding metrics, retrieving history, eviction, and thread safety considerations.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ExtendWith(MockitoExtension.class)
class InMemoryMetricsStoreTest {

    @Mock
    InstanceConfig config;

    @Mock
    InstanceConfig.SchemaConfig schemaConfig;

    @InjectMocks
    InMemoryMetricsStore metricsStore;

    @BeforeEach
    void setUp() {
        lenient().when(config.schema()).thenReturn(schemaConfig);
        lenient().when(schemaConfig.inMemoryMinutes()).thenReturn(30);
    }

    @Nested
    @DisplayName("addSystemMetrics tests")
    class AddSystemMetricsTests {

        @Test
        @DisplayName("should add metrics successfully with valid data")
        void addSystemMetrics_withValidData_storesMetrics() {
            // Arrange
            String instanceId = "test-instance";
            SystemMetricsHistory metrics = createTestMetrics();
            metrics.setSampledAt(Instant.now());

            // Act
            metricsStore.addSystemMetrics(instanceId, metrics);

            // Assert
            assertThat(metricsStore.getMetricsCount(instanceId)).isEqualTo(1);
        }

        @Test
        @DisplayName("should set sampledAt when null")
        void addSystemMetrics_withNullSampledAt_setsSampledAt() {
            // Arrange
            String instanceId = "test-instance";
            SystemMetricsHistory metrics = createTestMetrics();
            metrics.setSampledAt(null);

            // Act
            metricsStore.addSystemMetrics(instanceId, metrics);

            // Assert
            List<SystemMetricsHistory> history = metricsStore.getSystemMetricsHistory(instanceId, 1);
            assertThat(history).hasSize(1);
            assertThat(history.get(0).getSampledAt()).isNotNull();
        }

        @Test
        @DisplayName("should ignore null instance ID")
        void addSystemMetrics_withNullInstanceId_ignoresMetrics() {
            // Arrange
            SystemMetricsHistory metrics = createTestMetrics();

            // Act - should not throw
            metricsStore.addSystemMetrics(null, metrics);

            // Assert - nothing should be stored (implementation returns early)
            // No verification needed as the implementation guards against null
        }

        @Test
        @DisplayName("should ignore null metrics")
        void addSystemMetrics_withNullMetrics_ignoresRequest() {
            // Arrange
            String instanceId = "test-instance";

            // Act
            metricsStore.addSystemMetrics(instanceId, null);

            // Assert
            assertThat(metricsStore.getMetricsCount(instanceId)).isZero();
        }

        @Test
        @DisplayName("should add multiple metrics in order")
        void addSystemMetrics_multipleMetrics_maintainsOrder() {
            // Arrange
            String instanceId = "test-instance";
            Instant now = Instant.now();

            SystemMetricsHistory metrics1 = createTestMetrics();
            metrics1.setSampledAt(now.minus(2, ChronoUnit.MINUTES));

            SystemMetricsHistory metrics2 = createTestMetrics();
            metrics2.setSampledAt(now.minus(1, ChronoUnit.MINUTES));

            SystemMetricsHistory metrics3 = createTestMetrics();
            metrics3.setSampledAt(now);

            // Act
            metricsStore.addSystemMetrics(instanceId, metrics1);
            metricsStore.addSystemMetrics(instanceId, metrics2);
            metricsStore.addSystemMetrics(instanceId, metrics3);

            // Assert
            assertThat(metricsStore.getMetricsCount(instanceId)).isEqualTo(3);
            List<SystemMetricsHistory> history = metricsStore.getSystemMetricsHistory(instanceId, 1);
            assertThat(history).hasSize(3);
        }
    }

    @Nested
    @DisplayName("getSystemMetricsHistory tests")
    class GetSystemMetricsHistoryTests {

        @Test
        @DisplayName("should return empty list for null instance ID")
        void getSystemMetricsHistory_withNullInstanceId_returnsEmptyList() {
            // Act
            List<SystemMetricsHistory> history = metricsStore.getSystemMetricsHistory(null, 1);

            // Assert
            assertThat(history).isEmpty();
        }

        @Test
        @DisplayName("should return empty list for unknown instance")
        void getSystemMetricsHistory_withUnknownInstance_returnsEmptyList() {
            // Act
            List<SystemMetricsHistory> history = metricsStore.getSystemMetricsHistory("unknown", 1);

            // Assert
            assertThat(history).isEmpty();
        }

        @Test
        @DisplayName("should return metrics within time window")
        void getSystemMetricsHistory_withTimeWindow_returnsFilteredMetrics() {
            // Arrange
            String instanceId = "test-instance";
            Instant now = Instant.now();

            SystemMetricsHistory oldMetrics = createTestMetrics();
            oldMetrics.setSampledAt(now.minus(3, ChronoUnit.HOURS));

            SystemMetricsHistory recentMetrics1 = createTestMetrics();
            recentMetrics1.setSampledAt(now.minus(30, ChronoUnit.MINUTES));

            SystemMetricsHistory recentMetrics2 = createTestMetrics();
            recentMetrics2.setSampledAt(now.minus(10, ChronoUnit.MINUTES));

            metricsStore.addSystemMetrics(instanceId, oldMetrics);
            metricsStore.addSystemMetrics(instanceId, recentMetrics1);
            metricsStore.addSystemMetrics(instanceId, recentMetrics2);

            // Act
            List<SystemMetricsHistory> history = metricsStore.getSystemMetricsHistory(instanceId, 1);

            // Assert - should only return metrics from last 1 hour
            assertThat(history).hasSize(2);
        }

        @Test
        @DisplayName("should auto-set sampledAt when null")
        void getSystemMetricsHistory_withNullSampledAt_autoSetsTimestamp() {
            // Arrange
            String instanceId = "test-instance";

            SystemMetricsHistory metrics = createTestMetrics();
            metrics.setSampledAt(null);

            // Act
            metricsStore.addSystemMetrics(instanceId, metrics);

            // Assert - the implementation auto-sets sampledAt to now() if null
            List<SystemMetricsHistory> history = metricsStore.getSystemMetricsHistory(instanceId, 1);
            assertThat(history).hasSize(1);
            assertThat(history.get(0).getSampledAt()).isNotNull();
        }

        @Test
        @DisplayName("should return all metrics when time window is large")
        void getSystemMetricsHistory_withLargeTimeWindow_returnsAllMetrics() {
            // Arrange
            String instanceId = "test-instance";
            Instant now = Instant.now();

            for (int i = 0; i < 5; i++) {
                SystemMetricsHistory metrics = createTestMetrics();
                metrics.setSampledAt(now.minus(i * 10, ChronoUnit.MINUTES));
                metricsStore.addSystemMetrics(instanceId, metrics);
            }

            // Act
            List<SystemMetricsHistory> history = metricsStore.getSystemMetricsHistory(instanceId, 24);

            // Assert
            assertThat(history).hasSize(5);
        }
    }

    @Nested
    @DisplayName("getMetricsCount tests")
    class GetMetricsCountTests {

        @Test
        @DisplayName("should throw NPE for null instance ID")
        void getMetricsCount_withNullInstanceId_throwsNPE() {
            // Act & Assert - ConcurrentHashMap.get(null) throws NPE
            org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> {
                metricsStore.getMetricsCount(null);
            });
        }

        @Test
        @DisplayName("should return zero for unknown instance")
        void getMetricsCount_withUnknownInstance_returnsZero() {
            // Act
            int count = metricsStore.getMetricsCount("unknown");

            // Assert
            assertThat(count).isZero();
        }

        @Test
        @DisplayName("should return correct count for instance")
        void getMetricsCount_withMetrics_returnsCorrectCount() {
            // Arrange
            String instanceId = "test-instance";
            for (int i = 0; i < 5; i++) {
                metricsStore.addSystemMetrics(instanceId, createTestMetrics());
            }

            // Act
            int count = metricsStore.getMetricsCount(instanceId);

            // Assert
            assertThat(count).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("clear tests")
    class ClearTests {

        @Test
        @DisplayName("should clear all metrics")
        void clear_clearsAllMetrics() {
            // Arrange
            metricsStore.addSystemMetrics("instance1", createTestMetrics());
            metricsStore.addSystemMetrics("instance2", createTestMetrics());

            // Act
            metricsStore.clear();

            // Assert
            assertThat(metricsStore.getMetricsCount("instance1")).isZero();
            assertThat(metricsStore.getMetricsCount("instance2")).isZero();
        }

        @Test
        @DisplayName("should clear metrics for specific instance")
        void clear_withInstanceId_clearsSingleInstance() {
            // Arrange
            metricsStore.addSystemMetrics("instance1", createTestMetrics());
            metricsStore.addSystemMetrics("instance2", createTestMetrics());

            // Act
            metricsStore.clear("instance1");

            // Assert
            assertThat(metricsStore.getMetricsCount("instance1")).isZero();
            assertThat(metricsStore.getMetricsCount("instance2")).isEqualTo(1);
        }

        @Test
        @DisplayName("should handle clearing non-existent instance")
        void clear_withNonExistentInstance_doesNotThrow() {
            // Act & Assert - should not throw
            metricsStore.clear("non-existent");
        }
    }

    @Nested
    @DisplayName("evictOldEntries tests")
    class EvictOldEntriesTests {

        @Test
        @DisplayName("should not evict when schema is enabled")
        void evictOldEntries_whenSchemaEnabled_doesNotEvict() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(true);
            String instanceId = "test-instance";

            SystemMetricsHistory oldMetrics = createTestMetrics();
            oldMetrics.setSampledAt(Instant.now().minus(60, ChronoUnit.MINUTES));
            metricsStore.addSystemMetrics(instanceId, oldMetrics);

            // Act
            metricsStore.evictOldEntries();

            // Assert - metrics should still be there
            assertThat(metricsStore.getMetricsCount(instanceId)).isEqualTo(1);
        }

        @Test
        @DisplayName("should evict old entries when schema is disabled")
        void evictOldEntries_whenSchemaDisabled_evictsOldMetrics() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(false);
            when(schemaConfig.inMemoryMinutes()).thenReturn(30);

            String instanceId = "test-instance";
            Instant now = Instant.now();

            // Add old metrics (older than retention period)
            SystemMetricsHistory oldMetrics = createTestMetrics();
            oldMetrics.setSampledAt(now.minus(45, ChronoUnit.MINUTES));
            metricsStore.addSystemMetrics(instanceId, oldMetrics);

            // Add recent metrics (within retention period)
            SystemMetricsHistory recentMetrics = createTestMetrics();
            recentMetrics.setSampledAt(now.minus(10, ChronoUnit.MINUTES));
            metricsStore.addSystemMetrics(instanceId, recentMetrics);

            // Act
            metricsStore.evictOldEntries();

            // Assert - should only have recent metrics
            assertThat(metricsStore.getMetricsCount(instanceId)).isEqualTo(1);
        }

        @Test
        @DisplayName("should keep all metrics when within retention period")
        void evictOldEntries_withRecentMetrics_keepsAll() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(false);
            when(schemaConfig.inMemoryMinutes()).thenReturn(30);

            String instanceId = "test-instance";
            Instant now = Instant.now();

            for (int i = 0; i < 5; i++) {
                SystemMetricsHistory metrics = createTestMetrics();
                metrics.setSampledAt(now.minus(i, ChronoUnit.MINUTES));
                metricsStore.addSystemMetrics(instanceId, metrics);
            }

            // Act
            metricsStore.evictOldEntries();

            // Assert - all metrics should still be there
            assertThat(metricsStore.getMetricsCount(instanceId)).isEqualTo(5);
        }

        @Test
        @DisplayName("should handle empty store")
        void evictOldEntries_withEmptyStore_doesNotThrow() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(false);

            // Act & Assert - should not throw
            metricsStore.evictOldEntries();
        }

        @Test
        @DisplayName("should handle metrics with null sampledAt")
        void evictOldEntries_withNullSampledAt_doesNotEvict() {
            // Arrange
            when(schemaConfig.enabled()).thenReturn(false);
            when(schemaConfig.inMemoryMinutes()).thenReturn(30);

            String instanceId = "test-instance";
            SystemMetricsHistory metrics = createTestMetrics();
            metrics.setSampledAt(null);
            metricsStore.addSystemMetrics(instanceId, metrics);

            // Act
            metricsStore.evictOldEntries();

            // Assert - metrics with null timestamp should not be evicted
            assertThat(metricsStore.getMetricsCount(instanceId)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("getRetentionMinutes tests")
    class GetRetentionMinutesTests {

        @Test
        @DisplayName("should return configured retention minutes")
        void getRetentionMinutes_returnsConfiguredValue() {
            // Arrange
            when(schemaConfig.inMemoryMinutes()).thenReturn(45);

            // Act
            int retentionMinutes = metricsStore.getRetentionMinutes();

            // Assert
            assertThat(retentionMinutes).isEqualTo(45);
        }
    }

    @Nested
    @DisplayName("getStorageSummary tests")
    class GetStorageSummaryTests {

        @Test
        @DisplayName("should return empty summary when no metrics")
        void getStorageSummary_withNoMetrics_returnsEmptySummary() {
            // Act
            String summary = metricsStore.getStorageSummary();

            // Assert
            assertThat(summary).contains("empty");
            assertThat(summary).contains("retention: 30 min");
        }

        @Test
        @DisplayName("should include instance counts in summary")
        void getStorageSummary_withMetrics_includesInstanceCounts() {
            // Arrange
            metricsStore.addSystemMetrics("instance1", createTestMetrics());
            metricsStore.addSystemMetrics("instance1", createTestMetrics());
            metricsStore.addSystemMetrics("instance2", createTestMetrics());

            // Act
            String summary = metricsStore.getStorageSummary();

            // Assert
            assertThat(summary).contains("instance1=2");
            assertThat(summary).contains("instance2=1");
        }

        @Test
        @DisplayName("should include retention period in summary")
        void getStorageSummary_includesRetentionPeriod() {
            // Arrange
            when(schemaConfig.inMemoryMinutes()).thenReturn(60);

            // Act
            String summary = metricsStore.getStorageSummary();

            // Assert
            assertThat(summary).contains("retention: 60 min");
        }
    }

    @Nested
    @DisplayName("Thread safety tests")
    class ThreadSafetyTests {

        @Test
        @DisplayName("should handle concurrent additions")
        void concurrentAdditions_shouldNotThrow() throws InterruptedException {
            // Arrange
            String instanceId = "test-instance";
            int threadCount = 10;
            int metricsPerThread = 100;

            // Act
            Thread[] threads = new Thread[threadCount];
            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < metricsPerThread; j++) {
                        metricsStore.addSystemMetrics(instanceId, createTestMetrics());
                    }
                });
                threads[i].start();
            }

            // Wait for all threads to complete
            for (Thread thread : threads) {
                thread.join();
            }

            // Assert
            assertThat(metricsStore.getMetricsCount(instanceId))
                    .isEqualTo(threadCount * metricsPerThread);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("should handle multiple instances")
        void multipleInstances_shouldStoreSeparately() {
            // Arrange & Act
            metricsStore.addSystemMetrics("instance1", createTestMetrics());
            metricsStore.addSystemMetrics("instance1", createTestMetrics());
            metricsStore.addSystemMetrics("instance2", createTestMetrics());
            metricsStore.addSystemMetrics("instance3", createTestMetrics());
            metricsStore.addSystemMetrics("instance3", createTestMetrics());
            metricsStore.addSystemMetrics("instance3", createTestMetrics());

            // Assert
            assertThat(metricsStore.getMetricsCount("instance1")).isEqualTo(2);
            assertThat(metricsStore.getMetricsCount("instance2")).isEqualTo(1);
            assertThat(metricsStore.getMetricsCount("instance3")).isEqualTo(3);
        }

        @Test
        @DisplayName("should handle zero hours time window")
        void getSystemMetricsHistory_withZeroHours_returnsEmpty() {
            // Arrange
            String instanceId = "test-instance";
            metricsStore.addSystemMetrics(instanceId, createTestMetrics());

            // Act
            List<SystemMetricsHistory> history = metricsStore.getSystemMetricsHistory(instanceId, 0);

            // Assert
            assertThat(history).isEmpty();
        }

        @Test
        @DisplayName("should handle very large metrics values")
        void addSystemMetrics_withLargeValues_storesCorrectly() {
            // Arrange
            String instanceId = "test-instance";
            SystemMetricsHistory metrics = createTestMetrics();
            metrics.setTotalConnections(Integer.MAX_VALUE);
            metrics.setTotalDatabaseSizeBytes(Long.MAX_VALUE);

            // Act
            metricsStore.addSystemMetrics(instanceId, metrics);

            // Assert
            List<SystemMetricsHistory> history = metricsStore.getSystemMetricsHistory(instanceId, 1);
            assertThat(history).hasSize(1);
            assertThat(history.get(0).getTotalConnections()).isEqualTo(Integer.MAX_VALUE);
            assertThat(history.get(0).getTotalDatabaseSizeBytes()).isEqualTo(Long.MAX_VALUE);
        }
    }

    // Helper method to create test metrics
    private SystemMetricsHistory createTestMetrics() {
        SystemMetricsHistory metrics = new SystemMetricsHistory();
        metrics.setSampledAt(Instant.now());
        metrics.setTotalConnections(50);
        metrics.setMaxConnections(100);
        metrics.setActiveQueries(10);
        metrics.setIdleConnections(30);
        metrics.setIdleInTransaction(5);
        metrics.setBlockedQueries(2);
        metrics.setLongestQuerySeconds(15.5);
        metrics.setLongestTransactionSeconds(30.0);
        metrics.setCacheHitRatio(98.5);
        metrics.setTotalDatabaseSizeBytes(1024000000L);
        return metrics;
    }
}
