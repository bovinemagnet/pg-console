package com.bovinemagnet.pgconsole.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single difference between objects in source and destination schemas.
 * <p>
 * This class encapsulates all information about a schema difference, including
 * the type of difference, severity, affected object details, and generated DDL.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class ObjectDifference {

    /**
     * Type of difference between source and destination.
     */
    public enum DifferenceType {
        MISSING("Missing", "bg-warning", "Object exists in source but not in destination"),
        EXTRA("Extra", "bg-info", "Object exists in destination but not in source"),
        MODIFIED("Modified", "bg-primary", "Object exists in both but differs");

        private final String displayName;
        private final String cssClass;
        private final String description;

        DifferenceType(String displayName, String cssClass, String description) {
            this.displayName = displayName;
            this.cssClass = cssClass;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getCssClass() {
            return cssClass;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Severity of the difference for migration planning.
     */
    public enum Severity {
        BREAKING("Breaking", "bg-danger", "Requires DROP - potential data loss"),
        WARNING("Warning", "bg-warning text-dark", "Requires ALTER - may affect functionality"),
        INFO("Info", "bg-info", "Additive change - safe to apply");

        private final String displayName;
        private final String cssClass;
        private final String description;

        Severity(String displayName, String cssClass, String description) {
            this.displayName = displayName;
            this.cssClass = cssClass;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getCssClass() {
            return cssClass;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Type of database object being compared.
     */
    public enum ObjectType {
        TABLE("Table", "bi-table"),
        COLUMN("Column", "bi-columns-gap"),
        INDEX("Index", "bi-list-ul"),
        CONSTRAINT_PRIMARY("Primary Key", "bi-key"),
        CONSTRAINT_FOREIGN("Foreign Key", "bi-link"),
        CONSTRAINT_UNIQUE("Unique Constraint", "bi-asterisk"),
        CONSTRAINT_CHECK("Check Constraint", "bi-check-square"),
        VIEW("View", "bi-eye"),
        MATERIALIZED_VIEW("Materialised View", "bi-eye-fill"),
        FUNCTION("Function", "bi-code-slash"),
        PROCEDURE("Procedure", "bi-gear"),
        TRIGGER("Trigger", "bi-lightning"),
        SEQUENCE("Sequence", "bi-123"),
        TYPE_ENUM("Enum Type", "bi-list-ol"),
        TYPE_COMPOSITE("Composite Type", "bi-bricks"),
        TYPE_DOMAIN("Domain", "bi-shield"),
        EXTENSION("Extension", "bi-puzzle");

        private final String displayName;
        private final String iconClass;

        ObjectType(String displayName, String iconClass) {
            this.displayName = displayName;
            this.iconClass = iconClass;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getIconClass() {
            return iconClass;
        }
    }

    private String objectName;
    private String schemaName;
    private ObjectType objectType;
    private DifferenceType differenceType;
    private Severity severity;
    private String sourceDefinition;
    private String destinationDefinition;
    private List<AttributeDifference> attributeDifferences = new ArrayList<>();
    private String generatedDdl;
    private List<String> dependentObjects = new ArrayList<>();
    private String parentObjectName;

    public ObjectDifference() {
    }

    /**
     * Returns the fully qualified name (schema.object).
     *
     * @return full name of the object
     */
    public String getFullName() {
        if (schemaName == null || schemaName.isEmpty()) {
            return objectName;
        }
        return schemaName + "." + objectName;
    }

    /**
     * Checks if this difference is a breaking change.
     *
     * @return true if severity is BREAKING
     */
    public boolean isBreaking() {
        return severity == Severity.BREAKING;
    }

    /**
     * Checks if this difference is a warning-level change.
     *
     * @return true if severity is WARNING
     */
    public boolean isWarning() {
        return severity == Severity.WARNING;
    }

    /**
     * Checks if this difference is informational only.
     *
     * @return true if severity is INFO
     */
    public boolean isInfo() {
        return severity == Severity.INFO;
    }

    /**
     * Checks if this is a missing object (exists in source only).
     *
     * @return true if difference type is MISSING
     */
    public boolean isMissing() {
        return differenceType == DifferenceType.MISSING;
    }

    /**
     * Checks if this is an extra object (exists in destination only).
     *
     * @return true if difference type is EXTRA
     */
    public boolean isExtra() {
        return differenceType == DifferenceType.EXTRA;
    }

    /**
     * Checks if this object was modified.
     *
     * @return true if difference type is MODIFIED
     */
    public boolean isModified() {
        return differenceType == DifferenceType.MODIFIED;
    }

    /**
     * Gets summary description for display.
     *
     * @return summary text
     */
    public String getSummary() {
        return String.format("%s %s: %s",
                differenceType.getDisplayName(),
                objectType.getDisplayName(),
                getFullName());
    }

    /**
     * Creates a builder for constructing ObjectDifference instances.
     *
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for ObjectDifference.
     */
    public static class Builder {
        private final ObjectDifference diff = new ObjectDifference();

        public Builder objectName(String objectName) {
            diff.objectName = objectName;
            return this;
        }

        public Builder schemaName(String schemaName) {
            diff.schemaName = schemaName;
            return this;
        }

        public Builder objectType(ObjectType objectType) {
            diff.objectType = objectType;
            return this;
        }

        public Builder differenceType(DifferenceType differenceType) {
            diff.differenceType = differenceType;
            return this;
        }

        public Builder severity(Severity severity) {
            diff.severity = severity;
            return this;
        }

        public Builder sourceDefinition(String sourceDefinition) {
            diff.sourceDefinition = sourceDefinition;
            return this;
        }

        public Builder destinationDefinition(String destinationDefinition) {
            diff.destinationDefinition = destinationDefinition;
            return this;
        }

        public Builder attributeDifference(AttributeDifference attrDiff) {
            diff.attributeDifferences.add(attrDiff);
            return this;
        }

        public Builder attributeDifferences(List<AttributeDifference> attrDiffs) {
            diff.attributeDifferences.addAll(attrDiffs);
            return this;
        }

        public Builder generatedDdl(String generatedDdl) {
            diff.generatedDdl = generatedDdl;
            return this;
        }

        public Builder dependentObject(String dependentObject) {
            diff.dependentObjects.add(dependentObject);
            return this;
        }

        public Builder dependentObjects(List<String> dependentObjects) {
            diff.dependentObjects.addAll(dependentObjects);
            return this;
        }

        public Builder parentObjectName(String parentObjectName) {
            diff.parentObjectName = parentObjectName;
            return this;
        }

        public ObjectDifference build() {
            return diff;
        }
    }

    // Getters and Setters

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public ObjectType getObjectType() {
        return objectType;
    }

    public void setObjectType(ObjectType objectType) {
        this.objectType = objectType;
    }

    public DifferenceType getDifferenceType() {
        return differenceType;
    }

    public void setDifferenceType(DifferenceType differenceType) {
        this.differenceType = differenceType;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public String getSourceDefinition() {
        return sourceDefinition;
    }

    public void setSourceDefinition(String sourceDefinition) {
        this.sourceDefinition = sourceDefinition;
    }

    public String getDestinationDefinition() {
        return destinationDefinition;
    }

    public void setDestinationDefinition(String destinationDefinition) {
        this.destinationDefinition = destinationDefinition;
    }

    public List<AttributeDifference> getAttributeDifferences() {
        return attributeDifferences;
    }

    public void setAttributeDifferences(List<AttributeDifference> attributeDifferences) {
        this.attributeDifferences = attributeDifferences;
    }

    public String getGeneratedDdl() {
        return generatedDdl;
    }

    public void setGeneratedDdl(String generatedDdl) {
        this.generatedDdl = generatedDdl;
    }

    public List<String> getDependentObjects() {
        return dependentObjects;
    }

    public void setDependentObjects(List<String> dependentObjects) {
        this.dependentObjects = dependentObjects;
    }

    public String getParentObjectName() {
        return parentObjectName;
    }

    public void setParentObjectName(String parentObjectName) {
        this.parentObjectName = parentObjectName;
    }
}
