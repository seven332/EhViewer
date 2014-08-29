/*
 * Copyright (C) 2014 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.content.Context;

public class Log {
    private static final String NULL = "null";
    private static final String LOG_FILENAME = "log.log";

    private static final Object sLock = new Object();

    private static File sLogFile;

    public static void init(Context context) {
        if (Config.sExternalDir != null)
            sLogFile = new File(Config.sExternalDir, LOG_FILENAME);
        else
            sLogFile = new File(context.getFilesDir(), LOG_FILENAME);
    }

    public static void f(final String msg) {
        new BgThread() {
            @Override
            public void run() {
                synchronized(sLock) {
                    try {
                        FileWriter fw = new FileWriter(sLogFile, true);
                        fw.append(Utils.sDate.format(System.currentTimeMillis()))
                                .append(": ").append(msg).append("\n");
                        fw.flush();
                        fw.close();
                    } catch (IOException e) {}
                }
            }
        }.start();
    }

    public static void f(final Throwable tr) {
        new BgThread() {
            @Override
            public void run() {
                synchronized(sLock) {
                    try {
                        FileWriter fw = new FileWriter(sLogFile, true);
                        fw.append(Utils.sDate.format(System.currentTimeMillis()))
                                .append(": ").append(Crash.getThrowableInfo(tr)).append("\n");
                        fw.flush();
                        fw.close();
                    } catch (IOException e) {}
                }
            }
        }.start();
    }

    public static int v(String tag, String msg) {
        if (msg == null)
            msg = NULL;
        return android.util.Log.v(tag, msg);
    }

    public static int v(String tag, String msg, Throwable tr) {
        if (msg == null)
            msg = NULL;
        return android.util.Log.v(tag, msg, tr);
    }

    public static int d(String tag, String msg) {
        if (msg == null)
            msg = NULL;
        return android.util.Log.d(tag, msg);
    }

    public static int d(String tag, String msg, Throwable tr) {
        if (msg == null)
            msg = NULL;
        return android.util.Log.d(tag, msg, tr);
    }

    public static int i(String tag, String msg) {
        if (msg == null)
            msg = NULL;
        return android.util.Log.i(tag, msg);
    }

    public static int i(String tag, String msg, Throwable tr) {
        if (msg == null)
            msg = NULL;
        return android.util.Log.i(tag, msg, tr);
    }

    public static int w(String tag, String msg) {
        if (msg == null)
            msg = NULL;
        return android.util.Log.w(tag, msg);
    }

    public static int w(String tag, String msg, Throwable tr) {
        if (msg == null)
            msg = NULL;
        return android.util.Log.w(tag, msg, tr);
    }

    public static int w(String tag, Throwable tr) {
        return android.util.Log.w(tag, tr);
    }

    public static int e(String tag, String msg) {
        if (msg == null)
            msg = NULL;
        return android.util.Log.e(tag, msg);
    }

    public static int e(String tag, String msg, Throwable tr) {
        if (msg == null)
            msg = NULL;
        return android.util.Log.e(tag, msg, tr);
    }
}
