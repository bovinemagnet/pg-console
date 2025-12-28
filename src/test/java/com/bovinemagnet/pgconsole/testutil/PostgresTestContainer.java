package com.bovinemagnet.pgconsole.testutil;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Shared PostgreSQL test container for integration tests.
 * <p>
 * This container is started once and reused across all integration tests
 * to improve test execution speed. It initialises with pg_stat_statements
 * extension and the pgconsole schema.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public final class PostgresTestContainer {

    private static final String POSTGRES_IMAGE = "postgres:15-alpine";
    private static final String DATABASE_NAME = "testdb";
    private static final String USERNAME = "test";
    private static final String PASSWORD = "test";

    private static PostgreSQLContainer<?> container;

    private PostgresTestContainer() {
        // Utility class - prevent instantiation
    }

    /**
     * Gets or creates the shared PostgreSQL container.
     * <p>
     * The container is lazily initialised on first access and reused
     * for all subsequent calls. It includes the pg_stat_statements
     * extension which is required for slow query analysis.
     *
     * @return the shared PostgreSQL container
     */
    public static synchronized PostgreSQLContainer<?> getInstance() {
        if (container == null) {
            container = new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE))
                .withDatabaseName(DATABASE_NAME)
                .withUsername(USERNAME)
                .withPassword(PASSWORD)
                .withCommand("postgres", "-c", "shared_preload_libraries=pg_stat_statements")
                .withInitScript("init-test-db.sql");
            container.start();
        }
        return container;
    }

    /**
     * Gets the JDBC URL for the running container.
     *
     * @return the JDBC URL
     */
    public static String getJdbcUrl() {
        return getInstance().getJdbcUrl();
    }

    /**
     * Gets the database username.
     *
     * @return the username
     */
    public static String getUsername() {
        return getInstance().getUsername();
    }

    /**
     * Gets the database password.
     *
     * @return the password
     */
    public static String getPassword() {
        return getInstance().getPassword();
    }

    /**
     * Creates a new Connection to the test container.
     * <p>
     * This is useful for tests that need direct database access
     * without going through the Quarkus CDI container.
     *
     * @return a new database Connection
     * @throws SQLException if connection fails
     */
    public static Connection createConnection() throws SQLException {
        return DriverManager.getConnection(getJdbcUrl(), getUsername(), getPassword());
    }

    /**
     * Checks if the container is running.
     *
     * @return true if container is running
     */
    public static boolean isRunning() {
        return container != null && container.isRunning();
    }
}
