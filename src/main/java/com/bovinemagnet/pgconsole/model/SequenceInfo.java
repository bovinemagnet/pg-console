package com.bovinemagnet.pgconsole.model;

import java.math.BigInteger;

/**
 * Represents a sequence from the PostgreSQL system catalog {@code pg_sequences}.
 * <p>
 * This class captures information about sequences including their schema, name,
 * data type, current value, and computed usage percentage to warn about
 * potential exhaustion.
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see <a href="https://www.postgresql.org/docs/current/view-pg-sequences.html">pg_sequences Documentation</a>
 */
public class SequenceInfo {

    /** Schema name containing the sequence */
    private String schemaName;

    /** Name of the sequence */
    private String sequenceName;

    /** Owner of the sequence */
    private String sequenceOwner;

    /** Data type of the sequence (smallint, integer, bigint) */
    private String dataType;

    /** Start value of the sequence */
    private BigInteger startValue;

    /** Minimum value of the sequence */
    private BigInteger minValue;

    /** Maximum value of the sequence */
    private BigInteger maxValue;

    /** Increment value */
    private BigInteger incrementBy;

    /** Whether the sequence cycles */
    private boolean cycle;

    /** Cache size */
    private long cacheSize;

    /** Last value returned by the sequence (null if never used) */
    private BigInteger lastValue;

    /** Computed usage percentage (0-100) */
    private double usagePercent;

    /**
     * Constructs a new SequenceInfo instance.
     */
    public SequenceInfo() {
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getSequenceName() {
        return sequenceName;
    }

    public void setSequenceName(String sequenceName) {
        this.sequenceName = sequenceName;
    }

    public String getSequenceOwner() {
        return sequenceOwner;
    }

    public void setSequenceOwner(String sequenceOwner) {
        this.sequenceOwner = sequenceOwner;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public BigInteger getStartValue() {
        return startValue;
    }

    public void setStartValue(BigInteger startValue) {
        this.startValue = startValue;
    }

    public BigInteger getMinValue() {
        return minValue;
    }

    public void setMinValue(BigInteger minValue) {
        this.minValue = minValue;
    }

    public BigInteger getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(BigInteger maxValue) {
        this.maxValue = maxValue;
    }

    public BigInteger getIncrementBy() {
        return incrementBy;
    }

    public void setIncrementBy(BigInteger incrementBy) {
        this.incrementBy = incrementBy;
    }

    public boolean isCycle() {
        return cycle;
    }

    public void setCycle(boolean cycle) {
        this.cycle = cycle;
    }

    public long getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(long cacheSize) {
        this.cacheSize = cacheSize;
    }

    public BigInteger getLastValue() {
        return lastValue;
    }

    public void setLastValue(BigInteger lastValue) {
        this.lastValue = lastValue;
    }

    public double getUsagePercent() {
        return usagePercent;
    }

    public void setUsagePercent(double usagePercent) {
        this.usagePercent = usagePercent;
    }

    /**
     * Calculates the usage percentage based on current value and range.
     */
    public void calculateUsagePercent() {
        if (lastValue == null || maxValue == null || minValue == null) {
            this.usagePercent = 0;
            return;
        }

        BigInteger range = maxValue.subtract(minValue);
        if (range.equals(BigInteger.ZERO)) {
            this.usagePercent = 100;
            return;
        }

        BigInteger used;
        if (incrementBy != null && incrementBy.compareTo(BigInteger.ZERO) > 0) {
            // Ascending sequence
            used = lastValue.subtract(minValue);
        } else {
            // Descending sequence
            used = maxValue.subtract(lastValue);
        }

        // Calculate percentage (multiply by 100 first for precision)
        BigInteger percent = used.multiply(BigInteger.valueOf(100)).divide(range);
        this.usagePercent = percent.doubleValue();
    }

    /**
     * Returns the fully qualified sequence name.
     *
     * @return schema.sequence format
     */
    public String getFullName() {
        if (schemaName != null && !schemaName.isEmpty()) {
            return schemaName + "." + sequenceName;
        }
        return sequenceName;
    }

    /**
     * Returns the usage percentage formatted.
     *
     * @return formatted usage percentage
     */
    public String getUsagePercentFormatted() {
        return String.format("%.1f%%", usagePercent);
    }

    /**
     * Returns whether the sequence is near exhaustion (over 80% used).
     *
     * @return true if usage is critical
     */
    public boolean isCritical() {
        return usagePercent >= 80 && !cycle;
    }

    /**
     * Returns whether the sequence is getting full (over 50% used).
     *
     * @return true if usage is a warning
     */
    public boolean isWarning() {
        return usagePercent >= 50 && usagePercent < 80 && !cycle;
    }

    /**
     * Returns whether the sequence has never been used.
     *
     * @return true if lastValue is null
     */
    public boolean isUnused() {
        return lastValue == null;
    }

    /**
     * Returns Bootstrap CSS class for the row based on usage.
     *
     * @return Bootstrap table row class
     */
    public String getRowCssClass() {
        if (cycle) return "";
        if (isCritical()) return "table-danger";
        if (isWarning()) return "table-warning";
        return "";
    }

    /**
     * Returns Bootstrap CSS class for the usage badge.
     *
     * @return Bootstrap background class
     */
    public String getUsageCssClass() {
        if (cycle) return "bg-info";
        if (isCritical()) return "bg-danger";
        if (isWarning()) return "bg-warning text-dark";
        return "bg-success";
    }

    /**
     * Returns Bootstrap CSS class for the progress bar.
     *
     * @return Bootstrap background class
     */
    public String getProgressBarCssClass() {
        if (cycle) return "bg-info";
        if (isCritical()) return "bg-danger";
        if (isWarning()) return "bg-warning";
        return "bg-success";
    }

    /**
     * Returns Bootstrap CSS class for the cycle badge.
     *
     * @return Bootstrap background class
     */
    public String getCycleCssClass() {
        return cycle ? "bg-success" : "bg-secondary";
    }

    /**
     * Returns Bootstrap CSS class for the data type badge.
     *
     * @return Bootstrap background class
     */
    public String getDataTypeCssClass() {
        if (dataType == null) return "bg-secondary";
        return switch (dataType.toLowerCase()) {
            case "bigint" -> "bg-success";
            case "integer" -> "bg-info";
            case "smallint" -> "bg-warning text-dark";
            default -> "bg-secondary";
        };
    }

    /**
     * Returns the last value formatted, handling null case.
     *
     * @return formatted last value or "Never used"
     */
    public String getLastValueDisplay() {
        if (lastValue == null) return "Never used";
        return lastValue.toString();
    }
}
