package com.example.lotteryapp;


/**
 * Model for a notification which will be stored in a list
 * that list will be stored in firestore, so it can be accessed.
 */

public class NotificationModel  {

    private String senderAccountID;
    private String receiverAccountID;
    private String message;
    private String timestamp;

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
     * Sets the timestamp of the notification.
     * @param timestamp the timestamp of the notification.
     */
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
