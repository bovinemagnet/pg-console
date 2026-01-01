package com.bovinemagnet.pgconsole.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents the complete result of a schema comparison between two instances.
 * <p>
 * Contains all differences found, summary statistics, and metadata about
 * the comparison operation.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class SchemaComparisonResult {

    private String id;
    private String sourceInstance;
    private String destinationInstance;
    private String sourceSchema;
    private String destinationSchema;
    private Instant comparedAt;
    private ComparisonSummary summary;
    private List<ObjectDifference> differences = new ArrayList<>();
    private ComparisonFilter filter;
    private String performedBy;
    private long durationMillis;
    private String errorMessage;

    public SchemaComparisonResult() {
        this.id = UUID.randomUUID().toString();
        this.comparedAt = Instant.now();
        this.summary = new ComparisonSummary();
    }

    public SchemaComparisonResult(String sourceInstance, String destinationInstance,
                                   String sourceSchema, String destinationSchema) {
        this();
        this.sourceInstance = sourceInstance;
        this.destinationInstance = destinationInstance;
        this.sourceSchema = sourceSchema;
        this.destinationSchema = destinationSchema;
    }

    /**
     * Adds a difference and updates summary statistics.
     *
     * @param diff the difference to add
     */
    public void addDifference(ObjectDifference diff) {
        differences.add(diff);
        summary.updateCounts(diff);
    }

    /**
     * Checks if the schemas are identical.
     *
     * @return true if no differences found
     */
    public boolean isIdentical() {
        return differences.isEmpty();
    }

    /**
     * Checks if there are any breaking changes.
     *
     * @return true if any breaking differences exist
     */
    public boolean hasBreakingChanges() {
        return differences.stream()
                .anyMatch(d -> d.getSeverity() == ObjectDifference.Severity.BREAKING);
    }

    /**
     * Gets differences filtered by severity.
     *
     * @param severity severity to filter by
     * @return filtered list
     */
    public List<ObjectDifference> getDifferencesBySeverity(ObjectDifference.Severity severity) {
        return differences.stream()
                .filter(d -> d.getSeverity() == severity)
                .toList();
    }

    /**
     * Gets differences filtered by object type.
     *
     * @param objectType type to filter by
     * @return filtered list
     */
    public List<ObjectDifference> getDifferencesByType(ObjectDifference.ObjectType objectType) {
        return differences.stream()
                .filter(d -> d.getObjectType() == objectType)
                .toList();
    }

    /**
     * Gets differences filtered by difference type.
     *
     * @param differenceType type to filter by
     * @return filtered list
     */
    public List<ObjectDifference> getDifferencesByDiffType(ObjectDifference.DifferenceType differenceType) {
        return differences.stream()
                .filter(d -> d.getDifferenceType() == differenceType)
                .toList();
    }

    /**
     * Gets the formatted comparison timestamp.
     *
     * @return formatted date/time
     */
    public String getComparedAtFormatted() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(comparedAt);
    }

    /**
     * Gets a short description of the comparison.
     *
     * @return description
     */
    public String getDescription() {
        return String.format("%s.%s vs %s.%s",
                sourceInstance, sourceSchema,
                destinationInstance, destinationSchema);
    }

    /**
     * Gets count of breaking changes.
     *
     * @return breaking change count
     */
    public long getBreakingCount() {
        return differences.stream()
                .filter(d -> d.getSeverity() == ObjectDifference.Severity.BREAKING)
                .count();
    }

    /**
     * Gets count of warning-level changes.
     *
     * @return warning count
     */
    public long getWarningCount() {
        return differences.stream()
                .filter(d -> d.getSeverity() == ObjectDifference.Severity.WARNING)
                .count();
    }

    /**
     * Gets count of info-level changes.
     *
     * @return info count
     */
    public long getInfoCount() {
        return differences.stream()
                .filter(d -> d.getSeverity() == ObjectDifference.Severity.INFO)
                .count();
    }

    /**
     * Summary statistics for comparison results.
     */
    public static class ComparisonSummary {
        private int missingObjects;
        private int extraObjects;
        private int modifiedObjects;
        private int matchingObjects;
        private int tablesCompared;
        private int indexesCompared;
        private int constraintsCompared;
        private int viewsCompared;
        private int functionsCompared;
        private int triggersCompared;
        private int sequencesCompared;
        private int typesCompared;
        private int extensionsCompared;

        public void updateCounts(ObjectDifference diff) {
            switch (diff.getDifferenceType()) {
                case MISSING -> missingObjects++;
                case EXTRA -> extraObjects++;
                case MODIFIED -> modifiedObjects++;
            }
        }

        public int getTotalDifferences() {
            return missingObjects + extraObjects + modifiedObjects;
        }

        public int getTotalCompared() {
            return tablesCompared + indexesCompared + constraintsCompared
                    + viewsCompared + functionsCompared + triggersCompared
                    + sequencesCompared + typesCompared + extensionsCompared;
        }

        public boolean hasBreakingChanges() {
            return missingObjects > 0 || modifiedObjects > 0;
        }

        public String getSummaryCssClass() {
            if (getTotalDifferences() == 0) {
                return "bg-success";
            } else if (hasBreakingChanges()) {
                return "bg-danger";
            } else {
                return "bg-warning";
            }
        }

        public String getSummaryText() {
            if (getTotalDifferences() == 0) {
                return "Schemas are identical";
            }
            return String.format("%d differences (%d missing, %d extra, %d modified)",
                    getTotalDifferences(), missingObjects, extraObjects, modifiedObjects);
        }

        // Getters and Setters
        public int getMissingObjects() { return missingObjects; }
        public void setMissingObjects(int missingObjects) { this.missingObjects = missingObjects; }
        public int getExtraObjects() { return extraObjects; }
        public void setExtraObjects(int extraObjects) { this.extraObjects = extraObjects; }
        public int getModifiedObjects() { return modifiedObjects; }
        public void setModifiedObjects(int modifiedObjects) { this.modifiedObjects = modifiedObjects; }
        public int getMatchingObjects() { return matchingObjects; }
        public void setMatchingObjects(int matchingObjects) { this.matchingObjects = matchingObjects; }
        public int getTablesCompared() { return tablesCompared; }
        public void setTablesCompared(int tablesCompared) { this.tablesCompared = tablesCompared; }
        public int getIndexesCompared() { return indexesCompared; }
        public void setIndexesCompared(int indexesCompared) { this.indexesCompared = indexesCompared; }
        public int getConstraintsCompared() { return constraintsCompared; }
        public void setConstraintsCompared(int constraintsCompared) { this.constraintsCompared = constraintsCompared; }
        public int getViewsCompared() { return viewsCompared; }
        public void setViewsCompared(int viewsCompared) { this.viewsCompared = viewsCompared; }
        public int getFunctionsCompared() { return functionsCompared; }
        public void setFunctionsCompared(int functionsCompared) { this.functionsCompared = functionsCompared; }
        public int getTriggersCompared() { return triggersCompared; }
        public void setTriggersCompared(int triggersCompared) { this.triggersCompared = triggersCompared; }
        public int getSequencesCompared() { return sequencesCompared; }
        public void setSequencesCompared(int sequencesCompared) { this.sequencesCompared = sequencesCompared; }
        public int getTypesCompared() { return typesCompared; }
        public void setTypesCompared(int typesCompared) { this.typesCompared = typesCompared; }
        public int getExtensionsCompared() { return extensionsCompared; }
        public void setExtensionsCompared(int extensionsCompared) { this.extensionsCompared = extensionsCompared; }

        public void incrementTablesCompared() { tablesCompared++; }
        public void incrementIndexesCompared() { indexesCompared++; }
        public void incrementConstraintsCompared() { constraintsCompared++; }
        public void incrementViewsCompared() { viewsCompared++; }
        public void incrementFunctionsCompared() { functionsCompared++; }
        public void incrementTriggersCompared() { triggersCompared++; }
        public void incrementSequencesCompared() { sequencesCompared++; }
        public void incrementTypesCompared() { typesCompared++; }
        public void incrementExtensionsCompared() { extensionsCompared++; }
        public void incrementMatchingObjects() { matchingObjects++; }
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public Instant getComparedAt() {
        return comparedAt;
    }

    public void setComparedAt(Instant comparedAt) {
        this.comparedAt = comparedAt;
    }

    public ComparisonSummary getSummary() {
        return summary;
    }

    public void setSummary(ComparisonSummary summary) {
        this.summary = summary;
    }

    public List<ObjectDifference> getDifferences() {
        return differences;
    }

    public void setDifferences(List<ObjectDifference> differences) {
        this.differences = differences;
    }

    public ComparisonFilter getFilter() {
        return filter;
    }

    public void setFilter(ComparisonFilter filter) {
        this.filter = filter;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public void setDurationMillis(long durationMillis) {
        this.durationMillis = durationMillis;
    }

    /**
     * Checks if the comparison completed successfully.
     *
     * @return true if comparison succeeded without errors
     */
    public boolean isSuccess() {
        return errorMessage == null || errorMessage.isEmpty();
    }

    /**
     * Gets the error message if the comparison failed.
     *
     * @return error message or null if successful
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets an error message for failed comparisons.
     *
     * @param errorMessage the error message
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Sets whether the comparison was successful.
     *
     * @param success true if successful
     */
    public void setSuccess(boolean success) {
        if (success) {
            this.errorMessage = null;
        }
    }
}
