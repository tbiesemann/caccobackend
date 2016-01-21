package com.bla.DataTransferService;

import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class GmailUploadActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    CountDownTimer timer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gmail_upload);

        this.createTimer();
        this.registerToSettingsChange();


        final Button btnStartTransmission = (Button) findViewById(R.id.btnStartTransmission);
        btnStartTransmission.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startDataTransmission();
            }
        });


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


    private void updateTimer(String remainingDuration) {
        TextView txtNextTransmissionDuration = (TextView) findViewById(R.id.txtNextTransmissionDuration);
        txtNextTransmissionDuration.setText(remainingDuration);
    }

    private void startDataTransmission() {
        throw new UnsupportedOperationException("not yet implemented");
    }
}
