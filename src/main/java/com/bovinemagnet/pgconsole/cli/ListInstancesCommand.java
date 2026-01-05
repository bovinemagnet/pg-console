package com.bovinemagnet.pgconsole.cli;

import com.bovinemagnet.pgconsole.model.InstanceInfo;
import com.bovinemagnet.pgconsole.service.DataSourceManager;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * CLI command to list all configured PostgreSQL instances with their connection status.
 * <p>
 * This command queries the {@link DataSourceManager} to retrieve all configured PostgreSQL
 * instances and displays their current status, including connectivity and version information.
 * Output can be formatted as either a human-readable table or JSON for programmatic consumption.
 * <p>
 * The command supports two output modes:
 * <ul>
 *   <li><strong>Table mode</strong> (default) - Displays a formatted table with instance name,
 *       display name, connection status, and PostgreSQL version</li>
 *   <li><strong>JSON mode</strong> - Outputs structured JSON containing the same information
 *       plus a total count of instances</li>
 * </ul>
 * <p>
 * When verbose mode is enabled, additional details are displayed for each connected instance:
 * <ul>
 *   <li>Current database name</li>
 *   <li>Connected user</li>
 *   <li>Server host and port</li>
 *   <li>Active connection count</li>
 *   <li>Database size</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>{@code
 * # List all instances with basic information
 * pg-console list-instances
 *
 * # Show detailed information for connected instances
 * pg-console list-instances --verbose
 *
 * # Output in JSON format
 * pg-console list-instances --json
 * }</pre>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see DataSourceManager
 * @see InstanceInfo
 * @since 0.0.0
 */
@Command(name = "list-instances", description = "List all configured PostgreSQL instances with status", mixinStandardHelpOptions = true)
public class ListInstancesCommand implements Runnable {

	/**
	 * Enables verbose output mode with detailed instance information.
	 * <p>
	 * When enabled, the command displays additional details for each connected instance,
	 * including database name, connected user, server address, active connections, and
	 * database size. Only applicable in table output mode (not JSON).
	 */
	@Option(names = { "--verbose", "-v" }, description = "Show detailed instance information")
	private boolean verbose;

	/**
	 * Enables JSON output format instead of the default table format.
	 * <p>
	 * When enabled, the command outputs a JSON object containing an array of instances
	 * with their properties and a total count. Useful for scripting and programmatic
	 * consumption of instance information.
	 */
	@Option(names = { "--json" }, description = "Output in JSON format")
	private boolean json;

	/**
	 * Manages data sources for all configured PostgreSQL instances.
	 * <p>
	 * Provides access to instance metadata and JDBC connections for querying
	 * instance status and details.
	 */
	@Inject
	DataSourceManager dataSourceManager;

	/**
	 * Executes the list-instances command.
	 * <p>
	 * Retrieves all configured PostgreSQL instances from the {@link DataSourceManager}
	 * and outputs their information in either table or JSON format based on the
	 * {@code --json} flag.
	 */
	@Override
	public void run() {
		List<InstanceInfo> instances = dataSourceManager.getInstanceInfoList();

		if (json) {
			printJson(instances);
		} else {
			printTable(instances);
		}
	}

	/**
	 * Prints instance information in a formatted table to standard output.
	 * <p>
	 * Displays a table with columns for instance name, display name, connection status,
	 * and PostgreSQL version. If the {@code --verbose} flag is enabled, additional
	 * detailed information is printed for each connected instance.
	 * <p>
	 * When no instances are configured, displays helpful configuration instructions
	 * referencing the required application.properties settings.
	 *
	 * @param instances the list of instances to display, never null
	 * @see #printInstanceDetails(String)
	 */
	private void printTable(List<InstanceInfo> instances) {
		System.out.println();
		System.out.println("Configured PostgreSQL Instances");
		System.out.println("================================");
		System.out.println();

		if (instances.isEmpty()) {
			System.out.println("No instances configured.");
			System.out.println();
			System.out.println("Configure instances in application.properties:");
			System.out.println("  pg-console.instances=default,production,staging");
			System.out.println("  quarkus.datasource.production.jdbc.url=jdbc:postgresql://...");
			return;
		}

		// Print header
		System.out.printf("%-15s %-20s %-10s %-15s%n", "NAME", "DISPLAY NAME", "STATUS", "VERSION");
		System.out.println("-".repeat(65));

		for (InstanceInfo info : instances) {
			String status = checkConnectivity(info.getName()) ? "CONNECTED" : "OFFLINE";
			String version = getVersion(info.getName());

			System.out.printf("%-15s %-20s %-10s %-15s%n", info.getName(), info.getDisplayName(), status, version);

			if (verbose && "CONNECTED".equals(status)) {
				printInstanceDetails(info.getName());
			}
		}

		System.out.println();
		System.out.println("Total: " + instances.size() + " instance(s)");
	}

