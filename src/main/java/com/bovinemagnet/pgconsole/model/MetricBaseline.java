package com.bovinemagnet.pgconsole.model;

import java.time.Instant;

/**
 * Statistical baseline for a metric used in anomaly detection and performance monitoring.
 * <p>
 * This class stores computed statistical measures (mean, standard deviation, percentiles)
 * for a specific PostgreSQL performance metric. Baselines can be calculated for the overall
 * time period or segmented by hour of day and day of week to capture seasonal and cyclical
 * patterns in database behaviour.
 * <p>
 * The baseline enables anomaly detection by comparing current metric values against
 * historical normal behaviour. For example, a database that typically handles 100
 * queries per second during business hours can be flagged as anomalous if it suddenly
 * processes 1000 queries per second.
 * <p>
 * Example usage:
 * <pre>{@code
 * MetricBaseline baseline = new MetricBaseline("prod-db-01", "connections", Category.SYSTEM);
 * baseline.setMean(50.0);
 * baseline.setStddev(10.0);
 *
 * double currentValue = 85.0;
 * double sigma = baseline.calculateSigma(currentValue);  // Returns 3.5
 * boolean anomaly = baseline.isAnomaly(currentValue, 3.0);  // Returns true
 * }</pre>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see com.bovinemagnet.pgconsole.service.MetricsSamplerService
 */
public class MetricBaseline {

    /**
     * Categorises metrics by their scope and purpose for organisational grouping.
     * <p>
     * Categories help organise and filter baselines when displaying anomalies or
     * when applying different detection thresholds to different metric types.
     */
    public enum Category {
        /** System-wide metrics such as total connections, cache hit ratio, or disk I/O. */
        SYSTEM("System", "System-level metrics"),

        /** Query-specific metrics from pg_stat_statements such as execution time or calls. */
        QUERY("Query", "Query performance metrics"),

        /** Per-database metrics such as transactions, tuple operations, or database size. */
        DATABASE("Database", "Per-database metrics");

        private final String displayName;
        private final String description;

        /**
         * Constructs a category with display name and description.
         *
         * @param displayName human-readable name for UI presentation
         * @param description detailed explanation of the category's purpose
         */
        Category(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        /**
         * Returns the human-readable display name for this category.
         *
         * @return the display name, never null
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Returns the detailed description of this category's purpose.
         *
         * @return the description, never null
         */
        public String getDescription() {
            return description;
        }
    }

    /** Database-generated unique identifier for this baseline record. */
    private Long id;

    /** PostgreSQL instance identifier this baseline applies to. */
    private String instanceId;

    /** Name of the metric being measured (e.g., "connections", "cache_hit_ratio"). */
    private String metricName;

    /** Category classifying the type of metric. */
    private Category category;

    /** Arithmetic mean of all sampled values. */
    private double mean;

    /** Standard deviation measuring variability from the mean. */
    private double stddev;

    /** Minimum observed value in the sample period, or null if not calculated. */
    private Double min;

    /** Maximum observed value in the sample period, or null if not calculated. */
    private Double max;

    /** Median value (50th percentile), or null if not calculated. */
    private Double median;

    /** 95th percentile value, or null if not calculated. */
    private Double p95;

    /** 99th percentile value, or null if not calculated. */
    private Double p99;

    /** Number of data points used to calculate these statistics. */
    private int sampleCount;

    /**
     * Hour of day this baseline applies to (0-23), or null for overall baseline.
     * Used to capture hourly patterns such as peak business hours.
     */
    private Integer hourOfDay;

    /**
     * Day of week this baseline applies to (0-6 where Sunday=0), or null for overall baseline.
     * Used to capture weekly patterns such as weekday vs weekend behaviour.
     */
    private Integer dayOfWeek;

    /** Timestamp when these statistics were calculated. */
    private Instant calculatedAt;

    /** Start of the time period from which samples were collected. */
    private Instant periodStart;

    /** End of the time period from which samples were collected. */
    private Instant periodEnd;

    /**
     * Constructs an empty metric baseline.
     * All fields are initialised to their default values.
     */
    public MetricBaseline() {
    }

    /**
     * Constructs a metric baseline with core identifying attributes.
     *
     * @param instanceId the PostgreSQL instance identifier, must not be null
     * @param metricName the name of the metric being measured, must not be null
     * @param category the category classifying this metric, must not be null
     */
    public MetricBaseline(String instanceId, String metricName, Category category) {
        this.instanceId = instanceId;
        this.metricName = metricName;
        this.category = category;
    }

