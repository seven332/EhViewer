package com.hippo.ehviewer.util;

public class Log {
    private static final String NULL = "null";
    
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
