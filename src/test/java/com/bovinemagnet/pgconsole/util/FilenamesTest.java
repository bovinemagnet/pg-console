package com.bovinemagnet.pgconsole.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FilenamesTest {

    @Test
    void stripsQuotes() {
        assertThat(Filenames.sanitize("slow-queries-foo\"bar.csv"))
                .isEqualTo("slow-queries-foo_bar.csv");
    }

    @Test
    void stripsCrlfToPreventHeaderSplitting() {
        assertThat(Filenames.sanitize("report-foo\r\nEvil: 1.csv"))
                .doesNotContain("\r")
                .doesNotContain("\n");
    }

    @Test
    void stripsSemicolonsWhichTerminateHeaderParams() {
        assertThat(Filenames.sanitize("name; evil=1.csv"))
                .doesNotContain(";");
    }

    @Test
    void preservesOrdinaryFilenames() {
        assertThat(Filenames.sanitize("slow-queries-default-20260421-143000.csv"))
                .isEqualTo("slow-queries-default-20260421-143000.csv");
    }

    @Test
    void nullOrEmptyReturnsDefault() {
        assertThat(Filenames.sanitize(null)).isEqualTo("download");
        assertThat(Filenames.sanitize("")).isEqualTo("download");
    }

    @Test
    void trimsOverlongInput() {
        String input = "a".repeat(500) + ".csv";
        assertThat(Filenames.sanitize(input).length()).isLessThanOrEqualTo(200);
    }
}
