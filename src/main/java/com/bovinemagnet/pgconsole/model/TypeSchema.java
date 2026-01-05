package com.bovinemagnet.pgconsole.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a custom type schema definition from PostgreSQL.
 * <p>
 * This class models PostgreSQL user-defined types including enums, composite types,
 * domain types, and range types. It provides structural comparison capabilities for
 * schema migration and validation purposes.
 * <p>
 * Supported type kinds:
 * <ul>
 *   <li><b>ENUM</b> - Enumerated types with a fixed set of values</li>
 *   <li><b>COMPOSITE</b> - Row types composed of named attributes</li>
 *   <li><b>DOMAIN</b> - Constrained base types with optional defaults and checks</li>
 *   <li><b>RANGE</b> - Range types over a subtype (e.g., int4range, tstzrange)</li>
 * </ul>
 * <p>
 * Usage example:
 * <pre>{@code
 * TypeSchema enumType = TypeSchema.builder()
 *     .schemaName("public")
 *     .typeName("status")
 *     .kind(TypeKind.ENUM)
 *     .enumValues(List.of("pending", "active", "completed"))
 *     .build();
 *
 * if (!enumType.equalsStructure(otherType)) {
 *     List<AttributeDifference> diffs = enumType.getDifferencesFrom(otherType);
 *     // Handle schema differences
 * }
 * }</pre>
 *
 * @author Paul Snow
 * @since 0.0.0
 * @see CompositeAttribute
 * @see AttributeDifference
 */
public class TypeSchema {

    /**
     * Enumeration of PostgreSQL custom type categories.
     * <p>
     * Each kind corresponds to a type class in the PostgreSQL {@code pg_type} system
     * catalogue (typtype column). The code values match PostgreSQL's internal codes.
     */
    public enum TypeKind {
        /** Enumerated type with fixed set of labelled values. */
        ENUM("e", "Enum", "bi-list-ol"),

        /** Composite type (row type) with named attributes. */
        COMPOSITE("c", "Composite", "bi-bricks"),

        /** Domain type (constrained base type with optional default and check constraints). */
        DOMAIN("d", "Domain", "bi-shield"),

        /** Range type over a discrete or continuous subtype. */
        RANGE("r", "Range", "bi-arrows-angle-expand");

        private final String code;
        private final String displayName;
        private final String iconClass;

        /**
         * Constructs a type kind with display metadata.
         *
         * @param code PostgreSQL internal type code (from pg_type.typtype)
         * @param displayName human-readable name for UI display
         * @param iconClass Bootstrap icon class for visual representation
         */
        TypeKind(String code, String displayName, String iconClass) {
            this.code = code;
            this.displayName = displayName;
            this.iconClass = iconClass;
        }

        /**
         * Returns the PostgreSQL internal type code.
         *
         * @return single-character type code (e, c, d, or r)
         */
        public String getCode() {
            return code;
        }

        /**
         * Returns the human-readable display name.
         *
         * @return display name for UI rendering
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Returns the Bootstrap icon class for this type kind.
         *
         * @return Bootstrap icon class name (e.g., "bi-list-ol")
         */
        public String getIconClass() {
            return iconClass;
        }

        /**
         * Resolves a PostgreSQL type code to its corresponding TypeKind.
         *
         * @param code single-character PostgreSQL type code (e, c, d, or r)
         * @return matching TypeKind, or null if code is not recognised
         */
        public static TypeKind fromCode(String code) {
            for (TypeKind kind : values()) {
                if (kind.code.equals(code)) {
                    return kind;
                }
            }
            return null;
        }
    }

    /** Schema name containing this type (e.g., "public", "myschema"). */
    private String schemaName;

    /** Type name without schema qualification. */
    private String typeName;

    /** Database role that owns this type. */
    private String owner;

    /** Optional comment/description for this type. */
    private String comment;

    /** The kind of type (enum, composite, domain, or range). */
    private TypeKind kind;

    // Enum-specific fields

    /** Ordered list of enum labels (for ENUM types only). */
    private List<String> enumValues = new ArrayList<>();

    // Composite-specific fields

    /** List of attributes comprising a composite type (for COMPOSITE types only). */
    private List<CompositeAttribute> attributes = new ArrayList<>();

