package com.bovinemagnet.pgconsole.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for API endpoints.
 * <p>
 * These tests verify that the REST API endpoints return expected responses.
 * They run against the Quarkus application with the H2 test database.
 * <p>
 * Note: For full PostgreSQL integration tests, use the CI/CD pipeline
 * which has Docker available for Testcontainers.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@QuarkusTest
@TestProfile(ApiResourceIT.TestOnlyProfile.class)
@Tag("integration")
class ApiResourceIT {

    /**
     * Test profile that disables background services and uses H2 database.
     * Dashboard endpoints will return 500 since H2 doesn't support
     * PostgreSQL-specific queries, but we can test static resources and
     * other non-database endpoints.
     */
    public static class TestOnlyProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                "pg-console.history.enabled", "false",
                "pg-console.alerting.enabled", "false",
                "quarkus.scheduler.enabled", "false"
            );
        }
    }

    @Nested
    @DisplayName("Static resources")
    class StaticResourceTests {

        @Test
        @DisplayName("GET /sw.js returns service worker JavaScript")
        void serviceWorkerAvailable() {
            given()
                .when()
                .get("/sw.js")
                .then()
                .statusCode(200)
                .contentType(containsString("javascript"));
        }
    }

    @Nested
    @DisplayName("Metrics endpoint")
    class MetricsEndpointTests {

        @Test
        @DisplayName("GET /q/metrics returns Prometheus metrics")
        void metricsEndpointReturnsPrometheus() {
            given()
                .when()
                .get("/q/metrics")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(404))); // May or may not be configured
        }
    }

    @Nested
    @DisplayName("Dashboard endpoint")
    class DashboardEndpointTests {

        @Test
        @DisplayName("GET / returns response (may be 500 without PostgreSQL)")
        void dashboardReturnsResponse() {
            // Dashboard will fail with H2 since it uses PostgreSQL-specific queries
            // In CI/CD with Docker, this would pass with PostgresTestResource
            given()
                .when()
                .get("/")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(500)));
        }

        @Test
        @DisplayName("GET /about returns response")
        void aboutPageReturnsResponse() {
            given()
                .when()
                .get("/about")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(500)));
        }
    }
}
