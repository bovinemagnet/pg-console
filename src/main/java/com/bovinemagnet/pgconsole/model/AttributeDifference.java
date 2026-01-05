package com.bovinemagnet.pgconsole.model;

/**
 * Represents a difference in a specific attribute of a database object.
 * <p>
 * This class captures detailed attribute-level changes when comparing database schemas,
 * such as column data type modifications, nullability changes, or default value differences.
 * Each instance represents a single attribute change, tracking both the source (original) and
 * destination (modified) values, along with metadata indicating whether the change is
 * breaking (backward-incompatible) and an optional human-readable description.
 * </p>
 * <p>
 * The class provides convenience methods for determining the nature of the change:
 * </p>
 * <ul>
 *   <li>{@link #isAdded()} - Attribute exists only in destination</li>
 *   <li>{@link #isRemoved()} - Attribute exists only in source</li>
 *   <li>{@link #isModified()} - Attribute exists in both with different values</li>
 * </ul>
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * AttributeDifference diff = AttributeDifference.builder()
 *     .attributeName("data_type")
 *     .sourceValue("VARCHAR(50)")
 *     .destinationValue("TEXT")
 *     .breaking(false)
 *     .description("Column type widened for flexibility")
 *     .build();
 *
 * if (diff.isModified() && diff.isBreaking()) {
 *     logger.warn("Breaking change detected: " + diff.getSummary());
 * }
 * }</pre>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see DatabaseDifference
 */
public class AttributeDifference {

    /**
     * The name of the attribute that differs (e.g., "data_type", "nullable", "default_value").
     */
    private String attributeName;

    /**
     * The value of the attribute in the source (original) schema.
     * Null indicates the attribute did not exist in the source.
     */
    private String sourceValue;

    /**
     * The value of the attribute in the destination (modified) schema.
     * Null indicates the attribute does not exist in the destination.
     */
    private String destinationValue;

    /**
     * Indicates whether this difference represents a breaking (backward-incompatible) change.
     * Breaking changes typically require migration scripts or application updates.
     */
    private boolean breaking;

    /**
     * An optional human-readable description providing context or explanation for the difference.
     */
    private String description;

    /**
     * Constructs an empty AttributeDifference instance.
     * Primarily used by frameworks for deserialisation or by the builder pattern.
     */
    public AttributeDifference() {
    }

    /**
     * Constructs an AttributeDifference with the specified attribute name and values.
     * The change is marked as non-breaking by default.
     *
     * @param attributeName the name of the attribute that differs
     * @param sourceValue the value in the source schema, or null if the attribute was added
     * @param destinationValue the value in the destination schema, or null if the attribute was removed
     */
    public AttributeDifference(String attributeName, String sourceValue, String destinationValue) {
        this.attributeName = attributeName;
        this.sourceValue = sourceValue;
        this.destinationValue = destinationValue;
        this.breaking = false;
    }

    /**
     * Constructs an AttributeDifference with the specified attribute name, values, and breaking status.
     *
     * @param attributeName the name of the attribute that differs
     * @param sourceValue the value in the source schema, or null if the attribute was added
     * @param destinationValue the value in the destination schema, or null if the attribute was removed
     * @param breaking true if this change is backward-incompatible, false otherwise
     */
    public AttributeDifference(String attributeName, String sourceValue, String destinationValue, boolean breaking) {
        this.attributeName = attributeName;
        this.sourceValue = sourceValue;
        this.destinationValue = destinationValue;
        this.breaking = breaking;
    }

    /**
     * Creates a builder for constructing AttributeDifference instances.
     * The builder pattern provides a fluent API for setting optional fields.
     *
     * @return a new builder instance
     * @see Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a display-friendly summary of this difference in the format "attributeName: sourceValue -&gt; destinationValue".
     * Null values are displayed as "(none)" for clarity.
     *
     * @return a formatted summary string, never null
     */
    public String getSummary() {
        return String.format("%s: %s -> %s", attributeName,
                sourceValue != null ? sourceValue : "(none)",
                destinationValue != null ? destinationValue : "(none)");
    }

    /**
     * Determines whether this difference represents an added attribute.
     * An attribute is considered added when it exists in the destination schema but not in the source.
     *
     * @return true if the attribute was added, false otherwise
     * @see #isRemoved()
     * @see #isModified()
     */
    public boolean isAdded() {
        return sourceValue == null && destinationValue != null;
    }

    /**
     * Determines whether this difference represents a removed attribute.
     * An attribute is considered removed when it exists in the source schema but not in the destination.
     *
     * @return true if the attribute was removed, false otherwise
     * @see #isAdded()
     * @see #isModified()
     */
    public boolean isRemoved() {
        return sourceValue != null && destinationValue == null;
    }

