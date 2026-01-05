package com.bovinemagnet.pgconsole.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a PostgreSQL sequence schema definition.
 * <p>
 * Contains sequence properties for comparison including start value,
 * increment, min/max values, and cycle settings. This class is used
 * to capture sequence metadata for schema comparison and migration
 * analysis.
 * <p>
 * Sequences in PostgreSQL are special single-row tables used to generate
 * unique numeric identifiers. They can be standalone or owned by a table
 * column (as with SERIAL or IDENTITY columns).
 *
 * @author Paul Snow
 * @since 0.0.0
 * @see AttributeDifference
 */
public class SequenceSchema {

    /** The schema name containing this sequence. */
    private String schemaName;

    /** The sequence name within the schema. */
    private String sequenceName;

    /** The database role that owns this sequence. */
    private String owner;

    /** Optional comment/description for this sequence. May be null. */
    private String comment;

    /** The data type of the sequence: smallint, integer, or bigint. */
    private String dataType;

    /** The initial value of the sequence. */
    private long startValue;

    /** The increment value added to the current sequence value. */
    private long increment;

    /** The minimum value the sequence can generate. */
    private long minValue;

    /** The maximum value the sequence can generate. */
    private long maxValue;

    /** Number of sequence values to pre-allocate and cache in memory. */
    private long cacheSize;

    /** Whether the sequence cycles back to minValue after reaching maxValue. */
    private boolean cycle;

    /** The table name that owns this sequence, if any. Null for standalone sequences. */
    private String ownedByTable;

    /** The column name that owns this sequence, if any. Null for standalone sequences. */
    private String ownedByColumn;

    /**
     * Creates a builder for constructing SequenceSchema instances.
     * <p>
     * The builder pattern provides a fluent API for setting sequence properties.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing SequenceSchema instances with a fluent API.
     * <p>
     * Example usage:
     * <pre>{@code
     * SequenceSchema seq = SequenceSchema.builder()
     *     .schemaName("public")
     *     .sequenceName("users_id_seq")
     *     .dataType("bigint")
     *     .startValue(1)
     *     .increment(1)
     *     .build();
     * }</pre>
     */
    public static class Builder {
        private final SequenceSchema seq = new SequenceSchema();

        /**
         * Sets the schema name.
         *
         * @param schemaName the schema name containing the sequence
         * @return this builder for method chaining
         */
        public Builder schemaName(String schemaName) { seq.schemaName = schemaName; return this; }

        /**
         * Sets the sequence name.
         *
         * @param sequenceName the sequence name within the schema
         * @return this builder for method chaining
         */
        public Builder sequenceName(String sequenceName) { seq.sequenceName = sequenceName; return this; }

        /**
         * Sets the sequence owner.
         *
         * @param owner the database role that owns this sequence
         * @return this builder for method chaining
         */
        public Builder owner(String owner) { seq.owner = owner; return this; }

        /**
         * Sets the sequence comment.
         *
         * @param comment optional comment/description, may be null
         * @return this builder for method chaining
         */
        public Builder comment(String comment) { seq.comment = comment; return this; }

        /**
         * Sets the sequence data type.
         *
         * @param dataType the data type: smallint, integer, or bigint
         * @return this builder for method chaining
         */
        public Builder dataType(String dataType) { seq.dataType = dataType; return this; }

        /**
         * Sets the sequence start value.
         *
         * @param startValue the initial value of the sequence
         * @return this builder for method chaining
         */
        public Builder startValue(long startValue) { seq.startValue = startValue; return this; }

        /**
         * Sets the sequence increment.
         *
         * @param increment the value added to the current sequence value
         * @return this builder for method chaining
         */
        public Builder increment(long increment) { seq.increment = increment; return this; }

        /**
         * Sets the sequence minimum value.
         *
         * @param minValue the minimum value the sequence can generate
         * @return this builder for method chaining
         */
        public Builder minValue(long minValue) { seq.minValue = minValue; return this; }

        /**
         * Sets the sequence maximum value.
         *
         * @param maxValue the maximum value the sequence can generate
         * @return this builder for method chaining
         */
        public Builder maxValue(long maxValue) { seq.maxValue = maxValue; return this; }

