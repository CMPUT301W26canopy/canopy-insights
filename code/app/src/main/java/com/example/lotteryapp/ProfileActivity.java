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
        
        // Admin status will be set in loadProfile after data is fetched
    }

    private void loadProfile() {
        if (accountID == null) return;
        // Search in "accounts" collection since that's where user credentials are stored
        db.collection("accounts").document(accountID).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                ProfileModel p = doc.toObject(ProfileModel.class);
                if (p != null) {
                    tvUsername.setText(p.getUsername());
                    etName.setText(p.getName());
                    etEmail.setText(p.getEmail());
                    etPhone.setText(p.getPhoneNumber());
                    
                    // Set admin visibility based on username
                    checkAdminStatus(p.getUsername());
                }
            }
        });
    }

    private void saveProfile() {
        if (accountID == null) return;
        db.collection("accounts").document(accountID)
                .update("name", etName.getText().toString(),
                        "email", etEmail.getText().toString(),
                        "phoneNumber", etPhone.getText().toString())
                .addOnSuccessListener(v -> Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show());
    }

    private void checkAdminStatus(String username) {
        if (username != null && (username.equals("Heeya") || username.equals("fasih"))) {
            btnAdmin.setVisibility(View.VISIBLE);
        } else {
            btnAdmin.setVisibility(View.GONE);
        }
    }
}