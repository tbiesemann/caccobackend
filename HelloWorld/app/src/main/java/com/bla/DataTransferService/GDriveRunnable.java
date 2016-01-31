package com.bla.DataTransferService;


import java.util.Calendar;
import java.util.Date;
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
        try {

            while (true) {
                Thread.sleep(10);
                String text = mQueue.take(); //blocking call
                System.out.println("Writing to gdrive:" + text);
                writeDataToGDrive(text);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void writeDataToGDrive(String text) {
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        month++; //zero based
        int day = c.get(Calendar.DAY_OF_MONTH);

        String monthlyFileName = GlobalState.getInstance().settings.getLocation() + "_" + year + "_" + month + ".txt";
        String dailyFileName = GlobalState.getInstance().settings.getLocation() + "_" + year + "_" + month + "_" + day + ".txt";
        this.mGDriveUtilities.appendToFile(monthlyFileName, dailyFileName, text);

    }

}
