package com.example.lotteryapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Covers the small UI helper rules for the history screen model.
 */
public class HistoryEntryTest {

    @Test
    public void nullStatus_defaultsToWaiting() {
        HistoryEntry entry = new HistoryEntry("app-1", "event-1", null);
        assertEquals("waiting", entry.getStatus());
    }

    @Test
    public void selectedEntry_canRespondAndKeepsEventDetails() {
        HistoryEntry entry = new HistoryEntry("app-2", "event-2", "selected");
        entry.setEventName("Spring Fair");
        entry.setEventDate("2026-05-01");
        entry.setEventLocation("Edmonton");
        entry.setUpdating(true);

        assertTrue(entry.canRespond());
        assertTrue(entry.hasEvent());
        assertEquals("Spring Fair", entry.getEventName());
        assertEquals("2026-05-01", entry.getEventDate());
        assertEquals("Edmonton", entry.getEventLocation());
        assertTrue(entry.isUpdating());
    }

    @Test
    public void emptyEventId_reportsMissingEvent() {
        HistoryEntry entry = new HistoryEntry("app-3", "", "declined");
        assertFalse(entry.canRespond());
        assertFalse(entry.hasEvent());
    }

    @Test
    public void setStatus_null_resetsToWaiting() {
        HistoryEntry entry = new HistoryEntry("app-4", "event-4", "accepted");
        entry.setStatus(null);
        assertEquals("waiting", entry.getStatus());
    }

    @Test
    public void nonSelectedStatus_cannotRespond() {
        HistoryEntry entry = new HistoryEntry("app-5", "event-5", "accepted");
        assertFalse(entry.canRespond());
    }

    @Test
    public void constructor_keepsApplicationAndEventIds() {
        HistoryEntry entry = new HistoryEntry("app-6", "event-6", "waiting");
        assertEquals("app-6", entry.getApplicationId());
        assertEquals("event-6", entry.getEventId());
    }

    @Test
    public void defaultDisplayValues_areInitialized() {
        HistoryEntry entry = new HistoryEntry("app-7", "event-7", "waiting");
        assertEquals("Event unavailable", entry.getEventName());
        assertEquals("", entry.getEventDate());
        assertEquals("", entry.getEventLocation());
        assertFalse(entry.isUpdating());
    }
}
