package com.bovinemagnet.pgconsole.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a compliance score for a specific security area within PostgreSQL database monitoring.
 * <p>
 * This class encapsulates compliance metrics used to assess and track adherence to security
 * frameworks such as SOC 2, GDPR, and other regulatory requirements. Each instance represents
 * a single compliance area (e.g., access control, encryption) with associated scores, checks,
 * and recommendations.
 * <p>
 * The scoring system tracks:
 * <ul>
 *   <li>Achieved score vs. maximum possible score</li>
 *   <li>Lists of passed and failed compliance checks</li>
 *   <li>Actionable recommendations for improving compliance</li>
 *   <li>Visual indicators (grades, CSS classes) for UI rendering</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>{@code
 * ComplianceScore score = new ComplianceScore(
 *     ComplianceArea.ENCRYPTION,
 *     85,
 *     100,
 *     "SSL/TLS encryption status"
 * );
 * score.addPassedCheck("SSL connections enabled");
 * score.addFailedCheck("Weak cipher suites detected");
 * score.addRecommendation("Update to TLS 1.3 or higher");
 *
 * String grade = score.getGrade(); // Returns "B"
 * double percentage = score.getPercentage(); // Returns 85.0
 * }</pre>
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see ComplianceArea
 */
public class ComplianceScore {

    /**
     * Enumeration of compliance areas that are assessed for PostgreSQL security monitoring.
     * <p>
     * Each area represents a distinct aspect of database security and compliance, with
     * associated display metadata including a human-readable name, Bootstrap icon class,
     * and descriptive text explaining the area's scope.
     */
    public enum ComplianceArea {
        /** User and role access management compliance area. */
        ACCESS_CONTROL("Access Control", "bi-people", "User and role access management"),

        /** Data encryption at rest and in transit compliance area. */
        ENCRYPTION("Encryption", "bi-lock", "Data encryption at rest and in transit"),

        /** Activity logging and monitoring compliance area. */
        AUDIT_LOGGING("Audit Logging", "bi-journal-text", "Activity logging and monitoring"),

        /** Data classification and protection compliance area. */
        DATA_PROTECTION("Data Protection", "bi-shield-check", "Data classification and protection"),

        /** Authentication mechanisms and policies compliance area. */
        AUTHENTICATION("Authentication", "bi-key", "Authentication mechanisms and policies");

        /** The human-readable display name for this compliance area. */
        private final String displayName;

        /** The Bootstrap icon class (e.g., "bi-lock") for UI rendering. */
        private final String iconClass;

        /** A brief description of what this compliance area covers. */
        private final String description;

        /**
         * Constructs a ComplianceArea with display metadata.
         *
         * @param displayName the human-readable name for display
         * @param iconClass   the Bootstrap icon class for UI rendering
         * @param description a brief description of the compliance area
         */
        ComplianceArea(String displayName, String iconClass, String description) {
            this.displayName = displayName;
            this.iconClass = iconClass;
            this.description = description;
        }

        /**
         * Returns the human-readable display name for this compliance area.
         *
         * @return the display name (e.g., "Access Control")
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Returns the Bootstrap icon class for this compliance area.
         *
         * @return the icon class (e.g., "bi-people", "bi-lock")
         */
        public String getIconClass() {
            return iconClass;
        }

        /**
         * Returns a brief description of what this compliance area covers.
         *
         * @return the description text
         */
        public String getDescription() {
            return description;
        }
    }

    /** The compliance area being assessed (e.g., ENCRYPTION, ACCESS_CONTROL). */
    private ComplianceArea area;

    /** The achieved compliance score for this area. */
    private int score;

    /** The maximum possible score for this compliance area. */
    private int maxScore;

    /** A human-readable description of this compliance score assessment. */
    private String description;

    /** List of compliance checks that passed successfully. */
    private List<String> passedChecks = new ArrayList<>();

    /** List of compliance checks that failed. */
    private List<String> failedChecks = new ArrayList<>();

    /** List of recommendations for improving compliance in this area. */
    private List<String> recommendations = new ArrayList<>();

    /**
     * Constructs a new ComplianceScore with default values.
     * <p>
     * All fields are initialised to their default values. The passed checks,
     * failed checks, and recommendations lists are initialised as empty lists.
     */
    public ComplianceScore() {
    }

    /**
     * Constructs a ComplianceScore with the specified attributes.
     * <p>
     * The passed checks, failed checks, and recommendations lists are
     * initialised as empty lists and can be populated using the
     * {@link #addPassedCheck(String)}, {@link #addFailedCheck(String)},
     * and {@link #addRecommendation(String)} methods.
     *
     * @param area        the compliance area being assessed
     * @param score       the achieved score (should be between 0 and maxScore)
     * @param maxScore    the maximum possible score for this area
     * @param description a human-readable description of the assessment
     */
    public ComplianceScore(ComplianceArea area, int score, int maxScore, String description) {
        this.area = area;
        this.score = score;
        this.maxScore = maxScore;
        this.description = description;
    }

