package com.hippo.ehviewer;

import java.lang.Thread.UncaughtExceptionHandler;

import com.hippo.ehviewer.data.Data;
import com.hippo.ehviewer.network.HttpHelper;
import com.hippo.ehviewer.util.Cache;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Crash;
import com.hippo.ehviewer.util.Download;
import com.hippo.ehviewer.util.EhClient;
import com.hippo.ehviewer.util.ThreadPool;
import com.hippo.ehviewer.util.Ui;

import android.app.Application;

public class AppContext extends Application implements UncaughtExceptionHandler {
    
    @SuppressWarnings("unused")
    private static final String TAG = "AppContext";
    
    private Thread.UncaughtExceptionHandler mDefaultHandler;
    
    private Data mData;
    private EhClient mEhClient;
    
    private ThreadPool mNetworkThreadPool;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Init everything
        mNetworkThreadPool = new ThreadPool(1, 2);
        
        Config.init(this);
        Ui.init(this);
        Cache.init(this);
        Crash.init(this);
        BeautifyScreen.init(this);
        mEhClient = new EhClient(this);
        Download.init(this);
        
        mData = new Data(this);
        HttpHelper.setCookieHelper(this);
        
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }
    
    public EhClient getEhClient() {
        return mEhClient;
    }
    
    public ThreadPool getNetworkThreadPool() {
        return mNetworkThreadPool;
    }
    
    public Data getData() {
        return mData;
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
