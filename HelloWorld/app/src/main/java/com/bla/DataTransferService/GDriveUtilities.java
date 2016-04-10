package com.bla.DataTransferService;


import android.os.ParcelFileDescriptor;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.ExecutionOptions;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class GDriveUtilities {

    //final String mLogFileName = "log.txt";
    //    Context mContext;
    private GoogleApiClient mGoogleApiClient;
    //    private CurrentDataFile mCurrentFile;
    private DriveFile mLogFile;
//    DriveFolder mRootFolder;
//    DriveFolder mAquaFolder;
//    DriveFolder mLocationFolder;
//    DriveFolder mDailyReportsFolder;


    // public static final int REQUEST_CODE_RESOLUTION = 42;


    public GDriveUtilities(GoogleApiClient gDriveAPI) {// throws Exception {
//        if (context == null) {
//            throw new Exception("Mandatory context missing");
//        }
//        this.mContext = context;
        this.mGoogleApiClient = gDriveAPI;

    }


    public interface IGDriveConnectCompletedEventHandler {
        void handle();
    }

//    IGDriveConnectCompletedEventHandler mConnectionCompletedHandler;

//    public void registerConnectCompletedEventHandler(IGDriveConnectCompletedEventHandler handler) {
//        mConnectionCompletedHandler = handler;
//    }


    public void synchronize(String locationName, ArrayList<File> monthlyFiles, ArrayList<File> dailyFiles) {

        DriveFolder rootFolder = Drive.DriveApi.getRootFolder(this.mGoogleApiClient);
        DriveFolder aquaFolder = getOrCreateFolder(rootFolder, "Aqua");
        DriveFolder locationFolder = getOrCreateFolder(aquaFolder, locationName);
        DriveFolder dailyReportsFolder = getOrCreateFolder(locationFolder, "DailyReports");

        for (File file : monthlyFiles) {
            this.syncFileToGDrive(locationFolder, file);
        }
        for (File file : dailyFiles) {
            this.syncFileToGDrive(dailyReportsFolder, file);
        }

    }


    private void syncFileToGDrive(DriveFolder parentFolder, File file) {

        DriveFile driveFile = getOrCreateFile(parentFolder, file.getName());


        if (driveFile == null) {
            log("Error getting file '" + file.getName() + "' on GDrive");
            return;
        }

        DriveResource.MetadataResult metadata = driveFile.getMetadata(mGoogleApiClient).await();
        long gDriveFileSize = metadata.getMetadata().getFileSize();
        long fileSize = file.length();
        if (fileSize == gDriveFileSize) {
            log("File " + file.getName() + " with size " + gDriveFileSize + "is up to date and will not be synchronized to GDrive");
            return;
        }


        if (fileSize < gDriveFileSize) {
            log("Error: " + file.getName() + " has size " + fileSize + " in file system and " + gDriveFileSize + " in GDrive. File in GDrive must not be larger! GDrive will be overwritten.");
        } else {
            log("Writing " + fileSize + " bytes to " +  file.getName() + " in GDrive");
        }

        //Read data from file system
        String data = this.getFileContent(file);

        //Open file for editing. Content will be truncated.
        DriveApi.DriveContentsResult result = driveFile.open(mGoogleApiClient, DriveFile.MODE_WRITE_ONLY, null).await();
        if (!result.getStatus().isSuccess()) {
            log("Cannot open file '" + file.getName() + "' for editing");
            return;
        }
        DriveContents driveContents = result.getDriveContents();

        try {
            ParcelFileDescriptor parcelFileDescriptor = driveContents.getParcelFileDescriptor();
            FileOutputStream fileOutputStream = new FileOutputStream(parcelFileDescriptor.getFileDescriptor());

//            //Jump to end of file
//            FileChannel channel = fileOutputStream.getChannel();
//            long size = channel.size();
//            channel.position(size);

            Writer writer = new OutputStreamWriter(fileOutputStream);
            writer.write(data);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            log("Error writing to '" + file.getName() + "' in Gdrive: " + e.toString());
        }

        Status status = driveContents.commit(mGoogleApiClient, null).await();

        if (!status.getStatus().isSuccess()) {
            log("Error: Cannot commit changes to '" + file.getName() + "'");
            return;
        }

    }


    private String getFileContent(File file){
        int fileLength = (int) file.length();
        char[] buffer = new char[fileLength];

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            br.read(buffer);
            br.close();
        }
        catch (IOException ex) {
            log("Error reading file from file system: " + file.getName() + ex.toString());
        }

        String result = new String(buffer);
        return result;
    }


