package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating DDL statements from schema differences.
 * <p>
 * Creates CREATE, ALTER, and DROP statements with proper
 * dependency ordering for migration scripts.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class DdlGeneratorService {

    private static final Logger LOG = Logger.getLogger(DdlGeneratorService.class);

    // Execution order for object types (lower = earlier)
    private static final Map<ObjectDifference.ObjectType, Integer> TYPE_ORDER = Map.ofEntries(
            Map.entry(ObjectDifference.ObjectType.EXTENSION, 10),
            Map.entry(ObjectDifference.ObjectType.TYPE_ENUM, 20),
            Map.entry(ObjectDifference.ObjectType.TYPE_COMPOSITE, 21),
            Map.entry(ObjectDifference.ObjectType.TYPE_DOMAIN, 22),
            Map.entry(ObjectDifference.ObjectType.SEQUENCE, 30),
            Map.entry(ObjectDifference.ObjectType.TABLE, 40),
            Map.entry(ObjectDifference.ObjectType.COLUMN, 50),
            Map.entry(ObjectDifference.ObjectType.CONSTRAINT_PRIMARY, 60),
            Map.entry(ObjectDifference.ObjectType.CONSTRAINT_UNIQUE, 61),
            Map.entry(ObjectDifference.ObjectType.CONSTRAINT_CHECK, 62),
            Map.entry(ObjectDifference.ObjectType.CONSTRAINT_FOREIGN, 70),
            Map.entry(ObjectDifference.ObjectType.INDEX, 80),
            Map.entry(ObjectDifference.ObjectType.VIEW, 90),
            Map.entry(ObjectDifference.ObjectType.MATERIALIZED_VIEW, 91),
            Map.entry(ObjectDifference.ObjectType.FUNCTION, 100),
            Map.entry(ObjectDifference.ObjectType.PROCEDURE, 101),
            Map.entry(ObjectDifference.ObjectType.TRIGGER, 110)
    );

    /**
     * Generates a migration script from comparison differences.
     *
     * @param result comparison result
     * @param wrapOption transaction wrapping option
     * @param includeDrops whether to include DROP statements
     * @return migration script
     */
    public MigrationScript generateMigrationScript(SchemaComparisonResult result,
                                                    MigrationScript.WrapOption wrapOption,
                                                    boolean includeDrops) {
        MigrationScript script = new MigrationScript();
        script.setWrapOption(wrapOption);
        script.setIncludeDropStatements(includeDrops);
        script.setSourceInstance(result.getSourceInstance());
        script.setDestinationInstance(result.getDestinationInstance());
        script.setSourceSchema(result.getSourceSchema());
        script.setDestinationSchema(result.getDestinationSchema());

        int order = 0;

        // Generate statements for each difference
        for (ObjectDifference diff : result.getDifferences()) {
            List<MigrationScript.MigrationStatement> statements = generateStatements(diff, result.getDestinationSchema());
            for (MigrationScript.MigrationStatement stmt : statements) {
                stmt.setOrder(order++);
                script.addStatement(stmt);
            }
        }

        // Reorder by dependency
        reorderByDependency(script);

        return script;
    }

    /**
     * Generates DDL statements for a single difference.
     */
    private List<MigrationScript.MigrationStatement> generateStatements(ObjectDifference diff, String targetSchema) {
        List<MigrationScript.MigrationStatement> statements = new ArrayList<>();

        switch (diff.getDifferenceType()) {
            case MISSING -> statements.add(generateCreateStatement(diff, targetSchema));
            case EXTRA -> statements.add(generateDropStatement(diff, targetSchema));
            case MODIFIED -> statements.addAll(generateAlterStatements(diff, targetSchema));
        }

        return statements;
    }

    /**
     * Generates a CREATE statement for a missing object.
     */
    private MigrationScript.MigrationStatement generateCreateStatement(ObjectDifference diff, String targetSchema) {
        String ddl = switch (diff.getObjectType()) {
            case TABLE -> generateCreateTable(diff, targetSchema);
            case COLUMN -> generateAddColumn(diff, targetSchema);
            case INDEX -> generateCreateIndex(diff, targetSchema);
            case CONSTRAINT_PRIMARY -> generateAddPrimaryKey(diff, targetSchema);
            case CONSTRAINT_FOREIGN -> generateAddForeignKey(diff, targetSchema);
            case CONSTRAINT_UNIQUE -> generateAddUniqueConstraint(diff, targetSchema);
            case CONSTRAINT_CHECK -> generateAddCheckConstraint(diff, targetSchema);
            case VIEW -> generateCreateView(diff, targetSchema);
            case MATERIALIZED_VIEW -> generateCreateMaterialisedView(diff, targetSchema);
            case FUNCTION, PROCEDURE -> generateCreateFunction(diff);
            case TRIGGER -> generateCreateTrigger(diff);
            case SEQUENCE -> generateCreateSequence(diff, targetSchema);
            case TYPE_ENUM -> generateCreateEnumType(diff, targetSchema);
            case TYPE_COMPOSITE -> generateCreateCompositeType(diff, targetSchema);
            case TYPE_DOMAIN -> generateCreateDomain(diff, targetSchema);
            case EXTENSION -> generateCreateExtension(diff);
        };

        return MigrationScript.MigrationStatement.builder()
                .ddl(ddl)
                .objectType(diff.getObjectType())
                .objectName(diff.getObjectName())
                .severity(diff.getSeverity())
                .build();
    }

    /**
     * Generates a DROP statement for an extra object.
     */
    private MigrationScript.MigrationStatement generateDropStatement(ObjectDifference diff, String targetSchema) {
        String ddl = switch (diff.getObjectType()) {
            case TABLE -> String.format("DROP TABLE IF EXISTS %s.%s CASCADE", targetSchema, diff.getObjectName());
            case COLUMN -> {
                String[] parts = diff.getObjectName().split("\\.");
                yield String.format("ALTER TABLE %s.%s DROP COLUMN IF EXISTS %s",
                        targetSchema, parts[0], parts[1]);
            }
            case INDEX -> String.format("DROP INDEX IF EXISTS %s.%s", targetSchema, diff.getObjectName());
            case CONSTRAINT_PRIMARY, CONSTRAINT_FOREIGN, CONSTRAINT_UNIQUE, CONSTRAINT_CHECK -> {
                String[] parts = diff.getObjectName().split("\\.");
                yield String.format("ALTER TABLE %s.%s DROP CONSTRAINT IF EXISTS %s",
                        targetSchema, parts[0], parts[1]);
            }
            case VIEW -> String.format("DROP VIEW IF EXISTS %s.%s CASCADE", targetSchema, diff.getObjectName());
            case MATERIALIZED_VIEW -> String.format("DROP MATERIALIZED VIEW IF EXISTS %s.%s CASCADE",
                    targetSchema, diff.getObjectName());
            case FUNCTION -> String.format("DROP FUNCTION IF EXISTS %s.%s CASCADE", targetSchema, diff.getObjectName());
            case PROCEDURE -> String.format("DROP PROCEDURE IF EXISTS %s.%s CASCADE", targetSchema, diff.getObjectName());
            case TRIGGER -> {
                String[] parts = diff.getObjectName().split("\\.");
                yield String.format("DROP TRIGGER IF EXISTS %s ON %s.%s",
                        parts[1], targetSchema, parts[0]);
            }
            case SEQUENCE -> String.format("DROP SEQUENCE IF EXISTS %s.%s CASCADE", targetSchema, diff.getObjectName());
            case TYPE_ENUM, TYPE_COMPOSITE, TYPE_DOMAIN -> String.format("DROP TYPE IF EXISTS %s.%s CASCADE",
                    targetSchema, diff.getObjectName());
            case EXTENSION -> String.format("DROP EXTENSION IF EXISTS %s CASCADE", diff.getObjectName());
        };

        return MigrationScript.MigrationStatement.builder()
                .ddl(ddl)
                .objectType(diff.getObjectType())
                .objectName(diff.getObjectName())
                .severity(ObjectDifference.Severity.BREAKING)
                .warningMessage("DROP statement - potential data loss")
                .build();
    }

    /**
     * Generates ALTER statements for modified objects.
     */
    private List<MigrationScript.MigrationStatement> generateAlterStatements(ObjectDifference diff, String targetSchema) {
        List<MigrationScript.MigrationStatement> statements = new ArrayList<>();

        for (AttributeDifference attr : diff.getAttributeDifferences()) {
            String ddl = generateAlterForAttribute(diff, attr, targetSchema);
            if (ddl != null) {
                statements.add(MigrationScript.MigrationStatement.builder()
                        .ddl(ddl)
                        .objectType(diff.getObjectType())
                        .objectName(diff.getObjectName())
                        .severity(determineSeverity(diff.getObjectType(), attr))
                        .warningMessage(attr.isRemoved() ? "Removing attribute - verify data impact" : null)
                        .build());
            }
        }

        return statements;
    }

    private String generateAlterForAttribute(ObjectDifference diff, AttributeDifference attr, String targetSchema) {
        return switch (diff.getObjectType()) {
            case TABLE -> generateAlterTableAttribute(diff, attr, targetSchema);
            case COLUMN -> generateAlterColumnAttribute(diff, attr, targetSchema);
            case VIEW -> generateAlterView(diff, targetSchema);
            case FUNCTION, PROCEDURE -> generateCreateOrReplaceFunction(diff);
            case TYPE_ENUM -> generateAlterEnumType(diff, attr, targetSchema);
            default -> null;
        };
    }

    private String generateAlterTableAttribute(ObjectDifference diff, AttributeDifference attr, String targetSchema) {
        String tableName = diff.getObjectName();

        return switch (attr.getAttributeName()) {
            case "comment" -> String.format("COMMENT ON TABLE %s.%s IS '%s'",
                    targetSchema, tableName, escapeString(attr.getSourceValue()));
            case "owner" -> String.format("ALTER TABLE %s.%s OWNER TO %s",
                    targetSchema, tableName, attr.getSourceValue());
            default -> null;
        };
    }

    private String generateAlterColumnAttribute(ObjectDifference diff, AttributeDifference attr, String targetSchema) {
        String[] parts = diff.getObjectName().split("\\.");
        String tableName = parts[0];
        String columnName = parts[1];

        return switch (attr.getAttributeName()) {
            case "dataType" -> String.format("ALTER TABLE %s.%s ALTER COLUMN %s TYPE %s",
                    targetSchema, tableName, columnName, attr.getSourceValue());
            case "nullable" -> {
                boolean nullable = Boolean.parseBoolean(attr.getSourceValue());
                yield String.format("ALTER TABLE %s.%s ALTER COLUMN %s %s NOT NULL",
                        targetSchema, tableName, columnName, nullable ? "DROP" : "SET");
            }
            case "defaultValue" -> {
                if (attr.getSourceValue() == null || attr.getSourceValue().isEmpty()) {
                    yield String.format("ALTER TABLE %s.%s ALTER COLUMN %s DROP DEFAULT",
                            targetSchema, tableName, columnName);
                } else {
                    yield String.format("ALTER TABLE %s.%s ALTER COLUMN %s SET DEFAULT %s",
                            targetSchema, tableName, columnName, attr.getSourceValue());
                }
            }
            case "comment" -> String.format("COMMENT ON COLUMN %s.%s.%s IS '%s'",
                    targetSchema, tableName, columnName, escapeString(attr.getSourceValue()));
            default -> null;
        };
    }

    private String generateAlterView(ObjectDifference diff, String targetSchema) {
        // Views must be recreated
        if (diff.getSourceDefinition() != null) {
            return String.format("CREATE OR REPLACE VIEW %s.%s AS\n%s",
                    targetSchema, diff.getObjectName(), diff.getSourceDefinition());
        }
        return null;
    }

    private String generateCreateOrReplaceFunction(ObjectDifference diff) {
        // Functions can use CREATE OR REPLACE
        return diff.getSourceDefinition();
    }

    private String generateAlterEnumType(ObjectDifference diff, AttributeDifference attr, String targetSchema) {
        if ("enumLabels".equals(attr.getAttributeName())) {
            // Can only ADD values to enum types, not remove
            // Parse the lists to find new values
            List<String> sourceLabels = parseListString(attr.getSourceValue());
            List<String> destLabels = parseListString(attr.getDestinationValue());

            StringBuilder sb = new StringBuilder();
            String prevLabel = null;

            for (String label : sourceLabels) {
                if (!destLabels.contains(label)) {
                    sb.append(String.format("ALTER TYPE %s.%s ADD VALUE '%s'",
                            targetSchema, diff.getObjectName(), label));
                    if (prevLabel != null) {
                        sb.append(String.format(" AFTER '%s'", prevLabel));
                    }
                    sb.append(";\n");
                }
                prevLabel = label;
            }

            return sb.length() > 0 ? sb.toString().trim() : null;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<String> parseListString(String listStr) {
        if (listStr == null || listStr.isEmpty()) return new ArrayList<>();
        // Handle [a, b, c] format
        listStr = listStr.replaceAll("[\\[\\]]", "");
        return Arrays.stream(listStr.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    // CREATE statement generators

    private String generateCreateTable(ObjectDifference diff, String targetSchema) {
        // If we have full source definition, use it
        if (diff.getSourceDefinition() != null) {
            return diff.getSourceDefinition();
        }

        // Otherwise generate basic CREATE TABLE
        return String.format("CREATE TABLE %s.%s (\n    -- columns to be defined\n)",
                targetSchema, diff.getObjectName());
    }

    private String generateAddColumn(ObjectDifference diff, String targetSchema) {
        String[] parts = diff.getObjectName().split("\\.");
        String tableName = parts[0];
        String columnName = parts[1];

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("ALTER TABLE %s.%s ADD COLUMN %s",
                targetSchema, tableName, columnName));

        // Add data type if available from source definition
        if (diff.getSourceDefinition() != null) {
            sb.append(" ").append(diff.getSourceDefinition());
        } else {
            sb.append(" -- type to be specified");
        }

        return sb.toString();
    }

    private String generateCreateIndex(ObjectDifference diff, String targetSchema) {
        if (diff.getSourceDefinition() != null) {
            // Replace schema in definition if present
            return diff.getSourceDefinition().replace("CREATE INDEX", "CREATE INDEX IF NOT EXISTS");
        }
        return String.format("CREATE INDEX IF NOT EXISTS %s ON %s.table_name (columns)",
                diff.getObjectName(), targetSchema);
    }

    private String generateAddPrimaryKey(ObjectDifference diff, String targetSchema) {
        String[] parts = diff.getObjectName().split("\\.");
        String tableName = parts[0];
        String constraintName = parts[1];

        if (diff.getSourceDefinition() != null) {
            return String.format("ALTER TABLE %s.%s ADD CONSTRAINT %s %s",
                    targetSchema, tableName, constraintName, diff.getSourceDefinition());
        }
        return String.format("ALTER TABLE %s.%s ADD CONSTRAINT %s PRIMARY KEY (columns)",
                targetSchema, tableName, constraintName);
    }

    private String generateAddForeignKey(ObjectDifference diff, String targetSchema) {
        String[] parts = diff.getObjectName().split("\\.");
        String tableName = parts[0];
        String constraintName = parts[1];

        if (diff.getSourceDefinition() != null) {
            return String.format("ALTER TABLE %s.%s ADD CONSTRAINT %s %s",
                    targetSchema, tableName, constraintName, diff.getSourceDefinition());
        }
        return String.format("ALTER TABLE %s.%s ADD CONSTRAINT %s FOREIGN KEY (columns) REFERENCES table(columns)",
                targetSchema, tableName, constraintName);
    }

    private String generateAddUniqueConstraint(ObjectDifference diff, String targetSchema) {
        String[] parts = diff.getObjectName().split("\\.");
        String tableName = parts[0];
        String constraintName = parts[1];

        if (diff.getSourceDefinition() != null) {
            return String.format("ALTER TABLE %s.%s ADD CONSTRAINT %s %s",
                    targetSchema, tableName, constraintName, diff.getSourceDefinition());
        }
        return String.format("ALTER TABLE %s.%s ADD CONSTRAINT %s UNIQUE (columns)",
                targetSchema, tableName, constraintName);
    }

    private String generateAddCheckConstraint(ObjectDifference diff, String targetSchema) {
        String[] parts = diff.getObjectName().split("\\.");
        String tableName = parts[0];
        String constraintName = parts[1];

        if (diff.getSourceDefinition() != null) {
            return String.format("ALTER TABLE %s.%s ADD CONSTRAINT %s %s",
                    targetSchema, tableName, constraintName, diff.getSourceDefinition());
        }
        return String.format("ALTER TABLE %s.%s ADD CONSTRAINT %s CHECK (expression)",
                targetSchema, tableName, constraintName);
    }

    private String generateCreateView(ObjectDifference diff, String targetSchema) {
        if (diff.getSourceDefinition() != null) {
            return String.format("CREATE VIEW %s.%s AS\n%s",
                    targetSchema, diff.getObjectName(), diff.getSourceDefinition());
        }
        return String.format("CREATE VIEW %s.%s AS\n    SELECT -- query here",
                targetSchema, diff.getObjectName());
    }

    private String generateCreateMaterialisedView(ObjectDifference diff, String targetSchema) {
        if (diff.getSourceDefinition() != null) {
            return String.format("CREATE MATERIALIZED VIEW %s.%s AS\n%s",
                    targetSchema, diff.getObjectName(), diff.getSourceDefinition());
        }
        return String.format("CREATE MATERIALIZED VIEW %s.%s AS\n    SELECT -- query here",
                targetSchema, diff.getObjectName());
    }

    private String generateCreateFunction(ObjectDifference diff) {
        if (diff.getSourceDefinition() != null) {
            return diff.getSourceDefinition();
        }
        return "-- Function definition not available";
    }

    private String generateCreateTrigger(ObjectDifference diff) {
        if (diff.getSourceDefinition() != null) {
            return diff.getSourceDefinition();
        }
        return "-- Trigger definition not available";
    }

    private String generateCreateSequence(ObjectDifference diff, String targetSchema) {
        if (diff.getSourceDefinition() != null) {
            return String.format("CREATE SEQUENCE %s.%s %s",
                    targetSchema, diff.getObjectName(), diff.getSourceDefinition());
        }
        return String.format("CREATE SEQUENCE %s.%s", targetSchema, diff.getObjectName());
    }

    private String generateCreateEnumType(ObjectDifference diff, String targetSchema) {
        if (diff.getSourceDefinition() != null) {
            return String.format("CREATE TYPE %s.%s AS ENUM (%s)",
                    targetSchema, diff.getObjectName(), diff.getSourceDefinition());
        }
        return String.format("CREATE TYPE %s.%s AS ENUM ('values')",
                targetSchema, diff.getObjectName());
    }

    private String generateCreateCompositeType(ObjectDifference diff, String targetSchema) {
        if (diff.getSourceDefinition() != null) {
            return String.format("CREATE TYPE %s.%s AS (%s)",
                    targetSchema, diff.getObjectName(), diff.getSourceDefinition());
        }
        return String.format("CREATE TYPE %s.%s AS (attributes)",
                targetSchema, diff.getObjectName());
    }

    private String generateCreateDomain(ObjectDifference diff, String targetSchema) {
        if (diff.getSourceDefinition() != null) {
            return String.format("CREATE DOMAIN %s.%s AS %s",
                    targetSchema, diff.getObjectName(), diff.getSourceDefinition());
        }
        return String.format("CREATE DOMAIN %s.%s AS base_type",
                targetSchema, diff.getObjectName());
    }

    private String generateCreateExtension(ObjectDifference diff) {
        return String.format("CREATE EXTENSION IF NOT EXISTS %s", diff.getObjectName());
    }

    /**
     * Determines severity based on object type and attribute change.
     */
    private ObjectDifference.Severity determineSeverity(ObjectDifference.ObjectType type, AttributeDifference attr) {
        // Dropping anything is breaking
        if (attr.isRemoved()) {
            return ObjectDifference.Severity.BREAKING;
        }

        // Type changes can be breaking
        if ("dataType".equals(attr.getAttributeName())) {
            return ObjectDifference.Severity.WARNING;
        }

        // Adding NOT NULL is a warning
        if ("nullable".equals(attr.getAttributeName()) && "false".equals(attr.getSourceValue())) {
            return ObjectDifference.Severity.WARNING;
        }

        // Most other changes are informational
        return ObjectDifference.Severity.INFO;
    }

    /**
     * Reorders statements by object type dependencies.
     */
    private void reorderByDependency(MigrationScript script) {
        List<MigrationScript.MigrationStatement> statements = script.getStatements();

        // Separate CREATE and DROP statements
        List<MigrationScript.MigrationStatement> creates = new ArrayList<>();
        List<MigrationScript.MigrationStatement> alters = new ArrayList<>();
        List<MigrationScript.MigrationStatement> drops = new ArrayList<>();

        for (MigrationScript.MigrationStatement stmt : statements) {
            if (stmt.isCreateStatement()) {
                creates.add(stmt);
            } else if (stmt.isDropStatement()) {
                drops.add(stmt);
            } else {
                alters.add(stmt);
            }
        }

        // Sort CREATEs by type order (extensions first, triggers last)
        creates.sort(Comparator.comparingInt(s -> TYPE_ORDER.getOrDefault(s.getObjectType(), 999)));

        // Sort DROPs in reverse order (triggers first, extensions last)
        drops.sort(Comparator.comparingInt(s -> -TYPE_ORDER.getOrDefault(s.getObjectType(), 999)));

        // Combine: DROPs first, then CREATEs, then ALTERs
        List<MigrationScript.MigrationStatement> ordered = new ArrayList<>();
        ordered.addAll(drops);
        ordered.addAll(creates);
        ordered.addAll(alters);

        // Update order numbers
        for (int i = 0; i < ordered.size(); i++) {
            ordered.get(i).setOrder(i);
        }

        script.setStatements(ordered);
    }

    /**
     * Escapes single quotes in SQL strings.
     */
    private String escapeString(String value) {
        if (value == null) return null;
        return value.replace("'", "''");
    }

    /**
     * Generates DDL for creating a complete table with all its objects.
     *
     * @param table table schema
     * @param schemaName target schema name
     * @return complete CREATE TABLE DDL
     */
    public String generateFullTableDdl(TableSchema table, String schemaName) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("CREATE TABLE %s.%s (\n", schemaName, table.getTableName()));

        // Columns
        List<String> columnDefs = new ArrayList<>();
        for (TableSchema.ColumnDefinition col : table.getColumns()) {
            StringBuilder colDef = new StringBuilder();
            colDef.append("    ").append(col.getColumnName()).append(" ").append(col.getDataType());

            if (!col.isNullable()) {
                colDef.append(" NOT NULL");
            }
            if (col.getDefaultValue() != null && !col.getDefaultValue().isEmpty()) {
                colDef.append(" DEFAULT ").append(col.getDefaultValue());
            }
            if (col.getIdentity() != null) {
                colDef.append(" GENERATED ").append(col.getIdentity()).append(" AS IDENTITY");
            }

            columnDefs.add(colDef.toString());
        }

        // Primary key inline
        if (table.getPrimaryKey() != null) {
            columnDefs.add(String.format("    CONSTRAINT %s PRIMARY KEY (%s)",
                    table.getPrimaryKey().getConstraintName(),
                    String.join(", ", table.getPrimaryKey().getColumns())));
        }

        // Unique constraints inline
        for (TableSchema.UniqueConstraintDefinition uc : table.getUniqueConstraints()) {
            columnDefs.add(String.format("    CONSTRAINT %s UNIQUE (%s)",
                    uc.getConstraintName(),
                    String.join(", ", uc.getColumns())));
        }

        // Check constraints inline
        for (TableSchema.CheckConstraintDefinition cc : table.getCheckConstraints()) {
            columnDefs.add(String.format("    CONSTRAINT %s %s",
                    cc.getConstraintName(), cc.getExpression()));
        }

        sb.append(String.join(",\n", columnDefs));
        sb.append("\n)");

        // Partition key if partitioned
        if (table.getPartitionKey() != null && !table.getPartitionKey().isEmpty()) {
            sb.append(" PARTITION BY ").append(table.getPartitionKey());
        }

        sb.append(";\n");

        // Foreign keys (separate statements)
        for (TableSchema.ForeignKeyDefinition fk : table.getForeignKeys()) {
            sb.append(String.format("\nALTER TABLE %s.%s ADD CONSTRAINT %s FOREIGN KEY (%s) REFERENCES %s.%s (%s)",
                    schemaName, table.getTableName(), fk.getConstraintName(),
                    String.join(", ", fk.getColumns()),
                    fk.getReferencedSchema(), fk.getReferencedTable(),
                    String.join(", ", fk.getReferencedColumns())));

            if (!"NO ACTION".equals(fk.getOnUpdate())) {
                sb.append(" ON UPDATE ").append(fk.getOnUpdate());
            }
            if (!"NO ACTION".equals(fk.getOnDelete())) {
                sb.append(" ON DELETE ").append(fk.getOnDelete());
            }
            sb.append(";\n");
        }

        // Indexes (separate statements)
        for (TableSchema.IndexDefinition idx : table.getIndexes()) {
            if (idx.getDefinition() != null) {
                sb.append("\n").append(idx.getDefinition()).append(";\n");
            }
        }

        // Comment
        if (table.getComment() != null && !table.getComment().isEmpty()) {
            sb.append(String.format("\nCOMMENT ON TABLE %s.%s IS '%s';\n",
                    schemaName, table.getTableName(), escapeString(table.getComment())));
        }

        // Column comments
        for (TableSchema.ColumnDefinition col : table.getColumns()) {
            if (col.getComment() != null && !col.getComment().isEmpty()) {
                sb.append(String.format("COMMENT ON COLUMN %s.%s.%s IS '%s';\n",
                        schemaName, table.getTableName(), col.getColumnName(),
                        escapeString(col.getComment())));
            }
        }

        return sb.toString();
    }

    /**
     * Generates DDL for creating a function.
     *
     * @param function function schema
     * @return function DDL
     */
    public String generateFunctionDdl(FunctionSchema function) {
        if (function.getDefinition() != null) {
            return function.getDefinition();
        }
        return String.format("-- Function %s.%s definition not available",
                function.getSchemaName(), function.getSignature());
    }

    /**
     * Generates DDL for creating a view.
     *
     * @param view view schema
     * @param schemaName target schema name
     * @return view DDL
     */
    public String generateViewDdl(ViewSchema view, String schemaName) {
        StringBuilder sb = new StringBuilder();

        if (view.isMaterialised()) {
            sb.append("CREATE MATERIALIZED VIEW ");
        } else {
            sb.append("CREATE VIEW ");
        }

        sb.append(schemaName).append(".").append(view.getViewName()).append(" AS\n");
        sb.append(view.getDefinition());

        if (!view.getDefinition().trim().endsWith(";")) {
            sb.append(";");
        }

        return sb.toString();
    }

    /**
     * Generates DDL for creating an enum type.
     *
     * @param type type schema
     * @param schemaName target schema name
     * @return type DDL
     */
    public String generateEnumTypeDdl(TypeSchema type, String schemaName) {
        String labels = type.getEnumLabels().stream()
                .map(l -> "'" + escapeString(l) + "'")
                .collect(Collectors.joining(", "));

        return String.format("CREATE TYPE %s.%s AS ENUM (%s);",
                schemaName, type.getTypeName(), labels);
    }

    /**
     * Generates DDL for creating a sequence.
     *
     * @param seq sequence schema
     * @param schemaName target schema name
     * @return sequence DDL
     */
    public String generateSequenceDdl(SequenceSchema seq, String schemaName) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE SEQUENCE ").append(schemaName).append(".").append(seq.getSequenceName());

        if (seq.getDataType() != null && !seq.getDataType().equals("bigint")) {
            sb.append(" AS ").append(seq.getDataType());
        }

        sb.append(" INCREMENT BY ").append(seq.getIncrement());
        sb.append(" MINVALUE ").append(seq.getMinValue());
        sb.append(" MAXVALUE ").append(seq.getMaxValue());
        sb.append(" START WITH ").append(seq.getStartValue());
        sb.append(" CACHE ").append(seq.getCacheSize());

        if (seq.isCycle()) {
            sb.append(" CYCLE");
        } else {
            sb.append(" NO CYCLE");
        }

        sb.append(";");

        return sb.toString();
    }
}
