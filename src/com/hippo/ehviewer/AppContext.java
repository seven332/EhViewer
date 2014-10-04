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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;

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
import com.hippo.ehviewer.util.EhUtils;
import com.hippo.ehviewer.util.Favorite;
import com.hippo.ehviewer.util.Log;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.util.Utils;
import com.hippo.ehviewer.widget.MaterialToast;

public class AppContext extends Application implements UncaughtExceptionHandler {

    @SuppressWarnings("unused")
    private static final String TAG = "AppContext";

    public static final boolean DEBUG = true;

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
        Log.init(this);
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

        // Fix <=22 login error
        if (Config.getVersionCode() <= 22)
            EhInfo.getInstance(this).logout();

        // Fix <=25 Update
        if (Config.getVersionCode() <= 25) {
            File downloadDir = new File(Config.getDownloadPath());
            String cachePath = getExternalCacheDir() != null
                    ? getExternalCacheDir().getPath()
                    : getCacheDir().getPath();
            File oldEdInfoDir = new File(cachePath, "ExDownloaderManager");
            String[] list = downloadDir.list();
            if (list != null && list.length != 0) {
                for (String str : list) {
                    // Change download dir name from gid-title to gid
                    File oldDir = new File(downloadDir, str);
                    if (!new File(oldDir, EhUtils.EH_DOWNLOAD_FILENAME).exists())
                        continue;
                    int index = str.indexOf('-');
                    if (index == -1)
                        continue;
                    String gid = str.substring(0, index);
                    File newDir = new File(downloadDir, gid);
                    if (!oldDir.renameTo(newDir)) {
                        // if delete error, might same gid and different title
                        Utils.deleteDirInThread(oldDir);
                        continue;
                    }
                    // Move download info file
                    File oldInfoFile = new File(oldEdInfoDir, gid);
                    File newInfoFile = new File(newDir, EhUtils.EH_DOWNLOAD_FILENAME);
                    try {
                        FileOutputStream fos = new FileOutputStream(newInfoFile);
                        // Add last page
                        byte[] bs = {'0', '0', '0', '0', '0', '0', '0', '0', '\n'};
                        fos.write(bs);
                        Utils.copy(new FileInputStream(oldInfoFile), fos);
                    } catch (Throwable e) {
                        continue;
                    }
                }
            }
            // Delete old exdownload info dir
            Utils.deleteDirInThread(oldEdInfoDir);
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
