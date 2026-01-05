package com.bovinemagnet.pgconsole.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import java.util.Map;
import java.util.Optional;

/**
 * Configuration mapping for the pg-console application's operational settings.
 * <p>
 * This configuration interface defines the complete structure for configuring pg-console,
 * including multi-instance database monitoring, historical data sampling, security controls,
 * alerting thresholds, and metadata storage options. It uses Quarkus's SmallRye Config
 * framework with the {@code @ConfigMapping} annotation to provide type-safe access to
 * application properties.
 * <p>
 * The configuration supports monitoring multiple PostgreSQL instances simultaneously,
 * each with its own datasource and optional display name. The "default" instance maps
 * to Quarkus's unnamed datasource, whilst other instances use named datasources matching
 * the instance identifier.
 * <p>
 * <strong>Configuration Structure:</strong>
 * <ul>
 *   <li><strong>Instances:</strong> Multi-database monitoring with per-instance customisation</li>
 *   <li><strong>History:</strong> Automated sampling of metrics for trend analysis</li>
 *   <li><strong>Security:</strong> HTTP Basic authentication controls</li>
 *   <li><strong>Alerting:</strong> Threshold-based notifications via webhook or email</li>
 *   <li><strong>Schema:</strong> Read-only mode and in-memory trends support</li>
 *   <li><strong>Metadata:</strong> Separate storage for pg-console's operational data</li>
 * </ul>
 * <p>
 * <strong>Configuration Example (application.properties):</strong>
 * <pre>{@code
 * # Multi-instance setup
 * pg-console.instances=default,production,staging
 * pg-console.instance.production.display-name=Production DB
 * pg-console.instance.staging.display-name=Staging Environment
 *
 * # History sampling
 * pg-console.history.enabled=true
 * pg-console.history.interval-seconds=60
 * pg-console.history.retention-days=7
 * pg-console.history.top-queries=50
 *
 * # Security
 * pg-console.security.enabled=false
 *
 * # Alerting
 * pg-console.alerting.enabled=true
 * pg-console.alerting.webhook-url=https://hooks.slack.com/services/YOUR/WEBHOOK/URL
 * pg-console.alerting.thresholds.connection-percent=90
 * pg-console.alerting.thresholds.blocked-queries=5
 * pg-console.alerting.cooldown-seconds=300
 *
 * # Schema (set to false for read-only monitoring)
 * pg-console.schema.enabled=true
 * pg-console.schema.in-memory-minutes=30
 *
 * # Metadata storage (optional separate database)
 * pg-console.metadata.datasource=metadata
 * }</pre>
 * <p>
 * <strong>Read-Only Mode:</strong> When {@code pg-console.schema.enabled=false}, the application
 * operates without creating the {@code pgconsole} schema, using in-memory trends instead. This
 * is useful for monitoring production databases where schema modifications are restricted.
 * <p>
 * <strong>Metadata Separation:</strong> The {@code metadata.datasource} setting enables storing
 * pg-console's operational data (history samples, bookmarks, audit logs) in a separate database
 * from the monitored instances, supporting read-only production monitoring scenarios.
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see InstanceProperties
 * @see HistoryConfig
 * @see SecurityConfig
 * @see AlertingConfig
 * @see SchemaConfig
 * @see MetadataConfig
 * @since 0.0.0
 */
@ConfigMapping(prefix = "pg-console")
public interface InstanceConfig {
	/**
	 * Retrieves the comma-separated list of PostgreSQL instance names to monitor.
	 * <p>
	 * The "default" instance is special and maps to Quarkus's unnamed datasource
	 * configuration. All other instance names must have corresponding named datasource
	 * configurations (e.g., {@code quarkus.datasource.production.jdbc.url}).
	 * <p>
	 * Example: {@code "default,production,staging"} configures three instances.
	 *
	 * @return comma-separated string of instance identifiers, defaults to "default"
	 * @see InstanceProperties
	 */
	@WithDefault("default")
	String instances();

	/**
	 * Retrieves the per-instance configuration properties map.
	 * <p>
	 * Maps instance identifiers to their customisation settings. Each instance
	 * can have properties such as a custom display name for the user interface.
	 * Keys in this map correspond to instance names declared in {@link #instances()}.
	 *
	 * @return map with instance names as keys and {@link InstanceProperties} as values
	 * @see InstanceProperties
	 */
	@WithName("instance")
	Map<String, InstanceProperties> instanceProperties();

