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

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Typeface;
import android.view.ContextThemeWrapper;

import com.hippo.ehviewer.data.Data;
import com.hippo.ehviewer.ehclient.EhClient;
import com.hippo.ehviewer.ehclient.EhInfo;
import com.hippo.ehviewer.ehclient.ExDownloaderManager;
import com.hippo.ehviewer.network.HttpHelper;
import com.hippo.ehviewer.cache.ImageCache;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Crash;
import com.hippo.ehviewer.util.EhUtils;
import com.hippo.ehviewer.util.Favorite;
import com.hippo.ehviewer.util.Log;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.util.Utils;
import com.hippo.ehviewer.util.ViewUtils;
import com.hippo.ehviewer.widget.MaterialToast;
import com.hippo.ehviewer.widget.SlidingDrawerLayout;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;

public class AppContext extends Application implements UncaughtExceptionHandler {

    @SuppressWarnings("unused")
    private static final String TAG = AppContext.class.getSimpleName();

    private Thread.UncaughtExceptionHandler mDefaultHandler;

    private Typeface mFaceTypeface;

    @Override
    public void onCreate() {
        super.onCreate();

        // Do catch error prepare
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);

        Context mContextThemeWrapper = new ContextThemeWrapper(this, R.style.AppTheme);
        ViewUtils.initDialogViewInflater(this);

        // Init everything
        Ui.init(mContextThemeWrapper);
        Config.init(mContextThemeWrapper);
        Log.init(mContextThemeWrapper);
        Crash.init(mContextThemeWrapper);
        EhClient.createInstance(mContextThemeWrapper);
        Favorite.init(mContextThemeWrapper);
        Data.createInstance(mContextThemeWrapper);
        ExDownloaderManager.createInstance(mContextThemeWrapper);
        MaterialToast.setContext(mContextThemeWrapper);

        mFaceTypeface = Typeface.createFromAsset(getAssets(),"fonts/face.ttf");

        // Set drawer margin
        SlidingDrawerLayout.setDefaultMinDrawerMargin(Ui.ACTION_BAR_HEIGHT * 2);

        // Add .nomedia or delete it
        File nomedia = new File(Config.getDownloadPath(), ".nomedia");
        if (Config.getMediaScan()) {
            nomedia.delete();
        } else {
            try {
                new FileOutputStream(nomedia).close();
            } catch (IOException e) {}
        }

        // Update proxy urls
        HttpHelper.updateProxyUrls(mContextThemeWrapper);

        String keyVersionCode = "version_code";
        int oldVersion = Config.getInt(keyVersionCode, Integer.MAX_VALUE);
        // Fix <=22 login error
        if (oldVersion <= 22)
            EhInfo.getInstance(mContextThemeWrapper).logout();

        // Fix <=25 Update
        if (oldVersion <= 25) {
            File downloadDir = new File(Config.getDownloadPath());
            String cachePath = getExternalCacheDir() != null
                    ? getExternalCacheDir().getPath()
                    : getCacheDir().getPath();
            File oldEdInfoDir = new File(cachePath, "ExDownloaderManager");
            String[] list = downloadDir.list();
            if (list != null && list.length != 0) {
                for (String str : list) {
                    int index = str.indexOf('-');
                    if (index == -1)
                        continue;
                    String gid = str.substring(0, index);
                    File gDir = new File(downloadDir, str);
                    // Move download info file
                    File oldInfoFile = new File(oldEdInfoDir, gid);
                    File newInfoFile = new File(gDir, EhUtils.EH_DOWNLOAD_FILENAME);
                    if (!oldInfoFile.exists())
                        continue;
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
            Config.setInt(keyVersionCode, pi.versionCode);
        } catch (NameNotFoundException e) {}
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLowMemory() {
        ImageCache.getImageCache(this).evictAll();
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

    public Typeface getFaceTypeface() {
        return mFaceTypeface;
    }
}
