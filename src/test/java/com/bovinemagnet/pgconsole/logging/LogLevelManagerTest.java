package com.bovinemagnet.pgconsole.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that setting a log level on a logger that inherits its level (the
 * common case, getLevel()==null) succeeds instead of silently failing on a
 * ConcurrentHashMap null-value NPE (M34).
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@DisplayName("LogLevelManager inheriting-logger handling")
class LogLevelManagerTest {

    private final LogLevelManager manager = new LogLevelManager();

    @Test
    @DisplayName("Setting a level on an inheriting logger succeeds")
    void setLevelOnInheritingLogger() {
        String name = "com.bovinemagnet.pgconsole.test.m34.inheriting";
        Logger logger = Logger.getLogger(name);
        // Precondition: this logger inherits its level.
        assertThat(logger.getLevel()).isNull();

        boolean ok = manager.setLogLevel(name, "FINE");

        assertThat(ok).isTrue();
        assertThat(logger.getLevel()).isEqualTo(Level.FINE);
    }

    @Test
    @DisplayName("Reverting an inheriting logger restores inheritance")
    void revertRestoresInheritance() {
        String name = "com.bovinemagnet.pgconsole.test.m34.revert";
        Logger logger = Logger.getLogger(name);
        assertThat(logger.getLevel()).isNull();

        assertThat(manager.setLogLevel(name, "FINE")).isTrue();
        assertThat(logger.getLevel()).isEqualTo(Level.FINE);

        manager.revertLogLevel(name);

        // Back to inheriting (no explicit level).
        assertThat(logger.getLevel()).isNull();
    }
}
