package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.OverviewStats;
import com.bovinemagnet.pgconsole.model.SlowQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for comparing metrics across PostgreSQL instances.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class ComparisonService {

    private static final Logger LOG = Logger.getLogger(ComparisonService.class);

    @Inject
    DataSourceManager dataSourceManager;

    @Inject
    PostgresService postgresService;

    /**
     * Compares overview statistics across all configured PostgreSQL instances.
     * <p>
     * Collects overview statistics from each instance and returns a unified comparison
     * list. Instances that fail to connect are included with error information rather
     * than being excluded from results.
     *
     * @return list of instance comparisons containing stats or error details for each instance
     */
    public List<InstanceComparison> compareOverview() {
        List<InstanceComparison> comparisons = new ArrayList<>();

        for (var instanceInfo : dataSourceManager.getInstanceInfoList()) {
            String instanceId = instanceInfo.getName();
            try {
                OverviewStats stats = postgresService.getOverviewStats(instanceId);
                InstanceComparison comp = new InstanceComparison();
                comp.setInstanceId(instanceId);
                comp.setDisplayName(instanceInfo.getDisplayName());
                comp.setConnected(true);
                comp.setStats(stats);
                comparisons.add(comp);
            } catch (Exception e) {
                InstanceComparison comp = new InstanceComparison();
                comp.setInstanceId(instanceId);
                comp.setDisplayName(instanceInfo.getDisplayName());
                comp.setConnected(false);
                comp.setError(e.getMessage());
                comparisons.add(comp);
            }
        }

        return comparisons;
    }

    /**
     * Finds queries that appear across multiple PostgreSQL instances.
     * <p>
     * Queries are matched using their query fingerprint (normalised query hash) to identify
     * the same logical query running on different instances. Only queries present on at least
     * two instances are returned. Results are sorted by total execution time across all instances.
     *
     * @param instanceIds list of instance identifiers to compare
     * @param limit maximum number of common queries to return
     * @return list of queries appearing on multiple instances with performance metrics from each,
     *         sorted by total time descending
     */
    public List<CrossInstanceQuery> findCommonQueries(List<String> instanceIds, int limit) {
        Map<String, CrossInstanceQuery> queryMap = new HashMap<>();

        for (String instanceId : instanceIds) {
            try {
                List<SlowQuery> queries = postgresService.getSlowQueries(instanceId, "totalTime", "desc");

                for (SlowQuery query : queries) {
                    // Use query ID as fingerprint (normalized query hash)
                    String fingerprint = query.getQueryId();

                    CrossInstanceQuery crossQuery = queryMap.computeIfAbsent(fingerprint, k -> {
                        CrossInstanceQuery cq = new CrossInstanceQuery();
                        cq.setQueryId(fingerprint);
                        cq.setQueryText(query.getQuery());
                        cq.setInstanceMetrics(new HashMap<>());
                        return cq;
                    });

                    QueryMetrics metrics = new QueryMetrics();
                    metrics.setTotalCalls(query.getTotalCalls());
                    metrics.setTotalTime(query.getTotalTime());
                    metrics.setMeanTime(query.getMeanTime());
                    metrics.setMaxTime(query.getMaxTime());
                    metrics.setRows(query.getRows());

                    crossQuery.getInstanceMetrics().put(instanceId, metrics);
                }
            } catch (Exception e) {
                LOG.warnf("Failed to get queries for instance %s: %s", instanceId, e.getMessage());
            }
        }

        // Filter to only queries present in multiple instances
        List<CrossInstanceQuery> commonQueries = queryMap.values().stream()
                .filter(q -> q.getInstanceMetrics().size() > 1)
                .sorted((a, b) -> {
                    // Sort by total time across all instances
                    double totalA = a.getInstanceMetrics().values().stream()
                            .mapToDouble(QueryMetrics::getTotalTime).sum();
                    double totalB = b.getInstanceMetrics().values().stream()
                            .mapToDouble(QueryMetrics::getTotalTime).sum();
                    return Double.compare(totalB, totalA);
                })
                .limit(limit)
                .collect(Collectors.toList());

        // Calculate variance for each query
        for (CrossInstanceQuery query : commonQueries) {
            calculateVariance(query);
        }

        return commonQueries;
    }

    /**
     * Compares performance metrics for a specific query across multiple instances.
     * <p>
     * Retrieves metrics for the identified query from each specified instance and calculates
     * variance statistics including fastest/slowest instance and performance ratio.
     *
     * @param queryId the query identifier (fingerprint) to compare
     * @param instanceIds list of instance identifiers to check
     * @return cross-instance query comparison with metrics from each instance where the query exists
     */
    public CrossInstanceQuery compareQuery(String queryId, List<String> instanceIds) {
        CrossInstanceQuery crossQuery = new CrossInstanceQuery();
        crossQuery.setQueryId(queryId);
        crossQuery.setInstanceMetrics(new HashMap<>());

        for (String instanceId : instanceIds) {
            try {
                SlowQuery query = postgresService.getSlowQueryById(instanceId, queryId);
                if (query != null) {
                    if (crossQuery.getQueryText() == null) {
                        crossQuery.setQueryText(query.getQuery());
                    }

                    QueryMetrics metrics = new QueryMetrics();
                    metrics.setTotalCalls(query.getTotalCalls());
                    metrics.setTotalTime(query.getTotalTime());
                    metrics.setMeanTime(query.getMeanTime());
                    metrics.setMaxTime(query.getMaxTime());
                    metrics.setRows(query.getRows());

                    crossQuery.getInstanceMetrics().put(instanceId, metrics);
                }
            } catch (Exception e) {
                LOG.warnf("Failed to get query %s for instance %s: %s",
                        queryId, instanceId, e.getMessage());
            }
        }

        if (!crossQuery.getInstanceMetrics().isEmpty()) {
            calculateVariance(crossQuery);
        }

        return crossQuery;
    }

    /**
     * Calculates variance statistics for query performance across instances.
     * <p>
     * Computes mean time variance (standard deviation), identifies the fastest and slowest
     * instances, and calculates the performance ratio between them.
     *
     * @param query the cross-instance query to analyse
     */
    private void calculateVariance(CrossInstanceQuery query) {
        var metrics = query.getInstanceMetrics().values();
        if (metrics.size() < 2) {
            query.setMeanTimeVariance(0);
            return;
        }

        // Calculate mean of mean times
        double avgMeanTime = metrics.stream()
                .mapToDouble(QueryMetrics::getMeanTime)
                .average()
                .orElse(0);

        // Calculate variance
        double variance = metrics.stream()
                .mapToDouble(m -> Math.pow(m.getMeanTime() - avgMeanTime, 2))
                .sum() / metrics.size();

        query.setMeanTimeVariance(Math.sqrt(variance));

        // Find fastest and slowest instances
        QueryMetrics fastest = null;
        QueryMetrics slowest = null;
        String fastestInstance = null;
        String slowestInstance = null;

        for (var entry : query.getInstanceMetrics().entrySet()) {
            if (fastest == null || entry.getValue().getMeanTime() < fastest.getMeanTime()) {
                fastest = entry.getValue();
                fastestInstance = entry.getKey();
            }
            if (slowest == null || entry.getValue().getMeanTime() > slowest.getMeanTime()) {
                slowest = entry.getValue();
                slowestInstance = entry.getKey();
            }
        }

        query.setFastestInstance(fastestInstance);
        query.setSlowestInstance(slowestInstance);

        if (fastest != null && slowest != null && fastest.getMeanTime() > 0) {
            query.setPerformanceRatio(slowest.getMeanTime() / fastest.getMeanTime());
        }
    }

    /**
     * Encapsulates comparison data for a single PostgreSQL instance.
     * <p>
     * Contains either overview statistics if the instance connected successfully,
     * or error information if connection failed.
     */
    public static class InstanceComparison {
        private String instanceId;
        private String displayName;
        private boolean connected;
        private String error;
        private OverviewStats stats;

        public String getInstanceId() { return instanceId; }
        public void setInstanceId(String id) { this.instanceId = id; }

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String name) { this.displayName = name; }

        public boolean isConnected() { return connected; }
        public void setConnected(boolean connected) { this.connected = connected; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }

        public OverviewStats getStats() { return stats; }
        public void setStats(OverviewStats stats) { this.stats = stats; }
    }

    /**
     * Query performance metrics collected from a single instance.
     * <p>
     * Aggregates execution statistics including call counts, execution times,
     * and row counts for a specific query.
     */
    public static class QueryMetrics {
        private long totalCalls;
        private double totalTime;
        private double meanTime;
        private double maxTime;
        private long rows;

        public long getTotalCalls() { return totalCalls; }
        public void setTotalCalls(long calls) { this.totalCalls = calls; }

        public double getTotalTime() { return totalTime; }
        public void setTotalTime(double time) { this.totalTime = time; }

        public double getMeanTime() { return meanTime; }
        public void setMeanTime(double time) { this.meanTime = time; }

        public double getMaxTime() { return maxTime; }
        public void setMaxTime(double time) { this.maxTime = time; }

        public long getRows() { return rows; }
        public void setRows(long rows) { this.rows = rows; }

        public String getMeanTimeFormatted() {
            if (meanTime < 1) return String.format("%.3f ms", meanTime);
            if (meanTime < 1000) return String.format("%.2f ms", meanTime);
            if (meanTime < 60000) return String.format("%.2f s", meanTime / 1000);
            return String.format("%.2f min", meanTime / 60000);
        }
    }

    /**
     * Represents a query's performance data aggregated across multiple instances.
     * <p>
     * Contains the query text, metrics from each instance, and calculated statistics
     * including variance, fastest/slowest instances, and performance ratios.
     */
    public static class CrossInstanceQuery {
        private String queryId;
        private String queryText;
        private Map<String, QueryMetrics> instanceMetrics;
        private double meanTimeVariance;
        private String fastestInstance;
        private String slowestInstance;
        private double performanceRatio;

        public String getQueryId() { return queryId; }
        public void setQueryId(String id) { this.queryId = id; }

        public String getQueryText() { return queryText; }
        public void setQueryText(String text) { this.queryText = text; }

        public String getQueryTextPreview() {
            if (queryText == null) return "";
            return queryText.length() > 80 ? queryText.substring(0, 80) + "..." : queryText;
        }

        public Map<String, QueryMetrics> getInstanceMetrics() { return instanceMetrics; }
        public void setInstanceMetrics(Map<String, QueryMetrics> metrics) { this.instanceMetrics = metrics; }

        public int getInstanceCount() {
            return instanceMetrics != null ? instanceMetrics.size() : 0;
        }

        public double getMeanTimeVariance() { return meanTimeVariance; }
        public void setMeanTimeVariance(double variance) { this.meanTimeVariance = variance; }

        public String getFastestInstance() { return fastestInstance; }
        public void setFastestInstance(String instance) { this.fastestInstance = instance; }

        public String getSlowestInstance() { return slowestInstance; }
        public void setSlowestInstance(String instance) { this.slowestInstance = instance; }

        public double getPerformanceRatio() { return performanceRatio; }
        public void setPerformanceRatio(double ratio) { this.performanceRatio = ratio; }

        public String getPerformanceRatioFormatted() {
            if (performanceRatio <= 1) return "1x";
            return String.format("%.1fx", performanceRatio);
        }

        public boolean hasSignificantVariance() {
            return performanceRatio > 2.0;
        }

        public String getVarianceCssClass() {
            if (performanceRatio > 5) return "bg-danger";
            if (performanceRatio > 2) return "bg-warning text-dark";
            return "bg-success";
        }
    }
}
