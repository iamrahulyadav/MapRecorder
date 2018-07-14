package com.application.ningyitong.maprecorder;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.sql.Statement;

public class Database extends SQLiteOpenHelper {
    private static final String DB_NAME = "MapRecorder.db";
    private static final int DB_VERSION = 1;

    // Define parameters of table user
    private static final String TABLE_USER = "user";
    private static final String USER_COL_ID = "id";
    private static final String USER_COL_EMAIL = "email";
    private static final String USER_COL_PASS = "password";

    // Define parameters of table map
    private static final String TABLE_MAP = "map";
    private static final String MAP_COL_ID = "id";
    private static final String MAP_COL_NAME = "name";
    private static final String MAP_COL_CITY = "city";
    private static final String MAP_COL_DESCRIPTION = "description";
    private static final String MAP_COL_OWNER = "owner";
    private static final String MAP_COL_DATE = "date";

    public Database(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("Create table user(email text primary key, password text)");
        db.execSQL("Create table map()");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table if exists user");
    }

    // save user to db
    public boolean saveUser(String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("email", email);
        contentValues.put("password", password);
        long ins = db.insert("user", null, contentValues);
        if (ins==-1) return false;
        else return true;
    }
    // checking if email exists
    public Boolean checkEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("Select * from user where email=?", new String[]{email});
        if (cursor.getCount()>0) return false;
        else return true;
    }

    // checking the email and password
    public Boolean loginValidation(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from user where email=? and password=?", new String[]{email, password});
        if (cursor.getCount()>0) return true;
        else return false;
    }

}
