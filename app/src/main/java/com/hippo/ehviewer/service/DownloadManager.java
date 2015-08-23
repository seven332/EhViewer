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

package com.hippo.ehviewer.service;

import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.hippo.ehviewer.Constants;
import com.hippo.ehviewer.client.data.DownloadInfo;
import com.hippo.ehviewer.client.data.DownloadLabel;
import com.hippo.ehviewer.client.data.GalleryBase;
import com.hippo.ehviewer.gallery.GallerySpider;
import com.hippo.ehviewer.gallery.ImageHandler;
import com.hippo.ehviewer.gallery.SpiderQueen;
import com.hippo.ehviewer.util.DBUtils;
import com.hippo.ehviewer.util.EhUtils;
import com.hippo.yorozuya.AssertUtils;
import com.hippo.yorozuya.Messenger;
import com.hippo.yorozuya.SimpleHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

// TODO use SortedList
public class DownloadManager {

    private static final String TAG = DownloadManager.class.getSimpleName();

    public static final int OPS_SHIFT = 30;
    public static final int OPS_MASK  = 0x3 << OPS_SHIFT;
    public static final int OPS_ADD = 0 << OPS_SHIFT;
    public static final int OPS_REMOVE = 1 << OPS_SHIFT;
    public static final int OPS_UPDATE = 2 << OPS_SHIFT;
    //
    public static final int OPS_ALL_CHANGE = 3 << OPS_SHIFT;

    public static int getOps(int mix) {
        return (mix & OPS_MASK);
    }

    public static int getGid(int mix) {
        return (mix & ~OPS_MASK);
    }

    public static int makeMix(int gid, int ops) {
        return (gid & ~OPS_MASK) | (ops & OPS_MASK);
    }

    public static Comparator<DownloadInfo> TIME_ASC_COMPARATOR = new Comparator<DownloadInfo>() {
        @Override
        public int compare(DownloadInfo lhs, DownloadInfo rhs) {
            return lhs.time > rhs.time ? 1 : lhs.time < rhs.time ? -1 : 0;
        }
    };

    private List<DownloadInfo> mDownloadInfos;
    private Map<String, List<DownloadInfo>> mDownloadInfoMap;
    private List<DownloadInfo> mDefaultDownloadInfos;

    private List<DownloadInfo> mWaitList;

    private @Nullable DownloadInfo mCurrentTask;
    private GallerySpider mCurrentSpider;

    private SpeedReminder mSpeedReminder;
    private DownloadSpiderListener mDownloadSpiderListener;
    private @Nullable DownloadUpdateListener mDownloadUpdateListener;
    private DownloadUpdateInfo mUpdateInfo;

    private static DownloadManager sInstance;

    public static void initialize() {
        sInstance = new DownloadManager();
    }

    private Messenger.Receiver mReceiver = new Messenger.Receiver() {
        @Override
        public void onReceive(int id, Object obj) {
            AssertUtils.assertInstanceof("Messenger obj must be DownloadLabelOps", obj, DownloadLabelModify.class);
            DownloadLabelModify modify = (DownloadLabelModify) obj;

            List<DownloadInfo> list;
            switch (modify.ops) {
                case DownloadLabelModify.OPS_ADD:
                    // Add new label download info list
                    mDownloadInfoMap.put(modify.value, new ArrayList<DownloadInfo>());
                    // Save label to DB
                    DBUtils.addDownloadLabel(modify.value);
                    break;
                case DownloadLabelModify.OPS_REMOVE:
                    // Add target label download info to default list
                    list = mDownloadInfoMap.remove(modify.label.label);
                    if (list != null) {
                        // Remove label in download info
                        for (DownloadInfo info : list) {
                            info.label = null;
                            // Update in DB
                            DBUtils.updateDownloadInfo(info);
                        }
                        mDefaultDownloadInfos.addAll(list);
                        Collections.sort(mDefaultDownloadInfos, TIME_ASC_COMPARATOR);
                    }
                    // Remove label from DB
                    DBUtils.removeDownloadLabel(modify.label.id);
                    break;
                case DownloadLabelModify.OPS_MOVE:
                    // Only need to move in DB
                    DBUtils.moveDownloadLabel(modify.label.id, modify.label2.id);
                    break;
                case DownloadLabelModify.OPS_CHANGE:
                    list = mDownloadInfoMap.remove(modify.label.label);
                    if (list != null) {
                        for (DownloadInfo info : list) {
                            info.label = modify.value;
                            // Update in DB
                            DBUtils.updateDownloadInfo(info);
                        }
                        // Use new key
                        mDownloadInfoMap.put(modify.value, list);
                    }
                    // Update in DB
                    DownloadLabel label = modify.label.clone();
                    label.label = modify.value;
                    DBUtils.updateDownloadLabel(label);
                    break;
                default:
                    throw new IllegalStateException("Unknown download label ops");
            }
        }
    };

