/*
 * Copyright 2016 Hippo Seven
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

package com.hippo.ehviewer.client;

import android.content.Context;
import android.os.AsyncTask;

import com.hippo.ehviewer.EhApplication;
import com.hippo.yorozuya.PriorityThreadFactory;
import com.hippo.yorozuya.SimpleHandler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.OkHttpClient;

public class EhClient {

    public static final String TAG = EhClient.class.getSimpleName();

    public static final int METHOD_SIGN_IN = 0;

    private final ThreadPoolExecutor mRequestThreadPool;
    private final OkHttpClient mOkHttpClient;

    public EhClient(Context context) {
        int poolSize = 3;
        BlockingQueue<Runnable> requestWorkQueue = new LinkedBlockingQueue<>();
        ThreadFactory threadFactory = new PriorityThreadFactory(TAG,
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        mRequestThreadPool = new ThreadPoolExecutor(poolSize, poolSize,
                1L, TimeUnit.SECONDS, requestWorkQueue, threadFactory);
        mOkHttpClient = EhApplication.getOkHttpClient(context);
    }

    public void execute(EhRequest request) {
        if (!request.isCancelled()) {
            Task task = new Task(request.method, request.callback);
            task.executeOnExecutor(mRequestThreadPool, request.args);
            request.task = task;
        } else {
            request.callback.onCancel();
        }
    }

    public class Task extends AsyncTask<Object, Void, Object> {

        private int mMethod;
        private Callback mCallback;

        private Call mCall;
        private boolean mStop;

        public Task(int method, Callback callback) {
            mMethod = method;
            mCallback = callback;
        }

        public void stop() {
            if (!mStop) {
                mStop = true;

                if (mCallback != null) {
                    final Callback finalCallback = mCallback;
                    SimpleHandler.getInstance().post(new Runnable() {
                        @Override
                        public void run() {
                            finalCallback.onCancel();
                        }
                    });
                }

                Status status = getStatus();
                if (status == Status.PENDING) {
                    cancel(false);
                } else if (status == Status.RUNNING) {
                    if (mCall != null) {
                        mCall.cancel();
                    }
                }

                // Clear
                mCall = null;
                mCallback = null;
            }
        }

        private Object signIn(Object... params) throws Exception {
            Call call = EhEngine.prepareSignIn(mOkHttpClient, (String) params[0], (String) params[1]);
            if (!mStop) {
                mCall = call;
                return EhEngine.doSignIn(call);
            } else {
                throw new CancelledException();
            }
        }

        @Override
        protected Object doInBackground(Object... params) {
            try {
                switch (mMethod) {
                    case METHOD_SIGN_IN:
                        return signIn(params);
                    default:
                        return new IllegalStateException("Can't detect method " + mMethod);
                }
            } catch (Exception e) {
                return e;
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void onPostExecute(Object result) {
            if (mCallback != null) {
                //noinspection StatementWithEmptyBody
                if (!(result instanceof CancelledException)) {
                    if (result instanceof Exception) {
                        mCallback.onFailure((Exception) result);
                    } else {
                        mCallback.onSuccess(result);
                    }
                } else {
                    // onCancel is called in stop
                }
            }

            // Clear
            mCall = null;
            mCallback = null;
        }
    }

    public interface Callback<E> {

        void onSuccess(E result);

        void onFailure(Exception e);

        void onCancel();
    }
}
