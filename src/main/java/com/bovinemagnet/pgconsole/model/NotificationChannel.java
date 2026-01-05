package com.bovinemagnet.pgconsole.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a notification channel configuration for alert delivery.
 * <p>
 * This class manages the configuration and routing of database monitoring alerts
 * to various external notification systems. It supports multiple channel types
 * including Slack, Microsoft Teams, PagerDuty, Discord, and Email, each with
 * type-specific configuration options.
 * </p>
 * <p>
 * Channels can be configured with filters to control which alerts are delivered
 * based on severity level, alert type, and database instance. Rate limiting and
 * test mode capabilities provide protection against notification flooding and
 * safe testing of channel configurations.
 * </p>
 * <p>
 * The channel maintains delivery statistics (success/failure counts) and tracks
 * the last usage timestamp for monitoring channel health and activity.
 * </p>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see AlertRule
 */
public class NotificationChannel {

    /**
     * Supported notification channel types.
     * <p>
     * Each channel type provides integration with a different notification
     * platform, complete with display metadata (name, icon, description) used
     * in the user interface.
     * </p>
     */
    public enum ChannelType {
        /** Slack webhook integration using Slack's Incoming Webhooks API. */
        SLACK("Slack", "bi-slack", "Slack Webhook"),

        /** Microsoft Teams integration using Teams Webhook Connector. */
        TEAMS("Microsoft Teams", "bi-microsoft-teams", "Teams Webhook Connector"),

        /** PagerDuty integration using Events API v2 for incident management. */
        PAGERDUTY("PagerDuty", "bi-bell", "PagerDuty Events API v2"),

        /** Discord webhook integration for Discord server notifications. */
        DISCORD("Discord", "bi-discord", "Discord Webhook"),

        /** Email delivery with support for HTML templates and digest mode. */
        EMAIL("Email", "bi-envelope", "Enhanced Email with Templates");

        /** Human-readable display name for the channel type. */
        private final String displayName;

        /** Bootstrap icon CSS class for UI representation. */
        private final String iconClass;

        /** Brief description of the integration method. */
        private final String description;

        /**
         * Constructs a channel type with display metadata.
         *
         * @param displayName the human-readable name displayed in the UI
         * @param iconClass the Bootstrap icon CSS class for visual representation
         * @param description a brief description of how the integration works
         */
        ChannelType(String displayName, String iconClass, String description) {
            this.displayName = displayName;
            this.iconClass = iconClass;
            this.description = description;
        }

        /**
         * Returns the human-readable display name for this channel type.
         *
         * @return the display name (e.g., "Microsoft Teams")
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Returns the Bootstrap icon CSS class for this channel type.
         *
         * @return the icon class (e.g., "bi-slack")
         */
        public String getIconClass() {
            return iconClass;
        }

        /**
         * Returns a brief description of the integration method.
         *
         * @return the description (e.g., "Slack Webhook")
         */
        public String getDescription() {
            return description;
        }
    }

    /** Unique identifier for this notification channel. */
    private Long id;

    /** User-defined name for this notification channel. */
    private String name;

    /** The type of notification channel (Slack, Teams, PagerDuty, etc.). */
    private ChannelType channelType;

    /** Whether this channel is currently active for receiving notifications. */
    private boolean enabled = true;

    /**
     * JSON configuration string containing channel-specific settings.
     * This is parsed into the appropriate type-specific configuration object.
     */
    private String config;

    /**
     * List of severity levels this channel accepts (e.g., "CRITICAL", "WARNING").
     * Empty list means all severities are accepted.
     */
    private List<String> severityFilter = new ArrayList<>();

    /**
     * List of alert types this channel accepts (e.g., "CONNECTION", "LOCK").
     * Empty list means all alert types are accepted.
     */
    private List<String> alertTypeFilter = new ArrayList<>();

    /**
     * List of database instances this channel monitors.
     * Empty list means all instances are monitored.
     */
    private List<String> instanceFilter = new ArrayList<>();

    /** Maximum number of notifications allowed per hour to prevent flooding. */
    private int rateLimitPerHour = 100;

