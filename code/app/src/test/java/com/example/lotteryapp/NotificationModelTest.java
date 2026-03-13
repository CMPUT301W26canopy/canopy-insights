package com.example.lotteryapp;


import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests our Notification models methods
 */
public class NotificationModelTest {
    


    @Test
    public void testSetAndGetSenderAccountID() {
        NotificationModel notification = new NotificationModel();
        String senderID = "A sender";
        notification.setSenderAccountID(senderID);
        assertEquals(senderID, notification.getSenderAccountID());
    }

    @Test
    public void testSetAndGetReceiverAccountID() {
        NotificationModel notification = new NotificationModel();
        String receiverID = "the receiver";
        notification.setReceiverAccountID(receiverID);
        assertEquals(receiverID, notification.getReceiverAccountID());
    }

    @Test
    public void testSetAndGetMessage() {
        NotificationModel notification = new NotificationModel();
        String message = "Notification message";
        notification.setMessage(message);
        assertEquals(message, notification.getMessage());
    }

    @Test
    public void testSetAndGetTimestamp() {
        NotificationModel notification = new NotificationModel();
        String timestamp = "2026-13-02 13:29:01";
        notification.setTimestamp(timestamp);
        assertEquals(timestamp, notification.getTimestamp());
    }

}
