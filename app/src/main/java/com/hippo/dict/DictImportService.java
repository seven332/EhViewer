package com.hippo.dict;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;

import java.util.List;

public class DictImportService extends Service {
    private DictDatabase mDictDatabase;
    private List<ProcessListener> mListeners;
    private ProcessListener mDictProcessListener;

    public DictImportService() {
    }

    private Binder serviceBinder = new DictImportServiceBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mDictDatabase = DictDatabase.getInstance(this);
        mDictProcessListener = new ProcessListener() {
            @Override
            public void process(int progress) {
                for (ProcessListener listener : mListeners) {
                    listener.process(progress);
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    public interface ProcessListener {
        void process(int progress);
    }

    public class DictImportServiceBinder extends Binder {
        public DictImportService getService() {
            return DictImportService.this;
        }
    }

    public void importDict(Uri dictUri) {
        try {
            mDictDatabase.importDict(dictUri, mDictProcessListener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setOnProgressListener(ProcessListener onProgressListener) {
        if (onProgressListener != null) {
            mListeners.add(onProgressListener);
        }
    }
}
