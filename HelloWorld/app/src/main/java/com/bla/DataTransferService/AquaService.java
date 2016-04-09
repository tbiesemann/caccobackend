package com.bla.DataTransferService;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Message;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;


public class AquaService extends Service {

    public Date mServiceCreationDate;
    public String version = "v0.3";
    private FileService fileService;
    //    private BlockingQueue<String> mMessageQueue;
    public BluetoothUtilities bluetoothUtilities;
    public Settings settings;
    //    boolean isGdriveInitialized = false;
//    public GDriveUtilities driveUtilities;
    private Activity activity;
    //    private Handler handlerForLoggerInUIAndGDrive;
    private Handler logHandler;
    private ILogger consoleLogger;
    //    private Thread mGDriveWriterThread;
    private Thread mBluetoothCreateConnectionThread;
    private Thread mSyncWithGDriveThread;
//    private boolean mIsGDriveConnected = false;


    private final IBinder mBinder = new AquaServiceBinder();


    public AquaService() {
        mServiceCreationDate = new Date();
        instance = this;
//        this.mMessageQueue = new LinkedBlockingQueue<String>();


//        initializeGDriveAndUILogger();
        initializeUIOnlyLogger();
    }


//    private void initializeGDriveAndUILogger() {
//        this.handlerForLoggerInUIAndGDrive = new Handler() {
//            @Override
//            public void handleMessage(Message msg) {
//                final String text = msg.obj.toString();
//                //Log to main activity
//                consoleLogger.log(text);
//
//                //Log to gdrive
//                if (isGdriveInitialized) {
//                    Thread backgroundLoggerThread = new Thread(new Runnable() {
//                        public void run() {
//                            try {
//                                driveUtilities.appendToLogFile(text);
//                            } catch (Exception ex) {
//                                consoleLogger.log("ERROR: Writing log to GDrive failed - " + ex.toString());
//                            }
//                        }
//                    });
//                    backgroundLoggerThread.start();
//                }
//                super.handleMessage(msg);
//            }
//        };
//    }


    public void synchronizeToGDrive() {
        log("Starting GDrive synchronization");

//                    Thread forceSyncThead =new Thread(){  //Gdrive sync must be called in worker thread
//                        public void run() {
//                            AquaService.getInstance().driveUtilities.synchronizeGDrive();
//                        }
//                    };
//                    forceSyncThead.start();
    }

    private void initializeUIOnlyLogger() {
        this.logHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                final String text = msg.obj.toString();
                //Log to main activity
                consoleLogger.log(text);

                if (fileService != null) {
                    fileService.appendToLogFile(text);
                }

                super.handleMessage(msg);
            }
        };
    }

    public class AquaServiceBinder extends Binder {
        AquaService getService() {
            // Return this instance of LocalService so clients can call public methods
            return AquaService.this;
        }
    }

    private static AquaService instance;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }


    static public AquaService getInstance() {
        return instance;
    }


    public void setActivity(Activity activity, ILogger logger) throws Exception {
        this.consoleLogger = logger;

        log("Aqua Service was created " + mServiceCreationDate.toString());
        log("(Re)Starting Aqua Service " + version);

        this.activity = activity;
        if (settings != null) {
            settings.destroy();
        }
//        if (driveUtilities != null) {
//            driveUtilities.destroy();
//        }
        if (bluetoothUtilities != null) {
            bluetoothUtilities.destroy();
        }
//        if (mGDriveWriterThread != null) {
//            mGDriveWriterThread.interrupt(); //stop gdrive thread
//            mGDriveWriterThread = null;
//        }
        if (fileService != null) {
            fileService.destroy();
        }

        if (mBluetoothCreateConnectionThread != null) {
            mBluetoothCreateConnectionThread.interrupt();
            mBluetoothCreateConnectionThread = null;
        }

        if (mSyncWithGDriveThread != null) {
            mSyncWithGDriveThread.interrupt();
            mSyncWithGDriveThread = null;
        }

        this.settings = new Settings(activity);

        this.bluetoothUtilities = new BluetoothUtilities();

        this.fileService = new FileService(this.getApplicationContext(), this.settings.getLocation());

        setupBluetooth();
        setupGDriveSyncIntervallTimer(this.settings.getGDriveSyncIntervall());

//        this.isGdriveInitialized = false;
//        this.driveUtilities = new GDriveUtilities(activity);
//        this.driveUtilities.registerConnectCompletedEventHandler(new GDriveUtilities.IGDriveConnectCompletedEventHandler() {
//            @Override
//            public void handle() {
//                isGdriveInitialized = true;
//                setupBluetooth();
//            }
//        });

//        log("Connecting to GDrive...");
//        this.driveUtilities.connect();
    }


    private void setupGDriveSyncIntervallTimer(final Integer IntervalInHours) {
        long milliseconds = IntervalInHours * 60000;
        log("Synchronizing with GDrive every " + IntervalInHours + " hours");
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                synchronizeToGDrive();
            }
        }, 0, milliseconds);
    }


    private void setupBluetooth() {
        log("Opening bluetooth...");

        mBluetoothCreateConnectionThread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (bluetoothUtilities == null) {
                    return;
                }

                boolean success = bluetoothUtilities.establishConnection();
                if (success) {
//                    connectGDriveWithBluetooth();
                } else {
                    try {
                        log("Waiting 60 seconds before retry");
                        Thread.sleep(60000);
                    } catch (InterruptedException ex) {
                        return;
                    }
                    setupBluetooth();
                }
            }
        });
        mBluetoothCreateConnectionThread.start();
    }


    public void onBluetoothDeviceDisconnected(BluetoothDevice device) {
        setupBluetooth();
    }


    public void log(String text) { //Can be called from any Thread
        Message msg = Message.obtain();
        msg.obj = text;
        this.logHandler.sendMessage(msg);
    }

//    public void logToUIOnly(String text) { //Can be called from any Thread
//        Message msg = Message.obtain();
//        msg.obj = text;
//        this.logHandler.sendMessage(msg);
//    }


    public void handleIncomingData(String data) {
//        try {
        this.fileService.appendToDataFile(data);
//            this.mMessageQueue.put(data);
//        } catch (InterruptedException ex) {
//            log("Error writing onto queue:" + ex.toString());
//        }
    }


//    public void connectGDriveWithBluetooth() {
//        log("Connecting bluetooth with google drive..");
//        if (mGDriveWriterThread != null) {
//            log("GDriveRunnable thread is already started.....");
//            return;
//        } else {
//            GDriveRunnable gDriveRunnable = new GDriveRunnable(driveUtilities, mMessageQueue);
//            mGDriveWriterThread = new Thread(gDriveRunnable);
//            mGDriveWriterThread.start();
//            log("Aqua is up and running...");
//        }
//    }


    public void handleOnMainActivityResult(final int requestCode, final int resultCode) {  //Needed for sign in to GDrive
//        if (this.driveUtilities != null) {
//            this.driveUtilities.handleOnMainActivityResult(requestCode, resultCode);
//        }
    }

}
