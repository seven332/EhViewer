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

package com.hippo.ehviewer.download;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;

import com.hippo.ehviewer.EhDB;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.dao.DownloadInfoRaw;
import com.hippo.ehviewer.dao.DownloadLabelRaw;
import com.hippo.ehviewer.spider.SpiderQueen;
import com.hippo.image.Image;
import com.hippo.yorozuya.ConcurrentPool;
import com.hippo.yorozuya.IntList;
import com.hippo.yorozuya.ObjectUtils;
import com.hippo.yorozuya.SimpleHandler;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import de.greenrobot.dao.query.LazyList;

public class DownloadManager implements SpiderQueen.OnSpiderListener {

    private static final String TAG = DownloadManager.class.getSimpleName();

    private final Context mContext;

    @NonNull
    private final Map<String, LinkedList<DownloadInfo>> mMap;
    // All labels without default label
    @NonNull
    private final List<DownloadLabelRaw> mLabelList;
    // Store download info with default label
    @NonNull
    private final LinkedList<DownloadInfo> mDefaultInfoList;
    // For quick search
    @NonNull
    private final SparseArray<DownloadInfo> mAllInfoMap;
    // Store download info wait to start
    @NonNull
    private final LinkedList<DownloadInfo> mWaitList;
    @NonNull
    private final SpeedReminder mSpeedReminder;

    @Nullable
    private DownloadListener mDownloadListener;
    @Nullable
    private DownloadInfoListener mDownloadInfoListener;

    @Nullable
    private DownloadInfo mCurrentTask;
    @Nullable
    private SpiderQueen mCurrentSpider;

    @NonNull
    private final ConcurrentPool<NotifyTask> mNotifyTaskPool = new ConcurrentPool<>(5);

    public DownloadManager(Context context) {
        mContext = context;

        List<DownloadLabelRaw> labels = EhDB.getAllDownloadLabelList();
        mLabelList = labels;

        // Create list for each label
        HashMap<String, LinkedList<DownloadInfo>> map = new HashMap<>();
        mMap = map;
        for (DownloadLabelRaw label : labels) {
            map.put(label.getLabel(), new LinkedList<DownloadInfo>());
        }
        // Create default for non tag
        mDefaultInfoList = new LinkedList<>();
        // Create all download info map
        SparseArray<DownloadInfo> allInfoMap = new SparseArray<>();
        mAllInfoMap = allInfoMap;

        // Fill download info list
        LazyList<DownloadInfoRaw> lazyList = EhDB.getDownloadInfoLazyList();
        for (DownloadInfoRaw raw : lazyList) {
            DownloadInfo info = createDownloadInfo(raw);
            if (info == null) {
                continue;
            }

            // Add to all info map
            allInfoMap.put(info.galleryInfo.gid, info);

            // Add to list
            LinkedList<DownloadInfo> list = getInfoListForLabel(info.label);
            if (list == null) {
                list = new LinkedList<>();
                map.put(info.label, list);
                if (!containLabel(info.label)) {
                    // Add label to DB and list
                    labels.add(EhDB.addDownloadLabel(info.label));
                }
            }
            list.add(info);
        }
        lazyList.close();

        // Create wait list
        mWaitList = new LinkedList<>();

        mSpeedReminder = new SpeedReminder();
    }

    @Nullable
    private static DownloadInfo createDownloadInfo(DownloadInfoRaw raw) {
        GalleryInfo gi = EhDB.getGalleryInfo((int) (long) raw.getGid());
        if (gi == null) {
            Log.d(TAG, "Can't get GalleryInfo: " + raw.getGid());
            return null;
        }

        DownloadInfo info = new DownloadInfo();
        int state = raw.getState();
        if (state == DownloadInfo.STATE_WAIT || state == DownloadInfo.STATE_DOWNLOAD) {
            state = DownloadInfo.STATE_NONE;
        }
        info.galleryInfo = gi;
        info.state = state;
        info.legacy = raw.getLegacy();
        info.date = raw.getDate();
        info.label = raw.getLabel();
        return info;
    }

    @Nullable
    private LinkedList<DownloadInfo> getInfoListForLabel(String label) {
        if (label == null) {
            return mDefaultInfoList;
        } else {
            return mMap.get(label);
        }
    }

    public boolean containLabel(String label) {
        if (label == null) {
            return false;
        }

        for (DownloadLabelRaw raw: mLabelList) {
            if (label.equals(raw.getLabel())) {
                return true;
            }
        }

        return false;
    }