        /**
         * Sets the sequence cache size.
         *
         * @param cacheSize number of sequence values to pre-allocate in memory
         * @return this builder for method chaining
         */
        public Builder cacheSize(long cacheSize) { seq.cacheSize = cacheSize; return this; }

        /**
         * Sets whether the sequence cycles.
         *
         * @param cycle true if sequence should cycle back to minValue after reaching maxValue
         * @return this builder for method chaining
         */
        public Builder cycle(boolean cycle) { seq.cycle = cycle; return this; }

        /**
         * Sets the table that owns this sequence.
         *
         * @param ownedByTable the owning table name, null for standalone sequences
         * @return this builder for method chaining
         */
        public Builder ownedByTable(String ownedByTable) { seq.ownedByTable = ownedByTable; return this; }

        /**
         * Sets the column that owns this sequence.
         *
         * @param ownedByColumn the owning column name, null for standalone sequences
         * @return this builder for method chaining
         */
        public Builder ownedByColumn(String ownedByColumn) { seq.ownedByColumn = ownedByColumn; return this; }

        /**
         * Builds the SequenceSchema instance.
         *
         * @return the constructed SequenceSchema
         */
        public SequenceSchema build() { return seq; }
    }

    /**
     * Constructs an empty SequenceSchema.
     * <p>
     * Fields can be populated using setters or the builder pattern.
     */
    public SequenceSchema() {
    }

    /**
     * Constructs a SequenceSchema with schema and sequence names.
     *
     * @param schemaName the schema name containing the sequence
     * @param sequenceName the sequence name within the schema
     */
    public SequenceSchema(String schemaName, String sequenceName) {
        this.schemaName = schemaName;
        this.sequenceName = sequenceName;
    }

    /**
     * Returns the fully qualified sequence name.
     * <p>
     * Concatenates schema name and sequence name with a dot separator.
     *
     * @return the fully qualified name in the format "schema.sequence"
     */
    public String getFullName() {
        return schemaName + "." + sequenceName;
    }

    /**
     * Checks whether this sequence is owned by a table column.
     * <p>
     * Sequences are owned by columns when created as part of SERIAL or
     * IDENTITY column definitions. Standalone sequences are not owned
     * by any column.
     *
     * @return true if both ownedByTable and ownedByColumn are non-null,
     *         false for standalone sequences
     */
    public boolean isOwnedByColumn() {
        return ownedByTable != null && ownedByColumn != null;
    }

    /**
     * Returns a display string for the owning column.
     * <p>
     * Concatenates the owning table and column names with a dot separator.
     *
     * @return the owning column in the format "table.column", or null if
     *         the sequence is not owned by a column
     */
    public String getOwnedByDisplay() {
        if (ownedByTable == null || ownedByColumn == null) {
            return null;
        }
        return ownedByTable + "." + ownedByColumn;
    }

    /**
     * Compares the structural properties of this sequence with another.
     * <p>
     * Structural equality considers data type, start value, increment,
     * min/max values, cache size, and cycle settings. It does not compare
     * schema name, sequence name, owner, comment, or ownership information.
     * <p>
     * This method is useful for detecting schema drift between environments.
     *
     * @param other the sequence to compare with, may be null
     * @return true if all structural properties match, false otherwise
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
     * Identifies structural differences between this sequence and another.
     * <p>
     * Compares data type, start value, increment, min/max values, cache size,
     * and cycle settings. Returns a list of {@link AttributeDifference} objects
     * describing each detected difference.
     * <p>
     * Data type differences are marked as breaking changes, as they may require
     * data migration or cause application compatibility issues.
     *
     * @param other the sequence to compare with, may be null
     * @return a list of differences, empty if sequences are structurally identical
     * @see AttributeDifference
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

    /**
     * Returns the schema name containing this sequence.
     *
     * @return the schema name
     */
    public String getSchemaName() {
        return schemaName;
    }

    /**
     * Sets the schema name containing this sequence.
     *
     * @param schemaName the schema name
     */
    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    /**
     * Returns the sequence name within the schema.
     *
     * @return the sequence name
     */
    public String getSequenceName() {
        return sequenceName;
    }

    /**
     * Sets the sequence name within the schema.
     *
     * @param sequenceName the sequence name
     */
    public void setSequenceName(String sequenceName) {
        this.sequenceName = sequenceName;
    }

    /**
     * Returns the database role that owns this sequence.
     *
     * @return the owner role name
     */
    public String getOwner() {
        return owner;
    }

