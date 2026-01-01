package com.bovinemagnet.pgconsole.cli;

import com.bovinemagnet.pgconsole.service.DataSourceManager;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import javax.sql.DataSource;
import java.io.Console;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * CLI command to reset pg_stat_statements counters.
 * <p>
 * This is a destructive operation that clears all accumulated
 * query statistics. Requires confirmation unless --force is used.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@Command(
        name = "reset-stats",
        description = "Reset pg_stat_statements counters (requires confirmation)",
        mixinStandardHelpOptions = true
)
public class ResetStatsCommand implements Runnable {

    @Option(names = {"-i", "--instance"},
            description = "Target instance (default: default)")
    private String instance = "default";

    @Option(names = {"--force", "-f"},
            description = "Skip confirmation prompt")
    private boolean force;

    @Option(names = {"--all"},
            description = "Reset statistics for all databases")
    private boolean all;

    @Option(names = {"--database", "-d"},
            description = "Reset statistics for specific database only")
    private String database;

    @Inject
    DataSourceManager dataSourceManager;

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

            try (Connection conn = ds.getConnection();
                 Statement stmt = conn.createStatement()) {

                if (database != null && !database.isBlank()) {
                    // Reset for specific database
                    String sql = "SELECT pg_stat_statements_reset(NULL, " +
                            "(SELECT oid FROM pg_database WHERE datname = '" + database + "'), NULL)";
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

    private void showStatsSummary(DataSource ds) {
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {

            // Get statement count and top consumers
            System.out.println("Current Statistics Summary:");
            System.out.println("--------------------------");

            try (ResultSet rs = stmt.executeQuery(
                    "SELECT count(*) as total_statements, " +
                            "sum(calls) as total_calls, " +
                            "sum(total_exec_time) as total_time_ms " +
                            "FROM pg_stat_statements")) {
                if (rs.next()) {
                    System.out.printf("  Total statements: %,d%n", rs.getLong("total_statements"));
                    System.out.printf("  Total calls: %,d%n", rs.getLong("total_calls"));
                    System.out.printf("  Total execution time: %,.2f seconds%n",
                            rs.getDouble("total_time_ms") / 1000.0);
                }
            }

            // Show stats age if available
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT stats_reset FROM pg_stat_statements_info")) {
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
