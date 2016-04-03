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
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.hippo.dict.DictImportService;
import com.hippo.ehviewer.R;
import com.hippo.util.TextUrl;

public class DictImportActivity extends AppCompatActivity {

    private final static String TAG = "DictImportActivity";
    private DictImportService serviceBinder;
    private int mTotal = 0;

    private Button importBtn;
    private Button cancelBtn;
    private Button hideBtn;

    private ProgressBar progressBar;
    private TextView progressTipView;
    private TextView tipView;
    private Uri mDictUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_dict_improt);

        Intent intent = new Intent(DictImportActivity.this, DictImportService.class);
        Log.d(TAG, "[onCreate] bind service");
        bindService(intent, conn, Context.BIND_AUTO_CREATE);
        startService(intent);


        importBtn = (Button) findViewById(R.id.btn_confirm);
        cancelBtn = (Button) findViewById(R.id.btn_cancel);
        hideBtn = (Button) findViewById(R.id.btn_hide);

        progressBar = (ProgressBar) findViewById(R.id.bar_import);
        progressTipView = (TextView) findViewById(R.id.tv_progress);
        tipView = (TextView) findViewById(R.id.tv_tip);


        importBtn.setOnClickListener(confirmListener);
        cancelBtn.setOnClickListener(cancelListener);
        hideBtn.setOnClickListener(hideListener);
    }

    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {

        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            serviceBinder = ((DictImportService.DictImportServiceBinder) service).getService();
            serviceBinder.setOnProgressListener(importListener);

            // if we are doing a task,we load improt infomation from service
            // and ignore the newer import task
            if (serviceBinder.isRunning()) {
                initFromSerivce();
            } else {
                initFromIntent();
            }

            Log.d(TAG, "connect to service");
        }
    };

    private DictImportService.ProcessListener importListener = new DictImportService.ProcessListener() {
        @Override
        public void process(int progress) {
            Log.i(TAG, "process " + progress);
            progressBar.setProgress(progress);
            progressTipView.setText(progress + "/" + mTotal);
        }

        @Override
        public void processTotal(int total) {
            mTotal = total;
            progressBar.setMax(mTotal);
        }

        @Override
        public void processComplete() {
            cancelBtn.setText("完成");
            hideBtn.setVisibility(View.GONE);
        }
    };


    private View.OnClickListener cancelListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            DictImportActivity.this.finish();
        }
    };

    private View.OnClickListener abortListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            Log.d(TAG, "[onClick] abort");
            if (serviceBinder != null) {
                serviceBinder.abortImport();
            }
            DictImportActivity.this.finish();
        }
    };

    private View.OnClickListener confirmListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Log.d(TAG, "[onClick] confirm");
            if (serviceBinder == null) {
                // todo error tip
                return;
            }
            if (mDictUri == null) {
                // todo error tip
                return;
            }


            serviceBinder.importDict(mDictUri);

            view.setVisibility(View.GONE);
            cancelBtn.setOnClickListener(abortListener);
            hideBtn.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);

        }
    };

    private View.OnClickListener hideListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Log.d(TAG, "[onClick] hide");
            DictImportActivity.this.finish();
        }
    };

    @Override
    protected void onDestroy() {
        serviceBinder.removeOnProgressListener(importListener);
        unbindService(conn);
        super.onDestroy();
    }


    private void initFromSerivce() {
        mDictUri = serviceBinder.getUri();
        Log.d(TAG, "[initFromSerivce] " + mDictUri.toString());

        mTotal = serviceBinder.getMax();
        progressBar.setMax(mTotal);
        importBtn.setVisibility(View.GONE);
        hideBtn.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        tipView.setText(TextUrl.getFileName(mDictUri.toString()));
        cancelBtn.setOnClickListener(abortListener);
    }

    private void initFromIntent() {
        mDictUri = getIntent().getData();
        Log.d(TAG, "[initFromIntent] " + mDictUri.toString());
        tipView.setText(TextUrl.getFileName(mDictUri.toString()));
    }

}
