package com.bovinemagnet.pgconsole.config;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Configuration bean for enhanced logging and observability features in PG Console.
 * <p>
 * This CDI application-scoped bean provides centralised access to all logging-related
 * configuration properties. It uses Eclipse MicroProfile Config with type-safe property
 * injection to read settings from {@code application.properties} or environment variables.
 * All properties use the {@code pgconsole-logging} namespace prefix.
 * <p>
 * The configuration is organised into several logical groups:
 * <ul>
 *   <li><strong>General Settings</strong> - Log format (JSON vs plain text)</li>
 *   <li><strong>File Logging</strong> - File-based log output with rotation and error separation</li>
 *   <li><strong>SQL Logging</strong> - Database query logging with slow query detection</li>
 *   <li><strong>Redaction</strong> - Automatic masking of sensitive data (passwords, tokens, PII)</li>
 *   <li><strong>Async Logging</strong> - Non-blocking log processing with configurable queues</li>
 *   <li><strong>MDC Context</strong> - Mapped Diagnostic Context for request correlation and tracing</li>
 *   <li><strong>Performance Logging</strong> - Request latency and resource utilisation tracking</li>
 * </ul>
 * <p>
 * Example configuration in {@code application.properties}:
 * <pre>{@code
 * # Enable JSON logging with file output
 * pgconsole-logging.format=json
 * pgconsole-logging.file.enabled=true
 * pgconsole-logging.file.path=/var/log/pg-console
 *
 * # Log slow queries over 500ms
 * pgconsole-logging.sql.enabled=true
 * pgconsole-logging.sql.slow-threshold-ms=500
 *
 * # Automatic redaction of sensitive data
 * pgconsole-logging.redact.enabled=true
 * pgconsole-logging.redact.connection-strings=true
 * }</pre>
 * <p>
 * This bean is immutable after injection and thread-safe. All accessor methods
 * use present tense naming (e.g., {@code format()}, {@code fileEnabled()})
 * following the JavaBeans convention for boolean properties.
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see org.eclipse.microprofile.config.inject.ConfigProperty
 * @since 0.0.0
 */
@ApplicationScoped
public class LoggingConfig {

	// ========== General Settings ==========

	@ConfigProperty(name = "pgconsole-logging.format", defaultValue = "plain")
	String format;

	// ========== File Logging ==========

	@ConfigProperty(name = "pgconsole-logging.file.enabled", defaultValue = "false")
	boolean fileEnabled;

	@ConfigProperty(name = "pgconsole-logging.file.path", defaultValue = "/var/log/pg-console")
	String filePath;

	@ConfigProperty(name = "pgconsole-logging.file.max-size", defaultValue = "10M")
	String fileMaxSize;

	@ConfigProperty(name = "pgconsole-logging.file.max-backup", defaultValue = "5")
	int fileMaxBackup;

	@ConfigProperty(name = "pgconsole-logging.file.file-name", defaultValue = "pg-console.log")
	String fileFileName;

	@ConfigProperty(name = "pgconsole-logging.file.error-log-enabled", defaultValue = "false")
	boolean fileErrorLogEnabled;

	@ConfigProperty(name = "pgconsole-logging.file.error-file-name", defaultValue = "pg-console-error.log")
	String fileErrorFileName;

	// ========== SQL Logging ==========

	@ConfigProperty(name = "pgconsole-logging.sql.enabled", defaultValue = "false")
	boolean sqlEnabled;

	@ConfigProperty(name = "pgconsole-logging.sql.slow-threshold-ms", defaultValue = "1000")
	int sqlSlowThresholdMs;

	@ConfigProperty(name = "pgconsole-logging.sql.log-parameters", defaultValue = "false")
	boolean sqlLogParameters;

	@ConfigProperty(name = "pgconsole-logging.sql.max-query-length", defaultValue = "2000")
	int sqlMaxQueryLength;

	// ========== Redaction ==========

	@ConfigProperty(name = "pgconsole-logging.redact.enabled", defaultValue = "true")
	boolean redactEnabled;

	@ConfigProperty(name = "pgconsole-logging.redact.patterns", defaultValue = "password,secret,token,key,credential,auth,apikey,api_key,bearer,jwt")
	List<String> redactPatterns;

	@ConfigProperty(name = "pgconsole-logging.redact.replacement", defaultValue = "[REDACTED]")
	String redactReplacement;

	@ConfigProperty(name = "pgconsole-logging.redact.mask-pii", defaultValue = "false")
	boolean redactMaskPii;

	@ConfigProperty(name = "pgconsole-logging.redact.redact-connection-strings", defaultValue = "true")
	boolean redactConnectionStrings;

	// ========== Async Logging ==========

	@ConfigProperty(name = "pgconsole-logging.async.enabled", defaultValue = "true")
	boolean asyncEnabled;

	@ConfigProperty(name = "pgconsole-logging.async.queue-size", defaultValue = "1024")
	int asyncQueueSize;

	@ConfigProperty(name = "pgconsole-logging.async.overflow-policy", defaultValue = "BLOCK")
	String asyncOverflowPolicy;

	// ========== MDC Context ==========

	@ConfigProperty(name = "pgconsole-logging.mdc.correlation-id-enabled", defaultValue = "true")
	boolean mdcCorrelationIdEnabled;

