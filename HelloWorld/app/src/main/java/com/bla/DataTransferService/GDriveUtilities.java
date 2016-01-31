package com.bla.DataTransferService;


import android.app.Activity;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.channels.FileChannel;

public class GDriveUtilities implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    final String mLogFileName = "log.txt";
    Activity mActivity;
    private GoogleApiClient mGoogleApiClient;
    private CurrentFile mCurrentFile;
    private DriveFile mLogFile;
    DriveFolder mRootFolder;
    DriveFolder mAquaFolder;
    DriveFolder mWorkingFolder;
    DriveFolder mDailyReportsFolder;

    public static final int REQUEST_CODE_RESOLUTION = 42;


    public GDriveUtilities(Activity activity) throws Exception {
        if (activity == null) {
            throw new Exception("Mandatory activity missing");
        }
        this.mActivity = activity;
        mGoogleApiClient = new GoogleApiClient.Builder(this.mActivity)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    public interface IconnectCompletedEventHandler {
        void handle();
    }


    IconnectCompletedEventHandler mConnectionCompletedHandler;

    public void registerConnectCompletedEventHandler(IconnectCompletedEventHandler handler) {
        mConnectionCompletedHandler = handler;
    }


    public void connect() {
        if (mGoogleApiClient.isConnected()) {
            log("GDrive is already connected");
            return;
        }
        mGoogleApiClient.connect();


    }



    private void log(String text) {
        GlobalState.getInstance().log(text);
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


    public void appendToLogFile(String data) throws Exception {
        if (mLogFile == null) {
            throw new Exception("Log file does not exist");
        }


        String now = android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss", new java.util.Date()).toString();
        final String text = "\n" + now.toString() + "  " + data;
        mLogFile.open(mGoogleApiClient, DriveFile.MODE_READ_WRITE, null)
                .setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {
                                       @Override
                                       public void onResult(DriveApi.DriveContentsResult result) {
                                           if (!result.getStatus().isSuccess()) {
                                               log("Cannot open log file for editing");
                                               return;
                                           }
                                           DriveContents driveContents = result.getDriveContents();

                                           try {
                                               ParcelFileDescriptor parcelFileDescriptor = driveContents.getParcelFileDescriptor();

                                               //For debugging - read content of log file
//                                               FileInputStream fileInputStream = new FileInputStream(parcelFileDescriptor.getFileDescriptor());
//                                               byte[] fileContent = new byte[fileInputStream.available()];
//                                               fileInputStream.read(fileContent);
//                                               String str = new String(fileContent, "UTF-8");


                                               FileOutputStream fileOutputStream = new FileOutputStream(parcelFileDescriptor.getFileDescriptor());

                                               //Jump to end of file
                                               FileChannel channel = fileOutputStream.getChannel();
                                               long size = channel.size();
                                               channel.position(size);

                                               Writer writer = new OutputStreamWriter(fileOutputStream);
                                               writer.write(text);
                                               writer.flush();
                                               writer.close();
                                           } catch (IOException e) {
                                               log("Error writing to logfile in Gdrive: " + e.toString());
                                           }

                                           driveContents.commit(mGoogleApiClient, null).setResultCallback(new ResultCallback<Status>() {
                                               @Override
                                               public void onResult(Status result) {
                                                   if (!result.getStatus().isSuccess()) {
                                                       log("Cannot commit changes to log file");
                                                       return;
                                                   } else {
                                                       return;
                                                   }
                                               }
                                           });

                                       }
                                   }
                );
    }


    public void appendToFile(String fileName, String data) {

        if (mCurrentFile != null && mCurrentFile.fileName == fileName) {
            this.appendToFile(mCurrentFile.file, data);
        } else {
            mTextNotYetAppendedToDataFile = data;
            mCurrentFile = new CurrentFile(fileName, null);
            mWorkingFolder.listChildren(this.mGoogleApiClient).setResultCallback(workingFolderChildrenRetrievedForDataFileCallback);
        }
    }

    private String mTextNotYetAppendedToDataFile;


    ResultCallback<DriveApi.MetadataBufferResult> workingFolderChildrenRetrievedForDataFileCallback = new ResultCallback<DriveApi.MetadataBufferResult>() {
        @Override
        public void onResult(DriveApi.MetadataBufferResult result) {
            if (!result.getStatus().isSuccess()) {
                log("Problem reading children of working folder");
                return;
            }
            int length = result.getMetadataBuffer().getCount();
            for (int i = 0; i < length; i++) {
                Metadata metadata = result.getMetadataBuffer().get(i);
                if (!metadata.isFolder() && !metadata.isTrashed() && metadata.getTitle() != null && metadata.getTitle().equals(mCurrentFile.fileName)) {
                    DriveId fileDriveId = metadata.getDriveId();
                    mCurrentFile.file = fileDriveId.asDriveFile();
                    log("Successfully found " + mCurrentFile.fileName + " file");

                    continuePendingAppendOperation();
                    return;
                }
            }

            log("File '" + mCurrentFile.fileName + "' does not exist...Trying to create it");

            MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle(mCurrentFile.fileName).setMimeType("text/plain").build();
            mWorkingFolder.createFile(mGoogleApiClient, changeSet, null).setResultCallback(createEmptyDataFileCallback);

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

                continuePendingAppendOperation();
            }
        }
    };

    private void continuePendingAppendOperation() {
        if (!mTextNotYetAppendedToDataFile.isEmpty()) { //If there is a pending append
            String tmp = mTextNotYetAppendedToDataFile;
            mTextNotYetAppendedToDataFile = "";
            appendToFile(mCurrentFile.fileName, tmp);
        }
    }


    private void appendToFile(DriveFile file, String data) {
        final String text = data;
        if (file == null) {
            log("Programming Error: cannot append to file, it is null");
            return;
        }
        file.open(mGoogleApiClient, DriveFile.MODE_READ_WRITE, null)
                .setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {
                                       @Override
                                       public void onResult(DriveApi.DriveContentsResult result) {
                                           if (!result.getStatus().isSuccess()) {
                                               log("Cannot open data file for editing");
                                               return;
                                           }
                                           DriveContents driveContents = result.getDriveContents();

                                           try {
                                               ParcelFileDescriptor parcelFileDescriptor = driveContents.getParcelFileDescriptor();

                                               //For debugging - read content of file
//                                               FileInputStream fileInputStream = new FileInputStream(parcelFileDescriptor.getFileDescriptor());
//                                               byte[] fileContent = new byte[fileInputStream.available()];
//                                               fileInputStream.read(fileContent);
//                                               String str = new String(fileContent, "UTF-8");

                                               FileOutputStream fileOutputStream = new FileOutputStream(parcelFileDescriptor.getFileDescriptor());

                                               //Jump to end of file
                                               FileChannel channel = fileOutputStream.getChannel();
                                               long size = channel.size();
                                               channel.position(size);

                                               Writer writer = new OutputStreamWriter(fileOutputStream);
                                               writer.write(text);
                                               writer.flush();
                                               writer.close();
                                           } catch (IOException e) {
                                               log("Error writing to data file in Gdrive: " + e.toString());
                                           }

                                           driveContents.commit(mGoogleApiClient, null).setResultCallback(new ResultCallback<Status>() {
                                               @Override
                                               public void onResult(Status result) {
                                                   if (!result.getStatus().isSuccess()) {
                                                       log("Cannot commit changes to data file");
                                                       return;
                                                   } else {
                                                       log("Successfully written to GDrive data file");
                                                       return;
                                                   }
                                               }
                                           });

                                       }
                                   }
                );
    }


    public void forceSync(){
        log("Starting GDrive Synchronization");
        Drive.DriveApi.requestSync(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status result) {
                if (!result.isSuccess()) {
                    log("GDrive Force Synchronization failed");
                } else {
                    log("GDrive Force Synchronization finished");
                }
            }
        });
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        this.log("GDrive connected");
        this.log("Synchronizing...");
        Drive.DriveApi.requestSync(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status result) {
                if (!result.isSuccess()) {
                    log("Synchronization failed failed error - no network connection? Ignoring error...");
                    initWorkingFolder();
                    return;
                } else {
                    log("Synchronization finished...");
                    initWorkingFolder();
                }
            }
        });
    }


    @Override
    public void onConnectionSuspended(int cause) {
        this.log("GDrive connection suspended");
    }

    public void destroy() {

    }




    public void initWorkingFolder() {
        mRootFolder = Drive.DriveApi.getRootFolder(this.mGoogleApiClient);
        mRootFolder.listChildren(this.mGoogleApiClient).setResultCallback(rootChildrenRetrievedCallback);
    }


    ResultCallback<DriveApi.MetadataBufferResult> aquaChildrenRetrievedCallback = new ResultCallback<DriveApi.MetadataBufferResult>() {
        @Override
        public void onResult(DriveApi.MetadataBufferResult result) {
            if (!result.getStatus().isSuccess()) {
                log("Problem reading children of Aqua folder");
                return;
            }
            int length = result.getMetadataBuffer().getCount();
            String locationName = GlobalState.getInstance().settings.getLocation();
            for (int i = 0; i < length; i++) {
                Metadata metadata = result.getMetadataBuffer().get(i);
                if (metadata.isFolder() && !metadata.isTrashed() && metadata.getTitle() != null && metadata.getTitle().equals(locationName)) {
                    if (metadata.isTrashed()) {
                        log(locationName + " folder is trashed...will be ignored");
                    } else {
                        DriveId workingFolderDriveId = metadata.getDriveId();
                        mWorkingFolder = workingFolderDriveId.asDriveFolder();
                        log("Successfully found '" + locationName + "' folder");

                        log("Looking for an existing log file and DailyReports folder...");
                        mWorkingFolder.listChildren(mGoogleApiClient).setResultCallback(workingFolderChildrenRetrievedForLogFileCallback);
                        return;
                    }
                }
            }

            createWorkingFolder();
        }
    };




    private void createWorkingFolder() {
        final String locationName = GlobalState.getInstance().settings.getLocation();
        log("Creating folder '" + locationName + "'");
        MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle(locationName).build();
        mAquaFolder.createFolder(mGoogleApiClient, changeSet).setResultCallback(new ResultCallback<DriveFolder.DriveFolderResult>() {
            @Override
            public void onResult(DriveFolder.DriveFolderResult result) {
                if (!result.getStatus().isSuccess()) {
                    log("Cannot create '" + locationName + "' folder in GDrive");
                    return;
                }
                log("Folder '" + locationName + "' created in GDrive");
                mWorkingFolder = result.getDriveFolder();
                createDailyReportsFolderAndCreateLogFile();
            }
        });
    }



    private void createDailyReportsFolderAndCreateLogFile() {
        final String locationName = GlobalState.getInstance().settings.getLocation();
        log("Creating folder 'DailyReports'");
        MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle("DailyReports").build();
        mWorkingFolder.createFolder(mGoogleApiClient, changeSet).setResultCallback(new ResultCallback<DriveFolder.DriveFolderResult>() {
            @Override
            public void onResult(DriveFolder.DriveFolderResult result) {
                if (!result.getStatus().isSuccess()) {
                    log("Cannot create 'DailyReports' folder in GDrive");
                    return;
                }
                log("Folder 'DailyReports' created in GDrive");
                mDailyReportsFolder = result.getDriveFolder();
                createEmptyLogfile(true);
            }
        });
    }



    private void createDailyReportsFolder(final boolean fireCompletedEvent) {
        final String locationName = GlobalState.getInstance().settings.getLocation();
        log("Creating folder 'DailyReports'");
        MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle("DailyReports").build();
        mWorkingFolder.createFolder(mGoogleApiClient, changeSet).setResultCallback(new ResultCallback<DriveFolder.DriveFolderResult>() {
            @Override
            public void onResult(DriveFolder.DriveFolderResult result) {
                if (!result.getStatus().isSuccess()) {
                    log("Cannot create 'DailyReports' folder in GDrive");
                    return;
                }
                log("Folder 'DailyReports' created in GDrive");
                mDailyReportsFolder = result.getDriveFolder();
                if(fireCompletedEvent) {
                    executeInitializationCompletedEventHandler();
                }
            }
        });
    }




    private void createEmptyLogfile(final boolean fireCompletedEvent) {
        log("Creating log file...");
        MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle(mLogFileName).setMimeType("text/plain").build();
        mWorkingFolder.createFile(mGoogleApiClient, changeSet, null).setResultCallback(new ResultCallback<DriveFolder.DriveFileResult>() {
            @Override
            public void onResult(DriveFolder.DriveFileResult result) {
                if (!result.getStatus().isSuccess()) {
                    log("Problem creating empty log file " + mLogFileName);
                    return;
                } else {
                    mLogFile = result.getDriveFile();
                    log("Log File created: " + mLogFileName);
                    if (fireCompletedEvent) {
                        executeInitializationCompletedEventHandler();
                    }
                }
            }
        });
    }


    ResultCallback<DriveApi.MetadataBufferResult> workingFolderChildrenRetrievedForLogFileCallback = new ResultCallback<DriveApi.MetadataBufferResult>() {
        @Override
        public void onResult(DriveApi.MetadataBufferResult result) {
            boolean DailyReportsFolderFound = false;
            boolean LogFileFound = false;
            if (!result.getStatus().isSuccess()) {
                log("Problem reading children of working folder while searching for logfile and DailyReports folder");
                return;
            }
            int length = result.getMetadataBuffer().getCount();
            for (int i = 0; i < length; i++) {
                Metadata metadata = result.getMetadataBuffer().get(i);
                if (!metadata.isFolder() && !metadata.isTrashed() && metadata.getTitle() != null && metadata.getTitle().equals(mLogFileName)) {
                    DriveId fileDriveId = metadata.getDriveId();
                    mLogFile = fileDriveId.asDriveFile();
                    log("Successfully found " + mLogFileName);
                    LogFileFound = true;
                }

                if (metadata.isFolder() && metadata.getTitle() != null && metadata.getTitle().equals("DailyReports")) {
                    DriveId folderDriveId = metadata.getDriveId();
                    mDailyReportsFolder = folderDriveId.asDriveFolder();
                    log("Successfully found DailyReports folder");
                    DailyReportsFolderFound = true;
                }
            }

            if (DailyReportsFolderFound && LogFileFound){
                executeInitializationCompletedEventHandler();
                return;
            }

            if(!DailyReportsFolderFound && !LogFileFound){
                createDailyReportsFolderAndCreateLogFile();
                return;
            }

            if(DailyReportsFolderFound && !LogFileFound){
                log("Log file " + mLogFileName + "' does not exist");
                createEmptyLogfile(true);
                return;
            }

            if(!DailyReportsFolderFound && LogFileFound){
                log("DailyReport folder does not exist");
                createDailyReportsFolder(true);
                return;
            }


        }
    };


    ResultCallback<DriveApi.MetadataBufferResult> rootChildrenRetrievedCallback = new ResultCallback<DriveApi.MetadataBufferResult>() {
        @Override
        public void onResult(DriveApi.MetadataBufferResult result) {
            if (!result.getStatus().isSuccess()) {
                log("Problem reading children of root folder");
                return;
            }
            int length = result.getMetadataBuffer().getCount();
            for (int i = 0; i < length; i++) {
                Metadata metadata = result.getMetadataBuffer().get(i);


                if (metadata.isFolder() && metadata.getTitle() != null && metadata.getTitle().equals("Aqua")) {
                    if (metadata.isTrashed()) {
                        log("Aqua folder is trashed...will be ignored");
                    } else {
                        DriveId mAquaDriveId = metadata.getDriveId();
                        mAquaFolder = mAquaDriveId.asDriveFolder();
                        log("Successfully found 'Aqua' folder");
                        mAquaFolder.listChildren(mGoogleApiClient).setResultCallback(aquaChildrenRetrievedCallback);
                        return;
                    }
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


    private void executeInitializationCompletedEventHandler() {
        if (mConnectionCompletedHandler != null) {
            mConnectionCompletedHandler.handle();
        }
    }



    public void handleOnMainActivityResult(final int requestCode, final int resultCode) {
        switch (requestCode) {
            case GDriveUtilities.REQUEST_CODE_RESOLUTION:
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


}


