package com.bovinemagnet.pgconsole.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a compliance score for a specific security area.
 * <p>
 * Used to track and display compliance metrics for various
 * security frameworks like SOC 2 and GDPR.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class ComplianceScore {

    /**
     * Areas of compliance that are assessed.
     */
    public enum ComplianceArea {
        ACCESS_CONTROL("Access Control", "bi-people", "User and role access management"),
        ENCRYPTION("Encryption", "bi-lock", "Data encryption at rest and in transit"),
        AUDIT_LOGGING("Audit Logging", "bi-journal-text", "Activity logging and monitoring"),
        DATA_PROTECTION("Data Protection", "bi-shield-check", "Data classification and protection"),
        AUTHENTICATION("Authentication", "bi-key", "Authentication mechanisms and policies");

        private final String displayName;
        private final String iconClass;
        private final String description;

        ComplianceArea(String displayName, String iconClass, String description) {
            this.displayName = displayName;
            this.iconClass = iconClass;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getIconClass() {
            return iconClass;
        }

        public String getDescription() {
            return description;
        }
    }

    private ComplianceArea area;
    private int score;
    private int maxScore;
    private String description;
    private List<String> passedChecks = new ArrayList<>();
    private List<String> failedChecks = new ArrayList<>();
    private List<String> recommendations = new ArrayList<>();

    /**
     * Default constructor.
     */
    public ComplianceScore() {
    }

    /**
     * Constructs a ComplianceScore with the specified attributes.
     *
     * @param area        the compliance area
     * @param score       the achieved score
     * @param maxScore    the maximum possible score
     * @param description description of the score
     */
    public ComplianceScore(ComplianceArea area, int score, int maxScore, String description) {
        this.area = area;
        this.score = score;
        this.maxScore = maxScore;
        this.description = description;
    }

    // Getters and Setters

    public ComplianceArea getArea() {
        return area;
    }

    public void setArea(ComplianceArea area) {
        this.area = area;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(int maxScore) {
        this.maxScore = maxScore;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getPassedChecks() {
        return passedChecks;
    }

    public void setPassedChecks(List<String> passedChecks) {
        this.passedChecks = passedChecks;
    }

    public List<String> getFailedChecks() {
        return failedChecks;
    }

    public void setFailedChecks(List<String> failedChecks) {
        this.failedChecks = failedChecks;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }

    // Helper Methods

    /**
     * Returns the score as a percentage.
     *
     * @return percentage score (0-100)
     */
    public double getPercentage() {
        if (maxScore == 0) return 0;
        return (score * 100.0) / maxScore;
    }

    /**
     * Returns the CSS class based on the score percentage.
     *
     * @return CSS class for styling
     */
    public String getScoreCssClass() {
        double pct = getPercentage();
        if (pct >= 90) return "bg-success";
        if (pct >= 70) return "bg-info";
        if (pct >= 50) return "bg-warning";
        return "bg-danger";
    }

    /**
     * Returns the CSS class for progress bar colour.
     *
     * @return CSS class for progress bar
     */
    public String getProgressBarCssClass() {
        double pct = getPercentage();
        if (pct >= 90) return "progress-bar bg-success";
        if (pct >= 70) return "progress-bar bg-info";
        if (pct >= 50) return "progress-bar bg-warning";
        return "progress-bar bg-danger";
    }

    /**
     * Returns the score grade (A-F).
     *
     * @return letter grade
     */
    public String getGrade() {
        double pct = getPercentage();
        if (pct >= 90) return "A";
        if (pct >= 80) return "B";
        if (pct >= 70) return "C";
        if (pct >= 60) return "D";
        return "F";
    }

    /**
     * Returns the CSS class for the grade badge.
     *
     * @return CSS class name
     */
    public String getGradeCssClass() {
        String grade = getGrade();
        return switch (grade) {
            case "A" -> "bg-success";
            case "B" -> "bg-info";
            case "C" -> "bg-warning text-dark";
            case "D" -> "bg-warning text-dark";
            default -> "bg-danger";
        };
    }

    /**
     * Returns the score display string.
     *
     * @return score in "X/Y" format
     */
    public String getScoreDisplay() {
        return score + "/" + maxScore;
    }

    /**
     * Returns the percentage display string.
     *
     * @return percentage with % symbol
     */
    public String getPercentageDisplay() {
        return String.format("%.0f%%", getPercentage());
    }

    /**
     * Returns the area display name.
     *
     * @return area name for display
     */
    public String getAreaDisplay() {
        return area != null ? area.getDisplayName() : "Unknown";
    }

    /**
     * Returns the icon class for the area.
     *
     * @return Bootstrap icon class
     */
    public String getAreaIconClass() {
        return area != null ? area.getIconClass() : "bi-question-circle";
    }

    /**
     * Adds a passed check to the list.
     *
     * @param check the check description
     */
    public void addPassedCheck(String check) {
        this.passedChecks.add(check);
    }

    /**
     * Adds a failed check to the list.
     *
     * @param check the check description
     */
    public void addFailedCheck(String check) {
        this.failedChecks.add(check);
    }

    /**
     * Adds a recommendation to the list.
     *
     * @param recommendation the recommendation
     */
    public void addRecommendation(String recommendation) {
        this.recommendations.add(recommendation);
    }

    /**
     * Returns the total number of checks.
     *
     * @return total passed + failed checks
     */
    public int getTotalChecks() {
        return passedChecks.size() + failedChecks.size();
    }

    /**
     * Returns the number of passed checks.
     *
     * @return passed check count
     */
    public int getPassedCount() {
        return passedChecks.size();
    }

    /**
     * Returns the number of failed checks.
     *
     * @return failed check count
     */
    public int getFailedCount() {
        return failedChecks.size();
    }
}
