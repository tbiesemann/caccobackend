package com.bla.DataTransferService;


import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BluetoothStatusBroadcastReceiver extends BroadcastReceiver {

    public void onReceive(Context context, Intent intent) {
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        String action = intent.getAction();
        AquaService service = AquaService.getInstance();
        if (service == null) {
            return;
        }

        service.log("Bluetooth Intent Received: " + action.substring(33));  //Cut off namespace from intent (33 chars)
        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            service.log("A device was connected via bluetooth");
        }
        if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            service.log("A device was disconnected via bluetooth");
            service.onBluetoothDeviceDisconnected(device);
        }
    }
}