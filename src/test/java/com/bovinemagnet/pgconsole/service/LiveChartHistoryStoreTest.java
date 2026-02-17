package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.LiveChartHistoryPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LiveChartHistoryStore}.
 * <p>
 * Tests the in-memory storage functionality for live chart history points,
 * including adding points, retrieving with time-window filtering, eviction,
 * null safety, and clear operations.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
class LiveChartHistoryStoreTest {

    private LiveChartHistoryStore store;

    @BeforeEach
    void setUp() {
        store = new LiveChartHistoryStore();
    }

    /**
     * Creates a test history point with the given timestamp.
     */
    private LiveChartHistoryPoint createPoint(Instant sampledAt) {
        return new LiveChartHistoryPoint(
                sampledAt,
                5.0, 10.0, 2.0,     // connections: active, idle, idleInTxn
                1000.0, 5.0,         // transactions: commits, rollbacks
                500.0, 300.0, 100.0, // tuples: inserted, updated, deleted
                99.5, 98.0           // cache: buffer, index
        );
    }

    /**
     * Creates a test history point with specific metric values.
     */
    private LiveChartHistoryPoint createPoint(Instant sampledAt,
                                               double active, double commits,
                                               double bufferHit) {
        return new LiveChartHistoryPoint(
                sampledAt,
                active, 10.0, 2.0,
                commits, 5.0,
                500.0, 300.0, 100.0,
                bufferHit, 98.0
        );
    }

    @Nested
    @DisplayName("addPoint tests")
    class AddPointTests {

        @Test
        @DisplayName("should add point successfully with valid data")
        void addPoint_withValidData_storesPoint() {
            LiveChartHistoryPoint point = createPoint(Instant.now());
            store.addPoint("test-instance", point);

            assertThat(store.getPointCount("test-instance")).isEqualTo(1);
        }

        @Test
        @DisplayName("should handle multiple points for same instance")
        void addPoint_multiplePoints_allStored() {
            Instant now = Instant.now();
            store.addPoint("test-instance", createPoint(now.minusSeconds(10)));
            store.addPoint("test-instance", createPoint(now.minusSeconds(5)));
            store.addPoint("test-instance", createPoint(now));

            assertThat(store.getPointCount("test-instance")).isEqualTo(3);
        }

        @Test
        @DisplayName("should handle points for multiple instances")
        void addPoint_multipleInstances_separatelyStored() {
            store.addPoint("instance-1", createPoint(Instant.now()));
            store.addPoint("instance-2", createPoint(Instant.now()));

            assertThat(store.getPointCount("instance-1")).isEqualTo(1);
            assertThat(store.getPointCount("instance-2")).isEqualTo(1);
        }

        @Test
        @DisplayName("should ignore null instanceId")
        void addPoint_nullInstanceId_ignored() {
            store.addPoint(null, createPoint(Instant.now()));
            // No exception and no state change
            assertThat(store.getPointCount("anything")).isEqualTo(0);
        }

