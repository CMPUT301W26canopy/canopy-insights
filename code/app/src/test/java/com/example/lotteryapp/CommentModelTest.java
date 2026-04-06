package com.example.lotteryapp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;

/**
 * Covers the basic comment model fields used by the event discussion UI.
 */
public class CommentModelTest {

    @Test
    public void commentFields_roundTripThroughGetters() {
        CommentModel comment = new CommentModel();

        comment.setMessage("Hello there");
        comment.setPosterID("poster-1");
        comment.setMessageID("msg-1");
        comment.setParentID("parent-1");
        comment.setTimestamp("2026-04-06 09:15:00");
        comment.setLikedBy(Arrays.asList("user-a", "user-b"));
        comment.setDepth(2);

        assertEquals("Hello there", comment.getMessage());
        assertEquals("poster-1", comment.getPosterID());
        assertEquals("msg-1", comment.getMessageID());
        assertEquals("parent-1", comment.getParentID());
        assertEquals("2026-04-06 09:15:00", comment.getTimestamp());
        assertEquals(2, comment.getLikedBy().size());
        assertEquals(2, comment.getDepth());
    }
}
