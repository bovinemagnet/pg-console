package com.bovinemagnet.pgconsole.model;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a user-defined custom dashboard with personalised widgets.
 * Dashboards can be shared between users and set as default.
 *
 * @author Paul Snow
 * @since 0.0.0
 */
public class CustomDashboard {

    private Long id;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String instanceId;
    private String name;
    private String description;
    private String layout; // JSON string for layout configuration
    private String createdBy;
    private boolean isDefault;
    private boolean isShared;
    private List<String> tags;
    private List<CustomWidget> widgets;

    public CustomDashboard() {
        this.tags = new ArrayList<>();
        this.widgets = new ArrayList<>();
        this.layout = "{\"columns\": 2}";
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final CustomDashboard dashboard = new CustomDashboard();

        public Builder id(Long id) {
            dashboard.id = id;
            return this;
        }

        public Builder createdAt(OffsetDateTime createdAt) {
            dashboard.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(OffsetDateTime updatedAt) {
            dashboard.updatedAt = updatedAt;
            return this;
        }

        public Builder instanceId(String instanceId) {
            dashboard.instanceId = instanceId;
            return this;
        }

        public Builder name(String name) {
            dashboard.name = name;
            return this;
        }

        public Builder description(String description) {
            dashboard.description = description;
            return this;
        }

        public Builder layout(String layout) {
            dashboard.layout = layout;
            return this;
        }

        public Builder createdBy(String createdBy) {
            dashboard.createdBy = createdBy;
            return this;
        }

        public Builder isDefault(boolean isDefault) {
            dashboard.isDefault = isDefault;
            return this;
        }

        public Builder isShared(boolean isShared) {
            dashboard.isShared = isShared;
            return this;
        }

        public Builder tags(List<String> tags) {
            dashboard.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
            return this;
        }

        public Builder widgets(List<CustomWidget> widgets) {
            dashboard.widgets = widgets != null ? new ArrayList<>(widgets) : new ArrayList<>();
            return this;
        }

        public CustomDashboard build() {
            return dashboard;
        }
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLayout() {
        return layout;
    }

    public void setLayout(String layout) {
        this.layout = layout;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public boolean isShared() {
        return isShared;
    }

    public void setShared(boolean shared) {
        isShared = shared;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags != null ? tags : new ArrayList<>();
    }

    public List<CustomWidget> getWidgets() {
        return widgets;
    }

    public void setWidgets(List<CustomWidget> widgets) {
        this.widgets = widgets != null ? widgets : new ArrayList<>();
    }

    public void addWidget(CustomWidget widget) {
        if (this.widgets == null) {
            this.widgets = new ArrayList<>();
        }
        this.widgets.add(widget);
    }
}
