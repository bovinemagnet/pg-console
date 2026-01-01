package com.bovinemagnet.pgconsole.model;

/**
 * Summary of wait events aggregated from pg_stat_activity.
 *
 * @author Paul Snow
 * @version 0.0.0
 */
public class WaitEventSummary {

    private String waitEventType;
    private String waitEvent;
    private int sessionCount;
    private String description;

    public WaitEventSummary() {
    }

    public WaitEventSummary(String waitEventType, String waitEvent, int sessionCount) {
        this.waitEventType = waitEventType;
        this.waitEvent = waitEvent;
        this.sessionCount = sessionCount;
    }

    public String getWaitEventType() {
        return waitEventType;
    }

    public void setWaitEventType(String waitEventType) {
        this.waitEventType = waitEventType;
    }

    public String getWaitEvent() {
        return waitEvent;
    }

    public void setWaitEvent(String waitEvent) {
        this.waitEvent = waitEvent;
    }

    public int getSessionCount() {
        return sessionCount;
    }

    public void setSessionCount(int sessionCount) {
        this.sessionCount = sessionCount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the display name for the wait event type.
     *
     * @return display name
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
     * Returns the CSS class for colour-coding the wait event type.
     *
     * @return CSS class name
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
