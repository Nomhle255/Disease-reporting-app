package com.example.diseasealertlesotho;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {

    private TextView tvUserName, tvTotalReports, tvPendingReports, tvResolvedReports, tvNoReports;
    private LinearLayout layoutRecentReports;
    private SQLiteDatabase db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        tvUserName = view.findViewById(R.id.tv_user_name);
        tvTotalReports = view.findViewById(R.id.tv_total_reports);
        tvPendingReports = view.findViewById(R.id.tv_pending_reports);
        tvResolvedReports = view.findViewById(R.id.tv_resolved_reports);
        tvNoReports = view.findViewById(R.id.tv_no_reports);
        layoutRecentReports = view.findViewById(R.id.layout_recent_reports);

        loadUserData();

        return view;
    }

    private void loadUserData() {
        if (!isAdded()) return;

        SharedPreferences prefs = getActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String userName = prefs.getString("name", "Farmer");
        String phone = prefs.getString("phone", "");

        tvUserName.setText(userName);

        if (!phone.isEmpty()) {
            db = getActivity().openOrCreateDatabase("DiseaseAlertDB", Context.MODE_PRIVATE, null);
            
            // Total Reports
            Cursor cTotal = db.rawQuery("SELECT COUNT(*) FROM reports WHERE user_phone = ?", new String[]{phone});
            if (cTotal.moveToFirst()) tvTotalReports.setText(String.valueOf(cTotal.getInt(0)));
            cTotal.close();

            // Pending Reports
            Cursor cPending = db.rawQuery("SELECT COUNT(*) FROM reports WHERE user_phone = ? AND status = 'Pending'", new String[]{phone});
            if (cPending.moveToFirst()) tvPendingReports.setText(String.valueOf(cPending.getInt(0)));
            cPending.close();

            // Resolved Reports
            Cursor cResolved = db.rawQuery("SELECT COUNT(*) FROM reports WHERE user_phone = ? AND status = 'Resolved'", new String[]{phone});
            if (cResolved.moveToFirst()) tvResolvedReports.setText(String.valueOf(cResolved.getInt(0)));
            cResolved.close();

            loadRecentReports(phone);
        }
    }

    private void loadRecentReports(String phone) {
        layoutRecentReports.removeAllViews();
        layoutRecentReports.addView(tvNoReports); // Add it back as we'll hide it if data exists

        Cursor cursor = db.rawQuery("SELECT * FROM reports WHERE user_phone = ? ORDER BY id DESC LIMIT 3", new String[]{phone});
        
        if (cursor.getCount() > 0) {
            tvNoReports.setVisibility(View.GONE);
            while (cursor.moveToNext()) {
                String type = cursor.getString(cursor.getColumnIndexOrThrow("animal_type"));
                String symptoms = cursor.getString(cursor.getColumnIndexOrThrow("symptoms"));
                String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));

                addRecentReportItem(type, symptoms, status, date);
            }
        } else {
            tvNoReports.setVisibility(View.VISIBLE);
        }
        cursor.close();
    }

    private void addRecentReportItem(String type, String symptoms, String status, String date) {
        View itemView = LayoutInflater.from(getContext()).inflate(R.layout.item_report_history, layoutRecentReports, false);
        
        TextView tvTitle = itemView.findViewById(R.id.tv_report_title);
        TextView tvMeta = itemView.findViewById(R.id.tv_report_meta);
        TextView tvStatus = itemView.findViewById(R.id.tv_status_badge);
        TextView tvFooter = itemView.findViewById(R.id.tv_footer_message);

        tvTitle.setText(type + " — " + (symptoms.length() > 30 ? symptoms.substring(0, 27) + "..." : symptoms));
        tvMeta.setText(date);
        tvStatus.setText(status);
        
        // Hide elements not usually needed in a "mini" dashboard view
        tvFooter.setVisibility(View.GONE);

        // Styling status badge
        if (status.equalsIgnoreCase("Pending")) {
            tvStatus.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), R.color.tag_pending_bg));
            tvStatus.setTextColor(ContextCompat.getColor(getContext(), R.color.tag_pending_text));
        } else if (status.equalsIgnoreCase("Investigating")) {
            tvStatus.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), R.color.tag_investigating_bg));
            tvStatus.setTextColor(ContextCompat.getColor(getContext(), R.color.tag_investigating_text));
        } else if (status.equalsIgnoreCase("Resolved")) {
            tvStatus.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), R.color.tag_resolved_bg));
            tvStatus.setTextColor(ContextCompat.getColor(getContext(), R.color.tag_resolved_text));
        }

        layoutRecentReports.addView(itemView);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserData();
    }
}