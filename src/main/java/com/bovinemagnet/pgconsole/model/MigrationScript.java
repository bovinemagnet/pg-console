package com.bovinemagnet.pgconsole.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generated DDL migration script from schema comparison.
 * <p>
 * Contains ordered migration statements with options for transaction
 * wrapping and statement filtering.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class MigrationScript {

    /**
     * Transaction wrapping options for migration scripts.
     */
    public enum WrapOption {
        SINGLE_TRANSACTION("Single Transaction",
                "Wrap all statements in a single BEGIN/COMMIT block"),
        INDIVIDUAL_STATEMENTS("Individual Statements",
                "Execute each statement independently"),
        SAVEPOINT_PER_OBJECT("Savepoints",
                "Use savepoints to allow partial rollback on errors");

        private final String displayName;
        private final String description;

        WrapOption(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    private List<MigrationStatement> statements = new ArrayList<>();
    private WrapOption wrapOption = WrapOption.SINGLE_TRANSACTION;
    private boolean includeDropStatements;
    private Instant generatedAt;
    private String generatedBy;
    private String sourceInstance;
    private String destinationInstance;
    private String sourceSchema;
    private String destinationSchema;

    public MigrationScript() {
        this.generatedAt = Instant.now();
    }

    /**
     * Gets the full migration script as a single string.
     *
     * @return complete script
     */
    public String getFullScript() {
        StringBuilder script = new StringBuilder();

        // Header comment
        script.append("-- Migration Script\n");
        script.append("-- Generated: ").append(getGeneratedAtFormatted()).append("\n");
        script.append("-- Source: ").append(sourceInstance).append(".").append(sourceSchema).append("\n");
        script.append("-- Destination: ").append(destinationInstance).append(".").append(destinationSchema).append("\n");
        script.append("-- Statements: ").append(statements.size()).append("\n");
        script.append("\n");

        List<MigrationStatement> orderedStatements = getOrderedStatements();

        switch (wrapOption) {
            case SINGLE_TRANSACTION -> {
                script.append("BEGIN;\n\n");
                for (MigrationStatement stmt : orderedStatements) {
                    if (!includeDropStatements && stmt.isDropStatement()) {
                        continue;
                    }
                    script.append(formatStatement(stmt));
                }
                script.append("COMMIT;\n");
            }
            case INDIVIDUAL_STATEMENTS -> {
                for (MigrationStatement stmt : orderedStatements) {
                    if (!includeDropStatements && stmt.isDropStatement()) {
                        continue;
                    }
                    script.append(formatStatement(stmt));
                }
            }
            case SAVEPOINT_PER_OBJECT -> {
                script.append("BEGIN;\n\n");
                int savepoint = 0;
                for (MigrationStatement stmt : orderedStatements) {
                    if (!includeDropStatements && stmt.isDropStatement()) {
                        continue;
                    }
                    script.append("SAVEPOINT sp_").append(savepoint++).append(";\n");
                    script.append(formatStatement(stmt));
                }
                script.append("COMMIT;\n");
            }
        }

        return script.toString();
    }

    private String formatStatement(MigrationStatement stmt) {
        StringBuilder sb = new StringBuilder();
        if (stmt.warningMessage != null) {
            sb.append("-- WARNING: ").append(stmt.warningMessage).append("\n");
        }
        sb.append("-- ").append(stmt.objectType.getDisplayName())
                .append(": ").append(stmt.objectName).append("\n");
        sb.append(stmt.ddl);
        if (!stmt.ddl.endsWith(";")) {
            sb.append(";");
        }
        sb.append("\n\n");
        return sb.toString();
    }

    /**
     * Gets statements ordered by dependency.
     *
     * @return ordered statements
     */
    public List<MigrationStatement> getOrderedStatements() {
        return statements.stream()
                .sorted(Comparator.comparingInt(s -> s.order))
                .collect(Collectors.toList());
    }

    /**
     * Gets only CREATE statements.
     *
     * @return create statements only
     */
    public String getCreateStatementsOnly() {
        return statements.stream()
                .filter(s -> s.ddl.trim().toUpperCase().startsWith("CREATE"))
                .sorted(Comparator.comparingInt(s -> s.order))
                .map(s -> s.ddl + (s.ddl.endsWith(";") ? "" : ";"))
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * Gets only ALTER statements.
     *
     * @return alter statements only
     */
    public String getAlterStatementsOnly() {
        return statements.stream()
                .filter(s -> s.ddl.trim().toUpperCase().startsWith("ALTER"))
                .sorted(Comparator.comparingInt(s -> s.order))
                .map(s -> s.ddl + (s.ddl.endsWith(";") ? "" : ";"))
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * Gets only DROP statements.
     *
     * @return drop statements only
     */
    public String getDropStatementsOnly() {
        return statements.stream()
                .filter(MigrationStatement::isDropStatement)
                .sorted(Comparator.comparingInt(s -> s.order))
                .map(s -> s.ddl + (s.ddl.endsWith(";") ? "" : ";"))
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * Gets formatted generation timestamp.
     *
     * @return formatted date/time
     */
    public String getGeneratedAtFormatted() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(generatedAt);
    }

    /**
     * Gets total statement count.
     *
     * @return statement count
     */
    public int getStatementCount() {
        return statements.size();
    }

    /**
     * Gets count of breaking (DROP) statements.
     *
     * @return breaking statement count
     */
    public long getBreakingStatementCount() {
        return statements.stream()
                .filter(s -> s.severity == ObjectDifference.Severity.BREAKING)
                .count();
    }

    /**
     * Adds a migration statement.
     *
     * @param statement statement to add
     */
    public void addStatement(MigrationStatement statement) {
        statements.add(statement);
    }

    /**
     * Single DDL statement in the migration.
     */
    public static class MigrationStatement {
        private int order;
        private String ddl;
        private ObjectDifference.ObjectType objectType;
        private String objectName;
        private ObjectDifference.Severity severity;
        private List<String> dependencies = new ArrayList<>();
        private String warningMessage;
        private boolean reversible;
        private String rollbackDdl;

        public MigrationStatement() {
        }

        public MigrationStatement(int order, String ddl, ObjectDifference.ObjectType objectType,
                                   String objectName, ObjectDifference.Severity severity) {
            this.order = order;
            this.ddl = ddl;
            this.objectType = objectType;
            this.objectName = objectName;
            this.severity = severity;
        }

        public boolean isDropStatement() {
            return ddl != null && ddl.trim().toUpperCase().startsWith("DROP");
        }

        public boolean isCreateStatement() {
            return ddl != null && ddl.trim().toUpperCase().startsWith("CREATE");
        }

        public boolean isAlterStatement() {
            return ddl != null && ddl.trim().toUpperCase().startsWith("ALTER");
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private final MigrationStatement stmt = new MigrationStatement();

            public Builder order(int order) { stmt.order = order; return this; }
            public Builder ddl(String ddl) { stmt.ddl = ddl; return this; }
            public Builder objectType(ObjectDifference.ObjectType objectType) { stmt.objectType = objectType; return this; }
            public Builder objectName(String objectName) { stmt.objectName = objectName; return this; }
            public Builder severity(ObjectDifference.Severity severity) { stmt.severity = severity; return this; }
            public Builder dependency(String dep) { stmt.dependencies.add(dep); return this; }
            public Builder warningMessage(String msg) { stmt.warningMessage = msg; return this; }
            public Builder reversible(boolean rev) { stmt.reversible = rev; return this; }
            public Builder rollbackDdl(String ddl) { stmt.rollbackDdl = ddl; return this; }
            public MigrationStatement build() { return stmt; }
        }

        // Getters and Setters
        public int getOrder() { return order; }
        public void setOrder(int order) { this.order = order; }
        public String getDdl() { return ddl; }
        public void setDdl(String ddl) { this.ddl = ddl; }
        public ObjectDifference.ObjectType getObjectType() { return objectType; }
        public void setObjectType(ObjectDifference.ObjectType objectType) { this.objectType = objectType; }
        public String getObjectName() { return objectName; }
        public void setObjectName(String objectName) { this.objectName = objectName; }
        public ObjectDifference.Severity getSeverity() { return severity; }
        public void setSeverity(ObjectDifference.Severity severity) { this.severity = severity; }
        public List<String> getDependencies() { return dependencies; }
        public void setDependencies(List<String> dependencies) { this.dependencies = dependencies; }
        public String getWarningMessage() { return warningMessage; }
        public void setWarningMessage(String warningMessage) { this.warningMessage = warningMessage; }
        public boolean isReversible() { return reversible; }
        public void setReversible(boolean reversible) { this.reversible = reversible; }
        public String getRollbackDdl() { return rollbackDdl; }
        public void setRollbackDdl(String rollbackDdl) { this.rollbackDdl = rollbackDdl; }
    }

    // Getters and Setters

    public List<MigrationStatement> getStatements() {
        return statements;
    }

    public void setStatements(List<MigrationStatement> statements) {
        this.statements = statements;
    }

    public WrapOption getWrapOption() {
        return wrapOption;
    }

    public void setWrapOption(WrapOption wrapOption) {
        this.wrapOption = wrapOption;
    }

    public boolean isIncludeDropStatements() {
        return includeDropStatements;
    }

    public void setIncludeDropStatements(boolean includeDropStatements) {
        this.includeDropStatements = includeDropStatements;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(String generatedBy) {
        this.generatedBy = generatedBy;
    }

    public String getSourceInstance() {
        return sourceInstance;
    }

    public void setSourceInstance(String sourceInstance) {
        this.sourceInstance = sourceInstance;
    }

    public String getDestinationInstance() {
        return destinationInstance;
    }

    public void setDestinationInstance(String destinationInstance) {
        this.destinationInstance = destinationInstance;
    }

    public String getSourceSchema() {
        return sourceSchema;
    }

    public void setSourceSchema(String sourceSchema) {
        this.sourceSchema = sourceSchema;
    }

    public String getDestinationSchema() {
        return destinationSchema;
    }

    public void setDestinationSchema(String destinationSchema) {
        this.destinationSchema = destinationSchema;
    }
}
