package com.application.ningyitong.maprecorder;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Environment;
import android.os.NetworkOnMainThreadException;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.BoringLayout;
import android.view.Menu;
import android.view.MenuItem;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.kml.KmlDocument;
import org.osmdroid.bonuspack.kml.KmlFeature;
import org.osmdroid.bonuspack.kml.KmlPlacemark;
import org.osmdroid.bonuspack.kml.KmlTrack;
import org.osmdroid.bonuspack.kml.LineStyle;
import org.osmdroid.bonuspack.kml.Style;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.DelayedMapListener;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.MapTileProviderBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.NetworkLocationIgnorer;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;
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
import org.osmdroid.views.overlay.mylocation.DirectedLocationOverlay;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class MapActivity extends AppCompatActivity implements MapEventsReceiver, LocationListener {
    Database db;
    private MapView map_view;
    private IMapController mapController;
    private CircleMenu circleMenu;
    private ImageButton recordingGpsBtn;
    public Boolean isRecording = false;
    private Dialog saveMapDialog;
    float mAzimuthAngleSpeed = 0.0f;
    SharedPreferences sharedPreferences;
    public Boolean isDrawingOverlay = false;
    KmlDocument kmlDocument;
    FolderOverlay mKmlOverlay;
    // Location API
    GeoPoint startPoint, destinationPoint;
    LocationManager locationManager;

//    OsmLocationUpdateHelper locationUpdateHelper;
    DirectedLocationOverlay directedLocationOverlay;
    // Location update interval
    private static final long UPDATE_INTERVAL = 10000;

    // Define final object name
    private final String OBJECT_BUILDING = "Building";
    private final String OBJECT_HOSPITAL = "Hospital";
    private final String OBJECT_ATM = "ATM";
    private final String OBJECT_TRAFFIC_LIGHT = "Traffic Light";
    private final String OBJECT_LINE = "Line";
    private final String OBJECT_UNDO = "Undo";

    String object = "";

    // user session
    UserSessionManager session;
    int userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().setOsmdroidBasePath(new File(Environment.getExternalStorageDirectory(), "osmdroid"));
        Configuration.getInstance().setOsmdroidTileCache(new File(Environment.getExternalStorageDirectory(), "osmdroid/tiles"));

        // Set map shared preferences
        sharedPreferences = getSharedPreferences("MAPRECORDER", MODE_PRIVATE);

        session = new UserSessionManager(getApplicationContext());
        // Check user login status
        if (session.checkLogin()) {
            finish();
        }
        setContentView(R.layout.activity_map);

        // Get session userID
        HashMap<String, Integer> user = session.getUserDetails();
        userID = user.get(UserSessionManager.KEY_USERID);
        //important! set your user agent to prevent getting banned from the osm servers
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        // Setup bottom nav-bar
        setupBottomNavbar();
        // Initial OSM
        setupMapView(savedInstanceState);

        // Set recording button
        setupRecordingBtn();

        // Setup save map
        saveMapDialog = new Dialog(this);
        // Setup map basic control button
        setupMapControlBtn();
        // Setup circle menu
        setupCircleMenu();

        // KML
        kmlDocument = new KmlDocument();

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
        kmlDocument.mKmlRoot.addOverlay(marker, kmlDocument);
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
                    recordingGpsBtn.setKeepScreenOn(false);
                    Toast.makeText(getBaseContext(), "Recording stopped", Toast.LENGTH_SHORT).show();

                } else {
                    isRecording = true;
                    recordingGpsBtn.setImageResource(R.drawable.ic_recording_24dp);
                    recordingGpsBtn.setKeepScreenOn(true);
                    Toast.makeText(getBaseContext(), "Recording started", Toast.LENGTH_SHORT).show();

                    // Create start marker
                    //addStartMarker(startPoint);
                    if (directedLocationOverlay.isEnabled() && directedLocationOverlay.getLocation() != null)
                        map_view.getController().animateTo(directedLocationOverlay.getLocation());
                }
            }
        });
    }
    /** Create start marker **/
    private void addStartMarker(GeoPoint startPoint) {
        Marker startMarker = new Marker(map_view);
        startMarker.setPosition(startPoint);
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map_view.getOverlays().add(startMarker);
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
        if (isRecording) {
            Toast.makeText(getBaseContext(), "Please stop recording GPS first.", Toast.LENGTH_SHORT).show();
            return;
        }

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
                String tracking = name + "_" + owner + "_" + date + ".kml";

                if (name.equals("")) {
                    mapName.setError("Input map name");
                    return;
                }
//                if (owner.equals("")) {
//                    mapOwner.setError("Input map owner");
//                    return;
//                }
//                if (date.equals("")) {
//                    mapDate.setError("Input map record date");
//                    return;
//                }

                if (db.checkMap(name)) {
                    Boolean insert = db.saveMap(name, city, description, owner, date, tracking, userID);
                    if (insert) {
                        // Save map overlay
                        saveKmlFile(tracking);
                        Toast.makeText(getBaseContext(), "Save map info successfully", Toast.LENGTH_SHORT).show();
                        saveMapDialog.dismiss();
                        // Saved successfully, jump to map list activity
                        Intent intent_edit = new Intent(MapActivity.this, EditActivity.class);
                        startActivity(intent_edit);
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
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
    /** Save KML file **/
    private void saveKmlFile(String fileName) {
        boolean saved;
        File file = kmlDocument.getDefaultPathForAndroid(fileName);
        saved = kmlDocument.saveAsKML(file);
        if (saved)
            Toast.makeText(this, fileName + " saved", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(this, "Unable to save "+fileName, Toast.LENGTH_SHORT).show();
    }

    /** Create map controller buttons
     * Create zoom in button
     * Create zoom out button
     * Create set map center button **/
    private void setupMapControlBtn() {
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

        final ImageButton btnLocation = findViewById(R.id.location_track);
        btnLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnLocation.setFocusable(true);
                if (directedLocationOverlay.isEnabled()&& directedLocationOverlay.getLocation() != null){
                    map_view.getController().animateTo(directedLocationOverlay.getLocation());
                }
                map_view.setMapOrientation(-mAzimuthAngleSpeed);
                btnLocation.clearFocus();
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
    private void setupMapView(Bundle savedInstanceState) {
        map_view = findViewById(R.id.mapview);
        map_view.setTileSource(TileSourceFactory.MAPNIK);
        map_view.setClickable(true);        // Enable map clickable
        map_view.setBuiltInZoomControls(false);        // Disable builtin zoom controller
        map_view.setMultiTouchControls(true);        // Enable touch control
        map_view.setMinZoomLevel((double) 3);        // Set zoom minimise limitation
        map_view.setMaxZoomLevel((double) 22);        // Set zoom maximise limitation
        mapController = map_view.getController();
        mapController.setZoom((double) 18);        // Set map default zoom level
        // Create Compass overlay
        CompassOverlay compassOverlay = new CompassOverlay(this, new InternalCompassOrientationProvider(this), map_view);
        compassOverlay.enableCompass();
        compassOverlay.setCompassCenter(30, 55);
        map_view.getOverlays().add(compassOverlay);
        // Create Scale Bar overlay
        ScaleBarOverlay scaleBarOverlay = new ScaleBarOverlay(map_view);
        scaleBarOverlay.setCentred(true);
        scaleBarOverlay.setScaleBarOffset(this.getResources().getDisplayMetrics().widthPixels / 2, 10);
        map_view.getOverlays().add(scaleBarOverlay);
        // Initial MapEventsReceiver
        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(this);
        map_view.getOverlays().add(mapEventsOverlay);
        // Create Location Manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mapController.setCenter(new GeoPoint((double) sharedPreferences.getFloat("CENTER_LAT", 53.384f), (double)sharedPreferences.getFloat("CENTER_LON", -1.491f)));
        // Create map location overlay
        directedLocationOverlay = new DirectedLocationOverlay(this);
        map_view.getOverlays().add(directedLocationOverlay);

        if (savedInstanceState == null){
            Location location = null;
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location == null)
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            if (location != null) {
                //location known:
                onLocationChanged(location);
            } else {
                directedLocationOverlay.setEnabled(false);
            }
            startPoint = null;
            destinationPoint = null;

            if (directedLocationOverlay.isEnabled()&& directedLocationOverlay.getLocation() != null){
                mapController.animateTo(directedLocationOverlay.getLocation());
            }
        } else {
            directedLocationOverlay.setLocation((GeoPoint)savedInstanceState.getParcelable("location"));
            //TODO: restore other aspects of myLocationOverlay...
            startPoint = savedInstanceState.getParcelable("start");
            destinationPoint = savedInstanceState.getParcelable("destination");
        }
    }

    /** Save map preferences **/
    private void savePreferences() {
        SharedPreferences prefs = getSharedPreferences("MAPRECORDER", MODE_PRIVATE);
        SharedPreferences.Editor ed = prefs.edit();
        ed.putFloat("ZOOM_LEVEL", (float)map_view.getZoomLevelDouble());
        GeoPoint center = (GeoPoint) map_view.getMapCenter();
        ed.putFloat("CENTER_LAT", (float)center.getLatitude());
        ed.putFloat("CENTER_LON", (float)center.getLongitude());
        MapTileProviderBase tileProvider = map_view.getTileProvider();
        String tileProviderName = tileProvider.getTileSource().name();
        ed.putString("TILE_PROVIDER", tileProviderName);
        ed.apply();
    }
    /** Bottom navigation bar function **/
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


    /** Location Listener **/
    private final NetworkLocationIgnorer networkLocationIgnorer = new NetworkLocationIgnorer();
    long lastTime;
    double speed;
    @Override
    public void onLocationChanged(final Location location) {
        long currentTime = System.currentTimeMillis();
        if (networkLocationIgnorer.shouldIgnore(location.getProvider(), currentTime))
            return;
        double dT = currentTime - lastTime;
        if (dT < 100.0){
            //Toast.makeText(this, pLoc.getProvider()+" dT="+dT, Toast.LENGTH_SHORT).show();
            return;
        }
        lastTime = currentTime;

        GeoPoint newLocation = new GeoPoint(location);
        if (!directedLocationOverlay.isEnabled()){
            //we get the location for the first time:
            directedLocationOverlay.setEnabled(true);
            map_view.getController().animateTo(newLocation);
        }

        GeoPoint prevLocation = directedLocationOverlay.getLocation();
        directedLocationOverlay.setLocation(newLocation);
        directedLocationOverlay.setAccuracy((int)location.getAccuracy());

        if (prevLocation != null && location.getProvider().equals(LocationManager.GPS_PROVIDER)){
            speed = location.getSpeed() * 3.6;
            long speedInt = Math.round(speed);
            TextView speedTxt = (TextView)findViewById(R.id.speed);
            speedTxt.setText(speedInt + " km/h");

            //TODO: check if speed is not too small
            if (speed >= 0.1){
                mAzimuthAngleSpeed = location.getBearing();
                directedLocationOverlay.setBearing(mAzimuthAngleSpeed);
            }
        }

        if (isRecording){
            //keep the map view centered on current location:
            map_view.getController().animateTo(newLocation);
            map_view.setMapOrientation(-mAzimuthAngleSpeed);
            recordCurrentLocationInTrack("my_track", "My Track", newLocation);

        } else {
            //just redraw the location overlay:
            map_view.invalidate();
        }
    }

    static int[] TrackColor = {
            Color.CYAN-0x20000000, Color.BLUE-0x20000000, Color.MAGENTA-0x20000000, Color.RED-0x20000000, Color.YELLOW-0x20000000
    };
    KmlTrack createTrack(String id, String name) {
        KmlTrack t = new KmlTrack();
        KmlPlacemark p = new KmlPlacemark();
        p.mId = id;
        p.mName = name;
        p.mGeometry = t;
        kmlDocument.mKmlRoot.add(p);
        //set a color to this track by creating a style:
        Style s = new Style();
        int color;
        try {
            color = Integer.parseInt(id);
            color = color % TrackColor.length;
            color = TrackColor[color];
        } catch (NumberFormatException e) {
            color = Color.GREEN-0x20000000;
        }
        s.mLineStyle = new LineStyle(color, 8.0f);
        String styleId = kmlDocument.addStyle(s);
        p.mStyle = styleId;
        return t;
    }
    Style buildDefaultStyle(){
        Drawable defaultKmlMarker = ResourcesCompat.getDrawable(getResources(), R.drawable.marker_default, null);
        Bitmap bitmap = ((BitmapDrawable)defaultKmlMarker).getBitmap();
        return new Style(bitmap, 0x901010AA, 3.0f, 0x20AA1010);
    }
    void updateUIWithKml(){
        if (mKmlOverlay != null){
            mKmlOverlay.closeAllInfoWindows();
            map_view.getOverlays().remove(mKmlOverlay);
        }
        mKmlOverlay = (FolderOverlay)kmlDocument.mKmlRoot.buildOverlay(map_view, buildDefaultStyle(), null, kmlDocument);
        map_view.getOverlays().add(mKmlOverlay);
        map_view.invalidate();
    }

    void recordCurrentLocationInTrack(String trackId, String trackName, GeoPoint currentLocation) {
        //Find the KML track in the current KML structure - and create it if necessary:
        KmlTrack t;
        KmlFeature f = kmlDocument.mKmlRoot.findFeatureId(trackId, false);
        if (f == null)
            t = createTrack(trackId, trackName);
        else if (!(f instanceof KmlPlacemark))
            //id already defined but is not a PlaceMark
            return;
        else {
            KmlPlacemark p = (KmlPlacemark)f;
            if (!(p.mGeometry instanceof KmlTrack))
                //id already defined but is not a Track
                return;
            else
                t = (KmlTrack) p.mGeometry;
        }
        //TODO check if current location is really different from last point of the track
        //record in the track the current location at current time:
        t.add(currentLocation, new Date());
        //refresh KML:
        updateUIWithKml();
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    @Override
    public boolean singleTapConfirmedHelper(GeoPoint p) {
        Polygon circle = new Polygon(map_view);
        circle.setPoints(Polygon.pointsAsCircle(p, 2000.0));
        circle.setFillColor(0x12121212);
        circle.setStrokeColor(Color.RED);
        circle.setStrokeWidth(2);
        map_view.getOverlays().add(circle);
        map_view.invalidate();
        kmlDocument.mKmlRoot.addOverlay(circle, kmlDocument);

        if (!object.equals("")) {
            Toast.makeText(getBaseContext(), "Put " + object + " " + p.getLatitude() + "-" + p.getLongitude(), Toast.LENGTH_LONG).show();
            drawMarker(p, object);
        }
        return false;
    }

    @Override
    public boolean longPressHelper(GeoPoint p) {
        return false;
    }
}
