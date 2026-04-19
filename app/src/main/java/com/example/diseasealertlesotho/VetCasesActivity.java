package com.example.diseasealertlesotho;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class VetCasesActivity extends AppCompatActivity {

    private ListView listView;
    private EditText etSearch;
    private CaseAdapter adapter;
    private List<CaseReport> caseList = new ArrayList<>();
    private List<CaseReport> filteredList = new ArrayList<>();
    private SQLiteDatabase db;
    private String currentFilter = "All";
    private TextView tvStatPending, tvStatScheduled, tvStatResolved, tvEmptyState;
    private MaterialButton btnAll, btnPending, btnActive, btnResolved;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vet_cases);

        initViews();
        setupDatabase();
        setupSearch();
        setupFilters();
        setupNavigation();

        findViewById(R.id.tv_back_dashboard).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCases();
    }

    private void initViews() {
        listView = findViewById(R.id.list_cases);
        etSearch = new EditText(this); // Temporary until layout has one
        tvEmptyState = findViewById(R.id.tv_empty_state);

        adapter = new CaseAdapter(this, filteredList);
        listView.setAdapter(adapter);

        tvStatPending = findViewById(R.id.tv_stat_pending);
        tvStatScheduled = findViewById(R.id.tv_stat_scheduled);
        tvStatResolved = findViewById(R.id.tv_stat_resolved);

        btnAll = findViewById(R.id.btn_filter_all);
        btnPending = findViewById(R.id.btn_filter_pending);
        btnActive = findViewById(R.id.btn_filter_active);
        btnResolved = findViewById(R.id.btn_filter_resolved);
    }

    private void setupDatabase() {
        db = openOrCreateDatabase("DiseaseAlertDB", Context.MODE_PRIVATE, null);
    }

    private void loadCases() {
        caseList.clear();
        try {
            String query = "SELECT r.*, u.firstname, u.lastname FROM reports r " +
                          "LEFT JOIN users u ON r.user_phone = u.phone " +
                          "ORDER BY r.id DESC";
            Cursor cursor = db.rawQuery(query, null);
            
            if (cursor.moveToFirst()) {
                do {
                    CaseReport report = new CaseReport();
                    report.id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    
                    String fName = cursor.getString(cursor.getColumnIndexOrThrow("firstname"));
                    String lName = cursor.getString(cursor.getColumnIndexOrThrow("lastname"));
                    report.farmerName = (fName != null) ? fName + " " + lName : "Unknown Farmer";
                    
                    report.animalType = cursor.getString(cursor.getColumnIndexOrThrow("animal_type"));
                    report.animalCount = cursor.getInt(cursor.getColumnIndexOrThrow("count"));
                    report.symptoms = cursor.getString(cursor.getColumnIndexOrThrow("symptoms"));
                    report.date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                    report.status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
                    report.photo = cursor.getBlob(cursor.getColumnIndexOrThrow("photo"));
                    
                    if (report.status == null) report.status = "Pending";
                    
                    caseList.add(report);
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        applyFilters("");
        updateSummaryStats();
        updateFilterUI();
    }

    private void updateSummaryStats() {
        int pending = 0, scheduled = 0, resolved = 0;
        for (CaseReport r : caseList) {
            if (r.status.equalsIgnoreCase("Pending")) pending++;
            else if (r.status.equalsIgnoreCase("Scheduled")) scheduled++;
            else if (r.status.equalsIgnoreCase("Resolved")) resolved++;
        }

        if (tvStatPending != null) tvStatPending.setText(String.valueOf(pending));
        if (tvStatScheduled != null) tvStatScheduled.setText(String.valueOf(scheduled));
        if (tvStatResolved != null) tvStatResolved.setText(String.valueOf(resolved));
    }

    private void setupSearch() {
        // No et_search in current activity_vet_cases.xml
    }

    private void setupFilters() {
        btnAll.setOnClickListener(v -> updateFilter("All"));
        btnPending.setOnClickListener(v -> updateFilter("Pending"));
        btnActive.setOnClickListener(v -> updateFilter("Active")); // Internally mapped to "Scheduled"
        btnResolved.setOnClickListener(v -> updateFilter("Resolved"));
    }

    private void updateFilter(String filter) {
        currentFilter = filter;
        updateFilterUI();
        String query = (etSearch != null && etSearch.getText() != null) ? etSearch.getText().toString() : "";
        applyFilters(query);
    }

    private void updateFilterUI() {
        updateButtonStyle(btnAll, currentFilter.equals("All"));
        updateButtonStyle(btnPending, currentFilter.equals("Pending"));
        updateButtonStyle(btnActive, currentFilter.equals("Active"));
        updateButtonStyle(btnResolved, currentFilter.equals("Resolved"));
    }

    private void updateButtonStyle(MaterialButton button, boolean isActive) {
        if (isActive) {
            button.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.header_green)));
            button.setTextColor(ContextCompat.getColor(this, R.color.white));
            button.setStrokeWidth(0);
        } else {
            button.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.transparent)));
            button.setTextColor(ContextCompat.getColor(this, R.color.black));
            button.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.border_color)));
            button.setStrokeWidth((int) (1 * getResources().getDisplayMetrics().density));
        }
    }

    private void applyFilters(String query) {
        filteredList.clear();
        for (CaseReport report : caseList) {
            boolean matchesFilter;
            if (currentFilter.equals("Active")) {
                // Fixed: Only matches "Scheduled" now
                matchesFilter = report.status.equalsIgnoreCase("Scheduled");
            } else {
                matchesFilter = currentFilter.equals("All") || report.status.equalsIgnoreCase(currentFilter);
            }
            
            boolean matchesQuery = query.isEmpty() || 
                                 report.farmerName.toLowerCase().contains(query.toLowerCase()) || 
                                 report.animalType.toLowerCase().contains(query.toLowerCase()) ||
                                 report.symptoms.toLowerCase().contains(query.toLowerCase());
            
            if (matchesFilter && matchesQuery) {
                filteredList.add(report);
            }
        }
        
        if (filteredList.isEmpty()) {
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
            startActivity(new Intent(this, VetDashboardActivity.class));
            finish();
        });
        
        findViewById(R.id.layout_alerts_tab).setOnClickListener(v -> {
             startActivity(new Intent(this, VetAlertsActivity.class));
             finish();
        });

        findViewById(R.id.layout_profile_tab).setOnClickListener(v -> {
             Intent intent = new Intent(this, VetDashboardActivity.class);
             intent.putExtra("OPEN_FRAGMENT", "PROFILE");
             startActivity(intent);
             finish();
        });
    }

    static class CaseReport {
        int id;
        String farmerName, animalType, district, symptoms, date, status;
        int animalCount;
        byte[] photo;
    }

    private class CaseAdapter extends BaseAdapter {
        private Context context;
        private List<CaseReport> items;

        public CaseAdapter(Context context, List<CaseReport> items) {
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

            CaseReport report = items.get(position);
            TextView tvId = convertView.findViewById(R.id.tv_report_id);
            TextView tvDate = convertView.findViewById(R.id.tv_report_date);
            TextView tvFarmerAnimal = convertView.findViewById(R.id.tv_farmer_animal);
            TextView tvDetails = convertView.findViewById(R.id.tv_location_details);
            TextView tvStatus = convertView.findViewById(R.id.tv_status_tag);
            ImageView ivPhoto = convertView.findViewById(R.id.iv_report_photo);

            tvId.setText("RPT-" + String.format("%03d", report.id));
            tvDate.setText(report.date);
            tvFarmerAnimal.setText(report.farmerName + " — " + report.animalType);
            tvDetails.setText((report.district != null ? report.district : "") + " · " + report.animalCount + " animals · " + report.symptoms);
            
            if (report.photo != null && report.photo.length > 0) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(report.photo, 0, report.photo.length);
                ivPhoto.setImageBitmap(bitmap);
            } else {
                ivPhoto.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            String displayStatus = report.status;
            if (report.status.equalsIgnoreCase("Investigating")) {
                displayStatus = "Info Requested";
            }
            tvStatus.setText(displayStatus);

            if (report.status.equalsIgnoreCase("Pending")) {
                tvStatus.setBackgroundResource(R.drawable.tag_bg_pending);
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.tag_pending_text));
            } else if (report.status.equalsIgnoreCase("Investigating") || report.status.equalsIgnoreCase("Scheduled")) {
                tvStatus.setBackgroundResource(R.drawable.tag_bg_investigating);
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.tag_investigating_text));
            } else if (report.status.equalsIgnoreCase("Resolved")) {
                tvStatus.setBackgroundResource(R.drawable.tag_bg_resolved);
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.tag_resolved_text));
            }

            convertView.setOnClickListener(v -> {
                Intent intent = new Intent(context, VetCaseDetailsActivity.class);
                intent.putExtra("CASE_ID", "RPT-" + String.format("%03d", report.id));
                context.startActivity(intent);
            });

            return convertView;
        }
    }
}