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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class VetDashboardActivity extends AppCompatActivity {

    private TextView tvVetName, tvNewCount, tvResolvedCount, tvTotalCount;
    private LinearLayout layoutHomeTab, layoutCasesTab, layoutAlertsTab, layoutProfileTab, layoutRecentReports;
    private View scrollView, fragmentContainer;
    private ImageView ivHome, ivCases, ivAlerts, ivProfile;
    private TextView tvHome, tvCases, tvAlerts, tvProfile;
    private SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vet_dashboard);

        initViews();
        db = openOrCreateDatabase("DiseaseAlertDB", Context.MODE_PRIVATE, null);

        setupData();
        setupNavigation();

        // Handle profile navigation if requested
        String openFragment = getIntent().getStringExtra("OPEN_FRAGMENT");
        if ("PROFILE".equals(openFragment)) {
            loadFragment(new ProfileFragment(), "PROFILE");
            updateNavUI("PROFILE");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStats();
        loadRecentReports();
    }

    private void initViews() {
        scrollView = findViewById(R.id.vet_scroll_view);
        fragmentContainer = findViewById(R.id.vet_fragment_container);
        tvVetName = findViewById(R.id.tv_vet_name);
        
        tvNewCount = findViewById(R.id.tv_stat_new);
        tvResolvedCount = findViewById(R.id.tv_stat_active); // This is the 'Resolved' card in layout
        tvTotalCount = findViewById(R.id.tv_stat_total);

        layoutRecentReports = findViewById(R.id.layout_recent_reports);
        
        layoutHomeTab = findViewById(R.id.layout_home_tab);
        layoutCasesTab = findViewById(R.id.layout_cases_tab);
        layoutAlertsTab = findViewById(R.id.layout_alerts_tab);
        layoutProfileTab = findViewById(R.id.layout_profile_tab);

        ivHome = findViewById(R.id.iv_home_icon);
        ivCases = findViewById(R.id.iv_cases_icon);
        ivAlerts = findViewById(R.id.iv_alerts_icon);
        ivProfile = findViewById(R.id.iv_profile_icon);

        tvHome = findViewById(R.id.tv_home_text);
        tvCases = findViewById(R.id.tv_cases_text);
        tvAlerts = findViewById(R.id.tv_alerts_text);
        tvProfile = findViewById(R.id.tv_profile_text);
    }

    private void setupData() {
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String name = prefs.getString("firstname", "Vet") + " " + prefs.getString("lastname", "Officer");
        tvVetName.setText(name);
    }

    private void updateStats() {
        try {
            // New / Pending
            Cursor cNew = db.rawQuery("SELECT COUNT(*) FROM reports WHERE status = 'Pending' OR status IS NULL", null);
            if (cNew.moveToFirst()) tvNewCount.setText(String.valueOf(cNew.getInt(0)));
            cNew.close();

            // Resolved only
            Cursor cResolved = db.rawQuery("SELECT COUNT(*) FROM reports WHERE status = 'Resolved'", null);
            if (cResolved.moveToFirst()) tvResolvedCount.setText(String.valueOf(cResolved.getInt(0)));
            cResolved.close();

            // Total
            Cursor cTotal = db.rawQuery("SELECT COUNT(*) FROM reports", null);
            if (cTotal.moveToFirst()) tvTotalCount.setText(String.valueOf(cTotal.getInt(0)));
            cTotal.close();
        } catch (Exception ignored) {}
    }

    private void loadRecentReports() {
        layoutRecentReports.removeAllViews();
        try {
            String query = "SELECT r.*, u.firstname, u.lastname FROM reports r " +
                          "LEFT JOIN users u ON r.user_phone = u.phone " +
                          "ORDER BY r.id DESC LIMIT 5";
            Cursor cursor = db.rawQuery(query, null);

            if (cursor.moveToFirst()) {
                do {
                    View itemView = LayoutInflater.from(this).inflate(R.layout.report_item_small, layoutRecentReports, false);
                    
                    TextView tvTitle = itemView.findViewById(R.id.tv_item_title);
                    TextView tvSubtitle = itemView.findViewById(R.id.tv_item_subtitle);
                    ImageView ivPhoto = itemView.findViewById(R.id.iv_item_photo);
                    
                    String fName = cursor.getString(cursor.getColumnIndexOrThrow("firstname"));
                    String lName = cursor.getString(cursor.getColumnIndexOrThrow("lastname"));
                    String animal = cursor.getString(cursor.getColumnIndexOrThrow("animal_type"));
                    String symptoms = cursor.getString(cursor.getColumnIndexOrThrow("symptoms"));
                    String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    byte[] photo = cursor.getBlob(cursor.getColumnIndexOrThrow("photo"));

                    tvTitle.setText((fName != null ? fName + " " + lName : "Unknown") + " — " + animal);
                    tvSubtitle.setText(symptoms + " · " + date);

                    if (photo != null && photo.length > 0) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(photo, 0, photo.length);
                        ivPhoto.setImageBitmap(bitmap);
                    } else {
                        ivPhoto.setImageResource(android.R.drawable.ic_menu_gallery);
                    }

                    itemView.setOnClickListener(v -> {
                        Intent intent = new Intent(this, VetCaseDetailsActivity.class);
                        intent.putExtra("CASE_ID", "RPT-" + String.format("%03d", id));
                        startActivity(intent);
                    });

                    layoutRecentReports.addView(itemView);
                    
                    if (!cursor.isLast()) {
                        View divider = new View(this);
                        divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
                        divider.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                        layoutRecentReports.addView(divider);
                    }

                } while (cursor.moveToNext());
            } else {
                TextView tvNoReports = new TextView(this);
                tvNoReports.setText("No reports found");
                tvNoReports.setPadding(40, 40, 40, 40);
                layoutRecentReports.addView(tvNoReports);
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupNavigation() {
        layoutHomeTab.setOnClickListener(v -> {
            scrollView.setVisibility(View.VISIBLE);
            fragmentContainer.setVisibility(View.GONE);
            updateNavUI("HOME");
            updateStats();
            loadRecentReports();
        });

        layoutCasesTab.setOnClickListener(v -> {
            startActivity(new Intent(this, VetCasesActivity.class));
        });

        layoutAlertsTab.setOnClickListener(v -> {
            startActivity(new Intent(this, VetAlertsActivity.class));
        });

        layoutProfileTab.setOnClickListener(v -> {
            loadFragment(new ProfileFragment(), "PROFILE");
        });
    }

    private void loadFragment(Fragment fragment, String tag) {
        scrollView.setVisibility(View.GONE);
        fragmentContainer.setVisibility(View.VISIBLE);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.vet_fragment_container, fragment, tag);
        transaction.commit();
        updateNavUI(tag);
    }

    private void updateNavUI(String activeTab) {
        int grey = getResources().getColor(android.R.color.darker_gray);
        int green = getResources().getColor(R.color.header_green);

        ivHome.setColorFilter(grey);
        ivCases.setColorFilter(grey);
        ivAlerts.setColorFilter(grey);
        ivProfile.setColorFilter(grey);

        tvHome.setTextColor(grey);
        tvCases.setTextColor(grey);
        tvAlerts.setTextColor(grey);
        tvProfile.setTextColor(grey);

        if ("HOME".equals(activeTab)) {
            ivHome.setColorFilter(green);
            tvHome.setTextColor(green);
        } else if ("PROFILE".equals(activeTab)) {
            ivProfile.setColorFilter(green);
            tvProfile.setTextColor(green);
        }
    }
}