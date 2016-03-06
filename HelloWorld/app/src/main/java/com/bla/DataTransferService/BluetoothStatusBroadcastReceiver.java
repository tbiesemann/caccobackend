package com.bla.DataTransferService;


import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BluetoothStatusBroadcastReceiver extends BroadcastReceiver {

    public void onReceive(Context context, Intent intent) {
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);


        String action = intent.getAction();
        AquaService.getInstance().log("Bluetooth Intent Received:" + action);

        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)){
            AquaService.getInstance().log("A device was connected via bluetooth");
        }

        if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)){
            AquaService.getInstance().log("A device was disconnected via bluetooth");
            AquaService.getInstance().onBluetoothDeviceDisconnected(device);
        }
    }
}