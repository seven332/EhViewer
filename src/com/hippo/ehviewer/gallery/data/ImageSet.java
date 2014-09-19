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

package com.hippo.ehviewer.gallery.data;

import java.io.File;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.graphics.BitmapFactory;
import android.graphics.Movie;
import android.util.SparseArray;

import com.hippo.ehviewer.AppHandler;
import com.hippo.ehviewer.ehclient.ExDownloader;
import com.hippo.ehviewer.ehclient.ExDownloaderManager;
import com.hippo.ehviewer.gallery.image.Image;
import com.hippo.ehviewer.util.AutoExpandArray;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.EhUtils;
import com.hippo.ehviewer.util.Log;
import com.hippo.ehviewer.util.Utils;

public class ImageSet implements ExDownloader.ListenerForImageSet {

    private static final String TAG = ImageSet.class.getSimpleName();

    /**
     * These are code reture by getImage()
     */
    public static final int RESULT_NONE = 0x0;
    public static final int RESULT_DOWNLOADING = 0x1;
    public static final int RESULT_DECODE = 0x2;

    private final ExDownloaderManager mEdManager;
    private final ExDownloader mExDownloader;

    private final File mDir;
    private final AutoExpandArray<String> mImageFilenameArray;
    private ImageListener mListener;
    private final Queue<DecodeInfo> mDecodeQueue = new ConcurrentLinkedQueue<DecodeInfo>();
    private final SparseArray<Float> mPercentMap = new SparseArray<Float>(5);
    private final DecodeWorker mDecodeWorker;
    /** If true, then wake Worker, worker will stop **/
    private volatile boolean mStopWork = false;

    public class DecodeInfo {
        public int index;
        public String filename;

        public DecodeInfo(int index, String filename) {
            this.index = index;
            this.filename = filename;
        }
    }

    public interface ImageListener {
        /**
         * It will be invoke when ExDownloader download successful
         *
         * @param index
         */
        void onGetImage(int index);

        void onDownloading(int index, float percent);

        /**
         * It will be invoke when decode image over
         *
         * @param index
         * @param res
         */
        void onDecodeOver(int index, Object res);
    }

    public ImageSet(int gid, String token, String title, int startIndex) {

        mDir = EhUtils.getGalleryDir(gid, title);
        Utils.ensureDir(mDir, true);

        // Init ExDownloader
        mEdManager = ExDownloaderManager.getInstance();
        mExDownloader = mEdManager.getExDownloader(gid, token, title, Config.getMode());
        mExDownloader.setStartIndex(startIndex);
        mExDownloader.setListenerForImageSet(this);

        mImageFilenameArray = mExDownloader.getImageFilenameArray();

        // Start decode worker
        mDecodeWorker = new DecodeWorker();
        mDecodeWorker.start();
    }

    public boolean isStop() {
        return mStopWork;
    }

    public int getSize() {
        return mExDownloader.getImageNum();
    }

    public int getEnsureSize() {
        return mExDownloader.getMaxEnsureIndex() + 1;
    }

    public void setImageListener(ImageListener l) {
        mListener = l;
    }

    public void setTargetIndex(int index) {
        mExDownloader.setTargetIndex(index);
    }

    public void setStartIndex(int startIndex) {
        mExDownloader.setStartIndex(startIndex);
    }

    public Object getImage(int index) {
        // Try to get file name
        String filename = mImageFilenameArray.get(index);
        if (filename == null) {
            // Just guess filename
            for (String possibleFilename : EhUtils.getPossibleImageFilenames(index)) {
                File file = new File(mDir, possibleFilename);
                if (file.exists()) {
                    // Get filename
                    filename = possibleFilename;
                    mImageFilenameArray.set(index, filename);
                    break;
                }
            }
        }

        Float percent;
        if ((percent = mPercentMap.get(index)) != null) {
            return percent;
        } else if (mExDownloader.isDownloading(index)) {
            // downloading
            return RESULT_DOWNLOADING;
        } else if (filename == null || !new File(mDir, filename).exists()) {
            // Target index has not being downloading
            return RESULT_NONE;
        } else {
            mDecodeQueue.offer(new DecodeInfo(index, filename));
            // wake decode worker
            synchronized(mDecodeQueue) {mDecodeQueue.notify();}
            return RESULT_DECODE;
        }
    }

    /**
     * You must call it when you do not need it any more
     */
    public void free() {
        mStopWork = true;
        // wake decode worker to wash wash sleep
        synchronized(mDecodeQueue) {mDecodeQueue.notify();}

        // Remove listener for ImageSet
        mExDownloader.setListenerForImageSet(null);
        // Free ExDownloader
        mEdManager.freeExDownloader(mExDownloader);
    }

    private class DecodeWorker extends Thread {
        @Override
        public void run() {
            DecodeInfo decodeInfo;
            while (true) {
                // Check stop work or not
                if (mStopWork)
                    break;

                // Get decode task
                synchronized (mDecodeQueue) {
                    if (mDecodeQueue.isEmpty()) {
                        try {
                            mDecodeQueue.wait();
                        } catch (InterruptedException e) {}
                        continue;
                    }
                    decodeInfo = mDecodeQueue.poll();
                }

                // do decode
                final int index = decodeInfo.index;
                String pathName = Utils.getPathName(mDir.getPath(), decodeInfo.filename);
                Object res = null;

                if (Utils.SUPPORT_IMAGE && Config.getCustomCodec()) {
                    res = Image.decodeFile(pathName, Config.getDecodeFormat());
                } else {
                    if (Utils.getExtension(pathName, "jpg").equals("gif"))
                        res = Movie.decodeFile(pathName);
                    else
                        res = BitmapFactory.decodeFile(pathName);
                }

                final Object _res = res;
                // Post to UI thread
                AppHandler.getInstance().post(new Runnable() {
                    @Override
                    public void run() {
                        if (mListener != null)
                            mListener.onDecodeOver(index, _res);
                    }
                });
            }
            Log.d(TAG, "DecodeWorker stop working");
        }
    }

    @Override
    public void onDownloadStart(final int index) {
        AppHandler.getInstance().post(new Runnable() {
            @Override
            public void run() {
                if (mListener != null)
                    mListener.onDownloading(index, 0.0f);
            }
        });
    }

    @Override
    public void onDownloading(final int index, final float percent) {
        mPercentMap.append(index, percent);

        AppHandler.getInstance().post(new Runnable() {
            @Override
            public void run() {
                if (mListener != null)
                    mListener.onDownloading(index, percent);
            }
        });
    }

    @Override
    public void onDownloadComplete(final int index) {
        mPercentMap.remove(index);

        AppHandler.getInstance().post(new Runnable() {
            @Override
            public void run() {
                if (mListener != null)
                    mListener.onGetImage(index);
            }
        });
    }

    @Override
    public void onDownloadFail(int index) {
        mPercentMap.remove(index);
    }
}
