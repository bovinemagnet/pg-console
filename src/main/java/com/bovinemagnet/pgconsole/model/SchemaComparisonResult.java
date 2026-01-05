package com.bovinemagnet.pgconsole.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents the complete result of a schema comparison between two PostgreSQL database instances.
 * <p>
 * This class encapsulates all information about a schema comparison operation, including:
 * <ul>
 *   <li>Source and destination instance/schema identifiers</li>
 *   <li>Complete list of discovered differences (missing, extra, or modified objects)</li>
 *   <li>Summary statistics aggregated by object type and difference type</li>
 *   <li>Metadata about the comparison (timestamp, duration, performer)</li>
 *   <li>Error information if the comparison failed</li>
 * </ul>
 * <p>
 * Each comparison is assigned a unique UUID for tracking and reference purposes. The result
 * maintains both detailed differences and aggregated counts for efficient querying.
 * <p>
 * Thread-safety: This class is not thread-safe. Instances should not be shared across threads
 * without external synchronisation.
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see ObjectDifference
 * @see ComparisonFilter
 * @since 0.0.0
 */
public class SchemaComparisonResult {

    /**
     * Unique identifier for this comparison result.
     * Generated as a UUID when the result is created.
     */
    private String id;

    /**
     * Name of the source database instance being compared.
     * Typically represents the reference or baseline instance.
     */
    private String sourceInstance;

    /**
     * Name of the destination database instance being compared against.
     * Typically represents the target instance being validated.
     */
    private String destinationInstance;

    /**
     * Name of the schema within the source instance.
     */
    private String sourceSchema;

    /**
     * Name of the schema within the destination instance.
     */
    private String destinationSchema;

    /**
     * Timestamp when the comparison was performed.
     * Set to the current instant when the result object is created.
     */
    private Instant comparedAt;

    /**
     * Aggregate summary statistics for the comparison.
     * Tracks counts by object type and difference type.
     */
    private ComparisonSummary summary;

    /**
     * Complete list of differences discovered during comparison.
     * Each entry represents a missing, extra, or modified database object.
     */
    private List<ObjectDifference> differences = new ArrayList<>();

    /**
     * Optional filter that was applied during the comparison.
     * May be null if no filtering was used.
     */
    private ComparisonFilter filter;

    /**
     * Identifier of the user or system that performed the comparison.
     * May be null if not tracked.
     */
    private String performedBy;

    /**
     * Duration of the comparison operation in milliseconds.
     * Useful for performance monitoring and optimisation.
     */
    private long durationMillis;

    /**
     * Error message if the comparison failed.
     * Null or empty if the comparison completed successfully.
     */
    private String errorMessage;

    /**
     * Constructs a new schema comparison result with default initialisation.
     * <p>
     * Automatically generates a unique ID, sets the comparison timestamp to now,
     * and initialises an empty summary.
     */
    public SchemaComparisonResult() {
        this.id = UUID.randomUUID().toString();
        this.comparedAt = Instant.now();
        this.summary = new ComparisonSummary();
    }

    /**
     * Constructs a new schema comparison result with source and destination details.
     * <p>
     * This convenience constructor initialises the result with instance and schema
     * identifiers whilst delegating default initialisation to the no-arg constructor.
     *
     * @param sourceInstance      the name of the source database instance
     * @param destinationInstance the name of the destination database instance
     * @param sourceSchema        the name of the source schema
     * @param destinationSchema   the name of the destination schema
     */
    public SchemaComparisonResult(String sourceInstance, String destinationInstance,
                                   String sourceSchema, String destinationSchema) {
        this();
        this.sourceInstance = sourceInstance;
        this.destinationInstance = destinationInstance;
        this.sourceSchema = sourceSchema;
        this.destinationSchema = destinationSchema;
    }

    /**
     * Adds a difference to this comparison result and updates summary statistics.
     * <p>
     * This method automatically updates the appropriate counters in the summary
     * based on the difference type (MISSING, EXTRA, or MODIFIED).
     *
     * @param diff the object difference to add; must not be null
     * @see ComparisonSummary#updateCounts(ObjectDifference)
     */
    public void addDifference(ObjectDifference diff) {
        differences.add(diff);
        summary.updateCounts(diff);
    }

