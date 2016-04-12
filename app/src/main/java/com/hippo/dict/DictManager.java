/*
 * Copyright 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.dict;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.hippo.dict.util.DictLog;

import java.io.IOException;
import java.net.URISyntaxException;

public class DictManager {

    private static final String TAG = "DictManager";
    private DictDatabase mDictDatabase;

    private static boolean mEnFlag = true;
    private static boolean mKeywordFlag = true;
    private static boolean mPrefixFlag = false;

    private DictQueryAsyncTask mEnSuggestionsAsyncTask;
    private DictQueryAsyncTask mKeywordQueryAsyncTask;
    private DictQueryAsyncTask mPrefixQueryAsyncTask;

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
        if (!mEnFlag) {
            Log.w(TAG, "get en suggestions is be disable");
            return;
        }

        if (prefix == null) {
            Log.e(TAG, "prefix should not be null");
            return;
        }

        if (mEnSuggestionsAsyncTask != null) {
            mEnSuggestionsAsyncTask.cancel(true);
        }

        mEnSuggestionsAsyncTask = new DictQueryAsyncTask(prefix, listener, new OpPolicy() {
            @Override
            public String[] getSuggestions(String prefix) {
                return mDictDatabase.getEnSuggestions(prefix);
            }
        }, new DictFilter.EnFilter());
        mEnSuggestionsAsyncTask.execute();
    }

    public void getKeywordSuggestions(String prefix, final OnDictQueryResultListener listener) {
        if (!mKeywordFlag) {
            Log.w(TAG, "get keyword suggestions is be disable");
            return;
        }

        if (prefix == null) {
            Log.e(TAG, "prefix should not be null");
            return;
        }

        if (mKeywordQueryAsyncTask != null) {
            mKeywordQueryAsyncTask.cancel(true);
        }

        mKeywordQueryAsyncTask = new DictQueryAsyncTask(prefix, listener, new OpPolicy() {
            @Override
            public String[] getSuggestions(String prefix) {
                return mDictDatabase.getKeywordSuggestions(prefix);
            }
        }, new DictFilter.LocaleFilter());
        mKeywordQueryAsyncTask.execute();
    }

    public void getPrefixSuggestions(String prefix, final OnDictQueryResultListener listener) {
        if (!mPrefixFlag) {
            Log.w(TAG, "get prefix suggestions is be disable");
            return;
        }

        if (prefix == null) {
            Log.e(TAG, "prefix should not be null");
            return;
        }

        if (mPrefixQueryAsyncTask != null) {
            mPrefixQueryAsyncTask.cancel(true);
        }

        mPrefixQueryAsyncTask = new DictQueryAsyncTask(prefix, listener, new OpPolicy() {
            @Override
            public String[] getSuggestions(String prefix) {
                return mDictDatabase.getPrefixSuggestions(prefix);
            }
        }, new DictFilter.LocaleFilter());
        mPrefixQueryAsyncTask.execute();
    }

    public void importAbort() {
        mDictDatabase.importAbort();
    }

    private class DictQueryAsyncTask extends AsyncTask<Void, Void, String[]> {
        private String prefix;
        private OnDictQueryResultListener listener;
        private OpPolicy op;
        private DictFilter.Filter filter;

        public DictQueryAsyncTask(String prefix, final OnDictQueryResultListener listener,
                                  OpPolicy op, DictFilter.Filter filter) {
            this.prefix = prefix;
            this.listener = listener;
            this.op = op;
            this.filter = filter;
        }

        @Override
        protected String[] doInBackground(Void... voids) {
            return filter.filter(op.getSuggestions(prefix));
        }

        @Override
        protected void onPostExecute(String[] result) {
            super.onPostExecute(result);
            listener.getResult(result);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            DictLog.t(TAG, "[onCancelled] prefix query task for prefix " + prefix + " abort");
        }
    }

    public interface OnDictQueryResultListener {
        void getResult(String[] result);
    }

    private interface OpPolicy {
        String[] getSuggestions(String prefix);
    }
}
