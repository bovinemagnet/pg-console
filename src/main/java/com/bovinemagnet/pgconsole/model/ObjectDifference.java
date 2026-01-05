package com.bovinemagnet.pgconsole.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single difference between database objects when comparing source and destination schemas.
 * <p>
 * This class encapsulates comprehensive information about schema differences at the object level,
 * including tables, columns, indexes, constraints, views, functions, and other database objects.
 * It captures the type of difference (missing, extra, or modified), the severity of the change
 * (breaking, warning, or informational), and provides generated DDL for migration purposes.
 * </p>
 * <p>
 * The class supports hierarchical relationships through parent object names (e.g., a column
 * belongs to a table) and tracks dependent objects that may be affected by changes. It also
 * maintains detailed attribute-level differences for modified objects via {@link AttributeDifference}.
 * </p>
 * <p>
 * Key features:
 * </p>
 * <ul>
 *   <li>Support for multiple database object types (tables, views, functions, etc.)</li>
 *   <li>Three-tier severity classification for migration planning</li>
 *   <li>Automatic DDL generation for applying changes</li>
 *   <li>Dependency tracking for impact analysis</li>
 *   <li>Detailed attribute-level change tracking</li>
 *   <li>Bootstrap CSS class integration for UI rendering</li>
 * </ul>
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * ObjectDifference tableDiff = ObjectDifference.builder()
 *     .objectName("users")
 *     .schemaName("public")
 *     .objectType(ObjectType.TABLE)
 *     .differenceType(DifferenceType.MISSING)
 *     .severity(Severity.INFO)
 *     .sourceDefinition("CREATE TABLE users (...)")
 *     .generatedDdl("CREATE TABLE public.users (...)")
 *     .build();
 *
 * if (tableDiff.isBreaking()) {
 *     logger.error("Breaking change detected: " + tableDiff.getSummary());
 * }
 *
 * // Apply migration if needed
 * if (tableDiff.isMissing() && tableDiff.getGeneratedDdl() != null) {
 *     executeScript(tableDiff.getGeneratedDdl());
 * }
 * }</pre>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see AttributeDifference
 * @see DifferenceType
 * @see Severity
 * @see ObjectType
 */
public class ObjectDifference {

    /**
     * Classifies the type of difference between source and destination database objects.
     * <p>
     * This enumeration identifies whether an object exists only in the source (missing from destination),
     * only in the destination (extra compared to source), or exists in both but with different definitions.
     * Each type includes display metadata for UI rendering including CSS classes for Bootstrap styling.
     * </p>
     */
    public enum DifferenceType {
        /** Object exists in source schema but not in destination - typically requires CREATE DDL. */
        MISSING("Missing", "bg-warning", "Object exists in source but not in destination"),

        /** Object exists in destination schema but not in source - may require DROP DDL. */
        EXTRA("Extra", "bg-info", "Object exists in destination but not in source"),

        /** Object exists in both schemas with different definitions - typically requires ALTER DDL. */
        MODIFIED("Modified", "bg-primary", "Object exists in both but differs");

        private final String displayName;
        private final String cssClass;
        private final String description;

        /**
         * Constructs a DifferenceType with the specified display metadata.
         *
         * @param displayName human-readable name for UI display
         * @param cssClass Bootstrap CSS class for styling (e.g., "bg-warning")
         * @param description detailed explanation of this difference type
         */
        DifferenceType(String displayName, String cssClass, String description) {
            this.displayName = displayName;
            this.cssClass = cssClass;
            this.description = description;
        }

        /**
         * Returns the human-readable display name.
         *
         * @return the display name, never null
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Returns the Bootstrap CSS class for styling this difference type in HTML.
         *
         * @return the CSS class name, never null
         */
        public String getCssClass() {
            return cssClass;
        }

        /**
         * Returns a detailed description explaining this difference type.
         *
         * @return the description text, never null
         */
        public String getDescription() {
            return description;
        }
    }

