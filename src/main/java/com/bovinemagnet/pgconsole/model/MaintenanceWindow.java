package com.bovinemagnet.pgconsole.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a maintenance window during which alerts are suppressed.
 * <p>
 * Maintenance windows allow administrators to schedule periods when specific alerts
 * should not trigger notifications, typically during planned maintenance activities.
 * Windows can be configured as one-time events or recurring schedules, and can be
 * filtered to affect only specific database instances or alert types.
 * <p>
 * A maintenance window is considered active when the current time falls between
 * the configured start and end times. During this period, alerts matching the
 * window's filters will be suppressed.
 * <p>
 * Usage example:
 * <pre>{@code
 * MaintenanceWindow window = new MaintenanceWindow(
 *     "Weekly Database Backup",
 *     Instant.parse("2026-01-05T02:00:00Z"),
 *     Instant.parse("2026-01-05T04:00:00Z")
 * );
 * window.setRecurring(true);
 * window.setRecurrencePattern("WEEKLY");
 * window.setInstanceFilter(List.of("prod-db-1", "prod-db-2"));
 * window.setAlertTypeFilter(List.of("HIGH_LOAD", "SLOW_QUERY"));
 *
 * // Check if an alert should be suppressed
 * if (window.suppressesAlert("prod-db-1", "HIGH_LOAD")) {
 *     // Alert is suppressed during this maintenance window
 * }
 * }</pre>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @since 0.0.0
 * @see com.bovinemagnet.pgconsole.service.AlertService
 */
public class MaintenanceWindow {

    /**
     * Defines recurrence patterns for maintenance windows.
     * <p>
     * Determines how a maintenance window repeats over time. One-time windows
     * occur only once at the specified start and end times. Recurring patterns
     * (DAILY, WEEKLY, MONTHLY) automatically repeat the window at the specified
     * interval. Custom patterns allow specifying a cron expression for complex
     * scheduling requirements.
     *
     * @since 0.0.0
     */
    public enum RecurrencePattern {
        /** One-time window that does not repeat */
        NONE("One-time"),

        /** Window repeats every day at the same time */
        DAILY("Daily"),

        /** Window repeats every week on the same day and time */
        WEEKLY("Weekly"),

        /** Window repeats every month on the same day and time */
        MONTHLY("Monthly"),

        /** Window repeats according to a custom cron expression */
        CUSTOM("Custom (Cron)");

        /** Human-readable display name for the pattern */
        private final String displayName;

        /**
         * Constructs a recurrence pattern with the specified display name.
         *
         * @param displayName the human-readable name for this pattern
         */
        RecurrencePattern(String displayName) {
            this.displayName = displayName;
        }

        /**
         * Returns the human-readable display name for this recurrence pattern.
         *
         * @return the display name, never null
         */
        public String getDisplayName() {
            return displayName;
        }
    }

    /** Unique identifier for this maintenance window, null for unsaved windows */
    private Long id;

    /** Short descriptive name for the maintenance window */
    private String name;

    /** Detailed description of the maintenance purpose and scope */
    private String description;

    /** The instant when this maintenance window begins */
    private Instant startTime;

    /** The instant when this maintenance window ends */
    private Instant endTime;

    /** True if this window repeats according to a recurrence pattern */
    private boolean recurring;

    /**
     * The recurrence pattern as a string (DAILY, WEEKLY, MONTHLY, or cron expression).
     * Only applicable when recurring is true.
     */
    private String recurrencePattern;

    /**
     * List of database instance names to which this window applies.
     * Empty list means all instances are affected.
     */
    private List<String> instanceFilter = new ArrayList<>();

    /**
     * List of alert types to which this window applies.
     * Empty list means all alert types are affected.
     */
    private List<String> alertTypeFilter = new ArrayList<>();

    /** Username of the person who created this maintenance window */
    private String createdBy;

    /** Timestamp when this maintenance window was created */
    private Instant createdAt;

    /** Timestamp when this maintenance window was last updated */
    private Instant updatedAt;

    /**
     * Constructs an empty maintenance window with default values.
     * <p>
     * All fields will be null or empty collections. This constructor is primarily
     * used for deserialisation and framework purposes.
     */
    public MaintenanceWindow() {
    }

