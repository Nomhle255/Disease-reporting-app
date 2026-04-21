package com.example.diseasealertlesotho;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Color;

import com.google.android.material.button.MaterialButton;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class FarmerReportDetailsActivity extends AppCompatActivity {

    private TextView tvSubtitle, tvStatusBadge;
    private TextView tvAnimalType, tvAffectedCount, tvDateObserved, tvSymptoms;
    private ImageView ivReportPhoto, ivNewPhotoPreview;
    private LinearLayout layoutConversationContainer, layoutEmptyThread, layoutInputArea, btnUploadNewPhoto;
    private EditText etFarmerReply;
    private MaterialButton btnSendReply;

    private SQLiteDatabase db;
    private int reportId = -1;
    private String currentStatus = "";
    private byte[] newImageByteArray = null;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        InputStream is = getContentResolver().openInputStream(imageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(is);
                        ivNewPhotoPreview.setImageBitmap(bitmap);
                        ivNewPhotoPreview.setVisibility(View.VISIBLE);
                        newImageByteArray = bitmapToByteArray(bitmap);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farmer_report_history);

        db = openOrCreateDatabase("DiseaseAlertDB", Context.MODE_PRIVATE, null);

        initViews();
        loadReport();
    }

    private void initViews() {
        tvSubtitle      = findViewById(R.id.tv_subtitle);
        tvStatusBadge   = findViewById(R.id.tv_status_badge);
        tvAnimalType    = findViewById(R.id.tv_animal_type);
        tvAffectedCount = findViewById(R.id.tv_affected_count);
        tvDateObserved  = findViewById(R.id.tv_date_observed);
        tvSymptoms      = findViewById(R.id.tv_symptoms);
        ivReportPhoto   = findViewById(R.id.iv_report_photo_details);

        layoutConversationContainer = findViewById(R.id.layout_conversation_container_farmer);
        layoutEmptyThread           = findViewById(R.id.layout_empty_thread);
        layoutInputArea             = findViewById(R.id.layout_input_area);
        etFarmerReply               = findViewById(R.id.et_farmer_reply);
        btnSendReply                = findViewById(R.id.btn_send_reply);
        
        btnUploadNewPhoto           = findViewById(R.id.btn_upload_new_photo);
        ivNewPhotoPreview           = findViewById(R.id.iv_new_photo_preview);

        findViewById(R.id.tv_back).setOnClickListener(v -> finish());
        btnSendReply.setOnClickListener(v -> sendReply());
        btnUploadNewPhoto.setOnClickListener(v -> openGallery());
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

    private void loadReport() {
        reportId = getIntent().getIntExtra("REPORT_ID", -1);
        if (reportId == -1) { finish(); return; }

        try {
            Cursor cursor = db.rawQuery("SELECT * FROM reports WHERE id = ?", new String[]{String.valueOf(reportId)});
            if (cursor.moveToFirst()) {
                String animal   = cursor.getString(cursor.getColumnIndexOrThrow("animal_type"));
                int count       = cursor.getInt(cursor.getColumnIndexOrThrow("count"));
                String symptoms = cursor.getString(cursor.getColumnIndexOrThrow("symptoms"));
                String date     = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                currentStatus   = cursor.getString(cursor.getColumnIndexOrThrow("status"));
                byte[] photo    = cursor.getBlob(cursor.getColumnIndexOrThrow("photo"));

                tvSubtitle.setText("Report #" + reportId);
                tvAnimalType.setText(animal);
                tvAffectedCount.setText(count + " animals");
                tvDateObserved.setText(date);
                tvSymptoms.setText(symptoms);

                if (photo != null && photo.length > 0 && ivReportPhoto != null) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(photo, 0, photo.length);
                    ivReportPhoto.setImageBitmap(bitmap);
                }

                updateStatusBadge(currentStatus != null ? currentStatus : "Pending");
                loadConversationThread();
            }
            cursor.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadConversationThread() {
        if (layoutConversationContainer == null) return;
        layoutConversationContainer.removeAllViews();
        try {
            String query =
                "SELECT 'vet' AS sender, v.firstname, v.lastname, r.response_type, r.message, NULL AS photo, r.date_responded AS msg_date " +
                "FROM responses r LEFT JOIN users v ON r.vet_phone = v.phone WHERE r.report_id = ? " +
                "UNION ALL " +
                "SELECT 'farmer' AS sender, '' AS firstname, '' AS lastname, 'Farmer reply' AS response_type, farmer_message AS message, photo, date_submitted AS msg_date " +
                "FROM more_info WHERE report_id = ? " +
                "ORDER BY msg_date ASC";

            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(reportId), String.valueOf(reportId)});

            if (cursor.getCount() == 0) {
                layoutEmptyThread.setVisibility(View.VISIBLE);
                layoutInputArea.setVisibility(View.GONE);
            } else {
                layoutEmptyThread.setVisibility(View.GONE);
                layoutInputArea.setVisibility(currentStatus.equalsIgnoreCase("Investigating") ? View.VISIBLE : View.GONE);

                while (cursor.moveToNext()) {
                    String sender = cursor.getString(cursor.getColumnIndexOrThrow("sender"));
                    String message = cursor.getString(cursor.getColumnIndexOrThrow("message"));
                    String date = cursor.getString(cursor.getColumnIndexOrThrow("msg_date"));
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
                        if (sender.equals("farmer")) {
                            tvTp.setText("Your reply");
                        } else {
                            String vFirst = cursor.getString(cursor.getColumnIndexOrThrow("firstname"));
                            String vLast = cursor.getString(cursor.getColumnIndexOrThrow("lastname"));
                            String vetName = (vFirst != null) ? "Dr. " + vFirst + " " + vLast : "Dr. Vet";
                            tvTp.setText(vetName);
                        }
                    }

                    if (photoBytes != null && photoBytes.length > 0 && ivBubblePhoto != null) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.length);
                        ivBubblePhoto.setImageBitmap(bitmap);
                        cardPhoto.setVisibility(View.VISIBLE);
                    }

                    layoutConversationContainer.addView(chatView);
                }
            }
            cursor.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void sendReply() {
        String reply = etFarmerReply.getText().toString().trim();
        if (reply.isEmpty() && newImageByteArray == null) return;

        try {
            SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
            String phone = prefs.getString("phone", "");
            String farmerName = prefs.getString("name", "A Farmer");
            
            db.execSQL("INSERT INTO more_info (report_id, farmer_phone, farmer_message, photo, date_submitted) VALUES (?, ?, ?, ?, datetime('now'))",
                    new Object[]{reportId, phone, reply, newImageByteArray});
            
            // Notify Veterinary Officers
            NotificationHelper.showNotification(
                this,
                "New message from " + farmerName,
                "Farmer has replied to report #" + reportId,
                VetCasesActivity.class,
                "Vet",
                "MESSAGE"
            );

            etFarmerReply.setText("");
            ivNewPhotoPreview.setVisibility(View.GONE);
            newImageByteArray = null;
            loadConversationThread();
            Toast.makeText(this, "Reply sent to vet.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void updateStatusBadge(String status) {
        tvStatusBadge.setText(status);
        if (status.equalsIgnoreCase("Pending")) {
            tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_pending);
            tvStatusBadge.setTextColor(Color.parseColor("#F57F17"));
        } else if (status.equalsIgnoreCase("Investigating")) {
            tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_investigating);
            tvStatusBadge.setTextColor(Color.parseColor("#1565C0"));
        } else if (status.equalsIgnoreCase("Resolved")) {
            tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_resolved);
            tvStatusBadge.setTextColor(Color.parseColor("#2E7D32"));
        }
    }
}