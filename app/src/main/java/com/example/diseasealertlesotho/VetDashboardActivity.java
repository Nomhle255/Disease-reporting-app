package com.example.diseasealertlesotho;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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

public class VetDashboardActivity extends AppCompatActivity {

    private LinearLayout layoutHomeTab, layoutCasesTab, layoutAlertsTab, layoutProfileTab;
    private ImageView ivHome, ivCases, ivAlerts, ivProfile;
    private TextView tvHome, tvCases, tvAlerts, tvProfile, tvPendingBadge;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_vet_dashboard);

        dbHelper = new DatabaseHelper(this);

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
        updatePendingBadge();

        // Handle navigation
        String openFragment = getIntent().getStringExtra("OPEN_FRAGMENT");
        if ("PROFILE".equals(openFragment)) {
            loadFragment(new ProfileFragment(), "PROFILE");
        } else {
            loadFragment(new VetHomeFragment(), "HOME");
        }
    }

    private void initViews() {
        layoutHomeTab = findViewById(R.id.layout_home_tab);
        layoutCasesTab = findViewById(R.id.layout_cases_tab);
        layoutAlertsTab = findViewById(R.id.layout_alerts_tab);
        layoutProfileTab = findViewById(R.id.layout_profile_tab);

        ivHome = findViewById(R.id.iv_home_icon);
        ivCases = findViewById(R.id.iv_cases_icon);
        ivAlerts = findViewById(R.id.iv_alerts_icon);
        ivProfile = findViewById(R.id.iv_profile_icon);

        tvHome = findViewById(R.id.tv_home_text);
        tvCases = findViewById(R.id.tv_cases_text);
        tvAlerts = findViewById(R.id.tv_alerts_text);
        tvProfile = findViewById(R.id.tv_profile_text);
        
        tvPendingBadge = findViewById(R.id.tv_pending_badge);
    }

    private void setupNavigation() {
        layoutHomeTab.setOnClickListener(v -> loadFragment(new VetHomeFragment(), "HOME"));
        layoutCasesTab.setOnClickListener(v -> loadFragment(new VetCasesFragment(), "CASES"));
        layoutAlertsTab.setOnClickListener(v -> {
            startActivity(new Intent(this, VetAlertsActivity.class));
        });
        layoutProfileTab.setOnClickListener(v -> loadFragment(new ProfileFragment(), "PROFILE"));
    }

    public void updatePendingBadge() {
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM reports WHERE status = 'Pending' OR status IS NULL", null);
            if (cursor != null && cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                if (count > 0) {
                    tvPendingBadge.setText(String.valueOf(count));
                    tvPendingBadge.setVisibility(View.VISIBLE);
                } else {
                    tvPendingBadge.setVisibility(View.GONE);
                }
                cursor.close();
            }
        } catch (Exception e) {
            tvPendingBadge.setVisibility(View.GONE);
        }
    }

    private void loadFragment(Fragment fragment, String tag) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.vet_fragment_container, fragment, tag);
        transaction.commit();
        updateNavUI(tag);
        updatePendingBadge(); // Refresh badge when switching fragments
    }

    private void updateNavUI(String activeTab) {
        int grey = ContextCompat.getColor(this, android.R.color.darker_gray);
        int green = ContextCompat.getColor(this, R.color.header_green);

        ivHome.setColorFilter(grey);
        ivCases.setColorFilter(grey);
        ivAlerts.setColorFilter(grey);
        ivProfile.setColorFilter(grey);

        tvHome.setTextColor(grey);
        tvCases.setTextColor(grey);
        tvAlerts.setTextColor(grey);
        tvProfile.setTextColor(grey);

        switch (activeTab) {
            case "HOME": ivHome.setColorFilter(green); tvHome.setTextColor(green); break;
            case "CASES": ivCases.setColorFilter(green); tvCases.setTextColor(green); break;
            case "ALERTS": ivAlerts.setColorFilter(green); tvAlerts.setTextColor(green); break;
            case "PROFILE": ivProfile.setColorFilter(green); tvProfile.setTextColor(green); break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePendingBadge();
    }
}