	@ConfigProperty(name = "pgconsole-logging.mdc.correlation-id-header", defaultValue = "X-Correlation-ID")
	String mdcCorrelationIdHeader;

	@ConfigProperty(name = "pgconsole-logging.mdc.include-user", defaultValue = "true")
	boolean mdcIncludeUser;

	@ConfigProperty(name = "pgconsole-logging.mdc.include-instance", defaultValue = "true")
	boolean mdcIncludeInstance;

	@ConfigProperty(name = "pgconsole-logging.mdc.include-client-ip", defaultValue = "true")
	boolean mdcIncludeClientIp;

	// ========== Performance Logging ==========

	@ConfigProperty(name = "pgconsole-logging.performance.latency-logging-enabled", defaultValue = "true")
	boolean performanceLatencyLoggingEnabled;

	@ConfigProperty(name = "pgconsole-logging.performance.slow-threshold-ms", defaultValue = "5000")
	int performanceSlowThresholdMs;

	@ConfigProperty(name = "pgconsole-logging.performance.resource-logging-enabled", defaultValue = "false")
	boolean performanceResourceLoggingEnabled;

	@ConfigProperty(name = "pgconsole-logging.performance.resource-logging-interval-seconds", defaultValue = "60")
	int performanceResourceLoggingIntervalSeconds;

	// ========== Accessor Methods ==========

	/**
	 * Returns the log output format for the application.
	 * <p>
	 * Determines whether logs are written in structured JSON format (suitable for
	 * log aggregation systems like ELK, Splunk, or CloudWatch) or human-readable
	 * plain text format (suitable for console output and simple file tailing).
	 * <p>
	 * Valid values:
	 * <ul>
	 *   <li>{@code json} - Structured JSON format with timestamp, level, message, and MDC fields</li>
	 *   <li>{@code plain} - Traditional plain text format with basic formatting</li>
	 * </ul>
	 *
	 * @return the log format, either "json" or "plain" (default: "plain")
	 */
	public String format() {
		return format;
	}

	/**
	 * Indicates whether file-based logging is enabled.
	 * <p>
	 * When enabled, logs are written to files in the directory specified by
	 * {@link #filePath()}. File logging supports automatic rotation based on
	 * {@link #fileMaxSize()} and retention of backup files according to
	 * {@link #fileMaxBackup()}.
	 * <p>
	 * File logging can operate independently or alongside console logging.
	 * When {@link #fileErrorLogEnabled()} is true, errors are additionally
	 * written to a separate error log file.
	 *
	 * @return {@code true} if file logging is enabled, {@code false} otherwise (default: {@code false})
	 * @see #filePath()
	 * @see #fileMaxSize()
	 * @see #fileMaxBackup()
	 */
	public boolean fileEnabled() {
		return fileEnabled;
	}

	/**
	 * Returns the directory path where log files are written.
	 * <p>
	 * This directory must exist or be creatable by the application process.
	 * The application requires write permissions to this directory for log file
	 * creation and rotation. The path should be absolute to avoid ambiguity.
	 * <p>
	 * Log files are created with names specified by {@link #fileFileName()} and
	 * {@link #fileErrorFileName()} (if error logging is enabled).
	 *
	 * @return the log file directory path (default: "/var/log/pg-console")
	 * @see #fileEnabled()
	 * @see #fileFileName()
	 */
	public String filePath() {
		return filePath;
	}

	/**
	 * Returns the maximum size threshold for log file rotation.
	 * <p>
	 * When a log file reaches this size, it is rotated: the current file is renamed
	 * with a timestamp or sequence number, and a new file is started. The size string
	 * supports standard suffixes: K (kilobytes), M (megabytes), or G (gigabytes).
	 * <p>
	 * Examples: "10M" (10 megabytes), "500K" (500 kilobytes), "1G" (1 gigabyte).
	 * <p>
	 * Rotated files are retained according to the {@link #fileMaxBackup()} setting.
	 *
	 * @return the maximum log file size before rotation (default: "10M")
	 * @see #fileMaxBackup()
	 * @see #fileEnabled()
	 */
	public String fileMaxSize() {
		return fileMaxSize;
	}

	/**
	 * Returns the maximum number of rotated log backup files to retain.
	 * <p>
	 * When log files are rotated due to reaching {@link #fileMaxSize()}, older
	 * backup files are retained up to this count. Once this limit is reached,
	 * the oldest backup file is deleted when a new rotation occurs.
	 * <p>
	 * For example, with a value of 5, the system retains the current log file
	 * plus up to 5 backup files (6 files total). This prevents unbounded disk
	 * space usage whilst providing history for troubleshooting.
	 *
	 * @return the maximum number of backup log files to retain (default: 5)
	 * @see #fileMaxSize()
	 * @see #fileEnabled()
	 */
	public int fileMaxBackup() {
		return fileMaxBackup;
	}

	/**
	 * Returns the filename for the main application log file.
	 * <p>
	 * This file contains all log messages (INFO, WARN, ERROR, DEBUG) based on
	 * the configured logging levels. The file is created in the directory
	 * specified by {@link #filePath()}.
	 * <p>
	 * When rotation occurs, this base filename is used with a suffix or timestamp
	 * to create backup files (e.g., pg-console.log.1, pg-console.log.2).
	 *
	 * @return the main log filename (default: "pg-console.log")
	 * @see #filePath()
	 * @see #fileEnabled()
	 */
	public String fileFileName() {
		return fileFileName;
	}

