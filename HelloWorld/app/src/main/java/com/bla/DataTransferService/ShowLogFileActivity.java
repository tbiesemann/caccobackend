package com.bla.DataTransferService;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class ShowLogFileActivity extends AppCompatActivity {


//    ShowLogFileActivity mShowLogFileActivityInstance;
    TextView console;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        mShowLogFileActivityInstance = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_log_file);
        this.console = (TextView) findViewById(R.id.txtLog);
    }



    @Override
    protected void onStart() {
        super.onStart();

        final Handler LogFileReceiver = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                try {
                    final String logFile = msg.obj.toString();
                    console.setText(logFile);
                    super.handleMessage(msg);
                } catch(Exception ex){
                    console.setText("Error ocurred " + ex.toString());
                }
            }
        };

        Thread fetchLogFileThread = new Thread(new Runnable() {
            public void run() {
                String logFile = MainActivity.mainActivityInstance.mService.driveUtilities.readLogFile();
                Message msg = Message.obtain();
                msg.obj = logFile;
                LogFileReceiver.sendMessage(msg);
            }
        });
        fetchLogFileThread.start();


//        // Bind to Aqua Service
//        Intent intent = new Intent(this, AquaService.class);
//        this.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }
//
//    AquaService mService;
//    boolean mServiceBound = false;
//    private ServiceConnection mConnection = new ServiceConnection() {
//
//        @Override
//        public void onServiceConnected(ComponentName className, IBinder service) {
//            // We've bound to LocalService, cast the IBinder and get LocalService instance
//            AquaService.AquaServiceBinder binder = (AquaService.AquaServiceBinder) service;
//            mService = binder.getService();
//            mServiceBound = true;
//            try {
//                mService.setActivity(mShowLogFileActivityInstance, null);
//                String logFile = mService.driveUtilities.readLogFile();
//                console.setText(logFile);
//            } catch (Exception ex) {
//                console.setText("Something went really wrong: " + ex.toString());
//            }
//
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName arg0) {
//            mServiceBound = false;
//        }
//    };
}
