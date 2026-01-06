package com.bovinemagnet.pgconsole.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DeadlockStats model.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
class DeadlockStatsTest {

    @Test
    void testDefaultConstructor() {
        DeadlockStats stats = new DeadlockStats();
        assertNotNull(stats);
        assertNull(stats.getDatabaseName());
        assertEquals(0, stats.getDeadlockCount());
        assertEquals(0.0, stats.getDeadlocksPerHour(), 0.001);
        assertNull(stats.getSparklineSvg());
        assertNull(stats.getStatsReset());
    }

    @Test
    void testParameterisedConstructor() {
        Instant resetTime = Instant.now().minus(2, ChronoUnit.HOURS);
        DeadlockStats stats = new DeadlockStats(
            "testdb", 5, 2.5, "<svg>sparkline</svg>", resetTime
        );

        assertEquals("testdb", stats.getDatabaseName());
        assertEquals(5, stats.getDeadlockCount());
        assertEquals(2.5, stats.getDeadlocksPerHour(), 0.001);
        assertEquals("<svg>sparkline</svg>", stats.getSparklineSvg());
        assertEquals(resetTime, stats.getStatsReset());
    }

    @Test
    void testSettersAndGetters() {
        DeadlockStats stats = new DeadlockStats();
        Instant resetTime = Instant.now();

        stats.setDatabaseName("mydb");
        stats.setDeadlockCount(10);
        stats.setDeadlocksPerHour(3.5);
        stats.setSparklineSvg("<svg>test</svg>");
        stats.setStatsReset(resetTime);

        assertEquals("mydb", stats.getDatabaseName());
        assertEquals(10, stats.getDeadlockCount());
        assertEquals(3.5, stats.getDeadlocksPerHour(), 0.001);
        assertEquals("<svg>test</svg>", stats.getSparklineSvg());
        assertEquals(resetTime, stats.getStatsReset());
    }

    @Test
    void testHasDeadlocksWithZero() {
        DeadlockStats stats = new DeadlockStats();
        stats.setDeadlockCount(0);

        assertFalse(stats.hasDeadlocks());
    }

    @Test
    void testHasDeadlocksWithPositiveCount() {
        DeadlockStats stats = new DeadlockStats();
        stats.setDeadlockCount(5);

        assertTrue(stats.hasDeadlocks());
    }

    @Test
    void testHasHighRateBelowThreshold() {
        DeadlockStats stats = new DeadlockStats();
        stats.setDeadlocksPerHour(0.5);

        assertFalse(stats.hasHighRate());
    }

    @Test
    void testHasHighRateAboveThreshold() {
        DeadlockStats stats = new DeadlockStats();
        stats.setDeadlocksPerHour(1.5);

        assertTrue(stats.hasHighRate());
    }

    @Test
    void testHasHighRateAtExactThreshold() {
        DeadlockStats stats = new DeadlockStats();
        stats.setDeadlocksPerHour(1.0);

        assertFalse(stats.hasHighRate()); // Must be > 1.0, not >= 1.0
    }

    @Test
    void testGetSeverityCssClassDanger() {
        DeadlockStats stats = new DeadlockStats();
        stats.setDeadlocksPerHour(2.0);
        stats.setDeadlockCount(5);

        assertEquals("text-danger fw-bold", stats.getSeverityCssClass());
    }

    @Test
    void testGetSeverityCssClassDangerHighCount() {
        DeadlockStats stats = new DeadlockStats();
        stats.setDeadlocksPerHour(0.5);
        stats.setDeadlockCount(15); // > 10

        assertEquals("text-danger fw-bold", stats.getSeverityCssClass());
    }

    @Test
    void testGetSeverityCssClassWarning() {
        DeadlockStats stats = new DeadlockStats();
        stats.setDeadlocksPerHour(0.5);
        stats.setDeadlockCount(5);

        assertEquals("text-warning", stats.getSeverityCssClass());
    }

    @Test
    void testGetSeverityCssClassMuted() {
        DeadlockStats stats = new DeadlockStats();
        stats.setDeadlocksPerHour(0);
        stats.setDeadlockCount(0);

        assertEquals("text-muted", stats.getSeverityCssClass());
    }

    @Test
    void testGetBadgeCssClassDanger() {
        DeadlockStats stats = new DeadlockStats();
        stats.setDeadlocksPerHour(2.0);

        assertEquals("bg-danger", stats.getBadgeCssClass());
    }

