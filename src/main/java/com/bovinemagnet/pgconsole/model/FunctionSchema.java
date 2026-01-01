package com.bovinemagnet.pgconsole.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a function or procedure schema definition.
 * <p>
 * Contains the full function definition including signature, body,
 * and metadata for schema comparison.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class FunctionSchema {

    /**
     * Type of callable (function, procedure, aggregate, window).
     */
    public enum CallableKind {
        FUNCTION("f", "Function", "bi-code-slash"),
        PROCEDURE("p", "Procedure", "bi-gear"),
        AGGREGATE("a", "Aggregate", "bi-calculator"),
        WINDOW("w", "Window", "bi-window");

        private final String code;
        private final String displayName;
        private final String iconClass;

        CallableKind(String code, String displayName, String iconClass) {
            this.code = code;
            this.displayName = displayName;
            this.iconClass = iconClass;
        }

        public String getCode() {
            return code;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getIconClass() {
            return iconClass;
        }

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
     * Volatility category.
     */
    public enum Volatility {
        IMMUTABLE("i", "IMMUTABLE"),
        STABLE("s", "STABLE"),
        VOLATILE("v", "VOLATILE");

        private final String code;
        private final String displayName;

        Volatility(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }

        public String getCode() {
            return code;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static Volatility fromCode(String code) {
            for (Volatility v : values()) {
                if (v.code.equals(code)) {
                    return v;
                }
            }
            return VOLATILE;
        }
    }

    private String schemaName;
    private String functionName;
    private String arguments; // Identity arguments (for overloading)
    private String owner;
    private String comment;
    private String definition;
    private String returnType;
    private String language;
    private CallableKind kind = CallableKind.FUNCTION;
    private Volatility volatility = Volatility.VOLATILE;
    private boolean strict;
    private boolean securityDefiner;
    private boolean returnsSet;
    private double cost;
    private double rows;
    private List<String> configParams = new ArrayList<>();

    /**
     * Creates a builder for FunctionSchema.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for FunctionSchema.
     */
    public static class Builder {
        private final FunctionSchema func = new FunctionSchema();

        public Builder schemaName(String schemaName) { func.schemaName = schemaName; return this; }
        public Builder functionName(String functionName) { func.functionName = functionName; return this; }
        public Builder arguments(String arguments) { func.arguments = arguments; return this; }
        public Builder owner(String owner) { func.owner = owner; return this; }
        public Builder comment(String comment) { func.comment = comment; return this; }
        public Builder definition(String definition) { func.definition = definition; return this; }
        public Builder returnType(String returnType) { func.returnType = returnType; return this; }
        public Builder language(String language) { func.language = language; return this; }
        public Builder kind(CallableKind kind) { func.kind = kind; return this; }
        public Builder volatility(Volatility volatility) { func.volatility = volatility; return this; }
        public Builder strict(boolean strict) { func.strict = strict; return this; }
        public Builder securityDefiner(boolean securityDefiner) { func.securityDefiner = securityDefiner; return this; }
        public FunctionSchema build() { return func; }
    }

    public FunctionSchema() {
    }

    public FunctionSchema(String schemaName, String functionName, String arguments) {
        this.schemaName = schemaName;
        this.functionName = functionName;
        this.arguments = arguments;
    }

    /**
     * Gets the fully qualified function name.
     *
     * @return schema.function name
     */
    public String getFullName() {
        return schemaName + "." + functionName;
    }

    /**
     * Gets the function signature (name with arguments).
     *
     * @return function signature
     */
    public String getSignature() {
        return functionName + "(" + (arguments != null ? arguments : "") + ")";
    }

    /**
     * Gets the fully qualified signature.
     *
     * @return schema.function(args)
     */
    public String getFullSignature() {
        return schemaName + "." + getSignature();
    }

    /**
     * Checks if this function equals another for comparison purposes.
     *
     * @param other other function
     * @return true if structurally equal
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
     * Gets differences from another function.
     *
     * @param other other function
     * @return list of differences
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
     * Normalises a function definition for comparison.
     *
     * @param def definition text
     * @return normalised definition
     */
    private String normaliseDefinition(String def) {
        if (def == null) return null;
        // Normalise whitespace but preserve structure
        return def.replaceAll("\\s+", " ")
                .trim();
    }

    // Getters and Setters

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public CallableKind getKind() {
        return kind;
    }

    public void setKind(CallableKind kind) {
        this.kind = kind;
    }

    public Volatility getVolatility() {
        return volatility;
    }

    public void setVolatility(Volatility volatility) {
        this.volatility = volatility;
    }

    public boolean isStrict() {
        return strict;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    public boolean isSecurityDefiner() {
        return securityDefiner;
    }

    public void setSecurityDefiner(boolean securityDefiner) {
        this.securityDefiner = securityDefiner;
    }

    public boolean isReturnsSet() {
        return returnsSet;
    }

    public void setReturnsSet(boolean returnsSet) {
        this.returnsSet = returnsSet;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public double getRows() {
        return rows;
    }

    public void setRows(double rows) {
        this.rows = rows;
    }

    public List<String> getConfigParams() {
        return configParams;
    }

    public void setConfigParams(List<String> configParams) {
        this.configParams = configParams;
    }
}
