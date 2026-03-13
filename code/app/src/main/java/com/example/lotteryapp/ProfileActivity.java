package com.example.lotteryapp;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;

/**
 * Screen showing the user profile.
 * Loads details from Firestore and allows users to update their information.
 */
public class ProfileActivity extends AppCompatActivity {

    private Button cancelButton;
    private Button updateButton;
    private Button deleteButton;
    private Button signOutButton;

    private EditText nameInput;
    private EditText emailInput;
    private EditText phoneInput;
    private EditText usernameInput;
    
    private TextView welcomeText;

    // To store original data for comparison
    private String originalName = "";
    private String originalEmail = "";
    private String originalPhone = "";
    private String originalUsername = "";
    
    private String accountID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        
        // Connect Buttons
        cancelButton = findViewById(R.id.cancel_button);
        updateButton = findViewById(R.id.update_button);
        deleteButton = findViewById(R.id.delete_account_btn);
        signOutButton = findViewById(R.id.sign_out_btn);

        // Connect EditTexts
        nameInput = findViewById(R.id.name_provided);
        emailInput = findViewById(R.id.email_address);
        phoneInput = findViewById(R.id.phone_number);
        usernameInput = findViewById(R.id.username_provided);
        
        // Connect TextView
        welcomeText = findViewById(R.id.welcome_text);

        // Receive accountID from Intent
        accountID = getIntent().getStringExtra("accountID");
        if (accountID != null) {
            loadProfileData(accountID);
        }

        // Set up listeners to detect changes
        setupTextWatchers();
        
        // Set up bottom navigation
        setupBottomNav();

        // Cancel Button - Revert to original data
        if (cancelButton != null) {
            cancelButton.setOnClickListener(v -> revertChanges());
        }

        // Update Button
        if (updateButton != null) {
            updateButton.setOnClickListener(v -> updateProfile());
        }

        // Delete Button
        if (deleteButton != null) {
            deleteButton.setOnClickListener(v -> {
                // will delete the profile
                //Toast.makeText(this, "Feature not available yet", Toast.LENGTH_SHORT).show();
                deleteProfile();
            });
        }
        
        // Sign Out Button
        if (signOutButton != null) {
            signOutButton.setOnClickListener(v -> finish());
        }
    }

    private void deleteProfile() {
        if (accountID == null || isFinishing()) return;

        Toast.makeText(this, "Deleting profile...", Toast.LENGTH_SHORT).show();

        FirestoreHelper.getDb().collection("accounts").document(accountID)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (isFinishing()) return;
                    Toast.makeText(this, "Profile deleted successfully", Toast.LENGTH_LONG).show();
                    
                    // Navigate back to Login/Main or exit
                    finish();
                })
                .addOnFailureListener(e -> {
                    if (isFinishing()) return;
                    Toast.makeText(this, "Error deleting profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void revertChanges() {
        if (usernameInput == null || nameInput == null || emailInput == null || phoneInput == null) return;

        // Restore original values to EditTexts
        usernameInput.setText(originalUsername);
        nameInput.setText(originalName);
        emailInput.setText(originalEmail);
        phoneInput.setText(originalPhone);
        
        Toast.makeText(this, "Changes discarded", Toast.LENGTH_SHORT).show();
        
        checkForChanges();
    }

    private void updateProfile() {
        if (accountID == null || isFinishing()) return;

        String newUsername = usernameInput != null ? usernameInput.getText().toString().trim() : "";
        String newName = nameInput != null ? nameInput.getText().toString().trim() : "";
        String newEmail = emailInput != null ? emailInput.getText().toString().trim() : "";
        String newPhone = phoneInput != null ? phoneInput.getText().toString().trim() : "";

        Toast.makeText(this, "Updating profile...", Toast.LENGTH_SHORT).show();

        FirestoreHelper.getDb().collection("accounts").document(accountID)
                .update(
                        "username", newUsername,
                        "name", newName,
                        "email", newEmail,
                        "phoneNumber", newPhone
                )
                .addOnSuccessListener(aVoid -> {
                    if (isFinishing()) return; // Prevent crash if activity was closed
                    
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    
                    originalUsername = newUsername;
                    originalName = newName;
                    originalEmail = newEmail;
                    originalPhone = newPhone;
                    
                    if (welcomeText != null) {
                        welcomeText.setText("Welcome, " + originalUsername);
                    }
                    
                    checkForChanges(); 
                })
                .addOnFailureListener(e -> {
                    if (isFinishing()) return;
                    Toast.makeText(this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupTextWatchers() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                checkForChanges();
            }
        };

        if (usernameInput != null) usernameInput.addTextChangedListener(watcher);
        if (nameInput != null) nameInput.addTextChangedListener(watcher);
        if (emailInput != null) emailInput.addTextChangedListener(watcher);
        if (phoneInput != null) phoneInput.addTextChangedListener(watcher);
    }

    private void checkForChanges() {
        if (usernameInput == null || nameInput == null || emailInput == null || phoneInput == null || updateButton == null) return;

        String currentUsername = usernameInput.getText().toString().trim();
        String currentName = nameInput.getText().toString().trim();
        String currentEmail = emailInput.getText().toString().trim();
        String currentPhone = phoneInput.getText().toString().trim();

        boolean hasChanged = !currentUsername.equals(originalUsername) ||
                             !currentName.equals(originalName) ||
                             !currentEmail.equals(originalEmail) ||
                             !currentPhone.equals(originalPhone);

        if (hasChanged) {
            updateButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2196F3")));
            updateButton.setTextColor(Color.WHITE);
        } else {
            updateButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D3D3D3")));
            updateButton.setTextColor(Color.BLACK);
        }
    }

    private void loadProfileData(String accountID) {
        if (isFinishing()) return;

        FirestoreHelper.getDb().collection("accounts").document(accountID)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (isFinishing()) return; // Safety check

                    if (documentSnapshot.exists()) {
                        ProfileModel profile = documentSnapshot.toObject(ProfileModel.class);
                        if (profile != null) {
                            originalUsername = profile.getUsername() != null ? profile.getUsername() : "";
                            originalName = profile.getName() != null ? profile.getName() : "";
                            originalEmail = profile.getEmail() != null ? profile.getEmail() : "";
                            originalPhone = profile.getPhoneNumber() != null ? profile.getPhoneNumber() : "";

                            if (welcomeText != null) welcomeText.setText("Welcome, " + originalUsername);
                            if (usernameInput != null) usernameInput.setText(originalUsername);
                            if (nameInput != null) nameInput.setText(originalName);
                            if (emailInput != null) emailInput.setText(originalEmail);
                            if (phoneInput != null) phoneInput.setText(originalPhone);

                            checkForChanges();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (isFinishing()) return;
                    Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupBottomNav() {
        findViewById(R.id.navHome).setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
            startActivity(intent);


        });

        findViewById(R.id.navCreate).setOnClickListener(v ->{
            Log.d("DEBUG", "navCreate clicked");
            Intent intent = new Intent(ProfileActivity.this, CreateEventActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.navHistory).setOnClickListener(v ->
                //HistoryActivity
                Toast.makeText(this, "History — coming soon", Toast.LENGTH_SHORT).show());

        findViewById(R.id.navProfile).setOnClickListener(v ->{
            //ProfileActivity
            Toast.makeText(this, "Already on profile", Toast.LENGTH_SHORT).show();


        });
    }
}
