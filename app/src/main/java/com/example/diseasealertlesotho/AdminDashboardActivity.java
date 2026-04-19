package com.example.diseasealertlesotho;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView tvTotalUsers, tvTotalReports;
    private SQLiteDatabase db;
    private View scrollView, fragmentContainer;
    private ImageView ivHome, ivUsers, ivReports, ivStats, ivProfile;
    private TextView tvHome, tvUsers, tvReports, tvStats, tvProfile;

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

        initViews();
        db = openOrCreateDatabase("DiseaseAlertDB", Context.MODE_PRIVATE, null);

        updateStats();
        setupClickListeners();

        // Handle fragment navigation if requested
        String openFragment = getIntent().getStringExtra("OPEN_FRAGMENT");
        if ("PROFILE".equals(openFragment)) {
            loadProfileFragment();
        } else {
            updateNavUI("HOME");
        }
    }

    private void initViews() {
        scrollView = findViewById(R.id.admin_scroll_view);
        fragmentContainer = findViewById(R.id.admin_fragment_container);
        tvTotalUsers = findViewById(R.id.tv_count_total_users);
        tvTotalReports = findViewById(R.id.tv_count_total_reports);

        ivHome = findViewById(R.id.iv_home_icon);
        ivUsers = findViewById(R.id.iv_users_icon);
        ivReports = findViewById(R.id.iv_reports_icon);
        ivStats = findViewById(R.id.iv_stats_icon);
        ivProfile = findViewById(R.id.iv_profile_icon);

        tvHome = findViewById(R.id.tv_home_text);
        tvUsers = findViewById(R.id.tv_users_text);
        tvReports = findViewById(R.id.tv_reports_text);
        tvStats = findViewById(R.id.tv_stats_text);
        tvProfile = findViewById(R.id.tv_profile_text);
    }

    private void updateStats() {
        try {
            Cursor cUsers = db.rawQuery("SELECT COUNT(*) FROM users", null);
            if (cUsers.moveToFirst()) {
                tvTotalUsers.setText(String.valueOf(cUsers.getInt(0)));
            }
            cUsers.close();
            
            Cursor cReports = db.rawQuery("SELECT COUNT(*) FROM reports", null);
            if (cReports.moveToFirst()) {
                tvTotalReports.setText(String.valueOf(cReports.getInt(0)));
            }
            cReports.close();
        } catch (Exception ignored) {}
    }

    private void setupClickListeners() {
        findViewById(R.id.btn_logout_top).setOnClickListener(v -> showLogoutDialog());

        findViewById(R.id.layout_manage_users).setOnClickListener(v -> {
            startActivity(new Intent(this, ManageUsersActivity.class));
        });
        
        findViewById(R.id.layout_view_reports).setOnClickListener(v -> {
            startActivity(new Intent(this, AllReportsActivity.class));
        });
            
        findViewById(R.id.layout_statistics).setOnClickListener(v -> {
            startActivity(new Intent(this, StatisticsActivity.class));
        });

        // Bottom Navigation
        findViewById(R.id.layout_home_tab).setOnClickListener(v -> {
            scrollView.setVisibility(View.VISIBLE);
            fragmentContainer.setVisibility(View.GONE);
            updateNavUI("HOME");
        });

        findViewById(R.id.layout_users_tab).setOnClickListener(v -> {
            startActivity(new Intent(this, ManageUsersActivity.class));
        });
        
        findViewById(R.id.layout_reports_tab).setOnClickListener(v -> {
            startActivity(new Intent(this, AllReportsActivity.class));
        });

        findViewById(R.id.layout_stats_tab).setOnClickListener(v -> {
            startActivity(new Intent(this, StatisticsActivity.class));
        });

        findViewById(R.id.layout_profile_tab).setOnClickListener(v -> {
            loadProfileFragment();
        });
    }

    private void loadProfileFragment() {
        scrollView.setVisibility(View.GONE);
        fragmentContainer.setVisibility(View.VISIBLE);
        
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.admin_fragment_container, new ProfileFragment());
        transaction.commit();
        
        updateNavUI("PROFILE");
    }

    private void updateNavUI(String activeTab) {
        int grey = ContextCompat.getColor(this, R.color.text_hint);
        int green = ContextCompat.getColor(this, R.color.header_green);

        ivHome.setColorFilter(grey);
        ivUsers.setColorFilter(grey);
        ivReports.setColorFilter(grey);
        ivStats.setColorFilter(grey);
        ivProfile.setColorFilter(grey);

        tvHome.setTextColor(grey);
        tvUsers.setTextColor(grey);
        tvReports.setTextColor(grey);
        tvStats.setTextColor(grey);
        tvProfile.setTextColor(grey);

        switch (activeTab) {
            case "HOME":
                ivHome.setColorFilter(green);
                tvHome.setTextColor(green);
                break;
            case "USERS":
                ivUsers.setColorFilter(green);
                tvUsers.setTextColor(green);
                break;
            case "REPORTS":
                ivReports.setColorFilter(green);
                tvReports.setTextColor(green);
                break;
            case "STATS":
                ivStats.setColorFilter(green);
                tvStats.setTextColor(green);
                break;
            case "PROFILE":
                ivProfile.setColorFilter(green);
                tvProfile.setTextColor(green);
                break;
        }
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    getSharedPreferences("UserSession", MODE_PRIVATE).edit().clear().apply();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String openFragment = intent.getStringExtra("OPEN_FRAGMENT");
        if ("PROFILE".equals(openFragment)) {
            loadProfileFragment();
        } else {
            updateNavUI("HOME");
        }
    }
}
