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
     * @param applicationId The unique ID of the application document.
     * @param eventId The unique ID of the related event.
     * @param status The initial status of the application.
     */
    public HistoryEntry(String applicationId, String eventId, String status) {
        this.applicationId = applicationId;
        this.eventId = eventId;
        this.status = status == null ? "waiting" : status;
    }

    /**
     * Returns the application document ID behind this row.
     * @return The application document ID.
     */
    public String getApplicationId() {
        return applicationId;
    }

    /**
     * Returns the related event ID when one is available.
     * @return The related event ID.
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Returns the current application status.
     * @return The current status string.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the current application status.
     * @param status The new status string to set.
     */
    public void setStatus(String status) {
        this.status = status == null ? "waiting" : status;
    }

    /**
     * Returns the event name shown in history.
     * @return The event name.
     */
    public String getEventName() {
        return eventName;
    }

    /**
     * Sets the event name shown in history.
     * @param eventName The event name to display.
     */
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    /**
     * Returns the event date shown in history.
     * @return The event date string.
     */
    public String getEventDate() {
        return eventDate;
    }

    /**
     * Sets the event date shown in history.
     * @param eventDate The event date string to display.
     */
    public void setEventDate(String eventDate) {
        this.eventDate = eventDate;
    }

    /**
     * Returns the event location shown in history.
     * @return The event location.
     */
    public String getEventLocation() {
        return eventLocation;
    }

    /**
     * Sets the event location shown in history.
     * @param eventLocation The event location to display.
     */
    public void setEventLocation(String eventLocation) {
        this.eventLocation = eventLocation;
    }

    /**
     * Returns whether the row is currently submitting an action.
     * @return True if an update is in progress.
     */
    public boolean isUpdating() {
        return updating;
    }

    /**
     * Sets whether the row is currently submitting an action.
     * @param updating True to mark the row as updating.
     */
    public void setUpdating(boolean updating) {
        this.updating = updating;
    }

    /**
     * Returns true when the entrant can accept or decline from history.
     * @return True if the status is "selected".
     */
    public boolean canRespond() {
        return "selected".equals(status);
    }

    /**
     * Returns true when the row still points to a valid event.
     * @return True if eventId is non-null and non-empty.
     */
    public boolean hasEvent() {
        return eventId != null && !eventId.isEmpty();
    }
}
