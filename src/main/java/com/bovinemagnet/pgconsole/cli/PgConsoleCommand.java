package com.bovinemagnet.pgconsole.cli;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.Quarkus;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Main CLI entry point for PG Console application.
 * <p>
 * This command serves as both the default server launcher and the parent command for
 * administrative CLI operations. When invoked without subcommands, it starts the
 * Quarkus-based HTTP server for PostgreSQL monitoring. When invoked with subcommands,
 * it provides administrative tools for health checks, instance management, schema
 * initialisation, statistics reset, and report generation.
 * </p>
 * <p>
 * The command uses Picocli for argument parsing and supports comprehensive CLI options
 * for configuring server behaviour. All CLI arguments take precedence over environment
 * variables and application.properties configuration.
 * </p>
 * <p>
 * <strong>Available Subcommands:</strong>
 * <ul>
 *   <li>{@link HealthCheckCommand} - Validate database connectivity and extension availability</li>
 *   <li>{@link ListInstancesCommand} - List configured PostgreSQL instances</li>
 *   <li>{@link InitSchemaCommand} - Initialise pgconsole schema for history storage</li>
 *   <li>{@link ResetStatsCommand} - Reset pg_stat_statements statistics</li>
 *   <li>{@link ExportReportCommand} - Generate diagnostic reports</li>
 *   <li>{@link ExportConfigCommand} - Export current configuration</li>
 *   <li>{@link ValidateConfigCommand} - Validate configuration file syntax</li>
 *   <li>{@link GenerateCompletionCommand} - Generate shell completion scripts</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Usage Examples:</strong>
 * <pre>{@code
 * # Start server with defaults (port 8080, all features enabled)
 * java -jar pg-console.jar
 *
 * # Start server on custom port without history sampling
 * java -jar pg-console.jar --port 9090 --no-history
 *
 * # Start server with verbose logging
 * java -jar pg-console.jar --verbose
 *
 * # Run health check against configured instances
 * java -jar pg-console.jar health-check
 *
 * # List all configured PostgreSQL instances
 * java -jar pg-console.jar list-instances
 *
 * # Export diagnostic report to file
 * java -jar pg-console.jar export-report --output diagnostics.txt
 * }</pre>
 * </p>
 * <p>
 * The command handles both production and development launch modes appropriately,
 * avoiding duplicate Quarkus initialisation in dev mode while properly starting
 * the server in production builds.
 * </p>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @since 0.0.0
 * @see HealthCheckCommand
 * @see ListInstancesCommand
 * @see InitSchemaCommand
 */
@TopCommand
@Command(
	name = "pg-console",
	mixinStandardHelpOptions = true,
	version = "PG Console 1.0.0",
	description = "PostgreSQL monitoring and performance analysis console",
	subcommands = { HealthCheckCommand.class, ListInstancesCommand.class, InitSchemaCommand.class, ResetStatsCommand.class, ExportReportCommand.class, ExportConfigCommand.class, ValidateConfigCommand.class, GenerateCompletionCommand.class },
	footer = { "", "Configuration priority: CLI args > environment variables > application.properties", "", "For more information, visit: https://github.com/bovinemagnet/pg-console" }
)
public class PgConsoleCommand implements Runnable {

	/**
	 * HTTP server port to bind to.
	 * <p>
	 * Overrides the {@code quarkus.http.port} property when specified.
	 * When not specified, uses the configured Quarkus HTTP port.
	 * </p>
	 */
	@Option(names = { "-p", "--port" }, description = "HTTP server port (overrides configured port)")
	private Integer port;

	/**
	 * HTTP bind address for the server.
	 * <p>
	 * Overrides the {@code quarkus.http.host} property when specified.
	 * When not specified, uses the configured Quarkus HTTP host.
	 * Use "localhost" or "127.0.0.1" to restrict to local connections only.
	 * </p>
	 */
	@Option(names = { "--host" }, description = "HTTP bind address (overrides configured host)")
	private String host;

	/**
	 * Configured HTTP port from Quarkus configuration.
	 * <p>
	 * Retrieved from {@code quarkus.http.port} property with fallback to 8080.
	 * Used when no CLI port override is specified.
	 * </p>
	 */
	@Inject
	@ConfigProperty(name = "quarkus.http.port", defaultValue = "8080")
	Integer configuredPort;

	/**
	 * Configured HTTP host from Quarkus configuration.
	 * <p>
	 * Retrieved from {@code quarkus.http.host} property with fallback to "0.0.0.0".
	 * Used when no CLI host override is specified.
	 * </p>
	 */
	@Inject
	@ConfigProperty(name = "quarkus.http.host", defaultValue = "0.0.0.0")
	String configuredHost;

	/**
	 * Flag to disable history sampling at startup.
	 * <p>
	 * When set, history sampling will not collect metrics into the pgconsole schema.
	 * This overrides the {@code pg-console.history.enabled} property, setting it to false.
	 * Useful for read-only deployments or troubleshooting.
	 * </p>
	 */
	@Option(names = { "--no-history" }, description = "Disable history sampling at startup")
	private boolean noHistory;

	/**
	 * Flag to disable alerting at startup.
	 * <p>
	 * When set, the alerting subsystem will not be initialised or process rules.
	 * This overrides the {@code pg-console.alerting.enabled} property, setting it to false.
	 * Useful for testing or maintenance windows.
	 * </p>
	 */
	@Option(names = { "--no-alerting" }, description = "Disable alerting at startup")
	private boolean noAlerting;

	/**
	 * Default PostgreSQL instance identifier to connect to.
	 * <p>
	 * Specifies which configured instance should be used as the default connection.
	 * This overrides the {@code pg-console.default-instance} property when specified.
	 * The instance must be defined in the application configuration.
	 * </p>
	 */
	@Option(names = { "-i", "--instance" }, description = "Default PostgreSQL instance to connect to")
	private String instance;

