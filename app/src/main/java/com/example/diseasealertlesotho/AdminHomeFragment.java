package com.example.diseasealertlesotho;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class AdminHomeFragment extends Fragment {

    private TextView tvTotalUsers, tvTotalReports;
    private DatabaseHelper dbHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_home, container, false);

        tvTotalUsers = view.findViewById(R.id.tv_count_total_users);
        tvTotalReports = view.findViewById(R.id.tv_count_total_reports);
        dbHelper = new DatabaseHelper(requireContext());

        setupClickListeners(view);
        updateStats();

        return view;
    }

    private void updateStats() {
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cUsers = db.rawQuery("SELECT COUNT(*) FROM users", null);
            if (cUsers.moveToFirst()) {
                tvTotalUsers.setText(String.valueOf(cUsers.getInt(0)));
            }
            cUsers.close();
            
            Cursor cReports = db.rawQuery("SELECT COUNT(*) FROM reports", null);
            if (cReports.moveToFirst()) {
                tvTotalReports.setText(String.valueOf(cReports.getInt(0)));
            }
            cReports.close();
        } catch (Exception ignored) {}
    }

    private void setupClickListeners(View view) {
        // Switch fragments within the AdminDashboardActivity
        AdminDashboardActivity activity = (AdminDashboardActivity) getActivity();
        if (activity == null) return;

        view.findViewById(R.id.layout_manage_users).setOnClickListener(v -> {
            activity.loadFragment(new ManageUsersFragment(), "USERS");
        });
        
        view.findViewById(R.id.layout_view_reports).setOnClickListener(v -> {
            activity.loadFragment(new ManageReportsFragment(), "REPORTS");
        });
            
        view.findViewById(R.id.layout_statistics).setOnClickListener(v -> {
            activity.loadFragment(new StatisticsFragment(), "STATS");
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        updateStats();
    }
}
