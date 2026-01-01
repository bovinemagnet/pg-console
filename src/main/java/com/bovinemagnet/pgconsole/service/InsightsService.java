package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.*;

/**
 * Aggregates intelligent insights from all sources into a unified dashboard view.
 * <p>
 * Combines anomaly detection, forecasting, recommendations, and health metrics
 * to provide a comprehensive overview of database health.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class InsightsService {

    private static final Logger LOG = Logger.getLogger(InsightsService.class);

    @Inject
    AnomalyDetectionService anomalyDetectionService;

    @Inject
    ForecastingService forecastingService;

    @Inject
    UnifiedRecommendationService recommendationService;

    @Inject
    NaturalLanguageQueryService nlQueryService;

    @Inject
    RunbookService runbookService;

    /**
     * Get a comprehensive insights summary for an instance.
     *
     * @param instanceName the PostgreSQL instance name
     * @return insight summary
     */
    public InsightSummary getSummary(String instanceName) {
        InsightSummary summary = new InsightSummary(instanceName);

        // Get anomaly counts
        Map<DetectedAnomaly.Severity, Integer> anomalyCounts = anomalyDetectionService.getAnomalySummary(instanceName);
        summary.setAnomalyCountCritical(anomalyCounts.getOrDefault(DetectedAnomaly.Severity.CRITICAL, 0));
        summary.setAnomalyCountHigh(anomalyCounts.getOrDefault(DetectedAnomaly.Severity.HIGH, 0));
        summary.setAnomalyCountMedium(anomalyCounts.getOrDefault(DetectedAnomaly.Severity.MEDIUM, 0));
        summary.setAnomalyCountLow(anomalyCounts.getOrDefault(DetectedAnomaly.Severity.LOW, 0));

        // Get recommendation counts
        Map<UnifiedRecommendation.Severity, Integer> recCounts = recommendationService.getSummary(instanceName);
        summary.setRecommendationCountCritical(recCounts.getOrDefault(UnifiedRecommendation.Severity.CRITICAL, 0));
        summary.setRecommendationCountHigh(recCounts.getOrDefault(UnifiedRecommendation.Severity.HIGH, 0));
        summary.setRecommendationCountMedium(recCounts.getOrDefault(UnifiedRecommendation.Severity.MEDIUM, 0));
        summary.setRecommendationCountLow(recCounts.getOrDefault(UnifiedRecommendation.Severity.LOW, 0));

        // Get storage forecast alerts
        // Assume 100GB warning and 120GB critical thresholds for demo
        Integer storageDaysWarning = forecastingService.getDaysUntilThreshold(
                instanceName, "total_database_size_bytes", 100L * 1024 * 1024 * 1024);
        Integer storageDaysCritical = forecastingService.getDaysUntilThreshold(
                instanceName, "total_database_size_bytes", 120L * 1024 * 1024 * 1024);
        summary.setStorageDaysUntilWarning(storageDaysWarning);
        summary.setStorageDaysUntilCritical(storageDaysCritical);

        // Calculate health score
        int healthScore = InsightSummary.calculateHealthScore(
                summary.getAnomalyCountCritical() + summary.getRecommendationCountCritical(),
                summary.getAnomalyCountHigh() + summary.getRecommendationCountHigh(),
                summary.getAnomalyCountMedium() + summary.getRecommendationCountMedium(),
                summary.getAnomalyCountLow() + summary.getRecommendationCountLow(),
                storageDaysWarning
        );
        summary.setOverallHealthScore(healthScore);

        // Build top concerns list
        List<InsightSummary.Concern> concerns = buildTopConcerns(instanceName);
        summary.setTopConcerns(concerns);

        return summary;
    }

    /**
     * Get open anomalies for an instance.
     *
     * @param instanceName the PostgreSQL instance name
     * @return list of open anomalies
     */
    public List<DetectedAnomaly> getOpenAnomalies(String instanceName) {
        return anomalyDetectionService.getOpenAnomalies(instanceName);
    }

    /**
     * Get anomaly history.
     *
     * @param instanceName the PostgreSQL instance name
     * @param hours hours to look back
     * @return list of anomalies
     */
    public List<DetectedAnomaly> getAnomalyHistory(String instanceName, int hours) {
        return anomalyDetectionService.getAnomalyHistory(instanceName, hours);
    }

    /**
     * Get all recommendations.
     *
     * @param instanceName the PostgreSQL instance name
     * @return list of unified recommendations
     */
    public List<UnifiedRecommendation> getRecommendations(String instanceName) {
        return recommendationService.getRecommendations(instanceName);
    }

    /**
     * Get top recommendations.
     *
     * @param instanceName the PostgreSQL instance name
     * @param limit maximum number to return
     * @return top recommendations
     */
    public List<UnifiedRecommendation> getTopRecommendations(String instanceName, int limit) {
        return recommendationService.getTopRecommendations(instanceName, limit);
    }

    /**
     * Get storage forecast.
     *
     * @param instanceName the PostgreSQL instance name
     * @return list of storage forecasts
     */
    public List<MetricForecast> getStorageForecasts(String instanceName) {
        return forecastingService.getForecasts(instanceName, "total_database_size_bytes");
    }

    /**
     * Get connection forecast.
     *
     * @param instanceName the PostgreSQL instance name
     * @return list of connection forecasts
     */
    public List<MetricForecast> getConnectionForecasts(String instanceName) {
        return forecastingService.getForecasts(instanceName, "total_connections");
    }

    /**
     * Get storage growth rate.
     *
     * @param instanceName the PostgreSQL instance name
     * @return bytes per day, or null if not available
     */
    public Double getStorageGrowthRate(String instanceName) {
        return forecastingService.getStorageGrowthRate(instanceName);
    }

    /**
     * Parse a natural language query.
     *
     * @param queryText the query text
     * @param instanceName the PostgreSQL instance name
     * @return parsed query
     */
    public NaturalLanguageQuery parseNaturalLanguageQuery(String queryText, String instanceName) {
        return nlQueryService.parseQuery(queryText, instanceName);
    }

    /**
     * Get suggested natural language queries.
     *
     * @return list of example queries
     */
    public List<String> getSuggestedQueries() {
        return nlQueryService.getSuggestedQueries();
    }

    /**
     * Get available runbooks.
     *
     * @param instanceName the PostgreSQL instance name
     * @return list of runbooks
     */
    public List<Runbook> getRunbooks(String instanceName) {
        return runbookService.getRunbooks(instanceName);
    }

    /**
     * Get runbooks that might apply to current issues.
     *
     * @param instanceName the PostgreSQL instance name
     * @return list of suggested runbooks
     */
    public List<Runbook> getSuggestedRunbooks(String instanceName) {
        List<Runbook> suggested = new ArrayList<>();
        List<DetectedAnomaly> openAnomalies = getOpenAnomalies(instanceName);

        // Suggest runbooks based on current issues
        for (DetectedAnomaly anomaly : openAnomalies) {
            String metricName = anomaly.getMetricName();

            if ("total_connections".equals(metricName) && anomaly.getDirection() == DetectedAnomaly.Direction.ABOVE) {
                Runbook rb = runbookService.getRunbookByName(instanceName, "high_connection_usage");
                if (rb != null && !suggested.contains(rb)) {
                    suggested.add(rb);
                }
            }

            if ("blocked_queries".equals(metricName) && anomaly.getDirection() == DetectedAnomaly.Direction.ABOVE) {
                Runbook rb = runbookService.getRunbookByName(instanceName, "blocked_queries");
                if (rb != null && !suggested.contains(rb)) {
                    suggested.add(rb);
                }
            }

            if ("cache_hit_ratio".equals(metricName) && anomaly.getDirection() == DetectedAnomaly.Direction.BELOW) {
                Runbook rb = runbookService.getRunbookByName(instanceName, "low_cache_hit");
                if (rb != null && !suggested.contains(rb)) {
                    suggested.add(rb);
                }
            }
        }

        return suggested;
    }

    /**
     * Get in-progress runbook executions.
     *
     * @param instanceName the PostgreSQL instance name
     * @return list of in-progress executions
     */
    public List<RunbookExecution> getInProgressExecutions(String instanceName) {
        return runbookService.getInProgressExecutions(instanceName);
    }

    /**
     * Trigger baseline recalculation and forecasting.
     *
     * @param instanceName the PostgreSQL instance name
     */
    public void refreshInsights(String instanceName) {
        LOG.infof("Refreshing insights for instance %s", instanceName);

        try {
            // Recalculate baselines
            anomalyDetectionService.calculateBaselines(instanceName, 7);

            // Detect current anomalies
            anomalyDetectionService.detectAnomalies(instanceName);

            // Generate forecasts
            forecastingService.generateForecasts(instanceName, 7, 30);

            LOG.infof("Insights refresh completed for instance %s", instanceName);

        } catch (Exception e) {
            LOG.errorf(e, "Error refreshing insights for instance %s", instanceName);
        }
    }

    /**
     * Explain a database term in plain English.
     *
     * @param term the term to explain
     * @return explanation
     */
    public String explainTerm(String term) {
        return nlQueryService.explainTerm(term);
    }

    // Private helper methods

    private List<InsightSummary.Concern> buildTopConcerns(String instanceName) {
        List<InsightSummary.Concern> concerns = new ArrayList<>();

        // Add critical anomalies
        for (DetectedAnomaly anomaly : getOpenAnomalies(instanceName)) {
            if (anomaly.getSeverity() == DetectedAnomaly.Severity.CRITICAL
                    || anomaly.getSeverity() == DetectedAnomaly.Severity.HIGH) {
                concerns.add(new InsightSummary.Concern(
                        "Anomaly: " + anomaly.getMetricName(),
                        anomaly.getRootCauseSuggestion(),
                        anomaly.getSeverity(),
                        "anomaly",
                        "/insights/anomalies?id=" + anomaly.getId()
                ));
            }
        }

        // Add critical recommendations
        for (UnifiedRecommendation rec : getTopRecommendations(instanceName, 5)) {
            if (rec.getSeverity() == UnifiedRecommendation.Severity.CRITICAL
                    || rec.getSeverity() == UnifiedRecommendation.Severity.HIGH) {
                concerns.add(new InsightSummary.Concern(
                        rec.getTitle(),
                        rec.getDescription(),
                        DetectedAnomaly.Severity.valueOf(rec.getSeverity().name()),
                        "recommendation",
                        "/insights/recommendations"
                ));
            }
        }

        // Sort by severity (critical first) and limit to top 5
        concerns.sort((a, b) -> {
            if (a.getSeverity() == null && b.getSeverity() == null) return 0;
            if (a.getSeverity() == null) return 1;
            if (b.getSeverity() == null) return -1;
            return Integer.compare(
                    getSeverityWeight(b.getSeverity()),
                    getSeverityWeight(a.getSeverity())
            );
        });

        return concerns.size() > 5 ? concerns.subList(0, 5) : concerns;
    }

    private int getSeverityWeight(DetectedAnomaly.Severity severity) {
        return switch (severity) {
            case CRITICAL -> 4;
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }
}
