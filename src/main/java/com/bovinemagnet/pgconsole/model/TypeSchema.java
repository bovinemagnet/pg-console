package com.bovinemagnet.pgconsole.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a custom type schema definition.
 * <p>
 * Supports enum types, composite types, and domain types for
 * schema comparison.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class TypeSchema {

    /**
     * Type of custom type.
     */
    public enum TypeKind {
        ENUM("e", "Enum", "bi-list-ol"),
        COMPOSITE("c", "Composite", "bi-bricks"),
        DOMAIN("d", "Domain", "bi-shield"),
        RANGE("r", "Range", "bi-arrows-angle-expand");

        private final String code;
        private final String displayName;
        private final String iconClass;

        TypeKind(String code, String displayName, String iconClass) {
            this.code = code;
            this.displayName = displayName;
            this.iconClass = iconClass;
        }

        public String getCode() {
            return code;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getIconClass() {
            return iconClass;
        }

        public static TypeKind fromCode(String code) {
            for (TypeKind kind : values()) {
                if (kind.code.equals(code)) {
                    return kind;
                }
            }
            return null;
        }
    }

    private String schemaName;
    private String typeName;
    private String owner;
    private String comment;
    private TypeKind kind;

    // For enum types
    private List<String> enumValues = new ArrayList<>();

    // For composite types
    private List<CompositeAttribute> attributes = new ArrayList<>();

    // For domain types
    private String baseType;
    private String defaultValue;
    private boolean notNull;
    private List<String> checkConstraints = new ArrayList<>();

    // For range types
    private String subtype;

    /**
     * Creates a builder for TypeSchema.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for TypeSchema.
     */
    public static class Builder {
        private final TypeSchema type = new TypeSchema();

        public Builder schemaName(String schemaName) { type.schemaName = schemaName; return this; }
        public Builder typeName(String typeName) { type.typeName = typeName; return this; }
        public Builder owner(String owner) { type.owner = owner; return this; }
        public Builder comment(String comment) { type.comment = comment; return this; }
        public Builder kind(TypeKind kind) { type.kind = kind; return this; }
        public Builder enumValues(List<String> enumValues) { type.enumValues = enumValues; return this; }
        public Builder baseType(String baseType) { type.baseType = baseType; return this; }
        public Builder defaultValue(String defaultValue) { type.defaultValue = defaultValue; return this; }
        public Builder notNull(boolean notNull) { type.notNull = notNull; return this; }
        public Builder subtype(String subtype) { type.subtype = subtype; return this; }
        public Builder enumLabels(List<String> labels) { type.enumValues = labels; return this; }
        public Builder attributes(List<CompositeAttribute> attrs) { type.attributes = attrs; return this; }
        public Builder domainConstraints(List<String> constraints) { type.checkConstraints = constraints; return this; }
        public TypeSchema build() { return type; }
    }

    public TypeSchema() {
    }

    public TypeSchema(String schemaName, String typeName, TypeKind kind) {
        this.schemaName = schemaName;
        this.typeName = typeName;
        this.kind = kind;
    }

    /**
     * Gets the fully qualified type name.
     *
     * @return schema.type name
     */
    public String getFullName() {
        return schemaName + "." + typeName;
    }

    /**
     * Checks if this type equals another for comparison purposes.
     *
     * @param other other type
     * @return true if structurally equal
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
     * Gets differences from another type.
     *
     * @param other other type
     * @return list of differences
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
