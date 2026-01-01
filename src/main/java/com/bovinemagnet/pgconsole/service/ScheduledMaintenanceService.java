package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.ScheduledMaintenance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import javax.sql.DataSource;
import java.sql.*;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.*;

/**
 * Service for managing scheduled maintenance tasks.
 * <p>
 * Provides intelligent scheduling based on activity patterns,
 * cron-based scheduling, and one-time maintenance execution.
 * Monitors table bloat, dead tuples, and index health to determine
 * optimal maintenance windows.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class ScheduledMaintenanceService {

    private static final Logger LOG = Logger.getLogger(ScheduledMaintenanceService.class);

    @Inject
    DataSourceManager dataSourceManager;

    /**
     * Get all scheduled maintenance tasks.
     *
     * @param instanceName the PostgreSQL instance name
     * @return list of scheduled maintenance tasks
     */
    public List<ScheduledMaintenance> getScheduledTasks(String instanceName) {
        List<ScheduledMaintenance> tasks = new ArrayList<>();

        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            String sql = """
                SELECT id, name, description, task_type, target_schema, target_table,
                       schedule_type, cron_expression, preferred_window_start, preferred_window_end,
                       preferred_days, min_interval_hours, max_table_size_gb, enabled,
                       last_run_at, next_run_at, last_run_duration_ms, last_run_status,
                       created_at, created_by
                FROM pgconsole.scheduled_maintenance
                ORDER BY next_run_at NULLS LAST, name
                """;

            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    tasks.add(mapScheduledMaintenance(rs));
                }
            }

        } catch (SQLException e) {
            LOG.debugf(e, "Error getting scheduled tasks");
        }

        return tasks;
    }

    /**
     * Get a specific scheduled maintenance task.
     *
     * @param instanceName the PostgreSQL instance name
     * @param taskId the task ID
     * @return the task, or null if not found
     */
    public ScheduledMaintenance getTask(String instanceName, long taskId) {
        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            String sql = """
                SELECT id, name, description, task_type, target_schema, target_table,
                       schedule_type, cron_expression, preferred_window_start, preferred_window_end,
                       preferred_days, min_interval_hours, max_table_size_gb, enabled,
                       last_run_at, next_run_at, last_run_duration_ms, last_run_status,
                       created_at, created_by
                FROM pgconsole.scheduled_maintenance
                WHERE id = ?
                """;

            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setLong(1, taskId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapScheduledMaintenance(rs);
                    }
                }
            }

        } catch (SQLException e) {
            LOG.debugf(e, "Error getting task %d", taskId);
        }

        return null;
    }

    /**
     * Get tasks that are due to run.
     *
     * @param instanceName the PostgreSQL instance name
     * @return list of due tasks
     */
    public List<ScheduledMaintenance> getDueTasks(String instanceName) {
        List<ScheduledMaintenance> tasks = new ArrayList<>();

        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            String sql = """
                SELECT id, name, description, task_type, target_schema, target_table,
                       schedule_type, cron_expression, preferred_window_start, preferred_window_end,
                       preferred_days, min_interval_hours, max_table_size_gb, enabled,
                       last_run_at, next_run_at, last_run_duration_ms, last_run_status,
                       created_at, created_by
                FROM pgconsole.scheduled_maintenance
                WHERE enabled = true
                  AND (next_run_at IS NULL OR next_run_at <= NOW())
                ORDER BY next_run_at NULLS FIRST
                """;

            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    tasks.add(mapScheduledMaintenance(rs));
                }
            }

        } catch (SQLException e) {
            LOG.debugf(e, "Error getting due tasks");
        }

        return tasks;
    }

    /**
     * Create a new scheduled maintenance task.
     *
     * @param instanceName the PostgreSQL instance name
     * @param task the task to create
     * @return the created task with ID
     */
    public ScheduledMaintenance createTask(String instanceName, ScheduledMaintenance task) {
        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            String sql = """
                INSERT INTO pgconsole.scheduled_maintenance
                    (name, description, task_type, target_schema, target_table,
                     schedule_type, cron_expression, preferred_window_start, preferred_window_end,
                     preferred_days, min_interval_hours, max_table_size_gb, enabled,
                     next_run_at, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """;

            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, task.getName());
                stmt.setString(2, task.getDescription());
                stmt.setString(3, task.getTaskType() != null ? task.getTaskType().name() : null);
                stmt.setString(4, task.getTargetSchema());
                stmt.setString(5, task.getTargetTable());
                stmt.setString(6, task.getScheduleType() != null ? task.getScheduleType().name() : null);
                stmt.setString(7, task.getCronExpression());
                stmt.setObject(8, task.getPreferredWindowStart());
                stmt.setObject(9, task.getPreferredWindowEnd());
                stmt.setString(10, formatDays(task.getPreferredDays()));
                stmt.setObject(11, task.getMinIntervalHours());
                stmt.setObject(12, task.getMaxTableSizeGb());
                stmt.setBoolean(13, task.isEnabled());
                stmt.setTimestamp(14, task.getNextRunAt() != null ?
                        Timestamp.from(task.getNextRunAt()) : null);
                stmt.setString(15, task.getCreatedBy());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        task.setId(rs.getLong(1));
                    }
                }
            }

        } catch (SQLException e) {
            LOG.errorf(e, "Error creating task");
        }

        return task;
    }

    /**
     * Update a scheduled maintenance task.
     *
     * @param instanceName the PostgreSQL instance name
     * @param task the task to update
     * @return true if updated
     */
    public boolean updateTask(String instanceName, ScheduledMaintenance task) {
        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            String sql = """
                UPDATE pgconsole.scheduled_maintenance
                SET name = ?, description = ?, task_type = ?, target_schema = ?, target_table = ?,
                    schedule_type = ?, cron_expression = ?, preferred_window_start = ?,
                    preferred_window_end = ?, preferred_days = ?, min_interval_hours = ?,
                    max_table_size_gb = ?, enabled = ?, next_run_at = ?
                WHERE id = ?
                """;

            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, task.getName());
                stmt.setString(2, task.getDescription());
                stmt.setString(3, task.getTaskType() != null ? task.getTaskType().name() : null);
                stmt.setString(4, task.getTargetSchema());
                stmt.setString(5, task.getTargetTable());
                stmt.setString(6, task.getScheduleType() != null ? task.getScheduleType().name() : null);
                stmt.setString(7, task.getCronExpression());
                stmt.setObject(8, task.getPreferredWindowStart());
                stmt.setObject(9, task.getPreferredWindowEnd());
                stmt.setString(10, formatDays(task.getPreferredDays()));
                stmt.setObject(11, task.getMinIntervalHours());
                stmt.setObject(12, task.getMaxTableSizeGb());
                stmt.setBoolean(13, task.isEnabled());
                stmt.setTimestamp(14, task.getNextRunAt() != null ?
                        Timestamp.from(task.getNextRunAt()) : null);
                stmt.setLong(15, task.getId());

                return stmt.executeUpdate() > 0;
            }

        } catch (SQLException e) {
            LOG.errorf(e, "Error updating task %d", task.getId());
        }

        return false;
    }

    /**
     * Delete a scheduled maintenance task.
     *
     * @param instanceName the PostgreSQL instance name
     * @param taskId the task ID
     * @return true if deleted
     */
    public boolean deleteTask(String instanceName, long taskId) {
        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            String sql = "DELETE FROM pgconsole.scheduled_maintenance WHERE id = ?";

            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setLong(1, taskId);
                return stmt.executeUpdate() > 0;
            }

        } catch (SQLException e) {
            LOG.errorf(e, "Error deleting task %d", taskId);
        }

        return false;
    }

    /**
     * Enable or disable a task.
     *
     * @param instanceName the PostgreSQL instance name
     * @param taskId the task ID
     * @param enabled whether to enable
     * @return true if updated
     */
    public boolean setEnabled(String instanceName, long taskId, boolean enabled) {
        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            String sql = "UPDATE pgconsole.scheduled_maintenance SET enabled = ? WHERE id = ?";

            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setBoolean(1, enabled);
                stmt.setLong(2, taskId);
                return stmt.executeUpdate() > 0;
            }

        } catch (SQLException e) {
            LOG.errorf(e, "Error updating task %d enabled status", taskId);
        }

        return false;
    }

    /**
     * Execute a maintenance task immediately.
     *
     * @param instanceName the PostgreSQL instance name
     * @param taskId the task ID
     * @return execution result
     */
    public MaintenanceResult executeTask(String instanceName, long taskId) {
        ScheduledMaintenance task = getTask(instanceName, taskId);
        if (task == null) {
            return new MaintenanceResult(false, "Task not found", 0);
        }

        return executeTask(instanceName, task);
    }

    /**
     * Execute a maintenance task.
     *
     * @param instanceName the PostgreSQL instance name
     * @param task the task to execute
     * @return execution result
     */
    public MaintenanceResult executeTask(String instanceName, ScheduledMaintenance task) {
        long startTime = System.currentTimeMillis();
        String status = "SUCCESS";
        String message = "";

        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            String sql = buildMaintenanceSQL(task);
            if (sql == null) {
                return new MaintenanceResult(false, "Unknown task type: " + task.getTaskType(), 0);
            }

            LOG.infof("Executing maintenance task: %s - %s", task.getName(), sql);

            try (Connection conn = ds.getConnection();
                 Statement stmt = conn.createStatement()) {

                stmt.execute(sql);
                message = "Completed successfully";
            }

        } catch (SQLException e) {
            status = "FAILED";
            message = e.getMessage();
            LOG.errorf(e, "Error executing maintenance task %s", task.getName());
        }

        long duration = System.currentTimeMillis() - startTime;

        // Update task status
        updateTaskExecution(instanceName, task.getId(), status, duration);

        return new MaintenanceResult("SUCCESS".equals(status), message, duration);
    }

    /**
     * Get tables that need maintenance based on dead tuples and bloat.
     *
     * @param instanceName the PostgreSQL instance name
     * @param deadTupleThreshold minimum dead tuples to consider
     * @return list of tables needing maintenance
     */
    public List<TableMaintenanceNeed> getTablesNeedingMaintenance(String instanceName, long deadTupleThreshold) {
        List<TableMaintenanceNeed> tables = new ArrayList<>();

        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            String sql = """
                SELECT
                    schemaname,
                    relname,
                    n_dead_tup,
                    n_live_tup,
                    CASE WHEN n_live_tup > 0
                         THEN round((n_dead_tup::numeric / n_live_tup) * 100, 2)
                         ELSE 0 END as dead_ratio,
                    last_vacuum,
                    last_autovacuum,
                    last_analyze,
                    last_autoanalyze
                FROM pg_stat_user_tables
                WHERE n_dead_tup >= ?
                ORDER BY n_dead_tup DESC
                LIMIT 50
                """;

            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setLong(1, deadTupleThreshold);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        TableMaintenanceNeed need = new TableMaintenanceNeed();
                        need.setSchemaName(rs.getString("schemaname"));
                        need.setTableName(rs.getString("relname"));
                        need.setDeadTuples(rs.getLong("n_dead_tup"));
                        need.setLiveTuples(rs.getLong("n_live_tup"));
                        need.setDeadRatio(rs.getDouble("dead_ratio"));

                        Timestamp lastVacuum = rs.getTimestamp("last_vacuum");
                        if (lastVacuum != null) {
                            need.setLastVacuum(lastVacuum.toInstant());
                        }

                        Timestamp lastAutoVacuum = rs.getTimestamp("last_autovacuum");
                        if (lastAutoVacuum != null) {
                            need.setLastAutoVacuum(lastAutoVacuum.toInstant());
                        }

                        Timestamp lastAnalyze = rs.getTimestamp("last_analyze");
                        if (lastAnalyze != null) {
                            need.setLastAnalyze(lastAnalyze.toInstant());
                        }

                        tables.add(need);
                    }
                }
            }

        } catch (SQLException e) {
            LOG.debugf(e, "Error getting tables needing maintenance");
        }

        return tables;
    }

    /**
     * Get the optimal maintenance window based on activity patterns.
     *
     * @param instanceName the PostgreSQL instance name
     * @return recommended window
     */
    public MaintenanceWindow getRecommendedWindow(String instanceName) {
        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            // Find the hour with lowest average activity
            String sql = """
                SELECT
                    EXTRACT(HOUR FROM sampled_at) as hour,
                    EXTRACT(DOW FROM sampled_at) as dow,
                    AVG(total_connections) as avg_connections,
                    AVG(active_queries) as avg_active
                FROM pgconsole.overview_history
                WHERE sampled_at >= NOW() - INTERVAL '7 days'
                GROUP BY EXTRACT(HOUR FROM sampled_at), EXTRACT(DOW FROM sampled_at)
                ORDER BY avg_active, avg_connections
                LIMIT 1
                """;

            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    int hour = rs.getInt("hour");
                    int dow = rs.getInt("dow");
                    double avgConnections = rs.getDouble("avg_connections");
                    double avgActive = rs.getDouble("avg_active");

                    DayOfWeek day = dow == 0 ? DayOfWeek.SUNDAY : DayOfWeek.of(dow);
                    LocalTime startTime = LocalTime.of(hour, 0);
                    LocalTime endTime = LocalTime.of((hour + 2) % 24, 0);

                    return new MaintenanceWindow(day, startTime, endTime, avgConnections, avgActive);
                }
            }

        } catch (SQLException e) {
            LOG.debugf(e, "Error getting recommended maintenance window");
        }

        // Default to Sunday 3am-5am
        return new MaintenanceWindow(
                DayOfWeek.SUNDAY,
                LocalTime.of(3, 0),
                LocalTime.of(5, 0),
                0, 0
        );
    }

    /**
     * Get recent maintenance execution history.
     *
     * @param instanceName the PostgreSQL instance name
     * @param limit maximum number of executions
     * @return list of recent executions
     */
    public List<MaintenanceExecution> getRecentExecutions(String instanceName, int limit) {
        List<MaintenanceExecution> executions = new ArrayList<>();

        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            String sql = """
                SELECT me.id, me.task_id, sm.name as task_name, me.started_at, me.completed_at,
                       me.status, me.duration_ms, me.tables_processed, me.error_message
                FROM pgconsole.maintenance_execution me
                JOIN pgconsole.scheduled_maintenance sm ON sm.id = me.task_id
                ORDER BY me.started_at DESC
                LIMIT ?
                """;

            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, limit);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        MaintenanceExecution exec = new MaintenanceExecution();
                        exec.setId(rs.getLong("id"));
                        exec.setTaskId(rs.getLong("task_id"));
                        exec.setTaskName(rs.getString("task_name"));

                        Timestamp startedAt = rs.getTimestamp("started_at");
                        if (startedAt != null) {
                            exec.setStartedAt(startedAt.toInstant());
                        }

                        Timestamp completedAt = rs.getTimestamp("completed_at");
                        if (completedAt != null) {
                            exec.setCompletedAt(completedAt.toInstant());
                        }

                        exec.setStatus(rs.getString("status"));
                        exec.setDurationMs(rs.getLong("duration_ms"));
                        exec.setTablesProcessed(rs.getInt("tables_processed"));
                        exec.setErrorMessage(rs.getString("error_message"));

                        executions.add(exec);
                    }
                }
            }

        } catch (SQLException e) {
            LOG.debugf(e, "Error getting recent executions");
        }

        return executions;
    }

    // Private helper methods

    private ScheduledMaintenance mapScheduledMaintenance(ResultSet rs) throws SQLException {
        ScheduledMaintenance task = new ScheduledMaintenance();

        task.setId(rs.getLong("id"));
        task.setName(rs.getString("name"));
        task.setDescription(rs.getString("description"));

        String taskType = rs.getString("task_type");
        if (taskType != null) {
            try {
                task.setTaskType(ScheduledMaintenance.TaskType.valueOf(taskType));
            } catch (IllegalArgumentException e) {
                // Ignore unknown type
            }
        }

        task.setTargetSchema(rs.getString("target_schema"));
        task.setTargetTable(rs.getString("target_table"));

        String scheduleType = rs.getString("schedule_type");
        if (scheduleType != null) {
            try {
                task.setScheduleType(ScheduledMaintenance.ScheduleType.valueOf(scheduleType));
            } catch (IllegalArgumentException e) {
                // Ignore unknown type
            }
        }

        task.setCronExpression(rs.getString("cron_expression"));

        Time windowStart = rs.getTime("preferred_window_start");
        if (windowStart != null) {
            task.setPreferredWindowStart(windowStart.toLocalTime());
        }

        Time windowEnd = rs.getTime("preferred_window_end");
        if (windowEnd != null) {
            task.setPreferredWindowEnd(windowEnd.toLocalTime());
        }

        task.setPreferredDays(parseDays(rs.getString("preferred_days")));
        task.setMinIntervalHours((Integer) rs.getObject("min_interval_hours"));
        task.setMaxTableSizeGb((Double) rs.getObject("max_table_size_gb"));
        task.setEnabled(rs.getBoolean("enabled"));

        Timestamp lastRunAt = rs.getTimestamp("last_run_at");
        if (lastRunAt != null) {
            task.setLastRunAt(lastRunAt.toInstant());
        }

        Timestamp nextRunAt = rs.getTimestamp("next_run_at");
        if (nextRunAt != null) {
            task.setNextRunAt(nextRunAt.toInstant());
        }

        task.setLastRunDurationMs(rs.getLong("last_run_duration_ms"));
        task.setLastRunStatus(rs.getString("last_run_status"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            task.setCreatedAt(createdAt.toInstant());
        }

        task.setCreatedBy(rs.getString("created_by"));

        return task;
    }

    private String buildMaintenanceSQL(ScheduledMaintenance task) {
        if (task.getTaskType() == null) {
            return null;
        }

        String target = task.getTargetTable() != null ?
                String.format("\"%s\".\"%s\"", task.getTargetSchema(), task.getTargetTable()) :
                null;

        return switch (task.getTaskType()) {
            case VACUUM -> target != null ? "VACUUM " + target : "VACUUM";
            case VACUUM_FULL -> target != null ? "VACUUM FULL " + target : "VACUUM FULL";
            case ANALYSE -> target != null ? "ANALYZE " + target : "ANALYZE";
            case REINDEX -> {
                if (target != null) {
                    yield "REINDEX TABLE " + target;
                } else if (task.getTargetSchema() != null) {
                    yield String.format("REINDEX SCHEMA \"%s\"", task.getTargetSchema());
                } else {
                    yield "REINDEX DATABASE";
                }
            }
            case CLUSTER -> target != null ? "CLUSTER " + target : null;
        };
    }

    private void updateTaskExecution(String instanceName, long taskId, String status, long durationMs) {
        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            // Update the task's last run info
            String updateSql = """
                UPDATE pgconsole.scheduled_maintenance
                SET last_run_at = NOW(),
                    last_run_duration_ms = ?,
                    last_run_status = ?,
                    next_run_at = CASE
                        WHEN schedule_type = 'ONE_TIME' THEN NULL
                        ELSE NOW() + (min_interval_hours || ' hours')::INTERVAL
                    END
                WHERE id = ?
                """;

            // Insert execution record
            String insertSql = """
                INSERT INTO pgconsole.maintenance_execution
                    (task_id, started_at, completed_at, status, duration_ms)
                VALUES (?, NOW() - (? || ' milliseconds')::INTERVAL, NOW(), ?, ?)
                """;

            try (Connection conn = ds.getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                    stmt.setLong(1, durationMs);
                    stmt.setString(2, status);
                    stmt.setLong(3, taskId);
                    stmt.executeUpdate();
                }

                try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                    stmt.setLong(1, taskId);
                    stmt.setLong(2, durationMs);
                    stmt.setString(3, status);
                    stmt.setLong(4, durationMs);
                    stmt.executeUpdate();
                }
            }

        } catch (SQLException e) {
            LOG.errorf(e, "Error updating task execution status");
        }
    }

    private String formatDays(Set<DayOfWeek> days) {
        if (days == null || days.isEmpty()) {
            return null;
        }
        return days.stream()
                .map(DayOfWeek::name)
                .sorted()
                .reduce((a, b) -> a + "," + b)
                .orElse(null);
    }

    private Set<DayOfWeek> parseDays(String daysStr) {
        if (daysStr == null || daysStr.isBlank()) {
            return Set.of();
        }

        Set<DayOfWeek> days = new HashSet<>();
        for (String day : daysStr.split(",")) {
            try {
                days.add(DayOfWeek.valueOf(day.trim()));
            } catch (IllegalArgumentException e) {
                // Ignore invalid day
            }
        }
        return days;
    }

    /**
     * Result of a maintenance operation.
     */
    public record MaintenanceResult(boolean success, String message, long durationMs) {}

    /**
     * Table needing maintenance.
     */
    public static class TableMaintenanceNeed {
        private String schemaName;
        private String tableName;
        private long deadTuples;
        private long liveTuples;
        private double deadRatio;
        private Instant lastVacuum;
        private Instant lastAutoVacuum;
        private Instant lastAnalyze;

        public String getSchemaName() { return schemaName; }
        public void setSchemaName(String schemaName) { this.schemaName = schemaName; }
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public long getDeadTuples() { return deadTuples; }
        public void setDeadTuples(long deadTuples) { this.deadTuples = deadTuples; }
        public long getLiveTuples() { return liveTuples; }
        public void setLiveTuples(long liveTuples) { this.liveTuples = liveTuples; }
        public double getDeadRatio() { return deadRatio; }
        public void setDeadRatio(double deadRatio) { this.deadRatio = deadRatio; }
        public Instant getLastVacuum() { return lastVacuum; }
        public void setLastVacuum(Instant lastVacuum) { this.lastVacuum = lastVacuum; }
        public Instant getLastAutoVacuum() { return lastAutoVacuum; }
        public void setLastAutoVacuum(Instant lastAutoVacuum) { this.lastAutoVacuum = lastAutoVacuum; }
        public Instant getLastAnalyze() { return lastAnalyze; }
        public void setLastAnalyze(Instant lastAnalyze) { this.lastAnalyze = lastAnalyze; }

        public String getFullName() {
            return schemaName + "." + tableName;
        }
    }

    /**
     * Recommended maintenance window.
     */
    public record MaintenanceWindow(
            DayOfWeek day,
            LocalTime startTime,
            LocalTime endTime,
            double avgConnections,
            double avgActive
    ) {}

    /**
     * Maintenance execution record.
     */
    public static class MaintenanceExecution {
        private long id;
        private long taskId;
        private String taskName;
        private Instant startedAt;
        private Instant completedAt;
        private String status;
        private long durationMs;
        private int tablesProcessed;
        private String errorMessage;

        public long getId() { return id; }
        public void setId(long id) { this.id = id; }
        public long getTaskId() { return taskId; }
        public void setTaskId(long taskId) { this.taskId = taskId; }
        public String getTaskName() { return taskName; }
        public void setTaskName(String taskName) { this.taskName = taskName; }
        public Instant getStartedAt() { return startedAt; }
        public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
        public Instant getCompletedAt() { return completedAt; }
        public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public long getDurationMs() { return durationMs; }
        public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
        public int getTablesProcessed() { return tablesProcessed; }
        public void setTablesProcessed(int tablesProcessed) { this.tablesProcessed = tablesProcessed; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}
