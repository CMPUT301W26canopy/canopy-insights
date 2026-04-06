package com.example.lotteryapp;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Handles writing notifications to Firestore using one shared app schema.
 */
public class NotificationHelper {

    private NotificationHelper() {}

    /**
     * Sends notifications to a list of receiver account IDs.
     * Checks each recipient's preferences before writing the notification to Firestore.
     * @param senderAccountID The ID of the account sending the notification.
     * @param receiverIds The list of account IDs to receive the notification.
     * @param message The content of the notification.
     * @param eventId The optional ID of the related event.
     * @return A {@link Task} that completes when all notifications have been processed.
     */
    public static Task<Void> sendNotifications(String senderAccountID,
                                               List<String> receiverIds,
                                               String message,
                                               String eventId) {
        List<String> normalizedReceiverIds = normalizeReceiverIds(receiverIds);
        if (normalizedReceiverIds.isEmpty()) {
            return Tasks.forResult(null);
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());

        List<Task<Void>> tasks = new ArrayList<>();

        for (String receiverId : normalizedReceiverIds) {
            Task<Void> task = FirestoreHelper.getDb()
                    .collection("accounts")
                    .document(receiverId)
                    .get()
                    .continueWithTask(accountTask -> {
                        if (!accountTask.isSuccessful()) {
                            Exception exception = accountTask.getException();
                            return Tasks.forException(exception != null
                                    ? exception
                                    : new IllegalStateException("Failed to read notification recipient"));
                        }

                        if (accountTask.getResult() != null) {
                            ProfileModel profile = accountTask.getResult().toObject(ProfileModel.class);
                            if (profile != null && !profile.isNotificationEnabled()) {
                                return Tasks.forResult(null);
                            }
                        }

                        Map<String, Object> notif = new HashMap<>();
                        notif.put("senderAccountID", senderAccountID);
                        notif.put("receiverAccountID", receiverId);
                        notif.put("message", message);
                        notif.put("eventId", eventId);
                        notif.put("timestamp", timestamp);

                        return FirestoreHelper.getDb()
                                .collection("notifications")
                                .add(notif)
                                .continueWithTask(writeTask -> {
                                    if (!writeTask.isSuccessful()) {
                                        Exception exception = writeTask.getException();
                                        return Tasks.forException(exception != null
                                                ? exception
                                                : new IllegalStateException("Failed to write notification"));
                                    }
                                    return Tasks.forResult(null);
                                });
                    });
            tasks.add(task);
        }

        return Tasks.whenAll(tasks);
    }

    /**
     * Sends a standard notification to users who remain on the waiting list.
     * @param organizerId The ID of the organizer.
     * @param eventId The ID of the event.
     * @param eventName The name of the event.
     * @param userIds The list of user IDs to notify.
     * @return A {@link Task} for the asynchronous operation.
     */
    public static Task<Void> notifyWaitingList(String organizerId, String eventId,
                                               String eventName, List<String> userIds) {
        return sendNotifications(organizerId, userIds,
                "You were not selected for " + eventName +
                        " this round. You are still on the waiting list.", eventId);
    }

    /**
     * Sends a notification to users who have been selected in the lottery.
     * @param organizerId The ID of the organizer.
     * @param eventId The ID of the event.
     * @param eventName The name of the event.
     * @param userIds The list of user IDs to notify.
     * @return A {@link Task} for the asynchronous operation.
     */
    public static Task<Void> notifySelected(String organizerId, String eventId,
                                            String eventName, List<String> userIds) {
        return sendNotifications(organizerId, userIds,
                "You have been selected for " + eventName +
                        ". Open the event to accept or decline.", eventId);
    }

    /**
     * Sends a notification to users whose invitation has been cancelled.
     * @param organizerId The ID of the organizer.
     * @param eventId The ID of the event.
     * @param eventName The name of the event.
     * @param userIds The list of user IDs to notify.
     * @return A {@link Task} for the asynchronous operation.
     */
    public static Task<Void> notifyCancelled(String organizerId, String eventId,
                                             String eventName, List<String> userIds) {
        return sendNotifications(organizerId, userIds,
                "Your invitation for " + eventName + " has been cancelled.", eventId);
    }

    /**
     * Sends a custom notification with a user-provided message.
     * @param senderId The ID of the sender.
     * @param eventId The ID of the event.
     * @param eventName The name of the event.
     * @param userIds The list of user IDs to notify.
     * @param message The custom message content.
     * @return A {@link Task} for the asynchronous operation.
     */
    public static Task<Void> sendCustomNotification(String senderId, String eventId, String eventName, List<String> userIds, String message) {
        return sendNotifications(senderId, userIds, "[" + eventName + "] " + message, eventId);
    }

    /**
     * Cleans and deduplicates a list of receiver account IDs.
     * @param receiverIds The raw list of receiver IDs.
     * @return A list of unique, non-empty, and trimmed receiver IDs.
     */
    private static List<String> normalizeReceiverIds(List<String> receiverIds) {
        if (receiverIds == null || receiverIds.isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> uniqueIds = new LinkedHashSet<>();
        for (String receiverId : receiverIds) {
            if (receiverId == null) {
                continue;
            }

            String trimmed = receiverId.trim();
            if (!trimmed.isEmpty()) {
                uniqueIds.add(trimmed);
            }
        }
        return new ArrayList<>(uniqueIds);
    }
}