    @NonNull
    public List<DownloadLabelRaw> getLabelList() {
        return mLabelList;
    }

    @NonNull
    public List<DownloadInfo> getDefaultDownloadInfoList() {
        return mDefaultInfoList;
    }

    @Nullable
    public List<DownloadInfo> getLabelDownloadInfoList(String label) {
        return mMap.get(label);
    }

    public void setDownloadInfoListener(@Nullable DownloadInfoListener downloadInfoListener) {
        mDownloadInfoListener = downloadInfoListener;
    }

    public void setDownloadListener(@Nullable DownloadListener listener) {
        mDownloadListener = listener;
    }

    private void ensureDownload() {
        if (mCurrentTask != null) {
            // Only one download
            return;
        }

        // Get download from wait list
        if (!mWaitList.isEmpty()) {
            DownloadInfo info = mWaitList.removeFirst();
            SpiderQueen spider = SpiderQueen.obtainSpiderQueen(mContext, info.galleryInfo, SpiderQueen.MODE_DOWNLOAD);
            mCurrentTask = info;
            mCurrentSpider = spider;
            spider.addOnSpiderListener(this);
            info.state = DownloadInfo.STATE_DOWNLOAD;
            info.speed = -1;
            info.total = -1;
            info.download = 0;
            info.legacy = -1;
            // Update in DB
            EhDB.updateDownloadInfo(info);
            // Start speed count
            mSpeedReminder.start();
            // Notify start downloading
            if (mDownloadListener != null) {
                mDownloadListener.onStart(info);
            }
            // Notify state update
            List<DownloadInfo> list = getInfoListForLabel(info.label);
            if (list != null && mDownloadInfoListener != null) {
                mDownloadInfoListener.onUpdate(info, list);
            }
        }
    }

    void startDownload(GalleryInfo galleryInfo, @Nullable String label) {
        if (mCurrentTask != null && mCurrentTask.galleryInfo.gid == galleryInfo.gid) {
            // It is current task
            return;
        }

        // Check in download list
        DownloadInfo info = mAllInfoMap.get(galleryInfo.gid);
        if (info != null) { // Get it in download list
            if (info.state != DownloadInfo.STATE_WAIT) {
                // Set state DownloadInfo.STATE_WAIT
                info.state = DownloadInfo.STATE_WAIT;
                // Add to wait list
                mWaitList.add(info);
                // Update in DB
                EhDB.updateDownloadInfo(info);
                // Notify state update
                List<DownloadInfo> list = getInfoListForLabel(info.label);
                if (list != null && mDownloadInfoListener != null) {
                    mDownloadInfoListener.onUpdate(info, list);
                }
                // Make sure download is running
                ensureDownload();
            }
        } else {
            // It is new download info
            info = new DownloadInfo();
            info.galleryInfo = galleryInfo;
            info.label = label;
            info.state = DownloadInfo.STATE_WAIT;
            info.date = System.currentTimeMillis();

            // Add to label download list
            LinkedList<DownloadInfo> list = getInfoListForLabel(info.label);
            if (list == null) {
                Log.e(TAG, "Can't find download info list with label: " + label);
                return;
            }
            list.add(info);

            // Add to all download list and map
            mAllInfoMap.put(galleryInfo.gid, info);

            // Add to wait list
            mWaitList.add(info);

            // Save to
            EhDB.addDownloadInfo(info);

            // Notify
            if (mDownloadInfoListener != null) {
                mDownloadInfoListener.onAdd(info, list, list.size() - 1);
            }
            // Make sure download is running
            ensureDownload();
        }
    }

    void startRangeDownload(IntList gidList) {
        boolean update = false;

        for (int i = 0, n = gidList.size(); i < n; i++) {
            int gid = gidList.get(i);
            DownloadInfo info = mAllInfoMap.get(gid);
            if (null == info) {
                Log.d(TAG, "Can't get download info with gid: " + gid);
                continue;
            }

            if (info.state != DownloadInfo.STATE_WAIT) {
                update = true;
                // Set state DownloadInfo.STATE_WAIT
                info.state = DownloadInfo.STATE_WAIT;
                // Add to wait list
                mWaitList.add(info);
                // Update in DB
                EhDB.updateDownloadInfo(info);
            }
        }

        if (update) {
            // Notify Listener
            if (mDownloadInfoListener != null) {
                mDownloadInfoListener.onUpdateAll();
            }
            // Ensure download
            ensureDownload();
        }
    }