    /**
     * Classifies the severity of a schema difference for migration planning and risk assessment.
     * <p>
     * This enumeration categorises changes into three severity levels based on their potential
     * impact on data integrity, backward compatibility, and application functionality. The severity
     * guides migration planning and helps prioritise which changes require careful review versus
     * automatic application.
     * </p>
     */
    public enum Severity {
        /**
         * Breaking changes that typically require DROP operations with potential data loss.
         * Examples: removing a column, dropping a table, or deleting a constraint.
         * Requires careful review and potential data backup before application.
         */
        BREAKING("Breaking", "bg-danger", "Requires DROP - potential data loss"),

        /**
         * Warning-level changes that require ALTER operations and may affect functionality.
         * Examples: modifying column types, changing nullability, or altering constraints.
         * Application code may need updates to accommodate these changes.
         */
        WARNING("Warning", "bg-warning text-dark", "Requires ALTER - may affect functionality"),

        /**
         * Informational changes that are purely additive and safe to apply.
         * Examples: adding a new column, creating an index, or defining a new function.
         * Generally backward-compatible with minimal risk.
         */
        INFO("Info", "bg-info", "Additive change - safe to apply");

        private final String displayName;
        private final String cssClass;
        private final String description;

        /**
         * Constructs a Severity level with the specified display metadata.
         *
         * @param displayName human-readable name for UI display
         * @param cssClass Bootstrap CSS class for styling (e.g., "bg-danger")
         * @param description detailed explanation of this severity level
         */
        Severity(String displayName, String cssClass, String description) {
            this.displayName = displayName;
            this.cssClass = cssClass;
            this.description = description;
        }

        /**
         * Returns the human-readable display name.
         *
         * @return the display name, never null
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Returns the Bootstrap CSS class for styling this severity level in HTML.
         *
         * @return the CSS class name, never null
         */
        public String getCssClass() {
            return cssClass;
        }

        /**
         * Returns a detailed description explaining this severity level.
         *
         * @return the description text, never null
         */
        public String getDescription() {
            return description;
        }
    }

    /**
     * Classifies the type of database object being compared during schema analysis.
     * <p>
     * This enumeration covers all major PostgreSQL database object types that can be
     * compared between source and destination schemas. Each type includes display metadata
     * for UI rendering, including human-readable names and Bootstrap Icon classes for
     * consistent visual representation.
     * </p>
     */
    public enum ObjectType {
        /** Database table containing rows of data. */
        TABLE("Table", "bi-table"),

        /** Column within a table, view, or composite type. */
        COLUMN("Column", "bi-columns-gap"),

        /** Index for optimising query performance. */
        INDEX("Index", "bi-list-ul"),

        /** Primary key constraint ensuring row uniqueness. */
        CONSTRAINT_PRIMARY("Primary Key", "bi-key"),

        /** Foreign key constraint enforcing referential integrity. */
        CONSTRAINT_FOREIGN("Foreign Key", "bi-link"),

        /** Unique constraint preventing duplicate values. */
        CONSTRAINT_UNIQUE("Unique Constraint", "bi-asterisk"),

        /** Check constraint validating data values. */
        CONSTRAINT_CHECK("Check Constraint", "bi-check-square"),

        /** Standard view providing a virtual table. */
        VIEW("View", "bi-eye"),

        /** Materialised view with physically stored query results. */
        MATERIALIZED_VIEW("Materialised View", "bi-eye-fill"),

        /** Stored function returning a value or table. */
        FUNCTION("Function", "bi-code-slash"),

        /** Stored procedure performing operations without returning a value. */
        PROCEDURE("Procedure", "bi-gear"),

        /** Trigger that executes automatically on table events. */
        TRIGGER("Trigger", "bi-lightning"),

        /** Sequence generator for auto-incrementing values. */
        SEQUENCE("Sequence", "bi-123"),

        /** Enumerated type with a fixed set of allowed values. */
        TYPE_ENUM("Enum Type", "bi-list-ol"),

        /** Composite type defining a structured row type. */
        TYPE_COMPOSITE("Composite Type", "bi-bricks"),

