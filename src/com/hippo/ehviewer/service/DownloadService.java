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

package com.hippo.ehviewer.service;

import java.util.ArrayList;
import java.util.List;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.data.Data;
import com.hippo.ehviewer.data.DownloadInfo;
import com.hippo.ehviewer.data.GalleryInfo;
import com.hippo.ehviewer.ehclient.ExDownloader;
import com.hippo.ehviewer.ehclient.ExDownloaderManager;
import com.hippo.ehviewer.ui.DownloadActivity;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Utils;

public class DownloadService extends Service
        implements ExDownloader.ListenerForDownload {
    @SuppressWarnings("unused")
    private static final String TAG = DownloadService.class.getSimpleName();
    private static final int DOWNLOADING_NOTIFY_ID = -1;
    private static final int DOWNLOAD_NOTIFY_ID = -2;
    //private static final int DOWNLOAD_FAILED_NOTIFY_ID = -3;

    public static final String ACTION_UPDATE = "com.hippo.ehviewer.service.DownloadService.UPDATE";
    public static final String ACTION_STOP = "com.hippo.ehviewer.service.DownloadService.STOP";
    public static final String ACTION_STOP_ALL = "com.hippo.ehviewer.service.DownloadService.STOP_ALL";
    public static final String ACTION_CLEAR = "com.hippo.ehviewer.service.DownloadService.ACTION_CLEAR";

    private Context mContext;
    private Data mData;
    private ExDownloaderManager mEdManager;
    private volatile DownloadInfo mCurDownloadInfo = null;
    private volatile ExDownloader mCurExDownloader = null;
    private ServiceBinder mBinder = null;
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;
    private String mSpeedStr = null;

    private final List<Integer> mDownloadOk = new ArrayList<Integer>();
    private final List<Integer> mDownloadFailed = new ArrayList<Integer>();

    @Override
    public void onCreate() {
        super.onCreate();

        mContext = getApplication();
        mData = Data.getInstance();
        mEdManager = ExDownloaderManager.getInstance();

        mBinder = new ServiceBinder();
        mNotifyManager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private synchronized void handleIntent(Intent intent) {
        if (intent == null)
            return;

        String action = intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopCurrentTask();
            mNotifyManager.cancel(DOWNLOADING_NOTIFY_ID);
        } else if (ACTION_STOP_ALL.equals(action)) {
            stopAll();
            mNotifyManager.cancel(DOWNLOADING_NOTIFY_ID);
        } else if (ACTION_CLEAR.equals(action)) {
            mDownloadOk.clear();
            mDownloadFailed.clear();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        handleIntent(intent);

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mNotifyManager.cancel(DOWNLOADING_NOTIFY_ID);

        mBinder = null;
        mNotifyManager = null;
    }

    // Try to start download
    public synchronized void notifyDownloadInfoChanged() {
        if (mCurDownloadInfo != null || mCurExDownloader != null)
            return;

        mCurDownloadInfo = mData.getFirstWaitDownloadInfo();
        if (mCurDownloadInfo == null) {
            stopSelf();
            return;
        }

        mCurDownloadInfo.state = DownloadInfo.STATE_DOWNLOAD;
        mCurDownloadInfo.download = -1;
        mCurDownloadInfo.total = -1;

        GalleryInfo gi = mCurDownloadInfo.galleryInfo;
        mCurExDownloader = mEdManager.getExDownloader(gi.gid,
                gi.token, gi.title, mCurDownloadInfo.mode);
        mCurExDownloader.setListenerDownload(this);
        mCurExDownloader.setDownloadMode(true);
    }

    /**
     * If task is in list, reture false
     * @param galleryInfo
     * @return
     */
    public boolean add(GalleryInfo galleryInfo) {
        int gid = galleryInfo.gid;
        DownloadInfo di;
        if ((di = mData.getDownload(gid)) != null) {
            if (di.state != DownloadInfo.STATE_DOWNLOAD) {
                di.state = DownloadInfo.STATE_WAIT;
                notifyDownloadInfoChanged();
                notifyUpdate();
            }
            return false;
        } else {
            di = new DownloadInfo(galleryInfo, Config.getMode());
            di.state = DownloadInfo.STATE_WAIT;
            mData.addDownload(di);
            notifyDownloadInfoChanged();
            notifyUpdate();
            return true;
        }
    }

    public synchronized void stop(DownloadInfo di) {
        if (mCurDownloadInfo == di) {
            // Cancel download notification
            mNotifyManager.cancel(DOWNLOADING_NOTIFY_ID);

            // Target downloadinfo is downloading
            mCurDownloadInfo.state = DownloadInfo.STATE_NONE;
            mData.addDownload(mCurDownloadInfo);
            mCurDownloadInfo = null;

            mCurExDownloader.setListenerDownload(null);
            mCurExDownloader.setDownloadMode(false);
            mEdManager.freeExDownloader(mCurExDownloader);
            mCurExDownloader = null;

            notifyDownloadInfoChanged();
            notifyUpdate();
        } else {
            di.state = DownloadInfo.STATE_NONE;
            notifyUpdate();
        }
    }

    public void stopCurrentTask() {
        if (mCurDownloadInfo != null)
            stop(mCurDownloadInfo);
    }

    public void startAll() {
        for (DownloadInfo di : mData.getAllDownloads()) {
            if (di.state == DownloadInfo.STATE_NONE ||
                    (di.state == DownloadInfo.STATE_FINISH && di.legacy != 0))
                di.state = DownloadInfo.STATE_WAIT;
        }
        notifyDownloadInfoChanged();
        notifyUpdate();
    }

    public synchronized void stopAll() {
        for (DownloadInfo di : mData.getAllDownloads()) {
            if (di.state == DownloadInfo.STATE_WAIT ||
                    di.state == DownloadInfo.STATE_DOWNLOAD) {
                if (mCurDownloadInfo == di) {
                    // Cancel download notification
                    mNotifyManager.cancel(DOWNLOADING_NOTIFY_ID);

                    // Target downloadinfo is downloading
                    mCurDownloadInfo.state = DownloadInfo.STATE_NONE;
                    mData.addDownload(mCurDownloadInfo);
                    mCurDownloadInfo = null;

                    mCurExDownloader.setListenerDownload(null);
                    mCurExDownloader.setDownloadMode(false);
                    mEdManager.freeExDownloader(mCurExDownloader);
                    mCurExDownloader = null;
                } else {
                    di.state = DownloadInfo.STATE_NONE;
                }
            }
        }

        notifyDownloadInfoChanged();
        notifyUpdate();
    }

    public void delete(DownloadInfo di) {
        stop(di);
        mData.deleteDownload(di.galleryInfo.gid);
        notifyUpdate();
    }

    private void notifyUpdate() {
        Intent it = new Intent(ACTION_UPDATE);
        sendBroadcast(it);
    }

    public class ServiceBinder extends Binder{
        public DownloadService getService(){
            return DownloadService.this;
        }
    }

    private void ensureNotification() {
        if (mBuilder != null)
            return;

        mBuilder = new NotificationCompat.Builder(mContext);
        mBuilder.setSmallIcon(android.R.drawable.stat_sys_download);
        Intent intent = new Intent(DownloadService.this,DownloadActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(DownloadService.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setOngoing(true).setAutoCancel(false);

        // Add action
        Intent stopIntent = new Intent(this, DownloadService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent piStop = PendingIntent.getService(this, 0, stopIntent, 0);
        mBuilder.addAction(R.drawable.ic_clear2, getString(R.string.stop), piStop);

        Intent stopAllIntent = new Intent(this, DownloadService.class);
        stopAllIntent.setAction(ACTION_STOP_ALL);
        PendingIntent piStopAll = PendingIntent.getService(this, 0, stopAllIntent, 0);
        mBuilder.addAction(R.drawable.ic_clear, getString(R.string.stop_all), piStopAll);
    }

    @Override
    public void onStart(int gid) {
        if (mCurDownloadInfo == null)
            return;

        mCurDownloadInfo.download = -1;
        mCurDownloadInfo.total = -1;

        ensureNotification();

        mBuilder.setContentTitle(getString(R.string.start_download)  + " " + gid)
                .setContentText(null)
                .setProgress(0, 0, true);
        mNotifyManager.notify(DOWNLOADING_NOTIFY_ID, mBuilder.build());
        mNotifyManager.cancel(gid);

        notifyUpdate();
    }

    @Override
    public void onDownload(int gid, int downloadSize, int totalSize) {
        if (mCurDownloadInfo == null)
            return;

        mCurDownloadInfo.download = downloadSize;
        mCurDownloadInfo.total = totalSize;

        ensureNotification();

        mBuilder.setContentTitle(getString(R.string.downloading)  + " " + gid)
                .setContentText(mSpeedStr)
                .setProgress(totalSize, downloadSize, false);
        mNotifyManager.notify(DOWNLOADING_NOTIFY_ID, mBuilder.build());

        notifyUpdate();
    }

    @Override
    public void onUpdateSpeed(int gid, int speed) {
        if (mCurDownloadInfo == null)
            return;

        mCurDownloadInfo.speed = speed;
        mSpeedStr = Utils.sizeToString(speed) + "/S";

        ensureNotification();

        mBuilder.setContentTitle(getString(R.string.downloading)  + " " + gid)
                .setContentText(mSpeedStr)
                .setProgress(mCurDownloadInfo.total, mCurDownloadInfo.download,
                        mCurDownloadInfo.total == -1 ? true :false);
        mNotifyManager.notify(DOWNLOADING_NOTIFY_ID, mBuilder.build());

        notifyUpdate();
    }

    private synchronized String getDownloadNotificationText() {

        int ok = mDownloadOk.size();
        int failed = mDownloadFailed.size();

        // TODO
        if (ok == 0 && failed == 0) {
            return "null";
        } else if (ok == 1 && failed == 0) {
            return String.format(getString(R.string.download_1), mDownloadOk.get(0));
        } else if (failed == 0) {
            return String.format(getString(R.string.download_2), mDownloadOk.size());
        } else if (ok == 0 && failed == 1) {
            return String.format(getString(R.string.download_3), mDownloadFailed.get(0));
        } else if (ok == 0) {
            return String.format(getString(R.string.download_4), mDownloadFailed.size());
        } else if (ok == 1 && failed == 1){
            return String.format(getString(R.string.download_5), mDownloadOk.get(0), mDownloadFailed.get(0));
        } else {
            return String.format(getString(R.string.download_6), mDownloadOk.size(), mDownloadFailed.size());
        }
    }

    @Override
    public synchronized void onDownloadOver(int gid, int legacy) {
        if (mCurDownloadInfo == null)
            return;

        mCurDownloadInfo.legacy = legacy;
        if (mCurDownloadInfo.legacy == 0) {
            // Download ok
            if (!mDownloadOk.contains(gid))
                mDownloadOk.add(gid);
            mDownloadFailed.remove((Integer) gid);
        } else {
            // Download failed
            if (!mDownloadFailed.contains(gid))
                mDownloadFailed.add(gid);
            mDownloadOk.remove((Integer) gid);
        }

        Intent clearIntent = new Intent(this, DownloadService.class);
        clearIntent.setAction(ACTION_CLEAR);
        PendingIntent piClear = PendingIntent.getService(this, 0, clearIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
        builder.setSmallIcon(android.R.drawable.stat_sys_download_done);
        Intent intent = new Intent(DownloadService.this,DownloadActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(DownloadService.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
        builder.setContentTitle(getString(R.string.download_over))
                .setContentText(getDownloadNotificationText())
                .setDeleteIntent(piClear)
                .setOngoing(false).setAutoCancel(true);
        mNotifyManager.notify(DOWNLOAD_NOTIFY_ID, builder.build());
        mNotifyManager.cancel(DOWNLOADING_NOTIFY_ID);

        mCurDownloadInfo.legacy = legacy;
        mCurDownloadInfo.state = DownloadInfo.STATE_FINISH;
        mData.addDownload(mCurDownloadInfo);
        mCurDownloadInfo = null;

        mCurExDownloader.setListenerDownload(null);
        mCurExDownloader.setDownloadMode(false);
        mEdManager.freeExDownloader(mCurExDownloader);
        mCurExDownloader = null;

        notifyDownloadInfoChanged();
        notifyUpdate();
    }
}
