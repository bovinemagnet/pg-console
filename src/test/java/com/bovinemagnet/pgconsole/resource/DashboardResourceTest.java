package com.bovinemagnet.pgconsole.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for DashboardResource endpoints.
 * Tests basic endpoint responses and content types.
 * <p>
 * Note: These tests focus on verifying that endpoints are accessible
 * and return appropriate content types. Full integration tests require
 * a PostgreSQL database connection.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@QuarkusTest
@TestProfile(DashboardResourceTest.TestConfig.class)
class DashboardResourceTest {

    public static class TestConfig implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.ofEntries(
                Map.entry("quarkus.datasource.db-kind", "h2"),
                Map.entry("quarkus.datasource.jdbc.url", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"),
                Map.entry("quarkus.datasource.username", "sa"),
                Map.entry("quarkus.datasource.password", ""),
                Map.entry("quarkus.flyway.migrate-at-start", "false"),
                Map.entry("pg-console.instances", "default"),
                Map.entry("pg-console.history.enabled", "false"),
                Map.entry("pg-console.alerting.enabled", "false"),
                Map.entry("pg-console.security.enabled", "false"),
                Map.entry("quarkus.scheduler.enabled", "false")
            );
        }
    }

    // Note: Database-dependent tests are disabled because H2 doesn't support PostgreSQL functions
    // These tests require a real PostgreSQL database
    //
    // @Test
    // void testAboutPageReturnsHtml() { ... }
    //
    // @Test
    // void testAboutPageContainsAppInfo() { ... }

    @Test
    void testManifestJsonAvailable() {
        given()
            .when().get("/manifest.json")
            .then()
                .statusCode(200)
                .contentType(containsString("application/json"));
    }

    @Test
    void testServiceWorkerAvailable() {
        given()
            .when().get("/sw.js")
            .then()
                .statusCode(200)
                .contentType(containsString("javascript"));
    }

    @Test
    void testSvgIconAvailable() {
        given()
            .when().get("/icons/icon.svg")
            .then()
                .statusCode(200);
    }

    @Test
    void testPngIconsAvailable() {
        int[] sizes = {72, 96, 128, 144, 152, 192, 384, 512};
        for (int size : sizes) {
            given()
                .when().get("/icons/icon-" + size + ".png")
                .then()
                    .statusCode(200);
        }
    }
}
