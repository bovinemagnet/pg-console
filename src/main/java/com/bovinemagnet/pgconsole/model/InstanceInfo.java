package com.bovinemagnet.pgconsole.model;

/**
 * Represents a PostgreSQL database instance for display in the console UI.
 * <p>
 * This model class encapsulates the essential information about a PostgreSQL instance,
 * including its connection state, version details, and currently selected database.
 * It is primarily used for instance selection and status display in the navigation bar
 * and instance switcher components.
 * <p>
 * The class provides a computed status string via {@link #getStatus()} that summarises
 * the instance's current state for quick display.
 *
 * @author Paul Snow
 * @version 0.0.0
 * @since 0.0.0
 * @see com.bovinemagnet.pgconsole.service.PostgresService
 */
public class InstanceInfo {

    /**
     * The unique identifier for this instance (e.g., "production", "staging").
     * This name is used internally for configuration lookups and instance switching.
     */
    private String name;

    /**
     * The human-readable display name for this instance (e.g., "Production DB", "Staging Environment").
     * This is shown in the UI instead of the internal name.
     */
    private String displayName;

    /**
     * Indicates whether the application is currently connected to this instance.
     * When false, the instance is configured but not actively connected.
     */
    private boolean connected;

    /**
     * The PostgreSQL server version string (e.g., "14.5", "15.2").
     * Populated only when connected to the instance.
     */
    private String version;

    /**
     * The name of the currently selected database within this instance.
     * Populated only when connected to the instance.
     */
    private String currentDatabase;

    /**
     * Constructs an empty InstanceInfo.
     * <p>
     * This no-argument constructor is primarily used by serialisation frameworks
     * and should be followed by setter method calls to populate the instance.
     */
    public InstanceInfo() {
    }

    /**
     * Constructs an InstanceInfo with the specified name and display name.
     * <p>
     * The instance is created in a disconnected state with no version or database information.
     * These properties can be populated later using the appropriate setter methods once
     * a connection is established.
     *
     * @param name the unique identifier for this instance (e.g., "production")
     * @param displayName the human-readable name for display in the UI (e.g., "Production DB")
     */
    public InstanceInfo(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
    }

    /**
     * Returns the unique identifier for this instance.
     * <p>
     * This internal name is used for configuration lookups and instance switching
     * in the application. It should be a stable identifier that doesn't change
     * over the instance's lifecycle.
     *
     * @return the instance identifier, may be null if not yet set
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the unique identifier for this instance.
     *
     * @param name the instance identifier (e.g., "production", "staging")
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the human-readable display name for this instance.
     * <p>
     * This is the name shown to users in the UI, typically more descriptive
     * than the internal identifier.
     *
     * @return the display name, may be null if not yet set
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets the human-readable display name for this instance.
     *
     * @param displayName the display name (e.g., "Production Database", "Staging Environment")
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Checks whether the application is currently connected to this instance.
     *
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Sets the connection state for this instance.
     * <p>
     * When setting this to true, ensure that {@link #setVersion(String)} and
     * {@link #setCurrentDatabase(String)} are also called to provide complete
     * connection information.
     *
     * @param connected true if connected, false if disconnected
     */
    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    /**
     * Returns the PostgreSQL server version string.
     * <p>
     * This is typically in the format "major.minor" (e.g., "14.5", "15.2")
     * and is only available when connected to the instance.
     *
     * @return the version string, or null if not connected or version unknown
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the PostgreSQL server version string.
     *
     * @param version the version string (e.g., "14.5", "15.2"), or null if unknown
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Returns the name of the currently selected database within this instance.
     * <p>
     * PostgreSQL instances (clusters) can contain multiple databases. This
     * property indicates which database the application is currently connected to
     * for querying system statistics and metadata.
     *
     * @return the database name, or null if not connected
     */
    public String getCurrentDatabase() {
        return currentDatabase;
    }

    /**
     * Sets the name of the currently selected database.
     *
     * @param currentDatabase the database name (e.g., "postgres", "myapp_production"),
     *                        or null if disconnected
     */
    public void setCurrentDatabase(String currentDatabase) {
        this.currentDatabase = currentDatabase;
    }

    /**
     * Computes a concise status string summarising the instance's current state.
     * <p>
     * The status string varies based on the connection state and available information:
     * <ul>
     *   <li>"Disconnected" - when {@link #isConnected()} returns false</li>
     *   <li>"v{version} - {database}" - when connected with version and database information
     *       (e.g., "v14.5 - postgres")</li>
     *   <li>"Connected" - when connected but version or database information is unavailable</li>
     * </ul>
     * <p>
     * This method is primarily used for quick status display in the UI navigation
     * and instance switcher components.
     *
     * @return a non-null status string suitable for UI display
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
