package com.bovinemagnet.pgconsole.testutil;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

/**
 * Quarkus test profile for integration tests using Testcontainers.
 * <p>
 * This profile configures the application to use a PostgreSQL container
 * instead of H2, and disables background services that could interfere
 * with testing.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class IntegrationTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        // Check if Docker is available before trying to start container
        if (!PostgresTestContainer.isDockerAvailable()) {
            // Return dummy config - tests will be skipped
            return Map.ofEntries(
                Map.entry("quarkus.datasource.db-kind", "h2"),
                Map.entry("quarkus.datasource.jdbc.url", "jdbc:h2:mem:test"),
                Map.entry("quarkus.datasource.username", "sa"),
                Map.entry("quarkus.datasource.password", ""),
                Map.entry("quarkus.flyway.migrate-at-start", "false"),
                Map.entry("pg-console.history.enabled", "false"),
                Map.entry("pg-console.alerting.enabled", "false"),
                Map.entry("pg-console.security.enabled", "false"),
                Map.entry("quarkus.scheduler.enabled", "false"),
                Map.entry("pg-console.instances", "default"),
                Map.entry("pg-console.instances.default.display-name", "Test Instance"),
                Map.entry("quarkus.log.level", "WARN")
            );
        }

        // Start the container first to get the dynamic port
        var container = PostgresTestContainer.getInstance();

        return Map.ofEntries(
            // Database connection
            Map.entry("quarkus.datasource.db-kind", "postgresql"),
            Map.entry("quarkus.datasource.jdbc.url", container.getJdbcUrl()),
            Map.entry("quarkus.datasource.username", container.getUsername()),
            Map.entry("quarkus.datasource.password", container.getPassword()),

            // Enable Flyway migrations
            Map.entry("quarkus.flyway.migrate-at-start", "true"),
            Map.entry("quarkus.flyway.clean-at-start", "false"),

            // Disable background services
            Map.entry("pg-console.history.enabled", "false"),
            Map.entry("pg-console.alerting.enabled", "false"),
            Map.entry("pg-console.security.enabled", "false"),
            Map.entry("quarkus.scheduler.enabled", "false"),

            // Configure test instance
            Map.entry("pg-console.instances", "default"),
            Map.entry("pg-console.instances.default.display-name", "Test Instance"),

            // Reduce logging noise
            Map.entry("quarkus.log.level", "WARN"),
            Map.entry("quarkus.log.category.\"com.bovinemagnet\".level", "DEBUG")
        );
    }

    @Override
    public String getConfigProfile() {
        return "integration-test";
    }
}
