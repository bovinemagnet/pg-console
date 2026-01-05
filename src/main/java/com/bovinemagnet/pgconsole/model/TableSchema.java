package com.bovinemagnet.pgconsole.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a complete table schema definition within a PostgreSQL database.
 * <p>
 * This comprehensive model encapsulates all structural and metadata aspects of a database table,
 * including columns, constraints (primary key, foreign keys, unique, check), indexes, and triggers.
 * It is primarily used for schema comparison between database instances, enabling detection of
 * structural differences and schema drift.
 * </p>
 * <p>
 * The class supports both regular tables and partitioned tables, tracking partition-related metadata
 * such as partition strategy, partition keys, and whether a table is itself a partition. Additionally,
 * it captures table-level features like Row-Level Security (RLS) status, tablespace assignments,
 * and ownership information.
 * </p>
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * TableSchema schema = new TableSchema("public", "users");
 * schema.setOwner("postgres");
 *
 * ColumnDefinition idCol = ColumnDefinition.builder()
 *     .columnName("id")
 *     .dataType("bigint")
 *     .nullable(false)
 *     .isIdentity(true)
 *     .identityType("ALWAYS")
 *     .build();
 * schema.addColumn(idCol);
 *
 * PrimaryKeyDefinition pk = PrimaryKeyDefinition.builder()
 *     .constraintName("users_pkey")
 *     .columns(List.of("id"))
 *     .build();
 * schema.setPrimaryKey(pk);
 *
 * String fullName = schema.getFullName(); // Returns "public.users"
 * }</pre>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see ColumnDefinition
 * @see PrimaryKeyDefinition
 * @see ForeignKeyDefinition
 * @see IndexDefinition
 * @see TriggerDefinition
 * @see AttributeDifference
 */
public class TableSchema {

    /**
     * The PostgreSQL schema name containing this table (e.g., "public", "audit").
     */
    private String schemaName;

    /**
     * The table name within the schema.
     */
    private String tableName;

    /**
     * The database user who owns this table.
     */
    private String owner;

    /**
     * Optional table-level comment providing documentation or metadata.
     */
    private String comment;

    /**
     * Indicates whether this table uses declarative partitioning (is a parent partitioned table).
     */
    private boolean isPartitioned;

    /**
     * Indicates whether this table is itself a partition of a parent table.
     */
    private boolean isPartition;

    /**
     * The partitioning strategy if this is a partitioned table (e.g., RANGE, LIST, HASH).
     */
    private String partitionStrategy;

    /**
     * The partition key expression(s) for partitioned tables.
     */
    private String partitionKey;

    /**
     * Indicates whether Row-Level Security (RLS) is enabled on this table.
     */
    private boolean hasRls;

    /**
     * The tablespace where this table's data is stored, or null for the default tablespace.
     */
    private String tableSpace;

    /**
     * The list of column definitions in this table, ordered by column position.
     */
    private List<ColumnDefinition> columns = new ArrayList<>();

    /**
     * The primary key constraint definition, or null if no primary key exists.
     */
    private PrimaryKeyDefinition primaryKey;

    /**
     * The list of foreign key constraints referencing other tables.
     */
    private List<ForeignKeyDefinition> foreignKeys = new ArrayList<>();

    /**
     * The list of unique constraints on column combinations.
     */
    private List<UniqueConstraintDefinition> uniqueConstraints = new ArrayList<>();

    /**
     * The list of check constraints enforcing domain logic.
     */
    private List<CheckConstraintDefinition> checkConstraints = new ArrayList<>();

    /**
     * The list of indexes (including those created by constraints).
     */
    private List<IndexDefinition> indexes = new ArrayList<>();

    /**
     * The list of triggers defined on this table.
     */
    private List<TriggerDefinition> triggers = new ArrayList<>();

    /**
     * Constructs an empty TableSchema instance.
     * Primarily used by frameworks for deserialisation.
     */
    public TableSchema() {
    }

    /**
     * Constructs a TableSchema with the specified schema and table names.
     * All constraint and index lists are initialised as empty.
     *
     * @param schemaName the PostgreSQL schema name (e.g., "public")
     * @param tableName the table name within the schema
     */
    public TableSchema(String schemaName, String tableName) {
        this.schemaName = schemaName;
        this.tableName = tableName;
    }

    /**
     * Returns the fully qualified table name in the format "schema.table".
     * <p>
     * This is useful for constructing SQL queries and displaying unambiguous
     * table references in logs or user interfaces.
     * </p>
     *
     * @return the fully qualified table name (e.g., "public.users"), never null
     */
    public String getFullName() {
        return schemaName + "." + tableName;
    }

