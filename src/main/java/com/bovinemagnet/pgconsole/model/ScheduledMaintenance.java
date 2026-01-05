package com.bovinemagnet.pgconsole.model;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Set;

/**
 * Represents a scheduled maintenance task for PostgreSQL database operations.
 * <p>
 * This class models maintenance tasks such as VACUUM, ANALYSE, REINDEX, and CLUSTER
 * operations that can be scheduled to run against specific tables or entire schemas.
 * It supports three scheduling strategies:
 * <ul>
 * <li><strong>Intelligent scheduling</strong> - Runs tasks automatically when database
 * activity falls below a configurable threshold, optionally within preferred time windows</li>
 * <li><strong>Cron-based scheduling</strong> - Runs tasks at fixed intervals using standard
 * cron expressions</li>
 * <li><strong>One-time execution</strong> - Runs a task once at a specified time</li>
 * </ul>
 * <p>
 * Each maintenance task can be configured with execution limits such as maximum duration,
 * priority level, and constraints on table size or minimum interval between runs. The class
 * tracks execution history including last run time, duration, and status.
 * <p>
 * Example usage for intelligent scheduling:
 * <pre>{@code
 * ScheduledMaintenance task = new ScheduledMaintenance();
 * task.setName("Nightly vacuum");
 * task.setTaskType(TaskType.VACUUM);
 * task.setTargetObject("users");
 * task.setTargetSchema("public");
 * task.setScheduleType(ScheduleType.INTELLIGENT);
 * task.setActivityThreshold(20.0); // Run when activity < 20%
 * task.setPreferredWindowStart(LocalTime.of(2, 0));
 * task.setPreferredWindowEnd(LocalTime.of(6, 0));
 * }</pre>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see TaskType
 * @see ScheduleType
 */
public class ScheduledMaintenance {

    /**
     * Enumeration of PostgreSQL maintenance task types.
     * <p>
     * Each task type corresponds to a specific PostgreSQL maintenance operation with
     * different characteristics regarding locking, duration, and impact on the database.
     *
     * @see <a href="https://www.postgresql.org/docs/current/routine-vacuuming.html">PostgreSQL Routine Vacuuming</a>
     */
    public enum TaskType {
        /**
         * Standard VACUUM operation to remove dead tuples and free space for reuse.
         * Does not require exclusive locks and can run concurrently with normal operations.
         */
        VACUUM("VACUUM", "bi-wind", "Remove dead tuples and free space"),

        /**
         * VACUUM FULL operation to reclaim disk space by rewriting the entire table.
         * Requires an exclusive lock on the table and is typically much slower than standard VACUUM.
         */
        VACUUM_FULL("VACUUM FULL", "bi-wind", "Reclaim disk space (locks table)"),

        /**
         * ANALYSE operation to update table statistics for the query planner.
         * Uses a brief exclusive lock but is generally quick and has minimal impact.
         */
        ANALYSE("ANALYSE", "bi-bar-chart", "Update table statistics"),

        /**
         * REINDEX operation to rebuild indexes and remove bloat.
         * Locks out writes during execution but can significantly improve index performance.
         */
        REINDEX("REINDEX", "bi-list-ul", "Rebuild indexes"),

        /**
         * CLUSTER operation to physically reorder table data according to an index.
         * Requires an exclusive lock and rewrites the entire table, similar to VACUUM FULL.
         */
        CLUSTER("CLUSTER", "bi-layers", "Reorder table data by index");

        private final String displayName;
        private final String icon;
        private final String description;

        /**
         * Constructs a TaskType with display properties.
         *
         * @param displayName the human-readable name of the task
         * @param icon the Bootstrap icon class name for UI representation
         * @param description a brief description of what the task does
         */
        TaskType(String displayName, String icon, String description) {
            this.displayName = displayName;
            this.icon = icon;
            this.description = description;
        }

        /**
         * Returns the human-readable display name for this task type.
         *
         * @return the display name (e.g., "VACUUM", "VACUUM FULL")
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Returns the Bootstrap icon class name for this task type.
         *
         * @return the icon class name (e.g., "bi-wind", "bi-bar-chart")
         */
        public String getIcon() {
            return icon;
        }