    /**
     * Calculates how many standard deviations a value is from the mean (z-score).
     * <p>
     * This method computes the standardised score which indicates how unusual a value is
     * relative to the baseline. A z-score of 0 means the value equals the mean, positive
     * scores indicate values above the mean, and negative scores indicate values below.
     * <p>
     * If the standard deviation is zero (all values in the baseline are identical),
     * returns 0 to avoid division by zero.
     *
     * @param value the metric value to evaluate
     * @return the number of standard deviations from the mean (positive = above mean,
     *         negative = below mean, 0 = at mean or when stddev is zero)
     */
    public double calculateSigma(double value) {
        if (stddev == 0) {
            return 0;
        }
        return (value - mean) / stddev;
    }

    /**
     * Determines if a value is anomalous based on a sigma threshold.
     * <p>
     * A value is considered anomalous if its absolute z-score exceeds the specified
     * threshold. For example, with a threshold of 3.0, values more than 3 standard
     * deviations above or below the mean are flagged as anomalous.
     * <p>
     * Common thresholds:
     * <ul>
     *   <li>2.0 sigma - catches roughly 95% of normal variation (5% false positive rate)</li>
     *   <li>3.0 sigma - catches roughly 99.7% of normal variation (0.3% false positive rate)</li>
     *   <li>4.0 sigma - very conservative, flags only extreme outliers</li>
     * </ul>
     *
     * @param value the metric value to evaluate
     * @param sigmaThreshold the number of standard deviations beyond which a value is anomalous
     * @return true if the value's absolute z-score exceeds the threshold, false otherwise
     */
    public boolean isAnomaly(double value, double sigmaThreshold) {
        return Math.abs(calculateSigma(value)) > sigmaThreshold;
    }

    /**
     * Returns a formatted display name describing when this baseline applies.
     * <p>
     * The format varies based on the time segmentation:
     * <ul>
     *   <li>"Overall" - baseline applies to all times (no segmentation)</li>
     *   <li>"Monday 09:00-09:59" - specific hour on specific day</li>
     *   <li>"Monday" - entire day (all hours)</li>
     *   <li>"09:00-09:59" - specific hour across all days</li>
     * </ul>
     *
     * @return human-readable description of the baseline's temporal context, never null
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

    /**
     * Returns the database-generated unique identifier.
     *
     * @return the baseline ID, or null if not yet persisted
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the database-generated unique identifier.
     *
     * @param id the baseline ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns the PostgreSQL instance identifier this baseline applies to.
     *
     * @return the instance identifier, or null if not set
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * Sets the PostgreSQL instance identifier.
     *
     * @param instanceId the instance identifier
     */
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    /**
     * Returns the name of the metric being measured.
     *
     * @return the metric name (e.g., "connections", "cache_hit_ratio"), or null if not set
     */
    public String getMetricName() {
        return metricName;
    }

    /**
     * Sets the name of the metric being measured.
     *
     * @param metricName the metric name
     */
    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    /**
     * Returns the category classifying this metric's type.
     *
     * @return the metric category, or null if not set
     */
    public Category getCategory() {
        return category;
    }

    /**
     * Sets the category classifying this metric's type.
     *
     * @param category the metric category
     */
    public void setCategory(Category category) {
        this.category = category;
    }

    /**
     * Returns the arithmetic mean of all sampled values.
     *
     * @return the mean value
     */
    public double getMean() {
        return mean;
    }

    /**
     * Sets the arithmetic mean of sampled values.
     *
     * @param mean the mean value to set
     */
    public void setMean(double mean) {
        this.mean = mean;
    }

    /**
     * Returns the standard deviation measuring variability from the mean.
     *
     * @return the standard deviation
     */
    public double getStddev() {
        return stddev;
    }

    /**
     * Sets the standard deviation measuring variability.
     *
     * @param stddev the standard deviation to set
     */
    public void setStddev(double stddev) {
        this.stddev = stddev;
    }

    /**
     * Returns the minimum observed value in the sample period.
     *
     * @return the minimum value, or null if not calculated
     */
    public Double getMin() {
        return min;
    }

    /**
     * Sets the minimum observed value.
     *
     * @param min the minimum value, or null if not applicable
     */
    public void setMin(Double min) {
        this.min = min;
    }

    /**
     * Returns the maximum observed value in the sample period.
     *
     * @return the maximum value, or null if not calculated
     */
    public Double getMax() {
        return max;
    }

