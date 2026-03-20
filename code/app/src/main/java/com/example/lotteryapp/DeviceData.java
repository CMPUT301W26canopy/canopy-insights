package com.example.lotteryapp;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages user session using SharedPreferences with a Singleton pattern.
 * Stores and retrieves user account details securely and consistently across the app.
 */

//Gemini was used 2026-03-19 "How can I change my code so the implementation is one instance instead
// of creating an object every time I need it?"
public class DeviceData {
    private static final String PREF_NAME = "UserSession";
    private static final String KEY_ACCOUNT_ID = "accountID";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";

    private static DeviceData instance;
    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    /**
     * Private constructor to prevent direct instantiation.
     * @param context the application context.
     */
    private DeviceData(Context context) {
        // Use applicationContext to avoid memory leaks
        pref = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    /**
     * Gets the singleton instance of DeviceData.
     * @param context the context to initialize the singleton if it hasn't been created yet.
     * @return the singleton instance of DeviceData.
     */
    public static synchronized DeviceData getInstance(Context context) {
        if (instance == null) {
            instance = new DeviceData(context);
        }
        return instance;
    }

    /**
     * Creates a login session by storing user details.
     * @param accountID the user's unique account ID.
     * @param username the user's username.
     */
    public void createLoginSession(String accountID, String username) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_ACCOUNT_ID, accountID);
        editor.putString(KEY_USERNAME, username);
        editor.apply();
    }

    /**
     * Checks if the user is logged in.
     * @return true if logged in and accountID is present, false otherwise.
     */
    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false) && getAccountID() != null;
    }

    /**
     * Gets the stored account ID.
     * @return the account ID, or null if not found.
     */
    public String getAccountID() {
        return pref.getString(KEY_ACCOUNT_ID, null);
    }

    /**
     * Gets the stored username.
     * @return the username, or null if not found.
     */
    public String getUsername() {
        return pref.getString(KEY_USERNAME, null);
    }

    /**
     * Clears the session data and logs the user out.
     */
    public void logoutUser() {
        editor.clear();
        editor.apply();
    }
}
