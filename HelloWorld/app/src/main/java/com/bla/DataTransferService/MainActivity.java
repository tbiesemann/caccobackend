package com.bla.DataTransferService;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements ILogger {

    TextView console;
    private static String consoleText = "";

    public static MainActivity mainActivityInstance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainActivityInstance = this;
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        this.console = (TextView) findViewById(R.id.txtConsole);
        this.console.setText(consoleText);

        //Start Aqua service
        try {
            Intent intent = new Intent(this, AquaService.class);
            this.startService(intent);
        } catch (Exception ex) {
            this.log("Something went really wrong: " + ex.toString());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to Aqua Service
        Intent intent = new Intent(this, AquaService.class);
        this.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    AquaService mService;
    boolean mServiceBound = false;
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            AquaService.AquaServiceBinder binder = (AquaService.AquaServiceBinder) service;
            mService = binder.getService();
            mServiceBound = true;
            try {
                mService.setActivity(mainActivityInstance, mainActivityInstance);
            } catch (Exception ex) {
                log("Something went really wrong: " + ex.toString());
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mServiceBound = false;
        }
    };




    public void log(String text) {
        int maxConsoleLength = 4000;

        int length = consoleText.length();
        if (length > maxConsoleLength) {
            consoleText = consoleText.substring(0, maxConsoleLength);
        }
        consoleText = text + "\n" + consoleText;
        console.setText(consoleText);
        System.out.println(text);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mService.handleOnMainActivityResult(requestCode, resultCode);
    }


    @Override
    protected void onDestroy() {
        this.log("Main Activity is getting destroyed - Make sure that orientation change is disabled in the Android settings!");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ex) {
        }

        this.unbindService(mConnection);

        super.onDestroy();
    }


}


