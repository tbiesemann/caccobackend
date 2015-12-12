package com.bla.DataTransferService;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.content.Intent;
import android.widget.EditText;

public class BluetoothSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Fill text boxes initially
        SharedPreferences settings = getSharedPreferences("DataTransferService", MODE_PRIVATE);
        String deviceName = settings.getString("deviceName", "");
        String passphrase = settings.getString("passphrase", "");
        EditText txtBluetoothDeviceName = (EditText) findViewById(R.id.txtBluetoothDeviceName);
        txtBluetoothDeviceName.setText(deviceName);
        EditText txtBluetoothPassphrase = (EditText) findViewById(R.id.txtBluetoothPassphrase);
        txtBluetoothPassphrase.setText(passphrase);


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
                String passphrase = ((EditText) findViewById(R.id.txtBluetoothPassphrase)).getText().toString();
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("deviceName", deviceName);
                editor.putString("passphrase", passphrase);
                editor.commit();
                btnSaveSettings.setText("Saved...");
            }
        });



        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                // When discovery finds a device
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    // Add the name and address to an array adapter to show in a ListView
//TB                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());

                    //get device name from textbox
                    final EditText inputField = (EditText) findViewById(R.id.txtBluetoothDeviceName);
                    Editable deviceName = inputField.getText();

                    if (deviceName.toString() == device.getName()){
                        System.out.println("Device " + device.getName() + " found" );
                    } else {
                        System.out.println("Device " + device.getName() + " found but does not match " + deviceName.toString() );
                    }
                }
            }
        };

        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
    }


    private void testConnection(){

        //Make sure bluetooth is turned on
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null){
            System.out.println ("Bluetooth adapter is not available");
            return;
        }
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
//            //Ask user to enable bluetooth
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableBtIntent, 42);
        }
        mBluetoothAdapter.startDiscovery();

    }

    // Create a BroadcastReceiver for ACTION_FOUND
    private BroadcastReceiver mReceiver;


}
