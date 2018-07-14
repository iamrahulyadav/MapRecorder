package com.application.ningyitong.maprecorder;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.DelayedMapListener;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import android.app.Activity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;

public class MapActivity extends AppCompatActivity {
    private MapView map_view;
    private MapController   mMapController;

    // Location API
    private FusedLocationProviderClient fusedLocationProviderClient;
    private SettingsClient settingsClient;
    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;
    private LocationCallback locationCallback;
    private Location currentLocation;

    // Location update interval
    private static final long UPDATE_INTERVAL = 10000;

    // user session
    UserSessionManager session;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final DisplayMetrics dm = this.getResources().getDisplayMetrics();

        session = new UserSessionManager(getApplicationContext());
        // Check user login status
        if (session.checkLogin()) {
            finish();
        }

        Context ctx = getApplicationContext();
        //important! set your user agent to prevent getting banned from the osm servers
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        setContentView(R.layout.activity_map);

        // Bottom nav-bar
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        Menu menu = navigation.getMenu();
        MenuItem menuItem = menu.getItem(1);
        menuItem.setChecked(true);

        // Initial OSM
        map_view = (MapView) findViewById(R.id.mapview);
        map_view.setTileSource(TileSourceFactory.MAPNIK);
        // Enable map clickable
//        map_view.setClickable(true);

        // Disable builtin zoom controller
        map_view.setBuiltInZoomControls(false);
        // Enable touch control
        map_view.setMultiTouchControls(true);
        // Set zoom limitation
        map_view.setMinZoomLevel((double) 3);
        map_view.setMaxZoomLevel((double) 22);
        // Set map default zoom level
        mMapController = (MapController) map_view.getController();
        mMapController.setZoom(18);
        // Display scale bar
        ScaleBarOverlay sclaeBar = new ScaleBarOverlay(map_view);

        // Location
        LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new MyLocationListner();


        // Location
//        GpsMyLocationProvider gpsMyLocationProvider = new GpsMyLocationProvider(this);
//        gpsMyLocationProvider.setLocationUpdateMinDistance(100);
//        gpsMyLocationProvider.setLocationUpdateMinTime(10000);
//        gpsMyLocationProvider.addLocationSource(LocationManager.NETWORK_PROVIDER);

        MyLocationNewOverlay myLocationoverlay = new MyLocationNewOverlay(map_view);
        myLocationoverlay.enableFollowLocation();
        myLocationoverlay.enableMyLocation();
        map_view.getOverlays().add(myLocationoverlay);

        // Compass
        CompassOverlay mCompassOverlay = new CompassOverlay(this, new InternalCompassOrientationProvider(this), map_view);
        mCompassOverlay.enableCompass();
        map_view.getOverlays().add(mCompassOverlay);

        // Scale Bar
//        ScaleBarOverlay mScaleBarOverlay = new ScaleBarOverlay(map_view);
//        mScaleBarOverlay.setCentred(true);
//        mScaleBarOverlay.setScaleBarOffset(dm.widthPixels / 2, 10);
//        map_view.getOverlays().add(mScaleBarOverlay);

        // Set map center
        //final GeoPoint startPoint = new GeoPoint(52.245199, -0.1455979);
        //mMapController.setCenter(startPoint);

        // Zoom button
        ImageButton btnZoomIn = (ImageButton) findViewById(R.id.zoom_in);
        btnZoomIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMapController.zoomIn();
            }
        });

        ImageButton btnZoomOut = (ImageButton) findViewById(R.id.zoom_out);
        btnZoomOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMapController.zoomOut();
            }
        });

        ImageButton btnLocation = (ImageButton) findViewById(R.id.location_track);
        btnLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLocation != null) {
                    mMapController.setCenter(new GeoPoint(currentLocation));
                } else {
                    Toast.makeText(getBaseContext(), "Cannot access your current location", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    // Get coordinates listener
    private class MyLocationListner implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            Toast.makeText(getBaseContext(), "Location changed: Lat: " + location.getLatitude() + " Lng: " + location.getLongitude(), Toast.LENGTH_LONG).show();
            currentLocation = location;
            if (location != null) {
                map_view.getController().setCenter(new GeoPoint(location));
            }
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
                    Intent intent_clouud = new Intent(MapActivity.this, CloudActivity.class);
                    startActivity(intent_clouud);
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