	/**
	 * Retrieves the history sampling configuration.
	 * <p>
	 * Controls automated collection of database metrics over time, including
	 * sampling intervals, retention periods, and the number of top queries to track.
	 *
	 * @return the {@link HistoryConfig} for metric sampling settings
	 * @see HistoryConfig
	 */
	HistoryConfig history();

	/**
	 * Retrieves the security configuration.
	 * <p>
	 * Determines whether HTTP Basic authentication is enabled for accessing
	 * the pg-console web interface.
	 *
	 * @return the {@link SecurityConfig} for authentication settings
	 * @see SecurityConfig
	 */
	SecurityConfig security();

	/**
	 * Retrieves the alerting configuration.
	 * <p>
	 * Controls threshold-based alerting for database conditions such as high
	 * connection usage, blocked queries, and low cache hit ratios. Alerts can
	 * be delivered via webhooks or email.
	 *
	 * @return the {@link AlertingConfig} for notification settings
	 * @see AlertingConfig
	 * @see ThresholdsConfig
	 */
	AlertingConfig alerting();

	/**
	 * Retrieves the schema configuration for read-only mode support.
	 * <p>
	 * Controls whether the {@code pgconsole} schema is created in monitored databases.
	 * When disabled, the application operates in read-only mode with in-memory trends.
	 *
	 * @return the {@link SchemaConfig} for schema creation and in-memory settings
	 * @see SchemaConfig
	 */
	SchemaConfig schema();

	/**
	 * Retrieves the metadata datasource configuration.
	 * <p>
	 * Allows the {@code pgconsole} schema (containing history samples, bookmarks,
	 * and audit logs) to be stored in a separate database from the monitored instances.
	 * This enables read-only monitoring of production databases by storing operational
	 * data elsewhere.
	 *
	 * @return the {@link MetadataConfig} for metadata storage location
	 * @see MetadataConfig
	 */
	MetadataConfig metadata();

	/**
	 * Configuration properties for an individual PostgreSQL instance.
	 * <p>
	 * Provides customisation options for each monitored database instance, such as
	 * user-friendly display names for the web interface. Properties are configured
	 * using the pattern {@code pg-console.instance.<instance-name>.<property>}.
	 * <p>
	 * Example configuration:
	 * <pre>{@code
	 * pg-console.instance.production.display-name=Production Database (EU-West)
	 * pg-console.instance.staging.display-name=Staging Environment
	 * }</pre>
	 *
	 * @author Paul Snow
	 * @version 0.0.0
	 * @since 0.0.0
	 */
	interface InstanceProperties {
		/**
		 * Retrieves the display name shown in the user interface for this instance.
		 * <p>
		 * If not specified, the instance identifier (key from the instances list) is
		 * used as the display name. This allows for more descriptive labels in the UI
		 * whilst keeping instance identifiers concise.
		 *
		 * @return optional display name, or empty if not configured
		 */
		Optional<String> displayName();
	}

	/**
	 * Configuration for automated history sampling of database metrics.
	 * <p>
	 * Controls the collection and retention of time-series data from PostgreSQL
	 * instances, enabling trend analysis through sparklines and historical dashboards.
	 * Metrics are stored in the {@code pgconsole.metric_history} and
	 * {@code pgconsole.query_history} tables (unless schema is disabled).
	 * <p>
	 * Sampling is performed by a scheduled background task that queries PostgreSQL
	 * system views ({@code pg_stat_database}, {@code pg_stat_statements}) at regular
	 * intervals. Historical data older than the retention period is automatically purged.
	 * <p>
	 * Example configuration:
	 * <pre>{@code
	 * pg-console.history.enabled=true
	 * pg-console.history.interval-seconds=60
	 * pg-console.history.retention-days=7
	 * pg-console.history.top-queries=50
	 * }</pre>
	 *
	 * @author Paul Snow
	 * @version 0.0.0
	 * @since 0.0.0
	 */
	interface HistoryConfig {
		/**
		 * Determines whether history sampling is enabled.
		 * <p>
		 * When disabled, no historical data is collected and sparkline trends are not
		 * available (unless in-memory trends are enabled via {@link SchemaConfig}).
		 * Disabling history reduces database write operations.
		 *
		 * @return {@code true} if history sampling is enabled, {@code false} otherwise (default: {@code true})
		 */
		@WithDefault("true")
		boolean enabled();

