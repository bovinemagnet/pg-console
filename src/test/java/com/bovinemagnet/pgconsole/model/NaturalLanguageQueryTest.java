package com.bovinemagnet.pgconsole.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NaturalLanguageQuery model class.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
class NaturalLanguageQueryTest {

    @Test
    void testIntentEnumProperties() {
        NaturalLanguageQuery.Intent slowQueries = NaturalLanguageQuery.Intent.SLOW_QUERIES;
        assertEquals("Slow Queries", slowQueries.getDisplayName());
        assertEquals("/slow-queries", slowQueries.getTargetEndpoint());
        assertNotNull(slowQueries.getDescription());

        NaturalLanguageQuery.Intent locks = NaturalLanguageQuery.Intent.LOCKS;
        assertEquals("Locks", locks.getDisplayName());
        assertEquals("/locks", locks.getTargetEndpoint());

        NaturalLanguageQuery.Intent unknown = NaturalLanguageQuery.Intent.UNKNOWN;
        assertEquals("Unknown", unknown.getDisplayName());
        assertEquals("/", unknown.getTargetEndpoint());
    }

    @Test
    void testFeedbackEnumProperties() {
        NaturalLanguageQuery.Feedback helpful = NaturalLanguageQuery.Feedback.HELPFUL;
        assertEquals("Helpful", helpful.getDisplayName());
        assertNotNull(helpful.getDescription());

        NaturalLanguageQuery.Feedback notHelpful = NaturalLanguageQuery.Feedback.NOT_HELPFUL;
        assertEquals("Not Helpful", notHelpful.getDisplayName());

        NaturalLanguageQuery.Feedback wrong = NaturalLanguageQuery.Feedback.WRONG;
        assertEquals("Wrong", wrong.getDisplayName());
    }

    @Test
    void testBuildUrlWithIntent() {
        NaturalLanguageQuery query = new NaturalLanguageQuery("show slow queries");
        query.setMatchedIntent(NaturalLanguageQuery.Intent.SLOW_QUERIES);

        String url = query.buildUrl("test-instance");

        assertTrue(url.contains("/slow-queries"));
        assertTrue(url.contains("instance=test-instance"));
    }

    @Test
    void testBuildUrlWithParameters() {
        NaturalLanguageQuery query = new NaturalLanguageQuery("show slow queries from last 24 hours");
        query.setMatchedIntent(NaturalLanguageQuery.Intent.SLOW_QUERIES_HOURS);

        Map<String, String> params = new HashMap<>();
        params.put("hours", "24");
        query.setExtractedParameters(params);

        String url = query.buildUrl("test-instance");

        assertTrue(url.contains("/slow-queries"));
        assertTrue(url.contains("instance=test-instance"));
        assertTrue(url.contains("hours=24"));
    }

    @Test
    void testBuildUrlWithNoIntent() {
        NaturalLanguageQuery query = new NaturalLanguageQuery();
        query.setMatchedIntent(null);

        String url = query.buildUrl("test-instance");

        assertEquals("/", url);
    }

    @Test
    void testGetExplanationUnderstood() {
        NaturalLanguageQuery query = new NaturalLanguageQuery("show slow queries");
        query.setMatchedIntent(NaturalLanguageQuery.Intent.SLOW_QUERIES);

        String explanation = query.getExplanation();

        assertTrue(explanation.toLowerCase().contains("query"));
    }

    @Test
    void testGetExplanationWithParameters() {
        NaturalLanguageQuery query = new NaturalLanguageQuery();
        query.setMatchedIntent(NaturalLanguageQuery.Intent.SLOW_QUERIES_HOURS);

        Map<String, String> params = new HashMap<>();
        params.put("hours", "6");
        query.setExtractedParameters(params);

        String explanation = query.getExplanation();

        assertTrue(explanation.contains("hours"));
        assertTrue(explanation.contains("6"));
    }

    @Test
    void testGetExplanationUnknown() {
        NaturalLanguageQuery query = new NaturalLanguageQuery();
        query.setMatchedIntent(NaturalLanguageQuery.Intent.UNKNOWN);

        String explanation = query.getExplanation();

        assertTrue(explanation.contains("couldn't understand"));
    }

