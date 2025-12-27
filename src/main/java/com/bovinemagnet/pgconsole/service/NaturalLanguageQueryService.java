package com.bovinemagnet.pgconsole.service;

import com.bovinemagnet.pgconsole.model.NaturalLanguageQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for parsing natural language queries and mapping them to dashboard endpoints.
 * <p>
 * Uses pattern matching to understand user intent and extract relevant parameters.
 * Supports queries like "show me slow queries from yesterday" or "why is the database slow".
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class NaturalLanguageQueryService {

    private static final Logger LOG = Logger.getLogger(NaturalLanguageQueryService.class);

    // Built-in patterns (loaded at startup)
    private static final List<QueryPattern> BUILT_IN_PATTERNS = List.of(
            // Slow queries patterns
            new QueryPattern("(?i)slow\\s+quer(y|ies).*yesterday",
                    NaturalLanguageQuery.Intent.SLOW_QUERIES_YESTERDAY, 100),
            new QueryPattern("(?i)slow\\s+quer(y|ies).*last\\s+(\\d+)\\s+hours?",
                    NaturalLanguageQuery.Intent.SLOW_QUERIES_HOURS, 90),
            new QueryPattern("(?i)slow\\s+quer(y|ies)",
                    NaturalLanguageQuery.Intent.SLOW_QUERIES, 50),
            new QueryPattern("(?i)long\\s+running\\s+quer(y|ies)",
                    NaturalLanguageQuery.Intent.SLOW_QUERIES, 50),

            // Table/storage patterns
            new QueryPattern("(?i)which\\s+tables?\\s+(are\\s+)?growing\\s+(fastest|quickly)",
                    NaturalLanguageQuery.Intent.TABLE_GROWTH, 100),
            new QueryPattern("(?i)table\\s+(size|growth|storage)",
                    NaturalLanguageQuery.Intent.TABLE_SIZE, 50),
            new QueryPattern("(?i)biggest\\s+tables?",
                    NaturalLanguageQuery.Intent.TABLE_SIZE, 60),
            new QueryPattern("(?i)largest\\s+tables?",
                    NaturalLanguageQuery.Intent.TABLE_SIZE, 60),

            // Diagnosis patterns
            new QueryPattern("(?i)why\\s+(is\\s+)?(the\\s+)?database\\s+slow",
                    NaturalLanguageQuery.Intent.SLOW_DIAGNOSIS, 100),
            new QueryPattern("(?i)what('s|\\s+is)\\s+wrong",
                    NaturalLanguageQuery.Intent.SLOW_DIAGNOSIS, 90),
            new QueryPattern("(?i)diagnos(e|tic|is)",
                    NaturalLanguageQuery.Intent.SLOW_DIAGNOSIS, 80),

            // Lock patterns
            new QueryPattern("(?i)(current\\s+)?lock(s|ing)?",
                    NaturalLanguageQuery.Intent.LOCKS, 50),
            new QueryPattern("(?i)block(ed|ing)",
                    NaturalLanguageQuery.Intent.BLOCKING, 60),
            new QueryPattern("(?i)deadlock",
                    NaturalLanguageQuery.Intent.BLOCKING, 70),

            // Activity patterns
            new QueryPattern("(?i)active\\s+(connections?|sessions?|queries?)",
                    NaturalLanguageQuery.Intent.ACTIVITY, 50),
            new QueryPattern("(?i)who\\s+is\\s+connected",
                    NaturalLanguageQuery.Intent.ACTIVITY, 60),
            new QueryPattern("(?i)current\\s+(sessions?|connections?)",
                    NaturalLanguageQuery.Intent.ACTIVITY, 50),

            // Replication patterns
            new QueryPattern("(?i)replication\\s+(status|lag)",
                    NaturalLanguageQuery.Intent.REPLICATION, 50),
            new QueryPattern("(?i)replica\\s+lag",
                    NaturalLanguageQuery.Intent.REPLICATION, 60),

            // Index patterns
            new QueryPattern("(?i)(index|indexes)\\s+(recommend|suggest|advice)",
                    NaturalLanguageQuery.Intent.INDEX_ADVISOR, 50),
            new QueryPattern("(?i)missing\\s+index(es)?",
                    NaturalLanguageQuery.Intent.INDEX_ADVISOR, 60),
            new QueryPattern("(?i)unused\\s+index(es)?",
                    NaturalLanguageQuery.Intent.INDEX_ADVISOR, 60),

            // Vacuum patterns
            new QueryPattern("(?i)vacuum\\s+(status|progress|recommend)",
                    NaturalLanguageQuery.Intent.VACUUM, 50),
            new QueryPattern("(?i)dead\\s+tuples?",
                    NaturalLanguageQuery.Intent.VACUUM, 60),
            new QueryPattern("(?i)table\\s+bloat",
                    NaturalLanguageQuery.Intent.VACUUM, 60),

            // Storage patterns
            new QueryPattern("(?i)(disk|storage)\\s+(usage|space)",
                    NaturalLanguageQuery.Intent.STORAGE, 50),
            new QueryPattern("(?i)database\\s+size",
                    NaturalLanguageQuery.Intent.STORAGE, 50),

            // Anomaly patterns
            new QueryPattern("(?i)anomal(y|ies)",
                    NaturalLanguageQuery.Intent.ANOMALIES, 50),
            new QueryPattern("(?i)unusual\\s+(activity|behaviour)",
                    NaturalLanguageQuery.Intent.ANOMALIES, 60),

            // Forecast patterns
            new QueryPattern("(?i)forecast|predict",
                    NaturalLanguageQuery.Intent.FORECASTS, 50),
            new QueryPattern("(?i)when\\s+will\\s+(storage|disk)\\s+(be\\s+)?full",
                    NaturalLanguageQuery.Intent.FORECASTS, 70),
            new QueryPattern("(?i)capacity\\s+planning",
                    NaturalLanguageQuery.Intent.FORECASTS, 60),

            // Recommendation patterns
            new QueryPattern("(?i)recommend(ations)?",
                    NaturalLanguageQuery.Intent.RECOMMENDATIONS, 50),
            new QueryPattern("(?i)suggest(ions)?",
                    NaturalLanguageQuery.Intent.RECOMMENDATIONS, 50),
            new QueryPattern("(?i)what\\s+should\\s+i\\s+(do|fix)",
                    NaturalLanguageQuery.Intent.RECOMMENDATIONS, 70)
    );

    @Inject
    DataSourceManager dataSourceManager;

    /**
     * Parse a natural language query and determine intent.
     *
     * @param queryText the natural language query
     * @param instanceName the PostgreSQL instance name
     * @return parsed query with intent and parameters
     */
    public NaturalLanguageQuery parseQuery(String queryText, String instanceName) {
        NaturalLanguageQuery query = new NaturalLanguageQuery(queryText);

        if (queryText == null || queryText.isBlank()) {
            query.setMatchedIntent(NaturalLanguageQuery.Intent.UNKNOWN);
            query.setSuccessful(false);
            return query;
        }

        // Try to match against patterns
        QueryPattern bestMatch = null;
        Matcher bestMatcher = null;
        int bestPriority = -1;

        for (QueryPattern pattern : BUILT_IN_PATTERNS) {
            Matcher matcher = pattern.pattern.matcher(queryText);
            if (matcher.find() && pattern.priority > bestPriority) {
                bestMatch = pattern;
                bestMatcher = matcher;
                bestPriority = pattern.priority;
            }
        }

        if (bestMatch != null) {
            query.setMatchedIntent(bestMatch.intent);
            query.setMatchedPattern(bestMatch.patternString);
            query.setConfidenceScore(bestPriority / 100.0);

            // Extract parameters
            Map<String, String> params = extractParameters(queryText, bestMatcher, bestMatch.intent);
            query.setExtractedParameters(params);

            // Build resolved URL
            query.setResolvedTo(query.buildUrl(instanceName));
            query.setSuccessful(true);
        } else {
            query.setMatchedIntent(NaturalLanguageQuery.Intent.UNKNOWN);
            query.setSuccessful(false);
        }

        // Log query for learning
        logQuery(instanceName, query);

        return query;
    }

    /**
     * Get recent query history for learning/debugging.
     *
     * @param instanceName the PostgreSQL instance name
     * @param limit maximum number of queries to return
     * @return list of recent queries
     */
    public List<NaturalLanguageQuery> getQueryHistory(String instanceName, int limit) {
        List<NaturalLanguageQuery> history = new ArrayList<>();

        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            String sql = """
                SELECT id, query_text, matched_intent, resolved_to, successful,
                       user_feedback, queried_at, queried_by
                FROM pgconsole.nl_query_history
                ORDER BY queried_at DESC
                LIMIT ?
                """;

            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, limit);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        NaturalLanguageQuery query = new NaturalLanguageQuery();
                        query.setId(rs.getLong("id"));
                        query.setQueryText(rs.getString("query_text"));

                        String intentStr = rs.getString("matched_intent");
                        if (intentStr != null) {
                            try {
                                query.setMatchedIntent(NaturalLanguageQuery.Intent.valueOf(intentStr));
                            } catch (IllegalArgumentException e) {
                                query.setMatchedIntent(NaturalLanguageQuery.Intent.UNKNOWN);
                            }
                        }

                        query.setResolvedTo(rs.getString("resolved_to"));
                        query.setSuccessful(rs.getBoolean("successful"));

                        String feedbackStr = rs.getString("user_feedback");
                        if (feedbackStr != null) {
                            try {
                                query.setUserFeedback(NaturalLanguageQuery.Feedback.valueOf(feedbackStr));
                            } catch (IllegalArgumentException e) {
                                // Ignore unknown feedback
                            }
                        }

                        Timestamp queriedAt = rs.getTimestamp("queried_at");
                        if (queriedAt != null) {
                            query.setQueriedAt(queriedAt.toInstant());
                        }
                        query.setQueriedBy(rs.getString("queried_by"));

                        history.add(query);
                    }
                }

            } catch (SQLException e) {
                // Table might not exist yet
                LOG.debugf(e, "Error getting query history");
            }

        } catch (Exception e) {
            LOG.debugf(e, "Error getting query history");
        }

        return history;
    }

    /**
     * Record user feedback on a query result.
     *
     * @param instanceName the PostgreSQL instance name
     * @param queryId the query ID
     * @param feedback the user feedback
     */
    public void recordFeedback(String instanceName, long queryId, NaturalLanguageQuery.Feedback feedback) {
        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            String sql = "UPDATE pgconsole.nl_query_history SET user_feedback = ? WHERE id = ?";

            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, feedback.name());
                stmt.setLong(2, queryId);
                stmt.executeUpdate();
            }

        } catch (Exception e) {
            LOG.debugf(e, "Error recording feedback");
        }
    }

    /**
     * Get suggested queries based on common patterns.
     *
     * @return list of example queries users can try
     */
    public List<String> getSuggestedQueries() {
        return List.of(
                "Show me slow queries from yesterday",
                "Why is the database slow?",
                "Which tables are growing fastest?",
                "Show blocked queries",
                "What are the index recommendations?",
                "Show storage usage",
                "Active connections",
                "Replication lag",
                "Show anomalies",
                "When will storage be full?"
        );
    }

    /**
     * Get a plain English explanation for a metric or term.
     *
     * @param term the term to explain
     * @return explanation text
     */
    public String explainTerm(String term) {
        String lowerTerm = term.toLowerCase();

        return switch (lowerTerm) {
            case "cache hit ratio" -> "The percentage of data requests that are served from " +
                    "PostgreSQL's shared buffer cache instead of reading from disk. " +
                    "Higher is better - aim for above 99%.";
            case "dead tuples" -> "Rows that have been deleted or updated but haven't been " +
                    "cleaned up by VACUUM yet. Too many dead tuples cause table bloat " +
                    "and slower queries.";
            case "seq scan" -> "A sequential scan reads every row in a table. This is slow " +
                    "for large tables. High seq scan ratios often indicate missing indexes.";
            case "blocking" -> "When one query is waiting for another query to release a lock. " +
                    "This can cause cascading delays affecting many queries.";
            case "replication lag" -> "The delay between when data is written to the primary " +
                    "database and when it's available on replicas. Lower is better.";
            case "shared_buffers" -> "The amount of memory PostgreSQL uses for caching data. " +
                    "Typically set to 25% of available RAM for dedicated database servers.";
            case "work_mem" -> "Memory used for sorting and hashing operations within a query. " +
                    "Higher values can speed up complex queries but increase memory usage.";
            case "autovacuum" -> "An automatic process that cleans up dead tuples and updates " +
                    "table statistics. Essential for database health.";
            default -> "No explanation available for '" + term + "'.";
        };
    }

    // Private helper methods

    private Map<String, String> extractParameters(String queryText, Matcher matcher,
                                                    NaturalLanguageQuery.Intent intent) {
        Map<String, String> params = new HashMap<>();

        switch (intent) {
            case SLOW_QUERIES_HOURS -> {
                // Extract hour count from pattern
                Pattern hoursPattern = Pattern.compile("(\\d+)\\s+hours?", Pattern.CASE_INSENSITIVE);
                Matcher hoursMatcher = hoursPattern.matcher(queryText);
                if (hoursMatcher.find()) {
                    params.put("hours", hoursMatcher.group(1));
                }
            }
            case SLOW_QUERIES_YESTERDAY -> {
                params.put("timeRange", "yesterday");
            }
            case BLOCKING -> {
                params.put("focus", "blocking");
            }
            case TABLE_GROWTH -> {
                params.put("sort", "growth");
            }
            default -> {
                // No additional parameters needed
            }
        }

        return params;
    }

    private void logQuery(String instanceName, NaturalLanguageQuery query) {
        try {
            DataSource ds = dataSourceManager.getDataSource(instanceName);

            String sql = """
                INSERT INTO pgconsole.nl_query_history
                    (query_text, matched_intent, resolved_to, successful, queried_at)
                VALUES (?, ?, ?, ?, NOW())
                """;

            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                stmt.setString(1, query.getQueryText());
                stmt.setString(2, query.getMatchedIntent() != null ? query.getMatchedIntent().name() : null);
                stmt.setString(3, query.getResolvedTo());
                stmt.setBoolean(4, query.isSuccessful());

                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        query.setId(rs.getLong(1));
                    }
                }

            } catch (SQLException e) {
                // Table might not exist yet - that's OK
                LOG.debugf("Could not log NL query (table may not exist): %s", e.getMessage());
            }

        } catch (Exception e) {
            LOG.debugf(e, "Error logging query");
        }
    }

    /**
     * Pattern definition for matching queries.
     */
    private static class QueryPattern {
        final String patternString;
        final Pattern pattern;
        final NaturalLanguageQuery.Intent intent;
        final int priority;

        QueryPattern(String patternString, NaturalLanguageQuery.Intent intent, int priority) {
            this.patternString = patternString;
            this.pattern = Pattern.compile(patternString);
            this.intent = intent;
            this.priority = priority;
        }
    }
}
