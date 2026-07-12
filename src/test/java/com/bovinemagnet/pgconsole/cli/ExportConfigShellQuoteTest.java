package com.bovinemagnet.pgconsole.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the env-export shell quoting renders command-substitution and other
 * shell metacharacters inert when the generated file is sourced (M36).
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@DisplayName("ExportConfigCommand shell quoting")
class ExportConfigShellQuoteTest {

    @Test
    @DisplayName("Command substitution is inert inside single quotes")
    void commandSubstitutionInert() {
        String out = ExportConfigCommand.shellSingleQuote("$(rm -rf /)");
        assertThat(out).isEqualTo("'$(rm -rf /)'");
    }

    @Test
    @DisplayName("Backticks and semicolons are inert")
    void backticksAndSemicolons() {
        assertThat(ExportConfigCommand.shellSingleQuote("a`whoami`;b"))
            .isEqualTo("'a`whoami`;b'");
    }

    @Test
    @DisplayName("Embedded single quote is escaped with the '\\'' idiom")
    void embeddedSingleQuoteEscaped() {
        assertThat(ExportConfigCommand.shellSingleQuote("it's"))
            .isEqualTo("'it'\\''s'");
    }

    @Test
    @DisplayName("Null becomes an empty quoted string")
    void nullBecomesEmpty() {
        assertThat(ExportConfigCommand.shellSingleQuote(null)).isEqualTo("''");
    }
}
