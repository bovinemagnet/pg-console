package com.bovinemagnet.pgconsole.model;

/**
 * Represents a PostgreSQL replication slot from the pg_replication_slots system view.
 * <p>
 * Replication slots provide a mechanism to ensure that the WAL (Write-Ahead Log) segments
 * needed by a standby server or logical replication subscriber are retained until they
 * have been received. This prevents the primary server from removing WAL files that are
 * still needed for replication, which would otherwise cause replication to fail.
 * <p>
 * This model tracks both physical replication slots (for streaming replication to standby
 * servers) and logical replication slots (for logical decoding and change data capture).
 * It includes metrics such as WAL retention, activity status, and LSN (Log Sequence Number)
 * positions to help monitor replication health and identify potential issues.
 *
 * @author Paul Snow
 * @since 0.0.0
 * @see <a href="https://www.postgresql.org/docs/current/view-pg-replication-slots.html">PostgreSQL pg_replication_slots documentation</a>
 */
public class ReplicationSlot {

    /**
     * Enumeration of replication slot types supported by PostgreSQL.
     * <p>
     * Physical slots are used for streaming replication to standby servers,
     * whilst logical slots are used for logical replication and change data capture.
     */
    public enum SlotType {
        /**
         * Physical replication slot for streaming replication to standby servers.
         * Physical slots replicate all changes to the database cluster.
         */
        PHYSICAL,

        /**
         * Logical replication slot for logical decoding and selective replication.
         * Logical slots allow subscribers to receive specific changes based on publications.
         */
        LOGICAL
    }

    /**
     * The unique name of the replication slot.
     */
    private String slotName;

    /**
     * The name of the output plugin used by this logical replication slot.
     * Null for physical replication slots.
     */
    private String plugin;

    /**
     * The type of replication slot (physical or logical).
     */
    private SlotType slotType;

    /**
     * The name of the database this slot is associated with.
     * Only applicable for logical replication slots; null for physical slots.
     */
    private String database;

    /**
     * Indicates whether this is a temporary replication slot.
     * Temporary slots are automatically dropped when the session ends.
     */
    private boolean temporary;

    /**
     * Indicates whether this replication slot is currently active (has an active connection).
     */
    private boolean active;

    /**
     * The process ID of the session using this slot, or 0 if the slot is inactive.
     */
    private int activePid;

    /**
     * The oldest transaction ID that this slot needs the database to retain.
     * Prevents VACUUM from removing rows that might be needed by the slot.
     */
    private String xmin;

    /**
     * The oldest transaction ID affecting the system catalogs that this slot needs retained.
     * Only relevant for logical replication slots.
     */
    private String catalogXmin;

    /**
     * The LSN (Log Sequence Number) from which replication will restart.
     * This is the position in the WAL where the slot's consumer will resume reading.
     */
    private String restartLsn;

    /**
     * The LSN up to which the consumer has confirmed flushing data.
     * Only applicable for logical replication slots; indicates the consumer's progress.
     */
    private String confirmedFlushLsn;

    /**
     * The number of bytes of WAL being retained for this replication slot.
     * Large values may indicate a lagging or stuck replication consumer.
     */
    private long walRetainedBytes;

    /**
     * Indicates whether this slot can be safely deleted without data loss.
     * Derived from activity status and other metrics.
     */
    private boolean safeToDelete;

    /**
     * Constructs a new ReplicationSlot with default values.
     */
    public ReplicationSlot() {
    }

    /**
     * Returns the unique name of this replication slot.
     *
     * @return the slot name, or null if not set
     */
    public String getSlotName() {
        return slotName;
    }

    /**
     * Sets the unique name of this replication slot.
     *
     * @param slotName the slot name to set
     */
    public void setSlotName(String slotName) {
        this.slotName = slotName;
    }

    /**
     * Returns the name of the output plugin used by this logical replication slot.
     *
     * @return the plugin name, or null for physical replication slots
     */
    public String getPlugin() {
        return plugin;
    }

