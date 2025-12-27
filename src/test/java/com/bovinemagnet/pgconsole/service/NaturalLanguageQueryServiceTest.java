package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.NaturalLanguageQuery;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NaturalLanguageQueryService.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@QuarkusTest
@TestProfile(NaturalLanguageQueryServiceTest.TestConfig.class)
class NaturalLanguageQueryServiceTest {

    public static class TestConfig implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.ofEntries(
                    Map.entry("quarkus.datasource.db-kind", "h2"),
                    Map.entry("quarkus.datasource.jdbc.url", "jdbc:h2:mem:nltest;DB_CLOSE_DELAY=-1"),
                    Map.entry("quarkus.datasource.username", "sa"),
                    Map.entry("quarkus.datasource.password", ""),
                    Map.entry("quarkus.flyway.migrate-at-start", "false"),
                    Map.entry("pg-console.instances", "default"),
                    Map.entry("pg-console.history.enabled", "false"),
                    Map.entry("pg-console.alerting.enabled", "false"),
                    Map.entry("pg-console.security.enabled", "false"),
                    Map.entry("pg-console.dashboards.insights.enabled", "true"),
                    Map.entry("quarkus.scheduler.enabled", "false")
            );
        }
    }

    @Inject
    NaturalLanguageQueryService service;

    // Tests for slow query patterns

    @Test
    void testParseSlowQueriesBasic() {
        NaturalLanguageQuery result = service.parseQuery("show slow queries", "test");

        assertTrue(result.isSuccessful());
        assertEquals(NaturalLanguageQuery.Intent.SLOW_QUERIES, result.getMatchedIntent());
        assertTrue(result.isUnderstood());
    }

    @Test
    void testParseSlowQuerySingular() {
        NaturalLanguageQuery result = service.parseQuery("slow query", "test");

        assertTrue(result.isSuccessful());
        assertEquals(NaturalLanguageQuery.Intent.SLOW_QUERIES, result.getMatchedIntent());
    }

    @Test
    void testParseSlowQueriesYesterday() {
        NaturalLanguageQuery result = service.parseQuery("show slow queries from yesterday", "test");

        assertTrue(result.isSuccessful());
        assertEquals(NaturalLanguageQuery.Intent.SLOW_QUERIES_YESTERDAY, result.getMatchedIntent());
        assertNotNull(result.getExtractedParameters());
        assertEquals("yesterday", result.getExtractedParameters().get("timeRange"));
    }

    @Test
    void testParseSlowQueriesHours() {
        NaturalLanguageQuery result = service.parseQuery("slow queries last 6 hours", "test");

        assertTrue(result.isSuccessful());
        assertEquals(NaturalLanguageQuery.Intent.SLOW_QUERIES_HOURS, result.getMatchedIntent());
        assertEquals("6", result.getExtractedParameters().get("hours"));
    }

    @Test
    void testParseLongRunningQueries() {
        NaturalLanguageQuery result = service.parseQuery("long running queries", "test");

        assertTrue(result.isSuccessful());
        assertEquals(NaturalLanguageQuery.Intent.SLOW_QUERIES, result.getMatchedIntent());
    }

    // Tests for table/storage patterns

    @Test
    void testParseTableGrowthFastest() {
        NaturalLanguageQuery result = service.parseQuery("which tables are growing fastest", "test");

        assertTrue(result.isSuccessful());
        assertEquals(NaturalLanguageQuery.Intent.TABLE_GROWTH, result.getMatchedIntent());
    }

    @Test
    void testParseTableGrowthQuickly() {
        NaturalLanguageQuery result = service.parseQuery("which tables growing quickly", "test");

        assertTrue(result.isSuccessful());
        assertEquals(NaturalLanguageQuery.Intent.TABLE_GROWTH, result.getMatchedIntent());
    }

    @Test
    void testParseBiggestTables() {
        NaturalLanguageQuery result = service.parseQuery("show biggest tables", "test");

        assertTrue(result.isSuccessful());
        assertEquals(NaturalLanguageQuery.Intent.TABLE_SIZE, result.getMatchedIntent());
    }

    @Test
    void testParseLargestTables() {
        NaturalLanguageQuery result = service.parseQuery("largest tables", "test");

        assertTrue(result.isSuccessful());
        assertEquals(NaturalLanguageQuery.Intent.TABLE_SIZE, result.getMatchedIntent());
    }

    // Tests for diagnosis patterns

    @Test
    void testParseWhyDatabaseSlow() {
        NaturalLanguageQuery result = service.parseQuery("why is the database slow", "test");

        assertTrue(result.isSuccessful());
        assertEquals(NaturalLanguageQuery.Intent.SLOW_DIAGNOSIS, result.getMatchedIntent());
    }

    @Test
    void testParseWhyDatabaseSlowShort() {
        NaturalLanguageQuery result = service.parseQuery("why database slow", "test");

        assertTrue(result.isSuccessful());
        assertEquals(NaturalLanguageQuery.Intent.SLOW_DIAGNOSIS, result.getMatchedIntent());
    }

    @Test
    void testParseWhatsWrong() {
        NaturalLanguageQuery result = service.parseQuery("what's wrong", "test");

        assertTrue(result.isSuccessful());
        assertEquals(NaturalLanguageQuery.Intent.SLOW_DIAGNOSIS, result.getMatchedIntent());
    }

    @Test
    void testParseDiagnose() {
        NaturalLanguageQuery result = service.parseQuery("diagnose the issue", "test");

        assertTrue(result.isSuccessful());
        assertEquals(NaturalLanguageQuery.Intent.SLOW_DIAGNOSIS, result.getMatchedIntent());
    }

    // Tests for lock patterns

    @Test
    void testParseLocks() {
        NaturalLanguageQuery result = service.parseQuery("show current locks", "test");

        assertTrue(result.isSuccessful());
        assertEquals(NaturalLanguageQuery.Intent.LOCKS, result.getMatchedIntent());
    }

    @Test
    void testParseBlocking() {
        NaturalLanguageQuery result = service.parseQuery("blocked queries", "test");

        assertTrue(result.isSuccessful());
        assertEquals(NaturalLanguageQuery.Intent.BLOCKING, result.getMatchedIntent());
    }

    @Test
    void testParseDeadlock() {
        NaturalLanguageQuery result = service.parseQuery("check for deadlocks", "test");

        assertTrue(result.isSuccessful());
        assertEquals(NaturalLanguageQuery.Intent.BLOCKING, result.getMatchedIntent());
    }

    // Tests for activity patterns

    @Test
    void testParseActiveConnections() {
        NaturalLanguageQuery result = service.parseQuery("active connections", "test");

        assertTrue(result.isSuccessful());
        assertEquals(NaturalLanguageQuery.Intent.ACTIVITY, result.getMatchedIntent());
    }

    @Test
    void testParseWhoIsConnected() {
        NaturalLanguageQuery result = service.parseQuery("who is connected", "test");

        assertTrue(result.isSuccessful());
        assertEquals(NaturalLanguageQuery.Intent.ACTIVITY, result.getMatchedIntent());
    }

    @Test
    void testParseCurrentSessions() {
        NaturalLanguageQuery result = service.parseQuery("current sessions", "test");

        assertTrue(result.isSuccessful());
        assertEquals(NaturalLanguageQuery.Intent.ACTIVITY, result.getMatchedIntent());
    }

    // Tests for replication patterns

    @Test
    void testParseReplicationStatus() {
        NaturalLanguageQuery result = service.parseQuery("replication status", "test");

        assertTrue(result.isSuccessful());
        assertEquals(NaturalLanguageQuery.Intent.REPLICATION, result.getMatchedIntent());
    }

    @Test
    void testParseReplicaLag() {
        NaturalLanguageQuery result = service.parseQuery("replica lag", "test");

        assertTrue(result.isSuccessful());
        assertEquals(NaturalLanguageQuery.Intent.REPLICATION, result.getMatchedIntent());
    }

    // Tests for index patterns

    @Test
    void testParseIndexRecommendations() {
        NaturalLanguageQuery result = service.parseQuery("index recommendations", "test");

        assertTrue(result.isSuccessful());
        assertEquals(NaturalLanguageQuery.Intent.INDEX_ADVISOR, result.getMatchedIntent());
    }

    @Test
    void testParseMissingIndexes() {
        NaturalLanguageQuery result = service.parseQuery("missing indexes", "test");

        assertTrue(result.isSuccessful());
        assertEquals(NaturalLanguageQuery.Intent.INDEX_ADVISOR, result.getMatchedIntent());
    }

    @Test
    void testParseUnusedIndexes() {
        NaturalLanguageQuery result = service.parseQuery("unused indexes", "test");

        assertTrue(result.isSuccessful());
        assertEquals(NaturalLanguageQuery.Intent.INDEX_ADVISOR, result.getMatchedIntent());
    }

    // Tests for vacuum patterns

    @Test
    void testParseVacuumStatus() {
        NaturalLanguageQuery result = service.parseQuery("vacuum status", "test");

        assertTrue(result.isSuccessful());
        assertEquals(NaturalLanguageQuery.Intent.VACUUM, result.getMatchedIntent());
    }

    @Test
    void testParseDeadTuples() {
        NaturalLanguageQuery result = service.parseQuery("show dead tuples", "test");

        assertTrue(result.isSuccessful());
        assertEquals(NaturalLanguageQuery.Intent.VACUUM, result.getMatchedIntent());
    }

    @Test
    void testParseTableBloat() {
        NaturalLanguageQuery result = service.parseQuery("table bloat", "test");

        assertTrue(result.isSuccessful());
        assertEquals(NaturalLanguageQuery.Intent.VACUUM, result.getMatchedIntent());
    }

    // Tests for storage patterns

    @Test
    void testParseDiskUsage() {
        NaturalLanguageQuery result = service.parseQuery("disk usage", "test");

        assertTrue(result.isSuccessful());
        assertEquals(NaturalLanguageQuery.Intent.STORAGE, result.getMatchedIntent());
    }

    @Test
    void testParseDatabaseSize() {
        NaturalLanguageQuery result = service.parseQuery("database size", "test");

        assertTrue(result.isSuccessful());
        assertEquals(NaturalLanguageQuery.Intent.STORAGE, result.getMatchedIntent());
    }

    // Tests for anomaly patterns

    @Test
    void testParseAnomalies() {
        NaturalLanguageQuery result = service.parseQuery("show anomalies", "test");

        assertTrue(result.isSuccessful());
        assertEquals(NaturalLanguageQuery.Intent.ANOMALIES, result.getMatchedIntent());
    }

    @Test
    void testParseUnusualActivity() {
        NaturalLanguageQuery result = service.parseQuery("unusual activity", "test");

        assertTrue(result.isSuccessful());
        assertEquals(NaturalLanguageQuery.Intent.ANOMALIES, result.getMatchedIntent());
    }

    // Tests for forecast patterns

    @Test
    void testParseForecast() {
        NaturalLanguageQuery result = service.parseQuery("forecast storage", "test");

        assertTrue(result.isSuccessful());
        assertEquals(NaturalLanguageQuery.Intent.FORECASTS, result.getMatchedIntent());
    }

    @Test
    void testParseWhenWillStorageBeFull() {
        NaturalLanguageQuery result = service.parseQuery("when will storage be full", "test");

        assertTrue(result.isSuccessful());
        assertEquals(NaturalLanguageQuery.Intent.FORECASTS, result.getMatchedIntent());
    }

    @Test
    void testParseCapacityPlanning() {
        NaturalLanguageQuery result = service.parseQuery("capacity planning", "test");

        assertTrue(result.isSuccessful());
        assertEquals(NaturalLanguageQuery.Intent.FORECASTS, result.getMatchedIntent());
    }

    // Tests for recommendation patterns

    @Test
    void testParseRecommendations() {
        NaturalLanguageQuery result = service.parseQuery("show recommendations", "test");

        assertTrue(result.isSuccessful());
        assertEquals(NaturalLanguageQuery.Intent.RECOMMENDATIONS, result.getMatchedIntent());
    }

    @Test
    void testParseWhatShouldIDo() {
        NaturalLanguageQuery result = service.parseQuery("what should I do", "test");

        assertTrue(result.isSuccessful());
        assertEquals(NaturalLanguageQuery.Intent.RECOMMENDATIONS, result.getMatchedIntent());
    }

    // Tests for edge cases

    @Test
    void testParseEmptyQuery() {
        NaturalLanguageQuery result = service.parseQuery("", "test");

        assertFalse(result.isSuccessful());
        assertEquals(NaturalLanguageQuery.Intent.UNKNOWN, result.getMatchedIntent());
    }

    @Test
    void testParseNullQuery() {
        NaturalLanguageQuery result = service.parseQuery(null, "test");

        assertFalse(result.isSuccessful());
        assertEquals(NaturalLanguageQuery.Intent.UNKNOWN, result.getMatchedIntent());
    }

    @Test
    void testParseBlankQuery() {
        NaturalLanguageQuery result = service.parseQuery("   ", "test");

        assertFalse(result.isSuccessful());
        assertEquals(NaturalLanguageQuery.Intent.UNKNOWN, result.getMatchedIntent());
    }

    @Test
    void testParseUnknownQuery() {
        NaturalLanguageQuery result = service.parseQuery("xyzzy gobbledygook", "test");

        assertFalse(result.isSuccessful());
        assertEquals(NaturalLanguageQuery.Intent.UNKNOWN, result.getMatchedIntent());
    }

    @Test
    void testParseCaseInsensitive() {
        NaturalLanguageQuery result1 = service.parseQuery("SLOW QUERIES", "test");
        NaturalLanguageQuery result2 = service.parseQuery("Slow Queries", "test");
        NaturalLanguageQuery result3 = service.parseQuery("slow queries", "test");

        assertTrue(result1.isSuccessful());
        assertTrue(result2.isSuccessful());
        assertTrue(result3.isSuccessful());
        assertEquals(result1.getMatchedIntent(), result2.getMatchedIntent());
        assertEquals(result2.getMatchedIntent(), result3.getMatchedIntent());
    }

    @Test
    void testResolvedToContainsInstance() {
        NaturalLanguageQuery result = service.parseQuery("slow queries", "production-db");

        assertTrue(result.getResolvedTo().contains("production-db"));
    }

    // Test explain term

    @ParameterizedTest
    @CsvSource({
            "cache hit ratio, cache",
            "dead tuples, rows",
            "seq scan, sequential",
            "blocking, waiting",
            "replication lag, delay",
            "shared_buffers, memory",
            "work_mem, sorting",
            "autovacuum, automatic"
    })
    void testExplainTermContainsKeyword(String term, String expectedKeyword) {
        String explanation = service.explainTerm(term);

        assertNotNull(explanation);
        assertTrue(explanation.toLowerCase().contains(expectedKeyword.toLowerCase()),
                "Explanation for '" + term + "' should contain '" + expectedKeyword + "'");
    }

    @Test
    void testExplainTermUnknown() {
        String explanation = service.explainTerm("unknown_term_xyz");

        assertNotNull(explanation);
        assertTrue(explanation.contains("No explanation available"));
        assertTrue(explanation.contains("unknown_term_xyz"));
    }

    // Test suggested queries

    @Test
    void testGetSuggestedQueries() {
        List<String> suggestions = service.getSuggestedQueries();

        assertNotNull(suggestions);
        assertFalse(suggestions.isEmpty());
        assertTrue(suggestions.size() >= 5);

        // Verify some expected suggestions exist
        assertTrue(suggestions.stream().anyMatch(s -> s.toLowerCase().contains("slow")));
        assertTrue(suggestions.stream().anyMatch(s -> s.toLowerCase().contains("block")));
    }

    @Test
    void testConfidenceScore() {
        NaturalLanguageQuery highConfidence = service.parseQuery("slow queries yesterday", "test");
        NaturalLanguageQuery lowerConfidence = service.parseQuery("slow queries", "test");

        // Yesterday pattern has higher priority (100) than basic slow queries (50)
        assertTrue(highConfidence.getConfidenceScore() > lowerConfidence.getConfidenceScore());
    }
}
