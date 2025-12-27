package com.bovinemagnet.pgconsole.model;

import java.time.Instant;

/**
 * Statistical baseline for a metric used in anomaly detection.
 * <p>
 * Stores computed statistics (mean, stddev, percentiles) for a specific
 * metric, optionally segmented by hour of day or day of week to capture
 * seasonal patterns.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class MetricBaseline {

    /**
     * Metric category for grouping baselines.
     */
    public enum Category {
        SYSTEM("System", "System-level metrics"),
        QUERY("Query", "Query performance metrics"),
        DATABASE("Database", "Per-database metrics");

        private final String displayName;
        private final String description;

        Category(String displayName, String description) {
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
    private Category category;

    // Statistical values
    private double mean;
    private double stddev;
    private Double min;
    private Double max;
    private Double median;
    private Double p95;
    private Double p99;
    private int sampleCount;

    // Time-based patterns (null for overall baseline)
    private Integer hourOfDay;    // 0-23
    private Integer dayOfWeek;    // 0-6 (Sunday=0)

    // Metadata
    private Instant calculatedAt;
    private Instant periodStart;
    private Instant periodEnd;

    public MetricBaseline() {
    }

    public MetricBaseline(String instanceId, String metricName, Category category) {
        this.instanceId = instanceId;
        this.metricName = metricName;
        this.category = category;
    }

    /**
     * Calculate how many standard deviations a value is from the mean.
     *
     * @param value the value to check
     * @return number of standard deviations (positive = above mean, negative = below)
     */
    public double calculateSigma(double value) {
        if (stddev == 0) {
            return 0;
        }
        return (value - mean) / stddev;
    }

    /**
     * Check if a value is anomalous based on sigma threshold.
     *
     * @param value the value to check
     * @param sigmaThreshold number of standard deviations considered anomalous
     * @return true if the value exceeds the threshold
     */
    public boolean isAnomaly(double value, double sigmaThreshold) {
        return Math.abs(calculateSigma(value)) > sigmaThreshold;
    }

    /**
     * Get a formatted display name for this baseline's time context.
     *
     * @return description of when this baseline applies
     */
    public String getTimeContextDisplay() {
        if (hourOfDay == null && dayOfWeek == null) {
            return "Overall";
        }
        StringBuilder sb = new StringBuilder();
        if (dayOfWeek != null) {
            String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
            sb.append(days[dayOfWeek]);
        }
        if (hourOfDay != null) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(String.format("%02d:00-%02d:59", hourOfDay, hourOfDay));
        }
        return sb.toString();
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

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public double getMean() {
        return mean;
    }

    public void setMean(double mean) {
        this.mean = mean;
    }

    public double getStddev() {
        return stddev;
    }

    public void setStddev(double stddev) {
        this.stddev = stddev;
    }

    public Double getMin() {
        return min;
    }

    public void setMin(Double min) {
        this.min = min;
    }

    public Double getMax() {
        return max;
    }

    public void setMax(Double max) {
        this.max = max;
    }

    public Double getMedian() {
        return median;
    }

    public void setMedian(Double median) {
        this.median = median;
    }

    public Double getP95() {
        return p95;
    }

    public void setP95(Double p95) {
        this.p95 = p95;
    }

    public Double getP99() {
        return p99;
    }

    public void setP99(Double p99) {
        this.p99 = p99;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public void setSampleCount(int sampleCount) {
        this.sampleCount = sampleCount;
    }

    public Integer getHourOfDay() {
        return hourOfDay;
    }

    public void setHourOfDay(Integer hourOfDay) {
        this.hourOfDay = hourOfDay;
    }

    public Integer getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(Integer dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public Instant getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(Instant calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    public Instant getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(Instant periodStart) {
        this.periodStart = periodStart;
    }

    public Instant getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(Instant periodEnd) {
        this.periodEnd = periodEnd;
    }
}
