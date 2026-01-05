package com.bovinemagnet.pgconsole.cli;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * CLI command to export the current effective configuration to various formats.
 * <p>
 * This command retrieves all resolved configuration values from the MicroProfile Config
 * system, including properties from application.properties, environment variables, and
 * system properties. The output can be formatted as Java properties, YAML, or environment
 * variable export statements.
 * <p>
 * By default, sensitive properties (passwords, secrets, tokens, etc.) are masked with
 * asterisks. Quarkus internal properties are excluded unless explicitly requested via
 * the --prefix option.
 * <p>
 * Example usage:
 * <pre>{@code
 * # Export all configuration to stdout in properties format
 * java -jar app.jar export-config
 *
 * # Export to a file in YAML format
 * java -jar app.jar export-config -o config.yaml --format yaml
 *
 * # Export only pg-console properties including sensitive values
 * java -jar app.jar export-config --prefix pg-console --include-sensitive
 *
 * # Export as environment variables
 * java -jar app.jar export-config --format env -o env.sh
 * }</pre>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see Config
 * @see ConfigProvider
 * @since 0.0.0
 */
@Command(name = "export-config", description = "Export current effective configuration as properties", mixinStandardHelpOptions = true)
public class ExportConfigCommand implements Runnable {

	/**
	 * Output file path for the exported configuration.
	 * <p>
	 * If not specified or blank, output is written to stdout. The file is created
	 * if it does not exist, or overwritten if it does.
	 */
	@Option(names = { "-o", "--output" }, description = "Output file path (default: stdout)")
	private String outputFile;

	/**
	 * Output format for the exported configuration.
	 * <p>
	 * Supported formats:
	 * <ul>
	 * <li>{@code properties} - Java properties format with dotted keys</li>
	 * <li>{@code yaml} - YAML format with hierarchical structure</li>
	 * <li>{@code env} - Shell export statements for environment variables</li>
	 * </ul>
	 * Defaults to "properties" if not specified or invalid.
	 */
	@Option(names = { "--format" }, description = "Output format: properties, yaml, env (default: properties)")
	private String format = "properties";

	/**
	 * Configuration property prefix filter.
	 * <p>
	 * When specified, only properties whose names start with this prefix are included
	 * in the output. For example, {@code --prefix pg-console} will export only properties
	 * beginning with "pg-console".
	 * <p>
	 * If null, all non-Quarkus properties are exported. Quarkus internal properties
	 * (starting with "quarkus.") are excluded by default unless this prefix is set
	 * to "quarkus".
	 */
	@Option(names = { "--prefix" }, description = "Filter by configuration prefix (e.g., 'pg-console')")
	private String prefix;

	/**
	 * Flag to show configuration source for each property.
	 * <p>
	 * Currently not implemented. Reserved for future functionality to display
	 * which configuration source (environment variable, properties file, etc.)
	 * provides each value.
	 */
	@Option(names = { "--show-sources" }, description = "Show configuration source for each property")
	private boolean showSources;

	/**
	 * Flag to include sensitive values in the output.
	 * <p>
	 * When {@code false} (default), properties containing sensitive keywords
	 * (password, secret, token, key, credential, auth) are masked with "********".
	 * When {@code true}, actual values are included.
	 * <p>
	 * <strong>Warning:</strong> Be careful when using this option as it will expose
	 * sensitive credentials in the output.
	 */
	@Option(names = { "--include-sensitive" }, description = "Include sensitive values (passwords, secrets)")
	private boolean includeSensitive;

	/**
	 * Executes the configuration export command.
	 * <p>
	 * Retrieves all configuration properties from the MicroProfile Config system,
	 * filters them according to command-line options, and outputs them in the
	 * requested format. The process includes:
	 * <ol>
	 * <li>Opening output destination (file or stdout)</li>
	 * <li>Collecting and filtering configuration properties</li>
	 * <li>Masking sensitive values if required</li>
	 * <li>Sorting properties alphabetically</li>
	 * <li>Formatting and writing output</li>
	 * </ol>
	 * <p>
	 * Exits with code 0 on success or code 1 if an exception occurs during export.
	 */
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

