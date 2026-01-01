package com.bovinemagnet.pgconsole.model;

/**
 * Represents a replication slot from pg_replication_slots.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class ReplicationSlot {

    public enum SlotType {
        PHYSICAL,
        LOGICAL
    }

    private String slotName;
    private String plugin;
    private SlotType slotType;
    private String database;
    private boolean temporary;
    private boolean active;
    private int activePid;
    private String xmin;
    private String catalogXmin;
    private String restartLsn;
    private String confirmedFlushLsn;
    private long walRetainedBytes;
    private boolean safeToDelete;

    public ReplicationSlot() {
    }

    public String getSlotName() {
        return slotName;
    }

    public void setSlotName(String slotName) {
        this.slotName = slotName;
    }

    public String getPlugin() {
        return plugin;
    }

    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }

    public SlotType getSlotType() {
        return slotType;
    }

    public void setSlotType(SlotType slotType) {
        this.slotType = slotType;
    }

    public void setSlotTypeFromString(String typeStr) {
        if ("logical".equalsIgnoreCase(typeStr)) {
            this.slotType = SlotType.LOGICAL;
        } else {
            this.slotType = SlotType.PHYSICAL;
        }
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public boolean isTemporary() {
        return temporary;
    }

    public void setTemporary(boolean temporary) {
        this.temporary = temporary;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getActivePid() {
        return activePid;
    }

    public void setActivePid(int activePid) {
        this.activePid = activePid;
    }

    public String getXmin() {
        return xmin;
    }

    public void setXmin(String xmin) {
        this.xmin = xmin;
    }

    public String getCatalogXmin() {
        return catalogXmin;
    }

    public void setCatalogXmin(String catalogXmin) {
        this.catalogXmin = catalogXmin;
    }

    public String getRestartLsn() {
        return restartLsn;
    }

    public void setRestartLsn(String restartLsn) {
        this.restartLsn = restartLsn;
    }

    public String getConfirmedFlushLsn() {
        return confirmedFlushLsn;
    }

    public void setConfirmedFlushLsn(String confirmedFlushLsn) {
        this.confirmedFlushLsn = confirmedFlushLsn;
    }

    public long getWalRetainedBytes() {
        return walRetainedBytes;
    }

    public void setWalRetainedBytes(long walRetainedBytes) {
        this.walRetainedBytes = walRetainedBytes;
    }

    public boolean isSafeToDelete() {
        return safeToDelete;
    }

    public void setSafeToDelete(boolean safeToDelete) {
        this.safeToDelete = safeToDelete;
    }

    public String getSlotTypeCssClass() {
        return slotType == SlotType.LOGICAL ? "bg-info" : "bg-secondary";
    }

    public String getActiveCssClass() {
        return active ? "bg-success" : "bg-warning text-dark";
    }

    public String getActiveDisplay() {
        return active ? "Active" : "Inactive";
    }

    public String getWalRetainedFormatted() {
        return formatBytes(walRetainedBytes);
    }

    public boolean isRetainingExcessiveWal() {
        // Flag if retaining more than 1GB
        return walRetainedBytes > 1024L * 1024 * 1024;
    }

    public String getRetainedWalCssClass() {
        if (walRetainedBytes > 10L * 1024 * 1024 * 1024) {
            return "text-danger"; // > 10GB
        } else if (walRetainedBytes > 1024L * 1024 * 1024) {
            return "text-warning"; // > 1GB
        }
        return "";
    }

    private String formatBytes(long bytes) {
        if (bytes < 0) return "N/A";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
