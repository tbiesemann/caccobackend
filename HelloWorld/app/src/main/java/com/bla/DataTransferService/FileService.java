package com.bla.DataTransferService;


import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileService {

    private Context oContext;
    public FileService(Context context){
        this.oContext = context;
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public String getWorkingDirectory() {
        String sWorkingDirectory;

        if (isExternalStorageWritable()) {
            File root = new File(Environment.getExternalStorageDirectory(), "aqua");
            if (!root.exists()) {
                root.mkdirs();
            }
            sWorkingDirectory = Environment.getExternalStorageDirectory().getPath() + File.separator + "aqua" + File.separator;
        } else {
            sWorkingDirectory = this.oContext.getApplicationInfo().dataDir;
        }
        return sWorkingDirectory;
    }


    /**
     * If file already exists, content is appended to existing file
     * @param sFileName
     * @param sBody
     * @return
     */
    public String writeToFile(String sFileName,String sBody) {
        String sWorkingDirectory = getWorkingDirectory();
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


    public ArrayList<String> getFilesFromDisk(){
        ArrayList<String> result = new ArrayList<String>();
        String name;
        String sWorkingDirectory = getWorkingDirectory();

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
