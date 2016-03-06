package com.bla.DataTransferService;


import android.app.Activity;
import android.content.IntentSender;
import android.os.ParcelFileDescriptor;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
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

public class GDriveUtilities {

    final String mLogFileName = "log.txt";
    Activity mActivity;
    private GoogleApiClient mGoogleApiClient;
    private CurrentDataFile mCurrentFile;
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
                .build();
    }

    public interface IGDriveConnectCompletedEventHandler {
        void handle();
    }

    IGDriveConnectCompletedEventHandler mConnectionCompletedHandler;

    public void registerConnectCompletedEventHandler(IGDriveConnectCompletedEventHandler handler) {
        mConnectionCompletedHandler = handler;
    }

    private void _connect() {
        if (mGoogleApiClient.isConnected()) {
            log("GDrive is already connected");
            synchronizeGDrive();
            initWorkingFolder();
            return;
        }

        ConnectionResult connectionResult = mGoogleApiClient.blockingConnect();
        if (connectionResult.isSuccess()) {
            log("GDrive connected");
            synchronizeGDrive();
            initWorkingFolder();
        } else if (connectionResult.hasResolution()) {
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


    public void connect() {
        Thread backgroundThread = new Thread(new Runnable() {
            public void run() {
                _connect();
            }
        });
        backgroundThread.start();
    }


    private void log(String text) {
        AquaService.getInstance().log(text);
    }


    private class CurrentDataFile {
        public CurrentDataFile(String monthlyFileName, String dailyFileName, DriveFile monthlyFile, DriveFile dailyFile) {
            this.monthlyFileName = monthlyFileName;
            this.dailyFileName = dailyFileName;
            this.monthlyFile = monthlyFile;
            this.dailyFile = dailyFile;
        }

        public String monthlyFileName;
        public String dailyFileName;
        public DriveFile monthlyFile;
        public DriveFile dailyFile;
    }


    public void appendToLogFile(String data) throws Exception {
        String now = android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss", new java.util.Date()).toString();
        final String text = "\n" + now.toString() + "  " + data;

        appendDataToFile(mLogFile, mLogFileName, text);
    }

    public synchronized void appendDataToFile(DriveFile file, String fileName, String data) throws Exception {
        if (file == null) {
            throw new Exception("File '" + fileName + "' does not exist");
        }
        DriveApi.DriveContentsResult result = file.open(mGoogleApiClient, DriveFile.MODE_READ_WRITE, null).await();

        if (!result.getStatus().isSuccess()) {
            log("Cannot open file '" + fileName + "' for editing");
            return;
        }
        DriveContents driveContents = result.getDriveContents();

        try {
            ParcelFileDescriptor parcelFileDescriptor = driveContents.getParcelFileDescriptor();

            //For debugging - read content of log file
//            FileInputStream fileInputStream = new FileInputStream(parcelFileDescriptor.getFileDescriptor());
//            byte[] fileContent = new byte[fileInputStream.available()];
//            fileInputStream.read(fileContent);
//            String str = new String(fileContent, "UTF-8");

            FileOutputStream fileOutputStream = new FileOutputStream(parcelFileDescriptor.getFileDescriptor());

            //Jump to end of file
            FileChannel channel = fileOutputStream.getChannel();
            long size = channel.size();
            channel.position(size);

            Writer writer = new OutputStreamWriter(fileOutputStream);
            writer.write(data);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            log("Error writing to '" + fileName + "' in Gdrive: " + e.toString());
        }

        Status status = driveContents.commit(mGoogleApiClient, null).await();
        if (!status.getStatus().isSuccess()) {
            log("Cannot commit changes to '" + fileName + "'");
            return;
        } else {
            return;
        }

    }


    public void appendToDataFile(String monthlyDataFileName, String dailyDataFileName, String data) {

        if (mCurrentFile == null) {
            mCurrentFile = new CurrentDataFile(monthlyDataFileName, dailyDataFileName, null, null);
        }

        if (!monthlyDataFileName.equals(mCurrentFile.monthlyFileName) || mCurrentFile.monthlyFile == null) {
            mCurrentFile.monthlyFileName = monthlyDataFileName;
            mCurrentFile.monthlyFile = getOrCreateFile(mWorkingFolder, monthlyDataFileName);
        }

        if (!dailyDataFileName.equals(mCurrentFile.dailyFileName) || mCurrentFile.dailyFile == null) {
            mCurrentFile.dailyFileName = dailyDataFileName;
            mCurrentFile.dailyFile = getOrCreateFile(mDailyReportsFolder, dailyDataFileName);
        }

        try {
            this.appendDataToFile(mCurrentFile.monthlyFile, mCurrentFile.monthlyFileName, data);
            this.appendDataToFile(mCurrentFile.dailyFile, mCurrentFile.dailyFileName, data);
        } catch (Exception ex) {
            log("Error writing data files to GDrive");
        }
    }


    private DriveFolder getOrCreateFolder(DriveFolder parentFolder, String folderName) {
        DriveApi.MetadataBufferResult result = parentFolder.listChildren(this.mGoogleApiClient).await();
        if (!result.getStatus().isSuccess()) {
            log("Problem reading children of folder while searching for '" + folderName + "' folder");
            return null;
        }

        int length = result.getMetadataBuffer().getCount();
        try {
            for (int i = 0; i < length; i++) {
                Metadata metadata = result.getMetadataBuffer().get(i);
                if (metadata.isFolder() && !metadata.isTrashed() && metadata.getTitle() != null && metadata.getTitle().equals(folderName)) {
                    if (metadata.isTrashed()) {
                        log(folderName + " folder is trashed...will be ignored");
                    } else {
                        DriveId workingFolderDriveId = metadata.getDriveId();
                        log("Successfully found '" + folderName + "' folder");
                        return metadata.getDriveId().asDriveFolder();
                    }
                }
            }
        } finally {
            result.getMetadataBuffer().release();
        }

        log("Creating folder '" + folderName + "'");
        MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle(folderName).build();
        DriveFolder.DriveFolderResult createFolderResult = parentFolder.createFolder(mGoogleApiClient, changeSet).await();

        if (!createFolderResult.getStatus().isSuccess()) {
            log("Cannot create '" + folderName + "' folder in GDrive");
            return null;
        }
        log("Folder '" + folderName + "' created in GDrive");
        return createFolderResult.getDriveFolder();
    }


    private DriveFile getOrCreateFile(DriveFolder parentFolder, String fileName) {
        DriveApi.MetadataBufferResult result = parentFolder.listChildren(this.mGoogleApiClient).await();
        if (!result.getStatus().isSuccess()) {
            log("Problem reading children of folder while searching for file '" + fileName + "'");
            return null;
        }

        int length = result.getMetadataBuffer().getCount();
        try {
            for (int i = 0; i < length; i++) {
                Metadata metadata = result.getMetadataBuffer().get(i);
                if (!metadata.isFolder() && !metadata.isTrashed() && metadata.getTitle() != null && metadata.getTitle().equals(fileName)) {
                    if (metadata.isTrashed()) {
                        log(fileName + " file is trashed...will be ignored");
                    } else {
                        log("Successfully found '" + fileName + "' file");
                        return metadata.getDriveId().asDriveFile();
                    }
                }
            }
        } finally {
            result.getMetadataBuffer().release();
        }

        log("Creating file '" + fileName + "'");
        return createFile(parentFolder, fileName);
    }


    private DriveFile createFile(DriveFolder parentFolder, String fileName) {
        MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle(fileName).setMimeType("text/plain").build();
        DriveFolder.DriveFileResult result = parentFolder.createFile(mGoogleApiClient, changeSet, null).await();
        if (!result.getStatus().isSuccess()) {
            log("Problem creating file '" + fileName + "'");
            return null;
        } else {
            log("File created '" + fileName + "'");
            return result.getDriveFile();
        }
    }


    public void synchronizeGDrive() {
        this.log("Synchronizing...");
        try {
            Status result = Drive.DriveApi.requestSync(mGoogleApiClient).await();
            if (!result.isSuccess()) {
                log("Synchronization failed failed error - no network connection? Ignoring error...");
                initWorkingFolder();
                return;
            } else {
                log("Synchronization finished...");
            }
        } catch (Exception ex) {
            log("Synchronization crashed...");
        }
    }


    public void initWorkingFolder() {
        mRootFolder = Drive.DriveApi.getRootFolder(this.mGoogleApiClient);
        mAquaFolder = getOrCreateFolder(mRootFolder, "Aqua");
        String locationName = AquaService.getInstance().settings.getLocation();
        mWorkingFolder = getOrCreateFolder(mAquaFolder, locationName);
        mDailyReportsFolder = getOrCreateFolder(mWorkingFolder, "DailyReports");
        mLogFile = getOrCreateFile(mWorkingFolder, mLogFileName);
        executeInitializationCompletedEventHandler();
    }


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


    public void destroy() {

        this.mGoogleApiClient.disconnect();
        this.mGoogleApiClient = null;

        this.mActivity = null;
        this.mGoogleApiClient = null;
        this.mCurrentFile = null;
        this.mLogFile = null;
        this.mRootFolder = null;
        this.mAquaFolder = null;
        this.mWorkingFolder = null;
        this.mDailyReportsFolder = null;


    }

}


