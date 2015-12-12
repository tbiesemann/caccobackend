package com.bla.DataTransferService;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import java.util.ArrayList;

public class FilesOnDeviceActivity extends AppCompatActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filesondevice);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        ArrayList valueList = new ArrayList<String>();
        for (int i = 0; i < 20; i++){
            valueList.add("value"+i);
        }
     //   ListAdapter adapter = new ArrayAdapter(getApplicationContext(), R.layout.file_list_item, valueList);

      //  mAdapter = new SimpleCursorAdapter(this,R.layout.file_list_item, null,fromColumns, toViews, 0);

        ArrayAdapter adapter = new ArrayAdapter<String>(this, R.layout.file_list_item, R.id.textView1, valueList);





        final ListView lv = (ListView)findViewById(R.id.listView);

        lv.setAdapter(adapter);


    }



}
