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

package com.hippo.ehviewer.spider;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Process;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.webkit.MimeTypeMap;

import com.hippo.beerbelly.SimpleDiskCache;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.GetText;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.client.EhConfig;
import com.hippo.ehviewer.client.EhEngine;
import com.hippo.ehviewer.client.EhRequestBuilder;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.data.PreviewSet;
import com.hippo.ehviewer.client.exception.Image509Exception;
import com.hippo.ehviewer.client.exception.ParseException;
import com.hippo.ehviewer.client.parser.GalleryDetailParser;
import com.hippo.ehviewer.client.parser.GalleryPageParser;
import com.hippo.ehviewer.client.parser.GalleryPageUrlParser;
import com.hippo.ehviewer.gallery.GalleryProvider2;
import com.hippo.glgallery.GalleryPageView;
import com.hippo.glgallery.GalleryProvider;
import com.hippo.image.Image;
import com.hippo.streampipe.InputStreamPipe;
import com.hippo.streampipe.OutputStreamPipe;
import com.hippo.unifile.UniFile;
import com.hippo.util.ExceptionUtils;
import com.hippo.yorozuya.IOUtils;
import com.hippo.yorozuya.MathUtils;
import com.hippo.yorozuya.OSUtils;
import com.hippo.yorozuya.StringUtils;
import com.hippo.yorozuya.Utilities;
import com.hippo.yorozuya.collect.SparseJLArray;
import com.hippo.yorozuya.thread.PriorityThread;
import com.hippo.yorozuya.thread.PriorityThreadFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class SpiderQueen implements Runnable {

    private static final String TAG = SpiderQueen.class.getSimpleName();
    private static final AtomicInteger sIdGenerator = new AtomicInteger();
    private static final boolean DEBUG_LOG = false;
    private static final boolean DEBUG_PTOKEN = true;

    @IntDef({MODE_READ, MODE_DOWNLOAD})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Mode {}

    @IntDef({STATE_NONE, STATE_DOWNLOADING, STATE_FINISHED, STATE_FAILED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface State {}

    public static final int MODE_READ = 0;
    public static final int MODE_DOWNLOAD = 1;

    public static final int STATE_NONE = 0;
    public static final int STATE_DOWNLOADING = 1;
    public static final int STATE_FINISHED = 2;
    public static final int STATE_FAILED = 3;

    public static final int DECODE_THREAD_NUM = 1;

    public static final String SPIDER_INFO_FILENAME = ".ehviewer";

    private static final String[] URL_509_SUFFIX_ARRAY = {
            "/509.gif",
            "/509s.gif"
    };

    private static final SparseJLArray<SpiderQueen> sQueenMap = new SparseJLArray<>();

    @NonNull
    private final OkHttpClient mHttpClient;
    @NonNull
    private final SimpleDiskCache mSpiderInfoCache;
    @NonNull
    private final GalleryInfo mGalleryInfo;
    @NonNull
    private final SpiderDen mSpiderDen;

    private int mReadReference = 0;
    private int mDownloadReference = 0;

    // It mQueenThread is null, failed or stopped
    @Nullable
    private volatile Thread mQueenThread;
    private final Object mQueenLock = new Object();

    private final Thread[] mDecodeThreadArray = new Thread[DECODE_THREAD_NUM];
    private final int[] mDecodeIndexArray = new int[DECODE_THREAD_NUM];
    private final Queue<Integer> mDecodeRequestQueue = new LinkedList<>();

    private final Object mWorkerLock = new Object();
    private ThreadPoolExecutor mWorkerPoolExecutor;
    private int mWorkerCount;

    private final Object mPTokenLock = new Object();
    private final AtomicReference<SpiderInfo> mSpiderInfo = new AtomicReference<>();
    private final Queue<Integer> mRequestPTokenQueue = new ConcurrentLinkedQueue<>();

    private final Object mPageStateLock = new Object();
    private volatile int[] mPageStateArray;

    // Store request page. The index may be invalid
    private final Queue<Integer> mRequestPageQueue = new LinkedList<>();
    // Store preload page. The index may be invalid
    private final Queue<Integer> mRequestPageQueue2 = new LinkedList<>();
    // Store force request page. The index may be invalid
    private final Queue<Integer> mForceRequestPageQueue = new LinkedList<>();
    // For download, when it go to mPageStateArray.size(), done
    private volatile int mDownloadPage = -1;

    private final AtomicInteger mDownloadedPages = new AtomicInteger(0);
    private final AtomicInteger mFinishedPages = new AtomicInteger(0);

    // Store page error
    private final ConcurrentHashMap<Integer, String> mPageErrorMap = new ConcurrentHashMap<>();
    // Store page download percent
    private final ConcurrentHashMap<Integer, Float> mPagePercentMap = new ConcurrentHashMap<>();

    private final List<OnSpiderListener> mSpiderListeners = new ArrayList<>();

    private final int mWorkerMaxCount;
    private final int mPreloadNumber;

    private SpiderQueen(EhApplication application, @NonNull GalleryInfo galleryInfo) {
        mHttpClient = EhApplication.getOkHttpClient(application);
        mSpiderInfoCache = EhApplication.getSpiderInfoCache(application);
        mGalleryInfo = galleryInfo;
        mSpiderDen = new SpiderDen(mGalleryInfo);

        mWorkerMaxCount = MathUtils.clamp(Settings.getMultiThreadDownload(), 1, 10);
        mPreloadNumber = MathUtils.clamp(Settings.getPreloadImage(), 0, 100);

        for (int i = 0; i < DECODE_THREAD_NUM; i++) {
            mDecodeIndexArray[i] = GalleryPageView.INVALID_INDEX;
        }

        mWorkerPoolExecutor = new ThreadPoolExecutor(mWorkerMaxCount, mWorkerMaxCount,
                0, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>(),
                new PriorityThreadFactory(SpiderWorker.class.getSimpleName(), Process.THREAD_PRIORITY_BACKGROUND));
    }

    public void addOnSpiderListener(OnSpiderListener listener) {
        synchronized (mSpiderListeners) {
            mSpiderListeners.add(listener);
        }
    }

    public void removeOnSpiderListener(OnSpiderListener listener) {
        synchronized (mSpiderListeners) {
            mSpiderListeners.remove(listener);
        }
    }

    private void notifyGetPages(int pages) {
        synchronized (mSpiderListeners) {
            for (OnSpiderListener listener : mSpiderListeners) {
                listener.onGetPages(pages);
            }
        }
    }

    private void notifyGet509(int index) {
        synchronized (mSpiderListeners) {
            for (OnSpiderListener listener : mSpiderListeners) {
                listener.onGet509(index);
            }
        }
    }

    private void notifyPageDownload(int index, long contentLength, long receivedSize, int bytesRead) {
        synchronized (mSpiderListeners) {
            for (OnSpiderListener listener : mSpiderListeners) {
                listener.onPageDownload(index, contentLength, receivedSize, bytesRead);
            }
        }
    }

    private void notifyPageSuccess(int index) {
        int size = -1;
        int[] temp = mPageStateArray;
        if (temp != null) {
            size = temp.length;
        }
        synchronized (mSpiderListeners) {
            for (OnSpiderListener listener : mSpiderListeners) {
                listener.onPageSuccess(index, mFinishedPages.get(), mDownloadedPages.get(), size);
            }
        }
    }

    private void notifyPageFailure(int index, String error) {
        int size = -1;
        int[] temp = mPageStateArray;
        if (temp != null) {
            size = temp.length;
        }
        synchronized (mSpiderListeners) {
            for (OnSpiderListener listener : mSpiderListeners) {
                listener.onPageFailure(index, error, mFinishedPages.get(), mDownloadedPages.get(), size);
            }
        }
    }

    private void notifyFinish() {
        int size = -1;
        int[] temp = mPageStateArray;
        if (temp != null) {
            size = temp.length;
        }
        synchronized (mSpiderListeners) {
            for (OnSpiderListener listener : mSpiderListeners) {
                listener.onFinish(mFinishedPages.get(), mDownloadedPages.get(), size);
            }
        }
    }

    private void notifyGetImageSuccess(int index, Image image) {
        synchronized (mSpiderListeners) {
            for (OnSpiderListener listener : mSpiderListeners) {
                listener.onGetImageSuccess(index, image);
            }
        }
    }

    private void notifyGetImageFailure(int index, String error) {
        if (error == null) {
            error = GetText.getString(R.string.error_unknown);
        }
        synchronized (mSpiderListeners) {
            for (OnSpiderListener listener : mSpiderListeners) {
                listener.onGetImageFailure(index, error);
            }
        }
    }

    @UiThread
    public static SpiderQueen obtainSpiderQueen(@NonNull Context context,
            @NonNull GalleryInfo galleryInfo, @Mode int mode) {
        OSUtils.checkMainLoop();

        SpiderQueen queen = sQueenMap.get(galleryInfo.gid);
        if (queen == null) {
            EhApplication application = (EhApplication) context.getApplicationContext();
            queen = new SpiderQueen(application, galleryInfo);
            sQueenMap.put(galleryInfo.gid, queen);
            // Set mode
            queen.setMode(mode);
            queen.start();
        } else {
            // Set mode
            queen.setMode(mode);
        }
        return queen;
    }

    @UiThread
    public static void releaseSpiderQueen(@NonNull SpiderQueen queen, @Mode int mode) {
        OSUtils.checkMainLoop();

        // Clear mode
        queen.clearMode(mode);

        if (queen.mReadReference == 0 && queen.mDownloadReference == 0) {
            // Stop and remove if there is no reference
            queen.stop();
            sQueenMap.remove(queen.mGalleryInfo.gid);
        }
    }

    private void updateMode() {
        int mode;
        if (mDownloadReference > 0) {
            mode = MODE_DOWNLOAD;
        } else {
            mode = MODE_READ;
        }

        mSpiderDen.setMode(mode);

        // Update download page
        boolean intoDownloadMode = false;
        synchronized (mRequestPageQueue) {
            if (mode == MODE_DOWNLOAD) {
                if (mDownloadPage < 0) {
                    mDownloadPage = 0;
                    intoDownloadMode = true;
                }
            } else {
                mDownloadPage = -1;
            }
        }

        if (intoDownloadMode && mPageStateArray != null) {
            // Clear download state
            synchronized (mPageStateLock) {
                int[] temp = mPageStateArray;
                for (int i = 0, n = temp.length; i < n; i++) {
                    int oldState = temp[i];
                    if (STATE_DOWNLOADING != oldState) {
                        temp[i] = STATE_NONE;
                    }
                }
                mDownloadedPages.lazySet(0);
                mFinishedPages.lazySet(0);
                mPageErrorMap.clear();
                mPagePercentMap.clear();
            }
            // Ensure download workers
            ensureWorkers();
        }
    }

    private void setMode(@Mode int mode) {
        switch (mode) {
            case MODE_READ:
                mReadReference++;
                break;
            case MODE_DOWNLOAD:
                mDownloadReference++;
                break;
        }

        if (mDownloadReference > 1) {
            throw new IllegalStateException("mDownloadReference can't more than 0");
        }

        updateMode();
    }

    private void clearMode(@Mode int mode) {
        switch (mode) {
            case MODE_READ:
                mReadReference--;
                break;
            case MODE_DOWNLOAD:
                mDownloadReference--;
                break;
        }

        if (mReadReference < 0 || mDownloadReference < 0) {
            throw new IllegalStateException("Mode reference < 0");
        }

        updateMode();
    }

    private void start() {
        Thread queenThread = new PriorityThread(this, TAG + '-' + sIdGenerator.incrementAndGet(),
                Process.THREAD_PRIORITY_BACKGROUND);
        mQueenThread = queenThread;
        queenThread.start();
    }

    private void stop() {
        Thread queenThread = mQueenThread;
        if (queenThread != null) {
            queenThread.interrupt();
            mQueenThread = null;
        }
    }

    public int size() {
        if (mQueenThread == null) {
            return GalleryProvider.STATE_ERROR;
        } else if (mPageStateArray == null) {
            return GalleryProvider.STATE_WAIT;
        } else {
            return mPageStateArray.length;
        }
    }

    public String getError() {
        if (mQueenThread == null) {
            return "Error";
        } else {
            return null;
        }
    }

    public Object forceRequest(int index) {
        return request(index, true, false);
    }

    public Object request(int index) {
        return request(index, false, true);
    }

    private int getPageState(int index) {
        synchronized (mPageStateLock) {
            if (mPageStateArray != null && index >= 0 && index < mPageStateArray.length) {
                return mPageStateArray[index];
            } else {
                return STATE_NONE;
            }
        }
    }

    private void tryToEnsureWorkers() {
        boolean startWorkers = false;
        synchronized (mRequestPageQueue) {
            if (mPageStateArray != null &&
                    (!mForceRequestPageQueue.isEmpty() ||
                            !mRequestPageQueue.isEmpty() ||
                            !mRequestPageQueue2.isEmpty() ||
                            mDownloadPage >= 0 && mDownloadPage < mPageStateArray.length)) {
                startWorkers = true;
            }
        }

        if (startWorkers) {
            ensureWorkers();
        }
    }

    public void cancelRequest(int index) {
        if (mQueenThread == null) {
            return;
        }

        synchronized (mRequestPageQueue) {
            mRequestPageQueue.remove(index);
        }
        synchronized (mDecodeRequestQueue) {
            mDecodeRequestQueue.remove(index);
        }
    }

    /**
     * @return
     * String for error<br>
     * Float for download percent<br>
     * null for wait
     */
    private Object request(int index, boolean force, boolean addNeighbor) {
        if (mQueenThread == null) {
            return null;
        }

        // Get page state
        int state = getPageState(index);

        // Fix state for force
        if (force && (state == STATE_FINISHED || state == STATE_FAILED)) {
            // Update state to none at once
            updatePageState(index, STATE_NONE);
            state = STATE_NONE;
        }

        // Add to request
        synchronized (mRequestPageQueue) {
            if (state == STATE_NONE) {
                if (force) {
                    mForceRequestPageQueue.add(index);
                } else {
                    mRequestPageQueue.add(index);
                }
            }

            // Add next some pages to request queue
            if (addNeighbor) {
                mRequestPageQueue2.clear();
                int[] pageStateArray = mPageStateArray;
                int size;
                if (pageStateArray != null) {
                    size = pageStateArray.length;
                } else {
                    size = Integer.MAX_VALUE;
                }
                for (int i = index + 1, n = index + i + mPreloadNumber; i < n && i < size; i++) {
                    if (STATE_NONE == getPageState(i)) {
                        mRequestPageQueue2.add(i);
                    }
                }
            }
        }

        Object result;

        switch (state) {
            default:
            case STATE_NONE:
                result = null;
                break;
            case STATE_DOWNLOADING:
                result = mPagePercentMap.get(index);
                break;
            case STATE_FAILED:
                String error = mPageErrorMap.get(index);
                if (error == null) {
                    error = GetText.getString(R.string.error_unknown);
                }
                result = error;
                break;
            case STATE_FINISHED:
                synchronized (mDecodeRequestQueue) {
                    if (!contain(mDecodeIndexArray, index) && !mDecodeRequestQueue.contains(index)) {
                        mDecodeRequestQueue.add(index);
                        mDecodeRequestQueue.notify();
                    }
                }
                result = null;
                break;
        }

        tryToEnsureWorkers();

        return result;
    }

    public static boolean contain(int[] array, int value) {
        for (int v: array) {
            if (v == value) {
                return true;
            }
        }
        return false;
    }

    private void ensureWorkers() {
        synchronized (mWorkerLock) {
            if (null == mWorkerPoolExecutor) {
                Log.e(TAG, "Try to start worker after stopped");
                return;
            }

            for (; mWorkerCount < mWorkerMaxCount; mWorkerCount++) {
                mWorkerPoolExecutor.execute(new SpiderWorker());
            }
        }
    }

    public boolean save(int index, @NonNull UniFile file) {
        int state = getPageState(index);
        if (STATE_FINISHED != state) {
            return false;
        }

        InputStreamPipe pipe = mSpiderDen.openInputStreamPipe(index);
        if (null == pipe) {
            return false;
        }

        OutputStream os = null;
        try {
            os = file.openOutputStream();
            pipe.obtain();
            IOUtils.copy(pipe.open(), os);
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            pipe.close();
            pipe.release();
            IOUtils.closeQuietly(os);
        }
    }

    @Nullable
    public UniFile save(int index, @NonNull UniFile dir, @NonNull String filename) {
        int state = getPageState(index);
        if (STATE_FINISHED != state) {
            return null;
        }

        InputStreamPipe pipe = mSpiderDen.openInputStreamPipe(index);
        if (null == pipe) {
            return null;
        }

        OutputStream os = null;
        try {
            pipe.obtain();

            // Get dst file
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(pipe.open(), null, options);
            pipe.close();
            String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(options.outMimeType);
            UniFile dst = dir.subFile(null != extension ? filename + "." + extension : filename);
            if (null == dst) {
                return null;
            }

            // Copy
            os = dst.openOutputStream();
            IOUtils.copy(pipe.open(), os);
            return dst;
        } catch (IOException e) {
            return null;
        } finally {
            pipe.close();
            pipe.release();
            IOUtils.closeQuietly(os);
        }
    }

    public int getStartPage() {
        SpiderInfo spiderInfo = readSpiderInfoFromLocal();
        if (spiderInfo != null) {
            mSpiderInfo.lazySet(spiderInfo);
            return spiderInfo.startPage;
        } else {
            return 0;
        }
    }

    public void putStartPage(int page) {
        final SpiderInfo spiderInfo = mSpiderInfo.get();
        if (spiderInfo != null) {
            spiderInfo.startPage = page;
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    writeSpiderInfoToLocal(spiderInfo);
                    return null;
                }
            }.execute();
        }
    }

    private synchronized SpiderInfo readSpiderInfoFromLocal() {
        SpiderInfo spiderInfo = mSpiderInfo.get();
        if (spiderInfo != null) {
            return spiderInfo;
        }

        // Read from download dir
        UniFile downloadDir = mSpiderDen.getDownloadDir();
        if (downloadDir != null) {
            UniFile file = downloadDir.findFile(SPIDER_INFO_FILENAME);
            spiderInfo = SpiderInfo.read(file);
            if (spiderInfo != null && spiderInfo.gid == mGalleryInfo.gid &&
                    spiderInfo.token.equals(mGalleryInfo.token)) {
                return spiderInfo;
            }
        }

        // Read from cache
        InputStreamPipe pipe = mSpiderInfoCache.getInputStreamPipe(Long.toString(mGalleryInfo.gid));
        if (null != pipe) {
            try {
                pipe.obtain();
                spiderInfo = SpiderInfo.read(pipe.open());
                if (spiderInfo != null && spiderInfo.gid == mGalleryInfo.gid &&
                        spiderInfo.token.equals(mGalleryInfo.token)) {
                    return spiderInfo;
                }
            } catch (IOException e) {
                // Ignore
            } finally {
                pipe.close();
                pipe.release();
            }
        }

        return null;
    }

    private void readPreviews(String body, int index, SpiderInfo spiderInfo) throws ParseException {
        spiderInfo.pages = GalleryDetailParser.parsePages(body);
        spiderInfo.previewPages = GalleryDetailParser.parsePreviewPages(body);
        PreviewSet previewSet = GalleryDetailParser.parsePreviewSet(body);
        if ((index >= 0 && index < spiderInfo.pages - 1) || (index == 0 && spiderInfo.pages == 1)) {
            spiderInfo.previewPerPage = previewSet.size();
        } else {
            spiderInfo.previewPerPage = Math.max(spiderInfo.previewPerPage, previewSet.size());
        }

        for (int i = 0, n = previewSet.size(); i < n; i++) {
            GalleryPageUrlParser.Result result = GalleryPageUrlParser.parse(previewSet.getPageUrlAt(i));
            if (result != null) {
                synchronized (mPTokenLock) {
                    spiderInfo.pTokenMap.put(result.page, result.pToken);
                }
            }
        }
    }

    private SpiderInfo readSpiderInfoFromInternet(EhConfig config) {
        try {
            SpiderInfo spiderInfo = new SpiderInfo();
            spiderInfo.gid = mGalleryInfo.gid;
            spiderInfo.token = mGalleryInfo.token;

            Request request = new EhRequestBuilder(EhUrl.getGalleryDetailUrl(
                    mGalleryInfo.gid, mGalleryInfo.token, 0, false), config).build();
            Response response = mHttpClient.newCall(request).execute();
            String body = response.body().string();

            spiderInfo.pages = GalleryDetailParser.parsePages(body);
            spiderInfo.pTokenMap = new SparseArray<>(spiderInfo.pages);
            readPreviews(body, 0, spiderInfo);
            return spiderInfo;
        } catch (Exception e) {
            return null;
        }
    }

    private String getPTokenFromInternet(int index, EhConfig config) {
        SpiderInfo spiderInfo = mSpiderInfo.get();
        if (spiderInfo == null) {
            return null;
        }

        // Check previewIndex
        int previewIndex;
        if (spiderInfo.previewPerPage >= 0) {
            previewIndex = index / spiderInfo.previewPerPage;
        } else {
            previewIndex = 0;
        }

        try {
            String url = EhUrl.getGalleryDetailUrl(
                    mGalleryInfo.gid, mGalleryInfo.token, previewIndex, false);
            if (DEBUG_PTOKEN) {
                Log.d(TAG, "index " + index + ", previewIndex " + previewIndex +
                        ", previewPerPage " + spiderInfo.previewPerPage+ ", url " + url);
            }
            Request request = new EhRequestBuilder(url, config).build();
            Response response = mHttpClient.newCall(request).execute();
            String body = response.body().string();
            readPreviews(body, previewIndex, spiderInfo);

            // Save to local
            writeSpiderInfoToLocal(spiderInfo);

            String pToken;
            synchronized (mPTokenLock) {
                pToken = spiderInfo.pTokenMap.get(index);
            }
            return pToken;
        } catch (Exception e) {
            return null;
        }
    }

    private synchronized void writeSpiderInfoToLocal(@NonNull SpiderInfo spiderInfo) {
        // Write to download dir
        UniFile downloadDir = mSpiderDen.getDownloadDir();
        if (downloadDir != null) {
            UniFile file = downloadDir.createFile(SPIDER_INFO_FILENAME);
            try {
                spiderInfo.write(file.openOutputStream());
            } catch (Exception e) {
                // Ignore
            }
        }

        // Read from cache
        OutputStreamPipe pipe = mSpiderInfoCache.getOutputStreamPipe(Long.toString(mGalleryInfo.gid));
        try {
            pipe.obtain();
            spiderInfo.write(pipe.open());
        } catch (IOException e) {
            // Ignore
        } finally {
            pipe.close();
            pipe.release();
        }
    }

    private void runInternal() {
        // Get EhConfig
        EhConfig config = Settings.getEhConfig().clone();
        config.previewSize = EhConfig.PREVIEW_SIZE_NORMAL;
        config.setDirty();

        // Read spider info
        SpiderInfo spiderInfo = readSpiderInfoFromLocal();

        // Check interrupted
        if (Thread.currentThread().isInterrupted()) {
            return;
        }

        // Spider info from internet
        if (spiderInfo == null) {
            spiderInfo = readSpiderInfoFromInternet(config);
        }

        // Error! Can't get spiderInfo
        if (spiderInfo == null) {
            return;
        }
        mSpiderInfo.lazySet(spiderInfo);

        // Check interrupted
        if (Thread.currentThread().isInterrupted()) {
            return;
        }

        // Write spider info to file
        writeSpiderInfoToLocal(spiderInfo);

        // Check interrupted
        if (Thread.currentThread().isInterrupted()) {
            return;
        }

        // Setup page state
        synchronized (mPageStateLock) {
            mPageStateArray = new int[spiderInfo.pages];
        }

        // Notify get pages
        notifyGetPages(spiderInfo.pages);

        // Ensure worker
        tryToEnsureWorkers();

        // Start decoder
        for (int i = 0; i < DECODE_THREAD_NUM; i++) {
            Thread decoderThread = new PriorityThread(new SpiderDecoder(i),
                    "SpiderDecoder-" + i, Process.THREAD_PRIORITY_DEFAULT);
            mDecodeThreadArray[i] = decoderThread;
            decoderThread.start();
        }

        // handle pToken request
        while (!Thread.currentThread().isInterrupted()) {
            Integer index = mRequestPTokenQueue.poll();

            if (index == null) {
                // No request index, wait here
                synchronized (mQueenLock) {
                    try {
                        mQueenLock.wait();
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                continue;
            }

            // Check it in spider info
            String pToken;
            synchronized (mPTokenLock) {
                pToken = spiderInfo.pTokenMap.get(index);
            }
            if (pToken != null) {
                // Get pToken from spider info, notify worker
                synchronized (mWorkerLock) {
                    mWorkerLock.notifyAll();
                }
                continue;
            }

            // Get pToken from internet
            pToken = getPTokenFromInternet(index, config);
            if (null == pToken) {
                // Preview size may changed, so try to get pToken twice
                pToken = getPTokenFromInternet(index, config);
            }

            if (null == pToken) {
                // If failed, set the pToken "failed"
                synchronized (mPTokenLock) {
                    spiderInfo.pTokenMap.put(index, SpiderInfo.TOKEN_FAILED);
                }
            }

            // Notify worker
            synchronized (mWorkerLock) {
                mWorkerLock.notifyAll();
            }
        }
    }

    @Override
    public void run() {
        if (DEBUG_LOG) {
            Log.i(TAG, Thread.currentThread().getName() + ": start");
        }

        runInternal();

        // Set mQueenThread null
        mQueenThread = null;

        // Interrupt decoder
        for (Thread decoderThread : mDecodeThreadArray) {
            if (decoderThread != null) {
                decoderThread.interrupt();
            }
        }

        // Interrupt all workers
        synchronized (mWorkerLock) {
            mWorkerPoolExecutor.shutdownNow();
            mWorkerPoolExecutor = null;
        }
        notifyFinish();

        if (DEBUG_LOG) {
            Log.i(TAG, Thread.currentThread().getName() + ": end");
        }
    }

    private void updatePageState(int index, @State int state) {
        updatePageState(index, state, null);
    }

    private boolean isStateDone(int state) {
        return state == STATE_FINISHED || state == STATE_FAILED;
    }

    private void updatePageState(int index, @State int state, String error) {
        int oldState;
        synchronized (mPageStateLock) {
            oldState = mPageStateArray[index];
            mPageStateArray[index] = state;

            if (!isStateDone(oldState) && isStateDone(state)) {
                mDownloadedPages.incrementAndGet();
            } else if (isStateDone(oldState) && !isStateDone(state)) {
                mDownloadedPages.decrementAndGet();
            }
            if (oldState != STATE_FINISHED && state == STATE_FINISHED) {
                mFinishedPages.incrementAndGet();
            } else if (oldState == STATE_FINISHED && state != STATE_FINISHED) {
                mFinishedPages.decrementAndGet();
            }

            // Clear
            if (state == STATE_DOWNLOADING) {
                mPageErrorMap.remove(index);
            } else if (state == STATE_FINISHED || state == STATE_FAILED) {
                mPagePercentMap.remove(index);
            }

            // Get default error
            if (state == STATE_FAILED) {
                if (error == null) {
                    error = GetText.getString(R.string.error_unknown);
                }
                mPageErrorMap.put(index, error);
            }
        }

        // Notify listeners
        if (state == STATE_FAILED) {
            notifyPageFailure(index, error);
        } else if (state == STATE_FINISHED) {
            notifyPageSuccess(index);
        }
    }

    private class SpiderWorker implements Runnable {

        private final long mGid;

        public SpiderWorker() {
            mGid = mGalleryInfo.gid;
        }

        private GalleryPageParser.Result getImageUrl(long gid, int index, String pToken,
                String skipHathKey) throws Exception {
            String url = EhUrl.getPageUrl(gid, index, pToken);
            if (skipHathKey != null) {
                url = url + "?nl=" + skipHathKey;
            }

            GalleryPageParser.Result result = EhEngine.getGalleryPage(null, mHttpClient, url);
            if (StringUtils.endsWith(result.imageUrl, URL_509_SUFFIX_ARRAY)) {
                // Get 509
                // Notify listeners
                notifyGet509(index);
                throw new Image509Exception();
            }

            return result;
        }

        // false for stop
        private boolean downloadImage(long gid, int index, String pToken, boolean force) {
            String skipHathKey = null;
            String imageUrl;
            String error = null;
            boolean interrupt = false;

            // Try twice
            for (int i = 0; i < 2; i++) {
                if (i > 0 && TextUtils.isEmpty(skipHathKey)) {
                    // No need to get image url twice without skip hath key
                    break;
                }

                GalleryPageParser.Result result = null;
                try {
                    result = getImageUrl(gid, index, pToken, skipHathKey);
                } catch (Image509Exception e) {
                    error = GetText.getString(R.string.error_509);
                } catch (Exception e) {
                    error = ExceptionUtils.getReadableString(e);
                }
                if (result == null) {
                    // Get image url failed
                    break;
                }
                // Check interrupted
                if (Thread.currentThread().isInterrupted()) {
                    interrupt = true;
                    break;
                }

                if (Settings.getDownloadOriginImage() && !TextUtils.isEmpty(result.originImageUrl)) {
                    imageUrl = result.originImageUrl;
                } else {
                    imageUrl = result.imageUrl;
                }
                skipHathKey = result.skipHathKey;

                // If it is force request, skip first image
                if (force && i == 0) {
                    continue;
                }

                if (DEBUG_LOG) {
                    Log.d(TAG, imageUrl);
                }

                // Download image
                OutputStreamPipe pipe = null;
                InputStream is = null;
                try {
                    if (DEBUG_LOG) {
                        Log.d(TAG, "Start download image " + index);
                    }

                    Call call = mHttpClient.newCall(new EhRequestBuilder(imageUrl).build());
                    Response response = call.execute();
                    if (response.code() >= 400) {
                        // Maybe 404
                        response.body().close();
                        continue;
                    }
                    ResponseBody responseBody = response.body();

                    // Get extension
                    String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(
                            responseBody.contentType().toString());
                    // Ensure extension
                    if (!Utilities.contain(GalleryProvider2.SUPPORT_IMAGE_EXTENSIONS, extension)) {
                        extension = GalleryProvider2.SUPPORT_IMAGE_EXTENSIONS[0];
                    }

                    // Get out put pipe
                    pipe = mSpiderDen.openOutputStreamPipe(index, extension);
                    if (null == pipe) {
                        // Can't get pipe
                        error = GetText.getString(R.string.error_write_failed);
                        response.body().close();
                        break;
                    }

                    long contentLength = responseBody.contentLength();
                    is = responseBody.byteStream();
                    pipe.obtain();
                    OutputStream os = pipe.open();

                    final byte data[] = new byte[1024 * 4];
                    long receivedSize = 0;

                    while (!Thread.currentThread().isInterrupted()) {
                        int bytesRead = is.read(data);
                        if (bytesRead == -1) {
                            response.body().close();
                            break;
                        }
                        os.write(data, 0, bytesRead);
                        receivedSize += bytesRead;
                        // Update page percent
                        if (contentLength > 0) {
                            mPagePercentMap.put(index, (float) receivedSize / contentLength);
                        }
                        // Notify listener
                        notifyPageDownload(index, contentLength, receivedSize, bytesRead);
                    }
                    os.flush();

                    // Check interrupted
                    if (Thread.currentThread().isInterrupted()) {
                        interrupt = true;
                        break;
                    }

                    if (DEBUG_LOG) {
                        Log.d(TAG, "Download image succeed " + index);
                    }

                    // Download finished
                    updatePageState(index, STATE_FINISHED);
                    return true;
                } catch (IOException e) {
                    error = GetText.getString(R.string.error_socket);
                } finally {
                    IOUtils.closeQuietly(is);
                    if (null != pipe) {
                        pipe.close();
                        pipe.release();
                    }

                    if (DEBUG_LOG) {
                        Log.d(TAG, "End download image " + index);
                    }
                }
            }

            // Remove download failed image
            mSpiderDen.remove(index);

            updatePageState(index, STATE_FAILED, error);
            return !interrupt;
        }

        // false for stop
        private boolean runInternal() {
            SpiderInfo spiderInfo = mSpiderInfo.get();
            if (spiderInfo == null) {
                return false;
            }

            int size = mPageStateArray.length;

            // Get request index
            int index;
            // From force request
            boolean force = false;
            synchronized (mRequestPageQueue) {
                if (!mForceRequestPageQueue.isEmpty()) {
                    index = mForceRequestPageQueue.remove();
                    force = true;
                } else if (!mRequestPageQueue.isEmpty()) {
                    index = mRequestPageQueue.remove();
                } else if (!mRequestPageQueue2.isEmpty()) {
                    index = mRequestPageQueue2.remove();
                } else if (mDownloadPage >= 0 && mDownloadPage < size) {
                    index = mDownloadPage;
                    mDownloadPage++;
                } else {
                    // No index any more, stop
                    return false;
                }

                // Check out of range
                if (index < 0 || index >= size) {
                    // Invalid index
                    return true;
                }
            }

            synchronized (mPageStateLock) {
                // Check the page state
                int state = mPageStateArray[index];
                if (state == STATE_DOWNLOADING || (!force && (state == STATE_FINISHED || state == STATE_FAILED))) {
                    return true;
                }

                // Set state downloading
                updatePageState(index, STATE_DOWNLOADING);
            }

            // Check exist for not force request
            if (!force && mSpiderDen.contain(index)) {
                updatePageState(index , STATE_FINISHED);
                return true;
            }

            // Clear TOKEN_FAILED for force request
            if (force) {
                synchronized (mPTokenLock) {
                    int i = spiderInfo.pTokenMap.indexOfKey(index);
                    if (i >= 0) {
                        String pToken = spiderInfo.pTokenMap.valueAt(i);
                        if (SpiderInfo.TOKEN_FAILED.equals(pToken)) {
                            spiderInfo.pTokenMap.remove(i);
                        }
                    }
                }
            }

            String pToken = null;
            // Get token
            while (!Thread.currentThread().isInterrupted()) {
                synchronized (mPTokenLock) {
                    pToken = spiderInfo.pTokenMap.get(index);
                }
                if (pToken == null) {
                    mRequestPTokenQueue.add(index);
                    // Notify Queen
                    synchronized (mQueenLock) {
                        mQueenLock.notify();
                    }
                    // Wait
                    synchronized (mWorkerLock) {
                        try {
                            mWorkerLock.wait();
                        } catch (InterruptedException e) {
                            // Interrupted
                            if (DEBUG_LOG) {
                                Log.d(TAG, Thread.currentThread().getName() + " Interrupted");
                            }
                            break;
                        }
                    }
                } else {
                    break;
                }
            }

            if (pToken == null) {
                // Interrupted
                // Get token failed
                updatePageState(index, STATE_FAILED, null);
                return false;
            }

            if (SpiderInfo.TOKEN_FAILED.equals(pToken)) {
                // Get token failed
                updatePageState(index, STATE_FAILED, GetText.getString(R.string.error_get_ptoken_error));
                return true;
            }

            // Get image url
            return downloadImage(mGid, index, pToken, force);
        }

        @Override
        @SuppressWarnings("StatementWithEmptyBody")
        public void run() {
            if (DEBUG_LOG) {
                Log.i(TAG, Thread.currentThread().getName() + ": start");
            }

            while (mSpiderDen.isReady() && !Thread.currentThread().isInterrupted() && runInternal());

            boolean finish;
            // Clear in spider worker array
            synchronized (mWorkerLock) {
                mWorkerCount--;
                if (mWorkerCount < 0) {
                    Log.e(TAG, "WTF, mWorkerCount < 0, not thread safe or something wrong");
                    mWorkerCount = 0;
                }
                finish = mWorkerCount <= 0;
            }

            if (finish) {
                notifyFinish();
            }

            if (DEBUG_LOG) {
                Log.i(TAG, Thread.currentThread().getName() + ": end");
            }
        }
    }

    private class SpiderDecoder implements Runnable {

        private final int mThreadIndex;

        public SpiderDecoder(int index) {
            mThreadIndex = index;
        }

        private void resetDecodeIndex() {
            synchronized (mDecodeRequestQueue) {
                mDecodeIndexArray[mThreadIndex] = GalleryPageView.INVALID_INDEX;
            }
        }

        @Override
        public void run() {
            if (DEBUG_LOG) {
                Log.i(TAG, Thread.currentThread().getName() + ": start");
            }

            while (!Thread.currentThread().isInterrupted()) {
                int index;
                synchronized (mDecodeRequestQueue) {
                    if (mDecodeRequestQueue.isEmpty()) {
                        try {
                            mDecodeRequestQueue.wait();
                        } catch (InterruptedException e) {
                            // Interrupted
                            break;
                        }
                        continue;
                    }
                    index = mDecodeRequestQueue.remove();
                    mDecodeIndexArray[mThreadIndex] = index;
                }

                // Check index valid
                if (index < 0 || index >= mPageStateArray.length) {
                    resetDecodeIndex();
                    notifyGetImageFailure(index, GetText.getString(R.string.error_out_of_range));
                    continue;
                }

                InputStreamPipe pipe = mSpiderDen.openInputStreamPipe(index);
                if (pipe == null) {
                    resetDecodeIndex();
                    // Can't find the file, it might be removed from cache,
                    // Reset it state and request it
                    updatePageState(index, STATE_NONE, null);
                    request(index, false, false);
                    continue;
                }

                Image image = null;
                String error = null;
                InputStream is;

                pipe.obtain();
                try {
                    is = new AutoCloseInputStream(pipe, pipe.open());
                } catch (IOException e) {
                    // Can't open pipe
                    error = GetText.getString(R.string.error_reading_failed);
                    is = null;
                    pipe.close();
                    pipe.release();
                }

                if (is != null) {
                    image = Image.decode(is, true);
                    if (image == null) {
                        error = GetText.getString(R.string.error_decoding_failed);
                    }
                }

                // Notify
                if (image != null) {
                    notifyGetImageSuccess(index, image);
                } else {
                    notifyGetImageFailure(index, error);
                }

                resetDecodeIndex();
            }

            if (DEBUG_LOG) {
                Log.i(TAG, Thread.currentThread().getName() + ": end");
            }
        }
    }

    private class AutoCloseInputStream extends InputStream {

        private final InputStreamPipe mPipe;
        private final InputStream mIs;

        public AutoCloseInputStream(InputStreamPipe pipe, InputStream is) {
            mPipe = pipe;
            mIs = is;
        }

        @Override
        public int read() throws IOException {
            return mIs.read();
        }

        @Override
        public int read(@NonNull byte[] buffer, int byteOffset, int byteCount) throws IOException {
            return mIs.read(buffer, byteOffset, byteCount);
        }

        @Override
        public void close() throws IOException {
            mPipe.close();
            mPipe.release();
        }
    }

    public interface OnSpiderListener {

        void onGetPages(int pages);

        void onGet509(int index);

        /**
         * @param contentLength -1 for unknown
         */
        void onPageDownload(int index, long contentLength, long receivedSize, int bytesRead);

        void onPageSuccess(int index, int finished, int downloaded, int total);

        void onPageFailure(int index, String error, int finished, int downloaded, int total);

        /**
         * All workers end
         */
        void onFinish(int finished, int downloaded, int total);

        void onGetImageSuccess(int index, Image image);

        void onGetImageFailure(int index, String error);
    }
}
