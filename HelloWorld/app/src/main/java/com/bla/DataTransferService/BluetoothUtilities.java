package com.bla.DataTransferService;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.ParcelUuid;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
    private OutputStream mOutputStream;
    private InputStream mInputStream;

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


    public UUID getDeviceUUID(BluetoothDevice device) {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        return uuid;
    }


    public void establishConnection(String deviceName) {
        BluetoothDevice device;
        device = this.getDeviceByName(deviceName);
        if (device == null) {
            this.mActivity.log("Cannot find a device with name " + deviceName);
            return;
        }
        this.mActivity.log("Device " + deviceName + " found");


        UUID uuid = this.getDeviceUUID(device);


        try {
            this.mActivity.log("Establishing connection with device " + device.getName() + " and address " + device.getAddress() + " and UUID " + uuid.toString());
            mSocket = device.createRfcommSocketToServiceRecord(uuid);
            mSocket.connect();
        } catch (IOException e) {
            this.mActivity.log("Cannot establish connection - error ocurred " + e.toString());
            return;
        }

        this.mActivity.log("Bluetooth connection is established");

        try {
            mOutputStream = mSocket.getOutputStream();
            mInputStream = mSocket.getInputStream();
        } catch (IOException e) {
            this.mActivity.log("Error getting streams..." + e.toString());
        }

        //  beginListenForData();
    }


    public void sendData(String data) throws IOException {
        if (mOutputStream == null){
            this.mActivity.log("Cannot send data - connection is not established");
            return;
        }
        String msg = data + "\n";
        mOutputStream.write(msg.getBytes());
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


    public BluetoothDevice getDeviceByName(String expectedDeviceName) {
        BluetoothAdapter adapter = this.getBluetoothAdapter();
        Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                if (expectedDeviceName.equals(deviceName)) {
                    return device;
                }
            }
        }
        return null;
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
