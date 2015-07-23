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
import android.os.Looper;
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
import com.hippo.unifile.UniFile;
import com.hippo.yorozuya.AutoExpandArray;
import com.hippo.yorozuya.IOUtils;
import com.hippo.yorozuya.PriorityThreadFactory;
import com.hippo.yorozuya.Say;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.locks.ReentrantLock;

public class SpiderQueen implements Runnable {

    private static final String TAG = SpiderQueen.class.getSimpleName();

    public static final Object RESULT_SPIDER = new Object();

    public static final String SPIDER_FILENAME = ".ehviewer";

    private static final String[] URL_509_SUFFIX_ARRAY = {
            "/509.gif",
            "/509s.gif"
    };

    public static final int PAGE_STATE_NONE = 0;
    public static final int PAGE_STATE_SPIDER = 1;
    public static final int PAGE_STATE_SUCCEED = 2;
    public static final int PAGE_STATE_FAILED = 3;

    public EhHttpClient mHttpClient;
    public File mSpiderInfoDir;
    public GalleryBase mGalleryBase;
    private UniFile mDownloadDir;
    public ImageHandler mImageHandler;
    public int mSource;
    private ImageHandler.Mode mMode;
    private SpiderListener mSpiderListener;

    public SpiderInfo mSpiderInfo;
    public boolean mHasCheckIndexAgain = true;
    public transient int mNextIndex;
    public AtomicIntegerArray mPageStates;

    private transient HttpRequest mCurrentHttpRequest;

    private transient boolean mRestart = false;
    private transient boolean mRetry = false;
    private transient boolean mStop = false;

    private ThreadFactory mThreadFactory = new PriorityThreadFactory("SpiderWorker",
            Process.THREAD_PRIORITY_BACKGROUND);
    private SpiderWorker[] mSpiderWorkers;
    private LinkedList<Integer> mRequestTokens = new LinkedList<>();
    private Render mRender;

    // The lock to make Spider Queen sleep
    private final Object mQueenLock = new Object();
    // The lock to make worker sleep
    private final Object mWorkerLock = new Object();
    // The lock for next index
    private final Object mNextIndexLock = new Object();
    private final ReentrantLock mWorkerArrayLock = new ReentrantLock();
    // Lock for mRequestTokens and mSpiderInfo.tokens
    private final Object mTokenLock = new Object();

    public SpiderQueen(EhHttpClient httpClient, File spiderInfoDir,
            GalleryBase galleryBase, ImageHandler.Mode mode, UniFile downloadDir,
            SpiderListener listener) {
        mHttpClient = httpClient;
        mSpiderInfoDir = spiderInfoDir;
        mGalleryBase = galleryBase;
        mMode = mode;
        mDownloadDir = downloadDir;
        mSpiderListener = listener;

        mSource = Settings.getEhSource();
    }

    public void setMode(ImageHandler.Mode mode) {
        mMode = mode;
        if (mImageHandler != null) {
            mImageHandler.setMode(mode);
        }
    }

    // Is the spider queen ready to handle request
    private boolean isReady() {
        return mPageStates != null && mSpiderInfo != null &&
                mRender != null && mImageHandler != null;
    }

    public Object request(int index) {
        if (isReady()) {
            // check range
            if (index < 0 || index >= mSpiderInfo.pages) {
                return GalleryProvider.RESULT_OUT_OF_RANGE;
            }

            int state = mPageStates.get(index);
            switch (state) {
                case PAGE_STATE_NONE:
                    // TODO set next index
                    return GalleryProvider.RESULT_NONE;
                case PAGE_STATE_SPIDER:
                    return RESULT_SPIDER;
                case PAGE_STATE_SUCCEED:
                    // Add a render request
                    mRender.request(index);
                    return GalleryProvider.RESULT_WAIT;
                case PAGE_STATE_FAILED:
                    return GalleryProvider.RESULT_FAILED;
                default:
                    return GalleryProvider.RESULT_OUT_OF_RANGE;
            }
        } else {
            return GalleryProvider.RESULT_OUT_OF_RANGE;
        }
    }

