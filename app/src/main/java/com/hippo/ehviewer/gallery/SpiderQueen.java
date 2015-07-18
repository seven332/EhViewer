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

package com.hippo.ehviewer.gallery;

import android.graphics.Bitmap;
import android.os.Process;

import com.hippo.ehviewer.client.EhConfig;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.client.GalleryPageParser;
import com.hippo.ehviewer.client.data.GalleryBase;
import com.hippo.ehviewer.network.EhHttpClient;
import com.hippo.ehviewer.network.EhHttpRequest;
import com.hippo.ehviewer.util.Settings;
import com.hippo.httpclient.HttpRequest;
import com.hippo.httpclient.HttpResponse;
import com.hippo.yorozuya.AutoExpandArray;
import com.hippo.yorozuya.FileUtils;
import com.hippo.yorozuya.PriorityThreadFactory;
import com.hippo.yorozuya.Say;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.ReentrantLock;

public class SpiderQueen implements Runnable {

    private static final String TAG = SpiderQueen.class.getSimpleName();

    private static final String[] URL_509_SUFFIX_ARRAY = {
            "/509.gif",
            "/509s.gif"
    };

    public static final int PAGE_STATE_NONE = 0;
    public static final int PAGE_STATE_DOWNLOADING = 1;
    public static final int PAGE_STATE_SUCCEED = 2;
    public static final int PAGE_STATE_FAILED = 3;

    public EhHttpClient mHttpClient;
    public File mSpiderInfoDir;
    public GalleryBase mGalleryBase;
    public ImageHandler mImageHandler;
    public int mSource;
    private ImageHandler.Mode mMode;
    private SpiderListener mSpiderListener;

    public SpiderInfo mSpiderInfo;
    public int mNextIndex;
    public int[] mPageStates;

    public boolean mHasCheckIndexAgain = true;

    private ThreadFactory mThreadFactory = new PriorityThreadFactory("SpiderWorker",
            Process.THREAD_PRIORITY_BACKGROUND);
    private SpiderWorker[] mSpiderWorkers;
    private LinkedList<Integer> mRequestTokens = new LinkedList<>();
    private Render mRender;

    private final Object mQueenLock = new Object();
    private final Object mWorkerLock = new Object();
    private final Object mPageLock = new Object();
    private final ReentrantLock mWorkerArrayLock = new ReentrantLock();
    // Lock for mRequestTokens and mSpiderInfo.tokens
    private final Object mTokenLock = new Object();

    private boolean mStop = false;

    public SpiderQueen(EhHttpClient httpClient, File spiderInfoDir,
            GalleryBase galleryBase, ImageHandler.Mode mode, SpiderListener listener) {
        mHttpClient = httpClient;
        mSpiderInfoDir = spiderInfoDir;
        mGalleryBase = galleryBase;
        mSource = Settings.getEhSource();
        mMode = mode;
        mSpiderListener = listener;
    }

    public void setMode(ImageHandler.Mode mode) {
        mMode = mode;
        if (mImageHandler != null) {
            mImageHandler.setMode(mode);
        }
    }

    public void request(int index) {
        if (mRender == null) {
            mSpiderListener.onGetNone(index);
        } else {
            mRender.request(index);
        }
    }

    public void releaseBitmap(Bitmap bitmap) {
        if (mImageHandler != null) {
            mImageHandler.releaseBitmap(bitmap);
        }
    }

