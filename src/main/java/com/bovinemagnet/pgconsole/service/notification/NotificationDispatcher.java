package com.bovinemagnet.pgconsole.service.notification;

import com.bovinemagnet.pgconsole.model.ActiveAlert;
import com.bovinemagnet.pgconsole.model.NotificationChannel;
import com.bovinemagnet.pgconsole.model.NotificationResult;
import com.bovinemagnet.pgconsole.repository.AlertSilenceRepository;
import com.bovinemagnet.pgconsole.repository.MaintenanceWindowRepository;
import com.bovinemagnet.pgconsole.repository.NotificationChannelRepository;
import com.bovinemagnet.pgconsole.repository.NotificationHistoryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Central dispatcher for routing alerts to notification channels.
 * <p>
 * Coordinates notification delivery by:
 * <ul>
 *   <li>Checking maintenance windows and alert silences</li>
 *   <li>Applying rate limiting per channel</li>
 *   <li>Routing to appropriate channel senders</li>
 *   <li>Recording notification history</li>
 * </ul>
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class NotificationDispatcher {

    private static final Logger LOG = Logger.getLogger(NotificationDispatcher.class);

    @Inject
    NotificationChannelRepository channelRepository;

    @Inject
    NotificationHistoryRepository historyRepository;

    @Inject
    MaintenanceWindowRepository maintenanceWindowRepository;

    @Inject
    AlertSilenceRepository alertSilenceRepository;

    @Inject
    Instance<NotificationSender> senders;

    // Cache for sender lookup by channel type
    private Map<NotificationChannel.ChannelType, NotificationSender> senderMap;

    // Rate limiting tracking: channelId -> list of recent send timestamps
    private final Map<Long, List<Instant>> rateLimitTracker = new ConcurrentHashMap<>();

    /**
     * Dispatches an alert to all matching enabled channels.
     *
     * @param alert the alert to dispatch
     * @return list of notification results from all channels
     */
    public List<NotificationResult> dispatch(ActiveAlert alert) {
        List<NotificationResult> results = new ArrayList<>();

        // Check for maintenance window suppression
        if (isInMaintenanceWindow(alert)) {
            LOG.debugf("Alert %s suppressed by maintenance window", alert.getAlertId());
            return results;
        }

        // Check for alert silences
        if (isSilenced(alert)) {
            LOG.debugf("Alert %s suppressed by silence rule", alert.getAlertId());
            return results;
        }

        // Get all enabled channels
        List<NotificationChannel> channels = channelRepository.findEnabled();

        for (NotificationChannel channel : channels) {
            // Check if channel matches alert criteria
            if (!channelMatchesAlert(channel, alert)) {
                continue;
            }

            // Check rate limiting
            if (isRateLimited(channel)) {
                LOG.warnf("Channel %s rate limited, skipping alert %s",
                    channel.getName(), alert.getAlertId());
                results.add(createRateLimitedResult(channel, alert));
                continue;
            }

            // Send notification
            NotificationResult result = sendToChannel(channel, alert);
            results.add(result);

            // Record in history
            historyRepository.save(result);

            // Update rate limit tracker
            trackSend(channel.getId());

            // Update channel last used timestamp
            if (result.isSuccess()) {
                channelRepository.updateLastUsed(channel.getId());
            }
        }

        LOG.infof("Dispatched alert %s to %d channels, %d successful",
            alert.getAlertId(), results.size(),
            results.stream().filter(NotificationResult::isSuccess).count());

        return results;
    }

    /**
     * Dispatches an alert to specific channels by ID.
     *
     * @param alert the alert to dispatch
     * @param channelIds list of channel IDs to send to
     * @return list of notification results
     */
    public List<NotificationResult> dispatchToChannels(ActiveAlert alert, List<Long> channelIds) {
        List<NotificationResult> results = new ArrayList<>();

        for (Long channelId : channelIds) {
            channelRepository.findById(channelId).ifPresent(channel -> {
                if (channel.isEnabled()) {
                    NotificationResult result = sendToChannel(channel, alert);
                    results.add(result);
                    historyRepository.save(result);

                    if (result.isSuccess()) {
                        channelRepository.updateLastUsed(channel.getId());
                    }
                }
            });
        }

        return results;
    }

    /**
     * Sends a resolution notification for a resolved alert.
     *
     * @param alert the resolved alert
     * @param channelIds channels to notify (or all if empty)
     * @return list of notification results
     */
    public List<NotificationResult> dispatchResolution(ActiveAlert alert, List<Long> channelIds) {
        List<NotificationResult> results = new ArrayList<>();

        List<NotificationChannel> channels;
        if (channelIds == null || channelIds.isEmpty()) {
            channels = channelRepository.findEnabled();
        } else {
            channels = channelIds.stream()
                .flatMap(id -> channelRepository.findById(id).stream())
                .collect(Collectors.toList());
        }

        for (NotificationChannel channel : channels) {
            if (!channel.isEnabled()) continue;

            NotificationSender sender = getSender(channel.getChannelType());
            if (sender != null) {
                NotificationResult result = sender.sendResolution(channel, alert);
                results.add(result);
                historyRepository.save(result);
            }
        }

        return results;
    }

    /**
     * Tests a notification channel by sending a test message.
     *
     * @param channelId channel ID to test
     * @return notification result
     */
    public NotificationResult testChannel(Long channelId) {
        return channelRepository.findById(channelId)
            .map(channel -> {
                NotificationSender sender = getSender(channel.getChannelType());
                if (sender == null) {
                    return NotificationResult.failure(
                        channel.getId(),
                        channel.getName(),
                        channel.getChannelType(),
                        "test",
                        "No sender found for channel type: " + channel.getChannelType()
                    );
                }

                NotificationResult result = sender.sendTest(channel);
                historyRepository.save(result);

                if (result.isSuccess()) {
                    channelRepository.updateLastUsed(channel.getId());
                }

                return result;
            })
            .orElse(NotificationResult.failure(null, "Unknown", null, "test",
                "Channel not found: " + channelId));
    }

    /**
     * Validates a channel's configuration.
     *
     * @param channel channel to validate
     * @return true if configuration is valid
     */
    public boolean validateChannelConfig(NotificationChannel channel) {
        NotificationSender sender = getSender(channel.getChannelType());
        return sender != null && sender.validateConfig(channel);
    }

    /**
     * Gets notification statistics for a time period.
     *
     * @param hours hours to look back
     * @return statistics record
     */
    public NotificationHistoryRepository.NotificationStats getStats(int hours) {
        return historyRepository.getStats(hours);
    }

    /**
     * Gets recent notification history.
     *
     * @param limit maximum number of results
     * @return list of recent notifications
     */
    public List<NotificationResult> getRecentHistory(int limit) {
        return historyRepository.findRecent(limit);
    }

    /**
     * Gets notification history for a specific alert.
     *
     * @param alertId alert ID
     * @return list of notifications for the alert
     */
    public List<NotificationResult> getHistoryForAlert(String alertId) {
        return historyRepository.findByAlertId(alertId);
    }

    /**
     * Retries failed notifications.
     *
     * @param limit maximum number to retry
     * @return list of retry results
     */
    public List<NotificationResult> retryFailed(int limit) {
        List<NotificationResult> failed = historyRepository.findFailedForRetry(limit);
        List<NotificationResult> retryResults = new ArrayList<>();

        for (NotificationResult original : failed) {
            if (original.getChannelId() == null) continue;

            channelRepository.findById(original.getChannelId()).ifPresent(channel -> {
                if (!channel.isEnabled()) return;

                // Reconstruct alert from history
                ActiveAlert alert = new ActiveAlert(
                    original.getAlertId(),
                    original.getAlertType(),
                    original.getAlertSeverity(),
                    original.getAlertMessage()
                );
                alert.setInstanceName(original.getInstanceName());

                NotificationResult result = sendToChannel(channel, alert);
                retryResults.add(result);
                historyRepository.save(result);
            });
        }

        return retryResults;
    }

    private NotificationResult sendToChannel(NotificationChannel channel, ActiveAlert alert) {
        NotificationSender sender = getSender(channel.getChannelType());
        if (sender == null) {
            LOG.errorf("No sender found for channel type: %s", channel.getChannelType());
            return NotificationResult.failure(
                channel.getId(),
                channel.getName(),
                channel.getChannelType(),
                alert.getAlertId(),
                "No sender configured for channel type"
            );
        }

        try {
            return sender.send(channel, alert);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to send notification to channel %s", channel.getName());
            return NotificationResult.failure(
                channel.getId(),
                channel.getName(),
                channel.getChannelType(),
                alert.getAlertId(),
                e.getMessage()
            );
        }
    }

    private NotificationSender getSender(NotificationChannel.ChannelType channelType) {
        if (senderMap == null) {
            initializeSenderMap();
        }
        return senderMap.get(channelType);
    }

    private synchronized void initializeSenderMap() {
        if (senderMap != null) return;

        senderMap = new ConcurrentHashMap<>();
        for (NotificationSender sender : senders) {
            senderMap.put(sender.getChannelType(), sender);
            LOG.debugf("Registered notification sender for %s", sender.getChannelType());
        }
    }

    private boolean channelMatchesAlert(NotificationChannel channel, ActiveAlert alert) {
        // Check severity filter
        List<String> severityFilter = channel.getSeverityFilter();
        if (severityFilter != null && !severityFilter.isEmpty()) {
            if (alert.getAlertSeverity() != null &&
                !severityFilter.contains(alert.getAlertSeverity())) {
                return false;
            }
        }

        // Check alert type filter
        List<String> alertTypeFilter = channel.getAlertTypeFilter();
        if (alertTypeFilter != null && !alertTypeFilter.isEmpty()) {
            if (alert.getAlertType() != null &&
                !alertTypeFilter.contains(alert.getAlertType())) {
                return false;
            }
        }

        // Check instance filter
        List<String> instanceFilter = channel.getInstanceFilter();
        if (instanceFilter != null && !instanceFilter.isEmpty()) {
            if (alert.getInstanceName() != null &&
                !instanceFilter.contains(alert.getInstanceName())) {
                return false;
            }
        }

        return true;
    }

    private boolean isInMaintenanceWindow(ActiveAlert alert) {
        return maintenanceWindowRepository.shouldSuppress(
            alert.getInstanceName(),
            alert.getAlertType()
        );
    }

    private boolean isSilenced(ActiveAlert alert) {
        return alertSilenceRepository.isSilenced(
            alert.getAlertType(),
            alert.getAlertSeverity(),
            alert.getInstanceName(),
            alert.getAlertMessage()
        );
    }

    private boolean isRateLimited(NotificationChannel channel) {
        int rateLimit = channel.getRateLimitPerHour();
        if (rateLimit <= 0) {
            return false;
        }

        int sentLastHour = historyRepository.getCountLastHour(channel.getId());
        return sentLastHour >= rateLimit;
    }

    private void trackSend(Long channelId) {
        rateLimitTracker.computeIfAbsent(channelId, k -> new ArrayList<>())
            .add(Instant.now());

        // Clean up old entries (older than 1 hour)
        Instant oneHourAgo = Instant.now().minusSeconds(3600);
        rateLimitTracker.get(channelId).removeIf(t -> t.isBefore(oneHourAgo));
    }

    private NotificationResult createRateLimitedResult(NotificationChannel channel, ActiveAlert alert) {
        return NotificationResult.failure(
            channel.getId(),
            channel.getName(),
            channel.getChannelType(),
            alert.getAlertId(),
            "Rate limit exceeded: " + channel.getRateLimitPerHour() + " per hour"
        ).withAlertDetails(
            alert.getAlertType(),
            alert.getAlertSeverity(),
            alert.getAlertMessage(),
            alert.getInstanceName()
        );
    }
}
