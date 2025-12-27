package com.bovinemagnet.pgconsole.resource;

import com.bovinemagnet.pgconsole.model.*;
import com.bovinemagnet.pgconsole.service.*;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Resource for schema comparison and migration functionality.
 * <p>
 * Provides web UI endpoints for comparing schemas between
 * PostgreSQL instances, generating migration scripts, and
 * managing comparison profiles.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@Path("/schema-comparison")
public class SchemaComparisonResource {

    private static final Logger LOG = Logger.getLogger(SchemaComparisonResource.class);

    @Inject
    SchemaComparisonService comparisonService;

    @Inject
    ComparisonProfileService profileService;

    @Inject
    ComparisonHistoryService historyService;

    @Inject
    DataSourceManager dataSourceManager;

    @Inject
    Template schemaComparison;

    @Inject
    Template schemaComparisonResult;

    @Inject
    Template schemaComparisonProfiles;

    @Inject
    Template schemaComparisonHistory;

    @Inject
    Template migrationScript;

    @Inject
    FeatureToggleService featureToggleService;

    /**
     * Main schema comparison page.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index(@QueryParam("instance") @DefaultValue("default") String instance) {
        featureToggleService.requirePageEnabled("schema-comparison");
        List<String> instances = dataSourceManager.getAvailableInstances();
        List<String> schemas = comparisonService.getSchemas(instance);
        List<ComparisonProfile> profiles = profileService.findAll();

        return schemaComparison
                .data("instances", instances)
                .data("schemas", schemas)
                .data("profiles", profiles)
                .data("currentInstance", instance)
                .data("filterPresets", ComparisonFilter.FilterPreset.values())
                .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * HTMX endpoint to get schemas for a selected instance.
     */
    @GET
    @Path("/schemas")
    @Produces(MediaType.TEXT_HTML)
    public String getSchemas(@QueryParam("instance") String instance) {
        featureToggleService.requirePageEnabled("schema-comparison");
        List<String> schemas = comparisonService.getSchemas(instance);

        StringBuilder sb = new StringBuilder();
        for (String schema : schemas) {
            sb.append(String.format("<option value=\"%s\">%s</option>", schema, schema));
        }
        return sb.toString();
    }

