package com.example.lotteryapp;

import android.content.Intent;
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

import java.util.HashMap;
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
        loginToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainerView, LoginFragment.class, null)
                        .setReorderingAllowed(true)
                        .addToBackStack(null)
                        .commit();
            }
        });

        signUpToggleBtn = findViewById(R.id.sign_up_toggle_btn);
        signUpToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
     * Reuses an existing device-linked entrant account, or creates one on the fly.
     */
    private void loginWithDevice() {
        String deviceId = deviceData.getOrCreateDeviceId(this);
        if (deviceId == null || deviceId.trim().isEmpty()) {
            Toast.makeText(this, "Unable to identify this device", Toast.LENGTH_SHORT).show();
            return;
        }

        FirestoreHelper.getDb().collection("accounts")
                .whereEqualTo("deviceId", deviceId)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        reuseDeviceAccount(deviceId, queryDocumentSnapshots.getDocuments().get(0));
                    } else {
                        createDeviceAccount(deviceId);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Device login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * Completes the session with an already-known device account and fills in
     * any newer fields that older documents may be missing.
     */
    private void reuseDeviceAccount(String deviceId, DocumentSnapshot documentSnapshot) {
        String accountId = firstNonBlank(documentSnapshot.getString("accountID"), documentSnapshot.getId());
        String username = firstNonBlank(documentSnapshot.getString("username"), buildGuestUsername(deviceId));

        Map<String, Object> updates = new HashMap<>();
        if (!documentSnapshot.contains("accountID")) {
            updates.put("accountID", accountId);
        }
        if (documentSnapshot.getString("deviceId") == null || documentSnapshot.getString("deviceId").trim().isEmpty()) {
            updates.put("deviceId", deviceId);
        }
        if (documentSnapshot.getString("userType") == null || documentSnapshot.getString("userType").trim().isEmpty()) {
            updates.put("userType", "entrant");
        }
        if (!documentSnapshot.contains("notificationEnabled")) {
            updates.put("notificationEnabled", true);
        }

        if (!updates.isEmpty()) {
            documentSnapshot.getReference().update(updates);
        }

        openProfile(accountId, username);
    }

    /**
     * Creates a guest entrant profile tied to this device so the user can keep
     * using the app without a username/password account.
     */
    private void createDeviceAccount(String deviceId) {
        String accountId = "device_" + deviceId.replaceAll("[^A-Za-z0-9_-]", "");
        String username = buildGuestUsername(deviceId);

        ProfileModel profile = new ProfileModel();
        profile.setAccountID(accountId);
        profile.setUsername(username);
        profile.setName("Guest User");
        profile.setDeviceId(deviceId);
        profile.setUserType("entrant");
        profile.setNotificationEnabled(true);

        FirestoreHelper.getDb().collection("accounts")
                .document(accountId)
                .set(profile)
                .addOnSuccessListener(unused -> openProfile(accountId, username))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to create device account", Toast.LENGTH_SHORT).show());
    }

    /**
     * Starts the normal profile flow after a successful sign-in.
     */
    private void openProfile(String accountId, String username) {
        deviceData.createLoginSession(accountId, username);

        Intent intent = new Intent(this, ProfileActivity.class);
        intent.putExtra("accountID", accountId);
        startActivity(intent);
        finish();
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
