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

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;

import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.yorozuya.FileUtils;

public class DownloadService extends Service implements DownloadManager.DownloadListener {

    public static final String ACTION_START = "start";
    public static final String ACTION_START_ALL = "start_all";
    public static final String ACTION_STOP = "stop";
    public static final String ACTION_STOP_CURRENT = "stop_current";
    public static final String ACTION_STOP_ALL = "stop_all";
    public static final String ACTION_DELETE = "delete";

    public static final String ACTION_CLEAR = "clear";

    public static final String KEY_GALLERY_INFO = "gallery_info";
    public static final String KEY_LABEL = "label";
    public static final String KEY_GID = "gid";

    private static final int ID_DOWNLOADING = 1;
    private static final int ID_DOWNLOADED = 2;
    private static final int ID_509 = 3;

    @Nullable
    private NotificationManager mNotifyManager;
    @Nullable
    private DownloadManager mDownloadManager;
    private NotificationCompat.Builder mDownloadingBuilder;
    private NotificationCompat.Builder mDownloadedBuilder;
    private NotificationCompat.Builder m509dBuilder;

    @Nullable
    private SparseBooleanArray mItemStateArray;
    @Nullable
    private SparseArray<String> mItemTitleArray;

    private int mFailedCount;
    private int mFinishedCount;
    private int mDownloadedCount;

    private Bitmap mLargeIcon;

    public void init(SparseBooleanArray stateArray, SparseArray<String> titleArray,
            int failedCount, int finishedCount, int downloadedCount) {
        mItemStateArray = stateArray;
        mItemTitleArray = titleArray;
        mFailedCount = failedCount;
        mFinishedCount = finishedCount;
        mDownloadedCount = downloadedCount;
        mLargeIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
    }

    public void clear() {
        mFailedCount = 0;
        mFinishedCount = 0;
        mDownloadedCount = 0;
        if (mItemStateArray != null) {
            mItemStateArray.clear();
        }
        if (mItemTitleArray != null) {
            mItemTitleArray.clear();
        }
        mLargeIcon = null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mDownloadManager = EhApplication.getDownloadManager(getApplicationContext());
        mDownloadManager.setDownloadListener(this);
        EhApplication.initDownloadService(getApplicationContext(), this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mNotifyManager = null;
        if (mDownloadManager != null) {
            mDownloadManager.setDownloadListener(null);
            mDownloadManager = null;
        }
        mDownloadingBuilder = null;
        mDownloadedBuilder = null;
        m509dBuilder = null;
        EhApplication.backupDownloadService(getApplicationContext(),
                mFailedCount, mFinishedCount, mDownloadedCount);
        mItemStateArray = null;
        mItemTitleArray = null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleIntent(intent);
        return START_STICKY;
    }

    private void handleIntent(Intent intent) {
        String action = null;
        if (intent != null) {
            action = intent.getAction();
        }

        if (ACTION_START.equals(action)) {
            GalleryInfo gi= intent.getParcelableExtra(KEY_GALLERY_INFO);
            String label = intent.getStringExtra(KEY_LABEL);
            if (gi != null && mDownloadManager != null) {
                mDownloadManager.startDownload(gi, label);
            }
        } else if (ACTION_STOP.equals(action)) {
            int gid = intent.getIntExtra(KEY_GID, -1);
            if (gid != -1 && mDownloadManager != null) {
                mDownloadManager.stopDownload(gid);
            }
        } else if (ACTION_STOP_CURRENT.equals(action)) {
            if (mDownloadManager != null) {
                mDownloadManager.stopCurrentDownload();
            }
        } else if (ACTION_STOP_ALL.equals(action)) {
            if (mDownloadManager != null) {
                mDownloadManager.stopAllDownload();
            }
        } else if (ACTION_DELETE.equals(action)) {
            int gid = intent.getIntExtra(KEY_GID, -1);
            if (gid != -1 && mDownloadManager != null) {
                mDownloadManager.deleteDownload(gid);
            }
        } else if (ACTION_CLEAR.equals(action)) {
            EhApplication.clearDownloadService(getApplicationContext(), this);
        }

        checkStopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new IllegalStateException("No bindService");
    }

    private void ensureDownloadingBuilder() {
        if (mDownloadingBuilder != null) {
            return;
        }

        mDownloadingBuilder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setOngoing(true)
                .setAutoCancel(false)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setColor(getResources().getColor(R.color.colorPrimary));
    }

    private void ensureDownloadedBuilder() {
        if (mDownloadedBuilder != null) {
            return;
        }

        Intent clearIntent = new Intent(this, DownloadService.class);
        clearIntent.setAction(ACTION_CLEAR);
        PendingIntent piClear = PendingIntent.getService(this, 0, clearIntent, 0);
        mDownloadedBuilder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setLargeIcon(mLargeIcon)
                .setContentTitle(getString(R.string.stat_download_done_title))
                .setDeleteIntent(piClear)
                .setOngoing(false)
                .setAutoCancel(true);
    }

    private void ensure509Builder() {
        if (m509dBuilder != null) {
            return;
        }

        m509dBuilder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.ic_stat_alert)
                .setLargeIcon(mLargeIcon)
                .setContentText(getString(R.string.stat_509_alert_title))
                .setContentText(getString(R.string.stat_509_alert_text))
                .setAutoCancel(true)
                .setOngoing(false)
                .setCategory(NotificationCompat.CATEGORY_ERROR);
    }

