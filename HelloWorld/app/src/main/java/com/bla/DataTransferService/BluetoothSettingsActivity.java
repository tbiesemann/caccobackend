package com.bla.DataTransferService;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import java.util.List;

public class BluetoothSettingsActivity extends AppCompatActivity {

    EditText txtBluetoothDeviceName;
    CheckBox cbxUseWindowsLineEndings;
    Button btnShowPairedDevices;
    TextView console;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        txtBluetoothDeviceName = (EditText) findViewById(R.id.txtBluetoothDeviceName);
        cbxUseWindowsLineEndings = (CheckBox) findViewById(R.id.cbxUseRN);
        btnShowPairedDevices = (Button) findViewById(R.id.btnShowPairedDevices);
        console = (TextView) findViewById(R.id.txtConsole);

        //Read from Settings
        SharedPreferences settings = getSharedPreferences("DataTransferService", MODE_PRIVATE);
        String deviceName = settings.getString("deviceName", "");
        txtBluetoothDeviceName.setText(deviceName);
        boolean useWindowsLineEndings = settings.getBoolean("useWindowsLineEndings", false);
        cbxUseWindowsLineEndings.setChecked(useWindowsLineEndings);


        final Button btnSaveSettings = (Button) findViewById(R.id.btnSaveSettings);
        btnSaveSettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SharedPreferences settings = getSharedPreferences("DataTransferService", MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();

                String deviceName = txtBluetoothDeviceName.getText().toString();
                editor.putString("deviceName", deviceName);

                boolean useWindowsLineEndings = cbxUseWindowsLineEndings.isChecked();
                editor.putBoolean("useWindowsLineEndings", useWindowsLineEndings);

                editor.commit();
                btnSaveSettings.setText("Saved...");
            }
        });


        btnShowPairedDevices.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                List<String> devices =  GlobalState.getInstance().bluetoothUtilities.getPairedDevicesAsString();
                for (String device : devices) {
                    log(device);
                }
            }
        });

    }


    public void log(String text) {
        console.setText(console.getText() + "\n" + text);
        System.out.println(text);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}

