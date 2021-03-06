package com.application.ningyitong.maprecorder.MapList;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.application.ningyitong.maprecorder.DatabaseHelper;
import com.application.ningyitong.maprecorder.MapActivity;
import com.application.ningyitong.maprecorder.R;

import org.osmdroid.bonuspack.kml.KmlDocument;

import java.io.File;

public class EditMapItemActivity extends AppCompatActivity {
    // Create functional instances
    DatabaseHelper db;
    Button btnShare, btnDelete, btnSave;
    ImageButton btnBack, btnLoadMap;
    EditText mapName, mapCity, mapOwner, mapDescription, mapDate;

    private int selectedMapID;
    private String selectedMapName;
    private String selectedMapUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_map_item);

        // Create back page btn
        createBackBtn();

        // Initialise view
        initView();

        // Get data from EditActivity
        Intent receivedIntent = getIntent();
        selectedMapID = receivedIntent.getIntExtra("id", -1);
        selectedMapName = receivedIntent.getStringExtra("name");

        mapName.setText(selectedMapName);
        showMapDetails(selectedMapID, selectedMapName);

        // Load map btn
        btnLoadMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent mapActivity = new Intent(EditMapItemActivity.this, MapActivity.class);
                mapActivity.putExtra("id", selectedMapID);
                mapActivity.putExtra("name", selectedMapName);
                mapActivity.putExtra("tracking", selectedMapUrl);
                startActivity(mapActivity);
            }
        });

        // Share btn
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.setType("text/plain");
                String shareTitle = "Share KML Map" + mapName.getText().toString();

                String shareBody = "Map Title: " + mapName.getText().toString() + "\n" +
                        "City: " + mapCity.getText().toString() + "\n" +
                        "Owner: " + mapOwner.getText().toString() + "\n" +
                        "Date: " + mapDate.getText().toString() + "\n" +
                        "Description: " + mapDescription.getText().toString() + "\n" +
                        "--------------From Map Recorder";
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, shareTitle);
                sendIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
                KmlDocument kmlDocument = new KmlDocument();
                File file = kmlDocument.getDefaultPathForAndroid(selectedMapUrl);
                sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                startActivity(Intent.createChooser(sendIntent, "Share map using"));
            }
        });

        // Save btn
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveChangesConfirmationDialog();
            }
        });

        // Delete btn
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteConfirmationDialog();
            }
        });

    }

    /** Init view **/
    private void initView() {
        btnLoadMap = findViewById(R.id.map_item_load_btn);
        btnShare = findViewById(R.id.map_item_share_btn);
        btnDelete = findViewById(R.id.map_item_delete_btn);
        btnSave = findViewById(R.id.map_item_save_btn);
        mapName = findViewById(R.id.map_item_title);
        mapCity = findViewById(R.id.map_item_city);
        mapOwner = findViewById(R.id.map_item_owner);
        mapDescription = findViewById(R.id.map_item_description);
        mapDate = findViewById(R.id.map_item_date);
        db = new DatabaseHelper(this);
    }

    private void saveChangesConfirmationDialog() {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setMessage("Are you sure to save the changes?");
        alertDialog.setCancelable(false);
        // Confirm log out
        alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String newName = mapName.getText().toString();
                String newCity = mapCity.getText().toString();
                String newOwner = mapOwner.getText().toString();
                String newDate = mapDate.getText().toString();
                String newDescription = mapDescription.getText().toString();

                if (newName.equals("")) {
                    mapName.setError("Input map name");
                    return;
                }
                if (newOwner.equals("")) {
                    mapOwner.setError("Input map owner");
                    return;
                }
                if (newDate.equals("")) {
                    mapDate.setError("Input map date");
                    return;
                }

                // If map name exists, stop changing
                if (newName.equals(selectedMapName) || db.checkMap(newName)) {
                    db.updateMapInfo(newName, newCity, newOwner, newDate, newDescription, selectedMapID);
                    Toast.makeText(getBaseContext(), "Changes have been saved.", Toast.LENGTH_SHORT).show();
                    Intent intent_edit = new Intent(EditMapItemActivity.this, EditActivity.class);
                    startActivity(intent_edit);
                } else
                    Toast.makeText(getBaseContext(), "Map name exists, please change to another one.", Toast.LENGTH_SHORT).show();
            }
        });

        // Cancel operation
        alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });

        alertDialog.create().show();
    }

    /** Confirmation dialog for deleting map **/
    private void deleteConfirmationDialog() {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setMessage("Are you sure to delete this map?");
        alertDialog.setCancelable(false);
        // Confirm log out
        alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                db.deleteMap(selectedMapID);
                KmlDocument kmlDocument = new KmlDocument();
                File file = kmlDocument.getDefaultPathForAndroid(selectedMapUrl);
                if (file.exists()){
                    boolean deleteSuccessful = file.delete();
                    if (deleteSuccessful)
                        Log.v("Delete File", "Delete exist .kml file successful");
                    else
                        Log.v("Delete File", "Delete exist .kml file failed, the current will be overwritten");
                }
                Toast.makeText(getBaseContext(), mapName.getText().toString() + "has been deleted.", Toast.LENGTH_LONG).show();
                Intent intent_edit = new Intent(EditMapItemActivity.this, EditActivity.class);
                startActivity(intent_edit);
            }
        });

        // Cancel operation
        alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });

        alertDialog.create().show();
    }

    /** Show map current information **/
    private void showMapDetails(int selectedMapID, String selectedMapName) {
        db = new DatabaseHelper(this);
        Cursor mapItem = db.getMapInfoById(selectedMapID);
        if (mapItem.getCount()>0){
            mapItem.moveToFirst();
            if (mapItem.getString(mapItem.getColumnIndex("name")).equals(selectedMapName)) {
                mapName.setText(mapItem.getString(mapItem.getColumnIndex("name")));
                mapCity.setText(mapItem.getString(mapItem.getColumnIndex("city")));
                mapOwner.setText(mapItem.getString(mapItem.getColumnIndex("owner")));
                mapDate.setText(mapItem.getString(mapItem.getColumnIndex("date")));
                mapDescription.setText(mapItem.getString(mapItem.getColumnIndex("description")));
                selectedMapUrl = mapItem.getString(mapItem.getColumnIndex("tracking"));
            } else {
                Toast.makeText(getBaseContext(), "Render data failed.", Toast.LENGTH_LONG).show();
            }
        }
    }

    /** Create Back btn on nav-bar **/
    private void createBackBtn() {
        btnBack = findViewById(R.id.map_item_back_btn);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }
}
