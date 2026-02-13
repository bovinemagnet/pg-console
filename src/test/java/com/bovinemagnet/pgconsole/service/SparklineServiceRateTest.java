package com.bovinemagnet.pgconsole.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SparklineService rate computation.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
class SparklineServiceRateTest {

    private SparklineService sparklineService;

    @BeforeEach
    void setUp() {
        sparklineService = new SparklineService();
    }

    // --- computeRates tests ---

    @Test
    void testComputeRatesWithValidData() {
        Instant now = Instant.now();
        List<TestRecord> history = Arrays.asList(
                new TestRecord(100L, now),
                new TestRecord(200L, now.plus(60, ChronoUnit.SECONDS)),
                new TestRecord(400L, now.plus(120, ChronoUnit.SECONDS))
        );

        List<Double> rates = sparklineService.computeRates(
                history, TestRecord::getValue, TestRecord::getTimestamp);

        assertEquals(2, rates.size());
        // (200-100) / 60 = 1.6667
        assertEquals(100.0 / 60.0, rates.get(0), 0.01);
        // (400-200) / 60 = 3.3333
        assertEquals(200.0 / 60.0, rates.get(1), 0.01);
    }

    @Test
    void testComputeRatesWithNullHistory() {
        List<Double> rates = sparklineService.computeRates(
                null, TestRecord::getValue, TestRecord::getTimestamp);

        assertNotNull(rates);
        assertTrue(rates.isEmpty());
    }

    @Test
    void testComputeRatesWithEmptyList() {
        List<Double> rates = sparklineService.computeRates(
                Collections.emptyList(), TestRecord::getValue, TestRecord::getTimestamp);

        assertNotNull(rates);
        assertTrue(rates.isEmpty());
    }

    @Test
    void testComputeRatesWithSingleRecord() {
        Instant now = Instant.now();
        List<TestRecord> history = Collections.singletonList(new TestRecord(100L, now));

        List<Double> rates = sparklineService.computeRates(
                history, TestRecord::getValue, TestRecord::getTimestamp);

        assertNotNull(rates);
        assertTrue(rates.isEmpty());
    }

    @Test
    void testComputeRatesWithCounterReset() {
        Instant now = Instant.now();
        List<TestRecord> history = Arrays.asList(
                new TestRecord(1000L, now),
                new TestRecord(500L, now.plus(60, ChronoUnit.SECONDS)),   // counter reset
                new TestRecord(600L, now.plus(120, ChronoUnit.SECONDS))
        );

        List<Double> rates = sparklineService.computeRates(
                history, TestRecord::getValue, TestRecord::getTimestamp);

        assertEquals(2, rates.size());
        // Negative delta clamped to 0
        assertEquals(0.0, rates.get(0), 0.001);
        // (600-500) / 60
        assertEquals(100.0 / 60.0, rates.get(1), 0.01);
    }

    @Test
    void testComputeRatesWithNullValues() {
        Instant now = Instant.now();
        List<TestRecord> history = Arrays.asList(
                new TestRecord(null, now),
                new TestRecord(200L, now.plus(60, ChronoUnit.SECONDS)),
                new TestRecord(null, now.plus(120, ChronoUnit.SECONDS))
        );

        List<Double> rates = sparklineService.computeRates(
                history, TestRecord::getValue, TestRecord::getTimestamp);

        assertEquals(2, rates.size());
        assertEquals(0.0, rates.get(0), 0.001);
        assertEquals(0.0, rates.get(1), 0.001);
    }

    @Test
    void testComputeRatesWithNullTimestamps() {
        List<TestRecord> history = Arrays.asList(
                new TestRecord(100L, null),
                new TestRecord(200L, Instant.now())
        );

        List<Double> rates = sparklineService.computeRates(
                history, TestRecord::getValue, TestRecord::getTimestamp);

        assertEquals(1, rates.size());
        assertEquals(0.0, rates.get(0), 0.001);
    }

    @Test
    void testComputeRatesWithZeroTimeDelta() {
        Instant now = Instant.now();
        List<TestRecord> history = Arrays.asList(
                new TestRecord(100L, now),
                new TestRecord(200L, now) // same timestamp
        );

        List<Double> rates = sparklineService.computeRates(
                history, TestRecord::getValue, TestRecord::getTimestamp);

        assertEquals(1, rates.size());
        assertEquals(0.0, rates.get(0), 0.001);
    }

    @Test
    void testComputeRatesWithConstantValues() {
        Instant now = Instant.now();
        List<TestRecord> history = Arrays.asList(
                new TestRecord(100L, now),
                new TestRecord(100L, now.plus(60, ChronoUnit.SECONDS)),
                new TestRecord(100L, now.plus(120, ChronoUnit.SECONDS))
        );

        List<Double> rates = sparklineService.computeRates(
                history, TestRecord::getValue, TestRecord::getTimestamp);

        assertEquals(2, rates.size());
        assertEquals(0.0, rates.get(0), 0.001);
        assertEquals(0.0, rates.get(1), 0.001);
    }

    @Test
    void testComputeRatesWithVaryingIntervals() {
        Instant now = Instant.now();
        List<TestRecord> history = Arrays.asList(
                new TestRecord(0L, now),
                new TestRecord(30L, now.plus(30, ChronoUnit.SECONDS)),   // 30s interval
                new TestRecord(150L, now.plus(150, ChronoUnit.SECONDS))  // 120s interval
        );

        List<Double> rates = sparklineService.computeRates(
                history, TestRecord::getValue, TestRecord::getTimestamp);

        assertEquals(2, rates.size());
        // 30/30 = 1.0/sec
        assertEquals(1.0, rates.get(0), 0.01);
        // 120/120 = 1.0/sec
        assertEquals(1.0, rates.get(1), 0.01);
    }

    @Test
    void testComputeRatesWithLargeValues() {
        Instant now = Instant.now();
        List<TestRecord> history = Arrays.asList(
                new TestRecord(1_000_000_000L, now),
                new TestRecord(1_000_060_000L, now.plus(60, ChronoUnit.SECONDS))
        );

        List<Double> rates = sparklineService.computeRates(
                history, TestRecord::getValue, TestRecord::getTimestamp);

        assertEquals(1, rates.size());
        // 60000/60 = 1000/sec
        assertEquals(1000.0, rates.get(0), 0.01);
    }

    /**
     * Simple test record for rate computation tests.
     */
    private static class TestRecord {
        private final Long value;
        private final Instant timestamp;

        TestRecord(Long value, Instant timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }

        Long getValue() {
            return value;
        }

        Instant getTimestamp() {
            return timestamp;
        }
    }
}
