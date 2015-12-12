package com.bla.DataTransferService;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    CountDownTimer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        timer = createTimer();

    }

    private CountDownTimer createTimer(){
        SharedPreferences settings = getSharedPreferences("DataTransferService", MODE_PRIVATE);
        String interval = settings.getString("interval", "12");
        long milliseconds = Integer.parseInt(interval) * 60 * 60 * 1000;



        System.out.print("Starting timer for next transmission in " + milliseconds + " milliseconds");

        CountDownTimer timer = new CountDownTimer(milliseconds, 1000) {
            public void onTick(long milliseconds) {
                int seconds = (int) (milliseconds / 1000) % 60 ;
                int minutes = (int) ((milliseconds / (1000*60)) % 60);
                int hours   = (int) ((milliseconds / (1000*60*60)) % 24);
                String remainingDuration = "" + hours + ":" + minutes + ":" + seconds;
                updateTimer(remainingDuration);
            }
            public void onFinish() {
                startDataTransmission();
            }
        };

        timer.start();
        return timer;
    }

    private void updateTimer(String remainingDuration){
        TextView txtNextTransmissionDuration = (TextView) findViewById(R.id.txtNextTransmissionDuration);
        txtNextTransmissionDuration.setText(remainingDuration);
    }

    private void startDataTransmission(){
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
        switch (item.getItemId())
        {
            case (R.id.menu_bluetooth):
                this.startActivity(new Intent(this, BluetoothSettingsActivity.class));
                return true;
            case (R.id.menu_upload):
                this.startActivity(new Intent(this, UploadSettingsActivity.class));
                return true;
            case (R.id.menu_general):
                this.startActivity(new Intent(this, GeneralSettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