	/**
	 * Prints detailed information for a specific PostgreSQL instance.
	 * <p>
	 * Queries the instance database to retrieve and display:
	 * <ul>
	 *   <li>Current database name</li>
	 *   <li>Connected username</li>
	 *   <li>Server host and port (if available)</li>
	 *   <li>Number of active connections</li>
	 *   <li>Total database size in human-readable format</li>
	 * </ul>
	 * <p>
	 * This method is only called when verbose mode is enabled and the instance
	 * is successfully connected. If any error occurs during detail retrieval,
	 * an error message is printed instead.
	 *
	 * @param instanceName the name of the instance to query, must not be null
	 */
	private void printInstanceDetails(String instanceName) {
		try {
			DataSource ds = dataSourceManager.getDataSource(instanceName);
			try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement()) {
				// Get database info
				try (ResultSet rs = stmt.executeQuery("SELECT current_database(), current_user, inet_server_addr(), inet_server_port()")) {
					if (rs.next()) {
						System.out.println("    Database: " + rs.getString(1));
						System.out.println("    User: " + rs.getString(2));
						String host = rs.getString(3);
						int port = rs.getInt(4);
						if (host != null) {
							System.out.println("    Server: " + host + ":" + port);
						}
					}
				}

				// Get active connections count
				try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM pg_stat_activity WHERE state = 'active'")) {
					if (rs.next()) {
						System.out.println("    Active connections: " + rs.getInt(1));
					}
				}

				// Get database size
				try (ResultSet rs = stmt.executeQuery("SELECT pg_size_pretty(pg_database_size(current_database()))")) {
					if (rs.next()) {
						System.out.println("    Database size: " + rs.getString(1));
					}
				}

				System.out.println();
			}
		} catch (Exception e) {
			System.out.println("    Error getting details: " + e.getMessage());
			System.out.println();
		}
	}

	/**
	 * Checks whether a PostgreSQL instance is currently reachable and accepting connections.
	 * <p>
	 * Attempts to obtain a connection from the instance's data source and validates it
	 * with a 5-second timeout. Returns {@code true} if the connection is valid, {@code false}
	 * if the instance is unreachable or any error occurs during connection.
	 * <p>
	 * This method is safe to call for any instance and will not throw exceptions.
	 *
	 * @param instanceName the name of the instance to check, must not be null
	 * @return {@code true} if the instance is connected and responsive, {@code false} otherwise
	 */
	private boolean checkConnectivity(String instanceName) {
		try {
			DataSource ds = dataSourceManager.getDataSource(instanceName);
			try (Connection conn = ds.getConnection()) {
				return conn.isValid(5);
			}
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Retrieves the PostgreSQL server version for a specific instance.
	 * <p>
	 * Executes the {@code SHOW server_version} SQL command to obtain the version string
	 * from the PostgreSQL server. Returns "N/A" if the instance is unreachable or any
	 * error occurs during the query.
	 * <p>
	 * The version string typically includes the PostgreSQL version number and may include
	 * additional information such as the build platform.
	 *
	 * @param instanceName the name of the instance to query, must not be null
	 * @return the PostgreSQL version string, or "N/A" if the version cannot be determined
	 */
	private String getVersion(String instanceName) {
		try {
			DataSource ds = dataSourceManager.getDataSource(instanceName);
			try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SHOW server_version")) {
				if (rs.next()) {
					return rs.getString(1);
				}
			}
		} catch (Exception e) {
			return "N/A";
		}
		return "N/A";
	}

	/**
	 * Prints instance information in JSON format to standard output.
	 * <p>
	 * Outputs a JSON object containing an "instances" array with each instance's metadata
	 * and a "total" count. Each instance object includes:
	 * <ul>
	 *   <li>{@code name} - The instance identifier</li>
	 *   <li>{@code displayName} - The human-readable display name</li>
	 *   <li>{@code connected} - Boolean indicating connection status</li>
	 *   <li>{@code version} - PostgreSQL version string or "N/A"</li>
	 * </ul>
	 * <p>
	 * The JSON output is manually constructed for simplicity and to avoid external
	 * dependencies on JSON libraries for this CLI command.
	 * <p>
	 * Example output:
	 * <pre>{@code
	 * {
	 *   "instances": [
	 *     {
	 *       "name": "default",
	 *       "displayName": "Local PostgreSQL",
	 *       "connected": true,
	 *       "version": "14.5"
	 *     }
	 *   ],
	 *   "total": 1
	 * }
	 * }</pre>
	 *
	 * @param instances the list of instances to output, never null
	 */
	private void printJson(List<InstanceInfo> instances) {
		StringBuilder sb = new StringBuilder();
		sb.append("{\n  \"instances\": [\n");

		for (int i = 0; i < instances.size(); i++) {
			InstanceInfo info = instances.get(i);
			boolean connected = checkConnectivity(info.getName());
			String version = getVersion(info.getName());

			sb.append("    {\n");
			sb.append("      \"name\": \"").append(info.getName()).append("\",\n");
			sb.append("      \"displayName\": \"").append(info.getDisplayName()).append("\",\n");
			sb.append("      \"connected\": ").append(connected).append(",\n");
			sb.append("      \"version\": \"").append(version).append("\"\n");
			sb.append("    }");

			if (i < instances.size() - 1) {
				sb.append(",");
			}
			sb.append("\n");
		}

		sb.append("  ],\n");
		sb.append("  \"total\": ").append(instances.size()).append("\n");
		sb.append("}\n");

		System.out.println(sb);
	}
}
