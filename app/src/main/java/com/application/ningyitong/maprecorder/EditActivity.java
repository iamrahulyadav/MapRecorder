package com.application.ningyitong.maprecorder;

import android.content.Intent;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

public class EditActivity extends AppCompatActivity {
    Database db;
    private ArrayList<HashMap<String, String>> mapList;
    private ListView listView;

    // user session
    UserSessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new UserSessionManager(getApplicationContext());
        // Check user login status
        if (session.checkLogin()) {
            finish();
        }
        setContentView(R.layout.activity_edit);

        // Setup bottom nav-bar
        setupBottomNavbar();

        // Create list view
        listView = (ListView)findViewById(R.id.mapDataList);
        mapList = new ArrayList<>();

        // Load local map list
        loadMapList();
    }

    private void loadMapList() {
        db = new Database(this);
        Cursor mapItems = db.getTableItems();
        mapList.clear();
        if (mapItems.getCount()>0) {
            mapItems.moveToFirst();
            for (int i=0; i<mapItems.getCount(); i++) {
                String listName = mapItems.getString(mapItems.getColumnIndex("name"));
                String listOwner = mapItems.getString(mapItems.getColumnIndex("owner"));
                String listDescription = mapItems.getString(mapItems.getColumnIndex("description"));
                HashMap<String, String> maps = new HashMap<>();
                maps.put("name", listName);
                maps.put("owner", listOwner);
                maps.put("description", listDescription);
                mapList.add(maps);
                mapItems.moveToNext();
            }
            ListAdapter adapter = new SimpleAdapter(EditActivity.this,
                    mapList,
                    R.layout.map_listview_items,
                    new String[] {"name", "description"},
                    new int[] {R.id.list_item_map_name, R.id.list_item_map_description});
            listView.setAdapter(adapter);
        } else {
            listView.setAdapter(null);
            Toast.makeText(this, "No Map Data", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupBottomNavbar() {
        // Bottom nav-bar
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        Menu menu = navigation.getMenu();
        MenuItem menuItem = menu.getItem(2);
        menuItem.setChecked(true);
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_main:
                    Intent intent_main = new Intent(EditActivity.this, MainActivity.class);
                    startActivity(intent_main);
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    break;
                case R.id.navigation_map:
                    Intent intent_map = new Intent(EditActivity.this, MapActivity.class);
                    startActivity(intent_map);
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    break;
                case R.id.navigation_edit:
                    break;
                case R.id.navigation_cloud:
                    Intent intent_cloud = new Intent(EditActivity.this, CloudActivity.class);
                    startActivity(intent_cloud);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    break;
                case R.id.navigation_account:
                    Intent intent_account = new Intent(EditActivity.this, AccountActivity.class);
                    startActivity(intent_account);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    break;
            }
            return false;
        }
    };
}
