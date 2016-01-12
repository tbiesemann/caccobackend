package com.bla.DataTransferService;

import android.bluetooth.BluetoothAdapter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;

public class BluetoothSettingsActivity extends AppCompatActivity {

    private BluetoothUtilities bluetooth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Fill text boxes initially
        SharedPreferences settings = getSharedPreferences("DataTransferService", MODE_PRIVATE);
        String deviceName = settings.getString("deviceName", "");
        EditText txtBluetoothDeviceName = (EditText) findViewById(R.id.txtBluetoothDeviceName);
        txtBluetoothDeviceName.setText(deviceName);

        final Button btnTestConnection = (Button) findViewById(R.id.btnTestBluetoothConnection);
        btnTestConnection.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                btnTestConnection.setText("Opening Connection...");
                testConnection();
                btnTestConnection.setText("Open Connection");
            }
        });


        final Button btnSaveSettings = (Button) findViewById(R.id.btnSaveSettings);
        btnSaveSettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SharedPreferences settings = getSharedPreferences("DataTransferService", MODE_PRIVATE);
                String deviceName = ((EditText) findViewById(R.id.txtBluetoothDeviceName)).getText().toString();
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("deviceName", deviceName);
                editor.commit();
                btnSaveSettings.setText("Saved...");
            }
        });


        final Button btnShowPairedDevices = (Button) findViewById(R.id.btnShowPairedDevices);
        btnShowPairedDevices.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                List<String> devices = bluetooth.getPairedDevicesAsString();
                for (String device : devices) {
                    log(device);
                }
            }
        });


        this.bluetooth = BluetoothUtilitiesFactory.getBluetoothUtilities();

        this.bluetooth.setLogger(new ILogger() {
            @Override
            public void onLog(String text) {
                log(text);
            }
            public void onLogAsync(String text){
            }
        });
    }


    public void log(String text) {
        TextView console = (TextView) findViewById(R.id.txtConsole);
        console.setText(console.getText() + "\n" + text);
        System.out.println(text);
    }


    private void testConnection() {
        //Make sure bluetooth is turned on
        BluetoothAdapter mBluetoothAdapter = this.bluetooth.getBluetoothAdapter();
        if (mBluetoothAdapter == null) {
            this.log("Bluetooth adapter is not available");
            return;
        }
        this.bluetooth.enableBluetoothAdapter();
        String deviceName = ((EditText) findViewById(R.id.txtBluetoothDeviceName)).getText().toString();
        this.bluetooth.establishConnection(deviceName);
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}

