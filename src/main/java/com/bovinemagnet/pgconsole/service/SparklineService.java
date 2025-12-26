package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.QueryMetricsHistory;
import com.bovinemagnet.pgconsole.model.SystemMetricsHistory;
import com.bovinemagnet.pgconsole.repository.HistoryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.function.Function;

/**
 * Service for generating SVG sparkline charts.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class SparklineService {

    @Inject
    HistoryRepository historyRepository;

    // Sparkline colours
    public static final String COLOUR_PRIMARY = "#0d6efd";
    public static final String COLOUR_SUCCESS = "#198754";
    public static final String COLOUR_WARNING = "#ffc107";
    public static final String COLOUR_DANGER = "#dc3545";
    public static final String COLOUR_INFO = "#0dcaf0";

    /**
     * Generates a sparkline SVG from a list of values using the primary colour.
     * <p>
     * A sparkline is a small, simple line chart without axes or labels, designed to show
     * trends in a compact space. This method creates an SVG path with automatic scaling
     * to fit the provided dimensions.
     *
     * @param values the list of numeric values to plot; must contain at least 2 values
     * @param width the width of the SVG in pixels
     * @param height the height of the SVG in pixels
     * @return an SVG string representing the sparkline, or an empty sparkline if values is null or too small
     * @see #generateSparkline(List, int, int, String)
     */
    public String generateSparkline(List<Double> values, int width, int height) {
        return generateSparkline(values, width, height, COLOUR_PRIMARY);
    }

    /**
     * Generates a sparkline SVG with custom colour.
     * <p>
     * This method creates a line chart sparkline with automatic scaling and includes
     * a dot marker at the last data point. Values are normalised to fit within the
     * specified dimensions with a 1-pixel margin.
     *
     * @param values the list of numeric values to plot; must contain at least 2 values
     * @param width the width of the SVG in pixels
     * @param height the height of the SVG in pixels
     * @param colour the hex colour code for the line and dot (e.g., "#0d6efd")
     * @return an SVG string representing the sparkline, or an empty sparkline if values is null or too small
     */
    public String generateSparkline(List<Double> values, int width, int height, String colour) {
        if (values == null || values.size() < 2) {
            return generateEmptySparkline(width, height);
        }

        double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        double range = max - min;
        if (range == 0) range = 1.0;

        StringBuilder path = new StringBuilder();
        double step = (double) width / (values.size() - 1);

        for (int i = 0; i < values.size(); i++) {
            double x = i * step;
            double normalizedValue = (values.get(i) - min) / range;
            double y = height - (normalizedValue * (height - 2)) - 1; // Leave 1px margin

            if (i == 0) {
                path.append("M ").append(String.format("%.2f", x)).append(" ").append(String.format("%.2f", y));
            } else {
                path.append(" L ").append(String.format("%.2f", x)).append(" ").append(String.format("%.2f", y));
            }
        }

        // Add current value dot at the end
        double lastX = (values.size() - 1) * step;
        double lastY = height - ((values.get(values.size() - 1) - min) / range * (height - 2)) - 1;

        return String.format(
            "<svg width=\"%d\" height=\"%d\" class=\"sparkline\" xmlns=\"http://www.w3.org/2000/svg\">" +
            "<path d=\"%s\" fill=\"none\" stroke=\"%s\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/>" +
            "<circle cx=\"%.2f\" cy=\"%.2f\" r=\"2\" fill=\"%s\"/>" +
            "</svg>",
            width, height, path.toString(), colour, lastX, lastY, colour
        );
    }

    /**
     * Generates an area sparkline (filled under the line).
     * <p>
     * Creates a sparkline with a filled area underneath the line, useful for showing
     * cumulative or volume-based metrics. The area is rendered with 20% opacity to
     * allow overlaying multiple sparklines.
     *
     * @param values the list of numeric values to plot; must contain at least 2 values
     * @param width the width of the SVG in pixels
     * @param height the height of the SVG in pixels
     * @param colour the hex colour code for the line and fill area (e.g., "#0d6efd")
     * @return an SVG string representing the area sparkline, or an empty sparkline if values is null or too small
     */
    public String generateAreaSparkline(List<Double> values, int width, int height, String colour) {
        if (values == null || values.size() < 2) {
            return generateEmptySparkline(width, height);
        }

        double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        double range = max - min;
        if (range == 0) range = 1.0;

        StringBuilder linePath = new StringBuilder();
        StringBuilder areaPath = new StringBuilder();
        double step = (double) width / (values.size() - 1);

        areaPath.append("M 0 ").append(height);

        for (int i = 0; i < values.size(); i++) {
            double x = i * step;
            double normalizedValue = (values.get(i) - min) / range;
            double y = height - (normalizedValue * (height - 2)) - 1;

            if (i == 0) {
                linePath.append("M ").append(String.format("%.2f", x)).append(" ").append(String.format("%.2f", y));
                areaPath.append(" L ").append(String.format("%.2f", x)).append(" ").append(String.format("%.2f", y));
            } else {
                linePath.append(" L ").append(String.format("%.2f", x)).append(" ").append(String.format("%.2f", y));
                areaPath.append(" L ").append(String.format("%.2f", x)).append(" ").append(String.format("%.2f", y));
            }
        }

        areaPath.append(" L ").append(width).append(" ").append(height).append(" Z");

        return String.format(
            "<svg width=\"%d\" height=\"%d\" class=\"sparkline\" xmlns=\"http://www.w3.org/2000/svg\">" +
            "<path d=\"%s\" fill=\"%s\" fill-opacity=\"0.2\"/>" +
            "<path d=\"%s\" fill=\"none\" stroke=\"%s\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/>" +
            "</svg>",
            width, height, areaPath.toString(), colour, linePath.toString(), colour
        );
    }

    /**
     * Generates an empty sparkline placeholder.
     * <p>
     * Creates a dashed horizontal line in the middle of the space to indicate
     * no data is available. This provides a consistent visual placeholder when
     * sparkline data is insufficient or missing.
     *
     * @param width the width of the SVG in pixels
     * @param height the height of the SVG in pixels
     * @return an SVG string representing an empty sparkline placeholder
     */
    public String generateEmptySparkline(int width, int height) {
        return String.format(
            "<svg width=\"%d\" height=\"%d\" class=\"sparkline sparkline-empty\" xmlns=\"http://www.w3.org/2000/svg\">" +
            "<line x1=\"0\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#6c757d\" stroke-width=\"1\" stroke-dasharray=\"2,2\" opacity=\"0.5\"/>" +
            "</svg>",
            width, height, height / 2, width, height / 2
        );
    }

    // --- Convenience methods for common sparkline types ---

    /**
     * Generates a connections sparkline from system history for an instance.
     * <p>
     * Creates an area sparkline showing the total connection count trend over the
     * specified time period. Uses the primary colour (blue) for the visualisation.
     *
     * @param instanceId the database instance identifier
     * @param hours the number of hours of history to retrieve
     * @param width the width of the SVG in pixels
     * @param height the height of the SVG in pixels
     * @return an SVG string representing the connections trend sparkline
     */
    public String getConnectionsSparkline(String instanceId, int hours, int width, int height) {
        List<SystemMetricsHistory> history = historyRepository.getSystemMetricsHistory(instanceId, hours);
        List<Double> values = history.stream()
            .map(h -> (double) h.getTotalConnections())
            .toList();
        return generateAreaSparkline(values, width, height, COLOUR_PRIMARY);
    }

    /**
     * Generates a connections sparkline for the default instance.
     * <p>
     * Backward-compatible overload that uses the "default" instance identifier.
     *
     * @param hours the number of hours of history to retrieve
     * @param width the width of the SVG in pixels
     * @param height the height of the SVG in pixels
     * @return an SVG string representing the connections trend sparkline
     * @see #getConnectionsSparkline(String, int, int, int)
     */
    public String getConnectionsSparkline(int hours, int width, int height) {
        return getConnectionsSparkline("default", hours, width, height);
    }

    /**
     * Generates an active queries sparkline from system history for an instance.
     * <p>
     * Creates an area sparkline showing the count of actively executing queries over time.
     * Uses the success colour (green) for the visualisation.
     *
     * @param instanceId the database instance identifier
     * @param hours the number of hours of history to retrieve
     * @param width the width of the SVG in pixels
     * @param height the height of the SVG in pixels
     * @return an SVG string representing the active queries trend sparkline
     */
    public String getActiveQueriesSparkline(String instanceId, int hours, int width, int height) {
        List<SystemMetricsHistory> history = historyRepository.getSystemMetricsHistory(instanceId, hours);
        List<Double> values = history.stream()
            .map(h -> (double) h.getActiveQueries())
            .toList();
        return generateAreaSparkline(values, width, height, COLOUR_SUCCESS);
    }

    /**
     * Generates an active queries sparkline for the default instance.
     * Backward-compatible overload that uses the "default" instance identifier.
     *
     * @param hours the number of hours of history to retrieve
     * @param width the width of the SVG in pixels
     * @param height the height of the SVG in pixels
     * @return an SVG string representing the active queries trend sparkline
     * @see #getActiveQueriesSparkline(String, int, int, int)
     */
    public String getActiveQueriesSparkline(int hours, int width, int height) {
        return getActiveQueriesSparkline("default", hours, width, height);
    }

    /**
     * Generates a blocked queries sparkline from system history for an instance.
     * <p>
     * Creates an area sparkline showing the count of blocked (waiting for locks) queries over time.
     * Uses the danger colour (red) for the visualisation.
     *
     * @param instanceId the database instance identifier
     * @param hours the number of hours of history to retrieve
     * @param width the width of the SVG in pixels
     * @param height the height of the SVG in pixels
     * @return an SVG string representing the blocked queries trend sparkline
     */
    public String getBlockedQueriesSparkline(String instanceId, int hours, int width, int height) {
        List<SystemMetricsHistory> history = historyRepository.getSystemMetricsHistory(instanceId, hours);
        List<Double> values = history.stream()
            .map(h -> (double) h.getBlockedQueries())
            .toList();
        return generateAreaSparkline(values, width, height, COLOUR_DANGER);
    }

    /**
     * Generates a blocked queries sparkline for the default instance.
     * Backward-compatible overload that uses the "default" instance identifier.
     *
     * @param hours the number of hours of history to retrieve
     * @param width the width of the SVG in pixels
     * @param height the height of the SVG in pixels
     * @return an SVG string representing the blocked queries trend sparkline
     * @see #getBlockedQueriesSparkline(String, int, int, int)
     */
    public String getBlockedQueriesSparkline(int hours, int width, int height) {
        return getBlockedQueriesSparkline("default", hours, width, height);
    }

    /**
     * Generates a cache hit ratio sparkline from system history for an instance.
     * <p>
     * Creates a line sparkline showing the buffer cache hit ratio percentage over time.
     * Uses the info colour (cyan) for the visualisation. Missing or null values default to 100%.
     *
     * @param instanceId the database instance identifier
     * @param hours the number of hours of history to retrieve
     * @param width the width of the SVG in pixels
     * @param height the height of the SVG in pixels
     * @return an SVG string representing the cache hit ratio trend sparkline
     */
    public String getCacheHitRatioSparkline(String instanceId, int hours, int width, int height) {
        List<SystemMetricsHistory> history = historyRepository.getSystemMetricsHistory(instanceId, hours);
        List<Double> values = history.stream()
            .map(h -> h.getCacheHitRatio() != null ? h.getCacheHitRatio() : 100.0)
            .toList();
        return generateSparkline(values, width, height, COLOUR_INFO);
    }

    /**
     * Generates a cache hit ratio sparkline for the default instance.
     * Backward-compatible overload that uses the "default" instance identifier.
     *
     * @param hours the number of hours of history to retrieve
     * @param width the width of the SVG in pixels
     * @param height the height of the SVG in pixels
     * @return an SVG string representing the cache hit ratio trend sparkline
     * @see #getCacheHitRatioSparkline(String, int, int, int)
     */
    public String getCacheHitRatioSparkline(int hours, int width, int height) {
        return getCacheHitRatioSparkline("default", hours, width, height);
    }

    /**
     * Generates a query mean time sparkline from query history for an instance.
     * <p>
     * Creates a line sparkline showing the mean execution time trend for a specific query
     * over the specified time period. Uses the warning colour (yellow) for the visualisation.
     *
     * @param instanceId the database instance identifier
     * @param queryId the MD5 hash identifier of the query
     * @param hours the number of hours of history to retrieve
     * @param width the width of the SVG in pixels
     * @param height the height of the SVG in pixels
     * @return an SVG string representing the query mean time trend sparkline
     */
    public String getQueryMeanTimeSparkline(String instanceId, String queryId, int hours, int width, int height) {
        List<QueryMetricsHistory> history = historyRepository.getQueryMetricsHistory(instanceId, queryId, hours);
        List<Double> values = history.stream()
            .map(QueryMetricsHistory::getMeanTimeMs)
            .toList();
        return generateSparkline(values, width, height, COLOUR_WARNING);
    }

    /**
     * Generates a query mean time sparkline for the default instance.
     * Backward-compatible overload that uses the "default" instance identifier.
     *
     * @param queryId the MD5 hash identifier of the query
     * @param hours the number of hours of history to retrieve
     * @param width the width of the SVG in pixels
     * @param height the height of the SVG in pixels
     * @return an SVG string representing the query mean time trend sparkline
     * @see #getQueryMeanTimeSparkline(String, String, int, int, int)
     */
    public String getQueryMeanTimeSparkline(String queryId, int hours, int width, int height) {
        return getQueryMeanTimeSparkline("default", queryId, hours, width, height);
    }

    /**
     * Generates a query calls sparkline from query history for an instance.
     * <p>
     * Creates a line sparkline showing the total call count trend for a specific query
     * over the specified time period. Uses the primary colour (blue) for the visualisation.
     *
     * @param instanceId the database instance identifier
     * @param queryId the MD5 hash identifier of the query
     * @param hours the number of hours of history to retrieve
     * @param width the width of the SVG in pixels
     * @param height the height of the SVG in pixels
     * @return an SVG string representing the query calls trend sparkline
     */
    public String getQueryCallsSparkline(String instanceId, String queryId, int hours, int width, int height) {
        List<QueryMetricsHistory> history = historyRepository.getQueryMetricsHistory(instanceId, queryId, hours);
        List<Double> values = history.stream()
            .map(h -> (double) h.getTotalCalls())
            .toList();
        return generateSparkline(values, width, height, COLOUR_PRIMARY);
    }

    /**
     * Generates a query calls sparkline for the default instance.
     * Backward-compatible overload that uses the "default" instance identifier.
     *
     * @param queryId the MD5 hash identifier of the query
     * @param hours the number of hours of history to retrieve
     * @param width the width of the SVG in pixels
     * @param height the height of the SVG in pixels
     * @return an SVG string representing the query calls trend sparkline
     * @see #getQueryCallsSparkline(String, String, int, int, int)
     */
    public String getQueryCallsSparkline(String queryId, int hours, int width, int height) {
        return getQueryCallsSparkline("default", queryId, hours, width, height);
    }
}
