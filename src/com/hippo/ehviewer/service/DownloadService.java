package com.hippo.ehviewer.service;

import java.util.HashMap;
import java.util.Map;

import com.hippo.ehviewer.DownloadInfo;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.activity.DownloadActivity;
import com.hippo.ehviewer.util.Download;
import com.hippo.ehviewer.util.EhClient;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class DownloadService extends Service {
    private static final int DOWNLOAD_NOTIFY_ID = -1;
    private static final String TAG = "DownloadService";
    public static final String ACTION_UPDATE = "com.hippo.ehviewer.service.DownloadService";
    public static final String KEY_GID = "gid";
    public static final String KEY_INDEX = "index";
    public static final String KEY_STATE = "state";
    
    private ServiceBinder mBinder = null;
    
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;
    
    @Override  
    public void onCreate() {  
        super.onCreate();
        
        Log.d(TAG, "Service onCreate");
        
        mBinder = new ServiceBinder();
        mNotifyManager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setSmallIcon(R.drawable.ic_launcher);
        
        Intent intent = new Intent(DownloadService.this,DownloadActivity.class);  
        PendingIntent pendingIntent = PendingIntent.getActivity(DownloadService.this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);  
        mBuilder.setContentIntent(pendingIntent);  
        
        EhClient.DownloadMangaManager.setDownloadService(this);
        EhClient.DownloadMangaManager.setOnDownloadMangaListener(new EhClient.OnDownloadMangaListener() {
            @Override
            public void onDownloadMangaAllStart() {
                mBuilder.setContentTitle(getString(R.string.start_download_task))
                        .setContentText(null)
                        .setProgress(0, 0, true).setOngoing(true).setAutoCancel(false);;
                mNotifyManager.notify(DOWNLOAD_NOTIFY_ID, mBuilder.build());
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
                    mBuilder.setContentTitle(getString(R.string.downloading)
                        + " " + di.title)
                        .setContentText(null)
                        .setProgress(0, 0, true).setOngoing(true).setAutoCancel(false);;
                mNotifyManager.notify(DOWNLOAD_NOTIFY_ID, mBuilder.build());
                }
            }
            
            @Override
            public void onDownloadMangaStart(String id, int pageSum, int startIndex) {
                DownloadInfo di = Download.get(id);
                if (di == null)
                    mNotifyManager.cancel(DOWNLOAD_NOTIFY_ID);
                else {
                    mBuilder.setContentTitle(getString(R.string.downloading)
                            + " " + di.title)
                            .setContentText(startIndex + " / " + pageSum)
                            .setProgress(pageSum, startIndex, false).setOngoing(true)
                            .setAutoCancel(false);
                    mNotifyManager.notify(DOWNLOAD_NOTIFY_ID, mBuilder.build());
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
                mBuilder.setContentTitle(mesg);
                mBuilder.setContentText(null).setProgress(0, 0, false).setOngoing(false);
                mNotifyManager.notify(Integer.parseInt(id), mBuilder.build());
            }
            
            @Override
            public void onDownloadPage(String id, int pageSum, int index) {
                mBuilder.setContentTitle(getString(R.string.downloading)
                        + " " + Download.get(id).title)
                        .setContentText(index + " / " + pageSum)
                        .setProgress(pageSum, index, false).setOngoing(true)
                        .setAutoCancel(false);
                mNotifyManager.notify(DOWNLOAD_NOTIFY_ID, mBuilder.build());
            }

            @Override
            public void onDownloadPageProgress(String id, int pageSum,
                    int index, float totalSize, float downloadSize) {
                // TODO Auto-generated method stub
                
            }
        });
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
        mBuilder = null;
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
        EhClient.DownloadMangaManager.add(di);
    }
    
    public void cancel(String id) {
        EhClient.DownloadMangaManager.cancel(id);
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
