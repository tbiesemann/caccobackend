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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class GDriveUtilities implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    final String mLogFileName = "log.txt";
    Activity mActivity;
    private GoogleApiClient mGoogleApiClient;
    private IGDriveLogger mLogger;
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

    public interface IconnectCompletedEventHandler {
        void handle();
    }

    ;
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


    public void setLogger(IGDriveLogger listener) {
        mLogger = listener;
    }

    private void log(String text) {
        if (this.mLogger != null) {
            this.mLogger.onLog(text);
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
                                               FileOutputStream fileOutputStream = new FileOutputStream(parcelFileDescriptor.getFileDescriptor());
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
            mWorkingDirectory.listChildren(this.mGoogleApiClient).setResultCallback(workingDirectoryChildrenRetrievedForDataFileCallback);
        }
    }

    private String mTextNotYetAppendedToDataFile;


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
                if (!metadata.isFolder() && metadata.getTitle() != null && metadata.getTitle().equals(mCurrentFile.fileName)) {
                    DriveId fileDriveId = metadata.getDriveId();
                    mCurrentFile.file = fileDriveId.asDriveFile();
                    log("Successfully found " + mCurrentFile.fileName + " file");

                    continuePendingAppendOperation();
                    return;
                }
            }

            log("File '" + mCurrentFile.fileName + "' does not exist...Trying to create it");

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
                                               FileInputStream fileInputStream = new FileInputStream(parcelFileDescriptor.getFileDescriptor());
                                               byte[] fileContent = new byte[fileInputStream.available()];
                                               fileInputStream.read(fileContent);
                                               String str = new String(fileContent, "UTF-8");


                                               FileOutputStream fileOutputStream = new FileOutputStream(parcelFileDescriptor.getFileDescriptor());
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


    @Override
    public void onConnected(Bundle connectionHint) {
        this.log("GDrive connected");
        this.log("Synchronizing...");
        Drive.DriveApi.requestSync(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status result) {
                if (!result.isSuccess()) {
                    log("Synchronization failed failed error - no network connection? Ignoring error...");
                    initWorkingDirectory();
                    return;
                } else {
                    log("Synchronization finished...");
                    initWorkingDirectory();
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
                    if (metadata.isTrashed()) {
                        log(mLocationName + " folder is trashed...will be ignored");
                    } else {
                        DriveId workingDirectoryDriveId = metadata.getDriveId();
                        mWorkingDirectory = workingDirectoryDriveId.asDriveFolder();
                        log("Successfully found '" + mLocationName + "' folder");

                        log("Looking for an existing log file...");
                        mWorkingDirectory.listChildren(mGoogleApiClient).setResultCallback(workingDirectoryChildrenRetrievedForLogFileCallback);
                        return;
                    }
                }
            }

            createWorkingDirectory();
        }
    };


    private void createWorkingDirectory() {
        log("Creating folder '" + mLocationName + "'");
        MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle(mLocationName).build();
        mAquaFolder.createFolder(mGoogleApiClient, changeSet).setResultCallback(new ResultCallback<DriveFolder.DriveFolderResult>() {
            @Override
            public void onResult(DriveFolder.DriveFolderResult result) {
                if (!result.getStatus().isSuccess()) {
                    log("Cannot create '" + mLocationName + "' folder in GDrive");
                    return;
                }
                log("Folder '" + mLocationName + "' created in GDrive");
                mWorkingDirectory = result.getDriveFolder();

                createEmptyLogfile();
            }
        });

    }


    private void createEmptyLogfile() {
        log("Creating log file...");
        MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle(mLogFileName).setMimeType("text/plain").build();
        mWorkingDirectory.createFile(mGoogleApiClient, changeSet, null).setResultCallback(new ResultCallback<DriveFolder.DriveFileResult>() {
            @Override
            public void onResult(DriveFolder.DriveFileResult result) {
                if (!result.getStatus().isSuccess()) {
                    log("Problem creating empty log file " + mLogFileName);
                    return;
                } else {
                    mLogFile = result.getDriveFile();
                    log("Log File created: " + mLogFileName);
                    executeInitializationCompletedEventHandler();
                }
            }
        });
    }


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
                    executeInitializationCompletedEventHandler();
                    return;
                }
            }

            log("Log file " + mLogFileName + "' does not exist");
            createEmptyLogfile();
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
}


