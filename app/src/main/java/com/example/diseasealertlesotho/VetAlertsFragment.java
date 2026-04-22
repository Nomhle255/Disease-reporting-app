package com.example.diseasealertlesotho;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class VetAlertsFragment extends Fragment {

    private EditText etDiseaseType, etMessage;
    private MaterialButton btnSend;
    private List<CheckBox> districtCheckBoxes = new ArrayList<>();
    private DatabaseHelper dbHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_vet_alerts, container, false);

        dbHelper = new DatabaseHelper(requireContext());
        initViews(view);
        setupClickListeners();

        return view;
    }

    private void initViews(View view) {
        etDiseaseType = view.findViewById(R.id.et_disease_type);
        etMessage = view.findViewById(R.id.et_alert_message);
        btnSend = view.findViewById(R.id.btn_send_alert);

        // Initialize checkboxes
        districtCheckBoxes.add(view.findViewById(R.id.cb_maseru));
        districtCheckBoxes.add(view.findViewById(R.id.cb_leribe));
        districtCheckBoxes.add(view.findViewById(R.id.cb_berea));
        districtCheckBoxes.add(view.findViewById(R.id.cb_mafeteng));
        districtCheckBoxes.add(view.findViewById(R.id.cb_mohale));
        districtCheckBoxes.add(view.findViewById(R.id.cb_quthing));
        districtCheckBoxes.add(view.findViewById(R.id.cb_qacha));
        districtCheckBoxes.add(view.findViewById(R.id.cb_mokhotlong));
        districtCheckBoxes.add(view.findViewById(R.id.cb_thaba));
        districtCheckBoxes.add(view.findViewById(R.id.cb_butha));
    }

    private void setupClickListeners() {
        btnSend.setOnClickListener(v -> {
            String disease = etDiseaseType.getText().toString().trim();
            String message = etMessage.getText().toString().trim();

            if (disease.isEmpty() || message.isEmpty()) {
                Toast.makeText(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            List<String> selectedDistricts = new ArrayList<>();
            for (CheckBox cb : districtCheckBoxes) {
                if (cb.isChecked()) {
                    selectedDistricts.add(cb.getText().toString());
                }
            }

            if (selectedDistricts.isEmpty()) {
                Toast.makeText(getContext(), "Please select at least one district", Toast.LENGTH_SHORT).show();
                return;
            }

            sendAlerts(disease, message, selectedDistricts);
        });
    }

    private void sendAlerts(String disease, String message, List<String> districts) {
        try {
            String title = "DISEASE ALERT: " + disease;
            
            for (String district : districts) {
                String target = "DISTRICT:" + district;
                
                NotificationHelper.showNotification(
                        requireContext(),
                        title,
                        message,
                        FarmerDashboardActivity.class,
                        target,
                        "ALERT"
                );
            }

            Toast.makeText(getContext(), "Alerts broadcasted successfully!", Toast.LENGTH_SHORT).show();
            
            // Reset form
            etDiseaseType.setText("");
            etMessage.setText("");
            for (CheckBox cb : districtCheckBoxes) cb.setChecked(false);

        } catch (Exception e) {
            Toast.makeText(getContext(), "Error sending alerts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
