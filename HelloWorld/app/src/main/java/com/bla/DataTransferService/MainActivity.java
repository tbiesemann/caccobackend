package com.bla.DataTransferService;

import android.bluetooth.BluetoothAdapter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements ILogger {

    GDriveUtilities gDriveUtilities;
    Button btnConnectGDrive;
    Button btnOpenBluetoothConnection;
    Button btnStart;
    Button btnForceSync;
    TextView console;


    Thread mGDriveWriterThread;


    boolean isGdriveInitialized = false;


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

        GlobalState.getInstance().setActivity(this, this);


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
                    GlobalState.getInstance().log(text);
                }
            });
        } catch (Exception ex) {
            GlobalState.getInstance().log(ex.toString());
        }



        btnConnectGDrive.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                GlobalState.getInstance().log("Connecting to GDrive...");
                gDriveUtilities.connect();
            }
        });

        btnStart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (isGdriveInitialized != true) {
                    GlobalState.getInstance().log("Error: Cannot start - GDrive is not yet initialized!");
                    return;
                }
                if (mGDriveWriterThread != null){
                    String now = android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss", new java.util.Date()).toString();
                    try {
                        GlobalState.getInstance().mMessageQueue.put("Test content for GDrive" + now + "\n");
                    } catch (InterruptedException ex){
                        GlobalState.getInstance().log("Upps - error writing test data");
                    }
                    GlobalState.getInstance().log("Error: Already started.....");
                    return;
                }
                GlobalState.getInstance().log("Establish connection between bluetooth and GDrive...");

                GDriveRunnable gDriveRunnable = new GDriveRunnable(gDriveUtilities, GlobalState.getInstance().mMessageQueue);
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
                GlobalState.getInstance().log("Opening bluetooth...");
                openBluetoothConnection();
                GlobalState.getInstance().log("Done opening bluetooth connection");
            }
        });

        this.createBluetoothUtilities();

    }




    private void createBluetoothUtilities() {

        //Read from Settings
        SharedPreferences settings = getSharedPreferences("DataTransferService", MODE_PRIVATE);
        boolean useWindowsLineEndings = settings.getBoolean("useWindowsLineEndings", false);
        GlobalState.getInstance().bluetoothUtilities.useWindowsLineEndings = useWindowsLineEndings;

    }


    private void openBluetoothConnection() {

        //Make sure bluetooth is turned on
        BluetoothAdapter mBluetoothAdapter = GlobalState.getInstance().bluetoothUtilities.getBluetoothAdapter();
        if (mBluetoothAdapter == null) {
            GlobalState.getInstance().log("Bluetooth adapter is not available");
            return;
        }
        GlobalState.getInstance().bluetoothUtilities.enableBluetoothAdapter();

        SharedPreferences settings = getSharedPreferences("DataTransferService", MODE_PRIVATE);
        String deviceName = settings.getString("deviceName", "");
        GlobalState.getInstance().bluetoothUtilities.establishConnection(deviceName);
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
                    GlobalState.getInstance().log("Trying to connect after sign in");
                    gDriveUtilities.connect();
                } else if (resultCode == RESULT_CANCELED) {
                    GlobalState.getInstance().log("Sign in failed - cancelled");
                } else {
                    GlobalState.getInstance().log("Sign in failed!");
                }
                break;
        }
    }

}


