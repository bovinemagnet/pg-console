package com.bovinemagnet.pgconsole.resource;

import com.bovinemagnet.pgconsole.config.InstanceConfig;
import com.bovinemagnet.pgconsole.model.*;
import com.bovinemagnet.pgconsole.service.*;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Resource for cross-database schema comparison functionality.
 * <p>
 * Provides web UI endpoints for comparing schemas across different
 * databases on the same or different PostgreSQL instances.
 * <p>
 * Example comparisons:
 * <ul>
 *   <li>prod1.public vs prod2.public on same instance</li>
 *   <li>dev:myapp.public vs staging:myapp.public across instances</li>
 * </ul>
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@Path("/database-diff")
public class DatabaseDiffResource {

    private static final Logger LOG = Logger.getLogger(DatabaseDiffResource.class);

    @Inject
    DatabaseDiffService diffService;

    @Inject
    DataSourceManager dataSourceManager;

    @Inject
    FeatureToggleService featureToggleService;

    @Inject
    PdfExportService pdfExportService;

    @Inject
    InstanceConfig config;

    @Inject
    Template databaseDiff;

    @Inject
    Template databaseDiffResult;

    /**
     * Main database diff page.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index(@QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("database-diff");

        List<String> instances = dataSourceManager.getAvailableInstances();
        String currentInstance = instances.isEmpty() ? "default" :
                (instances.contains(instance) ? instance : instances.get(0));
        List<String> databases = diffService.getDatabases(currentInstance);
        String currentDatabase = databases.isEmpty() ? "" : databases.get(0);
        List<String> schemas = currentDatabase.isEmpty() ? List.of() :
                diffService.getSchemasForDatabase(currentInstance, currentDatabase);

        return databaseDiff
                .data("instances", instances)
                .data("databases", databases)
                .data("schemas", schemas)
                .data("currentInstance", currentInstance)
                .data("currentDatabase", currentDatabase)
                .data("filterPresets", ComparisonFilter.FilterPreset.values())
                .data("schemaEnabled", config.schema().enabled())
                .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * HTMX endpoint: Get databases for selected instance.
     */
    @GET
    @Path("/databases")
    @Produces(MediaType.TEXT_HTML)
    public String getDatabases(@QueryParam("instance") String instance) {
        featureToggleService.requirePageEnabled("database-diff");

        List<String> databases = diffService.getDatabases(instance);

        StringBuilder sb = new StringBuilder();
        for (String db : databases) {
            sb.append(String.format("<option value=\"%s\">%s</option>", db, db));
        }
        return sb.toString();
    }

    /**
     * HTMX endpoint: Get schemas for selected instance and database.
     */
    @GET
    @Path("/schemas")
    @Produces(MediaType.TEXT_HTML)
    public String getSchemas(
            @QueryParam("instance") String instance,
            @QueryParam("database") String database) {
        featureToggleService.requirePageEnabled("database-diff");

        if (database == null || database.isEmpty()) {
            return "<option value=\"public\" selected>public</option>";
        }

        List<String> schemas = diffService.getSchemasForDatabase(instance, database);

        StringBuilder sb = new StringBuilder();
        for (String schema : schemas) {
            String selected = "public".equals(schema) ? " selected" : "";
            sb.append(String.format("<option value=\"%s\"%s>%s</option>", schema, selected, schema));
        }
        return sb.toString();
    }

    /**
     * HTMX endpoint: Get schema summary for a database.
     */
    @GET
    @Path("/schema-summary")
    @Produces(MediaType.TEXT_HTML)
    public String getSchemaSummary(
            @QueryParam("instance") String instance,
            @QueryParam("database") String database,
            @QueryParam("schema") @DefaultValue("public") String schema) {
        featureToggleService.requirePageEnabled("database-diff");

        if (database == null || database.isEmpty()) {
            return "<span class=\"text-muted\">Select a database</span>";
        }

        Map<String, Integer> summary = diffService.getSchemaSummary(instance, database, schema);

        if (summary.isEmpty()) {
            return "<span class=\"text-muted\">No objects found</span>";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"d-flex flex-wrap gap-2\">");
        for (Map.Entry<String, Integer> entry : summary.entrySet()) {
            if (entry.getValue() > 0) {
                sb.append(String.format(
                        "<span class=\"badge bg-secondary\">%s: %d</span>",
                        entry.getKey(), entry.getValue()));
            }
        }
        sb.append("</div>");
        return sb.toString();
    }

