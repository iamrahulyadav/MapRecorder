package com.application.ningyitong.maprecorder;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.application.ningyitong.maprecorder.MapActivity;
import com.application.ningyitong.maprecorder.R;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

public class ObjectPolylineInfoWindow extends InfoWindow {

    private int selectPolyline;

    public ObjectPolylineInfoWindow(int layoutResId, MapView mapView) {
        super(layoutResId, mapView);
    }

    @Override
    public void onOpen(Object object) {
        final Polyline polyline = (Polyline)object;

        TextView title = mView.findViewById(R.id.polyline_polygon_title);
        TextView description = mView.findViewById(R.id.polyline_polygon_description);
        Button btnDelete = mView.findViewById(R.id.polyline_polygon_delete_btn);
        Button btnEdit = mView.findViewById(R.id.polyline_polygon_edit_btn);

        selectPolyline = (Integer)polyline.getRelatedObject();
        title.setText(polyline.getTitle());
        description.setText(polyline.getSubDescription());

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MapActivity mapActivity = (MapActivity) view.getContext();
                mapActivity.deletePolyline(polyline, selectPolyline);
                close();
            }
        });

        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createEdidDialog(polyline);
                close();
            }
        });
    }

    private void createEdidDialog(final Polyline polyline) {
        Context context = mMapView.getContext();

        final EditText polylineTitleET = new EditText(context);
        polylineTitleET.setHint("Enter line title");
        final EditText polylineDescriptionET = new EditText(context);
        polylineDescriptionET.setHint("Enter line description");
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(40,20,40,20);
        linearLayout.addView(polylineTitleET);
        linearLayout.addView(polylineDescriptionET);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Edit Details");
        builder.setView(linearLayout);
        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                polyline.setTitle("Title: " + polylineTitleET.getText().toString());
                polyline.setSubDescription("Description: \n" + polylineDescriptionET.getText().toString());
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