        /**
         * Returns a brief description of what this task type does.
         *
         * @return the task description
         */
        public String getDescription() {
            return description;
        }
    }

    /**
     * Enumeration of scheduling strategies for maintenance tasks.
     * <p>
     * Defines how and when a maintenance task should be executed, ranging from
     * intelligent activity-based scheduling to fixed time-based execution.
     */
    public enum ScheduleType {
        /**
         * Intelligent scheduling based on database activity patterns.
         * <p>
         * Tasks are executed when database activity falls below a configured threshold,
         * optionally within preferred time windows and on specific days of the week.
         * This strategy minimises the impact on production workloads by running
         * maintenance during quiet periods.
         */
        INTELLIGENT("Intelligent", "Runs when activity is low"),

        /**
         * Fixed scheduling using cron expressions.
         * <p>
         * Tasks are executed at predictable times defined by a standard cron expression.
         * This provides deterministic scheduling but does not account for current database
         * activity levels.
         */
        CRON("Cron", "Runs on a fixed schedule"),

        /**
         * One-time execution at a specific timestamp.
         * <p>
         * The task is executed once at the specified time and is not repeated.
         * Useful for ad-hoc maintenance operations or scheduled one-off interventions.
         */
        ONE_TIME("One-Time", "Runs once at specified time");

        private final String displayName;
        private final String description;

        /**
         * Constructs a ScheduleType with display properties.
         *
         * @param displayName the human-readable name of the schedule type
         * @param description a brief description of the scheduling behaviour
         */
        ScheduleType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        /**
         * Returns the human-readable display name for this schedule type.
         *
         * @return the display name (e.g., "Intelligent", "Cron", "One-Time")
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Returns a brief description of the scheduling behaviour.
         *
         * @return the schedule type description
         */
        public String getDescription() {
            return description;
        }
    }

    /**
     * Unique identifier for this scheduled maintenance task.
     */
    private Long id;

    /**
     * Identifier of the PostgreSQL instance this task is associated with.
     * Allows tasks to be scoped to specific database instances in multi-instance deployments.
     */
    private String instanceId;

    // Task details

    /**
     * Human-readable name for this maintenance task.
     */
    private String name;

    /**
     * Optional detailed description explaining the purpose and scope of this task.
     */
    private String description;

    /**
     * The type of maintenance operation to perform.
     *
     * @see TaskType
     */
    private TaskType taskType;

    /**
     * The target database object (typically a table name).
     * Use "*" to target all tables in the schema.
     */
    private String targetObject;

    /**
     * The schema containing the target object.
     * Defaults to "public" if not specified.
     */
    private String targetSchema;

    // Scheduling

    /**
     * The scheduling strategy for this task.
     * Defaults to {@link ScheduleType#INTELLIGENT}.
     *
     * @see ScheduleType
     */
    private ScheduleType scheduleType;

    /**
     * Cron expression for {@link ScheduleType#CRON} scheduling.
     * <p>
     * Standard cron format: "minute hour day-of-month month day-of-week"
     * Example: "0 2 * * *" runs daily at 2:00 AM
     */
    private String cronExpression;

    /**
     * Specific timestamp for {@link ScheduleType#ONE_TIME} execution.
     * The task will run once at this time and then not repeat.
     */
    private Instant scheduledTime;

    // Intelligent scheduling parameters

    /**
     * Activity threshold percentage for {@link ScheduleType#INTELLIGENT} scheduling.
     * <p>
     * The task will only run when database activity falls below this percentage.
     * For example, 20.0 means the task runs when activity is below 20%.
     * If null, a default threshold is used.
     */
    private Double activityThreshold;

    /**
     * Start of the preferred time window for {@link ScheduleType#INTELLIGENT} scheduling.
     * <p>
     * If specified along with {@link #preferredWindowEnd}, the task will only
     * run within this daily time range. If null, no time window constraint is applied.
     */
    private LocalTime preferredWindowStart;