	/**
	 * Indicates whether a separate error log file is enabled.
	 * <p>
	 * When enabled, ERROR and FATAL level log messages are written to both
	 * the main log file (specified by {@link #fileFileName()}) and a dedicated
	 * error log file (specified by {@link #fileErrorFileName()}). This separation
	 * facilitates error monitoring and alerting by providing a focused view
	 * of application failures.
	 * <p>
	 * This setting requires {@link #fileEnabled()} to be {@code true} to have effect.
	 *
	 * @return {@code true} if error log separation is enabled, {@code false} otherwise (default: {@code false})
	 * @see #fileErrorFileName()
	 * @see #fileEnabled()
	 */
	public boolean fileErrorLogEnabled() {
		return fileErrorLogEnabled;
	}

	/**
	 * Returns the filename for the dedicated error log file.
	 * <p>
	 * This file receives only ERROR and FATAL level messages when
	 * {@link #fileErrorLogEnabled()} is {@code true}. It is created in the
	 * same directory as the main log file ({@link #filePath()}) and follows
	 * the same rotation rules based on {@link #fileMaxSize()} and {@link #fileMaxBackup()}.
	 * <p>
	 * Having a separate error log facilitates monitoring, alerting, and analysis
	 * of application failures without scanning through informational messages.
	 *
	 * @return the error log filename (default: "pg-console-error.log")
	 * @see #fileErrorLogEnabled()
	 * @see #filePath()
	 */
	public String fileErrorFileName() {
		return fileErrorFileName;
	}

	/**
	 * Indicates whether SQL query logging is enabled.
	 * <p>
	 * When enabled, SQL queries executed by the application are logged with timing
	 * information. This is useful for identifying database performance issues and
	 * understanding query patterns. Queries exceeding the threshold specified by
	 * {@link #sqlSlowThresholdMs()} are logged with WARNING level to highlight
	 * potential performance problems.
	 * <p>
	 * Query logging can be combined with {@link #sqlLogParameters()} to include
	 * parameter values (exercise caution with sensitive data) and respects the
	 * {@link #sqlMaxQueryLength()} truncation limit for very long queries.
	 * <p>
	 * <strong>Note:</strong> This setting has performance implications and should
	 * be used judiciously in high-throughput production environments.
	 *
	 * @return {@code true} if SQL query logging is enabled, {@code false} otherwise (default: {@code false})
	 * @see #sqlSlowThresholdMs()
	 * @see #sqlLogParameters()
	 * @see #sqlMaxQueryLength()
	 */
	public boolean sqlEnabled() {
		return sqlEnabled;
	}

	/**
	 * Returns the threshold in milliseconds for identifying slow SQL queries.
	 * <p>
	 * Queries that take longer than this threshold to execute are logged at
	 * WARNING level to highlight potential performance issues. This allows
	 * developers and operators to quickly identify problematic queries that
	 * may benefit from optimisation, indexing, or query plan analysis.
	 * <p>
	 * Queries faster than this threshold are logged at DEBUG or TRACE level
	 * (when {@link #sqlEnabled()} is {@code true}), preventing log noise whilst
	 * maintaining visibility into database operations.
	 * <p>
	 * Recommended values:
	 * <ul>
	 *   <li>Development: 500-1000ms for early detection</li>
	 *   <li>Production: 1000-5000ms to focus on significant issues</li>
	 * </ul>
	 *
	 * @return the slow query threshold in milliseconds (default: 1000)
	 * @see #sqlEnabled()
	 */
	public int sqlSlowThresholdMs() {
		return sqlSlowThresholdMs;
	}

	/**
	 * Indicates whether SQL query parameters should be logged.
	 * <p>
	 * When {@code true}, parameter values bound to prepared statements are included
	 * in query logs. This provides complete visibility into executed queries but
	 * carries security and privacy risks as parameters may contain:
	 * <ul>
	 *   <li>Passwords or authentication credentials</li>
	 *   <li>Personally identifiable information (PII)</li>
	 *   <li>Sensitive business data</li>
	 * </ul>
	 * <p>
	 * When enabled, it is strongly recommended to also enable {@link #redactEnabled()}
	 * to automatically mask sensitive values based on field names and patterns.
	 * <p>
	 * This setting only has effect when {@link #sqlEnabled()} is {@code true}.
	 * <p>
	 * <strong>Security Warning:</strong> Use this setting with extreme caution in
	 * production environments. Logs may be stored in unsecured locations, transmitted
	 * to third-party services, or accessed by personnel who should not see sensitive data.
	 *
	 * @return {@code true} to log query parameters, {@code false} otherwise (default: {@code false})
	 * @see #sqlEnabled()
	 * @see #redactEnabled()
	 * @see #redactPatterns()
	 */
	public boolean sqlLogParameters() {
		return sqlLogParameters;
	}