    public static DownloadManager getInstance() {
        return sInstance;
    }

    public DownloadManager() {
        List<String> labels = DBUtils.getAllDownloadLabel();
        List<DownloadInfo> downloadInfos = DBUtils.getAllDownloadInfo();
        mDownloadInfos = downloadInfos;
        mDownloadInfoMap = new HashMap<>();

        // Create list for each label
        for (String label : labels) {
            mDownloadInfoMap.put(label, new ArrayList<DownloadInfo>());
        }
        // Create default for non tag
        mDefaultDownloadInfos = new ArrayList<>();

        // Fill each list with DownloadInfo
        for (DownloadInfo info : downloadInfos) {
            // Fix download state
            if (info.state == DownloadInfo.STATE_DOWNLOAD || info.state == DownloadInfo.STATE_WAIT) {
                info.state = DownloadInfo.STATE_NONE;
            }

            List<DownloadInfo> list;
            if (TextUtils.isEmpty(info.label)) {
                list = mDefaultDownloadInfos;
            } else {
                list = mDownloadInfoMap.get(info.label);
            }

            if (list == null) {
                list = mDefaultDownloadInfos;
            }

            list.add(info);
        }

        mWaitList = new LinkedList<>();
        mSpeedReminder = new SpeedReminder();
        mDownloadSpiderListener = new DownloadSpiderListener();
        mUpdateInfo = new DownloadUpdateInfo();

        Messenger.getInstance().register(Constants.MESSENGER_ID_MODIFY_DOWNLOAD_LABEL_FROM_SCENE, mReceiver);
    }

    public void setDownloadUpdateListener(@Nullable DownloadUpdateListener listener) {
        mDownloadUpdateListener = listener;
    }

    public List<DownloadInfo> getAllDownloadList() {
        return mDownloadInfos;
    }

    public List<DownloadInfo> getDownloadList(String label) {
        return getDownloadListInternal(label, false);
    }

    private List<DownloadInfo> getDownloadListInternal(String label, boolean create) {
        if (TextUtils.isEmpty(label)) {
            return mDefaultDownloadInfos;
        } else {
            List<DownloadInfo> list = mDownloadInfoMap.get(label);
            if (list == null && create) {
                list = new ArrayList<>();
                mDownloadInfoMap.put(label, list);
                // Save label to DB
                DBUtils.addDownloadLabel(label);
                // notify
                DownloadManager.DownloadLabelModify modify = new DownloadManager.DownloadLabelModify();
                modify.ops = DownloadManager.DownloadLabelModify.OPS_ADD;
                modify.value = label;
                Messenger.getInstance().notify(Constants.MESSENGER_ID_MODIFY_DOWNLOAD_LABEL_FROM_MANAGER, modify);
            }
            return list;
        }
    }

    public boolean isInDownloadList(int gid) {
        for (DownloadInfo info : mDownloadInfos) {
            if (info.galleryBase.gid == gid) {
                return true;
            }
        }
        return false;
    }

