package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.Runbook;
import com.bovinemagnet.pgconsole.model.RunbookExecution;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/**
 * Service for managing runbooks and their execution.
 * <p>
 * Provides guided troubleshooting and incident response through
 * step-by-step playbooks.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class RunbookService {

    private static final Logger LOG = Logger.getLogger(RunbookService.class);

    private final ObjectMapper objectMapper;

    public RunbookService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Inject
    DataSourceManager dataSourceManager;

    @Inject
    PostgresService postgresService;

    @Inject
    TableMaintenanceService tableMaintenanceService;

    /**
     * Get all available runbooks.
     *
     * @param instanceName the PostgreSQL instance name
     * @return list of runbooks
     */
    public List<Runbook> getRunbooks(String instanceName) {
        List<Runbook> runbooks = new ArrayList<>();

        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            String sql = """
                SELECT id, name, title, description, category, trigger_type,
                       trigger_conditions, steps, version, enabled, created_at,
                       updated_at, created_by, estimated_duration_minutes, auto_executable
                FROM pgconsole.runbook
                WHERE enabled = true
                ORDER BY category, title
                """;

            try (Connection conn = ds.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    runbooks.add(mapRunbook(rs));
                }
            }

        } catch (Exception e) {
            LOG.debugf(e, "Error getting runbooks");
        }

        return runbooks;
    }

    /**
     * Get runbooks by category.
     *
     * @param instanceName the PostgreSQL instance name
     * @param category the category to filter by
     * @return list of runbooks
     */
    public List<Runbook> getRunbooksByCategory(String instanceName, Runbook.Category category) {
        List<Runbook> runbooks = new ArrayList<>();

        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            String sql = """
                SELECT id, name, title, description, category, trigger_type,
                       trigger_conditions, steps, version, enabled, created_at,
                       updated_at, created_by, estimated_duration_minutes, auto_executable
                FROM pgconsole.runbook
                WHERE enabled = true AND category = ?
                ORDER BY title
                """;

            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, category.name());

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        runbooks.add(mapRunbook(rs));
                    }
                }
            }

        } catch (Exception e) {
            LOG.debugf(e, "Error getting runbooks by category");
        }

        return runbooks;
    }

    /**
     * Get a specific runbook by ID.
     *
     * @param instanceName the PostgreSQL instance name
     * @param runbookId the runbook ID
     * @return the runbook, or null if not found
     */
    public Runbook getRunbook(String instanceName, long runbookId) {
        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            String sql = """
                SELECT id, name, title, description, category, trigger_type,
                       trigger_conditions, steps, version, enabled, created_at,
                       updated_at, created_by, estimated_duration_minutes, auto_executable
                FROM pgconsole.runbook
                WHERE id = ?
                """;

            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setLong(1, runbookId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapRunbook(rs);
                    }
                }
            }

        } catch (Exception e) {
            LOG.errorf(e, "Error getting runbook %d", runbookId);
        }

        return null;
    }

    /**
     * Get a runbook by name.
     *
     * @param instanceName the PostgreSQL instance name
     * @param runbookName the runbook name
     * @return the runbook, or null if not found
     */
    public Runbook getRunbookByName(String instanceName, String runbookName) {
        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            String sql = """
                SELECT id, name, title, description, category, trigger_type,
                       trigger_conditions, steps, version, enabled, created_at,
                       updated_at, created_by, estimated_duration_minutes, auto_executable
                FROM pgconsole.runbook
                WHERE name = ?
                """;

            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, runbookName);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapRunbook(rs);
                    }
                }
            }

        } catch (Exception e) {
            LOG.errorf(e, "Error getting runbook %s", runbookName);
        }

        return null;
    }

    /**
     * Start execution of a runbook.
     *
     * @param instanceName the PostgreSQL instance name
     * @param runbookId the runbook ID to execute
     * @param triggeredBy what triggered the execution
     * @param username the user starting the execution
     * @return the execution record
     */
    public RunbookExecution startExecution(String instanceName, long runbookId,
                                            RunbookExecution.TriggeredBy triggeredBy,
                                            String username) {
        return startExecution(instanceName, runbookId, triggeredBy, username, null);
    }

    /**
     * Start execution of a runbook scoped to a specific database.
     *
     * @param instanceName the PostgreSQL instance name
     * @param runbookId the runbook ID to execute
     * @param triggeredBy what triggered the execution
     * @param username the user starting the execution
     * @param databaseName the specific database to scope to (null for all databases)
     * @return the execution record
     */
    public RunbookExecution startExecution(String instanceName, long runbookId,
                                            RunbookExecution.TriggeredBy triggeredBy,
                                            String username, String databaseName) {
        Runbook runbook = getRunbook(instanceName, runbookId);
        if (runbook == null) {
            throw new IllegalArgumentException("Runbook not found: " + runbookId);
        }

        RunbookExecution execution = new RunbookExecution();
        execution.setRunbookId(runbookId);
        execution.setRunbook(runbook);
        execution.setInstanceId(instanceName);
        execution.setDatabaseName(databaseName);
        execution.setTriggeredBy(triggeredBy);
        execution.setExecutedBy(username);
        execution.setStatus(RunbookExecution.Status.IN_PROGRESS);
        execution.setCurrentStep(1);

        // Initialise step results
        List<RunbookExecution.StepResult> stepResults = new ArrayList<>();
        for (Runbook.Step step : runbook.getSteps()) {
            stepResults.add(new RunbookExecution.StepResult(step.getOrder()));
        }
        execution.setStepResults(stepResults);

        // Save execution
        saveExecution(instanceName, execution);

        return execution;
    }

    /**
     * Advance to the next step in an execution.
     *
     * @param instanceName the PostgreSQL instance name
     * @param executionId the execution ID
     * @param output output from the completed step
     * @return updated execution
     */
    public RunbookExecution advanceStep(String instanceName, long executionId, String output) {
        RunbookExecution execution = getExecution(instanceName, executionId);
        if (execution == null) {
            throw new IllegalArgumentException("Execution not found: " + executionId);
        }

        // Mark current step as completed
        int currentIdx = execution.getCurrentStep() - 1;
        if (currentIdx < execution.getStepResults().size()) {
            RunbookExecution.StepResult stepResult = execution.getStepResults().get(currentIdx);
            stepResult.setStatus(RunbookExecution.StepResult.StepStatus.COMPLETED);
            stepResult.setCompletedAt(Instant.now());
            stepResult.setOutput(output);
        }

        // Move to next step or complete
        if (execution.getCurrentStep() >= execution.getRunbook().getStepCount()) {
            execution.setStatus(RunbookExecution.Status.COMPLETED);
            execution.setCompletedAt(Instant.now());
        } else {
            execution.setCurrentStep(execution.getCurrentStep() + 1);

            // Start next step
            int nextIdx = execution.getCurrentStep() - 1;
            if (nextIdx < execution.getStepResults().size()) {
                RunbookExecution.StepResult nextStep = execution.getStepResults().get(nextIdx);
                nextStep.setStatus(RunbookExecution.StepResult.StepStatus.RUNNING);
                nextStep.setStartedAt(Instant.now());
            }
        }

        // Update in database
        updateExecution(instanceName, execution);

        return execution;
    }

    /**
     * Skip a step in an execution.
     *
     * @param instanceName the PostgreSQL instance name
     * @param executionId the execution ID
     * @return updated execution
     */
    public RunbookExecution skipStep(String instanceName, long executionId) {
        RunbookExecution execution = getExecution(instanceName, executionId);
        if (execution == null) {
            throw new IllegalArgumentException("Execution not found: " + executionId);
        }

        // Mark current step as skipped
        int currentIdx = execution.getCurrentStep() - 1;
        if (currentIdx < execution.getStepResults().size()) {
            RunbookExecution.StepResult stepResult = execution.getStepResults().get(currentIdx);
            stepResult.setStatus(RunbookExecution.StepResult.StepStatus.SKIPPED);
            stepResult.setCompletedAt(Instant.now());
        }

        // Move to next step
        return advanceStep(instanceName, executionId, "Skipped");
    }

    /**
     * Cancel an execution.
     *
     * @param instanceName the PostgreSQL instance name
     * @param executionId the execution ID
     * @return updated execution
     */
    public RunbookExecution cancelExecution(String instanceName, long executionId) {
        RunbookExecution execution = getExecution(instanceName, executionId);
        if (execution == null) {
            throw new IllegalArgumentException("Execution not found: " + executionId);
        }

        execution.setStatus(RunbookExecution.Status.CANCELLED);
        execution.setCompletedAt(Instant.now());

        updateExecution(instanceName, execution);

        return execution;
    }

    /**
     * Auto-execute a runbook, running all steps automatically.
     * <p>
     * Only runbooks marked as autoExecutable can be auto-executed.
     * QUERY steps are executed directly, SQL_TEMPLATE steps with table_name
     * parameters are executed for each table needing maintenance.
     *
     * @param instanceName the PostgreSQL instance name
     * @param runbookId the runbook ID to execute
     * @param username the user starting the execution
     * @return the completed execution record
     */
    public RunbookExecution autoExecuteRunbook(String instanceName, long runbookId, String username) {
        return autoExecuteRunbook(instanceName, runbookId, username, null);
    }

    /**
     * Auto-execute a runbook scoped to a specific database.
     * <p>
     * Only runbooks marked as autoExecutable can be auto-executed.
     * QUERY steps are executed directly, SQL_TEMPLATE steps with table_name
     * parameters are executed for each table needing maintenance.
     *
     * @param instanceName the PostgreSQL instance name
     * @param runbookId the runbook ID to execute
     * @param username the user starting the execution
     * @param databaseName the specific database to scope to (null for all databases)
     * @return the completed execution record
     */
    public RunbookExecution autoExecuteRunbook(String instanceName, long runbookId,
                                                String username, String databaseName) {
        Runbook runbook = getRunbook(instanceName, runbookId);
        if (runbook == null) {
            throw new IllegalArgumentException("Runbook not found: " + runbookId);
        }

        if (!runbook.isAutoExecutable()) {
            throw new IllegalArgumentException("Runbook is not marked as auto-executable: " + runbook.getName());
        }

        // Start execution with database scope
        RunbookExecution execution = startExecution(instanceName, runbookId,
                RunbookExecution.TriggeredBy.MANUAL, username, databaseName);

        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            // Execute each step
            for (Runbook.Step step : runbook.getSteps()) {
                int stepIdx = step.getOrder() - 1;
                RunbookExecution.StepResult stepResult = execution.getStepResults().get(stepIdx);
                stepResult.setStatus(RunbookExecution.StepResult.StepStatus.RUNNING);
                stepResult.setStartedAt(Instant.now());

                try {
                    String output = executeStep(ds, instanceName, databaseName, step);
                    stepResult.setStatus(RunbookExecution.StepResult.StepStatus.COMPLETED);
                    stepResult.setOutput(output);
                } catch (Exception e) {
                    LOG.warnf(e, "Auto-execute step %d failed: %s", step.getOrder(), e.getMessage());
                    stepResult.setStatus(RunbookExecution.StepResult.StepStatus.FAILED);
                    stepResult.setErrorMessage(e.getMessage());
                    // Continue to next step, don't fail the whole runbook
                }

                stepResult.setCompletedAt(Instant.now());
                execution.setCurrentStep(step.getOrder() + 1);
            }

            // Mark execution as completed
            execution.setStatus(RunbookExecution.Status.COMPLETED);
            execution.setCompletedAt(Instant.now());
            execution.setCurrentStep(runbook.getStepCount());

        } catch (Exception e) {
            LOG.errorf(e, "Error auto-executing runbook %s", runbook.getName());
            execution.setStatus(RunbookExecution.Status.FAILED);
            execution.setCompletedAt(Instant.now());
            execution.setNotes("Auto-execution failed: " + e.getMessage());
        }

        updateExecution(instanceName, execution);
        return execution;
    }

    /**
     * Execute a single step and return the output.
     */
    private String executeStep(DataSource ds, String instanceName, String databaseName, Runbook.Step step) throws SQLException {
        StringBuilder output = new StringBuilder();

        switch (step.getActionType()) {
            case QUERY:
                // Execute the query and return results
                try (Connection conn = ds.getConnection();
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(step.getAction())) {

                    int rowCount = 0;
                    while (rs.next() && rowCount < 100) {
                        int colCount = rs.getMetaData().getColumnCount();
                        for (int i = 1; i <= colCount; i++) {
                            if (i > 1) output.append(" | ");
                            output.append(rs.getMetaData().getColumnName(i))
                                  .append(": ")
                                  .append(rs.getString(i));
                        }
                        output.append("\n");
                        rowCount++;
                    }
                    if (rowCount == 0) {
                        output.append("No results");
                    } else {
                        output.insert(0, rowCount + " row(s) returned:\n");
                    }
                }
                break;

            case SQL_TEMPLATE:
                // Handle SQL templates with table_name parameter
                String sql = step.getAction();
                if (sql.contains("{table_name}")) {
                    // Get tables needing vacuum
                    var recommendations = tableMaintenanceService.findTablesNeedingVacuum(instanceName);
                    if (recommendations.isEmpty()) {
                        output.append("No tables need maintenance");
                    } else {
                        int executed = 0;
                        for (var rec : recommendations) {
                            String tableFqn = rec.getSchemaName() + "." + rec.getTableName();
                            String execSql = sql.replace("{table_name}", tableFqn);

                            try (Connection conn = ds.getConnection();
                                 Statement stmt = conn.createStatement()) {
                                stmt.execute(execSql);
                                output.append("Executed: ").append(execSql).append("\n");
                                executed++;
                            } catch (SQLException e) {
                                output.append("Failed: ").append(execSql)
                                      .append(" - ").append(e.getMessage()).append("\n");
                            }

                            // Limit to first 10 tables to avoid long-running operations
                            if (executed >= 10) {
                                output.append("... limited to first 10 tables\n");
                                break;
                            }
                        }
                        output.insert(0, "Processed " + executed + " table(s):\n");
                    }
                } else {
                    // Execute as-is
                    try (Connection conn = ds.getConnection();
                         Statement stmt = conn.createStatement()) {
                        stmt.execute(sql);
                        output.append("Executed: ").append(sql);
                    }
                }
                break;

            case NAVIGATE:
                // Capture actual data from the target page instead of just linking to it
                output.append(captureNavigationData(instanceName, databaseName, step.getAction()));
                break;

            case DOCUMENTATION:
                output.append("Documentation reference: ").append(step.getAction());
                break;

            case MANUAL:
                output.append("Manual step skipped in auto-execute mode");
                break;

            default:
                output.append("Unknown action type: ").append(step.getActionType());
        }

        return output.toString();
    }

    /**
     * Get an execution by ID.
     *
     * @param instanceName the PostgreSQL instance name
     * @param executionId the execution ID
     * @return the execution, or null if not found
     */
    public RunbookExecution getExecution(String instanceName, long executionId) {
        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            String sql = """
                SELECT e.id, e.runbook_id, e.instance_id, e.database_name, e.started_at, e.completed_at,
                       e.status, e.triggered_by, e.current_step, e.step_results,
                       e.executed_by, e.notes,
                       r.id as r_id, r.name as r_name, r.title as r_title,
                       r.description as r_description, r.category as r_category,
                       r.trigger_type as r_trigger_type, r.steps as r_steps,
                       r.estimated_duration_minutes as r_estimated_duration
                FROM pgconsole.runbook_execution e
                JOIN pgconsole.runbook r ON r.id = e.runbook_id
                WHERE e.id = ? AND e.instance_id = ?
                """;

            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setLong(1, executionId);
                stmt.setString(2, instanceName);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapExecution(rs);
                    }
                }
            }

        } catch (Exception e) {
            LOG.errorf(e, "Error getting execution %d", executionId);
        }

        return null;
    }

    /**
     * Get recent executions for an instance.
     *
     * @param instanceName the PostgreSQL instance name
     * @param limit maximum number to return
     * @return list of executions
     */
    public List<RunbookExecution> getRecentExecutions(String instanceName, int limit) {
        List<RunbookExecution> executions = new ArrayList<>();

        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            String sql = """
                SELECT e.id, e.runbook_id, e.instance_id, e.database_name, e.started_at, e.completed_at,
                       e.status, e.triggered_by, e.current_step, e.step_results,
                       e.executed_by, e.notes,
                       r.id as r_id, r.name as r_name, r.title as r_title,
                       r.description as r_description, r.category as r_category,
                       r.trigger_type as r_trigger_type, r.steps as r_steps,
                       r.estimated_duration_minutes as r_estimated_duration
                FROM pgconsole.runbook_execution e
                JOIN pgconsole.runbook r ON r.id = e.runbook_id
                WHERE e.instance_id = ?
                ORDER BY e.started_at DESC
                LIMIT ?
                """;

            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, instanceName);
                stmt.setInt(2, limit);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        executions.add(mapExecution(rs));
                    }
                }
            }

        } catch (Exception e) {
            LOG.debugf(e, "Error getting recent executions");
        }

        return executions;
    }

    /**
     * Get in-progress executions.
     *
     * @param instanceName the PostgreSQL instance name
     * @return list of in-progress executions
     */
    public List<RunbookExecution> getInProgressExecutions(String instanceName) {
        List<RunbookExecution> executions = new ArrayList<>();

        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            String sql = """
                SELECT e.id, e.runbook_id, e.instance_id, e.database_name, e.started_at, e.completed_at,
                       e.status, e.triggered_by, e.current_step, e.step_results,
                       e.executed_by, e.notes,
                       r.id as r_id, r.name as r_name, r.title as r_title,
                       r.description as r_description, r.category as r_category,
                       r.trigger_type as r_trigger_type, r.steps as r_steps,
                       r.estimated_duration_minutes as r_estimated_duration
                FROM pgconsole.runbook_execution e
                JOIN pgconsole.runbook r ON r.id = e.runbook_id
                WHERE e.instance_id = ? AND e.status = 'IN_PROGRESS'
                ORDER BY e.started_at DESC
                """;

            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, instanceName);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        executions.add(mapExecution(rs));
                    }
                }
            }

        } catch (Exception e) {
            LOG.debugf(e, "Error getting in-progress executions");
        }

        return executions;
    }

    // Private helper methods

    private Runbook mapRunbook(ResultSet rs) throws SQLException {
        Runbook runbook = new Runbook();
        runbook.setId(rs.getLong("id"));
        runbook.setName(rs.getString("name"));
        runbook.setTitle(rs.getString("title"));
        runbook.setDescription(rs.getString("description"));

        String categoryStr = rs.getString("category");
        if (categoryStr != null) {
            try {
                runbook.setCategory(Runbook.Category.valueOf(categoryStr));
            } catch (IllegalArgumentException e) {
                runbook.setCategory(Runbook.Category.TROUBLESHOOTING);
            }
        }

        String triggerTypeStr = rs.getString("trigger_type");
        if (triggerTypeStr != null) {
            try {
                runbook.setTriggerType(Runbook.TriggerType.valueOf(triggerTypeStr));
            } catch (IllegalArgumentException e) {
                runbook.setTriggerType(Runbook.TriggerType.MANUAL);
            }
        }

        runbook.setTriggerConditions(rs.getString("trigger_conditions"));

        // Parse steps JSON
        String stepsJson = rs.getString("steps");
        if (stepsJson != null) {
            try {
                List<Runbook.Step> steps = objectMapper.readValue(stepsJson,
                        new TypeReference<List<Runbook.Step>>() {});
                runbook.setSteps(steps);
            } catch (Exception e) {
                LOG.debugf(e, "Error parsing steps JSON");
                runbook.setSteps(new ArrayList<>());
            }
        }

        runbook.setVersion(rs.getInt("version"));
        runbook.setEnabled(rs.getBoolean("enabled"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            runbook.setCreatedAt(createdAt.toInstant());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            runbook.setUpdatedAt(updatedAt.toInstant());
        }

        runbook.setCreatedBy(rs.getString("created_by"));
        runbook.setEstimatedDurationMinutes(rs.getInt("estimated_duration_minutes"));

        // Handle auto_executable column which may not exist in older schemas
        try {
            runbook.setAutoExecutable(rs.getBoolean("auto_executable"));
        } catch (SQLException e) {
            // Column doesn't exist, default to false
            runbook.setAutoExecutable(false);
        }

        return runbook;
    }

    private RunbookExecution mapExecution(ResultSet rs) throws SQLException {
        RunbookExecution execution = new RunbookExecution();
        execution.setId(rs.getLong("id"));
        execution.setRunbookId(rs.getLong("runbook_id"));
        execution.setInstanceId(rs.getString("instance_id"));

        // Read database_name (may be null for instance-wide executions)
        try {
            execution.setDatabaseName(rs.getString("database_name"));
        } catch (SQLException e) {
            // Column may not exist in older schemas
            execution.setDatabaseName(null);
        }

        Timestamp startedAt = rs.getTimestamp("started_at");
        if (startedAt != null) {
            execution.setStartedAt(startedAt.toInstant());
        }

        Timestamp completedAt = rs.getTimestamp("completed_at");
        if (completedAt != null) {
            execution.setCompletedAt(completedAt.toInstant());
        }

        String statusStr = rs.getString("status");
        if (statusStr != null) {
            try {
                execution.setStatus(RunbookExecution.Status.valueOf(statusStr));
            } catch (IllegalArgumentException e) {
                execution.setStatus(RunbookExecution.Status.IN_PROGRESS);
            }
        }

        String triggeredByStr = rs.getString("triggered_by");
        if (triggeredByStr != null) {
            try {
                execution.setTriggeredBy(RunbookExecution.TriggeredBy.valueOf(triggeredByStr));
            } catch (IllegalArgumentException e) {
                execution.setTriggeredBy(RunbookExecution.TriggeredBy.MANUAL);
            }
        }

        execution.setCurrentStep(rs.getInt("current_step"));

        // Parse step results JSON
        String stepResultsJson = rs.getString("step_results");
        if (stepResultsJson != null) {
            try {
                List<RunbookExecution.StepResult> stepResults = objectMapper.readValue(stepResultsJson,
                        new TypeReference<List<RunbookExecution.StepResult>>() {});
                execution.setStepResults(stepResults);
            } catch (Exception e) {
                LOG.warnf(e, "Error parsing step results JSON: %s", stepResultsJson);
                execution.setStepResults(new ArrayList<>());
            }
        }

        execution.setExecutedBy(rs.getString("executed_by"));
        execution.setNotes(rs.getString("notes"));

        // Map embedded runbook
        Runbook runbook = new Runbook();
        runbook.setId(rs.getLong("r_id"));
        runbook.setName(rs.getString("r_name"));
        runbook.setTitle(rs.getString("r_title"));
        runbook.setDescription(rs.getString("r_description"));

        String categoryStr = rs.getString("r_category");
        if (categoryStr != null) {
            try {
                runbook.setCategory(Runbook.Category.valueOf(categoryStr));
            } catch (IllegalArgumentException e) {
                runbook.setCategory(Runbook.Category.TROUBLESHOOTING);
            }
        }

        String stepsJson = rs.getString("r_steps");
        if (stepsJson != null) {
            try {
                List<Runbook.Step> steps = objectMapper.readValue(stepsJson,
                        new TypeReference<List<Runbook.Step>>() {});
                runbook.setSteps(steps);
            } catch (Exception e) {
                runbook.setSteps(new ArrayList<>());
            }
        }

        runbook.setEstimatedDurationMinutes(rs.getInt("r_estimated_duration"));

        execution.setRunbook(runbook);

        return execution;
    }

    private void saveExecution(String instanceName, RunbookExecution execution) {
        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            String sql = """
                INSERT INTO pgconsole.runbook_execution
                    (runbook_id, instance_id, database_name, started_at, status, triggered_by,
                     current_step, step_results, executed_by)
                VALUES (?, ?, ?, NOW(), ?, ?, ?, ?::jsonb, ?)
                RETURNING id
                """;

            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setLong(1, execution.getRunbookId());
                stmt.setString(2, instanceName);
                stmt.setString(3, execution.getDatabaseName());
                stmt.setString(4, execution.getStatus().name());
                stmt.setString(5, execution.getTriggeredBy().name());
                stmt.setInt(6, execution.getCurrentStep());
                stmt.setString(7, objectMapper.writeValueAsString(execution.getStepResults()));
                stmt.setString(8, execution.getExecutedBy());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        execution.setId(rs.getLong(1));
                    }
                }
            }

        } catch (Exception e) {
            LOG.errorf(e, "Error saving execution");
        }
    }

    private void updateExecution(String instanceName, RunbookExecution execution) {
        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            String sql = """
                UPDATE pgconsole.runbook_execution
                SET status = ?, completed_at = ?, current_step = ?, step_results = ?::jsonb, notes = ?
                WHERE id = ? AND instance_id = ?
                """;

            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, execution.getStatus().name());
                stmt.setTimestamp(2, execution.getCompletedAt() != null ?
                        Timestamp.from(execution.getCompletedAt()) : null);
                stmt.setInt(3, execution.getCurrentStep());
                stmt.setString(4, objectMapper.writeValueAsString(execution.getStepResults()));
                stmt.setString(5, execution.getNotes());
                stmt.setLong(6, execution.getId());
                stmt.setString(7, instanceName);

                stmt.executeUpdate();
            }

        } catch (Exception e) {
            LOG.errorf(e, "Error updating execution");
        }
    }

    // ============================================================
    // Navigation Data Capture Methods
    // ============================================================

    /**
     * Captures actual data from a navigation target instead of just linking to it.
     * This makes the runbook execution report self-contained with all diagnostic data.
     *
     * @param instanceName the PostgreSQL instance
     * @param databaseName the specific database to filter by (null for all)
     * @param navigationPath the path being navigated to (e.g., "/activity")
     * @return formatted data from the target page
     */
    private String captureNavigationData(String instanceName, String databaseName, String navigationPath) {
        if (navigationPath == null || navigationPath.isEmpty()) {
            return "No navigation target specified";
        }

        // Extract base path without query parameters
        String basePath = navigationPath.split("\\?")[0];

        // Add database scope header if filtering by database
        String scopeHeader = databaseName != null && !databaseName.isEmpty()
                ? "[Database: " + databaseName + "]\n\n"
                : "";

        try {
            String data = switch (basePath) {
                case "/activity" -> captureActivityData(instanceName, databaseName);
                case "/slow-queries" -> captureSlowQueriesData(instanceName, databaseName, navigationPath);
                case "/", "/index", "" -> captureOverviewData(instanceName, databaseName);
                case "/locks" -> captureLocksData(instanceName, databaseName);
                case "/table-maintenance" -> captureTableMaintenanceData(instanceName);
                case "/tables" -> captureTablesData(instanceName);
                default -> "Navigation to: " + navigationPath + " (data capture not available for this page)";
            };
            return scopeHeader + data;
        } catch (Exception e) {
            LOG.warnf(e, "Error capturing data for navigation: %s", navigationPath);
            return scopeHeader + "Navigation to: " + navigationPath + " (error capturing data: " + e.getMessage() + ")";
        }
    }

    /**
     * Captures current database activity (pg_stat_activity).
     *
     * @param instanceName the PostgreSQL instance
     * @param databaseName optional database to filter by (null for all)
     */
    private String captureActivityData(String instanceName, String databaseName) {
        var activities = postgresService.getCurrentActivity(instanceName);

        // Filter by database if specified
        if (databaseName != null && !databaseName.isEmpty()) {
            activities = activities.stream()
                    .filter(a -> databaseName.equals(a.getDatabase()))
                    .toList();
        }

        if (activities.isEmpty()) {
            return "=== Current Activity ===\nNo active sessions (excluding idle connections)";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== Current Activity ===\n");
        sb.append(String.format("Total active sessions: %d\n\n", activities.size()));

        // Group by state
        var byState = new LinkedHashMap<String, List<com.bovinemagnet.pgconsole.model.Activity>>();
        for (var activity : activities) {
            String state = activity.getState() != null ? activity.getState() : "unknown";
            byState.computeIfAbsent(state, k -> new ArrayList<>()).add(activity);
        }

        for (var entry : byState.entrySet()) {
            sb.append(String.format("--- %s (%d) ---\n", entry.getKey().toUpperCase(), entry.getValue().size()));
            for (var activity : entry.getValue()) {
                sb.append(String.format("  PID: %d | User: %s | DB: %s | App: %s\n",
                        activity.getPid(),
                        activity.getUser() != null ? activity.getUser() : "-",
                        activity.getDatabase() != null ? activity.getDatabase() : "-",
                        activity.getApplicationName() != null ? activity.getApplicationName() : "-"));
                if (activity.getQuery() != null && !activity.getQuery().isEmpty()) {
                    String truncatedQuery = activity.getQuery().length() > 200
                            ? activity.getQuery().substring(0, 200) + "..."
                            : activity.getQuery();
                    sb.append(String.format("    Query: %s\n", truncatedQuery.replace("\n", " ")));
                }
                if (activity.getWaitEventType() != null) {
                    sb.append(String.format("    Waiting: %s/%s\n",
                            activity.getWaitEventType(), activity.getWaitEvent()));
                }
                if (activity.getBlockingPid() != null && activity.getBlockingPid() > 0) {
                    sb.append(String.format("    BLOCKED BY PID: %d\n", activity.getBlockingPid()));
                }
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Captures slow query statistics (pg_stat_statements).
     *
     * @param instanceName the PostgreSQL instance
     * @param databaseName optional database to filter by (null for all)
     * @param navigationPath the navigation URL for parsing sort parameters
     */
    private String captureSlowQueriesData(String instanceName, String databaseName, String navigationPath) {
        // Parse sort parameter from URL
        String sortBy = "total_exec_time";
        if (navigationPath.contains("sort=duration")) {
            sortBy = "mean_exec_time";
        } else if (navigationPath.contains("sort=calls")) {
            sortBy = "calls";
        }

        var queries = postgresService.getSlowQueries(instanceName, sortBy, "DESC");

        // Filter by database if specified
        if (databaseName != null && !databaseName.isEmpty()) {
            queries = queries.stream()
                    .filter(q -> databaseName.equals(q.getDatabase()))
                    .toList();
        }

        if (queries.isEmpty()) {
            return "=== Slow Queries ===\nNo query statistics available (pg_stat_statements may not be enabled)";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== Slow Queries (Top 20 by " + sortBy + ") ===\n\n");

        int count = 0;
        for (var query : queries) {
            if (count >= 20) break;
            count++;

            sb.append(String.format("%d. Calls: %,d | Total: %.2f ms | Mean: %.2f ms | Max: %.2f ms\n",
                    count,
                    query.getTotalCalls(),
                    query.getTotalTime(),
                    query.getMeanTime(),
                    query.getMaxTime()));

            String truncatedQuery = query.getQuery() != null && query.getQuery().length() > 150
                    ? query.getQuery().substring(0, 150) + "..."
                    : query.getQuery();
            sb.append(String.format("   Query: %s\n\n",
                    truncatedQuery != null ? truncatedQuery.replace("\n", " ") : "-"));
        }

        return sb.toString();
    }

    /**
     * Captures dashboard overview statistics.
     * <p>
     * Note: Overview stats are instance-wide. When a database is specified,
     * a note is added but instance-level metrics are still shown.
     *
     * @param instanceName the PostgreSQL instance
     * @param databaseName optional database scope (informational only for overview)
     */
    private String captureOverviewData(String instanceName, String databaseName) {
        var stats = postgresService.getOverviewStats(instanceName);
        if (stats == null) {
            return "=== Overview ===\nUnable to retrieve overview statistics";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== Database Overview ===\n");
        if (databaseName != null && !databaseName.isEmpty()) {
            sb.append("(Note: Overview metrics are instance-wide)\n");
        }
        sb.append("\n");

        sb.append("--- Connection Stats ---\n");
        sb.append(String.format("  Connections: %d / %d (%.1f%% used)\n",
                stats.getConnectionsUsed(), stats.getMaxConnections(),
                stats.getMaxConnections() > 0 ? (stats.getConnectionsUsed() * 100.0 / stats.getMaxConnections()) : 0));
        sb.append(String.format("  Active Queries: %d\n", stats.getActiveQueries()));
        sb.append(String.format("  Blocked Queries: %d\n", stats.getBlockedQueries()));
        sb.append(String.format("  Longest Running: %s\n", stats.getLongestQueryDuration()));
        sb.append("\n");

        sb.append("--- Performance Stats ---\n");
        sb.append(String.format("  Cache Hit Ratio: %.2f%%\n", stats.getCacheHitRatio()));
        sb.append(String.format("  Database Size: %s\n", stats.getDatabaseSize()));
        sb.append("\n");

        if (stats.getTopTablesBySize() != null && !stats.getTopTablesBySize().isEmpty()) {
            sb.append("--- Top Tables by Size ---\n");
            for (var table : stats.getTopTablesBySize()) {
                sb.append(String.format("  %s.%s: %s\n",
                        table.getSchemaName(), table.getTableName(), table.getSize()));
            }
            sb.append("\n");
        }

        if (stats.getTopIndexesBySize() != null && !stats.getTopIndexesBySize().isEmpty()) {
            sb.append("--- Top Indexes by Size ---\n");
            for (var index : stats.getTopIndexesBySize()) {
                sb.append(String.format("  %s (%s): %s\n",
                        index.getIndexName(), index.getTableName(), index.getSize()));
            }
        }

        return sb.toString();
    }

    /**
     * Captures lock information and blocking tree.
     *
     * @param instanceName the PostgreSQL instance
     * @param databaseName optional database to filter by (null for all)
     */
    private String captureLocksData(String instanceName, String databaseName) {
        var blockingTree = postgresService.getBlockingTree(instanceName);
        var lockInfo = postgresService.getLockInfo(instanceName);

        // Filter lock info by database if specified
        if (databaseName != null && !databaseName.isEmpty()) {
            lockInfo = lockInfo.stream()
                    .filter(l -> databaseName.equals(l.getDatabase()))
                    .toList();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== Lock Analysis ===\n\n");

        // Blocking tree
        sb.append("--- Blocking Relationships ---\n");
        if (blockingTree.isEmpty()) {
            sb.append("  No blocking relationships detected\n");
        } else {
            for (var block : blockingTree) {
                sb.append(String.format("  BLOCKER PID %d (%s) -> BLOCKED PID %d (%s)\n",
                        block.getBlockerPid(),
                        block.getBlockerUser() != null ? block.getBlockerUser() : "-",
                        block.getBlockedPid(),
                        block.getBlockedUser() != null ? block.getBlockedUser() : "-"));
                sb.append(String.format("    Blocked duration: %s\n", block.getBlockedDuration()));
                sb.append(String.format("    Lock mode: %s\n", block.getLockMode()));
                if (block.getBlockerQuery() != null) {
                    String truncated = block.getBlockerQuery().length() > 100
                            ? block.getBlockerQuery().substring(0, 100) + "..."
                            : block.getBlockerQuery();
                    sb.append(String.format("    Blocker query: %s\n", truncated.replace("\n", " ")));
                }
            }
        }
        sb.append("\n");

        // Lock summary
        sb.append("--- Current Locks ---\n");
        if (lockInfo.isEmpty()) {
            sb.append("  No significant locks detected\n");
        } else {
            var granted = lockInfo.stream().filter(l -> l.isGranted()).count();
            var waiting = lockInfo.size() - granted;
            sb.append(String.format("  Total locks: %d (Granted: %d, Waiting: %d)\n",
                    lockInfo.size(), granted, waiting));

            // Show waiting locks
            var waitingLocks = lockInfo.stream().filter(l -> !l.isGranted()).limit(10).toList();
            if (!waitingLocks.isEmpty()) {
                sb.append("\n  Waiting locks:\n");
                for (var lock : waitingLocks) {
                    sb.append(String.format("    PID %d waiting for %s on %s\n",
                            lock.getPid(), lock.getMode(), lock.getRelation()));
                }
            }
        }

        return sb.toString();
    }

    /**
     * Captures table maintenance recommendations.
     */
    private String captureTableMaintenanceData(String instanceName) {
        var recommendations = tableMaintenanceService.getRecommendations(instanceName);
        if (recommendations.isEmpty()) {
            return "=== Table Maintenance ===\nNo maintenance recommendations at this time";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== Table Maintenance Recommendations ===\n\n");

        // Group by severity
        var bySeverity = new LinkedHashMap<String, List<com.bovinemagnet.pgconsole.model.TableMaintenanceRecommendation>>();
        for (var rec : recommendations) {
            String severity = rec.getSeverity() != null ? rec.getSeverity().name() : "UNKNOWN";
            bySeverity.computeIfAbsent(severity, k -> new ArrayList<>()).add(rec);
        }

        for (var entry : bySeverity.entrySet()) {
            sb.append(String.format("--- %s (%d tables) ---\n", entry.getKey(), entry.getValue().size()));
            for (var rec : entry.getValue()) {
                sb.append(String.format("  %s.%s\n", rec.getSchemaName(), rec.getTableName()));
                sb.append(String.format("    Action: %s\n", rec.getTypeDisplay()));
                sb.append(String.format("    Dead tuples: %,d (%.1f%%)\n",
                        rec.getDeadTuples(), rec.getDeadTupleRatio() * 100));
                sb.append(String.format("    Reason: %s\n", rec.getRationale()));
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Captures table statistics.
     */
    private String captureTablesData(String instanceName) {
        var tables = postgresService.getTableStats(instanceName);
        if (tables.isEmpty()) {
            return "=== Table Statistics ===\nNo table statistics available";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== Table Statistics (Top 30 by Size) ===\n\n");

        int count = 0;
        for (var table : tables) {
            if (count >= 30) break;
            count++;

            sb.append(String.format("%s.%s\n", table.getSchemaName(), table.getTableName()));
            sb.append(String.format("  Live rows: %,d | Dead rows: %,d (%.1f%% bloat)\n",
                    table.getnLiveTup(),
                    table.getnDeadTup(),
                    table.getBloatRatio() * 100));
            sb.append(String.format("  Seq scans: %,d | Idx scans: %,d\n",
                    table.getSeqScan(), table.getIdxScan()));
            sb.append("\n");
        }

        return sb.toString();
    }
}
