package com.bovinemagnet.pgconsole.model;

/**
 * Summary of wait events aggregated from PostgreSQL's {@code pg_stat_activity} system view.
 * <p>
 * This model represents a grouped view of database sessions categorised by their wait event type
 * and specific wait event. Wait events indicate what a backend process is currently waiting for,
 * providing insight into performance bottlenecks and resource contention within the database.
 * <p>
 * Wait events are classified into categories (wait event types) such as Lock, IO, Client, and
 * Activity. A {@code null} wait event type indicates an actively executing query that is not
 * waiting on any resource.
 * <p>
 * This class provides human-readable display names and CSS class mappings for rendering wait
 * event summaries in dashboard views.
 *
 * @author Paul Snow
 * @version 0.0.0
 * @see <a href="https://www.postgresql.org/docs/current/monitoring-stats.html#WAIT-EVENT-TABLE">PostgreSQL Wait Events</a>
 */
public class WaitEventSummary {

    /**
     * The category of the wait event (e.g., "Lock", "IO", "Client", "Activity").
     * A {@code null} value indicates an active session not waiting on any resource.
     */
    private String waitEventType;

    /**
     * The specific wait event within the category (e.g., "relation", "transactionid", "ClientRead").
     * May be {@code null} for active sessions.
     */
    private String waitEvent;

    /**
     * The number of database sessions currently experiencing this wait event.
     */
    private int sessionCount;

    /**
     * Optional human-readable description of the wait event.
     * May be {@code null} if no description is available.
     */
    private String description;

    /**
     * Constructs an empty WaitEventSummary.
     * All fields will be initialised to their default values.
     */
    public WaitEventSummary() {
    }

    /**
     * Constructs a WaitEventSummary with the specified wait event details.
     *
     * @param waitEventType the category of the wait event, or {@code null} for active sessions
     * @param waitEvent     the specific wait event name, or {@code null} for active sessions
     * @param sessionCount  the number of sessions experiencing this wait event
     */
    public WaitEventSummary(String waitEventType, String waitEvent, int sessionCount) {
        this.waitEventType = waitEventType;
        this.waitEvent = waitEvent;
        this.sessionCount = sessionCount;
    }

    /**
     * Returns the wait event type category.
     *
     * @return the wait event type, or {@code null} for active sessions not waiting
     */
    public String getWaitEventType() {
        return waitEventType;
    }

    /**
     * Sets the wait event type category.
     *
     * @param waitEventType the wait event type, or {@code null} for active sessions
     */
    public void setWaitEventType(String waitEventType) {
        this.waitEventType = waitEventType;
    }

    /**
     * Returns the specific wait event name.
     *
     * @return the wait event, or {@code null} for active sessions not waiting
     */
    public String getWaitEvent() {
        return waitEvent;
    }

    /**
     * Sets the specific wait event name.
     *
     * @param waitEvent the wait event, or {@code null} for active sessions
     */
    public void setWaitEvent(String waitEvent) {
        this.waitEvent = waitEvent;
    }

    /**
     * Returns the number of sessions experiencing this wait event.
     *
     * @return the session count
     */
    public int getSessionCount() {
        return sessionCount;
    }

    /**
     * Sets the number of sessions experiencing this wait event.
     *
     * @param sessionCount the session count
     */
    public void setSessionCount(int sessionCount) {
        this.sessionCount = sessionCount;
    }

    /**
     * Returns the human-readable description of this wait event.
     *
     * @return the description, or {@code null} if no description is available
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the human-readable description of this wait event.
     *
     * @param description the description, or {@code null} if not applicable
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns a human-readable display name for the wait event type.
     * <p>
     * Translates PostgreSQL's internal wait event type names into more descriptive labels
     * suitable for display in dashboards and reports. Common mappings include:
     * <ul>
     *   <li>{@code null} → "None (Active)" - actively executing query</li>
     *   <li>"Activity" → "Activity (Idle)" - idle session</li>
     *   <li>"Lock" → "Lock Acquisition" - waiting to acquire a lock</li>
     *   <li>"IO" → "I/O Operations" - waiting on disk I/O</li>
     *   <li>"Client" → "Client Communication" - waiting for client response</li>
     * </ul>
     * <p>
     * Unknown wait event types are returned unchanged.
     *
     * @return the human-readable display name for the wait event type
     */
    public String getWaitEventTypeDisplay() {
        if (waitEventType == null) {
            return "None (Active)";
        }
        return switch (waitEventType) {
            case "Activity" -> "Activity (Idle)";
            case "BufferPin" -> "Buffer Pin";
            case "Client" -> "Client Communication";
            case "Extension" -> "Extension";
            case "IO" -> "I/O Operations";
            case "IPC" -> "Inter-Process Communication";
            case "Lock" -> "Lock Acquisition";
            case "LWLock" -> "Lightweight Lock";
            case "Timeout" -> "Timeout";
            default -> waitEventType;
        };
    }

    /**
     * Returns the Bootstrap CSS class name for colour-coding the wait event type.
     * <p>
     * Provides visual categorisation of wait events in dashboard views using Bootstrap 5
     * background colour classes. The colour scheme reflects the severity and nature of each
     * wait event type:
     * <ul>
     *   <li>{@code null} (Active) → "bg-success" (green) - good, actively processing</li>
     *   <li>"Lock" → "bg-danger" (red) - concerning, lock contention</li>
     *   <li>"LWLock" → "bg-warning text-dark" (yellow) - moderate concern</li>
     *   <li>"IO" → "bg-info" (blue) - informational, I/O bound</li>
     *   <li>"Client" → "bg-secondary" (grey) - neutral, waiting on client</li>
     *   <li>"Activity" → "bg-light text-dark" (light grey) - idle session</li>
     *   <li>Other types → "bg-primary" (default blue)</li>
     * </ul>
     *
     * @return the Bootstrap CSS class name for styling this wait event type
     */
    public String getWaitEventTypeCssClass() {
        if (waitEventType == null) {
            return "bg-success";
        }
        return switch (waitEventType) {
            case "Lock" -> "bg-danger";
            case "LWLock" -> "bg-warning text-dark";
            case "IO" -> "bg-info";
            case "Client" -> "bg-secondary";
            case "Activity" -> "bg-light text-dark";
            default -> "bg-primary";
        };
    }
}
