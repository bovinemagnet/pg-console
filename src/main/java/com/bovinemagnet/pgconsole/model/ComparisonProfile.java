package com.bovinemagnet.pgconsole.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Saved configuration for schema comparisons.
 * <p>
 * Allows users to save comparison settings for quick re-runs
 * and tracks last execution results.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class ComparisonProfile {

    private Long id;
    private String name;
    private String description;
    private String sourceInstance;
    private String destinationInstance;
    private String sourceSchema;
    private String destinationSchema;
    private ComparisonFilter filter;
    private boolean isDefault;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastRunAt;
    private SchemaComparisonResult.ComparisonSummary lastRunSummary;

    public ComparisonProfile() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.sourceSchema = "public";
        this.destinationSchema = "public";
    }

    public ComparisonProfile(String name, String sourceInstance, String destinationInstance) {
        this();
        this.name = name;
        this.sourceInstance = sourceInstance;
        this.destinationInstance = destinationInstance;
    }

    /**
     * Gets formatted creation timestamp.
     *
     * @return formatted date/time
     */
    public String getCreatedAtFormatted() {
        if (createdAt == null) return "-";
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(createdAt);
    }

    /**
     * Gets formatted last run timestamp.
     *
     * @return formatted date/time or "-"
     */
    public String getLastRunAtFormatted() {
        if (lastRunAt == null) return "Never";
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(lastRunAt);
    }

    /**
     * Gets a description of the comparison.
     *
     * @return description text
     */
    public String getComparisonDescription() {
        return String.format("%s.%s → %s.%s",
                sourceInstance, sourceSchema,
                destinationInstance, destinationSchema);
    }

    /**
     * Checks if the profile has been run before.
     *
     * @return true if lastRunAt is set
     */
    public boolean hasBeenRun() {
        return lastRunAt != null;
    }

    /**
     * Gets the last run summary text.
     *
     * @return summary text or null
     */
    public String getLastRunSummaryText() {
        if (lastRunSummary == null) return null;
        return lastRunSummary.getSummaryText();
    }

    /**
     * Updates the last run information.
     *
     * @param summary comparison summary
     */
    public void updateLastRun(SchemaComparisonResult.ComparisonSummary summary) {
        this.lastRunAt = Instant.now();
        this.lastRunSummary = summary;
        this.updatedAt = Instant.now();
    }

    /**
     * Creates a builder for constructing profiles.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ComparisonProfile profile = new ComparisonProfile();

        public Builder id(Long id) { profile.id = id; return this; }
        public Builder name(String name) { profile.name = name; return this; }
        public Builder description(String description) { profile.description = description; return this; }
        public Builder sourceInstance(String sourceInstance) { profile.sourceInstance = sourceInstance; return this; }
        public Builder destinationInstance(String destinationInstance) { profile.destinationInstance = destinationInstance; return this; }
        public Builder sourceSchema(String sourceSchema) { profile.sourceSchema = sourceSchema; return this; }
        public Builder destinationSchema(String destinationSchema) { profile.destinationSchema = destinationSchema; return this; }
        public Builder filter(ComparisonFilter filter) { profile.filter = filter; return this; }
        public Builder isDefault(boolean isDefault) { profile.isDefault = isDefault; return this; }
        public Builder createdBy(String createdBy) { profile.createdBy = createdBy; return this; }
        public ComparisonProfile build() { return profile; }
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSourceInstance() {
        return sourceInstance;
    }

    public void setSourceInstance(String sourceInstance) {
        this.sourceInstance = sourceInstance;
    }

    public String getDestinationInstance() {
        return destinationInstance;
    }

    public void setDestinationInstance(String destinationInstance) {
        this.destinationInstance = destinationInstance;
    }

    public String getSourceSchema() {
        return sourceSchema;
    }

    public void setSourceSchema(String sourceSchema) {
        this.sourceSchema = sourceSchema;
    }

    public String getDestinationSchema() {
        return destinationSchema;
    }

    public void setDestinationSchema(String destinationSchema) {
        this.destinationSchema = destinationSchema;
    }

    public ComparisonFilter getFilter() {
        return filter;
    }

    public void setFilter(ComparisonFilter filter) {
        this.filter = filter;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getLastRunAt() {
        return lastRunAt;
    }

    public void setLastRunAt(Instant lastRunAt) {
        this.lastRunAt = lastRunAt;
    }

    public SchemaComparisonResult.ComparisonSummary getLastRunSummary() {
        return lastRunSummary;
    }

    public void setLastRunSummary(SchemaComparisonResult.ComparisonSummary lastRunSummary) {
        this.lastRunSummary = lastRunSummary;
    }
}