    /**
     * Sets the maximum observed value.
     *
     * @param max the maximum value, or null if not applicable
     */
    public void setMax(Double max) {
        this.max = max;
    }

    /**
     * Returns the median value (50th percentile).
     *
     * @return the median, or null if not calculated
     */
    public Double getMedian() {
        return median;
    }

    /**
     * Sets the median value (50th percentile).
     *
     * @param median the median value, or null if not applicable
     */
    public void setMedian(Double median) {
        this.median = median;
    }

    /**
     * Returns the 95th percentile value.
     * <p>
     * This represents the value below which 95% of observations fall,
     * useful for understanding typical upper bounds of normal behaviour.
     *
     * @return the 95th percentile, or null if not calculated
     */
    public Double getP95() {
        return p95;
    }

    /**
     * Sets the 95th percentile value.
     *
     * @param p95 the 95th percentile value, or null if not applicable
     */
    public void setP95(Double p95) {
        this.p95 = p95;
    }

    /**
     * Returns the 99th percentile value.
     * <p>
     * This represents the value below which 99% of observations fall,
     * useful for identifying extreme but still normal values.
     *
     * @return the 99th percentile, or null if not calculated
     */
    public Double getP99() {
        return p99;
    }

    /**
     * Sets the 99th percentile value.
     *
     * @param p99 the 99th percentile value, or null if not applicable
     */
    public void setP99(Double p99) {
        this.p99 = p99;
    }

    /**
     * Returns the number of data points used to calculate these statistics.
     * <p>
     * A higher sample count generally indicates more reliable statistics.
     * Very low sample counts (e.g., less than 30) may produce unreliable baselines.
     *
     * @return the number of samples, zero if not yet calculated
     */
    public int getSampleCount() {
        return sampleCount;
    }

    /**
     * Sets the number of data points used in statistical calculations.
     *
     * @param sampleCount the number of samples, must be non-negative
     */
    public void setSampleCount(int sampleCount) {
        this.sampleCount = sampleCount;
    }

    /**
     * Returns the hour of day this baseline applies to (0-23).
     * <p>
     * When not null, this baseline represents statistics for a specific hour across
     * multiple days. For example, hourOfDay=9 represents data from 09:00-09:59 each day.
     *
     * @return the hour (0-23), or null if this baseline applies to all hours
     */
    public Integer getHourOfDay() {
        return hourOfDay;
    }

    /**
     * Sets the hour of day for time-segmented baselines.
     *
     * @param hourOfDay the hour (0-23), or null for baselines covering all hours
     */
    public void setHourOfDay(Integer hourOfDay) {
        this.hourOfDay = hourOfDay;
    }

    /**
     * Returns the day of week this baseline applies to (0-6 where Sunday=0).
     * <p>
     * When not null, this baseline represents statistics for a specific day of the week.
     * The numbering follows the convention: 0=Sunday, 1=Monday, ..., 6=Saturday.
     *
     * @return the day of week (0-6), or null if this baseline applies to all days
     */
    public Integer getDayOfWeek() {
        return dayOfWeek;
    }

    /**
     * Sets the day of week for time-segmented baselines.
     *
     * @param dayOfWeek the day of week (0-6 where Sunday=0), or null for baselines covering all days
     */
    public void setDayOfWeek(Integer dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    /**
     * Returns the timestamp when these statistics were calculated.
     *
     * @return the calculation timestamp, or null if not set
     */
    public Instant getCalculatedAt() {
        return calculatedAt;
    }

    /**
     * Sets the timestamp when statistics were calculated.
     *
     * @param calculatedAt the calculation timestamp
     */
    public void setCalculatedAt(Instant calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    /**
     * Returns the start of the time period from which samples were collected.
     *
     * @return the period start timestamp, or null if not set
     */
    public Instant getPeriodStart() {
        return periodStart;
    }

    /**
     * Sets the start of the sampling period.
     *
     * @param periodStart the period start timestamp
     */
    public void setPeriodStart(Instant periodStart) {
        this.periodStart = periodStart;
    }

    /**
     * Returns the end of the time period from which samples were collected.
     *
     * @return the period end timestamp, or null if not set
     */
    public Instant getPeriodEnd() {
        return periodEnd;
    }

    /**
     * Sets the end of the sampling period.
     *
     * @param periodEnd the period end timestamp
     */
    public void setPeriodEnd(Instant periodEnd) {
        this.periodEnd = periodEnd;
    }
}
