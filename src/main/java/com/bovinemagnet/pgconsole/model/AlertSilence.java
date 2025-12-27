package com.bovinemagnet.pgconsole.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a silence rule for suppressing alerts matching specific patterns.
 * <p>
 * Silences are temporary and match alerts based on configurable matchers.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class AlertSilence {

    private Long id;
    private String name;
    private String description;
    private List<Matcher> matchers = new ArrayList<>();
    private Instant startTime;
    private Instant endTime;
    private String createdBy;
    private Instant createdAt;

    public AlertSilence() {
        this.startTime = Instant.now();
    }

    public AlertSilence(String name, Instant endTime) {
        this();
        this.name = name;
        this.endTime = endTime;
    }

    /**
     * Checks if the silence is currently active.
     *
     * @return true if active
     */
    public boolean isActive() {
        Instant now = Instant.now();
        return now.isAfter(startTime) && now.isBefore(endTime);
    }

    /**
     * Checks if this silence matches the given alert.
     *
     * @param alertType alert type
     * @param alertSeverity alert severity
     * @param instanceName instance name
     * @param alertMessage alert message
     * @return true if all matchers match
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
     * Gets remaining time until silence expires.
     *
     * @return remaining time in minutes
     */
    public long getRemainingMinutes() {
        if (endTime == null) return 0;
        long remaining = java.time.Duration.between(Instant.now(), endTime).toMinutes();
        return Math.max(0, remaining);
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
     * Gets status CSS class.
     *
     * @return CSS class
     */
    public String getStatusCssClass() {
        if (!isActive()) {
            return endTime.isBefore(Instant.now()) ? "bg-secondary" : "bg-info";
        }
        return "bg-warning";
    }

    /**
     * Gets status text.
     *
     * @return status text
     */
    public String getStatusText() {
        if (!isActive()) {
            return endTime.isBefore(Instant.now()) ? "Expired" : "Pending";
        }
        return "Active";
    }

    /**
     * Matcher for alert fields.
     */
    public static class Matcher {
        public enum Operator {
            EQUALS("equals"),
            NOT_EQUALS("not equals"),
            CONTAINS("contains"),
            REGEX("regex");

            private final String displayName;

            Operator(String displayName) {
                this.displayName = displayName;
            }

            public String getDisplayName() {
                return displayName;
            }
        }

        private String field;
        private Operator operator = Operator.EQUALS;
        private String value;

        public Matcher() {
        }

        public Matcher(String field, Operator operator, String value) {
            this.field = field;
            this.operator = operator;
            this.value = value;
        }

        /**
         * Checks if the given value matches this matcher.
         *
         * @param actualValue the value to check
         * @return true if matches
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
         * Gets display text for this matcher.
         *
         * @return display text
         */
        public String getDisplayText() {
            return field + " " + operator.getDisplayName() + " \"" + value + "\"";
        }

        // Getters and Setters
        public String getField() { return field; }
        public void setField(String field) { this.field = field; }
        public Operator getOperator() { return operator; }
        public void setOperator(Operator operator) { this.operator = operator; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<Matcher> getMatchers() { return matchers; }
    public void setMatchers(List<Matcher> matchers) { this.matchers = matchers; }
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }
    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public void addMatcher(Matcher matcher) {
        this.matchers.add(matcher);
    }
}
