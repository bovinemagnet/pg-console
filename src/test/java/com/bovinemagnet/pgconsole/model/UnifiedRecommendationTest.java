package com.bovinemagnet.pgconsole.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UnifiedRecommendation model class.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
class UnifiedRecommendationTest {

    @Test
    void testSourceEnumProperties() {
        UnifiedRecommendation.Source indexAdvisor = UnifiedRecommendation.Source.INDEX_ADVISOR;
        assertEquals("Index Advisor", indexAdvisor.getDisplayName());
        assertEquals("bi-list-ul", indexAdvisor.getIcon());
        assertNotNull(indexAdvisor.getDescription());

        UnifiedRecommendation.Source tableMaint = UnifiedRecommendation.Source.TABLE_MAINTENANCE;
        assertEquals("Table Maintenance", tableMaint.getDisplayName());

        UnifiedRecommendation.Source queryReg = UnifiedRecommendation.Source.QUERY_REGRESSION;
        assertEquals("Query Regression", queryReg.getDisplayName());

        UnifiedRecommendation.Source config = UnifiedRecommendation.Source.CONFIG_TUNING;
        assertEquals("Configuration", config.getDisplayName());

        UnifiedRecommendation.Source anomaly = UnifiedRecommendation.Source.ANOMALY;
        assertEquals("Anomaly", anomaly.getDisplayName());
    }

    @Test
    void testSeverityEnumProperties() {
        UnifiedRecommendation.Severity critical = UnifiedRecommendation.Severity.CRITICAL;
        assertEquals("Critical", critical.getDisplayName());
        assertEquals("bg-danger", critical.getCssClass());
        assertEquals(4, critical.getWeight());

        UnifiedRecommendation.Severity high = UnifiedRecommendation.Severity.HIGH;
        assertEquals(3, high.getWeight());

        UnifiedRecommendation.Severity medium = UnifiedRecommendation.Severity.MEDIUM;
        assertEquals(2, medium.getWeight());

        UnifiedRecommendation.Severity low = UnifiedRecommendation.Severity.LOW;
        assertEquals(1, low.getWeight());
    }

    @Test
    void testImpactEnumProperties() {
        UnifiedRecommendation.Impact high = UnifiedRecommendation.Impact.HIGH;
        assertEquals("High", high.getDisplayName());
        assertNotNull(high.getDescription());

        UnifiedRecommendation.Impact medium = UnifiedRecommendation.Impact.MEDIUM;
        assertEquals("Medium", medium.getDisplayName());

        UnifiedRecommendation.Impact low = UnifiedRecommendation.Impact.LOW;
        assertEquals("Low", low.getDisplayName());
    }

    @Test
    void testEffortEnumProperties() {
        UnifiedRecommendation.Effort minimal = UnifiedRecommendation.Effort.MINIMAL;
        assertEquals("Minimal", minimal.getDisplayName());
        assertNotNull(minimal.getDescription());
        assertTrue(minimal.getDescription().contains("5 minutes"));

        UnifiedRecommendation.Effort low = UnifiedRecommendation.Effort.LOW;
        assertEquals("Low", low.getDisplayName());

        UnifiedRecommendation.Effort medium = UnifiedRecommendation.Effort.MEDIUM;
        assertEquals("Medium", medium.getDisplayName());

        UnifiedRecommendation.Effort high = UnifiedRecommendation.Effort.HIGH;
        assertEquals("High", high.getDisplayName());
    }

    @Test
    void testStatusEnumProperties() {
        UnifiedRecommendation.Status open = UnifiedRecommendation.Status.OPEN;
        assertEquals("Open", open.getDisplayName());
        assertEquals("bg-primary", open.getCssClass());
        assertNotNull(open.getDescription());

        UnifiedRecommendation.Status inProgress = UnifiedRecommendation.Status.IN_PROGRESS;
        assertEquals("In Progress", inProgress.getDisplayName());

        UnifiedRecommendation.Status applied = UnifiedRecommendation.Status.APPLIED;
        assertEquals("Applied", applied.getDisplayName());
        assertEquals("bg-success", applied.getCssClass());

        UnifiedRecommendation.Status dismissed = UnifiedRecommendation.Status.DISMISSED;
        assertEquals("Dismissed", dismissed.getDisplayName());

        UnifiedRecommendation.Status deferred = UnifiedRecommendation.Status.DEFERRED;
        assertEquals("Deferred", deferred.getDisplayName());
    }

    @Test
    void testEffectivenessEnumProperties() {
        UnifiedRecommendation.Effectiveness excellent = UnifiedRecommendation.Effectiveness.EXCELLENT;
        assertEquals("Excellent", excellent.getDisplayName());
        assertEquals("bg-success", excellent.getCssClass());
        assertNotNull(excellent.getDescription());

        UnifiedRecommendation.Effectiveness good = UnifiedRecommendation.Effectiveness.GOOD;
        assertEquals("Good", good.getDisplayName());

        UnifiedRecommendation.Effectiveness neutral = UnifiedRecommendation.Effectiveness.NEUTRAL;
        assertEquals("Neutral", neutral.getDisplayName());

        UnifiedRecommendation.Effectiveness poor = UnifiedRecommendation.Effectiveness.POOR;
        assertEquals("Poor", poor.getDisplayName());
        assertEquals("bg-danger", poor.getCssClass());
    }

