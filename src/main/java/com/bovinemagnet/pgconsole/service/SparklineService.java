package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.config.InstanceConfig;
import com.bovinemagnet.pgconsole.model.DatabaseMetricsHistory;
import com.bovinemagnet.pgconsole.model.InfrastructureMetricsHistory;
import com.bovinemagnet.pgconsole.model.QueryMetricsHistory;
import com.bovinemagnet.pgconsole.model.SystemMetricsHistory;
import com.bovinemagnet.pgconsole.repository.HistoryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Service for generating SVG sparkline charts.
 * <p>
 * Supports both persistent history (when schema is enabled) and in-memory
 * metrics (when schema is disabled for read-only mode).
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class SparklineService {

    @Inject
    HistoryRepository historyRepository;

    @Inject
    InMemoryMetricsStore inMemoryMetricsStore;

    @Inject
    InstanceConfig config;

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
     * <p>
     * When schema is disabled, uses in-memory metrics for short-term trends.
     *
     * @param instanceId the database instance identifier
     * @param hours the number of hours of history to retrieve
     * @param width the width of the SVG in pixels
     * @param height the height of the SVG in pixels
     * @return an SVG string representing the connections trend sparkline
     */
    public String getConnectionsSparkline(String instanceId, int hours, int width, int height) {
        List<SystemMetricsHistory> history = getSystemMetricsHistory(instanceId, hours);
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
     * <p>
     * When schema is disabled, uses in-memory metrics for short-term trends.
     *
     * @param instanceId the database instance identifier
     * @param hours the number of hours of history to retrieve
     * @param width the width of the SVG in pixels
     * @param height the height of the SVG in pixels
     * @return an SVG string representing the active queries trend sparkline
     */
    public String getActiveQueriesSparkline(String instanceId, int hours, int width, int height) {
        List<SystemMetricsHistory> history = getSystemMetricsHistory(instanceId, hours);
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
     * <p>
     * When schema is disabled, uses in-memory metrics for short-term trends.
     *
     * @param instanceId the database instance identifier
     * @param hours the number of hours of history to retrieve
     * @param width the width of the SVG in pixels
     * @param height the height of the SVG in pixels
     * @return an SVG string representing the blocked queries trend sparkline
     */
    public String getBlockedQueriesSparkline(String instanceId, int hours, int width, int height) {
        List<SystemMetricsHistory> history = getSystemMetricsHistory(instanceId, hours);
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
     * <p>
     * When schema is disabled, uses in-memory metrics for short-term trends.
     *
     * @param instanceId the database instance identifier
     * @param hours the number of hours of history to retrieve
     * @param width the width of the SVG in pixels
     * @param height the height of the SVG in pixels
     * @return an SVG string representing the cache hit ratio trend sparkline
     */
    public String getCacheHitRatioSparkline(String instanceId, int hours, int width, int height) {
        List<SystemMetricsHistory> history = getSystemMetricsHistory(instanceId, hours);
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

    // --- Helper methods for data source routing ---

    /**
     * Retrieves system metrics history from the appropriate data source.
     * <p>
     * When schema is enabled, retrieves from the persistent history repository.
     * When schema is disabled (read-only mode), retrieves from in-memory store.
     *
     * @param instanceId the database instance identifier
     * @param hours the number of hours of history to retrieve
     * @return list of system metrics history, may be empty if no data available
     */
    private List<SystemMetricsHistory> getSystemMetricsHistory(String instanceId, int hours) {
        if (config.schema().enabled()) {
            return historyRepository.getSystemMetricsHistory(instanceId, hours);
        } else {
            return inMemoryMetricsStore.getSystemMetricsHistory(instanceId, hours);
        }
    }

    /**
     * Checks if the schema is enabled (persistent storage mode).
     *
     * @return true if schema is enabled, false for read-only mode
     */
    public boolean isSchemaEnabled() {
        return config.schema().enabled();
    }

    // --- Generic rate computation ---

    /**
     * Computes per-second rates from consecutive cumulative counter samples.
     * <p>
     * Clamps negative deltas to 0 (handles stats_reset). Returns empty list
     * if fewer than 2 records.
     *
     * @param history        the list of history records ordered by time ascending
     * @param valueExtractor function to extract the cumulative counter value
     * @param timeExtractor  function to extract the timestamp
     * @param <T>            the type of history record
     * @return list of per-second rate values (one fewer than input)
     */
    public <T> List<Double> computeRates(List<T> history,
            Function<T, Long> valueExtractor, Function<T, Instant> timeExtractor) {
        if (history == null || history.size() < 2) {
            return new ArrayList<>();
        }

        List<Double> rates = new ArrayList<>();
        for (int i = 1; i < history.size(); i++) {
            Long prevVal = valueExtractor.apply(history.get(i - 1));
            Long currVal = valueExtractor.apply(history.get(i));
            Instant prevTime = timeExtractor.apply(history.get(i - 1));
            Instant currTime = timeExtractor.apply(history.get(i));

            if (prevVal == null || currVal == null || prevTime == null || currTime == null) {
                rates.add(0.0);
                continue;
            }

            double deltaSeconds = Duration.between(prevTime, currTime).toMillis() / 1000.0;
            if (deltaSeconds <= 0) {
                rates.add(0.0);
                continue;
            }

            long delta = currVal - prevVal;
            // Clamp negative deltas to 0 (stats_reset)
            if (delta < 0) {
                delta = 0;
            }
            rates.add(delta / deltaSeconds);
        }
        return rates;
    }

    // --- Data source routing helpers ---

    /**
     * Retrieves database metrics history from the appropriate data source.
     *
     * @param instanceId   the database instance identifier
     * @param databaseName the database name
     * @param hours        the number of hours of history to retrieve
     * @return list of database metrics history
     */
    public List<DatabaseMetricsHistory> getDatabaseMetricsHistory(
            String instanceId, String databaseName, int hours) {
        if (config.schema().enabled()) {
            return historyRepository.getDatabaseMetricsHistory(instanceId, databaseName, hours);
        } else {
            return inMemoryMetricsStore.getDatabaseMetricsHistory(instanceId, databaseName, hours);
        }
    }

    /**
     * Retrieves infrastructure metrics history from the appropriate data source.
     *
     * @param instanceId the database instance identifier
     * @param hours      the number of hours of history to retrieve
     * @return list of infrastructure metrics history
     */
    public List<InfrastructureMetricsHistory> getInfrastructureMetricsHistory(
            String instanceId, int hours) {
        if (config.schema().enabled()) {
            return historyRepository.getInfrastructureMetricsHistory(instanceId, hours);
        } else {
            return inMemoryMetricsStore.getInfrastructureMetricsHistory(instanceId, hours);
        }
    }

    // --- Per-database sparkline methods ---

    /**
     * Generates a commit rate sparkline for a database.
     */
    public String getDatabaseCommitRateSparkline(String instanceId, String dbName, int hours, int width, int height) {
        List<DatabaseMetricsHistory> history = getDatabaseMetricsHistory(instanceId, dbName, hours);
        List<Double> rates = computeRates(history, DatabaseMetricsHistory::getXactCommit, DatabaseMetricsHistory::getSampledAt);
        return generateAreaSparkline(rates, width, height, COLOUR_SUCCESS);
    }

    /**
     * Generates a rollback rate sparkline for a database.
     */
    public String getDatabaseRollbackRateSparkline(String instanceId, String dbName, int hours, int width, int height) {
        List<DatabaseMetricsHistory> history = getDatabaseMetricsHistory(instanceId, dbName, hours);
        List<Double> rates = computeRates(history, DatabaseMetricsHistory::getXactRollback, DatabaseMetricsHistory::getSampledAt);
        return generateAreaSparkline(rates, width, height, COLOUR_DANGER);
    }

    /**
     * Generates a tuple insert rate sparkline for a database.
     */
    public String getDatabaseTupleInsertRateSparkline(String instanceId, String dbName, int hours, int width, int height) {
        List<DatabaseMetricsHistory> history = getDatabaseMetricsHistory(instanceId, dbName, hours);
        List<Double> rates = computeRates(history, DatabaseMetricsHistory::getTupInserted, DatabaseMetricsHistory::getSampledAt);
        return generateAreaSparkline(rates, width, height, COLOUR_SUCCESS);
    }

    /**
     * Generates a tuple update rate sparkline for a database.
     */
    public String getDatabaseTupleUpdateRateSparkline(String instanceId, String dbName, int hours, int width, int height) {
        List<DatabaseMetricsHistory> history = getDatabaseMetricsHistory(instanceId, dbName, hours);
        List<Double> rates = computeRates(history, DatabaseMetricsHistory::getTupUpdated, DatabaseMetricsHistory::getSampledAt);
        return generateAreaSparkline(rates, width, height, COLOUR_PRIMARY);
    }

    /**
     * Generates a tuple delete rate sparkline for a database.
     */
    public String getDatabaseTupleDeleteRateSparkline(String instanceId, String dbName, int hours, int width, int height) {
        List<DatabaseMetricsHistory> history = getDatabaseMetricsHistory(instanceId, dbName, hours);
        List<Double> rates = computeRates(history, DatabaseMetricsHistory::getTupDeleted, DatabaseMetricsHistory::getSampledAt);
        return generateAreaSparkline(rates, width, height, COLOUR_DANGER);
    }

    /**
     * Generates a tuple fetch rate sparkline for a database.
     */
    public String getDatabaseTupleFetchRateSparkline(String instanceId, String dbName, int hours, int width, int height) {
        List<DatabaseMetricsHistory> history = getDatabaseMetricsHistory(instanceId, dbName, hours);
        List<Double> rates = computeRates(history, DatabaseMetricsHistory::getTupFetched, DatabaseMetricsHistory::getSampledAt);
        return generateAreaSparkline(rates, width, height, COLOUR_INFO);
    }

    /**
     * Generates a cache hits rate sparkline for a database.
     */
    public String getDatabaseBlocksHitRateSparkline(String instanceId, String dbName, int hours, int width, int height) {
        List<DatabaseMetricsHistory> history = getDatabaseMetricsHistory(instanceId, dbName, hours);
        List<Double> rates = computeRates(history, DatabaseMetricsHistory::getBlksHit, DatabaseMetricsHistory::getSampledAt);
        return generateAreaSparkline(rates, width, height, COLOUR_SUCCESS);
    }

    /**
     * Generates a disk reads rate sparkline for a database.
     */
    public String getDatabaseBlocksReadRateSparkline(String instanceId, String dbName, int hours, int width, int height) {
        List<DatabaseMetricsHistory> history = getDatabaseMetricsHistory(instanceId, dbName, hours);
        List<Double> rates = computeRates(history, DatabaseMetricsHistory::getBlksRead, DatabaseMetricsHistory::getSampledAt);
        return generateAreaSparkline(rates, width, height, COLOUR_WARNING);
    }

    /**
     * Generates a cache hit ratio sparkline for a database (direct value, not rate).
     */
    public String getDatabaseCacheHitRatioSparkline(String instanceId, String dbName, int hours, int width, int height) {
        List<DatabaseMetricsHistory> history = getDatabaseMetricsHistory(instanceId, dbName, hours);
        List<Double> values = history.stream()
            .map(h -> h.getCacheHitRatio() != null ? h.getCacheHitRatio() : 100.0)
            .toList();
        return generateSparkline(values, width, height, COLOUR_INFO);
    }

    /**
     * Generates a connections sparkline for a database (direct value, not rate).
     */
    public String getDatabaseConnectionsSparkline(String instanceId, String dbName, int hours, int width, int height) {
        List<DatabaseMetricsHistory> history = getDatabaseMetricsHistory(instanceId, dbName, hours);
        List<Double> values = history.stream()
            .map(h -> (double) h.getNumBackends())
            .toList();
        return generateAreaSparkline(values, width, height, COLOUR_PRIMARY);
    }

    // --- System-level idle connections sparkline ---

    /**
     * Generates an idle connections sparkline from system history.
     */
    public String getIdleConnectionsSparkline(String instanceId, int hours, int width, int height) {
        List<SystemMetricsHistory> history = getSystemMetricsHistory(instanceId, hours);
        List<Double> values = history.stream()
            .map(h -> (double) h.getIdleConnections())
            .toList();
        return generateAreaSparkline(values, width, height, COLOUR_WARNING);
    }

    // --- WAL sparkline methods ---

    /**
     * Generates a WAL bytes/sec rate sparkline.
     */
    public String getWalBytesRateSparkline(String instanceId, int hours, int width, int height) {
        List<InfrastructureMetricsHistory> history = getInfrastructureMetricsHistory(instanceId, hours);
        List<Double> rates = computeRates(history, InfrastructureMetricsHistory::getWalBytes, InfrastructureMetricsHistory::getSampledAt);
        return generateAreaSparkline(rates, width, height, COLOUR_PRIMARY);
    }

    /**
     * Generates a WAL records/sec rate sparkline.
     */
    public String getWalRecordsRateSparkline(String instanceId, int hours, int width, int height) {
        List<InfrastructureMetricsHistory> history = getInfrastructureMetricsHistory(instanceId, hours);
        List<Double> rates = computeRates(history, InfrastructureMetricsHistory::getWalRecords, InfrastructureMetricsHistory::getSampledAt);
        return generateAreaSparkline(rates, width, height, COLOUR_SUCCESS);
    }

    // --- Checkpoint sparkline methods ---

    /**
     * Generates a timed checkpoints rate sparkline.
     */
    public String getCheckpointsTimedSparkline(String instanceId, int hours, int width, int height) {
        List<InfrastructureMetricsHistory> history = getInfrastructureMetricsHistory(instanceId, hours);
        List<Double> rates = computeRates(history, InfrastructureMetricsHistory::getCheckpointsTimed, InfrastructureMetricsHistory::getSampledAt);
        return generateSparkline(rates, width, height, COLOUR_PRIMARY);
    }

    /**
     * Generates a requested checkpoints rate sparkline.
     */
    public String getCheckpointsReqSparkline(String instanceId, int hours, int width, int height) {
        List<InfrastructureMetricsHistory> history = getInfrastructureMetricsHistory(instanceId, hours);
        List<Double> rates = computeRates(history, InfrastructureMetricsHistory::getCheckpointsReq, InfrastructureMetricsHistory::getSampledAt);
        return generateSparkline(rates, width, height, COLOUR_WARNING);
    }

    /**
     * Generates a buffers allocated rate sparkline.
     */
    public String getBuffersAllocRateSparkline(String instanceId, int hours, int width, int height) {
        List<InfrastructureMetricsHistory> history = getInfrastructureMetricsHistory(instanceId, hours);
        List<Double> rates = computeRates(history, InfrastructureMetricsHistory::getBuffersAlloc, InfrastructureMetricsHistory::getSampledAt);
        return generateAreaSparkline(rates, width, height, COLOUR_INFO);
    }
}
