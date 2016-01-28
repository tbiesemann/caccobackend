package com.bla.DataTransferService;


import android.app.Activity;
import android.content.SharedPreferences;

public class GlobalState {

    public BluetoothUtilities bluetoothUtilities;
//    GDriveUtilities driveUtilities;

    public GlobalState(){
        this.bluetoothUtilities = new BluetoothUtilities();
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
}
