package com.example.diseasealertlesotho;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

public class FarmerReportHistoryActivity extends AppCompatActivity {

    // ── Header
    private TextView tvSubtitle, tvStatusBadge;

    // ── Report fields
    private TextView tvAnimalType, tvAffectedCount, tvDateObserved, tvSymptoms;

    // ── Conversation thread
    private LinearLayout layoutEmptyThread;
    private RecyclerView rvConversation;

    // ── Input area (only shown when vet requests more info)
    private LinearLayout layoutInputArea;
    private EditText etFarmerReply;
    private MaterialButton btnSendReply;

    // ── DB & session
    private SQLiteDatabase db;
    private int reportId = -1;
    private String currentStatus = "";

    // Status constants — must match what VetCaseDetailsActivity saves
    private static final String STATUS_PENDING       = "Pending Review";
    private static final String STATUS_INVESTIGATING = "Under Investigation";
    private static final String STATUS_ADVICE        = "Advice Provided";
    private static final String STATUS_VISIT         = "Visit Scheduled";
    private static final String STATUS_RESOLVED      = "Case Resolved";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farmer_report_history);

        db = openOrCreateDatabase("DiseaseAlertDB", Context.MODE_PRIVATE, null);

        initViews();
        loadReport();
    }

    // ─────────────────────────────────────────────
    //  View binding
    // ─────────────────────────────────────────────
    private void initViews() {
        tvSubtitle      = findViewById(R.id.tv_subtitle);
        tvStatusBadge   = findViewById(R.id.tv_status_badge);

        tvAnimalType    = findViewById(R.id.tv_animal_type);
        tvAffectedCount = findViewById(R.id.tv_affected_count);
        tvDateObserved  = findViewById(R.id.tv_date_observed);
        tvSymptoms      = findViewById(R.id.tv_symptoms);

        layoutEmptyThread = findViewById(R.id.layout_empty_thread);
        rvConversation    = findViewById(R.id.rv_conversation);
        rvConversation.setLayoutManager(new LinearLayoutManager(this));

        layoutInputArea = findViewById(R.id.layout_input_area);
        etFarmerReply   = findViewById(R.id.et_farmer_reply);
        btnSendReply    = findViewById(R.id.btn_send_reply);

        findViewById(R.id.tv_back).setOnClickListener(v -> finish());

        btnSendReply.setOnClickListener(v -> sendMoreInfo());
    }

    // ─────────────────────────────────────────────
    //  Load report from DB
    // ─────────────────────────────────────────────
    private void loadReport() {
        reportId = getIntent().getIntExtra("REPORT_ID", -1);

        if (reportId == -1) {
            Toast.makeText(this, "Report not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            Cursor cursor = db.rawQuery(
                    "SELECT * FROM reports WHERE id = ?",
                    new String[]{String.valueOf(reportId)});

            if (cursor.moveToFirst()) {
                String animal   = cursor.getString(cursor.getColumnIndexOrThrow("animal_type"));
                int    count    = cursor.getInt(cursor.getColumnIndexOrThrow("count"));
                String symptoms = cursor.getString(cursor.getColumnIndexOrThrow("symptoms"));
                String date     = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                currentStatus   = cursor.getString(cursor.getColumnIndexOrThrow("status"));

                tvSubtitle.setText("Report #" + reportId);
                tvAnimalType.setText(animal != null ? animal : "—");
                tvAffectedCount.setText(count + " animals");
                tvDateObserved.setText(date != null ? date : "—");
                tvSymptoms.setText((symptoms != null && !symptoms.isEmpty())
                        ? symptoms : "No description provided.");

                updateStatusBadge(currentStatus != null ? currentStatus : STATUS_PENDING);
            }
            cursor.close();

            // Load conversation after report details are set
            loadConversationThread();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading report.", Toast.LENGTH_SHORT).show();
        }
    }

    // ─────────────────────────────────────────────
    //  Load conversation thread
    // ─────────────────────────────────────────────
    private void loadConversationThread() {
        try {
            Cursor countCursor = db.rawQuery(
                    "SELECT COUNT(*) FROM responses WHERE report_id = ?",
                    new String[]{String.valueOf(reportId)});

            int responseCount = 0;
            if (countCursor.moveToFirst()) {
                responseCount = countCursor.getInt(0);
            }
            countCursor.close();

            if (responseCount == 0) {
                // No vet response yet — show waiting state
                layoutEmptyThread.setVisibility(View.VISIBLE);
                rvConversation.setVisibility(View.GONE);
                layoutInputArea.setVisibility(View.GONE);
            } else {
                // Vet has responded — show thread
                layoutEmptyThread.setVisibility(View.GONE);
                rvConversation.setVisibility(View.VISIBLE);
                populateConversation();

                // Only show the reply input if vet's last action was requesting more info
                // i.e. status is still Under Investigation
                if (STATUS_INVESTIGATING.equals(currentStatus)) {
                    layoutInputArea.setVisibility(View.VISIBLE);
                } else {
                    layoutInputArea.setVisibility(View.GONE);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void populateConversation() {
        // Same UNION query as vet side — combines vet responses and farmer
        // more_info replies in chronological order
        String query =
                "SELECT 'vet' AS sender, response_type, message, date_responded AS msg_date " +
                        "FROM responses WHERE report_id = ? " +
                        "UNION ALL " +
                        "SELECT 'farmer' AS sender, 'Farmer reply' AS response_type, farmer_message AS message, date_submitted AS msg_date " +
                        "FROM more_info WHERE report_id = ? " +
                        "ORDER BY msg_date ASC";

        Cursor cursor = db.rawQuery(query, new String[]{
                String.valueOf(reportId), String.valueOf(reportId)});

        ConversationAdapter adapter = new ConversationAdapter(this, cursor);
        rvConversation.setAdapter(adapter);

        // Scroll to bottom so farmer sees the latest message
        rvConversation.scrollToPosition(adapter.getItemCount() - 1);
    }

    // ─────────────────────────────────────────────
    //  Farmer sends more info back to vet
    // ─────────────────────────────────────────────
    private void sendMoreInfo() {
        String reply = etFarmerReply.getText().toString().trim();

        if (reply.isEmpty()) {
            etFarmerReply.setError("Please type your reply before sending.");
            return;
        }

        try {
            SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
            String farmerPhone = prefs.getString("phone", "");

            // Get the response_id of the latest "Request more information" from the vet
            // so we can link this reply to the correct vet question
            Cursor responseCursor = db.rawQuery(
                    "SELECT id FROM responses WHERE report_id = ? AND response_type = ? ORDER BY date_responded DESC LIMIT 1",
                    new String[]{String.valueOf(reportId), "Request more information"});

            int latestResponseId = -1;
            if (responseCursor.moveToFirst()) {
                latestResponseId = responseCursor.getInt(0);
            }
            responseCursor.close();

            // Save farmer's reply into more_info table
            db.execSQL(
                    "INSERT INTO more_info (report_id, response_id, farmer_phone, farmer_message, date_submitted) " +
                            "VALUES (?, ?, ?, ?, datetime('now'))",
                    new Object[]{reportId, latestResponseId, farmerPhone, reply});

            // Clear input
            etFarmerReply.setText("");

            // Refresh thread to show the new reply
            loadConversationThread();

            Toast.makeText(this,
                    "Reply sent. The vet has been notified.",
                    Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error sending reply.", Toast.LENGTH_SHORT).show();
        }
    }

    // ─────────────────────────────────────────────
    //  Status badge
    // ─────────────────────────────────────────────
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
        }
    }

    // ─────────────────────────────────────────────
    //  Cleanup
    // ─────────────────────────────────────────────
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (db != null && db.isOpen()) {
            db.close();
        }
    }
}