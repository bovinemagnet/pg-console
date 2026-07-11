package com.bovinemagnet.pgconsole.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Verifies that enabling {@code pg-console.security.enabled} actually gates
 * every dashboard, API and export endpoint, not just the activity
 * cancel/terminate actions.
 * <p>
 * Uses H2 so no database round-trip is needed: a 401 is issued by the
 * HTTP security layer before any resource code runs.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@QuarkusTest
@TestProfile(SecurityGatingTest.SecurityEnabledProfile.class)
@DisplayName("Security gating (pg-console.security.enabled=true)")
class SecurityGatingTest {

    public static class SecurityEnabledProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.ofEntries(
                Map.entry("quarkus.datasource.db-kind", "h2"),
                Map.entry("quarkus.datasource.jdbc.url", "jdbc:h2:mem:sectest;DB_CLOSE_DELAY=-1"),
                Map.entry("quarkus.datasource.username", "sa"),
                Map.entry("quarkus.datasource.password", ""),
                Map.entry("quarkus.flyway.migrate-at-start", "false"),
                Map.entry("pg-console.instances", "default"),
                Map.entry("pg-console.history.enabled", "false"),
                Map.entry("pg-console.alerting.enabled", "false"),
                Map.entry("quarkus.scheduler.enabled", "false"),
                Map.entry("pg-console.security.enabled", "true"),
                Map.entry("quarkus.security.users.file.enabled", "true"),
                Map.entry("quarkus.security.users.file.plain-text", "true"),
                Map.entry("quarkus.security.users.file.users", "users.properties"),
                Map.entry("quarkus.security.users.file.roles", "roles.properties")
            );
        }
    }

    @Test
    @DisplayName("Anonymous request to a dashboard page returns 401")
    void anonymousDashboardPageIsRejected() {
        given().when().get("/").then().statusCode(401);
    }

    @Test
    @DisplayName("Anonymous request to an API endpoint returns 401")
    void anonymousApiIsRejected() {
        given().when().get("/api/overview").then().statusCode(401);
    }

    @Test
    @DisplayName("Anonymous request to a CSV export returns 401")
    void anonymousExportIsRejected() {
        given().when().get("/activity/export").then().statusCode(401);
    }

    @Test
    @DisplayName("Anonymous POST to activity terminate returns 401")
    void anonymousTerminateIsRejected() {
        given().when().post("/api/activity/1/terminate").then().statusCode(401);
    }

    @Test
    @DisplayName("Anonymous request to a static asset returns 401")
    void anonymousStaticAssetIsRejected() {
        given().when().get("/manifest.json").then().statusCode(401);
    }

    @Test
    @DisplayName("Authenticated request to a static asset succeeds")
    void authenticatedStaticAssetSucceeds() {
        given().auth().basic("admin", "admin")
            .when().get("/manifest.json")
            .then().statusCode(200);
    }
}
