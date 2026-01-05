package com.bovinemagnet.pgconsole.cli;

import com.bovinemagnet.pgconsole.service.DataSourceManager;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * CLI command to validate PG Console configuration without starting the server.
 * <p>
 * This command provides comprehensive validation of application configuration, database connectivity,
 * and feature toggles to ensure the application can start successfully. It is intended to be run
 * as a pre-deployment check or during troubleshooting to identify configuration issues before
 * attempting to start the full application.
 * <p>
 * Validation checks performed:
 * <ul>
 *   <li>Required properties - Verifies that essential database connection properties are set</li>
 *   <li>Property value ranges - Ensures numeric values are within acceptable bounds</li>
 *   <li>Database connectivity - Tests actual connections to configured PostgreSQL instances</li>
 *   <li>Feature toggle consistency - Checks for conflicting or incomplete feature configurations</li>
 * </ul>
 * <p>
 * Exit codes returned by this command:
 * <ul>
 *   <li>0 - Configuration is completely valid with no warnings</li>
 *   <li>1 - Configuration has errors that will prevent application startup</li>
 *   <li>2 - Configuration has warnings (only in strict mode)</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>{@code
 * # Validate default configuration
 * java -jar pg-console.jar validate-config
 *
 * # Validate specific configuration file
 * java -jar pg-console.jar validate-config -c /path/to/application.properties
 *
 * # Strict mode - treat warnings as errors
 * java -jar pg-console.jar validate-config --strict
 *
 * # Skip database connectivity tests
 * java -jar pg-console.jar validate-config --skip-db
 *
 * # Verbose output showing all checks
 * java -jar pg-console.jar validate-config --verbose
 * }</pre>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see DataSourceManager
 * @since 0.0.0
 */
@Command(name = "validate-config", description = "Validate configuration without starting server", mixinStandardHelpOptions = true)
public class ValidateConfigCommand implements Runnable {

	/**
	 * Path to an alternative configuration file to validate.
	 * <p>
	 * When specified, this file will be loaded in place of the default application.properties.
	 * The path can be absolute or relative to the current working directory.
	 */
	@Option(names = { "-c", "--config" }, description = "Path to configuration file to validate")
	private String configFile;

	/**
	 * Strict validation mode flag.
	 * <p>
	 * When enabled, warnings are treated as errors and will cause the command to exit with
	 * code 2. This is useful in CI/CD pipelines where any configuration issue should fail
	 * the build.
	 */
	@Option(names = { "--strict" }, description = "Treat warnings as errors")
	private boolean strict;

	/**
	 * Flag to skip database connectivity checks.
	 * <p>
	 * When enabled, the validator will not attempt to connect to any configured PostgreSQL
	 * instances. This is useful when validating configuration files in environments where
	 * database access is not available (e.g., during build processes).
	 */
	@Option(names = { "--skip-db" }, description = "Skip database connectivity checks")
	private boolean skipDb;

	/**
	 * Verbose output flag.
	 * <p>
	 * When enabled, the validator will output detailed information about every configuration
	 * property checked, including successful validations. Without this flag, only errors
	 * and warnings are displayed.
	 */
	@Option(names = { "--verbose" }, description = "Show all validated properties")
	private boolean verbose;

	/**
	 * Data source manager for accessing configured PostgreSQL instances.
	 * <p>
	 * Injected by Quarkus CDI. Used to retrieve and test database connections during
	 * validation when database connectivity checks are enabled.
	 */
	@Inject
	DataSourceManager dataSourceManager;

	/**
	 * List of validation errors encountered during configuration checks.
	 * <p>
	 * Errors represent critical configuration issues that will prevent the application
	 * from starting successfully. If this list is non-empty at the end of validation,
	 * the command will exit with code 1.
	 */
	private final List<String> errors = new ArrayList<>();

	/**
	 * List of validation warnings encountered during configuration checks.
	 * <p>
	 * Warnings represent potential issues or suboptimal configurations that may impact
	 * application behaviour or performance but won't prevent startup. In strict mode,
	 * non-empty warnings will cause exit code 2.
	 */
	private final List<String> warnings = new ArrayList<>();

