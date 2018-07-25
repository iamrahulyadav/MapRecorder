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
    private static final String MAP_COL_CREATOR = "creator";
    private static final String MAP_COL_TRACKING = "tracking";
    private static final String MAP_COL_USERID = "user_id";

    public Database(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("Create table user(id INTEGER PRIMARY KEY AUTOINCREMENT, email text, password text)");
        db.execSQL("Create table map(id INTEGER PRIMARY KEY AUTOINCREMENT, name text, city text, description text, owner text, date text, creator text, tracking text, user_id INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table if exists user");
        db.execSQL("drop table if exists map");
    }

    // save user to db
    public boolean saveUser(String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(USER_COL_EMAIL, email);
        contentValues.put(USER_COL_PASS, password);
        long ins = db.insert(TABLE_USER, null, contentValues);
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

    // checking if email exists
    public Boolean checkMap(String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("Select * from map where name=?", new String[]{name});
        if (cursor.getCount()>0) return false;
        else return true;
    }

    // Get user id by user name
    public Cursor getUserID(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor data = db.rawQuery("SELECT * FROM user WHERE email=?", new String[]{name});
        return data;
    }
    public Cursor getMapCount(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor data = db.rawQuery("Select * from user where email=?", new String[]{email});
        return data;
    }

    // save map to db
    public boolean saveMap(String name, String city, String description, String owner, String date, String creator, String tracking, int user_id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MAP_COL_NAME, name);
        contentValues.put(MAP_COL_CITY, city);
        contentValues.put(MAP_COL_DESCRIPTION, description);
        contentValues.put(MAP_COL_OWNER, owner);
        contentValues.put(MAP_COL_DATE, date);
        contentValues.put(MAP_COL_CREATOR, creator);
        contentValues.put(MAP_COL_TRACKING, tracking);
        contentValues.put(MAP_COL_USERID, user_id);
        long ins = db.insert(TABLE_MAP, null, contentValues);
        if (ins==-1) return false;
        else return true;
    }

    // get table data
    public Cursor getTableItems() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor data = db.rawQuery("SELECT * FROM map", null);
        return data;
    }

    // Search map by custom col
    public Cursor searchMapByType(String searchContent, String searchTypeContent) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor data = db.rawQuery("SELECT * FROM map WHERE " + searchTypeContent + "=?", new String[]{searchContent});
        return data;
    }
    public Cursor searchMapByType(int searchContent, String searchTypeContent) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor data = db.rawQuery("SELECT * FROM map WHERE " + searchTypeContent + "=" + searchContent, null);
        return data;
    }

    // Get map id by map name
    public Cursor getMapID(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor data = db.rawQuery("SELECT * FROM map WHERE name=?", new String[]{name});
        return data;
    }

    // Get map details by id
    public Cursor getMapInfoById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor data = db.rawQuery("SELECT * FROM map WHERE id=" + id, null);
        return data;
    }

    // Update map by id
    public void updateMapInfo(String newName, String newCity, String newOwner, String newDate, String newDescription, int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE map SET name='"+newName+"', city='"+newCity+"', owner='"+newOwner+"', date='"+newDate+"', description='"+newDescription+"' WHERE id=" + id);
    }

    // Delete map by id
    public void deleteMap(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM map where id=" + id);
    }

}
