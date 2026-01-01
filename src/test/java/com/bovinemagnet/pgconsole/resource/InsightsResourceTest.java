package com.bovinemagnet.pgconsole.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for InsightsResource endpoints.
 * <p>
 * Tests basic endpoint accessibility and response types.
 * Full integration tests require a PostgreSQL database.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@QuarkusTest
@TestProfile(InsightsResourceTest.TestConfig.class)
class InsightsResourceTest {

    public static class TestConfig implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.ofEntries(
                    Map.entry("quarkus.datasource.db-kind", "h2"),
                    Map.entry("quarkus.datasource.jdbc.url", "jdbc:h2:mem:insights_test;DB_CLOSE_DELAY=-1"),
                    Map.entry("quarkus.datasource.username", "sa"),
                    Map.entry("quarkus.datasource.password", ""),
                    Map.entry("quarkus.flyway.migrate-at-start", "false"),
                    Map.entry("pg-console.instances", "default"),
                    Map.entry("pg-console.history.enabled", "false"),
                    Map.entry("pg-console.alerting.enabled", "false"),
                    Map.entry("pg-console.security.enabled", "false"),
                    Map.entry("pg-console.dashboards.insights.enabled", "true"),
                    Map.entry("pg-console.dashboards.insights.dashboard", "true"),
                    Map.entry("pg-console.dashboards.insights.anomalies", "true"),
                    Map.entry("pg-console.dashboards.insights.forecasts", "true"),
                    Map.entry("pg-console.dashboards.insights.recommendations", "true"),
                    Map.entry("pg-console.dashboards.insights.runbooks", "true"),
                    Map.entry("quarkus.scheduler.enabled", "false")
            );
        }
    }

    // Note: These tests verify endpoints are accessible.
    // Full functionality requires PostgreSQL database.

    @Test
    void testExplainTermEndpoint() {
        given()
                .when().get("/insights/explain/cache hit ratio")
                .then()
                .statusCode(200)
                .contentType(containsString("application/json"))
                .body("term", equalTo("cache hit ratio"))
                .body("explanation", notNullValue());
    }

    @Test
    void testExplainTermUnknown() {
        given()
                .when().get("/insights/explain/unknown_term_xyz")
                .then()
                .statusCode(200)
                .contentType(containsString("application/json"))
                .body("explanation", containsString("No explanation available"));
    }

    @Test
    void testExplainTermDeadTuples() {
        given()
                .when().get("/insights/explain/dead tuples")
                .then()
                .statusCode(200)
                .body("explanation", containsString("deleted"));
    }

    @Test
    void testExplainTermBlocking() {
        given()
                .when().get("/insights/explain/blocking")
                .then()
                .statusCode(200)
                .body("explanation", containsString("waiting"));
    }

    @Test
    void testExplainTermReplicationLag() {
        given()
                .when().get("/insights/explain/replication lag")
                .then()
                .statusCode(200)
                .body("explanation", containsString("delay"));
    }

    @Test
    void testExplainTermSharedBuffers() {
        given()
                .when().get("/insights/explain/shared_buffers")
                .then()
                .statusCode(200)
                .body("explanation", containsString("memory"));
    }

    @Test
    void testExplainTermWorkMem() {
        given()
                .when().get("/insights/explain/work_mem")
                .then()
                .statusCode(200)
                .body("explanation", containsString("sorting"));
    }

    @Test
    void testExplainTermAutovacuum() {
        given()
                .when().get("/insights/explain/autovacuum")
                .then()
                .statusCode(200)
                .body("explanation", containsString("automatic"));
    }

    @Test
    void testExplainTermSeqScan() {
        given()
                .when().get("/insights/explain/seq scan")
                .then()
                .statusCode(200)
                .body("explanation", containsString("sequential"));
    }

    @Test
    void testAskQuestionEndpointSlowQueries() {
        given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("query", "show slow queries")
                .when().post("/insights/ask?instance=default")
                .then()
                .statusCode(200)
                .contentType(containsString("application/json"))
                .body("query", equalTo("show slow queries"))
                .body("understood", equalTo(true))
                .body("intent", equalTo("SLOW_QUERIES"))
                .body("redirectUrl", containsString("/slow-queries"));
    }

    @Test
    void testAskQuestionEndpointWhyDatabaseSlow() {
        given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("query", "why is the database slow")
                .when().post("/insights/ask?instance=default")
                .then()
                .statusCode(200)
                .body("understood", equalTo(true))
                .body("intent", equalTo("SLOW_DIAGNOSIS"));
    }

    @Test
    void testAskQuestionEndpointLocks() {
        given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("query", "show current locks")
                .when().post("/insights/ask?instance=default")
                .then()
                .statusCode(200)
                .body("understood", equalTo(true))
                .body("intent", equalTo("LOCKS"))
                .body("redirectUrl", containsString("/locks"));
    }

    @Test
    void testAskQuestionEndpointActivity() {
        given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("query", "active connections")
                .when().post("/insights/ask?instance=default")
                .then()
                .statusCode(200)
                .body("understood", equalTo(true))
                .body("intent", equalTo("ACTIVITY"))
                .body("redirectUrl", containsString("/activity"));
    }

    @Test
    void testAskQuestionEndpointTableGrowth() {
        given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("query", "which tables are growing fastest")
                .when().post("/insights/ask?instance=default")
                .then()
                .statusCode(200)
                .body("understood", equalTo(true))
                .body("intent", equalTo("TABLE_GROWTH"));
    }

    @Test
    void testAskQuestionEndpointAnomalies() {
        given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("query", "show anomalies")
                .when().post("/insights/ask?instance=default")
                .then()
                .statusCode(200)
                .body("understood", equalTo(true))
                .body("intent", equalTo("ANOMALIES"));
    }

    @Test
    void testAskQuestionEndpointForecasts() {
        given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("query", "when will storage be full")
                .when().post("/insights/ask?instance=default")
                .then()
                .statusCode(200)
                .body("understood", equalTo(true))
                .body("intent", equalTo("FORECASTS"));
    }

    @Test
    void testAskQuestionEndpointRecommendations() {
        given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("query", "what should I do")
                .when().post("/insights/ask?instance=default")
                .then()
                .statusCode(200)
                .body("understood", equalTo(true))
                .body("intent", equalTo("RECOMMENDATIONS"));
    }

    @Test
    void testAskQuestionEndpointIndexAdvisor() {
        given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("query", "missing indexes")
                .when().post("/insights/ask?instance=default")
                .then()
                .statusCode(200)
                .body("understood", equalTo(true))
                .body("intent", equalTo("INDEX_ADVISOR"));
    }

    @Test
    void testAskQuestionEndpointVacuum() {
        given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("query", "dead tuples")
                .when().post("/insights/ask?instance=default")
                .then()
                .statusCode(200)
                .body("understood", equalTo(true))
                .body("intent", equalTo("VACUUM"));
    }

    @Test
    void testAskQuestionEndpointUnknown() {
        given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("query", "xyzzy gobbledygook")
                .when().post("/insights/ask?instance=default")
                .then()
                .statusCode(200)
                .body("understood", equalTo(false))
                .body("intent", equalTo("UNKNOWN"));
    }

    @Test
    void testAskQuestionEndpointEmpty() {
        given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("query", "")
                .when().post("/insights/ask?instance=default")
                .then()
                .statusCode(200)
                .body("understood", equalTo(false));
    }

    @Test
    void testAskQuestionConfidenceScore() {
        given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("query", "slow queries yesterday")
                .when().post("/insights/ask?instance=default")
                .then()
                .statusCode(200)
                .body("confidence", greaterThan(0.0f));
    }

    @Test
    void testAskQuestionCaseInsensitive() {
        given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("query", "SLOW QUERIES")
                .when().post("/insights/ask?instance=default")
                .then()
                .statusCode(200)
                .body("understood", equalTo(true))
                .body("intent", equalTo("SLOW_QUERIES"));
    }

    @Test
    void testAskQuestionBlockedQueries() {
        given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("query", "blocked queries")
                .when().post("/insights/ask?instance=default")
                .then()
                .statusCode(200)
                .body("understood", equalTo(true))
                .body("intent", equalTo("BLOCKING"));
    }

    @Test
    void testAskQuestionReplicationStatus() {
        given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("query", "replication status")
                .when().post("/insights/ask?instance=default")
                .then()
                .statusCode(200)
                .body("understood", equalTo(true))
                .body("intent", equalTo("REPLICATION"));
    }

    @Test
    void testAskQuestionStorageUsage() {
        given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("query", "disk usage")
                .when().post("/insights/ask?instance=default")
                .then()
                .statusCode(200)
                .body("understood", equalTo(true))
                .body("intent", equalTo("STORAGE"));
    }
}
