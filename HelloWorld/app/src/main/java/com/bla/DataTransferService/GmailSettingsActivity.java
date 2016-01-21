package com.bla.DataTransferService;

import com.creativityapps.gmailbackgroundlibrary.BackgroundMail;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


public class GmailSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gmail_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        //Fill text boxes initially
        SharedPreferences settings = getSharedPreferences("DataTransferService", MODE_PRIVATE);
        String gmailAddress = settings.getString("gmailAddress", "kit.aqua.01@gmail.com");
        String gmailPassword = settings.getString("gmailPassword", "");
        String recipient = settings.getString("recipient", "kit.aqua@trash-mail.com");
        String interval = settings.getString("interval", "12");
        EditText txtGmailAddress = (EditText) findViewById(R.id.txtGmailAddress);
        txtGmailAddress.setText(gmailAddress);
        EditText txtGmailPassword = (EditText) findViewById(R.id.txtGmailPassword);
        txtGmailPassword.setText(gmailPassword);
        EditText txtRecipient = (EditText) findViewById(R.id.txtRecipient);
        txtRecipient.setText(recipient);
        EditText txtInterval = (EditText) findViewById(R.id.txtInterval);
        txtInterval.setText(interval);

        final Button btnSaveSettings = (Button) findViewById(R.id.btnSaveSettings);
        btnSaveSettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SharedPreferences settings = getSharedPreferences("DataTransferService", MODE_PRIVATE);
                String gmailAddress = ((EditText) findViewById(R.id.txtGmailAddress)).getText().toString();
                String gmailPassword = ((EditText) findViewById(R.id.txtGmailPassword)).getText().toString();
                String recipient = ((EditText) findViewById(R.id.txtRecipient)).getText().toString();
                String interval = ((EditText) findViewById(R.id.txtInterval)).getText().toString();
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("gmailAddress", gmailAddress);
                editor.putString("gmailPassword", gmailPassword);
                editor.putString("recipient", recipient);
                editor.putString("interval", interval);
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

    private void sendTestEmail() {

        String gmailAddress = ((EditText) findViewById(R.id.txtGmailAddress)).getText().toString();
        String gmailPassword = ((EditText) findViewById(R.id.txtGmailPassword)).getText().toString();
        String recipient = ((EditText) findViewById(R.id.txtRecipient)).getText().toString();

        BackgroundMail backgroundMail = new BackgroundMail(GmailSettingsActivity.this);
        backgroundMail.setGmailUserName(gmailAddress);
        backgroundMail.setGmailPassword(gmailPassword);
        backgroundMail.setMailTo(recipient);
        backgroundMail.setFormBody("this is the body");
        backgroundMail.setFormSubject("Test email - this is the subject");

        backgroundMail.showVisibleProgress(true);
        String sFilePath = createTestAttachment();
        backgroundMail.setAttachment(sFilePath);
        backgroundMail.send();
    }

    private String createTestAttachment() {
        FileService fileService = new FileService(this);
        String sFilePath = fileService.writeToFile("test.txt", "ding dong");
        return sFilePath;

    }


}
