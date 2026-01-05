package com.bovinemagnet.pgconsole.cli;

import com.bovinemagnet.pgconsole.service.DataSourceManager;
import jakarta.inject.Inject;
import java.io.Console;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * CLI command to reset pg_stat_statements counters for PostgreSQL databases.
 * <p>
 * This command provides a controlled interface for resetting query statistics
 * collected by the {@code pg_stat_statements} extension. The reset operation
 * is destructive and permanently deletes all accumulated query performance data,
 * including execution counts, timing information, and resource usage metrics.
 * <p>
 * Before performing the reset, the command displays a summary of current statistics
 * including total statements tracked, total calls, and cumulative execution time.
 * This allows administrators to review what data will be lost.
 * <p>
 * The command supports three reset scopes:
 * <ul>
 * <li>Current database only (default) - Resets statistics for the connected database</li>
 * <li>Specific database ({@code --database}) - Resets statistics for a named database</li>
 * <li>All databases ({@code --all}) - Resets statistics across all databases in the instance</li>
 * </ul>
 * <p>
 * <strong>Safety Features:</strong>
 * <ul>
 * <li>Interactive confirmation prompt (skippable with {@code --force})</li>
 * <li>Statistics summary display before reset</li>
 * <li>Clear warning messages about data loss</li>
 * <li>Helpful error messages for permission issues</li>
 * </ul>
 * <p>
 * <strong>Permission Requirements:</strong>
 * Resetting {@code pg_stat_statements} requires either:
 * <ul>
 * <li>PostgreSQL superuser privileges, or</li>
 * <li>Membership in the {@code pg_read_all_stats} role (PostgreSQL 10+)</li>
 * </ul>
 * <p>
 * <strong>Example Usage:</strong>
 * <pre>{@code
 * # Interactive reset for current database
 * pg-console reset-stats
 *
 * # Reset specific database without confirmation
 * pg-console reset-stats --database myapp_prod --force
 *
 * # Reset all databases across an instance
 * pg-console reset-stats --instance production --all --force
 * }</pre>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see com.bovinemagnet.pgconsole.service.PostgresService
 * @see com.bovinemagnet.pgconsole.service.DataSourceManager
 * @since 0.0.0
 */
@Command(name = "reset-stats", description = "Reset pg_stat_statements counters (requires confirmation)", mixinStandardHelpOptions = true)
public class ResetStatsCommand implements Runnable {

	/**
	 * The target PostgreSQL instance name.
	 * <p>
	 * Corresponds to the instance configuration in the application. The "default"
	 * instance connects using the standard {@code POSTGRES_URL}, {@code POSTGRES_USER},
	 * and {@code POSTGRES_PASSWORD} environment variables.
	 * <p>
	 * Defaults to "default" if not specified.
	 */
	@Option(names = { "-i", "--instance" }, description = "Target instance (default: default)")
	private String instance = "default";

	/**
	 * Whether to skip the interactive confirmation prompt.
	 * <p>
	 * When {@code true}, the command proceeds directly to resetting statistics
	 * without asking for user confirmation. This is useful for automation and
	 * scripting scenarios where interactive prompts would block execution.
	 * <p>
	 * Defaults to {@code false}, requiring explicit confirmation.
	 */
	@Option(names = { "--force", "-f" }, description = "Skip confirmation prompt")
	private boolean force;

	/**
	 * Whether to reset statistics for all databases in the instance.
	 * <p>
	 * When {@code true}, calls {@code pg_stat_statements_reset()} without
	 * parameters, clearing statistics across all databases. This is mutually
	 * exclusive with the {@link #database} option.
	 * <p>
	 * Defaults to {@code false}, resetting only the current database.
	 */
	@Option(names = { "--all" }, description = "Reset statistics for all databases")
	private boolean all;

	/**
	 * The name of a specific database to reset statistics for.
	 * <p>
	 * When specified, only statistics for this database are cleared by calling
	 * {@code pg_stat_statements_reset(NULL, database_oid, NULL)}. This is
	 * mutually exclusive with the {@link #all} option.
	 * <p>
	 * If {@code null} or blank, and {@link #all} is {@code false}, the command
	 * resets statistics for only the current database.
	 */
	@Option(names = { "--database", "-d" }, description = "Reset statistics for specific database only")
	private String database;

	/**
	 * Manages connections to configured PostgreSQL instances.
	 * <p>
	 * Provides access to {@link DataSource} objects for the target instance
	 * specified by {@link #instance}.
	 */
	@Inject
	DataSourceManager dataSourceManager;