	/**
	 * Returns the maximum length in characters for logged SQL queries.
	 * <p>
	 * Very long SQL queries (such as bulk inserts with many values, complex generated
	 * queries, or queries with large IN clauses) are truncated to this length before
	 * logging. This prevents individual log entries from becoming excessively large
	 * and consuming unnecessary storage or bandwidth.
	 * <p>
	 * Truncated queries include an ellipsis indicator (...) to show that content
	 * has been omitted. The full query can still be viewed through database query
	 * logs or monitoring tools if needed.
	 * <p>
	 * This setting only has effect when {@link #sqlEnabled()} is {@code true}.
	 *
	 * @return the maximum query length in characters (default: 2000)
	 * @see #sqlEnabled()
	 */
	public int sqlMaxQueryLength() {
		return sqlMaxQueryLength;
	}

	/**
	 * Indicates whether automatic redaction of sensitive data is enabled.
	 * <p>
	 * When enabled, the logging framework automatically detects and masks sensitive
	 * information in log messages based on field names, patterns, and data types.
	 * This provides defence-in-depth security by preventing accidental exposure of
	 * credentials, tokens, and other sensitive data in log files.
	 * <p>
	 * Redaction behaviour is controlled by several related settings:
	 * <ul>
	 *   <li>{@link #redactPatterns()} - Field name patterns that trigger redaction</li>
	 *   <li>{@link #redactReplacement()} - Text used to replace redacted values</li>
	 *   <li>{@link #redactMaskPii()} - Whether to mask PII like emails and phone numbers</li>
	 *   <li>{@link #redactConnectionStrings()} - Whether to redact database connection strings</li>
	 * </ul>
	 * <p>
	 * Redaction is particularly important when:
	 * <ul>
	 *   <li>Logs are transmitted to third-party services</li>
	 *   <li>Log files are accessible to multiple teams or contractors</li>
	 *   <li>Regulatory compliance (GDPR, HIPAA, PCI-DSS) is required</li>
	 *   <li>{@link #sqlLogParameters()} is enabled</li>
	 * </ul>
	 *
	 * @return {@code true} if automatic redaction is enabled, {@code false} otherwise (default: {@code true})
	 * @see #redactPatterns()
	 * @see #redactReplacement()
	 * @see #redactMaskPii()
	 * @see #redactConnectionStrings()
	 */
	public boolean redactEnabled() {
		return redactEnabled;
	}

	/**
	 * Returns the list of field name patterns that trigger automatic redaction.
	 * <p>
	 * When a log message includes structured data (such as JSON objects, method parameters,
	 * or SQL parameters), field names matching these patterns have their values replaced
	 * with the {@link #redactReplacement()} text. Pattern matching is case-insensitive
	 * and can match partial field names.
	 * <p>
	 * Default patterns include common sensitive field names:
	 * <ul>
	 *   <li>password, secret, token, key, credential</li>
	 *   <li>auth, apikey, api_key, bearer, jwt</li>
	 * </ul>
	 * <p>
	 * Additional application-specific patterns can be added via configuration:
	 * <pre>{@code
	 * pgconsole-logging.redact.patterns=password,secret,ssn,credit_card,account_number
	 * }</pre>
	 * <p>
	 * This setting only has effect when {@link #redactEnabled()} is {@code true}.
	 *
	 * @return an immutable list of redaction patterns (default: common sensitive field names)
	 * @see #redactEnabled()
	 * @see #redactReplacement()
	 */
	public List<String> redactPatterns() {
		return redactPatterns;
	}

	/**
	 * Returns the replacement text used for redacted values.
	 * <p>
	 * When sensitive data is detected and redacted, this text is substituted in place
	 * of the actual value. The replacement text should be clearly distinguishable to
	 * indicate redaction has occurred, avoiding confusion with legitimate data.
	 * <p>
	 * Common replacement values:
	 * <ul>
	 *   <li>{@code [REDACTED]} - Clear and unambiguous (default)</li>
	 *   <li>{@code ***} - Concise but less explicit</li>
	 *   <li>{@code <masked>} - XML/HTML-style indicator</li>
	 * </ul>
	 * <p>
	 * This setting only has effect when {@link #redactEnabled()} is {@code true}.
	 *
	 * @return the replacement text for redacted values (default: "[REDACTED]")
	 * @see #redactEnabled()
	 * @see #redactPatterns()
	 */
	public String redactReplacement() {
		return redactReplacement;
	}

	/**
	 * Indicates whether personally identifiable information (PII) should be automatically masked.
	 * <p>
	 * When enabled, the logging framework uses pattern recognition to detect and mask
	 * common PII formats including:
	 * <ul>
	 *   <li>Email addresses (e.g., user@example.com becomes u***@example.com)</li>
	 *   <li>Phone numbers (various international formats)</li>
	 *   <li>Credit card numbers (partial masking, showing last 4 digits)</li>
	 *   <li>Social security numbers and national ID formats</li>
	 *   <li>IP addresses (optional, may mask last octet)</li>
	 * </ul>
	 * <p>
	 * This provides an additional layer of protection beyond field name-based redaction
	 * ({@link #redactPatterns()}), catching PII that may appear in free-form text,
	 * exception messages, or log contexts.
	 * <p>
	 * <strong>Note:</strong> Pattern-based PII detection is not foolproof. It should be
	 * combined with other security measures and careful logging practices.
	 * <p>
	 * This setting only has effect when {@link #redactEnabled()} is {@code true}.
	 *
	 * @return {@code true} to mask PII, {@code false} otherwise (default: {@code false})
	 * @see #redactEnabled()
	 * @see #redactPatterns()
	 */
	public boolean redactMaskPii() {
		return redactMaskPii;
	}

