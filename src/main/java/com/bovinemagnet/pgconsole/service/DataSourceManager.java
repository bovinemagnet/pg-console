package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.config.InstanceConfig;
import com.bovinemagnet.pgconsole.model.InstanceInfo;
import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages multiple PostgreSQL datasources for multi-instance support.
 * <p>
 * The "default" instance uses the unnamed Quarkus datasource.
 * Named instances use Quarkus named datasources matching the instance name.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class DataSourceManager {

    private static final Logger LOG = Logger.getLogger(DataSourceManager.class);

    @Inject
    AgroalDataSource defaultDataSource;

    @Inject
    InstanceConfig instanceConfig;

    private final Map<String, AgroalDataSource> dataSourceCache = new ConcurrentHashMap<>();
    private List<String> availableInstances;

    /**
     * Initialises the DataSourceManager after construction.
     * <p>
     * Parses the configured instance names from the instance configuration
     * and caches the default datasource for quick access.
     */
    @PostConstruct
    void init() {
        // Parse configured instances
        String instancesStr = instanceConfig.instances();
        availableInstances = Arrays.stream(instancesStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        LOG.infof("Configured instances: %s", availableInstances);

        // Cache the default datasource
        dataSourceCache.put("default", defaultDataSource);
    }

    /**
     * Retrieves the datasource for the specified instance name.
     * <p>
     * Returns the default unnamed datasource for instance name "default", null, or empty string.
     * For named instances, performs a lazy lookup via Arc container and caches the result.
     *
     * @param instanceName the instance name ("default" for the unnamed datasource)
     * @return the datasource for the specified instance
     * @throws IllegalArgumentException if the instance is not configured or cannot be found
     * @see #lookupNamedDataSource(String)
     */
    public javax.sql.DataSource getDataSource(String instanceName) {
        if (instanceName == null || instanceName.isEmpty() || "default".equals(instanceName)) {
            return defaultDataSource;
        }

        return dataSourceCache.computeIfAbsent(instanceName, this::lookupNamedDataSource);
    }

    /**
     * Looks up a named datasource using the Arc CDI container.
     * <p>
     * This method is called when a datasource is not found in the cache.
     * It uses Quarkus Arc to locate the named datasource bean.
     *
     * @param name the name of the datasource to look up
     * @return the AgroalDataSource for the specified name
     * @throws IllegalArgumentException if the datasource is not configured or cannot be found
     */
    private AgroalDataSource lookupNamedDataSource(String name) {
        try {
            // Use Arc to look up the named datasource
            InstanceHandle<AgroalDataSource> handle = Arc.container()
                    .instance(AgroalDataSource.class, new DataSource.DataSourceLiteral(name));

            if (handle.isAvailable()) {
                LOG.infof("Found datasource for instance: %s", name);
                return handle.get();
            } else {
                LOG.warnf("No datasource configured for instance: %s", name);
                throw new IllegalArgumentException("No datasource configured for instance: " + name);
            }
        } catch (Exception e) {
            LOG.errorf("Failed to lookup datasource for instance %s: %s", name, e.getMessage());
            throw new IllegalArgumentException("Failed to lookup datasource for instance: " + name, e);
        }
    }

    /**
     * Retrieves the list of available instance names.
     * <p>
     * Returns the instance names parsed from configuration during initialisation.
     *
     * @return immutable list of configured instance names
     */
    public List<String> getAvailableInstances() {
        return availableInstances;
    }

    /**
     * Retrieves the display name for the specified instance.
     * <p>
     * First checks the instance configuration for a custom display name.
     * If not configured, returns "Default" for the default instance,
     * or capitalises the first letter of the instance name as a fallback.
     *
     * @param instanceName the instance name
     * @return the display name suitable for UI presentation
     */
    public String getDisplayName(String instanceName) {
        if (instanceName == null || instanceName.isEmpty() || "default".equals(instanceName)) {
            return "Default";
        }

        var props = instanceConfig.instanceProperties().get(instanceName);
        if (props != null && props.displayName().isPresent()) {
            return props.displayName().get();
        }

        // Capitalise the instance name as fallback
        return instanceName.substring(0, 1).toUpperCase() + instanceName.substring(1);
    }

    /**
     * Retrieves information about all configured PostgreSQL instances.
     * <p>
     * For each configured instance, gathers metadata including display name,
     * connection status, PostgreSQL version, and current database name.
     * Connection failures are logged but do not cause the method to fail.
     *
     * @return list of instance information objects with connection and version details
     * @see #isConnected(String)
     * @see #getInstanceDatabaseInfo(String)
     */
    public List<InstanceInfo> getInstanceInfoList() {
        List<InstanceInfo> instances = new ArrayList<>();

        for (String name : availableInstances) {
            InstanceInfo info = new InstanceInfo();
            info.setName(name);
            info.setDisplayName(getDisplayName(name));
            info.setConnected(isConnected(name));

            if (info.isConnected()) {
                try {
                    var dbInfo = getInstanceDatabaseInfo(name);
                    info.setVersion(dbInfo.version());
                    info.setCurrentDatabase(dbInfo.database());
                } catch (Exception e) {
                    LOG.debugf("Could not get database info for %s: %s", name, e.getMessage());
                }
            }

            instances.add(info);
        }

        return instances;
    }

    /**
     * Checks whether the specified instance is currently connected.
     * <p>
     * Attempts to obtain a connection and validates it with a 2-second timeout.
     * Returns false if the datasource cannot be found, connection fails, or validation times out.
     *
     * @param instanceName the instance name to check
     * @return true if the instance is connected and responsive, false otherwise
     */
    public boolean isConnected(String instanceName) {
        try {
            javax.sql.DataSource ds = getDataSource(instanceName);
            try (Connection conn = ds.getConnection()) {
                return conn.isValid(2);
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Internal record for holding basic database version and name information.
     *
     * @param version the PostgreSQL version string (major version only)
     * @param database the current database name
     */
    private record DatabaseInfo(String version, String database) {}

    /**
     * Retrieves basic database information for the specified instance.
     * <p>
     * Queries the database for version and current database name.
     * Returns a record with null values if the query fails.
     *
     * @param instanceName the instance name to query
     * @return database information record with version and database name, or nulls if query fails
     */
    private DatabaseInfo getInstanceDatabaseInfo(String instanceName) {
        try {
            javax.sql.DataSource ds = getDataSource(instanceName);
            try (Connection conn = ds.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT version(), current_database()")) {
                if (rs.next()) {
                    String version = rs.getString(1);
                    String database = rs.getString(2);
                    // Extract just the version number
                    if (version != null && version.contains(" ")) {
                        version = version.split(" ")[1];
                    }
                    return new DatabaseInfo(version, database);
                }
            }
        } catch (Exception e) {
            LOG.debugf("Failed to get database info for %s: %s", instanceName, e.getMessage());
        }
        return new DatabaseInfo(null, null);
    }
}
