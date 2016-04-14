package com.bla.DataTransferService;


import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DrivePreferencesApi;
import com.google.android.gms.drive.FileUploadPreferences;


public class GDriveSignIn {

    private Context mContext;
    private Activity mActivity;
    private GoogleApiClient mGoogleApiClient;

    private IGDriveSignInCompletedEventHandler mSignInCompletedHandler;
    public static final int REQUEST_CODE_RESOLUTION = 42;


    public GDriveSignIn(Activity activity, Context context, IGDriveSignInCompletedEventHandler handler) throws Exception {
        if (activity == null) {
            throw new Exception("Mandatory activity missing");
        }

        this.mActivity = activity;
        this.mContext = context;
        this.mSignInCompletedHandler = handler;

        mGoogleApiClient = new GoogleApiClient.Builder(this.mContext)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .build();

        this.checkDrivePreferences();

        this.connect();
    }


    private void checkDrivePreferences() {
        Drive.DrivePreferencesApi.getFileUploadPreferences(mGoogleApiClient).setResultCallback(new ResultCallback<DrivePreferencesApi.FileUploadPreferencesResult>() {
            @Override
            public void onResult(DrivePreferencesApi.FileUploadPreferencesResult result) {
                FileUploadPreferences prefs = result.getFileUploadPreferences();
                int battery = prefs.getBatteryUsagePreference();
                if (battery != FileUploadPreferences.BATTERY_USAGE_UNRESTRICTED) {
                    prefs.setBatteryUsagePreference(FileUploadPreferences.BATTERY_USAGE_UNRESTRICTED);
                }
                int networkType = prefs.getNetworkTypePreference();
                if (networkType != FileUploadPreferences.NETWORK_TYPE_ANY) {
                    prefs.setNetworkTypePreference(FileUploadPreferences.NETWORK_TYPE_ANY);
                }
                boolean roaming = prefs.isRoamingAllowed();
                if (roaming != true) {
                    prefs.setRoamingAllowed(true);
                }
            }
        });
    }


    public GoogleApiClient getGDriveAPI() {
        return this.mGoogleApiClient;
    }

    public interface IGDriveSignInCompletedEventHandler {
        void handle();
    }

    private void raiseSignInCompletedEvent() {
        mSignInCompletedHandler.handle();
    }


    private void connect() {
        Thread backgroundThread = new Thread(new Runnable() {
            public void run() {
                if (mGoogleApiClient.isConnected()) {
                    log("GDrive is already connected");
                    return;
                }

                ConnectionResult connectionResult = mGoogleApiClient.blockingConnect();
                if (connectionResult.isSuccess()) {
                    log("GDrive connected");
                    raiseSignInCompletedEvent();
                } else if (connectionResult.hasResolution()) {
                    try {
                        log("Info: Connection failed, user needs to sign in...");
                        connectionResult.startResolutionForResult(mActivity, REQUEST_CODE_RESOLUTION);
                    } catch (IntentSender.SendIntentException e) {
                        // Unable to resolve
                        log("Error: Something with GDrive went wrong.....");
                    }
                } else {
                    log("Error: Cannot connect to GDrive and no error resolution possible");
                }

            }
        });
        backgroundThread.start();
    }


    private void log(String text) {
        AquaService.getInstance().log(text);
    }


    public void handleOnMainActivityResult(final int requestCode, final int resultCode) {
        switch (requestCode) {
            case GDriveSignIn.REQUEST_CODE_RESOLUTION:
                if (resultCode == Activity.RESULT_OK) {
                    log("Trying to connect after sign in");
                    connect();
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    log("Sign in failed - cancelled");
                } else {
                    log("Sign in failed!");
                }
                break;
        }
    }


    public void destroy() {
        this.mGoogleApiClient.disconnect();
        this.mGoogleApiClient = null;
        this.mContext = null;
        this.mActivity = null;
    }

}