    /**
     * Test mode flag - when true, notifications are logged but not actually sent.
     * Useful for testing channel configuration without triggering real alerts.
     */
    private boolean testMode = false;

    /** Optional description explaining the purpose of this notification channel. */
    private String description;

    /** Timestamp when this channel was created. */
    private Instant createdAt;

    /** Timestamp when this channel configuration was last updated. */
    private Instant updatedAt;

    /** Timestamp when this channel last successfully delivered a notification. */
    private Instant lastUsedAt;

    /** Total count of successfully delivered notifications. */
    private int successCount;

    /** Total count of failed notification delivery attempts. */
    private int failureCount;

    // Type-specific configuration (parsed from JSON)

    /** Slack-specific configuration settings. Only populated when channelType is SLACK. */
    private SlackConfig slackConfig;

    /** Microsoft Teams-specific configuration settings. Only populated when channelType is TEAMS. */
    private TeamsConfig teamsConfig;

    /** PagerDuty-specific configuration settings. Only populated when channelType is PAGERDUTY. */
    private PagerDutyConfig pagerDutyConfig;

    /** Discord-specific configuration settings. Only populated when channelType is DISCORD. */
    private DiscordConfig discordConfig;

    /** Email-specific configuration settings. Only populated when channelType is EMAIL. */
    private EmailConfig emailConfig;

    /**
     * Constructs an empty notification channel.
     * All fields are initialised to their default values.
     */
    public NotificationChannel() {
    }

    /**
     * Constructs a notification channel with the specified name and type.
     *
     * @param name the user-defined name for this channel
     * @param channelType the type of notification channel to create
     */
    public NotificationChannel(String name, ChannelType channelType) {
        this.name = name;
        this.channelType = channelType;
    }

    /**
     * Determines if this channel should receive an alert based on configured filters.
     * <p>
     * The alert is accepted if the channel is enabled and matches all configured
     * filters. Disabled channels never receive alerts. Empty filter lists are treated
     * as "accept all" for that filter type.
     * </p>
     *
     * @param severity the severity level of the alert (e.g., "CRITICAL", "WARNING")
     * @param alertType the type of alert (e.g., "CONNECTION", "LOCK", "PERFORMANCE")
     * @return {@code true} if this channel should receive the alert, {@code false} otherwise
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
     * Calculates the success rate of notification deliveries as a percentage.
     * <p>
     * The success rate is computed as: (successCount / (successCount + failureCount)) * 100.
     * If no deliveries have been attempted, returns 100.0 (perfect score).
     * </p>
     *
     * @return the success rate as a value between 0.0 and 100.0
     */
    public double getSuccessRate() {
        int total = successCount + failureCount;
        if (total == 0) return 100.0;
        return (successCount * 100.0) / total;
    }

    /**
     * Determines the CSS class for displaying channel health based on success rate.
     * <p>
     * Health indicator thresholds:
     * <ul>
     *   <li>95% or higher: "text-success" (green)</li>
     *   <li>80% to 94%: "text-warning" (amber)</li>
     *   <li>Below 80%: "text-danger" (red)</li>
     * </ul>
     * </p>
     *
     * @return the Bootstrap CSS class appropriate for the current health status
     */
    public String getHealthCssClass() {
        double rate = getSuccessRate();
        if (rate >= 95) return "text-success";
        if (rate >= 80) return "text-warning";
        return "text-danger";
    }

    /**
     * Formats the last used timestamp for display in the UI.
     * <p>
     * Returns "Never" if this channel has not yet been used for any delivery.
     * Otherwise, returns the timestamp formatted as "yyyy-MM-dd HH:mm:ss" in the
     * system's default time zone.
     * </p>
     *
     * @return the formatted timestamp or "Never" if not yet used
     */
    public String getLastUsedAtFormatted() {
        if (lastUsedAt == null) return "Never";
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(lastUsedAt);
    }

    /**
     * Configuration settings specific to Slack webhook integration.
     * <p>
     * Slack notifications are sent using Incoming Webhooks, which post messages
     * to a specific channel or user. This configuration supports customisation
     * of the bot appearance, channel routing, and message formatting.
     * </p>
     *
     * @see <a href="https://api.slack.com/messaging/webhooks">Slack Incoming Webhooks</a>
     */
    public static class SlackConfig {
        /** The Slack incoming webhook URL for posting messages. Required. */
        private String webhookUrl;

