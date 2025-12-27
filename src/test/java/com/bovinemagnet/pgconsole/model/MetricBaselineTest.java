package com.bovinemagnet.pgconsole.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MetricBaseline model class.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
class MetricBaselineTest {

    @Test
    void testCalculateSigmaPositive() {
        MetricBaseline baseline = new MetricBaseline("instance", "metric", MetricBaseline.Category.SYSTEM);
        baseline.setMean(100.0);
        baseline.setStddev(10.0);

        // Value 130 is 3 sigma above mean
        assertEquals(3.0, baseline.calculateSigma(130.0), 0.001);
    }

    @Test
    void testCalculateSigmaNegative() {
        MetricBaseline baseline = new MetricBaseline("instance", "metric", MetricBaseline.Category.SYSTEM);
        baseline.setMean(100.0);
        baseline.setStddev(10.0);

        // Value 80 is 2 sigma below mean
        assertEquals(-2.0, baseline.calculateSigma(80.0), 0.001);
    }

    @Test
    void testCalculateSigmaZeroStddev() {
        MetricBaseline baseline = new MetricBaseline("instance", "metric", MetricBaseline.Category.SYSTEM);
        baseline.setMean(100.0);
        baseline.setStddev(0.0);

        // Should return 0 when stddev is 0 to avoid division by zero
        assertEquals(0.0, baseline.calculateSigma(150.0), 0.001);
    }

    @Test
    void testCalculateSigmaAtMean() {
        MetricBaseline baseline = new MetricBaseline("instance", "metric", MetricBaseline.Category.SYSTEM);
        baseline.setMean(100.0);
        baseline.setStddev(10.0);

        // Value at mean should have sigma of 0
        assertEquals(0.0, baseline.calculateSigma(100.0), 0.001);
    }

    @ParameterizedTest
    @CsvSource({
            "130.0, 10.0, 2.0, true",   // 3 sigma with threshold 2
            "115.0, 10.0, 2.0, false",  // 1.5 sigma with threshold 2
            "70.0, 10.0, 2.0, true",    // -3 sigma with threshold 2
            "85.0, 10.0, 2.0, false",   // -1.5 sigma with threshold 2
            "100.0, 10.0, 2.0, false"   // 0 sigma with threshold 2
    })
    void testIsAnomaly(double value, double stddev, double threshold, boolean expectedAnomaly) {
        MetricBaseline baseline = new MetricBaseline("instance", "metric", MetricBaseline.Category.SYSTEM);
        baseline.setMean(100.0);
        baseline.setStddev(stddev);

        assertEquals(expectedAnomaly, baseline.isAnomaly(value, threshold));
    }

    @Test
    void testGetTimeContextDisplayOverall() {
        MetricBaseline baseline = new MetricBaseline();
        baseline.setHourOfDay(null);
        baseline.setDayOfWeek(null);

        assertEquals("Overall", baseline.getTimeContextDisplay());
    }

    @Test
    void testGetTimeContextDisplayHourOnly() {
        MetricBaseline baseline = new MetricBaseline();
        baseline.setHourOfDay(14);
        baseline.setDayOfWeek(null);

        String display = baseline.getTimeContextDisplay();
        assertTrue(display.contains("14:00"));
    }

    @Test
    void testGetTimeContextDisplayDayOnly() {
        MetricBaseline baseline = new MetricBaseline();
        baseline.setHourOfDay(null);
        baseline.setDayOfWeek(1);  // Monday

        assertEquals("Monday", baseline.getTimeContextDisplay());
    }

    @Test
    void testGetTimeContextDisplayBoth() {
        MetricBaseline baseline = new MetricBaseline();
        baseline.setHourOfDay(9);
        baseline.setDayOfWeek(5);  // Friday

        String display = baseline.getTimeContextDisplay();
        assertTrue(display.contains("Friday"));
        assertTrue(display.contains("09:00"));
    }