    // Getters and Setters

    /**
     * Returns the compliance area being assessed.
     *
     * @return the compliance area, or null if not set
     */
    public ComplianceArea getArea() {
        return area;
    }

    /**
     * Sets the compliance area being assessed.
     *
     * @param area the compliance area to set
     */
    public void setArea(ComplianceArea area) {
        this.area = area;
    }

    /**
     * Returns the achieved compliance score.
     *
     * @return the current score
     */
    public int getScore() {
        return score;
    }

    /**
     * Sets the achieved compliance score.
     *
     * @param score the score to set (should be between 0 and maxScore)
     */
    public void setScore(int score) {
        this.score = score;
    }

    /**
     * Returns the maximum possible score for this compliance area.
     *
     * @return the maximum score
     */
    public int getMaxScore() {
        return maxScore;
    }

    /**
     * Sets the maximum possible score for this compliance area.
     *
     * @param maxScore the maximum score to set (should be positive)
     */
    public void setMaxScore(int maxScore) {
        this.maxScore = maxScore;
    }

    /**
     * Returns the human-readable description of this assessment.
     *
     * @return the description text, or null if not set
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the human-readable description of this assessment.
     *
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the list of compliance checks that passed successfully.
     * <p>
     * The returned list is mutable and can be modified directly, though
     * it is recommended to use {@link #addPassedCheck(String)} instead.
     *
     * @return the list of passed checks (never null)
     */
    public List<String> getPassedChecks() {
        return passedChecks;
    }

    /**
     * Sets the list of compliance checks that passed successfully.
     *
     * @param passedChecks the list of passed checks to set (must not be null)
     */
    public void setPassedChecks(List<String> passedChecks) {
        this.passedChecks = passedChecks;
    }

    /**
     * Returns the list of compliance checks that failed.
     * <p>
     * The returned list is mutable and can be modified directly, though
     * it is recommended to use {@link #addFailedCheck(String)} instead.
     *
     * @return the list of failed checks (never null)
     */
    public List<String> getFailedChecks() {
        return failedChecks;
    }

    /**
     * Sets the list of compliance checks that failed.
     *
     * @param failedChecks the list of failed checks to set (must not be null)
     */
    public void setFailedChecks(List<String> failedChecks) {
        this.failedChecks = failedChecks;
    }

    /**
     * Returns the list of recommendations for improving compliance.
     * <p>
     * The returned list is mutable and can be modified directly, though
     * it is recommended to use {@link #addRecommendation(String)} instead.
     *
     * @return the list of recommendations (never null)
     */
    public List<String> getRecommendations() {
        return recommendations;
    }

