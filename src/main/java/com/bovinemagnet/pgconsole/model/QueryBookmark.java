package com.bovinemagnet.pgconsole.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Represents a bookmarked query for tracking and annotation purposes.
 * <p>
 * Query bookmarks allow database administrators and developers to mark specific
 * queries from {@code pg_stat_statements} for future reference, investigation,
 * or monitoring. Each bookmark can include a custom title, notes, tags, priority
 * level, and status to facilitate query performance management workflows.
 * </p>
 * <p>
 * Bookmarks are typically created when identifying problematic queries that require
 * investigation, optimisation, or ongoing monitoring. The priority and status fields
 * help organise and triage bookmarked queries.
 * </p>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see com.bovinemagnet.pgconsole.model.SlowQuery
 */
public class QueryBookmark {

    /**
     * Represents the priority level assigned to a bookmarked query.
     * <p>
     * Priority levels help triage queries based on their impact on database
     * performance or business operations. Each priority has an associated
     * display name and Bootstrap CSS class for consistent UI rendering.
     * </p>
     */
    public enum Priority {
        /** Low priority query that can be addressed when convenient. */
        LOW("Low", "bg-secondary"),

        /** Normal priority query with standard importance. Default priority level. */
        NORMAL("Normal", "bg-info"),

        /** High priority query requiring prompt attention. */
        HIGH("High", "bg-warning text-dark"),

        /** Critical priority query requiring immediate investigation. */
        CRITICAL("Critical", "bg-danger");

        private final String displayName;
        private final String cssClass;

        /**
         * Constructs a Priority enum with display properties.
         *
         * @param displayName the human-readable name for display in the UI
         * @param cssClass the Bootstrap CSS class for visual styling
         */
        Priority(String displayName, String cssClass) {
            this.displayName = displayName;
            this.cssClass = cssClass;
        }

        /**
         * Returns the human-readable display name for this priority level.
         *
         * @return the display name, never {@code null}
         */
        public String getDisplayName() { return displayName; }

        /**
         * Returns the Bootstrap CSS class for styling this priority level.
         *
         * @return the CSS class string, never {@code null}
         */
        public String getCssClass() { return cssClass; }
    }

    /**
     * Represents the current status of a bookmarked query investigation.
     * <p>
     * Status values track the lifecycle of query investigations, from initial
     * identification through resolution or deliberate dismissal. Each status
     * has an associated display name and Bootstrap CSS class for UI rendering.
     * </p>
     */
    public enum Status {
        /** Query is active and requires attention. Default status. */
        ACTIVE("Active", "bg-primary"),

        /** Query is currently under investigation. */
        INVESTIGATING("Investigating", "bg-warning text-dark"),

        /** Query issue has been resolved or optimised. */
        RESOLVED("Resolved", "bg-success"),

        /** Query has been deliberately marked as acceptable or not requiring action. */
        IGNORED("Ignored", "bg-secondary");

        private final String displayName;
        private final String cssClass;

        /**
         * Constructs a Status enum with display properties.
         *
         * @param displayName the human-readable name for display in the UI
         * @param cssClass the Bootstrap CSS class for visual styling
         */
        Status(String displayName, String cssClass) {
            this.displayName = displayName;
            this.cssClass = cssClass;
        }

        /**
         * Returns the human-readable display name for this status.
         *
         * @return the display name, never {@code null}
         */
        public String getDisplayName() { return displayName; }

        /**
         * Returns the Bootstrap CSS class for styling this status.
         *
         * @return the CSS class string, never {@code null}
         */
        public String getCssClass() { return cssClass; }
    }

    /** Unique identifier for this bookmark. Primary key in the database. */
    private long id;

    /** Timestamp when this bookmark was created. Set automatically on construction. */
    private Instant createdAt;

    /** Timestamp when this bookmark was last updated. Set automatically on construction and updates. */
    private Instant updatedAt;

    /** Identifier of the PostgreSQL instance this query belongs to. */
    private String instanceId;

    /** The query identifier from {@code pg_stat_statements.queryid}. */
    private String queryId;

    /** The full SQL text of the bookmarked query. May be {@code null}. */
    private String queryText;

    /** Optional custom title for this bookmark. When {@code null}, a default title is generated. */
    private String title;

    /** Optional notes or comments about this query and its investigation. */
    private String notes;

    /** Optional list of tags for categorising this bookmark. May be {@code null} or empty. */
    private List<String> tags;

    /** Username or identifier of the person who created this bookmark. */
    private String createdBy;

    /** Priority level of this bookmark. Defaults to {@link Priority#NORMAL}. */
    private Priority priority = Priority.NORMAL;

    /** Current status of this bookmark. Defaults to {@link Status#ACTIVE}. */
    private Status status = Status.ACTIVE;

    /**
     * Constructs a new QueryBookmark with creation and update timestamps set to the current time.
     * <p>
     * Priority defaults to {@link Priority#NORMAL} and status defaults to {@link Status#ACTIVE}.
     * </p>
     */
    public QueryBookmark() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Returns the unique identifier for this bookmark.
     *
     * @return the bookmark ID
     */
    public long getId() {
        return id;
    }

