package com.example.lotteryapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Covers the plain profile data rules used across the app.
 */
public class ProfileModelTest {

    @Test
    public void profileFields_roundTripThroughGetters() {
        ProfileModel profile = new ProfileModel();

        profile.setAccountID("account-1");
        profile.setUsername("demoUser");
        profile.setName("Demo User");
        profile.setPassword("secret");
        profile.setEmail("demo@example.com");
        profile.setPhoneNumber("7801234567");
        profile.setUserType("entrant");
        profile.setDeviceId("device-1");
        profile.setProfileImage("image-data");
        profile.setNotificationEnabled(false);

        assertEquals("account-1", profile.getAccountID());
        assertEquals("demoUser", profile.getUsername());
        assertEquals("Demo User", profile.getName());
        assertEquals("secret", profile.getPassword());
        assertEquals("demo@example.com", profile.getEmail());
        assertEquals("7801234567", profile.getPhoneNumber());
        assertEquals("entrant", profile.getUserType());
        assertEquals("device-1", profile.getDeviceId());
        assertEquals("image-data", profile.getProfileImage());
        assertEquals(false, profile.isNotificationEnabled());
    }

    @Test
    public void notifications_defaultToEnabled() {
        ProfileModel profile = new ProfileModel();
        assertTrue(profile.isNotificationEnabled());
    }

    @Test
    public void equality_usesAccountId() {
        ProfileModel first = new ProfileModel();
        first.setAccountID("same-id");

        ProfileModel second = new ProfileModel();
        second.setAccountID("same-id");

        ProfileModel third = new ProfileModel();
        third.setAccountID("different-id");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, third);
    }
}
