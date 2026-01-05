package com.bovinemagnet.pgconsole.cli;

import com.bovinemagnet.pgconsole.service.DataSourceManager;
import com.bovinemagnet.pgconsole.service.PostgresService;
import jakarta.inject.Inject;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.sql.DataSource;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * CLI command to export a comprehensive PostgreSQL incident report snapshot.
 * <p>
 * This command generates detailed diagnostic reports capturing the current state
 * of a PostgreSQL database instance. The reports include server information,
 * current activity, lock contention, slow queries, connection statistics, and
 * database sizes. This tool is particularly useful for incident investigation,
 * performance troubleshooting, and capacity planning.
 * <p>
 * The command supports three output formats:
 * <ul>
 *   <li><strong>text</strong> - Human-readable plain text format with ASCII tables</li>
 *   <li><strong>markdown</strong> - Markdown format suitable for documentation systems</li>
 *   <li><strong>json</strong> - Structured JSON format for programmatic consumption</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>{@code
 * # Generate text report to stdout
 * pg-console export-report
 *
 * # Generate markdown report to file
 * pg-console export-report --format markdown -o incident-2024-01-15.md
 *
 * # Generate JSON report with full query text
 * pg-console export-report --format json --include-queries -o report.json
 *
 * # Generate report for specific instance, top 20 items
 * pg-console export-report -i production --top 20 -o prod-report.txt
 * }</pre>
 * <p>
 * The report includes the following sections:
 * <ul>
 *   <li>Server information (version, database, user, uptime, max connections)</li>
 *   <li>Current activity (connections by state, long-running queries)</li>
 *   <li>Lock information (blocking queries and deadlock candidates)</li>
 *   <li>Top N slow queries from pg_stat_statements</li>
 *   <li>Connection statistics per database (commits, rollbacks, cache hit ratio)</li>
 *   <li>Database sizes</li>
 * </ul>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see DataSourceManager
 * @see PostgresService
 * @since 0.0.0
 */
@Command(name = "export-report", description = "Generate incident report snapshot and exit", mixinStandardHelpOptions = true)
public class ExportReportCommand implements Runnable {

	/**
	 * Target PostgreSQL instance identifier.
	 * <p>
	 * References the instance name configured in the application properties.
	 * Defaults to "default" if not specified.
	 */
	@Option(names = { "-i", "--instance" }, description = "Target instance (default: default)")
	private String instance = "default";

	/**
	 * Output file path for the generated report.
	 * <p>
	 * If not specified or blank, the report is written to stdout.
	 * The file will be created or overwritten if it exists.
	 */
	@Option(names = { "-o", "--output" }, description = "Output file path (default: stdout)")
	private String outputFile;

	/**
	 * Output format for the report.
	 * <p>
	 * Supported formats:
	 * <ul>
	 *   <li>text - Plain text with ASCII tables (default)</li>
	 *   <li>markdown - Markdown format with tables</li>
	 *   <li>json - JSON format with structured data</li>
	 * </ul>
	 * Format is case-insensitive.
	 */
	@Option(names = { "--format" }, description = "Output format: text, markdown, json (default: text)")
	private String format = "text";

	/**
	 * Whether to include full query text in the report.
	 * <p>
	 * When enabled, query snippets are included in long-running query and
	 * slow query sections. Useful for detailed analysis but may produce
	 * verbose output.
	 */
	@Option(names = { "--include-queries" }, description = "Include full query text in report")
	private boolean includeQueries;

	/**
	 * Number of top items to include in each section.
	 * <p>
	 * Controls how many slow queries, long-running queries, and blocked
	 * queries appear in the report. Defaults to 10.
	 */
	@Option(names = { "--top" }, description = "Number of top items to include (default: 10)")
	private int topN = 10;

	/**
	 * Data source manager for obtaining database connections.
	 * <p>
	 * Provides access to configured PostgreSQL instances.
	 */
	@Inject
	DataSourceManager dataSourceManager;

	/**
	 * PostgreSQL service for database operations.
	 * <p>
	 * Provides high-level database query functionality.
	 */
	@Inject
	PostgresService postgresService;

	/**
	 * Print writer for output.
	 * <p>
	 * Initialised to either a file writer or stdout depending on configuration.
	 */
	private PrintWriter out;