		/**
		 * Retrieves the sampling interval in seconds.
		 * <p>
		 * Determines how frequently database metrics are collected. Lower values provide
		 * higher resolution trends but increase database load and storage requirements.
		 * Typical values range from 30 seconds (high-frequency) to 300 seconds (5 minutes).
		 *
		 * @return sampling interval in seconds (default: 60)
		 */
		@WithName("interval-seconds")
		@WithDefault("60")
		int intervalSeconds();

		/**
		 * Retrieves the number of days to retain history data.
		 * <p>
		 * Historical samples older than this period are automatically deleted by a
		 * scheduled cleanup task. Longer retention periods enable long-term trend
		 * analysis but consume more storage.
		 *
		 * @return retention period in days (default: 7)
		 */
		@WithName("retention-days")
		@WithDefault("7")
		int retentionDays();

		/**
		 * Retrieves the number of top queries to sample from {@code pg_stat_statements}.
		 * <p>
		 * During each sampling interval, the N slowest queries (by total execution time)
		 * are recorded in {@code pgconsole.query_history}. This enables tracking query
		 * performance trends over time. Higher values capture more queries but increase
		 * storage requirements.
		 *
		 * @return number of top queries to track (default: 50)
		 */
		@WithName("top-queries")
		@WithDefault("50")
		int topQueries();
	}

	/**
	 * Configuration for the {@code pgconsole} schema and read-only monitoring mode.
	 * <p>
	 * Controls whether pg-console creates its operational schema in monitored databases.
	 * When schema creation is disabled, the application operates in read-only mode,
	 * using in-memory data structures for trend visualisation without persisting any
	 * data to the monitored databases.
	 * <p>
	 * This is particularly useful for monitoring production databases where schema
	 * modifications are restricted or where avoiding any write operations is required
	 * for compliance or security reasons.
	 * <p>
	 * <strong>Read-Only Mode Behaviour:</strong>
	 * When {@code enabled=false}:
	 * <ul>
	 *   <li>Flyway migrations are skipped (no {@code pgconsole} schema created)</li>
	 *   <li>History sampling is automatically disabled</li>
	 *   <li>Audit logging, bookmarks, and alerting history are unavailable</li>
	 *   <li>Sparkline trends use in-memory circular buffers</li>
	 *   <li>Trend data is lost on application restart</li>
	 * </ul>
	 * <p>
	 * Example configuration:
	 * <pre>{@code
	 * # Read-only mode for production monitoring
	 * pg-console.schema.enabled=false
	 * pg-console.schema.in-memory-minutes=30
	 * }</pre>
	 *
	 * @author Paul Snow
	 * @version 0.0.0
	 * @since 0.0.0
	 */
	interface SchemaConfig {
		/**
		 * Determines whether the {@code pgconsole} schema is created in monitored databases.
		 * <p>
		 * When {@code false}, the application operates in read-only mode without creating
		 * any database objects. This prevents Flyway migrations from executing and disables
		 * all features requiring persistent storage (history sampling, bookmarks, audit logs).
		 * <p>
		 * When {@code true} (default), Flyway migrations create the {@code pgconsole} schema
		 * with tables for metric history, query history, bookmarks, and audit logs.
		 *
		 * @return {@code true} if schema creation is enabled (default), {@code false} for read-only mode
		 */
		@WithDefault("true")
		boolean enabled();

		/**
		 * Retrieves the in-memory trend retention period in minutes.
		 * <p>
		 * When schema is disabled ({@link #enabled()} returns {@code false}), metrics are
		 * stored in memory-resident circular buffers for this duration. This enables
		 * sparkline visualisation without database persistence. Data is discarded when
		 * it exceeds this age or when the application restarts.
		 * <p>
		 * Longer retention provides more historical context in sparklines but increases
		 * memory consumption. This setting is ignored when schema is enabled.
		 *
		 * @return in-memory retention period in minutes (default: 30)
		 */
		@WithName("in-memory-minutes")
		@WithDefault("30")
		int inMemoryMinutes();
	}

