package com.bovinemagnet.pgconsole.logging;

import com.bovinemagnet.pgconsole.config.LoggingConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for redacting sensitive data from log messages.
 * <p>
 * Automatically detects and masks passwords, secrets, tokens, API keys,
 * and other sensitive information to prevent accidental exposure in logs.
 * <p>
 * Supports:
 * <ul>
 *   <li>Pattern-based field name matching</li>
 *   <li>JDBC connection string sanitisation</li>
 *   <li>JSON value redaction</li>
 *   <li>Query parameter redaction</li>
 *   <li>Optional PII masking (email, phone, etc.)</li>
 * </ul>
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class LogRedactionService {

    // Patterns for various sensitive data formats
    private static final Pattern JDBC_PASSWORD_PATTERN =
        Pattern.compile("(password=)([^&;\\s]+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern JSON_SENSITIVE_PATTERN =
        Pattern.compile("\"(password|secret|token|key|credential|auth|apikey|api_key|bearer|jwt)\"\\s*:\\s*\"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern QUERY_PARAM_SENSITIVE_PATTERN =
        Pattern.compile("([?&])(password|secret|token|key|auth)=([^&\\s]+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern BEARER_TOKEN_PATTERN =
        Pattern.compile("(Bearer\\s+)([A-Za-z0-9\\-_.~+/]+=*)", Pattern.CASE_INSENSITIVE);

    private static final Pattern BASIC_AUTH_PATTERN =
        Pattern.compile("(Basic\\s+)([A-Za-z0-9+/]+=*)", Pattern.CASE_INSENSITIVE);

    // PII patterns
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");

    private static final Pattern PHONE_PATTERN =
        Pattern.compile("\\b(\\+?[0-9]{1,3}[-.\\s]?)?\\(?[0-9]{3}\\)?[-.\\s]?[0-9]{3}[-.\\s]?[0-9]{4}\\b");

    private static final Pattern SSN_PATTERN =
        Pattern.compile("\\b[0-9]{3}-[0-9]{2}-[0-9]{4}\\b");

    private static final Pattern CREDIT_CARD_PATTERN =
        Pattern.compile("\\b[0-9]{4}[-.\\s]?[0-9]{4}[-.\\s]?[0-9]{4}[-.\\s]?[0-9]{4}\\b");

    @Inject
    LoggingConfig loggingConfig;

    /**
     * Redacts sensitive data from the given message.
     *
     * @param message the message to redact
     * @return redacted message
     */
    public String redact(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        if (!loggingConfig.redactEnabled()) {
            return message;
        }

        String result = message;

        // Redact connection strings
        if (loggingConfig.redactConnectionStrings()) {
            result = redactConnectionStrings(result);
        }

        // Redact JSON values
        result = redactJsonValues(result);

        // Redact query parameters
        result = redactQueryParameters(result);

        // Redact auth tokens
        result = redactAuthTokens(result);

        // Redact based on custom patterns
        result = redactCustomPatterns(result);

        // Redact PII if enabled
        if (loggingConfig.redactMaskPii()) {
            result = redactPii(result);
        }

        return result;
    }

    /**
     * Redacts sensitive values from a key-value pair.
     *
     * @param key the key name
     * @param value the value to potentially redact
     * @return redacted value if key matches sensitive patterns, original otherwise
     */
    public String redactValue(String key, String value) {
        if (key == null || value == null) {
            return value;
        }

        if (!loggingConfig.redactEnabled()) {
            return value;
        }

        String lowerKey = key.toLowerCase();
        List<String> patterns = loggingConfig.redactPatterns();

        for (String pattern : patterns) {
            if (lowerKey.contains(pattern.toLowerCase())) {
                return loggingConfig.redactReplacement();
            }
        }

        return value;
    }

    /**
     * Checks if a key name matches sensitive patterns.
     *
     * @param key the key to check
     * @return true if sensitive
     */
    public boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }

        String lowerKey = key.toLowerCase();
        List<String> patterns = loggingConfig.redactPatterns();

        for (String pattern : patterns) {
            if (lowerKey.contains(pattern.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Redacts JDBC connection string passwords.
     */
    private String redactConnectionStrings(String message) {
        String replacement = loggingConfig.redactReplacement();
        return JDBC_PASSWORD_PATTERN.matcher(message).replaceAll("$1" + replacement);
    }

    /**
     * Redacts sensitive values in JSON format.
     */
    private String redactJsonValues(String message) {
        String replacement = loggingConfig.redactReplacement();
        Matcher matcher = JSON_SENSITIVE_PATTERN.matcher(message);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            matcher.appendReplacement(sb, "\"" + matcher.group(1) + "\": \"" + replacement + "\"");
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * Redacts sensitive query parameters.
     */
    private String redactQueryParameters(String message) {
        String replacement = loggingConfig.redactReplacement();
        return QUERY_PARAM_SENSITIVE_PATTERN.matcher(message).replaceAll("$1$2=" + replacement);
    }

    /**
     * Redacts Bearer and Basic authentication tokens.
     */
    private String redactAuthTokens(String message) {
        String replacement = loggingConfig.redactReplacement();
        String result = BEARER_TOKEN_PATTERN.matcher(message).replaceAll("$1" + replacement);
        return BASIC_AUTH_PATTERN.matcher(result).replaceAll("$1" + replacement);
    }

    /**
     * Redacts values matching custom sensitive patterns.
     */
    private String redactCustomPatterns(String message) {
        String replacement = loggingConfig.redactReplacement();
        List<String> patterns = loggingConfig.redactPatterns();
        String result = message;

        for (String pattern : patterns) {
            // Create pattern for key=value and key: value formats
            Pattern keyValuePattern = Pattern.compile(
                "(" + Pattern.quote(pattern) + "[\"']?\\s*[=:]\\s*[\"']?)([^\"'\\s,;}{\\]]+)",
                Pattern.CASE_INSENSITIVE
            );
            result = keyValuePattern.matcher(result).replaceAll("$1" + replacement);
        }

        return result;
    }

    /**
     * Redacts personally identifiable information (PII).
     */
    private String redactPii(String message) {
        String result = message;

        // Mask email addresses (keep domain)
        result = EMAIL_PATTERN.matcher(result).replaceAll("[email]@$0".split("@")[1]);

        // Mask phone numbers (keep last 4 digits)
        Matcher phoneMatcher = PHONE_PATTERN.matcher(result);
        StringBuffer sb = new StringBuffer();
        while (phoneMatcher.find()) {
            String phone = phoneMatcher.group();
            String masked = "***-***-" + phone.substring(Math.max(0, phone.length() - 4));
            phoneMatcher.appendReplacement(sb, masked);
        }
        phoneMatcher.appendTail(sb);
        result = sb.toString();

        // Fully mask SSN
        result = SSN_PATTERN.matcher(result).replaceAll("***-**-****");

        // Mask credit card numbers (keep last 4)
        Matcher ccMatcher = CREDIT_CARD_PATTERN.matcher(result);
        sb = new StringBuffer();
        while (ccMatcher.find()) {
            String cc = ccMatcher.group();
            String masked = "****-****-****-" + cc.substring(Math.max(0, cc.length() - 4));
            ccMatcher.appendReplacement(sb, masked);
        }
        ccMatcher.appendTail(sb);
        result = sb.toString();

        return result;
    }

    /**
     * Sanitises a JDBC URL for safe logging.
     *
     * @param jdbcUrl the JDBC URL to sanitise
     * @return sanitised URL with password removed
     */
    public String sanitiseJdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null) {
            return null;
        }
        return JDBC_PASSWORD_PATTERN.matcher(jdbcUrl).replaceAll("$1[REDACTED]");
    }

    /**
     * Creates a redacted copy of exception details.
     *
     * @param throwable the exception to redact
     * @return redacted exception message
     */
    public String redactException(Throwable throwable) {
        if (throwable == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(throwable.getClass().getName()).append(": ");
        sb.append(redact(throwable.getMessage()));

        // Optionally include redacted cause
        Throwable cause = throwable.getCause();
        if (cause != null && cause != throwable) {
            sb.append(" Caused by: ").append(redactException(cause));
        }

        return sb.toString();
    }
}
