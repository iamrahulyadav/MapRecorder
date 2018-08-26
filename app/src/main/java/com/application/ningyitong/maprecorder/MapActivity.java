package com.application.ningyitong.maprecorder;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.kml.KmlDocument;
import org.osmdroid.bonuspack.kml.KmlFeature;
import org.osmdroid.bonuspack.kml.KmlLineString;
import org.osmdroid.bonuspack.kml.KmlPlacemark;
import org.osmdroid.bonuspack.kml.KmlPoint;
import org.osmdroid.bonuspack.kml.KmlPolygon;
import org.osmdroid.bonuspack.kml.KmlTrack;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.NetworkLocationIgnorer;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.infowindow.InfoWindow;
import org.osmdroid.views.overlay.mylocation.DirectedLocationOverlay;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hitomi.cmlibrary.CircleMenu;
import com.hitomi.cmlibrary.OnMenuSelectedListener;
import com.hitomi.cmlibrary.OnMenuStatusChangeListener;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class MapActivity extends AppCompatActivity implements MapEventsReceiver, LocationListener {
    // Create functional instance
    Database db;
    SharedPreferences sharedPreferences;
    UserSessionManager session;
    int userID;

    // OSM map related instances
    private MapView map_view;
    InfoWindow objectMarkerInfoWindow;
    InfoWindow objectPolylineInfoWindow;
    InfoWindow objectPolygonInfoWindow;
    private IMapController mapController;
    private LocationManager locationManager;
    private DirectedLocationOverlay directedLocationOverlay;

    // OSM map overlay variables
    private KmlDocument kmlDocument;    // Create KmlDocument to save Map details and export them to .kml file
    private FolderOverlay folderOverlay;    // Create FolderOverlay instace to save overlays
    private Polyline routeLine; // Polyline for displaying GPS tracking path
//    private ArrayList<Location> routeLineLocation;  // Location array for saving GPS tracking location
    private ArrayList<Marker> objectMarkers;
    private ArrayList<Polyline> objectPolylines;
    private ArrayList<Polygon> objectPolygons;
    private Polyline polyline;  // Create line object when invoking draw line method
    private Polygon polygon;    // Create polygon object when invoking draw polygon method

    // UI related instances
    private CircleMenu circleMenu;  // The circle menu for choosing different objects
    private ImageButton recordingGpsBtn;    // Active record map mode button
    private Dialog saveMapDialog;   // Call save map dialog window
    private TextView drawPolylineText;  // Display how to exit draw line mode

    // Map related variables
    private float azimuthAngleSpeed = 0.0f;    // Map azimuth parameter
    private String gpsProvider; // GPS provider parameter
    private GeoPoint startPoint, destinationPoint;  // Start point and end point of a recorded route

    // Define final object name or value
    private String object = ""; // The variable object is used for saving temp value when placing different map overlay objects
    private final String OBJECT_BUILDING = "Building";
    private final String OBJECT_HOSPITAL = "Hospital";
    private final String OBJECT_TRAFFIC_LIGHT = "Traffic Light";
    private final String OBJECT_HOTEL = "Hotel";
    private final String OBJECT_SHOP = "Shop";
    private final String OBJECT_HOUSE = "House";
    private static final long UPDATE_INTERVAL = 100;    // Update location acceptable minimise time interval

    // Define boolean parameter
    private Boolean isDrawPolyline = false; // Detect if draw line mode is active
    private Boolean isDrawPolygon = false;  // Detect if draw polygon mode is active
    private Boolean isRecording = false;    // Detect if record map mode is active

    // Load map
    private String loadedMapTitle = "", loadedMapUrl = "";
    private int loadedMapId;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().setOsmdroidBasePath(new File(Environment.getExternalStorageDirectory(), "osmdroid"));
        Configuration.getInstance().setOsmdroidTileCache(new File(Environment.getExternalStorageDirectory(), "osmdroid/tiles"));

        // Set map shared preferences and user session
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
        //important! set user agent to prevent getting banned from the osm servers
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        // Setup bottom nav-bar
        setupBottomNavbar();
        // Check if is loading map
        context = this;
        Intent receivedIntent = getIntent();
        loadedMapId = receivedIntent.getIntExtra("id", -1);
        loadedMapTitle = receivedIntent.getStringExtra("name");
        loadedMapUrl = receivedIntent.getStringExtra("tracking");
        if (loadedMapTitle==null || loadedMapUrl==null) {
            kmlDocument = new KmlDocument();    // Create KML file
            folderOverlay = new FolderOverlay();    // Create folder overlay: markers
            folderOverlay.setName("Map Markers");
            // Display folder overlay
            map_view.getOverlays().add(folderOverlay);
            updateUIWithOverlays();
        }
        else
            loadExistMap();
        // Initial OSM map view
        setupMapView(savedInstanceState);
        // Get best GPS provider
        Criteria criteria = new Criteria();
        gpsProvider = locationManager.getBestProvider(criteria, false);

        // Setup recording button
        setupRecordingBtn();

        // Create save map dialog
        saveMapDialog = new Dialog(this);
        // Create map basic control button
        setupMapControlBtn();
        // Create circle menu
        setupCircleMenu();
    }

    /** Method to load KML file if exists **/
    private void loadExistMap() {
        TextView loadedMapTitleTV = findViewById(R.id.loaded_map_title);
        loadedMapTitleTV.setText(loadedMapTitle);
        new KmlLoader().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    class MyKmlStyler implements KmlFeature.Styler {

        @Override
        public void onFeature(Overlay overlay, KmlFeature kmlFeature) {

        }

        @Override
        public void onPoint(Marker marker, KmlPlacemark kmlPlacemark, KmlPoint kmlPoint) {
            objectMarkerInfoWindow = new ObjectMarkerInfoWindow(R.layout.marker_bubble, map_view);
            marker.setInfoWindow(objectMarkerInfoWindow);
            marker.setDraggable(true);
            marker.setOnMarkerDragListener(onObjectMarkerDragListener);
            marker.setRelatedObject(objectMarkers.size());
            // Set marker icon based on object type
            switch (marker.getTitle().replace("Type: ", "")) {
                case OBJECT_BUILDING:
                    marker.setIcon(getResources().getDrawable(R.drawable.ic_marker_building));
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
            objectMarkers.add(marker);
        }

        @Override
        public void onLineString(Polyline polyline, KmlPlacemark kmlPlacemark, KmlLineString kmlLineString) {
            objectPolylineInfoWindow = new ObjectPolylineInfoWindow(R.layout.polyline_polygon_bubble, map_view);
            polyline.setInfoWindow(objectPolylineInfoWindow);
            polyline.setColor(Color.RED);
            polyline.setWidth(15);
            objectPolylines.add(polyline);
            polyline.setRelatedObject(objectPolylines.size());
        }

        @Override
        public void onPolygon(Polygon polygon, KmlPlacemark kmlPlacemark, KmlPolygon kmlPolygon) {
            objectPolygonInfoWindow = new ObjectPolygonInfoWindow(R.layout.polyline_polygon_bubble, map_view);
            polygon.setInfoWindow(objectPolygonInfoWindow);
            polygon.setFillColor(0x12121212);
            polygon.setStrokeColor(Color.RED);
            polygon.setStrokeWidth(10);
            objectPolygons.add(polygon);
            polygon.setRelatedObject(objectPolygons.size());
        }

        @Override
        public void onTrack(Polyline polyline, KmlPlacemark kmlPlacemark, KmlTrack kmlTrack) {

        }
    }
    @SuppressLint("StaticFieldLeak")
    private class KmlLoader extends AsyncTask<Void, Void, Void> {
        ProgressDialog progressDialog = new ProgressDialog(context);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.setMessage("Loading Map " + loadedMapUrl);
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            kmlDocument = new KmlDocument();
            // Add styler
            KmlFeature.Styler styler = new MyKmlStyler();
            File file = kmlDocument.getDefaultPathForAndroid(loadedMapUrl);
            kmlDocument.parseKMLFile(file);
            folderOverlay = new FolderOverlay();
            folderOverlay = (FolderOverlay) kmlDocument.mKmlRoot.buildOverlay(map_view, null, styler, kmlDocument);
            map_view.getOverlays().add(folderOverlay);
//            updateUIWithOverlays();
            return null;

        }

        @Override
        protected void onPostExecute(Void aVoid) {
            progressDialog.dismiss();
            map_view.invalidate();
            try {
                BoundingBox bb = kmlDocument.mKmlRoot.getBoundingBox();
                if (bb != null) {
                    map_view.zoomToBoundingBox(bb, true);
                }
            } catch (Exception exception){
                Toast.makeText(getBaseContext(), "KML map Bounding Box Error, you still can view it", Toast.LENGTH_SHORT).show();
            }

            //map_view.zoomToBoundingBox(bb, true);
//            mapView.getController().setCenter(bb.getCenter());
            super.onPostExecute(aVoid);
        }
    }

    public void updateUIWithOverlays() {
        folderOverlay.closeAllInfoWindows();
        folderOverlay.getItems().clear();
//        map_view.getOverlays().clear();
        for (int i=0; i<objectMarkers.size(); i++) {
            folderOverlay.add(objectMarkers.get(i));
        }
        for (int i=0; i<objectPolylines.size(); i++) {
            folderOverlay.add(objectPolylines.get(i));
        }
        for (int i=0; i<objectPolygons.size(); i++) {
            folderOverlay.add(objectPolygons.get(i));
        }
        map_view.invalidate();
    }

    class OnObjectMarkerDragListener implements Marker.OnMarkerDragListener {
        @Override
        public void onMarkerDrag(Marker marker) {
        }

        @Override
        public void onMarkerDragEnd(Marker marker) {
            marker.setSnippet(marker.getPosition().getLatitude() + " " + marker.getPosition().getLongitude());
        }

        @Override
        public void onMarkerDragStart(Marker marker) {
        }
    }

    final OnObjectMarkerDragListener onObjectMarkerDragListener = new OnObjectMarkerDragListener();

    public void deleteMarker(Marker selectMarker, int index) {
        objectMarkers.remove(index);
        folderOverlay.remove(selectMarker);
        map_view.getOverlays().remove(selectMarker);
        updateUIWithOverlays();
    }
    public void deletePolyline(Polyline selectPolyline, int index) {
        objectPolylines.remove(index);
        folderOverlay.remove(selectPolyline);
        map_view.getOverlays().remove(selectPolyline);
        updateUIWithOverlays();
    }
    public void deletePolygon(Polygon selectPolygon, int index) {
        objectPolygons.remove(index);
        folderOverlay.remove(selectPolygon);
        map_view.getOverlays().remove(selectPolygon);
        updateUIWithOverlays();
    }

    /** Draw Marker **/
    public void drawMarker(GeoPoint p, String object, int index) {
        Marker marker = new Marker(map_view);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        objectMarkerInfoWindow = new ObjectMarkerInfoWindow(R.layout.marker_bubble, map_view);
        marker.setInfoWindow(objectMarkerInfoWindow);
        marker.setDraggable(true);
        marker.setOnMarkerDragListener(onObjectMarkerDragListener);
        marker.setTitle("Type: " + object);
        marker.setPosition(p);
        // Set marker icon based on object type
        switch (object) {
            case OBJECT_BUILDING:
                marker.setIcon(getResources().getDrawable(R.drawable.ic_marker_building));
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
        marker.setSnippet(p.getLatitude() + " " + p.getLongitude());
        folderOverlay.add(marker);
        objectMarkers.add(marker);
        map_view.invalidate();
    }

    /** Draw polyline **/
    @SuppressLint("SetTextI18n")
    public void drawPolyline(int index) {
        if (isDrawPolygon || isDrawPolyline) {
            Toast.makeText(getBaseContext(), "Pleas exit drawing mode first.", Toast.LENGTH_LONG).show();
            return;
        }
        isDrawPolyline = true;
        drawPolylineText.setText("Long press to exit drawing mode...");
        polyline = new Polyline();
        objectPolylineInfoWindow = new ObjectPolylineInfoWindow(R.layout.polyline_polygon_bubble, map_view);
        polyline.setInfoWindow(objectPolylineInfoWindow);
        polyline.setColor(Color.RED);
        polyline.setWidth(15);
        polyline.setRelatedObject(index);
        map_view.getOverlays().add(polyline);
    }

    /** Draw polygon **/
    @SuppressLint("SetTextI18n")
    public void drawPolygon(int index) {
        if (isDrawPolygon || isDrawPolyline) {
            Toast.makeText(getBaseContext(), "Pleas exit drawing mode first.", Toast.LENGTH_LONG).show();
            return;
        }
        isDrawPolygon = true;
        drawPolylineText.setText("Long press to exit drawing mode...");
        polygon = new Polygon();
        objectPolygonInfoWindow = new ObjectPolygonInfoWindow(R.layout.polyline_polygon_bubble, map_view);
        polygon.setInfoWindow(objectPolygonInfoWindow);
        polygon.setTitle("Custom area");
        polygon.setFillColor(0x12121212);
        polygon.setStrokeColor(Color.RED);
        polygon.setStrokeWidth(10);
        polygon.setRelatedObject(index);
        map_view.getOverlays().add(polygon);
    }

    /** Setup recording button **/
    private void setupRecordingBtn() {
        recordingGpsBtn = findViewById(R.id.recording_gps_btn);
        recordingGpsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isRecording) {
                    isRecording = false;
//                    DouglasPeuckerAlgorithm douglasPeuckerAlgorithm = new DouglasPeuckerAlgorithm();
                    ArrayList<GeoPoint> pointsTemp;
                    pointsTemp = DouglasPeuckerAlgorithm.reduceWithTolerance(routeLine.getPoints(), 1500.0);
                    Polyline newRouteLine = new Polyline();
                    newRouteLine.setPoints(pointsTemp);
                    objectPolylineInfoWindow = new ObjectPolylineInfoWindow(R.layout.polyline_polygon_bubble, map_view);
                    newRouteLine.setInfoWindow(objectPolylineInfoWindow);
                    newRouteLine.setWidth(15);
                    newRouteLine.setRelatedObject(objectPolylines.size());

                    map_view.getOverlays().remove(routeLine);

                    map_view.getOverlays().add(newRouteLine);
                    folderOverlay.add(newRouteLine);
                    objectPolylines.add(newRouteLine);

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
//                        routeLineLocation = new ArrayList<>();
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
        startMarker.setRelatedObject(objectMarkers.size());
        objectMarkerInfoWindow = new ObjectMarkerInfoWindow(R.layout.marker_bubble, map_view);
        startMarker.setInfoWindow(objectMarkerInfoWindow);
        startMarker.setOnMarkerDragListener(onObjectMarkerDragListener);
        startMarker.setSnippet(startPoint.getLatitude() + " " + startPoint.getLongitude());
        folderOverlay.add(startMarker);
        objectMarkers.add(startMarker);
    }

    /** Create end marker **/
    private void addEndMarker(GeoPoint endPoint) {
        Marker endMarker = new Marker(map_view);
        endMarker.setPosition(endPoint);
        endMarker.setTitle("End Point");
        endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        endMarker.setDraggable(true);
        endMarker.setRelatedObject(objectMarkers.size());
        objectMarkerInfoWindow = new ObjectMarkerInfoWindow(R.layout.marker_bubble, map_view);
        endMarker.setInfoWindow(objectMarkerInfoWindow);
        endMarker.setOnMarkerDragListener(onObjectMarkerDragListener);
        endMarker.setSnippet(startPoint.getLatitude() + " " + startPoint.getLongitude());
        folderOverlay.add(endMarker);
        objectMarkers.add(endMarker);
    }

    /** Setup circle menu **/
    private void setupCircleMenu() {
        circleMenu = findViewById(R.id.edit_map_circle_menu);
        circleMenu.setMainMenu(Color.parseColor("#ffffff"), R.drawable.ic_edit_menu_24dp, R.mipmap.icon_cancel);
        circleMenu.addSubMenu(Color.parseColor("#258CFF"), R.drawable.ic_building_white_24dp)
                .addSubMenu(Color.parseColor("#30A400"), R.drawable.ic_traffic_white_48dp)
                .addSubMenu(Color.parseColor("#FF4B32"), R.drawable.ic_line_white_24dp)
                .addSubMenu(Color.parseColor("#8A39FF"), R.drawable.ic_panorama_wide_angle_white_24dp)
                .addSubMenu(Color.parseColor("#FF6A00"), R.drawable.ic_hospital_white_24dp)
                .addSubMenu(Color.parseColor("#FF4B32"), R.drawable.ic_hotel_white_24dp)
                .addSubMenu(Color.parseColor("#8A39FF"), R.drawable.ic_home_white_24dp)
                .addSubMenu(Color.parseColor("#FF6A00"), R.drawable.ic_shopping_cart_white_24dp);
        circleMenu.setOnMenuSelectedListener(new OnMenuSelectedListener() {
            @Override
            public void onMenuSelected(int i) {
                switch (i) {
                    case 0:
                        object = OBJECT_BUILDING;
                        break;
                    case 1:
                        object = OBJECT_TRAFFIC_LIGHT;
                        break;
                    case 2:
                        drawPolyline(objectPolylines.size());
                        break;
                    case 3:
                        drawPolygon(objectPolygons.size());
                        break;
                    case 4:
                        object = OBJECT_HOSPITAL;
                        break;
                    case 5:
                        object = OBJECT_HOTEL;
                        break;
                    case 6:
                        object = OBJECT_HOUSE;
                        break;
                    case 7:
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
        // Verify if recording mode is still active, if so, break
        if (isRecording) {
            Toast.makeText(getBaseContext(), "Please stop recording GPS first.", Toast.LENGTH_SHORT).show();
            return;
        }
        // Verify if drawing line mode is still active, if so, break
        if (isDrawPolyline) {
            Toast.makeText(getBaseContext(), "Pleas stop drawing line first (long press screen to exit draw line mode)", Toast.LENGTH_SHORT).show();
            return;
        }
        // Verify if drawing line mode is still active, if so, break
        if (isDrawPolygon) {
            Toast.makeText(getBaseContext(), "Pleas stop drawing polygon first (long press screen to exit draw line mode)", Toast.LENGTH_SHORT).show();
            return;
        }

        if (loadedMapId != -1) {
            kmlDocument.mKmlRoot.addOverlay(folderOverlay, kmlDocument);
            // Save map overlay
            saveKmlFile(loadedMapUrl);
            Toast.makeText(getBaseContext(), "Modify map details successuf.", Toast.LENGTH_SHORT).show();
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
                String tracking = name + "_" + owner + "_" + date.replaceAll("/", "") + ".kml";

                if (name.equals("")) {
                    mapName.setError("Input map name");
                    return;
                }
                if (owner.equals("")) {
                    mapOwner.setError("Input map owner");
                    return;
                }
                if (date.equals("")) {
                    mapDate.setError("Input map record date");
                    return;
                }

                if (db.checkMap(name)) {
                    Boolean insert = db.saveMap(name, city, description, owner, date, tracking, userID);
                    if (insert) {
                        kmlDocument.mKmlRoot.addOverlay(folderOverlay, kmlDocument);
                        // Save map overlay
                        saveKmlFile(tracking);
                        Toast.makeText(getBaseContext(), "Save map info successfully", Toast.LENGTH_SHORT).show();
                        saveMapDialog.dismiss();
                        locationManager.removeUpdates(MapActivity.this);    // Remove location listener after saving map
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Objects.requireNonNull(saveMapDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        saveMapDialog.show();
    }

    /** Save KML file **/
    private void saveKmlFile(String fileName) {
        boolean saved;
        File file = kmlDocument.getDefaultPathForAndroid(fileName);
        if (file.exists()) {
            boolean deleteSuccessful = file.delete();
            if (deleteSuccessful)
                Toast.makeText(getBaseContext(), "Delete exist .kml file successful", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(getBaseContext(), "Delete exist .kml file faield, the current will be overrite", Toast.LENGTH_SHORT).show();
        }
        saved = kmlDocument.saveAsKML(file);
        if (saved)
            Toast.makeText(this, fileName + " saved", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(this, "Unable to save " + fileName, Toast.LENGTH_SHORT).show();
    }

    /** Create map controller buttons
     * Create zoom in button
     * Create zoom out button
     * Create set map center button **/
    private void setupMapControlBtn() {
        // Create show draw polyline info text view
        drawPolylineText = findViewById(R.id.draw_polyline_status);

        // Create zoom in button
        ImageButton btnZoomIn = findViewById(R.id.zoom_in);
        btnZoomIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapController.zoomIn();
            }
        });

        // Create zoom out button
        ImageButton btnZoomOut = findViewById(R.id.zoom_out);
        btnZoomOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapController.zoomOut();
            }
        });

        // Create navigate to current location button
        final ImageButton btnLocation = findViewById(R.id.location_track);
        btnLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnLocation.setFocusable(true);
                if (directedLocationOverlay.isEnabled() && directedLocationOverlay.getLocation() != null) {
                    map_view.getController().animateTo(directedLocationOverlay.getLocation());
                }
                map_view.setMapOrientation(-azimuthAngleSpeed);
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
        mapController.setCenter(new GeoPoint((double) sharedPreferences.getFloat("CENTER_LAT", 53.384f), (double) sharedPreferences.getFloat("CENTER_LON", -1.491f)));
        // Create map location overlay
        directedLocationOverlay = new DirectedLocationOverlay(this);
        map_view.getOverlays().add(directedLocationOverlay);

        if (savedInstanceState == null) {
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
            objectPolylines = new ArrayList<>();
            objectPolygons = new ArrayList<>();

            if (directedLocationOverlay.isEnabled() && directedLocationOverlay.getLocation() != null) {
                mapController.animateTo(directedLocationOverlay.getLocation());
            }
        } else {
            directedLocationOverlay.setLocation((GeoPoint) savedInstanceState.getParcelable("location"));
            startPoint = savedInstanceState.getParcelable("start");
            destinationPoint = savedInstanceState.getParcelable("destination");
        }
    }

    /** Save map preferences **/
    // TODO
//    private void savePreferences() {
//        SharedPreferences prefs = getSharedPreferences("MAPRECORDER", MODE_PRIVATE);
//        SharedPreferences.Editor ed = prefs.edit();
//        ed.putFloat("ZOOM_LEVEL", (float) map_view.getZoomLevelDouble());
//        GeoPoint center = (GeoPoint) map_view.getMapCenter();
//        ed.putFloat("CENTER_LAT", (float) center.getLatitude());
//        ed.putFloat("CENTER_LON", (float) center.getLongitude());
//        MapTileProviderBase tileProvider = map_view.getTileProvider();
//        String tileProviderName = tileProvider.getTileSource().name();
//        ed.putString("TILE_PROVIDER", tileProviderName);
//        ed.apply();
//    }

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
        // Get current time
        long currentTime = System.currentTimeMillis();
        // Ignore the change if time period smaller than GPS waiting time
        if (networkLocationIgnorer.shouldIgnore(location.getProvider(), currentTime))
            return;
        // Ignore the change if time period smaller than required value: 0.1s (default setting)
        if ((currentTime - lastTime) < UPDATE_INTERVAL) {
            return;
        }
        lastTime = currentTime;

        // Convert Location to GeoPoint
        GeoPoint newLocation = new GeoPoint(location);
        // Set map location overlay and navigate to current location
        directedLocationOverlay.setLocation(newLocation);
        directedLocationOverlay.setAccuracy((int) location.getAccuracy());

        // Get current speed
        speed = location.getSpeed() * 3.6;
        DecimalFormat speedPattern = new DecimalFormat("0.00");
        // Display current speed
        TextView speedTxt = findViewById(R.id.speed);
        speedTxt.setText(String.format("Speed: %s km/h", speedPattern.format(speed)));

        // If current speed is acceptable, record and update the location
        if (speed >= 0.1) {
            azimuthAngleSpeed = location.getBearing();
            directedLocationOverlay.setBearing(azimuthAngleSpeed);
            // Is recording mode is active
            if (isRecording) {
                // Keep the map view centered on current location:
                map_view.getController().animateTo(newLocation);
                map_view.setMapOrientation(-azimuthAngleSpeed);
                // Add current GeoPoint to the ArrayList<GeoPoint>
                routeLine.addPoint(newLocation);
                // Add current location to the ArrayList<Location>
//                routeLineLocation.add(location);
            }
        }
        map_view.invalidate();  // Update map view
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
        // Single tap to exit marker info window
        InfoWindow.closeAllInfoWindowsOn(map_view);

        // Draw Marker overlay based on different object types
        if (!object.equals("")) {
            Toast.makeText(getBaseContext(), "Place a " + object, Toast.LENGTH_LONG).show();
            drawMarker(p, object, objectMarkers.size());
            object = "";
        }

        // Add polyline points and refresh map view
        if (isDrawPolyline) {
            polyline.addPoint(p);
            map_view.invalidate();
        }

        // Add polygon points and refresh map view
        if (isDrawPolygon) {
            polygon.addPoint(p);
            map_view.invalidate();
        }
        return false;
    }

    @Override
    public boolean longPressHelper(GeoPoint p) {
        // Long press to exit draw polyline mode
        if (isDrawPolyline) {
            isDrawPolyline = false;
            polylineCreateDialog();
            Toast.makeText(getBaseContext(), "Exit draw polyline mode.", Toast.LENGTH_SHORT).show();
            drawPolylineText.setText("");
        }

        if (isDrawPolygon) {
            isDrawPolygon = false;
            polygonCreateDialog();
            Toast.makeText(getBaseContext(), "Exit draw polygon mode.", Toast.LENGTH_SHORT).show();
            drawPolylineText.setText("");
        }
        return false;
    }

    /** Create polyline dialog
     * set polyline title
     * set polyline description
     * cancel draw polyline **/
    private void polylineCreateDialog() {
        final EditText polylineTitleET = new EditText(this);
        polylineTitleET.setHint("Enter line title");
        final EditText polylineDescriptionET = new EditText(this);
        polylineDescriptionET.setHint("Enter line description");
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(40,20,40,20);
        linearLayout.addView(polylineTitleET);
        linearLayout.addView(polylineDescriptionET);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Polyline Details");
        builder.setView(linearLayout);
        builder.setCancelable(false);

        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                polyline.setTitle("Title: \n" + polylineTitleET.getText().toString());
                polyline.setSubDescription("Description: \n" + polylineDescriptionET.getText().toString());
                folderOverlay.add(polyline);
                objectPolylines.add(polyline);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                map_view.getOverlays().remove(polyline);
                dialogInterface.cancel();
            }
        });
        builder.show();
    }

    /** Create polygon dialog
     * set polygon title
     * set polygon description
     * cancel draw polygon **/
    private void polygonCreateDialog() {
        final EditText polygonTitleET = new EditText(this);
        polygonTitleET.setHint("Enter line title");
        final EditText polygonDescriptionET = new EditText(this);
        polygonDescriptionET.setHint("Enter line description");
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(40,20,40,20);
        linearLayout.addView(polygonTitleET);
        linearLayout.addView(polygonDescriptionET);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Polygon Details");
        builder.setView(linearLayout);
        builder.setCancelable(false);
        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                polygon.setTitle("Title: \n" + polygonTitleET.getText().toString());
                polygon.setSubDescription("Description: \n" + polygonDescriptionET.getText().toString());
                folderOverlay.add(polygon);
                objectPolygons.add(polygon);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                map_view.getOverlays().remove(polygon);
                dialogInterface.cancel();
            }
        });
        builder.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check permissions before calling locationManager
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(gpsProvider, 500, 1, this);  // IMPORTANT! Call LocationListner on resume, in case of only call LocationListener only once
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
