package com.bovinemagnet.pgconsole.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Saved configuration for schema comparisons between PostgreSQL database instances.
 * <p>
 * A comparison profile encapsulates all settings required to perform a schema comparison,
 * including source and destination instance/schema pairs, optional filtering criteria,
 * and execution history. Profiles enable users to save frequently-used comparison
 * configurations for quick re-runs and maintain a historical record of comparison results.
 * <p>
 * Each profile tracks metadata such as creation time, creator, and the timestamp and
 * summary of the most recent comparison execution. Profiles can be marked as default
 * for automatic selection in the UI.
 * <p>
 * Example usage:
 * <pre>{@code
 * ComparisonProfile profile = ComparisonProfile.builder()
 *     .name("Production vs Staging")
 *     .sourceInstance("prod-db")
 *     .destinationInstance("staging-db")
 *     .sourceSchema("public")
 *     .destinationSchema("public")
 *     .filter(new ComparisonFilter())
 *     .createdBy("admin")
 *     .build();
 * }</pre>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see ComparisonFilter
 * @see SchemaComparisonResult.ComparisonSummary
 */
public class ComparisonProfile {

    /**
     * Unique identifier for this comparison profile.
     * May be null for transient instances not yet persisted.
     */
    private Long id;

    /**
     * User-defined name for this comparison profile.
     * Should be descriptive and unique for easy identification.
     */
    private String name;

    /**
     * Optional detailed description of the profile's purpose or usage context.
     */
    private String description;

    /**
     * Identifier of the source PostgreSQL database instance.
     * Typically matches a configured instance name in the application.
     */
    private String sourceInstance;

    /**
     * Identifier of the destination PostgreSQL database instance.
     * Typically matches a configured instance name in the application.
     */
    private String destinationInstance;

    /**
     * Schema name in the source database to compare.
     * Defaults to "public" if not specified.
     */
    private String sourceSchema;

    /**
     * Schema name in the destination database to compare.
     * Defaults to "public" if not specified.
     */
    private String destinationSchema;

    /**
     * Optional filter configuration to restrict which database objects are compared.
     * May be null to compare all objects without filtering.
     */
    private ComparisonFilter filter;

    /**
     * Indicates whether this profile should be selected by default in the UI.
     * Only one profile should be marked as default.
     */
    private boolean isDefault;

    /**
     * Username or identifier of the user who created this profile.
     */
    private String createdBy;

    /**
     * Timestamp when this profile was created.
     * Automatically set to current time in the default constructor.
     */
    private Instant createdAt;

    /**
     * Timestamp when this profile was last modified.
     * Automatically updated when the profile is saved or last run information is updated.
     */
    private Instant updatedAt;

    /**
     * Timestamp of the most recent comparison execution using this profile.
     * Null if the profile has never been executed.
     */
    private Instant lastRunAt;

    /**
     * Summary statistics from the most recent comparison execution.
     * Null if the profile has never been executed.
     */
    private SchemaComparisonResult.ComparisonSummary lastRunSummary;

