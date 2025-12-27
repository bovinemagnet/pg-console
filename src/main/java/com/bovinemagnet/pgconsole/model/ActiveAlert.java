package com.bovinemagnet.pgconsole.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Represents an active alert being tracked for escalation.
 * <p>
 * Active alerts are created when an alert fires and remain active
 * until acknowledged or resolved.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class ActiveAlert {

    private Long id;
    private String alertId;
    private String alertType;
    private String alertSeverity;
    private String alertMessage;
    private String instanceName;
    private Instant firedAt;
    private Instant lastNotificationAt;
    private int currentEscalationTier = 1;
    private Long escalationPolicyId;
    private boolean acknowledged;
    private boolean resolved;
    private Instant resolvedAt;
    private String metadata; // JSON metadata

    public ActiveAlert() {
        this.firedAt = Instant.now();
    }

    public ActiveAlert(String alertId, String alertType, String alertSeverity, String alertMessage) {
        this();
        this.alertId = alertId;
        this.alertType = alertType;
        this.alertSeverity = alertSeverity;
        this.alertMessage = alertMessage;
    }

    /**
     * Gets the duration since the alert fired.
     *
     * @return duration in seconds
     */
    public long getDurationSeconds() {
        if (resolved && resolvedAt != null) {
            return java.time.Duration.between(firedAt, resolvedAt).toSeconds();
        }
        return java.time.Duration.between(firedAt, Instant.now()).toSeconds();
    }

    /**
     * Gets formatted duration.
     *
     * @return formatted duration string
     */
    public String getDurationFormatted() {
        long seconds = getDurationSeconds();
        if (seconds < 60) return seconds + "s";
        if (seconds < 3600) return (seconds / 60) + "m " + (seconds % 60) + "s";
        return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
    }

    /**
     * Gets formatted fired time.
     *
     * @return formatted time
     */
    public String getFiredAtFormatted() {
        if (firedAt == null) return "";
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(firedAt);
    }

    /**
     * Gets severity badge CSS class.
     *
     * @return CSS class
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
     * Gets status badge CSS class.
     *
     * @return CSS class
     */
    public String getStatusCssClass() {
        if (resolved) return "bg-success";
        if (acknowledged) return "bg-info";
        return "bg-danger";
    }

    /**
     * Gets status text.
     *
     * @return status text
     */
    public String getStatusText() {
        if (resolved) return "Resolved";
        if (acknowledged) return "Acknowledged";
        return "Firing";
    }

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }
    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
    public String getAlertSeverity() { return alertSeverity; }
    public void setAlertSeverity(String alertSeverity) { this.alertSeverity = alertSeverity; }
    public String getAlertMessage() { return alertMessage; }
    public void setAlertMessage(String alertMessage) { this.alertMessage = alertMessage; }
    public String getInstanceName() { return instanceName; }
    public void setInstanceName(String instanceName) { this.instanceName = instanceName; }
    public Instant getFiredAt() { return firedAt; }
    public void setFiredAt(Instant firedAt) { this.firedAt = firedAt; }
    public Instant getLastNotificationAt() { return lastNotificationAt; }
    public void setLastNotificationAt(Instant lastNotificationAt) { this.lastNotificationAt = lastNotificationAt; }
    public int getCurrentEscalationTier() { return currentEscalationTier; }
    public void setCurrentEscalationTier(int currentEscalationTier) { this.currentEscalationTier = currentEscalationTier; }
    public Long getEscalationPolicyId() { return escalationPolicyId; }
    public void setEscalationPolicyId(Long escalationPolicyId) { this.escalationPolicyId = escalationPolicyId; }
    public boolean isAcknowledged() { return acknowledged; }
    public void setAcknowledged(boolean acknowledged) { this.acknowledged = acknowledged; }
    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}
