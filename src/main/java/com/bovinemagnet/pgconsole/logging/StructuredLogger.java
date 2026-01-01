package com.bovinemagnet.pgconsole.logging;

import com.bovinemagnet.pgconsole.config.LoggingConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Structured logging service wrapper providing enhanced logging capabilities.
 * <p>
 * Provides:
 * <ul>
 *   <li>Automatic MDC context inclusion</li>
 *   <li>Sensitive data redaction</li>
 *   <li>Structured log events with metadata</li>
 *   <li>Performance timing utilities</li>
 *   <li>Exception logging with redaction</li>
 * </ul>
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class StructuredLogger {

    private static final Logger LOG = Logger.getLogger(StructuredLogger.class);

    @Inject
    LoggingConfig loggingConfig;

    @Inject
    LogRedactionService redactionService;

    /**
     * Logs an informational message with optional metadata.
     *
     * @param category log category/source
     * @param message log message
     * @param metadata additional key-value pairs
     */
    public void info(String category, String message, Map<String, Object> metadata) {
        log(Logger.Level.INFO, category, message, metadata, null);
    }

    /**
     * Logs an informational message.
     *
     * @param category log category/source
     * @param message log message
     */
    public void info(String category, String message) {
        log(Logger.Level.INFO, category, message, null, null);
    }

    /**
     * Logs a warning message with optional metadata.
     *
     * @param category log category/source
     * @param message log message
     * @param metadata additional key-value pairs
     */
    public void warn(String category, String message, Map<String, Object> metadata) {
        log(Logger.Level.WARN, category, message, metadata, null);
    }

    /**
     * Logs a warning message.
     *
     * @param category log category/source
     * @param message log message
     */
    public void warn(String category, String message) {
        log(Logger.Level.WARN, category, message, null, null);
    }

    /**
     * Logs an error message with optional metadata and exception.
     *
     * @param category log category/source
     * @param message log message
     * @param metadata additional key-value pairs
     * @param throwable exception to log
     */
    public void error(String category, String message, Map<String, Object> metadata, Throwable throwable) {
        log(Logger.Level.ERROR, category, message, metadata, throwable);
    }

    /**
     * Logs an error message with exception.
     *
     * @param category log category/source
     * @param message log message
     * @param throwable exception to log
     */
    public void error(String category, String message, Throwable throwable) {
        log(Logger.Level.ERROR, category, message, null, throwable);
    }

    /**
     * Logs an error message.
     *
     * @param category log category/source
     * @param message log message
     */
    public void error(String category, String message) {
        log(Logger.Level.ERROR, category, message, null, null);
    }

    /**
     * Logs a debug message with optional metadata.
     *
     * @param category log category/source
     * @param message log message
     * @param metadata additional key-value pairs
     */
    public void debug(String category, String message, Map<String, Object> metadata) {
        log(Logger.Level.DEBUG, category, message, metadata, null);
    }

    /**
     * Logs a debug message.
     *
     * @param category log category/source
     * @param message log message
     */
    public void debug(String category, String message) {
        log(Logger.Level.DEBUG, category, message, null, null);
    }

    /**
     * Logs a trace message.
     *
     * @param category log category/source
     * @param message log message
     */
    public void trace(String category, String message) {
        log(Logger.Level.TRACE, category, message, null, null);
    }

    /**
     * Logs an operation with timing.
     *
     * @param category log category/source
     * @param operation operation name
     * @param durationMs duration in milliseconds
     * @param success whether operation succeeded
     */
    public void logOperation(String category, String operation, long durationMs, boolean success) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("operation", operation);
        metadata.put("duration_ms", durationMs);
        metadata.put("success", success);

        Logger.Level level = Logger.Level.INFO;
        String message = String.format("Operation '%s' completed in %dms", operation, durationMs);

        // Check for slow operation
        if (loggingConfig.performanceLatencyLoggingEnabled() &&
            durationMs > loggingConfig.performanceSlowThresholdMs()) {
            level = Logger.Level.WARN;
            message = String.format("Slow operation '%s' took %dms (threshold: %dms)",
                operation, durationMs, loggingConfig.performanceSlowThresholdMs());
        }

        if (!success) {
            level = Logger.Level.ERROR;
            message = String.format("Operation '%s' failed after %dms", operation, durationMs);
        }

        log(level, category, message, metadata, null);
    }

    /**
     * Logs a database query with timing.
     *
     * @param query the SQL query (will be redacted)
     * @param durationMs execution time in milliseconds
     * @param rowCount number of rows affected/returned
     */
    public void logQuery(String query, long durationMs, int rowCount) {
        if (!loggingConfig.sqlEnabled()) {
            return;
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("duration_ms", durationMs);
        metadata.put("row_count", rowCount);

        // Truncate query if too long
        String truncatedQuery = query;
        int maxLength = loggingConfig.sqlMaxQueryLength();
        if (query != null && query.length() > maxLength) {
            truncatedQuery = query.substring(0, maxLength) + "... [truncated]";
        }

        // Redact sensitive values in query
        String redactedQuery = redactionService.redact(truncatedQuery);
        metadata.put("query", redactedQuery);

        Logger.Level level = Logger.Level.DEBUG;
        String message = String.format("Query executed in %dms, %d rows", durationMs, rowCount);

        // Check for slow query
        if (durationMs > loggingConfig.sqlSlowThresholdMs()) {
            level = Logger.Level.WARN;
            message = String.format("Slow query took %dms (threshold: %dms): %s",
                durationMs, loggingConfig.sqlSlowThresholdMs(),
                redactedQuery.substring(0, Math.min(100, redactedQuery.length())));
        }

        log(level, "SQL", message, metadata, null);
    }

    /**
     * Logs a security-related event.
     *
     * @param event security event type
     * @param user affected user
     * @param details event details
     * @param success whether the action succeeded
     */
    public void logSecurityEvent(String event, String user, String details, boolean success) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("security_event", event);
        metadata.put("user", user);
        metadata.put("details", redactionService.redact(details));
        metadata.put("success", success);

        Logger.Level level = success ? Logger.Level.INFO : Logger.Level.WARN;
        String message = String.format("Security event: %s for user '%s'", event, user);

        log(level, "SECURITY", message, metadata, null);
    }

    /**
     * Logs an audit event.
     *
     * @param action the action performed
     * @param resource the affected resource
     * @param user the user who performed the action
     * @param outcome the outcome (success/failure)
     */
    public void logAudit(String action, String resource, String user, String outcome) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("action", action);
        metadata.put("resource", resource);
        metadata.put("user", user);
        metadata.put("outcome", outcome);
        metadata.put("timestamp", Instant.now().toString());

        log(Logger.Level.INFO, "AUDIT", String.format("User '%s' performed '%s' on '%s': %s",
            user, action, resource, outcome), metadata, null);
    }

    /**
     * Creates a timing context for measuring operation duration.
     *
     * @param category log category
     * @param operation operation name
     * @return timing context
     */
    public TimingContext startTiming(String category, String operation) {
        return new TimingContext(this, category, operation);
    }

    /**
     * Core logging method.
     */
    private void log(Logger.Level level, String category, String message,
                     Map<String, Object> metadata, Throwable throwable) {

        // Redact message
        String redactedMessage = redactionService.redact(message);

        // Add standard context from MDC
        Map<String, Object> context = new HashMap<>();
        context.put("category", category);
        context.put("timestamp", Instant.now().toString());

        // Include MDC values
        Object correlationId = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID);
        if (correlationId != null) {
            context.put("correlationId", correlationId.toString());
        }

        Object user = MDC.get(CorrelationIdFilter.MDC_USER);
        if (user != null) {
            context.put("user", user.toString());
        }

        Object instance = MDC.get(CorrelationIdFilter.MDC_INSTANCE);
        if (instance != null) {
            context.put("instance", instance.toString());
        }

        // Merge provided metadata (redacting sensitive values)
        if (metadata != null) {
            for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof String) {
                    value = redactionService.redactValue(entry.getKey(), (String) value);
                }
                context.put(entry.getKey(), value);
            }
        }

        // Format message based on logging format
        String formattedMessage;
        if ("json".equalsIgnoreCase(loggingConfig.format())) {
            // In JSON mode, the JSON formatter will handle context
            formattedMessage = redactedMessage;
            // Add context to MDC for JSON formatter
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                MDC.put(entry.getKey(), entry.getValue());
            }
        } else {
            // In plain mode, append context to message
            formattedMessage = String.format("[%s] %s", category, redactedMessage);
        }

        // Log with appropriate level
        Logger categoryLogger = Logger.getLogger("pgconsole." + category);
        if (throwable != null) {
            categoryLogger.log(level, formattedMessage, throwable);
        } else {
            categoryLogger.log(level, formattedMessage);
        }

        // Clean up temporary MDC entries in JSON mode
        if ("json".equalsIgnoreCase(loggingConfig.format())) {
            for (String key : context.keySet()) {
                if (!key.equals("correlationId") && !key.equals("user") && !key.equals("instance")) {
                    MDC.remove(key);
                }
            }
        }
    }

    /**
     * Timing context for measuring operation duration.
     */
    public static class TimingContext implements AutoCloseable {
        private final StructuredLogger logger;
        private final String category;
        private final String operation;
        private final long startTime;
        private boolean success = true;

        TimingContext(StructuredLogger logger, String category, String operation) {
            this.logger = logger;
            this.category = category;
            this.operation = operation;
            this.startTime = System.currentTimeMillis();
        }

        /**
         * Marks the operation as failed.
         *
         * @return this context for chaining
         */
        public TimingContext failure() {
            this.success = false;
            return this;
        }

        /**
         * Gets elapsed time without closing.
         *
         * @return elapsed milliseconds
         */
        public long elapsed() {
            return System.currentTimeMillis() - startTime;
        }

        @Override
        public void close() {
            logger.logOperation(category, operation, elapsed(), success);
        }
    }
}
