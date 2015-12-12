package com.bla.DataTransferService;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public class FilesOnDeviceActivity extends AppCompatActivity {


    public class MyArrayAdapter extends ArrayAdapter<String> {
        public View view;
        private List<String> files;

        public MyArrayAdapter(Context context, @LayoutRes int resource, @IdRes int textViewResourceId, @NonNull List<String> objects) {
            super(context, resource,textViewResourceId, objects);
            this.files = objects;
        }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            this.view = convertView;
            if (this.view == null) {
                LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                this.view = vi.inflate(R.layout.file_list_item, null);
            }
            TextView textView = (TextView) view.findViewById(R.id.txtFileName);
            textView.setText(this.files.get(pos));
            return view;
        }

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filesondevice);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FileService fileService = new FileService(this);
        ArrayList<String> files = fileService.getFilesFromDisk();

        for (int i = 0; i < 20; i++) {
            files.add("some_dummy_file" + i + ".txt");
        }
        MyArrayAdapter adapter = new MyArrayAdapter(this, R.layout.file_list_item, R.id.txtFileName, files);

        final ListView lv = (ListView) findViewById(R.id.listView);
        lv.setAdapter(adapter);
    }


}
