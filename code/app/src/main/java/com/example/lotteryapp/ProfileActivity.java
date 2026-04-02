package com.example.lotteryapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPhone;
    private TextView tvUsername;
    private Button btnSave, btnAdmin;
    private String accountID;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        db = FirebaseFirestore.getInstance();
        accountID = getIntent().getStringExtra("accountID");

        tvUsername = findViewById(R.id.profile_username);
        etName     = findViewById(R.id.profile_name);
        etEmail    = findViewById(R.id.profile_email);
        etPhone    = findViewById(R.id.profile_phone);
        btnSave    = findViewById(R.id.save_profile_button);
        btnAdmin   = findViewById(R.id.admin_panel_button); // We'll add this to layout

        ImageButton btnBack = findViewById(R.id.back_btn_top);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        loadProfile();

        btnSave.setOnClickListener(v -> saveProfile());

        // Story 03.09.01 - Admin access from their own profile
        btnAdmin.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, AdminActivity.class);
            startActivity(intent);
        });
        
        // Check if user is admin to show button (simulated for now)
        checkAdminStatus();
    }

    private void loadProfile() {
        if (accountID == null) return;
        db.collection("profiles").document(accountID).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                ProfileModel p = doc.toObject(ProfileModel.class);
                if (p != null) {
                    tvUsername.setText(p.getUsername());
                    etName.setText(p.getName());
                    etEmail.setText(p.getEmail());
                    etPhone.setText(p.getPhoneNumber());
                }
            }
        });
    }

    private void saveProfile() {
        if (accountID == null) return;
        db.collection("profiles").document(accountID)
                .update("name", etName.getText().toString(),
                        "email", etEmail.getText().toString(),
                        "phoneNumber", etPhone.getText().toString())
                .addOnSuccessListener(v -> Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show());
    }

    private void checkAdminStatus() {
        // In a real scenario, we'd check a field in the profile like "isAdmin"
        // For this implementation, we'll show it for testing purposes
        btnAdmin.setVisibility(View.VISIBLE);
    }
}