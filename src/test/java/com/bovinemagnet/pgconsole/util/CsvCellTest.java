package com.bovinemagnet.pgconsole.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests CSV cell escaping, in particular neutralisation of spreadsheet formula
 * triggers (M06).
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@DisplayName("CsvCell escaping")
class CsvCellTest {

    @Test
    @DisplayName("A field starting with = is neutralised")
    void equalsPrefixNeutralised() {
        String out = CsvCell.escape("=HYPERLINK(\"http://evil\",\"x\")");
        assertThat(out).startsWith("\"'=");
        assertThat(out).doesNotStartWith("=");
    }

    @Test
    @DisplayName("Other formula-trigger prefixes are neutralised")
    void otherPrefixesNeutralised() {
        assertThat(CsvCell.escape("+1")).isEqualTo("'+1");
        assertThat(CsvCell.escape("-1")).isEqualTo("'-1");
        assertThat(CsvCell.escape("@cmd")).isEqualTo("'@cmd");
    }

    @Test
    @DisplayName("Ordinary values are unchanged")
    void ordinaryUnchanged() {
        assertThat(CsvCell.escape("myapp")).isEqualTo("myapp");
    }

    @Test
    @DisplayName("Comma/quote values are RFC-4180 quoted")
    void rfc4180Quoting() {
        assertThat(CsvCell.escape("a,b")).isEqualTo("\"a,b\"");
        assertThat(CsvCell.escape("a\"b")).isEqualTo("\"a\"\"b\"");
    }

    @Test
    @DisplayName("Null and empty become empty string")
    void nullEmpty() {
        assertThat(CsvCell.escape(null)).isEmpty();
        assertThat(CsvCell.escape("")).isEmpty();
    }
}