	/**
	 * Executes the report generation command.
	 * <p>
	 * This method orchestrates the entire report generation process:
	 * <ol>
	 *   <li>Initialises output (file or stdout)</li>
	 *   <li>Obtains data source for the specified instance</li>
	 *   <li>Delegates to format-specific generation method</li>
	 *   <li>Closes output and exits with status code</li>
	 * </ol>
	 * <p>
	 * The application exits with status code 0 on success or 1 on failure.
	 * Error messages are written to stderr.
	 *
	 * @throws RuntimeException if report generation fails (exits before throwing)
	 */
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

	/**
	 * Generates the report in plain text format.
	 * <p>
	 * Produces a human-readable report with ASCII art headers and formatted
	 * tables. Includes all report sections with proper alignment and spacing.
	 * <p>
	 * Output includes:
	 * <ul>
	 *   <li>Header with timestamp and instance name</li>
	 *   <li>Server information (version, database, user, uptime)</li>
	 *   <li>Current activity (connection states, long-running queries)</li>
	 *   <li>Lock information (blocking queries)</li>
	 *   <li>Top N slow queries from pg_stat_statements</li>
	 *   <li>Connection statistics per database</li>
	 *   <li>Database sizes in human-readable format</li>
	 *   <li>Footer</li>
	 * </ul>
	 *
	 * @param ds the data source for the PostgreSQL instance
	 * @throws Exception if database queries fail or output writing fails
	 */
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

	/**
	 * Generates the report in Markdown format.
	 * <p>
	 * Produces a Markdown-formatted report suitable for documentation systems,
	 * wikis, or version control. Uses Markdown tables for structured data.
	 * <p>
	 * Output includes:
	 * <ul>
	 *   <li>Header with timestamp and instance name (bold)</li>
	 *   <li>Server information in a Markdown table</li>
	 *   <li>Current activity with connection state counts</li>
	 *   <li>Lock information placeholder</li>
	 *   <li>Top N slow queries in a Markdown table</li>
	 * </ul>
	 * <p>
	 * Note: Lock information is abbreviated in Markdown format and refers
	 * to the text format for full details.
	 *
	 * @param ds the data source for the PostgreSQL instance
	 * @throws Exception if database queries fail or output writing fails
	 */
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

	/**
	 * Generates the report in JSON format.
	 * <p>
	 * Produces a structured JSON report suitable for programmatic consumption,
	 * log aggregation systems, or automated analysis tools. The JSON structure
	 * is designed for easy parsing and integration with monitoring systems.
	 * <p>
	 * Output JSON structure:
	 * <pre>{@code
	 * {
	 *   "reportType": "incident",
	 *   "generated": "2024-01-15T10:30:00",
	 *   "instance": "production",
	 *   "server": {
	 *     "version": "PostgreSQL 14.2",
	 *     "database": "mydb",
	 *     "user": "postgres"
	 *   },
	 *   "activity": {
	 *     "active": 5,
	 *     "idle": 10
	 *   },
	 *   "slowQueries": [
	 *     {"queryId": 12345, "calls": 100, "meanTimeMs": 1250.5}
	 *   ]
	 * }
	 * }</pre>
	 *
	 * @param ds the data source for the PostgreSQL instance
	 * @throws Exception if database queries fail or output writing fails
	 */
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

