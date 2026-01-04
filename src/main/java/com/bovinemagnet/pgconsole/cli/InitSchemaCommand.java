package com.bovinemagnet.pgconsole.cli;

import com.bovinemagnet.pgconsole.service.DataSourceManager;
import com.bovinemagnet.pgconsole.service.MetadataDataSourceProvider;
import jakarta.inject.Inject;
import org.flywaydb.core.Flyway;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * CLI command to initialise the pgconsole history schema.
 * <p>
 * Creates the required database schema and tables for storing
 * historical metrics data. Uses Flyway migrations to ensure
 * the schema is up to date.
 * <p>
 * The schema can be initialised on either:
 * <ul>
 *   <li>A specific PostgreSQL instance (default behaviour)</li>
 *   <li>The dedicated metadata database (when --metadata flag is used)</li>
 * </ul>
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@Command(
        name = "init-schema",
        description = "Initialise pgconsole history schema on target database",
        mixinStandardHelpOptions = true
)
public class InitSchemaCommand implements Runnable {

    @Option(names = {"-i", "--instance"},
            description = "Target instance (default: default). Ignored if --metadata is set.")
    private String instance = "default";

    @Option(names = {"--metadata"},
            description = "Initialise schema on the configured metadata database instead of an instance")
    private boolean useMetadata;

    @Option(names = {"--dry-run"},
            description = "Show what would be done without making changes")
    private boolean dryRun;

    @Option(names = {"--force"},
            description = "Force schema creation even if it exists")
    private boolean force;

    @Inject
    DataSourceManager dataSourceManager;

    @Inject
    MetadataDataSourceProvider metadataProvider;

    @Override
    public void run() {
        System.out.println();
        System.out.println("PG Console Schema Initialisation");
        System.out.println("=================================");
        System.out.println();

        DataSource ds;
        String targetDescription;

        if (useMetadata) {
            ds = metadataProvider.getDataSource();
            targetDescription = "Metadata database (" + metadataProvider.getDataSourceName() + ")";
            if (metadataProvider.isDedicatedMetadataDataSource()) {
                System.out.println("Target: Dedicated metadata database");
            } else {
                System.out.println("Target: Default datasource (no dedicated metadata database configured)");
            }
        } else {
            ds = dataSourceManager.getDataSource(instance);
            targetDescription = "Instance: " + instance;
            System.out.println(targetDescription);
        }
        System.out.println();

        try {

            // Check if schema already exists
            boolean schemaExists = checkSchemaExists(ds);

            if (schemaExists && !force) {
                System.out.println("Schema 'pgconsole' already exists.");
                System.out.println("Use --force to re-run migrations.");
                System.exit(0);
            }

            if (dryRun) {
                System.out.println("DRY RUN - No changes will be made");
                System.out.println();
                showPendingMigrations(ds);
                return;
            }

            // Run Flyway migrations
            System.out.println("Running schema migrations...");
            System.out.println();

            Flyway flyway = Flyway.configure()
                    .dataSource(ds)
                    .schemas("pgconsole")
                    .baselineOnMigrate(true)
                    .locations("classpath:db/migration")
                    .load();

            var result = flyway.migrate();

            System.out.println("Migrations applied: " + result.migrationsExecuted);
            if (result.targetSchemaVersion != null) {
                System.out.println("Schema version: " + result.targetSchemaVersion);
            }

            System.out.println();
            System.out.println("Schema initialisation completed successfully.");
            System.exit(0);

        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            System.exit(1);
        }
    }

    private boolean checkSchemaExists(DataSource ds) {
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT EXISTS(SELECT 1 FROM information_schema.schemata WHERE schema_name = 'pgconsole')")) {
            if (rs.next()) {
                return rs.getBoolean(1);
            }
        } catch (Exception e) {
            System.out.println("Warning: Could not check schema existence: " + e.getMessage());
        }
        return false;
    }

    private void showPendingMigrations(DataSource ds) {
        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(ds)
                    .schemas("pgconsole")
                    .baselineOnMigrate(true)
                    .locations("classpath:db/migration")
                    .load();

            var info = flyway.info();
            var pending = info.pending();

            if (pending.length == 0) {
                System.out.println("No pending migrations.");
            } else {
                System.out.println("Pending migrations:");
                for (var migration : pending) {
                    System.out.printf("  - %s: %s%n",
                            migration.getVersion(),
                            migration.getDescription());
                }
            }

            var current = info.current();
            if (current != null) {
                System.out.println();
                System.out.println("Current schema version: " + current.getVersion());
            }

        } catch (Exception e) {
            System.out.println("Error checking migrations: " + e.getMessage());
        }
    }
}
