package com.example.lotteryapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Tests the event model's small data helpers and guard rails.
 */
public class EventModelTest {

    @Test
    public void waitingListHelpers_addRemoveAndCheckUsers() {
        EventModel event = new EventModel();

        event.addToWaitingList("user-a");
        event.addToWaitingList("user-a");
        event.addToWaitingList("user-b");

        assertTrue(event.isOnWaitingList("user-a"));
        assertEquals(2, event.getWaitingList().size());

        event.removeFromWaitingList("user-a");

        assertFalse(event.isOnWaitingList("user-a"));
        assertEquals(1, event.getWaitingList().size());
    }

    @Test
    public void waitingListLimit_clampsNegativeValuesToZero() {
        EventModel event = new EventModel();
        event.setWaitingListLimit(-4);
        assertEquals(0, event.getWaitingListLimit());
    }

    @Test
    public void waitingCount_prefersExplicitValueWhenPresent() {
        EventModel event = new EventModel();
        event.setWaitingList(Arrays.asList("a", "b"));
        assertEquals(2, event.getWaitingListCount());

        event.setWaitingCount(7);
        assertEquals(7, event.getWaitingListCount());
    }

    @Test
    public void inviteAndHostLists_roundTripThroughModel() {
        EventModel event = new EventModel();

        event.setInvitedHosts(Arrays.asList("host-1"));
        event.setCoHosts(Arrays.asList("host-2"));
        event.setInvitedParticipants(Arrays.asList("user-1", "user-2"));
        event.setDeclinedParticipantInvites(Arrays.asList("user-3"));

        assertEquals(1, event.getInvitedHosts().size());
        assertEquals(1, event.getCoHosts().size());
        assertEquals(2, event.getInvitedParticipants().size());
        assertEquals(1, event.getDeclinedParticipantInvites().size());
    }

    @Test
    public void geolocationHelpers_manageLocations() {
        EventModel event = new EventModel();

        event.addLocation("Edmonton");
        event.addLocation("Calgary");
        assertEquals(2, event.getGeolocationList().size());

        event.removeLocation("Edmonton");
        assertEquals(1, event.getGeolocationList().size());

        ArrayList<String> replacement = new ArrayList<>();
        replacement.add("Red Deer");
        event.setGeolocationList(replacement);
        assertEquals("Red Deer", event.getGeolocationList().get(0));
    }
}
