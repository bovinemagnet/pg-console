package com.bovinemagnet.pgconsole.service.notification;

import com.bovinemagnet.pgconsole.model.ActiveAlert;
import com.bovinemagnet.pgconsole.model.NotificationChannel;
import com.bovinemagnet.pgconsole.model.NotificationChannel.PagerDutyConfig;
import com.bovinemagnet.pgconsole.model.NotificationResult;
import jakarta.enterprise.context.ApplicationScoped;

import java.net.http.HttpResponse;
import java.util.UUID;

/**
 * PagerDuty notification sender using Events API v2.
 * <p>
 * Creates incidents in PagerDuty with proper severity mapping,
 * deduplication, and automatic resolution support.
 * Features include:
 * <ul>
 *   <li>Events API v2 integration</li>
 *   <li>Incident creation with proper severity</li>
 *   <li>Deduplication key support</li>
 *   <li>Auto-resolution when alerts clear</li>
 *   <li>Service routing and grouping</li>
 * </ul>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see <a href="https://developer.pagerduty.com/docs/events-api-v2/trigger-events/">PagerDuty Events API</a>
 */
@ApplicationScoped
public class PagerDutyNotificationSender extends AbstractNotificationSender {

    private static final String PAGERDUTY_EVENTS_URL = "https://events.pagerduty.com/v2/enqueue";

    @Override
    public NotificationChannel.ChannelType getChannelType() {
        return NotificationChannel.ChannelType.PAGERDUTY;
    }