    @Override
    public void onGet509() {
        if (mNotifyManager == null) {
            return;
        }

        ensure509Builder();

        mNotifyManager.notify(ID_509, m509dBuilder.build());
    }

    @Override
    public void onStart(DownloadInfo info) {
        if (mNotifyManager == null) {
            return;
        }

        ensureDownloadingBuilder();

        mDownloadingBuilder.setContentTitle(info.galleryInfo.title)
                .setContentText(null)
                .setProgress(0, 0, true);

        startForeground(ID_DOWNLOADING, mDownloadingBuilder.build());
    }

    private void onUpdate(DownloadInfo info) {
        if (mNotifyManager == null) {
            return;
        }
        ensureDownloadingBuilder();

        long speed = info.speed;
        if (speed < 0) {
            speed = 0;
        }
        String text = FileUtils.humanReadableByteCount(speed, false) + "/S";
        mDownloadingBuilder.setContentTitle(info.galleryInfo.title)
                .setContentText(text)
                .setProgress(info.total, info.download, false);

        startForeground(ID_DOWNLOADING, mDownloadingBuilder.build());
    }

    @Override
    public void onDownload(DownloadInfo info) {
        onUpdate(info);
    }

    @Override
    public void onGetPage(DownloadInfo info) {
        onUpdate(info);
    }

    @Override
    public void onFinish(DownloadInfo info) {
        if (mNotifyManager == null || mItemStateArray == null || mItemTitleArray == null) {
            return;
        }

        mNotifyManager.cancel(ID_DOWNLOADING);

        ensureDownloadedBuilder();

        boolean finish = info.state == DownloadInfo.STATE_FINISH;
        int gid = info.galleryInfo.gid;
        int index = mItemStateArray.indexOfKey(gid);
        if (index < 0) { // Not contain
            mItemStateArray.put(gid, finish);
            mItemTitleArray.put(gid, info.galleryInfo.title);
            mDownloadedCount++;
            if (finish) {
                mFinishedCount++;
            } else {
                mFailedCount++;
            }
        } else { // Contain
            boolean oldFinish = mItemStateArray.valueAt(index);
            mItemStateArray.put(gid, finish);
            mItemTitleArray.put(gid, info.galleryInfo.title);
            if (oldFinish && !finish) {
                mFinishedCount--;
                mFailedCount++;
            } else if (!oldFinish && finish) {
                mFinishedCount++;
                mFailedCount--;
            }
        }

        String text;
        boolean needStyle;
        if (mFinishedCount != 0 && mFailedCount == 0) {
            if (mFinishedCount == 1) {
                if (mItemTitleArray.size() >= 1) {
                    text = getString(R.string.stat_download_done_line_succeeded, mItemTitleArray.valueAt(0));
                } else {
                    Log.d("TAG", "WTF, mItemTitleArray is null");
                    text = getString(R.string.error_unknown);
                }
                needStyle = false;
            } else {
                text = getString(R.string.stat_download_done_text_succeeded, mFinishedCount);
                needStyle = true;
            }
        } else if (mFinishedCount == 0 && mFailedCount != 0) {
            if (mFailedCount == 1) {
                if (mItemTitleArray.size() >= 1) {
                    text = getString(R.string.stat_download_done_line_failed, mItemTitleArray.valueAt(0));
                } else {
                    Log.d("TAG", "WTF, mItemTitleArray is null");
                    text = getString(R.string.error_unknown);
                }
                needStyle = false;
            } else {
                text = getString(R.string.stat_download_done_text_failed, mFailedCount);
                needStyle = true;
            }
        } else {
            text = getString(R.string.stat_download_done_text_mix, mFinishedCount, mFailedCount);
            needStyle = true;
        }

        NotificationCompat.InboxStyle style;
        if (needStyle) {
            style = new NotificationCompat.InboxStyle();
            style.setBigContentTitle(getString(R.string.stat_download_done_title));
            SparseBooleanArray stateArray = mItemStateArray;
            SparseArray<String> titleArray = mItemTitleArray;
            for (int i = 0, n = stateArray.size(); i < n; i++) {
                int id = stateArray.keyAt(i);
                boolean fin = stateArray.valueAt(i);
                String title = titleArray.get(id);
                if (title == null) {
                    continue;
                }
                style.addLine(getString(fin ? R.string.stat_download_done_line_succeeded :
                                R.string.stat_download_done_line_failed, title));
            }
        } else {
            style = null;
        }

        mDownloadedBuilder.setContentText(text)
                .setStyle(style)
                .setNumber(mDownloadedCount);

        mNotifyManager.notify(ID_DOWNLOADED, mDownloadedBuilder.build());

        checkStopSelf();
    }

    @Override
    public void onCancel(DownloadInfo info) {
        if (mNotifyManager == null) {
            return;
        }

        mNotifyManager.cancel(ID_DOWNLOADING);
        checkStopSelf();
    }

    private void checkStopSelf() {
        if (mDownloadManager == null || mDownloadManager.isIdle()) {
            stopForeground(true);
            stopSelf();
        }
    }
}