    @Test
    void testCalculatePriorityScoreCriticalHighMinimal() {
        int score = UnifiedRecommendation.calculatePriorityScore(
                UnifiedRecommendation.Severity.CRITICAL,
                UnifiedRecommendation.Impact.HIGH,
                UnifiedRecommendation.Effort.MINIMAL);

        // 4*12 + 30 + 20 = 98
        assertEquals(98, score);
    }

    @Test
    void testCalculatePriorityScoreLowLowHigh() {
        int score = UnifiedRecommendation.calculatePriorityScore(
                UnifiedRecommendation.Severity.LOW,
                UnifiedRecommendation.Impact.LOW,
                UnifiedRecommendation.Effort.HIGH);

        // 1*12 + 10 + 5 = 27
        assertEquals(27, score);
    }

    @Test
    void testCalculatePriorityScoreMediumMediumMedium() {
        int score = UnifiedRecommendation.calculatePriorityScore(
                UnifiedRecommendation.Severity.MEDIUM,
                UnifiedRecommendation.Impact.MEDIUM,
                UnifiedRecommendation.Effort.MEDIUM);

        // 2*12 + 20 + 10 = 54
        assertEquals(54, score);
    }

    @Test
    void testCalculatePriorityScoreMaxCapped() {
        // Score should not exceed 100
        int score = UnifiedRecommendation.calculatePriorityScore(
                UnifiedRecommendation.Severity.CRITICAL,
                UnifiedRecommendation.Impact.HIGH,
                UnifiedRecommendation.Effort.MINIMAL);

        assertTrue(score <= 100);
    }

    @Test
    void testIsOneClickApplicableTrue() {
        UnifiedRecommendation rec = new UnifiedRecommendation();
        rec.setSuggestedSql("CREATE INDEX idx_test ON table1(col1)");
        rec.setEstimatedEffort(UnifiedRecommendation.Effort.MINIMAL);

        assertTrue(rec.isOneClickApplicable());
    }

    @Test
    void testIsOneClickApplicableFalseNullSql() {
        UnifiedRecommendation rec = new UnifiedRecommendation();
        rec.setSuggestedSql(null);
        rec.setEstimatedEffort(UnifiedRecommendation.Effort.MINIMAL);

        assertFalse(rec.isOneClickApplicable());
    }

    @Test
    void testIsOneClickApplicableFalseBlankSql() {
        UnifiedRecommendation rec = new UnifiedRecommendation();
        rec.setSuggestedSql("   ");
        rec.setEstimatedEffort(UnifiedRecommendation.Effort.MINIMAL);

        assertFalse(rec.isOneClickApplicable());
    }

    @Test
    void testIsOneClickApplicableFalseHighEffort() {
        UnifiedRecommendation rec = new UnifiedRecommendation();
        rec.setSuggestedSql("CREATE INDEX...");
        rec.setEstimatedEffort(UnifiedRecommendation.Effort.HIGH);

        assertFalse(rec.isOneClickApplicable());
    }

    @Test
    void testGetImprovementPercentPositive() {
        UnifiedRecommendation rec = new UnifiedRecommendation();
        rec.setPreMetricValue(1000.0);
        rec.setPostMetricValue(600.0);

        // (1000-600)/1000 * 100 = 40%
        assertEquals(40.0, rec.getImprovementPercent(), 0.001);
    }

    @Test
    void testGetImprovementPercentNegative() {
        UnifiedRecommendation rec = new UnifiedRecommendation();
        rec.setPreMetricValue(100.0);
        rec.setPostMetricValue(150.0);

        // (100-150)/100 * 100 = -50%
        assertEquals(-50.0, rec.getImprovementPercent(), 0.001);
    }

    @Test
    void testGetImprovementPercentNullPre() {
        UnifiedRecommendation rec = new UnifiedRecommendation();
        rec.setPreMetricValue(null);
        rec.setPostMetricValue(100.0);

        assertNull(rec.getImprovementPercent());
    }

    @Test
    void testGetImprovementPercentNullPost() {
        UnifiedRecommendation rec = new UnifiedRecommendation();
        rec.setPreMetricValue(100.0);
        rec.setPostMetricValue(null);

        assertNull(rec.getImprovementPercent());
    }

    @Test
    void testGetImprovementPercentZeroPre() {
        UnifiedRecommendation rec = new UnifiedRecommendation();
        rec.setPreMetricValue(0.0);
        rec.setPostMetricValue(100.0);

        assertNull(rec.getImprovementPercent());
    }

    @Test
    void testDefaultConstructorValues() {
        UnifiedRecommendation rec = new UnifiedRecommendation();

        assertEquals(UnifiedRecommendation.Status.OPEN, rec.getStatus());
        assertNotNull(rec.getCreatedAt());
    }

