package com.application.ningyitong.maprecorder;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.BoringLayout;
import android.view.Menu;
import android.view.MenuItem;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.DelayedMapListener;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.TilesOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.hitomi.cmlibrary.CircleMenu;
import com.hitomi.cmlibrary.OnMenuSelectedListener;
import com.hitomi.cmlibrary.OnMenuStatusChangeListener;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

public class MapActivity extends AppCompatActivity {
    final double DEFAULT_LATITUDE = 44.445883;
    final double DEFAULT_LONGITUDE = 26.040963;

    Database db;
    private MapView map_view;
    private MapController mapController;
    private CircleMenu circleMenu;
    private ImageButton recordingGpsBtn;
    public Boolean isRecording = false;
    private Dialog saveMapDialog;

    public Boolean isDrawingOverlay = false;

    // Location API
    LocationManager locationManager;
    private OverlayItem lastPosition = null;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private SettingsClient settingsClient;
    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;
    private LocationCallback locationCallback;
    private GeoPoint currentLocation;
    OsmLocationUpdateHelper locationUpdateHelper;
    private ArrayList<OverlayItem> locationItems = new ArrayList<OverlayItem>();
    MapEventsReceiver mapEventsReceiver;

    // Location update interval
    private static final long UPDATE_INTERVAL = 10000;

    // Define final object name
    private final String OBJECT_BUILDING = "Building";
    private final String OBJECT_HOSPITAL = "Hospital";
    private final String OBJECT_ATM = "ATM";
    private final String OBJECT_TRAFFIC_LIGHT = "Traffic Light";
    private final String OBJECT_LINE = "Line";
    private final String OBJECT_UNDO = "Undo";

    ArrayList<OverlayItem> mItems;
    String object = "";

    // user session
    UserSessionManager session;
    int userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        session = new UserSessionManager(getApplicationContext());
        // Check user login status
        if (session.checkLogin()) {
            finish();
        }
        setContentView(R.layout.activity_map);

        // Get session userID
        HashMap<String, Integer> user = session.getUserDetails();
        userID = user.get(UserSessionManager.KEY_USERID);

        mItems = new ArrayList<OverlayItem>();

        //important! set your user agent to prevent getting banned from the osm servers
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        // Setup bottom nav-bar
        setupBottomNavbar();
        // Initial OSM
        setupMapView();

        MyLocationNewOverlay myLocationoverlay = new MyLocationNewOverlay(map_view);
        myLocationoverlay.enableFollowLocation();
        myLocationoverlay.enableMyLocation();
        map_view.getOverlays().add(myLocationoverlay);
        currentLocation = myLocationoverlay.getMyLocation();

        // Set recording button
        setupRecordingBtn();

        // Setup save map
        saveMapDialog = new Dialog(this);
        // Setup map basic control button
        setupMapControlBtn();
        // Setup circle menu
        setupCircleMenu();

        mapEventsReceiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
//                Toast.makeText(getBaseContext(), "Put " + object + " " + p.getLatitude() + "-" + p.getLongitude(), Toast.LENGTH_LONG).show();

//                    Polygon circle = new Polygon(map_view);
//                    circle.setPoints(Polygon.pointsAsCircle(p, 2000.0));
//
//                    circle.setFillColor(0x12121212);
//                    circle.setStrokeColor(Color.RED);
//                    circle.setStrokeWidth(2);
                if (!object.equals("") || !object.equals(null))
                    drawMarker(p, object);

