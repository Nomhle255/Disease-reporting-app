package com.example.diseasealertlesotho;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

public class ProfileFragment extends Fragment {

    private TextView tvName, tvRoleDistrict, tvPhone, tvEmail, tvDistrict, tvInitials;
    private MaterialButton btnEditProfile, btnLogout;
    private SQLiteDatabase db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        initViews(view);
        loadUserData();

        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EditProfileActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            SharedPreferences prefs = getActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
            prefs.edit().clear().apply();
            
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserData();
    }

    private void initViews(View view) {
        tvName = view.findViewById(R.id.tv_profile_name);
        tvRoleDistrict = view.findViewById(R.id.tv_profile_role_district);
        tvPhone = view.findViewById(R.id.tv_profile_phone);
        tvEmail = view.findViewById(R.id.tv_profile_email);
        tvDistrict = view.findViewById(R.id.tv_profile_district);
        tvInitials = view.findViewById(R.id.tv_profile_initials);
        btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        btnLogout = view.findViewById(R.id.btn_logout);
    }

    private void loadUserData() {
        SharedPreferences prefs = getActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String phoneSession = prefs.getString("phone", "");

        if (phoneSession.isEmpty()) return;

        try {
            db = getActivity().openOrCreateDatabase("DiseaseAlertDB", Context.MODE_PRIVATE, null);
            Cursor cursor = db.rawQuery("SELECT * FROM users WHERE phone = ?", new String[]{phoneSession});

            if (cursor.moveToFirst()) {
                String firstName = cursor.getString(cursor.getColumnIndexOrThrow("firstname"));
                String lastName = cursor.getString(cursor.getColumnIndexOrThrow("lastname"));
                String phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
                String role = cursor.getString(cursor.getColumnIndexOrThrow("role"));
                String email = cursor.getString(cursor.getColumnIndexOrThrow("email"));
                
                String district = "";
                try { district = cursor.getString(cursor.getColumnIndexOrThrow("district")); } catch (Exception ignored) {}

                String fullName = firstName + " " + lastName;
                tvName.setText(fullName);
                tvPhone.setText(phone);
                tvEmail.setText(email);
                tvDistrict.setText(district != null && !district.isEmpty() ? district : "Not set");
                
                String roleDistStr = role + (district != null && !district.isEmpty() ? " · " + district + " District" : "");
                tvRoleDistrict.setText(roleDistStr);
                
                // Set initials
                if (firstName.length() > 0 && lastName.length() > 0) {
                    String initials = (firstName.substring(0, 1) + lastName.substring(0, 1)).toUpperCase();
                    tvInitials.setText(initials);
                } else if (firstName.length() > 0) {
                    tvInitials.setText(firstName.substring(0, 1).toUpperCase());
                }
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
