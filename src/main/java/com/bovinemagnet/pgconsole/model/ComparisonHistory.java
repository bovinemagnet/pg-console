package com.bovinemagnet.pgconsole.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Record of a schema comparison execution.
 * <p>
 * Used for audit logging and drift detection over time.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class ComparisonHistory {

    private Long id;
    private Instant comparedAt;
    private String sourceInstance;
    private String destinationInstance;
    private String sourceSchema;
    private String destinationSchema;
    private String performedBy;
    private int missingCount;
    private int extraCount;
    private int modifiedCount;
    private int matchingCount;
    private String profileName;
    private String resultSnapshotJson;
    private String filterConfigJson;

    public ComparisonHistory() {
        this.comparedAt = Instant.now();
    }

    /**
     * Creates a history record from a comparison result.
     *
     * @param result comparison result
     * @param username user who performed comparison
     * @return history record
     */
    public static ComparisonHistory fromResult(SchemaComparisonResult result, String username) {
        ComparisonHistory history = new ComparisonHistory();
        history.comparedAt = result.getComparedAt();
        history.sourceInstance = result.getSourceInstance();
        history.destinationInstance = result.getDestinationInstance();
        history.sourceSchema = result.getSourceSchema();
        history.destinationSchema = result.getDestinationSchema();
        history.performedBy = username;
        history.missingCount = result.getSummary().getMissingObjects();
        history.extraCount = result.getSummary().getExtraObjects();
        history.modifiedCount = result.getSummary().getModifiedObjects();
        history.matchingCount = result.getSummary().getMatchingObjects();
        return history;
    }

    /**
     * Gets formatted comparison timestamp.
     *
     * @return formatted date/time
     */
    public String getComparedAtFormatted() {
        if (comparedAt == null) return "-";
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(comparedAt);
    }

    /**
     * Gets total difference count.
     *
     * @return total differences
     */
    public int getTotalDifferences() {
        return missingCount + extraCount + modifiedCount;
    }

    /**
     * Gets a description of the comparison.
     *
     * @return description text
     */
    public String getDescription() {
        return String.format("%s.%s → %s.%s",
                sourceInstance, sourceSchema,
                destinationInstance, destinationSchema);
    }

    /**
     * Gets summary text.
     *
     * @return summary text
     */
    public String getSummaryText() {
        if (getTotalDifferences() == 0) {
            return "Identical";
        }
        return String.format("%d differences", getTotalDifferences());
    }

    /**
     * Gets CSS class based on result.
     *
     * @return CSS class
     */
    public String getSummaryCssClass() {
        if (getTotalDifferences() == 0) {
            return "bg-success";
        } else if (missingCount > 0 || modifiedCount > 0) {
            return "bg-warning text-dark";
        } else {
            return "bg-info";
        }
    }

    /**
     * Checks if this comparison shows drift from a previous one.
     *
     * @param previous previous comparison
     * @return true if drift detected
     */
    public boolean hasDriftFrom(ComparisonHistory previous) {
        if (previous == null) return false;
        return missingCount != previous.missingCount
                || extraCount != previous.extraCount
                || modifiedCount != previous.modifiedCount;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getComparedAt() {
        return comparedAt;
    }

    public void setComparedAt(Instant comparedAt) {
        this.comparedAt = comparedAt;
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

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public int getMissingCount() {
        return missingCount;
    }

    public void setMissingCount(int missingCount) {
        this.missingCount = missingCount;
    }

    public int getExtraCount() {
        return extraCount;
    }

    public void setExtraCount(int extraCount) {
        this.extraCount = extraCount;
    }

    public int getModifiedCount() {
        return modifiedCount;
    }

    public void setModifiedCount(int modifiedCount) {
        this.modifiedCount = modifiedCount;
    }

    public int getMatchingCount() {
        return matchingCount;
    }

    public void setMatchingCount(int matchingCount) {
        this.matchingCount = matchingCount;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public String getResultSnapshotJson() {
        return resultSnapshotJson;
    }

    public void setResultSnapshotJson(String resultSnapshotJson) {
        this.resultSnapshotJson = resultSnapshotJson;
    }

    public String getFilterConfigJson() {
        return filterConfigJson;
    }

    public void setFilterConfigJson(String filterConfigJson) {
        this.filterConfigJson = filterConfigJson;
    }
}
