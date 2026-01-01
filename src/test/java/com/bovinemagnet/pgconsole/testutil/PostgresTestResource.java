package com.bovinemagnet.pgconsole.testutil;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.jboss.logging.Logger;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Quarkus test resource that manages a PostgreSQL Testcontainer.
 * <p>
 * This resource starts a PostgreSQL container with pg_stat_statements extension
 * before tests run and provides the connection configuration to Quarkus.
 * Use with @QuarkusTestResource(PostgresTestResource.class).
 * <p>
 * If Docker is not available, the resource returns empty configuration and
 * tests should be skipped or use fallback configuration.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class PostgresTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOG = Logger.getLogger(PostgresTestResource.class);

    private PostgreSQLContainer<?> postgres;
    private boolean dockerAvailable = false;

    @Override
    public Map<String, String> start() {
        // Check if Docker is available
        try {
            dockerAvailable = DockerClientFactory.instance().isDockerAvailable();
        } catch (Exception e) {
            LOG.warn("Docker availability check failed: " + e.getMessage());
            dockerAvailable = false;
        }

        if (!dockerAvailable) {
            LOG.warn("Docker is not available. PostgreSQL Testcontainer will not be started. " +
                     "Integration tests requiring real PostgreSQL will be skipped.");
            return Collections.emptyMap();
        }

        try {
            postgres = new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test")
                    .withCommand("postgres", "-c", "shared_preload_libraries=pg_stat_statements");

            postgres.start();

            Map<String, String> config = new HashMap<>();

            // Configure the default datasource for Quarkus
            config.put("quarkus.datasource.db-kind", "postgresql");
            config.put("quarkus.datasource.jdbc.url", postgres.getJdbcUrl());
            config.put("quarkus.datasource.username", postgres.getUsername());
            config.put("quarkus.datasource.password", postgres.getPassword());

            // Configure the pg-console instance datasource
            config.put("pg-console.instances", "default");
            config.put("pg-console.instance.default.display-name", "Test Instance");
            config.put("pg-console.instance.default.jdbc-url", postgres.getJdbcUrl());
            config.put("pg-console.instance.default.username", postgres.getUsername());
            config.put("pg-console.instance.default.password", postgres.getPassword());

            // Disable background services
            config.put("pg-console.history.enabled", "false");
            config.put("pg-console.alerting.enabled", "false");
            config.put("quarkus.scheduler.enabled", "false");
            config.put("quarkus.flyway.migrate-at-start", "false");

            LOG.info("PostgreSQL Testcontainer started successfully at: " + postgres.getJdbcUrl());
            return config;
        } catch (Exception e) {
            LOG.error("Failed to start PostgreSQL Testcontainer: " + e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    @Override
    public void stop() {
        if (postgres != null && postgres.isRunning()) {
            postgres.stop();
            LOG.info("PostgreSQL Testcontainer stopped.");
        }
    }

    /**
     * Check if Docker is available for running Testcontainers.
     *
     * @return true if Docker is available, false otherwise
     */
    public static boolean isDockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the JDBC URL for the running PostgreSQL container.
     *
     * @return the JDBC URL, or null if container is not running
     */
    public String getJdbcUrl() {
        return postgres != null ? postgres.getJdbcUrl() : null;
    }

    /**
     * Get the username for the running PostgreSQL container.
     *
     * @return the username, or null if container is not running
     */
    public String getUsername() {
        return postgres != null ? postgres.getUsername() : null;
    }

    /**
     * Get the password for the running PostgreSQL container.
     *
     * @return the password, or null if container is not running
     */
    public String getPassword() {
        return postgres != null ? postgres.getPassword() : null;
    }
}
