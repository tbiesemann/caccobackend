package com.bla.DataTransferService;

import android.app.Activity;
import android.content.SharedPreferences;


public class Settings {
    private Activity activity;
    public Settings(Activity activity){
        this.activity = activity;
    }


    public String getLocation(){
        SharedPreferences settings = activity.getSharedPreferences("DataTransferService", Activity.MODE_PRIVATE);
        String locationName = settings.getString("locationID", "Regenbecken42");
        return locationName;
    }
}
