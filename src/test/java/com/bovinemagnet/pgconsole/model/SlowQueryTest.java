package com.bovinemagnet.pgconsole.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SlowQuery model.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
class SlowQueryTest {

    @Test
    void testDefaultConstructor() {
        SlowQuery query = new SlowQuery();
        assertNotNull(query);
        assertNull(query.getQueryId());
        assertNull(query.getQuery());
        assertEquals(0, query.getTotalCalls());
    }

    @Test
    void testParameterisedConstructor() {
        SlowQuery query = new SlowQuery(
            "12345", "SELECT * FROM users", 100, 1000.0,
            10.0, 1.0, 50.0, 500, "postgres", "testdb"
        );

        assertEquals("12345", query.getQueryId());
        assertEquals("SELECT * FROM users", query.getQuery());
        assertEquals(100, query.getTotalCalls());
        assertEquals(1000.0, query.getTotalTime(), 0.001);
        assertEquals(10.0, query.getMeanTime(), 0.001);
        assertEquals(1.0, query.getMinTime(), 0.001);
        assertEquals(50.0, query.getMaxTime(), 0.001);
        assertEquals(500, query.getRows());
        assertEquals("postgres", query.getUser());
        assertEquals("testdb", query.getDatabase());
    }

    @Test
    void testSettersAndGetters() {
        SlowQuery query = new SlowQuery();

        query.setQueryId("test-id");
        query.setQuery("SELECT 1");
        query.setTotalCalls(50L);
        query.setTotalTime(500.0);
        query.setMeanTime(10.0);
        query.setMinTime(5.0);
        query.setMaxTime(20.0);
        query.setStddevTime(3.0);
        query.setRows(25L);
        query.setSharedBlksHit(1000L);
        query.setSharedBlksRead(50L);
        query.setSharedBlksWritten(10L);
        query.setTempBlksRead(5L);
        query.setTempBlksWritten(2L);
        query.setUser("testuser");
        query.setDatabase("testdb");

        assertEquals("test-id", query.getQueryId());
        assertEquals("SELECT 1", query.getQuery());
        assertEquals(50L, query.getTotalCalls());
        assertEquals(500.0, query.getTotalTime(), 0.001);
        assertEquals(10.0, query.getMeanTime(), 0.001);
        assertEquals(5.0, query.getMinTime(), 0.001);
        assertEquals(20.0, query.getMaxTime(), 0.001);
        assertEquals(3.0, query.getStddevTime(), 0.001);
        assertEquals(25L, query.getRows());
        assertEquals(1000L, query.getSharedBlksHit());
        assertEquals(50L, query.getSharedBlksRead());
        assertEquals(10L, query.getSharedBlksWritten());
        assertEquals(5L, query.getTempBlksRead());
        assertEquals(2L, query.getTempBlksWritten());
        assertEquals("testuser", query.getUser());
        assertEquals("testdb", query.getDatabase());
    }

    @Test
    void testGetShortQueryWithShortQuery() {
        SlowQuery query = new SlowQuery();
        query.setQuery("SELECT * FROM users");

        assertEquals("SELECT * FROM users", query.getShortQuery());
    }

    @Test
    void testGetShortQueryWithLongQuery() {
        SlowQuery query = new SlowQuery();
        String longQuery = "SELECT id, name, email, address, phone, created_at, updated_at, status, role, department FROM users WHERE active = true AND deleted = false ORDER BY name";
        query.setQuery(longQuery);

        String shortQuery = query.getShortQuery();
        assertEquals(100, shortQuery.length());
        assertTrue(shortQuery.endsWith("..."));
    }

    @Test
    void testGetShortQueryWithNull() {
        SlowQuery query = new SlowQuery();
        query.setQuery(null);

        assertEquals("", query.getShortQuery());
    }

    @Test
    void testGetShortQueryWithExactly100Chars() {
        SlowQuery query = new SlowQuery();
        String exactQuery = "a".repeat(100);
        query.setQuery(exactQuery);

        assertEquals(exactQuery, query.getShortQuery());
    }

    @Test
    void testFormatTimeMilliseconds() {
        SlowQuery query = new SlowQuery();
        query.setMeanTime(0.5);

        String formatted = query.getMeanTimeFormatted();
        assertTrue(formatted.contains("ms"));
    }

    @Test
    void testFormatTimeSeconds() {
        SlowQuery query = new SlowQuery();
        query.setMeanTime(1500.0);

        String formatted = query.getMeanTimeFormatted();
        assertTrue(formatted.contains("s"));
        assertFalse(formatted.contains("ms"));
    }

    @Test
    void testFormatTimeMinutes() {
        SlowQuery query = new SlowQuery();
        query.setMeanTime(120000.0); // 2 minutes

        String formatted = query.getMeanTimeFormatted();
        assertTrue(formatted.contains("min"));
    }

    @Test
    void testAllTimeFormattingMethods() {
        SlowQuery query = new SlowQuery();
        query.setTotalTime(10000.0);
        query.setMeanTime(100.0);
        query.setMinTime(10.0);
        query.setMaxTime(500.0);
        query.setStddevTime(50.0);

        assertNotNull(query.getTotalTimeFormatted());
        assertNotNull(query.getMeanTimeFormatted());
        assertNotNull(query.getMinTimeFormatted());
        assertNotNull(query.getMaxTimeFormatted());
        assertNotNull(query.getStddevTimeFormatted());
    }

    @Test
    void testCacheHitRatioWithHits() {
        SlowQuery query = new SlowQuery();
        query.setSharedBlksHit(950);
        query.setSharedBlksRead(50);

        double ratio = query.getCacheHitRatio();
        assertEquals(95.0, ratio, 0.001);
    }

    @Test
    void testCacheHitRatioWithNoBlocks() {
        SlowQuery query = new SlowQuery();
        query.setSharedBlksHit(0);
        query.setSharedBlksRead(0);

        double ratio = query.getCacheHitRatio();
        assertEquals(100.0, ratio, 0.001);
    }

    @Test
    void testCacheHitRatioPerfectHit() {
        SlowQuery query = new SlowQuery();
        query.setSharedBlksHit(1000);
        query.setSharedBlksRead(0);

        double ratio = query.getCacheHitRatio();
        assertEquals(100.0, ratio, 0.001);
    }

    @Test
    void testCacheHitRatioNoHits() {
        SlowQuery query = new SlowQuery();
        query.setSharedBlksHit(0);
        query.setSharedBlksRead(100);

        double ratio = query.getCacheHitRatio();
        assertEquals(0.0, ratio, 0.001);
    }

    @Test
    void testCacheHitRatioFormatted() {
        SlowQuery query = new SlowQuery();
        query.setSharedBlksHit(95);
        query.setSharedBlksRead(5);

        String formatted = query.getCacheHitRatioFormatted();
        assertEquals("95.0%", formatted);
    }
}
