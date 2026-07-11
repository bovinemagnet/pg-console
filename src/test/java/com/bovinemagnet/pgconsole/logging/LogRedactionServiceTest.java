package com.bovinemagnet.pgconsole.logging;

import com.bovinemagnet.pgconsole.config.LoggingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests log redaction: JDBC URL sanitisation (M02), userinfo credentials (M05)
 * and email PII masking (M04).
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@DisplayName("LogRedactionService")
class LogRedactionServiceTest {

    private LogRedactionService service;

    @BeforeEach
    void setUp() {
        service = new LogRedactionService();
        LoggingConfig config = mock(LoggingConfig.class);
        when(config.redactEnabled()).thenReturn(true);
        when(config.redactConnectionStrings()).thenReturn(true);
        when(config.redactMaskPii()).thenReturn(true);
        when(config.redactReplacement()).thenReturn("[REDACTED]");
        when(config.redactPatterns()).thenReturn(List.of());
        service.loggingConfig = config;
    }

    @Test
    @DisplayName("sanitiseJdbcUrl masks a password= parameter")
    void sanitiseMasksPasswordParam() {
        String out = LogRedactionService.sanitiseJdbcUrl(
            "jdbc:postgresql://host/db?user=x&password=secret");
        assertThat(out).doesNotContain("secret");
        assertThat(out).contains("password=[REDACTED]");
    }

    @Test
    @DisplayName("sanitiseJdbcUrl masks userinfo credentials, keeping the username")
    void sanitiseMasksUserinfo() {
        String out = LogRedactionService.sanitiseJdbcUrl(
            "jdbc:postgresql://admin:secret@host/db");
        assertThat(out).doesNotContain("secret");
        assertThat(out).contains("admin:[REDACTED]@");
    }

    @Test
    @DisplayName("redact masks a userinfo-style connection URL (M05)")
    void redactMasksUserinfoUrl() {
        String out = service.redact("connecting to postgresql://admin:secret@db:5432/app");
        assertThat(out).doesNotContain("secret");
        assertThat(out).contains("admin:[REDACTED]@");
    }

    @Test
    @DisplayName("redact actually masks an email when mask-pii is on (M04)")
    void redactMasksEmail() {
        String out = service.redact("user alice@example.com logged in");
        assertThat(out).doesNotContain("alice@example.com");
        assertThat(out).contains("[email]@example.com");
    }
}
