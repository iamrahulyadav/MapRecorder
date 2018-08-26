//package com.application.ningyitong.maprecorder;
//
//import org.osmdroid.util.GeoPoint;
//import org.osmdroid.views.overlay.Overlay;
//
//import android.location.Location;
//import android.location.LocationListener;
//import android.os.Bundle;
//import android.widget.Toast;
//
//import java.util.List;
//
//public class OsmLocationUpdateHelper implements LocationListener {
//
//    private MapActivity mapActivity;
//
//    // Define geo data
//    public List<GeoPoint> gpsDataList;
//    float speed;
//
//    public OsmLocationUpdateHelper(MapActivity mapActivity) {
//        this.mapActivity = mapActivity;
//    }
//
//    @Override
//    public void onLocationChanged(Location location) {
//        Toast.makeText(mapActivity, "Helper: " + "Latitude = " + location.getLatitude() * 1e6 + " Longitude = " + location.getLongitude() * 1e6, Toast.LENGTH_SHORT).show();
//
//        if (location != null) {
//            if (mapActivity.isRecording) {
//                Toast.makeText(mapActivity, "Recording...", Toast.LENGTH_SHORT).show();
//                if (gpsDataList!=null && gpsDataList.size()>=1) {
//                    speed = location.getSpeed();
//                    int latitude = (int) (location.getLatitude() * 1E6);
//                    int longitude = (int) (location.getLongitude() * 1E6);
//                    GeoPoint point = new GeoPoint((double) latitude, (double) longitude);
//                    //mapActivity.updateCurrentLocation(point);
//                    Toast.makeText(mapActivity, "Latitude = " + location.getLatitude() * 1e6 + " Longitude = " + location.getLongitude() * 1e6, Toast.LENGTH_SHORT).show();
//
//                    gpsDataList.add(point);
//
//                }
//            }
//        }
//
//    }
//
//    @Override
//    public void onStatusChanged(String provider, int status, Bundle extras) {
//
//    }
//
//    @Override
//    public void onProviderEnabled(String provider) {
//
//    }
//
//    @Override
//    public void onProviderDisabled(String provider) {
//
//    }
//}
