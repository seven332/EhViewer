package com.hippo.ehviewer.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Environment;
import android.support.v4.util.LruCache;

import com.hippo.ehviewer.data.GalleryDetail;
import com.hippo.ehviewer.util.Log;
import com.hippo.ehviewer.DiskCache;

public class Cache {
    private static final String TAG = "Cache";

    private static Context mContext;

    public static final String cpCachePath = "/EhViewer/cache/cover";
    public static DiskCache cpCache = null;
    public static LruCache<String, Bitmap> memoryCache = null;
    public static LruCache<String, GalleryDetail> mdCache = null;
    
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
        }
        
        //final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = 8 * 1024 * 1024; // 8MB, can store 80 thumb
        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @SuppressLint("NewApi")
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                if (Build.VERSION.SDK_INT < 19)
                    return bitmap.getByteCount();
                else
                    return bitmap.getAllocationByteCount();
            }
        };
        
        mdCache = new LruCache<String, GalleryDetail>(MD_CACHE_SIZE) {
            @Override
            protected int sizeOf(String key, GalleryDetail md) {
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
