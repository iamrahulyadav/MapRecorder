package com.application.ningyitong.maprecorder;

import org.osmdroid.util.GeoPoint;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.widget.Toast;

public class OsmLocationUpdateHelper implements LocationListener {

    private MapActivity mapActivity;

    public OsmLocationUpdateHelper(MapActivity mMapActivity) {
        this.mapActivity = mMapActivity;
    }

    @Override
    public void onLocationChanged(Location location) {
        Toast.makeText(mapActivity, "Latitude = " + location.getLatitude() * 1e6 + " Longitude = " + location.getLongitude() * 1e6, Toast.LENGTH_SHORT).show();
        int latitude = (int) (location.getLatitude() * 1E6);
        int longitude = (int) (location.getLongitude() * 1E6);
        GeoPoint point = new GeoPoint(latitude, longitude);
        mapActivity.updateCurrentLocation(point);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
