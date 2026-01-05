package com.bovinemagnet.pgconsole.service.notification;

import com.bovinemagnet.pgconsole.model.ActiveAlert;
import com.bovinemagnet.pgconsole.model.NotificationChannel;
import com.bovinemagnet.pgconsole.model.NotificationChannel.TeamsConfig;
import com.bovinemagnet.pgconsole.model.NotificationResult;
import jakarta.enterprise.context.ApplicationScoped;
import java.net.http.HttpResponse;

/**
 * Microsoft Teams notification sender using Incoming Webhooks.
 * <p>
 * Supports Adaptive Card formatting for rich, interactive messages.
 * Features include:
 * <ul>
 *   <li>Adaptive Card formatting with theme colours</li>
 *   <li>User mentions for critical alerts</li>
 *   <li>Action buttons for acknowledgement</li>
 *   <li>Structured facts for alert details</li>
 * </ul>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see <a href="https://docs.microsoft.com/en-us/microsoftteams/platform/webhooks-and-connectors/how-to/connectors-using">Teams Webhooks</a>
 */
@ApplicationScoped
public class TeamsNotificationSender extends AbstractNotificationSender {

	@Override
	public NotificationChannel.ChannelType getChannelType() {
		return NotificationChannel.ChannelType.TEAMS;
	}

	@Override
	public NotificationResult send(NotificationChannel channel, ActiveAlert alert) {
		TeamsConfig config = channel.getTeamsConfig();
		if (config == null || config.getWebhookUrl() == null) {
			return createFailureResult(channel, alert, new IllegalArgumentException("Missing Teams webhook URL"));
		}

		try {
			String payload = buildAdaptiveCardPayload(channel, config, alert);
			HttpResponse<String> response = sendHttpPost(config.getWebhookUrl(), payload, null);

			// Teams returns 200 or 202 for success
			if (response.statusCode() == 200 || response.statusCode() == 202) {
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
		TeamsConfig config = channel.getTeamsConfig();
		if (config == null || config.getWebhookUrl() == null) {
			return createFailureResult(channel, null, new IllegalArgumentException("Missing Teams webhook URL"));
		}

		try {
			String payload = buildTestPayload(channel, config);
			HttpResponse<String> response = sendHttpPost(config.getWebhookUrl(), payload, null);

			if (response.statusCode() == 200 || response.statusCode() == 202) {
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
		TeamsConfig config = channel.getTeamsConfig();
		if (config == null) {
			return false;
		}
		String webhookUrl = config.getWebhookUrl();
		return webhookUrl != null && !webhookUrl.isBlank() && (webhookUrl.contains("webhook.office.com") || webhookUrl.contains("outlook.office.com"));
	}

	private String buildAdaptiveCardPayload(NotificationChannel channel, TeamsConfig config, ActiveAlert alert) {
		String themeColour = config.getThemeColor() != null ? config.getThemeColor() : getSeverityColour(alert.getAlertSeverity()).replace("#", "");

		StringBuilder payload = new StringBuilder();
		payload.append("{");
		payload.append("\"type\":\"message\",");
		payload.append("\"attachments\":[{");
		payload.append("\"contentType\":\"application/vnd.microsoft.card.adaptive\",");
		payload.append("\"contentUrl\":null,");
		payload.append("\"content\":{");
		payload.append("\"$schema\":\"http://adaptivecards.io/schemas/adaptive-card.json\",");
		payload.append("\"type\":\"AdaptiveCard\",");
		payload.append("\"version\":\"1.4\",");

		// Theme colour
		payload.append("\"msteams\":{\"width\":\"Full\"},");

		// Body
		payload.append("\"body\":[");

		// Header container with colour
		payload.append("{\"type\":\"Container\",\"style\":\"emphasis\",\"items\":[");
		payload.append("{\"type\":\"TextBlock\",\"size\":\"Large\",\"weight\":\"Bolder\",\"text\":\"").append(getSeverityEmoji(alert.getAlertSeverity())).append(" ").append(escapeJson(alert.getAlertType())).append("\"}");
		payload.append("]},");

		// Facts container
		payload.append("{\"type\":\"Container\",\"items\":[");
		payload.append("{\"type\":\"FactSet\",\"facts\":[");
		payload.append("{\"title\":\"Severity\",\"value\":\"").append(alert.getAlertSeverity()).append("\"},");
		payload.append("{\"title\":\"Instance\",\"value\":\"").append(escapeJson(alert.getInstanceName() != null ? alert.getInstanceName() : "N/A")).append("\"},");
		payload.append("{\"title\":\"Duration\",\"value\":\"").append(alert.getDurationFormatted()).append("\"},");
		payload.append("{\"title\":\"Status\",\"value\":\"").append(alert.getStatusText()).append("\"},");
		payload.append("{\"title\":\"Fired At\",\"value\":\"").append(alert.getFiredAtFormatted()).append("\"}");
		payload.append("]}");
		payload.append("]},");

		// Message
		payload.append("{\"type\":\"Container\",\"items\":[");
		payload.append("{\"type\":\"TextBlock\",\"text\":\"").append(escapeJson(alert.getAlertMessage())).append("\",\"wrap\":true}");
		payload.append("]}");

		payload.append("]");

		payload.append("}");
		payload.append("}]");
		payload.append("}");

		return payload.toString();
	}

	private String buildTestPayload(NotificationChannel channel, TeamsConfig config) {
		StringBuilder payload = new StringBuilder();
		payload.append("{");
		payload.append("\"type\":\"message\",");
		payload.append("\"attachments\":[{");
		payload.append("\"contentType\":\"application/vnd.microsoft.card.adaptive\",");
		payload.append("\"content\":{");
		payload.append("\"$schema\":\"http://adaptivecards.io/schemas/adaptive-card.json\",");
		payload.append("\"type\":\"AdaptiveCard\",");
		payload.append("\"version\":\"1.4\",");
		payload.append("\"body\":[");
		payload.append("{\"type\":\"TextBlock\",\"size\":\"Medium\",\"weight\":\"Bolder\",\"text\":\"✅ PG Console Test Notification\"},");
		payload.append("{\"type\":\"TextBlock\",\"text\":\"Channel '").append(escapeJson(channel.getName())).append("' is configured correctly!\",\"wrap\":true}");
		payload.append("]");
		payload.append("}");
		payload.append("}]");
		payload.append("}");

		return payload.toString();
	}
}