        /** Optional channel override (e.g., "#alerts" or "@username"). If not set, uses webhook default. */
        private String channel;

        /** Optional custom username for the bot posting the message. If not set, uses webhook default. */
        private String username;

        /** Optional emoji icon for the bot (e.g., ":warning:"). If not set, uses webhook default. */
        private String iconEmoji;

        /** Whether to mention @channel for critical alerts to notify all channel members. */
        private boolean mentionChannel;

        /** Whether to use Slack Block Kit for rich formatting instead of plain text. */
        private boolean useBlocks;

        /**
         * Returns the Slack incoming webhook URL.
         *
         * @return the webhook URL
         */
        public String getWebhookUrl() { return webhookUrl; }

        /**
         * Sets the Slack incoming webhook URL.
         *
         * @param webhookUrl the webhook URL to use for posting messages
         */
        public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }

        /**
         * Returns the channel override setting.
         *
         * @return the channel (e.g., "#alerts") or {@code null} to use webhook default
         */
        public String getChannel() { return channel; }

        /**
         * Sets the channel override for message delivery.
         *
         * @param channel the channel name (e.g., "#alerts") or user (e.g., "@username")
         */
        public void setChannel(String channel) { this.channel = channel; }

        /**
         * Returns the custom bot username.
         *
         * @return the bot username or {@code null} to use webhook default
         */
        public String getUsername() { return username; }

        /**
         * Sets the custom bot username for posting messages.
         *
         * @param username the bot username to display
         */
        public void setUsername(String username) { this.username = username; }

        /**
         * Returns the emoji icon for the bot.
         *
         * @return the emoji icon (e.g., ":warning:") or {@code null} to use webhook default
         */
        public String getIconEmoji() { return iconEmoji; }

        /**
         * Sets the emoji icon for the bot.
         *
         * @param iconEmoji the emoji icon in Slack format (e.g., ":warning:")
         */
        public void setIconEmoji(String iconEmoji) { this.iconEmoji = iconEmoji; }

        /**
         * Returns whether @channel mentions are enabled for critical alerts.
         *
         * @return {@code true} if critical alerts should mention @channel
         */
        public boolean isMentionChannel() { return mentionChannel; }

        /**
         * Sets whether to mention @channel for critical alerts.
         *
         * @param mentionChannel {@code true} to enable @channel mentions for critical alerts
         */
        public void setMentionChannel(boolean mentionChannel) { this.mentionChannel = mentionChannel; }

        /**
         * Returns whether Block Kit formatting is enabled.
         *
         * @return {@code true} if Block Kit should be used for rich formatting
         */
        public boolean isUseBlocks() { return useBlocks; }

