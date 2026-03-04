package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.config.InstanceConfig;
import com.bovinemagnet.pgconsole.model.AggregatedMetrics;
import com.bovinemagnet.pgconsole.model.DatabaseMetricsHistory;
import com.bovinemagnet.pgconsole.model.LiveChartHistoryPoint;
import com.bovinemagnet.pgconsole.model.MetricsSnapshot;
import com.bovinemagnet.pgconsole.model.OverviewStats;
import com.bovinemagnet.pgconsole.model.SlowQuery;
import com.bovinemagnet.pgconsole.model.SystemMetricsHistory;
import com.bovinemagnet.pgconsole.model.TimeSeriesResult;
import com.bovinemagnet.pgconsole.repository.HistoryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Bridge service that routes metrics history queries to either the in-memory
 * {@link LiveChartHistoryStore} or the persisted {@link HistoryRepository}
 * based on the requested time window.
 * <p>
 * For time windows of 60 minutes or less, the in-memory store is used (5-second
 * resolution). For longer windows (up to 7 days), the persisted history tables
 * are queried (60-second resolution).
 * <p>
 * Also provides snapshot capture for the stopwatch feature and aggregated
 * metrics for the comparison window feature.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class MetricsHistoryBridgeService {

    private static final Logger LOG = Logger.getLogger(MetricsHistoryBridgeService.class);

    /** Threshold in minutes: at or below this, use in-memory store. */
    private static final int IN_MEMORY_THRESHOLD_MINUTES = 60;

    @Inject
    LiveChartHistoryStore liveChartHistoryStore;

    @Inject
    HistoryRepository historyRepository;

    @Inject
    InstanceConfig config;

    @Inject
    PostgresService postgresService;

    /**
     * Returns whether persisted history storage is available.
     *
     * @return true if schema and history are enabled
     */
    public boolean isPersistedStorageAvailable() {
        return config.schema().enabled() && config.history().enabled();
    }

    // ========================================
    // Connection History
    // ========================================

    /**
     * Gets connection history time series, routing to in-memory or persisted storage.
     *
     * @param instanceId the instance identifier
     * @param minutes    the time window in minutes
     * @return time-series result with active, idle, and idle-in-transaction series
     */
    public TimeSeriesResult getConnectionsHistory(String instanceId, int minutes) {
        if (minutes <= IN_MEMORY_THRESHOLD_MINUTES || !isPersistedStorageAvailable()) {
            return getConnectionsFromMemory(instanceId, minutes);
        }
        return getConnectionsFromPersisted(instanceId, minutes);
    }

    private TimeSeriesResult getConnectionsFromMemory(String instanceId, int minutes) {
        List<LiveChartHistoryPoint> points = liveChartHistoryStore.getHistory(instanceId, minutes);
        TimeSeriesResult result = new TimeSeriesResult();
        result.setDataSource("in-memory");
        result.setResolutionSeconds(5);

        List<Long> timestamps = new ArrayList<>(points.size());
        List<Double> active = new ArrayList<>(points.size());
        List<Double> idle = new ArrayList<>(points.size());
        List<Double> idleInTxn = new ArrayList<>(points.size());

        for (LiveChartHistoryPoint p : points) {
            timestamps.add(p.getSampledAt().toEpochMilli());
            active.add(p.getActive());
            idle.add(p.getIdle());
            idleInTxn.add(p.getIdleInTransaction());
        }

        result.setTimestamps(timestamps);
        result.addSeries("active", active);
        result.addSeries("idle", idle);
        result.addSeries("idleInTransaction", idleInTxn);
        result.setDataPoints(points.size());
        return result;
    }

    private TimeSeriesResult getConnectionsFromPersisted(String instanceId, int minutes) {
        int hours = Math.max(1, (minutes + 59) / 60);
        List<SystemMetricsHistory> history = historyRepository.getSystemMetricsHistory(instanceId, hours);

        TimeSeriesResult result = new TimeSeriesResult();
        result.setDataSource("persisted");
        result.setResolutionSeconds(config.history().intervalSeconds());

        List<Long> timestamps = new ArrayList<>(history.size());
        List<Double> active = new ArrayList<>(history.size());
        List<Double> idle = new ArrayList<>(history.size());
        List<Double> idleInTxn = new ArrayList<>(history.size());

        Instant cutoff = Instant.now().minusSeconds((long) minutes * 60);
        for (SystemMetricsHistory m : history) {
            if (m.getSampledAt().isBefore(cutoff)) continue;
            timestamps.add(m.getSampledAt().toEpochMilli());
            active.add((double) m.getActiveQueries());
            idle.add((double) m.getIdleConnections());
            idleInTxn.add((double) m.getIdleInTransaction());
        }

        result.setTimestamps(timestamps);
        result.addSeries("active", active);
        result.addSeries("idle", idle);
        result.addSeries("idleInTransaction", idleInTxn);
        result.setDataPoints(timestamps.size());
        return result;
    }

    // ========================================
    // Transaction History
    // ========================================

    /**
     * Gets transaction rate history time series.
     *
     * @param instanceId the instance identifier
     * @param minutes    the time window in minutes
     * @return time-series result with commitsRate and rollbacksRate series
     */
    public TimeSeriesResult getTransactionsHistory(String instanceId, int minutes) {
        if (minutes <= IN_MEMORY_THRESHOLD_MINUTES || !isPersistedStorageAvailable()) {
            return getTransactionsFromMemory(instanceId, minutes);
        }
        return getTransactionsFromPersisted(instanceId, minutes);
    }

    private TimeSeriesResult getTransactionsFromMemory(String instanceId, int minutes) {
        List<LiveChartHistoryPoint> points = liveChartHistoryStore.getHistory(instanceId, minutes);
        TimeSeriesResult result = new TimeSeriesResult();
        result.setDataSource("in-memory");
        result.setResolutionSeconds(5);

        List<Long> timestamps = new ArrayList<>();
        List<Double> commitsRate = new ArrayList<>();
        List<Double> rollbacksRate = new ArrayList<>();

        for (int i = 1; i < points.size(); i++) {
            LiveChartHistoryPoint prev = points.get(i - 1);
            LiveChartHistoryPoint curr = points.get(i);
            double seconds = Duration.between(prev.getSampledAt(), curr.getSampledAt()).toMillis() / 1000.0;
            if (seconds <= 0) continue;

            double cRate = Math.max(0, (curr.getCommits() - prev.getCommits()) / seconds);
            double rRate = Math.max(0, (curr.getRollbacks() - prev.getRollbacks()) / seconds);

            timestamps.add(curr.getSampledAt().toEpochMilli());
            commitsRate.add(Math.round(cRate * 10.0) / 10.0);
            rollbacksRate.add(Math.round(rRate * 10.0) / 10.0);
        }

        result.setTimestamps(timestamps);
        result.addSeries("commitsRate", commitsRate);
        result.addSeries("rollbacksRate", rollbacksRate);
        result.setDataPoints(timestamps.size());
        return result;
    }

    private TimeSeriesResult getTransactionsFromPersisted(String instanceId, int minutes) {
        int hours = Math.max(1, (minutes + 59) / 60);
        List<DatabaseMetricsHistory> history = historyRepository.getAggregatedDatabaseMetricsHistory(instanceId, hours);

        TimeSeriesResult result = new TimeSeriesResult();
        result.setDataSource("persisted");
        result.setResolutionSeconds(config.history().intervalSeconds());

        List<Long> timestamps = new ArrayList<>();
        List<Double> commitsRate = new ArrayList<>();
        List<Double> rollbacksRate = new ArrayList<>();

        Instant cutoff = Instant.now().minusSeconds((long) minutes * 60);
        DatabaseMetricsHistory prev = null;

        for (DatabaseMetricsHistory curr : history) {
            if (curr.getSampledAt().isBefore(cutoff)) {
                prev = curr;
                continue;
            }
            if (prev != null) {
                double seconds = Duration.between(prev.getSampledAt(), curr.getSampledAt()).toMillis() / 1000.0;
                if (seconds > 0) {
                    double cRate = Math.max(0, (curr.getXactCommit() - prev.getXactCommit()) / seconds);
                    double rRate = Math.max(0, (curr.getXactRollback() - prev.getXactRollback()) / seconds);
                    timestamps.add(curr.getSampledAt().toEpochMilli());
                    commitsRate.add(Math.round(cRate * 10.0) / 10.0);
                    rollbacksRate.add(Math.round(rRate * 10.0) / 10.0);
                }
            }
            prev = curr;
        }

        result.setTimestamps(timestamps);
        result.addSeries("commitsRate", commitsRate);
        result.addSeries("rollbacksRate", rollbacksRate);
        result.setDataPoints(timestamps.size());
        return result;
    }

    // ========================================
    // Tuple Operations History
    // ========================================

    /**
     * Gets tuple operations rate history time series.
     *
     * @param instanceId the instance identifier
     * @param minutes    the time window in minutes
     * @return time-series result with insertsRate, updatesRate, deletesRate series
     */
    public TimeSeriesResult getTuplesHistory(String instanceId, int minutes) {
        if (minutes <= IN_MEMORY_THRESHOLD_MINUTES || !isPersistedStorageAvailable()) {
            return getTuplesFromMemory(instanceId, minutes);
        }
        return getTuplesFromPersisted(instanceId, minutes);
    }

    private TimeSeriesResult getTuplesFromMemory(String instanceId, int minutes) {
        List<LiveChartHistoryPoint> points = liveChartHistoryStore.getHistory(instanceId, minutes);
        TimeSeriesResult result = new TimeSeriesResult();
        result.setDataSource("in-memory");
        result.setResolutionSeconds(5);

        List<Long> timestamps = new ArrayList<>();
        List<Double> insertsRate = new ArrayList<>();
        List<Double> updatesRate = new ArrayList<>();
        List<Double> deletesRate = new ArrayList<>();

        for (int i = 1; i < points.size(); i++) {
            LiveChartHistoryPoint prev = points.get(i - 1);
            LiveChartHistoryPoint curr = points.get(i);
            double seconds = Duration.between(prev.getSampledAt(), curr.getSampledAt()).toMillis() / 1000.0;
            if (seconds <= 0) continue;

            double iRate = Math.max(0, (curr.getInserted() - prev.getInserted()) / seconds);
            double uRate = Math.max(0, (curr.getUpdated() - prev.getUpdated()) / seconds);
            double dRate = Math.max(0, (curr.getDeleted() - prev.getDeleted()) / seconds);

            timestamps.add(curr.getSampledAt().toEpochMilli());
            insertsRate.add(Math.round(iRate * 10.0) / 10.0);
            updatesRate.add(Math.round(uRate * 10.0) / 10.0);
            deletesRate.add(Math.round(dRate * 10.0) / 10.0);
        }

        result.setTimestamps(timestamps);
        result.addSeries("insertsRate", insertsRate);
        result.addSeries("updatesRate", updatesRate);
        result.addSeries("deletesRate", deletesRate);
        result.setDataPoints(timestamps.size());
        return result;
    }

    private TimeSeriesResult getTuplesFromPersisted(String instanceId, int minutes) {
        int hours = Math.max(1, (minutes + 59) / 60);
        List<DatabaseMetricsHistory> history = historyRepository.getAggregatedDatabaseMetricsHistory(instanceId, hours);

        TimeSeriesResult result = new TimeSeriesResult();
        result.setDataSource("persisted");
        result.setResolutionSeconds(config.history().intervalSeconds());

        List<Long> timestamps = new ArrayList<>();
        List<Double> insertsRate = new ArrayList<>();
        List<Double> updatesRate = new ArrayList<>();
        List<Double> deletesRate = new ArrayList<>();

        Instant cutoff = Instant.now().minusSeconds((long) minutes * 60);
        DatabaseMetricsHistory prev = null;

        for (DatabaseMetricsHistory curr : history) {
            if (curr.getSampledAt().isBefore(cutoff)) {
                prev = curr;
                continue;
            }
            if (prev != null) {
                double seconds = Duration.between(prev.getSampledAt(), curr.getSampledAt()).toMillis() / 1000.0;
                if (seconds > 0) {
                    double iRate = Math.max(0, safeSubtract(curr.getTupInserted(), prev.getTupInserted()) / seconds);
                    double uRate = Math.max(0, safeSubtract(curr.getTupUpdated(), prev.getTupUpdated()) / seconds);
                    double dRate = Math.max(0, safeSubtract(curr.getTupDeleted(), prev.getTupDeleted()) / seconds);
                    timestamps.add(curr.getSampledAt().toEpochMilli());
                    insertsRate.add(Math.round(iRate * 10.0) / 10.0);
                    updatesRate.add(Math.round(uRate * 10.0) / 10.0);
                    deletesRate.add(Math.round(dRate * 10.0) / 10.0);
                }
            }
            prev = curr;
        }

        result.setTimestamps(timestamps);
        result.addSeries("insertsRate", insertsRate);
        result.addSeries("updatesRate", updatesRate);
        result.addSeries("deletesRate", deletesRate);
        result.setDataPoints(timestamps.size());
        return result;
    }

    // ========================================
    // Cache History
    // ========================================

    /**
     * Gets cache hit ratio history time series.
     *
     * @param instanceId the instance identifier
     * @param minutes    the time window in minutes
     * @return time-series result with bufferHitRatio and indexHitRatio series
     */
    public TimeSeriesResult getCacheHistory(String instanceId, int minutes) {
        if (minutes <= IN_MEMORY_THRESHOLD_MINUTES || !isPersistedStorageAvailable()) {
            return getCacheFromMemory(instanceId, minutes);
        }
        return getCacheFromPersisted(instanceId, minutes);
    }

    private TimeSeriesResult getCacheFromMemory(String instanceId, int minutes) {
        List<LiveChartHistoryPoint> points = liveChartHistoryStore.getHistory(instanceId, minutes);
        TimeSeriesResult result = new TimeSeriesResult();
        result.setDataSource("in-memory");
        result.setResolutionSeconds(5);

        List<Long> timestamps = new ArrayList<>(points.size());
        List<Double> bufferHit = new ArrayList<>(points.size());
        List<Double> indexHit = new ArrayList<>(points.size());

        for (LiveChartHistoryPoint p : points) {
            timestamps.add(p.getSampledAt().toEpochMilli());
            bufferHit.add(Math.round(p.getBufferCacheHitRatio() * 100.0) / 100.0);
            indexHit.add(Math.round(p.getIndexCacheHitRatio() * 100.0) / 100.0);
        }

        result.setTimestamps(timestamps);
        result.addSeries("bufferHitRatio", bufferHit);
        result.addSeries("indexHitRatio", indexHit);
        result.setDataPoints(points.size());
        return result;
    }

    private TimeSeriesResult getCacheFromPersisted(String instanceId, int minutes) {
        int hours = Math.max(1, (minutes + 59) / 60);
        List<SystemMetricsHistory> history = historyRepository.getSystemMetricsHistory(instanceId, hours);

        TimeSeriesResult result = new TimeSeriesResult();
        result.setDataSource("persisted");
        result.setResolutionSeconds(config.history().intervalSeconds());

        List<Long> timestamps = new ArrayList<>(history.size());
        List<Double> bufferHit = new ArrayList<>(history.size());
        List<Double> indexHit = new ArrayList<>(history.size());

        Instant cutoff = Instant.now().minusSeconds((long) minutes * 60);
        for (SystemMetricsHistory m : history) {
            if (m.getSampledAt().isBefore(cutoff)) continue;
            timestamps.add(m.getSampledAt().toEpochMilli());
            bufferHit.add(m.getCacheHitRatio() != null ? Math.round(m.getCacheHitRatio() * 100.0) / 100.0 : 0.0);
            // System metrics don't have separate index cache; use same value
            indexHit.add(m.getCacheHitRatio() != null ? Math.round(m.getCacheHitRatio() * 100.0) / 100.0 : 0.0);
        }

        result.setTimestamps(timestamps);
        result.addSeries("bufferHitRatio", bufferHit);
        result.addSeries("indexHitRatio", indexHit);
        result.setDataPoints(timestamps.size());
        return result;
    }

    // ========================================
    // Snapshot Capture (for Stopwatch)
    // ========================================

    /**
     * Captures a point-in-time metrics snapshot.
     * <p>
     * Used by the stopwatch feature to record the state of the database
     * at session start and stop times.
     *
     * @param instanceName the instance name
     * @return current metrics snapshot
     */
    public MetricsSnapshot captureSnapshot(String instanceName) {
        MetricsSnapshot snapshot = new MetricsSnapshot();
        snapshot.setCapturedAt(Instant.now());

        try {
            OverviewStats stats = postgresService.getOverviewStats(instanceName);
            snapshot.setTotalConnections(stats.getConnectionsUsed());
            snapshot.setActiveQueries(stats.getActiveQueries());
            snapshot.setBlockedQueries(stats.getBlockedQueries());
            snapshot.setCacheHitRatio(stats.getCacheHitRatio());
        } catch (Exception e) {
            LOG.warn("Failed to capture overview stats for snapshot", e);
        }

        try {
            List<SlowQuery> slowQueries = postgresService.getSlowQueries(instanceName, "total_time", "desc");
            if (slowQueries.size() > 20) {
                slowQueries = slowQueries.subList(0, 20);
            }
            snapshot.setTopQueries(slowQueries);
        } catch (Exception e) {
            LOG.warn("Failed to capture top queries for snapshot", e);
        }

        return snapshot;
    }

    // ========================================
    // Aggregated Metrics (for Comparison)
    // ========================================

    /**
     * Gets aggregated system metrics over a time window.
     * <p>
     * Computes averages of key metrics from persisted history data.
     * Used by the comparison window feature.
     *
     * @param instanceId the instance identifier
     * @param start      start of the window
     * @param end        end of the window
     * @return aggregated metrics for the window
     */
    public AggregatedMetrics getAggregatedMetrics(String instanceId, Instant start, Instant end) {
        AggregatedMetrics agg = new AggregatedMetrics();
        agg.setWindowStart(start);
        agg.setWindowEnd(end);

        int hours = (int) Math.ceil(Duration.between(start, Instant.now()).toHours()) + 1;
        List<SystemMetricsHistory> systemHistory = historyRepository.getSystemMetricsHistory(instanceId, hours);

        // Filter to our window
        List<SystemMetricsHistory> windowData = systemHistory.stream()
                .filter(m -> !m.getSampledAt().isBefore(start) && !m.getSampledAt().isAfter(end))
                .toList();

        if (windowData.isEmpty()) {
            agg.setSampleCount(0);
            return agg;
        }

        agg.setSampleCount(windowData.size());
        agg.setAvgTotalConnections(windowData.stream().mapToDouble(SystemMetricsHistory::getTotalConnections).average().orElse(0));
        agg.setAvgActiveQueries(windowData.stream().mapToDouble(SystemMetricsHistory::getActiveQueries).average().orElse(0));
        agg.setAvgBlockedQueries(windowData.stream().mapToDouble(SystemMetricsHistory::getBlockedQueries).average().orElse(0));
        agg.setAvgIdleInTransaction(windowData.stream().mapToDouble(SystemMetricsHistory::getIdleInTransaction).average().orElse(0));

        double cacheSum = 0;
        int cacheCount = 0;
        for (SystemMetricsHistory m : windowData) {
            if (m.getCacheHitRatio() != null) {
                cacheSum += m.getCacheHitRatio();
                cacheCount++;
            }
        }
        agg.setAvgCacheHitRatio(cacheCount > 0 ? cacheSum / cacheCount : null);

        Double avgLongest = windowData.stream()
                .filter(m -> m.getLongestQuerySeconds() != null)
                .mapToDouble(SystemMetricsHistory::getLongestQuerySeconds)
                .average()
                .orElse(0);
        agg.setAvgLongestQuerySeconds(avgLongest);

        Long avgSize = windowData.stream()
                .filter(m -> m.getTotalDatabaseSizeBytes() != null)
                .mapToLong(SystemMetricsHistory::getTotalDatabaseSizeBytes)
                .reduce(0L, Long::sum);
        long sizeCount = windowData.stream().filter(m -> m.getTotalDatabaseSizeBytes() != null).count();
        agg.setAvgDatabaseSizeBytes(sizeCount > 0 ? avgSize / sizeCount : null);

        return agg;
    }

    /**
     * Returns the total data point count, combining in-memory and persisted counts.
     *
     * @param instanceId the instance identifier
     * @param minutes    the time window in minutes
     * @return estimated data point count
     */
    public int getDataPointCount(String instanceId, int minutes) {
        if (minutes <= IN_MEMORY_THRESHOLD_MINUTES || !isPersistedStorageAvailable()) {
            return liveChartHistoryStore.getPointCount(instanceId);
        }
        int hours = Math.max(1, (minutes + 59) / 60);
        List<SystemMetricsHistory> history = historyRepository.getSystemMetricsHistory(instanceId, hours);
        Instant cutoff = Instant.now().minusSeconds((long) minutes * 60);
        return (int) history.stream().filter(m -> !m.getSampledAt().isBefore(cutoff)).count();
    }

    /**
     * Safely subtracts two nullable Long values, returning 0 if either is null.
     *
     * @param current  current value
     * @param previous previous value
     * @return delta, or 0 if either value is null
     */
    private double safeSubtract(Long current, Long previous) {
        if (current == null || previous == null) return 0;
        return current - previous;
    }
}
