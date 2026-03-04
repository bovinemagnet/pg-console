package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.AggregatedMetrics;
import com.bovinemagnet.pgconsole.model.ComparisonWindow;
import com.bovinemagnet.pgconsole.model.MetricDelta;
import com.bovinemagnet.pgconsole.model.QueryComparison;
import com.bovinemagnet.pgconsole.model.QueryMetricsHistory;
import com.bovinemagnet.pgconsole.model.WindowComparison;
import com.bovinemagnet.pgconsole.repository.HistoryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service for performing comparison window analysis between two time periods.
 * <p>
 * Builds {@link WindowComparison} results by aggregating system metrics and
 * query performance data from persisted history, computing deltas for key
 * metrics, and identifying per-query regressions, improvements, and new/gone
 * queries between the two windows.
 * <p>
 * Also provides preset comparison configurations for common use cases such as
 * "Yesterday vs Today" and "Last Week Same Time", with availability checks
 * based on existing history data.
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see WindowComparison
 * @see ComparisonWindow
 * @see MetricsHistoryBridgeService
 * @see HistoryRepository
 */
@ApplicationScoped
public class WindowComparisonService {

    private static final Logger LOG = Logger.getLogger(WindowComparisonService.class);

    @Inject
    MetricsHistoryBridgeService bridgeService;

    @Inject
    HistoryRepository historyRepository;

    // ========================================
    // Comparison
    // ========================================

    /**
     * Compares two time windows and produces a full comparison result.
     * <p>
     * Performs the following steps:
     * <ol>
     *   <li>Retrieves aggregated system metrics for both windows via the bridge service</li>
     *   <li>Builds a {@link MetricDelta} list comparing: connections, active queries,
     *       blocked queries, idle-in-transaction, cache hit ratio, commit rate,
     *       rollback rate, insert rate, update rate, and delete rate</li>
     *   <li>Retrieves aggregated query metrics for both windows and builds a
     *       {@link QueryComparison} list, matching by query ID and flagging
     *       queries as "existing", "new", or "gone"</li>
     *   <li>Assembles and returns the full {@link WindowComparison}</li>
     * </ol>
     *
     * @param instanceId the PostgreSQL instance identifier
     * @param startA     start of window A (baseline)
     * @param endA       end of window A (baseline)
     * @param startB     start of window B (current)
     * @param endB       end of window B (current)
     * @return the full comparison result
     */
    public WindowComparison compare(String instanceId, Instant startA, Instant endA,
                                     Instant startB, Instant endB) {
        LOG.debugf("Comparing windows: A[%s - %s] vs B[%s - %s] for instance %s",
                startA, endA, startB, endB, instanceId);

        // 1. Get aggregated metrics for both windows
        AggregatedMetrics metricsA = bridgeService.getAggregatedMetrics(instanceId, startA, endA);
        AggregatedMetrics metricsB = bridgeService.getAggregatedMetrics(instanceId, startB, endB);

        // Build comparison windows
        ComparisonWindow windowA = new ComparisonWindow();
        windowA.setLabel("Window A");
        windowA.setWindowStart(startA);
        windowA.setWindowEnd(endA);
        windowA.setAggregatedMetrics(metricsA);
        windowA.setSampleCount(metricsA.getSampleCount());

        ComparisonWindow windowB = new ComparisonWindow();
        windowB.setLabel("Window B");
        windowB.setWindowStart(startB);
        windowB.setWindowEnd(endB);
        windowB.setAggregatedMetrics(metricsB);
        windowB.setSampleCount(metricsB.getSampleCount());

        // 2. Build metric deltas
        List<MetricDelta> deltas = buildMetricDeltas(metricsA, metricsB);

        // 3. Build query comparisons
        List<QueryComparison> queryChanges = buildQueryComparisons(instanceId, startA, endA, startB, endB);

        // 4. Assemble result
        WindowComparison comparison = new WindowComparison();
        comparison.setWindowA(windowA);
        comparison.setWindowB(windowB);
        comparison.setDeltas(deltas);
        comparison.setQueryChanges(queryChanges);

        return comparison;
    }

    // ========================================
    // Presets
    // ========================================

