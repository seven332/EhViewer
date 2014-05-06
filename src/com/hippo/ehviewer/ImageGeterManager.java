package com.hippo.ehviewer;

import java.util.Stack;

import com.hippo.ehviewer.network.HttpHelper;
import com.hippo.ehviewer.util.Constants;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.util.Util;
import com.hippo.ehviewer.widget.LoadImageView;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.support.v4.util.LruCache;

import com.hippo.ehviewer.util.Log;

public class ImageGeterManager {
    private static final String TAG = "ImageGeterManager";
    
    private static final int CACHE = 0x0;
    private static final int DOWNLOAD = 0x1;
    
    private class LoadTask {
        public String url;
        public String key;
        public OnGetImageListener listener;
        public boolean download;
        public Bitmap bitmap;
        
        public LoadTask(String url, String key, OnGetImageListener listener, boolean download) {
            this.url = url;
            this.key = key;
            this.listener = listener;
            this.download = download;
        }
    }
    private Context mContext;
    
    private final Stack<LoadTask> mLoadCacheTask;
    private ImageDownloadManager mImageDownloadTask;
    
    private LruCache<String, Bitmap> mMemoryCache;
    private DiskCache mDiskCache;
    
    private Object mLock;
    
    private static final Handler mHandler = 
            new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    LoadTask task = (LoadTask)msg.obj;
                    switch (msg.what) {
                    case Constants.TRUE:
                        if (msg.arg1 == CACHE)
                            task.listener.onGetImageFromCacheSuccess(task.key, task.bitmap);
                        else if (msg.arg1 == DOWNLOAD)
                            task.listener.onGetImageFromDownloadSuccess(task.key, task.bitmap);
                        break;
                    case Constants.FALSE:
                        if (msg.arg1 == CACHE)
                            task.listener.onGetImageFromCacheFail(task.key);
                        else
                            task.listener.onGetImageFail(task.key);
                        break;
                    }
                }
            };
    
    public ImageGeterManager(Context context, LruCache<String, Bitmap> memoryCache,
            DiskCache diskCache) {
        mLoadCacheTask = new Stack<LoadTask>();
        mImageDownloadTask = new ImageDownloadManager();
        
        mContext = context;
        mMemoryCache = memoryCache;
        mDiskCache = diskCache;
        
        mLock = new Object();
        new Thread(new LoadFromCacheTask()).start();
    }
    
    public void add(String url, String key, OnGetImageListener listener, boolean download) {
        synchronized (mLock) {
            mLoadCacheTask.push(new LoadTask(url, key, listener, download));
            mLock.notify();
        }
    }
    
    private class LoadFromCacheTask implements Runnable {
        @Override
        public void run() {
            LoadTask loadTask;
            while (true) {
                synchronized (mLock) {
                    if (mLoadCacheTask.isEmpty()) {
                        try {
                            mLock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        continue;
                    }
                    loadTask = mLoadCacheTask.pop();
                }
                
                // Continue if do not need any more
                if (!loadTask.listener.onGetImage(loadTask.key))
                    continue;
                
                String key = loadTask.key;
                if (mMemoryCache == null || (loadTask.bitmap = mMemoryCache.get(key)) == null) {
                    if (mDiskCache != null
                            && (loadTask.bitmap = (Bitmap)mDiskCache.get(key, Util.BITMAP)) != null
                            && mMemoryCache != null)
                        mMemoryCache.put(key, loadTask.bitmap);
                }
                
                Message msg = new Message();
                msg.obj = loadTask;
                if (loadTask.bitmap == null) {
                    if (loadTask.download) {
                        mImageDownloadTask.add(loadTask);
                        msg.what = Constants.FALSE;
                        mHandler.sendMessage(msg);
                    } else {
                        msg.what = Constants.FALSE;
                        msg.arg1 = CACHE;
                        mHandler.sendMessage(msg);
                    }
                } else {
                    msg.what = Constants.TRUE;
                    msg.arg1 = CACHE;
                    mHandler.sendMessage(msg);
                }
            }
        }
    }
    
    private class ImageDownloadManager {
        
        private static final int MAX_DOWNLOAD_THREADS = 3;
        
        private final Stack<LoadTask> mDownloadTask;
        
        private int workingDownloadThreads = 0;
        
        public ImageDownloadManager() {
            mDownloadTask = new Stack<LoadTask>();
        }
        
        public synchronized void add(LoadTask loadTask) {
            mDownloadTask.push(loadTask);
            if (workingDownloadThreads < MAX_DOWNLOAD_THREADS) {
                new Thread(new DownloadImageTask()).start();
                workingDownloadThreads++;
            }
        }
        
        private class DownloadImageTask implements Runnable {
            @Override
            public void run() {
                LoadTask loadTask;
                HttpHelper httpHelper = new HttpHelper(mContext);
                while (true) {
                    synchronized (ImageDownloadManager.this) {
                        if (mDownloadTask.isEmpty()) {
                            loadTask = null;
                            workingDownloadThreads--;
                            break;
                        }
                        loadTask = mDownloadTask.pop();
                    }
                    
                    // Continue if do not need any more
                    if (!loadTask.listener.onGetImage(loadTask.key))
                        continue;
                    
                    loadTask.bitmap = httpHelper.getImage(loadTask.url,
                            loadTask.key, mMemoryCache, mDiskCache, true);
                    
                    // Continue if do not need any more
                    if (!loadTask.listener.onGetImage(loadTask.key))
                        continue;
                    
                    Message msg = new Message();
                    msg.obj = loadTask;
                    if (loadTask.bitmap == null) {
                        msg.what = Constants.FALSE;
                    } else {
                        msg.what = Constants.TRUE;
                        msg.arg1 = DOWNLOAD;
                    }
                    mHandler.sendMessage(msg);
                }
            }
        }
    }
    
    public interface OnGetImageListener {
        /**
         * @return False if you wanna stop get
         */
        boolean onGetImage(String key);
        
        /**
         * It will call when active download and get from cache miss
         * @param key
         */
        void onGetImageFromCacheFail(String key);
        void onGetImageFromCacheSuccess(String key, Bitmap bmp);
        void onGetImageFromDownloadSuccess(String key, Bitmap bmp);
        void onGetImageFail(String key);
    }
}
