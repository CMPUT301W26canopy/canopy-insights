package com.example.lotteryapp;

import java.util.Objects;

/**
 * Represents a user account stored in Firestore.
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
    private boolean notificationEnabled = true; // single consistent field name

    public ProfileModel() {}

    public String getAccountID() { return accountID; }
    public void setAccountID(String accountID) { this.accountID = accountID; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    // single getter/setter — no isNotificationsEnabled vs getNotificationsEnabled conflict
    public boolean isNotificationEnabled() { return notificationEnabled; }
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
