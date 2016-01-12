package com.bla.DataTransferService;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;


interface ILogger {
    void onLog(String msg);
}

public class BluetoothUtilities {

    BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mDevice;
    private BluetoothSocket mSocket;
    private OutputStream mOutputStream;
    private InputStream mInputStream;


    ILogger logger;
    public void setLogger(ILogger listener){
        logger = listener;
    }

    private void log(String text){
        if (this.logger != null){
            this.logger.onLog(text);
        }
    }

    public BluetoothUtilities() {
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
            this.log("Enabling Bluetooth adapter...");
            adapter.enable();
        }
        this.log("Bluetooth adapter is enabled");
    }


    public UUID getDeviceUUID(BluetoothDevice device) {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        return uuid;
    }




    public void establishConnection(String deviceName) {
        BluetoothDevice device;
        device = this.getDeviceByName(deviceName);
        if (device == null) {
            this.log("Cannot find a device with name " + deviceName);
            return;
        }
        this.log("Device " + deviceName + " found");


        UUID uuid = this.getDeviceUUID(device);


        try {
            this.log("Establishing connection with device " + device.getName() + " and address " + device.getAddress() + " and UUID " + uuid.toString());
            mSocket = device.createRfcommSocketToServiceRecord(uuid);
            mSocket.connect();
        } catch (IOException e) {
            this.log("Cannot establish connection - error ocurred " + e.toString());
            return;
        }

        this.log("Bluetooth connection is established");

        try {
            mOutputStream = mSocket.getOutputStream();
            mInputStream = mSocket.getInputStream();
        } catch (IOException e) {
            this.log("Error getting streams..." + e.toString());
        }

        //  beginListenForData();
    }


    public void sendData(String data) throws IOException {
        if (mOutputStream == null){
            this.log("Cannot send data - connection is not established");
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



    public void destroy() {

        if (this.mSocket != null) {
            try {
                this.mSocket.close();
            } catch (IOException ex) {
            }
        }

    }


}
