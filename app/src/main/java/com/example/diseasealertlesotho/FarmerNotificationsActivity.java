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

import java.util.ArrayList;
import java.util.List;

public class FarmerNotificationsActivity extends AppCompatActivity {

    private ListView lvToday, lvYesterday;
    private NotificationAdapter adapterToday, adapterYesterday;
    private List<NotificationItem> todayList = new ArrayList<>();
    private List<NotificationItem> yesterdayList = new ArrayList<>();
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
        lvYesterday = findViewById(R.id.lv_notifications_yesterday);

        adapterToday = new NotificationAdapter(this, todayList);
        adapterYesterday = new NotificationAdapter(this, yesterdayList);

        lvToday.setAdapter(adapterToday);
        lvYesterday.setAdapter(adapterYesterday);
    }

    private void loadNotificationsFromDb() {
        todayList.clear();
        yesterdayList.clear();

        SharedPreferences sp = getSharedPreferences("UserSession", MODE_PRIVATE);
        String phone = sp.getString("phone", "");

        // Fetch notifications for this farmer
        Cursor cursor = db.rawQuery("SELECT * FROM notifications WHERE user_phone = ? AND type = 'Farmer' ORDER BY id DESC", new String[]{phone});

        if (cursor.moveToFirst()) {
            do {
                String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                String message = cursor.getString(cursor.getColumnIndexOrThrow("message"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                
                // For now, put all in "Today" for simplicity, or you could parse the date
                todayList.add(new NotificationItem(
                        title,
                        message + " · " + date,
                        android.R.drawable.stat_sys_warning,
                        "#1E88E5"
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();

        adapterToday.notifyDataSetChanged();
        adapterYesterday.notifyDataSetChanged();
        
        setListViewHeightBasedOnChildren(lvToday);
        setListViewHeightBasedOnChildren(lvYesterday);
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