	/**
	 * Path to alternate configuration file.
	 * <p>
	 * Overrides the {@code quarkus.config.locations} property to load configuration
	 * from a custom file location. Supports absolute or relative paths. Can be used
	 * to maintain separate configurations for different environments.
	 * </p>
	 */
	@Option(names = { "-c", "--config" }, description = "Path to alternate configuration file")
	private String configFile;

	/**
	 * Flag to enable verbose logging output.
	 * <p>
	 * When set, enables DEBUG level logging for the application and Quarkus framework.
	 * Sets {@code quarkus.log.level} to DEBUG and
	 * {@code quarkus.log.category."com.bovinemagnet.pgconsole".level} to DEBUG.
	 * Useful for troubleshooting startup issues or runtime behaviour.
	 * </p>
	 */
	@Option(names = { "--verbose" }, description = "Enable verbose logging output")
	private boolean verbose;

	/**
	 * Application version string injected from Quarkus configuration.
	 * <p>
	 * Retrieved from {@code quarkus.application.version} property with fallback
	 * to "1.0.0". Used in the startup banner and version display.
	 * </p>
	 */
	@Inject
	@ConfigProperty(name = "quarkus.application.version", defaultValue = "1.0.0")
	String applicationVersion;

	/**
	 * Executes the main server startup sequence.
	 * <p>
	 * This method is invoked when the command is run without subcommands. It performs
	 * the following operations:
	 * </p>
	 * <ol>
	 *   <li>Applies CLI argument overrides as system properties, taking precedence over
	 *       environment variables and application.properties</li>
	 *   <li>Displays the application startup banner with version and configuration info</li>
	 *   <li>Starts the Quarkus application server (production/test mode) or waits for
	 *       exit signal (development mode with live reload)</li>
	 * </ol>
	 * <p>
	 * The method is launch-mode aware: in production and test modes it calls
	 * {@code Quarkus.run()} to initialise the server, whilst in development mode
	 * it calls {@code Quarkus.waitForExit()} to avoid duplicate server initialisation.
	 * </p>
	 * <p>
	 * Configuration precedence (highest to lowest):
	 * <ol>
	 *   <li>CLI arguments (this method's system property overrides)</li>
	 *   <li>Environment variables</li>
	 *   <li>application.properties / application.yml</li>
	 * </ol>
	 * </p>
	 *
	 * @see Runnable#run()
	 * @see Quarkus#run()
	 * @see Quarkus#waitForExit()
	 * @see LaunchMode
	 */
	@Override
	public void run() {
		// Apply CLI overrides as system properties before Quarkus starts
		if (port != null) {
			System.setProperty("quarkus.http.port", String.valueOf(port));
		}
		if (host != null) {
			System.setProperty("quarkus.http.host", host);
		}
		if (noHistory) {
			System.setProperty("pg-console.history.enabled", "false");
		}
		if (noAlerting) {
			System.setProperty("pg-console.alerting.enabled", "false");
		}
		if (instance != null && !instance.isBlank()) {
			System.setProperty("pg-console.default-instance", instance);
		}
		if (configFile != null && !configFile.isBlank()) {
			System.setProperty("quarkus.config.locations", configFile);
		}
		if (verbose) {
			System.setProperty("quarkus.log.level", "DEBUG");
			System.setProperty("quarkus.log.category.\"com.bovinemagnet.pgconsole\".level", "DEBUG");
		}

		// Print startup banner
		printBanner();

		// In dev mode, Quarkus is already running - don't try to start it again
		if (LaunchMode.current() != LaunchMode.DEVELOPMENT) {
			// Start Quarkus in server mode (production/test)
			Quarkus.run();
		} else {
			// In dev mode, just wait for exit signal
			Quarkus.waitForExit();
		}
	}

	/**
	 * Prints the application startup banner to standard output.
	 * <p>
	 * Displays ASCII art logo, version information, server configuration details,
	 * and status of optional features (history sampling, alerting). The banner
	 * provides immediate visual feedback about the server's configuration at startup.
	 * </p>
	 * <p>
	 * Output includes:
	 * <ul>
	 *   <li>ASCII art "PG Console" logo</li>
	 *   <li>Application version from {@link #applicationVersion}</li>
	 *   <li>Server bind address and port</li>
	 *   <li>Status of history sampling (if disabled via {@link #noHistory})</li>
	 *   <li>Status of alerting (if disabled via {@link #noAlerting})</li>
	 * </ul>
	 * </p>
	 */
	private void printBanner() {
		// Determine effective host and port (CLI override takes precedence)
		String effectiveHost = (host != null) ? host : configuredHost;
		int effectivePort = (port != null) ? port : configuredPort;

		System.out.println();
		System.out.println("  ____   ____    ____                      _      ");
		System.out.println(" |  _ \\ / ___|  / ___|___  _ __  ___  ___ | | ___ ");
		System.out.println(" | |_) | |  _  | |   / _ \\| '_ \\/ __|/ _ \\| |/ _ \\");
		System.out.println(" |  __/| |_| | | |__| (_) | | | \\__ \\ (_) | |  __/");
		System.out.println(" |_|    \\____|  \\____\\___/|_| |_|___/\\___/|_|\\___|");
		System.out.println();
		System.out.println(" PostgreSQL Monitoring Console v" + applicationVersion);
		System.out.println(" Starting server on " + effectiveHost + ":" + effectivePort);
		if (noHistory) {
			System.out.println(" History sampling: DISABLED");
		}
		if (noAlerting) {
			System.out.println(" Alerting: DISABLED");
		}
		System.out.println();
	}
}