        /**
         * Sets whether to use Slack Block Kit for rich message formatting.
         *
         * @param useBlocks {@code true} to use Block Kit, {@code false} for plain text
         */
        public void setUseBlocks(boolean useBlocks) { this.useBlocks = useBlocks; }
    }

    /**
     * Configuration settings specific to Microsoft Teams webhook integration.
     * <p>
     * Teams notifications are sent using Incoming Webhook connectors, which post
     * messages as Adaptive Cards to a Teams channel. This configuration supports
     * customisation of card appearance and user mentions for critical alerts.
     * </p>
     *
     * @see <a href="https://docs.microsoft.com/en-us/microsoftteams/platform/webhooks-and-connectors/how-to/add-incoming-webhook">Teams Incoming Webhooks</a>
     */
    public static class TeamsConfig {
        /** The Teams incoming webhook URL for posting messages. Required. */
        private String webhookUrl;

        /** Adaptive Card theme colour in hex format (e.g., "FF0000" for red). Controls card accent colour. */
        private String themeColor;

        /** Whether to @mention specific users for critical alerts. */
        private boolean mentionUsers;

        /** List of user IDs to mention when mentionUsers is enabled. */
        private List<String> mentionUserIds = new ArrayList<>();

        /**
         * Returns the Teams incoming webhook URL.
         *
         * @return the webhook URL
         */
        public String getWebhookUrl() { return webhookUrl; }

        /**
         * Sets the Teams incoming webhook URL.
         *
         * @param webhookUrl the webhook URL to use for posting messages
         */
        public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }

        /**
         * Returns the Adaptive Card theme colour.
         *
         * @return the colour in hex format (e.g., "FF0000") or {@code null} for default
         */
        public String getThemeColor() { return themeColor; }

        /**
         * Sets the Adaptive Card theme colour.
         *
         * @param themeColor the colour in hex format without # prefix (e.g., "FF0000")
         */
        public void setThemeColor(String themeColor) { this.themeColor = themeColor; }

        /**
         * Returns whether user mentions are enabled for critical alerts.
         *
         * @return {@code true} if users should be @mentioned for critical alerts
         */
        public boolean isMentionUsers() { return mentionUsers; }

        /**
         * Sets whether to @mention users for critical alerts.
         *
         * @param mentionUsers {@code true} to enable user mentions
         */
        public void setMentionUsers(boolean mentionUsers) { this.mentionUsers = mentionUsers; }

        /**
         * Returns the list of user IDs to mention.
         *
         * @return the list of Teams user IDs, never {@code null}
         */
        public List<String> getMentionUserIds() { return mentionUserIds; }

        /**
         * Sets the list of user IDs to mention when mentionUsers is enabled.
         *
         * @param mentionUserIds the list of Teams user IDs to @mention
         */
        public void setMentionUserIds(List<String> mentionUserIds) { this.mentionUserIds = mentionUserIds; }
    }

    /**
     * Configuration settings specific to PagerDuty Events API v2 integration.
     * <p>
     * PagerDuty notifications create incidents in the incident management platform,
     * triggering on-call escalation policies. This configuration supports incident
     * categorisation, severity mapping, and automatic resolution when alerts clear.
     * </p>
     *
     * @see <a href="https://developer.pagerduty.com/docs/ZG9jOjExMDI5NTgw-events-api-v2-overview">PagerDuty Events API v2</a>
     */
    public static class PagerDutyConfig {
        /** The routing key (integration key) for the PagerDuty service. Required. */
        private String routingKey;

        /** Optional service ID for explicitly targeting a specific PagerDuty service. */
        private String serviceId;

        /** Default severity level for incidents: "critical", "error", "warning", or "info". */
        private String severity;

        /** Component name for logical grouping of alerts (e.g., "database", "api"). */
        private String component;

        /** Logical grouping for alerts (e.g., "production", "staging"). */
        private String group;

        /** Whether to automatically resolve PagerDuty incidents when the alert clears. */
        private boolean autoResolve;

        /**
         * Returns the PagerDuty routing key.
         *
         * @return the routing key (integration key)
         */
        public String getRoutingKey() { return routingKey; }

        /**
         * Sets the PagerDuty routing key.
         *
         * @param routingKey the routing key for the target service
         */
        public void setRoutingKey(String routingKey) { this.routingKey = routingKey; }

        /**
         * Returns the optional service ID.
         *
         * @return the service ID or {@code null} if not specified
         */
        public String getServiceId() { return serviceId; }

        /**
         * Sets the optional service ID for explicit service targeting.
         *
         * @param serviceId the PagerDuty service ID
         */
        public void setServiceId(String serviceId) { this.serviceId = serviceId; }

        /**
         * Returns the default severity level for incidents.
         *
         * @return the severity ("critical", "error", "warning", or "info")
         */
        public String getSeverity() { return severity; }

        /**
         * Sets the default severity level for incidents.
         *
         * @param severity the severity level (one of: "critical", "error", "warning", "info")
         */
        public void setSeverity(String severity) { this.severity = severity; }

        /**
         * Returns the component name for alert categorisation.
         *
         * @return the component name or {@code null} if not specified
         */
        public String getComponent() { return component; }

        /**
         * Sets the component name for logical grouping of alerts.
         *
         * @param component the component name (e.g., "database", "api")
         */
        public void setComponent(String component) { this.component = component; }

        /**
         * Returns the group name for alert categorisation.
         *
         * @return the group name or {@code null} if not specified
         */
        public String getGroup() { return group; }

        /**
         * Sets the group name for logical grouping of alerts.
         *
         * @param group the group name (e.g., "production", "staging")
         */
        public void setGroup(String group) { this.group = group; }

        /**
         * Returns whether automatic incident resolution is enabled.
         *
         * @return {@code true} if incidents should auto-resolve when alerts clear
         */
        public boolean isAutoResolve() { return autoResolve; }

        /**
         * Sets whether to automatically resolve incidents when alerts clear.
         *
         * @param autoResolve {@code true} to enable automatic resolution
         */
        public void setAutoResolve(boolean autoResolve) { this.autoResolve = autoResolve; }
    }

    /**
     * Configuration settings specific to Discord webhook integration.
     * <p>
     * Discord notifications are sent using webhooks, which post messages to a
     * specific Discord channel. This configuration supports customisation of
     * the bot appearance, role mentions for critical alerts, and rich embed formatting.
     * </p>
     *
     * @see <a href="https://discord.com/developers/docs/resources/webhook">Discord Webhooks</a>
     */
    public static class DiscordConfig {
        /** The Discord webhook URL for posting messages. Required. */
        private String webhookUrl;

        /** Optional custom username for the webhook bot. If not set, uses webhook default. */
        private String username;

        /** Optional avatar image URL for the webhook bot. If not set, uses webhook default. */
        private String avatarUrl;

        /** Whether to use @everyone mention for critical alerts to notify all channel members. */
        private boolean mentionEveryone;

        /** List of Discord role IDs to mention for alerts. */
        private List<String> mentionRoleIds = new ArrayList<>();

        /** Whether to use rich embeds for message formatting instead of plain text. */
        private boolean useEmbeds;

        /**
         * Returns the Discord webhook URL.
         *
         * @return the webhook URL
         */
        public String getWebhookUrl() { return webhookUrl; }

        /**
         * Sets the Discord webhook URL.
         *
         * @param webhookUrl the webhook URL to use for posting messages
         */
        public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }

        /**
         * Returns the custom bot username.
         *
         * @return the bot username or {@code null} to use webhook default
         */
        public String getUsername() { return username; }

        /**
         * Sets the custom bot username for posting messages.
         *
         * @param username the bot username to display
         */
        public void setUsername(String username) { this.username = username; }

        /**
         * Returns the avatar image URL for the bot.
         *
         * @return the avatar URL or {@code null} to use webhook default
         */
        public String getAvatarUrl() { return avatarUrl; }

        /**
         * Sets the avatar image URL for the bot.
         *
         * @param avatarUrl the URL of the avatar image to display
         */
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

        /**
         * Returns whether @everyone mentions are enabled for critical alerts.
         *
         * @return {@code true} if critical alerts should mention @everyone
         */
        public boolean isMentionEveryone() { return mentionEveryone; }

        /**
         * Sets whether to mention @everyone for critical alerts.
         *
         * @param mentionEveryone {@code true} to enable @everyone mentions for critical alerts
         */
        public void setMentionEveryone(boolean mentionEveryone) { this.mentionEveryone = mentionEveryone; }

        /**
         * Returns the list of role IDs to mention.
         *
         * @return the list of Discord role IDs, never {@code null}
         */
        public List<String> getMentionRoleIds() { return mentionRoleIds; }

        /**
         * Sets the list of role IDs to mention for alerts.
         *
         * @param mentionRoleIds the list of Discord role IDs to @mention
         */
        public void setMentionRoleIds(List<String> mentionRoleIds) { this.mentionRoleIds = mentionRoleIds; }

        /**
         * Returns whether rich embed formatting is enabled.
         *
         * @return {@code true} if rich embeds should be used for formatting
         */
        public boolean isUseEmbeds() { return useEmbeds; }

        /**
         * Sets whether to use rich embeds for message formatting.
         *
         * @param useEmbeds {@code true} to use rich embeds, {@code false} for plain text
         */
        public void setUseEmbeds(boolean useEmbeds) { this.useEmbeds = useEmbeds; }
    }

    /**
     * Configuration settings specific to email notification delivery.
     * <p>
     * Email notifications support both immediate delivery and digest mode, where
     * multiple alerts are batched together and sent at regular intervals. Messages
     * can be formatted using HTML templates for professional presentation, and
     * detailed incident reports can be attached for comprehensive alert documentation.
     * </p>
     */
    public static class EmailConfig {
        /** List of email addresses to receive notifications. Required. */
        private List<String> recipients = new ArrayList<>();

        /** The "From" email address for outgoing notifications. */
        private String fromAddress;

        /** Prefix to add to email subject lines (e.g., "[ALERT]"). */
        private String subjectPrefix;

        /** Whether to use HTML templates for message formatting instead of plain text. */
        private boolean useHtmlTemplate;

        /**
         * Whether to batch multiple alerts into periodic digest emails.
         * When enabled, alerts are collected and sent together at regular intervals.
         */
        private boolean digestMode;

        /** Interval in minutes for sending digest emails when digest mode is enabled. */
        private int digestIntervalMinutes = 15;

        /** Whether to attach a detailed incident report to notification emails. */
        private boolean attachReport;

        /**
         * Returns the list of recipient email addresses.
         *
         * @return the list of email addresses, never {@code null}
         */
        public List<String> getRecipients() { return recipients; }

        /**
         * Sets the list of recipient email addresses.
         *
         * @param recipients the list of email addresses to receive notifications
         */
        public void setRecipients(List<String> recipients) { this.recipients = recipients; }

        /**
         * Returns the "From" email address.
         *
         * @return the sender email address or {@code null} to use system default
         */
        public String getFromAddress() { return fromAddress; }

        /**
         * Sets the "From" email address for outgoing notifications.
         *
         * @param fromAddress the sender email address
         */
        public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }

        /**
         * Returns the subject line prefix.
         *
         * @return the prefix (e.g., "[ALERT]") or {@code null} for no prefix
         */
        public String getSubjectPrefix() { return subjectPrefix; }

        /**
         * Sets the prefix to add to email subject lines.
         *
         * @param subjectPrefix the prefix to prepend (e.g., "[ALERT]", "[DB-MONITOR]")
         */
        public void setSubjectPrefix(String subjectPrefix) { this.subjectPrefix = subjectPrefix; }

        /**
         * Returns whether HTML templates are enabled for message formatting.
         *
         * @return {@code true} if HTML templates should be used
         */
        public boolean isUseHtmlTemplate() { return useHtmlTemplate; }

        /**
         * Sets whether to use HTML templates for message formatting.
         *
         * @param useHtmlTemplate {@code true} to use HTML templates, {@code false} for plain text
         */
        public void setUseHtmlTemplate(boolean useHtmlTemplate) { this.useHtmlTemplate = useHtmlTemplate; }

        /**
         * Returns whether digest mode is enabled.
         *
         * @return {@code true} if alerts should be batched into periodic digests
         */
        public boolean isDigestMode() { return digestMode; }

        /**
         * Sets whether to batch alerts into periodic digest emails.
         *
         * @param digestMode {@code true} to enable digest mode, {@code false} for immediate delivery
         */
        public void setDigestMode(boolean digestMode) { this.digestMode = digestMode; }

        /**
         * Returns the digest interval in minutes.
         *
         * @return the number of minutes between digest emails
         */
        public int getDigestIntervalMinutes() { return digestIntervalMinutes; }

        /**
         * Sets the interval for sending digest emails.
         *
         * @param digestIntervalMinutes the number of minutes between digest emails
         */
        public void setDigestIntervalMinutes(int digestIntervalMinutes) { this.digestIntervalMinutes = digestIntervalMinutes; }

        /**
         * Returns whether incident reports should be attached to emails.
         *
         * @return {@code true} if detailed reports should be attached
         */
        public boolean isAttachReport() { return attachReport; }

        /**
         * Sets whether to attach detailed incident reports to notification emails.
         *
         * @param attachReport {@code true} to attach reports, {@code false} for email body only
         */
        public void setAttachReport(boolean attachReport) { this.attachReport = attachReport; }
    }

    // Getters and Setters

    /**
     * Returns the unique identifier for this notification channel.
     *
     * @return the channel ID or {@code null} if not yet persisted
     */
    public Long getId() { return id; }

    /**
     * Sets the unique identifier for this notification channel.
     *
     * @param id the channel ID
     */
    public void setId(Long id) { this.id = id; }

    /**
     * Returns the user-defined name for this notification channel.
     *
     * @return the channel name
     */
    public String getName() { return name; }

    /**
     * Sets the user-defined name for this notification channel.
     *
     * @param name the channel name
     */
    public void setName(String name) { this.name = name; }

    /**
     * Returns the channel type (Slack, Teams, PagerDuty, Discord, or Email).
     *
     * @return the channel type
     */
    public ChannelType getChannelType() { return channelType; }

    /**
     * Sets the channel type.
     *
     * @param channelType the channel type to use
     */
    public void setChannelType(ChannelType channelType) { this.channelType = channelType; }

    /**
     * Returns whether this channel is enabled for receiving notifications.
     *
     * @return {@code true} if enabled, {@code false} if disabled
     */
    public boolean isEnabled() { return enabled; }

    /**
     * Sets whether this channel is enabled for receiving notifications.
     *
     * @param enabled {@code true} to enable, {@code false} to disable
     */
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    /**
     * Returns the JSON configuration string.
     *
     * @return the JSON configuration or {@code null} if not set
     */
    public String getConfig() { return config; }

    /**
     * Sets the JSON configuration string containing channel-specific settings.
     *
     * @param config the JSON configuration
     */
    public void setConfig(String config) { this.config = config; }

    /**
     * Returns the list of severity levels this channel accepts.
     *
     * @return the severity filter list, never {@code null}
     */
    public List<String> getSeverityFilter() { return severityFilter; }

    /**
     * Sets the list of severity levels this channel accepts.
     *
     * @param severityFilter the severity filter list (empty list accepts all)
     */
    public void setSeverityFilter(List<String> severityFilter) { this.severityFilter = severityFilter; }

    /**
     * Returns the list of alert types this channel accepts.
     *
     * @return the alert type filter list, never {@code null}
     */
    public List<String> getAlertTypeFilter() { return alertTypeFilter; }

    /**
     * Sets the list of alert types this channel accepts.
     *
     * @param alertTypeFilter the alert type filter list (empty list accepts all)
     */
    public void setAlertTypeFilter(List<String> alertTypeFilter) { this.alertTypeFilter = alertTypeFilter; }

    /**
     * Returns the list of database instances this channel monitors.
     *
     * @return the instance filter list, never {@code null}
     */
    public List<String> getInstanceFilter() { return instanceFilter; }

    /**
     * Sets the list of database instances this channel monitors.
     *
     * @param instanceFilter the instance filter list (empty list monitors all)
     */
    public void setInstanceFilter(List<String> instanceFilter) { this.instanceFilter = instanceFilter; }

    /**
     * Returns the maximum number of notifications allowed per hour.
     *
     * @return the rate limit per hour
     */
    public int getRateLimitPerHour() { return rateLimitPerHour; }

    /**
     * Sets the maximum number of notifications allowed per hour to prevent flooding.
     *
     * @param rateLimitPerHour the rate limit per hour
     */
    public void setRateLimitPerHour(int rateLimitPerHour) { this.rateLimitPerHour = rateLimitPerHour; }

    /**
     * Returns whether test mode is enabled.
     *
     * @return {@code true} if notifications are logged but not sent
     */
    public boolean isTestMode() { return testMode; }

    /**
     * Sets whether test mode is enabled for safe configuration testing.
     *
     * @param testMode {@code true} to log only, {@code false} to send notifications
     */
    public void setTestMode(boolean testMode) { this.testMode = testMode; }

    /**
     * Returns the optional description for this notification channel.
     *
     * @return the description or {@code null} if not set
     */
    public String getDescription() { return description; }

    /**
     * Sets the optional description explaining the purpose of this channel.
     *
     * @param description the channel description
     */
    public void setDescription(String description) { this.description = description; }

    /**
     * Returns the timestamp when this channel was created.
     *
     * @return the creation timestamp or {@code null} if not set
     */
    public Instant getCreatedAt() { return createdAt; }

    /**
     * Sets the timestamp when this channel was created.
     *
     * @param createdAt the creation timestamp
     */
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    /**
     * Returns the timestamp when this channel was last updated.
     *
     * @return the last update timestamp or {@code null} if not set
     */
    public Instant getUpdatedAt() { return updatedAt; }

    /**
     * Sets the timestamp when this channel was last updated.
     *
     * @param updatedAt the last update timestamp
     */
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    /**
     * Returns the timestamp when this channel last delivered a notification.
     *
     * @return the last used timestamp or {@code null} if never used
     */
    public Instant getLastUsedAt() { return lastUsedAt; }

    /**
     * Sets the timestamp when this channel last delivered a notification.
     *
     * @param lastUsedAt the last used timestamp
     */
    public void setLastUsedAt(Instant lastUsedAt) { this.lastUsedAt = lastUsedAt; }

    /**
     * Returns the total count of successfully delivered notifications.
     *
     * @return the success count
     */
    public int getSuccessCount() { return successCount; }

    /**
     * Sets the total count of successfully delivered notifications.
     *
     * @param successCount the success count
     */
    public void setSuccessCount(int successCount) { this.successCount = successCount; }

    /**
     * Returns the total count of failed notification delivery attempts.
     *
     * @return the failure count
     */
    public int getFailureCount() { return failureCount; }

    /**
     * Sets the total count of failed notification delivery attempts.
     *
     * @param failureCount the failure count
     */
    public void setFailureCount(int failureCount) { this.failureCount = failureCount; }

    /**
     * Returns the Slack-specific configuration settings.
     *
     * @return the Slack configuration or {@code null} if channel type is not SLACK
     */
    public SlackConfig getSlackConfig() { return slackConfig; }

    /**
     * Sets the Slack-specific configuration settings.
     *
     * @param slackConfig the Slack configuration
     */
    public void setSlackConfig(SlackConfig slackConfig) { this.slackConfig = slackConfig; }

    /**
     * Returns the Microsoft Teams-specific configuration settings.
     *
     * @return the Teams configuration or {@code null} if channel type is not TEAMS
     */
    public TeamsConfig getTeamsConfig() { return teamsConfig; }

    /**
     * Sets the Microsoft Teams-specific configuration settings.
     *
     * @param teamsConfig the Teams configuration
     */
    public void setTeamsConfig(TeamsConfig teamsConfig) { this.teamsConfig = teamsConfig; }

    /**
     * Returns the PagerDuty-specific configuration settings.
     *
     * @return the PagerDuty configuration or {@code null} if channel type is not PAGERDUTY
     */
    public PagerDutyConfig getPagerDutyConfig() { return pagerDutyConfig; }

    /**
     * Sets the PagerDuty-specific configuration settings.
     *
     * @param pagerDutyConfig the PagerDuty configuration
     */
    public void setPagerDutyConfig(PagerDutyConfig pagerDutyConfig) { this.pagerDutyConfig = pagerDutyConfig; }

    /**
     * Returns the Discord-specific configuration settings.
     *
     * @return the Discord configuration or {@code null} if channel type is not DISCORD
     */
    public DiscordConfig getDiscordConfig() { return discordConfig; }

    /**
     * Sets the Discord-specific configuration settings.
     *
     * @param discordConfig the Discord configuration
     */
    public void setDiscordConfig(DiscordConfig discordConfig) { this.discordConfig = discordConfig; }

    /**
     * Returns the Email-specific configuration settings.
     *
     * @return the Email configuration or {@code null} if channel type is not EMAIL
     */
    public EmailConfig getEmailConfig() { return emailConfig; }

    /**
     * Sets the Email-specific configuration settings.
     *
     * @param emailConfig the Email configuration
     */
    public void setEmailConfig(EmailConfig emailConfig) { this.emailConfig = emailConfig; }
}
