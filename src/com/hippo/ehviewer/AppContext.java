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

package com.hippo.ehviewer;

import java.io.File;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.hippo.ehviewer.data.Data;
import com.hippo.ehviewer.ehclient.EhClient;
import com.hippo.ehviewer.network.HttpHelper;
import com.hippo.ehviewer.util.Cache;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Crash;
import com.hippo.ehviewer.util.Download;
import com.hippo.ehviewer.util.Favorite;
import com.hippo.ehviewer.util.ThreadPool;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.widget.LoadImageView;

import android.app.Application;

public class AppContext extends Application implements UncaughtExceptionHandler {
    
    @SuppressWarnings("unused")
    private static final String TAG = "AppContext";
    private DateFormat mFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    
    private Thread.UncaughtExceptionHandler mDefaultHandler;
    
    private Data mData;
    private EhClient mEhClient;
    private ImageGeterManager mImageGeterManager;
    private ThreadPool mNetworkThreadPool;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Init everything
        mNetworkThreadPool = new ThreadPool(2, 4);
        
        Config.init(this);
        Ui.init(this);
        Cache.init(this);
        Crash.init(this);
        mEhClient = new EhClient(this);
        Download.init(this);
        Favorite.init(this);
        
        mData = new Data(this);
        mImageGeterManager = new ImageGeterManager(this, Cache.memoryCache, Cache.diskCache);
        LoadImageView.setImageGeterManager(mImageGeterManager);
        
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
        
        // Add .nomedia or delete it
        File nomedia = new File(Config.getDownloadPath(), ".nomedia");
        if (Config.getMediaScan()) {
            nomedia.delete();
        } else {
            try {
                nomedia.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
    
    public ImageGeterManager getImageGeterManager() {
        return mImageGeterManager;
    }
    
    public DateFormat getDateFormat() {
        return mFormatter;
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
