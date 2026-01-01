package com.bovinemagnet.pgconsole.resource;

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

    /**
     * Display the schema documentation generation page.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance showPage(@QueryParam("instance") @DefaultValue("default") String instance) {
        toggleService.requirePageEnabled("schema-docs");

        List<String> schemas = schemaDocService.getSchemas(instance);

        return schemaDocs.data("instance", instance)
                .data("instances", dataSourceManager.getInstanceInfoList())
                .data("currentInstance", instance)
                .data("schemas", schemas)
                .data("formats", OutputFormat.values())
                .data("toggles", toggleService.getAllToggles())
                .data("schemaEnabled", toggleService.isSchemaEnabled())
                .data("inMemoryMinutes", toggleService.getInMemoryMinutes());
    }

    /**
     * Generate and download schema documentation.
     */
    @GET
    @Path("/generate")
    @Produces(MediaType.TEXT_HTML)
    public Response generateDocumentation(
            @QueryParam("instance") @DefaultValue("default") String instance,
            @QueryParam("schema") @DefaultValue("public") String schemaName,
            @QueryParam("format") @DefaultValue("HTML") String formatStr,
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

        String documentation = schemaDocService.generateDocumentation(instance, schemaName, format, options);

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
            String filename = String.format("schema-doc-%s-%s-%s.%s", instance, schemaName, timestamp, extension);

            return Response.ok(documentation)
                    .type(contentType)
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .build();
        } else {
            return Response.ok(documentation).type(contentType).build();
        }
    }

    /**
     * API endpoint for schema documentation (JSON metadata).
     */
    @GET
    @Path("/api")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSchemaInfo(
            @QueryParam("instance") @DefaultValue("default") String instance) {

        toggleService.requirePageEnabled("schema-docs");

        List<String> schemas = schemaDocService.getSchemas(instance);

        return Response.ok(Map.of(
                "instance", instance,
                "schemas", schemas,
                "formats", List.of("HTML", "MARKDOWN", "ASCIIDOC")
        )).build();
    }
}
