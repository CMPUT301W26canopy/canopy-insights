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
}
