package com.bovinemagnet.pgconsole.cli;

import com.bovinemagnet.pgconsole.service.DataSourceManager;
import com.bovinemagnet.pgconsole.service.PostgresService;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

/**
 * CLI command to check database health and connectivity.
 * <p>
 * Validates:
 * <ul>
 *   <li>Database connectivity for configured instances</li>
 *   <li>pg_stat_statements extension availability</li>
 *   <li>Required permissions for monitoring queries</li>
 * </ul>
 * <p>
 * Exit codes:
 * <ul>
 *   <li>0 - All checks passed</li>
 *   <li>1 - One or more checks failed</li>
 * </ul>
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@Command(
        name = "health-check",
        description = "Test database connectivity and extension availability",
        mixinStandardHelpOptions = true
)
public class HealthCheckCommand implements Runnable {

    @Option(names = {"-i", "--instance"},
            description = "Specific instance to check (default: all instances)")
    private String instance;

    @Option(names = {"--verbose"},
            description = "Show detailed output")
    private boolean verbose;

    @Inject
    DataSourceManager dataSourceManager;

    @Inject
    PostgresService postgresService;

    private boolean allPassed = true;

    @Override
    public void run() {
        System.out.println();
        System.out.println("PG Console Health Check");
        System.out.println("========================");
        System.out.println();

        List<String> instances = dataSourceManager.getAvailableInstances();

        if (instance != null && !instance.isBlank()) {
            // Check specific instance
            if (!instances.contains(instance)) {
                System.out.println("ERROR: Instance '" + instance + "' not found in configuration");
                System.out.println("Available instances: " + String.join(", ", instances));
                System.exit(1);
            }
            checkInstance(instance);
        } else {
            // Check all instances
            System.out.println("Checking " + instances.size() + " configured instance(s)...");
            System.out.println();

            for (String inst : instances) {
                checkInstance(inst);
                System.out.println();
            }
        }

        // Summary
        System.out.println("========================");
        if (allPassed) {
            System.out.println("RESULT: All health checks PASSED");
            System.exit(0);
        } else {
            System.out.println("RESULT: One or more health checks FAILED");
            System.exit(1);
        }
    }

    private void checkInstance(String instanceName) {
        System.out.println("Instance: " + instanceName);
        System.out.println("------------------------");

        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            // Check 1: Basic connectivity
            checkConnectivity(ds, instanceName);

            // Check 2: PostgreSQL version
            checkPostgresVersion(ds);

            // Check 3: pg_stat_statements extension
            checkPgStatStatements(ds);

            // Check 4: Required permissions
            checkPermissions(ds);

        } catch (Exception e) {
            printFailed("Connection", e.getMessage());
            allPassed = false;
        }
    }

    private void checkConnectivity(DataSource ds, String instanceName) {
        try (Connection conn = ds.getConnection()) {
            if (conn.isValid(5)) {
                printPassed("Connectivity", "Connected successfully");
                if (verbose) {
                    System.out.println("    URL: " + conn.getMetaData().getURL());
                    System.out.println("    User: " + conn.getMetaData().getUserName());
                }
            } else {
                printFailed("Connectivity", "Connection not valid");
                allPassed = false;
            }
        } catch (Exception e) {
            printFailed("Connectivity", e.getMessage());
            allPassed = false;
        }
    }

    private void checkPostgresVersion(DataSource ds) {
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT version(), current_setting('server_version_num')::integer")) {

            if (rs.next()) {
                String version = rs.getString(1);
                int versionNum = rs.getInt(2);

                if (versionNum >= 120000) {
                    printPassed("PostgreSQL Version", extractVersion(version));
                } else if (versionNum >= 100000) {
                    printWarning("PostgreSQL Version", extractVersion(version) + " (recommended: 12+)");
                } else {
                    printFailed("PostgreSQL Version", extractVersion(version) + " (minimum: 10)");
                    allPassed = false;
                }
            }
        } catch (Exception e) {
            printFailed("PostgreSQL Version", e.getMessage());
            allPassed = false;
        }
    }

    private void checkPgStatStatements(DataSource ds) {
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT extname, extversion FROM pg_extension WHERE extname = 'pg_stat_statements'")) {

            if (rs.next()) {
                String version = rs.getString("extversion");
                printPassed("pg_stat_statements", "Installed (version " + version + ")");

                // Check if we can query it
                try (Statement stmt2 = conn.createStatement();
                     ResultSet rs2 = stmt2.executeQuery("SELECT count(*) FROM pg_stat_statements LIMIT 1")) {
                    if (rs2.next()) {
                        printPassed("pg_stat_statements access", "Query successful");
                    }
                } catch (Exception e) {
                    printFailed("pg_stat_statements access", e.getMessage());
                    allPassed = false;
                }
            } else {
                printFailed("pg_stat_statements", "Extension not installed");
                System.out.println("    To install: CREATE EXTENSION pg_stat_statements;");
                System.out.println("    Also add to postgresql.conf: shared_preload_libraries = 'pg_stat_statements'");
                allPassed = false;
            }
        } catch (Exception e) {
            printFailed("pg_stat_statements", e.getMessage());
            allPassed = false;
        }
    }

    private void checkPermissions(DataSource ds) {
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {

            // Check pg_stat_activity access
            try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM pg_stat_activity")) {
                printPassed("pg_stat_activity access", "Permitted");
            } catch (Exception e) {
                printFailed("pg_stat_activity access", "Denied - " + e.getMessage());
                allPassed = false;
            }

            // Check pg_locks access
            try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM pg_locks")) {
                printPassed("pg_locks access", "Permitted");
            } catch (Exception e) {
                printFailed("pg_locks access", "Denied - " + e.getMessage());
                allPassed = false;
            }

            // Check pg_stat_user_tables access
            try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM pg_stat_user_tables")) {
                printPassed("pg_stat_user_tables access", "Permitted");
            } catch (Exception e) {
                printFailed("pg_stat_user_tables access", "Denied - " + e.getMessage());
                allPassed = false;
            }

        } catch (Exception e) {
            printFailed("Permissions check", e.getMessage());
            allPassed = false;
        }
    }

    private String extractVersion(String fullVersion) {
        // Extract just "PostgreSQL X.Y" from full version string
        if (fullVersion.contains("PostgreSQL")) {
            int start = fullVersion.indexOf("PostgreSQL");
            int end = fullVersion.indexOf(" on ");
            if (end == -1) end = Math.min(fullVersion.length(), start + 20);
            return fullVersion.substring(start, end).trim();
        }
        return fullVersion;
    }

    private void printPassed(String check, String message) {
        System.out.println("  [PASS] " + check + ": " + message);
    }

    private void printFailed(String check, String message) {
        System.out.println("  [FAIL] " + check + ": " + message);
    }

    private void printWarning(String check, String message) {
        System.out.println("  [WARN] " + check + ": " + message);
    }
}