    void startAllDownload() {
        boolean update = false;
        // Start all STATE_NONE and STATE_FAILED item
        SparseArray<DownloadInfo> allInfoMap = mAllInfoMap;
        List<DownloadInfo> waitList = mWaitList;
        for (int i = 0, n = allInfoMap.size(); i < n; i++) {
            DownloadInfo info = allInfoMap.valueAt(i);
            if (info.state == DownloadInfo.STATE_NONE || info.state == DownloadInfo.STATE_FAILED) {
                update = true;
                // Set state DownloadInfo.STATE_WAIT
                info.state = DownloadInfo.STATE_WAIT;
                // Add to wait list
                waitList.add(info);
                // Update in DB
                EhDB.updateDownloadInfo(info);
            }
        }

        if (update) {
            // Notify Listener
            if (mDownloadInfoListener != null) {
                mDownloadInfoListener.onUpdateAll();
            }
            // Ensure download
            ensureDownload();
        }
    }

    void stopDownload(int gid) {
        DownloadInfo info = stopDownloadInternal(gid);
        if (info != null) {
            // Update listener
            List<DownloadInfo> list = getInfoListForLabel(info.label);
            if (list != null && mDownloadInfoListener != null) {
                mDownloadInfoListener.onUpdate(info, list);
            }
            // Ensure download
            ensureDownload();
        }
    }

    void stopCurrentDownload() {
        DownloadInfo info = stopCurrentDownloadInternal();
        if (info != null) {
            // Update listener
            List<DownloadInfo> list = getInfoListForLabel(info.label);
            if (list != null && mDownloadInfoListener != null) {
                mDownloadInfoListener.onUpdate(info, list);
            }
            // Ensure download
            ensureDownload();
        }
    }

    void stopRangeDownload(IntList gidList) {
        stopRangeDownloadInternal(gidList);

        // Update listener
        if (mDownloadInfoListener != null) {
            mDownloadInfoListener.onUpdateAll();
        }

        // Ensure download
        ensureDownload();
    }

    void stopAllDownload() {
        // Stop all in wait list
        for (DownloadInfo info : mWaitList) {
            info.state = DownloadInfo.STATE_NONE;
            // Update in DB
            EhDB.updateDownloadInfo(info);
        }
        mWaitList.clear();

        // Stop current
        stopCurrentDownloadInternal();

        // Notify mDownloadInfoListener
        if (mDownloadInfoListener != null) {
            mDownloadInfoListener.onUpdateAll();
        }
    }

    void deleteDownload(int gid) {
        stopDownloadInternal(gid);
        DownloadInfo info = mAllInfoMap.get(gid);
        if (info != null) {
            // Remove from DB
            EhDB.removeDownloadInfo(info.galleryInfo.gid);

            // Remove all list and map
            mAllInfoMap.remove(info.galleryInfo.gid);

            // Remove label list
            LinkedList<DownloadInfo> list = getInfoListForLabel(info.label);
            if (list != null) {
                int index = list.indexOf(info);
                if (index >= 0) {
                    list.remove(info);
                    // Update listener
                    if (mDownloadInfoListener != null) {
                        mDownloadInfoListener.onRemove(info, list, index);
                    }
                }
            }

            // Ensure download
            ensureDownload();
        }
    }

    void deleteRangeDownload(IntList gidList) {
        stopRangeDownloadInternal(gidList);

        for (int i = 0, n = gidList.size(); i < n; i++) {
            int gid = gidList.get(i);
            DownloadInfo info = mAllInfoMap.get(gid);
            if (null == info) {
                Log.d(TAG, "Can't get download info with gid: " + gid);
                continue;
            }

            // Remove from DB
            EhDB.removeDownloadInfo(info.galleryInfo.gid);

            // Remove from all info map
            mAllInfoMap.remove(info.galleryInfo.gid);

            // Remove from label list
            LinkedList<DownloadInfo> list = getInfoListForLabel(info.label);
            if (list != null) {
                list.remove(info);
            }
        }

        // Update listener
        if (mDownloadInfoListener != null) {
            mDownloadInfoListener.onReload();
        }
    }