	/**
	 * Executes the configuration validation command.
	 * <p>
	 * This method orchestrates the entire validation process by:
	 * <ol>
	 *   <li>Loading the specified configuration file or using defaults</li>
	 *   <li>Validating required properties are present</li>
	 *   <li>Checking property values are within acceptable ranges</li>
	 *   <li>Testing database connectivity (unless skipped)</li>
	 *   <li>Validating feature toggle consistency</li>
	 *   <li>Printing validation results to console</li>
	 *   <li>Exiting with appropriate status code</li>
	 * </ol>
	 * <p>
	 * This method will always call {@code System.exit()} with one of the documented exit codes.
	 * It never returns normally.
	 *
	 * @see #validateRequiredProperties(Config)
	 * @see #validatePropertyValues(Config)
	 * @see #validateDatabaseConnectivity()
	 * @see #validateFeatureToggles(Config)
	 * @see #printResults()
	 */
	@Override
	public void run() {
		System.out.println();
		System.out.println("PG Console Configuration Validator");
		System.out.println("===================================");
		System.out.println();

		if (configFile != null) {
			System.setProperty("quarkus.config.locations", configFile);
			System.out.println("Validating: " + configFile);
		} else {
			System.out.println("Validating: default configuration");
		}
		System.out.println();

		Config config = ConfigProvider.getConfig();

		// Validate required properties
		validateRequiredProperties(config);

		// Validate property values
		validatePropertyValues(config);

		// Validate database connectivity
		if (!skipDb) {
			validateDatabaseConnectivity();
		}

		// Validate feature toggles
		validateFeatureToggles(config);

		// Print results
		printResults();

		// Exit with appropriate code
		if (!errors.isEmpty()) {
			System.exit(1);
		} else if (!warnings.isEmpty() && strict) {
			System.exit(2);
		} else {
			System.exit(0);
		}
	}

	/**
	 * Validates that all required configuration properties are present and non-empty.
	 * <p>
	 * Checks the following properties:
	 * <ul>
	 *   <li>{@code quarkus.datasource.jdbc.url} - Database JDBC connection URL (required, error if missing)</li>
	 *   <li>{@code quarkus.datasource.username} - Database username (optional, warning if missing)</li>
	 *   <li>{@code quarkus.datasource.password} - Database password (optional, warning if missing)</li>
	 * </ul>
	 * <p>
	 * Missing required properties are added to the {@link #errors} list, whilst missing optional
	 * properties that may cause connection failures are added to the {@link #warnings} list.
	 * In verbose mode, successfully found properties are printed to console.
	 *
	 * @param config the MicroProfile Config instance containing application properties
	 * @see ConfigProvider#getConfig()
	 */
	private void validateRequiredProperties(Config config) {
		if (verbose) {
			System.out.println("Checking required properties...");
		}

		// Database URL
		var dbUrl = config.getOptionalValue("quarkus.datasource.jdbc.url", String.class);
		if (dbUrl.isEmpty() || dbUrl.get().isBlank()) {
			errors.add("quarkus.datasource.jdbc.url is not set");
		} else if (verbose) {
			System.out.println("  [OK] quarkus.datasource.jdbc.url");
		}

		// Database username
		var dbUser = config.getOptionalValue("quarkus.datasource.username", String.class);
		if (dbUser.isEmpty() || dbUser.get().isBlank()) {
			warnings.add("quarkus.datasource.username is not set (will use default)");
		} else if (verbose) {
			System.out.println("  [OK] quarkus.datasource.username");
		}

		// Database password
		var dbPass = config.getOptionalValue("quarkus.datasource.password", String.class);
		if (dbPass.isEmpty() || dbPass.get().isBlank()) {
			warnings.add("quarkus.datasource.password is not set (may fail to connect)");
		} else if (verbose) {
			System.out.println("  [OK] quarkus.datasource.password (set)");
		}

		if (verbose) {
			System.out.println();
		}
	}

