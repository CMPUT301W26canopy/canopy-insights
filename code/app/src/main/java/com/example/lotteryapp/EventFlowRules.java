package com.example.lotteryapp;

import java.util.List;
import java.util.Locale;

/**
 * Centralizes the small status rules used by the event, history, and organizer screens.
 * Keeping them here helps the app stay consistent across different views.
 */
public final class EventFlowRules {

    private EventFlowRules() {
    }

    /**
     * Returns a normalized lower-case status value for comparisons.
     * @param status The raw status string.
     * @return The normalized, lowercase status string.
     */
    public static String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toLowerCase(Locale.getDefault());
    }

    /**
     * Returns true when the current user can join the event waiting list.
     * @param status The current application status.
     * @param isInvitedHost Whether the user is a co-host.
     * @return True if the user is not a co-host and has no current application.
     */
    public static boolean canJoin(String status, boolean isInvitedHost) {
        return !isInvitedHost && normalizeStatus(status).isEmpty();
    }

    /**
     * Returns true when the current user can leave the waiting list.
     * @param status The current application status.
     * @return True if the status is "waiting".
     */
    public static boolean canLeave(String status) {
        return "waiting".equals(normalizeStatus(status));
    }

    /**
     * Returns true when the current user can accept the current offer.
     * @param status The current application status.
     * @return True if the status is "selected" or "invited".
     */
    public static boolean canAccept(String status) {
        String normalized = normalizeStatus(status);
        return "selected".equals(normalized) || "invited".equals(normalized);
    }

    /**
     * Returns true when the current user can decline the current offer.
     * @param status The current application status.
     * @return True if the status is "selected" or "invited".
     */
    public static boolean canDecline(String status) {
        String normalized = normalizeStatus(status);
        return "selected".equals(normalized) || "invited".equals(normalized);
    }

    /**
     * Returns true once any application has moved past the initial waiting state.
     * @param statuses A list of status strings for all event applications.
     * @return True if at least one status is not "waiting" or empty.
     */
    public static boolean hasLotteryStarted(List<String> statuses) {
        if (statuses == null) {
            return false;
        }

        for (String status : statuses) {
            String normalized = normalizeStatus(status);
            if (!normalized.isEmpty() && !"waiting".equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true when there are selected entrants who still need to respond.
     * @param statuses A list of status strings for all event applications.
     * @return True if at least one application has a "selected" status.
     */
    public static boolean hasPendingSelected(List<String> statuses) {
        if (statuses == null) {
            return false;
        }

        for (String status : statuses) {
            if ("selected".equals(normalizeStatus(status))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Counts the spots currently occupied by selected or accepted entrants.
     * @param statuses A list of status strings for all event applications.
     * @return The total number of "selected" or "accepted" applications.
     */
    public static int countOccupiedSpots(List<String> statuses) {
        if (statuses == null) {
            return 0;
        }

        int occupied = 0;
        for (String status : statuses) {
            String normalized = normalizeStatus(status);
            if ("accepted".equals(normalized) || "selected".equals(normalized)) {
                occupied++;
            }
        }
        return occupied;
    }

    /**
     * Returns true when an application should be included in the final CSV export.
     * @param status The current application status.
     * @return True if the status is "accepted".
     */
    public static boolean shouldExportToCsv(String status) {
        return "accepted".equals(normalizeStatus(status));
    }

    /**
     * Returns the history-friendly label for an application status.
     * @param rawStatus The raw status string from Firestore.
     * @return A human-readable label suitable for the history screen.
     */
    public static String getHistoryStatusLabel(String rawStatus) {
        String normalized = normalizeStatus(rawStatus);
        if (normalized.isEmpty()) {
            return "REGISTERED";
        }

        switch (normalized) {
            case "waiting":
                return "WAITING LIST";
            case "selected":
                return "SELECTED";
            case "invited":
                return "INVITED";
            case "accepted":
            case "registered":
                return "REGISTERED";
            case "attended":
                return "ATTENDED";
            case "declined":
                return "DECLINED";
            case "cancelled":
                return "CANCELLED";
            case "completed":
                return "COMPLETED";
            default:
                return rawStatus.trim().toUpperCase(Locale.getDefault());
        }
    }

    /**
     * Returns the event screen label for the current user's state.
     * @param status The current application status.
     * @param isInvitedHost Whether the user is a co-host.
     * @return A formatted string showing the user's current relationship to the event.
     */
    public static String getEventStatusLabel(String status, boolean isInvitedHost) {
        if (isInvitedHost) {
            return "Status : CO-HOST";
        }

        String normalized = normalizeStatus(status);
        switch (normalized) {
            case "waiting":
                return "Status : WAITING LIST";
            case "selected":
                return "Status : SELECTED";
            case "invited":
                return "Status : PRIVATE INVITE";
            case "accepted":
                return "Status : REGISTERED";
            case "declined":
                return "Status : DECLINED";
            case "cancelled":
                return "Status : CANCELLED";
            case "invite_declined":
                return "Status : INVITE DECLINED";
            default:
                return "Status : OPEN";
        }
    }
}