                return false;
            }
            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        };
        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(getBaseContext(), mapEventsReceiver);
        map_view.getOverlays().add(mapEventsOverlay);
    }

    /** Draw Marker **/
    private void drawMarker(GeoPoint p, String object) {
        if (object.equals(""))
            return;

//        if (object.equals(OBJECT_UNDO)) {
//            Toast.makeText(getBaseContext(), "Undo add Marker", Toast.LENGTH_LONG).show();
//            map_view.getOverlayManager().remove(marker);
//        }
        Marker marker = new Marker(map_view);
        marker.setPosition(p);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map_view.getOverlays().add(marker);
        switch (object) {
            case OBJECT_BUILDING:
                marker.setIcon(getResources().getDrawable(R.drawable.ic_location_city_black_24dp));
                break;
            case OBJECT_ATM:
                marker.setIcon(getResources().getDrawable(R.drawable.ic_local_atm_black_24dp));
                break;
            case OBJECT_HOSPITAL:
                marker.setIcon(getResources().getDrawable(R.drawable.ic_local_hospital_black_24dp));
                break;
            case OBJECT_LINE:
                marker.setIcon(getResources().getDrawable(R.drawable.ic_local_atm_black_24dp));
                break;
            case OBJECT_TRAFFIC_LIGHT:
                marker.setIcon(getResources().getDrawable(R.drawable.ic_traffic_black_24dp));
                break;
            default:
                break;
        }
        marker.setTitle(object);
        map_view.getOverlays().add(marker);

        map_view.invalidate();
    }

    /** Setup recording button **/
    private void setupRecordingBtn() {
        recordingGpsBtn = findViewById(R.id.recording_gps_btn);
        recordingGpsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isRecording) {
                    isRecording = false;
                    recordingGpsBtn.setImageResource(R.drawable.ic_ready_record_24dp);
                    Toast.makeText(getBaseContext(), "Recording stopped", Toast.LENGTH_SHORT).show();
                    // TODO
                } else {
                    isRecording = true;
                    recordingGpsBtn.setImageResource(R.drawable.ic_recording_24dp);
                    Toast.makeText(getBaseContext(), "Recording started", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /** Setup circle menu **/
    private void setupCircleMenu() {
        circleMenu = findViewById(R.id.edit_map_circle_menu);
        circleMenu.setMainMenu(Color.parseColor("#ffffff"), R.drawable.ic_edit_menu_24dp, R.mipmap.icon_cancel);
        circleMenu.addSubMenu(Color.parseColor("#258CFF"), R.drawable.ic_building_white_24dp)
                .addSubMenu(Color.parseColor("#30A400"), R.drawable.ic_traffic_white_48dp)
                .addSubMenu(Color.parseColor("#FF4B32"), R.drawable.ic_line_white_24dp)
                .addSubMenu(Color.parseColor("#8A39FF"), R.drawable.ic_atm_white_24dp)
                .addSubMenu(Color.parseColor("#FF6A00"), R.drawable.ic_hospital_white_24dp)
                .addSubMenu(Color.parseColor("#FF0000"), R.drawable.ic_undo_black_24dp);

        circleMenu.setOnMenuSelectedListener(new OnMenuSelectedListener() {
            @Override
            public void onMenuSelected(int i) {
                switch (i) {
                    case 0:
                        isDrawingOverlay = true;
                        object = OBJECT_BUILDING;
                        break;
                    case 1:
                        isDrawingOverlay = true;
                        object = OBJECT_TRAFFIC_LIGHT;
                        break;
                    case 2:
                        isDrawingOverlay = true;
                        object = OBJECT_LINE;
                        break;
                    case 3:
                        isDrawingOverlay = true;
                        object = OBJECT_ATM;
                        break;
                    case 4:
                        isDrawingOverlay = true;
                        object = OBJECT_HOSPITAL;
                        break;
                    case 5:
                        isDrawingOverlay = true;
                        object = OBJECT_UNDO;
                        break;
                }
            }
        });

        circleMenu.setOnMenuStatusChangeListener(new OnMenuStatusChangeListener() {
            @Override
            public void onMenuOpened() {
                Toast.makeText(getBaseContext(), "Choose an object", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onMenuClosed() {
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (circleMenu.isOpened()) {
            circleMenu.closeMenu();
        } else {
            finish();
        }
    }

    /** Show save map dialog **/
    public void ShowSaveMapDialog(View view) {
        ImageButton closeDialog;
        Button cancelSaveMapBtn, confirmSaveMapBtn;
        final EditText mapName, mapCity, mapOwner, mapDescription, mapDate;
        saveMapDialog.setContentView(R.layout.map_save_dialog);

        // Dismiss save map dialog
        closeDialog = saveMapDialog.findViewById(R.id.map_save_close);
        closeDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveMapDialog.dismiss();
            }
        });
        cancelSaveMapBtn = saveMapDialog.findViewById(R.id.map_save_cancel);
        cancelSaveMapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveMapDialog.dismiss();
            }
        });

        // Map info
        db = new Database(this);
        mapName = saveMapDialog.findViewById(R.id.save_map_title);
        mapCity = saveMapDialog.findViewById(R.id.save_map_city);
        mapOwner = saveMapDialog.findViewById(R.id.save_map_owner);
        mapDescription = saveMapDialog.findViewById(R.id.save_map_description);
        mapDate = saveMapDialog.findViewById(R.id.save_map_date);

        confirmSaveMapBtn = saveMapDialog.findViewById(R.id.map_save_confirm);
        confirmSaveMapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = mapName.getText().toString();
                String city = mapCity.getText().toString();
                String owner = mapOwner.getText().toString();
                String description = mapDescription.getText().toString();
                String date = mapDate.getText().toString();
                String tracking = "gps file path";

                // Save geo points
                FileOutputStream fileOutputStream = null;
                try {
                    fileOutputStream = openFileOutput(name, MODE_PRIVATE);
                    fileOutputStream.write("dddd".getBytes());

                    Toast.makeText(getBaseContext(), "Save path: " + getFilesDir() + "/" + name, Toast.LENGTH_LONG).show();
                    tracking = getFilesDir() + "/" + name;
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (fileOutputStream != null) {
                        try {
                            fileOutputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                if (name.equals("")) {
                    mapName.setError("Input map name");
                    return;
                }
                if (db.checkMap(name)) {
                    Boolean insert = db.saveMap(name, city, description, owner, date, tracking, userID);
                    if (insert) {
                        Toast.makeText(getBaseContext(), "Save map info successfully", Toast.LENGTH_SHORT).show();
                        saveMapDialog.dismiss();
                    } else {
                        Toast.makeText(getBaseContext(), "Failed to save map data", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getBaseContext(), "Map name exists, please change to another one.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        saveMapDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        saveMapDialog.show();
    }

    private void setupMapControlBtn() {
        // Zoom button
        ImageButton btnZoomIn = findViewById(R.id.zoom_in);
        btnZoomIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapController.zoomIn();
            }
        });

        ImageButton btnZoomOut = findViewById(R.id.zoom_out);
        btnZoomOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapController.zoomOut();
            }
        });

        ImageButton btnLocation = findViewById(R.id.location_track);
        btnLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDrawingOverlay = false;
                Toast.makeText(getBaseContext(), isDrawingOverlay.toString(), Toast.LENGTH_LONG).show();
//                if (currentLocation != null) {
//                    mapController.setCenter(new GeoPoint(currentLocation));
//                } else {
//                    Toast.makeText(getBaseContext(), "Cannot access your current location", Toast.LENGTH_LONG).show();
//                }
            }
        });
    }

    private void setupBottomNavbar() {
        // Bottom nav-bar
        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        Menu menu = navigation.getMenu();
        MenuItem menuItem = menu.getItem(1);
        menuItem.setChecked(true);
    }

    /** Init map view **/
    private void setupMapView() {
        map_view = findViewById(R.id.mapview);
        map_view.setTileSource(TileSourceFactory.MAPNIK);
        // Enable map clickable
        map_view.setClickable(true);
        // Disable builtin zoom controller
        map_view.setBuiltInZoomControls(false);
        // Enable touch control
        map_view.setMultiTouchControls(true);
        // Set zoom limitation
        map_view.setMinZoomLevel((double) 3);
        map_view.setMaxZoomLevel((double) 22);
        // Set map default zoom level
        mapController = (MapController) map_view.getController();
        mapController.setZoom(18);
        // Compass
        CompassOverlay compassOverlay = new CompassOverlay(this, new InternalCompassOrientationProvider(this), map_view);
        compassOverlay.enableCompass();
        compassOverlay.setCompassCenter(30, 55);
        map_view.getOverlays().add(compassOverlay);
        // Scale Bar
        ScaleBarOverlay scaleBarOverlay = new ScaleBarOverlay(map_view);
        scaleBarOverlay.setCentred(true);
        scaleBarOverlay.setScaleBarOffset(this.getResources().getDisplayMetrics().widthPixels / 2, 10);
        map_view.getOverlays().add(scaleBarOverlay);
        // Location
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationUpdateHelper = new OsmLocationUpdateHelper(this);
        Location location = null;

        for (String provider : locationManager.getProviders(true)) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            location = locationManager.getLastKnownLocation(provider);
            if (location != null) {
                locationManager.requestLocationUpdates(provider, 0, 0,locationUpdateHelper);
                break;
            }
        }

        if (location == null) {
            location = new Location(LocationManager.GPS_PROVIDER);
            location.setLatitude(DEFAULT_LATITUDE);
            location.setLongitude(DEFAULT_LONGITUDE);
            updateCurrentLocation(new GeoPoint(location));
            // Overlay
//            MyLocationNewOverlay locationNewOverlay;
//            Drawable drawable = this.getResources().getDrawable(R.drawable.marker_default);
//            ArrayList<OverlayItem> items = new ArrayList<OverlayItem>();
//            OverlayItem item = new OverlayItem("Hospital", "I am a hospital haha", (IGeoPoint) location);
//            item.setMarker(drawable);
//            items.add(item);

        }

    }

    public void updateCurrentLocation(GeoPoint geoPoint) {
        Toast.makeText(getBaseContext(), "Latituddddddddddddddde = " + geoPoint.getLatitude() * 1e6 + " Longitude = " + geoPoint.getLongitude() * 1e6, Toast.LENGTH_SHORT).show();
    }

    // Bottom navigation bar function
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_main:
                    Intent intent_main = new Intent(MapActivity.this, MainActivity.class);
                    startActivity(intent_main);
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    break;
                case R.id.navigation_map:
                    break;
                case R.id.navigation_edit:
                    Intent intent_edit = new Intent(MapActivity.this, EditActivity.class);
                    startActivity(intent_edit);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    break;
                case R.id.navigation_cloud:
                    Intent intent_cloud = new Intent(MapActivity.this, CloudActivity.class);
                    startActivity(intent_cloud);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    break;
                case R.id.navigation_account:
                    Intent intent_account = new Intent(MapActivity.this, AccountActivity.class);
                    startActivity(intent_account);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    break;
            }
            return false;
        }
    };
}
