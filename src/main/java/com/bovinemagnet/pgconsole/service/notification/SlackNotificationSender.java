package com.bovinemagnet.pgconsole.service.notification;

import com.bovinemagnet.pgconsole.model.ActiveAlert;
import com.bovinemagnet.pgconsole.model.NotificationChannel;
import com.bovinemagnet.pgconsole.model.NotificationChannel.SlackConfig;
import com.bovinemagnet.pgconsole.model.NotificationResult;
import jakarta.enterprise.context.ApplicationScoped;
import java.net.http.HttpResponse;

/**
 * Slack notification sender using Incoming Webhooks.
 * <p>
 * Supports both simple text messages and Block Kit for rich formatting.
 * Features include:
 * <ul>
 *   <li>Rich message formatting with severity colours</li>
 *   <li>Channel routing and overrides</li>
 *   <li>@channel mentions for critical alerts</li>
 *   <li>Block Kit support for interactive elements</li>
 * </ul>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see <a href="https://api.slack.com/messaging/webhooks">Slack Webhooks</a>
 */
@ApplicationScoped
public class SlackNotificationSender extends AbstractNotificationSender {

	@Override
	public NotificationChannel.ChannelType getChannelType() {
		return NotificationChannel.ChannelType.SLACK;
	}

	@Override
	public NotificationResult send(NotificationChannel channel, ActiveAlert alert) {
		SlackConfig config = channel.getSlackConfig();
		if (config == null || config.getWebhookUrl() == null) {
			return createFailureResult(channel, alert, new IllegalArgumentException("Missing Slack webhook URL"));
		}

		try {
			String payload;
			if (config.isUseBlocks()) {
				payload = buildBlockKitPayload(channel, config, alert);
			} else {
				payload = buildSimplePayload(channel, config, alert);
			}

			HttpResponse<String> response = sendHttpPost(config.getWebhookUrl(), payload, null);

			if (response.statusCode() == 200) {
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
		SlackConfig config = channel.getSlackConfig();
		if (config == null || config.getWebhookUrl() == null) {
			return createFailureResult(channel, null, new IllegalArgumentException("Missing Slack webhook URL"));
		}

		try {
			String payload = buildTestPayload(channel, config);
			HttpResponse<String> response = sendHttpPost(config.getWebhookUrl(), payload, null);

			if (response.statusCode() == 200) {
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
		SlackConfig config = channel.getSlackConfig();
		if (config == null) {
			return false;
		}
		String webhookUrl = config.getWebhookUrl();
		return webhookUrl != null && !webhookUrl.isBlank() && webhookUrl.startsWith("https://hooks.slack.com/");
	}

	private String buildSimplePayload(NotificationChannel channel, SlackConfig config, ActiveAlert alert) {
		StringBuilder payload = new StringBuilder();
		payload.append("{");

		// Optional channel override
		if (config.getChannel() != null && !config.getChannel().isBlank()) {
			payload.append("\"channel\":\"").append(escapeJson(config.getChannel())).append("\",");
		}

		// Optional username
		if (config.getUsername() != null && !config.getUsername().isBlank()) {
			payload.append("\"username\":\"").append(escapeJson(config.getUsername())).append("\",");
		}

		// Optional icon emoji
		if (config.getIconEmoji() != null && !config.getIconEmoji().isBlank()) {
			payload.append("\"icon_emoji\":\"").append(escapeJson(config.getIconEmoji())).append("\",");
		}

		// Build message text
		StringBuilder text = new StringBuilder();

		// Add @channel mention for critical alerts
		if (config.isMentionChannel() && "CRITICAL".equalsIgnoreCase(alert.getAlertSeverity())) {
			text.append("<!channel> ");
		}

		text.append(getSeverityEmoji(alert.getAlertSeverity())).append(" *").append(escapeJson(alert.getAlertType())).append("*").append(" [").append(alert.getAlertSeverity()).append("]\\n").append(escapeJson(alert.getAlertMessage()));

		if (alert.getInstanceName() != null) {
			text.append("\\n_Instance: ").append(escapeJson(alert.getInstanceName())).append("_");
		}

		text.append("\\n_Duration: ").append(alert.getDurationFormatted()).append("_");

		payload.append("\"text\":\"").append(text).append("\"");
		payload.append("}");

		return payload.toString();
	}

	private String buildBlockKitPayload(NotificationChannel channel, SlackConfig config, ActiveAlert alert) {
		StringBuilder payload = new StringBuilder();
		payload.append("{");

		// Optional channel override
		if (config.getChannel() != null && !config.getChannel().isBlank()) {
			payload.append("\"channel\":\"").append(escapeJson(config.getChannel())).append("\",");
		}

		// Optional username
		if (config.getUsername() != null && !config.getUsername().isBlank()) {
			payload.append("\"username\":\"").append(escapeJson(config.getUsername())).append("\",");
		}

		// Fallback text
		payload.append("\"text\":\"").append(getSeverityEmoji(alert.getAlertSeverity())).append(" ").append(escapeJson(alert.getAlertType())).append(": ").append(escapeJson(alert.getAlertMessage())).append("\",");

		// Blocks
		payload.append("\"blocks\":[");

		// Header block
		payload.append("{\"type\":\"header\",\"text\":{\"type\":\"plain_text\",\"text\":\"").append(getSeverityEmoji(alert.getAlertSeverity())).append(" ").append(escapeJson(alert.getAlertType())).append("\",\"emoji\":true}},");

		// Section with alert details
		payload.append("{\"type\":\"section\",\"fields\":[");
		payload.append("{\"type\":\"mrkdwn\",\"text\":\"*Severity:*\\n").append(alert.getAlertSeverity()).append("\"},");
		payload.append("{\"type\":\"mrkdwn\",\"text\":\"*Instance:*\\n").append(escapeJson(alert.getInstanceName() != null ? alert.getInstanceName() : "N/A")).append("\"},");
		payload.append("{\"type\":\"mrkdwn\",\"text\":\"*Duration:*\\n").append(alert.getDurationFormatted()).append("\"},");
		payload.append("{\"type\":\"mrkdwn\",\"text\":\"*Status:*\\n").append(alert.getStatusText()).append("\"}");
		payload.append("]},");

		// Message section
		payload.append("{\"type\":\"section\",\"text\":{\"type\":\"mrkdwn\",\"text\":\"").append(escapeJson(alert.getAlertMessage())).append("\"}},");

		// Divider
		payload.append("{\"type\":\"divider\"},");

		// Context with timestamp
		payload.append("{\"type\":\"context\",\"elements\":[");
		payload.append("{\"type\":\"mrkdwn\",\"text\":\"Fired at: ").append(alert.getFiredAtFormatted()).append("\"}");
		payload.append("]}");

		payload.append("],");

		// Attachment for colour strip
		payload.append("\"attachments\":[{\"color\":\"").append(getSeverityColour(alert.getAlertSeverity())).append("\"}]");

		payload.append("}");

		return payload.toString();
	}

	private String buildTestPayload(NotificationChannel channel, SlackConfig config) {
		StringBuilder payload = new StringBuilder();
		payload.append("{");

		if (config.getChannel() != null && !config.getChannel().isBlank()) {
			payload.append("\"channel\":\"").append(escapeJson(config.getChannel())).append("\",");
		}

		if (config.getUsername() != null && !config.getUsername().isBlank()) {
			payload.append("\"username\":\"").append(escapeJson(config.getUsername())).append("\",");
		}

		payload.append("\"text\":\"").append("✅ Test notification from PG Console - Channel '").append(escapeJson(channel.getName())).append("' is configured correctly!\"");
		payload.append("}");

		return payload.toString();
	}
}
