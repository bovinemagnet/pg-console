package com.bovinemagnet.pgconsole.model;

/**
 * Represents a difference in a specific attribute of a database object.
 * <p>
 * Used to capture detailed attribute-level changes such as column data type
 * modifications, nullability changes, or default value differences.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class AttributeDifference {

    private String attributeName;
    private String sourceValue;
    private String destinationValue;
    private boolean breaking;
    private String description;

    public AttributeDifference() {
    }

    public AttributeDifference(String attributeName, String sourceValue, String destinationValue) {
        this.attributeName = attributeName;
        this.sourceValue = sourceValue;
        this.destinationValue = destinationValue;
        this.breaking = false;
    }

    public AttributeDifference(String attributeName, String sourceValue, String destinationValue, boolean breaking) {
        this.attributeName = attributeName;
        this.sourceValue = sourceValue;
        this.destinationValue = destinationValue;
        this.breaking = breaking;
    }

    /**
     * Creates a builder for constructing AttributeDifference instances.
     *
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets a display-friendly summary of this difference.
     *
     * @return summary text
     */
    public String getSummary() {
        return String.format("%s: %s -> %s", attributeName,
                sourceValue != null ? sourceValue : "(none)",
                destinationValue != null ? destinationValue : "(none)");
    }

    /**
     * Checks if the attribute was added (null in source, present in destination).
     *
     * @return true if attribute was added
     */
    public boolean isAdded() {
        return sourceValue == null && destinationValue != null;
    }

    /**
     * Checks if the attribute was removed (present in source, null in destination).
     *
     * @return true if attribute was removed
     */
    public boolean isRemoved() {
        return sourceValue != null && destinationValue == null;
    }

    /**
     * Checks if the attribute was modified (different values in both).
     *
     * @return true if attribute was modified
     */
    public boolean isModified() {
        return sourceValue != null && destinationValue != null
                && !sourceValue.equals(destinationValue);
    }

    /**
     * Gets CSS class for styling based on breaking status.
     *
     * @return CSS class name
     */
    public String getCssClass() {
        return breaking ? "table-danger" : "table-warning";
    }

    /**
     * Builder for AttributeDifference.
     */
    public static class Builder {
        private final AttributeDifference diff = new AttributeDifference();

        public Builder attributeName(String attributeName) {
            diff.attributeName = attributeName;
            return this;
        }

        public Builder sourceValue(String sourceValue) {
            diff.sourceValue = sourceValue;
            return this;
        }

        public Builder destinationValue(String destinationValue) {
            diff.destinationValue = destinationValue;
            return this;
        }

        public Builder breaking(boolean breaking) {
            diff.breaking = breaking;
            return this;
        }

        public Builder description(String description) {
            diff.description = description;
            return this;
        }

        public AttributeDifference build() {
            return diff;
        }
    }

    // Getters and Setters

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public String getSourceValue() {
        return sourceValue;
    }

    public void setSourceValue(String sourceValue) {
        this.sourceValue = sourceValue;
    }

    public String getDestinationValue() {
        return destinationValue;
    }

    public void setDestinationValue(String destinationValue) {
        this.destinationValue = destinationValue;
    }

    public boolean isBreaking() {
        return breaking;
    }

    public void setBreaking(boolean breaking) {
        this.breaking = breaking;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
