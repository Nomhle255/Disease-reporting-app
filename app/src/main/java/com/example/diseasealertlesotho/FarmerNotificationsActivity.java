package com.example.diseasealertlesotho;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FarmerNotificationsActivity extends AppCompatActivity {

    private ListView lvToday, lvPrevious;
    private NotificationAdapter adapterToday, adapterPrevious;
    private List<NotificationItem> todayList = new ArrayList<>();
    private List<NotificationItem> previousList = new ArrayList<>();
    private TextView tvNoTodayAlerts, tvNoPreviousAlerts;
    private SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_farmer_notifications);

        View mainView = findViewById(android.R.id.content);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        db = openOrCreateDatabase("DiseaseAlertDB", Context.MODE_PRIVATE, null);

        initViews();
        loadNotificationsFromDb();
        setupNavigation();
    }

    private void initViews() {
        lvToday = findViewById(R.id.lv_notifications_today);
        lvPrevious = findViewById(R.id.lv_notifications_previous);
        tvNoTodayAlerts = findViewById(R.id.tv_no_today_alerts);
        tvNoPreviousAlerts = findViewById(R.id.tv_no_previous_alerts);

        adapterToday = new NotificationAdapter(this, todayList);
        adapterPrevious = new NotificationAdapter(this, previousList);

        lvToday.setAdapter(adapterToday);
        lvPrevious.setAdapter(adapterPrevious);
    }

    private void loadNotificationsFromDb() {
        todayList.clear();
        previousList.clear();

        SharedPreferences sp = getSharedPreferences("UserSession", MODE_PRIVATE);
        String phone = sp.getString("phone", "");
        String district = sp.getString("district", "");

        if (district.isEmpty()) {
            try {
                Cursor c = db.rawQuery("SELECT district FROM users WHERE phone = ?", new String[]{phone});
                if (c.moveToFirst()) {
                    district = c.getString(0);
                    sp.edit().putString("district", district).apply();
                }
                c.close();
            } catch (Exception ignored) {}
        }

        String todayDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

        String districtTarget = "DISTRICT:" + district;
        // Only load notifications of type 'ALERT'
        String query = "SELECT * FROM notifications WHERE (user_phone = ? OR user_phone = ?) AND type = 'ALERT' ORDER BY id DESC";
        
        Cursor cursor = db.rawQuery(query, new String[]{phone, districtTarget});

        if (cursor.moveToFirst()) {
            do {
                String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                String message = cursor.getString(cursor.getColumnIndexOrThrow("message"));
                String dateTime = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                String type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
                
                int icon = android.R.drawable.ic_dialog_info;
                String color = "#1E88E5"; 
                
                if ("ALERT".equals(type)) {
                    icon = android.R.drawable.stat_sys_warning;
                    color = "#D32F2F"; 
                }

                NotificationItem item = new NotificationItem(title, message + " · " + dateTime, icon, color);

                if (dateTime != null && dateTime.startsWith(todayDate)) {
                    todayList.add(item);
                } else {
                    previousList.add(item);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();

        adapterToday.notifyDataSetChanged();
        adapterPrevious.notifyDataSetChanged();
        
        // Handle Today section visibility
        if (todayList.isEmpty()) {
            tvNoTodayAlerts.setVisibility(View.VISIBLE);
            lvToday.setVisibility(View.GONE);
        } else {
            tvNoTodayAlerts.setVisibility(View.GONE);
            lvToday.setVisibility(View.VISIBLE);
        }

        // Handle Previous section visibility
        if (previousList.isEmpty()) {
            tvNoPreviousAlerts.setVisibility(View.VISIBLE);
            lvPrevious.setVisibility(View.GONE);
        } else {
            tvNoPreviousAlerts.setVisibility(View.GONE);
            lvPrevious.setVisibility(View.VISIBLE);
        }

        setListViewHeightBasedOnChildren(lvToday);
        setListViewHeightBasedOnChildren(lvPrevious);
    }

    private void setupNavigation() {
        findViewById(R.id.layout_home_tab).setOnClickListener(v -> finish());
        findViewById(R.id.layout_reports_tab).setOnClickListener(v -> {
            startActivity(new Intent(this, FarmerReportHistoryActivity.class));
            finish();
        });
        findViewById(R.id.layout_report_btn).setOnClickListener(v -> {
            startActivity(new Intent(this, ReportDiseaseActivity.class));
        });
        findViewById(R.id.layout_profile_tab).setOnClickListener(v -> {
            Intent intent = new Intent(this, FarmerDashboardActivity.class);
            intent.putExtra("OPEN_FRAGMENT", "PROFILE");
            startActivity(intent);
            finish();
        });
    }

    private void setListViewHeightBasedOnChildren(ListView listView) {
        NotificationAdapter adapter = (NotificationAdapter) listView.getAdapter();
        if (adapter == null || adapter.getCount() == 0) {
            listView.getLayoutParams().height = 0;
            listView.requestLayout();
            return;
        }
        int totalHeight = 0;
        for (int i = 0; i < adapter.getCount(); i++) {
            View listItem = adapter.getView(i, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (adapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }

    static class NotificationItem {
        String title, message, colorCode;
        int iconRes;

        public NotificationItem(String title, String message, int iconRes, String colorCode) {
            this.title = title;
            this.message = message;
            this.iconRes = iconRes;
            this.colorCode = colorCode;
        }
    }

    private class NotificationAdapter extends BaseAdapter {
        private Context context;
        private List<NotificationItem> items;

        public NotificationAdapter(Context context, List<NotificationItem> items) {
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
                convertView = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
            }

            NotificationItem item = items.get(position);
            TextView tvTitle = convertView.findViewById(R.id.tv_notification_title);
            TextView tvMessage = convertView.findViewById(R.id.tv_notification_message);
            ImageView ivIcon = convertView.findViewById(R.id.iv_notification_icon);
            View dot = convertView.findViewById(R.id.view_status_dot);

            tvTitle.setText(item.title);
            tvMessage.setText(item.message);
            ivIcon.setImageResource(item.iconRes);
            dot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(item.colorCode)));

            return convertView;
        }
    }
}