    public Object forceRequest(int index) {
        if (isReady()) {
            // check range
            if (index < 0 || index >= mSpiderInfo.pages) {
                return GalleryProvider.RESULT_OUT_OF_RANGE;
            }
            // TODO
            throw new IllegalStateException();



        } else {
            return GalleryProvider.RESULT_OUT_OF_RANGE;
        }
    }

    public void releaseBitmap(Bitmap bitmap) {
        if (mImageHandler != null) {
            mImageHandler.releaseBitmap(bitmap);
        }
    }

    public void stop() {
        mStop = true;
        if (mCurrentHttpRequest != null) {
            mCurrentHttpRequest.disconnect();
            mCurrentHttpRequest = null;
        }
        synchronized (mQueenLock) {
            mQueenLock.notify();
        }
    }

    public void restart() {
        mRestart = true;
        synchronized (mQueenLock) {
            mQueenLock.notify();
        }
    }

    public void retry() {
        mRetry = true;
        synchronized (mQueenLock) {
            mQueenLock.notify();
        }
    }

    public int pages() {
        if (mSpiderInfo != null) {
            return mSpiderInfo.pages;
        } else {
            return -1;
        }
    }

    public void setPause(boolean pause) {
        if (mRender != null) {
            mRender.setPause(pause);
        }
    }

    @Override
    public void run() {
        Say.d(TAG, "Spider Queen start");

        while (!mStop) {
            try {
                doIt();
            } catch (Exception e) {
                // Stop action, disconnect current HttpRequest may raise Exception
                // No need to report it
                if (mStop) {
                    break;
                }

                mRestart = false;
                mSpiderListener.onTotallyFailed(e);

                while (!mStop && !mRestart) {
                    synchronized (mQueenLock) {
                        try {
                            mQueenLock.wait();
                        } catch (InterruptedException ex) {
                            // Ignore
                        }
                    }
                }
            }
        }

        Say.d(TAG, "Spider Queen end");
    }

