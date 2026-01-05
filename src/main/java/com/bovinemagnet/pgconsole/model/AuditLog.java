package com.bovinemagnet.pgconsole.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Represents an audit log entry for tracking administrative actions and security events
 * within the PostgreSQL console application.
 * <p>
 * This class captures details of user actions including database operations (query cancellation,
 * connection termination), configuration changes (bookmark and report management), and
 * authentication events (login/logout). Each audit log entry records who performed the action,
 * when it occurred, whether it succeeded, and contextual details for compliance and security
 * monitoring purposes.
 * <p>
 * Audit logs are persisted to the database and can be queried for security auditing,
 * troubleshooting administrative actions, and compliance reporting.
 *
 * @author Paul Snow
 * @since 0.0.0
 * @see AuditLog.Action
 */
public class AuditLog {

    /**
     * Enumeration of auditable actions that can be tracked within the application.
     * <p>
     * Each action represents a significant administrative or security-relevant operation
     * performed by a user. Actions include database management operations, configuration
     * changes, and authentication events.
     */
    public enum Action {
        /** Cancels an actively running query in PostgreSQL */
        CANCEL_QUERY("Cancel Query"),

        /** Terminates an active database connection */
        TERMINATE_CONNECTION("Terminate Connection"),

        /** Resets PostgreSQL statistics counters */
        RESET_STATS("Reset Statistics"),

        /** Creates a new query or dashboard bookmark */
        CREATE_BOOKMARK("Create Bookmark"),

        /** Updates an existing bookmark */
        UPDATE_BOOKMARK("Update Bookmark"),

        /** Deletes a bookmark */
        DELETE_BOOKMARK("Delete Bookmark"),

        /** Creates a new report definition */
        CREATE_REPORT("Create Report"),

        /** Updates an existing report definition */
        UPDATE_REPORT("Update Report"),

        /** Deletes a report definition */
        DELETE_REPORT("Delete Report"),

        /** Records a successful user login */
        LOGIN("Login"),

        /** Records a user logout */
        LOGOUT("Logout"),

        /** Records access to sensitive data such as query parameters or connection strings */
        VIEW_SENSITIVE("View Sensitive Data");

        private final String displayName;

        /**
         * Constructs an Action with the specified display name.
         *
         * @param displayName the human-readable name for this action
         */
        Action(String displayName) {
            this.displayName = displayName;
        }

        /**
         * Returns the human-readable display name for this action.
         * <p>
         * This is suitable for displaying in user interfaces and audit reports.
         *
         * @return the display name, never null
         */
        public String getDisplayName() {
            return displayName;
        }
    }

    /** Unique identifier for this audit log entry */
    private long id;

    /** Timestamp when the audited action occurred */
    private Instant timestamp;

    /** Identifier of the PostgreSQL instance where the action was performed */
    private String instanceId;

    /** Username of the user who performed the action */
    private String username;

    /** String representation of the action performed (typically an Action enum name) */
    private String action;

    /** Type of the target entity affected by the action (e.g., "query", "connection", "bookmark") */
    private String targetType;

    /** Identifier of the specific target entity affected */
    private String targetId;

    /** Additional contextual details about the action as key-value pairs */
    private Map<String, Object> details;

    /** IP address of the client that initiated the action */
    private String clientIp;

    /** Whether the action completed successfully */
    private boolean success;

    /** Error message if the action failed, null otherwise */
    private String errorMessage;

    /**
     * Constructs a new AuditLog entry with default values.
     * <p>
     * The timestamp is initialised to the current instant and success is defaulted to true.
     * Other fields should be set using the appropriate setter methods.
     */
    public AuditLog() {
        this.timestamp = Instant.now();
        this.success = true;
    }

    /**
     * Returns the unique identifier for this audit log entry.
     *
     * @return the audit log ID
     */
    public long getId() {
        return id;
    }