    /**
     * Constructs a maintenance window with the specified name and time range.
     * <p>
     * Creates a non-recurring maintenance window with the given parameters.
     * Instance and alert type filters will be empty (affecting all instances
     * and alert types).
     *
     * @param name the name of the maintenance window, should not be null
     * @param startTime the start time of the window, should not be null
     * @param endTime the end time of the window, should not be null and should be after startTime
     */
    public MaintenanceWindow(String name, Instant startTime, Instant endTime) {
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * Checks if the maintenance window is currently active.
     * <p>
     * A window is considered active when the current system time falls between
     * the configured start and end times (exclusive of the boundaries). This method
     * does not consider recurrence patterns; it only checks the literal start and
     * end times.
     *
     * @return true if the current time is after the start time and before the end time,
     *         false otherwise or if either time is null
     */
    public boolean isActiveNow() {
        Instant now = Instant.now();
        return now.isAfter(startTime) && now.isBefore(endTime);
    }

    /**
     * Checks if this maintenance window suppresses the specified alert.
     * <p>
     * An alert is suppressed if all of the following conditions are met:
     * <ul>
     *   <li>The maintenance window is currently active ({@link #isActiveNow()} returns true)</li>
     *   <li>The instance name matches the instance filter (or filter is empty/null)</li>
     *   <li>The alert type matches the alert type filter (or filter is empty/null)</li>
     * </ul>
     * Empty or null filters are treated as "match all" for that dimension.
     *
     * @param instanceName the name of the database instance generating the alert, may be null
     * @param alertType the type of alert being checked, may be null
     * @return true if the alert should be suppressed during this maintenance window,
     *         false if the window is not active or the alert doesn't match the filters
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
     * Calculates the duration of the maintenance window in minutes.
     * <p>
     * Computes the time difference between the start and end times. If either
     * time is null, returns 0.
     *
     * @return the duration in minutes, or 0 if start or end time is null
     */
    public long getDurationMinutes() {
        if (startTime == null || endTime == null) return 0;
        return java.time.Duration.between(startTime, endTime).toMinutes();
    }

    /**
     * Returns the start time formatted for display in the user interface.
     * <p>
     * Formats the start time using the pattern "yyyy-MM-dd HH:mm" in the
     * system's default time zone. This provides a human-readable representation
     * suitable for display in dashboards and reports.
     *
     * @return the formatted start time, or an empty string if startTime is null
     */
    public String getStartTimeFormatted() {
        if (startTime == null) return "";
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(startTime);
    }

    /**
     * Returns the end time formatted for display in the user interface.
     * <p>
     * Formats the end time using the pattern "yyyy-MM-dd HH:mm" in the
     * system's default time zone. This provides a human-readable representation
     * suitable for display in dashboards and reports.
     *
     * @return the formatted end time, or an empty string if endTime is null
     */
    public String getEndTimeFormatted() {
        if (endTime == null) return "";
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(endTime);
    }

    /**
     * Returns the Bootstrap CSS class for the status badge.
     * <p>
     * Determines the appropriate Bootstrap background colour class based on
     * the window's current state:
     * <ul>
     *   <li>bg-warning (yellow) - Window is currently active</li>
     *   <li>bg-secondary (grey) - Window has ended</li>
     *   <li>bg-info (blue) - Window is scheduled for the future</li>
     * </ul>
     *
     * @return the Bootstrap CSS class for displaying the window's status
     */
    public String getStatusCssClass() {
        if (isActiveNow()) return "bg-warning";
        if (endTime.isBefore(Instant.now())) return "bg-secondary";
        return "bg-info";
    }

    /**
     * Returns the human-readable status text for this maintenance window.
     * <p>
     * Provides a simple text description of the window's current state:
     * <ul>
     *   <li>"Active" - Window is currently active</li>
     *   <li>"Ended" - Window has finished</li>
     *   <li>"Scheduled" - Window is planned for the future</li>
     * </ul>
     *
     * @return the status text describing the window's current state
     */
    public String getStatusText() {
        if (isActiveNow()) return "Active";
        if (endTime.isBefore(Instant.now())) return "Ended";
        return "Scheduled";
    }

    // Getters and Setters

    /**
     * Returns the unique identifier for this maintenance window.
     *
     * @return the window ID, or null if this window has not been persisted
     */
    public Long getId() { return id; }

    /**
     * Sets the unique identifier for this maintenance window.
     * <p>
     * This is typically set by the persistence layer when the window is saved.
     *
     * @param id the window ID to set
     */
    public void setId(Long id) { this.id = id; }

    /**
     * Returns the name of this maintenance window.
     *
     * @return the window name, may be null
     */
    public String getName() { return name; }

    /**
     * Sets the name of this maintenance window.
     * <p>
     * The name should be a short, descriptive identifier for the maintenance
     * activity, such as "Weekly Backup" or "Database Migration".
     *
     * @param name the window name to set
     */
    public void setName(String name) { this.name = name; }

    /**
     * Returns the detailed description of this maintenance window.
     *
     * @return the window description, may be null
     */
    public String getDescription() { return description; }

    /**
     * Sets the detailed description of this maintenance window.
     * <p>
     * The description can provide additional context about the maintenance
     * purpose, scope, and any special considerations.
     *
     * @param description the window description to set
     */
    public void setDescription(String description) { this.description = description; }

    /**
     * Returns the start time of this maintenance window.
     *
     * @return the start time, may be null
     */
    public Instant getStartTime() { return startTime; }

    /**
     * Sets the start time of this maintenance window.
     * <p>
     * The window becomes active when the current time is after this instant.
     * For recurring windows, this represents the start time of each occurrence.
     *
     * @param startTime the start time to set
     */
    public void setStartTime(Instant startTime) { this.startTime = startTime; }

    /**
     * Returns the end time of this maintenance window.
     *
     * @return the end time, may be null
     */
    public Instant getEndTime() { return endTime; }

    /**
     * Sets the end time of this maintenance window.
     * <p>
     * The window becomes inactive when the current time reaches this instant.
     * For recurring windows, this represents the end time of each occurrence.
     * Should be after the start time.
     *
     * @param endTime the end time to set
     */
    public void setEndTime(Instant endTime) { this.endTime = endTime; }

    /**
     * Returns whether this maintenance window recurs.
     *
     * @return true if the window repeats according to a recurrence pattern, false otherwise
     */
    public boolean isRecurring() { return recurring; }

    /**
     * Sets whether this maintenance window recurs.
     * <p>
     * When set to true, the window will repeat according to the configured
     * recurrence pattern. When false, the window occurs only once.
     *
     * @param recurring true to make this a recurring window, false for one-time
     */
    public void setRecurring(boolean recurring) { this.recurring = recurring; }

    /**
     * Returns the recurrence pattern for this maintenance window.
     *
     * @return the recurrence pattern string (e.g., "DAILY", "WEEKLY", "MONTHLY",
     *         or a cron expression), or null if not recurring
     */
    public String getRecurrencePattern() { return recurrencePattern; }

    /**
     * Sets the recurrence pattern for this maintenance window.
     * <p>
     * Valid values include "DAILY", "WEEKLY", "MONTHLY", or a custom cron
     * expression. This is only applicable when {@link #isRecurring()} returns true.
     *
     * @param recurrencePattern the recurrence pattern to set
     * @see RecurrencePattern
     */
    public void setRecurrencePattern(String recurrencePattern) { this.recurrencePattern = recurrencePattern; }

    /**
     * Returns the list of database instance names this window applies to.
     *
     * @return the instance filter list, never null but may be empty (meaning all instances)
     */
    public List<String> getInstanceFilter() { return instanceFilter; }

    /**
     * Sets the list of database instance names this window applies to.
     * <p>
     * If the list is empty or null, the window applies to all database instances.
     * Otherwise, the window only suppresses alerts from instances in this list.
     *
     * @param instanceFilter the instance filter list to set
     */
    public void setInstanceFilter(List<String> instanceFilter) { this.instanceFilter = instanceFilter; }

    /**
     * Returns the list of alert types this window applies to.
     *
     * @return the alert type filter list, never null but may be empty (meaning all alert types)
     */
    public List<String> getAlertTypeFilter() { return alertTypeFilter; }

    /**
     * Sets the list of alert types this window applies to.
     * <p>
     * If the list is empty or null, the window applies to all alert types.
     * Otherwise, the window only suppresses the specified alert types.
     *
     * @param alertTypeFilter the alert type filter list to set
     */
    public void setAlertTypeFilter(List<String> alertTypeFilter) { this.alertTypeFilter = alertTypeFilter; }

    /**
     * Returns the username of the person who created this maintenance window.
     *
     * @return the creator's username, may be null
     */
    public String getCreatedBy() { return createdBy; }

    /**
     * Sets the username of the person who created this maintenance window.
     * <p>
     * This is typically set automatically by the system when the window is created.
     *
     * @param createdBy the creator's username to set
     */
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    /**
     * Returns the timestamp when this maintenance window was created.
     *
     * @return the creation timestamp, may be null
     */
    public Instant getCreatedAt() { return createdAt; }

    /**
     * Sets the timestamp when this maintenance window was created.
     * <p>
     * This is typically set automatically by the system when the window is created.
     *
     * @param createdAt the creation timestamp to set
     */
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    /**
     * Returns the timestamp when this maintenance window was last updated.
     *
     * @return the last update timestamp, may be null
     */
    public Instant getUpdatedAt() { return updatedAt; }

    /**
     * Sets the timestamp when this maintenance window was last updated.
     * <p>
     * This is typically set automatically by the system when the window is modified.
     *
     * @param updatedAt the last update timestamp to set
     */
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
