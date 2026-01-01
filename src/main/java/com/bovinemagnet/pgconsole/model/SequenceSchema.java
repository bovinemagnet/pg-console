package com.bovinemagnet.pgconsole.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a sequence schema definition.
 * <p>
 * Contains sequence properties for comparison including start value,
 * increment, min/max values, and cycle settings.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class SequenceSchema {

    private String schemaName;
    private String sequenceName;
    private String owner;
    private String comment;
    private String dataType; // smallint, integer, bigint
    private long startValue;
    private long increment;
    private long minValue;
    private long maxValue;
    private long cacheSize;
    private boolean cycle;
    private String ownedByTable;
    private String ownedByColumn;

    /**
     * Creates a builder for SequenceSchema.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for SequenceSchema.
     */
    public static class Builder {
        private final SequenceSchema seq = new SequenceSchema();

        public Builder schemaName(String schemaName) { seq.schemaName = schemaName; return this; }
        public Builder sequenceName(String sequenceName) { seq.sequenceName = sequenceName; return this; }
        public Builder owner(String owner) { seq.owner = owner; return this; }
        public Builder comment(String comment) { seq.comment = comment; return this; }
        public Builder dataType(String dataType) { seq.dataType = dataType; return this; }
        public Builder startValue(long startValue) { seq.startValue = startValue; return this; }
        public Builder increment(long increment) { seq.increment = increment; return this; }
        public Builder minValue(long minValue) { seq.minValue = minValue; return this; }
        public Builder maxValue(long maxValue) { seq.maxValue = maxValue; return this; }
        public Builder cacheSize(long cacheSize) { seq.cacheSize = cacheSize; return this; }
        public Builder cycle(boolean cycle) { seq.cycle = cycle; return this; }
        public Builder ownedByTable(String ownedByTable) { seq.ownedByTable = ownedByTable; return this; }
        public Builder ownedByColumn(String ownedByColumn) { seq.ownedByColumn = ownedByColumn; return this; }
        public SequenceSchema build() { return seq; }
    }

    public SequenceSchema() {
    }

    public SequenceSchema(String schemaName, String sequenceName) {
        this.schemaName = schemaName;
        this.sequenceName = sequenceName;
    }

    /**
     * Gets the fully qualified sequence name.
     *
     * @return schema.sequence name
     */
    public String getFullName() {
        return schemaName + "." + sequenceName;
    }

    /**
     * Checks if this sequence is owned by a column (identity or serial).
     *
     * @return true if owned by a column
     */
    public boolean isOwnedByColumn() {
        return ownedByTable != null && ownedByColumn != null;
    }

    /**
     * Gets the owning column display text.
     *
     * @return table.column or null
     */
    public String getOwnedByDisplay() {
        if (ownedByTable == null || ownedByColumn == null) {
            return null;
        }
        return ownedByTable + "." + ownedByColumn;
    }

    /**
     * Checks if this sequence equals another for comparison purposes.
     *
     * @param other other sequence
     * @return true if structurally equal
     */
    public boolean equalsStructure(SequenceSchema other) {
        if (other == null) return false;
        return Objects.equals(dataType, other.dataType)
                && startValue == other.startValue
                && increment == other.increment
                && minValue == other.minValue
                && maxValue == other.maxValue
                && cacheSize == other.cacheSize
                && cycle == other.cycle;
    }

    /**
     * Gets differences from another sequence.
     *
     * @param other other sequence
     * @return list of differences
     */
    public List<AttributeDifference> getDifferencesFrom(SequenceSchema other) {
        List<AttributeDifference> diffs = new ArrayList<>();
        if (other == null) return diffs;

        if (!Objects.equals(dataType, other.dataType)) {
            diffs.add(new AttributeDifference("Data Type", dataType, other.dataType, true));
        }
        if (startValue != other.startValue) {
            diffs.add(new AttributeDifference("Start Value",
                    String.valueOf(startValue), String.valueOf(other.startValue), false));
        }
        if (increment != other.increment) {
            diffs.add(new AttributeDifference("Increment",
                    String.valueOf(increment), String.valueOf(other.increment), false));
        }
        if (minValue != other.minValue) {
            diffs.add(new AttributeDifference("Min Value",
                    String.valueOf(minValue), String.valueOf(other.minValue), false));
        }
        if (maxValue != other.maxValue) {
            diffs.add(new AttributeDifference("Max Value",
                    String.valueOf(maxValue), String.valueOf(other.maxValue), false));
        }
        if (cacheSize != other.cacheSize) {
            diffs.add(new AttributeDifference("Cache Size",
                    String.valueOf(cacheSize), String.valueOf(other.cacheSize), false));
        }
        if (cycle != other.cycle) {
            diffs.add(new AttributeDifference("Cycle",
                    cycle ? "YES" : "NO", other.cycle ? "YES" : "NO", false));
        }

        return diffs;
    }

    // Getters and Setters

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

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public long getStartValue() {
        return startValue;
    }

    public void setStartValue(long startValue) {
        this.startValue = startValue;
    }

    public long getIncrement() {
        return increment;
    }

    public void setIncrement(long increment) {
        this.increment = increment;
    }

    public long getMinValue() {
        return minValue;
    }

    public void setMinValue(long minValue) {
        this.minValue = minValue;
    }

    public long getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(long maxValue) {
        this.maxValue = maxValue;
    }

    public long getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(long cacheSize) {
        this.cacheSize = cacheSize;
    }

    public boolean isCycle() {
        return cycle;
    }

    public void setCycle(boolean cycle) {
        this.cycle = cycle;
    }

    public String getOwnedByTable() {
        return ownedByTable;
    }

    public void setOwnedByTable(String ownedByTable) {
        this.ownedByTable = ownedByTable;
    }

    public String getOwnedByColumn() {
        return ownedByColumn;
    }

    public void setOwnedByColumn(String ownedByColumn) {
        this.ownedByColumn = ownedByColumn;
    }

    /**
     * Sets both owned-by table and column at once.
     *
     * @param table the owning table name
     * @param column the owning column name
     */
    public void setOwnedBy(String table, String column) {
        this.ownedByTable = table;
        this.ownedByColumn = column;
    }
}
