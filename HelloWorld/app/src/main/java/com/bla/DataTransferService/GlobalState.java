package com.bla.DataTransferService;


import android.app.Activity;
import android.content.Intent;
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
    private boolean startUpFailed = false;
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


    public void startEverything() {


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

                if (!startUpFailed) {
                    log("Opening bluetooth...");
                    bluetoothUtilities.establishConnection();  //Gdrive is initialized, start initializing bluetooth
                    if (!startUpFailed) {
                        log("Done opening bluetooth connection");
                        log("Connection bluetooth and google drive");
                        connectGDriveAndBluetooth();
                    }
                }
            }
        });
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


    public void connectGDriveAndBluetooth() {
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


    public void setStartUpTpFailure() {
        this.startUpFailed = true;
    }


}
