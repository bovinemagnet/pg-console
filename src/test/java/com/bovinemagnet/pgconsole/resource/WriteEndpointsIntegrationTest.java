package com.bovinemagnet.pgconsole.resource;

import com.bovinemagnet.pgconsole.testutil.DockerAvailableCondition;
import com.bovinemagnet.pgconsole.testutil.IntegrationTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for write/mutation endpoints.
 * <p>
 * These tests verify that POST, PUT, and DELETE endpoints work correctly
 * with a real PostgreSQL database (via Testcontainers).
 * <p>
 * Tests are tagged with 'integration' and require Docker to run.
 * <p>
 * Note: Quarkus does not support @Nested test classes with @TestProfile,
 * so all tests are in the outer class.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@QuarkusTest
@TestProfile(IntegrationTestProfile.class)
@ExtendWith(DockerAvailableCondition.class)
@Tag("integration")
@DisplayName("Write Endpoints Integration Tests")
class WriteEndpointsIntegrationTest {

    private static final String INSTANCE = "default";

    // ========== Schema Comparison Write Endpoints ==========

    @Test
    @DisplayName("POST /schema-comparison/compare - should compare schemas")
    void testSchemaCompare() {
        given()
            .auth().basic("admin", "admin")
            .contentType(ContentType.URLENC)
            .formParam("sourceInstance", INSTANCE)
            .formParam("destInstance", INSTANCE)
            .formParam("sourceSchema", "public")
            .formParam("destSchema", "public")
            .formParam("includeTables", "true")
            .formParam("includeViews", "true")
            .formParam("includeFunctions", "true")
        .when()
            .post("/schema-comparison/compare")
        .then()
            .statusCode(200)
            .contentType(containsString("text/html"));
    }

    @Test
    @DisplayName("POST /schema-comparison/profiles - should create profile")
    void testCreateProfile() {
        String profileName = "test-profile-" + System.currentTimeMillis();

        given()
            .auth().basic("admin", "admin")
            .contentType(ContentType.URLENC)
            .formParam("name", profileName)
            .formParam("description", "Integration test profile")
            .formParam("sourceInstance", INSTANCE)
            .formParam("destInstance", INSTANCE)
            .formParam("sourceSchema", "public")
            .formParam("destSchema", "public")
            .formParam("isDefault", "false")
        .when()
            .post("/schema-comparison/profiles")
        .then()
            .statusCode(200)
            .contentType(containsString("text/html"));
    }

    @Test
    @DisplayName("GET /schema-comparison/export/pdf - should export PDF")
    void testSchemaComparisonExportPdf() {
        given()
            .queryParam("sourceInstance", INSTANCE)
            .queryParam("destInstance", INSTANCE)
            .queryParam("sourceSchema", "public")
            .queryParam("destSchema", "public")
        .when()
            .get("/schema-comparison/export/pdf")
        .then()
            .statusCode(200)
            .contentType("application/pdf")
            .header("Content-Disposition", containsString("attachment"));
    }

    @Test
    @DisplayName("GET /schema-comparison/export/html - should export HTML")
    void testSchemaComparisonExportHtml() {
        given()
            .queryParam("sourceInstance", INSTANCE)
            .queryParam("destInstance", INSTANCE)
            .queryParam("sourceSchema", "public")
            .queryParam("destSchema", "public")
        .when()
            .get("/schema-comparison/export/html")
        .then()
            .statusCode(200)
            .contentType(containsString("text/html"))
            .header("Content-Disposition", containsString("attachment"));
    }

    @Test
    @DisplayName("GET /schema-comparison/export/markdown - should export Markdown")
    void testSchemaComparisonExportMarkdown() {
        given()
            .queryParam("sourceInstance", INSTANCE)
            .queryParam("destInstance", INSTANCE)
            .queryParam("sourceSchema", "public")
            .queryParam("destSchema", "public")
        .when()
            .get("/schema-comparison/export/markdown")
        .then()
            .statusCode(200)
            .contentType(containsString("text/markdown"))
            .header("Content-Disposition", containsString("attachment"));
    }

    // ========== Custom Dashboard Write Endpoints ==========

    @Test
    @DisplayName("POST /dashboards/custom - should create dashboard")
    void testCreateDashboard() {
        String dashboardName = "test-dashboard-" + System.currentTimeMillis();

        given()
            .auth().basic("admin", "admin")
            .contentType(ContentType.URLENC)
            .formParam("name", dashboardName)
            .formParam("description", "Integration test dashboard")
            .formParam("isDefault", "false")
            .formParam("isShared", "false")
        .when()
            .post("/dashboards/custom")
        .then()
            .statusCode(anyOf(is(200), is(303)));
    }

    // ========== Insights Write Endpoints ==========

