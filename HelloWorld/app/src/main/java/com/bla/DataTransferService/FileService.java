package com.bla.DataTransferService;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

public class FileService {

    private Context oContext;
    private String mLocationName;
    private File mAquaDirectory;
    private File mLocationDirectory;
    private File mDailyReportsDirectory;
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

    private File getAquaDirectory() {
        boolean result = false;

        File appDirectory;
        if (isExternalStorageWritable()) {
            appDirectory = Environment.getExternalStorageDirectory();
        } else {
            appDirectory = new File(this.oContext.getApplicationInfo().dataDir);
        }

        File aquaDir = new File(appDirectory, "Aqua");
        if (!aquaDir.exists()) {

            result = aquaDir.mkdir();
        }

        if(!result){
            this.log("Cannot create Aqua directory: " + appDirectory.getAbsolutePath());
        }

        return aquaDir;
    }

    private File getLocationDirectory() {
        File locationDir = new File(this.mAquaDirectory, this.mLocationName);
        if (!locationDir.exists()) {
            locationDir.mkdirs();
        }
        return locationDir;
    }


    private File getDailyReportsDirectory() {
        File dailyReportsDir = new File(this.mLocationDirectory, "DailyReports");
        if (!dailyReportsDir.exists()) {
            dailyReportsDir.mkdirs();
        }
        return dailyReportsDir;
    }


    private File getOrCreateFile(File parentDirectory, String fileName) {
        File oFile = new File(parentDirectory, fileName);
        if (!oFile.exists()) {
            try {
                oFile.createNewFile();
            } catch (IOException ex) {
                this.log("Error getting file " + fileName + " in file system" + ex.toString());
            }
        }
        return oFile;
    }


    private void log(String text){
        System.out.println(text);
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


    public void appendToLogFile(String data) {
        Calendar c = Calendar.getInstance();
        String year = "" + (c.get(Calendar.YEAR));
        String month = "" + (c.get(Calendar.MONTH) + 1);
        if (c.get(Calendar.MONTH) < 10) {
            month = "0" + month;
        }
        String day = "" + c.get(Calendar.DAY_OF_MONTH);
        if (Calendar.DAY_OF_MONTH < 10) {
            day = "0" + day;
        }
        String logFileName = "log_" + AquaService.getInstance().settings.getLocation() + "_" + year + "_" + month + "_" + day + ".txt";

        File oLogFile = getOrCreateFile(this.mDailyReportsDirectory, logFileName);

        String now = android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss", new java.util.Date()).toString();
        final String text = "\n" + now.toString() + "  " + data;

        this.writeToFile(oLogFile, text);
    }


    public void appendToDataFile(String data) {

        // Get Names of monthly and daily data file
        Calendar c = Calendar.getInstance();
        String year = "" + (c.get(Calendar.YEAR));
        String month = "" + (c.get(Calendar.MONTH) + 1);
        if (c.get(Calendar.MONTH) < 10) {
            month = "0" + month;
        }
        String day = "" + c.get(Calendar.DAY_OF_MONTH);
        if (Calendar.DAY_OF_MONTH < 10) {
            day = "0" + day;
        }
        String monthlyDataFileName = AquaService.getInstance().settings.getLocation() + "_" + year + "_" + month + ".txt";
        String dailyDataFileName = AquaService.getInstance().settings.getLocation() + "_" + year + "_" + month + "_" + day + ".txt";

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
            FileWriter writer = new FileWriter(oFile, true);
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
        File oWorkingDirectory = getAquaDirectory();
        String sFilePath = "";

        try {

            File oFile = new File(oWorkingDirectory, sFileName);
            if (!oFile.exists()) {
                oFile.createNewFile();
            }

            FileWriter writer = new FileWriter(oFile);
            writer.append(sBody);
            writer.flush();
            writer.close();
            sFilePath = oWorkingDirectory + File.separator + sFileName;
            System.out.println("File saved in " + sFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sFilePath;
    }


    public void destroy() {
        this.oContext = null;
        this.mCurrentDataFile = null;
    }


    public ArrayList<File> getDailyFiles() {
        return getFilesOfDirectory(getDailyReportsDirectory());
    }

    public ArrayList<File> getMonthlyFiles() {
        return getFilesOfDirectory(getLocationDirectory());
    }


    private ArrayList<File> getFilesOfDirectory(File directory){
        ArrayList<File> result = new ArrayList<File>();
        File[] files = directory.listFiles();
        if (files != null) {
            for (File inFile : files) {
                if (inFile.isFile()) {
                    result.add(inFile);
                }
            }
        }
        return result;
    }


}