    // Update in DB
    // Update listener
    // No ensureDownload
    private DownloadInfo stopDownloadInternal(int gid) {
        // Check current task
        if (mCurrentTask != null && mCurrentTask.galleryInfo.gid == gid) {
            // Stop current
            return stopCurrentDownloadInternal();
        }

        for (Iterator<DownloadInfo> iterator = mWaitList.iterator(); iterator.hasNext();) {
            DownloadInfo info = iterator.next();
            if (info.galleryInfo.gid == gid) {
                // Remove from wait list
                iterator.remove();
                // Update state
                info.state = DownloadInfo.STATE_NONE;
                // Update in DB
                EhDB.updateDownloadInfo(info);
                return info;
            }
        }
        return null;
    }

    // Update in DB
    // Update mDownloadListener
    private DownloadInfo stopCurrentDownloadInternal() {
        DownloadInfo info = mCurrentTask;
        SpiderQueen spider = mCurrentSpider;
        // Release spider
        if (spider != null) {
            spider.removeOnSpiderListener(DownloadManager.this);
            SpiderQueen.releaseSpiderQueen(spider, SpiderQueen.MODE_DOWNLOAD);
        }
        mCurrentTask = null;
        mCurrentSpider = null;
        // Stop speed reminder
        mSpeedReminder.stop();
        if (info == null) {
            return null;
        }

        // Update state
        info.state = DownloadInfo.STATE_NONE;
        // Update in DB
        EhDB.updateDownloadInfo(info);
        // Listener
        if (mDownloadListener != null) {
            mDownloadListener.onCancel(info);
        }
        return info;
    }

    // Update in DB
    // Update mDownloadListener
    private void stopRangeDownloadInternal(IntList gidList) {
        // Two way
        if (gidList.size() < mWaitList.size()) {
            for (int i = 0, n = gidList.size(); i < n; i++) {
                stopDownloadInternal(gidList.get(i));
            }
        } else {
            // Check current task
            if (mCurrentTask != null && gidList.contains(mCurrentTask.galleryInfo.gid)) {
                // Stop current
                stopCurrentDownloadInternal();
            }

            // Check all in wait list
            for (Iterator<DownloadInfo> iterator = mWaitList.iterator(); iterator.hasNext();) {
                DownloadInfo info = iterator.next();
                if (gidList.contains(info.galleryInfo.gid)) {
                    // Remove from wait list
                    iterator.remove();
                    // Update state
                    info.state = DownloadInfo.STATE_NONE;
                    // Update in DB
                    EhDB.updateDownloadInfo(info);
                }
            }
        }
    }

    /**
     * @param label Not allow new label
     */
    public void changeLabel(List<DownloadInfo> list, String label) {
        if (null != label && !containLabel(label)) {
            Log.e(TAG, "Not exits label: " + label);
            return;
        }

        List<DownloadInfo> dstList = getInfoListForLabel(label);
        if (dstList == null) {
            Log.e(TAG, "Can't find label with label: " + label);
            return;
        }

        boolean changed = false;

        for (DownloadInfo info: list) {
            if (ObjectUtils.equal(info.label, label)) {
                continue;
            }

            List<DownloadInfo> srcList = getInfoListForLabel(info.label);
            if (srcList == null) {
                Log.e(TAG, "Can't find label with label: " + info.label);
                continue;
            }

            srcList.remove(info);
            dstList.add(info);
            info.label = label;
            // TODO Other comparator
            Collections.sort(dstList, sDateAscComparator);

            // Save to DB
            EhDB.updateDownloadInfo(info);

            changed = true;
        }

        if (changed && mDownloadInfoListener != null) {
            mDownloadInfoListener.onReload();
        }
    }

    boolean isIdle() {
        return mCurrentTask == null && mWaitList.isEmpty();
    }

    @Override
    public void onGetPages(int pages) {
        NotifyTask task = mNotifyTaskPool.pop();
        if (task == null) {
            task = new NotifyTask();
        }
        task.setOnGetPagesData(pages);
        SimpleHandler.getInstance().post(task);
    }

    @Override
    public void onGet509(int index) {
        NotifyTask task = mNotifyTaskPool.pop();
        if (task == null) {
            task = new NotifyTask();
        }
        task.setOnGet509Data(index);
        SimpleHandler.getInstance().post(task);
    }

    @Override
    public void onPageDownload(int index, long contentLength, long receivedSize, int bytesRead) {
        NotifyTask task = mNotifyTaskPool.pop();
        if (task == null) {
            task = new NotifyTask();
        }
        task.setOnPageDownloadData(index, contentLength, receivedSize, bytesRead);
        SimpleHandler.getInstance().post(task);
    }

