package com.application.ningyitong.maprecorder.MapOverlays;

import org.osmdroid.views.overlay.Marker;

public class OnObjectMarkerDragListener implements Marker.OnMarkerDragListener {
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
