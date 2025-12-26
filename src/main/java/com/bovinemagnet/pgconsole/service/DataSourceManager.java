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
     * Gets the datasource for a given instance name.
     *
     * @param instanceName the instance name ("default" for the unnamed datasource)
     * @return the datasource
     * @throws IllegalArgumentException if the instance is not configured
     */
    public javax.sql.DataSource getDataSource(String instanceName) {
        if (instanceName == null || instanceName.isEmpty() || "default".equals(instanceName)) {
            return defaultDataSource;
        }

        return dataSourceCache.computeIfAbsent(instanceName, this::lookupNamedDataSource);
    }

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
     * Gets the list of available instance names.
     *
     * @return list of instance names
     */
    public List<String> getAvailableInstances() {
        return availableInstances;
    }

    /**
     * Gets the display name for an instance.
     *
     * @param instanceName the instance name
     * @return the display name (or the instance name if not configured)
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
     * Gets information about all configured instances.
     *
     * @return list of instance info objects
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
     * Checks if an instance is connected.
     *
     * @param instanceName the instance name
     * @return true if connected
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

    private record DatabaseInfo(String version, String database) {}

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
