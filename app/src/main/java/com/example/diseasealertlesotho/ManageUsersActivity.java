package com.example.diseasealertlesotho;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

public class ManageUsersActivity extends AppCompatActivity {

    private ListView listView;
    private EditText etSearch;
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
        setupSearch();
        setupFilters();
        setupNavigation();

        findViewById(R.id.tv_back_dashboard).setOnClickListener(v -> finish());
    }

    private void initViews() {
        listView = findViewById(R.id.list_users);
        etSearch = findViewById(R.id.et_search_user);
        adapter = new UserAdapter(this, filteredList);
        listView.setAdapter(adapter);
    }

    private void setupDatabase() {
        db = openOrCreateDatabase("DiseaseAlertDB", Context.MODE_PRIVATE, null);
    }

    private void loadUsers() {
        userList.clear();
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
        applyFilters("");
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFilters() {
        findViewById(R.id.btn_filter_all).setOnClickListener(v -> updateFilter("All"));
        findViewById(R.id.btn_filter_farmers).setOnClickListener(v -> updateFilter("Farmer"));
        findViewById(R.id.btn_filter_vets).setOnClickListener(v -> updateFilter("Vet"));
        findViewById(R.id.btn_filter_admins).setOnClickListener(v -> updateFilter("Admin"));
    }

    private void updateFilter(String filter) {
        currentFilter = filter;
        applyFilters(etSearch.getText().toString());

        // Visual feedback for selected filter would go here (changing button background)
    }

    private void applyFilters(String query) {
        filteredList.clear();
        for (User user : userList) {
            boolean matchesFilter = currentFilter.equals("All") || user.role.equalsIgnoreCase(currentFilter);
            boolean matchesQuery = user.firstName.toLowerCase().contains(query.toLowerCase()) || 
                                 user.lastName.toLowerCase().contains(query.toLowerCase()) ||
                                 user.email.toLowerCase().contains(query.toLowerCase());
            
            if (matchesFilter && matchesQuery) {
                filteredList.add(user);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void setupNavigation() {
        findViewById(R.id.layout_home_tab).setOnClickListener(v -> {
            Intent intent = new Intent(ManageUsersActivity.this, AdminDashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.layout_stats_tab).setOnClickListener(v -> {
            Intent intent = new Intent(ManageUsersActivity.this, StatisticsActivity.class);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.layout_reports_tab).setOnClickListener(v -> {
            Intent intent = new Intent(ManageUsersActivity.this, AllReportsActivity.class);
            startActivity(intent);
            finish();
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
            tvDetails.setText(user.role + " · " + user.email);
            
            String initials = "";
            if (!user.firstName.isEmpty()) initials += user.firstName.charAt(0);
            if (!user.lastName.isEmpty()) initials += user.lastName.charAt(0);
            tvInitials.setText(initials.toUpperCase());

            return convertView;
        }
    }
}
