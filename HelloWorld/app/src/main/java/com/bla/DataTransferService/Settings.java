package com.bla.DataTransferService;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;


public class Settings {
    private SharedPreferences settings;

    public Settings(Context context){
        this.settings = context.getSharedPreferences("DataTransferService", Activity.MODE_PRIVATE);

    }


    public String getLocation(){
        String locationName = settings.getString("locationID", "Regenbecken42");
        return locationName;
    }


    public boolean getUseWindowsLineEndings(){
        boolean useWindowsLineEndings = settings.getBoolean("useWindowsLineEndings", false);
        return useWindowsLineEndings;
    }

    public String getDeviceName(){
        return settings.getString("deviceName", "HC-06");
    }

    public Integer getGDriveSyncIntervall(){
        return settings.getInt("GDriveSyncIntervall", 3);
    }


    public void destroy(){

    }

}
