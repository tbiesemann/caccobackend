package com.bla.DataTransferService;


import java.util.concurrent.BlockingQueue;

public class GDriveRunnable  implements Runnable {

    GDriveUtilities mGDriveUtilities;
    BlockingQueue<String> mQueue;
    public GDriveRunnable(GDriveUtilities gDriveUtilities,  BlockingQueue<String> queue){
        this.mGDriveUtilities = gDriveUtilities;
        this.mQueue = queue;
    }

    @Override
    public void run() {
        try {

            while (true) {
                Thread.sleep(10);
                String text = mQueue.take();
                System.out.println("Writing to gdrive:" + text);
                this.mGDriveUtilities.appendToFile("data.txt", text);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
