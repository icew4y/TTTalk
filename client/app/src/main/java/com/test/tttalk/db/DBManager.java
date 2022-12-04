package com.test.tttalk.db;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class DBManager {

    private DatabaseHelper dbHelper;

    private Context context;

    private SQLiteDatabase database;

    public DBManager(Context c) {
        context = c;
    }

    public DBManager open() throws SQLException {
        dbHelper = new DatabaseHelper(context);
        database = dbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        dbHelper.close();
    }

    public void insert(String userId, String nickName, String sex, String fromRoom) {
        ContentValues contentValue = new ContentValues();
        contentValue.put(DatabaseHelper.USERID, userId);
        contentValue.put(DatabaseHelper.NICKNAME, nickName);
        contentValue.put(DatabaseHelper.SEX, sex);
        contentValue.put(DatabaseHelper.FROM_ROOM, fromRoom);
        contentValue.put(DatabaseHelper.TIME, System.currentTimeMillis());
        database.insert(DatabaseHelper.TABLE_NAME, null, contentValue);
    }

    public Cursor fetch() {
        String[] columns = new String[] { DatabaseHelper._ID, DatabaseHelper.USERID, DatabaseHelper.NICKNAME, DatabaseHelper.SEX, DatabaseHelper.FROM_ROOM , DatabaseHelper.TIME};
        Cursor cursor = database.query(DatabaseHelper.TABLE_NAME, columns, null, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    public int update(long _id, String userId, String nickName, String sex, String fromRoom) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.USERID, userId);
        contentValues.put(DatabaseHelper.NICKNAME, nickName);
        contentValues.put(DatabaseHelper.SEX, sex);
        contentValues.put(DatabaseHelper.FROM_ROOM, fromRoom);
        contentValues.put(DatabaseHelper.TIME, System.currentTimeMillis());
        int i = database.update(DatabaseHelper.TABLE_NAME, contentValues, DatabaseHelper._ID + " = " + _id, null);
        return i;
    }

    public void delete(long _id) {
        database.delete(DatabaseHelper.TABLE_NAME, DatabaseHelper._ID + "=" + _id, null);
    }

}