	/**
	 * Validates that configuration property values are within acceptable ranges.
	 * <p>
	 * Performs range and sanity checks on the following properties:
	 * <ul>
	 *   <li>{@code quarkus.http.port} - HTTP server port (1-65535, warning if &lt;1024)</li>
	 *   <li>{@code pg-console.history.interval-seconds} - History sampling interval (warning if &lt;10)</li>
	 *   <li>{@code pg-console.history.retention-days} - History retention period (error if &lt;1, warning if &gt;365)</li>
	 *   <li>{@code pg-console.alerting.cooldown-seconds} - Alert cooldown period (warning if &lt;60)</li>
	 * </ul>
	 * <p>
	 * Invalid values that would prevent application startup are added to {@link #errors},
	 * whilst suboptimal values that may impact performance or usability are added to {@link #warnings}.
	 *
	 * @param config the MicroProfile Config instance containing application properties
	 */
	private void validatePropertyValues(Config config) {
		if (verbose) {
			System.out.println("Validating property values...");
		}

		// HTTP port
		var port = config.getOptionalValue("quarkus.http.port", Integer.class);
		if (port.isPresent()) {
			int p = port.get();
			if (p < 1 || p > 65535) {
				errors.add("quarkus.http.port must be between 1 and 65535 (got: " + p + ")");
			} else if (p < 1024) {
				warnings.add("quarkus.http.port is a privileged port (" + p + ") - may require root");
			} else if (verbose) {
				System.out.println("  [OK] quarkus.http.port = " + p);
			}
		}

		// History interval
		var historyInterval = config.getOptionalValue("pg-console.history.interval-seconds", Integer.class);
		if (historyInterval.isPresent()) {
			int interval = historyInterval.get();
			if (interval < 10) {
				warnings.add("pg-console.history.interval-seconds is very low (" + interval + "s) - may impact performance");
			} else if (verbose) {
				System.out.println("  [OK] pg-console.history.interval-seconds = " + interval);
			}
		}

		// History retention
		var historyRetention = config.getOptionalValue("pg-console.history.retention-days", Integer.class);
		if (historyRetention.isPresent()) {
			int days = historyRetention.get();
			if (days < 1) {
				errors.add("pg-console.history.retention-days must be at least 1 (got: " + days + ")");
			} else if (days > 365) {
				warnings.add("pg-console.history.retention-days is very high (" + days + " days) - may use significant storage");
			} else if (verbose) {
				System.out.println("  [OK] pg-console.history.retention-days = " + days);
			}
		}

		// Alerting cooldown
		var cooldown = config.getOptionalValue("pg-console.alerting.cooldown-seconds", Integer.class);
		if (cooldown.isPresent()) {
			int c = cooldown.get();
			if (c < 60) {
				warnings.add("pg-console.alerting.cooldown-seconds is low (" + c + "s) - may cause alert storms");
			} else if (verbose) {
				System.out.println("  [OK] pg-console.alerting.cooldown-seconds = " + c);
			}
		}

		if (verbose) {
			System.out.println();
		}
	}

	/**
	 * Validates database connectivity for all configured PostgreSQL instances.
	 * <p>
	 * This method retrieves all available instance names from the {@link DataSourceManager}
	 * and attempts to establish a connection to each one. For each instance, it:
	 * <ol>
	 *   <li>Retrieves the {@link DataSource} from the manager</li>
	 *   <li>Opens a connection</li>
	 *   <li>Validates the connection is functional using {@link Connection#isValid(int)} with 5 second timeout</li>
	 *   <li>Closes the connection</li>
	 * </ol>
	 * <p>
	 * Connection failures are added to the {@link #errors} list with details of which instance
	 * failed and the error message. If the {@link DataSourceManager} itself cannot be accessed,
	 * a warning is added instead of an error to allow validation to continue.
	 * <p>
	 * This method is skipped entirely if the {@link #skipDb} flag is set.
	 *
	 * @see DataSourceManager#getAvailableInstances()
	 * @see DataSourceManager#getDataSource(String)
	 */
	private void validateDatabaseConnectivity() {
		if (verbose) {
			System.out.println("Checking database connectivity...");
		}

		try {
			List<String> instances = dataSourceManager.getAvailableInstances();

			for (String instanceName : instances) {
				try {
					DataSource ds = dataSourceManager.getDataSource(instanceName);
					try (Connection conn = ds.getConnection()) {
						if (conn.isValid(5)) {
							if (verbose) {
								System.out.println("  [OK] Instance '" + instanceName + "' connected");
							}
						} else {
							errors.add("Instance '" + instanceName + "' connection is not valid");
						}
					}
				} catch (Exception e) {
					errors.add("Instance '" + instanceName + "' connection failed: " + e.getMessage());
				}
			}
		} catch (Exception e) {
			warnings.add("Could not validate database connectivity: " + e.getMessage());
		}

		if (verbose) {
			System.out.println();
		}
	}

