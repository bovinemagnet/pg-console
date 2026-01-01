package com.bovinemagnet.pgconsole.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a table's complete schema definition.
 * <p>
 * Contains all table metadata including columns, constraints, indexes,
 * and triggers. Used for schema comparison between instances.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class TableSchema {

    private String schemaName;
    private String tableName;
    private String owner;
    private String comment;
    private boolean isPartitioned;
    private boolean isPartition;
    private String partitionStrategy;
    private String partitionKey;
    private boolean hasRls;
    private String tableSpace;

    private List<ColumnDefinition> columns = new ArrayList<>();
    private PrimaryKeyDefinition primaryKey;
    private List<ForeignKeyDefinition> foreignKeys = new ArrayList<>();
    private List<UniqueConstraintDefinition> uniqueConstraints = new ArrayList<>();
    private List<CheckConstraintDefinition> checkConstraints = new ArrayList<>();
    private List<IndexDefinition> indexes = new ArrayList<>();
    private List<TriggerDefinition> triggers = new ArrayList<>();

    public TableSchema() {
    }

    public TableSchema(String schemaName, String tableName) {
        this.schemaName = schemaName;
        this.tableName = tableName;
    }

    /**
     * Gets the fully qualified table name.
     *
     * @return schema.table name
     */
    public String getFullName() {
        return schemaName + "." + tableName;
    }

    /**
     * Finds a column by name.
     *
     * @param columnName name of column
     * @return column definition or null
     */
    public ColumnDefinition findColumn(String columnName) {
        return columns.stream()
                .filter(c -> c.columnName.equals(columnName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Finds an index by name.
     *
     * @param indexName name of index
     * @return index definition or null
     */
    public IndexDefinition findIndex(String indexName) {
        return indexes.stream()
                .filter(i -> i.indexName.equals(indexName))
                .findFirst()
                .orElse(null);
    }

    // Nested classes

    /**
     * Column definition within a table.
     */
    public static class ColumnDefinition {
        private String columnName;
        private String dataType;
        private boolean nullable;
        private String defaultValue;
        private boolean isIdentity;
        private String identityType; // ALWAYS, BY DEFAULT
        private String collation;
        private int position;
        private String comment;
        private boolean isGenerated;
        private String generationExpression;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private final ColumnDefinition col = new ColumnDefinition();
            public Builder columnName(String v) { col.columnName = v; return this; }
            public Builder dataType(String v) { col.dataType = v; return this; }
            public Builder nullable(boolean v) { col.nullable = v; return this; }
            public Builder defaultValue(String v) { col.defaultValue = v; return this; }
            public Builder isIdentity(boolean v) { col.isIdentity = v; return this; }
            public Builder identityType(String v) { col.identityType = v; return this; }
            public Builder identity(String v) {
                if (v != null) { col.isIdentity = true; col.identityType = v; }
                return this;
            }
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
            public ColumnDefinition build() { return col; }
        }

        public ColumnDefinition() {
        }

        public ColumnDefinition(String columnName, String dataType, boolean nullable) {
            this.columnName = columnName;
            this.dataType = dataType;
            this.nullable = nullable;
        }

        /**
         * Checks if this column equals another for comparison purposes.
         *
         * @param other other column
         * @return true if structurally equal
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
         * Gets differences from another column.
         *
         * @param other other column
         * @return list of differences
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
     * Primary key constraint definition.
     */
    public static class PrimaryKeyDefinition {
        private String constraintName;
        private List<String> columns = new ArrayList<>();
        private String indexTableSpace;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private final PrimaryKeyDefinition pk = new PrimaryKeyDefinition();
            public Builder constraintName(String v) { pk.constraintName = v; return this; }
            public Builder columns(List<String> v) { pk.columns = v; return this; }
            public Builder indexTableSpace(String v) { pk.indexTableSpace = v; return this; }
            public PrimaryKeyDefinition build() { return pk; }
        }

        public PrimaryKeyDefinition() {
        }

        public PrimaryKeyDefinition(String constraintName, List<String> columns) {
            this.constraintName = constraintName;
            this.columns = columns;
        }

        public boolean equalsStructure(PrimaryKeyDefinition other) {
            if (other == null) return false;
            return Objects.equals(columns, other.columns);
        }

        public String getColumnsDisplay() {
            return String.join(", ", columns);
        }

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
     * Foreign key constraint definition.
     */
    public static class ForeignKeyDefinition {
        private String constraintName;
        private List<String> columns = new ArrayList<>();
        private String referencedSchema;
        private String referencedTable;
        private List<String> referencedColumns = new ArrayList<>();
        private String onDelete;
        private String onUpdate;
        private boolean deferrable;
        private boolean initiallyDeferred;

        public static Builder builder() {
            return new Builder();
        }

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
            public ForeignKeyDefinition build() { return fk; }
        }

        public ForeignKeyDefinition() {
        }

        public boolean equalsStructure(ForeignKeyDefinition other) {
            if (other == null) return false;
            return Objects.equals(columns, other.columns)
                    && Objects.equals(referencedSchema, other.referencedSchema)
                    && Objects.equals(referencedTable, other.referencedTable)
                    && Objects.equals(referencedColumns, other.referencedColumns)
                    && Objects.equals(onDelete, other.onDelete)
                    && Objects.equals(onUpdate, other.onUpdate);
        }

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

        public String getReferencedFullName() {
            return referencedSchema + "." + referencedTable;
        }

        public String getColumnsDisplay() {
            return String.join(", ", columns);
        }

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
     * Unique constraint definition.
     */
    public static class UniqueConstraintDefinition {
        private String constraintName;
        private List<String> columns = new ArrayList<>();

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private final UniqueConstraintDefinition uc = new UniqueConstraintDefinition();
            public Builder constraintName(String v) { uc.constraintName = v; return this; }
            public Builder columns(List<String> v) { uc.columns = v; return this; }
            public UniqueConstraintDefinition build() { return uc; }
        }

        public UniqueConstraintDefinition() {
        }

        public UniqueConstraintDefinition(String constraintName, List<String> columns) {
            this.constraintName = constraintName;
            this.columns = columns;
        }

        public boolean equalsStructure(UniqueConstraintDefinition other) {
            if (other == null) return false;
            return Objects.equals(columns, other.columns);
        }

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
     * Check constraint definition.
     */
    public static class CheckConstraintDefinition {
        private String constraintName;
        private String expression;
        private boolean noInherit;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private final CheckConstraintDefinition cc = new CheckConstraintDefinition();
            public Builder constraintName(String v) { cc.constraintName = v; return this; }
            public Builder expression(String v) { cc.expression = v; return this; }
            public Builder noInherit(boolean v) { cc.noInherit = v; return this; }
            public CheckConstraintDefinition build() { return cc; }
        }

        public CheckConstraintDefinition() {
        }

        public CheckConstraintDefinition(String constraintName, String expression) {
            this.constraintName = constraintName;
            this.expression = expression;
        }

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
     * Index definition.
     */
    public static class IndexDefinition {
        private String indexName;
        private String indexType; // btree, hash, gist, gin, brin
        private List<String> columns = new ArrayList<>();
        private List<String> includeColumns = new ArrayList<>();
        private boolean unique;
        private boolean primary;
        private String whereClause;
        private String expressionDef;
        private String tableSpace;
        private String definition; // Full index definition

        public static Builder builder() {
            return new Builder();
        }

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
            public IndexDefinition build() { return idx; }
        }

        public IndexDefinition() {
        }

        public boolean equalsStructure(IndexDefinition other) {
            if (other == null) return false;
            return Objects.equals(indexType, other.indexType)
                    && Objects.equals(columns, other.columns)
                    && Objects.equals(includeColumns, other.includeColumns)
                    && unique == other.unique
                    && Objects.equals(normaliseWhereClause(whereClause), normaliseWhereClause(other.whereClause));
        }

        private String normaliseWhereClause(String clause) {
            return clause != null ? clause.replaceAll("\\s+", " ").trim() : null;
        }

        public String getColumnsDisplay() {
            return String.join(", ", columns);
        }

        public boolean isPartial() {
            return whereClause != null && !whereClause.isEmpty();
        }

        public boolean isExpression() {
            return expressionDef != null && !expressionDef.isEmpty();
        }

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
     * Trigger definition on a table.
     */
    public static class TriggerDefinition {
        private String triggerName;
        private String timing; // BEFORE, AFTER, INSTEAD OF
        private String events; // INSERT, UPDATE, DELETE, TRUNCATE
        private String level; // ROW, STATEMENT
        private String functionName;
        private String functionSchema;
        private String condition;
        private boolean enabled;
        private String definition;

        public static Builder builder() {
            return new Builder();
        }

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
            public TriggerDefinition build() { return trg; }
        }

        public TriggerDefinition() {
        }

        public boolean equalsStructure(TriggerDefinition other) {
            if (other == null) return false;
            return Objects.equals(timing, other.timing)
                    && Objects.equals(events, other.events)
                    && Objects.equals(level, other.level)
                    && Objects.equals(functionName, other.functionName)
                    && Objects.equals(condition, other.condition);
        }

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

    public void addColumn(ColumnDefinition column) { this.columns.add(column); }
    public void addForeignKey(ForeignKeyDefinition fk) { this.foreignKeys.add(fk); }
    public void addUniqueConstraint(UniqueConstraintDefinition uc) { this.uniqueConstraints.add(uc); }
    public void addCheckConstraint(CheckConstraintDefinition cc) { this.checkConstraints.add(cc); }
    public void addIndex(IndexDefinition idx) { this.indexes.add(idx); }
    public void addTrigger(TriggerDefinition trg) { this.triggers.add(trg); }
}
