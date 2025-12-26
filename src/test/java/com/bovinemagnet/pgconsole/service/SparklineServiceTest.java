package com.bovinemagnet.pgconsole.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SparklineService SVG generation.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
class SparklineServiceTest {

    private SparklineService sparklineService;

    @BeforeEach
    void setUp() {
        sparklineService = new SparklineService();
    }

    @Test
    void testGenerateSparklineWithValidData() {
        List<Double> values = Arrays.asList(10.0, 20.0, 15.0, 30.0, 25.0);
        String svg = sparklineService.generateSparkline(values, 120, 24);

        assertNotNull(svg);
        assertTrue(svg.startsWith("<svg"));
        assertTrue(svg.endsWith("</svg>"));
        assertTrue(svg.contains("width=\"120\""));
        assertTrue(svg.contains("height=\"24\""));
        assertTrue(svg.contains("<path"));
        assertTrue(svg.contains("<circle"));
        assertTrue(svg.contains(SparklineService.COLOUR_PRIMARY));
    }

    @Test
    void testGenerateSparklineWithCustomColour() {
        List<Double> values = Arrays.asList(10.0, 20.0, 30.0);
        String svg = sparklineService.generateSparkline(values, 100, 20, SparklineService.COLOUR_DANGER);

        assertNotNull(svg);
        assertTrue(svg.contains(SparklineService.COLOUR_DANGER));
    }

    @Test
    void testGenerateSparklineWithNullValues() {
        String svg = sparklineService.generateSparkline(null, 120, 24);

        assertNotNull(svg);
        assertTrue(svg.contains("sparkline-empty"));
        assertTrue(svg.contains("stroke-dasharray"));
    }

    @Test
    void testGenerateSparklineWithEmptyList() {
        String svg = sparklineService.generateSparkline(Collections.emptyList(), 120, 24);

        assertNotNull(svg);
        assertTrue(svg.contains("sparkline-empty"));
    }

    @Test
    void testGenerateSparklineWithSingleValue() {
        List<Double> values = Collections.singletonList(50.0);
        String svg = sparklineService.generateSparkline(values, 120, 24);

        assertNotNull(svg);
        assertTrue(svg.contains("sparkline-empty"));
    }

    @Test
    void testGenerateSparklineWithConstantValues() {
        List<Double> values = Arrays.asList(50.0, 50.0, 50.0, 50.0);
        String svg = sparklineService.generateSparkline(values, 120, 24);

        assertNotNull(svg);
        assertTrue(svg.contains("<path"));
        assertFalse(svg.contains("sparkline-empty"));
    }

    @Test
    void testGenerateAreaSparklineWithValidData() {
        List<Double> values = Arrays.asList(5.0, 10.0, 8.0, 15.0, 12.0);
        String svg = sparklineService.generateAreaSparkline(values, 150, 30, SparklineService.COLOUR_SUCCESS);

        assertNotNull(svg);
        assertTrue(svg.startsWith("<svg"));
        assertTrue(svg.endsWith("</svg>"));
        assertTrue(svg.contains("width=\"150\""));
        assertTrue(svg.contains("height=\"30\""));
        // Should have two paths: one for the fill area and one for the line
        assertTrue(svg.contains("fill-opacity=\"0.2\""));
        assertTrue(svg.contains("fill=\"none\""));
        assertTrue(svg.contains(SparklineService.COLOUR_SUCCESS));
    }

    @Test
    void testGenerateAreaSparklineWithNullValues() {
        String svg = sparklineService.generateAreaSparkline(null, 120, 24, SparklineService.COLOUR_PRIMARY);

        assertNotNull(svg);
        assertTrue(svg.contains("sparkline-empty"));
    }

    @Test
    void testGenerateEmptySparkline() {
        String svg = sparklineService.generateEmptySparkline(100, 20);

        assertNotNull(svg);
        assertTrue(svg.contains("width=\"100\""));
        assertTrue(svg.contains("height=\"20\""));
        assertTrue(svg.contains("sparkline-empty"));
        assertTrue(svg.contains("<line"));
        assertTrue(svg.contains("stroke-dasharray=\"2,2\""));
    }

    @Test
    void testSparklineContainsValidSvgNamespace() {
        List<Double> values = Arrays.asList(1.0, 2.0, 3.0);
        String svg = sparklineService.generateSparkline(values, 100, 20);

        assertTrue(svg.contains("xmlns=\"http://www.w3.org/2000/svg\""));
    }

    @Test
    void testSparklinePathStartsWithM() {
        List<Double> values = Arrays.asList(10.0, 20.0, 30.0);
        String svg = sparklineService.generateSparkline(values, 100, 20);

        // The path should start with "M" (moveto command)
        assertTrue(svg.contains("d=\"M"));
    }

    @Test
    void testSparklineCircleAtEndOfLine() {
        List<Double> values = Arrays.asList(10.0, 20.0, 30.0);
        String svg = sparklineService.generateSparkline(values, 100, 20);

        // Should have a circle element for the current value indicator
        assertTrue(svg.contains("<circle"));
        assertTrue(svg.contains("r=\"2\""));
    }

    @Test
    void testSparklineStrokeProperties() {
        List<Double> values = Arrays.asList(10.0, 20.0);
        String svg = sparklineService.generateSparkline(values, 100, 20);

        assertTrue(svg.contains("stroke-width=\"1.5\""));
        assertTrue(svg.contains("stroke-linecap=\"round\""));
        assertTrue(svg.contains("stroke-linejoin=\"round\""));
    }

    @Test
    void testColourConstants() {
        assertEquals("#0d6efd", SparklineService.COLOUR_PRIMARY);
        assertEquals("#198754", SparklineService.COLOUR_SUCCESS);
        assertEquals("#ffc107", SparklineService.COLOUR_WARNING);
        assertEquals("#dc3545", SparklineService.COLOUR_DANGER);
        assertEquals("#0dcaf0", SparklineService.COLOUR_INFO);
    }

    @Test
    void testSparklineWithNegativeValues() {
        List<Double> values = Arrays.asList(-10.0, -5.0, 0.0, 5.0, 10.0);
        String svg = sparklineService.generateSparkline(values, 100, 20);

        assertNotNull(svg);
        assertTrue(svg.contains("<path"));
        assertFalse(svg.contains("sparkline-empty"));
    }

    @Test
    void testSparklineWithLargeValues() {
        List<Double> values = Arrays.asList(1000000.0, 2000000.0, 1500000.0);
        String svg = sparklineService.generateSparkline(values, 100, 20);

        assertNotNull(svg);
        assertTrue(svg.contains("<path"));
    }

    @Test
    void testSparklineWithVerySmallValues() {
        List<Double> values = Arrays.asList(0.001, 0.002, 0.0015);
        String svg = sparklineService.generateSparkline(values, 100, 20);

        assertNotNull(svg);
        assertTrue(svg.contains("<path"));
    }

    @Test
    void testSparklineClassAttribute() {
        List<Double> values = Arrays.asList(1.0, 2.0, 3.0);
        String svg = sparklineService.generateSparkline(values, 100, 20);

        assertTrue(svg.contains("class=\"sparkline\""));
    }

    @Test
    void testEmptySparklineClassAttribute() {
        String svg = sparklineService.generateEmptySparkline(100, 20);

        assertTrue(svg.contains("class=\"sparkline sparkline-empty\""));
    }
}
