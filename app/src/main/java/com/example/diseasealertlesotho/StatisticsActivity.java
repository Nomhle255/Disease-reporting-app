package com.example.diseasealertlesotho;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class StatisticsActivity extends AppCompatActivity {

    private SQLiteDatabase db;
    private TextView tvCattleCount, tvSheepCount, tvGoatsCount, tvPoultryCount;
    private ProgressBar pbCattle, pbSheep, pbGoats, pbPoultry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_statistics);

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
        
        loadStatistics();
        setupNavigation();

        findViewById(R.id.tv_back_dashboard).setOnClickListener(v -> finish());
    }

    private void initViews() {
        tvCattleCount = findViewById(R.id.tv_cattle_count);
        tvSheepCount = findViewById(R.id.tv_sheep_count);
        tvGoatsCount = findViewById(R.id.tv_goats_count);
        tvPoultryCount = findViewById(R.id.tv_poultry_count);

        pbCattle = findViewById(R.id.pb_cattle);
        pbSheep = findViewById(R.id.pb_sheep);
        pbGoats = findViewById(R.id.pb_goats);
        pbPoultry = findViewById(R.id.pb_poultry);
    }

    private void loadStatistics() {
        int totalReports = 0;
        Cursor cTotal = db.rawQuery("SELECT COUNT(*) FROM reports", null);
        if (cTotal.moveToFirst()) {
            totalReports = cTotal.getInt(0);
        }
        cTotal.close();

        if (totalReports == 0) return;

        // Animal Type Stats
        updateAnimalStat("Cattle", totalReports, tvCattleCount, pbCattle);
        updateAnimalStat("Sheep", totalReports, tvSheepCount, pbSheep);
        updateAnimalStat("Goats", totalReports, tvGoatsCount, pbGoats);
        updateAnimalStat("Poultry", totalReports, tvPoultryCount, pbPoultry);
    }

    private void updateAnimalStat(String type, int total, TextView tvCount, ProgressBar pb) {
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM reports WHERE animal_type = ?", new String[]{type});
        if (c.moveToFirst()) {
            int count = c.getInt(0);
            tvCount.setText(count + " reports");
            int percent = (count * 100) / total;
            pb.setProgress(percent);
        }
        c.close();
    }

    private void setupNavigation() {
        View bottomNav = findViewById(R.id.bottom_navigation);
        
        bottomNav.findViewById(R.id.layout_home_tab).setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminDashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        bottomNav.findViewById(R.id.layout_users_tab).setOnClickListener(v -> {
            startActivity(new Intent(this, ManageUsersActivity.class));
            finish();
        });
        
        bottomNav.findViewById(R.id.layout_reports_tab).setOnClickListener(v -> {
            startActivity(new Intent(this, AllReportsActivity.class));
            finish();
        });

        bottomNav.findViewById(R.id.layout_profile_tab).setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminDashboardActivity.class);
            intent.putExtra("OPEN_FRAGMENT", "PROFILE");
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // Highlight Stats tab
        ((ImageView)bottomNav.findViewById(R.id.iv_stats_icon)).setColorFilter(ContextCompat.getColor(this, R.color.header_green));
        ((TextView)bottomNav.findViewById(R.id.tv_stats_text)).setTextColor(ContextCompat.getColor(this, R.color.header_green));
    }
}
