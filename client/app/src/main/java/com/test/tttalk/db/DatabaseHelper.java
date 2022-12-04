package com.test.tttalk.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Table Name
    public static final String TABLE_NAME = "users";

    // Table columns
    public static final String _ID = "_id";
    public static final String USERID = "UserId";
    public static final String NICKNAME = "NickName";
    public static final String FROM_ROOM = "FromRoom";
    public static final String TIME = "Time";
    public static final String SEX = "Sex";

    // Database Information
    static final String DB_NAME = "database_users.DB";

    // database version
    static final int DB_VERSION = 1;

    // Creating table query
    private static final String CREATE_TABLE = "create table users(_id INTEGER PRIMARY KEY AUTOINCREMENT, UserId TEXT NOT NULL, NickName TEXT, Sex TEXT, FromRoom TEXT, Time int);";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
}