    /**
     * Run schema comparison.
     */
    @POST
    @Path("/compare")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance compare(
            @FormParam("sourceInstance") String sourceInstance,
            @FormParam("destInstance") String destInstance,
            @FormParam("sourceSchema") @DefaultValue("public") String sourceSchema,
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
            @FormParam("includeConstraints") @DefaultValue("true") boolean includeConstraints,
            @FormParam("profileId") Long profileId) {
        featureToggleService.requirePageEnabled("schema-comparison");

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
        SchemaComparisonResult result = comparisonService.compare(
                sourceInstance, destInstance, sourceSchema, destSchema, filter);

        // Record in history
        String profileName = null;
        if (profileId != null) {
            Optional<ComparisonProfile> profile = profileService.findById(profileId);
            if (profile.isPresent()) {
                profileName = profile.get().getName();
                profileService.updateLastRun(profileId, result.getSummary());
            }
        }
        historyService.record(result, "system", profileName);

        // Check for drift
        ComparisonHistoryService.DriftSummary drift = historyService.getDriftSummary(
                result, sourceInstance, destInstance);

        return schemaComparisonResult
                .data("result", result)
                .data("drift", drift)
                .data("wrapOptions", MigrationScript.WrapOption.values())
                .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Run comparison from saved profile.
     */
    @GET
    @Path("/run-profile/{id}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance runProfile(@PathParam("id") long id) {
        featureToggleService.requirePageEnabled("schema-comparison");
        Optional<ComparisonProfile> profileOpt = profileService.findById(id);
        if (profileOpt.isEmpty()) {
            return schemaComparison
                    .data("error", "Profile not found")
                    .data("instances", dataSourceManager.getAvailableInstances())
                    .data("schemas", List.of())
                    .data("profiles", profileService.findAll())
                    .data("filterPresets", ComparisonFilter.FilterPreset.values())
                    .data("toggles", featureToggleService.getAllToggles());
        }

        ComparisonProfile profile = profileOpt.get();

        SchemaComparisonResult result = comparisonService.compare(
                profile.getSourceInstance(),
                profile.getDestinationInstance(),
                profile.getSourceSchema(),
                profile.getDestinationSchema(),
                profile.getFilter());

        // Update profile and record history
        profileService.updateLastRun(id, result.getSummary());
        historyService.record(result, "system", profile.getName());

        ComparisonHistoryService.DriftSummary drift = historyService.getDriftSummary(
                result, profile.getSourceInstance(), profile.getDestinationInstance());

        return schemaComparisonResult
                .data("result", result)
                .data("profile", profile)
                .data("drift", drift)
                .data("wrapOptions", MigrationScript.WrapOption.values())
                .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Generate migration script.
     */
    @POST
    @Path("/generate-migration")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance generateMigration(
            @FormParam("sourceInstance") String sourceInstance,
            @FormParam("destInstance") String destInstance,
            @FormParam("sourceSchema") String sourceSchema,
            @FormParam("destSchema") String destSchema,
            @FormParam("wrapOption") @DefaultValue("SINGLE_TRANSACTION") String wrapOption,
            @FormParam("includeDrops") @DefaultValue("false") boolean includeDrops) {
        featureToggleService.requirePageEnabled("schema-comparison");

        // Re-run comparison to get fresh result
        SchemaComparisonResult result = comparisonService.compare(
                sourceInstance, destInstance, sourceSchema, destSchema, null);

        MigrationScript.WrapOption wrap;
        try {
            wrap = MigrationScript.WrapOption.valueOf(wrapOption);
        } catch (IllegalArgumentException e) {
            wrap = MigrationScript.WrapOption.SINGLE_TRANSACTION;
        }

        MigrationScript script = comparisonService.generateMigration(result, wrap, includeDrops);

        return migrationScript
                .data("script", script)
                .data("result", result)
                .data("wrapOptions", MigrationScript.WrapOption.values())
                .data("selectedWrapOption", wrap)
                .data("includeDrops", includeDrops)
                .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Download migration script as .sql file.
     */
    @GET
    @Path("/download-script")
    @Produces("text/plain")
    public Response downloadScript(
            @QueryParam("sourceInstance") String sourceInstance,
            @QueryParam("destInstance") String destInstance,
            @QueryParam("sourceSchema") String sourceSchema,
            @QueryParam("destSchema") String destSchema,
            @QueryParam("wrapOption") @DefaultValue("SINGLE_TRANSACTION") String wrapOption,
            @QueryParam("includeDrops") @DefaultValue("false") boolean includeDrops) {
        featureToggleService.requirePageEnabled("schema-comparison");

        SchemaComparisonResult result = comparisonService.compare(
                sourceInstance, destInstance, sourceSchema, destSchema, null);

        MigrationScript.WrapOption wrap;
        try {
            wrap = MigrationScript.WrapOption.valueOf(wrapOption);
        } catch (IllegalArgumentException e) {
            wrap = MigrationScript.WrapOption.SINGLE_TRANSACTION;
        }

        MigrationScript script = comparisonService.generateMigration(result, wrap, includeDrops);

        String filename = String.format("migration_%s_%s_to_%s_%s.sql",
                sourceInstance, sourceSchema, destInstance, destSchema);

        return Response.ok(script.getFullScript())
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .build();
    }

    /**
     * Profile management page.
     */
    @GET
    @Path("/profiles")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance profiles() {
        featureToggleService.requirePageEnabled("schema-comparison");
        return schemaComparisonProfiles
                .data("profiles", profileService.findAll())
                .data("instances", dataSourceManager.getAvailableInstances())
                .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Save a new profile.
     */
    @POST
    @Path("/profiles")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance saveProfile(
            @FormParam("name") String name,
            @FormParam("description") String description,
            @FormParam("sourceInstance") String sourceInstance,
            @FormParam("destInstance") String destInstance,
            @FormParam("sourceSchema") @DefaultValue("public") String sourceSchema,
            @FormParam("destSchema") @DefaultValue("public") String destSchema,
            @FormParam("isDefault") @DefaultValue("false") boolean isDefault) {
        featureToggleService.requirePageEnabled("schema-comparison");

        if (profileService.nameExists(name)) {
            return schemaComparisonProfiles
                    .data("error", "A profile with this name already exists")
                    .data("profiles", profileService.findAll())
                    .data("instances", dataSourceManager.getAvailableInstances())
                    .data("toggles", featureToggleService.getAllToggles());
        }

        ComparisonProfile profile = ComparisonProfile.builder()
                .name(name)
                .description(description)
                .sourceInstance(sourceInstance)
                .destinationInstance(destInstance)
                .sourceSchema(sourceSchema)
                .destinationSchema(destSchema)
                .isDefault(isDefault)
                .createdBy("system")
                .build();

        profileService.save(profile);

        return schemaComparisonProfiles
                .data("success", "Profile saved successfully")
                .data("profiles", profileService.findAll())
                .data("instances", dataSourceManager.getAvailableInstances())
                .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Delete a profile.
     */
    @DELETE
    @Path("/profiles/{id}")
    @Produces(MediaType.TEXT_HTML)
    public String deleteProfile(@PathParam("id") long id) {
        featureToggleService.requirePageEnabled("schema-comparison");
        profileService.delete(id);
        return "<div class=\"alert alert-success\" role=\"alert\">Profile deleted successfully</div>";
    }

    /**
     * Export profile as JSON.
     */
    @GET
    @Path("/profiles/{id}/export")
    @Produces(MediaType.APPLICATION_JSON)
    public Response exportProfile(@PathParam("id") long id) {
        featureToggleService.requirePageEnabled("schema-comparison");
        String json = profileService.exportAsJson(id);
        if (json == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(json)
                .header("Content-Disposition", "attachment; filename=\"profile-" + id + ".json\"")
                .build();
    }

    /**
     * Import profile from JSON.
     */
    @POST
    @Path("/profiles/import")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance importProfile(String json) {
        featureToggleService.requirePageEnabled("schema-comparison");
        ComparisonProfile profile = profileService.importFromJson(json);

        if (profile == null) {
            return schemaComparisonProfiles
                    .data("error", "Failed to import profile - invalid JSON")
                    .data("profiles", profileService.findAll())
                    .data("instances", dataSourceManager.getAvailableInstances())
                    .data("toggles", featureToggleService.getAllToggles());
        }

        return schemaComparisonProfiles
                .data("success", "Profile imported successfully")
                .data("profiles", profileService.findAll())
                .data("instances", dataSourceManager.getAvailableInstances())
                .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Comparison history page.
     */
    @GET
    @Path("/history")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance history(
            @QueryParam("limit") @DefaultValue("50") int limit) {
        featureToggleService.requirePageEnabled("schema-comparison");
        return schemaComparisonHistory
                .data("history", historyService.getHistory(limit))
                .data("totalCount", historyService.count())
                .data("instances", dataSourceManager.getAvailableInstances())
                .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * History detail.
     */
    @GET
    @Path("/history/{id}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance historyDetail(@PathParam("id") long id) {
        featureToggleService.requirePageEnabled("schema-comparison");
        Optional<ComparisonHistory> historyOpt = historyService.findById(id);
        if (historyOpt.isEmpty()) {
            return schemaComparisonHistory
                    .data("error", "History record not found")
                    .data("history", historyService.getHistory(50))
                    .data("totalCount", historyService.count())
                    .data("instances", dataSourceManager.getAvailableInstances())
                    .data("toggles", featureToggleService.getAllToggles());
        }

        return schemaComparisonHistory
                .data("detail", historyOpt.get())
                .data("history", historyService.getHistory(50))
                .data("totalCount", historyService.count())
                .data("instances", dataSourceManager.getAvailableInstances())
                .data("toggles", featureToggleService.getAllToggles());
    }

    /**
     * Export comparison result as HTML.
     */
    @GET
    @Path("/export/html")
    @Produces(MediaType.TEXT_HTML)
    public Response exportHtml(
            @QueryParam("sourceInstance") String sourceInstance,
            @QueryParam("destInstance") String destInstance,
            @QueryParam("sourceSchema") String sourceSchema,
            @QueryParam("destSchema") String destSchema) {
        featureToggleService.requirePageEnabled("schema-comparison");

        SchemaComparisonResult result = comparisonService.compare(
                sourceInstance, destInstance, sourceSchema, destSchema, null);

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html>\n<head>\n");
        html.append("<title>Schema Comparison Report</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; }\n");
        html.append("table { border-collapse: collapse; width: 100%; margin-bottom: 20px; }\n");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n");
        html.append("th { background-color: #f2f2f2; }\n");
        html.append(".missing { background-color: #fff3cd; }\n");
        html.append(".extra { background-color: #cce5ff; }\n");
        html.append(".modified { background-color: #d1ecf1; }\n");
        html.append("</style>\n</head>\n<body>\n");

        html.append("<h1>Schema Comparison Report</h1>\n");
        html.append(String.format("<p><strong>Source:</strong> %s.%s</p>\n",
                result.getSourceInstance(), result.getSourceSchema()));
        html.append(String.format("<p><strong>Destination:</strong> %s.%s</p>\n",
                result.getDestinationInstance(), result.getDestinationSchema()));
        html.append(String.format("<p><strong>Compared at:</strong> %s</p>\n",
                result.getComparedAtFormatted()));

        html.append("<h2>Summary</h2>\n");
        html.append("<ul>\n");
        html.append(String.format("<li>Missing: %d</li>\n", result.getSummary().getMissingObjects()));
        html.append(String.format("<li>Extra: %d</li>\n", result.getSummary().getExtraObjects()));
        html.append(String.format("<li>Modified: %d</li>\n", result.getSummary().getModifiedObjects()));
        html.append(String.format("<li>Matching: %d</li>\n", result.getSummary().getMatchingObjects()));
        html.append("</ul>\n");

        if (!result.getDifferences().isEmpty()) {
            html.append("<h2>Differences</h2>\n");
            html.append("<table>\n<tr><th>Type</th><th>Name</th><th>Status</th><th>Severity</th></tr>\n");

            for (ObjectDifference diff : result.getDifferences()) {
                String cssClass = switch (diff.getDifferenceType()) {
                    case MISSING -> "missing";
                    case EXTRA -> "extra";
                    case MODIFIED -> "modified";
                };
                html.append(String.format("<tr class=\"%s\"><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>\n",
                        cssClass,
                        diff.getObjectType().getDisplayName(),
                        diff.getObjectName(),
                        diff.getDifferenceType().getDisplayName(),
                        diff.getSeverity().getDisplayName()));
            }

            html.append("</table>\n");
        }

        html.append("</body>\n</html>");

        String filename = String.format("comparison_%s_%s_vs_%s_%s.html",
                sourceInstance, sourceSchema, destInstance, destSchema);

        return Response.ok(html.toString())
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
            @QueryParam("destInstance") String destInstance,
            @QueryParam("sourceSchema") String sourceSchema,
            @QueryParam("destSchema") String destSchema) {
        featureToggleService.requirePageEnabled("schema-comparison");

        SchemaComparisonResult result = comparisonService.compare(
                sourceInstance, destInstance, sourceSchema, destSchema, null);

        StringBuilder md = new StringBuilder();
        md.append("# Schema Comparison Report\n\n");
        md.append(String.format("**Source:** %s.%s\n\n", result.getSourceInstance(), result.getSourceSchema()));
        md.append(String.format("**Destination:** %s.%s\n\n", result.getDestinationInstance(), result.getDestinationSchema()));
        md.append(String.format("**Compared at:** %s\n\n", result.getComparedAtFormatted()));

        md.append("## Summary\n\n");
        md.append(String.format("- Missing: %d\n", result.getSummary().getMissingObjects()));
        md.append(String.format("- Extra: %d\n", result.getSummary().getExtraObjects()));
        md.append(String.format("- Modified: %d\n", result.getSummary().getModifiedObjects()));
        md.append(String.format("- Matching: %d\n\n", result.getSummary().getMatchingObjects()));

        if (!result.getDifferences().isEmpty()) {
            md.append("## Differences\n\n");
            md.append("| Type | Name | Status | Severity |\n");
            md.append("|------|------|--------|----------|\n");

            for (ObjectDifference diff : result.getDifferences()) {
                md.append(String.format("| %s | %s | %s | %s |\n",
                        diff.getObjectType().getDisplayName(),
                        diff.getObjectName(),
                        diff.getDifferenceType().getDisplayName(),
                        diff.getSeverity().getDisplayName()));
            }
        }

        String filename = String.format("comparison_%s_%s_vs_%s_%s.md",
                sourceInstance, sourceSchema, destInstance, destSchema);

        return Response.ok(md.toString())
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .build();
    }

    /**
     * Get schema summary (HTMX).
     */
    @GET
    @Path("/schema-summary")
    @Produces(MediaType.TEXT_HTML)
    public String getSchemaSummary(
            @QueryParam("instance") String instance,
            @QueryParam("schema") String schema) {
        featureToggleService.requirePageEnabled("schema-comparison");

        Map<String, Integer> summary = comparisonService.getSchemaSummary(instance, schema);

        StringBuilder html = new StringBuilder();
        html.append("<div class=\"row g-2\">\n");

        for (Map.Entry<String, Integer> entry : summary.entrySet()) {
            html.append(String.format("""
                <div class="col-auto">
                    <span class="badge bg-secondary">%s: %d</span>
                </div>
                """, entry.getKey(), entry.getValue()));
        }

        html.append("</div>\n");
        return html.toString();
    }
}