    /**
     * Returns a list of preset comparison configurations for common use cases.
     * <p>
     * Each preset is a map containing:
     * <ul>
     *   <li>{@code name} (String) - display name (e.g., "Yesterday vs Today")</li>
     *   <li>{@code startA} (String) - ISO-8601 start of window A</li>
     *   <li>{@code endA} (String) - ISO-8601 end of window A</li>
     *   <li>{@code startB} (String) - ISO-8601 start of window B</li>
     *   <li>{@code endB} (String) - ISO-8601 end of window B</li>
     *   <li>{@code available} (Boolean) - true if sufficient history data exists</li>
     * </ul>
     * <p>
     * Available presets:
     * <ol>
     *   <li><strong>Yesterday vs Today</strong> - same hour range, yesterday's date vs today</li>
     *   <li><strong>Last Week Same Time</strong> - same day-of-week, 7 days ago vs today</li>
     * </ol>
     *
     * @param instanceId the PostgreSQL instance identifier
     * @return list of preset comparison configurations
     */
    public List<Map<String, Object>> getPresets(String instanceId) {
        List<Map<String, Object>> presets = new ArrayList<>();

        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        LocalTime currentTime = now.toLocalTime();

        // Preset 1: Yesterday vs Today (same hour range)
        ZonedDateTime todayStart = now.toLocalDate().atStartOfDay(ZoneId.systemDefault())
                .with(currentTime.truncatedTo(ChronoUnit.HOURS).minusHours(
                        Math.min(currentTime.getHour(), 8)));
        ZonedDateTime todayEnd = now;
        ZonedDateTime yesterdayStart = todayStart.minusDays(1);
        ZonedDateTime yesterdayEnd = todayEnd.minusDays(1);

        Map<String, Object> yesterdayVsToday = new LinkedHashMap<>();
        yesterdayVsToday.put("name", "Yesterday vs Today");
        yesterdayVsToday.put("startA", yesterdayStart.toInstant().toString());
        yesterdayVsToday.put("endA", yesterdayEnd.toInstant().toString());
        yesterdayVsToday.put("startB", todayStart.toInstant().toString());
        yesterdayVsToday.put("endB", todayEnd.toInstant().toString());
        yesterdayVsToday.put("available", hasDataInRange(instanceId, yesterdayStart.toInstant(), yesterdayEnd.toInstant()));
        presets.add(yesterdayVsToday);

        // Preset 2: Last Week Same Time (same day-of-week, 7 days ago)
        ZonedDateTime lastWeekStart = todayStart.minusDays(7);
        ZonedDateTime lastWeekEnd = todayEnd.minusDays(7);

        Map<String, Object> lastWeekSameTime = new LinkedHashMap<>();
        lastWeekSameTime.put("name", "Last Week Same Time");
        lastWeekSameTime.put("startA", lastWeekStart.toInstant().toString());
        lastWeekSameTime.put("endA", lastWeekEnd.toInstant().toString());
        lastWeekSameTime.put("startB", todayStart.toInstant().toString());
        lastWeekSameTime.put("endB", todayEnd.toInstant().toString());
        lastWeekSameTime.put("available", hasDataInRange(instanceId, lastWeekStart.toInstant(), lastWeekEnd.toInstant()));
        presets.add(lastWeekSameTime);

        return presets;
    }

    // ========================================
    // Private Helpers
    // ========================================

    /**
     * Builds a list of metric deltas comparing two sets of aggregated metrics.
     * <p>
     * Compares: connections, active queries, blocked queries, idle-in-transaction,
     * cache hit ratio, commit rate, rollback rate, insert rate, update rate,
     * and delete rate.
     *
     * @param metricsA the baseline aggregated metrics (window A)
     * @param metricsB the current aggregated metrics (window B)
     * @return list of metric deltas
     */
    private List<MetricDelta> buildMetricDeltas(AggregatedMetrics metricsA, AggregatedMetrics metricsB) {
        List<MetricDelta> deltas = new ArrayList<>();

        deltas.add(new MetricDelta("Connections",
                metricsA.getAvgTotalConnections(), metricsB.getAvgTotalConnections(),
                "connections", false));

        deltas.add(new MetricDelta("Active Queries",
                metricsA.getAvgActiveQueries(), metricsB.getAvgActiveQueries(),
                "queries", false));

        deltas.add(new MetricDelta("Blocked Queries",
                metricsA.getAvgBlockedQueries(), metricsB.getAvgBlockedQueries(),
                "queries", false));

        deltas.add(new MetricDelta("Idle in Transaction",
                metricsA.getAvgIdleInTransaction(), metricsB.getAvgIdleInTransaction(),
                "connections", false));

        deltas.add(new MetricDelta("Cache Hit Ratio",
                metricsA.getAvgCacheHitRatio() != null ? metricsA.getAvgCacheHitRatio() : 0,
                metricsB.getAvgCacheHitRatio() != null ? metricsB.getAvgCacheHitRatio() : 0,
                "%", true));

        deltas.add(new MetricDelta("Commit Rate",
                metricsA.getAvgCommitRate(), metricsB.getAvgCommitRate(),
                "txn/s", true));

        deltas.add(new MetricDelta("Rollback Rate",
                metricsA.getAvgRollbackRate(), metricsB.getAvgRollbackRate(),
                "txn/s", false));

        deltas.add(new MetricDelta("Insert Rate",
                metricsA.getAvgInsertRate(), metricsB.getAvgInsertRate(),
                "rows/s", true));

        deltas.add(new MetricDelta("Update Rate",
                metricsA.getAvgUpdateRate(), metricsB.getAvgUpdateRate(),
                "rows/s", true));

        deltas.add(new MetricDelta("Delete Rate",
                metricsA.getAvgDeleteRate(), metricsB.getAvgDeleteRate(),
                "rows/s", true));

        return deltas;
    }