    /**
     * Sets the unique identifier for this bookmark.
     *
     * @param id the bookmark ID to set
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Returns the timestamp when this bookmark was created.
     *
     * @return the creation timestamp, never {@code null}
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the creation timestamp for this bookmark.
     *
     * @param createdAt the creation timestamp to set
     */
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Returns the creation timestamp formatted as a string for display.
     * <p>
     * Uses the pattern "yyyy-MM-dd HH:mm" in the system's default time zone.
     * </p>
     *
     * @return the formatted creation timestamp in the format "yyyy-MM-dd HH:mm"
     */
    public String getCreatedAtFormatted() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(createdAt);
    }

    /**
     * Returns the timestamp when this bookmark was last updated.
     *
     * @return the last update timestamp, never {@code null}
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the last update timestamp for this bookmark.
     *
     * @param updatedAt the update timestamp to set
     */
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Returns the update timestamp formatted as a string for display.
     * <p>
     * Uses the pattern "yyyy-MM-dd HH:mm" in the system's default time zone.
     * </p>
     *
     * @return the formatted update timestamp in the format "yyyy-MM-dd HH:mm"
     */
    public String getUpdatedAtFormatted() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(updatedAt);
    }

    /**
     * Returns the PostgreSQL instance identifier for this query.
     *
     * @return the instance ID, may be {@code null}
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * Sets the PostgreSQL instance identifier for this query.
     *
     * @param instanceId the instance ID to set
     */
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    /**
     * Returns the query identifier from {@code pg_stat_statements}.
     *
     * @return the query ID, may be {@code null}
     */
    public String getQueryId() {
        return queryId;
    }

    /**
     * Sets the query identifier from {@code pg_stat_statements}.
     *
     * @param queryId the query ID to set
     */
    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }

    /**
     * Returns the full SQL text of the bookmarked query.
     *
     * @return the query text, may be {@code null}
     */
    public String getQueryText() {
        return queryText;
    }

    /**
     * Sets the full SQL text of the bookmarked query.
     *
     * @param queryText the query text to set
     */
    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }

    /**
     * Returns a truncated preview of the query text for display in lists or tables.
     * <p>
     * If the query text is longer than 100 characters, it is truncated to 100 characters
     * with "..." appended. If the query text is {@code null}, returns an empty string.
     * </p>
     *
     * @return the query text preview, truncated to 100 characters if necessary, never {@code null}
     */
    public String getQueryTextPreview() {
        if (queryText == null) return "";
        return queryText.length() > 100 ? queryText.substring(0, 100) + "..." : queryText;
    }

    /**
     * Returns the custom title for this bookmark.
     *
     * @return the title, may be {@code null} or empty
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the custom title for this bookmark.
     *
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the display title for this bookmark.
     * <p>
     * If a custom title has been set, returns the custom title. Otherwise,
     * generates a default title in the format "Query {queryId}".
     * </p>
     *
     * @return the display title, never {@code null} or empty
     */
    public String getDisplayTitle() {
        if (title != null && !title.isEmpty()) {
            return title;
        }
        return "Query " + queryId;
    }

    /**
     * Returns the notes or comments for this bookmark.
     *
     * @return the notes, may be {@code null}
     */
    public String getNotes() {
        return notes;
    }

    /**
     * Sets the notes or comments for this bookmark.
     *
     * @param notes the notes to set
     */
    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * Returns the list of tags associated with this bookmark.
     *
     * @return the tags list, may be {@code null} or empty
     */
    public List<String> getTags() {
        return tags;
    }

    /**
     * Sets the list of tags for this bookmark.
     *
     * @param tags the tags list to set
     */
    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    /**
     * Returns a comma-separated string of all tags for display.
     * <p>
     * If the tags list is {@code null} or empty, returns an empty string.
     * </p>
     *
     * @return the comma-separated tags string, never {@code null}
     */
    public String getTagsFormatted() {
        if (tags == null || tags.isEmpty()) return "";
        return String.join(", ", tags);
    }

    /**
     * Returns the username or identifier of the person who created this bookmark.
     *
     * @return the creator username, may be {@code null}
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Sets the username or identifier of the person who created this bookmark.
     *
     * @param createdBy the creator username to set
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Returns the priority level of this bookmark.
     *
     * @return the priority, never {@code null}
     */
    public Priority getPriority() {
        return priority;
    }

    /**
     * Sets the priority level of this bookmark.
     *
     * @param priority the priority to set
     */
    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    /**
     * Sets the priority from a string value.
     * <p>
     * Attempts to parse the string as a {@link Priority} enum value (case-insensitive).
     * If the string is {@code null} or cannot be parsed, defaults to {@link Priority#NORMAL}.
     * </p>
     *
     * @param priority the priority string to parse (e.g., "high", "CRITICAL")
     */
    public void setPriorityFromString(String priority) {
        if (priority == null) {
            this.priority = Priority.NORMAL;
            return;
        }
        try {
            this.priority = Priority.valueOf(priority.toUpperCase());
        } catch (Exception e) {
            this.priority = Priority.NORMAL;
        }
    }

    /**
     * Returns the current status of this bookmark.
     *
     * @return the status, never {@code null}
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Sets the status of this bookmark.
     *
     * @param status the status to set
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Sets the status from a string value.
     * <p>
     * Attempts to parse the string as a {@link Status} enum value (case-insensitive).
     * If the string is {@code null} or cannot be parsed, defaults to {@link Status#ACTIVE}.
     * </p>
     *
     * @param status the status string to parse (e.g., "investigating", "RESOLVED")
     */
    public void setStatusFromString(String status) {
        if (status == null) {
            this.status = Status.ACTIVE;
            return;
        }
        try {
            this.status = Status.valueOf(status.toUpperCase());
        } catch (Exception e) {
            this.status = Status.ACTIVE;
        }
    }
}
