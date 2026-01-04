package com.bovinemagnet.pgconsole.service;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Service for creating connections to different databases on PostgreSQL instances.
 * <p>
 * Enables cross-database schema comparisons by creating ad-hoc connections
 * to arbitrary databases using the same credentials as the configured instances.
 * <p>
 * This service creates non-pooled connections that must be explicitly closed
 * by the caller using try-with-resources.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class CrossDatabaseConnectionService {

    private static final Logger LOG = Logger.getLogger(CrossDatabaseConnectionService.class);

    @Inject
    DataSourceManager dataSourceManager;

    /**
     * Lists all accessible databases on an instance.
     * <p>
     * Excludes template databases (template0, template1) and databases
     * that do not allow connections.
     *
     * @param instanceName the instance to query
     * @return list of database names, sorted alphabetically
     */
    public List<String> listDatabases(String instanceName) {
        List<String> databases = new ArrayList<>();
        String sql = """
            SELECT datname
            FROM pg_database
            WHERE datistemplate = false
              AND datallowconn = true
            ORDER BY datname
            """;

        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                databases.add(rs.getString("datname"));
            }
        } catch (SQLException e) {
            LOG.warnf("Failed to list databases for instance %s: %s", instanceName, e.getMessage());
        }

        return databases;
    }

    /**
     * Creates a connection to a specific database on the given instance.
     * <p>
     * This creates a new, non-pooled connection by modifying the JDBC URL
     * of the configured datasource to point to a different database.
     * The returned connection must be closed by the caller.
     *
     * @param instanceName the instance name (used to get base connection properties)
     * @param databaseName the target database name
     * @return a new connection to the specified database
     * @throws SQLException if connection cannot be established
     */
    public Connection getConnectionToDatabase(String instanceName, String databaseName) throws SQLException {
        javax.sql.DataSource ds = dataSourceManager.getDataSource(instanceName);

        // Get connection properties from the datasource
        try (Connection baseConn = ds.getConnection()) {
            DatabaseMetaData meta = baseConn.getMetaData();
            String baseUrl = meta.getURL();
            String username = meta.getUserName();

            // Check if we're already connected to the target database
            String currentDb = baseConn.getCatalog();
            if (databaseName.equals(currentDb)) {
                LOG.debugf("Already connected to %s, reusing connection properties", databaseName);
            }

            // Modify the JDBC URL to point to the target database
            String newUrl = replaceDatabase(baseUrl, databaseName);

            LOG.debugf("Creating connection to database '%s' on instance '%s' (URL: %s)",
                    databaseName, instanceName, sanitiseUrl(newUrl));

            // Get password from Agroal configuration if available
            String password = getPasswordFromDataSource(ds);

            Properties props = new Properties();
            props.setProperty("user", username);
            if (password != null) {
                props.setProperty("password", password);
            }

            return DriverManager.getConnection(newUrl, props);
        }
    }

    /**
     * Replaces the database name in a JDBC URL.
     * <p>
     * Handles URLs in the format:
     * <ul>
     *   <li>jdbc:postgresql://host:port/database</li>
     *   <li>jdbc:postgresql://host:port/database?params</li>
     *   <li>jdbc:postgresql://host/database</li>
     * </ul>
     *
     * @param jdbcUrl the original JDBC URL
     * @param newDatabase the new database name to use
     * @return the modified JDBC URL
     */
    private String replaceDatabase(String jdbcUrl, String newDatabase) {
        // Handle: jdbc:postgresql://host:port/database?params
        // or: jdbc:postgresql://host:port/database
        int dbStartIndex = jdbcUrl.lastIndexOf('/') + 1;
        int queryIndex = jdbcUrl.indexOf('?', dbStartIndex);

        if (queryIndex > 0) {
            return jdbcUrl.substring(0, dbStartIndex) + newDatabase + jdbcUrl.substring(queryIndex);
        } else {
            return jdbcUrl.substring(0, dbStartIndex) + newDatabase;
        }
    }

    /**
     * Attempts to retrieve the password from an Agroal datasource.
     * <p>
     * This is a best-effort approach since passwords are typically
     * not exposed directly. Falls back to null if password cannot be retrieved.
     *
     * @param ds the datasource
     * @return the password, or null if not available
     */
    private String getPasswordFromDataSource(javax.sql.DataSource ds) {
        try {
            if (ds instanceof AgroalDataSource agroalDs) {
                AgroalConnectionPoolConfiguration poolConfig = agroalDs.getConfiguration().connectionPoolConfiguration();
                var factoryConfig = poolConfig.connectionFactoryConfiguration();

                // Try to get credentials from the factory configuration
                var principal = factoryConfig.principal();
                var credentials = factoryConfig.credentials();

                if (credentials != null && !credentials.isEmpty()) {
                    // Credentials is a collection, get the first one
                    var cred = credentials.iterator().next();
                    if (cred instanceof io.agroal.api.security.SimplePassword simplePassword) {
                        return new String(simplePassword.getWord());
                    }
                }
            }
        } catch (Exception e) {
            LOG.debugf("Could not extract password from datasource: %s", e.getMessage());
        }

        return null;
    }

    /**
     * Sanitises a JDBC URL for logging by removing password if present.
     *
     * @param url the JDBC URL
     * @return sanitised URL safe for logging
     */
    private String sanitiseUrl(String url) {
        if (url == null) return "null";
        // Remove password parameter if present
        return url.replaceAll("password=[^&]*", "password=***");
    }

    /**
     * Gets the current database name for an instance.
     *
     * @param instanceName the instance name
     * @return the current database name, or null if cannot be determined
     */
    public String getCurrentDatabase(String instanceName) {
        try (Connection conn = dataSourceManager.getDataSource(instanceName).getConnection()) {
            return conn.getCatalog();
        } catch (SQLException e) {
            LOG.warnf("Failed to get current database for instance %s: %s", instanceName, e.getMessage());
            return null;
        }
    }

    /**
     * Tests connectivity to a specific database on an instance.
     *
     * @param instanceName the instance name
     * @param databaseName the database to test
     * @return true if connection successful, false otherwise
     */
    public boolean testDatabaseConnection(String instanceName, String databaseName) {
        try (Connection conn = getConnectionToDatabase(instanceName, databaseName)) {
            return conn.isValid(5);
        } catch (SQLException e) {
            LOG.debugf("Connection test failed for %s.%s: %s", instanceName, databaseName, e.getMessage());
            return false;
        }
    }
}
