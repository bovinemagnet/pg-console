package com.bovinemagnet.pgconsole.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a silence rule for suppressing alerts matching specific patterns.
 * <p>
 * Alert silences provide a mechanism to temporarily suppress alerts that match
 * defined criteria during maintenance windows, testing periods, or when investigating
 * known issues. Each silence contains one or more {@link Matcher} instances that
 * define the conditions under which alerts should be suppressed.
 * <p>
 * Silences have a defined time window (start and end times) and are only active
 * within that period. When multiple matchers are defined, all matchers must match
 * for an alert to be silenced (logical AND operation).
 * <p>
 * Example usage:
 * <pre>{@code
 * AlertSilence silence = new AlertSilence("Maintenance Window", Instant.now().plus(Duration.ofHours(2)));
 * silence.addMatcher(new Matcher("type", Matcher.Operator.EQUALS, "CONNECTION_ERROR"));
 * silence.addMatcher(new Matcher("instance", Matcher.Operator.EQUALS, "prod-db-01"));
 *
 * if (silence.matches(alertType, alertSeverity, instanceName, alertMessage)) {
 *     // Alert is silenced, do not send notification
 * }
 * }</pre>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see Matcher
 */
public class AlertSilence {

    /** Unique identifier for this silence rule. May be null for unsaved instances. */
    private Long id;

    /** Human-readable name for this silence rule. */
    private String name;

    /** Optional description explaining the purpose of this silence. */
    private String description;

    /** List of matchers that must all match for an alert to be silenced. Empty list matches all alerts. */
    private List<Matcher> matchers = new ArrayList<>();

    /** Time when this silence becomes active. */
    private Instant startTime;

    /** Time when this silence expires. */
    private Instant endTime;

    /** Username or identifier of the user who created this silence. */
    private String createdBy;

    /** Timestamp when this silence was created. */
    private Instant createdAt;

    /**
     * Creates a new alert silence with start time set to the current instant.
     * <p>
     * The end time must be set separately before this silence becomes useful.
     */
    public AlertSilence() {
        this.startTime = Instant.now();
    }

    /**
     * Creates a new alert silence with the specified name and end time.
     * <p>
     * The start time is automatically set to the current instant.
     *
     * @param name the human-readable name for this silence
     * @param endTime the time when this silence expires
     */
    public AlertSilence(String name, Instant endTime) {
        this();
        this.name = name;
        this.endTime = endTime;
    }

    /**
     * Checks if this silence is currently active based on the current time.
     * <p>
     * A silence is considered active if the current instant falls between
     * the start time (exclusive) and end time (exclusive).
     *
     * @return {@code true} if the current time is within the silence window, {@code false} otherwise
     */
    public boolean isActive() {
        Instant now = Instant.now();
        return now.isAfter(startTime) && now.isBefore(endTime);
    }