	/**
	 * Indicates whether passwords in database connection strings should be redacted.
	 * <p>
	 * When enabled, JDBC connection strings and database URLs that contain passwords
	 * or credentials have those portions automatically masked before logging. This
	 * prevents accidental exposure of database credentials in configuration logs,
	 * startup messages, or error traces.
	 * <p>
	 * Examples of connection strings that are processed:
	 * <ul>
	 *   <li>{@code jdbc:postgresql://host:5432/db?user=admin&password=secret}
	 *       becomes {@code jdbc:postgresql://host:5432/db?user=admin&password=[REDACTED]}</li>
	 *   <li>{@code postgresql://admin:secret@host:5432/db}
	 *       becomes {@code postgresql://admin:[REDACTED]@host:5432/db}</li>
	 * </ul>
	 * <p>
	 * This setting is particularly important for applications that log configuration
	 * details at startup or in error messages where connection strings may appear.
	 * <p>
	 * This setting only has effect when {@link #redactEnabled()} is {@code true}.
	 *
	 * @return {@code true} to redact connection string passwords, {@code false} otherwise (default: {@code true})
	 * @see #redactEnabled()
	 * @see #redactReplacement()
	 */
	public boolean redactConnectionStrings() {
		return redactConnectionStrings;
	}

	/**
	 * Indicates whether asynchronous logging is enabled.
	 * <p>
	 * When enabled, log messages are written to an in-memory queue and processed
	 * by a background thread, preventing logging operations from blocking application
	 * threads. This significantly improves throughput and responsiveness in high-load
	 * scenarios where logging overhead could impact user-facing latency.
	 * <p>
	 * Asynchronous logging behaviour is controlled by:
	 * <ul>
	 *   <li>{@link #asyncQueueSize()} - Maximum number of queued messages before overflow handling</li>
	 *   <li>{@link #asyncOverflowPolicy()} - Action when queue is full (BLOCK or DISCARD)</li>
	 * </ul>
	 * <p>
	 * <strong>Trade-offs:</strong>
	 * <ul>
	 *   <li><strong>Pro:</strong> Reduced latency for application threads</li>
	 *   <li><strong>Pro:</strong> Better throughput under high logging volume</li>
	 *   <li><strong>Con:</strong> Risk of message loss if application crashes before queue drains</li>
	 *   <li><strong>Con:</strong> Timestamps reflect queue time, not log call time (minor skew)</li>
	 * </ul>
	 * <p>
	 * Asynchronous logging is recommended for production environments where performance
	 * is critical and the slight risk of message loss during crashes is acceptable.
	 *
	 * @return {@code true} if asynchronous logging is enabled, {@code false} otherwise (default: {@code true})
	 * @see #asyncQueueSize()
	 * @see #asyncOverflowPolicy()
	 */
	public boolean asyncEnabled() {
		return asyncEnabled;
	}

	/**
	 * Returns the maximum size of the asynchronous logging queue.
	 * <p>
	 * This queue buffers log messages before they are written by the background
	 * logging thread. The size should be large enough to handle bursts of logging
	 * activity without triggering overflow policies, but not so large that it
	 * consumes excessive memory.
	 * <p>
	 * When the queue reaches capacity, behaviour is determined by {@link #asyncOverflowPolicy()}:
	 * <ul>
	 *   <li><strong>BLOCK</strong> - Caller waits until space is available (prevents message loss)</li>
	 *   <li><strong>DISCARD</strong> - Message is dropped (prevents caller blocking)</li>
	 * </ul>
	 * <p>
	 * Recommended sizing:
	 * <ul>
	 *   <li>Low-traffic applications: 256-512</li>
	 *   <li>Moderate traffic: 1024-2048 (default)</li>
	 *   <li>High traffic or burst scenarios: 4096-8192</li>
	 * </ul>
	 * <p>
	 * Monitor queue fullness metrics to determine if the size is adequate. Frequent
	 * queue overflow indicates the need for a larger queue or more aggressive log level filtering.
	 * <p>
	 * This setting only has effect when {@link #asyncEnabled()} is {@code true}.
	 *
	 * @return the maximum queue size in number of messages (default: 1024)
	 * @see #asyncEnabled()
	 * @see #asyncOverflowPolicy()
	 */
	public int asyncQueueSize() {
		return asyncQueueSize;
	}

