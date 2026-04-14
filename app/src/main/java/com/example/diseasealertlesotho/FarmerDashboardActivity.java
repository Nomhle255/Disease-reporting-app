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
    TextView tvUserName;

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
        
        // Retrieve the user's name from the Intent
        String userName = getIntent().getStringExtra("USER_NAME");
        if (userName != null && !userName.isEmpty()) {
            tvUserName.setText(userName);
        }

        btnReport = findViewById(R.id.btn_report_nav);
        btnReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FarmerDashboardActivity.this, ReportDiseaseActivity.class);
                startActivity(intent);
            }
        });
    }
}