    @Test
    void testSettersAndGetters() {
        UnifiedRecommendation rec = new UnifiedRecommendation();

        rec.setId(123L);
        rec.setInstanceId("prod-db");
        rec.setSource(UnifiedRecommendation.Source.INDEX_ADVISOR);
        rec.setRecommendationType("MISSING_INDEX");
        rec.setTitle("Create index on orders.customer_id");
        rec.setDescription("Adding this index will improve query performance");
        rec.setRationale("Table scans are occurring on orders due to missing index");
        rec.setSeverity(UnifiedRecommendation.Severity.HIGH);
        rec.setPriorityScore(85);
        rec.setEstimatedImpact(UnifiedRecommendation.Impact.HIGH);
        rec.setEstimatedEffort(UnifiedRecommendation.Effort.LOW);
        rec.setSuggestedSql("CREATE INDEX idx_orders_customer ON orders(customer_id)");
        rec.setSuggestedConfig(Map.of("work_mem", "256MB"));
        rec.setRollbackSql("DROP INDEX idx_orders_customer");
        rec.setAffectedTables(List.of("orders"));
        rec.setAffectedIndexes(List.of("idx_orders_customer"));
        rec.setAffectedQueries(List.of("SELECT * FROM orders WHERE customer_id = ?"));
        rec.setStatus(UnifiedRecommendation.Status.APPLIED);
        rec.setAppliedBy("dba");
        rec.setDismissedBy("user");
        rec.setDismissReason("Not needed");
        rec.setDeferredUntil(LocalDate.of(2025, 6, 1));
        rec.setPreMetricValue(500.0);
        rec.setPostMetricValue(100.0);
        rec.setEffectivenessRating(UnifiedRecommendation.Effectiveness.EXCELLENT);

        Instant now = Instant.now();
        rec.setCreatedAt(now);
        rec.setAppliedAt(now);
        rec.setDismissedAt(now);

        assertEquals(123L, rec.getId());
        assertEquals("prod-db", rec.getInstanceId());
        assertEquals(UnifiedRecommendation.Source.INDEX_ADVISOR, rec.getSource());
        assertEquals("MISSING_INDEX", rec.getRecommendationType());
        assertEquals("Create index on orders.customer_id", rec.getTitle());
        assertNotNull(rec.getDescription());
        assertNotNull(rec.getRationale());
        assertEquals(UnifiedRecommendation.Severity.HIGH, rec.getSeverity());
        assertEquals(85, rec.getPriorityScore());
        assertEquals(UnifiedRecommendation.Impact.HIGH, rec.getEstimatedImpact());
        assertEquals(UnifiedRecommendation.Effort.LOW, rec.getEstimatedEffort());
        assertNotNull(rec.getSuggestedSql());
        assertNotNull(rec.getSuggestedConfig());
        assertNotNull(rec.getRollbackSql());
        assertNotNull(rec.getAffectedTables());
        assertEquals(1, rec.getAffectedTables().size());
        assertNotNull(rec.getAffectedIndexes());
        assertNotNull(rec.getAffectedQueries());
        assertEquals(UnifiedRecommendation.Status.APPLIED, rec.getStatus());
        assertEquals(now, rec.getCreatedAt());
        assertEquals(now, rec.getAppliedAt());
        assertEquals("dba", rec.getAppliedBy());
        assertEquals(now, rec.getDismissedAt());
        assertEquals("user", rec.getDismissedBy());
        assertEquals("Not needed", rec.getDismissReason());
        assertEquals(LocalDate.of(2025, 6, 1), rec.getDeferredUntil());
        assertEquals(500.0, rec.getPreMetricValue());
        assertEquals(100.0, rec.getPostMetricValue());
        assertEquals(UnifiedRecommendation.Effectiveness.EXCELLENT, rec.getEffectivenessRating());
    }

    @Test
    void testAllSourceEnums() {
        for (UnifiedRecommendation.Source source : UnifiedRecommendation.Source.values()) {
            assertNotNull(source.getDisplayName());
            assertFalse(source.getDisplayName().isEmpty());
            assertNotNull(source.getIcon());
            assertNotNull(source.getDescription());
        }
    }

    @Test
    void testAllSeverityEnums() {
        for (UnifiedRecommendation.Severity severity : UnifiedRecommendation.Severity.values()) {
            assertNotNull(severity.getDisplayName());
            assertNotNull(severity.getCssClass());
            assertTrue(severity.getWeight() >= 1 && severity.getWeight() <= 4);
        }
    }

    @Test
    void testAllStatusEnums() {
        for (UnifiedRecommendation.Status status : UnifiedRecommendation.Status.values()) {
            assertNotNull(status.getDisplayName());
            assertNotNull(status.getCssClass());
            assertNotNull(status.getDescription());
        }
    }

    @Test
    void testAllEffectivenessEnums() {
        for (UnifiedRecommendation.Effectiveness eff : UnifiedRecommendation.Effectiveness.values()) {
            assertNotNull(eff.getDisplayName());
            assertNotNull(eff.getCssClass());
            assertNotNull(eff.getDescription());
        }
    }
}
