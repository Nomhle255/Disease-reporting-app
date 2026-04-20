package com.example.diseasealertlesotho;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
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

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class AllReportsActivity extends AppCompatActivity {

    private ListView listView;
    private ReportAdapter adapter;
    private List<Report> reportList = new ArrayList<>();
    private List<Report> filteredList = new ArrayList<>();
    private SQLiteDatabase db;
    private String currentFilter = "All";
    
    private MaterialButton btnAll, btnPending, btnInvestigating, btnResolved;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_all_reports);

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
        updateSummaryStats();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Report report = filteredList.get(position);
            Intent intent = new Intent(this, AdminReportDetailsActivity.class);
            intent.putExtra("REPORT_ID", report.id);
            startActivity(intent);
        });

        findViewById(R.id.tv_back_dashboard).setOnClickListener(v -> finish());
    }

    private void initViews() {
        listView = findViewById(R.id.list_reports);
        
        btnAll = findViewById(R.id.btn_filter_all);
        btnPending = findViewById(R.id.btn_filter_pending);
        btnInvestigating = findViewById(R.id.btn_filter_investigating);
        btnResolved = findViewById(R.id.btn_filter_resolved);
        
        adapter = new ReportAdapter(this, filteredList);
        listView.setAdapter(adapter);
    }

    private void setupDatabase() {
        db = openOrCreateDatabase("DiseaseAlertDB", Context.MODE_PRIVATE, null);
    }

    private void loadReports() {
        reportList.clear();
        try {
            String query = "SELECT r.*, u.firstname, u.lastname FROM reports r " +
                          "LEFT JOIN users u ON r.user_phone = u.phone " +
                          "ORDER BY r.id DESC";
            Cursor cursor = db.rawQuery(query, null);
            
            if (cursor.moveToFirst()) {
                do {
                    Report report = new Report();
                    report.id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    report.reportId = "RPT-" + String.format("%03d", report.id);
                    report.userPhone = cursor.getString(cursor.getColumnIndexOrThrow("user_phone"));
                    
                    String fName = cursor.getString(cursor.getColumnIndexOrThrow("firstname"));
                    String lName = cursor.getString(cursor.getColumnIndexOrThrow("lastname"));
                    report.farmerName = (fName != null) ? fName + " " + lName : "Unknown Farmer";
                    
                    report.animalType = cursor.getString(cursor.getColumnIndexOrThrow("animal_type"));
                    report.district = "Maseru"; 
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
        applyFilters();
    }

    private void updateSummaryStats() {
        int pending = 0, investigating = 0, resolved = 0;
        for (Report r : reportList) {
            if (r.status.equalsIgnoreCase("Pending")) pending++;
            else if (r.status.equalsIgnoreCase("Investigating")) investigating++;
            else if (r.status.equalsIgnoreCase("Resolved")) resolved++;
        }

        View layoutSummary = findViewById(R.id.layout_summary);
        if (layoutSummary instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) layoutSummary;
            if (group.getChildCount() >= 4) {
                // Set all summary numbers to use green color as requested
                setupSummaryCard(group.getChildAt(0), String.valueOf(reportList.size()), "Total", R.color.header_green);
                setupSummaryCard(group.getChildAt(1), String.valueOf(pending), "Pending", R.color.header_green);
                setupSummaryCard(group.getChildAt(2), String.valueOf(investigating), "Active", R.color.header_green);
                setupSummaryCard(group.getChildAt(3), String.valueOf(resolved), "Resolved", R.color.header_green);
            }
        }
    }

    private void setupSummaryCard(View card, String count, String label, int colorRes) {
        if (card == null) return;
        TextView tvCount = card.findViewById(R.id.tv_summary_count);
        TextView tvLabel = card.findViewById(R.id.tv_summary_label);
        tvCount.setText(count);
        tvCount.setTextColor(ContextCompat.getColor(this, colorRes));
        tvLabel.setText(label);
    }

    private void setupFilters() {
        btnAll.setOnClickListener(v -> updateFilter("All"));
        btnPending.setOnClickListener(v -> updateFilter("Pending"));
        btnInvestigating.setOnClickListener(v -> updateFilter("Investigating"));
        btnResolved.setOnClickListener(v -> updateFilter("Resolved"));
        updateFilterUI();
    }

    private void updateFilter(String filter) {
        currentFilter = filter;
        applyFilters();
        updateFilterUI();
    }

    private void updateFilterUI() {
        resetButtonStyle(btnAll);
        resetButtonStyle(btnPending);
        resetButtonStyle(btnInvestigating);
        resetButtonStyle(btnResolved);

        MaterialButton selected = btnAll;
        if (currentFilter.equalsIgnoreCase("Pending")) selected = btnPending;
        else if (currentFilter.equalsIgnoreCase("Investigating")) selected = btnInvestigating;
        else if (currentFilter.equalsIgnoreCase("Resolved")) selected = btnResolved;

        selected.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.header_green));
        selected.setTextColor(ContextCompat.getColor(this, R.color.white));
    }

    private void resetButtonStyle(MaterialButton btn) {
        btn.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.transparent));
        btn.setTextColor(ContextCompat.getColor(this, R.color.black));
        btn.setStrokeColor(ContextCompat.getColorStateList(this, R.color.border_color));
    }

    private void applyFilters() {
        filteredList.clear();
        for (Report report : reportList) {
            boolean matchesFilter = currentFilter.equals("All") || report.status.equalsIgnoreCase(currentFilter);
            if (matchesFilter) {
                filteredList.add(report);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void setupNavigation() {
        View bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.findViewById(R.id.layout_home_tab).setOnClickListener(v -> finish());
        bottomNav.findViewById(R.id.layout_users_tab).setOnClickListener(v -> {
            startActivity(new Intent(this, ManageUsersActivity.class));
            finish();
        });
        bottomNav.findViewById(R.id.layout_stats_tab).setOnClickListener(v -> {
            startActivity(new Intent(this, StatisticsActivity.class));
            finish();
        });
        bottomNav.findViewById(R.id.layout_profile_tab).setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminDashboardActivity.class);
            intent.putExtra("OPEN_FRAGMENT", "PROFILE");
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        ((ImageView)bottomNav.findViewById(R.id.iv_reports_icon)).setColorFilter(ContextCompat.getColor(this, R.color.header_green));
        ((TextView)bottomNav.findViewById(R.id.tv_reports_text)).setTextColor(ContextCompat.getColor(this, R.color.header_green));
    }

    static class Report {
        int id;
        String reportId, farmerName, animalType, district, symptoms, date, status, userPhone;
        int animalCount;
        byte[] photo;
    }

    private class ReportAdapter extends BaseAdapter {
        private Context context;
        private List<Report> items;
        public ReportAdapter(Context context, List<Report> items) { this.context = context; this.items = items; }
        @Override public int getCount() { return items.size(); }
        @Override public Object getItem(int position) { return items.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.report_item, parent, false);
            }
            Report report = items.get(position);
            TextView tvId = convertView.findViewById(R.id.tv_report_id);
            TextView tvDate = convertView.findViewById(R.id.tv_report_date);
            TextView tvFarmerAnimal = convertView.findViewById(R.id.tv_farmer_animal);
            TextView tvDetails = convertView.findViewById(R.id.tv_location_details);
            TextView tvStatus = convertView.findViewById(R.id.tv_status_tag);
            ImageView ivPhoto = convertView.findViewById(R.id.iv_report_photo);

            tvId.setText(report.reportId);
            tvDate.setText(report.date);
            tvFarmerAnimal.setText(report.farmerName + " — " + report.animalType);
            tvDetails.setText(report.district + " · " + report.animalCount + " animals · " + report.symptoms);
            
            if (report.photo != null && report.photo.length > 0) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(report.photo, 0, report.photo.length);
                ivPhoto.setImageBitmap(bitmap);
            } else {
                ivPhoto.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            tvStatus.setText(report.status);
            if (report.status.equalsIgnoreCase("Pending")) {
                tvStatus.setBackgroundResource(R.drawable.tag_bg_pending);
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.tag_pending_text));
            } else if (report.status.equalsIgnoreCase("Investigating")) {
                tvStatus.setBackgroundResource(R.drawable.tag_bg_investigating);
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.tag_investigating_text));
            } else if (report.status.equalsIgnoreCase("Resolved")) {
                tvStatus.setBackgroundResource(R.drawable.tag_bg_resolved);
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.tag_resolved_text));
            }
            return convertView;
        }
    }
}
