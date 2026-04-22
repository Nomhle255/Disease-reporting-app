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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class VetCasesFragment extends Fragment {

    private ListView listView;
    private CaseAdapter adapter;
    private List<CaseReport> caseList = new ArrayList<>();
    private List<CaseReport> filteredList = new ArrayList<>();
    private DatabaseHelper dbHelper;
    private String currentFilter = "All";
    private TextView tvStatPending, tvStatScheduled, tvStatResolved, tvEmptyState;
    private MaterialButton btnAll, btnPending, btnActive, btnResolved;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_vet_cases, container, false);

        dbHelper = new DatabaseHelper(requireContext());
        initViews(view);
        setupFilters();
        loadCases();

        return view;
    }

    private void initViews(View view) {
        listView = view.findViewById(R.id.list_cases);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);

        adapter = new CaseAdapter(requireContext(), filteredList);
        listView.setAdapter(adapter);

        tvStatPending = view.findViewById(R.id.tv_stat_pending);
        tvStatScheduled = view.findViewById(R.id.tv_stat_scheduled);
        tvStatResolved = view.findViewById(R.id.tv_stat_resolved);

        btnAll = view.findViewById(R.id.btn_filter_all);
        btnPending = view.findViewById(R.id.btn_filter_pending);
        btnActive = view.findViewById(R.id.btn_filter_active);
        btnResolved = view.findViewById(R.id.btn_filter_resolved);
    }

    private void loadCases() {
        caseList.clear();
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
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
        applyFilters();
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

    private void setupFilters() {
        btnAll.setOnClickListener(v -> updateFilter("All"));
        btnPending.setOnClickListener(v -> updateFilter("Pending"));
        btnActive.setOnClickListener(v -> updateFilter("Active")); 
        btnResolved.setOnClickListener(v -> updateFilter("Resolved"));
    }

    private void updateFilter(String filter) {
        currentFilter = filter;
        updateFilterUI();
        applyFilters();
    }

    private void updateFilterUI() {
        updateButtonStyle(btnAll, currentFilter.equals("All"));
        updateButtonStyle(btnPending, currentFilter.equals("Pending"));
        updateButtonStyle(btnActive, currentFilter.equals("Active"));
        updateButtonStyle(btnResolved, currentFilter.equals("Resolved"));
    }

    private void updateButtonStyle(MaterialButton button, boolean isActive) {
        if (isActive) {
            button.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.header_green)));
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            button.setStrokeWidth(0);
        } else {
            button.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), android.R.color.transparent)));
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
            button.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.border_color)));
            button.setStrokeWidth((int) (1 * getResources().getDisplayMetrics().density));
        }
    }

    private void applyFilters() {
        filteredList.clear();
        for (CaseReport report : caseList) {
            boolean matchesFilter;
            if (currentFilter.equals("Active")) {
                matchesFilter = report.status.equalsIgnoreCase("Scheduled");
            } else {
                matchesFilter = currentFilter.equals("All") || report.status.equalsIgnoreCase(currentFilter);
            }
            
            if (matchesFilter) {
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

    static class CaseReport {
        int id;
        String farmerName, animalType, symptoms, date, status;
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
            tvDetails.setText(report.animalCount + " animals · " + report.symptoms);
            
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

    @Override
    public void onResume() {
        super.onResume();
        loadCases();
    }
}