    @Test
    void testGetBadgeCssClassWarning() {
        DeadlockStats stats = new DeadlockStats();
        stats.setDeadlocksPerHour(0.5);
        stats.setDeadlockCount(3);

        assertEquals("bg-warning text-dark", stats.getBadgeCssClass());
    }

    @Test
    void testGetBadgeCssClassSuccess() {
        DeadlockStats stats = new DeadlockStats();
        stats.setDeadlocksPerHour(0);
        stats.setDeadlockCount(0);

        assertEquals("bg-success", stats.getBadgeCssClass());
    }

    @Test
    void testGetTimeSinceResetWithNull() {
        DeadlockStats stats = new DeadlockStats();
        stats.setStatsReset(null);

        assertEquals("Unknown", stats.getTimeSinceReset());
    }

    @Test
    void testGetTimeSinceResetDays() {
        DeadlockStats stats = new DeadlockStats();
        stats.setStatsReset(Instant.now().minus(3, ChronoUnit.DAYS));

        String result = stats.getTimeSinceReset();
        assertTrue(result.contains("day"));
    }

    @Test
    void testGetTimeSinceResetHours() {
        DeadlockStats stats = new DeadlockStats();
        stats.setStatsReset(Instant.now().minus(5, ChronoUnit.HOURS));

        String result = stats.getTimeSinceReset();
        assertTrue(result.contains("hour"));
    }

    @Test
    void testGetTimeSinceResetMinutes() {
        DeadlockStats stats = new DeadlockStats();
        stats.setStatsReset(Instant.now().minus(30, ChronoUnit.MINUTES));

        String result = stats.getTimeSinceReset();
        assertTrue(result.contains("minute"));
    }

    @Test
    void testGetTimeSinceResetSingularDay() {
        DeadlockStats stats = new DeadlockStats();
        // Set to exactly 25 hours ago to ensure it shows "1 day"
        stats.setStatsReset(Instant.now().minus(25, ChronoUnit.HOURS));

        String result = stats.getTimeSinceReset();
        assertEquals("1 day", result);
    }

    @Test
    void testGetFormattedRateUnavailable() {
        DeadlockStats stats = new DeadlockStats();
        stats.setDeadlocksPerHour(-1);

        assertEquals("N/A", stats.getFormattedRate());
    }

    @Test
    void testGetFormattedRateZero() {
        DeadlockStats stats = new DeadlockStats();
        stats.setDeadlocksPerHour(0);

        assertEquals("0", stats.getFormattedRate());
    }

    @Test
    void testGetFormattedRateVerySmall() {
        DeadlockStats stats = new DeadlockStats();
        stats.setDeadlocksPerHour(0.005);

        assertEquals("< 0.01", stats.getFormattedRate());
    }

    @Test
    void testGetFormattedRateNormal() {
        DeadlockStats stats = new DeadlockStats();
        stats.setDeadlocksPerHour(2.567);

        assertEquals("2.57", stats.getFormattedRate());
    }

    @Test
    void testHasSparklineWithNull() {
        DeadlockStats stats = new DeadlockStats();
        stats.setSparklineSvg(null);

        assertFalse(stats.hasSparkline());
    }

    @Test
    void testHasSparklineWithEmpty() {
        DeadlockStats stats = new DeadlockStats();
        stats.setSparklineSvg("");

        assertFalse(stats.hasSparkline());
    }

    @Test
    void testHasSparklineWithContent() {
        DeadlockStats stats = new DeadlockStats();
        stats.setSparklineSvg("<svg>test</svg>");

        assertTrue(stats.hasSparkline());
    }

    @Test
    void testHoursSinceResetCalculation() {
        DeadlockStats stats = new DeadlockStats();
        Instant twoHoursAgo = Instant.now().minus(2, ChronoUnit.HOURS);
        stats.setStatsReset(twoHoursAgo);

        assertTrue(stats.getHoursSinceReset() >= 1.9);
        assertTrue(stats.getHoursSinceReset() <= 2.1);
    }

    @Test
    void testHoursSinceResetInConstructor() {
        Instant fiveHoursAgo = Instant.now().minus(5, ChronoUnit.HOURS);
        DeadlockStats stats = new DeadlockStats("testdb", 0, 0, "", fiveHoursAgo);

        assertTrue(stats.getHoursSinceReset() >= 4.9);
        assertTrue(stats.getHoursSinceReset() <= 5.1);
    }
}
