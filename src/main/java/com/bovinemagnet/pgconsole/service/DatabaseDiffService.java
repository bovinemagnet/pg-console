package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for performing schema comparisons across different databases.
 * <p>
 * Enables cross-database comparisons by creating ad-hoc connections to
 * arbitrary databases on PostgreSQL instances. Supports comparing schemas
 * between different databases on the same instance or across different instances.
 * <p>
 * Example use cases:
 * <ul>
 *   <li>Compare prod1.public with prod2.public on same server</li>
 *   <li>Compare dev.myapp_schema with staging.myapp_schema across instances</li>
 * </ul>
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class DatabaseDiffService {

    private static final Logger LOG = Logger.getLogger(DatabaseDiffService.class);

    @Inject
    CrossDatabaseConnectionService connectionService;

    @Inject
    SchemaExtractorService extractorService;

    @Inject
    DdlGeneratorService ddlGeneratorService;

    @Inject
    DataSourceManager dataSourceManager;

    /**
     * Compares schemas across different databases.
     *
     * @param sourceInstance source instance name
     * @param sourceDatabase source database name
     * @param sourceSchema source schema name
     * @param destInstance destination instance name
     * @param destDatabase destination database name
     * @param destSchema destination schema name
     * @param filter comparison filter (null for no filtering)
     * @return comparison result with all differences
     */
    public SchemaComparisonResult compareCrossDatabase(
            String sourceInstance, String sourceDatabase, String sourceSchema,
            String destInstance, String destDatabase, String destSchema,
            ComparisonFilter filter) {

        LOG.infof("Starting cross-database comparison: %s.%s.%s -> %s.%s.%s",
                sourceInstance, sourceDatabase, sourceSchema,
                destInstance, destDatabase, destSchema);

        SchemaComparisonResult result = new SchemaComparisonResult();
        // Include database name in instance identifier for clarity
        result.setSourceInstance(sourceInstance + ":" + sourceDatabase);
        result.setDestinationInstance(destInstance + ":" + destDatabase);
        result.setSourceSchema(sourceSchema);
        result.setDestinationSchema(destSchema);
        result.setComparedAt(Instant.now());

        if (filter == null) {
            filter = new ComparisonFilter();
        }
        result.setFilter(filter);

        try (Connection sourceConn = connectionService.getConnectionToDatabase(sourceInstance, sourceDatabase);
             Connection destConn = connectionService.getConnectionToDatabase(destInstance, destDatabase)) {

            // Compare tables
            if (filter.isIncludeTables()) {
                compareTables(sourceConn, destConn, sourceSchema, destSchema, filter, result);
            }

            // Compare views
            if (filter.isIncludeViews()) {
                compareViews(sourceConn, destConn, sourceSchema, destSchema, filter, result);
            }

            // Compare functions
            if (filter.isIncludeFunctions()) {
                compareFunctions(sourceConn, destConn, sourceSchema, destSchema, filter, result);
            }

            // Compare sequences
            if (filter.isIncludeSequences()) {
                compareSequences(sourceConn, destConn, sourceSchema, destSchema, filter, result);
            }

            // Compare types
            if (filter.isIncludeTypes()) {
                compareTypes(sourceConn, destConn, sourceSchema, destSchema, filter, result);
            }

            // Compare extensions
            if (filter.isIncludeExtensions()) {
                compareExtensions(sourceConn, destConn, result);
            }

            result.setSuccess(true);
            LOG.infof("Cross-database comparison completed: %d differences found", result.getDifferences().size());

        } catch (SQLException e) {
            LOG.errorf("Cross-database comparison failed: %s", e.getMessage());
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    /**
     * Lists available databases for an instance.
     *
     * @param instanceName the instance to query
     * @return list of database names
     */
    public List<String> getDatabases(String instanceName) {
        return connectionService.listDatabases(instanceName);
    }

    /**
     * Gets schemas for a specific database on an instance.
     *
     * @param instanceName the instance name
     * @param databaseName the database name
     * @return list of schema names
     */
    public List<String> getSchemasForDatabase(String instanceName, String databaseName) {
        try (Connection conn = connectionService.getConnectionToDatabase(instanceName, databaseName)) {
            return extractorService.getSchemas(conn);
        } catch (SQLException e) {
            LOG.warnf("Failed to get schemas for %s.%s: %s", instanceName, databaseName, e.getMessage());
            return List.of();
        }
    }

    /**
     * Gets schema summary for a specific database on an instance.
     *
     * @param instanceName the instance name
     * @param databaseName the database name
     * @param schemaName the schema name
     * @return map of object type to count
     */
    public Map<String, Integer> getSchemaSummary(String instanceName, String databaseName, String schemaName) {
        try (Connection conn = connectionService.getConnectionToDatabase(instanceName, databaseName)) {
            return extractorService.getSchemaSummary(conn, schemaName);
        } catch (SQLException e) {
            LOG.warnf("Failed to get schema summary for %s.%s.%s: %s",
                    instanceName, databaseName, schemaName, e.getMessage());
            return Map.of();
        }
    }

    /**
     * Gets available instances.
     *
     * @return list of instance names
     */
    public List<String> getAvailableInstances() {
        return dataSourceManager.getAvailableInstances();
    }

    /**
     * Generates a migration script from a comparison result.
     *
     * @param result comparison result
     * @param wrapOption transaction wrapping option
     * @param includeDrops whether to include DROP statements
     * @return migration script
     */
    public MigrationScript generateMigration(SchemaComparisonResult result,
                                              MigrationScript.WrapOption wrapOption,
                                              boolean includeDrops) {
        return ddlGeneratorService.generateMigrationScript(result, wrapOption, includeDrops);
    }

    // =========================================================================
    // Private comparison methods (mirroring SchemaComparisonService)
    // =========================================================================

    private void compareTables(Connection sourceConn, Connection destConn,
                               String sourceSchema, String destSchema,
                               ComparisonFilter filter, SchemaComparisonResult result) throws SQLException {
        List<TableSchema> sourceTables = extractorService.extractTables(sourceConn, sourceSchema);
        List<TableSchema> destTables = extractorService.extractTables(destConn, destSchema);

        Map<String, TableSchema> sourceMap = sourceTables.stream()
                .filter(t -> filter.matchesTable(t.getTableName()))
                .collect(Collectors.toMap(TableSchema::getTableName, t -> t));

        Map<String, TableSchema> destMap = destTables.stream()
                .filter(t -> filter.matchesTable(t.getTableName()))
                .collect(Collectors.toMap(TableSchema::getTableName, t -> t));

        // Find missing tables (in source but not dest)
        for (String tableName : sourceMap.keySet()) {
            if (!destMap.containsKey(tableName)) {
                TableSchema table = sourceMap.get(tableName);
                ObjectDifference diff = ObjectDifference.builder()
                        .objectType(ObjectDifference.ObjectType.TABLE)
                        .objectName(tableName)
                        .differenceType(ObjectDifference.DifferenceType.MISSING)
                        .severity(ObjectDifference.Severity.INFO)
                        .sourceDefinition(ddlGeneratorService.generateFullTableDdl(table, destSchema))
                        .build();
                result.addDifference(diff);
            }
        }

        // Find extra tables (in dest but not source)
        for (String tableName : destMap.keySet()) {
            if (!sourceMap.containsKey(tableName)) {
                ObjectDifference diff = ObjectDifference.builder()
                        .objectType(ObjectDifference.ObjectType.TABLE)
                        .objectName(tableName)
                        .differenceType(ObjectDifference.DifferenceType.EXTRA)
                        .severity(ObjectDifference.Severity.BREAKING)
                        .build();
                result.addDifference(diff);
            }
        }

        // Compare matching tables
        for (String tableName : sourceMap.keySet()) {
            if (destMap.containsKey(tableName)) {
                TableSchema sourceTable = sourceMap.get(tableName);
                TableSchema destTable = destMap.get(tableName);
                compareTableStructure(sourceTable, destTable, destSchema, filter, result);
            }
        }
    }

    private void compareTableStructure(TableSchema source, TableSchema dest,
                                        String destSchema, ComparisonFilter filter,
                                        SchemaComparisonResult result) {
        String tableName = source.getTableName();

        // Compare columns
        if (filter.isIncludeColumns()) {
            compareColumns(source, dest, result);
        }

        // Compare primary key
        if (filter.isIncludePrimaryKeys()) {
            comparePrimaryKey(source, dest, tableName, result);
        }

        // Compare foreign keys
        if (filter.isIncludeForeignKeys()) {
            compareForeignKeys(source, dest, tableName, result);
        }

        // Compare unique constraints
        if (filter.isIncludeUniqueConstraints()) {
            compareUniqueConstraints(source, dest, tableName, result);
        }

        // Compare check constraints
        if (filter.isIncludeCheckConstraints()) {
            compareCheckConstraints(source, dest, tableName, result);
        }

        // Compare indexes
        if (filter.isIncludeIndexes()) {
            compareIndexes(source, dest, tableName, result);
        }

        // Compare triggers
        if (filter.isIncludeTriggers()) {
            compareTriggers(source, dest, tableName, result);
        }

        // Compare table attributes
        compareTableAttributes(source, dest, result);
    }

    private void compareColumns(TableSchema source, TableSchema dest, SchemaComparisonResult result) {
        String tableName = source.getTableName();

        Map<String, TableSchema.ColumnDefinition> sourceMap = source.getColumns().stream()
                .collect(Collectors.toMap(TableSchema.ColumnDefinition::getColumnName, c -> c));

        Map<String, TableSchema.ColumnDefinition> destMap = dest.getColumns().stream()
                .collect(Collectors.toMap(TableSchema.ColumnDefinition::getColumnName, c -> c));

        // Missing columns
        for (String colName : sourceMap.keySet()) {
            if (!destMap.containsKey(colName)) {
                TableSchema.ColumnDefinition col = sourceMap.get(colName);
                result.addDifference(ObjectDifference.builder()
                        .objectType(ObjectDifference.ObjectType.COLUMN)
                        .objectName(tableName + "." + colName)
                        .differenceType(ObjectDifference.DifferenceType.MISSING)
                        .severity(ObjectDifference.Severity.INFO)
                        .sourceDefinition(col.getDataType() +
                                (col.isNullable() ? "" : " NOT NULL") +
                                (col.getDefaultValue() != null ? " DEFAULT " + col.getDefaultValue() : ""))
                        .build());
            }
        }

        // Extra columns
        for (String colName : destMap.keySet()) {
            if (!sourceMap.containsKey(colName)) {
                result.addDifference(ObjectDifference.builder()
                        .objectType(ObjectDifference.ObjectType.COLUMN)
                        .objectName(tableName + "." + colName)
                        .differenceType(ObjectDifference.DifferenceType.EXTRA)
                        .severity(ObjectDifference.Severity.BREAKING)
                        .build());
            }
        }

        // Modified columns
        for (String colName : sourceMap.keySet()) {
            if (destMap.containsKey(colName)) {
                TableSchema.ColumnDefinition sourceCol = sourceMap.get(colName);
                TableSchema.ColumnDefinition destCol = destMap.get(colName);

                List<AttributeDifference> attrDiffs = sourceCol.getDifferencesFrom(destCol);
                if (!attrDiffs.isEmpty()) {
                    result.addDifference(ObjectDifference.builder()
                            .objectType(ObjectDifference.ObjectType.COLUMN)
                            .objectName(tableName + "." + colName)
                            .differenceType(ObjectDifference.DifferenceType.MODIFIED)
                            .severity(determineColumnSeverity(attrDiffs))
                            .attributeDifferences(attrDiffs)
                            .sourceDefinition(sourceCol.getDataType())
                            .destinationDefinition(destCol.getDataType())
                            .build());
                }
            }
        }
    }

    private ObjectDifference.Severity determineColumnSeverity(List<AttributeDifference> diffs) {
        for (AttributeDifference diff : diffs) {
            if ("dataType".equals(diff.getAttributeName())) {
                return ObjectDifference.Severity.WARNING;
            }
            if ("nullable".equals(diff.getAttributeName()) &&
                    "false".equals(diff.getSourceValue())) {
                return ObjectDifference.Severity.WARNING;
            }
        }
        return ObjectDifference.Severity.INFO;
    }

    private void comparePrimaryKey(TableSchema source, TableSchema dest,
                                   String tableName, SchemaComparisonResult result) {
        TableSchema.PrimaryKeyDefinition sourcePk = source.getPrimaryKey();
        TableSchema.PrimaryKeyDefinition destPk = dest.getPrimaryKey();

        if (sourcePk == null && destPk == null) {
            return;
        }

        if (sourcePk != null && destPk == null) {
            result.addDifference(ObjectDifference.builder()
                    .objectType(ObjectDifference.ObjectType.CONSTRAINT_PRIMARY)
                    .objectName(tableName + "." + sourcePk.getConstraintName())
                    .differenceType(ObjectDifference.DifferenceType.MISSING)
                    .severity(ObjectDifference.Severity.WARNING)
                    .sourceDefinition("PRIMARY KEY (" + String.join(", ", sourcePk.getColumns()) + ")")
                    .build());
            return;
        }

        if (sourcePk == null) {
            result.addDifference(ObjectDifference.builder()
                    .objectType(ObjectDifference.ObjectType.CONSTRAINT_PRIMARY)
                    .objectName(tableName + "." + destPk.getConstraintName())
                    .differenceType(ObjectDifference.DifferenceType.EXTRA)
                    .severity(ObjectDifference.Severity.BREAKING)
                    .build());
            return;
        }

        // Both exist, compare
        List<AttributeDifference> diffs = sourcePk.getDifferencesFrom(destPk);
        if (!diffs.isEmpty()) {
            result.addDifference(ObjectDifference.builder()
                    .objectType(ObjectDifference.ObjectType.CONSTRAINT_PRIMARY)
                    .objectName(tableName + "." + sourcePk.getConstraintName())
                    .differenceType(ObjectDifference.DifferenceType.MODIFIED)
                    .severity(ObjectDifference.Severity.WARNING)
                    .attributeDifferences(diffs)
                    .sourceDefinition("PRIMARY KEY (" + String.join(", ", sourcePk.getColumns()) + ")")
                    .destinationDefinition("PRIMARY KEY (" + String.join(", ", destPk.getColumns()) + ")")
                    .build());
        }
    }

    private void compareForeignKeys(TableSchema source, TableSchema dest,
                                    String tableName, SchemaComparisonResult result) {
        Map<String, TableSchema.ForeignKeyDefinition> sourceMap = source.getForeignKeys().stream()
                .collect(Collectors.toMap(TableSchema.ForeignKeyDefinition::getConstraintName, fk -> fk));

        Map<String, TableSchema.ForeignKeyDefinition> destMap = dest.getForeignKeys().stream()
                .collect(Collectors.toMap(TableSchema.ForeignKeyDefinition::getConstraintName, fk -> fk));

        for (String fkName : sourceMap.keySet()) {
            if (!destMap.containsKey(fkName)) {
                TableSchema.ForeignKeyDefinition fk = sourceMap.get(fkName);
                result.addDifference(ObjectDifference.builder()
                        .objectType(ObjectDifference.ObjectType.CONSTRAINT_FOREIGN)
                        .objectName(tableName + "." + fkName)
                        .differenceType(ObjectDifference.DifferenceType.MISSING)
                        .severity(ObjectDifference.Severity.WARNING)
                        .sourceDefinition(String.format("FOREIGN KEY (%s) REFERENCES %s.%s (%s)",
                                String.join(", ", fk.getColumns()),
                                fk.getReferencedSchema(), fk.getReferencedTable(),
                                String.join(", ", fk.getReferencedColumns())))
                        .build());
            }
        }

        for (String fkName : destMap.keySet()) {
            if (!sourceMap.containsKey(fkName)) {
                result.addDifference(ObjectDifference.builder()
                        .objectType(ObjectDifference.ObjectType.CONSTRAINT_FOREIGN)
                        .objectName(tableName + "." + fkName)
                        .differenceType(ObjectDifference.DifferenceType.EXTRA)
                        .severity(ObjectDifference.Severity.BREAKING)
                        .build());
            }
        }

        for (String fkName : sourceMap.keySet()) {
            if (destMap.containsKey(fkName)) {
                TableSchema.ForeignKeyDefinition sourceFk = sourceMap.get(fkName);
                TableSchema.ForeignKeyDefinition destFk = destMap.get(fkName);

                List<AttributeDifference> diffs = sourceFk.getDifferencesFrom(destFk);
                if (!diffs.isEmpty()) {
                    result.addDifference(ObjectDifference.builder()
                            .objectType(ObjectDifference.ObjectType.CONSTRAINT_FOREIGN)
                            .objectName(tableName + "." + fkName)
                            .differenceType(ObjectDifference.DifferenceType.MODIFIED)
                            .severity(ObjectDifference.Severity.WARNING)
                            .attributeDifferences(diffs)
                            .build());
                }
            }
        }
    }

    private void compareUniqueConstraints(TableSchema source, TableSchema dest,
                                          String tableName, SchemaComparisonResult result) {
        Map<String, TableSchema.UniqueConstraintDefinition> sourceMap = source.getUniqueConstraints().stream()
                .collect(Collectors.toMap(TableSchema.UniqueConstraintDefinition::getConstraintName, uc -> uc));

        Map<String, TableSchema.UniqueConstraintDefinition> destMap = dest.getUniqueConstraints().stream()
                .collect(Collectors.toMap(TableSchema.UniqueConstraintDefinition::getConstraintName, uc -> uc));

        for (String ucName : sourceMap.keySet()) {
            if (!destMap.containsKey(ucName)) {
                TableSchema.UniqueConstraintDefinition uc = sourceMap.get(ucName);
                result.addDifference(ObjectDifference.builder()
                        .objectType(ObjectDifference.ObjectType.CONSTRAINT_UNIQUE)
                        .objectName(tableName + "." + ucName)
                        .differenceType(ObjectDifference.DifferenceType.MISSING)
                        .severity(ObjectDifference.Severity.INFO)
                        .sourceDefinition("UNIQUE (" + String.join(", ", uc.getColumns()) + ")")
                        .build());
            }
        }

        for (String ucName : destMap.keySet()) {
            if (!sourceMap.containsKey(ucName)) {
                result.addDifference(ObjectDifference.builder()
                        .objectType(ObjectDifference.ObjectType.CONSTRAINT_UNIQUE)
                        .objectName(tableName + "." + ucName)
                        .differenceType(ObjectDifference.DifferenceType.EXTRA)
                        .severity(ObjectDifference.Severity.BREAKING)
                        .build());
            }
        }
    }

    private void compareCheckConstraints(TableSchema source, TableSchema dest,
                                         String tableName, SchemaComparisonResult result) {
        Map<String, TableSchema.CheckConstraintDefinition> sourceMap = source.getCheckConstraints().stream()
                .collect(Collectors.toMap(TableSchema.CheckConstraintDefinition::getConstraintName, cc -> cc));

        Map<String, TableSchema.CheckConstraintDefinition> destMap = dest.getCheckConstraints().stream()
                .collect(Collectors.toMap(TableSchema.CheckConstraintDefinition::getConstraintName, cc -> cc));

        for (String ccName : sourceMap.keySet()) {
            if (!destMap.containsKey(ccName)) {
                TableSchema.CheckConstraintDefinition cc = sourceMap.get(ccName);
                result.addDifference(ObjectDifference.builder()
                        .objectType(ObjectDifference.ObjectType.CONSTRAINT_CHECK)
                        .objectName(tableName + "." + ccName)
                        .differenceType(ObjectDifference.DifferenceType.MISSING)
                        .severity(ObjectDifference.Severity.INFO)
                        .sourceDefinition(cc.getExpression())
                        .build());
            }
        }

        for (String ccName : destMap.keySet()) {
            if (!sourceMap.containsKey(ccName)) {
                result.addDifference(ObjectDifference.builder()
                        .objectType(ObjectDifference.ObjectType.CONSTRAINT_CHECK)
                        .objectName(tableName + "." + ccName)
                        .differenceType(ObjectDifference.DifferenceType.EXTRA)
                        .severity(ObjectDifference.Severity.BREAKING)
                        .build());
            }
        }
    }

    private void compareIndexes(TableSchema source, TableSchema dest,
                                String tableName, SchemaComparisonResult result) {
        Map<String, TableSchema.IndexDefinition> sourceMap = source.getIndexes().stream()
                .collect(Collectors.toMap(TableSchema.IndexDefinition::getIndexName, idx -> idx));

        Map<String, TableSchema.IndexDefinition> destMap = dest.getIndexes().stream()
                .collect(Collectors.toMap(TableSchema.IndexDefinition::getIndexName, idx -> idx));

        for (String idxName : sourceMap.keySet()) {
            if (!destMap.containsKey(idxName)) {
                TableSchema.IndexDefinition idx = sourceMap.get(idxName);
                result.addDifference(ObjectDifference.builder()
                        .objectType(ObjectDifference.ObjectType.INDEX)
                        .objectName(idxName)
                        .differenceType(ObjectDifference.DifferenceType.MISSING)
                        .severity(ObjectDifference.Severity.INFO)
                        .sourceDefinition(idx.getDefinition())
                        .build());
            }
        }

        for (String idxName : destMap.keySet()) {
            if (!sourceMap.containsKey(idxName)) {
                result.addDifference(ObjectDifference.builder()
                        .objectType(ObjectDifference.ObjectType.INDEX)
                        .objectName(idxName)
                        .differenceType(ObjectDifference.DifferenceType.EXTRA)
                        .severity(ObjectDifference.Severity.INFO)
                        .build());
            }
        }

        for (String idxName : sourceMap.keySet()) {
            if (destMap.containsKey(idxName)) {
                TableSchema.IndexDefinition sourceIdx = sourceMap.get(idxName);
                TableSchema.IndexDefinition destIdx = destMap.get(idxName);

                List<AttributeDifference> diffs = sourceIdx.getDifferencesFrom(destIdx);
                if (!diffs.isEmpty()) {
                    result.addDifference(ObjectDifference.builder()
                            .objectType(ObjectDifference.ObjectType.INDEX)
                            .objectName(idxName)
                            .differenceType(ObjectDifference.DifferenceType.MODIFIED)
                            .severity(ObjectDifference.Severity.INFO)
                            .attributeDifferences(diffs)
                            .sourceDefinition(sourceIdx.getDefinition())
                            .destinationDefinition(destIdx.getDefinition())
                            .build());
                }
            }
        }
    }

    private void compareTriggers(TableSchema source, TableSchema dest,
                                 String tableName, SchemaComparisonResult result) {
        Map<String, TableSchema.TriggerDefinition> sourceMap = source.getTriggers().stream()
                .collect(Collectors.toMap(TableSchema.TriggerDefinition::getTriggerName, t -> t));

        Map<String, TableSchema.TriggerDefinition> destMap = dest.getTriggers().stream()
                .collect(Collectors.toMap(TableSchema.TriggerDefinition::getTriggerName, t -> t));

        for (String trgName : sourceMap.keySet()) {
            if (!destMap.containsKey(trgName)) {
                TableSchema.TriggerDefinition trg = sourceMap.get(trgName);
                result.addDifference(ObjectDifference.builder()
                        .objectType(ObjectDifference.ObjectType.TRIGGER)
                        .objectName(tableName + "." + trgName)
                        .differenceType(ObjectDifference.DifferenceType.MISSING)
                        .severity(ObjectDifference.Severity.WARNING)
                        .sourceDefinition(trg.getDefinition())
                        .build());
            }
        }

        for (String trgName : destMap.keySet()) {
            if (!sourceMap.containsKey(trgName)) {
                result.addDifference(ObjectDifference.builder()
                        .objectType(ObjectDifference.ObjectType.TRIGGER)
                        .objectName(tableName + "." + trgName)
                        .differenceType(ObjectDifference.DifferenceType.EXTRA)
                        .severity(ObjectDifference.Severity.BREAKING)
                        .build());
            }
        }

        for (String trgName : sourceMap.keySet()) {
            if (destMap.containsKey(trgName)) {
                TableSchema.TriggerDefinition sourceTrg = sourceMap.get(trgName);
                TableSchema.TriggerDefinition destTrg = destMap.get(trgName);

                List<AttributeDifference> diffs = sourceTrg.getDifferencesFrom(destTrg);
                if (!diffs.isEmpty()) {
                    result.addDifference(ObjectDifference.builder()
                            .objectType(ObjectDifference.ObjectType.TRIGGER)
                            .objectName(tableName + "." + trgName)
                            .differenceType(ObjectDifference.DifferenceType.MODIFIED)
                            .severity(ObjectDifference.Severity.WARNING)
                            .attributeDifferences(diffs)
                            .sourceDefinition(sourceTrg.getDefinition())
                            .destinationDefinition(destTrg.getDefinition())
                            .build());
                }
            }
        }
    }

    private void compareTableAttributes(TableSchema source, TableSchema dest, SchemaComparisonResult result) {
        List<AttributeDifference> diffs = new ArrayList<>();

        if (!Objects.equals(source.getComment(), dest.getComment())) {
            diffs.add(AttributeDifference.builder()
                    .attributeName("comment")
                    .sourceValue(source.getComment())
                    .destinationValue(dest.getComment())
                    .build());
        }

        if (!Objects.equals(source.getOwner(), dest.getOwner())) {
            diffs.add(AttributeDifference.builder()
                    .attributeName("owner")
                    .sourceValue(source.getOwner())
                    .destinationValue(dest.getOwner())
                    .build());
        }

        if (!diffs.isEmpty()) {
            result.addDifference(ObjectDifference.builder()
                    .objectType(ObjectDifference.ObjectType.TABLE)
                    .objectName(source.getTableName())
                    .differenceType(ObjectDifference.DifferenceType.MODIFIED)
                    .severity(ObjectDifference.Severity.INFO)
                    .attributeDifferences(diffs)
                    .build());
        }
    }

    private void compareViews(Connection sourceConn, Connection destConn,
                              String sourceSchema, String destSchema,
                              ComparisonFilter filter, SchemaComparisonResult result) throws SQLException {
        List<ViewSchema> sourceViews = extractorService.extractViews(sourceConn, sourceSchema);
        List<ViewSchema> destViews = extractorService.extractViews(destConn, destSchema);

        Map<String, ViewSchema> sourceMap = sourceViews.stream()
                .filter(v -> filter.matchesTable(v.getViewName()))
                .collect(Collectors.toMap(ViewSchema::getViewName, v -> v));

        Map<String, ViewSchema> destMap = destViews.stream()
                .filter(v -> filter.matchesTable(v.getViewName()))
                .collect(Collectors.toMap(ViewSchema::getViewName, v -> v));

        for (String viewName : sourceMap.keySet()) {
            if (!destMap.containsKey(viewName)) {
                ViewSchema view = sourceMap.get(viewName);
                ObjectDifference.ObjectType type = view.isMaterialised() ?
                        ObjectDifference.ObjectType.MATERIALIZED_VIEW :
                        ObjectDifference.ObjectType.VIEW;

                result.addDifference(ObjectDifference.builder()
                        .objectType(type)
                        .objectName(viewName)
                        .differenceType(ObjectDifference.DifferenceType.MISSING)
                        .severity(ObjectDifference.Severity.INFO)
                        .sourceDefinition(view.getDefinition())
                        .build());
            }
        }

        for (String viewName : destMap.keySet()) {
            if (!sourceMap.containsKey(viewName)) {
                ViewSchema view = destMap.get(viewName);
                ObjectDifference.ObjectType type = view.isMaterialised() ?
                        ObjectDifference.ObjectType.MATERIALIZED_VIEW :
                        ObjectDifference.ObjectType.VIEW;

                result.addDifference(ObjectDifference.builder()
                        .objectType(type)
                        .objectName(viewName)
                        .differenceType(ObjectDifference.DifferenceType.EXTRA)
                        .severity(ObjectDifference.Severity.BREAKING)
                        .build());
            }
        }

        for (String viewName : sourceMap.keySet()) {
            if (destMap.containsKey(viewName)) {
                ViewSchema sourceView = sourceMap.get(viewName);
                ViewSchema destView = destMap.get(viewName);

                List<AttributeDifference> diffs = sourceView.getDifferencesFrom(destView);
                if (!diffs.isEmpty()) {
                    ObjectDifference.ObjectType type = sourceView.isMaterialised() ?
                            ObjectDifference.ObjectType.MATERIALIZED_VIEW :
                            ObjectDifference.ObjectType.VIEW;

                    result.addDifference(ObjectDifference.builder()
                            .objectType(type)
                            .objectName(viewName)
                            .differenceType(ObjectDifference.DifferenceType.MODIFIED)
                            .severity(ObjectDifference.Severity.WARNING)
                            .attributeDifferences(diffs)
                            .sourceDefinition(sourceView.getDefinition())
                            .destinationDefinition(destView.getDefinition())
                            .build());
                }
            }
        }
    }

    private void compareFunctions(Connection sourceConn, Connection destConn,
                                   String sourceSchema, String destSchema,
                                   ComparisonFilter filter, SchemaComparisonResult result) throws SQLException {
        List<FunctionSchema> sourceFuncs = extractorService.extractFunctions(sourceConn, sourceSchema);
        List<FunctionSchema> destFuncs = extractorService.extractFunctions(destConn, destSchema);

        Map<String, FunctionSchema> sourceMap = sourceFuncs.stream()
                .collect(Collectors.toMap(FunctionSchema::getSignature, f -> f));

        Map<String, FunctionSchema> destMap = destFuncs.stream()
                .collect(Collectors.toMap(FunctionSchema::getSignature, f -> f));

        for (String sig : sourceMap.keySet()) {
            if (!destMap.containsKey(sig)) {
                FunctionSchema func = sourceMap.get(sig);
                ObjectDifference.ObjectType type = func.getKind() == FunctionSchema.CallableKind.PROCEDURE ?
                        ObjectDifference.ObjectType.PROCEDURE :
                        ObjectDifference.ObjectType.FUNCTION;

                result.addDifference(ObjectDifference.builder()
                        .objectType(type)
                        .objectName(sig)
                        .differenceType(ObjectDifference.DifferenceType.MISSING)
                        .severity(ObjectDifference.Severity.INFO)
                        .sourceDefinition(func.getDefinition())
                        .build());
            }
        }

        for (String sig : destMap.keySet()) {
            if (!sourceMap.containsKey(sig)) {
                FunctionSchema func = destMap.get(sig);
                ObjectDifference.ObjectType type = func.getKind() == FunctionSchema.CallableKind.PROCEDURE ?
                        ObjectDifference.ObjectType.PROCEDURE :
                        ObjectDifference.ObjectType.FUNCTION;

                result.addDifference(ObjectDifference.builder()
                        .objectType(type)
                        .objectName(sig)
                        .differenceType(ObjectDifference.DifferenceType.EXTRA)
                        .severity(ObjectDifference.Severity.BREAKING)
                        .build());
            }
        }

        for (String sig : sourceMap.keySet()) {
            if (destMap.containsKey(sig)) {
                FunctionSchema sourceFunc = sourceMap.get(sig);
                FunctionSchema destFunc = destMap.get(sig);

                List<AttributeDifference> diffs = sourceFunc.getDifferencesFrom(destFunc);
                if (!diffs.isEmpty()) {
                    ObjectDifference.ObjectType type = sourceFunc.getKind() == FunctionSchema.CallableKind.PROCEDURE ?
                            ObjectDifference.ObjectType.PROCEDURE :
                            ObjectDifference.ObjectType.FUNCTION;

                    result.addDifference(ObjectDifference.builder()
                            .objectType(type)
                            .objectName(sig)
                            .differenceType(ObjectDifference.DifferenceType.MODIFIED)
                            .severity(ObjectDifference.Severity.WARNING)
                            .attributeDifferences(diffs)
                            .sourceDefinition(sourceFunc.getDefinition())
                            .destinationDefinition(destFunc.getDefinition())
                            .build());
                }
            }
        }
    }

    private void compareSequences(Connection sourceConn, Connection destConn,
                                   String sourceSchema, String destSchema,
                                   ComparisonFilter filter, SchemaComparisonResult result) throws SQLException {
        List<SequenceSchema> sourceSeqs = extractorService.extractSequences(sourceConn, sourceSchema);
        List<SequenceSchema> destSeqs = extractorService.extractSequences(destConn, destSchema);

        Map<String, SequenceSchema> sourceMap = sourceSeqs.stream()
                .collect(Collectors.toMap(SequenceSchema::getSequenceName, s -> s));

        Map<String, SequenceSchema> destMap = destSeqs.stream()
                .collect(Collectors.toMap(SequenceSchema::getSequenceName, s -> s));

        for (String seqName : sourceMap.keySet()) {
            if (!destMap.containsKey(seqName)) {
                SequenceSchema seq = sourceMap.get(seqName);
                result.addDifference(ObjectDifference.builder()
                        .objectType(ObjectDifference.ObjectType.SEQUENCE)
                        .objectName(seqName)
                        .differenceType(ObjectDifference.DifferenceType.MISSING)
                        .severity(ObjectDifference.Severity.INFO)
                        .sourceDefinition(ddlGeneratorService.generateSequenceDdl(seq, destSchema))
                        .build());
            }
        }

        for (String seqName : destMap.keySet()) {
            if (!sourceMap.containsKey(seqName)) {
                result.addDifference(ObjectDifference.builder()
                        .objectType(ObjectDifference.ObjectType.SEQUENCE)
                        .objectName(seqName)
                        .differenceType(ObjectDifference.DifferenceType.EXTRA)
                        .severity(ObjectDifference.Severity.BREAKING)
                        .build());
            }
        }

        for (String seqName : sourceMap.keySet()) {
            if (destMap.containsKey(seqName)) {
                SequenceSchema sourceSeq = sourceMap.get(seqName);
                SequenceSchema destSeq = destMap.get(seqName);

                List<AttributeDifference> diffs = sourceSeq.getDifferencesFrom(destSeq);
                if (!diffs.isEmpty()) {
                    result.addDifference(ObjectDifference.builder()
                            .objectType(ObjectDifference.ObjectType.SEQUENCE)
                            .objectName(seqName)
                            .differenceType(ObjectDifference.DifferenceType.MODIFIED)
                            .severity(ObjectDifference.Severity.INFO)
                            .attributeDifferences(diffs)
                            .build());
                }
            }
        }
    }

    private void compareTypes(Connection sourceConn, Connection destConn,
                               String sourceSchema, String destSchema,
                               ComparisonFilter filter, SchemaComparisonResult result) throws SQLException {
        List<TypeSchema> sourceTypes = extractorService.extractTypes(sourceConn, sourceSchema);
        List<TypeSchema> destTypes = extractorService.extractTypes(destConn, destSchema);

        Map<String, TypeSchema> sourceMap = sourceTypes.stream()
                .collect(Collectors.toMap(TypeSchema::getTypeName, t -> t));

        Map<String, TypeSchema> destMap = destTypes.stream()
                .collect(Collectors.toMap(TypeSchema::getTypeName, t -> t));

        for (String typeName : sourceMap.keySet()) {
            if (!destMap.containsKey(typeName)) {
                TypeSchema type = sourceMap.get(typeName);
                ObjectDifference.ObjectType objType = getObjectType(type);

                String definition = type.getKind() == TypeSchema.TypeKind.ENUM ?
                        ddlGeneratorService.generateEnumTypeDdl(type, destSchema) : null;

                result.addDifference(ObjectDifference.builder()
                        .objectType(objType)
                        .objectName(typeName)
                        .differenceType(ObjectDifference.DifferenceType.MISSING)
                        .severity(ObjectDifference.Severity.INFO)
                        .sourceDefinition(definition)
                        .build());
            }
        }

        for (String typeName : destMap.keySet()) {
            if (!sourceMap.containsKey(typeName)) {
                TypeSchema type = destMap.get(typeName);
                ObjectDifference.ObjectType objType = getObjectType(type);

                result.addDifference(ObjectDifference.builder()
                        .objectType(objType)
                        .objectName(typeName)
                        .differenceType(ObjectDifference.DifferenceType.EXTRA)
                        .severity(ObjectDifference.Severity.BREAKING)
                        .build());
            }
        }

        for (String typeName : sourceMap.keySet()) {
            if (destMap.containsKey(typeName)) {
                TypeSchema sourceType = sourceMap.get(typeName);
                TypeSchema destType = destMap.get(typeName);

                List<AttributeDifference> diffs = sourceType.getDifferencesFrom(destType);
                if (!diffs.isEmpty()) {
                    ObjectDifference.ObjectType objType = getObjectType(sourceType);

                    result.addDifference(ObjectDifference.builder()
                            .objectType(objType)
                            .objectName(typeName)
                            .differenceType(ObjectDifference.DifferenceType.MODIFIED)
                            .severity(ObjectDifference.Severity.WARNING)
                            .attributeDifferences(diffs)
                            .build());
                }
            }
        }
    }

    private ObjectDifference.ObjectType getObjectType(TypeSchema type) {
        return switch (type.getKind()) {
            case ENUM -> ObjectDifference.ObjectType.TYPE_ENUM;
            case COMPOSITE -> ObjectDifference.ObjectType.TYPE_COMPOSITE;
            case DOMAIN, RANGE -> ObjectDifference.ObjectType.TYPE_DOMAIN;
        };
    }

    private void compareExtensions(Connection sourceConn, Connection destConn,
                                    SchemaComparisonResult result) throws SQLException {
        Map<String, String> sourceExts = extractorService.extractExtensions(sourceConn);
        Map<String, String> destExts = extractorService.extractExtensions(destConn);

        for (String extName : sourceExts.keySet()) {
            if (!destExts.containsKey(extName)) {
                result.addDifference(ObjectDifference.builder()
                        .objectType(ObjectDifference.ObjectType.EXTENSION)
                        .objectName(extName)
                        .differenceType(ObjectDifference.DifferenceType.MISSING)
                        .severity(ObjectDifference.Severity.WARNING)
                        .sourceDefinition("CREATE EXTENSION IF NOT EXISTS " + extName)
                        .build());
            }
        }

        for (String extName : destExts.keySet()) {
            if (!sourceExts.containsKey(extName)) {
                result.addDifference(ObjectDifference.builder()
                        .objectType(ObjectDifference.ObjectType.EXTENSION)
                        .objectName(extName)
                        .differenceType(ObjectDifference.DifferenceType.EXTRA)
                        .severity(ObjectDifference.Severity.BREAKING)
                        .build());
            }
        }

        for (String extName : sourceExts.keySet()) {
            if (destExts.containsKey(extName)) {
                String sourceVer = sourceExts.get(extName);
                String destVer = destExts.get(extName);

                if (!Objects.equals(sourceVer, destVer)) {
                    result.addDifference(ObjectDifference.builder()
                            .objectType(ObjectDifference.ObjectType.EXTENSION)
                            .objectName(extName)
                            .differenceType(ObjectDifference.DifferenceType.MODIFIED)
                            .severity(ObjectDifference.Severity.INFO)
                            .attributeDifferences(List.of(
                                    AttributeDifference.builder()
                                            .attributeName("version")
                                            .sourceValue(sourceVer)
                                            .destinationValue(destVer)
                                            .build()
                            ))
                            .build());
                }
            }
        }
    }
}
