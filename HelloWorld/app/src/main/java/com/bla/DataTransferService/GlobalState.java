package com.bla.DataTransferService;


import android.app.Activity;
import android.os.Handler;
import android.os.Message;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class GlobalState {

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
                consoleLogger.log(text);
                if (isGdriveInitialized) {
                    try {
                        driveUtilities.appendToLogFile(text);
                    } catch (Exception ex) {
                        if (isGdriveInitialized) {
                            consoleLogger.log("Cannot write log to gdrive");
                        }
                    }
                }
                super.handleMessage(msg);
            }
        };
    }


    private static GlobalState instance;

    static public GlobalState getInstance() {
        if (instance == null) {
            instance = new GlobalState();
        }
        return instance;
    }


    public void setActivity(Activity activity, ILogger logger) throws Exception {
        this.consoleLogger = logger;
        this.activity = activity;
        this.settings = new Settings(activity);
        this.bluetoothUtilities = new BluetoothUtilities();
        this.driveUtilities = new GDriveUtilities(activity);
        this.driveUtilities.registerConnectCompletedEventHandler(new GDriveUtilities.IconnectCompletedEventHandler() {
            @Override
            public void handle() {
                isGdriveInitialized = true;
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


    public void start() {
        if (isGdriveInitialized != true) {
            log("Error: Cannot start - GDrive is not yet initialized!");
            return;
        }

        if (mGDriveWriterThread != null) {
            log("Error: Already started.....");
            String now = android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss", new java.util.Date()).toString();
            try {
                GlobalState.getInstance().mMessageQueue.put("Test content for GDrive" + now + "\n");
            } catch (InterruptedException ex) {
                log("Upps - error writing test data");
            }
            return;
        } else {
            log("Establish connection between bluetooth and GDrive...");
            GDriveRunnable gDriveRunnable = new GDriveRunnable(driveUtilities, mMessageQueue);
            mGDriveWriterThread = new Thread(gDriveRunnable);
            mGDriveWriterThread.start();
        }
    }


}
