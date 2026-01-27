package com.bovinemagnet.pgconsole.model;

import java.time.Instant;

/**
 * Represents an open cursor from the PostgreSQL system view {@code pg_cursors}.
 * <p>
 * This class captures information about cursors that are currently declared
 * and available across all sessions, including the cursor properties and
 * the session that created it.
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see <a href="https://www.postgresql.org/docs/current/view-pg-cursors.html">pg_cursors Documentation</a>
 */
public class OpenCursor {

    /** Name of the cursor */
    private String name;

    /** The SQL statement that declared the cursor */
    private String statement;

    /** Whether the cursor is holdable (survives transaction commit) */
    private boolean isHoldable;

    /** Whether the cursor is binary */
    private boolean isBinary;

    /** Whether the cursor is scrollable */
    private boolean isScrollable;

    /** Time when the cursor was created */
    private Instant creationTime;

    /**
     * Constructs a new OpenCursor instance.
     */
    public OpenCursor() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatement() {
        return statement;
    }

    public void setStatement(String statement) {
        this.statement = statement;
    }

    public boolean isHoldable() {
        return isHoldable;
    }

    public void setHoldable(boolean holdable) {
        isHoldable = holdable;
    }

    public boolean isBinary() {
        return isBinary;
    }

    public void setBinary(boolean binary) {
        isBinary = binary;
    }

    public boolean isScrollable() {
        return isScrollable;
    }

    public void setScrollable(boolean scrollable) {
        isScrollable = scrollable;
    }

    public Instant getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Instant creationTime) {
        this.creationTime = creationTime;
    }

    /**
     * Returns a truncated version of the statement for display.
     *
     * @param maxLength maximum length before truncation
     * @return truncated statement
     */
    public String getStatementTruncated(int maxLength) {
        if (statement == null) return "";
        String normalised = statement.replaceAll("\\s+", " ").trim();
        if (normalised.length() <= maxLength) return normalised;
        return normalised.substring(0, maxLength - 3) + "...";
    }

    /**
     * Returns a list of cursor properties as badges.
     *
     * @return comma-separated list of properties
     */
    public String getProperties() {
        StringBuilder sb = new StringBuilder();
        if (isHoldable) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("Holdable");
        }
        if (isScrollable) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("Scrollable");
        }
        if (isBinary) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("Binary");
        }
        if (sb.length() == 0) {
            return "Standard";
        }
        return sb.toString();
    }

    /**
     * Returns Bootstrap CSS class for holdable status.
     *
     * @return Bootstrap background class
     */
    public String getHoldableCssClass() {
        return isHoldable ? "bg-warning text-dark" : "bg-secondary";
    }

    /**
     * Returns Bootstrap CSS class for scrollable status.
     *
     * @return Bootstrap background class
     */
    public String getScrollableCssClass() {
        return isScrollable ? "bg-info" : "bg-secondary";
    }

    /**
     * Returns the age of the cursor as a human-readable string.
     *
     * @return formatted age string
     */
    public String getAgeFormatted() {
        if (creationTime == null) return "Unknown";
        long seconds = java.time.Duration.between(creationTime, Instant.now()).getSeconds();
        if (seconds < 60) return seconds + " seconds";
        if (seconds < 3600) return (seconds / 60) + " minutes";
        if (seconds < 86400) return (seconds / 3600) + " hours";
        return (seconds / 86400) + " days";
    }
}
