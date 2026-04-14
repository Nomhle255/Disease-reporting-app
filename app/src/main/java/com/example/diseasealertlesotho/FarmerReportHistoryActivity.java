package com.example.diseasealertlesotho;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

public class FarmerReportHistoryActivity extends AppCompatActivity {

    private ListView listView;
    private View emptyState;
    private ReportHistoryAdapter adapter;
    private List<HistoryReport> reportList = new ArrayList<>();
    private List<HistoryReport> filteredList = new ArrayList<>();
    private SQLiteDatabase db;
    private String currentFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_farmer_report_history);

        View mainView = findViewById(android.R.id.content);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        initViews();
        setupDatabase();
        loadReports();
        setupFilters();
        setupNavigation();

        findViewById(R.id.tv_back_dashboard).setOnClickListener(v -> finish());
    }

    private void initViews() {
        listView = findViewById(R.id.list_report_history);
        emptyState = findViewById(R.id.layout_empty_state);
        adapter = new ReportHistoryAdapter(this, filteredList);
        listView.setAdapter(adapter);
    }

    private void setupDatabase() {
        db = openOrCreateDatabase("DiseaseAlertDB", Context.MODE_PRIVATE, null);
    }

    private void loadReports() {
        reportList.clear();
        // In a real app, we would filter by the current user's ID
        try {
            Cursor cursor = db.rawQuery("SELECT * FROM reports ORDER BY id DESC", null);
            if (cursor.moveToFirst()) {
                do {
                    HistoryReport report = new HistoryReport();
                    report.reportId = cursor.getString(cursor.getColumnIndexOrThrow("report_id"));
                    report.animalType = cursor.getString(cursor.getColumnIndexOrThrow("animal_type"));
                    report.district = cursor.getString(cursor.getColumnIndexOrThrow("district"));
                    report.symptoms = cursor.getString(cursor.getColumnIndexOrThrow("symptoms"));
                    report.date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                    report.status = cursor.getString(cursor.getColumnIndexOrThrow("status"));

                    // Mocking some footer messages based on status
                    if (report.status.equalsIgnoreCase("Investigating")) {
                        report.footerMessage = "Vet responded · Farm visit scheduled 12 Apr";
                        report.progress = 60;
                    } else if (report.status.equalsIgnoreCase("Resolved")) {
                        report.footerMessage = "Case closed · Treatment advised · No visit needed";
                        report.progress = 100;
                    } else {
                        report.footerMessage = "Awaiting vet review";
                        report.progress = 20;
                    }

                    reportList.add(report);
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (Exception e) {
            // Table might not exist yet or no records found
        }
        applyFilter();
    }

    private void setupFilters() {
        findViewById(R.id.btn_filter_all).setOnClickListener(v -> {
            currentFilter = "All";
            applyFilter();
        });
        findViewById(R.id.btn_filter_pending).setOnClickListener(v -> {
            currentFilter = "Pending";
            applyFilter();
        });
        findViewById(R.id.btn_filter_active).setOnClickListener(v -> {
            currentFilter = "Investigating";
            applyFilter();
        });
        findViewById(R.id.btn_filter_resolved).setOnClickListener(v -> {
            currentFilter = "Resolved";
            applyFilter();
        });
    }

    private void applyFilter() {
        filteredList.clear();
        for (HistoryReport report : reportList) {
            if (currentFilter.equals("All") || report.status.equalsIgnoreCase(currentFilter)) {
                filteredList.add(report);
            }
        }
        
        // Show/Hide empty state
        if (filteredList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
        }

        adapter.notifyDataSetChanged();
    }

    private void setupNavigation() {
        findViewById(R.id.layout_home_tab).setOnClickListener(v -> {
            startActivity(new Intent(this, FarmerDashboardActivity.class));
            finish();
        });
        findViewById(R.id.layout_report_btn).setOnClickListener(v -> {
            startActivity(new Intent(this, ReportDiseaseActivity.class));
        });
        findViewById(R.id.layout_alerts_tab).setOnClickListener(v -> {
            Toast.makeText(this, "Alerts clicked", Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.layout_profile_tab).setOnClickListener(v -> {
            Toast.makeText(this, "Profile clicked", Toast.LENGTH_SHORT).show();
        });
    }

    static class HistoryReport {
        String reportId, animalType, district, symptoms, date, status, footerMessage;
        int progress;
    }

    private class ReportHistoryAdapter extends BaseAdapter {
        private Context context;
        private List<HistoryReport> items;

        public ReportHistoryAdapter(Context context, List<HistoryReport> items) {
            this.context = context;
            this.items = items;
        }

        @Override
        public int getCount() { return items.size(); }
        @Override
        public Object getItem(int position) { return items.get(position); }
        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_report_history, parent, false);
            }

            HistoryReport report = items.get(position);
            TextView tvTitle = convertView.findViewById(R.id.tv_report_title);
            TextView tvMeta = convertView.findViewById(R.id.tv_report_meta);
            TextView tvStatus = convertView.findViewById(R.id.tv_status_badge);
            TextView tvFooter = convertView.findViewById(R.id.tv_footer_message);
            ProgressBar pb = convertView.findViewById(R.id.pb_status);
            ImageView ivIcon = convertView.findViewById(R.id.iv_animal_icon);

            tvTitle.setText(report.animalType + " — " + report.symptoms);
            tvMeta.setText(report.reportId + " · " + report.date + " · " + report.district);
            tvStatus.setText(report.status);
            tvFooter.setText(report.footerMessage);
            pb.setProgress(report.progress);

            // Styling status badge
            if (report.status.equalsIgnoreCase("Pending")) {
                tvStatus.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.tag_pending_bg));
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.tag_pending_text));
            } else if (report.status.equalsIgnoreCase("Investigating")) {
                tvStatus.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.tag_investigating_bg));
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.tag_investigating_text));
            } else if (report.status.equalsIgnoreCase("Resolved")) {
                tvStatus.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.tag_resolved_bg));
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.tag_resolved_text));
            }

            // Animal Icons
            if (report.animalType.contains("Cattle")) ivIcon.setImageResource(android.R.drawable.ic_menu_gallery); // Placeholder
            else if (report.animalType.contains("Goat")) ivIcon.setImageResource(android.R.drawable.ic_menu_gallery);
            else if (report.animalType.contains("Poultry")) ivIcon.setImageResource(android.R.drawable.ic_menu_gallery);

            return convertView;
        }
    }
}
