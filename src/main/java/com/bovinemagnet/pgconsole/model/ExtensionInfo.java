package com.bovinemagnet.pgconsole.model;

/**
 * Represents a PostgreSQL extension from the system catalogs
 * {@code pg_extension} and {@code pg_available_extensions}.
 * <p>
 * This class captures information about both installed and available extensions,
 * including version information and upgrade availability.
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see <a href="https://www.postgresql.org/docs/current/view-pg-available-extensions.html">pg_available_extensions Documentation</a>
 */
public class ExtensionInfo {

    /** Name of the extension */
    private String name;

    /** Default version available for installation */
    private String defaultVersion;

    /** Currently installed version (null if not installed) */
    private String installedVersion;

    /** Description of the extension */
    private String comment;

    /** Schema where the extension is installed (null if not installed) */
    private String schema;

    /** Whether the extension is relocatable */
    private boolean relocatable;

    /** Whether the extension is installed */
    private boolean installed;

    /** Whether an upgrade is available */
    private boolean upgradeAvailable;

    /**
     * Constructs a new ExtensionInfo instance.
     */
    public ExtensionInfo() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDefaultVersion() {
        return defaultVersion;
    }

    public void setDefaultVersion(String defaultVersion) {
        this.defaultVersion = defaultVersion;
    }

    public String getInstalledVersion() {
        return installedVersion;
    }

    public void setInstalledVersion(String installedVersion) {
        this.installedVersion = installedVersion;
        this.installed = installedVersion != null && !installedVersion.isEmpty();
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public boolean isRelocatable() {
        return relocatable;
    }

    public void setRelocatable(boolean relocatable) {
        this.relocatable = relocatable;
    }

    public boolean isInstalled() {
        return installed;
    }

    public void setInstalled(boolean installed) {
        this.installed = installed;
    }

    public boolean isUpgradeAvailable() {
        return upgradeAvailable;
    }

    public void setUpgradeAvailable(boolean upgradeAvailable) {
        this.upgradeAvailable = upgradeAvailable;
    }

    /**
     * Checks if an upgrade is available by comparing versions.
     */
    public void checkUpgradeAvailable() {
        if (!installed || installedVersion == null || defaultVersion == null) {
            this.upgradeAvailable = false;
            return;
        }
        // Simple version comparison - versions are different and default is "newer"
        this.upgradeAvailable = !installedVersion.equals(defaultVersion);
    }

    /**
     * Returns a truncated description for display.
     *
     * @param maxLength maximum length before truncation
     * @return truncated description
     */
    public String getCommentTruncated(int maxLength) {
        if (comment == null) return "";
        if (comment.length() <= maxLength) return comment;
        return comment.substring(0, maxLength - 3) + "...";
    }

    /**
     * Returns Bootstrap CSS class for installed status badge.
     *
     * @return Bootstrap background class
     */
    public String getInstalledCssClass() {
        return installed ? "bg-success" : "bg-secondary";
    }

    /**
     * Returns the installed status display text.
     *
     * @return status text
     */
    public String getInstalledDisplay() {
        return installed ? "Installed" : "Available";
    }

    /**
     * Returns Bootstrap CSS class for upgrade badge.
     *
     * @return Bootstrap background class
     */
    public String getUpgradeCssClass() {
        if (!installed) return "bg-secondary";
        return upgradeAvailable ? "bg-warning text-dark" : "bg-success";
    }

    /**
     * Returns the version display text.
     *
     * @return version display text
     */
    public String getVersionDisplay() {
        if (!installed) {
            return defaultVersion != null ? defaultVersion : "N/A";
        }
        if (upgradeAvailable) {
            return installedVersion + " → " + defaultVersion;
        }
        return installedVersion;
    }

    /**
     * Returns Bootstrap CSS class for the row based on status.
     *
     * @return Bootstrap table row class
     */
    public String getRowCssClass() {
        if (upgradeAvailable) return "table-warning";
        return "";
    }

    /**
     * Returns Bootstrap CSS class for relocatable badge.
     *
     * @return Bootstrap background class
     */
    public String getRelocatableCssClass() {
        return relocatable ? "bg-info" : "bg-secondary";
    }

    /**
     * Returns the schema display text.
     *
     * @return schema name or "N/A"
     */
    public String getSchemaDisplay() {
        return schema != null && !schema.isEmpty() ? schema : "N/A";
    }

    /**
     * Returns whether this is a commonly used/important extension.
     *
     * @return true if extension is commonly used
     */
    public boolean isCommon() {
        if (name == null) return false;
        return switch (name.toLowerCase()) {
            case "pg_stat_statements", "pgcrypto", "uuid-ossp", "hstore",
                 "postgis", "pg_trgm", "btree_gin", "btree_gist",
                 "citext", "ltree", "tablefunc", "fuzzystrmatch" -> true;
            default -> false;
        };
    }

    /**
     * Returns the category of the extension.
     *
     * @return category name
     */
    public String getCategory() {
        if (name == null) return "Other";
        String lower = name.toLowerCase();
        if (lower.startsWith("pg_stat") || lower.equals("auto_explain")) return "Monitoring";
        if (lower.startsWith("postgis") || lower.equals("address_standardizer")) return "Spatial";
        if (lower.equals("pgcrypto") || lower.equals("sslinfo")) return "Security";
        if (lower.contains("search") || lower.equals("pg_trgm") || lower.equals("fuzzystrmatch")) return "Search";
        if (lower.equals("uuid-ossp") || lower.equals("pgstattuple") || lower.equals("tablefunc")) return "Utility";
        if (lower.startsWith("btree_") || lower.equals("intarray")) return "Index";
        return "Other";
    }
}
