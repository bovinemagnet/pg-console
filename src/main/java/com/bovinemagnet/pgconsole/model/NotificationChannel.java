package com.bovinemagnet.pgconsole.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a notification channel configuration.
 * <p>
 * Supports multiple channel types: Slack, Teams, PagerDuty, Discord, Email.
 * Each channel can filter alerts by severity and type.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class NotificationChannel {

    /**
     * Supported notification channel types.
     */
    public enum ChannelType {
        SLACK("Slack", "bi-slack", "Slack Webhook"),
        TEAMS("Microsoft Teams", "bi-microsoft-teams", "Teams Webhook Connector"),
        PAGERDUTY("PagerDuty", "bi-bell", "PagerDuty Events API v2"),
        DISCORD("Discord", "bi-discord", "Discord Webhook"),
        EMAIL("Email", "bi-envelope", "Enhanced Email with Templates");

        private final String displayName;
        private final String iconClass;
        private final String description;

        ChannelType(String displayName, String iconClass, String description) {
            this.displayName = displayName;
            this.iconClass = iconClass;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getIconClass() {
            return iconClass;
        }

        public String getDescription() {
            return description;
        }
    }

    private Long id;
    private String name;
    private ChannelType channelType;
    private boolean enabled = true;
    private String config; // JSON configuration
    private List<String> severityFilter = new ArrayList<>();
    private List<String> alertTypeFilter = new ArrayList<>();
    private List<String> instanceFilter = new ArrayList<>();
    private int rateLimitPerHour = 100; // Max notifications per hour
    private boolean testMode = false; // Log only, don't actually send
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastUsedAt;
    private int successCount;
    private int failureCount;

    // Type-specific configuration (parsed from JSON)
    private SlackConfig slackConfig;
    private TeamsConfig teamsConfig;
    private PagerDutyConfig pagerDutyConfig;
    private DiscordConfig discordConfig;
    private EmailConfig emailConfig;

    public NotificationChannel() {
    }

    public NotificationChannel(String name, ChannelType channelType) {
        this.name = name;
        this.channelType = channelType;
    }

    /**
     * Checks if this channel should receive an alert.
     *
     * @param severity alert severity
     * @param alertType alert type
     * @return true if channel should receive the alert
     */
    public boolean matchesAlert(String severity, String alertType) {
        if (!enabled) return false;

        // Check severity filter
        if (severityFilter != null && !severityFilter.isEmpty()) {
            if (!severityFilter.contains(severity)) {
                return false;
            }
        }

        // Check alert type filter
        if (alertTypeFilter != null && !alertTypeFilter.isEmpty()) {
            if (!alertTypeFilter.contains(alertType)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Gets the success rate as a percentage.
     *
     * @return success rate 0-100
     */
    public double getSuccessRate() {
        int total = successCount + failureCount;
        if (total == 0) return 100.0;
        return (successCount * 100.0) / total;
    }

    /**
     * Gets CSS class for channel health indicator.
     *
     * @return CSS class
     */
    public String getHealthCssClass() {
        double rate = getSuccessRate();
        if (rate >= 95) return "text-success";
        if (rate >= 80) return "text-warning";
        return "text-danger";
    }

    /**
     * Gets formatted last used time.
     *
     * @return formatted time
     */
    public String getLastUsedAtFormatted() {
        if (lastUsedAt == null) return "Never";
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(lastUsedAt);
    }

    /**
     * Slack channel configuration.
     */
    public static class SlackConfig {
        private String webhookUrl;
        private String channel; // Optional channel override
        private String username; // Optional bot username
        private String iconEmoji; // Optional emoji icon
        private boolean mentionChannel; // @channel for critical alerts
        private boolean useBlocks; // Use Block Kit for rich formatting

        public String getWebhookUrl() { return webhookUrl; }
        public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
        public String getChannel() { return channel; }
        public void setChannel(String channel) { this.channel = channel; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getIconEmoji() { return iconEmoji; }
        public void setIconEmoji(String iconEmoji) { this.iconEmoji = iconEmoji; }
        public boolean isMentionChannel() { return mentionChannel; }
        public void setMentionChannel(boolean mentionChannel) { this.mentionChannel = mentionChannel; }
        public boolean isUseBlocks() { return useBlocks; }
        public void setUseBlocks(boolean useBlocks) { this.useBlocks = useBlocks; }
    }

    /**
     * Microsoft Teams channel configuration.
     */
    public static class TeamsConfig {
        private String webhookUrl;
        private String themeColor; // Adaptive card accent colour
        private boolean mentionUsers; // @mention users for critical
        private List<String> mentionUserIds = new ArrayList<>();

        public String getWebhookUrl() { return webhookUrl; }
        public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
        public String getThemeColor() { return themeColor; }
        public void setThemeColor(String themeColor) { this.themeColor = themeColor; }
        public boolean isMentionUsers() { return mentionUsers; }
        public void setMentionUsers(boolean mentionUsers) { this.mentionUsers = mentionUsers; }
        public List<String> getMentionUserIds() { return mentionUserIds; }
        public void setMentionUserIds(List<String> mentionUserIds) { this.mentionUserIds = mentionUserIds; }
    }

    /**
     * PagerDuty channel configuration.
     */
    public static class PagerDutyConfig {
        private String routingKey; // Integration key
        private String serviceId; // Optional service ID
        private String severity; // critical, error, warning, info
        private String component; // Component name
        private String group; // Logical grouping
        private boolean autoResolve; // Auto-resolve when alert clears

        public String getRoutingKey() { return routingKey; }
        public void setRoutingKey(String routingKey) { this.routingKey = routingKey; }
        public String getServiceId() { return serviceId; }
        public void setServiceId(String serviceId) { this.serviceId = serviceId; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public String getComponent() { return component; }
        public void setComponent(String component) { this.component = component; }
        public String getGroup() { return group; }
        public void setGroup(String group) { this.group = group; }
        public boolean isAutoResolve() { return autoResolve; }
        public void setAutoResolve(boolean autoResolve) { this.autoResolve = autoResolve; }
    }

    /**
     * Discord channel configuration.
     */
    public static class DiscordConfig {
        private String webhookUrl;
        private String username; // Optional bot username
        private String avatarUrl; // Optional avatar URL
        private boolean mentionEveryone; // @everyone for critical
        private List<String> mentionRoleIds = new ArrayList<>(); // Role IDs to mention
        private boolean useEmbeds; // Use rich embeds

        public String getWebhookUrl() { return webhookUrl; }
        public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
        public boolean isMentionEveryone() { return mentionEveryone; }
        public void setMentionEveryone(boolean mentionEveryone) { this.mentionEveryone = mentionEveryone; }
        public List<String> getMentionRoleIds() { return mentionRoleIds; }
        public void setMentionRoleIds(List<String> mentionRoleIds) { this.mentionRoleIds = mentionRoleIds; }
        public boolean isUseEmbeds() { return useEmbeds; }
        public void setUseEmbeds(boolean useEmbeds) { this.useEmbeds = useEmbeds; }
    }

    /**
     * Enhanced email configuration.
     */
    public static class EmailConfig {
        private List<String> recipients = new ArrayList<>();
        private String fromAddress;
        private String subjectPrefix;
        private boolean useHtmlTemplate;
        private boolean digestMode; // Batch alerts into digest
        private int digestIntervalMinutes = 15; // Digest send interval
        private boolean attachReport; // Attach incident report

        public List<String> getRecipients() { return recipients; }
        public void setRecipients(List<String> recipients) { this.recipients = recipients; }
        public String getFromAddress() { return fromAddress; }
        public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }
        public String getSubjectPrefix() { return subjectPrefix; }
        public void setSubjectPrefix(String subjectPrefix) { this.subjectPrefix = subjectPrefix; }
        public boolean isUseHtmlTemplate() { return useHtmlTemplate; }
        public void setUseHtmlTemplate(boolean useHtmlTemplate) { this.useHtmlTemplate = useHtmlTemplate; }
        public boolean isDigestMode() { return digestMode; }
        public void setDigestMode(boolean digestMode) { this.digestMode = digestMode; }
        public int getDigestIntervalMinutes() { return digestIntervalMinutes; }
        public void setDigestIntervalMinutes(int digestIntervalMinutes) { this.digestIntervalMinutes = digestIntervalMinutes; }
        public boolean isAttachReport() { return attachReport; }
        public void setAttachReport(boolean attachReport) { this.attachReport = attachReport; }
    }

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ChannelType getChannelType() { return channelType; }
    public void setChannelType(ChannelType channelType) { this.channelType = channelType; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getConfig() { return config; }
    public void setConfig(String config) { this.config = config; }
    public List<String> getSeverityFilter() { return severityFilter; }
    public void setSeverityFilter(List<String> severityFilter) { this.severityFilter = severityFilter; }
    public List<String> getAlertTypeFilter() { return alertTypeFilter; }
    public void setAlertTypeFilter(List<String> alertTypeFilter) { this.alertTypeFilter = alertTypeFilter; }
    public List<String> getInstanceFilter() { return instanceFilter; }
    public void setInstanceFilter(List<String> instanceFilter) { this.instanceFilter = instanceFilter; }
    public int getRateLimitPerHour() { return rateLimitPerHour; }
    public void setRateLimitPerHour(int rateLimitPerHour) { this.rateLimitPerHour = rateLimitPerHour; }
    public boolean isTestMode() { return testMode; }
    public void setTestMode(boolean testMode) { this.testMode = testMode; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(Instant lastUsedAt) { this.lastUsedAt = lastUsedAt; }
    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }
    public int getFailureCount() { return failureCount; }
    public void setFailureCount(int failureCount) { this.failureCount = failureCount; }
    public SlackConfig getSlackConfig() { return slackConfig; }
    public void setSlackConfig(SlackConfig slackConfig) { this.slackConfig = slackConfig; }
    public TeamsConfig getTeamsConfig() { return teamsConfig; }
    public void setTeamsConfig(TeamsConfig teamsConfig) { this.teamsConfig = teamsConfig; }
    public PagerDutyConfig getPagerDutyConfig() { return pagerDutyConfig; }
    public void setPagerDutyConfig(PagerDutyConfig pagerDutyConfig) { this.pagerDutyConfig = pagerDutyConfig; }
    public DiscordConfig getDiscordConfig() { return discordConfig; }
    public void setDiscordConfig(DiscordConfig discordConfig) { this.discordConfig = discordConfig; }
    public EmailConfig getEmailConfig() { return emailConfig; }
    public void setEmailConfig(EmailConfig emailConfig) { this.emailConfig = emailConfig; }
}
