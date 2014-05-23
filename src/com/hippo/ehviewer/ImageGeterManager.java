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
    
    public static final int MEMORY_CACHE = 0x1;
    public static final int DISK_CACHE = 0x2;
    public static final int DOWNLOAD = 0x4;
    
    private class LoadTask {
        public String url;
        public String key;
        public int mode;
        public OnGetImageListener listener;
        public Bitmap bitmap;
        
        public LoadTask(String url, String key, int mode, OnGetImageListener listener) {
            this.url = url;
            this.key = key;
            this.mode = mode;
            this.listener = listener;
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
                    if (msg.what == Constants.TRUE) {
                        task.listener.onGetImageSuccess(task.key, task.bitmap, msg.arg1);
                    } else if (msg.what == Constants.FALSE){
                        task.listener.onGetImageFail(task.key);
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
    
    public void add(String url, String key, int mode, OnGetImageListener listener) {
        synchronized (mLock) {
            mLoadCacheTask.push(new LoadTask(url, key, mode, listener));
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
                if (!loadTask.listener.onGetImageStart(loadTask.key))
                    continue;
                
                String key = loadTask.key;
                int mode = loadTask.mode;
                int getMode = MEMORY_CACHE;
                if ((mode & MEMORY_CACHE) != 0 && mMemoryCache != null
                        && (loadTask.bitmap = mMemoryCache.get(key)) != null)
                    getMode = MEMORY_CACHE;
                else if ((mode & DISK_CACHE) != 0 && mDiskCache != null
                        && (loadTask.bitmap = (Bitmap)mDiskCache.get(key, Util.BITMAP)) != null) {
                    getMode = DISK_CACHE;
                    if (mMemoryCache != null)
                        mMemoryCache.put(key, loadTask.bitmap);
                }
                
                if (loadTask.bitmap != null) { // Get bitmap
                    Message msg = new Message();
                    msg.obj = loadTask;
                    msg.what = Constants.TRUE;
                    msg.arg1 = getMode;
                    mHandler.sendMessage(msg);
                } else { // get from cache miss
                    if ((mode & DOWNLOAD) != 0) {
                        mImageDownloadTask.add(loadTask);
                    } else {
                        Message msg = new Message();
                        msg.obj = loadTask;
                        msg.what = Constants.FALSE;
                        mHandler.sendMessage(msg);
                    }
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
                    
                    loadTask.bitmap = httpHelper.getImage(loadTask.url,
                            loadTask.key, mMemoryCache, mDiskCache, true);
                    
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
        boolean onGetImageStart(String key);
        void onGetImageSuccess(String key, Bitmap bmp, int mode);
        void onGetImageFail(String key);
    }
}