    /**
     * Determines whether the compared schemas are identical.
     * <p>
     * Schemas are considered identical when no differences of any kind have been
     * detected during the comparison.
     *
     * @return true if no differences were found; false otherwise
     */
    public boolean isIdentical() {
        return differences.isEmpty();
    }

    /**
     * Determines whether the comparison contains any breaking changes.
     * <p>
     * Breaking changes are differences marked with {@link ObjectDifference.Severity#BREAKING}
     * severity, indicating changes that could potentially break dependent applications
     * or queries.
     *
     * @return true if at least one breaking difference exists; false otherwise
     * @see ObjectDifference.Severity#BREAKING
     */
    public boolean hasBreakingChanges() {
        return differences.stream()
                .anyMatch(d -> d.getSeverity() == ObjectDifference.Severity.BREAKING);
    }

    /**
     * Retrieves all differences matching the specified severity level.
     * <p>
     * This method filters the complete list of differences to return only those
     * with the specified severity (BREAKING, WARNING, or INFO).
     *
     * @param severity the severity level to filter by; must not be null
     * @return an immutable list of differences with the specified severity; never null but may be empty
     * @see ObjectDifference.Severity
     */
    public List<ObjectDifference> getDifferencesBySeverity(ObjectDifference.Severity severity) {
        return differences.stream()
                .filter(d -> d.getSeverity() == severity)
                .toList();
    }

    /**
     * Retrieves all differences for the specified database object type.
     * <p>
     * This method filters the complete list of differences to return only those
     * affecting objects of the specified type (TABLE, INDEX, VIEW, etc.).
     *
     * @param objectType the database object type to filter by; must not be null
     * @return an immutable list of differences for the specified object type; never null but may be empty
     * @see ObjectDifference.ObjectType
     */
    public List<ObjectDifference> getDifferencesByType(ObjectDifference.ObjectType objectType) {
        return differences.stream()
                .filter(d -> d.getObjectType() == objectType)
                .toList();
    }

    /**
     * Retrieves all differences matching the specified difference type.
     * <p>
     * This method filters the complete list of differences to return only those
     * of the specified type (MISSING, EXTRA, or MODIFIED).
     *
     * @param differenceType the difference type to filter by; must not be null
     * @return an immutable list of differences with the specified type; never null but may be empty
     * @see ObjectDifference.DifferenceType
     */
    public List<ObjectDifference> getDifferencesByDiffType(ObjectDifference.DifferenceType differenceType) {
        return differences.stream()
                .filter(d -> d.getDifferenceType() == differenceType)
                .toList();
    }