	/**
	 * Returns the overflow policy for the asynchronous logging queue.
	 * <p>
	 * This policy determines what happens when log messages arrive faster than they
	 * can be written and the queue specified by {@link #asyncQueueSize()} becomes full.
	 * <p>
	 * Valid policies:
	 * <ul>
	 *   <li><strong>BLOCK</strong> (default) - Caller thread waits until queue space is available.
	 *       Guarantees no message loss but may impact application performance if logging
	 *       cannot keep pace with message generation. Use when log message integrity is
	 *       critical.</li>
	 *   <li><strong>DISCARD</strong> - New messages are silently dropped when queue is full.
	 *       Prevents application threads from blocking but results in incomplete logs.
	 *       Use when application responsiveness takes priority over complete logging.</li>
	 * </ul>
	 * <p>
	 * <strong>Choosing a policy:</strong>
	 * <ul>
	 *   <li>Use BLOCK for debugging, troubleshooting, or when investigating issues</li>
	 *   <li>Use DISCARD for production systems where a logging surge should not impact users</li>
	 *   <li>Consider increasing {@link #asyncQueueSize()} before switching to DISCARD</li>
	 * </ul>
	 * <p>
	 * This setting only has effect when {@link #asyncEnabled()} is {@code true}.
	 *
	 * @return the overflow policy, either "BLOCK" or "DISCARD" (default: "BLOCK")
	 * @see #asyncEnabled()
	 * @see #asyncQueueSize()
	 */
	public String asyncOverflowPolicy() {
		return asyncOverflowPolicy;
	}

	/**
	 * Indicates whether correlation ID tracking is enabled in the MDC context.
	 * <p>
	 * When enabled, the logging framework extracts or generates a correlation ID
	 * (also known as a request ID or trace ID) for each HTTP request and stores it
	 * in the Mapped Diagnostic Context (MDC). This ID is automatically included in
	 * all log messages generated during request processing, enabling log aggregation
	 * and distributed tracing across services.
	 * <p>
	 * The correlation ID is extracted from the HTTP header specified by
	 * {@link #mdcCorrelationIdHeader()}. If the header is not present, a new UUID
	 * is generated. The ID is included in HTTP responses to enable client-side
	 * request tracking and debugging.
	 * <p>
	 * <strong>Benefits:</strong>
	 * <ul>
	 *   <li>Correlate all log entries from a single user request</li>
	 *   <li>Trace requests across multiple microservices</li>
	 *   <li>Debug distributed transactions and workflows</li>
	 *   <li>Integrate with distributed tracing systems (Jaeger, Zipkin, etc.)</li>
	 * </ul>
	 * <p>
	 * This is essential for production systems and microservices architectures where
	 * understanding the flow of individual requests is critical for troubleshooting.
	 *
	 * @return {@code true} if correlation ID tracking is enabled, {@code false} otherwise (default: {@code true})
	 * @see #mdcCorrelationIdHeader()
	 * @see #mdcIncludeUser()
	 * @see #mdcIncludeInstance()
	 * @see #mdcIncludeClientIp()
	 */
	public boolean mdcCorrelationIdEnabled() {
		return mdcCorrelationIdEnabled;
	}

	/**
	 * Returns the HTTP header name used for correlation ID extraction.
	 * <p>
	 * When processing HTTP requests, the logging framework checks for this header
	 * to extract an existing correlation ID. This enables correlation ID propagation
	 * across services: upstream services can generate an ID and pass it downstream,
	 * allowing end-to-end tracing of requests through a distributed system.
	 * <p>
	 * Common header names used in the industry:
	 * <ul>
	 *   <li>{@code X-Correlation-ID} (default) - Generic, widely used</li>
	 *   <li>{@code X-Request-ID} - Common in HTTP proxies and API gateways</li>
	 *   <li>{@code X-Trace-ID} - Often used with distributed tracing</li>
	 *   <li>{@code traceparent} - W3C Trace Context standard</li>
	 * </ul>
	 * <p>
	 * If the specified header is not present in an incoming request, the framework
	 * generates a new UUID as the correlation ID. The ID is then included in the
	 * response headers using the same header name, enabling clients to reference
	 * it when reporting issues.
	 * <p>
	 * This setting only has effect when {@link #mdcCorrelationIdEnabled()} is {@code true}.
	 *
	 * @return the HTTP header name for correlation IDs (default: "X-Correlation-ID")
	 * @see #mdcCorrelationIdEnabled()
	 */
	public String mdcCorrelationIdHeader() {
		return mdcCorrelationIdHeader;
	}

	/**
	 * Indicates whether the authenticated user identifier should be included in MDC context.
	 * <p>
	 * When enabled, the username or user ID of the authenticated user making the request
	 * is automatically added to the Mapped Diagnostic Context (MDC) and included in all
	 * log messages during request processing. This enables filtering and searching logs
	 * by user, which is invaluable for:
	 * <ul>
	 *   <li>Debugging user-reported issues</li>
	 *   <li>Auditing user actions and access patterns</li>
	 *   <li>Identifying users affected by errors or performance issues</li>
	 *   <li>Security analysis and incident response</li>
	 * </ul>
	 * <p>
	 * The user identifier is extracted from the security context (typically from JWT
	 * tokens, session data, or authentication headers) after authentication has occurred.
	 * For unauthenticated requests, the MDC field may be absent or contain a placeholder
	 * such as "anonymous".
	 * <p>
	 * <strong>Privacy consideration:</strong> Including user identifiers in logs may have
	 * data protection implications under GDPR, HIPAA, or other privacy regulations.
	 * Ensure logs containing user data are:
	 * <ul>
	 *   <li>Stored securely with appropriate access controls</li>
	 *   <li>Retained according to data retention policies</li>
	 *   <li>Protected when transmitted to log aggregation services</li>
	 * </ul>
	 *
	 * @return {@code true} to include user identifiers in MDC, {@code false} otherwise (default: {@code true})
	 * @see #mdcCorrelationIdEnabled()
	 * @see #mdcIncludeInstance()
	 * @see #mdcIncludeClientIp()
	 */
	public boolean mdcIncludeUser() {
		return mdcIncludeUser;
	}

