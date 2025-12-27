package com.bovinemagnet.pgconsole.service.notification;

import com.bovinemagnet.pgconsole.model.ActiveAlert;
import com.bovinemagnet.pgconsole.model.NotificationChannel;
import com.bovinemagnet.pgconsole.model.NotificationChannel.DiscordConfig;
import com.bovinemagnet.pgconsole.model.NotificationResult;
import jakarta.enterprise.context.ApplicationScoped;

import java.net.http.HttpResponse;
import java.util.List;

/**
 * Discord notification sender using Webhooks.
 * <p>
 * Supports rich embed formatting for clear, visually appealing notifications.
 * Features include:
 * <ul>
 *   <li>Rich embed formatting with severity colours</li>
 *   <li>@everyone mentions for critical alerts</li>
 *   <li>Role mentions for specific alert types</li>
 *   <li>Custom username and avatar</li>
 * </ul>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see <a href="https://discord.com/developers/docs/resources/webhook">Discord Webhooks</a>
 */
@ApplicationScoped
public class DiscordNotificationSender extends AbstractNotificationSender {

    @Override
    public NotificationChannel.ChannelType getChannelType() {
        return NotificationChannel.ChannelType.DISCORD;
    }

    @Override
    public NotificationResult send(NotificationChannel channel, ActiveAlert alert) {
        DiscordConfig config = channel.getDiscordConfig();
        if (config == null || config.getWebhookUrl() == null) {
            return createFailureResult(channel, alert, new IllegalArgumentException("Missing Discord webhook URL"));
        }

        try {
            String payload;
            if (config.isUseEmbeds()) {
                payload = buildEmbedPayload(channel, config, alert);
            } else {
                payload = buildSimplePayload(channel, config, alert);
            }

            HttpResponse<String> response = sendHttpPost(config.getWebhookUrl(), payload, null);

            // Discord returns 204 No Content on success
            if (response.statusCode() == 204 || response.statusCode() == 200) {
                logNotification(channel, alert, true);
                return createSuccessResult(channel, alert, response);
            } else {
                logNotification(channel, alert, false);
                return createFailureResult(channel, alert, response);
            }
        } catch (Exception e) {
            logNotification(channel, alert, false);
            return createFailureResult(channel, alert, e);
        }
    }

    @Override
    public NotificationResult sendTest(NotificationChannel channel) {
        DiscordConfig config = channel.getDiscordConfig();
        if (config == null || config.getWebhookUrl() == null) {
            return createFailureResult(channel, null, new IllegalArgumentException("Missing Discord webhook URL"));
        }

        try {
            String payload = buildTestPayload(channel, config);
            HttpResponse<String> response = sendHttpPost(config.getWebhookUrl(), payload, null);

            if (response.statusCode() == 204 || response.statusCode() == 200) {
                return createSuccessResult(channel, null, response);
            } else {
                return createFailureResult(channel, null, response);
            }
        } catch (Exception e) {
            return createFailureResult(channel, null, e);
        }
    }

    @Override
    public boolean validateConfig(NotificationChannel channel) {
        DiscordConfig config = channel.getDiscordConfig();
        if (config == null) {
            return false;
        }
        String webhookUrl = config.getWebhookUrl();
        return webhookUrl != null &&
               !webhookUrl.isBlank() &&
               webhookUrl.contains("discord.com/api/webhooks/");
    }

    private String buildSimplePayload(NotificationChannel channel, DiscordConfig config, ActiveAlert alert) {
        StringBuilder payload = new StringBuilder();
        payload.append("{");

        // Optional username
        if (config.getUsername() != null && !config.getUsername().isBlank()) {
            payload.append("\"username\":\"").append(escapeJson(config.getUsername())).append("\",");
        }

        // Optional avatar
        if (config.getAvatarUrl() != null && !config.getAvatarUrl().isBlank()) {
            payload.append("\"avatar_url\":\"").append(escapeJson(config.getAvatarUrl())).append("\",");
        }

        // Build content
        StringBuilder content = new StringBuilder();

        // Add @everyone or role mentions for critical alerts
        if ("CRITICAL".equalsIgnoreCase(alert.getAlertSeverity())) {
            if (config.isMentionEveryone()) {
                content.append("@everyone ");
            } else if (config.getMentionRoleIds() != null && !config.getMentionRoleIds().isEmpty()) {
                for (String roleId : config.getMentionRoleIds()) {
                    content.append("<@&").append(roleId).append("> ");
                }
            }
        }

        content.append(getSeverityEmoji(alert.getAlertSeverity()))
               .append(" **").append(escapeJson(alert.getAlertType())).append("** [")
               .append(alert.getAlertSeverity()).append("]\\n")
               .append(escapeJson(alert.getAlertMessage()));

        if (alert.getInstanceName() != null) {
            content.append("\\n*Instance:* ").append(escapeJson(alert.getInstanceName()));
        }

        content.append("\\n*Duration:* ").append(alert.getDurationFormatted());

        payload.append("\"content\":\"").append(content).append("\"");
        payload.append("}");

        return payload.toString();
    }