    @Override
    public void onPageSuccess(int index, int downloaded, int total) {
        NotifyTask task = mNotifyTaskPool.pop();
        if (task == null) {
            task = new NotifyTask();
        }
        task.setOnPageSuccessData(index, downloaded, total);
        SimpleHandler.getInstance().post(task);
    }

    @Override
    public void onPageFailure(int index, String error, int downloaded, int total) {
        NotifyTask task = mNotifyTaskPool.pop();
        if (task == null) {
            task = new NotifyTask();
        }
        task.setOnPageFailureDate(index, error, downloaded, total);
        SimpleHandler.getInstance().post(task);
    }

    @Override
    public void onFinish(int downloaded, int total) {
        NotifyTask task = mNotifyTaskPool.pop();
        if (task == null) {
            task = new NotifyTask();
        }
        task.setOnFinishDate(downloaded, total);
        SimpleHandler.getInstance().post(task);
    }

    @Override
    public void onGetImageSuccess(int index, Image image) {
        // Ignore
    }

    @Override
    public void onGetImageFailure(int index, String error) {
        // Ignore
    }

    private class NotifyTask implements Runnable {

        public static final int TYPE_ON_GET_PAGES = 0;
        public static final int TYPE_ON_GET_509 = 1;
        public static final int TYPE_ON_PAGE_DOWNLOAD = 2;
        public static final int TYPE_ON_PAGE_SUCCESS = 3;
        public static final int TYPE_ON_PAGE_FAILURE = 4;
        public static final int TYPE_ON_FINISH = 5;

        private int mType;
        private int mPages;
        private int mIndex;
        private long mContentLength;
        private long mReceivedSize;
        private int mBytesRead;
        private String mError;
        private int mDownloaded;
        private int mTotal;

        public void setOnGetPagesData(int pages) {
            mType = TYPE_ON_GET_PAGES;
            mPages = pages;
        }

        public void setOnGet509Data(int index) {
            mType = TYPE_ON_GET_509;
            mIndex = index;
        }

        public void setOnPageDownloadData(int index, long contentLength, long receivedSize, int bytesRead) {
            mType = TYPE_ON_PAGE_DOWNLOAD;
            mIndex = index;
            mContentLength = contentLength;
            mReceivedSize = receivedSize;
            mBytesRead = bytesRead;
        }

        public void setOnPageSuccessData(int index, int downloaded, int total) {
            mType = TYPE_ON_PAGE_SUCCESS;
            mIndex = index;
            mDownloaded = downloaded;
            mTotal = total;
        }

        public void setOnPageFailureDate(int index, String error, int downloaded, int total) {
            mType = TYPE_ON_PAGE_FAILURE;
            mIndex = index;
            mError = error;
            mDownloaded = downloaded;
            mTotal = total;
        }

        public void setOnFinishDate(int downloaded, int total) {
            mType = TYPE_ON_FINISH;
            mDownloaded = downloaded;
            mTotal = total;
        }

