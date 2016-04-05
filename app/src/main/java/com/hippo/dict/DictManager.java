package com.hippo.dict;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DictManager {

    private static final String TAG = "DictManager";
    // pattern for language
    private static final String enRegEx = "^[0-9a-zA-Z_\\s]+$";
    private static final String zhRegEx = "^.*[\u4e00-\u9fa5].*$";

    private DictDatabase mDictDatabase;

    private DictEnQueryAsyncTask enSuggestionsAsyncTask;
    private DictPreFixQueryAsyncTask preFixQueryAsyncTask;

    public DictManager(Context context) {
        mDictDatabase = DictDatabase.getInstance(context);
    }

    public void importDict(final Uri dictUri, final DictImportService.ProcessListener listener)
            throws IOException, URISyntaxException {
        mDictDatabase.importDict(dictUri, listener);
    }

    public void deletDict(String dict) {
        mDictDatabase.deletDict(dict);
    }

    public void getEnSuggestions(String prefix, final OnDictQueryResultListener listener) {
        if (prefix == null) {
            Log.e(TAG, "prefix should not be null");
        }

        if (enSuggestionsAsyncTask != null) {
            enSuggestionsAsyncTask.cancel(true);
        }

        enSuggestionsAsyncTask = new DictEnQueryAsyncTask(prefix, listener);
         enSuggestionsAsyncTask.execute();
    }

    public void getPreFixSuggestions(String prefix, final OnDictQueryResultListener listener) {
        if (prefix == null) {
            Log.e(TAG, "prefix should not be null");
        }

        if (preFixQueryAsyncTask != null) {
            preFixQueryAsyncTask.cancel(true);
        }

        preFixQueryAsyncTask = new DictPreFixQueryAsyncTask(prefix, listener);
        preFixQueryAsyncTask.execute();
    }

    public void importAbort() {
        mDictDatabase.importAbort();
    }

    public boolean filter(String item) {
        // return true;
        Pattern en = Pattern.compile(enRegEx);
        Matcher m = en.matcher(item);
        return m.matches();
    }

    public boolean filterLocale(String item) {
        Pattern en = Pattern.compile(zhRegEx);
        Matcher m = en.matcher(item);
        return m.matches();
    }

    private class DictEnQueryAsyncTask extends AsyncTask<Void, Void, String[]> {

        private String prefix;
        private OnDictQueryResultListener listener;

        public DictEnQueryAsyncTask(String prefix, final OnDictQueryResultListener listener) {
            this.prefix = prefix;
            this.listener = listener;
        }

        @Override
        protected String[] doInBackground(Void... voids) {
            List<String> result = new ArrayList<>();
            String databaseResult[] = mDictDatabase.getEnSuggestions(prefix);
            for (String s : databaseResult) {
                if (filter(s)) {
                    result.add(s);
                }
            }
            return result.toArray(new String[result.size()]);
        }

        @Override
        protected void onPostExecute(String[] result) {
            super.onPostExecute(result);
            listener.getResult(result);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            Log.d(TAG, "task for prefix " + prefix + " abort");
        }
    }

    private class DictPreFixQueryAsyncTask extends AsyncTask<Void, Void, String[]> {
        private String prefix;
        private OnDictQueryResultListener listener;

        public DictPreFixQueryAsyncTask(String prefix, final OnDictQueryResultListener listener) {
            this.prefix = prefix;
            this.listener = listener;
        }

        @Override
        protected String[] doInBackground(Void... voids) {
            List<String> result = new ArrayList<>();
            String databaseResult[] = mDictDatabase.getPrefixSuggestions(prefix);
            for (String s : databaseResult) {
                if (filterLocale(s)) {
                    result.add(s);
                }
            }
            return result.toArray(new String[result.size()]);
        }

        @Override
        protected void onPostExecute(String[] result) {
            super.onPostExecute(result);
            listener.getResult(result);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            Log.d(TAG, "[onCancelled] prefix query task for prefix " + prefix + " abort");
        }
    }

    public interface OnDictQueryResultListener {
        void getResult(String[] result);
    }
}
