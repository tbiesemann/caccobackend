package com.bla.DataTransferService;

import android.bluetooth.BluetoothAdapter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.widget.Button;
import android.widget.TextView;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MainActivity extends AppCompatActivity {

    GDriveUtilities gDriveUtilities;
    Button btnConnectGDrive;
    Button btnOpenBluetoothConnection;
    Button btnStart;
    Button btnForceSync;
    TextView console;

    GlobalState state;
    BlockingQueue<String> mMessageQueue;

    Thread mGDriveWriterThread;


    boolean isGdriveInitialized = false;

//    private BluetoothUtilities mBluetoothUtilities;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        this.btnConnectGDrive = (Button) findViewById(R.id.btnConnectGDrive);
        this.btnOpenBluetoothConnection = (Button) findViewById(R.id.btnOpenBluetoothConnection);
        this.btnStart = (Button) findViewById(R.id.btnStart);
        this.btnForceSync = (Button) findViewById(R.id.btnForceSync);
        this.console = (TextView) findViewById(R.id.txtConsole);


        this.state = GlobalState.getInstance();


        try {
            this.gDriveUtilities = GDriveUtilitiesFactory.createGDriveUtilities(this);
            this.gDriveUtilities.registerConnectCompletedEventHandler(new GDriveUtilities.IconnectCompletedEventHandler() {
                @Override
                public void handle() {
                    isGdriveInitialized = true;
                }
            });

            this.gDriveUtilities.setLogger(new IGDriveLogger() {
                public void onLog(String text) {
                    log(text);
                }
            });
        } catch (Exception ex) {
            this.log(ex.toString());
        }

        mMessageQueue =  new LinkedBlockingQueue<String>();


        btnConnectGDrive.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                log("Connecting to GDrive...");
                gDriveUtilities.connect();
            }
        });

        btnStart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (isGdriveInitialized != true) {
                    log("Error: Cannot start - GDrive is not yet initialized!");
                    return;
                }
                if (mGDriveWriterThread != null){
                    String now = android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss", new java.util.Date()).toString();
                    try {
                        mMessageQueue.put("Test content for GDrive" + now + "\n");
                    } catch (InterruptedException ex){
                        log("Upps - error writing test data");
                    }
                    log("Error: Already started.....");
                    return;
                }
                log("Establish connection between bluetooth and GDrive...");

                GDriveRunnable gDriveRunnable = new GDriveRunnable(gDriveUtilities, mMessageQueue);
                mGDriveWriterThread = new Thread(gDriveRunnable);
                mGDriveWriterThread.start();
            }
        });

        this.btnForceSync.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (gDriveUtilities != null){
                    gDriveUtilities.forceSync();
                }
            }
        });

        this.btnOpenBluetoothConnection.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                log("Opening bluetooth...");
                openBluetoothConnection();
                log("Done opening bluetooth connection");
            }
        });

        this.createBluetoothUtilities();

    }


    private void createBluetoothUtilities() {

//        this.mBluetoothUtilities = state.bluetoothUtilitiesBluetoothUtilitiesFactory.getBluetoothUtilities();

        //Read from Settings
        SharedPreferences settings = getSharedPreferences("DataTransferService", MODE_PRIVATE);
        boolean useWindowsLineEndings = settings.getBoolean("useWindowsLineEndings", false);
        state.bluetoothUtilities.useWindowsLineEndings = useWindowsLineEndings;
        state.bluetoothUtilities.setLogger(new ILogger() {
            @Override
            public void onLog(String text) {
                log(text);
            }

            public void onLogAsync(String text) {
                try {
                    if (mMessageQueue != null) {
                        mMessageQueue.put(text); //Write message from bluetooth onto the queue
                    }
                } catch (InterruptedException ex){
                    log("Error writing onto queue:" + ex.toString());
                }
            }
        });
    }


    private void openBluetoothConnection() {

        //Make sure bluetooth is turned on
        BluetoothAdapter mBluetoothAdapter = state.bluetoothUtilities.getBluetoothAdapter();
        if (mBluetoothAdapter == null) {
            this.log("Bluetooth adapter is not available");
            return;
        }
        state.bluetoothUtilities.enableBluetoothAdapter();

        SharedPreferences settings = getSharedPreferences("DataTransferService", MODE_PRIVATE);
        String deviceName = settings.getString("deviceName", "");
        state.bluetoothUtilities.establishConnection(deviceName);
    }

    public void log(String text) {
        int maxConsoleLength = 4000;

        String consoleText = console.getText().toString();
        int length = consoleText.length();
        if (length > maxConsoleLength){
            consoleText = consoleText.substring(length - maxConsoleLength, length);
        }

        console.setText(consoleText + "\n" + text);
        System.out.println(text);
        try {
            gDriveUtilities.appendToLogFile(text);
        } catch (Exception ex) {
            if (isGdriveInitialized) {
                console.setText(console.getText() + "\n" + "Cannot write log to gdrive");
                System.out.println(text);
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case (R.id.menu_bluetooth):
                this.startActivity(new Intent(this, BluetoothSettingsActivity.class));
                return true;
            case (R.id.menu_general):
                this.startActivity(new Intent(this, GeneralSettingsActivity.class));
                return true;
            case (R.id.menu_files):
                this.startActivity(new Intent(this, FilesOnDeviceActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {

            case GDriveUtilities.REQUEST_CODE_RESOLUTION:

                if (resultCode == RESULT_OK) {
                    this.log("Trying to connect after sign in");
                    gDriveUtilities.connect();
                } else if (resultCode == RESULT_CANCELED) {
                    this.log("Sign in failed - cancelled");
                } else {
                    this.log("Sign in failed!");
                }
                break;
        }
    }

}


