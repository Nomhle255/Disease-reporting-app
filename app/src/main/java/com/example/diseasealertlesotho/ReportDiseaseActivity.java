package com.example.diseasealertlesotho;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import java.io.FileOutputStream;
import java.util.Calendar;

public class ReportDiseaseActivity extends AppCompatActivity {

    ImageView btnBack;
    EditText etDate, etAnimalCount, etSymptoms;
    AutoCompleteTextView actAnimalType;
    MaterialButton btnSubmit;
    SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_report_disease);

        View mainView = findViewById(android.R.id.content);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        db = openOrCreateDatabase("DiseaseAlertDB", Context.MODE_PRIVATE, null);

        initViews();
        setupAnimalTypeDropdown();

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        etDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }
        });

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitReport();
            }
        });
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        etDate = findViewById(R.id.et_date);
        etAnimalCount = findViewById(R.id.et_animal_count);
        etSymptoms = findViewById(R.id.et_symptoms);
        actAnimalType = findViewById(R.id.act_animal_type);
        btnSubmit = findViewById(R.id.btn_submit_report);
    }

    private void setupAnimalTypeDropdown() {
        String[] animalTypes = {"Cattle", "Sheep", "Goats", "Poultry"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, animalTypes);
        actAnimalType.setAdapter(adapter);
    }

    private void showDatePickerDialog() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        String selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
                        etDate.setText(selectedDate);
                    }
                }, year, month, day);
        datePickerDialog.show();
    }

    private void submitReport() {
        String type = actAnimalType.getText().toString();
        String countStr = etAnimalCount.getText().toString();
        String symptoms = etSymptoms.getText().toString();
        String date = etDate.getText().toString();

        if (type.isEmpty() || countStr.isEmpty() || symptoms.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences sp = getSharedPreferences("UserSession", MODE_PRIVATE);
        String phone = sp.getString("phone", "Unknown");

        ContentValues cv = new ContentValues();
        cv.put("user_phone", phone);
        cv.put("animal_type", type);
        cv.put("count", Integer.parseInt(countStr));
        cv.put("symptoms", symptoms);
        cv.put("date", date);
        cv.put("status", "Pending");

        long id = db.insert("reports", null, cv);

        if (id != -1) {
            saveToHistoryFile(type, countStr, symptoms, date);
            Toast.makeText(this, "Report submitted successfully", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to submit report", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveToHistoryFile(String type, String count, String symptoms, String date) {
        String filename = "report_history.txt";
        String fileContents = "Type: " + type + ", Count: " + count + ", Symptoms: " + symptoms + ", Date: " + date + "\n";
        try (FileOutputStream fos = openFileOutput(filename, Context.MODE_APPEND)) {
            fos.write(fileContents.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}