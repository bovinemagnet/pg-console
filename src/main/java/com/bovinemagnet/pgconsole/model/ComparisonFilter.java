package com.bovinemagnet.pgconsole.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Filter configuration for PostgreSQL schema comparisons.
 * <p>
 * Provides comprehensive filtering capabilities for database object comparisons,
 * supporting inclusion/exclusion by object type, table name patterns (wildcards or regex),
 * and schema patterns. Includes predefined presets for common filtering scenarios
 * such as excluding temporary tables and system schemas.
 * <p>
 * Filtering logic follows these rules:
 * <ul>
 *   <li>Schema exclusions are evaluated first - if schema matches, object is excluded</li>
 *   <li>Table exclusions are evaluated next - if table name matches, object is excluded</li>
 *   <li>Table inclusions (if specified) act as an allowlist - only matching tables are included</li>
 *   <li>Object type filters determine which types of database objects are compared</li>
 * </ul>
 * <p>
 * Pattern matching supports two modes:
 * <ul>
 *   <li>Wildcard mode (default): Uses {@code *} for zero or more characters and {@code ?} for single character</li>
 *   <li>Regex mode: Uses full regular expression syntax via {@link Pattern}</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>{@code
 * // Create filter excluding temporary tables and system schemas
 * ComparisonFilter filter = ComparisonFilter.fromPreset(FilterPreset.PRODUCTION_SAFE);
 *
 * // Create custom filter with wildcard patterns
 * ComparisonFilter customFilter = new ComparisonFilter();
 * customFilter.addExcludeTablePattern("temp_*");
 * customFilter.addExcludeSchemaPattern("pg_catalog");
 * customFilter.setIncludeTables(true);
 * customFilter.setIncludeViews(true);
 *
 * // Test if table should be included
 * boolean include = filter.matchesTable("public", "users");
 * }</pre>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see ObjectDifference
 * @see FilterPreset
 */
public class ComparisonFilter {

    /**
     * Predefined filter presets for common database comparison scenarios.
     * <p>
     * Each preset provides a preconfigured set of table and schema patterns
     * suitable for specific use cases, from permissive (no filters) to
     * production-safe filtering that excludes temporary tables and system schemas.
     */
    public enum FilterPreset {
        /**
         * No filtering applied - includes all database objects.
         */
        NONE("No filters", List.of(), List.of()),

        /**
         * Excludes temporary and backup tables.
         * <p>
         * Patterns: {@code temp_*}, {@code tmp_*}, {@code *_backup}, {@code *_bak}, {@code zz_*}
         */
        EXCLUDE_TEMP_TABLES("Exclude temp/backup tables",
                List.of("temp_*", "tmp_*", "*_backup", "*_bak", "zz_*"),
                List.of()),

        /**
         * Excludes PostgreSQL system schemas and the pgconsole schema.
         * <p>
         * Schemas: {@code pg_catalog}, {@code information_schema}, {@code pgconsole}
         */
        EXCLUDE_SYSTEM_SCHEMAS("Exclude system schemas",
                List.of(),
                List.of("pg_catalog", "information_schema", "pgconsole")),

        /**
         * Production-safe filtering combining temporary table exclusions and system schema exclusions.
         * <p>
         * Recommended for production database comparisons to avoid noise from temporary
         * objects and system-managed schemas.
         */
        PRODUCTION_SAFE("Production-safe defaults",
                List.of("temp_*", "tmp_*", "*_backup", "*_bak", "zz_*"),
                List.of("pg_catalog", "information_schema", "pgconsole"));

        private final String displayName;
        private final List<String> tablePatterns;
        private final List<String> schemaPatterns;

        /**
         * Constructs a filter preset with specified patterns.
         *
         * @param displayName    human-readable name for UI display
         * @param tablePatterns  wildcard patterns for table exclusion
         * @param schemaPatterns wildcard patterns for schema exclusion
         */
        FilterPreset(String displayName, List<String> tablePatterns, List<String> schemaPatterns) {
            this.displayName = displayName;
            this.tablePatterns = tablePatterns;
            this.schemaPatterns = schemaPatterns;
        }

        /**
         * Returns the human-readable display name for this preset.
         *
         * @return display name suitable for UI presentation
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Returns the table exclusion patterns for this preset.
         *
         * @return immutable list of wildcard patterns for table name matching
         */
        public List<String> getTablePatterns() {
            return tablePatterns;
        }

