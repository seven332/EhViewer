package com.hippo.ehviewer;

import java.util.Stack;

import com.hippo.ehviewer.network.HttpHelper;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.util.Util;
import com.hippo.ehviewer.widget.LoadImageView;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.support.v4.util.LruCache;

public class ImageLoadManager {
    
    private class LoadTask {
        public LoadImageView liv;
        public boolean download;
        public Bitmap bitmap;
        
        public LoadTask(LoadImageView liv, boolean download) {
            this.liv = liv;
            this.download = download;
        }
    }
    
    private final Stack<LoadTask> mLoadCacheTask;
    private ImageDownloadManager mImageDownloadTask;
    
    private LruCache<String, Bitmap> mMemoryCache;
    private DiskCache mDiskCache;
    
    private LoadTask curLoadTask;
    private final LoadTask emptyLoadTask = new LoadTask(null, false);
    
    private static final Handler loadImageHandler = 
            new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    LoadTask task = (LoadTask)msg.obj;
                    task.liv.setImageBitmap(task.bitmap);
                }
            };
    
    public ImageLoadManager(LruCache<String, Bitmap> memoryCache,
            DiskCache diskCache) {
        mLoadCacheTask = new Stack<LoadTask>();
        mImageDownloadTask = new ImageDownloadManager();
        
        mMemoryCache = memoryCache;
        mDiskCache = diskCache;
    }
    
    public synchronized void add(LoadImageView liv, boolean download) {
        liv.setState(LoadImageView.LOADING);
        mLoadCacheTask.push(new LoadTask(liv, download));
        if (curLoadTask == null) {
            new Thread(new LoadFromCacheTask()).start();
            curLoadTask = emptyLoadTask;
        }
    }
    
    private class LoadFromCacheTask implements Runnable {
        @Override
        public void run() {
            while (true) {
                synchronized (ImageLoadManager.this) {
                    if (mLoadCacheTask.isEmpty()) {
                        curLoadTask = null;
                        break;
                    }
                    curLoadTask = mLoadCacheTask.pop();
                }
                LoadImageView liv = curLoadTask.liv;
                String key = liv.getKey();
                Bitmap bitmap;
                if ((bitmap = mMemoryCache.get(key)) == null) {
                    bitmap = (Bitmap)mDiskCache.get(key, Util.BITMAP);
                    if (bitmap != null)
                        mMemoryCache.put(key, bitmap);
                }
                
                Message msg = new Message();
                msg.obj = curLoadTask;
                if (bitmap == null) { // Load from cache error
                    curLoadTask.bitmap = Ui.BITMAP_LAUNCH;
                    if (curLoadTask.download)
                        mImageDownloadTask.add(curLoadTask);
                    else
                        liv.setState(LoadImageView.FAIL);
                } else {
                    curLoadTask.bitmap = bitmap;
                    liv.setState(LoadImageView.LOADED);
                }
                loadImageHandler.sendMessage(msg);
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
                HttpHelper httpHelper = new HttpHelper();
                while (true) {
                    synchronized (ImageDownloadManager.this) {
                        if (mDownloadTask.isEmpty()) {
                            loadTask = null;
                            workingDownloadThreads--;
                            break;
                        }
                        loadTask = mDownloadTask.pop();
                    }
                    LoadImageView liv = loadTask.liv;
                    Bitmap bitmap = httpHelper.getImage(liv.getUrl(),
                            liv.getKey(), mMemoryCache, mDiskCache, true);
                    
                    Message msg = new Message();
                    msg.obj = loadTask;
                    if (bitmap == null) {
                        loadTask.bitmap = Ui.BITMAP_TOUCH;
                        liv.setState(LoadImageView.FAIL);
                        liv.setOnClickListener(ImageLoadManager.this);
                    } else {
                        loadTask.bitmap = bitmap;
                        liv.setState(LoadImageView.LOADED);
                    }
                    loadImageHandler.sendMessage(msg);
                }
            }
        }
    }
}