    @Override
    public void run() {
        Say.d(TAG, "Spider Queen starts");

        EhConfig ehConfig = mHttpClient.getEhConfigClone();
        GalleryBase gb = mGalleryBase;
        SpiderInfo spiderInfo = null;

        // 1. get spider info from dir
        File spiderInfoFile = new File(mSpiderInfoDir, Integer.toString(gb.gid));
        if (spiderInfoFile.exists()) {
            try {
                spiderInfo = SpiderInfo.read(new FileInputStream(spiderInfoFile));
                if (gb.gid != spiderInfo.galleryBase.gid) {
                    spiderInfo = null;
                    throw new IllegalStateException("Gid not match in SpiderInfo file");
                }
            } catch (Exception e) {
                FileUtils.delete(spiderInfoFile);
            }
        }
        // Check stop
        if (mStop) {
            return;
        }

        // 2. if can't get spider info from dir, get it from internet
        if (spiderInfo == null) {
            // Use normal size to get more preview
            ehConfig.previewSize = EhConfig.PREVIEW_SIZE_NORMAL;
            ehConfig.setDirty();

            EhHttpRequest request = new EhHttpRequest();
            request.setEhConfig(ehConfig);
            request.setUrl(EhUrl.getDetailUrl(mSource, gb.gid, gb.token, 0));
            try {
                HttpResponse response = mHttpClient.execute(request);
                String body = response.getString();
                SpiderParser.Result result = SpiderParser.parse(body, SpiderParser.REQUEST_ALL);
                spiderInfo = new SpiderInfo();
                spiderInfo.galleryBase = gb;
                spiderInfo.pages = result.pages;
                spiderInfo.previewSize = result.previewSize;
                spiderInfo.previewPages = result.previewPages;
                spiderInfo.previewCountPerPage = result.tokens.size();
                spiderInfo.tokens = new AutoExpandArray<>(spiderInfo.pages);
            } catch (Exception e) {
                // TODO Add a listener to tell spider error
                e.printStackTrace();
                return;
            } finally {
                request.disconnect();
            }
        }
        // Update EhConfig preview size
        ehConfig.previewSize = spiderInfo.previewSize;
        // Write to file
        try {
            spiderInfo.write(new FileOutputStream(spiderInfoFile));
        } catch (IOException e) {
            // Ignore
        }
        // Check stop
        if (mStop) {
            return;
        }
        // Listener
        mSpiderListener.onGetPages(spiderInfo.pages);

        // 3. prepare
        mSpiderInfo = spiderInfo;
        mNextIndex = 0;
        mPageStates = new int[spiderInfo.pages];
        Arrays.fill(mPageStates, PAGE_STATE_NONE);
        mSpiderWorkers = new SpiderWorker[3];
        mImageHandler = new ImageHandler(gb, spiderInfo.pages, mMode);
        // Check stop
        if (mStop) {
            return;
        }

        // 4. Make Worker to do work
        ensureWorker();
        mRender = new Render();
        Thread thread = new Thread(mRender, "Render");
        thread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // 4. Get tokens
        while (!mStop) {
            Integer index;
            synchronized (mTokenLock) {
                index = mRequestTokens.poll();
            }

            if (index == null) {
                // Can't get request index
                synchronized (mQueenLock) {
                    try {
                        mQueenLock.wait();
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                }
                continue;
            }

            // Check if we has the token first
            String token;
            synchronized (mTokenLock) {
                token = spiderInfo.tokens.get(index);
            }
            if (token == null) {
                SpiderParser.Result result;
                int previewIndex = index / spiderInfo.previewCountPerPage;
                EhHttpRequest request = new EhHttpRequest();
                request.setEhConfig(ehConfig);
                request.setUrl(EhUrl.getDetailUrl(mSource, gb.gid, gb.token, previewIndex));
                try {
                    HttpResponse response = mHttpClient.execute(request);
                    String body = response.getString();
                    result = SpiderParser.parse(body, SpiderParser.REQUEST_PREVIEW);
                } catch (Exception e) {
                    // TODO Add a listener to tell spider error
                    e.printStackTrace();
                    break;
                } finally {
                    request.disconnect();
                }
                synchronized (mTokenLock) {
                    AutoExpandArray<String> tokens = spiderInfo.tokens;
                    List<String> list = result.tokens;
                    int start = result.startIndex;
                    int end = start + list.size();
                    for (int i = start; i < end; i++) {
                        tokens.set(i, list.get(i - start));
                    }
                }
                // Write to file
                try {
                    spiderInfo.write(new FileOutputStream(spiderInfoFile));
                } catch (IOException e) {
                    // Ignore
                }
            }
            // Notify
            synchronized (mWorkerLock) {
                mWorkerLock.notifyAll();
            }
        }

        // 5. The queen dies, workers die, render dies
        mWorkerArrayLock.lock();

        for (SpiderWorker worker : mSpiderWorkers) {
            if (worker != null) {
                worker.mStop = true;
            }
        }
        synchronized (mWorkerLock) {
            mWorkerLock.notifyAll();
        }

        mWorkerArrayLock.unlock();

        mRender.stop();


        Say.d(TAG, "Spider dies");
    }

    public int pages() {
        if (mSpiderInfo != null) {
            return mSpiderInfo.pages;
        } else {
            return -1;
        }
    }

    public void stop() {
        mStop = true;
        synchronized (mQueenLock) {
            mQueenLock.notify();
        }
    }

    private void ensureWorker() {
        mWorkerArrayLock.lock();

        int length = mSpiderWorkers.length;
        for (int i = 0; i < length; i++) {
            if (mSpiderWorkers[i] == null) {
                SpiderWorker worker = new SpiderWorker(i);
                mSpiderWorkers[i] = worker;
                mThreadFactory.newThread(worker).start();
            }
        }

        mWorkerArrayLock.unlock();
    }

    public void setNextIndex(int index) {
        synchronized (mPageLock) {
            if (mSpiderInfo != null && index < mSpiderInfo.pages) {
                mNextIndex = index;
            }
        }
    }

    public String getTokenByIndex(int index) {
        synchronized (mTokenLock) {
            String token = mSpiderInfo.tokens.get(index);
            if (token == null && !mRequestTokens.contains(index)) {
                mRequestTokens.offer(index);
            }
            // Notify Queen
            synchronized (mQueenLock) {
                mQueenLock.notify();
            }
            return token;
        }
    }

    private int getRequestIndex() {
        synchronized (mPageLock) {
            int length = mSpiderInfo.pages;
            if (mNextIndex >= length) {
                if (!mHasCheckIndexAgain) {
                    mHasCheckIndexAgain = true;
                    mNextIndex = 0;
                }
            }

            while (mNextIndex < length && mPageStates[mNextIndex] != PAGE_STATE_NONE) {
                mNextIndex++;
            }

            return mNextIndex >= length ? -1 : mNextIndex++;
        }
    }

    class SpiderWorker implements Runnable {

        private final String TAG = SpiderWorker.class.getSimpleName();

        public int mIndex;

        public boolean mStop = false;

        public SpiderWorker(int index) {
            mIndex = index;
        }

        private String getImageUrl(int gid, String token, int index) throws Exception {
            HttpRequest request = new HttpRequest();
            request.setUrl(EhUrl.getPageUrl(mSource, mGalleryBase.gid, token, index));
            try {
                HttpResponse response = mHttpClient.execute(request);
                String body = response.getString();
                String imageUrl = GalleryPageParser.parse(body);
                for (String suffix : URL_509_SUFFIX_ARRAY) {
                    if (imageUrl.endsWith(suffix)) {
                        // Get 509 gif here
                        throw new Image509Expection();
                    }
                }
                return imageUrl;
            } finally {
                request.disconnect();
            }
        }

        @Override
        public void run() {
            Say.d(TAG, "Spider worker starts");

            GalleryBase gb = mGalleryBase;
            ImageHandler handler = mImageHandler;

            while (!mStop) {
                // Get index
                int index;
                synchronized (mPageLock) {
                    index = getRequestIndex();
                    if (index == -1) {
                        // There is no more
                        break;
                    } else {
                        mPageStates[index] = PAGE_STATE_DOWNLOADING;
                    }
                }

                // Check contain
                if (handler.contain(index)) {
                    // Find it, no need to download
                    synchronized (mPageLock) {
                        mPageStates[index] = PAGE_STATE_SUCCEED;
                    }
                    // Listener
                    mSpiderListener.onSpiderSucceed(index);
                    continue;
                }

                // Get token
                String token;
                for (;;) {
                    token = getTokenByIndex(index);
                    if (token == null) {
                        synchronized (mWorkerLock) {
                            try {
                                mWorkerLock.wait();
                            } catch (InterruptedException e) {
                                // Ignore
                            }
                            if (mStop) {
                                break;
                            }
                        }
                    } else {
                        break;
                    }
                }
                // For stop
                if (mStop) {
                    break;
                }


                // Get image url
                String imageUrl;
                try {
                    imageUrl = getImageUrl(gb.gid, token, index);
                } catch (Exception e) {
                    e.printStackTrace();
                    // TODO notify 509 error
                    synchronized (mPageLock) {
                        mPageStates[index] = PAGE_STATE_FAILED;
                    }
                    // Listener
                    mSpiderListener.onSpiderFailed(index);
                    continue;
                }

                // Download image
                HttpRequest request = new HttpRequest();
                request.setUrl(imageUrl);
                try {
                    HttpResponse response = mHttpClient.execute(request);
                    handler.save(index, response);

                    synchronized (mPageLock) {
                        mPageStates[index] = PAGE_STATE_SUCCEED;
                    }
                    // Listener
                    mSpiderListener.onSpiderSucceed(index);
                } catch (Exception e) {
                    synchronized (mPageLock) {
                        mPageStates[index] = PAGE_STATE_FAILED;
                    }
                    // Listener
                    mSpiderListener.onSpiderFailed(index);
                } finally {
                    request.disconnect();
                }
            }

            // TODO this lock is not right, we need to lock when before check it is over
            mWorkerArrayLock.lock();
            mSpiderWorkers[mIndex] = null;
            mWorkerArrayLock.unlock();

            Say.d(TAG, "Spider Worker dies");
        }
    }

    // The runner to handle request
    class Render implements Runnable {

        private final String TAG = Render.class.getSimpleName();

        private boolean mStop = false;

        private final Object mRenderLock = new Object();

        private BlockingQueue<Integer> mQueue = new LinkedBlockingQueue<>();

        public void stop() {
            mStop = true;
            synchronized (mRenderLock) {
                mRenderLock.notify();
            }
        }

        public void request(int index) {
            mQueue.offer(index);
            synchronized (mRenderLock) {
                mRenderLock.notify();
            }
        }

        @Override
        public void run() {
            Say.d(TAG, "Render starts");

            while (!mStop) {

                Integer index = mQueue.poll();
                if (index == null) {
                    synchronized (mRenderLock) {
                        try {
                            mRenderLock.wait();
                        } catch (InterruptedException e) {
                            // Ignore
                        }
                    }
                    continue;
                }

                int state;
                synchronized (mPageLock) {
                    state = mPageStates[index];
                }

                switch (state) {
                    case PAGE_STATE_NONE:
                        mSpiderListener.onGetNone(index);
                        break;
                    case PAGE_STATE_DOWNLOADING:
                        mSpiderListener.onGetSpider(index, 0.5f); // TODO
                        break;
                    case PAGE_STATE_SUCCEED:
                        mSpiderListener.onGetBitmap(index, mImageHandler == null ? null : mImageHandler.getBitmap(index));
                        break;
                    default:
                    case PAGE_STATE_FAILED:
                        mSpiderListener.onGetFailed(index);
                }
            }

            Say.d(TAG, "Render dies");
        }
    }

    public interface SpiderListener {

        void onGetPages(int pages);

        void onSpiderPage(int index, float percent);

        void onSpiderSucceed(int index);

        void onSpiderFailed(int index);

        void onGetNone(int index);

        void onGetBitmap(int index, Bitmap bitmap);

        void onGetSpider(int index, float percent);

        void onGetFailed(int index);
    }
}
