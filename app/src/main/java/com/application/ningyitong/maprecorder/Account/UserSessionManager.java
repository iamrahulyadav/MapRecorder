package com.application.ningyitong.maprecorder.Account;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.HashMap;

public class UserSessionManager {
    // shared preferences
    private SharedPreferences preferences;

    // editor reference
    private SharedPreferences.Editor editor;

    // context
    private Context _context;

    // shared preference mode
    static private int PRVATE_MODE = 0;

    // shared preference file name
    private static final String PREFERENCE_NAME = "MapRecorderPreference";

    // shared preferences keys
    private static final String IS_USER_LOGIN = "IsUserLoggedIn";

    // user email
//    public static final String KEY_EMAIL = "email";
//    public static final String KEY_NAME = "username";
    public static final String KEY_USERID = "user_id";

    // constructor
    public UserSessionManager(Context context) {
        this._context = context;
        preferences = _context.getSharedPreferences(PREFERENCE_NAME, PRVATE_MODE);
        editor = preferences.edit();
    }

    // create login session
    public void createUserLoginSession(int userID) {
        // save login value as true
        editor.putBoolean(IS_USER_LOGIN, true);
        // save username in preference
        editor.putInt(KEY_USERID, userID);
        // commit changes
        editor.commit();
    }

    // check login
    public boolean checkLogin() {
        if (!this.isUserLoggedIn()) {
            Intent intent = new Intent(_context, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            _context.startActivity(intent);

            return true;
        }
        return false;
    }

    // save session data
    public HashMap<String, Integer> getUserDetails() {
        HashMap<String, Integer> user = new HashMap<>();
        user.put(KEY_USERID, preferences.getInt(KEY_USERID, -1));
        return user;
    }

    // remove session
    public void logoutUser() {
        editor.clear();
        editor.commit();

        Intent intent = new Intent(_context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        _context.startActivity(intent);
    }

    // check for login
    public boolean isUserLoggedIn() {
        return preferences.getBoolean(IS_USER_LOGIN, false);
    }
}