	/**
	 * Configuration for metadata datasource separation.
	 * <p>
	 * Enables the {@code pgconsole} schema (containing history samples, bookmarks,
	 * audit logs, and alerting state) to be stored in a separate database from the
	 * monitored PostgreSQL instances. This architectural pattern supports several
	 * deployment scenarios:
	 * <ul>
	 *   <li><strong>Read-only monitoring:</strong> Monitor production databases without
	 *       write permissions by storing all pg-console metadata elsewhere</li>
	 *   <li><strong>Multi-instance correlation:</strong> Centralise historical data from
	 *       multiple instances in a single database for cross-instance analysis</li>
	 *   <li><strong>Separation of concerns:</strong> Keep monitoring metadata distinct
	 *       from application data for cleaner database organisation</li>
	 *   <li><strong>Compliance:</strong> Satisfy regulatory requirements prohibiting
	 *       modification of production database schemas</li>
	 * </ul>
	 * <p>
	 * When configured, Flyway migrations execute against the metadata datasource
	 * rather than the monitored instances, creating the {@code pgconsole} schema
	 * there. All monitoring instances are then accessed in read-only mode.
	 * <p>
	 * Example configuration:
	 * <pre>{@code
	 * # Separate metadata storage
	 * pg-console.metadata.datasource=metadata
	 *
	 * # Metadata datasource configuration
	 * quarkus.datasource.metadata.jdbc.url=jdbc:postgresql://metadata-db:5432/pgconsole_metadata
	 * quarkus.datasource.metadata.username=pgconsole
	 * quarkus.datasource.metadata.password=secret
	 * }</pre>
	 *
	 * @author Paul Snow
	 * @version 0.0.0
	 * @since 0.0.0
	 */
	interface MetadataConfig {
		/**
		 * Retrieves the datasource name for storing pg-console metadata.
		 * <p>
		 * Determines which database receives the {@code pgconsole} schema containing
		 * operational data such as metric history, query history, user bookmarks, and
		 * audit logs. The value must correspond to a configured Quarkus datasource name.
		 * <p>
		 * <strong>Supported values:</strong>
		 * <ul>
		 *   <li><strong>Empty/not set:</strong> Uses the "default" datasource (backwards
		 *       compatible behaviour; metadata stored alongside monitored data)</li>
		 *   <li><strong>"metadata":</strong> Uses a dedicated metadata datasource configured
		 *       as {@code quarkus.datasource.metadata.*}</li>
		 *   <li><strong>Instance name:</strong> Uses the datasource for a specific monitored
		 *       instance (e.g., "production" to store metadata in the production database)</li>
		 * </ul>
		 * <p>
		 * If the specified datasource does not exist, the application fails to start
		 * with a configuration error.
		 *
		 * @return optional datasource name, or empty to use the default datasource
		 */
		Optional<String> datasource();
	}

	/**
	 * Configuration for web interface security controls.
	 * <p>
	 * Controls authentication requirements for accessing the pg-console web interface.
	 * When enabled, HTTP Basic authentication is enforced for all dashboard endpoints,
	 * requiring users to provide credentials before viewing database metrics.
	 * <p>
	 * Authentication is implemented using Quarkus Security with credentials managed
	 * through Quarkus's identity provider configuration (typically properties-based,
	 * LDAP, or database authentication). By default, security is disabled for ease
	 * of development, but should be enabled in production environments.
	 * <p>
	 * Example configuration:
	 * <pre>{@code
	 * # Enable HTTP Basic authentication
	 * pg-console.security.enabled=true
	 *
	 * # Configure users (Quarkus Security)
	 * quarkus.security.users.embedded.enabled=true
	 * quarkus.security.users.embedded.plain-text=true
	 * quarkus.security.users.embedded.users.admin=secretPassword
	 * quarkus.security.users.embedded.roles.admin=admin
	 * }</pre>
	 *
	 * @author Paul Snow
	 * @version 0.0.0
	 * @since 0.0.0
	 */
	interface SecurityConfig {
		/**
		 * Determines whether HTTP Basic authentication is required for web access.
		 * <p>
		 * When {@code true}, all pg-console dashboard endpoints require valid credentials.
		 * Unauthenticated requests receive a 401 Unauthorised response with a WWW-Authenticate
		 * challenge, prompting the browser for credentials.
		 * <p>
		 * When {@code false} (default), the web interface is accessible without authentication.
		 * This is suitable for development environments or when pg-console is deployed behind
		 * a reverse proxy or VPN that handles authentication.
		 *
		 * @return {@code true} if authentication is required, {@code false} for open access (default: {@code false})
		 */
		@WithDefault("false")
		boolean enabled();
	}

