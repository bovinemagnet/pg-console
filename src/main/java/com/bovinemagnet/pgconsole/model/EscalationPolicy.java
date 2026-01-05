package com.bovinemagnet.pgconsole.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an escalation policy for alert notifications in the PostgreSQL console monitoring system.
 * <p>
 * An escalation policy defines a multi-tiered notification strategy that is triggered when alerts
 * require attention. If an alert is not acknowledged within a specified time period, the policy
 * automatically escalates notifications to subsequent tiers, ensuring that critical database issues
 * are addressed promptly.
 * <p>
 * Each escalation policy consists of:
 * <ul>
 *   <li>One or more {@link EscalationTier} instances, each with a configurable delay</li>
 *   <li>A repeat count that determines how many times to cycle through all tiers</li>
 *   <li>An enabled/disabled state for temporary policy deactivation</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>{@code
 * EscalationPolicy policy = new EscalationPolicy("Critical DB Alerts");
 * policy.setDescription("For production database alerts");
 *
 * // Tier 1: Immediate notification to on-call engineer
 * EscalationTier tier1 = new EscalationTier(1, 0);
 * tier1.setChannelIds(List.of(emailChannelId));
 *
 * // Tier 2: After 15 minutes, notify team lead
 * EscalationTier tier2 = new EscalationTier(2, 15);
 * tier2.setChannelIds(List.of(slackChannelId));
 *
 * policy.addTier(tier1);
 * policy.addTier(tier2);
 * }</pre>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see EscalationTier
 * @see NotificationChannel
 * @since 0.0.0
 */
public class EscalationPolicy {

    /**
     * Unique identifier for this escalation policy.
     */
    private Long id;

    /**
     * Human-readable name for this escalation policy.
     * Should be descriptive and unique within the system.
     */
    private String name;

    /**
     * Optional detailed description of when and how this policy should be used.
     */
    private String description;

    /**
     * Flag indicating whether this policy is currently active.
     * Disabled policies are not used for alert escalation.
     */
    private boolean enabled = true;

    /**
     * Number of times to repeat all escalation tiers if the alert remains unacknowledged.
     * A value of 0 means the policy executes once through all tiers and then stops.
     */
    private int repeatCount;

    /**
     * Timestamp when this policy was created.
     */
    private Instant createdAt;

    /**
     * Timestamp when this policy was last modified.
     */
    private Instant updatedAt;

    /**
     * Ordered list of escalation tiers that define the escalation sequence.
     * Tiers are processed in order based on their {@link EscalationTier#getTierOrder()}.
     */
    private List<EscalationTier> tiers = new ArrayList<>();

    /**
     * Creates a new escalation policy with default values.
     */
    public EscalationPolicy() {
    }

    /**
     * Creates a new escalation policy with the specified name.
     *
     * @param name the name for this escalation policy
     */
    public EscalationPolicy(String name) {
        this.name = name;
    }

    /**
     * Retrieves the next tier in the escalation sequence after the specified tier.
     * <p>
     * This method is used to determine which tier should be notified next when
     * an alert escalates. Tiers are identified by their order number, which is
     * 1-based and sequential.
     *
     * @param currentTier the current tier number (1-based)
     * @return the next {@link EscalationTier} in sequence, or {@code null} if there are no more tiers
     * @see EscalationTier#getTierOrder()
     */
    public EscalationTier getNextTier(int currentTier) {
        return tiers.stream()
                .filter(t -> t.getTierOrder() == currentTier + 1)
                .findFirst()
                .orElse(null);
    }

    /**
     * Retrieves a specific escalation tier by its order number.
     * <p>
     * This method allows direct access to any tier in the policy by its position
     * in the escalation sequence.
     *
     * @param tierOrder the tier number to retrieve (1-based)
     * @return the {@link EscalationTier} with the specified order, or {@code null} if not found
     * @see EscalationTier#getTierOrder()
     */
    public EscalationTier getTier(int tierOrder) {
        return tiers.stream()
                .filter(t -> t.getTierOrder() == tierOrder)
                .findFirst()
                .orElse(null);
    }

    /**
     * Determines the highest tier order number in this policy.
     * <p>
     * This is useful for determining when the escalation sequence has reached its end,
     * or for validating tier operations.
     *
     * @return the maximum tier order number, or 0 if there are no tiers in this policy
     */
    public int getMaxTier() {
        return tiers.stream()
                .mapToInt(EscalationTier::getTierOrder)
                .max()
                .orElse(0);
    }

    /**
     * Represents a single tier within an escalation policy.
     * <p>
     * Each tier defines a notification step in the escalation sequence. When an alert
     * is triggered or escalated to this tier, all associated {@link NotificationChannel}s
     * are notified. If the alert is not acknowledged within the specified delay period,
     * escalation proceeds to the next tier.
     * <p>
     * Tiers are ordered sequentially starting from 1. The {@code tierOrder} determines
     * the sequence in which tiers are processed during escalation. A tier with
     * {@code delayMinutes} of 0 represents immediate notification.
     *
     * @see NotificationChannel
     * @see EscalationPolicy
     * @since 0.0.0
     */
    public static class EscalationTier {
        /**
         * Unique identifier for this escalation tier.
         */
        private Long id;

