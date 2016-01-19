package com.bla.DataTransferService;


import android.app.Activity;
import android.content.IntentSender;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;

public class GDriveUtilities implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    Activity mActivity;
    private GoogleApiClient mGoogleApiClient;
    private ILogger logger;
    private String mLocationName;

    public static final int REQUEST_CODE_RESOLUTION = 42;


    public GDriveUtilities(Activity activity, String locationName) throws Exception{
        if (activity == null){
            throw new Exception("Mandatory activity missing");
        }
        this.mLocationName = locationName;
        this.mActivity = activity;
        mGoogleApiClient = new GoogleApiClient.Builder(this.mActivity)
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
                this.log("Info: Connection failed, user needs to sign in...");
                connectionResult.startResolutionForResult(this.mActivity, REQUEST_CODE_RESOLUTION);
            } catch (IntentSender.SendIntentException e) {
                // Unable to resolve, message user appropriately
                this.log("Error: Something with GDrive went wrong.....");
            }
        } else {
            this.log("Error: Cannot connect to GDrive ad no error resolution possible");
        }
    }




    @Override
    public void onConnected(Bundle connectionHint) {
        this.log("GDrive connected");
    }


    @Override
    public void onConnectionSuspended(int cause) {
        this.log("GDrive connection suspended");
    }

    public void destroy() {

    }


    DriveFolder mAquaFolder;
    DriveFolder mWorkingDirectory;

    public void initWorkingDirectory(){
        DriveFolder rootFolder = Drive.DriveApi.getRootFolder(this.mGoogleApiClient);
        rootFolder.listChildren(this.mGoogleApiClient).setResultCallback(rootChildrenRetrievedCallback);
        //Aqua folder does not exist, create it
        if(this.mAquaFolder == null) {
            MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle("Aqua").build();
            rootFolder.createFolder(this.mGoogleApiClient, changeSet).setResultCallback(AquaFolderCreatedCallback);
        }

        if(this.mAquaFolder == null) {
            log("Error - no aqua folder - crap...");
            return;
        }

        mAquaFolder.listChildren(this.mGoogleApiClient).setResultCallback(aquaChildrenRetrievedCallback);
        if(this.mWorkingDirectory == null){
            MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle(mLocationName).build();
            mAquaFolder.createFolder(this.mGoogleApiClient, changeSet).setResultCallback(WorkingDirectoryCreatedCallback);

        }

    }


    ResultCallback<DriveApi.MetadataBufferResult> aquaChildrenRetrievedCallback = new ResultCallback<DriveApi.MetadataBufferResult>() {
        @Override
        public void onResult(DriveApi.MetadataBufferResult result) {
            if (!result.getStatus().isSuccess()) {
                log("Problem reading children of root directory");
                return;
            }
            int length = result.getMetadataBuffer().getCount();
            for (int i = 0; i < length; i++) {
                Metadata metadata = result.getMetadataBuffer().get(i);
                if (metadata.isFolder() && metadata.getOriginalFilename().equals(mLocationName)){
                    DriveId workingDirectoryDriveId = metadata.getDriveId();
                    mWorkingDirectory = workingDirectoryDriveId.asDriveFolder();
                    log("Folder '" + mLocationName + "' already exists");
                    return;
                }
            }
            log("Folder '" + mLocationName + "' does not exist...Trying to create it");
        }
    };



    ResultCallback<DriveFolder.DriveFolderResult> WorkingDirectoryCreatedCallback = new ResultCallback<DriveFolder.DriveFolderResult>() {
        @Override
        public void onResult(DriveFolder.DriveFolderResult result) {
            if (!result.getStatus().isSuccess()) {
                log("Cannot create '" + mLocationName + "' folder in GDrive");
                return;
            }
            log("Folder '" + mLocationName + "' created in GDrive");
            mWorkingDirectory = result.getDriveFolder();
        }
    };


    ResultCallback<DriveApi.MetadataBufferResult> rootChildrenRetrievedCallback = new ResultCallback<DriveApi.MetadataBufferResult>() {
        @Override
        public void onResult(DriveApi.MetadataBufferResult result) {
            if (!result.getStatus().isSuccess()) {
                log("Problem reading children of root directory");
                return;
            }
            int length = result.getMetadataBuffer().getCount();
            for (int i = 0; i < length; i++) {
                Metadata metadata = result.getMetadataBuffer().get(i);
                if (metadata.isFolder() && metadata.getOriginalFilename().equals("Aqua")){
                    DriveId mAquaDriveId = metadata.getDriveId();
                    mAquaFolder = mAquaDriveId.asDriveFolder();
                    log("Successfully found 'Aqua' folder");
                    return;
                }
            }
            log("Aqua folder does not exist...Trying to create it");
        }
    };



    ResultCallback<DriveFolder.DriveFolderResult> AquaFolderCreatedCallback = new ResultCallback<DriveFolder.DriveFolderResult>() {
                @Override
                public void onResult(DriveFolder.DriveFolderResult result) {
                    if (!result.getStatus().isSuccess()) {
                        log("Cannot create 'Aqua' folder in GDrive");
                        return;
                    }
                    log("Aqua folder created in GDrive");
                    mAquaFolder = result.getDriveFolder();
                }
            };
}