    /**
     * Sets the unique identifier for this audit log entry.
     *
     * @param id the audit log ID
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Returns the timestamp when the audited action occurred.
     *
     * @return the action timestamp, never null for valid audit logs
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp when the audited action occurred.
     *
     * @param timestamp the action timestamp
     */
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Returns the timestamp formatted as a human-readable string.
     * <p>
     * The format used is "yyyy-MM-dd HH:mm:ss" in the system's default time zone.
     * This method is convenient for display in user interfaces and reports.
     *
     * @return the formatted timestamp string
     */
    public String getTimestampFormatted() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(timestamp);
    }

    /**
     * Returns the identifier of the PostgreSQL instance where the action was performed.
     *
     * @return the instance ID, or null if not set
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * Sets the identifier of the PostgreSQL instance where the action was performed.
     *
     * @param instanceId the instance ID
     */
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    /**
     * Returns the username of the user who performed the action.
     *
     * @return the username, or null if not set
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username of the user who performed the action.
     *
     * @param username the username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Returns the string representation of the action performed.
     * <p>
     * This is typically the name of an {@link Action} enum constant.
     *
     * @return the action name, or null if not set
     */
    public String getAction() {
        return action;
    }

    /**
     * Sets the action performed using a string representation.
     *
     * @param action the action name
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * Sets the action performed using an Action enum constant.
     * <p>
     * This method converts the enum to its name and stores it as a string.
     *
     * @param action the action enum constant
     */
    public void setAction(Action action) {
        this.action = action.name();
    }

    /**
     * Returns the human-readable display name for the action.
     * <p>
     * If the action string corresponds to a valid {@link Action} enum constant,
     * returns its display name. Otherwise, returns the raw action string.
     *
     * @return the action display name, or the raw action string if not a valid Action
     */
    public String getActionDisplayName() {
        try {
            return Action.valueOf(action).getDisplayName();
        } catch (Exception e) {
            return action;
        }
    }

    /**
     * Returns the type of the target entity affected by the action.
     *
     * @return the target type (e.g., "query", "connection", "bookmark"), or null if not set
     */
    public String getTargetType() {
        return targetType;
    }

    /**
     * Sets the type of the target entity affected by the action.
     *
     * @param targetType the target type (e.g., "query", "connection", "bookmark")
     */
    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    /**
     * Returns the identifier of the specific target entity affected.
     *
     * @return the target ID, or null if not set
     */
    public String getTargetId() {
        return targetId;
    }

    /**
     * Sets the identifier of the specific target entity affected.
     *
     * @param targetId the target ID
     */
    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    /**
     * Returns additional contextual details about the action.
     * <p>
     * Details are stored as key-value pairs and can include information such as
     * query text, connection parameters, or other action-specific metadata.
     *
     * @return the details map, or null if not set
     */
    public Map<String, Object> getDetails() {
        return details;
    }

    /**
     * Sets additional contextual details about the action.
     *
     * @param details the details map containing action-specific metadata
     */
    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    /**
     * Returns the IP address of the client that initiated the action.
     *
     * @return the client IP address, or null if not set
     */
    public String getClientIp() {
        return clientIp;
    }

    /**
     * Sets the IP address of the client that initiated the action.
     *
     * @param clientIp the client IP address
     */
    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    /**
     * Returns whether the action completed successfully.
     *
     * @return true if the action succeeded, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Sets whether the action completed successfully.
     *
     * @param success true if the action succeeded, false otherwise
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * Returns the error message if the action failed.
     *
     * @return the error message, or null if the action succeeded
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the error message for a failed action.
     *
     * @param errorMessage the error message, or null if the action succeeded
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Returns the Bootstrap CSS class appropriate for displaying the action status.
     * <p>
     * This method is useful for applying colour-coding in user interfaces:
     * <ul>
     *     <li>Success: "bg-success" (green background)</li>
     *     <li>Failure: "bg-danger" (red background)</li>
     * </ul>
     *
     * @return the Bootstrap CSS class name for the status
     */
    public String getStatusCssClass() {
        return success ? "bg-success" : "bg-danger";
    }

    /**
     * Returns a human-readable display text for the action status.
     *
     * @return "Success" if the action succeeded, "Failed" otherwise
     */
    public String getStatusDisplay() {
        return success ? "Success" : "Failed";
    }

    /**
     * Returns the Bootstrap CSS class appropriate for displaying the action type.
     * <p>
     * Different action types are colour-coded for quick visual identification:
     * <ul>
     *     <li>Potentially destructive actions (CANCEL_QUERY, TERMINATE_CONNECTION): "bg-warning text-dark" (yellow)</li>
     *     <li>Destructive actions (RESET_STATS, DELETE_*): "bg-danger" (red)</li>
     *     <li>Creation actions (CREATE_*): "bg-success" (green)</li>
     *     <li>Authentication actions (LOGIN): "bg-info" (blue)</li>
     *     <li>Other actions: "bg-secondary" (grey)</li>
     * </ul>
     *
     * @return the Bootstrap CSS class name for the action type, or "bg-secondary" if action is null
     */
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
