package com.example.diseasealertlesotho;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AdminReportDetailsActivity extends AppCompatActivity {

    private TextView tvSubtitle, tvStatusBadge, tvFarmerName, tvReportSummary, tvSymptoms;
    private ImageView ivReportPhoto;
    private LinearLayout layoutConversationContainer, layoutEmptyThread;
    private SQLiteDatabase db;
    private int reportId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_report_details);

        db = openOrCreateDatabase("DiseaseAlertDB", Context.MODE_PRIVATE, null);
        initViews();
        loadData();
    }

    private void initViews() {
        tvSubtitle = findViewById(R.id.tv_subtitle);
        tvStatusBadge = findViewById(R.id.tv_status_badge);
        tvFarmerName = findViewById(R.id.tv_farmer_name);
        tvReportSummary = findViewById(R.id.tv_report_summary);
        tvSymptoms = findViewById(R.id.tv_symptoms);
        ivReportPhoto = findViewById(R.id.iv_report_photo);
        layoutConversationContainer = findViewById(R.id.layout_conversation_container);
        layoutEmptyThread = findViewById(R.id.layout_empty_thread);

        findViewById(R.id.tv_back).setOnClickListener(v -> finish());
    }

    private void loadData() {
        reportId = getIntent().getIntExtra("REPORT_ID", -1);
        if (reportId == -1) {
            finish();
            return;
        }

        try {
            String query = "SELECT r.*, u.firstname, u.lastname FROM reports r " +
                    "LEFT JOIN users u ON r.user_phone = u.phone " +
                    "WHERE r.id = ?";
            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(reportId)});

            if (cursor.moveToFirst()) {
                String fName = cursor.getString(cursor.getColumnIndexOrThrow("firstname"));
                String lName = cursor.getString(cursor.getColumnIndexOrThrow("lastname"));
                String animal = cursor.getString(cursor.getColumnIndexOrThrow("animal_type"));
                int count = cursor.getInt(cursor.getColumnIndexOrThrow("count"));
                String symptoms = cursor.getString(cursor.getColumnIndexOrThrow("symptoms"));
                String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
                byte[] photo = cursor.getBlob(cursor.getColumnIndexOrThrow("photo"));

                String farmerFull = (fName != null) ? fName + " " + lName : "Unknown Farmer";
                tvSubtitle.setText("Report #" + reportId);
                tvFarmerName.setText(farmerFull);
                tvReportSummary.setText(animal + " · " + count + " animals");
                tvSymptoms.setText(symptoms);

                if (photo != null && photo.length > 0) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(photo, 0, photo.length);
                    ivReportPhoto.setImageBitmap(bitmap);
                }

                updateStatusBadge(status != null ? status : "Pending");
                loadConversationThread();
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadConversationThread() {
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
                layoutConversationContainer.addView(layoutEmptyThread);
            } else {
                layoutEmptyThread.setVisibility(View.GONE);
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
                        if (sender.equals("vet")) {
                            String vFirst = cursor.getString(cursor.getColumnIndexOrThrow("firstname"));
                            String vLast = cursor.getString(cursor.getColumnIndexOrThrow("lastname"));
                            tvTp.setText((vFirst != null) ? "Dr. " + vFirst + " " + vLast : "Veterinary Doctor");
                        } else {
                            tvTp.setText("Farmer");
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
        } else {
            tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_pending);
            tvStatusBadge.setTextColor(Color.parseColor("#757575"));
        }
    }
}
