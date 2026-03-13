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

    // Empty constructor for Firestore
    public NotificationModel() {}

    public String getSenderAccountID() {
        return senderAccountID;
    }

    public void setSenderAccountID(String senderAccountID) {
        this.senderAccountID = senderAccountID;
    }

    public String getReceiverAccountID() {
        return receiverAccountID;
    }

    public void setReceiverAccountID(String receiverAccountID) {
        this.receiverAccountID = receiverAccountID;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
