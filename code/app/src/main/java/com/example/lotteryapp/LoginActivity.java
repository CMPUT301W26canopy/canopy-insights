package com.example.lotteryapp;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

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

public class LoginActivity extends AppCompatActivity {

    private Button loginToggleBtn;
    private Button signUpToggleBtn;
    private Button loginDeviceBtn;
    private DeviceData deviceData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        deviceData = DeviceData.getInstance(this);

        ImageButton btnBack = findViewById(R.id.back_btn_top);
        btnBack.setOnClickListener(v -> finish());

        // Set up fragment buttons

        loginToggleBtn = findViewById(R.id.login_toggle_btn);
        loginToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainerView, LoginFragment.class, null)
                        .setReorderingAllowed(true)
                        .addToBackStack(null)
                        .commit()

                ;


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
                        .commit()

                ;


            }

        });

        loginDeviceBtn = findViewById(R.id.login_device_btn);
        if (loginDeviceBtn != null) {
            loginDeviceBtn.setOnClickListener(v -> loginWithDevice());
        }

        setupBottomNav();

    }

    private void loginWithDevice() {
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        if (androidId == null) {
            Toast.makeText(this, "Could not retrieve device ID", Toast.LENGTH_SHORT).show();
            return;
        }

        FirestoreHelper.getDb().collection("accounts").document(androidId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        String userType = documentSnapshot.getString("userType");
                        deviceData.createLoginSession(androidId, username, userType != null ? userType : "User");
                        navigateToMain();
                    } else {
                        createDeviceAccount(androidId);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void createDeviceAccount(String androidId) {
        String defaultUsername = "Guest_" + androidId.substring(0, 4);
        String timestamp = new SimpleDateFormat("MMddHHmmss", Locale.getDefault()).format(new Date());

        Map<String, Object> account = new HashMap<>();
        account.put("accountID", androidId);
        account.put("username", defaultUsername);
        account.put("name", defaultUsername);
        account.put("userType", "User"); // default for device login
        account.put("notificationEnabled", true);
        account.put("notificationsRead", 0);

        FirestoreHelper.getDb().collection("accounts").document(androidId).set(account)
                .addOnSuccessListener(aVoid -> {
                    // Send system notification
                    NotificationModel notification = new NotificationModel();
                    notification.setSenderAccountID("SYSTEM");
                    notification.setReceiverAccountID(androidId);
                    notification.setMessage("Welcome to Lottery App! You have logged in with your device.");
                    notification.setTimestamp(timestamp);

                    List<NotificationModel> notificationList = new ArrayList<>();
                    notificationList.add(notification);

                    Map<String, Object> notifData = new HashMap<>();
                    notifData.put("notificationList", notificationList);

                    FirestoreHelper.getDb().collection("notifications").document(androidId).set(notifData)
                            .addOnSuccessListener(aVoidNotif -> {
                                deviceData.createLoginSession(androidId, defaultUsername, "User");
                                navigateToMain();
                            })
                            .addOnFailureListener(e -> {
                                // Even if notification fails, log the user in
                                deviceData.createLoginSession(androidId, defaultUsername, "User");
                                navigateToMain();
                            });
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Account creation failed", Toast.LENGTH_SHORT).show());
    }

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupBottomNav() {
        findViewById(R.id.navHome).setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);


        });

        findViewById(R.id.navCreate).setOnClickListener(v ->{
            Log.d("DEBUG", "navCreate clicked");
            Intent intent = new Intent(LoginActivity.this, OrganizerActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.navHistory).setOnClickListener(v ->
                NavigationHelper.openHistory(this));

        findViewById(R.id.navProfile).setOnClickListener(v ->{
            //ProfileActivity
            // Toast.makeText(this, "Profile — coming soon", Toast.LENGTH_SHORT).show()
        });
    }


}
