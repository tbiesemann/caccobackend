package com.bla.DataTransferService;

import android.bluetooth.BluetoothAdapter;
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
    Button btnTestConnection;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        txtBluetoothDeviceName = (EditText) findViewById(R.id.txtBluetoothDeviceName);
        cbxUseWindowsLineEndings = (CheckBox) findViewById(R.id.cbxUseRN);
        btnTestConnection = (Button) findViewById(R.id.btnTestBluetoothConnection);

        //Read from Settings
        SharedPreferences settings = getSharedPreferences("DataTransferService", MODE_PRIVATE);
        String deviceName = settings.getString("deviceName", "");
        txtBluetoothDeviceName.setText(deviceName);
        boolean useWindowsLineEndings = settings.getBoolean("useWindowsLineEndings", false);
        cbxUseWindowsLineEndings.setChecked(useWindowsLineEndings);


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
                SharedPreferences.Editor editor = settings.edit();

                String deviceName = txtBluetoothDeviceName.getText().toString();
                editor.putString("deviceName", deviceName);

                boolean useWindowsLineEndings = cbxUseWindowsLineEndings.isChecked();
                editor.putBoolean("useWindowsLineEndings", useWindowsLineEndings);

                editor.commit();
                btnSaveSettings.setText("Saved...");
            }
        });


        final Button btnShowPairedDevices = (Button) findViewById(R.id.btnShowPairedDevices);
        btnShowPairedDevices.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                List<String> devices =  GlobalState.getInstance().bluetoothUtilities.getPairedDevicesAsString();
                for (String device : devices) {
                    log(device);
                }
            }
        });


        this.createBluetoothUtilities();
    }


    private void createBluetoothUtilities(){



        GlobalState.getInstance().bluetoothUtilities.useWindowsLineEndings = cbxUseWindowsLineEndings.isChecked();
        GlobalState.getInstance().bluetoothUtilities.setLogger(new ILogger() {
            @Override
            public void onLog(String text) {
                log(text);
            }

            public void onLogAsync(String text) {
            }
        });
    }


    public void log(String text) {
        TextView console = (TextView) findViewById(R.id.txtConsole);
        console.setText(console.getText() + "\n" + text);
        System.out.println(text);
    }


    private void testConnection() {

        GlobalState.getInstance().bluetoothUtilities.useWindowsLineEndings = cbxUseWindowsLineEndings.isChecked();

        //Make sure bluetoothUtilities is turned on
        BluetoothAdapter mBluetoothAdapter =  GlobalState.getInstance().bluetoothUtilities.getBluetoothAdapter();
        if (mBluetoothAdapter == null) {
            this.log("Bluetooth adapter is not available");
            return;
        }
        GlobalState.getInstance().bluetoothUtilities.enableBluetoothAdapter();
        String deviceName = ((EditText) findViewById(R.id.txtBluetoothDeviceName)).getText().toString();
        GlobalState.getInstance().bluetoothUtilities.establishConnection(deviceName);
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}