        @Test
        @DisplayName("should ignore null point")
        void addPoint_nullPoint_ignored() {
            store.addPoint("test-instance", null);
            assertThat(store.getPointCount("test-instance")).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("getHistory tests")
    class GetHistoryTests {

        @Test
        @DisplayName("should return all points within time window")
        void getHistory_withinWindow_returnsAll() {
            Instant now = Instant.now();
            store.addPoint("test", createPoint(now.minusSeconds(60)));
            store.addPoint("test", createPoint(now.minusSeconds(30)));
            store.addPoint("test", createPoint(now));

            List<LiveChartHistoryPoint> history = store.getHistory("test", 5);
            assertThat(history).hasSize(3);
        }

        @Test
        @DisplayName("should filter out points outside time window")
        void getHistory_outsideWindow_filtered() {
            Instant now = Instant.now();
            store.addPoint("test", createPoint(now.minus(10, ChronoUnit.MINUTES)));
            store.addPoint("test", createPoint(now.minus(3, ChronoUnit.MINUTES)));
            store.addPoint("test", createPoint(now));

            List<LiveChartHistoryPoint> history = store.getHistory("test", 5);
            assertThat(history).hasSize(2);
        }

        @Test
        @DisplayName("should return empty list for unknown instance")
        void getHistory_unknownInstance_returnsEmpty() {
            List<LiveChartHistoryPoint> history = store.getHistory("unknown", 5);
            assertThat(history).isEmpty();
        }

        @Test
        @DisplayName("should return empty list for null instanceId")
        void getHistory_nullInstanceId_returnsEmpty() {
            List<LiveChartHistoryPoint> history = store.getHistory(null, 5);
            assertThat(history).isEmpty();
        }

        @Test
        @DisplayName("should return points ordered oldest first")
        void getHistory_ordering_oldestFirst() {
            Instant now = Instant.now();
            Instant t1 = now.minusSeconds(30);
            Instant t2 = now.minusSeconds(15);
            Instant t3 = now;

            store.addPoint("test", createPoint(t1));
            store.addPoint("test", createPoint(t2));
            store.addPoint("test", createPoint(t3));

            List<LiveChartHistoryPoint> history = store.getHistory("test", 5);
            assertThat(history).hasSize(3);
            assertThat(history.get(0).getSampledAt()).isEqualTo(t1);
            assertThat(history.get(1).getSampledAt()).isEqualTo(t2);
            assertThat(history.get(2).getSampledAt()).isEqualTo(t3);
        }

        @Test
        @DisplayName("should preserve metric values in retrieved points")
        void getHistory_metricValues_preserved() {
            Instant now = Instant.now();
            store.addPoint("test", createPoint(now, 7.0, 2000.0, 95.5));

            List<LiveChartHistoryPoint> history = store.getHistory("test", 5);
            assertThat(history).hasSize(1);

            LiveChartHistoryPoint point = history.get(0);
            assertThat(point.getActive()).isEqualTo(7.0);
            assertThat(point.getCommits()).isEqualTo(2000.0);
            assertThat(point.getBufferCacheHitRatio()).isEqualTo(95.5);
        }
    }

    @Nested
    @DisplayName("getPointCount tests")
    class GetPointCountTests {

        @Test
        @DisplayName("should return 0 for unknown instance")
        void getPointCount_unknownInstance_returnsZero() {
            assertThat(store.getPointCount("unknown")).isEqualTo(0);
        }

        @Test
        @DisplayName("should return correct count after adding points")
        void getPointCount_afterAdding_returnsCorrectCount() {
            store.addPoint("test", createPoint(Instant.now()));
            store.addPoint("test", createPoint(Instant.now()));
            assertThat(store.getPointCount("test")).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("evictOldEntries tests")
    class EvictOldEntriesTests {

        @Test
        @DisplayName("should remove entries older than 24 hours")
        void evictOldEntries_oldEntries_removed() {
            Instant now = Instant.now();
            store.addPoint("test", createPoint(now.minus(25, ChronoUnit.HOURS)));
            store.addPoint("test", createPoint(now.minus(23, ChronoUnit.HOURS)));
            store.addPoint("test", createPoint(now));

            store.evictOldEntries();

            assertThat(store.getPointCount("test")).isEqualTo(2);
        }

        @Test
        @DisplayName("should keep entries within 24 hours")
        void evictOldEntries_recentEntries_kept() {
            Instant now = Instant.now();
            store.addPoint("test", createPoint(now.minusSeconds(60)));
            store.addPoint("test", createPoint(now));

            store.evictOldEntries();

            assertThat(store.getPointCount("test")).isEqualTo(2);
        }

        @Test
        @DisplayName("should handle empty store")
        void evictOldEntries_emptyStore_noError() {
            store.evictOldEntries();
            // No exception expected
        }

        @Test
        @DisplayName("should evict across multiple instances")
        void evictOldEntries_multipleInstances_allEvicted() {
            Instant old = Instant.now().minus(25, ChronoUnit.HOURS);
            Instant recent = Instant.now();

            store.addPoint("inst-1", createPoint(old));
            store.addPoint("inst-1", createPoint(recent));
            store.addPoint("inst-2", createPoint(old));
            store.addPoint("inst-2", createPoint(recent));

            store.evictOldEntries();

            assertThat(store.getPointCount("inst-1")).isEqualTo(1);
            assertThat(store.getPointCount("inst-2")).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("clear tests")
    class ClearTests {

        @Test
        @DisplayName("should clear all instances")
        void clear_allInstances_emptied() {
            store.addPoint("inst-1", createPoint(Instant.now()));
            store.addPoint("inst-2", createPoint(Instant.now()));

            store.clear();

            assertThat(store.getPointCount("inst-1")).isEqualTo(0);
            assertThat(store.getPointCount("inst-2")).isEqualTo(0);
        }

        @Test
        @DisplayName("should clear specific instance only")
        void clear_specificInstance_onlyThatCleared() {
            store.addPoint("inst-1", createPoint(Instant.now()));
            store.addPoint("inst-2", createPoint(Instant.now()));

            store.clear("inst-1");

            assertThat(store.getPointCount("inst-1")).isEqualTo(0);
            assertThat(store.getPointCount("inst-2")).isEqualTo(1);
        }

        @Test
        @DisplayName("should handle clearing unknown instance")
        void clear_unknownInstance_noError() {
            store.clear("unknown");
            // No exception expected
        }
    }

    @Nested
    @DisplayName("Thread safety tests")
    class ThreadSafetyTests {

        @Test
        @DisplayName("should handle concurrent adds without error")
        void concurrentAdds_noErrors() throws InterruptedException {
            int threadCount = 10;
            int pointsPerThread = 100;
            Thread[] threads = new Thread[threadCount];

            for (int i = 0; i < threadCount; i++) {
                final int threadNum = i;
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < pointsPerThread; j++) {
                        store.addPoint("test",
                                createPoint(Instant.now().minusSeconds(j)));
                    }
                });
            }

            for (Thread t : threads) t.start();
            for (Thread t : threads) t.join();

            assertThat(store.getPointCount("test"))
                    .isEqualTo(threadCount * pointsPerThread);
        }
    }
}
