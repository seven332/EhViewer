/*
 * Copyright 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer;

import android.app.ActivityManager;
import android.content.Context;

import com.hippo.beerbelly.SimpleDiskCache;
import com.hippo.conaco.Conaco;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.gallery.GallerySpider;
import com.hippo.ehviewer.gallery.ImageHandler;
import com.hippo.ehviewer.gallery.ui.TouchHelper;
import com.hippo.ehviewer.network.EhHttpClient;
import com.hippo.ehviewer.service.DownloadManager;
import com.hippo.ehviewer.util.DBUtils;
import com.hippo.ehviewer.util.Settings;
import com.hippo.scene.SceneApplication;
import com.hippo.vectorold.content.VectorContext;
import com.hippo.yorozuya.FileUtils;
import com.hippo.yorozuya.Say;

import java.io.File;
import java.io.IOException;

public class EhApplication extends SceneApplication {

    private static final String TAG = EhApplication.class.getSimpleName();

    private EhHttpClient mEhHttpClient;
    private EhClient mEhClient;
    private Conaco mConaco;

    @Override
    public void onCreate() {
        super.onCreate();

        Settings.initialize(this);
        DBUtils.initialize(this);
        TouchHelper.initialize(this);
        DownloadManager.initialize();

        File sayFile = AppConfig.getFileInAppDir("EhViewer.log");
        if (sayFile != null) {
            Say.initSayFile(sayFile);
        }

        mEhHttpClient = new EhHttpClient(this);
        mEhClient = new EhClient(mEhHttpClient);

        Conaco.Builder conacoBuilder = new Conaco.Builder();
        conacoBuilder.hasMemoryCache = true;
        conacoBuilder.memoryCacheMaxSize = getMemoryCacheMaxSize();
        conacoBuilder.hasDiskCache = true;
        conacoBuilder.diskCacheDir = new File(getCacheDir(), "conaco");
        conacoBuilder.diskCacheMaxSize = 50 * 1024 * 1024;
        conacoBuilder.httpClient = mEhHttpClient;
        mConaco = conacoBuilder.build();

        SimpleDiskCache readcache = null;
        try {
            readcache = new SimpleDiskCache(new File(getCacheDir(), "read"), 200 * 1024 * 1024);
        } catch (IOException e) {
            // TODO need a better idea
            e.printStackTrace();
        }
        ImageHandler.init(readcache);

        File spiderInfoDir = getDir("spiderInfo", Context.MODE_PRIVATE);
        if (!FileUtils.ensureDirectory(spiderInfoDir)) {
            // TODO Can't create spider info dir
        }
        GallerySpider.init(mEhHttpClient, spiderInfoDir);

        Settings.getImageDownloadLocation().ensureDir();
        Settings.getArchiveDownloadLocation().ensureDir();


        Settings.getImageDownloadLocation().listFiles();
    }

    private int getMemoryCacheMaxSize() {
        final ActivityManager activityManager = (ActivityManager)
                getSystemService(Context.ACTIVITY_SERVICE);
        return Math.min(20 * 1024 * 1024,
                Math.round(0.2f * activityManager.getMemoryClass() * 1024 * 1024));
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(VectorContext.wrapContext(newBase));
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        if (level == TRIM_MEMORY_UI_HIDDEN) {
            Say.d(TAG, "Application hiden");
        }
    }

    public static EhHttpClient getEhHttpClient(Context context) {
        EhApplication application = (EhApplication) context.getApplicationContext();
        return application.mEhHttpClient;
    }

    public static EhClient getEhClient(Context context) {
        EhApplication application = (EhApplication) context.getApplicationContext();
        return application.mEhClient;
    }

    public static Conaco getConaco(Context context) {
        EhApplication application = (EhApplication) context.getApplicationContext();
        return application.mConaco;
    }
}