        /**
         * Returns the schema exclusion patterns for this preset.
         *
         * @return immutable list of wildcard patterns for schema name matching
         */
        public List<String> getSchemaPatterns() {
            return schemaPatterns;
        }
    }

    /** Object types explicitly included in the comparison. */
    private Set<ObjectDifference.ObjectType> includedObjectTypes = new HashSet<>();

    /** Object types explicitly excluded from the comparison. */
    private Set<ObjectDifference.ObjectType> excludedObjectTypes = new HashSet<>();

    /** Wildcard or regex patterns for excluding tables by name. */
    private List<String> excludeTablePatterns = new ArrayList<>();

    /** Wildcard or regex patterns for excluding schemas by name. */
    private List<String> excludeSchemaPatterns = new ArrayList<>();

    /** Wildcard or regex patterns for including tables by name (acts as allowlist when specified). */
    private List<String> includeTablePatterns = new ArrayList<>();

    /** Whether to interpret patterns as regular expressions instead of wildcards. */
    private boolean useRegex = false;

    /**
     * Constructs a new filter with all object types included by default.
     * <p>
     * No exclusion patterns are set. To apply common filters, use
     * {@link #fromPreset(FilterPreset)} factory method instead.
     */
    public ComparisonFilter() {
        // Include all object types by default
        for (ObjectDifference.ObjectType type : ObjectDifference.ObjectType.values()) {
            includedObjectTypes.add(type);
        }
    }

    /**
     * Creates a filter from a predefined preset configuration.
     * <p>
     * Factory method that constructs a filter with the table and schema
     * exclusion patterns specified by the given preset. All object types
     * remain included by default.
     *
     * @param preset the preset configuration to apply
     * @return a new filter configured with the preset's exclusion patterns
     * @see FilterPreset
     */
    public static ComparisonFilter fromPreset(FilterPreset preset) {
        ComparisonFilter filter = new ComparisonFilter();
        filter.excludeTablePatterns.addAll(preset.getTablePatterns());
        filter.excludeSchemaPatterns.addAll(preset.getSchemaPatterns());
        return filter;
    }

    /**
     * Creates a filter from a comma-separated pattern string.
     * <p>
     * Parses the provided patterns string, splitting on commas and trimming whitespace.
     * Each resulting pattern is added as a table exclusion pattern. Empty patterns
     * are ignored.
     * <p>
     * Example: {@code "temp_*,tmp_*,*_backup"} creates exclusions for three patterns.
     *
     * @param patterns comma-separated exclusion patterns, or {@code null} for no patterns
     * @param useRegex {@code true} to interpret patterns as regular expressions,
     *                 {@code false} for wildcard matching
     * @return a new filter configured with the specified table exclusion patterns
     */
    public static ComparisonFilter fromPatternString(String patterns, boolean useRegex) {
        ComparisonFilter filter = new ComparisonFilter();
        filter.useRegex = useRegex;
        if (patterns != null && !patterns.isBlank()) {
            for (String pattern : patterns.split(",")) {
                String trimmed = pattern.trim();
                if (!trimmed.isEmpty()) {
                    filter.excludeTablePatterns.add(trimmed);
                }
            }
        }
        return filter;
    }

    /**
     * Determines whether a table should be included in the comparison based on filter rules.
     * <p>
     * Evaluation proceeds in the following order:
     * <ol>
     *   <li>If schema is excluded, returns {@code false}</li>
     *   <li>If table name is excluded, returns {@code false}</li>
     *   <li>If include patterns exist, returns {@code true} only if table matches at least one</li>
     *   <li>Otherwise returns {@code true}</li>
     * </ol>
     *
     * @param schemaName the schema containing the table, may be {@code null}
     * @param tableName  the table name to evaluate
     * @return {@code true} if the table passes all filter criteria and should be included,
     *         {@code false} if it should be excluded
     */
    public boolean matchesTable(String schemaName, String tableName) {
        // Check schema exclusions
        if (!excludeSchemaPatterns.isEmpty()) {
            for (String pattern : excludeSchemaPatterns) {
                if (matchesPattern(schemaName, pattern)) {
                    return false;
                }
            }
        }

        // Check table exclusions
        if (!excludeTablePatterns.isEmpty()) {
            for (String pattern : excludeTablePatterns) {
                if (matchesPattern(tableName, pattern)) {
                    return false;
                }
            }
        }

        // Check table inclusions (if specified, only include matching)
        if (!includeTablePatterns.isEmpty()) {
            for (String pattern : includeTablePatterns) {
                if (matchesPattern(tableName, pattern)) {
                    return true;
                }
            }
            return false; // If include patterns specified, must match at least one
        }

        return true;
    }

