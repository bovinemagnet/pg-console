package com.bovinemagnet.pgconsole.service.notification;

import com.bovinemagnet.pgconsole.model.ActiveAlert;
import com.bovinemagnet.pgconsole.model.EscalationPolicy;
import com.bovinemagnet.pgconsole.model.EscalationPolicy.EscalationTier;
import com.bovinemagnet.pgconsole.model.NotificationResult;
import com.bovinemagnet.pgconsole.repository.ActiveAlertRepository;
import com.bovinemagnet.pgconsole.repository.EscalationPolicyRepository;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages alert escalation based on configured policies.
 * <p>
 * Escalation policies define multiple tiers of notification channels
 * that are contacted in sequence if an alert is not acknowledged.
 * Features include:
 * <ul>
 *   <li>Multi-tier escalation with configurable delays</li>
 *   <li>Automatic escalation based on time thresholds</li>
 *   <li>Policy-based routing to notification channels</li>
 *   <li>Repeat cycles for continued escalation</li>
 * </ul>
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class EscalationService {

    private static final Logger LOG = Logger.getLogger(EscalationService.class);

    @Inject
    ActiveAlertRepository alertRepository;

    @Inject
    EscalationPolicyRepository policyRepository;

    @Inject
    NotificationDispatcher dispatcher;

    /**
     * Processes alerts due for escalation.
     * <p>
     * This method is scheduled to run periodically and checks for
     * unacknowledged alerts that have exceeded their escalation delay.
     */
    @Scheduled(every = "60s")
    public void processEscalations() {
        List<ActiveAlert> alertsToEscalate = alertRepository.findDueForEscalation();

        if (alertsToEscalate.isEmpty()) {
            LOG.debugf("No alerts due for escalation");
            return;
        }

        LOG.infof("Processing %d alerts for escalation", alertsToEscalate.size());

        for (ActiveAlert alert : alertsToEscalate) {
            try {
                escalateAlert(alert);
            } catch (Exception e) {
                LOG.errorf(e, "Failed to escalate alert %s", alert.getAlertId());
            }
        }
    }

    /**
     * Escalates an alert to the next tier in its escalation policy.
     *
     * @param alert the alert to escalate
     * @return list of notification results from the escalated tier
     */
    public List<NotificationResult> escalateAlert(ActiveAlert alert) {
        List<NotificationResult> results = new ArrayList<>();

        if (alert.getEscalationPolicyId() == null) {
            LOG.debugf("Alert %s has no escalation policy", alert.getAlertId());
            return results;
        }

        EscalationPolicy policy = policyRepository.findById(alert.getEscalationPolicyId())
            .orElse(null);

        if (policy == null || !policy.isEnabled()) {
            LOG.debugf("Escalation policy not found or disabled for alert %s", alert.getAlertId());
            return results;
        }

        int currentTier = alert.getCurrentEscalationTier();
        int maxTier = policy.getMaxTier();

        // Check if we've exhausted all tiers
        if (currentTier >= maxTier) {
            if (policy.getRepeatCount() > 0) {
                // Restart escalation if repeat is configured
                LOG.infof("Restarting escalation for alert %s (repeat cycle)", alert.getAlertId());
                currentTier = 0;
            } else {
                LOG.infof("Alert %s has exhausted all escalation tiers", alert.getAlertId());
                return results;
            }
        }

        // Get the next tier
        EscalationTier nextTier = policy.getNextTier(currentTier);
        if (nextTier == null) {
            LOG.warnf("No next tier found for alert %s at tier %d", alert.getAlertId(), currentTier);
            return results;
        }

        LOG.infof("Escalating alert %s from tier %d to tier %d",
            alert.getAlertId(), currentTier, nextTier.getTierOrder());

        // Send notifications to all channels in the tier
        if (nextTier.getChannelIds() != null && !nextTier.getChannelIds().isEmpty()) {
            results = dispatcher.dispatchToChannels(alert, nextTier.getChannelIds());

            // Set escalation tier in results
            for (NotificationResult result : results) {
                result.setEscalationTier(nextTier.getTierOrder());
            }
        }

        // Update alert escalation state
        alert.setCurrentEscalationTier(nextTier.getTierOrder());
        alert.setLastNotificationAt(Instant.now());
        alertRepository.update(alert);

        return results;
    }

    /**
     * Assigns an escalation policy to an alert and triggers initial notification.
     *
     * @param alert the alert to assign
     * @param policyId the escalation policy ID
     * @return notification results from the first tier
     */
    public List<NotificationResult> assignPolicyAndNotify(ActiveAlert alert, Long policyId) {
        List<NotificationResult> results = new ArrayList<>();

        EscalationPolicy policy = policyRepository.findById(policyId).orElse(null);
        if (policy == null || !policy.isEnabled()) {
            LOG.warnf("Escalation policy %d not found or disabled", policyId);
            return results;
        }

        alert.setEscalationPolicyId(policyId);
        alert.setCurrentEscalationTier(0);

        // Get first tier
        EscalationTier firstTier = policy.getTier(1);
        if (firstTier == null) {
            LOG.warnf("Escalation policy %d has no first tier", policyId);
            alertRepository.update(alert);
            return results;
        }

        // Send to first tier channels
        if (firstTier.getChannelIds() != null && !firstTier.getChannelIds().isEmpty()) {
            results = dispatcher.dispatchToChannels(alert, firstTier.getChannelIds());

            for (NotificationResult result : results) {
                result.setEscalationTier(1);
            }
        }

        // Update alert state
        alert.setCurrentEscalationTier(1);
        alert.setLastNotificationAt(Instant.now());
        alertRepository.update(alert);

        return results;
    }

    /**
     * Gets escalation status for an alert.
     *
     * @param alertId the alert database ID
     * @return escalation status information
     */
    public EscalationStatus getEscalationStatus(Long alertId) {
        ActiveAlert alert = alertRepository.findById(alertId).orElse(null);
        if (alert == null) {
            return null;
        }

        EscalationPolicy policy = null;
        if (alert.getEscalationPolicyId() != null) {
            policy = policyRepository.findById(alert.getEscalationPolicyId()).orElse(null);
        }

        return new EscalationStatus(alert, policy);
    }

    /**
     * Lists all escalation policies.
     *
     * @return list of all policies
     */
    public List<EscalationPolicy> listPolicies() {
        return policyRepository.findAll();
    }

    /**
     * Gets an escalation policy by ID.
     *
     * @param id policy ID
     * @return the policy or null if not found
     */
    public EscalationPolicy getPolicy(Long id) {
        return policyRepository.findById(id).orElse(null);
    }

    /**
     * Creates a new escalation policy.
     *
     * @param policy the policy to create
     * @return the created policy with ID
     */
    public EscalationPolicy createPolicy(EscalationPolicy policy) {
        return policyRepository.save(policy);
    }

    /**
     * Updates an escalation policy.
     *
     * @param policy the policy to update
     * @return the updated policy
     */
    public EscalationPolicy updatePolicy(EscalationPolicy policy) {
        return policyRepository.update(policy);
    }

    /**
     * Deletes an escalation policy.
     *
     * @param id the policy ID to delete
     */
    public void deletePolicy(Long id) {
        policyRepository.delete(id);
    }

    /**
     * Escalation status information for an alert.
     */
    public static class EscalationStatus {
        private final ActiveAlert alert;
        private final EscalationPolicy policy;
        private final int currentTier;
        private final int maxTier;
        private final long minutesSinceLastNotification;
        private final EscalationTier nextTier;
        private final int minutesUntilNextEscalation;

        public EscalationStatus(ActiveAlert alert, EscalationPolicy policy) {
            this.alert = alert;
            this.policy = policy;
            this.currentTier = alert.getCurrentEscalationTier();
            this.maxTier = policy != null ? policy.getMaxTier() : 0;

            if (alert.getLastNotificationAt() != null) {
                this.minutesSinceLastNotification = java.time.Duration
                    .between(alert.getLastNotificationAt(), Instant.now())
                    .toMinutes();
            } else {
                this.minutesSinceLastNotification = 0;
            }

            if (policy != null && currentTier < maxTier) {
                this.nextTier = policy.getNextTier(currentTier);
                if (nextTier != null) {
                    this.minutesUntilNextEscalation = Math.max(0,
                        nextTier.getDelayMinutes() - (int) minutesSinceLastNotification);
                } else {
                    this.minutesUntilNextEscalation = -1;
                }
            } else {
                this.nextTier = null;
                this.minutesUntilNextEscalation = -1;
            }
        }

        public ActiveAlert getAlert() { return alert; }
        public EscalationPolicy getPolicy() { return policy; }
        public int getCurrentTier() { return currentTier; }
        public int getMaxTier() { return maxTier; }
        public long getMinutesSinceLastNotification() { return minutesSinceLastNotification; }
        public EscalationTier getNextTier() { return nextTier; }
        public int getMinutesUntilNextEscalation() { return minutesUntilNextEscalation; }

        public boolean isAtMaxTier() {
            return currentTier >= maxTier;
        }

        public String getStatusText() {
            if (alert.isResolved()) return "Resolved";
            if (alert.isAcknowledged()) return "Acknowledged";
            if (isAtMaxTier()) return "Max Tier Reached";
            if (minutesUntilNextEscalation <= 0) return "Escalation Pending";
            return "Tier " + currentTier + " (Next in " + minutesUntilNextEscalation + "m)";
        }
    }
}
