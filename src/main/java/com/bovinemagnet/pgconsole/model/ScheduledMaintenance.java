package com.bovinemagnet.pgconsole.model;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Set;

/**
 * Represents a scheduled maintenance task.
 * <p>
 * Supports intelligent scheduling based on activity patterns,
 * cron-based scheduling, and one-time execution.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class ScheduledMaintenance {

    /**
     * Type of maintenance task.
     */
    public enum TaskType {
        VACUUM("VACUUM", "bi-wind", "Remove dead tuples and free space"),
        VACUUM_FULL("VACUUM FULL", "bi-wind", "Reclaim disk space (locks table)"),
        ANALYSE("ANALYSE", "bi-bar-chart", "Update table statistics"),
        REINDEX("REINDEX", "bi-list-ul", "Rebuild indexes"),
        CLUSTER("CLUSTER", "bi-layers", "Reorder table data by index");

        private final String displayName;
        private final String icon;
        private final String description;

        TaskType(String displayName, String icon, String description) {
            this.displayName = displayName;
            this.icon = icon;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getIcon() {
            return icon;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Type of scheduling.
     */
    public enum ScheduleType {
        INTELLIGENT("Intelligent", "Runs when activity is low"),
        CRON("Cron", "Runs on a fixed schedule"),
        ONE_TIME("One-Time", "Runs once at specified time");

        private final String displayName;
        private final String description;

        ScheduleType(String displayName, String description) {
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

    private Long id;
    private String instanceId;

    // Task details
    private String name;
    private String description;
    private TaskType taskType;
    private String targetObject;    // Table name or '*' for all
    private String targetSchema;

    // Scheduling
    private ScheduleType scheduleType;
    private String cronExpression;
    private Instant scheduledTime;    // For ONE_TIME

    // Intelligent scheduling parameters
    private Double activityThreshold;    // Run when activity below this %
    private LocalTime preferredWindowStart;
    private LocalTime preferredWindowEnd;
    private Set<DayOfWeek> preferredDays;
    private Integer minIntervalHours;
    private Double maxTableSizeGb;

    // Execution limits
    private Integer maxDurationMinutes;
    private int priority;

    // State
    private boolean enabled;
    private Instant lastRunAt;
    private Instant nextRunAt;
    private Long lastRunDurationMs;
    private String lastRunStatus;

    // Metadata
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;

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
     * Check if this task targets all tables.
     */
    public boolean isAllTables() {
        return "*".equals(targetObject);
    }

    /**
     * Get a display name for the target.
     */
    public String getTargetDisplay() {
        if (isAllTables()) {
            return "All tables in " + targetSchema;
        }
        return targetSchema + "." + targetObject;
    }

    /**
     * Get the SQL command for this task.
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
     * Get a human-readable schedule description.
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
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    public String getTargetObject() {
        return targetObject;
    }

    public void setTargetObject(String targetObject) {
        this.targetObject = targetObject;
    }

    public String getTargetSchema() {
        return targetSchema;
    }

    public void setTargetSchema(String targetSchema) {
        this.targetSchema = targetSchema;
    }

    public ScheduleType getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(ScheduleType scheduleType) {
        this.scheduleType = scheduleType;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public Instant getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(Instant scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public Double getActivityThreshold() {
        return activityThreshold;
    }

    public void setActivityThreshold(Double activityThreshold) {
        this.activityThreshold = activityThreshold;
    }

    public LocalTime getPreferredWindowStart() {
        return preferredWindowStart;
    }

    public void setPreferredWindowStart(LocalTime preferredWindowStart) {
        this.preferredWindowStart = preferredWindowStart;
    }

    public LocalTime getPreferredWindowEnd() {
        return preferredWindowEnd;
    }

    public void setPreferredWindowEnd(LocalTime preferredWindowEnd) {
        this.preferredWindowEnd = preferredWindowEnd;
    }

    public Integer getMaxDurationMinutes() {
        return maxDurationMinutes;
    }

    public void setMaxDurationMinutes(Integer maxDurationMinutes) {
        this.maxDurationMinutes = maxDurationMinutes;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getLastRunAt() {
        return lastRunAt;
    }

    public void setLastRunAt(Instant lastRunAt) {
        this.lastRunAt = lastRunAt;
    }

    public Instant getNextRunAt() {
        return nextRunAt;
    }

    public void setNextRunAt(Instant nextRunAt) {
        this.nextRunAt = nextRunAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTargetTable() {
        return targetObject;
    }

    public void setTargetTable(String targetTable) {
        this.targetObject = targetTable;
    }

    public Set<DayOfWeek> getPreferredDays() {
        return preferredDays;
    }

    public void setPreferredDays(Set<DayOfWeek> preferredDays) {
        this.preferredDays = preferredDays;
    }

    public Integer getMinIntervalHours() {
        return minIntervalHours;
    }

    public void setMinIntervalHours(Integer minIntervalHours) {
        this.minIntervalHours = minIntervalHours;
    }

    public Double getMaxTableSizeGb() {
        return maxTableSizeGb;
    }

    public void setMaxTableSizeGb(Double maxTableSizeGb) {
        this.maxTableSizeGb = maxTableSizeGb;
    }

    public Long getLastRunDurationMs() {
        return lastRunDurationMs;
    }

    public void setLastRunDurationMs(Long lastRunDurationMs) {
        this.lastRunDurationMs = lastRunDurationMs;
    }

    public String getLastRunStatus() {
        return lastRunStatus;
    }

    public void setLastRunStatus(String lastRunStatus) {
        this.lastRunStatus = lastRunStatus;
    }
}
