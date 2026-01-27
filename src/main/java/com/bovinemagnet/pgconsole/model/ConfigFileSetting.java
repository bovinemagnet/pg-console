package com.bovinemagnet.pgconsole.model;

/**
 * Represents a configuration file setting from the PostgreSQL system view
 * {@code pg_file_settings}.
 * <p>
 * This class captures information about configuration settings as they appear
 * in configuration files (postgresql.conf, pg_hba.conf, etc.), including
 * any errors that would prevent the setting from being applied.
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see <a href="https://www.postgresql.org/docs/current/view-pg-file-settings.html">pg_file_settings Documentation</a>
 */
public class ConfigFileSetting {

    /** Source file path containing the setting */
    private String sourcefile;

    /** Line number in the source file */
    private int sourceline;

    /** Sequence number for ordering settings */
    private int seqno;

    /** Name of the configuration parameter */
    private String name;

    /** Value of the configuration parameter as a string */
    private String setting;

    /** Whether the setting was successfully applied */
    private boolean applied;

    /** Error message if the setting could not be applied */
    private String error;

    /**
     * Constructs a new ConfigFileSetting instance.
     */
    public ConfigFileSetting() {
    }

    public String getSourcefile() {
        return sourcefile;
    }

    public void setSourcefile(String sourcefile) {
        this.sourcefile = sourcefile;
    }

    public int getSourceline() {
        return sourceline;
    }

    public void setSourceline(int sourceline) {
        this.sourceline = sourceline;
    }

    public int getSeqno() {
        return seqno;
    }

    public void setSeqno(int seqno) {
        this.seqno = seqno;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSetting() {
        return setting;
    }

    public void setSetting(String setting) {
        this.setting = setting;
    }

    public boolean isApplied() {
        return applied;
    }

    public void setApplied(boolean applied) {
        this.applied = applied;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    /**
     * Returns whether this setting has an error.
     *
     * @return true if the setting has an error
     */
    public boolean hasError() {
        return error != null && !error.isEmpty();
    }

    /**
     * Returns the filename only (without path).
     *
     * @return filename without directory path
     */
    public String getFilename() {
        if (sourcefile == null) return "Unknown";
        int lastSlash = sourcefile.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < sourcefile.length() - 1) {
            return sourcefile.substring(lastSlash + 1);
        }
        return sourcefile;
    }

    /**
     * Returns the location formatted as file:line.
     *
     * @return formatted location string
     */
    public String getLocationFormatted() {
        return getFilename() + ":" + sourceline;
    }

    /**
     * Returns Bootstrap CSS class for the row based on error state.
     *
     * @return Bootstrap table row class
     */
    public String getRowCssClass() {
        if (hasError()) return "table-danger";
        if (!applied) return "table-warning";
        return "";
    }

    /**
     * Returns Bootstrap CSS class for the status badge.
     *
     * @return Bootstrap background class
     */
    public String getStatusCssClass() {
        if (hasError()) return "bg-danger";
        if (!applied) return "bg-warning text-dark";
        return "bg-success";
    }

    /**
     * Returns the status display text.
     *
     * @return status text
     */
    public String getStatusDisplay() {
        if (hasError()) return "Error";
        if (!applied) return "Pending";
        return "Applied";
    }

    /**
     * Returns a truncated version of the setting value for display.
     *
     * @param maxLength maximum length before truncation
     * @return truncated setting value
     */
    public String getSettingTruncated(int maxLength) {
        if (setting == null) return "";
        if (setting.length() <= maxLength) return setting;
        return setting.substring(0, maxLength - 3) + "...";
    }

    /**
     * Returns a truncated error message for display.
     *
     * @param maxLength maximum length before truncation
     * @return truncated error message
     */
    public String getErrorTruncated(int maxLength) {
        if (error == null) return "";
        if (error.length() <= maxLength) return error;
        return error.substring(0, maxLength - 3) + "...";
    }
}
