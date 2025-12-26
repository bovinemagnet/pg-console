package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.config.InstanceConfig;
import com.bovinemagnet.pgconsole.model.OverviewStats;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for sending alerts when thresholds are exceeded.
 * Supports webhook and email notifications.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class AlertingService {

    private static final Logger LOG = Logger.getLogger(AlertingService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Inject
    InstanceConfig config;

    // Track when last alert was sent for each alert type to implement cooldown
    private final Map<String, LocalDateTime> lastAlertTime = new ConcurrentHashMap<>();

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    /**
     * Checks metrics and sends alerts if thresholds are exceeded.
     *
     * @param instanceName the database instance name
     * @param stats the current overview stats
     */
    public void checkAndAlert(String instanceName, OverviewStats stats) {
        if (!config.alerting().enabled()) {
            return;
        }

        var thresholds = config.alerting().thresholds();

        // Check connection percentage
        int connPercent = stats.getConnectionPercentage();
        if (connPercent >= thresholds.connectionPercent()) {
            sendAlert(instanceName, "HIGH_CONNECTIONS",
                "Connection usage critical",
                String.format("Connection usage is at %d%% (%d/%d). Threshold: %d%%",
                    connPercent, stats.getConnectionsUsed(), stats.getConnectionsMax(),
                    thresholds.connectionPercent()));
        }

        // Check blocked queries
        int blockedQueries = stats.getBlockedQueries();
        if (blockedQueries >= thresholds.blockedQueries()) {
            sendAlert(instanceName, "BLOCKED_QUERIES",
                "Multiple blocked queries detected",
                String.format("%d queries are currently blocked. Threshold: %d",
                    blockedQueries, thresholds.blockedQueries()));
        }

        // Check cache hit ratio
        double cacheHitRatio = stats.getCacheHitRatio();
        if (cacheHitRatio < thresholds.cacheHitRatio() && cacheHitRatio > 0) {
            sendAlert(instanceName, "LOW_CACHE_HIT",
                "Cache hit ratio is low",
                String.format("Cache hit ratio is %.2f%%. Threshold: %d%%",
                    cacheHitRatio, thresholds.cacheHitRatio()));
        }
    }

    /**
     * Sends an alert if cooldown period has passed.
     *
     * @param instanceName the database instance
     * @param alertType unique identifier for the alert type
     * @param title short alert title
     * @param message detailed alert message
     */
    public void sendAlert(String instanceName, String alertType, String title, String message) {
        String alertKey = instanceName + ":" + alertType;

        // Check cooldown
        LocalDateTime lastAlert = lastAlertTime.get(alertKey);
        LocalDateTime now = LocalDateTime.now();

        if (lastAlert != null) {
            long secondsSinceLastAlert = Duration.between(lastAlert, now).toSeconds();
            if (secondsSinceLastAlert < config.alerting().cooldownSeconds()) {
                LOG.debugf("Alert %s is in cooldown (%d seconds remaining)",
                    alertKey, config.alerting().cooldownSeconds() - secondsSinceLastAlert);
                return;
            }
        }

        // Update cooldown tracker
        lastAlertTime.put(alertKey, now);

        // Send via configured channels
        config.alerting().webhookUrl().ifPresent(url -> sendWebhook(url, instanceName, alertType, title, message));
        config.alerting().emailTo().ifPresent(email -> logEmailAlert(email, instanceName, alertType, title, message));

        LOG.infof("Alert sent: [%s] %s - %s", instanceName, title, message);
    }

    /**
     * Sends an alert via webhook.
     */
    private void sendWebhook(String url, String instanceName, String alertType, String title, String message) {
        try {
            String timestamp = TIMESTAMP_FORMAT.format(LocalDateTime.now());

            // Build JSON payload (simple format compatible with most webhook receivers)
            String json = String.format("""
                {
                    "timestamp": "%s",
                    "instance": "%s",
                    "alertType": "%s",
                    "title": "%s",
                    "message": "%s",
                    "source": "pg-console"
                }
                """,
                timestamp,
                escapeJson(instanceName),
                escapeJson(alertType),
                escapeJson(title),
                escapeJson(message));

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .timeout(Duration.ofSeconds(30))
                .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        LOG.debugf("Webhook alert sent successfully to %s", url);
                    } else {
                        LOG.warnf("Webhook alert failed with status %d: %s",
                            response.statusCode(), response.body());
                    }
                })
                .exceptionally(e -> {
                    LOG.errorf(e, "Failed to send webhook alert to %s", url);
                    return null;
                });

        } catch (Exception e) {
            LOG.errorf(e, "Failed to send webhook alert to %s", url);
        }
    }

    /**
     * Logs an email alert (email sending requires Quarkus mailer configuration).
     * For now, just logs the intent - actual email integration can be added later.
     */
    private void logEmailAlert(String email, String instanceName, String alertType, String title, String message) {
        LOG.infof("Email alert would be sent to %s: [%s][%s] %s - %s",
            email, instanceName, alertType, title, message);
        // TODO: Integrate with Quarkus Mailer when available
        // io.quarkus:quarkus-mailer dependency and configuration required
    }

    /**
     * Escapes special characters for JSON strings.
     */
    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    /**
     * Clears the cooldown for a specific alert type.
     * Useful for testing or manual reset.
     *
     * @param instanceName the instance name
     * @param alertType the alert type
     */
    public void clearCooldown(String instanceName, String alertType) {
        lastAlertTime.remove(instanceName + ":" + alertType);
    }

    /**
     * Clears all cooldowns.
     */
    public void clearAllCooldowns() {
        lastAlertTime.clear();
    }
}
