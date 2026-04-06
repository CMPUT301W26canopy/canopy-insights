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

    public static Task<Void> notifyWaitingList(String organizerId, String eventId,
                                               String eventName, List<String> userIds) {
        return sendNotifications(organizerId, userIds,
                "You were not selected for " + eventName +
                        " this round. You are still on the waiting list.", eventId);
    }

    public static Task<Void> notifySelected(String organizerId, String eventId,
                                            String eventName, List<String> userIds) {
        return sendNotifications(organizerId, userIds,
                "You have been selected for " + eventName +
                        ". Open the event to accept or decline.", eventId);
    }

    public static Task<Void> notifyCancelled(String organizerId, String eventId,
                                             String eventName, List<String> userIds) {
        return sendNotifications(organizerId, userIds,
                "Your invitation for " + eventName + " has been cancelled.", eventId);
    }

    public static Task<Void> sendCustomNotification(String senderId, String eventId, String eventName, List<String> userIds, String message) {
        return sendNotifications(senderId, userIds, "[" + eventName + "] " + message, eventId);
    }

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