    /**
     * Checks if this silence matches the given alert attributes.
     * <p>
     * For an alert to be matched (and thus silenced), all of the following conditions must be true:
     * <ul>
     * <li>The silence must be currently active (within its time window)</li>
     * <li>If matchers are defined, all matchers must match their respective alert fields</li>
     * <li>If no matchers are defined, all alerts are matched (convenience for time-based silencing)</li>
     * </ul>
     * <p>
     * Field matching is performed using the configured {@link Matcher.Operator} for each matcher.
     * The following field names are recognised (case-insensitive):
     * <ul>
     * <li>"type" or "alerttype" - matches against {@code alertType} parameter</li>
     * <li>"severity" or "alertseverity" - matches against {@code alertSeverity} parameter</li>
     * <li>"instance" or "instancename" - matches against {@code instanceName} parameter</li>
     * <li>"message" or "alertmessage" - matches against {@code alertMessage} parameter</li>
     * </ul>
     *
     * @param alertType the type of the alert (e.g., "CONNECTION_ERROR", "SLOW_QUERY")
     * @param alertSeverity the severity level (e.g., "WARNING", "CRITICAL")
     * @param instanceName the database instance name generating the alert
     * @param alertMessage the alert message text
     * @return {@code true} if this silence matches all specified alert attributes and is currently active,
     *         {@code false} otherwise
     * @see Matcher#matches(String)
     */
    public boolean matches(String alertType, String alertSeverity, String instanceName, String alertMessage) {
        if (!isActive()) return false;
        if (matchers.isEmpty()) return true;

        for (Matcher matcher : matchers) {
            String value = switch (matcher.getField().toLowerCase()) {
                case "type", "alerttype" -> alertType;
                case "severity", "alertseverity" -> alertSeverity;
                case "instance", "instancename" -> instanceName;
                case "message", "alertmessage" -> alertMessage;
                default -> null;
            };

            if (!matcher.matches(value)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calculates the remaining time until this silence expires.
     * <p>
     * Returns 0 if the silence has already expired or if no end time is set.
     * Useful for displaying countdown timers in user interfaces.
     *
     * @return the remaining time in minutes, or 0 if expired or end time is null
     */
    public long getRemainingMinutes() {
        if (endTime == null) return 0;
        long remaining = java.time.Duration.between(Instant.now(), endTime).toMinutes();
        return Math.max(0, remaining);
    }

    /**
     * Formats the end time for display using the system's default time zone.
     * <p>
     * The format pattern is "yyyy-MM-dd HH:mm" (e.g., "2026-01-05 14:30").
     *
     * @return the formatted end time, or an empty string if end time is null
     */
    public String getEndTimeFormatted() {
        if (endTime == null) return "";
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(endTime);
    }

    /**
     * Returns a Bootstrap CSS class appropriate for the current status of this silence.
     * <p>
     * The returned class can be used to visually indicate the silence state:
     * <ul>
     * <li>{@code "bg-warning"} - Silence is currently active</li>
     * <li>{@code "bg-secondary"} - Silence has expired</li>
     * <li>{@code "bg-info"} - Silence is pending (not yet started)</li>
     * </ul>
     *
     * @return a Bootstrap background colour class representing the silence status
     */
    public String getStatusCssClass() {
        if (!isActive()) {
            return endTime.isBefore(Instant.now()) ? "bg-secondary" : "bg-info";
        }
        return "bg-warning";
    }

    /**
     * Returns a human-readable text description of the current status.
     * <p>
     * Possible values:
     * <ul>
     * <li>{@code "Active"} - Silence is currently suppressing alerts</li>
     * <li>{@code "Expired"} - Silence has ended and is no longer in effect</li>
     * <li>{@code "Pending"} - Silence is scheduled but not yet active</li>
     * </ul>
     *
     * @return a status description suitable for display in user interfaces
     */
    public String getStatusText() {
        if (!isActive()) {
            return endTime.isBefore(Instant.now()) ? "Expired" : "Pending";
        }
        return "Active";
    }

    /**
     * Defines a matching condition for alert fields.
     * <p>
     * A matcher specifies a field name (e.g., "type", "severity"), an operator
     * (e.g., equals, contains, regex), and a value to compare against. When an
     * alert is evaluated against a silence, each matcher determines whether its
     * specific field matches the expected value.
     * <p>
     * Example usage:
     * <pre>{@code
     * // Match alerts where severity exactly equals "CRITICAL"
     * Matcher severityMatcher = new Matcher("severity", Operator.EQUALS, "CRITICAL");
     *
     * // Match alerts where message contains "connection timeout"
     * Matcher messageMatcher = new Matcher("message", Operator.CONTAINS, "connection timeout");
     *
     * // Match alerts where instance name matches a pattern
     * Matcher instanceMatcher = new Matcher("instance", Operator.REGEX, "prod-db-\\d+");
     * }</pre>
     *
     * @see AlertSilence#matches(String, String, String, String)
     */
    public static class Matcher {
        /**
         * Defines the comparison operation to perform when matching alert field values.
         * <p>
         * Each operator has a display name suitable for showing in user interfaces.
         */
        public enum Operator {
            /** Performs case-insensitive equality comparison. */
            EQUALS("equals"),

            /** Performs case-insensitive inequality comparison. */
            NOT_EQUALS("not equals"),

            /** Checks if the actual value contains the expected value (case-insensitive substring match). */
            CONTAINS("contains"),

            /** Matches the actual value against a regular expression pattern. */
            REGEX("regex");

            private final String displayName;

            /**
             * Creates an operator with the specified display name.
             *
             * @param displayName human-readable name for this operator
             */
            Operator(String displayName) {
                this.displayName = displayName;
            }

            /**
             * Returns the human-readable display name for this operator.
             *
             * @return the display name (e.g., "equals", "contains")
             */
            public String getDisplayName() {
                return displayName;
            }
        }

        /** The name of the alert field to match against (e.g., "type", "severity", "instance", "message"). */
        private String field;

        /** The comparison operator to use for matching. Defaults to {@link Operator#EQUALS}. */
        private Operator operator = Operator.EQUALS;

        /** The expected value to compare against the alert field. */
        private String value;

        /**
         * Creates an empty matcher.
         * <p>
         * Field, operator, and value must be set before this matcher can be used.
         */
        public Matcher() {
        }

        /**
         * Creates a matcher with the specified field, operator, and value.
         *
         * @param field the name of the alert field to match (e.g., "type", "severity")
         * @param operator the comparison operator to use
         * @param value the expected value to compare against
         */
        public Matcher(String field, Operator operator, String value) {
            this.field = field;
            this.operator = operator;
            this.value = value;
        }

        /**
         * Evaluates whether the given actual value matches this matcher's criteria.
         * <p>
         * Null values are treated as empty strings for comparison purposes.
         * The comparison behaviour depends on the configured {@link Operator}:
         * <ul>
         * <li>{@link Operator#EQUALS} - Case-insensitive equality</li>
         * <li>{@link Operator#NOT_EQUALS} - Case-insensitive inequality</li>
         * <li>{@link Operator#CONTAINS} - Case-insensitive substring search</li>
         * <li>{@link Operator#REGEX} - Regular expression pattern matching (case-sensitive)</li>
         * </ul>
         *
         * @param actualValue the value from the alert field to compare against this matcher's value
         * @return {@code true} if the actual value matches according to the operator, {@code false} otherwise
         */
        public boolean matches(String actualValue) {
            if (actualValue == null) actualValue = "";
            if (value == null) value = "";

            return switch (operator) {
                case EQUALS -> actualValue.equalsIgnoreCase(value);
                case NOT_EQUALS -> !actualValue.equalsIgnoreCase(value);
                case CONTAINS -> actualValue.toLowerCase().contains(value.toLowerCase());
                case REGEX -> actualValue.matches(value);
            };
        }

        /**
         * Returns a human-readable representation of this matcher suitable for display.
         * <p>
         * The format is: {@code field operator "value"}
         * <p>
         * Example: {@code severity equals "CRITICAL"}
         *
         * @return formatted string describing this matcher's condition
         */
        public String getDisplayText() {
            return field + " " + operator.getDisplayName() + " \"" + value + "\"";
        }

        // Getters and Setters

        /**
         * Returns the alert field name being matched.
         *
         * @return the field name
         */
        public String getField() { return field; }

        /**
         * Sets the alert field name to match.
         *
         * @param field the field name (e.g., "type", "severity", "instance", "message")
         */
        public void setField(String field) { this.field = field; }

        /**
         * Returns the comparison operator.
         *
         * @return the operator
         */
        public Operator getOperator() { return operator; }

        /**
         * Sets the comparison operator.
         *
         * @param operator the operator to use for matching
         */
        public void setOperator(Operator operator) { this.operator = operator; }

        /**
         * Returns the expected value to compare against.
         *
         * @return the expected value
         */
        public String getValue() { return value; }

        /**
         * Sets the expected value to compare against.
         *
         * @param value the expected value
         */
        public void setValue(String value) { this.value = value; }
    }

    // Getters and Setters

    /**
     * Returns the unique identifier for this silence.
     *
     * @return the silence ID, or null if not yet persisted
     */
    public Long getId() { return id; }

    /**
     * Sets the unique identifier for this silence.
     *
     * @param id the silence ID
     */
    public void setId(Long id) { this.id = id; }

    /**
     * Returns the human-readable name of this silence.
     *
     * @return the silence name
     */
    public String getName() { return name; }

    /**
     * Sets the human-readable name for this silence.
     *
     * @param name the silence name
     */
    public void setName(String name) { this.name = name; }

    /**
     * Returns the optional description explaining the purpose of this silence.
     *
     * @return the description, or null if not set
     */
    public String getDescription() { return description; }

    /**
     * Sets the optional description explaining the purpose of this silence.
     *
     * @param description the description text
     */
    public void setDescription(String description) { this.description = description; }

    /**
     * Returns the list of matchers defining which alerts should be silenced.
     * <p>
     * All matchers must match for an alert to be silenced (logical AND).
     * If the list is empty, all alerts within the time window are silenced.
     *
     * @return the list of matchers (never null)
     */
    public List<Matcher> getMatchers() { return matchers; }

    /**
     * Sets the list of matchers defining which alerts should be silenced.
     *
     * @param matchers the list of matchers
     */
    public void setMatchers(List<Matcher> matchers) { this.matchers = matchers; }

    /**
     * Returns the time when this silence becomes active.
     *
     * @return the start time
     */
    public Instant getStartTime() { return startTime; }

    /**
     * Sets the time when this silence becomes active.
     *
     * @param startTime the start time
     */
    public void setStartTime(Instant startTime) { this.startTime = startTime; }

    /**
     * Returns the time when this silence expires.
     *
     * @return the end time
     */
    public Instant getEndTime() { return endTime; }

    /**
     * Sets the time when this silence expires.
     *
     * @param endTime the end time
     */
    public void setEndTime(Instant endTime) { this.endTime = endTime; }

    /**
     * Returns the username or identifier of the user who created this silence.
     *
     * @return the creator identifier, or null if not set
     */
    public String getCreatedBy() { return createdBy; }

    /**
     * Sets the username or identifier of the user who created this silence.
     *
     * @param createdBy the creator identifier
     */
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    /**
     * Returns the timestamp when this silence was created.
     *
     * @return the creation timestamp, or null if not set
     */
    public Instant getCreatedAt() { return createdAt; }

    /**
     * Sets the timestamp when this silence was created.
     *
     * @param createdAt the creation timestamp
     */
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    /**
     * Adds a matcher to this silence's list of matching conditions.
     * <p>
     * All matchers must match for an alert to be silenced.
     *
     * @param matcher the matcher to add
     */
    public void addMatcher(Matcher matcher) {
        this.matchers.add(matcher);
    }
}
