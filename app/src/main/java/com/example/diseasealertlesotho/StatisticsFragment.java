package com.example.diseasealertlesotho;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class StatisticsFragment extends Fragment {

    private DatabaseHelper dbHelper;
    private TextView tvCattleCount, tvSheepCount, tvGoatsCount, tvPoultryCount;
    private ProgressBar pbCattle, pbSheep, pbGoats, pbPoultry;
    private LinearLayout layoutDistrictStats;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statistics, container, false);

        dbHelper = new DatabaseHelper(requireContext());
        initViews(view);
        loadStatistics();

        return view;
    }

    private void initViews(View view) {
        tvCattleCount = view.findViewById(R.id.tv_cattle_count);
        tvSheepCount = view.findViewById(R.id.tv_sheep_count);
        tvGoatsCount = view.findViewById(R.id.tv_goats_count);
        tvPoultryCount = view.findViewById(R.id.tv_poultry_count);

        pbCattle = view.findViewById(R.id.pb_cattle);
        pbSheep = view.findViewById(R.id.pb_sheep);
        pbGoats = view.findViewById(R.id.pb_goats);
        pbPoultry = view.findViewById(R.id.pb_poultry);
        
        layoutDistrictStats = view.findViewById(R.id.layout_district_stats);
    }

    private void loadStatistics() {
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            int totalReports = 0;
            Cursor cTotal = db.rawQuery("SELECT COUNT(*) FROM reports", null);
            if (cTotal.moveToFirst()) {
                totalReports = cTotal.getInt(0);
            }
            cTotal.close();

            if (totalReports == 0) return;

            updateAnimalStat(db, "Cattle", totalReports, tvCattleCount, pbCattle);
            updateAnimalStat(db, "Sheep", totalReports, tvSheepCount, pbSheep);
            updateAnimalStat(db, "Goats", totalReports, tvGoatsCount, pbGoats);
            updateAnimalStat(db, "Poultry", totalReports, tvPoultryCount, pbPoultry);
            
            loadDistrictStats(db, totalReports);
            
        } catch (Exception ignored) {}
    }

    private void updateAnimalStat(SQLiteDatabase db, String type, int total, TextView tvCount, ProgressBar pb) {
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM reports WHERE animal_type = ?", new String[]{type});
        if (c.moveToFirst()) {
            int count = c.getInt(0);
            tvCount.setText(count + " reports");
            int percent = (count * 100) / total;
            pb.setProgress(percent);
        }
        c.close();
    }

    private void loadDistrictStats(SQLiteDatabase db, int total) {
        layoutDistrictStats.removeAllViews();
        
        String query = "SELECT u.district, COUNT(r.id) as count FROM reports r " +
                      "JOIN users u ON r.user_phone = u.phone " +
                      "GROUP BY u.district ORDER BY count DESC";
        
        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do {
                String district = cursor.getString(0);
                int count = cursor.getInt(1);
                
                if (district == null || district.isEmpty()) district = "Other";

                addDistrictCard(district, count, total);

            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    private void addDistrictCard(String district, int count, int total) {
        View cardView = LayoutInflater.from(getContext()).inflate(R.layout.item_stat_card, layoutDistrictStats, false);
        
        TextView tvTitle = cardView.findViewById(R.id.tv_stat_title);
        TextView tvCount = cardView.findViewById(R.id.tv_stat_count);
        ProgressBar pb = cardView.findViewById(R.id.pb_stat);
        
        tvTitle.setText(district);
        tvCount.setText(count + " reports");
        int percent = (count * 100) / total;
        pb.setProgress(percent);
        
        layoutDistrictStats.addView(cardView);
    }
}