        /** Domain type with constraints on an underlying base type. */
        TYPE_DOMAIN("Domain", "bi-shield"),

        /** PostgreSQL extension adding additional functionality. */
        EXTENSION("Extension", "bi-puzzle");

        private final String displayName;
        private final String iconClass;

        /**
         * Constructs an ObjectType with the specified display metadata.
         *
         * @param displayName human-readable name for UI display
         * @param iconClass Bootstrap Icon class name (e.g., "bi-table")
         */
        ObjectType(String displayName, String iconClass) {
            this.displayName = displayName;
            this.iconClass = iconClass;
        }

        /**
         * Returns the human-readable display name.
         *
         * @return the display name, never null
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Returns the Bootstrap Icon class for visual representation.
         *
         * @return the icon class name, never null
         */
        public String getIconClass() {
            return iconClass;
        }
    }

    /**
     * The name of the database object (e.g., table name, column name, function name).
     * Does not include the schema qualifier.
     */
    private String objectName;

    /**
     * The schema containing this object (e.g., "public", "schema_name").
     * May be null or empty for schema-agnostic objects.
     */
    private String schemaName;

    /**
     * The type of database object being compared.
     */
    private ObjectType objectType;

    /**
     * The type of difference detected (missing, extra, or modified).
     */
    private DifferenceType differenceType;

    /**
     * The severity classification for migration planning.
     */
    private Severity severity;

    /**
     * The complete DDL or definition of the object in the source schema.
     * May be null if the object doesn't exist in the source.
     */
    private String sourceDefinition;

    /**
     * The complete DDL or definition of the object in the destination schema.
     * May be null if the object doesn't exist in the destination.
     */
    private String destinationDefinition;

    /**
     * Detailed attribute-level differences for modified objects.
     * Empty list if the object is missing, extra, or has no attribute changes.
     */
    private List<AttributeDifference> attributeDifferences = new ArrayList<>();

    /**
     * The generated DDL statement to apply this change to the destination schema.
     * For MISSING objects, typically a CREATE statement; for EXTRA objects, a DROP statement;
     * for MODIFIED objects, an ALTER statement.
     */
    private String generatedDdl;

    /**
     * List of other database objects that depend on this object.
     * Useful for impact analysis and determining safe migration order.
     * For example, a table may list views, foreign keys, or triggers that depend on it.
     */
    private List<String> dependentObjects = new ArrayList<>();

    /**
     * The name of the parent object, if this object is a child.
     * For example, a column's parent would be its table name,
     * an index's parent would be the table it indexes.
     * Null for top-level objects like tables, views, and functions.
     */
    private String parentObjectName;