    /**
     * Determines whether a database object type should be included in the comparison.
     * <p>
     * Returns {@code false} if the type is explicitly excluded. Otherwise returns
     * {@code true} if the included types set is empty (all types allowed) or if the
     * type is explicitly included.
     *
     * @param objectType the database object type to evaluate
     * @return {@code true} if the object type should be included, {@code false} otherwise
     * @see ObjectDifference.ObjectType
     */
    public boolean matchesObjectType(ObjectDifference.ObjectType objectType) {
        if (excludedObjectTypes.contains(objectType)) {
            return false;
        }
        return includedObjectTypes.isEmpty() || includedObjectTypes.contains(objectType);
    }

    /**
     * Matches a value against a pattern using either wildcard or regex syntax.
     * <p>
     * Pattern interpretation depends on the {@link #useRegex} flag:
     * <ul>
     *   <li>When {@code false} (default): Uses wildcard syntax ({@code *} and {@code ?})</li>
     *   <li>When {@code true}: Uses full regular expression syntax</li>
     * </ul>
     * Returns {@code false} if either parameter is {@code null} or if the regex
     * pattern is syntactically invalid.
     *
     * @param value   the string value to test, may be {@code null}
     * @param pattern the pattern to match against, may be {@code null}
     * @return {@code true} if the value matches the pattern, {@code false} otherwise
     */
    private boolean matchesPattern(String value, String pattern) {
        if (value == null || pattern == null) {
            return false;
        }

        if (useRegex) {
            try {
                return Pattern.matches(pattern, value);
            } catch (PatternSyntaxException e) {
                return false;
            }
        } else {
            // Convert wildcard pattern to regex
            String regex = wildcardToRegex(pattern);
            return Pattern.matches(regex, value);
        }
    }

    /**
     * Converts a wildcard pattern to a regular expression pattern.
     * <p>
     * Supports the following wildcard syntax:
     * <ul>
     *   <li>{@code *} - Matches zero or more characters</li>
     *   <li>{@code ?} - Matches exactly one character</li>
     * </ul>
     * All regex special characters in the input are escaped to ensure literal matching.
     * The resulting pattern is anchored with {@code ^} and {@code $} to require full string match.
     *
     * @param wildcard the wildcard pattern using {@code *} and {@code ?}
     * @return a regular expression pattern string equivalent to the wildcard
     */
    private String wildcardToRegex(String wildcard) {
        StringBuilder regex = new StringBuilder("^");
        for (int i = 0; i < wildcard.length(); i++) {
            char c = wildcard.charAt(i);
            switch (c) {
                case '*' -> regex.append(".*");
                case '?' -> regex.append(".");
                case '.' -> regex.append("\\.");
                case '[' -> regex.append("\\[");
                case ']' -> regex.append("\\]");
                case '(' -> regex.append("\\(");
                case ')' -> regex.append("\\)");
                case '^' -> regex.append("\\^");
                case '$' -> regex.append("\\$");
                case '|' -> regex.append("\\|");
                case '+' -> regex.append("\\+");
                case '\\' -> regex.append("\\\\");
                default -> regex.append(c);
            }
        }
        regex.append("$");
        return regex.toString();
    }

    /**
     * Returns a human-readable summary of the active filter criteria.
     * <p>
     * The summary includes information about:
     * <ul>
     *   <li>Excluded table patterns (if any)</li>
     *   <li>Excluded schema patterns (if any)</li>
     *   <li>Count of excluded object types (if any)</li>
     * </ul>
     * Returns "No filters applied" when no filtering criteria are active.
     *
     * @return a semicolon-separated summary string describing the active filters
     */
    public String getSummary() {
        List<String> parts = new ArrayList<>();

        if (!excludeTablePatterns.isEmpty()) {
            parts.add("Excluding: " + String.join(", ", excludeTablePatterns));
        }

        if (!excludeSchemaPatterns.isEmpty()) {
            parts.add("Excluding schemas: " + String.join(", ", excludeSchemaPatterns));
        }

        if (!excludedObjectTypes.isEmpty()) {
            parts.add("Excluding types: " + excludedObjectTypes.size());
        }

        if (parts.isEmpty()) {
            return "No filters applied";
        }

        return String.join("; ", parts);
    }

