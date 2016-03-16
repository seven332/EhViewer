/*
 * Copyright 2016 Hippo Seven
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
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Debug;
import android.support.annotation.NonNull;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.util.SparseArray;

import com.hippo.beerbelly.SimpleDiskCache;
import com.hippo.conaco.Conaco;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.client.EhCookieStore;
import com.hippo.ehviewer.client.data.GalleryDetail;
import com.hippo.ehviewer.client.data.LargePreviewSet;
import com.hippo.ehviewer.download.DownloadManager;
import com.hippo.ehviewer.spider.SpiderDen;
import com.hippo.network.StatusCodeException;
import com.hippo.okhttp.CookieDB;
import com.hippo.scene.SceneApplication;
import com.hippo.text.Html;
import com.hippo.util.ReadableTime;
import com.hippo.yorozuya.FileUtils;
import com.hippo.yorozuya.SimpleHandler;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.OkHttpClient;

public class EhApplication extends SceneApplication implements Thread.UncaughtExceptionHandler {

    private static final String TAG = EhApplication.class.getSimpleName();

    public static final boolean BETA = true;

    private static final boolean DEBUG_CONACO = false;
    private static final boolean DEBUG_NATIVE_MEMORY = false;

    private Thread.UncaughtExceptionHandler mDefaultHandler;

    private final AtomicInteger mIdGenerator = new AtomicInteger();
    private final SparseArray<Object> mGlobalStuffMap = new SparseArray<>();
    private EhCookieStore mEhCookieStore;
    private EhClient mEhClient;
    private OkHttpClient mOkHttpClient;
    private BitmapHelper mBitmapHelper;
    private Conaco<Bitmap> mConaco;
    private LruCache<Long, GalleryDetail> mGalleryDetailCache;
    private LruCache<String, LargePreviewSet> mLargePreviewSetCache;
    private LruCache<Long, Integer> mPreviewPagesCache;
    private SimpleDiskCache mSpiderInfoCache;
    private DownloadManager mDownloadManager;

    @Override
    public void onCreate() {
        // Prepare to catch crash
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);

        super.onCreate();

        GetText.initialize(this);
        CookieDB.initialize(this);
        StatusCodeException.initialize(this);
        Settings.initialize(this);
        ReadableTime.initialize(this);
        Html.initialize(this);
        AppConfig.initialize(this);
        SpiderDen.initialize(this);
        EhDB.initialize(this);

        if (EhDB.needMerge()) {
            EhDB.mergeOldDB(this);
        }

        if (Settings.getEnableAnalytics()) {
            Analytics.start(this);
        }

        // Update version code
        try {
            PackageInfo pi= getPackageManager().getPackageInfo(getPackageName(), 0);
            Settings.putVersionCode(pi.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            // Ignore
        }

        if (DEBUG_NATIVE_MEMORY) {
            debugNativeMemory();
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            if (null != mConaco) {
                mConaco.clearMemoryCache();
            }
            if (null != mGalleryDetailCache) {
                mGalleryDetailCache.evictAll();
            }
            if (null != mLargePreviewSetCache) {
                mLargePreviewSetCache.evictAll();
            }
            if (null != mPreviewPagesCache) {
                mPreviewPagesCache.evictAll();
            }
        }
    }

    private void debugNativeMemory() {
        new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "Native memory: " + FileUtils.humanReadableByteCount(
                        Debug.getNativeHeapAllocatedSize(), false));
                SimpleHandler.getInstance().postDelayed(this, 3000);
            }
        }.run();
    }

    public int putGlobalStuff(@NonNull Object o) {
        int id = mIdGenerator.getAndDecrement();
        mGlobalStuffMap.put(id, o);
        return id;
    }

    public boolean containGlobalStuff(int id) {
        return mGlobalStuffMap.indexOfKey(id) >= 0;
    }

    public Object getGlobalStuff(int id) {
        return mGlobalStuffMap.get(id);
    }

    public Object removeGlobalStuff(int id) {
        int index = mGlobalStuffMap.indexOfKey(id);
        if (index >= 0) {
            Object o = mGlobalStuffMap.valueAt(index);
            mGlobalStuffMap.removeAt(index);
            return o;
        } else {
            return null;
        }
    }

    public boolean removeGlobalStuff(Object o) {
        int index = mGlobalStuffMap.indexOfValue(o);
        if (index >= 0) {
            mGlobalStuffMap.removeAt(index);
            return true;
        } else {
            return false;
        }
    }

    public static EhCookieStore getEhCookieStore(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        if (application.mEhCookieStore == null) {
            application.mEhCookieStore = new EhCookieStore();
        }
        return application.mEhCookieStore;
    }

    @NonNull
    public static EhClient getEhClient(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        if (application.mEhClient == null) {
            application.mEhClient = new EhClient(application);
        }
        return application.mEhClient;
    }

    @NonNull
    public static OkHttpClient getOkHttpClient(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        if (application.mOkHttpClient == null) {
            application.mOkHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .cookieJar(getEhCookieStore(application))
                    .build();
        }
        return application.mOkHttpClient;
    }

    @NonNull
    public static BitmapHelper getBitmapHelper(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        if (application.mBitmapHelper == null) {
            application.mBitmapHelper = new BitmapHelper();
        }
        return application.mBitmapHelper;
    }

    private static int getMemoryCacheMaxSize(Context context) {
        final ActivityManager activityManager = (ActivityManager) context.
                getSystemService(Context.ACTIVITY_SERVICE);
        return Math.min(20 * 1024 * 1024,
                Math.round(0.2f * activityManager.getMemoryClass() * 1024 * 1024));
    }

    @NonNull
    public static Conaco<Bitmap> getConaco(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        if (application.mConaco == null) {
            Conaco.Builder<Bitmap> builder = new Conaco.Builder<>();
            builder.hasMemoryCache = true;
            builder.memoryCacheMaxSize = getMemoryCacheMaxSize(context);
            builder.hasDiskCache = true;
            builder.diskCacheDir = new File(context.getCacheDir(), "thumb");
            builder.diskCacheMaxSize = 80 * 1024 * 1024; // 80MB
            builder.okHttpClient = getOkHttpClient(context);
            builder.objectHelper = getBitmapHelper(context);
            builder.debug = DEBUG_CONACO;
            application.mConaco = builder.build();
        }
        return application.mConaco;
    }

    @NonNull
    public static LruCache<Long, GalleryDetail> getGalleryDetailCache(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        if (application.mGalleryDetailCache == null) {
            // Max size 25, 3 min timeout
            application.mGalleryDetailCache = new LruCache<>(25);
        }
        return application.mGalleryDetailCache;
    }

    @NonNull
    public static LruCache<String, LargePreviewSet> getLargePreviewSetCache(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        if (application.mLargePreviewSetCache == null) {
            // Max size 50, 3 min timeout
            application.mLargePreviewSetCache = new LruCache<>(50);
        }
        return application.mLargePreviewSetCache;
    }

    @NonNull
    public static LruCache<Long, Integer> getPreviewPagesCache(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        if (application.mPreviewPagesCache == null) {
            // Max size 50, 3 min timeout
            application.mPreviewPagesCache = new LruCache<>(50);
        }
        return application.mPreviewPagesCache;
    }

    @NonNull
    public static SimpleDiskCache getSpiderInfoCache(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        if (null == application.mSpiderInfoCache) {
            application.mSpiderInfoCache = new SimpleDiskCache(
                    new File(context.getCacheDir(), "spider_info"), 5 * 1024 * 1024); // 5M
        }
        return application.mSpiderInfoCache;
    }

    @NonNull
    public static DownloadManager getDownloadManager(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        if (application.mDownloadManager == null) {
            application.mDownloadManager = new DownloadManager(application);
        }
        return application.mDownloadManager;
    }

    private boolean handleException(Throwable ex) {
        if (ex == null) {
            return false;
        }
        try {
            ex.printStackTrace();
            Crash.saveCrashInfo2File(this, ex);
            return true;
        } catch (Throwable tr) {
            return false;
        }
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        if (!handleException(ex) && mDefaultHandler != null) {
            mDefaultHandler.uncaughtException(thread, ex);
        }
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
    }

    @NonNull
    public static String getDeveloperEmail() {
        return "ehviewersu$gmail.com".replace('$', '@');
    }
}
