package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.sql.*;
import java.util.*;

/**
 * Service for extracting schema definitions from PostgreSQL system catalogs.
 * <p>
 * Queries pg_class, pg_attribute, pg_constraint, pg_index, pg_proc,
 * pg_trigger, pg_sequence, pg_type, and pg_extension to build comprehensive
 * schema representations for comparison.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class SchemaExtractorService {

    private static final Logger LOG = Logger.getLogger(SchemaExtractorService.class);

    @Inject
    DataSourceManager dataSourceManager;

    /**
     * Extracts all tables from a schema.
     *
     * @param instanceName database instance
     * @param schemaName schema to extract from
     * @return list of table definitions
     */
    public List<TableSchema> extractTables(String instanceName, String schemaName) {
        String sql = """
            SELECT c.relname AS table_name,
                   pg_get_userbyid(c.relowner) AS owner,
                   d.description AS comment,
                   c.relispartition AS is_partition,
                   c.relhassubclass AS has_subclass,
                   c.relrowsecurity AS row_security,
                   pg_get_partkeydef(c.oid) AS partition_key
            FROM pg_class c
            JOIN pg_namespace n ON n.oid = c.relnamespace
            LEFT JOIN pg_description d ON d.objoid = c.oid AND d.objsubid = 0
            WHERE n.nspname = ? AND c.relkind IN ('r', 'p')
            ORDER BY c.relname
            """;

        List<TableSchema> tables = new ArrayList<>();

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, schemaName);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    TableSchema table = new TableSchema();
                    table.setSchemaName(schemaName);
                    table.setTableName(rs.getString("table_name"));
                    table.setOwner(rs.getString("owner"));
                    table.setComment(rs.getString("comment"));
                    table.setPartition(rs.getBoolean("is_partition"));
                    table.setPartitionKey(rs.getString("partition_key"));

                    // Extract related objects
                    table.setColumns(extractColumns(conn, schemaName, table.getTableName()));
                    table.setPrimaryKey(extractPrimaryKey(conn, schemaName, table.getTableName()));
                    table.setForeignKeys(extractForeignKeys(conn, schemaName, table.getTableName()));
                    table.setUniqueConstraints(extractUniqueConstraints(conn, schemaName, table.getTableName()));
                    table.setCheckConstraints(extractCheckConstraints(conn, schemaName, table.getTableName()));
                    table.setIndexes(extractIndexes(conn, schemaName, table.getTableName()));
                    table.setTriggers(extractTableTriggers(conn, schemaName, table.getTableName()));

                    tables.add(table);
                }
            }

        } catch (SQLException e) {
            LOG.errorf("Failed to extract tables from %s.%s: %s", instanceName, schemaName, e.getMessage());
        }

        return tables;
    }

    /**
     * Extracts columns for a table.
     */
    private List<TableSchema.ColumnDefinition> extractColumns(Connection conn, String schemaName, String tableName) throws SQLException {
        String sql = """
            SELECT a.attname AS column_name,
                   pg_catalog.format_type(a.atttypid, a.atttypmod) AS data_type,
                   NOT a.attnotnull AS is_nullable,
                   pg_get_expr(d.adbin, d.adrelid) AS default_value,
                   a.attidentity AS identity,
                   a.attgenerated AS generated,
                   a.attnum AS ordinal_position,
                   col_description(c.oid, a.attnum) AS comment
            FROM pg_attribute a
            JOIN pg_class c ON c.oid = a.attrelid
            JOIN pg_namespace n ON n.oid = c.relnamespace
            LEFT JOIN pg_attrdef d ON d.adrelid = a.attrelid AND d.adnum = a.attnum
            WHERE n.nspname = ? AND c.relname = ?
              AND a.attnum > 0 AND NOT a.attisdropped
            ORDER BY a.attnum
            """;

        List<TableSchema.ColumnDefinition> columns = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, schemaName);
            stmt.setString(2, tableName);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    TableSchema.ColumnDefinition col = TableSchema.ColumnDefinition.builder()
                            .columnName(rs.getString("column_name"))
                            .dataType(rs.getString("data_type"))
                            .nullable(rs.getBoolean("is_nullable"))
                            .defaultValue(rs.getString("default_value"))
                            .identity(parseIdentityType(rs.getString("identity")))
                            .generated(parseGeneratedType(rs.getString("generated")))
                            .ordinalPosition(rs.getInt("ordinal_position"))
                            .comment(rs.getString("comment"))
                            .build();
                    columns.add(col);
                }
            }
        }

        return columns;
    }

    private String parseIdentityType(String identity) {
        if (identity == null || identity.isEmpty()) return null;
        return switch (identity) {
            case "a" -> "ALWAYS";
            case "d" -> "BY DEFAULT";
            default -> null;
        };
    }

    private String parseGeneratedType(String generated) {
        if (generated == null || generated.isEmpty()) return null;
        return switch (generated) {
            case "s" -> "STORED";
            default -> null;
        };
    }

    /**
     * Extracts primary key for a table.
     */
    private TableSchema.PrimaryKeyDefinition extractPrimaryKey(Connection conn, String schemaName, String tableName) throws SQLException {
        String sql = """
            SELECT c.conname AS constraint_name,
                   array_agg(a.attname ORDER BY array_position(c.conkey, a.attnum)) AS columns
            FROM pg_constraint c
            JOIN pg_class t ON c.conrelid = t.oid
            JOIN pg_namespace n ON t.relnamespace = n.oid
            JOIN pg_attribute a ON t.oid = a.attrelid AND a.attnum = ANY(c.conkey)
            WHERE c.contype = 'p' AND n.nspname = ? AND t.relname = ?
            GROUP BY c.conname
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, schemaName);
            stmt.setString(2, tableName);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return TableSchema.PrimaryKeyDefinition.builder()
                            .constraintName(rs.getString("constraint_name"))
                            .columns(arrayToList(rs.getArray("columns")))
                            .build();
                }
            }
        }

        return null;
    }

    /**
     * Extracts foreign keys for a table.
     */
    private List<TableSchema.ForeignKeyDefinition> extractForeignKeys(Connection conn, String schemaName, String tableName) throws SQLException {
        String sql = """
            SELECT c.conname AS constraint_name,
                   array_agg(a1.attname ORDER BY array_position(c.conkey, a1.attnum)) AS columns,
                   n2.nspname AS ref_schema,
                   t2.relname AS ref_table,
                   array_agg(a2.attname ORDER BY array_position(c.confkey, a2.attnum)) AS ref_columns,
                   c.confupdtype AS on_update,
                   c.confdeltype AS on_delete
            FROM pg_constraint c
            JOIN pg_class t1 ON c.conrelid = t1.oid
            JOIN pg_namespace n1 ON t1.relnamespace = n1.oid
            JOIN pg_class t2 ON c.confrelid = t2.oid
            JOIN pg_namespace n2 ON t2.relnamespace = n2.oid
            JOIN pg_attribute a1 ON t1.oid = a1.attrelid AND a1.attnum = ANY(c.conkey)
            JOIN pg_attribute a2 ON t2.oid = a2.attrelid AND a2.attnum = ANY(c.confkey)
            WHERE c.contype = 'f' AND n1.nspname = ? AND t1.relname = ?
            GROUP BY c.conname, n2.nspname, t2.relname, c.confupdtype, c.confdeltype
            """;

        List<TableSchema.ForeignKeyDefinition> fks = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, schemaName);
            stmt.setString(2, tableName);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    TableSchema.ForeignKeyDefinition fk = TableSchema.ForeignKeyDefinition.builder()
                            .constraintName(rs.getString("constraint_name"))
                            .columns(arrayToList(rs.getArray("columns")))
                            .referencedSchema(rs.getString("ref_schema"))
                            .referencedTable(rs.getString("ref_table"))
                            .referencedColumns(arrayToList(rs.getArray("ref_columns")))
                            .onUpdate(parseFkAction(rs.getString("on_update")))
                            .onDelete(parseFkAction(rs.getString("on_delete")))
                            .build();
                    fks.add(fk);
                }
            }
        }

        return fks;
    }

    private String parseFkAction(String action) {
        if (action == null) return "NO ACTION";
        return switch (action) {
            case "a" -> "NO ACTION";
            case "r" -> "RESTRICT";
            case "c" -> "CASCADE";
            case "n" -> "SET NULL";
            case "d" -> "SET DEFAULT";
            default -> "NO ACTION";
        };
    }

    /**
     * Extracts unique constraints for a table.
     */
    private List<TableSchema.UniqueConstraintDefinition> extractUniqueConstraints(Connection conn, String schemaName, String tableName) throws SQLException {
        String sql = """
            SELECT c.conname AS constraint_name,
                   array_agg(a.attname ORDER BY array_position(c.conkey, a.attnum)) AS columns
            FROM pg_constraint c
            JOIN pg_class t ON c.conrelid = t.oid
            JOIN pg_namespace n ON t.relnamespace = n.oid
            JOIN pg_attribute a ON t.oid = a.attrelid AND a.attnum = ANY(c.conkey)
            WHERE c.contype = 'u' AND n.nspname = ? AND t.relname = ?
            GROUP BY c.conname
            """;

        List<TableSchema.UniqueConstraintDefinition> constraints = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, schemaName);
            stmt.setString(2, tableName);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    constraints.add(TableSchema.UniqueConstraintDefinition.builder()
                            .constraintName(rs.getString("constraint_name"))
                            .columns(arrayToList(rs.getArray("columns")))
                            .build());
                }
            }
        }

        return constraints;
    }

    /**
     * Extracts check constraints for a table.
     */
    private List<TableSchema.CheckConstraintDefinition> extractCheckConstraints(Connection conn, String schemaName, String tableName) throws SQLException {
        String sql = """
            SELECT c.conname AS constraint_name,
                   pg_get_constraintdef(c.oid, true) AS expression
            FROM pg_constraint c
            JOIN pg_class t ON c.conrelid = t.oid
            JOIN pg_namespace n ON t.relnamespace = n.oid
            WHERE c.contype = 'c' AND n.nspname = ? AND t.relname = ?
              AND c.conname NOT LIKE '%_not_null'
            """;

        List<TableSchema.CheckConstraintDefinition> constraints = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, schemaName);
            stmt.setString(2, tableName);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    constraints.add(TableSchema.CheckConstraintDefinition.builder()
                            .constraintName(rs.getString("constraint_name"))
                            .expression(rs.getString("expression"))
                            .build());
                }
            }
        }

        return constraints;
    }

    /**
     * Extracts indexes for a table.
     */
    private List<TableSchema.IndexDefinition> extractIndexes(Connection conn, String schemaName, String tableName) throws SQLException {
        String sql = """
            SELECT i.relname AS index_name,
                   am.amname AS index_type,
                   pg_get_indexdef(i.oid) AS definition,
                   ix.indisunique AS is_unique,
                   ix.indisprimary AS is_primary,
                   pg_get_expr(ix.indpred, ix.indrelid) AS where_clause,
                   array_agg(a.attname ORDER BY array_position(ix.indkey, a.attnum)) AS columns
            FROM pg_index ix
            JOIN pg_class i ON i.oid = ix.indexrelid
            JOIN pg_class t ON t.oid = ix.indrelid
            JOIN pg_namespace n ON n.oid = t.relnamespace
            JOIN pg_am am ON am.oid = i.relam
            LEFT JOIN pg_attribute a ON t.oid = a.attrelid AND a.attnum = ANY(ix.indkey)
            WHERE n.nspname = ? AND t.relname = ? AND NOT ix.indisprimary
            GROUP BY i.relname, am.amname, i.oid, ix.indisunique, ix.indisprimary, ix.indpred, ix.indrelid
            """;

        List<TableSchema.IndexDefinition> indexes = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, schemaName);
            stmt.setString(2, tableName);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    indexes.add(TableSchema.IndexDefinition.builder()
                            .indexName(rs.getString("index_name"))
                            .indexType(rs.getString("index_type"))
                            .definition(rs.getString("definition"))
                            .unique(rs.getBoolean("is_unique"))
                            .whereClause(rs.getString("where_clause"))
                            .columns(arrayToList(rs.getArray("columns")))
                            .build());
                }
            }
        }

        return indexes;
    }

    /**
     * Extracts triggers for a table.
     */
    private List<TableSchema.TriggerDefinition> extractTableTriggers(Connection conn, String schemaName, String tableName) throws SQLException {
        String sql = """
            SELECT t.tgname AS trigger_name,
                   pg_get_triggerdef(t.oid, true) AS definition,
                   t.tgenabled AS enabled
            FROM pg_trigger t
            JOIN pg_class c ON c.oid = t.tgrelid
            JOIN pg_namespace n ON n.oid = c.relnamespace
            WHERE n.nspname = ? AND c.relname = ? AND NOT t.tgisinternal
            """;

        List<TableSchema.TriggerDefinition> triggers = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, schemaName);
            stmt.setString(2, tableName);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    triggers.add(TableSchema.TriggerDefinition.builder()
                            .triggerName(rs.getString("trigger_name"))
                            .definition(rs.getString("definition"))
                            .enabled(parseTriggerEnabled(rs.getString("enabled")))
                            .build());
                }
            }
        }

        return triggers;
    }

    private boolean parseTriggerEnabled(String enabled) {
        // O = enabled, D = disabled, R = replica, A = always
        return enabled != null && (enabled.equals("O") || enabled.equals("A"));
    }

    /**
     * Extracts all views from a schema.
     *
     * @param instanceName database instance
     * @param schemaName schema to extract from
     * @return list of view definitions
     */
    public List<ViewSchema> extractViews(String instanceName, String schemaName) {
        String sql = """
            SELECT c.relname AS view_name,
                   pg_get_viewdef(c.oid, true) AS definition,
                   c.relkind = 'm' AS is_materialised,
                   pg_get_userbyid(c.relowner) AS owner,
                   d.description AS comment
            FROM pg_class c
            JOIN pg_namespace n ON n.oid = c.relnamespace
            LEFT JOIN pg_description d ON d.objoid = c.oid AND d.objsubid = 0
            WHERE n.nspname = ? AND c.relkind IN ('v', 'm')
            ORDER BY c.relname
            """;

        List<ViewSchema> views = new ArrayList<>();

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, schemaName);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ViewSchema view = ViewSchema.builder()
                            .schemaName(schemaName)
                            .viewName(rs.getString("view_name"))
                            .definition(rs.getString("definition"))
                            .materialised(rs.getBoolean("is_materialised"))
                            .owner(rs.getString("owner"))
                            .comment(rs.getString("comment"))
                            .build();

                    // Extract columns for the view
                    view.setViewColumns(extractViewColumns(conn, schemaName, view.getViewName()));

                    // For materialised views, extract indexes
                    if (view.isMaterialised()) {
                        view.setIndexes(extractMaterialisedViewIndexes(conn, schemaName, view.getViewName()));
                    }

                    views.add(view);
                }
            }

        } catch (SQLException e) {
            LOG.errorf("Failed to extract views from %s.%s: %s", instanceName, schemaName, e.getMessage());
        }

        return views;
    }

    private List<ViewSchema.ViewColumn> extractViewColumns(Connection conn, String schemaName, String viewName) throws SQLException {
        String sql = """
            SELECT a.attname AS column_name,
                   pg_catalog.format_type(a.atttypid, a.atttypmod) AS data_type,
                   a.attnum AS ordinal_position
            FROM pg_attribute a
            JOIN pg_class c ON c.oid = a.attrelid
            JOIN pg_namespace n ON n.oid = c.relnamespace
            WHERE n.nspname = ? AND c.relname = ?
              AND a.attnum > 0 AND NOT a.attisdropped
            ORDER BY a.attnum
            """;

        List<ViewSchema.ViewColumn> columns = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, schemaName);
            stmt.setString(2, viewName);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    columns.add(new ViewSchema.ViewColumn(
                            rs.getString("column_name"),
                            rs.getString("data_type"),
                            rs.getInt("ordinal_position")
                    ));
                }
            }
        }

        return columns;
    }

    private List<String> extractMaterialisedViewIndexes(Connection conn, String schemaName, String viewName) throws SQLException {
        String sql = """
            SELECT i.relname AS index_name
            FROM pg_index ix
            JOIN pg_class i ON i.oid = ix.indexrelid
            JOIN pg_class t ON t.oid = ix.indrelid
            JOIN pg_namespace n ON n.oid = t.relnamespace
            WHERE n.nspname = ? AND t.relname = ?
            """;

        List<String> indexes = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, schemaName);
            stmt.setString(2, viewName);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    indexes.add(rs.getString("index_name"));
                }
            }
        }

        return indexes;
    }

    /**
     * Extracts all functions and procedures from a schema.
     *
     * @param instanceName database instance
     * @param schemaName schema to extract from
     * @return list of function definitions
     */
    public List<FunctionSchema> extractFunctions(String instanceName, String schemaName) {
        String sql = """
            SELECT p.proname AS function_name,
                   pg_get_function_identity_arguments(p.oid) AS arguments,
                   pg_get_functiondef(p.oid) AS definition,
                   pg_get_function_result(p.oid) AS return_type,
                   l.lanname AS language,
                   p.prokind AS kind,
                   p.provolatile AS volatility,
                   p.proisstrict AS is_strict,
                   p.prosecdef AS security_definer,
                   pg_get_userbyid(p.proowner) AS owner,
                   d.description AS comment
            FROM pg_proc p
            JOIN pg_namespace n ON n.oid = p.pronamespace
            JOIN pg_language l ON l.oid = p.prolang
            LEFT JOIN pg_description d ON d.objoid = p.oid
            WHERE n.nspname = ? AND p.prokind IN ('f', 'p', 'a', 'w')
            ORDER BY p.proname, pg_get_function_identity_arguments(p.oid)
            """;

        List<FunctionSchema> functions = new ArrayList<>();

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, schemaName);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    FunctionSchema func = FunctionSchema.builder()
                            .schemaName(schemaName)
                            .functionName(rs.getString("function_name"))
                            .arguments(rs.getString("arguments"))
                            .definition(rs.getString("definition"))
                            .returnType(rs.getString("return_type"))
                            .language(rs.getString("language"))
                            .kind(parseCallableKind(rs.getString("kind")))
                            .volatility(parseVolatility(rs.getString("volatility")))
                            .strict(rs.getBoolean("is_strict"))
                            .securityDefiner(rs.getBoolean("security_definer"))
                            .owner(rs.getString("owner"))
                            .comment(rs.getString("comment"))
                            .build();
                    functions.add(func);
                }
            }

        } catch (SQLException e) {
            LOG.errorf("Failed to extract functions from %s.%s: %s", instanceName, schemaName, e.getMessage());
        }

        return functions;
    }

    private FunctionSchema.CallableKind parseCallableKind(String kind) {
        if (kind == null) return FunctionSchema.CallableKind.FUNCTION;
        return switch (kind) {
            case "f" -> FunctionSchema.CallableKind.FUNCTION;
            case "p" -> FunctionSchema.CallableKind.PROCEDURE;
            case "a" -> FunctionSchema.CallableKind.AGGREGATE;
            case "w" -> FunctionSchema.CallableKind.WINDOW;
            default -> FunctionSchema.CallableKind.FUNCTION;
        };
    }

    private FunctionSchema.Volatility parseVolatility(String vol) {
        if (vol == null) return FunctionSchema.Volatility.VOLATILE;
        return switch (vol) {
            case "i" -> FunctionSchema.Volatility.IMMUTABLE;
            case "s" -> FunctionSchema.Volatility.STABLE;
            case "v" -> FunctionSchema.Volatility.VOLATILE;
            default -> FunctionSchema.Volatility.VOLATILE;
        };
    }

    /**
     * Extracts all sequences from a schema.
     *
     * @param instanceName database instance
     * @param schemaName schema to extract from
     * @return list of sequence definitions
     */
    public List<SequenceSchema> extractSequences(String instanceName, String schemaName) {
        // PostgreSQL 10+ uses pg_sequence
        String sql = """
            SELECT c.relname AS sequence_name,
                   s.seqstart AS start_value,
                   s.seqincrement AS increment,
                   s.seqmin AS min_value,
                   s.seqmax AS max_value,
                   s.seqcache AS cache_size,
                   s.seqcycle AS is_cycle,
                   pg_get_userbyid(c.relowner) AS owner,
                   d.description AS comment,
                   pg_catalog.format_type(s.seqtypid, NULL) AS data_type
            FROM pg_sequence s
            JOIN pg_class c ON c.oid = s.seqrelid
            JOIN pg_namespace n ON n.oid = c.relnamespace
            LEFT JOIN pg_description d ON d.objoid = c.oid AND d.objsubid = 0
            WHERE n.nspname = ?
            ORDER BY c.relname
            """;

        List<SequenceSchema> sequences = new ArrayList<>();

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, schemaName);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    SequenceSchema seq = SequenceSchema.builder()
                            .schemaName(schemaName)
                            .sequenceName(rs.getString("sequence_name"))
                            .startValue(rs.getLong("start_value"))
                            .increment(rs.getLong("increment"))
                            .minValue(rs.getLong("min_value"))
                            .maxValue(rs.getLong("max_value"))
                            .cacheSize(rs.getLong("cache_size"))
                            .cycle(rs.getBoolean("is_cycle"))
                            .owner(rs.getString("owner"))
                            .comment(rs.getString("comment"))
                            .dataType(rs.getString("data_type"))
                            .build();

                    // Check if sequence is owned by a column (serial/identity)
                    String owned = getSequenceOwner(conn, schemaName, seq.getSequenceName());
                    if (owned != null && owned.contains(".")) {
                        String[] parts = owned.split("\\.", 2);
                        seq.setOwnedBy(parts[0], parts[1]);
                    }

                    sequences.add(seq);
                }
            }

        } catch (SQLException e) {
            LOG.errorf("Failed to extract sequences from %s.%s: %s", instanceName, schemaName, e.getMessage());
        }

        return sequences;
    }

    private String getSequenceOwner(Connection conn, String schemaName, String sequenceName) throws SQLException {
        String sql = """
            SELECT d.refobjid::regclass || '.' || a.attname AS owned_by
            FROM pg_depend d
            JOIN pg_class c ON c.oid = d.objid
            JOIN pg_namespace n ON n.oid = c.relnamespace
            JOIN pg_attribute a ON a.attrelid = d.refobjid AND a.attnum = d.refobjsubid
            WHERE n.nspname = ? AND c.relname = ?
              AND d.deptype = 'a' AND d.classid = 'pg_class'::regclass
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, schemaName);
            stmt.setString(2, sequenceName);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("owned_by");
                }
            }
        }

        return null;
    }

    /**
     * Extracts all custom types from a schema.
     *
     * @param instanceName database instance
     * @param schemaName schema to extract from
     * @return list of type definitions
     */
    public List<TypeSchema> extractTypes(String instanceName, String schemaName) {
        List<TypeSchema> types = new ArrayList<>();

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection()) {
            types.addAll(extractEnumTypes(conn, schemaName));
            types.addAll(extractCompositeTypes(conn, schemaName));
            types.addAll(extractDomainTypes(conn, schemaName));
            types.addAll(extractRangeTypes(conn, schemaName));
        } catch (SQLException e) {
            LOG.errorf("Failed to extract types from %s.%s: %s", instanceName, schemaName, e.getMessage());
        }

        return types;
    }

    private List<TypeSchema> extractEnumTypes(Connection conn, String schemaName) throws SQLException {
        String sql = """
            SELECT t.typname AS type_name,
                   array_agg(e.enumlabel ORDER BY e.enumsortorder) AS labels,
                   pg_get_userbyid(t.typowner) AS owner,
                   d.description AS comment
            FROM pg_enum e
            JOIN pg_type t ON e.enumtypid = t.oid
            JOIN pg_namespace n ON t.typnamespace = n.oid
            LEFT JOIN pg_description d ON d.objoid = t.oid
            WHERE n.nspname = ?
            GROUP BY t.typname, t.typowner, d.description
            ORDER BY t.typname
            """;

        List<TypeSchema> types = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, schemaName);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    TypeSchema type = TypeSchema.builder()
                            .schemaName(schemaName)
                            .typeName(rs.getString("type_name"))
                            .kind(TypeSchema.TypeKind.ENUM)
                            .enumLabels(arrayToList(rs.getArray("labels")))
                            .owner(rs.getString("owner"))
                            .comment(rs.getString("comment"))
                            .build();
                    types.add(type);
                }
            }
        }

        return types;
    }

    private List<TypeSchema> extractCompositeTypes(Connection conn, String schemaName) throws SQLException {
        String sql = """
            SELECT t.typname AS type_name,
                   pg_get_userbyid(t.typowner) AS owner,
                   d.description AS comment
            FROM pg_type t
            JOIN pg_namespace n ON t.typnamespace = n.oid
            LEFT JOIN pg_description d ON d.objoid = t.oid
            WHERE n.nspname = ? AND t.typtype = 'c'
              AND NOT EXISTS (
                  SELECT 1 FROM pg_class c
                  WHERE c.reltype = t.oid AND c.relkind IN ('r', 'v', 'm', 'f', 'p')
              )
            ORDER BY t.typname
            """;

        List<TypeSchema> types = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, schemaName);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String typeName = rs.getString("type_name");
                    TypeSchema type = TypeSchema.builder()
                            .schemaName(schemaName)
                            .typeName(typeName)
                            .kind(TypeSchema.TypeKind.COMPOSITE)
                            .attributes(extractCompositeAttributes(conn, schemaName, typeName))
                            .owner(rs.getString("owner"))
                            .comment(rs.getString("comment"))
                            .build();
                    types.add(type);
                }
            }
        }

        return types;
    }

    private List<TypeSchema.CompositeAttribute> extractCompositeAttributes(Connection conn, String schemaName, String typeName) throws SQLException {
        String sql = """
            SELECT a.attname AS attribute_name,
                   pg_catalog.format_type(a.atttypid, a.atttypmod) AS data_type,
                   a.attnum AS ordinal_position
            FROM pg_attribute a
            JOIN pg_type t ON t.typrelid = a.attrelid
            JOIN pg_namespace n ON t.typnamespace = n.oid
            WHERE n.nspname = ? AND t.typname = ?
              AND a.attnum > 0 AND NOT a.attisdropped
            ORDER BY a.attnum
            """;

        List<TypeSchema.CompositeAttribute> attributes = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, schemaName);
            stmt.setString(2, typeName);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    attributes.add(new TypeSchema.CompositeAttribute(
                            rs.getString("attribute_name"),
                            rs.getString("data_type"),
                            rs.getInt("ordinal_position")
                    ));
                }
            }
        }

        return attributes;
    }

    private List<TypeSchema> extractDomainTypes(Connection conn, String schemaName) throws SQLException {
        String sql = """
            SELECT t.typname AS type_name,
                   pg_catalog.format_type(t.typbasetype, t.typtypmod) AS base_type,
                   t.typnotnull AS not_null,
                   pg_get_expr(t.typdefaultbin, 0) AS default_value,
                   pg_get_userbyid(t.typowner) AS owner,
                   d.description AS comment
            FROM pg_type t
            JOIN pg_namespace n ON t.typnamespace = n.oid
            LEFT JOIN pg_description d ON d.objoid = t.oid
            WHERE n.nspname = ? AND t.typtype = 'd'
            ORDER BY t.typname
            """;

        List<TypeSchema> types = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, schemaName);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String typeName = rs.getString("type_name");
                    TypeSchema type = TypeSchema.builder()
                            .schemaName(schemaName)
                            .typeName(typeName)
                            .kind(TypeSchema.TypeKind.DOMAIN)
                            .baseType(rs.getString("base_type"))
                            .notNull(rs.getBoolean("not_null"))
                            .defaultValue(rs.getString("default_value"))
                            .domainConstraints(extractDomainConstraints(conn, schemaName, typeName))
                            .owner(rs.getString("owner"))
                            .comment(rs.getString("comment"))
                            .build();
                    types.add(type);
                }
            }
        }

        return types;
    }

    private List<String> extractDomainConstraints(Connection conn, String schemaName, String typeName) throws SQLException {
        String sql = """
            SELECT pg_get_constraintdef(c.oid, true) AS constraint_def
            FROM pg_constraint c
            JOIN pg_type t ON c.contypid = t.oid
            JOIN pg_namespace n ON t.typnamespace = n.oid
            WHERE n.nspname = ? AND t.typname = ?
            ORDER BY c.conname
            """;

        List<String> constraints = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, schemaName);
            stmt.setString(2, typeName);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    constraints.add(rs.getString("constraint_def"));
                }
            }
        }

        return constraints;
    }

    private List<TypeSchema> extractRangeTypes(Connection conn, String schemaName) throws SQLException {
        String sql = """
            SELECT t.typname AS type_name,
                   pg_catalog.format_type(r.rngsubtype, NULL) AS subtype,
                   pg_get_userbyid(t.typowner) AS owner,
                   d.description AS comment
            FROM pg_range r
            JOIN pg_type t ON t.oid = r.rngtypid
            JOIN pg_namespace n ON t.typnamespace = n.oid
            LEFT JOIN pg_description d ON d.objoid = t.oid
            WHERE n.nspname = ?
            ORDER BY t.typname
            """;

        List<TypeSchema> types = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, schemaName);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    TypeSchema type = TypeSchema.builder()
                            .schemaName(schemaName)
                            .typeName(rs.getString("type_name"))
                            .kind(TypeSchema.TypeKind.RANGE)
                            .baseType(rs.getString("subtype"))
                            .owner(rs.getString("owner"))
                            .comment(rs.getString("comment"))
                            .build();
                    types.add(type);
                }
            }
        }

        return types;
    }

    /**
     * Extracts all extensions from a database.
     *
     * @param instanceName database instance
     * @return list of extension names with versions
     */
    public Map<String, String> extractExtensions(String instanceName) {
        String sql = """
            SELECT e.extname AS name,
                   e.extversion AS version
            FROM pg_extension e
            ORDER BY e.extname
            """;

        Map<String, String> extensions = new LinkedHashMap<>();

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                extensions.put(rs.getString("name"), rs.getString("version"));
            }

        } catch (SQLException e) {
            LOG.errorf("Failed to extract extensions from %s: %s", instanceName, e.getMessage());
        }

        return extensions;
    }

    /**
     * Gets list of available schemas in a database.
     *
     * @param instanceName database instance
     * @return list of schema names
     */
    public List<String> getSchemas(String instanceName) {
        String sql = """
            SELECT nspname
            FROM pg_namespace
            WHERE nspname NOT LIKE 'pg_%'
              AND nspname != 'information_schema'
            ORDER BY nspname
            """;

        List<String> schemas = new ArrayList<>();

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                schemas.add(rs.getString("nspname"));
            }

        } catch (SQLException e) {
            LOG.errorf("Failed to get schemas from %s: %s", instanceName, e.getMessage());
        }

        return schemas;
    }

    /**
     * Gets summary statistics for a schema.
     *
     * @param instanceName database instance
     * @param schemaName schema name
     * @return map of object type to count
     */
    public Map<String, Integer> getSchemaSummary(String instanceName, String schemaName) {
        Map<String, Integer> summary = new LinkedHashMap<>();

        String sql = """
            SELECT
                COUNT(*) FILTER (WHERE c.relkind IN ('r', 'p')) AS tables,
                COUNT(*) FILTER (WHERE c.relkind = 'v') AS views,
                COUNT(*) FILTER (WHERE c.relkind = 'm') AS matviews,
                COUNT(*) FILTER (WHERE c.relkind = 'S') AS sequences,
                COUNT(*) FILTER (WHERE c.relkind = 'i') AS indexes
            FROM pg_class c
            JOIN pg_namespace n ON n.oid = c.relnamespace
            WHERE n.nspname = ?
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, schemaName);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    summary.put("Tables", rs.getInt("tables"));
                    summary.put("Views", rs.getInt("views"));
                    summary.put("Materialised Views", rs.getInt("matviews"));
                    summary.put("Sequences", rs.getInt("sequences"));
                    summary.put("Indexes", rs.getInt("indexes"));
                }
            }

            // Count functions separately
            String funcSql = """
                SELECT COUNT(*) AS count
                FROM pg_proc p
                JOIN pg_namespace n ON n.oid = p.pronamespace
                WHERE n.nspname = ?
                """;

            try (PreparedStatement funcStmt = conn.prepareStatement(funcSql)) {
                funcStmt.setString(1, schemaName);
                try (ResultSet rs = funcStmt.executeQuery()) {
                    if (rs.next()) {
                        summary.put("Functions", rs.getInt("count"));
                    }
                }
            }

            // Count types separately
            String typeSql = """
                SELECT COUNT(*) AS count
                FROM pg_type t
                JOIN pg_namespace n ON t.typnamespace = n.oid
                WHERE n.nspname = ? AND t.typtype IN ('e', 'c', 'd')
                  AND NOT EXISTS (
                      SELECT 1 FROM pg_class c
                      WHERE c.reltype = t.oid AND c.relkind IN ('r', 'v', 'm', 'f', 'p')
                  )
                """;

            try (PreparedStatement typeStmt = conn.prepareStatement(typeSql)) {
                typeStmt.setString(1, schemaName);
                try (ResultSet rs = typeStmt.executeQuery()) {
                    if (rs.next()) {
                        summary.put("Types", rs.getInt("count"));
                    }
                }
            }

        } catch (SQLException e) {
            LOG.errorf("Failed to get schema summary from %s.%s: %s", instanceName, schemaName, e.getMessage());
        }

        return summary;
    }

    private List<String> arrayToList(Array array) throws SQLException {
        if (array == null) return new ArrayList<>();
        Object[] values = (Object[]) array.getArray();
        List<String> result = new ArrayList<>();
        for (Object value : values) {
            if (value != null) {
                result.add(value.toString());
            }
        }
        return result;
    }
}
