package com.bovinemagnet.pgconsole.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Represents the result of sending a notification.
 * <p>
 * Used for tracking notification history and debugging failures.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class NotificationResult {

    private Long id;
    private Long channelId;
    private String channelName;
    private NotificationChannel.ChannelType channelType;
    private String alertId;
    private String alertType;
    private String alertSeverity;
    private String alertMessage;
    private String instanceName;
    private Instant sentAt;
    private boolean success;
    private int responseCode;
    private String responseBody;
    private String errorMessage;
    private Integer escalationTier;
    private String dedupKey;

    public NotificationResult() {
        this.sentAt = Instant.now();
    }

    /**
     * Creates a successful result.
     *
     * @param channelId channel ID
     * @param channelName channel name
     * @param channelType channel type
     * @param alertId alert ID
     * @return success result
     */
    public static NotificationResult success(Long channelId, String channelName,
                                              NotificationChannel.ChannelType channelType, String alertId) {
        NotificationResult result = new NotificationResult();
        result.channelId = channelId;
        result.channelName = channelName;
        result.channelType = channelType;
        result.alertId = alertId;
        result.success = true;
        return result;
    }

    /**
     * Creates a failure result.
     *
     * @param channelId channel ID
     * @param channelName channel name
     * @param channelType channel type
     * @param alertId alert ID
     * @param errorMessage error message
     * @return failure result
     */
    public static NotificationResult failure(Long channelId, String channelName,
                                              NotificationChannel.ChannelType channelType, String alertId,
                                              String errorMessage) {
        NotificationResult result = new NotificationResult();
        result.channelId = channelId;
        result.channelName = channelName;
        result.channelType = channelType;
        result.alertId = alertId;
        result.success = false;
        result.errorMessage = errorMessage;
        return result;
    }

    /**
     * Gets the result status CSS class.
     *
     * @return CSS class
     */
    public String getStatusCssClass() {
        return success ? "text-success" : "text-danger";
    }

    /**
     * Gets the result status icon.
     *
     * @return icon class
     */
    public String getStatusIcon() {
        return success ? "bi-check-circle-fill" : "bi-x-circle-fill";
    }

    /**
     * Gets formatted sent time.
     *
     * @return formatted time
     */
    public String getSentAtFormatted() {
        if (sentAt == null) return "";
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(sentAt);
    }

    // Builder methods for fluent API

    public NotificationResult withResponseCode(int code) {
        this.responseCode = code;
        return this;
    }

    public NotificationResult withResponseBody(String body) {
        this.responseBody = body;
        return this;
    }

    public NotificationResult withAlertDetails(String type, String severity, String message, String instance) {
        this.alertType = type;
        this.alertSeverity = severity;
        this.alertMessage = message;
        this.instanceName = instance;
        return this;
    }

    public NotificationResult withEscalationTier(int tier) {
        this.escalationTier = tier;
        return this;
    }

    public NotificationResult withDedupKey(String key) {
        this.dedupKey = key;
        return this;
    }

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getChannelId() { return channelId; }
    public void setChannelId(Long channelId) { this.channelId = channelId; }
    public String getChannelName() { return channelName; }
    public void setChannelName(String channelName) { this.channelName = channelName; }
    public NotificationChannel.ChannelType getChannelType() { return channelType; }
    public void setChannelType(NotificationChannel.ChannelType channelType) { this.channelType = channelType; }
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
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public int getResponseCode() { return responseCode; }
    public void setResponseCode(int responseCode) { this.responseCode = responseCode; }
    public String getResponseBody() { return responseBody; }
    public void setResponseBody(String responseBody) { this.responseBody = responseBody; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Integer getEscalationTier() { return escalationTier; }
    public void setEscalationTier(Integer escalationTier) { this.escalationTier = escalationTier; }
    public String getDedupKey() { return dedupKey; }
    public void setDedupKey(String dedupKey) { this.dedupKey = dedupKey; }
}
