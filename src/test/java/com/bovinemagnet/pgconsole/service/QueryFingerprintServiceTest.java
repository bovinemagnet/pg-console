package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.QueryFingerprint;
import com.bovinemagnet.pgconsole.model.SlowQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for QueryFingerprintService SQL normalisation and grouping.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
class QueryFingerprintServiceTest {

    private QueryFingerprintService service;

    @BeforeEach
    void setUp() {
        service = new QueryFingerprintService();
    }

    // --- Normalisation Tests ---

    @Test
    void testNormaliseQueryWithStringLiterals() {
        String query = "SELECT * FROM users WHERE name = 'John'";
        String normalised = service.normaliseQuery(query);

        assertTrue(normalised.contains("?"));
        assertFalse(normalised.contains("'John'"));
    }

    @Test
    void testNormaliseQueryWithNumericLiterals() {
        String query = "SELECT * FROM orders WHERE amount > 100 AND quantity = 5";
        String normalised = service.normaliseQuery(query);

        assertTrue(normalised.contains("?"));
        assertFalse(normalised.contains("100"));
        assertFalse(normalised.contains("5"));
    }

    @Test
    void testNormaliseQueryWithPositionalParameters() {
        String query = "SELECT * FROM users WHERE id = $1 AND status = $2";
        String normalised = service.normaliseQuery(query);

        assertFalse(normalised.contains("$1"));
        assertFalse(normalised.contains("$2"));
    }

    @Test
    void testNormaliseQueryWithInList() {
        String query = "SELECT * FROM products WHERE category_id IN (1, 2, 3, 4, 5)";
        String normalised = service.normaliseQuery(query);

        assertTrue(normalised.contains("in (...)"));
    }

    @Test
    void testNormaliseQueryCollapseWhitespace() {
        String query = "SELECT   *    FROM    users    WHERE    id = 1";
        String normalised = service.normaliseQuery(query);

        assertFalse(normalised.contains("  "));
    }

    @Test
    void testNormaliseQueryToLowercase() {
        String query = "SELECT * FROM Users WHERE Id = 1";
        String normalised = service.normaliseQuery(query);

        assertEquals(normalised, normalised.toLowerCase());
    }

    @Test
    void testNormaliseQueryWithNull() {
        String normalised = service.normaliseQuery(null);
        assertEquals("", normalised);
    }

    @Test
    void testNormaliseQueryWithEmptyString() {
        String normalised = service.normaliseQuery("");
        assertEquals("", normalised);
    }

    @Test
    void testNormaliseQueryWithEscapedQuotes() {
        String query = "SELECT * FROM users WHERE name = 'O''Brien'";
        String normalised = service.normaliseQuery(query);

        assertTrue(normalised.contains("?"));
        assertFalse(normalised.contains("O''Brien"));
    }

    @Test
    void testNormaliseQueryWithDecimalNumbers() {
        String query = "SELECT * FROM products WHERE price > 19.99";
        String normalised = service.normaliseQuery(query);

        assertFalse(normalised.contains("19.99"));
    }

    @Test
    void testNormaliseQueryPreservesTableNames() {
        String query = "SELECT * FROM table_123 WHERE id = 1";
        String normalised = service.normaliseQuery(query);

        assertTrue(normalised.contains("table_123"));
    }

    @Test
    void testNormaliseQueryPreservesColumnNames() {
        String query = "SELECT column_1, column_2 FROM users WHERE id = 1";
        String normalised = service.normaliseQuery(query);

        assertTrue(normalised.contains("column_1"));
        assertTrue(normalised.contains("column_2"));
    }

    // --- Fingerprint Tests ---

    @Test
    void testComputeFingerprintNotNull() {
        String fingerprint = service.computeFingerprint("select * from users");
        assertNotNull(fingerprint);
        assertFalse(fingerprint.isEmpty());
    }

    @Test
    void testComputeFingerprintConsistent() {
        String query = "select * from users where id = ?";
        String fingerprint1 = service.computeFingerprint(query);
        String fingerprint2 = service.computeFingerprint(query);

        assertEquals(fingerprint1, fingerprint2);
    }

    @Test
    void testComputeFingerprintDifferentForDifferentQueries() {
        String fingerprint1 = service.computeFingerprint("select * from users");
        String fingerprint2 = service.computeFingerprint("select * from orders");

        assertNotEquals(fingerprint1, fingerprint2);
    }

    @Test
    void testComputeFingerprintLength() {
        String fingerprint = service.computeFingerprint("select * from users");
        assertEquals(16, fingerprint.length()); // 8 bytes = 16 hex chars
    }

    @Test
    void testComputeFingerprintHexFormat() {
        String fingerprint = service.computeFingerprint("select * from users");
        assertTrue(fingerprint.matches("[0-9a-f]+"));
    }

    @Test
    void testComputeFingerprintWithNull() {
        String fingerprint = service.computeFingerprint(null);
        assertEquals("", fingerprint);
    }

    @Test
    void testComputeFingerprintWithEmptyString() {
        String fingerprint = service.computeFingerprint("");
        assertEquals("", fingerprint);
    }

    // --- Similar Queries Produce Same Fingerprint ---

