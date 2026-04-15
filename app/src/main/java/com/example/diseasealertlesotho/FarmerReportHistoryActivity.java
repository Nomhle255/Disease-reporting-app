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
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
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
    private String userPhone;

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

        SharedPreferences sp = getSharedPreferences("UserSession", MODE_PRIVATE);
        userPhone = sp.getString("phone", "");

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
        try {
            Cursor cursor = db.rawQuery("SELECT * FROM reports WHERE user_phone = ? ORDER BY id DESC", new String[]{userPhone});
            if (cursor.moveToFirst()) {
                do {
                    HistoryReport report = new HistoryReport();
                    report.reportId = "REP-" + cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    report.animalType = cursor.getString(cursor.getColumnIndexOrThrow("animal_type"));
                    report.district = "Maseru"; // Placeholder
                    report.symptoms = cursor.getString(cursor.getColumnIndexOrThrow("symptoms"));
                    report.date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                    report.status = cursor.getString(cursor.getColumnIndexOrThrow("status"));

                    if (report.status == null) report.status = "Pending";

                    if (report.status.equalsIgnoreCase("Investigating")) {
                        report.footerMessage = "Vet requested more info · Please check alerts";
                    } else if (report.status.equalsIgnoreCase("Scheduled")) {
                        report.footerMessage = "Vet responded · Farm visit scheduled";
                    } else if (report.status.equalsIgnoreCase("Resolved")) {
                        report.footerMessage = "Case closed · Treatment advised";
                    } else {
                        report.footerMessage = "Awaiting vet review";
                    }

                    reportList.add(report);
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
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
            currentFilter = "Scheduled";
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
            finish();
        });
        findViewById(R.id.layout_report_btn).setOnClickListener(v -> {
            Intent intent = new Intent(FarmerReportHistoryActivity.this, ReportDiseaseActivity.class);
            startActivity(intent);
        });
        findViewById(R.id.layout_alerts_tab).setOnClickListener(v -> {
            Intent intent = new Intent(FarmerReportHistoryActivity.this, FarmerNotificationsActivity.class);
            startActivity(intent);
            finish();
        });
        findViewById(R.id.layout_profile_tab).setOnClickListener(v -> {
            Intent intent = new Intent(FarmerReportHistoryActivity.this, FarmerDashboardActivity.class);
            intent.putExtra("OPEN_FRAGMENT", "PROFILE");
            startActivity(intent);
            finish();
        });
    }

    static class HistoryReport {
        String reportId, animalType, district, symptoms, date, status, footerMessage;
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
            ImageView ivIcon = convertView.findViewById(R.id.iv_animal_icon);

            tvTitle.setText(report.animalType + " — " + report.symptoms);
            tvMeta.setText(report.reportId + " · " + report.date + " · " + report.district);
            
            // Map status for display
            String displayStatus = report.status;
            if (report.status.equalsIgnoreCase("Investigating")) {
                displayStatus = "Info Requested";
            }
            tvStatus.setText(displayStatus);
            tvFooter.setText(report.footerMessage);

            if (report.status.equalsIgnoreCase("Pending")) {
                tvStatus.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.tag_pending_bg));
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.tag_pending_text));
            } else if (report.status.equalsIgnoreCase("Investigating") || report.status.equalsIgnoreCase("Scheduled")) {
                tvStatus.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.tag_investigating_bg));
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.tag_investigating_text));
            } else if (report.status.equalsIgnoreCase("Resolved")) {
                tvStatus.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.tag_resolved_bg));
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.tag_resolved_text));
            }

            ivIcon.setImageResource(android.R.drawable.ic_menu_gallery);

            return convertView;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReports();
    }
}