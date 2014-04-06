package com.hippo.ehviewer.view;

import com.hippo.ehviewer.DiskCache;
import com.hippo.ehviewer.util.EhClient;
import com.hippo.ehviewer.util.Util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Movie;
import android.support.v4.util.LruCache;
import android.util.AttributeSet;
import com.hippo.ehviewer.util.Log;
import android.view.View;

public class OlImageView extends SuperImageView {
    private static String TAG = "OlImageView";
    private String url = null;
    private String key = null;

    public static final int STATUS_NON_LOADED = 0x0;
    public static final int STATUS_LOADING = 0x1;
    public static final int STATUS_LOADED = 0x2;
    public static final int STATUS_LOAD_FAIL = 0x3;

    private int status = STATUS_NON_LOADED;

    public static final int LOAD_FAILED = 0x0;
    public static final int LOAD_SUCCESSDE = 0x1;

    private LruCache<String, Bitmap> memoryCache = null;
    private DiskCache diskCache = null;

    private static Bitmap waitBitmap;
    private static Bitmap refreshBitmap;

    private OnLoadListener onLoadListener = null;

    public interface OnLoadListener {
        void onLoadCompleted(boolean ok);
    }

    public void setOnLoadListener(OnLoadListener listener) {
        onLoadListener = listener;
    }

    public OlImageView(Context context) {
        super(context);
    }

    public OlImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OlImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setWaitMovie() {
        setImageBitmap(waitBitmap);
    }

    public void setUrl(String s) {
        url = s;
        key = url;
        status = STATUS_NON_LOADED;
    }

    public String getUrl() {
        return url;
    }

    public void setKey(String s) {
        key = s;
    }

    public String getKey() {
        return key;
    }

    public void setCache(LruCache<String, Bitmap> memoryCache,
            DiskCache diskCache) {
        this.memoryCache = memoryCache;
        this.diskCache = diskCache;
    }
    
    /**
     * If load from cache fail, will not set status flag as STATUS_LOAD_FAIL
     * @return
     */
    public boolean loadFromCache() {
        synchronized (this) {
            if (status == STATUS_LOADED)
                return true;
            if (status == STATUS_LOADING || url == null || key == null)
                return false;

            int type = Util.getResourcesType(url);
            Object res = null;
            // Check in memory
            if (type == Util.BITMAP && memoryCache != null
                    && (res = memoryCache.get(key)) != null) {
                status = STATUS_LOADED;
                setImageBitmap((Bitmap) res);
                return true;
            }
            // If not find in memory cache or do not have memory cache
            // Check disk cache
            if (diskCache != null && (res = diskCache.get(key, type)) != null) {
                status = STATUS_LOADED;
                if (res instanceof Bitmap) {
                    setImageBitmap((Bitmap) res);
                    if (memoryCache != null)
                        memoryCache.put(key, (Bitmap) res);
                } else
                    OlImageView.this.setImageMovie((Movie) res);
                return true;
            }
            setImageBitmap(waitBitmap);
            return false;
        }
    }
    
    /**
     * Load image for url
     * @param froce If True, will get image even status is fail
     * @return
     */
    public boolean loadImage(boolean froce) {
        if (status == STATUS_LOADED)
            return true;
        if (status == STATUS_LOADING || url == null || key == null)
            return false;
        if (!froce && status == STATUS_LOAD_FAIL)
            return false;
        
        load();

        return true;
    }
    
    public boolean reloadImage() {
        if ((status == STATUS_LOADED
                || status == STATUS_LOAD_FAIL
                || status == STATUS_NON_LOADED)
                && url != null && key != null) {
            load();
            return true;
        }
        else
            return false;
    }
    
    private void load() {
        status = STATUS_LOADING;
        
        setImageBitmap(waitBitmap);

        int type = Util.getResourcesType(url);
        synchronized (this) {
            // If can't find download
            EhClient.getImage(url, key, type, memoryCache, diskCache, null,
                    new EhClient.OnGetImageListener() {
                        @Override
                        public void onSuccess(Object checkFlag, Object res) {
                            if (res instanceof Bitmap) {
                                OlImageView.this.setImageBitmap((Bitmap) res);
                            } else
                                OlImageView.this.setImageMovie((Movie) res);
                            OlImageView.this.status = STATUS_LOADED;
                            if (onLoadListener != null)
                                onLoadListener.onLoadCompleted(true);
                        }

                        @Override
                        public void onFailure(int errorMessageId) {
                            OlImageView.this.setImageBitmap(refreshBitmap);
                            OlImageView.this.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View arg0) {
                                    setImageBitmap(waitBitmap);
                                    loadImage(true);
                                    setClickable(false);
                                }
                            });
                            OlImageView.this.status = STATUS_LOAD_FAIL;
                            if (onLoadListener != null)
                                onLoadListener.onLoadCompleted(false);
                        }
                    });
        }
    }
    
    public static void setDefaultImage(Bitmap waitBitmap, Bitmap refreshBitmap) {
        OlImageView.waitBitmap = waitBitmap;
        OlImageView.refreshBitmap = refreshBitmap;
    }

    public int getStatus() {
        return status;
    }
}
