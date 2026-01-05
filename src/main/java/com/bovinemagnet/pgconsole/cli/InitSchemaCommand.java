package com.bovinemagnet.pgconsole.cli;

import com.bovinemagnet.pgconsole.service.DataSourceManager;
import com.bovinemagnet.pgconsole.service.MetadataDataSourceProvider;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

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
 * <p>
 * Example usage:
 * <pre>{@code
 * # Initialise schema on the default instance
 * java -jar app.jar init-schema
 *
 * # Initialise schema on a specific instance
 * java -jar app.jar init-schema --instance production
 *
 * # Initialise schema on the metadata database
 * java -jar app.jar init-schema --metadata
 *
 * # Preview migrations without applying them
 * java -jar app.jar init-schema --dry-run
 *
 * # Force re-run migrations even if schema exists
 * java -jar app.jar init-schema --force
 * }</pre>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see DataSourceManager
 * @see MetadataDataSourceProvider
 * @since 0.0.0
 */
@Command(name = "init-schema", description = "Initialise pgconsole history schema on target database", mixinStandardHelpOptions = true)
public class InitSchemaCommand implements Runnable {

	/**
	 * Target PostgreSQL instance name for schema initialisation.
	 * <p>
	 * Specifies which configured instance to initialise the schema on.
	 * This option is ignored when {@link #useMetadata} is set to true.
	 * <p>
	 * Default value: "default"
	 */
	@Option(names = { "-i", "--instance" }, description = "Target instance (default: default). Ignored if --metadata is set.")
	private String instance = "default";

	/**
	 * Flag to initialise schema on the metadata database instead of a monitored instance.
	 * <p>
	 * When set to true, the schema will be created on the dedicated metadata database
	 * configured via {@link MetadataDataSourceProvider}. If no dedicated metadata database
	 * is configured, the default datasource will be used.
	 * <p>
	 * Default value: false
	 */
	@Option(names = { "--metadata" }, description = "Initialise schema on the configured metadata database instead of an instance")
	private boolean useMetadata;

	/**
	 * Dry-run mode flag that previews migrations without applying them.
	 * <p>
	 * When set to true, displays pending migrations and current schema version
	 * without making any changes to the database. Useful for verifying what
	 * migrations would be applied before actual execution.
	 * <p>
	 * Default value: false
	 */
	@Option(names = { "--dry-run" }, description = "Show what would be done without making changes")
	private boolean dryRun;

	/**
	 * Force flag to re-run migrations even if the schema already exists.
	 * <p>
	 * Normally, if the 'pgconsole' schema already exists, the command will exit
	 * without making changes. Setting this flag to true bypasses that check and
	 * allows Flyway to apply any pending migrations.
	 * <p>
	 * Default value: false
	 */
	@Option(names = { "--force" }, description = "Force schema creation even if it exists")
	private boolean force;

	/**
	 * Manager for accessing configured PostgreSQL instance datasources.
	 * <p>
	 * Provides access to datasources for specific instances identified by name.
	 * Used when initialising the schema on a monitored PostgreSQL instance.
	 */
	@Inject
	DataSourceManager dataSourceManager;

	/**
	 * Provider for the metadata database datasource.
	 * <p>
	 * Supplies the datasource for the dedicated metadata database, or falls back
	 * to the default datasource if no dedicated metadata database is configured.
	 * Used when the {@link #useMetadata} flag is set.
	 */
	@Inject
	MetadataDataSourceProvider metadataProvider;

	/**
	 * Executes the schema initialisation command.
	 * <p>
	 * This method performs the following steps:
	 * <ol>
	 *   <li>Determines the target datasource (instance or metadata database)</li>
	 *   <li>Checks if the 'pgconsole' schema already exists</li>
	 *   <li>In dry-run mode, displays pending migrations without applying them</li>
	 *   <li>Otherwise, executes Flyway migrations to create or update the schema</li>
	 *   <li>Reports the number of migrations applied and final schema version</li>
	 * </ol>
	 * <p>
	 * The command will exit with status code 0 on success or 1 on failure.
	 * If the schema already exists and {@link #force} is false, exits without
	 * making changes.
	 *
	 * @throws RuntimeException if database connection fails or migrations cannot be applied
	 */
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

			Flyway flyway = Flyway.configure().dataSource(ds).schemas("pgconsole").baselineOnMigrate(true).locations("classpath:db/migration").load();

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

	/**
	 * Checks whether the 'pgconsole' schema exists in the target database.
	 * <p>
	 * Queries the {@code information_schema.schemata} system catalog to determine
	 * if the pgconsole schema has been created. This check is used to prevent
	 * re-running migrations unnecessarily unless the {@link #force} flag is set.
	 *
	 * @param ds the datasource to check for schema existence
	 * @return true if the 'pgconsole' schema exists, false otherwise
	 */
	private boolean checkSchemaExists(DataSource ds) {
		try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT EXISTS(SELECT 1 FROM information_schema.schemata WHERE schema_name = 'pgconsole')")) {
			if (rs.next()) {
				return rs.getBoolean(1);
			}
		} catch (Exception e) {
			System.out.println("Warning: Could not check schema existence: " + e.getMessage());
		}
		return false;
	}

	/**
	 * Displays pending Flyway migrations for the target database in dry-run mode.
	 * <p>
	 * Configures a Flyway instance and retrieves migration information without
	 * executing any migrations. Displays:
	 * <ul>
	 *   <li>List of pending migrations with their version and description</li>
	 *   <li>Current schema version if the schema already exists</li>
	 *   <li>Message if no pending migrations are found</li>
	 * </ul>
	 * <p>
	 * This method is called when the {@link #dryRun} flag is set, allowing users
	 * to preview what migrations would be applied before actual execution.
	 *
	 * @param ds the datasource to check for pending migrations
	 */
	private void showPendingMigrations(DataSource ds) {
		try {
			Flyway flyway = Flyway.configure().dataSource(ds).schemas("pgconsole").baselineOnMigrate(true).locations("classpath:db/migration").load();

			var info = flyway.info();
			var pending = info.pending();

			if (pending.length == 0) {
				System.out.println("No pending migrations.");
			} else {
				System.out.println("Pending migrations:");
				for (var migration : pending) {
					System.out.printf("  - %s: %s%n", migration.getVersion(), migration.getDescription());
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
