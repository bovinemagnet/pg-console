package com.bovinemagnet.pgconsole.resource;

import com.bovinemagnet.pgconsole.model.CustomDashboard;
import com.bovinemagnet.pgconsole.model.CustomWidget;
import com.bovinemagnet.pgconsole.service.CustomDashboardService;
import com.bovinemagnet.pgconsole.service.DataSourceManager;
import com.bovinemagnet.pgconsole.service.FeatureToggleService;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * REST resource for custom dashboard management.
 * Provides UI and API endpoints for creating, viewing, editing, and deleting
 * user-defined dashboards with customisable widgets.
 *
 * @author Paul Snow
 * @since 0.0.0
 */
@Path("/dashboards/custom")
public class CustomDashboardResource {

    @Inject
    Template customDashboards;

    @Inject
    Template customDashboard;

    @Inject
    Template customDashboardEdit;

    @Inject
    CustomDashboardService dashboardService;

    @Inject
    FeatureToggleService toggleService;

    @Inject
    DataSourceManager dataSourceManager;

    // ========================================
    // HTML Pages
    // ========================================

    /**
     * List all custom dashboards for an instance.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance listDashboards(
            @QueryParam("instance") @DefaultValue("default") String instance) {

        toggleService.requirePageEnabled("custom-dashboards");

        List<CustomDashboard> dashboards = dashboardService.getDashboards(instance);

        return customDashboards.data("instance", instance)
                .data("instances", dataSourceManager.getInstanceInfoList())
                .data("currentInstance", instance)
                .data("dashboards", dashboards)
                .data("toggles", toggleService.getAllToggles())
                .data("schemaEnabled", toggleService.isSchemaEnabled())
                .data("inMemoryMinutes", toggleService.getInMemoryMinutes());
    }

    /**
     * View a custom dashboard with its widgets rendered.
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance viewDashboard(
            @PathParam("id") Long id,
            @QueryParam("instance") @DefaultValue("default") String instance) {

        toggleService.requirePageEnabled("custom-dashboards");

        CustomDashboard dashboard = dashboardService.requireDashboard(id);
        dashboardService.loadWidgetData(dashboard, instance);

        return customDashboard.data("instance", instance)
                .data("instances", dataSourceManager.getInstanceInfoList())
                .data("currentInstance", instance)
                .data("dashboard", dashboard)
                .data("toggles", toggleService.getAllToggles())
                .data("schemaEnabled", toggleService.isSchemaEnabled())
                .data("inMemoryMinutes", toggleService.getInMemoryMinutes());
    }

    /**
     * Show the create dashboard form.
     */
    @GET
    @Path("/new")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance newDashboardForm(
            @QueryParam("instance") @DefaultValue("default") String instance) {

        toggleService.requirePageEnabled("custom-dashboards");

        if (!toggleService.isSchemaEnabled()) {
            throw new BadRequestException("Custom dashboards require schema mode to be enabled");
        }

        CustomDashboard dashboard = new CustomDashboard();
        dashboard.setInstanceId(instance);

        return customDashboardEdit.data("instance", instance)
                .data("instances", dataSourceManager.getInstanceInfoList())
                .data("currentInstance", instance)
                .data("dashboard", dashboard)
                .data("widgetTypes", dashboardService.getAvailableWidgetTypes())
                .data("isNew", true)
                .data("toggles", toggleService.getAllToggles())
                .data("schemaEnabled", toggleService.isSchemaEnabled())
                .data("inMemoryMinutes", toggleService.getInMemoryMinutes());
    }

    /**
     * Show the edit dashboard form.
     */
    @GET
    @Path("/{id}/edit")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance editDashboardForm(
            @PathParam("id") Long id,
            @QueryParam("instance") @DefaultValue("default") String instance) {

        toggleService.requirePageEnabled("custom-dashboards");

        if (!toggleService.isSchemaEnabled()) {
            throw new BadRequestException("Custom dashboards require schema mode to be enabled");
        }

        CustomDashboard dashboard = dashboardService.requireDashboard(id);

        return customDashboardEdit.data("instance", instance)
                .data("instances", dataSourceManager.getInstanceInfoList())
                .data("currentInstance", instance)
                .data("dashboard", dashboard)
                .data("widgetTypes", dashboardService.getAvailableWidgetTypes())
                .data("isNew", false)
                .data("toggles", toggleService.getAllToggles())
                .data("schemaEnabled", toggleService.isSchemaEnabled())
                .data("inMemoryMinutes", toggleService.getInMemoryMinutes());
    }

    // ========================================
    // Form Handling
    // ========================================

