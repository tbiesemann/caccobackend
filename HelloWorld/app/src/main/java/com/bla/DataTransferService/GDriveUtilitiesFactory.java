package com.bla.DataTransferService;


import android.app.Activity;
import android.content.SharedPreferences;

public class GDriveUtilitiesFactory {

    private static GDriveUtilities utils;

    public static GDriveUtilities createGDriveUtilities(Activity activity) throws Exception{
        if (utils == null){
            SharedPreferences settings = activity.getSharedPreferences("DataTransferService", Activity.MODE_PRIVATE);
            String locationName = settings.getString("locationID", "Regenbecken42");

            utils = new GDriveUtilities(activity, locationName);
        }
        return utils;
    }
    public static GDriveUtilities getGDriveUtilities(){
        return utils;
    }
}