    /**
     * Run cross-database comparison.
     */
    @POST
    @Path("/compare")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance compare(
            @FormParam("sourceInstance") String sourceInstance,
            @FormParam("sourceDatabase") String sourceDatabase,
            @FormParam("sourceSchema") @DefaultValue("public") String sourceSchema,
            @FormParam("destInstance") String destInstance,
            @FormParam("destDatabase") String destDatabase,
            @FormParam("destSchema") @DefaultValue("public") String destSchema,
            @FormParam("filterPreset") String filterPreset,
            @FormParam("excludePatterns") String excludePatterns,
            @FormParam("includeTables") @DefaultValue("true") boolean includeTables,
            @FormParam("includeViews") @DefaultValue("true") boolean includeViews,
            @FormParam("includeFunctions") @DefaultValue("true") boolean includeFunctions,
            @FormParam("includeSequences") @DefaultValue("true") boolean includeSequences,
            @FormParam("includeTypes") @DefaultValue("true") boolean includeTypes,
            @FormParam("includeExtensions") @DefaultValue("false") boolean includeExtensions,
            @FormParam("includeIndexes") @DefaultValue("true") boolean includeIndexes,
            @FormParam("includeTriggers") @DefaultValue("true") boolean includeTriggers,
            @FormParam("includeConstraints") @DefaultValue("true") boolean includeConstraints) {
        featureToggleService.requirePageEnabled("database-diff");

        LOG.infof("Cross-database comparison requested: %s.%s.%s -> %s.%s.%s",
                sourceInstance, sourceDatabase, sourceSchema,
                destInstance, destDatabase, destSchema);

        // Build filter
        ComparisonFilter filter = new ComparisonFilter();

        if (filterPreset != null && !filterPreset.isEmpty()) {
            try {
                filter = ComparisonFilter.fromPreset(ComparisonFilter.FilterPreset.valueOf(filterPreset));
            } catch (IllegalArgumentException e) {
                // Use default filter
            }
        }

        if (excludePatterns != null && !excludePatterns.isBlank()) {
            for (String pattern : excludePatterns.split("\n")) {
                pattern = pattern.trim();
                if (!pattern.isEmpty()) {
                    filter.getExcludePatterns().add(pattern);
                }
            }
        }

        filter.setIncludeTables(includeTables);
        filter.setIncludeViews(includeViews);
        filter.setIncludeFunctions(includeFunctions);
        filter.setIncludeSequences(includeSequences);
        filter.setIncludeTypes(includeTypes);
        filter.setIncludeExtensions(includeExtensions);
        filter.setIncludeIndexes(includeIndexes);
        filter.setIncludeTriggers(includeTriggers);
        filter.setIncludePrimaryKeys(includeConstraints);
        filter.setIncludeForeignKeys(includeConstraints);
        filter.setIncludeUniqueConstraints(includeConstraints);
        filter.setIncludeCheckConstraints(includeConstraints);

        // Run comparison
        SchemaComparisonResult result = diffService.compareCrossDatabase(
                sourceInstance, sourceDatabase, sourceSchema,
                destInstance, destDatabase, destSchema,
                filter);

        return databaseDiffResult
                .data("result", result)
                .data("sourceDatabase", sourceDatabase)
                .data("destDatabase", destDatabase)
                .data("wrapOptions", MigrationScript.WrapOption.values())
                .data("schemaEnabled", config.schema().enabled())
                .data("toggles", featureToggleService.getAllToggles())
                .data("instances", dataSourceManager.getAvailableInstances());
    }

    /**
     * Generate migration script from comparison result.
     */
    @POST
    @Path("/generate-migration")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance generateMigration(
            @FormParam("sourceInstance") String sourceInstance,
            @FormParam("sourceDatabase") String sourceDatabase,
            @FormParam("sourceSchema") @DefaultValue("public") String sourceSchema,
            @FormParam("destInstance") String destInstance,
            @FormParam("destDatabase") String destDatabase,
            @FormParam("destSchema") @DefaultValue("public") String destSchema,
            @FormParam("wrapOption") @DefaultValue("SINGLE_TRANSACTION") MigrationScript.WrapOption wrapOption,
            @FormParam("includeDrops") @DefaultValue("false") boolean includeDrops) {
        featureToggleService.requirePageEnabled("database-diff");

        // Re-run comparison to get fresh result
        SchemaComparisonResult compResult = diffService.compareCrossDatabase(
                sourceInstance, sourceDatabase, sourceSchema,
                destInstance, destDatabase, destSchema,
                new ComparisonFilter());

        // Generate migration script
        MigrationScript script = diffService.generateMigration(compResult, wrapOption, includeDrops);

        return databaseDiffResult
                .data("result", compResult)
                .data("script", script)
                .data("sourceDatabase", sourceDatabase)
                .data("destDatabase", destDatabase)
                .data("wrapOptions", MigrationScript.WrapOption.values())
                .data("selectedWrapOption", wrapOption)
                .data("includeDrops", includeDrops)
                .data("schemaEnabled", config.schema().enabled())
                .data("toggles", featureToggleService.getAllToggles())
                .data("instances", dataSourceManager.getAvailableInstances());
    }

