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

import com.hippo.ehviewer.BuildConfig;

public class Log {

    private static final String TAG = Log.class.getSimpleName();

    private static final String avoidNull(String str) {
        return str == null ? TextUtils.STRING_NULL : str;
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
     * It is {@link android.util.Log#w(String, Throwable)}, only work when debug.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param tr An exception to log
     */
    public static int w(String tag, Throwable tr) {
        if (BuildConfig.DEBUG) {
            return android.util.Log.w(tag, tr);
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