        /**
         * Foreign key reference to the parent {@link EscalationPolicy}.
         */
        private Long policyId;

        /**
         * Position of this tier in the escalation sequence (1-based).
         * Lower numbers are processed first.
         */
        private int tierOrder;

        /**
         * Time in minutes to wait before escalating to the next tier.
         * A value of 0 indicates immediate notification with no delay.
         */
        private int delayMinutes;

        /**
         * List of notification channel IDs associated with this tier.
         * These IDs reference {@link NotificationChannel} entities.
         */
        private List<Long> channelIds = new ArrayList<>();

        /**
         * Lazily loaded list of actual {@link NotificationChannel} instances.
         * Populated when the tier's channels are resolved from the database.
         */
        private List<NotificationChannel> channels = new ArrayList<>();

        /**
         * Creates a new escalation tier with default values.
         */
        public EscalationTier() {
        }

        /**
         * Creates a new escalation tier with the specified order and delay.
         *
         * @param tierOrder the position of this tier in the escalation sequence (1-based)
         * @param delayMinutes the time in minutes to wait before escalating to the next tier
         */
        public EscalationTier(int tierOrder, int delayMinutes) {
            this.tierOrder = tierOrder;
            this.delayMinutes = delayMinutes;
        }

        /**
         * Generates a human-readable display text for this tier.
         * <p>
         * The format varies based on the delay:
         * <ul>
         *   <li>For immediate tiers (delay = 0): "Tier N (Immediate)"</li>
         *   <li>For delayed tiers: "Tier N (After M min)"</li>
         * </ul>
         *
         * @return a formatted string describing this tier's position and timing
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

    /**
     * Retrieves the unique identifier for this escalation policy.
     *
     * @return the policy ID, or {@code null} if not yet persisted
     */
    public Long getId() { return id; }

    /**
     * Sets the unique identifier for this escalation policy.
     *
     * @param id the policy ID to set
     */
    public void setId(Long id) { this.id = id; }

    /**
     * Retrieves the name of this escalation policy.
     *
     * @return the policy name
     */
    public String getName() { return name; }

    /**
     * Sets the name of this escalation policy.
     *
     * @param name the policy name to set
     */
    public void setName(String name) { this.name = name; }

    /**
     * Retrieves the description of this escalation policy.
     *
     * @return the policy description, or {@code null} if not set
     */
    public String getDescription() { return description; }

    /**
     * Sets the description of this escalation policy.
     *
     * @param description the policy description to set
     */
    public void setDescription(String description) { this.description = description; }

    /**
     * Checks whether this escalation policy is currently enabled.
     *
     * @return {@code true} if the policy is enabled and active, {@code false} otherwise
     */
    public boolean isEnabled() { return enabled; }

    /**
     * Sets whether this escalation policy is enabled.
     *
     * @param enabled {@code true} to enable the policy, {@code false} to disable it
     */
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    /**
     * Retrieves the number of times to repeat all escalation tiers.
     *
     * @return the repeat count, where 0 means execute once and stop
     */
    public int getRepeatCount() { return repeatCount; }

    /**
     * Sets the number of times to repeat all escalation tiers.
     *
     * @param repeatCount the repeat count to set, where 0 means execute once and stop
     */
    public void setRepeatCount(int repeatCount) { this.repeatCount = repeatCount; }

    /**
     * Retrieves the timestamp when this policy was created.
     *
     * @return the creation timestamp, or {@code null} if not set
     */
    public Instant getCreatedAt() { return createdAt; }

    /**
     * Sets the timestamp when this policy was created.
     *
     * @param createdAt the creation timestamp to set
     */
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    /**
     * Retrieves the timestamp when this policy was last updated.
     *
     * @return the last update timestamp, or {@code null} if not set
     */
    public Instant getUpdatedAt() { return updatedAt; }

    /**
     * Sets the timestamp when this policy was last updated.
     *
     * @param updatedAt the update timestamp to set
     */
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    /**
     * Retrieves the list of escalation tiers in this policy.
     *
     * @return the mutable list of tiers, never {@code null}
     */
    public List<EscalationTier> getTiers() { return tiers; }

    /**
     * Replaces the current list of escalation tiers with a new list.
     *
     * @param tiers the list of tiers to set, should not be {@code null}
     */
    public void setTiers(List<EscalationTier> tiers) { this.tiers = tiers; }

    /**
     * Adds a new escalation tier to this policy.
     * <p>
     * This method automatically sets the tier's {@code policyId} to match this policy's ID
     * before adding it to the internal list. This ensures referential integrity when the
     * tier is persisted.
     *
     * @param tier the escalation tier to add, should not be {@code null}
     * @see EscalationTier#setPolicyId(Long)
     */
    public void addTier(EscalationTier tier) {
        tier.setPolicyId(this.id);
        this.tiers.add(tier);
    }
}
