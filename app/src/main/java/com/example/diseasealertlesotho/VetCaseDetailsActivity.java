package com.example.diseasealertlesotho;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class VetCaseDetailsActivity extends AppCompatActivity {

    private TextView tvAnimalType, tvAffectedCount, tvDateObserved, tvSymptoms, tvDetailSubtitle;
    private EditText etAdvice;
    private MaterialButton btnSchedule, btnRequest, btnResolve;
    private SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vet_case_details);

        db = openOrCreateDatabase("DiseaseAlertDB", Context.MODE_PRIVATE, null);
        
        initViews();
        setupData();
        setupClickListeners();
    }

    private void initViews() {
        tvDetailSubtitle = findViewById(R.id.tv_detail_subtitle);
        tvAnimalType = findViewById(R.id.tv_animal_type);
        tvAffectedCount = findViewById(R.id.tv_affected_count);
        tvDateObserved = findViewById(R.id.tv_date_observed);
        tvSymptoms = findViewById(R.id.tv_symptoms_details);
        etAdvice = findViewById(R.id.et_vet_advice);
        
        btnSchedule = findViewById(R.id.btn_schedule_visit);
        btnRequest = findViewById(R.id.btn_request_info);
        btnResolve = findViewById(R.id.btn_resolve_case);

        findViewById(R.id.tv_back_cases).setOnClickListener(v -> finish());
    }

    private void setupData() {
        String caseIdStr = getIntent().getStringExtra("CASE_ID");
        if (caseIdStr != null && caseIdStr.startsWith("RPT-")) {
            try {
                int id = Integer.parseInt(caseIdStr.substring(4));
                loadReportFromDB(id);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid Case ID format", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadReportFromDB(int id) {
        try {
            String query = "SELECT r.*, u.firstname, u.lastname FROM reports r " +
                          "LEFT JOIN users u ON r.user_phone = u.phone " +
                          "WHERE r.id = ?";
            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(id)});

            if (cursor.moveToFirst()) {
                String fName = cursor.getString(cursor.getColumnIndexOrThrow("firstname"));
                String lName = cursor.getString(cursor.getColumnIndexOrThrow("lastname"));
                String animal = cursor.getString(cursor.getColumnIndexOrThrow("animal_type"));
                int count = cursor.getInt(cursor.getColumnIndexOrThrow("count"));
                String symptoms = cursor.getString(cursor.getColumnIndexOrThrow("symptoms"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));

                tvDetailSubtitle.setText("Reviewing submission from " + (fName != null ? fName + " " + lName : "Unknown Farmer"));
                tvAnimalType.setText(animal);
                tvAffectedCount.setText(String.valueOf(count));
                tvDateObserved.setText(date);
                tvSymptoms.setText(symptoms != null && !symptoms.isEmpty() ? symptoms : "No description provided.");
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupClickListeners() {
        btnSchedule.setOnClickListener(v -> {
            Toast.makeText(this, "Farm visit scheduled. Notification sent to farmer.", Toast.LENGTH_SHORT).show();
        });

        btnRequest.setOnClickListener(v -> {
            Toast.makeText(this, "Info request sent. Farmer asked to provide more details/photos.", Toast.LENGTH_SHORT).show();
        });

        btnResolve.setOnClickListener(v -> {
            String advice = etAdvice.getText().toString().trim();
            if (advice.isEmpty()) {
                etAdvice.setError("Please provide advice or treatment recommendations.");
                return;
            }

            String caseIdStr = getIntent().getStringExtra("CASE_ID");
            if (caseIdStr != null && caseIdStr.startsWith("RPT-")) {
                int id = Integer.parseInt(caseIdStr.substring(4));
                updateStatus(id, "Resolved");
            }

            Toast.makeText(this, "Response submitted. Case marked as Resolved.", Toast.LENGTH_LONG).show();
            finish();
        });
        
        findViewById(R.id.cv_view_photo).setOnClickListener(v -> {
            Toast.makeText(this, "Opening high-resolution evidence photo...", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateStatus(int id, String status) {
        try {
            db.execSQL("UPDATE reports SET status = ? WHERE id = ?", new Object[]{status, id});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (db != null && db.isOpen()) {
            db.close();
        }
    }
}
