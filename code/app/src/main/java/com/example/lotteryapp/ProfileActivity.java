package com.example.lotteryapp;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Shows the current user's profile and keeps the common account actions
 * in one place: editing details, notification preferences, inbox access,
 * and a simple profile image.
 */
public class ProfileActivity extends AppCompatActivity {

    private Button cancelButton;
    private Button updateButton;
    private Button deleteButton;
    private Button signOutButton;
    private Button btnAdmin;
    private Button btnChangeProfileImage;
    private ImageButton inboxButton;
    private View inboxRedDot;
    private ImageView profileImageView;

    private EditText nameInput;
    private EditText emailInput;
    private EditText phoneInput;
    private EditText usernameInput;
    private SwitchCompat notificationsSwitch;

    private TextView welcomeText;

    private String originalName = "";
    private String originalEmail = "";
    private String originalPhone = "";
    private String originalUsername = "";
    private String originalProfileImage = "";
    private boolean originalNotificationsEnabled = false;

    private String selectedProfileImage = "";
    private String accountID;
    private DeviceData deviceData;
    private ActivityResultLauncher<String> pickProfileImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        deviceData = DeviceData.getInstance(this);

        cancelButton = findViewById(R.id.cancel_button);
        updateButton = findViewById(R.id.update_button);
        deleteButton = findViewById(R.id.delete_account_btn);
        signOutButton = findViewById(R.id.sign_out_btn);
        btnAdmin = findViewById(R.id.admin_panel_button);
        btnChangeProfileImage = findViewById(R.id.btnChangeProfileImage);
        inboxButton = findViewById(R.id.inbox_button);
        inboxRedDot = findViewById(R.id.inbox_red_dot);
        profileImageView = findViewById(R.id.ivProfileImage);

        nameInput = findViewById(R.id.name_provided);
        emailInput = findViewById(R.id.email_address);
        phoneInput = findViewById(R.id.phone_number);
        usernameInput = findViewById(R.id.username_provided);
        notificationsSwitch = findViewById(R.id.notifications_switch);
        welcomeText = findViewById(R.id.welcome_text);

        pickProfileImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                this::handleProfileImageSelection
        );

        accountID = getIntent().getStringExtra("accountID");
        if (accountID == null && deviceData.isLoggedIn()) {
            accountID = deviceData.getAccountID();
        }

        if (accountID == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadProfileData(accountID);
        setupTextWatchers();
        if (notificationsSwitch != null) {
            notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> checkForChanges());
        }
        setupBottomNav();
        checkAdminStatus();

        if (btnChangeProfileImage != null) {
            btnChangeProfileImage.setOnClickListener(v -> pickProfileImageLauncher.launch("image/*"));
        }

        if (inboxButton != null) {
            inboxButton.setOnClickListener(v -> openInbox());
        }

        if (cancelButton != null) {
            cancelButton.setOnClickListener(v -> revertChanges());
        }

        if (updateButton != null) {
            updateButton.setOnClickListener(v -> updateProfile());
        }

        if (deleteButton != null) {
            deleteButton.setOnClickListener(v -> deleteProfile());
        }

        if (signOutButton != null) {
            signOutButton.setOnClickListener(v -> {
                deviceData.logoutUser();
                Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        if (btnAdmin != null) {
            btnAdmin.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, AdminActivity.class)));
        }
    }

    /**
     * Refreshes the unread notification status when the activity comes to the foreground.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (accountID != null) {
            checkUnreadNotifications();
        }
    }

    /**
     * Reads a chosen image, crops it into a square avatar, and keeps the new
     * value in memory until the user saves the profile.
     * @param uri The URI of the selected image.
     */
    private void handleProfileImageSelection(Uri uri) {
        if (uri == null) {
            return;
        }

        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            byte[] imageBytes = readBytes(inputStream);
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            if (bitmap == null) {
                Toast.makeText(this, "Unable to read image", Toast.LENGTH_SHORT).show();
                return;
            }

            Bitmap squareBitmap = cropToSquare(bitmap);
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(squareBitmap, 600, 600, true);
            selectedProfileImage = encodeBitmap(scaledBitmap);
            bindProfileImage(selectedProfileImage);
            checkForChanges();
        } catch (IOException e) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Opens the inbox overlay and marks the current set of notifications as read.
     */
    private void openInbox() {
        if (accountID == null) {
            return;
        }

        FirestoreHelper.getDb().collection("notifications")
                .whereEqualTo("receiverAccountID", accountID)
                .get()
                .addOnSuccessListener(snap -> FirestoreHelper.getDb().collection("accounts")
                        .document(accountID)
                        .update("notificationsRead", snap.size())
                        .addOnSuccessListener(aVoid -> {
                            if (inboxRedDot != null) {
                                inboxRedDot.setVisibility(View.GONE);
                            }
                        }));

        InboxFragment fragment = InboxFragment.newInstance(accountID);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    /**
     * Permanently deletes the current user's profile from Firestore and logs them out.
     */
    private void deleteProfile() {
        if (accountID == null || isFinishing()) {
            return;
        }

        Toast.makeText(this, "Deleting profile...", Toast.LENGTH_SHORT).show();

        FirestoreHelper.getDb().collection("accounts").document(accountID)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (isFinishing()) {
                        return;
                    }
                    deviceData.logoutUser();
                    Toast.makeText(this, "Profile deleted successfully", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    if (!isFinishing()) {
                        Toast.makeText(this, "Error deleting profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Discards any unsaved changes and reverts input fields to their original values.
     */
    private void revertChanges() {
        if (usernameInput == null || nameInput == null || emailInput == null || phoneInput == null || notificationsSwitch == null) {
            return;
        }

        usernameInput.setText(originalUsername);
        nameInput.setText(originalName);
        emailInput.setText(originalEmail);
        phoneInput.setText(originalPhone);
        notificationsSwitch.setChecked(originalNotificationsEnabled);
        selectedProfileImage = originalProfileImage;
        bindProfileImage(originalProfileImage);

        Toast.makeText(this, "Changes discarded", Toast.LENGTH_SHORT).show();
        checkForChanges();
    }

    /**
     * Saves the editable profile fields and refreshes the local session name.
     */
    private void updateProfile() {
        if (accountID == null || isFinishing()) {
            return;
        }

        String newUsername = usernameInput != null ? usernameInput.getText().toString().trim() : "";
        String newName = nameInput != null ? nameInput.getText().toString().trim() : "";
        String newEmail = emailInput != null ? emailInput.getText().toString().trim() : "";
        String newPhone = phoneInput != null ? phoneInput.getText().toString().trim() : "";
        boolean newNotificationsEnabled = notificationsSwitch != null && notificationsSwitch.isChecked();

        Toast.makeText(this, "Updating profile...", Toast.LENGTH_SHORT).show();

        FirestoreHelper.getDb().collection("accounts").document(accountID)
                .update(
                        "username", newUsername,
                        "name", newName,
                        "email", newEmail,
                        "phoneNumber", newPhone,
                        "notificationEnabled", newNotificationsEnabled,
                        "profileImage", selectedProfileImage
                )
                .addOnSuccessListener(aVoid -> {
                    if (isFinishing()) {
                        return;
                    }

                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();

                    originalUsername = newUsername;
                    originalName = newName;
                    originalEmail = newEmail;
                    originalPhone = newPhone;
                    originalNotificationsEnabled = newNotificationsEnabled;
                    originalProfileImage = selectedProfileImage;
                    deviceData.createLoginSession(accountID, newUsername);

                    if (welcomeText != null) {
                        welcomeText.setText("Welcome, " + originalUsername);
                    }

                    checkForChanges();
                    checkUnreadNotifications();
                })
                .addOnFailureListener(e -> {
                    if (!isFinishing()) {
                        Toast.makeText(this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Sets up text watchers for all input fields to detect changes in real-time.
     */
    private void setupTextWatchers() {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

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

    /**
     * Compares the current input values with the original data to enable or disable the Update button.
     */
    private void checkForChanges() {
        if (usernameInput == null || nameInput == null || emailInput == null || phoneInput == null
                || notificationsSwitch == null || updateButton == null) {
            return;
        }

        String currentUsername = usernameInput.getText().toString().trim();
        String currentName = nameInput.getText().toString().trim();
        String currentEmail = emailInput.getText().toString().trim();
        String currentPhone = phoneInput.getText().toString().trim();
        boolean currentNotificationsEnabled = notificationsSwitch.isChecked();

        boolean hasChanged = !currentUsername.equals(originalUsername)
                || !currentName.equals(originalName)
                || !currentEmail.equals(originalEmail)
                || !currentPhone.equals(originalPhone)
                || currentNotificationsEnabled != originalNotificationsEnabled
                || !safe(selectedProfileImage).equals(safe(originalProfileImage));

        if (hasChanged) {
            updateButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#6B5FA6")));
            updateButton.setTextColor(Color.WHITE);
        } else {
            updateButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D3D3D3")));
            updateButton.setTextColor(Color.BLACK);
        }
    }

    /**
     * Loads user profile data from Firestore and populates the UI.
     * @param accountID The ID of the account to load.
     */
    private void loadProfileData(String accountID) {
        if (isFinishing()) {
            return;
        }

        FirestoreHelper.getDb().collection("accounts").document(accountID)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (isFinishing() || !documentSnapshot.exists()) {
                        return;
                    }

                    ProfileModel profile = documentSnapshot.toObject(ProfileModel.class);
                    if (profile == null) {
                        return;
                    }

                    originalUsername = safe(profile.getUsername());
                    originalName = safe(profile.getName());
                    originalEmail = safe(profile.getEmail());
                    originalPhone = safe(profile.getPhoneNumber());
                    originalNotificationsEnabled = profile.isNotificationEnabled();
                    originalProfileImage = safe(profile.getProfileImage());
                    selectedProfileImage = originalProfileImage;

                    if (!documentSnapshot.contains("notificationsRead")) {
                        FirestoreHelper.getDb().collection("accounts").document(accountID)
                                .update("notificationsRead", 0);
                    }

                    if (welcomeText != null) {
                        welcomeText.setText("Welcome, " + originalUsername);
                    }
                    if (usernameInput != null) usernameInput.setText(originalUsername);
                    if (nameInput != null) nameInput.setText(originalName);
                    if (emailInput != null) emailInput.setText(originalEmail);
                    if (phoneInput != null) phoneInput.setText(originalPhone);
                    if (notificationsSwitch != null) notificationsSwitch.setChecked(originalNotificationsEnabled);
                    bindProfileImage(originalProfileImage);

                    checkForChanges();
                    checkUnreadNotifications();
                    checkAdminStatus();
                })
                .addOnFailureListener(e -> {
                    if (!isFinishing()) {
                        Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Decodes a base64 profile image string and binds it to the profile ImageView.
     * @param profileImage The base64 encoded image string.
     */
    private void bindProfileImage(String profileImage) {
        if (profileImageView == null) {
            return;
        }
        if (profileImage != null && !profileImage.trim().isEmpty()) {
            try {
                byte[] decoded = Base64.decode(profileImage, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                if (bitmap != null) {
                    profileImageView.setImageBitmap(bitmap);
                    return;
                }
            } catch (Exception ignored) {
            }
        }
        profileImageView.setImageResource(R.drawable.ic_person);
    }

    /**
     * Checks for unread notifications in Firestore and toggles the red dot visibility.
     */
    private void checkUnreadNotifications() {
        if (accountID == null || inboxRedDot == null) {
            return;
        }

        FirestoreHelper.getDb().collection("accounts").document(accountID).get()
                .addOnSuccessListener(userDoc -> {
                    if (!userDoc.exists()) {
                        return;
                    }

                    boolean enabled = userDoc.getBoolean("notificationEnabled") != null
                            ? userDoc.getBoolean("notificationEnabled") : true;

                    if (!enabled) {
                        inboxRedDot.setVisibility(View.GONE);
                        return;
                    }

                    long readCount = userDoc.getLong("notificationsRead") != null
                            ? userDoc.getLong("notificationsRead") : 0;

                    FirestoreHelper.getDb().collection("notifications")
                            .whereEqualTo("receiverAccountID", accountID)
                            .get()
                            .addOnSuccessListener(snap ->
                                    inboxRedDot.setVisibility(readCount < snap.size() ? View.VISIBLE : View.GONE));
                });
    }

    /**
     * Wires the shared bottom navigation used by the profile screen.
     */
    private void setupBottomNav() {
        findViewById(R.id.navHome).setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.navCreate).setOnClickListener(v -> {
            Log.d("DEBUG", "navCreate clicked");
            Intent intent = new Intent(ProfileActivity.this, OrganizerActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.navHistory).setOnClickListener(v ->
                NavigationHelper.openHistory(this));

        findViewById(R.id.navProfile).setOnClickListener(v ->
                Toast.makeText(this, "Already on profile", Toast.LENGTH_SHORT).show());
    }

    /**
     * Checks if the user has admin privileges based on their username and updates the UI.
     */
    private void checkAdminStatus() {
        if (btnAdmin == null) {
            return;
        }
        String username = usernameInput != null ? usernameInput.getText().toString().trim() : "";
        boolean isAdmin = username.equalsIgnoreCase("Heeya") || username.equalsIgnoreCase("fasih");
        btnAdmin.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
    }

    /**
     * Reads all bytes from an input stream into a byte array.
     * @param inputStream The input stream to read from.
     * @return The resulting byte array.
     * @throws IOException If an I/O error occurs.
     */
    private byte[] readBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        return outputStream.toByteArray();
    }

    /**
     * Crops a bitmap to a square aspect ratio.
     * @param bitmap The bitmap to crop.
     * @return The resulting square bitmap.
     */
    private Bitmap cropToSquare(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int size = Math.min(width, height);
        int left = Math.max((width - size) / 2, 0);
        int top = Math.max((height - size) / 2, 0);
        return Bitmap.createBitmap(bitmap, left, top, size, size);
    }

    /**
     * Encodes a bitmap into a base64 string.
     * @param bitmap The bitmap to encode.
     * @return The base64 encoded string.
     */
    private String encodeBitmap(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 82, outputStream);
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP);
    }

    /**
     * Returns the original string or an empty string if null.
     * @param value The candidate string.
     * @return The resulting string.
     */
    private String safe(String value) {
        return value == null ? "" : value;
    }
}