    /**
     * End of the preferred time window for {@link ScheduleType#INTELLIGENT} scheduling.
     * <p>
     * Must be specified together with {@link #preferredWindowStart}.
     */
    private LocalTime preferredWindowEnd;

    /**
     * Days of the week when this task is allowed to run for {@link ScheduleType#INTELLIGENT} scheduling.
     * <p>
     * If null or empty, the task can run on any day of the week.
     */
    private Set<DayOfWeek> preferredDays;

    /**
     * Minimum interval in hours between consecutive executions.
     * <p>
     * Prevents the task from running too frequently, even if activity conditions are met.
     * If null, no minimum interval is enforced.
     */
    private Integer minIntervalHours;

    /**
     * Maximum table size in gigabytes for this task to target.
     * <p>
     * If specified, the task will only run on tables smaller than this size.
     * Useful for avoiding long-running operations on very large tables.
     * If null, no size constraint is applied.
     */
    private Double maxTableSizeGb;

    // Execution limits

    /**
     * Maximum duration in minutes that this task is allowed to run.
     * <p>
     * If the task exceeds this duration, it may be cancelled or flagged.
     * Defaults to 60 minutes if not specified.
     */
    private Integer maxDurationMinutes;

    /**
     * Priority level for this task (typically 1-10, where higher numbers indicate higher priority).
     * <p>
     * When multiple tasks are eligible to run, higher priority tasks are executed first.
     * Defaults to 5.
     */
    private int priority;

    // State

    /**
     * Whether this task is currently enabled and eligible for scheduling.
     * <p>
     * Disabled tasks are not executed but remain in the system for future re-enablement.
     * Defaults to true.
     */
    private boolean enabled;

    /**
     * Timestamp of the last successful or attempted execution of this task.
     * Null if the task has never run.
     */
    private Instant lastRunAt;

    /**
     * Calculated timestamp for the next scheduled execution.
     * <p>
     * For {@link ScheduleType#CRON}, this is calculated from the cron expression.
     * For {@link ScheduleType#INTELLIGENT}, this may be null or represent an estimated next run time.
     * For {@link ScheduleType#ONE_TIME}, this equals {@link #scheduledTime} until executed.
     */
    private Instant nextRunAt;

    /**
     * Duration in milliseconds of the last execution.
     * Null if the task has never completed.
     */
    private Long lastRunDurationMs;

    /**
     * Status message or result from the last execution.
     * <p>
     * May contain success confirmation, error messages, or execution statistics.
     * Null if the task has never run.
     */
    private String lastRunStatus;

    // Metadata

    /**
     * Timestamp when this task was created.
     */
    private Instant createdAt;

    /**
     * Timestamp when this task was last modified.
     */
    private Instant updatedAt;

    /**
     * Username or identifier of the user who created this task.
     */
    private String createdBy;

