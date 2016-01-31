package com.bla.DataTransferService;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements ILogger {

    Button btnStart;

    TextView console;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        this.btnStart = (Button) findViewById(R.id.btnStart);
        this.console = (TextView) findViewById(R.id.txtConsole);
        this.console.setText(consoleText);

        try {
            GlobalState.getInstance().setActivity(this, this);
        } catch (Exception ex) {
            this.log("Something went really wrong: " + ex.toString());
        }


        btnStart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                GlobalState.getInstance().startEverything();
            }
        });




    }



    private static String consoleText = "";
    public void log(String text) {
        int maxConsoleLength = 4000;

        int length = consoleText.length();
        if (length > maxConsoleLength) {
            consoleText = consoleText.substring(0 , maxConsoleLength);
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
            case (R.id.menu_files):
                this.startActivity(new Intent(this, FilesOnDeviceActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data );
        GlobalState.getInstance().handleOnMainActivityResult(requestCode, resultCode);
    }

}