    /**
     * Sets the name of the output plugin for this logical replication slot.
     *
     * @param plugin the plugin name to set
     */
    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }

    /**
     * Returns the type of this replication slot.
     *
     * @return the slot type (PHYSICAL or LOGICAL)
     */
    public SlotType getSlotType() {
        return slotType;
    }

    /**
     * Sets the type of this replication slot.
     *
     * @param slotType the slot type to set
     */
    public void setSlotType(SlotType slotType) {
        this.slotType = slotType;
    }

    /**
     * Sets the slot type from a string representation.
     * <p>
     * Converts the PostgreSQL slot type string to the corresponding enum value.
     * The string "logical" (case-insensitive) maps to {@link SlotType#LOGICAL};
     * all other values default to {@link SlotType#PHYSICAL}.
     *
     * @param typeStr the slot type string from PostgreSQL ("logical" or "physical")
     */
    public void setSlotTypeFromString(String typeStr) {
        if ("logical".equalsIgnoreCase(typeStr)) {
            this.slotType = SlotType.LOGICAL;
        } else {
            this.slotType = SlotType.PHYSICAL;
        }
    }

    /**
     * Returns the name of the database this slot is associated with.
     *
     * @return the database name, or null for physical replication slots
     */
    public String getDatabase() {
        return database;
    }

    /**
     * Sets the name of the database this slot is associated with.
     *
     * @param database the database name to set
     */
    public void setDatabase(String database) {
        this.database = database;
    }

    /**
     * Indicates whether this is a temporary replication slot.
     * <p>
     * Temporary slots are automatically dropped when the session ends.
     *
     * @return true if this is a temporary slot, false otherwise
     */
    public boolean isTemporary() {
        return temporary;
    }

    /**
     * Sets whether this is a temporary replication slot.
     *
     * @param temporary true if this is a temporary slot, false otherwise
     */
    public void setTemporary(boolean temporary) {
        this.temporary = temporary;
    }

    /**
     * Indicates whether this replication slot is currently active.
     * <p>
     * An active slot has an established connection from a replication consumer.
     *
     * @return true if the slot is active, false if inactive
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Sets whether this replication slot is currently active.
     *
     * @param active true if the slot is active, false if inactive
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Returns the process ID of the session using this slot.
     *
     * @return the process ID, or 0 if the slot is inactive
     */
    public int getActivePid() {
        return activePid;
    }

    /**
     * Sets the process ID of the session using this slot.
     *
     * @param activePid the process ID, or 0 if the slot is inactive
     */
    public void setActivePid(int activePid) {
        this.activePid = activePid;
    }

    /**
     * Returns the oldest transaction ID that this slot needs the database to retain.
     * <p>
     * This prevents VACUUM from removing rows that might still be needed by the
     * replication slot's consumer.
     *
     * @return the xmin transaction ID as a string, or null if not applicable
     */
    public String getXmin() {
        return xmin;
    }

    /**
     * Sets the oldest transaction ID that this slot needs the database to retain.
     *
     * @param xmin the xmin transaction ID to set
     */
    public void setXmin(String xmin) {
        this.xmin = xmin;
    }

    /**
     * Returns the oldest transaction ID affecting system catalogs that this slot needs retained.
     * <p>
     * Only relevant for logical replication slots, as they need to track catalog changes
     * for schema evolution.
     *
     * @return the catalog_xmin transaction ID as a string, or null if not applicable
     */
    public String getCatalogXmin() {
        return catalogXmin;
    }

    /**
     * Sets the oldest transaction ID affecting system catalogs that this slot needs retained.
     *
     * @param catalogXmin the catalog_xmin transaction ID to set
     */
    public void setCatalogXmin(String catalogXmin) {
        this.catalogXmin = catalogXmin;
    }

    /**
     * Returns the LSN from which replication will restart.
     * <p>
     * This is the position in the WAL where the slot's consumer will resume reading
     * after reconnecting.
     *
     * @return the restart LSN as a string, or null if not set
     */
    public String getRestartLsn() {
        return restartLsn;
    }

    /**
     * Sets the LSN from which replication will restart.
     *
     * @param restartLsn the restart LSN to set
     */
    public void setRestartLsn(String restartLsn) {
        this.restartLsn = restartLsn;
    }

    /**
     * Returns the LSN up to which the consumer has confirmed flushing data.
     * <p>
     * Only applicable for logical replication slots. This indicates the consumer's
     * progress and confirms which changes have been safely persisted.
     *
     * @return the confirmed flush LSN as a string, or null for physical slots
     */
    public String getConfirmedFlushLsn() {
        return confirmedFlushLsn;
    }

    /**
     * Sets the LSN up to which the consumer has confirmed flushing data.
     *
     * @param confirmedFlushLsn the confirmed flush LSN to set
     */
    public void setConfirmedFlushLsn(String confirmedFlushLsn) {
        this.confirmedFlushLsn = confirmedFlushLsn;
    }

    /**
     * Returns the number of bytes of WAL being retained for this replication slot.
     * <p>
     * Large values may indicate a lagging or stuck replication consumer, which could
     * lead to disk space issues on the primary server.
     *
     * @return the number of WAL bytes retained
     */
    public long getWalRetainedBytes() {
        return walRetainedBytes;
    }

    /**
     * Sets the number of bytes of WAL being retained for this replication slot.
     *
     * @param walRetainedBytes the number of WAL bytes retained
     */
    public void setWalRetainedBytes(long walRetainedBytes) {
        this.walRetainedBytes = walRetainedBytes;
    }

    /**
     * Indicates whether this slot can be safely deleted without data loss.
     * <p>
     * This is derived from activity status and other metrics to help administrators
     * identify unused or safe-to-remove slots.
     *
     * @return true if the slot can be safely deleted, false otherwise
     */
    public boolean isSafeToDelete() {
        return safeToDelete;
    }

    /**
     * Sets whether this slot can be safely deleted without data loss.
     *
     * @param safeToDelete true if the slot can be safely deleted, false otherwise
     */
    public void setSafeToDelete(boolean safeToDelete) {
        this.safeToDelete = safeToDelete;
    }

    /**
     * Returns the appropriate Bootstrap CSS class for displaying the slot type.
     * <p>
     * Used in UI templates to visually distinguish between logical and physical slots.
     * Logical slots receive the "bg-info" class (blue background), whilst physical
     * slots receive the "bg-secondary" class (grey background).
     *
     * @return "bg-info" for logical slots, "bg-secondary" for physical slots
     */
    public String getSlotTypeCssClass() {
        return slotType == SlotType.LOGICAL ? "bg-info" : "bg-secondary";
    }

    /**
     * Returns the appropriate Bootstrap CSS class for displaying the slot's active status.
     * <p>
     * Active slots receive a green background ("bg-success"), whilst inactive slots
     * receive a yellow background with dark text ("bg-warning text-dark") to indicate
     * a potential issue.
     *
     * @return "bg-success" if active, "bg-warning text-dark" if inactive
     */
    public String getActiveCssClass() {
        return active ? "bg-success" : "bg-warning text-dark";
    }

    /**
     * Returns a human-readable display string for the slot's active status.
     *
     * @return "Active" if the slot is active, "Inactive" otherwise
     */
    public String getActiveDisplay() {
        return active ? "Active" : "Inactive";
    }

    /**
     * Returns a human-readable formatted string representing the WAL retained bytes.
     * <p>
     * Formats the byte count with appropriate units (B, KB, MB, GB) for display
     * in the user interface.
     *
     * @return formatted string like "1.5 GB" or "256.0 MB"
     * @see #formatBytes(long)
     */
    public String getWalRetainedFormatted() {
        return formatBytes(walRetainedBytes);
    }

    /**
     * Indicates whether this slot is retaining an excessive amount of WAL data.
     * <p>
     * Returns true if the slot is retaining more than 1 GB of WAL, which may
     * indicate a lagging or stuck replication consumer that requires attention.
     *
     * @return true if retaining more than 1 GB of WAL, false otherwise
     */
    public boolean isRetainingExcessiveWal() {
        // Flag if retaining more than 1GB
        return walRetainedBytes > 1024L * 1024 * 1024;
    }

    /**
     * Returns the appropriate CSS class for displaying the retained WAL amount.
     * <p>
     * Applies colour coding based on retention thresholds:
     * <ul>
     * <li>Over 10 GB: "text-danger" (red) - critical level</li>
     * <li>Over 1 GB: "text-warning" (yellow) - warning level</li>
     * <li>Under 1 GB: empty string (default colour) - normal level</li>
     * </ul>
     *
     * @return Bootstrap CSS class for colour coding, or empty string for normal levels
     */
    public String getRetainedWalCssClass() {
        if (walRetainedBytes > 10L * 1024 * 1024 * 1024) {
            return "text-danger"; // > 10GB
        } else if (walRetainedBytes > 1024L * 1024 * 1024) {
            return "text-warning"; // > 1GB
        }
        return "";
    }

    /**
     * Formats a byte count into a human-readable string with appropriate units.
     * <p>
     * Converts bytes to the most appropriate unit (B, KB, MB, GB) with one decimal
     * place precision for values over 1 KB.
     *
     * @param bytes the number of bytes to format
     * @return formatted string such as "1.5 GB", "256.0 MB", "42 B", or "N/A" for negative values
     */
    private String formatBytes(long bytes) {
        if (bytes < 0) return "N/A";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