	/**
	 * Configuration for automated alerting based on database health thresholds.
	 * <p>
	 * Enables proactive monitoring by automatically sending notifications when database
	 * conditions exceed configured thresholds. Alerts can be delivered via webhooks
	 * (e.g., Slack, Microsoft Teams, PagerDuty) or email, with configurable cooldown
	 * periods to prevent alert fatigue.
	 * <p>
	 * The alerting system monitors conditions such as:
	 * <ul>
	 *   <li>Connection pool exhaustion (percentage of maximum connections)</li>
	 *   <li>Blocked query count (queries waiting on locks)</li>
	 *   <li>Long-running queries exceeding time limits</li>
	 *   <li>Low cache hit ratios indicating insufficient shared buffers</li>
	 * </ul>
	 * <p>
	 * Alerts are evaluated periodically by a scheduled background task. Each alert
	 * type respects a cooldown period to avoid repeated notifications for the same
	 * ongoing condition. Alert history is stored in the {@code pgconsole.alert_log}
	 * table (when schema is enabled).
	 * <p>
	 * Example configuration:
	 * <pre>{@code
	 * # Enable alerting
	 * pg-console.alerting.enabled=true
	 *
	 * # Slack webhook integration
	 * pg-console.alerting.webhook-url=https://hooks.slack.com/services/T00/B00/XX
	 *
	 * # Email integration (requires Quarkus Mailer configuration)
	 * pg-console.alerting.email-to=dba-team@example.com
	 *
	 * # Threshold configuration
	 * pg-console.alerting.thresholds.connection-percent=85
	 * pg-console.alerting.thresholds.blocked-queries=3
	 * pg-console.alerting.thresholds.long-query-seconds=600
	 * pg-console.alerting.thresholds.cache-hit-ratio=95
	 *
	 * # Alert cooldown (5 minutes)
	 * pg-console.alerting.cooldown-seconds=300
	 * }</pre>
	 *
	 * @author Paul Snow
	 * @version 0.0.0
	 * @see ThresholdsConfig
	 * @since 0.0.0
	 */
	interface AlertingConfig {
		/**
		 * Determines whether the alerting system is active.
		 * <p>
		 * When {@code true}, a scheduled background task periodically evaluates database
		 * metrics against configured thresholds and sends notifications when conditions
		 * are breached. When {@code false} (default), no alerts are generated regardless
		 * of database conditions.
		 * <p>
		 * At least one delivery method ({@link #webhookUrl()} or {@link #emailTo()})
		 * must be configured for alerts to be sent.
		 *
		 * @return {@code true} if alerting is enabled, {@code false} otherwise (default: {@code false})
		 */
		@WithDefault("false")
		boolean enabled();

		/**
		 * Retrieves the webhook URL for delivering alerts via HTTP POST.
		 * <p>
		 * When configured, alerts are sent as JSON POST requests to this URL. This
		 * enables integration with platforms such as Slack (incoming webhooks),
		 * Microsoft Teams (connectors), PagerDuty, or custom alerting systems.
		 * <p>
		 * The JSON payload includes alert metadata such as instance name, alert type,
		 * severity, threshold values, current values, and timestamps. The exact schema
		 * is designed to be compatible with common webhook receivers.
		 * <p>
		 * Example Slack webhook:
		 * {@code https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXX}
		 *
		 * @return optional webhook URL, or empty if webhook delivery is not configured
		 */
		@WithName("webhook-url")
		Optional<String> webhookUrl();

		/**
		 * Retrieves the email address for delivering alerts via SMTP.
		 * <p>
		 * When configured, alerts are sent as formatted emails to this address. Email
		 * delivery requires Quarkus Mailer to be configured with SMTP settings in
		 * {@code application.properties} (e.g., {@code quarkus.mailer.host},
		 * {@code quarkus.mailer.username}, {@code quarkus.mailer.password}).
		 * <p>
		 * Alert emails include subject lines indicating severity and instance, with
		 * HTML-formatted bodies containing detailed threshold breach information and
		 * links to relevant pg-console dashboards.
		 * <p>
		 * Example configuration:
		 * <pre>{@code
		 * pg-console.alerting.email-to=database-alerts@example.com
		 * quarkus.mailer.host=smtp.example.com
		 * quarkus.mailer.port=587
		 * quarkus.mailer.start-tls=REQUIRED
		 * quarkus.mailer.username=alerts@example.com
		 * quarkus.mailer.password=secret
		 * }</pre>
		 *
		 * @return optional email address, or empty if email delivery is not configured
		 */
		@WithName("email-to")
		Optional<String> emailTo();

