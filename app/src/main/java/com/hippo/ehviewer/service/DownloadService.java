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

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.data.GalleryBase;
import com.hippo.ehviewer.ui.ContentActivity;
import com.hippo.ehviewer.ui.scene.DownloadScene;
import com.hippo.yorozuya.FileUtils;

// TODO Avoid frequent notification
public class DownloadService extends Service implements DownloadManager.DownloadUpdateListener {

    private static final String TAG = DownloadService.class.getSimpleName();

    public static final String KEY_GALLERY_BASE = "gallery_base";
    public static final String KEY_TAG = "tag";
    public static final String KEY_GID = "gid";

    public static final String KEY_CLEAR = "clear";

    public static final String ACTION_START = "com.hippo.ehviewer.service.DownloadService.START";
    public static final String ACTION_START_ALL = "com.hippo.ehviewer.service.DownloadService.START_ALL";
    public static final String ACTION_STOP = "com.hippo.ehviewer.service.DownloadService.STOP";
    public static final String ACTION_STOP_CURRENT = "com.hippo.ehviewer.service.DownloadService.STOP_CURRENT";
    public static final String ACTION_STOP_ALL = "com.hippo.ehviewer.service.DownloadService.STOP_ALL";
    public static final String ACTION_DELETE = "com.hippo.ehviewer.service.DownloadService.DELETE";
    public static final String ACTION_CLEAR = "com.hippo.ehviewer.service.DownloadService.CLEAR";

    private static final int NOTIFY_ID_DOWNLOADING = -1;
    private static final int NOTIFY_ID_DOWNLOAD = -2;

    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mDownloadingBuilder;
    private NotificationCompat.Builder mDownloadedBuilder;

    private static int sSucceeCount = 0;
    private static int sFailedCount = 0;

    private DownloadManager mDownloadManager;

    public static void clear() {
        sSucceeCount = 0;
        sFailedCount = 0;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mDownloadManager = DownloadManager.getInstance();
        mDownloadManager.setDownloadUpdateListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mDownloadManager.setDownloadUpdateListener(null);
        mDownloadManager = null;
        mNotifyManager = null;
        mDownloadingBuilder = null;
        mDownloadedBuilder = null;
    }

    @Override
    public @Nullable IBinder onBind(Intent intent) {
        throw new IllegalStateException("No bindService");
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

        boolean checkStop = false;

        if (ACTION_START.equals(action)) {
            GalleryBase gb = intent.getParcelableExtra(KEY_GALLERY_BASE);
            String tag = intent.getStringExtra(KEY_TAG);
            if (gb != null) {
                mDownloadManager.startDownload(gb, tag);
            }
        } else if (ACTION_START_ALL.equals(action)) {
            //mDownloadManager.startAllDownload();
        } else if (ACTION_STOP.equals(action)) {
            int gid = intent.getIntExtra(KEY_GID, 0);
            if (gid != 0) {
                mDownloadManager.stopDownload(gid);
            }
            checkStop = true;
        } else if (ACTION_STOP_CURRENT.equals(action)) {
            mDownloadManager.stopCurrentDownload();
            checkStop = true;
        } else if (ACTION_STOP_ALL.equals(action)) {
            mDownloadManager.stopAllDownload();
            checkStop = true;
        } else if (ACTION_DELETE.equals(action)) {
            // TODO
            int gid = intent.getIntExtra(KEY_GID, 0);
            if (gid != 0) {
                mDownloadManager.stopDownload(gid);
            }
            checkStop = true;



        } else if (ACTION_CLEAR.equals(action)) {
            sSucceeCount = 0;
            sFailedCount = 0;
        } else {
            checkStop = true;
        }

        if (checkStop) {
            if (!mDownloadManager.isDownloading()) {
                stopForeground(true);
                stopSelf();
            }
        }
    }

