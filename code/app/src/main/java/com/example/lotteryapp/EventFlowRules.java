package com.example.lotteryapp;

import java.util.List;
import java.util.Locale;

public final class EventFlowRules {

    private EventFlowRules() {
    }

    public static String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toLowerCase(Locale.getDefault());
    }

    public static boolean canJoin(String status, boolean isInvitedHost) {
        return !isInvitedHost && normalizeStatus(status).isEmpty();
    }

    public static boolean canLeave(String status) {
        return "waiting".equals(normalizeStatus(status));
    }

    public static boolean canAccept(String status) {
        return "selected".equals(normalizeStatus(status));
    }

    public static boolean canDecline(String status) {
        return "selected".equals(normalizeStatus(status));
    }

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

    public static boolean shouldExportToCsv(String status) {
        return "accepted".equals(normalizeStatus(status));
    }

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
            case "accepted":
                return "Status : REGISTERED";
            case "declined":
                return "Status : DECLINED";
            case "cancelled":
                return "Status : CANCELLED";
            default:
                return "Status : OPEN";
        }
    }
}