    /**
     * Create a new dashboard.
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response createDashboard(
            @FormParam("instance") @DefaultValue("default") String instance,
            @FormParam("name") String name,
            @FormParam("description") String description,
            @FormParam("isDefault") @DefaultValue("false") boolean isDefault,
            @FormParam("isShared") @DefaultValue("false") boolean isShared,
            @FormParam("tags") String tagsStr,
            @FormParam("widgetTypes") List<String> widgetTypes) {

        toggleService.requirePageEnabled("custom-dashboards");

        CustomDashboard dashboard = new CustomDashboard();
        dashboard.setInstanceId(instance);
        dashboard.setName(name);
        dashboard.setDescription(description);
        dashboard.setDefault(isDefault);
        dashboard.setShared(isShared);

        // Parse tags
        if (tagsStr != null && !tagsStr.isBlank()) {
            List<String> tags = Arrays.asList(tagsStr.split(","));
            dashboard.setTags(tags.stream().map(String::trim).filter(s -> !s.isEmpty()).toList());
        }

        // Add widgets
        if (widgetTypes != null) {
            List<CustomWidget> widgets = new ArrayList<>();
            int position = 0;
            for (String widgetType : widgetTypes) {
                CustomWidget widget = CustomWidget.builder()
                        .widgetType(widgetType)
                        .position(position++)
                        .width(6)
                        .height(1)
                        .build();
                widgets.add(widget);
            }
            dashboard.setWidgets(widgets);
        }

        CustomDashboard saved = dashboardService.createDashboard(dashboard);

        return Response.seeOther(URI.create("/dashboards/custom/" + saved.getId() + "?instance=" + instance))
                .build();
    }

    /**
     * Update an existing dashboard.
     */
    @POST
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response updateDashboard(
            @PathParam("id") Long id,
            @FormParam("instance") @DefaultValue("default") String instance,
            @FormParam("name") String name,
            @FormParam("description") String description,
            @FormParam("isDefault") @DefaultValue("false") boolean isDefault,
            @FormParam("isShared") @DefaultValue("false") boolean isShared,
            @FormParam("tags") String tagsStr,
            @FormParam("widgetTypes") List<String> widgetTypes) {

        toggleService.requirePageEnabled("custom-dashboards");

        CustomDashboard dashboard = dashboardService.requireDashboard(id);
        dashboard.setName(name);
        dashboard.setDescription(description);
        dashboard.setDefault(isDefault);
        dashboard.setShared(isShared);

        // Parse tags
        if (tagsStr != null && !tagsStr.isBlank()) {
            List<String> tags = Arrays.asList(tagsStr.split(","));
            dashboard.setTags(tags.stream().map(String::trim).filter(s -> !s.isEmpty()).toList());
        } else {
            dashboard.setTags(new ArrayList<>());
        }

        dashboardService.updateDashboard(dashboard);

        // Update widgets
        if (widgetTypes != null) {
            List<CustomWidget> widgets = new ArrayList<>();
            int position = 0;
            for (String widgetType : widgetTypes) {
                CustomWidget widget = CustomWidget.builder()
                        .dashboardId(id)
                        .widgetType(widgetType)
                        .position(position++)
                        .width(6)
                        .height(1)
                        .build();
                widgets.add(widget);
            }
            dashboardService.replaceWidgets(id, widgets);
        }

        return Response.seeOther(URI.create("/dashboards/custom/" + id + "?instance=" + instance))
                .build();
    }

