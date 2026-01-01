package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.Runbook;
import com.bovinemagnet.pgconsole.model.RunbookExecution;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    DataSourceManager dataSourceManager;

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
                       updated_at, created_by, estimated_duration_minutes
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
                       updated_at, created_by, estimated_duration_minutes
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
                       updated_at, created_by, estimated_duration_minutes
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
                       updated_at, created_by, estimated_duration_minutes
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
        Runbook runbook = getRunbook(instanceName, runbookId);
        if (runbook == null) {
            throw new IllegalArgumentException("Runbook not found: " + runbookId);
        }

        RunbookExecution execution = new RunbookExecution();
        execution.setRunbookId(runbookId);
        execution.setRunbook(runbook);
        execution.setInstanceId(instanceName);
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
                SELECT e.id, e.runbook_id, e.instance_id, e.started_at, e.completed_at,
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
                SELECT e.id, e.runbook_id, e.instance_id, e.started_at, e.completed_at,
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
                SELECT e.id, e.runbook_id, e.instance_id, e.started_at, e.completed_at,
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

        return runbook;
    }

    private RunbookExecution mapExecution(ResultSet rs) throws SQLException {
        RunbookExecution execution = new RunbookExecution();
        execution.setId(rs.getLong("id"));
        execution.setRunbookId(rs.getLong("runbook_id"));
        execution.setInstanceId(rs.getString("instance_id"));

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
                LOG.debugf(e, "Error parsing step results JSON");
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
                    (runbook_id, instance_id, started_at, status, triggered_by,
                     current_step, step_results, executed_by)
                VALUES (?, ?, NOW(), ?, ?, ?, ?::jsonb, ?)
                RETURNING id
                """;

            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setLong(1, execution.getRunbookId());
                stmt.setString(2, instanceName);
                stmt.setString(3, execution.getStatus().name());
                stmt.setString(4, execution.getTriggeredBy().name());
                stmt.setInt(5, execution.getCurrentStep());
                stmt.setString(6, objectMapper.writeValueAsString(execution.getStepResults()));
                stmt.setString(7, execution.getExecutedBy());

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
}
