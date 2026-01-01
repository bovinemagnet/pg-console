package com.bovinemagnet.pgconsole.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DetectedAnomaly model class.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
class DetectedAnomalyTest {

    @Test
    void testSeverityFromSigmaCritical() {
        assertEquals(DetectedAnomaly.Severity.CRITICAL,
                DetectedAnomaly.Severity.fromSigma(4.0));
        assertEquals(DetectedAnomaly.Severity.CRITICAL,
                DetectedAnomaly.Severity.fromSigma(5.0));
        assertEquals(DetectedAnomaly.Severity.CRITICAL,
                DetectedAnomaly.Severity.fromSigma(-4.5));
    }

    @Test
    void testSeverityFromSigmaHigh() {
        assertEquals(DetectedAnomaly.Severity.HIGH,
                DetectedAnomaly.Severity.fromSigma(3.0));
        assertEquals(DetectedAnomaly.Severity.HIGH,
                DetectedAnomaly.Severity.fromSigma(3.5));
        assertEquals(DetectedAnomaly.Severity.HIGH,
                DetectedAnomaly.Severity.fromSigma(-3.9));
    }

    @Test
    void testSeverityFromSigmaMedium() {
        assertEquals(DetectedAnomaly.Severity.MEDIUM,
                DetectedAnomaly.Severity.fromSigma(2.5));
        assertEquals(DetectedAnomaly.Severity.MEDIUM,
                DetectedAnomaly.Severity.fromSigma(2.8));
        assertEquals(DetectedAnomaly.Severity.MEDIUM,
                DetectedAnomaly.Severity.fromSigma(-2.9));
    }

    @Test
    void testSeverityFromSigmaLow() {
        assertEquals(DetectedAnomaly.Severity.LOW,
                DetectedAnomaly.Severity.fromSigma(2.0));
        assertEquals(DetectedAnomaly.Severity.LOW,
                DetectedAnomaly.Severity.fromSigma(2.3));
        assertEquals(DetectedAnomaly.Severity.LOW,
                DetectedAnomaly.Severity.fromSigma(-2.4));
    }

    @Test
    void testSeverityEnumProperties() {
        DetectedAnomaly.Severity critical = DetectedAnomaly.Severity.CRITICAL;
        assertEquals("Critical", critical.getDisplayName());
        assertEquals("bg-danger", critical.getCssClass());
        assertNotNull(critical.getDescription());

        DetectedAnomaly.Severity high = DetectedAnomaly.Severity.HIGH;
        assertEquals("High", high.getDisplayName());
        assertEquals("bg-warning text-dark", high.getCssClass());
    }

    @Test
    void testAnomalyTypeEnumProperties() {
        DetectedAnomaly.AnomalyType spike = DetectedAnomaly.AnomalyType.SPIKE;
        assertEquals("Spike", spike.getDisplayName());
        assertEquals("bi-graph-up-arrow", spike.getIcon());
        assertNotNull(spike.getDescription());

        DetectedAnomaly.AnomalyType drop = DetectedAnomaly.AnomalyType.DROP;
        assertEquals("Drop", drop.getDisplayName());
        assertEquals("bi-graph-down-arrow", drop.getIcon());
    }

    @Test
    void testDirectionEnumProperties() {
        DetectedAnomaly.Direction above = DetectedAnomaly.Direction.ABOVE;
        assertEquals("Above Baseline", above.getDisplayName());
        assertEquals("text-danger", above.getCssClass());

        DetectedAnomaly.Direction below = DetectedAnomaly.Direction.BELOW;
        assertEquals("Below Baseline", below.getDisplayName());
        assertEquals("text-info", below.getCssClass());
    }

    @Test
    void testIsOpen() {
        DetectedAnomaly anomaly = new DetectedAnomaly();
        assertTrue(anomaly.isOpen());

        anomaly.setResolvedAt(Instant.now());
        assertFalse(anomaly.isOpen());
    }

    @Test
    void testIsAcknowledged() {
        DetectedAnomaly anomaly = new DetectedAnomaly();
        assertFalse(anomaly.isAcknowledged());

        anomaly.setAcknowledgedAt(Instant.now());
        assertTrue(anomaly.isAcknowledged());
    }

