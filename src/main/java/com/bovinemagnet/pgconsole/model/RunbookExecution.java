package com.bovinemagnet.pgconsole.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Represents an execution of a runbook.
 * <p>
 * Tracks the progress through runbook steps, results from each step,
 * and the overall status of the execution.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class RunbookExecution {

    /**
     * Status of the execution.
     */
    public enum Status {
        IN_PROGRESS("In Progress", "bg-primary", "bi-hourglass-split"),
        COMPLETED("Completed", "bg-success", "bi-check-circle"),
        FAILED("Failed", "bg-danger", "bi-x-circle"),
        CANCELLED("Cancelled", "bg-secondary", "bi-slash-circle");

        private final String displayName;
        private final String cssClass;
        private final String icon;

        Status(String displayName, String cssClass, String icon) {
            this.displayName = displayName;
            this.cssClass = cssClass;
            this.icon = icon;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getCssClass() {
            return cssClass;
        }

        public String getIcon() {
            return icon;
        }
    }

    /**
     * What triggered the execution.
     */
    public enum TriggeredBy {
        MANUAL("Manual", "User initiated"),
        ALERT("Alert", "Triggered by alert"),
        ANOMALY("Anomaly", "Triggered by anomaly detection"),
        SCHEDULED("Scheduled", "Scheduled execution");

        private final String displayName;
        private final String description;

        TriggeredBy(String displayName, String description) {
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
     * Result of a single step execution.
     */
    public static class StepResult {

        /**
         * Status of the step.
         */
        public enum StepStatus {
            PENDING("Pending"),
            RUNNING("Running"),
            COMPLETED("Completed"),
            SKIPPED("Skipped"),
            FAILED("Failed");

            private final String displayName;

            StepStatus(String displayName) {
                this.displayName = displayName;
            }

            public String getDisplayName() {
                return displayName;
            }
        }

        private int stepOrder;
        private StepStatus status;
        private Instant startedAt;
        private Instant completedAt;
        private String output;
        private String errorMessage;
        private boolean userConfirmed;

        public StepResult() {
            this.status = StepStatus.PENDING;
        }

        public StepResult(int stepOrder) {
            this.stepOrder = stepOrder;
            this.status = StepStatus.PENDING;
        }

        /**
         * Get the duration of this step.
         */
        @JsonIgnore
        public Duration getDuration() {
            if (startedAt == null || completedAt == null) {
                return null;
            }
            return Duration.between(startedAt, completedAt);
        }

        // Getters and setters
        public int getStepOrder() {
            return stepOrder;
        }

        public void setStepOrder(int stepOrder) {
            this.stepOrder = stepOrder;
        }

        public StepStatus getStatus() {
            return status;
        }

        public void setStatus(StepStatus status) {
            this.status = status;
        }

        public Instant getStartedAt() {
            return startedAt;
        }

        public void setStartedAt(Instant startedAt) {
            this.startedAt = startedAt;
        }

        public Instant getCompletedAt() {
            return completedAt;
        }

        public void setCompletedAt(Instant completedAt) {
            this.completedAt = completedAt;
        }

        public String getOutput() {
            return output;
        }

        public void setOutput(String output) {
            this.output = output;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public boolean isUserConfirmed() {
            return userConfirmed;
        }

        public void setUserConfirmed(boolean userConfirmed) {
            this.userConfirmed = userConfirmed;
        }
    }

    private Long id;
    private Long runbookId;
    private String instanceId;
    private String databaseName;  // Specific database to scope diagnostics to (null = all databases)

    // Associated runbook (for display)
    private Runbook runbook;

    // Execution details
    private Instant startedAt;
    private Instant completedAt;
    private Status status;

    // Trigger context
    private TriggeredBy triggeredBy;
    private Long alertId;
    private Long anomalyId;

    // Step progress
    private int currentStep;
    private List<StepResult> stepResults;

    // User context
    private String executedBy;
    private String notes;

    public RunbookExecution() {
        this.startedAt = Instant.now();
        this.status = Status.IN_PROGRESS;
        this.currentStep = 1;
    }

    /**
     * Get the total duration of the execution.
     */
    public Duration getDuration() {
        if (startedAt == null) {
            return null;
        }
        Instant end = completedAt != null ? completedAt : Instant.now();
        return Duration.between(startedAt, end);
    }

    /**
     * Get a formatted duration string.
     */
    public String getFormattedDuration() {
        Duration duration = getDuration();
        if (duration == null) {
            return "N/A";
        }
        long minutes = duration.toMinutes();
        long seconds = duration.toSecondsPart();
        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }

    /**
     * Get the progress percentage.
     */
    public int getProgressPercent() {
        if (runbook == null || runbook.getStepCount() == 0) {
            return 0;
        }
        int completedSteps = 0;
        if (stepResults != null) {
            completedSteps = (int) stepResults.stream()
                    .filter(r -> r.getStatus() == StepResult.StepStatus.COMPLETED
                            || r.getStatus() == StepResult.StepStatus.SKIPPED)
                    .count();
        }
        return (completedSteps * 100) / runbook.getStepCount();
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRunbookId() {
        return runbookId;
    }

    public void setRunbookId(Long runbookId) {
        this.runbookId = runbookId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    /**
     * Get a display-friendly scope description.
     *
     * @return database name if set, otherwise "All databases"
     */
    public String getScopeDisplay() {
        return databaseName != null && !databaseName.isEmpty()
                ? databaseName
                : "All databases";
    }

    public Runbook getRunbook() {
        return runbook;
    }

    public void setRunbook(Runbook runbook) {
        this.runbook = runbook;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public TriggeredBy getTriggeredBy() {
        return triggeredBy;
    }

    public void setTriggeredBy(TriggeredBy triggeredBy) {
        this.triggeredBy = triggeredBy;
    }

    public Long getAlertId() {
        return alertId;
    }

    public void setAlertId(Long alertId) {
        this.alertId = alertId;
    }

    public Long getAnomalyId() {
        return anomalyId;
    }

    public void setAnomalyId(Long anomalyId) {
        this.anomalyId = anomalyId;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(int currentStep) {
        this.currentStep = currentStep;
    }

    public List<StepResult> getStepResults() {
        return stepResults;
    }

    public void setStepResults(List<StepResult> stepResults) {
        this.stepResults = stepResults;
    }

    /**
     * Get a specific step result by step number (1-based).
     *
     * @param stepNumber the step number (1-based)
     * @return the step result, or null if not found
     */
    public StepResult getStepResult(int stepNumber) {
        if (stepResults == null || stepNumber < 1 || stepNumber > stepResults.size()) {
            return null;
        }
        return stepResults.get(stepNumber - 1);
    }

    public String getExecutedBy() {
        return executedBy;
    }

    public void setExecutedBy(String executedBy) {
        this.executedBy = executedBy;
    }

    /**
     * Alias for executedBy for template compatibility.
     *
     * @return the user who started/executed this runbook
     */
    public String getStartedBy() {
        return executedBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
