package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.LiveChartData;
import com.bovinemagnet.pgconsole.model.LiveChartHistoryPoint;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * In-memory store for live chart history data points.
 * <p>
 * Samples all 4 live chart metrics (connections, transactions, tuples, cache)
 * every 5 seconds and stores them in memory for up to 24 hours. This allows
 * the Metrics History dashboard to display historical trends.
 * <p>
 * Runs unconditionally (not gated by schema.enabled) since it stores data
 * in memory only. The feature toggle controls page visibility, not sampling.
 * <p>
 * Thread-safe for concurrent sampling and reading.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class LiveChartHistoryStore {

    private static final Logger LOG = Logger.getLogger("pgconsole.LiveChartHistoryStore");

    /** Maximum retention period: 24 hours. */
    private static final int MAX_RETENTION_MINUTES = 24 * 60;

    /**
     * History storage keyed by instance ID.
     */
    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<LiveChartHistoryPoint>> historyMap =
            new ConcurrentHashMap<>();

    @Inject
    PostgresService postgresService;

    @Inject
    DataSourceManager dataSourceManager;

    /**
     * Scheduled sampler that collects all 4 chart metrics every 5 seconds.
     * <p>
     * Iterates over all configured instances and samples connections,
     * transactions, tuples, and cache hit ratios into a single
     * {@link LiveChartHistoryPoint}.
     */
    @Scheduled(every = "5s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void sampleAllInstances() {
        List<String> instances = dataSourceManager.getAvailableInstances();
        for (String instanceId : instances) {
            try {
                sampleInstance(instanceId);
            } catch (Exception e) {
                LOG.warnf("Failed to sample live chart history for instance '%s': %s",
                        instanceId, e.getMessage());
            }
        }
    }

    /**
     * Samples all metrics for a single instance and stores the data point.
     *
     * @param instanceId the instance identifier
     */
    void sampleInstance(String instanceId) {
        Instant now = Instant.now();

        // Connections (gauges)
        LiveChartData connChart = postgresService.getConnectionsChartData(instanceId);
        double active = getSeriesValue(connChart, "Active");
        double idle = getSeriesValue(connChart, "Idle");
        double idleInTxn = getSeriesValue(connChart, "Idle in Transaction");

        // Transactions (cumulative counters)
        LiveChartData txnChart = postgresService.getTransactionsChartData(instanceId);
        double commits = getSeriesValue(txnChart, "Commits");
        double rollbacks = getSeriesValue(txnChart, "Rollbacks");

        // Tuples (cumulative counters)
        LiveChartData tupleChart = postgresService.getTuplesChartData(instanceId);
        double inserted = getSeriesValue(tupleChart, "Inserted");
        double updated = getSeriesValue(tupleChart, "Updated");
        double deleted = getSeriesValue(tupleChart, "Deleted");

        // Cache (percentages)
        LiveChartData cacheChart = postgresService.getCacheChartData(instanceId);
        double bufferHit = getSeriesValue(cacheChart, "Buffer Cache");
        double indexHit = getSeriesValue(cacheChart, "Index Cache");

        LiveChartHistoryPoint point = new LiveChartHistoryPoint(
                now, active, idle, idleInTxn,
                commits, rollbacks,
                inserted, updated, deleted,
                bufferHit, indexHit);

        addPoint(instanceId, point);
    }

    /**
     * Extracts the latest value from a chart series by name.
     *
     * @param chart      the chart data
     * @param seriesName the series name
     * @return the latest value, or 0 if not found
     */
    private double getSeriesValue(LiveChartData chart, String seriesName) {
        if (chart == null) return 0;
        var series = chart.getSeriesByName(seriesName);
        return series != null ? series.getLatestValue() : 0;
    }

    /**
     * Adds a history point for the given instance.
     *
     * @param instanceId the instance identifier
     * @param point      the data point to store
     */
    public void addPoint(String instanceId, LiveChartHistoryPoint point) {
        if (instanceId == null || point == null) {
            return;
        }
        ConcurrentLinkedDeque<LiveChartHistoryPoint> deque = historyMap.computeIfAbsent(
                instanceId, k -> new ConcurrentLinkedDeque<>());
        deque.addLast(point);
        LOG.debugf("Added live chart history point for instance '%s', store size: %d",
                instanceId, deque.size());
    }

    /**
     * Retrieves history points for a given instance within a time window.
     *
     * @param instanceId the instance identifier
     * @param minutes    number of minutes to look back
     * @return list of history points ordered by time (oldest first)
     */
    public List<LiveChartHistoryPoint> getHistory(String instanceId, int minutes) {
        if (instanceId == null) {
            return new ArrayList<>();
        }

        ConcurrentLinkedDeque<LiveChartHistoryPoint> deque = historyMap.get(instanceId);
        if (deque == null || deque.isEmpty()) {
            return new ArrayList<>();
        }

        Instant cutoff = Instant.now().minus(minutes, ChronoUnit.MINUTES);
        List<LiveChartHistoryPoint> result = new ArrayList<>();

        for (LiveChartHistoryPoint p : deque) {
            if (p.getSampledAt() != null && p.getSampledAt().isAfter(cutoff)) {
                result.add(p);
            }
        }

        LOG.debugf("Retrieved %d live chart history points for instance '%s' (last %d minutes)",
                Integer.valueOf(result.size()), instanceId, Integer.valueOf(minutes));
        return result;
    }

    /**
     * Gets the current number of stored points for an instance.
     *
     * @param instanceId the instance identifier
     * @return number of stored points
     */
    public int getPointCount(String instanceId) {
        ConcurrentLinkedDeque<LiveChartHistoryPoint> deque = historyMap.get(instanceId);
        return deque != null ? deque.size() : 0;
    }

    /**
     * Scheduled eviction job that removes entries older than 24 hours.
     * <p>
     * Runs every 60 seconds and removes old entries from the front of each deque.
     */
    @Scheduled(every = "60s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP,
               identity = "live-chart-history-eviction")
    void evictOldEntries() {
        Instant cutoff = Instant.now().minus(MAX_RETENTION_MINUTES, ChronoUnit.MINUTES);
        int totalEvicted = 0;

        for (var entry : historyMap.entrySet()) {
            ConcurrentLinkedDeque<LiveChartHistoryPoint> deque = entry.getValue();
            int evicted = 0;
            while (!deque.isEmpty()) {
                LiveChartHistoryPoint oldest = deque.peekFirst();
                if (oldest != null && oldest.getSampledAt() != null
                        && oldest.getSampledAt().isBefore(cutoff)) {
                    deque.pollFirst();
                    evicted++;
                } else {
                    break;
                }
            }
            if (evicted > 0) {
                LOG.debugf("Evicted %d old live chart history entries from '%s'",
                        evicted, entry.getKey());
            }
            totalEvicted += evicted;
        }

        if (totalEvicted > 0) {
            LOG.debugf("Live chart history eviction complete: removed %d entries older than 24 hours",
                    totalEvicted);
        }
    }

    /**
     * Clears all stored history (useful for testing).
     */
    public void clear() {
        historyMap.clear();
        LOG.debug("Cleared all live chart history");
    }

    /**
     * Clears stored history for a specific instance.
     *
     * @param instanceId the instance identifier
     */
    public void clear(String instanceId) {
        historyMap.remove(instanceId);
        LOG.debugf("Cleared live chart history for instance '%s'", instanceId);
    }
}
