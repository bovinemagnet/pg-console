package com.bovinemagnet.pgconsole.model;

/**
 * Represents database information for the About page.
 * <p>
 * This model encapsulates essential metadata about the PostgreSQL
 * instance, including version information, encoding, server uptime,
 * and extension availability. The data is retrieved from PostgreSQL
 * system functions such as {@code version()}, {@code current_database()},
 * {@code current_user}, and {@code pg_stat_get_db_conflict_all()}.
 * </p>
 * <p>
 * This model is populated by {@link com.bovinemagnet.pgconsole.service.PostgresService}
 * and rendered on the {@code /about} dashboard route to provide administrators
 * with visibility into the PostgreSQL server configuration and status.
 * </p>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see com.bovinemagnet.pgconsole.service.PostgresService#getDatabaseInfo(String)
 * @see com.bovinemagnet.pgconsole.resource.DashboardResource
 */
public class DatabaseInfo {
    /** The PostgreSQL server version string (e.g., "PostgreSQL 15.4 on x86_64-pc-linux-gnu"). */
    private String postgresVersion;

    /** The name of the database currently connected to. */
    private String currentDatabase;

    /** The name of the current PostgreSQL user/role. */
    private String currentUser;

    /** The server character encoding (e.g., "UTF8"). */
    private String serverEncoding;

    /** The timestamp when the PostgreSQL server was last started, formatted as a string. */
    private String serverStartTime;

    /** Flag indicating whether the pg_stat_statements extension is installed and available. */
    private boolean pgStatStatementsEnabled;

    /** The version of the pg_stat_statements extension, or null if not installed. */
    private String pgStatStatementsVersion;

    /**
     * Returns the PostgreSQL version string.
     * <p>
     * This typically includes the major and minor version, platform,
     * and compiler information (e.g., "PostgreSQL 15.4 on x86_64-pc-linux-gnu").
     * </p>
     *
     * @return the PostgreSQL version string, or null if not yet initialised
     */
    public String getPostgresVersion() {
        return postgresVersion;
    }

    /**
     * Sets the PostgreSQL version string.
     * <p>
     * This value is typically obtained from the PostgreSQL {@code version()} function.
     * </p>
     *
     * @param postgresVersion the PostgreSQL version string
     */
    public void setPostgresVersion(String postgresVersion) {
        this.postgresVersion = postgresVersion;
    }

    /**
     * Returns the name of the current database.
     * <p>
     * This is the database to which the monitoring connection is established,
     * typically obtained from the PostgreSQL {@code current_database()} function.
     * </p>
     *
     * @return the database name, or null if not yet initialised
     */
    public String getCurrentDatabase() {
        return currentDatabase;
    }

    /**
     * Sets the name of the current database.
     *
     * @param currentDatabase the database name
     */
    public void setCurrentDatabase(String currentDatabase) {
        this.currentDatabase = currentDatabase;
    }

    /**
     * Returns the current PostgreSQL user.
     * <p>
     * This is the database role/user under which the monitoring connection
     * is authenticated, typically obtained from the PostgreSQL {@code current_user}
     * session variable.
     * </p>
     *
     * @return the user name, or null if not yet initialised
     */
    public String getCurrentUser() {
        return currentUser;
    }

    /**
     * Sets the current PostgreSQL user.
     *
     * @param currentUser the user name
     */
    public void setCurrentUser(String currentUser) {
        this.currentUser = currentUser;
    }

    /**
     * Returns the server character encoding.
     * <p>
     * This indicates the character set used by the PostgreSQL server for
     * storing and interpreting text data. Common values include "UTF8",
     * "SQL_ASCII", "LATIN1", etc.
     * </p>
     *
     * @return the server encoding (e.g., "UTF8"), or null if not yet initialised
     */
    public String getServerEncoding() {
        return serverEncoding;
    }

    /**
     * Sets the server character encoding.
     *
     * @param serverEncoding the server encoding
     */
    public void setServerEncoding(String serverEncoding) {
        this.serverEncoding = serverEncoding;
    }

    /**
     * Returns the server start time as a formatted string.
     * <p>
     * This represents when the PostgreSQL server process was last started,
     * obtained from {@code pg_postmaster_start_time()}. The format is typically
     * an ISO-8601 timestamp string.
     * </p>
     *
     * @return the server start time as a formatted timestamp, or null if not yet initialised
     */
    public String getServerStartTime() {
        return serverStartTime;
    }

    /**
     * Sets the server start time as a formatted string.
     *
     * @param serverStartTime the server start time
     */
    public void setServerStartTime(String serverStartTime) {
        this.serverStartTime = serverStartTime;
    }

    /**
     * Checks whether the pg_stat_statements extension is enabled.
     * <p>
     * The {@code pg_stat_statements} extension is essential for query performance
     * monitoring in pg-console. If this returns false, the slow queries dashboard
     * and related features will not be available.
     * </p>
     *
     * @return true if pg_stat_statements is installed and available, false otherwise
     * @see <a href="https://www.postgresql.org/docs/current/pgstatstatements.html">pg_stat_statements documentation</a>
     */
    public boolean isPgStatStatementsEnabled() {
        return pgStatStatementsEnabled;
    }

    /**
     * Sets whether the pg_stat_statements extension is enabled.
     *
     * @param pgStatStatementsEnabled true if enabled, false otherwise
     */
    public void setPgStatStatementsEnabled(boolean pgStatStatementsEnabled) {
        this.pgStatStatementsEnabled = pgStatStatementsEnabled;
    }

    /**
     * Returns the version of the pg_stat_statements extension.
     * <p>
     * This is queried from the {@code pg_available_extensions} system view.
     * Different versions may support different features and columns.
     * </p>
     *
     * @return the extension version (e.g., "1.10"), or null if the extension is not installed
     */
    public String getPgStatStatementsVersion() {
        return pgStatStatementsVersion;
    }

    /**
     * Sets the version of the pg_stat_statements extension.
     *
     * @param pgStatStatementsVersion the extension version, or null if not installed
     */
    public void setPgStatStatementsVersion(String pgStatStatementsVersion) {
        this.pgStatStatementsVersion = pgStatStatementsVersion;
    }
}
