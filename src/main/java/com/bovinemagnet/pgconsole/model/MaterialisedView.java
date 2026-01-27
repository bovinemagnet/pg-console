package com.bovinemagnet.pgconsole.model;

/**
 * Represents a materialised view from the PostgreSQL system catalog {@code pg_matviews}.
 * <p>
 * This class captures information about materialised views including their
 * schema, name, owner, storage properties, and population status.
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see <a href="https://www.postgresql.org/docs/current/view-pg-matviews.html">pg_matviews Documentation</a>
 */
public class MaterialisedView {

    /** Schema name containing the materialised view */
    private String schemaName;

    /** Name of the materialised view */
    private String matviewName;

    /** Owner of the materialised view */
    private String matviewOwner;

    /** Tablespace where the view is stored (null = default) */
    private String tablespace;

    /** Whether the view has any indexes */
    private boolean hasIndexes;

    /** Whether the view has been populated */
    private boolean ispopulated;

    /** The query definition of the view */
    private String definition;

    /** Size of the materialised view in bytes */
    private long sizeBytes;

    /** Human-readable size */
    private String sizeFormatted;

    /**
     * Constructs a new MaterialisedView instance.
     */
    public MaterialisedView() {
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getMatviewName() {
        return matviewName;
    }

    public void setMatviewName(String matviewName) {
        this.matviewName = matviewName;
    }

    public String getMatviewOwner() {
        return matviewOwner;
    }

    public void setMatviewOwner(String matviewOwner) {
        this.matviewOwner = matviewOwner;
    }

    public String getTablespace() {
        return tablespace;
    }

    public void setTablespace(String tablespace) {
        this.tablespace = tablespace;
    }

    public boolean isHasIndexes() {
        return hasIndexes;
    }

    public void setHasIndexes(boolean hasIndexes) {
        this.hasIndexes = hasIndexes;
    }

    public boolean isIspopulated() {
        return ispopulated;
    }

    public void setIspopulated(boolean ispopulated) {
        this.ispopulated = ispopulated;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getSizeFormatted() {
        return sizeFormatted;
    }

    public void setSizeFormatted(String sizeFormatted) {
        this.sizeFormatted = sizeFormatted;
    }

    /**
     * Returns the fully qualified view name.
     *
     * @return schema.view format
     */
    public String getFullName() {
        if (schemaName != null && !schemaName.isEmpty()) {
            return schemaName + "." + matviewName;
        }
        return matviewName;
    }

    /**
     * Returns a truncated version of the definition for display.
     *
     * @param maxLength maximum length before truncation
     * @return truncated definition
     */
    public String getDefinitionTruncated(int maxLength) {
        if (definition == null) return "";
        String normalised = definition.replaceAll("\\s+", " ").trim();
        if (normalised.length() <= maxLength) return normalised;
        return normalised.substring(0, maxLength - 3) + "...";
    }

    /**
     * Returns Bootstrap CSS class for populated status badge.
     *
     * @return Bootstrap background class
     */
    public String getPopulatedCssClass() {
        return ispopulated ? "bg-success" : "bg-warning text-dark";
    }

    /**
     * Returns the populated status display text.
     *
     * @return status text
     */
    public String getPopulatedDisplay() {
        return ispopulated ? "Populated" : "Empty";
    }

    /**
     * Returns Bootstrap CSS class for indexes status badge.
     *
     * @return Bootstrap background class
     */
    public String getIndexesCssClass() {
        return hasIndexes ? "bg-info" : "bg-secondary";
    }

    /**
     * Returns Bootstrap CSS class for the row based on status.
     *
     * @return Bootstrap table row class
     */
    public String getRowCssClass() {
        if (!ispopulated) return "table-warning";
        return "";
    }

    /**
     * Returns the tablespace display text.
     *
     * @return tablespace name or "Default"
     */
    public String getTablespaceDisplay() {
        return tablespace != null && !tablespace.isEmpty() ? tablespace : "Default";
    }

    /**
     * Formats the size bytes as a human-readable string.
     *
     * @return formatted size string
     */
    public String formatSize() {
        if (sizeBytes < 0) return "N/A";
        if (sizeBytes == 0) return "0 B";
        if (sizeBytes < 1024) return sizeBytes + " B";
        if (sizeBytes < 1024 * 1024) return String.format("%.1f KB", sizeBytes / 1024.0);
        if (sizeBytes < 1024 * 1024 * 1024) return String.format("%.1f MB", sizeBytes / (1024.0 * 1024));
        return String.format("%.1f GB", sizeBytes / (1024.0 * 1024 * 1024));
    }
}