    // Make sure is downloading
    private void ensureDownload() {
        if (mCurrentTask != null) {
            // Only one download
            return;
        }

        // get download from wait list
        if (!mWaitList.isEmpty()) {
            DownloadInfo info = mWaitList.remove(0);
            GallerySpider spider = null;
            try {
                spider = GallerySpider.obtain(info.galleryBase, ImageHandler.Mode.DOWNLOAD);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (spider != null) {
                int gid = info.galleryBase.gid;
                mCurrentTask =  info;
                mCurrentSpider = spider;
                info.state = DownloadInfo.STATE_DOWNLOAD;
                info.speed = -1;
                // Fill Download update info
                DownloadUpdateInfo updateInfo = mUpdateInfo;
                updateInfo.gid = gid;
                updateInfo.title = EhUtils.getSuitableTitle(info.galleryBase);
                updateInfo.pages = spider.size();
                updateInfo.downloadedPages = 0;
                updateInfo.speed = -1;
                updateInfo.legacy = Integer.MAX_VALUE;
                // Notify
                Messenger.getInstance().notify(Constants.MESSENGER_ID_UPDATE_DOWNLOAD,
                        makeMix(gid, OPS_UPDATE));
                // Listener
                if (mDownloadUpdateListener != null) {
                    mDownloadUpdateListener.onStart(updateInfo);
                }
                // Update DownloadInfo to DB
                DBUtils.updateDownloadInfo(info);

                info.total = spider.size();
                mDownloadSpiderListener.start();
                spider.addSpiderSpiderListener(mDownloadSpiderListener);
                mSpeedReminder.start();

            } else {
                // Get error when getting spider
                info.state = DownloadInfo.STATE_FINISH;
                info.legacy = Integer.MAX_VALUE;
                // Fill Download update info
                DownloadUpdateInfo updateInfo = mUpdateInfo;
                updateInfo.gid = info.galleryBase.gid;
                updateInfo.title = EhUtils.getSuitableTitle(info.galleryBase);
                updateInfo.pages = -1;
                updateInfo.downloadedPages = 0;
                updateInfo.speed = -1;
                updateInfo.legacy = Integer.MAX_VALUE;
                // Notify
                Messenger.getInstance().notify(Constants.MESSENGER_ID_UPDATE_DOWNLOAD,
                        makeMix(info.galleryBase.gid, OPS_UPDATE));
                // Listener
                if (mDownloadUpdateListener != null) {
                    mDownloadUpdateListener.onFinish(updateInfo);
                }
                // Update DownloadInfo to DB
                DBUtils.updateDownloadInfo(info);

                // Continue
                ensureDownload();
            }
        }

        // No gallery to download
    }

    /**
     * @param label null for default
     */
    void startDownload(GalleryBase galleryBase, @Nullable String label) {
        try {
            if (mCurrentTask != null && mCurrentTask.galleryBase.gid == galleryBase.gid) {
                // It is current task
                return;
            }

            // Check in download list
            for (DownloadInfo info: mDownloadInfos) {
                if (info.galleryBase.gid == galleryBase.gid) {
                    // It is in download list
                    if (info.state == DownloadInfo.STATE_FINISH || info.state == DownloadInfo.STATE_NONE) {
                        info.state = DownloadInfo.STATE_WAIT;
                        // Add to wait list
                        mWaitList.add(info);
                        // Notify
                        Messenger.getInstance().notify(Constants.MESSENGER_ID_UPDATE_DOWNLOAD,
                                makeMix(galleryBase.gid, OPS_UPDATE));
                        // Update DownloadInfo to DB
                        DBUtils.updateDownloadInfo(info);
                    } else {
                        // No need to update
                    }
                    return;
                }
            }

            // It is new download info
            DownloadInfo downloadInfo = new DownloadInfo();
            downloadInfo.galleryBase = galleryBase;
            downloadInfo.label = label;
            downloadInfo.state = DownloadInfo.STATE_WAIT;
            downloadInfo.time = System.currentTimeMillis();

            List<DownloadInfo> list = getDownloadListInternal(label, true);
            list.add(downloadInfo);
            mDownloadInfos.add(downloadInfo);

            // Add to wait list
            mWaitList.add(downloadInfo);
            // Notify
            Messenger.getInstance().notify(Constants.MESSENGER_ID_UPDATE_DOWNLOAD,
                    makeMix(galleryBase.gid, OPS_ADD));
            // Save DownloadInfo to DB
            DBUtils.addDownloadInfo(downloadInfo);
        } finally {
            ensureDownload();
        }
    }

    void startAllDownload() {
        for (DownloadInfo info : mDownloadInfos) {
            if (info.state == DownloadInfo.STATE_NONE ||
                    (info.state == DownloadInfo.STATE_FINISH && info.legacy != 0)) {
                info.state = DownloadInfo.STATE_WAIT;
                // Add to wait list
                mWaitList.add(info);
                // Notify
                Messenger.getInstance().notify(Constants.MESSENGER_ID_UPDATE_DOWNLOAD,
                        makeMix(info.galleryBase.gid, OPS_UPDATE));
                // Update DownloadInfo to DB
                DBUtils.updateDownloadInfo(info);
            }
        }
        ensureDownload();
    }

    public void stopDownload(int gid) {
        // Check current task
        if (mCurrentTask != null && mCurrentTask.galleryBase.gid == gid) {
            // Stop current
            stopCurrentDownloadInternal();

            // Continue
            ensureDownload();
            return;
        }

        // Check in wait list
        if (!mWaitList.isEmpty()) {
            for (Iterator<DownloadInfo> iter = mWaitList.iterator(); iter.hasNext();) {
                DownloadInfo info = iter.next();
                if (info.galleryBase.gid == gid) {
                    // Remove from wait list
                    iter.remove();

                    info.state = DownloadInfo.STATE_NONE;

                    // Notify
                    Messenger.getInstance().notify(Constants.MESSENGER_ID_UPDATE_DOWNLOAD,
                            makeMix(gid, OPS_UPDATE));
                    // Update in DB
                    DBUtils.updateDownloadInfo(info);
                    return;
                }
            }
        }
    }

    public void stopCurrentDownload() {
        stopCurrentDownloadInternal();
        ensureDownload();
    }

    public void stopAllDownload() {
        // Stop all in wait list
        for (DownloadInfo info : mWaitList) {
            info.state = DownloadInfo.STATE_NONE;
            // Notify
            Messenger.getInstance().notify(Constants.MESSENGER_ID_UPDATE_DOWNLOAD,
                    makeMix(info.galleryBase.gid, OPS_UPDATE));
            // Update in DB
            DBUtils.updateDownloadInfo(info);
        }
        mWaitList.clear();

        // Stop current
        stopCurrentDownloadInternal();
    }

    private void stopCurrentDownloadInternal() {
        if (mCurrentTask == null) {
            return;
        }

        mCurrentTask.state = DownloadInfo.STATE_NONE;

        int gid = mCurrentTask.galleryBase.gid;
        // Notify
        Messenger.getInstance().notify(Constants.MESSENGER_ID_UPDATE_DOWNLOAD,
                makeMix(gid, OPS_UPDATE));
        // Listener
        if (mDownloadUpdateListener != null) {
            mDownloadUpdateListener.onCancel(mUpdateInfo);
        }
        // Update in DB
        DBUtils.updateDownloadInfo(mCurrentTask);

        // Release
        releaseCurrent();
    }

    public static boolean isDefaultLabel(String label) {
        return TextUtils.isEmpty(label);
    }

    public static boolean labelEquals(String label1, String label2) {
        if (TextUtils.isEmpty(label1)) {
            return TextUtils.isEmpty(label2);
        } else {
            return label1.equals(label2);
        }
    }

    /**
     * Change label
     */
    public void moveDownloadInfo(List<DownloadInfo> list, String label) {
        // Get the download list to add to
        List<DownloadInfo> targetList;
        if (isDefaultLabel(label)) {
            targetList = mDefaultDownloadInfos;
        } else {
            targetList = getDownloadListInternal(label, true);
        }

        String lastLabel = null;
        List<DownloadInfo> lastList = null;
        Set<List<DownloadInfo>> modifiedInfoList = new HashSet<>(1);

        for (DownloadInfo info : list) {
            List<DownloadInfo> infoList = null;
            if (labelEquals(info.label, lastLabel)) {
                infoList = lastList;
            }
            if (infoList == null) {
                infoList = getDownloadListInternal(info.label, false);
            }
            if (infoList == null) {
                Log.e("TAG", "Can't get download info list, label is " + info.label);
                continue;
            }

            info.label = label;
            DBUtils.updateDownloadInfo(info);
            infoList.remove(info);
            targetList.add(info);

            lastLabel = info.label;
            lastList = infoList;
            modifiedInfoList.add(lastList);
        }

        // Sort modified info list
        for (List<DownloadInfo> infoList : modifiedInfoList) {
            Collections.sort(infoList, TIME_ASC_COMPARATOR);
        }
    }

    public boolean hasWaitingDownloadInfo() {
        return !mWaitList.isEmpty();
    }

    public boolean isDownloading() {
        return mCurrentTask != null || hasWaitingDownloadInfo();
    }

    private void releaseCurrent() {
        if (mCurrentTask != null) {
            mCurrentTask.speed = -1;
            mCurrentTask.download = 0;
            mCurrentTask.total = -1;
            mCurrentTask = null;
            mCurrentSpider.removeSpiderSpiderListener(mDownloadSpiderListener);
            GallerySpider.release(mCurrentSpider);
            mCurrentSpider = null;
            mDownloadSpiderListener.reset();
            mSpeedReminder.stop();
        }
    }

    private class DownloadSpiderListener implements SpiderQueen.SpiderListener {

        private long mReceived;
        private long mTime;

        public void start() {
            // reset
            mReceived = 0;
            mTime = SystemClock.uptimeMillis();
        }

        public void reset() {
            // reset
            mReceived = 0;
        }

        // -1 for can't get speed
        public long getSpeed() {
            if (mCurrentTask == null || mCurrentTask.total == -1) {
                return -1;
            } else {
                long time = SystemClock.uptimeMillis();
                long speed = mReceived * 1000 / (time - mTime);
                mTime = time;
                mReceived = 0;
                return speed;
            }
        }

        @Override
        public void onTotallyFailed(Exception e) {
            onDone(Integer.MAX_VALUE); // TODO
        }

        @Override
        public void onPartlyFailed(Exception e) {
            onDone(Integer.MAX_VALUE); // TODO
        }

        @Override
        public void onDone(int legacy) {
            if (mCurrentTask == null) {
                return;
            }

            mCurrentTask.state = DownloadInfo.STATE_FINISH;
            mCurrentTask.legacy = legacy;

            int gid = mCurrentTask.galleryBase.gid;
            // Fill download update info
            mUpdateInfo.legacy = legacy;
            // Notify
            Messenger.getInstance().notify(Constants.MESSENGER_ID_UPDATE_DOWNLOAD,
                    makeMix(gid, OPS_UPDATE));
            // Listener
            if (mDownloadUpdateListener != null) {
                mDownloadUpdateListener.onFinish(mUpdateInfo);
            }
            // Update to DB
            DBUtils.updateDownloadInfo(mCurrentTask);

            // Release
            releaseCurrent();

            // Continue
            ensureDownload();
        }

        @Override
        public void onGetPages(int pages) {
            if (mCurrentTask == null) {
                return;
            }

            mCurrentTask.total = pages;

            int gid = mCurrentTask.galleryBase.gid;
            // Fill download update info
            mUpdateInfo.pages = pages;
            // Notify
            Messenger.getInstance().notify(Constants.MESSENGER_ID_UPDATE_DOWNLOAD,
                    makeMix(gid, OPS_UPDATE));
            // Listener
            if (mDownloadUpdateListener != null) {
                mDownloadUpdateListener.onGetPages(mUpdateInfo);
            }
        }

        @Override
        public void onSpiderStart(int index, long totalSize) {
        }

        @Override
        public void onSpiderPage(int index, long receivedSize, long singleReceivedSize) {
            mReceived += singleReceivedSize;
        }

        @Override
        public void onSpiderSucceed(int index) {
            // TODO force request may onSpiderSucceed more than once
            if (mCurrentTask == null) {
                return;
            }

            mCurrentTask.download++;

            int gid = mCurrentTask.galleryBase.gid;
            // Fill download update info
            mUpdateInfo.downloadedPages++;
            // Notify
            Messenger.getInstance().notify(Constants.MESSENGER_ID_UPDATE_DOWNLOAD,
                    makeMix(gid, OPS_UPDATE));
            // Listener
            if (mDownloadUpdateListener != null) {
                mDownloadUpdateListener.onDownloadPage(mUpdateInfo);
            }
        }

        @Override
        public void onSpiderFailed(int index, Exception e) {
        }

        @Override
        public void onSpiderCancelled(int index) {
        }

        @Override
        public void onGetImage(int index, Object obj) {
        }
    }

    class SpeedReminder implements Runnable {

        private boolean mStop = true;

        public void start() {
            if (mStop) {
                mStop = false;
                SimpleHandler.getInstance().post(this);
            }
        }

        public void stop() {
            if (!mStop) {
                mStop = true;
                SimpleHandler.getInstance().removeCallbacks(this);
            }
        }

        @Override
        public void run() {
            if (mCurrentTask != null && mCurrentTask.total >= 0) {
                long speed = mDownloadSpiderListener.getSpeed();
                // Fill download update info
                mUpdateInfo.speed = speed;
                mCurrentTask.speed = speed;

                if (speed != -1) {
                    // Notify
                    Messenger.getInstance().notify(Constants.MESSENGER_ID_UPDATE_DOWNLOAD,
                            makeMix(mCurrentTask.galleryBase.gid, OPS_UPDATE));
                    // Listener
                    if (mDownloadUpdateListener != null) {
                        mDownloadUpdateListener.onDownload(mUpdateInfo);
                    }
                }
            }

            if (!mStop) {
                SimpleHandler.getInstance().postDelayed(this, 2000);
            }
        }
    }

    public interface DownloadUpdateListener {

        void onStart(DownloadUpdateInfo info);

        void onGetPages(DownloadUpdateInfo info);

        // Update download pages
        void onDownloadPage(DownloadUpdateInfo info);

        // Update speed
        void onDownload(DownloadUpdateInfo info);

        void onFinish(DownloadUpdateInfo info);

        void onCancel(DownloadUpdateInfo info);
    }

    public static class DownloadUpdateInfo {
        public int gid;
        public String title;
        public int pages;
        public int downloadedPages;
        public long speed;
        public int legacy;
    }

    public static class DownloadLabelModify {

        public static final int OPS_ADD = 0;
        public static final int OPS_REMOVE = 1;
        public static final int OPS_MOVE = 2;
        public static final int OPS_CHANGE = 3;

        public int ops;
        // for remove and change
        public DownloadLabel label;
        // for add and change
        public String value;
        // for move
        public DownloadLabel label2;
    }
}