	/**
	 * Outputs configuration entries in Java properties format.
	 * <p>
	 * Writes properties in standard Java properties file format with dotted keys.
	 * Includes a header comment with generation timestamp and groups properties
	 * by their top-level prefix (e.g., "pg-console", "quarkus") with section
	 * headers for readability.
	 * <p>
	 * Special characters (backslashes, newlines, etc.) are escaped according to
	 * properties file conventions.
	 *
	 * @param out the PrintWriter to write formatted output to
	 * @param entries the sorted list of configuration entries to output
	 * @see #escapePropertyValue(String)
	 */
	private void outputProperties(PrintWriter out, List<ConfigEntry> entries) {
		out.println("# PG Console Configuration Export");
		out.println("# Generated: " + java.time.LocalDateTime.now());
		out.println();

		String currentSection = "";
		for (ConfigEntry entry : entries) {
			// Add section headers
			String section = entry.name.contains(".") ? entry.name.substring(0, entry.name.indexOf('.')) : "";
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

	/**
	 * Outputs configuration entries in YAML format.
	 * <p>
	 * Converts dotted property names (e.g., "pg-console.history.enabled") into
	 * YAML hierarchical structure with appropriate indentation. String values
	 * that could be misinterpreted as other YAML types (booleans, numbers, etc.)
	 * are quoted.
	 * <p>
	 * Includes a header comment with generation timestamp.
	 * <p>
	 * <strong>Note:</strong> This implementation creates a flat YAML structure
	 * where each property path is fully expanded. It does not merge sibling
	 * properties under common parent keys.
	 *
	 * @param out the PrintWriter to write formatted output to
	 * @param entries the sorted list of configuration entries to output
	 * @see #needsQuoting(String)
	 * @see #escapeYamlString(String)
	 */
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

	/**
	 * Outputs configuration entries as shell environment variable export statements.
	 * <p>
	 * Converts property names to environment variable naming convention by:
	 * <ul>
	 * <li>Converting to uppercase</li>
	 * <li>Replacing dots (.) with underscores (_)</li>
	 * <li>Replacing hyphens (-) with underscores (_)</li>
	 * </ul>
	 * <p>
	 * Values containing spaces or quotes are wrapped in double quotes with
	 * internal quotes escaped. Each entry is prefixed with "export " for
	 * direct sourcing in shell scripts.
	 * <p>
	 * Example output: {@code export PG_CONSOLE_HISTORY_ENABLED=true}
	 *
	 * @param out the PrintWriter to write formatted output to
	 * @param entries the sorted list of configuration entries to output
	 */
	private void outputEnv(PrintWriter out, List<ConfigEntry> entries) {
		out.println("# PG Console Configuration Export (Environment Variables)");
		out.println("# Generated: " + java.time.LocalDateTime.now());
		out.println();

		for (ConfigEntry entry : entries) {
			// Convert property name to environment variable format
			String envName = entry.name.toUpperCase().replace('.', '_').replace('-', '_');

			String value = entry.value;
			if (value.contains(" ") || value.contains("\"") || value.contains("'")) {
				value = "\"" + value.replace("\"", "\\\"") + "\"";
			}

			out.println("export " + envName + "=" + value);
		}
	}

	/**
	 * Determines whether a property name indicates sensitive data.
	 * <p>
	 * Checks if the property name (case-insensitive) contains any of the following
	 * keywords that commonly indicate sensitive configuration:
	 * <ul>
	 * <li>password</li>
	 * <li>secret</li>
	 * <li>token</li>
	 * <li>key</li>
	 * <li>credential</li>
	 * <li>auth</li>
	 * </ul>
	 * <p>
	 * This is used to determine which properties should be masked when
	 * {@link #includeSensitive} is {@code false}.
	 *
	 * @param name the property name to check
	 * @return {@code true} if the property is considered sensitive, {@code false} otherwise
	 */
	private boolean isSensitiveProperty(String name) {
		String lower = name.toLowerCase();
		return lower.contains("password") || lower.contains("secret") || lower.contains("token") || lower.contains("key") || lower.contains("credential") || lower.contains("auth");
	}

	/**
	 * Escapes special characters for Java properties file format.
	 * <p>
	 * Replaces the following characters with their escaped equivalents:
	 * <ul>
	 * <li>Backslash (\) to double backslash (\\)</li>
	 * <li>Newline (\n) to escaped newline (\\n)</li>
	 * <li>Carriage return (\r) to escaped carriage return (\\r)</li>
	 * <li>Tab (\t) to escaped tab (\\t)</li>
	 * </ul>
	 *
	 * @param value the raw property value to escape
	 * @return the escaped value suitable for properties file format
	 */
	private String escapePropertyValue(String value) {
		// Escape backslashes and newlines for .properties format
		return value.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
	}

	/**
	 * Escapes special characters for YAML string values.
	 * <p>
	 * Replaces the following characters with their escaped equivalents:
	 * <ul>
	 * <li>Backslash (\) to double backslash (\\)</li>
	 * <li>Double quote (") to escaped quote (\")</li>
	 * <li>Newline (\n) to escaped newline (\\n)</li>
	 * </ul>
	 *
	 * @param value the raw string value to escape
	 * @return the escaped value suitable for quoted YAML strings
	 */
	private String escapeYamlString(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
	}

	/**
	 * Determines whether a value needs quoting in YAML format.
	 * <p>
	 * Returns {@code true} if the value requires quotes to prevent misinterpretation
	 * by YAML parsers. Values that do NOT need quoting:
	 * <ul>
	 * <li>Boolean literals: "true", "false"</li>
	 * <li>Numeric values: integers and decimals (e.g., "42", "3.14", "-10")</li>
	 * </ul>
	 * <p>
	 * Values that DO need quoting:
	 * <ul>
	 * <li>Empty strings</li>
	 * <li>Strings containing colons (:) or hashes (#)</li>
	 * <li>Strings with leading or trailing spaces</li>
	 * <li>Strings containing quotes</li>
	 * </ul>
	 *
	 * @param value the value to check
	 * @return {@code true} if the value should be quoted in YAML output, {@code false} otherwise
	 */
	private boolean needsQuoting(String value) {
		if (value.isEmpty()) return true;
		if (value.equals("true") || value.equals("false")) return false;
		if (value.matches("-?\\d+(\\.\\d+)?")) return false;
		return value.contains(":") || value.contains("#") || value.startsWith(" ") || value.endsWith(" ") || value.contains("\"") || value.contains("'");
	}

	/**
	 * Immutable data carrier for a configuration property name-value pair.
	 * <p>
	 * Used internally to collect, sort, and format configuration entries
	 * during the export process.
	 *
	 * @param name the configuration property name (e.g., "pg-console.history.enabled")
	 * @param value the configuration property value (may be masked if sensitive)
	 */
	private record ConfigEntry(String name, String value) {}
}
