package com.example.diseasealertlesotho;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

public class AllReportsActivity extends AppCompatActivity {

    private ListView listView;
    private EditText etSearch;
    private ReportAdapter adapter;
    private List<Report> reportList = new ArrayList<>();
    private List<Report> filteredList = new ArrayList<>();
    private SQLiteDatabase db;
    private String currentFilter = "All";

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
        setupSearch();
        setupFilters();
        setupNavigation();
        updateSummaryCards();

        findViewById(R.id.tv_back_dashboard).setOnClickListener(v -> finish());
    }

    private void initViews() {
        listView = findViewById(R.id.list_reports);
        etSearch = findViewById(R.id.et_search_reports);
        adapter = new ReportAdapter(this, filteredList);
        listView.setAdapter(adapter);
    }

    private void setupDatabase() {
        db = openOrCreateDatabase("DiseaseAlertDB", Context.MODE_PRIVATE, null);
        // Ensure reports table exists for demo purposes if not already created elsewhere
        db.execSQL("CREATE TABLE IF NOT EXISTS reports (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "report_id TEXT, " +
                "farmer_name TEXT, " +
                "animal_type TEXT, " +
                "district TEXT, " +
                "animal_count INTEGER, " +
                "symptoms TEXT, " +
                "date TEXT, " +
                "assigned_to TEXT, " +
                "status TEXT)");
        
        // Add some sample data if table is empty
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM reports", null);
        cursor.moveToFirst();
        if (cursor.getInt(0) == 0) {
            db.execSQL("INSERT INTO reports (report_id, farmer_name, animal_type, district, animal_count, symptoms, date, assigned_to, status) VALUES " +
                    "('RPT-045', 'Thabo Mokete', 'Cattle', 'Maseru', 5, 'FMD symptoms', '10 Apr 2026', 'Dr. Nthabiseng', 'Investigating')," +
                    "('RPT-044', 'Lineo Tšepe', 'Sheep', 'Leribe', 12, 'Skin lesions', '9 Apr 2026', 'Unassigned', 'Pending')," +
                    "('RPT-043', 'Rethabile Sello', 'Poultry', 'Berea', 30, 'Sudden deaths', '9 Apr 2026', 'Visit scheduled 12 Apr', 'Visit Scheduled')," +
                    "('RPT-042', 'Mpho Ramoeli', 'Goats', 'Mafeteng', 8, 'Respiratory', '5 Apr 2026', 'Case closed', 'Resolved')");
        }
        cursor.close();
    }

    private void loadReports() {
        reportList.clear();
        Cursor cursor = db.rawQuery("SELECT * FROM reports ORDER BY id DESC", null);
        if (cursor.moveToFirst()) {
            do {
                Report report = new Report();
                report.reportId = cursor.getString(cursor.getColumnIndexOrThrow("report_id"));
                report.farmerName = cursor.getString(cursor.getColumnIndexOrThrow("farmer_name"));
                report.animalType = cursor.getString(cursor.getColumnIndexOrThrow("animal_type"));
                report.district = cursor.getString(cursor.getColumnIndexOrThrow("district"));
                report.animalCount = cursor.getInt(cursor.getColumnIndexOrThrow("animal_count"));
                report.symptoms = cursor.getString(cursor.getColumnIndexOrThrow("symptoms"));
                report.date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                report.assignedTo = cursor.getString(cursor.getColumnIndexOrThrow("assigned_to"));
                report.status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
                reportList.add(report);
            } while (cursor.moveToNext());
        }
        cursor.close();
        applyFilters("");
    }

    private void updateSummaryCards() {
        // Find the summary layout and its children
        View layoutSummary = findViewById(R.id.layout_summary);
        if (layoutSummary instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) layoutSummary;
            
            setupSummaryCard(group.getChildAt(0), "8", "New", R.color.status_new);
            setupSummaryCard(group.getChildAt(1), "15", "Active", R.color.status_active);
            setupSummaryCard(group.getChildAt(2), "5", "Visit", R.color.status_visit);
            setupSummaryCard(group.getChildAt(3), "35", "Resolved", R.color.status_resolved);
        }
    }

    private void setupSummaryCard(View card, String count, String label, int colorRes) {
        TextView tvCount = card.findViewById(R.id.tv_summary_count);
        TextView tvLabel = card.findViewById(R.id.tv_summary_label);
        tvCount.setText(count);
        tvCount.setTextColor(ContextCompat.getColor(this, colorRes));
        tvLabel.setText(label);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFilters() {
        findViewById(R.id.btn_filter_all).setOnClickListener(v -> updateFilter("All"));
        findViewById(R.id.btn_filter_pending).setOnClickListener(v -> updateFilter("Pending"));
        findViewById(R.id.btn_filter_investigating).setOnClickListener(v -> updateFilter("Investigating"));
        findViewById(R.id.btn_filter_resolved).setOnClickListener(v -> updateFilter("Resolved"));
    }

    private void updateFilter(String filter) {
        currentFilter = filter;
        applyFilters(etSearch.getText().toString());
    }

    private void applyFilters(String query) {
        filteredList.clear();
        for (Report report : reportList) {
            boolean matchesFilter = currentFilter.equals("All") || report.status.equalsIgnoreCase(currentFilter);
            boolean matchesQuery = report.farmerName.toLowerCase().contains(query.toLowerCase()) || 
                                 report.animalType.toLowerCase().contains(query.toLowerCase()) ||
                                 report.district.toLowerCase().contains(query.toLowerCase());
            
            if (matchesFilter && matchesQuery) {
                filteredList.add(report);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void setupNavigation() {
        View bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.findViewById(R.id.layout_home_tab).setOnClickListener(v -> {
            startActivity(new Intent(this, AdminDashboardActivity.class));
            finish();
        });
        bottomNav.findViewById(R.id.layout_users_tab).setOnClickListener(v -> {
            startActivity(new Intent(this, ManageUsersActivity.class));
            finish();
        });
        bottomNav.findViewById(R.id.layout_stats_tab).setOnClickListener(v -> {
            startActivity(new Intent(this, StatisticsActivity.class));
            finish();
        });

        // Set Reports as active
        ((ImageView)bottomNav.findViewById(R.id.iv_reports_icon)).setColorFilter(ContextCompat.getColor(this, R.color.header_green));
        ((TextView)bottomNav.findViewById(R.id.tv_reports_text)).setTextColor(ContextCompat.getColor(this, R.color.header_green));
    }

    static class Report {
        String reportId, farmerName, animalType, district, symptoms, date, assignedTo, status;
        int animalCount;
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
                convertView = LayoutInflater.from(context).inflate(R.layout.report_item, parent, false);
            }

            Report report = items.get(position);
            TextView tvId = convertView.findViewById(R.id.tv_report_id);
            TextView tvDate = convertView.findViewById(R.id.tv_report_date);
            TextView tvFarmerAnimal = convertView.findViewById(R.id.tv_farmer_animal);
            TextView tvDetails = convertView.findViewById(R.id.tv_location_details);
            TextView tvAssigned = convertView.findViewById(R.id.tv_assigned_info);
            TextView tvStatus = convertView.findViewById(R.id.tv_status_tag);
            ImageView ivAnimal = convertView.findViewById(R.id.iv_animal_icon);

            tvId.setText(report.reportId);
            tvDate.setText(report.date);
            tvFarmerAnimal.setText(report.farmerName + " — " + report.animalType);
            tvDetails.setText(report.district + " · " + report.animalCount + " animals · " + report.symptoms);
            tvAssigned.setText(report.assignedTo + (report.status.equals("Investigating") ? " assigned" : ""));
            tvStatus.setText(report.status);

            // Dynamic status styling
            if (report.status.equalsIgnoreCase("Pending")) {
                tvStatus.setBackgroundResource(R.drawable.tag_bg_pending);
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.tag_pending_text));
            } else if (report.status.equalsIgnoreCase("Investigating")) {
                tvStatus.setBackgroundResource(R.drawable.tag_bg_investigating);
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.tag_investigating_text));
            } else if (report.status.equalsIgnoreCase("Resolved")) {
                tvStatus.setBackgroundResource(R.drawable.tag_bg_resolved);
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.tag_resolved_text));
            } else {
                tvStatus.setBackgroundResource(R.drawable.tag_bg_pending); // Default
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.tag_pending_text));
            }

            return convertView;
        }
    }
}
