package com.bovinemagnet.pgconsole.model;

/**
 * Represents a health check result for a specific PostgreSQL metric or subsystem.
 * <p>
 * Health checks provide quick visibility into the state of various database
 * components, using traffic light indicators (OK/WARNING/CRITICAL) for easy
 * scanning. Each check includes the current value, thresholds, and a link
 * to a detailed dashboard for further investigation.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class HealthCheck {

    /**
     * Health status levels using traffic light colours.
     */
    public enum Status {
        OK("OK", "bg-success", "text-success", "bi-check-circle-fill"),
        WARNING("Warning", "bg-warning text-dark", "text-warning", "bi-exclamation-triangle-fill"),
        CRITICAL("Critical", "bg-danger", "text-danger", "bi-x-circle-fill");

        private final String displayName;
        private final String badgeCssClass;
        private final String textCssClass;
        private final String iconClass;

        Status(String displayName, String badgeCssClass, String textCssClass, String iconClass) {
            this.displayName = displayName;
            this.badgeCssClass = badgeCssClass;
            this.textCssClass = textCssClass;
            this.iconClass = iconClass;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getBadgeCssClass() {
            return badgeCssClass;
        }

        public String getTextCssClass() {
            return textCssClass;
        }

        public String getIconClass() {
            return iconClass;
        }
    }

    /**
     * Categories for grouping related health checks.
     */
    public enum Category {
        CONNECTIONS("Connections", "bi-plug", 1),
        PERFORMANCE("Performance", "bi-speedometer2", 2),
        MAINTENANCE("Maintenance", "bi-wrench", 3),
        REPLICATION("Replication", "bi-arrow-repeat", 4),
        CONFIGURATION("Configuration", "bi-gear", 5);

        private final String displayName;
        private final String iconClass;
        private final int order;

        Category(String displayName, String iconClass, int order) {
            this.displayName = displayName;
            this.iconClass = iconClass;
            this.order = order;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getIconClass() {
            return iconClass;
        }

        public int getOrder() {
            return order;
        }
    }

    private String name;
    private String description;
    private String currentValue;
    private Status status;
    private Category category;
    private String warningThreshold;
    private String criticalThreshold;
    private String detailsLink;
    private String recommendation;

    public HealthCheck() {
    }

    // Getters and Setters

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

    public String getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(String currentValue) {
        this.currentValue = currentValue;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getWarningThreshold() {
        return warningThreshold;
    }

    public void setWarningThreshold(String warningThreshold) {
        this.warningThreshold = warningThreshold;
    }

    public String getCriticalThreshold() {
        return criticalThreshold;
    }

    public void setCriticalThreshold(String criticalThreshold) {
        this.criticalThreshold = criticalThreshold;
    }

    public String getDetailsLink() {
        return detailsLink;
    }

    public void setDetailsLink(String detailsLink) {
        this.detailsLink = detailsLink;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    // Helper methods

    public String getStatusBadgeClass() {
        return status != null ? status.getBadgeCssClass() : "bg-secondary";
    }

    public String getStatusTextClass() {
        return status != null ? status.getTextCssClass() : "text-muted";
    }

    public String getStatusIconClass() {
        return status != null ? status.getIconClass() : "bi-question-circle";
    }

    public String getCategoryDisplayName() {
        return category != null ? category.getDisplayName() : "Other";
    }

    public String getCategoryIconClass() {
        return category != null ? category.getIconClass() : "bi-grid";
    }

    public boolean hasRecommendation() {
        return recommendation != null && !recommendation.isEmpty();
    }

    public boolean hasDetailsLink() {
        return detailsLink != null && !detailsLink.isEmpty();
    }

    /**
     * Creates a builder for fluent construction.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating HealthCheck instances.
     */
    public static class Builder {
        private final HealthCheck check = new HealthCheck();

        public Builder name(String name) {
            check.setName(name);
            return this;
        }

        public Builder description(String description) {
            check.setDescription(description);
            return this;
        }

        public Builder currentValue(String currentValue) {
            check.setCurrentValue(currentValue);
            return this;
        }

        public Builder status(Status status) {
            check.setStatus(status);
            return this;
        }

        public Builder category(Category category) {
            check.setCategory(category);
            return this;
        }

        public Builder warningThreshold(String warningThreshold) {
            check.setWarningThreshold(warningThreshold);
            return this;
        }

        public Builder criticalThreshold(String criticalThreshold) {
            check.setCriticalThreshold(criticalThreshold);
            return this;
        }

        public Builder detailsLink(String detailsLink) {
            check.setDetailsLink(detailsLink);
            return this;
        }

        public Builder recommendation(String recommendation) {
            check.setRecommendation(recommendation);
            return this;
        }

        public HealthCheck build() {
            return check;
        }
    }
}
