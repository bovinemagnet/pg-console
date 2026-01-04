package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.config.InstanceConfig;
import com.bovinemagnet.pgconsole.config.MetadataDataSource;
import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Provides the metadata datasource for pgconsole schema storage.
 * <p>
 * This provider resolves the appropriate datasource based on configuration:
 * <ul>
 *   <li>If {@code pg-console.metadata.datasource} is empty or blank, uses the default datasource
 *       (backwards compatible behaviour)</li>
 *   <li>If set to "metadata", uses a dedicated metadata datasource configured as
 *       {@code quarkus.datasource.metadata.*}</li>
 *   <li>If set to an instance name (e.g., "shared"), uses that instance's datasource</li>
 * </ul>
 * <p>
 * The resolved datasource is cached and produced for CDI injection using the
 * {@link MetadataDataSource} qualifier.
 * <p>
 * Example configuration:
 * <pre>
 * # Use default datasource (backwards compatible)
 * pg-console.metadata.datasource=
 *
 * # Use dedicated metadata datasource
 * pg-console.metadata.datasource=metadata
 * quarkus.datasource.metadata.db-kind=postgresql
 * quarkus.datasource.metadata.jdbc.url=jdbc:postgresql://control:5432/pgconsole
 *
 * # Use an existing instance as metadata store
 * pg-console.metadata.datasource=shared
 * </pre>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see MetadataDataSource
 * @see InstanceConfig.MetadataConfig
 */
@ApplicationScoped
public class MetadataDataSourceProvider {

    private static final Logger LOG = Logger.getLogger(MetadataDataSourceProvider.class);

    @Inject
    AgroalDataSource defaultDataSource;

    @Inject
    InstanceConfig instanceConfig;

    private javax.sql.DataSource resolvedDataSource;
    private String resolvedDataSourceName;

    /**
     * Initialises the provider by resolving the configured metadata datasource.
     */
    @PostConstruct
    void init() {
        var datasourceOpt = instanceConfig.metadata().datasource();

        if (datasourceOpt.isEmpty() || datasourceOpt.get().isBlank() || "default".equals(datasourceOpt.get())) {
            // Backwards compatible: use default datasource
            resolvedDataSource = defaultDataSource;
            resolvedDataSourceName = "default";
            LOG.info("Metadata datasource: using default datasource (backwards compatible)");
        } else {
            // Look up named datasource
            String datasourceName = datasourceOpt.get();
            resolvedDataSource = lookupNamedDataSource(datasourceName);
            resolvedDataSourceName = datasourceName;
            LOG.infof("Metadata datasource: using named datasource '%s'", datasourceName);
        }
    }

    /**
     * Looks up a named datasource using the Arc CDI container.
     *
     * @param name the name of the datasource to look up
     * @return the datasource for the specified name
     * @throws IllegalArgumentException if the datasource is not configured or cannot be found
     */
    private AgroalDataSource lookupNamedDataSource(String name) {
        try {
            InstanceHandle<AgroalDataSource> handle = Arc.container()
                    .instance(AgroalDataSource.class, new DataSource.DataSourceLiteral(name));

            if (handle.isAvailable()) {
                LOG.infof("Found metadata datasource: %s", name);
                return handle.get();
            } else {
                LOG.warnf("No datasource configured for metadata: %s. Falling back to default.", name);
                return defaultDataSource;
            }
        } catch (Exception e) {
            LOG.errorf("Failed to lookup metadata datasource '%s': %s. Falling back to default.",
                    name, e.getMessage());
            return defaultDataSource;
        }
    }

    /**
     * Returns the resolved metadata datasource.
     *
     * @return the datasource for storing pgconsole metadata
     */
    public javax.sql.DataSource getDataSource() {
        return resolvedDataSource;
    }

    /**
     * Returns the name of the resolved datasource.
     *
     * @return the datasource name ("default" or the configured name)
     */
    public String getDataSourceName() {
        return resolvedDataSourceName;
    }

    /**
     * Checks whether a dedicated metadata datasource is configured.
     *
     * @return true if using a non-default datasource for metadata
     */
    public boolean isDedicatedMetadataDataSource() {
        return !"default".equals(resolvedDataSourceName);
    }

    /**
     * CDI producer for the metadata datasource.
     * <p>
     * Enables injection of the metadata datasource using:
     * <pre>
     * &#64;Inject
     * &#64;MetadataDataSource
     * DataSource metadataDs;
     * </pre>
     *
     * @return the resolved metadata datasource
     */
    @Produces
    @MetadataDataSource
    public javax.sql.DataSource produceMetadataDataSource() {
        return resolvedDataSource;
    }
}
