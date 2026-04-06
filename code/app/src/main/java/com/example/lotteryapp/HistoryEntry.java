package com.example.lotteryapp;

/**
 * Represents one row in the entrant history screen.
 * It combines event details with the status from the related application.
 */
public class HistoryEntry {

    private final String applicationId;
    private final String eventId;
    private String status;
    private String eventName = "Event unavailable";
    private String eventDate = "";
    private String eventLocation = "";
    private boolean updating;

    /**
     * Creates a history entry from an application and its current status.
     */
    public HistoryEntry(String applicationId, String eventId, String status) {
        this.applicationId = applicationId;
        this.eventId = eventId;
        this.status = status == null ? "waiting" : status;
    }

    /**
     * Returns the application document ID behind this row.
     */
    public String getApplicationId() {
        return applicationId;
    }

    /**
     * Returns the related event ID when one is available.
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Returns the current application status.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the current application status.
     */
    public void setStatus(String status) {
        this.status = status == null ? "waiting" : status;
    }

    /**
     * Returns the event name shown in history.
     */
    public String getEventName() {
        return eventName;
    }

    /**
     * Sets the event name shown in history.
     */
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    /**
     * Returns the event date shown in history.
     */
    public String getEventDate() {
        return eventDate;
    }

    /**
     * Sets the event date shown in history.
     */
    public void setEventDate(String eventDate) {
        this.eventDate = eventDate;
    }

    /**
     * Returns the event location shown in history.
     */
    public String getEventLocation() {
        return eventLocation;
    }

    /**
     * Sets the event location shown in history.
     */
    public void setEventLocation(String eventLocation) {
        this.eventLocation = eventLocation;
    }

    /**
     * Returns whether the row is currently submitting an action.
     */
    public boolean isUpdating() {
        return updating;
    }

    /**
     * Sets whether the row is currently submitting an action.
     */
    public void setUpdating(boolean updating) {
        this.updating = updating;
    }

    /**
     * Returns true when the entrant can accept or decline from history.
     */
    public boolean canRespond() {
        return "selected".equals(status);
    }

    /**
     * Returns true when the row still points to a valid event.
     */
    public boolean hasEvent() {
        return eventId != null && !eventId.isEmpty();
    }
}
