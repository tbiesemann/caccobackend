package com.bla.DataTransferService;


import android.app.Activity;
import android.content.SharedPreferences;

public class GDriveUtilitiesFactory {

    private static GDriveUtilities utils;

    public static GDriveUtilities createGDriveUtilities(Activity activity) throws Exception{
        if (utils == null){

            utils = new GDriveUtilities(activity);
        }
        return utils;
    }
    public static GDriveUtilities getGDriveUtilities(){
        return utils;
    }
}