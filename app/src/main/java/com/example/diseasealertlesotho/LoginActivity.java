package com.example.diseasealertlesotho;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

public class LoginActivity extends AppCompatActivity {

    EditText etUsername, etPassword;
    MaterialButton btnLogin;
    TextView tvForgotPassword, tvRegister;

    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        View mainView = findViewById(android.R.id.content);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        dbHelper = new DatabaseHelper(this);
        requestNotificationPermission();
        initViews();

        btnLogin.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        tvRegister.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void initViews() {
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        tvRegister = findViewById(R.id.tv_register);
    }

    private void loginUser() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showMessage("Error", "Please enter all values");
            return;
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM users WHERE (email=? OR phone=?) AND password=?", new String[]{username, username, password});

        if (c.moveToFirst()) {
            int userId = c.getInt(c.getColumnIndexOrThrow("id"));
            String role = c.getString(c.getColumnIndexOrThrow("role"));
            String firstName = c.getString(c.getColumnIndexOrThrow("firstname"));
            String lastName = c.getString(c.getColumnIndexOrThrow("lastname"));
            String phone = c.getString(c.getColumnIndexOrThrow("phone"));
            String email = c.getString(c.getColumnIndexOrThrow("email"));
            
            String district = "";
            try { district = c.getString(c.getColumnIndexOrThrow("district")); } catch (Exception ignored) {}
            
            SharedPreferences sp = getSharedPreferences("UserSession", MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.putInt("userid", userId);
            editor.putString("phone", phone);
            editor.putString("email", email);
            editor.putString("firstname", firstName);
            editor.putString("lastname", lastName);
            editor.putString("name", firstName + " " + lastName);
            editor.putString("role", role);
            editor.putString("district", district);
            editor.putBoolean("isLoggedIn", true);
            editor.apply();

            Intent intent;
            if ("Admin".equalsIgnoreCase(role)) {
                intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
            } else if ("Farmer".equalsIgnoreCase(role)) {
                intent = new Intent(LoginActivity.this, FarmerDashboardActivity.class);
            } else if ("Vet".equalsIgnoreCase(role)) {
                intent = new Intent(LoginActivity.this, VetDashboardActivity.class);
            } else {
                showMessage("Error", "Unknown role: " + role);
                c.close();
                return;
            }
            startActivity(intent);
            finish();
        } else {
            showMessage("Error", "Invalid Credentials");
        }
        c.close();
    }

    public void showMessage(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.show();
    }
}
