package com.bovinemagnet.pgconsole.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a view or materialised view schema definition from PostgreSQL.
 * <p>
 * This class encapsulates complete metadata for both standard views and materialised views,
 * including their SQL definitions, column structures, dependencies, and ownership information.
 * It provides utilities for structural comparison between view schemas, which is particularly
 * useful for schema migration and validation tasks.
 * </p>
 * <p>
 * The class distinguishes between regular views (virtual tables defined by queries) and
 * materialised views (physically stored query results that can be refreshed). For materialised
 * views, it tracks whether they are currently populated with data.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * ViewSchema view = ViewSchema.builder()
 *     .schemaName("public")
 *     .viewName("active_users")
 *     .owner("postgres")
 *     .definition("SELECT id, name FROM users WHERE active = true")
 *     .materialised(false)
 *     .build();
 *
 * // Compare with another view
 * if (!view.equalsStructure(otherView)) {
 *     List<AttributeDifference> diffs = view.getDifferencesFrom(otherView);
 *     // Process differences
 * }
 * }</pre>
 * </p>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see ViewColumn
 * @see AttributeDifference
 */
public class ViewSchema {

    /**
     * The PostgreSQL schema name containing this view (e.g., "public", "analytics").
     */
    private String schemaName;

    /**
     * The name of the view within its schema.
     */
    private String viewName;

    /**
     * The PostgreSQL role that owns this view.
     */
    private String owner;

    /**
     * Optional comment/description attached to the view in PostgreSQL.
     * May be null if no comment is set.
     */
    private String comment;

    /**
     * The SQL definition of the view (the SELECT statement).
     * For regular views, this is the query executed each time the view is accessed.
     * For materialised views, this is the query used to populate the physical storage.
     */
    private String definition;

    /**
     * Indicates whether this is a materialised view (true) or a regular view (false).
     * Materialised views store query results physically and can be refreshed on demand.
     */
    private boolean materialised;

    /**
     * Indicates whether a materialised view is currently populated with data.
     * This field is only relevant when {@link #materialised} is true.
     * A materialised view can be created WITHOUT DATA and populated later via REFRESH.
     */
    private boolean populated;

    /**
     * Detailed column metadata for this view.
     * Contains structured information about each column including name, data type, and position.
     * @see ViewColumn
     */
    private List<ViewColumn> viewColumns = new ArrayList<>();

    /**
     * Simple list of column names in the view.
     * This is a lighter-weight alternative to {@link #viewColumns} when only names are needed.
     */
    private List<String> columns = new ArrayList<>();

    /**
     * List of database objects this view depends on (tables, other views, functions).
     * Represented as fully qualified names where applicable.
     */
    private List<String> dependencies = new ArrayList<>();

    /**
     * List of indexes defined on this view.
     * Only applicable to materialised views, as regular views cannot have indexes.
     */
    private List<String> indexes = new ArrayList<>();

    /**
     * Represents a single column within a view, including its metadata.
     * <p>
     * This record encapsulates the essential information about a view column as retrieved
     * from PostgreSQL's information schema. The ordinal position determines the column's
     * order in the view's output.
     * </p>
     *
     * @param columnName      the name of the column as it appears in the view
     * @param dataType        the PostgreSQL data type of the column (e.g., "integer", "character varying", "timestamp without time zone")
     * @param ordinalPosition the 1-based position of the column in the view (first column is 1)
     */
    public record ViewColumn(String columnName, String dataType, int ordinalPosition) {}

    /**
     * Creates a builder for constructing ViewSchema instances.
     * <p>
     * The builder pattern allows for fluent, readable construction of ViewSchema objects
     * with optional parameters set as needed.
     * </p>
     *
     * @return a new Builder instance
     * @see Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ViewSchema} instances using a fluent API.
     * <p>
     * This builder provides a convenient way to construct ViewSchema objects with
     * only the required fields set. All fields are optional and default to their
     * Java default values (null for references, false for booleans, empty lists).
     * </p>
     * <p>
     * Example usage:
     * <pre>{@code
     * ViewSchema view = ViewSchema.builder()
     *     .schemaName("analytics")
     *     .viewName("monthly_revenue")
     *     .definition("SELECT date_trunc('month', order_date) AS month, SUM(total) ...")
     *     .materialised(true)
     *     .populated(true)
     *     .build();
     * }</pre>
     * </p>
     */
    public static class Builder {
        private final ViewSchema view = new ViewSchema();

        /**
         * Sets the schema name.
         *
         * @param schemaName the PostgreSQL schema name
         * @return this builder for method chaining
         */
        public Builder schemaName(String schemaName) { view.schemaName = schemaName; return this; }

        /**
         * Sets the view name.
         *
         * @param viewName the name of the view
         * @return this builder for method chaining
         */
        public Builder viewName(String viewName) { view.viewName = viewName; return this; }

        /**
         * Sets the owner role.
         *
         * @param owner the PostgreSQL role that owns this view
         * @return this builder for method chaining
         */
        public Builder owner(String owner) { view.owner = owner; return this; }