    /**
     * Builds per-query comparisons between two time windows.
     * <p>
     * Retrieves aggregated query metrics for both windows from the history
     * repository, matches queries by query ID, and categorises each as
     * "existing" (in both), "new" (only in B), or "gone" (only in A).
     *
     * @param instanceId the PostgreSQL instance identifier
     * @param startA     start of window A
     * @param endA       end of window A
     * @param startB     start of window B
     * @param endB       end of window B
     * @return list of query comparisons
     */
    private List<QueryComparison> buildQueryComparisons(String instanceId,
                                                         Instant startA, Instant endA,
                                                         Instant startB, Instant endB) {
        List<QueryComparison> queryChanges = new ArrayList<>();

        try {
            // Convert Instant ranges to hours-ago values for the repository
            Instant now = Instant.now();
            int startAHoursAgo = (int) Math.ceil(Duration.between(startA, now).toHours()) + 1;
            int endAHoursAgo = (int) Math.max(0, Duration.between(endA, now).toHours());
            int startBHoursAgo = (int) Math.ceil(Duration.between(startB, now).toHours()) + 1;
            int endBHoursAgo = (int) Math.max(0, Duration.between(endB, now).toHours());

            List<QueryMetricsHistory> queriesA = historyRepository.getAggregatedQueryMetrics(
                    instanceId, startAHoursAgo, endAHoursAgo);
            List<QueryMetricsHistory> queriesB = historyRepository.getAggregatedQueryMetrics(
                    instanceId, startBHoursAgo, endBHoursAgo);

            // Index window A queries by ID
            Map<String, QueryMetricsHistory> queryMapA = new HashMap<>();
            for (QueryMetricsHistory q : queriesA) {
                queryMapA.put(q.getQueryId(), q);
            }

            // Index window B queries by ID
            Map<String, QueryMetricsHistory> queryMapB = new HashMap<>();
            for (QueryMetricsHistory q : queriesB) {
                queryMapB.put(q.getQueryId(), q);
            }

            // Collect all query IDs
            Set<String> allQueryIds = new HashSet<>();
            allQueryIds.addAll(queryMapA.keySet());
            allQueryIds.addAll(queryMapB.keySet());

            for (String queryId : allQueryIds) {
                QueryMetricsHistory qA = queryMapA.get(queryId);
                QueryMetricsHistory qB = queryMapB.get(queryId);

                QueryComparison qc = new QueryComparison();
                qc.setQueryId(queryId);

                if (qA != null && qB != null) {
                    // Existing query - present in both windows
                    qc.setStatus("existing");
                    qc.setQueryText(qB.getQueryText() != null ? qB.getQueryText() : qA.getQueryText());
                    qc.setMeanTimeA(qA.getMeanTimeMs());
                    qc.setMeanTimeB(qB.getMeanTimeMs());
                    qc.setCallCountA(qA.getTotalCalls());
                    qc.setCallCountB(qB.getTotalCalls());
                } else if (qA == null && qB != null) {
                    // New query - only in window B
                    qc.setStatus("new");
                    qc.setQueryText(qB.getQueryText());
                    qc.setMeanTimeA(0);
                    qc.setMeanTimeB(qB.getMeanTimeMs());
                    qc.setCallCountA(0);
                    qc.setCallCountB(qB.getTotalCalls());
                } else {
                    // Gone query - only in window A
                    qc.setStatus("gone");
                    qc.setQueryText(qA.getQueryText());
                    qc.setMeanTimeA(qA.getMeanTimeMs());
                    qc.setMeanTimeB(0);
                    qc.setCallCountA(qA.getTotalCalls());
                    qc.setCallCountB(0);
                }

                queryChanges.add(qc);
            }

            // Sort by absolute mean time change (largest regressions first)
            queryChanges.sort((a, b) -> Double.compare(
                    Math.abs(b.getMeanTimeDelta()), Math.abs(a.getMeanTimeDelta())));

        } catch (Exception e) {
            LOG.warn("Failed to build query comparisons for instance " + instanceId, e);
        }

        return queryChanges;
    }

    /**
     * Checks whether there is history data available in the specified time range.
     * <p>
     * Used to determine preset availability. A preset is considered available
     * if at least one system metrics sample exists within the range.
     *
     * @param instanceId the PostgreSQL instance identifier
     * @param start      start of the range
     * @param end        end of the range
     * @return true if history data exists in the range
     */
    private boolean hasDataInRange(String instanceId, Instant start, Instant end) {
        try {
            AggregatedMetrics metrics = bridgeService.getAggregatedMetrics(instanceId, start, end);
            return metrics.getSampleCount() > 0;
        } catch (Exception e) {
            LOG.debugf("No data available in range [%s - %s] for instance %s", start, end, instanceId);
            return false;
        }
    }
}