    @Test
    @DisplayName("POST /insights/refresh - should refresh insights")
    void testRefreshInsights() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .post("/insights/refresh")
        .then()
            .statusCode(200)
            .contentType(containsString("application/json"));
    }

    @Test
    @DisplayName("POST /insights/ask - should parse natural language query")
    void testAskInsights() {
        given()
            .contentType(ContentType.URLENC)
            .formParam("query", "show slow queries")
        .when()
            .post("/insights/ask")
        .then()
            .statusCode(200)
            .contentType(containsString("application/json"));
    }

    // ========== Notification Write Endpoints ==========

    @Test
    @DisplayName("POST /notifications/api/channels - should create channel")
    void testCreateChannel() {
        String channelName = "test-channel-" + System.currentTimeMillis();
        String json = String.format(
            "{\"name\":\"%s\",\"type\":\"WEBHOOK\",\"enabled\":true," +
            "\"config\":{\"url\":\"http://localhost:9999/test\"}}",
            channelName);

        given()
            .auth().basic("admin", "admin")
            .contentType(ContentType.JSON)
            .body(json)
        .when()
            .post("/notifications/api/channels")
        .then()
            .statusCode(201)
            .contentType(containsString("application/json"));
    }

    @Test
    @DisplayName("POST /notifications/api/silences - should create silence")
    void testCreateSilence() {
        String json = "{\"alertType\":\"HIGH_CONNECTIONS\"," +
            "\"instancePattern\":\"*\"," +
            "\"reason\":\"Integration test silence\"," +
            "\"expiresAt\":\"2099-12-31T23:59:59Z\"}";

        given()
            .auth().basic("admin", "admin")
            .contentType(ContentType.JSON)
            .body(json)
        .when()
            .post("/notifications/api/silences")
        .then()
            .statusCode(201)
            .contentType(containsString("application/json"));
    }

    @Test
    @DisplayName("POST /notifications/api/silences/quick - should create quick silence")
    void testCreateQuickSilence() {
        given()
            .auth().basic("admin", "admin")
            .contentType(ContentType.URLENC)
            .formParam("alertType", "HIGH_CONNECTIONS")
            .formParam("instanceName", INSTANCE)
            .formParam("durationMinutes", "5")
        .when()
            .post("/notifications/api/silences/quick")
        .then()
            .statusCode(201);
    }

    @Test
    @DisplayName("POST /notifications/api/maintenance - should create maintenance window")
    void testCreateMaintenanceWindow() {
        String windowName = "test-maintenance-" + System.currentTimeMillis();
        String json = String.format(
            "{\"name\":\"%s\",\"instancePattern\":\"*\"," +
            "\"startTime\":\"2099-01-01T00:00:00Z\"," +
            "\"endTime\":\"2099-01-01T01:00:00Z\"," +
            "\"reason\":\"Integration test maintenance\"}",
            windowName);

        given()
            .auth().basic("admin", "admin")
            .contentType(ContentType.JSON)
            .body(json)
        .when()
            .post("/notifications/api/maintenance")
        .then()
            .statusCode(201)
            .contentType(containsString("application/json"));
    }

    @Test
    @DisplayName("POST /notifications/api/escalation - should create escalation policy")
    void testCreateEscalationPolicy() {
        String policyName = "test-escalation-" + System.currentTimeMillis();
        String json = String.format(
            "{\"name\":\"%s\",\"alertTypes\":[\"HIGH_CONNECTIONS\"]," +
            "\"levels\":[{\"delayMinutes\":5,\"channelIds\":[]}]}",
            policyName);

        given()
            .auth().basic("admin", "admin")
            .contentType(ContentType.JSON)
            .body(json)
        .when()
            .post("/notifications/api/escalation")
        .then()
            .statusCode(201)
            .contentType(containsString("application/json"));
    }

    // ========== Logging Control Endpoints ==========

    @Test
    @DisplayName("POST /api/v1/logging/debug - should enable debug mode")
    void testEnableDebugMode() {
        given()
            .auth().basic("admin", "admin")
            .contentType(ContentType.JSON)
            .queryParam("duration", "1")
        .when()
            .post("/api/v1/logging/debug")
        .then()
            .statusCode(200)
            .contentType(containsString("application/json"));
    }

    @Test
    @DisplayName("DELETE /api/v1/logging/debug - should disable debug mode")
    void testDisableDebugMode() {
        given()
            .auth().basic("admin", "admin")
        .when()
            .delete("/api/v1/logging/debug")
        .then()
            .statusCode(200)
            .contentType(containsString("application/json"));
    }

    @Test
    @DisplayName("POST /api/v1/logging/preset/{preset} - should apply logging preset")
    void testApplyLoggingPreset() {
        given()
            .auth().basic("admin", "admin")
            .contentType(ContentType.JSON)
        .when()
            .post("/api/v1/logging/preset/INFO")
        .then()
            .statusCode(200)
            .contentType(containsString("application/json"));
    }

    // ========== EXPLAIN Query Endpoint ==========

    @Test
    @DisplayName("POST /api/explain - should generate execution plan")
    void testExplainQuery() {
        given()
            .contentType(ContentType.URLENC)
            .formParam("query", "SELECT 1")
            .formParam("analyse", "false")
            .formParam("buffers", "false")
            .queryParam("instance", INSTANCE)
        .when()
            .post("/api/explain")
        .then()
            .statusCode(200)
            .contentType(containsString("text/html"));
    }
}
