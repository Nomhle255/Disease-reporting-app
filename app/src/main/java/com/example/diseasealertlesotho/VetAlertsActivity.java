package com.example.diseasealertlesotho;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class VetAlertsActivity extends AppCompatActivity {

    private EditText etDiseaseType, etMessage;
    private MaterialButton btnSend;
    private List<CheckBox> districtCheckBoxes = new ArrayList<>();
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vet_alerts);

        dbHelper = new DatabaseHelper(this);
        initViews();
        setupClickListeners();
        setupNavigation();
    }

    private void initViews() {
        etDiseaseType = findViewById(R.id.et_disease_type);
        etMessage = findViewById(R.id.et_alert_message);
        btnSend = findViewById(R.id.btn_send_alert);

        // Initialize checkboxes
        districtCheckBoxes.add(findViewById(R.id.cb_maseru));
        districtCheckBoxes.add(findViewById(R.id.cb_leribe));
        districtCheckBoxes.add(findViewById(R.id.cb_berea));
        districtCheckBoxes.add(findViewById(R.id.cb_mafeteng));
        districtCheckBoxes.add(findViewById(R.id.cb_mohale));
        districtCheckBoxes.add(findViewById(R.id.cb_quthing));
        districtCheckBoxes.add(findViewById(R.id.cb_qacha));
        districtCheckBoxes.add(findViewById(R.id.cb_mokhotlong));
        districtCheckBoxes.add(findViewById(R.id.cb_thaba));
        districtCheckBoxes.add(findViewById(R.id.cb_butha));
    }

    private void setupClickListeners() {
        btnSend.setOnClickListener(v -> {
            String disease = etDiseaseType.getText().toString().trim();
            String message = etMessage.getText().toString().trim();

            if (disease.isEmpty() || message.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            List<String> selectedDistricts = new ArrayList<>();
            for (CheckBox cb : districtCheckBoxes) {
                if (cb.isChecked()) {
                    selectedDistricts.add(cb.getText().toString());
                }
            }

            if (selectedDistricts.isEmpty()) {
                Toast.makeText(this, "Please select at least one district", Toast.LENGTH_SHORT).show();
                return;
            }

            sendAlerts(disease, message, selectedDistricts);
        });

        findViewById(R.id.tv_back_dashboard).setOnClickListener(v -> finish());
    }

    private void sendAlerts(String disease, String message, List<String> districts) {
        try {
            String title = "DISEASE ALERT: " + disease;
            
            for (String district : districts) {
                String target = "DISTRICT:" + district;
                
                // Use NotificationHelper to save to DB and trigger a system notification
                // FarmerDashboardActivity is the host for FarmerNotificationsFragment
                NotificationHelper.showNotification(
                        this,
                        title,
                        message,
                        FarmerDashboardActivity.class,
                        target,
                        "ALERT"
                );
            }

            Toast.makeText(this, "Alerts broadcasted successfully!", Toast.LENGTH_SHORT).show();
            
            // Reset form
            etDiseaseType.setText("");
            etMessage.setText("");
            for (CheckBox cb : districtCheckBoxes) cb.setChecked(false);

        } catch (Exception e) {
            Toast.makeText(this, "Error sending alerts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupNavigation() {
        findViewById(R.id.layout_home_tab).setOnClickListener(v -> {
            startActivity(new Intent(this, VetDashboardActivity.class));
            finish();
        });

        findViewById(R.id.layout_cases_tab).setOnClickListener(v -> {
            Intent intent = new Intent(this, VetDashboardActivity.class);
            intent.putExtra("OPEN_FRAGMENT", "CASES");
            startActivity(intent);
            finish();
        });

        findViewById(R.id.layout_profile_tab).setOnClickListener(v -> {
             Intent intent = new Intent(this, VetDashboardActivity.class);
             intent.putExtra("OPEN_FRAGMENT", "PROFILE");
             startActivity(intent);
             finish();
        });
    }
}