    /**
     * Download migration script as SQL file.
     */
    @GET
    @Path("/download-script")
    @Produces("application/sql")
    public Response downloadScript(
            @QueryParam("sourceInstance") String sourceInstance,
            @QueryParam("sourceDatabase") String sourceDatabase,
            @QueryParam("sourceSchema") @DefaultValue("public") String sourceSchema,
            @QueryParam("destInstance") String destInstance,
            @QueryParam("destDatabase") String destDatabase,
            @QueryParam("destSchema") @DefaultValue("public") String destSchema,
            @QueryParam("wrapOption") @DefaultValue("SINGLE_TRANSACTION") MigrationScript.WrapOption wrapOption,
            @QueryParam("includeDrops") @DefaultValue("false") boolean includeDrops) {
        featureToggleService.requirePageEnabled("database-diff");

        // Run comparison
        SchemaComparisonResult compResult = diffService.compareCrossDatabase(
                sourceInstance, sourceDatabase, sourceSchema,
                destInstance, destDatabase, destSchema,
                new ComparisonFilter());

        // Generate script
        MigrationScript script = diffService.generateMigration(compResult, wrapOption, includeDrops);

        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now());

        String filename = String.format("migration_%s_%s_to_%s_%s_%s.sql",
                sourceInstance, sourceDatabase, destInstance, destDatabase, timestamp);

