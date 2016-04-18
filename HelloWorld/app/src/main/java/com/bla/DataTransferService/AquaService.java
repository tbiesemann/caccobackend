package com.bla.DataTransferService;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;

import com.google.android.gms.common.api.GoogleApiClient;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;


public class AquaService extends Service {

    private PowerManager.WakeLock mWakeLock;
    private boolean mIsInitialized = false;
    private Date mServiceCreationDate;
    public String version = "v0.6";
    private FileService mFileService;
    public BluetoothUtilities bluetoothUtilities;
    public Settings settings;
    private GoogleApiClient mGDriveAPI;
    private Handler logHandler;
    private Timer mBluetoothRetryTimer;
    private Timer mGDriveSyncTimer;


    private final IBinder mBinder = new AquaServiceBinder();


    public AquaService() {
        mServiceCreationDate = new Date();
        mInstance = this;
        initializeLogger();
    }


    public void init(GoogleApiClient gDriveAPI) throws Exception {
        log("Aqua Service was created " + mServiceCreationDate.toString());

        if (this.mIsInitialized) {
            log("Error: Service is already initialized");
            return;
        }

        this.acquireWakeLock();

        this.mIsInitialized = true;

        mGDriveAPI = gDriveAPI;
        this.settings = new Settings(this.getApplicationContext());
        this.bluetoothUtilities = new BluetoothUtilities();
        this.mFileService = new FileService(this.getApplicationContext(), this.settings.getLocation());
        setupBluetooth();
        setupGDriveSyncIntervallTimer(settings.getGDriveSyncIntervall());
    }

    public void acquireWakeLock(){
        PowerManager powerManager = (PowerManager) this.getApplicationContext().getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "aquaWakelock()");
        this.mWakeLock.acquire();
    }


    public boolean isInitialized() {
        return this.mIsInitialized;
    }


    public void synchronizeToGDrive() {
        log("Starting GDrive synchronization");

        GDriveUtilities driveUtilities;
        try {
            driveUtilities = new GDriveUtilities(this.mGDriveAPI);
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
                if(MainActivity.mainActivityInstance != null) {
                    MainActivity.mainActivityInstance.log(text);
                }

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

    private static AquaService mInstance;

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
        return mInstance;
    }


    private int minutesTillNextGDriveSync;
    private void setupGDriveSyncIntervallTimer(final Integer IntervalInHours) {
        long milliseconds = IntervalInHours * 3600000;

        minutesTillNextGDriveSync = 0;


        Calendar now = Calendar.getInstance();
        now.add(Calendar.HOUR, IntervalInHours);
        log("Synchronizing with GDrive every " + IntervalInHours + " hours. Next Sync will be at " + now.getTime());
        if (this.mGDriveSyncTimer != null) {
            log("Error: race condition when setting up GDrive");
            return;
        }

        this.mGDriveSyncTimer = new Timer();

        this.mGDriveSyncTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                minutesTillNextGDriveSync--;
                if (minutesTillNextGDriveSync <= 0) {
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.HOUR, IntervalInHours);
                    log("Starting timed GDrive sync. Next Sync will be at " + cal.getTime());

                    synchronizeToGDrive();
                    minutesTillNextGDriveSync = IntervalInHours * 60;
                } else {
                    log("" + minutesTillNextGDriveSync + " minutes till next sync ");
                }
            }
        }, 0, 60000);
    }


    private void setupBluetooth() {
        log("Opening bluetooth...");

        if (mBluetoothRetryTimer != null) {
            mBluetoothRetryTimer.cancel();
            log("Error: Race condition when initializing bluetooth");
        }

        TimerTask timertask = new TimerTask() {
            @Override
            public void run() {
                boolean success = bluetoothUtilities.establishConnection();
                if (!success) {
                    log("Waiting 60 seconds before retry");
                } else {
                    mBluetoothRetryTimer.cancel();
                    mBluetoothRetryTimer = null;
                }
            }
        };
        mBluetoothRetryTimer = new Timer();
        mBluetoothRetryTimer.schedule(timertask, 0, 60000);
    }


    public void onBluetoothDeviceDisconnected(BluetoothDevice device) {
        setupBluetooth();
    }


    public Date getServiceCreationDateTime() {
        return mServiceCreationDate;
    }


    public void log(String text) { //Can be called from any Thread
        Message msg = Message.obtain();
        msg.obj = text;
        this.logHandler.sendMessage(msg);
    }


    public void handleIncomingData(String data) {
        this.mFileService.appendToDataFile(data);
    }

    public void onDestroy() {
        log("Service being destroyed...");
        super.onDestroy();

        this.mIsInitialized = false;
        if (settings != null) {
            settings.destroy();
        }
        if (bluetoothUtilities != null) {
            bluetoothUtilities.destroy();
        }
        if (mFileService != null) {
            mFileService.destroy();
        }
        if (this.mGDriveSyncTimer != null) {
            this.mGDriveSyncTimer.cancel();
            this.mGDriveSyncTimer = null;
        }
        if (mBluetoothRetryTimer != null) {
            mBluetoothRetryTimer.cancel();
            mBluetoothRetryTimer = null;
        }
        if(this.mWakeLock != null){
            this.mWakeLock.release();
        }
    }

}
