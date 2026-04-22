package com.example.diseasealertlesotho;

import android.content.Context;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class ManageUsersFragment extends Fragment {

    private ListView listView;
    private TextView tvEmptyView;
    private UserAdapter adapter;
    private List<User> userList = new ArrayList<>();
    private List<User> filteredList = new ArrayList<>();
    private DatabaseHelper dbHelper;
    private String currentFilter = "All";

    private MaterialButton btnAll, btnFarmers, btnVets, btnAdmins;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_users, container, false);

        dbHelper = new DatabaseHelper(requireContext());
        initViews(view);
        setupFilters();
        loadUsers();

        return view;
    }

    private void initViews(View view) {
        listView = view.findViewById(R.id.list_users);
        tvEmptyView = view.findViewById(R.id.tv_empty_view);
        
        btnAll = view.findViewById(R.id.btn_filter_all);
        btnFarmers = view.findViewById(R.id.btn_filter_farmers);
        btnVets = view.findViewById(R.id.btn_filter_vets);
        btnAdmins = view.findViewById(R.id.btn_filter_admins);
        
        listView.setEmptyView(tvEmptyView);
        
        adapter = new UserAdapter(requireContext(), filteredList);
        listView.setAdapter(adapter);
    }

    private void loadUsers() {
        userList.clear();
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT * FROM users", null);
            if (cursor.moveToFirst()) {
                do {
                    User user = new User();
                    user.firstName = cursor.getString(cursor.getColumnIndexOrThrow("firstname"));
                    user.lastName = cursor.getString(cursor.getColumnIndexOrThrow("lastname"));
                    user.role = cursor.getString(cursor.getColumnIndexOrThrow("role"));
                    user.phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
                    user.district = cursor.getString(cursor.getColumnIndexOrThrow("district"));
                    userList.add(user);
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        applyFilters();
    }

    private void setupFilters() {
        btnAll.setOnClickListener(v -> updateFilter("All"));
        btnFarmers.setOnClickListener(v -> updateFilter("Farmer"));
        btnVets.setOnClickListener(v -> updateFilter("Veterinary"));
        btnAdmins.setOnClickListener(v -> updateFilter("Admin"));
        updateFilterUI();
    }

    private void updateFilter(String filter) {
        currentFilter = filter;
        applyFilters();
        updateFilterUI();
    }

    private void updateFilterUI() {
        resetButtonStyle(btnAll);
        resetButtonStyle(btnFarmers);
        resetButtonStyle(btnVets);
        resetButtonStyle(btnAdmins);

        MaterialButton selected = btnAll;
        if (currentFilter.equals("Farmer")) selected = btnFarmers;
        else if (currentFilter.equals("Veterinary")) selected = btnVets;
        else if (currentFilter.equals("Admin")) selected = btnAdmins;

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
        for (User user : userList) {
            if (currentFilter.equals("All") || user.role.equalsIgnoreCase(currentFilter)) {
                filteredList.add(user);
            }
        }
        adapter.notifyDataSetChanged();
    }

    static class User {
        String firstName, lastName, role, phone, district;
    }

    private class UserAdapter extends BaseAdapter {
        private Context context;
        private List<User> items;
        public UserAdapter(Context context, List<User> items) { this.context = context; this.items = items; }
        @Override public int getCount() { return items.size(); }
        @Override public Object getItem(int position) { return items.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.user_item, parent, false);
            }
            User user = items.get(position);
            ((TextView)convertView.findViewById(R.id.tv_user_name)).setText(user.firstName + " " + user.lastName);
            
            String details = user.role + " · " + (user.district != null ? user.district : "N/A") + " · " + user.phone;
            ((TextView)convertView.findViewById(R.id.tv_user_details)).setText(details);
            
            String initials = "";
            if (user.firstName != null && !user.firstName.isEmpty()) initials += user.firstName.substring(0,1);
            if (user.lastName != null && !user.lastName.isEmpty()) initials += user.lastName.substring(0,1);
            ((TextView)convertView.findViewById(R.id.tv_initials)).setText(initials.toUpperCase());
            
            return convertView;
        }
    }
}
