package com.bla.DataTransferService;


import android.app.Activity;
import android.content.SharedPreferences;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class GlobalState {

    public BlockingQueue<String> mMessageQueue;
    public BluetoothUtilities bluetoothUtilities;
    public Settings settings;
//    GDriveUtilities driveUtilities;
    private Activity activity;

    public GlobalState(){
        this.bluetoothUtilities = new BluetoothUtilities();
        this.mMessageQueue =  new LinkedBlockingQueue<String>();

//
//        SharedPreferences settings = activity.getSharedPreferences("DataTransferService", Activity.MODE_PRIVATE);
//        String locationName = settings.getString("locationID", "Regenbecken42");
//
//        utils = new GDriveUtilities(activity, locationName);

    }

    private static GlobalState instance;
    static public GlobalState getInstance(){
        if (instance == null){
            instance = new GlobalState();
        }
        return instance;
    }


    public void setActivity(Activity activity){
        this.activity = activity;
        this.settings = new Settings(activity);
    }
}
