package com.bla.DataTransferService;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class UploadSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        //Fill text boxes initially
        SharedPreferences settings = getPreferences(0);
        String gmailAddress = settings.getString("gmailAddress", "@gmail.com");
        String gmailPassword = settings.getString("gmailPassword", "");
        String recipient = settings.getString("recipient", "datenempfaenger@example.com");
        EditText txtGmailAddress = (EditText) findViewById(R.id.txtGmailAddress);
        txtGmailAddress.setText(gmailAddress);
        EditText txtGmailPassword = (EditText) findViewById(R.id.txtGmailPassword);
        txtGmailPassword.setText(gmailPassword);
        EditText txtRecipient = (EditText) findViewById(R.id.txtRecipient);
        txtRecipient.setText(recipient);

        final Button btnSaveSettings = (Button) findViewById(R.id.btnSaveSettings);
        btnSaveSettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SharedPreferences settings = getPreferences(0);
                String gmailAddress = ((EditText) findViewById(R.id.txtGmailAddress)).getText().toString();
                String gmailPassword = ((EditText) findViewById(R.id.txtGmailPassword)).getText().toString();
                String recipient = ((EditText) findViewById(R.id.txtRecipient)).getText().toString();
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("gmailAddress", gmailAddress);
                editor.putString("gmailPassword", gmailPassword);
                editor.putString("recipient", recipient);
                editor.commit();
                btnSaveSettings.setText("Saved...");
            }
        });


        final Button btnTestEmail = (Button) findViewById(R.id.btnTestEmail);
        btnTestEmail.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                btnTestEmail.setText("Sending Test Email...");
                sendTestEmail();
            }
        });

    }

    private void sendTestEmail(){
    }

}
