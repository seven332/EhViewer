/*
 * Copyright (C) 2014-2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.util;

import android.os.Looper;
import android.os.Process;

import com.hippo.ehviewer.BuildConfig;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Log {

    private static final String TAG = Log.class.getSimpleName();

    private static final String NO_MESSAGE = "No Message";

    private static final SaveLogThreadPool mSaveLogThreadPool;
    private static final Pool<SaveMsgTask> mSaveLogTaskPool;

    private static final File sLogFile = new File("/sdcard/EhViewer/log.log"); // TODO

    static {
        mSaveLogThreadPool = new SaveLogThreadPool();
        mSaveLogTaskPool = new Pool<>(10);
    }

    private static String avoidNull(String str) {
        return str == null ? "null" : str;
    }

    public static void f(String msg) {
        appendSaveLogTask(System.currentTimeMillis(), msg, null);
    }

    public static void f(String msg, Throwable tr) {
        appendSaveLogTask(System.currentTimeMillis(), msg, tr);
    }

    private static void appendSaveLogTask(long time, String msg, Throwable tr) {
        SaveMsgTask task = mSaveLogTaskPool.obtain();
        if (task == null) {
            task = new SaveMsgTask();
        }
        task.setParams(time, msg, tr);

        try {
            mSaveLogThreadPool.execute(task);
        } catch (Throwable t) {
            Log.e("Get error when save message to file", t);
        }
    }

    // TODO Need a better serial thread executor
    private static class SaveLogThreadPool extends ThreadPoolExecutor {

        public SaveLogThreadPool() {
            super(0, 1, 10L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
                    new PriorityThreadFactory("SaveLog", Process.THREAD_PRIORITY_BACKGROUND));
        }

        protected void afterExecute(Runnable r, Throwable t) {
            if (r instanceof SaveMsgTask) {
                SaveMsgTask task = (SaveMsgTask) r;
                task.clear();
                mSaveLogTaskPool.recycle(task);
            }
        }
    }

    private static class SaveMsgTask implements Runnable {

        private long mTime;
        private String mMsg;
        private Throwable mTr;

        public void clear() {
            setParams(0, null, null);
        }

        public void setParams(long time, String msg, Throwable tr) {
            mTime = time;
            mMsg = msg;
            mTr = tr;
        }

        @Override
        public void run() {

            Log.d("MainLooper thread is " + Looper.getMainLooper().getThread().toString());
            Log.d("This thread is " + Thread.currentThread().toString());

            FileWriter fw = null;
            try {
                fw = new FileWriter(sLogFile, true);
                fw.append(Long.toString(mTime)).append(" : ").append(mMsg).append("\n")
                        .append(Log.getStackTraceString(mTr));
                fw.flush(); // TODO Only do flush when the thread is about to close
            } catch (IOException e) {
                Log.e("Get error when save message to file", e);
                Utils.closeQuietly(fw);
            }
        }
    }

    /**
     * Call {@link android.util.Log#v(String, String)} with default tag.
     * @param msg The message you would like logged.
     */
    public static int v(String msg) {
        return v(TAG, msg);
    }

    /**
     * It is {@link android.util.Log#v(String, String)}, only work when debug.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int v(String tag, String msg) {
        if (BuildConfig.DEBUG) {
            return android.util.Log.v(tag, avoidNull(msg));
        } else {
            return 0;
        }
    }

    /**
     * Call {@link android.util.Log#v(String, String, Throwable)} with default tag
     * and no message.
     * @param tr An exception to log
     */
    public static int v(Throwable tr) {
        return v(TAG, NO_MESSAGE, tr);
    }

    /**
     * Call {@link android.util.Log#v(String, String, Throwable)} with default tag.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static int v(String msg, Throwable tr) {
        return v(TAG, msg, tr);
    }

    /**
     * It is {@link android.util.Log#v(String, String, Throwable)}, only work when debug.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static int v(String tag, String msg, Throwable tr) {
        if (BuildConfig.DEBUG) {
            return android.util.Log.v(tag, avoidNull(msg), tr);
        } else {
            return 0;
        }
    }

    /**
     * Call {@link android.util.Log#d(String, String)} with default tag.
     * @param msg The message you would like logged.
     */
    public static int d(String msg) {
        return d(TAG, msg);
    }

    /**
     * It is {@link android.util.Log#d(String, String)}, only work when debug.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int d(String tag, String msg) {
        if (BuildConfig.DEBUG) {
            return android.util.Log.d(tag, avoidNull(msg));
        } else {
            return 0;
        }
    }

    /**
     * Call {@link android.util.Log#d(String, String, Throwable)} with default tag
     * and no message.
     * @param tr An exception to log
     */
    public static int d(Throwable tr) {
        return d(TAG, NO_MESSAGE, tr);
    }

    /**
     * Call {@link android.util.Log#d(String, String, Throwable)} with default tag.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static int d(String msg, Throwable tr) {
        return d(TAG, msg, tr);
    }

    /**
     * It is {@link android.util.Log#d(String, String, Throwable)}, only work when debug.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static int d(String tag, String msg, Throwable tr) {
        if (BuildConfig.DEBUG) {
            return android.util.Log.d(tag, avoidNull(msg), tr);
        } else {
            return 0;
        }
    }

    /**
     * Call {@link android.util.Log#i(String, String)} with default tag.
     * @param msg The message you would like logged.
     */
    public static int i(String msg) {
        return i(TAG, msg);
    }

    /**
     * It is {@link android.util.Log#i(String, String)}, only work when debug.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int i(String tag, String msg) {
        if (BuildConfig.DEBUG) {
            return android.util.Log.i(tag, avoidNull(msg));
        } else {
            return 0;
        }
    }

    /**
     * Call {@link android.util.Log#i(String, String, Throwable)} with default tag
     * and no message.
     * @param tr An exception to log
     */
    public static int i(Throwable tr) {
        return i(TAG, NO_MESSAGE, tr);
    }

    /**
     * Call {@link android.util.Log#i(String, String, Throwable)} with default tag.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static int i(String msg, Throwable tr) {
        return i(TAG, msg, tr);
    }

    /**
     * It is {@link android.util.Log#i(String, String, Throwable)}, only work when debug.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static int i(String tag, String msg, Throwable tr) {
        if (BuildConfig.DEBUG) {
            return android.util.Log.i(tag, avoidNull(msg), tr);
        } else {
            return 0;
        }
    }

    /**
     * Call {@link android.util.Log#w(String, String)} with default tag.
     * @param msg The message you would like logged.
     */
    public static int w(String msg) {
        return w(TAG, msg);
    }

    /**
     * It is {@link android.util.Log#w(String, String)}, only work when debug.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int w(String tag, String msg) {
        if (BuildConfig.DEBUG) {
            return android.util.Log.w(tag, avoidNull(msg));
        } else {
            return 0;
        }
    }

    /**
     * Call {@link android.util.Log#w(String, String, Throwable)} with default tag
     * and no message.
     * @param tr An exception to log
     */
    public static int w(Throwable tr) {
        return w(TAG, NO_MESSAGE, tr);
    }

    /**
     * Call {@link android.util.Log#w(String, String, Throwable)} with default tag.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static int w(String msg, Throwable tr) {
        return w(TAG, msg, tr);
    }

    /**
     * It is {@link android.util.Log#w(String, String, Throwable)}, only work when debug.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static int w(String tag, String msg, Throwable tr) {
        if (BuildConfig.DEBUG) {
            return android.util.Log.w(tag, avoidNull(msg), tr);
        } else {
            return 0;
        }
    }

    /**
     * Call {@link android.util.Log#e(String, String)} with default tag.
     * @param msg The message you would like logged.
     */
    public static int e(String msg) {
        return e(TAG, msg);
    }

    /**
     * It is {@link android.util.Log#e(String, String)}, only work when debug.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int e(String tag, String msg) {
        if (BuildConfig.DEBUG) {
            return android.util.Log.e(tag, avoidNull(msg));
        } else {
            return 0;
        }
    }

    /**
     * Call {@link android.util.Log#e(String, String, Throwable)} with default tag
     * and no message.
     * @param tr An exception to log
     */
    public static int e(Throwable tr) {
        return e(TAG, NO_MESSAGE, tr);
    }

    /**
     * Call {@link android.util.Log#e(String, String, Throwable)} with default tag.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static int e(String msg, Throwable tr) {
        return e(TAG, msg, tr);
    }

    /**
     * It is {@link android.util.Log#e(String, String, Throwable)}, only work when debug.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static int e(String tag, String msg, Throwable tr) {
        if (BuildConfig.DEBUG) {
            return android.util.Log.e(tag, avoidNull(msg), tr);
        } else {
            return 0;
        }
    }

    /**
     * It is {@link android.util.Log#getStackTraceString(Throwable)}.<br>
     * Handy function to get a loggable stack trace from a Throwable
     * @param tr An exception to log
     */
    public static String getStackTraceString(Throwable tr) {
        return android.util.Log.getStackTraceString(tr);
    }
}
