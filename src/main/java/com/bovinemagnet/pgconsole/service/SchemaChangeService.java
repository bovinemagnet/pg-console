package com.bovinemagnet.pgconsole.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for schema change detection and event trigger management.
 * Monitors DDL events, object dependencies, and schema structure.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class SchemaChangeService {

    private static final Logger LOG = Logger.getLogger(SchemaChangeService.class);

    @Inject
    DataSourceManager dataSourceManager;

    /**
     * Retrieves all event triggers defined in the specified database instance.
     * <p>
     * Event triggers are database-wide triggers that fire for DDL (Data Definition Language) events
     * such as CREATE, ALTER, and DROP commands. This method queries the {@code pg_event_trigger}
     * system catalogue to retrieve trigger metadata including the trigger name, event type, owner,
     * associated function, enabled status, and command tags that activate the trigger.
     * <p>
     * The function definition for each trigger is also retrieved to facilitate analysis of the
     * trigger's behaviour.
     *
     * @param instanceName the name of the PostgreSQL database instance to query
     * @return a list of {@link EventTrigger} objects ordered by trigger name; returns an empty list
     *         if the query fails or no event triggers exist
     * @see EventTrigger
     */
    public List<EventTrigger> getEventTriggers(String instanceName) {
        List<EventTrigger> triggers = new ArrayList<>();

        String sql = """
            SELECT
                e.evtname as name,
                e.evtevent as event,
                e.evtowner::regrole::text as owner,
                e.evtfoid::regproc::text as function_name,
                e.evtenabled as enabled,
                e.evttags as tags,
                pg_get_functiondef(e.evtfoid) as function_def
            FROM pg_event_trigger e
            ORDER BY e.evtname
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                EventTrigger trigger = new EventTrigger();
                trigger.setName(rs.getString("name"));
                trigger.setEvent(rs.getString("event"));
                trigger.setOwner(rs.getString("owner"));
                trigger.setFunctionName(rs.getString("function_name"));
                trigger.setEnabled(rs.getString("enabled"));
                trigger.setFunctionDef(rs.getString("function_def"));

                var tags = rs.getArray("tags");
                if (tags != null) {
                    trigger.setTags(List.of((String[]) tags.getArray()));
                }

                triggers.add(trigger);
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to get event triggers for %s: %s", instanceName, e.getMessage());
        }

        return triggers;
    }

    /**
     * Retrieves dependency relationships for a specific database object.
     * <p>
     * Queries the {@code pg_depend} system catalogue to identify all objects that depend on the
     * specified object. This is useful for understanding the impact of schema changes, as dropping
     * or modifying an object may affect dependent objects such as views, functions, or triggers.
     * <p>
     * Only normal ('n') and automatic ('a') dependencies are included, filtering out internal
     * PostgreSQL dependencies that are not relevant for schema analysis.
     *
     * @param instanceName the name of the PostgreSQL database instance to query
     * @param objectName the fully qualified name of the database object (e.g., "schema.table")
     * @return a list of {@link ObjectDependency} objects ordered by dependent object name; returns
     *         an empty list if the query fails or no dependencies exist
     * @see ObjectDependency
     */
    public List<ObjectDependency> getObjectDependencies(String instanceName, String objectName) {
        List<ObjectDependency> dependencies = new ArrayList<>();

        String sql = """
            SELECT DISTINCT
                d.classid::regclass::text as dependent_type,
                d.objid::regclass::text as dependent_name,
                d.refclassid::regclass::text as referenced_type,
                d.refobjid::regclass::text as referenced_name,
                d.deptype
            FROM pg_depend d
            WHERE d.refobjid = ?::regclass
              AND d.deptype IN ('n', 'a')
            ORDER BY dependent_name
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, objectName);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ObjectDependency dep = new ObjectDependency();
                    dep.setDependentType(rs.getString("dependent_type"));
                    dep.setDependentName(rs.getString("dependent_name"));
                    dep.setReferencedType(rs.getString("referenced_type"));
                    dep.setReferencedName(rs.getString("referenced_name"));
                    dep.setDependencyType(rs.getString("deptype"));
                    dependencies.add(dep);
                }
            }
        } catch (SQLException e) {
            LOG.debugf("Failed to get dependencies for %s.%s: %s", instanceName, objectName, e.getMessage());
        }

        return dependencies;
    }

    /**
     * Retrieves all foreign key constraint relationships in the specified database instance.
     * <p>
     * Queries the {@code information_schema} views to extract foreign key metadata including
     * the source table, source column, referenced table, referenced column, and the cascade
     * rules for updates and deletes. This information is essential for understanding referential
     * integrity constraints and planning schema modifications.
     * <p>
     * System schemas (pg_catalog, information_schema, pgconsole) are excluded from the results.
     * Results are ordered by schema, table, and constraint name for consistent presentation.
     *
     * @param instanceName the name of the PostgreSQL database instance to query
     * @return a list of {@link ForeignKeyRelationship} objects ordered by schema and table name;
     *         returns an empty list if the query fails or no foreign keys exist
     * @see ForeignKeyRelationship
     */
    public List<ForeignKeyRelationship> getForeignKeyRelationships(String instanceName) {
        List<ForeignKeyRelationship> relationships = new ArrayList<>();

        String sql = """
            SELECT
                tc.constraint_name,
                tc.table_schema as from_schema,
                tc.table_name as from_table,
                kcu.column_name as from_column,
                ccu.table_schema as to_schema,
                ccu.table_name as to_table,
                ccu.column_name as to_column,
                rc.update_rule,
                rc.delete_rule
            FROM information_schema.table_constraints tc
            JOIN information_schema.key_column_usage kcu
                ON tc.constraint_name = kcu.constraint_name
                AND tc.table_schema = kcu.table_schema
            JOIN information_schema.constraint_column_usage ccu
                ON ccu.constraint_name = tc.constraint_name
                AND ccu.table_schema = tc.table_schema
            JOIN information_schema.referential_constraints rc
                ON rc.constraint_name = tc.constraint_name
                AND rc.constraint_schema = tc.table_schema
            WHERE tc.constraint_type = 'FOREIGN KEY'
              AND tc.table_schema NOT IN ('pg_catalog', 'information_schema', 'pgconsole')
            ORDER BY tc.table_schema, tc.table_name, tc.constraint_name
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                ForeignKeyRelationship fk = new ForeignKeyRelationship();
                fk.setConstraintName(rs.getString("constraint_name"));
                fk.setFromSchema(rs.getString("from_schema"));
                fk.setFromTable(rs.getString("from_table"));
                fk.setFromColumn(rs.getString("from_column"));
                fk.setToSchema(rs.getString("to_schema"));
                fk.setToTable(rs.getString("to_table"));
                fk.setToColumn(rs.getString("to_column"));
                fk.setUpdateRule(rs.getString("update_rule"));
                fk.setDeleteRule(rs.getString("delete_rule"));
                relationships.add(fk);
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to get foreign keys for %s: %s", instanceName, e.getMessage());
        }

        return relationships;
    }

    /**
     * Retrieves dependency relationships for views and materialised views in the database.
     * <p>
     * Identifies the base tables and other objects that views depend upon by querying the
     * {@code pg_views} system view and correlating it with dependency information from
     * {@code pg_depend}. This is critical for understanding the impact of table modifications
     * on dependent views, as changes to base tables may require view updates or recreation.
     * <p>
     * Only normal dependencies on class objects (typically tables) are included, excluding
     * self-references and system objects.
     *
     * @param instanceName the name of the PostgreSQL database instance to query
     * @return a list of {@link ViewDependency} objects ordered by schema and view name; returns
     *         an empty list if the query fails or no view dependencies exist
     * @see ViewDependency
     */
    public List<ViewDependency> getViewDependencies(String instanceName) {
        List<ViewDependency> dependencies = new ArrayList<>();

        String sql = """
            SELECT
                v.schemaname as view_schema,
                v.viewname as view_name,
                d.refclassid::regclass::text as dep_type,
                d.refobjid::regclass::text as dep_name
            FROM pg_views v
            JOIN pg_class c ON c.relname = v.viewname
            JOIN pg_namespace n ON n.oid = c.relnamespace AND n.nspname = v.schemaname
            JOIN pg_depend d ON d.objid = c.oid
            WHERE v.schemaname NOT IN ('pg_catalog', 'information_schema', 'pgconsole')
              AND d.deptype = 'n'
              AND d.refclassid = 'pg_class'::regclass
              AND d.refobjid != c.oid
            ORDER BY v.schemaname, v.viewname
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                ViewDependency dep = new ViewDependency();
                dep.setViewSchema(rs.getString("view_schema"));
                dep.setViewName(rs.getString("view_name"));
                dep.setDependsOnType(rs.getString("dep_type"));
                dep.setDependsOnName(rs.getString("dep_name"));
                dependencies.add(dep);
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to get view dependencies for %s: %s", instanceName, e.getMessage());
        }

        return dependencies;
    }

    /**
     * Retrieves dependency relationships for user-defined functions and procedures.
     * <p>
     * Identifies the database objects (typically tables) that functions and procedures depend upon
     * by querying the {@code pg_proc} system catalogue and correlating it with {@code pg_depend}.
     * This helps understand which functions may be affected by table schema changes and aids in
     * impact analysis for database modifications.
     * <p>
     * Results are limited to 100 function dependencies to avoid excessive data retrieval in
     * databases with numerous stored procedures.
     *
     * @param instanceName the name of the PostgreSQL database instance to query
     * @return a list of {@link FunctionDependency} objects ordered by schema and function name;
     *         returns an empty list if the query fails or no function dependencies exist
     * @see FunctionDependency
     */
    public List<FunctionDependency> getFunctionDependencies(String instanceName) {
        List<FunctionDependency> dependencies = new ArrayList<>();

        String sql = """
            SELECT
                p.proname as function_name,
                n.nspname as schema_name,
                pg_get_function_identity_arguments(p.oid) as arguments,
                d.refclassid::regclass::text as dep_type,
                d.refobjid::regclass::text as dep_name
            FROM pg_proc p
            JOIN pg_namespace n ON n.oid = p.pronamespace
            JOIN pg_depend d ON d.objid = p.oid
            WHERE n.nspname NOT IN ('pg_catalog', 'information_schema', 'pgconsole')
              AND d.deptype = 'n'
              AND d.refclassid = 'pg_class'::regclass
            ORDER BY n.nspname, p.proname
            LIMIT 100
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                FunctionDependency dep = new FunctionDependency();
                dep.setFunctionName(rs.getString("function_name"));
                dep.setSchemaName(rs.getString("schema_name"));
                dep.setArguments(rs.getString("arguments"));
                dep.setDependsOnType(rs.getString("dep_type"));
                dep.setDependsOnName(rs.getString("dep_name"));
                dependencies.add(dep);
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to get function dependencies for %s: %s", instanceName, e.getMessage());
        }

        return dependencies;
    }

    /**
     * Retrieves aggregate schema statistics for the specified database instance.
     * <p>
     * Provides a database-wide summary of schema objects including counts of event triggers
     * (both total and enabled), foreign key constraints, views, materialised views, and
     * user-defined functions. This summary is useful for understanding the overall schema
     * complexity and identifying potential areas requiring attention.
     * <p>
     * System schemas (pg_catalog, information_schema, pgconsole) are excluded from all counts.
     *
     * @param instanceName the name of the PostgreSQL database instance to query
     * @return a {@link SchemaSummary} object containing aggregate schema statistics; returns an
     *         object with default values (zeros) if the query fails
     * @see SchemaSummary
     */
    public SchemaSummary getSummary(String instanceName) {
        SchemaSummary summary = new SchemaSummary();

        String sql = """
            SELECT
                (SELECT count(*) FROM pg_event_trigger) as event_trigger_count,
                (SELECT count(*) FROM pg_event_trigger WHERE evtenabled != 'D') as enabled_triggers,
                (SELECT count(*) FROM information_schema.table_constraints
                 WHERE constraint_type = 'FOREIGN KEY'
                 AND table_schema NOT IN ('pg_catalog', 'information_schema', 'pgconsole')) as fk_count,
                (SELECT count(*) FROM pg_views
                 WHERE schemaname NOT IN ('pg_catalog', 'information_schema', 'pgconsole')) as view_count,
                (SELECT count(*) FROM pg_matviews
                 WHERE schemaname NOT IN ('pg_catalog', 'information_schema', 'pgconsole')) as matview_count,
                (SELECT count(*) FROM pg_proc p
                 JOIN pg_namespace n ON n.oid = p.pronamespace
                 WHERE n.nspname NOT IN ('pg_catalog', 'information_schema', 'pgconsole')) as function_count
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                summary.setEventTriggerCount(rs.getInt("event_trigger_count"));
                summary.setEnabledTriggers(rs.getInt("enabled_triggers"));
                summary.setForeignKeyCount(rs.getInt("fk_count"));
                summary.setViewCount(rs.getInt("view_count"));
                summary.setMaterialisedViewCount(rs.getInt("matview_count"));
                summary.setFunctionCount(rs.getInt("function_count"));
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to get schema summary for %s: %s", instanceName, e.getMessage());
        }

        return summary;
    }

    // --- Model Classes ---

    /**
     * Data transfer object representing a PostgreSQL event trigger.
     * <p>
     * Event triggers are database-wide triggers that fire for DDL events. This class encapsulates
     * the trigger metadata including its name, the event type it responds to (e.g., ddl_command_start,
     * ddl_command_end, sql_drop), the associated function, enabled status, and optional command tags
     * that filter which DDL commands activate the trigger.
     *
     * @see #getEventTriggers(String)
     */
    public static class EventTrigger {
        private String name;
        private String event;
        private String owner;
        private String functionName;
        private String enabled;
        private List<String> tags;
        private String functionDef;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getEvent() { return event; }
        public void setEvent(String event) { this.event = event; }

        public String getOwner() { return owner; }
        public void setOwner(String owner) { this.owner = owner; }

        public String getFunctionName() { return functionName; }
        public void setFunctionName(String name) { this.functionName = name; }

        public String getEnabled() { return enabled; }
        public void setEnabled(String enabled) { this.enabled = enabled; }

        public boolean isEnabled() {
            return !"D".equals(enabled);
        }

        public String getEnabledDisplay() {
            if (enabled == null) return "Unknown";
            return switch (enabled) {
                case "O" -> "Origin";
                case "D" -> "Disabled";
                case "R" -> "Replica";
                case "A" -> "Always";
                default -> enabled;
            };
        }

        public String getEnabledCssClass() {
            if ("D".equals(enabled)) return "bg-secondary";
            return "bg-success";
        }

        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }

        public String getTagsDisplay() {
            if (tags == null || tags.isEmpty()) return "All commands";
            return String.join(", ", tags);
        }

        public String getFunctionDef() { return functionDef; }
        public void setFunctionDef(String def) { this.functionDef = def; }
    }

    /**
     * Data transfer object representing a dependency relationship between database objects.
     * <p>
     * Captures the relationship where one database object (the dependent) relies on another
     * database object (the referenced). Includes the type of both objects (e.g., table, view,
     * function) and the nature of the dependency (normal, automatic, internal, etc.). This
     * information is crucial for understanding the impact of schema changes.
     *
     * @see #getObjectDependencies(String, String)
     */
    public static class ObjectDependency {
        private String dependentType;
        private String dependentName;
        private String referencedType;
        private String referencedName;
        private String dependencyType;

        public String getDependentType() { return dependentType; }
        public void setDependentType(String type) { this.dependentType = type; }

        public String getDependentName() { return dependentName; }
        public void setDependentName(String name) { this.dependentName = name; }

        public String getReferencedType() { return referencedType; }
        public void setReferencedType(String type) { this.referencedType = type; }

        public String getReferencedName() { return referencedName; }
        public void setReferencedName(String name) { this.referencedName = name; }

        public String getDependencyType() { return dependencyType; }
        public void setDependencyType(String type) { this.dependencyType = type; }

        public String getDependencyTypeDisplay() {
            if (dependencyType == null) return "Unknown";
            return switch (dependencyType) {
                case "n" -> "Normal";
                case "a" -> "Auto";
                case "i" -> "Internal";
                case "e" -> "Extension";
                case "p" -> "Pin";
                default -> dependencyType;
            };
        }
    }

    /**
     * Data transfer object representing a foreign key constraint relationship between tables.
     * <p>
     * Encapsulates the complete foreign key relationship including the source table and column,
     * the referenced (target) table and column, and the cascade rules for update and delete
     * operations. This information is essential for understanding referential integrity constraints
     * and planning schema modifications that may affect related tables.
     *
     * @see #getForeignKeyRelationships(String)
     */
    public static class ForeignKeyRelationship {
        private String constraintName;
        private String fromSchema;
        private String fromTable;
        private String fromColumn;
        private String toSchema;
        private String toTable;
        private String toColumn;
        private String updateRule;
        private String deleteRule;

        public String getConstraintName() { return constraintName; }
        public void setConstraintName(String name) { this.constraintName = name; }

        public String getFromSchema() { return fromSchema; }
        public void setFromSchema(String schema) { this.fromSchema = schema; }

        public String getFromTable() { return fromTable; }
        public void setFromTable(String table) { this.fromTable = table; }

        public String getFromColumn() { return fromColumn; }
        public void setFromColumn(String column) { this.fromColumn = column; }

        public String getFromFullName() { return fromSchema + "." + fromTable; }

        public String getToSchema() { return toSchema; }
        public void setToSchema(String schema) { this.toSchema = schema; }

        public String getToTable() { return toTable; }
        public void setToTable(String table) { this.toTable = table; }

        public String getToColumn() { return toColumn; }
        public void setToColumn(String column) { this.toColumn = column; }

        public String getToFullName() { return toSchema + "." + toTable; }

        public String getUpdateRule() { return updateRule; }
        public void setUpdateRule(String rule) { this.updateRule = rule; }

        public String getDeleteRule() { return deleteRule; }
        public void setDeleteRule(String rule) { this.deleteRule = rule; }

        public boolean hasCascade() {
            return "CASCADE".equals(updateRule) || "CASCADE".equals(deleteRule);
        }
    }

    /**
     * Data transfer object representing a dependency relationship for a database view.
     * <p>
     * Identifies the base table or other object that a view depends upon. When base tables
     * are modified, dependent views may need to be updated or recreated. This class helps
     * track these relationships to facilitate impact analysis and schema migration planning.
     *
     * @see #getViewDependencies(String)
     */
    public static class ViewDependency {
        private String viewSchema;
        private String viewName;
        private String dependsOnType;
        private String dependsOnName;

        public String getViewSchema() { return viewSchema; }
        public void setViewSchema(String schema) { this.viewSchema = schema; }

        public String getViewName() { return viewName; }
        public void setViewName(String name) { this.viewName = name; }

        public String getViewFullName() { return viewSchema + "." + viewName; }

        public String getDependsOnType() { return dependsOnType; }
        public void setDependsOnType(String type) { this.dependsOnType = type; }

        public String getDependsOnName() { return dependsOnName; }
        public void setDependsOnName(String name) { this.dependsOnName = name; }
    }

    /**
     * Data transfer object representing a dependency relationship for a user-defined function or procedure.
     * <p>
     * Captures the database objects that a function depends upon, typically tables that the function
     * queries or modifies. Understanding these dependencies is important for impact analysis when
     * modifying table schemas, as function code may need to be updated to reflect schema changes.
     *
     * @see #getFunctionDependencies(String)
     */
    public static class FunctionDependency {
        private String functionName;
        private String schemaName;
        private String arguments;
        private String dependsOnType;
        private String dependsOnName;

        public String getFunctionName() { return functionName; }
        public void setFunctionName(String name) { this.functionName = name; }

        public String getSchemaName() { return schemaName; }
        public void setSchemaName(String schema) { this.schemaName = schema; }

        public String getFullName() { return schemaName + "." + functionName; }

        public String getArguments() { return arguments; }
        public void setArguments(String args) { this.arguments = args; }

        public String getDependsOnType() { return dependsOnType; }
        public void setDependsOnType(String type) { this.dependsOnType = type; }

        public String getDependsOnName() { return dependsOnName; }
        public void setDependsOnName(String name) { this.dependsOnName = name; }
    }

    /**
     * Data transfer object containing aggregate schema statistics for a database instance.
     * <p>
     * Provides summary counts of various schema objects including event triggers, foreign key
     * constraints, views, materialised views, and user-defined functions. This summary helps
     * assess the overall complexity of the database schema and identify potential areas that
     * may require attention during maintenance or migration activities.
     *
     * @see #getSummary(String)
     */
    public static class SchemaSummary {
        private int eventTriggerCount;
        private int enabledTriggers;
        private int foreignKeyCount;
        private int viewCount;
        private int materialisedViewCount;
        private int functionCount;

        public int getEventTriggerCount() { return eventTriggerCount; }
        public void setEventTriggerCount(int count) { this.eventTriggerCount = count; }

        public int getEnabledTriggers() { return enabledTriggers; }
        public void setEnabledTriggers(int count) { this.enabledTriggers = count; }

        public int getForeignKeyCount() { return foreignKeyCount; }
        public void setForeignKeyCount(int count) { this.foreignKeyCount = count; }

        public int getViewCount() { return viewCount; }
        public void setViewCount(int count) { this.viewCount = count; }

        public int getMaterialisedViewCount() { return materialisedViewCount; }
        public void setMaterialisedViewCount(int count) { this.materialisedViewCount = count; }

        public int getFunctionCount() { return functionCount; }
        public void setFunctionCount(int count) { this.functionCount = count; }

        public boolean hasEventTriggers() { return eventTriggerCount > 0; }
    }
}
