package com.bovinemagnet.pgconsole.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

/**
 * Represents a runbook for incident response or maintenance procedures.
 * <p>
 * Runbooks provide step-by-step guidance for common database
 * operations and troubleshooting scenarios.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class Runbook {

    /**
     * Category of runbook.
     */
    public enum Category {
        INCIDENT("Incident Response", "bi-exclamation-diamond", "Respond to active incidents"),
        MAINTENANCE("Maintenance", "bi-wrench", "Regular maintenance procedures"),
        TROUBLESHOOTING("Troubleshooting", "bi-search", "Diagnose and resolve issues"),
        RECOVERY("Recovery", "bi-arrow-counterclockwise", "Disaster recovery procedures");

        private final String displayName;
        private final String icon;
        private final String description;

        Category(String displayName, String icon, String description) {
            this.displayName = displayName;
            this.icon = icon;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getIcon() {
            return icon;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Type of trigger that can start a runbook.
     */
    public enum TriggerType {
        MANUAL("Manual", "Started manually by user"),
        ALERT("Alert", "Triggered by an alert"),
        ANOMALY("Anomaly", "Triggered by anomaly detection"),
        SCHEDULED("Scheduled", "Runs on a schedule");

        private final String displayName;
        private final String description;

        TriggerType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * A single step in the runbook.
     */
    public static class Step {

        /**
         * Type of action for this step.
         */
        public enum ActionType {
            NAVIGATE("Navigate", "bi-box-arrow-up-right", "Navigate to a dashboard page"),
            QUERY("Query", "bi-code-slash", "Execute a read-only SQL query"),
            SQL_TEMPLATE("SQL Template", "bi-file-code", "Execute SQL with parameters"),
            MANUAL("Manual", "bi-person-check", "Manual action required"),
            DOCUMENTATION("Documentation", "bi-book", "Reference documentation");

            private final String displayName;
            private final String icon;
            private final String description;

            ActionType(String displayName, String icon, String description) {
                this.displayName = displayName;
                this.icon = icon;
                this.description = description;
            }

            public String getDisplayName() {
                return displayName;
            }

            public String getIcon() {
                return icon;
            }

            public String getDescription() {
                return description;
            }
        }

        private int order;
        private String title;
        private String description;

        @JsonProperty("action_type")
        private ActionType actionType;

        private String action;

        @JsonProperty("expected_outcome")
        private String expectedOutcome;

        @JsonProperty("auto_execute")
        private boolean autoExecute;

        @JsonProperty("requires_confirmation")
        private boolean requiresConfirmation;

        public Step() {
        }

        public Step(int order, String title, String description, ActionType actionType, String action) {
            this.order = order;
            this.title = title;
            this.description = description;
            this.actionType = actionType;
            this.action = action;
            this.autoExecute = false;
            this.requiresConfirmation = false;
        }

        // Getters and setters
        public int getOrder() {
            return order;
        }

        public void setOrder(int order) {
            this.order = order;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public ActionType getActionType() {
            return actionType;
        }

        public void setActionType(ActionType actionType) {
            this.actionType = actionType;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public boolean isAutoExecute() {
            return autoExecute;
        }

        public void setAutoExecute(boolean autoExecute) {
            this.autoExecute = autoExecute;
        }

        public boolean isRequiresConfirmation() {
            return requiresConfirmation;
        }

        public void setRequiresConfirmation(boolean requiresConfirmation) {
            this.requiresConfirmation = requiresConfirmation;
        }

        public String getExpectedOutcome() {
            return expectedOutcome;
        }

        public void setExpectedOutcome(String expectedOutcome) {
            this.expectedOutcome = expectedOutcome;
        }
    }

    private Long id;
    private String name;
    private String title;
    private String description;
    private Category category;

    // Trigger configuration
    private TriggerType triggerType;
    private String triggerConditions;  // JSON string

    // Steps
    private List<Step> steps;

    // Metadata
    private int version;
    private boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;

    // Estimated duration
    private Integer estimatedDurationMinutes;

    // Whether this runbook can be auto-executed (all steps are safe/non-destructive)
    private boolean autoExecutable;

    public Runbook() {
        this.version = 1;
        this.enabled = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Get the total number of steps.
     */
    public int getStepCount() {
        return steps != null ? steps.size() : 0;
    }

    /**
     * Get the number of auto-executable steps.
     */
    public int getAutoStepCount() {
        if (steps == null) {
            return 0;
        }
        return (int) steps.stream().filter(Step::isAutoExecute).count();
    }

    /**
     * Get a formatted duration display.
     */
    public String getFormattedDuration() {
        if (estimatedDurationMinutes == null) {
            return "Unknown";
        } else if (estimatedDurationMinutes < 60) {
            return estimatedDurationMinutes + " minutes";
        } else {
            int hours = estimatedDurationMinutes / 60;
            int minutes = estimatedDurationMinutes % 60;
            return hours + "h " + minutes + "m";
        }
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public TriggerType getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(TriggerType triggerType) {
        this.triggerType = triggerType;
    }

    public String getTriggerConditions() {
        return triggerConditions;
    }

    public void setTriggerConditions(String triggerConditions) {
        this.triggerConditions = triggerConditions;
    }

    public List<Step> getSteps() {
        return steps;
    }

    public void setSteps(List<Step> steps) {
        this.steps = steps;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Integer getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
    }

    public boolean isAutoExecutable() {
        return autoExecutable;
    }

    public void setAutoExecutable(boolean autoExecutable) {
        this.autoExecutable = autoExecutable;
    }
}
