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
    //    boolean isGdriveInitialized = false;
//    GDriveUtilities driveUtilities;
    private Activity activity;
    private Handler handler;
    private ILogger consoleLogger;


    public GlobalState() {
        this.bluetoothUtilities = new BluetoothUtilities();
        this.mMessageQueue = new LinkedBlockingQueue<String>();


        this.handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String text = msg.obj.toString();
                consoleLogger.log(text);
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


    public void setActivity(Activity activity, ILogger logger) {
        this.consoleLogger = logger;
        this.activity = activity;
        this.settings = new Settings(activity);
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


}