        /**
         * Sets the view comment.
         *
         * @param comment the comment/description for this view
         * @return this builder for method chaining
         */
        public Builder comment(String comment) { view.comment = comment; return this; }

        /**
         * Sets the view definition (SQL query).
         *
         * @param definition the SQL SELECT statement defining the view
         * @return this builder for method chaining
         */
        public Builder definition(String definition) { view.definition = definition; return this; }

        /**
         * Sets whether this is a materialised view.
         *
         * @param materialised true if this is a materialised view, false for a regular view
         * @return this builder for method chaining
         */
        public Builder materialised(boolean materialised) { view.materialised = materialised; return this; }

        /**
         * Sets whether a materialised view is populated.
         *
         * @param populated true if the materialised view contains data, false if created WITHOUT DATA
         * @return this builder for method chaining
         */
        public Builder populated(boolean populated) { view.populated = populated; return this; }

        /**
         * Constructs the ViewSchema instance with the configured values.
         *
         * @return a new ViewSchema instance
         */
        public ViewSchema build() { return view; }
    }

    /**
     * Constructs an empty ViewSchema.
     * <p>
     * Consider using {@link #builder()} for a more fluent construction approach.
     * </p>
     */
    public ViewSchema() {
    }

    /**
     * Constructs a ViewSchema with schema and view name.
     * <p>
     * This constructor is useful when creating a minimal view reference for comparison
     * or lookup purposes. Other properties can be set via setter methods or use
     * {@link #builder()} for a more complete construction.
     * </p>
     *
     * @param schemaName the PostgreSQL schema name
     * @param viewName   the name of the view
     */
    public ViewSchema(String schemaName, String viewName) {
        this.schemaName = schemaName;
        this.viewName = viewName;
    }

    /**
     * Gets the fully qualified view name in schema.viewname format.
     * <p>
     * This is the canonical way to reference a view in PostgreSQL when the schema
     * is not the default search path.
     * </p>
     *
     * @return the fully qualified name as "schemaName.viewName"
     */
    public String getFullName() {
        return schemaName + "." + viewName;
    }

    /**
     * Checks if this view is structurally equal to another view for comparison purposes.
     * <p>
     * This method performs a semantic comparison by normalising whitespace in the view
     * definitions before comparing them. This allows views with identical logic but
     * different formatting to be considered equal. It also compares the view type
     * (materialised vs regular).
     * </p>
     * <p>
     * Note: This comparison does not include column metadata, dependencies, or indexes,
     * focusing solely on the core definition and type.
     * </p>
     *
     * @param other the other ViewSchema to compare against
     * @return true if the views have equivalent normalised definitions and types, false otherwise
     * @see #normaliseDefinition(String)
     * @see #getDifferencesFrom(ViewSchema)
     */
    public boolean equalsStructure(ViewSchema other) {
        if (other == null) return false;
        String normThis = normaliseDefinition(definition);
        String normOther = normaliseDefinition(other.definition);
        return Objects.equals(normThis, normOther)
                && materialised == other.materialised;
    }

    /**
     * Computes the structural differences between this view and another view.
     * <p>
     * This method identifies discrepancies in the view definition and type, returning
     * a list of {@link AttributeDifference} objects that describe each difference.
     * All differences are marked as breaking since changing a view definition or type
     * constitutes a schema-breaking change.
     * </p>
     * <p>
     * The comparison normalises whitespace in definitions to avoid false positives from
     * formatting differences. Returns an empty list if the views are structurally identical
     * or if the other view is null.
     * </p>
     *
     * @param other the other ViewSchema to compare against
     * @return a list of {@link AttributeDifference} objects describing the differences;
     *         empty list if views are identical or other is null
     * @see AttributeDifference
     * @see #equalsStructure(ViewSchema)
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
     * Normalises a view definition for consistent comparison.
     * <p>
     * This method applies several transformations to make definitions comparable:
     * </p>
     * <ul>
     *   <li>Collapses all whitespace sequences to single spaces</li>
     *   <li>Removes trailing semicolons</li>
     *   <li>Trims leading and trailing whitespace</li>
     *   <li>Converts to lowercase</li>
     * </ul>
     * <p>
     * These normalizations allow views with identical semantics but different
     * formatting or casing to be recognized as equivalent.
     * </p>
     *
     * @param def the raw view definition to normalise
     * @return the normalised definition, or null if the input is null
     */
    private String normaliseDefinition(String def) {
        if (def == null) return null;
        return def.replaceAll("\\s+", " ")
                .replaceAll(";\\s*$", "")
                .trim()
                .toLowerCase();
    }

    /**
     * Gets a human-readable description of the view type.
     * <p>
     * This method is useful for display in user interfaces and reports.
     * </p>
     *
     * @return "Materialised View" if this is a materialised view, "View" otherwise
     * @see #isMaterialised()
     */
    public String getViewType() {
        return materialised ? "Materialised View" : "View";
    }

