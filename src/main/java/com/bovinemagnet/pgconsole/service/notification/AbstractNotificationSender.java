package com.bovinemagnet.pgconsole.service.notification;

import com.bovinemagnet.pgconsole.logging.StructuredLogger;
import com.bovinemagnet.pgconsole.model.ActiveAlert;
import com.bovinemagnet.pgconsole.model.NotificationChannel;
import com.bovinemagnet.pgconsole.model.NotificationResult;
import jakarta.inject.Inject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Abstract base class for notification senders with common HTTP functionality.
 * <p>
 * Provides shared utilities for webhook-based notification channels including
 * HTTP client management, request building, and error handling.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public abstract class AbstractNotificationSender implements NotificationSender {

	protected static final int DEFAULT_TIMEOUT_SECONDS = 30;
	protected static final String CONTENT_TYPE_JSON = "application/json";

	protected final HttpClient httpClient;

	@Inject
	protected StructuredLogger logger;

	protected AbstractNotificationSender() {
		this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
	}

	/**
	 * Sends an HTTP POST request with JSON body.
	 *
	 * @param url the webhook URL
	 * @param jsonBody the JSON request body
	 * @param headers additional headers
	 * @return HTTP response
	 * @throws Exception if request fails
	 */
	protected HttpResponse<String> sendHttpPost(String url, String jsonBody, Map<String, String> headers) throws Exception {
		HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS)).header("Content-Type", CONTENT_TYPE_JSON).POST(HttpRequest.BodyPublishers.ofString(jsonBody));

		if (headers != null) {
			headers.forEach(requestBuilder::header);
		}

		return httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
	}

	/**
	 * Creates a success result from an HTTP response.
	 *
	 * @param channel the notification channel
	 * @param alert the alert
	 * @param response the HTTP response
	 * @return success result
	 */
	/**
	 * Creates a success result for a no-op send that never made an HTTP call
	 * (e.g. a PagerDuty resolution when auto-resolve is disabled). Avoids the
	 * NPE that arises from passing a null {@link HttpResponse} to the
	 * response-taking overload.
	 *
	 * @param channel the notification channel
	 * @param alert the alert (may be null)
	 * @return a success result with no response code
	 */
	protected NotificationResult createSuccessResult(NotificationChannel channel, ActiveAlert alert) {
		return NotificationResult.success(channel.getId(), channel.getName(), channel.getChannelType(), alert != null ? alert.getAlertId() : "test")
			.withAlertDetails(alert != null ? alert.getAlertType() : "TEST", alert != null ? alert.getAlertSeverity() : "INFO", alert != null ? alert.getAlertMessage() : "Test notification", alert != null ? alert.getInstanceName() : "test");
	}

	protected NotificationResult createSuccessResult(NotificationChannel channel, ActiveAlert alert, HttpResponse<String> response) {
		// Deliberately do NOT echo the remote response body back to the caller: for
		// an SSRF-style probe that would leak internal service responses. Only the
		// status code is retained.
		return NotificationResult.success(channel.getId(), channel.getName(), channel.getChannelType(), alert != null ? alert.getAlertId() : "test")
			.withResponseCode(response.statusCode())
			.withAlertDetails(alert != null ? alert.getAlertType() : "TEST", alert != null ? alert.getAlertSeverity() : "INFO", alert != null ? alert.getAlertMessage() : "Test notification", alert != null ? alert.getInstanceName() : "test");
	}

	/**
	 * Creates a failure result from an exception.
	 *
	 * @param channel the notification channel
	 * @param alert the alert
	 * @param error the exception
	 * @return failure result
	 */
	protected NotificationResult createFailureResult(NotificationChannel channel, ActiveAlert alert, Throwable error) {
		return NotificationResult.failure(channel.getId(), channel.getName(), channel.getChannelType(), alert != null ? alert.getAlertId() : "test", error.getMessage()).withAlertDetails(
			alert != null ? alert.getAlertType() : "TEST",
			alert != null ? alert.getAlertSeverity() : "INFO",
			alert != null ? alert.getAlertMessage() : "Test notification",
			alert != null ? alert.getInstanceName() : "test"
		);
	}

	/**
	 * Creates a failure result from an HTTP response.
	 *
	 * @param channel the notification channel
	 * @param alert the alert
	 * @param response the HTTP response
	 * @return failure result
	 */
	protected NotificationResult createFailureResult(NotificationChannel channel, ActiveAlert alert, HttpResponse<String> response) {
		// As with success, the remote body is not echoed back to the caller; only the
		// HTTP status code is reported so an SSRF probe cannot read internal responses.
		return NotificationResult.failure(channel.getId(), channel.getName(), channel.getChannelType(), alert != null ? alert.getAlertId() : "test", "HTTP " + response.statusCode())
			.withResponseCode(response.statusCode())
			.withAlertDetails(alert != null ? alert.getAlertType() : "TEST", alert != null ? alert.getAlertSeverity() : "INFO", alert != null ? alert.getAlertMessage() : "Test notification", alert != null ? alert.getInstanceName() : "test");
	}

	/**
	 * Gets severity colour for visual formatting.
	 *
	 * @param severity alert severity
	 * @return hex colour code
	 */
	protected String getSeverityColour(String severity) {
		if (severity == null) {
			return "#808080"; // Grey
		}
		return switch (severity.toUpperCase()) {
			case "CRITICAL" -> "#FF0000"; // Red
			case "HIGH" -> "#FF8C00"; // Dark Orange
			case "MEDIUM", "WARNING" -> "#FFD700"; // Gold
			case "LOW", "INFO" -> "#00CED1"; // Dark Cyan
			default -> "#808080"; // Grey
		};
	}

	/**
	 * Gets severity emoji for text formatting.
	 *
	 * @param severity alert severity
	 * @return emoji string
	 */
	protected String getSeverityEmoji(String severity) {
		if (severity == null) {
			return "ℹ️";
		}
		return switch (severity.toUpperCase()) {
			case "CRITICAL" -> "🔴";
			case "HIGH" -> "🟠";
			case "MEDIUM", "WARNING" -> "🟡";
			case "LOW", "INFO" -> "🟢";
			default -> "ℹ️";
		};
	}

	/**
	 * Escapes a string for safe inclusion inside a JSON string literal.
	 * <p>
	 * Escapes the structural characters (backslash, double-quote) and every
	 * control character below {@code 0x20} as a {@code \\uXXXX} sequence. This
	 * prevents an attacker-supplied field (e.g. an alert severity such as
	 * {@code HIGH","text":"...}) from breaking out of its string and rewriting
	 * the surrounding payload structure.
	 *
	 * @param text text to escape (may be null)
	 * @return escaped text, empty string for null
	 */
	protected String escapeJson(String text) {
		if (text == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder(text.length() + 8);
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			switch (c) {
				case '\\' -> sb.append("\\\\");
				case '"' -> sb.append("\\\"");
				case '\n' -> sb.append("\\n");
				case '\r' -> sb.append("\\r");
				case '\t' -> sb.append("\\t");
				case '\b' -> sb.append("\\b");
				case '\f' -> sb.append("\\f");
				default -> {
					if (c < 0x20) {
						sb.append(String.format("\\u%04x", (int) c));
					} else {
						sb.append(c);
					}
				}
			}
		}
		return sb.toString();
	}

	/**
	 * Logs notification attempt.
	 *
	 * @param channel channel used
	 * @param alert alert being notified
	 * @param success whether notification succeeded
	 */
	protected void logNotification(NotificationChannel channel, ActiveAlert alert, boolean success) {
		if (logger != null) {
			String message = String.format("Notification %s via %s channel '%s' for alert '%s'", success ? "sent" : "failed", getChannelType().getDisplayName(), channel.getName(), alert != null ? alert.getAlertId() : "test");

			if (success) {
				logger.info("NOTIFICATION", message);
			} else {
				logger.warn("NOTIFICATION", message);
			}
		}
	}
}
