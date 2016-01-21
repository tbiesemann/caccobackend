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

public class MainActivity extends AppCompatActivity{

    GDriveUtilities gDriveUtilities;
    Button btnConnectGDrive;
    TextView console;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        this.btnConnectGDrive = (Button) findViewById(R.id.btnConnectGDrive);
        this.console = (TextView) findViewById(R.id.txtConsole);

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
            case (R.id.menu_gmail):
                this.startActivity(new Intent(this, GmailSettingsActivity.class));
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
            case (R.id.menu_gmail_upload):
                this.startActivity(new Intent(this, GmailUploadActivity.class));
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
