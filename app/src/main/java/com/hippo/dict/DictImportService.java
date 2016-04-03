package com.hippo.dict;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DictImportService extends Service {
    private static final String TAG = "DictImportSerevice";

    private DictManager mDictManager;
    private List<ProcessListener> mListeners = new ArrayList<>();
    private ProcessListener mDictProcessListener;
    private AsyncTask mImportAsyncTask;

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
        mDictManager = new DictManager(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    public interface ProcessListener {
        void process(int progress);

        void processTotal(int total);

        void processComplete();
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
        mImportAsyncTask = new ImportAsyncTask(dictUri).execute();
    }

    public void abortImport() {
        if (mImportAsyncTask == null) {
            Log.e(TAG, "mImportAsyncTask is null");
            return;
        }

        Log.d(TAG, "[abortImport] improt abort");

        // fixme this i just set a flag to abort the prase thread,it may cause a exception
        // it there may be a elegant way to shut the worker thread down
        mDictManager.importAbort();
    }

    public void setOnProgressListener(ProcessListener onProgressListener) {
        if (onProgressListener != null) {
            mListeners.add(onProgressListener);
        }
    }

    public void removeOnProgressListener(ProcessListener onProgressListener) {
        if (onProgressListener != null) {
            mListeners.remove(onProgressListener);
        }
    }

    class ImportAsyncTask extends AsyncTask<Void, Integer, Void> {

        public Uri mDictUri;

        public ImportAsyncTask(Uri dictUri) {
            mDictUri = dictUri;
            mDictProcessListener = new ProcessListener() {
                @Override
                public void process(int progress) {
                    Log.d(TAG, "[process] progress:" + progress);
                    publishProgress(progress);
                }

                @Override
                public void processTotal(int total) {
                    Log.d(TAG, "process total " + total);
                    for (ProcessListener listener : mListeners) {
                        listener.processTotal(total);
                    }
                }

                @Override
                public void processComplete() {
                    // let async task handle this
                    Log.d(TAG, "processComplete");
                }

            };
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                mDictManager.importDict(mDictUri, mDictProcessListener);
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
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            for (ProcessListener listener : mListeners) {
                listener.processComplete();
            }
        }
    }
}