    /**
     * Determines whether this filter has any active filtering criteria.
     * <p>
     * Returns {@code true} if any of the following are non-empty:
     * <ul>
     *   <li>Table exclusion patterns</li>
     *   <li>Schema exclusion patterns</li>
     *   <li>Excluded object types</li>
     *   <li>Table inclusion patterns (allowlist)</li>
     * </ul>
     *
     * @return {@code true} if any filters are active, {@code false} if this is an unfiltered comparison
     */
    public boolean hasFilters() {
        return !excludeTablePatterns.isEmpty()
                || !excludeSchemaPatterns.isEmpty()
                || !excludedObjectTypes.isEmpty()
                || !includeTablePatterns.isEmpty();
    }

    // Getters and Setters

    /**
     * Returns the set of object types explicitly included in the comparison.
     *
     * @return mutable set of included object types
     */
    public Set<ObjectDifference.ObjectType> getIncludedObjectTypes() {
        return includedObjectTypes;
    }

    /**
     * Sets the object types to be included in the comparison.
     *
     * @param includedObjectTypes the set of object types to include
     */
    public void setIncludedObjectTypes(Set<ObjectDifference.ObjectType> includedObjectTypes) {
        this.includedObjectTypes = includedObjectTypes;
    }

    /**
     * Returns the set of object types explicitly excluded from the comparison.
     *
     * @return mutable set of excluded object types
     */
    public Set<ObjectDifference.ObjectType> getExcludedObjectTypes() {
        return excludedObjectTypes;
    }

    /**
     * Sets the object types to be excluded from the comparison.
     *
     * @param excludedObjectTypes the set of object types to exclude
     */
    public void setExcludedObjectTypes(Set<ObjectDifference.ObjectType> excludedObjectTypes) {
        this.excludedObjectTypes = excludedObjectTypes;
    }

    /**
     * Returns the list of table name exclusion patterns.
     *
     * @return mutable list of table exclusion patterns
     */
    public List<String> getExcludeTablePatterns() {
        return excludeTablePatterns;
    }

    /**
     * Sets the patterns for excluding tables by name.
     *
     * @param excludeTablePatterns list of wildcard or regex patterns
     */
    public void setExcludeTablePatterns(List<String> excludeTablePatterns) {
        this.excludeTablePatterns = excludeTablePatterns;
    }

    /**
     * Returns the list of schema name exclusion patterns.
     *
     * @return mutable list of schema exclusion patterns
     */
    public List<String> getExcludeSchemaPatterns() {
        return excludeSchemaPatterns;
    }

    /**
     * Sets the patterns for excluding schemas by name.
     *
     * @param excludeSchemaPatterns list of wildcard or regex patterns
     */
    public void setExcludeSchemaPatterns(List<String> excludeSchemaPatterns) {
        this.excludeSchemaPatterns = excludeSchemaPatterns;
    }

    /**
     * Returns the list of table name inclusion patterns (allowlist).
     *
     * @return mutable list of table inclusion patterns
     */
    public List<String> getIncludeTablePatterns() {
        return includeTablePatterns;
    }

    /**
     * Sets the patterns for including tables by name (allowlist mode).
     * <p>
     * When non-empty, only tables matching at least one pattern are included.
     *
     * @param includeTablePatterns list of wildcard or regex patterns
     */
    public void setIncludeTablePatterns(List<String> includeTablePatterns) {
        this.includeTablePatterns = includeTablePatterns;
    }

    /**
     * Returns whether patterns are interpreted as regular expressions.
     *
     * @return {@code true} if using regex mode, {@code false} if using wildcard mode
     */
    public boolean isUseRegex() {
        return useRegex;
    }

    /**
     * Sets whether patterns should be interpreted as regular expressions.
     *
     * @param useRegex {@code true} for regex mode, {@code false} for wildcard mode
     */
    public void setUseRegex(boolean useRegex) {
        this.useRegex = useRegex;
    }