    private SpiderInfo readSpiderInfoFromDataDir() {
        SpiderInfo spiderInfo;
        int gid = mGalleryBase.gid;

        File file = new File(mSpiderInfoDir, Integer.toString(gid));
        if (!file.isFile()) {
            return null;
        }

        InputStream is = null;
        try {
            is = new FileInputStream(file);
            spiderInfo = SpiderInfo.read(is);
            if (gid != spiderInfo.galleryBase.gid) {
                throw new IllegalStateException("Gid not match in SpiderInfo file");
            }
            return spiderInfo;
        } catch (Exception e) {
            return null;
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    private SpiderInfo readSpiderInfoFromDownloadDir() {
        if (mMode == ImageHandler.Mode.DOWNLOAD) {
            SpiderInfo spiderInfo;
            UniFile file = mDownloadDir.findFile(SPIDER_FILENAME);
            if (file == null) {
                return null;
            }
            InputStream is = null;
            try {
                is = file.openInputStream();
                spiderInfo = SpiderInfo.read(is);
                if (mGalleryBase.gid != spiderInfo.galleryBase.gid) {
                    throw new IllegalStateException("Gid not match in SpiderInfo file");
                }
                return spiderInfo;
            } catch (Exception e) {
                // Ignore
                return null;
            } finally {
                IOUtils.closeQuietly(is);
            }
        } else {
            return null;
        }
    }

    private void writeSpiderInfoToDataDir(SpiderInfo spiderInfo) {
        OutputStream os = null;
        try {
            File file = new File(mSpiderInfoDir, Integer.toString(mGalleryBase.gid));
            os = new FileOutputStream(file);
            spiderInfo.write(os);
        } catch (Exception e) {
            // Ingore
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

    private void writeSpiderInfoToDownloadDir(SpiderInfo spiderInfo) {
        if (mMode == ImageHandler.Mode.DOWNLOAD) {
            OutputStream os = null;
            try {
                UniFile file = mDownloadDir.findFile(SPIDER_FILENAME);
                if (file != null) {
                    os = file.openOutputStream();
                    spiderInfo.write(os);
                }
            } catch (Exception e) {
                // Ingore
            } finally {
                IOUtils.closeQuietly(os);
            }
        }
    }

    private SpiderInfo getSpiderInfoFromInternet(EhConfig ehConfig) throws Exception {
        SpiderInfo spiderInfo;

        // Use normal size to get more preview
        ehConfig.previewSize = EhConfig.PREVIEW_SIZE_NORMAL;
        ehConfig.setDirty();

        EhHttpRequest request = new EhHttpRequest();
        request.setEhConfig(ehConfig);
        request.setUrl(EhUrl.getDetailUrl(mSource, mGalleryBase.gid, mGalleryBase.token, 0));

        try {
            mCurrentHttpRequest = request;
            HttpResponse response = mHttpClient.execute(request);
            String body = response.getString();
            SpiderParser.Result result = SpiderParser.parse(body, SpiderParser.REQUEST_ALL);
            spiderInfo = new SpiderInfo();
            spiderInfo.galleryBase = mGalleryBase;
            spiderInfo.pages = result.pages;
            spiderInfo.previewSize = result.previewSize;
            spiderInfo.previewPages = result.previewPages;
            spiderInfo.previewCountPerPage = result.tokens.size();
            spiderInfo.tokens = new AutoExpandArray<>(spiderInfo.pages);
            int size = result.tokens.size();
            for (int i = 0; i < size; i++) {
                spiderInfo.tokens.set(i, result.tokens.get(i));
            }

            return spiderInfo;
        } finally {
            mCurrentHttpRequest = null;
            request.disconnect();
        }
    }

    private SpiderParser.Result getTokensFromInternet(int index, EhConfig ehConfig) throws Exception {
        int previewIndex = index / mSpiderInfo.previewCountPerPage;

        EhHttpRequest request = new EhHttpRequest();
        request.setEhConfig(ehConfig);
        request.setUrl(EhUrl.getDetailUrl(mSource, mGalleryBase.gid, mGalleryBase.token, previewIndex));
        try {
            mCurrentHttpRequest = request;
            HttpResponse response = mHttpClient.execute(request);
            String body = response.getString();
            return SpiderParser.parse(body, SpiderParser.REQUEST_PREVIEW);
        } finally {
            mCurrentHttpRequest = null;
            request.disconnect();
        }
    }

    private void doToken(SpiderInfo spiderInfo, EhConfig ehConfig) throws Exception {
        Integer index;
        synchronized (mTokenLock) {
            index = mRequestTokens.poll();
        }

        if (index == null) {
            // Can't get request index, just wait here
            synchronized (mQueenLock) {
                try {
                    mQueenLock.wait();
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
            // Go into a new loop
            return;
        }


        // Check if we has the token first
        String token;
        synchronized (mTokenLock) {
            token = spiderInfo.tokens.get(index);
        }
        if (token == null) {
            SpiderParser.Result result = null;
            Exception exception = null;

            // Try twice
            int k = 2;
            while (k-- > 0) {
                try {
                    result = getTokensFromInternet(index, ehConfig);
                } catch (Exception e) {
                    exception = e;
                }
            }
            if (result == null) {
                throw exception;
            }

            synchronized (mTokenLock) {
                AutoExpandArray<String> tokens = spiderInfo.tokens;
                List<String> list = result.tokens;
                int start = result.startIndex;
                int end = start + list.size();
                if (index < start || index >= end) {
                    // TODO Maybe need a way to handle it
                    throw new Exception("Can't get no target index when find token");
                }

                for (int i = start; i < end; i++) {
                    tokens.set(i, list.get(i - start));
                }
            }

            // Write spider into file
            writeSpiderInfoToDataDir(spiderInfo);
            writeSpiderInfoToDownloadDir(spiderInfo);
        }

        // Notify
        synchronized (mWorkerLock) {
            mWorkerLock.notifyAll();
        }
    }

    private void handleToken(SpiderInfo spiderInfo, EhConfig ehConfig) {
        while (!mStop) {
            try {
                doToken(spiderInfo, ehConfig);
            } catch (Exception e) {
                // Stop action, disconnect current HttpRequest may raise Exception
                // No need to report it
                if (mStop) {
                    break;
                }

                mRetry = false;
                mSpiderListener.onPartlyFailed(e);

                while (!mStop && !mRetry) {
                    synchronized (mQueenLock) {
                        try {
                            mQueenLock.wait();
                        } catch (InterruptedException ex) {
                            // Ignore
                        }
                    }
                }
            }
        }
    }

    public void doIt() throws Exception {
        Say.d(TAG, "Spider Queen do it");

        EhConfig ehConfig = mHttpClient.getEhConfigClone();
        GalleryBase gb = mGalleryBase;
        SpiderInfo spiderInfo = mSpiderInfo;

        // 1. get spider info from dir
        if (spiderInfo == null) { // If it is not null, may it is
            spiderInfo = readSpiderInfoFromDataDir();
        }
        // Check stop
        if (mStop) {
            return;
        }

        // 2. get spider info download dir
        if (spiderInfo == null) {
            spiderInfo = readSpiderInfoFromDownloadDir();
        }
        // Check stop
        if (mStop) {
            return;
        }

        // 3. get spider info internet
        if (spiderInfo == null) {
            // spiderInfo must be non null or throw Exception
            spiderInfo = getSpiderInfoFromInternet(ehConfig);
        }
        // Check stop
        if (mStop) {
            return;
        }

        // 4. write it to file
        writeSpiderInfoToDataDir(spiderInfo);
        writeSpiderInfoToDownloadDir(spiderInfo);
        // Check stop
        if (mStop) {
            return;
        }

        // 5. Perpare
        // Update EhConfig preview size
        ehConfig.previewSize = spiderInfo.previewSize;
        ehConfig.setDirty();
        mSpiderInfo = spiderInfo;
        mNextIndex = 0;
        // Init AtomicIntegerArray stuff
        int length = spiderInfo.pages;
        mPageStates = new AtomicIntegerArray(length);
        for (int i = 0; i < length; i++) {
            mPageStates.lazySet(i, PAGE_STATE_NONE);
        }
        mSpiderWorkers = new SpiderWorker[3];
        mImageHandler = new ImageHandler(gb, spiderInfo.pages, mMode, mDownloadDir);
        // Check stop
        if (mStop) {
            return;
        }


        // TODO make sure onGetPages first then other listener
        // 7. tell listener pages
        mSpiderListener.onGetPages(spiderInfo.pages);

        // 6. Make workers and render to work
        ensureWorker();
        mRender = new Render();
        Thread thread = new Thread(mRender, "Render");
        thread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // 8. Get tokens
        handleToken(spiderInfo, ehConfig);

        // 5. The queen dies, workers die, render dies
        mWorkerArrayLock.lock();

        for (SpiderWorker worker : mSpiderWorkers) {
            if (worker != null) {
                worker.stop();
            }
        }
        synchronized (mWorkerLock) {
            mWorkerLock.notifyAll();
        }

        mWorkerArrayLock.unlock();

        mRender.stop();
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

    private void setNextIndex(int index) {
        synchronized (mNextIndexLock) {
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

                // Notify Queen
                synchronized (mQueenLock) {
                    mQueenLock.notify();
                }
            }
            return token;
        }
    }

    private int getRequestIndex() {
        synchronized (mNextIndexLock) {
            int length = mSpiderInfo.pages;

            for (;;) {
                if (mNextIndex >= length) {
                    if (!mHasCheckIndexAgain) {
                        mHasCheckIndexAgain = true;
                        mNextIndex = 0;
                    } else {
                        return -1;
                    }
                }

                int index = mNextIndex;
                if (mPageStates.get(mNextIndex++) == PAGE_STATE_NONE) {
                    return index;
                }
            }
        }
    }

    class SpiderWorker implements Runnable {

        private String TAG = SpiderWorker.class.getSimpleName();

        public int mIndex;

        private transient HttpRequest mCurrentHttpRequest;
        private transient ImageHandler.SaveHelper mCurrentSaveHelper;

        public boolean mStop = false;

        public SpiderWorker(int index) {
            mIndex = index;
        }

        // Notify by Queen
        public void stop() {
            mStop = true;
            if (mCurrentHttpRequest != null) {
                mCurrentHttpRequest.disconnect();
                mCurrentHttpRequest = null;
            }
            if (mCurrentSaveHelper != null) {
                mCurrentSaveHelper.cancel();
                mCurrentSaveHelper = null;
            }
        }

        private String getImageUrl(int gid, String token, int index) throws Exception {
            HttpRequest request = new HttpRequest();
            request.setUrl(EhUrl.getPageUrl(mSource, gid, token, index));
            try {
                mCurrentHttpRequest = request;
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
                mCurrentHttpRequest = null;
                request.disconnect();
            }
        }

        @Override
        public void run() {
            TAG = Thread.currentThread().getName();

            Say.d(TAG, Thread.currentThread().getName() + " starts");

            GalleryBase gb = mGalleryBase;
            ImageHandler handler = mImageHandler;

            while (!mStop) {
                // Get index
                final int index = getRequestIndex();
                if (index == -1) {
                    // There is no more
                    break;
                } else {
                    mPageStates.lazySet(index, PAGE_STATE_SPIDER);
                }

                // Check contain
                if (handler.contain(index)) {
                    // Find it, no need to download
                    mPageStates.lazySet(index, PAGE_STATE_SUCCEED);
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
                    mPageStates.lazySet(index, PAGE_STATE_FAILED);
                    // Listener
                    mSpiderListener.onSpiderFailed(index, e);
                    continue;
                }

                // Download image
                try {
                    mCurrentSaveHelper = new ImageHandler.SaveHelper() {
                        @Override
                        public void onStartSaving(long totalSize) {
                            mSpiderListener.onSpiderStart(index, totalSize);
                        }

                        @Override
                        public void onSave(long receivedSize) {
                            mSpiderListener.onSpiderPage(index, receivedSize);
                        }
                    };
                    handler.save(mHttpClient, imageUrl, index, mCurrentSaveHelper);
                    mPageStates.set(index, PAGE_STATE_SUCCEED);
                    // Listener
                    mSpiderListener.onSpiderSucceed(index);
                } catch (Exception e) {
                    mPageStates.set(index, PAGE_STATE_FAILED);
                    // Listener
                    mSpiderListener.onSpiderFailed(index, e);
                } finally {
                    mCurrentSaveHelper = null;
                }
            }

            // TODO this lock is not right, we need to lock when before check it is over
            mWorkerArrayLock.lock();
            mSpiderWorkers[mIndex] = null;
            mWorkerArrayLock.unlock();

            Say.d(TAG, Thread.currentThread().getName() + " dies");
        }
    }

    // The runner to handle request
    class Render implements Runnable {

        private final String TAG = Render.class.getSimpleName();

        private boolean mStop = false;

        private final Object mRenderLock = new Object();

        private BlockingQueue<Integer> mQueue = new LinkedBlockingQueue<>();

        public boolean mPauseAccess = false;
        private final Object mPauseLock = new Object();

        public void stop() {
            mStop = true;
            setPause(false);
            synchronized (mRenderLock) {
                mRenderLock.notify();
            }
        }

        public void setPause(final boolean pause) {
            synchronized (mPauseLock) {
                if (mPauseAccess != pause) {
                    mPauseAccess = pause;
                    if (!pause) {
                        mPauseLock.notify();
                    }
                }
            }
        }

        private void waitUntilUnpaused() {
            synchronized (mPauseLock) {
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    while (mPauseAccess) {
                        try {
                            mPauseLock.wait();
                        } catch (InterruptedException e) {
                            // ignored, we'll start waiting again
                        }
                    }
                }
            }
        }

        public void request(int index) {
            if (!mQueue.contains(index)) {
                mQueue.offer(index);
            }
            synchronized (mRenderLock) {
                mRenderLock.notify();
            }
        }

        @Override
        public void run() {
            Say.d(TAG, "Render starts");

            while (!mStop) {

                waitUntilUnpaused();

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

                mSpiderListener.onGetBitmap(index, mImageHandler == null ? null : mImageHandler.getBitmap(index));
            }

            Say.d(TAG, "Render dies");
        }
    }

    public interface SpiderListener {

        void onTotallyFailed(Exception e);

        void onPartlyFailed(Exception e);

        /**
         * The whole gallery has been spidered
         * TODO
         */
        void onDone();

        void onGetPages(int pages);

        void onSpiderStart(int index, long totalSize);

        void onSpiderPage(int index, long receivedSize);

        void onSpiderSucceed(int index);

        void onSpiderFailed(int index, Exception e);

        void onGetBitmap(int index, Bitmap bitmap);
    }
}
