package com.bovinemagnet.pgconsole.cli;

import com.bovinemagnet.pgconsole.service.DataSourceManager;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * CLI command to validate configuration without starting the server.
 * <p>
 * Checks for:
 * <ul>
 *   <li>Required properties being set</li>
 *   <li>Valid property values</li>
 *   <li>Database connectivity</li>
 *   <li>Feature toggle consistency</li>
 * </ul>
 * <p>
 * Exit codes:
 * <ul>
 *   <li>0 - Configuration is valid</li>
 *   <li>1 - Configuration has errors</li>
 *   <li>2 - Configuration has warnings but is usable</li>
 * </ul>
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@Command(
        name = "validate-config",
        description = "Validate configuration without starting server",
        mixinStandardHelpOptions = true
)
public class ValidateConfigCommand implements Runnable {

    @Option(names = {"-c", "--config"},
            description = "Path to configuration file to validate")
    private String configFile;

    @Option(names = {"--strict"},
            description = "Treat warnings as errors")
    private boolean strict;

    @Option(names = {"--skip-db"},
            description = "Skip database connectivity checks")
    private boolean skipDb;

    @Option(names = {"--verbose"},
            description = "Show all validated properties")
    private boolean verbose;

    @Inject
    DataSourceManager dataSourceManager;

    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

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

    private void validateFeatureToggles(Config config) {
        if (verbose) {
            System.out.println("Checking feature toggles...");
        }

        // Check for conflicting toggle settings
        boolean monitoringEnabled = config.getOptionalValue(
                "pg-console.dashboards.monitoring.enabled", Boolean.class).orElse(true);

        if (!monitoringEnabled) {
            boolean dashboardEnabled = config.getOptionalValue(
                    "pg-console.dashboards.monitoring.dashboard", Boolean.class).orElse(true);

            if (dashboardEnabled && verbose) {
                System.out.println("  [INFO] Dashboard page toggle is true but Monitoring section is disabled");
            }
        }

        // Check security configuration
        boolean securityEnabled = config.getOptionalValue(
                "pg-console.security.enabled", Boolean.class).orElse(false);

        if (securityEnabled) {
            var usersFile = config.getOptionalValue(
                    "quarkus.security.users.file.users", String.class);

            if (usersFile.isEmpty()) {
                warnings.add("Security is enabled but no users file configured");
            } else if (verbose) {
                System.out.println("  [OK] Security enabled with users file: " + usersFile.get());
            }
        }

        // Check alerting configuration
        boolean alertingEnabled = config.getOptionalValue(
                "pg-console.alerting.enabled", Boolean.class).orElse(false);

        if (alertingEnabled) {
            var webhookUrl = config.getOptionalValue(
                    "pg-console.alerting.webhook-url", String.class);
            var emailTo = config.getOptionalValue(
                    "pg-console.alerting.email-to", String.class);

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
