package com.bla.DataTransferService;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Message;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class AquaService extends Service {

    public String version = "v0.2";
    public BlockingQueue<String> mMessageQueue;
    public BluetoothUtilities bluetoothUtilities;
    public Settings settings;
    boolean isGdriveInitialized = false;
    public GDriveUtilities driveUtilities;
    private Activity activity;
    private Handler handlerForLoggerInUIAndGDrive;
    private Handler handlerForUIOnly;
    private ILogger consoleLogger;
    private Thread mGDriveWriterThread;

    private final IBinder mBinder = new AquaServiceBinder();


    public AquaService() {
        instance = this;
        this.mMessageQueue = new LinkedBlockingQueue<String>();

        initializeGDriveAndUILogger();
        initializeUIOnlyLogger();
    }


    private void initializeGDriveAndUILogger() {
        this.handlerForLoggerInUIAndGDrive = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                final String text = msg.obj.toString();
                //Log to main activity
                consoleLogger.log(text);

                //Log to gdrive
                if (isGdriveInitialized) {
                    Thread backgroundLoggerThread = new Thread(new Runnable() {
                        public void run() {
                            try {
                                driveUtilities.appendToLogFile(text);
                            } catch (Exception ex) {
                                consoleLogger.log("ERROR: Writing log to GDrive failed - " + ex.toString());
                            }
                        }
                    });
                    backgroundLoggerThread.start();
                }
                super.handleMessage(msg);
            }
        };
    }


    private void initializeUIOnlyLogger() {
        this.handlerForUIOnly = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                final String text = msg.obj.toString();
                //Log to main activity
                consoleLogger.log(text);
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


    private boolean mIsGDriveConnected = false;
    public void startInitialization() {
        if(mIsGDriveConnected) {
            log("Aqua Service is already initialized");
        } else {
            mIsGDriveConnected = true;
            log("Starting Aqua Service " + version);
            log("Connecting to GDrive...");
            this.driveUtilities.connect();
        }
    }



    static public AquaService getInstance() {
//        if (instance == null) {
//            instance = new AquaService();  //TODO: Does this work ??
//        }
        return instance;
    }



    public void setActivity(Activity activity, ILogger logger) throws Exception {
        this.consoleLogger = logger;
        this.activity = activity;
        if (settings != null) {
            settings.destroy();
        }
        if (driveUtilities != null) {
            driveUtilities.destroy();
        }
        if (bluetoothUtilities != null) {
            bluetoothUtilities.destroy();
        }
        this.settings = new Settings(activity);
        this.bluetoothUtilities = new BluetoothUtilities();
        this.isGdriveInitialized = false;
        this.driveUtilities = new GDriveUtilities(activity);
        this.driveUtilities.registerConnectCompletedEventHandler(new GDriveUtilities.IGDriveConnectCompletedEventHandler() {
            @Override
            public void handle() {
                isGdriveInitialized = true;
                setupBluetooth();
            }
        });
    }


    private void setupBluetooth() {
        log("Opening bluetooth...");

        new Thread(new Runnable() {
            @Override
            public void run() {

                boolean success = bluetoothUtilities.establishConnection();
                if (success) {
                    connectGDriveWithBluetooth();
                } else {
                    try {
                        log("Waiting 40 seconds before retry");
                        Thread.sleep(40000);
                    } catch (InterruptedException ex) {
                    }
                    setupBluetooth();
                }
            }
        }).start();
    }



    public void onBluetoothDeviceDisconnected(BluetoothDevice device) {
        setupBluetooth();
    }


    public void log(String text) { //Can be called from any Thread
        Message msg = Message.obtain();
        msg.obj = text;
        this.handlerForLoggerInUIAndGDrive.sendMessage(msg);
    }

    public void logToUIOnly(String text) { //Can be called from any Thread
        Message msg = Message.obtain();
        msg.obj = text;
        this.handlerForUIOnly.sendMessage(msg);
    }


    public void handleIncomingData(String data) { //Can be called from any thread
        try {
            this.mMessageQueue.put(data);
        } catch (InterruptedException ex) {
            log("Error writing onto queue:" + ex.toString());
        }
    }


    public void connectGDriveWithBluetooth() {
        log("Connecting bluetooth with google drive..");
        if (mGDriveWriterThread != null) {
            log("GDriveRunnable thread is already started.....");
            return;
        } else {
            GDriveRunnable gDriveRunnable = new GDriveRunnable(driveUtilities, mMessageQueue);
            mGDriveWriterThread = new Thread(gDriveRunnable);
            mGDriveWriterThread.start();
            log("Aqua is up and running...");
        }
    }


    public void handleOnMainActivityResult(final int requestCode, final int resultCode) {
        if (this.driveUtilities != null) {
            this.driveUtilities.handleOnMainActivityResult(requestCode, resultCode);
        }
    }

}
