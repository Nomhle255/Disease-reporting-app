package com.example.diseasealertlesotho;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class FarmerDashboardActivity extends AppCompatActivity {

    private LinearLayout layoutHome, layoutReports, layoutAlerts, layoutProfile;
    private ImageView ivHome, ivReports, ivAlerts, ivProfile;
    private TextView tvHome, tvReports, tvAlerts, tvProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_farmer_dashboard);

        View mainView = findViewById(android.R.id.content);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        initViews();
        setupNavigation();

        // Check if we need to open a specific fragment
        String openFragment = getIntent().getStringExtra("OPEN_FRAGMENT");
        if ("PROFILE".equals(openFragment)) {
            loadFragment(new ProfileFragment(), "PROFILE");
            updateNavUI("PROFILE");
        } else {
            // Load Farmer Home Fragment by default
            loadFragment(new FarmerHomeFragment(), "HOME");
            updateNavUI("HOME");
        }
    }

    private void initViews() {
        layoutHome = findViewById(R.id.layout_home_tab);
        layoutReports = findViewById(R.id.layout_reports_tab);
        layoutAlerts = findViewById(R.id.layout_alerts_tab);
        layoutProfile = findViewById(R.id.layout_profile_tab);

        ivHome = findViewById(R.id.iv_home_icon);
        ivReports = findViewById(R.id.iv_reports_icon);
        ivAlerts = findViewById(R.id.iv_alerts_icon);
        ivProfile = findViewById(R.id.iv_profile_icon);

        tvHome = findViewById(R.id.tv_home_text);
        tvReports = findViewById(R.id.tv_reports_text);
        tvAlerts = findViewById(R.id.tv_alerts_text);
        tvProfile = findViewById(R.id.tv_profile_text);
    }

    private void setupNavigation() {
        layoutHome.setOnClickListener(v -> {
            loadFragment(new FarmerHomeFragment(), "HOME");
            updateNavUI("HOME");
        });

        layoutReports.setOnClickListener(v -> {
            startActivity(new Intent(this, FarmerReportHistoryActivity.class));
        });

        layoutAlerts.setOnClickListener(v -> {
            startActivity(new Intent(this, FarmerNotificationsActivity.class));
        });

        layoutProfile.setOnClickListener(v -> {
            loadFragment(new ProfileFragment(), "PROFILE");
            updateNavUI("PROFILE");
        });

        findViewById(R.id.btn_report_nav).setOnClickListener(v -> {
            Intent intent = new Intent(this, ReportDiseaseActivity.class);
            startActivity(intent);
        });
    }

    private void loadFragment(Fragment fragment, String tag) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment, tag);
        transaction.commit();
    }

    private void updateNavUI(String activeTab) {
        // Reset all to grey
        int grey = ContextCompat.getColor(this, R.color.text_hint);
        int green = ContextCompat.getColor(this, R.color.primary_green);

        ivHome.setColorFilter(grey);
        ivReports.setColorFilter(grey);
        ivAlerts.setColorFilter(grey);
        ivProfile.setColorFilter(grey);

        tvHome.setTextColor(grey);
        tvReports.setTextColor(grey);
        tvAlerts.setTextColor(grey);
        tvProfile.setTextColor(grey);

        // Highlight active
        switch (activeTab) {
            case "HOME":
                ivHome.setColorFilter(green);
                tvHome.setTextColor(green);
                break;
            case "PROFILE":
                ivProfile.setColorFilter(green);
                tvProfile.setTextColor(green);
                break;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String openFragment = intent.getStringExtra("OPEN_FRAGMENT");
        if ("PROFILE".equals(openFragment)) {
            loadFragment(new ProfileFragment(), "PROFILE");
            updateNavUI("PROFILE");
        }
    }
}