    /**
     * Constructs a new comparison profile with default values.
     * <p>
     * Initialises creation and update timestamps to the current time,
     * and sets both source and destination schemas to "public".
     */
    public ComparisonProfile() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.sourceSchema = "public";
        this.destinationSchema = "public";
    }

    /**
     * Constructs a new comparison profile with the specified name and instances.
     * <p>
     * This is a convenience constructor that delegates to the default constructor
     * and then sets the name and instance identifiers. Source and destination schemas
     * will default to "public".
     *
     * @param name the user-defined name for this profile
     * @param sourceInstance the identifier of the source database instance
     * @param destinationInstance the identifier of the destination database instance
     */
    public ComparisonProfile(String name, String sourceInstance, String destinationInstance) {
        this();
        this.name = name;
        this.sourceInstance = sourceInstance;
        this.destinationInstance = destinationInstance;
    }

    /**
     * Returns the creation timestamp formatted for display.
     * <p>
     * Uses the pattern "yyyy-MM-dd HH:mm" in the system default time zone.
     *
     * @return formatted creation date and time, or "-" if creation timestamp is null
     */
    public String getCreatedAtFormatted() {
        if (createdAt == null) return "-";
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(createdAt);
    }

    /**
     * Returns the last run timestamp formatted for display.
     * <p>
     * Uses the pattern "yyyy-MM-dd HH:mm" in the system default time zone.
     *
     * @return formatted last run date and time, or "Never" if the profile has not been executed
     */
    public String getLastRunAtFormatted() {
        if (lastRunAt == null) return "Never";
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(lastRunAt);
    }

    /**
     * Returns a human-readable description of the comparison direction and schemas.
     * <p>
     * The format is: {@code sourceInstance.sourceSchema → destinationInstance.destinationSchema}
     * <p>
     * Example output: {@code prod-db.public → staging-db.public}
     *
     * @return formatted comparison description showing source and destination
     */
    public String getComparisonDescription() {
        return String.format("%s.%s → %s.%s",
                sourceInstance, sourceSchema,
                destinationInstance, destinationSchema);
    }

    /**
     * Determines whether this profile has been executed at least once.
     *
     * @return true if the profile has been run (lastRunAt is not null), false otherwise
     */
    public boolean hasBeenRun() {
        return lastRunAt != null;
    }

    /**
     * Returns the summary text from the most recent comparison execution.
     * <p>
     * The summary text typically contains a count of differences found during the comparison.
     *
     * @return summary text from the last run, or null if the profile has never been executed
     * @see SchemaComparisonResult.ComparisonSummary#getSummaryText()
     */
    public String getLastRunSummaryText() {
        if (lastRunSummary == null) return null;
        return lastRunSummary.getSummaryText();
    }

    /**
     * Updates this profile with the results of a comparison execution.
     * <p>
     * Sets the last run timestamp to the current time, stores the provided summary,
     * and updates the profile's modification timestamp. This method should be called
     * immediately after a comparison completes.
     *
     * @param summary the comparison summary containing statistics about the comparison results
     */
    public void updateLastRun(SchemaComparisonResult.ComparisonSummary summary) {
        this.lastRunAt = Instant.now();
        this.lastRunSummary = summary;
        this.updatedAt = Instant.now();
    }

    /**
     * Creates a new builder for constructing comparison profiles.
     * <p>
     * The builder pattern provides a fluent API for setting profile properties
     * and is the recommended approach for creating profiles with multiple attributes.
     *
     * @return a new Builder instance
     * @see Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ComparisonProfile} instances with a fluent API.
     * <p>
     * The builder creates a new profile with default values and allows selective
     * customisation of properties. Defaults from the {@link ComparisonProfile#ComparisonProfile()}
     * constructor are automatically applied.
     * <p>
     * Example usage:
     * <pre>{@code
     * ComparisonProfile profile = ComparisonProfile.builder()
     *     .name("Production vs Staging")
     *     .sourceInstance("prod-db")
     *     .destinationInstance("staging-db")
     *     .isDefault(true)
     *     .createdBy("admin")
     *     .build();
     * }</pre>
     *
     * @see ComparisonProfile#builder()
     */
    public static class Builder {
        private final ComparisonProfile profile = new ComparisonProfile();

        /**
         * Sets the unique identifier for the profile.
         *
         * @param id the profile ID
         * @return this builder for method chaining
         */
        public Builder id(Long id) { profile.id = id; return this; }

        /**
         * Sets the user-defined name for the profile.
         *
         * @param name the profile name
         * @return this builder for method chaining
         */
        public Builder name(String name) { profile.name = name; return this; }

        /**
         * Sets the optional description for the profile.
         *
         * @param description the profile description
         * @return this builder for method chaining
         */
        public Builder description(String description) { profile.description = description; return this; }

        /**
         * Sets the source database instance identifier.
         *
         * @param sourceInstance the source instance name
         * @return this builder for method chaining
         */
        public Builder sourceInstance(String sourceInstance) { profile.sourceInstance = sourceInstance; return this; }

        /**
         * Sets the destination database instance identifier.
         *
         * @param destinationInstance the destination instance name
         * @return this builder for method chaining
         */
        public Builder destinationInstance(String destinationInstance) { profile.destinationInstance = destinationInstance; return this; }

        /**
         * Sets the source schema name.
         * <p>
         * Defaults to "public" if not specified.
         *
         * @param sourceSchema the source schema name
         * @return this builder for method chaining
         */
        public Builder sourceSchema(String sourceSchema) { profile.sourceSchema = sourceSchema; return this; }

        /**
         * Sets the destination schema name.
         * <p>
         * Defaults to "public" if not specified.
         *
         * @param destinationSchema the destination schema name
         * @return this builder for method chaining
         */
        public Builder destinationSchema(String destinationSchema) { profile.destinationSchema = destinationSchema; return this; }

        /**
         * Sets the comparison filter configuration.
         *
         * @param filter the filter to apply during comparison, or null for no filtering
         * @return this builder for method chaining
         */
        public Builder filter(ComparisonFilter filter) { profile.filter = filter; return this; }

        /**
         * Sets whether this profile should be the default selection.
         *
         * @param isDefault true to mark as default, false otherwise
         * @return this builder for method chaining
         */
        public Builder isDefault(boolean isDefault) { profile.isDefault = isDefault; return this; }

        /**
         * Sets the creator username or identifier.
         *
         * @param createdBy the username of the profile creator
         * @return this builder for method chaining
         */
        public Builder createdBy(String createdBy) { profile.createdBy = createdBy; return this; }

        /**
         * Constructs the comparison profile with the configured properties.
         *
         * @return the constructed ComparisonProfile instance
         */
        public ComparisonProfile build() { return profile; }
    }

    // Getters and Setters

    /**
     * Returns the unique identifier for this profile.
     *
     * @return the profile ID, or null if not yet persisted
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the unique identifier for this profile.
     *
     * @param id the profile ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns the user-defined name for this profile.
     *
     * @return the profile name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the user-defined name for this profile.
     *
     * @param name the profile name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the optional description for this profile.
     *
     * @return the profile description, or null if not set
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the optional description for this profile.
     *
     * @param description the profile description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the source database instance identifier.
     *
     * @return the source instance name
     */
    public String getSourceInstance() {
        return sourceInstance;
    }

    /**
     * Sets the source database instance identifier.
     *
     * @param sourceInstance the source instance name
     */
    public void setSourceInstance(String sourceInstance) {
        this.sourceInstance = sourceInstance;
    }

    /**
     * Returns the destination database instance identifier.
     *
     * @return the destination instance name
     */
    public String getDestinationInstance() {
        return destinationInstance;
    }

    /**
     * Sets the destination database instance identifier.
     *
     * @param destinationInstance the destination instance name
     */
    public void setDestinationInstance(String destinationInstance) {
        this.destinationInstance = destinationInstance;
    }

    /**
     * Returns the source schema name.
     *
     * @return the source schema name
     */
    public String getSourceSchema() {
        return sourceSchema;
    }

    /**
     * Sets the source schema name.
     *
     * @param sourceSchema the source schema name
     */
    public void setSourceSchema(String sourceSchema) {
        this.sourceSchema = sourceSchema;
    }

    /**
     * Returns the destination schema name.
     *
     * @return the destination schema name
     */
    public String getDestinationSchema() {
        return destinationSchema;
    }

    /**
     * Sets the destination schema name.
     *
     * @param destinationSchema the destination schema name
     */
    public void setDestinationSchema(String destinationSchema) {
        this.destinationSchema = destinationSchema;
    }

    /**
     * Returns the comparison filter configuration.
     *
     * @return the filter, or null if no filtering is applied
     */
    public ComparisonFilter getFilter() {
        return filter;
    }

    /**
     * Sets the comparison filter configuration.
     *
     * @param filter the filter to apply, or null to disable filtering
     */
    public void setFilter(ComparisonFilter filter) {
        this.filter = filter;
    }

    /**
     * Determines whether this profile is marked as the default.
     *
     * @return true if this is the default profile, false otherwise
     */
    public boolean isDefault() {
        return isDefault;
    }

    /**
     * Sets whether this profile should be marked as the default.
     *
     * @param aDefault true to mark as default, false otherwise
     */
    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    /**
     * Returns the username or identifier of the profile creator.
     *
     * @return the creator username
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Sets the username or identifier of the profile creator.
     *
     * @param createdBy the creator username
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Returns the timestamp when this profile was created.
     *
     * @return the creation timestamp
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the timestamp when this profile was created.
     *
     * @param createdAt the creation timestamp
     */
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Returns the timestamp when this profile was last modified.
     *
     * @return the last update timestamp
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the timestamp when this profile was last modified.
     *
     * @param updatedAt the last update timestamp
     */
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Returns the timestamp of the most recent comparison execution.
     *
     * @return the last run timestamp, or null if never executed
     */
    public Instant getLastRunAt() {
        return lastRunAt;
    }

    /**
     * Sets the timestamp of the most recent comparison execution.
     *
     * @param lastRunAt the last run timestamp
     */
    public void setLastRunAt(Instant lastRunAt) {
        this.lastRunAt = lastRunAt;
    }

    /**
     * Returns the summary from the most recent comparison execution.
     *
     * @return the comparison summary, or null if never executed
     */
    public SchemaComparisonResult.ComparisonSummary getLastRunSummary() {
        return lastRunSummary;
    }

    /**
     * Sets the summary from the most recent comparison execution.
     *
     * @param lastRunSummary the comparison summary
     */
    public void setLastRunSummary(SchemaComparisonResult.ComparisonSummary lastRunSummary) {
        this.lastRunSummary = lastRunSummary;
    }
}
