package com.example.lotteryapp;

import android.content.Intent;
import android.os.Bundle;
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

public class LoginActivity extends AppCompatActivity {

    private Button loginToggleBtn;
    private Button signUpToggleBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);


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

        setupBottomNav();

    }


    // bottom navigation — other screens
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
                //HistoryActivity
                Toast.makeText(this, "History — coming soon", Toast.LENGTH_SHORT).show());

        findViewById(R.id.navProfile).setOnClickListener(v ->{
            //ProfileActivity
            // Toast.makeText(this, "Profile — coming soon", Toast.LENGTH_SHORT).show());


        });
    }


}
