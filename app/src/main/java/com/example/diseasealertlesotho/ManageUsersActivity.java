package com.example.diseasealertlesotho;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

public class ManageUsersActivity extends AppCompatActivity {

    private ListView listView;
    private TextView tvEmptyView;
    private UserAdapter adapter;
    private List<User> userList = new ArrayList<>();
    private List<User> filteredList = new ArrayList<>();
    private SQLiteDatabase db;
    private String currentFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_users);

        View mainView = findViewById(android.R.id.content);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        initViews();
        setupDatabase();
        loadUsers();
        setupFilters();
        setupNavigation();

        findViewById(R.id.tv_back_dashboard).setOnClickListener(v -> finish());
        
        View btnAddUser = findViewById(R.id.btn_add_user);
        if (btnAddUser != null) {
            btnAddUser.setOnClickListener(v -> {
                Intent intent = new Intent(ManageUsersActivity.this, MainActivity.class);
                startActivity(intent);
            });
        }
    }

    private void initViews() {
        listView = findViewById(R.id.list_users);
        tvEmptyView = findViewById(R.id.tv_empty_view);
        
        // Set the empty view for the ListView
        listView.setEmptyView(tvEmptyView);
        
        adapter = new UserAdapter(this, filteredList);
        listView.setAdapter(adapter);
    }

    private void setupDatabase() {
        db = openOrCreateDatabase("DiseaseAlertDB", Context.MODE_PRIVATE, null);
    }

    private void loadUsers() {
        userList.clear();
        try {
            Cursor cursor = db.rawQuery("SELECT * FROM users", null);
            if (cursor.moveToFirst()) {
                do {
                    User user = new User();
                    user.email = cursor.getString(cursor.getColumnIndexOrThrow("email"));
                    user.firstName = cursor.getString(cursor.getColumnIndexOrThrow("firstname"));
                    user.lastName = cursor.getString(cursor.getColumnIndexOrThrow("lastname"));
                    user.role = cursor.getString(cursor.getColumnIndexOrThrow("role"));
                    user.phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
                    userList.add(user);
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        applyFilters();
    }

    private void setupFilters() {
        findViewById(R.id.btn_filter_all).setOnClickListener(v -> updateFilter("All"));
        findViewById(R.id.btn_filter_farmers).setOnClickListener(v -> updateFilter("Farmer"));
        findViewById(R.id.btn_filter_vets).setOnClickListener(v -> updateFilter("Vet"));
        findViewById(R.id.btn_filter_admins).setOnClickListener(v -> updateFilter("Admin"));
    }

    private void updateFilter(String filter) {
        currentFilter = filter;
        applyFilters();
    }

    private void applyFilters() {
        filteredList.clear();
        for (User user : userList) {
            boolean matchesFilter = currentFilter.equals("All") || user.role.equalsIgnoreCase(currentFilter);
            if (matchesFilter) {
                filteredList.add(user);
            }
        }

        // Update empty view text based on filter
        if (filteredList.isEmpty()) {
            switch (currentFilter) {
                case "Farmer":
                    tvEmptyView.setText("No farmer registered yet");
                    break;
                case "Vet":
                    tvEmptyView.setText("No vet registered yet");
                    break;
                case "Admin":
                    tvEmptyView.setText("No admin registered yet");
                    break;
                default:
                    tvEmptyView.setText("No users registered yet");
                    break;
            }
        }

        adapter.notifyDataSetChanged();
    }

    private void setupNavigation() {
        findViewById(R.id.layout_home_tab).setOnClickListener(v -> {
            Intent intent = new Intent(ManageUsersActivity.this, AdminDashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        findViewById(R.id.layout_stats_tab).setOnClickListener(v -> {
            Intent intent = new Intent(ManageUsersActivity.this, StatisticsActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.layout_reports_tab).setOnClickListener(v -> {
            Intent intent = new Intent(ManageUsersActivity.this, AllReportsActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.layout_profile_tab).setOnClickListener(v -> {
            Intent intent = new Intent(ManageUsersActivity.this, AdminDashboardActivity.class);
            intent.putExtra("OPEN_FRAGMENT", "PROFILE");
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });
    }

    // Static User Model
    static class User {
        String email, firstName, lastName, role, phone;
    }

    // Custom Adapter
    private class UserAdapter extends BaseAdapter {
        private Context context;
        private List<User> items;

        public UserAdapter(Context context, List<User> items) {
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
                convertView = LayoutInflater.from(context).inflate(R.layout.user_item, parent, false);
            }

            User user = items.get(position);
            TextView tvName = convertView.findViewById(R.id.tv_user_name);
            TextView tvDetails = convertView.findViewById(R.id.tv_user_details);
            TextView tvInitials = convertView.findViewById(R.id.tv_initials);

            tvName.setText(user.firstName + " " + user.lastName);
            tvDetails.setText(user.role + " · " + user.phone);
            
            String initials = "";
            if (user.firstName != null && !user.firstName.isEmpty()) initials += user.firstName.charAt(0);
            if (user.lastName != null && !user.lastName.isEmpty()) initials += user.lastName.charAt(0);
            if (tvInitials != null) tvInitials.setText(initials.toUpperCase());

            return convertView;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUsers();
    }
}