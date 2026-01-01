package com.bovinemagnet.pgconsole.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a view or materialised view schema definition.
 * <p>
 * Contains the view definition, columns, and materialised view-specific
 * properties for schema comparison.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class ViewSchema {

    private String schemaName;
    private String viewName;
    private String owner;
    private String comment;
    private String definition;
    private boolean materialised;
    private boolean populated; // For materialised views
    private List<ViewColumn> viewColumns = new ArrayList<>();
    private List<String> columns = new ArrayList<>();
    private List<String> dependencies = new ArrayList<>();
    private List<String> indexes = new ArrayList<>();

    /**
     * View column information.
     */
    public record ViewColumn(String columnName, String dataType, int ordinalPosition) {}

    /**
     * Creates a builder for ViewSchema.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for ViewSchema.
     */
    public static class Builder {
        private final ViewSchema view = new ViewSchema();

        public Builder schemaName(String schemaName) { view.schemaName = schemaName; return this; }
        public Builder viewName(String viewName) { view.viewName = viewName; return this; }
        public Builder owner(String owner) { view.owner = owner; return this; }
        public Builder comment(String comment) { view.comment = comment; return this; }
        public Builder definition(String definition) { view.definition = definition; return this; }
        public Builder materialised(boolean materialised) { view.materialised = materialised; return this; }
        public Builder populated(boolean populated) { view.populated = populated; return this; }
        public ViewSchema build() { return view; }
    }

    public ViewSchema() {
    }

    public ViewSchema(String schemaName, String viewName) {
        this.schemaName = schemaName;
        this.viewName = viewName;
    }

    /**
     * Gets the fully qualified view name.
     *
     * @return schema.view name
     */
    public String getFullName() {
        return schemaName + "." + viewName;
    }

    /**
     * Checks if this view equals another for comparison purposes.
     * <p>
     * Normalises whitespace in definitions for comparison.
     *
     * @param other other view
     * @return true if structurally equal
     */
    public boolean equalsStructure(ViewSchema other) {
        if (other == null) return false;
        String normThis = normaliseDefinition(definition);
        String normOther = normaliseDefinition(other.definition);
        return Objects.equals(normThis, normOther)
                && materialised == other.materialised;
    }

    /**
     * Gets differences from another view.
     *
     * @param other other view
     * @return list of differences
     */
    public List<AttributeDifference> getDifferencesFrom(ViewSchema other) {
        List<AttributeDifference> diffs = new ArrayList<>();
        if (other == null) return diffs;

        String normThis = normaliseDefinition(definition);
        String normOther = normaliseDefinition(other.definition);

        if (!Objects.equals(normThis, normOther)) {
            diffs.add(AttributeDifference.builder()
                    .attributeName("Definition")
                    .sourceValue(definition)
                    .destinationValue(other.definition)
                    .breaking(true)
                    .build());
        }

        if (materialised != other.materialised) {
            diffs.add(AttributeDifference.builder()
                    .attributeName("Type")
                    .sourceValue(materialised ? "MATERIALISED VIEW" : "VIEW")
                    .destinationValue(other.materialised ? "MATERIALISED VIEW" : "VIEW")
                    .breaking(true)
                    .build());
        }

        return diffs;
    }

    /**
     * Normalises a view definition for comparison.
     *
     * @param def definition text
     * @return normalised definition
     */
    private String normaliseDefinition(String def) {
        if (def == null) return null;
        return def.replaceAll("\\s+", " ")
                .replaceAll(";\\s*$", "")
                .trim()
                .toLowerCase();
    }

    /**
     * Gets the view type as display text.
     *
     * @return "Materialised View" or "View"
     */
    public String getViewType() {
        return materialised ? "Materialised View" : "View";
    }

    /**
     * Gets the view type CSS class.
     *
     * @return CSS class for badge
     */
    public String getViewTypeCssClass() {
        return materialised ? "bg-info" : "bg-secondary";
    }

    // Getters and Setters

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getViewName() {
        return viewName;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
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

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public boolean isMaterialised() {
        return materialised;
    }

    public void setMaterialised(boolean materialised) {
        this.materialised = materialised;
    }

    public boolean isPopulated() {
        return populated;
    }

    public void setPopulated(boolean populated) {
        this.populated = populated;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

    public void addColumn(String column) {
        this.columns.add(column);
    }

    public void addDependency(String dependency) {
        this.dependencies.add(dependency);
    }

    public List<ViewColumn> getViewColumns() {
        return viewColumns;
    }

    public void setViewColumns(List<ViewColumn> columns) {
        this.viewColumns = columns;
    }

    public List<String> getIndexes() {
        return indexes;
    }

    public void setIndexes(List<String> indexes) {
        this.indexes = indexes;
    }
}
