package com.example.diseasealertlesotho;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView tvTotalUsers, tvTotalReports;
    private SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_dashboard);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // Initialize TextViews for summary
        tvTotalUsers = findViewById(R.id.tv_count_total_users);
        tvTotalReports = findViewById(R.id.tv_count_total_reports);

        // Open the existing database
        db = openOrCreateDatabase("DiseaseAlertDB", Context.MODE_PRIVATE, null);

        // Load the real counts from the database
        updateStats();
        
        // Initialize click listeners for quick actions and navigation
        setupClickListeners();
    }

    private void updateStats() {
        try {
            // Calculate Total Number of Users
            Cursor cUsers = db.rawQuery("SELECT COUNT(*) FROM users", null);
            if (cUsers.moveToFirst()) {
                tvTotalUsers.setText(String.valueOf(cUsers.getInt(0)));
            }
            cUsers.close();
            
            // Calculate Total Number of Reports
            Cursor cReports = db.rawQuery("SELECT COUNT(*) FROM reports", null);
            if (cReports.moveToFirst()) {
                tvTotalReports.setText(String.valueOf(cReports.getInt(0)));
            }
            cReports.close();
        } catch (Exception e) {
            // Table might not exist yet
        }
    }

    private void setupClickListeners() {
        // Top Logout Button
        findViewById(R.id.btn_logout_top).setOnClickListener(v -> showLogoutDialog());

        // Manage Users Action
        findViewById(R.id.layout_manage_users).setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, ManageUsersActivity.class);
            startActivity(intent);
        });
        
        findViewById(R.id.layout_view_reports).setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AllReportsActivity.class);
            startActivity(intent);
        });
            
        findViewById(R.id.layout_send_alerts).setOnClickListener(v -> 
            Toast.makeText(this, "Send Alerts clicked", Toast.LENGTH_SHORT).show());

        findViewById(R.id.layout_statistics).setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, StatisticsActivity.class);
            startActivity(intent);
        });

        // Bottom Navigation "Users" tab
        findViewById(R.id.bottom_navigation).findViewById(R.id.layout_users_tab).setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, ManageUsersActivity.class);
            startActivity(intent);
        });
        
        // Bottom navigation "Reports" tab
        findViewById(R.id.bottom_navigation).findViewById(R.id.layout_reports_tab).setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AllReportsActivity.class);
            startActivity(intent);
        });

        // Bottom navigation "Statistics" tab
        findViewById(R.id.bottom_navigation).findViewById(R.id.layout_stats_tab).setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, StatisticsActivity.class);
            startActivity(intent);
        });

        // Bottom navigation "Profile" tab
        findViewById(R.id.bottom_navigation).findViewById(R.id.layout_profile_tab).setOnClickListener(v -> {
            Toast.makeText(this, "Profile clicked", Toast.LENGTH_SHORT).show();
        });

        // Bottom navigation "Home" tab
        findViewById(R.id.bottom_navigation).findViewById(R.id.layout_home_tab).setOnClickListener(v -> {
            // Already here
        });
    }

    private void showLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Logout");
        builder.setMessage("Are you sure you want to logout?");
        builder.setPositiveButton("Logout", (dialog, which) -> {
            SharedPreferences sp = getSharedPreferences("UserSession", MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.clear();
            editor.apply();

            Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}