    @Test
    void testDeviationDisplay() {
        DetectedAnomaly anomaly = new DetectedAnomaly();
        anomaly.setDeviationSigma(3.5);
        anomaly.setDirection(DetectedAnomaly.Direction.ABOVE);

        String display = anomaly.getDeviationDisplay();
        assertTrue(display.contains("3.5"));
        assertTrue(display.contains("above"));

        anomaly.setDeviationSigma(-2.8);
        anomaly.setDirection(DetectedAnomaly.Direction.BELOW);
        display = anomaly.getDeviationDisplay();
        assertTrue(display.contains("2.8"));
        assertTrue(display.contains("below"));
    }

    @Test
    void testValueComparisonDisplay() {
        DetectedAnomaly anomaly = new DetectedAnomaly();
        anomaly.setAnomalyValue(150.0);
        anomaly.setBaselineMean(100.0);
        anomaly.setBaselineStddev(10.0);

        String display = anomaly.getValueComparisonDisplay();
        assertTrue(display.contains("150.00"));
        assertTrue(display.contains("100.00"));
        assertTrue(display.contains("10.00"));
    }

    @Test
    void testCorrelatedMetric() {
        DetectedAnomaly.CorrelatedMetric metric = new DetectedAnomaly.CorrelatedMetric(
                "cache_hit_ratio", -5.0, DetectedAnomaly.Direction.BELOW);

        assertEquals("cache_hit_ratio", metric.getMetricName());
        assertEquals(-5.0, metric.getChangePercent());
        assertEquals(DetectedAnomaly.Direction.BELOW, metric.getDirection());
    }

    @Test
    void testSettersAndGetters() {
        DetectedAnomaly anomaly = new DetectedAnomaly();

        anomaly.setId(123L);
        anomaly.setInstanceId("test-instance");
        anomaly.setMetricName("total_connections");
        anomaly.setMetricCategory("system");
        anomaly.setSeverity(DetectedAnomaly.Severity.HIGH);
        anomaly.setAnomalyType(DetectedAnomaly.AnomalyType.SPIKE);
        anomaly.setRootCauseSuggestion("Check for connection leaks");
        anomaly.setAcknowledgedBy("admin");
        anomaly.setResolutionNotes("Fixed by restarting application");
        anomaly.setAlertId(456L);

        assertEquals(123L, anomaly.getId());
        assertEquals("test-instance", anomaly.getInstanceId());
        assertEquals("total_connections", anomaly.getMetricName());
        assertEquals("system", anomaly.getMetricCategory());
        assertEquals(DetectedAnomaly.Severity.HIGH, anomaly.getSeverity());
        assertEquals(DetectedAnomaly.AnomalyType.SPIKE, anomaly.getAnomalyType());
        assertEquals("Check for connection leaks", anomaly.getRootCauseSuggestion());
        assertEquals("admin", anomaly.getAcknowledgedBy());
        assertEquals("Fixed by restarting application", anomaly.getResolutionNotes());
        assertEquals(456L, anomaly.getAlertId());
    }

    @Test
    void testCorrelatedMetricsList() {
        DetectedAnomaly anomaly = new DetectedAnomaly();

        List<DetectedAnomaly.CorrelatedMetric> correlatedMetrics = List.of(
                new DetectedAnomaly.CorrelatedMetric("metric1", 10.0, DetectedAnomaly.Direction.ABOVE),
                new DetectedAnomaly.CorrelatedMetric("metric2", -5.0, DetectedAnomaly.Direction.BELOW)
        );

        anomaly.setCorrelatedMetrics(correlatedMetrics);

        assertNotNull(anomaly.getCorrelatedMetrics());
        assertEquals(2, anomaly.getCorrelatedMetrics().size());
    }

    @Test
    void testDefaultConstructorSetsDetectedAt() {
        DetectedAnomaly anomaly = new DetectedAnomaly();
        assertNotNull(anomaly.getDetectedAt());
    }
}
