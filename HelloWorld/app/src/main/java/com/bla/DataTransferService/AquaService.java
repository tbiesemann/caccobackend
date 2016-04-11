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
    private FileService mFileService;
    public BluetoothUtilities bluetoothUtilities;
    public Settings settings;
    private GDriveSignIn mGDriveSignIn;
    private Activity activity;
    private Handler logHandler;
    private ILogger consoleLogger;
    private Thread mBluetoothCreateConnectionThread;


    private final IBinder mBinder = new AquaServiceBinder();


    public AquaService() {
        mServiceCreationDate = new Date();
        instance = this;
        initializeLogger();
    }


    public void setActivity(Activity activity, ILogger logger) throws Exception {
        this.consoleLogger = logger;

        log("Aqua Service was created " + mServiceCreationDate.toString());
        log("(Re)Starting Aqua Service " + version);

        this.activity = activity;
        if (settings != null) {
            settings.destroy();
        }
        if (bluetoothUtilities != null) {
            bluetoothUtilities.destroy();
        }
        if (mGDriveSignIn != null) {
            mGDriveSignIn.destroy();
            mGDriveSignIn = null;
        }

        if (mFileService != null) {
            mFileService.destroy();
        }

        if (mBluetoothCreateConnectionThread != null) {
            mBluetoothCreateConnectionThread.interrupt();
            mBluetoothCreateConnectionThread = null;
        }
        this.settings = new Settings(activity);

        this.bluetoothUtilities = new BluetoothUtilities();

        this.mFileService = new FileService(this.getApplicationContext(), this.settings.getLocation());

        setupBluetooth();

        this.mGDriveSignIn = new GDriveSignIn(activity, this.getApplicationContext(), new GDriveSignIn.IGDriveSignInCompletedEventHandler() {
            @Override
            public void handle() {
                setupGDriveSyncIntervallTimer(settings.getGDriveSyncIntervall());
            }
        });		
    }


    public void synchronizeToGDrive() {
        log("Starting GDrive synchronization");

        GDriveUtilities driveUtilities;
        try {
            driveUtilities = new GDriveUtilities(this.mGDriveSignIn.getGDriveAPI());
        } catch (Exception ex) {
            log("Exception while creating GDrive utilities: " + ex.toString());
            return;
        }
        ArrayList<File> dailyFiles = this.mFileService.getDailyFiles();
        ArrayList<File> monthlyFiles = this.mFileService.getMonthlyFiles();
        driveUtilities.synchronize(this.settings.getLocation(), monthlyFiles, dailyFiles);
    }


    private void initializeLogger() {
        this.logHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                final String text = msg.obj.toString();
                //Log to main activity
                consoleLogger.log(text);

                if (mFileService != null) {
                    mFileService.appendToLogFile(text);
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
        // Service shall continue running until it is explicitly stopped ==> return sticky.
        return START_STICKY;
    }


    static public AquaService getInstance() {
        return instance;
    }


    private void setupGDriveSyncIntervallTimer(final Integer IntervalInHours) {
        long milliseconds = IntervalInHours * 3600000;
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


    public void handleIncomingData(String data) {
        this.mFileService.appendToDataFile(data);
    }



    public void handleOnMainActivityResult(final int requestCode, final int resultCode) {  //Needed for sign in to GDrive
        if (this.mGDriveSignIn != null) {
            this.mGDriveSignIn.handleOnMainActivityResult(requestCode, resultCode);
        }
    }

}
