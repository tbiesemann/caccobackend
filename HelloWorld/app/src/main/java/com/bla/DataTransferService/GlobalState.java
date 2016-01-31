package com.bla.DataTransferService;


import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Message;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class GlobalState {

    public String version = "v0.1";
    public BlockingQueue<String> mMessageQueue;
    public BluetoothUtilities bluetoothUtilities;
    public Settings settings;
    boolean isGdriveInitialized = false;
    public GDriveUtilities driveUtilities;
    private Activity activity;
    private Handler handler;
    private ILogger consoleLogger;
    private Thread mGDriveWriterThread;

    public GlobalState() {


        this.mMessageQueue = new LinkedBlockingQueue<String>();


        this.handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String text = msg.obj.toString();
                //Log to main activity
                consoleLogger.log(text);

                //Log to gdrive
                if (isGdriveInitialized) {
                    try {
                        driveUtilities.appendToLogFile(text);
                    } catch (Exception ex) {

                    }
                }
                super.handleMessage(msg);
            }
        };
    }


    private static GlobalState instance;


    public void start() {


        //Write test data
        if (mGDriveWriterThread != null) {
            log("Already started - writing test data instead.... To be removed from productive code");
            String now = android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss", new java.util.Date()).toString();
            try {
                GlobalState.getInstance().mMessageQueue.put("Test content for GDrive" + now + "\n");
            } catch (InterruptedException ex) {
                log("Upps - error writing test data");
            }
            return;
        }


        log("Starting Aqua " + version);
        log("Connecting to GDrive...");
        GlobalState.getInstance().driveUtilities.connect();
    }

    static public GlobalState getInstance() {
        if (instance == null) {
            instance = new GlobalState();
        }
        return instance;
    }


    public void setActivity(Activity activity, ILogger logger) throws Exception {
        this.consoleLogger = logger;
        this.activity = activity;
        if (settings != null) {
            settings.destroy();
        }
        if (driveUtilities != null) {
            driveUtilities.destroy();
        }
        if (bluetoothUtilities != null) {
            bluetoothUtilities.destroy();
        }
        this.settings = new Settings(activity);
        this.bluetoothUtilities = new BluetoothUtilities();
        this.driveUtilities = new GDriveUtilities(activity);
        this.driveUtilities.registerConnectCompletedEventHandler(new GDriveUtilities.IconnectCompletedEventHandler() {
            @Override
            public void handle() {
                isGdriveInitialized = true;
                setupBluetoothAndLinkWithGdrive();
            }
        });
    }


    private void setupBluetoothAndLinkWithGdrive() {
        setupBluetooth();
    }

    private void setupBluetooth() {
        log("Opening bluetooth...");

        new Thread(new Runnable() {
            @Override
            public void run() {

                boolean success = bluetoothUtilities.establishConnection();
                if (success) {
                    onBluetoothSuccess();
                } else {
                    try {
                        log("Waiting 20 seconds before retry");
                        Thread.sleep(20000);
                    } catch (InterruptedException ex) {
                    }
                    setupBluetooth();
                }
            }
        }).start();
    }

    private void onBluetoothSuccess() {
        log("Done opening bluetooth connection");
        connectGDriveWithBluetooth();
    }


    public void onBluetoothDeviceDisconnected(BluetoothDevice device) {

    }


    public void log(String text) { //Can be called from any Thread
        Message msg = Message.obtain();
        msg.obj = text;
        this.handler.sendMessage(msg);
    }


    public void handleIncomingData(String data) { //Can be called from any thread
        try {
            this.mMessageQueue.put(data);
        } catch (InterruptedException ex) {
            log("Error writing onto queue:" + ex.toString());
        }
    }


    public void connectGDriveWithBluetooth() {
        log("Connecting bluetooth with google drive..");
        if (mGDriveWriterThread != null) {
            log("Error: Worker thread is already started.....");
            return;
        } else {
            GDriveRunnable gDriveRunnable = new GDriveRunnable(driveUtilities, mMessageQueue);
            mGDriveWriterThread = new Thread(gDriveRunnable);
            mGDriveWriterThread.start();
            log("Aqua is up and running...");
        }
    }


    public void handleOnMainActivityResult(final int requestCode, final int resultCode) {
        if (this.driveUtilities != null) {
            this.driveUtilities.handleOnMainActivityResult(requestCode, resultCode);
        }
    }


}