    @Test
    void testSimilarQueriesSameFingerprint() {
        String query1 = "SELECT * FROM users WHERE id = 1";
        String query2 = "SELECT * FROM users WHERE id = 999";

        String normalised1 = service.normaliseQuery(query1);
        String normalised2 = service.normaliseQuery(query2);

        String fingerprint1 = service.computeFingerprint(normalised1);
        String fingerprint2 = service.computeFingerprint(normalised2);

        assertEquals(fingerprint1, fingerprint2);
    }

    @Test
    void testSimilarQueriesWithDifferentStrings() {
        String query1 = "SELECT * FROM users WHERE name = 'Alice'";
        String query2 = "SELECT * FROM users WHERE name = 'Bob'";

        String normalised1 = service.normaliseQuery(query1);
        String normalised2 = service.normaliseQuery(query2);

        String fingerprint1 = service.computeFingerprint(normalised1);
        String fingerprint2 = service.computeFingerprint(normalised2);

        assertEquals(fingerprint1, fingerprint2);
    }

    // --- Grouping Tests ---

    @Test
    void testGroupQueriesEmpty() {
        List<QueryFingerprint> result = service.groupQueries(Collections.emptyList());
        assertTrue(result.isEmpty());
    }

    @Test
    void testGroupQueriesSingleQuery() {
        SlowQuery query = createSlowQuery("SELECT * FROM users WHERE id = 1", 100, 10.0, 1.0);
        List<QueryFingerprint> result = service.groupQueries(Collections.singletonList(query));

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getInstanceCount());
    }

    @Test
    void testGroupQueriesSimilarQueriesGrouped() {
        SlowQuery query1 = createSlowQuery("SELECT * FROM users WHERE id = 1", 100, 10.0, 1.0);
        SlowQuery query2 = createSlowQuery("SELECT * FROM users WHERE id = 2", 200, 20.0, 2.0);

        List<QueryFingerprint> result = service.groupQueries(Arrays.asList(query1, query2));

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getInstanceCount());
    }

    @Test
    void testGroupQueriesDifferentQueriesNotGrouped() {
        SlowQuery query1 = createSlowQuery("SELECT * FROM users WHERE id = 1", 100, 10.0, 1.0);
        SlowQuery query2 = createSlowQuery("SELECT * FROM orders WHERE id = 1", 200, 20.0, 2.0);

        List<QueryFingerprint> result = service.groupQueries(Arrays.asList(query1, query2));

        assertEquals(2, result.size());
    }

    @Test
    void testGroupQueriesStatsAggregated() {
        SlowQuery query1 = createSlowQuery("SELECT * FROM users WHERE id = 1", 100, 10.0, 1.0);
        SlowQuery query2 = createSlowQuery("SELECT * FROM users WHERE id = 2", 200, 20.0, 2.0);

        List<QueryFingerprint> result = service.groupQueries(Arrays.asList(query1, query2));

        assertEquals(1, result.size());
        QueryFingerprint group = result.get(0);
        assertEquals(300, group.getTotalCalls()); // 100 + 200
        assertEquals(30.0, group.getTotalTime(), 0.001); // 10 + 20
    }

    // --- Sorting Tests ---

    @Test
    void testGroupQueriesSortedByTotalTime() {
        SlowQuery query1 = createSlowQuery("SELECT * FROM users WHERE id = 1", 100, 50.0, 5.0);
        SlowQuery query2 = createSlowQuery("SELECT * FROM orders WHERE id = 1", 100, 100.0, 10.0);

        List<QueryFingerprint> result = service.groupQueriesSortedByTotalTime(Arrays.asList(query1, query2));

        assertEquals(2, result.size());
        assertTrue(result.get(0).getTotalTime() >= result.get(1).getTotalTime());
    }

    @Test
    void testGroupQueriesSortedByCalls() {
        SlowQuery query1 = createSlowQuery("SELECT * FROM users WHERE id = 1", 50, 10.0, 1.0);
        SlowQuery query2 = createSlowQuery("SELECT * FROM orders WHERE id = 1", 200, 10.0, 1.0);

        List<QueryFingerprint> result = service.groupQueriesSortedByCalls(Arrays.asList(query1, query2));

        assertEquals(2, result.size());
        assertTrue(result.get(0).getTotalCalls() >= result.get(1).getTotalCalls());
    }

    @Test
    void testGroupQueriesSortedByAvgTime() {
        SlowQuery query1 = createSlowQuery("SELECT * FROM users WHERE id = 1", 100, 10.0, 0.1);
        SlowQuery query2 = createSlowQuery("SELECT * FROM orders WHERE id = 1", 100, 10.0, 0.5);

        List<QueryFingerprint> result = service.groupQueriesSortedByAvgTime(Arrays.asList(query1, query2));

        assertEquals(2, result.size());
        assertTrue(result.get(0).getAvgMeanTime() >= result.get(1).getAvgMeanTime());
    }

    // --- Helper Methods ---

    private SlowQuery createSlowQuery(String query, long calls, double totalTime, double meanTime) {
        SlowQuery slowQuery = new SlowQuery();
        slowQuery.setQuery(query);
        slowQuery.setTotalCalls(calls);
        slowQuery.setTotalTime(totalTime);
        slowQuery.setMeanTime(meanTime);
        slowQuery.setQueryId("test-" + System.nanoTime());
        return slowQuery;
    }
}
