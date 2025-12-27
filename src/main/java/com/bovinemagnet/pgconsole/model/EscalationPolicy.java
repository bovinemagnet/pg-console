package com.bovinemagnet.pgconsole.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an escalation policy for alert notifications.
 * <p>
 * Escalation policies define multiple tiers of notification channels
 * that are contacted in sequence if an alert is not acknowledged.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class EscalationPolicy {

    private Long id;
    private String name;
    private String description;
    private boolean enabled = true;
    private int repeatCount; // Number of times to repeat all tiers
    private Instant createdAt;
    private Instant updatedAt;
    private List<EscalationTier> tiers = new ArrayList<>();

    public EscalationPolicy() {
    }

    public EscalationPolicy(String name) {
        this.name = name;
    }

    /**
     * Gets the next tier to escalate to.
     *
     * @param currentTier current tier number (1-based)
     * @return next tier or null if no more tiers
     */
    public EscalationTier getNextTier(int currentTier) {
        return tiers.stream()
                .filter(t -> t.getTierOrder() == currentTier + 1)
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets a specific tier by order.
     *
     * @param tierOrder tier number (1-based)
     * @return tier or null
     */
    public EscalationTier getTier(int tierOrder) {
        return tiers.stream()
                .filter(t -> t.getTierOrder() == tierOrder)
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets the maximum tier number.
     *
     * @return max tier order or 0 if no tiers
     */
    public int getMaxTier() {
        return tiers.stream()
                .mapToInt(EscalationTier::getTierOrder)
                .max()
                .orElse(0);
    }

    /**
     * Escalation tier within a policy.
     */
    public static class EscalationTier {
        private Long id;
        private Long policyId;
        private int tierOrder;
        private int delayMinutes; // Time before escalating to next tier
        private List<Long> channelIds = new ArrayList<>();
        private List<NotificationChannel> channels = new ArrayList<>(); // Loaded channels

        public EscalationTier() {
        }

        public EscalationTier(int tierOrder, int delayMinutes) {
            this.tierOrder = tierOrder;
            this.delayMinutes = delayMinutes;
        }

        /**
         * Gets display text for the tier.
         *
         * @return tier display text
         */
        public String getDisplayText() {
            if (delayMinutes == 0) {
                return "Tier " + tierOrder + " (Immediate)";
            }
            return String.format("Tier %d (After %d min)", tierOrder, delayMinutes);
        }

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getPolicyId() { return policyId; }
        public void setPolicyId(Long policyId) { this.policyId = policyId; }
        public int getTierOrder() { return tierOrder; }
        public void setTierOrder(int tierOrder) { this.tierOrder = tierOrder; }
        public int getDelayMinutes() { return delayMinutes; }
        public void setDelayMinutes(int delayMinutes) { this.delayMinutes = delayMinutes; }
        public List<Long> getChannelIds() { return channelIds; }
        public void setChannelIds(List<Long> channelIds) { this.channelIds = channelIds; }
        public List<NotificationChannel> getChannels() { return channels; }
        public void setChannels(List<NotificationChannel> channels) { this.channels = channels; }
    }

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getRepeatCount() { return repeatCount; }
    public void setRepeatCount(int repeatCount) { this.repeatCount = repeatCount; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public List<EscalationTier> getTiers() { return tiers; }
    public void setTiers(List<EscalationTier> tiers) { this.tiers = tiers; }

    public void addTier(EscalationTier tier) {
        tier.setPolicyId(this.id);
        this.tiers.add(tier);
    }
}
