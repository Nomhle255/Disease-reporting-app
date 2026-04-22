package com.example.diseasealertlesotho;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Calendar;

public class ReportDiseaseActivity extends AppCompatActivity {

    ImageView btnBack, ivPreview;
    EditText etDate, etAnimalCount, etSymptoms;
    AutoCompleteTextView actAnimalType;
    MaterialButton btnSubmit, btnCaptureGps;
    LinearLayout btnUploadPhoto, layoutUploadPlaceholder;
    TextView tvGpsLocation;
    ProgressBar pbGpsLoading;

    DatabaseHelper dbHelper;
    byte[] imageByteArray = null;
    String gpsLocation = "Unknown";

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        InputStream is = getContentResolver().openInputStream(imageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(is);
                        ivPreview.setImageBitmap(bitmap);
                        ivPreview.setVisibility(View.VISIBLE);
                        layoutUploadPlaceholder.setVisibility(View.GONE);
                        
                        imageByteArray = bitmapToByteArray(bitmap);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

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

        dbHelper = new DatabaseHelper(this);

        initViews();
        setupAnimalTypeDropdown();

        btnBack.setOnClickListener(v -> finish());
        etDate.setOnClickListener(v -> showDatePickerDialog());
        btnUploadPhoto.setOnClickListener(v -> checkPermissionAndOpenGallery());
        btnSubmit.setOnClickListener(v -> submitReport());
        btnCaptureGps.setOnClickListener(v -> requestLocation());
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        etDate = findViewById(R.id.et_date);
        etAnimalCount = findViewById(R.id.et_animal_count);
        etSymptoms = findViewById(R.id.et_symptoms);
        actAnimalType = findViewById(R.id.act_animal_type);
        btnSubmit = findViewById(R.id.btn_submit_report);
        btnUploadPhoto = findViewById(R.id.btn_upload_photo);
        ivPreview = findViewById(R.id.iv_preview);
        layoutUploadPlaceholder = findViewById(R.id.layout_upload_placeholder);
        tvGpsLocation = findViewById(R.id.tv_gps_location);
        pbGpsLoading = findViewById(R.id.pb_gps_loading);
        btnCaptureGps = findViewById(R.id.btn_capture_gps);
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
                (view, year1, month1, dayOfMonth) -> {
                    String selectedDate = dayOfMonth + "/" + (month1 + 1) + "/" + year1;
                    etDate.setText(selectedDate);
                }, year, month, day);
        datePickerDialog.show();
    }

    private void requestLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 103);
        } else {
            fetchGPS();
        }
    }

    private void fetchGPS() {
        try {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null) {
                pbGpsLoading.setVisibility(View.VISIBLE);
                btnCaptureGps.setVisibility(View.GONE);
                tvGpsLocation.setText("Capturing...");

                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        gpsLocation = location.getLatitude() + ", " + location.getLongitude();
                        tvGpsLocation.setText(gpsLocation);
                        pbGpsLoading.setVisibility(View.GONE);
                        btnCaptureGps.setVisibility(View.VISIBLE);
                        btnCaptureGps.setText("Recapture");
                    }
                    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
                    @Override public void onProviderEnabled(@NonNull String provider) {}
                    @Override public void onProviderDisabled(@NonNull String provider) {
                        tvGpsLocation.setText("GPS Disabled");
                        pbGpsLoading.setVisibility(View.GONE);
                        btnCaptureGps.setVisibility(View.VISIBLE);
                    }
                }, null);
            }
        } catch (SecurityException e) {
            e.printStackTrace();
            tvGpsLocation.setText("Permission Denied");
            pbGpsLoading.setVisibility(View.GONE);
            btnCaptureGps.setVisibility(View.VISIBLE);
        }
    }

    private void checkPermissionAndOpenGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 102);
            } else {
                openGallery();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 102);
            } else {
                openGallery();
            }
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream);
        return stream.toByteArray();
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

        if (gpsLocation.equals("Unknown") || gpsLocation.equals("Not captured")) {
            Toast.makeText(this, "Please capture your GPS location", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences sp = getSharedPreferences("UserSession", MODE_PRIVATE);
        String phone = sp.getString("phone", "Unknown");

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("user_phone", phone);
        cv.put("animal_type", type);
        cv.put("count", Integer.parseInt(countStr));
        cv.put("symptoms", symptoms);
        cv.put("date", date);
        cv.put("status", "Pending");
        cv.put("gps_location", gpsLocation);
        if (imageByteArray != null) {
            cv.put("photo", imageByteArray);
        }

        long id = db.insert("reports", null, cv);

        if (id != -1) {
            saveToHistoryFile(type, countStr, symptoms, date);
            
            NotificationHelper.showNotification(
                this, 
                "New Report Submitted", 
                "A new " + type + " disease report has been received.", 
                VetDashboardActivity.class,
                "Vet", 
                "Vet"
            );

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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 102 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else if (requestCode == 103 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchGPS();
        }
    }
}