    /**
     * Adds a pattern to the table exclusion list.
     *
     * @param pattern wildcard or regex pattern to exclude tables by name
     */
    public void addExcludeTablePattern(String pattern) {
        this.excludeTablePatterns.add(pattern);
    }

    /**
     * Adds a pattern to the schema exclusion list.
     *
     * @param pattern wildcard or regex pattern to exclude schemas by name
     */
    public void addExcludeSchemaPattern(String pattern) {
        this.excludeSchemaPatterns.add(pattern);
    }

    /**
     * Adds a pattern to the table inclusion list (allowlist).
     *
     * @param pattern wildcard or regex pattern to include tables by name
     */
    public void addIncludeTablePattern(String pattern) {
        this.includeTablePatterns.add(pattern);
    }

    /**
     * Excludes a specific object type from the comparison.
     * <p>
     * Removes the type from the included set and adds it to the excluded set.
     *
     * @param type the object type to exclude
     */
    public void excludeObjectType(ObjectDifference.ObjectType type) {
        this.excludedObjectTypes.add(type);
        this.includedObjectTypes.remove(type);
    }

    /**
     * Includes a specific object type in the comparison.
     * <p>
     * Removes the type from the excluded set and adds it to the included set.
     *
     * @param type the object type to include
     */
    public void includeObjectType(ObjectDifference.ObjectType type) {
        this.includedObjectTypes.add(type);
        this.excludedObjectTypes.remove(type);
    }

    // Convenience methods for include/exclude by object type category

    /**
     * Returns the table exclusion patterns.
     * <p>
     * Alias for {@link #getExcludeTablePatterns()} for backward compatibility.
     *
     * @return mutable list of table exclusion patterns
     * @see #getExcludeTablePatterns()
     */
    public List<String> getExcludePatterns() {
        return excludeTablePatterns;
    }

    /**
     * Determines whether a table should be included based on its name only.
     * <p>
     * Simplified version of {@link #matchesTable(String, String)} that evaluates
     * only table name patterns without considering schema patterns.
     *
     * @param tableName the table name to evaluate
     * @return {@code true} if the table passes filter criteria, {@code false} otherwise
     * @see #matchesTable(String, String)
     */
    public boolean matchesTable(String tableName) {
        return matchesTable(null, tableName);
    }

    /**
     * Checks whether tables are included in the comparison.
     *
     * @return {@code true} if TABLE object type is included
     */
    public boolean isIncludeTables() {
        return includedObjectTypes.contains(ObjectDifference.ObjectType.TABLE);
    }

    /**
     * Sets whether tables should be included in the comparison.
     *
     * @param include {@code true} to include tables, {@code false} to exclude
     */
    public void setIncludeTables(boolean include) {
        if (include) {
            includeObjectType(ObjectDifference.ObjectType.TABLE);
        } else {
            excludeObjectType(ObjectDifference.ObjectType.TABLE);
        }
    }

    /**
     * Checks whether views are included in the comparison.
     * <p>
     * Returns {@code true} if either VIEW or MATERIALIZED_VIEW types are included.
     *
     * @return {@code true} if view object types are included
     */
    public boolean isIncludeViews() {
        return includedObjectTypes.contains(ObjectDifference.ObjectType.VIEW)
                || includedObjectTypes.contains(ObjectDifference.ObjectType.MATERIALIZED_VIEW);
    }

    /**
     * Sets whether views should be included in the comparison.
     * <p>
     * Applies to both regular views and materialised views.
     *
     * @param include {@code true} to include views, {@code false} to exclude
     */
    public void setIncludeViews(boolean include) {
        if (include) {
            includeObjectType(ObjectDifference.ObjectType.VIEW);
            includeObjectType(ObjectDifference.ObjectType.MATERIALIZED_VIEW);
        } else {
            excludeObjectType(ObjectDifference.ObjectType.VIEW);
            excludeObjectType(ObjectDifference.ObjectType.MATERIALIZED_VIEW);
        }
    }

    /**
     * Checks whether functions are included in the comparison.
     * <p>
     * Returns {@code true} if either FUNCTION or PROCEDURE types are included.
     *
     * @return {@code true} if function object types are included
     */
    public boolean isIncludeFunctions() {
        return includedObjectTypes.contains(ObjectDifference.ObjectType.FUNCTION)
                || includedObjectTypes.contains(ObjectDifference.ObjectType.PROCEDURE);
    }

