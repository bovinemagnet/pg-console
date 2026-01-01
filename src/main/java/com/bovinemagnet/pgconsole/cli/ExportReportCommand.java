package com.bovinemagnet.pgconsole.cli;

import com.bovinemagnet.pgconsole.service.DataSourceManager;
import com.bovinemagnet.pgconsole.service.PostgresService;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import javax.sql.DataSource;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * CLI command to export an incident report snapshot.
 * <p>
 * Generates a comprehensive report including current activity,
 * locks, slow queries, and system metrics. Useful for incident
 * investigation and capacity planning.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@Command(
        name = "export-report",
        description = "Generate incident report snapshot and exit",
        mixinStandardHelpOptions = true
)
public class ExportReportCommand implements Runnable {

    @Option(names = {"-i", "--instance"},
            description = "Target instance (default: default)")
    private String instance = "default";

    @Option(names = {"-o", "--output"},
            description = "Output file path (default: stdout)")
    private String outputFile;

    @Option(names = {"--format"},
            description = "Output format: text, markdown, json (default: text)")
    private String format = "text";

    @Option(names = {"--include-queries"},
            description = "Include full query text in report")
    private boolean includeQueries;

    @Option(names = {"--top"},
            description = "Number of top items to include (default: 10)")
    private int topN = 10;

    @Inject
    DataSourceManager dataSourceManager;

    @Inject
    PostgresService postgresService;

    private PrintWriter out;

    @Override
    public void run() {
        try {
            // Setup output
            if (outputFile != null && !outputFile.isBlank()) {
                out = new PrintWriter(new FileWriter(outputFile));
            } else {
                out = new PrintWriter(System.out, true);
            }

            DataSource ds = dataSourceManager.getDataSource(instance);

            switch (format.toLowerCase()) {
                case "markdown" -> generateMarkdownReport(ds);
                case "json" -> generateJsonReport(ds);
                default -> generateTextReport(ds);
            }

            if (outputFile != null) {
                out.close();
                System.out.println("Report written to: " + outputFile);
            }

            System.exit(0);

        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(1);
        }
    }

    private void generateTextReport(DataSource ds) throws Exception {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        out.println("================================================================================");
        out.println("                         PG CONSOLE INCIDENT REPORT");
        out.println("================================================================================");
        out.println();
        out.println("Generated: " + timestamp);
        out.println("Instance:  " + instance);
        out.println();

        try (Connection conn = ds.getConnection()) {
            // Server Information
            out.println("SERVER INFORMATION");
            out.println("-".repeat(80));
            printServerInfo(conn);

            // Current Activity
            out.println();
            out.println("CURRENT ACTIVITY");
            out.println("-".repeat(80));
            printCurrentActivity(conn);

            // Lock Information
            out.println();
            out.println("LOCK INFORMATION");
            out.println("-".repeat(80));
            printLockInfo(conn);

            // Slow Queries
            out.println();
            out.println("TOP " + topN + " SLOW QUERIES");
            out.println("-".repeat(80));
            printSlowQueries(conn);

            // Connection Statistics
            out.println();
            out.println("CONNECTION STATISTICS");
            out.println("-".repeat(80));
            printConnectionStats(conn);

            // Database Sizes
            out.println();
            out.println("DATABASE SIZES");
            out.println("-".repeat(80));
            printDatabaseSizes(conn);
        }

        out.println();
        out.println("================================================================================");
        out.println("                              END OF REPORT");
        out.println("================================================================================");
    }

    private void generateMarkdownReport(DataSource ds) throws Exception {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        out.println("# PG Console Incident Report");
        out.println();
        out.println("**Generated:** " + timestamp);
        out.println("**Instance:** " + instance);
        out.println();

        try (Connection conn = ds.getConnection()) {
            out.println("## Server Information");
            out.println();
            printServerInfoMarkdown(conn);

            out.println("## Current Activity");
            out.println();
            printCurrentActivityMarkdown(conn);

            out.println("## Lock Information");
            out.println();
            printLockInfoMarkdown(conn);

            out.println("## Top " + topN + " Slow Queries");
            out.println();
            printSlowQueriesMarkdown(conn);
        }
    }

    private void generateJsonReport(DataSource ds) throws Exception {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        out.println("{");
        out.println("  \"reportType\": \"incident\",");
        out.println("  \"generated\": \"" + timestamp + "\",");
        out.println("  \"instance\": \"" + instance + "\",");

        try (Connection conn = ds.getConnection()) {
            printServerInfoJson(conn);
            printCurrentActivityJson(conn);
            printSlowQueriesJson(conn);
        }

        out.println("}");
    }

