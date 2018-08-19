package com.application.ningyitong.maprecorder;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.infowindow.InfoWindow;
import org.osmdroid.views.overlay.Marker;

public class ObjectMarkerInfoWindow extends InfoWindow {

    /**
     * To have a better details in Object markers
     * Custom the markers info window
     */
    private int selectMarker;

    public ObjectMarkerInfoWindow(int layoutResId, MapView mapView) {
        super(layoutResId, mapView);
    }

    @Override
    public void onOpen(Object object) {
        TextView markerTitle = mView.findViewById(R.id.marker_title);
        TextView markerLati = mView.findViewById(R.id.marker_lati);
        TextView markerLong = mView.findViewById(R.id.marker_long);

        Button btnDelete = mView.findViewById(R.id.marker_delete);
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MapActivity mapActivity = (MapActivity) view.getContext();
                mapActivity.deletePoint(selectMarker);
                close();
            }
        });
        Marker marker = (Marker)object;
        selectMarker = (Integer)marker.getRelatedObject();
        markerTitle.setText(marker.getTitle());
        markerLati.setText("Lati: " + marker.getPosition().getLatitude());
        markerLong.setText("Long: " + marker.getPosition().getLongitude());

    }

    @Override
    public void onClose() {

    }
}
