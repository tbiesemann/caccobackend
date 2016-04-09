package com.bla.DataTransferService;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class FileService {

    private Context oContext;
    private String mLocationName;
    private String mAquaDirectory;
    private String mLocationDirectory;
    private String mDailyReportsDirectory;
    private CurrentDataFile mCurrentDataFile;

    public FileService(Context context, String locationName) {
        this.oContext = context;
        this.mLocationName = locationName;
        this.mAquaDirectory = this.getAquaDirectory();
        this.mLocationDirectory = this.getLocationDirectory();
        this.mDailyReportsDirectory = this.getDailyReportsDirectory();
    }


    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    private String getAquaDirectory() {
        String sAquaDirectory;

        if (isExternalStorageWritable()) {
            File aquaDir = new File(Environment.getExternalStorageDirectory(), "Aqua");
            if (!aquaDir.exists()) {
                aquaDir.mkdirs();
            }
            sAquaDirectory = Environment.getExternalStorageDirectory().getPath() + File.separator + "Aqua" + File.separator;
        } else {
            sAquaDirectory = this.oContext.getApplicationInfo().dataDir;
        }
        return sAquaDirectory;
    }

    private String getLocationDirectory() {
        String sLocationDirectory;
        File locationDir = new File(this.mAquaDirectory, this.mLocationName);
        if (!locationDir.exists()) {
            locationDir.mkdirs();
        }
        sLocationDirectory = Environment.getExternalStorageDirectory().getPath() + File.separator + this.mLocationName + File.separator;
        return sLocationDirectory;
    }


    private String getDailyReportsDirectory() {
        String sLocationDirectory;
        File locationDir = new File(this.mAquaDirectory, "DailyReports");
        if (!locationDir.exists()) {
            locationDir.mkdirs();
        }
        sLocationDirectory = Environment.getExternalStorageDirectory().getPath() + File.separator + "DailyReports" + File.separator;
        return sLocationDirectory;
    }


    private File getOrCreateFile(String parentDirectory, String fileName) {
        File oFile = new File(parentDirectory, fileName);
        if (!oFile.exists()) {
            try {
                oFile.createNewFile();
            } catch (IOException ex) {
                System.out.println("Error getting file " + fileName + " in file system" + ex.toString());
            }
        }
        return oFile;
    }


    private class CurrentDataFile {
        public CurrentDataFile(String monthlyFileName, String dailyFileName, File monthlyFile, File dailyFile) {
            this.monthlyFileName = monthlyFileName;
            this.dailyFileName = dailyFileName;
            this.monthlyFile = monthlyFile;
            this.dailyFile = dailyFile;
        }

        public String monthlyFileName;
        public String dailyFileName;
        public File monthlyFile;
        public File dailyFile;
    }


    public void appendToDataFile(String monthlyDataFileName, String dailyDataFileName, String data) {

        if (mCurrentDataFile == null) {
            mCurrentDataFile = new CurrentDataFile(monthlyDataFileName, dailyDataFileName, null, null);
        }

        if (!monthlyDataFileName.equals(mCurrentDataFile.monthlyFileName) || mCurrentDataFile.monthlyFile == null) {
            mCurrentDataFile.monthlyFileName = monthlyDataFileName;
            mCurrentDataFile.monthlyFile = getOrCreateFile(this.mLocationDirectory, monthlyDataFileName);
        }

        if (!dailyDataFileName.equals(mCurrentDataFile.dailyFileName) || mCurrentDataFile.dailyFile == null) {
            mCurrentDataFile.dailyFileName = dailyDataFileName;
            mCurrentDataFile.dailyFile = getOrCreateFile(this.mDailyReportsDirectory, dailyDataFileName);
        }

        try {
            this.writeToFile(mCurrentDataFile.monthlyFile, data);
            this.writeToFile(mCurrentDataFile.dailyFile, data);
        } catch (Exception ex) {
            System.out.println("Error writing data files to file system" + ex.toString());
        }
    }


    private void writeToFile(File oFile, String data) {
        try {
            FileWriter writer = new FileWriter(oFile);
            writer.append(data);
            writer.flush();
            writer.close();
            System.out.println("File saved in " + oFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * If file already exists, content is appended to existing file
     *
     * @param sFileName
     * @param sBody
     * @return
     */
    private String writeToFile(String sFileName, String sBody) {
        String sWorkingDirectory = getAquaDirectory();
        String sFilePath = "";

        try {

            File oFile = new File(sWorkingDirectory, sFileName);
            if (!oFile.exists()) {
                oFile.createNewFile();
            }

            FileWriter writer = new FileWriter(oFile);
            writer.append(sBody);
            writer.flush();
            writer.close();
            sFilePath = sWorkingDirectory + File.separator + sFileName;
            System.out.println("File saved in " + sFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sFilePath;
    }


    public ArrayList<String> getFilesFromDisk() {
        ArrayList<String> result = new ArrayList<String>();
        String name;
        String sWorkingDirectory = getAquaDirectory();

        File dir = new File(sWorkingDirectory);
        File[] files = dir.listFiles();
        if (files != null) {
            for (File inFile : files) {
                if (inFile.isFile()) {
                    name = inFile.getName();
                    result.add(name);
                }
            }
        }

        return result;
    }


}
