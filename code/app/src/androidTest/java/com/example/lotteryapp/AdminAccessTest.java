package com.example.lotteryapp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AdminAccessTest {

    @Test
    public void testAdminAccessForFasih() {
        // Start ProfileActivity with fasih's accountID
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ProfileActivity.class);
        intent.putExtra("accountID", "0402022956"); // fasih's ID
        
        try (ActivityScenario<ProfileActivity> scenario = ActivityScenario.launch(intent)) {
            // Wait for profile to load (username check happens after load)
            try { Thread.sleep(2000); } catch (InterruptedException e) {}
            
            // Admin button should be visible
            onView(withId(R.id.admin_panel_button)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testAdminAccessDeniedForRegularUser() {
        // Start ProfileActivity with User1's accountID
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ProfileActivity.class);
        intent.putExtra("accountID", "0402001630"); // User1's ID
        
        try (ActivityScenario<ProfileActivity> scenario = ActivityScenario.launch(intent)) {
            try { Thread.sleep(2000); } catch (InterruptedException e) {}
            
            // Admin button should NOT be visible
            onView(withId(R.id.admin_panel_button)).check(matches(not(isDisplayed())));
        }
    }
}