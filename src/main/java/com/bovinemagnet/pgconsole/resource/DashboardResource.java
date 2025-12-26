package com.bovinemagnet.pgconsole.resource;

import com.bovinemagnet.pgconsole.model.Activity;
import com.bovinemagnet.pgconsole.model.SlowQuery;
import com.bovinemagnet.pgconsole.model.TableStats;
import com.bovinemagnet.pgconsole.service.PostgresService;
import com.bovinemagnet.pgconsole.service.SparklineService;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.Arrays;
import java.util.List;

@Path("/")
public class DashboardResource {

    @Inject
    Template index;

    @Inject
    Template slowQueries;

    @Inject
    Template activity;

    @Inject
    Template tables;

    @Inject
    PostgresService postgresService;

    @Inject
    SparklineService sparklineService;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index() {
        return index.instance();
    }

    @GET
    @Path("/slow-queries")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance slowQueries(
            @QueryParam("sortBy") String sortBy,
            @QueryParam("order") String order) {
        
        List<SlowQuery> queries = postgresService.getSlowQueries(
            sortBy != null ? sortBy : "totalTime",
            order != null ? order : "desc"
        );
        
        return slowQueries.data("queries", queries)
                         .data("sortBy", sortBy != null ? sortBy : "totalTime")
                         .data("order", order != null ? order : "desc");
    }

    @GET
    @Path("/activity")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance activity() {
        List<Activity> activities = postgresService.getCurrentActivity();
        return activity.data("activities", activities);
    }

    @GET
    @Path("/tables")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance tables() {
        List<TableStats> stats = postgresService.getTableStats();
        return tables.data("tables", stats);
    }

    @GET
    @Path("/api/sparkline")
    @Produces(MediaType.TEXT_HTML)
    public String sparkline(@QueryParam("values") String values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        
        List<Double> valueList = Arrays.stream(values.split(","))
            .map(String::trim)
            .map(Double::parseDouble)
            .toList();
        
        return sparklineService.generateSparkline(valueList, 100, 30);
    }
}
