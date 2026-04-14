package com.example.diseasealertlesotho;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

public class LoginActivity extends AppCompatActivity {

    EditText etUsername, etPassword;
    MaterialButton btnLogin;
    TextView tvForgotPassword, tvRegister;

    SQLiteDatabase db;

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

        initViews();

        db = openOrCreateDatabase("DiseaseAlertDB", Context.MODE_PRIVATE, null);
        // Ensure tables exist
        db.execSQL("CREATE TABLE IF NOT EXISTS users(phone VARCHAR PRIMARY KEY, firstname VARCHAR, lastname VARCHAR, email VARCHAR, role VARCHAR, password VARCHAR);");
        db.execSQL("CREATE TABLE IF NOT EXISTS reports(id INTEGER PRIMARY KEY AUTOINCREMENT, user_phone VARCHAR, animal_type VARCHAR, count INTEGER, symptoms TEXT, date VARCHAR, photo BLOB);");

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

        if (username.length() == 0 || password.length() == 0) {
            showMessage("Error", "Please enter all values");
            return;
        }

        // Query database for user using parameterized query for safety
        Cursor c = db.rawQuery("SELECT * FROM users WHERE (email=? OR phone=?) AND password=?", new String[]{username, username, password});
        
        if (c.moveToFirst()) {
            String role = c.getString(c.getColumnIndexOrThrow("role"));
            String firstName = c.getString(c.getColumnIndexOrThrow("firstname"));
            String lastName = c.getString(c.getColumnIndexOrThrow("lastname"));
            String phone = c.getString(c.getColumnIndexOrThrow("phone"));
            
            if ("Admin".equalsIgnoreCase(role)) {
                Intent intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                startActivity(intent);
                finish();
            } else if ("Farmer".equalsIgnoreCase(role)) {
                // Get report counts using phone
                int totalReports = 0;
                Cursor cursorReports = db.rawQuery("SELECT COUNT(*) FROM reports WHERE user_phone=?", new String[]{phone});
                if (cursorReports.moveToFirst()) {
                    totalReports = cursorReports.getInt(0);
                }
                cursorReports.close();

                Intent intent = new Intent(LoginActivity.this, FarmerDashboardActivity.class);
                intent.putExtra("USER_NAME", firstName + " " + lastName);
                intent.putExtra("TOTAL_REPORTS", String.valueOf(totalReports));
                intent.putExtra("USER_PHONE", phone);
                startActivity(intent);
                finish();
            } else {
                showMessage("Success", "Login Successful as " + role);
            }
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