	/**
	 * Indicates whether the application instance identifier should be included in MDC context.
	 * <p>
	 * When enabled, a unique identifier for this application instance (such as hostname,
	 * pod name, or a configured instance ID) is added to the Mapped Diagnostic Context (MDC)
	 * and included in all log messages. In distributed or load-balanced deployments, this
	 * allows logs from different instances to be distinguished and analysed separately.
	 * <p>
	 * <strong>Use cases:</strong>
	 * <ul>
	 *   <li>Identify which instance generated an error in multi-instance deployments</li>
	 *   <li>Detect instance-specific issues (memory leaks, cache problems, etc.)</li>
	 *   <li>Correlate logs with instance metrics (CPU, memory, connections)</li>
	 *   <li>Debug load balancing and session affinity issues</li>
	 * </ul>
	 * <p>
	 * The instance identifier is typically one of:
	 * <ul>
	 *   <li>Hostname (e.g., "pg-console-prod-01")</li>
	 *   <li>Kubernetes pod name (e.g., "pg-console-6c8f9d7b-xk2m4")</li>
	 *   <li>Container ID or Docker container name</li>
	 *   <li>Configured instance name from environment variables</li>
	 * </ul>
	 * <p>
	 * This is particularly valuable in cloud-native and containerised deployments where
	 * instances are ephemeral and auto-scaled.
	 *
	 * @return {@code true} to include instance identifier in MDC, {@code false} otherwise (default: {@code true})
	 * @see #mdcCorrelationIdEnabled()
	 * @see #mdcIncludeUser()
	 * @see #mdcIncludeClientIp()
	 */
	public boolean mdcIncludeInstance() {
		return mdcIncludeInstance;
	}

	/**
	 * Indicates whether the client IP address should be included in MDC context.
	 * <p>
	 * When enabled, the IP address of the client making the HTTP request is extracted
	 * and added to the Mapped Diagnostic Context (MDC), appearing in all log messages
	 * during request processing. This enables filtering, analysis, and security monitoring
	 * based on client location and identity.
	 * <p>
	 * <strong>Use cases:</strong>
	 * <ul>
	 *   <li>Security monitoring and intrusion detection</li>
	 *   <li>Identifying sources of abusive or malicious traffic</li>
	 *   <li>Debugging client-specific issues (network, CDN, geo-routing)</li>
	 *   <li>Geographic analysis of traffic patterns</li>
	 * </ul>
	 * <p>
	 * The IP address extraction handles common proxy scenarios:
	 * <ul>
	 *   <li>Checks {@code X-Forwarded-For} header for the original client IP when behind proxies</li>
	 *   <li>Checks {@code X-Real-IP} header as an alternative</li>
	 *   <li>Falls back to direct connection IP if headers are absent</li>
	 *   <li>Supports both IPv4 and IPv6 addresses</li>
	 * </ul>
	 * <p>
	 * <strong>Privacy consideration:</strong> IP addresses are considered personally
	 * identifiable information (PII) under GDPR and some other regulations. Ensure
	 * compliance with applicable privacy laws:
	 * <ul>
	 *   <li>Document IP logging in privacy policies</li>
	 *   <li>Apply appropriate data retention limits</li>
	 *   <li>Consider {@link #redactMaskPii()} for partial IP masking</li>
	 *   <li>Secure log storage and transmission</li>
	 * </ul>
	 *
	 * @return {@code true} to include client IP in MDC, {@code false} otherwise (default: {@code true})
	 * @see #mdcCorrelationIdEnabled()
	 * @see #mdcIncludeUser()
	 * @see #mdcIncludeInstance()
	 * @see #redactMaskPii()
	 */
	public boolean mdcIncludeClientIp() {
		return mdcIncludeClientIp;
	}

	/**
	 * Indicates whether HTTP request latency logging is enabled.
	 * <p>
	 * When enabled, the logging framework measures and logs the duration of each HTTP
	 * request from initial receipt to response completion. This provides visibility into
	 * request processing performance and helps identify slow endpoints or operations.
	 * <p>
	 * Request latency information includes:
	 * <ul>
	 *   <li>Total request duration in milliseconds</li>
	 *   <li>HTTP method and path</li>
	 *   <li>Response status code</li>
	 *   <li>MDC context (correlation ID, user, etc. if configured)</li>
	 * </ul>
	 * <p>
	 * Requests exceeding the threshold specified by {@link #performanceSlowThresholdMs()}
	 * are logged at WARNING level to highlight performance issues. Normal requests are
	 * logged at INFO or DEBUG level to avoid log noise.
	 * <p>
	 * <strong>Benefits:</strong>
	 * <ul>
	 *   <li>Identify slow API endpoints and user-facing operations</li>
	 *   <li>Establish performance baselines and SLA compliance</li>
	 *   <li>Detect performance regressions after deployments</li>
	 *   <li>Correlate latency with database query performance</li>
	 * </ul>
	 * <p>
	 * This is recommended for all production environments to maintain visibility into
	 * application performance from the user perspective.
	 *
	 * @return {@code true} if request latency logging is enabled, {@code false} otherwise (default: {@code true})
	 * @see #performanceSlowThresholdMs()
	 * @see #performanceResourceLoggingEnabled()
	 */
	public boolean performanceLatencyLoggingEnabled() {
		return performanceLatencyLoggingEnabled;
	}