    // Domain-specific fields

    /** Underlying base type for a domain (for DOMAIN types only). */
    private String baseType;

    /** Default value expression for a domain (for DOMAIN types only). */
    private String defaultValue;

    /** Whether the domain enforces NOT NULL constraint (for DOMAIN types only). */
    private boolean notNull;

    /** List of CHECK constraint expressions for a domain (for DOMAIN types only). */
    private List<String> checkConstraints = new ArrayList<>();

    // Range-specific fields

    /** Subtype over which the range is defined (for RANGE types only). */
    private String subtype;

    /**
     * Creates a builder for constructing TypeSchema instances.
     * <p>
     * The builder pattern allows for flexible construction of TypeSchema objects
     * with only the relevant fields populated for each type kind.
     *
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing TypeSchema instances with a fluent API.
     * <p>
     * Example usage:
     * <pre>{@code
     * TypeSchema domainType = TypeSchema.builder()
     *     .schemaName("public")
     *     .typeName("positive_int")
     *     .kind(TypeKind.DOMAIN)
     *     .baseType("integer")
     *     .notNull(true)
     *     .domainConstraints(List.of("VALUE > 0"))
     *     .build();
     * }</pre>
     */
    public static class Builder {
        private final TypeSchema type = new TypeSchema();

        /**
         * Sets the schema name.
         *
         * @param schemaName schema containing the type
         * @return this builder
         */
        public Builder schemaName(String schemaName) { type.schemaName = schemaName; return this; }

        /**
         * Sets the type name.
         *
         * @param typeName name of the type
         * @return this builder
         */
        public Builder typeName(String typeName) { type.typeName = typeName; return this; }

        /**
         * Sets the owner role.
         *
         * @param owner database role that owns this type
         * @return this builder
         */
        public Builder owner(String owner) { type.owner = owner; return this; }

        /**
         * Sets the type comment.
         *
         * @param comment descriptive comment
         * @return this builder
         */
        public Builder comment(String comment) { type.comment = comment; return this; }

        /**
         * Sets the type kind (enum, composite, domain, or range).
         *
         * @param kind type kind
         * @return this builder
         */
        public Builder kind(TypeKind kind) { type.kind = kind; return this; }

        /**
         * Sets the enum values (for ENUM types).
         *
         * @param enumValues ordered list of enum labels
         * @return this builder
         */
        public Builder enumValues(List<String> enumValues) { type.enumValues = enumValues; return this; }

        /**
         * Sets the base type (for DOMAIN types).
         *
         * @param baseType underlying PostgreSQL type
         * @return this builder
         */
        public Builder baseType(String baseType) { type.baseType = baseType; return this; }

        /**
         * Sets the default value (for DOMAIN types).
         *
         * @param defaultValue default value expression
         * @return this builder
         */
        public Builder defaultValue(String defaultValue) { type.defaultValue = defaultValue; return this; }

        /**
         * Sets whether NOT NULL constraint is enforced (for DOMAIN types).
         *
         * @param notNull true if NOT NULL is enforced
         * @return this builder
         */
        public Builder notNull(boolean notNull) { type.notNull = notNull; return this; }

        /**
         * Sets the subtype (for RANGE types).
         *
         * @param subtype subtype over which the range is defined
         * @return this builder
         */
        public Builder subtype(String subtype) { type.subtype = subtype; return this; }

        /**
         * Sets the enum labels (alias for enumValues).
         *
         * @param labels ordered list of enum labels
         * @return this builder
         */
        public Builder enumLabels(List<String> labels) { type.enumValues = labels; return this; }

        /**
         * Sets the composite type attributes (for COMPOSITE types).
         *
         * @param attrs list of attributes
         * @return this builder
         */
        public Builder attributes(List<CompositeAttribute> attrs) { type.attributes = attrs; return this; }

        /**
         * Sets the domain check constraints (for DOMAIN types).
         *
         * @param constraints list of CHECK constraint expressions
         * @return this builder
         */
        public Builder domainConstraints(List<String> constraints) { type.checkConstraints = constraints; return this; }

        /**
         * Builds the TypeSchema instance.
         *
         * @return constructed TypeSchema
         */
        public TypeSchema build() { return type; }
    }

