package com.hippo.dict.util;

import android.util.Log;

/**
 * Created by axlecho on 2016/4/8.
 */
public class DictLog {

    static private boolean isTrace = true;

    static public void t(String tag, String msg) {
        if (!isTrace) return;
        Log.d(tag, msg);
    }
}
