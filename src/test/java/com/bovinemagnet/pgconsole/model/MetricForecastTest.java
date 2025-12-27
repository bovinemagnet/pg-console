package com.bovinemagnet.pgconsole.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MetricForecast model class.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
class MetricForecastTest {

    @Test
    void testModelTypeEnumProperties() {
        MetricForecast.ModelType linear = MetricForecast.ModelType.LINEAR;
        assertEquals("Linear Regression", linear.getDisplayName());
        assertNotNull(linear.getDescription());

        MetricForecast.ModelType exponential = MetricForecast.ModelType.EXPONENTIAL;
        assertEquals("Exponential Growth", exponential.getDisplayName());

        MetricForecast.ModelType seasonal = MetricForecast.ModelType.SEASONAL;
        assertEquals("Seasonal", seasonal.getDisplayName());
    }

    @Test
    void testGetConfidenceIntervalWidth() {
        MetricForecast forecast = new MetricForecast();
        forecast.setConfidenceLower(90.0);
        forecast.setConfidenceUpper(110.0);

        assertEquals(20.0, forecast.getConfidenceIntervalWidth(), 0.001);
    }

    @Test
    void testGetConfidenceIntervalWidthNullLower() {
        MetricForecast forecast = new MetricForecast();
        forecast.setConfidenceLower(null);
        forecast.setConfidenceUpper(110.0);

        assertNull(forecast.getConfidenceIntervalWidth());
    }

    @Test
    void testGetConfidenceIntervalWidthNullUpper() {
        MetricForecast forecast = new MetricForecast();
        forecast.setConfidenceLower(90.0);
        forecast.setConfidenceUpper(null);

        assertNull(forecast.getConfidenceIntervalWidth());
    }

    @ParameterizedTest
    @CsvSource({
            "0.95, true",
            "0.90, true",
            "0.70, true",
            "0.69, false",
            "0.50, false",
            "0.10, false"
    })
    void testIsHighQuality(double rSquared, boolean expected) {
        MetricForecast forecast = new MetricForecast();
        forecast.setRSquared(rSquared);

        assertEquals(expected, forecast.isHighQuality());
    }

    @Test
    void testIsHighQualityNull() {
        MetricForecast forecast = new MetricForecast();
        forecast.setRSquared(null);

        assertFalse(forecast.isHighQuality());
    }

    @ParameterizedTest
    @CsvSource({
            "0.95, Excellent",
            "0.90, Excellent",
            "0.85, Good",
            "0.70, Good",
            "0.60, Fair",
            "0.50, Fair",
            "0.40, Poor",
            "0.10, Poor"
    })
    void testGetQualityDescription(double rSquared, String expectedDescription) {
        MetricForecast forecast = new MetricForecast();
        forecast.setRSquared(rSquared);

        assertEquals(expectedDescription, forecast.getQualityDescription());
    }

    @Test
    void testGetQualityDescriptionNull() {
        MetricForecast forecast = new MetricForecast();
        forecast.setRSquared(null);

        assertEquals("Unknown", forecast.getQualityDescription());
    }

    @ParameterizedTest
    @CsvSource({
            "0.90, text-success",
            "0.70, text-success",
            "0.60, text-warning",
            "0.50, text-warning",
            "0.40, text-danger",
            "0.10, text-danger"
    })
    void testGetQualityCssClass(double rSquared, String expectedClass) {
        MetricForecast forecast = new MetricForecast();
        forecast.setRSquared(rSquared);

        assertEquals(expectedClass, forecast.getQualityCssClass());
    }

    @Test
    void testGetQualityCssClassNull() {
        MetricForecast forecast = new MetricForecast();
        forecast.setRSquared(null);

        assertEquals("text-secondary", forecast.getQualityCssClass());
    }

    @Test
    void testGetFormattedValueForSize() {
        MetricForecast forecast = new MetricForecast();
        forecast.setMetricName("total_database_size_bytes");
        forecast.setForecastValue(1024.0 * 1024 * 1024);  // 1 GB

        String formatted = forecast.getFormattedValue();
        assertTrue(formatted.contains("1.0") || formatted.contains("1,0"));
        assertTrue(formatted.contains("GB"));
    }

    @Test
    void testGetFormattedValueForRatio() {
        MetricForecast forecast = new MetricForecast();
        forecast.setMetricName("cache_hit_ratio");
        forecast.setForecastValue(0.985);

        String formatted = forecast.getFormattedValue();
        assertTrue(formatted.contains("98.5"));
        assertTrue(formatted.contains("%"));
    }

    @Test
    void testGetFormattedValueForGeneric() {
        MetricForecast forecast = new MetricForecast();
        forecast.setMetricName("total_connections");
        forecast.setForecastValue(123.45);

        String formatted = forecast.getFormattedValue();
        assertTrue(formatted.contains("123.45"));
    }