    /**
     * Constructs an empty TypeSchema.
     * <p>
     * Fields should be populated via setters or preferably via the builder.
     */
    public TypeSchema() {
    }

    /**
     * Constructs a TypeSchema with basic identification.
     *
     * @param schemaName schema containing the type
     * @param typeName name of the type
     * @param kind type kind (enum, composite, domain, or range)
     */
    public TypeSchema(String schemaName, String typeName, TypeKind kind) {
        this.schemaName = schemaName;
        this.typeName = typeName;
        this.kind = kind;
    }

    /**
     * Returns the fully qualified type name.
     *
     * @return schema-qualified name in the form "schema.typename"
     */
    public String getFullName() {
        return schemaName + "." + typeName;
    }

    /**
     * Checks whether this type is structurally equal to another type.
     * <p>
     * Structural equality compares the type kind and kind-specific attributes:
     * <ul>
     *   <li><b>ENUM</b> - compares enum value lists</li>
     *   <li><b>COMPOSITE</b> - compares attribute names and types</li>
     *   <li><b>DOMAIN</b> - compares base type, default, NOT NULL flag, and constraints</li>
     *   <li><b>RANGE</b> - compares subtype</li>
     * </ul>
     * Owner and comment fields are not considered in structural comparison.
     *
     * @param other type to compare against
     * @return true if structurally equivalent, false otherwise
     */
    public boolean equalsStructure(TypeSchema other) {
        if (other == null) return false;
        if (kind != other.kind) return false;

        return switch (kind) {
            case ENUM -> Objects.equals(enumValues, other.enumValues);
            case COMPOSITE -> attributesEqual(other.attributes);
            case DOMAIN -> Objects.equals(baseType, other.baseType)
                    && Objects.equals(defaultValue, other.defaultValue)
                    && notNull == other.notNull
                    && Objects.equals(checkConstraints, other.checkConstraints);
            case RANGE -> Objects.equals(subtype, other.subtype);
        };
    }

