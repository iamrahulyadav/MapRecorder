package com.application.ningyitong.maprecorder;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;

public class AccountActivity extends AppCompatActivity {

    Database db;
    Button btnLogout, btnSettings, btnEditProfile;
    TextView username, mapCount;
    String userEmail;

    // user session
    UserSessionManager session;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // session class instance
        session = new UserSessionManager(getApplicationContext());
        // Check user login status
        if (session.checkLogin()) {
            finish();
        }
        setContentView(R.layout.activity_account);

        // Setup bottom nav-bar
        setupBottomNavbar();

        // get user data
        HashMap<String, String> user = session.getUserDetails();
        userEmail = user.get(UserSessionManager.KEY_EMAIL);

        // Show user name
        username = (TextView)findViewById(R.id.account_page_username);
        username.setText(userEmail);

        mapCount = (TextView)findViewById(R.id.account_page_map_count);
        mapCount.setText("");

        // Load user map amount
        displayMapCount();



        // Edit profile
        btnEditProfile = (Button)findViewById(R.id.btn_edit_profile);
        btnEditProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent_edit = new Intent(AccountActivity.this, EditProfileActivity.class);
                startActivity(intent_edit);
            }
        });
        // Settings
        btnSettings = (Button)findViewById(R.id.btn_app_settings);
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent_edit = new Intent(AccountActivity.this, SettingsActivity.class);
                startActivity(intent_edit);
            }
        });

        // Btn logout
        btnLogout = (Button)findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnLogout.setAlpha((float) 0.5);
                // Call confirmation dialog
                signOutConfirmationDialog();
            }
        });
    }

    // display user map amount
    private void displayMapCount() {
        db = new Database(this);
        // Get user_id first
        Cursor data = db.getUserID(userEmail);
        int userID = -1;
        while (data.moveToNext()) {
            userID = data.getInt(0);
        }
        if (userID > -1) {
            // Get map number by searching use user_id
            Cursor mapByUserId = db.searchMapByType(userID, "user_id");
            int count = mapByUserId.getCount();
            mapCount = (TextView)findViewById(R.id.account_page_map_count);
            mapCount.setText(String.valueOf(count));
        } else {
            Toast.makeText(getBaseContext(), "Please login first", Toast.LENGTH_SHORT).show();
        }
    }

    // Sign out confirmation dialog
    public void signOutConfirmationDialog() {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setMessage("Are you sure you want to log out?");
        alertDialog.setCancelable(false);

        // Confirm log out
        alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                session.logoutUser();
            }
        });

        // Cancel operation
        alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                btnLogout.setAlpha((float) 1);
            }
        });

        alertDialog.create().show();
    }

    // Setup nav-bar
    private void setupBottomNavbar() {
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        Menu menu = navigation.getMenu();
        MenuItem menuItem = menu.getItem(4);
        menuItem.setChecked(true);
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_main:
                    Intent intent_main = new Intent(AccountActivity.this, MainActivity.class);
                    startActivity(intent_main);
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    break;
                case R.id.navigation_map:
                    Intent intent_map = new Intent(AccountActivity.this, MapActivity.class);
                    startActivity(intent_map);
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    break;
                case R.id.navigation_edit:
                    Intent intent_edit = new Intent(AccountActivity.this, EditActivity.class);
                    startActivity(intent_edit);
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    break;
                case R.id.navigation_cloud:
                    Intent intent_cloud = new Intent(AccountActivity.this, CloudActivity.class);
                    startActivity(intent_cloud);
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    break;
                case R.id.navigation_account:
                    break;
            }
            return false;
        }
    };
}
