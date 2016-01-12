package com.bla.DataTransferService;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Button;
import java.io.IOException;


public class TerminalActivity extends AppCompatActivity {

    EditText txtDataForSending;
    TextView txtConsole;
    BluetoothUtilities bluetooth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terminal);


        Button sendButton = (Button) findViewById(R.id.send);

        txtDataForSending = (EditText) findViewById(R.id.entry);
        txtConsole = (TextView) findViewById(R.id.txtConsole);

        //Send Button
        sendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    sendData();
                    txtDataForSending.setText("");
                } catch (IOException ex) {
                    log("Error sending data:" + ex.toString());
                }
            }
        });


        this.bluetooth = BluetoothUtilitiesFactory.getBluetoothUtilities();
        this.bluetooth.setLogger(new ILogger() {
            @Override
            public void onLog(String text) {
                log(text);
            }
            public void onLogAsync(String text) {
                final String myText = text;
                txtConsole.post(new Runnable() {
                    public void run() {
                        log(myText);
                    }
                });
            }
        });

    }


    public void log(String text) {
        TextView console = (TextView) findViewById(R.id.txtConsole);
        console.setText(console.getText() + "\n" + text);
        System.out.println(text);
    }


    void sendData() throws IOException {
        String text = txtDataForSending.getText().toString();
        this.bluetooth.sendData(text);
    }

}