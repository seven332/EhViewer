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

import android.app.Activity;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Debug;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.LruCache;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;
import com.hippo.beerbelly.SimpleDiskCache;
import com.hippo.conaco.Conaco;
import com.hippo.content.RecordingApplication;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.client.EhCookieStore;
import com.hippo.ehviewer.client.EhDns;
import com.hippo.ehviewer.client.EhEngine;
import com.hippo.ehviewer.client.data.GalleryDetail;
import com.hippo.ehviewer.download.DownloadManager;
import com.hippo.ehviewer.spider.SpiderDen;
import com.hippo.ehviewer.ui.CommonOperations;
import com.hippo.image.Image;
import com.hippo.image.ImageBitmap;
import com.hippo.network.StatusCodeException;
import com.hippo.text.Html;
import com.hippo.unifile.UniFile;
import com.hippo.util.BitmapUtils;
import com.hippo.util.ReadableTime;
import com.hippo.yorozuya.FileUtils;
import com.hippo.yorozuya.IntIdGenerator;
import com.hippo.yorozuya.OSUtils;
import com.hippo.yorozuya.SimpleHandler;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;

public class EhApplication extends RecordingApplication implements Thread.UncaughtExceptionHandler {

    private static final String TAG = EhApplication.class.getSimpleName();

    public static final boolean BETA = false;

    private static final boolean DEBUG_CONACO = false;
    private static final boolean DEBUG_PRINT_NATIVE_MEMORY = false;
    private static final boolean DEBUG_PRINT_IMAGE_COUNT = false;
    private static final long DEBUG_PRINT_INTERVAL = 3000L;

    private Thread.UncaughtExceptionHandler mDefaultHandler;

    private final IntIdGenerator mIdGenerator = new IntIdGenerator();
    private final HashMap<Integer, Object> mGlobalStuffMap = new HashMap<>();
    private EhCookieStore mEhCookieStore;
    private EhClient mEhClient;
    private OkHttpClient mOkHttpClient;
    private ImageBitmapHelper mImageBitmapHelper;
    private Conaco<ImageBitmap> mConaco;
    private LruCache<Long, GalleryDetail> mGalleryDetailCache;
    private SimpleDiskCache mSpiderInfoCache;
    private DownloadManager mDownloadManager;
    private Hosts mHosts;

    private final List<Activity> mActivityList = new ArrayList<>();

    @Override
    public void onCreate() {
        // Prepare to catch crash
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);

        super.onCreate();

        GetText.initialize(this);
        StatusCodeException.initialize(this);
        Settings.initialize(this);
        ReadableTime.initialize(this);
        Html.initialize(this);
        AppConfig.initialize(this);
        SpiderDen.initialize(this);
        EhDB.initialize(this);
        EhEngine.initialize();
        BitmapUtils.initialize(this);

        setNightMode();

        if (EhDB.needMerge()) {
            EhDB.mergeOldDB(this);
        }

        if (Settings.getEnableAnalytics()) {
            Analytics.start(this);
        }

        // Check no media file
        UniFile downloadLocation = Settings.getDownloadLocation();
        if (Settings.getMediaScan()) {
            CommonOperations.removeNoMediaFile(downloadLocation);
        } else {
            CommonOperations.ensureNoMediaFile(downloadLocation);
        }

        // Clear temp dir
        clearTempDir();

        // Check app update
        update();

        // Update version code
        try {
            PackageInfo pi= getPackageManager().getPackageInfo(getPackageName(), 0);
            Settings.putVersionCode(pi.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            // Ignore
        }

        if (DEBUG_PRINT_NATIVE_MEMORY || DEBUG_PRINT_IMAGE_COUNT) {
            debugPrint();
        }
    }

    private void setNightMode(){
        int mode = Settings.getNightMode();
        if (AppCompatDelegate.getDefaultNightMode() != mode){
            AppCompatDelegate.setDefaultNightMode(mode);
        }
    }