    private void ensureDownloadingNotification() {
        if (mDownloadingBuilder != null) {
            return;
        }

        mDownloadingBuilder = new NotificationCompat.Builder(getApplicationContext());
        mDownloadingBuilder.setSmallIcon(android.R.drawable.stat_sys_download);
        mDownloadingBuilder.setOngoing(true).setAutoCancel(false);

        // Add action
        Intent stopIntent = new Intent(this, DownloadService.class);
        stopIntent.setAction(ACTION_STOP_CURRENT);
        PendingIntent piStop = PendingIntent.getService(this, 0, stopIntent, 0);
        mDownloadingBuilder.addAction(R.drawable.ic_pause_x24, getString(R.string.stop), piStop);

        Intent stopAllIntent = new Intent(this, DownloadService.class);
        stopAllIntent.setAction(ACTION_STOP_ALL);
        PendingIntent piStopAll = PendingIntent.getService(this, 0, stopAllIntent, 0);
        mDownloadingBuilder.addAction(R.drawable.ic_close_x24, getString(R.string.stop_all), piStopAll);

        Intent intent = new Intent(DownloadService.this, ContentActivity.class);
        intent.setAction(ContentActivity.ACTION_START_SCENE);
        intent.putExtra(ContentActivity.KEY_SCENE, DownloadScene.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(DownloadService.this, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mDownloadingBuilder.setContentIntent(pendingIntent);
    }

    private void ensureDownloadedNotification() {
        if (mDownloadedBuilder != null) {
            return;
        }

        Intent clearIntent = new Intent(this, DownloadService.class);
        clearIntent.setAction(ACTION_CLEAR);
        PendingIntent piClear = PendingIntent.getService(this, 0, clearIntent, 0);
        mDownloadedBuilder = new NotificationCompat.Builder(getApplicationContext());
        mDownloadedBuilder.setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setDeleteIntent(piClear).setOngoing(false).setAutoCancel(true);

        Intent intent = new Intent(DownloadService.this, ContentActivity.class);
        intent.setAction(ContentActivity.ACTION_START_SCENE);
        intent.putExtra(ContentActivity.KEY_SCENE, DownloadScene.class);
        intent.putExtra(DownloadService.KEY_CLEAR, true);
        PendingIntent pendingIntent = PendingIntent.getActivity(DownloadService.this, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mDownloadedBuilder.setContentIntent(pendingIntent);
    }

    private void setPendingIntent() {
        // TODO
    }

    @Override
    public void onStart(DownloadManager.DownloadUpdateInfo info) {
        ensureDownloadingNotification();
        setPendingIntent();

        mDownloadingBuilder.setContentTitle(info.title)
                .setContentText(null)
                .setProgress(0, 0, true);
        startForeground(NOTIFY_ID_DOWNLOADING, mDownloadingBuilder.build());
    }

    @Override
    public void onGetPages(DownloadManager.DownloadUpdateInfo info) {
        ensureDownloadingNotification();
        setPendingIntent();

        mDownloadingBuilder.setContentTitle(info.title)
                .setContentText(null)
                .setProgress(info.pages, info.downloadedPages, false);
        startForeground(NOTIFY_ID_DOWNLOADING, mDownloadingBuilder.build());
    }

    @Override
    public void onDownloadPage(DownloadManager.DownloadUpdateInfo info) {
        ensureDownloadingNotification();
        setPendingIntent();

        String text = FileUtils.humanReadableByteCount(info.speed, false) + "/S";

        mDownloadingBuilder.setContentTitle(info.title)
                .setContentText(text)
                .setProgress(info.pages, info.downloadedPages, false);
        startForeground(NOTIFY_ID_DOWNLOADING, mDownloadingBuilder.build());
    }

    @Override
    public void onDownload(DownloadManager.DownloadUpdateInfo info) {
        ensureDownloadingNotification();
        setPendingIntent();

        String text = FileUtils.humanReadableByteCount(info.speed, false) + "/S";

        mDownloadingBuilder.setContentTitle(info.title)
                .setContentText(text)
                .setProgress(info.pages, info.downloadedPages, false);
        startForeground(NOTIFY_ID_DOWNLOADING, mDownloadingBuilder.build());
    }

    @Override
    public void onFinish(DownloadManager.DownloadUpdateInfo info) {
        mNotifyManager.cancel(NOTIFY_ID_DOWNLOADING);

        ensureDownloadedNotification();

        if (info.legacy == 0) {
            sSucceeCount++;
        } else {
            sFailedCount++;
        }

        if (sSucceeCount + sFailedCount == 1) {
            mDownloadedBuilder.setContentTitle(info.title);
            if (sSucceeCount == 1) {
                mDownloadedBuilder.setContentText(getResources().getString(R.string.successfully_downloaded));
            } else {
                mDownloadedBuilder.setContentText(getResources().getString(R.string.incompletely_downloaded));
            }
        } else {
            mDownloadedBuilder.setContentTitle(getResources().getString(R.string.downloading_complete));
            StringBuilder sb = new StringBuilder();
            if (sSucceeCount != 0) {
                sb.append(getResources().getQuantityString(R.plurals.item_successfully_downloaded, sSucceeCount, sSucceeCount));
            }
            if (sFailedCount != 0) {
                if (sSucceeCount != 0) {
                    sb.append("\n");
                }
                sb.append(getResources().getQuantityString(R.plurals.item_incompletely_downloaded, sFailedCount, sFailedCount));
            }
            mDownloadedBuilder.setContentText(sb.toString());
        }
        mNotifyManager.notify(NOTIFY_ID_DOWNLOAD, mDownloadedBuilder.build());

        checkStopSelf();
    }

    @Override
    public void onCancel(DownloadManager.DownloadUpdateInfo info) {
        mNotifyManager.cancel(NOTIFY_ID_DOWNLOADING);
        checkStopSelf();
    }

    private void checkStopSelf() {
        if (!mDownloadManager.hasWaitingDownloadInfo()) {
            stopForeground(true);
            stopSelf();
        }
    }
}
