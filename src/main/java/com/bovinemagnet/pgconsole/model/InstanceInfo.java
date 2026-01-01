package com.bovinemagnet.pgconsole.model;

/**
 * Represents a PostgreSQL instance for UI display.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class InstanceInfo {

    private String name;
    private String displayName;
    private boolean connected;
    private String version;
    private String currentDatabase;

    public InstanceInfo() {
    }

    public InstanceInfo(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getCurrentDatabase() {
        return currentDatabase;
    }

    public void setCurrentDatabase(String currentDatabase) {
        this.currentDatabase = currentDatabase;
    }

    /**
     * Gets a short status string for display.
     *
     * @return status string
     */
    public String getStatus() {
        if (!connected) {
            return "Disconnected";
        }
        if (version != null && currentDatabase != null) {
            return "v" + version + " - " + currentDatabase;
        }
        return "Connected";
    }
}
