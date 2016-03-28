package com.hippo.ehviewer.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.hippo.dict.DictImportService;
import com.hippo.ehviewer.R;

public class DictImportActivity extends AppCompatActivity {

    private final static String TAG = DictImportActivity.class.getSimpleName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, getIntent().getData().toString());
        setContentView(R.layout.activity_dict_improt);

        Intent intent = new Intent();
        bindService(intent, conn, Context.BIND_AUTO_CREATE);


        Button importBtn = (Button) findViewById(R.id.btn_import);
        importBtn.setText(importBtn.getText() + getIntent().getData().toString());
        final Uri dictUri = getIntent().getData();
        importBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
    }

    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {

        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DictImportService serviceBinder = ((DictImportService.DictImportServiceBinder) service).getService();
            serviceBinder.setOnProgressListener(new DictImportService.ProcessListener() {
                @Override
                public void process(int progress) {
                    Log.i(TAG, "process " + progress);
                }
            });
        }
    };
}
