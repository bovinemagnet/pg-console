package com.bovinemagnet.pgconsole.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

/**
 * Represents a runbook for incident response or maintenance procedures.
 * <p>
 * Runbooks provide step-by-step guidance for common database operations, troubleshooting scenarios,
 * and incident response workflows. Each runbook consists of an ordered sequence of steps that can be
 * executed manually or automatically, depending on the step type and safety considerations.
 * <p>
 * A runbook can be triggered in various ways:
 * <ul>
 *   <li>Manually by a user through the UI</li>
 *   <li>Automatically by an alert detection system</li>
 *   <li>Through anomaly detection mechanisms</li>
 *   <li>On a predefined schedule</li>
 * </ul>
 * <p>
 * Each runbook tracks metadata including version, creation/update timestamps, and the creator.
 * Runbooks can be enabled or disabled, and may support full auto-execution if all steps are
 * non-destructive and safe to run without human intervention.
 * <p>
 * Example usage in a template:
 * <pre>{@code
 * Runbook runbook = new Runbook();
 * runbook.setName("high-connection-count");
 * runbook.setTitle("High Connection Count Response");
 * runbook.setCategory(Category.INCIDENT);
 * runbook.setTriggerType(TriggerType.ALERT);
 * runbook.setSteps(Arrays.asList(
 *     new Step(1, "Check Connections", "Review active connections",
 *              Step.ActionType.NAVIGATE, "/activity"),
 *     new Step(2, "Identify Blocking", "Check for blocking queries",
 *              Step.ActionType.NAVIGATE, "/locks")
 * ));
 * }</pre>
 *
 * @author Paul Snow
 * @since 0.0.0
 */
public class Runbook {

    /**
     * Defines the category of a runbook, which determines its purpose and presentation in the UI.
     * <p>
     * Each category has an associated display name, Bootstrap icon class, and description
     * that helps users understand when to use runbooks in that category.
     */
    public enum Category {
        /** Runbooks for responding to active database incidents requiring immediate attention. */
        INCIDENT("Incident Response", "bi-exclamation-diamond", "Respond to active incidents"),

        /** Runbooks for regular database maintenance tasks and routine operations. */
        MAINTENANCE("Maintenance", "bi-wrench", "Regular maintenance procedures"),

        /** Runbooks for diagnosing and resolving database issues and performance problems. */
        TROUBLESHOOTING("Troubleshooting", "bi-search", "Diagnose and resolve issues"),

        /** Runbooks for disaster recovery, backup restoration, and failover procedures. */
        RECOVERY("Recovery", "bi-arrow-counterclockwise", "Disaster recovery procedures");

        private final String displayName;
        private final String icon;
        private final String description;

        /**
         * Constructs a category with display information.
         *
         * @param displayName the human-readable name shown in the UI
         * @param icon the Bootstrap icon class (e.g., "bi-exclamation-diamond")
         * @param description a brief description of when to use this category
         */
        Category(String displayName, String icon, String description) {
            this.displayName = displayName;
            this.icon = icon;
            this.description = description;
        }

        /**
         * Returns the human-readable display name for this category.
         *
         * @return the display name (e.g., "Incident Response")
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Returns the Bootstrap icon class for this category.
         *
         * @return the icon class name (e.g., "bi-exclamation-diamond")
         */
        public String getIcon() {
            return icon;
        }

        /**
         * Returns a description of when to use runbooks in this category.
         *
         * @return the category description
         */
        public String getDescription() {
            return description;
        }
    }

    /**
     * Defines the type of trigger that can initiate a runbook execution.
     * <p>
     * Trigger types determine how and when a runbook is invoked, ranging from manual
     * user-initiated execution to automated triggers based on alerts, anomalies, or schedules.
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