    @Test
    void testGetFormattedConfidenceIntervalForSize() {
        MetricForecast forecast = new MetricForecast();
        forecast.setMetricName("database_size_bytes");
        forecast.setConfidenceLower(900.0 * 1024 * 1024);  // 900 MB
        forecast.setConfidenceUpper(1100.0 * 1024 * 1024);  // 1100 MB

        String formatted = forecast.getFormattedConfidenceInterval();
        assertTrue(formatted.contains("MB") || formatted.contains("GB"));
        assertTrue(formatted.contains("-"));
    }

    @Test
    void testGetFormattedConfidenceIntervalForGeneric() {
        MetricForecast forecast = new MetricForecast();
        forecast.setMetricName("total_connections");
        forecast.setConfidenceLower(100.0);
        forecast.setConfidenceUpper(200.0);

        String formatted = forecast.getFormattedConfidenceInterval();
        assertEquals("100.00 - 200.00", formatted);
    }

    @Test
    void testGetFormattedConfidenceIntervalNull() {
        MetricForecast forecast = new MetricForecast();
        forecast.setConfidenceLower(null);
        forecast.setConfidenceUpper(null);

        assertEquals("N/A", forecast.getFormattedConfidenceInterval());
    }

    @Test
    void testFormatBytesTB() {
        MetricForecast forecast = new MetricForecast();
        forecast.setMetricName("size");
        forecast.setForecastValue(2.0 * 1024 * 1024 * 1024 * 1024);

        String formatted = forecast.getFormattedValue();
        assertTrue(formatted.contains("TB"));
    }

    @Test
    void testFormatBytesGB() {
        MetricForecast forecast = new MetricForecast();
        forecast.setMetricName("size");
        forecast.setForecastValue(5.0 * 1024 * 1024 * 1024);

        String formatted = forecast.getFormattedValue();
        assertTrue(formatted.contains("GB"));
    }

    @Test
    void testFormatBytesMB() {
        MetricForecast forecast = new MetricForecast();
        forecast.setMetricName("size");
        forecast.setForecastValue(100.0 * 1024 * 1024);

        String formatted = forecast.getFormattedValue();
        assertTrue(formatted.contains("MB"));
    }

    @Test
    void testFormatBytesKB() {
        MetricForecast forecast = new MetricForecast();
        forecast.setMetricName("size");
        forecast.setForecastValue(50.0 * 1024);

        String formatted = forecast.getFormattedValue();
        assertTrue(formatted.contains("KB"));
    }

    @Test
    void testFormatBytesSmall() {
        MetricForecast forecast = new MetricForecast();
        forecast.setMetricName("size");
        forecast.setForecastValue(500.0);

        String formatted = forecast.getFormattedValue();
        assertTrue(formatted.contains("B"));
    }

    @Test
    void testSettersAndGetters() {
        MetricForecast forecast = new MetricForecast();

        forecast.setId(123L);
        forecast.setInstanceId("test-instance");
        forecast.setMetricName("total_connections");
        forecast.setMetricCategory("system");
        forecast.setForecastDate(LocalDate.of(2025, 1, 15));
        forecast.setForecastValue(150.0);
        forecast.setConfidenceLower(140.0);
        forecast.setConfidenceUpper(160.0);
        forecast.setConfidenceLevel(0.95);
        forecast.setModelType(MetricForecast.ModelType.LINEAR);
        forecast.setRSquared(0.85);
        forecast.setDataPointsUsed(30);
        forecast.setTrainingPeriodDays(7);

        Instant now = Instant.now();
        forecast.setCalculatedAt(now);

        assertEquals(123L, forecast.getId());
        assertEquals("test-instance", forecast.getInstanceId());
        assertEquals("total_connections", forecast.getMetricName());
        assertEquals("system", forecast.getMetricCategory());
        assertEquals(LocalDate.of(2025, 1, 15), forecast.getForecastDate());
        assertEquals(150.0, forecast.getForecastValue());
        assertEquals(140.0, forecast.getConfidenceLower());
        assertEquals(160.0, forecast.getConfidenceUpper());
        assertEquals(0.95, forecast.getConfidenceLevel());
        assertEquals(MetricForecast.ModelType.LINEAR, forecast.getModelType());
        assertEquals(0.85, forecast.getRSquared());
        assertEquals(30, forecast.getDataPointsUsed());
        assertEquals(7, forecast.getTrainingPeriodDays());
        assertEquals(now, forecast.getCalculatedAt());
    }

    @Test
    void testDefaultConstructor() {
        MetricForecast forecast = new MetricForecast();

        assertNull(forecast.getId());
        assertNull(forecast.getInstanceId());
        assertNull(forecast.getMetricName());
        assertEquals(0.0, forecast.getForecastValue());
        assertNull(forecast.getRSquared());
    }
}
