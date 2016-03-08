package com.bla.DataTransferService;

import android.app.Service;
import android.content.Intent;
import android.nfc.Tag;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.drive.events.ChangeEvent;
import com.google.android.gms.drive.events.CompletionEvent;
import com.google.android.gms.drive.events.DriveEventService;

import java.util.List;

public class DriveNotificationService extends DriveEventService {
    @Override
    public void onChange(ChangeEvent event){
        Log.d("DriveNotifService", "Change Event: " + event);
    }
    @Override
    public void onCompletion(CompletionEvent event){

        List<String> tags = event.getTrackingTags();
        String tag;
        if (tags.size() > 0){
            tag = tags.get(0);
        } else {
            tag = "someTag";
        }
        Log.d(tag, "Completion Event: " + event);
        if (event.getStatus() == CompletionEvent.STATUS_SUCCESS) {
            Log.d(tag, "Success");
        } else if (event.getStatus() == CompletionEvent.STATUS_CANCELED) {
            Log.d(tag, "Canceled");
        } else if (event.getStatus() == CompletionEvent.STATUS_CONFLICT) {
            Log.d(tag, "Conflict");
        } else if (event.getStatus() == CompletionEvent.STATUS_FAILURE) {
            Log.d(tag, "Failure");
        }

        event.dismiss();
    }
}
