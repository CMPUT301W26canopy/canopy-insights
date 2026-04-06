package com.example.lotteryapp;


/**
 * Represents one notification stored in Firestore.
 * Notifications can optionally point back to a specific event.
 */

public class NotificationModel  {

    private String senderAccountID;
    private String receiverAccountID;
    private String message;
    private String timestamp;

    private String eventId;

    /**
     * Empty constructor for Firestore
     */
    public NotificationModel() {}

    /**
     * Gets the sender's account ID.
     * @return the sender's account ID.
     */
    public String getSenderAccountID() {
        return senderAccountID;
    }

    /**
     * Sets the sender's account ID.
     * @param senderAccountID the sender's account ID.
     */
    public void setSenderAccountID(String senderAccountID) {
        this.senderAccountID = senderAccountID;
    }

    /**
     * Gets the receiver's account ID.
     * @return the receiver's account ID.
     */
    public String getReceiverAccountID() {
        return receiverAccountID;
    }

    /**
     * Sets the receiver's account ID.
     * @param receiverAccountID the receiver's account ID.
     */
    public void setReceiverAccountID(String receiverAccountID) {
        this.receiverAccountID = receiverAccountID;
    }

    /**
     * Gets the notification message.
     * @return the notification message.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the notification message.
     * @param message the notification message.
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Gets the timestamp of the notification.
     * @return the timestamp of the notification.
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the related event ID, if the notification links to an event.
     * @return the event ID or null when the notification is general.
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Sets the related event ID for event-linked notifications.
     * @param eventId the related event ID.
     */
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    /**
     * Sets the timestamp of the notification.
     * @param timestamp the timestamp of the notification.
     */
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