	/**
	 * Prints server information section in text format.
	 * <p>
	 * Queries PostgreSQL system functions to retrieve and display:
	 * <ul>
	 *   <li>PostgreSQL version (extracted from version() function)</li>
	 *   <li>Current database name</li>
	 *   <li>Current user</li>
	 *   <li>Server address and port</li>
	 *   <li>Server start time (postmaster uptime)</li>
	 *   <li>Maximum connections allowed</li>
	 * </ul>
	 *
	 * @param conn active database connection
	 * @throws Exception if query execution fails
	 */
	private void printServerInfo(Connection conn) throws Exception {
		try (
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT version(), current_database(), current_user, " + "inet_server_addr(), inet_server_port(), " + "pg_postmaster_start_time(), " + "current_setting('max_connections')::int")
		) {
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

	/**
	 * Prints current activity section in text format.
	 * <p>
	 * Displays two subsections:
	 * <ol>
	 *   <li>Connection counts grouped by state (active, idle, idle in transaction, etc.)</li>
	 *   <li>Long-running queries (exceeding 30 seconds) with PID, user, and duration</li>
	 * </ol>
	 * <p>
	 * Long-running queries are sorted by start time (oldest first) and limited
	 * to the top N entries. If {@link #includeQueries} is enabled, query text
	 * snippets (first 100 characters) are included.
	 *
	 * @param conn active database connection
	 * @throws Exception if query execution fails
	 */
	private void printCurrentActivity(Connection conn) throws Exception {
		try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT state, count(*) FROM pg_stat_activity " + "WHERE backend_type = 'client backend' " + "GROUP BY state ORDER BY count(*) DESC")) {
			out.println("Connections by State:");
			while (rs.next()) {
				String state = rs.getString(1);
				if (state == null) state = "(null)";
				out.printf("  %-20s %d%n", state, rs.getInt(2));
			}
		}

		out.println();

		// Long running queries
		try (
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(
				"SELECT pid, usename, state, " +
					"EXTRACT(EPOCH FROM (now() - query_start))::int as duration_sec, " +
					"LEFT(query, 100) as query " +
					"FROM pg_stat_activity " +
					"WHERE state != 'idle' AND query_start < now() - interval '30 seconds' " +
					"ORDER BY query_start LIMIT " +
					topN
			)
		) {
			out.println("Long Running Queries (>30s):");
			boolean hasResults = false;
			while (rs.next()) {
				hasResults = true;
				out.printf("  PID: %d | User: %s | Duration: %ds%n", rs.getInt("pid"), rs.getString("usename"), rs.getInt("duration_sec"));
				if (includeQueries) {
					out.println("    Query: " + rs.getString("query") + "...");
				}
			}
			if (!hasResults) {
				out.println("  (none)");
			}
		}
	}

	/**
	 * Prints lock information section in text format.
	 * <p>
	 * Identifies and displays blocked queries by joining {@code pg_locks} with
	 * {@code pg_stat_activity} to show blocking relationships. For each blocked
	 * query, displays:
	 * <ul>
	 *   <li>Blocked process ID and user</li>
	 *   <li>Blocking process ID and user</li>
	 * </ul>
	 * <p>
	 * This query uses complex lock matching logic to identify contentious locks
	 * across all lock types (relation, transaction, tuple, etc.). Results are
	 * limited to the top N entries.
	 *
	 * @param conn active database connection
	 * @throws Exception if query execution fails
	 */
	private void printLockInfo(Connection conn) throws Exception {
		try (
			Statement stmt = conn.createStatement();
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
					"WHERE NOT blocked_locks.granted LIMIT " +
					topN
			)
		) {
			out.println("Blocked Queries:");
			boolean hasResults = false;
			while (rs.next()) {
				hasResults = true;
				out.printf("  Blocked: PID %d (%s) <- Blocking: PID %d (%s)%n", rs.getInt("blocked_pid"), rs.getString("blocked_user"), rs.getInt("blocking_pid"), rs.getString("blocking_user"));
			}
			if (!hasResults) {
				out.println("  (no blocked queries)");
			}
		}
	}

	/**
	 * Prints slow queries section in text format.
	 * <p>
	 * Retrieves query statistics from {@code pg_stat_statements} and displays
	 * the top N queries ordered by mean execution time. For each query, displays:
	 * <ul>
	 *   <li>Query ID (hash identifier)</li>
	 *   <li>Number of calls</li>
	 *   <li>Mean execution time in milliseconds</li>
	 *   <li>Total execution time in seconds</li>
	 *   <li>Query text preview (first 100 characters, if enabled)</li>
	 * </ul>
	 * <p>
	 * Requires the {@code pg_stat_statements} extension to be installed and enabled.
	 * Results are formatted in a columnar table layout.
	 *
	 * @param conn active database connection
	 * @throws Exception if query execution fails or pg_stat_statements is not available
	 */
	private void printSlowQueries(Connection conn) throws Exception {
		try (
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT queryid, calls, mean_exec_time, " + "total_exec_time, LEFT(query, 100) as query_preview " + "FROM pg_stat_statements " + "ORDER BY mean_exec_time DESC LIMIT " + topN)
		) {
			out.printf("%-12s %-10s %-15s %-15s%n", "QUERY ID", "CALLS", "MEAN TIME (ms)", "TOTAL TIME (s)");
			out.println("-".repeat(60));
			while (rs.next()) {
				out.printf("%-12d %-10d %-15.2f %-15.2f%n", rs.getLong("queryid"), rs.getLong("calls"), rs.getDouble("mean_exec_time"), rs.getDouble("total_exec_time") / 1000.0);
				if (includeQueries) {
					out.println("  " + rs.getString("query_preview") + "...");
				}
			}
		}
	}