    private void printServerInfo(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT version(), current_database(), current_user, " +
                             "inet_server_addr(), inet_server_port(), " +
                             "pg_postmaster_start_time(), " +
                             "current_setting('max_connections')::int")) {
            if (rs.next()) {
                out.println("PostgreSQL Version: " + rs.getString(1).split(",")[0]);
                out.println("Current Database:   " + rs.getString(2));
                out.println("Current User:       " + rs.getString(3));
                out.println("Server Address:     " + rs.getString(4) + ":" + rs.getInt(5));
                out.println("Server Started:     " + rs.getTimestamp(6));
                out.println("Max Connections:    " + rs.getInt(7));
            }
        }
    }

    private void printCurrentActivity(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT state, count(*) FROM pg_stat_activity " +
                             "WHERE backend_type = 'client backend' " +
                             "GROUP BY state ORDER BY count(*) DESC")) {
            out.println("Connections by State:");
            while (rs.next()) {
                String state = rs.getString(1);
                if (state == null) state = "(null)";
                out.printf("  %-20s %d%n", state, rs.getInt(2));
            }
        }

        out.println();

        // Long running queries
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT pid, usename, state, " +
                             "EXTRACT(EPOCH FROM (now() - query_start))::int as duration_sec, " +
                             "LEFT(query, 100) as query " +
                             "FROM pg_stat_activity " +
                             "WHERE state != 'idle' AND query_start < now() - interval '30 seconds' " +
                             "ORDER BY query_start LIMIT " + topN)) {
            out.println("Long Running Queries (>30s):");
            boolean hasResults = false;
            while (rs.next()) {
                hasResults = true;
                out.printf("  PID: %d | User: %s | Duration: %ds%n",
                        rs.getInt("pid"),
                        rs.getString("usename"),
                        rs.getInt("duration_sec"));
                if (includeQueries) {
                    out.println("    Query: " + rs.getString("query") + "...");
                }
            }
            if (!hasResults) {
                out.println("  (none)");
            }
        }
    }

    private void printLockInfo(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT blocked_locks.pid AS blocked_pid, " +
                             "blocked_activity.usename AS blocked_user, " +
                             "blocking_locks.pid AS blocking_pid, " +
                             "blocking_activity.usename AS blocking_user, " +
                             "blocked_activity.query AS blocked_statement " +
                             "FROM pg_catalog.pg_locks blocked_locks " +
                             "JOIN pg_catalog.pg_stat_activity blocked_activity " +
                             "  ON blocked_activity.pid = blocked_locks.pid " +
                             "JOIN pg_catalog.pg_locks blocking_locks " +
                             "  ON blocking_locks.locktype = blocked_locks.locktype " +
                             "  AND blocking_locks.DATABASE IS NOT DISTINCT FROM blocked_locks.DATABASE " +
                             "  AND blocking_locks.relation IS NOT DISTINCT FROM blocked_locks.relation " +
                             "  AND blocking_locks.page IS NOT DISTINCT FROM blocked_locks.page " +
                             "  AND blocking_locks.tuple IS NOT DISTINCT FROM blocked_locks.tuple " +
                             "  AND blocking_locks.virtualxid IS NOT DISTINCT FROM blocked_locks.virtualxid " +
                             "  AND blocking_locks.transactionid IS NOT DISTINCT FROM blocked_locks.transactionid " +
                             "  AND blocking_locks.classid IS NOT DISTINCT FROM blocked_locks.classid " +
                             "  AND blocking_locks.objid IS NOT DISTINCT FROM blocked_locks.objid " +
                             "  AND blocking_locks.objsubid IS NOT DISTINCT FROM blocked_locks.objsubid " +
                             "  AND blocking_locks.pid != blocked_locks.pid " +
                             "JOIN pg_catalog.pg_stat_activity blocking_activity " +
                             "  ON blocking_activity.pid = blocking_locks.pid " +
                             "WHERE NOT blocked_locks.granted LIMIT " + topN)) {
            out.println("Blocked Queries:");
            boolean hasResults = false;
            while (rs.next()) {
                hasResults = true;
                out.printf("  Blocked: PID %d (%s) <- Blocking: PID %d (%s)%n",
                        rs.getInt("blocked_pid"),
                        rs.getString("blocked_user"),
                        rs.getInt("blocking_pid"),
                        rs.getString("blocking_user"));
            }
            if (!hasResults) {
                out.println("  (no blocked queries)");
            }
        }
    }

    private void printSlowQueries(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT queryid, calls, mean_exec_time, " +
                             "total_exec_time, LEFT(query, 100) as query_preview " +
                             "FROM pg_stat_statements " +
                             "ORDER BY mean_exec_time DESC LIMIT " + topN)) {
            out.printf("%-12s %-10s %-15s %-15s%n",
                    "QUERY ID", "CALLS", "MEAN TIME (ms)", "TOTAL TIME (s)");
            out.println("-".repeat(60));
            while (rs.next()) {
                out.printf("%-12d %-10d %-15.2f %-15.2f%n",
                        rs.getLong("queryid"),
                        rs.getLong("calls"),
                        rs.getDouble("mean_exec_time"),
                        rs.getDouble("total_exec_time") / 1000.0);
                if (includeQueries) {
                    out.println("  " + rs.getString("query_preview") + "...");
                }
            }
        }
    }

    private void printConnectionStats(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT datname, numbackends, " +
                             "xact_commit, xact_rollback, " +
                             "blks_read, blks_hit, " +
                             "CASE WHEN blks_read + blks_hit > 0 " +
                             "  THEN round(100.0 * blks_hit / (blks_read + blks_hit), 2) " +
                             "  ELSE 100 END as cache_hit_ratio " +
                             "FROM pg_stat_database " +
                             "WHERE datname NOT LIKE 'template%' " +
                             "ORDER BY numbackends DESC")) {
            out.printf("%-20s %-12s %-12s %-12s %-15s%n",
                    "DATABASE", "CONNECTIONS", "COMMITS", "ROLLBACKS", "CACHE HIT %");
            out.println("-".repeat(75));
            while (rs.next()) {
                out.printf("%-20s %-12d %-12d %-12d %-15.2f%n",
                        rs.getString("datname"),
                        rs.getInt("numbackends"),
                        rs.getLong("xact_commit"),
                        rs.getLong("xact_rollback"),
                        rs.getDouble("cache_hit_ratio"));
            }
        }
    }

    private void printDatabaseSizes(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT datname, pg_size_pretty(pg_database_size(datname)) as size " +
                             "FROM pg_database " +
                             "WHERE datname NOT LIKE 'template%' " +
                             "ORDER BY pg_database_size(datname) DESC")) {
            out.printf("%-30s %-15s%n", "DATABASE", "SIZE");
            out.println("-".repeat(50));
            while (rs.next()) {
                out.printf("%-30s %-15s%n",
                        rs.getString("datname"),
                        rs.getString("size"));
            }
        }
    }

    // Markdown format methods
    private void printServerInfoMarkdown(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT version(), current_database(), current_user")) {
            if (rs.next()) {
                out.println("| Property | Value |");
                out.println("|----------|-------|");
                out.println("| Version | " + rs.getString(1).split(",")[0] + " |");
                out.println("| Database | " + rs.getString(2) + " |");
                out.println("| User | " + rs.getString(3) + " |");
                out.println();
            }
        }
    }

    private void printCurrentActivityMarkdown(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT state, count(*) FROM pg_stat_activity " +
                             "WHERE backend_type = 'client backend' " +
                             "GROUP BY state ORDER BY count(*) DESC")) {
            out.println("| State | Count |");
            out.println("|-------|-------|");
            while (rs.next()) {
                String state = rs.getString(1);
                if (state == null) state = "(null)";
                out.println("| " + state + " | " + rs.getInt(2) + " |");
            }
            out.println();
        }
    }

    private void printLockInfoMarkdown(Connection conn) throws Exception {
        out.println("_Lock analysis available in text format_");
        out.println();
    }

    private void printSlowQueriesMarkdown(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT queryid, calls, mean_exec_time " +
                             "FROM pg_stat_statements " +
                             "ORDER BY mean_exec_time DESC LIMIT " + topN)) {
            out.println("| Query ID | Calls | Mean Time (ms) |");
            out.println("|----------|-------|----------------|");
            while (rs.next()) {
                out.printf("| %d | %d | %.2f |%n",
                        rs.getLong(1),
                        rs.getLong(2),
                        rs.getDouble(3));
            }
            out.println();
        }
    }

    // JSON format methods
    private void printServerInfoJson(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT version(), current_database(), current_user")) {
            if (rs.next()) {
                out.println("  \"server\": {");
                out.println("    \"version\": \"" + escapeJson(rs.getString(1).split(",")[0]) + "\",");
                out.println("    \"database\": \"" + rs.getString(2) + "\",");
                out.println("    \"user\": \"" + rs.getString(3) + "\"");
                out.println("  },");
            }
        }
    }

    private void printCurrentActivityJson(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT state, count(*) FROM pg_stat_activity " +
                             "WHERE backend_type = 'client backend' " +
                             "GROUP BY state")) {
            out.println("  \"activity\": {");
            boolean first = true;
            while (rs.next()) {
                if (!first) out.println(",");
                String state = rs.getString(1);
                if (state == null) state = "null";
                out.print("    \"" + state + "\": " + rs.getInt(2));
                first = false;
            }
            out.println();
            out.println("  },");
        }
    }

    private void printSlowQueriesJson(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT queryid, calls, mean_exec_time " +
                             "FROM pg_stat_statements " +
                             "ORDER BY mean_exec_time DESC LIMIT " + topN)) {
            out.println("  \"slowQueries\": [");
            boolean first = true;
            while (rs.next()) {
                if (!first) out.println(",");
                out.print("    {\"queryId\": " + rs.getLong(1) +
                        ", \"calls\": " + rs.getLong(2) +
                        ", \"meanTimeMs\": " + rs.getDouble(3) + "}");
                first = false;
            }
            out.println();
            out.println("  ]");
        }
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
