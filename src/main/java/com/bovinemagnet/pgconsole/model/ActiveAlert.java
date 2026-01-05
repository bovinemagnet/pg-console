package com.bovinemagnet.pgconsole.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Represents an active alert being tracked for escalation in the PostgreSQL console monitoring system.
 * <p>
 * Active alerts are created when a monitoring alert fires and remain active until they are
 * either acknowledged by an operator or automatically resolved when the underlying condition
 * clears. The alert tracks escalation state, notification history, and lifecycle metadata.
 * </p>
 * <p>
 * Alerts support multi-tier escalation policies, allowing notifications to escalate through
 * different severity levels or contact groups based on time and acknowledgement state.
 * </p>
 * <p>
 * Example alert lifecycle:
 * <pre>{@code
 * ActiveAlert alert = new ActiveAlert("db-high-cpu", "PERFORMANCE", "CRITICAL", "CPU usage exceeded 90%");
 * alert.setInstanceName("production-db");
 * alert.setEscalationPolicyId(1L);
 *
 * // Alert fires and escalates
 * alert.setCurrentEscalationTier(2);
 * alert.setLastNotificationAt(Instant.now());
 *
 * // Operator acknowledges
 * alert.setAcknowledged(true);
 *
 * // Condition clears and alert resolves
 * alert.setResolved(true);
 * alert.setResolvedAt(Instant.now());
 * }</pre>
 * </p>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see EscalationPolicy
 * @since 0.0.0
 */
public class ActiveAlert {

    /**
     * Primary key identifier for the alert record in the database.
     * Null for new alerts not yet persisted.
     */
    private Long id;

    /**
     * Unique identifier for this specific alert instance.
     * Used to correlate notifications and track alert history.
     */
    private String alertId;

    /**
     * The type or category of the alert (e.g., "PERFORMANCE", "CONNECTION", "DISK_SPACE").
     * Used to group related alerts and route notifications appropriately.
     */
    private String alertType;

    /**
     * The severity level of the alert (e.g., "CRITICAL", "HIGH", "MEDIUM", "LOW").
     * Determines escalation urgency and notification priority.
     * @see #getSeverityCssClass()
     */
    private String alertSeverity;

    /**
     * Human-readable description of the alert condition.
     * Should provide sufficient context for operators to understand and respond to the issue.
     */
    private String alertMessage;

    /**
     * The name of the PostgreSQL instance or database that generated this alert.
     * Null if the alert is not instance-specific.
     */
    private String instanceName;

    /**
     * The timestamp when this alert first fired.
     * Automatically set to the current time in the default constructor.
     * Never null for properly initialised alerts.
     */
    private Instant firedAt;

    /**
     * The timestamp when the last notification was sent for this alert.
     * Null if no notifications have been sent yet.
     * Used to calculate notification intervals and escalation timing.
     */
    private Instant lastNotificationAt;

    /**
     * The current escalation tier (1-based index).
     * Starts at tier 1 and increments as the alert escalates through notification tiers.
     * Defaults to 1 for new alerts.
     */
    private int currentEscalationTier = 1;

    /**
     * Foreign key reference to the escalation policy governing this alert.
     * Null if no escalation policy is assigned.
     * @see EscalationPolicy
     */
    private Long escalationPolicyId;

    /**
     * Indicates whether an operator has acknowledged this alert.
     * Acknowledgement may affect escalation behaviour depending on the policy.
     * Defaults to false.
     */
    private boolean acknowledged;

    /**
     * Indicates whether this alert has been resolved (condition cleared).
     * Resolved alerts typically stop escalating and are archived.
     * Defaults to false.
     */
    private boolean resolved;

    /**
     * The timestamp when this alert was resolved.
     * Null if the alert is still active. Set when {@link #resolved} becomes true.
     */
    private Instant resolvedAt;

    /**
     * Additional metadata about the alert in JSON format.
     * Can contain context-specific information such as metric values,
     * thresholds, query details, or diagnostic data.
     */
    private String metadata;

    /**
     * Constructs a new active alert with the current timestamp as the fired time.
     * The {@link #firedAt} field is automatically set to {@link Instant#now()}.
     * <p>
     * This is the default constructor used by persistence frameworks and when
     * creating alerts programmatically without specifying details.
     * </p>
     */
    public ActiveAlert() {
        this.firedAt = Instant.now();
    }

    /**
     * Constructs a new active alert with the specified details.
     * The {@link #firedAt} timestamp is automatically set to the current time.
     *
     * @param alertId       the unique identifier for this alert instance
     * @param alertType     the type or category of the alert (e.g., "PERFORMANCE")
     * @param alertSeverity the severity level (e.g., "CRITICAL", "HIGH", "MEDIUM", "LOW")
     * @param alertMessage  the human-readable alert message describing the condition
     */
    public ActiveAlert(String alertId, String alertType, String alertSeverity, String alertMessage) {
        this();
        this.alertId = alertId;
        this.alertType = alertType;
        this.alertSeverity = alertSeverity;
        this.alertMessage = alertMessage;
    }

