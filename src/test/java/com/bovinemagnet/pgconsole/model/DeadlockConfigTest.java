package com.bovinemagnet.pgconsole.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DeadlockConfig model.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
class DeadlockConfigTest {

    @Test
    void testDefaultConstructor() {
        DeadlockConfig config = new DeadlockConfig();
        assertNotNull(config);
        assertNull(config.getDeadlockTimeout());
        assertFalse(config.isLogLockWaits());
        assertNull(config.getLockTimeout());
    }

    @Test
    void testParameterisedConstructor() {
        DeadlockConfig config = new DeadlockConfig("1s", true, "5s");

        assertEquals("1s", config.getDeadlockTimeout());
        assertTrue(config.isLogLockWaits());
        assertEquals("5s", config.getLockTimeout());
    }

    @Test
    void testSettersAndGetters() {
        DeadlockConfig config = new DeadlockConfig();

        config.setDeadlockTimeout("2s");
        config.setLogLockWaits(true);
        config.setLockTimeout("10s");

        assertEquals("2s", config.getDeadlockTimeout());
        assertTrue(config.isLogLockWaits());
        assertEquals("10s", config.getLockTimeout());
    }

    @Test
    void testRecommendedValues() {
        DeadlockConfig config = new DeadlockConfig();

        assertEquals("1s", config.getRecommendedDeadlockTimeout());
        assertTrue(config.getRecommendedLogLockWaits());
    }

    @Test
    void testIsDeadlockTimeoutOptimalWhenOptimal() {
        DeadlockConfig config = new DeadlockConfig("1s", false, "0");

        assertTrue(config.isDeadlockTimeoutOptimal());
    }

    @Test
    void testIsDeadlockTimeoutOptimalWhenNotOptimal() {
        DeadlockConfig config = new DeadlockConfig("5s", false, "0");

        assertFalse(config.isDeadlockTimeoutOptimal());
    }

    @Test
    void testIsLogLockWaitsOptimalWhenEnabled() {
        DeadlockConfig config = new DeadlockConfig("1s", true, "0");

        assertTrue(config.isLogLockWaitsOptimal());
    }

    @Test
    void testIsLogLockWaitsOptimalWhenDisabled() {
        DeadlockConfig config = new DeadlockConfig("1s", false, "0");

        assertFalse(config.isLogLockWaitsOptimal());
    }

    @Test
    void testIsOptimalWhenAllOptimal() {
        DeadlockConfig config = new DeadlockConfig("1s", true, "0");

        assertTrue(config.isOptimal());
    }

    @Test
    void testIsOptimalWhenOneNotOptimal() {
        DeadlockConfig config = new DeadlockConfig("1s", false, "0");

        assertFalse(config.isOptimal());
    }

    @Test
    void testIsOptimalWhenNoneOptimal() {
        DeadlockConfig config = new DeadlockConfig("5s", false, "0");

        assertFalse(config.isOptimal());
    }

    @Test
    void testGetIssueCountZero() {
        DeadlockConfig config = new DeadlockConfig("1s", true, "0");

        assertEquals(0, config.getIssueCount());
    }

    @Test
    void testGetIssueCountOne() {
        DeadlockConfig config = new DeadlockConfig("1s", false, "0");

        assertEquals(1, config.getIssueCount());
    }

    @Test
    void testGetIssueCountTwo() {
        DeadlockConfig config = new DeadlockConfig("5s", false, "0");

        assertEquals(2, config.getIssueCount());
    }

    @Test
    void testGetRecommendationsWhenOptimal() {
        DeadlockConfig config = new DeadlockConfig("1s", true, "0");

        List<String> recommendations = config.getRecommendations();
        assertEquals(1, recommendations.size());
        assertTrue(recommendations.get(0).contains("optimal"));
    }

