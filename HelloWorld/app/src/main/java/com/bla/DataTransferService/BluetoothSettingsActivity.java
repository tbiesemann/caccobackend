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

        final Button btnTestConnection = (Button) findViewById(R.id.btnTestBluetoothConnectionn);
        btnTestConnection.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                btnTestConnection.setText("Testing Connection...");
                testConnection();
                // Perform action on click
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
                for(String device : devices){
                    log(device);
                }
            }
        });

//        mReceiver = new BroadcastReceiver() {
//
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                String action = intent.getAction();
//                // When discovery finds a device
//                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
//                    // Get the BluetoothDevice object from the Intent
//                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                    String discoveredDeviceName = device.getName();
//
//                    //get device name from textbox
//                    final EditText inputField = (EditText) findViewById(R.id.txtBluetoothDeviceName);
//                    String deviceName = inputField.getText().toString();
//
//                    if (deviceName.equals(discoveredDeviceName)) {
//                        log("Device " + discoveredDeviceName + " found");
//                        setBluetoothDevice(device);
//                    } else {
//                        log("Device " + discoveredDeviceName + " found but does not match " + deviceName);
//                    }
//                }
//            }
//        };

//        // Register the BroadcastReceiver
//        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
//        registerReceiver(mReceiver, filter);

        final EditText inputField = (EditText) findViewById(R.id.txtBluetoothDeviceName);
        this.bluetooth = new BluetoothUtilities(inputField.getText().toString(), this);
    }


//    private void setBluetoothDevice(BluetoothDevice device) {
//        UUID uuid;
//        try {
//            uuid = device.getUuids()[0].getUuid();
//        } catch (NullPointerException e) {
//            uuid = UUID.fromString("00000000-0000-0000-0000-000000000001"); //default UUID
//        }
//        log("Establishing connection with device " + device.getName() + " and address " + device.getAddress() + " and UUID " + uuid.toString());
//        try {
//            socket = device.createRfcommSocketToServiceRecord(uuid);
//            socket.connect();
//        } catch (IOException e) {
//            log("Cannot establish connection - error ocurred " + e.toString());
//            return;
//        }
//        this.log("Bluetooth connection is established");
//
//    }

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
        this.bluetooth.startDeviceDiscovery();

//
//        this.log("Bluetooth adapter is available");
//        this.bluetooth
//        if (!mBluetoothAdapter.isEnabled()) {
//            this.log("Enabling Bluetooth adapter...");
//            mBluetoothAdapter.enable();
//        }
//        this.log("Bluetooth adapter is enabled");
//        this.log("Starting device discovery...");
//        mBluetoothAdapter.startDiscovery();

    }

//    private BluetoothSocket socket;
//    private BroadcastReceiver mReceiver;


    @Override
    public void onDestroy() {
        super.onDestroy();
        this.bluetooth.destroy();
//        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        if (mBluetoothAdapter != null) {
//            mBluetoothAdapter.cancelDiscovery();
//        }

//        if (this.socket != null) {
//            try {
//                this.socket.close();
//            } catch (IOException ex) {
//            }
//        }
//
//        this.unregisterReceiver(mReceiver);

    }

}

