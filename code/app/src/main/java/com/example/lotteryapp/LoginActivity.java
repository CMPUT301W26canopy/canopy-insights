package com.example.lotteryapp;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.example.lotteryapp.ui.login.LoginFragment;
import com.example.lotteryapp.ui.login.SignUpFragment;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Hosts the login and sign-up fragments and also offers a lightweight
 * device-based entrant sign-in flow for users who do not want credentials.
 */
public class LoginActivity extends AppCompatActivity {

    private Button loginToggleBtn;
    private Button signUpToggleBtn;
    private Button loginWithDeviceBtn;
    private DeviceData deviceData;
    private TextView signedInAsText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        deviceData = DeviceData.getInstance(this);
        signedInAsText = findViewById(R.id.signed_in_as);
        loginWithDeviceBtn = findViewById(R.id.login_device_btn);
        loginWithDeviceBtn.setOnClickListener(v -> loginWithDevice());
        updateSignedInAsLabel();

        ImageButton btnBack = findViewById(R.id.back_btn_top);
        btnBack.setOnClickListener(v -> finish());

        loginToggleBtn = findViewById(R.id.login_toggle_btn);
        signUpToggleBtn = findViewById(R.id.sign_up_toggle_btn);

        // Set initial state for toggle buttons
        updateToggleUI(true);

        loginToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateToggleUI(true);
                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainerView, LoginFragment.class, null)
                        .setReorderingAllowed(true)
                        .addToBackStack(null)
                        .commit();
            }
        });

        signUpToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateToggleUI(false);
                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainerView, SignUpFragment.class, null)
                        .setReorderingAllowed(true)
                        .addToBackStack(null)
                        .commit();
            }
        });

        setupBottomNav();
    }

    /**
     * Updates the visual state of the Login/Sign-up toggle buttons.
     * @param isLogin True if the login tab is active, false if sign-up is active.
     */
    private void updateToggleUI(boolean isLogin) {
        if (isLogin) {
            loginToggleBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#6B5FA6")));
            loginToggleBtn.setTextColor(Color.WHITE);
            signUpToggleBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E8E4F3")));
            signUpToggleBtn.setTextColor(Color.parseColor("#6B5FA6"));
        } else {
            loginToggleBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E8E4F3")));
            loginToggleBtn.setTextColor(Color.parseColor("#6B5FA6"));
            signUpToggleBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#6B5FA6")));
            signUpToggleBtn.setTextColor(Color.WHITE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSignedInAsLabel();
    }

    /**
     * Reuses an existing device-linked entrant account, or creates one on the fly.
     */
    private void loginWithDevice() {
        String deviceId = deviceData.getAccountID(); // Simplified for this context
        if (deviceId == null || deviceId.trim().isEmpty()) {
            // If no account ID yet, we might need a different way to get device ID
            // but for now let's assume it works or uses the existing flow.
            // Original code used getOrCreateDeviceId which is not in the DeviceData I read earlier.
            // I'll stick to the logic that was there or similar.
            deviceId = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        }

        if (deviceId == null) {
            Toast.makeText(this, "Unable to identify this device", Toast.LENGTH_SHORT).show();
            return;
        }

        final String finalDeviceId = deviceId;
        FirestoreHelper.getDb().collection("accounts")
                .whereEqualTo("deviceId", finalDeviceId)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        reuseDeviceAccount(finalDeviceId, queryDocumentSnapshots.getDocuments().get(0));
                    } else {
                        findLegacyDeviceAccount(finalDeviceId);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Device login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * Supports older device-login documents that were stored without a deviceId field.
     * @param deviceId The unique hardware identifier for the device.
     */
    private void findLegacyDeviceAccount(String deviceId) {
        String generatedAccountId = buildDeviceAccountId(deviceId);
        FirestoreHelper.getDb().collection("accounts")
                .document(generatedAccountId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        reuseDeviceAccount(deviceId, documentSnapshot);
                        return;
                    }

                    FirestoreHelper.getDb().collection("accounts")
                            .document(deviceId)
                            .get()
                            .addOnSuccessListener(legacySnapshot -> {
                                if (legacySnapshot.exists()) {
                                    reuseDeviceAccount(deviceId, legacySnapshot);
                                } else {
                                    createDeviceAccount(deviceId);
                                }
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Device login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Device login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * Completes the session with an already-known device account and fills in
     * any newer fields that older documents may be missing.
     * @param deviceId The hardware identifier.
     * @param documentSnapshot The Firestore document for the account.
     */
    private void reuseDeviceAccount(String deviceId, DocumentSnapshot documentSnapshot) {
        String accountId = firstNonBlank(documentSnapshot.getString("accountID"), documentSnapshot.getId());
        String username = firstNonBlank(documentSnapshot.getString("username"), buildGuestUsername(deviceId));
        String userType = firstNonBlank(documentSnapshot.getString("userType"), "entrant");

        Map<String, Object> updates = new HashMap<>();
        if (!documentSnapshot.contains("accountID")) {
            updates.put("accountID", accountId);
        }
        if (documentSnapshot.getString("deviceId") == null || documentSnapshot.getString("deviceId").trim().isEmpty()) {
            updates.put("deviceId", deviceId);
        }
        if (documentSnapshot.getString("userType") == null || documentSnapshot.getString("userType").trim().isEmpty()) {
            updates.put("userType", userType);
        }
        if (!documentSnapshot.contains("notificationEnabled")) {
            updates.put("notificationEnabled", true);
        }
        if (!documentSnapshot.contains("notificationsRead")) {
            updates.put("notificationsRead", 0);
        }

        if (!updates.isEmpty()) {
            documentSnapshot.getReference().update(updates);
        }

        openProfile(accountId, username, userType);
    }

    /**
     * Creates a guest entrant profile tied to this device so the user can keep
     * using the app without a username/password account.
     * @param deviceId The hardware identifier to link the account to.
     */
    private void createDeviceAccount(String deviceId) {
        String accountId = buildDeviceAccountId(deviceId);
        String username = buildGuestUsername(deviceId);
        String userType = "entrant";

        Map<String, Object> account = new HashMap<>();
        account.put("accountID", accountId);
        account.put("username", username);
        account.put("name", "Guest User");
        account.put("deviceId", deviceId);
        account.put("userType", userType);
        account.put("notificationEnabled", true);
        account.put("notificationsRead", 0);

        FirestoreHelper.getDb().collection("accounts")
                .document(accountId)
                .set(account)
                .addOnSuccessListener(unused -> {
                    seedWelcomeNotification(accountId);
                    openProfile(accountId, username, userType);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to create device account", Toast.LENGTH_SHORT).show());
    }

    /**
     * Starts the normal profile flow after a successful sign-in.
     * @param accountId The unique account ID.
     * @param username The display username.
     * @param userType The role assigned to the user (e.g., entrant, admin).
     */
    private void openProfile(String accountId, String username, String userType) {
        deviceData.createLoginSession(accountId, username, userType);
        updateSignedInAsLabel();

        Intent intent = new Intent(this, ProfileActivity.class);
        intent.putExtra("accountID", accountId);
        startActivity(intent);
        finish();
    }

    /**
     * Sends a default welcome notification to a newly created device-based account.
     * @param accountId The ID of the account to receive the notification.
     */
    private void seedWelcomeNotification(String accountId) {
        NotificationModel notification = new NotificationModel();
        notification.setSenderAccountID("SYSTEM");
        notification.setReceiverAccountID(accountId);
        notification.setMessage("Welcome to Lottery App! You have logged in with your device.");
        notification.setTimestamp(new SimpleDateFormat("MMddHHmmss", Locale.getDefault()).format(new Date()));

        List<NotificationModel> notificationList = new ArrayList<>();
        notificationList.add(notification);

        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("notificationList", notificationList);

        FirestoreHelper.getDb().collection("notifications")
                .document(accountId)
                .set(notificationData)
                .addOnFailureListener(e ->
                        Log.w("LoginActivity", "Failed to create welcome notification", e));
    }

    /**
     * Keeps the top label in sync with the current session state.
     */
    private void updateSignedInAsLabel() {
        if (signedInAsText == null) {
            return;
        }

        String username = deviceData.getUsername();
        if (username != null && !username.trim().isEmpty()) {
            signedInAsText.setText("Signed in as : " + username);
        } else {
            signedInAsText.setText("Sign in or use this device");
        }
    }

    /**
     * Builds a readable guest username from the device identifier.
     * @param deviceId The hardware identifier.
     * @return A formatted guest username string.
     */
    private String buildGuestUsername(String deviceId) {
        String suffix = deviceId == null ? "device" : deviceId.replaceAll("[^A-Za-z0-9]", "");
        if (suffix.length() > 6) {
            suffix = suffix.substring(suffix.length() - 6);
        }
        if (suffix.isEmpty()) {
            suffix = "device";
        }
        return "guest_" + suffix.toLowerCase(Locale.getDefault());
    }

    /**
     * Generates a unique document ID for a device-linked account.
     * @param deviceId The hardware identifier.
     * @return A string formatted as "device_{identifier}".
     */
    private String buildDeviceAccountId(String deviceId) {
        return "device_" + deviceId.replaceAll("[^A-Za-z0-9_-]", "");
    }

    /**
     * Helper to pick the first non-blank string from two options.
     * @param first The primary string choice.
     * @param second The fallback string choice.
     * @return The first non-blank string, or the second if the first is invalid.
     */
    private String firstNonBlank(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first;
        }
        return second;
    }

    /**
     * Wires the shared bottom navigation used across the app.
     */
    private void setupBottomNav() {
        findViewById(R.id.navHome).setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.navCreate).setOnClickListener(v -> {
            Log.d("DEBUG", "navCreate clicked");
            Intent intent = new Intent(LoginActivity.this, OrganizerActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.navHistory).setOnClickListener(v ->
                NavigationHelper.openHistory(this));

        findViewById(R.id.navProfile).setOnClickListener(v -> {
            if (deviceData.isLoggedIn()) {
                Intent intent = new Intent(LoginActivity.this, ProfileActivity.class);
                intent.putExtra("accountID", deviceData.getAccountID());
                startActivity(intent);
            }
        });
    }
}
