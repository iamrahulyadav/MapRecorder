package com.application.ningyitong.maprecorder;

import android.content.Intent;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EditActivity extends AppCompatActivity {
    Database db;
    EditText searchText;
    ImageView searchBtn;
    Spinner searchType;
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

        // Create search bar
        searchText = (EditText)findViewById(R.id.edit_page_search_text);
        // Create search type spinner;
        createSearchTypeSpinner();
        // Create search button
        searchBtn = (ImageButton)findViewById(R.id.edit_page_search_btn);
        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String searchContent = searchText.getText().toString();
                if (searchContent.equals("") || searchContent.equals("Search...")) {
                    loadMapList();
                    Toast.makeText(getBaseContext(), "No search content detected, show all map list", Toast.LENGTH_LONG).show();
                } else {
                    String searchTypeContent = searchType.getSelectedItem().toString();
                    searchMapByType(searchContent, searchTypeContent);
                    Toast.makeText(getBaseContext(), "Search Content: " + searchContent + "; search type: " + searchTypeContent, Toast.LENGTH_LONG).show();
                }
            }
        });

        // Create list view
        listView = (ListView)findViewById(R.id.mapDataList);
        mapList = new ArrayList<>();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                // Get item string and remove the first character '{'
                String item = listView.getItemAtPosition(i).toString().substring(1);
//                Toast.makeText(getBaseContext(), item, Toast.LENGTH_SHORT).show();
                // Divide string into 4 parts by ','
                String[] mapInfo = item.split(",", 3);
                // Get the map name
                String name = mapInfo[0].replace("name=", "");
//                Toast.makeText(getBaseContext(), name, Toast.LENGTH_SHORT).show();

                Cursor data = db.getMapID(name);
                int mapID = -1;
                while (data.moveToNext()) {
                    mapID = data.getInt(0);
                }
                if (mapID > -1) {
//                    Toast.makeText(getBaseContext(), "Map ID is: " + mapID, Toast.LENGTH_SHORT).show();
                    Intent editMapItemActivity = new Intent(EditActivity.this, EditMapItemActivity.class);
                    editMapItemActivity.putExtra("id", mapID);
                    editMapItemActivity.putExtra("name", name);
                    startActivity(editMapItemActivity);
                } else {
                    Toast.makeText(getBaseContext(), "Cannot find the map", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Load local map list
        loadMapList();
    }

    // Create search type spinner
    private void createSearchTypeSpinner() {
        searchType = (Spinner)findViewById(R.id.edit_page_search_dropdown);
    }

    private void loadMapList() {
        db = new Database(this);
        Cursor mapItems = db.getTableItems();
        mapList.clear();
        if (mapItems.getCount()>0) {
            mapItems.moveToFirst();
            for (int i=0; i<mapItems.getCount(); i++) {
                String listName = mapItems.getString(mapItems.getColumnIndex("name"));
                String listOwner = "Owner: " + mapItems.getString(mapItems.getColumnIndex("owner"));
                String listCity = "City: " + mapItems.getString(mapItems.getColumnIndex("city"));
                String listDescription = mapItems.getString(mapItems.getColumnIndex("description"));
                HashMap<String, String> maps = new HashMap<>();
                maps.put("name", listName);
                maps.put("owner", listOwner);
                maps.put("city", listCity);
                maps.put("description", listDescription);
                mapList.add(maps);
                mapItems.moveToNext();
            }
            ListAdapter adapter = new SimpleAdapter(EditActivity.this,
                    mapList,
                    R.layout.map_listview_items,
                    new String[] {"name", "city", "owner", "description"},
                    new int[] {R.id.list_item_map_name, R.id.list_item_map_city, R.id.list_item_map_owner, R.id.list_item_map_description});
            listView.setAdapter(adapter);
        } else {
            listView.setAdapter(null);
            Toast.makeText(this, "No map data found", Toast.LENGTH_SHORT).show();
        }
    }

    private void searchMapByType(String searchContent, String searchTypeContent) {
        db = new Database(this);
        Cursor mapItems = db.searchMapByType(searchContent, searchTypeContent);
        mapList.clear();
        if (mapItems.getCount()>0) {
            mapItems.moveToFirst();
            for (int i=0; i<mapItems.getCount(); i++) {
                String listName = mapItems.getString(mapItems.getColumnIndex("name"));
                String listOwner = "Owner: " + mapItems.getString(mapItems.getColumnIndex("owner"));
                String listCity = "City: " + mapItems.getString(mapItems.getColumnIndex("city"));
                String listDescription = mapItems.getString(mapItems.getColumnIndex("description"));
                HashMap<String, String> maps = new HashMap<>();
                maps.put("name", listName);
                maps.put("owner", listOwner);
                maps.put("city", listCity);
                maps.put("description", listDescription);
                mapList.add(maps);
                mapItems.moveToNext();
            }
            ListAdapter adapter = new SimpleAdapter(EditActivity.this,
                    mapList,
                    R.layout.map_listview_items,
                    new String[] {"name", "city", "owner", "description"},
                    new int[] {R.id.list_item_map_name, R.id.list_item_map_city, R.id.list_item_map_owner, R.id.list_item_map_description});
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