		/**
		 * Retrieves the threshold configuration defining alert trigger conditions.
		 * <p>
		 * Thresholds determine when alerts are generated based on database metrics.
		 * Each threshold type has a default value suitable for typical production
		 * environments, but should be tuned based on specific database workloads
		 * and operational requirements.
		 *
		 * @return the {@link ThresholdsConfig} containing alert thresholds
		 * @see ThresholdsConfig
		 */
		ThresholdsConfig thresholds();

		/**
		 * Retrieves the minimum time between repeated alerts of the same type.
		 * <p>
		 * The cooldown period prevents alert storms when a database condition remains
		 * breached over an extended period. After an alert is sent, subsequent alerts
		 * of the same type (e.g., high connection usage on the same instance) are
		 * suppressed until this duration elapses.
		 * <p>
		 * Cooldown tracking is per-alert-type and per-instance, allowing different
		 * alert types to fire independently whilst preventing duplicate notifications
		 * for ongoing conditions.
		 * <p>
		 * Typical values:
		 * <ul>
		 *   <li>300 seconds (5 minutes) - default, suitable for most environments</li>
		 *   <li>600 seconds (10 minutes) - for less critical alerts</li>
		 *   <li>60 seconds (1 minute) - for critical production systems requiring rapid awareness</li>
		 * </ul>
		 *
		 * @return cooldown period in seconds (default: 300)
		 */
		@WithName("cooldown-seconds")
		@WithDefault("300")
		int cooldownSeconds();
	}

	/**
	 * Configuration for alert threshold values that trigger notifications.
	 * <p>
	 * Defines the specific metric levels that cause pg-console to generate alerts.
	 * Each threshold type monitors a different aspect of database health, enabling
	 * comprehensive coverage of common operational issues:
	 * <ul>
	 *   <li><strong>Connection exhaustion:</strong> Detects when connection pools
	 *       approach capacity, risking connection refusals</li>
	 *   <li><strong>Lock contention:</strong> Identifies blocking query situations
	 *       that indicate potential deadlocks or slow transactions</li>
	 *   <li><strong>Query performance:</strong> Catches runaway queries consuming
	 *       excessive resources or indicating application bugs</li>
	 *   <li><strong>Memory pressure:</strong> Recognises low cache hit ratios suggesting
	 *       undersized shared buffers or inefficient query patterns</li>
	 * </ul>
	 * <p>
	 * Thresholds should be tuned based on:
	 * <ul>
	 *   <li>Database workload characteristics (OLTP vs OLAP)</li>
	 *   <li>Historical performance baselines</li>
	 *   <li>Service level objectives (SLOs)</li>
	 *   <li>Team response capacity</li>
	 * </ul>
	 * <p>
	 * Example configuration balancing sensitivity with alert fatigue:
	 * <pre>{@code
	 * # Conservative thresholds for production
	 * pg-console.alerting.thresholds.connection-percent=85
	 * pg-console.alerting.thresholds.blocked-queries=3
	 * pg-console.alerting.thresholds.long-query-seconds=600
	 * pg-console.alerting.thresholds.cache-hit-ratio=95
	 * }</pre>
	 *
	 * @author Paul Snow
	 * @version 0.0.0
	 * @since 0.0.0
	 */
	interface ThresholdsConfig {
		/**
		 * Retrieves the connection usage percentage threshold for alerts.
		 * <p>
		 * Triggers an alert when the number of active database connections exceeds this
		 * percentage of the PostgreSQL {@code max_connections} setting. High connection
		 * usage can lead to connection pool exhaustion, causing application errors and
		 * degraded performance.
		 * <p>
		 * The threshold is evaluated against the ratio:
		 * {@code (current_connections / max_connections) * 100}
		 * <p>
		 * <strong>Recommended values:</strong>
		 * <ul>
		 *   <li><strong>85-90%:</strong> Standard threshold providing early warning</li>
		 *   <li><strong>95%:</strong> Critical threshold for immediate investigation</li>
		 *   <li><strong>70-80%:</strong> Conservative threshold for databases with
		 *       unpredictable connection spikes</li>
		 * </ul>
		 * <p>
		 * Consider lowering this threshold if connection pools are sized close to
		 * {@code max_connections}, or if connection acquisition failures have occurred
		 * historically.
		 *
		 * @return threshold percentage (0-100, default: 90)
		 */
		@WithName("connection-percent")
		@WithDefault("90")
		int connectionPercent();

