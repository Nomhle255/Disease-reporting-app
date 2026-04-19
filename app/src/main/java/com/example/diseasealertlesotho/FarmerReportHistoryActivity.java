package com.example.diseasealertlesotho;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class FarmerReportHistoryActivity extends AppCompatActivity {

    private ListView listView;
    private ReportAdapter adapter;
    private List<Report> reportList = new ArrayList<>();
    private SQLiteDatabase db;
    private TextView tvEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farmer_report_history_list);

        initViews();
        setupDatabase();
        loadReports();
        setupNavigation();

        findViewById(R.id.tv_back).setOnClickListener(v -> finish());

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Report selectedReport = reportList.get(position);
            Intent intent = new Intent(FarmerReportHistoryActivity.this, FarmerReportDetailsActivity.class);
            intent.putExtra("REPORT_ID", selectedReport.id);
            startActivity(intent);
        });
    }

    private void initViews() {
        listView = findViewById(R.id.list_reports_history);
        tvEmptyState = findViewById(R.id.tv_empty_state_history);
        
        adapter = new ReportAdapter(this, reportList);
        listView.setAdapter(adapter);
    }

    private void setupDatabase() {
        db = openOrCreateDatabase("DiseaseAlertDB", Context.MODE_PRIVATE, null);
    }

    private void loadReports() {
        reportList.clear();
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String phone = prefs.getString("phone", "");

        if (phone.isEmpty()) {
            Toast.makeText(this, "User session expired", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Cursor cursor = db.rawQuery("SELECT * FROM reports WHERE user_phone = ? ORDER BY id DESC", new String[]{phone});
            
            if (cursor.moveToFirst()) {
                do {
                    Report report = new Report();
                    report.id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    report.animalType = cursor.getString(cursor.getColumnIndexOrThrow("animal_type"));
                    report.animalCount = cursor.getInt(cursor.getColumnIndexOrThrow("count"));
                    report.symptoms = cursor.getString(cursor.getColumnIndexOrThrow("symptoms"));
                    report.date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                    report.status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
                    report.photo = cursor.getBlob(cursor.getColumnIndexOrThrow("photo"));
                    
                    if (report.status == null) report.status = "Pending";
                    
                    reportList.add(report);
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (reportList.isEmpty()) {
            listView.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            listView.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);
        }
        adapter.notifyDataSetChanged();
    }

    private void setupNavigation() {
        findViewById(R.id.layout_home_tab).setOnClickListener(v -> {
            Intent intent = new Intent(this, FarmerDashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.layout_reports_tab).setOnClickListener(v -> {
            // Already here, maybe refresh?
            loadReports();
        });

        findViewById(R.id.btn_report_nav).setOnClickListener(v -> {
            Intent intent = new Intent(this, ReportDiseaseActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.layout_alerts_tab).setOnClickListener(v -> {
            startActivity(new Intent(this, FarmerNotificationsActivity.class));
            finish();
        });

        findViewById(R.id.layout_profile_tab).setOnClickListener(v -> {
            Intent intent = new Intent(this, FarmerDashboardActivity.class);
            intent.putExtra("OPEN_FRAGMENT", "PROFILE");
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }

    static class Report {
        int id;
        String animalType, symptoms, date, status;
        int animalCount;
        byte[] photo;
    }

    private class ReportAdapter extends BaseAdapter {
        private Context context;
        private List<Report> items;

        public ReportAdapter(Context context, List<Report> items) {
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

            Report report = items.get(position);
            TextView tvTitle = convertView.findViewById(R.id.tv_report_title);
            TextView tvMeta = convertView.findViewById(R.id.tv_report_meta);
            TextView tvStatus = convertView.findViewById(R.id.tv_status_badge);
            ImageView ivPhoto = convertView.findViewById(R.id.iv_report_photo_history);

            tvTitle.setText(report.animalType + " (" + report.animalCount + " animals)");
            tvMeta.setText(report.date + " · " + (report.symptoms.length() > 40 ? report.symptoms.substring(0, 37) + "..." : report.symptoms));
            tvStatus.setText(report.status);

            if (report.photo != null && report.photo.length > 0) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(report.photo, 0, report.photo.length);
                ivPhoto.setImageBitmap(bitmap);
            } else {
                ivPhoto.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            // Styling
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

            return convertView;
        }
    }
}