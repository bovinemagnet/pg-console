package com.bovinemagnet.pgconsole.resource;

import com.bovinemagnet.pgconsole.model.ComparisonFilter;
import com.bovinemagnet.pgconsole.service.CrossDatabaseConnectionService;
import com.bovinemagnet.pgconsole.service.DataSourceManager;
import com.bovinemagnet.pgconsole.service.FeatureToggleService;
import com.bovinemagnet.pgconsole.service.SchemaDocumentationService;
import com.bovinemagnet.pgconsole.service.SchemaDocumentationService.DocumentationOptions;
import com.bovinemagnet.pgconsole.service.SchemaDocumentationService.OutputFormat;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * REST resource for schema documentation generation.
 * Provides UI for generating and downloading database schema documentation
 * in multiple formats (HTML, Markdown, AsciiDoc).
 *
 * @author Paul Snow
 * @since 0.0.0
 */
@Path("/schema-docs")
public class SchemaDocResource {

    @Inject
    Template schemaDocs;

    @Inject
    SchemaDocumentationService schemaDocService;

    @Inject
    FeatureToggleService toggleService;

    @Inject
    DataSourceManager dataSourceManager;

    @Inject
    CrossDatabaseConnectionService crossDbService;

    /**
     * Display the schema documentation generation page.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance showPage(@QueryParam("instance") @DefaultValue("default") String instance) {
        toggleService.requirePageEnabled("schema-docs");

        List<String> instanceNames = dataSourceManager.getAvailableInstances();
        String currentInstance = instanceNames.isEmpty() ? "default" :
                (instanceNames.contains(instance) ? instance : instanceNames.get(0));

        List<String> databases = crossDbService.listDatabases(currentInstance);
        String currentDatabase = databases.isEmpty() ? "" : databases.get(0);

        List<String> schemas = currentDatabase.isEmpty() ? List.of("public") :
                schemaDocService.getSchemas(currentInstance, currentDatabase);

        return schemaDocs.data("instance", currentInstance)
                .data("instances", dataSourceManager.getInstanceInfoList())
                .data("currentInstance", currentInstance)
                .data("databases", databases)
                .data("currentDatabase", currentDatabase)
                .data("schemas", schemas)
                .data("formats", OutputFormat.values())
                .data("toggles", toggleService.getAllToggles())
                .data("schemaEnabled", toggleService.isSchemaEnabled())
                .data("inMemoryMinutes", toggleService.getInMemoryMinutes());
    }

    /**
     * HTMX endpoint: Get databases for selected instance.
     */
    @GET
    @Path("/databases")
    @Produces(MediaType.TEXT_HTML)
    public String getDatabases(@QueryParam("instance") String instance) {
        toggleService.requirePageEnabled("schema-docs");

        List<String> databases = crossDbService.listDatabases(instance);

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
        toggleService.requirePageEnabled("schema-docs");

        if (database == null || database.isEmpty()) {
            return "<option value=\"public\" selected>public</option>";
        }

        List<String> schemas = schemaDocService.getSchemas(instance, database);

        StringBuilder sb = new StringBuilder();
        for (String schema : schemas) {
            String selected = "public".equals(schema) ? " selected" : "";
            sb.append(String.format("<option value=\"%s\"%s>%s</option>", schema, selected, schema));
        }
        return sb.toString();
    }

