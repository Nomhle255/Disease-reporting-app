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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class FarmerReportsFragment extends Fragment {

    private ListView listView;
    private ReportAdapter adapter;
    private List<Report> allReports = new ArrayList<>();
    private List<Report> filteredReports = new ArrayList<>();
    private DatabaseHelper dbHelper;
    private TextView tvEmptyState;
    private String currentFilter = "All";

    private MaterialButton btnAll, btnPending, btnActive, btnResolved;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_farmer_reports, container, false);

        dbHelper = new DatabaseHelper(requireContext());
        listView = view.findViewById(R.id.list_reports_history);
        tvEmptyState = view.findViewById(R.id.tv_empty_state_history);
        
        btnAll = view.findViewById(R.id.btn_filter_all);
        btnPending = view.findViewById(R.id.btn_filter_pending);
        btnActive = view.findViewById(R.id.btn_filter_active);
        btnResolved = view.findViewById(R.id.btn_filter_resolved);

        adapter = new ReportAdapter(requireContext(), filteredReports);
        listView.setAdapter(adapter);

        setupFilters();
        loadReports();

        listView.setOnItemClickListener((parent, v, position, id) -> {
            Report selectedReport = filteredReports.get(position);
            Intent intent = new Intent(getActivity(), FarmerReportDetailsActivity.class);
            intent.putExtra("REPORT_ID", selectedReport.id);
            startActivity(intent);
        });

        return view;
    }

    private void setupFilters() {
        btnAll.setOnClickListener(v -> updateFilter("All"));
        btnPending.setOnClickListener(v -> updateFilter("Pending"));
        btnActive.setOnClickListener(v -> updateFilter("Scheduled"));
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
        resetButtonStyle(btnActive);
        resetButtonStyle(btnResolved);

        MaterialButton selected = btnAll;
        if (currentFilter.equals("Pending")) selected = btnPending;
        else if (currentFilter.equals("Scheduled")) selected = btnActive;
        else if (currentFilter.equals("Resolved")) selected = btnResolved;

        selected.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.header_green));
        selected.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
    }

    private void resetButtonStyle(MaterialButton btn) {
        btn.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.transparent));
        btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
        btn.setStrokeColor(ContextCompat.getColorStateList(requireContext(), R.color.border_color));
    }

    private void applyFilters() {
        filteredReports.clear();
        for (Report report : allReports) {
            if (currentFilter.equals("All") || report.status.equalsIgnoreCase(currentFilter)) {
                filteredReports.add(report);
            }
        }

        if (filteredReports.isEmpty()) {
            listView.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            listView.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);
        }
        adapter.notifyDataSetChanged();
    }

    private void loadReports() {
        allReports.clear();
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String phone = prefs.getString("phone", "");

        if (phone.isEmpty()) return;

        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
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
                    allReports.add(report);
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        applyFilters();
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
        public ReportAdapter(Context context, List<Report> items) { this.context = context; this.items = items; }
        @Override public int getCount() { return items.size(); }
        @Override public Object getItem(int position) { return items.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_report_history, parent, false);
            }

            Report report = items.get(position);
            ((TextView)convertView.findViewById(R.id.tv_report_title)).setText(report.animalType + " (" + report.animalCount + " animals)");
            ((TextView)convertView.findViewById(R.id.tv_report_meta)).setText(report.date + " · " + (report.symptoms.length() > 40 ? report.symptoms.substring(0, 37) + "..." : report.symptoms));
            
            TextView tvStatus = convertView.findViewById(R.id.tv_status_badge);
            tvStatus.setText(report.status);

            ImageView ivPhoto = convertView.findViewById(R.id.iv_report_photo_history);
            if (report.photo != null && report.photo.length > 0) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(report.photo, 0, report.photo.length);
                ivPhoto.setImageBitmap(bitmap);
            } else {
                ivPhoto.setImageResource(android.R.drawable.ic_menu_gallery);
            }

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

    @Override
    public void onResume() {
        super.onResume();
        loadReports();
    }
}
