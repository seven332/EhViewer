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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.hippo.ehviewer.AppContext;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.ehclient.EhClient;
import com.hippo.ehviewer.ehclient.EhClient.DownloadMangaManager;
import com.hippo.ehviewer.ui.DownloadActivity;
import com.hippo.ehviewer.ui.DownloadInfo;
import com.hippo.ehviewer.util.Download;
import com.hippo.ehviewer.util.Log;

public class DownloadService extends Service {
    private static final int DOWNLOAD_NOTIFY_ID = -1;
    private static final String TAG = "DownloadService";
    public static final String ACTION_UPDATE = "com.hippo.ehviewer.service.DownloadService";
    public static final String KEY_GID = "gid";
    public static final String KEY_INDEX = "index";
    public static final String KEY_STATE = "state";

    private AppContext mAppContext;

    private ServiceBinder mBinder = null;

    private NotificationManager mNotifyManager;

    private DownloadMangaManager downloadMangaManager;

    @Override
    public void onCreate() {
        super.onCreate();

        mAppContext = (AppContext)getApplication();

        mBinder = new ServiceBinder();
        mNotifyManager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);

        downloadMangaManager = EhClient.getInstance().new DownloadMangaManager();

        downloadMangaManager.setDownloadService(this);
        downloadMangaManager.setOnDownloadMangaListener(new EhClient.OnDownloadMangaListener() {
            @Override
            public void onDownloadMangaAllStart() {
                Notification.Builder builder = new Notification.Builder(getApplicationContext());
                setNotification(builder);
                builder.setContentTitle(getString(R.string.start_download_task))
                        .setContentText(null)
                        .setProgress(0, 0, true).setOngoing(true).setAutoCancel(false);
                mNotifyManager.notify(DOWNLOAD_NOTIFY_ID, builder.getNotification());
            }

            @Override
            public void onDownloadMangaAllOver() {
                mNotifyManager.cancel(DOWNLOAD_NOTIFY_ID);
                stopSelf();
            }

            @Override
            public void onDownloadMangaStart(String id) {
                DownloadInfo di = Download.get(id);
                if (di == null)
                    mNotifyManager.cancel(DOWNLOAD_NOTIFY_ID);
                else {
                    Notification.Builder builder = new Notification.Builder(getApplicationContext());
                    setNotification(builder);
                    builder.setContentTitle(getString(R.string.downloading)
                        + " " + di.title)
                        .setContentText(null)
                        .setProgress(0, 0, true).setOngoing(true).setAutoCancel(false);
                    mNotifyManager.notify(DOWNLOAD_NOTIFY_ID, builder.getNotification());
                }
            }

            @Override
            public void onDownloadMangaStart(String id, int pageSum, int startIndex) {
                DownloadInfo di = Download.get(id);
                if (di == null)
                    mNotifyManager.cancel(DOWNLOAD_NOTIFY_ID);
                else {
                    Notification.Builder builder = new Notification.Builder(getApplicationContext());
                    setNotification(builder);
                    builder.setContentTitle(getString(R.string.downloading)
                            + " " + di.title)
                            .setContentText(startIndex + " / " + pageSum)
                            .setProgress(pageSum, startIndex, false).setOngoing(true)
                            .setAutoCancel(false);
                    mNotifyManager.notify(DOWNLOAD_NOTIFY_ID, builder.getNotification());
                    mNotifyManager.cancel(Integer.parseInt(id));
                }
            }

            @Override
            public void onDownloadMangaStop(String id) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onDownloadMangaOver(String id, boolean ok) {
                String mesg = null;
                if (ok)
                    mesg = getString(R.string.download_successfully) + " ";
                else
                    mesg = getString(R.string.download_unsuccessfully) + " ";
                DownloadInfo di = Download.get(id);
                if (di != null)
                    mesg += Download.get(id).title;
                Notification.Builder builder = new Notification.Builder(getApplicationContext());
                setNotification(builder);
                builder.setContentTitle(mesg);
                builder.setContentText(null).setProgress(0, 0, false).setOngoing(false);
                mNotifyManager.notify(Integer.parseInt(id), builder.getNotification());
            }

            @Override
            public void onDownloadPage(String id, int pageSum, int index) {
                DownloadInfo di = Download.get(id);
                if (di == null)
                    mNotifyManager.cancel(DOWNLOAD_NOTIFY_ID);
                else {
                    Notification.Builder builder = new Notification.Builder(getApplicationContext());
                    setNotification(builder);
                    builder.setContentTitle(getString(R.string.downloading)
                            + " " + di.title)
                            .setContentText(index + " / " + pageSum)
                            .setProgress(pageSum, index, false).setOngoing(true)
                            .setAutoCancel(false);
                    mNotifyManager.notify(DOWNLOAD_NOTIFY_ID, builder.getNotification());
                }
            }

            @Override
            public void onDownloadPageProgress(String id, int pageSum,
                    int index, float totalSize, float downloadSize) {
                // TODO Auto-generated method stub

            }
        });
    }

    private void setNotification(Notification.Builder builder) {

        builder.setSmallIcon(R.drawable.ic_launcher);

        Intent intent = new Intent(DownloadService.this,DownloadActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(DownloadService.this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand");

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Service onBind");

        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "Service onDestroy");
        mNotifyManager.cancel(DOWNLOAD_NOTIFY_ID);

        mBinder = null;
        mNotifyManager = null;
    }

    public void add(String gid, String thumb, String detailUrlStr, String title) {
        DownloadInfo di = Download.get(gid);
        if (di == null) {
            di = new DownloadInfo();
            di.status = DownloadInfo.STOP;
            di.gid = gid;
            di.thumb = thumb;
            di.title = title;
            di.type = DownloadInfo.DETAIL_URL;
            di.detailUrlStr = detailUrlStr;
            Download.add(String.valueOf(gid), di);
            notifyUpdate();
        }
        add(di);
    }

    public void add(String gid, String thumb, String detailUrlStr,
            String pageUrlStr, int pageSum, int lastStartIndex, String title) {
        DownloadInfo di = Download.get(gid);
        if (di == null) {
            di = new DownloadInfo();
            di.status = DownloadInfo.STOP;
            di.gid = gid;
            di.thumb = thumb;
            di.title = title;
            di.type = DownloadInfo.PAGE_URL;
            di.detailUrlStr = detailUrlStr;
            di.pageUrlStr = pageUrlStr;
            di.pageSum = pageSum;
            di.lastStartIndex = lastStartIndex;
            Download.add(String.valueOf(gid), di);
            notifyUpdate();
        }
        add(di);
    }

    public void add(DownloadInfo di) {
        if (di.status == DownloadInfo.DOWNLOADING
                || di.status == DownloadInfo.WAITING)
            return;
        di.status = DownloadInfo.WAITING;
        downloadMangaManager.add(di);
    }

    public void cancel(String id) {
        downloadMangaManager.cancel(id);
    }

    public void notifyUpdate(){
        Intent it = new Intent(ACTION_UPDATE);
        sendBroadcast(it);
    }

    public void notifyUpdate(String gid, int index, int state){
        Intent it = new Intent(ACTION_UPDATE);
        it.putExtra(KEY_GID, gid);
        it.putExtra(KEY_INDEX, index);
        it.putExtra(KEY_STATE, state);
        sendBroadcast(it);
    }

    public class ServiceBinder extends Binder{
        public DownloadService getService(){
            return DownloadService.this;
        }
    }
}
