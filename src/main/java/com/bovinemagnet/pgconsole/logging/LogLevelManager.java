package com.bovinemagnet.pgconsole.logging;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Service for managing log levels at runtime.
 * <p>
 * Provides:
 * <ul>
 *   <li>Runtime log level adjustment without restart</li>
 *   <li>Per-package/category log level configuration</li>
 *   <li>Temporary debug mode with auto-revert</li>
 *   <li>Log level presets (minimal, standard, verbose, debug)</li>
 * </ul>
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class LogLevelManager {

    private static final Logger LOG = Logger.getLogger(LogLevelManager.class);

    private final Map<String, Level> originalLevels = new ConcurrentHashMap<>();
    private final Map<String, Instant> temporaryLevelExpiry = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /**
     * Log level presets for common scenarios.
     */
    public enum LogPreset {
        /**
         * Minimal logging - errors and critical warnings only.
         */
        MINIMAL(Level.WARNING),

        /**
         * Standard logging - informational messages and above.
         */
        STANDARD(Level.INFO),

        /**
         * Verbose logging - includes debug information.
         */
        VERBOSE(Level.FINE),

        /**
         * Debug logging - all available detail.
         */
        DEBUG(Level.FINEST);

        private final Level level;

        LogPreset(Level level) {
            this.level = level;
        }

        public Level getLevel() {
            return level;
        }
    }

    /**
     * Gets the current log level for a logger.
     *
     * @param loggerName logger name/category
     * @return current level as string
     */
    public String getLogLevel(String loggerName) {
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(loggerName);
        Level level = logger.getLevel();

        // Walk up the hierarchy to find effective level
        if (level == null) {
            java.util.logging.Logger parent = logger.getParent();
            while (parent != null && level == null) {
                level = parent.getLevel();
                parent = parent.getParent();
            }
        }

        return level != null ? level.getName() : "INFO";
    }

    /**
     * Sets the log level for a logger.
     *
     * @param loggerName logger name/category
     * @param levelName new level (SEVERE, WARNING, INFO, FINE, FINER, FINEST, ALL, OFF)
     * @return true if level was set successfully
     */
    public boolean setLogLevel(String loggerName, String levelName) {
        try {
            Level level = parseLevel(levelName);
            java.util.logging.Logger logger = java.util.logging.Logger.getLogger(loggerName);

            // Store original level for potential revert
            if (!originalLevels.containsKey(loggerName)) {
                originalLevels.put(loggerName, logger.getLevel());
            }

            logger.setLevel(level);
            LOG.infof("Log level for '%s' set to %s", loggerName, level.getName());
            return true;
        } catch (Exception e) {
            LOG.errorf(e, "Failed to set log level for '%s' to '%s'", loggerName, levelName);
            return false;
        }
    }

    /**
     * Sets a temporary log level that auto-reverts after the specified duration.
     *
     * @param loggerName logger name/category
     * @param levelName temporary level
     * @param duration how long to maintain the temporary level
     * @return true if level was set successfully
     */
    public boolean setTemporaryLogLevel(String loggerName, String levelName, Duration duration) {
        if (!setLogLevel(loggerName, levelName)) {
            return false;
        }

        Instant expiry = Instant.now().plus(duration);
        temporaryLevelExpiry.put(loggerName, expiry);

        // Schedule revert
        scheduler.schedule(() -> revertLogLevel(loggerName), duration.toMillis(), TimeUnit.MILLISECONDS);

        LOG.infof("Temporary log level for '%s' set to %s, will revert in %s",
            loggerName, levelName, duration);
        return true;
    }

    /**
     * Reverts a logger to its original level.
     *
     * @param loggerName logger name/category
     */
    public void revertLogLevel(String loggerName) {
        Level originalLevel = originalLevels.remove(loggerName);
        temporaryLevelExpiry.remove(loggerName);

        if (originalLevel != null) {
            java.util.logging.Logger logger = java.util.logging.Logger.getLogger(loggerName);
            logger.setLevel(originalLevel);
            LOG.infof("Log level for '%s' reverted to %s", loggerName, originalLevel.getName());
        }
    }

    /**
     * Applies a log level preset to the root logger.
     *
     * @param preset the preset to apply
     */
    public void applyPreset(LogPreset preset) {
        setLogLevel("", preset.getLevel().getName());
        LOG.infof("Applied logging preset: %s", preset.name());
    }

    /**
     * Applies a log level preset to a specific logger.
     *
     * @param loggerName logger name/category
     * @param preset the preset to apply
     */
    public void applyPreset(String loggerName, LogPreset preset) {
        setLogLevel(loggerName, preset.getLevel().getName());
    }

    /**
     * Enables debug mode for all pg-console loggers temporarily.
     *
     * @param duration how long to maintain debug mode
     */
    public void enableDebugMode(Duration duration) {
        setTemporaryLogLevel("com.bovinemagnet.pgconsole", "FINEST", duration);
        setTemporaryLogLevel("pgconsole", "FINEST", duration);
        LOG.infof("Debug mode enabled for %s", duration);
    }

    /**
     * Disables debug mode and reverts to normal levels.
     */
    public void disableDebugMode() {
        revertLogLevel("com.bovinemagnet.pgconsole");
        revertLogLevel("pgconsole");
        LOG.info("Debug mode disabled");
    }

    /**
     * Gets the current log configuration summary.
     *
     * @return map of logger names to their current levels
     */
    public Map<String, String> getLogConfiguration() {
        Map<String, String> config = new ConcurrentHashMap<>();

        // Add root logger
        config.put("ROOT", getLogLevel(""));

        // Add known pg-console loggers
        config.put("com.bovinemagnet.pgconsole", getLogLevel("com.bovinemagnet.pgconsole"));
        config.put("pgconsole", getLogLevel("pgconsole"));
        config.put("pgconsole.SQL", getLogLevel("pgconsole.SQL"));
        config.put("pgconsole.SECURITY", getLogLevel("pgconsole.SECURITY"));
        config.put("pgconsole.AUDIT", getLogLevel("pgconsole.AUDIT"));

        // Add any custom levels that have been set
        for (String loggerName : originalLevels.keySet()) {
            config.put(loggerName, getLogLevel(loggerName));
        }

        return config;
    }

    /**
     * Gets temporary level expiry information.
     *
     * @return map of logger names to their expiry times
     */
    public Map<String, Instant> getTemporaryLevelExpiry() {
        return Map.copyOf(temporaryLevelExpiry);
    }

    /**
     * Parses a level name string to a Level object.
     */
    private Level parseLevel(String levelName) {
        if (levelName == null || levelName.isBlank()) {
            return Level.INFO;
        }

        return switch (levelName.toUpperCase()) {
            case "TRACE", "FINEST" -> Level.FINEST;
            case "DEBUG", "FINER" -> Level.FINER;
            case "FINE" -> Level.FINE;
            case "INFO" -> Level.INFO;
            case "WARN", "WARNING" -> Level.WARNING;
            case "ERROR", "SEVERE" -> Level.SEVERE;
            case "OFF" -> Level.OFF;
            case "ALL" -> Level.ALL;
            default -> Level.parse(levelName);
        };
    }
}
