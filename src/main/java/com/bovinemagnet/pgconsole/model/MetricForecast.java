package com.bovinemagnet.pgconsole.model;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Represents a forecasted value for a metric at a future date.
 * <p>
 * Includes confidence intervals and model quality metrics to help
 * users understand the reliability of the forecast.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class MetricForecast {

    /**
     * Type of forecasting model used.
     */
    public enum ModelType {
        LINEAR("Linear Regression", "Assumes constant rate of change"),
        EXPONENTIAL("Exponential Growth", "Assumes percentage-based growth"),
        SEASONAL("Seasonal", "Accounts for cyclical patterns");

        private final String displayName;
        private final String description;

        ModelType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    private Long id;
    private String instanceId;
    private String metricName;
    private String metricCategory;

    // Forecast values
    private LocalDate forecastDate;
    private double forecastValue;
    private Double confidenceLower;
    private Double confidenceUpper;
    private Double confidenceLevel;  // e.g., 0.95 for 95%

    // Model metadata
    private ModelType modelType;
    private Double rSquared;
    private int dataPointsUsed;
    private int trainingPeriodDays;

    // Timestamps
    private Instant calculatedAt;

    public MetricForecast() {
    }

    /**
     * Get the width of the confidence interval.
     */
    public Double getConfidenceIntervalWidth() {
        if (confidenceLower != null && confidenceUpper != null) {
            return confidenceUpper - confidenceLower;
        }
        return null;
    }

    /**
     * Check if the model quality is acceptable (R² > 0.7).
     */
    public boolean isHighQuality() {
        return rSquared != null && rSquared >= 0.7;
    }

    /**
     * Get a description of model quality.
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
     * Get CSS class for quality indicator.
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
     * Format the forecast value for display.
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
     * Format the confidence interval for display.
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
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public String getMetricCategory() {
        return metricCategory;
    }

    public void setMetricCategory(String metricCategory) {
        this.metricCategory = metricCategory;
    }

    public LocalDate getForecastDate() {
        return forecastDate;
    }

    public void setForecastDate(LocalDate forecastDate) {
        this.forecastDate = forecastDate;
    }

    public double getForecastValue() {
        return forecastValue;
    }

    public void setForecastValue(double forecastValue) {
        this.forecastValue = forecastValue;
    }

    public Double getConfidenceLower() {
        return confidenceLower;
    }

    public void setConfidenceLower(Double confidenceLower) {
        this.confidenceLower = confidenceLower;
    }

    public Double getConfidenceUpper() {
        return confidenceUpper;
    }

    public void setConfidenceUpper(Double confidenceUpper) {
        this.confidenceUpper = confidenceUpper;
    }

    public Double getConfidenceLevel() {
        return confidenceLevel;
    }

    public void setConfidenceLevel(Double confidenceLevel) {
        this.confidenceLevel = confidenceLevel;
    }

    public ModelType getModelType() {
        return modelType;
    }

    public void setModelType(ModelType modelType) {
        this.modelType = modelType;
    }

    public Double getRSquared() {
        return rSquared;
    }

    public void setRSquared(Double rSquared) {
        this.rSquared = rSquared;
    }

    public int getDataPointsUsed() {
        return dataPointsUsed;
    }

    public void setDataPointsUsed(int dataPointsUsed) {
        this.dataPointsUsed = dataPointsUsed;
    }

    public int getTrainingPeriodDays() {
        return trainingPeriodDays;
    }

    public void setTrainingPeriodDays(int trainingPeriodDays) {
        this.trainingPeriodDays = trainingPeriodDays;
    }

    public Instant getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(Instant calculatedAt) {
        this.calculatedAt = calculatedAt;
    }
}
