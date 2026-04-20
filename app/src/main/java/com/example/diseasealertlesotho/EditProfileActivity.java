package com.example.diseasealertlesotho;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName, etEmail, etPhone;
    private AutoCompleteTextView actDistrict;
    private MaterialButton btnSave;
    private SQLiteDatabase db;
    private String emailSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        db = openOrCreateDatabase("DiseaseAlertDB", Context.MODE_PRIVATE, null);
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        emailSession = prefs.getString("email", "");
        
        // Backwards compatibility: if email is missing in session, try to find it via phone
        if (emailSession.isEmpty()) {
            String phoneSession = prefs.getString("phone", "");
            if (!phoneSession.isEmpty()) {
                try {
                    Cursor c = db.rawQuery("SELECT email FROM users WHERE phone = ?", new String[]{phoneSession});
                    if (c.moveToFirst()) emailSession = c.getString(0);
                    c.close();
                } catch (Exception ignored) {}
            }
        }

        initViews();
        setupDropdown();
        loadCurrentData();

        findViewById(R.id.tv_back).setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void initViews() {
        etFirstName = findViewById(R.id.et_edit_first_name);
        etLastName = findViewById(R.id.et_edit_last_name);
        etEmail = findViewById(R.id.et_edit_email);
        etPhone = findViewById(R.id.et_edit_phone);
        actDistrict = findViewById(R.id.act_edit_district);
        btnSave = findViewById(R.id.btn_save_profile);
    }

    private void setupDropdown() {
        String[] districts = getResources().getStringArray(R.array.districts_array);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, districts);
        actDistrict.setAdapter(adapter);
    }

    private void loadCurrentData() {
        if (emailSession.isEmpty()) return;
        try {
            Cursor cursor = db.rawQuery("SELECT * FROM users WHERE email = ?", new String[]{emailSession});
            if (cursor.moveToFirst()) {
                etFirstName.setText(cursor.getString(cursor.getColumnIndexOrThrow("firstname")));
                etLastName.setText(cursor.getString(cursor.getColumnIndexOrThrow("lastname")));
                etEmail.setText(cursor.getString(cursor.getColumnIndexOrThrow("email")));
                etPhone.setText(cursor.getString(cursor.getColumnIndexOrThrow("phone")));
                
                try {
                    String district = cursor.getString(cursor.getColumnIndexOrThrow("district"));
                    if (district != null) actDistrict.setText(district, false);
                } catch (Exception ignored) {}
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveProfile() {
        String fName = etFirstName.getText().toString().trim();
        String lName = etLastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String district = actDistrict.getText().toString().trim();

        if (fName.isEmpty() || lName.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Check if new phone is already taken by someone else
            Cursor c = db.rawQuery("SELECT email FROM users WHERE phone = ? AND email != ?", new String[]{phone, emailSession});
            if (c.getCount() > 0) {
                Toast.makeText(this, "Phone number already in use", Toast.LENGTH_SHORT).show();
                c.close();
                return;
            }
            c.close();

            // Check if new email is already taken by someone else
            c = db.rawQuery("SELECT phone FROM users WHERE email = ? AND email != ?", new String[]{email, emailSession});
            if (c.getCount() > 0) {
                Toast.makeText(this, "Email address already in use", Toast.LENGTH_SHORT).show();
                c.close();
                return;
            }
            c.close();

            db.execSQL("UPDATE users SET firstname=?, lastname=?, email=?, phone=?, district=? WHERE email=?",
                    new Object[]{fName, lName, email, phone, district, emailSession});
            
            // Update Session
            SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("email", email);
            editor.putString("phone", phone);
            editor.putString("firstname", fName);
            editor.putString("lastname", lName);
            editor.putString("district", district);
            editor.apply();
            
            emailSession = email; // Update local tracker

            Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