    /**
     * Constructs a new ScheduledMaintenance instance with sensible defaults.
     * <p>
     * Default values:
     * <ul>
     * <li>{@link #scheduleType} = {@link ScheduleType#INTELLIGENT}</li>
     * <li>{@link #targetSchema} = "public"</li>
     * <li>{@link #enabled} = true</li>
     * <li>{@link #priority} = 5 (medium priority)</li>
     * <li>{@link #maxDurationMinutes} = 60 (one hour)</li>
     * <li>{@link #createdAt} and {@link #updatedAt} = current timestamp</li>
     * </ul>
     */
    public ScheduledMaintenance() {
        this.scheduleType = ScheduleType.INTELLIGENT;
        this.targetSchema = "public";
        this.enabled = true;
        this.priority = 5;
        this.maxDurationMinutes = 60;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Checks if this task targets all tables in the schema.
     * <p>
     * A task targets all tables when {@link #targetObject} is set to "*".
     *
     * @return true if this task targets all tables, false otherwise
     */
    public boolean isAllTables() {
        return "*".equals(targetObject);
    }

    /**
     * Returns a human-readable display name for the target of this task.
     * <p>
     * For tasks targeting all tables, returns "All tables in {schema}".
     * For specific tables, returns "{schema}.{table}".
     *
     * @return the formatted target display name
     */
    public String getTargetDisplay() {
        if (isAllTables()) {
            return "All tables in " + targetSchema;
        }
        return targetSchema + "." + targetObject;
    }

    /**
     * Generates the PostgreSQL SQL command that will be executed for this task.
     * <p>
     * The generated command depends on the {@link #taskType} and whether the task
     * targets all tables or a specific table. Examples:
     * <ul>
     * <li>VACUUM on specific table: "VACUUM public.users"</li>
     * <li>VACUUM on all tables: "VACUUM"</li>
     * <li>REINDEX on specific table: "REINDEX TABLE public.users"</li>
     * <li>REINDEX on all tables: "REINDEX DATABASE"</li>
     * </ul>
     *
     * @return the SQL command string ready for execution
     */
    public String getSqlCommand() {
        String target = isAllTables() ? "" : " " + targetSchema + "." + targetObject;
        return switch (taskType) {
            case VACUUM -> "VACUUM" + target;
            case VACUUM_FULL -> "VACUUM FULL" + target;
            case ANALYSE -> "ANALYSE" + target;
            case REINDEX -> isAllTables() ? "REINDEX DATABASE" : "REINDEX TABLE " + targetSchema + "." + targetObject;
            case CLUSTER -> isAllTables() ? "CLUSTER" : "CLUSTER " + targetSchema + "." + targetObject;
        };
    }

    /**
     * Returns a human-readable description of this task's scheduling configuration.
     * <p>
     * The description format depends on the {@link #scheduleType}:
     * <ul>
     * <li><strong>INTELLIGENT:</strong> "When activity &lt; X% between HH:MM - HH:MM" or
     * "When activity &lt; X%" if no time window is specified</li>
     * <li><strong>CRON:</strong> "Cron: {expression}"</li>
     * <li><strong>ONE_TIME:</strong> "At {timestamp}" or "Not scheduled" if no time is set</li>
     * </ul>
     *
     * @return the formatted schedule description
     */
    public String getScheduleDescription() {
        return switch (scheduleType) {
            case INTELLIGENT -> {
                if (preferredWindowStart != null && preferredWindowEnd != null) {
                    yield "When activity < " + (activityThreshold != null ? activityThreshold.intValue() : 20)
                            + "% between " + preferredWindowStart + " - " + preferredWindowEnd;
                }
                yield "When activity < " + (activityThreshold != null ? activityThreshold.intValue() : 20) + "%";
            }
            case CRON -> "Cron: " + cronExpression;
            case ONE_TIME -> scheduledTime != null ? "At " + scheduledTime : "Not scheduled";
        };
    }

    // Getters and setters

    /**
     * Returns the unique identifier for this scheduled maintenance task.
     *
     * @return the task ID, or null if not yet persisted
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the unique identifier for this scheduled maintenance task.
     *
     * @param id the task ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns the PostgreSQL instance identifier this task is associated with.
     *
     * @return the instance ID, or null if not scoped to a specific instance
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * Sets the PostgreSQL instance identifier this task is associated with.
     *
     * @param instanceId the instance ID
     */
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    /**
     * Returns the type of maintenance operation to perform.
     *
     * @return the task type
     * @see TaskType
     */
    public TaskType getTaskType() {
        return taskType;
    }

    /**
     * Sets the type of maintenance operation to perform.
     *
     * @param taskType the task type
     * @see TaskType
     */
    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    /**
     * Returns the target database object (table name or "*" for all tables).
     *
     * @return the target object name
     */
    public String getTargetObject() {
        return targetObject;
    }

    /**
     * Sets the target database object (table name or "*" for all tables).
     *
     * @param targetObject the target object name
     */
    public void setTargetObject(String targetObject) {
        this.targetObject = targetObject;
    }

    /**
     * Returns the schema containing the target object.
     *
     * @return the target schema name
     */
    public String getTargetSchema() {
        return targetSchema;
    }

    /**
     * Sets the schema containing the target object.
     *
     * @param targetSchema the target schema name
     */
    public void setTargetSchema(String targetSchema) {
        this.targetSchema = targetSchema;
    }

    /**
     * Returns the scheduling strategy for this task.
     *
     * @return the schedule type
     * @see ScheduleType
     */
    public ScheduleType getScheduleType() {
        return scheduleType;
    }

    /**
     * Sets the scheduling strategy for this task.
     *
     * @param scheduleType the schedule type
     * @see ScheduleType
     */
    public void setScheduleType(ScheduleType scheduleType) {
        this.scheduleType = scheduleType;
    }

    /**
     * Returns the cron expression for CRON-based scheduling.
     *
     * @return the cron expression, or null if not using CRON scheduling
     */
    public String getCronExpression() {
        return cronExpression;
    }

    /**
     * Sets the cron expression for CRON-based scheduling.
     *
     * @param cronExpression the cron expression in standard format
     */
    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    /**
     * Returns the specific timestamp for ONE_TIME execution.
     *
     * @return the scheduled time, or null if not using ONE_TIME scheduling
     */
    public Instant getScheduledTime() {
        return scheduledTime;
    }

    /**
     * Sets the specific timestamp for ONE_TIME execution.
     *
     * @param scheduledTime the scheduled time
     */
    public void setScheduledTime(Instant scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    /**
     * Returns the activity threshold percentage for intelligent scheduling.
     *
     * @return the activity threshold, or null to use the default
     */
    public Double getActivityThreshold() {
        return activityThreshold;
    }

    /**
     * Sets the activity threshold percentage for intelligent scheduling.
     *
     * @param activityThreshold the activity threshold (e.g., 20.0 for 20%)
     */
    public void setActivityThreshold(Double activityThreshold) {
        this.activityThreshold = activityThreshold;
    }

    /**
     * Returns the start of the preferred time window for intelligent scheduling.
     *
     * @return the window start time, or null if no time window is specified
     */
    public LocalTime getPreferredWindowStart() {
        return preferredWindowStart;
    }

    /**
     * Sets the start of the preferred time window for intelligent scheduling.
     *
     * @param preferredWindowStart the window start time
     */
    public void setPreferredWindowStart(LocalTime preferredWindowStart) {
        this.preferredWindowStart = preferredWindowStart;
    }

    /**
     * Returns the end of the preferred time window for intelligent scheduling.
     *
     * @return the window end time, or null if no time window is specified
     */
    public LocalTime getPreferredWindowEnd() {
        return preferredWindowEnd;
    }

    /**
     * Sets the end of the preferred time window for intelligent scheduling.
     *
     * @param preferredWindowEnd the window end time
     */
    public void setPreferredWindowEnd(LocalTime preferredWindowEnd) {
        this.preferredWindowEnd = preferredWindowEnd;
    }

    /**
     * Returns the maximum duration in minutes for task execution.
     *
     * @return the maximum duration in minutes
     */
    public Integer getMaxDurationMinutes() {
        return maxDurationMinutes;
    }

    /**
     * Sets the maximum duration in minutes for task execution.
     *
     * @param maxDurationMinutes the maximum duration in minutes
     */
    public void setMaxDurationMinutes(Integer maxDurationMinutes) {
        this.maxDurationMinutes = maxDurationMinutes;
    }

    /**
     * Returns the priority level for this task.
     *
     * @return the priority (typically 1-10, higher is more important)
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Sets the priority level for this task.
     *
     * @param priority the priority (typically 1-10, higher is more important)
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * Returns whether this task is currently enabled.
     *
     * @return true if enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether this task is currently enabled.
     *
     * @param enabled true to enable, false to disable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns the timestamp of the last execution.
     *
     * @return the last run time, or null if never run
     */
    public Instant getLastRunAt() {
        return lastRunAt;
    }

    /**
     * Sets the timestamp of the last execution.
     *
     * @param lastRunAt the last run time
     */
    public void setLastRunAt(Instant lastRunAt) {
        this.lastRunAt = lastRunAt;
    }

    /**
     * Returns the calculated timestamp for the next scheduled execution.
     *
     * @return the next run time, or null if not yet calculated
     */
    public Instant getNextRunAt() {
        return nextRunAt;
    }

    /**
     * Sets the calculated timestamp for the next scheduled execution.
     *
     * @param nextRunAt the next run time
     */
    public void setNextRunAt(Instant nextRunAt) {
        this.nextRunAt = nextRunAt;
    }

    /**
     * Returns the timestamp when this task was created.
     *
     * @return the creation time
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the timestamp when this task was created.
     *
     * @param createdAt the creation time
     */
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Returns the timestamp when this task was last modified.
     *
     * @return the last update time
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the timestamp when this task was last modified.
     *
     * @param updatedAt the last update time
     */
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Returns the username or identifier of the user who created this task.
     *
     * @return the creator identifier, or null if not recorded
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Sets the username or identifier of the user who created this task.
     *
     * @param createdBy the creator identifier
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Returns the human-readable name for this maintenance task.
     *
     * @return the task name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the human-readable name for this maintenance task.
     *
     * @param name the task name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the optional detailed description of this task.
     *
     * @return the task description, or null if not provided
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the optional detailed description of this task.
     *
     * @param description the task description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the target table name (alias for {@link #getTargetObject()}).
     *
     * @return the target table name
     */
    public String getTargetTable() {
        return targetObject;
    }

    /**
     * Sets the target table name (alias for {@link #setTargetObject(String)}).
     *
     * @param targetTable the target table name
     */
    public void setTargetTable(String targetTable) {
        this.targetObject = targetTable;
    }

    /**
     * Returns the days of the week when this task is allowed to run.
     *
     * @return the set of preferred days, or null if no day restriction
     */
    public Set<DayOfWeek> getPreferredDays() {
        return preferredDays;
    }

    /**
     * Sets the days of the week when this task is allowed to run.
     *
     * @param preferredDays the set of preferred days, or null for no restriction
     */
    public void setPreferredDays(Set<DayOfWeek> preferredDays) {
        this.preferredDays = preferredDays;
    }

    /**
     * Returns the minimum interval in hours between consecutive executions.
     *
     * @return the minimum interval in hours, or null if no minimum
     */
    public Integer getMinIntervalHours() {
        return minIntervalHours;
    }

    /**
     * Sets the minimum interval in hours between consecutive executions.
     *
     * @param minIntervalHours the minimum interval in hours
     */
    public void setMinIntervalHours(Integer minIntervalHours) {
        this.minIntervalHours = minIntervalHours;
    }

    /**
     * Returns the maximum table size in gigabytes for this task to target.
     *
     * @return the maximum table size in GB, or null if no size limit
     */
    public Double getMaxTableSizeGb() {
        return maxTableSizeGb;
    }

    /**
     * Sets the maximum table size in gigabytes for this task to target.
     *
     * @param maxTableSizeGb the maximum table size in GB
     */
    public void setMaxTableSizeGb(Double maxTableSizeGb) {
        this.maxTableSizeGb = maxTableSizeGb;
    }

    /**
     * Returns the duration in milliseconds of the last execution.
     *
     * @return the last run duration in milliseconds, or null if never completed
     */
    public Long getLastRunDurationMs() {
        return lastRunDurationMs;
    }

    /**
     * Sets the duration in milliseconds of the last execution.
     *
     * @param lastRunDurationMs the last run duration in milliseconds
     */
    public void setLastRunDurationMs(Long lastRunDurationMs) {
        this.lastRunDurationMs = lastRunDurationMs;
    }

    /**
     * Returns the status message or result from the last execution.
     *
     * @return the last run status, or null if never run
     */
    public String getLastRunStatus() {
        return lastRunStatus;
    }

    /**
     * Sets the status message or result from the last execution.
     *
     * @param lastRunStatus the last run status
     */
    public void setLastRunStatus(String lastRunStatus) {
        this.lastRunStatus = lastRunStatus;
    }
}
