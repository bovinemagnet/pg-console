package com.bovinemagnet.pgconsole.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Represents a bookmarked query for tracking and annotation.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class QueryBookmark {

    public enum Priority {
        LOW("Low", "bg-secondary"),
        NORMAL("Normal", "bg-info"),
        HIGH("High", "bg-warning text-dark"),
        CRITICAL("Critical", "bg-danger");

        private final String displayName;
        private final String cssClass;

        Priority(String displayName, String cssClass) {
            this.displayName = displayName;
            this.cssClass = cssClass;
        }

        public String getDisplayName() { return displayName; }
        public String getCssClass() { return cssClass; }
    }

    public enum Status {
        ACTIVE("Active", "bg-primary"),
        INVESTIGATING("Investigating", "bg-warning text-dark"),
        RESOLVED("Resolved", "bg-success"),
        IGNORED("Ignored", "bg-secondary");

        private final String displayName;
        private final String cssClass;

        Status(String displayName, String cssClass) {
            this.displayName = displayName;
            this.cssClass = cssClass;
        }

        public String getDisplayName() { return displayName; }
        public String getCssClass() { return cssClass; }
    }

    private long id;
    private Instant createdAt;
    private Instant updatedAt;
    private String instanceId;
    private String queryId;
    private String queryText;
    private String title;
    private String notes;
    private List<String> tags;
    private String createdBy;
    private Priority priority = Priority.NORMAL;
    private Status status = Status.ACTIVE;

    public QueryBookmark() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedAtFormatted() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(createdAt);
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedAtFormatted() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(updatedAt);
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getQueryId() {
        return queryId;
    }

    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }

    public String getQueryText() {
        return queryText;
    }

    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }

    public String getQueryTextPreview() {
        if (queryText == null) return "";
        return queryText.length() > 100 ? queryText.substring(0, 100) + "..." : queryText;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDisplayTitle() {
        if (title != null && !title.isEmpty()) {
            return title;
        }
        return "Query " + queryId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getTagsFormatted() {
        if (tags == null || tags.isEmpty()) return "";
        return String.join(", ", tags);
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

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