    private String buildEmbedPayload(NotificationChannel channel, DiscordConfig config, ActiveAlert alert) {
        StringBuilder payload = new StringBuilder();
        payload.append("{");

        // Optional username
        if (config.getUsername() != null && !config.getUsername().isBlank()) {
            payload.append("\"username\":\"").append(escapeJson(config.getUsername())).append("\",");
        }

        // Optional avatar
        if (config.getAvatarUrl() != null && !config.getAvatarUrl().isBlank()) {
            payload.append("\"avatar_url\":\"").append(escapeJson(config.getAvatarUrl())).append("\",");
        }

        // Content with mentions for critical
        StringBuilder content = new StringBuilder();
        if ("CRITICAL".equalsIgnoreCase(alert.getAlertSeverity())) {
            if (config.isMentionEveryone()) {
                content.append("@everyone ");
            } else if (config.getMentionRoleIds() != null && !config.getMentionRoleIds().isEmpty()) {
                for (String roleId : config.getMentionRoleIds()) {
                    content.append("<@&").append(roleId).append("> ");
                }
            }
        }
        if (content.length() > 0) {
            payload.append("\"content\":\"").append(content.toString().trim()).append("\",");
        }

        // Embed
        payload.append("\"embeds\":[{");

        // Title
        payload.append("\"title\":\"").append(getSeverityEmoji(alert.getAlertSeverity())).append(" ")
               .append(escapeJson(alert.getAlertType())).append("\",");

        // Description
        payload.append("\"description\":\"").append(escapeJson(alert.getAlertMessage())).append("\",");

        // Colour (Discord uses decimal colour)
        int colour = parseHexColour(getSeverityColour(alert.getAlertSeverity()));
        payload.append("\"color\":").append(colour).append(",");

        // Fields
        payload.append("\"fields\":[");
        payload.append("{\"name\":\"Severity\",\"value\":\"").append(alert.getAlertSeverity()).append("\",\"inline\":true},");
        payload.append("{\"name\":\"Instance\",\"value\":\"").append(escapeJson(alert.getInstanceName() != null ? alert.getInstanceName() : "N/A")).append("\",\"inline\":true},");
        payload.append("{\"name\":\"Duration\",\"value\":\"").append(alert.getDurationFormatted()).append("\",\"inline\":true},");
        payload.append("{\"name\":\"Status\",\"value\":\"").append(alert.getStatusText()).append("\",\"inline\":true}");
        payload.append("],");

        // Footer
        payload.append("\"footer\":{\"text\":\"PG Console Alert\"},");

        // Timestamp
        payload.append("\"timestamp\":\"").append(alert.getFiredAt().toString()).append("\"");

        payload.append("}]");
        payload.append("}");

        return payload.toString();
    }

    private String buildTestPayload(NotificationChannel channel, DiscordConfig config) {
        StringBuilder payload = new StringBuilder();
        payload.append("{");

        if (config.getUsername() != null && !config.getUsername().isBlank()) {
            payload.append("\"username\":\"").append(escapeJson(config.getUsername())).append("\",");
        }

        if (config.getAvatarUrl() != null && !config.getAvatarUrl().isBlank()) {
            payload.append("\"avatar_url\":\"").append(escapeJson(config.getAvatarUrl())).append("\",");
        }

        if (config.isUseEmbeds()) {
            payload.append("\"embeds\":[{");
            payload.append("\"title\":\"✅ PG Console Test Notification\",");
            payload.append("\"description\":\"Channel '").append(escapeJson(channel.getName())).append("' is configured correctly!\",");
            payload.append("\"color\":3066993"); // Green
            payload.append("}]");
        } else {
            payload.append("\"content\":\"✅ Test notification from PG Console - Channel '")
                   .append(escapeJson(channel.getName())).append("' is configured correctly!\"");
        }

        payload.append("}");

        return payload.toString();
    }

    private int parseHexColour(String hexColour) {
        try {
            String hex = hexColour.replace("#", "");
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            return 8421504; // Grey
        }
    }
}
