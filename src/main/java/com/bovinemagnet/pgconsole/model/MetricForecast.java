package com.bovinemagnet.pgconsole.model;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Represents a forecasted value for a metric at a future date.
 * <p>
 * This model encapsulates the results of time-series forecasting for PostgreSQL metrics,
 * including the predicted value, confidence intervals, and model quality indicators. Forecasts
 * are generated using various statistical models (linear, exponential, seasonal) based on
 * historical metric data sampled over time.
 * <p>
 * Each forecast includes:
 * <ul>
 *   <li>A point estimate ({@link #getForecastValue()}) for the metric at a specific future date</li>
 *   <li>Confidence intervals ({@link #getConfidenceLower()}, {@link #getConfidenceUpper()})
 *       indicating prediction uncertainty</li>
 *   <li>Model quality metrics ({@link #getRSquared()}) to assess reliability</li>
 *   <li>Metadata about the forecasting model and training data used</li>
 * </ul>
 * <p>
 * Forecasts are typically used to predict future resource requirements such as database size,
 * connection counts, or cache hit ratios, enabling proactive capacity planning.
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see com.bovinemagnet.pgconsole.model.DatabaseHistory
 * @since 0.0.0
 */
public class MetricForecast {

    /**
     * Enumeration of forecasting model types used for time-series prediction.
     * <p>
     * Each model type represents a different approach to analysing historical data
     * and generating future predictions. The choice of model depends on the characteristics
     * of the metric being forecast.
     */
    public enum ModelType {
        /**
         * Linear regression model assuming constant rate of change.
         * Suitable for metrics with steady, linear growth patterns.
         */
        LINEAR("Linear Regression", "Assumes constant rate of change"),

        /**
         * Exponential growth model assuming percentage-based growth.
         * Suitable for metrics that grow at an increasing rate over time.
         */
        EXPONENTIAL("Exponential Growth", "Assumes percentage-based growth"),

        /**
         * Seasonal model accounting for cyclical patterns.
         * Suitable for metrics with recurring patterns (daily, weekly, monthly cycles).
         */
        SEASONAL("Seasonal", "Accounts for cyclical patterns");

        private final String displayName;
        private final String description;

        /**
         * Constructs a model type with display information.
         *
         * @param displayName the human-readable name of the model type
         * @param description a brief explanation of the model's characteristics
         */
        ModelType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        /**
         * Returns the human-readable display name for this model type.
         *
         * @return the display name (e.g., "Linear Regression")
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Returns a brief description of this model type's characteristics.
         *
         * @return the description explaining when this model is suitable
         */
        public String getDescription() {
            return description;
        }
    }

    /**
     * Unique identifier for this forecast record.
     */
    private Long id;

    /**
     * PostgreSQL instance identifier for multi-instance deployments.
     * Used to distinguish forecasts across different database instances.
     */
    private String instanceId;

    /**
     * Name of the metric being forecast (e.g., "database_size", "cache_hit_ratio").
     */
    private String metricName;

    /**
     * Category grouping for the metric (e.g., "storage", "performance", "connections").
     */
    private String metricCategory;

    /**
     * The future date for which this forecast is calculated.
     */
    private LocalDate forecastDate;

    /**
     * The predicted value of the metric at the forecast date.
     * This is the point estimate from the forecasting model.
     */
    private double forecastValue;

    /**
     * Lower bound of the confidence interval.
     * Represents the minimum expected value within the confidence level.
     * May be null if confidence intervals were not calculated.
     */
    private Double confidenceLower;

    /**
     * Upper bound of the confidence interval.
     * Represents the maximum expected value within the confidence level.
     * May be null if confidence intervals were not calculated.
     */
    private Double confidenceUpper;

    /**
     * The confidence level for the interval (e.g., 0.95 for 95% confidence).
     * Indicates the probability that the actual value will fall within the
     * confidence interval bounds. May be null if not calculated.
     */
    private Double confidenceLevel;

    /**
     * The type of forecasting model used to generate this prediction.
     *
     * @see ModelType
     */
    private ModelType modelType;

    /**
     * R-squared (coefficient of determination) indicating model fit quality.
     * <p>
     * Values range from 0 to 1, where:
     * <ul>
     *   <li>0.9-1.0: Excellent fit</li>
     *   <li>0.7-0.9: Good fit</li>
     *   <li>0.5-0.7: Fair fit</li>
     *   <li>Below 0.5: Poor fit</li>
     * </ul>
     * May be null if R-squared was not calculated.
     *
     * @see #isHighQuality()
     * @see #getQualityDescription()
     */
    private Double rSquared;

    /**
     * Number of historical data points used to train the forecasting model.
     * Higher values generally indicate more reliable predictions.
     */
    private int dataPointsUsed;

    /**
     * Duration in days of the historical period used for model training.
     * For example, 30 indicates the model was trained on the past 30 days of data.
     */
    private int trainingPeriodDays;

    /**
     * Timestamp when this forecast was calculated.
     * Used to track forecast age and determine when recalculation is needed.
     */
    private Instant calculatedAt;

    /**
     * Creates a new empty MetricForecast instance.
     * All fields must be set using setter methods.
     */
    public MetricForecast() {
    }

    /**
     * Calculates the width of the confidence interval.
     * <p>
     * The interval width represents the range of uncertainty in the forecast.
     * Narrower intervals indicate more precise predictions, while wider intervals
     * suggest greater uncertainty.
     *
     * @return the difference between upper and lower confidence bounds, or null
     *         if either bound is not available
     * @see #getConfidenceLower()
     * @see #getConfidenceUpper()
     */
    public Double getConfidenceIntervalWidth() {
        if (confidenceLower != null && confidenceUpper != null) {
            return confidenceUpper - confidenceLower;
        }
        return null;
    }

    /**
     * Determines whether the forecasting model has acceptable quality.
     * <p>
     * A model is considered high quality if its R-squared value is 0.7 or higher,
     * indicating that the model explains at least 70% of the variance in the data.
     *
     * @return true if R-squared is at least 0.7, false otherwise
     * @see #getRSquared()
     * @see #getQualityDescription()
     */
    public boolean isHighQuality() {
        return rSquared != null && rSquared >= 0.7;
    }

    /**
     * Returns a human-readable description of the model's quality.
     * <p>
     * Quality classifications based on R-squared:
     * <ul>
     *   <li>"Excellent" - R² ≥ 0.9</li>
     *   <li>"Good" - R² ≥ 0.7</li>
     *   <li>"Fair" - R² ≥ 0.5</li>
     *   <li>"Poor" - R² &lt; 0.5</li>
     *   <li>"Unknown" - R² not available</li>
     * </ul>
     *
     * @return a quality description suitable for display to users
     * @see #isHighQuality()
     * @see #getQualityCssClass()
     */
    public String getQualityDescription() {
        if (rSquared == null) {
            return "Unknown";
        } else if (rSquared >= 0.9) {
            return "Excellent";
        } else if (rSquared >= 0.7) {
            return "Good";
        } else if (rSquared >= 0.5) {
            return "Fair";
        } else {
            return "Poor";
        }
    }

    /**
     * Returns a Bootstrap CSS class for styling the quality indicator.
     * <p>
     * The CSS class reflects the model quality using standard Bootstrap text colour classes:
     * <ul>
     *   <li>"text-success" (green) - Good or excellent quality (R² ≥ 0.7)</li>
     *   <li>"text-warning" (yellow) - Fair quality (0.5 ≤ R² &lt; 0.7)</li>
     *   <li>"text-danger" (red) - Poor quality (R² &lt; 0.5)</li>
     *   <li>"text-secondary" (grey) - Unknown quality (R² not available)</li>
     * </ul>
     *
     * @return a Bootstrap CSS class name for quality colouring
     * @see #getQualityDescription()
     */
    public String getQualityCssClass() {
        if (rSquared == null) {
            return "text-secondary";
        } else if (rSquared >= 0.7) {
            return "text-success";
        } else if (rSquared >= 0.5) {
            return "text-warning";
        } else {
            return "text-danger";
        }
    }

    /**
     * Formats the forecast value for human-readable display.
     * <p>
     * The formatting is context-aware based on the metric name:
     * <ul>
     *   <li>Size metrics (containing "size") - formatted as bytes (B, KB, MB, GB, TB)</li>
     *   <li>Ratio metrics (containing "ratio") - formatted as percentage</li>
     *   <li>Other metrics - formatted as decimal with 2 decimal places</li>
     * </ul>
     *
     * @return the formatted forecast value suitable for display
     * @see #getFormattedConfidenceInterval()
     */
    public String getFormattedValue() {
        if (metricName != null && metricName.toLowerCase().contains("size")) {
            return formatBytes(forecastValue);
        } else if (metricName != null && metricName.toLowerCase().contains("ratio")) {
            return String.format("%.1f%%", forecastValue * 100);
        }
        return String.format("%.2f", forecastValue);
    }

    /**
     * Formats the confidence interval for human-readable display.
     * <p>
     * Returns a range string (e.g., "1.5 GB - 2.3 GB") formatted according to
     * the metric type. If confidence bounds are not available, returns "N/A".
     *
     * @return the formatted confidence interval range, or "N/A" if bounds are null
     * @see #getFormattedValue()
     * @see #getConfidenceLower()
     * @see #getConfidenceUpper()
     */
    public String getFormattedConfidenceInterval() {
        if (confidenceLower == null || confidenceUpper == null) {
            return "N/A";
        }
        if (metricName != null && metricName.toLowerCase().contains("size")) {
            return formatBytes(confidenceLower) + " - " + formatBytes(confidenceUpper);
        }
        return String.format("%.2f - %.2f", confidenceLower, confidenceUpper);
    }

    /**
     * Formats a byte value into human-readable units (B, KB, MB, GB, TB).
     * <p>
     * Uses base-1024 conversion and selects the largest unit that results in
     * a value greater than or equal to 1.0.
     *
     * @param bytes the byte value to format
     * @return a formatted string with appropriate unit suffix (e.g., "1.5 GB")
     */
    private String formatBytes(double bytes) {
        if (bytes >= 1024L * 1024 * 1024 * 1024) {
            return String.format("%.1f TB", bytes / (1024.0 * 1024 * 1024 * 1024));
        } else if (bytes >= 1024L * 1024 * 1024) {
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        } else if (bytes >= 1024L * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        } else if (bytes >= 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        return String.format("%.0f B", bytes);
    }

    // Getters and setters

    /**
     * Returns the unique identifier for this forecast record.
     *
     * @return the forecast ID, or null if not yet persisted
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the unique identifier for this forecast record.
     *
     * @param id the forecast ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns the PostgreSQL instance identifier.
     *
     * @return the instance ID
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * Sets the PostgreSQL instance identifier.
     *
     * @param instanceId the instance ID
     */
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    /**
     * Returns the name of the metric being forecast.
     *
     * @return the metric name (e.g., "database_size", "cache_hit_ratio")
     */
    public String getMetricName() {
        return metricName;
    }

    /**
     * Sets the name of the metric being forecast.
     *
     * @param metricName the metric name
     */
    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    /**
     * Returns the category grouping for this metric.
     *
     * @return the metric category (e.g., "storage", "performance")
     */
    public String getMetricCategory() {
        return metricCategory;
    }

    /**
     * Sets the category grouping for this metric.
     *
     * @param metricCategory the metric category
     */
    public void setMetricCategory(String metricCategory) {
        this.metricCategory = metricCategory;
    }

    /**
     * Returns the future date for which this forecast applies.
     *
     * @return the forecast date
     */
    public LocalDate getForecastDate() {
        return forecastDate;
    }

    /**
     * Sets the future date for which this forecast applies.
     *
     * @param forecastDate the forecast date
     */
    public void setForecastDate(LocalDate forecastDate) {
        this.forecastDate = forecastDate;
    }

    /**
     * Returns the predicted value of the metric at the forecast date.
     *
     * @return the forecast value (point estimate)
     */
    public double getForecastValue() {
        return forecastValue;
    }

    /**
     * Sets the predicted value of the metric at the forecast date.
     *
     * @param forecastValue the forecast value
     */
    public void setForecastValue(double forecastValue) {
        this.forecastValue = forecastValue;
    }

    /**
     * Returns the lower bound of the confidence interval.
     *
     * @return the lower confidence bound, or null if not calculated
     */
    public Double getConfidenceLower() {
        return confidenceLower;
    }

    /**
     * Sets the lower bound of the confidence interval.
     *
     * @param confidenceLower the lower confidence bound, or null if not available
     */
    public void setConfidenceLower(Double confidenceLower) {
        this.confidenceLower = confidenceLower;
    }

    /**
     * Returns the upper bound of the confidence interval.
     *
     * @return the upper confidence bound, or null if not calculated
     */
    public Double getConfidenceUpper() {
        return confidenceUpper;
    }

    /**
     * Sets the upper bound of the confidence interval.
     *
     * @param confidenceUpper the upper confidence bound, or null if not available
     */
    public void setConfidenceUpper(Double confidenceUpper) {
        this.confidenceUpper = confidenceUpper;
    }

    /**
     * Returns the confidence level for the interval.
     *
     * @return the confidence level (e.g., 0.95 for 95%), or null if not calculated
     */
    public Double getConfidenceLevel() {
        return confidenceLevel;
    }

    /**
     * Sets the confidence level for the interval.
     *
     * @param confidenceLevel the confidence level (e.g., 0.95 for 95%)
     */
    public void setConfidenceLevel(Double confidenceLevel) {
        this.confidenceLevel = confidenceLevel;
    }

    /**
     * Returns the type of forecasting model used.
     *
     * @return the model type
     * @see ModelType
     */
    public ModelType getModelType() {
        return modelType;
    }

    /**
     * Sets the type of forecasting model used.
     *
     * @param modelType the model type
     * @see ModelType
     */
    public void setModelType(ModelType modelType) {
        this.modelType = modelType;
    }

    /**
     * Returns the R-squared value indicating model fit quality.
     *
     * @return the R-squared value (0 to 1), or null if not calculated
     * @see #isHighQuality()
     * @see #getQualityDescription()
     */
    public Double getRSquared() {
        return rSquared;
    }

    /**
     * Sets the R-squared value indicating model fit quality.
     *
     * @param rSquared the R-squared value (0 to 1)
     */
    public void setRSquared(Double rSquared) {
        this.rSquared = rSquared;
    }

    /**
     * Returns the number of data points used to train the model.
     *
     * @return the number of data points
     */
    public int getDataPointsUsed() {
        return dataPointsUsed;
    }

    /**
     * Sets the number of data points used to train the model.
     *
     * @param dataPointsUsed the number of data points
     */
    public void setDataPointsUsed(int dataPointsUsed) {
        this.dataPointsUsed = dataPointsUsed;
    }

    /**
     * Returns the duration in days of the training period.
     *
     * @return the training period in days
     */
    public int getTrainingPeriodDays() {
        return trainingPeriodDays;
    }

    /**
     * Sets the duration in days of the training period.
     *
     * @param trainingPeriodDays the training period in days
     */
    public void setTrainingPeriodDays(int trainingPeriodDays) {
        this.trainingPeriodDays = trainingPeriodDays;
    }

    /**
     * Returns the timestamp when this forecast was calculated.
     *
     * @return the calculation timestamp
     */
    public Instant getCalculatedAt() {
        return calculatedAt;
    }

    /**
     * Sets the timestamp when this forecast was calculated.
     *
     * @param calculatedAt the calculation timestamp
     */
    public void setCalculatedAt(Instant calculatedAt) {
        this.calculatedAt = calculatedAt;
    }
}
