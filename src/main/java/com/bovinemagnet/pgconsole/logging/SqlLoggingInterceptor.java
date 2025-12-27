package com.bovinemagnet.pgconsole.logging;

import com.bovinemagnet.pgconsole.config.LoggingConfig;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.jboss.logging.Logger;

import java.lang.reflect.Method;

/**
 * CDI interceptor for logging SQL query execution with timing.
 * <p>
 * Automatically logs SQL queries, their execution time, and row counts.
 * Warns when queries exceed the configured slow threshold.
 * <p>
 * Use with @LoggedSql annotation on methods to enable SQL logging.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@LoggedSql
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class SqlLoggingInterceptor {

    private static final Logger LOG = Logger.getLogger(SqlLoggingInterceptor.class);

    @Inject
    LoggingConfig loggingConfig;

    @Inject
    StructuredLogger structuredLogger;

    @Inject
    LogRedactionService redactionService;

    /**
     * Intercepts method calls to log SQL execution timing.
     *
     * @param context invocation context
     * @return method result
     * @throws Exception if method throws
     */
    @AroundInvoke
    public Object logSqlExecution(InvocationContext context) throws Exception {
        if (!loggingConfig.sqlEnabled()) {
            return context.proceed();
        }

        Method method = context.getMethod();
        String methodName = method.getDeclaringClass().getSimpleName() + "." + method.getName();

        // Try to extract SQL from parameters
        String sql = extractSql(context.getParameters());

        long startTime = System.currentTimeMillis();
        int rowCount = 0;
        boolean success = true;

        try {
            Object result = context.proceed();

            // Try to get row count from result
            rowCount = extractRowCount(result);

            return result;
        } catch (Exception e) {
            success = false;
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            if (sql != null) {
                structuredLogger.logQuery(sql, duration, rowCount);
            } else {
                // Log method execution without SQL
                logMethodExecution(methodName, duration, rowCount, success);
            }
        }
    }

    /**
     * Extracts SQL string from method parameters.
     */
    private String extractSql(Object[] parameters) {
        if (parameters == null) {
            return null;
        }

        for (Object param : parameters) {
            if (param instanceof String) {
                String str = (String) param;
                // Check if it looks like SQL
                if (looksLikeSql(str)) {
                    return str;
                }
            }
        }

        return null;
    }

    /**
     * Checks if a string looks like a SQL statement.
     */
    private boolean looksLikeSql(String str) {
        if (str == null || str.isBlank()) {
            return false;
        }

        String upper = str.trim().toUpperCase();
        return upper.startsWith("SELECT") ||
               upper.startsWith("INSERT") ||
               upper.startsWith("UPDATE") ||
               upper.startsWith("DELETE") ||
               upper.startsWith("WITH") ||
               upper.startsWith("CREATE") ||
               upper.startsWith("ALTER") ||
               upper.startsWith("DROP") ||
               upper.startsWith("EXPLAIN");
    }

    /**
     * Extracts row count from result object.
     */
    private int extractRowCount(Object result) {
        if (result == null) {
            return 0;
        }

        if (result instanceof Integer) {
            return (Integer) result;
        }

        if (result instanceof Long) {
            return ((Long) result).intValue();
        }

        if (result instanceof java.util.List) {
            return ((java.util.List<?>) result).size();
        }

        return -1; // Unknown
    }

    /**
     * Logs method execution without SQL details.
     */
    private void logMethodExecution(String methodName, long duration, int rowCount, boolean success) {
        if (duration > loggingConfig.sqlSlowThresholdMs()) {
            LOG.warnf("Slow database operation: %s took %dms (threshold: %dms), rows: %d",
                methodName, duration, loggingConfig.sqlSlowThresholdMs(), rowCount);
        } else if (LOG.isDebugEnabled()) {
            LOG.debugf("Database operation: %s completed in %dms, rows: %d",
                methodName, duration, rowCount);
        }
    }
}
