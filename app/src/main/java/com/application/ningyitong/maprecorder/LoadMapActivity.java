package com.application.ningyitong.maprecorder;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
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

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.kml.KmlDocument;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.TileSystem;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.TilesOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.DirectedLocationOverlay;

import java.io.File;
import java.util.ArrayList;

public class LoadMapActivity extends AppCompatActivity implements LocationListener{

    String mapTitle, mapUrl;
    // Define UI
    TextView mapTitleText;
    DirectedLocationOverlay directedLocationOverlay;

    Database db;
    private MapView map_view;
    private IMapController mapController;
    private Context context;

    // Location API
    LocationManager locationManager;


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
        setContentView(R.layout.activity_load_map);
        // Get data from EditMapItemActivity
        context = this;
        Intent receivedIntent = getIntent();
        mapTitle = receivedIntent.getStringExtra("name");
        mapUrl = receivedIntent.getStringExtra("tracking");
        // Initial OSM
        setupMapView(savedInstanceState);
        // Initial Zoom control button
        setupMapControlBtn();
        // Setup circle menu
//        setupCircleMenu();

        mapTitleText = findViewById(R.id.load_map_title);
        mapTitleText.setText(mapTitle);
        loadKML();
    }

    /** Load KML file **/
    private void loadKML() {
        new KmlLoader().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onLocationChanged(Location location) {
        GeoPoint newLocation = new GeoPoint(location);
        if (!directedLocationOverlay.isEnabled()){
            //we get the location for the first time:
            directedLocationOverlay.setEnabled(true);
            map_view.getController().animateTo(newLocation);
        }
        directedLocationOverlay.setLocation(newLocation);
        directedLocationOverlay.setAccuracy((int)location.getAccuracy());
        map_view.getController().animateTo(newLocation);
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

    class KmlLoader extends AsyncTask<Void, Void, Void> {
        ProgressDialog progressDialog = new ProgressDialog(context);
        KmlDocument kmlDocument;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.setMessage("Loading Map " + mapUrl);
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            kmlDocument = new KmlDocument();
            File file = kmlDocument.getDefaultPathForAndroid(mapUrl);
            kmlDocument.parseKMLFile(file);

            FolderOverlay kmlOverlay = (FolderOverlay) kmlDocument.mKmlRoot.buildOverlay(map_view, null, null, kmlDocument);
            map_view.getOverlays().add(kmlOverlay);
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


    /** Init map view **/
    private void setupMapView(Bundle savedInstanceState) {
        map_view = findViewById(R.id.load_map_map_view);
        map_view.setTilesScaledToDpi(true);
        map_view.setClickable(true);
        map_view.setBuiltInZoomControls(false);
        map_view.setMultiTouchControls(true);
        map_view.setMinZoomLevel((double) 3);
        map_view.setMaxZoomLevel((double) 22);
        mapController = map_view.getController();
        mapController.setZoom((double) 18);
//        map_view.setVerticalMapRepetitionEnabled(false);
//        map_view.setScrollableAreaLimitLatitude(TileSystem.MaxLatitude,-TileSystem.MaxLatitude, 0/*map.getHeight()/2*/);

        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);

        mapController.setCenter(new GeoPoint(53.384f,-1.491f));
        // Scale Bar
        ScaleBarOverlay scaleBarOverlay = new ScaleBarOverlay(map_view);
        scaleBarOverlay.setCentred(true);
        scaleBarOverlay.setScaleBarOffset(this.getResources().getDisplayMetrics().widthPixels / 2, 10);
        map_view.getOverlays().add(scaleBarOverlay);
        // Compass
        CompassOverlay compassOverlay = new CompassOverlay(this, new InternalCompassOrientationProvider(this), map_view);
        compassOverlay.enableCompass();
        compassOverlay.setCompassCenter(30, 55);
        map_view.getOverlays().add(compassOverlay);

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
                //no location known: hide myLocationOverlay
                directedLocationOverlay.setEnabled(false);
            }
            if (directedLocationOverlay.isEnabled()&& directedLocationOverlay.getLocation() != null){
                mapController.animateTo(directedLocationOverlay.getLocation());
            }

        } else {
            directedLocationOverlay.setLocation((GeoPoint)savedInstanceState.getParcelable("location"));
        }

    }

    private void setupMapControlBtn() {
        // Zoom button
        ImageButton btnZoomIn = findViewById(R.id.load_map_zoom_in);
        btnZoomIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapController.zoomIn();
            }
        });

        ImageButton btnZoomOut = findViewById(R.id.load_map_zoom_out);
        btnZoomOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapController.zoomOut();
            }
        });

        final ImageButton btnLocation = findViewById(R.id.load_map_location_track);
        btnLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (directedLocationOverlay.isEnabled()&& directedLocationOverlay.getLocation() != null)
                    map_view.getController().animateTo(directedLocationOverlay.getLocation());
                else
                    Toast.makeText(getBaseContext(), "Cannot access your location", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
