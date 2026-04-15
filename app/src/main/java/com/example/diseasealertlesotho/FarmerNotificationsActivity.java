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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

public class FarmerNotificationsActivity extends AppCompatActivity {

    private ListView lvToday;
    private NotificationAdapter adapterToday;
    private List<NotificationItem> todayList = new ArrayList<>();
    private SQLiteDatabase db;
    private String userPhone;

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

        SharedPreferences sp = getSharedPreferences("UserSession", MODE_PRIVATE);
        userPhone = sp.getString("phone", "");

        db = openOrCreateDatabase("DiseaseAlertDB", Context.MODE_PRIVATE, null);

        initViews();
        loadNotifications();
        setupNavigation();
    }

    private void initViews() {
        lvToday = findViewById(R.id.lv_notifications_today);
        // We will use one list for all notifications for now
        adapterToday = new NotificationAdapter(this, todayList);
        lvToday.setAdapter(adapterToday);
        
        // Hide Yesterday label if not using it for now
        TextView tvYesterday = findViewById(R.id.tv_yesterday_label);
        if (tvYesterday != null) tvYesterday.setVisibility(View.GONE);
        ListView lvYesterday = findViewById(R.id.lv_notifications_yesterday);
        if (lvYesterday != null) lvYesterday.setVisibility(View.GONE);
    }

    private void loadNotifications() {
        todayList.clear();
        try {
            String query = "SELECT res.*, u.firstname, u.lastname FROM responses res " +
                          "LEFT JOIN users u ON res.vet_phone = u.phone " +
                          "WHERE res.farmer_phone = ? " +
                          "ORDER BY res.response_id DESC";
            
            Cursor cursor = db.rawQuery(query, new String[]{userPhone});
            
            if (cursor.moveToFirst()) {
                do {
                    String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
                    String response = cursor.getString(cursor.getColumnIndexOrThrow("response"));
                    String vFname = cursor.getString(cursor.getColumnIndexOrThrow("firstname"));
                    String vLname = cursor.getString(cursor.getColumnIndexOrThrow("lastname"));
                    String reportId = "RPT-" + String.format("%03d", cursor.getInt(cursor.getColumnIndexOrThrow("report_id")));
                    
                    String title;
                    int icon;
                    String color;

                    if ("Investigating".equalsIgnoreCase(status)) {
                        title = "More Info Requested — " + reportId;
                        icon = android.R.drawable.ic_menu_camera;
                        color = "#1E88E5";
                    } else if ("Scheduled".equalsIgnoreCase(status)) {
                        title = "Farm Visit Scheduled — " + reportId;
                        icon = android.R.drawable.ic_menu_agenda;
                        color = "#1976D2";
                    } else {
                        title = "Advice Provided — " + reportId;
                        icon = android.R.drawable.checkbox_on_background;
                        color = "#43A047";
                    }

                    String vetName = (vFname != null ? "Dr. " + vFname : "A Vet");
                    String message = response + " — " + vetName;

                    todayList.add(new NotificationItem(title, message, icon, color));
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        adapterToday.notifyDataSetChanged();
        setListViewHeightBasedOnChildren(lvToday);
    }

    private void setupNavigation() {
        findViewById(R.id.layout_home_tab).setOnClickListener(v -> {
            finish();
        });
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
        if (adapter == null) return;
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