package com.bla.DataTransferService;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    CountDownTimer timer;

    GDriveUtilities gDriveUtilities;
    Button btnConnectGDrive;
    TextView console;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        this.createTimer();
        this.registerToSettingsChange();

        this.btnConnectGDrive = (Button) findViewById(R.id.btnConnectGDrive);
        this.console = (TextView) findViewById(R.id.txtConsole);
        this.log("Starting...");

        try {
            this.gDriveUtilities = GDriveUtilitiesFactory.createGDriveUtilities(this);
            this.gDriveUtilities.setLogger(new ILogger() {
//                @Override
                public void onLog(String text) {
                    log(text);
                }

                public void onLogAsync(String text) {
                }
            });
        } catch (Exception ex) {
            this.log(ex.toString());
        }

        final Button btnStartTransmission = (Button) findViewById(R.id.btnStartTransmission);
        btnStartTransmission.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startDataTransmission();
            }
        });


        btnConnectGDrive.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                log("Connecting to GDrive...");
                gDriveUtilities.connect();
            }
        });
    }


    public void log(String text) {

        console.setText(console.getText() + "\n" + text);
        System.out.println(text);
    }


    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key == "interval") {
            this.timer.cancel();
            this.createTimer();
        }
    }


    private void registerToSettingsChange() {
        SharedPreferences settings = getSharedPreferences("DataTransferService", MODE_PRIVATE);
        settings.registerOnSharedPreferenceChangeListener(this);
    }


    private void createTimer() {
        SharedPreferences settings = getSharedPreferences("DataTransferService", MODE_PRIVATE);
        String interval = settings.getString("interval", "12");
        long milliseconds = Integer.parseInt(interval) * 60 * 60 * 1000;


        System.out.print("Starting timer for next transmission in " + milliseconds + " milliseconds");

        this.timer = new CountDownTimer(milliseconds, 1000) {
            public void onTick(long milliseconds) {
                int seconds = (int) (milliseconds / 1000) % 60;
                int minutes = (int) ((milliseconds / (1000 * 60)) % 60);
                int hours = (int) ((milliseconds / (1000 * 60 * 60)) % 24);
                String remainingDuration = "" + hours + ":" + minutes + ":" + seconds;
                updateTimer(remainingDuration);
            }

            public void onFinish() {
                startDataTransmission();
            }
        };

        this.timer.start();
    }

    private void updateTimer(String remainingDuration) {
        TextView txtNextTransmissionDuration = (TextView) findViewById(R.id.txtNextTransmissionDuration);
        txtNextTransmissionDuration.setText(remainingDuration);
    }

    private void startDataTransmission() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case (R.id.menu_bluetooth):
                this.startActivity(new Intent(this, BluetoothSettingsActivity.class));
                return true;
            case (R.id.menu_upload):
                this.startActivity(new Intent(this, UploadSettingsActivity.class));
                return true;
            case (R.id.menu_general):
                this.startActivity(new Intent(this, GeneralSettingsActivity.class));
                return true;
            case (R.id.menu_files):
                this.startActivity(new Intent(this, FilesOnDeviceActivity.class));
                return true;
            case (R.id.menu_terminal):
                this.startActivity(new Intent(this, TerminalActivity.class));
                return true;
            case (R.id.menu_gdrive):
                this.startActivity(new Intent(this, GDriveActivity.class));
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
