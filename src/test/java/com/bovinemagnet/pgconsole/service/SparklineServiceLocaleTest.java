package com.bovinemagnet.pgconsole.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies sparkline SVG numbers use a dot decimal regardless of the JVM
 * default locale, so a comma-decimal locale does not produce malformed SVG
 * (M19).
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@DisplayName("SparklineService locale independence")
class SparklineServiceLocaleTest {

    private Locale original;
    private final SparklineService service = new SparklineService();

    @BeforeEach
    void setUp() {
        original = Locale.getDefault();
        Locale.setDefault(Locale.GERMANY); // comma decimal separator
    }

    @AfterEach
    void tearDown() {
        Locale.setDefault(original);
    }

    @Test
    @DisplayName("Line sparkline coordinates use a dot decimal, not a comma")
    void lineSparklineUsesDotDecimal() {
        String svg = service.generateSparkline(List.of(10.0, 20.5, 15.25, 30.0), 120, 24);

        // The circle marker uses %.2f; under de-DE an unlocalised format would
        // emit cx="3,50" and break the SVG.
        assertThat(svg).contains("<svg");
        assertThat(svg).doesNotContain(",\" cy");
        assertThat(svg).doesNotContain(",\" r=");
    }

    @Test
    @DisplayName("Area sparkline path uses dot decimals")
    void areaSparklineUsesDotDecimal() {
        String svg = service.generateAreaSparkline(List.of(1.5, 2.5, 3.5), 120, 24, "#000");

        // A malformed path would contain "M 0,00" style comma decimals.
        assertThat(svg).doesNotContain(",00");
        assertThat(svg).doesNotContain(",50");
    }
}