    @Test
    void testGetTimeContextDisplaySunday() {
        MetricBaseline baseline = new MetricBaseline();
        baseline.setDayOfWeek(0);  // Sunday

        assertEquals("Sunday", baseline.getTimeContextDisplay());
    }

    @Test
    void testCategoryEnumProperties() {
        MetricBaseline.Category system = MetricBaseline.Category.SYSTEM;
        assertEquals("System", system.getDisplayName());
        assertNotNull(system.getDescription());

        MetricBaseline.Category query = MetricBaseline.Category.QUERY;
        assertEquals("Query", query.getDisplayName());

        MetricBaseline.Category database = MetricBaseline.Category.DATABASE;
        assertEquals("Database", database.getDisplayName());
    }

    @Test
    void testConstructorWithParameters() {
        MetricBaseline baseline = new MetricBaseline("test-instance", "test-metric",
                MetricBaseline.Category.QUERY);

        assertEquals("test-instance", baseline.getInstanceId());
        assertEquals("test-metric", baseline.getMetricName());
        assertEquals(MetricBaseline.Category.QUERY, baseline.getCategory());
    }

    @Test
    void testSettersAndGetters() {
        MetricBaseline baseline = new MetricBaseline();

        baseline.setId(123L);
        baseline.setInstanceId("instance1");
        baseline.setMetricName("cpu_usage");
        baseline.setCategory(MetricBaseline.Category.SYSTEM);
        baseline.setMean(50.0);
        baseline.setStddev(5.0);
        baseline.setMin(10.0);
        baseline.setMax(90.0);
        baseline.setMedian(48.0);
        baseline.setP95(85.0);
        baseline.setP99(88.0);
        baseline.setSampleCount(1000);
        baseline.setHourOfDay(10);
        baseline.setDayOfWeek(3);

        Instant now = Instant.now();
        baseline.setCalculatedAt(now);
        baseline.setPeriodStart(now.minusSeconds(86400));
        baseline.setPeriodEnd(now);

        assertEquals(123L, baseline.getId());
        assertEquals("instance1", baseline.getInstanceId());
        assertEquals("cpu_usage", baseline.getMetricName());
        assertEquals(MetricBaseline.Category.SYSTEM, baseline.getCategory());
        assertEquals(50.0, baseline.getMean());
        assertEquals(5.0, baseline.getStddev());
        assertEquals(10.0, baseline.getMin());
        assertEquals(90.0, baseline.getMax());
        assertEquals(48.0, baseline.getMedian());
        assertEquals(85.0, baseline.getP95());
        assertEquals(88.0, baseline.getP99());
        assertEquals(1000, baseline.getSampleCount());
        assertEquals(10, baseline.getHourOfDay());
        assertEquals(3, baseline.getDayOfWeek());
        assertEquals(now, baseline.getCalculatedAt());
        assertNotNull(baseline.getPeriodStart());
        assertNotNull(baseline.getPeriodEnd());
    }

    @Test
    void testDefaultConstructor() {
        MetricBaseline baseline = new MetricBaseline();
        assertNull(baseline.getId());
        assertNull(baseline.getInstanceId());
        assertNull(baseline.getMetricName());
        assertNull(baseline.getCategory());
        assertEquals(0.0, baseline.getMean());
        assertEquals(0.0, baseline.getStddev());
    }

    @Test
    void testPercentileValues() {
        MetricBaseline baseline = new MetricBaseline();

        // Test nullable Double fields
        assertNull(baseline.getMin());
        assertNull(baseline.getMax());
        assertNull(baseline.getMedian());
        assertNull(baseline.getP95());
        assertNull(baseline.getP99());

        baseline.setMin(1.0);
        baseline.setMax(100.0);
        baseline.setMedian(50.0);
        baseline.setP95(95.0);
        baseline.setP99(99.0);

        assertEquals(1.0, baseline.getMin());
        assertEquals(100.0, baseline.getMax());
        assertEquals(50.0, baseline.getMedian());
        assertEquals(95.0, baseline.getP95());
        assertEquals(99.0, baseline.getP99());
    }
}
