package com.application.ningyitong.maprecorder.MapOverlays;

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
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

public class ObjectPolygonInfoWindow extends InfoWindow{

    private int selectPolygon;

    public ObjectPolygonInfoWindow(int layoutResId, MapView mapView) {
        super(layoutResId, mapView);
    }

    @Override
    public void onOpen(Object object) {
        final Polygon polygon = (Polygon)object;

        TextView title = mView.findViewById(R.id.polyline_polygon_title);
        TextView description = mView.findViewById(R.id.polyline_polygon_description);
        Button btnDelete = mView.findViewById(R.id.polyline_polygon_delete_btn);
        Button btnEdit = mView.findViewById(R.id.polyline_polygon_edit_btn);

        selectPolygon = (Integer)polygon.getRelatedObject();
        title.setText(polygon.getTitle());
        description.setText(polygon.getSubDescription());

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MapActivity mapActivity = (MapActivity) view.getContext();
                mapActivity.deletePolygon(polygon, selectPolygon);
                close();
            }
        });

        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createEdidDialog(polygon);
                close();
            }
        });
    }

    private void createEdidDialog(final Polygon polygon) {
        Context context = mMapView.getContext();

        final EditText polygonTitleET = new EditText(context);
        polygonTitleET.setHint("Enter polygon title");
        final EditText polygonDescriptionET = new EditText(context);
        polygonDescriptionET.setHint("Enter polygon description");
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(40,20,40,20);
        linearLayout.addView(polygonTitleET);
        linearLayout.addView(polygonDescriptionET);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Edit Details");
        builder.setView(linearLayout);
        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                polygon.setTitle("Title: " + polygonTitleET.getText().toString());
                polygon.setSubDescription("Description: \n" + polygonDescriptionET.getText().toString());
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
