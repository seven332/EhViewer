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
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;

import com.hippo.ehviewer.cache.ImageCache;
import com.hippo.ehviewer.data.Data;
import com.hippo.ehviewer.ehclient.EhClient;
import com.hippo.ehviewer.ehclient.EhInfo;
import com.hippo.ehviewer.ehclient.ExDownloaderManager;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Crash;
import com.hippo.ehviewer.util.Favorite;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.widget.MaterialToast;

public class AppContext extends Application implements UncaughtExceptionHandler {

    @SuppressWarnings("unused")
    private static final String TAG = "AppContext";
    @SuppressLint("SimpleDateFormat")
    public static final DateFormat sFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private Thread.UncaughtExceptionHandler mDefaultHandler;

    private static AppContext sInstance;

    public static AppContext getInstance() {
        return sInstance;
    }

    public AppContext() {
        sInstance = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Init everything
        Config.init(this);
        Ui.init(this);
        Crash.init(this);
        EhClient.createInstance(this);
        Favorite.init(this);
        Data.createInstance(this);
        ExDownloaderManager.createInstance();
        MaterialToast.setContext(this);

        // Do catch error prepare
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);

        // Add .nomedia or delete it
        File nomedia = new File(Config.getDownloadPath(), ".nomedia");
        if (Config.getMediaScan()) {
            nomedia.delete();
        } else {
            try {
                new FileOutputStream(nomedia).close();
            } catch (IOException e) {}
        }

        // Fix <=22 error
        if (Config.getVersionCode() <= 22) {
            EhInfo.getInstance(this).logout();
        }


        // Update version code
        try {
            PackageInfo pi= getPackageManager().getPackageInfo(getPackageName(), 0);
            Config.setVersionCode(pi.versionCode);
        } catch (NameNotFoundException e) {}
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLowMemory() {
        ImageCache.getInstance(this).evictAll();
        super.onLowMemory();
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
