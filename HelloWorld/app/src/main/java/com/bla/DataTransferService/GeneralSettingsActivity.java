package com.bla.DataTransferService;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;

public class GeneralSettingsActivity extends AppCompatActivity {

    Button btnForceSync;
    EditText txtLocationID;
    NumberPicker numberPickerGDriveSyncIntervall;
    Button btnSaveSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_general_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SharedPreferences settings = getSharedPreferences("DataTransferService", MODE_PRIVATE);
        String locationID = settings.getString("locationID", "Regenbecken42");
        txtLocationID = (EditText) findViewById(R.id.txtLocationID);
        txtLocationID.setText(locationID);

        Integer GDriveSyncIntervall = settings.getInt("GDriveSyncIntervall", 3);
        numberPickerGDriveSyncIntervall = (NumberPicker) findViewById(R.id.numberPickerGDriveSyncIntervall);
        numberPickerGDriveSyncIntervall.setMinValue(1);
        numberPickerGDriveSyncIntervall.setMaxValue(48);
        numberPickerGDriveSyncIntervall.setValue(GDriveSyncIntervall);


        btnSaveSettings = (Button) findViewById(R.id.btnSaveSettings);
        btnSaveSettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SharedPreferences settings = getSharedPreferences("DataTransferService", MODE_PRIVATE);
                String locationID = ((EditText) findViewById(R.id.txtLocationID)).getText().toString();
                Integer GDriveSyncIntervall = ((NumberPicker) findViewById(R.id.numberPickerGDriveSyncIntervall)).getValue();
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("locationID", locationID);
                editor.putInt("GDriveSyncIntervall", GDriveSyncIntervall);
                editor.commit();
                btnSaveSettings.setText("Saved...");
                try {
                    AquaService.getInstance().setActivity(MainActivity.mainActivityInstance, MainActivity.mainActivityInstance);
                } catch (Exception ex) {
                    btnSaveSettings.setText("Something went really wrong: " + ex.toString());
                }
            }
        });


        btnForceSync = (Button) findViewById(R.id.btnForceGDriveSync);
        this.btnForceSync.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Thread forceSyncThead = new Thread() {  //Gdrive sync must be called in worker thread
                    public void run() {
                        AquaService.getInstance().log("Force Sync...");
                        AquaService.getInstance().synchronizeToGDrive();
                    }
                };
                forceSyncThead.start();
            }
        });
    }
}
