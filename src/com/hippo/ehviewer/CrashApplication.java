package com.hippo.ehviewer;

import java.lang.Thread.UncaughtExceptionHandler;

import com.hippo.ehviewer.util.Cache;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Crash;
import com.hippo.ehviewer.util.EhClient;
import com.hippo.ehviewer.util.Favourite;
import com.hippo.ehviewer.util.Tag;
import com.hippo.ehviewer.util.Ui;

import android.app.Application;

public class CrashApplication extends Application implements UncaughtExceptionHandler {
    
    private static final String TAG = "CrashApplication";
    
    private Thread.UncaughtExceptionHandler mDefaultHandler;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // TODO Init everything
        Config.init(this);
        Ui.init(this);
        Cache.init(this);
        Favourite.init(this);
        Tag.init(this);
        Crash.init(this);
        BeautifyScreen.init(this);
        EhClient.init(this);
        
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }
    
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        if (!handleException(ex) && mDefaultHandler != null) {
            mDefaultHandler.uncaughtException(thread, ex);
        }
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
    }
    
    private boolean handleException(Throwable ex) {
        if (ex == null)
            return false;
        Crash.saveCrashInfo2File(ex);
        return true;
    }
}