    /**
     * Searches for a column definition by column name.
     * <p>
     * Performs a case-sensitive comparison against the column name. This is useful
     * for schema comparison when determining if a column exists or has changed.
     * </p>
     *
     * @param columnName the name of the column to find (case-sensitive)
     * @return the matching ColumnDefinition, or null if no column with that name exists
     * @see ColumnDefinition
     */
    public ColumnDefinition findColumn(String columnName) {
        return columns.stream()
                .filter(c -> c.columnName.equals(columnName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Searches for an index definition by index name.
     * <p>
     * Performs a case-sensitive comparison against the index name. This is useful
     * for schema comparison when determining if an index exists or has changed.
     * </p>
     *
     * @param indexName the name of the index to find (case-sensitive)
     * @return the matching IndexDefinition, or null if no index with that name exists
     * @see IndexDefinition
     */
    public IndexDefinition findIndex(String indexName) {
        return indexes.stream()
                .filter(i -> i.indexName.equals(indexName))
                .findFirst()
                .orElse(null);
    }

    // Nested classes

    /**
     * Represents a single column definition within a table schema.
     * <p>
     * This class captures all metadata associated with a table column, including its name,
     * data type, nullability, default values, identity properties (for auto-generated values),
     * collation settings, position within the table, and generated column expressions.
     * </p>
     * <p>
     * Supports PostgreSQL-specific features:
     * </p>
     * <ul>
     *   <li>Identity columns (GENERATED ALWAYS/BY DEFAULT AS IDENTITY)</li>
     *   <li>Generated columns (GENERATED ALWAYS AS ... STORED)</li>
     *   <li>Custom collation for text columns</li>
     *   <li>Column-level comments</li>
     * </ul>
     * <p>
     * Example usage:
     * </p>
     * <pre>{@code
     * ColumnDefinition column = ColumnDefinition.builder()
     *     .columnName("email")
     *     .dataType("character varying(255)")
     *     .nullable(false)
     *     .defaultValue("''::character varying")
     *     .position(3)
     *     .comment("User email address")
     *     .build();
     * }</pre>
     *
     * @see TableSchema
     * @see AttributeDifference
     */
    public static class ColumnDefinition {
        /**
         * The column name.
         */
        private String columnName;

        /**
         * The PostgreSQL data type (e.g., "integer", "character varying(255)", "timestamp with time zone").
         */
        private String dataType;

        /**
         * Indicates whether the column allows NULL values.
         */
        private boolean nullable;

        /**
         * The default value expression, or null if no default is defined.
         */
        private String defaultValue;

        /**
         * Indicates whether this is an identity column (auto-incrementing).
         */
        private boolean isIdentity;

        /**
         * The identity generation type: "ALWAYS" or "BY DEFAULT", or null if not an identity column.
         */
        private String identityType;

        /**
         * The collation name for text columns, or null for default collation.
         */
        private String collation;

        /**
         * The ordinal position of the column within the table (1-based).
         */
        private int position;

        /**
         * Optional column-level comment providing documentation.
         */
        private String comment;

        /**
         * Indicates whether this is a generated column.
         */
        private boolean isGenerated;

        /**
         * The generation expression for generated columns, or null if not generated.
         */
        private String generationExpression;

        /**
         * Creates a builder for constructing ColumnDefinition instances.
         *
         * @return a new builder instance
         * @see Builder
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Builder for constructing {@link ColumnDefinition} instances with a fluent API.
         * <p>
         * Provides convenience methods for setting identity and generated column properties.
         * All setter methods return the builder instance to enable method chaining.
         * </p>
         */
        public static class Builder {
            private final ColumnDefinition col = new ColumnDefinition();

            public Builder columnName(String v) { col.columnName = v; return this; }
            public Builder dataType(String v) { col.dataType = v; return this; }
            public Builder nullable(boolean v) { col.nullable = v; return this; }
            public Builder defaultValue(String v) { col.defaultValue = v; return this; }
            public Builder isIdentity(boolean v) { col.isIdentity = v; return this; }
            public Builder identityType(String v) { col.identityType = v; return this; }

            /**
             * Sets identity properties from the identity type string.
             * If the type is non-null, both isIdentity and identityType are set.
             *
             * @param v the identity type ("ALWAYS" or "BY DEFAULT"), or null for non-identity columns
             * @return this builder instance for method chaining
             */
            public Builder identity(String v) {
                if (v != null) { col.isIdentity = true; col.identityType = v; }
                return this;
            }

            /**
             * Sets generated column properties from the generation expression.
             * If the expression is non-null and non-empty, both isGenerated and generationExpression are set.
             *
             * @param v the generation expression, or null for non-generated columns
             * @return this builder instance for method chaining
             */
            public Builder generated(String v) {
                col.isGenerated = (v != null && !v.isEmpty());
                col.generationExpression = v;
                return this;
            }

            public Builder ordinalPosition(int v) { col.position = v; return this; }
            public Builder collation(String v) { col.collation = v; return this; }
            public Builder position(int v) { col.position = v; return this; }
            public Builder comment(String v) { col.comment = v; return this; }
            public Builder isGenerated(boolean v) { col.isGenerated = v; return this; }
            public Builder generationExpression(String v) { col.generationExpression = v; return this; }

            /**
             * Builds and returns the configured ColumnDefinition instance.
             *
             * @return the constructed ColumnDefinition
             */
            public ColumnDefinition build() { return col; }
        }

        /**
         * Constructs an empty ColumnDefinition instance.
         * Primarily used by frameworks for deserialisation or by the builder pattern.
         */
        public ColumnDefinition() {
        }

        /**
         * Constructs a ColumnDefinition with the specified name, data type, and nullability.
         *
         * @param columnName the column name
         * @param dataType the PostgreSQL data type
         * @param nullable true if the column allows NULL values, false otherwise
         */
        public ColumnDefinition(String columnName, String dataType, boolean nullable) {
            this.columnName = columnName;
            this.dataType = dataType;
            this.nullable = nullable;
        }

        /**
         * Compares this column definition with another for structural equality.
         * <p>
         * Two columns are considered structurally equal if they have the same name, data type,
         * nullability, default value, and identity status. This method is used during schema
         * comparison to determine if a column has changed between database instances.
         * </p>
         *
         * @param other the other column definition to compare with
         * @return true if the columns are structurally identical, false otherwise
         * @see #getDifferencesFrom(ColumnDefinition)
         */
        public boolean equalsStructure(ColumnDefinition other) {
            if (other == null) return false;
            return Objects.equals(columnName, other.columnName)
                    && Objects.equals(dataType, other.dataType)
                    && nullable == other.nullable
                    && Objects.equals(defaultValue, other.defaultValue)
                    && isIdentity == other.isIdentity;
        }

        /**
         * Computes detailed attribute-level differences between this column and another.
         * <p>
         * Returns a list of {@link AttributeDifference} objects, each representing a specific
         * attribute that differs between the two column definitions. This method examines:
         * </p>
         * <ul>
         *   <li>Data type changes (marked as breaking)</li>
         *   <li>Nullability changes (marked as breaking if changing from nullable to NOT NULL)</li>
         *   <li>Default value changes (non-breaking)</li>
         *   <li>Identity property changes (non-breaking)</li>
         * </ul>
         *
         * @param other the other column definition to compare with
         * @return a list of attribute differences, empty if the columns are identical or other is null
         * @see AttributeDifference
         * @see #equalsStructure(ColumnDefinition)
         */
        public List<AttributeDifference> getDifferencesFrom(ColumnDefinition other) {
            List<AttributeDifference> diffs = new ArrayList<>();
            if (other == null) {
                return diffs;
            }

            if (!Objects.equals(dataType, other.dataType)) {
                diffs.add(new AttributeDifference("Data Type", dataType, other.dataType, true));
            }
            if (nullable != other.nullable) {
                diffs.add(new AttributeDifference("Nullable",
                        nullable ? "YES" : "NO",
                        other.nullable ? "YES" : "NO",
                        !nullable && other.nullable)); // Breaking if making NOT NULL
            }
            if (!Objects.equals(defaultValue, other.defaultValue)) {
                diffs.add(new AttributeDifference("Default", defaultValue, other.defaultValue, false));
            }
            if (isIdentity != other.isIdentity) {
                diffs.add(new AttributeDifference("Identity",
                        isIdentity ? identityType : "NO",
                        other.isIdentity ? other.identityType : "NO",
                        false));
            }

            return diffs;
        }

        // Getters and Setters
        public String getColumnName() { return columnName; }
        public void setColumnName(String columnName) { this.columnName = columnName; }
        public String getDataType() { return dataType; }
        public void setDataType(String dataType) { this.dataType = dataType; }
        public boolean isNullable() { return nullable; }
        public void setNullable(boolean nullable) { this.nullable = nullable; }
        public String getDefaultValue() { return defaultValue; }
        public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
        public boolean isIdentity() { return isIdentity; }
        public void setIdentity(boolean identity) { isIdentity = identity; }
        public String getIdentityType() { return identityType; }
        public void setIdentityType(String identityType) { this.identityType = identityType; }
        public String getCollation() { return collation; }
        public void setCollation(String collation) { this.collation = collation; }
        public int getPosition() { return position; }
        public void setPosition(int position) { this.position = position; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
        public boolean isGenerated() { return isGenerated; }
        public void setGenerated(boolean generated) { isGenerated = generated; }
        public String getGenerationExpression() { return generationExpression; }
        public void setGenerationExpression(String generationExpression) { this.generationExpression = generationExpression; }
        public String getIdentity() { return isIdentity ? identityType : null; }
    }

    /**
     * Represents a primary key constraint definition.
     * <p>
     * A primary key uniquely identifies each row in a table and enforces both uniqueness
     * and non-nullability on the specified column(s). PostgreSQL automatically creates a
     * unique B-tree index to support the constraint.
     * </p>
     * <p>
     * Example usage:
     * </p>
     * <pre>{@code
     * PrimaryKeyDefinition pk = PrimaryKeyDefinition.builder()
     *     .constraintName("users_pkey")
     *     .columns(List.of("id"))
     *     .indexTableSpace("pg_default")
     *     .build();
     * }</pre>
     *
     * @see TableSchema
     * @see IndexDefinition
     */
    public static class PrimaryKeyDefinition {
        /**
         * The constraint name (e.g., "users_pkey").
         */
        private String constraintName;

        /**
         * The list of column names that comprise the primary key, in order.
         */
        private List<String> columns = new ArrayList<>();

        /**
         * The tablespace where the supporting index is stored, or null for the default tablespace.
         */
        private String indexTableSpace;

        /**
         * Creates a builder for constructing PrimaryKeyDefinition instances.
         *
         * @return a new builder instance
         * @see Builder
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Builder for constructing {@link PrimaryKeyDefinition} instances with a fluent API.
         */
        public static class Builder {
            private final PrimaryKeyDefinition pk = new PrimaryKeyDefinition();

            public Builder constraintName(String v) { pk.constraintName = v; return this; }
            public Builder columns(List<String> v) { pk.columns = v; return this; }
            public Builder indexTableSpace(String v) { pk.indexTableSpace = v; return this; }

            /**
             * Builds and returns the configured PrimaryKeyDefinition instance.
             *
             * @return the constructed PrimaryKeyDefinition
             */
            public PrimaryKeyDefinition build() { return pk; }
        }

        /**
         * Constructs an empty PrimaryKeyDefinition instance.
         * Primarily used by frameworks for deserialisation or by the builder pattern.
         */
        public PrimaryKeyDefinition() {
        }

        /**
         * Constructs a PrimaryKeyDefinition with the specified constraint name and columns.
         *
         * @param constraintName the constraint name
         * @param columns the list of column names in the primary key
         */
        public PrimaryKeyDefinition(String constraintName, List<String> columns) {
            this.constraintName = constraintName;
            this.columns = columns;
        }

        /**
         * Compares this primary key definition with another for structural equality.
         * <p>
         * Two primary keys are considered structurally equal if they have the same
         * column list in the same order. The constraint name is not considered.
         * </p>
         *
         * @param other the other primary key definition to compare with
         * @return true if the primary keys are structurally identical, false otherwise
         * @see #getDifferencesFrom(PrimaryKeyDefinition)
         */
        public boolean equalsStructure(PrimaryKeyDefinition other) {
            if (other == null) return false;
            return Objects.equals(columns, other.columns);
        }

        /**
         * Returns a comma-separated display string of the primary key columns.
         *
         * @return the column names joined with ", " (e.g., "id" or "account_id, user_id")
         */
        public String getColumnsDisplay() {
            return String.join(", ", columns);
        }

        /**
         * Computes detailed attribute-level differences between this primary key and another.
         * <p>
         * Changes to primary key columns are marked as breaking changes since they affect
         * the table's uniqueness constraint and may require data migration.
         * </p>
         *
         * @param other the other primary key definition to compare with
         * @return a list of attribute differences, empty if the primary keys are identical or other is null
         * @see AttributeDifference
         */
        public List<AttributeDifference> getDifferencesFrom(PrimaryKeyDefinition other) {
            List<AttributeDifference> diffs = new ArrayList<>();
            if (other == null) return diffs;
            if (!Objects.equals(columns, other.columns)) {
                diffs.add(new AttributeDifference("Columns", getColumnsDisplay(), other.getColumnsDisplay(), true));
            }
            return diffs;
        }

        // Getters and Setters
        public String getConstraintName() { return constraintName; }
        public void setConstraintName(String constraintName) { this.constraintName = constraintName; }
        public List<String> getColumns() { return columns; }
        public void setColumns(List<String> columns) { this.columns = columns; }
        public String getIndexTableSpace() { return indexTableSpace; }
        public void setIndexTableSpace(String indexTableSpace) { this.indexTableSpace = indexTableSpace; }
    }

    /**
     * Represents a foreign key constraint definition.
     * <p>
     * A foreign key establishes a referential integrity relationship between this table's
     * column(s) and another table's column(s). It enforces that values in the foreign key
     * columns must exist in the referenced table's primary key or unique constraint.
     * </p>
     * <p>
     * PostgreSQL supports various referential actions:
     * </p>
     * <ul>
     *   <li>ON DELETE/UPDATE CASCADE - propagate changes to child rows</li>
     *   <li>ON DELETE/UPDATE SET NULL - set child columns to NULL</li>
     *   <li>ON DELETE/UPDATE SET DEFAULT - set child columns to default value</li>
     *   <li>ON DELETE/UPDATE RESTRICT - prevent deletion/update if child rows exist</li>
     *   <li>ON DELETE/UPDATE NO ACTION - same as RESTRICT but deferrable</li>
     * </ul>
     * <p>
     * Example usage:
     * </p>
     * <pre>{@code
     * ForeignKeyDefinition fk = ForeignKeyDefinition.builder()
     *     .constraintName("orders_customer_id_fkey")
     *     .columns(List.of("customer_id"))
     *     .referencedSchema("public")
     *     .referencedTable("customers")
     *     .referencedColumns(List.of("id"))
     *     .onDelete("CASCADE")
     *     .onUpdate("NO ACTION")
     *     .build();
     * }</pre>
     *
     * @see TableSchema
     * @see PrimaryKeyDefinition
     * @see UniqueConstraintDefinition
     */
    public static class ForeignKeyDefinition {
        /**
         * The constraint name (e.g., "orders_customer_id_fkey").
         */
        private String constraintName;

        /**
         * The list of column names in this table that comprise the foreign key.
         */
        private List<String> columns = new ArrayList<>();

        /**
         * The schema name of the referenced table.
         */
        private String referencedSchema;

        /**
         * The table name being referenced.
         */
        private String referencedTable;

        /**
         * The list of column names in the referenced table.
         */
        private List<String> referencedColumns = new ArrayList<>();

        /**
         * The action to take when a referenced row is deleted (e.g., "CASCADE", "SET NULL", "RESTRICT").
         */
        private String onDelete;

        /**
         * The action to take when a referenced row's key is updated (e.g., "CASCADE", "SET NULL", "NO ACTION").
         */
        private String onUpdate;

        /**
         * Indicates whether the constraint is deferrable (can be postponed until transaction commit).
         */
        private boolean deferrable;

        /**
         * Indicates whether the constraint check is initially deferred to transaction commit.
         */
        private boolean initiallyDeferred;

        /**
         * Creates a builder for constructing ForeignKeyDefinition instances.
         *
         * @return a new builder instance
         * @see Builder
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Builder for constructing {@link ForeignKeyDefinition} instances with a fluent API.
         */
        public static class Builder {
            private final ForeignKeyDefinition fk = new ForeignKeyDefinition();

            public Builder constraintName(String v) { fk.constraintName = v; return this; }
            public Builder columns(List<String> v) { fk.columns = v; return this; }
            public Builder referencedSchema(String v) { fk.referencedSchema = v; return this; }
            public Builder referencedTable(String v) { fk.referencedTable = v; return this; }
            public Builder referencedColumns(List<String> v) { fk.referencedColumns = v; return this; }
            public Builder onDelete(String v) { fk.onDelete = v; return this; }
            public Builder onUpdate(String v) { fk.onUpdate = v; return this; }
            public Builder deferrable(boolean v) { fk.deferrable = v; return this; }
            public Builder initiallyDeferred(boolean v) { fk.initiallyDeferred = v; return this; }

            /**
             * Builds and returns the configured ForeignKeyDefinition instance.
             *
             * @return the constructed ForeignKeyDefinition
             */
            public ForeignKeyDefinition build() { return fk; }
        }

        /**
         * Constructs an empty ForeignKeyDefinition instance.
         * Primarily used by frameworks for deserialisation or by the builder pattern.
         */
        public ForeignKeyDefinition() {
        }

        /**
         * Compares this foreign key definition with another for structural equality.
         * <p>
         * Two foreign keys are considered structurally equal if they have the same columns,
         * referenced schema and table, referenced columns, and referential actions (ON DELETE/UPDATE).
         * The constraint name and deferrable settings are not considered.
         * </p>
         *
         * @param other the other foreign key definition to compare with
         * @return true if the foreign keys are structurally identical, false otherwise
         * @see #getDifferencesFrom(ForeignKeyDefinition)
         */
        public boolean equalsStructure(ForeignKeyDefinition other) {
            if (other == null) return false;
            return Objects.equals(columns, other.columns)
                    && Objects.equals(referencedSchema, other.referencedSchema)
                    && Objects.equals(referencedTable, other.referencedTable)
                    && Objects.equals(referencedColumns, other.referencedColumns)
                    && Objects.equals(onDelete, other.onDelete)
                    && Objects.equals(onUpdate, other.onUpdate);
        }

        /**
         * Computes detailed attribute-level differences between this foreign key and another.
         * <p>
         * Examines differences in columns, referenced table, and referential actions.
         * Column and table reference changes are marked as breaking changes.
         * </p>
         *
         * @param other the other foreign key definition to compare with
         * @return a list of attribute differences, empty if the foreign keys are identical or other is null
         * @see AttributeDifference
         */
        public List<AttributeDifference> getDifferencesFrom(ForeignKeyDefinition other) {
            List<AttributeDifference> diffs = new ArrayList<>();
            if (other == null) return diffs;
            if (!Objects.equals(columns, other.columns)) {
                diffs.add(new AttributeDifference("Columns", getColumnsDisplay(), other.getColumnsDisplay(), true));
            }
            if (!Objects.equals(referencedTable, other.referencedTable)) {
                diffs.add(new AttributeDifference("Referenced Table", getReferencedFullName(), other.getReferencedFullName(), true));
            }
            if (!Objects.equals(onDelete, other.onDelete)) {
                diffs.add(new AttributeDifference("ON DELETE", onDelete, other.onDelete, false));
            }
            if (!Objects.equals(onUpdate, other.onUpdate)) {
                diffs.add(new AttributeDifference("ON UPDATE", onUpdate, other.onUpdate, false));
            }
            return diffs;
        }

        /**
         * Returns the fully qualified name of the referenced table.
         *
         * @return the referenced table name in "schema.table" format (e.g., "public.customers")
         */
        public String getReferencedFullName() {
            return referencedSchema + "." + referencedTable;
        }

        /**
         * Returns a comma-separated display string of the foreign key columns.
         *
         * @return the column names joined with ", " (e.g., "customer_id" or "user_id, account_id")
         */
        public String getColumnsDisplay() {
            return String.join(", ", columns);
        }

        /**
         * Returns a comma-separated display string of the referenced columns.
         *
         * @return the referenced column names joined with ", " (e.g., "id" or "pk_col1, pk_col2")
         */
        public String getReferencedColumnsDisplay() {
            return String.join(", ", referencedColumns);
        }

        // Getters and Setters
        public String getConstraintName() { return constraintName; }
        public void setConstraintName(String constraintName) { this.constraintName = constraintName; }
        public List<String> getColumns() { return columns; }
        public void setColumns(List<String> columns) { this.columns = columns; }
        public String getReferencedSchema() { return referencedSchema; }
        public void setReferencedSchema(String referencedSchema) { this.referencedSchema = referencedSchema; }
        public String getReferencedTable() { return referencedTable; }
        public void setReferencedTable(String referencedTable) { this.referencedTable = referencedTable; }
        public List<String> getReferencedColumns() { return referencedColumns; }
        public void setReferencedColumns(List<String> referencedColumns) { this.referencedColumns = referencedColumns; }
        public String getOnDelete() { return onDelete; }
        public void setOnDelete(String onDelete) { this.onDelete = onDelete; }
        public String getOnUpdate() { return onUpdate; }
        public void setOnUpdate(String onUpdate) { this.onUpdate = onUpdate; }
        public boolean isDeferrable() { return deferrable; }
        public void setDeferrable(boolean deferrable) { this.deferrable = deferrable; }
        public boolean isInitiallyDeferred() { return initiallyDeferred; }
        public void setInitiallyDeferred(boolean initiallyDeferred) { this.initiallyDeferred = initiallyDeferred; }
    }

    /**
     * Represents a unique constraint definition.
     * <p>
     * A unique constraint ensures that the values in the specified column(s) are unique
     * across all rows in the table. PostgreSQL automatically creates a unique B-tree index
     * to support the constraint. Unlike primary keys, unique constraints allow NULL values.
     * </p>
     * <p>
     * Example usage:
     * </p>
     * <pre>{@code
     * UniqueConstraintDefinition unique = UniqueConstraintDefinition.builder()
     *     .constraintName("users_email_key")
     *     .columns(List.of("email"))
     *     .build();
     * }</pre>
     *
     * @see TableSchema
     * @see PrimaryKeyDefinition
     * @see IndexDefinition
     */
    public static class UniqueConstraintDefinition {
        /**
         * The constraint name (e.g., "users_email_key").
         */
        private String constraintName;

        /**
         * The list of column names that must be unique together.
         */
        private List<String> columns = new ArrayList<>();

        /**
         * Creates a builder for constructing UniqueConstraintDefinition instances.
         *
         * @return a new builder instance
         * @see Builder
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Builder for constructing {@link UniqueConstraintDefinition} instances with a fluent API.
         */
        public static class Builder {
            private final UniqueConstraintDefinition uc = new UniqueConstraintDefinition();

            public Builder constraintName(String v) { uc.constraintName = v; return this; }
            public Builder columns(List<String> v) { uc.columns = v; return this; }

            /**
             * Builds and returns the configured UniqueConstraintDefinition instance.
             *
             * @return the constructed UniqueConstraintDefinition
             */
            public UniqueConstraintDefinition build() { return uc; }
        }

        /**
         * Constructs an empty UniqueConstraintDefinition instance.
         * Primarily used by frameworks for deserialisation or by the builder pattern.
         */
        public UniqueConstraintDefinition() {
        }

        /**
         * Constructs a UniqueConstraintDefinition with the specified constraint name and columns.
         *
         * @param constraintName the constraint name
         * @param columns the list of column names that must be unique together
         */
        public UniqueConstraintDefinition(String constraintName, List<String> columns) {
            this.constraintName = constraintName;
            this.columns = columns;
        }

        /**
         * Compares this unique constraint definition with another for structural equality.
         * <p>
         * Two unique constraints are considered structurally equal if they have the same
         * column list in the same order. The constraint name is not considered.
         * </p>
         *
         * @param other the other unique constraint definition to compare with
         * @return true if the unique constraints are structurally identical, false otherwise
         */
        public boolean equalsStructure(UniqueConstraintDefinition other) {
            if (other == null) return false;
            return Objects.equals(columns, other.columns);
        }

        /**
         * Returns a comma-separated display string of the unique constraint columns.
         *
         * @return the column names joined with ", " (e.g., "email" or "account_id, username")
         */
        public String getColumnsDisplay() {
            return String.join(", ", columns);
        }

        // Getters and Setters
        public String getConstraintName() { return constraintName; }
        public void setConstraintName(String constraintName) { this.constraintName = constraintName; }
        public List<String> getColumns() { return columns; }
        public void setColumns(List<String> columns) { this.columns = columns; }
    }

    /**
     * Represents a check constraint definition.
     * <p>
     * A check constraint enforces a boolean condition that must evaluate to TRUE or NULL
     * for all rows in the table. It is used to implement domain logic and business rules
     * directly at the database level.
     * </p>
     * <p>
     * PostgreSQL supports the NO INHERIT option for check constraints, which prevents
     * child tables (in table inheritance hierarchies) from inheriting the constraint.
     * </p>
     * <p>
     * Example usage:
     * </p>
     * <pre>{@code
     * CheckConstraintDefinition check = CheckConstraintDefinition.builder()
     *     .constraintName("accounts_balance_positive")
     *     .expression("(balance >= 0)")
     *     .noInherit(false)
     *     .build();
     * }</pre>
     *
     * @see TableSchema
     */
    public static class CheckConstraintDefinition {
        /**
         * The constraint name (e.g., "accounts_balance_positive").
         */
        private String constraintName;

        /**
         * The boolean expression that must evaluate to TRUE or NULL (e.g., "(balance >= 0)").
         */
        private String expression;

        /**
         * Indicates whether the constraint is marked NO INHERIT (not inherited by child tables).
         */
        private boolean noInherit;

        /**
         * Creates a builder for constructing CheckConstraintDefinition instances.
         *
         * @return a new builder instance
         * @see Builder
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Builder for constructing {@link CheckConstraintDefinition} instances with a fluent API.
         */
        public static class Builder {
            private final CheckConstraintDefinition cc = new CheckConstraintDefinition();

            public Builder constraintName(String v) { cc.constraintName = v; return this; }
            public Builder expression(String v) { cc.expression = v; return this; }
            public Builder noInherit(boolean v) { cc.noInherit = v; return this; }

            /**
             * Builds and returns the configured CheckConstraintDefinition instance.
             *
             * @return the constructed CheckConstraintDefinition
             */
            public CheckConstraintDefinition build() { return cc; }
        }

        /**
         * Constructs an empty CheckConstraintDefinition instance.
         * Primarily used by frameworks for deserialisation or by the builder pattern.
         */
        public CheckConstraintDefinition() {
        }

        /**
         * Constructs a CheckConstraintDefinition with the specified constraint name and expression.
         *
         * @param constraintName the constraint name
         * @param expression the boolean expression to enforce
         */
        public CheckConstraintDefinition(String constraintName, String expression) {
            this.constraintName = constraintName;
            this.expression = expression;
        }

        /**
         * Compares this check constraint definition with another for structural equality.
         * <p>
         * Two check constraints are considered structurally equal if they have the same
         * expression after normalising whitespace. The constraint name is not considered.
         * Whitespace normalisation converts multiple spaces to single spaces and trims
         * leading/trailing whitespace to handle formatting differences.
         * </p>
         *
         * @param other the other check constraint definition to compare with
         * @return true if the check constraints are structurally identical, false otherwise
         */
        public boolean equalsStructure(CheckConstraintDefinition other) {
            if (other == null) return false;
            // Normalise whitespace for comparison
            String normThis = expression != null ? expression.replaceAll("\\s+", " ").trim() : null;
            String normOther = other.expression != null ? other.expression.replaceAll("\\s+", " ").trim() : null;
            return Objects.equals(normThis, normOther);
        }

        // Getters and Setters
        public String getConstraintName() { return constraintName; }
        public void setConstraintName(String constraintName) { this.constraintName = constraintName; }
        public String getExpression() { return expression; }
        public void setExpression(String expression) { this.expression = expression; }
        public boolean isNoInherit() { return noInherit; }
        public void setNoInherit(boolean noInherit) { this.noInherit = noInherit; }
    }

    /**
     * Represents an index definition.
     * <p>
     * An index improves query performance by creating a data structure that allows faster
     * lookups on specified column(s). PostgreSQL supports several index types, each optimised
     * for different use cases:
     * </p>
     * <ul>
     *   <li><b>B-tree (btree)</b> - Default; handles equality and range queries</li>
     *   <li><b>Hash</b> - Optimised for simple equality comparisons</li>
     *   <li><b>GiST (gist)</b> - Generalised Search Tree for geometric and full-text data</li>
     *   <li><b>GIN (gin)</b> - Generalised Inverted Index for array and JSONB data</li>
     *   <li><b>BRIN (brin)</b> - Block Range Index for large, naturally sorted tables</li>
     * </ul>
     * <p>
     * Indexes can be unique (enforcing uniqueness), partial (with a WHERE clause), or
     * expression-based (indexing computed values). PostgreSQL 11+ supports INCLUDE columns,
     * which are stored in the index but not part of the key, enabling index-only scans.
     * </p>
     * <p>
     * Example usage:
     * </p>
     * <pre>{@code
     * IndexDefinition index = IndexDefinition.builder()
     *     .indexName("users_email_idx")
     *     .indexType("btree")
     *     .columns(List.of("email"))
     *     .unique(true)
     *     .whereClause("(deleted_at IS NULL)")
     *     .build();
     * }</pre>
     *
     * @see TableSchema
     * @see PrimaryKeyDefinition
     * @see UniqueConstraintDefinition
     */
    public static class IndexDefinition {
        /**
         * The index name (e.g., "users_email_idx").
         */
        private String indexName;

        /**
         * The index type: "btree", "hash", "gist", "gin", or "brin".
         */
        private String indexType;

        /**
         * The list of indexed column names or expressions.
         */
        private List<String> columns = new ArrayList<>();

        /**
         * The list of INCLUDE columns (stored in the index but not part of the key).
         */
        private List<String> includeColumns = new ArrayList<>();

        /**
         * Indicates whether this is a unique index.
         */
        private boolean unique;

        /**
         * Indicates whether this index was created to support a primary key constraint.
         */
        private boolean primary;

        /**
         * The WHERE clause for partial indexes, or null if not partial.
         */
        private String whereClause;

        /**
         * The expression definition for expression-based indexes.
         */
        private String expressionDef;

        /**
         * The tablespace where this index is stored, or null for the default tablespace.
         */
        private String tableSpace;

        /**
         * The full index definition SQL statement.
         */
        private String definition;

        /**
         * Creates a builder for constructing IndexDefinition instances.
         *
         * @return a new builder instance
         * @see Builder
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Builder for constructing {@link IndexDefinition} instances with a fluent API.
         */
        public static class Builder {
            private final IndexDefinition idx = new IndexDefinition();

            public Builder indexName(String v) { idx.indexName = v; return this; }
            public Builder indexType(String v) { idx.indexType = v; return this; }
            public Builder columns(List<String> v) { idx.columns = v; return this; }
            public Builder includeColumns(List<String> v) { idx.includeColumns = v; return this; }
            public Builder unique(boolean v) { idx.unique = v; return this; }
            public Builder primary(boolean v) { idx.primary = v; return this; }
            public Builder whereClause(String v) { idx.whereClause = v; return this; }
            public Builder expressionDef(String v) { idx.expressionDef = v; return this; }
            public Builder tableSpace(String v) { idx.tableSpace = v; return this; }
            public Builder definition(String v) { idx.definition = v; return this; }

            /**
             * Builds and returns the configured IndexDefinition instance.
             *
             * @return the constructed IndexDefinition
             */
            public IndexDefinition build() { return idx; }
        }

        /**
         * Constructs an empty IndexDefinition instance.
         * Primarily used by frameworks for deserialisation or by the builder pattern.
         */
        public IndexDefinition() {
        }

        /**
         * Compares this index definition with another for structural equality.
         * <p>
         * Two indexes are considered structurally equal if they have the same index type,
         * columns, INCLUDE columns, uniqueness, and WHERE clause (normalised). The index
         * name is not considered.
         * </p>
         *
         * @param other the other index definition to compare with
         * @return true if the indexes are structurally identical, false otherwise
         * @see #getDifferencesFrom(IndexDefinition)
         */
        public boolean equalsStructure(IndexDefinition other) {
            if (other == null) return false;
            return Objects.equals(indexType, other.indexType)
                    && Objects.equals(columns, other.columns)
                    && Objects.equals(includeColumns, other.includeColumns)
                    && unique == other.unique
                    && Objects.equals(normaliseWhereClause(whereClause), normaliseWhereClause(other.whereClause));
        }

        /**
         * Normalises a WHERE clause by converting multiple spaces to single spaces and trimming.
         *
         * @param clause the WHERE clause to normalise
         * @return the normalised clause, or null if the input is null
         */
        private String normaliseWhereClause(String clause) {
            return clause != null ? clause.replaceAll("\\s+", " ").trim() : null;
        }

        /**
         * Returns a comma-separated display string of the indexed columns.
         *
         * @return the column names joined with ", " (e.g., "email" or "last_name, first_name")
         */
        public String getColumnsDisplay() {
            return String.join(", ", columns);
        }

        /**
         * Determines whether this is a partial index.
         * <p>
         * A partial index includes only a subset of table rows, defined by the WHERE clause.
         * </p>
         *
         * @return true if the index has a non-empty WHERE clause, false otherwise
         */
        public boolean isPartial() {
            return whereClause != null && !whereClause.isEmpty();
        }

        /**
         * Determines whether this is an expression-based index.
         * <p>
         * An expression index indexes computed values rather than column values directly.
         * </p>
         *
         * @return true if the index has a non-empty expression definition, false otherwise
         */
        public boolean isExpression() {
            return expressionDef != null && !expressionDef.isEmpty();
        }

        /**
         * Computes detailed attribute-level differences between this index and another.
         * <p>
         * Examines differences in index type, columns, uniqueness, and WHERE clause.
         * Changes to index type, columns, and uniqueness are marked as breaking changes.
         * </p>
         *
         * @param other the other index definition to compare with
         * @return a list of attribute differences, empty if the indexes are identical or other is null
         * @see AttributeDifference
         */
        public List<AttributeDifference> getDifferencesFrom(IndexDefinition other) {
            List<AttributeDifference> diffs = new ArrayList<>();
            if (other == null) return diffs;
            if (!Objects.equals(indexType, other.indexType)) {
                diffs.add(new AttributeDifference("Index Type", indexType, other.indexType, true));
            }
            if (!Objects.equals(columns, other.columns)) {
                diffs.add(new AttributeDifference("Columns", getColumnsDisplay(), other.getColumnsDisplay(), true));
            }
            if (unique != other.unique) {
                diffs.add(new AttributeDifference("Unique", unique ? "YES" : "NO", other.unique ? "YES" : "NO", true));
            }
            String normThis = normaliseWhereClause(whereClause);
            String normOther = normaliseWhereClause(other.whereClause);
            if (!Objects.equals(normThis, normOther)) {
                diffs.add(new AttributeDifference("WHERE Clause", whereClause, other.whereClause, false));
            }
            return diffs;
        }

        // Getters and Setters
        public String getIndexName() { return indexName; }
        public void setIndexName(String indexName) { this.indexName = indexName; }
        public String getIndexType() { return indexType; }
        public void setIndexType(String indexType) { this.indexType = indexType; }
        public List<String> getColumns() { return columns; }
        public void setColumns(List<String> columns) { this.columns = columns; }
        public List<String> getIncludeColumns() { return includeColumns; }
        public void setIncludeColumns(List<String> includeColumns) { this.includeColumns = includeColumns; }
        public boolean isUnique() { return unique; }
        public void setUnique(boolean unique) { this.unique = unique; }
        public boolean isPrimary() { return primary; }
        public void setPrimary(boolean primary) { this.primary = primary; }
        public String getWhereClause() { return whereClause; }
        public void setWhereClause(String whereClause) { this.whereClause = whereClause; }
        public String getExpressionDef() { return expressionDef; }
        public void setExpressionDef(String expressionDef) { this.expressionDef = expressionDef; }
        public String getTableSpace() { return tableSpace; }
        public void setTableSpace(String tableSpace) { this.tableSpace = tableSpace; }
        public String getDefinition() { return definition; }
        public void setDefinition(String definition) { this.definition = definition; }
    }

    /**
     * Represents a trigger definition on a table.
     * <p>
     * A trigger is a database callback function that is automatically executed in response
     * to certain events on a table. PostgreSQL triggers can fire BEFORE, AFTER, or INSTEAD OF
     * data modification events (INSERT, UPDATE, DELETE, TRUNCATE), and can execute at either
     * the row level or statement level.
     * </p>
     * <p>
     * Trigger timing options:
     * </p>
     * <ul>
     *   <li><b>BEFORE</b> - Trigger fires before the event, can modify or prevent the operation</li>
     *   <li><b>AFTER</b> - Trigger fires after the event completes</li>
     *   <li><b>INSTEAD OF</b> - Trigger replaces the event (used for views)</li>
     * </ul>
     * <p>
     * Trigger level options:
     * </p>
     * <ul>
     *   <li><b>ROW</b> - Trigger fires once for each affected row</li>
     *   <li><b>STATEMENT</b> - Trigger fires once per SQL statement</li>
     * </ul>
     * <p>
     * Triggers can optionally specify a WHEN condition to fire only when certain criteria are met.
     * </p>
     * <p>
     * Example usage:
     * </p>
     * <pre>{@code
     * TriggerDefinition trigger = TriggerDefinition.builder()
     *     .triggerName("update_modified_at")
     *     .timing("BEFORE")
     *     .events("UPDATE")
     *     .level("ROW")
     *     .functionSchema("public")
     *     .functionName("set_modified_at")
     *     .enabled(true)
     *     .build();
     * }</pre>
     *
     * @see TableSchema
     */
    public static class TriggerDefinition {
        /**
         * The trigger name (e.g., "update_modified_at").
         */
        private String triggerName;

        /**
         * The trigger timing: "BEFORE", "AFTER", or "INSTEAD OF".
         */
        private String timing;

        /**
         * The trigger events as a comma-separated list (e.g., "INSERT, UPDATE", "DELETE").
         */
        private String events;

        /**
         * The trigger level: "ROW" or "STATEMENT".
         */
        private String level;

        /**
         * The name of the trigger function to execute.
         */
        private String functionName;

        /**
         * The schema containing the trigger function.
         */
        private String functionSchema;

        /**
         * The optional WHEN condition that must be true for the trigger to fire.
         */
        private String condition;

        /**
         * Indicates whether the trigger is currently enabled.
         */
        private boolean enabled;

        /**
         * The full trigger definition SQL statement.
         */
        private String definition;

        /**
         * Creates a builder for constructing TriggerDefinition instances.
         *
         * @return a new builder instance
         * @see Builder
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Builder for constructing {@link TriggerDefinition} instances with a fluent API.
         */
        public static class Builder {
            private final TriggerDefinition trg = new TriggerDefinition();

            public Builder triggerName(String v) { trg.triggerName = v; return this; }
            public Builder timing(String v) { trg.timing = v; return this; }
            public Builder events(String v) { trg.events = v; return this; }
            public Builder level(String v) { trg.level = v; return this; }
            public Builder functionName(String v) { trg.functionName = v; return this; }
            public Builder functionSchema(String v) { trg.functionSchema = v; return this; }
            public Builder condition(String v) { trg.condition = v; return this; }
            public Builder enabled(boolean v) { trg.enabled = v; return this; }
            public Builder definition(String v) { trg.definition = v; return this; }

            /**
             * Builds and returns the configured TriggerDefinition instance.
             *
             * @return the constructed TriggerDefinition
             */
            public TriggerDefinition build() { return trg; }
        }

        /**
         * Constructs an empty TriggerDefinition instance.
         * Primarily used by frameworks for deserialisation or by the builder pattern.
         */
        public TriggerDefinition() {
        }

        /**
         * Compares this trigger definition with another for structural equality.
         * <p>
         * Two triggers are considered structurally equal if they have the same timing, events,
         * level, function name, and condition. The trigger name and enabled status are not considered.
         * </p>
         *
         * @param other the other trigger definition to compare with
         * @return true if the triggers are structurally identical, false otherwise
         * @see #getDifferencesFrom(TriggerDefinition)
         */
        public boolean equalsStructure(TriggerDefinition other) {
            if (other == null) return false;
            return Objects.equals(timing, other.timing)
                    && Objects.equals(events, other.events)
                    && Objects.equals(level, other.level)
                    && Objects.equals(functionName, other.functionName)
                    && Objects.equals(condition, other.condition);
        }

        /**
         * Computes detailed attribute-level differences between this trigger and another.
         * <p>
         * Examines differences in timing, events, level, function, condition, and enabled status.
         * Changes to timing, events, level, and function are marked as breaking changes.
         * </p>
         *
         * @param other the other trigger definition to compare with
         * @return a list of attribute differences, empty if the triggers are identical or other is null
         * @see AttributeDifference
         */
        public List<AttributeDifference> getDifferencesFrom(TriggerDefinition other) {
            List<AttributeDifference> diffs = new ArrayList<>();
            if (other == null) return diffs;
            if (!Objects.equals(timing, other.timing)) {
                diffs.add(new AttributeDifference("Timing", timing, other.timing, true));
            }
            if (!Objects.equals(events, other.events)) {
                diffs.add(new AttributeDifference("Events", events, other.events, true));
            }
            if (!Objects.equals(level, other.level)) {
                diffs.add(new AttributeDifference("Level", level, other.level, true));
            }
            if (!Objects.equals(functionName, other.functionName)) {
                diffs.add(new AttributeDifference("Function", getFunctionFullName(), other.getFunctionFullName(), true));
            }
            if (!Objects.equals(condition, other.condition)) {
                diffs.add(new AttributeDifference("Condition", condition, other.condition, false));
            }
            if (enabled != other.enabled) {
                diffs.add(new AttributeDifference("Enabled", enabled ? "YES" : "NO", other.enabled ? "YES" : "NO", false));
            }
            return diffs;
        }

        /**
         * Returns the fully qualified name of the trigger function.
         * <p>
         * If the function schema is specified, returns "schema.function", otherwise returns
         * just the function name.
         * </p>
         *
         * @return the fully qualified function name (e.g., "public.set_modified_at" or "set_modified_at")
         */
        public String getFunctionFullName() {
            return functionSchema != null ? functionSchema + "." + functionName : functionName;
        }

        // Getters and Setters
        public String getTriggerName() { return triggerName; }
        public void setTriggerName(String triggerName) { this.triggerName = triggerName; }
        public String getTiming() { return timing; }
        public void setTiming(String timing) { this.timing = timing; }
        public String getEvents() { return events; }
        public void setEvents(String events) { this.events = events; }
        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
        public String getFunctionName() { return functionName; }
        public void setFunctionName(String functionName) { this.functionName = functionName; }
        public String getFunctionSchema() { return functionSchema; }
        public void setFunctionSchema(String functionSchema) { this.functionSchema = functionSchema; }
        public String getCondition() { return condition; }
        public void setCondition(String condition) { this.condition = condition; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getDefinition() { return definition; }
        public void setDefinition(String definition) { this.definition = definition; }
    }

    // Getters and Setters for TableSchema

    public String getSchemaName() { return schemaName; }
    public void setSchemaName(String schemaName) { this.schemaName = schemaName; }
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public boolean isPartitioned() { return isPartitioned; }
    public void setPartitioned(boolean partitioned) { isPartitioned = partitioned; }
    public boolean isPartition() { return isPartition; }
    public void setPartition(boolean partition) { isPartition = partition; }
    public String getPartitionStrategy() { return partitionStrategy; }
    public void setPartitionStrategy(String partitionStrategy) { this.partitionStrategy = partitionStrategy; }
    public String getPartitionKey() { return partitionKey; }
    public void setPartitionKey(String partitionKey) { this.partitionKey = partitionKey; }
    public boolean isHasRls() { return hasRls; }
    public void setHasRls(boolean hasRls) { this.hasRls = hasRls; }
    public String getTableSpace() { return tableSpace; }
    public void setTableSpace(String tableSpace) { this.tableSpace = tableSpace; }
    public List<ColumnDefinition> getColumns() { return columns; }
    public void setColumns(List<ColumnDefinition> columns) { this.columns = columns; }
    public PrimaryKeyDefinition getPrimaryKey() { return primaryKey; }
    public void setPrimaryKey(PrimaryKeyDefinition primaryKey) { this.primaryKey = primaryKey; }
    public List<ForeignKeyDefinition> getForeignKeys() { return foreignKeys; }
    public void setForeignKeys(List<ForeignKeyDefinition> foreignKeys) { this.foreignKeys = foreignKeys; }
    public List<UniqueConstraintDefinition> getUniqueConstraints() { return uniqueConstraints; }
    public void setUniqueConstraints(List<UniqueConstraintDefinition> uniqueConstraints) { this.uniqueConstraints = uniqueConstraints; }
    public List<CheckConstraintDefinition> getCheckConstraints() { return checkConstraints; }
    public void setCheckConstraints(List<CheckConstraintDefinition> checkConstraints) { this.checkConstraints = checkConstraints; }
    public List<IndexDefinition> getIndexes() { return indexes; }
    public void setIndexes(List<IndexDefinition> indexes) { this.indexes = indexes; }
    public List<TriggerDefinition> getTriggers() { return triggers; }
    public void setTriggers(List<TriggerDefinition> triggers) { this.triggers = triggers; }

    /**
     * Adds a column definition to this table schema.
     *
     * @param column the column definition to add
     */
    public void addColumn(ColumnDefinition column) { this.columns.add(column); }

    /**
     * Adds a foreign key constraint to this table schema.
     *
     * @param fk the foreign key definition to add
     */
    public void addForeignKey(ForeignKeyDefinition fk) { this.foreignKeys.add(fk); }

    /**
     * Adds a unique constraint to this table schema.
     *
     * @param uc the unique constraint definition to add
     */
    public void addUniqueConstraint(UniqueConstraintDefinition uc) { this.uniqueConstraints.add(uc); }

    /**
     * Adds a check constraint to this table schema.
     *
     * @param cc the check constraint definition to add
     */
    public void addCheckConstraint(CheckConstraintDefinition cc) { this.checkConstraints.add(cc); }

    /**
     * Adds an index definition to this table schema.
     *
     * @param idx the index definition to add
     */
    public void addIndex(IndexDefinition idx) { this.indexes.add(idx); }

    /**
     * Adds a trigger definition to this table schema.
     *
     * @param trg the trigger definition to add
     */
    public void addTrigger(TriggerDefinition trg) { this.triggers.add(trg); }
}
