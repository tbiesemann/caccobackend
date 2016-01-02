package com.bla.DataTransferService;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.EditText;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public class BluetoothUtilities {

    String mDeviceName;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSettingsActivity mActivity;
    private BluetoothDevice mDevice;
    private BluetoothSocket mSocket;

    public BluetoothUtilities(String deviceName, BluetoothSettingsActivity activity) {
        mDeviceName = deviceName;
        mActivity = activity;
    }


    public BluetoothAdapter getBluetoothAdapter() {
        if (mBluetoothAdapter == null) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        return mBluetoothAdapter;
    }

    public void enableBluetoothAdapter() {
        BluetoothAdapter adapter = this.getBluetoothAdapter();
        if (!adapter.isEnabled()) {
            this.mActivity.log("Enabling Bluetooth adapter...");
            adapter.enable();
        }
        this.mActivity.log("Bluetooth adapter is enabled");
    }


    public void startDeviceDiscovery() {
        this.mActivity.log("Starting device discovery...");
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        mActivity.registerReceiver(mReceiver, filter);
        mBluetoothAdapter.startDiscovery();
    }


    public UUID getDeviceUUID(BluetoothDevice device){
        UUID uuid;
        try {
            uuid = device.getUuids()[0].getUuid();
        } catch (NullPointerException e) {
            uuid = UUID.fromString("00000000-0000-0000-0000-000000000001"); //default UUID
        }
        return uuid;
    }

    public void establishConnection(BluetoothDevice device) {
        UUID uuid = this.getDeviceUUID(device);

        this.mActivity.log("Establishing connection with device " + device.getName() + " and address " + device.getAddress() + " and UUID " + uuid.toString());
        try {
            mSocket = device.createRfcommSocketToServiceRecord(uuid);
            mSocket.connect();
        } catch (IOException e) {
            this.mActivity.log("Cannot establish connection - error ocurred " + e.toString());
            return;
        }
        this.mActivity.log("Bluetooth connection is established");

    }


    public List<String> getPairedDevicesAsString() {
        BluetoothAdapter adapter = this.getBluetoothAdapter();
        Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
        List<String> result = new ArrayList<String>();
        result.add("Number of paired devices: " + pairedDevices.size());
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                result.add(device.getName() + " - " + device.getAddress());
            }
        }
        return result;
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String discoveredDeviceName = device.getName();

                if (mDeviceName.equals(discoveredDeviceName)) {
                    mActivity.log("Device " + discoveredDeviceName + " found");
                } else {
                    mActivity.log("Device " + discoveredDeviceName + " found but does not match " + mDeviceName);
                }
            }
        }
    };


    public void destroy() {
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }
        if (this.mSocket != null) {
            try {
                this.mSocket.close();
            } catch (IOException ex) {
            }
        }
        mActivity.unregisterReceiver(mReceiver);
    }


}