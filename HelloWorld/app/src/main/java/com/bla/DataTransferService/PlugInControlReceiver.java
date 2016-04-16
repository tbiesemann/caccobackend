package com.bla.DataTransferService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class PlugInControlReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        AquaService service = AquaService.getInstance();
        if (service == null) {
            return;
        }

        if (action.equals(Intent.ACTION_POWER_CONNECTED)) {
            service.log("Warning: power is connected again");
        } else if (action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
            service.log("Warning: power was disconnected");
        }
    }
}
