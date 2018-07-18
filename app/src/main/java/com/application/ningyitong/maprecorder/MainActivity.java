package com.application.ningyitong.maprecorder;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.support.v4.view.ViewPager;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends Activity {
    Database db;
    private ArrayList<HashMap<String, String>> mapList;
    private ListView listView;

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
        setContentView(R.layout.activity_main);

        // Check and set permission
        permissionState();

        // Setup bottom nav-bar
        setupBottomNavbar();

        // Create list view
        listView = (ListView)findViewById(R.id.mapDataList);
        mapList = new ArrayList<>();

        // Load local map list
        loadMapList();
    }

    private void loadMapList() {
        //TODO
//        db = new Database(this);
//        Cursor mapItems = db.getTableItems();
//        mapList.clear();
//        if (mapItems.getCount()>0) {
//            mapItems.moveToFirst();
//            for (int i=0; i<mapItems.getCount(); i++) {
//                String listName = mapItems.getString(mapItems.getColumnIndex("name"));
//                String listOwner = mapItems.getString(mapItems.getColumnIndex("owner"));
//                String listDescription = mapItems.getString(mapItems.getColumnIndex("description"));
//                HashMap<String, String> maps = new HashMap<>();
//                maps.put("name", listName);
//                maps.put("owner", listOwner);
//                maps.put("description", listDescription);
//                mapList.add(maps);
//                mapItems.moveToNext();
//            }
//            ListAdapter adapter = new SimpleAdapter(MainActivity.this,
//                    mapList,
//                    R.layout.map_listview_items,
//                    new String[] {"name", "description"},
//                    new int[] {R.id.list_item_map_name, R.id.list_item_map_description});
//            listView.setAdapter(adapter);
//        } else {
//            listView.setAdapter(null);
//            Toast.makeText(this, "No Map Data", Toast.LENGTH_SHORT).show();
//        }
    }

    private void searchMap(String name) {
        // TODO
    }

    private void setupBottomNavbar() {
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        Menu menu = navigation.getMenu();
        MenuItem menuItem = menu.getItem(0);
        menuItem.setChecked(true);
    }

    private void permissionState() {
        // Check SDK version
        if (Build.VERSION.SDK_INT < 23)
            return;

        // Check device permissions
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION }, 0);
        }
    }

    // Permission
    //@Override
    public void onRequetPermissionsResult (int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED
                || grantResults[1] == PackageManager.PERMISSION_GRANTED
                || grantResults[2] == PackageManager.PERMISSION_GRANTED) {

        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
                        || shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        || shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage("Enable to continue the app.").setTitle("Necessary permission required");
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                requestPermissions(new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION}, 0);
                            }
                        }
                    });
                    requestPermissions(new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION}, 0);
                } else {
                    Toast.makeText(this, "Some functons of the application are disabled", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_main:
                    break;
                case R.id.navigation_map:
                    Intent intent_map = new Intent(MainActivity.this, MapActivity.class);
                    startActivity(intent_map);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    break;
                case R.id.navigation_edit:
                    Intent intent_edit = new Intent(MainActivity.this, EditActivity.class);
                    startActivity(intent_edit);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    break;
                case R.id.navigation_cloud:
                    Intent intent_cloud = new Intent(MainActivity.this, CloudActivity.class);
                    startActivity(intent_cloud);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    break;
                case R.id.navigation_account:
                    Intent intent_account = new Intent(MainActivity.this, AccountActivity.class);
                    startActivity(intent_account);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    break;
            }
            return false;
        }
    };



}
