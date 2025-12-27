package com.bovinemagnet.pgconsole.cli;

import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * CLI command to export the current effective configuration.
 * <p>
 * Exports all resolved configuration values including those from
 * environment variables and system properties. Useful for debugging
 * and documenting current settings.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@Command(
        name = "export-config",
        description = "Export current effective configuration as properties",
        mixinStandardHelpOptions = true
)
public class ExportConfigCommand implements Runnable {

    @Option(names = {"-o", "--output"},
            description = "Output file path (default: stdout)")
    private String outputFile;

    @Option(names = {"--format"},
            description = "Output format: properties, yaml, env (default: properties)")
    private String format = "properties";

    @Option(names = {"--prefix"},
            description = "Filter by configuration prefix (e.g., 'pg-console')")
    private String prefix;

    @Option(names = {"--show-sources"},
            description = "Show configuration source for each property")
    private boolean showSources;

    @Option(names = {"--include-sensitive"},
            description = "Include sensitive values (passwords, secrets)")
    private boolean includeSensitive;

    @Override
    public void run() {
        try {
            PrintWriter out;
            if (outputFile != null && !outputFile.isBlank()) {
                out = new PrintWriter(new FileWriter(outputFile));
            } else {
                out = new PrintWriter(System.out, true);
            }

            Config config = ConfigProvider.getConfig();

            // Collect properties
            List<ConfigEntry> entries = new ArrayList<>();

            for (String name : config.getPropertyNames()) {
                // Filter by prefix if specified
                if (prefix != null && !name.startsWith(prefix)) {
                    continue;
                }

                // Skip internal Quarkus properties unless explicitly requested
                if (name.startsWith("quarkus.") && prefix == null) {
                    continue;
                }

                String value = config.getOptionalValue(name, String.class).orElse("");

                // Mask sensitive values
                if (!includeSensitive && isSensitiveProperty(name)) {
                    value = "********";
                }

                entries.add(new ConfigEntry(name, value));
            }

            // Sort by name
            entries.sort(Comparator.comparing(e -> e.name));

            // Output in requested format
            switch (format.toLowerCase()) {
                case "yaml" -> outputYaml(out, entries);
                case "env" -> outputEnv(out, entries);
                default -> outputProperties(out, entries);
            }

            if (outputFile != null) {
                out.close();
                System.out.println("Configuration written to: " + outputFile);
            }

            System.exit(0);

        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(1);
        }
    }

    private void outputProperties(PrintWriter out, List<ConfigEntry> entries) {
        out.println("# PG Console Configuration Export");
        out.println("# Generated: " + java.time.LocalDateTime.now());
        out.println();

        String currentSection = "";
        for (ConfigEntry entry : entries) {
            // Add section headers
            String section = entry.name.contains(".") ?
                    entry.name.substring(0, entry.name.indexOf('.')) : "";
            if (!section.equals(currentSection)) {
                if (!currentSection.isEmpty()) {
                    out.println();
                }
                out.println("# " + section);
                currentSection = section;
            }

            out.println(entry.name + "=" + escapePropertyValue(entry.value));
        }
    }

    private void outputYaml(PrintWriter out, List<ConfigEntry> entries) {
        out.println("# PG Console Configuration Export");
        out.println("# Generated: " + java.time.LocalDateTime.now());
        out.println();

        for (ConfigEntry entry : entries) {
            // Convert dotted property name to YAML structure
            String[] parts = entry.name.split("\\.");
            StringBuilder indent = new StringBuilder();

            for (int i = 0; i < parts.length - 1; i++) {
                out.println(indent + parts[i] + ":");
                indent.append("  ");
            }

            String lastPart = parts[parts.length - 1];
            String value = entry.value;

            // Quote strings that might be interpreted as other types
            if (needsQuoting(value)) {
                value = "\"" + escapeYamlString(value) + "\"";
            }

            out.println(indent + lastPart + ": " + value);
        }
    }

    private void outputEnv(PrintWriter out, List<ConfigEntry> entries) {
        out.println("# PG Console Configuration Export (Environment Variables)");
        out.println("# Generated: " + java.time.LocalDateTime.now());
        out.println();

        for (ConfigEntry entry : entries) {
            // Convert property name to environment variable format
            String envName = entry.name
                    .toUpperCase()
                    .replace('.', '_')
                    .replace('-', '_');

            String value = entry.value;
            if (value.contains(" ") || value.contains("\"") || value.contains("'")) {
                value = "\"" + value.replace("\"", "\\\"") + "\"";
            }

            out.println("export " + envName + "=" + value);
        }
    }

    private boolean isSensitiveProperty(String name) {
        String lower = name.toLowerCase();
        return lower.contains("password") ||
                lower.contains("secret") ||
                lower.contains("token") ||
                lower.contains("key") ||
                lower.contains("credential") ||
                lower.contains("auth");
    }

    private String escapePropertyValue(String value) {
        // Escape backslashes and newlines for .properties format
        return value
                .replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String escapeYamlString(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }

    private boolean needsQuoting(String value) {
        if (value.isEmpty()) return true;
        if (value.equals("true") || value.equals("false")) return false;
        if (value.matches("-?\\d+(\\.\\d+)?")) return false;
        return value.contains(":") || value.contains("#") ||
                value.startsWith(" ") || value.endsWith(" ") ||
                value.contains("\"") || value.contains("'");
    }

    private record ConfigEntry(String name, String value) {}
}
