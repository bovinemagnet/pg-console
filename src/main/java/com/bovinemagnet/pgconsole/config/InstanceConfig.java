package com.bovinemagnet.pgconsole.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

import java.util.Map;
import java.util.Optional;

/**
 * Configuration mapping for pg-console application.
 * <p>
 * Includes instance configuration, history sampling, security settings,
 * and schema-free mode configuration.
 * <p>
 * Configuration example:
 * <pre>
 * pg-console.instances=default,production,staging
 * pg-console.instance.production.display-name=Production DB
 * pg-console.history.enabled=true
 * pg-console.history.interval-seconds=60
 * pg-console.security.enabled=false
 * pg-console.schema.enabled=true
 * pg-console.schema.in-memory-minutes=30
 * </pre>
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ConfigMapping(prefix = "pg-console")
public interface InstanceConfig {

    /**
     * Comma-separated list of instance names.
     * The "default" instance uses the unnamed Quarkus datasource.
     * Other instances use named datasources matching the instance name.
     *
     * @return comma-separated instance names
     */
    @WithDefault("default")
    String instances();

    /**
     * Per-instance configuration properties.
     *
     * @return map of instance name to properties
     */
    @WithName("instance")
    Map<String, InstanceProperties> instanceProperties();

    /**
     * History sampling configuration.
     *
     * @return history configuration
     */
    HistoryConfig history();

    /**
     * Security configuration.
     *
     * @return security configuration
     */
    SecurityConfig security();

    /**
     * Alerting configuration.
     *
     * @return alerting configuration
     */
    AlertingConfig alerting();

    /**
     * Schema configuration for read-only mode support.
     *
     * @return schema configuration
     */
    SchemaConfig schema();

    /**
     * Properties for a specific instance.
     */
    interface InstanceProperties {
        /**
         * Display name shown in the UI.
         * Defaults to the instance name if not specified.
         *
         * @return display name
         */
        Optional<String> displayName();
    }

    /**
     * History sampling configuration.
     */
    interface HistoryConfig {
        /**
         * Enable or disable history sampling.
         *
         * @return true if enabled
         */
        @WithDefault("true")
        boolean enabled();

        /**
         * Sampling interval in seconds.
         *
         * @return interval seconds
         */
        @WithName("interval-seconds")
        @WithDefault("60")
        int intervalSeconds();

        /**
         * Number of days to retain history data.
         *
         * @return retention days
         */
        @WithName("retention-days")
        @WithDefault("7")
        int retentionDays();

        /**
         * Number of top queries to sample.
         *
         * @return number of queries
         */
        @WithName("top-queries")
        @WithDefault("50")
        int topQueries();
    }

    /**
     * Schema configuration for read-only mode support.
     * <p>
     * When schema is disabled, the pgconsole schema is not created and the
     * application operates in read-only mode with in-memory trends only.
     */
    interface SchemaConfig {
        /**
         * Enable or disable pgconsole schema creation and persistence.
         * <p>
         * When false:
         * <ul>
         *   <li>No pgconsole schema is created (Flyway migrations skipped)</li>
         *   <li>History sampling is disabled</li>
         *   <li>Audit logging, bookmarks, and alerting history are disabled</li>
         *   <li>In-memory trends are used for sparklines</li>
         * </ul>
         *
         * @return true if schema features are enabled (default)
         */
        @WithDefault("true")
        boolean enabled();

        /**
         * In-memory trend retention in minutes.
         * <p>
         * Only used when schema is disabled. Metrics are stored in memory
         * for this duration to provide short-term sparkline trends.
         * Data is lost on application restart.
         *
         * @return retention minutes (default 30)
         */
        @WithName("in-memory-minutes")
        @WithDefault("30")
        int inMemoryMinutes();
    }

    /**
     * Security configuration.
     */
    interface SecurityConfig {
        /**
         * Enable or disable security (HTTP Basic auth).
         *
         * @return true if security is enabled
         */
        @WithDefault("false")
        boolean enabled();
    }

    /**
     * Alerting configuration.
     */
    interface AlertingConfig {
        /**
         * Enable or disable alerting.
         *
         * @return true if alerting is enabled
         */
        @WithDefault("false")
        boolean enabled();

        /**
         * Webhook URL for sending alerts.
         * If set, alerts will be sent as JSON POST requests.
         *
         * @return optional webhook URL
         */
        @WithName("webhook-url")
        Optional<String> webhookUrl();

        /**
         * Email address for sending alerts.
         * Requires SMTP configuration in Quarkus mailer.
         *
         * @return optional email address
         */
        @WithName("email-to")
        Optional<String> emailTo();

        /**
         * Threshold configuration for alerts.
         *
         * @return threshold configuration
         */
        ThresholdsConfig thresholds();

        /**
         * Minimum interval between alerts of the same type (in seconds).
         * Prevents alert storms.
         *
         * @return cooldown period in seconds
         */
        @WithName("cooldown-seconds")
        @WithDefault("300")
        int cooldownSeconds();
    }

    /**
     * Threshold configuration for triggering alerts.
     */
    interface ThresholdsConfig {
        /**
         * Connection usage percentage threshold.
         * Alert when connections exceed this percentage of max.
         *
         * @return threshold percentage (0-100)
         */
        @WithName("connection-percent")
        @WithDefault("90")
        int connectionPercent();

        /**
         * Blocked queries threshold.
         * Alert when this many queries are blocked.
         *
         * @return number of blocked queries
         */
        @WithName("blocked-queries")
        @WithDefault("5")
        int blockedQueries();

        /**
         * Long-running query threshold in seconds.
         * Alert when any query runs longer than this.
         *
         * @return threshold in seconds
         */
        @WithName("long-query-seconds")
        @WithDefault("300")
        int longQuerySeconds();

        /**
         * Cache hit ratio threshold.
         * Alert when cache hit ratio drops below this percentage.
         *
         * @return threshold percentage (0-100)
         */
        @WithName("cache-hit-ratio")
        @WithDefault("90")
        int cacheHitRatio();
    }
}