    @Override
    public NotificationResult send(NotificationChannel channel, ActiveAlert alert) {
        PagerDutyConfig config = channel.getPagerDutyConfig();
        if (config == null || config.getRoutingKey() == null) {
            return createFailureResult(channel, alert, new IllegalArgumentException("Missing PagerDuty routing key"));
        }

        try {
            String payload = buildTriggerPayload(channel, config, alert);
            HttpResponse<String> response = sendHttpPost(PAGERDUTY_EVENTS_URL, payload, null);

            // PagerDuty returns 202 for accepted
            if (response.statusCode() == 202) {
                logNotification(channel, alert, true);
                NotificationResult result = createSuccessResult(channel, alert, response);
                return result.withDedupKey(generateDedupKey(alert));
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
    public NotificationResult sendResolution(NotificationChannel channel, ActiveAlert alert) {
        PagerDutyConfig config = channel.getPagerDutyConfig();
        if (config == null || config.getRoutingKey() == null || !config.isAutoResolve()) {
            return createSuccessResult(channel, alert, null);
        }

        try {
            String payload = buildResolvePayload(config, alert);
            HttpResponse<String> response = sendHttpPost(PAGERDUTY_EVENTS_URL, payload, null);

            if (response.statusCode() == 202) {
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
        PagerDutyConfig config = channel.getPagerDutyConfig();
        if (config == null || config.getRoutingKey() == null) {
            return createFailureResult(channel, null, new IllegalArgumentException("Missing PagerDuty routing key"));
        }

        try {
            String payload = buildTestPayload(channel, config);
            HttpResponse<String> response = sendHttpPost(PAGERDUTY_EVENTS_URL, payload, null);

            if (response.statusCode() == 202) {
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
        PagerDutyConfig config = channel.getPagerDutyConfig();
        if (config == null) {
            return false;
        }
        String routingKey = config.getRoutingKey();
        return routingKey != null && !routingKey.isBlank() && routingKey.length() == 32;
    }

    private String buildTriggerPayload(NotificationChannel channel, PagerDutyConfig config, ActiveAlert alert) {
        String dedupKey = generateDedupKey(alert);
        String severity = mapSeverity(alert.getAlertSeverity());

        StringBuilder payload = new StringBuilder();
        payload.append("{");
        payload.append("\"routing_key\":\"").append(escapeJson(config.getRoutingKey())).append("\",");
        payload.append("\"event_action\":\"trigger\",");
        payload.append("\"dedup_key\":\"").append(escapeJson(dedupKey)).append("\",");

        // Payload section
        payload.append("\"payload\":{");
        payload.append("\"summary\":\"").append(escapeJson(alert.getAlertType())).append(": ")
               .append(escapeJson(alert.getAlertMessage())).append("\",");
        payload.append("\"severity\":\"").append(severity).append("\",");
        payload.append("\"source\":\"").append(escapeJson(alert.getInstanceName() != null ? alert.getInstanceName() : "pg-console")).append("\",");
        payload.append("\"timestamp\":\"").append(alert.getFiredAt().toString()).append("\",");

        // Component and group
        if (config.getComponent() != null && !config.getComponent().isBlank()) {
            payload.append("\"component\":\"").append(escapeJson(config.getComponent())).append("\",");
        }
        if (config.getGroup() != null && !config.getGroup().isBlank()) {
            payload.append("\"group\":\"").append(escapeJson(config.getGroup())).append("\",");
        }

        // Class (alert type)
        payload.append("\"class\":\"").append(escapeJson(alert.getAlertType())).append("\",");

        // Custom details
        payload.append("\"custom_details\":{");
        payload.append("\"alert_id\":\"").append(escapeJson(alert.getAlertId())).append("\",");
        payload.append("\"alert_type\":\"").append(escapeJson(alert.getAlertType())).append("\",");
        payload.append("\"instance\":\"").append(escapeJson(alert.getInstanceName() != null ? alert.getInstanceName() : "N/A")).append("\",");
        payload.append("\"duration\":\"").append(alert.getDurationFormatted()).append("\",");
        payload.append("\"message\":\"").append(escapeJson(alert.getAlertMessage())).append("\"");
        payload.append("}");

        payload.append("},"); // End payload

        // Client info
        payload.append("\"client\":\"PG Console\",");
        payload.append("\"client_url\":\"https://github.com/bovinemagnet/pg-console\"");

        payload.append("}");

        return payload.toString();
    }

    private String buildResolvePayload(PagerDutyConfig config, ActiveAlert alert) {
        String dedupKey = generateDedupKey(alert);

        StringBuilder payload = new StringBuilder();
        payload.append("{");
        payload.append("\"routing_key\":\"").append(escapeJson(config.getRoutingKey())).append("\",");
        payload.append("\"event_action\":\"resolve\",");
        payload.append("\"dedup_key\":\"").append(escapeJson(dedupKey)).append("\"");
        payload.append("}");

        return payload.toString();
    }

    private String buildTestPayload(NotificationChannel channel, PagerDutyConfig config) {
        String dedupKey = "pg-console-test-" + UUID.randomUUID().toString().substring(0, 8);

        StringBuilder payload = new StringBuilder();
        payload.append("{");
        payload.append("\"routing_key\":\"").append(escapeJson(config.getRoutingKey())).append("\",");
        payload.append("\"event_action\":\"trigger\",");
        payload.append("\"dedup_key\":\"").append(dedupKey).append("\",");
        payload.append("\"payload\":{");
        payload.append("\"summary\":\"PG Console Test - Channel '").append(escapeJson(channel.getName())).append("' configured correctly\",");
        payload.append("\"severity\":\"info\",");
        payload.append("\"source\":\"pg-console-test\"");
        payload.append("}");
        payload.append("}");

        return payload.toString();
    }

    private String generateDedupKey(ActiveAlert alert) {
        return "pg-console-" + alert.getAlertType() + "-" +
               (alert.getInstanceName() != null ? alert.getInstanceName() : "default") + "-" +
               alert.getAlertId();
    }

    private String mapSeverity(String alertSeverity) {
        if (alertSeverity == null) {
            return "info";
        }
        return switch (alertSeverity.toUpperCase()) {
            case "CRITICAL" -> "critical";
            case "HIGH" -> "error";
            case "MEDIUM", "WARNING" -> "warning";
            default -> "info";
        };
    }
}