    @Test
    void testGetRecommendationsWhenLogLockWaitsDisabled() {
        DeadlockConfig config = new DeadlockConfig("1s", false, "0");

        List<String> recommendations = config.getRecommendations();
        assertEquals(1, recommendations.size());
        assertTrue(recommendations.get(0).contains("log_lock_waits"));
    }

    @Test
    void testGetRecommendationsWhenDeadlockTimeoutNotOptimal() {
        DeadlockConfig config = new DeadlockConfig("5s", true, "0");

        List<String> recommendations = config.getRecommendations();
        assertEquals(1, recommendations.size());
        assertTrue(recommendations.get(0).contains("deadlock_timeout"));
        assertTrue(recommendations.get(0).contains("5s"));
    }

    @Test
    void testGetRecommendationsWhenBothNotOptimal() {
        DeadlockConfig config = new DeadlockConfig("5s", false, "0");

        List<String> recommendations = config.getRecommendations();
        assertEquals(2, recommendations.size());
    }

    @Test
    void testGetDeadlockTimeoutBadgeClassOptimal() {
        DeadlockConfig config = new DeadlockConfig("1s", true, "0");

        assertEquals("bg-success", config.getDeadlockTimeoutBadgeClass());
    }

    @Test
    void testGetDeadlockTimeoutBadgeClassNotOptimal() {
        DeadlockConfig config = new DeadlockConfig("5s", true, "0");

        assertEquals("bg-warning text-dark", config.getDeadlockTimeoutBadgeClass());
    }

    @Test
    void testGetLogLockWaitsBadgeClassOptimal() {
        DeadlockConfig config = new DeadlockConfig("1s", true, "0");

        assertEquals("bg-success", config.getLogLockWaitsBadgeClass());
    }

    @Test
    void testGetLogLockWaitsBadgeClassNotOptimal() {
        DeadlockConfig config = new DeadlockConfig("1s", false, "0");

        assertEquals("bg-warning text-dark", config.getLogLockWaitsBadgeClass());
    }

    @Test
    void testGetOverallBadgeClassOptimal() {
        DeadlockConfig config = new DeadlockConfig("1s", true, "0");

        assertEquals("bg-success", config.getOverallBadgeClass());
    }

    @Test
    void testGetOverallBadgeClassNotOptimal() {
        DeadlockConfig config = new DeadlockConfig("5s", false, "0");

        assertEquals("bg-warning text-dark", config.getOverallBadgeClass());
    }

    @Test
    void testGetStatusSummaryOptimal() {
        DeadlockConfig config = new DeadlockConfig("1s", true, "0");

        assertEquals("Optimal", config.getStatusSummary());
    }

    @Test
    void testGetStatusSummaryOneIssue() {
        DeadlockConfig config = new DeadlockConfig("1s", false, "0");

        assertEquals("1 issue", config.getStatusSummary());
    }

    @Test
    void testGetStatusSummaryTwoIssues() {
        DeadlockConfig config = new DeadlockConfig("5s", false, "0");

        assertEquals("2 issues", config.getStatusSummary());
    }

    @Test
    void testGetDeadlockTimeoutExplanation() {
        DeadlockConfig config = new DeadlockConfig();

        String explanation = config.getDeadlockTimeoutExplanation();
        assertNotNull(explanation);
        assertTrue(explanation.contains("deadlock"));
        assertTrue(explanation.contains("lock"));
    }

    @Test
    void testGetLogLockWaitsExplanation() {
        DeadlockConfig config = new DeadlockConfig();

        String explanation = config.getLogLockWaitsExplanation();
        assertNotNull(explanation);
        assertTrue(explanation.contains("log"));
        assertTrue(explanation.contains("lock"));
    }

    @Test
    void testGetLockTimeoutExplanation() {
        DeadlockConfig config = new DeadlockConfig();

        String explanation = config.getLockTimeoutExplanation();
        assertNotNull(explanation);
        assertTrue(explanation.contains("lock"));
        assertTrue(explanation.contains("wait"));
    }
}