	/**
	 * Returns the threshold in milliseconds for identifying slow HTTP requests.
	 * <p>
	 * HTTP requests that take longer than this threshold to complete are logged at
	 * WARNING level to highlight potential performance problems. This enables operators
	 * and developers to quickly identify user-facing latency issues that may require
	 * optimisation.
	 * <p>
	 * Requests faster than this threshold are logged at INFO or DEBUG level when
	 * {@link #performanceLatencyLoggingEnabled()} is {@code true}, maintaining
	 * visibility without creating excessive log noise.
	 * <p>
	 * Recommended thresholds based on application type:
	 * <ul>
	 *   <li><strong>Interactive APIs:</strong> 1000-3000ms - Users notice delays above 1 second</li>
	 *   <li><strong>Background APIs:</strong> 5000-10000ms (default: 5000ms) - Longer operations acceptable</li>
	 *   <li><strong>Real-time APIs:</strong> 100-500ms - Sub-second response critical</li>
	 *   <li><strong>Batch operations:</strong> 10000-30000ms - Long-running processes expected</li>
	 * </ul>
	 * <p>
	 * This threshold is independent of {@link #sqlSlowThresholdMs()}, allowing different
	 * sensitivity levels for database queries versus complete HTTP requests.
	 *
	 * @return the slow request threshold in milliseconds (default: 5000)
	 * @see #performanceLatencyLoggingEnabled()
	 * @see #sqlSlowThresholdMs()
	 */
	public int performanceSlowThresholdMs() {
		return performanceSlowThresholdMs;
	}

	/**
	 * Indicates whether resource utilisation logging is enabled.
	 * <p>
	 * When enabled, the application periodically logs system resource metrics including:
	 * <ul>
	 *   <li>JVM heap memory usage (used, committed, max)</li>
	 *   <li>JVM non-heap memory usage</li>
	 *   <li>CPU utilisation percentage</li>
	 *   <li>Thread count and states</li>
	 *   <li>Garbage collection statistics</li>
	 *   <li>Database connection pool statistics (if applicable)</li>
	 * </ul>
	 * <p>
	 * Resource metrics are sampled at the interval specified by
	 * {@link #performanceResourceLoggingIntervalSeconds()} and logged at INFO level.
	 * This provides historical resource usage data that can be analysed to:
	 * <ul>
	 *   <li>Detect memory leaks and heap exhaustion trends</li>
	 *   <li>Identify CPU saturation and thread pool exhaustion</li>
	 *   <li>Correlate resource usage with load and performance</li>
	 *   <li>Establish capacity planning baselines</li>
	 * </ul>
	 * <p>
	 * <strong>Performance impact:</strong> Resource sampling has minimal overhead but adds
	 * log volume. In environments with dedicated monitoring systems (Prometheus, Datadog, etc.),
	 * this may be redundant. Consider disabling if using external monitoring.
	 * <p>
	 * <strong>Note:</strong> This complements but does not replace dedicated Application
	 * Performance Monitoring (APM) solutions. For production systems, consider using APM
	 * tools alongside or instead of log-based resource monitoring.
	 *
	 * @return {@code true} if resource logging is enabled, {@code false} otherwise (default: {@code false})
	 * @see #performanceResourceLoggingIntervalSeconds()
	 * @see #performanceLatencyLoggingEnabled()
	 */
	public boolean performanceResourceLoggingEnabled() {
		return performanceResourceLoggingEnabled;
	}

	/**
	 * Returns the interval in seconds between resource utilisation log entries.
	 * <p>
	 * This controls how frequently the application samples and logs JVM and system
	 * resource metrics when {@link #performanceResourceLoggingEnabled()} is {@code true}.
	 * <p>
	 * Recommended intervals based on requirements:
	 * <ul>
	 *   <li><strong>High-resolution monitoring:</strong> 10-30 seconds - Captures rapid changes,
	 *       higher log volume</li>
	 *   <li><strong>Standard monitoring:</strong> 60 seconds (default) - Balances visibility
	 *       with log volume</li>
	 *   <li><strong>Long-term trending:</strong> 300-600 seconds (5-10 minutes) - Minimal overhead,
	 *       coarse granularity</li>
	 * </ul>
	 * <p>
	 * Shorter intervals provide better visibility into transient resource spikes but increase
	 * log volume and storage requirements. Longer intervals reduce overhead but may miss
	 * brief resource exhaustion events.
	 * <p>
	 * <strong>Considerations:</strong>
	 * <ul>
	 *   <li>Align with metric retention and aggregation policies</li>
	 *   <li>Consider log storage costs and retention periods</li>
	 *   <li>Balance with external monitoring tool sampling rates</li>
	 * </ul>
	 * <p>
	 * This setting only has effect when {@link #performanceResourceLoggingEnabled()} is {@code true}.
	 *
	 * @return the resource logging interval in seconds (default: 60)
	 * @see #performanceResourceLoggingEnabled()
	 */
	public int performanceResourceLoggingIntervalSeconds() {
		return performanceResourceLoggingIntervalSeconds;
	}
}