    /**
     * Gets the Bootstrap CSS class for styling the view type badge.
     * <p>
     * This method supports UI rendering by providing appropriate CSS classes for
     * Bootstrap badges to visually distinguish between view types.
     * </p>
     *
     * @return "bg-info" for materialised views, "bg-secondary" for regular views
     * @see #isMaterialised()
     */
    public String getViewTypeCssClass() {
        return materialised ? "bg-info" : "bg-secondary";
    }

    // Getters and Setters

    /**
     * Gets the schema name.
     *
     * @return the PostgreSQL schema name containing this view
     */
    public String getSchemaName() {
        return schemaName;
    }

    /**
     * Sets the schema name.
     *
     * @param schemaName the PostgreSQL schema name
     */
    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    /**
     * Gets the view name.
     *
     * @return the name of the view within its schema
     */
    public String getViewName() {
        return viewName;
    }

    /**
     * Sets the view name.
     *
     * @param viewName the name of the view
     */
    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    /**
     * Gets the owner role.
     *
     * @return the PostgreSQL role that owns this view
     */
    public String getOwner() {
        return owner;
    }

    /**
     * Sets the owner role.
     *
     * @param owner the PostgreSQL role that owns this view
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * Gets the view comment.
     *
     * @return the comment/description attached to the view, or null if none exists
     */
    public String getComment() {
        return comment;
    }

    /**
     * Sets the view comment.
     *
     * @param comment the comment/description for this view
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Gets the view definition (SQL query).
     *
     * @return the SQL SELECT statement defining the view
     */
    public String getDefinition() {
        return definition;
    }

    /**
     * Sets the view definition.
     *
     * @param definition the SQL SELECT statement defining the view
     */
    public void setDefinition(String definition) {
        this.definition = definition;
    }

    /**
     * Checks if this is a materialised view.
     *
     * @return true if this is a materialised view, false for a regular view
     */
    public boolean isMaterialised() {
        return materialised;
    }

    /**
     * Sets whether this is a materialised view.
     *
     * @param materialised true if this is a materialised view, false for a regular view
     */
    public void setMaterialised(boolean materialised) {
        this.materialised = materialised;
    }

    /**
     * Checks if the materialised view is populated with data.
     * <p>
     * This property is only meaningful for materialised views. A materialised view
     * can be created WITH NO DATA and populated later via REFRESH MATERIALIZED VIEW.
     * </p>
     *
     * @return true if the materialised view contains data, false if unpopulated or if this is a regular view
     */
    public boolean isPopulated() {
        return populated;
    }

    /**
     * Sets whether the materialised view is populated.
     *
     * @param populated true if the materialised view contains data, false otherwise
     */
    public void setPopulated(boolean populated) {
        this.populated = populated;
    }

    /**
     * Gets the list of column names.
     * <p>
     * This is a mutable list that can be modified directly or replaced via
     * {@link #setColumns(List)}.
     * </p>
     *
     * @return the list of column names in this view
     */
    public List<String> getColumns() {
        return columns;
    }

    /**
     * Sets the list of column names.
     *
     * @param columns the list of column names
     */
    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    /**
     * Gets the list of dependencies.
     * <p>
     * Dependencies are other database objects (tables, views, functions) that
     * this view references in its definition. This is a mutable list.
     * </p>
     *
     * @return the list of dependency names (fully qualified where applicable)
     */
    public List<String> getDependencies() {
        return dependencies;
    }

    /**
     * Sets the list of dependencies.
     *
     * @param dependencies the list of database objects this view depends on
     */
    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

    /**
     * Adds a column name to the columns list.
     * <p>
     * This is a convenience method for incrementally building the column list.
     * </p>
     *
     * @param column the column name to add
     */
    public void addColumn(String column) {
        this.columns.add(column);
    }

    /**
     * Adds a dependency to the dependencies list.
     * <p>
     * This is a convenience method for incrementally building the dependency list.
     * </p>
     *
     * @param dependency the fully qualified name of the dependent object
     */
    public void addDependency(String dependency) {
        this.dependencies.add(dependency);
    }

    /**
     * Gets the detailed column metadata.
     * <p>
     * This returns {@link ViewColumn} records containing type and position information
     * in addition to column names. This is a mutable list.
     * </p>
     *
     * @return the list of ViewColumn records
     * @see ViewColumn
     */
    public List<ViewColumn> getViewColumns() {
        return viewColumns;
    }

    /**
     * Sets the detailed column metadata.
     *
     * @param columns the list of ViewColumn records
     */
    public void setViewColumns(List<ViewColumn> columns) {
        this.viewColumns = columns;
    }

    /**
     * Gets the list of indexes defined on this view.
     * <p>
     * Only materialised views can have indexes; regular views cannot. This is a mutable list.
     * </p>
     *
     * @return the list of index names for this materialised view, or an empty list for regular views
     */
    public List<String> getIndexes() {
        return indexes;
    }

    /**
     * Sets the list of indexes.
     *
     * @param indexes the list of index names
     */
    public void setIndexes(List<String> indexes) {
        this.indexes = indexes;
    }
}
