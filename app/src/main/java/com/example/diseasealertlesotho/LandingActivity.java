package com.example.diseasealertlesotho;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

public class LandingActivity extends AppCompatActivity {

    private MaterialButton btnGetStarted, btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check login session before anything else
        SharedPreferences sp = getSharedPreferences("UserSession", MODE_PRIVATE);
        boolean isLoggedIn = sp.getBoolean("isLoggedIn", false);
        if (isLoggedIn) {
            String role = sp.getString("role", "");
            redirectBasedOnRole(role);
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_landing);

        View mainView = findViewById(android.R.id.content);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        initViews();

        btnGetStarted.setOnClickListener(v -> {
            startActivity(new Intent(LandingActivity.this, RegistrationActivity.class));
        });

        btnLogin.setOnClickListener(v -> {
            startActivity(new Intent(LandingActivity.this, LoginActivity.class));
        });
    }

    private void initViews() {
        btnGetStarted = findViewById(R.id.btn_get_started);
        btnLogin = findViewById(R.id.btn_login);
    }

    private void redirectBasedOnRole(String role) {
        Intent intent;
        if ("Admin".equalsIgnoreCase(role)) {
            intent = new Intent(LandingActivity.this, AdminDashboardActivity.class);
        } else if ("Veterinary".equalsIgnoreCase(role)) {
            intent = new Intent(LandingActivity.this, VetDashboardActivity.class);
        } else {
            // Default to Farmer
            intent = new Intent(LandingActivity.this, FarmerDashboardActivity.class);
            SharedPreferences sp = getSharedPreferences("UserSession", MODE_PRIVATE);
            intent.putExtra("USER_NAME", sp.getString("name", "User"));
            intent.putExtra("USER_PHONE", sp.getString("phone", ""));
        }
        startActivity(intent);
        finish();
    }
}
