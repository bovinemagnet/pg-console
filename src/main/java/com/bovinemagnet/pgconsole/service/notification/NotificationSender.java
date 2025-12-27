package com.bovinemagnet.pgconsole.service.notification;

import com.bovinemagnet.pgconsole.model.ActiveAlert;
import com.bovinemagnet.pgconsole.model.NotificationChannel;
import com.bovinemagnet.pgconsole.model.NotificationResult;

/**
 * Interface for notification channel implementations.
 * <p>
 * Each notification channel type (Slack, Teams, PagerDuty, etc.) implements
 * this interface to provide channel-specific notification delivery.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public interface NotificationSender {

    /**
     * Gets the channel type this sender handles.
     *
     * @return channel type
     */
    NotificationChannel.ChannelType getChannelType();

    /**
     * Sends a notification for an alert.
     *
     * @param channel the notification channel configuration
     * @param alert the alert to notify about
     * @return result of the notification attempt
     */
    NotificationResult send(NotificationChannel channel, ActiveAlert alert);

    /**
     * Sends a test notification to verify channel configuration.
     *
     * @param channel the notification channel configuration
     * @return result of the test notification
     */
    NotificationResult sendTest(NotificationChannel channel);

    /**
     * Sends a resolution notification when an alert is resolved.
     *
     * @param channel the notification channel configuration
     * @param alert the resolved alert
     * @return result of the notification attempt
     */
    default NotificationResult sendResolution(NotificationChannel channel, ActiveAlert alert) {
        return send(channel, alert);
    }

    /**
     * Validates the channel configuration.
     *
     * @param channel the channel to validate
     * @return true if configuration is valid
     */
    boolean validateConfig(NotificationChannel channel);

    /**
     * Gets the display name for this sender type.
     *
     * @return display name
     */
    default String getDisplayName() {
        return getChannelType().getDisplayName();
    }
}