    /**
     * Checks whether the composite attributes are structurally equal.
     *
     * @param other list of attributes to compare against
     * @return true if all attributes match in name, type, and position
     */
    private boolean attributesEqual(List<CompositeAttribute> other) {
        if (attributes.size() != other.size()) return false;
        for (int i = 0; i < attributes.size(); i++) {
            if (!attributes.get(i).equalsStructure(other.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Computes the structural differences between this type and another type.
     * <p>
     * This method identifies differences in type kind and kind-specific attributes,
     * marking each difference as breaking or non-breaking for schema migration purposes.
     * <p>
     * Breaking changes include:
     * <ul>
     *   <li>Different type kinds</li>
     *   <li>Modified enum values</li>
     *   <li>Added, removed, or modified composite attributes</li>
     *   <li>Changed domain base type or NOT NULL constraint</li>
     *   <li>Changed range subtype</li>
     * </ul>
     * Non-breaking changes include:
     * <ul>
     *   <li>Modified domain default values</li>
     * </ul>
     *
     * @param other type to compare against
     * @return list of differences, empty if types are structurally equal
     * @see AttributeDifference
     */
    public List<AttributeDifference> getDifferencesFrom(TypeSchema other) {
        List<AttributeDifference> diffs = new ArrayList<>();
        if (other == null) return diffs;

        if (kind != other.kind) {
            diffs.add(new AttributeDifference("Type Kind",
                    kind.getDisplayName(), other.kind.getDisplayName(), true));
            return diffs; // Can't compare further if different kinds
        }

        switch (kind) {
            case ENUM -> {
                if (!Objects.equals(enumValues, other.enumValues)) {
                    diffs.add(AttributeDifference.builder()
                            .attributeName("Enum Values")
                            .sourceValue(String.join(", ", enumValues))
                            .destinationValue(String.join(", ", other.enumValues))
                            .breaking(true)
                            .build());
                }
            }
            case COMPOSITE -> {
                // Compare attributes
                for (CompositeAttribute attr : attributes) {
                    CompositeAttribute otherAttr = other.findAttribute(attr.attributeName);
                    if (otherAttr == null) {
                        diffs.add(new AttributeDifference("Attribute " + attr.attributeName,
                                attr.dataType, null, true));
                    } else if (!attr.equalsStructure(otherAttr)) {
                        diffs.add(new AttributeDifference("Attribute " + attr.attributeName,
                                attr.dataType, otherAttr.dataType, true));
                    }
                }
                for (CompositeAttribute otherAttr : other.attributes) {
                    if (findAttribute(otherAttr.attributeName) == null) {
                        diffs.add(new AttributeDifference("Attribute " + otherAttr.attributeName,
                                null, otherAttr.dataType, true));
                    }
                }
            }
            case DOMAIN -> {
                if (!Objects.equals(baseType, other.baseType)) {
                    diffs.add(new AttributeDifference("Base Type", baseType, other.baseType, true));
                }
                if (!Objects.equals(defaultValue, other.defaultValue)) {
                    diffs.add(new AttributeDifference("Default", defaultValue, other.defaultValue, false));
                }
                if (notNull != other.notNull) {
                    diffs.add(new AttributeDifference("Not Null",
                            notNull ? "YES" : "NO", other.notNull ? "YES" : "NO", true));
                }
            }
            case RANGE -> {
                if (!Objects.equals(subtype, other.subtype)) {
                    diffs.add(new AttributeDifference("Subtype", subtype, other.subtype, true));
                }
            }
        }

        return diffs;
    }

    /**
     * Finds an attribute by name in a composite type.
     *
     * @param name attribute name
     * @return attribute or null
     */
    public CompositeAttribute findAttribute(String name) {
        return attributes.stream()
                .filter(a -> a.attributeName.equals(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Represents an attribute of a composite type.
     */
    public static class CompositeAttribute {
        private String attributeName;
        private String dataType;
        private String collation;
        private int position;

        public CompositeAttribute() {
        }

        public CompositeAttribute(String attributeName, String dataType) {
            this.attributeName = attributeName;
            this.dataType = dataType;
        }

        public CompositeAttribute(String attributeName, String dataType, int position) {
            this.attributeName = attributeName;
            this.dataType = dataType;
            this.position = position;
        }

        public boolean equalsStructure(CompositeAttribute other) {
            if (other == null) return false;
            return Objects.equals(attributeName, other.attributeName)
                    && Objects.equals(dataType, other.dataType);
        }

        // Getters and Setters
        public String getAttributeName() { return attributeName; }
        public void setAttributeName(String attributeName) { this.attributeName = attributeName; }
        public String getDataType() { return dataType; }
        public void setDataType(String dataType) { this.dataType = dataType; }
        public String getCollation() { return collation; }
        public void setCollation(String collation) { this.collation = collation; }
        public int getPosition() { return position; }
        public void setPosition(int position) { this.position = position; }
    }

    // Getters and Setters

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public TypeKind getKind() {
        return kind;
    }

    public void setKind(TypeKind kind) {
        this.kind = kind;
    }

    public List<String> getEnumValues() {
        return enumValues;
    }

    public void setEnumValues(List<String> enumValues) {
        this.enumValues = enumValues;
    }

    public List<CompositeAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<CompositeAttribute> attributes) {
        this.attributes = attributes;
    }

    public String getBaseType() {
        return baseType;
    }

    public void setBaseType(String baseType) {
        this.baseType = baseType;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isNotNull() {
        return notNull;
    }

    public void setNotNull(boolean notNull) {
        this.notNull = notNull;
    }

    public List<String> getCheckConstraints() {
        return checkConstraints;
    }

    public void setCheckConstraints(List<String> checkConstraints) {
        this.checkConstraints = checkConstraints;
    }

    public String getSubtype() {
        return subtype;
    }

    public void setSubtype(String subtype) {
        this.subtype = subtype;
    }

    public void addEnumValue(String value) {
        this.enumValues.add(value);
    }

    public void addAttribute(CompositeAttribute attr) {
        this.attributes.add(attr);
    }

    public void addCheckConstraint(String constraint) {
        this.checkConstraints.add(constraint);
    }

    /**
     * Gets enum labels (alias for getEnumValues for API compatibility).
     *
     * @return list of enum labels
     */
    public List<String> getEnumLabels() {
        return enumValues;
    }
}