    /**
     * Generate and download schema documentation.
     */
    @GET
    @Path("/generate")
    @Produces(MediaType.TEXT_HTML)
    public Response generateDocumentation(
            @QueryParam("instance") @DefaultValue("default") String instance,
            @QueryParam("database") String database,
            @QueryParam("schema") @DefaultValue("public") String schemaName,
            @QueryParam("format") @DefaultValue("HTML") String formatStr,
            @QueryParam("filterPreset") @DefaultValue("NONE") String filterPresetStr,
            @QueryParam("excludePatterns") String excludePatterns,
            @QueryParam("includeTables") @DefaultValue("true") boolean includeTables,
            @QueryParam("includeViews") @DefaultValue("true") boolean includeViews,
            @QueryParam("includeFunctions") @DefaultValue("true") boolean includeFunctions,
            @QueryParam("includeSequences") @DefaultValue("true") boolean includeSequences,
            @QueryParam("includeTypes") @DefaultValue("true") boolean includeTypes,
            @QueryParam("includeIndexes") @DefaultValue("true") boolean includeIndexes,
            @QueryParam("includeConstraints") @DefaultValue("true") boolean includeConstraints,
            @QueryParam("includeTriggers") @DefaultValue("true") boolean includeTriggers,
            @QueryParam("includeComments") @DefaultValue("true") boolean includeComments,
            @QueryParam("includeExtensions") @DefaultValue("true") boolean includeExtensions,
            @QueryParam("download") @DefaultValue("false") boolean download) {

        toggleService.requirePageEnabled("schema-docs");

        OutputFormat format;
        try {
            format = OutputFormat.valueOf(formatStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            format = OutputFormat.HTML;
        }

        // Build filter from preset and custom patterns
        ComparisonFilter filter = buildFilter(filterPresetStr, excludePatterns);

        DocumentationOptions options = new DocumentationOptions();
        options.includeTables = includeTables;
        options.includeViews = includeViews;
        options.includeFunctions = includeFunctions;
        options.includeSequences = includeSequences;
        options.includeTypes = includeTypes;
        options.includeIndexes = includeIndexes;
        options.includeConstraints = includeConstraints;
        options.includeTriggers = includeTriggers;
        options.includeComments = includeComments;
        options.includeExtensions = includeExtensions;

        // Use cross-database connection if database is specified
        String documentation;
        if (database != null && !database.isEmpty()) {
            documentation = schemaDocService.generateDocumentation(instance, database, schemaName, format, options, filter);
        } else {
            documentation = schemaDocService.generateDocumentation(instance, schemaName, format, options, filter);
        }

        String contentType;
        String extension;
        switch (format) {
            case MARKDOWN -> {
                contentType = "text/markdown";
                extension = "md";
            }
            case ASCIIDOC -> {
                contentType = "text/asciidoc";
                extension = "adoc";
            }
            default -> {
                contentType = MediaType.TEXT_HTML;
                extension = "html";
            }
        }

        if (download) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            String dbPart = (database != null && !database.isEmpty()) ? database + "-" : "";
            String filename = String.format("schema-doc-%s-%s%s-%s.%s", instance, dbPart, schemaName, timestamp, extension);

            return Response.ok(documentation)
                    .type(contentType)
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .build();
        } else {
            return Response.ok(documentation).type(contentType).build();
        }
    }

    /**
     * Build a ComparisonFilter from preset and custom patterns.
     */
    private ComparisonFilter buildFilter(String filterPresetStr, String excludePatterns) {
        ComparisonFilter filter;

        // Start with preset
        try {
            ComparisonFilter.FilterPreset preset = ComparisonFilter.FilterPreset.valueOf(filterPresetStr);
            filter = ComparisonFilter.fromPreset(preset);
        } catch (IllegalArgumentException e) {
            filter = new ComparisonFilter();
        }

        // Add custom exclude patterns
        if (excludePatterns != null && !excludePatterns.isBlank()) {
            for (String pattern : excludePatterns.split(",")) {
                String trimmed = pattern.trim();
                if (!trimmed.isEmpty()) {
                    filter.addExcludeTablePattern(trimmed);
                }
            }
        }

        return filter;
    }

    /**
     * API endpoint for schema documentation (JSON metadata).
     */
    @GET
    @Path("/api")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSchemaInfo(
            @QueryParam("instance") @DefaultValue("default") String instance,
            @QueryParam("database") String database) {

        toggleService.requirePageEnabled("schema-docs");

        List<String> databases = crossDbService.listDatabases(instance);
        String currentDatabase = (database != null && !database.isEmpty()) ? database :
                (databases.isEmpty() ? "" : databases.get(0));

        List<String> schemas = currentDatabase.isEmpty() ? List.of("public") :
                schemaDocService.getSchemas(instance, currentDatabase);

        return Response.ok(Map.of(
                "instance", instance,
                "databases", databases,
                "currentDatabase", currentDatabase,
                "schemas", schemas,
                "formats", List.of("HTML", "MARKDOWN", "ASCIIDOC")
        )).build();
    }
}
