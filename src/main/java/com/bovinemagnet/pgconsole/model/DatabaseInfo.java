package com.bovinemagnet.pgconsole.model;

/**
 * Represents database information for the About page.
 * <p>
 * This model encapsulates essential metadata about the PostgreSQL
 * instance, including version information, encoding, server uptime,
 * and extension availability.
 * </p>
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class DatabaseInfo {
    private String postgresVersion;
    private String currentDatabase;
    private String currentUser;
    private String serverEncoding;
    private String serverStartTime;
    private boolean pgStatStatementsEnabled;
    private String pgStatStatementsVersion;

    /**
     * Returns the PostgreSQL version string.
     *
     * @return the PostgreSQL version
     */
    public String getPostgresVersion() {
        return postgresVersion;
    }

    /**
     * Sets the PostgreSQL version string.
     *
     * @param postgresVersion the PostgreSQL version
     */
    public void setPostgresVersion(String postgresVersion) {
        this.postgresVersion = postgresVersion;
    }

    /**
     * Returns the name of the current database.
     *
     * @return the database name
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
     *
     * @return the user name
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
     *
     * @return the server encoding (e.g., UTF8)
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
     *
     * @return the server start time
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
     *
     * @return true if pg_stat_statements is available, false otherwise
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
     *
     * @return the extension version, or null if not installed
     */
    public String getPgStatStatementsVersion() {
        return pgStatStatementsVersion;
    }

    /**
     * Sets the version of the pg_stat_statements extension.
     *
     * @param pgStatStatementsVersion the extension version
     */
    public void setPgStatStatementsVersion(String pgStatStatementsVersion) {
        this.pgStatStatementsVersion = pgStatStatementsVersion;
    }
}
