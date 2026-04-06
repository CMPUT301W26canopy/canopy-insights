package com.example.lotteryapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;

public class EventFlowRulesTest {

    @Test
    public void selectedEntrant_canAcceptAndDecline() {
        assertTrue(EventFlowRules.canAccept("selected"));
        assertTrue(EventFlowRules.canDecline("selected"));
        assertFalse(EventFlowRules.canJoin("selected", false));
        assertFalse(EventFlowRules.canLeave("selected"));
    }

    @Test
    public void waitingEntrant_canLeaveButCannotAcceptOrDecline() {
        assertTrue(EventFlowRules.canLeave("waiting"));
        assertFalse(EventFlowRules.canAccept("waiting"));
        assertFalse(EventFlowRules.canDecline("waiting"));
        assertFalse(EventFlowRules.canJoin("waiting", false));
    }

    @Test
    public void openEntrant_canJoinWhenNotInvitedHost() {
        assertTrue(EventFlowRules.canJoin("", false));
        assertTrue(EventFlowRules.canJoin(null, false));
        assertFalse(EventFlowRules.canJoin("", true));
    }

    @Test
    public void lotteryStarted_whenAnyStatusHasMovedPastWaiting() {
        assertFalse(EventFlowRules.hasLotteryStarted(Arrays.asList("waiting", "waiting")));
        assertTrue(EventFlowRules.hasLotteryStarted(Arrays.asList("waiting", "selected")));
        assertTrue(EventFlowRules.hasLotteryStarted(Arrays.asList("accepted")));
    }

    @Test
    public void pendingSelected_detectedCorrectly() {
        assertTrue(EventFlowRules.hasPendingSelected(Arrays.asList("waiting", "selected")));
        assertFalse(EventFlowRules.hasPendingSelected(Arrays.asList("waiting", "accepted", "declined")));
    }

    @Test
    public void occupiedSpots_countsAcceptedAndSelected() {
        int occupied = EventFlowRules.countOccupiedSpots(
                Arrays.asList("waiting", "selected", "accepted", "declined", "cancelled")
        );

        assertEquals(2, occupied);
    }

    @Test
    public void csvExport_onlyAcceptedEntrants() {
        assertTrue(EventFlowRules.shouldExportToCsv("accepted"));
        assertFalse(EventFlowRules.shouldExportToCsv("selected"));
        assertFalse(EventFlowRules.shouldExportToCsv("waiting"));
    }

    @Test
    public void historyLabels_mapAppStatusesForDemo() {
        assertEquals("WAITING LIST", EventFlowRules.getHistoryStatusLabel("waiting"));
        assertEquals("SELECTED", EventFlowRules.getHistoryStatusLabel("selected"));
        assertEquals("REGISTERED", EventFlowRules.getHistoryStatusLabel("accepted"));
        assertEquals("DECLINED", EventFlowRules.getHistoryStatusLabel("declined"));
        assertEquals("CANCELLED", EventFlowRules.getHistoryStatusLabel("cancelled"));
    }

    @Test
    public void eventStatusLabels_matchEntrantStates() {
        assertEquals("Status : OPEN", EventFlowRules.getEventStatusLabel("", false));
        assertEquals("Status : WAITING LIST", EventFlowRules.getEventStatusLabel("waiting", false));
        assertEquals("Status : SELECTED", EventFlowRules.getEventStatusLabel("selected", false));
        assertEquals("Status : REGISTERED", EventFlowRules.getEventStatusLabel("accepted", false));
        assertEquals("Status : CO-HOST", EventFlowRules.getEventStatusLabel("", true));
    }
}