    /**
     * Constructs an empty ObjectDifference instance.
     * Primarily used by frameworks for deserialisation or by the builder pattern.
     */
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
     * The builder pattern provides a fluent API for setting fields and improves readability
     * when creating instances with multiple optional properties.
     *
     * @return a new builder instance
     * @see Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ObjectDifference} instances with a fluent API.
     * <p>
     * This builder enables step-by-step construction of complex ObjectDifference instances
     * with optional fields and collections. All setter methods return the builder instance
     * to enable method chaining.
     * </p>
     * <p>
     * Example usage:
     * </p>
     * <pre>{@code
     * ObjectDifference diff = ObjectDifference.builder()
     *     .objectName("users")
     *     .schemaName("public")
     *     .objectType(ObjectType.TABLE)
     *     .differenceType(DifferenceType.MODIFIED)
     *     .severity(Severity.WARNING)
     *     .sourceDefinition("CREATE TABLE users (id INTEGER, ...)")
     *     .destinationDefinition("CREATE TABLE users (id BIGINT, ...)")
     *     .attributeDifference(AttributeDifference.builder()
     *         .attributeName("id_type")
     *         .sourceValue("INTEGER")
     *         .destinationValue("BIGINT")
     *         .build())
     *     .generatedDdl("ALTER TABLE public.users ALTER COLUMN id TYPE BIGINT")
     *     .build();
     * }</pre>
     */
    public static class Builder {
        private final ObjectDifference diff = new ObjectDifference();

        /**
         * Sets the object name.
         *
         * @param objectName the name of the database object
         * @return this builder instance for method chaining
         */
        public Builder objectName(String objectName) {
            diff.objectName = objectName;
            return this;
        }

        /**
         * Sets the schema name.
         *
         * @param schemaName the schema containing the object
         * @return this builder instance for method chaining
         */
        public Builder schemaName(String schemaName) {
            diff.schemaName = schemaName;
            return this;
        }

        /**
         * Sets the object type.
         *
         * @param objectType the type of database object
         * @return this builder instance for method chaining
         */
        public Builder objectType(ObjectType objectType) {
            diff.objectType = objectType;
            return this;
        }

        /**
         * Sets the difference type.
         *
         * @param differenceType the type of difference detected
         * @return this builder instance for method chaining
         */
        public Builder differenceType(DifferenceType differenceType) {
            diff.differenceType = differenceType;
            return this;
        }

        /**
         * Sets the severity level.
         *
         * @param severity the severity classification
         * @return this builder instance for method chaining
         */
        public Builder severity(Severity severity) {
            diff.severity = severity;
            return this;
        }

        /**
         * Sets the source definition.
         *
         * @param sourceDefinition the DDL or definition in the source schema
         * @return this builder instance for method chaining
         */
        public Builder sourceDefinition(String sourceDefinition) {
            diff.sourceDefinition = sourceDefinition;
            return this;
        }

        /**
         * Sets the destination definition.
         *
         * @param destinationDefinition the DDL or definition in the destination schema
         * @return this builder instance for method chaining
         */
        public Builder destinationDefinition(String destinationDefinition) {
            diff.destinationDefinition = destinationDefinition;
            return this;
        }

        /**
         * Adds a single attribute difference to the collection.
         *
         * @param attrDiff the attribute difference to add
         * @return this builder instance for method chaining
         */
        public Builder attributeDifference(AttributeDifference attrDiff) {
            diff.attributeDifferences.add(attrDiff);
            return this;
        }

        /**
         * Adds multiple attribute differences to the collection.
         *
         * @param attrDiffs the list of attribute differences to add
         * @return this builder instance for method chaining
         */
        public Builder attributeDifferences(List<AttributeDifference> attrDiffs) {
            diff.attributeDifferences.addAll(attrDiffs);
            return this;
        }

        /**
         * Sets the generated DDL statement.
         *
         * @param generatedDdl the DDL to apply this change
         * @return this builder instance for method chaining
         */
        public Builder generatedDdl(String generatedDdl) {
            diff.generatedDdl = generatedDdl;
            return this;
        }

        /**
         * Adds a single dependent object to the collection.
         *
         * @param dependentObject the name of a dependent object
         * @return this builder instance for method chaining
         */
        public Builder dependentObject(String dependentObject) {
            diff.dependentObjects.add(dependentObject);
            return this;
        }

        /**
         * Adds multiple dependent objects to the collection.
         *
         * @param dependentObjects the list of dependent object names to add
         * @return this builder instance for method chaining
         */
        public Builder dependentObjects(List<String> dependentObjects) {
            diff.dependentObjects.addAll(dependentObjects);
            return this;
        }

        /**
         * Sets the parent object name.
         *
         * @param parentObjectName the name of the parent object, if this is a child object
         * @return this builder instance for method chaining
         */
        public Builder parentObjectName(String parentObjectName) {
            diff.parentObjectName = parentObjectName;
            return this;
        }

        /**
         * Builds and returns the configured ObjectDifference instance.
         *
         * @return the constructed ObjectDifference
         */
        public ObjectDifference build() {
            return diff;
        }
    }

    // Getters and Setters

    /**
     * Returns the name of the database object.
     *
     * @return the object name, may be null
     */
    public String getObjectName() {
        return objectName;
    }

    /**
     * Sets the name of the database object.
     *
     * @param objectName the object name to set
     */
    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    /**
     * Returns the schema containing this object.
     *
     * @return the schema name, may be null or empty
     */
    public String getSchemaName() {
        return schemaName;
    }

    /**
     * Sets the schema containing this object.
     *
     * @param schemaName the schema name to set
     */
    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    /**
     * Returns the type of database object.
     *
     * @return the object type, may be null
     */
    public ObjectType getObjectType() {
        return objectType;
    }

    /**
     * Sets the type of database object.
     *
     * @param objectType the object type to set
     */
    public void setObjectType(ObjectType objectType) {
        this.objectType = objectType;
    }

    /**
     * Returns the type of difference detected.
     *
     * @return the difference type, may be null
     */
    public DifferenceType getDifferenceType() {
        return differenceType;
    }

    /**
     * Sets the type of difference detected.
     *
     * @param differenceType the difference type to set
     */
    public void setDifferenceType(DifferenceType differenceType) {
        this.differenceType = differenceType;
    }

    /**
     * Returns the severity classification.
     *
     * @return the severity level, may be null
     */
    public Severity getSeverity() {
        return severity;
    }

    /**
     * Sets the severity classification.
     *
     * @param severity the severity level to set
     */
    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    /**
     * Returns the DDL or definition of the object in the source schema.
     *
     * @return the source definition, may be null
     */
    public String getSourceDefinition() {
        return sourceDefinition;
    }

    /**
     * Sets the DDL or definition of the object in the source schema.
     *
     * @param sourceDefinition the source definition to set
     */
    public void setSourceDefinition(String sourceDefinition) {
        this.sourceDefinition = sourceDefinition;
    }

    /**
     * Returns the DDL or definition of the object in the destination schema.
     *
     * @return the destination definition, may be null
     */
    public String getDestinationDefinition() {
        return destinationDefinition;
    }

    /**
     * Sets the DDL or definition of the object in the destination schema.
     *
     * @param destinationDefinition the destination definition to set
     */
    public void setDestinationDefinition(String destinationDefinition) {
        this.destinationDefinition = destinationDefinition;
    }

    /**
     * Returns the list of detailed attribute-level differences.
     * The returned list is mutable and can be modified directly.
     *
     * @return the list of attribute differences, never null but may be empty
     */
    public List<AttributeDifference> getAttributeDifferences() {
        return attributeDifferences;
    }

    /**
     * Sets the list of detailed attribute-level differences.
     *
     * @param attributeDifferences the list of attribute differences to set
     */
    public void setAttributeDifferences(List<AttributeDifference> attributeDifferences) {
        this.attributeDifferences = attributeDifferences;
    }

    /**
     * Returns the generated DDL statement to apply this change.
     *
     * @return the generated DDL, may be null
     */
    public String getGeneratedDdl() {
        return generatedDdl;
    }

    /**
     * Sets the generated DDL statement to apply this change.
     *
     * @param generatedDdl the generated DDL to set
     */
    public void setGeneratedDdl(String generatedDdl) {
        this.generatedDdl = generatedDdl;
    }

    /**
     * Returns the list of dependent object names.
     * The returned list is mutable and can be modified directly.
     *
     * @return the list of dependent objects, never null but may be empty
     */
    public List<String> getDependentObjects() {
        return dependentObjects;
    }

    /**
     * Sets the list of dependent object names.
     *
     * @param dependentObjects the list of dependent objects to set
     */
    public void setDependentObjects(List<String> dependentObjects) {
        this.dependentObjects = dependentObjects;
    }

    /**
     * Returns the name of the parent object, if this is a child object.
     *
     * @return the parent object name, or null for top-level objects
     */
    public String getParentObjectName() {
        return parentObjectName;
    }

    /**
     * Sets the name of the parent object.
     *
     * @param parentObjectName the parent object name to set, or null for top-level objects
     */
    public void setParentObjectName(String parentObjectName) {
        this.parentObjectName = parentObjectName;
    }
}
