package com.application.ningyitong.maprecorder;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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

    @SuppressLint("SetTextI18n")
    @Override
    public void onOpen(Object object) {
        TextView markerTitle = mView.findViewById(R.id.marker_title);
        TextView markerDescription = mView.findViewById(R.id.marker_description);
        TextView markerLati = mView.findViewById(R.id.marker_lati);
        TextView markerLong = mView.findViewById(R.id.marker_long);
        Button btnDelete = mView.findViewById(R.id.marker_delete_btn);
        Button btnEdit = mView.findViewById(R.id.marker_edit_btn);

        final Marker marker = (Marker)object;
        selectMarker = (Integer)marker.getRelatedObject();
        markerTitle.setText(marker.getTitle());
        markerLati.setText("Lati: " + marker.getPosition().getLatitude());
        markerLong.setText("Long: " + marker.getPosition().getLongitude());

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MapActivity mapActivity = (MapActivity) view.getContext();
                mapActivity.deleteMarker(marker, selectMarker);
                close();
            }
        });

        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createEditDialog(marker);
                close();
            }
        });
    }

    private void createEditDialog(final Marker marker) {
        Context context = mMapView.getContext();

        final EditText markerTitleET = new EditText(context);
        markerTitleET.setHint("Enter marker title");
        final EditText markerDescriptionET = new EditText(context);
        markerDescriptionET.setHint("Enter marker description");
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(40,20,40,20);
        linearLayout.addView(markerTitleET);
        linearLayout.addView(markerDescriptionET);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Edit Marker Details");
        builder.setView(linearLayout);
        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                marker.setTitle("Type: " + markerTitleET.getText().toString());
                marker.setSubDescription("Description: \n" + markerDescriptionET.getText().toString());
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        builder.show();

    }

    @Override
    public void onClose() {

    }
}