	/**
	 * Prints connection statistics section in text format.
	 * <p>
	 * Retrieves per-database statistics from {@code pg_stat_database} and displays:
	 * <ul>
	 *   <li>Database name</li>
	 *   <li>Current number of connections (numbackends)</li>
	 *   <li>Transaction commits</li>
	 *   <li>Transaction rollbacks</li>
	 *   <li>Cache hit ratio percentage (buffer cache effectiveness)</li>
	 * </ul>
	 * <p>
	 * Results exclude template databases and are ordered by number of connections
	 * (most active databases first). Cache hit ratio is calculated as the percentage
	 * of block reads served from memory vs. disk.
	 *
	 * @param conn active database connection
	 * @throws Exception if query execution fails
	 */
	private void printConnectionStats(Connection conn) throws Exception {
		try (
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(
				"SELECT datname, numbackends, " +
					"xact_commit, xact_rollback, " +
					"blks_read, blks_hit, " +
					"CASE WHEN blks_read + blks_hit > 0 " +
					"  THEN round(100.0 * blks_hit / (blks_read + blks_hit), 2) " +
					"  ELSE 100 END as cache_hit_ratio " +
					"FROM pg_stat_database " +
					"WHERE datname NOT LIKE 'template%' " +
					"ORDER BY numbackends DESC"
			)
		) {
			out.printf("%-20s %-12s %-12s %-12s %-15s%n", "DATABASE", "CONNECTIONS", "COMMITS", "ROLLBACKS", "CACHE HIT %");
			out.println("-".repeat(75));
			while (rs.next()) {
				out.printf("%-20s %-12d %-12d %-12d %-15.2f%n", rs.getString("datname"), rs.getInt("numbackends"), rs.getLong("xact_commit"), rs.getLong("xact_rollback"), rs.getDouble("cache_hit_ratio"));
			}
		}
	}

	/**
	 * Prints database sizes section in text format.
	 * <p>
	 * Retrieves and displays the disk space usage for each database using
	 * PostgreSQL's {@code pg_database_size} function. Sizes are formatted
	 * in human-readable units (KB, MB, GB, TB) via {@code pg_size_pretty}.
	 * <p>
	 * Results exclude template databases and are ordered by size (largest first).
	 * This section is useful for capacity planning and identifying databases
	 * that may need cleanup or archival.
	 *
	 * @param conn active database connection
	 * @throws Exception if query execution fails
	 */
	private void printDatabaseSizes(Connection conn) throws Exception {
		try (
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT datname, pg_size_pretty(pg_database_size(datname)) as size " + "FROM pg_database " + "WHERE datname NOT LIKE 'template%' " + "ORDER BY pg_database_size(datname) DESC")
		) {
			out.printf("%-30s %-15s%n", "DATABASE", "SIZE");
			out.println("-".repeat(50));
			while (rs.next()) {
				out.printf("%-30s %-15s%n", rs.getString("datname"), rs.getString("size"));
			}
		}
	}

