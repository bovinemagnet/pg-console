package com.bovinemagnet.pgconsole.model;

/**
 * Database information for the About page.
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

    public String getPostgresVersion() {
        return postgresVersion;
    }

    public void setPostgresVersion(String postgresVersion) {
        this.postgresVersion = postgresVersion;
    }

    public String getCurrentDatabase() {
        return currentDatabase;
    }

    public void setCurrentDatabase(String currentDatabase) {
        this.currentDatabase = currentDatabase;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(String currentUser) {
        this.currentUser = currentUser;
    }

    public String getServerEncoding() {
        return serverEncoding;
    }

    public void setServerEncoding(String serverEncoding) {
        this.serverEncoding = serverEncoding;
    }

    public String getServerStartTime() {
        return serverStartTime;
    }

    public void setServerStartTime(String serverStartTime) {
        this.serverStartTime = serverStartTime;
    }

    public boolean isPgStatStatementsEnabled() {
        return pgStatStatementsEnabled;
    }

    public void setPgStatStatementsEnabled(boolean pgStatStatementsEnabled) {
        this.pgStatStatementsEnabled = pgStatStatementsEnabled;
    }

    public String getPgStatStatementsVersion() {
        return pgStatStatementsVersion;
    }

    public void setPgStatStatementsVersion(String pgStatStatementsVersion) {
        this.pgStatStatementsVersion = pgStatStatementsVersion;
    }
}
