package com.example.lotteryapp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class LoginActivityTest {

    @Rule
    public ActivityScenarioRule<LoginActivity> activityRule =
            new ActivityScenarioRule<>(LoginActivity.class);

    @Test
    public void testLoginElementsDisplayed() {
        // Check if the top bar elements are displayed
        onView(withId(R.id.signed_in_as)).check(matches(isDisplayed()));
        onView(withId(R.id.back_btn_top)).check(matches(isDisplayed()));
        
        // Check if the toggle buttons are displayed (connected to the fragments)
        onView(withId(R.id.login_toggle_btn)).check(matches(isDisplayed()));
        onView(withId(R.id.sign_up_toggle_btn)).check(matches(isDisplayed()));
    }

    @Test
    public void testToggleBetweenLoginAndSignUp() {
        // Click on Sign Up toggle
        onView(withId(R.id.sign_up_toggle_btn)).perform(click());
        // Check if the register button from SignUpFragment is displayed
        onView(withId(R.id.register)).check(matches(isDisplayed()));

        // Click on Login toggle
        onView(withId(R.id.login_toggle_btn)).perform(click());
        // Check if the login button from LoginFragment is displayed
        onView(withId(R.id.login)).check(matches(isDisplayed()));
    }

    @Test
    public void testBottomNavigation() {
        // Check if bottom nav buttons are displayed
        onView(withId(R.id.navHome)).check(matches(isDisplayed()));
        onView(withId(R.id.navCreate)).check(matches(isDisplayed()));
        onView(withId(R.id.navHistory)).check(matches(isDisplayed()));
        onView(withId(R.id.navProfile)).check(matches(isDisplayed()));
    }

    @Test
    public void testBackButton() {
        // Activity should finish when th button is clicked
        onView(withId(R.id.back_btn_top)).perform(click());

    }
}
