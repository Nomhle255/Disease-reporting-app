package com.example.diseasealertlesotho;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

    private ImageView ivHome, ivUsers, ivReports, ivStats, ivProfile;
    private TextView tvHome, tvUsers, tvReports, tvStats, tvProfile;
    private LinearLayout layoutLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_dashboard);

        View mainView = findViewById(android.R.id.content);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        initViews();
        setupClickListeners();

        layoutLogout.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
            prefs.edit().clear().apply();
            Intent intent = new Intent(this, LandingActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        String openFragment = getIntent().getStringExtra("OPEN_FRAGMENT");
        if ("PROFILE".equals(openFragment)) {
            loadFragment(new ProfileFragment(), "PROFILE");
        } else {
            loadFragment(new AdminHomeFragment(), "HOME");
        }
    }

    private void initViews() {
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
        
        layoutLogout = findViewById(R.id.layout_logout_top);
    }

    private void setupClickListeners() {
        findViewById(R.id.layout_home_tab).setOnClickListener(v -> loadFragment(new AdminHomeFragment(), "HOME"));
        findViewById(R.id.layout_users_tab).setOnClickListener(v -> loadFragment(new ManageUsersFragment(), "USERS"));
        findViewById(R.id.layout_reports_tab).setOnClickListener(v -> loadFragment(new ManageReportsFragment(), "REPORTS"));
        findViewById(R.id.layout_stats_tab).setOnClickListener(v -> loadFragment(new StatisticsFragment(), "STATS"));
        findViewById(R.id.layout_profile_tab).setOnClickListener(v -> loadFragment(new ProfileFragment(), "PROFILE"));
    }

    // Public method for fragments to call
    public void loadFragment(Fragment fragment, String tag) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.admin_fragment_container, fragment, tag);
        transaction.commit();
        updateNavUI(tag);
    }

    private void updateNavUI(String activeTab) {
        int grey = ContextCompat.getColor(this, R.color.text_hint);
        int green = ContextCompat.getColor(this, R.color.header_green);

        ivHome.setColorFilter(grey); ivUsers.setColorFilter(grey); ivReports.setColorFilter(grey);
        ivStats.setColorFilter(grey); ivProfile.setColorFilter(grey);
        tvHome.setTextColor(grey); tvUsers.setTextColor(grey); tvReports.setTextColor(grey);
        tvStats.setTextColor(grey); tvProfile.setTextColor(grey);

        switch (activeTab) {
            case "HOME": ivHome.setColorFilter(green); tvHome.setTextColor(green); break;
            case "USERS": ivUsers.setColorFilter(green); tvUsers.setTextColor(green); break;
            case "REPORTS": ivReports.setColorFilter(green); tvReports.setTextColor(green); break;
            case "STATS": ivStats.setColorFilter(green); tvStats.setTextColor(green); break;
            case "PROFILE": ivProfile.setColorFilter(green); tvProfile.setTextColor(green); break;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String openFragment = intent.getStringExtra("OPEN_FRAGMENT");
        if ("PROFILE".equals(openFragment)) {
            loadFragment(new ProfileFragment(), "PROFILE");
        }
    }
}
