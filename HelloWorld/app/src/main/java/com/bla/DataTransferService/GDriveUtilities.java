package com.bla.DataTransferService;


import android.app.Activity;
import android.content.IntentSender;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;

public class GDriveUtilities implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    final String mLogFileName = "log.txt";
    Activity mActivity;
    private GoogleApiClient mGoogleApiClient;
    private ILogger logger;
    private String mLocationName;
    private CurrentFile mCurrentFile;
    private DriveFile mLogFile;


    public static final int REQUEST_CODE_RESOLUTION = 42;


    public GDriveUtilities(Activity activity, String locationName) throws Exception {
        if (activity == null) {
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


    public void connect() {
        if(mGoogleApiClient.isConnected()){
            log("GDrive is already connected");
            return;
        }
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
            this.log("Error: Cannot connect to GDrive and no error resolution possible");
        }
    }


    private class CurrentFile {
        public CurrentFile(String fileName, DriveFile file) {
            this.fileName = fileName;
            this.file = file;
        }

        public String fileName;
        public DriveFile file;
    }




    public void appendToFile(String fileName, String data) {

        if (mCurrentFile != null && mCurrentFile.fileName == fileName) {
            this.appendToFile(mCurrentFile.file, data);
        } else {
            mCurrentFile = new CurrentFile(fileName, null);
            mWorkingDirectory.listChildren(this.mGoogleApiClient).setResultCallback(workingDirectoryChildrenRetrievedForDataFileCallback);
        }
    }


    ResultCallback<DriveApi.MetadataBufferResult> workingDirectoryChildrenRetrievedForDataFileCallback = new ResultCallback<DriveApi.MetadataBufferResult>() {
        @Override
        public void onResult(DriveApi.MetadataBufferResult result) {
            if (!result.getStatus().isSuccess()) {
                log("Problem reading children of working directory");
                return;
            }
            int length = result.getMetadataBuffer().getCount();
            for (int i = 0; i < length; i++) {
                Metadata metadata = result.getMetadataBuffer().get(i);
                if (metadata.isFolder() && metadata.getTitle() != null && metadata.getTitle().equals(mCurrentFile.fileName)) {
                    DriveId fileDriveId = metadata.getDriveId();
                    mCurrentFile.file = fileDriveId.asDriveFile();
                    log("Successfully found " + mCurrentFile.fileName + " file");
                    return;
                }
            }

            log("File '" + mCurrentFile + "' does not exist...Trying to create it");

            MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle(mCurrentFile.fileName).setMimeType("text/plain").build();
            mWorkingDirectory.createFile(mGoogleApiClient, changeSet, null).setResultCallback(createEmptyDataFileCallback);

        }
    };


    ResultCallback<DriveFolder.DriveFileResult> createEmptyDataFileCallback = new ResultCallback<DriveFolder.DriveFileResult>() {
        @Override
        public void onResult(DriveFolder.DriveFileResult result) {
            if (!result.getStatus().isSuccess()) {
                log("Problem creating empty file'" + mCurrentFile.fileName + "'");
                return;
            } else {
                log("File created'" + mCurrentFile.fileName + "'");
            }

        }
    };


    private void appendToFile(DriveFile file, String data) {

    }


    @Override
    public void onConnected(Bundle connectionHint) {
        this.log("GDrive connected");
        this.initWorkingDirectory();
    }


    @Override
    public void onConnectionSuspended(int cause) {
        this.log("GDrive connection suspended");
    }

    public void destroy() {

    }


    DriveFolder mRootFolder;
    DriveFolder mAquaFolder;
    DriveFolder mWorkingDirectory;

    public void initWorkingDirectory() {
        mRootFolder = Drive.DriveApi.getRootFolder(this.mGoogleApiClient);
        mRootFolder.listChildren(this.mGoogleApiClient).setResultCallback(rootChildrenRetrievedCallback);
    }





    ResultCallback<DriveApi.MetadataBufferResult> aquaChildrenRetrievedCallback = new ResultCallback<DriveApi.MetadataBufferResult>() {
        @Override
        public void onResult(DriveApi.MetadataBufferResult result) {
            if (!result.getStatus().isSuccess()) {
                log("Problem reading children of Aqua directory");
                return;
            }
            int length = result.getMetadataBuffer().getCount();
            for (int i = 0; i < length; i++) {
                Metadata metadata = result.getMetadataBuffer().get(i);
                if (metadata.isFolder() && metadata.getTitle() != null && metadata.getTitle().equals(mLocationName)) {
                    DriveId workingDirectoryDriveId = metadata.getDriveId();
                    mWorkingDirectory = workingDirectoryDriveId.asDriveFolder();
                    log("Folder '" + mLocationName + "' already exists");

                    log("Looking for an existing log file...");
                    mWorkingDirectory.listChildren(mGoogleApiClient).setResultCallback(workingDirectoryChildrenRetrievedForLogFileCallback);
                    return;
                }
            }
            log("Folder '" + mLocationName + "' does not exist...Trying to create it");
            MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle(mLocationName).build();
            mAquaFolder.createFolder(mGoogleApiClient, changeSet).setResultCallback(WorkingDirectoryCreatedCallback);

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


            log("Creating log file...");
            MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle(mLogFileName).setMimeType("text/plain").build();
            mWorkingDirectory.createFile(mGoogleApiClient, changeSet, null).setResultCallback(createEmptyLogFileCallback);

        }
    };


    ResultCallback<DriveApi.MetadataBufferResult> workingDirectoryChildrenRetrievedForLogFileCallback = new ResultCallback<DriveApi.MetadataBufferResult>() {
        @Override
        public void onResult(DriveApi.MetadataBufferResult result) {
            if (!result.getStatus().isSuccess()) {
                log("Problem reading children of working directory while searching for logfile");
                return;
            }
            int length = result.getMetadataBuffer().getCount();
            for (int i = 0; i < length; i++) {
                Metadata metadata = result.getMetadataBuffer().get(i);
                if (!metadata.isFolder() && metadata.getTitle() != null && metadata.getTitle().equals(mLogFileName)) {
                    DriveId fileDriveId = metadata.getDriveId();
                    mLogFile = fileDriveId.asDriveFile();
                    log("Successfully found " + mLogFileName);
                    return;
                }
            }

            log("Log file " + mLogFileName + "' does not exist...Trying to create it");

            MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle(mLogFileName).setMimeType("text/plain").build();
            mWorkingDirectory.createFile(mGoogleApiClient, changeSet, null).setResultCallback(createEmptyLogFileCallback);

        }
    };



    ResultCallback<DriveFolder.DriveFileResult> createEmptyLogFileCallback = new ResultCallback<DriveFolder.DriveFileResult>() {
        @Override
        public void onResult(DriveFolder.DriveFileResult result) {
            if (!result.getStatus().isSuccess()) {
                log("Problem creating empty log file " + mLogFileName);
                return;
            } else {
                mLogFile = result.getDriveFile();
                log("Log File created: " + mLogFileName);
            }
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
                if (metadata.isFolder() && metadata.getTitle() != null && metadata.getTitle().equals("Aqua")) {
                    DriveId mAquaDriveId = metadata.getDriveId();
                    mAquaFolder = mAquaDriveId.asDriveFolder();
                    log("Successfully found 'Aqua' folder");
                    mAquaFolder.listChildren(mGoogleApiClient).setResultCallback(aquaChildrenRetrievedCallback);
                    return;
                }
            }

            log("Aqua folder does not exist...Trying to create it");
            MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle("Aqua").build();
            mRootFolder.createFolder(mGoogleApiClient, changeSet).setResultCallback(AquaFolderCreatedCallback);
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
            mAquaFolder.listChildren(mGoogleApiClient).setResultCallback(aquaChildrenRetrievedCallback);
        }
    };
}