        @Override
        public void run() {
            switch (mType) {
                case TYPE_ON_GET_PAGES: {
                    DownloadInfo info = mCurrentTask;
                    if (info == null) {
                        Log.e(TAG, "Current task is null, but it should not be");
                    } else {
                        info.total = mPages;
                        List<DownloadInfo> list = getInfoListForLabel(info.label);
                        if (list != null && mDownloadInfoListener != null) {
                            mDownloadInfoListener.onUpdate(info, list);
                        }
                    }
                    break;
                }
                case TYPE_ON_GET_509: {
                    if (mDownloadListener != null) {
                        mDownloadListener.onGet509();
                    }
                    break;
                }
                case TYPE_ON_PAGE_DOWNLOAD: {
                    mSpeedReminder.up(mBytesRead);
                    break;
                }
                case TYPE_ON_PAGE_SUCCESS: {
                    DownloadInfo info = mCurrentTask;
                    if (info == null) {
                        Log.e(TAG, "Current task is null, but it should not be");
                    } else {
                        info.download = mDownloaded;
                        info.total = mTotal;
                        if (mDownloadListener != null) {
                            mDownloadListener.onGetPage(info);
                        }
                        List<DownloadInfo> list = getInfoListForLabel(info.label);
                        if (list != null && mDownloadInfoListener != null) {
                            mDownloadInfoListener.onUpdate(info, list);
                        }
                    }
                    break;
                }
                case TYPE_ON_PAGE_FAILURE: {
                    DownloadInfo info = mCurrentTask;
                    if (info == null) {
                        Log.e(TAG, "Current task is null, but it should not be");
                    } else {
                        info.download = mDownloaded;
                        info.total = mTotal;
                        List<DownloadInfo> list = getInfoListForLabel(info.label);
                        if (list != null && mDownloadInfoListener != null) {
                            mDownloadInfoListener.onUpdate(info, list);
                        }
                    }
                    break;
                }
                case TYPE_ON_FINISH: {
                    // Download done
                    DownloadInfo info = mCurrentTask;
                    mCurrentTask = null;
                    SpiderQueen spider = mCurrentSpider;
                    mCurrentSpider = null;
                    // Release spider
                    if (spider != null) {
                        spider.removeOnSpiderListener(DownloadManager.this);
                        SpiderQueen.releaseSpiderQueen(spider, SpiderQueen.MODE_DOWNLOAD);
                    }
                    // Check null
                    if (info == null || spider == null) {
                        Log.e(TAG, "Current stuff is null, but it should not be");
                        break;
                    }
                    // Stop speed count
                    mSpeedReminder.stop();
                    // Update state
                    info.download = mDownloaded;
                    info.total = mTotal;
                    info.legacy = mTotal - mDownloaded;
                    if (info.legacy == 0) {
                        info.state = DownloadInfo.STATE_FINISH;
                    } else {
                        info.state = DownloadInfo.STATE_FAILED;
                    }
                    // Update in DB
                    EhDB.updateDownloadInfo(info);
                    // Notify
                    if (mDownloadListener != null) {
                        mDownloadListener.onFinish(info);
                    }
                    List<DownloadInfo> list = getInfoListForLabel(info.label);
                    if (list != null && mDownloadInfoListener != null) {
                        mDownloadInfoListener.onUpdate(info, list);
                    }
                    // Start next download
                    ensureDownload();
                    break;
                }
            }

            mNotifyTaskPool.push(this);
        }
    }


    class SpeedReminder implements Runnable {

        private boolean mStop = true;

        private final AtomicLong mBytesRead = new AtomicLong();

        public void start() {
            if (mStop) {
                mStop = false;
                SimpleHandler.getInstance().post(this);
            }
        }

        public void stop() {
            if (!mStop) {
                mStop = true;
                mBytesRead.lazySet(0L);
                SimpleHandler.getInstance().removeCallbacks(this);
            }
        }

        public void up(int bytesRead) {
            mBytesRead.addAndGet(bytesRead);
        }

        @Override
        public void run() {
            DownloadInfo info = mCurrentTask;
            if (info != null) {
                info.speed = mBytesRead.getAndSet(0) / 2;
                if (mDownloadListener != null) {
                    mDownloadListener.onDownload(info);
                }
                List<DownloadInfo> list = getInfoListForLabel(info.label);
                if (list != null && mDownloadInfoListener != null) {
                    mDownloadInfoListener.onUpdate(info, list);
                }
            }

            if (!mStop) {
                SimpleHandler.getInstance().postDelayed(this, 2000);
            }
        }
    }

    private static Comparator<DownloadInfo> sDateAscComparator = new Comparator<DownloadInfo>() {
        @Override
        public int compare(DownloadInfo lhs, DownloadInfo rhs) {
            return lhs.date - rhs.date > 0 ? 1 : -1;
        }
    };

    public interface DownloadInfoListener {

        void onAdd(DownloadInfo info, List<DownloadInfo> list, int position);

        void onUpdate(DownloadInfo info, List<DownloadInfo> list);

        void onUpdateAll();

        void onReload();

        void onRemove(DownloadInfo info, List<DownloadInfo> list, int position);

        void onUpdateLabels();
    }

    public interface DownloadListener {

        /**
         * Get 509 error
         */
        void onGet509();

        /**
         * Start download
         */
        void onStart(DownloadInfo info);

        /**
         * Update download speed
         */
        void onDownload(DownloadInfo info);

        /**
         * Update page downloaded
         */
        void onGetPage(DownloadInfo info);

        /**
         * Download done
         */
        void onFinish(DownloadInfo info);

        /**
         * Download done
         */
        void onCancel(DownloadInfo info);
    }
}