	/**
	 * Executes the statistics reset command.
	 * <p>
	 * This method orchestrates the entire reset process:
	 * <ol>
	 * <li>Displays the target instance information</li>
	 * <li>Shows a summary of current statistics via {@link #showStatsSummary(DataSource)}</li>
	 * <li>Prompts for confirmation unless {@link #force} is {@code true}</li>
	 * <li>Executes the appropriate {@code pg_stat_statements_reset()} function based on scope</li>
	 * <li>Reports the outcome and exits</li>
	 * </ol>
	 * <p>
	 * The reset scope is determined by the {@link #all} and {@link #database} options:
	 * <ul>
	 * <li>If {@link #database} is specified: resets only that database</li>
	 * <li>If {@link #all} is {@code true}: resets all databases</li>
	 * <li>Otherwise: resets only the current database</li>
	 * </ul>
	 * <p>
	 * <strong>Exit Codes:</strong>
	 * <ul>
	 * <li>0 - Success or user cancelled operation</li>
	 * <li>1 - Error occurred (permission denied, SQL error, no console for confirmation)</li>
	 * </ul>
	 * <p>
	 * <strong>Thread Safety:</strong> This method is not thread-safe and should only
	 * be called once per command execution.
	 *
	 * @throws RuntimeException if unable to connect to the database or execute SQL commands
	 */
	@Override
	public void run() {
		System.out.println();
		System.out.println("PG Console Statistics Reset");
		System.out.println("===========================");
		System.out.println();
		System.out.println("Instance: " + instance);
		System.out.println();

		try {
			DataSource ds = dataSourceManager.getDataSource(instance);

			// Show current statistics summary before reset
			showStatsSummary(ds);

			// Confirmation
			if (!force) {
				System.out.println();
				System.out.println("WARNING: This will permanently delete all accumulated query statistics!");
				System.out.println();

				Console console = System.console();
				if (console != null) {
					String response = console.readLine("Are you sure you want to proceed? (yes/no): ");
					if (!"yes".equalsIgnoreCase(response.trim())) {
						System.out.println("Operation cancelled.");
						System.exit(0);
					}
				} else {
					System.out.println("No console available for confirmation.");
					System.out.println("Use --force to skip confirmation.");
					System.exit(1);
				}
			}

			// Perform the reset
			System.out.println();
			System.out.println("Resetting statistics...");

			try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement()) {
				if (database != null && !database.isBlank()) {
					// Reset for specific database
					String sql = "SELECT pg_stat_statements_reset(NULL, " + "(SELECT oid FROM pg_database WHERE datname = '" + database + "'), NULL)";
					stmt.execute(sql);
					System.out.println("Statistics reset for database: " + database);
				} else if (all) {
					// Reset all
					stmt.execute("SELECT pg_stat_statements_reset()");
					System.out.println("All statistics have been reset.");
				} else {
					// Reset current database only
					stmt.execute("SELECT pg_stat_statements_reset(NULL, (SELECT oid FROM pg_database WHERE datname = current_database()), NULL)");
					System.out.println("Statistics reset for current database.");
				}
			}

			System.out.println();
			System.out.println("Operation completed successfully.");
			System.exit(0);
		} catch (Exception e) {
			System.out.println("ERROR: " + e.getMessage());
			if (e.getMessage().contains("permission denied")) {
				System.out.println();
				System.out.println("Note: Resetting pg_stat_statements requires superuser privileges");
				System.out.println("or membership in the pg_read_all_stats role.");
			}
			System.exit(1);
		}
	}

	/**
	 * Displays a summary of current statistics from pg_stat_statements.
	 * <p>
	 * Queries {@code pg_stat_statements} to retrieve and display:
	 * <ul>
	 * <li>Total number of unique statements tracked</li>
	 * <li>Total number of statement executions (calls)</li>
	 * <li>Cumulative execution time across all statements</li>
	 * <li>Last reset timestamp (from {@code pg_stat_statements_info} if available)</li>
	 * </ul>
	 * <p>
	 * This information helps administrators understand the scope of data that will
	 * be deleted by the reset operation. If statistics cannot be retrieved (due to
	 * missing extension or permissions), an error message is displayed but execution
	 * continues.
	 * <p>
	 * <strong>PostgreSQL Version Compatibility:</strong>
	 * The {@code pg_stat_statements_info} view is only available in PostgreSQL 14+.
	 * On older versions, the last reset timestamp will be silently skipped.
	 *
	 * @param ds the data source to query for statistics information
	 */
	private void showStatsSummary(DataSource ds) {
		try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement()) {
			// Get statement count and top consumers
			System.out.println("Current Statistics Summary:");
			System.out.println("--------------------------");

			try (ResultSet rs = stmt.executeQuery("SELECT count(*) as total_statements, " + "sum(calls) as total_calls, " + "sum(total_exec_time) as total_time_ms " + "FROM pg_stat_statements")) {
				if (rs.next()) {
					System.out.printf("  Total statements: %,d%n", rs.getLong("total_statements"));
					System.out.printf("  Total calls: %,d%n", rs.getLong("total_calls"));
					System.out.printf("  Total execution time: %,.2f seconds%n", rs.getDouble("total_time_ms") / 1000.0);
				}
			}

			// Show stats age if available
			try (ResultSet rs = stmt.executeQuery("SELECT stats_reset FROM pg_stat_statements_info")) {
				if (rs.next()) {
					var resetTime = rs.getTimestamp("stats_reset");
					if (resetTime != null) {
						System.out.println("  Last reset: " + resetTime);
					} else {
						System.out.println("  Last reset: Never (since server start)");
					}
				}
			} catch (Exception e) {
				// pg_stat_statements_info not available in older versions
			}
		} catch (Exception e) {
			System.out.println("  Could not retrieve statistics summary: " + e.getMessage());
		}
	}
}