    private void clearTempDir() {
        File dir = AppConfig.getTempDir();
        if (null != dir) {
            FileUtils.deleteContent(dir);
        }
        dir = AppConfig.getExternalTempDir();
        if (null != dir) {
            FileUtils.deleteContent(dir);
        }

        // Add .nomedia to external temp dir
        CommonOperations.ensureNoMediaFile(UniFile.fromFile(AppConfig.getExternalTempDir()));
    }

    private void update() {
        int version = Settings.getVersionCode();
        if (version < 52) {
            Settings.putGuideGallery(true);
        }
    }

    public void clearMemoryCache() {
        if (null != mConaco) {
            mConaco.getBeerBelly().clearMemory();
        }
        if (null != mGalleryDetailCache) {
            mGalleryDetailCache.evictAll();
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            clearMemoryCache();
        }
    }

    private void debugPrint() {
        new Runnable() {
            @Override
            public void run() {
                if (DEBUG_PRINT_NATIVE_MEMORY) {
                    Log.i(TAG, "Native memory: " + FileUtils.humanReadableByteCount(
                            Debug.getNativeHeapAllocatedSize(), false));
                }
                if (DEBUG_PRINT_IMAGE_COUNT) {
                    Log.i(TAG, "Image count: " + Image.getImageCount());
                }
                SimpleHandler.getInstance().postDelayed(this, DEBUG_PRINT_INTERVAL);
            }
        }.run();
    }

    public int putGlobalStuff(@NonNull Object o) {
        int id = mIdGenerator.nextId();
        mGlobalStuffMap.put(id, o);
        return id;
    }

    public boolean containGlobalStuff(int id) {
        return mGlobalStuffMap.containsKey(id);
    }

    public Object getGlobalStuff(int id) {
        return mGlobalStuffMap.get(id);
    }

    public Object removeGlobalStuff(int id) {
        return mGlobalStuffMap.remove(id);
    }

    public boolean removeGlobalStuff(Object o) {
        return mGlobalStuffMap.values().removeAll(Collections.singleton(o));
    }

    public static EhCookieStore getEhCookieStore(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        if (application.mEhCookieStore == null) {
            application.mEhCookieStore = new EhCookieStore(context);
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
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .cookieJar(getEhCookieStore(application))
                    .dns(new EhDns(application))
                    .build();
        }
        return application.mOkHttpClient;
    }

    @NonNull
    public static ImageBitmapHelper getImageBitmapHelper(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        if (application.mImageBitmapHelper == null) {
            application.mImageBitmapHelper = new ImageBitmapHelper();
        }
        return application.mImageBitmapHelper;
    }

    private static int getMemoryCacheMaxSize() {
        return Math.min(20 * 1024 * 1024, (int) OSUtils.getAppMaxMemory());
    }

    @NonNull
    public static Conaco<ImageBitmap> getConaco(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        if (application.mConaco == null) {
            Conaco.Builder<ImageBitmap> builder = new Conaco.Builder<>();
            builder.hasMemoryCache = true;
            builder.memoryCacheMaxSize = getMemoryCacheMaxSize();
            builder.hasDiskCache = true;
            builder.diskCacheDir = new File(context.getCacheDir(), "thumb");
            builder.diskCacheMaxSize = 80 * 1024 * 1024; // 80MB
            builder.okHttpClient = getOkHttpClient(context);
            builder.objectHelper = getImageBitmapHelper(context);
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

    @NonNull
    public static Hosts getHosts(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        if (application.mHosts == null) {
            application.mHosts = new Hosts(application, "hosts.db");
        }
        return application.mHosts;
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

        Activity activity = getTopActivity();
        if (activity != null) {
            activity.finish();
        }

        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
    }

    @NonNull
    public static String getDeveloperEmail() {
        return "ehviewersu$gmail.com".replace('$', '@');
    }

    public void registerActivity(Activity activity) {
        mActivityList.add(activity);
    }

    public void unregisterActivity(Activity activity) {
        mActivityList.remove(activity);
    }

    @Nullable
    public Activity getTopActivity() {
        if (!mActivityList.isEmpty()) {
            return mActivityList.get(mActivityList.size() - 1);
        } else {
            return null;
        }
    }
}