    /**
     * Returns the comparison timestamp formatted for display.
     * <p>
     * The timestamp is formatted using the pattern "yyyy-MM-dd HH:mm:ss" in the
     * system default time zone.
     *
     * @return the formatted timestamp string; never null
     */
    public String getComparedAtFormatted() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(comparedAt);
    }

    /**
     * Returns a concise human-readable description of this comparison.
     * <p>
     * The description includes both source and destination identifiers in the
     * format "sourceInstance.sourceSchema vs destinationInstance.destinationSchema".
     *
     * @return a short description of what was compared; never null
     */
    public String getDescription() {
        return String.format("%s.%s vs %s.%s",
                sourceInstance, sourceSchema,
                destinationInstance, destinationSchema);
    }

    /**
     * Counts the number of differences marked as breaking changes.
     * <p>
     * Breaking changes indicate potentially incompatible schema modifications that
     * could affect existing applications or queries.
     *
     * @return the count of breaking severity differences; zero or greater
     * @see ObjectDifference.Severity#BREAKING
     */
    public long getBreakingCount() {
        return differences.stream()
                .filter(d -> d.getSeverity() == ObjectDifference.Severity.BREAKING)
                .count();
    }

    /**
     * Counts the number of differences marked as warnings.
     * <p>
     * Warning-level changes indicate potentially problematic schema modifications that
     * warrant attention but are not necessarily breaking.
     *
     * @return the count of warning severity differences; zero or greater
     * @see ObjectDifference.Severity#WARNING
     */
    public long getWarningCount() {
        return differences.stream()
                .filter(d -> d.getSeverity() == ObjectDifference.Severity.WARNING)
                .count();
    }

    /**
     * Counts the number of differences marked as informational.
     * <p>
     * Informational changes are benign differences that are unlikely to cause
     * issues but are worth noting.
     *
     * @return the count of info severity differences; zero or greater
     * @see ObjectDifference.Severity#INFO
     */
    public long getInfoCount() {
        return differences.stream()
                .filter(d -> d.getSeverity() == ObjectDifference.Severity.INFO)
                .count();
    }

    /**
     * Aggregate summary statistics for a schema comparison result.
     * <p>
     * This inner class maintains counts of differences grouped by both difference type
     * (missing, extra, modified) and by database object type (tables, indexes, views, etc.).
     * It provides convenience methods for computing totals and generating display-friendly
     * summaries.
     * <p>
     * The class supports automatic updating via {@link #updateCounts(ObjectDifference)} and
     * provides helper methods for template rendering (CSS classes, summary text).
     *
     * @see SchemaComparisonResult
     * @since 0.0.0
     */
    public static class ComparisonSummary {
        /** Number of objects present in source but missing from destination. */
        private int missingObjects;

        /** Number of objects present in destination but not in source. */
        private int extraObjects;

        /** Number of objects present in both but with differing definitions. */
        private int modifiedObjects;

        /** Number of objects that are identical between source and destination. */
        private int matchingObjects;

        /** Number of table objects compared. */
        private int tablesCompared;

        /** Number of index objects compared. */
        private int indexesCompared;

        /** Number of constraint objects compared. */
        private int constraintsCompared;

        /** Number of view objects compared. */
        private int viewsCompared;

        /** Number of function/procedure objects compared. */
        private int functionsCompared;

        /** Number of trigger objects compared. */
        private int triggersCompared;

        /** Number of sequence objects compared. */
        private int sequencesCompared;

        /** Number of type objects compared. */
        private int typesCompared;

        /** Number of extension objects compared. */
        private int extensionsCompared;

        /**
         * Updates the summary counts based on a newly added difference.
         * <p>
         * This method increments the appropriate counter (missing, extra, or modified)
         * based on the difference's type. It is automatically called by
         * {@link SchemaComparisonResult#addDifference(ObjectDifference)}.
         *
         * @param diff the difference to account for; must not be null
         */
        public void updateCounts(ObjectDifference diff) {
            switch (diff.getDifferenceType()) {
                case MISSING -> missingObjects++;
                case EXTRA -> extraObjects++;
                case MODIFIED -> modifiedObjects++;
            }
        }

        /**
         * Calculates the total number of differences across all types.
         * <p>
         * This is the sum of missing, extra, and modified objects. Matching objects
         * are excluded from this count.
         *
         * @return the total difference count; zero or greater
         */
        public int getTotalDifferences() {
            return missingObjects + extraObjects + modifiedObjects;
        }

        /**
         * Calculates the total number of database objects compared.
         * <p>
         * This is the sum of all object type counts (tables, indexes, views, etc.)
         * that were examined during the comparison.
         *
         * @return the total count of objects compared; zero or greater
         */
        public int getTotalCompared() {
            return tablesCompared + indexesCompared + constraintsCompared
                    + viewsCompared + functionsCompared + triggersCompared
                    + sequencesCompared + typesCompared + extensionsCompared;
        }

        /**
         * Determines whether there are any breaking changes in the summary.
         * <p>
         * Breaking changes are defined as any missing or modified objects, as these
         * could potentially impact dependent applications or queries.
         *
         * @return true if missing or modified objects exist; false otherwise
         */
        public boolean hasBreakingChanges() {
            return missingObjects > 0 || modifiedObjects > 0;
        }

        /**
         * Returns a Bootstrap CSS class name appropriate for displaying the summary.
         * <p>
         * The CSS class is determined by the comparison outcome:
         * <ul>
         *   <li>"bg-success" - schemas are identical (no differences)</li>
         *   <li>"bg-danger" - breaking changes exist</li>
         *   <li>"bg-warning" - only extra objects (non-breaking differences)</li>
         * </ul>
         *
         * @return a Bootstrap background colour class; never null
         */
        public String getSummaryCssClass() {
            if (getTotalDifferences() == 0) {
                return "bg-success";
            } else if (hasBreakingChanges()) {
                return "bg-danger";
            } else {
                return "bg-warning";
            }
        }

        /**
         * Returns a human-readable summary of the comparison results.
         * <p>
         * The summary text varies based on the comparison outcome. If schemas are
         * identical, returns "Schemas are identical". Otherwise, returns a formatted
         * string showing the total count and breakdown by difference type.
         *
         * @return a summary message suitable for display; never null
         */
        public String getSummaryText() {
            if (getTotalDifferences() == 0) {
                return "Schemas are identical";
            }
            return String.format("%d differences (%d missing, %d extra, %d modified)",
                    getTotalDifferences(), missingObjects, extraObjects, modifiedObjects);
        }

        // Getters and Setters

        /**
         * Returns the count of objects present in source but missing from destination.
         *
         * @return the missing objects count; zero or greater
         */
        public int getMissingObjects() { return missingObjects; }

        /**
         * Sets the count of missing objects.
         *
         * @param missingObjects the missing objects count; should be zero or greater
         */
        public void setMissingObjects(int missingObjects) { this.missingObjects = missingObjects; }

        /**
         * Returns the count of objects present in destination but not in source.
         *
         * @return the extra objects count; zero or greater
         */
        public int getExtraObjects() { return extraObjects; }

        /**
         * Sets the count of extra objects.
         *
         * @param extraObjects the extra objects count; should be zero or greater
         */
        public void setExtraObjects(int extraObjects) { this.extraObjects = extraObjects; }

        /**
         * Returns the count of objects present in both schemas but with differing definitions.
         *
         * @return the modified objects count; zero or greater
         */
        public int getModifiedObjects() { return modifiedObjects; }

        /**
         * Sets the count of modified objects.
         *
         * @param modifiedObjects the modified objects count; should be zero or greater
         */
        public void setModifiedObjects(int modifiedObjects) { this.modifiedObjects = modifiedObjects; }

        /**
         * Returns the count of objects that are identical between source and destination.
         *
         * @return the matching objects count; zero or greater
         */
        public int getMatchingObjects() { return matchingObjects; }

        /**
         * Sets the count of matching objects.
         *
         * @param matchingObjects the matching objects count; should be zero or greater
         */
        public void setMatchingObjects(int matchingObjects) { this.matchingObjects = matchingObjects; }

        /**
         * Returns the count of table objects compared.
         *
         * @return the tables compared count; zero or greater
         */
        public int getTablesCompared() { return tablesCompared; }

        /**
         * Sets the count of table objects compared.
         *
         * @param tablesCompared the tables compared count; should be zero or greater
         */
        public void setTablesCompared(int tablesCompared) { this.tablesCompared = tablesCompared; }

        /**
         * Returns the count of index objects compared.
         *
         * @return the indexes compared count; zero or greater
         */
        public int getIndexesCompared() { return indexesCompared; }

        /**
         * Sets the count of index objects compared.
         *
         * @param indexesCompared the indexes compared count; should be zero or greater
         */
        public void setIndexesCompared(int indexesCompared) { this.indexesCompared = indexesCompared; }

        /**
         * Returns the count of constraint objects compared.
         *
         * @return the constraints compared count; zero or greater
         */
        public int getConstraintsCompared() { return constraintsCompared; }

        /**
         * Sets the count of constraint objects compared.
         *
         * @param constraintsCompared the constraints compared count; should be zero or greater
         */
        public void setConstraintsCompared(int constraintsCompared) { this.constraintsCompared = constraintsCompared; }

        /**
         * Returns the count of view objects compared.
         *
         * @return the views compared count; zero or greater
         */
        public int getViewsCompared() { return viewsCompared; }

        /**
         * Sets the count of view objects compared.
         *
         * @param viewsCompared the views compared count; should be zero or greater
         */
        public void setViewsCompared(int viewsCompared) { this.viewsCompared = viewsCompared; }

        /**
         * Returns the count of function/procedure objects compared.
         *
         * @return the functions compared count; zero or greater
         */
        public int getFunctionsCompared() { return functionsCompared; }

        /**
         * Sets the count of function/procedure objects compared.
         *
         * @param functionsCompared the functions compared count; should be zero or greater
         */
        public void setFunctionsCompared(int functionsCompared) { this.functionsCompared = functionsCompared; }

        /**
         * Returns the count of trigger objects compared.
         *
         * @return the triggers compared count; zero or greater
         */
        public int getTriggersCompared() { return triggersCompared; }

        /**
         * Sets the count of trigger objects compared.
         *
         * @param triggersCompared the triggers compared count; should be zero or greater
         */
        public void setTriggersCompared(int triggersCompared) { this.triggersCompared = triggersCompared; }

        /**
         * Returns the count of sequence objects compared.
         *
         * @return the sequences compared count; zero or greater
         */
        public int getSequencesCompared() { return sequencesCompared; }

        /**
         * Sets the count of sequence objects compared.
         *
         * @param sequencesCompared the sequences compared count; should be zero or greater
         */
        public void setSequencesCompared(int sequencesCompared) { this.sequencesCompared = sequencesCompared; }

        /**
         * Returns the count of type objects compared.
         *
         * @return the types compared count; zero or greater
         */
        public int getTypesCompared() { return typesCompared; }

        /**
         * Sets the count of type objects compared.
         *
         * @param typesCompared the types compared count; should be zero or greater
         */
        public void setTypesCompared(int typesCompared) { this.typesCompared = typesCompared; }

        /**
         * Returns the count of extension objects compared.
         *
         * @return the extensions compared count; zero or greater
         */
        public int getExtensionsCompared() { return extensionsCompared; }

        /**
         * Sets the count of extension objects compared.
         *
         * @param extensionsCompared the extensions compared count; should be zero or greater
         */
        public void setExtensionsCompared(int extensionsCompared) { this.extensionsCompared = extensionsCompared; }

        /**
         * Increments the count of table objects compared by one.
         */
        public void incrementTablesCompared() { tablesCompared++; }

        /**
         * Increments the count of index objects compared by one.
         */
        public void incrementIndexesCompared() { indexesCompared++; }

        /**
         * Increments the count of constraint objects compared by one.
         */
        public void incrementConstraintsCompared() { constraintsCompared++; }

        /**
         * Increments the count of view objects compared by one.
         */
        public void incrementViewsCompared() { viewsCompared++; }

        /**
         * Increments the count of function/procedure objects compared by one.
         */
        public void incrementFunctionsCompared() { functionsCompared++; }

        /**
         * Increments the count of trigger objects compared by one.
         */
        public void incrementTriggersCompared() { triggersCompared++; }

        /**
         * Increments the count of sequence objects compared by one.
         */
        public void incrementSequencesCompared() { sequencesCompared++; }

        /**
         * Increments the count of type objects compared by one.
         */
        public void incrementTypesCompared() { typesCompared++; }

        /**
         * Increments the count of extension objects compared by one.
         */
        public void incrementExtensionsCompared() { extensionsCompared++; }

        /**
         * Increments the count of matching objects by one.
         */
        public void incrementMatchingObjects() { matchingObjects++; }
    }

    // Getters and Setters

    /**
     * Returns the unique identifier for this comparison result.
     *
     * @return the comparison ID; never null
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the unique identifier for this comparison result.
     *
     * @param id the comparison ID; should not be null
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the name of the source database instance.
     *
     * @return the source instance name; may be null
     */
    public String getSourceInstance() {
        return sourceInstance;
    }

    /**
     * Sets the name of the source database instance.
     *
     * @param sourceInstance the source instance name
     */
    public void setSourceInstance(String sourceInstance) {
        this.sourceInstance = sourceInstance;
    }

    /**
     * Returns the name of the destination database instance.
     *
     * @return the destination instance name; may be null
     */
    public String getDestinationInstance() {
        return destinationInstance;
    }

    /**
     * Sets the name of the destination database instance.
     *
     * @param destinationInstance the destination instance name
     */
    public void setDestinationInstance(String destinationInstance) {
        this.destinationInstance = destinationInstance;
    }

    /**
     * Returns the name of the source schema.
     *
     * @return the source schema name; may be null
     */
    public String getSourceSchema() {
        return sourceSchema;
    }

    /**
     * Sets the name of the source schema.
     *
     * @param sourceSchema the source schema name
     */
    public void setSourceSchema(String sourceSchema) {
        this.sourceSchema = sourceSchema;
    }

    /**
     * Returns the name of the destination schema.
     *
     * @return the destination schema name; may be null
     */
    public String getDestinationSchema() {
        return destinationSchema;
    }

    /**
     * Sets the name of the destination schema.
     *
     * @param destinationSchema the destination schema name
     */
    public void setDestinationSchema(String destinationSchema) {
        this.destinationSchema = destinationSchema;
    }

    /**
     * Returns the timestamp when the comparison was performed.
     *
     * @return the comparison timestamp; never null
     */
    public Instant getComparedAt() {
        return comparedAt;
    }

    /**
     * Sets the timestamp when the comparison was performed.
     *
     * @param comparedAt the comparison timestamp; should not be null
     */
    public void setComparedAt(Instant comparedAt) {
        this.comparedAt = comparedAt;
    }

    /**
     * Returns the aggregate summary statistics for this comparison.
     *
     * @return the comparison summary; never null
     */
    public ComparisonSummary getSummary() {
        return summary;
    }

    /**
     * Sets the aggregate summary statistics for this comparison.
     *
     * @param summary the comparison summary; should not be null
     */
    public void setSummary(ComparisonSummary summary) {
        this.summary = summary;
    }

    /**
     * Returns the complete list of differences discovered during comparison.
     *
     * @return the list of differences; never null but may be empty
     */
    public List<ObjectDifference> getDifferences() {
        return differences;
    }

    /**
     * Sets the complete list of differences.
     * <p>
     * Note: This replaces the entire list. Consider using {@link #addDifference(ObjectDifference)}
     * to add individual differences whilst maintaining summary statistics.
     *
     * @param differences the list of differences; should not be null
     */
    public void setDifferences(List<ObjectDifference> differences) {
        this.differences = differences;
    }

    /**
     * Returns the filter that was applied during the comparison.
     *
     * @return the comparison filter; may be null if no filtering was used
     */
    public ComparisonFilter getFilter() {
        return filter;
    }

    /**
     * Sets the filter that was applied during the comparison.
     *
     * @param filter the comparison filter; may be null
     */
    public void setFilter(ComparisonFilter filter) {
        this.filter = filter;
    }

    /**
     * Returns the identifier of the user or system that performed the comparison.
     *
     * @return the performer identifier; may be null if not tracked
     */
    public String getPerformedBy() {
        return performedBy;
    }

    /**
     * Sets the identifier of the user or system that performed the comparison.
     *
     * @param performedBy the performer identifier
     */
    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    /**
     * Returns the duration of the comparison operation in milliseconds.
     *
     * @return the duration in milliseconds; zero or greater
     */
    public long getDurationMillis() {
        return durationMillis;
    }

    /**
     * Sets the duration of the comparison operation in milliseconds.
     *
     * @param durationMillis the duration in milliseconds; should be zero or greater
     */
    public void setDurationMillis(long durationMillis) {
        this.durationMillis = durationMillis;
    }

    /**
     * Determines whether the comparison completed successfully.
     * <p>
     * A comparison is considered successful if no error message has been set.
     *
     * @return true if the comparison succeeded without errors; false otherwise
     */
    public boolean isSuccess() {
        return errorMessage == null || errorMessage.isEmpty();
    }

    /**
     * Returns the error message if the comparison failed.
     * <p>
     * This value is null or empty if the comparison completed successfully.
     *
     * @return the error message; null if successful
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets an error message indicating the comparison failed.
     *
     * @param errorMessage the error message; null or empty indicates success
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Sets whether the comparison was successful.
     * <p>
     * If success is true, this clears any existing error message. If false,
     * you should separately set an appropriate error message via
     * {@link #setErrorMessage(String)}.
     *
     * @param success true to mark as successful; false otherwise
     */
    public void setSuccess(boolean success) {
        if (success) {
            this.errorMessage = null;
        }
    }
}