	/**
	 * Prints server information section in Markdown format.
	 * <p>
	 * Generates a Markdown table with server details including version,
	 * database name, and current user. Uses standard Markdown table syntax
	 * with header row and alignment.
	 *
	 * @param conn active database connection
	 * @throws Exception if query execution fails
	 */
	private void printServerInfoMarkdown(Connection conn) throws Exception {
		try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT version(), current_database(), current_user")) {
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

	/**
	 * Prints current activity section in Markdown format.
	 * <p>
	 * Generates a Markdown table showing connection counts grouped by state.
	 * Displays state names (active, idle, etc.) and their corresponding counts.
	 *
	 * @param conn active database connection
	 * @throws Exception if query execution fails
	 */
	private void printCurrentActivityMarkdown(Connection conn) throws Exception {
		try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT state, count(*) FROM pg_stat_activity " + "WHERE backend_type = 'client backend' " + "GROUP BY state ORDER BY count(*) DESC")) {
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

	/**
	 * Prints lock information section in Markdown format.
	 * <p>
	 * Currently outputs a placeholder message indicating that detailed lock
	 * analysis is available in the text format report. This is a simplified
	 * version for Markdown output.
	 *
	 * @param conn active database connection (not used in current implementation)
	 * @throws Exception if an error occurs
	 */
	private void printLockInfoMarkdown(Connection conn) throws Exception {
		out.println("_Lock analysis available in text format_");
		out.println();
	}

	/**
	 * Prints slow queries section in Markdown format.
	 * <p>
	 * Generates a Markdown table displaying the top N slow queries from
	 * {@code pg_stat_statements}. Shows query ID, call count, and mean
	 * execution time in milliseconds.
	 *
	 * @param conn active database connection
	 * @throws Exception if query execution fails or pg_stat_statements is not available
	 */
	private void printSlowQueriesMarkdown(Connection conn) throws Exception {
		try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT queryid, calls, mean_exec_time " + "FROM pg_stat_statements " + "ORDER BY mean_exec_time DESC LIMIT " + topN)) {
			out.println("| Query ID | Calls | Mean Time (ms) |");
			out.println("|----------|-------|----------------|");
			while (rs.next()) {
				out.printf("| %d | %d | %.2f |%n", rs.getLong(1), rs.getLong(2), rs.getDouble(3));
			}
			out.println();
		}
	}

	/**
	 * Prints server information section in JSON format.
	 * <p>
	 * Generates a JSON object with server details including version (first
	 * part before comma), database name, and current user. Output is formatted
	 * with proper indentation and comma placement for valid JSON structure.
	 *
	 * @param conn active database connection
	 * @throws Exception if query execution fails
	 */
	private void printServerInfoJson(Connection conn) throws Exception {
		try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT version(), current_database(), current_user")) {
			if (rs.next()) {
				out.println("  \"server\": {");
				out.println("    \"version\": \"" + escapeJson(rs.getString(1).split(",")[0]) + "\",");
				out.println("    \"database\": \"" + rs.getString(2) + "\",");
				out.println("    \"user\": \"" + rs.getString(3) + "\"");
				out.println("  },");
			}
		}
	}

	/**
	 * Prints current activity section in JSON format.
	 * <p>
	 * Generates a JSON object with connection states as keys and counts as
	 * values. Properly handles comma separation between entries to maintain
	 * valid JSON structure.
	 *
	 * @param conn active database connection
	 * @throws Exception if query execution fails
	 */
	private void printCurrentActivityJson(Connection conn) throws Exception {
		try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT state, count(*) FROM pg_stat_activity " + "WHERE backend_type = 'client backend' " + "GROUP BY state")) {
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

	/**
	 * Prints slow queries section in JSON format.
	 * <p>
	 * Generates a JSON array of slow query objects from {@code pg_stat_statements}.
	 * Each object contains queryId, calls, and meanTimeMs fields. Handles comma
	 * separation between array elements for valid JSON structure.
	 *
	 * @param conn active database connection
	 * @throws Exception if query execution fails or pg_stat_statements is not available
	 */
	private void printSlowQueriesJson(Connection conn) throws Exception {
		try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT queryid, calls, mean_exec_time " + "FROM pg_stat_statements " + "ORDER BY mean_exec_time DESC LIMIT " + topN)) {
			out.println("  \"slowQueries\": [");
			boolean first = true;
			while (rs.next()) {
				if (!first) out.println(",");
				out.print("    {\"queryId\": " + rs.getLong(1) + ", \"calls\": " + rs.getLong(2) + ", \"meanTimeMs\": " + rs.getDouble(3) + "}");
				first = false;
			}
			out.println();
			out.println("  ]");
		}
	}

	/**
	 * Escapes a string for safe inclusion in JSON output.
	 * <p>
	 * Handles the following escape sequences:
	 * <ul>
	 *   <li>Backslash ({@code \}) to {@code \\}</li>
	 *   <li>Double quote ({@code "}) to {@code \"}</li>
	 *   <li>Newline ({@code \n}) to {@code \\n}</li>
	 *   <li>Carriage return ({@code \r}) to {@code \\r}</li>
	 *   <li>Tab ({@code \t}) to {@code \\t}</li>
	 * </ul>
	 * <p>
	 * This ensures that string values containing special characters can be
	 * safely embedded in JSON without breaking the structure.
	 *
	 * @param s the string to escape
	 * @return the escaped string suitable for JSON
	 */
	private String escapeJson(String s) {
		return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
	}
}
