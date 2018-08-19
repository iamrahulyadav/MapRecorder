package com.application.ningyitong.maprecorder;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.kml.KmlDocument;
import org.osmdroid.bonuspack.kml.KmlFeature;
import org.osmdroid.bonuspack.kml.KmlPlacemark;
import org.osmdroid.bonuspack.kml.KmlTrack;
import org.osmdroid.bonuspack.kml.LineStyle;
import org.osmdroid.bonuspack.kml.Style;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.MapTileProviderBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.NetworkLocationIgnorer;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.PathOverlay;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.infowindow.InfoWindow;
import org.osmdroid.views.overlay.mylocation.DirectedLocationOverlay;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.hitomi.cmlibrary.CircleMenu;
import com.hitomi.cmlibrary.OnMenuSelectedListener;
import com.hitomi.cmlibrary.OnMenuStatusChangeListener;

import java.io.File;
import java.security.Policy;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class MapActivity extends AppCompatActivity implements MapEventsReceiver, LocationListener {
    Database db;
    protected ArrayList<GeoPoint> objectMarkers;
    protected InfoWindow objectMarkerInfoWindow;
    protected FolderOverlay folderOverlay;
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
    private Polyline routeLine;
    private ArrayList<Location> routeLineLocation;
//    FolderOverlay mKmlOverlay;
    // Location API
    GeoPoint startPoint, destinationPoint;
    LocationManager locationManager;
    protected Polyline[] routeOverlay;
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
    private final String OBJECT_HOTEL = "Hotel";
    private final String OBJECT_SHOP = "Shop";
    private final String OBJECT_HOUSE = "House";

    private Boolean isDrawPolyline = false;
    String object = "";
private Polyline polyline;
    TextView drawPolylineText;
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

        // Set folder overlay: markers
        folderOverlay = new FolderOverlay();
        folderOverlay.setName("Map Markers");
        map_view.getOverlays().add(folderOverlay);
        updateUIWithObjectMarkers();

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
        MyLocationNewOverlay myLocationNewOverlay = new MyLocationNewOverlay(map_view);

        drawPolylineText = findViewById(R.id.draw_polyline_status);
    }

    public void updateUIWithObjectMarkers() {
        folderOverlay.closeAllInfoWindows();
        folderOverlay.getItems().clear();
        for (int i=0; i<objectMarkers.size(); i++) {
            drawMarker(objectMarkers.get(i), object, -1);
        }
    }

    class OnObjectMarkerDragListener implements Marker.OnMarkerDragListener {
        @Override public void onMarkerDrag(Marker marker) {}
        @Override public void onMarkerDragEnd(Marker marker) {
            int index = (Integer)marker.getRelatedObject();
            objectMarkers.set(index, marker.getPosition());
            marker.setSnippet(marker.getPosition().getLatitude() + " " + marker.getPosition().getLongitude());
        }

        @Override
        public void onMarkerDragStart(Marker marker) {
        }
    }
    final OnObjectMarkerDragListener onObjectMarkerDragListener = new OnObjectMarkerDragListener();

    public void deletePoint(int selectMarker) {
//        folderOverlay.remove(objectMarkers[selectMarker]);
        objectMarkers.remove(selectMarker);
//        kmlDocument.mKmlRoot.removeItem(selectMarker);//TODO unreliable
        updateUIWithObjectMarkers();
    }

    /** Draw Marker **/
    public void drawMarker(GeoPoint p, String object, int index) {

        Marker marker = new Marker(map_view);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        objectMarkerInfoWindow = new ObjectMarkerInfoWindow(R.layout.marker_bubble, map_view);
        marker.setInfoWindow(objectMarkerInfoWindow);
        marker.setDraggable(true);
        marker.setOnMarkerDragListener(onObjectMarkerDragListener);

        marker.setTitle(object);
        marker.setPosition(p);
        switch (object) {
            case OBJECT_BUILDING:
                marker.setIcon(getResources().getDrawable(R.drawable.ic_marker_building));
                break;
            case OBJECT_ATM:
                marker.setIcon(getResources().getDrawable(R.drawable.ic_marker_atm));
                break;
            case OBJECT_HOSPITAL:
                marker.setIcon(getResources().getDrawable(R.drawable.ic_marker_hospital));
                break;
            case OBJECT_TRAFFIC_LIGHT:
                marker.setIcon(getResources().getDrawable(R.drawable.ic_marker_traffic_light));
                break;
            case OBJECT_HOTEL:
                marker.setIcon(getResources().getDrawable(R.drawable.ic_marker_hotel));
                break;
            case OBJECT_HOUSE:
                marker.setIcon(getResources().getDrawable(R.drawable.ic_marker_home));
                break;
            case OBJECT_SHOP:
                marker.setIcon(getResources().getDrawable(R.drawable.ic_marker_shop));
                break;
            default:
                break;
        }
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setRelatedObject(index);
        marker.setSnippet(p.getLatitude()+ " " + p.getLongitude());
        folderOverlay.add(marker);

//        map_view.getOverlays().add(marker);
        map_view.invalidate();
//        kmlDocument.mKmlRoot.addOverlay(marker, kmlDocument);
    }
    /** Draw polyline **/
    public void drawPolyline() {
        isDrawPolyline = true;
        drawPolylineText.setText("Long press to exit drawing mode...");
        polyline = new Polyline();
        polyline.setColor(Color.RED);
        polyline.setWidth(15);
        map_view.getOverlays().add(polyline);
//        map_view.invalidate();
    }

    /** Setup recording button **/
    private void setupRecordingBtn() {
        recordingGpsBtn = findViewById(R.id.recording_gps_btn);
        recordingGpsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isRecording) {
                    isRecording = false;
                    folderOverlay.add(routeLine);
                    recordingGpsBtn.setImageResource(R.drawable.ic_ready_record_24dp);
                    recordingGpsBtn.setKeepScreenOn(false);
                    Toast.makeText(getBaseContext(), "Recording stopped", Toast.LENGTH_SHORT).show();
                    if (directedLocationOverlay.isEnabled() && directedLocationOverlay.getLocation() != null) {
                        map_view.getController().animateTo(directedLocationOverlay.getLocation());
                        // Create start marker
                        destinationPoint = directedLocationOverlay.getLocation();
                        addEndMarker(destinationPoint);
                    }
                } else {
                    isRecording = true;
                    recordingGpsBtn.setImageResource(R.drawable.ic_recording_24dp);
                    recordingGpsBtn.setKeepScreenOn(true);
                    Toast.makeText(getBaseContext(), "Recording started", Toast.LENGTH_SHORT).show();

                    if (directedLocationOverlay.isEnabled() && directedLocationOverlay.getLocation() != null) {
                        map_view.getController().animateTo(directedLocationOverlay.getLocation());
                        // Create start marker
                        startPoint = directedLocationOverlay.getLocation();
                        addStartMarker(startPoint);
                        routeLineLocation = new ArrayList<>();
                        routeLine = new Polyline();
                        routeLine.setWidth(15);
                        routeLine.addPoint(startPoint);
                        map_view.getOverlays().add(routeLine);
                    }
                }
            }
        });
    }
    /** Create start marker **/
    private void addStartMarker(GeoPoint startPoint) {
        Marker startMarker = new Marker(map_view);
        startMarker.setPosition(startPoint);
        startMarker.setTitle("Start Point");
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        startMarker.setDraggable(true);
//        startMarker.setOnMarkerDragListener(onObjectMarkerDragListener);
        startMarker.setSnippet(startPoint.getLatitude()+ " " + startPoint.getLongitude());
        map_view.getOverlays().add(startMarker);
//        kmlDocument.mKmlRoot.addOverlay(startMarker, kmlDocument);
        folderOverlay.add(startMarker);
    }
    /** Create end marker **/
    private void addEndMarker(GeoPoint endPoint) {
        Marker endMarker = new Marker(map_view);
        endMarker.setPosition(startPoint);
        endMarker.setTitle("Start Point");
        endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        endMarker.setDraggable(true);
//        endMarker.setOnMarkerDragListener(onObjectMarkerDragListener);
        endMarker.setSnippet(startPoint.getLatitude()+ " " + startPoint.getLongitude());
        map_view.getOverlays().add(endMarker);
//        kmlDocument.mKmlRoot.addOverlay(endMarker, kmlDocument);
        folderOverlay.add(endMarker);
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
                .addSubMenu(Color.parseColor("#FF4B32"), R.drawable.ic_hotel_white_24dp)
                .addSubMenu(Color.parseColor("#8A39FF"), R.drawable.ic_home_white_24dp)
                .addSubMenu(Color.parseColor("#FF6A00"), R.drawable.ic_shopping_cart_white_24dp);

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
                        drawPolyline();
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
                        object = OBJECT_HOTEL;
                        break;
                    case 6:
                        isDrawingOverlay = true;
                        object = OBJECT_HOUSE;
                        break;
                    case 7:
                        isDrawingOverlay = true;
                        object = OBJECT_SHOP;
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
        if (isDrawPolyline) {
            Toast.makeText(getBaseContext(), "Pleas stop draw line first (long press screen to exit draw line mode)", Toast.LENGTH_SHORT).show();
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
                        if (routeOverlay != null) {
                            for (int i=0; i<routeOverlay.length; i++)
                                kmlDocument.mKmlRoot.addOverlay(routeOverlay[i],kmlDocument);
                        }
                        kmlDocument.mKmlRoot.addOverlay(folderOverlay, kmlDocument);
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
            objectMarkers = new ArrayList<>();

            if (directedLocationOverlay.isEnabled()&& directedLocationOverlay.getLocation() != null){
                mapController.animateTo(directedLocationOverlay.getLocation());
            }
        } else {
            directedLocationOverlay.setLocation((GeoPoint)savedInstanceState.getParcelable("location"));
            //TODO: restore other aspects of myLocationOverlay...
            startPoint = savedInstanceState.getParcelable("start");
            destinationPoint = savedInstanceState.getParcelable("destination");
            objectMarkers = savedInstanceState.getParcelableArrayList("object_markers");
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
        Toast.makeText(getBaseContext(), "Latitude = " + location.getLatitude() * 1e6 + " Longitude = " + location.getLongitude() * 1e6, Toast.LENGTH_SHORT).show();
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
//        if (!directedLocationOverlay.isEnabled()){
//            //we get the location for the first time:
//            directedLocationOverlay.setEnabled(true);
//            map_view.getController().animateTo(newLocation);
//        }

        GeoPoint prevLocation = directedLocationOverlay.getLocation();
        directedLocationOverlay.setLocation(newLocation);
        directedLocationOverlay.setAccuracy((int)location.getAccuracy());

//        if (prevLocation != null && location.getProvider().equals(LocationManager.GPS_PROVIDER)){
            speed = location.getSpeed() * 3.6;
            long speedInt = Math.round(speed);
            TextView speedTxt = findViewById(R.id.speed);
            String speedString = String.format("Speed: %s km/h", speedInt);
            speedTxt.setText(speedString);

            //TODO: check if speed is not too small
            if (speed >= 0.1){
                mAzimuthAngleSpeed = location.getBearing();
                directedLocationOverlay.setBearing(mAzimuthAngleSpeed);
            }
//        }

        if (isRecording){
            if (speed >= 0.1) {
                //keep the map view centered on current location:
                map_view.getController().animateTo(newLocation);
                map_view.setMapOrientation(-mAzimuthAngleSpeed);
                recordCurrentLocationInTrack("my_track", "My Track", newLocation);
                routeLine.addPoint(newLocation);
                routeLineLocation.add(location);
            }
        }
        map_view.invalidate();
    }

    static int[] TrackColor = {
            Color.CYAN-0x20000000, Color.BLUE-0x20000000, Color.MAGENTA-0x20000000, Color.RED-0x20000000, Color.YELLOW-0x20000000
    };
    KmlTrack createTrack(String id, String name) {
        KmlTrack kmlTrack = new KmlTrack();
        KmlPlacemark kmlPlacemark = new KmlPlacemark();
        kmlPlacemark.mId = id;
        kmlPlacemark.mName = name;
        kmlPlacemark.mGeometry = kmlTrack;
        kmlDocument.mKmlRoot.add(kmlPlacemark);
        //set a color to this track by creating a style:
        Style style = new Style();
        int color;
        try {
            color = Integer.parseInt(id);
            color = color % TrackColor.length;
            color = TrackColor[color];
        } catch (NumberFormatException e) {
            color = Color.GREEN-0x20000000;
        }
        style.mLineStyle = new LineStyle(color, 8.0f);
        kmlPlacemark.mStyle = kmlDocument.addStyle(style);
        return kmlTrack;
    }
    Style buildDefaultStyle(){
        Drawable defaultKmlMarker = ResourcesCompat.getDrawable(getResources(), R.drawable.marker_default, null);
        assert defaultKmlMarker != null;
        Bitmap bitmap = ((BitmapDrawable)defaultKmlMarker).getBitmap();
        return new Style(bitmap, 0x901010AA, 3.0f, 0x20AA1010);
    }
    void updateUIWithKml(){
        if (folderOverlay != null){
            folderOverlay.closeAllInfoWindows();
            map_view.getOverlays().remove(folderOverlay);
        }
        folderOverlay = (FolderOverlay)kmlDocument.mKmlRoot.buildOverlay(map_view, buildDefaultStyle(), null, kmlDocument);
        map_view.getOverlays().add(folderOverlay);
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
        InfoWindow.closeAllInfoWindowsOn(map_view);
        if (!object.equals("")) {
            Toast.makeText(getBaseContext(), "Put " + object + " " + p.getLatitude() + "-" + p.getLongitude(), Toast.LENGTH_LONG).show();
            objectMarkers.add(p);
            drawMarker(p, object, objectMarkers.size()-1);
            object = "";
        }

        if (isDrawPolyline) {
            polyline.addPoint(p);
            map_view.invalidate();
        }
        return false;
    }

    @Override
    public boolean longPressHelper(GeoPoint p) {
        if (isDrawPolyline){
            isDrawPolyline = false;
            folderOverlay.add(polyline);
            Toast.makeText(getBaseContext(), "Exit draw polyline mode.", Toast.LENGTH_SHORT).show();
            drawPolylineText.setText("");
        }
        return false;
    }

    @Override
    public void onResume(){
        super.onResume();
        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        if (map_view!=null)
            map_view.onResume();
    }

    @Override
    public void onPause(){
        super.onPause();
        Configuration.getInstance().save(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        if (map_view!=null)
            map_view.onPause();
    }
}
