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
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
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
    protected HttpResponse<String> sendHttpPost(String url, String jsonBody,
                                                 Map<String, String> headers) throws Exception {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
            .header("Content-Type", CONTENT_TYPE_JSON)
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

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
    protected NotificationResult createSuccessResult(NotificationChannel channel,
                                                      ActiveAlert alert,
                                                      HttpResponse<String> response) {
        return NotificationResult.success(
                channel.getId(),
                channel.getName(),
                channel.getChannelType(),
                alert != null ? alert.getAlertId() : "test"
            )
            .withResponseCode(response.statusCode())
            .withResponseBody(truncateBody(response.body()))
            .withAlertDetails(
                alert != null ? alert.getAlertType() : "TEST",
                alert != null ? alert.getAlertSeverity() : "INFO",
                alert != null ? alert.getAlertMessage() : "Test notification",
                alert != null ? alert.getInstanceName() : "test"
            );
    }

    /**
     * Creates a failure result from an exception.
     *
     * @param channel the notification channel
     * @param alert the alert
     * @param error the exception
     * @return failure result
     */
    protected NotificationResult createFailureResult(NotificationChannel channel,
                                                      ActiveAlert alert,
                                                      Throwable error) {
        return NotificationResult.failure(
                channel.getId(),
                channel.getName(),
                channel.getChannelType(),
                alert != null ? alert.getAlertId() : "test",
                error.getMessage()
            )
            .withAlertDetails(
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
    protected NotificationResult createFailureResult(NotificationChannel channel,
                                                      ActiveAlert alert,
                                                      HttpResponse<String> response) {
        return NotificationResult.failure(
                channel.getId(),
                channel.getName(),
                channel.getChannelType(),
                alert != null ? alert.getAlertId() : "test",
                "HTTP " + response.statusCode() + ": " + truncateBody(response.body())
            )
            .withResponseCode(response.statusCode())
            .withResponseBody(truncateBody(response.body()))
            .withAlertDetails(
                alert != null ? alert.getAlertType() : "TEST",
                alert != null ? alert.getAlertSeverity() : "INFO",
                alert != null ? alert.getAlertMessage() : "Test notification",
                alert != null ? alert.getInstanceName() : "test"
            );
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
     * Truncates response body for storage.
     *
     * @param body response body
     * @return truncated body
     */
    protected String truncateBody(String body) {
        if (body == null) {
            return null;
        }
        if (body.length() > 1000) {
            return body.substring(0, 1000) + "...[truncated]";
        }
        return body;
    }

    /**
     * Escapes special characters for JSON strings.
     *
     * @param text text to escape
     * @return escaped text
     */
    protected String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
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
            String message = String.format("Notification %s via %s channel '%s' for alert '%s'",
                success ? "sent" : "failed",
                getChannelType().getDisplayName(),
                channel.getName(),
                alert != null ? alert.getAlertId() : "test");

            if (success) {
                logger.info("NOTIFICATION", message);
            } else {
                logger.warn("NOTIFICATION", message);
            }
        }
    }
}
