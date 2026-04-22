package com.example.diseasealertlesotho;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FarmerDiseaseAlertsFragment extends Fragment {

    private LinearLayout layoutToday, layoutPrevious;
    private TextView tvNoTodayAlerts, tvNoPreviousAlerts;
    private DatabaseHelper dbHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_farmer_disease_alerts, container, false);

        dbHelper = new DatabaseHelper(requireContext());
        initViews(view);
        loadAlertsFromDb();

        return view;
    }

    private void initViews(View view) {
        layoutToday = view.findViewById(R.id.layout_notifications_today);
        layoutPrevious = view.findViewById(R.id.layout_notifications_previous);
        tvNoTodayAlerts = view.findViewById(R.id.tv_no_today_alerts);
        tvNoPreviousAlerts = view.findViewById(R.id.tv_no_previous_alerts);
    }

    private void loadAlertsFromDb() {
        if (!isAdded()) return;

        layoutToday.removeAllViews();
        layoutPrevious.removeAllViews();

        SharedPreferences sp = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String district = sp.getString("district", "");

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String todayDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        String districtTarget = "DISTRICT:" + district;
        
        // Strictly load only 'ALERT' type notifications targeted at the user's district
        String query = "SELECT * FROM notifications WHERE user_phone = ? AND type = 'ALERT' ORDER BY id DESC";
        Cursor cursor = db.rawQuery(query, new String[]{districtTarget});

        int todayCount = 0;
        int prevCount = 0;

        if (cursor.moveToFirst()) {
            do {
                String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                String message = cursor.getString(cursor.getColumnIndexOrThrow("message"));
                String dateTime = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                
                int icon = android.R.drawable.stat_sys_warning;
                String color = "#D32F2F"; // Alert Red

                View alertView = createAlertItemView(title, message + " · " + dateTime, icon, color);

                if (dateTime != null && dateTime.startsWith(todayDate)) {
                    layoutToday.addView(alertView);
                    todayCount++;
                } else {
                    layoutPrevious.addView(alertView);
                    prevCount++;
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        
        tvNoTodayAlerts.setVisibility(todayCount == 0 ? View.VISIBLE : View.GONE);
        tvNoPreviousAlerts.setVisibility(prevCount == 0 ? View.VISIBLE : View.GONE);
    }

    private View createAlertItemView(String title, String message, int iconRes, String colorCode) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.item_notification, layoutToday, false);
        
        TextView tvTitle = view.findViewById(R.id.tv_notification_title);
        TextView tvMessage = view.findViewById(R.id.tv_notification_message);
        ImageView ivIcon = view.findViewById(R.id.iv_notification_icon);
        View dot = view.findViewById(R.id.view_status_dot);

        tvTitle.setText(title);
        tvMessage.setText(message);
        ivIcon.setImageResource(iconRes);
        dot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(colorCode)));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAlertsFromDb();
    }
}
