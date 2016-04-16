package com.bla.DataTransferService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class PlugInControlReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action.equals(Intent.ACTION_POWER_CONNECTED)) {
            AquaService service = AquaService.getInstance();
            if (service != null) {
                service.log("Warning: power is connected again");
            }

        } else if (action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
            AquaService service = AquaService.getInstance();
            if (service != null) {
                AquaService.getInstance().log("Warning: power was disconnected");
            }
        }
    }
}