//    private void initWorkingFolder(String locationName) {
//        mRootFolder = Drive.DriveApi.getRootFolder(this.mGoogleApiClient);
//        mAquaFolder = getOrCreateFolder(mRootFolder, "Aqua");
//        mLocationFolder = getOrCreateFolder(mAquaFolder, locationName);
//        mDailyReportsFolder = getOrCreateFolder(mLocationFolder, "DailyReports");
////        mLogFile = getOrCreateFile(mLocationFolder, mLogFileName);
////        mLogFile.addChangeSubscription(mGoogleApiClient);
////        executeInitializationCompletedEventHandler();
//    }


//    private void _connect(Activity activity) {
//        if (mGoogleApiClient.isConnected()) {
//            log("GDrive is already connected");
////            synchronizeGDrive();
////            initWorkingFolder();
//            return;
//        }
//
//        ConnectionResult connectionResult = mGoogleApiClient.blockingConnect();
//        if (connectionResult.isSuccess()) {
//            log("GDrive connected");
////            synchronizeGDrive();
////            initWorkingFolder();
//        } else if (connectionResult.hasResolution()) {
//            try {
//                this.log("Info: Connection failed, user needs to sign in...");
//                connectionResult.startResolutionForResult(this.mContext, REQUEST_CODE_RESOLUTION);
//            } catch (IntentSender.SendIntentException e) {
//                // Unable to resolve, message user appropriately
//                this.log("Error: Something with GDrive went wrong.....");
//            }
//        } else {
//            this.log("Error: Cannot connect to GDrive and no error resolution possible");
//        }
//    }


//    public void connect(final Activity activity) {
//        Thread backgroundThread = new Thread(new Runnable() {
//            public void run() {
//                _connect(activity);
//            }
//        });
//        backgroundThread.start();
//    }


    private void log(String text) {
        AquaService.getInstance().log(text);
    }


//    private class CurrentDataFile {
//        public CurrentDataFile(String monthlyFileName, String dailyFileName, DriveFile monthlyFile, DriveFile dailyFile) {
//            this.monthlyFileName = monthlyFileName;
//            this.dailyFileName = dailyFileName;
//            this.monthlyFile = monthlyFile;
//            this.dailyFile = dailyFile;
//        }
//
//        public String monthlyFileName;
//        public String dailyFileName;
//        public DriveFile monthlyFile;
//        public DriveFile dailyFile;
//    }


    public synchronized String readLogFile() {
        if (mLogFile == null) {
            return "GDrive not initialized";
        }

        DriveApi.DriveContentsResult result = mLogFile.open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null).await();

        if (!result.getStatus().isSuccess()) {
            return "Cannot open log file for reading";
        }

        DriveContents driveContents = result.getDriveContents();
        String logFileContent;
        try {
            ParcelFileDescriptor parcelFileDescriptor = driveContents.getParcelFileDescriptor();

            FileInputStream fileInputStream = new FileInputStream(parcelFileDescriptor.getFileDescriptor());
            byte[] fileContent = new byte[fileInputStream.available()];
            fileInputStream.read(fileContent);
            logFileContent = new String(fileContent, "UTF-8");
        } catch (Exception ex) {
            return "Error reading log file";
        }
        return logFileContent;
    }
//
//    public void appendToLogFile(String data) throws Exception {
//        String now = android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss", new java.util.Date()).toString();
//        final String text = "\n" + now.toString() + "  " + data;
//
//        appendDataToFile(mLogFile, mLogFileName, text);
//    }


//    private class GDriveBufferClass {
//        DriveFile file;
//        String fileName;
//        String buffer;
//    }

//    HashMap<String, GDriveBufferClass> gDriveBuffer = new HashMap<>();

//    Date lastCommitDate = new Date();

    /**
     * Saves data and commits to GDrive at most every 5 minutes
     *
     * @param file
     * @param fileName
     * @param data
     * @throws Exception
     */