		/**
		 * Retrieves the blocked query count threshold for alerts.
		 * <p>
		 * Triggers an alert when this number of queries are simultaneously blocked,
		 * waiting for locks held by other transactions. Blocked queries indicate lock
		 * contention, which can cascade into application timeouts and degraded user
		 * experience.
		 * <p>
		 * This metric is derived from {@code pg_stat_activity} by counting queries in
		 * the "active" state with non-null {@code wait_event_type} values indicating
		 * lock waits (e.g., "Lock", "heavyweight lock").
		 * <p>
		 * <strong>Recommended values:</strong>
		 * <ul>
		 *   <li><strong>5-10:</strong> Standard threshold for OLTP systems</li>
		 *   <li><strong>2-3:</strong> Sensitive threshold for low-latency requirements</li>
		 *   <li><strong>20+:</strong> Relaxed threshold for batch processing systems
		 *       where some lock contention is expected</li>
		 * </ul>
		 *
		 * @return threshold count (default: 5)
		 */
		@WithName("blocked-queries")
		@WithDefault("5")
		int blockedQueries();

		/**
		 * Retrieves the long-running query duration threshold in seconds.
		 * <p>
		 * Triggers an alert when any active query exceeds this execution time. Long-running
		 * queries can indicate application bugs (missing {@code WHERE} clauses, N+1 problems),
		 * inefficient query plans, or stuck transactions holding locks.
		 * <p>
		 * The threshold is evaluated against the {@code state_duration} from
		 * {@code pg_stat_activity} for queries in the "active" state.
		 * <p>
		 * <strong>Recommended values:</strong>
		 * <ul>
		 *   <li><strong>300s (5 minutes):</strong> Default, suitable for mixed workloads</li>
		 *   <li><strong>60s (1 minute):</strong> Strict threshold for OLTP systems where
		 *       all queries should complete quickly</li>
		 *   <li><strong>1800s (30 minutes):</strong> Relaxed threshold for analytics
		 *       databases with legitimate long-running reports</li>
		 * </ul>
		 * <p>
		 * Consider different thresholds for different query types by configuring multiple
		 * pg-console instances or filtering alert recipients.
		 *
		 * @return threshold in seconds (default: 300)
		 */
		@WithName("long-query-seconds")
		@WithDefault("300")
		int longQuerySeconds();

		/**
		 * Retrieves the minimum acceptable cache hit ratio percentage.
		 * <p>
		 * Triggers an alert when the database's buffer cache hit ratio falls below this
		 * percentage, indicating excessive disk I/O. Low cache hit ratios can result from
		 * undersized {@code shared_buffers}, inefficient queries performing full table scans,
		 * or workload changes introducing cold data access patterns.
		 * <p>
		 * The ratio is calculated from {@code pg_stat_database} as:
		 * {@code (blks_hit / (blks_hit + blks_read)) * 100}
		 * <p>
		 * <strong>Recommended values:</strong>
		 * <ul>
		 *   <li><strong>90-95%:</strong> Typical threshold for OLTP systems with
		 *       working sets fitting in memory</li>
		 *   <li><strong>80-85%:</strong> Relaxed threshold for analytics workloads
		 *       with large sequential scans</li>
		 *   <li><strong>99%:</strong> Aggressive threshold for databases with very
		 *       large {@code shared_buffers} and hot data access patterns</li>
		 * </ul>
		 * <p>
		 * Note that cache hit ratios below 90% often indicate opportunities for
		 * performance optimisation through increased memory allocation or query tuning.
		 *
		 * @return threshold percentage (0-100, default: 90)
		 */
		@WithName("cache-hit-ratio")
		@WithDefault("90")
		int cacheHitRatio();
	}
}
