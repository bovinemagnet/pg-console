package com.bovinemagnet.pgconsole.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a maintenance window during which alerts are suppressed.
 * <p>
 * Maintenance windows can be one-time or recurring, and can filter
 * by instance and alert type.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class MaintenanceWindow {

    /**
     * Recurrence patterns for maintenance windows.
     */
    public enum RecurrencePattern {
        NONE("One-time"),
        DAILY("Daily"),
        WEEKLY("Weekly"),
        MONTHLY("Monthly"),
        CUSTOM("Custom (Cron)");

        private final String displayName;

        RecurrencePattern(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private Long id;
    private String name;
    private String description;
    private Instant startTime;
    private Instant endTime;
    private boolean recurring;
    private String recurrencePattern; // DAILY, WEEKLY, MONTHLY, or cron expression
    private List<String> instanceFilter = new ArrayList<>();
    private List<String> alertTypeFilter = new ArrayList<>();
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    public MaintenanceWindow() {
    }

    public MaintenanceWindow(String name, Instant startTime, Instant endTime) {
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * Checks if the maintenance window is currently active.
     *
     * @return true if active now
     */
    public boolean isActiveNow() {
        Instant now = Instant.now();
        return now.isAfter(startTime) && now.isBefore(endTime);
    }

    /**
     * Checks if this window suppresses the given alert.
     *
     * @param instanceName instance name
     * @param alertType alert type
     * @return true if alert should be suppressed
     */
    public boolean suppressesAlert(String instanceName, String alertType) {
        if (!isActiveNow()) return false;

        // Check instance filter
        if (instanceFilter != null && !instanceFilter.isEmpty()) {
            if (instanceName != null && !instanceFilter.contains(instanceName)) {
                return false;
            }
        }

        // Check alert type filter
        if (alertTypeFilter != null && !alertTypeFilter.isEmpty()) {
            if (alertType != null && !alertTypeFilter.contains(alertType)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Gets the duration of the window in minutes.
     *
     * @return duration in minutes
     */
    public long getDurationMinutes() {
        if (startTime == null || endTime == null) return 0;
        return java.time.Duration.between(startTime, endTime).toMinutes();
    }

    /**
     * Gets formatted start time.
     *
     * @return formatted time
     */
    public String getStartTimeFormatted() {
        if (startTime == null) return "";
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(startTime);
    }

    /**
     * Gets formatted end time.
     *
     * @return formatted time
     */
    public String getEndTimeFormatted() {
        if (endTime == null) return "";
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(endTime);
    }

    /**
     * Gets status badge CSS class.
     *
     * @return CSS class
     */
    public String getStatusCssClass() {
        if (isActiveNow()) return "bg-warning";
        if (endTime.isBefore(Instant.now())) return "bg-secondary";
        return "bg-info";
    }

    /**
     * Gets status text.
     *
     * @return status text
     */
    public String getStatusText() {
        if (isActiveNow()) return "Active";
        if (endTime.isBefore(Instant.now())) return "Ended";
        return "Scheduled";
    }

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }
    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
    public boolean isRecurring() { return recurring; }
    public void setRecurring(boolean recurring) { this.recurring = recurring; }
    public String getRecurrencePattern() { return recurrencePattern; }
    public void setRecurrencePattern(String recurrencePattern) { this.recurrencePattern = recurrencePattern; }
    public List<String> getInstanceFilter() { return instanceFilter; }
    public void setInstanceFilter(List<String> instanceFilter) { this.instanceFilter = instanceFilter; }
    public List<String> getAlertTypeFilter() { return alertTypeFilter; }
    public void setAlertTypeFilter(List<String> alertTypeFilter) { this.alertTypeFilter = alertTypeFilter; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
