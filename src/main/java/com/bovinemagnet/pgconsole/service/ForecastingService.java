package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.MetricForecast;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * Service for generating metric forecasts using linear regression.
 * <p>
 * Provides predictions for storage growth, connection trends, and
 * other key metrics to support capacity planning.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class ForecastingService {

    private static final Logger LOG = Logger.getLogger(ForecastingService.class);

    // Metrics to forecast
    private static final List<String> FORECASTABLE_METRICS = List.of(
            "total_database_size_bytes",
            "total_connections"
    );

    @Inject
    DataSourceManager dataSourceManager;

    /**
     * Generate forecasts for all forecastable metrics.
     *
     * @param instanceName the PostgreSQL instance name
     * @param trainingDays days of historical data to use
     * @param forecastDays days to forecast into the future
     * @return map of metric name to list of forecasts
     */
    public Map<String, List<MetricForecast>> generateForecasts(String instanceName,
                                                                 int trainingDays,
                                                                 int forecastDays) {
        Map<String, List<MetricForecast>> forecasts = new HashMap<>();

        for (String metric : FORECASTABLE_METRICS) {
            List<MetricForecast> metricForecasts = generateForecastForMetric(
                    instanceName, metric, trainingDays, forecastDays);
            if (!metricForecasts.isEmpty()) {
                forecasts.put(metric, metricForecasts);
            }
        }

        return forecasts;
    }

    /**
     * Generate forecast for a specific metric.
     *
     * @param instanceName the PostgreSQL instance name
     * @param metricName the metric to forecast
     * @param trainingDays days of historical data to use
     * @param forecastDays days to forecast
     * @return list of forecasts
     */
    public List<MetricForecast> generateForecastForMetric(String instanceName,
                                                           String metricName,
                                                           int trainingDays,
                                                           int forecastDays) {
        List<MetricForecast> forecasts = new ArrayList<>();

        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            // Get historical data points
            List<DataPoint> dataPoints = getHistoricalData(ds, instanceName, metricName, trainingDays);

            if (dataPoints.size() < 7) {
                LOG.debugf("Not enough data points for forecasting %s (need at least 7, have %d)",
                        metricName, dataPoints.size());
                return forecasts;
            }

            // Perform linear regression
            RegressionResult regression = performLinearRegression(dataPoints);

            if (regression == null) {
                return forecasts;
            }

            // Generate forecasts for each day
            LocalDate today = LocalDate.now();
            for (int day = 1; day <= forecastDays; day++) {
                LocalDate forecastDate = today.plusDays(day);

                // Calculate forecast value using the regression line
                // x is days from the start of training period
                double x = dataPoints.size() + day;
                double forecastValue = regression.slope * x + regression.intercept;

                // Calculate confidence interval (using standard error)
                double confidenceMultiplier = 1.96;  // 95% confidence
                double standardError = regression.standardError * Math.sqrt(1 + 1.0 / dataPoints.size()
                        + Math.pow(x - regression.meanX, 2) / regression.sumSquaredDeviationsX);
                double confidenceLower = forecastValue - confidenceMultiplier * standardError;
                double confidenceUpper = forecastValue + confidenceMultiplier * standardError;

                // Ensure non-negative values for size metrics
                if (metricName.contains("size") || metricName.contains("connections")) {
                    forecastValue = Math.max(0, forecastValue);
                    confidenceLower = Math.max(0, confidenceLower);
                    confidenceUpper = Math.max(0, confidenceUpper);
                }

                MetricForecast forecast = new MetricForecast();
                forecast.setInstanceId(instanceName);
                forecast.setMetricName(metricName);
                forecast.setMetricCategory("system");
                forecast.setForecastDate(forecastDate);
                forecast.setForecastValue(forecastValue);
                forecast.setConfidenceLower(confidenceLower);
                forecast.setConfidenceUpper(confidenceUpper);
                forecast.setConfidenceLevel(0.95);
                forecast.setModelType(MetricForecast.ModelType.LINEAR);
                forecast.setRSquared(regression.rSquared);
                forecast.setDataPointsUsed(dataPoints.size());
                forecast.setTrainingPeriodDays(trainingDays);
                forecast.setCalculatedAt(Instant.now());

                forecasts.add(forecast);

                // Save to database
                saveForecast(ds, forecast);
            }

        } catch (Exception e) {
            LOG.errorf(e, "Error generating forecast for metric %s", metricName);
        }

        return forecasts;
    }

    /**
     * Get stored forecasts for a metric.
     *
     * @param instanceName the PostgreSQL instance name
     * @param metricName the metric name
     * @return list of forecasts
     */
    public List<MetricForecast> getForecasts(String instanceName, String metricName) {
        List<MetricForecast> forecasts = new ArrayList<>();

        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            String sql = """
                SELECT id, metric_name, metric_category, forecast_date, forecast_value,
                       confidence_lower, confidence_upper, confidence_level,
                       model_type, r_squared, data_points_used, training_period_days, calculated_at
                FROM pgconsole.metric_forecast
                WHERE instance_id = ? AND metric_name = ? AND forecast_date >= CURRENT_DATE
                ORDER BY forecast_date
                """;

            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, instanceName);
                stmt.setString(2, metricName);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        forecasts.add(mapForecast(rs, instanceName));
                    }
                }
            }

        } catch (Exception e) {
            LOG.errorf(e, "Error getting forecasts for metric %s", metricName);
        }

        return forecasts;
    }

    /**
     * Get days until a threshold is breached.
     *
     * @param instanceName the PostgreSQL instance name
     * @param metricName the metric name
     * @param threshold the threshold value
     * @return days until breach, or null if threshold won't be breached in forecast period
     */
    public Integer getDaysUntilThreshold(String instanceName, String metricName, double threshold) {
        List<MetricForecast> forecasts = getForecasts(instanceName, metricName);

        for (MetricForecast forecast : forecasts) {
            if (forecast.getForecastValue() >= threshold) {
                return (int) java.time.temporal.ChronoUnit.DAYS.between(
                        LocalDate.now(), forecast.getForecastDate());
            }
        }

        return null;
    }

    /**
     * Get storage growth rate in bytes per day.
     *
     * @param instanceName the PostgreSQL instance name
     * @return average daily growth in bytes, or null if not enough data
     */
    public Double getStorageGrowthRate(String instanceName) {
        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            List<DataPoint> dataPoints = getHistoricalData(ds, instanceName,
                    "total_database_size_bytes", 7);

            if (dataPoints.size() < 2) {
                return null;
            }

            RegressionResult regression = performLinearRegression(dataPoints);
            return regression != null ? regression.slope : null;

        } catch (Exception e) {
            LOG.debugf(e, "Error calculating storage growth rate");
            return null;
        }
    }

    /**
     * Calculate days until storage is full based on current growth rate.
     *
     * @param instanceName the PostgreSQL instance name
     * @param maxStorageBytes maximum available storage in bytes
     * @return days until full, or null if not determinable
     */
    public Integer getDaysUntilStorageFull(String instanceName, long maxStorageBytes) {
        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            // Get current size
            Long currentSize = getCurrentValue(ds, instanceName, "total_database_size_bytes");
            if (currentSize == null) {
                return null;
            }

            Double growthRate = getStorageGrowthRate(instanceName);
            if (growthRate == null || growthRate <= 0) {
                return null;  // Not growing or can't determine
            }

            double remainingSpace = maxStorageBytes - currentSize;
            if (remainingSpace <= 0) {
                return 0;  // Already full
            }

            return (int) Math.ceil(remainingSpace / growthRate);

        } catch (Exception e) {
            LOG.debugf(e, "Error calculating days until storage full");
            return null;
        }
    }

    // Private helper methods

    private List<DataPoint> getHistoricalData(DataSource ds, String instanceName,
                                               String metricName, int days) {
        List<DataPoint> dataPoints = new ArrayList<>();

        String sql = """
            SELECT DATE(sampled_at) as day, AVG(%s) as value
            FROM pgconsole.system_metrics_history
            WHERE instance_id = ? AND sampled_at > NOW() - INTERVAL '%d days'
            GROUP BY DATE(sampled_at)
            ORDER BY day
            """.formatted(metricName, days);

        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, instanceName);

            try (ResultSet rs = stmt.executeQuery()) {
                int index = 0;
                while (rs.next()) {
                    java.sql.Date date = rs.getDate("day");
                    double value = rs.getDouble("value");
                    dataPoints.add(new DataPoint(index++, date.toLocalDate(), value));
                }
            }

        } catch (Exception e) {
            LOG.debugf(e, "Error getting historical data for %s", metricName);
        }

        return dataPoints;
    }

    private Long getCurrentValue(DataSource ds, String instanceName, String metricName) {
        String sql = """
            SELECT %s FROM pgconsole.system_metrics_history
            WHERE instance_id = ?
            ORDER BY sampled_at DESC LIMIT 1
            """.formatted(metricName);

        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, instanceName);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }

        } catch (Exception e) {
            LOG.debugf(e, "Error getting current value for %s", metricName);
        }

        return null;
    }

    private RegressionResult performLinearRegression(List<DataPoint> dataPoints) {
        if (dataPoints.isEmpty()) {
            return null;
        }

        int n = dataPoints.size();

        // Calculate means
        double sumX = 0, sumY = 0;
        for (DataPoint dp : dataPoints) {
            sumX += dp.x;
            sumY += dp.value;
        }
        double meanX = sumX / n;
        double meanY = sumY / n;

        // Calculate slope and intercept
        double sumXY = 0, sumXX = 0, sumYY = 0;
        for (DataPoint dp : dataPoints) {
            double dx = dp.x - meanX;
            double dy = dp.value - meanY;
            sumXY += dx * dy;
            sumXX += dx * dx;
            sumYY += dy * dy;
        }

        if (sumXX == 0) {
            return null;
        }

        double slope = sumXY / sumXX;
        double intercept = meanY - slope * meanX;

        // Calculate R-squared
        double ssTotal = sumYY;
        double ssResidual = 0;
        for (DataPoint dp : dataPoints) {
            double predicted = slope * dp.x + intercept;
            ssResidual += Math.pow(dp.value - predicted, 2);
        }
        double rSquared = ssTotal > 0 ? 1 - (ssResidual / ssTotal) : 0;

        // Calculate standard error
        double standardError = n > 2 ? Math.sqrt(ssResidual / (n - 2)) : 0;

        return new RegressionResult(slope, intercept, rSquared, standardError, meanX, sumXX);
    }

    private void saveForecast(DataSource ds, MetricForecast forecast) {
        String sql = """
            INSERT INTO pgconsole.metric_forecast
                (instance_id, metric_name, metric_category, forecast_date, forecast_value,
                 confidence_lower, confidence_upper, confidence_level,
                 model_type, r_squared, data_points_used, training_period_days, calculated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
            ON CONFLICT (instance_id, metric_name, forecast_date)
            DO UPDATE SET
                forecast_value = EXCLUDED.forecast_value,
                confidence_lower = EXCLUDED.confidence_lower,
                confidence_upper = EXCLUDED.confidence_upper,
                r_squared = EXCLUDED.r_squared,
                data_points_used = EXCLUDED.data_points_used,
                calculated_at = NOW()
            """;

        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, forecast.getInstanceId());
            stmt.setString(2, forecast.getMetricName());
            stmt.setString(3, forecast.getMetricCategory());
            stmt.setDate(4, java.sql.Date.valueOf(forecast.getForecastDate()));
            stmt.setDouble(5, forecast.getForecastValue());
            stmt.setObject(6, forecast.getConfidenceLower(), Types.DOUBLE);
            stmt.setObject(7, forecast.getConfidenceUpper(), Types.DOUBLE);
            stmt.setObject(8, forecast.getConfidenceLevel(), Types.DOUBLE);
            stmt.setString(9, forecast.getModelType().name());
            stmt.setObject(10, forecast.getRSquared(), Types.DOUBLE);
            stmt.setInt(11, forecast.getDataPointsUsed());
            stmt.setInt(12, forecast.getTrainingPeriodDays());

            stmt.executeUpdate();

        } catch (Exception e) {
            LOG.debugf(e, "Error saving forecast");
        }
    }

    private MetricForecast mapForecast(ResultSet rs, String instanceName) throws SQLException {
        MetricForecast forecast = new MetricForecast();
        forecast.setId(rs.getLong("id"));
        forecast.setInstanceId(instanceName);
        forecast.setMetricName(rs.getString("metric_name"));
        forecast.setMetricCategory(rs.getString("metric_category"));

        java.sql.Date forecastDate = rs.getDate("forecast_date");
        if (forecastDate != null) {
            forecast.setForecastDate(forecastDate.toLocalDate());
        }

        forecast.setForecastValue(rs.getDouble("forecast_value"));
        forecast.setConfidenceLower(rs.getDouble("confidence_lower"));
        forecast.setConfidenceUpper(rs.getDouble("confidence_upper"));
        forecast.setConfidenceLevel(rs.getDouble("confidence_level"));

        String modelTypeStr = rs.getString("model_type");
        if (modelTypeStr != null) {
            try {
                forecast.setModelType(MetricForecast.ModelType.valueOf(modelTypeStr));
            } catch (IllegalArgumentException e) {
                forecast.setModelType(MetricForecast.ModelType.LINEAR);
            }
        }

        forecast.setRSquared(rs.getDouble("r_squared"));
        forecast.setDataPointsUsed(rs.getInt("data_points_used"));
        forecast.setTrainingPeriodDays(rs.getInt("training_period_days"));

        Timestamp calculatedAt = rs.getTimestamp("calculated_at");
        if (calculatedAt != null) {
            forecast.setCalculatedAt(calculatedAt.toInstant());
        }

        return forecast;
    }

    /**
     * A data point for regression.
     */
    private record DataPoint(int x, LocalDate date, double value) {
    }

    /**
     * Result of linear regression.
     */
    private record RegressionResult(double slope, double intercept, double rSquared,
                                     double standardError, double meanX,
                                     double sumSquaredDeviationsX) {
    }
}