    /**
     * Calculates the duration in seconds since the alert fired.
     * <p>
     * For resolved alerts, returns the duration between {@link #firedAt} and {@link #resolvedAt}.
     * For active alerts, returns the duration between {@link #firedAt} and the current time.
     * </p>
     *
     * @return the alert duration in seconds, or 0 if {@link #firedAt} is null
     * @see #getDurationFormatted()
     */
    public long getDurationSeconds() {
        if (resolved && resolvedAt != null) {
            return java.time.Duration.between(firedAt, resolvedAt).toSeconds();
        }
        return java.time.Duration.between(firedAt, Instant.now()).toSeconds();
    }

    /**
     * Returns a human-readable formatted duration string.
     * <p>
     * Formats the duration using the most appropriate time unit:
     * <ul>
     *   <li>Less than 60 seconds: "30s"</li>
     *   <li>Less than 1 hour: "5m 30s"</li>
     *   <li>1 hour or more: "2h 15m"</li>
     * </ul>
     * </p>
     *
     * @return the formatted duration string, never null
     * @see #getDurationSeconds()
     */
    public String getDurationFormatted() {
        long seconds = getDurationSeconds();
        if (seconds < 60) return seconds + "s";
        if (seconds < 3600) return (seconds / 60) + "m " + (seconds % 60) + "s";
        return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
    }

    /**
     * Returns the alert fired timestamp formatted for display in templates.
     * <p>
     * Uses the system default time zone and formats as "yyyy-MM-dd HH:mm:ss".
     * This method is typically called from Qute templates for rendering alert times.
     * </p>
     *
     * @return the formatted timestamp, or an empty string if {@link #firedAt} is null
     */
    public String getFiredAtFormatted() {
        if (firedAt == null) return "";
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(firedAt);
    }

    /**
     * Returns the Bootstrap CSS class for displaying the alert severity as a badge.
     * <p>
     * Maps severity levels to appropriate Bootstrap background colour classes:
     * <ul>
     *   <li>CRITICAL: "bg-danger" (red)</li>
     *   <li>HIGH: "bg-warning text-dark" (yellow with dark text)</li>
     *   <li>MEDIUM: "bg-info" (blue)</li>
     *   <li>LOW: "bg-secondary" (grey)</li>
     *   <li>Unknown/null: "bg-secondary" (grey)</li>
     * </ul>
     * </p>
     *
     * @return the Bootstrap CSS class string, never null
     * @see #getStatusCssClass()
     */
    public String getSeverityCssClass() {
        if (alertSeverity == null) return "bg-secondary";
        return switch (alertSeverity.toUpperCase()) {
            case "CRITICAL" -> "bg-danger";
            case "HIGH" -> "bg-warning text-dark";
            case "MEDIUM" -> "bg-info";
            case "LOW" -> "bg-secondary";
            default -> "bg-secondary";
        };
    }

    /**
     * Returns the Bootstrap CSS class for displaying the alert status as a badge.
     * <p>
     * Maps alert lifecycle states to appropriate Bootstrap background colour classes:
     * <ul>
     *   <li>Resolved: "bg-success" (green)</li>
     *   <li>Acknowledged but not resolved: "bg-info" (blue)</li>
     *   <li>Firing (active, unacknowledged): "bg-danger" (red)</li>
     * </ul>
     * </p>
     *
     * @return the Bootstrap CSS class string, never null
     * @see #getStatusText()
     * @see #getSeverityCssClass()
     */
    public String getStatusCssClass() {
        if (resolved) return "bg-success";
        if (acknowledged) return "bg-info";
        return "bg-danger";
    }

    /**
     * Returns the human-readable status text for the alert.
     * <p>
     * Provides a text representation of the alert's current lifecycle state:
     * <ul>
     *   <li>"Resolved" - alert condition has cleared</li>
     *   <li>"Acknowledged" - operator has acknowledged but not resolved</li>
     *   <li>"Firing" - alert is active and unacknowledged</li>
     * </ul>
     * </p>
     *
     * @return the status text, never null
     * @see #getStatusCssClass()
     */
    public String getStatusText() {
        if (resolved) return "Resolved";
        if (acknowledged) return "Acknowledged";
        return "Firing";
    }

    // Getters and Setters

    /**
     * Returns the primary key identifier.
     *
     * @return the database ID, or null if not yet persisted
     */
    public Long getId() { return id; }

    /**
     * Sets the primary key identifier.
     *
     * @param id the database ID
     */
    public void setId(Long id) { this.id = id; }

    /**
     * Returns the unique alert identifier.
     *
     * @return the alert ID, may be null
     */
    public String getAlertId() { return alertId; }

