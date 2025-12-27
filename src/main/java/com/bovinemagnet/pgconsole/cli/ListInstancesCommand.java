package com.bovinemagnet.pgconsole.cli;

import com.bovinemagnet.pgconsole.model.InstanceInfo;
import com.bovinemagnet.pgconsole.service.DataSourceManager;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

/**
 * CLI command to list all configured PostgreSQL instances.
 * <p>
 * Displays instance name, display name, connection status, and optionally
 * detailed information about each instance.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@Command(
        name = "list-instances",
        description = "List all configured PostgreSQL instances with status",
        mixinStandardHelpOptions = true
)
public class ListInstancesCommand implements Runnable {

    @Option(names = {"--verbose", "-v"},
            description = "Show detailed instance information")
    private boolean verbose;

    @Option(names = {"--json"},
            description = "Output in JSON format")
    private boolean json;

    @Inject
    DataSourceManager dataSourceManager;

    @Override
    public void run() {
        List<InstanceInfo> instances = dataSourceManager.getInstanceInfoList();

        if (json) {
            printJson(instances);
        } else {
            printTable(instances);
        }
    }

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
        System.out.printf("%-15s %-20s %-10s %-15s%n",
                "NAME", "DISPLAY NAME", "STATUS", "VERSION");
        System.out.println("-".repeat(65));

        for (InstanceInfo info : instances) {
            String status = checkConnectivity(info.getName()) ? "CONNECTED" : "OFFLINE";
            String version = getVersion(info.getName());

            System.out.printf("%-15s %-20s %-10s %-15s%n",
                    info.getName(),
                    info.getDisplayName(),
                    status,
                    version);

            if (verbose && "CONNECTED".equals(status)) {
                printInstanceDetails(info.getName());
            }
        }

        System.out.println();
        System.out.println("Total: " + instances.size() + " instance(s)");
    }

    private void printInstanceDetails(String instanceName) {
        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);
            try (Connection conn = ds.getConnection();
                 Statement stmt = conn.createStatement()) {

                // Get database info
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT current_database(), current_user, inet_server_addr(), inet_server_port()")) {
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
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT count(*) FROM pg_stat_activity WHERE state = 'active'")) {
                    if (rs.next()) {
                        System.out.println("    Active connections: " + rs.getInt(1));
                    }
                }

                // Get database size
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT pg_size_pretty(pg_database_size(current_database()))")) {
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

    private String getVersion(String instanceName) {
        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);
            try (Connection conn = ds.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SHOW server_version")) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        } catch (Exception e) {
            return "N/A";
        }
        return "N/A";
    }

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