    /**
     * Sets whether functions should be included in the comparison.
     * <p>
     * Applies to both functions and stored procedures.
     *
     * @param include {@code true} to include functions, {@code false} to exclude
     */
    public void setIncludeFunctions(boolean include) {
        if (include) {
            includeObjectType(ObjectDifference.ObjectType.FUNCTION);
            includeObjectType(ObjectDifference.ObjectType.PROCEDURE);
        } else {
            excludeObjectType(ObjectDifference.ObjectType.FUNCTION);
            excludeObjectType(ObjectDifference.ObjectType.PROCEDURE);
        }
    }

    /**
     * Checks whether sequences are included in the comparison.
     *
     * @return {@code true} if SEQUENCE object type is included
     */
    public boolean isIncludeSequences() {
        return includedObjectTypes.contains(ObjectDifference.ObjectType.SEQUENCE);
    }

    /**
     * Sets whether sequences should be included in the comparison.
     *
     * @param include {@code true} to include sequences, {@code false} to exclude
     */
    public void setIncludeSequences(boolean include) {
        if (include) {
            includeObjectType(ObjectDifference.ObjectType.SEQUENCE);
        } else {
            excludeObjectType(ObjectDifference.ObjectType.SEQUENCE);
        }
    }

    /**
     * Checks whether custom types are included in the comparison.
     * <p>
     * Returns {@code true} if any of TYPE_ENUM, TYPE_COMPOSITE, or TYPE_DOMAIN are included.
     *
     * @return {@code true} if type object types are included
     */
    public boolean isIncludeTypes() {
        return includedObjectTypes.contains(ObjectDifference.ObjectType.TYPE_ENUM)
                || includedObjectTypes.contains(ObjectDifference.ObjectType.TYPE_COMPOSITE)
                || includedObjectTypes.contains(ObjectDifference.ObjectType.TYPE_DOMAIN);
    }

    /**
     * Sets whether custom types should be included in the comparison.
     * <p>
     * Applies to enum types, composite types, and domain types.
     *
     * @param include {@code true} to include types, {@code false} to exclude
     */
    public void setIncludeTypes(boolean include) {
        if (include) {
            includeObjectType(ObjectDifference.ObjectType.TYPE_ENUM);
            includeObjectType(ObjectDifference.ObjectType.TYPE_COMPOSITE);
            includeObjectType(ObjectDifference.ObjectType.TYPE_DOMAIN);
        } else {
            excludeObjectType(ObjectDifference.ObjectType.TYPE_ENUM);
            excludeObjectType(ObjectDifference.ObjectType.TYPE_COMPOSITE);
            excludeObjectType(ObjectDifference.ObjectType.TYPE_DOMAIN);
        }
    }

    /**
     * Checks whether extensions are included in the comparison.
     *
     * @return {@code true} if EXTENSION object type is included
     */
    public boolean isIncludeExtensions() {
        return includedObjectTypes.contains(ObjectDifference.ObjectType.EXTENSION);
    }

    /**
     * Sets whether extensions should be included in the comparison.
     *
     * @param include {@code true} to include extensions, {@code false} to exclude
     */
    public void setIncludeExtensions(boolean include) {
        if (include) {
            includeObjectType(ObjectDifference.ObjectType.EXTENSION);
        } else {
            excludeObjectType(ObjectDifference.ObjectType.EXTENSION);
        }
    }

    /**
     * Checks whether indexes are included in the comparison.
     *
     * @return {@code true} if INDEX object type is included
     */
    public boolean isIncludeIndexes() {
        return includedObjectTypes.contains(ObjectDifference.ObjectType.INDEX);
    }

    /**
     * Sets whether indexes should be included in the comparison.
     *
     * @param include {@code true} to include indexes, {@code false} to exclude
     */
    public void setIncludeIndexes(boolean include) {
        if (include) {
            includeObjectType(ObjectDifference.ObjectType.INDEX);
        } else {
            excludeObjectType(ObjectDifference.ObjectType.INDEX);
        }
    }

