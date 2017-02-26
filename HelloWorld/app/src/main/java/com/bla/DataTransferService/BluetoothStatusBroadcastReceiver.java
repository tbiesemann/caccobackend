package com.bla.DataTransferService;


import android.bluetooth.BluetoothAdapter;
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

        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            service.log("Bluetooth Intent Received: " + action.substring(32));  //Cut off namespace from intent (33 chars)
            service.log("A device was connected via bluetooth");
        }
        else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            service.log("Bluetooth Intent Received: " + action.substring(32));  //Cut off namespace from intent (33 chars)
            service.log("A device was disconnected via bluetooth");
            service.onBluetoothDeviceDisconnected(device);
        } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            service.log("Bluetooth Intent Received: " + action);

            int previousState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, 42);
            service.log("Previous state: " + getStateAsString(previousState));

            int newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 42);
            service.log("New state: " + getStateAsString(newState));


            if (newState == BluetoothAdapter.STATE_OFF) {
                service.onBluetoothDeviceDisconnected(device);
            }

        } else {
            service.log("Unexpected Bluetooth Intent Received: " + action);
        }
    }


    private String getStateAsString(int state){
        if (state == BluetoothAdapter.STATE_OFF){
            return "OFF";
        } else if (state == BluetoothAdapter.STATE_ON){
            return "ON";
        } else if (state == BluetoothAdapter.STATE_TURNING_ON){
            return "TURNING_ON";
        } else if (state == BluetoothAdapter.STATE_TURNING_OFF){
            return "TURNING_OFF";
        }
        return "Unknown state" + state;
    }
}