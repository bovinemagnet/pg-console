package com.bovinemagnet.pgconsole.cli;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.LaunchMode;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Main CLI entry point for PG Console.
 * <p>
 * Provides both server mode (default) and command mode for administrative tasks.
 * Uses Picocli for argument parsing with subcommands for specific operations.
 * <p>
 * Usage examples:
 * <pre>
 * # Start server (default)
 * java -jar pg-console.jar
 *
 * # Start server with options
 * java -jar pg-console.jar --port 9090 --no-alerting
 *
 * # Run admin commands
 * java -jar pg-console.jar health-check
 * java -jar pg-console.jar list-instances
 * java -jar pg-console.jar export-report --output report.txt
 * </pre>
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@TopCommand
@Command(
        name = "pg-console",
        mixinStandardHelpOptions = true,
        version = "PG Console 1.0.0",
        description = "PostgreSQL monitoring and performance analysis console",
        subcommands = {
                HealthCheckCommand.class,
                ListInstancesCommand.class,
                InitSchemaCommand.class,
                ResetStatsCommand.class,
                ExportReportCommand.class,
                ExportConfigCommand.class,
                ValidateConfigCommand.class,
                GenerateCompletionCommand.class
        },
        footer = {
                "",
                "Configuration priority: CLI args > environment variables > application.properties",
                "",
                "For more information, visit: https://github.com/bovinemagnet/pg-console"
        }
)
public class PgConsoleCommand implements Runnable {

    @Option(names = {"-p", "--port"},
            description = "HTTP server port (default: ${DEFAULT-VALUE})",
            defaultValue = "8080")
    private int port;

    @Option(names = {"--host"},
            description = "HTTP bind address (default: ${DEFAULT-VALUE})",
            defaultValue = "0.0.0.0")
    private String host;

    @Option(names = {"--no-history"},
            description = "Disable history sampling at startup")
    private boolean noHistory;

    @Option(names = {"--no-alerting"},
            description = "Disable alerting at startup")
    private boolean noAlerting;

    @Option(names = {"-i", "--instance"},
            description = "Default PostgreSQL instance to connect to")
    private String instance;

    @Option(names = {"-c", "--config"},
            description = "Path to alternate configuration file")
    private String configFile;

    @Option(names = {"--verbose"},
            description = "Enable verbose logging output")
    private boolean verbose;

    @Inject
    @ConfigProperty(name = "quarkus.application.version", defaultValue = "1.0.0")
    String applicationVersion;

    @Override
    public void run() {
        // Apply CLI overrides as system properties before Quarkus starts
        if (port != 8080) {
            System.setProperty("quarkus.http.port", String.valueOf(port));
        }
        if (!"0.0.0.0".equals(host)) {
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

    private void printBanner() {
        System.out.println();
        System.out.println("  ____   ____    ____                      _      ");
        System.out.println(" |  _ \\ / ___|  / ___|___  _ __  ___  ___ | | ___ ");
        System.out.println(" | |_) | |  _  | |   / _ \\| '_ \\/ __|/ _ \\| |/ _ \\");
        System.out.println(" |  __/| |_| | | |__| (_) | | | \\__ \\ (_) | |  __/");
        System.out.println(" |_|    \\____|  \\____\\___/|_| |_|___/\\___/|_|\\___|");
        System.out.println();
        System.out.println(" PostgreSQL Monitoring Console v" + applicationVersion);
        System.out.println(" Starting server on " + host + ":" + port);
        if (noHistory) {
            System.out.println(" History sampling: DISABLED");
        }
        if (noAlerting) {
            System.out.println(" Alerting: DISABLED");
        }
        System.out.println();
    }
}
