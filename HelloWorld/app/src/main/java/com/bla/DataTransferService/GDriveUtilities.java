package com.bla.DataTransferService;


import android.app.Activity;
import android.content.IntentSender;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;

public class GDriveUtilities implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    Activity _mActivity;
    private GoogleApiClient mGoogleApiClient;
    private ILogger logger;

    public static final int REQUEST_CODE_RESOLUTION = 1;


    public GDriveUtilities(Activity activity) throws Exception{
        if (activity == null){
            throw new Exception("Mandatory activity missing");
        }
        this._mActivity = activity;
        mGoogleApiClient = new GoogleApiClient.Builder(this._mActivity)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }



    public void connect(){
        mGoogleApiClient.connect();
    }




    public void setLogger(ILogger listener) {
        logger = listener;
    }

    private void log(String text) {
        if (this.logger != null) {
            this.logger.onLog(text);
        }
    }


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                this.log("Info: Connection failed, user needs o sign in...");
                connectionResult.startResolutionForResult(this._mActivity, REQUEST_CODE_RESOLUTION);
            } catch (IntentSender.SendIntentException e) {
                // Unable to resolve, message user appropriately
                this.log("Error: Something with GDrive went wrong.....");
            }
        } else {
            this.log("Error: Cannot connect to GDive ad no error resolution possible");
        }
    }




    @Override
    public void onConnected(Bundle connectionHint) {
        this.log("GoogleApiClient connected");
    }


    @Override
    public void onConnectionSuspended(int cause) {
        this.log("GoogleApiClient connection suspended");
    }

    public void destroy() {

    }
}


