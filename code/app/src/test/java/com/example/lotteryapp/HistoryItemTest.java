package com.example.lotteryapp;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests the HistoryItem data container used by HistoryAdapter.
 */
public class HistoryItemTest {

    @Test
    public void publicFields_storeValuesCorrectly() {
        HistoryAdapter.HistoryItem item = new HistoryAdapter.HistoryItem();
        item.eventId = "event-1";
        item.eventName = "Spring Fair";
        item.eventDate = "2026-05-01";
        item.rawStatus = "waiting";
        item.ageGroup = "18+";
        item.location = "Edmonton";
        item.description = "Fun event";
        item.posterBase64 = "abc123";
        item.price = 15.0;
        item.totalSpots = 40;
        item.waitingCount = 12;
        item.sortTimeMs = 123456789L;

        assertEquals("event-1", item.eventId);
        assertEquals("Spring Fair", item.eventName);
        assertEquals("2026-05-01", item.eventDate);
        assertEquals("waiting", item.rawStatus);
        assertEquals("18+", item.ageGroup);
        assertEquals("Edmonton", item.location);
        assertEquals("Fun event", item.description);
        assertEquals("abc123", item.posterBase64);
        assertEquals(15.0, item.price, 0.001);
        assertEquals(40, item.totalSpots);
        assertEquals(12, item.waitingCount);
        assertEquals(123456789L, item.sortTimeMs);
    }
}