    /**
     * Sets the list of recommendations for improving compliance.
     *
     * @param recommendations the list of recommendations to set (must not be null)
     */
    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }

    // Helper Methods

    /**
     * Calculates and returns the compliance score as a percentage.
     * <p>
     * The percentage is calculated as {@code (score / maxScore) * 100}.
     * If maxScore is zero, this method returns 0 to avoid division by zero.
     *
     * @return the percentage score as a double value between 0.0 and 100.0
     */
    public double getPercentage() {
        if (maxScore == 0) return 0;
        return (score * 100.0) / maxScore;
    }

    /**
     * Returns the appropriate Bootstrap CSS background class based on the score percentage.
     * <p>
     * The colour scale is:
     * <ul>
     *   <li>90-100%: "bg-success" (green)</li>
     *   <li>70-89%: "bg-info" (blue)</li>
     *   <li>50-69%: "bg-warning" (yellow)</li>
     *   <li>0-49%: "bg-danger" (red)</li>
     * </ul>
     *
     * @return the Bootstrap CSS class for background colour styling
     * @see #getPercentage()
     */
    public String getScoreCssClass() {
        double pct = getPercentage();
        if (pct >= 90) return "bg-success";
        if (pct >= 70) return "bg-info";
        if (pct >= 50) return "bg-warning";
        return "bg-danger";
    }

    /**
     * Returns the appropriate Bootstrap CSS classes for progress bar styling.
     * <p>
     * This combines the base "progress-bar" class with a colour class
     * based on the score percentage. The colour scale matches
     * {@link #getScoreCssClass()}.
     *
     * @return the complete CSS class string for progress bar elements
     * @see #getScoreCssClass()
     */
    public String getProgressBarCssClass() {
        double pct = getPercentage();
        if (pct >= 90) return "progress-bar bg-success";
        if (pct >= 70) return "progress-bar bg-info";
        if (pct >= 50) return "progress-bar bg-warning";
        return "progress-bar bg-danger";
    }

    /**
     * Calculates and returns the letter grade (A-F) based on the score percentage.
     * <p>
     * Grading scale:
     * <ul>
     *   <li>A: 90-100%</li>
     *   <li>B: 80-89%</li>
     *   <li>C: 70-79%</li>
     *   <li>D: 60-69%</li>
     *   <li>F: 0-59%</li>
     * </ul>
     *
     * @return a single character string representing the letter grade
     * @see #getPercentage()
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
     * Returns the appropriate Bootstrap CSS classes for styling a grade badge.
     * <p>
     * The colour corresponds to the grade:
     * <ul>
     *   <li>A: "bg-success" (green)</li>
     *   <li>B: "bg-info" (blue)</li>
     *   <li>C: "bg-warning text-dark" (yellow with dark text)</li>
     *   <li>D: "bg-warning text-dark" (yellow with dark text)</li>
     *   <li>F: "bg-danger" (red)</li>
     * </ul>
     *
     * @return the CSS class string for badge styling
     * @see #getGrade()
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
     * Returns a formatted string representation of the score.
     * <p>
     * The format is "X/Y" where X is the achieved score and Y is the maximum score.
     * For example: "85/100" or "42/50".
     *
     * @return the score formatted as "achieved/maximum"
     */
    public String getScoreDisplay() {
        return score + "/" + maxScore;
    }

    /**
     * Returns a formatted string representation of the percentage score.
     * <p>
     * The percentage is rounded to the nearest whole number and includes
     * the percent symbol. For example: "85%" or "42%".
     *
     * @return the percentage formatted with no decimal places and a % symbol
     * @see #getPercentage()
     */
    public String getPercentageDisplay() {
        return String.format("%.0f%%", getPercentage());
    }

    /**
     * Returns the display name for the compliance area.
     * <p>
     * If the area is null, returns "Unknown" as a fallback value.
     *
     * @return the human-readable area name, or "Unknown" if area is null
     * @see ComplianceArea#getDisplayName()
     */
    public String getAreaDisplay() {
        return area != null ? area.getDisplayName() : "Unknown";
    }

    /**
     * Returns the Bootstrap icon class for the compliance area.
     * <p>
     * If the area is null, returns "bi-question-circle" as a fallback icon.
     *
     * @return the Bootstrap icon class name, or "bi-question-circle" if area is null
     * @see ComplianceArea#getIconClass()
     */
    public String getAreaIconClass() {
        return area != null ? area.getIconClass() : "bi-question-circle";
    }

    /**
     * Adds a passed compliance check to the list of successful checks.
     * <p>
     * This method is used to track individual compliance requirements
     * that have been successfully met. The check description should be
     * concise and meaningful for display purposes.
     *
     * @param check a human-readable description of the passed check
     * @see #getPassedChecks()
     * @see #getPassedCount()
     */
    public void addPassedCheck(String check) {
        this.passedChecks.add(check);
    }

    /**
     * Adds a failed compliance check to the list of unsuccessful checks.
     * <p>
     * This method is used to track individual compliance requirements
     * that have not been met. The check description should clearly
     * indicate what requirement failed for diagnostic purposes.
     *
     * @param check a human-readable description of the failed check
     * @see #getFailedChecks()
     * @see #getFailedCount()
     */
    public void addFailedCheck(String check) {
        this.failedChecks.add(check);
    }

    /**
     * Adds a recommendation to the list of suggested improvements.
     * <p>
     * Recommendations provide actionable guidance for improving the
     * compliance score in this area. They are typically generated based
     * on failed checks or potential security improvements.
     *
     * @param recommendation a human-readable recommendation for improving compliance
     * @see #getRecommendations()
     */
    public void addRecommendation(String recommendation) {
        this.recommendations.add(recommendation);
    }

    /**
     * Returns the total number of compliance checks performed.
     * <p>
     * This is the sum of both passed and failed checks. It represents
     * the total number of individual requirements assessed for this
     * compliance area.
     *
     * @return the total count of all checks (passed + failed)
     * @see #getPassedCount()
     * @see #getFailedCount()
     */
    public int getTotalChecks() {
        return passedChecks.size() + failedChecks.size();
    }

    /**
     * Returns the number of compliance checks that passed successfully.
     *
     * @return the count of passed checks
     * @see #getPassedChecks()
     * @see #getTotalChecks()
     */
    public int getPassedCount() {
        return passedChecks.size();
    }

    /**
     * Returns the number of compliance checks that failed.
     *
     * @return the count of failed checks
     * @see #getFailedChecks()
     * @see #getTotalChecks()
     */
    public int getFailedCount() {
        return failedChecks.size();
    }
}
