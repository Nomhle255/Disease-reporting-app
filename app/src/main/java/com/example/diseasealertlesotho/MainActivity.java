package com.example.diseasealertlesotho;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName, etEmail, etPhone, etPassword, etConfirmPassword;
    private AutoCompleteTextView actRole;
    private MaterialButton btnCreateAccount;
    private TextView tvLogin;
    private SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupDropdowns();
        initDatabase();

        btnCreateAccount.setOnClickListener(v -> {
            if (validateForm()) {
                saveToDatabase();
            }
        });

        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void initDatabase() {
        db = openOrCreateDatabase("DiseaseAlertDB", Context.MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS users(" +
                "phone VARCHAR PRIMARY KEY, " +
                "firstname VARCHAR, " +
                "lastname VARCHAR, " +
                "email VARCHAR, " +
                "role VARCHAR, " +
                "password VARCHAR);");

        db.execSQL("CREATE TABLE IF NOT EXISTS reports(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_phone VARCHAR, " +
                "animal_type VARCHAR, " +
                "count INTEGER, " +
                "symptoms TEXT, " +
                "date VARCHAR, " +
                "photo BLOB, " +
                "status VARCHAR DEFAULT 'Pending');");

        db.execSQL("CREATE TABLE IF NOT EXISTS responses(" +
                "response_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "report_id INTEGER, " +
                "vet_phone VARCHAR, " +
                "farmer_phone VARCHAR, " +
                "response TEXT, " +
                "status VARCHAR DEFAULT 'Resolved', " +
                "date VARCHAR, " +
                "FOREIGN KEY(report_id) REFERENCES reports(id), " +
                "FOREIGN KEY(vet_phone) REFERENCES users(phone), " +
                "FOREIGN KEY(farmer_phone) REFERENCES users(phone));");
        
        // Ensure columns exist if table was created previously
        try {
            db.execSQL("ALTER TABLE reports ADD COLUMN status VARCHAR DEFAULT 'Pending'");
        } catch (Exception ignored) {}
    }

    private void initViews() {
        etFirstName = findViewById(R.id.et_first_name);
        etLastName = findViewById(R.id.et_last_name);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);

        actRole = findViewById(R.id.act_role);
        btnCreateAccount = findViewById(R.id.btn_create_account);
        tvLogin = findViewById(R.id.tv_login);
    }

    private void setupDropdowns() {
        String[] roles = getResources().getStringArray(R.array.roles_array);
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, roles);
        actRole.setAdapter(roleAdapter);
        actRole.setText(roles[0], false);
    }

    private void saveToDatabase() {
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String fName = etFirstName.getText().toString().trim();
        String lName = etLastName.getText().toString().trim();
        String role = actRole.getText().toString().trim();
        String pwd = etPassword.getText().toString().trim();

        // Check for duplicate phone
        Cursor cursor = db.rawQuery("SELECT * FROM users WHERE phone = ?", new String[]{phone});
        if (cursor.getCount() > 0) {
            showMessage("Error", "User with this phone number already exists");
            cursor.close();
            return;
        }
        cursor.close();

        try {
            db.execSQL("INSERT INTO users VALUES(?, ?, ?, ?, ?, ?)",
                    new Object[]{phone, fName, lName, email, role, pwd});
            Toast.makeText(this, "Account created successfully", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        } catch (Exception e) {
            showMessage("Error", "Registration failed: " + e.getMessage());
        }
    }

    private boolean validateForm() {
        if (etFirstName.getText().toString().trim().isEmpty()) {
            etFirstName.setError("Required");
            return false;
        }
        if (etLastName.getText().toString().trim().isEmpty()) {
            etLastName.setError("Required");
            return false;
        }
        if (etPhone.getText().toString().trim().isEmpty()) {
            etPhone.setError("Required");
            return false;
        }
        if (etEmail.getText().toString().trim().isEmpty()) {
            etEmail.setError("Required");
            return false;
        }
        if (etPassword.getText().toString().length() < 6) {
            etPassword.setError("Min 6 chars");
            return false;
        }
        if (!etPassword.getText().toString().equals(etConfirmPassword.getText().toString())) {
            etConfirmPassword.setError("Mismatch");
            return false;
        }
        return true;
    }

    private void showMessage(String title, String message) {
        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle(title)
                .setMessage(message)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (db != null && db.isOpen()) {
            db.close();
        }
    }
}