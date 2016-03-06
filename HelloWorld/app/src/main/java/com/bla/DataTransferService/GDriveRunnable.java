package com.bla.DataTransferService;

import java.util.Calendar;
import java.util.concurrent.BlockingQueue;

public class GDriveRunnable implements Runnable {

    GDriveUtilities mGDriveUtilities;
    BlockingQueue<String> mQueue;


    public GDriveRunnable(GDriveUtilities gDriveUtilities, BlockingQueue<String> queue) {
        this.mGDriveUtilities = gDriveUtilities;
        this.mQueue = queue;
    }

    @Override
    public void run() {
        String text = "";
        try {
            while (true) {
                if(Thread.interrupted()){
                    AquaService.getInstance().logToUIOnly("Stopping GDrive runnable..");
                    return;
                }
                Thread.sleep(10);
                text = mQueue.take(); //blocking call
                AquaService.getInstance().logToUIOnly("Writing " + text.length() + " bytes to gdrive:" + text);
                writeDataToGDrive(text);
            }
        } catch (InterruptedException e) {
            AquaService.getInstance().log("Writing to GDrive aborted. Thread was interrupted. Following " + text.length() + " bytes might be lost:" + text);
            e.printStackTrace();
        }
    }

    private void writeDataToGDrive(String text) {
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

        String monthlyFileName = AquaService.getInstance().settings.getLocation() + "_" + year + "_" + month + ".txt";
        String dailyFileName = AquaService.getInstance().settings.getLocation() + "_" + year + "_" + month + "_" + day + ".txt";
        this.mGDriveUtilities.appendToDataFile(monthlyFileName, dailyFileName, text);
    }
}
