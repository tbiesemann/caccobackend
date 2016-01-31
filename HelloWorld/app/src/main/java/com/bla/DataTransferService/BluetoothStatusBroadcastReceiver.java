package com.bla.DataTransferService;


import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BluetoothStatusBroadcastReceiver extends BroadcastReceiver {

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        GlobalState.getInstance().log("Bluetooth Intent Recieved:" + action);

        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)){
            GlobalState.getInstance().log("A device was connected via bluetooth");
        }

        if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)){
            GlobalState.getInstance().log("A device was disconnected via bluetooth");
        }
    }
}