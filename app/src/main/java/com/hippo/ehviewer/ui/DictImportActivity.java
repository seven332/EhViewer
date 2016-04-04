package com.hippo.ehviewer.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.hippo.dict.DictImportService;
import com.hippo.ehviewer.R;
import com.hippo.util.TextUrl;

public class DictImportActivity extends EhActivity {

    private final static String TAG = "DictImportActivity";
    private DictImportService serviceBinder;
    private int mItemNum = 0;

    private Button mConfirmBtn;
    private Button mCancelBtn;
    private Button mHideBtn;

    private ProgressBar mProgressBar;
    private TextView mProgressTipView;
    private TextView mTipView;
    private Uri mDictUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_dict_improt);

        Intent intent = new Intent(DictImportActivity.this, DictImportService.class);
        Log.d(TAG, "[onCreate] bind service");
        bindService(intent, conn, Context.BIND_AUTO_CREATE);
        startService(intent);


        mConfirmBtn = (Button) findViewById(R.id.btn_confirm);
        mCancelBtn = (Button) findViewById(R.id.btn_cancel);
        mHideBtn = (Button) findViewById(R.id.btn_hide);

        mProgressBar = (ProgressBar) findViewById(R.id.bar_import);
        mProgressTipView = (TextView) findViewById(R.id.tv_progress);
        mTipView = (TextView) findViewById(R.id.tv_tip);

        mConfirmBtn.setOnClickListener(confirmListener);
        mCancelBtn.setOnClickListener(cancelListener);
        mHideBtn.setOnClickListener(hideListener);
    }

    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.w(TAG, "[onServiceConnected] disconnect to service");
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

            Log.d(TAG, "[onServiceConnected] connect to service");
        }
    };

    private DictImportService.ProcessListener importListener = new DictImportService.ProcessListener() {
        @Override
        public void process(int progress) {
            Log.i(TAG, "[process] " + progress);
            mProgressBar.setProgress(progress);
            mProgressTipView.setText(progress + "/" + mItemNum);
        }

        @Override
        public void processTotal(int total) {
            mItemNum = total;
            mProgressBar.setMax(mItemNum);
        }

        @Override
        public void processComplete() {
            mCancelBtn.setText(getResources().getString(R.string.dict_done));
            mHideBtn.setVisibility(View.GONE);
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
                Log.e(TAG, "[onClick] service is not connected");
                return;
            }
            if (mDictUri == null) {
                // todo error tip
                Log.e(TAG, "[onClick] mDictUri is not be init");
                return;
            }
            serviceBinder.importDict(mDictUri);

            view.setVisibility(View.GONE);
            mCancelBtn.setOnClickListener(abortListener);
            mHideBtn.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.VISIBLE);

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
        if (mDictUri == null) {
            Log.e(TAG, "[initFromSerivce] error use of initFromeService,you may use initFromIntent instead");
            return;
        }

        Log.d(TAG, "[initFromSerivce] " + mDictUri.toString());

        mItemNum = serviceBinder.getItemNum();
        mProgressBar.setMax(mItemNum);
        mConfirmBtn.setVisibility(View.GONE);
        mHideBtn.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.VISIBLE);
        mTipView.setText(TextUrl.getFileName(mDictUri.toString()));
        mCancelBtn.setOnClickListener(abortListener);
    }

    private void initFromIntent() {
        mDictUri = getIntent().getData();
        Log.d(TAG, "[initFromIntent] " + mDictUri.toString());
        mTipView.setText(TextUrl.getFileName(mDictUri.toString()));
    }

}