	/**
	 * Validates feature toggle configuration for consistency and completeness.
	 * <p>
	 * This method checks that feature flags are configured correctly and that all required
	 * supporting configuration is present when a feature is enabled. Specifically, it validates:
	 * <ul>
	 *   <li><b>Monitoring toggles</b> - Checks for potential conflicts where dashboard toggles
	 *       are enabled but the parent monitoring section is disabled</li>
	 *   <li><b>Security configuration</b> - When {@code pg-console.security.enabled} is true,
	 *       verifies that {@code quarkus.security.users.file.users} is configured</li>
	 *   <li><b>Alerting configuration</b> - When {@code pg-console.alerting.enabled} is true,
	 *       verifies that at least one alert destination (webhook URL or email address) is configured</li>
	 * </ul>
	 * <p>
	 * Missing supporting configuration for enabled features results in warnings being added
	 * to {@link #warnings}, as the application may still start but the feature will not
	 * function correctly.
	 *
	 * @param config the MicroProfile Config instance containing application properties
	 */
	private void validateFeatureToggles(Config config) {
		if (verbose) {
			System.out.println("Checking feature toggles...");
		}

		// Check for conflicting toggle settings
		boolean monitoringEnabled = config.getOptionalValue("pg-console.dashboards.monitoring.enabled", Boolean.class).orElse(true);

		if (!monitoringEnabled) {
			boolean dashboardEnabled = config.getOptionalValue("pg-console.dashboards.monitoring.dashboard", Boolean.class).orElse(true);

			if (dashboardEnabled && verbose) {
				System.out.println("  [INFO] Dashboard page toggle is true but Monitoring section is disabled");
			}
		}

		// Check security configuration
		boolean securityEnabled = config.getOptionalValue("pg-console.security.enabled", Boolean.class).orElse(false);

		if (securityEnabled) {
			var usersFile = config.getOptionalValue("quarkus.security.users.file.users", String.class);

			if (usersFile.isEmpty()) {
				warnings.add("Security is enabled but no users file configured");
			} else if (verbose) {
				System.out.println("  [OK] Security enabled with users file: " + usersFile.get());
			}
		}

		// Check alerting configuration
		boolean alertingEnabled = config.getOptionalValue("pg-console.alerting.enabled", Boolean.class).orElse(false);

		if (alertingEnabled) {
			var webhookUrl = config.getOptionalValue("pg-console.alerting.webhook-url", String.class);
			var emailTo = config.getOptionalValue("pg-console.alerting.email-to", String.class);

			if (webhookUrl.isEmpty() && emailTo.isEmpty()) {
				warnings.add("Alerting is enabled but no webhook URL or email configured");
			} else if (verbose) {
				if (webhookUrl.isPresent()) {
					System.out.println("  [OK] Alerting webhook configured");
				}
				if (emailTo.isPresent()) {
					System.out.println("  [OK] Alerting email configured: " + emailTo.get());
				}
			}
		}

		if (verbose) {
			System.out.println();
		}
	}

	/**
	 * Prints the final validation results to the console.
	 * <p>
	 * This method outputs a formatted summary of all validation errors and warnings encountered
	 * during the configuration checks. The output format is:
	 * <ul>
	 *   <li>If no errors or warnings: "[SUCCESS] Configuration is valid"</li>
	 *   <li>If errors exist: All errors listed with [ERROR] prefix, followed by "[FAILED] Configuration has errors"</li>
	 *   <li>If warnings exist but no errors: All warnings listed with [WARN] prefix,
	 *       followed by "[SUCCESS] Configuration is valid (with warnings)"</li>
	 * </ul>
	 * <p>
	 * This method only prints output; it does not affect the exit code or modify any state.
	 * The actual exit code is determined by the {@link #run()} method based on the contents
	 * of {@link #errors} and {@link #warnings}.
	 *
	 * @see #errors
	 * @see #warnings
	 */
	private void printResults() {
		System.out.println("Validation Results");
		System.out.println("------------------");
		System.out.println();

		if (errors.isEmpty() && warnings.isEmpty()) {
			System.out.println("[SUCCESS] Configuration is valid");
			System.out.println();
			return;
		}

		if (!errors.isEmpty()) {
			System.out.println("ERRORS (" + errors.size() + "):");
			for (String error : errors) {
				System.out.println("  [ERROR] " + error);
			}
			System.out.println();
		}

		if (!warnings.isEmpty()) {
			System.out.println("WARNINGS (" + warnings.size() + "):");
			for (String warning : warnings) {
				System.out.println("  [WARN] " + warning);
			}
			System.out.println();
		}

		if (!errors.isEmpty()) {
			System.out.println("[FAILED] Configuration has errors");
		} else {
			System.out.println("[SUCCESS] Configuration is valid (with warnings)");
		}
		System.out.println();
	}
}
