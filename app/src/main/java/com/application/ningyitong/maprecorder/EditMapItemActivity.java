package com.application.ningyitong.maprecorder;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.maps.model.TileOverlay;

public class EditMapItemActivity extends AppCompatActivity {

    Database db;

    private Button btnDelete, btnSave;
    ImageButton btnBack;
    EditText mapName, mapCity, mapOwner, mapDescription, mapDate;

    private int selectedMapID;
    private String selectedMapName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_map_item);

        // Create back page btn
        createBackBtn();

        // Find view
        btnDelete = (Button)findViewById(R.id.map_item_delete_btn);
        btnSave = (Button)findViewById(R.id.map_item_save_btn);
        mapName = (EditText)findViewById(R.id.map_item_title);
        mapCity = (EditText)findViewById(R.id.map_item_city);
        mapOwner = (EditText)findViewById(R.id.map_item_owner);
        mapDescription = (EditText)findViewById(R.id.map_item_description);
        mapDate = (EditText)findViewById(R.id.map_item_date);
        db = new Database(this);

        // Get data from EditActivity
        Intent receivedIntent = getIntent();
        selectedMapID = receivedIntent.getIntExtra("id", -1);
        selectedMapName = receivedIntent.getStringExtra("name");

        mapName.setText(selectedMapName);
        showMapDetails(selectedMapID, selectedMapName);

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

                if (!newName.equals("")) {
                    if (db.checkMap(newName)) {
                        db.updateMapInfo(newName, newCity, newOwner, newDate, newDescription, selectedMapID);
                        Toast.makeText(getBaseContext(), "Changes have been saved.", Toast.LENGTH_SHORT).show();
                        Intent intent_edit = new Intent(EditMapItemActivity.this, EditActivity.class);
                        startActivity(intent_edit);
                    } else {
                        Toast.makeText(getBaseContext(), "Map name exists, please change to another one.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getBaseContext(), "Map title cannot empty!", Toast.LENGTH_SHORT).show();
                }
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

    private void deleteConfirmationDialog() {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setMessage("Are you sure to delete this map?");
        alertDialog.setCancelable(false);
        // Confirm log out
        alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                db.deleteMap(selectedMapID);
                Toast.makeText(getBaseContext(), mapName.getText().toString() + "has been removed from database.", Toast.LENGTH_LONG).show();
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

    private void showMapDetails(int selectedMapID, String selectedMapName) {
        db = new Database(this);
        Cursor mapItem = db.getMapInfoById(selectedMapID);
        if (mapItem.getCount()>0){
            mapItem.moveToFirst();
            if (mapItem.getString(mapItem.getColumnIndex("name")).equals(selectedMapName)) {
                mapName.setText(mapItem.getString(mapItem.getColumnIndex("name")));
                mapCity.setText(mapItem.getString(mapItem.getColumnIndex("city")));
                mapOwner.setText(mapItem.getString(mapItem.getColumnIndex("owner")));
                mapDate.setText(mapItem.getString(mapItem.getColumnIndex("date")));
                mapDescription.setText(mapItem.getString(mapItem.getColumnIndex("description")));
            } else {
                Toast.makeText(getBaseContext(), "Render data failed.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void createBackBtn() {
        btnBack = (ImageButton)findViewById(R.id.map_item_back_btn);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }
}