    /**
     * Delete a dashboard.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.TEXT_HTML)
    public Response deleteDashboard(
            @PathParam("id") Long id,
            @QueryParam("instance") @DefaultValue("default") String instance) {

        toggleService.requirePageEnabled("custom-dashboards");

        dashboardService.deleteDashboard(id);

        return Response.seeOther(URI.create("/dashboards/custom?instance=" + instance))
                .build();
    }

    // ========================================
    // HTMX Endpoints
    // ========================================

    /**
     * Refresh a single widget's data (HTMX partial).
     */
    @GET
    @Path("/{dashboardId}/widget/{widgetId}/refresh")
    @Produces(MediaType.TEXT_HTML)
    public String refreshWidget(
            @PathParam("dashboardId") Long dashboardId,
            @PathParam("widgetId") Long widgetId,
            @QueryParam("instance") @DefaultValue("default") String instance) {

        toggleService.requirePageEnabled("custom-dashboards");

        CustomDashboard dashboard = dashboardService.requireDashboard(dashboardId);

        // Find the specific widget
        CustomWidget widget = dashboard.getWidgets().stream()
                .filter(w -> w.getId().equals(widgetId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Widget not found: " + widgetId));

        // Fetch widget data
        Object data = dashboardService.fetchWidgetData(widget, instance, null);
        widget.setData(data);

        // Return rendered widget content
        return renderWidgetContent(widget);
    }

    /**
     * Add a widget to a dashboard (HTMX).
     */
    @POST
    @Path("/{id}/widgets")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response addWidget(
            @PathParam("id") Long dashboardId,
            @QueryParam("instance") @DefaultValue("default") String instance,
            @FormParam("widgetType") String widgetType,
            @FormParam("title") String title,
            @FormParam("width") @DefaultValue("6") int width) {

        toggleService.requirePageEnabled("custom-dashboards");

        CustomWidget widget = CustomWidget.builder()
                .widgetType(widgetType)
                .title(title)
                .width(width)
                .height(1)
                .build();

        dashboardService.addWidget(dashboardId, widget);

        return Response.seeOther(URI.create("/dashboards/custom/" + dashboardId + "/edit?instance=" + instance))
                .build();
    }

    /**
     * Remove a widget from a dashboard (HTMX).
     */
    @DELETE
    @Path("/widgets/{widgetId}")
    @Produces(MediaType.TEXT_HTML)
    public Response removeWidget(
            @PathParam("widgetId") Long widgetId,
            @QueryParam("dashboardId") Long dashboardId,
            @QueryParam("instance") @DefaultValue("default") String instance) {

        toggleService.requirePageEnabled("custom-dashboards");

        dashboardService.deleteWidget(widgetId);

        if (dashboardId != null) {
            return Response.seeOther(URI.create("/dashboards/custom/" + dashboardId + "/edit?instance=" + instance))
                    .build();
        }

        return Response.ok().build();
    }

    // ========================================
    // API Endpoints
    // ========================================

    /**
     * API: Get all dashboards for an instance.
     */
    @GET
    @Path("/api")
    @Produces(MediaType.APPLICATION_JSON)
    public Response apiListDashboards(
            @QueryParam("instance") @DefaultValue("default") String instance) {

        toggleService.requirePageEnabled("custom-dashboards");

        List<CustomDashboard> dashboards = dashboardService.getDashboards(instance);

        return Response.ok(Map.of(
                "instance", instance,
                "dashboards", dashboards,
                "widgetTypes", dashboardService.getAvailableWidgetTypes()
        )).build();
    }

    /**
     * API: Get a single dashboard with widget data.
     */
    @GET
    @Path("/api/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response apiGetDashboard(
            @PathParam("id") Long id,
            @QueryParam("instance") @DefaultValue("default") String instance) {

        toggleService.requirePageEnabled("custom-dashboards");

        CustomDashboard dashboard = dashboardService.requireDashboard(id);
        dashboardService.loadWidgetData(dashboard, instance);

        return Response.ok(dashboard).build();
    }

    // ========================================
    // Helper Methods
    // ========================================

    /**
     * Render widget content as HTML string.
     */
    private String renderWidgetContent(CustomWidget widget) {
        Object data = widget.getData();
        String type = widget.getWidgetType();

        // Simple HTML rendering for widget types
        if (data instanceof Map<?, ?> map) {
            if (map.containsKey("error")) {
                return "<div class=\"alert alert-danger\">" + map.get("error") + "</div>";
            }

            StringBuilder html = new StringBuilder();
            html.append("<div class=\"widget-content\">");

            switch (type) {
                case "connections" -> {
                    Object current = map.get("current");
                    Object max = map.get("max");
                    Object pct = map.get("percentage");
                    html.append("<div class=\"display-4 text-primary\">").append(current != null ? current : 0).append("</div>");
                    html.append("<div class=\"text-muted\">of ").append(max != null ? max : 0).append(" max (").append(pct != null ? pct : 0).append("%)</div>");
                }
                case "cache-ratio" -> {
                    Object formatted = map.get("formatted");
                    html.append("<div class=\"display-4 text-success\">").append(formatted != null ? formatted : "0%").append("</div>");
                }
                case "active-queries", "blocked-queries" -> {
                    Object countObj = map.get("count");
                    int count = countObj instanceof Number ? ((Number) countObj).intValue() : 0;
                    String colour = type.equals("blocked-queries") && count > 0 ? "text-danger" : "text-info";
                    html.append("<div class=\"display-4 ").append(colour).append("\">").append(count).append("</div>");
                }
                case "db-size" -> {
                    Object formatted = map.get("formatted");
                    html.append("<div class=\"display-4 text-info\">").append(formatted != null ? formatted : "0 B").append("</div>");
                }
                case "longest-query" -> {
                    Object formatted = map.get("formatted");
                    html.append("<div class=\"display-4 text-warning\">").append(formatted != null ? formatted : "0 ms").append("</div>");
                }
                default -> {
                    // Generic key-value display
                    html.append("<dl class=\"row mb-0\">");
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        html.append("<dt class=\"col-6\">").append(entry.getKey()).append("</dt>");
                        html.append("<dd class=\"col-6\">").append(entry.getValue()).append("</dd>");
                    }
                    html.append("</dl>");
                }
            }

            html.append("</div>");
            return html.toString();
        } else if (data instanceof List<?>) {
            // Table data
            return "<div class=\"text-muted\">Table data available</div>";
        } else if (data instanceof String) {
            // SVG sparkline or other string content
            return data.toString();
        }

        return "<div class=\"text-muted\">No data</div>";
    }
}
