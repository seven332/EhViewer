package com.hippo.dict;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.List;

public class DictImportService extends Service {
    private static final String TAG = "DictImportSerevice";

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
        if (dictUri == null) {
            // TODO error tip
        }

        Log.d(TAG, "start import async task");
        new ImportAsyncTask(dictUri).execute();
    }

    public void setOnProgressListener(ProcessListener onProgressListener) {
        if (onProgressListener != null) {
            mListeners.add(onProgressListener);
        }
    }

    class ImportAsyncTask extends AsyncTask<Void, Integer, Integer> {

        public Uri mDictUri;

        public ImportAsyncTask(Uri dictUri) {
            mDictUri = dictUri;
            mDictProcessListener = new ProcessListener() {
                @Override
                public void process(int progress) {
                    publishProgress(progress);
                }
            };
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            try {
                mDictDatabase.importDict(mDictUri, mDictProcessListener);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);

            Log.d(TAG, "process item " + progress[0]);
            for (ProcessListener listener : mListeners) {
                listener.process(progress[0]);
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            Log.d(TAG,"cancel the import task");
            mDictDatabase.importAbort();
        }

    }
}
