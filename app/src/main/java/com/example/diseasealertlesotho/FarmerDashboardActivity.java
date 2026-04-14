package com.example.diseasealertlesotho;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class FarmerDashboardActivity extends AppCompatActivity {

    View btnReport;
    TextView tvUserName, tvTotalReports;

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

        tvUserName = findViewById(R.id.tv_user_name);
        tvTotalReports = findViewById(R.id.tv_total_reports);
        
        // Retrieve data from Intent
        String userName = getIntent().getStringExtra("USER_NAME");
        String totalReports = getIntent().getStringExtra("TOTAL_REPORTS");
        final String userEmail = getIntent().getStringExtra("USER_EMAIL"); // Need this to pass to report activity

        if (userName != null && !userName.isEmpty()) {
            tvUserName.setText(userName);
        }
        
        if (totalReports != null) {
            tvTotalReports.setText(totalReports);
        }

        btnReport = findViewById(R.id.btn_report_nav);
        btnReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FarmerDashboardActivity.this, ReportDiseaseActivity.class);
                intent.putExtra("USER_EMAIL", userEmail);
                startActivity(intent);
            }
        });
        findViewById(R.id.layout_reports_tab).setOnClickListener(v -> {
            Intent intent = new Intent(FarmerDashboardActivity.this, FarmerReportHistoryActivity.class);
            startActivity(intent);
        });
    }
}