package com.example.diseasealertlesotho;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "DiseaseAlertDB";
    private static final int DATABASE_VERSION = 4;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Users table
        db.execSQL("CREATE TABLE users(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "phone VARCHAR UNIQUE, " +
                "email VARCHAR UNIQUE, " +
                "firstname VARCHAR, " +
                "lastname VARCHAR, " +
                "role VARCHAR, " +
                "password VARCHAR, " +
                "district VARCHAR, " +
                "village VARCHAR);");

        // Reports table
        db.execSQL("CREATE TABLE reports(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER, " +
                "animal_type VARCHAR, " +
                "count INTEGER, " +
                "symptoms TEXT, " +
                "date VARCHAR, " +
                "photo BLOB, " +
                "gps_location VARCHAR, " +
                "status VARCHAR DEFAULT 'Pending', " +
                "FOREIGN KEY(user_id) REFERENCES users(id));");

        // Notifications table
        db.execSQL("CREATE TABLE notifications(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER, " +
                "title VARCHAR, " +
                "message TEXT, " +
                "date VARCHAR, " +
                "type VARCHAR, " +
                "FOREIGN KEY(user_id) REFERENCES users(id));");

        // Responses table
        db.execSQL("CREATE TABLE responses(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "report_id INTEGER, " +
                "vet_id INTEGER, " +
                "farmer_id INTEGER, " +
                "response_type VARCHAR, " +
                "message TEXT, " +
                "status_changed_to VARCHAR, " +
                "date_responded VARCHAR, " +
                "FOREIGN KEY(report_id) REFERENCES reports(id), " +
                "FOREIGN KEY(vet_id) REFERENCES users(id), " +
                "FOREIGN KEY(farmer_id) REFERENCES users(id));");

        // More Info table
        db.execSQL("CREATE TABLE more_info(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "report_id INTEGER, " +
                "response_id INTEGER, " +
                "farmer_id INTEGER, " +
                "farmer_message TEXT, " +
                "photo BLOB, " +
                "date_submitted VARCHAR, " +
                "FOREIGN KEY(report_id) REFERENCES reports(id), " +
                "FOREIGN KEY(response_id) REFERENCES responses(id), " +
                "FOREIGN KEY(farmer_id) REFERENCES users(id));");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Simple upgrade policy
        db.execSQL("DROP TABLE IF EXISTS more_info");
        db.execSQL("DROP TABLE IF EXISTS responses");
        db.execSQL("DROP TABLE IF EXISTS notifications");
        db.execSQL("DROP TABLE IF EXISTS reports");
        db.execSQL("DROP TABLE IF EXISTS users");
        onCreate(db);
    }
}
