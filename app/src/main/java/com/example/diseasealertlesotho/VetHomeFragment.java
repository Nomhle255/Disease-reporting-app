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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class VetHomeFragment extends Fragment {

    private TextView tvVetName, tvNewCount, tvResolvedCount, tvTotalCount, tvNoReports;
    private LinearLayout layoutRecentReports;
    private DatabaseHelper dbHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_vet_home, container, false);

        tvVetName = view.findViewById(R.id.tv_vet_name);
        tvNewCount = view.findViewById(R.id.tv_stat_new);
        tvResolvedCount = view.findViewById(R.id.tv_stat_active);
        tvTotalCount = view.findViewById(R.id.tv_stat_total);
        tvNoReports = view.findViewById(R.id.tv_no_reports_vet);
        layoutRecentReports = view.findViewById(R.id.layout_recent_reports);

        dbHelper = new DatabaseHelper(requireContext());

        setupData();
        updateStats();
        loadRecentReports();

        return view;
    }

    private void setupData() {
        if (!isAdded()) return;
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String name = prefs.getString("firstname", "Vet") + " " + prefs.getString("lastname", "Officer");
        tvVetName.setText(name);
    }

    private void updateStats() {
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cNew = db.rawQuery("SELECT COUNT(*) FROM reports WHERE status = 'Pending' OR status IS NULL", null);
            if (cNew.moveToFirst()) tvNewCount.setText(String.valueOf(cNew.getInt(0)));
            cNew.close();

            Cursor cResolved = db.rawQuery("SELECT COUNT(*) FROM reports WHERE status = 'Resolved'", null);
            if (cResolved.moveToFirst()) tvResolvedCount.setText(String.valueOf(cResolved.getInt(0)));
            cResolved.close();

            Cursor cTotal = db.rawQuery("SELECT COUNT(*) FROM reports", null);
            if (cTotal.moveToFirst()) tvTotalCount.setText(String.valueOf(cTotal.getInt(0)));
            cTotal.close();
        } catch (Exception ignored) {}
    }

    private void loadRecentReports() {
        layoutRecentReports.removeAllViews();
        layoutRecentReports.addView(tvNoReports);
        
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String query = "SELECT r.*, u.firstname, u.lastname FROM reports r " +
                          "LEFT JOIN users u ON r.user_phone = u.phone " +
                          "ORDER BY r.id DESC LIMIT 5";
            Cursor cursor = db.rawQuery(query, null);

            if (cursor.moveToFirst()) {
                tvNoReports.setVisibility(View.GONE);
                do {
                    View itemView = LayoutInflater.from(getContext()).inflate(R.layout.report_item_small, layoutRecentReports, false);
                    
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
                        Intent intent = new Intent(getActivity(), VetCaseDetailsActivity.class);
                        intent.putExtra("CASE_ID", "RPT-" + String.format("%03d", id));
                        startActivity(intent);
                    });

                    layoutRecentReports.addView(itemView);

                } while (cursor.moveToNext());
            } else {
                tvNoReports.setVisibility(View.VISIBLE);
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateStats();
        loadRecentReports();
    }
}
