package com.example.lotteryapp;

import java.util.Objects;

/**
 * Represents a user profile stored in Firestore.
 * This model is shared by entrant, organizer, and admin accounts.
 */
public class ProfileModel {

    private String accountID;
    private String username;
    private String name;
    private String password;
    private String email;
    private String phoneNumber;
    private String userType;
    private String deviceId;
    private String profileImage;
    private boolean notificationEnabled = true;

    /**
     * Empty constructor required for Firestore mapping.
     */
    public ProfileModel() {}

    /**
     * Returns the unique account ID.
     * @return The account ID.
     */
    public String getAccountID() { return accountID; }

    /**
     * Sets the unique account ID.
     * @param accountID The account ID.
     */
    public void setAccountID(String accountID) { this.accountID = accountID; }

    /**
     * Returns the username used for sign in.
     * @return The username.
     */
    public String getUsername() { return username; }

    /**
     * Sets the username used for sign in.
     * @param username The username.
     */
    public void setUsername(String username) { this.username = username; }

    /**
     * Returns the display name shown around the app.
     * @return The display name.
     */
    public String getName() { return name; }

    /**
     * Sets the display name shown around the app.
     * @param name The display name.
     */
    public void setName(String name) { this.name = name; }

    /**
     * Returns the stored password value.
     * @return The password.
     */
    public String getPassword() { return password; }

    /**
     * Sets the stored password value.
     * @param password The password.
     */
    public void setPassword(String password) { this.password = password; }

    /**
     * Returns the email address tied to the profile.
     * @return The email address.
     */
    public String getEmail() { return email; }

    /**
     * Sets the email address tied to the profile.
     * @param email The email address.
     */
    public void setEmail(String email) { this.email = email; }

    /**
     * Returns the optional phone number for the profile.
     * @return The phone number.
     */
    public String getPhoneNumber() { return phoneNumber; }

    /**
     * Sets the optional phone number for the profile.
     * @param phoneNumber The phone number.
     */
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    /**
     * Returns the role assigned to the account.
     * @return The user type or role.
     */
    public String getUserType() { return userType; }

    /**
     * Sets the role assigned to the account.
     * @param userType The user type or role.
     */
    public void setUserType(String userType) { this.userType = userType; }

    /**
     * Returns the stable device ID for device-based sign in.
     * @return The device ID.
     */
    public String getDeviceId() { return deviceId; }

    /**
     * Sets the stable device ID for device-based sign in.
     * @param deviceId The device ID.
     */
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    /**
     * Returns the saved profile image as a URL or encoded string.
     * @return The profile image data.
     */
    public String getProfileImage() { return profileImage; }

    /**
     * Sets the saved profile image as a URL or encoded string.
     * @param profileImage The profile image data.
     */
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }

    /**
     * Returns whether the user wants to receive new notifications.
     * @return True if notifications are enabled.
     */
    public boolean isNotificationEnabled() { return notificationEnabled; }

    /**
     * Sets whether the user wants to receive new notifications.
     * @param notificationEnabled True to enable notifications.
     */
    public void setNotificationEnabled(boolean notificationEnabled) {
        this.notificationEnabled = notificationEnabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProfileModel that = (ProfileModel) o;
        return Objects.equals(accountID, that.accountID);
    }

    @Override
    public int hashCode() { return Objects.hash(accountID); }
}