    /**
     * Determines whether this difference represents a modified attribute.
     * An attribute is considered modified when it exists in both schemas with different values.
     *
     * @return true if the attribute was modified, false otherwise
     * @see #isAdded()
     * @see #isRemoved()
     */
    public boolean isModified() {
        return sourceValue != null && destinationValue != null
                && !sourceValue.equals(destinationValue);
    }

    /**
     * Returns the appropriate Bootstrap CSS class for styling this difference in HTML tables.
     * Breaking changes are styled with "table-danger" (typically red),
     * while non-breaking changes use "table-warning" (typically yellow/orange).
     *
     * @return "table-danger" if breaking, "table-warning" otherwise
     */
    public String getCssClass() {
        return breaking ? "table-danger" : "table-warning";
    }

    /**
     * Builder for constructing {@link AttributeDifference} instances with a fluent API.
     * <p>
     * This builder enables optional field configuration and improves readability when
     * creating instances with multiple fields. All setter methods return the builder
     * instance to enable method chaining.
     * </p>
     * <p>
     * Example:
     * </p>
     * <pre>{@code
     * AttributeDifference diff = AttributeDifference.builder()
     *     .attributeName("column_type")
     *     .sourceValue("INTEGER")
     *     .destinationValue("BIGINT")
     *     .breaking(false)
     *     .description("Widened integer type to support larger values")
     *     .build();
     * }</pre>
     */
    public static class Builder {
        private final AttributeDifference diff = new AttributeDifference();

        /**
         * Sets the attribute name.
         *
         * @param attributeName the name of the attribute that differs
         * @return this builder instance for method chaining
         */
        public Builder attributeName(String attributeName) {
            diff.attributeName = attributeName;
            return this;
        }

        /**
         * Sets the source value.
         *
         * @param sourceValue the value in the source schema, or null if the attribute was added
         * @return this builder instance for method chaining
         */
        public Builder sourceValue(String sourceValue) {
            diff.sourceValue = sourceValue;
            return this;
        }

        /**
         * Sets the destination value.
         *
         * @param destinationValue the value in the destination schema, or null if the attribute was removed
         * @return this builder instance for method chaining
         */
        public Builder destinationValue(String destinationValue) {
            diff.destinationValue = destinationValue;
            return this;
        }

        /**
         * Sets whether this difference represents a breaking change.
         *
         * @param breaking true if this change is backward-incompatible, false otherwise
         * @return this builder instance for method chaining
         */
        public Builder breaking(boolean breaking) {
            diff.breaking = breaking;
            return this;
        }

        /**
         * Sets the human-readable description.
         *
         * @param description an optional explanation or context for this difference
         * @return this builder instance for method chaining
         */
        public Builder description(String description) {
            diff.description = description;
            return this;
        }

        /**
         * Builds and returns the configured AttributeDifference instance.
         *
         * @return the constructed AttributeDifference
         */
        public AttributeDifference build() {
            return diff;
        }
    }

    // Getters and Setters

    /**
     * Returns the name of the attribute that differs.
     *
     * @return the attribute name, may be null
     */
    public String getAttributeName() {
        return attributeName;
    }

    /**
     * Sets the name of the attribute that differs.
     *
     * @param attributeName the attribute name to set
     */
    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    /**
     * Returns the value of the attribute in the source schema.
     *
     * @return the source value, or null if the attribute was added
     */
    public String getSourceValue() {
        return sourceValue;
    }

    /**
     * Sets the value of the attribute in the source schema.
     *
     * @param sourceValue the source value to set, or null if the attribute was added
     */
    public void setSourceValue(String sourceValue) {
        this.sourceValue = sourceValue;
    }

    /**
     * Returns the value of the attribute in the destination schema.
     *
     * @return the destination value, or null if the attribute was removed
     */
    public String getDestinationValue() {
        return destinationValue;
    }

    /**
     * Sets the value of the attribute in the destination schema.
     *
     * @param destinationValue the destination value to set, or null if the attribute was removed
     */
    public void setDestinationValue(String destinationValue) {
        this.destinationValue = destinationValue;
    }

    /**
     * Determines whether this difference represents a breaking change.
     *
     * @return true if the change is backward-incompatible, false otherwise
     */
    public boolean isBreaking() {
        return breaking;
    }

    /**
     * Sets whether this difference represents a breaking change.
     *
     * @param breaking true if the change is backward-incompatible, false otherwise
     */
    public void setBreaking(boolean breaking) {
        this.breaking = breaking;
    }

    /**
     * Returns the human-readable description of this difference.
     *
     * @return the description, may be null
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the human-readable description of this difference.
     *
     * @param description an optional explanation or context for this difference
     */
    public void setDescription(String description) {
        this.description = description;
    }
}