        return Response.ok(script.getFullScript().getBytes(StandardCharsets.UTF_8))
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .build();
    }

    /**
     * Export comparison result as HTML.
     */
    @GET
    @Path("/export/html")
    @Produces(MediaType.TEXT_HTML)
    public Response exportHtml(
            @QueryParam("sourceInstance") String sourceInstance,
            @QueryParam("sourceDatabase") String sourceDatabase,
            @QueryParam("sourceSchema") @DefaultValue("public") String sourceSchema,
            @QueryParam("destInstance") String destInstance,
            @QueryParam("destDatabase") String destDatabase,
            @QueryParam("destSchema") @DefaultValue("public") String destSchema) {
        featureToggleService.requirePageEnabled("database-diff");

        SchemaComparisonResult result = diffService.compareCrossDatabase(
                sourceInstance, sourceDatabase, sourceSchema,
                destInstance, destDatabase, destSchema,
                new ComparisonFilter());

        String html = generateHtmlReport(result);

        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now());

        String filename = String.format("db-diff_%s_%s_vs_%s_%s_%s.html",
                sourceInstance, sourceDatabase, destInstance, destDatabase, timestamp);

        return Response.ok(html)
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .build();
    }

    /**
     * Export comparison result as Markdown.
     */
    @GET
    @Path("/export/markdown")
    @Produces("text/markdown")
    public Response exportMarkdown(
            @QueryParam("sourceInstance") String sourceInstance,
            @QueryParam("sourceDatabase") String sourceDatabase,
            @QueryParam("sourceSchema") @DefaultValue("public") String sourceSchema,
            @QueryParam("destInstance") String destInstance,
            @QueryParam("destDatabase") String destDatabase,
            @QueryParam("destSchema") @DefaultValue("public") String destSchema) {
        featureToggleService.requirePageEnabled("database-diff");

        SchemaComparisonResult result = diffService.compareCrossDatabase(
                sourceInstance, sourceDatabase, sourceSchema,
                destInstance, destDatabase, destSchema,
                new ComparisonFilter());

        String markdown = generateMarkdownReport(result);

        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now());

        String filename = String.format("db-diff_%s_%s_vs_%s_%s_%s.md",
                sourceInstance, sourceDatabase, destInstance, destDatabase, timestamp);

        return Response.ok(markdown.getBytes(StandardCharsets.UTF_8))
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .build();
    }

    /**
     * Export comparison result as PDF.
     */
    @GET
    @Path("/export/pdf")
    @Produces("application/pdf")
    public Response exportPdf(
            @QueryParam("sourceInstance") String sourceInstance,
            @QueryParam("sourceDatabase") String sourceDatabase,
            @QueryParam("sourceSchema") @DefaultValue("public") String sourceSchema,
            @QueryParam("destInstance") String destInstance,
            @QueryParam("destDatabase") String destDatabase,
            @QueryParam("destSchema") @DefaultValue("public") String destSchema) {
        featureToggleService.requirePageEnabled("database-diff");

        SchemaComparisonResult result = diffService.compareCrossDatabase(
                sourceInstance, sourceDatabase, sourceSchema,
                destInstance, destDatabase, destSchema,
                new ComparisonFilter());

        byte[] pdfBytes = pdfExportService.generateSchemaComparisonPdf(result);

        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now());

        String filename = String.format("db-diff_%s_%s_vs_%s_%s_%s.pdf",
                sourceInstance, sourceDatabase, destInstance, destDatabase, timestamp);

        return Response.ok(pdfBytes)
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .build();
    }

    // =========================================================================
    // Private helper methods
    // =========================================================================

    private String generateHtmlReport(SchemaComparisonResult result) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html><head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<title>Cross-Database Schema Diff Report</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 20px; }\n");
        html.append("h1 { color: #333; }\n");
        html.append("table { border-collapse: collapse; width: 100%; margin-top: 20px; }\n");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n");
        html.append("th { background-color: #4CAF50; color: white; }\n");
        html.append(".missing { background-color: #fff3cd; }\n");
        html.append(".extra { background-color: #f8d7da; }\n");
        html.append(".modified { background-color: #d1ecf1; }\n");
        html.append(".summary { display: flex; gap: 20px; margin: 20px 0; }\n");
        html.append(".summary-card { padding: 15px; border-radius: 8px; min-width: 150px; }\n");
        html.append(".summary-card.missing { background-color: #fff3cd; }\n");
        html.append(".summary-card.extra { background-color: #f8d7da; }\n");
        html.append(".summary-card.modified { background-color: #d1ecf1; }\n");
        html.append("</style>\n</head><body>\n");

        html.append("<h1>Cross-Database Schema Diff Report</h1>\n");
        html.append(String.format("<p><strong>Source:</strong> %s.%s</p>\n",
                result.getSourceInstance(), result.getSourceSchema()));
        html.append(String.format("<p><strong>Destination:</strong> %s.%s</p>\n",
                result.getDestinationInstance(), result.getDestinationSchema()));
        html.append(String.format("<p><strong>Compared at:</strong> %s</p>\n", result.getComparedAt()));

        // Summary
        SchemaComparisonResult.ComparisonSummary summary = result.getSummary();
        html.append("<div class=\"summary\">\n");
        html.append(String.format("<div class=\"summary-card missing\"><h3>Missing</h3><p>%d objects</p></div>\n",
                summary.getMissingObjects()));
        html.append(String.format("<div class=\"summary-card extra\"><h3>Extra</h3><p>%d objects</p></div>\n",
                summary.getExtraObjects()));
        html.append(String.format("<div class=\"summary-card modified\"><h3>Modified</h3><p>%d objects</p></div>\n",
                summary.getModifiedObjects()));
        html.append("</div>\n");

        // Differences table
        if (!result.getDifferences().isEmpty()) {
            html.append("<table>\n");
            html.append("<tr><th>Type</th><th>Name</th><th>Difference</th><th>Severity</th></tr>\n");

            for (ObjectDifference diff : result.getDifferences()) {
                String rowClass = switch (diff.getDifferenceType()) {
                    case MISSING -> "missing";
                    case EXTRA -> "extra";
                    case MODIFIED -> "modified";
                };
                html.append(String.format("<tr class=\"%s\"><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>\n",
                        rowClass, diff.getObjectType(), diff.getObjectName(),
                        diff.getDifferenceType(), diff.getSeverity()));
            }

            html.append("</table>\n");
        } else {
            html.append("<p>No differences found. Schemas are identical.</p>\n");
        }

        html.append("</body></html>");
        return html.toString();
    }

    private String generateMarkdownReport(SchemaComparisonResult result) {
        StringBuilder md = new StringBuilder();
        md.append("# Cross-Database Schema Diff Report\n\n");
        md.append(String.format("**Source:** %s.%s\n\n", result.getSourceInstance(), result.getSourceSchema()));
        md.append(String.format("**Destination:** %s.%s\n\n", result.getDestinationInstance(), result.getDestinationSchema()));
        md.append(String.format("**Compared at:** %s\n\n", result.getComparedAt()));

        // Summary
        SchemaComparisonResult.ComparisonSummary summary = result.getSummary();
        md.append("## Summary\n\n");
        md.append(String.format("- **Missing:** %d objects\n", summary.getMissingObjects()));
        md.append(String.format("- **Extra:** %d objects\n", summary.getExtraObjects()));
        md.append(String.format("- **Modified:** %d objects\n", summary.getModifiedObjects()));
        md.append(String.format("- **Matching:** %d objects\n\n", summary.getMatchingObjects()));

        // Differences
        if (!result.getDifferences().isEmpty()) {
            md.append("## Differences\n\n");
            md.append("| Type | Name | Difference | Severity |\n");
            md.append("|------|------|------------|----------|\n");

            for (ObjectDifference diff : result.getDifferences()) {
                md.append(String.format("| %s | %s | %s | %s |\n",
                        diff.getObjectType(), diff.getObjectName(),
                        diff.getDifferenceType(), diff.getSeverity()));
            }
        } else {
            md.append("## Result\n\nNo differences found. Schemas are identical.\n");
        }

        return md.toString();
    }
}