    /**
     * Checks whether triggers are included in the comparison.
     *
     * @return {@code true} if TRIGGER object type is included
     */
    public boolean isIncludeTriggers() {
        return includedObjectTypes.contains(ObjectDifference.ObjectType.TRIGGER);
    }

    /**
     * Sets whether triggers should be included in the comparison.
     *
     * @param include {@code true} to include triggers, {@code false} to exclude
     */
    public void setIncludeTriggers(boolean include) {
        if (include) {
            includeObjectType(ObjectDifference.ObjectType.TRIGGER);
        } else {
            excludeObjectType(ObjectDifference.ObjectType.TRIGGER);
        }
    }

    /**
     * Checks whether columns are included in the comparison.
     *
     * @return {@code true} if COLUMN object type is included
     */
    public boolean isIncludeColumns() {
        return includedObjectTypes.contains(ObjectDifference.ObjectType.COLUMN);
    }

    /**
     * Sets whether columns should be included in the comparison.
     *
     * @param include {@code true} to include columns, {@code false} to exclude
     */
    public void setIncludeColumns(boolean include) {
        if (include) {
            includeObjectType(ObjectDifference.ObjectType.COLUMN);
        } else {
            excludeObjectType(ObjectDifference.ObjectType.COLUMN);
        }
    }

    /**
     * Checks whether primary key constraints are included in the comparison.
     *
     * @return {@code true} if CONSTRAINT_PRIMARY object type is included
     */
    public boolean isIncludePrimaryKeys() {
        return includedObjectTypes.contains(ObjectDifference.ObjectType.CONSTRAINT_PRIMARY);
    }

    /**
     * Sets whether primary key constraints should be included in the comparison.
     *
     * @param include {@code true} to include primary keys, {@code false} to exclude
     */
    public void setIncludePrimaryKeys(boolean include) {
        if (include) {
            includeObjectType(ObjectDifference.ObjectType.CONSTRAINT_PRIMARY);
        } else {
            excludeObjectType(ObjectDifference.ObjectType.CONSTRAINT_PRIMARY);
        }
    }

    /**
     * Checks whether foreign key constraints are included in the comparison.
     *
     * @return {@code true} if CONSTRAINT_FOREIGN object type is included
     */
    public boolean isIncludeForeignKeys() {
        return includedObjectTypes.contains(ObjectDifference.ObjectType.CONSTRAINT_FOREIGN);
    }

    /**
     * Sets whether foreign key constraints should be included in the comparison.
     *
     * @param include {@code true} to include foreign keys, {@code false} to exclude
     */
    public void setIncludeForeignKeys(boolean include) {
        if (include) {
            includeObjectType(ObjectDifference.ObjectType.CONSTRAINT_FOREIGN);
        } else {
            excludeObjectType(ObjectDifference.ObjectType.CONSTRAINT_FOREIGN);
        }
    }

    /**
     * Checks whether unique constraints are included in the comparison.
     *
     * @return {@code true} if CONSTRAINT_UNIQUE object type is included
     */
    public boolean isIncludeUniqueConstraints() {
        return includedObjectTypes.contains(ObjectDifference.ObjectType.CONSTRAINT_UNIQUE);
    }

    /**
     * Sets whether unique constraints should be included in the comparison.
     *
     * @param include {@code true} to include unique constraints, {@code false} to exclude
     */
    public void setIncludeUniqueConstraints(boolean include) {
        if (include) {
            includeObjectType(ObjectDifference.ObjectType.CONSTRAINT_UNIQUE);
        } else {
            excludeObjectType(ObjectDifference.ObjectType.CONSTRAINT_UNIQUE);
        }
    }

    /**
     * Checks whether check constraints are included in the comparison.
     *
     * @return {@code true} if CONSTRAINT_CHECK object type is included
     */
    public boolean isIncludeCheckConstraints() {
        return includedObjectTypes.contains(ObjectDifference.ObjectType.CONSTRAINT_CHECK);
    }

    /**
     * Sets whether check constraints should be included in the comparison.
     *
     * @param include {@code true} to include check constraints, {@code false} to exclude
     */
    public void setIncludeCheckConstraints(boolean include) {
        if (include) {
            includeObjectType(ObjectDifference.ObjectType.CONSTRAINT_CHECK);
        } else {
            excludeObjectType(ObjectDifference.ObjectType.CONSTRAINT_CHECK);
        }
    }
}
