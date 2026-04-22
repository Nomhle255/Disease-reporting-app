package com.example.diseasealertlesotho;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Color;

import com.google.android.material.button.MaterialButton;

import java.util.Calendar;
import java.util.Locale;

public class VetCaseDetailsActivity extends AppCompatActivity {

    private TextView tvSubtitle, tvStatusBadge;
    private TextView tvAnimalType, tvAffectedCount, tvDateObserved, tvSymptoms, tvGpsLocation;
    private ImageView ivReportPhoto;
    private LinearLayout layoutConversationContainer, layoutEmptyThread;
    private Spinner spinnerResponseType;
    private EditText etResponseMessage, etVisitDate;
    private MaterialButton btnSendResponse;

    private DatabaseHelper dbHelper;
    private String farmerPhone = "";
    private String farmerName = "Farmer";
    private int reportId = -1;
    private String selectedResponseType = "Request more information";

    private static final String TYPE_MORE_INFO   = "Request more information";
    private static final String TYPE_ADVICE      = "Provide advice / treatment";
    private static final String TYPE_VISIT       = "Schedule farm visit";

    // Status values consistent with the rest of the app
    private static final String STATUS_PENDING        = "Pending";
    private static final String STATUS_INVESTIGATING  = "Investigating";
    private static final String STATUS_ADVICE         = "Advice Provided";
    private static final String STATUS_VISIT          = "Scheduled";
    private static final String STATUS_RESOLVED       = "Resolved";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vet_case_details);

        dbHelper = new DatabaseHelper(this);

        initViews();
        setupSpinner();
        setupData();
        setupClickListeners();
    }

    private void initViews() {
        tvSubtitle      = findViewById(R.id.tv_subtitle);
        tvStatusBadge   = findViewById(R.id.tv_status_badge);
        tvAnimalType    = findViewById(R.id.tv_animal_type);
        tvAffectedCount = findViewById(R.id.tv_affected_count);
        tvDateObserved  = findViewById(R.id.tv_date_observed);
        tvSymptoms      = findViewById(R.id.tv_symptoms);
        tvGpsLocation   = findViewById(R.id.tv_gps_location);
        ivReportPhoto   = findViewById(R.id.iv_report_photo);

        layoutConversationContainer = findViewById(R.id.layout_conversation_container);
        layoutEmptyThread           = findViewById(R.id.layout_empty_thread);

        spinnerResponseType = findViewById(R.id.spinner_response_type);
        etVisitDate         = findViewById(R.id.et_visit_date);
        etResponseMessage   = findViewById(R.id.et_response_message);
        btnSendResponse     = findViewById(R.id.btn_send_response);

        findViewById(R.id.tv_back).setOnClickListener(v -> finish());
    }

    private void setupSpinner() {
        String[] responseTypes = {TYPE_MORE_INFO, TYPE_ADVICE, TYPE_VISIT};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, R.layout.spinner_item_black, responseTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerResponseType.setAdapter(adapter);

        spinnerResponseType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedResponseType = responseTypes[position];
                updateUIForResponseType();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateUIForResponseType() {
        if (TYPE_VISIT.equals(selectedResponseType)) {
            etVisitDate.setVisibility(View.VISIBLE);
            etResponseMessage.setHint("Provide visit details (e.g. arrival time)...");
        } else {
            etVisitDate.setVisibility(View.GONE);
            if (TYPE_MORE_INFO.equals(selectedResponseType)) {
                etResponseMessage.setHint("Type the question for the farmer...");
            } else {
                etResponseMessage.setHint("Type your advice/treatment...");
            }
        }
    }

    private void setupData() {
        String caseIdStr = getIntent().getStringExtra("CASE_ID");
        if (caseIdStr != null && caseIdStr.startsWith("RPT-")) {
            try {
                reportId = Integer.parseInt(caseIdStr.substring(4));
                loadReportFromDB(reportId);
                loadConversationThread(reportId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void loadReportFromDB(int id) {
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String query = "SELECT r.*, u.firstname, u.lastname FROM reports r " +
                    "LEFT JOIN users u ON r.user_phone = u.phone " +
                    "WHERE r.id = ?";
            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(id)});

            if (cursor.moveToFirst()) {
                String fName    = cursor.getString(cursor.getColumnIndexOrThrow("firstname"));
                String lName    = cursor.getString(cursor.getColumnIndexOrThrow("lastname"));
                String animal   = cursor.getString(cursor.getColumnIndexOrThrow("animal_type"));
                int count       = cursor.getInt(cursor.getColumnIndexOrThrow("count"));
                String symptoms = cursor.getString(cursor.getColumnIndexOrThrow("symptoms"));
                String date     = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                String status   = cursor.getString(cursor.getColumnIndexOrThrow("status"));
                String gps      = cursor.getString(cursor.getColumnIndexOrThrow("gps_location"));
                farmerPhone     = cursor.getString(cursor.getColumnIndexOrThrow("user_phone"));
                byte[] photo    = cursor.getBlob(cursor.getColumnIndexOrThrow("photo"));

                farmerName = (fName != null) ? fName + " " + lName : "Unknown Farmer";
                tvSubtitle.setText("Report #" + id + " · Submitted by " + farmerName);

                tvAnimalType.setText(animal != null ? animal : "—");
                tvAffectedCount.setText(count + " animals");
                tvDateObserved.setText(date != null ? date : "—");
                tvSymptoms.setText((symptoms != null && !symptoms.isEmpty()) ? symptoms : "No description.");
                tvGpsLocation.setText((gps != null && !gps.isEmpty()) ? gps : "Location not captured");

                if (photo != null && photo.length > 0) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(photo, 0, photo.length);
                    ivReportPhoto.setImageBitmap(bitmap);
                }

                updateStatusBadge(status != null ? status : STATUS_PENDING);
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadConversationThread(int id) {
        layoutConversationContainer.removeAllViews();
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String query =
                "SELECT 'vet' AS sender, response_type, message, NULL AS photo, date_responded AS msg_date " +
                "FROM responses WHERE report_id = ? " +
                "UNION ALL " +
                "SELECT 'farmer' AS sender, 'Farmer reply' AS response_type, farmer_message AS message, photo, date_submitted AS msg_date " +
                "FROM more_info WHERE report_id = ? " +
                "ORDER BY msg_date ASC";

            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(id), String.valueOf(id)});

            if (cursor.getCount() == 0) {
                layoutConversationContainer.addView(layoutEmptyThread);
            } else {
                layoutEmptyThread.setVisibility(View.GONE);
                while (cursor.moveToNext()) {
                    String sender = cursor.getString(cursor.getColumnIndexOrThrow("sender"));
                    String message = cursor.getString(cursor.getColumnIndexOrThrow("message"));
                    String date = cursor.getString(cursor.getColumnIndexOrThrow("msg_date"));
                    String type = cursor.getString(cursor.getColumnIndexOrThrow("response_type"));
                    byte[] photoBytes = cursor.getBlob(cursor.getColumnIndexOrThrow("photo"));

                    int layoutRes = sender.equals("vet") ? R.layout.item_bubble_vet : R.layout.item_bubble_farmer;
                    View chatView = LayoutInflater.from(this).inflate(layoutRes, layoutConversationContainer, false);

                    TextView tvMsg = chatView.findViewById(R.id.tv_bubble_message);
                    TextView tvDt = chatView.findViewById(R.id.tv_bubble_meta);
                    TextView tvTp = chatView.findViewById(R.id.tv_bubble_tag);
                    ImageView ivBubblePhoto = chatView.findViewById(R.id.iv_bubble_image);
                    View cardPhoto = chatView.findViewById(R.id.card_bubble_image);

                    tvMsg.setText(message);
                    tvDt.setText(date);
                    if (tvTp != null) {
                        if (sender.equals("vet")) {
                            tvTp.setText("Your reply");
                        } else {
                            tvTp.setText(farmerName);
                        }
                    }

                    if (photoBytes != null && photoBytes.length > 0 && ivBubblePhoto != null) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.length);
                        ivBubblePhoto.setImageBitmap(bitmap);
                        if (cardPhoto != null) cardPhoto.setVisibility(View.VISIBLE);
                    }

                    layoutConversationContainer.addView(chatView);
                }
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupClickListeners() {
        etVisitDate.setOnClickListener(v -> showDatePicker());

        btnSendResponse.setOnClickListener(v -> {
            String message = etResponseMessage.getText().toString().trim();
            if (message.isEmpty()) return;

            if (TYPE_VISIT.equals(selectedResponseType)) {
                String visitDate = etVisitDate.getText().toString().trim();
                if (visitDate.isEmpty()) {
                    Toast.makeText(this, "Please select a visit date", Toast.LENGTH_SHORT).show();
                    return;
                }
                message = "[SCHEDULED VISIT: " + visitDate + "] " + message;
            }

            String newStatus = resolveStatus(selectedResponseType);
            saveResponse(reportId, farmerPhone, message, selectedResponseType, newStatus);
            
            // Trigger notification to farmer
            NotificationHelper.showNotification(
                this,
                "New Vet Response",
                "A veterinary officer has responded to your report regarding " + tvAnimalType.getText().toString(),
                FarmerDashboardActivity.class,
                farmerPhone,
                "RESPONSE"
            );

            loadConversationThread(reportId);
            etResponseMessage.setText("");
            etVisitDate.setText("");
            updateStatusBadge(newStatus);
            Toast.makeText(this, "Response sent and farmer notified.", Toast.LENGTH_SHORT).show();
        });
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dpd = new DatePickerDialog(this, (view, year1, month1, dayOfMonth) -> {
            String date = String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, month1 + 1, year1);
            etVisitDate.setText(date);
        }, year, month, day);
        dpd.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dpd.show();
    }

    private String resolveStatus(String responseType) {
        switch (responseType) {
            case TYPE_MORE_INFO: return STATUS_INVESTIGATING;
            case TYPE_ADVICE:    return STATUS_ADVICE;
            case TYPE_VISIT:     return STATUS_VISIT;
            default:             return STATUS_INVESTIGATING;
        }
    }

    private void saveResponse(int rId, String fPhone, String message, String responseType, String newStatus) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
            String vetPhone = prefs.getString("phone", "");
            db.execSQL("INSERT INTO responses (report_id, vet_phone, farmer_phone, response_type, message, status_changed_to, date_responded) VALUES (?, ?, ?, ?, ?, ?, datetime('now'))",
                    new Object[]{rId, vetPhone, fPhone, responseType, message, newStatus});
            db.execSQL("UPDATE reports SET status = ? WHERE id = ?", new Object[]{newStatus, rId});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateStatusBadge(String status) {
        tvStatusBadge.setText(status);
        switch (status) {
            case STATUS_PENDING:
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_pending);
                tvStatusBadge.setTextColor(Color.parseColor("#F57F17"));
                break;
            case STATUS_INVESTIGATING:
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_investigating);
                tvStatusBadge.setTextColor(Color.parseColor("#1565C0"));
                break;
            case STATUS_ADVICE:
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_advice);
                tvStatusBadge.setTextColor(Color.parseColor("#4527A0"));
                break;
            case STATUS_VISIT:
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_visit);
                tvStatusBadge.setTextColor(Color.parseColor("#E65100"));
                break;
            case STATUS_RESOLVED:
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_resolved);
                tvStatusBadge.setTextColor(Color.parseColor("#2E7D32"));
                break;
            default:
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_pending);
                tvStatusBadge.setTextColor(Color.parseColor("#757575"));
                break;
        }
    }
}
