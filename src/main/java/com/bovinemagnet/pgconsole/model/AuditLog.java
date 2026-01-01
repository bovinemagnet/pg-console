package com.bovinemagnet.pgconsole.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Represents an audit log entry for tracking admin actions.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class AuditLog {

    public enum Action {
        CANCEL_QUERY("Cancel Query"),
        TERMINATE_CONNECTION("Terminate Connection"),
        RESET_STATS("Reset Statistics"),
        CREATE_BOOKMARK("Create Bookmark"),
        UPDATE_BOOKMARK("Update Bookmark"),
        DELETE_BOOKMARK("Delete Bookmark"),
        CREATE_REPORT("Create Report"),
        UPDATE_REPORT("Update Report"),
        DELETE_REPORT("Delete Report"),
        LOGIN("Login"),
        LOGOUT("Logout"),
        VIEW_SENSITIVE("View Sensitive Data");

        private final String displayName;

        Action(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private long id;
    private Instant timestamp;
    private String instanceId;
    private String username;
    private String action;
    private String targetType;
    private String targetId;
    private Map<String, Object> details;
    private String clientIp;
    private boolean success;
    private String errorMessage;

    public AuditLog() {
        this.timestamp = Instant.now();
        this.success = true;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getTimestampFormatted() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(timestamp);
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setAction(Action action) {
        this.action = action.name();
    }

    public String getActionDisplayName() {
        try {
            return Action.valueOf(action).getDisplayName();
        } catch (Exception e) {
            return action;
        }
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getStatusCssClass() {
        return success ? "bg-success" : "bg-danger";
    }

    public String getStatusDisplay() {
        return success ? "Success" : "Failed";
    }

    public String getActionCssClass() {
        if (action == null) return "bg-secondary";
        return switch (action) {
            case "CANCEL_QUERY", "TERMINATE_CONNECTION" -> "bg-warning text-dark";
            case "RESET_STATS", "DELETE_BOOKMARK", "DELETE_REPORT" -> "bg-danger";
            case "CREATE_BOOKMARK", "CREATE_REPORT" -> "bg-success";
            case "LOGIN" -> "bg-info";
            default -> "bg-secondary";
        };
    }
}
