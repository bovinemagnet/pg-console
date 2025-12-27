package com.bovinemagnet.pgconsole.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Filter configuration for schema comparisons.
 * <p>
 * Supports filtering by object type, name patterns (wildcards or regex),
 * and schema exclusions. Includes preset configurations for common use cases.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class ComparisonFilter {

    /**
     * Predefined filter presets for common scenarios.
     */
    public enum FilterPreset {
        NONE("No filters", List.of(), List.of()),
        EXCLUDE_TEMP_TABLES("Exclude temp/backup tables",
                List.of("temp_*", "tmp_*", "*_backup", "*_bak", "zz_*"),
                List.of()),
        EXCLUDE_SYSTEM_SCHEMAS("Exclude system schemas",
                List.of(),
                List.of("pg_catalog", "information_schema", "pgconsole")),
        PRODUCTION_SAFE("Production-safe defaults",
                List.of("temp_*", "tmp_*", "*_backup", "*_bak", "zz_*"),
                List.of("pg_catalog", "information_schema", "pgconsole"));

        private final String displayName;
        private final List<String> tablePatterns;
        private final List<String> schemaPatterns;

        FilterPreset(String displayName, List<String> tablePatterns, List<String> schemaPatterns) {
            this.displayName = displayName;
            this.tablePatterns = tablePatterns;
            this.schemaPatterns = schemaPatterns;
        }

        public String getDisplayName() {
            return displayName;
        }

        public List<String> getTablePatterns() {
            return tablePatterns;
        }

        public List<String> getSchemaPatterns() {
            return schemaPatterns;
        }
    }

    private Set<ObjectDifference.ObjectType> includedObjectTypes = new HashSet<>();
    private Set<ObjectDifference.ObjectType> excludedObjectTypes = new HashSet<>();
    private List<String> excludeTablePatterns = new ArrayList<>();
    private List<String> excludeSchemaPatterns = new ArrayList<>();
    private List<String> includeTablePatterns = new ArrayList<>();
    private boolean useRegex = false;

    public ComparisonFilter() {
        // Include all object types by default
        for (ObjectDifference.ObjectType type : ObjectDifference.ObjectType.values()) {
            includedObjectTypes.add(type);
        }
    }

    /**
     * Creates a filter from a preset.
     *
     * @param preset the preset to apply
     * @return configured filter
     */
    public static ComparisonFilter fromPreset(FilterPreset preset) {
        ComparisonFilter filter = new ComparisonFilter();
        filter.excludeTablePatterns.addAll(preset.getTablePatterns());
        filter.excludeSchemaPatterns.addAll(preset.getSchemaPatterns());
        return filter;
    }

    /**
     * Creates a filter from a comma-separated pattern string.
     *
     * @param patterns comma-separated patterns
     * @param useRegex whether to interpret as regex
     * @return configured filter
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
     * Checks if a table should be included based on filter patterns.
     *
     * @param schemaName schema name
     * @param tableName  table name
     * @return true if table should be included
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
     * Checks if an object type should be included.
     *
     * @param objectType the object type
     * @return true if object type should be included
     */
    public boolean matchesObjectType(ObjectDifference.ObjectType objectType) {
        if (excludedObjectTypes.contains(objectType)) {
            return false;
        }
        return includedObjectTypes.isEmpty() || includedObjectTypes.contains(objectType);
    }

    /**
     * Matches a value against a pattern (wildcard or regex).
     *
     * @param value   value to test
     * @param pattern pattern to match
     * @return true if matches
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
     * Converts a wildcard pattern to a regex pattern.
     *
     * @param wildcard wildcard pattern (using * and ?)
     * @return regex pattern
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
     * Gets a display-friendly summary of the filter.
     *
     * @return summary text
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
     * Checks if this filter has any active exclusions.
     *
     * @return true if any filters are active
     */
    public boolean hasFilters() {
        return !excludeTablePatterns.isEmpty()
                || !excludeSchemaPatterns.isEmpty()
                || !excludedObjectTypes.isEmpty()
                || !includeTablePatterns.isEmpty();
    }

    // Getters and Setters

    public Set<ObjectDifference.ObjectType> getIncludedObjectTypes() {
        return includedObjectTypes;
    }

    public void setIncludedObjectTypes(Set<ObjectDifference.ObjectType> includedObjectTypes) {
        this.includedObjectTypes = includedObjectTypes;
    }

    public Set<ObjectDifference.ObjectType> getExcludedObjectTypes() {
        return excludedObjectTypes;
    }

    public void setExcludedObjectTypes(Set<ObjectDifference.ObjectType> excludedObjectTypes) {
        this.excludedObjectTypes = excludedObjectTypes;
    }

    public List<String> getExcludeTablePatterns() {
        return excludeTablePatterns;
    }

    public void setExcludeTablePatterns(List<String> excludeTablePatterns) {
        this.excludeTablePatterns = excludeTablePatterns;
    }

    public List<String> getExcludeSchemaPatterns() {
        return excludeSchemaPatterns;
    }

    public void setExcludeSchemaPatterns(List<String> excludeSchemaPatterns) {
        this.excludeSchemaPatterns = excludeSchemaPatterns;
    }

    public List<String> getIncludeTablePatterns() {
        return includeTablePatterns;
    }

    public void setIncludeTablePatterns(List<String> includeTablePatterns) {
        this.includeTablePatterns = includeTablePatterns;
    }

    public boolean isUseRegex() {
        return useRegex;
    }

    public void setUseRegex(boolean useRegex) {
        this.useRegex = useRegex;
    }

    public void addExcludeTablePattern(String pattern) {
        this.excludeTablePatterns.add(pattern);
    }

    public void addExcludeSchemaPattern(String pattern) {
        this.excludeSchemaPatterns.add(pattern);
    }

    public void addIncludeTablePattern(String pattern) {
        this.includeTablePatterns.add(pattern);
    }

    public void excludeObjectType(ObjectDifference.ObjectType type) {
        this.excludedObjectTypes.add(type);
        this.includedObjectTypes.remove(type);
    }

    public void includeObjectType(ObjectDifference.ObjectType type) {
        this.includedObjectTypes.add(type);
        this.excludedObjectTypes.remove(type);
    }

    // Convenience methods for include/exclude by object type category

    /**
     * Gets exclude patterns (alias for excludeTablePatterns).
     *
     * @return exclude patterns list
     */
    public List<String> getExcludePatterns() {
        return excludeTablePatterns;
    }

    /**
     * Checks if table just by name (simplified version).
     *
     * @param tableName table name
     * @return true if table should be included
     */
    public boolean matchesTable(String tableName) {
        return matchesTable(null, tableName);
    }

    public boolean isIncludeTables() {
        return includedObjectTypes.contains(ObjectDifference.ObjectType.TABLE);
    }

    public void setIncludeTables(boolean include) {
        if (include) {
            includeObjectType(ObjectDifference.ObjectType.TABLE);
        } else {
            excludeObjectType(ObjectDifference.ObjectType.TABLE);
        }
    }

    public boolean isIncludeViews() {
        return includedObjectTypes.contains(ObjectDifference.ObjectType.VIEW)
                || includedObjectTypes.contains(ObjectDifference.ObjectType.MATERIALIZED_VIEW);
    }

    public void setIncludeViews(boolean include) {
        if (include) {
            includeObjectType(ObjectDifference.ObjectType.VIEW);
            includeObjectType(ObjectDifference.ObjectType.MATERIALIZED_VIEW);
        } else {
            excludeObjectType(ObjectDifference.ObjectType.VIEW);
            excludeObjectType(ObjectDifference.ObjectType.MATERIALIZED_VIEW);
        }
    }

    public boolean isIncludeFunctions() {
        return includedObjectTypes.contains(ObjectDifference.ObjectType.FUNCTION)
                || includedObjectTypes.contains(ObjectDifference.ObjectType.PROCEDURE);
    }

    public void setIncludeFunctions(boolean include) {
        if (include) {
            includeObjectType(ObjectDifference.ObjectType.FUNCTION);
            includeObjectType(ObjectDifference.ObjectType.PROCEDURE);
        } else {
            excludeObjectType(ObjectDifference.ObjectType.FUNCTION);
            excludeObjectType(ObjectDifference.ObjectType.PROCEDURE);
        }
    }

    public boolean isIncludeSequences() {
        return includedObjectTypes.contains(ObjectDifference.ObjectType.SEQUENCE);
    }

    public void setIncludeSequences(boolean include) {
        if (include) {
            includeObjectType(ObjectDifference.ObjectType.SEQUENCE);
        } else {
            excludeObjectType(ObjectDifference.ObjectType.SEQUENCE);
        }
    }

    public boolean isIncludeTypes() {
        return includedObjectTypes.contains(ObjectDifference.ObjectType.TYPE_ENUM)
                || includedObjectTypes.contains(ObjectDifference.ObjectType.TYPE_COMPOSITE)
                || includedObjectTypes.contains(ObjectDifference.ObjectType.TYPE_DOMAIN);
    }

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

    public boolean isIncludeExtensions() {
        return includedObjectTypes.contains(ObjectDifference.ObjectType.EXTENSION);
    }

    public void setIncludeExtensions(boolean include) {
        if (include) {
            includeObjectType(ObjectDifference.ObjectType.EXTENSION);
        } else {
            excludeObjectType(ObjectDifference.ObjectType.EXTENSION);
        }
    }

    public boolean isIncludeIndexes() {
        return includedObjectTypes.contains(ObjectDifference.ObjectType.INDEX);
    }

    public void setIncludeIndexes(boolean include) {
        if (include) {
            includeObjectType(ObjectDifference.ObjectType.INDEX);
        } else {
            excludeObjectType(ObjectDifference.ObjectType.INDEX);
        }
    }

    public boolean isIncludeTriggers() {
        return includedObjectTypes.contains(ObjectDifference.ObjectType.TRIGGER);
    }

    public void setIncludeTriggers(boolean include) {
        if (include) {
            includeObjectType(ObjectDifference.ObjectType.TRIGGER);
        } else {
            excludeObjectType(ObjectDifference.ObjectType.TRIGGER);
        }
    }

    public boolean isIncludeColumns() {
        return includedObjectTypes.contains(ObjectDifference.ObjectType.COLUMN);
    }

    public void setIncludeColumns(boolean include) {
        if (include) {
            includeObjectType(ObjectDifference.ObjectType.COLUMN);
        } else {
            excludeObjectType(ObjectDifference.ObjectType.COLUMN);
        }
    }

    public boolean isIncludePrimaryKeys() {
        return includedObjectTypes.contains(ObjectDifference.ObjectType.CONSTRAINT_PRIMARY);
    }

    public void setIncludePrimaryKeys(boolean include) {
        if (include) {
            includeObjectType(ObjectDifference.ObjectType.CONSTRAINT_PRIMARY);
        } else {
            excludeObjectType(ObjectDifference.ObjectType.CONSTRAINT_PRIMARY);
        }
    }

    public boolean isIncludeForeignKeys() {
        return includedObjectTypes.contains(ObjectDifference.ObjectType.CONSTRAINT_FOREIGN);
    }

    public void setIncludeForeignKeys(boolean include) {
        if (include) {
            includeObjectType(ObjectDifference.ObjectType.CONSTRAINT_FOREIGN);
        } else {
            excludeObjectType(ObjectDifference.ObjectType.CONSTRAINT_FOREIGN);
        }
    }

    public boolean isIncludeUniqueConstraints() {
        return includedObjectTypes.contains(ObjectDifference.ObjectType.CONSTRAINT_UNIQUE);
    }

    public void setIncludeUniqueConstraints(boolean include) {
        if (include) {
            includeObjectType(ObjectDifference.ObjectType.CONSTRAINT_UNIQUE);
        } else {
            excludeObjectType(ObjectDifference.ObjectType.CONSTRAINT_UNIQUE);
        }
    }

    public boolean isIncludeCheckConstraints() {
        return includedObjectTypes.contains(ObjectDifference.ObjectType.CONSTRAINT_CHECK);
    }

    public void setIncludeCheckConstraints(boolean include) {
        if (include) {
            includeObjectType(ObjectDifference.ObjectType.CONSTRAINT_CHECK);
        } else {
            excludeObjectType(ObjectDifference.ObjectType.CONSTRAINT_CHECK);
        }
    }
}
