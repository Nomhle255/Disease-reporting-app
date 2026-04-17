package com.example.diseasealertlesotho;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationHelper {

    private static final String CHANNEL_ID = "disease_alerts_channel";
    private static final String CHANNEL_NAME = "Disease Alerts";

    public static void showNotification(Context context, String title, String message, Class<?> targetActivity, String userPhone, String type) {
        // 1. Save to Database
        saveNotificationToDb(context, title, message, userPhone, type);

        // 2. Show System Notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, targetActivity);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private static void saveNotificationToDb(Context context, String title, String message, String userPhone, String type) {
        SQLiteDatabase db = context.openOrCreateDatabase("DiseaseAlertDB", Context.MODE_PRIVATE, null);
        String date = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        
        ContentValues cv = new ContentValues();
        cv.put("user_phone", userPhone);
        cv.put("title", title);
        cv.put("message", message);
        cv.put("date", date);
        cv.put("type", type);
        
        db.insert("notifications", null, cv);
        db.close();
    }
}