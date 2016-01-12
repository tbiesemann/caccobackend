package com.bla.DataTransferService;

public class BluetoothUtilitiesFactory {
    private static BluetoothUtilities utils;
    public static BluetoothUtilities getBluetoothUtilities(){
        if (utils == null){
            utils = new BluetoothUtilities();
        }
        return utils;
    }
}
