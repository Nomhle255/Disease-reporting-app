package com.example.diseasealertlesotho;

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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class ManageReportsFragment extends Fragment {

    private ListView listView;
    private TextView tvEmptyView;
    private ReportAdapter adapter;
    private List<Report> reportList = new ArrayList<>();
    private List<Report> filteredList = new ArrayList<>();
    private DatabaseHelper dbHelper;
    private String currentFilter = "All";
    
    private MaterialButton btnAll, btnPending, btnInvestigating, btnResolved;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_reports, container, false);

        dbHelper = new DatabaseHelper(requireContext());
        initViews(view);
        loadReports();
        setupFilters();
        updateSummaryStats(view);

        listView.setOnItemClickListener((parent, v, position, id) -> {
            Report report = filteredList.get(position);
            Intent intent = new Intent(getActivity(), AdminReportDetailsActivity.class);
            intent.putExtra("REPORT_ID", report.id);
            startActivity(intent);
        });

        return view;
    }

    private void initViews(View view) {
        listView = view.findViewById(R.id.list_reports);
        tvEmptyView = view.findViewById(R.id.tv_empty_view_reports);
        
        btnAll = view.findViewById(R.id.btn_filter_all);
        btnPending = view.findViewById(R.id.btn_filter_pending);
        btnInvestigating = view.findViewById(R.id.btn_filter_investigating);
        btnResolved = view.findViewById(R.id.btn_filter_resolved);
        
        adapter = new ReportAdapter(requireContext(), filteredList);
        listView.setAdapter(adapter);
    }

    private void loadReports() {
        reportList.clear();
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String query = "SELECT r.*, u.firstname, u.lastname FROM reports r " +
                          "LEFT JOIN users u ON r.user_phone = u.phone " +
                          "ORDER BY r.id DESC";
            Cursor cursor = db.rawQuery(query, null);
            
            if (cursor.moveToFirst()) {
                do {
                    Report report = new Report();
                    report.id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    report.reportId = "RPT-" + String.format("%03d", report.id);
                    
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

    private void updateSummaryStats(View view) {
        int pending = 0, investigating = 0, resolved = 0;
        for (Report r : reportList) {
            if (r.status.equalsIgnoreCase("Pending")) pending++;
            else if (r.status.equalsIgnoreCase("Investigating")) investigating++;
            else if (r.status.equalsIgnoreCase("Resolved")) resolved++;
        }

        View layoutSummary = view.findViewById(R.id.layout_summary);
        if (layoutSummary instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) layoutSummary;
            if (group.getChildCount() >= 4) {
                setupSummaryCard(group.getChildAt(0), String.valueOf(reportList.size()), "Total");
                setupSummaryCard(group.getChildAt(1), String.valueOf(pending), "Pending");
                setupSummaryCard(group.getChildAt(2), String.valueOf(investigating), "Active");
                setupSummaryCard(group.getChildAt(3), String.valueOf(resolved), "Resolved");
            }
        }
    }

    private void setupSummaryCard(View card, String count, String label) {
        if (card == null) return;
        TextView tvCount = card.findViewById(R.id.tv_summary_count);
        TextView tvLabel = card.findViewById(R.id.tv_summary_label);
        tvCount.setText(count);
        tvCount.setTextColor(ContextCompat.getColor(requireContext(), R.color.header_green));
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

        selected.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.header_green));
        selected.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
    }

    private void resetButtonStyle(MaterialButton btn) {
        btn.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.transparent));
        btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
        btn.setStrokeColor(ContextCompat.getColorStateList(requireContext(), R.color.border_color));
    }

    private void applyFilters() {
        filteredList.clear();
        for (Report report : reportList) {
            if (currentFilter.equals("All") || report.status.equalsIgnoreCase(currentFilter)) {
                filteredList.add(report);
            }
        }

        if (filteredList.isEmpty()) {
            tvEmptyView.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
        } else {
            tvEmptyView.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
        }

        adapter.notifyDataSetChanged();
    }

    static class Report {
        int id;
        String reportId, farmerName, animalType, district, symptoms, date, status;
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
            ((TextView)convertView.findViewById(R.id.tv_report_id)).setText(report.reportId);
            ((TextView)convertView.findViewById(R.id.tv_report_date)).setText(report.date);
            ((TextView)convertView.findViewById(R.id.tv_farmer_animal)).setText(report.farmerName + " — " + report.animalType);
            ((TextView)convertView.findViewById(R.id.tv_location_details)).setText(report.district + " · " + report.animalCount + " animals · " + report.symptoms);
            
            ImageView ivPhoto = convertView.findViewById(R.id.iv_report_photo);
            if (report.photo != null && report.photo.length > 0) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(report.photo, 0, report.photo.length);
                ivPhoto.setImageBitmap(bitmap);
            } else {
                ivPhoto.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            TextView tvStatus = convertView.findViewById(R.id.tv_status_tag);
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

    @Override
    public void onResume() {
        super.onResume();
        loadReports();
    }
}