    /**
     * Sets the database role that owns this sequence.
     *
     * @param owner the owner role name
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * Returns the optional comment or description for this sequence.
     *
     * @return the comment, or null if no comment is set
     */
    public String getComment() {
        return comment;
    }

    /**
     * Sets the optional comment or description for this sequence.
     *
     * @param comment the comment, may be null
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Returns the data type of the sequence.
     *
     * @return the data type: smallint, integer, or bigint
     */
    public String getDataType() {
        return dataType;
    }

    /**
     * Sets the data type of the sequence.
     *
     * @param dataType the data type: smallint, integer, or bigint
     */
    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    /**
     * Returns the initial value of the sequence.
     *
     * @return the start value
     */
    public long getStartValue() {
        return startValue;
    }

    /**
     * Sets the initial value of the sequence.
     *
     * @param startValue the start value
     */
    public void setStartValue(long startValue) {
        this.startValue = startValue;
    }

    /**
     * Returns the increment value added to the current sequence value.
     *
     * @return the increment, typically 1 or -1
     */
    public long getIncrement() {
        return increment;
    }

    /**
     * Sets the increment value added to the current sequence value.
     *
     * @param increment the increment, typically 1 or -1
     */
    public void setIncrement(long increment) {
        this.increment = increment;
    }

    /**
     * Returns the minimum value the sequence can generate.
     *
     * @return the minimum value
     */
    public long getMinValue() {
        return minValue;
    }

    /**
     * Sets the minimum value the sequence can generate.
     *
     * @param minValue the minimum value
     */
    public void setMinValue(long minValue) {
        this.minValue = minValue;
    }

    /**
     * Returns the maximum value the sequence can generate.
     *
     * @return the maximum value
     */
    public long getMaxValue() {
        return maxValue;
    }

    /**
     * Sets the maximum value the sequence can generate.
     *
     * @param maxValue the maximum value
     */
    public void setMaxValue(long maxValue) {
        this.maxValue = maxValue;
    }

    /**
     * Returns the number of sequence values to pre-allocate and cache in memory.
     * <p>
     * Caching improves performance by reducing the number of disk writes needed
     * when generating sequence values.
     *
     * @return the cache size
     */
    public long getCacheSize() {
        return cacheSize;
    }

    /**
     * Sets the number of sequence values to pre-allocate and cache in memory.
     *
     * @param cacheSize the cache size, must be positive
     */
    public void setCacheSize(long cacheSize) {
        this.cacheSize = cacheSize;
    }

    /**
     * Returns whether the sequence cycles back to the minimum value after
     * reaching the maximum value.
     *
     * @return true if the sequence cycles, false if it raises an error at max value
     */
    public boolean isCycle() {
        return cycle;
    }

    /**
     * Sets whether the sequence cycles back to the minimum value after
     * reaching the maximum value.
     *
     * @param cycle true to enable cycling, false to raise an error at max value
     */
    public void setCycle(boolean cycle) {
        this.cycle = cycle;
    }

    /**
     * Returns the table name that owns this sequence.
     *
     * @return the owning table name, or null for standalone sequences
     */
    public String getOwnedByTable() {
        return ownedByTable;
    }

    /**
     * Sets the table name that owns this sequence.
     *
     * @param ownedByTable the owning table name, null for standalone sequences
     */
    public void setOwnedByTable(String ownedByTable) {
        this.ownedByTable = ownedByTable;
    }

    /**
     * Returns the column name that owns this sequence.
     *
     * @return the owning column name, or null for standalone sequences
     */
    public String getOwnedByColumn() {
        return ownedByColumn;
    }

    /**
     * Sets the column name that owns this sequence.
     *
     * @param ownedByColumn the owning column name, null for standalone sequences
     */
    public void setOwnedByColumn(String ownedByColumn) {
        this.ownedByColumn = ownedByColumn;
    }

    /**
     * Sets both the owning table and column in a single operation.
     * <p>
     * This is a convenience method for sequences owned by table columns,
     * such as those created with SERIAL or IDENTITY column types.
     *
     * @param table the owning table name
     * @param column the owning column name
     */
    public void setOwnedBy(String table, String column) {
        this.ownedByTable = table;
        this.ownedByColumn = column;
    }
}