    /**
     * Sets the unique alert identifier.
     *
     * @param alertId the alert ID
     */
    public void setAlertId(String alertId) { this.alertId = alertId; }

    /**
     * Returns the alert type or category.
     *
     * @return the alert type (e.g., "PERFORMANCE", "CONNECTION"), may be null
     */
    public String getAlertType() { return alertType; }

    /**
     * Sets the alert type or category.
     *
     * @param alertType the alert type (e.g., "PERFORMANCE", "CONNECTION")
     */
    public void setAlertType(String alertType) { this.alertType = alertType; }

    /**
     * Returns the alert severity level.
     *
     * @return the severity (e.g., "CRITICAL", "HIGH", "MEDIUM", "LOW"), may be null
     */
    public String getAlertSeverity() { return alertSeverity; }

    /**
     * Sets the alert severity level.
     *
     * @param alertSeverity the severity (e.g., "CRITICAL", "HIGH", "MEDIUM", "LOW")
     */
    public void setAlertSeverity(String alertSeverity) { this.alertSeverity = alertSeverity; }

    /**
     * Returns the human-readable alert message.
     *
     * @return the alert message, may be null
     */
    public String getAlertMessage() { return alertMessage; }

    /**
     * Sets the human-readable alert message.
     *
     * @param alertMessage the alert message
     */
    public void setAlertMessage(String alertMessage) { this.alertMessage = alertMessage; }

    /**
     * Returns the PostgreSQL instance name that generated this alert.
     *
     * @return the instance name, or null if not instance-specific
     */
    public String getInstanceName() { return instanceName; }

    /**
     * Sets the PostgreSQL instance name.
     *
     * @param instanceName the instance name
     */
    public void setInstanceName(String instanceName) { this.instanceName = instanceName; }

    /**
     * Returns the timestamp when the alert first fired.
     *
     * @return the fired timestamp, never null for properly initialised alerts
     */
    public Instant getFiredAt() { return firedAt; }

    /**
     * Sets the timestamp when the alert first fired.
     *
     * @param firedAt the fired timestamp
     */
    public void setFiredAt(Instant firedAt) { this.firedAt = firedAt; }

    /**
     * Returns the timestamp of the last notification sent for this alert.
     *
     * @return the last notification timestamp, or null if no notifications sent
     */
    public Instant getLastNotificationAt() { return lastNotificationAt; }

    /**
     * Sets the timestamp of the last notification.
     *
     * @param lastNotificationAt the last notification timestamp
     */
    public void setLastNotificationAt(Instant lastNotificationAt) { this.lastNotificationAt = lastNotificationAt; }

    /**
     * Returns the current escalation tier (1-based).
     *
     * @return the escalation tier, defaults to 1
     */
    public int getCurrentEscalationTier() { return currentEscalationTier; }

    /**
     * Sets the current escalation tier.
     *
     * @param currentEscalationTier the escalation tier (1-based index)
     */
    public void setCurrentEscalationTier(int currentEscalationTier) { this.currentEscalationTier = currentEscalationTier; }

    /**
     * Returns the foreign key reference to the escalation policy.
     *
     * @return the escalation policy ID, or null if no policy assigned
     */
    public Long getEscalationPolicyId() { return escalationPolicyId; }

    /**
     * Sets the escalation policy reference.
     *
     * @param escalationPolicyId the escalation policy ID
     */
    public void setEscalationPolicyId(Long escalationPolicyId) { this.escalationPolicyId = escalationPolicyId; }

    /**
     * Returns whether this alert has been acknowledged by an operator.
     *
     * @return true if acknowledged, false otherwise
     */
    public boolean isAcknowledged() { return acknowledged; }

    /**
     * Sets the acknowledged state.
     *
     * @param acknowledged true if acknowledged, false otherwise
     */
    public void setAcknowledged(boolean acknowledged) { this.acknowledged = acknowledged; }

    /**
     * Returns whether this alert has been resolved (condition cleared).
     *
     * @return true if resolved, false otherwise
     */
    public boolean isResolved() { return resolved; }

    /**
     * Sets the resolved state.
     *
     * @param resolved true if resolved, false otherwise
     */
    public void setResolved(boolean resolved) { this.resolved = resolved; }

    /**
     * Returns the timestamp when this alert was resolved.
     *
     * @return the resolved timestamp, or null if still active
     */
    public Instant getResolvedAt() { return resolvedAt; }

    /**
     * Sets the timestamp when this alert was resolved.
     *
     * @param resolvedAt the resolved timestamp
     */
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }

    /**
     * Returns the additional metadata in JSON format.
     *
     * @return the JSON metadata string, or null if no metadata
     */
    public String getMetadata() { return metadata; }

    /**
     * Sets the additional metadata in JSON format.
     *
     * @param metadata the JSON metadata string
     */
    public void setMetadata(String metadata) { this.metadata = metadata; }
}
