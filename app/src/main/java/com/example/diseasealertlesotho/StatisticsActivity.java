package com.example.diseasealertlesotho;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class StatisticsActivity extends AppCompatActivity {

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

        // Back to Dashboard
        findViewById(R.id.tv_back_dashboard).setOnClickListener(v -> finish());

        // Navigation
        setupNavigation();
    }

    private void setupNavigation() {
        findViewById(R.id.layout_home_tab).setOnClickListener(v -> {
            Intent intent = new Intent(StatisticsActivity.this, AdminDashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.layout_users_tab).setOnClickListener(v -> {
            Intent intent = new Intent(StatisticsActivity.this, ManageUsersActivity.class);
            startActivity(intent);
            finish();
        });
        findViewById(R.id.layout_reports_tab).setOnClickListener(v -> {
            Intent intent = new Intent(StatisticsActivity.this, AllReportsActivity.class);
            startActivity(intent);
            finish();
        });

    }
}