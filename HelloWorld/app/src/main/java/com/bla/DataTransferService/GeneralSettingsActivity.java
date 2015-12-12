package com.bla.DataTransferService;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class GeneralSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_general_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Fill text boxes initially


        SharedPreferences settings = getSharedPreferences("DataTransferService", MODE_PRIVATE);
        String locationID = settings.getString("locationID", "Regenbecken42");
        EditText txtLocationID = (EditText) findViewById(R.id.txtLocationID);
        txtLocationID.setText(locationID);

        final Button btnSaveSettings = (Button) findViewById(R.id.btnSaveSettings);
        btnSaveSettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SharedPreferences settings = getSharedPreferences("DataTransferService", MODE_PRIVATE);
                String locationID = ((EditText) findViewById(R.id.txtLocationID)).getText().toString();
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("locationID", locationID);
                editor.commit();
                btnSaveSettings.setText("Saved...");
            }
        });
    }
}
