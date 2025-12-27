package com.bovinemagnet.pgconsole.config;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;
import java.util.Optional;

/**
 * Configuration for enhanced logging and observability.
 * <p>
 * Uses CDI injection with Eclipse MicroProfile Config for reliable
 * configuration reading. All properties use the {@code pgconsole-logging} prefix.
 *
 * @author Paul Snow
 * @version 0.0.0
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
     * Log format: 'json' for structured output, 'plain' for human-readable.
     *
     * @return log format
     */
    public String format() {
        return format;
    }

    /**
     * Enable file logging.
     *
     * @return true if enabled
     */
    public boolean fileEnabled() {
        return fileEnabled;
    }

    /**
     * Log file directory path.
     *
     * @return directory path
     */
    public String filePath() {
        return filePath;
    }

    /**
     * Maximum log file size before rotation.
     *
     * @return max size (e.g., "10M")
     */
    public String fileMaxSize() {
        return fileMaxSize;
    }

    /**
     * Number of backup files to retain.
     *
     * @return max backup count
     */
    public int fileMaxBackup() {
        return fileMaxBackup;
    }

    /**
     * Main log file name.
     *
     * @return file name
     */
    public String fileFileName() {
        return fileFileName;
    }

    /**
     * Enable separate error log file.
     *
     * @return true if enabled
     */
    public boolean fileErrorLogEnabled() {
        return fileErrorLogEnabled;
    }

    /**
     * Error log file name.
     *
     * @return error file name
     */
    public String fileErrorFileName() {
        return fileErrorFileName;
    }

    /**
     * Enable SQL query logging.
     *
     * @return true if enabled
     */
    public boolean sqlEnabled() {
        return sqlEnabled;
    }

    /**
     * Threshold in milliseconds for slow query warnings.
     *
     * @return threshold in ms
     */
    public int sqlSlowThresholdMs() {
        return sqlSlowThresholdMs;
    }

    /**
     * Log query parameters (may contain sensitive data).
     *
     * @return true to log parameters
     */
    public boolean sqlLogParameters() {
        return sqlLogParameters;
    }

    /**
     * Maximum query length to log.
     *
     * @return max length in characters
     */
    public int sqlMaxQueryLength() {
        return sqlMaxQueryLength;
    }

    /**
     * Enable automatic redaction of sensitive data.
     *
     * @return true if enabled
     */
    public boolean redactEnabled() {
        return redactEnabled;
    }

    /**
     * Field name patterns that trigger redaction.
     *
     * @return list of patterns
     */
    public List<String> redactPatterns() {
        return redactPatterns;
    }

    /**
     * Replacement text for redacted values.
     *
     * @return replacement string
     */
    public String redactReplacement() {
        return redactReplacement;
    }

    /**
     * Mask personally identifiable information (email, phone, etc.).
     *
     * @return true to mask PII
     */
    public boolean redactMaskPii() {
        return redactMaskPii;
    }

    /**
     * Redact passwords in connection strings.
     *
     * @return true to redact
     */
    public boolean redactConnectionStrings() {
        return redactConnectionStrings;
    }

    /**
     * Enable asynchronous logging for better performance.
     *
     * @return true if enabled
     */
    public boolean asyncEnabled() {
        return asyncEnabled;
    }

    /**
     * Async queue size.
     *
     * @return queue size
     */
    public int asyncQueueSize() {
        return asyncQueueSize;
    }

    /**
     * Overflow policy when queue is full.
     *
     * @return policy (BLOCK or DISCARD)
     */
    public String asyncOverflowPolicy() {
        return asyncOverflowPolicy;
    }

    /**
     * Enable correlation ID tracking.
     *
     * @return true if enabled
     */
    public boolean mdcCorrelationIdEnabled() {
        return mdcCorrelationIdEnabled;
    }

    /**
     * HTTP header name for correlation ID.
     *
     * @return header name
     */
    public String mdcCorrelationIdHeader() {
        return mdcCorrelationIdHeader;
    }

    /**
     * Include user in MDC context.
     *
     * @return true to include
     */
    public boolean mdcIncludeUser() {
        return mdcIncludeUser;
    }

    /**
     * Include instance name in MDC context.
     *
     * @return true to include
     */
    public boolean mdcIncludeInstance() {
        return mdcIncludeInstance;
    }

    /**
     * Include client IP in MDC context.
     *
     * @return true to include
     */
    public boolean mdcIncludeClientIp() {
        return mdcIncludeClientIp;
    }

    /**
     * Enable request latency logging.
     *
     * @return true if enabled
     */
    public boolean performanceLatencyLoggingEnabled() {
        return performanceLatencyLoggingEnabled;
    }

    /**
     * Threshold in milliseconds for slow request warnings.
     *
     * @return threshold in ms
     */
    public int performanceSlowThresholdMs() {
        return performanceSlowThresholdMs;
    }

    /**
     * Enable resource usage logging (CPU, memory).
     *
     * @return true if enabled
     */
    public boolean performanceResourceLoggingEnabled() {
        return performanceResourceLoggingEnabled;
    }

    /**
     * Interval for resource logging in seconds.
     *
     * @return interval in seconds
     */
    public int performanceResourceLoggingIntervalSeconds() {
        return performanceResourceLoggingIntervalSeconds;
    }
}