    @Test
    void testGetExplanationNullIntent() {
        NaturalLanguageQuery query = new NaturalLanguageQuery();
        query.setMatchedIntent(null);

        String explanation = query.getExplanation();

        assertTrue(explanation.contains("couldn't understand"));
    }

    @Test
    void testIsUnderstoodTrue() {
        NaturalLanguageQuery query = new NaturalLanguageQuery();
        query.setMatchedIntent(NaturalLanguageQuery.Intent.SLOW_QUERIES);

        assertTrue(query.isUnderstood());
    }

    @Test
    void testIsUnderstoodFalseUnknown() {
        NaturalLanguageQuery query = new NaturalLanguageQuery();
        query.setMatchedIntent(NaturalLanguageQuery.Intent.UNKNOWN);

        assertFalse(query.isUnderstood());
    }

    @Test
    void testIsUnderstoodFalseNull() {
        NaturalLanguageQuery query = new NaturalLanguageQuery();
        query.setMatchedIntent(null);

        assertFalse(query.isUnderstood());
    }

    @Test
    void testDefaultConstructor() {
        NaturalLanguageQuery query = new NaturalLanguageQuery();

        assertNotNull(query.getQueriedAt());
        assertTrue(query.isSuccessful());
    }

    @Test
    void testConstructorWithQueryText() {
        NaturalLanguageQuery query = new NaturalLanguageQuery("show me locks");

        assertEquals("show me locks", query.getQueryText());
        assertNotNull(query.getQueriedAt());
        assertTrue(query.isSuccessful());
    }

    @Test
    void testSettersAndGetters() {
        NaturalLanguageQuery query = new NaturalLanguageQuery();

        query.setId(123L);
        query.setQueryText("show slow queries");
        query.setMatchedIntent(NaturalLanguageQuery.Intent.SLOW_QUERIES);
        query.setResolvedTo("/slow-queries?instance=test");
        query.setSuccessful(true);
        query.setUserFeedback(NaturalLanguageQuery.Feedback.HELPFUL);
        query.setQueriedBy("admin");
        query.setConfidenceScore(0.95);
        query.setMatchedPattern("slow\\s+queries");

        Instant now = Instant.now();
        query.setQueriedAt(now);

        Map<String, String> params = Map.of("sort", "time");
        query.setExtractedParameters(params);

        assertEquals(123L, query.getId());
        assertEquals("show slow queries", query.getQueryText());
        assertEquals(NaturalLanguageQuery.Intent.SLOW_QUERIES, query.getMatchedIntent());
        assertEquals("/slow-queries?instance=test", query.getResolvedTo());
        assertTrue(query.isSuccessful());
        assertEquals(NaturalLanguageQuery.Feedback.HELPFUL, query.getUserFeedback());
        assertEquals(now, query.getQueriedAt());
        assertEquals("admin", query.getQueriedBy());
        assertEquals(0.95, query.getConfidenceScore());
        assertEquals("slow\\s+queries", query.getMatchedPattern());
        assertNotNull(query.getExtractedParameters());
        assertEquals("time", query.getExtractedParameters().get("sort"));
    }

    @Test
    void testAllIntentEndpoints() {
        // Verify all intents have valid endpoints
        for (NaturalLanguageQuery.Intent intent : NaturalLanguageQuery.Intent.values()) {
            assertNotNull(intent.getTargetEndpoint());
            assertFalse(intent.getTargetEndpoint().isEmpty());
            assertNotNull(intent.getDisplayName());
            assertNotNull(intent.getDescription());
        }
    }

    @Test
    void testBuildUrlWithMultipleParameters() {
        NaturalLanguageQuery query = new NaturalLanguageQuery();
        query.setMatchedIntent(NaturalLanguageQuery.Intent.TABLE_GROWTH);

        Map<String, String> params = new HashMap<>();
        params.put("sort", "growth");
        params.put("limit", "10");
        query.setExtractedParameters(params);

        String url = query.buildUrl("prod-db");

        assertTrue(url.startsWith("/storage-insights"));
        assertTrue(url.contains("instance=prod-db"));
        assertTrue(url.contains("sort=growth"));
        assertTrue(url.contains("limit=10"));
    }
}
