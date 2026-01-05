package com.bovinemagnet.pgconsole.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a PostgreSQL function or procedure schema definition.
 * <p>
 * Encapsulates the complete metadata for a database callable object (function, procedure,
 * aggregate, or window function), including its signature, definition, ownership, and
 * runtime characteristics. This class is primarily used for schema comparison operations
 * to detect differences between source and destination database environments.
 * <p>
 * The class provides structural equality checking through {@link #equalsStructure(FunctionSchema)}
 * which normalises definitions to ignore insignificant whitespace differences, and detailed
 * difference reporting via {@link #getDifferencesFrom(FunctionSchema)}.
 * <p>
 * Example usage:
 * <pre>{@code
 * FunctionSchema func = FunctionSchema.builder()
 *     .schemaName("public")
 *     .functionName("calculate_total")
 *     .arguments("integer, numeric")
 *     .returnType("numeric")
 *     .language("plpgsql")
 *     .volatility(Volatility.IMMUTABLE)
 *     .build();
 * }</pre>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see CallableKind
 * @see Volatility
 * @see AttributeDifference
 */
public class FunctionSchema {

    /**
     * Enumeration of PostgreSQL callable object types.
     * <p>
     * Represents the different kinds of callable database objects as stored in the
     * {@code pg_proc.prokind} system catalogue column. Each kind has an associated
     * single-character code, display name, and Bootstrap icon class for UI rendering.
     */
    public enum CallableKind {
        /** Standard SQL function that returns a value. */
        FUNCTION("f", "Function", "bi-code-slash"),

        /** SQL procedure (does not return a value). */
        PROCEDURE("p", "Procedure", "bi-gear"),

        /** Aggregate function (operates on sets of rows). */
        AGGREGATE("a", "Aggregate", "bi-calculator"),

        /** Window function (operates over a window of rows). */
        WINDOW("w", "Window", "bi-window");

        private final String code;
        private final String displayName;
        private final String iconClass;

        /**
         * Constructs a CallableKind with the specified attributes.
         *
         * @param code the single-character PostgreSQL prokind code
         * @param displayName the human-readable name for display
         * @param iconClass the Bootstrap icon class for UI rendering
         */
        CallableKind(String code, String displayName, String iconClass) {
            this.code = code;
            this.displayName = displayName;
            this.iconClass = iconClass;
        }

        /**
         * Gets the PostgreSQL system catalogue code for this kind.
         *
         * @return the single-character prokind code (f, p, a, or w)
         */
        public String getCode() {
            return code;
        }

        /**
         * Gets the human-readable display name for this kind.
         *
         * @return the display name (e.g., "Function", "Procedure")
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Gets the Bootstrap icon class for UI rendering.
         *
         * @return the icon class name (e.g., "bi-code-slash")
         */
        public String getIconClass() {
            return iconClass;
        }

        /**
         * Resolves a CallableKind from its PostgreSQL prokind code.
         * <p>
         * If the code does not match any known kind, defaults to {@link #FUNCTION}.
         *
         * @param code the single-character prokind code
         * @return the corresponding CallableKind, or FUNCTION if not recognised
         */
        public static CallableKind fromCode(String code) {
            for (CallableKind kind : values()) {
                if (kind.code.equals(code)) {
                    return kind;
                }
            }
            return FUNCTION;
        }
    }

    /**
     * Enumeration of PostgreSQL function volatility categories.
     * <p>
     * Volatility determines whether the database can optimise function calls based on
     * assumptions about the function's behaviour when given identical inputs. This
     * corresponds to the {@code pg_proc.provolatile} system catalogue column.
     * <ul>
     * <li><b>IMMUTABLE</b>: Always returns the same result for the same input (e.g., mathematical functions)</li>
     * <li><b>STABLE</b>: Returns the same result for the same input within a single statement (e.g., current_timestamp)</li>
     * <li><b>VOLATILE</b>: May return different results even with identical inputs (e.g., random(), nextval())</li>
     * </ul>
     */
    public enum Volatility {
        /** Function always returns the same result for the same input. Can be pre-evaluated by optimiser. */
        IMMUTABLE("i", "IMMUTABLE"),

        /** Function returns the same result for the same input within a single statement. */
        STABLE("s", "STABLE"),

        /** Function may return different results even with identical inputs. No optimisation allowed. */
        VOLATILE("v", "VOLATILE");

        private final String code;
        private final String displayName;

        /**
         * Constructs a Volatility with the specified attributes.
         *
         * @param code the single-character PostgreSQL provolatile code
         * @param displayName the SQL keyword for this volatility level
         */
        Volatility(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }

        /**
         * Gets the PostgreSQL system catalogue code for this volatility.
         *
         * @return the single-character provolatile code (i, s, or v)
         */
        public String getCode() {
            return code;
        }

        /**
         * Gets the SQL keyword for this volatility level.
         *
         * @return the volatility keyword (IMMUTABLE, STABLE, or VOLATILE)
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Resolves a Volatility from its PostgreSQL provolatile code.
         * <p>
         * If the code does not match any known volatility, defaults to {@link #VOLATILE}
         * as the safest (least optimisable) option.
         *
         * @param code the single-character provolatile code
         * @return the corresponding Volatility, or VOLATILE if not recognised
         */
        public static Volatility fromCode(String code) {
            for (Volatility v : values()) {
                if (v.code.equals(code)) {
                    return v;
                }
            }
            return VOLATILE;
        }
    }

    /** The schema containing this function (e.g., "public", "pg_catalog"). */
    private String schemaName;

    /** The unqualified function name (e.g., "calculate_total"). */
    private String functionName;

    /** The function's identity arguments used for overload resolution (e.g., "integer, numeric"). */
    private String arguments;

    /** The database role that owns this function. */
    private String owner;

    /** Optional user-provided comment describing the function's purpose. */
    private String comment;

    /** The complete function body/definition in the function's implementation language. */
    private String definition;

    /** The return type specification (e.g., "integer", "SETOF record", "void" for procedures). */
    private String returnType;

    /** The implementation language (e.g., "sql", "plpgsql", "c", "internal"). */
    private String language;

    /** The callable object type (function, procedure, aggregate, or window). Defaults to FUNCTION. */
    private CallableKind kind = CallableKind.FUNCTION;

    /** The volatility category affecting query optimisation. Defaults to VOLATILE. */
    private Volatility volatility = Volatility.VOLATILE;

    /** Whether the function returns NULL when any argument is NULL (RETURNS NULL ON NULL INPUT). */
    private boolean strict;

    /** Whether the function executes with the privileges of the owner rather than the caller. */
    private boolean securityDefiner;

    /** Whether the function returns a set of values rather than a single value. */
    private boolean returnsSet;

    /** The estimated execution cost in CPU units (default 100 for SQL, 1 for C). */
    private double cost;

    /** The estimated number of rows returned (only relevant when returnsSet is true). */
    private double rows;

    /** Runtime configuration parameters set for this function (e.g., "search_path=public"). */
    private List<String> configParams = new ArrayList<>();

    /**
     * Creates a builder for constructing FunctionSchema instances.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder pattern implementation for FunctionSchema.
     * <p>
     * Provides a fluent interface for constructing FunctionSchema instances with
     * optional parameters. All builder methods return the builder instance to allow
     * method chaining.
     */
    public static class Builder {
        private final FunctionSchema func = new FunctionSchema();

        /** Sets the schema name. @param schemaName the schema name. @return this builder */
        public Builder schemaName(String schemaName) { func.schemaName = schemaName; return this; }

        /** Sets the function name. @param functionName the function name. @return this builder */
        public Builder functionName(String functionName) { func.functionName = functionName; return this; }

        /** Sets the function arguments. @param arguments the identity arguments. @return this builder */
        public Builder arguments(String arguments) { func.arguments = arguments; return this; }

        /** Sets the owner. @param owner the database role that owns this function. @return this builder */
        public Builder owner(String owner) { func.owner = owner; return this; }

        /** Sets the comment. @param comment the user comment. @return this builder */
        public Builder comment(String comment) { func.comment = comment; return this; }

        /** Sets the definition. @param definition the function body. @return this builder */
        public Builder definition(String definition) { func.definition = definition; return this; }

        /** Sets the return type. @param returnType the return type specification. @return this builder */
        public Builder returnType(String returnType) { func.returnType = returnType; return this; }

        /** Sets the language. @param language the implementation language. @return this builder */
        public Builder language(String language) { func.language = language; return this; }

        /** Sets the callable kind. @param kind the callable object type. @return this builder */
        public Builder kind(CallableKind kind) { func.kind = kind; return this; }

        /** Sets the volatility. @param volatility the volatility category. @return this builder */
        public Builder volatility(Volatility volatility) { func.volatility = volatility; return this; }

        /** Sets the strict flag. @param strict whether the function is strict. @return this builder */
        public Builder strict(boolean strict) { func.strict = strict; return this; }

        /** Sets the security definer flag. @param securityDefiner whether to run with owner privileges. @return this builder */
        public Builder securityDefiner(boolean securityDefiner) { func.securityDefiner = securityDefiner; return this; }

        /**
         * Builds and returns the configured FunctionSchema instance.
         *
         * @return the constructed FunctionSchema
         */
        public FunctionSchema build() { return func; }
    }

    /**
     * Constructs an empty FunctionSchema.
     * <p>
     * All fields will be initialised to their default values. Prefer using the
     * {@link #builder()} method for more readable construction.
     */
    public FunctionSchema() {
    }

    /**
     * Constructs a FunctionSchema with the essential identification fields.
     * <p>
     * This constructor provides the minimum information needed to uniquely identify
     * a function in PostgreSQL, which uses the combination of schema, name, and
     * argument types to distinguish between overloaded functions.
     *
     * @param schemaName the schema containing this function
     * @param functionName the unqualified function name
     * @param arguments the identity arguments (parameter types only)
     */
    public FunctionSchema(String schemaName, String functionName, String arguments) {
        this.schemaName = schemaName;
        this.functionName = functionName;
        this.arguments = arguments;
    }

    /**
     * Gets the fully qualified function name.
     * <p>
     * Combines the schema and function name in the standard PostgreSQL notation.
     *
     * @return the qualified name in the format "schema.function"
     */
    public String getFullName() {
        return schemaName + "." + functionName;
    }

    /**
     * Gets the function signature with argument types.
     * <p>
     * Returns the function name followed by its argument type list in parentheses.
     * This format matches the PostgreSQL function identity notation used in
     * {@code DROP FUNCTION} and other DDL commands.
     *
     * @return the signature in the format "function(arg_types)"
     */
    public String getSignature() {
        return functionName + "(" + (arguments != null ? arguments : "") + ")";
    }

    /**
     * Gets the fully qualified function signature.
     * <p>
     * Combines the schema name, function name, and argument types into the complete
     * function identifier that uniquely identifies this function across the database.
     *
     * @return the fully qualified signature in the format "schema.function(arg_types)"
     */
    public String getFullSignature() {
        return schemaName + "." + getSignature();
    }

    /**
     * Checks whether this function is structurally equivalent to another function.
     * <p>
     * Structural equality compares the normalised function definitions (with whitespace
     * normalisation to ignore formatting differences) along with critical properties:
     * language, volatility, strictness, and security definer settings. This method is
     * used to determine if two functions are functionally identical even if minor
     * formatting or whitespace differences exist.
     * <p>
     * Note that this method does not compare schema name, function name, or arguments,
     * as it's intended to compare the implementation of functions that are already
     * known to be the same function (by identity).
     *
     * @param other the function to compare against, may be null
     * @return true if the functions are structurally equivalent, false otherwise
     */
    public boolean equalsStructure(FunctionSchema other) {
        if (other == null) return false;
        String normThis = normaliseDefinition(definition);
        String normOther = normaliseDefinition(other.definition);
        return Objects.equals(normThis, normOther)
                && Objects.equals(language, other.language)
                && volatility == other.volatility
                && strict == other.strict
                && securityDefiner == other.securityDefiner;
    }

    /**
     * Computes the detailed differences between this function and another function.
     * <p>
     * Compares this function (source) with another function (destination) and returns
     * a list of {@link AttributeDifference} objects describing each difference found.
     * The comparison includes:
     * <ul>
     * <li>Definition body (marked as breaking)</li>
     * <li>Implementation language (marked as breaking)</li>
     * <li>Volatility category (non-breaking)</li>
     * <li>Strict/NULL handling (non-breaking)</li>
     * <li>Security definer setting (non-breaking)</li>
     * </ul>
     * <p>
     * Breaking differences indicate changes that fundamentally alter the function's
     * behaviour, while non-breaking differences represent optimisation hints or
     * security settings that don't change the function's output.
     *
     * @param other the function to compare against (destination), may be null
     * @return a list of differences, empty if other is null or functions are identical
     * @see AttributeDifference
     */
    public List<AttributeDifference> getDifferencesFrom(FunctionSchema other) {
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

        if (!Objects.equals(language, other.language)) {
            diffs.add(new AttributeDifference("Language", language, other.language, true));
        }

        if (volatility != other.volatility) {
            diffs.add(new AttributeDifference("Volatility",
                    volatility.getDisplayName(),
                    other.volatility.getDisplayName(),
                    false));
        }

        if (strict != other.strict) {
            diffs.add(new AttributeDifference("Strict",
                    strict ? "YES" : "NO",
                    other.strict ? "YES" : "NO",
                    false));
        }

        if (securityDefiner != other.securityDefiner) {
            diffs.add(new AttributeDifference("Security Definer",
                    securityDefiner ? "YES" : "NO",
                    other.securityDefiner ? "YES" : "NO",
                    false));
        }

        return diffs;
    }

    /**
     * Normalises a function definition for comparison purposes.
     * <p>
     * Collapses all whitespace sequences (spaces, tabs, newlines) into single spaces
     * and trims leading/trailing whitespace. This allows structural comparison whilst
     * ignoring formatting differences such as indentation, line breaks, and extra spaces.
     * <p>
     * Note: This is a simple normalisation strategy. More sophisticated comparison might
     * use AST-based comparison or PostgreSQL's own normalisation functions.
     *
     * @param def the function definition text, may be null
     * @return the normalised definition, or null if input was null
     */
    private String normaliseDefinition(String def) {
        if (def == null) return null;
        // Normalise whitespace but preserve structure
        return def.replaceAll("\\s+", " ")
                .trim();
    }

    // Getters and Setters

    /**
     * Gets the schema name.
     *
     * @return the schema containing this function
     */
    public String getSchemaName() {
        return schemaName;
    }

    /**
     * Sets the schema name.
     *
     * @param schemaName the schema containing this function
     */
    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    /**
     * Gets the function name.
     *
     * @return the unqualified function name
     */
    public String getFunctionName() {
        return functionName;
    }

    /**
     * Sets the function name.
     *
     * @param functionName the unqualified function name
     */
    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    /**
     * Gets the function arguments.
     *
     * @return the identity arguments (parameter types)
     */
    public String getArguments() {
        return arguments;
    }

    /**
     * Sets the function arguments.
     *
     * @param arguments the identity arguments (parameter types)
     */
    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    /**
     * Gets the function owner.
     *
     * @return the database role that owns this function
     */
    public String getOwner() {
        return owner;
    }

    /**
     * Sets the function owner.
     *
     * @param owner the database role that owns this function
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * Gets the function comment.
     *
     * @return the user-provided comment, may be null
     */
    public String getComment() {
        return comment;
    }

    /**
     * Sets the function comment.
     *
     * @param comment the user-provided comment
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Gets the function definition.
     *
     * @return the complete function body/implementation
     */
    public String getDefinition() {
        return definition;
    }

    /**
     * Sets the function definition.
     *
     * @param definition the complete function body/implementation
     */
    public void setDefinition(String definition) {
        this.definition = definition;
    }

    /**
     * Gets the return type.
     *
     * @return the return type specification
     */
    public String getReturnType() {
        return returnType;
    }

    /**
     * Sets the return type.
     *
     * @param returnType the return type specification
     */
    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    /**
     * Gets the implementation language.
     *
     * @return the language name (e.g., "sql", "plpgsql")
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Sets the implementation language.
     *
     * @param language the language name (e.g., "sql", "plpgsql")
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * Gets the callable kind.
     *
     * @return the callable object type
     */
    public CallableKind getKind() {
        return kind;
    }

    /**
     * Sets the callable kind.
     *
     * @param kind the callable object type
     */
    public void setKind(CallableKind kind) {
        this.kind = kind;
    }

    /**
     * Gets the volatility category.
     *
     * @return the volatility level affecting query optimisation
     */
    public Volatility getVolatility() {
        return volatility;
    }

    /**
     * Sets the volatility category.
     *
     * @param volatility the volatility level affecting query optimisation
     */
    public void setVolatility(Volatility volatility) {
        this.volatility = volatility;
    }

    /**
     * Checks whether this function is strict.
     * <p>
     * Strict functions (RETURNS NULL ON NULL INPUT) automatically return NULL
     * if any argument is NULL, without executing the function body.
     *
     * @return true if the function is strict, false otherwise
     */
    public boolean isStrict() {
        return strict;
    }

    /**
     * Sets whether this function is strict.
     *
     * @param strict true if the function returns NULL when any argument is NULL
     */
    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    /**
     * Checks whether this function uses SECURITY DEFINER.
     * <p>
     * Security definer functions execute with the privileges of the user who
     * created the function, rather than the user who calls it. This is similar
     * to the setuid feature in Unix.
     *
     * @return true if the function executes with owner privileges, false otherwise
     */
    public boolean isSecurityDefiner() {
        return securityDefiner;
    }

    /**
     * Sets whether this function uses SECURITY DEFINER.
     *
     * @param securityDefiner true to execute with owner privileges
     */
    public void setSecurityDefiner(boolean securityDefiner) {
        this.securityDefiner = securityDefiner;
    }

    /**
     * Checks whether this function returns a set of rows.
     *
     * @return true if the function returns multiple rows (SETOF), false otherwise
     */
    public boolean isReturnsSet() {
        return returnsSet;
    }

    /**
     * Sets whether this function returns a set of rows.
     *
     * @param returnsSet true if the function returns SETOF
     */
    public void setReturnsSet(boolean returnsSet) {
        this.returnsSet = returnsSet;
    }

    /**
     * Gets the estimated execution cost.
     * <p>
     * This is a relative cost estimate in CPU units used by the query planner.
     * The default is 100 for SQL/PL/pgSQL functions and 1 for C-language functions.
     *
     * @return the estimated cost in CPU units
     */
    public double getCost() {
        return cost;
    }

    /**
     * Sets the estimated execution cost.
     *
     * @param cost the estimated cost in CPU units
     */
    public void setCost(double cost) {
        this.cost = cost;
    }

    /**
     * Gets the estimated number of rows returned.
     * <p>
     * This estimate is only relevant when {@link #isReturnsSet()} returns true.
     * It helps the query planner make better decisions about query plans.
     *
     * @return the estimated row count
     */
    public double getRows() {
        return rows;
    }

    /**
     * Sets the estimated number of rows returned.
     *
     * @param rows the estimated row count for set-returning functions
     */
    public void setRows(double rows) {
        this.rows = rows;
    }

    /**
     * Gets the runtime configuration parameters.
     * <p>
     * These are configuration settings (e.g., "search_path", "work_mem") that are
     * set locally when the function executes. The list contains strings in the
     * format "parameter=value".
     *
     * @return the list of configuration parameter settings, never null
     */
    public List<String> getConfigParams() {
        return configParams;
    }

    /**
     * Sets the runtime configuration parameters.
     *
     * @param configParams the list of configuration parameter settings
     */
    public void setConfigParams(List<String> configParams) {
        this.configParams = configParams;
    }
}
