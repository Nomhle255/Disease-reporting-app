package com.example.diseasealertlesotho;

import android.content.Intent;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vet_alerts);

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

            // Mock sending broadcast
            Toast.makeText(this, "Alert for " + disease + " broadcasted to farmers in " + selectedDistricts.size() + " districts!", Toast.LENGTH_LONG).show();
            
            // Reset form
            etDiseaseType.setText("");
            etMessage.setText("");
            for (CheckBox cb : districtCheckBoxes) cb.setChecked(false);
        });

        findViewById(R.id.tv_back_dashboard).setOnClickListener(v -> finish());
    }

    private void setupNavigation() {
        findViewById(R.id.layout_home_tab).setOnClickListener(v -> {
            startActivity(new Intent(this, VetDashboardActivity.class));
            finish();
        });

        findViewById(R.id.layout_cases_tab).setOnClickListener(v -> {
            startActivity(new Intent(this, VetCasesActivity.class));
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
