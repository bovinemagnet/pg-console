package com.bovinemagnet.pgconsole.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Tests for PWA static resources (manifest.json, service worker, icons).
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@QuarkusTest
@TestProfile(DashboardResourceTest.TestConfig.class)
class StaticResourcesTest {

    @Test
    void testManifestJsonExists() {
        given()
            .when().get("/manifest.json")
            .then()
                .statusCode(200)
                .contentType(containsString("application/json"));
    }

    @Test
    void testManifestJsonContent() {
        given()
            .when().get("/manifest.json")
            .then()
                .statusCode(200)
                .body("name", equalTo("PostgreSQL Console"))
                .body("short_name", equalTo("PG Console"))
                .body("display", equalTo("standalone"))
                .body("theme_color", equalTo("#0d6efd"))
                .body("background_color", equalTo("#1a1d21"))
                .body("start_url", equalTo("/"))
                .body("icons", hasSize(greaterThan(0)));
    }

    @Test
    void testManifestJsonHasIcons() {
        given()
            .when().get("/manifest.json")
            .then()
                .statusCode(200)
                .body("icons.find { it.sizes == 'any' }.src", equalTo("/icons/icon.svg"))
                .body("icons.find { it.sizes == '192x192' }.src", equalTo("/icons/icon-192.png"))
                .body("icons.find { it.sizes == '512x512' }.src", equalTo("/icons/icon-512.png"));
    }

    @Test
    void testManifestJsonHasShortcuts() {
        given()
            .when().get("/manifest.json")
            .then()
                .statusCode(200)
                .body("shortcuts", hasSize(3))
                .body("shortcuts[0].name", equalTo("Dashboard"))
                .body("shortcuts[0].url", equalTo("/"))
                .body("shortcuts[1].name", equalTo("Active Sessions"))
                .body("shortcuts[1].url", equalTo("/activity"))
                .body("shortcuts[2].name", equalTo("Slow Queries"))
                .body("shortcuts[2].url", equalTo("/slow-queries"));
    }

    @Test
    void testServiceWorkerExists() {
        given()
            .when().get("/sw.js")
            .then()
                .statusCode(200)
                .contentType(containsString("javascript"));
    }

    @Test
    void testServiceWorkerContent() {
        given()
            .when().get("/sw.js")
            .then()
                .statusCode(200)
                .body(containsString("CACHE_NAME"))
                .body(containsString("install"))
                .body(containsString("activate"))
                .body(containsString("fetch"));
    }

    @Test
    void testServiceWorkerHasOfflineSupport() {
        given()
            .when().get("/sw.js")
            .then()
                .statusCode(200)
                .body(containsString("offlineFallback"))
                .body(containsString("cacheFirst"))
                .body(containsString("networkFirst"))
                .body(containsString("staleWhileRevalidate"));
    }

    @Test
    void testSvgIconExists() {
        given()
            .when().get("/icons/icon.svg")
            .then()
                .statusCode(200)
                .contentType(containsString("svg"));
    }

    @Test
    void testSvgIconContent() {
        given()
            .when().get("/icons/icon.svg")
            .then()
                .statusCode(200)
                .body(containsString("<svg"))
                .body(containsString("viewBox"))
                .body(containsString("</svg>"));
    }

    @Test
    void testPngIcon72Exists() {
        given()
            .when().get("/icons/icon-72.png")
            .then()
                .statusCode(200)
                .contentType(containsString("image/png"));
    }

    @Test
    void testPngIcon192Exists() {
        given()
            .when().get("/icons/icon-192.png")
            .then()
                .statusCode(200)
                .contentType(containsString("image/png"));
    }

    @Test
    void testPngIcon512Exists() {
        given()
            .when().get("/icons/icon-512.png")
            .then()
                .statusCode(200)
                .contentType(containsString("image/png"));
    }

    @Test
    void testAllPngIconsExist() {
        int[] sizes = {72, 96, 128, 144, 152, 192, 384, 512};
        for (int size : sizes) {
            given()
                .when().get("/icons/icon-" + size + ".png")
                .then()
                    .statusCode(200)
                    .contentType(containsString("image/png"));
        }
    }
}
