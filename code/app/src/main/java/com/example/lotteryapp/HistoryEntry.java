package com.example.lotteryapp;

public class HistoryEntry {

    private final String applicationId;
    private final String eventId;
    private String status;
    private String eventName = "Event unavailable";
    private String eventDate = "";
    private String eventLocation = "";
    private boolean updating;

    public HistoryEntry(String applicationId, String eventId, String status) {
        this.applicationId = applicationId;
        this.eventId = eventId;
        this.status = status == null ? "waiting" : status;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getEventId() {
        return eventId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status == null ? "waiting" : status;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getEventDate() {
        return eventDate;
    }

    public void setEventDate(String eventDate) {
        this.eventDate = eventDate;
    }

    public String getEventLocation() {
        return eventLocation;
    }

    public void setEventLocation(String eventLocation) {
        this.eventLocation = eventLocation;
    }

    public boolean isUpdating() {
        return updating;
    }

    public void setUpdating(boolean updating) {
        this.updating = updating;
    }

    public boolean canRespond() {
        return "selected".equals(status);
    }

    public boolean hasEvent() {
        return eventId != null && !eventId.isEmpty();
    }
}
