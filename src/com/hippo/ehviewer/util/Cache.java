package com.hippo.ehviewer.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Environment;
import android.support.v4.util.LruCache;

import com.hippo.ehviewer.DiskCache;
import com.hippo.ehviewer.MangaDetail;

public class Cache {
    private static final String TAG = "Cache";

    private static Context mContext;

    public static final String cpCachePath = "/EhViewer/cache/cover";
    public static final String pageCachePath = "/EhViewer/cache/page";
    public static DiskCache cpCache = null;
    public static DiskCache pageCache = null;
    public static LruCache<String, Bitmap> memoryCache = null;
    public static LruCache<String, MangaDetail> mdCache = null;
    
    private static final int MD_CACHE_SIZE = 5;
    
    private static boolean mInit = false;

    /**
     * Init Cache
     * 
     * @param context Application context
     */
    public static void init(Context context) {
        if (mInit)
            return;
        mInit = true;
        
        mContext = context;

        if (hasSdCard()) {
            try {
                int cpCacheSize = Config.getCoverDiskCacheSize() * 1024 * 1024;
                if (cpCacheSize <= 0)
                    cpCache = null;
                else
                    cpCache = new DiskCache(mContext, cpCachePath, cpCacheSize);
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                int pageCacheSize = Config.getPageDiskCacheSize() * 1024 * 1024;
                if (pageCacheSize <= 0)
                    pageCache = null;
                else
                    pageCache = new DiskCache(mContext, pageCachePath,
                            pageCacheSize);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;
        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @SuppressLint("NewApi")
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                if (Build.VERSION.SDK_INT < 19)
                    return bitmap.getByteCount() / 1024;
                else
                    return bitmap.getAllocationByteCount() / 1024;
            }
        };
        
        mdCache = new LruCache<String, MangaDetail>(MD_CACHE_SIZE) {
            @Override
            protected int sizeOf(String key, MangaDetail md) {
                return 1;
            }
        };
    }
    
    /**
     * Is init
     * @return True if init
     */
    public static boolean isInit() {
        return mInit;
    }
    
    /**
     * Have sd card or not
     * @return True if has sd card
     */
    public static boolean hasSdCard() {
        return Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
    }
}
