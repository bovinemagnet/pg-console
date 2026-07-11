package com.bovinemagnet.pgconsole.util;

/**
 * Allowlist of non-destructive SQL maintenance verbs that a runbook step may
 * auto-execute.
 * <p>
 * Only statements whose leading verb is VACUUM, ANALYZE or REINDEX are
 * permitted, and only when they contain no statement separator. This prevents
 * an auto-executable runbook row from running arbitrary DDL/DML such as DROP,
 * TRUNCATE or {@code pg_terminate_backend}.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public final class SqlMaintenanceVerbs {

    private SqlMaintenanceVerbs() {
    }

    private static final java.util.regex.Pattern ALLOWED =
        java.util.regex.Pattern.compile("(?is)^\\s*(VACUUM|ANALYZE|REINDEX)\\b.*");

    /**
     * Determines whether an auto-executable SQL statement is a permitted
     * maintenance operation.
     *
     * @param sql the SQL text (may be null)
     * @return true only when the statement begins with an allowed maintenance
     *         verb and contains no ';' statement separator
     */
    public static boolean isAllowed(String sql) {
        if (sql == null || sql.isBlank()) {
            return false;
        }
        if (sql.indexOf(';') >= 0) {
            return false;
        }
        return ALLOWED.matcher(sql).matches();
    }
}