//    private synchronized void appendDataToFile(DriveFile file, String fileName, String data) throws Exception {
//        long gDriveCommitPeriod = 1000 * 60 * 2;
//
//        GDriveBufferClass gDriveFileBuffer = gDriveBuffer.get(fileName);
//        if (gDriveFileBuffer == null) {
//            gDriveFileBuffer = new GDriveBufferClass();
//            gDriveFileBuffer.buffer = data;
//            gDriveFileBuffer.file = file;
//            gDriveFileBuffer.fileName = fileName;
//            gDriveBuffer.put(fileName, gDriveFileBuffer);
//        } else {
//            gDriveFileBuffer.file = file;
//            gDriveFileBuffer.buffer = gDriveFileBuffer.buffer + data;
//        }
//
//        long currentTime = new Date().getTime();
//        long lastCommitTime = lastCommitDate.getTime();
//        long timeSinceLastCommit = Math.abs(currentTime - lastCommitTime);
//        if (timeSinceLastCommit > gDriveCommitPeriod) {  //5 minutes since last commit passed
//            GDriveBufferClass[] buffers = gDriveBuffer.values().toArray(new GDriveBufferClass[gDriveBuffer.size()]);
//            for (int i = 0; i < buffers.length; i++) {
//                GDriveBufferClass buffer = buffers[i];
//                appendDataToFileAndCommit(buffer.file, buffer.fileName, buffer.buffer);
//                buffer.buffer = "";
//            }
//            lastCommitDate = new Date();
//        }
//    }


    /**
     * Commits to GDrive
     *
     * @param file
     * @param fileName
     * @param data
     * @throws Exception
     */
    private synchronized void appendDataToFileAndCommit(DriveFile file, String fileName, String data) throws Exception {
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

        Status status;
        if (fileName.equals("log.txt")) {
            ExecutionOptions executionOptions = new ExecutionOptions.Builder().setNotifyOnCompletion(true).setTrackingTag("AppendToLogFile").build();
            status = driveContents.commit(mGoogleApiClient, null, executionOptions).await();
        } else {
            status = driveContents.commit(mGoogleApiClient, null).await();
        }

        if (!status.getStatus().isSuccess()) {
            log("Cannot commit changes to '" + fileName + "'");
            return;
        } else {
//            logToUIOnly("Committing to Gdrive suceeded for " + fileName + /*" " + data.substring(0, 10) + "..." +*/ " with status " + status.getStatusMessage());
            return;
        }

    }


//    public void appendToDataFile(String monthlyDataFileName, String dailyDataFileName, String data) {
//
//        if (mCurrentFile == null) {
//            mCurrentFile = new CurrentDataFile(monthlyDataFileName, dailyDataFileName, null, null);
//        }
//
//        if (!monthlyDataFileName.equals(mCurrentFile.monthlyFileName) || mCurrentFile.monthlyFile == null) {
//            mCurrentFile.monthlyFileName = monthlyDataFileName;
//            mCurrentFile.monthlyFile = getOrCreateFile(mLocationFolder, monthlyDataFileName);
//        }
//
//        if (!dailyDataFileName.equals(mCurrentFile.dailyFileName) || mCurrentFile.dailyFile == null) {
//            mCurrentFile.dailyFileName = dailyDataFileName;
//            mCurrentFile.dailyFile = getOrCreateFile(mDailyReportsFolder, dailyDataFileName);
//        }
//
//        try {
//            this.appendDataToFile(mCurrentFile.monthlyFile, mCurrentFile.monthlyFileName, data);
//            this.appendDataToFile(mCurrentFile.dailyFile, mCurrentFile.dailyFileName, data);
//        } catch (Exception ex) {
//            log("Error writing data files to GDrive");
//        }
//    }


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
                        //log("Successfully found '" + fileName + "' file");
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

//
//    public void synchronizeGDrive() {
//        this.log("Synchronizing...");
//        try {
//            Status result = Drive.DriveApi.requestSync(mGoogleApiClient).await();
//            if (!result.isSuccess()) {
//                log("Synchronization failed failed error - no network connection? Ignoring error...");
//                initWorkingFolder();
//                return;
//            } else {
//                log("Synchronization finished...");
//            }
//        } catch (Exception ex) {
//            log("Synchronization crashed...");
//        }
//    }


//    private void executeInitializationCompletedEventHandler() {
//        if (mConnectionCompletedHandler != null) {
//            mConnectionCompletedHandler.handle();
//        }
//    }

//
//    public void handleOnMainActivityResult(final int requestCode, final int resultCode) {
//        switch (requestCode) {
//            case GDriveUtilities.REQUEST_CODE_RESOLUTION:
//                if (resultCode == Activity.RESULT_OK) {
//                    log("Trying to connect after sign in");
//                    connect();
//                } else if (resultCode == Activity.RESULT_CANCELED) {
//                    log("Sign in failed - cancelled");
//                } else {
//                    log("Sign in failed!");
//                }
//                break;
//        }
//    }


    public void destroy() {

        this.mGoogleApiClient.disconnect();
        this.mGoogleApiClient = null;

        // this.mContext = null;
//        this.mCurrentFile = null;
//        this.mLogFile = null;
//        this.mRootFolder = null;
//        this.mAquaFolder = null;
//        this.mLocationFolder = null;
//        this.mDailyReportsFolder = null;
//        this.gDriveBuffer.clear();
    }

}


