package com.bla.DataTransferService;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public class BluetoothUtilities {

    BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mSocket;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private BluetoothDevice mDevice;
    public BluetoothUtilities() {
        this.log("Creating bluetooth utilities");
    }


    private void log(String text) {
        AquaService.getInstance().log(text);
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


    public boolean establishConnection() {

        //Make sure bluetooth is turned on
        enableBluetoothAdapter();

        String deviceName = AquaService.getInstance().settings.getDeviceName();

        mDevice = this.getDeviceByName(deviceName);
        if (mDevice == null) {
            this.log("Cannot find a device with name " + deviceName);
            return false;
        }
        this.log("Device " + deviceName + " found");


        UUID uuid = this.getDeviceUUID(mDevice);


        try {
            this.log("Establishing connection with device " + mDevice.getName() + " and address " + mDevice.getAddress() + " and UUID " + uuid.toString());
            mSocket = mDevice.createRfcommSocketToServiceRecord(uuid);
            mSocket.connect();
        } catch (IOException e) {
            this.log("ERROR: Cannot establish bluetooth connection - " + e.toString());
            return false;
        }

        try {
            mOutputStream = mSocket.getOutputStream();
            mInputStream = mSocket.getInputStream();
        } catch (IOException e) {
            this.log("Error getting streams..." + e.toString());
            return false;
        }

        this.log("Bluetooth connection is established");

        beginListenForData();
        return true;
    }


    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;

    void beginListenForData() {
        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                    try {
                        int bytesAvailable = mInputStream.available();
                        if (bytesAvailable > 0) {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mInputStream.read(packetBytes);

                            //log(convertBytesToString(packetBytes)); //for analysis of missing bytes

                            for (int i = 0; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];
                                boolean useWindowsLineEndings = AquaService.getInstance().settings.getUseWindowsLineEndings();

                                if (((!useWindowsLineEndings) && (b == 10)) || ((useWindowsLineEndings) && (i < bytesAvailable - 1) && (b == 13) && (packetBytes[i + 1] == 10))) {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
                                    log("" + data.length() + " bytes");
                                    AquaService.getInstance().handleIncomingData(data);
                                } else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    } catch (IOException ex) {
                        stopWorker = true;
                    }
                }
            }
        });
        workerThread.start();
    }


    public String convertBytesToString(byte[] data){
        String text = "";
        try {
            text = new String(data, "US-ASCII");
        } catch (UnsupportedEncodingException ex){
            log("Error converting to ascii");
        }
        return text;
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
        stopWorker = true;

        if (this.mOutputStream != null) {
            try {
                this.mOutputStream.close();
            } catch (IOException ex) {
            }
        }

        if (this.mInputStream != null) {
            try {
                mInputStream.close();
            } catch (IOException ex) {
            }
        }

        if (this.mSocket != null) {
            try {
                this.mSocket.close();
            } catch (IOException ex) {
            }
        }

        this.mBluetoothAdapter = null;
        this.mDevice = null;

        this.mDevice = null;
    }


}
